package nd.edu.bluenet_stack;

import java.util.*;

/**
 * Network layer that is intended as a loopback for testing upper layers of the
 * stack.
 * 
 * @author Josh Siva
 * @see LayerIFace
 */
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

	/**
	 * @param advPayload the payload to pass through to the next layer
	 * @return 0 if successful, else error code
	 */
	public int read(AdvertisementPayload advPayload) {
		//System.out.println("Dummy hit");
		return mReadCB.read(advPayload);
	}

	/**
	 * Not supported by this layer
	 * 
	 * @param src
	 * @param message
	 * @throws UnsupportedOperationException
	 */
	public int read(String src, byte[] message) {
		throw new java.lang.UnsupportedOperationException("Not supported.");
	}

	/**
	 * Dumps the payload to a byte array which is then parsed into a payload
	 * again.
	 * 
	 * @param advPayload the payload to dump to a byte array
	 * @return result of read--0 if successful, else negative
	 */
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

		// System.out.println("Dummy hit");
		// System.out.println(advPayload.getPrettyBytes());
		//System.out.println(advPayload.getMsg().getPrettyBytes());

		//get bytes and parse to test functionality

		AdvertisementPayload payload = new AdvertisementPayload();
		byte[] tmpA = advPayload.getHeader();
		byte[] tmpB = advPayload.getMsg();

		byte[] all = new byte[tmpA.length + tmpB.length];
		System.arraycopy(tmpA, 0, all, 0, tmpA.length);
		System.arraycopy(tmpB, 0, all, tmpA.length, tmpB.length);
		//System.out.println("raw: " +  new String(all));
		payload.fromBytes(all);
		//System.out.println("after parse:" + new String(payload.getMsg()));

		payload.setMsgType (advPayload.getMsgType());

		return read(payload);
	}

	/**
	 * Not supported by this layer
	 * 
	 * @param dest
	 * @param message
	 * @throws UnsupportedOperationException
	 */
	public int write(String dest, byte[] message) {
		throw new java.lang.UnsupportedOperationException("Not supported.");
	}

	/**
	 * @param myQuery query to which this layer could respond
	 * @return response to query if implemented, else empty String
	 */
	public String query(String myQuery) {
		String resultString = new String();

		if (Objects.equals(myQuery, "tag")) {
			resultString = new String("BLE");
		}

		return resultString;
	}
}