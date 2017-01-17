package twcore.bots.pokerbot;

import java.util.Random;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

import twcore.core.BotAction;
import twcore.core.game.Player;
import twcore.core.util.Point;
import twcore.core.util.Tools;
import twcore.core.BotAction;

import static twcore.bots.pokerbot.util.PokerUtils.isCoordsInReach;
import static twcore.bots.pokerbot.util.PokerUtils.formatMoney;

/**
 * ZombieMiniGame is what controls a variant of zombies minigame.
 * when 2 winners are declared in a poker game.
 */
public class ZombieMiniGame {
    private BotAction m_BotAction;
    private pokerbot m_pokerbot;
    private Table[] pokerTables;
    private String[] humans;
    private int[] humansOutOrder;
    private List<String> killers;
    private int humanOutCounter;
    private int totalPotSize; //winnable pot size
    public static Map<String, Integer> zombies;
    private int minigameState;
    private long zombieStartLastTime;
    private int alertState;
    private int NOT_OUT = 0;
    private int ZOMBIE_NOT_SITTED = -1;
    private int ZOMBIE_FREQ = 1;
    private int HUMAN_FREQ = 0;
    private int totalHumanWinnings;
    private int totalZombieWinnings;
     		
     public ZombieMiniGame(BotAction m_BotAction, pokerbot m_pokerbot) {
     	this.m_BotAction = m_BotAction;
     	this.m_pokerbot = m_pokerbot;
     	killers = new ArrayList<String>();
     	minigameState = -1;
     }
     
     public void startMiniGame(Table[] pokerTables, String[] humans, int totalPotSize) {
     	this.pokerTables = pokerTables;
     	this.humans = humans;
     	humansOutOrder = new int[humans.length];
     	for(int i = 0; i < humansOutOrder.length; i++) {
     		humansOutOrder[i] = NOT_OUT;
     	}
     	humanOutCounter = 0;
     	this.totalPotSize = totalPotSize;
     	zombies = new ConcurrentHashMap<String, Integer>();	
     	minigameState = 0;
     	alertState = 0;
     	totalHumanWinnings = (int)(totalPotSize*(Constants.MINIGAME_HUMAN_WINNINGS_PERCENTAGE/100.0f));
        totalZombieWinnings = totalPotSize - totalHumanWinnings;
     }
     
     public int getMiniGameState() {
     	return minigameState;
     }
     
