package ch.epfl.unison.data;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;

import ch.epfl.unison.AppData;
import ch.epfl.unison.Const;
import ch.epfl.unison.LibraryHelper;
import ch.epfl.unison.Const.PrefKeys;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.Request;
import ch.epfl.unison.api.UnisonAPI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author marc
 */
public class PlaylistLibraryService extends AbstractService {

    private static final String TAG = "ch.epfl.unison.PlaylistLibraryService";
    private static final int MIN_UPDATE_INTERVAL = 60 * 60 * 10;  // In seconds.
    private static final long MILLIS_IN_S = 1000L;  // Number of milliseconds in a second.

    public static final String ACTION_UPDATE = "ch.epfl.unison.action.UPDATE";
    public static final String ACTION_TRUNCATE = "ch.epfl.unison.action.TRUNCATE";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "starting the playlist library service");
        String action = intent.getAction();
        if (action.equals(ACTION_UPDATE)) {
            update();
        } else if (action.equals(ACTION_TRUNCATE)) {
            truncate();
        }
        return START_NOT_STICKY;
    }

    private void truncate() {
        Log.d(TAG, "truncating the user playlist library");
        UnisonDB mDB = new UnisonDB(this);
        mDB.truncate(Playlist.class);
    }

    /*
     * Don't do it now.
     */
    private void update() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // How many seconds elapsed since the last successful update ?
        long interval = (System.currentTimeMillis() / MILLIS_IN_S)
                - prefs.getLong(Const.PrefKeys.LASTUPDATE, -1);

        if (!isUpdating() && interval > MIN_UPDATE_INTERVAL) {
            setIsUpdating(true);
            UnisonDB mDB = new UnisonDB(this);
            if (mDB.isEmpty(Playlist.class)) {
                // If the DB is empty, just PUT all the tracks.
                Log.d(TAG, "uploading all the playlists");
                new Uploader().execute();
            } else {
                Log.d(TAG, "updating the library");
                new Updater().execute();
            }
        } else {
            Log.d(TAG, String.format("didn't update the library (interval: %d)", interval));
        }
    }

    /**
     * Abstract base class for synchronization tasks (either "truncate and upload"
     * or "update some deltas").
     */
    private abstract class LibraryTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPostExecute(Boolean isSuccessful) {
            setIsUpdating(false);

            if (isSuccessful) {
                SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences(PlaylistLibraryService.this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong("lastupdate", System.currentTimeMillis() / MILLIS_IN_S);
                editor.commit();
            }
        }

//        public Set<Playlist> getRealPlaylists() {
//            Set<Playlist> set = new HashSet<Playlist>();
//            String[] columns = {
//                    MediaStore.Audio.Playlists._ID,
//                    MediaStore.Audio.Playlists.NAME
//            };
//            Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
//            Cursor cur = PlaylistLibraryService.this.getContentResolver().query(uri, columns,
////                    MediaStore.Audio.Playlists.CONTENT_TYPE + " = vnd.android.cursor.dir/playlist",
//                    null,
//                    null, null);
//
//            if (cur != null && cur.moveToFirst()) {
//                int colId = cur.getColumnIndex(MediaStore.Audio.Playlists._ID);
//                int colName = cur.getColumnIndex(MediaStore.Audio.Playlists.NAME);
//                do {
//                    Playlist pl = new Playlist(cur.getInt(colId),
//                            cur.getString(colName));
//                    AndroidDB.getTracks(getContentResolver(), pl);
//                    set.add(pl);
//                } while (cur.moveToNext());
//                cur.close();
//            }
//            return set;
//        }
    }

    /**
     * If the local DB is already populated, we only send deltas to the server (i.e.
     * a list of tracks that were added and a list of tracks that were removed).
     */
    private class Updater extends LibraryTask {

        private List<JsonStruct.Delta> getDeltas(UnisonDB uDB) {
            // Setting up the expectations.
            Set<Playlist> expectation = (Set<Playlist>) uDB.getEntries(Playlist.class);
            Log.d(TAG, "number of OUR entries: " + expectation.size());

            // Take a hard look at the reality.
//            Set<Playlist> reality = getRealPlaylists();
            Set<Playlist> reality = AndroidDB.getPlaylists(getContentResolver());
            Log.d(TAG, "number of TRUE music entries: " + reality.size());

            
            
            // Trying to reconcile everyone.
            List<JsonStruct.Delta> deltas = new ArrayList<JsonStruct.Delta>();
            for (Playlist item : reality) {
                if (!expectation.contains(item)) {
                    Log.d(TAG, "Adding playlist: " + item.getTitle());
//                    deltas.add(new JsonStruct.Delta(JsonStruct.Delta.TYPE_PUT,
//                            item.localId, item.artist, item.title));  // Add item.
                }
            }
//            for (MusicItem item : expectation) {
//                if (!reality.contains(item)) {
//                    Log.d(TAG, "Removing track: " + item.title);
//                    deltas.add(new JsonStruct.Delta(JsonStruct.Delta.TYPE_DELETE,
//                            item.localId, item.artist, item.title));  // Delete item.
//                }
//            }
            Log.d(TAG, "number of deltas: " + deltas.size());
            return deltas;
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            UnisonDB uDB = new UnisonDB(PlaylistLibraryService.this);
            List<JsonStruct.Delta> deltas = getDeltas(uDB);

            // Sending the updates to the server.
            UnisonAPI api = AppData.getInstance(PlaylistLibraryService.this).getAPI();
            long uid = AppData.getInstance(PlaylistLibraryService.this).getUid();

            Request.Result<JsonStruct.Success> res = api.updateLibrarySync(uid, deltas);
            if (res.result == null) {
                if (res.error.hasJsonError()) {
                    Log.w(TAG, "couldn't send deltas to server: " + res.error.jsonError.message);
                } else {
                    Log.w(TAG, "couldn't send deltas to server.", res.error.error);
                }
                return false;
            }

//            // "Commiting" the changes locally.
//            for (JsonStruct.Delta delta : deltas) {
//                MusicItem item = new MusicItem(
//                        delta.entry.localId, delta.entry.artist, delta.entry.title);
//                if (delta.type.equals(JsonStruct.Delta.TYPE_PUT)) {
//                    helper.insert(item);
//                } else {  // TYPE_DELETE.
//                    helper.delete(item);
//                }
//            }

            return true;
        }
    }

    /**
     * If it's the first time that the application is used (i.e. the local DB doesn't exist
     * yet) we simply "upload" all the music on the server (which will then invalidate any
     * library entries previously valid for the user).
     */
    private class Uploader extends LibraryTask {

        @Override
        protected Boolean doInBackground(Void... params) {
            List<JsonStruct.Track> tracks = new ArrayList<JsonStruct.Track>();
//            Iterable<Playlist> playlist = getRealPlaylists();

//            for (Playlist item : playlist) {
////                tracks.add(new JsonStruct.Track(item.localId, item.artist, item.title));
//            }

            // Sending the updates to the server.
            UnisonAPI api = AppData.getInstance(PlaylistLibraryService.this).getAPI();
            long uid = AppData.getInstance(PlaylistLibraryService.this).getUid();

            Request.Result<JsonStruct.Success> res = api.uploadLibrarySync(uid, tracks);
            if (res.result == null) {
                if (res.error.hasJsonError()) {
                    Log.w(TAG, "couldn't send tracks to server: " + res.error.jsonError.message);
                } else {
                    Log.w(TAG, "couldn't send tracks to server.", res.error.error);
                }
                return false;
            }

//            // Store the music in the library.
//            LibraryHelper helper = new LibraryHelper(PlaylistLibraryService.this);
//            for (MusicItem item : music) {
//                helper.insert(item);
//            }

            return true;
        }

    }

}
