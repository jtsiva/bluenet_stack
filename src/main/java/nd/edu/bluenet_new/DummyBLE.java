package nd.edu.bluenet_new;

import java.util.*;

public class DummyBLE implements LayerIFace{
	protected Reader mReadCB;
	protected Writer mWriteCB;
	protected Query mQueryCB;

	public void setReadCB (Reader reader) {
		this.mReadCB = reader;
	}

	public void setWriteCB (Writer writer) {
		this.mWriteCB = writer;
	}

	public void setQueryCB (Query q) {
		mQueryCB = q;
	}

	public int read(AdvertisementPayload advPayload) {
		System.out.println("Dummy hit");
		return mReadCB.read(advPayload);
	}

	public int read(Message message) {
		throw new java.lang.UnsupportedOperationException("Not supported.");
	}

	public int write(AdvertisementPayload advPayload) {
		/*
		 let T be the send timeout
		 if advPayload.getMsgType() == SMALL_MESSAGE:
		 	//check size or truncate payload
		 	setAdvertisementPayload(advPayload.getBytes() + advPayload.getMsg().getBytes())
		 	setTimeout = T
		 else:
		 	setAdvertisementPayload(advPayload.getBytes())
		 	setCharacteristic(advPayload.getMsg().getBytes())
		 	setTimeout = T //clear the characteristic after

		 return status
		 */

		System.out.println("Dummy hit");
		return read(advPayload);
	}

	public int write(Message message) {
		throw new java.lang.UnsupportedOperationException("Not supported.");
	}

	public String query(String myQuery) {
		String resultString = new String();

		if (Objects.equals(myQuery, "tag")) {
			resultString = new String("BLE");
		}

		return resultString;
	}
}