package  nd.edu.bluenet_stack;

import static org.junit.Assert.*;
import org.junit.*;

import java.nio.ByteBuffer;
import java.util.*;


public class LocationTest {
	private final static String MY_ID = "1111";
	private LocationManager mLocMgr = null;
	private AdvertisementPayload mAdvPayload = null;

	@Before
	public void setup() {
		mLocMgr = new LocationManager();
		mLocMgr.setQueryCB(new Query() {
			public String ask(String question) {
				String returnString = "";
				if (Objects.equals(question, "global.id")) {
					returnString = "1111";
				}

				return returnString;
			}
		});

		mLocMgr.setWriteCB(new Writer () {
			public int write(AdvertisementPayload advPayload) {
				mAdvPayload = advPayload;
				return 0;
			}
			public int write(String dest, byte[] message) {
				throw new java.lang.UnsupportedOperationException("Not supported.");
			}
		});

		mLocMgr.setReadCB(new Reader () {
			public int read(AdvertisementPayload advPayload) {
				mAdvPayload = advPayload;
				return 0;
			}

			public int read(String src, byte[] message) {
				throw new java.lang.UnsupportedOperationException("Not supported.");
			}
		});


	}
	
	@Test
	public void shouldInit() {
		assertNotNull(mLocMgr);
	}

	@Test
	public void shouldReturnTag() {
		assertEquals("LocMgr", mLocMgr.ask("tag"));
	}

	@Test
	public void shouldReturn0ForLocation() {
		String result = mLocMgr.ask("getLocation 1111");
		String[] coord = result.split("\\s+");

		assertEquals(0.0, Float.parseFloat(coord[0]), 0.000001);
		assertEquals(0.0, Float.parseFloat(coord[1]), 0.000001);
	}

	@Test
	public void shouldSendLocation() {
		mLocMgr.ask("sendLocation");

		byte[] message = mAdvPayload.getMsg();

		byte [] latBytes = Arrays.copyOfRange(message,0,4);
		byte [] lonBytes = Arrays.copyOfRange(message,4,8);

		float lat =  ByteBuffer.wrap(latBytes).getFloat();
		float lon =  ByteBuffer.wrap(lonBytes).getFloat();

		
		assertEquals(0.0, lat, 0.000001);
		assertEquals(0.0, lon, 0.000001);
	}

	@Test
	public void shouldSendLocationOnSet() {
		mLocMgr.ask("setLocation 10.0 10.0");

		byte[] message = mAdvPayload.getMsg();

		byte [] latBytes = Arrays.copyOfRange(message,0,4);
		byte [] lonBytes = Arrays.copyOfRange(message,4,8);

		float lat =  ByteBuffer.wrap(latBytes).getFloat();
		float lon =  ByteBuffer.wrap(lonBytes).getFloat();

		
		assertEquals(10.0, lat, 0.000001);
		assertEquals(10.0, lon, 0.000001);
	}

	@Test
	public void shouldSetAndGetSimpleLocation() {
		mLocMgr.ask("setLocation 10.0 10.0");
		String result = mLocMgr.ask("getLocation 1111");

		String[] coord = result.split("\\s+");

		assertEquals(10.0, Float.parseFloat(coord[0]), 0.000001);
		assertEquals(10.0, Float.parseFloat(coord[1]), 0.000001);
	}

	@Test
	public void shouldSetAndGetLocationRealLocation() {
		mLocMgr.ask("setLocation 41.6926321 -86.2445672");
		String result = mLocMgr.ask("getLocation 1111");
		//System.out.println(result);
		
		String[] coord = result.split("\\s+");
		//System.out.println(Float.parseFloat(coord[0]));
		assertEquals(41.6926321, Float.parseFloat(coord[0]), 0.00001);
		assertEquals(-86.2445672, Float.parseFloat(coord[1]), 0.00001);
	}

	@Test
	public void shouldGetFromLocationPayload() {
		String a = "2222";

		AdvertisementPayload advPayload = new AdvertisementPayload();

		float latitude = 41.703799f;
		float longitude = -86.239010f;
		byte[] lat = ByteBuffer.allocate(4).putFloat(latitude).array();
		byte[] lon = ByteBuffer.allocate(4).putFloat(longitude).array();

		byte[] allBytes = new byte[lat.length + lon.length+2];
		System.arraycopy(lat, 0, allBytes,0,lat.length);
		System.arraycopy(lon, 0, allBytes,lat.length,lon.length);

		//msg.fromBytes(allBytes);
		advPayload.setMsg (allBytes);
		advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
		advPayload.setSrcID(a);
		advPayload.setDestID(Group.BROADCAST_GROUP);
		advPayload.setMsgID((byte)0b0);

		mLocMgr.read(advPayload);

		String result = mLocMgr.ask("getLocation 2222");
		//System.out.println(result);
		
		String[] coord = result.split("\\s+");
		//System.out.println(Float.parseFloat(coord[0]));
		assertEquals(41.703799f, Float.parseFloat(coord[0]), 0.00001);
		assertEquals(-86.239010f, Float.parseFloat(coord[1]), 0.00001);
	}

