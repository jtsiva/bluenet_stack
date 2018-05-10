package nd.edu.bluenet_stack;

public class NamedGroup extends Group {
	private String mName;

	public NamedGroup(String id, String name) {
		super (id, Group.NAMED_GROUP);
		mName = name;
	}

	public NamedGroup(String id) {
		this (id, "");
	}

	public String getName() {
		return mName;
	}


}