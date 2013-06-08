
package ch.epfl.unison.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import ch.epfl.unison.R;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;

/**
 * @author marc bourqui
 */
public class HomeActivity extends SherlockActivity {

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

}
