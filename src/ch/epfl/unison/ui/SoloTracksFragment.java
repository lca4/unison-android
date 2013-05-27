package ch.epfl.unison.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;

import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * Fragment that is displayed inside MainActivity (one of the tabs). Contains
 * the list of the tracks of the playlist.
 *
 * @author lum
 */
public class SoloTracksFragment extends SherlockFragment 
    implements SoloMainActivity.OnPlaylistInfoListener {

    @SuppressWarnings("unused")
    private static final String TAG = "ch.epfl.unison.StatsActivity";

    private ListView mTracksList;
    private TextView mTrackTitle;

    private SoloMainActivity mActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.list, container, false);

        mTracksList = (ListView) v.findViewById(R.id.listList);
        mTrackTitle = (TextView) v.findViewById(R.id.listTitle);

        return v;
    }

    @Override
    public void onPlaylistInfo(JsonStruct.PlaylistJS playlistInfo) {
        if (playlistInfo.title != null) {
            mTrackTitle.setText(playlistInfo.title);
        }
        mTracksList.setAdapter(new TracksAdapter(playlistInfo));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (SoloMainActivity) activity;
        mActivity.registerPlaylistInfoListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity.unregisterPlaylistInfoListener(this);
    }

    /** ArrayAdapter that displays the stats associated with each user in the group. */
    private class TracksAdapter extends ArrayAdapter<JsonStruct.Track> {

        public static final int ROW_LAYOUT = R.layout.track_row;

        public TracksAdapter(JsonStruct.PlaylistJS playlist) {
            super(SoloTracksFragment.this.getActivity(), 0, playlist.tracks);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) SoloTracksFragment.this.getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(ROW_LAYOUT, parent, false);
            }
            ((TextView) view.findViewById(R.id.username)).setText(getItem(position).title);
            int rating = 0;
            if (getItem(position).rating != null) {
                rating = getItem(position).rating;
            }
            ((RatingBar) view.findViewById(R.id.trRating)).setRating(rating);

            return view;
        }
    }
}
