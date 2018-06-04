package  nd.edu.bluenet_stack;

import static org.junit.Assert.*;
import org.junit.*;

import java.nio.ByteBuffer;
import java.util.*;

import java.nio.charset.StandardCharsets;


public class GroupTest {
	private final static String MY_ID = "1111";
	private List<AdvertisementPayload> mAdvPayloads = new ArrayList<AdvertisementPayload>();
	private String mMessage = "";
	private GroupManager mGrpMgr;
	private int id = 0;

	@Before
	public void setup() {
		mAdvPayloads = new ArrayList<AdvertisementPayload>();
		mMessage = "";
		id = 0;
		mGrpMgr = new GroupManager();
		mGrpMgr.setQueryCB(new Query() {
			public String ask(String question) {
				String returnString = "";
				if (Objects.equals(question, "global.id")) {
					returnString = "1111";
				}
				else if (Objects.equals(question, "global.getNewID")) {
					returnString = "111" + String.valueOf(id);
					id += 2;
				}

				return returnString;
			}
		});

		mGrpMgr.setWriteCB(new Writer () {
			public int write(AdvertisementPayload advPayload) {
				mAdvPayloads.add(advPayload);
				return 0;
			}
			public int write(String dest, byte[] message) {
				throw new java.lang.UnsupportedOperationException("Not supported.");
			}
		});

		mGrpMgr.setReadCB(new Reader () {
			public int read(AdvertisementPayload advPayload) {
				mAdvPayloads.add(advPayload);
				return 0;
			}

			public int read(String src, byte[] message) {
				mMessage = new String(message);
				return 0;
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

	@Test
	public void shouldBeEqual() {
		Group grp = new Group(MY_ID, Group.NONE);
		Group other = new Group(MY_ID, Group.NONE);
		assertEquals(grp, other);
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
		assertEquals("GrpMgr", mGrpMgr.ask("tag"));
	}

	@Test
	public void shouldGet0ChkSum() {
		//System.out.println(mGrpMgr.getChkSum());
		assertTrue(Arrays.equals(new byte[] {-1,-1}, mGrpMgr.getChkSum()));
	}

	@Test
	public void shouldGetAddNamedGroupGetNon0ChkSum() {
		mGrpMgr.ask("addGroup blargity_blarg");
		assertTrue(!(Arrays.equals(new byte[] {-1,-1},mGrpMgr.getChkSum())));
	}

	@Test
	public void shouldGetEmptyListOfGroups() {
		Group [] grps = mGrpMgr.getGroups();
		assertTrue(0 == grps.length);
	}

	@Test
	public void shouldAddandGetBothGroupTypes() {
		mGrpMgr.ask("addGroup blargity_blarg");
		mGrpMgr.ask("addGroup 10.0 10.0 30.0");
		Group [] grps = mGrpMgr.getGroups();
		int numFound = 0;

		assertEquals(2, grps.length);

		for (int i = 0; i < grps.length; i++) {
			if (Group.NAMED_GROUP == grps[i].getType()) {
				numFound++;
			}
			else if (Group.GEO_GROUP == grps[i].getType()) {
				numFound++;
			}
		}

		assertEquals(2, numFound);

	}

	@Test
	public void shouldAddandJoinGroup() {
		mGrpMgr.ask("addGroup blargity_blarg");
		Group [] grps = mGrpMgr.getGroups();

		assertTrue(!grps[0].getStatus());
		mGrpMgr.ask("joinGroup 1110");
		grps = mGrpMgr.getGroups();

		assertTrue(grps[0].getStatus());
	}

	@Test
	public void shouldSetChkSum() {
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
		advPayload.setSrcID(MY_ID);
		mGrpMgr.read(advPayload);
		byte [] msg = mAdvPayloads.get(0).getMsg();
		byte [] chksum = new byte[2];

		System.arraycopy(msg, 8, chksum, 0, 2);

		assertTrue(Arrays.equals(new byte[] {-1,-1},chksum));
	}

	@Test
	public void shouldCheckChkSumNotSendQuery() {
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
		advPayload.setSrcID("2222");
		byte [] msg = new byte[10];
		msg[8] = (byte)-1;
		msg[9] = (byte)-1;
		//set a chk sum that will match
		advPayload.setMsg(msg);
		mGrpMgr.read(advPayload);
		assertEquals(1, mAdvPayloads.size());
		assertEquals(advPayload, mAdvPayloads.get(0));
	}

	@Test
	public void shouldCheckChkSumSendQueryEmptyGroupTable() {
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
		advPayload.setSrcID("2222");
		byte [] msg = new byte[10];
		msg[8] = (byte)0;
		msg[9] = (byte)0;
		//set a chk sum that will not match
		advPayload.setMsg(msg);
		mGrpMgr.read(advPayload);
		assertEquals(2, mAdvPayloads.size());
		assertEquals(advPayload, mAdvPayloads.get(1));
		assertEquals(AdvertisementPayload.GROUP_QUERY, mAdvPayloads.get(0).getMsgType());
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.put(mAdvPayloads.get(0).getMsg());
		buffer.flip();
		assertEquals(0, buffer.getLong());
	}

	@Test
	public void shouldCheckChkSumSendQueryNonEmptyGroupTable() {
		mGrpMgr.ask("addGroup blargity_blarg");
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setMsgType(AdvertisementPayload.LOCATION_UPDATE);
		advPayload.setSrcID("2222");
		byte [] msg = new byte[10];
		msg[8] = (byte)0;
		msg[9] = (byte)0;
		//set a chk sum that will not match
		advPayload.setMsg(msg);
		mGrpMgr.read(advPayload);
		assertEquals(2, mAdvPayloads.size());
		assertEquals(advPayload, mAdvPayloads.get(1));
		assertEquals(AdvertisementPayload.GROUP_QUERY, mAdvPayloads.get(0).getMsgType());
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.put(mAdvPayloads.get(0).getMsg());
		buffer.flip();
		assertTrue(0 != buffer.getLong());
	}

	@Test
	public void shouldCheckQueryTimestampDoNothing(){
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setMsgType(AdvertisementPayload.GROUP_QUERY);
		advPayload.setSrcID("2222");
		advPayload.setMsg(ByteBuffer.allocate(8).putLong(0L).array()); //I will not have a newer timestamp than them
		mGrpMgr.read(advPayload);
		assertEquals(1, mAdvPayloads.size());
		assertEquals(AdvertisementPayload.GROUP_QUERY, mAdvPayloads.get(0).getMsgType());
	}

	@Test
	public void shouldCheckQueryTimestampSendGroupUpdateNamedGroup(){
		mGrpMgr.ask("addGroup blargity_blarg");
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setMsgType(AdvertisementPayload.GROUP_QUERY);
		advPayload.setSrcID("2222");
		advPayload.setMsg(ByteBuffer.allocate(8).putLong(0L).array()); //I will have a newer timestamp than them
		mGrpMgr.read(advPayload);
		
		assertEquals(2, mAdvPayloads.size());
		assertEquals(advPayload, mAdvPayloads.get(1));
		assertEquals(AdvertisementPayload.GROUP_UPDATE, mAdvPayloads.get(0).getMsgType());
		
		String groups = new String(mAdvPayloads.get(0).getMsg());
		String [] parts = groups.split("\\s+");
		assertEquals("1110", parts[0]);
		assertEquals("0", parts[1]);
		assertEquals("blargity_blarg", parts[2]);
	}

	@Test
	public void shouldCheckQueryTimestampSendGroupUpdateGeoGroup(){
		mGrpMgr.ask("addGroup 10.0 10.0 10.0");
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setMsgType(AdvertisementPayload.GROUP_QUERY);
		advPayload.setSrcID("2222");
		advPayload.setMsg(ByteBuffer.allocate(8).putLong(0L).array()); //I will have a newer timestamp than them
		mGrpMgr.read(advPayload);
		
		assertEquals(2, mAdvPayloads.size());
		assertEquals(advPayload, mAdvPayloads.get(1));
		assertEquals(AdvertisementPayload.GROUP_UPDATE, mAdvPayloads.get(0).getMsgType());
		
		String groups = new String(mAdvPayloads.get(0).getMsg());
		String [] parts = groups.split("\\s+");
		assertEquals("1110", parts[0]);
		assertEquals("1", parts[1]);
		assertEquals(10.0, Float.parseFloat(parts[2]), .00001);
		assertEquals(10.0, Float.parseFloat(parts[3]), .00001);
		assertEquals(10.0, Float.parseFloat(parts[4]), .00001);
	}

	@Test
	public void shouldParseGroupUpdate () {
		String groupTable = "2223 0 one 2224 0 two 2225 1 5.0 -25.345 2.5 2226 1 1.0 1.0 1.0";
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setMsgType(AdvertisementPayload.GROUP_UPDATE);
		advPayload.setSrcID("2222");
		advPayload.setMsg(groupTable.getBytes(StandardCharsets.UTF_8));
		mGrpMgr.read(advPayload);

		Group [] grps = mGrpMgr.getGroups();
		int numFound = 0;

		assertEquals(4, grps.length);

		for (int i = 0; i < grps.length; i++) {
			if (Group.NAMED_GROUP == grps[i].getType()) {
				numFound++;
			}
			else if (Group.GEO_GROUP == grps[i].getType()) {
				numFound++;
			}
		}

		assertEquals(4, numFound);
	}

	@Test
	public void shouldIgnoreMessageBecauseGroupNotKnown() {
		String msg = "hello world!";
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setMsgType(AdvertisementPayload.REGULAR_MESSAGE);
		advPayload.setSrcID("2222");
		advPayload.setDestID("1110");
		advPayload.setMsg(msg.getBytes(StandardCharsets.UTF_8));
		mGrpMgr.read(advPayload);
		assertEquals(advPayload, mAdvPayloads.get(0));
		assertEquals("", mMessage);
	}

	@Test
	public void shouldIgnoreMessageBecauseGroupNotJoined() {
		mGrpMgr.ask("addGroup blargity_blarg");

		//Send a message to the group
		String msg = "hello world!";
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setMsgType(AdvertisementPayload.REGULAR_MESSAGE);
		advPayload.setSrcID("2222");
		advPayload.setDestID("1110");
		advPayload.setMsg(msg.getBytes(StandardCharsets.UTF_8));
		mGrpMgr.read(advPayload);
		assertEquals(advPayload, mAdvPayloads.get(0));
		assertEquals("", mMessage);
	}

	@Test
	public void shouldGetMessageBecauseGroupJoined() {
		mGrpMgr.ask("addGroup blargity_blarg");
		mGrpMgr.ask("joinGroup 1110");

		//Send a message to the group
		String msg = "hello world!";
		AdvertisementPayload advPayload = new AdvertisementPayload();
		advPayload.setMsgType(AdvertisementPayload.REGULAR_MESSAGE);
		advPayload.setSrcID("2222");
		advPayload.setDestID("1110");
		advPayload.setMsg(msg.getBytes(StandardCharsets.UTF_8));
		mGrpMgr.read(advPayload);
		assertEquals(advPayload, mAdvPayloads.get(0));
		assertEquals(msg, mMessage);
	}
}