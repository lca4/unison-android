
package ch.epfl.unison.ui;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import ch.epfl.unison.R;

import com.actionbarsherlock.app.SherlockActivity;

/**
 * @author vincent source: http://stackoverflow.com/questions/14222831/
 *         how-to-display-ndef-message-after-ndef-discovered-activity-launched
 */
public class NFCReceiveGroupActivity extends SherlockActivity {

    private static final String TAG = "ch.epfl.unison.NFCRecieveGroupActivity";

    private boolean mNFCStatusChecked = false;
    private NfcAdapter mAdapter = null;
    private NdefMessage mMessage = null;
    private int mDebugGroupID = 123;
    private PendingIntent mPe = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_nfc_receive_group);
        setupNFC();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

        if (mNFCStatusChecked) {
            mAdapter.enableForegroundDispatch(this, mPe, null, null);
        }
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        if (mNFCStatusChecked) {
            mAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // TODO Auto-generated method stub
        super.onNewIntent(intent);

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            NdefMessage[] messages = null;
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                messages = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    messages[i] = (NdefMessage) rawMsgs[i];
                }
            } else if (rawMsgs == null) {
                Toast.makeText(getApplicationContext(), "No NDEF Message Read",
                        Toast.LENGTH_LONG).show();
            }
            if (messages[0] != null) {
                String result = "";
                byte[] payload = messages[0].getRecords()[0].getPayload();
                for (int b = 0; b < payload.length; b++) {
                    result += (char) payload[b];
                }
                Toast.makeText(getApplicationContext(), "Read: " + result,
                        Toast.LENGTH_SHORT).show();

                long groupID = Long.valueOf(result);

            }
        } else {
            Toast.makeText(getApplicationContext(), "Intent Error...",
                    Toast.LENGTH_LONG).show();
        }

        /*
         * Log.d(TAG, "Recieved intent : " + intent.toString()); Bundle extras =
         * intent.getExtras(); Log.d(TAG, "Containing the extras : " +
         * extras.toString()); for (String key : extras.keySet()) { Log.d(TAG,
         * key); } NdefMessage[] messages =
         * extras.getParcelableArray(NfcAdapter.EXTRA_NDEF_MESSAGES); Log.d(TAG,
         * "we got " + messages.length + " messages."); if (intent.getAction()
         * == NfcAdapter.ACTION_NDEF_DISCOVERED) { } else { Log.d(TAG,
         * "ignoring intent because not related to NFC"); }
         */

        // CRAP:
        // mMessage = intent.getParcelableExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

        // if (mMessage == null) {
        // Log.d(TAG, "could not retrieve message from intent.");
        // return;
        // }
        /*
         * StringBuilder sb = new StringBuilder(); for(int i = 0; i <
         * tag.getId().length; i++){ sb.append(new Integer(tag.getId()[i]) +
         * " "); }
         */
        // info.setText("TagID: " + bytesToHex(tag.getId()));

        // NdefRecord[] records = mMessage.getRecords();
        // NdefRecord record = records[0];
        // String payload = new String(record.getPayload());
        // Log.d(TAG, "We recieved this payload: " + payload);

    }

    private void setupNFC() {

        NfcManager manager = (NfcManager) getApplicationContext()
                .getSystemService(Context.NFC_SERVICE);
        mAdapter = manager.getDefaultAdapter();
        if (mAdapter == null) {
            Toast.makeText(NFCReceiveGroupActivity.this, R.string.error_NFC_not_present,
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!mAdapter.isEnabled()) {
            Toast.makeText(NFCReceiveGroupActivity.this, R.string.error_NFC_not_active,
                    Toast.LENGTH_LONG).show();
            // startActivity(new Intent(Settings.ACTION_NFCSHARING_SETTINGS));
        } else {
            Log.d(TAG, "NFC is enabled!");
            mPe = PendingIntent.getActivity(
                    getApplicationContext(), 3, new Intent(this,
                            NFCReceiveGroupActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

            mNFCStatusChecked = true;

        }

    }

}
