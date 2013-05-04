package ch.epfl.unison.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import ch.epfl.unison.AppData;
import ch.epfl.unison.R;
import ch.epfl.unison.Uutils;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.JsonStruct.Success;
import ch.epfl.unison.api.TrackQueue;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;
import ch.epfl.unison.data.MusicItem;
import ch.epfl.unison.music.MusicService;
import ch.epfl.unison.music.MusicService.MusicServiceBinder;

import com.actionbarsherlock.app.SherlockFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that is displayed inside MainActivity (one of the tabs). It contains
 * the UI of the music player (media player buttons, cover art, ...).
 *
 * @author lum
 */
public class PlayerFragment extends SherlockFragment implements
        OnClickListener, MainActivity.OnGroupInfoListener {

    private static final String TAG = "ch.epfl.unison.PlayerFragment";
    private static final int CLICK_INTERVAL = 5 * 1000; // In milliseconds.
    private static final int UPDATE_INTERVAL = 1000;  // In milliseconds.
    private static final int SEEK_BAR_MAX = 100;  // mSeekBar goes from 0 to SEEK_BAR_MAX.

    // EPFL Polydome.
    private static final double DEFAULT_LATITUDE = 46.52147800207456;
    private static final double DEFAULT_LONGITUDE = 6.568992733955383;

    private MainActivity mActivity;

    private Button mDjBtn;
    private Button mRatingBtn;
    private Button mToggleBtn;
    private Button mNextBtn;
    private Button mPrevBtn;

    private SeekBar mSeekBar;
    // private TrackProgress progressTracker = null;
    private Handler mHandler = new Handler();

    private View mButtons;
    private TextView mArtistTxt;
    private TextView mTitleTxt;
    private ImageView mCoverImg;

    private boolean mIsDJ;

    private TrackQueue mTrackQueue;

    private MusicItem mCurrentTrack;
    private List<MusicItem> mHistory;
    private int mHistPointer;

    /** State of the music player (from the UI point of view). */
    private enum Status {
        Stopped, Playing, Paused
    }
    private Status mStatus = Status.Stopped;

    private BroadcastReceiver mCompletedReceiver = new TrackCompletedReceiver();
    private MusicServiceBinder mMusicService;
    private boolean mIsBound;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mMusicService = (MusicServiceBinder) service;
            mIsBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mIsBound = false;
        }
    };

    private void initSeekBar() {
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) { }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mHandler.removeCallbacks(mUpdateProgressTask);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mHandler.removeCallbacks(mUpdateProgressTask);
                int currentPosition = seekBar.getProgress();

                seek(currentPosition * (mMusicService.getDuration() / SEEK_BAR_MAX));

                updateProgressBar();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.player, container, false);

        mToggleBtn = (Button) v.findViewById(R.id.musicToggleBtn);
        mToggleBtn.setOnClickListener(this);
        mNextBtn = (Button) v.findViewById(R.id.musicNextBtn);
        mNextBtn.setOnClickListener(this);
        mPrevBtn = (Button) v.findViewById(R.id.musicPrevBtn);
        mPrevBtn.setOnClickListener(this);
        mDjBtn = (Button) v.findViewById(R.id.djToggleBtn);
        mDjBtn.setOnClickListener(this);
        mRatingBtn = (Button) v.findViewById(R.id.ratingBtn);
        mRatingBtn.setOnClickListener(new OnRatingClickListener());

        mSeekBar = (SeekBar) v.findViewById(R.id.musicProgBar);
        initSeekBar();

        mButtons = v.findViewById(R.id.musicButtons);

        mArtistTxt = (TextView) v.findViewById(R.id.musicArtist);
        mTitleTxt = (TextView) v.findViewById(R.id.musicTitle);
        mCoverImg = (ImageView) v.findViewById(R.id.musicCover);

        mHistory = new ArrayList<MusicItem>();
        mHistPointer = 0;

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (MainActivity) activity;
        mActivity.registerGroupInfoListener(this);
        mActivity.registerReceiver(mCompletedReceiver,
                new IntentFilter(MusicService.ACTION_COMPLETED));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity.unregisterGroupInfoListener(this);
        mActivity.unregisterReceiver(mCompletedReceiver);
    }

    @Override
    public void onStart() {
        super.onStart();
        mSeekBar.setEnabled(mIsDJ);
        if (!mIsDJ) {
            // Just to make sure, when the activity is recreated.
            mButtons.setVisibility(View.INVISIBLE);
            mDjBtn.setText(getString(R.string.player_become_dj));
            mSeekBar.setVisibility(View.INVISIBLE);
        } else {
            mSeekBar.setVisibility(View.VISIBLE);
        }
        mActivity.bindService(
                new Intent(mActivity, MusicService.class), mConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mIsBound) {
            mActivity.unbindService(mConnection);
        }
        if (mTrackQueue != null) {
            mTrackQueue.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mUpdateProgressTask);
        getActivity().startService(new Intent(MusicService.ACTION_STOP));
    }

    @Override
    public void onGroupInfo(JsonStruct.Group groupInfo) {
        // Check that we're consistent with respect to the DJ position.
        Long uid = AppData.getInstance(mActivity).getUid();
        if (!mIsDJ && groupInfo.master != null
                && uid.equals(groupInfo.master.uid)) {
            setDJ(true);
        } else if (mIsDJ
                && (groupInfo.master == null || !uid
                        .equals(groupInfo.master.uid))) {
            setDJ(false);
        }

        // Update track information.
        if (groupInfo.track != null) {
            mArtistTxt.setText(groupInfo.track.artist);
            mTitleTxt.setText(groupInfo.track.title);
            mCurrentTrack = new MusicItem(-1, groupInfo.track.artist,
                    groupInfo.track.title);
            if (groupInfo.track.image != null) {
                Uutils.setBitmapFromURL(mCoverImg, groupInfo.track.image);
            } else {
                mCoverImg.setImageResource(R.drawable.cover);
            }
        } else {
            mCurrentTrack = null;
            mCoverImg.setImageResource(R.drawable.cover);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mToggleBtn) {
            if (mStatus == Status.Stopped) {
                next();
            } else { // Paused or Playing.
                toggle();
            }
        } else if (v == mNextBtn) {
            next();
        } else if (v == mPrevBtn) {
            prev();
        } else if (v == mDjBtn) {
            Log.d(TAG, "Clicked DJ button");
            setDJ(!mIsDJ);
        }
    }

    private void seek(int progress) {
        if ((mStatus == Status.Playing || mStatus == Status.Paused) && mIsDJ) {
            mMusicService.setCurrentPosition(progress);
        }
    }

    private void prev() {
        int curPos = 0;
        if (mIsBound) {
            curPos = mMusicService.getCurrentPosition();
        }
        if (curPos < CLICK_INTERVAL
                && mHistPointer < mHistory.size() - 1) {
            // We play the *previous* track.
            mHistPointer += 1;
            play(mHistory.get(mHistPointer));
        } else if (mHistPointer < mHistory.size()) {
            // We just restart the current track.
            play(mHistory.get(mHistPointer));
        }
    }

    private void next() {
        if (!mHistory.isEmpty()
                && mHistPointer == 0
                && (mStatus == Status.Playing || mStatus == Status.Paused)) {
            // We're skipping a song that is heard for the first time. Notify
            // the server.
            notifySkip();
        }

        if (mHistPointer > 0) {
            mHistPointer -= 1;
            play(mHistory.get(mHistPointer));
        } else {
            // We need a new track.
            mTrackQueue.get(new TrackQueue.Callback() {

                @Override
                public void callback(MusicItem item) {
                    mHistory.add(0, item);
                    play(item);
                }

                @Override
                public void onError() {
                    Context c = getActivity();
                    if (c != null) {
                        Toast.makeText(c, R.string.error_getting_track,
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    private void toggle() {
        if (mStatus == Status.Playing) {
            getActivity().startService(
                    new Intent(MusicService.ACTION_PAUSE));
            mStatus = Status.Paused;
            mToggleBtn.setBackgroundResource(R.drawable.btn_play);
        } else if (mStatus == Status.Paused) {
            getActivity().startService(
                    new Intent(MusicService.ACTION_PLAY));
            mStatus = Status.Playing;
            mToggleBtn.setBackgroundResource(R.drawable.btn_pause);

        }
    }

    private void play(MusicItem item) {
        Log.i(TAG, String.format("playing %s - %s", item.artist, item.title));
        // Send the song to the music player service.
        Uri uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, item.localId);
        mActivity.startService(new Intent(MusicService.ACTION_LOAD)
                .setData(uri));
        mCurrentTrack = item;
        mStatus = Status.Playing;

        // Update the interface.
        mToggleBtn.setBackgroundResource(R.drawable.btn_pause);
        mCoverImg.setImageResource(R.drawable.cover);
        mArtistTxt.setText(item.artist);
        mTitleTxt.setText(item.title);

        // Log.d(TAG, "musicService gave us a duration of " + duration + " ms");
        mSeekBar.setProgress(0);
        mSeekBar.setMax(SEEK_BAR_MAX);
        updateProgressBar();

        notifyPlay(item);  // Notify the server.
    }

    private void notifyPlay(MusicItem item) {
        UnisonAPI api = AppData.getInstance(mActivity).getAPI();
        api.setCurrentTrack(mActivity.getGroupId(), item.artist,
                item.title, new UnisonAPI.Handler<JsonStruct.Success>() {

                    @Override
                    public void callback(JsonStruct.Success struct) {
                        // Automatically refresh the content (in particular, to
                        // get the cover art).
                        mActivity.onRefresh();
                    }

                    @Override
                    public void onError(Error error) {
                        Log.d(TAG, error.toString());
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(),
                                    R.string.error_sending_track,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void notifySkip() {
        UnisonAPI api = AppData.getInstance(mActivity).getAPI();
        api.skipTrack(mActivity.getGroupId(),
                new UnisonAPI.Handler<JsonStruct.Success>() {
                    @Override
                    public void callback(JsonStruct.Success struct) {
                    }

                    @Override
                    public void onError(Error error) {
                        Log.d(TAG, error.toString());
                    }
                });
    }

    /** Don't call this directly. Call setDJ() instead. */
    private void grabDJSeat() {
        final long gid = ((MainActivity) getActivity()).getGroupId();
        AppData data = AppData.getInstance(getActivity());
        double lat, lon;
        if (data.getLocation() != null) {
            lat = data.getLocation().getLatitude();
            lon = data.getLocation().getLongitude();
        } else {
            lat = DEFAULT_LATITUDE;
            lon = DEFAULT_LONGITUDE;
            Log.i(TAG, "location was null, using default values");
        }

        data.getAPI().becomeMaster(gid, data.getUid(), lat, lon,
                new UnisonAPI.Handler<JsonStruct.Success>() {

                    @Override
                    public void callback(Success structure) {
                        mDjBtn.setText(getString(R.string.player_leave_dj));
                        mToggleBtn.setBackgroundResource(R.drawable.btn_play);
                        mButtons.setVisibility(View.VISIBLE);
                        mSeekBar.setVisibility(View.VISIBLE);
                        mSeekBar.setEnabled(true);
                        mTrackQueue = new TrackQueue(getActivity(), gid).start();
                    }

                    @Override
                    public void onError(Error error) {
                        Log.d(TAG, error.toString());
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(),
                                    R.string.error_becoming_dj, Toast.LENGTH_LONG).show();
                        }
                        PlayerFragment.this.setDJ(false);
                    }
                });
    }

    /** Don't call this directly. Call setDJ() instead. */
    private void dropDJSeat() {
        final long gid = ((MainActivity) getActivity()).getGroupId();
        AppData data = AppData.getInstance(getActivity());

        if (mTrackQueue != null) {
            mTrackQueue.stop();
        }
        data.getAPI().resignMaster(gid, data.getUid(),
                new UnisonAPI.Handler<JsonStruct.Success>() {

                    @Override
                    public void callback(Success structure) {
                        mDjBtn.setText(getString(R.string.player_become_dj));
                        mButtons.setVisibility(View.INVISIBLE);
                        mSeekBar.setVisibility(View.INVISIBLE);
                        mSeekBar.setEnabled(false);

                        getActivity().startService(
                                new Intent(MusicService.ACTION_STOP));
                        mStatus = Status.Stopped;
                    }

                    @Override
                    public void onError(Error error) {
                        Log.d(TAG, error.toString());
                    }
                });
    }

    private void setDJ(boolean wantsToBeDJ) {
        if (wantsToBeDJ) {
            grabDJSeat();
        } else {
            dropDJSeat();
        }
        setIsDj(wantsToBeDJ);
    }

    /**
     * Handles instant ratings (when the user clicks on the rating button in
     * the player interface).
     */
    private class OnRatingClickListener implements OnClickListener {

        private void sendRating(MusicItem item, int rating) {
            Log.d(TAG, String.format("artist: %s, title: %s, rating: %d",
                    item.artist, item.title, rating));

            UnisonAPI api = AppData.getInstance(getActivity()).getAPI();
            api.instantRate(mActivity.getGroupId(), item.artist, item.title, rating,
                    new UnisonAPI.Handler<JsonStruct.Success>() {
                        @Override
                        public void callback(JsonStruct.Success struct) { }

                        @Override
                        public void onError(Error error) {
                            Log.d(TAG, error.toString());
                            if (getActivity() != null) {
                                Toast.makeText(getActivity(),
                                        R.string.error_sending_rating, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }

        @Override
        public void onClick(View v) {
            if (mCurrentTrack == null) {
                return;
            }

            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(getString(R.string.player_rate));
            alert.setMessage(getString(R.string.player_like));

            LayoutInflater inflater = (LayoutInflater) getActivity()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.rating_dialog, null);
            final RatingBar bar = (RatingBar) layout
                    .findViewById(R.id.ratingBar);

            alert.setView(layout);
            alert.setPositiveButton(getString(R.string.player_ok),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            if (mCurrentTrack != null) {
                                int newRating = Math.max((int) bar.getRating(), 1);
                                sendRating(mCurrentTrack, newRating);
                            }
                        }
                    });

            alert.setNegativeButton(getString(R.string.player_cancel), null);
            alert.show();
        }
    }

    /** Listens to broadcasts from the media player indicating when a track is over. */
    private class TrackCompletedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            PlayerFragment.this.mStatus = Status.Stopped;
            Log.i(TAG, "track has completed, send the next one.");
            PlayerFragment.this.next();
        }

    }

    public void updateProgressBar() {
        mHandler.postDelayed(mUpdateProgressTask, UPDATE_INTERVAL);
    }

    private Runnable mUpdateProgressTask = new Runnable() {

        @Override
        public void run() {
            int currentPosition = mMusicService.getCurrentPosition();
            int total = mMusicService.getDuration();

            if (total == 0) {
                total = Integer.MAX_VALUE;
            }

            mSeekBar.setProgress((SEEK_BAR_MAX * currentPosition) / total);
            mHandler.postDelayed(this, UPDATE_INTERVAL);
        }
    };
    
    public boolean getIsDj() {
        return mIsDJ;
    }
    
    private void setIsDj(boolean isdj) {
        mIsDJ = isdj;
        mActivity.setDJ(isdj);
    }
}
