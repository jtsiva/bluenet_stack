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
		assertEquals("RouteMgr", mRouteMgr.query("tag"));
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
		assertEquals(2, mAdvPayload.getTTL());
		assertEquals(mMessage, null);
		assertEquals(mID, null);

	}
}