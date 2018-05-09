package nd.edu.bluenet_stack;

import java.util.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class LayerTest {

	static private ArrayList<LayerIFace> mLayers = new ArrayList<>(); 
	
	public static void main(String[] args){

		System.out.println("setting things up...");

		MessageLayer msgL = new MessageLayer();
		LocationManager locMgr = new LocationManager();
		DummyBLE dummy = new DummyBLE();

		//makes life easier to add all layers to an arraylist
		mLayers.add(msgL);
		mLayers.add(locMgr);
		mLayers.add(dummy);

		RandomString randString = new RandomString(4);
		final String myID = randString.nextString();

		Query myQ = new Query() {
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
						resultString = myID;
					}
					else if (Objects.equals("reset id", parts[QUERY])) { //id collision detected so regen
						//myID = randString.nextString();
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

		// Set the above query handler for all layers
		locMgr.setQueryCB(myQ);
		msgL.setQueryCB(myQ);
		dummy.setQueryCB(myQ);

		//Connect the layers together

		//the dummy ble layer get AdvertisementPayloads and passes them to 
		//the message layer
		dummy.setReadCB(new Reader() {
			public int read(AdvertisementPayload advPayload) {
				return msgL.read(advPayload);
			}

			public int read(String src, byte[] message) {
				return -1;
			}
		});

		//The message layer writes Messages or AdvertisementPayloads to the 
		//dummy ble layer
		msgL.setWriteCB(new Writer() {
			public int write(AdvertisementPayload advPayload) {
				return dummy.write(advPayload);
			}
			public int write(String dest, byte[] message) {
				return dummy.write(dest, message);
			}
		});

		//The message layer will hand off messages to this (the top layer) to be printed
		//However, an AdvertisementPayload is passed up then it is sent to LocationManager
		//to handle
		msgL.setReadCB(new Reader() {
			public int read(String src, byte[] message) {
				//From: https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
				final  char[] hexArray = "0123456789ABCDEF".toCharArray();
				byte[] bytes = message;
				char[] hexChars = new char[bytes.length * 2];
			    for ( int j = 0; j < bytes.length; j++ ) {
			        int v = bytes[j] & 0xFF;
			        hexChars[j * 2] = hexArray[v >>> 4];
			        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
			    }
			    System.out.println(hexChars);
				return 0;
			}

			public int read(AdvertisementPayload advPayload) {
				return locMgr.read(advPayload);
			}
		});

		//The location manager writes (mostly) complete AdvertisementPayloads to the
		//message layer to complete and send
		locMgr.setWriteCB(new Writer() {
			public int write(AdvertisementPayload advPayload) {
				return msgL.write(advPayload);
			}
			public int write(String dest, byte[] message) {
				return -1;
			}
		});

		//Example of sending a message to the broadcast group (everyone)
		String msg = new String();
		msg = "hello world!";
		msgL.write(Group.BROADCAST_GROUP, msg);

		//testing out the basic query framework

		System.out.println(myQ.ask("BLE.tag"));
		System.out.println(myQ.ask("LocMgr.tag"));
		System.out.println(myQ.ask("MsgLayer.tag"));

		// Testing out the location manager a bit more
		// top: 41.703799, -86.239010
		// middle: 41.6926321,-86.2445672
		// bottom: 41.681207, -86.228968
		// 
		float latitude = 0.0f;
		float longitude = 0.0f;

		//set location

		myQ.ask("LocMgr.setLocation 41.6926321 -86.2445672");

		//get location

		System.out.println(myQ.ask("LocMgr.getLocation " + myID));

		//broadcast my own location (will be ignored)

		myQ.ask("LocMgr.sendLocation");

		//send location update packets (as if from another node)

		String a = randString.nextString();
		String b = randString.nextString();

		AdvertisementPayload advPayload = new AdvertisementPayload();

		latitude = 41.703799f;
		longitude = -86.239010f;
		byte[] lat = ByteBuffer.allocate(4).putFloat(latitude).array();
		byte[] lon = ByteBuffer.allocate(4).putFloat(longitude).array();

		byte[] header = {(byte)0b11001000};
		byte[] allBytes = new byte[header.length + lat.length + lon.length];
		System.arraycopy(header, 0, allBytes,0,header.length);
		System.arraycopy(lat, 0, allBytes,header.length,lat.length);
		System.arraycopy(lon, 0, allBytes,header.length+lat.length,lon.length);

		//msg.fromBytes(allBytes);
		advPayload.setMsg (allBytes);
		advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
		advPayload.setSrcID(a);
		advPayload.setDestID(Group.BROADCAST_GROUP);
		advPayload.setMsgID((byte)0b0);

		System.out.println("sending a");
		dummy.write(advPayload); //send  update for 'node' a


		advPayload = new AdvertisementPayload();
		latitude = 41.681207f;
		longitude = -86.228968f;
		lat = ByteBuffer.allocate(4).putFloat(latitude).array();
		lon = ByteBuffer.allocate(4).putFloat(longitude).array();

		System.arraycopy(header, 0, allBytes,0,header.length);
		System.arraycopy(lat, 0, allBytes,header.length,lat.length);
		System.arraycopy(lon, 0, allBytes,header.length+lat.length,lon.length);

		//msg.fromBytes(allBytes);
		advPayload.setMsg (allBytes);
		advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
		advPayload.setSrcID(b);
		advPayload.setDestID(Group.BROADCAST_GROUP);
		advPayload.setMsgID((byte)0b0);
		System.out.println("sending b");
		dummy.write(advPayload); //send  update for 'node' b

		//get locations
		System.out.println("a is at: " + myQ.ask("LocMgr.getLocation " + a));
		System.out.println("b is at: " + myQ.ask("LocMgr.getLocation " + b));

		//see if I am in the direction of the dest node: true!

		System.out.println("I'm closer to b than a is: " + myQ.ask("LocMgr.inDirection " + a + " " + b));

		//see if I am in the direction of the dest node: false!

		myQ.ask("LocMgr.setLocation 41.715011 -86.250768");
		System.out.println("I'm closer to b than a is: " + myQ.ask("LocMgr.inDirection " + a + " " + b));
	

		
		//Test the protocol container
		ProtocolContainer proto = new ProtocolContainer();
		proto.regCallback(new Result () {
			public int provide (String src, byte[] data) {
				System.out.print(src + ": ");
				System.out.println(data);
				return 0;
			}
		});

		proto.write(MessageLayer.BROADCAST_GROUP, "proto test: hello world!");


	}
}