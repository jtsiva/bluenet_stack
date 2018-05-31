package nd.edu.bluenet_stack;

/**
 * A type of group that is based on a location. The group consists of any node
 * that lies within a certain radius of a set of coordinates.
 * 
 * @author Josh Siva
 * @see Group
 * @see LocationManager
 */
public class GeoGroup extends Group{
	private float mLatitude;
	private float mLongitude;
	private float mRadius;

	/**
	 * Initialize geographic group with coordinates plus a distance from those
	 * coordinates.
	 * 
	 * @param id alphanumeric BlueNet ID assigned to this group
	 * @param lat latitude of the center of this group
	 * @param lon longitude of the center of this group
	 * @param rad radius (in meters) from the center of the group
	 * 		  that is considered within the group
	 */
	public GeoGroup(String id, float lat, float lon, float rad) {
		super(id, Group.GEO_GROUP);
		mLatitude = lat;
		mLongitude = lon;
		mRadius = rad;
	}

	public GeoGroup(String id) {
		this (id, 0.0f, 0.0f, 0.0f);
	}

	/**
	 * @return latitude of center of group
	 */
	public float getLatitude () {
		return mLatitude;
	}

	/**
	 * @return longitude of center of group
	 */
	public float getLongitude () {
		return mLongitude;
	}

	/**
	 * @return radius from center of group considered within the group
	 */
	public float getRadius () {
		return mRadius;
	}

	/**
	 * Try to join the geographic group given a latitude and longitude. If the
	 * coordinates are within the group then the group is joined, otherwise the
	 * group is left (which does nothing if the group wasn't joined in the first
	 * place).
	 * 
	 * @param latitude of the node
	 * @param longitude of the node
	 * @see LocationManager
	 */
	public void join(float latitude, float longitude) {
		//will join if within radius of geogroup
		double dist = LocationManager.distance(mLatitude, mLongitude, latitude, longitude);

		if (dist < mRadius) {
			super.join();
		}
		else {
			super.leave();
		}
	}
}