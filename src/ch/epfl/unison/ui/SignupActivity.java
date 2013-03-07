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
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.PreferenceKeys;
import ch.epfl.unison.api.UnisonAPI;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

public class SignupActivity extends SherlockActivity {

    private static final String TAG = "ch.epfl.unison.SignupActivity";

    private Button submitBtn;
    private TextView email;
    private TextView password;
    private TextView password2;
    private CheckBox tou;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.signup);
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.submitBtn = (Button) this.findViewById(R.id.submitBtn);
        this.submitBtn.setOnClickListener(new SubmitListener());

        this.email = (TextView) this.findViewById(R.id.email);
        this.password = (TextView) this.findViewById(R.id.password);
        this.password2 = (TextView) this.findViewById(R.id.password2);
        this.tou = (CheckBox) this.findViewById(R.id.touCbox);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
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
        this.submitBtn.setEnabled(false);

        UnisonAPI api = AppData.getInstance(this).getAPI();
        api.createUser(email, password, new UnisonAPI.Handler<JsonStruct.User>() {

            public void onError(UnisonAPI.Error error) {
                handleError(error);
                submitBtn.setEnabled(true);
                dialog.dismiss();
            }

            public void callback(JsonStruct.User struct) {
                startActivity(new Intent(SignupActivity.this, LoginActivity.class)
                        .putExtra(PreferenceKeys.EMAIL_KEY, email).putExtra(PreferenceKeys.PASSWORD_KEY, password));
                dialog.dismiss();
            }
        });
    }

    public void showError(String error) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }

    public void handleError(UnisonAPI.Error error) {
        Log.d(TAG, error.toString());
        if (error.hasJsonError()) {
            if (UnisonAPI.ErrorCodes.MISSING_FIELD == error.jsonError.error) {
                this.showError(getString(R.string.signup_form_missing_fields));

            } else if (UnisonAPI.ErrorCodes.EXISTING_USER == error.jsonError.error) {
                this.showError(getString(R.string.signup_form_email_in_use));

            } else if (UnisonAPI.ErrorCodes.INVALID_EMAIL == error.jsonError.error) {
                this.showError(getString(R.string.signup_form_email_invalid));

            } else if (UnisonAPI.ErrorCodes.INVALID_PASSWORD == error.jsonError.error) {
                this.showError(getString(R.string.signup_form_password_invalid));
            }
            return;
        }
        // Last resort.
        this.showError(getString(R.string.signup_form_unable_to_create));
    }

    private class SubmitListener implements OnClickListener {

        public void onClick(View v) {
            if (TextUtils.isEmpty(email.getText())
                    || TextUtils.isEmpty(password.getText())
                    || TextUtils.isEmpty(password2.getText())) {
                showError(getString(R.string.signup_form_fillout_fields));
            } else if (!password.getText().toString().equals(password2.getText().toString())) {
                showError(getString(R.string.signup_form_password_match));
            } else if (password.length() < 6) {
                showError(getString(R.string.signup_form_password_short));
            } else if (!tou.isChecked()) {
                showError(getString(R.string.signup_form_tou));
            } else {
                signup(email.getText().toString(), password.getText().toString());
            }
        }

    }

}
