package ch.epfl.unison.api;

/**
 * POJOs for JSON serialization / deserialization.
 *
 * @author lum
 */
public abstract class JsonStruct {

    /** Error message from the server. */
    public static class Error extends JsonStruct {

        public Integer error;
        public String message;
    }

    /** Success message from the server. */
    public static class Success extends JsonStruct {

        public Boolean success;
    }

    /** Information about a user (used in both directions). */
    public static class User extends JsonStruct {

        public Long uid;
        public String nickname;
        public String email;
        public String password;
        public Long gid;
        public Integer score;
        public Boolean predicted;  // or isPredicted?
    }

    /** Information about a track (used in both directions). */
    public static class Track extends JsonStruct {

        public String artist;
        public String title;
        public String image;
        public Integer localId;
        public Integer rating;

        public Track() { }

        public Track(int id, String a, String t) {
            localId = id;
            artist = a;
            title = t;
        }
    }

    /** Information about a playlist (sent by server). */
    public static class TracksList extends JsonStruct {

        public String playlistId;
        public Track[] tracks;
    }

    /** Information about a group (used in both directions). */
    public static class Group extends JsonStruct {

        public Long gid;
        public String name;
        public Track track;
        public Float distance;
        public User master;
        public User[] users;
        public Integer nbUsers;
    }

    /** List of groups. */
    public static class GroupsList extends JsonStruct {

        public Group[] groups;
    }

    /** Notification of addition / removal of track on the device. */
    public static class Delta extends JsonStruct {

        public static final String TYPE_PUT = "PUT";
        public static final String TYPE_DELETE = "DELETE";

        public String type;
        public Track entry;

        public Delta() { }

        public Delta(String t, int localId, String artist, String title) {
            this.type = t;
            this.entry = new Track(localId, artist, title);
        }
    }
    
    /** Information about a playlist (used in both directions). */
    public static class Playlist extends JsonStruct {

        public Long plid;
        public String name;
        //TODO complete
        public Integer size;
        public Integer listeners;
    }

    /** List of groups. */
    public static class PlaylistsList extends JsonStruct {

        public Playlist[] playlists;
    }
}
