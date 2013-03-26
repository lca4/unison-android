package ch.epfl.unison.api;

import android.os.AsyncTask;

import java.net.URL;

/**
 * Wraps a Request in an AsyncTask to allow non-blocking requests.
 *
 * @param <T> the type of the response
 * @author lum
 */
public final class AsyncRequest<T extends JsonStruct>
        extends AsyncTask<AsyncRequest.Method, Void, Request.Result<T>> {

    private Request<T> mRequest;
    private UnisonAPI.Handler<T> mHandler;

    /** Types of HTTP requests. */
    public static enum Method {
        GET,
        POST,
        PUT,
        DELETE,
    }


    private AsyncRequest(URL url, UnisonAPI.Handler<T> handler, Class<T> classOfT) {
        mRequest = Request.of(url, classOfT);
        mHandler = handler;
    }

    public static <S extends JsonStruct> AsyncRequest<S> of(
            URL url, UnisonAPI.Handler<S> handler, Class<S> classOfS) {
        return new AsyncRequest<S>(url, handler, classOfS);
    }

    public AsyncRequest<T> addParam(String key, Object value) {
        mRequest.addParam(key, value);
        return this;
    }

    public AsyncRequest<T> setAuth(String auth) {
        mRequest.setAuth(auth);
        return this;
    }

    public void doGET() {
        execute(Method.GET);
    }

    public void doPOST() {
        execute(Method.POST);
    }

    public void doPUT() {
        execute(Method.PUT);
    }

    public void doDELETE() {
        execute(Method.DELETE);
    }

    @Override
    protected Request.Result<T> doInBackground(Method... method) {
        switch(method[0]) {
        case GET:
            return mRequest.doGET();
        case POST:
            return mRequest.doPOST();
        case PUT:
            return mRequest.doPUT();
        case DELETE:
            return mRequest.doDELETE();
        default:
            return null;  // Should never happen.
        }
    }

    @Override
    protected void onPostExecute(Request.Result<T> res) {
        if (res.result != null) {
            mHandler.callback(res.result);
        } else {
            mHandler.onError(res.error);
        }
    }
}
