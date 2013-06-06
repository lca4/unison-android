
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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
import ch.epfl.unison.ui.GroupsActivity;
import ch.epfl.unison.ui.GroupsMainActivity;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Singleton object containing various utilities for the app.
 * 
 * @author lum
 */
public final class AppData implements OnSharedPreferenceChangeListener {

    private static final String TAG = "ch.epfl.unison.AppData";
    // private static final int LOCATION_INTERVAL = 20 * 60 * 1000; // In ms.
    // For testing purpose, TODO: set multiple intervals
    // private static final int LOCATION_INTERVAL = 30 * 1000; // In ms.
    private static final int LOCATION_EXPIRATION_TIME = 10 * 60 * 1000; // In
                                                                        // ms.
    private static final int MAX_LOCATION_HISTORY_SIZE = 3;
    private static final int MAX_HISTORY_SIZE = 10;

    private static final int SLOW = 20 * 60 * 1000;
    private static final int MEDIUM = 60 * 1000;
    private static final int FAST = 10 * 1000;

    private static final int GROUP_PASSWORD_LENGTH = 4;

    private static int sCurrentSpeed = SLOW;

    private static AppData sInstance;

    private Context mContext;
    private UnisonAPI mApi;
    private SharedPreferences mPrefs;
    private Type mGroupHistoryMapType = new
            TypeToken<Map<Long, Pair<JsonStruct.Group, Date>>>() {
            }.getType();
    private Type mLocationHistoryQueueType = new
            TypeToken<LinkedList<Pair<Pair<Double, Double>, Date>>>() {
            }.getType();

    private LocationManager mLocationMgr;
    private UnisonLocationListener mGpsListener;
    private Location mGpsLocation;
    private UnisonLocationListener mNetworkListener;
    private Location mNetworkLocation;

