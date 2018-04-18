package nd.edu.bluenet_new;

import java.util.*;
import java.nio.ByteBuffer;
import java.sql.Timestamp;

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

	// next two functions based on: https://stackoverflow.com/questions/14308746/how-to-convert-from-a-float-to-4-bytes-in-java
	private void sendLocation() {
		LocationEntry myLoc = mIDLocationTable.get(mID);
		if (myLoc != null) {
			Message msg = new Message();
			AdvertisementPayload advPayload = new AdvertisementPayload();

			byte[] lat = ByteBuffer.allocate(4).putFloat(myLoc.mLatitude).array();
			byte[] lon = ByteBuffer.allocate(4).putFloat(myLoc.mLongitude).array();

			//Query for standard header? final in relevant class?
			byte[] header = {(byte)0b11001000};
			byte[] allBytes = new byte[header.length + lat.length + lon.length];
			System.arraycopy(header, 0, allBytes,0,header.length);
			System.arraycopy(lat, 0, allBytes,header.length,lat.length);
			System.arraycopy(lon, 0, allBytes,header.length+lat.length,lon.length);

			msg.fromBytes(allBytes);
			advPayload.setMsg (msg);
			advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
			advPayload.setSrcID(mID);
			advPayload.setDestID(MessageLayer.BROADCAST_GROUP); //not sure if this really matters
			mWriteCB.write(advPayload);
		}
	}

	private int updateLocation(String id, Message message) {
		byte [] latBytes = Arrays.copyOfRange(message.getData(),0,4);
		byte [] lonBytes = Arrays.copyOfRange(message.getData(),4,8);

		float lat =  ByteBuffer.wrap(latBytes).getFloat();
		float lon =  ByteBuffer.wrap(lonBytes).getFloat();

		System.out.println("Updating: " + id);

		LocationEntry entry = new LocationEntry();

		entry.mLatitude = lat;
		entry.mLongitude = lon;
		entry.mTimestamp = new Timestamp(System.currentTimeMillis());

		mIDLocationTable.put(id, entry);

		return 0;
	}

	private boolean inDirection(String srcID, String destID) {
		//used to answer query about whether this node is in the 
		//correct direction
		LocationEntry srcLoc = mIDLocationTable.get(srcID);
		LocationEntry destLoc = mIDLocationTable.get(destID);
		LocationEntry myLoc = mIDLocationTable.get(mID);

		boolean result;
		if (srcLoc == null || destLoc == null || myLoc == null) {
			result = true; //we don't know otherwise
		}
		else {
			double srcToDestDist = distance(srcLoc.mLatitude, srcLoc.mLongitude, destLoc.mLatitude, destLoc.mLongitude);
			double meToDestDist = distance(myLoc.mLatitude, myLoc.mLongitude, destLoc.mLatitude, destLoc.mLongitude);
			
			// System.out.println(srcToDestDist);
			// System.out.println(meToDestDist);
			result = meToDestDist < srcToDestDist;
		}

		return result;
	}

	private LocationEntry getLocation(String id) {
		// provide location of given node id
		return mIDLocationTable.get(id);
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
			if (!Objects.equals(advPayload.getSrcID(), mID)) { //don't allow spoofed srcID to make change

				result = updateLocation(new String(advPayload.getSrcID()), advPayload.getMsg());
			}
		}
		else
		{
			//don't know what to do with it, so pass it on
			result = mReadCB.read(advPayload);
		}


		return result;
	}

	public int read(String src, Message message) {

		return -1;
	}
	public int write(AdvertisementPayload advPayload)
	{

		return 0;
	}
	public int write(String dest, Message message){

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

			LocationEntry loc = getLocation(parts[1]);
			if (loc == null) {
				resultString = "0.0 0.0";
			}
			else {
				resultString = String.valueOf(loc.mLatitude) + " " + String.valueOf(loc.mLongitude);
			}
		}
		else if (Objects.equals(parts[0], "setLocation")) {
			// allow setLocation? Bad from a security standpoint, but simplifies
			// handling of locations within the stack
			// expects:
			// latitude longitude
			LocationEntry loc = new LocationEntry();
			loc.mLatitude = Float.parseFloat(parts[1]);
			loc.mLongitude = Float.parseFloat(parts[2]);
			loc.mTimestamp = new Timestamp(System.currentTimeMillis());

			mIDLocationTable.put(mID, loc);
		}
		else if (Objects.equals(parts[0], "getNeighbors")) {
			for ( String key : mIDLocationTable.keySet() ) {
				resultString += key + " ";
			}
		}
		

		return resultString;
	}

	//From: https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude-what-am-i-doi

	private double distance(double lat1, double lon1, double lat2, double lon2) {
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