
package ch.epfl.unison.ui;

import android.os.Bundle;
import android.webkit.WebView;

import ch.epfl.unison.R;

import com.actionbarsherlock.app.SherlockActivity;

/**
 * Simple activity consisting of a WebView displaying the help.
 * 
 * @author lum
 */
public class HelpActivity extends SherlockActivity {

    @SuppressWarnings("unused")
    private static final String TAG = "ch.epfl.unison.HelpActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help);

        WebView webView = (WebView) this.findViewById(R.id.webview);
        webView.loadUrl("file:///android_asset/help.html");
    }
}
