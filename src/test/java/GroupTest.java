package  nd.edu.bluenet_stack;

import static org.junit.Assert.*;
import org.junit.*;

import java.nio.ByteBuffer;
import java.util.*;


public class GroupTest {
	private final static String MY_ID = "1111";
	private AdvertisementPayload mAdvPayload = null;
	private GroupManager mGrpMgr;

	@Before
	public void setup() {
		mGrpMgr = new GroupManager();
		mGrpMgr.setQueryCB(new Query() {
			public String ask(String question) {
				String returnString = "";
				if (Objects.equals(question, "global.id")) {
					returnString = "1111";
				}

				return returnString;
			}
		});

		mGrpMgr.setWriteCB(new Writer () {
			public int write(AdvertisementPayload advPayload) {
				mAdvPayload = advPayload;
				return 0;
			}
			public int write(String dest, byte[] message) {
				throw new java.lang.UnsupportedOperationException("Not supported.");
			}
		});

		mGrpMgr.setReadCB(new Reader () {
			public int read(AdvertisementPayload advPayload) {
				mAdvPayload = advPayload;
				return 0;
			}

			public int read(String src, byte[] message) {
				throw new java.lang.UnsupportedOperationException("Not supported.");
			}
		});
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

	//Named group

	@Test
	public void shouldSetupMinimalNamedGroup() {
		NamedGroup grp = new NamedGroup(MY_ID);
		assertEquals(MY_ID, new String(grp.getID()));
		assertEquals(Group.NAMED_GROUP, grp.getType());
	}

	@Test
	public void shouldSetupNamedGroup() {
		NamedGroup grp = new NamedGroup(MY_ID, "awesome_group");
		assertEquals(MY_ID, new String(grp.getID()));
		assertEquals(Group.NAMED_GROUP, grp.getType());
		assertEquals("awesome_group", grp.getName());
	}

	// Geo group

	public void shouldSetupMinimalGeoGroup() {
		GeoGroup grp = new GeoGroup(MY_ID);
		assertEquals(MY_ID, new String(grp.getID()));
		assertEquals(Group.GEO_GROUP, grp.getType());
		assertEquals(0.0, grp.getLatitude(), .00001);
		assertEquals(0.0, grp.getLongitude(), .00001);
		assertEquals(0.0, grp.getRadius(), .00001);
	}

	@Test
	public void shouldSetupGeoGroup() {
		GeoGroup grp = new GeoGroup(MY_ID, 10.005f,-35.12345f,15f);
		assertEquals(MY_ID, new String(grp.getID()));
		assertEquals(Group.GEO_GROUP, grp.getType());
		assertEquals(10.005, grp.getLatitude(), .00001);
		assertEquals(-35.12345, grp.getLongitude(), .00001);
		assertEquals(15.0, grp.getRadius(), .00001);
	}

	@Test
	public void shouldJoinGeoGroup() {
		GeoGroup grp = new GeoGroup(MY_ID, 10.005f,-35.12345f,15f);
		grp.join(10.005f,-35.12354f);
		assertTrue(grp.getStatus());
	}

	@Test
	public void shouldNotJoinGeoGroup() {
		GeoGroup grp = new GeoGroup(MY_ID, 10.005f,-35.12345f,15f);
		grp.join(11f, -35.12345f);
		assertTrue(!grp.getStatus());
	}

	// Group manager

	@Test
	public void shouldInit() {
		assertNotNull(mGrpMgr);
	}

	@Test
	public void shouldReturnTag() {
		assertEquals("GrpMgr", mGrpMgr.query("tag"));
	}
}