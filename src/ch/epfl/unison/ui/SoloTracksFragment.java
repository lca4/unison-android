
package ch.epfl.unison.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ch.epfl.unison.R;
import ch.epfl.unison.Uutils;

/**
 * Fragment that is displayed inside MainActivity (one of the tabs). Contains
 * the list of the tracks of the playlist.
 * 
 * @author mbourqui
 */
public class SoloTracksFragment extends AbstractListFragment {

    private SoloMainActivity mHostActivity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mHostActivity = (SoloMainActivity) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refresh();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    /**
     * Doesn't work, getListView count is 0.
     */
    private void highlightCurrent() {
        View v = getListView().findViewWithTag(mHostActivity.getPlaylist().current());
        if (v != null) {
            v.findViewById(R.id.listrow_track_image).setVisibility(View.VISIBLE);
        }
    }

    public void refreshView() {
        refresh();
    }

    private void refresh() {
        setListAdapter(new Uutils.Adapters.TracksAdapter(mHostActivity,
                mHostActivity.getPlaylist()));
        highlightCurrent();
    }

}
