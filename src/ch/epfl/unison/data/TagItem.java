package ch.epfl.unison.data;

/**
 * 
 * @author marc
 *
 */
public class TagItem implements Comparable<TagItem> {
    
    public final int localId;
    public final String name;
    public final Long remoteId; // id on GS database, hash in fact
    
    public TagItem(int lid, String n, Long rid) {
        localId = lid;
        name = n;
        remoteId = rid;
    }

    @Override
    public int compareTo(TagItem another) {
        int nameComp = name.compareTo(another.name);
        if (nameComp != 0) {
            return nameComp;
        }
        if (localId < another.localId) {
            return -1;
        } else if (localId > another.localId) {
            return 1;
        }
        if (localId < another.localId) {
            return -1;
        } else if (localId > another.localId) {
            return 1;
        }
        return 0;
    }

}
