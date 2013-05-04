package ch.epfl.unison.music;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import ch.epfl.unison.R;
import ch.epfl.unison.ui.MainActivity;

import java.io.IOException;

/**
 * Music player service. Inspired by the Android SDK's sample application,
 * RandomMusicPlayer. We're taking some shortcuts with respect to the sample
 * application, for example, we don't handle remote controls, etc.
 *
 * @author lum
 */
public class MusicService extends Service
        implements OnCompletionListener, OnPreparedListener, OnErrorListener {

    private static final String TAG = "MusicService";
    private static final int NOTIFICATION_ID = 1;
    private static final float DUCK_VOLUME = 0.1f;

    // Actions used on the service.
    public static final String ACTION_LOAD = "ch.epfl.unison.music.action.LOAD";
    public static final String ACTION_PLAY = "ch.epfl.unison.music.action.PLAY";
    public static final String ACTION_PAUSE = "ch.epfl.unison.music.action.PAUSE";
    public static final String ACTION_STOP = "ch.epfl.unison.music.action.STOP";
    public static final String ACTION_TOGGLE_PLAYBACK =
            "ch.epfl.unison.music.action.TOGGLE_PLAYBACK";

    // Actions broadcasted.
    public static final String ACTION_COMPLETED = "ch.epfl.unison.music.action.COMPLETED";

    private AudioFocusHelper mFocusHelper;
    private MediaPlayer mMediaPlayer;
    private Notification mNotification;

    private MusicServiceBinder mBinder = new MusicServiceBinder();

    /** State of the the audio player. */
    private enum State {
        Stopped,   // Media player is stopped.
        Preparing, // Media player is preparing
        Playing,   // Currently playing.
        Paused,    // Paused by user.
    }
    private State mState = State.Stopped;

    /** State of the audio focus. */
    private enum AudioFocus {
        NoFocusNoDuck,  // We don't have the focus and can't duck.
        NoFocusCanDuck, // We don't have the focus but can duck.
        Focused,        // We have the focus. Yay!
    }
    private AudioFocus mFocus = AudioFocus.NoFocusNoDuck;

    @Override
    public void onCreate() {
        mFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
    }

    @Override
    public void onDestroy() {
        // Service is being killed, so make sure we release our resources.
        mState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
    }

    /**
     * Called when we receive an intent.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action.equals(ACTION_TOGGLE_PLAYBACK)) {
            toggle();
        } else if (action.equals(ACTION_LOAD)) {
            load(intent);
        } else if (action.equals(ACTION_PLAY)) {
            play();
        } else if (action.equals(ACTION_PAUSE)) {
            pause();
        } else if (action.equals(ACTION_STOP)) {
            stop();
        }
        // Don't restart if killed.
        return START_NOT_STICKY;
    }

    private void toggle() {
        Log.i(TAG, "PLAY/PAUSE button pressed");
        if (mState == State.Paused) {
            play();
        } else if (mState == State.Playing) {
            pause();
        }
    }

    private void load(Intent intent) {
        Log.i(TAG, "loading track");
        mState = State.Stopped;
        relaxResources(false);
        tryToGetAudioFocus();

        try {
            Uri uri = intent.getData();
            createMediaPlayerIfNeeded();

            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDataSource(getApplicationContext(), uri);

            mState = State.Preparing;
            // Calls OnPreparedListener when ready.
            mMediaPlayer.prepareAsync();

            setUpAsForeground("Unison"); // TODO Change notification text.
        } catch (IOException ioe) {
            Log.e(TAG, "Couldn't load resource.");
        }
    }

    private void play() {
        if (mState == State.Paused) {
            tryToGetAudioFocus();
            setUpAsForeground("Unison"); // TODO Change notification text
            mState = State.Playing;
            configAndStartMediaPlayer();
        }
    }

    private void pause() {
        if (mState == State.Playing) {
            mState = State.Paused;
            mMediaPlayer.pause();
            relaxResources(false); // Keep audio focus.
        }
    }

    private void stop() {
        if (mState == State.Playing || mState == State.Paused) {
            relaxResources(true);
            mState = State.Stopped;
            giveUpAudioFocus();

            // Service is no longer necessary.
            stopSelf();
        }
    }

    /**
     * Makes sure the media player exists and has been reset. Creates one if it
     * doesn't exist.
     */
    private void createMediaPlayerIfNeeded() {
        if (mMediaPlayer != null) {
            // The MediaPlayer object is already set up. We just reset it.
            mMediaPlayer.reset();
            return;
        }
        mMediaPlayer = new MediaPlayer();
        // This means that the screen can go off, but the CPU has to stay running.
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        // Various events we need to handle.
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it.
     */
    void configAndStartMediaPlayer() {
        Log.d(TAG, "duration = " + mMediaPlayer.getDuration());
        if (mFocus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause.
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause(); // Note: the status remains Playing.
            }
        } else if (mFocus == AudioFocus.NoFocusCanDuck) {
            mMediaPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);
            if (!mMediaPlayer.isPlaying()) {
                mMediaPlayer.start();
            }
        } else { // this.focus == AudioFocus.Focused
            mMediaPlayer.setVolume(1.0f, 1.0f);
            if (!mMediaPlayer.isPlaying()) {
                mMediaPlayer.start();
            }
        }
    }

    private void setUpAsForeground(String text) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        mNotification = new Notification();
        mNotification.tickerText = text;
        mNotification.icon = R.drawable.ic_media_play;
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mNotification.setLatestEventInfo(getApplicationContext(), "Unison",
                text, pi);
        startForeground(NOTIFICATION_ID, mNotification);
    }

    /**
     * Releases resources used by the service for playback.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should
     *     also be released or not.
     */
    void relaxResources(boolean releaseMediaPlayer) {
        stopForeground(true);

        if (releaseMediaPlayer && mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    void tryToGetAudioFocus() {
        if (mFocus != AudioFocus.Focused && mFocusHelper != null
                && mFocusHelper.requestFocus()) {
            mFocus = AudioFocus.Focused;
        }
    }

    void giveUpAudioFocus() {
        if (mFocus == AudioFocus.Focused && mFocusHelper != null
                && mFocusHelper.abandonFocus()) {
            mFocus = AudioFocus.NoFocusNoDuck;
        }
    }

    public void onGainedAudioFocus() {
        mFocus = AudioFocus.Focused;
        if (mState == State.Playing) {
            configAndStartMediaPlayer();
        }
    }

    public void onLostAudioFocus(boolean canDuck) {
        if (canDuck) {
            mFocus = AudioFocus.NoFocusCanDuck;
        } else {
            mFocus = AudioFocus.NoFocusNoDuck;
        }
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            configAndStartMediaPlayer();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "Media player error: what=" + String.valueOf(what)
                + ", extra=" + String.valueOf(extra));

        mState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
        return true; // true indicates we handled the error
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mState = State.Playing;
        configAndStartMediaPlayer();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.i(TAG, "track completed - sending broadcast message");
        sendBroadcast(new Intent().setAction(ACTION_COMPLETED));
    }

    /** Binder to get access to the media player. */
    public class MusicServiceBinder extends Binder {
        public int getCurrentPosition() {
            if (mMediaPlayer != null) {
                return  mMediaPlayer.getCurrentPosition();
            }
            return -1;
        }

        public void setCurrentPosition(int newPos) {
            if (mMediaPlayer != null) {
                Log.d(TAG, "using seekTo(" + newPos + ")");
                mMediaPlayer.seekTo(newPos);
            }
        }
        public int getDuration() {
            if (mMediaPlayer != null) {
                return mMediaPlayer.getDuration();
            }
            return 0;
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
