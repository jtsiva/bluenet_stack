package nd.edu.bluenet_new;

public class LayerTest {
	
	public static void main(String[] args){

		System.out.println("setting things up...");
		Message msg = new Message();
		//AdvertisementPayload advPayload = new AdvertisementPayload();

		ConnectionLayer connL = new ConnectionLayer();
		FilterCopies filterCopies = new FilterCopies();
		DummyBLE dummy = new DummyBLE();


		dummy.setReadCB(new Reader() {
			public int read(AdvertisementPayload advPayload) {
				return filterCopies.read(advPayload);
			}

			public int read(Message message) {
				return -1;
			}
		});

		filterCopies.setReadCB(new Reader() {
			public int read(AdvertisementPayload advPayload) {
				return connL.read(advPayload);
			}
			public int read(Message message) {
				return -1;
			}
		});

		connL.setWriteCB(new Writer() {
			public int write(AdvertisementPayload advPayload) {
				return dummy.write(advPayload);
			}
			public int write(Message message) {
				return dummy.write(message);
			}
		});

		connL.setReadCB(new Reader() {
			public int read(Message message) {
				final  char[] hexArray = "0123456789ABCDEF".toCharArray();
				byte[] bytes = message.getBytes();
				char[] hexChars = new char[bytes.length * 2];
			    for ( int j = 0; j < bytes.length; j++ ) {
			        int v = bytes[j] & 0xFF;
			        hexChars[j * 2] = hexArray[v >>> 4];
			        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
			    }
			    System.out.println(hexChars);
				return 0;
			}
			public int read(AdvertisementPayload advPayload) {
				return -1;
			}
		});
		msg.setData("hello world!");
		connL.write(msg);


	}
}