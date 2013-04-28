package ch.epfl.unison.api;

import ch.epfl.unison.data.TagItem;
import ch.epfl.unison.Playlist;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;

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
    public static class Group extends JsonStruct implements Serializable {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
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
    public static class PlaylistJS extends JsonStruct {

        public Long id;
        public String title;
        public Calendar created;
        public Calendar updated;
        public String image; // Not used for now
        public Integer authorId;
        //public String authorName; //TODO
        public Integer size;
        public Track[] tracks;
        public Integer rating;
        public String comment;
        public Integer listeners;
        public Double avgRating;
        public Boolean shared;
        public Boolean synced;
        
        public Playlist toObject() {
            //TODO complete
            return new Playlist.Builder().id(id).title(title).tracks(tracks).build();
        }
        
    }

    /** List of playlists. */
    public static class PlaylistsList extends JsonStruct {

        public PlaylistJS[] playlists;
        
        public ArrayList<Playlist> toObject() {
            ArrayList<Playlist> al = new ArrayList<Playlist>();
            for (int i = 0; i < playlists.length; i++) {
                al.add(playlists[i].toObject());
            }
            return al;
        }
    }
    
    /** Information about a tag (used in both directions). */
    public static class Tag extends JsonStruct {

//        public int tid;
        public String name;
        public Long refId;
        
        public TagItem getTagItem() {
            return new TagItem(name, refId);
        }
    }

    /** List of tags. */
    public static class TagsList extends JsonStruct {

        public Tag[] tags;
    }
}
