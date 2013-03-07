package ch.epfl.unison.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.Preference;
import android.util.Log;
import android.widget.Toast;
import ch.epfl.unison.AppData;
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.PreferenceKeys;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class PrefsActivity extends SherlockPreferenceActivity {

    private static final String TAG = "ch.epfl.unison.PrefsActivity";

    private BroadcastReceiver logoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This activity should finish on logout.
        this.registerReceiver(this.logoutReceiver,
                new IntentFilter(UnisonMenu.ACTION_LOGOUT));

        this.setTitle(R.string.activity_title_prefs);
        this.addPreferencesFromResource(R.xml.prefs);

        this.findPreference(PreferenceKeys.NICKNAME_KEY).setOnPreferenceChangeListener(
                new NicknameChangeListener());

        this.findPreference(PreferenceKeys.UID_KEY).setSummary(
                String.format("your UID is: %d", AppData.getInstance(this).getUid()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(this.logoutReceiver);
    }

    private class NicknameChangeListener implements Preference.OnPreferenceChangeListener {

        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final String newNick = newValue.toString();
            AppData data = AppData.getInstance(PrefsActivity.this);
            data.getAPI().setNickname(data.getUid(), newNick,
                    new UnisonAPI.Handler<JsonStruct.Success>() {

                public void callback(JsonStruct.Success struct) {
                    Log.i(TAG, String.format("changed nickname to %s", newNick));
                }

                public void onError(Error error) {
                    Log.w(TAG, String.format("couldn't set new nickname %s", newNick));
                    Log.d(TAG, error.toString());
                    if (PrefsActivity.this != null) {
                        Toast.makeText(PrefsActivity.this, R.string.error_updating_nick,
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
            return true;
        }

    }


}
