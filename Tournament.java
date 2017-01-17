package twcore.bots.pokerbot;

import twcore.core.BotAction;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.TimerTask;

public class Tournament {
   /* The Bot Actions */
   private BotAction botAction;
   /* Amount PubBux set for this tournament as reward */
   private int pubBux;
   /* Number of players can win a some kind of reward */
   private int numWinners;
   /* Number of players required to start tournament */
   private int minPlayers;
    
    /** The players registered to tournament mode. */
    private final VectoidReUse<String, PokerPlayer> players;
    /** A List of machine Id's for each player name. */
    private final Map<String, String> machineIds;
    /** A List of IP Addresses for each player name. */
    private final Map<String, String> ipAddresses;
    /** List of players which have to be checked with *info */
    private final List<String> registeringPlayers;
    /** This timer removes player's from registeringPlayers, once they pass the *info check */
    private TimerTask registeringPlayerAcceptorTimer;
    
   /**
     * Constructor.
     *
     * @param PubBux
     *            The amount of PubBux is generated for the winner, $1,000,000 is limit.
     * @param numWinners
     *            The number of winners possible for this tournament.
     * @param minPlayers
     *            The minimum amount of players required to start.
     */
   public Tournament(BotAction botAction, int pubBux, int numWinners, int minPlayers) {
   	this.botAction = botAction;
    	this.pubBux = pubBux;
    	this.numWinners = numWinners;
    	this.minPlayers = minPlayers;
    	players = new VectoidReUse<String, PokerPlayer>();
    	machineIds = new HashMap<String, String>();
    	ipAddresses = new HashMap<String, String>();
    	registeringPlayers = new ArrayList<String>();
    	registeringPlayerAcceptor();
   }
   
   /**
     * Registers a player to the tournament
     *
     * @param name
     *            The name of the player (Subspace name).
     * @param staffName (optional)
     *            Staff name of the player who forceRegistered, so the player knows.
     * If more then 80 players registered then some will have to wait until they can play.
     */
   public void register(String name, String... staffName) {
   	//Machine ID check
   	//So people don't get 2 accounts into the tournament from one computer.
   	//Since Machine ID's could be the same for different computers a IP check is used as double checker.
   	//Can't use IP check alone because like me and Sawyer we use the same IP adderss.
	
	name = name.toLowerCase();
   	//Send *info to the player to get his Machine ID and IP.
    	botAction.sendUnfilteredPrivateMessage(name, "*info");
    	
    	//Players pending *info command
   	if(registeringPlayers.contains(name) && staffName.length == 0) {
   		botAction.sendSmartPrivateMessage(name, "You are already pending registration");
	   	botAction.sendSmartPrivateMessage(name, "To remove yourself from tournament type !notplaying or !np");
	   	return;
   	}
   	
   	if(players.containsKey(name)) {
		botAction.sendSmartPrivateMessage(name, "You are already registered as (" + players.indexOfKey(name) + "/" + players.size() + ").");
	   	botAction.sendSmartPrivateMessage(name, "To remove yourself from tournament type !notplaying or !np");
	   	return;
   	}
   	
   	if(staffName.length== 0) {
		botAction.sendSmartPrivateMessage(name, "You have been added to pending registration list.");
   		registeringPlayers.add(name);
   	} else {
   		int playerId = botAction.getPlayerID(name);
        	if(playerId == -1) {
        		botAction.sendSmartPrivateMessage(staffName[0], "Couldn't find the player " + name + " in the arena!");
        		return;
        	}
   		players.put(name, new PokerPlayer(name, 10000));
   		botAction.sendSmartPrivateMessage(staffName[0], "Successfully registered " + name + " as (" + players.indexOfKey(name) + "/" + players.size() + ") to play the tournament.");
        	botAction.sendSmartPrivateMessage(name, "You have registered as (" + players.indexOfKey(name) + "/" + players.size() + ") by " + staffName[0] + ".");
   	}
   }
   
   /**
     * Unregisters a player to the tournament
     *
     * @param name
     *            The name of the player (Subspace name).
     */
   public void unregister(String name) {
   	name = name.toLowerCase();
   	
   	registeringPlayers.remove(name);
   	machineIds.remove(name);
   	ipAddresses.remove(name);
   	if(players.containsKey(name)) {
		botAction.sendSmartPrivateMessage(name, "You have been removed from playing in the tournament.");
		players.remove(name);
   	} else {
   		botAction.sendSmartPrivateMessage(name, "Don't worry you didn't type !play, so you are not playing the tournament anyways.");
   	}
   }
   
