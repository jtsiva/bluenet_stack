package nd.edu.bluenet_stack;

import java.util.*;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import static java.lang.Math.sqrt;

/**
 * This class is responsible for handling location information including
 * sending location updates, managing recent locations for all discovered nodes,
 * determining location spread of a given node, and determining the distance
 * between two nodes.
 * 
 * @author Josh Siva
 * @see LayerIFace
 * @see LocationEntry
 * @see Reader
 * @see Writer
 * @see Query
 */
public class LocationManager extends LayerBase implements Reader, Query {
	protected HashMap<String, LocationEntry> mIDLocationTable = null;
	

	/**
	 * Default constructor
	 */
	public LocationManager() {
		mIDLocationTable = new HashMap<String, LocationEntry>();
	}

	/**
	 * Send a location update packet containing the latitude and longitude as floats
	 * 
	 * https://stackoverflow.com/questions/14308746/how-to-convert-from-a-float-to-4-bytes-in-java
	 */
	private void sendLocation() {

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

		advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
		advPayload.setSrcID(mID);

		advPayload.setDestID(Group.BROADCAST_GROUP); //eventually need to handle specific location update

		mReadCB.read(advPayload);
		
	}

	/**
	 * Update the location entry of the indicated node
	 * 
	 * @param id alphanumeric BlueNet ID of the node to update
	 * @param lat latitude
	 * @param lon longitude
	 * @return 0 if successful, negative otherwise
	 */
	private int updateLocation(String id, float lat, float lon) {
		LocationEntry loc = mIDLocationTable.get(id);

		if (loc == null) {
			loc = new LocationEntry();
		}
		
		loc.update(lat, lon);

		mIDLocationTable.put(id, loc);

		return 0;
	}

	/**
	 * Update the location of a given node from a byte array containing
	 * latitude and longitude as floats
	 * 
	 * @param id alphanumeric BlueNet ID of the node to update
	 * @param message byte array containing latitude longitude as floats
	 * @return 0 if successful, negative otherwise
	 */
	private int updateLocation(String id, byte[] message) {
		byte [] latBytes = Arrays.copyOfRange(message,0,4);
		byte [] lonBytes = Arrays.copyOfRange(message,4,8);

		float lat =  ByteBuffer.wrap(latBytes).getFloat();
		float lon =  ByteBuffer.wrap(lonBytes).getFloat();

		//System.out.println("Updating: " + id);

		return this.updateLocation(id, lat, lon);
	}

	/**
	 * @param id alphanumeric BlueNet ID of the node to get a location for
	 * @return the average coordinate of the given ID if known. (0,0) otherwise
	 * @see Coordinate
	 */
	private Coordinate getLocation(String id) {
		// provide location centroid of given node id
		LocationEntry entry = mIDLocationTable.get(id);
		Coordinate coordAvg = new Coordinate();

		if (null != entry) {

			for (Coordinate coord : entry.mCoordinates) {
				coordAvg.mLatitude += coord.mLatitude;
				coordAvg.mLongitude += coord.mLongitude;
			}

			if (entry.mCoordinates.size() > 0) {
				coordAvg.mLatitude /= entry.mCoordinates.size();
				coordAvg.mLongitude /= entry.mCoordinates.size();
			}
		}


		return coordAvg;
	}

	/**
	 * Determine whether this node is closer to the destination node than
	 * the source node is
	 * 
	 * @param srcID alphanumeric BlueNet ID of the source node
	 * @param destID alphanumeric BlueNet ID of the destination node
	 * @return true if we are closer, false otherwise
	 */
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

	/**
	 * Determine whether we have enough location measurements to calculate a
	 * position spread
	 * 
	 * @param id alphanumeric BlueNet ID of the node to check
	 * @return true if we have at least 2 measurements, false otherwise
	 */
	private boolean sufficientData(String id) {
		LocationEntry entry = mIDLocationTable.get(id);
		return entry != null && entry.mCoordinates.size() >= 2;
	}

	/**
	 * Calculate the spread (some sort of mean squared calculation) of the 
	 * indicated node's locations
	 * 
	 * @param id alphanumeric BlueNet ID of the node
	 * @return the means squared location spread if there are sufficient measurements
	 * 		   0.0 otherwise.
	 */
	private float positionSpread(String id) {
		float d = 0.0f;
		if (sufficientData(id)) {
			d = meanSquaredDisplacement(id);
		}

		return d;
	}

	/**
	 * https://en.wikipedia.org/wiki/Mean_squared_displacement
	 */
	private float meanSquaredDisplacement(String id) {
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

	/**
	 * https://en.wikipedia.org/wiki/Root-mean-square_deviation_of_atomic_positions
	 */
	private float rootMeanSquareDeviationAtomicPositions(String id) {
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

	/**
	 * https://en.wikipedia.org/wiki/Root-mean-square_deviation
	 */
	private float rootMeanSquareDeviation(String id) {
		return 0.0f;
	}

	/**
	 * Check whether the incoming message is a location update. If so, update
	 * the corresponding node's location. Pass the payload onto the next
	 * layer of the stack
	 * 
	 * @param advPayload the payload to handle
	 * @return result of read--0 if successful, negative otherwise
	 */
	public int read(AdvertisementPayload advPayload)
	{
		//System.out.println("Loc hit");
		int result = 0;

		if (advPayload.getMsgType() == AdvertisementPayload.LOCATION_UPDATE) {
			if (!Objects.equals(new String(advPayload.getSrcID()), mID)) { //don't allow spoofed srcID to make change

				result = updateLocation(new String(advPayload.getSrcID()), advPayload.getMsg());
			}
		}

		// pass this on to the routing layer

		result = mReadCB.read(advPayload);
		
		return result;
	}

	/**
	 * @param src
	 * @param message
	 * @throws UnsupportedOperationException
	 */
	public int read(String src, byte[] message) {
		throw new java.lang.UnsupportedOperationException("Not supported.");
	}

	/**
	 * Handle queries:
	 * <p>-send location update
	 * <p>-check if we're in the direction of the destination
	 * <p>-get location of a node
	 * <p>-get the position spread of a node
	 * <p>-set our location (and send location update)
	 * <p>-get a list of neighbor IDs
	 * 
	 * @param myQuery the query string
	 * @return the result or answer string if the query is handled, empty
	 * 		   String otherwise
	 */
	public String ask(String myQuery)	{
		String resultString = null;

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
				if (!Objects.equals(key, mID)) {
					if (null == resultString) {
						resultString = new String();
					}
					resultString += key + " ";
				}
			}
		}
		else if (Objects.equals(parts[0], "cleanNeighbors")) {
			long timeout = Long.parseLong(parts[1]);
			for ( Map.Entry<String, LocationEntry> entry : mIDLocationTable.entrySet() ) {
				if (timeout < System.currentTimeMillis() - entry.getValue().mTimestamp.getTime()) {
					mIDLocationTable.remove(entry.getKey());
				}
			}
		}

		

		return resultString;
	}

	//From: https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude-what-am-i-doi

	/**
	 * Get the distance in meters between two coordinates
	 * From <a href="https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude-what-am-i-doi">here</a>
	 * 
	 * @param lat1 latitude of node 1
	 * @param lon1 longitude of node 1
	 * @param lat2 latitude of node 2
	 * @param lon2 longitude of node 2
	 * @return the distance in meters between the two points
	 */
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