	@Test
	public void shouldBeInDirection() {
		//set location

		mLocMgr.ask("setLocation 41.6926321 -86.2445672");

		//send location update packets (as if from another node)

		String a = "2222";
		String b = "3333";

		AdvertisementPayload advPayload = new AdvertisementPayload();

		float latitude = 41.703799f;
		float longitude = -86.239010f;
		byte[] lat = ByteBuffer.allocate(4).putFloat(latitude).array();
		byte[] lon = ByteBuffer.allocate(4).putFloat(longitude).array();

		byte[] allBytes = new byte[lat.length + lon.length+2];
		System.arraycopy(lat, 0, allBytes,0,lat.length);
		System.arraycopy(lon, 0, allBytes,lat.length,lon.length);

		//msg.fromBytes(allBytes);
		advPayload.setMsg (allBytes);
		advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
		advPayload.setSrcID(a);
		advPayload.setDestID(Group.BROADCAST_GROUP);
		advPayload.setMsgID((byte)0b0);

		mLocMgr.read(advPayload);


		advPayload = new AdvertisementPayload();
		latitude = 41.681207f;
		longitude = -86.228968f;
		lat = ByteBuffer.allocate(4).putFloat(latitude).array();
		lon = ByteBuffer.allocate(4).putFloat(longitude).array();

		
		System.arraycopy(lat, 0, allBytes,0,lat.length);
		System.arraycopy(lon, 0, allBytes,lat.length,lon.length);

		//msg.fromBytes(allBytes);
		advPayload.setMsg (allBytes);
		advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
		advPayload.setSrcID(b);
		advPayload.setDestID(Group.BROADCAST_GROUP);
		advPayload.setMsgID((byte)0b0);

		mLocMgr.read(advPayload);

		assertEquals("true", mLocMgr.ask("inDirection " + a + " " + b));
	}

	@Test
	public void shouldNotBeInDirection () {
		mLocMgr.ask("setLocation 41.715011 -86.250768");

		//send location update packets (as if from another node)

		String a = "2222";
		String b = "3333";

		AdvertisementPayload advPayload = new AdvertisementPayload();

		float latitude = 41.703799f;
		float longitude = -86.239010f;
		byte[] lat = ByteBuffer.allocate(4).putFloat(latitude).array();
		byte[] lon = ByteBuffer.allocate(4).putFloat(longitude).array();

		byte[] allBytes = new byte[lat.length + lon.length+2];
		System.arraycopy(lat, 0, allBytes,0,lat.length);
		System.arraycopy(lon, 0, allBytes,lat.length,lon.length);

		//msg.fromBytes(allBytes);
		advPayload.setMsg (allBytes);
		advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
		advPayload.setSrcID(a);
		advPayload.setDestID(Group.BROADCAST_GROUP);
		advPayload.setMsgID((byte)0b0);

		mLocMgr.read(advPayload);


		advPayload = new AdvertisementPayload();
		latitude = 41.681207f;
		longitude = -86.228968f;
		lat = ByteBuffer.allocate(4).putFloat(latitude).array();
		lon = ByteBuffer.allocate(4).putFloat(longitude).array();

		
		System.arraycopy(lat, 0, allBytes,0,lat.length);
		System.arraycopy(lon, 0, allBytes,lat.length,lon.length);

		//msg.fromBytes(allBytes);
		advPayload.setMsg (allBytes);
		advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
		advPayload.setSrcID(b);
		advPayload.setDestID(Group.BROADCAST_GROUP);
		advPayload.setMsgID((byte)0b0);

		mLocMgr.read(advPayload);

		assertEquals("false", mLocMgr.ask("inDirection " + a + " " + b));
	}

