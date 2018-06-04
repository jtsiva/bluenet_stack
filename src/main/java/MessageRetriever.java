package nd.edu.bluenet_stack;

/**
 * Callback mechanism for handling pull based communication. The retrieve
 * function should read the characteristic containing the message and return
 * it through this function.
 * 
 * @author Josh Siva
 * @see AdvertisementPayload
 */
public interface MessageRetriever {

	/**
	 * @param srcID the alphanumeric BlueNet ID of the device from which to
	 *        fetch the data
	 * @return the contents read from the message characteristic
	 */
	public byte[] retrieve(byte [] srcID);
}