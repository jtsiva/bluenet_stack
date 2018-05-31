package nd.edu.bluenet_stack;

import java.util.*;
import java.nio.charset.StandardCharsets;

/**
 * A Group is a way of providing multicast (or pub/sub). A group is
 * addressed in the exact same way as a node. A node may join as many groups 
 * as it likes. The general idea is that while a node is apart of a group, any 
 * message intended for that group will be seen by the node (rather than 
 * ignored)
 * 
 * @author Josh Siva
 * @see GeoGroup
 * @see NamedGroup
 */
public class Group {
	public final static String BROADCAST_GROUP = "0000";

	public final static int NONE = -1;
	public final static int NAMED_GROUP = 0;
	public final static int GEO_GROUP = 1;
	public final static int SECURE_GROUP = 2;


	private String mID;
	private int mType;
	private boolean mJoined;

	/**
	 * Initialize a default group as the special broadcast group
	 */
	public Group() {
		mID = BROADCAST_GROUP;
		mType = NONE;
		mJoined = false;
	}

	/**
	 * @param id alphanumeric BlueNet ID
	 * @param type integer representing the group type
	 */
	public Group (String id, int type) {
		mID = id;
		mType = type;
		mJoined = false;
	}

	/**
	 * Override equals so that we can tell when two groups are equal. Useful
	 * for when we want to compare group tables between nodes
	 * 
	 * @param obj Group or obj against which to compare
	 * @return true if the IDs match, false otherwise
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

        Group other = (Group)obj;
        if (!Objects.equals(mID, new String(other.getID()))) {
            return false;
        }

        return true;
    }

    /**
     * @return the ID of the group as a byte array
     */
	public byte[] getID () {
		return mID.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * @return this groups type
	 */
	public int getType () {
		return mType;
	}

	/**
	 * @return true if this group has been joined, false otherwise
	 */
	public boolean getStatus() {
		return mJoined;
	}

	/**
	 * set the status of this group to joined
	 */
	public void join() {
		mJoined = true;
	}

	/**
	 * set the status of this group as not joined
	 */
	public void leave() {
		mJoined = false;
	}
	

}