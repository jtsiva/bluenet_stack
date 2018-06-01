package nd.edu.bluenet_stack;

/**
 * Multicast/subscription group with an identifying name. Note that there are
 * no restrictions on naming. In some ways this is more of a convenience class
 * that provides a more human-friendly identification for the group with the
 * benefit that it is agreed upon by all (name assignment does not need to be 
 * handled at the app layer as would be the case with using the basic Group).
 * 
 * @author Josh Siva
 * @see Group
 */
public class NamedGroup extends Group {
	private String mName;

	/**
	 * @param id alphanumeric BlueNet ID with which to initialize the group
	 * @param name to be given to the group
	 */
	public NamedGroup(String id, String name) {
		super (id, Group.NAMED_GROUP);
		mName = name;
	}

	/**
	 * Initialize group with blank name
	 * 
	 * @param id alphanumeric BlueNet ID with which to initialize the group
	 */
	public NamedGroup(String id) {
		this (id, "");
	}

	/**
	 * @return the name given to the group
	 */
	public String getName() {
		return mName;
	}


}