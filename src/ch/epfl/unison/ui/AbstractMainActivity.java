package ch.epfl.unison.ui;

import android.os.Bundle;

import ch.epfl.unison.R;

import com.actionbarsherlock.app.ActionBar;

/**
 * Provides some default behaviors. Not supposed to be instantiated directly,
 * but to be extended.
 * 
 * @see AbstractFragmentActivity
 * 
 * @author marc
 * 
 */
public abstract class AbstractMainActivity extends AbstractFragmentActivity {

	private static final String TAG = "ch.epfl.unison.UnisonMainActivity";

	private ActionBar mSupportActionBar;
	
	private boolean mIsDj = false;

	protected ActionBar getSupportActBar() {
		return mSupportActionBar;
	}

	protected abstract void handleExtras(Bundle extras);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		handleExtras(getIntent().getExtras());

		// Set up Action Bar
		mSupportActionBar = getSupportActionBar();
		mSupportActionBar.setDisplayHomeAsUpEnabled(true);
		mSupportActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

	}
	
	public void setDJ(boolean dj) {
		mIsDj = dj;
		getMenu().findItem(R.id.menu_item_manage_group).setVisible(dj);
	}
	
	public boolean getDJ() {
		return mIsDj;
	}

}
