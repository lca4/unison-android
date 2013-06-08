
package ch.epfl.unison.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import ch.epfl.unison.AppData;
import ch.epfl.unison.Const;
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.UnisonAPI;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

/**
 * Activity that allows the user to sign up for the service.
 * 
 * @author lum
 */
public class SignupActivity extends SherlockActivity {

    private static final String TAG = "ch.epfl.unison.SignupActivity";
    private static final int MIN_PASSWORD_LENGTH = 6;

    private Button mSubmitBtn;
    private TextView mEmail;
    private TextView mPassword;
    private TextView mPassword2;
    private CheckBox mTou;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mSubmitBtn = (Button) findViewById(R.id.submitBtn);
        mSubmitBtn.setOnClickListener(new SubmitListener());

        mEmail = (TextView) findViewById(R.id.email);
        mPassword = (TextView) findViewById(R.id.password);
        mPassword2 = (TextView) findViewById(R.id.password2);
        mTou = (CheckBox) findViewById(R.id.touCbox);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { // if using home button
                                                     // from menu: R.id.home
            // app icon in Action Bar clicked; go home
            startActivity(new Intent(this, LoginActivity.class)
                    .setAction(GroupsActivity.ACTION_LEAVE_GROUP)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
        return true;
    }

    public void signup(final String email, final String password) {
        final ProgressDialog dialog = ProgressDialog.show(
                SignupActivity.this, null, getString(R.string.signup_signing_up));
        mSubmitBtn.setEnabled(false);

        UnisonAPI api = AppData.getInstance(this).getAPI();
        api.createUser(email, password, new UnisonAPI.Handler<JsonStruct.User>() {

            @Override
            public void onError(UnisonAPI.Error error) {
                handleError(error);
                mSubmitBtn.setEnabled(true);
                dialog.dismiss();
            }

            @Override
            public void callback(JsonStruct.User struct) {
                startActivity(new Intent(SignupActivity.this, LoginActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra(Const.PrefKeys.EMAIL, email)
                        .putExtra(Const.PrefKeys.PASSWORD, password));
                dialog.dismiss();
            }
        });
    }

    public void showError(String error) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }

    public void handleError(UnisonAPI.Error error) {
        if (error != null) {
            Log.d(TAG, error.toString());
            if (error.hasJsonError()) {
                if (UnisonAPI.ErrorCodes.MISSING_FIELD == error.jsonError.error) {
                    showError(getString(R.string.signup_form_missing_fields));

                } else if (UnisonAPI.ErrorCodes.EXISTING_USER == error.jsonError.error) {
                    showError(getString(R.string.signup_form_email_in_use));

                } else if (UnisonAPI.ErrorCodes.INVALID_EMAIL == error.jsonError.error) {
                    showError(getString(R.string.signup_form_email_invalid));

                } else if (UnisonAPI.ErrorCodes.INVALID_PASSWORD == error.jsonError.error) {
                    showError(getString(R.string.signup_form_password_invalid));
                }
                return;
            }
        }
        // Last resort.
        showError(getString(R.string.signup_form_unable_to_create));
    }

    /** Validates the data before submitting it to the back-end. */
    private class SubmitListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            if (TextUtils.isEmpty(mEmail.getText())
                    || TextUtils.isEmpty(mPassword.getText())
                    || TextUtils.isEmpty(mPassword2.getText())) {
                showError(getString(R.string.signup_form_fillout_fields));
            } else if (!mPassword.getText().toString().equals(mPassword2.getText().toString())) {
                showError(getString(R.string.signup_form_password_match));
            } else if (mPassword.length() < MIN_PASSWORD_LENGTH) {
                showError(getString(R.string.signup_form_password_short));
            } else if (!mTou.isChecked()) {
                showError(getString(R.string.signup_form_tou));
            } else {
                signup(mEmail.getText().toString(), mPassword.getText().toString());
            }
        }

    }

}
