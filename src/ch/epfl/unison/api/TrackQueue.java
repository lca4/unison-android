
package ch.epfl.unison.api;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import ch.epfl.unison.AppData;
import ch.epfl.unison.api.JsonStruct.TracksList;
import ch.epfl.unison.api.UnisonAPI.Error;
import ch.epfl.unison.data.MusicItem;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Implements an "infinite" playlist. Tracks are buffered locally such that we
 * can always start playing the next song. The buffer is periodically updated
 * with fresh tracks from the server. As a track might not be readily available,
 * the get() method works asynchronously and returns as soon as we have
 * something in the buffer (in most cases, hopefully immediately).
 * 
 * @author lum
 */
public class TrackQueue {

    private static final String TAG = "ch.epfl.unison.TrackQueue";

    private static final int SLEEP_INTERVAL = 2000; // in ms.
    private static final int POLL_INTERVAL = 30000; // in ms.

    /** Number of attempts to get an element from the queue. **/
    private static final int MAX_RETRIES = 15; // To be multiplied by
                                               // SLEEP_INTERVAL.

    /** Max number of requests to get a track from the server. */
    private static final int MAX_REQUESTS = 10;

    private Set<MusicItem> mPlaylist; // Is a set, but insertion order is
                                      // important!
    private String mPlaylistId;
    private int mNextPtr;

    private boolean mIsActive;
    private boolean mIsPending;
    private Handler mHandler;

    private Context mContext;
    private long mGroupId;

    public TrackQueue(Context context, long gid) {
        // LinkedHashSet returns insert-order iterators.
        mPlaylist = Collections.synchronizedSet(
                new LinkedHashSet<MusicItem>());
        mIsPending = false;
        mIsActive = false;
        mHandler = new Handler();
        mContext = context;
        mGroupId = gid;
    }

    /** Populate the track queue, and start polling for changes. */
    public TrackQueue start() {
        mIsActive = true;
        ensureEnoughElements();
        mHandler.postDelayed(new Poller(), POLL_INTERVAL);
        return this;
    }

    public void stop() {
        this.mIsActive = false;
        this.mPlaylist.clear();
        this.mNextPtr = 0;
    }

    /** Simple callback for the asynchronous get() method. */
    public interface Callback {
        void callback(MusicItem item);

        void onError();
    }

    public void get(final Callback clbk) {
        if (!this.mIsActive) {
            throw new RuntimeException("track queue is inactive");
        }

        this.ensureEnoughElements();
        AsyncTask<Void, Void, MusicItem> task = new AsyncTask<Void, Void, MusicItem>() {

            @Override
            protected MusicItem doInBackground(Void... nothing) {
                for (int i = 0; i < MAX_RETRIES; ++i) {
                    try {
                        MusicItem next = new LinkedList<MusicItem>(mPlaylist).get(mNextPtr);
                        mNextPtr += 1;
                        return next;
                    } catch (IndexOutOfBoundsException e) {
                        Log.i(TAG, "get(): track queue does not yet have enough tracks.");
                    }
                    try {
                        Thread.sleep(SLEEP_INTERVAL);
                    } catch (InterruptedException e) {
                        Log.i(TAG, "sleep got interrupted - wtf?", e);
                    }
                }
                return null; // We declare defeat.
            }

            @Override
            protected void onPostExecute(MusicItem item) {
                if (item != null) {
                    ensureEnoughElements();
                    clbk.callback(item);
                } else {
                    clbk.onError();
                }
            }
        };
        task.execute();
    }

    private synchronized void ensureEnoughElements() {
        if (!this.mIsPending && this.mPlaylist.size() - this.mNextPtr < 1) {
            this.requestTracks();
        }
    }

    private void requestTracks() {
        this.requestTracks(MAX_REQUESTS);
    }

    private void requestTracks(final int trials) {
        if (trials == 0) {
            this.mIsPending = false;
            return;
        }

        UnisonAPI api = AppData.getInstance(this.mContext).getAPI();
        this.mIsPending = true;
        api.getNextTracks(this.mGroupId, new UnisonAPI.Handler<JsonStruct.TracksList>() {

            @Override
            public void callback(JsonStruct.TracksList chunk) {
                mIsPending = false;
                if (chunk.tracks != null && chunk.playlistId != null) {
                    if (!chunk.playlistId.equals(mPlaylistId)) {
                        // The playlist has changed - reset it.
                        mPlaylistId = chunk.playlistId;
                        mNextPtr = 0;
                        mPlaylist.clear();
                    }
                    for (JsonStruct.Track track : chunk.tracks) {
                        Log.d(TAG, String.format("Adding %s - %s to the queue",
                                track.artist, track.title));
                        if (!mPlaylist.add(new MusicItem(track.localId, track.artist,
                                track.title))) {
                            mNextPtr = 0;
                        }
                    }
                }
            }

            @Override
            public void onError(UnisonAPI.Error error) {
                if (error != null) {
                    Log.d(TAG, error.toString());
                }
                requestTracks(trials - 1);
            }
        });
    }

    /** Periodically poll for the next part of the playlist. */
    public class Poller implements Runnable {

        @Override
        public void run() {
            if (!mIsActive) {
                return;
            }

            Log.d(TAG, "Polling for a new playlist...");
            UnisonAPI api = AppData.getInstance(mContext).getAPI();
            api.getPlaylistId(mGroupId, new UnisonAPI.Handler<JsonStruct.TracksList>() {

                @Override
                public void callback(TracksList struct) {
                    if (struct.playlistId != null
                            && !struct.playlistId.equals(mPlaylistId)) {
                        Log.d(TAG, String.format("Playlist ID changed from %s to %s",
                                mPlaylistId, struct.playlistId));
                        requestTracks();
                    }
                    mHandler.postDelayed(Poller.this, POLL_INTERVAL);
                }

                @Override
                public void onError(Error error) {
                    mHandler.postDelayed(Poller.this, POLL_INTERVAL);
                }
            });
        }
    }
}
