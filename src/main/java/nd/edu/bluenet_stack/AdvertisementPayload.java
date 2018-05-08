package nd.edu.bluenet_stack;

import java.util.Arrays;
import java.nio.charset.StandardCharsets;

/**
 * Created by jerry on 4/9/18.
 * updated by josh on 4/16/18
 */

public class AdvertisementPayload {
    public final static int SMALL_MESSAGE = 0x186A;
    public final static int REGULAR_MESSAGE = 0x1869;
    public final static int LOCATION_UPDATE = 0x1868;
    //public final static byte GROUP_ADVERTISE = 0x186B;
    //public final static byte GROUP_QUERY = 0x186C;
    //public final static byte GROUP_REGISTER = 0x186D;

    private byte[] srcID = null;
    private byte[] destID = null;
    private byte msgID = 0;
    private int msgType = 0;
    private Message msg = null; //only used if msg type is small

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

        byte[] msg = Arrays.copyOfRange(bytes, 9, bytes.length);

        this.srcID = srcID;
        this.destID = destID;
        this.msgID = msgID[0]; //single bytes
        this.msg = new Message();
        this.msg.fromBytes(msg);
    }

    public byte[] getBytes(){
        byte [] bytes = new byte[9];
        System.arraycopy(srcID, 0, bytes,0,srcID.length);
        System.arraycopy(destID, 0, bytes,srcID.length,destID.length);
        bytes[8] = msgID;
        return bytes;
    }

    public char[] getPrettyBytes() {
        final  char[] hexArray = "0123456789ABCDEF".toCharArray();
        byte[] bytes = this.getBytes();
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

    public byte[] getSrcID() {
        return srcID;
    }

    public void setDestID(String destID){
        this.destID = destID.getBytes(StandardCharsets.UTF_8);
    }

    public void setMsgID (byte msgID) {
        this.msgID = msgID;
    }

    public byte getMsgID () {
        return msgID;
    }

    public void setMsg(Message msg) {
        this.msg = msg;
    }

    public Message getMsg() {
        return msg;
    }

    public void setMsgType (int msgType) {
        this.msgType = msgType;
    }

    public int getMsgType () {
        return msgType;
    }

}
