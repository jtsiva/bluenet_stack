package nd.edu.bluenet_new;

import java.util.HashMap;
import java.sql.Timestamp;

public class ConnectionLayer implements LayerIFace {
	protected HashMap<String, LocationEntry> mIDLocationTable = null;
	protected Reader mReadCB;
	protected Writer mWriteCB;

	public void setReadCB (Reader reader) {
		mReadCB = reader;
	}

	public void setWriteCB (Writer writer) {
		mWriteCB = writer;
	}

	/*
	* Only meant to be called with COMPLETE payloads. That is, data (as bytes)
	* should constitute a complete message from BLE either through GATT or SC
	*/
	public int read(AdvertisementPayload advPayload) {
		System.out.println("CL hit");
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		timestamp.getTime();

		mReadCB.read(advPayload.getMsg());
		return 0;
	}

	public int write(Message message) {

		System.out.println("CL hit");
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setMsg(message);
		mWriteCB.write(advPayload);
		return 0;
	}

	public int read(Message message) {
		throw new java.lang.UnsupportedOperationException("Not supported.");
	}
	public int write(AdvertisementPayload advPayload) {
		throw new java.lang.UnsupportedOperationException("Not supported.");
	}

	public String query(String question) {
		String res = new String();

		return res;
	}


}