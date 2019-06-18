/* Copyright 2018 Conny Duck
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

package com.gab.gabby.db

import androidx.paging.DataSource
import androidx.room.*
import com.gab.gabby.components.conversation.ConversationEntity

@Dao
interface ConversationsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(conversations: List<ConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(conversation: ConversationEntity)

    @Delete
    fun delete(conversation: ConversationEntity)

    @Query("SELECT * FROM ConversationEntity WHERE accountId = :accountId ORDER BY s_createdAt DESC")
    fun conversationsForAccount(accountId: Long) : DataSource.Factory<Int, ConversationEntity>

    @Query("DELETE FROM ConversationEntity WHERE accountId = :accountId")
    fun deleteForAccount(accountId: Long)


}
