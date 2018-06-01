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
	 * @return the contents read from the message characteristic
	 */
	public byte[] retrieve();
}