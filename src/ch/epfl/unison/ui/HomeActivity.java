
package ch.epfl.unison.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import ch.epfl.unison.R;

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
 * Raw copy of GroupsActivity. Has to be properly cleaned up.
 * 
 * @author marc bourqui
 */
public class HomeActivity extends AbstractFragmentActivity {
/*
 * TODO remove ListView and replace with buttons
 */

    private static final String TAG = "ch.epfl.unison.HomelistsActivity";
    private static final int RELOAD_INTERVAL = 120 * 1000; // in ms.


    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        
        setReloadInterval(RELOAD_INTERVAL);

        final ListView listview = (ListView) findViewById(R.id.home_activities_list);
        String[] values = new String[] {
                "Groups", "Solo"
        };

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
                final String itemem = (String) parent.getItemAtPosition(position);
                if (position == 0) {
                    startActivity(new Intent(HomeActivity.this, GroupsActivity.class));
                } else if (position == 1) {
                    startActivity(new Intent(HomeActivity.this, SoloPlaylistsActivity.class));
                }
            }

        });
    }

    /**
     * @author marc
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
    	
    }


}
