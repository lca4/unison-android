package ch.epfl.unison.ui;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import ch.epfl.unison.AppData;
import ch.epfl.unison.LibraryHelper;
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;
import ch.epfl.unison.data.MusicItem;

import com.actionbarsherlock.app.SherlockActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity where the user can rate the music on his device (or, more precisely: the
 * tracks that are on the local SQLite DB managed by the app).
 *
 * @author lum
 */
public class RatingsActivity extends SherlockActivity {

    private static final String TAG = "ch.epfl.unison.RatingsActivity";

    private ListView mMusicList;
    private CheckBox mUnratedCheck;

    private Map<String, Integer> mRatings;
    private List<MusicItem> mItems;

    private boolean mUnratedOnly;

    private BroadcastReceiver mLogoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This activity should finish on logout.
        registerReceiver(mLogoutReceiver,
                new IntentFilter(UnisonMenu.ACTION_LOGOUT));

        setContentView(R.layout.ratings);
        setTitle(R.string.activity_title_ratings);

        mMusicList = (ListView) findViewById(R.id.listMusicList);
        mMusicList.setOnItemClickListener(new OnChangeRatingListener());

        mUnratedCheck = (CheckBox) findViewById(R.id.unratedCheckBox);
        mUnratedCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mUnratedOnly = isChecked;
                refreshList();
            }
        });

        initItems();
        initRatings(new Runnable() {
            @Override
            public void run() {
                refreshList();
                mUnratedCheck.setEnabled(true);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mLogoutReceiver);
    }

    private void initRatings(final Runnable clbk) {
        AppData data = AppData.getInstance(this);
        data.getAPI().getRatings(data.getUid(), new UnisonAPI.Handler<JsonStruct.TracksList>() {

            @Override
            public void callback(JsonStruct.TracksList struct) {
                mRatings = new HashMap<String, Integer>();
                for (JsonStruct.Track t : struct.tracks) {
                    mRatings.put(t.artist + t.title, t.rating);
                }
                clbk.run();
            }

            @Override
            public void onError(Error error) {
                Log.d(TAG, error.toString());
                if (RatingsActivity.this != null) {
                    Toast.makeText(RatingsActivity.this, R.string.error_loading_ratings,
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void initItems() {
        LibraryHelper helper = new LibraryHelper(this);
        mItems = new ArrayList<MusicItem>(helper.getEntries());
        Collections.sort(mItems);
        helper.close();
    }

    private void refreshList() {
        refreshList(0);
    }

    private void refreshList(int position) {
        if (mUnratedOnly) {
            List<MusicItem> filtered = new ArrayList<MusicItem>();
            for (MusicItem item : mItems) {
                if (!mRatings.containsKey(item.artist + item.title)) {
                    filtered.add(item);
                }
            }
            mMusicList.setAdapter(new RatingsAdapter(filtered));
        } else {
            mMusicList.setAdapter(new RatingsAdapter(mItems));
        }
        mMusicList.setSelection(position);
    }

    /** Adapter used to display tracks and their ratings in a ListView. */
    private class RatingsAdapter extends ArrayAdapter<MusicItem> {

        public static final int ROW_LAYOUT = R.layout.ratings_row;

        private RatingsActivity mActivity;

        public RatingsAdapter(List<MusicItem> ratings) {
            super(RatingsActivity.this, 0, ratings);
            mActivity = RatingsActivity.this;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(ROW_LAYOUT, parent, false);
            }
            String artist = getItem(position).artist;
            String title = getItem(position).title;
            Integer rating = mActivity.mRatings.get(artist + title);

            ((TextView) view.findViewById(R.id.artistMusicList)).setText(artist);
            ((TextView) view.findViewById(R.id.textMusicList)).setText(title);
            RatingBar bar = (RatingBar) view.findViewById(R.id.ratingBarMusicList);
            if (rating != null) {
                bar.setRating(rating);
            } else {
                bar.setRating(0);
            }

            view.setTag(getItem(position));
            return view;
        }
    }

    /** Listens to changes in ratings and updates the back-end accordingly. */
    private class OnChangeRatingListener implements OnItemClickListener {

        public void sendRating(final MusicItem item, final int rating, final int position) {
            AppData data = AppData.getInstance(RatingsActivity.this);
            data.getAPI().rate(data.getUid(), item.artist, item.title, rating,
                    new UnisonAPI.Handler<JsonStruct.Success>() {

                        @Override
                        public void callback(JsonStruct.Success struct) {
                            mRatings.put(item.artist + item.title, rating);
                            refreshList(position);
                        }

                        @Override
                        public void onError(Error error) {
                            Log.d(TAG, error.toString());
                            if (RatingsActivity.this != null) {
                                Toast.makeText(RatingsActivity.this,
                                        R.string.error_updating_rating,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id)  {
            final MusicItem item = (MusicItem) view.getTag();
            int tempRating = 0;
            if (mRatings.get(item.artist + item.title) != null) {
                tempRating = mRatings.get(item.artist + item.title);
            }
            final int oldRating = tempRating;

            AlertDialog.Builder alert = new AlertDialog.Builder(RatingsActivity.this);

            alert.setTitle(item.title);
            alert.setMessage(getString(R.string.ratings_like));

            LayoutInflater inflater = (LayoutInflater) RatingsActivity.this
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.rating_dialog, null);
            final RatingBar bar = (RatingBar) layout.findViewById(R.id.ratingBar);
            bar.setRating(oldRating);

            alert.setView(layout);
            alert.setPositiveButton(getString(R.string.ratings_ok),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            final int newRating = Math.max((int) bar.getRating(), 1);
                            if (newRating != oldRating) {
                                sendRating(item, newRating, position);
                            }
                }
            });

            alert.setNegativeButton(getString(R.string.ratings_cancel), null);
            alert.show();
        }
    }
}
