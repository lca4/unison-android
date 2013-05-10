
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
import ch.epfl.unison.api.JsonStruct.PlaylistJS;
import ch.epfl.unison.api.TrackQueue;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.data.MusicItem;
import ch.epfl.unison.music.MusicService;
import ch.epfl.unison.music.MusicService.MusicServiceBinder;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that is displayed inside MainActivity (one of the tabs). It contains
 * the UI of the music player (media player buttons, cover art, ...).
 * 
 * @author marc
 */
public class SoloPlayerFragment extends UnisonPlayerFragment
        implements SoloMainActivity.OnPlaylistInfoListener {
    // extends SherlockFragment implements OnClickListener,
    // SoloMainActivity.OnPlaylistInfoListener {

    private static final String TAG = "ch.epfl.unison.SoloPlayerFragment";
    private static final int CLICK_INTERVAL = 5 * 1000; // In milliseconds.
    
    

    

    // private Button mDjBtn;
    private Button mRatingBtn;
    private Button mToggleBtn;

    private boolean mIsDJ;


    private MusicItem mCurrentTrack;
    private List<MusicItem> mHistory;
    private int mHistPointer;

    
    

    

    private void initSeekBar() {
        getSeekBar().setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                getHandler().removeCallbacks(getUpdateProgressTask());
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                getHandler().removeCallbacks(getUpdateProgressTask());
                int currentPosition = seekBar.getProgress();

                seek(currentPosition * (getMusicService().getDuration() / getSeekBarMax()));

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
        setNextBtn((Button) v.findViewById(R.id.musicNextBtn));
        getNextBtn().setOnClickListener(this);
        setPrevBtn((Button) v.findViewById(R.id.musicPrevBtn));
        getPrevBtn().setOnClickListener(this);
        // mDjBtn = (Button) v.findViewById(R.id.djToggleBtn);
        // mDjBtn.setOnClickListener(this);
        mRatingBtn = (Button) v.findViewById(R.id.ratingBtn);
        mRatingBtn.setOnClickListener(new OnRatingClickListener());

        setSeekBar((SeekBar) v.findViewById(R.id.musicProgBar));
        initSeekBar();

        setButtons(v.findViewById(R.id.musicButtons));

        setArtistTxt((TextView) v.findViewById(R.id.musicArtist));
        setTitleTxt((TextView) v.findViewById(R.id.musicTitle));
        setCoverImg((ImageView) v.findViewById(R.id.musicCover));

        mHistory = new ArrayList<MusicItem>();
        mHistPointer = 0;

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        setMainActivity((SoloMainActivity) activity);
        // mActivity.registerGroupInfoListener(this);
        getMainActivity().registerReceiver(mCompletedReceiver,
                new IntentFilter(MusicService.ACTION_COMPLETED));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // mActivity.unregisterGroupInfoListener(this);
        getMainActivity().unregisterReceiver(mCompletedReceiver);
    }

    

    

    

    // @Override
    // public void onGroupInfo(JsonStruct.Group groupInfo) {
    // // Check that we're consistent with respect to the DJ position.
    // Long uid = AppData.getInstance(mActivity).getUid();
    // if (!mIsDJ && groupInfo.master != null
    // && uid.equals(groupInfo.master.uid)) {
    // setDJ(true);
    // } else if (mIsDJ
    // && (groupInfo.master == null || !uid
    // .equals(groupInfo.master.uid))) {
    // setDJ(false);
    // }
    //
    // // Update track information.
    // if (groupInfo.track != null) {
    // mArtistTxt.setText(groupInfo.track.artist);
    // mTitleTxt.setText(groupInfo.track.title);
    // mCurrentTrack = new MusicItem(-1, groupInfo.track.artist,
    // groupInfo.track.title);
    // if (groupInfo.track.image != null) {
    // Uutils.setBitmapFromURL(mCoverImg, groupInfo.track.image);
    // } else {
    // mCoverImg.setImageResource(R.drawable.cover);
    // }
    // } else {
    // mCurrentTrack = null;
    // mCoverImg.setImageResource(R.drawable.cover);
    // }
    // }

    @Override
    public void onClick(View v) {
        if (v == mToggleBtn) {
            if (getStatus() == Status.Stopped) {
                next();
            } else { // Paused or Playing.
                toggle();
            }
        } else if (v == getNextBtn()) {
            next();
        } else if (v == getPrevBtn()) {
            prev();
        }
        // } else if (v == mDjBtn) {
        // Log.d(TAG, "Clicked DJ button");
        // // setDJ(!mIsDJ);
        // }
    }

    private void seek(int progress) {
        if ((getStatus() == Status.Playing || getStatus() == Status.Paused) && mIsDJ) {
            getMusicService().setCurrentPosition(progress);
        }
    }

    private void prev() {
        int curPos = 0;
        if (isBound()) {
            curPos = getMusicService().getCurrentPosition();
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
                && (getStatus() == Status.Playing || getStatus() == Status.Paused)) {
            // We're skipping a song that is heard for the first time. Notify
            // the server.
            notifySkip();
        }

        if (mHistPointer > 0) {
            mHistPointer -= 1;
            play(mHistory.get(mHistPointer));
        } else {
            // We need a new track.
            getTrackQueue().get(new TrackQueue.Callback() {

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
        if (getStatus() == Status.Playing) {
            getActivity().startService(
                    new Intent(MusicService.ACTION_PAUSE));
            setStatus(Status.Paused);
            mToggleBtn.setBackgroundResource(R.drawable.btn_play);
        } else if (getStatus() == Status.Paused) {
            getActivity().startService(
                    new Intent(MusicService.ACTION_PLAY));
            setStatus(Status.Playing);
            mToggleBtn.setBackgroundResource(R.drawable.btn_pause);

        }
    }

    private void play(MusicItem item) {
        Log.i(TAG, String.format("playing %s - %s", item.artist, item.title));
        // Send the song to the music player service.
        Uri uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, item.localId);
        getMainActivity().startService(new Intent(MusicService.ACTION_LOAD)
                .setData(uri));
        mCurrentTrack = item;
        setStatus(Status.Playing);

        // Update the interface.
        mToggleBtn.setBackgroundResource(R.drawable.btn_pause);
        getCoverImg().setImageResource(R.drawable.cover);
        getArtistTxt().setText(item.artist);
        getTitleTxt().setText(item.title);

        // Log.d(TAG, "musicService gave us a duration of " + duration + " ms");
        getSeekBar().setProgress(0);
        getSeekBar().setMax(getSeekBarMax());
        updateProgressBar();

        notifyPlay(item); // Notify the server.
    }

    private void notifyPlay(MusicItem item) {
        UnisonAPI api = AppData.getInstance(getMainActivity()).getAPI();
        // api.setCurrentTrack(mActivity.getGroupId(), item.artist,
        // item.title, new UnisonAPI.Handler<JsonStruct.Success>() {
        //
        // @Override
        // public void callback(JsonStruct.Success struct) {
        // // Automatically refresh the content (in particular, to
        // // get the cover art).
        // mActivity.onRefresh();
        // }
        //
        // @Override
        // public void onError(Error error) {
        // Log.d(TAG, error.toString());
        // if (getActivity() != null) {
        // Toast.makeText(getActivity(),
        // R.string.error_sending_track,
        // Toast.LENGTH_LONG).show();
        // }
        // }
        // });
    }

    private void notifySkip() {
        UnisonAPI api = AppData.getInstance(getMainActivity()).getAPI();
        // api.skipTrack(mActivity.getGroupId(),
        // new UnisonAPI.Handler<JsonStruct.Success>() {
        // @Override
        // public void callback(JsonStruct.Success struct) {
        // }
        //
        // @Override
        // public void onError(Error error) {
        // Log.d(TAG, error.toString());
        // }
        // });
    }

    // /** Don't call this directly. Call setDJ() instead. */
    // private void grabDJSeat() {
    // final long gid = ((MainActivity) getActivity()).getGroupId();
    // AppData data = AppData.getInstance(getActivity());
    // double lat, lon;
    // if (data.getLocation() != null) {
    // lat = data.getLocation().getLatitude();
    // lon = data.getLocation().getLongitude();
    // } else {
    // lat = DEFAULT_LATITUDE;
    // lon = DEFAULT_LONGITUDE;
    // Log.i(TAG, "location was null, using default values");
    // }
    //
    // data.getAPI().becomeMaster(gid, data.getUid(), lat, lon,
    // new UnisonAPI.Handler<JsonStruct.Success>() {
    //
    // @Override
    // public void callback(Success structure) {
    // mDjBtn.setText(getString(R.string.player_leave_dj));
    // mToggleBtn.setBackgroundResource(R.drawable.btn_play);
    // mButtons.setVisibility(View.VISIBLE);
    // mSeekBar.setVisibility(View.VISIBLE);
    // mSeekBar.setEnabled(true);
    // mTrackQueue = new TrackQueue(getActivity(), gid).start();
    // }
    //
    // @Override
    // public void onError(Error error) {
    // Log.d(TAG, error.toString());
    // if (getActivity() != null) {
    // Toast.makeText(getActivity(),
    // R.string.error_becoming_dj, Toast.LENGTH_LONG).show();
    // }
    // SoloPlayerFragment.this.setDJ(false);
    // }
    // });
    // }

    // /** Don't call this directly. Call setDJ() instead. */
    // private void dropDJSeat() {
    // final long gid = ((MainActivity) getActivity()).getGroupId();
    // AppData data = AppData.getInstance(getActivity());
    //
    // if (mTrackQueue != null) {
    // mTrackQueue.stop();
    // }
    // data.getAPI().resignMaster(gid, data.getUid(),
    // new UnisonAPI.Handler<JsonStruct.Success>() {
    //
    // @Override
    // public void callback(Success structure) {
    // mDjBtn.setText(getString(R.string.player_become_dj));
    // mButtons.setVisibility(View.INVISIBLE);
    // mSeekBar.setVisibility(View.INVISIBLE);
    // mSeekBar.setEnabled(false);
    //
    // getActivity().startService(
    // new Intent(MusicService.ACTION_STOP));
    // mStatus = Status.Stopped;
    // }
    //
    // @Override
    // public void onError(Error error) {
    // Log.d(TAG, error.toString());
    // }
    // });
    // }

    // private void setDJ(boolean wantsToBeDJ) {
    // if (wantsToBeDJ) {
    // grabDJSeat();
    // } else {
    // dropDJSeat();
    // }
    // mIsDJ = wantsToBeDJ;
    // }

    /**
     * Handles instant ratings (when the user clicks on the rating button in the
     * player interface).
     */
    private class OnRatingClickListener implements OnClickListener {

        private void sendRating(MusicItem item, int rating) {
            Log.d(TAG, String.format("artist: %s, title: %s, rating: %d",
                    item.artist, item.title, rating));

            UnisonAPI api = AppData.getInstance(getActivity()).getAPI();
            // api.instantRate(mActivity.getGroupId(), item.artist, item.title,
            // rating,
            // new UnisonAPI.Handler<JsonStruct.Success>() {
            // @Override
            // public void callback(JsonStruct.Success struct) { }
            //
            // @Override
            // public void onError(Error error) {
            // Log.d(TAG, error.toString());
            // if (getActivity() != null) {
            // Toast.makeText(getActivity(),
            // R.string.error_sending_rating, Toast.LENGTH_LONG).show();
            // }
            // }
            // });
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

    

    

    @Override
    public void onPlaylistInfo(PlaylistJS playlistInfo) {
        // TODO Auto-generated method stub

    }
}
