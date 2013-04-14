
package ch.epfl.unison.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.util.Log;
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
import ch.epfl.unison.Const.SeedType;
import ch.epfl.unison.LibraryService;
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.JsonStruct.PlaylistsList;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

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
public class SoloPlaylistsActivity extends SherlockFragmentActivity
        implements UnisonMenu.OnRefreshListener {

    private static final String TAG = "ch.epfl.unison.SoloPlaylistsActivity";
    private static final int RELOAD_INTERVAL = 120 * 1000; // in ms.
    private static final int INITIAL_DELAY = 500; // in ms.

    private static SeedType smSeedType;
    private static ArrayList<Integer> smSeeds = new ArrayList<Integer>();

    // public static final String ACTION_LEAVE_GROUP =
    // "ch.epfl.unison.action.LEAVE_GROUP";

    private ListView mPlaylistsList;
    private Menu mMenu;

    private boolean mIsForeground = false;
    private Handler mHandler = new Handler();
    private Runnable mUpdater = new Runnable() {
        @Override
        public void run() {
            if (mIsForeground) {
                onRefresh();
                mHandler.postDelayed(this, RELOAD_INTERVAL);
            }
        }
    };

    /*
     * Coulde be refactorized
     */
    private BroadcastReceiver mLogoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This activity should finish on logout.
        registerReceiver(mLogoutReceiver, new IntentFilter(UnisonMenu.ACTION_LOGOUT));

        setContentView(R.layout.solo_playlists);

        ((Button) findViewById(R.id.createPlaylistBtn))
                .setOnClickListener(new OnCreatePlaylistListener());

        mPlaylistsList = (ListView) findViewById(R.id.soloPlaylistsList);
        mPlaylistsList.setOnItemClickListener(new OnPlaylistSelectedListener());

        // // Actions that should be taken when activity is started.
        // if (ACTION_LEAVE_GROUP.equals(getIntent().getAction())) {
        // // We are coming back from a group - let's make sure the back-end
        // // knows.
        // leaveGroup();
        // } else if (AppData.getInstance(this).showHelpDialog()) {
        // showHelpDialog();
        // }
    }

    /*
     * Could be refactorized (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();
        mIsForeground = true;
        startService(new Intent(LibraryService.ACTION_UPDATE));
        mHandler.postDelayed(mUpdater, INITIAL_DELAY);
    }

    /*
     * Could be refactorized (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockActivity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();
        mIsForeground = false;
    }

    /*
     * Could be refactorized (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockActivity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mLogoutReceiver);
    };

    /*
     * Could be refactorized (non-Javadoc)
     * @see
     * com.actionbarsherlock.app.SherlockActivity#onCreateOptionsMenu(android
     * .view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        return UnisonMenu.onCreateOptionsMenu(this, menu);
    }

    /*
     * Could be refactorized (non-Javadoc)
     * @see
     * com.actionbarsherlock.app.SherlockActivity#onOptionsItemSelected(android
     * .view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return UnisonMenu.onOptionsItemSelected(this, this, item);
    }

    @Override
    public void onRefresh() {
        repaintRefresh(true);

        UnisonAPI.Handler<JsonStruct.PlaylistsList> handler =
                new UnisonAPI.Handler<JsonStruct.PlaylistsList>() {

                    @Override
                    public void callback(PlaylistsList struct) {
                        try {
                            SoloPlaylistsActivity.this.mPlaylistsList
                                    .setAdapter(new PlaylistsAdapter(struct));
                            SoloPlaylistsActivity.this.repaintRefresh(false);
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
        AppData data = AppData.getInstance(this);
        data.getAPI().listPlaylists(data.getUid(), handler);
    }

    public void repaintRefresh(boolean isRefreshing) {
        if (mMenu == null) {
            Log.d(TAG, "repaintRefresh: mMenu is null");
            return;
        }

        MenuItem refreshItem = mMenu.findItem(R.id.menu_item_refresh);
        if (refreshItem != null) {
            if (isRefreshing) {
                LayoutInflater inflater = (LayoutInflater) getSupportActionBar()
                        .getThemedContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View refreshView = inflater.inflate(
                        R.layout.actionbar_indeterminate_progress, null);
                refreshItem.setActionView(refreshView);
            } else {
                refreshItem.setActionView(null);
            }
        } else {
            Log.d(TAG, "repaintRefresh: menu_item_refresh not found");
        }
    }

    // private void leaveGroup() {
    // // Make sure the user is not marked as present in any group.
    // AppData data = AppData.getInstance(this);
    // data.getAPI().leaveGroup(data.getUid(), new
    // UnisonAPI.Handler<JsonStruct.Success>() {
    //
    // @Override
    // public void callback(Success struct) {
    // Log.d(TAG, "successfully left group");
    // }
    //
    // @Override
    // public void onError(Error error) {
    // Log.d(TAG, error.toString());
    // }
    // });
    // }

    // private void showHelpDialog() {
    // AlertDialog.Builder alert = new AlertDialog.Builder(this);
    //
    // alert.setTitle(getString(R.string.groups_helpdialog_title));
    // alert.setMessage(getString(R.string.groups_helpdialog_message));
    //
    // final CheckBox cbox = new CheckBox(this);
    // cbox.setText(getString(R.string.groups_helpdialog_chkbox));
    // alert.setView(cbox);
    //
    // DialogInterface.OnClickListener click = new
    // DialogInterface.OnClickListener() {
    //
    // @Override
    // public void onClick(DialogInterface dialog, int which) {
    // if (cbox.isChecked()) {
    // // Don't show the dialog again in the future.
    // AppData.getInstance(SoloPlaylistsActivity.this).setShowHelpDialog(false);
    // }
    // if (DialogInterface.BUTTON_POSITIVE == which) {
    // startActivity(new Intent(SoloPlaylistsActivity.this,
    // HelpActivity.class));
    // }
    // }
    // };
    //
    // alert.setPositiveButton(getString(R.string.groups_helpdialog_yesBtn),
    // click);
    // alert.setNegativeButton(getString(R.string.groups_helpdialog_noBtn),
    // click);
    // alert.show();
    // }

    /** Adapter used to populate the ListView listing the groups. */
    private class PlaylistsAdapter extends ArrayAdapter<JsonStruct.Playlist> {

        public static final int ROW_LAYOUT = R.layout.solo_playlists_row;

        public PlaylistsAdapter(JsonStruct.PlaylistsList list) {
            super(SoloPlaylistsActivity.this, 0, list.playlists);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            JsonStruct.Playlist playlist = getItem(position);
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) SoloPlaylistsActivity.this
                        .getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(ROW_LAYOUT, parent, false);
            }
            ((TextView) view.findViewById(R.id.playlistName)).setText(playlist.name);
            String subtitle = null;
            if (playlist.listeners > 0) {
                String plural = "s";
                if (playlist.listeners == 1) {
                    plural = "";
                }
                subtitle = String.format("%d tracks - %d listener" + plural, playlist.size,
                        playlist.listeners);
            } else {
                subtitle = String.format("%d tracks", playlist.size);
            }
            ((TextView) view.findViewById(R.id.nbTracks)).setText(subtitle);

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
             * TODO: - show dialog listing types of seeds
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
         * Creates a playlist on the back-end. If it succeeds, the ListView
         * containing the list of playlists is updated. If it fails, a toast
         * notification is shown.
         */

        @SuppressLint("NewApi")
        protected void createPlaylist() {
            AppData data = AppData.getInstance(SoloPlaylistsActivity.this);

            // Create the JSONObject containing seeds
            for (int i = 0; i < smSeeds.size(); i++) {
                System.out.println("seeds: " + smSeeds.get(i) + "\n");
            }

            Resources res = getResources();
            TypedArray tags = res.obtainTypedArray(R.array.tags);
            String seedsInString = "";
            HashMap<String, String> seedsMap = new HashMap<String, String>();
            for (int i = 0; i < smSeeds.size(); i++) {
                if (!seedsInString.isEmpty()) {
                    seedsInString.concat(",");
                }
                seedsInString.concat(tags.getString(i));
            }
            seedsMap.put("tags", seedsInString);
            Gson gson = new Gson();
            String json = gson.toJson(seedsMap);

            data.getAPI().createPlaylist(data.getUid(), null,
                    new UnisonAPI.Handler<JsonStruct.PlaylistsList>() {
                        @Override
                        public void callback(PlaylistsList struct) {
                            SoloPlaylistsActivity.this.mPlaylistsList
                                    .setAdapter(new PlaylistsAdapter(struct));
                        }
                        @Override
                        public void onError(Error error) {
                            Log.d(TAG, error.toString());
                            if (SoloPlaylistsActivity.this != null) {
                                Toast.makeText(SoloPlaylistsActivity.this,
                                        R.string.error_creating_group,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }

        /**
         * @author marc
         */
        @SuppressLint({
                "ValidFragment", "NewApi"
        })
        private class PickSeedDialogFragment extends android.support.v4.app.DialogFragment {
            // private SeedType mType = null;

            // private ArrayList<Integer> mSeeds = new ArrayList<Integer>();

            // public PickSeedDialogFragment() {
            // // TODO Auto-generated constructor stub
            // }

//            public void show(FragmentManager supportFragmentManager, String string) {
//                // TODO Auto-generated method stub
//                this.show(getSupportFragmentManager(), "seedTypes");
//            }

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
                            switch (seedType) {
                                case TAGS:
                                    PickTagsDialogFragment pickTagsDialog =
                                            new PickTagsDialogFragment();
                                    pickTagsDialog.show(getSupportFragmentManager(), "tags");
                                    break;
                                case TRACKS:
                                    /*
                                     * TODO open a new dialog containing a music
                                     * file chooser.
                                     */
                                    break;
                                default:
                                    // Should never happen
                                    break;
                            }
                        }
                        // else {
                        // //TODO handle error, but should never happen.
                        // }
                    }
                });
                return builder.create();
            }

            // public void setSeeds(ArrayList<Integer> seeds) {
            // mSeeds = seeds;
            // }
            //
            // public ArrayList<Integer> getSeeds() {
            // return this.mSeeds;
            // }
        }

        /**
         * @author marc
         */
        @SuppressLint({
                "ValidFragment", "NewApi"
        })
        private class PickTagsDialogFragment extends android.support.v4.app.DialogFragment {
            private final ArrayList<Integer> mSelectedItems = new ArrayList<Integer>();

            public PickTagsDialogFragment() {
                // TODO Auto-generated constructor stub
            }

//            public void show(FragmentManager supportFragmentManager, String string) {
//                // TODO Auto-generated method stub
//                this.show(getSupportFragmentManager(), "tags");
//            }

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                // Where we track the selected items
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                // Set the dialog title
                builder.setTitle(R.string.solo_playlists_dialog_pick_seeds)
                        // Specify the list array, the items to be selected by
                        // default (null for none), and the listener through
                        // which
                        // to receive callbacks when items are selected
                        .setMultiChoiceItems(R.array.tags, null,
                                new DialogInterface.OnMultiChoiceClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which,
                                            boolean isChecked) {
                                        if (isChecked) {
                                            // If the user checked the item, add
                                            // it
                                            // to the selected items
                                            mSelectedItems.add(which);
                                        } else if (mSelectedItems.contains(which)) {
                                            // Else, if the item is already in
                                            // the
                                            // array, remove it
                                            mSelectedItems.remove(Integer.valueOf(which));
                                        }
                                    }
                                })
                        // Set the action buttons
                        .setPositiveButton(R.string.generic_ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        // User clicked OK, so save the
                                        // mSelectedItems
                                        // results somewhere or return them to
                                        // the component
                                        // that opened the dialog
                                        storeTracks();
                                    }
                                })
                        .setNegativeButton(R.string.generic_cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        // TODO
                                    }
                                });
                return builder.create();
            }

            private void storeTracks() {
                SoloPlaylistsActivity.smSeeds = mSelectedItems;
            }
        }

    }

    /**
     * When clicking on a group, send a request to the server and start
     * MainActivity.
     */
    private class OnPlaylistSelectedListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            UnisonAPI api = AppData.getInstance(SoloPlaylistsActivity.this).getAPI();
            long uid = AppData.getInstance(SoloPlaylistsActivity.this).getUid();
            final JsonStruct.Playlist playlist = (JsonStruct.Playlist) view.getTag();

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

}
