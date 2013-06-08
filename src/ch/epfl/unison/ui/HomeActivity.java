
package ch.epfl.unison.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import ch.epfl.unison.R;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * @author marc bourqui
 */
public class HomeActivity extends SherlockActivity {

    private static final String TAG = "ch.epfl.unison.HomelistsActivity";

    private BroadcastReceiver mLogoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        // This activity should finish on logout.
        registerReceiver(mLogoutReceiver, new IntentFilter(AbstractMenu.ACTION_LOGOUT));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mLogoutReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean success = AbstractMenu.onCreateOptionsMenu(this, menu);
        if (success) {
            MenuItem refreshItem = menu.findItem(R.id.menu_item_refresh);
            refreshItem.setVisible(false);
        }
        return success;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return AbstractMenu.onOptionsItemSelected(this, null, item);
    }

    public void onButtonClickGroups(View view) {
        startActivity(new Intent(HomeActivity.this, GroupsActivity.class));
    }

    public void onButtonClickSolo(View view) {
        startActivity(new Intent(HomeActivity.this, SoloPlaylistsActivity.class));
    }

}
