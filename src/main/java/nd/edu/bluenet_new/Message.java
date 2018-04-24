package nd.edu.bluenet_new;

import java.util.Arrays;
import java.nio.charset.StandardCharsets;

/**
 * Created by josh on 4/16/18.
 */

public class Message {
    private byte ttl;
    private byte ack;
    private byte len; // only changed from 0 when getBytes is called since set by data.size()
    private byte[] data;

    public Message() {
        ttl = 0b0;
        ack = 0b0;
        len = 0b0;
        data = null;
    }


    public byte getTTL(){
        return ttl;
    }

    public byte getAck(){
        return ack;
    }

    public byte getLen(){
        return len;
    }

    public byte[] getData(){
        return data;
    }

    public void setData(String data) {
        this.data = data.getBytes(StandardCharsets.UTF_8);
    }

    public void setData(byte[] data) {
        this.data = data;
        this.len = (byte)data.length;
    }

    public void fromBytes(byte[] message){
        byte header = message[0];
        ttl = (byte)((header & 0b11000000) >>> 6);
        ack = (byte)((header & 0b00100000) >>> 5);
        len = (byte)(header & 0b00011111);

        data = Arrays.copyOfRange(message, 1, len + 1);
    }

    public byte[] getBytes(){
        len = (byte)data.length;

        byte header = 0b0;
        header = (byte)(header | (ttl << 6));
        header = (byte)(header | (ack << 5));
        header = (byte)(header | len);

        byte [] returnBytes = new byte[len + 1];
        returnBytes[0] = header;
        System.arraycopy(data, 0, returnBytes, 1, data.length);

        return returnBytes;
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



}