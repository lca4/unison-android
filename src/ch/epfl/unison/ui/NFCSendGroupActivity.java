
package ch.epfl.unison.ui;

import android.content.Context;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import ch.epfl.unison.R;

import com.actionbarsherlock.app.SherlockActivity;

/**
 * @author vincent
 */
public class NFCSendGroupActivity extends SherlockActivity {

    private static final String TAG = "ch.epfl.unison.NFCSendGroupActivity";
    private NfcAdapter mAdapter = null;
    private boolean mNFCStatusChecked = false;
    private byte[] mDummyByte = {
            new Integer(1).byteValue()
    };
    private NdefMessage mMessage = null;
    private int mDebugGroupID = 123;
    private String mDebugRecord = "123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_send_group);

        setupNFC();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNFCStatusChecked) {
            mAdapter.disableForegroundNdefPush(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNFCStatusChecked) {
            if (mAdapter == null) {
                Log.d(TAG, "adapter was NULL !!!");
            }
            try {
                mMessage = new NdefMessage(mDummyByte);
                mAdapter.enableForegroundNdefPush(this, mMessage);
            } catch (FormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void setupNFC() {

        NfcManager manager = (NfcManager) getApplicationContext()
                .getSystemService(Context.NFC_SERVICE);
        mAdapter = manager.getDefaultAdapter();
        if (mAdapter == null) {
            Toast.makeText(NFCSendGroupActivity.this, R.string.error_NFC_not_present,
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!mAdapter.isEnabled()) {
            Toast.makeText(NFCSendGroupActivity.this, R.string.error_NFC_not_active,
                    Toast.LENGTH_LONG).show();
            // startActivity(new Intent(Settings.ACTION_NFCSHARING_SETTINGS));
        } else {
            Log.d(TAG, "NFC is enabled!");
            mNFCStatusChecked = true;

        }

    }

    private void setMessage(int groupID) throws FormatException {
        NdefRecord[] records = new NdefRecord[1];
        // records[0] = new NdefRecord(mDebugRecord.getBytes());
        records[0] = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_TEXT, new byte[] {}, mDebugRecord.getBytes());
        mMessage = new NdefMessage(records);
    }

    public void buttonClicked(View v) {
        sendNFCMessage();
    }

    private void sendNFCMessage() {
        try {
            setMessage(mDebugGroupID);
            mAdapter.enableForegroundNdefPush(this, mMessage);
        } catch (FormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
