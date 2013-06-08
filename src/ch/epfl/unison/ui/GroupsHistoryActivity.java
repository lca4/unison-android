
package ch.epfl.unison.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import ch.epfl.unison.AppData;
import ch.epfl.unison.Const;
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Louis Activity that is used to display the history for groups. This
 *         history is stored locally, not on the server.
 */
public class GroupsHistoryActivity extends SherlockActivity {

    private static final String TAG = "ch.epfl.unison.GroupHistoryActivity";
    // private Menu mMenu;
    private List<JsonStruct.Group> mGroupsHistory = null;
    private ListView mGroupsList;
    private JsonStruct.Group mGroupClicked = null;
    private boolean mAlreadyInGroup = false;
    // private AlertDialog mGroupNoLongerExistsDialog;

    private BroadcastReceiver mLogoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This activity should finish on logout.
        registerReceiver(mLogoutReceiver, new IntentFilter(AbstractMenu.ACTION_LOGOUT));
        setContentView(R.layout.group_history);

        mGroupsList = (ListView) findViewById(R.id.groupHistoryList);
        mGroupsList.setOnItemClickListener(new OnGroupSelectedListener());

        // ((Button) findViewById(R.id.deleteHistoryBtn))
        // .setOnClickListener(new OnDeleteHistoryListener());

        String caller = getIntent().getStringExtra(Const.Strings.CALLER);

        Log.d(TAG, "called by" + caller);

        if (caller != null) {
            mAlreadyInGroup = caller.contains("GroupsMainActivity");
            Log.d(TAG, "going in groupsHistoryActitvity from groupsMainActivity");
        }

        // get the map of visited groups, sorted by chronological order (newer
        // first).
        Map<Long, Pair<JsonStruct.Group, Date>> mapOfGroups = AppData
                .getInstance(this).getHistory();
        if (mapOfGroups == null) {
            mGroupsHistory = new ArrayList<JsonStruct.Group>();
        } else {
            mapOfGroupsToArrayListWithSort(mapOfGroups);
        }

