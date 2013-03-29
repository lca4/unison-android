package ch.epfl.unison;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

/**
 * Singleton object containing various utilities for the app.
 *
 * @author lum
 */
public final class AppData implements OnSharedPreferenceChangeListener {

    private static final String TAG = "ch.epfl.unison.AppData";
    private static final int LOCATION_INTERVAL = 20 * 60 * 1000;  // In ms.

    private static AppData sInstance;

    private Context mContext;
    private UnisonAPI mApi;
    private SharedPreferences mPrefs;

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

    public Location getLocation() {
        if (mGpsLocation != null) {
            return mGpsLocation;  // Prefer GPS locations over network locations.
        }
        return mNetworkLocation;
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
        

        history.put(Long.valueOf(group.gid), new Pair<JsonStruct.Group, Date>(group, new Date()));
        
        //Possible optimization here
              
        String value = new GsonBuilder().create().toJson(history);
        
        return mPrefs.edit().putString(Const.PrefKeys.HISTORY, value).commit();
    }
    
    @SuppressWarnings("unchecked")
    public Map<Long, Pair<JsonStruct.Group, Date>> getHistory() {
        String value = mPrefs.getString(Const.PrefKeys.HISTORY, null);
        
        if (value == null) {
            return null;
        }

        return new GsonBuilder().create().fromJson(value, Map.class);
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
