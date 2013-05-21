
package ch.epfl.unison.data;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import ch.epfl.unison.Const;
import ch.epfl.unison.Uutils;

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

            addTracksToPlaylist(cr, pl);

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
    private static void addTracksToPlaylist(ContentResolver resolver, Playlist pl) {
        ContentResolver mCR = resolver;
        ContentProviderClient mCRC = null;
        try {
            int mPlaylistId = pl.getLocalId();
            Uri mUri = MediaStore.Audio.Playlists.Members.getContentUri("external", mPlaylistId);

            mCRC = mCR.acquireContentProviderClient(mUri);
            ContentValues values = new ContentValues();
            int tracks = pl.getTracks().size();
            int percent = 0;
            int i = 0;
            // TODO improve: bulk insertion instead of sequential insertion
            Iterator<MusicItem> it = pl.getTracks().iterator();
            while (it.hasNext()) {
                // Don't pollute with progress messages..has to be at least 1% increments
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
        return resolver.delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                MediaStore.Audio.Playlists._ID + " = ? ", new String[] {
                    String.valueOf(pl.getLocalId())
                });
    }

}
