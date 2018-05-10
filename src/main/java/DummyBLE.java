package nd.edu.bluenet_stack;

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

	public int read(String src, byte[] message) {
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
		System.out.println(advPayload.getPrettyBytes());
		//System.out.println(advPayload.getMsg().getPrettyBytes());

		//get bytes and parse to test functionality

		AdvertisementPayload payload = new AdvertisementPayload();
		byte[] tmpA = advPayload.getHeader();
		byte[] tmpB = advPayload.getMsg();
		byte[] all = new byte[tmpA.length + tmpB.length];
		System.arraycopy(tmpA, 0, all, 0, tmpA.length);
		System.arraycopy(tmpB, 0, all, tmpA.length, tmpB.length);
		payload.fromBytes(all);

		payload.setMsgType (advPayload.getMsgType());

		return read(payload);
	}

	public int write(String dest, byte[] message) {
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