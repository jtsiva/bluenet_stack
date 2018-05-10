package nd.edu.bluenet_stack;

public interface LayerIFace{

	public void setReadCB(Reader reader);
	public void setWriteCB (Writer writer);
	public void setQueryCB (Query q);

	public int read(AdvertisementPayload advPayload);
	public int read(String src, byte [] message);
	public int write(AdvertisementPayload advPayload);
	public int write(String dest, byte [] message);
	public String query(String myQuery);

}