package nd.edu.bluenet_stack;

/**
 * Object that nicely encapsulates latitude and longitude together
 * 
 * @author Josh siva
 */
public class Coordinate {
	public float mLatitude;
	public float mLongitude;

	/**
	 * Initialize latitude and longitude to (0.0,0.0)
	 */
	public Coordinate() {
		mLatitude = 0.0f;
		mLongitude = 0.0f;
	}

	/**
	 * initialize coordinate to given latitude and longitude
	 * 
	 * @param lat latitude to set
	 * @param lon longitude to set
	 */
	public Coordinate(float lat, float lon) {
		mLatitude = lat;
		mLongitude = lon;
	}
}