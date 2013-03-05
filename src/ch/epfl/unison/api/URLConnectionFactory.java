package ch.epfl.unison.api;

import java.io.IOException;
import java.net.URLConnection;

/**
 * 
 * Factory with setter so we can use JMockit to mock the URLConnection
 * 
 */

public class URLConnectionFactory {

	public static boolean DEBUG = false;

	private static String TAG = "ch.epfl.unison.URLConnectionFactory";

	private static URLConnection mUrlConnection = null;

	public static void setInstance(URLConnection connection) {
		if (DEBUG) {
			if (mUrlConnection == null) {
				mUrlConnection = connection;
			}
		} else {
			mUrlConnection = connection;
		}
	}

	public static URLConnection getInstance() throws IOException {
		if (mUrlConnection == null) {
			throw new IOException(TAG);
		}
		return mUrlConnection;
	}
}
