/*
 * Copyright (c) 2014 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.ui2.gallery;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.util.ViewStateSaver;

import java.util.List;

import mortar.ViewPresenter;
import rx.Subscription;
import rx.functions.Func1;

/**
 * Created by drew on 10/19/14.
 */
public abstract class BasePresenter<T> extends ViewPresenter<RecyclerView> implements HasOptionsMenu {

    protected final AppPreferences preferences;

    protected List<T> items;
    protected Subscription subscription;
    protected ActionBarOwner.MenuConfig actionBarMenu;

    public BasePresenter(AppPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        setupRecyclerView();
        if (items != null && !items.isEmpty()) {
            setAdapter(newAdapter(items));
            if (savedInstanceState != null) {
                ViewStateSaver.restore(getView(), savedInstanceState, "recyclerview");
            }
        } else if (subscription == null || subscription.isUnsubscribed()) {
            load();
        }
    }

    @Override
    protected void onSave(Bundle outState) {
        super.onSave(outState);
        RecyclerView v = getView();
        if (v != null) {
            ViewStateSaver.save(v, outState, "recyclerview");
        }
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        if (subscription != null) subscription.unsubscribe();
    }

    // Init the recyclerview
    protected void setupRecyclerView() {
        RecyclerView v = getView();
        if (v == null) return;
        v.setHasFixedSize(!isStaggered());
        v.setLayoutManager(getLayoutManager(v.getContext()));
    }

    // reset the recyclerview for eg layoutmanager change
    protected void resetRecyclerView() {
        RecyclerView v = getView();
        if (v == null) return;
        v.getRecycledViewPool().clear();
        setupRecyclerView();
        if (items != null && !items.isEmpty()) {
            setAdapter(newAdapter(items));
        } else {
            v.setAdapter(null);
            reload();
        }
    }

    // make a new adapter
    protected abstract BaseAdapter<T> newAdapter(List<T> items);
    // start the loader
    protected abstract void load();

    // cancels any ongoing load and starts a new one
    protected void reload() {
        if (subscription != null) subscription.unsubscribe();
        load();
    }

    // sets the adapter for recycler view
    protected boolean setAdapter(BaseAdapter<T> adapter) {
        RecyclerView v = getView();
        if (v == null) return false;
        boolean isGrid = isGrid() || isStaggered();
        adapter.setGridStyle(isGrid);
        v.setAdapter(adapter);
        return true;
    }

    // updates this.items incase we are inthe middel of config change
    // and trys to set the adapter, if that fails it will be set
    // on the next onLoad
    protected void addItems(List<T> items) {
        this.items = items;
        setAdapter(newAdapter(items));
    }

    // adds a single item to the adapter
    protected void addItem(T item) {
        RecyclerView v = getView();
        if (v == null) return;
        BaseAdapter<T> adapter = (BaseAdapter<T>) v.getAdapter();
        adapter.add(item);
    }

    protected boolean isGrid() {
        return false;
    }

    protected boolean isStaggered() {
        return false;
    }

    protected RecyclerView.LayoutManager getLayoutManager(Context context) {
        if (isStaggered()) {
            return makeStaggerdLayoutManager(context);
        } else if (isGrid()) {
            return makeGridLayoutManager(context);
        } else {
            return makeListLayoutManager(context);
        }
    }

    protected RecyclerView.LayoutManager makeStaggerdLayoutManager(Context context) {
        int numCols = context.getResources().getInteger(R.integer.grid_columns);
        return new StaggeredGridLayoutManager(numCols, StaggeredGridLayoutManager.VERTICAL);
    }

    protected RecyclerView.LayoutManager makeGridLayoutManager(Context context) {
        int numCols = context.getResources().getInteger(R.integer.grid_columns);
        return new GridLayoutManager(context, numCols, GridLayoutManager.VERTICAL, false);
    }

    protected RecyclerView.LayoutManager makeListLayoutManager(Context context) {
        return new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
    }

}
