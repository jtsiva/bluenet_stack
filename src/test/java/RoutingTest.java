package  nd.edu.bluenet_stack;

import static org.junit.Assert.*;
import org.junit.*;

import java.nio.ByteBuffer;
import java.util.*;
import java.nio.charset.StandardCharsets;


public class RoutingTest {
	private final static String MY_ID = "1111";
	private AdvertisementPayload mAdvPayload = null;
	private RoutingManager mRouteMgr;
	private String mMessage;
	private String mID;

	@Before
	public void setup() {
		mAdvPayload = null;
		mMessage = null;
		mID = null;
		mRouteMgr = new RoutingManager();
		mRouteMgr.setQueryCB(new Query() {
			public String ask(String question) {
				String returnString = "";
				if (Objects.equals(question, "global.id")) {
					returnString = MY_ID;
				}

				return returnString;
			}
		});

		mRouteMgr.setWriteCB(new Writer () {
			public int write(AdvertisementPayload advPayload) {
				mAdvPayload = advPayload;
				return 0;
			}
			public int write(String dest, byte[] message) {
				mMessage = new String(message);
				mID = dest;
				return 0;
			}
		});

		mRouteMgr.setReadCB(new Reader () {
			public int read(AdvertisementPayload advPayload) {
				mAdvPayload = advPayload;
				return 0;
			}

			public int read(String src, byte[] message) {
				mMessage = new String(message);
				mID = src;
				return 0;
			}
		});

	}

	@Test
	public void shouldGetTag() {
		assertEquals("RouteMgr", mRouteMgr.ask("tag"));
	}

	@Test
	public void shouldFwdGroupQuery () {
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setMsgType(AdvertisementPayload.GROUP_QUERY);
		mRouteMgr.read(advPayload);
		assertEquals(mAdvPayload, advPayload);
	}

	@Test
	public void shouldFwdGroupUpdate () {
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setMsgType(AdvertisementPayload.GROUP_UPDATE);
		mRouteMgr.read(advPayload);
		assertEquals(mAdvPayload, advPayload);
	}

	@Test
	public void shouldNotFwdGroupQuery () {
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setMsgType(AdvertisementPayload.GROUP_QUERY);
		advPayload.setSrcID("2222");
		advPayload.decTTL();
		mRouteMgr.read(advPayload);
		assertEquals(null, mAdvPayload);
	}

	@Test
	public void shouldNotFwdGroupUpdate () {
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setMsgType(AdvertisementPayload.GROUP_UPDATE);
		advPayload.setSrcID("2222");
		advPayload.decTTL();
		mRouteMgr.read(advPayload);
		assertEquals(null, mAdvPayload);
	}
	@Test
	public void shouldReceiveMessage(){
		String message = "hello world!";
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setMsgType(AdvertisementPayload.REGULAR_MESSAGE);
		advPayload.setSrcID("2222");
		advPayload.setDestID(MY_ID);
		advPayload.setMsg(message.getBytes(StandardCharsets.UTF_8));
		mRouteMgr.read(advPayload);

		assertEquals(mMessage, message);
		assertEquals(mID, "2222");

	}

	@Test
	public void shouldForwardMessage(){
		String message = "hello world!";
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setMsgType(AdvertisementPayload.REGULAR_MESSAGE);
		advPayload.setSrcID("2222");
		advPayload.setDestID("3333");
		advPayload.setMsg(message.getBytes(StandardCharsets.UTF_8));
		mRouteMgr.read(advPayload);

		assertEquals(mAdvPayload, advPayload);
		assertEquals(6, mAdvPayload.getTTL());
		assertEquals(mMessage, null);
		assertEquals(mID, null);

	}

	private void layerSetup () {
		mRouteMgr.setQueryCB(new Query() {
			public String ask(String question) {
				String returnString = "";
				String [] parts = question.split("\\s+");
				if (Objects.equals(question, "global.id")) {
					returnString = MY_ID;
				}
				else if (Objects.equals(parts[0], "LocMgr.getPositionSpread")) {
				
					returnString = "0.0"; // never a change from position
					
				}

				return returnString;
			}
		});
	}

	@Test
	public void shouldNotForwardLocationNotEnoughData(){ //Need to call at least once to set timestamp
		AdvertisementPayload advPayload = new AdvertisementPayload();

		float latitude = 41.703799f;
		float longitude = -86.239010f;
		byte[] lat = ByteBuffer.allocate(4).putFloat(latitude).array();
		byte[] lon = ByteBuffer.allocate(4).putFloat(longitude).array();

		byte[] allBytes = new byte[lat.length + lon.length+2];
		System.arraycopy(lat, 0, allBytes,0,lat.length);
		System.arraycopy(lon, 0, allBytes,lat.length,lon.length);

		advPayload.setMsg (allBytes);
		advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
		advPayload.setSrcID(MY_ID);
		advPayload.setDestID(Group.BROADCAST_GROUP);
		advPayload.setMsgID((byte)0b0);
		
		layerSetup();
		mRouteMgr.read(advPayload);

		assertEquals(mAdvPayload, null);
		
	}

	@Test
	public void shouldNotForwardLocationNotEnoughTimePassed(){
		AdvertisementPayload advPayload = new AdvertisementPayload();

		float latitude = 41.703799f;
		float longitude = -86.239010f;
		byte[] lat = ByteBuffer.allocate(4).putFloat(latitude).array();
		byte[] lon = ByteBuffer.allocate(4).putFloat(longitude).array();

		byte[] allBytes = new byte[lat.length + lon.length+2];
		System.arraycopy(lat, 0, allBytes,0,lat.length);
		System.arraycopy(lon, 0, allBytes,lat.length,lon.length);

		advPayload.setMsg (allBytes);
		advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
		advPayload.setSrcID(MY_ID);
		advPayload.setDestID(Group.BROADCAST_GROUP);
		advPayload.setMsgID((byte)0b0);
		
		layerSetup();
		mRouteMgr.read(advPayload);
		mRouteMgr.read(advPayload);

		assertEquals(mAdvPayload, null);
	}

	@Test
	public void shouldForwardLocationEnoughTimePassed() throws InterruptedException{
		AdvertisementPayload advPayload = new AdvertisementPayload();

		float latitude = 41.703799f;
		float longitude = -86.239010f;
		byte[] lat = ByteBuffer.allocate(4).putFloat(latitude).array();
		byte[] lon = ByteBuffer.allocate(4).putFloat(longitude).array();

		byte[] allBytes = new byte[lat.length + lon.length+2];
		System.arraycopy(lat, 0, allBytes,0,lat.length);
		System.arraycopy(lon, 0, allBytes,lat.length,lon.length);

		advPayload.setMsg (allBytes);
		advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
		advPayload.setSrcID(MY_ID);
		advPayload.setDestID(Group.BROADCAST_GROUP);
		advPayload.setMsgID((byte)0b1);
		
		layerSetup();
		mRouteMgr.read(advPayload);
		Thread.sleep(5000);
		mRouteMgr.read(advPayload);

		assertEquals(mAdvPayload, advPayload);
	}
}