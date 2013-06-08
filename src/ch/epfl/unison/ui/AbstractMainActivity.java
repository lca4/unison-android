
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

    private boolean mIsDj = false;

    protected abstract void handleExtras(Bundle extras);

    // protected abstract PlaylistItem getPlaylist();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleExtras(getIntent().getExtras());
    }

    public void setDJ(boolean dj) {
        mIsDj = dj;
        getMenu().findItem(R.id.menu_item_manage_group).setVisible(mIsDj);
    }

    public boolean isDJ() {
        return mIsDj;
    }

}
