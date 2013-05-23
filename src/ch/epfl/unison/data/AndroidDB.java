
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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;

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
    static int insert(ContentResolver cr, Playlist pl) {
        // Add the playlist to the android dabase
        ContentValues mValues = new ContentValues();
        mValues.put(MediaStore.Audio.Playlists.NAME, pl.getTitle());
        mValues.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis());
        mValues.put(MediaStore.Audio.Playlists.DATE_MODIFIED, System.currentTimeMillis());

        Uri uri = cr.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mValues);
        int playlistId = Uutils.lastInsert(uri);
        pl.setLocalId(playlistId);
        if (playlistId >= 0) {

            Log.d(TAG, "PLAYLIST_ADD - mPlaylistId: " + playlistId + "  mUri: " + uri.toString());
            // Add tracks to the new playlist
            uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);

            insertTracks(cr, pl);

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
     */
    private static void insertTracks(ContentResolver resolver, Playlist pl) {
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
            Iterator<MusicItem> it = pl.getTracks().iterator();
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
                MusicItem mi = it.next();
                values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, mi.localId);
                values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, mi.playOrder);
                Uri insertUri = mCRC.insert(mUri, values);
                Log.d(TAG,
                        "addSongsInCursorToPlaylist -Adding AudioID: "
                                + Uutils.lastInsert(insertUri)
                                + " with Uri : " + insertUri
                                + " to Uri: " + mUri.toString());
            }
            mCRC.release();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    static int delete(ContentResolver resolver, Playlist pl) {
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
     * Inspiration found on
     * <a href=http://stackoverflow.com/questions/7774384/get-next-previous
     * -song-from-android-playlist>http://stackoverflow.com/questions/7774384/get-next-previous
     * -song-from-android-playlist</a>.
     * 
     * @param resolver
     * @param pl
     */
    static void getTracks(ContentResolver resolver, Playlist pl) {
        String[] projection = new String[] {
                // MediaStore.Audio.Playlists.Members.PLAYLIST_ID,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                // MediaStore.Audio.Media.DATA,
                // MediaStore.Audio.Media.ALBUM,
                // MediaStore.Audio.Playlists.Members.AUDIO_ID,
                MediaStore.Audio.Playlists.Members.PLAY_ORDER
        };
        Uri contentUri = MediaStore.Audio.Playlists.Members
                .getContentUri("external", pl.getLocalId()).buildUpon().build();
        Cursor cur = resolver.query(contentUri,
                projection,
                null, null, MediaStore.Audio.Playlists.Members.PLAY_ORDER);
        if (cur != null) {
            LinkedList<MusicItem> mTracks = new LinkedList<MusicItem>();
            int colId = cur.getColumnIndex(MediaStore.Audio.Media._ID);
            int colTitle = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int colArtist = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int colPlayOrder = cur.getColumnIndex(MediaStore.Audio.Playlists.Members.PLAY_ORDER);
            do {
                mTracks.add(new MusicItem(cur.getInt(colId), cur.getString(colArtist),
                        cur.getString(colTitle), cur.getInt(colPlayOrder)));
            } while (cur.moveToNext());
        }
    }

}
