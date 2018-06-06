package nd.edu.bluenet_stack;

import java.util.*;

/**
 * This class handles simple message filtering and helps provide
 * the final few fields to the payload before it is sent
 * 
 * @author Josh Siva
 * @see LayerIFace
 * @see Reader
 * @see Writer
 */
public class MessageLayer extends LayerBase implements Reader, Writer, Query {
	private final static int SMALL_MSG_MAX = 9;

	private byte mMsgIndex;

	public int filterWindowSize = 126;
	private ArrayList<AdvertisementPayload> mWindow = new ArrayList<>(); 
	private boolean mAllowFromSelf = false;

	/**
	 * Default constructor. Set msg ID (to be assigned to outgoing messages)
	 * to 0. This will be incremented for each new outgoing message.
	 */
	public MessageLayer() {
		mMsgIndex = 0b0;
	}

	/**
	 * Check to see whether the current message is a "new" message based on
	 * source ID and the message ID
	 * 
	 * @param advPayload the payload
	 * @return true if the message has not been repeated within the window,
	 * 		   false otherwise.
	 * @see AdvertisementPayload
	 */
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

	/**
	 * Check to make sure we haven't already seen the message.
	 * 
	 * @param advPayload a complete payload from a lower layer
	 * @return result of read callback-- 0 if successful, negative otherwise
	 */
	public int read(AdvertisementPayload advPayload) {

		//System.out.println("ML hit");
		if (isNew(advPayload)) {//no repeats!
			if (mAllowFromSelf || !Objects.equals(mID, new String(advPayload.getSrcID()))) { 
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
			
		}
		
		return 0; //even though we're ignoring the message, it's not an error so return 0
	}

	/**
	 * Take a simple byte message and a destination and create an complete
	 * AdvertisementPayload to be passed down to a lower layer of the stack.
	 * Set the message type based on the size of the message array
	 * 
	 * @param dest the alphanumeric BlueNet ID of the destination node
	 * @param message the stuff to send
	 * @return the result of writing, 0 if successful, negative otherwise
	 */
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

	/**
	 * @param src
	 * @param message
	 * @throws UnsupportedOperationException
	 */
	public int read(String src, byte[] message) {
		throw new java.lang.UnsupportedOperationException("Not supported.");
	}

	/**
	 * If we receive an AdvertisementPayload, it's safe to assume that all 
	 * necessary fields have been filled out except for the msgID which can 
	 * only be set here.
	 * 
	 * <p>However, we do not want to change the msgID of a forwarded message,
	 * so just pass it through
	 * 
	 * @param advPayload the payload to write to a lower layer of the stack
	 * @return result of write--0 if successful, negative otherwise
	 */
	public int write(AdvertisementPayload advPayload) {
		// 

		if (Objects.equals(mQueryCB.ask("global.id"), new String(advPayload.getSrcID()))) {
			advPayload.setMsgID(mMsgIndex);
			mMsgIndex++;
		}

		return mWriteCB.write(advPayload);
	}

	/**
	 * Respond to queries. Only handles identification of layer
	 * 
	 * @param myQuery the query
	 * @return response to query or empty String if not handled
	 */
	public String ask(String myQuery) {
		String resultString = new String();
		if (Objects.equals(myQuery, "tag")) {
			resultString = new String("MsgLayer");
		}
		else if (Objects.equals(myQuery, "allowFromSelf")) {
			mAllowFromSelf = true;
		}

		return resultString;
	}


}