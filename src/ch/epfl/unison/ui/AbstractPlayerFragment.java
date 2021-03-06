
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
import ch.epfl.unison.Const;
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;
import ch.epfl.unison.data.MusicItem;
import ch.epfl.unison.music.MusicService;
import ch.epfl.unison.music.MusicService.MusicServiceBinder;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * Fragment that is displayed inside {@link AbstractMainActivity} (one of the
 * tabs). It contains the UI of the music player (media player buttons, cover
 * art, ...). <br />
 * Provides:
 * <ul>
 * <li>Default layout of the player,
 * <li>MusicService interaction
 * </ul>
 * Playlist management methods are abstract, sucht that every specialization has
 * to specify how the tracks are handled.
 * 
 * @author marc
 */
public abstract class AbstractPlayerFragment extends SherlockFragment implements
        OnClickListener {

    /*
     * -------------------------------------------------------------------------
     * ABSTRACT METHODS
     * -------------------------------------------------------------------------
     */
    protected abstract void prev();

    protected abstract void next();

    protected abstract void notifyPlay(MusicItem item);

    protected abstract void notifySkip();

    /**
     * Handles instant ratings (when the user clicks on the rating button in the
     * player interface).
     */
    private class OnRatingClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            if (getCurrentTrack() == null) {
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
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            if (getCurrentTrack() != null) {
                                int newRating = Math.max((int) bar.getRating(),
                                        1);
                                sendRating(getCurrentTrack(), newRating);
                            }
                        }
                    });

            alert.setNegativeButton(getString(R.string.player_cancel), null);
            alert.show();
        }

        private void sendRating(MusicItem item, int rating) {
            Log.d(mTag, String.format("artist: %s, title: %s, rating: %d",
                    item.artist, item.title, rating));

            AppData data = AppData.getInstance(getActivity());

            UnisonAPI api = data.getAPI();
            // TODO adapt
            api.instantRate(data.getCurrentGID(), item.artist, item.title, rating,
                    new UnisonAPI.Handler<JsonStruct.Success>() {
                        @Override
                        public void callback(JsonStruct.Success struct) {
                        }

                        @Override
                        public void onError(Error error) {
                            if (error != null) {
                                Log.d(mTag, error.toString());
                            }
                            if (getActivity() != null) {
                                Toast.makeText(getActivity(),
                                        R.string.error_sending_rating,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    /** State of the music player (from the UI point of view). */
    protected enum Status {
        Stopped, Playing, Paused
    }

    /**
     * Listens to broadcasts from the media player indicating when a track is
     * over.
     */
    private class TrackCompletedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            AbstractPlayerFragment.this.mStatus = Status.Stopped;
            Log.i(mTag, "track has completed, send the next one.");
            AbstractPlayerFragment.this.next();
        }

    }

    private String mTag = "ch.epfl.unison.AbstractPlayerFragment";

    private static final int UPDATE_INTERVAL = 1000; // In milliseconds.
    private static final int CLICK_INTERVAL = 5 * 1000; // In milliseconds.
    private static final int SEEK_BAR_MAX = 100; // mSeekBar goes from 0 to the
                                                 // given max


    private AbstractMainActivity mMainActivity;

    private View mButtons;
    private Button mNextBtn;
    private Button mPrevBtn;
    private Button mToggleBtn;
    private Button mRatingBtn;
    private Button mDjBtn;

    private SeekBar mSeekBar;

    private TextView mArtistTxt;
    private TextView mTitleTxt;
    private ImageView mCoverImg;

    private MusicServiceBinder mMusicService;
    private Handler mHandler = new Handler();

    private MusicItem mCurrentTrack;

    private Status mStatus = Status.Stopped;
    private BroadcastReceiver mCompletedReceiver = new TrackCompletedReceiver();

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

    private Runnable mUpdateProgressTask = new Runnable() {

        @Override
        public void run() {
            int currentPosition = mMusicService.getCurrentPosition();
            int total = mMusicService.getDuration();

            if (total == 0) {
                total = Integer.MAX_VALUE;
            }

            mSeekBar.setProgress(
                    (SEEK_BAR_MAX * currentPosition) / total);
            mHandler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mMainActivity = (AbstractMainActivity) activity;
        mMainActivity.registerReceiver(mCompletedReceiver,
                new IntentFilter(MusicService.ACTION_COMPLETED));
        mTag = this.getClass().getName();
    }

    @Override
    public void onClick(View v) {
        if (v == mToggleBtn) {
            Log.d(mTag, "Clicked on play button, status = " + mStatus);
            if (mStatus == Status.Stopped) {
                next();
            } else { // Paused or Playing.
                toggle();
            }
        } else if (v == mNextBtn) {
            next();
        } else if (v == mPrevBtn) {
            prev();
        }
        // else if (mDJSupport && v == mDjBtn) {
        // Log.d(mTag, "Clicked DJ button");
        // //Here we are (almost) sure that the main activity is still not null,
        // so we collect usefull
        // //information for latter servercomm:
        // Activity activity = getActivity();
        // if (activity == null) {
        // //this should never happen
        // Log.d(mTag,
        // "Trying to get or release DJ seat while the activity was null! Aborting.");
        // }
        // AppData data = AppData.getInstance(activity);
        // Location loc = data.getLocation();
        // double lat, lon;
        // if (loc != null) {
        // lat = loc.getLatitude();
        // lon = loc.getLongitude();
        // } else {
        // lat = DEFAULT_LATITUDE;
        // lon = DEFAULT_LONGITUDE;
        // Log.i(mTag, "location was null, using default values");
        // }
        //
        // // setIsDJ(!mIsDJ, data.getAPI(), data.getUid(), activity.getG);
        // }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mTag = this.getClass().getName();

        View v = inflater.inflate(R.layout.player, container, false);

        mToggleBtn = (Button) v.findViewById(R.id.musicToggleBtn);
        mToggleBtn.setOnClickListener(this);
        mNextBtn = (Button) v.findViewById(R.id.musicNextBtn);
        mNextBtn.setOnClickListener(this);
        mPrevBtn = (Button) v.findViewById(R.id.musicPrevBtn);
        mPrevBtn.setOnClickListener(this);

        mDjBtn = (Button) v.findViewById(R.id.djToggleBtn);
        mDjBtn.setOnClickListener(this);
        mDjBtn.setVisibility(View.INVISIBLE);

        mRatingBtn = (Button) v.findViewById(R.id.ratingBtn);
        mRatingBtn.setOnClickListener(new OnRatingClickListener());

        this.mSeekBar = (SeekBar) v.findViewById(R.id.musicProgBar);
        initSeekBar();

        this.mButtons = v.findViewById(R.id.musicButtons);

        this.mArtistTxt = (TextView) v.findViewById(R.id.musicArtist);
        this.mTitleTxt = (TextView) v.findViewById(R.id.musicTitle);
        this.mCoverImg = (ImageView) v.findViewById(R.id.musicCover);

        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(getUpdateProgressTask());
        getActivity().startService(new Intent(MusicService.ACTION_STOP));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMainActivity.unregisterReceiver(mCompletedReceiver);
    }

    @Override
    public void onStart() {
        super.onStart();
        getSeekBar().setEnabled(true);
        // if (mDJSupport && !mIsDJ) {
        // // Just to make sure, when the activity is recreated.
        // getButtons().setVisibility(View.INVISIBLE);
        // mDjBtn.setText(getString(R.string.player_become_dj));
        // getSeekBar().setVisibility(View.INVISIBLE);
        // } else {
        getSeekBar().setVisibility(View.VISIBLE);
        // }
        mMainActivity.bindService(
                new Intent(mMainActivity, MusicService.class),
                mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mIsBound) {
            mMainActivity.unbindService(mConnection);
        }
    }

    protected TextView getArtistTxt() {
        return mArtistTxt;
    }

    View getButtons() {
        return mButtons;
    }

    protected String getClassTag() {
        return mTag;
    }

    protected int getClickInterval() {
        return CLICK_INTERVAL;
    }

    protected ImageView getCoverImg() {
        return mCoverImg;
    }

    protected int getCurrentPosition() {
        if (mIsBound) {
            return mMusicService.getCurrentPosition();
        } else {
            return 0;
        }
    }

    protected MusicItem getCurrentTrack() {
        return mCurrentTrack;
    }

    protected Button getDjBtn() {
        return mDjBtn;
    }

    protected SeekBar getSeekBar() {
        return mSeekBar;
    }

    protected Status getStatus() {
        return mStatus;
    }

    protected TextView getTitleTxt() {
        return mTitleTxt;
    }

    protected Button getToggleBtn() {
        return mToggleBtn;
    }

    private Runnable getUpdateProgressTask() {
        return mUpdateProgressTask;
    }

    private void initSeekBar() {
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromTouch) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mHandler.removeCallbacks(mUpdateProgressTask);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mHandler.removeCallbacks(mUpdateProgressTask);
                int currentPosition = seekBar.getProgress();

                seek(currentPosition
                        * (mMusicService.getDuration() / SEEK_BAR_MAX));

                updateProgressBar();
            }
        });
    }

    protected void play(MusicItem item) {
        Log.i(mTag, String.format("playing %s - %s", item.artist, item.title));
        // Send the song to the music player service.
        Uri uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, item.localId);
        mMainActivity.startService(
                new Intent(MusicService.ACTION_LOAD).setData(uri).putExtra(
                        Const.Strings.SONG_ARTIST_TITLE,
                        String.format("%s - %s", item.artist, item.title)));
        this.mCurrentTrack = item;
        this.mStatus = Status.Playing;

        // Update the interface.
        mToggleBtn.setBackgroundResource(R.drawable.btn_pause);
        mCoverImg.setImageResource(R.drawable.cover);
        mArtistTxt.setText(item.artist);
        mTitleTxt.setText(item.title);

        // Log.d(TAG, "musicService gave us a duration of " + duration + " ms");
        getSeekBar().setProgress(0);
        getSeekBar().setMax(SEEK_BAR_MAX);
        updateProgressBar();

        notifyPlay(item); // Notify the server.
    }

    private void seek(int progress) {
        if ((mStatus == Status.Playing || mStatus == Status.Paused)) {
            mMusicService.setCurrentPosition(progress);
        }
    }

    protected void setCurrentTrack(MusicItem currentTrack) {
        this.mCurrentTrack = currentTrack;
    }

    protected void setDjBtn(Button djBtn) {
        this.mDjBtn = djBtn;
    }

    protected void setStatus(Status status) {
        this.mStatus = status;
    }

    private void toggle() {
        if (getStatus() == Status.Playing) {
            getActivity().startService(new Intent(MusicService.ACTION_PAUSE));
            this.mStatus = Status.Paused;
            mToggleBtn.setBackgroundResource(R.drawable.btn_play);
        } else if (getStatus() == Status.Paused) {
            getActivity().startService(new Intent(MusicService.ACTION_PLAY));
            this.mStatus = Status.Playing;
            mToggleBtn.setBackgroundResource(R.drawable.btn_pause);

        }
    }

    private void updateProgressBar() {
        mHandler.postDelayed(getUpdateProgressTask(), UPDATE_INTERVAL);
    }
}
