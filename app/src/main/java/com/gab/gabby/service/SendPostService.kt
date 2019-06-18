package com.gab.gabby.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Parcelable
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.gab.gabby.R
import com.gab.gabby.appstore.EventHub
import com.gab.gabby.appstore.StatusComposedEvent
import com.gab.gabby.db.AccountEntity
import com.gab.gabby.db.AccountManager
import com.gab.gabby.db.AppDatabase
import com.gab.gabby.di.Injectable
import com.gab.gabby.entity.Status
import com.gab.gabby.network.MastodonApi
import com.gab.gabby.util.SavePostHelper
import com.gab.gabby.util.randomAlphanumericString
import dagger.android.AndroidInjection
import kotlinx.android.parcel.Parcelize
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SendPostService : Service(), Injectable {

    @Inject
    lateinit var mastodonApi: MastodonApi
    @Inject
    lateinit var accountManager: AccountManager
    @Inject
    lateinit var eventHub: EventHub
    @Inject
    lateinit var database: AppDatabase

    private lateinit var savePostHelper: SavePostHelper

    private val postsToSend = ConcurrentHashMap<Int, PostToSend>()
    private val sendCalls = ConcurrentHashMap<Int, Call<Status>>()

    private val timer = Timer()

    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onCreate() {
        AndroidInjection.inject(this)
        savePostHelper = SavePostHelper(database.postDao(), this)
        super.onCreate()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        if (intent.hasExtra(KEY_POST)) {
            val postToSend = intent.getParcelableExtra<PostToSend>(KEY_POST)
                    ?: throw IllegalStateException("SendPostService started without $KEY_POST extra")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL_ID, getString(R.string.send_post_notification_channel_name), NotificationManager.IMPORTANCE_LOW)
                notificationManager.createNotificationChannel(channel)

            }

            var notificationText = postToSend.warningText
            if (notificationText.isBlank()) {
                notificationText = postToSend.text
            }

            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notify)
                    .setContentTitle(getString(R.string.send_post_notification_title))
                    .setContentText(notificationText)
                    .setProgress(1, 0, true)
                    .setOngoing(true)
                    .setColor(ContextCompat.getColor(this, R.color.gabby_blue))
                    .addAction(0, getString(android.R.string.cancel), cancelSendingIntent(sendingNotificationId))

            if (postsToSend.size == 0 || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
                startForeground(sendingNotificationId, builder.build())
            } else {
                notificationManager.notify(sendingNotificationId, builder.build())
            }

            postsToSend[sendingNotificationId] = postToSend
            sendPost(sendingNotificationId--)

        } else {

            if (intent.hasExtra(KEY_CANCEL)) {
                cancelSending(intent.getIntExtra(KEY_CANCEL, 0))
            }

        }

        return START_NOT_STICKY

    }

    private fun sendPost(postId: Int) {

        // when postToSend == null, sending has been canceled
        val postToSend = postsToSend[postId] ?: return

        // when account == null, user has logged out, cancel sending
        val account = accountManager.getAccountById(postToSend.accountId)

        if (account == null) {
            postsToSend.remove(postId)
            notificationManager.cancel(postId)
            stopSelfWhenDone()
            return
        }

        postToSend.retries++

        val sendCall = mastodonApi.createStatus(
                "Bearer " + account.accessToken,
                account.domain,
                postToSend.text,
                postToSend.inReplyToId,
                postToSend.warningText,
                postToSend.visibility,
                postToSend.sensitive,
                postToSend.mediaIds,
                postToSend.idempotencyKey
        )


        sendCalls[postId] = sendCall

        val callback = object : Callback<Status> {
            override fun onResponse(call: Call<Status>, response: Response<Status>) {

                postsToSend.remove(postId)

                if (response.isSuccessful) {
                    // If the status was loaded from a draft, delete the draft and associated media files.
                    if (postToSend.savedPostUid != 0) {
                        savePostHelper.deleteDraft(postToSend.savedPostUid)
                    }

                    response.body()?.let(::StatusComposedEvent)?.let(eventHub::dispatch)

                    notificationManager.cancel(postId)

                } else {
                    // the server refused to accept the post, save post & show error message
                    savePostToDrafts(postToSend)

                    val builder = NotificationCompat.Builder(this@SendPostService, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_notify)
                            .setContentTitle(getString(R.string.send_post_notification_error_title))
                            .setContentText(getString(R.string.send_post_notification_saved_content))
                            .setColor(ContextCompat.getColor(this@SendPostService, R.color.gabby_blue))

                    notificationManager.cancel(postId)
                    notificationManager.notify(errorNotificationId--, builder.build())

                }

                stopSelfWhenDone()

            }

            override fun onFailure(call: Call<Status>, t: Throwable) {
                var backoff = TimeUnit.SECONDS.toMillis(postToSend.retries.toLong())
                if (backoff > MAX_RETRY_INTERVAL) {
                    backoff = MAX_RETRY_INTERVAL
                }

                timer.schedule(object : TimerTask() {
                    override fun run() {
                        sendPost(postId)
                    }
                }, backoff)
            }
        }

        sendCall.enqueue(callback)

    }

    private fun stopSelfWhenDone() {

        if (postsToSend.isEmpty()) {
            ServiceCompat.stopForeground(this@SendPostService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun cancelSending(postId: Int) {
        val postToCancel = postsToSend.remove(postId)
        if (postToCancel != null) {
            val sendCall = sendCalls.remove(postId)
            sendCall?.cancel()

            savePostToDrafts(postToCancel)

            val builder = NotificationCompat.Builder(this@SendPostService, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notify)
                    .setContentTitle(getString(R.string.send_post_notification_cancel_title))
                    .setContentText(getString(R.string.send_post_notification_saved_content))
                    .setColor(ContextCompat.getColor(this@SendPostService, R.color.gabby_blue))

            notificationManager.notify(postId, builder.build())

            timer.schedule(object : TimerTask() {
                override fun run() {
                    notificationManager.cancel(postId)
                    stopSelfWhenDone()
                }
            }, 5000)

        }
    }

    private fun savePostToDrafts(post: PostToSend) {

        savePostHelper.savePost(post.text,
                post.warningText,
                post.savedJsonUrls,
                post.mediaUris,
                post.mediaDescriptions,
                post.savedPostUid,
                post.inReplyToId,
                post.replyingStatusContent,
                post.replyingStatusAuthorUsername,
                Status.Visibility.byString(post.visibility))
    }

    private fun cancelSendingIntent(postId: Int): PendingIntent {

        val intent = Intent(this, SendPostService::class.java)

        intent.putExtra(KEY_CANCEL, postId)

        return PendingIntent.getService(this, postId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }


    companion object {

        private const val KEY_POST = "post"
        private const val KEY_CANCEL = "cancel_id"
        private const val CHANNEL_ID = "send_posts"

        private val MAX_RETRY_INTERVAL = TimeUnit.MINUTES.toMillis(1)

        private var sendingNotificationId = -1 // use negative ids to not clash with other notis
        private var errorNotificationId = Int.MIN_VALUE // use even more negative ids to not clash with other notis

        @JvmStatic
        fun sendPostIntent(context: Context,
                           text: String,
                           warningText: String,
                           visibility: Status.Visibility,
                           sensitive: Boolean,
                           mediaIds: List<String>,
                           mediaUris: List<Uri>,
                           mediaDescriptions: List<String>,
                           inReplyToId: String?,
                           replyingStatusContent: String?,
                           replyingStatusAuthorUsername: String?,
                           savedJsonUrls: String?,
                           account: AccountEntity,
                           savedPostUid: Int
        ): Intent {
            val intent = Intent(context, SendPostService::class.java)

            val idempotencyKey = randomAlphanumericString(16)

            val postToSend = PostToSend(text,
                    warningText,
                    visibility.serverString(),
                    sensitive,
                    mediaIds,
                    mediaUris.map { it.toString() },
                    mediaDescriptions,
                    inReplyToId,
                    replyingStatusContent,
                    replyingStatusAuthorUsername,
                    savedJsonUrls,
                    account.id,
                    savedPostUid,
                    idempotencyKey,
                    0)

            intent.putExtra(KEY_POST, postToSend)

            if(mediaUris.isNotEmpty()) {
                // forward uri permissions
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val uriClip = ClipData(
                        ClipDescription("Post Media", arrayOf("image/*", "video/*")),
                        ClipData.Item(mediaUris[0])
                )
                mediaUris
                        .drop(1)
                        .forEach { mediaUri ->
                            uriClip.addItem(ClipData.Item(mediaUri))
                        }

                intent.clipData = uriClip

            }

            return intent
        }

    }
}

@Parcelize
data class PostToSend(val text: String,
                      val warningText: String,
                      val visibility: String,
                      val sensitive: Boolean,
                      val mediaIds: List<String>,
                      val mediaUris: List<String>,
                      val mediaDescriptions: List<String>,
                      val inReplyToId: String?,
                      val replyingStatusContent: String?,
                      val replyingStatusAuthorUsername: String?,
                      val savedJsonUrls: String?,
                      val accountId: Long,
                      val savedPostUid: Int,
                      val idempotencyKey: String,
                      var retries: Int) : Parcelable
