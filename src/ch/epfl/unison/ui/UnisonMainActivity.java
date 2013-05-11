package ch.epfl.unison.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;

import ch.epfl.unison.R;
import ch.epfl.unison.ui.SoloMainActivity.TabsAdapter;
import ch.epfl.unison.ui.UnisonMenu.OnRefreshListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

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

	private static String smTag = "ch.epfl.unison.UnisonMainActivity";
	private static final int INITIAL_DELAY = 500; // in ms.
	private static int smReloadInterval;
	private boolean mIsForeground = false;
	private Menu mMenu;

	private ActionBar mSupportActionBar;

	private boolean mIsDJ = false;

	private Handler mHandler = new Handler();
	private Runnable mUpdater = new Runnable() {
		@Override
		public void run() {
			if (isForeground()) {
				onRefresh();
				getHandler().postDelayed(this, getReloadInterval());
			}
		}
	};

	private BroadcastReceiver mLogoutReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			finish();
		}
	};

	@Override
	protected void onPause() {
		super.onPause();
		setToForeground(false);
	}

	@Override
	protected void onResume() {
		super.onResume();
		setToForeground(true);
		getHandler().postDelayed(getUpdater(), getInitialDelay());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(getLogoutReceiver());
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		handleExtras(getIntent().getExtras());

		setReloadInterval(30 * 1000);

		// This activity should finish on logout.
		registerReceiver(getLogoutReceiver(), new IntentFilter(
				UnisonMenu.ACTION_LOGOUT));

		// Set up the tabs & stuff.
		setViewPager(new ViewPager(this));
		getViewPager().setId(R.id.realtabcontent); // TODO change
		setContentView(getViewPager());

		mSupportActionBar = getSupportActionBar();
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		setTabsAdapter(new TabsAdapter(this, getViewPager()));
	}

	protected abstract void handleExtras(Bundle extras);

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mMenu = menu;
		return UnisonMenu.onCreateOptionsMenu(this, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return UnisonMenu.onOptionsItemSelected(this, this, item);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			startActivity(new Intent(this, GroupsActivity.class).setAction(
					GroupsActivity.ACTION_LEAVE_GROUP).addFlags(
					Intent.FLAG_ACTIVITY_CLEAR_TOP));
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	public void repaintRefresh(boolean isRefreshing) {
		if (mMenu == null) {
			Log.d(smTag, "repaintRefresh: mMenu is null");
			return;
		}

		MenuItem refreshItem = mMenu.findItem(R.id.menu_item_refresh);
		if (refreshItem != null) {
			if (isRefreshing) {
				LayoutInflater inflater = (LayoutInflater) getSupportActionBar()
						.getThemedContext().getSystemService(
								Context.LAYOUT_INFLATER_SERVICE);
				View refreshView = inflater.inflate(
						R.layout.actionbar_indeterminate_progress, null);
				refreshItem.setActionView(refreshView);
			} else {
				refreshItem.setActionView(null);
			}
		} else {
			Log.d(smTag, "repaintRefresh: menu_item_refresh not found");
		}
	}

	protected boolean isForeground() {
		return mIsForeground;
	}

	protected void setToForeground(boolean isForeground) {
		this.mIsForeground = isForeground;
	}

	protected BroadcastReceiver getLogoutReceiver() {
		return mLogoutReceiver;
	}

	protected Handler getHandler() {
		return mHandler;
	}

	protected static int getInitialDelay() {
		return INITIAL_DELAY;
	}

	protected static int getReloadInterval() {
		return smReloadInterval;
	}

	protected static void setReloadInterval(int reloadInterval) {
		smReloadInterval = reloadInterval;
	}

	protected Runnable getUpdater() {
		return mUpdater;
	}

	protected static void setTag(String tag) {
		smTag = tag;
	}

	protected Menu getMenu() {
		return mMenu;
	}

	protected void setDJ(boolean dj) {
		mIsDJ = dj;
		getMenu().findItem(R.id.menu_item_manage_group).setVisible(dj);
	}

	protected ActionBar getSupportActBar() {
		return mSupportActionBar;
	}

}
