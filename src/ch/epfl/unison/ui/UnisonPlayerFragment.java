package ch.epfl.unison.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import ch.epfl.unison.api.TrackQueue;
import ch.epfl.unison.music.MusicService;
import ch.epfl.unison.music.MusicService.MusicServiceBinder;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * Quiet empty for now, some work is to be done here.
 * 
 * TODO implement this superclass
 * 
 * @author marc
 *
 */
public class UnisonPlayerFragment extends SherlockFragment implements
OnClickListener {
	
	/** State of the music player (from the UI point of view). */
    protected enum Status {
        Stopped, Playing, Paused
    }
	
	private static final String TAG = "ch.epfl.unison.UnisonPlayerFragment";
	private static final int UPDATE_INTERVAL = 1000; // In milliseconds.
	
	private static final int SEEK_BAR_MAX = 100; // mSeekBar goes from 0 to
    // SEEK_BAR_MAX.
	
	private UnisonFragmentActivity mMainActivity;
	
	private Button mNextBtn;
    private Button mPrevBtn;

    private SeekBar mSeekBar;
 // private TrackProgress progressTracker = null;
    private Handler mHandler = new Handler();
    
    private View mButtons;
    private TextView mArtistTxt;
    private TextView mTitleTxt;
    private ImageView mCoverImg;
	
    private TrackQueue mTrackQueue;
    
    private Status mStatus = Status.Stopped;
    
    private BroadcastReceiver mCompletedReceiver;
    private MusicServiceBinder mMusicService;
    private boolean mIsBound;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            setMusicService((MusicServiceBinder) service);
            setBound(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            setBound(false);
        }
    };
    
    private BroadcastReceiver mCompletedReceiver = new TrackCompletedReceiver();
    
    public void updateProgressBar() {
        getHandler().postDelayed(getUpdateProgressTask(), UPDATE_INTERVAL);
    }

    private Runnable mUpdateProgressTask = new Runnable() {

        @Override
        public void run() {
            int currentPosition = getMusicService().getCurrentPosition();
            int total = getMusicService().getDuration();

            if (total == 0) {
                total = Integer.MAX_VALUE;
            }

            getSeekBar().setProgress((getSeekBarMax() * currentPosition) / total);
            getHandler().postDelayed(this, UPDATE_INTERVAL);
        }
    };
    
    @Override
    public void onStart() {
        super.onStart();
        getSeekBar().setEnabled(true);
//        if (!mIsDJ) {
//            // Just to make sure, when the activity is recreated.
//            getButtons().setVisibility(View.INVISIBLE);
//            // mDjBtn.setText(getString(R.string.player_become_dj));
//            getSeekBar().setVisibility(View.INVISIBLE);
//        } else {
            getSeekBar().setVisibility(View.VISIBLE);
//        }
        getMainActivity().bindService(
                new Intent(getMainActivity(), MusicService.class), getConnection(),
                Context.BIND_AUTO_CREATE);
    }
    
    
    @Override
    public void onStop() {
        super.onStop();
        if (isBound()) {
            getMainActivity().unbindService(getConnection());
        }
        if (getTrackQueue() != null) {
            getTrackQueue().stop();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        getHandler().removeCallbacks(getUpdateProgressTask());
        getActivity().startService(new Intent(MusicService.ACTION_STOP));
    }

    /**
     * Listens to broadcasts from the media player indicating when a track is
     * over.
     */
    private class TrackCompletedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            UnisonPlayerFragment.this.setStatus(Status.Stopped);
            Log.i(TAG, "track has completed, send the next one.");
            UnisonPlayerFragment.this.next();
        }

    }
    
    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        
    }

	public SeekBar getSeekBar() {
		return mSeekBar;
	}

	public void setSeekBar(SeekBar mSeekBar) {
		this.mSeekBar = mSeekBar;
	}

	public Button getNextBtn() {
		return mNextBtn;
	}

	public void setNextBtn(Button mNextBtn) {
		this.mNextBtn = mNextBtn;
	}

	public Button getPrevBtn() {
		return mPrevBtn;
	}

	public void setPrevBtn(Button mPrevBtn) {
		this.mPrevBtn = mPrevBtn;
	}

	public static int getSeekBarMax() {
		return SEEK_BAR_MAX;
	}

	public Handler getHandler() {
		return mHandler;
	}

	public void setHandler(Handler mHandler) {
		this.mHandler = mHandler;
	}

	public MusicServiceBinder getMusicService() {
		return mMusicService;
	}

	public void setMusicService(MusicServiceBinder mMusicService) {
		this.mMusicService = mMusicService;
	}

	public View getButtons() {
		return mButtons;
	}

	public void setButtons(View mButtons) {
		this.mButtons = mButtons;
	}

	public TextView getArtistTxt() {
		return mArtistTxt;
	}

	public void setArtistTxt(TextView mArtistTxt) {
		this.mArtistTxt = mArtistTxt;
	}

	public TextView getTitleTxt() {
		return mTitleTxt;
	}

	public void setTitleTxt(TextView mTitleTxt) {
		this.mTitleTxt = mTitleTxt;
	}

	public ImageView getCoverImg() {
		return mCoverImg;
	}

	public void setCoverImg(ImageView mCoverImg) {
		this.mCoverImg = mCoverImg;
	}

	public ServiceConnection getConnection() {
		return mConnection;
	}

	public void setConnection(ServiceConnection mConnection) {
		this.mConnection = mConnection;
	}

	public boolean isBound() {
		return mIsBound;
	}

	public void setBound(boolean mIsBound) {
		this.mIsBound = mIsBound;
	}

	public TrackQueue getTrackQueue() {
		return mTrackQueue;
	}

	public void setTrackQueue(TrackQueue mTrackQueue) {
		this.mTrackQueue = mTrackQueue;
	}

	public Status getStatus() {
		return mStatus;
	}

	public void setStatus(Status mStatus) {
		this.mStatus = mStatus;
	}

	public UnisonFragmentActivity getMainActivity() {
		return mMainActivity;
	}

	public void setMainActivity(UnisonFragmentActivity mActivity) {
		this.mMainActivity = mActivity;
	}

	public Runnable getUpdateProgressTask() {
		return mUpdateProgressTask;
	}

	public void setUpdateProgressTask(Runnable mUpdateProgressTask) {
		this.mUpdateProgressTask = mUpdateProgressTask;
	}

}
