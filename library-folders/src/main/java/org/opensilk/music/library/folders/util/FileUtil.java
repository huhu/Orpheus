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

package org.opensilk.music.library.folders.util;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.opensilk.music.api.model.Folder;
import org.opensilk.music.api.model.Song;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by drew on 11/13/14.
 */
public class FileUtil {
    private static final boolean DUMPSTACKS = true;

    //Library identities
    public static final String PRIMARY_STORAGE_ID = "0";
    public static final String SECONDARY_STORAGE_ID = "1";

    public static final File PRIMARY_STORAGE_DIR;
    public static final File SECONDARY_STORAGE_DIR;

    public static final Uri BASE_ARTWORK_URI;
    public static final String[] SONG_PROJECTION;
    public static final String[] SONG_ALBUM_PROJECTION;
    public static final String[] MEDIA_TYPE_PROJECTION;

    public static final String SONG_SELECTION;
    public static final String SONG_ALBUM_SELECTION;
    public static final String MEDIA_TYPE_SELECTION;

    private static final DateFormat sDateFormat;

    static {
        PRIMARY_STORAGE_DIR = Environment.getExternalStorageDirectory();
        SECONDARY_STORAGE_DIR = getSecondaryStorageDir();
        BASE_ARTWORK_URI = Uri.parse("content://media/external/audio/albumart");
        SONG_PROJECTION = new String[] {
                BaseColumns._ID,
                MediaStore.Audio.AudioColumns.DISPLAY_NAME, //Better sorting than TITLE
                MediaStore.Audio.AudioColumns.ARTIST,
                MediaStore.Audio.AudioColumns.ALBUM,
                MediaStore.Audio.AudioColumns.ALBUM_ID,
                MediaStore.Audio.AudioColumns.DURATION,
                MediaStore.Audio.AudioColumns.MIME_TYPE,
        };
        SONG_ALBUM_PROJECTION = new String[] {
                MediaStore.Audio.AlbumColumns.ARTIST
        };
        MEDIA_TYPE_PROJECTION = new String[] {
                MediaStore.Files.FileColumns.MEDIA_TYPE
        };
        SONG_SELECTION = MediaStore.Audio.AudioColumns.DATA+"=?";
        SONG_ALBUM_SELECTION = BaseColumns._ID+"=?";
        MEDIA_TYPE_SELECTION = MediaStore.Files.FileColumns.DATA+"=?";
        sDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    }

