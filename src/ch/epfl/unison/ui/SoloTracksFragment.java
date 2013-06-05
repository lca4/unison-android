
package ch.epfl.unison.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import ch.epfl.unison.R;
import ch.epfl.unison.Uutils;
import ch.epfl.unison.data.MusicItem;
import ch.epfl.unison.data.PlaylistItem;

/**
 * Fragment that is displayed inside MainActivity (one of the tabs). Contains
 * the list of the tracks of the playlist.
 * 
 * @author mbourqui
 */
public class SoloTracksFragment extends AbstractListFragment
        implements SoloMainActivity.OnPlaylistInfoListener {
    
    private SoloMainActivity mHostActivity;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        SoloTracksFragment.this
        .setListAdapter(new Uutils.Adapters.TracksAdapter(mHostActivity, 
                mHostActivity.getPlaylist()));
        return v;
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onPlaylistInfo(PlaylistItem playlistInfo) {
//        if (playlistInfo.getTitle() != null) {
//            getTitle().setText(playlistInfo.getTitle());
//        }
        setListAdapter(new Uutils.Adapters.TracksAdapter(mHostActivity, playlistInfo));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mHostActivity = (SoloMainActivity) activity;
        mHostActivity.registerPlaylistInfoListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mHostActivity.unregisterPlaylistInfoListener(this);
    }

    
}
