
package ch.epfl.unison.ui;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;
import ch.epfl.unison.AppData;
import ch.epfl.unison.Const;
import ch.epfl.unison.R;
import ch.epfl.unison.Uutils;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.data.PlaylistItem;

/**
 * Shows the locally stored playlists made with GS.
 * 
 * @author marc
 */
public class SoloPlaylistsLocalFragment extends AbstractListFragment {

    /** Container Activity must implement this interface. */
    public interface OnPlaylistsLocalListener {
        boolean onDeletePlaylist(PlaylistItem playlist);

        void setPlaylistsLocalFragmentTag(String tag);
    }

    private SoloPlaylistsActivity mHostActivity;
    private OnPlaylistsLocalListener mListener;

    private ArrayList<PlaylistItem> mPlaylistsLocal;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mHostActivity = (SoloPlaylistsActivity) activity;
        try {
            mListener = (OnPlaylistsLocalListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSavePlaylistListener");
        }
        String tag = SoloPlaylistsLocalFragment.this.getTag();
        mListener.setPlaylistsLocalFragmentTag(tag);
        mPlaylistsLocal = new ArrayList<PlaylistItem>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        mPlaylistsLocal = new ArrayList<PlaylistItem>();
        View v = super.onCreateView(inflater, container, savedInstanceState);
        initPlaylistsLocal();
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setOnItemClickListener(new OnLocalPlaylistSelectedListener());
        registerForContextMenu(getListView());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = mHostActivity.getMenuInflater();
        inflater.inflate(R.menu.solo_playlists_context_menu, menu);
        // menu.findItem(R.id.solo_playlists_context_menu_item_edit).setVisible(true);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        // @see http://stackoverflow.com/a/10125761 to learn more about this
        // hack
        if (!getUserVisibleHint()) {
            return super.onContextItemSelected((android.view.MenuItem) item);
        }
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

            case R.id.solo_playlists_context_menu_item_delete:
                /*
                 * In the case of a local playlist, remove it from android and
                 * GS in-app DBs, but keeps it in the user library on GS server.
                 */
                try {
                    // Set local_id to null on GS server
                    data.getAPI().updatePlaylist(data.getUid(),
                            mPlaylistsLocal
                                    .get(info.position).getPLId(),
                            new JSONObject().put("local_id", JSONObject.NULL),
                            new UnisonAPI.Handler<JsonStruct.PlaylistJS>() {

                                @Override
                                public void callback(JsonStruct.PlaylistJS struct) {
                                    // Then from local databases
                                    if (mHostActivity.getDB().delete(
                                            mPlaylistsLocal
                                                    .get(info.position)) > 0) {
                                        mListener.onDeletePlaylist(
                                                mPlaylistsLocal
                                                        .remove(info.position));
                                        Log.w(getClassTag(),
                                                "Successfully removed playlist with id "
                                                        + struct.gsPlaylistId
                                                        + " from user library");
                                        refreshPlaylistsLocal();
                                    } else {
                                        Toast.makeText(
                                                mHostActivity,
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
                } catch (IndexOutOfBoundsException ioobe) {
                    ioobe.printStackTrace();
                }
                return true;

            default:
                return super.onContextItemSelected((android.view.MenuItem) item);
        }
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
        }
    }

    /**
     * To be used only once, at onCreate time.
     */
    private void initPlaylistsLocal() {
        final Uri mUri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        final String[] mPlaylistsIdNameProjection = new String[] {
                MediaStore.Audio.Playlists._ID,
                MediaStore.Audio.Playlists.NAME
        };
        Cursor cur = mHostActivity.getContentResolver().query(mUri,
                mPlaylistsIdNameProjection,
                null, null, null);
        if (cur != null && cur.moveToFirst()) {
            int colId = cur.getColumnIndex(MediaStore.Audio.Playlists._ID);
            int colName = cur.getColumnIndex(MediaStore.Audio.Playlists.NAME);
            do {
                long localId = cur.getLong(colId);
                final AppData data = AppData.getInstance(mHostActivity);
                if (mHostActivity.getDB().isMadeWithGS(localId, data.getUid())) {
                    int size = mHostActivity.getDB().getTracksCount(localId);
                    PlaylistItem pl = new PlaylistItem.Builder().localId(localId)
                            .size(size).build();
                    if (pl != null) {
                        pl.setTitle(cur.getString(colName));
                        mPlaylistsLocal.add(pl);
                    }
                }
                // Non-GS playlists are not shown
            } while (cur.moveToNext());
            cur.close();
        }
        refreshPlaylistsLocal();
    }

    /**
     * To be used to refresh the ListView when changes are made to ArraList.
     */
    private void refreshPlaylistsLocal() {
        setListAdapter(new Uutils.Adapters.PlaylistsAdapter(mHostActivity, mPlaylistsLocal));
    }

    /*
     * -------------------------------------------------------------------------
     * PUBLIC METHODS (used by SoloPlaylistsActivity)
     * -------------------------------------------------------------------------
     */

    /**
     * @param playlist
     * @return always true
     */
    public boolean add(PlaylistItem playlist) {
        boolean b = mPlaylistsLocal.add(playlist);
        refreshPlaylistsLocal();
        return b;
    }
}
