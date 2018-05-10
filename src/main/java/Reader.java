package nd.edu.bluenet_stack;

public interface Reader {
	public int read(AdvertisementPayload advPayload);
	public int read(String src, byte[] message);
}
