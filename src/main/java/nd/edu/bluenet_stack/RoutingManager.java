package nd.edu.bluenet_stack;

import java.util.*;
import java.sql.Timestamp;

public class RoutingManager implements LayerIFace{
	protected Reader mReadCB;
	protected Writer mWriteCB;
	protected Query mQueryCB;
	private String mID;

	protected HashMap<String, Timestamp> mFwdTimes = new HashMap<String, Timestamp>();
	protected HashMap<String, List<String>> mLocalNodes = new HashMap<String, List<String>>();

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


	private float applyError(float d, Timestamp lastForward) {
		return d; //no time-derived error applied
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
		Timestamp time = mFwdTimes.get(id);


		float d = applyError(Float.parseFloat(mQueryCB.ask("LocMgr.getPositionSpread " + id)), time);
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
	
		if (result || null == time) {
			time = new Timestamp(System.currentTimeMillis());
			mFwdTimes.put(id, time);
		}

		return result;
	}

	public int read(AdvertisementPayload advPayload) {
		int retVal = 0;

		if (advPayload.getMsgType() == AdvertisementPayload.LOCATION_UPDATE && shouldPass(advPayload)) {
			
			if (!Objects.equals(mID, advPayload.getSrcID())) {
				advPayload.decTTL();
			}

			retVal = mWriteCB.write(advPayload);
		}
		else if (advPayload.getMsgType() == AdvertisementPayload.SMALL_MESSAGE 
				|| advPayload.getMsgType() == AdvertisementPayload.REGULAR_MESSAGE) {
			advPayload.decTTL();

			if (Objects.equals(mID, new String(advPayload.getDestID()))) {
				// message is for us so we pass up 
				mReadCB.read(new String(advPayload.getSrcID()), advPayload.getMsg()); //get msg pulls the message
			}
			else { //not for us, so forward
				if (advPayload.getTTL() > 0) {
					//check inDirection
					//check traffic volume
					//check neighbors' direction
					// make sure to pull the message
					//forward if we are eligible
					retVal = mWriteCB.write(advPayload);
				}
			}
		}

		if (AdvertisementPayload.MAX_TTL - 1 == advPayload.getTTL()) { //1 hop neighbors
			mLocalNodes.put(mID, advPayload.getSrcID());
		}
		else if (AdvertisementPayload.MAX_TTL - 2 == advPayload.getTTL()) {//2 hop neighbors
			mLocalNodes.put(advPayload.getOneHopNeighbor(), advPayload.getSrcID());
		}


		return retVal;
	}
	public int read(String src, byte [] message) {
		throw new java.lang.UnsupportedOperationException("Not supported.");
	}
	public int write(AdvertisementPayload advPayload);
	public int write(String dest, byte [] message) {
		throw new java.lang.UnsupportedOperationException("Not supported.");
	}
	public String query(String myQuery) {
		String resultString = new String();

		String[] parts = myQuery.split("\\s+");

		if (Objects.equals(parts[0], "tag")) {
			resultString = new String("RouteMgr");
		}

		return resultString;
	}

}