package ch.epfl.unison.ui;

import android.os.Bundle;

import ch.epfl.unison.R;

import com.actionbarsherlock.app.ActionBar;

/**
 * Provides some default behaviors. Not supposed to be instantiated directly,
 * but to be extended.
 * 
 * @see UnisonFragmentActivity
 * 
 * @author marc
 * 
 */
public abstract class UnisonMainActivity extends UnisonFragmentActivity {

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

		setTag(TAG);

		// Set up Action Bar
		mSupportActionBar = getSupportActionBar();
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

	}
	
	public void setDJ(boolean dj) {
		mIsDj = dj;
		getMenu().findItem(R.id.menu_item_manage_group).setVisible(dj);
	}
	
	public boolean getDJ() {
		return mIsDj;
	}

}
