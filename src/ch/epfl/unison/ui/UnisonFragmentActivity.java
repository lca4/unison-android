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
 * 
 * @author marc
 *
 */
public class UnisonFragmentActivity extends SherlockFragmentActivity implements OnRefreshListener {
    
    private static final String TAG = "ch.epfl.unison.UnisonFragmentActivity";
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
            Log.d(TAG, "repaintRefresh: mMenu is null");
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
            Log.d(TAG, "repaintRefresh: menu_item_refresh not found");
        }
    }
    
    @Override
    public void onRefresh() {
        // TODO Auto-generated method stub
        repaintRefresh(true);
    }

    public boolean isForeground() {
        return mIsForeground;
    }

    public void setToForeground(boolean isForeground) {
        this.mIsForeground = isForeground;
    }

    public BroadcastReceiver getLogoutReceiver() {
        return mLogoutReceiver;
    }

//    public static int getReloadInterval() {
//        return RELOAD_INTERVAL;
//    }

    public Handler getHandler() {
        return mHandler;
    }

    public static int getInitialDelay() {
        return INITIAL_DELAY;
    }

    public static int getReloadInterval() {
        return smReloadInterval;
    }

    public static void setReloadInterval(int reloadInterval) {
        UnisonFragmentActivity.smReloadInterval = reloadInterval;
    }

    public Runnable getUpdater() {
        return mUpdater;
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
