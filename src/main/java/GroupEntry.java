package nd.edu.bluenet_stack;

import java.util.*;
import java.sql.Timestamp;

/**
 * This class captures group information as well as when that group has been
 * updated. Useful for tracking the staleness of a group
 * 
 * @author Josh Siva
 * @see Group
 */
public class GroupEntry {
	public Group mGroup;
	public Timestamp mTimestamp;

	/**
	 * set the group and timestamp to null by default
	 */
	public GroupEntry() {
		mGroup = null;
		mTimestamp = null;
	}

	/**
	 * Initialize the group and set the timestamp
	 * 
	 * @param grp the group used to initialize the entry
	 */
	public GroupEntry(Group grp) {
		update(grp);
	}

	/**
	 * Check whether the groups are equal
	 * 
	 * @param obj the group or object to check for equality
	 * @return true if the groups are equal, false otherwise
	 * @see Group
	 */
	@Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()){
            return false;
        }

        GroupEntry other = (GroupEntry)obj;
        if (!Objects.equals(mGroup, other.mGroup)) {
            return false;
        }

        return true;
    }

    /**
     * Update the group in the entry and update the timestamp to now
     * 
     * @param grp new group to which the group in this entry is set
     */
	public void update(Group grp) {
		mGroup = grp;
		mTimestamp = new Timestamp(System.currentTimeMillis());
	}

	/**
	 * Update the timestamp for the group entry to now.
	 */
	public void update() {
		mTimestamp = new Timestamp(System.currentTimeMillis());
	}
}