
package ch.epfl.unison.data;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import ch.epfl.unison.Const;
import ch.epfl.unison.Uutils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * Utility to handle interactions with Original code from MarkG on <a
 * href="http://stackoverflow.com/questions /10969281/how-to-create-a-new
 * -playlist-using-contentresolver">http://stackoverflow.com/questions
 * /10969281/how-to-create-a-new-playlist-using-contentresolver</a>.
 * 
 * @author marc
 */
final class AndroidDB {

    private static final String TAG = "ch.epfl.unison.AndroidDB";

    private static final String[] PROJECTION_PLAYLIST = new String[] {
            MediaStore.Audio.Playlists._ID,
            MediaStore.Audio.Playlists.NAME,
            MediaStore.Audio.Playlists.DATA
    };

    // Projection to get high water mark of PLAY_ORDER in a particular playlist
    private static final String[] PROJECTION_PLAYLISTMEMBERS_PLAYORDER = new String[] {
            MediaStore.Audio.Playlists.Members._ID,
            MediaStore.Audio.Playlists.Members.PLAY_ORDER
    };
    // Projection to get the list of song IDs to be added to a playlist
    private static final String[] PROJECTION_SONGS_ADDTOPLAYLIST = new String[] {
            MediaStore.Audio.Media._ID,
    };

    private AndroidDB() {
        // Not instanciable
    }

    /**
     * WORK IN PROGRESS. <br />
     * The local android playlist id is stored directly in the given pl object,
     * or -1 if an error occured.
     * 
     * @param cr The contect resolver based on the context of the caller
     *            activity
     * @param pl The playlist to store in android databse
     * @return local playlist id
     */
    static long insert(ContentResolver cr, PlaylistItem pl) {
        // Add the playlist to the android dabase
        long timestamp = System.currentTimeMillis();
        ContentValues mValues = new ContentValues();
        mValues.put(MediaStore.Audio.Playlists.NAME, pl.getTitle());
        mValues.put(MediaStore.Audio.Playlists.DATE_ADDED, timestamp);
        mValues.put(MediaStore.Audio.Playlists.DATE_MODIFIED, timestamp);

        Uri uri = cr.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mValues);
        long playlistId = Uutils.lastInsertId(uri);
        pl.setLocalId(playlistId);
        if (playlistId >= 0) {

            Log.d(TAG, "PLAYLIST_ADD - mPlaylistId: " + playlistId + "  mUri: " + uri.toString());
            // Add tracks to the new playlist
            uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);

