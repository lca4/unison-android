
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
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
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
import ch.epfl.unison.api.JsonStruct.Group;
import ch.epfl.unison.api.JsonStruct.GroupSuggestion;
import ch.epfl.unison.api.JsonStruct.GroupsList;
import ch.epfl.unison.api.JsonStruct.Success;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Listing of the groups.
 * 
 * @author lum
 */
public class GroupsActivity extends SherlockActivity implements AbstractMenu.OnRefreshListener {

    private static final String TAG = "ch.epfl.unison.GroupsActivity";
    private static final int RELOAD_INTERVAL = 120 * 1000; // in ms.
    private static final int INITIAL_DELAY = 500; // in ms.

    // EPFL Polydome.
    private static final double DEFAULT_LATITUDE = 46.52147800207456;
    private static final double DEFAULT_LONGITUDE = 6.568992733955383;

    private static final int MAXIMUM_GROUP_NAME_LENGTH = 30;

    private String mAction = null;

    public static final String ACTION_FROM_SOLO = "ch.epfl.unison.action.FROM_SOLO";
    public static final String ACTION_LEAVE_GROUP = "ch.epfl.unison.action.LEAVE_GROUP";
    public static final String ACTION_CREATE_AND_JOIN_GROUP =
            "ch.epfl.unison.action.CREATE_AND_JOIN_GROUP";
    public static final String ACTION_LEAVE_JOIN_GROUP =
            "ch.epfl.unison.action.LEAVE_JOIN_GROUP";
    public static final String ACTION_JOIN_GROUP = "ch.epfl.unison.action.JOIN_GROUP";

    private List<String> mSupportedActions = null;

    private ListView mGroupsList;
    private Menu mMenu;

    private JsonStruct.GroupSuggestion mSuggestion;

    private boolean mDismissedHelp = false;
    private boolean mSuggestionIsForeground = false;
    private boolean mProcessingAutoAction = false;
    private DialogInterface.OnClickListener mSuggestionClick;

    private JsonStruct.Group mGroupClicked = null;
    // private NfcAdapter mNfcAdapter = null;
    // private PendingIntent mNfcIntent = null;

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

    // ===== ERROR POPUP =====
    private AlertDialog mGroupNoLongerExistsDialog;

    // ====================== METHODS ===================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "GroupsActivity is being created.");

        // This activity should finish on logout.
        registerReceiver(mLogoutReceiver, new IntentFilter(AbstractMenu.ACTION_LOGOUT));

        setContentView(R.layout.groups);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ((Button) findViewById(R.id.createGroupBtn))
                .setOnClickListener(new OnCreateGroupListener());

        setupSuggestionButton();

        mGroupsList = (ListView) findViewById(R.id.groupsList);
        mGroupsList.setOnItemClickListener(new OnGroupSelectedListener());

        AppData data = AppData.getInstance(GroupsActivity.this);

        // We might still be in a group according to the server, but we know we
        // are not in groupMainActivity
        data.setInGroup(false);

        handleExtras();
        // Actions that should be taken whe activity is started.
        setSupportedActions();
        mAction = getIntent().getAction();
        if (mAction != null && mSupportedActions.contains(mAction)
                && !ACTION_FROM_SOLO.equals(mAction)) {
            toggleActivityButtons(false);
            mProcessingAutoAction = true;
            if (ACTION_LEAVE_GROUP.equals(mAction)
                    || ACTION_LEAVE_JOIN_GROUP.equals(mAction)) {
                // We are coming back from a group - let's make sure the
                // back-end
                // knows.

                // Here mGroupClicked is null when the action is
                // ACTION_LEAVE_GROUP
                // and is not null when the action is
                // ACTION_FROM_HISTORY_LEAVE_GROUP
                leaveGroup(mGroupClicked);
                mDismissedHelp = true;
            } else if (ACTION_JOIN_GROUP.equals(mAction)) {
                // Automatic actions are going to be performed, we disable
                // unwanted popups:

                mDismissedHelp = true;

                // TODO: this code is duplicated in leaveGroup and should be
                // factorized
                if (mGroupClicked != null) {
                    // This means that we have to
                    // Log.d(TAG,
                    // "trying to rejoin a group: we have extras in the intent");
                    if (mGroupClicked.password) {
                        promptForPassword(mGroupClicked);
                    } else {
                        joinGroup(mGroupClicked, null);
                    }
                    // Log.d(TAG,
                    // "trying to rejoin a group: the group was not null: ");
                } else {
                    Log.d(TAG, "Tried to recreate a group but could not extract infos from intent.");
                    Toast.makeText(GroupsActivity.this, R.string.error_group_to_recreate,
                            Toast.LENGTH_LONG).show();
                    mProcessingAutoAction = false;
                    toggleActivityButtons(true);
                }

            } else {
                toggleActivityButtons(true);
                mProcessingAutoAction = false;
            }
        } else if (data.showHelpDialog() && !mProcessingAutoAction) {
            showHelpDialog();
        } else {
            mDismissedHelp = true;
        }