   /**
     * Turns off the tournament mode
     */
   public void stop() {
   	//Close the registering acceptor timer.
   	if(registeringPlayerAcceptorTimer != null)
   		registeringPlayerAcceptorTimer.cancel();
   }

   /**
     * Makes a little database of player names linked to machine Id value.
     * Used to check when player is getting registered for the tournament.
     */
   public void addSecurityInformation(String name, String machineId, String ipAddress) {
   	//TypedName is longer then the subspace name is allowed so this causes bugs need to trim it.
   	//While trimming it, it will add all the variances of the same name with one letter removed at end.
   	name = name.toLowerCase();
   	
   	//I believe that Continuum cuts off names after 19 characters.
   	//I tested with %tickname and it's only 19 characters long [my logged in name is 23 characters long]
   	//Even though they can login with longer names.
   	if(name.length() > 19)
   		name = name.substring(0, name.length()-(name.length()-19));

   	machineIds.put(name, machineId);
   	ipAddresses.put(name, ipAddress);
   }
   
   /**
     * List of all player name's that share the same machine Id, ignoring the parameter name.
     */
   public List<String> getMachineIdMatches(String name, String machineId) {
   	String tmpName;
   	String tmpMachineId;
   	
   	List<String> matchedNames = new ArrayList<String>();
   	
 	for(Map.Entry<String, String> entry : machineIds.entrySet()) {
            tmpName = entry.getKey();
            tmpMachineId = entry.getValue();
            
            if(tmpMachineId.equals(machineId) && !tmpName.equalsIgnoreCase(name)) {
            	matchedNames.add(tmpName);
            }
        }
        return matchedNames;
   }
   
   /**
     * List of all player name's that share the same ip address, ignoring the parameter name.
     */
   public List<String> getIPAddressMatches(String name, String ipAddress) {
   	String tmpName;
   	String tmpIPAdderss;
   	
   	List<String> matchedNames = new ArrayList<String>();
   	
 	for(Map.Entry<String, String> entry : ipAddresses.entrySet()) {
            tmpName = entry.getKey();
            tmpIPAdderss = entry.getValue();
            
            if(tmpIPAdderss.equals(ipAddress) && !tmpName.equalsIgnoreCase(name)) {
            	matchedNames.add(tmpName);
            }
        }
        return matchedNames;
   }
   
   public void registeringPlayerAcceptor() {
           registeringPlayerAcceptorTimer = new TimerTask() {
            public void run() {
            	if(registeringPlayers.size() == 0) return;
            	
	   	String machineId;
	   	String ipAddress;
	   	List<String> machineIdNameMatches = null;
	   	List<String> ipAddressNameMatches = null;
	   	
	   	List<String> removePendingPlayers = new ArrayList<String>();
	   	
	   	for(String checkName : registeringPlayers) {
		   	if(machineIds.containsKey(checkName)) {
		   		machineId = machineIds.get(checkName);
		   		machineIdNameMatches = getMachineIdMatches(checkName, machineId);
		   	}
		   	
		   	if(ipAddresses.containsKey(checkName)) {
		   		ipAddress = ipAddresses.get(checkName);
		   		ipAddressNameMatches = getIPAddressMatches(checkName, ipAddress);
		   	}   	
		   	
		   	//Check if both machineIdNameMatches and ipAddressNameMatches both have same names.
		   	if((ipAddressNameMatches != null && ipAddressNameMatches.size() > 0) &&
		   	    (machineIdNameMatches != null && machineIdNameMatches.size() > 0)) {
		   	
		   		//Clears all the machine Id name's which don't contain the same name as ip address names
		   		machineIdNameMatches.retainAll(ipAddressNameMatches);
		   		
		   		if(machineIdNameMatches.size() > 0) {
		   			botAction.sendSmartPrivateMessage(checkName, "Your pending registration has been denied.");
		   			botAction.sendSmartPrivateMessage(checkName, "The player" + (machineIdNameMatches.size() > 1 ? "s " : " ") + machineIdNameMatches + " " + (machineIdNameMatches.size() > 1 ? "were" : "was") +" registered at your location.");
		   			botAction.sendSmartPrivateMessage(checkName, "If that's not you, talk to the staff hosting this event about this, they might put you in.");
		   		}
		   	} else {
		   		players.put(checkName, new PokerPlayer(checkName, 10000));
		        	botAction.sendSmartPrivateMessage(checkName, "You have registered as (" + players.indexOfKey(checkName) + "/" + players.size() + ").");
		   	}
		   	removePendingPlayers.add(checkName);
	   	}
	   	registeringPlayers.removeAll(removePendingPlayers);
            };
        };
        botAction.scheduleTaskAtFixedRate(registeringPlayerAcceptorTimer, 0, 1000);
   }
}