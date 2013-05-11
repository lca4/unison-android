
package ch.epfl.unison.ui;

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
 * @see UnisonMainActivity
 * 
 * @author lum
 */
public class SoloMainActivity extends UnisonMainActivity {

    private static final String TAG = "ch.epfl.unison.SoloMainActivity";
    private static final int RELOAD_INTERVAL = 30 * 1000; // in ms.

    private TabsAdapter mTabsAdapter;
    private ViewPager mViewPager;


    private Set<OnPlaylistInfoListener> mListeners = new HashSet<OnPlaylistInfoListener>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setReloadInterval(RELOAD_INTERVAL);
        setTag(TAG);
        mTabsAdapter.addTab(getSupportActBar().newTab().setText(R.string.solo_player_fragment_title),
                SoloPlayerFragment.class, null);
        mTabsAdapter.addTab(getSupportActBar().newTab().setText(R.string.solo_playlist_fragment_title),
                SoloTracksFragment.class, null);
    }

    protected void handleExtras(Bundle extras) {
        if (extras == null || !extras.containsKey(Const.Strings.PLID)) {
            // Should never happen. If it does, redirect the user to the groups
            // list.
            startActivity(new Intent(this, SoloPlaylistsActivity.class));
            finish();
        } else {
            if (extras.containsKey(Const.Strings.TITLE)) {
                setTitle(extras.getString(Const.Strings.TITLE));
            }
        }
    }

    @Override
    public void onRefresh() {
        super.onRefresh();
        UnisonAPI api = AppData.getInstance(this).getAPI();

        //TODO
        
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
