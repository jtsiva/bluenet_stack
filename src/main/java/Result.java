package nd.edu.bluenet_stack;

/**
 * A callback to be implemented in the application that provides data
 * intended for this device to the application asynchronously.
 * 
 * @author Josh Siva
 * @see ProtocolContainer
 */
public interface Result {
	public int provide(String src, byte[] data);
}