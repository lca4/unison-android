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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import ch.epfl.unison.AppData;
import ch.epfl.unison.Const;
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.UnisonAPI;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Activity that is displayed once you're inside the group. Displays the music player
 * and information about the group (through fragments).
 *
 * @author lum
 */
public class MainActivity extends SherlockFragmentActivity implements UnisonMenu.OnRefreshListener {

    private static final String TAG = "ch.epfl.unison.MainActivity";
    private static final int RELOAD_INTERVAL = 30 * 1000;  // in ms.
    private static final int INITIAL_DELAY = 500; // in ms.

    private TabsAdapter mTabsAdapter;
    private ViewPager mViewPager;
    private Menu mMenu;

    private boolean mIsForeground = false;
    private Handler mHandler = new Handler();
    private Runnable mUpdater = new Runnable() {
        @Override
        public void run() {
            if (mIsForeground) {
                onRefresh();
                mHandler.postDelayed(this, RELOAD_INTERVAL);
            }
        }
    };

    private BroadcastReceiver mLogoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    private Set<OnGroupInfoListener> mListeners = new HashSet<OnGroupInfoListener>();

    private long mGroupId;

    public long getGroupId() {
        return mGroupId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleExtras(getIntent().getExtras());
        

        // This activity should finish on logout.
        registerReceiver(mLogoutReceiver, new IntentFilter(UnisonMenu.ACTION_LOGOUT));

        // Set up the tabs & stuff.
        mViewPager = new ViewPager(this);
        mViewPager.setId(R.id.realtabcontent); // TODO change
        setContentView(mViewPager);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mTabsAdapter = new TabsAdapter(this, mViewPager);
        mTabsAdapter.addTab(bar.newTab().setText(R.string.fragment_title_player),
                PlayerFragment.class, null);
        mTabsAdapter.addTab(bar.newTab().setText(R.string.fragment_title_stats),
                StatsFragment.class, null);
    }

    private void handleExtras(Bundle extras) {
        if (extras == null || !extras.containsKey(Const.Strings.GID)) {
            // Should never happen. If it does, redirect the user to the groups list.
            startActivity(new Intent(this, GroupsActivity.class));
            finish();
        } else {
            mGroupId = extras.getLong(Const.Strings.GID);
            AppData.getInstance(this).addToHistory(mGroupId);
            Log.i(TAG, "joined group " + mGroupId);
            if (extras.containsKey(Const.Strings.NAME)) {
                setTitle(extras.getString(Const.Strings.NAME));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsForeground = true;
        mHandler.postDelayed(mUpdater, INITIAL_DELAY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsForeground = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mLogoutReceiver);
    }

    public void repaintRefresh(boolean isRefreshing) {
        if (mMenu == null) {
            return;
        }

        MenuItem refreshItem = mMenu.findItem(R.id.menu_item_refresh);
        if (refreshItem != null) {
            if (isRefreshing) {
                LayoutInflater inflater = (LayoutInflater) getSupportActionBar()
                        .getThemedContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View refreshView = inflater.inflate(
                        R.layout.actionbar_indeterminate_progress, null);
                refreshItem.setActionView(refreshView);
            } else {
                refreshItem.setActionView(null);
            }
        }
    }

    @Override
    public void onRefresh() {
        repaintRefresh(true);
        UnisonAPI api = AppData.getInstance(this).getAPI();
        api.getGroupInfo(mGroupId, new UnisonAPI.Handler<JsonStruct.Group>() {

            @Override
            public void callback(JsonStruct.Group struct) {
                try {
                    MainActivity.this.onGroupInfo(struct);
                    MainActivity.this.dispatchGroupInfo(struct);
                    MainActivity.this.repaintRefresh(false);
                } catch (NullPointerException e) {
                    Log.w(TAG, "group or activity is null?", e);
                }
            }

            @Override
            public void onError(UnisonAPI.Error error) {
                Log.d(TAG, error.toString());
                if (MainActivity.this != null) {
                    Toast.makeText(MainActivity.this, R.string.error_loading_info,
                            Toast.LENGTH_LONG).show();
                    MainActivity.this.repaintRefresh(false);
                }
            }

        });
    }

    private void onGroupInfo(JsonStruct.Group group) {
        setTitle(group.name);
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
            startActivity(new Intent(this, GroupsActivity.class)
                    .setAction(GroupsActivity.ACTION_LEAVE_GROUP)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /** Simple interface to be notified about group info updates. */
    public interface OnGroupInfoListener {
        void onGroupInfo(JsonStruct.Group groupInfo);
    }

    public void dispatchGroupInfo(JsonStruct.Group groupInfo) {
        for (OnGroupInfoListener listener : mListeners) {
            listener.onGroupInfo(groupInfo);
        }
    }

    public void registerGroupInfoListener(OnGroupInfoListener listener) {
        mListeners.add(listener);
    }

    public void unregisterGroupInfoListener(OnGroupInfoListener listener) {
        mListeners.remove(listener);
    }


    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost.  It relies on a
     * trick.  Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show.  This is not sufficient for switching
     * between pages.  So instead we make the content part of the tab host
     * 0dp high (it is not shown) and the TabsAdapter supplies its own dummy
     * view to show as the tab content.  It listens to changes in tabs, and takes
     * care of switch to the correct paged in the ViewPager whenever the selected
     * tab changes.
     *
     * @author no-freaking-idea
     */
    public static class TabsAdapter extends FragmentPagerAdapter
            implements ActionBar.TabListener, ViewPager.OnPageChangeListener {
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
            return Fragment.instantiate(mContext, info.mClass.getName(), info.mArgs);
        }

        @Override
        public void onPageSelected(int position) {
            mActionBar.setSelectedNavigationItem(position);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }
        @Override
        public void onPageScrollStateChanged(int state) { }

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
        public void onTabUnselected(Tab tab, FragmentTransaction ft) { }
        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) { }
    }
}
