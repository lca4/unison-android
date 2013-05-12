package ch.epfl.unison.ui;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import ch.epfl.unison.R;
import ch.epfl.unison.ui.UnisonMenu.OnRefreshListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * Provides some default behaviors. Not supposed to be instantiated directly,
 * but to be extended.
 * 
 * @author marc
 * 
 */
public abstract class UnisonFragmentActivity extends SherlockFragmentActivity
		implements OnRefreshListener {

	private static String smTag = "ch.epfl.unison.UnisonFragmentActivity";
	private static final int INITIAL_DELAY = 500; // in ms.
	private static int smReloadInterval;
	private boolean mIsForeground = false;
	private Menu mMenu;

	private TabsAdapter mTabsAdapter;
	private ViewPager mViewPager;

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

		setReloadInterval(30 * 1000);

		// This activity should finish on logout.
		registerReceiver(getLogoutReceiver(), new IntentFilter(
				UnisonMenu.ACTION_LOGOUT));

		// Set up the tabs & stuff.
		setViewPager(new ViewPager(this));
		getViewPager().setId(R.id.realtabcontent); // TODO change
		setContentView(getViewPager());
		setTabsAdapter(new TabsAdapter(this, getViewPager()));
	}

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

	/**
	 * Default implementation. Should be overridden to suitable behavior.
	 * 
	 * Default implementation: repaintRefresh(true)
	 */
	@Override
	public void onRefresh() {
		repaintRefresh(true);
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

	protected TabsAdapter getTabsAdapter() {
		return mTabsAdapter;
	}

	private void setTabsAdapter(TabsAdapter ta) {
		this.mTabsAdapter = ta;
	}

	protected ViewPager getViewPager() {
		return mViewPager;
	}

	private void setViewPager(ViewPager vp) {
		this.mViewPager = vp;
	}
	
	/**
	 * This is a helper class that implements the management of tabs and all
	 * details of connecting a ViewPager with associated TabHost. It relies on a
	 * trick. Normally a tab host has a simple API for supplying a View or
	 * Intent that each tab will show. This is not sufficient for switching
	 * between pages. So instead we make the content part of the tab host 0dp
	 * high (it is not shown) and the TabsAdapter supplies its own dummy view to
	 * show as the tab content. It listens to changes in tabs, and takes care of
	 * switch to the correct paged in the ViewPager whenever the selected tab
	 * changes.
	 * 
	 * @author no-freaking-idea
	 */
	public static class TabsAdapter extends FragmentPagerAdapter implements
			ActionBar.TabListener, ViewPager.OnPageChangeListener {
		private final Context mContext;
		private final ActionBar mActionBar;
		private final ViewPager mViewPager;
		private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

		/** No idea what this does. Copy-pasted this stuff from somewhere. */
		static final class TabInfo {
			private final Class<?> mClass;
			private final Bundle mArgs;

			TabInfo(Class<?> clss, Bundle args) {
				mClass = clss;
				mArgs = args;
			}
		}

		public TabsAdapter(SherlockFragmentActivity activity, ViewPager pager) {
			super(activity.getSupportFragmentManager());
			mContext = activity;
			mActionBar = activity.getSupportActionBar();
			mViewPager = pager;
			mViewPager.setAdapter(this);
			mViewPager.setOnPageChangeListener(this);
		}

		public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {
			TabInfo info = new TabInfo(clss, args);
			tab.setTag(info);
			tab.setTabListener(this);
			mTabs.add(info);
			mActionBar.addTab(tab);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mTabs.size();
		}

		@Override
		public Fragment getItem(int position) {
			TabInfo info = mTabs.get(position);
			return Fragment.instantiate(mContext, info.mClass.getName(),
					info.mArgs);
		}

		@Override
		public void onPageSelected(int position) {
			mActionBar.setSelectedNavigationItem(position);
		}

		@Override
		public void onPageScrolled(int position, float positionOffset,
				int positionOffsetPixels) {
		}

		@Override
		public void onPageScrollStateChanged(int state) {
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			mViewPager.setCurrentItem(tab.getPosition());
			Object tag = tab.getTag();
			for (int i = 0; i < mTabs.size(); ++i) {
				if (mTabs.get(i) == tag) {
					mViewPager.setCurrentItem(i);
				}
			}
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	}

}
