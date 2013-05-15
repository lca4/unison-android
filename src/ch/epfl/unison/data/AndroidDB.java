
package ch.epfl.unison.data;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.location.Address;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Playlists;
import android.util.Log;

import java.util.Iterator;

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
    static int insertToAndroid(ContentResolver cr, Playlist pl) {
        // Add the playlist to the android dabase
        ContentValues mValues = new ContentValues();
        mValues.put(MediaStore.Audio.Playlists.NAME, pl.getTitle());
        mValues.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis());
        mValues.put(MediaStore.Audio.Playlists.DATE_MODIFIED, System.currentTimeMillis());
        // mValues.put(MediaStore.Audio.Playlists.CONTENT_TYPE,
        // "vnd.android.cursor.dir/playlist");
        // mValues.put(MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE,
        // "vnd.android.cursor.item/playlist" );
        // mValues.put(MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER, "name");
        // ContentResolver mCR = resolver.getContentResolver();
        Uri uri = cr.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mValues);
        int playlistId = -1;
        if (uri != null) {
            Cursor c = cr.query(uri, PROJECTION_PLAYLIST, null, null, null);
            if (c != null) {
                // Save the newly created ID so it can be selected.
                playlistId = c.getInt(c.getColumnIndex(MediaStore.Audio.Playlists._ID));
                c.close();
            }
            Log.d(TAG, "PLAYLIST_ADD - mPlaylistId: " + playlistId + "  mUri: " + uri.toString());
            pl.setLocalId(playlistId);
            // Add tracks to the new playlist
            uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
            c = cr.query(uri, PROJECTION_PLAYLIST, null, null, null);
            if (c != null) {
                addSongsInCursorToPlaylist(cr, pl);
                c.close();
            }
        }
        return playlistId;
    }

    /**
     * WORK IN PROGRESS. To be adapted to our playlists.
     * 
     * @param resolver
     * @param playlistId
     * @param c
     */
    private static void addSongsInCursorToPlaylist(ContentResolver resolver, Playlist pl) {
        int mIdCol;
        int mCount;
        int mPercent = 0;
        ContentResolver mCR = resolver;
        ContentProviderClient mCRC = null;
        try {
//            mCount = c.getCount();
//            mIdCol = c.getColumnIndex(MediaStore.Audio.Media._ID);
            ContentValues[] mInsertList = new ContentValues[1];
//            mInsertList[0] = new ContentValues();
            int mPlaylistId = pl.getLocalId();
            Uri mUri = MediaStore.Audio.Playlists.Members.getContentUri("external", mPlaylistId);
            Cursor c2 = mCR.query(mUri,
                    PROJECTION_PLAYLISTMEMBERS_PLAYORDER, null, null,
                    MediaStore.Audio.Playlists.Members.PLAY_ORDER + " DESC ");
            int mPlayOrder = 1;
            if (c2 != null) {
                if (c2.moveToFirst()) {
                    mPlayOrder = (c2.getInt(c2
                            .getColumnIndex(MediaStore.Audio.Playlists.Members.PLAY_ORDER))) + 1;
                }
                c2.close();
            }
            mCRC = mCR.acquireContentProviderClient(mUri);

            Log.d(TAG, "addSongsInCursorToPlaylist -Content Uri: " + mUri.toString()
                    + "  PlaylistID: " + mPlaylistId + " mPlayOrder: " + mPlayOrder);

            ContentValues candidate = new ContentValues();
            Iterator<MusicItem> it = pl.getTracks().iterator();
            if (it.hasNext()) {
                // Don't pollute with progress messages..has to be at least
                // 1% increments
//                    int mTemp = (i * 100) / (mCount);
//                    if (mTemp > mPercent) {
//                        mPercent = mTemp;
//                        // publishProgress(mPercent);
//                    }
                candidate.clear();
                MusicItem mi = it.next();
                candidate.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, mi.localId);
                candidate.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, mi.playOrder);
                mCRC.insert(mUri, candidate);
                mCRC.release();
                Log.d(TAG,
                        "addSongsInCursorToPlaylist -Adding AudioID: " + 0
                        + " to Uri: " + mUri.toString());
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }

}
