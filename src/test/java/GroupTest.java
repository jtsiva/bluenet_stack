package  nd.edu.bluenet_stack;

import static org.junit.Assert.*;
import org.junit.*;

import java.nio.ByteBuffer;
import java.util.*;


public class GroupTest {
	private final static String MY_ID = "1111";
	private AdvertisementPayload mAdvPayload = null;

	@Before
	public void setup() {
	}

	//Base group tests

	@Test
	public void shouldSetupMinimalDefaultGroup() {
		Group grp = new Group();
		assertEquals(Group.BROADCAST_GROUP, new String(grp.getID()));
		assertEquals(Group.NONE, grp.getType());
		assertTrue(!grp.getStatus());
	}

	@Test
	public void shouldSetupMinimalGroup() {
		Group grp = new Group(MY_ID, Group.NAMED_GROUP);
		assertEquals(MY_ID, new String(grp.getID()));
		assertEquals(Group.NAMED_GROUP, grp.getType());
		assertTrue(!grp.getStatus());
	}

	@Test
	public void shouldJoinAndLeaveGroup() {
		Group grp = new Group();

		grp.join();
		assertTrue(grp.getStatus());
		grp.leave();
		assertTrue(!grp.getStatus());
	}
}