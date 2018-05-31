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
	 * @return Coordinate object with latitude and longitude initialized to (0.0,0.0)
	 */
	public Coordinate() {
		mLatitude = 0.0f;
		mLongitude = 0.0f;
	}

	/**
	 * @param lat latitude to set
	 * @param lon longitude to set
	 * @return Coordinate object with latitude and longitude initialized to passed in values
	 */
	public Coordinate(float lat, float lon) {
		mLatitude = lat;
		mLongitude = lon;
	}
}