    private AppData(Context context) {
        mContext = context;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        // Here the shared prefs are explicitely set to mode private.
        // mPrefs = context.getSharedPreferences(context.getPackageName()+".sp",
        // Context.MODE_PRIVATE);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    private AppData setupLocation() {
        Log.d(TAG, "Calling SetupLocation()");
        if (mLocationMgr == null) {
            mLocationMgr = (LocationManager) mContext.getSystemService(
                    Context.LOCATION_SERVICE);
        }
        // try to set up the network location listener.
        if (mNetworkListener == null
                && mLocationMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Log.d(TAG, "NetworkListener was null");
            mNetworkListener = new UnisonLocationListener(LocationManager.NETWORK_PROVIDER);
            mLocationMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    sCurrentSpeed, 1f, mNetworkListener);
            mNetworkLocation = mLocationMgr.getLastKnownLocation(
                    LocationManager.NETWORK_PROVIDER);
        }
        // try to set up the GPS location listener.
        if (mGpsListener == null
                && mLocationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d(TAG, "GpsListener was null");
            mGpsListener = new UnisonLocationListener(LocationManager.GPS_PROVIDER);
            mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    sCurrentSpeed, 1f, mGpsListener);
            mGpsLocation = mLocationMgr.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER);
        }

        if (mLocationMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            mLocationMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    sCurrentSpeed, 1f, mNetworkListener);
        }
        if (mLocationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    sCurrentSpeed, 1f, mGpsListener);
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

    private LinkedList<Pair<Pair<Double, Double>, Date>> extractQueueFromString(String value) {
        if (value == null) {
            return new LinkedList<Pair<Pair<Double, Double>, Date>>();
        } else {
            return new GsonBuilder().create().fromJson(value, mLocationHistoryQueueType);
        }
    }

    private LinkedList<Pair<Pair<Double, Double>, Date>> removeOutdatedLocations(
            LinkedList<Pair<Pair<Double, Double>, Date>> queue, double currentTime)
    {
        int n = 0;
        while (!queue.isEmpty() && n < MAX_LOCATION_HISTORY_SIZE) {
            // Clear location history according to threshold
            if (currentTime - queue.peek().second.getTime()
            > LOCATION_EXPIRATION_TIME) {
                queue.remove();
            }
            ++n;
        }

        return queue;
    }

    public Location getLocation() {
        String value = mPrefs.getString(Const.PrefKeys.LOCATION_HISTORY, null);
        LinkedList<Pair<Pair<Double, Double>, Date>> positionHistoryQueue =
                extractQueueFromString(value);
        Location lastLocation = chooseLocationOrigin();
        // This means that we never had a location...
        if (lastLocation == null) {
            return null;
        }
        Date lastLocationTimestamp = new Date();
        positionHistoryQueue =
                removeOutdatedLocations(positionHistoryQueue, lastLocationTimestamp.getTime());
        if (positionHistoryQueue.size() >= MAX_LOCATION_HISTORY_SIZE) {
            positionHistoryQueue.remove();
        }
        positionHistoryQueue.offer(new Pair<Pair<Double, Double>, Date>(
                new Pair<Double, Double>(lastLocation.getLatitude(), lastLocation.getLongitude()),
                lastLocationTimestamp));

        double meanLat = 0.0, meanLon = 0.0;

        for (Pair<Pair<Double, Double>, Date> p : positionHistoryQueue) {
            meanLat += p.first.first;
            meanLon += p.first.second;
        }
        meanLat /= (double) (positionHistoryQueue.size());
        meanLon /= (double) (positionHistoryQueue.size());

        Location meanLocation = new Location(lastLocation); // we must copy an
                                                            // old location...
        meanLocation.setLatitude(meanLat);
        meanLocation.setLongitude(meanLon);

        value = new GsonBuilder().create().toJson(positionHistoryQueue, mLocationHistoryQueueType);
        if (value != null) {
            mPrefs.edit().putString(Const.PrefKeys.LOCATION_HISTORY, value).commit();
            Log.d(TAG, "location added in Queue!");
        }

        return meanLocation;
    }

    private Location chooseLocationOrigin() {
        if (mGpsLocation != null) {
            return mGpsLocation;
        } else {
            return mNetworkLocation;
        }
    }

    public static synchronized AppData getInstance(Context c) {
        if (sInstance == null) {
            sInstance = new AppData(c.getApplicationContext());
        }
        if (c instanceof GroupsActivity) {
            sCurrentSpeed = FAST;
        } else if (c instanceof GroupsMainActivity) {
            sCurrentSpeed = MEDIUM;
        } else {
            sCurrentSpeed = SLOW;
        }
        Log.d(TAG, "location refresh speed = " + sCurrentSpeed);
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

        // make sure the Map doesn't become too big:
        truncateHistory(history);
        // adds new entry
        history.put(Long.valueOf(group.gid), new Pair<JsonStruct.Group, Date>(group, new Date()));

        // Possible optimization heres
        String value = new GsonBuilder().create().toJson(history, mGroupHistoryMapType);
        return mPrefs.edit().putString(Const.PrefKeys.HISTORY, value).commit();
    }

    public Map<Long, Pair<JsonStruct.Group, Date>> getHistory() {
        String value = mPrefs.getString(Const.PrefKeys.HISTORY, null);

        if (value == null) {
            return null;
        }
        return new GsonBuilder().create().fromJson(value, mGroupHistoryMapType);
    }

    public int getGroupPasswordLength() {
        return GROUP_PASSWORD_LENGTH;
    }

    /**
     * This method checks whether the history has become too long and, if so,
     * deletes the oldest entries. It should be called in the addToHistory
     * method so that it truncates the history map "in place" before it is being
     * stored in the shared prefs. It can handle a way too large history in case
     * of bug, but it should usually only remove one entry at a time.
     */
    private boolean truncateHistory(Map<Long, Pair<JsonStruct.Group, Date>> history) {

        if (history == null) {
            return false;
        }

        int historySize = history.size();

        if (historySize <= MAX_HISTORY_SIZE - 1) {
            return false;
        }

        // TODO code duplicated from groupHistoryActivity.
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

    public boolean removeOneHistoryItem(Long gid) {
        Map<Long, Pair<JsonStruct.Group, Date>> history = getHistory();

        history.remove(gid);

        // Possible optimization heres
        String value = new GsonBuilder().create().toJson(history, mGroupHistoryMapType);
        return mPrefs.edit().putString(Const.PrefKeys.HISTORY, value).commit();
    }

    public boolean setLoggedIn(boolean loggedIn) {
        return mPrefs.edit().putBoolean(Const.PrefKeys.LOGGED_IN, loggedIn).commit();
    }

    public boolean getLoggedIn() {
        return mPrefs.getBoolean(Const.PrefKeys.LOGGED_IN, false);
    }

    public boolean setInGroup(boolean inGroup) {
        return mPrefs.edit().putBoolean(Const.PrefKeys.IN_GROUP, inGroup).commit();
    }

    public boolean getInGroup() {
        return mPrefs.getBoolean(Const.PrefKeys.IN_GROUP, false);
    }

    public boolean setInSolo(boolean solo) {
        return mPrefs.edit().putBoolean(Const.PrefKeys.IN_SOLO, solo).commit();
    }

    public boolean getInSolo() {
        return mPrefs.getBoolean(Const.PrefKeys.IN_SOLO, false);
    }

    public boolean setCurrentGID(Long gid) {
        return mPrefs.edit().putLong(Const.PrefKeys.CURRENT_GID, gid).commit();
    }

    public Long getCurrentGID() {
        return mPrefs.getLong(Const.PrefKeys.CURRENT_GID, -1);
    }

    public void deleteEmailAndPassword() {
        Editor editor = mPrefs.edit();
        editor.remove(Const.PrefKeys.EMAIL);
        editor.remove(Const.PrefKeys.PASSWORD);
        editor.remove(Const.PrefKeys.UID);
        editor.remove(Const.PrefKeys.LASTUPDATE);
        editor.commit();
    }

    public void storeEmail(String email) {
        Editor editor = mPrefs.edit();
        editor.putString(Const.PrefKeys.EMAIL, email);
        editor.commit();
    }

    public void storePassword(String password) {
        Editor editor = mPrefs.edit();
        editor.putString(Const.PrefKeys.PASSWORD, password);
        editor.commit();
    }

    public void deleteUID() {
        Editor editor = mPrefs.edit();
        editor.remove(Const.PrefKeys.UID);
        editor.commit();
    }

    public void deleteLastUpdate() {
        Editor editor = mPrefs.edit();
        editor.remove(Const.PrefKeys.LASTUPDATE);
        editor.commit();
    }

    public void storeNickname(String nickname) {
        Editor editor = mPrefs.edit();
        editor.putString(Const.PrefKeys.NICKNAME, nickname);
        editor.commit();
    }
    
    public String getNickname() {
        return mPrefs.getString(Const.PrefKeys.NICKNAME, "user" + getUid());
    }

    public void storeUID(long uid) {
        Editor editor = mPrefs.edit();
        editor.putLong(Const.PrefKeys.UID, uid);
        editor.commit();
    }

    public String getEmail() {
        return mPrefs.getString(Const.PrefKeys.EMAIL, null);
    }

    public String getPassword() {
        return mPrefs.getString(Const.PrefKeys.PASSWORD, null);
    }

    /**
     * Simple LocationListener that differentiates updates from the network
     * provider and those from the GPS provider.
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
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }
}
