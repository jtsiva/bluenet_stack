package nd.edu.bluenet_stack;

import java.util.*;
import java.nio.charset.StandardCharsets;

public class ProtocolContainer implements BlueNetIFace {
	private Result mResultHandler = null;

	private ArrayList<LayerIFace> mLayers = new ArrayList<>(); 
	
	private RoutingManager mRoute = new RoutingManager();
	private GroupManager mGrp = new GroupManager();
	private LocationManager mLoc = new LocationManager();
	private MessageLayer mMsg = new MessageLayer();
	private DummyBLE mBLE = new DummyBLE();

	private Query mQuery;
	private RandomString mRandString = new RandomString(4);
	private String mID;

	public ProtocolContainer () {
		//makes life easier to add all layers to an arraylist
		//order matters here. it is assumed that the topmost layer (first added)
		//will implement a write that takes Messages and a read that provides Messages
		
		mLayers.add(mRoute);
		mLayers.add(mGrp);
		mLayers.add(mLoc);
		mLayers.add(mMsg);
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
					else if (Objects.equals("getNewID", parts[QUERY])) {
						resultString = mRandString.nextString();
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

		//The message layer writes AdvertisementPayloads to the 
		//dummy ble layer
		mMsg.setWriteCB(new Writer() {
			public int write(AdvertisementPayload advPayload) {
				return mBLE.write(advPayload);
			}
			public int write(String dest, byte[] message) {
				return -1;
			}
		});

		//The message layer will hand off messages to this (the top layer) to be printed
		//However, an AdvertisementPayload is passed up then it is sent to LocationManager
		//to handle
		mMsg.setReadCB(new Reader() {
			public int read(String src, byte[] message) {
				
			    
				return 0;
			}

			public int read(AdvertisementPayload advPayload) {
				return mLoc.read(advPayload);
			}
		});

		//The location manager passes messages up the stack to the group manager
		mLoc.setReadCB(new Reader() {
			public int read(AdvertisementPayload advPayload) {
				return mGrp.read(advPayload);
			}
			public int read(String src, byte[] message) {
				return -1;
			}
		});

		//group manager hands to routing manager or all the way to 
		mGrp.setReadCB(new Reader() {
			public int read(AdvertisementPayload advPayload) {
				return mRoute.read(advPayload);
			}
			public int read(String src, byte[] message) {
				if (mResultHandler != null) {
			    	mResultHandler.provide(src, message);
			    }
				return 0;
			}
		});

		//routing manager can only hand 'up' to result handler
		mRoute.setReadCB(new Reader() {
			public int read(AdvertisementPayload advPayload) {
				return -1;
			}
			public int read(String src, byte[] message) {
				if (mResultHandler != null) {
			    	mResultHandler.provide(src, message);
			    }
				return 0;
			}
		});

		//pass writes down to the message layer
		mRoute.setWriteCB(new Writer() {
			public int write(AdvertisementPayload advPayload) {
				return mMsg.write(advPayload);
			}
			public int write(String dest, byte[] message) {
				return mMsg.write(dest, message);
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

		int result = mLayers.get(0).write(destID, input.getBytes(StandardCharsets.UTF_8));

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
		return mGrp.getGroups();
	}

	public void addGroup(String name) {
		mQuery.ask("GrpMgr.addGroup " + name);
	}

	public void addGroup(float lat, float lon, float rad) {
		mQuery.ask("GrpMgr.addGroup " + String.valueOf(lat) + " " + String.valueOf(lon) + " " + String.valueOf(rad));
	}
	public boolean joinGroup(String id) {
		String res = mQuery.ask("GrpMgr.joinGroup " + id);
		return Objects.equals("ok", res);
	}

	public boolean leaveGroup(String id) {
		String res = mQuery.ask("GrpMgr.leaveGroup " + id);
		return Objects.equals("ok", res);
	}

	//************************************************
	//Other functions that need to taken care of here:
	//--periodically updating this devices location
	//************************************************

	private void updateLocation() {
		//get location from Android Location Services
		double lat = 0.0;
		double lon = 0.0;

		String res = mQuery.ask("global.setLocation " + String.valueOf(lat) + " " + String.valueOf(lon));
	}

	
}