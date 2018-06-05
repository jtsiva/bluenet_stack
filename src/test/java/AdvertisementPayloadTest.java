package  nd.edu.bluenet_stack;

import static org.junit.Assert.*;
import org.junit.*;

import java.nio.ByteBuffer;
import java.util.*;
import java.nio.charset.StandardCharsets;



public class AdvertisementPayloadTest {

	@Before
	public void setup() {
	}

	@Test
	public void shouldEqualWhenSameObject() {
		AdvertisementPayload adv1 = new AdvertisementPayload();
		AdvertisementPayload adv2 = adv1;
		assertEquals(adv1, adv2);
	}

	@Test
	public void shouldNotEqualIfOneIsNull() {
		AdvertisementPayload adv1 = new AdvertisementPayload ();
		AdvertisementPayload adv2 = null;
		assertTrue(adv1 != adv2);
	}

	@Test
	public void shouldNotEqualWhenSrcIDNotSame() {
		AdvertisementPayload adv1 = new AdvertisementPayload();
		AdvertisementPayload adv2 = new AdvertisementPayload();
		adv1.setSrcID("1111");
		adv2.setSrcID("2222");
		assertTrue(adv1 != adv2);
	}

	@Test
	public void shouldNotEqualWhenMsgIDNotSame() {
		AdvertisementPayload adv1 = new AdvertisementPayload();
		AdvertisementPayload adv2 = new AdvertisementPayload();
		adv1.setMsgID((byte)0);
		adv2.setMsgID((byte)1);
		assertTrue(adv1 != adv2);
	}

	@Test
	public void shouldEqualWhenMsgIDandSrcIDTheSame() {
		AdvertisementPayload adv1 = new AdvertisementPayload();
		AdvertisementPayload adv2 = new AdvertisementPayload();
		adv1.setSrcID("1111");
		adv2.setSrcID("1111");
		adv1.setMsgID((byte)1);
		adv2.setMsgID((byte)1);
		assertEquals(adv1, adv2);
	}

	@Test
	public void shouldParsePushedByteString() {
		//CCCC, DDDD, 1, ttl=2, hp=1, len=8, hello!!!
		byte [] data = new byte[] {(byte)0x43,(byte)0x43,(byte)0x43,(byte)0x43,(byte)0x44,(byte)0x44,(byte)0x44,(byte)0x44,(byte)0x01,(byte)0x50, (byte)0x08,(byte)0x68,(byte)0x65,(byte)0x6C,(byte)0x6C,(byte)0x6F,(byte)0x21,(byte)0x21,(byte)0x21};
	
		AdvertisementPayload adv = new AdvertisementPayload();
		assertTrue(adv.fromBytes(data));

		assertEquals("CCCC", new String(adv.getSrcID()));
		assertEquals("DDDD", new String(adv.getDestID()));
		assertEquals(1, adv.getMsgID());
		assertEquals(2, adv.getTTL());
		assertTrue(adv.isHighPriority());
		assertEquals("hello!!!", new String(adv.getMsg()));
	}

	@Test
	public void shouldParsePulledByteString() {
		//CCCC, DDDD, 1, ttl=2, hp=1, len=8
		byte [] data = new byte[] {(byte)0x43,(byte)0x43,(byte)0x43,(byte)0x43,(byte)0x44,(byte)0x44,(byte)0x44,(byte)0x44,(byte)0x01,(byte)0x50, (byte)0x08};
	
		// hello!!!
		byte [] msg = new byte[] {(byte)0x68,(byte)0x65,(byte)0x6C,(byte)0x6C,(byte)0x6F,(byte)0x21,(byte)0x21,(byte)0x21};

		AdvertisementPayload adv = new AdvertisementPayload();
		adv.setRetriever(new MessageRetriever () {
			public byte[] retrieve(byte [] srcID) {
				return msg;
			}
		});
		assertTrue(adv.fromBytes(data));

		assertEquals("CCCC", new String(adv.getSrcID()));
		assertEquals("DDDD", new String(adv.getDestID()));
		assertEquals(1, adv.getMsgID());
		assertEquals(2, adv.getTTL());
		assertTrue(adv.isHighPriority());
		assertEquals("hello!!!", new String(adv.getMsg()));
	}

