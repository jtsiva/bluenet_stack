package nd.edu.bluenet_stack;

/**
 * Callback for querying about things and getting a response
 *
 * elements of question must be space separated
 * 
 * @author Josh Siva
 * @see ProtocolContainer
 */
public interface Query {
	public String ask(String question);//tokens are space separated
}