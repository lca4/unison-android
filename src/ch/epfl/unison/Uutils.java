
package ch.epfl.unison;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import ch.epfl.unison.data.PlaylistItem;
import ch.epfl.unison.data.TrackItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

/**
 * Small utilities needed here and there.
 * 
 * @author lum
 */
public final class Uutils {

    private static final String TAG = "ch.epfl.unison.Uutils";
    private static final int ONE_KILOMETER = 1000; // in meters.

    /**
     * Hide the constructor. We use this class for namespacing, an object makes
     * no sense.
     */
    private Uutils() {
    };

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
        JSONObject[] objs = new JSONObject[] {
                json1, json2
        };
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
     * Small helper class that sets an ImageView from a URL. It fetches the
     * image over the network in the background, and upon receiving the file,
     * decodes it and updates the View.
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
     * @param u Uri used to make the insertion
     * @return
     */
    public static long lastInsertId(Uri u) {
        // Empirically seen that the index is at the end
        return Long.parseLong(u.getPathSegments().get(u.getPathSegments().size() - 1));
    }

    /** Container of adapters. */
    public static final class Adapters {

        private static int smRowLayout = R.layout.listrow; // default layout
        private static Activity smActivity;

        private Adapters() {
            // Can't be instantiated
        }

        /** ArrayAdapter that displays the tracks of the playlist. */
        public static class PlaylistsAdapter extends ArrayAdapter<PlaylistItem> {

            // private static final int ROW_LAYOUT = R.layout.list_row;

            public PlaylistsAdapter(Activity activity, ArrayList<PlaylistItem> playlists) {
                super(activity, 0, playlists);
                smActivity = activity;
                smRowLayout = R.layout.listrow_playlist;
            }

            @Override
            public View getView(int position, View view, ViewGroup parent) {
                PlaylistItem pl = (PlaylistItem) getItem(position);
                if (view == null) {
                    LayoutInflater inflater =
                            (LayoutInflater) smActivity
                                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = inflater.inflate(smRowLayout, parent, false);
                }
                ((TextView) view.findViewById(R.id.listrow_playlist_title))
                        .setText((getItem(position)).getTitle());
                int size = (int) getItem(position).getSize();
                String subtitle = smActivity.getString(R.string.default_empty);
                switch (size) {
                    case 0:
                        subtitle = smActivity.getString(R.string.solo_playlist_contains_no_track);
                        break;
                    case 1:
                        subtitle = smActivity.getString(R.string.solo_playlist_contains_track);
                    default:
                        subtitle = smActivity
                                .getString(R.string.solo_playlist_contains_tracks, size);
                        break;
                }
                ((TextView) view.findViewById(R.id.listrow_playlist_nbTracks))
                        .setText(subtitle);
                view.setTag(pl);
                return view;
            }
        }

        /** ArrayAdapter that displays the tracks of the playlist. */
        public static class TracksAdapter extends ArrayAdapter<TrackItem> {

            public TracksAdapter(Activity activity, PlaylistItem playlist) {
                super(activity, 0, playlist.getTracks());
                smActivity = activity;
                smRowLayout = R.layout.listrow_track;
            }

            @Override
            public View getView(int position, View view, ViewGroup parent) {
                TrackItem track = getItem(position);
                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater) smActivity
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = inflater.inflate(smRowLayout, parent, false);
                }
                ((TextView) view.findViewById(R.id.listrow_track_title))
                        .setText(getItem(position).title);
                ((TextView) view.findViewById(R.id.listrow_track_artist))
                        .setText(getItem(position).artist);
                view.findViewById(R.id.listrow_track_rating).setVisibility(View.INVISIBLE);
                // int rating = 0;
                // if (getItem(position).rating != null) {
                // rating = getItem(position).rating;
                // }
                // ((RatingBar)
                // view.findViewById(R.id.trRating)).setRating(rating);
                view.setTag(track);
                return view;
            }
        }
    }
}
