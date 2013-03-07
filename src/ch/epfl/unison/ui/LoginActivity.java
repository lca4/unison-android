package ch.epfl.unison.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import ch.epfl.unison.AppData;
import ch.epfl.unison.LibraryService;
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.PreferenceKeys;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;

import com.actionbarsherlock.app.SherlockActivity;

public class LoginActivity extends SherlockActivity {

    private static final String TAG = "ch.epfl.unison.LoginActivity";
    
    private Button loginBtn;
    private TextView signupTxt;

    private EditText email;
    private EditText password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.login);

        this.email = (EditText) findViewById(R.id.email);
        this.password = (EditText) findViewById(R.id.password);

        this.signupTxt = (TextView) findViewById(R.id.signupTxt);
        this.signupTxt.setText(Html.fromHtml(getString(R.string.login_signup_html)));
        this.signupTxt.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            }

        });

        this.loginBtn = (Button) findViewById(R.id.loginBtn);
        this.loginBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                final String email = LoginActivity.this.email.getText().toString();
                final String password = LoginActivity.this.password.getText().toString();
                LoginActivity.this.login(email, password);
            }
        });

        // Initialize the AppData instance.
        AppData.getInstance(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        Bundle extras = this.getIntent().getExtras();
        if (extras != null && extras.getBoolean(PreferenceKeys.LOGOUT_KEY)) {
            Log.d(TAG, "logging out");
            // Remove e-mail and password from the preferences.
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(PreferenceKeys.EMAIL_KEY);
            editor.remove(PreferenceKeys.PASSWORD_KEY);
            editor.remove(PreferenceKeys.UID_KEY);
            editor.remove(PreferenceKeys.LASTUPDATE_KEY);
            editor.commit();

            // Truncate the library.
            this.startService(new Intent(LibraryService.ACTION_TRUNCATE));

        } else if (extras != null) {
            // We're coming from the signup form (whether native or online).
            String email = extras.getString(PreferenceKeys.EMAIL_KEY);
            String password = extras.getString(PreferenceKeys.PASSWORD_KEY);
            if (email != null && password != null) {
                // Login information is in the intent.
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(PreferenceKeys.EMAIL_KEY, email);
                editor.putString(PreferenceKeys.PASSWORD_KEY, password);
                editor.remove(PreferenceKeys.UID_KEY);
                editor.remove(PreferenceKeys.LASTUPDATE_KEY);
                editor.commit();

                // Truncate the library. You never know.
                this.startService(new Intent(LibraryService.ACTION_TRUNCATE));
            }
        }

        // Try to login from the saved preferences.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String email = prefs.getString(PreferenceKeys.EMAIL_KEY, null);
        String password = prefs.getString(PreferenceKeys.PASSWORD_KEY, null);

        if (email != null && password != null) {
            this.login(email, password);
        }

        this.fillEmailPassword();
    }

    private void fillEmailPassword() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.email.setText(prefs.getString(PreferenceKeys.EMAIL_KEY, null));
        this.password.setText(prefs.getString(PreferenceKeys.PASSWORD_KEY, null));
    }

    private void storeInfo(String email, String password, String nickname, Long uid) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(PreferenceKeys.EMAIL_KEY, email);
        editor.putString(PreferenceKeys.PASSWORD_KEY, password);
        editor.putString(PreferenceKeys.NICKNAME_KEY, nickname);
        editor.putLong(PreferenceKeys.UID_KEY, uid != null ? uid : -1);
        editor.commit();
    }

    private void nextActivity(JsonStruct.User user) {
        if (user.gid != null) {
            // Directly go into group.
            this.startActivity(new Intent(this, MainActivity.class)
                    .putExtra(PreferenceKeys.GID_KEY, user.gid));
        } else {
            // Display list of groups.
            this.startActivity(new Intent(this, GroupsActivity.class));
        }
        // Close this activity.
        this.finish();
    }

    private void login(final String email, final String password) {
        final ProgressDialog dialog = ProgressDialog.show(LoginActivity.this, null, getString(R.string.login_signing_in));
        UnisonAPI api = new UnisonAPI(email, password);
        api.login(new UnisonAPI.Handler<JsonStruct.User>() {

            public void callback(JsonStruct.User user) {
                LoginActivity.this.storeInfo(email, password, user.nickname, user.uid);
                LoginActivity.this.nextActivity(user);
                dialog.dismiss();
            }

            public void onError(Error error) {
                Log.d(TAG, error.toString());
                if (error.statusCode == 403) {
                    Toast.makeText(LoginActivity.this, R.string.error_unauthorized,
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(LoginActivity.this, R.string.error_login_general,
                            Toast.LENGTH_LONG).show();
                }
                dialog.dismiss();
            }
        });
    }
}
