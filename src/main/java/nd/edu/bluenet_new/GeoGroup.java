package nd.edu.bluenet_new;

public class GeoGroup {
	private float mLatitude;
	private float mLongitude;
	private float mRadius;

	public GeoGroup(String id, float lat, float lon, float rad) {
		super(id, Group.GEO_GROUP);
		mLatitude = lat;
		mLongitude = lon;
		mRadius = rad;
	}

	public GeoGroup(String id) {
		GeoGroup (id, 0.0f, 0.0f, 0.0f);
	}

	public float getLatitude () {
		return mLatitude;
	}

	public float getLongitude () {
		return mLongitude;
	}

	public float getRadius () {
		return mRadius;
	}

	public void join(float latitude, float longitude) {
		//will join if within radius of geogroup
		double dist = LocationManager.distance(mLatitude, mLongitude, latitude, longitude);

		if (dist < mRadius) {
			join();
		}
		else {
			leave();
		}
	}
}