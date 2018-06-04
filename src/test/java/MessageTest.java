package  nd.edu.bluenet_stack;

import static org.junit.Assert.*;
import org.junit.*;

import java.nio.ByteBuffer;
import java.util.*;


public class MessageTest {
	private final static String MY_ID = "1111";
	private AdvertisementPayload mAdvPayload = null;
	private MessageLayer msgL;

	@Before
	public void setup() {
		msgL = new MessageLayer();

		msgL.setQueryCB(new Query() {
			public String ask(String question) {
				String returnString = "";
				if (Objects.equals(question, "global.id")) {
					returnString = MY_ID;
				}

				return returnString;
			}
		});

		msgL.setReadCB(new Reader() {
			public int read(AdvertisementPayload advPayload) {
				mAdvPayload = advPayload;
				return 0;
			}

			public int read(String src, byte[] message) {
				throw new java.lang.UnsupportedOperationException("Not supported.");
			}
		});

		msgL.setWriteCB(new Writer() {
			public int write(AdvertisementPayload advPayload) {
				mAdvPayload = advPayload;
				return 0;
			}
			public int write(String dest, byte[] message) {
				throw new java.lang.UnsupportedOperationException("Not supported.");
			}
		});
	}

	@Test
	public void shouldGetTag() {
		assertEquals("MsgLayer", msgL.ask("tag"));
	}

	@Test
	public void shouldProvideAdvPayload() {
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setSrcID("");
		msgL.write(advPayload);
		assertEquals(advPayload, mAdvPayload);
	}

	@Test
	public void shouldIncrementMsgID() {
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setSrcID(MY_ID);
		msgL.write(advPayload);
		msgL.write(advPayload);
		assertEquals(1, mAdvPayload.getMsgID());
	}

	@Test
	public void shouldNotIncrementMsgID() {
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setSrcID("2222");
		advPayload.setMsgID((byte)5);
		msgL.write(advPayload);
		msgL.write(advPayload);
		assertEquals(5, mAdvPayload.getMsgID());
	}

	@Test
	public void shouldBeGoodSmallMessage() {
		String msg = "hello!";
		
		msgL.write("2222", msg.getBytes());
		assertEquals(MY_ID, new String(mAdvPayload.getSrcID()));
		assertEquals("2222", new String(mAdvPayload.getDestID()));
		assertEquals(0, mAdvPayload.getMsgID());
		assertTrue(Arrays.equals(msg.getBytes(), mAdvPayload.getMsg()));
		assertEquals(AdvertisementPayload.SMALL_MESSAGE, mAdvPayload.getMsgType());
	}

	@Test
	public void shouldBeGoodRegularMessage() {
		String msg = "hello world!";
		
		msgL.write("2222", msg.getBytes());
		assertEquals(MY_ID, new String(mAdvPayload.getSrcID()));
		assertEquals("2222", new String(mAdvPayload.getDestID()));
		assertEquals(0, mAdvPayload.getMsgID());
		assertTrue(Arrays.equals(msg.getBytes(), mAdvPayload.getMsg()));
		assertEquals(AdvertisementPayload.REGULAR_MESSAGE, mAdvPayload.getMsgType());
	}

	@Test
	public void shouldAcceptMsg() {
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setSrcID("2222");
		advPayload.setMsgID((byte)1);
		msgL.read(advPayload);
		assertEquals("2222", new String(mAdvPayload.getSrcID()));
		assertEquals(1, mAdvPayload.getMsgID());
	}

	@Test
	public void shouldAcceptSameSrcDiffMsgID() {
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setSrcID(MY_ID);
		advPayload.setMsgID((byte)0);
		msgL.read(advPayload);
		advPayload.setMsgID((byte)1);
		msgL.read(advPayload);
		
		assertEquals(advPayload, mAdvPayload);
	}

	@Test
	public void shouldAcceptSameMsgIDDiffSrc() {
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setSrcID(MY_ID);
		advPayload.setMsgID((byte)0);
		msgL.read(advPayload);
		advPayload.setSrcID("2222");
		msgL.read(advPayload);
		
		assertEquals(advPayload, mAdvPayload);
	}

	@Test
	public void shouldRejectSameMsgIDSameSrc() {
		AdvertisementPayload advPayload = new AdvertisementPayload();
		AdvertisementPayload advPayload2 = new AdvertisementPayload();
		String msg = "hello";
		advPayload.setSrcID(MY_ID);
		advPayload.setMsgID((byte)0);
		advPayload.setMsg(msg.getBytes());
		msgL.read(advPayload);
		advPayload2.setSrcID(MY_ID);
		advPayload2.setMsgID((byte)0);
		msg = "world";
		advPayload2.setMsg(msg.getBytes());
		msgL.read(advPayload2);
		
		//should not have set to second message because of duplicate msg_id and src
		assertEquals("hello", new String(mAdvPayload.getMsg()));
	}

	@Test
	public void shouldAcceptSameOutsideFilterRange() {

		for (int i = 0; i <= msgL.filterWindowSize; i++) {
			AdvertisementPayload advPayload = new AdvertisementPayload();
			advPayload.setSrcID(MY_ID);
			advPayload.setMsgID((byte)(i % msgL.filterWindowSize));
			advPayload.setMsg(new byte[] {(byte)(i % msgL.filterWindowSize)});
			msgL.read(advPayload);
		}
		
		assertEquals(0, mAdvPayload.getMsg()[0]);
	}
}