package nd.edu.bluenet_stack;

import java.util.*;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import static java.lang.Math.sqrt;

public class LocationManager implements LayerIFace {
	protected HashMap<String, LocationEntry> mIDLocationTable = null;
	protected Reader mReadCB;
	protected Writer mWriteCB;
	protected Query mQueryCB;

	private String mID;

	public LocationManager() {
		mIDLocationTable = new HashMap<String, LocationEntry>();
		mReadCB = null;
		mWriteCB = null;
	}

	// https://stackoverflow.com/questions/14308746/how-to-convert-from-a-float-to-4-bytes-in-java
	private void sendLocation() {
		LocationEntry myLoc = mIDLocationTable.get(mID);
		if (myLoc != null) {
			AdvertisementPayload advPayload = new AdvertisementPayload();

			byte[] lat = ByteBuffer.allocate(4).putFloat(getLocation(mID).mLatitude).array();
			byte[] lon = ByteBuffer.allocate(4).putFloat(getLocation(mID).mLongitude).array();
			//group checksum

			//Query for standard header? final in relevant class?

			byte[] allBytes = new byte[lat.length + lon.length + 2];
			System.arraycopy(lat, 0, allBytes,0,lat.length);
			System.arraycopy(lon, 0, allBytes,lat.length,lon.length);


			//msg.fromBytes(allBytes);
			advPayload.setMsg (allBytes);

			String result = mQueryCB.ask("GrpMgr", "getCheckSum");
			byte [] chksum = result.getBytes();

			System.arraycopy(chksum, 0, allBytes, lat.length+lon.length, chksum.length);

			advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
			advPayload.setSrcID(mID);

			advPayload.setDestID(Group.BROADCAST_GROUP); //not sure if this really matters

			if (this.shouldPass(advPayload)) {
				mWriteCB.write(advPayload);
			}

		}
	}

	private int updateLocation(String id, float lat, float lon) {
		LocationEntry loc = mIDLocationTable.get(id);

		if (loc == null) {
			loc = new LocationEntry();
		}
		
		loc.update(lat, lon);

		mIDLocationTable.put(id, loc);

		return 0;
	}

	private int updateLocation(String id, byte[] message) {
		byte [] latBytes = Arrays.copyOfRange(message,0,4);
		byte [] lonBytes = Arrays.copyOfRange(message,4,8);

		float lat =  ByteBuffer.wrap(latBytes).getFloat();
		float lon =  ByteBuffer.wrap(lonBytes).getFloat();

		System.out.println("Updating: " + id);

		return this.updateLocation(id, lat, lon);
	}


	private Coordinate getLocation(String id) {
		// provide location centroid of given node id
		LocationEntry entry = mIDLocationTable.get(id);
		Coordinate coordAvg = new Coordinate();

		for (Coordinate coord : entry.mCoordinates) {
			coordAvg.mLatitude += coord.mLatitude;
			coordAvg.mLongitude += coord.mLongitude;
		}

		if (entry.mCoordinates.size() > 0) {
			coordAvg.mLatitude /= entry.mCoordinates.size();
			coordAvg.mLongitude /= entry.mCoordinates.size();
		}


		return coordAvg;
	}

	//TODO: move the two routing related things below to Routing Layer

	private boolean inDirection(String srcID, String destID) {
		//used to answer query about whether this node is in the 
		//correct direction
		Coordinate srcLoc = getLocation(srcID);
		Coordinate destLoc = getLocation(destID);
		Coordinate myLoc = getLocation(mID);

		boolean result;
		if (srcLoc == null || destLoc == null || myLoc == null) {
			result = true; //we don't know otherwise
		}
		else {
			double srcToDestDist = LocationManager.distance(srcLoc.mLatitude, srcLoc.mLongitude, destLoc.mLatitude, destLoc.mLongitude);
			double meToDestDist = LocationManager.distance(myLoc.mLatitude, myLoc.mLongitude, destLoc.mLatitude, destLoc.mLongitude);
			
			// System.out.println(srcToDestDist);
			// System.out.println(meToDestDist);
			result = meToDestDist < srcToDestDist;
		}

		return result;
	}

	private boolean shouldPass(AdvertisementPayload advPayload) {
		/*
			Implement policy to determine whether the location update from
			the given ID should be forwarded. We do not want a broadcast
			storm of location updates.

			See protocol proposal in OSF

			The distance thresholds will likely need to be adjusted 

		*/

		final byte ME = 4; //hops
		final float ME_D_THRESHOLD = 1.0f; //meters
		final byte NEAR = 3;
		final float NEAR_D_THRESHOLD = 3.0f;
		final byte MEDIUM = 2;
		final float MEDIUM_D_THRESHOLD = 7.0f;
		final byte FAR = 1;
		final float FAR_D_THRESHOLD = 13.0f;

		String id = new String(advPayload.getSrcID());
		byte ttl = advPayload.getTTL();
		LocationEntry entry = mIDLocationTable.get(id);
		float d = applyError(positionSpread(id), entry.mLastForward);
		boolean result = false;

		if (ME == ttl) {
			result = d > ME_D_THRESHOLD;
		}
		else if (NEAR == ttl) {
			result = d > NEAR_D_THRESHOLD;
		}
		else if (MEDIUM == ttl) {
			result = d > MEDIUM_D_THRESHOLD;
		}
		else if (FAR == ttl) {
			result = d > FAR_D_THRESHOLD;
		}
	
		if (result || null == entry.mLastForward) {
			entry.mLastForward = new Timestamp(System.currentTimeMillis());
			mIDLocationTable.put(id, entry);
		}

		return result;
	}

