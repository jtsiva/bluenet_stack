package nd.edu.bluenet_stack;

import java.util.*;
import java.sql.Timestamp;

public class GroupEntry {
	public Group mGroup;
	public Timestamp mTimestamp;

	public GroupEntry() {
		mGroup = null;
		mTimestamp = null;
	}

	public GroupEntry(Group grp) {
		update(grp);
	}

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

	public void update(Group grp) {
		mGroup = grp;
		mTimestamp = new Timestamp(System.currentTimeMillis());
	}

	public void update() {
		mTimestamp = new Timestamp(System.currentTimeMillis());
	}
}