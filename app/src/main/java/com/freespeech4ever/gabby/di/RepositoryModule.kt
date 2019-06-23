package com.freespeech4ever.gabby.di

import com.google.gson.Gson
import com.freespeech4ever.gabby.db.AccountManager
import com.freespeech4ever.gabby.db.AppDatabase
import com.freespeech4ever.gabby.network.MastodonApi
import com.freespeech4ever.gabby.repository.TimelineRepository
import com.freespeech4ever.gabby.repository.TimelineRepositoryImpl
import com.freespeech4ever.gabby.util.HtmlConverter
import dagger.Module
import dagger.Provides

@Module
class RepositoryModule {
    @Provides
    fun providesTimelineRepository(db: AppDatabase, mastodonApi: MastodonApi,
                                   accountManager: AccountManager, gson: Gson,
                                   htmlConverter: HtmlConverter): TimelineRepository {
        return TimelineRepositoryImpl(db.timelineDao(), mastodonApi, accountManager, gson,
                htmlConverter)
    }
}