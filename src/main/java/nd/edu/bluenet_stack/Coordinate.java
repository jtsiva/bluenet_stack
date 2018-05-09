package nd.edu.bluenet_stack;

public class Coordinate {
	public float mLatitude;
	public float mLongitude;

	public Coordinate() {
		mLatitude = 0.0f;
		mLongitude = 0.0f;
	}

	public Coordinate(float lat, float lon) {
		mLatitude = lat;
		mLongitude = lon;
	}
}