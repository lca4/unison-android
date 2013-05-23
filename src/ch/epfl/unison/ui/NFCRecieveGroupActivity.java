
package ch.epfl.unison.ui;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus.NmeaListener;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import ch.epfl.unison.R;

import com.actionbarsherlock.app.SherlockActivity;

public class NFCRecieveGroupActivity extends SherlockActivity {



    private static final String TAG = "ch.epfl.unison.NFCRecieveGroupActivity";

    private boolean mNFCStatusChecked = false;
    private NfcAdapter mAdapter = null;
    private NdefMessage mMessage = null;
    private int mDebugGroupID = 123;
    private PendingIntent mPe = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_recieve_group);
        setupNFC();
    }


    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        
        if(mNFCStatusChecked ) {
            mAdapter.enableForegroundDispatch(this, mPe, null, null);
        }
    }
    
    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        if(mNFCStatusChecked) {
            mAdapter.disableForegroundDispatch(this);
        }
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        // TODO Auto-generated method stub
        super.onNewIntent(intent);
        
        
        mMessage  = intent.getParcelableExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        
        if (mMessage == null) {
            Log.d(TAG, "could not retrieve message from intent.");
            return;
        }
        /*StringBuilder sb = new StringBuilder();
        for(int i = 0; i < tag.getId().length; i++){
            sb.append(new Integer(tag.getId()[i]) + " ");
        }*/
//        info.setText("TagID: " + bytesToHex(tag.getId()));
        
        NdefRecord[] records = mMessage.getRecords();
        NdefRecord record = records[0];
        String payload = new String(record.getPayload());
        Log.d(TAG, "We recieved this payload: " + payload);
        
    }



    private void setupNFC() {
        
        NfcManager manager = (NfcManager) getApplicationContext().getSystemService(Context.NFC_SERVICE);
        mAdapter = manager.getDefaultAdapter();
        if (mAdapter == null) {
           Toast.makeText(NFCRecieveGroupActivity.this, R.string.error_NFC_not_present,
                    Toast.LENGTH_LONG).show();
           finish();
           return;
        }
        
        
        if (!mAdapter.isEnabled()) {
            Toast.makeText(NFCRecieveGroupActivity.this, R.string.error_NFC_not_active,
                    Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_NFCSHARING_SETTINGS));
        } else {
            Log.d(TAG, "NFC is enabled!");
            mPe = PendingIntent.getActivity(
                    getApplicationContext(), 3, new Intent(this,
                            NFCRecieveGroupActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            
            
            mNFCStatusChecked = true;
            
            
        }
        
    }
    

}
