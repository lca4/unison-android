
package ch.epfl.unison.ui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import ch.epfl.unison.AppData;
import ch.epfl.unison.R;
import ch.epfl.unison.Uutils;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.data.PlaylistItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * @author marc
 */
public class SoloPlaylistsRemoteFragment extends AbstractListFragment {

    /** Container Activity must implement this interface. */
    public interface OnPlaylistsRemoteListener {
        boolean onSavePlaylist(PlaylistItem playlist);
        void setPlaylistsRemoteFragmentTag(String tag);
    }

    private SoloPlaylistsActivity mHostActivity;
    private OnPlaylistsRemoteListener mListener;

    private ArrayList<PlaylistItem> mPlaylistsRemote;
//    private PlaylistsAdapter mAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mHostActivity = (SoloPlaylistsActivity) activity;
        try {
            mListener = (OnPlaylistsRemoteListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSavePlaylistListener");
        }
        String tag = SoloPlaylistsRemoteFragment.this.getTag();
        mListener.setPlaylistsRemoteFragmentTag(tag);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mPlaylistsRemote = new ArrayList<PlaylistItem>();
        View v = super.onCreateView(inflater, container, savedInstanceState);
        SoloPlaylistsRemoteFragment.this
        .setListAdapter(new Uutils.Adapters.PlaylistsAdapter(mHostActivity, mPlaylistsRemote));
        return v;
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        registerForContextMenu(getListView());
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshPlaylistsRemote();
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = mHostActivity.getMenuInflater();
        inflater.inflate(R.menu.solo_playlists_context_menu, menu);
        menu.findItem(R.id.solo_playlists_context_menu_item_save).setVisible(true);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        // @see http://stackoverflow.com/a/10125761 to learn more about this hack
        if (!getUserVisibleHint()) {
            return super.onContextItemSelected((android.view.MenuItem) item);
        }
        super.onContextItemSelected(item);
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        AppData data = AppData.getInstance(mHostActivity);
        switch (item.getItemId()) {
            case R.id.solo_playlists_context_menu_item_edit:
                Log.i(getClassTag(), "Not yet implemented...");
                Toast.makeText(mHostActivity,
                        R.string.error_not_yet_available,
                        Toast.LENGTH_LONG).show();
                return true;

            case R.id.solo_playlists_context_menu_item_save:
                PlaylistItem pl = mPlaylistsRemote.get(info.position);
                // Adds PL to local databases
                long localId = mHostActivity.getDB().insert(pl);
                if (localId >= 0) {
                    try {
                        // Updates the local_id on server
                        JSONObject json = new JSONObject();
                        json.put("local_id", localId);
                        data.getAPI().updatePlaylist(data.getUid(), pl.getPLId(), json,
                                new UnisonAPI.Handler<JsonStruct.PlaylistJS>() {

                                    @Override
                                    public void callback(JsonStruct.PlaylistJS struct) {
                                        // TODO some verifications?
                                        // (local_id)
                                    }

                                    @Override
                                    public void onError(UnisonAPI.Error error) {
                                        Log.d(getClassTag(), error.toString());
                                    }
                                });
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    mListener.onSavePlaylist(mPlaylistsRemote.remove(info.position));
                    // refreshPlaylistsLocal();
                    refreshPlaylistsRemote();
                } else {
                    Toast.makeText(mHostActivity,
                            R.string.error_solo_save_playlist,
                            Toast.LENGTH_LONG).show();
                }
                return true;

            case R.id.solo_playlists_context_menu_item_delete:
                /*
                 * In the case of a local playlist, remove it from android and
                 * GS in-app DBs, but keeps it in the user library on GS server.
                 */
                // Remove from server
                data.getAPI().removePlaylist(data.getUid(),
                        mPlaylistsRemote.get(info.position).getPLId(),
                        new UnisonAPI.Handler<JsonStruct.Success>() {

                            @Override
                            public void callback(JsonStruct.Success struct) {
                                mPlaylistsRemote.remove(info.position);
                                refreshPlaylistsRemote();
                            }

                            @Override
                            public void onError(UnisonAPI.Error error) {
                                Log.d(getClassTag(), error.toString());
                                Toast.makeText(mHostActivity,
                                        R.string.error_solo_remove_playlist_from_gs_server,
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                return true;

            default:
                return super.onContextItemSelected((android.view.MenuItem) item);
        }
    }

//    /** ArrayAdapter that displays the tracks of the playlist. */
//    private class PlaylistsAdapter extends ArrayAdapter<PlaylistItem> {
//
//        public static final int ROW_LAYOUT = R.layout.list_row;
//        
//        public PlaylistsAdapter(ArrayList<PlaylistItem> playlists) {
//            super(SoloPlaylistsRemoteFragment.this.getActivity(), 0, playlists);
//        }
//
//        @Override
//        public View getView(int position, View view, ViewGroup parent) {
//            PlaylistItem pl = (PlaylistItem) getItem(position);
//            if (view == null) {
//                LayoutInflater inflater =
//                        (LayoutInflater) SoloPlaylistsRemoteFragment.this.getActivity()
//                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//                view = inflater.inflate(ROW_LAYOUT, parent, false);
//            }
//            ((TextView) view.findViewById(R.id.listrow_title))
//                    .setText((getItem(position)).getTitle());
//            int size = (int) getItem(position).size();
//            String subtitle = getString(R.string.default_empty);
//            switch (size) {
//                case 0:
//                    subtitle = getString(R.string.solo_playlist_contains_no_track);
//                    break;
//                case 1:
//                    subtitle = getString(R.string.solo_playlist_contains_track);
//                default:
//                    subtitle = getString(R.string.solo_playlist_contains_tracks, size);
//                    break;
//            }
//            ((TextView) view.findViewById(R.id.listrow_subtitle))
//                    .setText(subtitle);
//            view.setTag(pl);
//            return view;
//        }
//    }

//    /**
//     * When clicking on a playlist, start MainActivity.
//     */
//    private class OnLocalPlaylistSelectedListener implements OnItemClickListener {
//
//        @Override
//        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//
//            // Give tracks to player here
//            mHostActivity.startActivity(new Intent(mHostActivity,
//                    SoloMainActivity.class)
//                    .putExtra(Const.Strings.LOCAL_ID, ((PlaylistItem) view.getTag()).getLocalId())
//                    .putExtra(Const.Strings.TITLE, ((PlaylistItem) view.getTag()).getTitle()));
//            // .putExtra(Const.Strings.PLID,
//            // view.getTag());
//            // UnisonAPI api =
//            // AppData.getInstance(SoloPlaylistsActivity.this).getAPI();
//            // long uid =
//            // AppData.getInstance(SoloPlaylistsActivity.this).getUid();
//            // final JsonStruct.Playlist playlist = (JsonStruct.Playlist)
//            // view.getTag();
//
//            // api.joinGroup(uid, playlist.plid, new
//            // UnisonAPI.Handler<JsonStruct.Success>() {
//            //
//            // @Override
//            // public void callback(Success struct) {
//            // SoloPlaylistsActivity.this.startActivity(
//            // new Intent(SoloPlaylistsActivity.this, MainActivity.class)
//            // .putExtra(Const.Strings.GID, playlist.plid)
//            // .putExtra(Const.Strings.NAME, playlist.name));
//            // }
//            //
//            // @Override
//            // public void onError(Error error) {
//            // Log.d(TAG, error.toString());
//            // if (SoloPlaylistsActivity.this != null) {
//            // Toast.makeText(SoloPlaylistsActivity.this,
//            // R.string.error_joining_group,
//            // Toast.LENGTH_LONG).show();
//            // }
//            // }
//            //
//            // });
//        }
//    }

    /**
     * To be used only once, at onCreate time.
     * TODO Use this to display an row 
     * telling the list is empty.
     */
    private void initPlaylistsRemote() {
        refreshPlaylistsRemote();
    }

    /**
     * To be used to refresh the ListView when changes are made to ArraList.
     */
    private void refreshPlaylistsRemote() {
         setListAdapter(new Uutils.Adapters.PlaylistsAdapter(mHostActivity, mPlaylistsRemote));

    }

//    public void refreshPlaylistsRemote(Activity activity) {
//        setListAdapter(new PlaylistsAdapter(activity, mPlaylistsRemote));
//    }

    /*
     * ---------------------------------------
     * PUBLIC METHODS (used by SoloPlaylistsActivity)
     * ---------------------------------------
     */

    /**
     * @param playlist
     * @return always true
     */
    public boolean add(PlaylistItem playlist) {
        boolean b = mPlaylistsRemote.add(playlist);
        refreshPlaylistsRemote();
        return b;
    }

    /**
     * @param index
     * @param playlist
     */
    public void add(int index, PlaylistItem playlist) {
        mPlaylistsRemote.add(index, playlist);
        refreshPlaylistsRemote();
    }

    /**
     * Replaces the list of playlists by the <code>playlists</code>.
     * 
     * @param activity
     * @param playlists
     */
//    public void set(Activity activity, ArrayList<PlaylistItem> playlists) {
//        mPlaylistsRemote = playlists;
//        refreshPlaylistsRemote(activity);
//    }
    
    /**
     * Replaces the list of playlists by the <code>playlists</code>.
     * 
     * @param playlists
     */
    public void set(ArrayList<PlaylistItem> playlists) {
        mPlaylistsRemote = playlists;
        refreshPlaylistsRemote();
    }

}
