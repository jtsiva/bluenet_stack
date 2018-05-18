package nd.edu.bluenet_stack;

import java.util.Arrays;
import java.nio.charset.StandardCharsets;

/**
 * Created by jerry on 4/9/18.
 * updated by josh on 4/16/18
 */

public class AdvertisementPayload {
    public final static int SMALL_MESSAGE = 0x186A; //always a push
    public final static int REGULAR_MESSAGE = 0x1869; //push or pull based on size of advertisement
    //public final static int REGULAR_PULL_MESSAGE = 0x186E;// pull option
    public final static int LOCATION_UPDATE = 0x1868; //always a push
    public final static int GROUP_UPDATE = 0x186B;
    public final static int GROUP_QUERY = 0x186C;
    //public final static byte GROUP_REGISTER = 0x186D;

    public final static byte MAX_TTL = 0b111;

    // srcID || destID || msgID || ttl | hp | unused || len || msg 
    // 4B       4B        1B       3b    1b   4b        1B     9B

    private byte[] srcID = null;
    private byte[] destID = null;
    private byte msgID = 0;
    private int msgType = 0;
    private byte ttl = MAX_TTL;
    private byte hp = 0b0;
    private byte unassigned = 0b0;
    private byte len = 0b0;
    private byte[] msg = null; //only used if msg type is small
    private byte[] oneHopNeighbor = null;

    private MessageRetriever msgRetriever = null;
    private boolean needRetriever = false;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()){
            return false;
        }

        AdvertisementPayload other = (AdvertisementPayload)obj;
        if (!Arrays.equals(srcID, other.getSrcID())) {
            return false;
        }

        if (msgID != other.getMsgID())
        {
            return false;
        }

        return true;
    }

    public void fromBytes(byte[] bytes) {
        byte[] srcID = Arrays.copyOfRange(bytes, 0, 4);
        byte[] destID = Arrays.copyOfRange(bytes, 4, 8);
        byte[] msgID = Arrays.copyOfRange(bytes, 8, 9);

        byte header = Arrays.copyOfRange(bytes, 9, 10)[0];

        this.srcID = srcID;
        this.destID = destID;
        this.msgID = msgID[0]; //single bytes

        this.ttl = (byte)((header & 0b11100000) >>> 5);
        this.hp = (byte)((header & 0b00010000) >>> 4);
        this.unassigned = (byte)(header & 0b00001111);

        this.len = Arrays.copyOfRange(bytes, 10, 11)[0];

        if (11 < bytes.length) { //message was pushed
            this.msg = Arrays.copyOfRange(bytes, 11, 11 + this.len);
        }
        else {
            needRetriever = true; //we're going to pull the data when we need it
        }

    }

    public void setRetriever(MessageRetriever retriever) {
        msgRetriever = retriever;
    }

    public byte[] getHeader(){
        byte [] bytes = new byte[11];
        System.arraycopy(srcID, 0, bytes,0,srcID.length);
        System.arraycopy(destID, 0, bytes,srcID.length,destID.length);
        bytes[8] = msgID;

        byte header = 0b0;
       
        header = (byte)((this.ttl << 5) | (this.hp << 4) | (int)this.unassigned);
        
        bytes[9] = header;
        bytes[10] = this.len;
        return bytes;
    }

    public char[] getPrettyBytes() {
        final  char[] hexArray = "0123456789ABCDEF".toCharArray();
        byte[] bytes = this.getHeader();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }

        return hexChars;
    }

    public void setSrcID(String srcID){
        this.srcID = srcID.getBytes(StandardCharsets.UTF_8);
    }

    public void setSrcID(byte [] srcID){
        this.srcID = srcID.clone();
    }

    public byte[] getSrcID() {
        return srcID;
    }

    public void setDestID(String destID){
        this.destID = destID.getBytes(StandardCharsets.UTF_8);
    }

    public void setDestID(byte [] destID){
        this.destID = destID.clone();
    }

    public byte[] getDestID() {
        return destID;
    }

    public void setOneHopNeighbor(String neighborID) {
        this.oneHopNeighbor = neighborID.getBytes(StandardCharsets.UTF_8);
    }

    public byte[] getOneHopNeighbor () {
        return oneHopNeighbor;
    }

    public void setMsgID (byte msgID) {
        this.msgID = msgID;
    }

    public byte getMsgID () {
        return msgID;
    }

    public void setMsgType (int msgType) {
        this.msgType = msgType;
    }

    public int getMsgType () {
        return msgType;
    }

    public byte getTTL() {
        return this.ttl;
    }

    public void decTTL () {
        this.ttl -= 1;
    }

    public boolean isHighPriority() {
        return (1 == this.hp);
    }

    public void setHighPriority(boolean priority) {
        if (priority) {
            this.hp = 0b1;
        }
        else {
            this.hp = 0b0;
        }
    }

    public byte getMsgLength() {
        return this.len;
    }

    public void setMsg(byte[] msg) {
        this.len = (byte)msg.length;
        this.msg = msg.clone();
    }

    public byte[] getMsg() {
        if (needRetriever) {
            msg = msgRetriever.retrieve();// we need to go pull in the message
            //check length of msg against len in header to make sure we have valid data
            needRetriever = false;
        }

        return msg;
    }

   

}
