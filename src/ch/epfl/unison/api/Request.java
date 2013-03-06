package ch.epfl.unison.api;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Request<T extends JsonStruct> {

    private static final String TAG = "ch.epfl.unison.Request";

    static {
        // Close HTTP connections at end of request. Fixes some bugs in early
        // versions of Android.
        System.setProperty("http.keepAlive", "false");
    }

    private static final String ENCODING = "UTF-8";
    private static final int CONNECT_TIMEOUT = 30 * 1000;  // In ms.
    private static final int READ_TIMEOUT = 30 * 1000;  // In ms.
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    private URL url;
    private Class<T> classOfT;

    private String auth;
    private Map<String, List<String>> data;

    private Request(URL url, Class<T> classOfT) {
        this.url = url;

        // We need this to be able to instantiate the correct JSONStruct.
        this.classOfT = classOfT;
    }

    public static <S extends JsonStruct> Request<S> of(
    		URL url, Class<S> classOfS) {
        return new Request<S>(url, classOfS);
    }

    public Request<T> addParam(String key, Object value) {
        if (this.data == null) {
            // This is the first parameter. Initialize the map.
            this.data = new HashMap<String, List<String>>();
        }
        if (!this.data.containsKey(key)) {
            // First value for this key. Initialize the list of values.
            this.data.put(key, new LinkedList<String>());
        }
        this.data.get(key).add(value.toString());
        return this;
    }

    public Request<T> setAuth(String auth) {
        this.auth = auth;
        return this;
    }

    public Result<T> doGET() {
        return this.execute(new HttpGet());
    }

    public Result<T> doPOST() {
        return this.execute(new HttpPost());
    }

    public Result<T> doPUT() {
        return this.execute(new HttpPut());
    }

    public Result<T> doDELETE() {
        return this.execute(new HttpDelete());
    }

    private Result<T> execute(HttpRequestBase request) {
    	try {
			request.setURI(this.url.toURI());
		} catch (URISyntaxException e) {
			//This should never happen
			return new Result<T>(new UnisonAPI.Error(0, "", "", e));
		}
        Log.i(TAG, String.format("%s request to %s", request.getMethod(), request.getURI()));
        String responseContent = null;
        HttpResponse response = null;
        StatusLine responseStatusLine = null;
        
        try {
            // Configure some sensible defaults.
            //Timeout setting could also be done in client, we choose to keep it here for better readability.
            request.getParams().setParameter("http.connection.timeout", Integer.valueOf(CONNECT_TIMEOUT));
            request.getParams().setParameter("http.socket.timeout", Integer.valueOf(READ_TIMEOUT));
            

            if (this.auth != null) {
                // Set a raw HTTP Basic Auth header (java.net.Authenticator has issues).
                request.addHeader("Authorization", "Basic " + this.auth);
            }

            if (this.data != null && request instanceof HttpEntityEnclosingRequestBase) {
                // Write out the request body (i.e. the form data).
//            	request.setHeader("", data);
//            	((HttpEntityEnclosingRequestBase) request).setEntity(new StringEntity(data, ENCODING));
            	((HttpEntityEnclosingRequestBase) request).setEntity(new UrlEncodedFormEntity(generateQueryNVP(this.data), ENCODING));
//            	Log.d(TAG, "entity = " + EntityUtils.toString(((HttpEntityEnclosingRequestBase) request).getEntity()));
//            	((HttpEntityEnclosingRequestBase) request).setEntity(new UrlEncodedFormEntity(generateQueryNVP(this.data), ENCODING));
            }

            try {
                // Get the response as a string.
            	response = HttpClientFactory.getInstance().execute(request);
            	responseStatusLine = response.getStatusLine();
//            	Log.d(TAG, "status = " + responseStatusLine.toString());
            	responseContent = response.getEntity() == null ? null : EntityUtils
        				.toString(response.getEntity());
            } catch(IOException ioe) {
                // Happens when the server returns an error status code.
            	responseContent = responseStatusLine == null ? null : responseStatusLine.getReasonPhrase();
            }

            int status = responseStatusLine.getStatusCode();
            
            if (status < 200 || status > 299) {
                // We didn't receive a 2xx status code - we treat it as an error.
                JsonStruct.Error jsonError = GSON.fromJson(responseContent, JsonStruct.Error.class);
                return new Result<T>(new UnisonAPI.Error(status,
                		responseStatusLine.toString(), responseContent, jsonError));
            } else {
                // Success.
//            	Log.d(TAG, "received: " + responseContent);
                T jsonStruct = GSON.fromJson(responseContent, this.classOfT);
                return new Result<T>(jsonStruct);
            }

        } catch (Exception e) {
            // Under this catch-all, we mean:
            // - IOException, thrown by most HttpURLConnection methods,
            // - NullPointerException. if there's not InputStream nor ErrorStream,
            // - JsonSyntaxException, if we fail to decode the server's response.
            Log.e(TAG, "caught exception while handling request", e);
            int statusCode = 0;
            String statusMessage = null;
            try {
                statusCode = responseStatusLine.getStatusCode();
                statusMessage = responseStatusLine.getReasonPhrase();
            } catch(Exception foobar) {}

            return new Result<T>(new UnisonAPI.Error(statusCode, statusMessage, responseContent, e));

        }
    }
    
    private static List<NameValuePair> generateQueryNVP(Map<String, List<String>> data) {
    	List<NameValuePair> nvp = new ArrayList<NameValuePair>();
		for (Map.Entry<String, List<String>> entry : data.entrySet()) {
			for (String value : entry.getValue()) {
				nvp.add(new BasicNameValuePair(entry.getKey(), value));
			}
		}
		return nvp;
    }

    public static class Result<S> {
        public final UnisonAPI.Error error;
        public final S result;

        private Result(S result, UnisonAPI.Error error) {
            this.result = result;
            this.error = error;
        }

        public Result(S result) {
            this(result, null);
        }

        public Result(UnisonAPI.Error error) {
            this(null, error);
        }
    }

}
