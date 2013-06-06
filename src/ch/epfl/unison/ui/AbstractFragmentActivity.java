
package ch.epfl.unison.ui;

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
import android.view.LayoutInflater;
import android.view.View;

import ch.epfl.unison.R;
import ch.epfl.unison.ui.AbstractMenu.OnRefreshListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import java.util.ArrayList;

/**
 * Provides some default behaviors. Not supposed to be instantiated directly,
 * but to be extended.<br />
 * <br />
 * Useful link: <a href=
 * "https://developer.android.com/guide/components/fragments.html#CommunicatingWithActivity"
 * >Android Developer Guide</a>
 * 
 * @author marc
 */
public abstract class AbstractFragmentActivity extends SherlockFragmentActivity
        implements OnRefreshListener {

    private String mTag = "ch.epfl.unison.UnisonFragmentActivity";
    private static final int INITIAL_DELAY = 500; // in ms.
    private static final int DEFAULT_RELOAD_INTERVAL = 30 * 1000; // in ms.
    private static int smReloadInterval;
    private boolean mIsForeground = false;
    private Menu mMenu;

    private TabsAdapter mTabsAdapter;
    private ViewPager mViewPager;
    private ActionBar mSupportActionBar;

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

    // /** Simple interface to be notified about playlist info updates. */
    // public interface OnContentInfoListener {
    // // void onPlaylistInfo(JsonStruct.PlaylistJS playlistInfo);
    //
    // void onContentInfo(Object contentInfo);
    // }

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
        unregisterReceiver(mLogoutReceiver);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTag = this.getClass().getName();

        setReloadInterval(DEFAULT_RELOAD_INTERVAL);

        // This activity should finish on logout.
        registerReceiver(mLogoutReceiver, new IntentFilter(
                AbstractMenu.ACTION_LOGOUT));

        // Set up the tabs & stuff.
        mViewPager = new ViewPager(this);
        mViewPager.setId(R.id.realtabcontent); // TODO change
        setContentView(mViewPager);
        setTabsAdapter(new TabsAdapter(this, mViewPager));

        // Set up Action Bar
        mSupportActionBar = getSupportActionBar();
        mSupportActionBar.setDisplayHomeAsUpEnabled(true);
        mSupportActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        return AbstractMenu.onCreateOptionsMenu(this, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return AbstractMenu.onOptionsItemSelected(this, this, item);
    }

    public void repaintRefresh(boolean isRefreshing) {
        if (mMenu == null) {
            Log.d(mTag, "repaintRefresh: mMenu is null");
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
            Log.d(mTag, "repaintRefresh: menu_item_refresh not found");
        }
    }

    /**
     * Default implementation. Should be overridden to suitable behavior.
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

    protected Menu getMenu() {
        return mMenu;
    }

    protected TabsAdapter getTabsAdapter() {
        return mTabsAdapter;
    }

    protected String getTag() {
        return mTag;
    }

    private void setTabsAdapter(TabsAdapter ta) {
        this.mTabsAdapter = ta;
    }

    protected ViewPager getViewPager() {
        return mViewPager;
    }

    // private void setViewPager(ViewPager vp) {
    // this.mViewPager = vp;
    // }

    protected ActionBar getSupportActBar() {
        return mSupportActionBar;
    }
    
    protected void showRefresh(boolean visible) {
        getMenu().findItem(R.id.menu_item_refresh).setVisible(visible);
    }
    
    protected void showSolo(boolean visible) {
        getMenu().findItem(R.id.menu_item_solo).setVisible(visible);
    }
    
    protected void showGroups(boolean visible) {
        getMenu().findItem(R.id.menu_item_groups).setVisible(visible);
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
     * @author <a href=
     *         "https://developer.android.com/reference/android/support/v4/view/ViewPager.html"
     *         > developer.android.com</a>
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

    public boolean onContextItemSelected(android.view.MenuItem item) {
        return super.onContextItemSelected(item);
    }

}
