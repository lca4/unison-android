
package ch.epfl.unison.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.actionbarsherlock.app.SherlockActivity;

import ch.epfl.unison.R;

/**
 * 
 * @author marc bourqui
 */
public class HomeActivity extends SherlockActivity {

    private static final String TAG = "ch.epfl.unison.HomelistsActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
    }

    public void onButtonClickGroups(View view) {
        startActivity(new Intent(HomeActivity.this, GroupsActivity.class));
    }

    public void onButtonClickSolo(View view) {
        startActivity(new Intent(HomeActivity.this, SoloPlaylistsActivity.class));
    }

}
