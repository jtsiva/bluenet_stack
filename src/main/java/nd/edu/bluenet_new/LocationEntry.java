package nd.edu.bluenet_new;


import java.sql.Timestamp;

public class LocationEntry {
	public double mLatitude;
	public double mLongitude;
	public Timestamp mTimestamp;

	public LocationEntry() {
		mLatitude = 0.0;
		mLongitude = 0.0;
		mTimestamp = null;
	}

	public LocationEntry(double lat, double lon, Timestamp timestamp) {
		mLatitude = lat;
		mLongitude = lon;
		mTimestamp = timestamp;
	}
}