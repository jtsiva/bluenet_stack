package nd.edu.bluenet_stack;

import java.util.*;
import java.sql.Timestamp;

public class LocationEntry {
	public static final int WINDOW_LENGTH = 5;
	public List<Coordinate> mCoordinates = new ArrayList<Coordinate>();
	public Timestamp mTimestamp;
	public Timestamp mLastForward;

	public LocationEntry() {
		mTimestamp = null;
		mLastForward = null;
	}

	public LocationEntry(float lat, float lon) {
		Coordinate coord = new Coordinate(lat, lon);
		mCoordinates.add(coord);
	}

	public void update(float lat, float lon) {
		if (mCoordinates.size() == WINDOW_LENGTH) {
			mCoordinates.remove(0);
		}

		mCoordinates.add(new Coordinate(lat, lon));

		mTimestamp = new Timestamp(System.currentTimeMillis());
	}
}