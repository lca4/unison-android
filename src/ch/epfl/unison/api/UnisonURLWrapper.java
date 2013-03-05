package ch.epfl.unison.api;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * 
 * This class is used to hide the URLConnectionFactory from the rest
 * of the code.
 *
 */
public class UnisonURLWrapper {
	
	private URL mUrl;
	
	public UnisonURLWrapper(String spec) throws MalformedURLException {
		this.mUrl = new URL(spec);
	}
	
	public URLConnection openConnection() throws IOException {
		URLConnectionFactory.setInstance(mUrl.openConnection());
		return URLConnectionFactory.getInstance();
	}
	
	@Override
	public String toString() {
		return mUrl.toString();
	}
}




