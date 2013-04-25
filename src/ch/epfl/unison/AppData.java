package ch.epfl.unison;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.UnisonAPI;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Singleton object containing various utilities for the app.
 *
 * @author lum
 */
public final class AppData implements OnSharedPreferenceChangeListener {

    private static final String TAG = "ch.epfl.unison.AppData";
//    private static final int LOCATION_INTERVAL = 20 * 60 * 1000;  // In ms.
    private static final int LOCATION_INTERVAL = 30 * 1000;  // In ms. For testing purpose, TODO: set multiple intervals
    private static final int LOCATION_EXPIRATION_TIME = 10 * 60 * 1000;  // In ms.
    private static final int MAX_LOCATION_HISTORY_SIZE = 3;
    private static final int MAX_HISTORY_SIZE = 10;

    private static AppData sInstance;

    private Context mContext;
    private UnisonAPI mApi;
    private SharedPreferences mPrefs;
    private Type groupHistoryMapType = new TypeToken<Map<Long, Pair<JsonStruct.Group, Date>>>() { } .getType();
    private Type locationHistoryQueueType = new TypeToken<LinkedList<Pair<Location, Date>>>() { } .getType();
    //TODO Use Pair<Double, Double> instead of Position 

    private LocationManager mLocationMgr;
    private UnisonLocationListener mGpsListener;
    private Location mGpsLocation;
    private UnisonLocationListener mNetworkListener;
    private Location mNetworkLocation;

