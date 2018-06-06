package nd.edu.bluenet_stack;

import java.sql.Timestamp;
import java.nio.ByteBuffer;
import java.util.*;
import java.nio.charset.StandardCharsets;

/**
 * This class is responsible for handling all AdvertisementPayload messages
 * that have something to do with a group. Groups are kept up-to-date between
 * nodes by checking whether another node's list of groups is newer than our
 * own. If it is then we request that they send us their table and we update
 * ours accordingly.
 *
 * <p>Assume that this class sits above the LocationManager.
 *
 * @author Josh Siva
 * @see LayerIFace
 * @see Reader
 * @see Writer
 * @see Query
 * @see GroupEntry
 * @see AdvertisementPayload
 */
public class GroupManager extends LayerBase implements Reader, Query{
	public long deleteThreshold = 30000L;//30 seconds (DEBUG)  //1800000L; //30 minutes
	private List<GroupEntry> mGroups = new ArrayList<GroupEntry>();
	private Timestamp mLastUpdate = null;

	/**
	 * Handles the group table propagation/update protocol and determines
	 * whether a message intended for a group can be routed to us. Always
	 * passes the AdvertisementPayload up to the next layer.
	 *
	 * <p>If we receive a location update from our LocationManager we need to
	 * add the checksum for our group table to the end of the message.
	 *
	 * <p>If we receive a location update from another node we need to check
	 * the checksum on that message against ours to determine whether there
	 * is a mismatch (meaning we have different group tables). In the case of
	 * a mismatch, we send a query to source of the location update with the
	 * latest timestamp for our group table.
	 *
	 * <p>If we receive a query from another node with a timestamp that is
	 * older than ours then we send our group table as an update. Otherwise,
	 * we ignore the message.
	 *
	 * <p>If we receive a group table update then we parse the message, add
	 * any groups that we are missing, and update our table timestamp.
	 *
	 * <p>If we receive a message then we check to see if the destination ID
	 * is for a group that we have joined. If it is, then we can pass the
	 * message up and update the timestamp for that particular group.
	 * 
	 * @param advPayload the payload to evaluate
	 * @return the result of passing the payload up the stack
	 */
	public int read(AdvertisementPayload advPayload) {

		//we need to add the group table chksum to the location update
		if (AdvertisementPayload.LOCATION_UPDATE == advPayload.getMsgType() && Objects.equals(mID, new String(advPayload.getSrcID()))) {
			byte [] chksum = getChkSum();
			byte [] msgBytes = advPayload.getMsg();

			if (null == msgBytes) {
				msgBytes = new byte [10];
			} 
			else if (10 > msgBytes.length) {
				byte [] tmp = msgBytes.clone();
				msgBytes = new byte [10];
				System.arraycopy(tmp, 0, msgBytes, 0, tmp.length);
			}

			System.arraycopy(chksum, 0, msgBytes, 8, chksum.length);
			advPayload.setMsg(msgBytes);
		}
		else if (AdvertisementPayload.LOCATION_UPDATE == advPayload.getMsgType() && !Objects.equals(mID, new String(advPayload.getSrcID()))) {
			byte [] chksum = new byte [2];
			//QUESTION: should we restrict group updates to 1 hop neighbors?
			//there is a verification step that could result in many group 
			//queries from 2nd and even 3rd hop neighbors (likely handled by routing layer)

			//check to see if the checksum matches ours
			byte [] msg = advPayload.getMsg();
			System.arraycopy(msg, 8, chksum, 0, chksum.length);

			//if it doesn't then we need to find out whether the sender has a more up to date
			//list of groups
			if (!Arrays.equals(chksum, getChkSum())) {
				//send group query with timestamp
				AdvertisementPayload newAdv = new AdvertisementPayload();
				newAdv.setSrcID(mID);
				newAdv.setDestID(advPayload.getSrcID());
				newAdv.setMsgType(AdvertisementPayload.GROUP_QUERY);
				long lastUpdate = 0L;
				if (null != mLastUpdate) {
					lastUpdate = mLastUpdate.getTime();
				}
				newAdv.setMsg(ByteBuffer.allocate(8).putLong(lastUpdate).array());

				mReadCB.read(newAdv);
			}
		}
		else if (AdvertisementPayload.GROUP_QUERY == advPayload.getMsgType()) {
			//someone else is querying for our group table
			//check their timestamp against ours (assumes synchronized clocks!!!)

			//from: https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java/29132118#29132118
			ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
			buffer.put(advPayload.getMsg());
			buffer.flip();
			long time = buffer.getLong();
		   
		   	//is our group table newer than theirs?
		   	if (null != mLastUpdate && time < mLastUpdate.getTime()) {
		   		//send group update message containing group table
				AdvertisementPayload newAdv = new AdvertisementPayload();
				newAdv.setSrcID(mID);
				newAdv.setDestID(advPayload.getSrcID());
				newAdv.setMsgType(AdvertisementPayload.GROUP_UPDATE);
				String groupTable = ask("getGroups");
				newAdv.setMsg(groupTable.getBytes(StandardCharsets.UTF_8));

				mReadCB.read(newAdv);
		   	}

		}
		else if (AdvertisementPayload.GROUP_UPDATE == advPayload.getMsgType()) {
			//someone has responded to our group query with an update
			//parse group table in message
			//Bad table could be sent intentionally!!!
			String groupTable = new String(advPayload.getMsg());
			String [] parts = groupTable.split("\\s+");

			for (int i = 0; i < parts.length; i++) {
				String id = parts[i];
				int type = Integer.parseInt(parts[i+1]);
				Group tmpGrp = new Group(id, type);
				Group newGrp = null;

				if (Group.NAMED_GROUP == type) {
					newGrp = new NamedGroup(id, parts[i+2]);
					i += 2;
				}
				else if (Group.GEO_GROUP == type) {
					newGrp = new GeoGroup(id, Float.parseFloat(parts[i+2]),Float.parseFloat(parts[i+3]),Float.parseFloat(parts[i+4]));
					i += 4;
				}

				if (null != newGrp) {
					//if the group already exists in the table, do nothing since groups are immutable
					if (!mGroups.contains(tmpGrp)) {
						GroupEntry grpEntry = new GroupEntry(newGrp);
						mLastUpdate = grpEntry.mTimestamp;
						mGroups.add(grpEntry);
					}
				}

				
			}

		}
		else {

			boolean found = false;

			for (GroupEntry groupEntry: mGroups) {
				//check if we have the group in our table
				if (Arrays.equals(groupEntry.mGroup.getID(), advPayload.getDestID())) {
					groupEntry.update();// clearly, there is activity on group, so update timestamp

					if (groupEntry.mGroup.getStatus()) { //if we have joined this group
						found = true;
					}
				}
			}

			// we belong to the group to which the message was addressed!
			// we always belong to the broadcast group
			if (found || Objects.equals(Group.BROADCAST_GROUP, new String(advPayload.getDestID()))) {
				mReadCB.read(new String(advPayload.getSrcID()), advPayload.getMsg());
			}
		}

		// but more handling may still be required (such as forwarding)
		
		return mReadCB.read(advPayload);

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
	 * Respond to query strings. Handles:
	 *  <p>- checking whether we can join geographical group given a location update
	 *  <p>- adding a group
	 *  <p>- returning a list of groups
	 *  <p>- joining/leaving groups
	 *  <p>- cleaning up group table based on age of GroupEntry
	 * 
	 * @param myQuery the query
	 * @return response if query understood/handled, empty String otherwise
	 * @see GroupEntry
	 */
	public String ask(String myQuery) {
		String resultString = new String();

		String[] parts = myQuery.split("\\s+");

		if (Objects.equals(parts[0], "tag")) {
			resultString = new String("GrpMgr");
		}
		else if (Objects.equals(parts[0], "setLocation")) {
			// expects:
			// latitude longitude
	
			float latitude = Float.parseFloat(parts[1]);
			float longitude = Float.parseFloat(parts[2]);
			
			for (GroupEntry groupEntry: mGroups) {
				if (Group.GEO_GROUP == groupEntry.mGroup.getType()) {
					GeoGroup tmpGrp = (GeoGroup)groupEntry.mGroup;
					tmpGrp.join(latitude, longitude);
				}
			}
		}
		else if (Objects.equals(parts[0], "addGroup")) {
			//can provide:
			//name
			//or
			//latitude longitude radius

			//need to ask for new ID -- probably handled by stack container
			//update mLastUpdate

			boolean bueno = true;
			String newID = mQueryCB.ask("global.getNewID");
			Group grp = null;

			if (2 == parts.length) {
				grp = new NamedGroup(newID, parts[1]);
			}
			else if (4 == parts.length) {
				grp = new GeoGroup(newID, Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3]));
			}
			else {
				bueno = false;
			}

			if (bueno && null != grp) {
				GroupEntry grpEntry = new GroupEntry(grp);
				mLastUpdate = grpEntry.mTimestamp;
				mGroups.add(grpEntry);
				resultString = "ok";
			}
			else {
				resultString = "fail";
			}

		}
		else if (Objects.equals(parts[0], "getGroups")) {
			for (GroupEntry groupEntry: mGroups) {
				resultString += new String(groupEntry.mGroup.getID()) + " " + String.valueOf(groupEntry.mGroup.getType()) + " ";
				
				if (Group.NAMED_GROUP == groupEntry.mGroup.getType()) {
					NamedGroup tmpGrp = (NamedGroup)groupEntry.mGroup;
					resultString += tmpGrp.getName() + " ";
				}
				else if (Group.GEO_GROUP == groupEntry.mGroup.getType()) {
					GeoGroup tmpGrp = (GeoGroup)groupEntry.mGroup;
					resultString += String.valueOf(tmpGrp.getLatitude()) + " " + String.valueOf(tmpGrp.getLongitude()) + " " + String.valueOf(tmpGrp.getRadius()) + " ";
				}

				//don't need to append time stamp since no one needs to know that--for internal use only
			}
		}
		else if (Objects.equals(parts[0], "joinGroup")) {
			resultString = "fail";
			for (GroupEntry groupEntry: mGroups) {
				if (Objects.equals(parts[1], new String(groupEntry.mGroup.getID())) && Group.GEO_GROUP != groupEntry.mGroup.getType()) {
					//the ID needs to be in the group table and we can't choose to join a geographic group
					groupEntry.mGroup.join();
					resultString = "ok";
				}
			}
		}
		else if (Objects.equals(parts[0], "leaveGroup")) {
			resultString = "fail";
			for (GroupEntry groupEntry: mGroups) {
				if (Objects.equals(parts[1], new String(groupEntry.mGroup.getID())) && Group.GEO_GROUP != groupEntry.mGroup.getType()) {
					//the ID needs to be in the group table and we can't choose to join a geographic group
					groupEntry.mGroup.leave();
					resultString = "ok";
				}
			}
		}
		else if (Objects.equals(parts[0], "cleanupGroups")) {
			//remove group if we haven't used it in a while

			//from: https://stackoverflow.com/questions/10431981/remove-elements-from-collection-while-iterating

			List<GroupEntry> found = new ArrayList<GroupEntry>();

			for (GroupEntry groupEntry: mGroups) {
				if (System.currentTimeMillis() - groupEntry.mTimestamp.getTime() > deleteThreshold) {
					found.add(groupEntry);
				}
			}

			mGroups.removeAll(found);
		}
		else if (Objects.equals(parts[0], "getCheckSum")) {
			resultString = new String(getChkSum());
		}