    public static String[] getStoragePaths(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                Field scu = Environment.class.getDeclaredField("sCurrentUser");
                scu.setAccessible(true);
                Object cu = scu.get(null);
                Class<?> ue = Class.forName("android.os.Environment$UserEnvironment");
                Method getExternalDirsForApp = ue.getDeclaredMethod("getExternalDirsForApp");
                Object[] dirs = (Object[]) getExternalDirsForApp.invoke(cu);
                String[] paths = new String[dirs.length];
                for (int ii=0; ii<dirs.length; ii++) {
                    paths[ii] = ((File)dirs[ii]).getPath();
                }
                return paths;
            } catch (Exception e) {
                if (DUMPSTACKS) Log.e("FileUtil", "getStoragePaths", e);
            }
        } else {
            try {
                StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
                Method getVolumeList = StorageManager.class.getDeclaredMethod("getVolumeList");
                Class<?> volume = Class.forName("android.os.storage.StorageVolume");
                Method getPath =  volume.getDeclaredMethod("getPath");
                Object[] volumes = (Object[]) getVolumeList.invoke(sm);
                String[] paths = new String[volumes.length];
                for (int ii=0; ii<volumes.length; ii++) {
                    paths[ii] = (String) getPath.invoke(volumes[ii]);
                }
                return paths;
            } catch (Exception e) {
                if (DUMPSTACKS) Log.e("FileUtil", "getStoragePaths", e);
            }
        }
        Log.w("FileUtil", "Failed to get storage paths via reflection");
        return new String[]{Environment.getExternalStorageDirectory().getPath()};
    }

    @Nullable
    static File getSecondaryStorageDir() {
        File[] files = checkSecondaryStorage();
        if (files.length >= 2 && files[1].canRead()) {
            Log.i("Folders", "getSecondaryStorageDir() <- " + files[1]);
            return files[1];
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static File[] checkSecondaryStorage() {
        //Kitkat and above only
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return new File[0];
        try {
            Field f = Environment.class.getDeclaredField("sCurrentUser");
            f.setAccessible(true);
            Object o = f.get(null);
            Class c = Class.forName("android.os.Environment$UserEnvironment");
            Method m = c.getDeclaredMethod("getExternalDirsForApp");
            Object o2 = m.invoke(o);
            return (File[]) o2;
        } catch (Exception e) {
            if (DUMPSTACKS) Log.e("FileUtil", "checkSecondaryStorage", e);
            return new File[0];
        }
    }

    public static String getFileExtension(String name) {
        String ext;
        int lastDot = name.lastIndexOf('.');
        int secondLastDot = name.lastIndexOf('.', lastDot-1);
        if (secondLastDot > 0 ) { // Double extension
            ext = name.substring(secondLastDot + 1);
            if (!ext.startsWith("tar")) {
                ext = name.substring(lastDot + 1);
            }
        } else if (lastDot > 0) { // Single extension
            ext = name.substring(lastDot + 1);
        } else { // No extension
            ext = "";
        }
        return ext;
    }

    public static String getFileExtension(File f) {
        return getFileExtension(f.getName());
    }

    public static String guessMimeType(File f) {
        return guessMimeType(getFileExtension(f));
    }

    public static String guessMimeType(String ext) {
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        if (mimeType == null) {
            mimeType = "*/*";
        }
        return mimeType;
    }

    @NonNull
    public static String formatDate(long ms) {
        return sDateFormat.format(new Date(ms));
    }

    /*
     * Android external storage paths have changed at least a dozen times since
     * ive been watching so we use relative paths for ids so we don't get screwed
     * up when it changes again
     */
    @NonNull
    public static String toRelativePath(File base, File f) {
        String p = f.getAbsolutePath().replace(base.getAbsolutePath(), "");
        return !p.startsWith("/") ? p : p.substring(1);
    }

    @NonNull
    public static Folder makeFolder(File base, File dir) {
        final String[] children = dir.list();
        return new Folder.Builder()
                .setIdentity(toRelativePath(base, dir))
                .setName(dir.getName())
                .setChildCount(children != null ? children.length : 0)
                .setDate(formatDate(dir.lastModified()))
                .build();
    }

    @NonNull
    public static Song makeSong(Context context, File base, File f) {
        Cursor c = null;
        Cursor c2 = null;
        try {
            c = context.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    SONG_PROJECTION,
                    SONG_SELECTION,
                    new String[]{f.getAbsolutePath()},
                    null
            );
            c.moveToFirst();
            c2 = context.getContentResolver().query(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    SONG_ALBUM_PROJECTION,
                    SONG_ALBUM_SELECTION,
                    new String[]{c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))},
                    null
            );
            c2.moveToFirst();
            return new Song.Builder()
                    .setIdentity(toRelativePath(base, f))
                    .setName(c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME)))
                    .setArtistName(c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST)))
                    .setAlbumName(c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM)))
                    .setAlbumIdentity(c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID)))
                    .setDuration(c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)) / 1000)
                    .setMimeType(c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.MIME_TYPE)))
                    .setDataUri(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID))))
                    .setArtworkUri(ContentUris.withAppendedId(BASE_ARTWORK_URI,
                            c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))))
                    .setAlbumArtistName(c2.getString(c2.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.ARTIST)))
                    .build();
        } catch (Exception e) {
            if (DUMPSTACKS) Log.e("FileUtil", "makeSong", e);
            return new Song.Builder()
                    .setIdentity(toRelativePath(base, f))
                    .setName(f.getName())
                    .setMimeType(guessMimeType(f))
                    .setDataUri(Uri.fromFile(f))
                    .build();
        } finally {
            closeQuietly(c);
            closeQuietly(c2);
        }
    }

    public static boolean isAudio(Context context, File f) {
        Cursor c = null;
        try {
            c = context.getContentResolver().query(
                    MediaStore.Files.getContentUri("external"),
                    MEDIA_TYPE_PROJECTION,
                    MEDIA_TYPE_SELECTION,
                    new String[]{f.getAbsolutePath()},
                    null);
            c.moveToFirst();
            int mediaType = c.getInt(0);
            return mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO;
        } catch (Exception e) {
            if (DUMPSTACKS) Log.e("FileUtil", "isAudio", e);
            String mime = guessMimeType(f);
            return mime.contains("audio") || mime.equals("application/ogg");
        } finally {
            closeQuietly(c);
        }
    }

    static void closeQuietly(Cursor c) {
        try {
            if (c != null) c.close();
        } catch (Exception ignored) { }
    }

}