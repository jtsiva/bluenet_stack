package nd.edu.bluenet_new;

public class NamedGroup extends Group {
	private String mName;

	public NamedGroup(String id, String name) {
		super (id, Group.NAMED_GROUP);
		mName = name;
	}

	public NamedGroup(String id) {
		NamedGroup (id, "");
	}

	public getName() {
		return mName;
	}


}