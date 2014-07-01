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

package org.opensilk.music.ui.home;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CursorAdapter;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.SortOrder;

import org.opensilk.music.ui.home.adapter.AlbumGridAdapter;
import org.opensilk.music.ui.home.adapter.AlbumListAdapter;
import org.opensilk.music.ui.home.loader.AlbumLoader;
import org.opensilk.music.ui.modules.DrawerHelper;
import org.opensilk.silkdagger.qualifier.ForActivity;

import javax.inject.Inject;

import static com.andrew.apollo.utils.PreferenceUtils.ALBUM_LAYOUT;

/**
 * Created by drew on 6/28/14.
 */
public class AlbumFragment extends BasePagerFragment {

    @Inject @ForActivity
    DrawerHelper mDrawerHelper;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!mDrawerHelper.isDrawerOpen()) {
            inflater.inflate(R.menu.album_sort_by, menu);
            inflater.inflate(R.menu.view_as, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sort_by_az:
                mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_A_Z);
                refresh();
                return true;
            case R.id.menu_sort_by_za:
                mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_Z_A);
                refresh();
                return true;
            case R.id.menu_sort_by_artist:
                mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_ARTIST);
                refresh();
                return true;
            case R.id.menu_sort_by_year:
                mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_YEAR);
                refresh();
                return true;
            case R.id.menu_sort_by_number_of_songs:
                mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_NUMBER_OF_SONGS);
                refresh();
                return true;
            case R.id.menu_view_as_simple:
                mPreferences.setAlbumLayout("simple");
                NavUtils.goHome(getActivity());
                return true;
            case R.id.menu_view_as_grid:
                mPreferences.setAlbumLayout("grid");
                NavUtils.goHome(getActivity());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new AlbumLoader(getActivity());
    }

    @Override
    protected CursorAdapter createAdapter() {
        if (wantGridView()) {
            return new AlbumGridAdapter(getActivity(), mInjector);
        } else {
            return new AlbumListAdapter(getActivity(), mInjector);
        }
    }

    @Override
    public boolean wantGridView() {
        return !mPreferences.isSimpleLayout(ALBUM_LAYOUT, getActivity());
    }

}