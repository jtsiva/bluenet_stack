package nd.edu.bluenet_stack;

/**
 * Callback for handling writes. This is used to help connect layers of the
 * stack together so that the stack can run asynchronously
 * 
 * @author Josh Siva
 * @see Reader
 * @see ProtocolContainer
 */
public interface Writer{
	public int write(AdvertisementPayload advPayload);
	public int write(String dest, byte[] message);
}