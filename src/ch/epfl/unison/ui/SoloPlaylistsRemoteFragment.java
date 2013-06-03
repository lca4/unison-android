
package ch.epfl.unison.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import ch.epfl.unison.AppData;
import ch.epfl.unison.Const;
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.data.PlaylistItem;
import ch.epfl.unison.data.UnisonDB;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * @author marc
 */
public class SoloPlaylistsRemoteFragment extends AbstractListFragment {
//    implements SoloPlaylistsActivity.OnPlaylistsRemoteInfoListener {
    
    /** Container Activity must implement this interface.  */
    public interface OnSavePlaylistListener {
        boolean onSavePlaylist(PlaylistItem playlist);
    }
    
    private OnSavePlaylistListener mListener;

    
    
    // Not really useful, but nice to avoid explicit casting every time
    private SoloPlaylistsActivity mHostActivity;

    private UnisonDB mDB;
    private ArrayList<PlaylistItem> mPlaylistsRemote;
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mHostActivity = (SoloPlaylistsActivity) activity;
        try {
            mListener = (OnSavePlaylistListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() 
                    + " must implement OnSavePlaylistListener");
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDB = new UnisonDB(getActivity());
//        mHostActivity.registerPlaylistsRemoteInfoListener(this);
        mPlaylistsRemote = new ArrayList<PlaylistItem>();
        initPlaylistsRemote();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
//        return super.onCreateView(inflater, container, savedInstanceState);
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.list_fragment, container, false);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getHostActivity().getMenuInflater();
        inflater.inflate(R.menu.playlist_local_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
//        ListView lv = (ListView) info.targetView.getParent();
        AppData data = AppData.getInstance(getHostActivity());
        switch (item.getItemId()) {
            case R.id.playlist_context_menu_item_edit:
                Log.i(getClassTag(), "Not yet implemented...");
                Toast.makeText(getHostActivity(),
                        R.string.error_not_yet_available,
                        Toast.LENGTH_LONG).show();
                return true;
                
            case R.id.playlist_context_menu_item_save:
                PlaylistItem pl = mPlaylistsRemote.get(info.position);
                // Adds PL to local databases
                long localId = mDB.insert(pl);
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
                
            case R.id.playlist_context_menu_item_delete:
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
    
//    @Override
//    public void onPlaylistInfo(PlaylistItem playlistInfo) {
//        if (playlistInfo.getTitle() != null) {
//            getTitle().setText(playlistInfo.getTitle());
//        }
//        getList().setAdapter(new PlaylistsAdapter(playlistInfo));
//    }

    /** ArrayAdapter that displays the tracks of the playlist. */
    private class PlaylistsAdapter extends ArrayAdapter<PlaylistItem> {

        public static final int ROW_LAYOUT = R.layout.list_row;

        public PlaylistsAdapter(ArrayList<PlaylistItem> playlists) {
            super(SoloPlaylistsRemoteFragment.this.getActivity(), 0, playlists);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            PlaylistItem pl = getItem(position);
            if (view == null) {
                LayoutInflater inflater =
                        (LayoutInflater) SoloPlaylistsRemoteFragment.this.getActivity()
                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(ROW_LAYOUT, parent, false);
            }
            ((TextView) view.findViewById(R.id.listrow_title))
                    .setText(getItem(position).getTitle());
            ((TextView) view.findViewById(R.id.listrow_subtitle))
                    .setText(String.valueOf(getItem(position).size()));
            // int rating = 0;
            // if (getItem(position).rating != null) {
            // rating = getItem(position).rating;
            // }
            // ((RatingBar) view.findViewById(R.id.trRating)).setRating(rating);
            view.setTag(pl);
            return view;
        }
    }

//    @Override
//    public void onPlaylistsRemoteInfo(Object contentInfo) {
//        // TODO Auto-generated method stub
//
//    }
    
    void addToPlRemote() {
        
    }

    /**
     * When clicking on a playlist, start MainActivity.
     */
    private class OnLocalPlaylistSelectedListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            // Give tracks to player here
            mHostActivity.startActivity(new Intent(mHostActivity,
                    SoloMainActivity.class)
                    .putExtra(Const.Strings.LOCAL_ID, ((PlaylistItem) view.getTag()).getLocalId())
                    .putExtra(Const.Strings.TITLE, ((PlaylistItem) view.getTag()).getTitle()));
            // .putExtra(Const.Strings.PLID,
            // view.getTag());
            // UnisonAPI api =
            // AppData.getInstance(SoloPlaylistsActivity.this).getAPI();
            // long uid =
            // AppData.getInstance(SoloPlaylistsActivity.this).getUid();
            // final JsonStruct.Playlist playlist = (JsonStruct.Playlist)
            // view.getTag();

            // api.joinGroup(uid, playlist.plid, new
            // UnisonAPI.Handler<JsonStruct.Success>() {
            //
            // @Override
            // public void callback(Success struct) {
            // SoloPlaylistsActivity.this.startActivity(
            // new Intent(SoloPlaylistsActivity.this, MainActivity.class)
            // .putExtra(Const.Strings.GID, playlist.plid)
            // .putExtra(Const.Strings.NAME, playlist.name));
            // }
            //
            // @Override
            // public void onError(Error error) {
            // Log.d(TAG, error.toString());
            // if (SoloPlaylistsActivity.this != null) {
            // Toast.makeText(SoloPlaylistsActivity.this,
            // R.string.error_joining_group,
            // Toast.LENGTH_LONG).show();
            // }
            // }
            //
            // });
        }
    }

    /**
     * To be used only once, at onCreate time.
     */
    private void initPlaylistsRemote() {

        refreshPlaylistsRemote();
    }

    /**
     * To be used to refresh the ListView when changes are made to ArraList.
     */
    private void refreshPlaylistsRemote() {
        //TODO update
//        mHostActivity.mPlaylistsLocalListView
//                .setAdapter(new PlaylistsAdapter(mPlaylistsLocal));
        if (isVisible()) {
            getListView().setAdapter(new PlaylistsAdapter(mPlaylistsRemote));
        }
    }
    
    /* ---------------------------------------
     * PUBLIC METHODS (used by SoloPlaylistsActivity)
     * --------------------------------------- */
    
    /**
     * 
     * @param playlist
     * @return always true
     */
    public boolean add(PlaylistItem playlist) {
        return mPlaylistsRemote.add(playlist);
    }
    
    /**
     * 
     * @param index
     * @param playlist
     */
    public void add(int index, PlaylistItem playlist) {
        mPlaylistsRemote.add(index, playlist);
    }
    
    /**
     * 
     * @param playlists
     */
    public void set(ArrayList<PlaylistItem> playlists) {
        mPlaylistsRemote = playlists;
        refreshPlaylistsRemote();
    }

}
