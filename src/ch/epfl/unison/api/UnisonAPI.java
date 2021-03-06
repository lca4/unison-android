
package ch.epfl.unison.api;

import android.util.Base64;
import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Java interface to the Unison RESTful HTTP API.
 * 
 * @author lum
 */
public class UnisonAPI {

    private static final String TAG = "ch.epfl.unison.UnisonAPI";

    // TODO: revert to production server (make it a preference?).
    // private static final String API_ROOT =
    // "http://staging.groupstreamer.com";
    // private static final String API_ROOT = "https://127.0.0.1"
    private static final String API_ROOT = "https://api.groupstreamer.com";
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    private String mAuth;

    /** No-args constructor for use without authentication. */
    public UnisonAPI() {
    }

    public UnisonAPI(String email, String password) {
        setAuth(email, password);
    }

    public UnisonAPI setAuth(String email, String password) {
        // It is safe to call getBytes without charset, as the default (on
        // Android) is UTF-8.
        String encEmail = Base64.encodeToString(email.getBytes(), Base64.NO_WRAP);
        String encPassword = Base64.encodeToString(password.getBytes(), Base64.NO_WRAP);

        // At this point, it should be encoded using ISO-8859-1, but the string
        // is ASCII anyways.
        mAuth = Base64.encodeToString((encEmail + ':' + encPassword).getBytes(), Base64.NO_WRAP);
        return this;
    }

    public void login(Handler<JsonStruct.User> handler) {
        URL url = urlFor("/");
        AsyncRequest.of(url, handler, JsonStruct.User.class)
                .setAuth(mAuth).doGET();
    }

    public void createUser(String email, String password,
            Handler<JsonStruct.User> handler) {
        URL url = urlFor("/users");
        AsyncRequest.of(url, handler, JsonStruct.User.class)
                .addParam("email", email).addParam("password", password).doPOST();
    }

    public void getNickname(long uid, Handler<JsonStruct.User> handler) {
        URL url = urlFor("/users/%d/nickname", uid);
        AsyncRequest.of(url, handler, JsonStruct.User.class)
                .setAuth(mAuth).doGET();
    }

    public void setNickname(long uid, String nickname, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/users/%d/nickname", uid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .addParam("nickname", nickname).setAuth(mAuth).doPUT();
    }

    public void setEmail(long uid, String email, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/users/%d/email", uid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .addParam("email", email).setAuth(mAuth).doPUT();
    }

    public void setPassword(long uid, String password, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/users/%d/password", uid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .addParam("password", password).setAuth(mAuth).doPUT();
    }

    public void getFavoriteTags(long uid, Handler<JsonStruct.FavTagsList> handler) {
        URL url = urlFor("/users/%d/tags", uid);
        AsyncRequest.of(url, handler, JsonStruct.FavTagsList.class)
                .setAuth(mAuth).doGET();
    }

