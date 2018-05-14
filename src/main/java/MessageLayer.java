package nd.edu.bluenet_stack;

import java.util.*;

public class MessageLayer implements LayerIFace {
	private final static int SMALL_MSG_MAX = 10;
	protected Reader mReadCB;
	protected Writer mWriteCB;
	protected Query mQueryCB;
	private byte mMsgIndex;

	public int filterWindowSize = 126;
	private ArrayList<AdvertisementPayload> mWindow = new ArrayList<>(); 

	private boolean isNew(AdvertisementPayload advPayload) {
		if (!mWindow.contains(advPayload)) {
			mWindow.add(advPayload); //haven't seen this message so put in list
			if (filterWindowSize <= mWindow.size()) {
				mWindow.remove(0); //remove oldest
			}

			return true;
		}

		return false;
	}

	public MessageLayer() {
		mReadCB = null;
		mWriteCB = null;
		mMsgIndex = 0b0;
	}

	public void setReadCB (Reader reader) {
		mReadCB = reader;
	}

	public void setWriteCB (Writer writer) {
		mWriteCB = writer;
	}

	public void setQueryCB (Query q) {
		mQueryCB = q;
	}

	/*
	* Only meant to be called with COMPLETE payloads. That is, data (as bytes)
	* should constitute a complete message from BLE either through GATT or SC
	*/
	public int read(AdvertisementPayload advPayload) {
		/*
			Check to make sure we haven't already seen the message
			Double check that message type is correct (only pass up small/reg messages)
			handle message sequencing?
		*/
		//System.out.println("ML hit");
		if (isNew(advPayload)) { //no repeats!

			// We could allow this sort of short circuiting type of message passing, but we wouldn't be
			// able to capture complete network statistics at the routing layer then.

			// if (advPayload.getMsgType() == AdvertisementPayload.SMALL_MESSAGE 
			// 	|| advPayload.getMsgType() == AdvertisementPayload.REGULAR_MESSAGE) { //correct type!
			// 	return mReadCB.read(new String(advPayload.getSrcID()), advPayload.getMsg());
			// }
			// else {
				return mReadCB.read(advPayload);
			//}
		}
		
		return 0;
	}

	public int write(String dest, byte[] message) {
		/*
			Set up the fields for the advertisement:
				set message type
				set msgid
				set dest id
				set src id
				add message to newly created advertisement
		*/
		//System.out.println("ML hit");

		AdvertisementPayload advPayload = new AdvertisementPayload();

		if (message.length > SMALL_MSG_MAX) {
			advPayload.setMsgType(AdvertisementPayload.REGULAR_MESSAGE);
		}
		else {
			advPayload.setMsgType(AdvertisementPayload.SMALL_MESSAGE);
		}

		advPayload.setSrcID(mQueryCB.ask("global.id"));
		advPayload.setDestID(dest);

		advPayload.setMsg(message);

		return this.write(advPayload);
	}

	public int read(String src, byte[] message) {
		throw new java.lang.UnsupportedOperationException("Not supported.");
	}
	public int write(AdvertisementPayload advPayload) {
		// If we receive an AdvertisementPayload, it's safe to assume that all necessary
		// fields have been filled out except for the msgID which can only be set here.
		// However, we do not want to change the msgID of a forwarded message!

		if (Objects.equals(mQueryCB.ask("global.id"), new String(advPayload.getSrcID()))) {
			advPayload.setMsgID(mMsgIndex);
			mMsgIndex++;
		}

		return mWriteCB.write(advPayload);
	}

	public String query(String myQuery) {
		String resultString = new String();
		if (Objects.equals(myQuery, "tag")) {
			resultString = new String("MsgLayer");
		}

		return resultString;
	}


}