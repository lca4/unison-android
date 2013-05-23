
package ch.epfl.unison;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class NFCSendGroupActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_send_group);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.nfcsend_group, menu);
        return true;
    }

}
