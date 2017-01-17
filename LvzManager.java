package twcore.bots.pokerbot;

import twcore.core.BotAction;
import twcore.core.lvz.LvzObject;
import twcore.core.lvz.CoordType;
import twcore.core.game.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

public class LvzManager {
    //This is a array of all 5000[MAX_LVZ_OBJECTS][playerNames, -1 = TO ALL] possible object id's which are in use and which are not.
    private Map<Integer, List<String>> allActiveLVZObjectsOnMap;
    //This Map below is for DrawString case where you have any amount of Lvz Object's associated to one unique id.
    //This is also used to render new players entering with currently turned on objects. TODO:
    private Map<Integer, List<LvzObjectPackage>> allLvzObjectsByUniqueID;

    private BotAction botAction;

    public LvzManager(BotAction botAction) {
        this.botAction = botAction;

        //Lvz Object Id for a list of player Name's, 
        //[-1 = To all, -2 = To all, but one, PlayerName = just that playername
        allActiveLVZObjectsOnMap = new HashMap<Integer, List<String>>();
        //Unique Id for a list of LvzObjects
        allLvzObjectsByUniqueID = new HashMap<Integer,  List<LvzObjectPackage>>();
    }
    
    public String[] getDebugInfo() {
		int aALOOM_key_count = 0;
		int aALOOM_value_count = 0;
		int aALOOM_null_value_count = 0;
				
		int aLOBUI_key_count = 0;
		int aLOBUI_value_count = 0;
		int aLOBUI_null_value_count = 0;
		
		int map_total_on_count = 0;
		int map_negitive1_on_count = 0;
		int map_negitive2_on_count = 0;
		int map_player_on_count = 0;
		
		int screen_total_on_count = 0;
		int screen_negitive1_on_count = 0;
		int screen_negitive2_on_count = 0;
		int screen_player_on_count = 0;
	
		//Checks all active LvzObjects On Map [screen and map]
		for(Map.Entry<Integer, List<String>> entry : allActiveLVZObjectsOnMap.entrySet()) {
		    int objectId = entry.getKey();
		    List<String> playerNames = entry.getValue();
		
			aALOOM_key_count++;
			if(playerNames == null) {
				aALOOM_null_value_count++;
				continue;
			}
			aALOOM_value_count += playerNames.size();
			
			if(objectId <=Constants.MAX_LVZ_OBJECTS) {
				map_total_on_count++;
				if(playerNames.contains(Constants.LVZ_SEND_TO_ALL))
					map_negitive1_on_count++;
				if(playerNames.contains(Constants.LVZ_SEND_TO_ALL_BUT_ONE))
					map_negitive2_on_count++;
				if(!playerNames.contains(Constants.LVZ_SEND_TO_ALL) && !playerNames.contains(Constants.LVZ_SEND_TO_ALL_BUT_ONE) && playerNames.size() > 0)
					map_player_on_count++;
			} else {
				screen_total_on_count++;
				if(playerNames.contains(Constants.LVZ_SEND_TO_ALL))
					screen_negitive1_on_count++;
				if(playerNames.contains(Constants.LVZ_SEND_TO_ALL_BUT_ONE))
					screen_negitive2_on_count++;
				if(!playerNames.contains(Constants.LVZ_SEND_TO_ALL) && !playerNames.contains(Constants.LVZ_SEND_TO_ALL_BUT_ONE) && playerNames.size() > 0)
					screen_player_on_count++;
			}
		}
		
		//Checks all LvzObjects By Unique Id
		for(Map.Entry<Integer, List<LvzObjectPackage>> entry : allLvzObjectsByUniqueID.entrySet()) {
		    int uniqueId = entry.getKey();
		    List<LvzObjectPackage> lvzObjectPackages = entry.getValue();
		
			aLOBUI_key_count++;
			if(lvzObjectPackages == null) {
				aLOBUI_null_value_count++;
				continue;
			}
			aLOBUI_value_count += lvzObjectPackages.size();
		}
    
        String[] out = {
                    "LvzManager Info: (Check for leaks)",
                    "----------------------------------------------------------------------------",
                    "Key Count: aALOOM[" + aALOOM_key_count + "] aLOBUI[" + aLOBUI_key_count + "]",
                    "Value Count: aALOOM[" + aALOOM_value_count + "] aLOBUI[" + aLOBUI_value_count + "]" ,
                    "Null Values Count: aALOOM[" + aALOOM_null_value_count + "] aLOBUI[" + aLOBUI_null_value_count + "]",
                    "----------------------------------------------------------------------------",
                    "Total mapObjects ON: [" + map_total_on_count + "]",
                    "Total LVZ_SEND_TO_ALL ON Count: [" + map_negitive1_on_count + "]",
                    "Total LVZ_SEND_TO_ALL_BUT_ONE ON Count: [" + map_negitive2_on_count + "]",
                    "Total Single Player ON Count: [" + map_player_on_count + "]",
                    "----------------------------------------------------------------------------",         
                    "Total screenObjects ON: [" + screen_total_on_count + "]",
                    "Total LVZ_SEND_TO_ALL ON Count: [" + screen_negitive1_on_count + "]",
                    "Total LVZ_SEND_TO_ALL_BUT_ONE ON Count: [" + screen_negitive2_on_count + "]",
                    "Total Single Player ON Count: [" + screen_player_on_count + "]"
            };
       return out;
    }

    private int getFirstUnusedLvzObjectId(String playerName, boolean isMapObject) {
        /**
        * This function returns the index of lvz objects that is available for use.
        * Keeps track of all indexes of lvz objects available for use for any playerName.
        * If index of lvz object has playerName = "-1" (Constants.LVZ_SEND_TO_ALL) 
        * or "-2" (Constants.LVZ_SEND_TO_ALL_BUT_ONE)
        * then it cannot be unused for anybody else.
        * Also duplicate playerName's will always get a new index of lvz object.
        * Returns -1 if no lvz objects are available for use.
        */
        
	playerName = playerName.toLowerCase();
        //Don't ever use 0 for lvz Object or it will turn off all LVZ objects, that are not server-controlled
        int startLvzObjectId = isMapObject ? Constants.START_MAP_LVZ_OBJECTS :  Constants.START_SCREEN_LVZ_OBJECTS;
        int endLvzObjectId = isMapObject ? Constants.MAX_LVZ_OBJECTS : (Constants.START_SCREEN_LVZ_OBJECTS + Constants.MAX_LVZ_OBJECTS);

        if(playerName.equals(Constants.LVZ_SEND_TO_ALL) || playerName.equals(Constants.LVZ_SEND_TO_ALL_BUT_ONE)) {
            //This one is for Multiplayer Control Types like (-1 or -2)
            for(int i = startLvzObjectId; i <= endLvzObjectId; i++) {
                if(allActiveLVZObjectsOnMap.get(i) == null)
                    return i;
            }
        } else if(!playerName.equals(Constants.LVZ_SEND_TO_ALL) && !playerName.equals(Constants.LVZ_SEND_TO_ALL_BUT_ONE)) {
            //This one is for Single Player PlayerName's only (no control types like -1 or -2).
            int firstEmptyGlobalSlot = -1;
            for(int i = startLvzObjectId; i <= endLvzObjectId; i++) {
                //firstEmptyGlobalSlot is just in case it fails to find any single-player owned objects
                //Or if it happens where all single-player owned objects already have this playerId.
                //In both cases a new single-player owned object has to be created.
                if(firstEmptyGlobalSlot == -1) {
                    if(allActiveLVZObjectsOnMap.get(i) == null)
                        firstEmptyGlobalSlot = i;
                }
                if(allActiveLVZObjectsOnMap.get(i) != null) {
                    if(!allActiveLVZObjectsOnMap.get(i).contains(Constants.LVZ_SEND_TO_ALL) && //global
                        !allActiveLVZObjectsOnMap.get(i).contains(Constants.LVZ_SEND_TO_ALL_BUT_ONE) && //global but ignore a certain playerName.
                        !allActiveLVZObjectsOnMap.get(i).contains(playerName)) //single-player or anything else.
                        	return i;
                }
            }
            //If it ever gets here then, return firstEmptyGlobalSlot
            return firstEmptyGlobalSlot;
        }

        return -1;
    }

