
package ch.epfl.unison;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.Log;

import ch.epfl.unison.Const.SeedType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Abstraction of a playlist. A Playlist object could be shared between the
 * single-user mode and groups.
 * 
 * @author marc
 */
public class Playlist {

    // private HashMap<SeedType, LinkedList<Long>> mRaw;
    private ArrayList<Integer> mRawTags;
    // TODO mRawTracks
    private LinkedList<MusicItem> mPlaylist;
    private HashMap<String, Object> mOptions;

    public Playlist() {
        mRawTags = new ArrayList<Integer>();
        mPlaylist = new LinkedList<MusicItem>();
        mOptions = new HashMap<String, Object>();
    }

    public void addRawTags(ArrayList<Integer> seeds) {
//        Log.i("Playlist", "to be added to rawlist: " + seeds + "\n");
        mRawTags.addAll(seeds);
//        Log.i("Playlist", "rawlist: " + mRawTags.toString() + "\n");
    }

    /**
     * Helper to convert a TypedArray to a String in JSONObject format. The
     * TypedArray values are treated as Strings. Only the values with index from
     * indexes are selected.
     * 
     * @param key
     * @param values
     * @param indexes
     * @return null in case of failure
     */
    @SuppressLint("NewApi")
    public JSONObject export(Resources res) {

        if (mRawTags.isEmpty()) {
            return null;
        }

        JSONObject json = new JSONObject();

        // Tags
        TypedArray tags = res.obtainTypedArray(R.array.tags);
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < mRawTags.size(); i++) {
            jsonArray.put(tags.getString(i));
        }
        tags.recycle();

        // Tracks
        // TODO

        try {
            json.put(SeedType.TAGS.getLabel(), jsonArray);
            // json.put(SeedType.TRACKS.getLabel(), tracksInString);
            if (!mOptions.isEmpty()) {
                // TODO
                String foo = null;
            }
            return json;
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

}
