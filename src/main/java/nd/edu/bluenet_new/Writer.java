package nd.edu.bluenet_new;

public interface Writer{
	public int write(AdvertisementPayload advPayload);
	public int write(String dest, Message message);
}