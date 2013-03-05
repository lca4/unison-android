package ch.epfl.unison.api;

import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectHandler;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

public class HttpClientFactory {

	
	private static HttpClient httpClient;
	
	private static final int HTTP_PORT = 80;
	private static final int HTTPS_PORT = 443;
	
	public static synchronized HttpClient getInstance() {
		if (httpClient == null) {
			httpClient = create();
		}
		
		return httpClient;
	}

	final private static RedirectHandler REDIRECT_NO_FOLLOW = new RedirectHandler() {
		
		public boolean isRedirectRequested(HttpResponse response, HttpContext context) {
			return false;
		}

		
		public URI getLocationURI(HttpResponse response, HttpContext context) throws org.apache.http.ProtocolException {
			return null;
		}
	};

	private static HttpClient create() {
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), HTTP_PORT));
		schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), HTTPS_PORT));
		HttpParams params = new BasicHttpParams();
		ThreadSafeClientConnManager connManager = new ThreadSafeClientConnManager(params, schemeRegistry);
		AbstractHttpClient result = new DefaultHttpClient(connManager, params);
		result.setRedirectHandler(REDIRECT_NO_FOLLOW);
		return result;
	}

	//For tests since Mockito cannot mock AbstractHttpClient.
	public static void setInstance(HttpClient instance) {
		httpClient = instance;
	}
}
