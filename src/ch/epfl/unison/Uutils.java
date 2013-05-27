package ch.epfl.unison;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

/**
 * Small utilities needed here and there.
 *
 * @author lum
 */
public final class Uutils {

    private static final String TAG = "ch.epfl.unison.Uutils";
    private static final int ONE_KILOMETER = 1000;  // in meters.

    /**
     * Hide the constructor. We use this class for namespacing, an object
     * makes no sense.
     */
    private Uutils() { };

    public static String distToString(Float dist) {
        if (dist == null) {
            return "";
        }
        if (dist >= ONE_KILOMETER) {
            return String.format("%.2fkm", dist / ONE_KILOMETER);
        } else {
            return String.format("%dm", dist.intValue());
        }
    }

    public static void setBitmapFromURL(ImageView image, String url) {
        new BitmapFromURL(image, url).execute();
    }
    
    public static JSONObject merge(JSONObject json1, JSONObject json2) {
        JSONObject merged = new JSONObject();
        if (json1 == null && json2 == null) {
            return null;
        } else if (json1 == null) {
            return json2;
        } else if (json2 == null) {
            return json1;
        }
        JSONObject[] objs = new JSONObject[] { json1, json2 };
        for (JSONObject obj : objs) {
            Iterator<String> it = obj.keys();
            while (it.hasNext()) {
                String key = it.next();
                try {
                    merged.put(key, obj.get(key));
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    Log.i(TAG, e.getMessage());
                }
            }
        }
        return merged;
    }

    /**
     * Small helper class that sets an ImageView from a URL. It fetches the image
     * over the network in the background, and upon receiving the file, decodes it
     * and updates the View.
     */
    private static class BitmapFromURL extends AsyncTask<Void, Void, Bitmap> {

        private ImageView mImage;
        private String mUrl;

        public BitmapFromURL(ImageView image, String url) {
            mImage = image;
            mUrl = url;
        }

        @Override
        protected Bitmap doInBackground(Void... nothing) {
            try {
                InputStream stream = (InputStream) new URL(mUrl).getContent();
                return BitmapFactory.decodeStream(stream);
            } catch (Exception e) {
                Log.i(TAG, String.format("couldn't get a bitmap from %s", mUrl), e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                mImage.setImageBitmap(result);
            }
        }
    }
    
    public static Date stringToDate(String s) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(s);
    }
    
    /**
     * 
     * @param u Uri used to make the insertion
     * @return
     */
    public static int lastInsert(Uri u) {
        // Empirically seen that the index is at the end
        return Integer.parseInt(u.getPathSegments().get(u.getPathSegments().size() - 1));
    }
}