     public void processMiniGame() {
            	switch(minigameState) {
            	case 0: //get list of zombies.
	                Table pokerTable;
	                int zombieIndex = 0;
	                zombieStartLastTime = System.currentTimeMillis();
	                for(int t = 0; t < pokerTables.length; t++) {
	                    if(pokerTables[t] == null) continue;
	
	                    pokerTable = pokerTables[t];
	
	                    //Close table if no players left on it.
	                    if(pokerTable.getPlayerCount() > 0) {
	                    	for(PokerPlayer pokerPlayer : pokerTable.getPlayers()) {
			            if(pokerPlayer == null) continue;
			            	if(isHuman(pokerPlayer.getName())) continue;
			                int playerSeat = pokerTable.getPlayerIndex(pokerPlayer.getName());
			                if(playerSeat == -1) continue;
			                zombies.put(pokerPlayer.getName(), ZOMBIE_NOT_SITTED);
				}
	                    }
	                }
	                minigameState = 1;
            		break;
            	case 1: //warp humans first
            		warpHumans();
            		String humansDetected = "";
            		for(int t = 0; t < humans.length; t++) {
		            humansDetected += humans[t];
		            if((t+1) != humans.length)
		            	humansDetected += ", ";
		        }
            		m_BotAction.sendArenaMessage("Detected multiple winners! [" + humansDetected + "] will now try to survive an attack", Tools.Sound.HALLELUJAH);
            		m_BotAction.sendArenaMessage("All players who were playing poker will now be forced to play a Zombies minigame!" );
            		m_BotAction.sendArenaMessage("Total Winnings: " + formatMoney(totalPotSize) + " last surviving human will get " + formatMoney(totalHumanWinnings) + " zombie killer(s) will get " + formatMoney(totalZombieWinnings), Tools.Sound.BEEP1);

            		minigameState = 2;
            	case 2: //wait 10 seconds until warping the zombies and starting game
            	
            		//After 5 seconds the first warning alert is sent
            		if( alertState == 0 && (System.currentTimeMillis() - zombieStartLastTime) >= Tools.TimeInMillis.SECOND * 5) {
            			m_BotAction.sendArenaMessage("Zombies minigame will begin in 10 seconds!", TextToSpeech.SOUNDS.DRUM_ZOMBIE_LOOP_MUSIC.id);
            			makeHumansMove();
            			alertState = 1;
            		} else if(alertState == 1 && (System.currentTimeMillis() - zombieStartLastTime) >= Tools.TimeInMillis.SECOND * 15) {
            			m_BotAction.sendArenaMessage("GO GO GO!", Tools.Sound.GOGOGO);
            			warpZombies();
            			minigameState = 3;
            		}
            		break;
            	case 3: //check if game is over
            		int humansStillPlaying = humans.length;
            		for(int t = 0; t < humans.length; t++) {
    			        if(humans[t] == null) continue;
            			Player player = m_BotAction.getPlayer(humans[t]);
	            		if(player == null) continue;

		            	 if(!isCoordsInReach(player.getXTileLocation(), player.getYTileLocation(), Constants.miniGameHumanWarpPoint.x,  Constants.miniGameHumanWarpPoint.y, Constants.HumanRoamRadius))
		            		humansStillPlaying--;
		            		if(humansOutOrder[t] == NOT_OUT)
		            			humansOutOrder[t] = ++humanOutCounter;
	            		}
	            		
				//declares a winner
	            		if(humansStillPlaying <= 1) {
	            		        m_BotAction.sendArenaMessage("Game is over!", Tools.Sound.HALLELUJAH);
	            		        
				     	   /* Bubble Sort */
					    for (int n = 0; n < humansOutOrder.length; n++) {
					        for (int m = 0; m < (humansOutOrder.length - 1) - n; m++) {
					            if (humansOutOrder[m] > humansOutOrder[m + 1]) {
					                int outOrderValue = humansOutOrder[m];
					                humansOutOrder[m] = humansOutOrder[m + 1];
					                humansOutOrder[m + 1] = outOrderValue;
					                String playerName = humans[m];
					                humans[m] = humans[m + 1];
					                humans[m + 1] = playerName;
					            }
					        }
					    }
	            		        
	            		        String winners = "";
	            		        for(int t = 0; t < (humans.length-1); t++) {
				            winners += humans[t];
				            if((t+1) != (humans.length-1))
				            	winners += ", ";
				     	}
				     	
	            		        String zombies = "";
	            		        for(int t = 0; t < killers.size(); t++) {
				            zombies += killers.get(t);
				            if((t+1) != killers.size())
				            	zombies += ", ";
				     	}
				     	if(killers.size() == 0)
				     		zombies = "Nobody";
				     	
				     	int humanSplit = (int)(totalHumanWinnings / (humans.length > 1 ? (humans.length-1) : 1));
				     	int zombieSplit = (int)(totalZombieWinnings / (killers.size() == 0 ? 1 : killers.size()));

	            		        m_BotAction.sendArenaMessage("Our winning human" + (((humans.length-1) > 1) ? "s are " : " is ") + winners + " with a winning " +  (((humans.length-1) > 1) ? "split " : "sum ")  + "of " + formatMoney(humanSplit));
	            		        m_BotAction.sendArenaMessage("Our winning zombie" + ((killers.size() > 1) ? "s are " : " is ") + zombies + " with a winning "+  ((killers.size() > 1) ? "split " : "sum ")  + "of " + formatMoney(zombieSplit));         		        
	            			makeHumansNotMove();
	            			
	            			//Credit with PubBux the human(s)
	            			for(int t = 0; t < (humans.length-1); t++) {
	            				if(m_pokerbot != null && m_pokerbot.getPubBuxManager() != null) {
	      						m_pokerbot.getPubBuxManager().modifyPubBuxForPlayerName(humans[t], humanSplit);
	      					}
	            			}
	            			//Credit with PubBux the zombie(s)
	            			for(int t = 0; t < killers.size(); t++) {
	            				if(killers.get(t) == null) continue;
	            				if(m_pokerbot != null && m_pokerbot.getPubBuxManager() != null) {
	      						m_pokerbot.getPubBuxManager().modifyPubBuxForPlayerName(killers.get(t), zombieSplit);
	      					}
	            			}
	            			//makes everybody back into a warbird to play poker again
	            			m_BotAction.changeAllShips(Tools.Ship.WARBIRD);
	            			//This will avoid multiple tables starting a zombie event right after 1 was already played.
			                for(int t = 0; t < pokerTables.length; t++) {
			                   if(pokerTables[t] == null) continue;
					   pokerTables[t].resetMultipleWinners();
					}
					minigameState = -1; //lets another minigame of zombies be possible.
	            		}
            		break;
            	}
     }
          
