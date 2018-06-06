package nd.edu.bluenet_stack;

import java.util.Arrays;
import java.nio.charset.StandardCharsets;

/**
 * This class encapsulates both the header and message elements required for
 * communication. The class identifies different message types, parses byte
 * strings to set this classes variables, and dumps the classes variables into
 * a byte array.
 *
 * <p>The expected and produced format of the byte array is the following
 * <pre>{@code
 *       srcID || destID || msgID || ttl | hp | unused || len || msg 
 *       4B       4B        1B       3b    1b   4b        1B     9B
 * }</pre>
 *
 * <p>This class provides mechanisms for setting priority, tracking next hop
 * neighbors, and supporting both push and pull based communication styles. 
 *
 * @author  Josh Siva
 * @see     MessageRetriever
 */

public class AdvertisementPayload {
    public final static int SMALL_MESSAGE = 0x186A; //always a push
    public final static String SMALL_MESSAGE_STR = "186a";
    public final static int REGULAR_MESSAGE = 0x1869; //push or pull based on size of advertisement
    public final static String REGULAR_MESSAGE_STR = "1869";
    //public final static int REGULAR_PULL_MESSAGE = 0x186E;// pull option
    public final static int LOCATION_UPDATE = 0x1868; //always a push
    public final static String LOCATION_UPDATE_STR = "1868";
    public final static int GROUP_UPDATE = 0x186B;
    public final static String GROUP_UPDATE_STR = "186b";
    public final static int GROUP_QUERY = 0x186C;
    public final static String GROUP_QUERY_STR = "186c";
    //public final static byte GROUP_REGISTER = 0x186D;

    public final static byte MAX_TTL = 0b111;

    public final static int HEADER_SIZE = 11;


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
    public boolean push = true;

    /**
     * Override the equals operator so that payloads can be compared for
     * equality. This is useful for determining whether we have seen a
     * message before. For two AdvertisementPayloads to be equal, both the
     * msgID and msgSrc must be equal.
     * 
     * @param  obj the object against which we are comparing
     * @return true if the AdvertisementPayloads are equal, false otherwise
     */
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

    /**
     * Parse a byte array into the appropriate member variables. The byte array
     * is assumed to be properly formatted. If the length of the byte array is
     * greater than the length of the header then it is assumed that the message
     * was pushed along with the header; otherwise, we will need to retrieve the
     * message through a callback
     * 
     * @param bytes the byte array that we parse
     * @return true if successfully parsed, false if there was an issue
     * @see MessageRetriever
     */
    public boolean fromBytes(byte[] bytes) {
        boolean parseSuccessful = true;

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

        if (this.len == bytes.length - HEADER_SIZE) { //message was pushed and it's all here
            this.msg = Arrays.copyOfRange(bytes, HEADER_SIZE, HEADER_SIZE + this.len);
        }
        else if (this.len > 0 && bytes.length == HEADER_SIZE) {
            needRetriever = true; //we're going to pull the data when we need it
        }
        else {
            parseSuccessful = false;
        }

        return parseSuccessful;
    }

    /**
     * Set the java equivalent of a callback function for getting the message
     * byte array at some later point in time. The byte array will be retrieved
     * from a separate characteristic in the appropriate GATT service.
     * 
     * @param retriever the implemented interface that is used to read the
     *        message
     * @see MessageRetriever
     */
    public void setRetriever(MessageRetriever retriever) {
        msgRetriever = retriever;
    }

    /**
     *  Return the header information as a byte array
     * 
     * @return the bytes that constitute the header
     */
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

    /**
     * A convenience function for printing the header in hex. For debugging.
     * 
     * @return the header in hex as a char array
     */
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

    /**
     * @param srcID the alphanumeric BlueNet ID string for the sender
     */
    public void setSrcID(String srcID){
        this.srcID = srcID.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * @param srcID the alphanumeric BlueNet ID for the sender as a byte array
     */
    public void setSrcID(byte [] srcID){
        this.srcID = srcID.clone();
    }

    /**
     * @return the byte array that is the BlueNet ID for the sender if set,
     *         else null
     */
    public byte[] getSrcID() {
        return srcID;
    }

    /**
     * @param destID the alphanumeric BlueNet ID for the receiver as a String
     */
    public void setDestID(String destID){
        this.destID = destID.getBytes(StandardCharsets.UTF_8);
    }

    /**
    * @param destID the alphanumeric BlueNet ID for the receiver as a byte 
    *               array
    */
    public void setDestID(byte [] destID){
        this.destID = destID.clone();
    }

    /**
     * @return the byte array that is the BlueNet ID for the receiver if set,
     *         else null
     */
    public byte[] getDestID() {
        return destID;
    }

    /**
     * Since the one hop neighbor is not always the source of the message, we
     * want to keep track of the direct neighbor who sent the message. The
     * one hop neighbor can be determined by checking the TTL.
     * 
     * @param neighborID the alphanumeric BlueNetID for the one hop neighbor 
     *                   that sent this message as a String
     */
    public void setOneHopNeighbor(String neighborID) {
        this.oneHopNeighbor = neighborID.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * @return the alphanumeric BlueNet ID of the one hop neighbor that sent
     *         the message as a byte array if set, else null
     */
    public byte[] getOneHopNeighbor () {
        return oneHopNeighbor;
    }

    /**
     * @param msgID 1 byte number that identifies the message
     */
    public void setMsgID (byte msgID) {
        this.msgID = msgID;
    }

    /**
     * @return the ID number of the message if set, else 0
     */
    public byte getMsgID () {
        return msgID;
    }

    /**
     * @param msgType 16-bit UUID corresponding to the GATT service ID. NOT
     *                included in BlueNet header
     */
    public void setMsgType (int msgType) {
        this.msgType = msgType;
    }
    
    /**
     * @return the message type if set, 0 otherwise
     */
    public int getMsgType () {
        return msgType;
    }

    /**
     * @return the current time-to-live (by hop) of this message, MAX_TTL by
     *         default.
     */
    public byte getTTL() {
        return this.ttl;
    }

    /**
     * Decrease the TTL by 1
     */
    public void decTTL () {
        this.ttl -= 1;
    }

    /**
     * @return true if the high priority bit has been set, false otherwise
     */
    public boolean isHighPriority() {
        return (1 == this.hp);
    }

    /**
     * @param priority true or false depending on whether we want the message
     *        to be marked as high priority
     */
    public void setHighPriority(boolean priority) {
        if (priority) {
            this.hp = 0b1;
        }
        else {
            this.hp = 0b0;
        }
    }

    /**
     * @return the length of the message as indicated in the header
     */
    public byte getMsgLength() {
        return this.len;
    }

    /**
     * Set the message for the payload as well as the length field in the
     * header
     * 
     * @param msg the byte array containing the message
     */
    public void setMsg(byte[] msg) {
        this.len = (byte)msg.length;
        this.msg = msg.clone();
    }

    /**
     * The message is retrieved either from the member variable or by using
     * a callback to pull in the message.
     * 
     * @return the byte array constituting the message
     * @see MessageRetriever
     */
    public byte[] getMsg() {
        if (needRetriever) {
            if (null == msgRetriever) {
                msg = null;
            }
            else{
                msg = msgRetriever.retrieve(this.srcID);// we need to go pull in the message
                
                //if we pull a message that doesn't match the size we expect
                //then we should should just give up and set msg to null.
                //This could happen if the characteristic is changed between
                //when we got the header and when we read the message
                if (this.len != msg.length) {
                    msg = null;
                }
            
                needRetriever = false;
            }

           
        }

        return msg;
    }

   

}
