
package ch.epfl.unison.ui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import ch.epfl.unison.AppData;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.data.MusicItem;

/**
 * Specialized Fragment for {@link SoloMainFragment}. Contains the music player.
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

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mHostActivity = (SoloMainActivity) activity;
        // Ensure the host implements the interface
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
        // setHistory(null); // TODO fetch a real playlist
        return v;
    }

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

    protected void prev() {
        try {
            if (getCurrentPosition() < getClickInterval()) {
                play(mHostActivity.getPlaylist().previous());
            } else {
                play(mHostActivity.getPlaylist().current());
            }
        } catch (NullPointerException npe) {
            // Internal error occured
            Log.i(getTag(), "prev: internal error : playlist is null.");
        } catch (IndexOutOfBoundsException ioobe) {
            // TODO Else, display error message
            Log.i(getTag(), "prev: no track found to play.");
        }
    }

    @Override
    protected void notifyPlay(MusicItem item) {
        UnisonAPI api = AppData.getInstance(mHostActivity).getAPI();
        // TODO tell the server to increment the listener counter
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
        UnisonAPI api = AppData.getInstance(mHostActivity).getAPI();
        // TODO tell the server to decrement the listener counter
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
}
