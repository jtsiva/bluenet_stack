package nd.edu.bluenet_stack;

import java.util.*;
import java.sql.Timestamp;

/**
 * This class maintains a history of coordinates along with the timestamp
 * of the last update.
 * 
 * @author Josh Siva
 * @see Coordinate
 */
public class LocationEntry {
	public static final int WINDOW_LENGTH = 5;
	public List<Coordinate> mCoordinates = new ArrayList<Coordinate>();
	public Timestamp mTimestamp;
	public Timestamp mLastForward;

	/**
	 * Default constructor which initializes timestamps to null
	 */
	public LocationEntry() {
		mTimestamp = null;
		mLastForward = null;
	}

	/**
	 * Constructor that sets an initial coordinate
	 * 
	 * @param lat initial latitude to set
	 * @param lon initial longitude to set
	 */
	public LocationEntry(float lat, float lon) {
		Coordinate coord = new Coordinate(lat, lon);
		mCoordinates.add(coord);
	}

	/**
	 * Add a new coordinate to the list (but keep within window) and update
	 * timestamp
	 * 
	 * @param lat new latitude
	 * @param lon new longitude
	 */
	public void update(float lat, float lon) {
		if (mCoordinates.size() == WINDOW_LENGTH) {
			mCoordinates.remove(0);
		}

		mCoordinates.add(new Coordinate(lat, lon));

		mTimestamp = new Timestamp(System.currentTimeMillis());
	}
}