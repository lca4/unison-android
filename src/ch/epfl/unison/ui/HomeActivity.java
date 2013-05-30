
package ch.epfl.unison.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import ch.epfl.unison.R;

/*
 * TODO
 * 
 * - retrieve playlists stored in android database
 * - store playlist to android database 
 * 
 */

/**
 * Raw copy of GroupsActivity. Has to be properly cleaned up.
 * 
 * @author marc bourqui
 */
public class HomeActivity extends AbstractFragmentActivity {

    private static final String TAG = "ch.epfl.unison.HomelistsActivity";
    private static final int RELOAD_INTERVAL = 120 * 1000; // in ms.

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        setReloadInterval(RELOAD_INTERVAL);
    }

    @Override
    public void onRefresh() {
        // Do nothing
    }

    public void onButtonClickGroups(View view) {
        startActivity(new Intent(HomeActivity.this, GroupsActivity.class));
    }

    public void onButtonClickSolo(View view) {
        startActivity(new Intent(HomeActivity.this, SoloPlaylistsActivity.class));
    }

}