	@Test
	public void shouldGetNeighbors() {
		String a = "2222";
		String b = "3333";

		AdvertisementPayload advPayload = new AdvertisementPayload();

		float latitude = 41.703799f;
		float longitude = -86.239010f;
		byte[] lat = ByteBuffer.allocate(4).putFloat(latitude).array();
		byte[] lon = ByteBuffer.allocate(4).putFloat(longitude).array();

		byte[] allBytes = new byte[lat.length + lon.length+2];
		System.arraycopy(lat, 0, allBytes,0,lat.length);
		System.arraycopy(lon, 0, allBytes,lat.length,lon.length);

		//msg.fromBytes(allBytes);
		advPayload.setMsg (allBytes);
		advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
		advPayload.setSrcID(a);
		advPayload.setDestID(Group.BROADCAST_GROUP);
		advPayload.setMsgID((byte)0b0);

		mLocMgr.read(advPayload);


		advPayload = new AdvertisementPayload();
		latitude = 41.681207f;
		longitude = -86.228968f;
		lat = ByteBuffer.allocate(4).putFloat(latitude).array();
		lon = ByteBuffer.allocate(4).putFloat(longitude).array();

		
		System.arraycopy(lat, 0, allBytes,0,lat.length);
		System.arraycopy(lon, 0, allBytes,lat.length,lon.length);

		//msg.fromBytes(allBytes);
		advPayload.setMsg (allBytes);
		advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
		advPayload.setSrcID(b);
		advPayload.setDestID(Group.BROADCAST_GROUP);
		advPayload.setMsgID((byte)0b0);

		mLocMgr.read(advPayload);

		String res = mLocMgr.ask("getNeighbors");
		String[]parts = res.split("\\s+");
		assertTrue(Objects.equals(parts[0], "2222") || Objects.equals(parts[1], "2222"));
		assertTrue(Objects.equals(parts[0], "3333") || Objects.equals(parts[1], "3333"));
	}

	@Test
	public void shouldReturnEmptyNeighborsNothingSet() {
		String res = mLocMgr.ask("getNeighbors");
		assertEquals(null, res);
	}

	@Test
	public void shouldReturnEmptyNeighborsMeSetSet() {
		mLocMgr.ask("setLocation 41.715011 -86.250768");
		String res = mLocMgr.ask("getNeighbors");
		assertEquals(null, res);
	}

	@Test
	public void shouldOnlyGetNeighbors() {
		mLocMgr.ask("setLocation 41.715011 -86.250768");

		String a = "2222";
		String b = "3333";

		AdvertisementPayload advPayload = new AdvertisementPayload();

		float latitude = 41.703799f;
		float longitude = -86.239010f;
		byte[] lat = ByteBuffer.allocate(4).putFloat(latitude).array();
		byte[] lon = ByteBuffer.allocate(4).putFloat(longitude).array();

		byte[] allBytes = new byte[lat.length + lon.length+2];
		System.arraycopy(lat, 0, allBytes,0,lat.length);
		System.arraycopy(lon, 0, allBytes,lat.length,lon.length);

		//msg.fromBytes(allBytes);
		advPayload.setMsg (allBytes);
		advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
		advPayload.setSrcID(a);
		advPayload.setDestID(Group.BROADCAST_GROUP);
		advPayload.setMsgID((byte)0b0);

		mLocMgr.read(advPayload);


		advPayload = new AdvertisementPayload();
		latitude = 41.681207f;
		longitude = -86.228968f;
		lat = ByteBuffer.allocate(4).putFloat(latitude).array();
		lon = ByteBuffer.allocate(4).putFloat(longitude).array();

		
		System.arraycopy(lat, 0, allBytes,0,lat.length);
		System.arraycopy(lon, 0, allBytes,lat.length,lon.length);

		//msg.fromBytes(allBytes);
		advPayload.setMsg (allBytes);
		advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
		advPayload.setSrcID(b);
		advPayload.setDestID(Group.BROADCAST_GROUP);
		advPayload.setMsgID((byte)0b0);

		mLocMgr.read(advPayload);

		String res = mLocMgr.ask("getNeighbors");
		String[]parts = res.split("\\s+");
		assertTrue(Objects.equals(parts[0], "2222") || Objects.equals(parts[1], "2222"));
		assertTrue(Objects.equals(parts[0], "3333") || Objects.equals(parts[1], "3333"));
	}

	@Test
	public void shouldReturn0PositionSpreadWithNoPositionSet () {
		//mLocMgr.ask("setLocation 41.715011 -86.250768");

		String result = mLocMgr.ask("getPositionSpread 1111");
		//System.out.println(result);
		
		String[] res = result.split("\\s+");
		//System.out.println(Float.parseFloat(coord[0]));
		assertEquals(0.0f, Float.parseFloat(res[0]), 0.00001);

	}

