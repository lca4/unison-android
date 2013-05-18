
package ch.epfl.unison.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

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
import ch.epfl.unison.data.Playlist;
import ch.epfl.unison.data.UnisonDB;

import com.actionbarsherlock.view.MenuItem;

/*
 * TODO
 * 
 * - retrieve playlists stored in android database
 * - store playlist to android database 
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
    private ArrayList<Playlist> mPLsLocal;
    private ArrayList<Playlist> mPLsRemote;

    // GUI specific
    private ListView mPlaylistsListLocal;
    private ListView mPlaylistsListRemote;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setReloadInterval(RELOAD_INTERVAL);

        // mPlaylist = new Playlist();
        mDB = new UnisonDB(this);

        setContentView(R.layout.solo_playlists);
        ((Button) findViewById(R.id.createPlaylistBtn))
                .setOnClickListener(new OnCreatePlaylistListener());

        mPlaylistsListLocal = (ListView) findViewById(R.id.soloPlaylistsListLocal);
        mPlaylistsListLocal.setOnItemClickListener(new OnLocalPlaylistSelectedListener());
        registerForContextMenu(mPlaylistsListLocal);
        // TODO Load local PLs
        Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        String[] projection = new String[] {
                MediaStore.Audio.Playlists._ID,
                MediaStore.Audio.Playlists.NAME
        };
        Cursor cur = SoloPlaylistsActivity.this.getContentResolver().query(uri, projection,
                null, null, null);
        if (cur != null && cur.moveToFirst()) {
            int colId = cur.getColumnIndex(MediaStore.Audio.Playlists._ID);
            int colName = cur.getColumnIndex(MediaStore.Audio.Playlists.NAME);
            do {
                mPLsLocal.add(new Playlist.Builder().localId(cur.getInt(colId))
                        .title(cur.getString(colName)).build());
            } while (cur.moveToNext());
        } else {
            mPLsLocal = new ArrayList<Playlist>();
        }
        if (!cur.isClosed()) {
            cur.close();
        }
        mPLsRemote = new ArrayList<Playlist>();

        mPlaylistsListRemote = (ListView) findViewById(R.id.soloPlaylistsListRemote);
        mPlaylistsListRemote.setOnItemClickListener(new OnRemotePlaylistSelectedListener());
        registerForContextMenu(mPlaylistsListRemote);

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
        android.view.MenuInflater inflater = getMenuInflater();
        if (v == mPlaylistsListLocal) {
            inflater.inflate(R.menu.playlist_local_context_menu, menu);
        } else {
            inflater.inflate(R.menu.playlist_remote_context_menu, menu);
        }
    }

    // @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        ListView lv = (ListView) info.targetView.getParent();
        switch (item.getItemId()) {
            case R.id.playlist_context_menu_item_edit:
                return true;
            case R.id.playlist_context_menu_item_delete:
                return true;
            case R.id.playlist_context_menu_item_save:
                if (lv == mPlaylistsListRemote) {
                    Log.i(TAG, "Selected playlist has index : " + info.position);
                    savePlaylist(mPLsRemote.get(info.position));
                }
                return true;
            default:
                return super.onContextItemSelected((android.view.MenuItem) item);
        }
    }

    @Override
    public void onRefresh() {
        repaintRefresh(true);

        SoloPlaylistsActivity.this.mPlaylistsListLocal
                .setAdapter(new PlaylistsAdapter(mPLsLocal));

        // Update playlists
        UnisonAPI.Handler<JsonStruct.PlaylistsList> playlistsHandler =
                new UnisonAPI.Handler<JsonStruct.PlaylistsList>() {

                    @Override
                    public void callback(PlaylistsList struct) {
                        try {
                            if (struct.isEmtpy()) {
                                // TODO display row item to tell no playlist is
                                // available
                                Toast.makeText(SoloPlaylistsActivity.this,
                                        R.string.solo_playlists_noRemotePL,
                                        Toast.LENGTH_LONG).show();
                            } else {
                                mPLsRemote = struct.toObject();
                                SoloPlaylistsActivity.this.mPlaylistsListRemote
                                        .setAdapter(new PlaylistsAdapter(mPLsRemote));
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

                        // TODO remove after tests
                        // mDB.tagEmpty();

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
        data.getAPI().listPlaylists(data.getUid(), playlistsHandler);
        data.getAPI().listTags(data.getUid(), tagsHandler);
    }

    /** Adapter used to populate the ListView listing the playlists. */
    private class PlaylistsAdapter extends ArrayAdapter<Playlist> {

        public static final int ROW_LAYOUT = R.layout.list_row;

        public PlaylistsAdapter(ArrayList<Playlist> list) {
            super(SoloPlaylistsActivity.this, 0, list);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            Playlist playlist = getItem(position);
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
                            // switch (seedType) {
                            // case TAGS:
                            // PickTagsDialogFragment pickTagsDialog =
                            // new PickTagsDialogFragment();
                            // pickTagsDialog.show(getSupportFragmentManager(),
                            // "tags");
                            // break;
                            // case TRACKS:
                            // /*
                            // * TODO Checklist from lib_entries
                            // */
                            //
                            // break;
                            // default:
                            // // Should never happen
                            // break;
                            // }
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
                                    mPLsRemote.add(0, struct.toObject());
                                    SoloPlaylistsActivity.this.mPlaylistsListRemote
                                            .setAdapter(new PlaylistsAdapter(mPLsRemote));
                                    // SoloPlaylistsActivity.this.mPlaylistsListRemote
                                    // .setAdapter(new
                                    // PlaylistsAdapter(struct.toObject()));
                                } else {
                                    Log.i(TAG, "Playlist created, but could not be fetched...");
                                }
                            }

                            @Override
                            public void onError(Error error) {
                                Log.d(TAG, error.toString());
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

            SoloPlaylistsActivity.this.startActivity(new Intent(SoloPlaylistsActivity.this,
                    SoloMainActivity.class).putExtra(Const.Strings.PLID,
                    ((Playlist) view.getTag()).getPLId()).putExtra(Const.Strings.TITLE,
                    ((Playlist) view.getTag()).getTitle())); // .putExtra(Const.Strings.PLID,
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

            SoloPlaylistsActivity.this.startActivity(new Intent(SoloPlaylistsActivity.this,
                    SoloMainActivity.class).putExtra(Const.Strings.PLID,
                    ((Playlist) view.getTag()).getPLId()).putExtra(Const.Strings.TITLE,
                    ((Playlist) view.getTag()).getTitle())); // .putExtra(Const.Strings.PLID,
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

    private void savePlaylist(Playlist pl) {
        mDB.insert(pl);
//        onRefresh();
    }

}
