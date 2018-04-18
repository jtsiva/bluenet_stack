package nd.edu.bluenet_new;


public interface BlueNetIFace {
   public String getMyID(); // Returns id of user's device
   public int write(String destID, String input); // Returns number of bytes written, error code (-1) otherwise
   public void regCallback(Result resultHandler); // sets handler to be called on message received
   public String[] getNeighbors(); // returns array of ids connected to this device
   public String getLocation(String id); // returns location of id (as: lat lon), or 0.0 0.0 if id does not exist
}