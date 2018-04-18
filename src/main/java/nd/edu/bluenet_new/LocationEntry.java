package nd.edu.bluenet_new;


import java.sql.Timestamp;

public class LocationEntry {
	public float mLatitude;
	public float mLongitude;
	public Timestamp mTimestamp;

	public LocationEntry() {
		mLatitude = 0.0f;
		mLongitude = 0.0f;
		mTimestamp = null;
	}

	public LocationEntry(float lat, float lon, Timestamp timestamp) {
		mLatitude = lat;
		mLongitude = lon;
		mTimestamp = timestamp;
	}
}