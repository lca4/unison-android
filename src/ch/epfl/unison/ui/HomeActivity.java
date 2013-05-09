
package ch.epfl.unison.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import ch.epfl.unison.R;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
 * TODO
 * 
 * - retrieve playlists stored in android database
 * - store playlist to android database 
 * 
 */

/**
 * Listing of the groups.
 * 
 * @author marc bourqui
 */
public class HomeActivity extends SherlockFragmentActivity
        implements UnisonMenu.OnRefreshListener {

    private static final String TAG = "ch.epfl.unison.HomelistsActivity";
    private static final int RELOAD_INTERVAL = 120 * 1000; // in ms.
    private static final int INITIAL_DELAY = 500; // in ms.

    // public static final String ACTION_LEAVE_GROUP =
    // "ch.epfl.unison.action.LEAVE_GROUP";

    // GUI specific
    private ListView mActivitiesList;
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

    /*
     * Coulde be refactorized
     */
    private BroadcastReceiver mLogoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.home);

      final ListView listview = (ListView) findViewById(R.id.home_activities_list);
      String[] values = new String[] { "Groups", "Solo" };

      final ArrayList<String> list = new ArrayList<String>();
      for (int i = 0; i < values.length; ++i) {
        list.add(values[i]);
      }
      final StableArrayAdapter adapter = new StableArrayAdapter(this,
          android.R.layout.simple_list_item_1, list);
      listview.setAdapter(adapter);

      listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, final View view,
            int position, long id) {
          final String item = (String) parent.getItemAtPosition(position);
          if (position == 0) {
              startActivity(new Intent(HomeActivity.this, GroupsActivity.class));
          } else if (position == 1) {
              startActivity(new Intent(HomeActivity.this, SoloPlaylistsActivity.class));
          }
        }

      });
    }

    /**
     * 
     * @author marc
     *
     */
    private class StableArrayAdapter extends ArrayAdapter<String> {

      private HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

      public StableArrayAdapter(Context context, int textViewResourceId,
          List<String> objects) {
        super(context, textViewResourceId, objects);
        for (int i = 0; i < objects.size(); ++i) {
          mIdMap.put(objects.get(i), i);
        }
      }

      @Override
      public long getItemId(int position) {
        String item = getItem(position);
        return mIdMap.get(item);
      }

      @Override
      public boolean hasStableIds() {
        return true;
      }

    }

    @Override
    public void onRefresh() {
        // TODO Auto-generated method stub
        
    }

}