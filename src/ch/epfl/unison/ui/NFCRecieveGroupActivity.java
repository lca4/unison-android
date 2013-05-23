
package ch.epfl.unison.ui;

import ch.epfl.unison.R;
import ch.epfl.unison.R.layout;
import ch.epfl.unison.R.menu;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class NFCRecieveGroupActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_recieve_group);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.nfcrecieve_group, menu);
        return true;
    }

}