		return resultString;
	}

	/**
	 * @return array of groups that we know of
	 */
	public Group[] getGroups() {
		Group [] grpList = new Group[mGroups.size()];
		for (int i = 0; i < grpList.length; i++) {
			grpList[i] = mGroups.get(i).mGroup;
		}

		return grpList;
	}

	/**
	 * @return 16-bit chksum as length 2 byte array
	 */
	public byte[] getChkSum () {
		byte[] buf = new byte[mGroups.size() * 4];
		byte[] chksum = {0b0, 0b0};
		//make one byte array of all IDs and compute

		//set up the buffer as an array of group ids
		int index = 0;
		for (GroupEntry groupEntry: mGroups) {
			byte[] id = groupEntry.mGroup.getID();
			System.arraycopy(id, 0, buf,index,id.length);
			index += id.length;
		}

		//from: https://stackoverflow.com/questions/4113890/how-to-calculate-the-internet-checksum-from-a-byte-in-java

		int length = buf.length;
	    int i = 0;

	    long sum = 0;
	    long data;

	    // Handle all pairs
	    while (length > 1) {
	      // Corrected to include @Andy's edits and various comments on Stack Overflow
	      data = (((buf[i] << 8) & 0xFF00) | ((buf[i + 1]) & 0xFF));
	      sum += data;
	      // 1's complement carry bit correction in 16-bits (detecting sign extension)
	      if ((sum & 0xFFFF0000) > 0) {
	        sum = sum & 0xFFFF;
	        sum += 1;
	      }

	      i += 2;
	      length -= 2;
	    }

	    // Final 1's complement value correction to 16-bits
	    sum = ~sum;
	    sum = sum & 0xFFFF;

	    chksum[0] = (byte)((sum & 0xFF00) >>> 8);
	    //System.out.println(chksum[0]);
	    chksum[1] = (byte)(sum & 0x00FF);
	    //System.out.println(chksum[1]);

		return chksum;
	}

}