    private AppData(Context context) {
        mContext = context;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    private AppData setupLocation() {
        if (mLocationMgr == null) {
            mLocationMgr = (LocationManager) mContext.getSystemService(
                    Context.LOCATION_SERVICE);
        }
        // try to set up the network location listener.
        if (mNetworkListener == null
                && mLocationMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            mNetworkListener = new UnisonLocationListener(LocationManager.NETWORK_PROVIDER);
            mLocationMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    LOCATION_INTERVAL, 1f, mNetworkListener);
            mNetworkLocation = mLocationMgr.getLastKnownLocation(
                    LocationManager.NETWORK_PROVIDER);
        }
        // try to set up the GPS location listener.
        if (mGpsListener == null
                && mLocationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mGpsListener = new UnisonLocationListener(LocationManager.GPS_PROVIDER);
            mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    LOCATION_INTERVAL, 1f, mGpsListener);
            mGpsLocation = mLocationMgr.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER);
        }
        return this;
    }

    public UnisonAPI getAPI() {
        if (mApi == null) {
            String email = mPrefs.getString(Const.PrefKeys.EMAIL, null);
            String password = mPrefs.getString(Const.PrefKeys.PASSWORD, null);
            if (email != null && password != null) {
                mApi = new UnisonAPI(email, password);
            } else {
                mApi = new UnisonAPI();
            }
        }
        return mApi;
    }

    public long getUid() {
        return mPrefs.getLong(Const.PrefKeys.UID, -1);
    }

    public boolean showHelpDialog() {
        return mPrefs.getBoolean(Const.PrefKeys.HELPDIALOG, true);
    }

    public void setShowHelpDialog(boolean value) {
        mPrefs.edit().putBoolean(Const.PrefKeys.HELPDIALOG, value).commit();
    }
    
    public boolean showGroupSuggestion() {
        return mPrefs.getBoolean(Const.PrefKeys.GROUP_SUGGESTION, true);
    }
    
    public void setShowGroupSuggestion(boolean value) {
        mPrefs.edit().putBoolean(Const.PrefKeys.GROUP_SUGGESTION, value).commit();
    }

    public Location getLocation() {
      //TODO Use Pair<Double, Double> instead of Position
        String value = mPrefs.getString(Const.PrefKeys.LOCATION_HISTORY, null);
        LinkedList<Pair<Location, Date>> positionHistoryQueue;
        
        if(value == null) {
            positionHistoryQueue = new LinkedList<Pair<Location,Date>>(); // We use the linkedList as a Queue.
        } else {
            Log.d(TAG, value);
            positionHistoryQueue = new GsonBuilder().create().fromJson(value, locationHistoryQueueType);
        }
        
        Location lastLocation;
        Date lastLocationTimestamp;
        if (mGpsLocation != null) {
            lastLocation = mGpsLocation;  // Prefer GPS locations over network locations.
        } else {
            lastLocation = mNetworkLocation;            
        }
        lastLocationTimestamp = new Date();
        
        while(!positionHistoryQueue.isEmpty()){
            //Clear location history according to threshold
            if(lastLocationTimestamp.getTime() - positionHistoryQueue.peek().second.getTime() > LOCATION_EXPIRATION_TIME) {
                positionHistoryQueue.remove();
            } else {
                break;
            }
        }
        
        if(positionHistoryQueue.size() >= MAX_LOCATION_HISTORY_SIZE) {
            positionHistoryQueue.remove();
        }
        positionHistoryQueue.offer(new Pair<Location, Date>(lastLocation, lastLocationTimestamp));
        
        double meanLat = 0;
        double meanLon = 0;
        
        for(Pair<Location,Date> p : positionHistoryQueue) {
            meanLat += p.first.getLatitude();
            meanLon += p.first.getLongitude();
        }
        meanLat /= (double)(positionHistoryQueue.size());
        meanLon /= (double)(positionHistoryQueue.size());
        
        Location meanLocation = new Location(lastLocation); //we must copy an old location...
        meanLocation.setLatitude(meanLat);
        meanLocation.setLongitude(meanLon);
        
        
        value = new GsonBuilder().create().toJson(positionHistoryQueue, locationHistoryQueueType);
        if (value != null) {
            mPrefs.edit().putString(Const.PrefKeys.LOCATION_HISTORY, value).commit();
            Log.d(TAG, "location added in Queue!");
        }
        
        return meanLocation;
    }

    public static synchronized AppData getInstance(Context c) {
        if (sInstance == null) {
            sInstance = new AppData(c.getApplicationContext());
        }
        return sInstance.setupLocation();
    }

    @Override
    public synchronized void onSharedPreferenceChanged(
            SharedPreferences sharedPreferences, String key) {
        if (key.equals(Const.PrefKeys.EMAIL)
                || key.equals(Const.PrefKeys.PASSWORD)
                || key.equals(Const.PrefKeys.UID)) {
            mApi = null;
        }
    }
    
    public boolean addToHistory(JsonStruct.Group group) {
    	if (group == null) {
    		return false;
    	}
    	
        Map<Long, Pair<JsonStruct.Group, Date>> history = this.getHistory();
        if (history == null) {
            history = new HashMap<Long, Pair<JsonStruct.Group, Date>>();
        }       

        //make sure the Map doesn't become too big:
        truncateHistory(history);
        //adds new entry
        history.put(Long.valueOf(group.gid), new Pair<JsonStruct.Group, Date>(group, new Date()));
        
        //Possible optimization heres
        String value = new GsonBuilder().create().toJson(history, groupHistoryMapType);
        return mPrefs.edit().putString(Const.PrefKeys.HISTORY, value).commit();
    }
    
    public Map<Long, Pair<JsonStruct.Group, Date>> getHistory() {
        String value = mPrefs.getString(Const.PrefKeys.HISTORY, null);
        
        if (value == null) {
            return null;
        }
        return new GsonBuilder().create().fromJson(value, groupHistoryMapType);
    }
    
    /**
     * This method checks whether the history has become too long and,
     * if so, deletes the oldest entries. It should be called in the
     * addToHistory method so that it truncates the history map "in place" before
     * it is being stored in the shared prefs.
     * It can handle a way too large history in case of bug, but it should usually
     * only remove one entry at a time.  
     */
    private boolean truncateHistory(Map<Long, Pair<JsonStruct.Group, Date>> history) {
        
        if (history == null) {
            return false;
        }

        int historySize = history.size();
        
        if (historySize <= MAX_HISTORY_SIZE - 1) {
            return false;
        } 
        
        //TODO code duplicated from groupHistoryActivity.
        List<Pair<JsonStruct.Group, Date>> listOfGroups =
                new ArrayList<Pair<JsonStruct.Group, Date>>(
                        history.values());
        Collections.sort(listOfGroups,
                new Comparator<Pair<JsonStruct.Group, Date>>() {
            public int compare(Pair<JsonStruct.Group, Date> o1,
                    Pair<JsonStruct.Group, Date> o2) {
                return -o1.second.compareTo(o2.second);
            }
        });
        
       for (int i = 1; i <= historySize - (MAX_HISTORY_SIZE - 1); i++) {
            history.remove(listOfGroups.get(historySize - i).first.gid);
        }
       return true;
    }
    
    
    public boolean clearHistory() {
        return mPrefs.edit().remove(Const.PrefKeys.HISTORY).commit();
    }

    /**
     * Simple LocationListener that differentiates updates from the
     * network provider and those from the GPS provider.
     *
     * @author lum
     */
    public class UnisonLocationListener implements LocationListener {

        private String mProvider;

        public UnisonLocationListener(String provider) {
            mProvider = provider;
        }

        @Override
        public void onLocationChanged(Location location) {
            if (LocationManager.GPS_PROVIDER.equals(mProvider)) {
                AppData.this.mGpsLocation = location;
            } else if (LocationManager.NETWORK_PROVIDER.equals(mProvider)) {
                AppData.this.mNetworkLocation = location;
            } else {
                throw new RuntimeException("unsupported location provider");
            }
            Log.i(TAG, String.format("Got location (%s): lat=%f, lon=%f",
                    mProvider, location.getLatitude(), location.getLongitude()));
        }

        @Override
        public void onProviderDisabled(String provider) { }
        @Override
        public void onProviderEnabled(String provider) { }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) { }
    }
}
