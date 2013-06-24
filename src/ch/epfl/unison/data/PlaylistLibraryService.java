
package ch.epfl.unison.data;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import ch.epfl.unison.AppData;
import ch.epfl.unison.Const;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.Request;
import ch.epfl.unison.api.UnisonAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author marc
 */
public class PlaylistLibraryService extends AbstractService {

    private static final String TAG = "ch.epfl.unison.PlaylistLibraryService";
    private static final int MIN_UPDATE_INTERVAL = 60 * 60 * 10; // In seconds.
    private static final long MILLIS_IN_S = 1000L; // Number of milliseconds in
                                                   // a second.

    public static final String ACTION_UPDATE = "ch.epfl.unison.action.UPDATE";
    public static final String ACTION_TRUNCATE = "ch.epfl.unison.action.TRUNCATE";
    public static final String ACTION_STOP = "ch.epfl.unison.action.STOP";

    private long mUid;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting the playlist library service");
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
        mDB.getPlaylistHandler().truncate();
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
            // if (mDB.getPlaylistHandler().isEmpty()) {
            // If the DB is empty, just PUT all the playlists.
            // Log.d(TAG, "uploading all the playlists");
            // new Uploader().execute();
            // } else {
            Log.d(TAG, "Updating the playlist library"); // Always update
                                                         // (nothing else to do)
            new Updater().execute();
            // }
        } else {
            Log.d(TAG, String.format("didn't update the library (interval: %d)", interval));
        }
    }

    /**
     * Abstract base class for synchronization tasks (either
     * "truncate and upload" or "update some deltas").
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

        // public Set<Playlist> getRealPlaylists() {
        // Set<Playlist> set = new HashSet<Playlist>();
        // String[] columns = {
        // MediaStore.Audio.Playlists._ID,
        // MediaStore.Audio.Playlists.NAME
        // };
        // Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        // Cursor cur =
        // PlaylistLibraryService.this.getContentResolver().query(uri, columns,
        // // MediaStore.Audio.Playlists.CONTENT_TYPE +
        // " = vnd.android.cursor.dir/playlist",
        // null,
        // null, null);
        //
        // if (cur != null && cur.moveToFirst()) {
        // int colId = cur.getColumnIndex(MediaStore.Audio.Playlists._ID);
        // int colName = cur.getColumnIndex(MediaStore.Audio.Playlists.NAME);
        // do {
        // Playlist pl = new Playlist(cur.getInt(colId),
        // cur.getString(colName));
        // AndroidDB.getTracks(getContentResolver(), pl);
        // set.add(pl);
        // } while (cur.moveToNext());
        // cur.close();
        // }
        // return set;
        // }
    }

    /**
     * If the local DB is already populated, we only send deltas to the server
     * (i.e. a list of playlists that were removed and/or a list of changes
     * inside a playlist (added/removed tracks, reordering of the tracks)).
     */
    private class Updater extends LibraryTask {

        private List<JsonStruct.PlaylistDelta> getDeltas(UnisonDB uDB) {
            
            /*
             * TODO compare local update time:
             * - if removed outside app, commit delete delta;
             * - else,
             *      - if same, do nothing;
             *      - else, commit delta track by track (like LibraryService)
             */
            
            
            
            
            // Setting up the expectations.
            Set<PlaylistItem> expectation = uDB.getPlaylistHandler().getItems();
            Log.d(TAG, "Number of OUR entries: " + expectation.size());

            // Take a hard look at the reality.
            Set<PlaylistItem> reality = AndroidDB.getPlaylists(getContentResolver());
            Log.d(TAG, "Number of TRUE music entries: " + reality.size());
            HashMap<Long, PlaylistItem> realityIds = extractLocalIds(reality);

            // Trying to reconcile everyone.
            List<JsonStruct.PlaylistDelta> deltas = new ArrayList<JsonStruct.PlaylistDelta>();

            for (PlaylistItem item : expectation) {
                if (!realityIds.containsKey(item.getLocalId())) {
                    Log.d(TAG, "Removing playlist: " + item.getTitle());
                    deltas.add(new
                            JsonStruct.PlaylistDelta(JsonStruct.PlaylistDelta.TYPE_DELETE,
                                    item.getUserId(), item.getLocalId(), item.getPlaylistId()
                            )); // Delete item.
                } else {
                    long realDateModified = realityIds.get(item.getLocalId()).getDateModified();
                    long expectationDateModified = item.getDateModified();
                    if (realDateModified > expectationDateModified) {
                        // Update from outside the app.
                        //TODO
                    }
                }
            }

            Log.d(TAG, "number of deltas: " + deltas.size());
            return deltas;
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            UnisonDB uDB = new UnisonDB(PlaylistLibraryService.this);
            List<JsonStruct.PlaylistDelta> deltas = getDeltas(uDB);

            // Sending the updates to the server.
            UnisonAPI api = AppData.getInstance(PlaylistLibraryService.this).getAPI();
            long uid = AppData.getInstance(PlaylistLibraryService.this).getUid();

            Request.Result<JsonStruct.Success> res = api.updatePlaylistLibrarySync(uid, deltas);
            if (res.result == null) {
                if (res.error.hasJsonError()) {
                    Log.w(TAG, "Couldn't send playlist deltas to server: "
                            + res.error.jsonError.message);
                } else {
                    Log.w(TAG, "Couldn't send playlist deltas to server.", res.error.error);
                }
                return false;
            }

            return true;
        }

        private HashMap<Long, PlaylistItem> extractLocalIds(Set<PlaylistItem> playlist) {
            HashMap<Long, PlaylistItem> res = new  HashMap<Long, PlaylistItem>();
            for (PlaylistItem item : playlist) {
                res.put(item.getLocalId(), item);
            }
            return res;
        }
    }

}