        if (data.showGroupSuggestion() && mDismissedHelp && !mProcessingAutoAction) {
            fetchGroupSuggestion();
        }

        // setupNFC();
    }

    // ===== OnCreate helpers =====
    private void handleExtras() {
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(Const.Strings.GROUP)) {
            // Log.d(TAG,
            // "trying to rejoin a group: we have extras in the intent");
            Group group = (Group) extras.get(Const.Strings.GROUP);

            if (group != null) {
                mGroupClicked = group;
            } else {
                // CAUTION!
                mGroupClicked = null;
            }

            // now we delete the group from the extra
            extras.remove(Const.Strings.GROUP);
        }
    }

    private void setSupportedActions() {
        mSupportedActions = new ArrayList<String>();
        mSupportedActions.add(ACTION_FROM_SOLO);
        mSupportedActions.add(ACTION_CREATE_AND_JOIN_GROUP);
        mSupportedActions.add(ACTION_JOIN_GROUP);
        mSupportedActions.add(ACTION_LEAVE_JOIN_GROUP);
        mSupportedActions.add(ACTION_LEAVE_GROUP);
    }

    // ===== End of OnCreate helpers =====

    /*
     * @Override protected void onActivityResult(int requestCode, int
     * resultCode, Intent data) { super.onActivityResult(requestCode,
     * resultCode, data); //For now, we only use the result to be notified when
     * the user comes back from the settings. //This should be useless since
     * onResume is called Log.d(TAG,
     * "Called onActivityResult with result code = " + resultCode);
     * updateSuggestionButton(true); }
     */

    // @Override
    // protected void onNewIntent(Intent intent) {
    // super.onNewIntent(intent);
    //
    // if (mNfcAdapter == null) {
    // return;
    // }
    //
    // // NdefMessage[] messages;
    //
    // if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
    // NdefMessage[] messages = (NdefMessage[])
    // intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
    // if (messages == null) {
    // //FIXME
    // Toast.makeText(GroupsActivity.this, "fixme", Toast.LENGTH_LONG).show();
    //
    // } else {
    // StringBuilder sb = new StringBuilder();
    //
    // for (NdefMessage msg: messages) {
    // NdefRecord[] records = msg.getRecords();
    // if (records != null) {
    // for (NdefRecord rec : records) {
    // if (rec != null) {
    // byte[] pl = rec.getPayload();
    // if (pl != null) {
    // sb.append(new String(pl));
    // }
    // }
    // }
    // }
    // }
    //
    // String result = sb.toString();
    //
    // //FIXME
    // Toast.makeText(GroupsActivity.this, "Read " + result,
    // Toast.LENGTH_LONG).show();
    // }
    // }
    // }

    @Override
    protected void onResume() {
        super.onResume();
        // mDismissedHelp = true;
        mIsForeground = true;
        startService(new Intent(LibraryService.ACTION_UPDATE));
        mHandler.postDelayed(mUpdater, INITIAL_DELAY);

        // if (mNfcAdapter != null && mNfcIntent != null) {
        // mNfcAdapter.enableForegroundDispatch(GroupsActivity.this,
        // mNfcIntent, null, null);
        // }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsForeground = false;

        // if (mNfcAdapter != null) {
        // mNfcAdapter.disableForegroundDispatch(GroupsActivity.this);
        // }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mLogoutReceiver);
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        boolean res = AbstractMenu.onCreateOptionsMenu(this, menu);
        mMenu.findItem(R.id.menu_item_groups).setVisible(false);
        mMenu.findItem(R.id.menu_item_history).setVisible(true);
        return res;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return AbstractMenu.onOptionsItemSelected(this, this, item);
    }

    @Override
    public void onRefresh() {

        // toggleActivityButtons(!mProcessingAutoAction);

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
        // switchSuggestionButtonState(data.showGroupSuggestion());

        // updateSuggestionButton(!mProcessingAutoAction);
        toggleActivityButtons(!mProcessingAutoAction);
        if (mDismissedHelp && data.showGroupSuggestion()) {
            fetchGroupSuggestion();
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

    private void leaveGroup(final Group group) {
        // Make sure the user is not marked as present in any group.
        final AppData data = AppData.getInstance(this);
        data.getAPI().leaveGroup(data.getUid(), data.getCurrentGID(),
                new UnisonAPI.Handler<JsonStruct.Success>() {

                    @Override
                    public void callback(Success struct) {
                        data.setCurrentGID(Long.valueOf(-1));
                        Log.d(TAG, "successfully left group");
                        if (group != null) {
                            if (group.password) {
                                promptForPassword(group);
                            } else {
                                joinGroup(group, null);
                            }
                        } else {
                            toggleActivityButtons(true);
                            mProcessingAutoAction = false;
                        }
                    }

                    @Override
                    public void onError(Error error) {
                        if (error != null) {
                            Log.d(TAG, error.toString());
                        } else {
                            Log.d(TAG, "error on leaveGroup() and the error was null!");
                        }
                        if (group != null) {
                            if (group.password) {
                                promptForPassword(group);
                            } else {
                                joinGroup(group, null);
                            }
                        }
                        toggleActivityButtons(true);
                        mProcessingAutoAction = false;
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

    /**
     * @param mask : added so that we can either force the button to be
     *            disabled, or enable it under the usual conditions
     */
    private void updateSuggestionButton(boolean mask) {

        Button reDisplaySuggestion = (Button) findViewById(R.id.displaySuggestion);
        AppData data = AppData.getInstance(GroupsActivity.this);
        Location currentLoc = data.getLocation();

        if (!data.showGroupSuggestion()) {
            reDisplaySuggestion.setText(R.string.groups_suggestion_no_suggest);
            reDisplaySuggestion.setEnabled(false);
            return;
        }

        if (currentLoc == null) {
            reDisplaySuggestion.setText(R.string.groups_suggestion_goto_settings);
            reDisplaySuggestion.setEnabled(mask); // was previously true
            return;
        }

        if (!mask) {
            switchSuggestionButtonState(false);
            return;
        }

        if (mSuggestion == null) {
            switchSuggestionButtonState(false);
        } else {
            switchSuggestionButtonState(true);
        }

    }

    private void switchSuggestionButtonState(boolean enabled) {
        Button reDisplaySuggestion = (Button) findViewById(R.id.displaySuggestion);
        reDisplaySuggestion.setEnabled(enabled);
        if (enabled) {
            reDisplaySuggestion.setText(R.string.groups_display_suggestion_enabled);
        } else {
            reDisplaySuggestion.setText(R.string.groups_display_suggestion_disabled);
        }
    }

    private AlertDialog.Builder prepareSuggestionBuilder() {
        // checks:
        if (mSuggestion == null || mSuggestion.users == null || mSuggestion.group == null) {
            return null;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(GroupsActivity.this);
        builder.setTitle(getString(R.string.groups_suggestion_title));
        LayoutInflater layoutInflater = (LayoutInflater)
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = layoutInflater.inflate(R.layout.suggestion_dialog, null);
        builder.setView(dialogView);
        ListView userView = (ListView) dialogView.findViewById(R.id.suggestionUserList);
        final CheckBox cbox = (CheckBox) dialogView.findViewById(R.id.suggestionCheckbox);
        ArrayAdapter<String> userAdapter = new ArrayAdapter<String>(GroupsActivity.this,
                R.layout.listrow_group_suggestion_user,
                R.id.group_suggestion_username, mSuggestion.users);
        userView.setAdapter(userAdapter);
        userView.setSelector(android.R.color.transparent);
        // this is a bit too much, the user cannot scroll the list anymore
        // userView.setEnabled(false);

        mSuggestionClick =
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSuggestionIsForeground = false;
                        mDismissedHelp = true;

                        if (cbox.isChecked()) {
                            AppData.getInstance(GroupsActivity.this).setShowGroupSuggestion(false);
                        }
                        if (DialogInterface.BUTTON_POSITIVE == which) {
                            // UnisonAPI api =
                            // AppData.getInstance(GroupsActivity.this).getAPI();
                            // long uid =
                            // AppData.getInstance(GroupsActivity.this).getUid();
                            // api.joinGroup(uid, mSuggestion.group.gid,
                            // mAcceptSuggestionHandler);
                            joinGroup(mSuggestion.group, null);
                        } else if (DialogInterface.BUTTON_NEGATIVE == which) {
                            updateSuggestionButton(true);
                        }
                    }
                };

        return builder;
    }

    private void showSuggestionDialog() {
        AlertDialog.Builder builder = prepareSuggestionBuilder();

        // Check:
        if (builder == null) {
            // something wrong happened to the suggestion
            Toast.makeText(GroupsActivity.this, R.string.error_creating_suggestion_popup,
                    Toast.LENGTH_LONG).show();
            Log.d(TAG, "we got an error while trying to show a suggestion popup!");
            return;
        }

        // This is supposed to handle the situation where the user presses the
        // BACK key too.
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // mSuggestion = null;
                mSuggestionIsForeground = false;

                // switchSuggestionButtonState(false);
                // We could handle here whether the checkbox was checked or not,
                // but it makes more sense to do so only when the user presses a
                // button.
            }
        });

        builder.setPositiveButton(getString(R.string.groups_suggestion_yesBtn),
                mSuggestionClick);
        builder.setNegativeButton(getString(R.string.groups_suggestion_noBtn),
                mSuggestionClick);

        updateSuggestionButton(true);

        // mSuggestionIsForeground = true;
        final Dialog dialog = builder.create();
        dialog.show();
    }

    private boolean validSuggestion(JsonStruct.GroupSuggestion sugg) {
        return !(sugg == null || GroupsActivity.this == null
                || !sugg.suggestion
                || sugg.users == null
                || sugg.cluster == null
                || sugg.group == null);
    }

    private UnisonAPI.Handler<JsonStruct.GroupSuggestion> mSuggestionHandler =
            new UnisonAPI.Handler<JsonStruct.GroupSuggestion>() {

                @Override
                public void callback(GroupSuggestion struct) {
                    // If we get the same suggestion twice we don't want to show
                    // the pop up again.
                    if (validSuggestion(struct) && validSuggestion(mSuggestion)) {
                        if (mSuggestion.group.gid.equals(struct.group.gid)) {
                            mSuggestionIsForeground = false;
                            mSuggestion = struct;

                            String nick = AppData.getInstance(GroupsActivity.this).getNickname();
                            if (nick != null) {
                                Log.d(TAG, "removing " + nick + " from suggestion");
                                ArrayList<String> users = new ArrayList<String>(
                                        Arrays.asList(mSuggestion.users));
                                users.remove(nick);
                                mSuggestion.users = Arrays.copyOf(users.toArray(),
                                        users.toArray().length,
                                        String[].class);
                            }

                            return;
                        }
                    }
                    // Sanity check on the Suggestion we just received.

                    mSuggestion = struct;
                    if (!validSuggestion(struct)) {
                        mSuggestion = null;
                        mSuggestionIsForeground = false;

                        return;
                    }

                    String nick = AppData.getInstance(GroupsActivity.this).getNickname();
                    if (nick != null) {
                        Log.d(TAG, "removing " + nick + " from suggestion");
                        ArrayList<String> users = new ArrayList<String>(
                                Arrays.asList(mSuggestion.users));
                        users.remove(nick);
                        mSuggestion.users = Arrays.copyOf(users.toArray(), users.toArray().length,
                                String[].class);
                    }

                    updateSuggestionButton(!mProcessingAutoAction);

                    if (!mProcessingAutoAction) {
                        showSuggestionDialog();
                    }
                }

                @Override
                public void onError(Error error) {
                    mSuggestionIsForeground = false;

                    updateSuggestionButton(!mProcessingAutoAction);
                    // Do nothing, errors silently happen in the background.
                }
            };

    /**
     * Pass information as arguments for now for easy testing. They could be
     * written as class variables.
     */
    private void fetchGroupSuggestion() {
        if (mSuggestionIsForeground) {
            return;
        }
        // Set it to true as soon as possible to avoid pilling up of pop-ups.
        mSuggestionIsForeground = true;

        AppData data = AppData.getInstance(GroupsActivity.this);
        UnisonAPI api = data.getAPI();

        Location currentLoc = data.getLocation();

        // TODO check if needed - EDIT: should be ok.
        // updateSuggestionButton(!mProcessingAutoAction);

        // Only do suggestions based on location for now.
        if (currentLoc != null) {
            double lat = currentLoc.getLatitude();
            double lon = currentLoc.getLongitude();
            api.getSuggestion(lat, lon, mSuggestionHandler);
        } else {
            mSuggestionIsForeground = false;
        }
    }

    /** Adapter used to populate the ListView listing the groups. */
    private class GroupsAdapter extends ArrayAdapter<JsonStruct.Group> {

        public static final int ROW_LAYOUT = R.layout.listrow_groups;

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
            InputFilter filterLength = new InputFilter.LengthFilter(MAXIMUM_GROUP_NAME_LENGTH);
            input.setFilters(new InputFilter[] {
                    filterLength
            });
            // input.setMaxLines(1);
            alert.setView(input);

            // When clicking on "OK", create the group.
            alert.setPositiveButton(getString(R.string.groups_alert_newgroup_ok),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String name = input.getText().toString().trim();
                            name = name.replaceAll("[\n\t\r]", "");
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
                Pair<Double, Double> p = getLocation();
                double lat = p.first;
                double lon = p.second;

                data.getAPI().getGroupListAfterCreateGroup(name, lat, lon,
                        new UnisonAPI.Handler<JsonStruct.GroupsList>() {
                            @Override
                            public void callback(GroupsList struct) {
                                GroupsActivity.this.mGroupsList
                                        .setAdapter(new GroupsAdapter(struct));
                            }

                            @Override
                            public void onError(Error error) {
                                if (error != null) {
                                    Log.d(TAG, error.toString());
                                }
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
            if (mProcessingAutoAction) {
                return;
            }

            final JsonStruct.Group group = (JsonStruct.Group) view.getTag();
            mGroupClicked = group; // saved for later processing

            if (group.password) {
                promptForPassword(group);
            } else {
                joinGroup(group, null);
            }
        }
    }

    private void joinGroup(final JsonStruct.Group group, String password) {
        mProcessingAutoAction = true;
        toggleActivityButtons(false);

        if (group != null) {
            Log.d(TAG, "Calling joinGroup with groupID = " + group.gid);

        } else {
            Log.d(TAG, "tried to join a group that was null!");
            mProcessingAutoAction = false;
            return;
        }

        final AppData data = AppData.getInstance(GroupsActivity.this);
        UnisonAPI api = data.getAPI();
        long uid = data.getUid();

        /*
         * UnisonAPI.Handler<JsonStruct.Success> handler = new
         * UnisonAPI.Handler<JsonStruct.Success>() {
         * @Override public void callback(Success struct) {
         * GroupsActivity.this.startActivity( new Intent(GroupsActivity.this,
         * GroupsMainActivity.class) .putExtra(Const.Strings.GROUP, group));
         * //optional: This creates a little stability gap because the user
         * could join multiple groups //when spamming another group while we're
         * finished joining him in a group but the //groupsMainActiity has not
         * been started yet. On the other hand it ensures compatibility
         * //regarding whether or not we keep the current activity mechanism in
         * our app. mProcessingAutoAction = false; }
         * @Override public void onError(Error error) { mProcessingAutoAction =
         * false; if (error != null) { Log.d(TAG, error.toString()); } if
         * (GroupsActivity.this != null) { Toast.makeText(GroupsActivity.this,
         * R.string.error_joining_group, Toast.LENGTH_LONG).show(); } } };
         */

        UnisonAPI.Handler<JsonStruct.Success> handler =
                new UnisonAPI.Handler<JsonStruct.Success>() {

                    @Override
                    public void callback(Success struct) {
                        // This is done because we don't want to be kicked from
                        // a autogoup
                        // if we join it using the history.
                        // This is in case of wrong automatic behavior.

                        if (group.automatic && ACTION_JOIN_GROUP.equals(mAction)
                                || ACTION_LEAVE_JOIN_GROUP.equals(mAction)) {
                            group.automatic = false;
                        }
                        startActivity(
                        new Intent(GroupsActivity.this, GroupsMainActivity.class)
                                .putExtra(Const.Strings.GROUP, group));
                    }

                    @Override
                    public void onError(Error error) {
                        mProcessingAutoAction = false;
                        toggleActivityButtons(true);

                        if (error != null) {
                            Log.d(TAG, error.toString());
                        }

                        Log.d(TAG, "The error was not due to an invalid group.");
                        Toast.makeText(GroupsActivity.this, R.string.error_joining_group,
                                Toast.LENGTH_LONG).show();
                        if (error != null && GroupsActivity.this != null) {
                            if (error.hasJsonError()) {
                                if (error.jsonError.error == UnisonAPI.ErrorCodes.INVALID_GROUP) {
                                    // here the group no longer exists, the user
                                    // needs to take an action:
                                    // you may comment this line for testing
                                    // purpose only!
                                    data.removeOneHistoryItem(group.gid);

                                    showErrorPopup();
                                } else if (error.jsonError.error
                                == UnisonAPI.ErrorCodes.PASSWORD_EXPECTED) {
                                    group.password = true;
                                    data.addToHistory(group);
                                    promptForPassword(group);
                                }
                            }
                        }
                    }

                };

        if (group != null) {
            mProcessingAutoAction = true; // we don't want the user to be able
                                          // to mess the app for a while
            if (group.password && password != null) {
                api.joinProtectedGroup(uid, group.gid, password, handler);
            } else {
                api.joinGroup(uid, group.gid, handler);
            }
        }
    }

    private void promptForPassword(final JsonStruct.Group group) {
        if (group.password) {
            AlertDialog.Builder builder = new AlertDialog.Builder(GroupsActivity.this);
            builder.setTitle(R.string.groups_password_dialog_title);

            LayoutInflater layoutInflater = (LayoutInflater)
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View dialogView = layoutInflater.inflate(R.layout.password_prompt_dialog, null);

            builder.setView(dialogView);

            final EditText password = (EditText)
                    dialogView.findViewById(R.id.groupPassword);
            DialogInterface.OnClickListener passwordClick = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == Dialog.BUTTON_POSITIVE) {
                        joinGroup(group, password.getText().toString());
                    } else if (which == Dialog.BUTTON_NEGATIVE) {
                        mProcessingAutoAction = false;
                        onRefresh();
                    }
                }
            };

            builder.setPositiveButton(getString(R.string.main_password_ok), passwordClick);
            builder.setNegativeButton(getString(R.string.main_password_cancel), passwordClick);

            // This is supposed to handle the situation where the user presses
            // the
            // BACK key too.
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    mProcessingAutoAction = false;
                    onRefresh();
                }
            });

            final AlertDialog dialog = builder.create();

            password.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                            .setEnabled(s.length() == AppData.getInstance(GroupsActivity.this)
                                    .getGroupPasswordLength());
                }

                @Override
                public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                    // Do nothing
                }

                @Override
                public void afterTextChanged(Editable arg0) {
                    // Do nothing
                }
            });

            dialog.show();
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        }
    }

    private Pair<Double, Double> getLocation() {
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
        return new Pair<Double, Double>(lat, lon);
    }

    private void recreateGroupThenJoin(String name) {
        if (name == null || name.equals("")) {
            Toast.makeText(GroupsActivity.this,
                    R.string.error_creating_group_empty_name, Toast.LENGTH_LONG).show();
        } else {
            AppData data = AppData.getInstance(GroupsActivity.this);
            Pair<Double, Double> p = getLocation();
            double lat = p.first;
            double lon = p.second;

            data.getAPI().createGroup(name, lat, lon,
                    new UnisonAPI.Handler<JsonStruct.Group>() {
                        @Override
                        public void callback(JsonStruct.Group struct) {
                            if ((struct == null || struct.gid == null)
                                    && GroupsActivity.this != null) {
                                // Log.d(TAG,
                                // "recreateGroup() callback: something went wrong: "
                                // +
                                // "group is null: " + (struct == null) +
                                // ", and its gid is null: " + (struct.gid ==
                                // null));
                                Toast.makeText(GroupsActivity.this,
                                        R.string.error_group_to_recreate,
                                        Toast.LENGTH_LONG).show();
                                mProcessingAutoAction = false;
                            } else {
                                // Log.d(TAG, "recreateGroup() callback:"
                                // + " going to join a recreated group, yay!");
                                joinGroup(struct, null);
                            }
                        }

                        @Override
                        public void onError(Error error) {
                            mProcessingAutoAction = false;
                            if (error != null) {
                                Log.d(TAG, error.toString());
                            }
                            if (GroupsActivity.this != null) {
                                Log.d(TAG,
                                        "recreateGroup() onError: we got an error from the server");
                                Toast.makeText(GroupsActivity.this,
                                        R.string.error_group_to_recreate,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    private void toggleActivityButtons(boolean bool) {
        ((Button) findViewById(R.id.createGroupBtn)).setEnabled(bool);
        updateSuggestionButton(bool);
    }

    private void setupSuggestionButton() {
        Button reDisplaySuggestion = (Button) findViewById(R.id.displaySuggestion);

        reDisplaySuggestion.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG, "Clicked on suggestionBtn, v instanceof Button: "
                        + (v instanceof Button)
                        + " text: " + ((Button) v).getText().toString());
                if (v instanceof Button && ((Button) v).getText()
                        .toString().equals(getString(R.string.groups_suggestion_goto_settings))) {
                    /*
                     * startActivityForResult( new
                     * Intent(android.provider.Settings
                     * .ACTION_LOCATION_SOURCE_SETTINGS), 1);
                     */
                    startActivity(
                    new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));

                } else if (!mProcessingAutoAction) {
                    showSuggestionDialog();
                }
            }
        });

        updateSuggestionButton(true);
    }

    // ============================= ERROR POPUP METHODS
    // ===========================
    private void showErrorPopup() {
        mProcessingAutoAction = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(GroupsActivity.this);
        builder.setTitle(R.string.group_no_longer_exists_dialog_title);
        LayoutInflater layoutInflater = (LayoutInflater)
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        int layout = R.layout.group_no_longer_exists_dialog;
        if (mGroupClicked.automatic) {
            layout = R.layout.automatic_group_no_longer_exists_dialog;
        }
        View dialogView = layoutInflater.inflate(layout, null);
        builder.setView(dialogView);

        // This is supposed to handle the situation where the user presses the
        // BACK key too.
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // mSuggestion = null;
                mSuggestionIsForeground = false;

                // switchSuggestionButtonState(false);
                // We could handle here whether the checkbox was checked or not,
                // but it makes more sense to do so only when the user presses a
                // button.
            }
        });

        mGroupNoLongerExistsDialog = builder.create();
        mGroupNoLongerExistsDialog.show();
    }

    public void errorDialogCreateGroupPressed(View view) {
        recreateGroupThenJoin(mGroupClicked.name);

        mGroupNoLongerExistsDialog.dismiss();
    }

    public void errorDialogGoHistoryActivityPressed(View view) {
        startActivity(new Intent(this, GroupsHistoryActivity.class));

        mGroupNoLongerExistsDialog.dismiss();
        mProcessingAutoAction = false;
    }

    public void errorDialogCancelPressed(View view) {
        mGroupNoLongerExistsDialog.dismiss();
        mProcessingAutoAction = false;
    }
}
