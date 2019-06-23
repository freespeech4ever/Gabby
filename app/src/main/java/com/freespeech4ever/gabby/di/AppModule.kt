/* Copyright 2018 charlag
 *
 * This file is a part of Tusky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <http://www.gnu.org/licenses>. */


package com.freespeech4ever.gabby.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.freespeech4ever.gabby.TuskyApplication
import com.freespeech4ever.gabby.appstore.EventHub
import com.freespeech4ever.gabby.appstore.EventHubImpl
import com.freespeech4ever.gabby.db.AccountManager
import com.freespeech4ever.gabby.db.AppDatabase
import com.freespeech4ever.gabby.network.MastodonApi
import com.freespeech4ever.gabby.network.TimelineCases
import com.freespeech4ever.gabby.network.TimelineCasesImpl
import com.freespeech4ever.gabby.util.HtmlConverter
import com.freespeech4ever.gabby.util.HtmlConverterImpl
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by charlag on 3/21/18.
 */

@Module
class AppModule {

    @Provides
    fun providesApplication(app: TuskyApplication): Application = app

    @Provides
    fun providesContext(app: Application): Context = app

    @Provides
    fun providesSharedPreferences(app: Application): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(app)
    }

    @Provides
    fun providesBroadcastManager(app: Application): LocalBroadcastManager {
        return LocalBroadcastManager.getInstance(app)
    }

    @Provides
    fun providesTimelineUseCases(api: MastodonApi,
                                 eventHub: EventHub): TimelineCases {
        return TimelineCasesImpl(api, eventHub)
    }

    @Provides
    @Singleton
    fun providesAccountManager(app: TuskyApplication): AccountManager {
        return app.serviceLocator.get(AccountManager::class.java)
    }

    @Provides
    @Singleton
    fun providesEventHub(): EventHub = EventHubImpl

    @Provides
    @Singleton
    fun providesDatabase(app: TuskyApplication): AppDatabase {
        return app.serviceLocator.get(AppDatabase::class.java)
    }

    @Provides
    @Singleton
    fun providesHtmlConverter(): HtmlConverter {
        return HtmlConverterImpl()
    }
}