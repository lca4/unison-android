
package ch.epfl.unison.data;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Utility to handle interactions with
 * Original code from MarkG on <a href="http://stackoverflow.com/questions
 *      /10969281/how-to-create-a-new
 *      -playlist-using-contentresolver">http://stackoverflow.com/questions
 *      /10969281/how-to-create-a-new-playlist-using-contentresolver</a>.
 *      
 * @author marc
 * 
 * 
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
     * WORK IN PROGRESS.
     * 
     * @param cr
     * @param pl
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
        Uri mUri = cr.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mValues);
        if (mUri != null) {
            int mPlaylistId = -1;
            Cursor c = cr.query(mUri, PROJECTION_PLAYLIST, null, null, null);
            if (c != null) {
                // Save the newly created ID so it can be selected.
                mPlaylistId = c.getInt(c.getColumnIndex(MediaStore.Audio.Playlists._ID));
                c.close();
            }
            Log.d(TAG, "PLAYLIST_ADD - mPlaylistId: " + mPlaylistId + "  mUri: " + mUri.toString());
        }

        // Insert data
        LinkedList<MusicItem> tracks = pl.getTracks();
        Iterator<MusicItem> it = tracks.iterator();
        while (it.hasNext()) {
            MusicItem mi = it.next();
            mValues = new ContentValues();
        }

        // Then set the android id to the pl object and store pl to the app
        // database

        return 0;
    }

    /**
     * WORK IN PROGRESS.
     * To be adapted to our playlists.
     * 
     * @param resolver
     * @param playlistId
     * @param c
     */
    private void addSongsInCursorToPlaylist(ContentResolver resolver, int playlistId, Cursor c) {
        int mIdCol;
        int mCount;
        int mPercent = 0;
//        ContentResolver mCR = mContext.getContentResolver();
        ContentResolver mCR = resolver;
        ContentProviderClient mCRC = null;
        try {
            mCount = c.getCount();
            mIdCol = c.getColumnIndex(MediaStore.Audio.Media._ID);
            ContentValues[] mInsertList = new ContentValues[1];
            mInsertList[0] = new ContentValues();
//            int mPlaylistId = mPrefs.getInt(AM.PLAYLIST_NOWPLAYING_ID, AM.PLAYLIST_ID_DEFAULT);
            int mPlaylistId = playlistId;
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
//            if (DBG.AUDIO) {
                Log.d(TAG, "addSongsInCursorToPlaylist -Content Uri: " + mUri.toString()
                        + "  PlaylistID: " + mPlaylistId + " mPlayOrder: " + mPlayOrder);
//            }
            for (int i = 0; i < mCount; i++) {
                if (c.moveToPosition(i)) {
                    // Don't pollute with progress messages..has to be at least
                    // 1% increments
                    int mTemp = (i * 100) / (mCount);
                    if (mTemp > mPercent) {
                        mPercent = mTemp;
//                        publishProgress(mPercent);
                    }
                    mInsertList[0].put(MediaStore.Audio.Playlists.Members.AUDIO_ID,
                            c.getLong(mIdCol));
                    mInsertList[0].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, mPlayOrder++);
                    mCR.insert(mUri, mInsertList[0]);
//                    if (DBG.AUDIO) {
                        Log.d(TAG,
                                "addSongsInCursorToPlaylist -Adding AudioID: " + c.getLong(mIdCol)
                                        + " to Uri: " + mUri.toString());
//                    }
                }
                mCRC.release();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }

}
