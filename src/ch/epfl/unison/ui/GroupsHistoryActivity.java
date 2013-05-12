
package ch.epfl.unison.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import ch.epfl.unison.AppData;
import ch.epfl.unison.Const;
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.JsonStruct.Success;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * @author Louis Activity that is used to display the history for groups. This
 *         history is stored locally, not on the server.
 */
public class GroupsHistoryActivity extends SherlockActivity {

    private static final String TAG = "ch.epfl.unison.GroupHistoryActivity";
    private Menu mMenu;
    private List<JsonStruct.Group> mGroupsHistory = null;
    private ListView mGroupsList;
    private boolean mAlreadyInGroup = false;

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

        ((Button) findViewById(R.id.deleteHistoryBtn))
                .setOnClickListener(new OnDeleteHistoryListener());

        String caller = getIntent().getStringExtra(Const.Strings.CALLER);

        Log.d(TAG, "called by" + caller);

        if (caller != null) {
            mAlreadyInGroup = caller.contains("MainActivity");
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
            repaintRefresh(false);
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

    // @Override
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

        public static final int ROW_LAYOUT = R.layout.group_history_row;

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

    // @Override
    // public void onRefresh() {
    // repaintRefresh(true);
    // //No server comm for now.
    // }

    public void repaintRefresh(boolean isRefreshing) {
        if (mMenu == null) {
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
        }
    }

    /**
     * When clicking on a group, send a request to the server and start
     * MainActivity.
     */
    private class OnGroupSelectedListener implements OnItemClickListener {
        // FIXME This is a duplicate of the listener in GroupsActivity.
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            UnisonAPI api = AppData.getInstance(GroupsHistoryActivity.this).getAPI();
            long uid = AppData.getInstance(GroupsHistoryActivity.this).getUid();
            final JsonStruct.Group group = (JsonStruct.Group) view.getTag();

            UnisonAPI.Handler<JsonStruct.Success> enterGroup =
                    new UnisonAPI.Handler<JsonStruct.Success>() {

                        @Override
                        public void callback(Success struct) {

                            // This is done because we don't want to be kicked
                            // from a autogoup if we join it
                            // using the history. This is in case of wrong
                            // automatic behavior.
                            group.automatic = false;
                            GroupsHistoryActivity.this
                                    .startActivity(
                                    new Intent(GroupsHistoryActivity.this, GroupsMainActivity.class)
                                            .putExtra(Const.Strings.GROUP, group));
                            finish();
                        }

                        @Override
                        public void onError(Error error) {
                            Log.d(TAG, error.toString());
                            if (GroupsHistoryActivity.this != null) {
                                Toast.makeText(GroupsHistoryActivity.this,
                                        R.string.error_joining_group,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    };

            if (mAlreadyInGroup) {
                leaveThenJoinGroup(enterGroup, uid, group.gid);
            } else {
                api.joinGroup(uid, group.gid, enterGroup);
            }
        }
    }

    private void leaveThenJoinGroup(final UnisonAPI.Handler<JsonStruct.Success> enterGroup,
            final long uid,
            final long gid) {
        // Make sure the user is not marked as present in any group.
        AppData data = AppData.getInstance(this);
        UnisonAPI api = data.getAPI();
        api.leaveGroup(data.getUid(), new UnisonAPI.Handler<JsonStruct.Success>() {

            @Override
            public void callback(Success struct) {
                Log.d(TAG, "successfully left group");
                AppData.getInstance(GroupsHistoryActivity.this).getAPI()
                        .joinGroup(uid, gid, enterGroup);
            }

            @Override
            public void onError(Error error) {
                Log.d(TAG, error.toString());
            }
        });
    }

    /**
     * Calls delete history on AppData.
     */
    private class OnDeleteHistoryListener implements OnClickListener {

        @Override
        public void onClick(View view) {
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

                repaintRefresh(false);
            }
        }

    }
}
