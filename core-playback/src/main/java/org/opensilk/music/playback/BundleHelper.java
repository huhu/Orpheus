/*
 * Copyright (c) 2015 OpenSilk Productions LLC
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

package org.opensilk.music.playback;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by drew on 4/24/15.
 */
public class BundleHelper {
    public static final String INT_ARG = "intarg";
    public static final String LIST_ART = "listarg";
    public static final String URI_ARG = "uriarg";
    public static final String STRING_ARG = "stringarg";

    public static int getInt(Bundle b) {
        return b.getInt(INT_ARG);
    }

    public static <T extends Parcelable> List<T> getList(Bundle b) {
        b.setClassLoader(BundleHelper.class.getClassLoader());
        return b.getParcelableArrayList(LIST_ART);
    }

    public static Uri getUri(Bundle b) {
        b.setClassLoader(BundleHelper.class.getClassLoader());
        return b.getParcelable(URI_ARG);
    }

    public static String getString(Bundle b) {
        return b.getString(STRING_ARG);
    }

    public static BundleHelper.Builder builder() {
        return new BundleHelper.Builder(new Bundle());
    }

    /**
     * Wraps bundle for chaining.
     */
    public static class Builder {
        final Bundle b;

        private Builder(Bundle b) {
            this.b = b;
        }

        public Builder putInt(int val) {
            b.putInt(INT_ARG, val);
            return this;
        }

        public <T extends Parcelable> Builder putList(List<T> list) {
            b.putParcelableArrayList(LIST_ART, new ArrayList<Parcelable>(list));
            return this;
        }

        public Builder putUri(Uri uri) {
            b.putParcelable(URI_ARG, uri);
            return this;
        }

        public Builder putString(String string) {
            b.putString(STRING_ARG, string);
            return this;
        }

        public Bundle get() {
            return b;
        }
    }




}
