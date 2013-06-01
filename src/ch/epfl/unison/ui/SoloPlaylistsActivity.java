
package ch.epfl.unison.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import ch.epfl.unison.AppData;
import ch.epfl.unison.Const;
import ch.epfl.unison.Const.SeedType;
import ch.epfl.unison.LibraryService;
import ch.epfl.unison.R;
import ch.epfl.unison.Uutils;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.JsonStruct.PlaylistsList;
import ch.epfl.unison.api.JsonStruct.TagsList;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;
import ch.epfl.unison.data.PlaylistItem;
import ch.epfl.unison.data.UnisonDB;

/*
 * TODO
 * 
 * - retrieve playlists stored in android database
 * 
 */

/**
 * Listing of the groups.
 * 
 * @author marc bourqui
 */
public class SoloPlaylistsActivity extends AbstractFragmentActivity {
    // extends SherlockFragmentActivity implements UnisonMenu.OnRefreshListener
    // {

    private static final String TAG = "ch.epfl.unison.SoloPlaylistsActivity";
    private static final int RELOAD_INTERVAL = 120 * 1000; // in ms.

    // private Playlist mPlaylist;
    private UnisonDB mDB;
    private ArrayList<PlaylistItem> mPlaylistsLocal;
    private ArrayList<PlaylistItem> mPlaylistsRemote;

    // GUI specific
    private ListView mPlaylistsLocalListView;
    private ListView mPlaylistsRemoteListView;

    private final Uri mUri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
    private final String[] mPlaylistsIdNameProjection = new String[] {
            MediaStore.Audio.Playlists._ID,
            MediaStore.Audio.Playlists.NAME
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setReloadInterval(RELOAD_INTERVAL);

        // mPlaylist = new Playlist();
        mDB = new UnisonDB(this);

        setContentView(R.layout.solo_playlists);
        ((Button) findViewById(R.id.createPlaylistBtn))
                .setOnClickListener(new OnCreatePlaylistListener());

        mPlaylistsLocalListView = (ListView) findViewById(R.id.soloPlaylistsListLocal);
        mPlaylistsLocalListView.setOnItemClickListener(new OnLocalPlaylistSelectedListener());
        registerForContextMenu(mPlaylistsLocalListView);
        mPlaylistsLocal = new ArrayList<PlaylistItem>();
        initLocalPlaylists();

        mPlaylistsRemote = new ArrayList<PlaylistItem>();
        mPlaylistsRemoteListView = (ListView) findViewById(R.id.soloPlaylistsListRemote);
        mPlaylistsRemoteListView.setOnItemClickListener(new OnRemotePlaylistSelectedListener());
        registerForContextMenu(mPlaylistsLocalListView);
        registerForContextMenu(mPlaylistsRemoteListView);

        // // Actions that should be taken when activity is started.
        // if (ACTION_LEAVE_GROUP.equals(getIntent().getAction())) {
        // // We are coming back from a group - let's make sure the back-end
        // // knows.
        // leaveGroup();
        // } else if (AppData.getInstance(this).showHelpDialog()) {
        // showHelpDialog();
        // }
    }