	@Test
	public void shouldReturn0PositionSpreadWithOnePositionSet () {
		mLocMgr.ask("setLocation 41.715011 -86.250768");

		String result = mLocMgr.ask("getPositionSpread 1111");
		//System.out.println(result);
		
		String[] res = result.split("\\s+");
		//System.out.println(Float.parseFloat(coord[0]));
		assertEquals(0.0f, Float.parseFloat(res[0]), 0.00001);

	}

	@Test
	public void shouldReturnPositionSpreadWithTwoLocs () {
		//values based on estimation formula: https://gis.stackexchange.com/questions/2951/algorithm-for-offsetting-a-latitude-longitude-by-some-amount-of-meters
		mLocMgr.ask("setLocation 1.0 1.0");
		mLocMgr.ask("setLocation 1.0009 1.0");

		//NOTE: using meanSquaredDisplacemet
		String result = mLocMgr.ask("getPositionSpread 1111");
		//System.out.println(result);
		
		String[] res = result.split("\\s+");
		//System.out.println(Float.parseFloat(coord[0]));
		assertEquals(5000.0f, Float.parseFloat(res[0]), 10.0);

	}

	@Test
	public void shouldReturnPositionSpreadWithFullWindow () {
		//values based on estimation formula: https://gis.stackexchange.com/questions/2951/algorithm-for-offsetting-a-latitude-longitude-by-some-amount-of-meters
		mLocMgr.ask("setLocation 1.0 1.0");
		mLocMgr.ask("setLocation 1.0009 1.0"); //100*100
		mLocMgr.ask("setLocation 1.0009 0.999099"); //141.42*141.42
		mLocMgr.ask("setLocation 1.0 0.999099"); //100*100
		mLocMgr.ask("setLocation 1.0 1.0"); //0
		//---------------------------------------------------------
		//sum
		//    /= 5
		// ~8000

		//NOTE: using meanSquaredDisplacemet
		String result = mLocMgr.ask("getPositionSpread 1111");
		//System.out.println(result);
		
		String[] res = result.split("\\s+");
		//System.out.println(Float.parseFloat(coord[0]));
		assertEquals(8000.0f, Float.parseFloat(res[0]), 50.0);

	}

	@Test
	public void shouldNotDeleteNeighbor() {
		String a = "2222";

		AdvertisementPayload advPayload = new AdvertisementPayload();

		float latitude = 41.703799f;
		float longitude = -86.239010f;
		byte[] lat = ByteBuffer.allocate(4).putFloat(latitude).array();
		byte[] lon = ByteBuffer.allocate(4).putFloat(longitude).array();

		byte[] allBytes = new byte[lat.length + lon.length+2];
		System.arraycopy(lat, 0, allBytes,0,lat.length);
		System.arraycopy(lon, 0, allBytes,lat.length,lon.length);

		//msg.fromBytes(allBytes);
		advPayload.setMsg (allBytes);
		advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
		advPayload.setSrcID(a);
		advPayload.setDestID(Group.BROADCAST_GROUP);
		advPayload.setMsgID((byte)0b0);

		mLocMgr.read(advPayload);

		mLocMgr.ask("cleanNeighbors 10000");
		String res = mLocMgr.ask("getNeighbors");

		String[] ids = null;
        if (null != res) {
            ids = res.split("\\s+");
        }

        assertTrue(0 < ids.length);
	}

	@Test
	public void shouldDeleteNeighbor() {
		String a = "2222";

		AdvertisementPayload advPayload = new AdvertisementPayload();

		float latitude = 41.703799f;
		float longitude = -86.239010f;
		byte[] lat = ByteBuffer.allocate(4).putFloat(latitude).array();
		byte[] lon = ByteBuffer.allocate(4).putFloat(longitude).array();

		byte[] allBytes = new byte[lat.length + lon.length+2];
		System.arraycopy(lat, 0, allBytes,0,lat.length);
		System.arraycopy(lon, 0, allBytes,lat.length,lon.length);

		//msg.fromBytes(allBytes);
		advPayload.setMsg (allBytes);
		advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
		advPayload.setSrcID(a);
		advPayload.setDestID(Group.BROADCAST_GROUP);
		advPayload.setMsgID((byte)0b0);

		mLocMgr.read(advPayload);
		try {                 
			Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

		mLocMgr.ask("cleanNeighbors 100");

		mLocMgr.ask("getNeighbors");

		String res = mLocMgr.ask("getNeighbors");

		String[] ids = new String[0];
        if (null != res) {
            ids = res.split("\\s+");
        }

        assertEquals (0, ids.length);
	}
}