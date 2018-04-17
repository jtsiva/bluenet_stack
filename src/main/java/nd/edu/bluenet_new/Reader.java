package nd.edu.bluenet_new;

public interface Reader {
	public int read(AdvertisementPayload advPayload);
	public int read(Message message);
}
