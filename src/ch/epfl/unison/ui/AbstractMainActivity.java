
package ch.epfl.unison.ui;

import android.os.Bundle;
import ch.epfl.unison.R;

/**
 * To be used when using a player fragment. Provides some default behaviors.
 * 
 * @see AbstractFragmentActivity
 * @author marc
 */
public abstract class AbstractMainActivity extends AbstractFragmentActivity {

    private static final String TAG = "ch.epfl.unison.UnisonMainActivity";

//    private ActionBar mSupportActionBar;

//    protected ActionBar getSupportActBar() {
//        return mSupportActionBar;
//    }

    protected abstract void handleExtras(Bundle extras);

    // protected abstract PlaylistItem getPlaylist();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleExtras(getIntent().getExtras());
    }

        // Set up Action Bar
//        mSupportActionBar = getSupportActionBar();
//        mSupportActionBar.setDisplayHomeAsUpEnabled(true);
//        mSupportActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);


}
