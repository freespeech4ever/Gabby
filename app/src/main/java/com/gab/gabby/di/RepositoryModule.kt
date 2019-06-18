package com.gab.gabby.di

import com.google.gson.Gson
import com.gab.gabby.db.AccountManager
import com.gab.gabby.db.AppDatabase
import com.gab.gabby.network.MastodonApi
import com.gab.gabby.repository.TimelineRepository
import com.gab.gabby.repository.TimelineRepositoryImpl
import com.gab.gabby.util.HtmlConverter
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