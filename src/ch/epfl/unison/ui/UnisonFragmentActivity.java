package ch.epfl.unison.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import ch.epfl.unison.R;
import ch.epfl.unison.ui.UnisonMenu.OnRefreshListener;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * Provides some default behaviors.
 * Not supposed to be instantiated directly, but to be extended.
 * 
 * @author marc
 *
 */
public class UnisonFragmentActivity extends SherlockFragmentActivity implements OnRefreshListener {
    
    private static String smTag = "ch.epfl.unison.UnisonFragmentActivity";
    private static int smReloadInterval;
    private boolean mIsForeground = false;
    private Menu mMenu;
//    private static final int RELOAD_INTERVAL = 120 * 1000; // in ms.
    private static final int INITIAL_DELAY = 500; // in ms.
    
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
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        return UnisonMenu.onCreateOptionsMenu(this, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return UnisonMenu.onOptionsItemSelected(this, this, item);
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
                        .getThemedContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
     * Default implementation.
     * Should be overridden to suitable behavior.
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

//    public static int getReloadInterval() {
//        return RELOAD_INTERVAL;
//    }

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

//    public void setUpdater(Runnable updater) {
//        this.mUpdater = updater;
//    }

//    public void setHandler(Handler mHandler) {
//        this.mHandler = mHandler;
//    }

//    public void setLogoutReceiver(BroadcastReceiver mLogoutReceiver) {
//        this.mLogoutReceiver = mLogoutReceiver;
//    }

}
