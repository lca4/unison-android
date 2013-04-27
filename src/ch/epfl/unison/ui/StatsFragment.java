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
 * information about the members of the group: nickname, rating of current song,
 * etc.
 *
 * @author lum
 */
public class StatsFragment extends SherlockFragment implements MainActivity.OnGroupInfoListener {

    @SuppressWarnings("unused")
    private static final String TAG = "ch.epfl.unison.StatsActivity";

    // TODO(lum) Improve this mess.
    private static final float TEN = 10f;
    private static final float TWO = 2f;

    private ListView mUsersList;
    private TextView mTrackTitle;

    private MainActivity mActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.stats, container, false);

        mUsersList = (ListView) v.findViewById(R.id.usersList);
        mTrackTitle = (TextView) v.findViewById(R.id.trackTitle);

        return v;
    }

    @Override
    public void onGroupInfo(JsonStruct.Group groupInfo) {
        if (groupInfo.track != null && groupInfo.track.title != null) {
            mTrackTitle.setText(groupInfo.track.title);
        }
        mUsersList.setAdapter(new StatsAdapter(groupInfo));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (MainActivity) activity;
        mActivity.registerGroupInfoListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity.unregisterGroupInfoListener(this);
    }

    /** ArrayAdapter that displays the stats associated with each user in the group. */
    private class StatsAdapter extends ArrayAdapter<JsonStruct.User> {

        public static final int ROW_LAYOUT = R.layout.stats_row;

        public StatsAdapter(JsonStruct.Group group) {
            super(StatsFragment.this.getActivity(), 0, group.users);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) StatsFragment.this.getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(ROW_LAYOUT, parent, false);
            }
            ((TextView) view.findViewById(R.id.username)).setText(getItem(position).nickname);
            int score = 0;
            if (getItem(position).score != null) {
                score = getItem(position).score;
            }
            float rating = Math.round(score / TEN) / TWO;
            ((RatingBar) view.findViewById(R.id.trRating)).setRating(rating);

            TextView explanation = (TextView) view.findViewById(R.id.likingExplanation);
            if (getItem(position).score == null || getItem(position).predicted == null) {
                explanation.setText(R.string.rating_unknown);
            } else if (getItem(position).predicted) {
                explanation.setText(R.string.rating_predicted);
            } else {
                explanation.setText(R.string.rating_true);
            }

            return view;
        }
    }
}
