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

package com.andrew.apollo.meta;

import android.content.ComponentName;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by drew on 7/2/14.
 */
public class LibraryInfo implements Parcelable {

    public final String libraryId;
    public final ComponentName libraryComponent;
    public final String currentFolderId;

    public LibraryInfo(String libraryId, ComponentName libraryComponent, String currentFolderId) {
        this.libraryId = libraryId;
        this.libraryComponent = libraryComponent;
        this.currentFolderId = currentFolderId;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(libraryId);
        dest.writeString(libraryComponent.flattenToString());
        dest.writeString(currentFolderId);
    }

    private static LibraryInfo fromParcel(Parcel source) {
        final String id = source.readString();
        final ComponentName cmpnt = ComponentName.unflattenFromString(source.readString());
        final String fid = source.readString();
        return new LibraryInfo(id, cmpnt, fid);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LibraryInfo> CREATOR = new Creator<LibraryInfo>() {
        @Override
        public LibraryInfo createFromParcel(Parcel source) {
            return LibraryInfo.fromParcel(source);
        }

        @Override
        public LibraryInfo[] newArray(int size) {
            return new LibraryInfo[size];
        }
    };
}