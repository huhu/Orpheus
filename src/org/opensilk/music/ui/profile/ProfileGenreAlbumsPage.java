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

package org.opensilk.music.ui.profile;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import com.andrew.apollo.Config;
import com.andrew.apollo.model.Genre;

import org.opensilk.music.adapters.ProfileAlbumListCardCursorAdapter;
import org.opensilk.music.loaders.GenreAlbumCursorLoader;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 3/31/14.
 */
public class ProfileGenreAlbumsPage extends ProfileGenrePageBase implements LoaderManager.LoaderCallbacks<Cursor> {

    // Loader identifier
    protected static final int LOADER = 200001;

    // Adapter
    protected final CursorAdapter mAdapter;

    ProfileGenreAlbumsPage(ProfileGenreFragment fragment, Genre genre, ViewGroup container) {
        super(fragment, genre, container);
        mAdapter = new ProfileAlbumListCardCursorAdapter(fragment.getActivity());
        // set the adapter
        mListView.setAdapter(mAdapter);
        fragment.getLoaderManager().initLoader(LOADER, createLoaderArgs(), this);
    }

    protected Bundle createLoaderArgs() {
        final Bundle b = new Bundle();
        b.putLong(Config.ID, mGenre.mGenreId);
        return b;
    }

    @Override
    public void finish() {
        mHostFragment.getLoaderManager().destroyLoader(LOADER);
    }

    /*
     * Implement Loader callbacks
     */

    @Override
    @DebugLog
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == LOADER) {
            return new GenreAlbumCursorLoader(mHostFragment.getActivity(), args.getLong(Config.ID));
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == LOADER) {
            mAdapter.swapCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == LOADER) {
            mAdapter.swapCursor(null);
        }
    }

}