    private LvzObject getNewLvzObject(String playerName, boolean isMapObject, String... ignoredPlayerName) {
        //playerName == [-1] LVZ_SEND_TO_ALL means global object id.
        //playerName == [-2] LVZ_SEND_TO_ALL_BUT_ONE means global object but ignore certain playerName. [must be used with optional parameter]
        //playerName == anything else is a single player object.
        //ignoredPlayerName is optional parameter which player to not send object too.
        if(playerName.equals(Constants.LVZ_SEND_TO_ALL_BUT_ONE) && ignoredPlayerName.length == 0) {
            botAction.sendArenaMessage("[PokerBot Error]: Can't use LVZ_SEND_TO_ALL_BUT_ONE with no ignoredPlayerName!");
            return null;
        }
        
        playerName = playerName.toLowerCase();
        int objectId = getFirstUnusedLvzObjectId(playerName, isMapObject);
        if(objectId == -1) {
            botAction.sendArenaMessage("[PokerBot Error]: Ran out of Image Objects to use!");
            return null;
        }

        if(allActiveLVZObjectsOnMap.get(objectId) == null)
            allActiveLVZObjectsOnMap.put(objectId, new ArrayList<String>());
        allActiveLVZObjectsOnMap.get(objectId).add(playerName);
	if(playerName.equals(Constants.LVZ_SEND_TO_ALL_BUT_ONE) && ignoredPlayerName.length > 0) { //ignored player name.
	    ignoredPlayerName[0] = ignoredPlayerName[0].toLowerCase();
	    allActiveLVZObjectsOnMap.get(objectId).add(ignoredPlayerName[0]);
	}
        return new LvzObject(objectId, isMapObject);
    }

    /**
      * Gets all the Active LvzObject's for a specific playerName.
      *
      * @param playerName
      *		The player's (Subspace Name) which is getting a list of active LvzObject's
      * @return LvzObject[]
      *		Array of LvzObject's which this player needs applied to his name.
      *
      */
    public LvzObject[] getAllMyActiveLvzObjects(String playerName) {
	playerName = playerName.toLowerCase();
        List<Integer> allMyLvzObjectIds = new ArrayList<Integer>();

        //First make a list of all LvzObject Id's which are visible to this player.
        for(Map.Entry<Integer, List<String>> entry : allActiveLVZObjectsOnMap.entrySet()) {
            int lvzObjectId = entry.getKey();
            List<String> listOfPlayerNames  = entry.getValue();
            if(listOfPlayerNames != null) {
                if(listOfPlayerNames.contains(Constants.LVZ_SEND_TO_ALL) ||  //global object id, aka playername == "-1".
                  (listOfPlayerNames.contains(Constants.LVZ_SEND_TO_ALL_BUT_ONE) && !listOfPlayerNames.contains(playerName)) || //global object id but ignore this playerName
                  (listOfPlayerNames.contains(playerName) && !listOfPlayerNames.contains(Constants.LVZ_SEND_TO_ALL) && !listOfPlayerNames.contains(Constants.LVZ_SEND_TO_ALL_BUT_ONE)))  //Single player object, only this player.
                    allMyLvzObjectIds.add(lvzObjectId);
            }
        }

        //Next make the final list of all LvzObject's
        List<LvzObject> allMyLvzObjects = new ArrayList<LvzObject>();

        for(List<LvzObjectPackage> lvzObjectPackages : allLvzObjectsByUniqueID.values()) {
            if(lvzObjectPackages == null) continue;
            for(LvzObjectPackage lvzObjectPackage : lvzObjectPackages) {
                if(lvzObjectPackage == null || lvzObjectPackage.getLvzObject() == null) continue;
                if(allMyLvzObjectIds.contains(lvzObjectPackage.getLvzObject().getObjectID()) &&
                (lvzObjectPackage.getPlayerName().equals(Constants.LVZ_SEND_TO_ALL) ||  //global object id, aka playername == "-1".
                  (lvzObjectPackage.getPlayerName().equals(Constants.LVZ_SEND_TO_ALL_BUT_ONE) && !lvzObjectPackage.getIgnoredPlayerName().equals(playerName)) || //global object id but ignore this playerName
                  lvzObjectPackage.getPlayerName().equals(playerName)))  //Single player object, only this player.
                    allMyLvzObjects.add(lvzObjectPackage.getLvzObject());
            }
        }
        return allMyLvzObjects.toArray(new LvzObject[allMyLvzObjects.size()]);
    }

    public void clearAllLvzObjectIdsByPlayerName(String playerName, Boolean... mayRejoin) {
        //playerName == -1 isn't allowed here.
        //If mayRejoin is set, it will only clear non-important Lvz, like Rules.
        
        if (playerName.equals(Constants.LVZ_SEND_TO_ALL)) return;
	playerName = playerName.toLowerCase();
	
	//Creates the mayRejoin delete List, so far only RULES.
        List<Integer> mayRejoinDeleteObjectIds = new ArrayList<Integer>();
        if(mayRejoin.length > 0 && mayRejoin[0]) {
		for(Map.Entry<Integer,List<LvzObjectPackage>> entry : allLvzObjectsByUniqueID.entrySet()) {
			int uniqueId = entry.getKey();
	            	List<LvzObjectPackage> listOfLvzObjectPackages  = entry.getValue();
	            	if(uniqueId == Constants.ID_RULES) {
		            	for(LvzObjectPackage lvzObjectPackage : listOfLvzObjectPackages) {
		            		if(lvzObjectPackage == null || lvzObjectPackage.getLvzObject() == null) continue;
		            		if(lvzObjectPackage.getPlayerName().equals(playerName))
		            			mayRejoinDeleteObjectIds.add(lvzObjectPackage.getLvzObject().getObjectID());
		            	}
	            	}
		}
	}
	
	List<Integer> removedObjectIds = new ArrayList<Integer>();
        //Clears all objectId's used by this player and makes a list of which ones it cleared.
        Iterator<Map.Entry<Integer, List<String>>> iter = allActiveLVZObjectsOnMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer, List<String>> entry = iter.next();
            int lvzObjectId = entry.getKey();
            List <String> listOfPlayerNames = entry.getValue();
            if (listOfPlayerNames != null) {
                if (listOfPlayerNames.contains(playerName)) { //Non-global lvz object id.
                    //If mayRejoin mode is active.
                    if(mayRejoin.length > 0 && mayRejoin[0] && !mayRejoinDeleteObjectIds.contains(lvzObjectId)) 
                	continue;
                    
                    if (allActiveLVZObjectsOnMap.get(lvzObjectId) != null) {
                        //Special Case: If a playerName which also has -2 in that list,
                        //Don't remove the playerName or the -2 attached to it.
                        //Since it's semi-global and players may come back to the arena from lagout/internet disconnect etc...

                        if (allActiveLVZObjectsOnMap.get(lvzObjectId).contains(playerName) &&
                             !allActiveLVZObjectsOnMap.get(lvzObjectId).contains(Constants.LVZ_SEND_TO_ALL_BUT_ONE))
                              allActiveLVZObjectsOnMap.get(lvzObjectId).remove(playerName); //Non-global lvz object.
                        if (allActiveLVZObjectsOnMap.get(lvzObjectId).contains(Constants.LVZ_SEND_TO_ALL)) {
                    		//Trace this bug if it ever happens.
                    		LvzObject buggedLvzObject = null;
                    		for(List<LvzObjectPackage> lvzObjectPackages : allLvzObjectsByUniqueID.values()) {
			            if(lvzObjectPackages == null) continue;
			            for(LvzObjectPackage lvzObjectPackage : lvzObjectPackages) {
			                if(lvzObjectPackage == null || lvzObjectPackage.getLvzObject() == null) continue;
			                if(lvzObjectId == lvzObjectPackage.getLvzObject().getObjectID()) {
			                    buggedLvzObject = lvzObjectPackage.getLvzObject();
			                    break;
			                }
			            }
			        }
                               botAction.sendArenaMessage("[LvzManager Error]: Found -1 for " + ((buggedLvzObject == null) ? "null" : " imgIndex = " + buggedLvzObject.getImageID() + " x: " + buggedLvzObject.getXLocation() + " y: " + buggedLvzObject.getYLocation()));
                        }
                        if (allActiveLVZObjectsOnMap.get(lvzObjectId).size() == 0) //if list size==0 then it's gone
                            iter.remove();
                    }
                    removedObjectIds.add(lvzObjectId);
                }
            } else {
                iter.remove();
            }
        }

