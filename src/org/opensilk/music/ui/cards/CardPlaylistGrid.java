/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.ui.cards;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.menu.RenamePlaylist;
import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.ui.activities.ProfileActivity;
import com.andrew.apollo.utils.MusicUtils;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.internal.base.BaseCard;

/**
 * Created by drew on 2/16/14.
 */
public class CardPlaylistGrid extends CardBaseThumb<Playlist> {

    public CardPlaylistGrid(Context context, Playlist data) {
        this(context, data, R.layout.card_grid_item_layout);
    }

    public CardPlaylistGrid(Context context, Playlist data, int innerLayout) {
        super(context, data, innerLayout);
    }

    @Override
    protected void initContent() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                final Bundle bundle = new Bundle();
                String playlistName;
                // Favorites list
                if (mData.mPlaylistId == -1) {
                    playlistName = getContext().getString(R.string.playlist_favorites);
                    bundle.putString(Config.MIME_TYPE, getContext().getString(R.string.playlist_favorites));
                    // Last added
                } else if (mData.mPlaylistId == -2) {
                    playlistName = getContext().getString(R.string.playlist_last_added);
                    bundle.putString(Config.MIME_TYPE, getContext().getString(R.string.playlist_last_added));
                } else {
                    // User created
                    playlistName = mData.mPlaylistName;
                    bundle.putString(Config.MIME_TYPE, MediaStore.Audio.Playlists.CONTENT_TYPE);
                    bundle.putLong(Config.ID, mData.mPlaylistId);
                }

                bundle.putString(Config.NAME, playlistName);

                // Create the intent to launch the profile activity
                final Intent intent = new Intent(getContext(), ProfileActivity.class);
                intent.putExtras(bundle);
                getContext().startActivity(intent);
            }
        });
    }

    @Override
    protected void initHeader() {
        final CardHeaderGrid header = new CardHeaderGrid(getContext());
        header.setButtonOverflowVisible(true);
        header.setTitle(mData.mPlaylistName);
        header.setLineTwo(MusicUtils.makeLabel(getContext(), R.plurals.Nsongs, mData.mSongs.size()));
        header.setPopupMenu(R.menu.card_artist, getNewHeaderPopupMenuListener());
        addCardHeader(header);
    }

    @Override
    protected void loadThumbnail(ImageFetcher fetcher, ImageView view) {
        if (mData.mSongs.size() > 0) {
            // for now just load the first songs art //TODO stacked art like gmusic
            Song song = mData.mSongs.get(0);
            fetcher.loadAlbumImage(song.mArtistName, song.mAlbumName, song.mAlbumId, view);
        }
    }

    protected CardHeader.OnClickCardHeaderPopupMenuListener getNewHeaderPopupMenuListener() {
        return new CardHeader.OnClickCardHeaderPopupMenuListener() {
            @Override
            public void onMenuItemClick(BaseCard baseCard, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.card_menu_play:
                        if (mData.mPlaylistId == -1) {
                            MusicUtils.playFavorites(getContext());
                        } else if (mData.mPlaylistId == -2) {
                            MusicUtils.playLastAdded(getContext());
                        } else {
                            MusicUtils.playPlaylist(getContext(), mData.mPlaylistId);
                        }
                        break;
                    case R.id.card_menu_add_queue:
                        //TODO we have the songs already just get the list from them.
                        long[] list = null;
                        if (mData.mPlaylistId == -1) {
                            list = MusicUtils.getSongListForFavorites(getContext());
                        } else if (mData.mPlaylistId == -2) {
                            list = MusicUtils.getSongListForLastAdded(getContext());
                        } else {
                            list = MusicUtils.getSongListForPlaylist(getContext(),
                                    mData.mPlaylistId);
                        }
                        MusicUtils.addToQueue(getContext(), list);
                        break;
                    case R.id.card_menu_rename:
                        RenamePlaylist.getInstance(mData.mPlaylistId).show(
                                ((FragmentActivity) getContext()).getSupportFragmentManager(),
                                "RenameDialog");
                        break;
                    case R.id.card_menu_delete:
                        new AlertDialog.Builder(getContext())
                                .setTitle(getContext().getString(R.string.delete_dialog_title, mData.mPlaylistName))
                                .setPositiveButton(R.string.context_menu_delete, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialog, final int which) {
                                        final Uri mUri = ContentUris.withAppendedId(
                                                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                                                mData.mPlaylistId);
                                        getContext().getContentResolver().delete(mUri, null, null);
                                        MusicUtils.refresh();
                                    }
                                })
                                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialog, final int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .setMessage(R.string.cannot_be_undone)
                                .create()
                                .show();
                        break;
                }
            }
        };
    }

}