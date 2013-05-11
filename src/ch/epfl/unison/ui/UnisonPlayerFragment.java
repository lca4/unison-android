package ch.epfl.unison.ui;

import java.util.ArrayList;
import java.util.List;

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
 * Provides the standard stuff about handling the music player.
 * 
 * Provides basic support for dj.
 * 
 * Does not offer a data structure
 * 
 * @author marc
 * 
 */
public abstract class UnisonPlayerFragment extends SherlockFragment implements
		OnClickListener {

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
			Log.d(smTag, String.format("artist: %s, title: %s, rating: %d",
					item.artist, item.title, rating));

			UnisonAPI api = AppData.getInstance(getActivity()).getAPI();
			// TODO adapt
			api.instantRate(0, item.artist, item.title, rating,
					new UnisonAPI.Handler<JsonStruct.Success>() {
						@Override
						public void callback(JsonStruct.Success struct) {
						}

						@Override
						public void onError(Error error) {
							Log.d(smTag, error.toString());
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
			UnisonPlayerFragment.this.setStatus(Status.Stopped);
			Log.i(smTag, "track has completed, send the next one.");
			UnisonPlayerFragment.this.next();
		}

	}

	private static String smTag = "ch.epfl.unison.UnisonPlayerFragment";

	private static final int UPDATE_INTERVAL = 1000; // In milliseconds.

	private static final int CLICK_INTERVAL = 5 * 1000; // In milliseconds.

	private static final int SEEK_BAR_MAX = 100; // mSeekBar goes from 0 to

	// SEEK_BAR_MAX.
	public static int getSeekBarMax() {
		return SEEK_BAR_MAX;
	}

	private UnisonFragmentActivity mMainActivity;

	private Button mNextBtn;
	private Button mPrevBtn;
	private Button mToggleBtn;
	private Button mRatingBtn;
	private Button mDjBtn;

	private SeekBar mSeekBar;
	// private TrackProgress progressTracker = null;
	private Handler mHandler = new Handler();
	private View mButtons;

	private TextView mArtistTxt;
	private TextView mTitleTxt;
	private ImageView mCoverImg;
//	private TrackQueue mTrackQueue;

	private MusicItem mCurrentTrack;
	private List<MusicItem> mHistory;

	private int mHistPointer;

	private boolean mDJSupport;
	private boolean mIsDJ;
	private Status mStatus = Status.Stopped;
	private BroadcastReceiver mCompletedReceiver = new TrackCompletedReceiver();

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

	private Runnable mUpdateProgressTask = new Runnable() {

		@Override
		public void run() {
			int currentPosition = getMusicService().getCurrentPosition();
			int total = getMusicService().getDuration();

			if (total == 0) {
				total = Integer.MAX_VALUE;
			}

			getSeekBar().setProgress(
					(getSeekBarMax() * currentPosition) / total);
			getHandler().postDelayed(this, UPDATE_INTERVAL);
		}
	};

	public void addToHistory(MusicItem item) {
		mHistory.add(0, item);
	}

	public TextView getArtistTxt() {
		return mArtistTxt;
	}

	public View getButtons() {
		return mButtons;
	}

	public ServiceConnection getConnection() {
		return mConnection;
	}

	public ImageView getCoverImg() {
		return mCoverImg;
	}

	public MusicItem getCurrentTrack() {
		return mCurrentTrack;
	}
	
	public Button getDJBtn() {
		return mDjBtn;
	}
	
	public Handler getHandler() {
		return mHandler;
	}
	
	public List<MusicItem> getHistory() {
		return mHistory;
	}

	public int getHistPointer() {
		return mHistPointer;
	}

	public UnisonFragmentActivity getMainActivity() {
		return mMainActivity;
	}

	public MusicServiceBinder getMusicService() {
		return mMusicService;
	}

	public Button getNextBtn() {
		return mNextBtn;
	}

	public Button getPrevBtn() {
		return mPrevBtn;
	}

	public SeekBar getSeekBar() {
		return mSeekBar;
	}

	public Status getStatus() {
		return mStatus;
	}

	public TextView getTitleTxt() {
		return mTitleTxt;
	}

	public Button getToggleBtn() {
		return mToggleBtn;
	}

//	public TrackQueue getTrackQueue() {
//		return mTrackQueue;
//	}

	public Runnable getUpdateProgressTask() {
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

	public boolean isBound() {
		return mIsBound;
	}

	public boolean isDJ() {
		return mIsDJ;
	}

	protected void next() {
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
			if (requestTrack()) {
				play(mHistory.get(0));
			}
		}
	}

	protected abstract void notifyPlay(MusicItem item);

	protected abstract void notifySkip();

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		setMainActivity((UnisonFragmentActivity) activity);
		getMainActivity().registerReceiver(mCompletedReceiver,
				new IntentFilter(MusicService.ACTION_COMPLETED));
	}

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
		} else if (mDJSupport && v == mDjBtn) {
			Log.d(smTag, "Clicked DJ button");
			setIsDJ(!mIsDJ);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.player, container, false);

		// Default values
		mDJSupport = false;
		mIsDJ = false;

		mToggleBtn = (Button) v.findViewById(R.id.musicToggleBtn);
		mToggleBtn.setOnClickListener(this);
		setNextBtn((Button) v.findViewById(R.id.musicNextBtn));
		getNextBtn().setOnClickListener(this);
		setPrevBtn((Button) v.findViewById(R.id.musicPrevBtn));
		getPrevBtn().setOnClickListener(this);
		mDjBtn = (Button) v.findViewById(R.id.djToggleBtn);
		mDjBtn.setOnClickListener(this);
		mDjBtn.setVisibility(View.INVISIBLE);
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
	public void onDestroy() {
		super.onDestroy();
		getHandler().removeCallbacks(getUpdateProgressTask());
		getActivity().startService(new Intent(MusicService.ACTION_STOP));
	}

	@Override
	public void onDetach() {
		super.onDetach();
		getMainActivity().unregisterReceiver(mCompletedReceiver);
	}

	@Override
	public void onStart() {
		super.onStart();
		getSeekBar().setEnabled(true);
		if (mDJSupport && !mIsDJ) {
			// Just to make sure, when the activity is recreated.
			getButtons().setVisibility(View.INVISIBLE);
			mDjBtn.setText(getString(R.string.player_become_dj));
			getSeekBar().setVisibility(View.INVISIBLE);
		} else {
			getSeekBar().setVisibility(View.VISIBLE);
		}
		getMainActivity().bindService(
				new Intent(getMainActivity(), MusicService.class),
				getConnection(), Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onStop() {
		super.onStop();
		if (isBound()) {
			getMainActivity().unbindService(getConnection());
		}
//		if (getTrackQueue() != null) {
//			getTrackQueue().stop();
//		}
	}

	protected void play(MusicItem item) {
		Log.i(smTag, String.format("playing %s - %s", item.artist, item.title));
		// Send the song to the music player service.
		Uri uri = ContentUris.withAppendedId(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, item.localId);
		getMainActivity().startService(
				new Intent(MusicService.ACTION_LOAD).setData(uri).putExtra(
						Const.Strings.SONG_ARTIST_TITLE,
						String.format("%s - %s", item.artist, item.title)));
		setCurrentTrack(item);
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

	private void prev() {
		int curPos = 0;
		if (isBound()) {
			curPos = getMusicService().getCurrentPosition();
		}
		if (curPos < CLICK_INTERVAL && mHistPointer < mHistory.size() - 1) {
			// We play the *previous* track.
			mHistPointer += 1;
			play(mHistory.get(mHistPointer));
		} else if (mHistPointer < mHistory.size()) {
			// We just restart the current track.
			play(mHistory.get(mHistPointer));
		}
	}

	/**
	 * Does not return explicitly a new track. A track should be added the 
	 * history through {@link #addToHistory(MusicItem)}.
	 * In case of success, return true.
	 * Else, return false.
	 * @return
	 */
	protected abstract boolean requestTrack();

	private void seek(int progress) {
		if ((getStatus() == Status.Playing || getStatus() == Status.Paused)) {
			getMusicService().setCurrentPosition(progress);
		}
	}

	public void setArtistTxt(TextView mArtistTxt) {
		this.mArtistTxt = mArtistTxt;
	}

	public void setBound(boolean mIsBound) {
		this.mIsBound = mIsBound;
	}

	public void setButtons(View mButtons) {
		this.mButtons = mButtons;
	}

	public void setConnection(ServiceConnection mConnection) {
		this.mConnection = mConnection;
	}

	public void setCoverImg(ImageView mCoverImg) {
		this.mCoverImg = mCoverImg;
	}

	public void setCurrentTrack(MusicItem mCurrentTrack) {
		this.mCurrentTrack = mCurrentTrack;
	}

	/**
	 * Also makes the DJ toggle visible.
	 * 
	 * @param djSupport
	 */
	public void setDJSupport(boolean djSupport) {
		mDJSupport = djSupport;
		if (mDJSupport) {
			mDjBtn.setVisibility(View.VISIBLE);
		} else {
			mDjBtn.setVisibility(View.INVISIBLE);
		}
	}

	public void setHandler(Handler mHandler) {
		this.mHandler = mHandler;
	}

	public void setHistPointer(int val) {
		mHistPointer = val;
	}
	
	public void setHistory(List<MusicItem> history) {
		mHistory = history;
	}

	/**
	 * Be sure you asked for DJ support first. If the DJ is not supported,
	 * throws an UnsupportedOperationException.
	 * 
	 * @param wantsToBeDJ
	 */
	protected void setIsDJ(boolean wantsToBeDJ) {
		if (mDJSupport) {
			mIsDJ = wantsToBeDJ;
			mMainActivity.setDJ(wantsToBeDJ);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	private void setMainActivity(UnisonFragmentActivity mActivity) {
		this.mMainActivity = mActivity;
	}

	public void setMusicService(MusicServiceBinder mMusicService) {
		this.mMusicService = mMusicService;
	}

	public void setNextBtn(Button mNextBtn) {
		this.mNextBtn = mNextBtn;
	}

	public void setPrevBtn(Button mPrevBtn) {
		this.mPrevBtn = mPrevBtn;
	}

	public void setSeekBar(SeekBar mSeekBar) {
		this.mSeekBar = mSeekBar;
	}

	public void setStatus(Status mStatus) {
		this.mStatus = mStatus;
	}

//	public void setTrackQueue(TrackQueue mTrackQueue) {
//		this.mTrackQueue = mTrackQueue;
//	}

	protected void setTag(String tag) {
		smTag = tag;
	}
	
	public void setTitleTxt(TextView mTitleTxt) {
		this.mTitleTxt = mTitleTxt;
	}

	public void setUpdateProgressTask(Runnable mUpdateProgressTask) {
		this.mUpdateProgressTask = mUpdateProgressTask;
	}

	private void toggle() {
		if (getStatus() == Status.Playing) {
			getActivity().startService(new Intent(MusicService.ACTION_PAUSE));
			setStatus(Status.Paused);
			mToggleBtn.setBackgroundResource(R.drawable.btn_play);
		} else if (getStatus() == Status.Paused) {
			getActivity().startService(new Intent(MusicService.ACTION_PLAY));
			setStatus(Status.Playing);
			mToggleBtn.setBackgroundResource(R.drawable.btn_pause);

		}
	}

	public void updateProgressBar() {
		getHandler().postDelayed(getUpdateProgressTask(), UPDATE_INTERVAL);
	}
}
