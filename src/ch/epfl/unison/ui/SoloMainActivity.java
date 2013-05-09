
package ch.epfl.unison.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import ch.epfl.unison.AppData;
import ch.epfl.unison.Const;
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.UnisonAPI;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Activity that is displayed once you're inside the group. Displays the music
 * player and information about the group (through fragments).
 * 
 * @author lum
 */
public class SoloMainActivity extends UnisonFragmentActivity { 
// extends SherlockFragmentActivity implements UnisonMenu.OnRefreshListener {

    private static final String TAG = "ch.epfl.unison.SoloMainActivity";
    private static final int RELOAD_INTERVAL = 30 * 1000; // in ms.
//    private static final int INITIAL_DELAY = 500; // in ms.

    private TabsAdapter mTabsAdapter;
    private ViewPager mViewPager;
//    private Menu mMenu;

//    private boolean mIsForeground = false;
//    private Handler mHandler = new Handler();
//    private Runnable mUpdater = new Runnable() {
//        @Override
//        public void run() {
//            if (isForeground()) {
//                onRefresh();
//                getHandler().postDelayed(this, RELOAD_INTERVAL);
//            }
//        }
//    };

//    private BroadcastReceiver mLogoutReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            finish();
//        }
//    };

    private Set<OnPlaylistInfoListener> mListeners = new HashSet<OnPlaylistInfoListener>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleExtras(getIntent().getExtras());
        
        setReloadInterval(RELOAD_INTERVAL);
        setTag(TAG);

        // This activity should finish on logout.
        registerReceiver(getLogoutReceiver(), new IntentFilter(UnisonMenu.ACTION_LOGOUT));

        // Set up the tabs & stuff.
        mViewPager = new ViewPager(this);
        mViewPager.setId(R.id.realtabcontent); // TODO change
        setContentView(mViewPager);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mTabsAdapter = new TabsAdapter(this, mViewPager);
        mTabsAdapter.addTab(bar.newTab().setText(R.string.solo_player_fragment_title),
                SoloPlayerFragment.class, null);
        mTabsAdapter.addTab(bar.newTab().setText(R.string.solo_playlist_fragment_title),
                SoloTracksFragment.class, null);
    }

    private void handleExtras(Bundle extras) {
        if (extras == null || !extras.containsKey(Const.Strings.PLID)) {
            // Should never happen. If it does, redirect the user to the groups
            // list.
            startActivity(new Intent(this, SoloPlaylistsActivity.class));
            finish();
        } else {
            // mGroupId = extras.getLong(Const.Strings.GID);
            // Log.i(TAG, "joined group " + mGroupId);
            if (extras.containsKey(Const.Strings.TITLE)) {
                setTitle(extras.getString(Const.Strings.TITLE));
            }
        }
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        setToForeground(true);
//        getHandler().postDelayed(getUpdater(), getInitialDelay());
//    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        mIsForeground = false;
//    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        unregisterReceiver(mLogoutReceiver);
//    }

//    public void repaintRefresh(boolean isRefreshing) {
//        if (mMenu == null) {
//            return;
//        }
//
//        MenuItem refreshItem = mMenu.findItem(R.id.menu_item_refresh);
//        if (refreshItem != null) {
//            if (isRefreshing) {
//                LayoutInflater inflater = (LayoutInflater) getSupportActionBar()
//                        .getThemedContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//                View refreshView = inflater.inflate(
//                        R.layout.actionbar_indeterminate_progress, null);
//                refreshItem.setActionView(refreshView);
//            } else {
//                refreshItem.setActionView(null);
//            }
//        }
//    }

    /*
     * Could be refactorized (non-Javadoc)
     * @see ch.epfl.unison.ui.UnisonMenu.OnRefreshListener#onRefresh()
     */
    @Override
    public void onRefresh() {
        repaintRefresh(true);
        UnisonAPI api = AppData.getInstance(this).getAPI();

        //TODO
        
    }

    /*
     * Could be refactorized (non-Javadoc)
     * @see
     * com.actionbarsherlock.app.SherlockFragmentActivity#onCreateOptionsMenu
     * (android.view.Menu)
     */
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        mMenu = menu;
//        return UnisonMenu.onCreateOptionsMenu(this, menu);
//    }

    /*
     * Could be refactorized (non-Javadoc)
     * @see
     * com.actionbarsherlock.app.SherlockFragmentActivity#onOptionsItemSelected
     * (android.view.MenuItem)
     */
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        return UnisonMenu.onOptionsItemSelected(this, this, item);
//    }

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
    public interface OnPlaylistInfoListener {
        void onPlaylistInfo(JsonStruct.PlaylistJS playlistInfo);
    }

    public void dispatchPlaylistInfo(JsonStruct.PlaylistJS playlistInfo) {
        for (OnPlaylistInfoListener listener : mListeners) {
            listener.onPlaylistInfo(playlistInfo);
        }
    }

    public void registerPlaylistInfoListener(OnPlaylistInfoListener listener) {
        mListeners.add(listener);
    }

    public void unregisterPlaylistInfoListener(OnPlaylistInfoListener listener) {
        mListeners.remove(listener);
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
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
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
