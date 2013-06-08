
package ch.epfl.unison.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    @Override
    public void onStart() {
        super.onStart();
        if (!mIsDJ) {
            // Just to make sure, when the activity is recreated.
            getButtons().setVisibility(View.INVISIBLE);
            mDjBtn.setText(getString(R.string.player_become_dj));
            getSeekBar().setVisibility(View.INVISIBLE);
        }
    }

    // private Button mDjBtn;
    private boolean mIsDJ = false;

    // This boolean was added in order to avoid inconsistency in the app state
    // when spamming the DJ btton.
    private boolean mProcessingDjRequest = false;

    // EPFL Polydome.
    private static final double DEFAULT_LATITUDE = 46.52147800207456;
    private static final double DEFAULT_LONGITUDE = 6.568992733955383;

    @Override
    public void onClick(View v) {
        super.onClick(v);

        // now we check if the DJ button was clicked:
        if (v == mDjBtn) {
            if (mProcessingDjRequest) {
                return;
            }
            mProcessingDjRequest = true;

            // Log.d(TAG, "Clicked DJ button, mProcessing is now true");
            // Here we are (almost) sure that the main activity is still not
            // null, so we collect usefull
            // information for latter servercomm:
            if (!setupServerCommBundleForDJ()) {
                Log.d(TAG, "The activity was null, aborting.");
                return;
            }
            setIsDJ(true, !mIsDJ, mApi, mUid, mGid, mLatitude, mLongitude);
        }

    }

    private static final String TAG = "ch.epfl.unison.PlayerFragment";

    private TrackQueue mTrackQueue;
    private boolean mTrackAdded;

    private UnisonAPI mApi;
    private double mLatitude;
    private double mLongitude;
    private long mUid;
    private long mGid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        mProcessingDjRequest = false;
        // setTag(TAG);
        // Default values
        // mDJSupport = false;
        mIsDJ = false;
        // setDJSupport(true);

        mDjBtn = (Button) v.findViewById(R.id.djToggleBtn);
        mDjBtn.setVisibility(View.VISIBLE);
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((GroupsMainActivity) getMainActivity())
                .registerGroupInfoListener(this);
        setMode(Mode.Groups);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mTrackQueue != null) {
            mTrackQueue.stop();
        }
        ((GroupsMainActivity) getMainActivity())
                .unregisterGroupInfoListener(this);
    }

    @Override
    public void onGroupInfo(JsonStruct.Group groupInfo) {
        // Check that we're consistent with respect to the DJ position.
        // Long uid = AppData.getInstance((getMainActivity())).getUid();

        if (!setupServerCommBundleForDJ()) {
            Log.d(TAG, "The activity was null, aborting.");
            return;
        }
        // now everything is set up.
        if (!isDJ() && groupInfo.master != null
                && Long.valueOf(mUid).equals(groupInfo.master.uid)) {
            setIsDJ(false, true, mApi, mUid, mGid, mLatitude, mLongitude);
        } else if (isDJ()
                && (groupInfo.master == null || !Long.valueOf(mUid)
                        .equals(groupInfo.master.uid))) {
            setIsDJ(false, false, mApi, mUid, mGid, mLatitude, mLongitude);
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
                        if (error != null) {
                            Log.d(TAG, error.toString());
                        }
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
                        if (error != null) {
                            Log.d(TAG, error.toString());
                        }
                    }
                });
    }

    /** Don't call this directly. Call setDJ() instead. */
    private void grabDJSeat(final UnisonAPI api, final long uid,
            final long gid, final double lat, final double lon) {

        /*
         * if (getActivity() == null) { return; }
         */
        // final long gid = ((GroupsMainActivity) getActivity()).getGroupId();
        // AppData data = AppData.getInstance(getActivity());
        // double lat, lon;
        /*
         * if (data.getLocation() != null) { lat =
         * data.getLocation().getLatitude(); lon =
         * data.getLocation().getLongitude(); } else { lat = DEFAULT_LATITUDE;
         * lon = DEFAULT_LONGITUDE; Log.i(TAG,
         * "location was null, using default values"); }
         */

        // update: now we always get a non null location from the "activity"
        // when the DJ button is pressed.
        // It is set to the default location at this time.

        // data.getAPI().becomeMaster(gid, data.getUid(), lat, lon,
        api.becomeMaster(gid, uid, lat, lon,
                new UnisonAPI.Handler<JsonStruct.Success>() {

                    @Override
                    public void callback(Success structure) {
                        if (getActivity() == null) {
                            Log.d(TAG,
                                    "Tried to update an Activity that was null!");
                            return;
                        }
                        if (mIsDJ) {
                            toggleDJState(true, gid);
                        }
                        mProcessingDjRequest = false;
                        // Log.d(TAG, "mProcessing is now false");
                    }

                    @Override
                    public void onError(Error error) {
                        mIsDJ = false;
                        if (error != null) {
                            Log.d(TAG, error.toString());
                        }
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(),
                                    R.string.error_becoming_dj,
                                    Toast.LENGTH_LONG).show();
                        }
                        GroupsPlayerFragment.this.setIsDJ(true, false, api, uid, gid,
                                lat, lon);
                        mProcessingDjRequest = false;
                        Log.d(TAG, "mProcessing is now false");
                    }
                });
    }

    /** Don't call this directly. Call setDJ() instead. */
    private void dropDJSeat(UnisonAPI api, long uid, long gid) {

        /*
         * if (getActivity() == null) { return; }
         */
        // final long gid = ((GroupsMainActivity) getActivity()).getGroupId();
        // AppData data = AppData.getInstance(getActivity());

        if (mTrackQueue != null) {
            mTrackQueue.stop();
        }

        // data.getAPI().resignMaster(gid, data.getUid(),
        api.resignMaster(gid, uid, new UnisonAPI.Handler<JsonStruct.Success>() {

            @Override
            public void callback(Success structure) {
                if (getActivity() == null) {
                    Log.d(TAG, "Tried to update an Activity that was null!");

                    return;
                }
                if (!mIsDJ) {
                    toggleDJState(false, -1);
                }
                mProcessingDjRequest = false;
                // Log.d(TAG, "mProcessing is now false");
            }

            @Override
            public void onError(Error error) {
                if (error != null) {
                    Log.d(TAG, error.toString());
                }
                mIsDJ = true;
                mProcessingDjRequest = false;
                // Log.d(TAG, "mProcessing is now false");
            }
        });
    }

    protected void setIsDJ(boolean serverComm, boolean wantsToBeDJ, UnisonAPI api, long uid,
            long gid, double lat,
            double lon) {
        if (serverComm) {
            if (wantsToBeDJ) {
                grabDJSeat(api, uid, gid, lat, lon);
            } else {
                dropDJSeat(api, uid, gid);
            }
        } else {
            toggleDJState(wantsToBeDJ, -1);
        }

        mIsDJ = wantsToBeDJ;
        ((GroupsMainActivity) mMainActivity).setDJ(wantsToBeDJ);
    }

    // /**
    // * Handles instant ratings (when the user clicks on the rating button in
    // the
    // * player interface).
    // */
    // private class OnRatingClickListener implements OnClickListener {
    //
    // private void sendRating(MusicItem item, int rating) {
    // Log.d(TAG, String.format("artist: %s, title: %s, rating: %d",
    // item.artist, item.title, rating));
    //
    // UnisonAPI api = AppData.getInstance(getActivity()).getAPI();
    // api.instantRate(
    // ((GroupsMainActivity) getMainActivity()).getGroupId(),
    // item.artist, item.title, rating,
    // new UnisonAPI.Handler<JsonStruct.Success>() {
    // @Override
    // public void callback(JsonStruct.Success struct) {
    // }
    //
    // @Override
    // public void onError(Error error) {
    // if (error != null) {
    // Log.d(TAG, error.toString());
    // }
    // if (getActivity() != null) {
    // Toast.makeText(getActivity(),
    // R.string.error_sending_rating,
    // Toast.LENGTH_LONG).show();
    // }
    // }
    // });
    // }
    //
    // @Override
    // public void onClick(View v) {
    // if (getCurrentTrack() == null) {
    // return;
    // }
    //
    // AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
    // alert.setTitle(getString(R.string.player_rate));
    // alert.setMessage(getString(R.string.player_like));
    //
    // LayoutInflater inflater = (LayoutInflater) getActivity()
    // .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    // View layout = inflater.inflate(R.layout.rating_dialog, null);
    // final RatingBar bar = (RatingBar) layout
    // .findViewById(R.id.ratingBar);
    //
    // alert.setView(layout);
    // alert.setPositiveButton(getString(R.string.player_ok),
    // new DialogInterface.OnClickListener() {
    //
    // @Override
    // public void onClick(DialogInterface dialog,
    // int whichButton) {
    // if (getCurrentTrack() != null) {
    // int newRating = Math.max((int) bar.getRating(),
    // 1);
    // sendRating(getCurrentTrack(), newRating);
    // }
    // }
    // });
    //
    // alert.setNegativeButton(getString(R.string.player_cancel), null);
    // alert.show();
    // }
    // }

    @Override
    protected boolean requestTrack() {
        mTrackQueue.get(new TrackQueue.Callback() {

            @Override
            public void callback(MusicItem item) {
                addToHistory(item);
                mTrackAdded = true;
                play(getHistory().get(0));
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
        Log.d(TAG, "RequestTrack returning: " + mTrackAdded);
        return mTrackAdded;
    }

    Button getDJBtn() {
        return mDjBtn;
    }

    protected boolean isDJ() {
        return mIsDJ;
    }

    private boolean setupServerCommBundleForDJ() {
        boolean complete = false;

        GroupsMainActivity activity = (GroupsMainActivity) getActivity();
        if (activity == null) {
            // this should never happen
            Log.d(TAG,
                    "Trying to get or release DJ seat while the activity was null! Aborting.");
            return complete;
        }
        mGid = activity.getGroupId();
        AppData data = AppData.getInstance(activity);
        mUid = data.getUid();
        mApi = data.getAPI();

        Location loc = data.getLocation();
        if (loc != null) {
            mLatitude = loc.getLatitude();
            mLongitude = loc.getLongitude();
        } else {
            mLatitude = DEFAULT_LATITUDE;
            mLongitude = DEFAULT_LONGITUDE;
            Log.i(TAG, "location was null, using default values");
        }
        complete = true;
        return complete;
    }

    private void toggleDJState(boolean isDJ, long gid) {
        if (isDJ) {
            if (gid == -1) {
                Log.d(TAG, "something went wrong when calling toggleDJState");
                return;
            }
            getDJBtn().setText(
                    getString(R.string.player_leave_dj));
            getToggleBtn().setBackgroundResource(
                    R.drawable.btn_play);
            getButtons().setVisibility(View.VISIBLE);
            getSeekBar().setVisibility(View.VISIBLE);
            getSeekBar().setEnabled(true);
            mTrackQueue = new TrackQueue(getActivity(), gid)
                    .start();
        } else {
            getDJBtn().setText(getString(R.string.player_become_dj));
            getButtons().setVisibility(View.INVISIBLE);
            getSeekBar().setVisibility(View.INVISIBLE);
            getSeekBar().setEnabled(false);
            getActivity().startService(
                    new Intent(MusicService.ACTION_STOP));
            setStatus(Status.Stopped);
        }
    }
}
