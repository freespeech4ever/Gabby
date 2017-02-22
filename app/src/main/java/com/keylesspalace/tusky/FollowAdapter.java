/* Copyright 2017 Andrew Dawson
 *
 * This file is part of Tusky.
 *
 * Tusky is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky. If not, see
 * <http://www.gnu.org/licenses/>. */

package com.keylesspalace.tusky;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

/** Both for follows and following lists. */
class FollowAdapter extends AccountAdapter {
    private static final int VIEW_TYPE_ACCOUNT = 0;
    private static final int VIEW_TYPE_FOOTER = 1;

    FollowAdapter(AccountActionListener accountActionListener,
            FooterActionListener footerActionListener) {
        super(accountActionListener, footerActionListener);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            default:
            case VIEW_TYPE_ACCOUNT: {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_account, parent, false);
                return new AccountViewHolder(view);
            }
            case VIEW_TYPE_FOOTER: {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_footer, parent, false);
                return new FooterViewHolder(view);
            }
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (position < accountList.size()) {
            AccountViewHolder holder = (AccountViewHolder) viewHolder;
            holder.setupWithAccount(accountList.get(position));
            holder.setupActionListener(accountActionListener);
        } else {
            FooterViewHolder holder = (FooterViewHolder) viewHolder;
            holder.setState(footerState);
            holder.setupButton(footerActionListener);
            holder.setRetryMessage(R.string.footer_retry_accounts);
            holder.setEndOfTimelineMessage(R.string.footer_end_of_accounts);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == accountList.size()) {
            return VIEW_TYPE_FOOTER;
        } else {
            return VIEW_TYPE_ACCOUNT;
        }
    }

    private static class AccountViewHolder extends RecyclerView.ViewHolder {
        private View container;
        private TextView username;
        private TextView displayName;
        private TextView note;
        private NetworkImageView avatar;
        private String id;

        AccountViewHolder(View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.account_container);
            username = (TextView) itemView.findViewById(R.id.account_username);
            displayName = (TextView) itemView.findViewById(R.id.account_display_name);
            note = (TextView) itemView.findViewById(R.id.account_note);
            avatar = (NetworkImageView) itemView.findViewById(R.id.account_avatar);
            avatar.setDefaultImageResId(R.drawable.avatar_default);
            avatar.setErrorImageResId(R.drawable.avatar_error);
        }

        void setupWithAccount(Account account) {
            id = account.id;
            String format = username.getContext().getString(R.string.status_username_format);
            String formattedUsername = String.format(format, account.username);
            username.setText(formattedUsername);
            displayName.setText(account.displayName);
            note.setText(account.note);
            Context context = avatar.getContext();
            ImageLoader imageLoader = VolleySingleton.getInstance(context).getImageLoader();
            avatar.setImageUrl(account.avatar, imageLoader);
        }

        void setupActionListener(final AccountActionListener listener) {
            container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onViewAccount(id);
                }
            });
        }
    }
}
