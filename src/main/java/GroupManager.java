package nd.edu.bluenet_stack;

import java.sql.Timestamp;
import java.nio.ByteBuffer;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class GroupManager implements LayerIFace{
	public long deleteThreshold = 30000L;//30 seconds (DEBUG)  //1800000L; //30 minutes
	protected Reader mReadCB;
	protected Writer mWriteCB;
	protected Query mQueryCB;
	private List<GroupEntry> mGroups = new ArrayList<GroupEntry>();
	private Timestamp mLastUpdate = null;
	private String mID; 

	public void setReadCB (Reader reader) {
		this.mReadCB = reader;
	}

	public void setWriteCB (Writer writer) {
		this.mWriteCB = writer;
	}

	public void setQueryCB (Query q) {
		mQueryCB = q;
		mID = mQueryCB.ask("global.id");
	}

	public int read(AdvertisementPayload advPayload) {

		//handle things: 
		//	- update available groups
		//	- check if we are in the group or the recipient to which the message is addressed
		//	- pass message that require additional handling as AdvertisementPayloads--\
		//	- pass message relevant to this device as srcID and String-----------------Not mutually exclusive


		//we need to add the group table chksum to the location update
		if (AdvertisementPayload.LOCATION_UPDATE == advPayload.getMsgType() && Objects.equals(mID, advPayload.getSrcID())) {
			byte [] chksum = getChkSum();
			byte [] msgBytes = advPayload.getMsg();

			System.arraycopy(chksum, 0, msgBytes, 8, chksum.length);
			advPayload.setMsg(msgBytes);
		}
		else if (AdvertisementPayload.LOCATION_UPDATE == advPayload.getMsgType() && !Objects.equals(mID, advPayload.getSrcID())) {
			byte [] chksum = byte [2];
			//QUESTION: should we restrict group updates to 1 hop neighbors?
			//there is a verification step that could result in many group 
			//queries from 2nd and even 3rd hop neighbors (likely handled by routing layer)

			//check to see if the checksum matches ours
			System.arraycopy(advPayload.getMsg(), 8, chksum, chksum.length);

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
				newAdv.setMsg(ByteBuffer.allocate(8).putLong(lastUpdate).array())

				mReadCB.read(newAdv);
			}
		}
		else if (AdvertisementPayload.GROUP_QUERY == advPayload.getMsgType()) {
			//someone else is querying for our group table
			//check their timestamp against ours (assumes synchronized clocks!!!)

			//from: https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java/29132118#29132118
			long time = ByteBuffer.allocate(Long.BYTES).put(advPayload.getMsg()).flip().getLong();
		   
		   	if (time > mLastUpdate.getTime()) {
		   		//send group update message containing group table
				AdvertisementPayload newAdv = new AdvertisementPayload();
				newAdv.setSrcID(mID);
				newAdv.setDestID(advPayload.getSrcID());
				newAdv.setMsgType(AdvertisementPayload.GROUP_UPDATE);
				String groupTable = query("getGroups");
				newAdv.setMsg(groupTable.getBytes(StandardCharsets.UTF_8))

				mReadCB.read(newAdv);
		   	}

		}
		else if (AdvertisementPayload.GROUP_UPDATE == advPayload.getMsgType()) {
			//someone has responded to our group query with an update
			//parse group table in message
			String groupTable = new String(advPayload.getMsg());
			String [] parts = groupTable.split("\\s+");

			for (int i = 0; i < parts.length; i++) {
				String id = parts[i];
				int type = Integer.parseInt(parts[i+1]);
				Group tmpGrp = new Group(id, type);

				if (Group.NAMED_GROUP == type) {
					NamedGroup newGrp = new NamedGroup(id, parts[i+2]);
					i += 2;
				}
				else if (Group.GEO_GROUP == type) {
					GeoGroup newGrp = new GeoGroup(id, Float.parseFloat(parts[i+2]),Float.parseFloat(parts[i+3]),Float.parseFloat(parts[i+4]));
					i += 4;
				}

				//if the group already exists in the table, do nothing since groups are immutable
				if (!mGroups.contains(tmpGrp)) {
					GroupEntry grpEntry = new GroupEntry(newGrp);
					mLastUpdate = grpEntry.mTimestamp;
					mGroups.add(grpEntry);
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
			if (found) {
				mReadCB.read(new String(advPayload.getSrcID()), advPayload.getMsg());
			}
		}

		// but more handling may still be required (such as forwarding)
		mReadCB.read(advPayload);
		

		return 0;

	}
	public int read(String src, byte[] message) {
		throw new java.lang.UnsupportedOperationException("Not supported.");
	}
	public int write(AdvertisementPayload advPayload) {
		//Used for group creation?
		throw new java.lang.UnsupportedOperationException("Not supported.");
	}
	public int write(String dest, byte[] message) {
		//Used for group creation?
		throw new java.lang.UnsupportedOperationException("Not supported.");
	}
	public String query(String myQuery) {
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
			
			for (Group group: mGroups) {
				if (Group.GEO_GROUP == group.getType()) {
					GeoGroup tmpGrp = (GeoGroup)group;
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
			if (2 == parts.length) {
				NamedGroup grp = new NamedGroup(newID, parts[1]);
			}
			else if (4 == parts.length) {
				GeoGroup grp = new GeoGroup(newID, Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3]));
			}
			else {
				bueno = false;
			}

			if (bueno) {
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
				resultString += groupEntry.mGroup.getID() + " " + new String(groupEntry.mGroup.getType()) + " ";
				
				if (Group.NAMED_GROUP == group.getType()) {
					NamedGroup tmpGrp = (NamedGroup)groupEntry.mGroup;
					resultString += tmpGrp.getName() + " ";
				}
				else if (Group.GEO_GROUP == group.type()) {
					GeoGroup tmpGrp = (GeoGroup)groupEntry.mGroup;
					resultString += String.valueOf(tmpGrp.getLatitude()) + " " + String.valueOf(tmpGrp.getLongitude()) + " " + String.valueOf(tmpGrp.getRadius()) + " ";
				}

				//don't need to append time stamp since no one needs to know that--for internal use only
			}
		}
		else if (Objects.equals(parts[0], "joinGroups")) {
			resultString = "fail";
			for (GroupEntry groupEntry.mGroup: mGroups) {
				if (Objects.equals(parts[1], groupEntry.mGroup.getID())) {
					groupEntry.mGroup.join();
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

	private byte[] getChkSum () {
		byte[] buf = new byte[mGroups.size() * 4];
		byte[] chksum = {0b0, 0b0};
		//make one byte array of all IDs and compute

		//set up the buffer as an array of group ids
		int index = 0;
		for (Group group: mGroups) {
			byte[] id = group.getID();
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
	    chksum[1] = (byte)(sum & 0x00FF);

		return chksum;
	}

}