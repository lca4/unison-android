package ch.epfl.unison.data;

/**
 * Simple POJO used to store, compare and process a track. It is represented
 * by a local ID that allows to play the file, an artist and a title.
 *
 * @author lum
 */
public class MusicItem implements Comparable<MusicItem> {

    public final int localId;
    public final String artist;
    public final String title;
    public final int playOrder;

    public MusicItem(int id, String a, String t) {
        localId = id;
        artist = a;
        title = t;
        playOrder = -1;
    }

    public MusicItem(int id, String a, String t, int o) {
        localId = id;
        artist = a;
        title = t;
        playOrder = o;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result;
        if (artist != null) {
            result += artist.hashCode();
        }
        result = prime * result + localId;
        result = prime * result;
        if (title != null) {
            result += title.hashCode();
        }
        return result;
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
        MusicItem other = (MusicItem) obj;
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

    @Override
    public int compareTo(MusicItem another) {
        int artistComp = artist.compareTo(another.artist);
        if (artistComp != 0) {
            return artistComp;
        }
        int titleComp = title.compareTo(another.title);
        if (titleComp != 0) {
            return titleComp;
        }
        if (localId < another.localId) {
            return -1;
        } else if (localId > another.localId) {
            return 1;
        }
        return 0;
    }

}