	private boolean sufficientData(String id) {
		LocationEntry entry = mIDLocationTable.get(id);
		return entry.mCoordinates.size() >= 2;
	}

	private float positionSpread(String id) {
		float d = 0.0f;
		if (sufficientData(id)) {
			d = meanSquaredDisplacement(id);
		}

		return d;
	}

	private float meanSquaredDisplacement(String id) {
		//https://en.wikipedia.org/wiki/Mean_squared_displacement
		LocationEntry entry = mIDLocationTable.get(id);
		Coordinate c_0 = entry.mCoordinates.get(0);
		float distanceSum = 0.0f;
		int numMeasurements = entry.mCoordinates.size();

		for (Coordinate coord : entry.mCoordinates) {
			float dist = (float)distance(c_0.mLatitude, c_0.mLongitude, coord.mLatitude, coord.mLongitude);
			distanceSum += (dist * dist);
		}

		return distanceSum / entry.mCoordinates.size();
	}

	private float rootMeanSquareDeviationAtomicPositions(String id) {
		//https://en.wikipedia.org/wiki/Root-mean-square_deviation_of_atomic_positions
		LocationEntry entry = mIDLocationTable.get(id);

		Coordinate coordAvg = new Coordinate();
		float distanceSum = 0.0f;

		for (Coordinate coord : entry.mCoordinates) {
			coordAvg.mLatitude += coord.mLatitude;
			coordAvg.mLongitude += coord.mLongitude;
		}

		coordAvg.mLatitude /= entry.mCoordinates.size();
		coordAvg.mLongitude /= entry.mCoordinates.size();


		for (Coordinate coord : entry.mCoordinates) {
			float dist = (float)distance(coordAvg.mLatitude, coordAvg.mLongitude, coord.mLatitude, coord.mLongitude);
			distanceSum += (dist * dist);
		}

		return (float)sqrt(distanceSum / entry.mCoordinates.size());
	}

	private float rootMeanSquareDeviation(String id) {
		//https://en.wikipedia.org/wiki/Root-mean-square_deviation

		return 0.0f;
	}

	private float applyError(float d, Timestamp lastForward) {
		return d; //no time-derived error applied
	}


	public void setReadCB (Reader reader) {
		mReadCB = reader;
	}

	public void setWriteCB (Writer writer) {
		mWriteCB = writer;
	}

	public void setQueryCB (Query q) {
		mQueryCB = q;
		mID = mQueryCB.ask("global.id");
	}

	public int read(AdvertisementPayload advPayload)
	{
		System.out.println("Loc hit");
		int result = 0;

		if (advPayload.getMsgType() == AdvertisementPayload.LOCATION_UPDATE) {
			if (!Objects.equals(new String(advPayload.getSrcID()), mID)) { //don't allow spoofed srcID to make change

				result = updateLocation(new String(advPayload.getSrcID()), advPayload.getMsg());
			}
		}

		//check to see if this location is eligible for forwarding
		//in other words, should we pass this on to the routing layer
		if (shouldPass(advPayload)) {
			//someone else might need this, so pass it on
			result = mReadCB.read(advPayload);
		}
		
		return result;
	}

	public int read(String src, byte[] message) {

		return -1;
	}
	public int write(AdvertisementPayload advPayload)
	{

		return 0;
	}
	public int write(String dest, byte[] message){

		return -1;
	}
	public String query(String myQuery)	{
		String resultString = new String();

		String[] parts = myQuery.split("\\s+");

		if (Objects.equals(parts[0], "tag")) {
			resultString = new String("LocMgr");
		}
		else if (Objects.equals(parts[0], "sendLocation")) {
			//send a complete location message
			sendLocation();
		}
		else if (Objects.equals(parts[0], "inDirection")) {
			//return "true" or "false"
			String from = parts[1];
			String to = parts[2];

			resultString = String.valueOf(inDirection(from, to));

		}
		else if (Objects.equals(parts[0], "getLocation")) {
			//return a string that is:
			//latitude longitude

			resultString = String.valueOf(getLocation(parts[1]).mLatitude) + " " + String.valueOf(getLocation(parts[1]).mLongitude);
		}
		else if (Objects.equals(parts[0], "getPositionSpread")) {
			//return a string that is:
			//position spread
			resultString = String.valueOf(positionSpread(parts[1]));
		}
		else if (Objects.equals(parts[0], "setLocation")) {
			// allow setLocation? Bad from a security standpoint, but simplifies
			// handling of locations within the stack
			// expects:
			// latitude longitude

			this.updateLocation(mID, Float.parseFloat(parts[1]), Float.parseFloat(parts[2]));
			sendLocation();
		}
		else if (Objects.equals(parts[0], "getNeighbors")) {
			for ( String key : mIDLocationTable.keySet() ) {
				resultString += key + " ";
			}
		}
		

		return resultString;
	}

	//From: https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude-what-am-i-doi

	public static double distance(double lat1, double lon1, double lat2, double lon2) {
		final int R = 6371; // Radius of the earth

		double latDistance = Math.toRadians(lat2 - lat1);
		double lonDistance = Math.toRadians(lon2 - lon1);
		double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
		        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
		        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double distance = R * c * 1000; // convert to meters

		double height = 0;// if elevation is taken into account el1 - el2;

		distance = Math.pow(distance, 2) + Math.pow(height, 2);

		return Math.sqrt(distance);

    }
}