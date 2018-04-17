package nd.edu.bluenet_new;

import java.util.*;

import java.sql.Timestamp;

public class LocationManager implements LayerIFace {
	protected HashMap<String, LocationEntry> mIDLocationTable = null;
	protected Reader mReadCB;
	protected Writer mWriteCB;

	public LocationManager() {
		mIDLocationTable = new HashMap<String, LocationEntry>();
		mReadCB = null;
		mWriteCB = null;
	}

	private boolean inDirection(byte[] srcID, byte[] destID) {
		//used to answer query about whether this node is in the 
		//correct direction
		return true;
	}

	private LocationEntry getLocation(byte[] id) {
		// provide location of given node id
		return mIDLocationTable.get(id);
	}

	public void setReadCB (Reader reader) {
		mReadCB = reader;
	}

	public void setWriteCB (Writer writer) {
		mWriteCB = writer;
	}

	public int read(AdvertisementPayload advPayload)
	{
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		timestamp.getTime();


		return 0;
	}

	public int read(Message message) {

		return -1;
	}
	public int write(AdvertisementPayload advPayload)
	{

		return 0;
	}
	public int write(Message message){

		return -1;
	}
	public String query(String myQuery)
	{
		return new String();
	}

}