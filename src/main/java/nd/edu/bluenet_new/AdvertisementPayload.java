package nd.edu.bluenet_new;

import java.util.Arrays;
import java.nio.charset.StandardCharsets;

/**
 * Created by jerry on 4/9/18.
 * updated by josh on 4/16/18
 */

public class AdvertisementPayload {
    public final static byte SMALL_MESSAGE = 1;
    public final static byte REGULAR_MESSAGE = 2;
    public final static byte LOCATION_UPDATE = 3;
    //public final static byte GROUP_ADVERTISE = 4;
    //public final static byte GROUP_QUERY = 5;
    //public final static byte GROUP_REGISTER = 6;

    private byte[] srcID = null;
    private byte[] destID = null;
    private byte msgID = 0;
    private byte msgType = 0;
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

    public byte[] getBytes(){
        byte [] data = srcID;
        return data;
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

    public void setMsgType (byte msgType) {
        this.msgType = msgType;
    }

    public int getMsgType () {
        return msgType;
    }

}
