package nd.edu.bluenet_stack;

import java.util.*;
import java.sql.Timestamp;

/**
 * The layer of the stack makes all forwarding decisions. Messages generated
 * at lower layers of the network stack should all send their payload up to
 * here to make the decision about how to send the packet.
 * 
 * @author Josh Siva
 * @see LayerIFace
 */
public class RoutingManager extends LayerBase implements Reader, Writer, Query{

	protected HashMap<String, Timestamp> mFwdTimes = new HashMap<String, Timestamp>();
	protected HashMap<String, List<String>> mLocalNodes = new HashMap<String, List<String>>();

	/**
	 * Convenience function for applying a time-derived error model to a
	 * measurement of the spread of recent location updates from a node.
	 *
	 * <p>No error model applied at this time.
	 * 
	 * @param d the spread of recent location updates
	 * @param lastForward timestamp for the last time we forwarded the location
	 * 		  update for the node
	 * @return spread of recent location updates adjusted for any time-derived
	 * 		   errors
	 */
	private float applyError(float d, Timestamp lastForward) {
		if (null != lastForward) {
			long timeDiff = System.currentTimeMillis() - lastForward.getTime();

			float seconds = timeDiff / 1000.0f;

			d = d + (seconds * .5f); //increase the spread by .5m each second
		}

		return d;
	}

	/**
	 * Implement policy to determine whether the location update from
     * the given ID should be forwarded. We do not want a broadcast
	 * storm of location updates. 
	 * 
	 * <p>The general idea is that the further we are from the source
	 * of the location update, the less inclined we will be to forward
	 * the update
	 * 
	 * @param advPayload the payload to evaluate
	 * @return true if we should forward the payload, false otherwise
	 */
	private boolean shouldPass(AdvertisementPayload advPayload) {
		/*
			Implement policy to determine whether the location update from
			the given ID should be forwarded. We do not want a broadcast
			storm of location updates.

			See protocol proposal in OSF

			The distance thresholds will likely need to be adjusted 

		*/

		final byte ME = AdvertisementPayload.MAX_TTL; //hops
		final float ME_D_THRESHOLD = 1.0f; //meters
		final byte NEAR = ME - 1;
		final float NEAR_D_THRESHOLD = 3.0f;
		final byte MEDIUM = ME - 2;
		final float MEDIUM_D_THRESHOLD = 7.0f;
		final byte FAR = ME - 3;
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

	/**
	 * This function handles all of the forwarding decisions and captures
	 * local topology (out to 2 hop neighbors) to aid in making those 
	 * decisions. Forwarding decisions are determined by message type:
	 *
	 * <p>Location updates are forwarded based on the somewhat more complicated
	 * policy found in {@code shouldPass()}.
	 *
	 * <p>Group queries and updates are only sent to one-hop neighbors
	 *
	 * <p>Messages intended for this node are passed up the stack. Messages can
	 * be forwarded based on a number of criteria, but for now are simply
	 * rebroadcast.
	 * 
	 * @param advPayload the payload to evaluate
	 * @return 0 if successful, negative otherwise
	 * @see AdvertisementPayload
	 */
	public int read(AdvertisementPayload advPayload) {
		int retVal = 0;

		if (AdvertisementPayload.MAX_TTL - 1 == advPayload.getTTL()) { //1 hop neighbors
			List<String> neighbors = mLocalNodes.get(mID);

			if (null == neighbors) {
				neighbors = new ArrayList<String>();
			}
			String newNeighbor = new String(advPayload.getSrcID());
			if (!neighbors.contains(newNeighbor)) {
				neighbors.add(newNeighbor);
				mLocalNodes.put(mID, neighbors);
			}
		}
		else if (AdvertisementPayload.MAX_TTL - 2 == advPayload.getTTL()) {//2 hop neighbors
			if (null != advPayload.getOneHopNeighbor()) {
				List<String> neighbors = mLocalNodes.get(new String(advPayload.getOneHopNeighbor()));

				if (null == neighbors) {
					neighbors = new ArrayList<String>();
				}
				String newNeighbor = new String(advPayload.getSrcID());
				if (!neighbors.contains(newNeighbor)) {
					neighbors.add(newNeighbor);
					mLocalNodes.put(new String(advPayload.getOneHopNeighbor()), neighbors);
				}
			}
		}

		if (advPayload.getMsgType() == AdvertisementPayload.LOCATION_UPDATE && shouldPass(advPayload)) {
			
			if (!Objects.equals(mID, new String(advPayload.getSrcID()))) {
				advPayload.decTTL();
			}

			retVal = mWriteCB.write(advPayload);
		}
		else if (advPayload.getMsgType() == AdvertisementPayload.SMALL_MESSAGE 
				|| advPayload.getMsgType() == AdvertisementPayload.REGULAR_MESSAGE) {
			
			if (Objects.equals(mID, new String(advPayload.getDestID()))) {
				// message is for us so we pass up 
				mReadCB.read(new String(advPayload.getSrcID()), advPayload.getMsg()); //get msg pulls the message
			}
			else { //not for us, so forward
				if (advPayload.getTTL() > 0) {
					advPayload.decTTL();
					//check inDirection
					//check traffic volume
					//check neighbors' direction
					
					// make sure to pull the message
					advPayload.getMsg();
					//forward if we are eligible
					
					//using pull-based by default
					advPayload.push = false;
					retVal = mWriteCB.write(advPayload);
				}
			}
		}
		else if (advPayload.getMsgType() == AdvertisementPayload.GROUP_QUERY 
				|| advPayload.getMsgType() == AdvertisementPayload.GROUP_UPDATE) {
			//Do not forward these message types! Only for 1-hop neighbors
			if (advPayload.getTTL() == AdvertisementPayload.MAX_TTL) {
				advPayload.decTTL();
				retVal = mWriteCB.write(advPayload);
			}
		}


		return retVal;
	}

	/**
	 * @param src
	 * @param message
	 * @throws UnsupportedOperationException
	 */
	public int read(String src, byte [] message) {
		throw new java.lang.UnsupportedOperationException("Not supported.");
	}

	/**
	 * @param advPayload
	 * @throws UnsupportedOperationException
	 */
	public int write(AdvertisementPayload advPayload) {
		return mWriteCB.write(advPayload);
	}

	/**
	 * @param dest
	 * @param message
	 * @throws UnsupportedOperationException
	 */
	public int write(String dest, byte [] message) {
		return mWriteCB.write(dest, message);
	}

	/**
	 * Handles queries. Right now only responds to ID request
	 * 
	 * @param myQuery question posed to layer
	 * @return response or empty String if not handled
	 */
	public String ask(String myQuery) {
		String resultString = new String();

		String[] parts = myQuery.split("\\s+");

		if (Objects.equals(parts[0], "tag")) {
			resultString = new String("RouteMgr");
		}

		return resultString;
	}

}