        try {
            mGroupsList.setAdapter(new GroupsAdapter());
        } catch (NullPointerException e) {
            Log.w(TAG, "group or activity is null?", e);
        }
    }

    private void mapOfGroupsToArrayListWithSort(Map<Long, Pair<JsonStruct.Group, Date>> map) {
        List<Pair<JsonStruct.Group, Date>> listOfGroups =
                new ArrayList<Pair<JsonStruct.Group, Date>>(
                        map.values());
        sortChronological(listOfGroups);

        mGroupsHistory = new ArrayList<JsonStruct.Group>();

        for (Pair<JsonStruct.Group, Date> p : listOfGroups) {
            mGroupsHistory.add(p.first);
        }

    }

    private void sortChronological(List<Pair<JsonStruct.Group, Date>> list) {
        Collections.sort(list,
                new Comparator<Pair<JsonStruct.Group, Date>>() {
                    public int compare(Pair<JsonStruct.Group, Date> o1,
                            Pair<JsonStruct.Group, Date> o2) {
                        return -o1.second.compareTo(o2.second);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mLogoutReceiver);
    };

    // @Override
    // public boolean onCreateOptionsMenu(Menu menu) {
    // mMenu = menu;
    // return UnisonMenu.onCreateOptionsMenu(this, menu);
    // }

    // @Override
    // public boolean onOptionsItemSelected(MenuItem item) {
    // return UnisonMenu.onOptionsItemSelected(this, this, item);
    // }

    /** Adapter used to populate the ListView listing the groups. */
    private class GroupsAdapter extends ArrayAdapter<JsonStruct.Group> {

        public static final int ROW_LAYOUT = R.layout.listrow_groups_history;

        public GroupsAdapter() {
            super(GroupsHistoryActivity.this, 0, mGroupsHistory);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            JsonStruct.Group group = getItem(position);
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) GroupsHistoryActivity.
                        this.getSystemService(
                                Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(ROW_LAYOUT, parent, false);
            }
            ((TextView) view.findViewById(R.id.groupHistoryName)).setText(group.name);
            // Not using the subtitle for now.
            // String subtitle = null;
            // if (group.distance != null) {
            // subtitle = String.format("%s away - %d people.",
            // Uutils.distToString(group.distance), group.nbUsers);
            // } else {
            // String format = "%d person.";
            // if (group.nbUsers > 1) {
            // format = "%d people.";
            // }
            // subtitle = String.format(format, group.nbUsers);
            // }
            // ((TextView)
            // view.findViewById(R.id.nbParticipants)).setText(subtitle);

            view.setTag(group);
            return view;
        }
    }

    /**
     * When clicking on a group, send a request to the server and start
     * MainActivity.
     */
    private class OnGroupSelectedListener implements OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // UnisonAPI api =
            // AppData.getInstance(GroupsHistoryActivity.this).getAPI();
            // long uid =
            // AppData.getInstance(GroupsHistoryActivity.this).getUid();
            mGroupClicked = (JsonStruct.Group) view.getTag();

            // UnisonAPI.Handler<JsonStruct.Success> enterGroup =
            // new UnisonAPI.Handler<JsonStruct.Success>() {
            //
            // @Override
            // public void callback(Success struct) {
            //
            // //This is done because we don't want to be kicked from a autogoup
            // //if we join it using the history.
            // //This is in case of wrong automatic behavior.
            // group.automatic = false;
            // GroupsHistoryActivity.this.startActivity(
            // new Intent(GroupsHistoryActivity.this, GroupsMainActivity.class)
            // .putExtra(Const.Strings.GROUP, group)
            // .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            // // finish();
            // }
            //
            // @Override
            // public void onError(Error error) {
            // Log.d(TAG, error.toString());
            // if (GroupsHistoryActivity.this != null) {
            // Toast.makeText(GroupsHistoryActivity.this,
            // R.string.error_joining_group,
            // Toast.LENGTH_LONG).show();
            // }
            // }
            // };

            /*
             * if (mGroupClicked.password) { promptForPassword(mGroupClicked); }
             * else { joinGroup(mGroupClicked, null); }
             */
            Intent intent = new Intent(GroupsHistoryActivity.this, GroupsActivity.class)
                    .putExtra(Const.Strings.GROUP, mGroupClicked)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if (mAlreadyInGroup) {
                intent.setAction(GroupsActivity.ACTION_LEAVE_JOIN_GROUP);
            } else {
                intent.setAction(GroupsActivity.ACTION_JOIN_GROUP);
            }
            startActivity(intent);
        }
    }

    // Important remark: not used anymore, the server takes care of it
    // private void leaveThenJoinGroup(final
    // UnisonAPI.Handler<JsonStruct.Success> enterGroup,
    // final long uid,
    // final JsonStruct.Group group,
    // final String password) {
    // // Make sure the user is not marked as present in any group.
    // AppData data = AppData.getInstance(this);
    // final UnisonAPI api = data.getAPI();
    // api.leaveGroup(data.getUid(), new UnisonAPI.Handler<JsonStruct.Success>()
    // {
    //
    // @Override
    // public void callback(Success struct) {
    // Log.d(TAG, "successfully left group");
    //
    // if (password != null && group.password) {
    // api.joinProtectedGroup(uid, group.gid, password, enterGroup);
    // } else {
    // api.joinGroup(uid, group.gid, enterGroup);
    // }
    // }
    //
    // @Override
    // public void onError(Error error) {
    // Log.d(TAG, error.toString());
    // }
    // });
    // }

    /*
     * private void joinGroup(final JsonStruct.Group group, String password) {
     * final AppData data = AppData.getInstance(GroupsHistoryActivity.this);
     * UnisonAPI api = data.getAPI(); long uid = data.getUid();
     * UnisonAPI.Handler<JsonStruct.Success> handler = new
     * UnisonAPI.Handler<JsonStruct.Success>() {
     * @Override public void callback(Success struct) { //This is done because
     * we don't want to be kicked from a autogoup //if we join it using the
     * history. //This is in case of wrong automatic behavior. group.automatic =
     * false; GroupsHistoryActivity.this.startActivity( new
     * Intent(GroupsHistoryActivity.this, GroupsMainActivity.class)
     * .putExtra(Const.Strings.GROUP, group)
     * .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)); }
     * @Override public void onError(Error error) { if (error != null) {
     * Log.d(TAG, error.toString()); } if (GroupsHistoryActivity.this != null) {
     * if (error.hasJsonError() && error.jsonError.error ==
     * UnisonAPI.ErrorCodes.INVALID_GROUP) { //here the group no longer exists,
     * the user needs to take an action: //you may comment this line for testing
     * purpose only! data.removeOneHistoryItem(group.gid);
     * mGroupsHistory.remove(group); mGroupsList.setAdapter(new
     * GroupsAdapter()); showErrorPopup(); } else { Log.d(TAG,
     * "The error was not due to an invalid group.");
     * Toast.makeText(GroupsHistoryActivity.this, R.string.error_joining_group,
     * Toast.LENGTH_LONG).show(); } } } }; if (group.password && password !=
     * null) { // if (mAlreadyInGroup) { // leaveThenJoinGroup(handler, uid,
     * group, password); // } else { api.joinProtectedGroup(uid, group.gid,
     * password, handler); // } } else { // if (mAlreadyInGroup) { //
     * leaveThenJoinGroup(handler, uid, group, null); // } else {
     * api.joinGroup(uid, group.gid, handler); // } } }
     */

    /*
     * private void showErrorPopup() { AlertDialog.Builder builder = new
     * AlertDialog.Builder(GroupsHistoryActivity.this);
     * builder.setTitle(R.string.group_no_longer_exists_dialog_title);
     * LayoutInflater layoutInflater = (LayoutInflater)
     * getSystemService(Context.LAYOUT_INFLATER_SERVICE); int layout =
     * R.layout.group_no_longer_exists_dialog; if (mGroupClicked.automatic) {
     * layout = R.layout.automatic_group_no_longer_exists_dialog; } View
     * dialogView = layoutInflater.inflate(layout, null);
     * builder.setView(dialogView); mGroupNoLongerExistsDialog =
     * builder.create(); mGroupNoLongerExistsDialog.show(); } public void
     * errorDialogCreateGroupPressed(View view) { startActivity(new Intent(this,
     * GroupsActivity.class).setAction(
     * GroupsActivity.ACTION_CREATE_AND_JOIN_GROUP).addFlags(
     * Intent.FLAG_ACTIVITY_CLEAR_TOP)
     * .putExtra(Const.Strings.GROUP_TO_CREATE_NAME, mGroupClicked.name));
     * mGroupNoLongerExistsDialog.dismiss(); } public void
     * errorDialogGoGroupsActivityPressed(View view) { if (mAlreadyInGroup) {
     * startActivity(new Intent(this, GroupsActivity.class).setAction(
     * GroupsActivity.ACTION_LEAVE_GROUP).addFlags(
     * Intent.FLAG_ACTIVITY_CLEAR_TOP)); } else { startActivity(new Intent(this,
     * GroupsActivity.class).addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP)); }
     * mGroupNoLongerExistsDialog.dismiss(); finish(); } public void
     * errorDialogCancelPressed(View view) {
     * mGroupNoLongerExistsDialog.dismiss(); }
     */

    /*
     * private void promptForPassword(final JsonStruct.Group group) { if
     * (group.password) { AlertDialog.Builder builder = new
     * AlertDialog.Builder(GroupsHistoryActivity.this);
     * builder.setTitle(R.string.groups_password_dialog_title); LayoutInflater
     * layoutInflater = (LayoutInflater)
     * getSystemService(Context.LAYOUT_INFLATER_SERVICE); View dialogView =
     * layoutInflater.inflate(R.layout.password_prompt_dialog, null);
     * builder.setView(dialogView); final EditText password = (EditText)
     * dialogView.findViewById(R.id.groupPassword);
     * DialogInterface.OnClickListener passwordClick = new
     * DialogInterface.OnClickListener() {
     * @Override public void onClick(DialogInterface dialog, int which) { if
     * (which == Dialog.BUTTON_POSITIVE) { joinGroup(group,
     * password.getText().toString()); } } };
     * builder.setPositiveButton(getString(R.string.generic_ok), passwordClick);
     * builder.setNegativeButton(getString(R.string.generic_cancel),
     * passwordClick); final AlertDialog dialog = builder.create();
     * password.addTextChangedListener(new TextWatcher() {
     * @Override public void onTextChanged(CharSequence s, int start, int
     * before, int count) { dialog.getButton(DialogInterface.BUTTON_POSITIVE)
     * .setEnabled(s.length() == AppData.getInstance(GroupsHistoryActivity.this)
     * .getGroupPasswordLength()); }
     * @Override public void beforeTextChanged(CharSequence arg0, int arg1, int
     * arg2, int arg3) { //Do nothing }
     * @Override public void afterTextChanged(Editable arg0) { //Do nothing }
     * }); dialog.show();
     * dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false); } }
     */

    public void clearHistory(View view) {
        AppData data = AppData.getInstance(GroupsHistoryActivity.this);
        JsonStruct.Group currentGrp = null;
        if (data.clearHistory()) {
            if (mAlreadyInGroup) {
                currentGrp = mGroupsHistory.get(0);
                data.addToHistory(currentGrp);
            }

            mGroupsHistory = new ArrayList<JsonStruct.Group>();

            if (currentGrp != null) {
                mGroupsHistory.add(currentGrp);
            }

            mGroupsList.setAdapter(new GroupsAdapter());

        }
    }

}
