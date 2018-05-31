package nd.edu.bluenet_stack;

/**
 * Application-facing interface for the BlueNet stack
 * 
 * @author Josh Siva
 */
public interface BlueNetIFace {
   /**
    * @return the BlueNet ID of this device as a String
    */
   public String getMyID();

   /**
    * Write a message/payload to BlueNet network
    * 
    * @param destID the alphanumeric BlueNet ID of the intended recipient
    * @param input the message to be sent
    * @return the number of bytes written or -1 in the case of an error
    */
   public int write(String destID, String input);

   //Unsure about the inclusion this version of a write
   //public int write(String destID, String input, boolean highPriority); //set whether this message is high priority
   
   /**
    * @param resultHandler the user-implemented callback so that reads can be
    *        be delivered asynchronously
    * @see Result
    */
   public void regCallback(Result resultHandler); // sets handler to be called on message received
   
   /**
    * @return array of BlueNet IDs of all discovered devices in the network
    * @see LocationManager
    */
   public String[] getNeighbors(); 
   
   /**
    * @param id alphanumeric BlueNet ID of device whose location you want
    * @return the average location of the device (as lat lon) or 0.0 0.0 if location not known
    * @see LocationManager
    */
   public String getLocation(String id); // returns average location of id (as: lat lon), or 0.0 0.0 if id does not exist

   //Group operations

   /**
    * @return Group objects for all groups (including those that are not joinable)
    * @see Group
    * @see GeoGroup
    * @see NamedGroup
    * @see GroupManager
    */
   public Group [] getGroups();//return joinable groups (maybe not geo since those are automatic)
   
   /**
    * Create a new named group
    * 
    * @param name to be applied to the group
    * @see Group
    * @see NamedGroup
    */
   public void addGroup(String name); //add named group
   
   /**
    * Create a new geographical group
    * 
    * @param lat latitude of center of group location
    * @param lon longitude of center of group location
    * @param rad radius from center of group location that is considered "in" 
    *            the group
    * @see Group
    * @see GeoGroup
    */
   public void addGroup(float lat, float lon, float rad);
   
   /**
    * Attempt to join a group. Some groups, like geographical groups, cannot be
    * joined in the way. Entry into a group means that messages addressed to
    * that group will be able to be received by this node
    * 
    * @param id alphanumeric BlueNet ID of group that user wants to join
    * @return true if the group was joined, false otherwise
    */
   public boolean joinGroup(String id);

   /**
    * Attempt to leave a group. Some groups cannot be left in this way.
    * Departure from a group means that messages addressed to that group will not
    * be received by this node
    * 
    * @param id alphanumeric BlueNet ID of group that user wants to leave
    * @return true if the group was left, false otherwise
    */
   public boolean leaveGroup(String id); //leave group with id; returns true if left, false otherwise
   		

}