
package ch.epfl.unison.ui;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import ch.epfl.unison.AppData;
import ch.epfl.unison.Const;
import ch.epfl.unison.R;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Abstract base class for all activities that give access to the menu.
 * 
 * @author lum
 */
public abstract class AbstractMenu {

    @SuppressWarnings("unused")
    private static final String TAG = "ch.epfl.chunison.ui.UnisonMenu";

    public static final String ACTION_LOGOUT = "ch.epfl.unison.action.LOGOUT";

    public static boolean onCreateOptionsMenu(SherlockActivity activity, Menu menu) {
        MenuInflater inflater = activity.getSupportMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public static boolean onCreateOptionsMenu(SherlockFragmentActivity activity, Menu menu) {
        MenuInflater inflater = activity.getSupportMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public static boolean onOptionsItemSelected(Activity activity,
            OnRefreshListener listener, MenuItem item) {
        AppData data;

        switch (item.getItemId()) {
            case R.id.menu_item_refresh:
                if (listener != null) {
                    listener.onRefresh();
                }
                break;
            case R.id.menu_item_ratings:
                activity.startActivity(new Intent(activity, GroupsRatingsActivity.class));
                break;
            case R.id.menu_item_prefs:
                activity.startActivity(new Intent(activity, PrefsActivity.class));
                break;
            case R.id.menu_item_help:
                activity.startActivity(new Intent(activity, HelpActivity.class));
                break;
            case R.id.menu_item_groups:
                activity.startActivity(new Intent(activity, GroupsActivity.class)
                        .setAction(
                                GroupsActivity.ACTION_FROM_SOLO).addFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TOP));
                break;
            case R.id.menu_item_solo:
                // check if the user is in a group:
                data = AppData.getInstance(activity);
                if (data.getInGroup()) {
                    // here we notify the user that he should leave the group
                    // first:
                    if (activity != null) {
                        Toast.makeText(activity, activity.getString(
                                R.string.error_solo_while_in_group), Toast.LENGTH_LONG).show();
                    }
                    break;
                }
                activity.startActivity(new Intent(activity, SoloPlaylistsActivity.class).addFlags(
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                break;
            case R.id.menu_item_history:
                activity.startActivity(new Intent(activity, GroupsHistoryActivity.class).putExtra(
                        Const.Strings.CALLER, activity.getClass().getName()));
                break;
            case R.id.menu_item_logout:

                // check if the user is in a group:
                data = AppData.getInstance(activity);
                if (data.getInGroup()) {
                    // here we notify the user that he should leave the group
                    // first:
                    if (activity != null) {
                        Toast.makeText(activity, activity.getString(
                                R.string.error_logout_while_in_group), Toast.LENGTH_LONG).show();
                    }
                    break;
                }

                activity.startActivity(new Intent(activity, LoginActivity.class)
                        .putExtra(Const.Strings.LOGOUT, true));
                // Send broadcast to all activities that can only be used when
                // logged in.
                activity.sendBroadcast(new Intent().setAction(ACTION_LOGOUT));
                break;
            case R.id.menu_item_manage_group:
                if (activity instanceof GroupsMainActivity
                        && ((GroupsMainActivity) activity).isDJ()) {
                    // TODO is it a problem if we call this here?
                    ((GroupsMainActivity) activity).displayPasswordDialog();
                }
                break;
            case android.R.id.home: // if using home button from menu: R.id.home
                // app icon in Action Bar clicked; go home
                // activity.startActivity(new Intent(activity,
                // HomeActivity.class));
                // activity.startActivity(new Intent(activity,
                // GroupsActivity.class)
                // .setAction(GroupsActivity.ACTION_LEAVE_GROUP)
                // .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));

                if (!(activity instanceof HomeActivity)) {
                    // In GroupsMain we prefer going to Groups and leave the
                    // group.
                    if (activity instanceof GroupsMainActivity) {
                        activity.startActivity(new Intent(activity, GroupsActivity.class)
                                .setAction(
                                        GroupsActivity.ACTION_LEAVE_GROUP).addFlags(
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    } else {
                        activity.startActivity(new Intent(activity, HomeActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    }
                    // Make sure the activity is finished, even if it was at the
                    // bottom of the stack.
                    activity.finish();
                }
                break;
            default:
                break; // Should never happen.
        }
        return true;
    }

    /** Simple interface to notify listeners when the refresh button is hit. */
    public interface OnRefreshListener {
        void onRefresh();
    }
}
