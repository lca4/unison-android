
package ch.epfl.unison.data;

/**
 * Simple POJO used to store, compare and process a track. It is represented by
 * a local ID that allows to play the file, an artist and a title.
 * 
 * @author lum
 */
public class TrackItem extends AbstractItem<TrackItem> {

    public final long localId;
    public final String artist;
    public final String title;
    public final long playOrder;

    public TrackItem(long id, String a, String t) {
        localId = id;
        artist = a;
        title = t;
        playOrder = -1;
    }

    public TrackItem(long id, String a, String t, long o) {
        localId = id;
        artist = a;
        title = t;
        playOrder = o;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        long result = 1;
        result = prime * result;
        if (artist != null) {
            result += artist.hashCode();
        }
        result = prime * result + localId;
        result = prime * result;
        if (title != null) {
            result += title.hashCode();
        }
        return (int) result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TrackItem other = (TrackItem) obj;
        if (artist == null) {
            if (other.artist != null) {
                return false;
            }
        } else if (!artist.equals(other.artist)) {
            return false;
        }
        if (localId != other.localId) {
            return false;
        }
        if (title == null) {
            if (other.title != null) {
                return false;
            }
        } else if (!title.equals(other.title)) {
            return false;
        }
        return true;
    }

    public int compareTo(TrackItem another, boolean withRespectToPlayOrder) {
        int comparison = this.compareTo(another);
        if (comparison != 0 || !withRespectToPlayOrder) {
            return comparison;
        }
        if (playOrder < another.playOrder) {
            return -1;
        } else if (playOrder > another.playOrder) {
            return 1;
        }
        return 0;
    }

    @Override
    public int compareTo(TrackItem another) {
        TrackItem musicItem = (TrackItem) another;
        int artistComp = artist.compareTo(musicItem.artist);
        if (artistComp != 0) {
            return artistComp;
        }
        int titleComp = title.compareTo(musicItem.title);
        if (titleComp != 0) {
            return titleComp;
        }
        if (localId < musicItem.localId) {
            return -1;
        } else if (localId > musicItem.localId) {
            return 1;
        }
        return 0;
    }
}