     public void makeHumansMove() {
                 m_BotAction.sendUnfilteredPublicMessage("?set Warbird:InitialSpeed:2000");
                 m_BotAction.sendUnfilteredPublicMessage("?set Warbird:MaximumSpeed:6000");
                 m_BotAction.sendUnfilteredPublicMessage("?set Warbird:InitialThrust:16");
                 m_BotAction.sendUnfilteredPublicMessage("?set Warbird:MaximumThrust:24");
     }
     
     public void makeHumansNotMove() {
                 m_BotAction.sendUnfilteredPublicMessage("?set Warbird:InitialSpeed:0");
                 m_BotAction.sendUnfilteredPublicMessage("?set Warbird:MaximumSpeed:0");
                 m_BotAction.sendUnfilteredPublicMessage("?set Warbird:InitialThrust:0");
                 m_BotAction.sendUnfilteredPublicMessage("?set Warbird:MaximumThrust:0");
     }
     
     public boolean isHuman(String humanName) {
    		for(int t = 0; t < humans.length; t++) {
    			if(humans[t] == null) continue;
    			if(humans[t].equalsIgnoreCase(humanName))
    				return true;
    		}
    		return false;
     }
     
     public void warpHumans() {
    		for(int t = 0; t < humans.length; t++) {
    			if(humans[t] == null) continue;
			Player player = m_BotAction.getPlayer(humans[t]);
	            	if(player == null) continue;
	           	if(!player.isPlaying()) continue;
 			if(player.getShipType() != Tools.Ship.WARBIRD)
    	   	 		m_BotAction.setShip(humans[t], Tools.Ship.WARBIRD);
    	    		if(player.getFrequency() != HUMAN_FREQ)
    	    			m_BotAction.setFreq(humans[t], HUMAN_FREQ);
	                m_BotAction.warpTo(humans[t], Constants.miniGameHumanWarpPoint.x, Constants.miniGameHumanWarpPoint.y);
	        }
     }
     
     public void warpZombies() {
     		Random rng = new Random();
     		for(String playerName : zombies.keySet()) {
	            //If player has specced or left he won't get to play.
	            Player player = m_BotAction.getPlayer(playerName);
	            if(player == null) continue;
	            if(!player.isPlaying()) continue;
	            
	            checkZombiePosition(playerName);
		}
    }
    	 
     public void checkZombiePosition(String name) {
     		Player player = m_BotAction.getPlayer(name);
	    if(player == null) return;
	    if(!player.isPlaying()) return;
	    if(player.getShipType() != Tools.Ship.SPIDER)
    	   	 m_BotAction.setShip(name, Tools.Ship.SPIDER);
    	    if(player.getFrequency() != ZOMBIE_FREQ)
    	    	m_BotAction.setFreq(name, ZOMBIE_FREQ);
    	    Integer zombieSeat = zombies.get(name);
    	    if(zombieSeat != null) {
    	    	if(zombieSeat == ZOMBIE_NOT_SITTED) {
    	    		zombieSeat = new Random().nextInt(Constants.miniGameZombieWarpPoints.length);
    	    		zombies.put(name, zombieSeat);
    	    	}
    	    	Point zombieCoords = Constants.miniGameZombieWarpPoints[zombieSeat];
    	    	//If player isn't in his seat on this table, it will warp them to it.
	        if(!isCoordsInReach(player.getXTileLocation(), player.getYTileLocation(), zombieCoords.x, zombieCoords.y, 1))
	            m_BotAction.warpTo(name, zombieCoords.x, zombieCoords.y);
    	    }
     }
     
     public void addNewKiller(String killer) {
     	killers.add(killer);
     }
}
