
package ch.epfl.unison.music;

import android.content.Context;
import android.media.AudioManager;

/**
 * Small helper class that deals with audio focus. Inspired by the Android SDK's
 * sample application, RandomMusicPlayer.
 * 
 * @author lum
 */
public class AudioFocusHelper implements AudioManager.OnAudioFocusChangeListener {

    private AudioManager mAudioManager;
    private MusicService mMusicService;

    public AudioFocusHelper(Context context, MusicService musicService) {
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mMusicService = musicService;
    }

    public boolean requestFocus() {
        return mAudioManager.requestAudioFocus(
                this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
                == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    public boolean abandonFocus() {
        return mAudioManager.abandonAudioFocus(this)
                == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (mMusicService == null) {
            return;
        }

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                mMusicService.onGainedAudioFocus();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                mMusicService.onLostAudioFocus(false);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mMusicService.onLostAudioFocus(true);
                break;
            default: // Should never happen.
                break;
        }

    }
}
