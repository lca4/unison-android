
package ch.epfl.unison.ui;

import android.app.Activity;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ch.epfl.unison.AppData;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.data.MusicItem;
import ch.epfl.unison.data.PlaylistItem;
import ch.epfl.unison.music.MusicService;
import ch.epfl.unison.ui.SoloPlaylistsRemoteFragment.OnPlaylistsRemoteListener;

/**
 * Specialized Fragment for {@link SoloMainFragment}.
 * 
 * @see AbstractMainActivity
 * @author marc
 */
public class SoloPlayerFragment extends AbstractPlayerFragment {
    
    /** Container Activity must implement this interface. */
    public interface OnSoloPlayerListener {
        void onTrackChange();
        void setPlayerFragmentTag(String tag);
    }

    private SoloMainActivity mHostActivity;
    private OnSoloPlayerListener mListener;
    
    // /**
    // * Handles instant ratings (when the user clicks on the rating button in
    // the
    // * player interface).
    // */
    // private class OnRatingClickListener implements OnClickListener {
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
    //
    // private void sendRating(MusicItem item, int rating) {
    // Log.d(TAG, String.format("artist: %s, title: %s, rating: %d",
    // item.artist, item.title, rating));
    //
    // UnisonAPI api = AppData.getInstance(getActivity()).getAPI();
    // // api.instantRate(mActivity.getGroupId(), item.artist, item.title,
    // // rating,
    // // new UnisonAPI.Handler<JsonStruct.Success>() {
    // // @Override
    // // public void callback(JsonStruct.Success struct) { }
    // //
    // // @Override
    // // public void onError(Error error) {
    // // Log.d(TAG, error.toString());
    // // if (getActivity() != null) {
    // // Toast.makeText(getActivity(),
    // // R.string.error_sending_rating, Toast.LENGTH_LONG).show();
    // // }
    // // }
    // // });
    // }
    // }

    // private static final String TAG = "ch.epfl.unison.SoloPlayerFragment";

    @Override
    protected void notifyPlay(MusicItem item) {
        UnisonAPI api = AppData.getInstance(getMainActivity()).getAPI();
        // api.setCurrentTrack(mActivity.getGroupId(), item.artist,
        // item.title, new UnisonAPI.Handler<JsonStruct.Success>() {
        //
        // @Override
        // public void callback(JsonStruct.Success struct) {
        // // Automatically refresh the content (in particular, to
        // // get the cover art).
        // mActivity.onRefresh();
        // }
        //
        // @Override
        // public void onError(Error error) {
        // Log.d(TAG, error.toString());
        // if (getActivity() != null) {
        // Toast.makeText(getActivity(),
        // R.string.error_sending_track,
        // Toast.LENGTH_LONG).show();
        // }
        // }
        // });
    }

    @Override
    protected void notifySkip() {
        UnisonAPI api = AppData.getInstance(getMainActivity()).getAPI();
        // api.skipTrack(mActivity.getGroupId(),
        // new UnisonAPI.Handler<JsonStruct.Success>() {
        // @Override
        // public void callback(JsonStruct.Success struct) {
        // }
        //
        // @Override
        // public void onError(Error error) {
        // Log.d(TAG, error.toString());
        // }
        // });
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mHostActivity = (SoloMainActivity) activity;
        try {
            mListener = (SoloMainActivity) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSavePlaylistListener");
        }
        String tag = SoloPlayerFragment.this.getTag();
        mListener.setPlayerFragmentTag(tag);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        setHistory(null); // TODO fetch a real playlist
        return v;
    }

    @Override
    protected boolean requestTrack() {
        /*
         * Since the playlist is generated once and all the tracks are known
         * before playing, there is no need to request for new tracks on the
         * fly.
         */
        return false;
    }

    @Override
    protected void next() {
        try {
            play(mHostActivity.getPlaylist().next());
            mListener.onTrackChange();
        } catch (NullPointerException npe) {
            Log.i(getTag(), "next: " + npe.getMessage());
        } catch (IndexOutOfBoundsException ioobe) {
            Log.i(getTag(), "next: " + ioobe.getMessage());
        }
    }
}
