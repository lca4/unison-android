
package ch.epfl.unison.ui;

import java.util.Arrays;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;
import ch.epfl.unison.Const;
import ch.epfl.unison.R;

import com.actionbarsherlock.app.SherlockActivity;

/**
 * @author vincent source: http://stackoverflow.com/questions/14222831/
 *         how-to-display-ndef-message-after-ndef-discovered-activity-launched
 */
public class NfcManagementActivity extends SherlockActivity {

    private static final String TAG = "ch.epfl.unison.NfcManagementActivity";

    // private boolean mNFCStatusChecked = false;
    private NfcAdapter mAdapter = null;
    // private NdefMessage mMessage = null;
    // private int mDebugGroupID = 123;
    private PendingIntent mNfcIntent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_management);
        // setupNFC();
        handleNFCIntent(getIntent());
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

        if (mAdapter != null) {
            mAdapter.enableForegroundDispatch(this, mNfcIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        if (mAdapter != null) {
            mAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // TODO Auto-generated method stub
        super.onNewIntent(intent);

        handleNFCIntent(intent);

    }

    private void setupNFC() {

        NfcManager manager = (NfcManager) getApplicationContext()
                .getSystemService(Context.NFC_SERVICE);
        mAdapter = manager.getDefaultAdapter();
        if (mAdapter == null) {
            Toast.makeText(NfcManagementActivity.this,
                    R.string.error_NFC_not_present, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!mAdapter.isEnabled()) {
            Toast.makeText(NfcManagementActivity.this,
                    R.string.error_NFC_not_active, Toast.LENGTH_LONG).show();
            // startActivity(new Intent(Settings.ACTION_NFCSHARING_SETTINGS));
        } else {
            Log.d(TAG, "NFC is enabled!");
            mNfcIntent = PendingIntent.getActivity(getApplicationContext(), 3,
                    new Intent(this, NFCReceiveGroupActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        }

    }

    private void handleNFCIntent(Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            NdefMessage[] messages = null;
            Parcelable[] rawMsgs = intent
                    .getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                messages = Arrays.copyOf(rawMsgs, rawMsgs.length,
                        NdefMessage[].class);
                Log.d(TAG, "there were messages");
                // rawMsgs.
                // messages = new NdefMessage[rawMsgs.length];
                // for (int i = 0; i < rawMsgs.length; i++) {
                // messages[i] = (NdefMessage) rawMsgs[i];
                // }
            } else {
                Toast.makeText(getApplicationContext(), "No NDEF Message Read",
                        Toast.LENGTH_LONG).show();
                return;
            }
            if (messages != null && messages[0] != null
                    && messages[0].getRecords() != null) {
                // String result = "";
                // byte[] payload = messages[0].getRecords()[0].getPayload();
                // for (int b = 0; b < payload.length; b++) {
                // result += (char) payload[b];
                // }
                // Toast.makeText(getApplicationContext(), "Read: " + result,
                // Toast.LENGTH_SHORT).show();
                //
                // long groupID = Long.valueOf(result);
                String plString;
                int gid;

                //Look for our custom message
                for (NdefMessage msg : messages) {
                    NdefRecord[] records = msg.getRecords();
                    if (records != null) {
                        for (NdefRecord rec : records) {
                            if (rec != null) {
                                if (rec.getTnf() == NdefRecord.TNF_EXTERNAL_TYPE
                                        && byteArrayEqual(rec.getType(), Const.Strings.UNISON_NFC_MIME_TYPE.getBytes())) {
                                    //This is it
                                    byte[] pl = rec.getPayload();
                                    if (pl != null) {
                                        plString = new String(pl);
                                        try {
                                            JSONObject jo = new JSONObject(plString);
                                            gid = jo.getInt("gid");
                                            
                                            //TODO do something with the group ID.
                                        } catch (JSONException je) {
                                            Toast.makeText(getApplicationContext(), "Intent Error...",
                                                    Toast.LENGTH_LONG).show();
                                        }
                                        break;
                                    }
                                } 
                            }
                        }
                    }
                }


//                // FIXME
//                Log.d(TAG, "Read " + result);

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
    
    private boolean byteArrayEqual(byte[] t1, byte[] t2) {
        boolean eq = true;
        if (t1 == null || t2 == null || t1.length != t2.length) {
            eq = false;
        } else {
            for (int i = 0; i < t1.length; ++i) {
                if (t1[i] != t2[i]) {
                    eq = false;
                    break;
                }
            }
        }
        return eq;
    }

}
