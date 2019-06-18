/* Copyright 2017 Andrew Dawson
 *
 * This file is a part of Gabby.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Gabby is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Gabby; if not,
 * see <http://www.gnu.org/licenses>. */

package com.gab.gabby;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.gab.gabby.adapter.SavedPostAdapter;
import com.gab.gabby.appstore.EventHub;
import com.gab.gabby.appstore.StatusComposedEvent;
import com.gab.gabby.db.AppDatabase;
import com.gab.gabby.db.PostDao;
import com.gab.gabby.db.PostEntity;
import com.gab.gabby.di.Injectable;
import com.gab.gabby.util.SavePostHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.android.schedulers.AndroidSchedulers;

import static com.uber.autodispose.AutoDispose.autoDisposable;
import static com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider.from;

public final class SavedPostActivity extends BaseActivity implements SavedPostAdapter.SavedPostAction,
        Injectable {

    private SavePostHelper savePostHelper;

    // ui
    private SavedPostAdapter adapter;
    private TextView noContent;

    private List<PostEntity> posts = new ArrayList<>();
    @Nullable
    private AsyncTask<?, ?, ?> asyncTask;

    @Inject
    EventHub eventHub;
    @Inject
    AppDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        savePostHelper = new SavePostHelper(database.postDao(), this);

        eventHub.getEvents()
                .observeOn(AndroidSchedulers.mainThread())
                .ofType(StatusComposedEvent.class)
                .as(autoDisposable(from(this, Lifecycle.Event.ON_DESTROY)))
                .subscribe((__) -> this.fetchPosts());

        setContentView(R.layout.activity_saved_post);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setTitle(getString(R.string.title_saved_post));
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setDisplayShowHomeEnabled(true);
        }

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        noContent = findViewById(R.id.no_content);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        DividerItemDecoration divider = new DividerItemDecoration(
                this, layoutManager.getOrientation());
        recyclerView.addItemDecoration(divider);
        adapter = new SavedPostAdapter(this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchPosts();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (asyncTask != null) asyncTask.cancel(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchPosts() {
        asyncTask = new FetchPojosTask(this, database.postDao())
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void setNoContent(int size) {
        if (size == 0) {
            noContent.setVisibility(View.VISIBLE);
        } else {
            noContent.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void delete(int position, PostEntity item) {

        savePostHelper.deleteDraft(item);

        posts.remove(position);
        // update adapter
        if (adapter != null) {
            adapter.removeItem(position);
            setNoContent(posts.size());
        }
    }

    @Override
    public void click(int position, PostEntity item) {
        Intent intent = new ComposeActivity.IntentBuilder()
                .savedPostUid(item.getUid())
                .postText(item.getText())
                .contentWarning(item.getContentWarning())
                .savedJsonUrls(item.getUrls())
                .savedJsonDescriptions(item.getDescriptions())
                .inReplyToId(item.getInReplyToId())
                .replyingStatusAuthor(item.getInReplyToUsername())
                .replyingStatusContent(item.getInReplyToText())
                .visibility(item.getVisibility())
                .build(this);
        startActivity(intent);
    }

    static final class FetchPojosTask extends AsyncTask<Void, Void, List<PostEntity>> {

        private final WeakReference<SavedPostActivity> activityRef;
        private final PostDao postDao;

        FetchPojosTask(SavedPostActivity activity, PostDao postDao) {
            this.activityRef = new WeakReference<>(activity);
            this.postDao = postDao;
        }

        @Override
        protected List<PostEntity> doInBackground(Void... voids) {
            return postDao.loadAll();
        }

        @Override
        protected void onPostExecute(List<PostEntity> pojos) {
            super.onPostExecute(pojos);
            SavedPostActivity activity = activityRef.get();
            if (activity == null) return;

            activity.posts.clear();
            activity.posts.addAll(pojos);

            // set ui
            activity.setNoContent(pojos.size());
            activity.adapter.setItems(activity.posts);
            activity.adapter.notifyDataSetChanged();
        }
    }
}