    @Override
    public void onResume() {
        super.onResume();
        startService(new Intent(LibraryService.ACTION_UPDATE));
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        if (v == mPlaylistsLocalListView) {
            inflater.inflate(R.menu.playlist_local_context_menu, menu);
        } else {
            inflater.inflate(R.menu.playlist_remote_context_menu, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        ListView lv = (ListView) info.targetView.getParent();
        AppData data = AppData.getInstance(this);
        switch (item.getItemId()) {
            case R.id.playlist_context_menu_item_edit:
                if (lv == mPlaylistsLocalListView) {
                    Log.i(TAG, "Not yet implemented...");
                    Toast.makeText(SoloPlaylistsActivity.this,
                            R.string.error_not_yet_available,
                            Toast.LENGTH_LONG).show();
                }
                return true;

            case R.id.playlist_context_menu_item_delete:
                /*
                 * In the case of a local playlist, remove it from android and
                 * GS in-app DBs, but keeps it in the user library on GS server.
                 */
                if (lv == mPlaylistsLocalListView) {
                    try {
                        // Set local_id to null on GS server
                        data.getAPI().updatePlaylist(data.getUid(),
                                mPlaylistsLocal.get(info.position).getPLId(),
                                new JSONObject().put("local_id", JSONObject.NULL),
                                new UnisonAPI.Handler<JsonStruct.PlaylistJS>() {

                                    @Override
                                    public void callback(JsonStruct.PlaylistJS struct) {
                                        // Then from local databases
                                        if (mDB.delete(mPlaylistsLocal.get(info.position)) > 0) {
                                            mPlaylistsRemote.add(mPlaylistsLocal
                                                    .remove(info.position));
                                            Log.w(TAG, "Successfully removed playlist with id "
                                                    + struct.gsPlaylistId + " from user library");
                                            refreshPlaylistsLocal();
                                            refreshPlaylistsRemote();
                                        } else {
                                            Toast.makeText(
                                                    SoloPlaylistsActivity.this,
                                                    R.string
                                                    .error_solo_remove_playlist_from_local_dbs,
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    }

                                    @Override
                                    public void onError(UnisonAPI.Error error) {
                                        if (error != null) {
                                            Log.d(TAG, error.toString());
                                        }
                                        Toast.makeText(
                                                SoloPlaylistsActivity.this,
                                                R.string
                                                .error_solo_remove_playlist_from_local_dbs,
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (lv == mPlaylistsRemoteListView) {
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
                                public void onError(Error error) {
                                    if (error != null) {
                                        Log.d(TAG, error.toString());
                                    }
                                    Toast.makeText(SoloPlaylistsActivity.this,
                                            R.string.error_solo_remove_playlist_from_gs_server,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                }
                return true;

            case R.id.playlist_context_menu_item_save:
                if (lv == mPlaylistsRemoteListView) {
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
                                        public void onError(Error error) {
                                            Log.d(TAG, error.toString());
                                        }
                                    });
                        } catch (JSONException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        mPlaylistsLocal.add(mPlaylistsRemote.remove(info.position));
                        refreshPlaylistsLocal();
                        refreshPlaylistsRemote();
                    } else {
                        Toast.makeText(SoloPlaylistsActivity.this,
                                R.string.error_solo_save_playlist,
                                Toast.LENGTH_LONG).show();
                    }
                }
                return true;
            default:
                return super.onContextItemSelected((android.view.MenuItem) item);
        }
    }

    @Override
    public void onRefresh() {
        repaintRefresh(true);

        SoloPlaylistsActivity.this.mPlaylistsLocalListView
                .setAdapter(new PlaylistsAdapter(mPlaylistsLocal));

        // Update remote playlists
        UnisonAPI.Handler<JsonStruct.PlaylistsList> playlistsHandler =
                new UnisonAPI.Handler<JsonStruct.PlaylistsList>() {

                    @Override
                    public void callback(PlaylistsList struct) {
                        try {
                            if (struct.isEmtpy()) {
                                mPlaylistsRemote.clear();
                                // TODO display row item to tell no playlist is
                                // available
                                Toast.makeText(SoloPlaylistsActivity.this,
                                        R.string.solo_playlists_noRemotePL,
                                        Toast.LENGTH_LONG).show();
                            } else {
                                mPlaylistsRemote = struct.toObject();
                                // refreshRemotePlaylists();
                                SoloPlaylistsActivity.this.repaintRefresh(false);
                            }
                        } catch (NullPointerException e) {
                            Log.w(TAG, "playlist or activity is null?", e);
                        }
                    }

                    @Override
                    public void onError(UnisonAPI.Error error) {
                        if (error != null) {
                            Log.d(TAG, error.toString());
                        }
                        if (SoloPlaylistsActivity.this != null) {
                            Toast.makeText(SoloPlaylistsActivity.this,
                                    R.string.error_loading_playlists,
                                    Toast.LENGTH_LONG).show();
                            SoloPlaylistsActivity.this.repaintRefresh(false);
                        }
                    }
                };

        // Update tags
        UnisonAPI.Handler<JsonStruct.TagsList> tagsHandler =
                new UnisonAPI.Handler<JsonStruct.TagsList>() {

                    @Override
                    public void callback(TagsList struct) {
                        for (int i = 0; i < struct.tags.length; i++) {
                            mDB.insert(struct.tags[i].getTagItem());
                        }
                    }

                    @Override
                    public void onError(UnisonAPI.Error error) {
                        if (error != null) {
                            Log.d(TAG, error.toString());
                        }
                        if (SoloPlaylistsActivity.this != null) {
                            Toast.makeText(SoloPlaylistsActivity.this,
                                    R.string.error_loading_tags,
                                    Toast.LENGTH_LONG).show();
                            SoloPlaylistsActivity.this.repaintRefresh(false);
                        }
                    }
                };

        AppData data = AppData.getInstance(this);
        data.getAPI().listUserPlaylists(data.getUid(), playlistsHandler);
        data.getAPI().listTopTags(data.getUid(), tagsHandler);
    }

    /** Adapter used to populate the ListView listing the playlists. */
    private class PlaylistsAdapter extends ArrayAdapter<PlaylistItem> {

        public static final int ROW_LAYOUT = R.layout.list_row;

        public PlaylistsAdapter(ArrayList<PlaylistItem> list) {
            super(SoloPlaylistsActivity.this, 0, list);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            PlaylistItem playlist = getItem(position);
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) SoloPlaylistsActivity.this
                        .getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(ROW_LAYOUT, parent, false);
            }
            ((TextView) view.findViewById(R.id.lr_title)).setText(playlist.getTitle());
            String subtitle = null;
            if (playlist.getListeners() > 0) {
                String plural = "s";
                if (playlist.getListeners() == 1) {
                    plural = "";
                }
                subtitle = String.format("%d tracks - %d listener" + plural, playlist.getSize(),
                        playlist.getListeners());
            } else {
                subtitle = String.format("%d tracks", playlist.getSize());
            }
            ((TextView) view.findViewById(R.id.lr_notifRight)).setText(subtitle);

            view.setTag(playlist);
            return view;
        }
    }

    /**
     * When clicking on "create new group", trigger an AlertView that asks for a
     * group name and creates the group on the back-end through the API.
     */
    private class OnCreatePlaylistListener implements OnClickListener {

        @Override
        public void onClick(View v) {

            /*
             * TODO allow user to specify title. In order to achieve this, use a
             * custom view, like the one found on
             * http://prativas.wordpress.com/category
             * /android/expandable-list-view-in-android/
             */

            PickSeedDialogFragment pickSeedDialog = new PickSeedDialogFragment();
            pickSeedDialog.show(getSupportFragmentManager(), "seedTypes");

            // AlertDialog.Builder alert = new
            // AlertDialog.Builder(SoloPlaylistsActivity.this);
            //
            // alert.setTitle(getString(R.string.solo_playlists_alert_newplaylist_title));
            // alert.setMessage(getString(R.string.solo_playlists_alert_newplaylist_message));
            //
            // // Set an EditText view to get user input
            // final EditText input = new EditText(SoloPlaylistsActivity.this);
            // alert.setView(input);
            //
            // // When clicking on "OK", create the group.
            // alert.setPositiveButton(getString(R.string.solo_playlists_alert_newplaylist_ok),
            // new DialogInterface.OnClickListener() {
            //
            // @Override
            // public void onClick(DialogInterface dialog, int whichButton) {
            // String name = input.getText().toString().trim();
            // OnCreatePlaylistListener.this.createPlaylist(name);
            // }
            // });
            //
            // alert.setNegativeButton(getString(R.string.solo_playlists_alert_newplaylist_cancel),
            // null);
            // alert.show();
        }

        /**
         * @author marc
         */
        @SuppressLint({
                "ValidFragment", "NewApi"
        })
        private class PickSeedDialogFragment extends android.support.v4.app.DialogFragment {

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.solo_playlists_dialog_pick_seedtype);
                builder.setItems(R.array.seedtypes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        Resources res = getResources();
                        TypedArray seedsTypes = res.obtainTypedArray(R.array.seedtypes);
                        String seed = seedsTypes.getString(which);
                        seedsTypes.recycle();
                        if (seed != null) {
                            SeedType seedType = SeedType.getSeedType(seed);
                            PickItemsDialogFragment pickItemsDialog = new PickItemsDialogFragment(
                                    seedType);
                            if (pickItemsDialog != null) {
                                pickItemsDialog.show(getFragmentManager(), seed);
                            }
                        }
                    }
                });
                return builder.create();
            }
        }

        /**
         * @author marc
         */
        @SuppressLint({
                "ValidFragment", "NewApi"
        })
        private class PickItemsDialogFragment extends android.support.v4.app.DialogFragment {
            private SeedType mType;
            private ArrayList<Integer> mSelectedItems;
            private LinkedHashMap<String, Integer> mItems;

            public PickItemsDialogFragment(SeedType type) {
                this.mType = type;
                this.mSelectedItems = new ArrayList<Integer>();
                switch (mType) {
                    case TAGS:
                        mItems = mDB.getTags();
                        break;
                    case TRACKS:
                        mItems = mDB.getLibEntries();
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                if (mItems != null) {
                    final boolean[] checkedItems = new boolean[mItems.size()];
                    for (int i = 0; i < checkedItems.length; i++) {
                        checkedItems[i] = false;
                    }
                    List<CharSequence> items = new ArrayList<CharSequence>();
                    Set<String> keys = mItems.keySet();
                    Iterator<String> it = keys.iterator();
                    while (it.hasNext()) {
                        items.add(it.next());
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            SoloPlaylistsActivity.this);
                    builder.setTitle(R.string.solo_playlists_dialog_pick_seeds)
                            .setPositiveButton(R.string.generic_ok,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                            Log.i(TAG, mSelectedItems.toString());
                                            mDB.setChecked(mType, mItems, checkedItems);
                                            OnCreatePlaylistListener.this.generatePlaylist();
                                        }
                                    })
                            .setNegativeButton(R.string.generic_cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                            mSelectedItems.clear();
                                            mItems.clear();
                                        }
                                    })
                            .setMultiChoiceItems(items.toArray(new CharSequence[items.size()]),
                                    checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                            if (isChecked) {
                                                checkedItems[which] = true;
                                                mSelectedItems.add(which);
                                            } else if
                                            (mSelectedItems.contains(which)) {
                                                // Else, if the item is already
                                                // in
                                                // the array, remove it
                                                checkedItems[which] = false;
                                                mSelectedItems.remove(Integer.valueOf(which));
                                            }
                                        }
                                    });
                    return builder.create();
                } else {
                    return null;
                }
            }
        }

        /**
         * Creates a playlist on the back-end. If it succeeds, the ListView
         * containing the list of playlists is updated. If it fails, a toast
         * notification is shown.
         */
        @SuppressLint("NewApi")
        protected void generatePlaylist() {
            JSONObject seeds = Uutils.merge(mDB.getCheckedItems(SeedType.TAGS),
                    mDB.getCheckedItems(SeedType.TRACKS));

            if (seeds != null) {
                AppData data = AppData.getInstance(SoloPlaylistsActivity.this);
                JSONObject options = new JSONObject();
                data.getAPI().generatePlaylist(data.getUid(), seeds, options,
                        new UnisonAPI.Handler<JsonStruct.PlaylistJS>() {
                            @Override
                            public void callback(JsonStruct.PlaylistJS struct) {
                                if (struct != null) {
                                    Log.i(TAG, "Playlist created!");
                                    mPlaylistsRemote.add(0, struct.toObject());
                                    SoloPlaylistsActivity.this.mPlaylistsRemoteListView
                                            .setAdapter(new PlaylistsAdapter(mPlaylistsRemote));
                                    // SoloPlaylistsActivity.this.mPlaylistsListRemote
                                    // .setAdapter(new
                                    // PlaylistsAdapter(struct.toObject()));
                                } else {
                                    Log.i(TAG, "Playlist created, but could not be fetched...");
                                }
                            }

                            @Override
                            public void onError(Error error) {
                                if (error != null) {
                                    Log.d(TAG, error.toString());
                                }
                                if (SoloPlaylistsActivity.this != null) {
                                    Toast.makeText(SoloPlaylistsActivity.this,
                                            R.string.error_creating_playlist,
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        }

    }

    /**
     * When clicking on a playlist, start MainActivity.
     */
    private class OnRemotePlaylistSelectedListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            // TODO on click, display list of tracks

            // SoloPlaylistsActivity.this.startActivity(
            // new Intent(SoloPlaylistsActivity.this, SoloMainActivity.class)
            // .putExtra(Const.Strings.LOCAL_ID,
            // ((PlaylistItem) view.getTag()).getLocalId())
            // .putExtra(Const.Strings.TITLE,
            // ((PlaylistItem) view.getTag()).getTitle())
            // );
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
     * When clicking on a playlist, start MainActivity.
     */
    private class OnLocalPlaylistSelectedListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            // Give tracks to player here
            SoloPlaylistsActivity.this.startActivity(new Intent(SoloPlaylistsActivity.this,
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
        Cursor cur = SoloPlaylistsActivity.this.getContentResolver().query(mUri,
                mPlaylistsIdNameProjection,
                null, null, null);
        if (cur != null && cur.moveToFirst()) {
            int colId = cur.getColumnIndex(MediaStore.Audio.Playlists._ID);
            int colName = cur.getColumnIndex(MediaStore.Audio.Playlists.NAME);
            do {
                PlaylistItem pl = (PlaylistItem) mDB.getItem(PlaylistItem.class, cur.getInt(colId));
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
        SoloPlaylistsActivity.this.mPlaylistsLocalListView
                .setAdapter(new PlaylistsAdapter(mPlaylistsLocal));
    }

    private void refreshPlaylistsRemote() {
        SoloPlaylistsActivity.this.mPlaylistsRemoteListView
                .setAdapter(new PlaylistsAdapter(mPlaylistsRemote));
    }
}
