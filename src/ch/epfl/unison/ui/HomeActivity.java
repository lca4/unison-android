
package ch.epfl.unison.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;

import ch.epfl.unison.R;

/**
 * 
 * @author marc bourqui
 */
public class HomeActivity extends SherlockActivity implements AbstractMenu.OnRefreshListener {

    private static final String TAG = "ch.epfl.unison.HomelistsActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return AbstractMenu.onCreateOptionsMenu(this, menu);
    }

    public void onButtonClickGroups(View view) {
        startActivity(new Intent(HomeActivity.this, GroupsActivity.class));
    }

    public void onButtonClickSolo(View view) {
        startActivity(new Intent(HomeActivity.this, SoloPlaylistsActivity.class));
    }

    @Override
    public void onRefresh() {
        // Nothing to refresh
    }

}
