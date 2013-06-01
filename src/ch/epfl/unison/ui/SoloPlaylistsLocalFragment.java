
package ch.epfl.unison.ui;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

/**
 * Shows the locally stored playlists made with GS.
 * 
 * @author marc
 */
public class SoloPlaylistsLocalFragment extends AbstractListFragment implements
        SoloPlaylistsActivity.OnPlaylistsLocalInfoListener {

    // Not really useful, but nice to avoid explicit casting every time
    private SoloPlaylistsActivity mHostActivity;

    private UnisonDB mDB;
    private ArrayList<PlaylistItem> mPlaylistsLocal;
    
    private final Uri mUri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
    private final String[] mPlaylistsIdNameProjection = new String[] {
            MediaStore.Audio.Playlists._ID,
            MediaStore.Audio.Playlists.NAME
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // mHostActivity = (SoloPlaylistsActivity) activity;
        mHostActivity = (SoloPlaylistsActivity) getActivity();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDB = new UnisonDB(getActivity());
        mHostActivity.registerPlaylistsLocalInfoListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
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

            case R.id.playlist_context_menu_item_delete:
                /*
                 * In the case of a local playlist, remove it from android and
                 * GS in-app DBs, but keeps it in the user library on GS server.
                 */
                try {
                    // Set local_id to null on GS server
                    data.getAPI().updatePlaylist(data.getUid(),
                            mHostActivity.getPlaylistsLocal()
                                    .get(info.position).getPLId(),
                            new JSONObject().put("local_id", JSONObject.NULL),
                            new UnisonAPI.Handler<JsonStruct.PlaylistJS>() {

                                @Override
                                public void callback(JsonStruct.PlaylistJS struct) {
                                    // Then from local databases
                                    if (mHostActivity.getDB().delete(
                                            ((SoloPlaylistsActivity) getHostActivity())
                                                    .getPlaylistsLocal()
                                                    .get(info.position)) > 0) {
                                        mHostActivity
                                                .getPlaylistsRemote().add(
                                                        ((SoloPlaylistsActivity) getHostActivity())
                                                                .getPlaylistsLocal()
                                                                .remove(info.position));
                                        Log.w(getClassTag(),
                                                "Successfully removed playlist with id "
                                                        + struct.gsPlaylistId
                                                        + " from user library");
                                        // refreshPlaylistsLocal();
                                    } else {
                                        Toast.makeText(
                                                getHostActivity(),
                                                R.string
                                                .error_solo_remove_playlist_from_local_dbs,
                                                Toast.LENGTH_LONG).show();
                                    }
                                }

                                @Override
                                public void onError(UnisonAPI.Error error) {
                                    Log.d(getClassTag(), error.toString());
                                    Toast.makeText(
                                            mHostActivity,
                                            R.string
                                            .error_solo_remove_playlist_from_local_dbs,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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
            super(SoloPlaylistsLocalFragment.this.getActivity(), 0, playlists);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            PlaylistItem pl = getItem(position);
            if (view == null) {
                LayoutInflater inflater =
                        (LayoutInflater) SoloPlaylistsLocalFragment.this.getActivity()
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

    @Override
    public void onPlaylistsLocalInfo(Object contentInfo) {
        // TODO Auto-generated method stub

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
    private void initLocalPlaylists() {
        Cursor cur = mHostActivity.getContentResolver().query(mUri,
                mPlaylistsIdNameProjection,
                null, null, null);
        if (cur != null && cur.moveToFirst()) {
            int colId = cur.getColumnIndex(MediaStore.Audio.Playlists._ID);
            int colName = cur.getColumnIndex(MediaStore.Audio.Playlists.NAME);
            do {
                int size = mHostActivity.getDB().getTracksCount(
                        cur.getLong(colId));
                PlaylistItem pl = null; // TODO
                if (pl != null) {
                    pl.setTitle(cur.getString(colName));
                    mPlaylistsLocal.add(pl);
                }
                // Non-GS playlists not shown
            } while (cur.moveToNext());
            cur.close();
        }
        refreshPlaylistsLocal();
    }

    /**
     * To be used to refresh the ListView when changes are made to ArraList.
     */
    private void refreshPlaylistsLocal() {
        //TODO update
//        mHostActivity.mPlaylistsLocalListView
//                .setAdapter(new PlaylistsAdapter(mPlaylistsLocal));
    }
}
