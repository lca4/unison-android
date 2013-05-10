
package ch.epfl.unison.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
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
import ch.epfl.unison.api.JsonStruct.GroupSuggestion;
import ch.epfl.unison.api.JsonStruct.GroupsList;
import ch.epfl.unison.api.JsonStruct.Success;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * Listing of the groups.
 * 
 * @author lum
 */
public class GroupsActivity extends SherlockActivity implements UnisonMenu.OnRefreshListener {

    private static final String TAG = "ch.epfl.unison.GroupsActivity";
    private static final int RELOAD_INTERVAL = 120 * 1000; // in ms.
    private static final int INITIAL_DELAY = 500; // in ms.

    // EPFL Polydome.
    private static final double DEFAULT_LATITUDE = 46.52147800207456;
    private static final double DEFAULT_LONGITUDE = 6.568992733955383;

    public static final String ACTION_LEAVE_GROUP = "ch.epfl.unison.action.LEAVE_GROUP";

    private ListView mGroupsList;
    private Menu mMenu;

    private JsonStruct.GroupSuggestion mSuggestion;

    private boolean mDismissedHelp = false;
    private boolean mSuggestionIsForeground = false;

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

    private BroadcastReceiver mLogoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "GroupsActivity is being created.");

        // This activity should finish on logout.
        registerReceiver(mLogoutReceiver, new IntentFilter(UnisonMenu.ACTION_LOGOUT));

        setContentView(R.layout.groups);

        ((Button) findViewById(R.id.createGroupBtn))
                .setOnClickListener(new OnCreateGroupListener());

        mGroupsList = (ListView) findViewById(R.id.groupsList);
        mGroupsList.setOnItemClickListener(new OnGroupSelectedListener());

        // Actions that should be taken whe activity is started.
        if (ACTION_LEAVE_GROUP.equals(getIntent().getAction())) {
            // We are coming back from a group - let's make sure the back-end
            // knows.
            leaveGroup();
            mDismissedHelp = true;
        } else if (AppData.getInstance(this).showHelpDialog()) {
            showHelpDialog();
        } else {
            mDismissedHelp = true;
        }

        if (AppData.getInstance(this).showGroupSuggestion() && mDismissedHelp) {
            showGroupSuggestion();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        // mDismissedHelp = true;
        mIsForeground = true;
        startService(new Intent(LibraryService.ACTION_UPDATE));
        mHandler.postDelayed(mUpdater, INITIAL_DELAY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsForeground = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mLogoutReceiver);
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        return UnisonMenu.onCreateOptionsMenu(this, menu);
    }

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
                    GroupsActivity.this.mGroupsList
                            .setAdapter(new GroupsAdapter(struct));
                    GroupsActivity.this.repaintRefresh(false);
                } catch (NullPointerException e) {
                    Log.w(TAG, "group or activity is null?", e);
                }
            }

            @Override
            public void onError(UnisonAPI.Error error) {
                if (error != null) {
                    Log.d(TAG, error.toString());
                }
                if (GroupsActivity.this != null) {
                    Toast.makeText(GroupsActivity.this, R.string.error_loading_groups,
                            Toast.LENGTH_LONG).show();
                    GroupsActivity.this.repaintRefresh(false);
                }
            }
        };
        AppData data = AppData.getInstance(this);
        Location currentLoc = data.getLocation();
        if (currentLoc != null) {
            double lat = currentLoc.getLatitude();
            double lon = currentLoc.getLongitude();
            data.getAPI().listGroups(lat, lon, handler);
        } else {
            data.getAPI().listGroups(handler);
        }
        if (mDismissedHelp && data.showGroupSuggestion()) {
            showGroupSuggestion();
        }
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

    private void leaveGroup() {
        // Make sure the user is not marked as present in any group.
        AppData data = AppData.getInstance(this);
        data.getAPI().leaveGroup(data.getUid(), new UnisonAPI.Handler<JsonStruct.Success>() {

            @Override
            public void callback(Success struct) {
                Log.d(TAG, "successfully left group");
            }

            @Override
            public void onError(Error error) {
                Log.d(TAG, error.toString());
            }
        });
    }

    private void showHelpDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(getString(R.string.groups_helpdialog_title));
        alert.setMessage(getString(R.string.groups_helpdialog_message));

        final CheckBox cbox = new CheckBox(this);
        cbox.setText(getString(R.string.groups_helpdialog_chkbox));
        alert.setView(cbox);

        DialogInterface.OnClickListener click = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                mDismissedHelp = true;
                if (cbox.isChecked()) {
                    // Don't show the dialog again in the future.
                    AppData.getInstance(GroupsActivity.this).setShowHelpDialog(false);
                }
                if (DialogInterface.BUTTON_POSITIVE == which) {
                    startActivity(new Intent(GroupsActivity.this, HelpActivity.class));
                }
            }
        };

        alert.setPositiveButton(getString(R.string.groups_helpdialog_yesBtn), click);
        alert.setNegativeButton(getString(R.string.groups_helpdialog_noBtn), click);
        alert.show();
    }

    private DialogInterface.OnClickListener mSuggestionClick;

    private UnisonAPI.Handler<JsonStruct.Success> mAcceptSuggestionHandler =
            new UnisonAPI.Handler<JsonStruct.Success>() {

                @Override
                public void callback(Success struct) {
                    GroupsActivity.this.startActivity(
                            new Intent(GroupsActivity.this, GroupsMainActivity.class)
                                    .putExtra(Const.Strings.GROUP, mSuggestion.group));
                }

                @Override
                public void onError(Error error) {
                    Log.d(TAG, error.toString());
                    if (GroupsActivity.this != null) {
                        Toast.makeText(GroupsActivity.this,
                                R.string.error_joining_group,
                                Toast.LENGTH_LONG).show();
                    }
                }

            };

    private AlertDialog.Builder prepareSuggestionBuilder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(GroupsActivity.this);
        builder.setTitle(getString(R.string.groups_suggestion_title));
        LayoutInflater layoutInflater = (LayoutInflater)
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = layoutInflater.inflate(R.layout.suggestion_dialog, null);
        builder.setView(dialogView);
        ListView userView = (ListView) dialogView.findViewById(R.id.suggestionUserList);
        final CheckBox cbox = (CheckBox) dialogView.findViewById(R.id.suggestionCheckbox);
        ArrayAdapter<String> userAdapter = new ArrayAdapter<String>(GroupsActivity.this,
                R.layout.group_suggestion_user_row,
                R.id.group_suggestion_username, mSuggestion.users);
        userView.setAdapter(userAdapter);
        userView.setSelector(android.R.color.transparent);
        // this is a bit too much, the user cannot scroll the list anymore
        // userView.setEnabled(false); //this is a bit too much, the user cannot
        // scroll the list anymore

        mSuggestionClick =
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSuggestionIsForeground = false;

                        if (cbox.isChecked()) {
                            AppData.getInstance(GroupsActivity.this).setShowGroupSuggestion(false);
                        }
                        if (DialogInterface.BUTTON_POSITIVE == which) {
                            UnisonAPI api = AppData.getInstance(GroupsActivity.this).getAPI();
                            long uid = AppData.getInstance(GroupsActivity.this).getUid();
                            api.joinGroup(uid, mSuggestion.group.gid, mAcceptSuggestionHandler);
                        }
                    }
                };

        return builder;
    }

    private UnisonAPI.Handler<JsonStruct.GroupSuggestion> mSuggestionHandler =
            new UnisonAPI.Handler<JsonStruct.GroupSuggestion>() {

                @Override
                public void callback(GroupSuggestion struct) {
                    mSuggestion = struct;
                    if (mSuggestion == null || GroupsActivity.this == null
                            || !mSuggestion.suggestion
                            || mSuggestion.users == null
                            || mSuggestion.cluster == null
                            || mSuggestion.group == null) {
                        return;
                    }
                    AlertDialog.Builder builder = prepareSuggestionBuilder();

                    // This is supposed to handle the situation where the user
                    // presses the BACK key too.
                    builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            mSuggestionIsForeground = false;

                            // We could handle here whether the checkbox was
                            // checked or not,
                            // but it makes more sense to do so only when the
                            // user presses a button.
                        }
                    });

                    builder.setPositiveButton(getString(R.string.groups_suggestion_yesBtn),
                            mSuggestionClick);
                    builder.setNegativeButton(getString(R.string.groups_suggestion_noBtn),
                            mSuggestionClick);

                    mSuggestionIsForeground = true;
                    final Dialog dialog = builder.create();
                    dialog.show();
                }

                @Override
                public void onError(Error error) {
                    mSuggestion = null;
                    // Do nothing, errors silently happen in the background.
                }
            };

    /**
     * Pass information as arguments for now for easy testing. They could be
     * written as class variables.
     */
    private void showGroupSuggestion() {
        if (mSuggestionIsForeground) {
            return;
        }

        AppData data = AppData.getInstance(GroupsActivity.this);
        UnisonAPI api = data.getAPI();

        Location currentLoc = data.getLocation();

        // Only do suggestions based on location for now.
        if (currentLoc != null) {
            double lat = currentLoc.getLatitude();
            double lon = currentLoc.getLongitude();
            api.getSuggestion(lat, lon, mSuggestionHandler);
        }
    }

    /** Adapter used to populate the ListView listing the groups. */
    private class GroupsAdapter extends ArrayAdapter<JsonStruct.Group> {

        public static final int ROW_LAYOUT = R.layout.groups_row;

        public GroupsAdapter(JsonStruct.GroupsList list) {
            super(GroupsActivity.this, 0, list.groups);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            JsonStruct.Group group = getItem(position);
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) GroupsActivity.this.getSystemService(
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
    private class OnCreateGroupListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            AlertDialog.Builder alert = new AlertDialog.Builder(GroupsActivity.this);

            alert.setTitle(getString(R.string.groups_alert_newgroup_title));
            alert.setMessage(getString(R.string.groups_alert_newgroup_message));

            // Set an EditText view to get user input
            final EditText input = new EditText(GroupsActivity.this);
            alert.setView(input);

            // When clicking on "OK", create the group.
            alert.setPositiveButton(getString(R.string.groups_alert_newgroup_ok),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String name = input.getText().toString().trim();
                            OnCreateGroupListener.this.createGroup(name);
                        }
                    });

            alert.setNegativeButton(getString(R.string.groups_alert_newgroup_cancel), null);
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
                Toast.makeText(GroupsActivity.this,
                        R.string.error_creating_group_empty_name, Toast.LENGTH_LONG).show();
            } else {
                AppData data = AppData.getInstance(GroupsActivity.this);
                double lat, lon;
                Location currentLoc = data.getLocation();
                if (currentLoc != null) {
                    lat = currentLoc.getLatitude();
                    lon = currentLoc.getLongitude();
                } else {
                    lat = DEFAULT_LATITUDE;
                    lon = DEFAULT_LONGITUDE;
                    Log.i(TAG, "location was null, using default values");
                }

                data.getAPI().createGroup(name, lat, lon,
                        new UnisonAPI.Handler<JsonStruct.GroupsList>() {
                            @Override
                            public void callback(GroupsList struct) {
                                GroupsActivity.this.mGroupsList
                                        .setAdapter(new GroupsAdapter(struct));
                            }

                            @Override
                            public void onError(Error error) {
                                Log.d(TAG, error.toString());
                                if (GroupsActivity.this != null) {
                                    Toast.makeText(GroupsActivity.this,
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
    private class OnGroupSelectedListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            UnisonAPI api = AppData.getInstance(GroupsActivity.this).getAPI();
            long uid = AppData.getInstance(GroupsActivity.this).getUid();
            final JsonStruct.Group group = (JsonStruct.Group) view.getTag();

            // TODO modularize
            api.joinGroup(uid, group.gid, new UnisonAPI.Handler<JsonStruct.Success>() {

                @Override
                public void callback(Success struct) {

                    GroupsActivity.this.startActivity(
                            new Intent(GroupsActivity.this, GroupsMainActivity.class)
                                    .putExtra(Const.Strings.GROUP, group));
                }

                @Override
                public void onError(Error error) {
                    Log.d(TAG, error.toString());
                    if (GroupsActivity.this != null) {
                        Toast.makeText(GroupsActivity.this, R.string.error_joining_group,
                                Toast.LENGTH_LONG).show();
                    }
                }

            });
        }
    }
}
