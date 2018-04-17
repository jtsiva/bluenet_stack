package nd.edu.bluenet_new;

public interface LayerIFace{

	public void setReadCB(Reader reader);
	public void setWriteCB (Writer writer);
	public void setQueryCB (Query q);

	public int read(AdvertisementPayload advPayload);
	public int read(Message message);
	public int write(AdvertisementPayload advPayload);
	public int write(Message message);
	public String query(String myQuery);

}