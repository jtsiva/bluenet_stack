package  nd.edu.bluenet_stack;

import static org.junit.Assert.*;
import org.junit.*;

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
		assertEquals("LocMgr", mLocMgr.query("tag"));
	}

	@Test
	public void shouldSetAndGetSimpleLocation() {
		mLocMgr.query("setLocation 10.0 10.0");
		String result = mLocMgr.query("getLocation 1111");

		String[] coord = result.split("\\s+");

		assertEquals(10.0, Float.parseFloat(coord[0]), 0.000001);
		assertEquals(10.0, Float.parseFloat(coord[1]), 0.000001);
	}

	@Test
	public void shouldSetAndGetLocationRealLocation() {
		mLocMgr.query("setLocation 41.6926321 -86.2445672");
		String result = mLocMgr.query("getLocation 1111");
		//System.out.println(result);
		
		String[] coord = result.split("\\s+");
		//System.out.println(Float.parseFloat(coord[0]));
		assertEquals(41.6926321, Float.parseFloat(coord[0]), 0.00001);
		assertEquals(-86.2445672, Float.parseFloat(coord[1]), 0.00001);
	}

	@Ignore("not there yet!")
	public void shouldCreateNewEntry() {
		assertTrue(false);
	}
}