    public void updatePreference(long uid, String pref, Handler<JsonStruct.Success> handler) {
        Log.d("PREF", "update preference");
        URL url = urlFor("/users/%d/pref", uid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .addParam("pref", pref).setAuth(mAuth).doPUT();
    }

    public void joinGroup(long uid, long gid, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/users/%d/group", uid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .addParam("gid", gid).setAuth(mAuth).doPUT();
    }

    public void joinProtectedGroup(long uid, long gid,
            String password, Handler<JsonStruct.Success> handler) {
        Log.d(TAG, "joining group protected and using password: " + password);
        URL url = urlFor("/users/%d/group", uid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .addParam("gid", gid)
                .addParam("password", password).setAuth(mAuth).doPUT();
    }

    public void leaveGroup(long uid, long gid, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/users/%d/group?gid=%d", uid, gid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .setAuth(mAuth).doDELETE();
    }

    public void listGroups(Handler<JsonStruct.GroupsList> handler) {
        URL url = urlFor("/groups");
        AsyncRequest.of(url, handler, JsonStruct.GroupsList.class)
                .setAuth(mAuth).doGET();
    }

    public void getSuggestion(double lat, double lon,
            Handler<JsonStruct.GroupSuggestion> handler) {
        URL url = urlFor(String.format("/groups/suggestion?lat=%f&lon=%f", lat, lon));
        AsyncRequest.of(url, handler, JsonStruct.GroupSuggestion.class).setAuth(mAuth).doGET();
    }

    public void listGroups(double lat, double lon, Handler<JsonStruct.GroupsList> handler) {
        URL url = urlFor("/groups?lat=%f&lon=%f", lat, lon);
        AsyncRequest.of(url, handler, JsonStruct.GroupsList.class)
                .setAuth(mAuth).doGET();
    }

    public void setGroupPassword(long gid, String password, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/groups/%d", gid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .addParam("password", password)
                .setAuth(mAuth).doPUT();
    }

    public void getGroupListAfterCreateGroup(String name, double lat, double lon,
            Handler<JsonStruct.GroupsList> handler) {
        URL url = urlFor("/groups");
        AsyncRequest.of(url, handler, JsonStruct.GroupsList.class)
                .addParam("name", name).addParam("lat", lat)
                .addParam("lon", lon).addParam("list", true).setAuth(mAuth).doPOST();
    }

    public void createGroup(String name, double lat, double lon,
            Handler<JsonStruct.Group> handler) {
        URL url = urlFor("/groups");
        AsyncRequest.of(url, handler, JsonStruct.Group.class)
                .addParam("name", name).addParam("lat", lat)
                .addParam("lon", lon).addParam("list", false).setAuth(mAuth).doPOST();
    }

    public void getGroupInfo(long gid, Handler<JsonStruct.Group> handler) {
        URL url = urlFor("/groups/%d", gid);
        AsyncRequest.of(url, handler, JsonStruct.Group.class)
                .setAuth(mAuth).doGET();
    }

    public void getNextTracks(long gid, Handler<JsonStruct.TracksList> handler) {
        URL url = urlFor("/groups/%d/tracks", gid);
        AsyncRequest.of(url, handler, JsonStruct.TracksList.class)
                .setAuth(mAuth).doGET();
    }

    public void getPlaylistId(long gid, final Handler<JsonStruct.TracksList> handler) {
        URL url = urlFor("/groups/%d/playlist", gid);
        AsyncRequest.of(url, handler, JsonStruct.TracksList.class)
                .setAuth(mAuth).doGET();
    }

    public void setCurrentTrack(long gid, String artist, String title,
            Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/groups/%d/current", gid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .addParam("artist", artist).addParam("title", title)
                .setAuth(mAuth).doPUT();
    }

    public void skipTrack(long gid, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/groups/%d/current", gid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .setAuth(mAuth).doDELETE();
    }

    public void instantRate(long gid, String artist, String title, int rating,
            Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/groups/%d/ratings", gid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .addParam("artist", artist).addParam("title", title).addParam("rating", rating)
                .setAuth(mAuth).doPOST();
    }

    public void becomeMaster(long gid, long uid, double lat, double lon,
            Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/groups/%d/master", gid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .addParam("uid", uid).addParam("lat", lat)
                .addParam("lon", lon).setAuth(mAuth).doPUT();
    }

    public void resignMaster(long gid, long uid, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/groups/%d/master", gid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .setAuth(mAuth).doDELETE();
    }

    public Request.Result<JsonStruct.Success> uploadLibrarySync(
            long uid, Iterable<JsonStruct.Track> tracks) {
        URL url = urlFor("/libentries/%d", uid);
        Request<JsonStruct.Success> request = Request.of(url, JsonStruct.Success.class)
                .setAuth(mAuth);
        for (JsonStruct.Track track : tracks) {
            request.addParam("entry", GSON.toJson(track));
        }
        return request.doPUT();
    }

    public Request.Result<JsonStruct.Success> updateLibrarySync(
            long uid, Iterable<JsonStruct.Delta> deltas) {
        URL url = urlFor("/libentries/%d/batch", uid);
        Request<JsonStruct.Success> request = Request.of(url, JsonStruct.Success.class)
                .setAuth(mAuth);
        for (JsonStruct.Delta delta : deltas) {
            request.addParam("delta", GSON.toJson(delta));
        }
        return request.doPOST();
    }

    public void getRatings(long uid, Handler<JsonStruct.TracksList> handler) {
        URL url = urlFor("/libentries/%d/ratings", uid);
        AsyncRequest.of(url, handler, JsonStruct.TracksList.class)
                .setAuth(mAuth).doGET();
    }

    public void rate(long uid, String artist, String title, int rating,
            Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/libentries/%d/ratings", uid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class).setAuth(mAuth)
                .addParam("artist", artist).addParam("title", title)
                .addParam("rating", rating).doPOST();
    }

    private static URL urlFor(String suffix, Object... objects) {
        try {
            return new URL(API_ROOT + String.format(suffix, objects));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Solo mode
     */

    // ---------------
    // PLAYLISTS

    public void generatePlaylist(long uid, JSONObject seeds, JSONObject options,
            Handler<JsonStruct.PlaylistJS> handler) {
        Log.i(TAG, "Ready to get!");
        if (seeds != null) {
            URL url = urlFor("/solo/%d/playlist", uid);
            AsyncRequest.of(url, handler, JsonStruct.PlaylistJS.class)
                    .setAuth(mAuth).addParam("seeds", seeds).addParam("options", options).doPOST();
        } else {
            throw new IllegalArgumentException();
        }

    }

    public void updatePlaylist(long uid, long plid, JSONObject fields,
            Handler<JsonStruct.PlaylistJS> handler) {
        URL url = urlFor("/solo/%d/playlist/%d", uid, plid);
        AsyncRequest.of(url, handler, JsonStruct.PlaylistJS.class)
                .setAuth(mAuth).addParam("fields", fields).doPOST();
    }

    public void listUserPlaylists(long uid, Handler<JsonStruct.PlaylistsList> handler) {
        URL url = urlFor("/solo/%d/playlists", uid);
        AsyncRequest.of(url, handler, JsonStruct.PlaylistsList.class)
                .setAuth(mAuth).doGET();
    }

    public void listSharedPlaylists(Handler<JsonStruct.PlaylistsList> handler) {
        URL url = urlFor("/solo/playlists/shared");
        // TODO
        AsyncRequest.of(url, handler, JsonStruct.PlaylistsList.class).setAuth(mAuth).doGET();
    }

    public void removePlaylist(long uid, long plid, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/solo/%d/playlist/%d", uid, plid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .setAuth(mAuth).doDELETE();
    }

    public void resetPassword(String email, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/resetpw");
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .addParam("email", email).doPOST();
    }

    // ---------------
    // TAGS

    public void listTopTags(long uid, Handler<JsonStruct.TagsList> handler) {
        URL url = urlFor("/solo/tags/top", uid);
        AsyncRequest.of(url, handler, JsonStruct.TagsList.class)
                .setAuth(mAuth).doGET();
    }

    /** Simple interface for async calls. */
    public interface Handler<S> {
        void callback(S struct);

        void onError(Error error);
    }

    /** Container for error messages & related information. */
    public static class Error {

        public static final int STATUS_FORBIDDEN = 403;
        public static final int STATUS_INTERNAL_ERROR = 500;
        public static final int STATUS_NOT_FOUND = 404;

        public final int statusCode;
        public final String statusMessage;
        public final String response;

        public final Throwable error;
        public final JsonStruct.Error jsonError;

        public Error(int sCode, String sMessage, String resp, JsonStruct.Error jsonErr) {
            this(sCode, sMessage, resp, jsonErr, null);
        }

        public Error(int sCode, String sMessage, String resp, Throwable err) {
            this(sCode, sMessage, resp, null, err);
        }

        public Error(int sCode, String sMessage, String resp,
                JsonStruct.Error jsonErr, Throwable err) {
            statusCode = sCode;
            statusMessage = sMessage;
            response = resp;
            jsonError = jsonErr;
            error = err;
        }

        public boolean hasJsonError() {
            return jsonError != null;
        }

        @Override
        public String toString() {
            if (hasJsonError() && jsonError.error != null && jsonError.message != null) {
                return String.format("JSON error:\ncode: %d\nmessage: %s",
                        jsonError.error, jsonError.message);
            } else if (error != null && response != null) {
                return String.format("Error type: %s\nstatus: %d\nresponse: %s",
                        error.getClass().toString(), statusCode, response);
            } else {
                return "We got an error without anything to log";
            }
        }
    }

    /** Corresponds to JSON error codes - synced with back-end. (not so much) */
    public static final class ErrorCodes {
        public static final int MISSING_FIELD = 1;
        public static final int EXISTING_USER = 2;
        public static final int INVALID_EMAIL = 4;
        public static final int INVALID_PASSWORD = 5;
        public static final int INVALID_GROUP = 6;
        public static final int INVALID_TRACK = 0x07;
        public static final int INVALID_LIBENTRY = 0x08;
        public static final int INVALID_DELTA = 0x09;
        public static final int UNAUTHORIZED = 0x0a;
        public static final int TRACKS_DEPLETED = 0x0b;
        public static final int MASTER_TAKEN = 0x0c;
        public static final int NO_CURRENT_TRACK = 0x0d;
        // Added by Vincent:
        public static final int MISSING_CLUSTER = 0x0e;
        public static final int FORBIDDEN = 0x0f;
        // public static final int GROUP_JOIN_FORBIDDEN = 15;
        public static final int PASSWORD_EXPECTED = 0x10;
        // Added by Marc:
        public static final int IS_EMPTY = 32;
        public static final int OPERATION_FAILED = 0x21;
        public static final int NO_TAGGED_TRACKS = 0x22;

    }

}
