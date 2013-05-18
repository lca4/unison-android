
package ch.epfl.unison.ui;

import java.util.HashSet;
import java.util.Set;

import android.content.Intent;
import android.os.Bundle;
import ch.epfl.unison.AppData;
import ch.epfl.unison.Const;
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.UnisonAPI;

/**
 * Activity that is displayed once you're inside the group. Displays the music
 * player and information about the group (through fragments).
 * 
 * @see AbstractMainActivity
 * 
 * @author lum
 */
public class SoloMainActivity extends AbstractMainActivity {

    /** Simple interface to be notified about playlist info updates. */
    public interface OnPlaylistInfoListener {
        void onPlaylistInfo(JsonStruct.PlaylistJS playlistInfo);
    }


    private static final String TAG = "ch.epfl.unison.SoloMainActivity";

    private Set<OnPlaylistInfoListener> mListeners = new HashSet<OnPlaylistInfoListener>();

    public void dispatchPlaylistInfo(JsonStruct.PlaylistJS playlistInfo) {
        for (OnPlaylistInfoListener listener : mListeners) {
            listener.onPlaylistInfo(playlistInfo);
        }
    }

    protected void handleExtras(Bundle extras) {
        if (extras == null || !extras.containsKey(Const.Strings.PLID)) {
            // Should never happen. If it does, redirect the user to the groups
            // list.
            startActivity(new Intent(this, SoloPlaylistsActivity.class));
            finish();
        } else {
            if (extras.containsKey(Const.Strings.TITLE)) {
                setTitle(extras.getString(Const.Strings.TITLE));
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setReloadInterval(RELOAD_INTERVAL);
        getTabsAdapter().addTab(getSupportActBar().newTab().setText(R.string.solo_player_fragment_title),
                SoloPlayerFragment.class, null);
        getTabsAdapter().addTab(getSupportActBar().newTab().setText(R.string.solo_playlist_fragment_title),
                SoloTracksFragment.class, null);
    }

    @Override
    public void onRefresh() {
        super.onRefresh();
        UnisonAPI api = AppData.getInstance(this).getAPI();

        //TODO
        
    }

    public void registerPlaylistInfoListener(OnPlaylistInfoListener listener) {
        mListeners.add(listener);
    }

    public void unregisterPlaylistInfoListener(OnPlaylistInfoListener listener) {
        mListeners.remove(listener);
    }

}
