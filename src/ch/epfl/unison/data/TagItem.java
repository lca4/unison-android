package ch.epfl.unison.data;

/**
 * 
 * @author marc
 *
 */
public class TagItem implements Comparable<TagItem> {
    
    public final int localId;
    public final String name;
    public boolean isChecked;
    public final Long remoteId; // id on GS database, hash in fact
    
    public TagItem(String n, Long rid) {
        this.localId = -1;
        this.name = n;
        this.isChecked = false;
        this.remoteId = rid;
    }
    
    public TagItem(int lid, String n, int checked, Long rid) {
        this.localId = lid;
        this.name = n;
        if (checked == 0) {
            this.isChecked = false;
        } else {
            this.isChecked = true;
        }
        this.remoteId = rid;
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
