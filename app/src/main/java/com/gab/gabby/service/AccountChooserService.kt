/* Copyright 2019 Levi Bard
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

package com.gab.gabby.service

import android.annotation.TargetApi
import android.content.ComponentName
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Bundle
import android.service.chooser.ChooserTarget
import android.service.chooser.ChooserTargetService
import android.text.TextUtils
import com.bumptech.glide.Glide
import com.gab.gabby.R
import com.gab.gabby.GabbyApplication
import com.gab.gabby.db.AccountManager
import com.gab.gabby.di.Injectable
import com.gab.gabby.util.NotificationHelper


@TargetApi(23)
class AccountChooserService : ChooserTargetService(), Injectable {

    // cannot inject here, it crashes on APIs < 23
    lateinit var accountManager: AccountManager

    override fun onCreate() {
        super.onCreate()
        accountManager = (application as GabbyApplication).serviceLocator.get(AccountManager::class.java)
    }

    override fun onGetChooserTargets(targetActivityName: ComponentName?, intentFilter: IntentFilter?): MutableList<ChooserTarget> {
        val targets = mutableListOf<ChooserTarget>()
        for (account in accountManager.getAllAccountsOrderedByActive()) {
            val icon: Icon = if (TextUtils.isEmpty(account.profilePictureUrl)) {
                Icon.createWithResource(applicationContext, R.drawable.avatar_default)
            } else {
                val bmp = Glide.with(this)
                        .asBitmap()
                        .load(account.profilePictureUrl)
                        .error(R.drawable.avatar_default)
                        .placeholder(R.drawable.avatar_default)
                        .submit()
                Icon.createWithBitmap(bmp.get())
            }
            val bundle = Bundle()
            bundle.putLong(NotificationHelper.ACCOUNT_ID, account.id)
            targets.add(ChooserTarget(account.displayName, icon, 1.0f, targetActivityName, bundle))
        }
        return targets
    }
}