	@Test
	public void shouldFailToParsePushedMessageShort() {
		//CCCC, DDDD, 1, ttl=2, hp=1, len=8, hello!!!
		byte [] data = new byte[] {(byte)0x43,(byte)0x43,(byte)0x43,(byte)0x43,(byte)0x44,(byte)0x44,(byte)0x44,(byte)0x44,(byte)0x01,(byte)0x50, (byte)0x08,(byte)0x68,(byte)0x65,(byte)0x6C,(byte)0x6C,(byte)0x6F,(byte)0x21,(byte)0x21};
	
		AdvertisementPayload adv = new AdvertisementPayload();
		assertTrue(!adv.fromBytes(data));
	}

	@Test
	public void shouldFailToParsePushedMessageLarge() {
		//CCCC, DDDD, 1, ttl=2, hp=1, len=8, hello!!!
		byte [] data = new byte[] {(byte)0x43,(byte)0x43,(byte)0x43,(byte)0x43,(byte)0x44,(byte)0x44,(byte)0x44,(byte)0x44,(byte)0x01,(byte)0x50, (byte)0x08,(byte)0x68,(byte)0x65,(byte)0x6C,(byte)0x6C,(byte)0x6F,(byte)0x21,(byte)0x21,(byte)0x21,(byte)0x21};
	
		AdvertisementPayload adv = new AdvertisementPayload();
		assertTrue(!adv.fromBytes(data));
	}

	@Test
	public void shouldGetBytesAndParseAgain() {
		//CCCC, DDDD, 1, ttl=2, hp=1, len=8, hello!!!	
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setSrcID("CCCC");
		advPayload.setDestID("DDDD");
		advPayload.setMsgID((byte)1);
		advPayload.decTTL();
		advPayload.setHighPriority(true);
		String msg = "hello! this a little bit longer message!";
		advPayload.setMsg(msg.getBytes(StandardCharsets.UTF_8));

		//System.out.println(String.valueOf(advPayload.getMsgLength()));

		AdvertisementPayload payload = new AdvertisementPayload();
		byte[] tmpA = advPayload.getHeader();
		byte[] tmpB = advPayload.getMsg();
		//System.out.println(String.valueOf(tmpA[10]));

		byte[] all = new byte[tmpA.length + tmpB.length];
		System.arraycopy(tmpA, 0, all, 0, tmpA.length);
		System.arraycopy(tmpB, 0, all, tmpA.length, tmpB.length);

		assertTrue(payload.fromBytes(all));

		//System.out.println(String.valueOf(payload.getMsgLength()));

		assertEquals("CCCC", new String(payload.getSrcID()));
		assertEquals("DDDD", new String(payload.getDestID()));
		assertEquals(1, payload.getMsgID());
		assertEquals(6, payload.getTTL());
		assertTrue(payload.isHighPriority());
		assertEquals(msg, new String(payload.getMsg()));
	}

	@Test
	public void shouldGetHeaderBytes() {
		AdvertisementPayload adv = new AdvertisementPayload();
		String src = "AAAA";
		String dest = "BBBB";
		adv.setSrcID(src);
		adv.setDestID(dest);
		adv.setMsgID((byte)1);
		adv.setHighPriority(true);
		byte [] msg = new byte[] {(byte)0x68,(byte)0x65,(byte)0x6C,(byte)0x6C,(byte)0x6F,(byte)0x21,(byte)0x21,(byte)0x21};
		adv.setMsg(msg);

		byte [] header = adv.getHeader();

		assertTrue(Arrays.equals(src.getBytes(StandardCharsets.UTF_8), Arrays.copyOfRange(header, 0, 4)));
		assertTrue(Arrays.equals(dest.getBytes(StandardCharsets.UTF_8), Arrays.copyOfRange(header, 4, 8)));
		assertEquals(1, header[8]);
		assertTrue(adv.isHighPriority());
		assertEquals(header[10], adv.getMsgLength());
	}

	@Test
	public void shouldReturn0ForMsgTypeByDefault () {
		AdvertisementPayload adv = new AdvertisementPayload();

		assertEquals(0, adv.getMsgType());
	}

	@Test
	public void shouldSetAndGetMsgType () {
		AdvertisementPayload adv = new AdvertisementPayload();
		adv.setMsgType(AdvertisementPayload.REGULAR_MESSAGE);
		assertEquals(AdvertisementPayload.REGULAR_MESSAGE, adv.getMsgType());
	}

	@Test
	public void shouldDecTTL () {
		AdvertisementPayload adv = new AdvertisementPayload();
		adv.decTTL();

		assertEquals(6, adv.getTTL());
	}

	@Test
	public void shouldSetAndGetOneHopNeighbor () {
		AdvertisementPayload adv = new AdvertisementPayload();
		adv.setOneHopNeighbor("CCCC");
		assertEquals("CCCC", new String(adv.getOneHopNeighbor()));
	}
}