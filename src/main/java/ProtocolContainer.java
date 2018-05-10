package nd.edu.bluenet_stack;

import java.util.*;

public class ProtocolContainer implements BlueNetIFace {
	private Result mResultHandler = null;

	private ArrayList<LayerIFace> mLayers = new ArrayList<>(); 
	
	private MessageLayer mMsg = new MessageLayer();
	private LocationManager mLoc = new LocationManager();
	private DummyBLE mBLE = new DummyBLE();

	private Query mQuery;
	private RandomString mRandString = new RandomString(4);
	private String mID;

	public ProtocolContainer () {
		//makes life easier to add all layers to an arraylist
		//order matters here. it is assumed that the topmost layer (first added)
		//will implement a write that takes Messages and a read that provides Messages
		mLayers.add(mMsg);
		mLayers.add(mLoc);
		mLayers.add(mBLE);

		mID = mRandString.nextString();

		mQuery = new Query() {
			public String ask(String question) {
				final String TAG_Q = "tag";
				final int TAG = 0;
				final int QUERY = 1;
				/*
					Questions will be dispatched to specific submodules from here
					Each question/command must start with a tag word (no periods) followed
					by a period and then the query:
					<tag>.<query>

					each submodule (layer) must at least respond to query('tag') with something
					to identify the layer for dispatching the query

					The top-most module (the one in which this is implemented) can receive
					queries if it catches the tag 'global'

				*/
				String[] parts = question.split("\\.", 2);

				String resultString = new String();

				if (Objects.equals("global", parts[TAG])) {
					if (Objects.equals("id", parts[QUERY])) {
						resultString = mID;
					}
					else if (Objects.equals("reset id", parts[QUERY])) { //id collision detected so regen
						mID = mRandString.nextString();
					}
					else if (parts[QUERY].contains("setLocation")) { //maybe all other global queries are passed to everyone?
						for (LayerIFace layer: mLayers) {
							resultString = layer.query(parts[QUERY]);
						}
					}
				}
				else {

					for (LayerIFace layer: mLayers) {
						if (Objects.equals(parts[TAG], layer.query(TAG_Q))) {
							resultString = layer.query(parts[QUERY]);
						}
					}
				}

				return resultString;
			}
		};

		for (LayerIFace layer: mLayers) {
			layer.setQueryCB(mQuery);
		}

		connectLayers();
	}

	private void connectLayers() {
		//Connect the layers together

		//the dummy ble layer get AdvertisementPayloads and passes them to 
		//the message layer
		mBLE.setReadCB(new Reader() {
			public int read(AdvertisementPayload advPayload) {
				return mMsg.read(advPayload);
			}

			public int read(String src, byte[] message) {
				return -1;
			}
		});

		//The message layer writes Messages or AdvertisementPayloads to the 
		//dummy ble layer
		mMsg.setWriteCB(new Writer() {
			public int write(AdvertisementPayload advPayload) {
				return mBLE.write(advPayload);
			}
			public int write(String dest, byte[] message) {
				return mBLE.write(dest, message);
			}
		});

		//The message layer will hand off messages to this (the top layer) to be printed
		//However, an AdvertisementPayload is passed up then it is sent to LocationManager
		//to handle
		mMsg.setReadCB(new Reader() {
			public int read(String src, byte[] message) {
				
			    if (mResultHandler != null) {
			    	mResultHandler.provide(src, message);
			    }

				return 0;
			}

			public int read(AdvertisementPayload advPayload) {
				return mLoc.read(advPayload);
			}
		});

		//The location manager writes (mostly) complete AdvertisementPayloads to the
		//message layer to complete and send
		mLoc.setWriteCB(new Writer() {
			public int write(AdvertisementPayload advPayload) {
				return mMsg.write(advPayload);
			}
			public int write(String dest, byte[] message) {
				return -1;
			}
		});
	}

	//***********************************
	//Interface things to implement
	//***********************************

	public String getMyID() {
		return mID;
	}
	public int write(String destID, String input) {

		int result = mLayers.get(0).write(destID, input.getBytes());

		if (0 == result) {
			result = input.length();
		}

		return result;
	}

	public void regCallback(Result resultHandler) {
		mResultHandler = resultHandler;
	}

	public String[] getNeighbors() {
		String res = mQuery.ask("LocMgr.getNeighbors");
		String[] ids = res.split("\\s+");

		return ids;
	}
	public String getLocation(String id) {
		return mQuery.ask("LocMgr.getLocation " + id);
	}

	public Group [] getGroups()	{

		String res = mQuery.ask("GrpMgr.getGroups");
		String[] groupInfo = res.split(",");
		Group[] groups = new Group[groupInfo.length];
		int index = 0;

		for (String group: groupInfo) {
			String[] parts = group.split("\\s+");
			groups[index] = new NamedGroup(parts[0],parts[1]);
			index++;
		}
		

		return groups;
	}

	public void addGroup(String name) {

	}

	public void addGroup(float lat, float lon, float rad) {

	}
	public boolean joinGroup(String id) {
		return false;
	}

	public boolean leaveGroup(String id) {
		return false;
	}

	//************************************************
	//Other functions that need to taken care of here:
	//--periodically broadcast location updates
	//--periodically updating this devices location
	//************************************************

	private void updateLocation() {
		//get location from Android Location Services
		double lat = 0.0;
		double lon = 0.0;

		String res = mQuery.ask("LocMgr.setLocation " + String.valueOf(lat) + " " + String.valueOf(lon));
	}

	private void sendUpdate() {
		String res = mQuery.ask ("LocMgr.sendLocation");
	}

}