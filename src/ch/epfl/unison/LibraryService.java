
package ch.epfl.unison;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.Request;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.data.MusicItem;
import ch.epfl.unison.data.UnisonDB;

/**
 * Android service that helps maintaining the back-end DB in sync with the
 * actual music on the device. It does so by keeping a local "copy" (as a SQLite
 * DB) that mirrors what's on the back-end DB. Whenever it sees that the local
 * copy is out of sync with the music on the device it sends updates to the
 * server.
 * 
 * @author lum
 */
public class LibraryService extends Service {

    private static final String TAG = "ch.epfl.unison.LibraryService";
    private static final int MIN_UPDATE_INTERVAL = 60 * 60 * 10; // In seconds.
    private static final long MILLIS_IN_S = 1000L; // Number of milliseconds in
                                                   // a second.

    public static final String ACTION_UPDATE = "ch.epfl.unison.action.UPDATE";
    public static final String ACTION_TRUNCATE = "ch.epfl.unison.action.TRUNCATE";

    private boolean mIsUpdating;

    private UnisonDB mDB;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "starting the library service");
        String action = intent.getAction();
        mDB = new UnisonDB(this);
        if (action.equals(ACTION_UPDATE)) {
            update();
        } else if (action.equals(ACTION_TRUNCATE)) {
            truncate();
        }
        return START_NOT_STICKY;
    }

    private void truncate() {
        Log.d(TAG, "truncating the user library");
        // LibraryHelper helper = new LibraryHelper(this);
        // helper.truncate();
        // helper.close();
        mDB.truncate(MusicItem.class);

    }

    private void update() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // How many seconds elapsed since the last successful update ?
        long interval = (System.currentTimeMillis() / MILLIS_IN_S)
                - prefs.getLong(Const.PrefKeys.LASTUPDATE, -1);

        if (!mIsUpdating && interval > MIN_UPDATE_INTERVAL) {
            mIsUpdating = true;
            // LibraryHelper helper = new LibraryHelper(this);
            // if (helper.isEmpty()) {
            if (mDB.isEmpty(MusicItem.class)) {
                // If the DB is empty, just PUT all the tracks.
                Log.d(TAG, "uploading all the music");
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
     * Abstract base class for synchronization tasks (either
     * "truncate and upload" or "update some deltas").
     */
    private abstract class LibraryTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPostExecute(Boolean isSuccessful) {
            LibraryService.this.mIsUpdating = false;

            if (isSuccessful) {
                SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences(LibraryService.this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong("lastupdate", System.currentTimeMillis() / MILLIS_IN_S);
                editor.commit();
            }
        }

        public Set<MusicItem> getRealMusic() {
            Set<MusicItem> set = new HashSet<MusicItem>();
            String[] columns = {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.TITLE,
            };
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            Cursor cur = LibraryService.this.getContentResolver().query(uri, columns,
                    MediaStore.Audio.Media.IS_MUSIC + " = 1", null, null);

            if (cur != null && cur.moveToFirst()) {
                int colId = cur.getColumnIndex(MediaStore.Audio.Media._ID);
                int colArtist = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                int colTitle = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
                do {
                    set.add(new MusicItem(cur.getInt(colId),
                            cur.getString(colArtist), cur.getString(colTitle)));
                } while (cur.moveToNext());
            }
            if (cur != null && !cur.isClosed()) {
                cur.close();
            }

            return set;
        }
    }

    /**
     * If the local DB is already populated, we only send deltas to the server
     * (i.e. a list of tracks that were added and a list of tracks that were
     * removed).
     */
    private class Updater extends LibraryTask {

        // private List<JsonStruct.Delta> getDeltas(LibraryHelper helper) {
        private List<JsonStruct.Delta> getDeltas() {
            // Setting up the expectations.
            // Set<MusicItem> expectation = helper.getEntries();
            Set<MusicItem> expectation = (Set<MusicItem>) mDB.getEntries(MusicItem.class);
            Log.d(TAG, "number of OUR entries: " + expectation.size());

            // Take a hard look at the reality.
            Set<MusicItem> reality = getRealMusic();
            Log.d(TAG, "number of TRUE music entries: " + reality.size());

            // Trying to reconcile everyone.
            List<JsonStruct.Delta> deltas = new ArrayList<JsonStruct.Delta>();
            for (MusicItem item : reality) {
                if (!expectation.contains(item)) {
                    Log.d(TAG, "Adding track: " + item.title);
                    deltas.add(new JsonStruct.Delta(JsonStruct.Delta.TYPE_PUT,
                            item.localId, item.artist, item.title)); // Add
                                                                     // item.
                }
            }
            for (MusicItem item : expectation) {
                if (!reality.contains(item)) {
                    Log.d(TAG, "Removing track: " + item.title);
                    deltas.add(new JsonStruct.Delta(JsonStruct.Delta.TYPE_DELETE,
                            item.localId, item.artist, item.title)); // Delete
                                                                     // item.
                }
            }
            Log.d(TAG, "number of deltas: " + deltas.size());
            return deltas;
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            // LibraryHelper helper = new LibraryHelper(LibraryService.this);
            // List<JsonStruct.Delta> deltas = getDeltas(helper);
            List<JsonStruct.Delta> deltas = getDeltas();

            // Sending the updates to the server.
            UnisonAPI api = AppData.getInstance(LibraryService.this).getAPI();
            long uid = AppData.getInstance(LibraryService.this).getUid();

            Request.Result<JsonStruct.Success> res = api.updateLibrarySync(uid, deltas);
            if (res.result == null) {
                if (res.error.hasJsonError()) {
                    Log.w(TAG, "couldn't send deltas to server: " + res.error.jsonError.message);
                } else {
                    Log.w(TAG, "couldn't send deltas to server.", res.error.error);
                }
                return false;
            }

            // "Commiting" the changes locally.
            for (JsonStruct.Delta delta : deltas) {
                MusicItem item = new MusicItem(
                        delta.entry.localId, delta.entry.artist, delta.entry.title);
                if (delta.type.equals(JsonStruct.Delta.TYPE_PUT)) {
                    // helper.insert(item);
                    mDB.insert(item);
                } else { // TYPE_DELETE.
                    // helper.delete(item);
                    mDB.delete(item);
                }
            }

            // helper.close();
            return true;
        }
    }

    /**
     * If it's the first time that the application is used (i.e. the local DB
     * doesn't exist yet) we simply "upload" all the music on the server (which
     * will then invalidate any library entries previously valid for the user).
     */
    private class Uploader extends LibraryTask {

        @Override
        protected Boolean doInBackground(Void... params) {
            List<JsonStruct.Track> tracks = new ArrayList<JsonStruct.Track>();
            Iterable<MusicItem> music = getRealMusic();

            for (MusicItem item : music) {
                tracks.add(new JsonStruct.Track(item.localId, item.artist, item.title));
            }

            // Sending the updates to the server.
            UnisonAPI api = AppData.getInstance(LibraryService.this).getAPI();
            long uid = AppData.getInstance(LibraryService.this).getUid();

            Request.Result<JsonStruct.Success> res = api.uploadLibrarySync(uid, tracks);
            if (res.result == null) {
                if (res.error.hasJsonError()) {
                    Log.w(TAG, "couldn't send tracks to server: " + res.error.jsonError.message);
                } else {
                    Log.w(TAG, "couldn't send tracks to server.", res.error.error);
                }
                return false;
            }

            // Store the music in the library.
            // LibraryHelper helper = new LibraryHelper(LibraryService.this);
            for (MusicItem item : music) {
                // helper.insert(item);
                mDB.insert(item);
            }

            // helper.close();
            return true;
        }

    }

}
