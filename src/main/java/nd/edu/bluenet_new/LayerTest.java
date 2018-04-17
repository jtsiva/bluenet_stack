package nd.edu.bluenet_new;

import java.util.*;

public class LayerTest {

	static private ArrayList<LayerIFace> mLayers = new ArrayList<>(); 
	
	public static void main(String[] args){

		System.out.println("setting things up...");
		Message msg = new Message();

		MessageLayer msgL = new MessageLayer();
		LocationManager locMgr = new LocationManager();
		DummyBLE dummy = new DummyBLE();

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
				String[] parts = question.split("\\.");

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

		msgL.setQueryCB(myQ);
		locMgr.setQueryCB(myQ);
		dummy.setQueryCB(myQ);


		dummy.setReadCB(new Reader() {
			public int read(AdvertisementPayload advPayload) {
				return msgL.read(advPayload);
			}

			public int read(Message message) {
				return -1;
			}
		});

		msgL.setWriteCB(new Writer() {
			public int write(AdvertisementPayload advPayload) {
				return dummy.write(advPayload);
			}
			public int write(Message message) {
				return dummy.write(message);
			}
		});

		msgL.setReadCB(new Reader() {
			public int read(Message message) {
				//From: https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
				final  char[] hexArray = "0123456789ABCDEF".toCharArray();
				byte[] bytes = message.getBytes();
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
				return -1;
			}
		});
		msg.setData("hello world!");
		msgL.write(msg);

		System.out.println(myQ.ask("BLE.tag"));
		System.out.println(myQ.ask("LocMgr.tag"));
		System.out.println(myQ.ask("MsgLayer.tag"));
		System.out.println(myQ.ask("LocMgr.inDirection"));


	}
}