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
import ch.epfl.unison.Const;
import ch.epfl.unison.LibraryService;
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Log into the application (checks username / password against the back-end).
 *
 * @author lum
 */
public class LoginActivity extends SherlockActivity {

    private static final String TAG = "ch.epfl.unison.LoginActivity";
    
    private static final int SIGNUP_SUCCESS_RESULT_CODE = 0;

    private Button mLoginBtn;
    private TextView mSignupTxt;

    private EditText mEmail;
    private EditText mPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        
        Log.d(TAG, "Calling LoginActivity.onCreate");

        mEmail = (EditText) findViewById(R.id.email);
        mPassword = (EditText) findViewById(R.id.password);

        mSignupTxt = (TextView) findViewById(R.id.signupTxt);
        mSignupTxt.setText(Html.fromHtml(getString(R.string.login_signup_html)));
        mSignupTxt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            }

        });

        mLoginBtn = (Button) findViewById(R.id.loginBtn);
        mLoginBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = LoginActivity.this.mEmail.getText().toString();
                final String password = LoginActivity.this.mPassword.getText().toString();
                LoginActivity.this.login(email, password);
            }
        });

        // Initialize the AppData instance.
        AppData.getInstance(this);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = LoginActivity.this.getSupportMenuInflater();
        inflater.inflate(R.menu.login_menu, menu);  
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.login_menu_item_signup:
                startActivity(new Intent(LoginActivity.this, SignupActivity.class));
                break;
            default:
                break;
        }
        return true;
    }

    private void logout() {
        Log.d(TAG, "logging out");
        // Remove e-mail and password from the preferences.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(Const.PrefKeys.EMAIL);
        editor.remove(Const.PrefKeys.PASSWORD);
        editor.remove(Const.PrefKeys.UID);
        editor.remove(Const.PrefKeys.LASTUPDATE);
        editor.commit();

        // Truncate the library.
        startService(new Intent(LibraryService.ACTION_TRUNCATE));
    }

    /**
     * Called when starting the Activity after signing up. Sets the preferences (email,
     * password) according to the parameters.
     */
    private void bootstrap(String email, String password) {
        // We're coming from the signup form (whether native or online).
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Const.PrefKeys.EMAIL, email);
        editor.putString(Const.PrefKeys.PASSWORD, password);
        editor.remove(Const.PrefKeys.UID);
        editor.remove(Const.PrefKeys.LASTUPDATE);
        editor.commit();

        // Truncate the library. You never know.
        startService(new Intent(LibraryService.ACTION_TRUNCATE));
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.d(TAG, "Calling LoginActivity.onStart");
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean(Const.Strings.LOGOUT)) {
            logout();
        } else if (extras != null) {
            String email = extras.getString(Const.PrefKeys.EMAIL);
            String password = extras.getString(Const.PrefKeys.PASSWORD);
            if (email != null && password != null) {
                bootstrap(email, password);
            }
        }

        // Try to login from the saved preferences.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String email = prefs.getString(Const.PrefKeys.EMAIL, null);
        String password = prefs.getString(Const.PrefKeys.PASSWORD, null);

        if (email != null && password != null) {
            login(email, password);
        }

        fillEmailPassword();
    }

    private void fillEmailPassword() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mEmail.setText(prefs.getString(Const.PrefKeys.EMAIL, null));
        mPassword.setText(prefs.getString(Const.PrefKeys.PASSWORD, null));
    }

    private void storeInfo(String email, String password, String nickname, Long uid) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(Const.PrefKeys.EMAIL, email);
        editor.putString(Const.PrefKeys.PASSWORD, password);
        editor.putString(Const.PrefKeys.NICKNAME, nickname);
        if (uid != null) {
            editor.putLong(Const.PrefKeys.UID, uid);
        } else {
            editor.putLong(Const.PrefKeys.UID, -1);
        }
        editor.commit();
    }

    private void nextActivity(JsonStruct.User user) {
        if (user.gid != null) {
            // Directly go into group.
            startActivity(new Intent(this, GroupsMainActivity.class)
                    .putExtra(Const.Strings.GID, user.gid));
        } else {
            // Display list of groups.
//            startActivity(new Intent(this, GroupsActivity.class));
            startActivity(new Intent(this, HomeActivity.class));
        }
        // Close this activity.
        
        Log.d(TAG, "Going to finish LoginActiviy");
        finish();
    }

    private void login(final String email, final String password) {
        final ProgressDialog dialog = ProgressDialog.show(
                LoginActivity.this, null, getString(R.string.login_signing_in));
        UnisonAPI api = new UnisonAPI(email, password);
        api.login(new UnisonAPI.Handler<JsonStruct.User>() {

            @Override
            public void callback(JsonStruct.User user) {
                LoginActivity.this.storeInfo(email, password, user.nickname, user.uid);
                LoginActivity.this.nextActivity(user);
                dialog.dismiss();
            }

            @Override
            public void onError(Error error) {
                Log.d(TAG, error.toString());
                if (error.statusCode == Error.STATUS_FORBIDDEN) {
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
