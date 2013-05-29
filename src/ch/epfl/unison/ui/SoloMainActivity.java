
package ch.epfl.unison.ui;

import java.util.HashSet;
import java.util.Set;

import android.content.Intent;
import android.os.Bundle;
import ch.epfl.unison.Const;
import ch.epfl.unison.R;
import ch.epfl.unison.data.PlaylistItem;
import ch.epfl.unison.data.UnisonDB;

/**
 * Activity that is displayed once you're inside the group. Displays the music
 * player and information about the playlist (through fragments).
 * 
 * @see AbstractMainActivity
 * @author mbourqui
 */
public class SoloMainActivity extends AbstractMainActivity {

    /** Simple interface to be notified about playlist info updates. */
    public interface OnPlaylistInfoListener {
//        void onPlaylistInfo(JsonStruct.PlaylistJS playlistInfo);

        void onPlaylistInfo(PlaylistItem playlistInfo);
    }

    private static final String TAG = "ch.epfl.unison.SoloMainActivity";

    private UnisonDB mDB;
    private PlaylistItem mPlaylist;
//    private List<MusicItem> mHistory;

    private Set<OnPlaylistInfoListener> mListeners = new HashSet<OnPlaylistInfoListener>();

    public void dispatchPlaylistInfo(PlaylistItem playlistInfo) {
        for (OnPlaylistInfoListener listener : mListeners) {
//            listener.onPlaylistInfo(playlistInfo);
            listener.onPlaylistInfo(playlistInfo);
        }
    }

    protected void handleExtras(Bundle extras) {
        if (extras == null || !extras.containsKey(Const.Strings.LOCAL_ID)) {
            // Should never happen. If it does, redirect the user to the
            // playlists list.
            startActivity(new Intent(this, SoloPlaylistsActivity.class));
            finish();
        } else {
            if (extras.containsKey(Const.Strings.TITLE)) {
                setTitle(extras.getString(Const.Strings.TITLE));
            }
            mPlaylist = (PlaylistItem) mDB.getItem(PlaylistItem.class,
                    extras.getLong(Const.Strings.LOCAL_ID));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mDB = new UnisonDB(getApplicationContext());
        super.onCreate(savedInstanceState);
        // setReloadInterval(RELOAD_INTERVAL);
        getTabsAdapter().addTab(
                getSupportActBar().newTab().setText(R.string.solo_player_fragment_title),
                SoloPlayerFragment.class, null);
        getTabsAdapter().addTab(
                getSupportActBar().newTab().setText(R.string.solo_playlist_fragment_title),
                SoloTracksFragment.class, null);
        
    }

    @Override
    public void onRefresh() {
        super.onRefresh();
        // UnisonAPI api = AppData.getInstance(this).getAPI();

        // TODO

    }

    public void registerPlaylistInfoListener(OnPlaylistInfoListener listener) {
        mListeners.add(listener);
    }

    public void unregisterPlaylistInfoListener(OnPlaylistInfoListener listener) {
        mListeners.remove(listener);
    }
    
    protected PlaylistItem getPlaylist() {
        return mPlaylist;
    }

}