            insertTracks(cr, pl);
            pl.setDateModified(timestamp);
        } else {
            throw new SQLiteException("Playlists " + pl.toString() + " could not be inserted to "
                    + MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI);
        }
        return playlistId;
    }

    /**
     * WORK IN PROGRESS.<br />
     * Creates a new playlist, doesn't check if another playlist with same title
     * exists.
     * 
     * @param resolver
     * @param playlistId
     * @param c
     * @return DATE_MODIFIED
     */
    private static long insertTracks(ContentResolver resolver, PlaylistItem pl) {
        ContentResolver mCR = resolver;
        ContentProviderClient mCRC = null;
        try {
            long mPlaylistId = pl.getLocalId();
            Uri mUri = MediaStore.Audio.Playlists.Members.getContentUri("external", mPlaylistId);

            mCRC = mCR.acquireContentProviderClient(mUri);
            ContentValues values = new ContentValues();
            int tracks = pl.getTracks().size();
            int percent = 0;
            int i = 0;
            // TODO improve: bulk insertion instead of sequential insertion
            Iterator<TrackItem> it = pl.getTracks().iterator();
            while (it.hasNext()) {
                // Don't pollute with progress messages..has to be at least 1%
                // increments
                int temp = (i * Const.Integers.HUNDRED) / (tracks);
                if (temp > percent) {
                    percent = temp;
                    // publishProgress(mPercent);
                }
                i++;
                values.clear();
                TrackItem mi = it.next();
                values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, mi.localId);
                values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, mi.playOrder);
                Uri insertUri = mCRC.insert(mUri, values);
                Log.d(TAG,
                        "addSongsInCursorToPlaylist -Adding AudioID: "
                                + Uutils.lastInsertId(insertUri)
                                + " with Uri : " + insertUri
                                + " to Uri: " + mUri.toString());
            }
            mCRC.release();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        // TODO return DATE_MODIFIED (to update GS app DB)
        return 0;
    }

    static int delete(ContentResolver resolver, PlaylistItem pl) {
        int res = resolver.delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                MediaStore.Audio.Playlists._ID + " = ? ", new String[] {
                    String.valueOf(pl.getLocalId())
                });
        if (res < 1) {
            throw new SQLiteException("Playlist with _ID " + pl.getLocalId()
                    + " could not be removed from "
                    + MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI);
        }
        return res;
    }

    /**
     * Inspiration found on <a
     * href=http://stackoverflow.com/questions/7774384/get-next-previous
     * -song-from
     * -android-playlist>http://stackoverflow.com/questions/7774384/get
     * -next-previous -song-from-android-playlist</a>.
     * 
     * @param resolver
     * @param pl
     */
    static void getTracks(ContentResolver resolver, PlaylistItem pl) {
        String[] projection = new String[] {
                // MediaStore.Audio.Playlists.Members.PLAYLIST_ID,
                MediaStore.Audio.Playlists.Members.AUDIO_ID,
                MediaStore.Audio.Playlists.Members.ARTIST,
                MediaStore.Audio.Playlists.Members.TITLE,
                // MediaStore.Audio.Media.DATA,
                // MediaStore.Audio.Media.ALBUM,
                // MediaStore.Audio.Playlists.Members.AUDIO_ID,
                MediaStore.Audio.Playlists.Members.PLAY_ORDER
        };
        // Sort the tracks by play_order
        Cursor cur = resolver.query(getPlaylistMembersUri(pl.getLocalId()),
                projection,
                null,
                null,
                MediaStore.Audio.Playlists.Members.PLAY_ORDER + " ASC ");
        if (cur != null & cur.moveToFirst()) {
            LinkedList<TrackItem> mTracks = new LinkedList<TrackItem>();
            int colId = cur.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID);
            int colTitle = cur.getColumnIndex(MediaStore.Audio.Playlists.Members.TITLE);
            int colArtist = cur.getColumnIndex(MediaStore.Audio.Playlists.Members.ARTIST);
            int colPlayOrder = cur.getColumnIndex(MediaStore.Audio.Playlists.Members.PLAY_ORDER);
            do {
                mTracks.add(new TrackItem(cur.getLong(colId), cur.getString(colArtist),
                        cur.getString(colTitle), cur.getInt(colPlayOrder)));
            } while (cur.moveToNext());
            pl.setTracks(mTracks);
            cur.close();
        }
    }

    static int getTracksCount(ContentResolver resolver, long playlistId) {
        String[] projection = new String[] {
                MediaStore.Audio.Playlists.Members.AUDIO_ID
        };
        Cursor cur = resolver.query(getPlaylistMembersUri(playlistId),
                projection,
                null,
                null,
                null);
        int count = 0;
        if (cur != null) {
            count = cur.getCount();
            cur.close();
        }
        return count;
    }

    /**
     * Tracks are sorted by ascending play_order.
     * 
     * @param resolver
     * @return
     */
    static Set<PlaylistItem> getPlaylists(ContentResolver resolver) {
        Set<PlaylistItem> set = new HashSet<PlaylistItem>();
        String[] columns = {
                MediaStore.Audio.Playlists._ID,
                MediaStore.Audio.Playlists.NAME,
                MediaStore.Audio.Playlists.DATE_MODIFIED
        };
        Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        Cursor cur = resolver.query(uri, columns,
                // MediaStore.Audio.Playlists.CONTENT_TYPE +
                // " = vnd.android.cursor.dir/playlist",
                null,
                null, null);

        if (cur != null && cur.moveToFirst()) {
            int colId = cur.getColumnIndex(MediaStore.Audio.Playlists._ID);
            int colName = cur.getColumnIndex(MediaStore.Audio.Playlists.NAME);
            int colDateModified = cur.getColumnIndex(MediaStore.Audio.Playlists.DATE_MODIFIED);
            do {
                PlaylistItem pl = new PlaylistItem.Builder().plId(cur.getLong(colId))
                        .title(cur.getString(colName)).modified(cur.getLong(colDateModified)).build();

                getTracks(resolver, pl);
                set.add(pl);
            } while (cur.moveToNext());
            cur.close();
        }
        return set;
    }

    static Uri getPlaylistMembersUri(long playlistId) {
        return MediaStore.Audio.Playlists.Members
                .getContentUri("external", playlistId).buildUpon().build();
    }

}
