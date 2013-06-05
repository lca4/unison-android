
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
import ch.epfl.unison.Const;
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

/**
 * Activity to manage the application's settings (i.e. preferences).
 * 
 * @author lum
 */
public class PrefsActivity extends SherlockPreferenceActivity {

    private static final String TAG = "ch.epfl.unison.PrefsActivity";

    private BroadcastReceiver mLogoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This activity should finish on logout.
        registerReceiver(this.mLogoutReceiver,
                new IntentFilter(AbstractMenu.ACTION_LOGOUT));

        setTitle(R.string.activity_title_prefs);
        addPreferencesFromResource(R.xml.prefs);

        findPreference(Const.PrefKeys.NICKNAME).setOnPreferenceChangeListener(
                new NicknameChangeListener());

        findPreference(Const.PrefKeys.UID).setSummary(
                String.valueOf(AppData.getInstance(this).getUid()));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mLogoutReceiver);
    }

    /** Update information on the back-end when nickname is changed. */
    private class NicknameChangeListener implements Preference.OnPreferenceChangeListener {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final String newNick = newValue.toString();
            AppData data = AppData.getInstance(PrefsActivity.this);
            data.getAPI().setNickname(data.getUid(), newNick,
                    new UnisonAPI.Handler<JsonStruct.Success>() {

                        @Override
                        public void callback(JsonStruct.Success struct) {
                            Log.i(TAG, String.format("changed nickname to %s", newNick));
                        }

                        @Override
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
