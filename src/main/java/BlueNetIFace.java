package nd.edu.bluenet_stack;


public interface BlueNetIFace {
   public String getMyID(); // Returns id of user's device
   public int write(String destID, String input); // Returns number of bytes written, error code (-1) otherwise

   //Unsure about the inclusion this version of a write
   //public int write(String destID, String input, boolean highPriority); //set whether this message is high priority
   
   public void regCallback(Result resultHandler); // sets handler to be called on message received
   public String[] getNeighbors(); // returns array of ids connected to this device
   public String getLocation(String id); // returns average location of id (as: lat lon), or 0.0 0.0 if id does not exist

   //Group operations
   public Group [] getGroups();//return joinable groups (maybe not geo since those are automatic)
   public void addGroup(String name); //add named group
   public void addGroup(float lat, float lon, float rad); //add geographic group
   public boolean joinGroup(String id); //join group with id; returns true if joined, false otherwise
   public boolean leaveGroup(String id); //leave group with id; returns true if left, false otherwise
   		

}