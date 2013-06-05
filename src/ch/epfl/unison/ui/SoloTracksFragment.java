
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
    
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//            Bundle savedInstanceState) {
//        View v = super.onCreateView(inflater, container, savedInstanceState);
//        SoloTracksFragment.this.getList()
//        .setAdapter(new TracksAdapter(mHostActivity.getPlaylist()));
//        return v;
//    }

    @Override
    public void onPlaylistInfo(PlaylistItem playlistInfo) {
//        if (playlistInfo.getTitle() != null) {
//            getTitle().setText(playlistInfo.getTitle());
//        }
        setListAdapter(new TracksAdapter(playlistInfo));
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

    /** ArrayAdapter that displays the tracks of the playlist. */
    private class TracksAdapter extends ArrayAdapter<MusicItem> {

        public static final int TRACK_ROW_LAYOUT = R.layout.track_row;

        public TracksAdapter(PlaylistItem playlist) {
            super(SoloTracksFragment.this.getActivity(), 0, playlist.getTracks());
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            MusicItem track = getItem(position);
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) SoloTracksFragment.this.getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(TRACK_ROW_LAYOUT, parent, false);
            }
            ((TextView) view.findViewById(R.id.trackrow_title)).setText(getItem(position).title);
            // int rating = 0;
            // if (getItem(position).rating != null) {
            // rating = getItem(position).rating;
            // }
            // ((RatingBar) view.findViewById(R.id.trRating)).setRating(rating);
            view.setTag(track);
            return view;
        }
    }
}
