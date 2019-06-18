/* Copyright 2019 Conny Duck
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

package com.gab.gabby.entity

import com.google.gson.annotations.SerializedName

data class Conversation(
        val id: String,
        val accounts: List<Account>,
        @SerializedName("last_status") val lastStatus: Status?,  // should never be null, but apparently its possible https://github.com/gabbyapp/Gabby/issues/1038
        val unread: Boolean
)