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

package org.opensilk.music.library.mediastore.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.webkit.MimeTypeMap;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.music.library.mediastore.BuildConfig;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Track;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import timber.log.Timber;

import static org.opensilk.music.library.mediastore.util.CursorHelpers.generateArtworkUri;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.generateDataUri;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.getLongOrZero;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.getStringOrNull;

/**
 * Created by drew on 4/28/15.
 */
public class FilesUtil {
    public static final boolean DUMPSTACKS = BuildConfig.DEBUG;

    private static final DateFormat sDateFormat;

    static {
        sDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
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
     * Android external storage paths have changed numerous times since
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
        return Folder.builder()
                .setIdentity(toRelativePath(base, dir))
                .setName(dir.getName())
                .setChildCount(children != null ? children.length : 0)
                .setDate(formatDate(dir.lastModified()))
                .build();
    }

    public static Track makeTrackFromFile(File base, File f) {
        return Track.builder()
                .setIdentity(toRelativePath(base, f))
                .setName(f.getName())
                .setMimeType(guessMimeType(f))
                .setDataUri(Uri.fromFile(f))
                .build();
    }

    @NonNull
    public static List<File> filterAudioFiles(Context context, List<File> files) {
        if (files.size() == 0) {
            return Collections.emptyList();
        }
        //Map for cursor
        final HashMap<String, File> pathMap = new HashMap<>();
        //The returned list
        final List<File> audioFiles = new ArrayList<>();

        //Build the selection
        final int size = files.size();
        final StringBuilder selection = new StringBuilder();
        selection.append(MediaStore.Files.FileColumns.DATA + " IN (");
        for (int i = 0; i < size; i++) {
            final File f = files.get(i);
            final String path = f.getAbsolutePath();
            pathMap.put(path, f); //Add file to map while where iterating
            //TODO it would probably be better to use selectionArgs
            selection.append("'").append(StringUtils.replace(path, "'", "''")).append("'");
            if (i < size - 1) {
                selection.append(",");
            }
        }
        selection.append(")");
        Cursor c = null;
        try {
            c = context.getContentResolver().query(
                    MediaStore.Files.getContentUri("external"),
                    Projections.MEDIA_TYPE_PROJECTION,
                    selection.toString(),
                    null,
                    null);
            if (c != null && c.moveToFirst()) {
                do {
                    final int mediaType = c.getInt(0);
                    final String path = c.getString(1);
                    final File f = pathMap.remove(path);
                    if (f != null && mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO) {
                        audioFiles.add(f);
                    } //else throw away
                } while (c.moveToNext());
            }
            //either the query failed or the cursor didn't contain all the files we asked for.
            if (!pathMap.isEmpty()) {
                Timber.w("%d files weren't found in mediastore. Best guessing mime type", pathMap.size());
                for (File f : pathMap.values()) {
                    final String mime = guessMimeType(f);
                    if (StringUtils.contains(mime, "audio") || "application/ogg".equals(mime)) {
                        audioFiles.add(f);
                    }
                }
            }
        } catch (Exception e) {
            if (FilesUtil.DUMPSTACKS) Timber.e(e, "filterAudioFiles");
        } finally {
            closeQuietly(c);
        }
        return audioFiles;
    }

    @NonNull
    public static List<Track> convertAudioFilesToTracks(Context context, File base, List<File> audioFiles) {
        if (audioFiles.size() == 0) {
            return Collections.emptyList();
        }

        final List<Track> trackList = new ArrayList<>(audioFiles.size());

        Cursor c = null;
        try {
            final HashMap<String, File> pathMap = new HashMap<>();

            //Build the selection
            final int size = audioFiles.size();
            final StringBuilder selection = new StringBuilder();
            selection.append(MediaStore.Audio.AudioColumns.DATA + " IN (");
            for (int i = 0; i < size; i++) {
                final File f = audioFiles.get(i);
                final String path = f.getAbsolutePath();
                pathMap.put(path, f); //Add file to map while where iterating
                //TODO it would probably be better to use selectionArgs
                selection.append("'").append(StringUtils.replace(path, "'", "''")).append("'");
                if (i < size - 1) {
                    selection.append(",");
                }
            }
            selection.append(")");

            //make query
            c = context.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    Projections.SONG_FILE,
                    selection.toString(),
                    null,
                    null);
            if (c != null && c.moveToFirst()) {
                do {
                    final File f = pathMap.remove(c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATA)));
                    if (f != null) {
                        try {
                            Track.Builder tb = Track.builder()
                                    .setIdentity(c.getString(c.getColumnIndexOrThrow(BaseColumns._ID)))
                                    .setName(getStringOrNull(c, MediaStore.Audio.AudioColumns.DISPLAY_NAME))
                                    .setArtistName(getStringOrNull(c, MediaStore.Audio.AudioColumns.ARTIST))
                                    .setAlbumName(getStringOrNull(c, MediaStore.Audio.AudioColumns.ALBUM))
                                    .setAlbumArtistName(getStringOrNull(c, "album_artist"))
                                    .setAlbumIdentity(getStringOrNull(c, MediaStore.Audio.AudioColumns.ALBUM_ID))
                                    .setDuration((int) (getLongOrZero(c, MediaStore.Audio.AudioColumns.DURATION) / 1000))
                                    .setMimeType(getStringOrNull(c, MediaStore.Audio.AudioColumns.MIME_TYPE))
                                    .setDataUri(generateDataUri(c.getString(c.getColumnIndexOrThrow(BaseColumns._ID))))
                                    .setArtworkUri(generateArtworkUri(
                                            c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))));
                            trackList.add(tb.build());
                        } catch (IllegalArgumentException ignored) {}
                    }
                } while (c.moveToNext());
            }
            if (!pathMap.isEmpty()) {
                Timber.w("%d audioFiles didn't make the cursor", pathMap.size());
                for (File f : pathMap.values()) {
                    trackList.add(makeTrackFromFile(base, f));
                }
            }
        } catch (Exception e) {
            if (DUMPSTACKS) Timber.e(e, "convertAudioFilesToTracks");
        } finally {
            closeQuietly(c);
        }
        return trackList;
    }

    public static void closeQuietly(Cursor c) {
        try {
            if (c != null) c.close();
        } catch (Exception ignored) { }
    }
}
