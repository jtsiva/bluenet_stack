package nd.edu.bluenet_stack;

import java.util.*;

public class GroupManager implements LayerIFace{
	protected Reader mReadCB;
	protected Writer mWriteCB;
	protected Query mQueryCB;
	private ArrayList<Group> mGroups = new ArrayList<>();
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


		boolean found = false;

		for (Group group: mGroups) {
			if (Arrays.equals(group.getID(), advPayload.getDestID()) && group.getStatus()) {
				found = true;
			}
		}

		// we belong to the group to which the message was addressed!
		if (found) {
			mReadCB.read(new String(advPayload.getSrcID()), advPayload.getMsg());
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
		else if (Objects.equals(parts[0], "getGroups")) {
			for (Group group: mGroups) {
				if (Group.NAMED_GROUP == group.getType()) {
					NamedGroup tmpGrp = (NamedGroup)group;
					resultString += new String(tmpGrp.getID()) + " " + tmpGrp.getName() + ",";
				}
			}
		}
		else if (Objects.equals(parts[0], "joinGroups")) {
			for (Group group: mGroups) {
				if (Objects.equals(parts[1], group.getID())) {
					group.join();
				}
			}
		}
		else if (Objects.equals(parts[0], "cleanupGroups")) {
			//remove group if we haven't used it in a while
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