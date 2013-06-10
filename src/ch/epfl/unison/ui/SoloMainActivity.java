
package ch.epfl.unison.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import ch.epfl.unison.Const;
import ch.epfl.unison.R;
import ch.epfl.unison.data.PlaylistItem;
import ch.epfl.unison.data.UnisonDB;

import com.actionbarsherlock.view.Menu;

import java.util.HashMap;

/**
 * Activity that is displayed once you opened a playlist. Displays the music
 * player and the list of the tracks (through fragments).
 * 
 * @see AbstractMainActivity
 * @author mbourqui
 */
public class SoloMainActivity extends AbstractMainActivity
        implements SoloPlayerFragment.OnSoloPlayerListener {

    /** Hosted fragments. */
    @SuppressLint("ValidFragment")
    // Avoids Lint wrong warning due to "Fragment" in the enum name
    private enum HostedFragment {
        PLAYER, TRACKS
    }

    /** Simple interface to be notified about playlist info updates. */
    public interface OnPlaylistInfoListener {

        void onPlaylistInfo(PlaylistItem playlistInfo);
    }

    private HashMap<HostedFragment, String> mHostedFragments;

    private UnisonDB mDB;
    private PlaylistItem mPlaylist;

    protected void handleExtras(Bundle extras) {
        if (extras == null || !extras.containsKey(Const.Strings.LOCAL_ID)) {
            // Should never happen. If it does, redirect the user to the
            // SoloPlaylistsActivity.
            startActivity(new Intent(this, SoloPlaylistsActivity.class));
            finish();
        } else {
            if (extras.containsKey(Const.Strings.TITLE)) {
                setTitle(extras.getString(Const.Strings.TITLE));
            }
            mPlaylist = (PlaylistItem) mDB.getItem(PlaylistItem.class,
                    extras.getLong(Const.Strings.LOCAL_ID));
            if (mPlaylist == null) {
                finish();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mDB = new UnisonDB(getApplicationContext());
        super.onCreate(savedInstanceState);

        mHostedFragments = new HashMap<HostedFragment, String>();

        getTabsAdapter().addTab(
                getSupportActBar().newTab().setText(R.string.solo_fragment_player_title),
                SoloPlayerFragment.class, null);
        getTabsAdapter().addTab(
                getSupportActBar().newTab().setText(R.string.solo_fragment_playlist_title),
                SoloTracksFragment.class, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean b = super.onCreateOptionsMenu(menu);
        showSolo(false);
        showRefresh(false);
        return b;
    }

    @Override
    public void onRefresh() {
        // super.onRefresh();
        repaintRefresh(false);
        // UnisonAPI api = AppData.getInstance(this).getAPI();

        // TODO

    }

    protected PlaylistItem getPlaylist() {
        return mPlaylist;
    }

    @Override
    public void onTrackChange() {
        getTracksFragment().refreshView();
    }

    @Override
    public void setPlayerFragmentTag(String tag) {
        mHostedFragments.put(HostedFragment.PLAYER, tag);
    }

    private SoloPlayerFragment getPlayerFragment() {
        return (SoloPlayerFragment) getSupportFragmentManager()
                .findFragmentByTag(mHostedFragments.get(HostedFragment.PLAYER));
    }

    private SoloTracksFragment getTracksFragment() {
        return (SoloTracksFragment) getSupportFragmentManager()
                .findFragmentByTag(mHostedFragments.get(HostedFragment.TRACKS));
    }

}
