
package ch.epfl.unison.ui;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import ch.epfl.unison.AppData;
import ch.epfl.unison.Const;
import ch.epfl.unison.LibraryService;
import ch.epfl.unison.R;
import ch.epfl.unison.Uutils;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.JsonStruct.GroupsList;
import ch.epfl.unison.api.JsonStruct.PlaylistsList;
import ch.epfl.unison.api.JsonStruct.Success;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * Listing of the groups.
 * 
 * @author marc bourqui
 */
public class SoloPlaylistsActivity extends SherlockActivity implements UnisonMenu.OnRefreshListener {

    private static final String TAG = "ch.epfl.unison.SoloPlaylistsActivity";
    private static final int RELOAD_INTERVAL = 120 * 1000; // in ms.
    private static final int INITIAL_DELAY = 500; // in ms.

    //public static final String ACTION_LEAVE_GROUP = "ch.epfl.unison.action.LEAVE_GROUP";

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

        ((Button) findViewById(R.id.createGroupBtn))
                .setOnClickListener(new OnCreatePlaylistListener());

        mPlaylistsList = (ListView) findViewById(R.id.groupsList);
        mPlaylistsList.setOnItemClickListener(new OnPlaylistSelectedListener());

//        // Actions that should be taken when activity is started.
//        if (ACTION_LEAVE_GROUP.equals(getIntent().getAction())) {
//            // We are coming back from a group - let's make sure the back-end
//            // knows.
//            leaveGroup();
//        } else if (AppData.getInstance(this).showHelpDialog()) {
//            showHelpDialog();
//        }
    }

    /*
     * Could be refactorized
     * 
     * (non-Javadoc)
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
     * Could be refactorized
     * 
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockActivity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();
        mIsForeground = false;
    }
    
    /*
     * Could be refactorized
     * 
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockActivity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mLogoutReceiver);
    };

    /*
     * Could be refactorized
     * 
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockActivity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        return UnisonMenu.onCreateOptionsMenu(this, menu);
    }

    /*
     * Could be refactorized
     * 
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockActivity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return UnisonMenu.onOptionsItemSelected(this, this, item);
    }

    @Override
    public void onRefresh() {
        repaintRefresh(true);

        UnisonAPI.Handler<JsonStruct.GroupsList> handler = new UnisonAPI.Handler<JsonStruct.GroupsList>() {

            @Override
            public void callback(GroupsList struct) {
                try {
                    SoloPlaylistsActivity.this.mPlaylistsList
                            .setAdapter(new PlaylistsAdapter(struct));
                    SoloPlaylistsActivity.this.repaintRefresh(false);
                } catch (NullPointerException e) {
                    Log.w(TAG, "group or activity is null?", e);
                }

            }

            @Override
            public void onError(UnisonAPI.Error error) {
                if (error != null) {
                    Log.d(TAG, error.toString());
                }
                if (SoloPlaylistsActivity.this != null) {
                    Toast.makeText(SoloPlaylistsActivity.this, R.string.error_loading_playlists,
                            Toast.LENGTH_LONG).show();
                    SoloPlaylistsActivity.this.repaintRefresh(false);
                }
            }
        };
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

//    private void leaveGroup() {
//        // Make sure the user is not marked as present in any group.
//        AppData data = AppData.getInstance(this);
//        data.getAPI().leaveGroup(data.getUid(), new UnisonAPI.Handler<JsonStruct.Success>() {
//
//            @Override
//            public void callback(Success struct) {
//                Log.d(TAG, "successfully left group");
//            }
//
//            @Override
//            public void onError(Error error) {
//                Log.d(TAG, error.toString());
//            }
//        });
//    }

//    private void showHelpDialog() {
//        AlertDialog.Builder alert = new AlertDialog.Builder(this);
//
//        alert.setTitle(getString(R.string.groups_helpdialog_title));
//        alert.setMessage(getString(R.string.groups_helpdialog_message));
//
//        final CheckBox cbox = new CheckBox(this);
//        cbox.setText(getString(R.string.groups_helpdialog_chkbox));
//        alert.setView(cbox);
//
//        DialogInterface.OnClickListener click = new DialogInterface.OnClickListener() {
//
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                if (cbox.isChecked()) {
//                    // Don't show the dialog again in the future.
//                    AppData.getInstance(SoloPlaylistsActivity.this).setShowHelpDialog(false);
//                }
//                if (DialogInterface.BUTTON_POSITIVE == which) {
//                    startActivity(new Intent(SoloPlaylistsActivity.this, HelpActivity.class));
//                }
//            }
//        };
//
//        alert.setPositiveButton(getString(R.string.groups_helpdialog_yesBtn), click);
//        alert.setNegativeButton(getString(R.string.groups_helpdialog_noBtn), click);
//        alert.show();
//    }

    /** Adapter used to populate the ListView listing the groups. */
    private class PlaylistsAdapter extends ArrayAdapter<JsonStruct.Group> {

        public static final int ROW_LAYOUT = R.layout.groups_row;

        public PlaylistsAdapter(JsonStruct.PlaylistsList list) {
            super(SoloPlaylistsActivity.this, 0, list.playlists);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            JsonStruct.Group group = getItem(position);
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) SoloPlaylistsActivity.this
                        .getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(ROW_LAYOUT, parent, false);
            }
            ((TextView) view.findViewById(R.id.groupName)).setText(group.name);
            String subtitle = null;
            if (group.distance != null) {
                subtitle = String.format("%s away - %d people.",
                        Uutils.distToString(group.distance), group.nbUsers);
            } else {
                String format = "%d person.";
                if (group.nbUsers > 1) {
                    format = "%d people.";
                }
                subtitle = String.format(format, group.nbUsers);
            }
            ((TextView) view.findViewById(R.id.nbParticipants)).setText(subtitle);

            view.setTag(group);
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
            AlertDialog.Builder alert = new AlertDialog.Builder(SoloPlaylistsActivity.this);

            alert.setTitle(getString(R.string.solo_playlists_alert_newplaylist_title));
            alert.setMessage(getString(R.string.solo_playlists_alert_newplaylist_message));

            // Set an EditText view to get user input
            final EditText input = new EditText(SoloPlaylistsActivity.this);
            alert.setView(input);

            // When clicking on "OK", create the group.
            alert.setPositiveButton(getString(R.string.solo_playlists_alert_newplaylist_ok),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String name = input.getText().toString().trim();
                            OnCreatePlaylistListener.this.createGroup(name);
                        }
                    });

            alert.setNegativeButton(getString(R.string.solo_playlists_alert_newplaylist_cancel), null);
            alert.show();
        }

        /**
         * Creates a group on the back-end. If it succeeds, the ListView
         * containing the list of groups is updated. If it fails, a toast
         * notification is shown.
         * 
         * @param name the name of the group to be created
         */
        private void createGroup(String name) {
            if (name == null || name.equals("")) {
                Toast.makeText(SoloPlaylistsActivity.this,
                        R.string.error_creating_group_empty_name, Toast.LENGTH_LONG).show();
            } else {
                AppData data = AppData.getInstance(SoloPlaylistsActivity.this);

                data.getAPI().createPlaylist(name, new UnisonAPI.Handler<JsonStruct.PlaylistsList>() {
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
            final JsonStruct.Group group = (JsonStruct.Group) view.getTag();

            api.joinGroup(uid, group.gid, new UnisonAPI.Handler<JsonStruct.Success>() {

                @Override
                public void callback(Success struct) {
                    SoloPlaylistsActivity.this.startActivity(
                            new Intent(SoloPlaylistsActivity.this, MainActivity.class)
                                    .putExtra(Const.Strings.GID, group.gid)
                                    .putExtra(Const.Strings.NAME, group.name));
                }

                @Override
                public void onError(Error error) {
                    Log.d(TAG, error.toString());
                    if (SoloPlaylistsActivity.this != null) {
                        Toast.makeText(SoloPlaylistsActivity.this, R.string.error_joining_group,
                                Toast.LENGTH_LONG).show();
                    }
                }

            });
        }
    }
}
