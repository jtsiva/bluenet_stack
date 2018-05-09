package nd.edu.bluenet_new;

public class Group {
	public final static String BROADCAST_GROUP = "0000";

	public final static int NAMED_GROUP = 0;
	public final static int GEO_GROUP = 1;
	public final static int SECURE_GROUP = 2;


	private String mID;
	private int mType;
	private boolean mJoined;

	public Group (String id, int type) {
		mID = id;
		mType = type;
		mJoined = false;
	}

	public byte[] getID () {
		return mID.getBytes(StandardCharsets.UTF_8);
	}

	public int getType () {
		return mType;
	}

	public boolean getStatus() {
		return mJoined;
	}

	public void join() {
		mJoined = true;
	}

	public void leave() {
		mJoined = false;
	}
	

}