package nd.edu.bluenet_stack;


import java.sql.Timestamp;

public class LocationEntry {
	public float mLatitude;
	public float mLongitude;
	public Timestamp mTimestamp;
	public Timestamp mLastForward;

	public LocationEntry() {
		mLatitude = 0.0f;
		mLongitude = 0.0f;
		mTimestamp = null;
		mLastForward = null;
	}

	public LocationEntry(float lat, float lon, Timestamp timestamp) {
		mLatitude = lat;
		mLongitude = lon;
		mTimestamp = timestamp;
	}
}