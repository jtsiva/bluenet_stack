package  nd.edu.bluenet_stack;

import static org.junit.Assert.*;
import org.junit.*;

import java.nio.ByteBuffer;
import java.util.*;


public class StackTest { //Using protocol container?
	private ProtocolContainer stack;
	private String mID;
	private String mData;

	@Before
	public void setup() {
		mID = "";
		mData = "";
		stack = new ProtocolContainer();
		stack.regCallback(new Result() {
			public int provide(String src, byte[] data) {
				mID = src;
				mData = new String(data);
				return 0;
			}
		});
	}

	@Test
	public void shouldGetID() {
		assertNotNull(stack.getMyID());
		assertEquals(4, stack.getMyID().length());
	}

	@Test
	public void shouldWriteShortMessage() {
		String msg = "hello";
		stack.write(stack.getMyID(), msg);
		assertEquals(msg, mData);
		assertEquals(stack.getMyID(), mID);
	}

	@Ignore//@Test
	public void shouldWriteLongMessage() {
		String msg = "This is a little bit longer message with a more text and stuff. I'll even add in some punctuation!";
		stack.write(stack.getMyID(), msg);
		assertEquals(msg, mData);
		assertEquals(stack.getMyID(), mID);
	}
}