	List<LvzObjectPackage> listOfRemovedLvzObjectPackages = new ArrayList<LvzObjectPackage>();
	    
        //Removes any of the unique Id keys that have all the removed objectId's LVZ's as the values.
        Iterator<Map.Entry<Integer,List<LvzObjectPackage>>> iter2 = allLvzObjectsByUniqueID.entrySet().iterator();
        while (iter2.hasNext()) {
            Map.Entry<Integer, List<LvzObjectPackage>> entry = iter2.next();
            int uniqueId = entry.getKey();
            List<LvzObjectPackage> listOfLvzObjectPackages  = entry.getValue();
	    
	    listOfRemovedLvzObjectPackages.clear();
	    
            if(listOfLvzObjectPackages != null) {
            	for(LvzObjectPackage lvzObjectPackage : listOfLvzObjectPackages) {
            		if(lvzObjectPackage == null || lvzObjectPackage.getLvzObject() == null) continue;
            		if(removedObjectIds.contains(lvzObjectPackage.getLvzObject().getObjectID()))
            			listOfRemovedLvzObjectPackages.add(lvzObjectPackage);
            	}
                listOfLvzObjectPackages.removeAll(listOfRemovedLvzObjectPackages);
                if(listOfLvzObjectPackages.size() == 0)
                    iter2.remove();
            } else {
                iter2.remove();
            }
        }
    }

    public void renderAllLvzForPlayer(String playerName) {
        //Renders all the globals objects and those not directed to playerName but still global objects.
	playerName = playerName.toLowerCase();
        LvzObject[] allMyActiveLvzObjects = getAllMyActiveLvzObjects(playerName);
        HashMap<Integer,Boolean>allMyActiveLvzObjectsOn = new HashMap<Integer,Boolean>();

        for(LvzObject lvz : allMyActiveLvzObjects) {
            if(lvz != null)
                allMyActiveLvzObjectsOn.put(lvz.getObjectID(), true);
        }
        
        int playerId = botAction.getPlayerID(playerName);
        if(playerId == -1) {
        	botAction.sendSmartPrivateMessage(playerName, "Couldn't load your lvz objects, try to rejoin the arena ?go poker");
        	return;
        }

        botAction.manuallySetObjectModifications(playerId,  new LinkedList<LvzObject>(Arrays.asList(allMyActiveLvzObjects)));
        botAction.manuallySetObjects(allMyActiveLvzObjectsOn, playerId);
    }

    private void clearUsedLvzObjectId(int objectId, String playerName) {
    	//-1 or -2 both act the same here, removes it for everybody.
    	//if it's just a playerName without -1 or -2 attached to it, then it just remove a single player object.
    	
        if(objectId <= 0 || objectId > (Constants.MAX_LVZ_OBJECTS+Constants.START_SCREEN_LVZ_OBJECTS)) return;
	playerName = playerName.toLowerCase();
	
        if(allActiveLVZObjectsOnMap.get(objectId) != null) {               
            //Non-global objects. Excludes if contains -1 or -2
            if(!playerName.equals(Constants.LVZ_SEND_TO_ALL) &&
                !playerName.equals(Constants.LVZ_SEND_TO_ALL_BUT_ONE) &&
                !allActiveLVZObjectsOnMap.get(objectId).contains(Constants.LVZ_SEND_TO_ALL) && 
                !allActiveLVZObjectsOnMap.get(objectId).contains(Constants.LVZ_SEND_TO_ALL_BUT_ONE) && 
                 allActiveLVZObjectsOnMap.get(objectId).contains(playerName))
                    allActiveLVZObjectsOnMap.get(objectId).remove(playerName);
                    
            //If list contains -1 or -2 or size==0 then it's removed.
            if(allActiveLVZObjectsOnMap.get(objectId).contains(Constants.LVZ_SEND_TO_ALL) ||
                allActiveLVZObjectsOnMap.get(objectId).contains(Constants.LVZ_SEND_TO_ALL_BUT_ONE) ||
                allActiveLVZObjectsOnMap.get(objectId).size() == 0)
                	allActiveLVZObjectsOnMap.remove(objectId);
        }
    }

    public void clearLvzByUniqueId(int id, String... playerName) {
    	if(allLvzObjectsByUniqueID.get(id) == null) return;
    	
    	//Makes a copy of the returned List<LvzObjectPackage> to avoid ConcurrentException.
        List<LvzObjectPackage> lvzObjectPackages = new ArrayList<LvzObjectPackage>(allLvzObjectsByUniqueID.get(id));
	List<LvzObject> listOfLvzObjects = new ArrayList<LvzObject>();
        int playerId = -1;
	String ignoredPlayerName = "";
	int typeSendFound = 123;
	boolean mismatchFound = false;
	LvzObject lvz;
	
        if(lvzObjectPackages != null) {
            for(LvzObjectPackage lvzObjectPackage : lvzObjectPackages) {
                if(lvzObjectPackage == null ||
                    lvzObjectPackage.getPlayerName() == null ||
                    lvzObjectPackage.getLvzObject() == null) continue;
                
                lvz = lvzObjectPackage.getLvzObject();
                
                if(playerName.length == 0 && lvzObjectPackage.getPlayerName().equals(Constants.LVZ_SEND_TO_ALL)) {
                    	botAction.setupObject(lvz.getObjectID(), false);
                    	
                	if(typeSendFound == 123 || typeSendFound == -1)
                		typeSendFound = -1;
                	else
                		mismatchFound = true;
                	//Get's rid of unique ID completely if it's -1 or -2
                	allLvzObjectsByUniqueID.remove(id);
                } else if(playerName.length == 0 && lvzObjectPackage.getPlayerName().equals(Constants.LVZ_SEND_TO_ALL_BUT_ONE)) {
       	        	ignoredPlayerName = lvzObjectPackage.getIgnoredPlayerName();
			listOfLvzObjects.add(lvz);
			
                	if(typeSendFound == 123 || typeSendFound == -2)
                		typeSendFound = -2;
                	else
                		mismatchFound = true;
                	//Get's rid of unique ID completely if it's -1 or -2
                	allLvzObjectsByUniqueID.remove(id); 
                } else if(playerName.length > 0 && lvzObjectPackage.getPlayerName().equals(playerName[0].toLowerCase())) {
                	playerId = botAction.getPlayerID(playerName[0].toLowerCase());
                	if(playerId == -1) continue;
		      	botAction.setupObject(playerId, lvz.getObjectID(), false);
		      	
                	if(typeSendFound == 123 || typeSendFound == 0)
                		typeSendFound = 0;
                	else
                		mismatchFound = true;
                	 //Gets rid of only one of the playerName's from that unique specific ID.
                	 allLvzObjectsByUniqueID.get(id).remove(lvzObjectPackage);
                	 //Get's rid of unique ID completely since no more playerName's left on it
                	 if(allLvzObjectsByUniqueID.get(id).size() == 0)
                	 	allLvzObjectsByUniqueID.remove(id); 
		}
                clearUsedLvzObjectId(lvz.getObjectID(), lvzObjectPackage.getPlayerName());
            }
            
            if(mismatchFound)
            	 botAction.sendArenaMessage("[LvzManager Error]: [Id: " + id + "] Found multiple playerNames, graphical glitches will occur!");
            
            if(typeSendFound == -1) {
                botAction.sendSetupObjects(); //send lvz hide package to all.
            } else if(typeSendFound == -2) {
                //This will do until they add more commands in BotAction.
        	if(listOfLvzObjects.size() == 0) return;
        	
        	List<Integer> listOfPlayerIds = new ArrayList<Integer>();
        
                Map<Integer, Player> m_playerMap = botAction.getPlayerMap();
	        Iterator<Map.Entry<Integer, Player>> iter = m_playerMap.entrySet().iterator();

	        synchronized(m_playerMap) {
	            while (iter.hasNext())
	            {
	            	Map.Entry<Integer, Player> entry = iter.next();
	            	int playerIdCheck = entry.getKey();
           		Player player = entry.getValue();
			
			if(player == null || playerIdCheck < 0) continue;
			
	            	//If Player doesn't match ignorePlayerName.
	                if(!ignoredPlayerName.equals(player.getPlayerName().toLowerCase()))
	                	listOfPlayerIds.add(playerIdCheck);
	            }
	         }
	         
	         if(listOfPlayerIds.size() == 0) return;

	         for(int playerIdd : listOfPlayerIds) {
		         for(LvzObject lvzObject : listOfLvzObjects) {
		         	if(lvzObject == null) continue;
		          	botAction.hideObjectForPlayer(playerIdd, lvzObject.getObjectID());
		         }
	         }
            } else if(playerId != -1) {
                botAction.sendSetupObjectsForPlayer(playerId); //send lvz hide package to single player.
            }
        }
    }

    public boolean isLvzByUniqueIdShown(int id, String playerName) {
        //Checks if you already sent a lvz with the same unique Id and playerName
        //Only checks first match, since unique ID's must be sent as one package!
        
        //TODO: 
        //For this to work properly no unique ID package must have more then 1 purpose.
        //Either -1 or -2 or just playerName, no mixes.
        playerName = playerName.toLowerCase();
        	
        List<LvzObjectPackage> lvzObjectPackages = allLvzObjectsByUniqueID.get(id);
        LvzObject lvz;
        
        if(lvzObjectPackages != null) {
            for(LvzObjectPackage lvzObjectPackage : lvzObjectPackages) {
            	if(lvzObjectPackage == null || lvzObjectPackage.getLvzObject() == null) continue;

            	lvz = lvzObjectPackage.getLvzObject();

                if(allActiveLVZObjectsOnMap.get(lvz.getObjectID()) != null) {
                	if(playerName.equals(Constants.LVZ_SEND_TO_ALL_BUT_ONE) &&
                	    allActiveLVZObjectsOnMap.get(lvz.getObjectID()).contains(Constants.LVZ_SEND_TO_ALL_BUT_ONE))
                	        return true;
                	    
                	if(playerName.equals(Constants.LVZ_SEND_TO_ALL) &&
                	    allActiveLVZObjectsOnMap.get(lvz.getObjectID()).contains(Constants.LVZ_SEND_TO_ALL))
                	        return true;  
                	             
                	if(allActiveLVZObjectsOnMap.get(lvz.getObjectID()).contains(Constants.LVZ_SEND_TO_ALL_BUT_ONE) &&
                    	    allActiveLVZObjectsOnMap.get(lvz.getObjectID()).contains(playerName))
                    		return false;
                
                	if(allActiveLVZObjectsOnMap.get(lvz.getObjectID()).contains(playerName))
                    		return true;
                }
            }
        }
        return false;
   }

   public void drawMapStringToPlayer(int id, String playerName, String message, int xPixels, int yPixels, int widthInPixels) {
	if(playerName.equals(Constants.LVZ_SEND_TO_ALL) || playerName.equals(Constants.LVZ_SEND_TO_ALL_BUT_ONE)) {
		botAction.sendArenaMessage("[LvzManager Error]: Can't use LVZ_SEND_TO_ALL or LVZ_SEND_TO_ALL_BUT_ONE in drawMapStringToPlayer 1");
		return; 
	}
	
    	Object[] arguments = {
   		new Object[] {"id", id},
   		new Object[] {"playerName", playerName},
   		new Object[] {"message", message},
   		new Object[] {"xPixels", xPixels},
   		new Object[] {"yPixels", yPixels},
   		new Object[] {"widthInPixels", widthInPixels},
   		new Object[] {"mapObject"}
   	};
   	drawString(arguments);
   }

   public void drawMapStringToPlayer(int id, String playerName, String message, int xPixels, int yPixels) {
	if(playerName.equals(Constants.LVZ_SEND_TO_ALL) || playerName.equals(Constants.LVZ_SEND_TO_ALL_BUT_ONE)) {
		botAction.sendArenaMessage("[LvzManager Error]: Can't use LVZ_SEND_TO_ALL or LVZ_SEND_TO_ALL_BUT_ONE in drawMapStringToPlayer 2");
		return; 
	}
	
	Object[] arguments = {
   		new Object[] {"id", id},
   		new Object[] {"playerName", playerName},
   		new Object[] {"message", message},
   		new Object[] {"xPixels", xPixels},
   		new Object[] {"yPixels", yPixels},
   		new Object[] {"mapObject"}
   	};
   	drawString(arguments);
   }
   
    public void drawMapStringToAll(int id, String message, int xPixels, int yPixels, int widthInPixels) {
   	Object[] arguments = {
   		new Object[] {"id", id},
   		new Object[] {"playerName", Constants.LVZ_SEND_TO_ALL},
   		new Object[] {"message", message},
   		new Object[] {"xPixels", xPixels},
   		new Object[] {"yPixels", yPixels},
   		new Object[] {"widthInPixels", widthInPixels},
   		new Object[] {"mapObject"}
   	};
   	drawString(arguments);
   }
   
   public void drawMapStringToAll(int id, String message, int xPixels, int yPixels) {
   	Object[] arguments = {
   		new Object[] {"id", id},
   		new Object[] {"playerName", Constants.LVZ_SEND_TO_ALL},
   		new Object[] {"message", message},
   		new Object[] {"xPixels", xPixels},
   		new Object[] {"yPixels", yPixels},
   		new Object[] {"mapObject"}
   	};
   	drawString(arguments);
   }
  
   public void drawMapStringToAllButIgnorePlayer(int id, String ignorePlayerName, String message, int xPixels, int yPixels, int widthInPixels) {
   	Object[] arguments = {
   		new Object[] {"id", id},
   		new Object[] {"playerName", Constants.LVZ_SEND_TO_ALL_BUT_ONE},
   		new Object[] {"ignorePlayerName", ignorePlayerName}, 		
   		new Object[] {"message", message},
   		new Object[] {"xPixels", xPixels},
   		new Object[] {"yPixels", yPixels},
   		new Object[] {"widthInPixels", widthInPixels},
   		new Object[] {"mapObject"}
   	};
   	drawString(arguments);
   }

   public void drawMapStringToAllButIgnorePlayer(int id, String ignorePlayerName, String message, int xPixels, int yPixels) {
   	Object[] arguments = {
   		new Object[] {"id", id},
   		new Object[] {"playerName", Constants.LVZ_SEND_TO_ALL_BUT_ONE},
   		new Object[] {"ignorePlayerName", ignorePlayerName}, 		
   		new Object[] {"message", message},
   		new Object[] {"xPixels", xPixels},
   		new Object[] {"yPixels", yPixels},
   		new Object[] {"mapObject"}
   	};
   	drawString(arguments);
   }

   public void drawScreenStringToPlayer(int id, String playerName, String message, CoordType coordTypeX, CoordType coordTypeY, int xPixels, int yPixels, int widthInPixels) {
	if(playerName.equals(Constants.LVZ_SEND_TO_ALL) || playerName.equals(Constants.LVZ_SEND_TO_ALL_BUT_ONE)) {
		botAction.sendArenaMessage("[LvzManager Error]: Can't use LVZ_SEND_TO_ALL or LVZ_SEND_TO_ALL_BUT_ONE in drawMapStringToPlayer 1");
		return; 
	}
	
    	Object[] arguments = {
   		new Object[] {"id", id},
   		new Object[] {"playerName", playerName},
   		new Object[] {"message", message},
   		new Object[] {"xCoordType", coordTypeX.ordinal()},
   		new Object[] {"yCoordType", coordTypeY.ordinal()},
   		new Object[] {"xPixels", xPixels},
   		new Object[] {"yPixels", yPixels},
   		new Object[] {"widthInPixels", widthInPixels},
   		new Object[] {"screenObject"}
   	};
   	drawString(arguments);
   }

   public void drawScreenStringToPlayer(int id, String playerName, String message, CoordType coordTypeX, CoordType coordTypeY, int xPixels, int yPixels) {
	if(playerName.equals(Constants.LVZ_SEND_TO_ALL) || playerName.equals(Constants.LVZ_SEND_TO_ALL_BUT_ONE)) {
		botAction.sendArenaMessage("[LvzManager Error]: Can't use LVZ_SEND_TO_ALL or LVZ_SEND_TO_ALL_BUT_ONE in drawMapStringToPlayer 2");
		return; 
	}
	
	Object[] arguments = {
   		new Object[] {"id", id},
   		new Object[] {"playerName", playerName},
   		new Object[] {"message", message},
   		new Object[] {"xCoordType", coordTypeX.ordinal()},
   		new Object[] {"yCoordType", coordTypeY.ordinal()},
   		new Object[] {"xPixels", xPixels},
   		new Object[] {"yPixels", yPixels},
   		new Object[] {"screenObject"}
   	};
   	drawString(arguments);
   }

    public void drawScreenStringToAll(int id, String message, CoordType coordTypeX, CoordType coordTypeY, int xPixels, int yPixels, int widthInPixels) {
   	Object[] arguments = {
   		new Object[] {"id", id},
   		new Object[] {"playerName", Constants.LVZ_SEND_TO_ALL},
   		new Object[] {"message", message},
   		new Object[] {"xCoordType", coordTypeX.ordinal()},
   		new Object[] {"yCoordType", coordTypeY.ordinal()},
   		new Object[] {"xPixels", xPixels},
   		new Object[] {"yPixels", yPixels},
   		new Object[] {"widthInPixels", widthInPixels},
   		new Object[] {"screenObject"}
   	};
   	drawString(arguments);
   }
   
   public void drawScreenStringToAll(int id, String message, CoordType coordTypeX, CoordType coordTypeY, int xPixels, int yPixels) {
   	Object[] arguments = {
   		new Object[] {"id", id},
   		new Object[] {"playerName", Constants.LVZ_SEND_TO_ALL},
   		new Object[] {"message", message},
   		new Object[] {"xCoordType", coordTypeX.ordinal()},
   		new Object[] {"yCoordType", coordTypeY.ordinal()},
   		new Object[] {"xPixels", xPixels},
   		new Object[] {"yPixels", yPixels},
   		new Object[] {"screenObject"}
   	};
   	drawString(arguments);
   }

   public void drawScreenStringToAllButIgnorePlayer(int id, String ignorePlayerName, String message, CoordType coordTypeX, CoordType coordTypeY, int xPixels, int yPixels, int widthInPixels) {
   	Object[] arguments = {
   		new Object[] {"id", id},
   		new Object[] {"playerName", Constants.LVZ_SEND_TO_ALL_BUT_ONE},
   		new Object[] {"ignorePlayerName", ignorePlayerName}, 		
   		new Object[] {"message", message},
   		new Object[] {"xCoordType", coordTypeX.ordinal()},
   		new Object[] {"yCoordType", coordTypeY.ordinal()},
   		new Object[] {"xPixels", xPixels},
   		new Object[] {"yPixels", yPixels},
   		new Object[] {"widthInPixels", widthInPixels},
   		new Object[] {"screenObject"}
   	};
   	drawString(arguments);
   }

   public void drawScreenStringToAllButIgnorePlayer(int id, String ignorePlayerName, String message, CoordType coordTypeX, CoordType coordTypeY, int xPixels, int yPixels) {
   	Object[] arguments = {
   		new Object[] {"id", id},
   		new Object[] {"playerName", Constants.LVZ_SEND_TO_ALL_BUT_ONE},
   		new Object[] {"ignorePlayerName", ignorePlayerName}, 		
   		new Object[] {"message", message},
   		new Object[] {"xCoordType", coordTypeX.ordinal()},
   		new Object[] {"yCoordType", coordTypeY.ordinal()},
   		new Object[] {"xPixels", xPixels},
   		new Object[] {"yPixels", yPixels},	
   		new Object[] {"screenObject"}
   	};
   	drawString(arguments);
   }

   private void drawString(Object[] args) {
   	if(args == null || args.length == 0) {
        	botAction.sendArenaMessage("[LvzManager Error]: Glitch? drawString arguments are empty.");
		return; 
        }
        
        //~~~~ Parameters ~~~~
	   String playerName, ignorePlayerName, message; 
	   playerName = ignorePlayerName = message = ""; //beats making it null less checking code.
	   int id, xPixels, yPixels, widthInPixels;
	   id = xPixels = yPixels = widthInPixels = -1;
	   byte xCoordType, yCoordType;
	   xCoordType = yCoordType = -1;
   	   Boolean isMapObject = null; //using non-primitive to detect errors.
   	//~~~~~~~~~~~~~~~~~
   	
        boolean isLeftOfArgument = true;
        String lastCommand = "";
        
   	for(Object argument : args) {
   		if(argument == null) continue;
   		Object[] argsValues = (Object[]) argument;
   		for(Object value : argsValues) {
	   		if(isLeftOfArgument && value instanceof String)
	   			lastCommand = (String)value;
	   		
	   		if(!isLeftOfArgument) {
	   			if(lastCommand.equalsIgnoreCase("id") && value instanceof Integer)
	   				id = (Integer)value;
	   			else if(lastCommand.equalsIgnoreCase("playerName") && value instanceof String)
	   				playerName = (String)value;
	   			else if(lastCommand.equalsIgnoreCase("ignorePlayerName") && value instanceof String)
	   				ignorePlayerName = (String)value;
	   			else if(lastCommand.equalsIgnoreCase("message") && value instanceof String)
	   				message = (String)value;
	   			else if(lastCommand.equalsIgnoreCase("xPixels") && value instanceof Integer)
	   				xPixels = (Integer)value;
	   			else if(lastCommand.equalsIgnoreCase("yPixels") && value instanceof Integer)
	   				yPixels = (Integer)value;
	   			else if(lastCommand.equalsIgnoreCase("widthInPixels") && value instanceof Integer)
	   				widthInPixels = (Integer)value;
	   			else if(lastCommand.equalsIgnoreCase("xCoordType") && value instanceof Integer)
	   				xCoordType = (byte)((Integer)value).intValue();
	   			else if(lastCommand.equalsIgnoreCase("yCoordType") && value instanceof Integer)
	   				yCoordType = (byte)((Integer)value).intValue();
	   		}
	   		
	   		if(lastCommand.equalsIgnoreCase("mapObject"))
	   			isMapObject = true;
	   		else if(lastCommand.equalsIgnoreCase("screenObject"))
	   			isMapObject = false;
	   			
	   		isLeftOfArgument = false;
   		}
   		isLeftOfArgument = true;
   	}
   	
   	if( id == -1 || playerName.isEmpty() || message.isEmpty() || xPixels == -1 || yPixels == -1 || isMapObject == null) {
        	botAction.sendArenaMessage("[LvzManager Error]: Glitch? drawString arguments not initialized.");
		return; 
   	}
   	
   	if(!isMapObject && (xCoordType == -1 || yCoordType == -1)) {
        	botAction.sendArenaMessage("[LvzManager Error]: Glitch? drawString screenObject arguments not initialized.");
		return; 
   	}

        //if playerName == -1 then it's for all players, 
        //if playerName == -2 then it's for all players, but not ignorePlayerName,
        //otherwise it's for single player.
        int lvzLetterObjectIndex;
	List<LvzObject> listOfLvzObjects = new ArrayList<LvzObject>();
	
	playerName = playerName.toLowerCase();
	ignorePlayerName = ignorePlayerName.toLowerCase();
	
        //Finds the proper x coordinate in pixels to start from to have string centered based on size.
        //Width is used only for isCentered, so it's optional parameter, if not set it won't center
        //Formula: (Width to center in - (msgSize * 8 pixels width per letter)) / 2 [half of image]
        //Make sure Width can be divided evenly by 8 [pixels width per letter].
        if(widthInPixels > 0)
            xPixels += (widthInPixels - (message.length() * 8)) / 2;

        for(int i = 0; i < message.length(); i++) {
            lvzLetterObjectIndex = Constants.FONT.indexOf(message.charAt(i)) == -1 ? -1 : (Constants.firstFontLetterImageIndex + Constants.FONT.indexOf(message.charAt(i)));

            //Any letter that is not found in font char-array skip [8x8] position to act as  blank.
            if(lvzLetterObjectIndex != -1) {
            	LvzObject letterObject;
            	if(!ignorePlayerName.isEmpty())
                	letterObject = getNewLvzObject(playerName, isMapObject, ignorePlayerName);
                else
                	letterObject = getNewLvzObject(playerName, isMapObject);
		
                if(letterObject != null) {
	            if(!isMapObject) {
	                //C and S seem like the only useful CoordType's
		        letterObject = letterObject.setXLocationType(xCoordType); //Stats box, lower right corner
		        letterObject = letterObject.setYLocationType(yCoordType); //Stats box, lower right corner
		    }
                    letterObject = letterObject.setXLocation(xPixels);
                    letterObject = letterObject.setYLocation(yPixels);
                    letterObject = letterObject.updateLocation(true);
                    letterObject = letterObject.setImageID(lvzLetterObjectIndex);
                    letterObject = letterObject.updateImage(true);

                    if(allLvzObjectsByUniqueID.get(id) == null)
                        allLvzObjectsByUniqueID.put(id, new ArrayList<LvzObjectPackage>());

                    allLvzObjectsByUniqueID.get(id).add(new LvzObjectPackage(letterObject, playerName, ignorePlayerName));

                    if(playerName.equals(Constants.LVZ_SEND_TO_ALL)) {
                        botAction.setupObjectModification(letterObject); //setup mod for all players.
                        botAction.setupObject(letterObject.getObjectID(), true); //setup visible for all players.
                    } else if(playerName.equals(Constants.LVZ_SEND_TO_ALL_BUT_ONE)) { //Build a list of Lvz's here
                    	listOfLvzObjects.add(letterObject);
                    } else {
		        int playerId = botAction.getPlayerID(playerName);
		        if(playerId == -1) return;
                        botAction.setupObjectModification(playerId, letterObject); //setup mod for single player.
                        botAction.setupObject(playerId, letterObject.getObjectID(), true); //setup visible for single player.
                    }
                } else {
                    botAction.sendArenaMessage("[LvzManager Error]: Couldn't use drawString(), most likely ran out object ids");
                    return;
                }
            }
            xPixels += 8;
        }

        //Send packages
        if(playerName.equals(Constants.LVZ_SEND_TO_ALL)) { //send it to all
            botAction.sendSetupObjectModifications(); //setup mod for all.
            botAction.sendSetupObjects(); //setup visible for all.
        } else if(playerName.equals(Constants.LVZ_SEND_TO_ALL_BUT_ONE)) { //send it to all but ignore one player.
                //This will do until they add more commands in BotAction.
        	if(listOfLvzObjects.size() == 0) return;
        	
        	List<Integer> listOfPlayerIds = new ArrayList<Integer>();
        
                Map<Integer, Player> m_playerMap = botAction.getPlayerMap();
	        Iterator<Map.Entry<Integer, Player>> iter = m_playerMap.entrySet().iterator();

	        synchronized(m_playerMap) {
	            while (iter.hasNext())
	            {
	            	Map.Entry<Integer, Player> entry = iter.next();
	            	int playerId = entry.getKey();
           		Player player = entry.getValue();
			
			if(player == null || playerId < 0) continue;
			
	            	//If Player doesn't match ignorePlayerName.
	                if(!ignorePlayerName.equals(player.getPlayerName().toLowerCase()))
	                	listOfPlayerIds.add(playerId);
	            }
	         }
	         
	         if(listOfPlayerIds.size() == 0) return;

	         for(int playerId : listOfPlayerIds) {
		         for(LvzObject lvzObject : listOfLvzObjects) {
		         	if(lvzObject == null) continue;
		         	botAction.modifyObjectForPlayer(playerId, lvzObject);
		         	botAction.showObjectForPlayer(playerId, lvzObject.getObjectID());
		         }
	         }
        } else { //send it to single player.
	    int playerId = botAction.getPlayerID(playerName);
	    if(playerId == -1) return;
            botAction.sendSetupObjectModificationsForPlayer(playerId); //setup mod for single player.
            botAction.sendSetupObjectsForPlayer(playerId); //setup visible for single player.
        }
    }

    public void drawMapImageToPlayer(String playerName, int id, int x, int y, int imageIndex) {
	if(playerName.equals(Constants.LVZ_SEND_TO_ALL) || playerName.equals(Constants.LVZ_SEND_TO_ALL_BUT_ONE)) {
		botAction.sendArenaMessage("[LvzManager Error]: Can't use LVZ_SEND_TO_ALL or LVZ_SEND_TO_ALL_BUT_ONE in drawMapImageToPlayer 1");
		return; 
	}
	
    	Object[] arguments = {
    		new Object[] {"playerName", playerName},
   		new Object[] {"images", new String[] {id + "," + x + "," + y + "," + imageIndex}},
   		new Object[] {"mapObject"}
   	};
   	drawImage(arguments);
    }

    public void drawMapImageToPlayer(String playerName, List<String> images) {
	if(playerName.equals(Constants.LVZ_SEND_TO_ALL) || playerName.equals(Constants.LVZ_SEND_TO_ALL_BUT_ONE)) {
		botAction.sendArenaMessage("[LvzManager Error]: Can't use LVZ_SEND_TO_ALL or LVZ_SEND_TO_ALL_BUT_ONE in drawMapImageToPlayer 2");
		return; 
	}
	
    	Object[] arguments = {
    		new Object[] {"playerName", playerName},
   		new Object[] {"images", images.toArray(new String[0])},
   		new Object[] {"mapObject"}
   	};
   	drawImage(arguments);
    }
    
    public void drawMapImageToAll(int id, int x, int y, int imageIndex) {
        //Only when you have to draw one image for that unique Id.
        Object[] arguments = {
    		new Object[] {"playerName", Constants.LVZ_SEND_TO_ALL},
   		new Object[] {"images", new String[] {id + "," + x + "," + y + "," + imageIndex}},
   		new Object[] {"mapObject"}
   	};
   	drawImage(arguments);
    }
    
    public void drawMapImageToAll(List<String> images) {
        //Only when you have to draw one image for that unique Id.
        Object[] arguments = {
    		new Object[] {"playerName", Constants.LVZ_SEND_TO_ALL},
   		new Object[] {"images", images.toArray(new String[0])},
   		new Object[] {"mapObject"}
   	};
   	drawImage(arguments);
    }
    
    public void drawMapImageToAllButIgnorePlayer(String ignorePlayerName, int id, int x, int y, int imageIndex) {
        //Only when you have to draw one image for that unique Id.
        Object[] arguments = {
    		new Object[] {"playerName", Constants.LVZ_SEND_TO_ALL_BUT_ONE},
    		new Object[] {"ignorePlayerName", ignorePlayerName},  		
   		new Object[] {"images", new String[] {id + "," + x + "," + y + "," + imageIndex}},
   		new Object[] {"mapObject"}
   	};
   	drawImage(arguments);
    }
    
    public void drawMapImageToAllButIgnorePlayer(String ignorePlayerName, List<String> images) {
        //Only when you have to draw one image for that unique Id.
        Object[] arguments = {
    		new Object[] {"playerName", Constants.LVZ_SEND_TO_ALL_BUT_ONE},
    		new Object[] {"ignorePlayerName", ignorePlayerName},  		
   		new Object[] {"images", images.toArray(new String[0])},
   		new Object[] {"mapObject"}
   	};
   	drawImage(arguments);
    }
    
    public void drawScreenImageToPlayer(String playerName, int id, CoordType coordTypeX, CoordType coordTypeY, int x, int y, int imageIndex) {
        //Only when you have to draw one screen image for that unique Id.
	if(playerName.equals(Constants.LVZ_SEND_TO_ALL) || playerName.equals(Constants.LVZ_SEND_TO_ALL_BUT_ONE)) {
		botAction.sendArenaMessage("[LvzManager Error]: Can't use LVZ_SEND_TO_ALL or LVZ_SEND_TO_ALL_BUT_ONE in drawScreenImageToPlayer 1");
		return; 
	}
	
    	Object[] arguments = {
    		new Object[] {"playerName", playerName},
   		new Object[] {"images", new String[] {id + "," + coordTypeX.ordinal() + "," + coordTypeY.ordinal() + "," + x + "," + y + "," + imageIndex}},
   		new Object[] {"screenObject"}
   	};
   	drawImage(arguments);
   }
    
    public void drawScreenImageToAll(int id, CoordType coordTypeX, CoordType coordTypeY, int x, int y, int imageIndex) {
        //Only when you have to draw one screen image for that unique Id.	
    	Object[] arguments = {
    		new Object[] {"playerName", Constants.LVZ_SEND_TO_ALL},
   		new Object[] {"images", new String[] {id + "," + coordTypeX.ordinal() + "," + coordTypeY.ordinal() + "," + x + "," + y + "," + imageIndex}},
   		new Object[] {"screenObject"}
   	};
   	drawImage(arguments);
    }

    public void drawScreenImageToAllButIgnorePlayer(String ignorePlayerName, int id, CoordType coordTypeX, CoordType coordTypeY, int x, int y, int imageIndex) {
        //Only when you have to draw one screen image for that unique Id.
    	Object[] arguments = {
    		new Object[] {"playerName", Constants.LVZ_SEND_TO_ALL_BUT_ONE},
    		new Object[] {"ignorePlayerName", ignorePlayerName},	
   		new Object[] {"images", new String[] {id + "," + coordTypeX.ordinal() + "," + coordTypeY.ordinal() + "," + x + "," + y + "," + imageIndex}},
   		new Object[] {"screenObject"}
   	};
   	drawImage(arguments);
   }

    private void drawImage(Object[] args) {
        //if playerName == -1 then it's for all players, otherwise it's for single player.
        //For mapObject
        //images = {"uniqueId,x,y,imageIndex},{"uniqueId,x,y,imageIndex}, etc...
        //For screenObject
        //images = {"uniqueId,coordTypeX,coordTypeY,x,y,imageIndex"},{"uniqueId,coordTypeX,coordTypeY,x,y,imageIndex"}, etc...
        //The unique id is important because it's used for 2 purposes.
        //Since the Lvz ObjectId's are already unpredictable from a pile of un-used objectId's.
        //This makes purpose of the unique id is to be able to distinguish which LvzObject belongs where.
        //Also in drawString the unique id is used to make a bunch of LvzObjects act as one object.
        //You could also bunch together a bunch of drawImage's on same unique id.
        //Which you will be able to turn off all the drawImage's at once, by just one command.

        if(args == null || args.length == 0) {
        	botAction.sendArenaMessage("[LvzManager Error]: Glitch? drawImage arguments are empty.");
		return; 
        }

	//~~~~Parameters~~~~
	String playerName, ignorePlayerName;
	playerName = ignorePlayerName = ""; //beats making it null less checking code.
	Boolean isMapObject = null; //using non-primitive to detect errors.
	String[] images = null;
	//~~~~~~~~~~~~~~~
        
        boolean isLeftOfArgument = true;
        String lastCommand = "";
        
   	for(Object argument : args) {
   		if(argument == null) continue;
   		Object[] argsValues = (Object[]) argument;
   		for(Object value : argsValues) {
	   		if(isLeftOfArgument && value instanceof String)
	   			lastCommand = (String)value;
	   		
	   		if(!isLeftOfArgument) {
	   			if(lastCommand.equalsIgnoreCase("playerName") && value instanceof String)
	   				playerName = (String)value;
	   			else if(lastCommand.equalsIgnoreCase("ignorePlayerName") && value instanceof String)
	   				ignorePlayerName = (String)value;
	   			else if(lastCommand.equalsIgnoreCase("images") && value instanceof String[])
	   				images = (String[])value;
	   		}
	   		
	   		if(lastCommand.equalsIgnoreCase("mapObject"))
	   			isMapObject = true;
	   		else if(lastCommand.equalsIgnoreCase("screenObject"))
	   			isMapObject = false;
	   				
	   		isLeftOfArgument = false;
   		}
   		isLeftOfArgument = true;
   	}

	if(images == null || images.length == 0) {
		botAction.sendArenaMessage("[LvzManager Error]: Glitch? drawImage, images are empty.");
		return;
	}
	if(playerName.isEmpty()) {
		botAction.sendArenaMessage("[LvzManager Error]: Glitch? drawImage, playerName is empty.");
		return;
	}
	if(isMapObject == null) {
		botAction.sendArenaMessage("[LvzManager Error]: Glitch? drawImage, isMapObject not initialized.");
		return;
	}
	
        int id, xPixels, yPixels, imageIndex;
        id = xPixels = yPixels = imageIndex = -1;
        byte xCoordType, yCoordType;
        xCoordType = yCoordType = -1;
	List<LvzObject> listOfLvzObjects = new ArrayList<LvzObject>();
        String[] splitData;
        
	playerName = playerName.toLowerCase();
	ignorePlayerName = ignorePlayerName.toLowerCase();
	
        for(String imageData : images) {
            splitData = imageData.split(",");
            if(isMapObject && splitData.length != 4) continue;
            if(!isMapObject && splitData.length != 6) continue;       
            
            id = Integer.parseInt(splitData[0]);
            
            if(isMapObject) {
            	xPixels = Integer.parseInt(splitData[1]);
            	yPixels = Integer.parseInt(splitData[2]);
            	imageIndex = Integer.parseInt(splitData[3]);
            } else if(!isMapObject) {
	        xCoordType = Byte.parseByte(splitData[1]);
	        yCoordType = Byte.parseByte(splitData[2]);
	        xPixels = Integer.parseInt(splitData[3]);
	        yPixels = Integer.parseInt(splitData[4]);
	        imageIndex = Integer.parseInt(splitData[5]);
            }

            LvzObject lvzImageObject;
            if(!ignorePlayerName.isEmpty())
                lvzImageObject= getNewLvzObject(playerName, isMapObject, ignorePlayerName);
	    else
                lvzImageObject= getNewLvzObject(playerName, isMapObject);

            if(lvzImageObject != null) {
            	if(!isMapObject) { //C and S seem like the only useful CoordType's
	                lvzImageObject = lvzImageObject.setXLocationType(xCoordType); //Stats box, lower right corner
	                lvzImageObject = lvzImageObject.setYLocationType(yCoordType); //Stats box, lower right corner
	        }
                lvzImageObject = lvzImageObject.setXLocation(xPixels);
                lvzImageObject = lvzImageObject.setYLocation(yPixels);
                lvzImageObject = lvzImageObject.updateLocation(true);
                lvzImageObject = lvzImageObject.setImageID(imageIndex);
                lvzImageObject = lvzImageObject.updateImage(true);

                if(allLvzObjectsByUniqueID.get(id) == null)
                    allLvzObjectsByUniqueID.put(id, new ArrayList<LvzObjectPackage>());

                allLvzObjectsByUniqueID.get(id).add(new LvzObjectPackage(lvzImageObject, playerName, ignorePlayerName));

                if(playerName.equals(Constants.LVZ_SEND_TO_ALL)) {
                    botAction.setupObjectModification(lvzImageObject); //setup mod for all players.
                    botAction.setupObject(lvzImageObject.getObjectID(), true); //setup visible for all players.
                } else if(playerName.equals(Constants.LVZ_SEND_TO_ALL_BUT_ONE)) { //Build a list of Lvz's here
                    	listOfLvzObjects.add(lvzImageObject);
                } else {
                    int playerId = botAction.getPlayerID(playerName);
		    if(playerId == -1) return;
                    botAction.setupObjectModification(playerId, lvzImageObject); //setup mod for single player.
                    botAction.setupObject(playerId, lvzImageObject.getObjectID(), true); //setup visible for single player.
                }
            } else {
                botAction.sendArenaMessage("[LvzManager Error]: Couldn't use drawImage(), most likely ran out object ids");
                return;
            }
        }

        //Send packages
        if(playerName.equals(Constants.LVZ_SEND_TO_ALL)) {
            botAction.sendSetupObjectModifications(); //setup mod for all.
            botAction.sendSetupObjects(); //setup visible for all.
        } else if(playerName.equals(Constants.LVZ_SEND_TO_ALL_BUT_ONE)) { //send it to all but ignore one player.
                //This will do until they add more commands in BotAction.
        	if(listOfLvzObjects.size() == 0) return;
        	
        	List<Integer> listOfPlayerIds = new ArrayList<Integer>();
        
                Map<Integer, Player> m_playerMap = botAction.getPlayerMap();
	        Iterator<Map.Entry<Integer, Player>> iter = m_playerMap.entrySet().iterator();

	        synchronized(m_playerMap) {
	            while (iter.hasNext())
	            {
	            	Map.Entry<Integer, Player> entry = iter.next();
	            	int playerId = entry.getKey();
           		Player player = entry.getValue();
			
			if(player == null || playerId < 0) continue;
			
	            	//If Player doesn't match ignorePlayerName.
	                if(!ignorePlayerName.equals(player.getPlayerName().toLowerCase()))
	                	listOfPlayerIds.add(playerId);
	            }
	         }
	         
	         if(listOfPlayerIds.size() == 0) return;

	         for(int playerId : listOfPlayerIds) {
		         for(LvzObject lvzObject : listOfLvzObjects) {
		         	if(lvzObject == null) continue;
		         	botAction.modifyObjectForPlayer(playerId, lvzObject);
		         	botAction.showObjectForPlayer(playerId, lvzObject.getObjectID());
		         }
	         }
        } else {
            int playerId = botAction.getPlayerID(playerName);
	    if(playerId == -1) return;
            botAction.sendSetupObjectModificationsForPlayer(playerId); //setup mod for single player.
            botAction.sendSetupObjectsForPlayer(playerId); //setup visible for single player.
        }
    }
    
    public void replaceShownImageToPlayer(String playerName, int id, int newImageIndex) {
        //Only when you have to draw one screen image for that unique Id.
   	//TODO: check for indirect access to -1 or -2 return exception.
        replaceShownImage(id, playerName, "", newImageIndex);
    }
    
    public void replaceShownImageToAll(int id, int newImageIndex) {
        //Only when you have to draw one screen image for that unique Id.
        replaceShownImage(id, Constants.LVZ_SEND_TO_ALL, "", newImageIndex);
    }

    public void replaceShownImageToAllButIgnorePlayer(String ignorePlayerName, int id, int newImageIndex) {
        //Only when you have to draw one screen image for that unique Id.
        replaceShownImage(id, Constants.LVZ_SEND_TO_ALL_BUT_ONE, ignorePlayerName, newImageIndex);
    }
    
    private void replaceShownImage(int uniqueId, String playerName, String ignorePlayerName, int newImageIndex) {
	//This only used to replace any kind of image, with a new image.
	//This works with both [Screen images and Map images].
	//Make sure unique Id's only have one image attached to them, or single player, single image.
	
	if(allLvzObjectsByUniqueID.get(uniqueId) == null) return;
	
	playerName = playerName.toLowerCase();
	List<LvzObject> listOfLvzObjects = new ArrayList<LvzObject>();
	
	for(LvzObjectPackage lvzObjectPackage : allLvzObjectsByUniqueID.get(uniqueId)) {
		if(lvzObjectPackage == null || lvzObjectPackage.getPlayerName() == null) continue;
		
		if(lvzObjectPackage.getPlayerName().equals(playerName)) {
			if(lvzObjectPackage.getPlayerName().equals(Constants.LVZ_SEND_TO_ALL_BUT_ONE) &&
			lvzObjectPackage.getIgnoredPlayerName() != null &&
			!lvzObjectPackage.getIgnoredPlayerName().equals(ignorePlayerName)) continue;
			
			LvzObject lvzObject = lvzObjectPackage.getLvzObject();
			if(lvzObject== null) continue;
			
			lvzObject.setImageID(newImageIndex);
			lvzObject.updateImage(true);

			if(playerName.equals(Constants.LVZ_SEND_TO_ALL)) {
	                    botAction.setupObjectModification(lvzObject); //setup mod for all players.
	                    botAction.setupObject(lvzObject.getObjectID(), true); //setup visible for all players.
	                } else if(playerName.equals(Constants.LVZ_SEND_TO_ALL_BUT_ONE)) { //Build a list of Lvz's here
	                    	listOfLvzObjects.add(lvzObject);
			} else {
	                    int playerId = botAction.getPlayerID(playerName);
		            if(playerId == -1) continue;
	                    botAction.setupObjectModification(playerId, lvzObject); //setup mod for single player.
	                    botAction.setupObject(playerId, lvzObject.getObjectID(), true); //setup visible for single player.
			}
		}
	}
	
        //Send packages
        if(playerName.equals(Constants.LVZ_SEND_TO_ALL)) {
            botAction.sendSetupObjectModifications(); //setup mod for all.
            botAction.sendSetupObjects(); //setup visible for all.
        } else if(playerName.equals(Constants.LVZ_SEND_TO_ALL_BUT_ONE)) { //send it to all but ignore one player.
                //This will do until they add more commands in BotAction.
        	if(listOfLvzObjects.size() == 0) return;
        	
        	List<Integer> listOfPlayerIds = new ArrayList<Integer>();
        
                Map<Integer, Player> m_playerMap = botAction.getPlayerMap();
	        Iterator<Map.Entry<Integer, Player>> iter = m_playerMap.entrySet().iterator();

	        synchronized(m_playerMap) {
	            while (iter.hasNext())
	            {
	            	Map.Entry<Integer, Player> entry = iter.next();
	            	int playerId = entry.getKey();
           		Player player = entry.getValue();
			
			if(player == null || playerId < 0) continue;
			
	            	//If Player doesn't match ignorePlayerName.
	                if(!ignorePlayerName.equals(player.getPlayerName().toLowerCase()))
	                	listOfPlayerIds.add(playerId);
	            }
	         }
	         
	         if(listOfPlayerIds.size() == 0) return;

	         for(int playerId : listOfPlayerIds) {
		         for(LvzObject lvzObject : listOfLvzObjects) {
		         	if(lvzObject == null) continue;
		         	botAction.modifyObjectForPlayer(playerId, lvzObject);
		         	botAction.showObjectForPlayer(playerId, lvzObject.getObjectID());
		         }
	         }
        } else {
            int playerId = botAction.getPlayerID(playerName);
	    if(playerId == -1) return;
            botAction.sendSetupObjectModificationsForPlayer(playerId); //setup mod for single player.
            botAction.sendSetupObjectsForPlayer(playerId); //setup visible for single player.
        }
    }
}