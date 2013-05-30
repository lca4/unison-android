
package ch.epfl.unison.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.Toast;
import ch.epfl.unison.AppData;
import ch.epfl.unison.R;
import ch.epfl.unison.Uutils;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.JsonStruct.Success;
import ch.epfl.unison.api.TrackQueue;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;
import ch.epfl.unison.data.MusicItem;
import ch.epfl.unison.music.MusicService;

/**
 * Specialized Fragment for {@link GroupsMainFragment}.
 * 
 * @see AbstractMainActivity
 * @author lum
 */
public class GroupsPlayerFragment extends AbstractPlayerFragment implements
        GroupsMainActivity.OnGroupInfoListener {

    private static final String TAG = "ch.epfl.unison.PlayerFragment";

    // EPFL Polydome.
    private static final double DEFAULT_LATITUDE = 46.52147800207456;
    private static final double DEFAULT_LONGITUDE = 6.568992733955383;

    private TrackQueue mTrackQueue;
    private boolean mTrackAdded;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        // setTag(TAG);
        setDJSupport(true);
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((GroupsMainActivity) getMainActivity())
                .registerGroupInfoListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ((GroupsMainActivity) getMainActivity())
                .unregisterGroupInfoListener(this);
    }

    @Override
    public void onGroupInfo(JsonStruct.Group groupInfo) {
        // Check that we're consistent with respect to the DJ position.
        Long uid = AppData.getInstance((getMainActivity())).getUid();
        if (!isDJ() && groupInfo.master != null
                && uid.equals(groupInfo.master.uid)) {
            setIsDJ(true);
        } else if (isDJ()
                && (groupInfo.master == null || !uid
                        .equals(groupInfo.master.uid))) {
            setIsDJ(false);
        }

        // Update track information.
        if (groupInfo.track != null) {
            getArtistTxt().setText(groupInfo.track.artist);
            getTitleTxt().setText(groupInfo.track.title);
            setCurrentTrack(new MusicItem(-1, groupInfo.track.artist,
                    groupInfo.track.title));
            if (groupInfo.track.image != null) {
                Uutils.setBitmapFromURL(getCoverImg(), groupInfo.track.image);
            } else {
                getCoverImg().setImageResource(R.drawable.cover);
            }
        } else {
            setCurrentTrack(null);
            getCoverImg().setImageResource(R.drawable.cover);
        }
    }

    @Override
    protected void notifyPlay(MusicItem item) {
        UnisonAPI api = AppData.getInstance((getMainActivity())).getAPI();
        api.setCurrentTrack(
                ((GroupsMainActivity) getMainActivity()).getGroupId(),
                item.artist, item.title,
                new UnisonAPI.Handler<JsonStruct.Success>() {

                    @Override
                    public void callback(JsonStruct.Success struct) {
                        // Automatically refresh the content (in particular, to
                        // get the cover art).
                        ((GroupsMainActivity) getMainActivity()).onRefresh();
                    }

                    @Override
                    public void onError(Error error) {
                        Log.d(TAG, error.toString());
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(),
                                    R.string.error_sending_track,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    @Override
    protected void notifySkip() {
        UnisonAPI api = AppData.getInstance((getMainActivity())).getAPI();
        api.skipTrack(((GroupsMainActivity) getMainActivity()).getGroupId(),
                new UnisonAPI.Handler<JsonStruct.Success>() {
                    @Override
                    public void callback(JsonStruct.Success struct) {
                    }

                    @Override
                    public void onError(Error error) {
                        Log.d(TAG, error.toString());
                    }
                });
    }

    /** Don't call this directly. Call setDJ() instead. */
    private void grabDJSeat() {
        final long gid = ((GroupsMainActivity) getActivity()).getGroupId();
        AppData data = AppData.getInstance(getActivity());
        double lat, lon;
        if (data.getLocation() != null) {
            lat = data.getLocation().getLatitude();
            lon = data.getLocation().getLongitude();
        } else {
            lat = DEFAULT_LATITUDE;
            lon = DEFAULT_LONGITUDE;
            Log.i(TAG, "location was null, using default values");
        }

        data.getAPI().becomeMaster(gid, data.getUid(), lat, lon,
                new UnisonAPI.Handler<JsonStruct.Success>() {

                    @Override
                    public void callback(Success structure) {
                        getDJBtn().setText(getString(R.string.player_leave_dj));
                        getToggleBtn().setBackgroundResource(
                                R.drawable.btn_play);
                        getButtons().setVisibility(View.VISIBLE);
                        getSeekBar().setVisibility(View.VISIBLE);
                        getSeekBar().setEnabled(true);
                        mTrackQueue = new TrackQueue(getActivity(), gid)
                                .start();
                    }

                    @Override
                    public void onError(Error error) {
                        Log.d(TAG, error.toString());
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(),
                                    R.string.error_becoming_dj,
                                    Toast.LENGTH_LONG).show();
                        }
                        GroupsPlayerFragment.this.setIsDJ(false);
                    }
                });
    }

    /** Don't call this directly. Call setDJ() instead. */
    private void dropDJSeat() {
        final long gid = ((GroupsMainActivity) getActivity()).getGroupId();
        AppData data = AppData.getInstance(getActivity());

        if (mTrackQueue != null) {
            mTrackQueue.stop();
        }
        data.getAPI().resignMaster(gid, data.getUid(),
                new UnisonAPI.Handler<JsonStruct.Success>() {

                    @Override
                    public void callback(Success structure) {
                        getDJBtn()
                                .setText(getString(R.string.player_become_dj));
                        getButtons().setVisibility(View.INVISIBLE);
                        getSeekBar().setVisibility(View.INVISIBLE);
                        getSeekBar().setEnabled(false);

                        getActivity().startService(
                                new Intent(MusicService.ACTION_STOP));
                        setStatus(Status.Stopped);
                    }

                    @Override
                    public void onError(Error error) {
                        Log.d(TAG, error.toString());
                    }
                });
    }

    @Override
    protected void setIsDJ(boolean wantsToBeDJ) {
        if (wantsToBeDJ) {
            grabDJSeat();
        } else {
            dropDJSeat();
        }
        super.setIsDJ(wantsToBeDJ);
    }

    /**
     * Handles instant ratings (when the user clicks on the rating button in the
     * player interface).
     */
    private class OnRatingClickListener implements OnClickListener {

        private void sendRating(MusicItem item, int rating) {
            Log.d(TAG, String.format("artist: %s, title: %s, rating: %d",
                    item.artist, item.title, rating));

            UnisonAPI api = AppData.getInstance(getActivity()).getAPI();
            api.instantRate(
                    ((GroupsMainActivity) getMainActivity()).getGroupId(),
                    item.artist, item.title, rating,
                    new UnisonAPI.Handler<JsonStruct.Success>() {
                        @Override
                        public void callback(JsonStruct.Success struct) {
                        }

                        @Override
                        public void onError(Error error) {
                            Log.d(TAG, error.toString());
                            if (getActivity() != null) {
                                Toast.makeText(getActivity(),
                                        R.string.error_sending_rating,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }

        @Override
        public void onClick(View v) {
            if (getCurrentTrack() == null) {
                return;
            }

            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(getString(R.string.player_rate));
            alert.setMessage(getString(R.string.player_like));

            LayoutInflater inflater = (LayoutInflater) getActivity()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.rating_dialog, null);
            final RatingBar bar = (RatingBar) layout
                    .findViewById(R.id.ratingBar);

            alert.setView(layout);
            alert.setPositiveButton(getString(R.string.player_ok),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            if (getCurrentTrack() != null) {
                                int newRating = Math.max((int) bar.getRating(),
                                        1);
                                sendRating(getCurrentTrack(), newRating);
                            }
                        }
                    });

            alert.setNegativeButton(getString(R.string.player_cancel), null);
            alert.show();
        }
    }

    @Override
    protected boolean requestTrack() {
        mTrackQueue.get(new TrackQueue.Callback() {

            @Override
            public void callback(MusicItem item) {
                addToHistory(item);
                mTrackAdded = true;
            }

            @Override
            public void onError() {
                Context c = getActivity();
                if (c != null) {
                    Toast.makeText(c, R.string.error_getting_track,
                            Toast.LENGTH_LONG).show();
                }
                mTrackAdded = false;
            }
        });
        return mTrackAdded;
    }
}
