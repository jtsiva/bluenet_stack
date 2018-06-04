package nd.edu.bluenet_stack;

import java.util.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class LayerTest {

	static private ArrayList<LayerBase> mLayers = new ArrayList<>(); 
	
	public static void main(String[] args){
		
		//Test the protocol container
		ProtocolContainer proto = new ProtocolContainer();
		proto.regCallback(new Result () {
			public int provide (String src, byte[] data) {
				System.out.print(src + ": ");
				System.out.println(data);
				return 0;
			}
		});

		proto.write(Group.BROADCAST_GROUP, "proto test: hello world!");


	}
}