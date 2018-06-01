package nd.edu.bluenet_stack;

/**
 * Callback for handling reads. This is used to help connect layers of the
 * stack together so that the stack can run asynchronously
 * 
 * @author Josh Siva
 * @see Writer
 * @see ProtocolContainer
 */
public interface Reader {
	public int read(AdvertisementPayload advPayload);
	public int read(String src, byte[] message);
}
