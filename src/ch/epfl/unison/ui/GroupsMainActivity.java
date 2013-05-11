package ch.epfl.unison.ui;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

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
 * Activity that is displayed once you're inside the group. Displays the music player
 * and information about the group (through fragments).
 *
 * @author lum
 */
public class GroupsMainActivity extends UnisonFragmentActivity {

    private static final String TAG = "ch.epfl.unison.MainActivity";
    private static final int RELOAD_INTERVAL = 30 * 1000;  // in ms.
    //Distance threshold between the user and the automatic group in meters.
    private static final double MAX_DISTANCE = 2000;
    
    private TabsAdapter mTabsAdapter;
    private ViewPager mViewPager;

    private Set<OnGroupInfoListener> mListeners = new HashSet<OnGroupInfoListener>();

    private JsonStruct.Group mGroup;
    
    private boolean mIsDj = false;

    public long getGroupId() {
        return mGroup.gid;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setReloadInterval(RELOAD_INTERVAL);
        setTag(TAG);

//        // This activity should finish on logout.
//        registerReceiver(getLogoutReceiver(), new IntentFilter(UnisonMenu.ACTION_LOGOUT));
//
//        // Set up the tabs & stuff.
//        mViewPager = new ViewPager(this);
//        mViewPager.setId(R.id.realtabcontent); // TODO change
//        setContentView(mViewPager);
//
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//
//        ActionBar bar = getSupportActionBar();
//        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
//        mTabsAdapter = new TabsAdapter(this, mViewPager);
        mTabsAdapter.addTab(getSupportActBar().newTab().setText(R.string.fragment_title_player),
                GroupsPlayerFragment.class, null);
        mTabsAdapter.addTab(getSupportActBar().newTab().setText(R.string.fragment_title_stats),
                GroupsStatsFragment.class, null);
    }

    protected void handleExtras(Bundle extras) {
        if (extras == null || !extras.containsKey(Const.Strings.GROUP)) {
            // Should never happen. If it does, redirect the user to the groups list.
            Log.d(TAG, "Tried creating mainActivity"
                    + " without coming from groupsActivity! Going to close");
            startActivity(new Intent(this, GroupsActivity.class));
            finish();
        } else {
        	mGroup = (JsonStruct.Group) extras.get(Const.Strings.GROUP);
            Log.i(TAG, "joined group " + getGroupId());
            
            setTitle(mGroup.name);
            AppData.getInstance(this).addToHistory(mGroup);
             
        }
    }
    
    private void autoLeave() {
        AppData data = AppData.getInstance(this);
        Location currentLoc = data.getLocation();
        if (currentLoc != null 
                && mGroup.lat != null 
                && mGroup.lon != null 
                && mGroup.automatic) {
            double lat = currentLoc.getLatitude();
            double lon = currentLoc.getLongitude();
            float[] res = new float[1];
            
            Location.distanceBetween(mGroup.lat, mGroup.lon, lat, lon, res);
            double dist =  res[0];
            
            if (dist > MAX_DISTANCE) {
                onKeyDown(KeyEvent.KEYCODE_BACK, null);
            }
        }
    }

    @Override
    public void onRefresh() {
    	super.onRefresh();
        UnisonAPI api = AppData.getInstance(this).getAPI();
        
        autoLeave();
        
        api.getGroupInfo(getGroupId(), new UnisonAPI.Handler<JsonStruct.Group>() {

            @Override
            public void callback(JsonStruct.Group struct) {
                try {
                    GroupsMainActivity.this.onGroupInfo(struct);
                    GroupsMainActivity.this.dispatchGroupInfo(struct);
                    GroupsMainActivity.this.repaintRefresh(false);
                } catch (NullPointerException e) {
                    Log.w(TAG, "group or activity is null?", e);
                }
            }

            @Override
            public void onError(UnisonAPI.Error error) {
                Log.d(TAG, error.toString());
                if (GroupsMainActivity.this != null) {
                    Toast.makeText(GroupsMainActivity.this, R.string.error_loading_info,
                            Toast.LENGTH_LONG).show();
                    GroupsMainActivity.this.repaintRefresh(false);
                }
            }

        });
    }

    private void onGroupInfo(JsonStruct.Group group) {
        setTitle(group.name);
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
    
    public void setDJ(boolean dj) {
        mIsDj = dj;
        getMenu().findItem(R.id.menu_item_manage_group).setVisible(dj);
    }
    
    public boolean getDJ() {
        return mIsDj;
    }
}
