
package ch.epfl.unison.ui;

import java.util.HashSet;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;

import ch.epfl.unison.AppData;
import ch.epfl.unison.Const;
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.JsonStruct.Success;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;
import ch.epfl.unison.data.PlaylistItem;

/**
 * Activity that is displayed once you're inside the group. Displays the music
 * player and information about the group (through fragments).
 * 
 * @see AbstractMainActivity
 * @author lum
 */
public class GroupsMainActivity extends AbstractMainActivity {

    /** Simple interface to be notified about group info updates. */
    public interface OnGroupInfoListener {
        void onGroupInfo(JsonStruct.Group groupInfo);
    }

    private static final String TAG = "ch.epfl.unison.MainActivity";

    private static final double MAX_DISTANCE = 2000;

    private Set<OnGroupInfoListener> mListeners = new HashSet<OnGroupInfoListener>();

    private JsonStruct.Group mGroup;

    private void autoLeave() {
        AppData data = AppData.getInstance(this);
        Location currentLoc = data.getLocation();
        if (currentLoc != null && mGroup.lat != null && mGroup.lon != null
                && mGroup.automatic) {
            double lat = currentLoc.getLatitude();
            double lon = currentLoc.getLongitude();
            float[] res = new float[1];

            Location.distanceBetween(mGroup.lat, mGroup.lon, lat, lon, res);
            double dist = res[0];

            if (dist > MAX_DISTANCE) {
                onKeyDown(KeyEvent.KEYCODE_BACK, null);
            }
        }
    }

    public void dispatchGroupInfo(JsonStruct.Group groupInfo) {
        for (OnGroupInfoListener listener : mListeners) {
            listener.onGroupInfo(groupInfo);
        }
    }

    public long getGroupId() {
        return mGroup.gid;
    }

    protected void handleExtras(Bundle extras) {
        if (extras == null || !extras.containsKey(Const.Strings.GROUP)) {
            // Should never happen. If it does, redirect the user to the groups
            // list.
            Log.d(TAG, "Tried creating mainActivity"
                    + " without coming from groupsActivity! Going to close");
            startActivity(new Intent(this, GroupsActivity.class));
            finish();
        } else {
            mGroup = (JsonStruct.Group) extras.get(Const.Strings.GROUP);
            Log.i(TAG, "joined group " + getGroupId());

            setTitle(mGroup.name);
            AppData.getInstance(this).addToHistory(mGroup);

        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // First choice: we restart the activity:
        startActivity(intent);
        finish();

        // Second choice:
        // setIntent(intent); //optional
        // handleExtras(intent.getExtras());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getTabsAdapter().addTab(
                getSupportActBar().newTab().setText(
                        R.string.fragment_title_player),
                GroupsPlayerFragment.class, null);
        getTabsAdapter().addTab(
                getSupportActBar().newTab().setText(
                        R.string.fragment_title_stats),
                GroupsStatsFragment.class, null);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean res = super.onCreateOptionsMenu(menu);
        getMenu().findItem(R.id.menu_item_history).setVisible(true);
        return res;
    }

    private void onGroupInfo(JsonStruct.Group group) {
        setTitle(group.name);
    }

    @Override
    public void onRefresh() {
        super.onRefresh();
        UnisonAPI api = AppData.getInstance(this).getAPI();

        autoLeave();

        api.getGroupInfo(getGroupId(),
                new UnisonAPI.Handler<JsonStruct.Group>() {

                    @Override
                    public void callback(JsonStruct.Group struct) {
                        try {
                            GroupsMainActivity.this.onGroupInfo(struct);
                            GroupsMainActivity.this.dispatchGroupInfo(struct);
                            GroupsMainActivity.this.repaintRefresh(false);
                        } catch (NullPointerException e) {
                            Log.w(TAG, "group or activity is null?", e);
                        }
                    }

                    @Override
                    public void onError(UnisonAPI.Error error) {
                        Log.d(TAG, error.toString());
                        if (GroupsMainActivity.this != null) {
                            Toast.makeText(GroupsMainActivity.this,
                                    R.string.error_loading_info,
                                    Toast.LENGTH_LONG).show();
                            GroupsMainActivity.this.repaintRefresh(false);
                        }
                    }

                });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            startActivity(new Intent(this, GroupsActivity.class).setAction(
                    GroupsActivity.ACTION_LEAVE_GROUP).addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void registerGroupInfoListener(OnGroupInfoListener listener) {
        mListeners.add(listener);
    }

    public void unregisterGroupInfoListener(OnGroupInfoListener listener) {
        mListeners.remove(listener);
    }

    private DialogInterface.OnClickListener mPasswordClick = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == Dialog.BUTTON_POSITIVE) {
                String password = ((EditText) ((Dialog) dialog).findViewById(R.id.groupPassword))
                        .getText().toString();
                sendPassword(password);
            }
        }
    };

    private void sendPassword(String pw) {
        AppData data = AppData.getInstance(GroupsMainActivity.this);
        UnisonAPI api = data.getAPI();

        api.setGroupPassword(mGroup.gid, pw,
                new UnisonAPI.Handler<JsonStruct.Success>() {

                    @Override
                    public void callback(Success struct) {
                        if (GroupsMainActivity.this != null) {
                            Toast.makeText(GroupsMainActivity.this,
                                    R.string.main_success_setting_password,
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(Error error) {
                        if (GroupsMainActivity.this != null) {
                            Toast.makeText(GroupsMainActivity.this,
                                    R.string.error_setting_password,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    public void displayPasswordDialog() {
        if (isDJ()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(GroupsMainActivity.this);
            builder.setTitle(R.string.main_set_password_title);
            LayoutInflater layoutInflater = (LayoutInflater)
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View dialogView = layoutInflater.inflate(R.layout.set_password_dialog, null);
            builder.setView(dialogView);
            EditText password = (EditText) dialogView.findViewById(R.id.groupPassword);

            builder.setPositiveButton(getString(R.string.main_password_ok), mPasswordClick);
            builder.setNegativeButton(getString(R.string.main_password_cancel), mPasswordClick);

            final AlertDialog dialog = builder.create();

            ((Button) dialogView.findViewById(R.id.rstPassword))
                    .setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            sendPassword("");
                            dialog.dismiss();
                        }
                    });

            password.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    dialog.getButton(Dialog.BUTTON_POSITIVE)
                            .setEnabled(s.length() == AppData
                                    .getInstance(GroupsMainActivity.this).getGroupPasswordLength());
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
            dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
        }
    }

//    @Override
//    protected PlaylistItem getPlaylist() {
//        return null;
//    }

}
