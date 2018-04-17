package nd.edu.bluenet_new;

import java.util.*;

public class FilterCopies implements LayerIFace {
	public int filterWindowSize = 126;
	private ArrayList<AdvertisementPayload> mWindow = new ArrayList<>(); 
	protected Reader mReadCB;
	protected Writer mWriteCB;

	public void setReadCB (Reader reader) {
		mReadCB = reader;
	}

	public void setWriteCB (Writer writer) {
		mWriteCB = writer;
	}

	//Note that object must implement equals
	private boolean isNew(AdvertisementPayload advPayload) {
		if (!mWindow.contains(advPayload)) {
			mWindow.add(advPayload); //haven't seen this message so put in list
			if (filterWindowSize <= mWindow.size()) {
				mWindow.remove(0); //remove oldest
			}

			return true;
		}

		return false;
	}

	public int read(AdvertisementPayload advPayload)
	{
		if (isNew(advPayload)) {
			return mReadCB.read(advPayload);
		}

		return 0;
	}

	public int read(Message message) {

		return -1;
	}
	public int write(AdvertisementPayload advPayload)
	{
		if (isNew(advPayload)) {
			return mWriteCB.write(advPayload);
		}

		return 0;
	}
	public int write(Message message){

		return -1;
	}
	public String query(String myQuery)
	{
		return new String();
	}

}