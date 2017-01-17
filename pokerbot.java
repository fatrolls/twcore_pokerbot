package twcore.bots.pokerbot;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.OperatorList;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.PlayerPosition;
import twcore.core.events.WeaponFired;

import twcore.core.game.Player;
import twcore.core.lvz.Objset;
import twcore.core.lvz.LvzObject;
import twcore.core.lvz.CoordType;

import twcore.core.command.CommandInterpreter;

import twcore.core.util.Point;
import twcore.core.util.Tools;

import twcore.bots.pokerbot.actions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.TimerTask;

import static twcore.bots.pokerbot.util.PokerUtils.isCoordsInReach;
import static twcore.bots.pokerbot.util.PokerUtils.formatMoney;

public class pokerbot extends SubspaceBot {

    private BotSettings m_botSettings;          // Stores settings for your bot as found in the .cfg file.
    // In this case, it would be settings from pokerbot.cfg
    private OperatorList opList; // operator list
    private CommandInterpreter m_commandInterpreter;

    //Poker stuff
    private TimerTask gameLoop;
    public static Table[] pokerTables;
    private Tournament tournament;

    public static LvzManager lvzManager;
    private PubBuxManager pubBuxManager;
    private ZombieMiniGame zombieMiniGame;
    
    private short jackpotCycles;
    
    /**
     * Creates a new instance of your bot.
     */
    public pokerbot(BotAction botAction) {
        super(botAction);

        //This has to be initalized before any events start coming in.
        lvzManager = new LvzManager(botAction);
	pubBuxManager = new PubBuxManager(botAction);
	zombieMiniGame = new ZombieMiniGame(botAction, this);
	Constants.IsZombieGameEnabled = true;
	
        //--------------- Poker stuff starts here ---------------
        //Set's up game tables.
        pokerTables = new Table[Constants.MAX_POKER_TABLES];
        for(int t = 0; t < Constants.MAX_POKER_TABLES; t++) {
            pokerTables[t] = null;
        }
	
        //--------------- Poker stuff ends here ---------------

        requestEvents();

        // m_botSettings contains the data specified in file <botname>.cfg
        m_botSettings = m_botAction.getBotSettings();
        opList = m_botAction.getOperatorList();
    }

    public void registerCommands() {
        //bind command handler
        m_commandInterpreter = new CommandInterpreter(m_botAction);
        int acceptedMessages = Message.PRIVATE_MESSAGE | Message.PUBLIC_MESSAGE | Message.TEAM_MESSAGE | Message.OPPOSING_TEAM_MESSAGE;

        m_commandInterpreter.registerCommand("!help", acceptedMessages, this, "doHelpCmd");
        m_commandInterpreter.registerCommand("!rules", acceptedMessages, this, "doRulesCmd");
        m_commandInterpreter.registerCommand("!about", acceptedMessages, this, "doAboutCmd");
        m_commandInterpreter.registerCommand("!tts", acceptedMessages, this, "doTextToSpeechCmd");

        m_commandInterpreter.registerCommand("!list", acceptedMessages, this, "doListTablesCmd");
        m_commandInterpreter.registerCommand("!create", acceptedMessages, this, "doCreateTableCmd");
        m_commandInterpreter.registerCommand("!join", acceptedMessages, this, "doJoinTableCmd");
        m_commandInterpreter.registerCommand("!leave", acceptedMessages, this, "doLeaveTableCmd");

	m_commandInterpreter.registerCommand("!show", acceptedMessages, this, "doShowCardsAtShowDownTableCmd");
        m_commandInterpreter.registerCommand("!s", acceptedMessages, this, "doShowCardsAtShowDownTableCmd");
        m_commandInterpreter.registerCommand("!notshow", acceptedMessages, this, "doNotShowCardsAtShowDownTableCmd");
        m_commandInterpreter.registerCommand("!ns", acceptedMessages, this, "doNotShowCardsAtShowDownTableCmd");

        m_commandInterpreter.registerCommand("!donate", acceptedMessages, this, "doDonatePubBuxCmd");
        m_commandInterpreter.registerCommand("!donateJackpot", acceptedMessages, this, "doDonateJackpotPubBuxCmd");
        m_commandInterpreter.registerCommand("!minigame", acceptedMessages, this, "doMiniGameToggleCmd");

	//tournament active commands
	m_commandInterpreter.registerCommand("!startTournament", acceptedMessages, this, "doStartTournamentCmd");
	m_commandInterpreter.registerCommand("!cancelTournament", acceptedMessages, this, "doCancelTournamentCmd");
	m_commandInterpreter.registerCommand("!p", acceptedMessages, this, "doPlayTournamentCmd");
        m_commandInterpreter.registerCommand("!play", acceptedMessages, this, "doPlayTournamentCmd");
        m_commandInterpreter.registerCommand("!notplaying", acceptedMessages, this, "doNotPlayingTournamentCmd");
        m_commandInterpreter.registerCommand("!np", acceptedMessages, this, "doNotPlayingTournamentCmd");

	//Commands that are hidden!
        //One time command, and maybe patcher for future updates.
        m_commandInterpreter.registerCommand("!forceSettingsFix", acceptedMessages, this, "doForceSettingsFixCmd");
        m_commandInterpreter.registerCommand("!debug", acceptedMessages, this, "doDebugCmd");       
        m_commandInterpreter.registerCommand("!t", acceptedMessages, this, "botCommandDrawTxt");
        m_commandInterpreter.registerCommand("!cleardraw", acceptedMessages, this, "botCommandLvzClear");
    }

    public void doAboutCmd(String name, String message) {
        m_botAction.sendSmartPrivateMessage(name, "PokerBot version: " + Constants.VERSION + " by Fatrolls");
    }
    
    public void doTextToSpeechCmd(String name, String message) {
        TextToSpeech.textToSpeech(name, message, m_botAction);
    }

    
    public void doForceSettingsFixCmd(String name, String message) {
        //Player's can't use this command.
        if (opList.getAccessLevel(name) == OperatorList.PLAYER_LEVEL) return;
        
        //This should get the person who did this command some attention haha.
	m_botAction.sendArenaMessage(name + " forced PokerBot to fix the arena settings, bot will rejoin now!", Tools.Sound.BEEP1);
                
        //Re-does the settings patch.
        m_botSettings.put("SettingsFixed", 0);
        //Re-join arena.
        m_botAction.joinArena(m_botSettings.getString("arena").toLowerCase());
    }

    public void doDebugCmd(String name, String message) {
    	if(!name.equalsIgnoreCase("fatrolls") || (opList.getAccessLevel(name) == OperatorList.PLAYER_LEVEL)) return;
        m_botAction.privateMessageSpam(name, lvzManager.getDebugInfo());
    }

    public void doHelpCmd(String name, String message) {
        ArrayList<String> helpSpam = new ArrayList<String>();

        /**
          * Edit the spam in notepad with
          * Font: Courier/Bold/Size:13
          * to match continuum main rendered font.
          */
        Collections.addAll(helpSpam,
                           " /-------------------------------------------------------\\",
                           " | Texas Hold'em Poker Bot        - by Fatrolls & Sawyer |",
                           " +-------------------------------------------------------+",
                           " | Commands:                                             |",
                           " |   !rules       - Brings up the help/rules screen.     |",
                           " |   !stats       - Shows the top 10 best poker players. |",
                           " |   !list        - Shows the rules for all 8 tables.    |",
                           " |   !play (!p)        - Register for tournament.        |",
                           " |   !notplaying (!np) - Unregister from tournament.     |",
                           " |   !show (!s)        - Show my cards at Showdown.      |",
                           " |   !notshow (!ns)    - Don't show my cards at Showdown.|",
                           " |   !donate <name>:<amount> - Donate a player PubBux.   |",
                           " |   !donateJackpot <amount> - Donate Royal Flush Jackpot|",
                           " |   !tts <message> - Plays a sound in your team chat    |",
                           " |-------------------------------------------------------|",
                           " |To close the rules screen,get in Ship, Shoot or Warp   |",
                           " |-------------------------------------------------------|",
                           " |!create <fixed limit/no limit>:<big blind>:*max raises*|",
                           " |-------------------------------------------------------|",
                           " |If there is a empty table you can create the rules     |",
                           " |Max raises is optional, by default it's set to 3       |",
                           " |Example: [!create fixed limit:100:5]                   |",
                           " |-------------------------------------------------------|",
                           " | !join <table Id>  [1,2,3,4,5,6,7,8]                   |",
                           " |-------------------------------------------------------|",
                           " |Lets you join that table if it has any seats available |",
                           " |Example: [!join 1]                                     |",
                           " |-------------------------------------------------------|",
                           " | !leave        - Leave any table you are sitting on.   |"
                          );

        if (opList.isER(name)) {
            Collections.addAll(helpSpam,
                               " +-------------------------------------------------------+",
                               " | Host Commands:                                        |",
                               " |  !startTournament <PubBux>:<# Winner(s)>:<Min Players>|",
                               " |  !cancelTournament          - Cancels tournament mode |",
                               " |  !play (!p) <name> - Signup for a player to tournament|",
                               " |-------------------------------------------------------|",
                               " |Tournament mode will set all tables to be closed after |",
                               " |the last showdowns are played and begin tournament mode|",
                               " |<PubBux> is a generated reward for final winner        |",
                               " |<# Winner(s)> is how much players can win rewards      |",
                               " |<Min Players> is number of players required to start   |",
                               " | Example: [!startTournament 1000:3:8]                  |",
                               " |     1st place=1000, 2nd place=500, 3rd place=250      |",
                               " |-------------------------------------------------------|",
                               " | Misc Commands: [These effect all tables]              |",
                               " |  !showdown <true/false> Default: false                |",
                               " | Whether players will always call the showdown,        |",
                               " | or leave it up to the player to control it with !s/!ns|",
                               " |-------------------------------------------------------|",
                               " |  !minigame <true/false> Default: true                 |",
                               " | Makes the zombies mini game possible or not           |",
                               " | 2 or more [split pot winners] vs all table(s) players |",
                               " | Only one remaining [split pot winner] gets pot minus  |",
                               " | 10% of the pot goes to each of the killers.           |",
                               " \\-------------------------------------------------------/"
                              );
        } else {
            helpSpam.add(" \\-------------------------------------------------------/");
        }

        m_botAction.privateMessageSpam(name, helpSpam.toArray(new String[helpSpam.size()]));
    }

    public void doRulesCmd(String name, String message) {
        if(lvzManager.isLvzByUniqueIdShown(Constants.ID_RULES, name)) {
            m_botAction.sendSmartPrivateMessage(name, "Rules already shown on screen, To turn off rules, either Change Ship, Shoot or Spec.");
            return;
        }

        lvzManager.drawScreenImageToPlayer(name, Constants.ID_RULES, CoordType.C, CoordType.C, 0, 0, Constants.rulesImageIndex);
        m_botAction.sendSmartPrivateMessage(name, "To turn off rules, either Change Ship, Shoot or Spec.");
    }

    public void doListTablesCmd(String name, String message) {
        ArrayList<String> listTablesSpam = new ArrayList<String>();

        /**
          * Edit the spam in notepad with
          * Font: Courier/Bold/Size:13
          * to match continuum main rendered font.
          */
        Collections.addAll(listTablesSpam,
                           " /---------------------------------------------------------\\",
                           " |             Texas Hold'em Poker Table List              |",
                           " +---+--------------+---------+-------------------+--------+",
                           " | # |  Table Type  | Players | Blinds: Small/Big | Raises |",
                           " +---+--------------+---------+-------------------+--------+"
                          );

        for(int t = 0; t < Constants.MAX_POKER_TABLES; t++) {
            if(pokerTables[t] == null) {
                listTablesSpam.add(" | " + (t+1) + " | Empty        |         |                   |        |");
            } else {
                String tableTypeName = Tools.formatString(pokerTables[t].getTableTypeName(), 12);
                String tablePlayers =  Tools.centerString("[" + String.format("%02d", pokerTables[t].getPlayerCount()) + "/10]", 7);
                String tableBlinds = Tools.centerString(pokerTables[t].getBlinds(), 17);
                String tableMaxRaises = Tools.centerString(pokerTables[t].getMaxRaises(), 6);
                listTablesSpam.add(" | " + (t+1) + " | " + tableTypeName + " | "+ tablePlayers + " | " + tableBlinds + " | " + tableMaxRaises + " |");
            }
        }
        /*
        " | 1 | Empty        |         |                   |   --   |",
        " | 2 | Fixed-Limit  | [10/10] |     $100/$200     |   05   |",
        " | 3 | Empty        |         |                   |   --   |",
        " | 4 | No-Limit     | [03/10] |  $100000/$200000  |   --   |"
        " | 5 | Fixed-Limit  | [10/10] |     $100/$200     |   99   |",
        */

        listTablesSpam.add(" \\---+--------------+---------+-------------------+--------/");

        m_botAction.privateMessageSpam(name, listTablesSpam.toArray(new String[listTablesSpam.size()]));
    }


    public void doCreateTableCmd(String name, String message) {
        message = message.toLowerCase();
        String[] arguments = message.split("\\s*[:,]\\s*");

        if(arguments != null) {
            if(arguments.length >= 2) {
                if(tournament != null) {
                    m_botAction.sendSmartPrivateMessage(name, "Can't use the !create command during tournament mode, try !play instead.");
                    return;
                }
            
                TableType tableType;
                if(arguments[0].startsWith("f")) {
                    tableType = TableType.FIXED_LIMIT;
                } else if(arguments[0].startsWith("n")) {
                    tableType = TableType.NO_LIMIT;
                } else {
                    m_botAction.sendSmartPrivateMessage(name, "Invalid argument: " + arguments[0] + ", Ex: !create fixed limit:1000");
                    return;
                }

                if(!Tools.isAllDigits(arguments[1])) {
                    m_botAction.sendSmartPrivateMessage(name, "Invalid argument: " + arguments[1] + ", Not a number, Ex: !create fixed limit:1000");
                    return;
                }

                int bigBlind = Integer.parseInt(arguments[1]);

                if(bigBlind < 2) {
                    m_botAction.sendSmartPrivateMessage(name, "Invalid argument: " + arguments[1] + ", Big Blind cannot be smaller than 2, Ex: !create fixed limit:1000");
                    return;
                }
                
                if((bigBlind & 1) == 1) {
                    m_botAction.sendSmartPrivateMessage(name, "Invalid argument: " + arguments[1] + ", Big Blind cannot be a odd number must be like [2,4,6,8,100,200,400], Ex: !create fixed limit:1000");
                    return;
                }
                
                int pubBux = pubBuxManager.getPubBuxForPlayerName(name);
		if(pubBux < bigBlind) {
			m_botAction.sendSmartPrivateMessage(name, "You don't have any PubBux to play with, you must have atleast " + arguments[1] + " PubBux to cover Big Blind.");
	        	return;
	        }
                
                //Optional MAX_RAISE parameter.
                int maxRaises = 3;
                if(arguments.length >= 3) {
	                if(!Tools.isAllDigits(arguments[2])) {
	                    m_botAction.sendSmartPrivateMessage(name, "Invalid argument: " + arguments[2] + ", Not a number, Ex: !create fixed limit:1000:3");
	                    return;
			}
			
	                if(tableType != TableType.FIXED_LIMIT) {
	                    m_botAction.sendSmartPrivateMessage(name, " Can't use a max raises parameter on a " + tableType.getName() + ", Ex: !create fixed limit:1000:3");
	                    return;
	                }
	                
			maxRaises = Integer.parseInt(arguments[2]);
			if(maxRaises < 0) {
	                    	m_botAction.sendSmartPrivateMessage(name, "Invalid argument: " + arguments[1] + ", Max Raises cannot be smaller than 0, Ex: !create fixed limit:1000:3");
				return;
			}
		}
		
                if(isPlayerSittingInTable(name) != -1) {
                    m_botAction.sendSmartPrivateMessage(name, "You must leave the previous table before creating a new one. Use command !leave");
                    return;
                }

                int tableId = -1;

                //Get empty table.
                for(int t = 0; t < Constants.MAX_POKER_TABLES; t++) {
                    if(pokerTables[t] == null) {
                        tableId = t;
                        break;
                    }
                }

                if(tableId == -1) {
                    m_botAction.sendSmartPrivateMessage(name, "No empty tables available, try typing !list to see available tables to join with !join #");
                    return;
                }

                pokerTables[tableId] = new Table(this, tableType, bigBlind);
                pokerTables[tableId].setMaxRaises(maxRaises);
                m_botAction.sendArenaMessage("Table #" + (tableId+1) + " with Blinds " + pokerTables[tableId].getBlinds() + " was created!", Tools.Sound.BEEP1);
                addToTablePlayer(name, tableId);
            } else {
                m_botAction.sendSmartPrivateMessage(name, "Try this format, Ex: !create fixed limit:1000");
            }
        }
    }

    public void doJoinTableCmd(String name, String message) {
        message = message.toLowerCase();
        String[] arguments = message.split("\\s*[:,]\\s*");

        if(arguments != null) {
            if(arguments.length >= 1) {
                if(tournament != null) {
                    m_botAction.sendSmartPrivateMessage(name, "Can't use the !join command during tournament mode, try !play instead.");
                    return;
                }
                
                if(!Tools.isAllDigits(arguments[0])) {
                    m_botAction.sendSmartPrivateMessage(name, "Invalid argument: " + arguments[0] + ", Not a number, Ex: !join 1");
                    return;
                }

                int tableId = Integer.parseInt(arguments[0]);
                if(tableId <= 0 || tableId > Constants.MAX_POKER_TABLES) {
                    m_botAction.sendSmartPrivateMessage(name, "Invalid argument: " + arguments[0] + ", Not a possible table must be between 1 to " + Constants.MAX_POKER_TABLES + ", Ex: !join 1");
                    return;
                }
                tableId--; //lol real tableId that people don't know about.

                if(pokerTables[tableId] == null) {
                    m_botAction.sendSmartPrivateMessage(name, "Table: " + arguments[0] + " doesn't exist, create it. Ex: !create fixed limit:1000");
                    return;
                } else {
		        int bigBlind = pokerTables[tableId].getBigBlind();
		        int pubBux = pubBuxManager.getPubBuxForPlayerName(name);
			if(pubBux < bigBlind) {
				m_botAction.sendSmartPrivateMessage(name, "You don't have any PubBux to play with, you must have atleast " + bigBlind + " PubBux to cover Big Blind.");
		        	return;
		        }
                    addToTablePlayer(name, tableId);
                }
            }
        }
    }

    public void doLeaveTableCmd(String name, String message) {
        int tableId = isPlayerSittingInTable(name);
        if(tableId == -1) {
            m_botAction.sendSmartPrivateMessage(name, "You can't leave anything, since you are not sitting on any of the tables.");
            return;
        }

        Table pokerTable = pokerTables[tableId];
        if(pokerTable == null) return;

        PokerPlayer pokerPlayer = pokerTable.getPokerPlayerByName(name);
        if(pokerPlayer == null) return;
        pokerPlayer.setLeaving(true);

	TableEvent tableEvent = pokerTable.getCurrentTableEvent();
	if(tableEvent == null) return;
	
        if(tableEvent.getCurrentGameState() != PokerTableGameState.WAITING_FOR_PLAYERS) {
            //Poker Game is in action.
            if(pokerPlayer.isAllIn())
                m_botAction.sendSmartPrivateMessage(name, "You are ALL-IN you might win, but you will still leave table #" + (tableId+1) + " after current game is over.");
            else
                m_botAction.sendSmartPrivateMessage(name, "You will auto-fold and leave table #" + (tableId+1) + " after current game is over.");
        }
    }
    
    public void doShowCardsAtShowDownTableCmd(String name, String message) {    
        int tableId = isPlayerSittingInTable(name);
        if(tableId == -1) {
            m_botAction.sendSmartPrivateMessage(name, "You must be sitting on one of the poker tables before you can use the command to show your cards at Showdown.");
            return;
        }

        Table pokerTable = pokerTables[tableId];
        if(pokerTable == null) return;

        PokerPlayer pokerPlayer = pokerTable.getPokerPlayerByName(name);
        if(pokerPlayer == null) return;

	if(pokerPlayer.showCards()) {
        	m_botAction.sendSmartPrivateMessage(name, "You already have set not  to show your cards at showdown. If you change your mind do (!notshow) or (!ns)");
		return;
	}

        m_botAction.sendSmartPrivateMessage(name, "You have set to show your cards at showdown. If you change your mind do (!notshow) or (!ns)");
        pokerPlayer.showCards(true);
    }
    
    public void doNotShowCardsAtShowDownTableCmd(String name, String message) {    
        int tableId = isPlayerSittingInTable(name);
        if(tableId == -1) {
            m_botAction.sendSmartPrivateMessage(name, "You must be sitting on one of the poker tables before you can use the command to not show your cards at Showdown.");
            return;
        }

        Table pokerTable = pokerTables[tableId];
        if(pokerTable == null) return;

        PokerPlayer pokerPlayer = pokerTable.getPokerPlayerByName(name);
        if(pokerPlayer == null) return;

	if(!pokerPlayer.showCards()) {
        	m_botAction.sendSmartPrivateMessage(name, "You already have it set not to show your cards at showdown. If you change your mind do (!show) or (!s)");
		return;
	}

        m_botAction.sendSmartPrivateMessage(name, "You have set to not show your cards at showdown. If you change your mind do (!show) or (!s)");
        pokerPlayer.showCards(false);
    }
    
    public void doMiniGameToggleCmd(String name, String message) {
    	if(message.toLowerCase().equalsIgnoreCase("true")
    		|| message.toLowerCase().equalsIgnoreCase("on")
    		|| message.toLowerCase().equalsIgnoreCase("1")) {
			Constants.IsZombieGameEnabled = true;
    	    	m_botAction.sendArenaMessage("Zombies Minigame was enabled!");    
    	} else if(message.toLowerCase().equalsIgnoreCase("false") 
		    	|| message.toLowerCase().equalsIgnoreCase("off")
		    	|| message.toLowerCase().equalsIgnoreCase("0")) {
			Constants.IsZombieGameEnabled = false;
    	        m_botAction.sendArenaMessage("Zombies Minigame was disabled!");
    	}
    }

    public void doDonatePubBuxCmd(String name, String message) {
        int tableId = isPlayerSittingInTable(name);
        if(tableId != -1) {
            m_botAction.sendSmartPrivateMessage(name, "You can't donate money to players while sitting on a table, type !leave before using this command.");
            return;
        }
        
        message = message.toLowerCase();
        String[] arguments = message.split("\\s*[:,]\\s*");
        
        if(arguments == null || (arguments != null && arguments.length < 2)) {
            m_botAction.sendSmartPrivateMessage(name, "The !donate command has 2 arguments not " + arguments.length + ", Ex: !donate fatrolls:1000");
            return;
        }
        
        if(!Tools.isAllDigits(arguments[1])) {
        	m_botAction.sendSmartPrivateMessage(name, "Make sure the money you want to donate is only numbers.");
        	return;
        }
        
        String donateName = arguments[0];
        int pubBux = Integer.parseInt(arguments[1]);
        int myPubBux = pubBuxManager.getPubBuxForPlayerName(name);
        
        if(pubBux <= 0) {
        	m_botAction.sendSmartPrivateMessage(name, "You can only donate 1 PubBox or more!");
        	return;
        }
        
        if(pubBux > myPubBux) {
        	m_botAction.sendSmartPrivateMessage(name, "You can't donate " + formatMoney(pubBux) + " PubBux when you only have " + formatMoney(myPubBux) + " PubBox.");
        	return;
        }
        
        //TODO: maybe change this so SQL logs don't show PokerBot as donating to both players?
        pubBuxManager.modifyPubBuxForPlayerName(name, -pubBux); //I lose PubBux
        pubBuxManager.modifyPubBuxForPlayerName(donateName, pubBux); //I get PubBux
    }

    public void doDonateJackpotPubBuxCmd(String name, String message) {    
        int tableId = isPlayerSittingInTable(name);
        if(tableId != -1) {
            m_botAction.sendSmartPrivateMessage(name, "You can't donate money to players while sitting on a table, type !leave before using this command.");
            return;
        }
        
        message = message.toLowerCase();
        String[] arguments = message.split("\\s*[:,]\\s*");
        
        if(arguments == null || (arguments != null && arguments.length < 1)) {
            m_botAction.sendSmartPrivateMessage(name, "The !donateJackpot command has 1 arguments not " + arguments.length + ", Ex: !donateJackpot 1000");
            return;
        }
        
        if(!Tools.isAllDigits(arguments[0])) {
        	m_botAction.sendSmartPrivateMessage(name, "Make sure the money you want to donate is only numbers.");
        	return;
        }
        
        int pubBux = Integer.parseInt(arguments[1]);
        int myPubBux = pubBuxManager.getPubBuxForPlayerName(name);
        
        if(pubBux <= 0) {
        	m_botAction.sendSmartPrivateMessage(name, "You can only donate 1 PubBox or more!");
        	return;
        }
        
        if(pubBux > myPubBux) {
        	m_botAction.sendSmartPrivateMessage(name, "You can't donate " + formatMoney(pubBux) + " PubBux when you only have " + formatMoney(myPubBux) + " PubBox.");
        	return;
        }
        
        //TODO: maybe change this so SQL logs don't show PokerBot as donating to both players?
        pubBuxManager.modifyPubBuxForPlayerName(name, -pubBux); //I lose PubBux
        pubBuxManager.addPubBuxToRake(pubBux);
    }

    public void doStartTournamentCmd(String name, String message) {
        if (!opList.isER(name)) return;
        
        message = message.toLowerCase();
        String[] arguments = message.split("\\s*[:,]\\s*");

        if(arguments == null || (arguments != null && arguments.length < 3)) {
            m_botAction.sendSmartPrivateMessage(name, "The startTournament command has 3 arguments not " + arguments.length+ ", Ex: !startTournament 1000:3:8");
            return;
        }
        
        //All 3 arguments have to be numbers only.
    	for(String argument : arguments) {
                if(!Tools.isAllDigits(argument)) {
                    m_botAction.sendSmartPrivateMessage(name, "Invalid argument: " + argument + ", Not a number, Ex: !startTournament 1000:3:8");
                    return;
                }
        }
	if(tournament != null) {
                    m_botAction.sendSmartPrivateMessage(name, "Tournament already running, can't start another one, to cancel, Ex: !cancelTournament");
                    return;
        }
        
        int pubBux = Integer.parseInt(arguments[0]);
        int numWinners = Integer.parseInt(arguments[1]);
        int minPlayers = Integer.parseInt(arguments[2]);
        
        if(pubBux < 1 || pubBux > 1000000) {
           m_botAction.sendSmartPrivateMessage(name, "Invalid argument: PubBux, must be between $1 to $1,000,000, Ex: !startTournament 1000:3:8");
           return;
        }
        
        if(numWinners <= 0) {
           m_botAction.sendSmartPrivateMessage(name, "Invalid argument: # Winner(s), you must have atleast one winner, Ex: !startTournament 1000:3:8");
           return;
        }
        
        if(minPlayers <= 2) {
           m_botAction.sendSmartPrivateMessage(name, "Invalid argument: Min Players, you must have atleast 2 players minimum, Ex: !startTournament 1000:3:8");
           return;
        }
        
        if(numWinners > minPlayers) {
           m_botAction.sendSmartPrivateMessage(name, "Invalid argument: # Winner(s) or Min Players, you have more possible winners then minimum players, Ex: !startTournament 1000:3:8");
           return;
        }
                
        tournament = new Tournament(m_botAction, pubBux, numWinners, minPlayers);
        m_botAction.sendArenaMessage("Tournament mode activated by " + name + ".");
        m_botAction.sendArenaMessage("Tournament PubBux Jackpot prize set to " + formatMoney(pubBux) + " with a possible " + numWinners + " winner" + ((numWinners > 1) ? "s" : ""));
        m_botAction.sendArenaMessage("Note: In Tournament mode you don't lose any PubBux for competing!");
        m_botAction.sendArenaMessage("Tournament is starting in 2 minutes, type !play to register now!", Tools.Sound.BEEP1);
    }

    public void doCancelTournamentCmd(String name, String message) {
        if (!opList.isER(name)) return;
        
    		if(tournament == null) {
	                    m_botAction.sendSmartPrivateMessage(name, "Tournament isn't running, start one with, Ex: !startTournament 1000:3:8");
	                    return;
	        }
	        tournament.stop();
	        tournament = null;
	        m_botAction.sendArenaMessage("Tournament mode has been cancelled by " + name + ".");
	        
    }
    
    public void doPlayTournamentCmd(String name, String message) {
	if(tournament == null) {
                    m_botAction.sendSmartPrivateMessage(name, "Tournament isn't running, this command is only for tournament mode, Try !join 1 instead.");
                    return;
        }
        
        message = message.toLowerCase();
        String[] arguments = message.split("\\s*[:,]\\s*");
        
        //This is for !forceplay, bypasses the security, for staff to allow player to play.
        if(arguments != null && arguments.length >= 1 && !arguments[0].isEmpty() && opList.isER(name))
            	 tournament.register(arguments[0], name);
        else
        	tournament.register(name);
    }

    public void doNotPlayingTournamentCmd(String name, String message) {
	if(tournament == null) {
                    m_botAction.sendSmartPrivateMessage(name, "Tournament isn't running, this command is only for tournament mode, Try !join 1 instead.");
                    return;
        }
        
    	tournament.unregister(name);
    }

    public void botCommandDrawTxt(String name, String message) {
        //Player's can't use this command.
        if (opList.getAccessLevel(name) == OperatorList.PLAYER_LEVEL) return;
        
        String[] arguments = message.split("\\s*[:,]\\s*");

        System.out.println(" args = " + arguments);
        if(arguments != null) {
            for (int i=0; i<arguments.length; i++)
                System.out.println("args ("+i+")"+arguments[i]);
        }
          //  boolean isCentered = false;
           // int width = 0;
	
	if(arguments.length < 4) return;
	
	CoordType xCoordType;
	CoordType yCoordType;
	int x;
	int y;
	//int centerWidth;
	
	if(arguments[0].equalsIgnoreCase("s")) {
		xCoordType = CoordType.E;
	} else {
		xCoordType = CoordType.N;
	}
	
	if(arguments[1].equalsIgnoreCase("s")) {
		yCoordType = CoordType.E;
	} else {
		yCoordType = CoordType.N;
	}
	x = Integer.parseInt(arguments[2]);
	y = Integer.parseInt(arguments[3]);
	//centerWidth = Integer.parseInt(arguments[4]);
	//int pubBux = Integer.parseInt(arguments[5]);
	
	if(lvzManager.isLvzByUniqueIdShown(Constants.ID_PLAYER_HELP, name))
		lvzManager.clearLvzByUniqueId(Constants.ID_PLAYER_HELP, name);
	
        lvzManager.drawScreenImageToPlayer(name, Constants.ID_PLAYER_HELP, xCoordType, yCoordType, x, y, Constants.checkBetHelpImageIndex);
        
       /*
            if(arguments.length >= 7) {
                if ("1".equalsIgnoreCase(arguments[5]) || "yes".equalsIgnoreCase(arguments[5]) || "true".equalsIgnoreCase(arguments[5]) || "on".equalsIgnoreCase(arguments[5]))
                    isCentered = true;
                width = Integer.parseInt(arguments[6]);
            }
	*/
            //lvzManager.drawString(Integer.parseInt(arguments[0]), Integer.parseInt(arguments[1]), arguments[2], Integer.parseInt(arguments[3]), Integer.parseInt(arguments[4]), isCentered, width);
    }

    public void botCommandLvzClear(String name, String message) {
        //Player's can't use this command.
        if (opList.getAccessLevel(name) == OperatorList.PLAYER_LEVEL) return;
        String[] arguments = message.split("\\s*[:,]\\s*");

        System.out.println(" args = " + arguments);
        if(arguments != null) {
            for (int i=0; i<arguments.length; i++)
                System.out.println("args ("+i+")"+arguments[i]);

            lvzManager.clearLvzByUniqueId(Integer.parseInt(arguments[0]), arguments[1]);
        }
    }

    /**
     * This method requests event information from any events your bot wishes
     * to "know" about; if left commented your bot will simply ignore them.
     */
    public void requestEvents() {
        EventRequester req = m_botAction.getEventRequester();
        req.request(EventRequester.MESSAGE);
        // req.request(EventRequester.ARENA_LIST);
        req.request(EventRequester.ARENA_JOINED);
        req.request(EventRequester.PLAYER_ENTERED);
        req.request(EventRequester.PLAYER_POSITION);
        req.request(EventRequester.PLAYER_LEFT);
        req.request(EventRequester.PLAYER_DEATH);
        // req.request(EventRequester.PRIZE);
        // req.request(EventRequester.SCORE_UPDATE);
        req.request(EventRequester.WEAPON_FIRED);
        //req.request(EventRequester.FREQUENCY_CHANGE);
        req.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        req.request(EventRequester.LOGGED_ON);
        // req.request(EventRequester.FILE_ARRIVED);
        // req.request(EventRequester.FLAG_VICTORY);
        // req.request(EventRequester.FLAG_REWARD);
        // req.request(EventRequester.SCORE_RESET);
        // req.request(EventRequester.WATCH_DAMAGE);
        // req.request(EventRequester.SOCCER_GOAL);
        // req.request(EventRequester.BALL_POSITION);
        // req.request(EventRequester.FLAG_POSITION);
        // req.request(EventRequester.FLAG_DROPPED);
        // req.request(EventRequester.FLAG_CLAIMED);
    }

    public void handleEvent(PlayerPosition event) {
        Player player = m_botAction.getPlayer(event.getPlayerID());
        byte rotation = event.getRotation();
        String playerName = m_botAction.getPlayerName(event.getPlayerID());
        if(playerName != null)
        	playerName = playerName.toLowerCase();

	if(zombieMiniGame.getMiniGameState() == -1) {
	        if(player != null) checkPlayerSeat(player);
	        updatePlayerBetRaiseGauge(playerName, rotation);
        }
    }

    public void handleEvent(FrequencyShipChange event) {
        //This event actually comes before your coordinates are updated 50% of the time.
        //So it thinks you are at the old position even though you were warped by freq change.
        Player player = m_botAction.getPlayer(event.getPlayerID());
        String playerName = m_botAction.getPlayerName(event.getPlayerID());
        if(playerName != null)
        	playerName = playerName.toLowerCase();
        	
	if(zombieMiniGame.getMiniGameState() == -1) {
	        if(player != null) checkPlayerSeat(player);
	        closeRules(playerName);
        }
    }

    public void handleEvent(WeaponFired event) {
        double bearing = Math.PI * 2 * (double)event.getRotation() / 40.0;
        String playerName = m_botAction.getPlayerName(event.getPlayerID());
        if(playerName != null)
        	playerName = playerName.toLowerCase();
        
        //m_botAction.sendArenaMessage("player = " + playerName + " rot:" + event.getRotation() + " X:" + event.getXLocation() + " Y:" + event.getYLocation()  + " XVel:" + event.getXVelocity() + " YVel:" + event.getYVelocity());
        if(zombieMiniGame.getMiniGameState() == -1) {
	        closeRules(playerName);
	        if(isPlayerLocatedAtTable(playerName)) {
	            setPlayerAction(playerName, event.getRotation());
	        }
        }
    }

    public void handleEvent(Message event) {
        m_commandInterpreter.handleEvent(event);
        
        //Wanted TextToSpeech to work with TEAM_MESSAGE.
        //But the BOT can't be on everybodys freq at once.
        //Maybe it's possible to log team messages somehow.
        //I made TextToSpeech atm work by PMing the bot !tts <message>.
        
        //Gather Machine Id's and IP's for tournament mode, from *info command.
        if(tournament != null) {
        	if(event.getMessageType() == Message.ARENA_MESSAGE && 
        	event.getMessage().startsWith("IP:") &&
        	event.getMessage().contains("TypedName:") &&
        	event.getMessage().contains("MachineId:")) {
        		//Sorts information from *info
        		String[] pieces = event.getMessage().split("  ");
        		String ip = pieces[0].substring(3);
        		String name = pieces[3].substring(10);
		        String mid = pieces[5].substring(10);
		        tournament.addSecurityInformation(name, mid, ip);
        	}
        }
    }
    
    public void handleEvent( PlayerDeath event ){
        if( Constants.IsZombieGameEnabled ){
            Player p = m_botAction.getPlayer( event.getKilleeID() );
            Player p2 = m_botAction.getPlayer( event.getKillerID() );
            if( p == null || p2 == null )
                return;
            zombieMiniGame.addNewKiller(p2.getPlayerName());
        }
    }

    public void handleEvent(PlayerEntered event) {
        //Player just entered this arena.
        String name = event.getPlayerName();
        String nameById = m_botAction.getPlayerName(event.getPlayerID());
        
        if (name != null)
        	name = name.toLowerCase();
        else if(nameById != null)
        	name = nameById.toLowerCase();
        else
	    	return; //No playerName can be detected, TODO:? somehow alert the player to re-join arena.
	    	
        //Greeting bongg
        m_botAction.sendUnfilteredPrivateMessage(name, "Greetings!", TextToSpeech.SOUNDS.GREETING.id);
        //This handles checking if player is sitting on table, updating their table money.
        checkTableEnteringPlayer(name);
        //Renders all the LVZ objects this player should be able to see.
        lvzManager.renderAllLvzForPlayer(name);
    }

    public void handleEvent(PlayerLeft event) {
        //Player just left this arena.
        Player player = m_botAction.getPlayer(event.getPlayerID());
        String playerName = m_botAction.getPlayerName(event.getPlayerID());
        if (playerName == null) return;
        playerName = playerName.toLowerCase();
        lvzManager.clearAllLvzObjectIdsByPlayerName(playerName, true);
	if(player != null) checkPlayerSeat(player, true);
    }

    public void handleEvent(LoggedOn event) {
        registerCommands();
        try {
            //Set the resolution to 32767x32767, which should allow larger area of WeaponFired Events.
            //Even though this seems looks like a hack.
            //It's not that bad and adds extra vew distance, more functionality with no cost.
            m_botAction.joinArena(m_botSettings.getString("arena"), Short.MAX_VALUE, Short.MAX_VALUE);
        } catch(Exception ex) {
            m_botAction.joinArena(m_botSettings.getString("arena"));
        }
    }

    public void handleEvent(ArenaJoined event) {
        m_botAction.receiveAllPlayerDeaths();
        //Makes the bot specate the middle of the map, instead of cycling the speccing of each player like crazy.
        m_botAction.stopReliablePositionUpdating();

        //This prevents the below settings fixer running more then once.
        boolean fixSettings = false;
        if(m_botSettings.getInt("SettingsFixed") != 1) {
            fixSettings = true;
            m_botSettings.put("SettingsFixed", 1);
            m_botSettings.save();
        }

        //Default settings fixer (this is temporary one time thing, so I know what settings need changing)
        //Should probably be ran once to fix whatever settings currently exist in that subarena.
        if(fixSettings && m_botAction.getArenaName().toLowerCase().equals("egcasino")) {
            //Max ship changes, Max times to recieve death without shooting set to high values, to avoid disconnects.
            m_botAction.sendUnfilteredPublicMessage("?set Security:MaxShipTypeSwitchCount=255");
            m_botAction.sendUnfilteredPublicMessage("?set Security:MaxDeathWithoutFiring=255");
            //Lol you could kill yourself? Don't remember this one haha.
            m_botAction.sendUnfilteredPublicMessage("?set Security:SuicideLimit=255");
            //No security kick offs
            m_botAction.sendUnfilteredPublicMessage("?set Security:SecurityKickOff=0");
            
            //No limit on messages
            m_botAction.sendUnfilteredPublicMessage("?set Message:QuickMessageLimit:10000");
            //People can use BonG's
            m_botAction.sendUnfilteredPublicMessage("?set Message:BongAllowed:1");
            
            //Makes all players on freq 0 who join ship instantly in middle of map.
            //This also works for all freqs even using setship/setfreq
            m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-X:512");
            m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Y:512");
            m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Radius:0");
            //So unlimited players can be on the same freq.
            m_botAction.sendUnfilteredPublicMessage("?set Team:MaxPerTeam:0");
            //Doesn't do anything but still good fix.
            m_botAction.sendUnfilteredPublicMessage("?set Misc:WarpRadiusLimit:0");

            //This is important so packets weapon packets get missed less often.
            m_botAction.sendUnfilteredPublicMessage("?set Routing:DeathDistance:16384");

            //So arena saves scores.
            m_botAction.sendUnfilteredPublicMessage("?set Misc:SaveSpawnScore:1");
            //Players start from spec mode.
            m_botAction.sendUnfilteredPublicMessage("?set Misc:StartInSpec:1");
            //Lvz file
            m_botAction.sendUnfilteredPublicMessage("?set Misc:LevelFiles:poker.lvz");

            //Greet Message
            m_botAction.sendUnfilteredPublicMessage("?set Misc:GreetMessage:Welcome to Texas Hold'em Poker Arena by Fatrolls & Sawyer");

            //Banner points to wear banner.
            m_botAction.sendUnfilteredPublicMessage("?set Misc:BannerPoints:0");

            //Warbirds and Spiders [The zombie dueling arena]
            //Red bomb for warbirds with same speed as javelin.
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:InitalBombs:1");
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:MaxBombs:1");
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:EmpBomb:0");
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:BombBounceCount:1"); //offical jav in tw.
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:InitialGuns:3"); //offical wb in tw.
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:MaxGuns:3"); //offical wb in tw.
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:InitialEnergy:1500"); //offical jav/wb in tw.
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:MaximumEnergy:1500"); //offical jav/wb in tw.
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:BombSpeed:2250"); //offical jav in tw.
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:BombFireDelay:75"); //offical jav in tw.
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:BombFireEnergy:1100"); //offical jav in tw.
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:BulletSpeed:5000"); //offical wb in tw.
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:BulletFireDelay:60"); //offical wb in tw.
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:BulletFireEnergy:450"); //offical wb in tw.

            m_botAction.sendUnfilteredPublicMessage("?set Warbird:BurstSpeed:3500"); //offical terr in tw.
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:BurstShrapnel:3"); //offical terr in tw.
            m_botAction.sendUnfilteredPublicMessage("?set Spider:BurstSpeed:3500"); //offical terr in tw.
            m_botAction.sendUnfilteredPublicMessage("?set Spider:BurstShrapnel:3"); //offical terr in tw.

            m_botAction.sendUnfilteredPublicMessage("?set Warbird:SuperTime:30000"); //offical in tw.
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:ShieldsTime:30000"); //offical in tw.
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:RocketTime:400"); //offical jav in tw.
            m_botAction.sendUnfilteredPublicMessage("?set Spider:SuperTime:30000"); //offical in tw.
            m_botAction.sendUnfilteredPublicMessage("?set Spider:ShieldsTime:30000"); //offical in tw.
            m_botAction.sendUnfilteredPublicMessage("?set Spider:RocketTime:400"); //offical jav in tw.

            //Warbird's starting items which are buyable should all be 0.
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:InitialRepel:0");
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:InitialBurst:0");
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:InitialThor:0");
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:InitialDecoy:0");
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:InitialPortal:0");
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:InitialRocket:0");
            //Warbird's max items which are buyable should all be 10.. [doesn't matter can be 255].
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:RepelMax:10");
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:BurstMax:10");
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:ThorMax:10");
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:DecoyMax:10");
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:PortalMax:10");
            m_botAction.sendUnfilteredPublicMessage("?set Warbird:RocketMax:10");
            //Spider's starting items which are buyable should all be 0.
            m_botAction.sendUnfilteredPublicMessage("?set Spider:InitialRepel:0");
            m_botAction.sendUnfilteredPublicMessage("?set Spider:InitialBurst:0");
            m_botAction.sendUnfilteredPublicMessage("?set Spider:InitialThor:0");
            m_botAction.sendUnfilteredPublicMessage("?set Spider:InitialDecoy:0");
            m_botAction.sendUnfilteredPublicMessage("?set Spider:InitialPortal:0");
            m_botAction.sendUnfilteredPublicMessage("?set Spider:InitialRocket:0");
            //Spider's max items which are buyable should all be 10.. [doesn't matter can be 255].
            m_botAction.sendUnfilteredPublicMessage("?set Spider:RepelMax:10");
            m_botAction.sendUnfilteredPublicMessage("?set Spider:BurstMax:10");
            m_botAction.sendUnfilteredPublicMessage("?set Spider:ThorMax:10");
            m_botAction.sendUnfilteredPublicMessage("?set Spider:DecoyMax:10");
            m_botAction.sendUnfilteredPublicMessage("?set Spider:PortalMax:10");
            m_botAction.sendUnfilteredPublicMessage("?set Spider:RocketMax:10");

            // Purchase any where
            m_botAction.sendUnfilteredPublicMessage("?set Cost:PurchaseAnytime:1");
            // Super costs 250,000 points [must win $250,000 or more points to get this]
            m_botAction.sendUnfilteredPublicMessage("?set Cost:Super:250000");
            // Shields cost 250,000 points [must win $250,000 or more points to get this]
            m_botAction.sendUnfilteredPublicMessage("?set Cost:Shield:250000");
            // Prox costs 200,000 points [must win $200,000 or more points to get this].
            m_botAction.sendUnfilteredPublicMessage("?set Cost:Prox:200000");
            // Bounce costs 100,000 points [must win $100,000 or more points to get this].
            m_botAction.sendUnfilteredPublicMessage("?set Cost:Bounce:100000");
            // Thor cost 50,000 points [must win $50,000 or more points to get this]
            m_botAction.sendUnfilteredPublicMessage("?set Cost:Thor:50000");
            // Rocket cost 25,000 points [must win $25,000 or more points to get this]
            m_botAction.sendUnfilteredPublicMessage("?set Cost:Rocket:5000");
            // Burst cost 5,000 points [must win $5,000 or more points to get this]
            m_botAction.sendUnfilteredPublicMessage("?set Cost:Burst:5000");
            // Repel cost 5,000 points [must win $5,000 or more points to get this]
            m_botAction.sendUnfilteredPublicMessage("?set Cost:Repel:5000");
            // Portal cost 5,000 points [must win $5,000 or more points to get this]
            m_botAction.sendUnfilteredPublicMessage("?set Cost:Portal:5000");
            // Decoy cost 1,000 points [must win $1,000 or more points to get this]
            m_botAction.sendUnfilteredPublicMessage("?set Cost:Decoy:1000");
        }

        startPokerGameLoop();
        m_botAction.sendArenaMessage("Texas Hold'em Poker has loaded.");
    }

   public boolean isTournamentMode() {
   	return tournament != null;
   }

   public PubBuxManager getPubBuxManager() {
   	return pubBuxManager;
   }

   public LvzManager getLvzManager() {
   	return lvzManager;
   }

    public void closeRules(String playerName) {
        if(lvzManager.isLvzByUniqueIdShown(Constants.ID_RULES, playerName))
            lvzManager.clearLvzByUniqueId(Constants.ID_RULES, playerName);
    }

    public void addToTablePlayer(String name, int tableIndex) {
        if(tableIndex < 0 || tableIndex >= pokerTables.length) return;

        Player player = m_botAction.getPlayer(name);
        if(player == null) return;

        Table pokerTable = pokerTables[tableIndex];
        if(pokerTable == null) {
            m_botAction.sendArenaMessage("[PokerBot Error]: Poker table doesn't exist.");
            return;
        }

	int pubBux = pubBuxManager.getPubBuxForPlayerName(name);
        PokerPlayer pokerPlayer = new PokerPlayer(name, pubBux);
        int playerSeat = pokerTable.addPlayer(pokerPlayer);

        //If player didn't exist, then table is already full before hand
        if(playerSeat == -1) {
            m_botAction.sendSmartPrivateMessage(name, "This table is full, try typing !list to see available tables to join with !joinTable #");
            return;
        }

        warpPlayerToSeat(name);
    }
    
    public void checkTableEnteringPlayer(String name) {
    	if(tournament != null) return; //PubBux isn't used in tournament mode.
    	
        int tableIndex = isPlayerSittingInTable(name);
        if(tableIndex == -1) return;

        Table pokerTable = pokerTables[tableIndex];
        if(pokerTable == null) return;

        PokerPlayer pokerPlayer = pokerTable.getPokerPlayerByName(name);
        if(pokerPlayer == null) return;
        
        //If any errors happen with SQL, then PubBux will always show as 0 on table.
	int pubBux = pubBuxManager.getPubBuxForPlayerName(name);
	pokerPlayer.setCash(pubBux);
        drawPlayerMoneyOnTable(pokerPlayer, tableIndex); 
    }

    public void warpPlayerToSeat(String name) {
        /**
          * If there is no problem with the table and the player.
          * It will force them to warp into table seat.
          * Where they are supposed to be no matter they are doing.
          */

        int tableIndex = isPlayerSittingInTable(name);
        if(tableIndex == -1) return;

        Player player = m_botAction.getPlayer(name);
        if(player == null) return;

        Table pokerTable = pokerTables[tableIndex];
        if(pokerTable == null) return;

        int playerSeat = pokerTable.getPlayerIndex(name);
        if(playerSeat == -1) return;

        Point mySeatLocation = getTableSeatLocation(tableIndex, playerSeat);
        if(mySeatLocation == null) return;

        //If player is specced, time to get him into a ship.
        if(!player.isPlaying())
            m_botAction.setShip(name, Tools.Ship.WARBIRD);

        //All players on the same table are on the same frequency as table number.
        if(player.getFrequency() != (tableIndex+1))
            m_botAction.setFreq(name, (tableIndex+1));

        //If player isn't in his seat on this table, it will warp them to it.
        if(!isCoordsInReach(player.getXTileLocation(), player.getYTileLocation(), mySeatLocation.x, mySeatLocation.y, 1))
            m_botAction.warpTo(name, mySeatLocation.x, mySeatLocation.y);
    }

    public boolean isPlayerLocatedAtTable(String name) {
        int tableIndex = isPlayerSittingInTable(name);
        if(tableIndex == -1) return false;

        Player player = m_botAction.getPlayer(name);
        if(player == null) return false;

        Table pokerTable = pokerTables[tableIndex];
        if(pokerTable == null) return false;

        int playerSeat = pokerTable.getPlayerIndex(name);
        if(playerSeat == -1) return false;

        Point mySeatLocation = getTableSeatLocation(tableIndex, playerSeat);
        if(mySeatLocation == null) return false;

        //If player is specced, time to get him into a ship.
        if(!player.isPlaying())
            return false;

        //If player isn't in his seat on this table, it will warp them to it.
        if(isCoordsInReach(player.getXTileLocation(), player.getYTileLocation(), mySeatLocation.x, mySeatLocation.y, 1))
            return true;
        return false;
    }

    /**
    * Sets the Player's action based on his playername and ship rotation.
    * Ship rotation determines which side of the square the player picked.
    * Also determines if a 2nd step process is required.
    * To complete any Bet/Raise with Sliders (second [bullet/bomb] shot).
    */
    public void setPlayerAction(String playerName, byte shipRotation) {
        int tableIndex = isPlayerSittingInTable(playerName);
        if(tableIndex == -1) return;
        
        Table pokerTable = pokerTables[tableIndex];
        if(pokerTable == null) return;
        
        PokerPlayer myPokerPlayer = pokerTable.getPokerPlayerByName(playerName);
        if(myPokerPlayer == null) return;
        
        //If not the current table actor, then no point in even going deeper then this.
        if(!pokerTable.isActor(myPokerPlayer)) return;
        
        //Check if this actor already set a action before prevent access below.
        //BET(0) or RAISE(0) are excluded since they use a 2 step process.
        Action action = myPokerPlayer.getAction();
        if((action != Action.BET && action != Action.RAISE && action != Action.BIG_BLIND && action != Action.SMALL_BLIND) && action != null) return;

        //Clear screen help screen (if any), this speeds it up rotateActor takes 200 milliseconds doesn't look good.
        if(lvzManager.isLvzByUniqueIdShown(Constants.ID_PLAYER_HELP, myPokerPlayer.getName()))
           lvzManager.clearLvzByUniqueId(Constants.ID_PLAYER_HELP, myPokerPlayer.getName());

        //Check if slider value selected by this shot.
        if(lvzManager.isLvzByUniqueIdShown(Constants.ID_PLAYER_GAUGE, myPokerPlayer.getName())) {
		//Removes the gauge.
		 lvzManager.clearLvzByUniqueId(Constants.ID_PLAYER_GAUGE, myPokerPlayer.getName());
		//gauge value 0 is minimum bet, 1-10 gauge is 10% percentages per gauge value.
	        int gaugeValue = pokerTable.getActorGaugeValue() * 10;
	        int betAmount = (gaugeValue == 0) ? pokerTable.getMinimumBet() : ((myPokerPlayer.getCash() / 100) * gaugeValue);

	        if(action instanceof BetAction) {
	        	m_botAction.sendSmartPrivateMessage(playerName, "You bet: ");
	        	m_botAction.sendSmartPrivateMessage(playerName, "Gauge Value: " + gaugeValue + " money is " + formatMoney(betAmount));
			action = new BetAction(betAmount);
		} else if(action instanceof RaiseAction) {
	        	m_botAction.sendSmartPrivateMessage(playerName, "You raised: ");
	        	m_botAction.sendSmartPrivateMessage(playerName, "Gauge Value: " + gaugeValue + " money is " + formatMoney(betAmount));
			action = new RaiseAction(betAmount);
		}
		myPokerPlayer.setAction(action);	
		pokerTable.setActorActed();
		return;
        }
        
        Player player = m_botAction.getPlayer(playerName);
        if(player == null) return;

        int playerSeat = pokerTable.getPlayerIndex(playerName);
        if(playerSeat == -1) return;

        Point mySeatLocation = getTableSeatLocation(tableIndex, playerSeat);
        if(mySeatLocation == null) return;

        boolean leftWallHit = false;
        boolean topWallHit = false;
        boolean rightWallHit = false;
        boolean bottomWallHit = false;
        boolean cornerHit = false;

        //If you are not in center of seat, then it has to simulate a bullet movement.
        if((mySeatLocation.x != player.getXTileLocation()) ||
                (mySeatLocation.y != player.getYTileLocation())) {

            double bearing = (Math.PI * 2) * ((double)shipRotation / 40.0);
            double simulatedPositionX = player.getXLocation();
            double simulatedPositionY = player.getYLocation();
            //Run simulation of bullet motion until it goes out of bound of the seat. [3x3 radius]
            //[3*16=48 loops maximum]

            //TODO: Optimize, avoiding the loop by doing multiplication based on some kind of distance.

            while(true) {
                //Bullet Travels at the rate of 1 pixel per step.
                simulatedPositionX += Math.sin(bearing);
                simulatedPositionY -= Math.cos(bearing);

                if(!isCoordsInReach((int)(simulatedPositionX / 16), (int)(simulatedPositionY / 16), mySeatLocation.x, mySeatLocation.y, 1))
                    break;
            }
            //Offsets hit coord X,Y by my seat middle coordinates, to make it universal for all seats.
            int hitOffsetX = mySeatLocation.x - (int)(simulatedPositionX / 16);
            int hitOffsetY = mySeatLocation.y - (int)(simulatedPositionY / 16);


            if((hitOffsetX == 2 && hitOffsetY == 2) || //Top Left Corner
                    (hitOffsetX == -2 && hitOffsetY == 2) || //Top Right Corner
                    (hitOffsetX == -2 && hitOffsetY == -2) ||  //Bottom Right Corner
                    (hitOffsetX == 2 && hitOffsetY == -2)) //Bottom Left Corner
                cornerHit = true;
            else if(hitOffsetX == 2 && (hitOffsetY >= -1 && hitOffsetY <= 1)) //Any Left Wall Tile
                leftWallHit = true;
            else if((hitOffsetX >= -1 && hitOffsetX <= 1) && hitOffsetY == 2) //Any Top Wall Tile
                topWallHit = true;
            else if(hitOffsetX == -2 && (hitOffsetY >= -1 && hitOffsetY <= 1)) //Any Right Wall Tile
                rightWallHit = true;
            else if((hitOffsetX >= -1 && hitOffsetX <= 1) && hitOffsetY == -2) //Any Bottom Wall Tile
                bottomWallHit = true;
        } else {
            //Center of the seat no need to simulate any motion here.
            if(shipRotation >= 26 && shipRotation <= 34)
                leftWallHit = true;
            else if(shipRotation >= 36 || shipRotation <= 4)
                topWallHit = true;
            else if(shipRotation >= 6 && shipRotation <= 14)
                rightWallHit = true;
            else if(shipRotation >= 16 && shipRotation <= 24)
                bottomWallHit = true;
            else
                cornerHit = true;
        }

        Set<Action> allowedActions = pokerTable.getAllowedActions(myPokerPlayer);

        if(leftWallHit && (allowedActions.contains(Action.CHECK) || allowedActions.contains(Action.CALL))) {
        	myPokerPlayer.setAction(allowedActions.contains(Action.CHECK) ? Action.CHECK : Action.CALL);
        } else if(topWallHit && (allowedActions.contains(Action.RAISE) || allowedActions.contains(Action.BET))) {
        	myPokerPlayer.setAction(allowedActions.contains(Action.RAISE) ? Action.RAISE : Action.BET);
        } else if(rightWallHit && allowedActions.contains(Action.FOLD)) {
        	myPokerPlayer.setAction(Action.FOLD);
        } else if(bottomWallHit) {
        	if(allowedActions.contains(Action.ALL_IN)) {
        		myPokerPlayer.setAction(Action.ALL_IN);
        	} else {
        		m_botAction.sendSmartPrivateMessage(playerName, "You can't go ALL IN, this must be a fixed limit game. (Join No-Limit table to do ALL IN)");
        		return;
        	}
        } else if(cornerHit) {
        	m_botAction.sendSmartPrivateMessage(playerName, "You can't shoot the corners, try to shoot a side, hurry up!");
        	return;
        }

        //Remove the warning (more time for 2nd step slider), as long as it isn't a corner shot to avoid trolling.
        if(!cornerHit) {
        	pokerTable.resetActTime();
        	myPokerPlayer.setWarning(null);
        }
        
	if(!topWallHit && !cornerHit) //This is only if it doesn't open the Bet/Raise Gauges, and not corner shot.
         	pokerTable.setActorActed();
    }

    /**
    *
    * Gets the table Seat Warp Coords Location,
    * Based on what table and seat you are sitting on.
    *
    */
    public Point getTableSeatLocation(int tableIndex, int playerSeat) {
        if(playerSeat == -1) return null;
        if(tableIndex == -1 || tableIndex >= pokerTables.length) return null;

        int x = tableIndex % 4;
        int y = (tableIndex <= 3) ? 0 : 1;

        Point tableFirstSeatLocation = Constants.tableSeatWarpPoints[playerSeat];
        x = ((x*Constants.tableSeatWarpPointOffset.x) + tableFirstSeatLocation.x);
        y = ((y*Constants.tableSeatWarpPointOffset.y) + tableFirstSeatLocation.y);
        return new Point(x, y);
    }

    public int isPlayerSittingInTable(String name) {
        for(int t = 0; t < pokerTables.length; t++) {
            if(pokerTables[t] == null) continue;
            if(pokerTables[t].hasPlayerByName(name))
                return t;
        }
        return -1;
    }

    public boolean isPlayerDisconnectedFromTable(String name) {
        for(int t = 0; t < pokerTables.length; t++) {
            if(pokerTables[t] == null) continue;
            if(pokerTables[t].isDisconnectedPlayer(name))
                return true;
        }
        return false;
    }

    public void checkPlayerSeat(Player player, Boolean...playerLeft) {
        if(player != null) {
            String playerName = player.getPlayerName().toLowerCase();
            int sittingTableId = isPlayerSittingInTable(playerName);
            if(sittingTableId != -1) {
                Table pokerTable = pokerTables[sittingTableId];
                if(pokerTable == null) return;
                PokerPlayer pokerPlayer = pokerTable.getPokerPlayerByName(playerName);

                //If you spec, 60 second countdown starts until you are removed from table.
                if(player.getShipType() == Tools.Ship.SPECTATOR && !isPlayerDisconnectedFromTable(playerName)) {
                    //Don't add this player to disconnect list as player already did !leave command.
                    if(pokerPlayer != null && pokerPlayer.isLeaving()) return;
                    pokerTable.addDisconnectedPlayer(playerName);
                    m_botAction.sendSmartPrivateMessage(playerName, "You have 60 seconds to enter a ship, or you will be kicked out of the table #" + (sittingTableId+1) + ".");
                } else if(playerLeft.length > 0 && playerLeft[0]) {
                    //Don't add this player to disconnect list as player already did !leave command.
                    if(pokerPlayer != null && pokerPlayer.isLeaving()) return;
                      pokerTable.addDisconnectedPlayer(playerName);
                    m_botAction.sendRemotePrivateMessage(playerName, "You have left the Poker Arena, you have 60 seconds to come back and enter a ship or you will be kicked out of the table #" + (sittingTableId+1) + ".");
                } else {
                    //Instantly warp back to table seat, if you change ships.
                    warpPlayerToSeat(playerName);
                    pokerTable.removeDisconnectedPlayer(playerName);
                }
            }
        }
    }
    
public void updateRoyalFlushJackpot() {
	if(m_botAction.getBotName() == null || m_botAction.getBotName().isEmpty()) {
		m_botAction.sendArenaMessage("Seems the PokerBot can't figure out it's own name!");
		return;
	}
	
	int pubBux = pubBuxManager.getPubBuxForPlayerName(m_botAction.getBotName());
   
	if(!lvzManager.isLvzByUniqueIdShown(Constants.ID_ROYAL_FLUSH_JACKPOT_LABEL, Constants.LVZ_SEND_TO_ALL))
		lvzManager.drawScreenStringToAll(Constants.ID_ROYAL_FLUSH_JACKPOT_LABEL, "Royal Flush Jackpot", CoordType.N, CoordType.S, 10, 10);
        if(lvzManager.isLvzByUniqueIdShown(Constants.ID_ROYAL_FLUSH_JACKPOT_VALUE, Constants.LVZ_SEND_TO_ALL))
            lvzManager.clearLvzByUniqueId(Constants.ID_ROYAL_FLUSH_JACKPOT_VALUE);
        lvzManager.drawScreenStringToAll(Constants.ID_ROYAL_FLUSH_JACKPOT_VALUE, formatMoney(pubBux), CoordType.N, CoordType.S, 10, 20, 150);
}

public void setPlayerShootHelpOptions(int tableIndex) {
	//TODO: Add more Lvz Images without ALL-IN.. for Fixed Games..?
	//Downside is LVZ will be +/- 200 KB bigger.
	 
        if(tableIndex < 0 || tableIndex >= pokerTables.length) return;
        
        Table pokerTable = pokerTables[tableIndex];
        if(pokerTable == null) return;
        
        PokerPlayer actor = pokerTable.getActor();
        if(actor == null) return;
        
        Action action = actor.getAction();
        //Don't re-show it after it's closed, ignore BIG_BLIND and SMALL_BLIND or SET to any other action.
        if(action != Action.BIG_BLIND && action != Action.SMALL_BLIND && action != null) return;
        
        //Already seen help screen or some old help screen was never removed.
        if(lvzManager.isLvzByUniqueIdShown(Constants.ID_PLAYER_HELP, actor.getName())) return;

        int helpNumber = -1;
        int xPosition = -161;
        
        Set<Action> allowedActions = pokerTable.getAllowedActions(actor);
        
        if(allowedActions.contains(Action.CHECK) && allowedActions.contains(Action.BET)) {
        	helpNumber = Constants.checkBetHelpImageIndex;
        	xPosition = Constants.checkBetHelpWidthX;
        } else if(allowedActions.contains(Action.CHECK) && allowedActions.contains(Action.RAISE)) {
        	helpNumber = Constants.checkRaiseHelpImageIndex;
        	xPosition = Constants.checkRaiseHelpWidthX;
        } else if(allowedActions.contains(Action.CALL) && allowedActions.contains(Action.BET)) {
        	helpNumber = Constants.callBetHelpImageIndex;
        	xPosition = Constants.callBetHelpWidthX;
        } else if(allowedActions.contains(Action.CALL) && allowedActions.contains(Action.RAISE)) {
        	helpNumber = Constants.callRaiseHelpImageIndex;
       		xPosition = Constants.callRaiseHelpWidthX;
  	}
  	if(helpNumber == -1) return;
  	
        lvzManager.drawScreenImageToPlayer(actor.getName(), Constants.ID_PLAYER_HELP, CoordType.E, CoordType.E, xPosition, 0, helpNumber);
}


public void updatePlayerBetRaiseGauge(String playerName, byte currentRotation) {
        int tableIndex = isPlayerSittingInTable(playerName);
        if(tableIndex < 0 || tableIndex >= pokerTables.length) return;
        
        Table pokerTable = pokerTables[tableIndex];
        if(pokerTable == null) return;
        
        PokerPlayer myPokerPlayer = pokerTable.getPokerPlayerByName(playerName);
        if(myPokerPlayer == null) return;
        
        if(!pokerTable.isActor(myPokerPlayer)) return;
        
        Action action = myPokerPlayer.getAction();
        if(action == null || !(action instanceof BetAction || action instanceof RaiseAction)) return;

        Set<Action> allowedActions = pokerTable.getAllowedActions(myPokerPlayer);
        if(!allowedActions.contains(action)) return;
        
        int gaugeLvzImgageId = Constants.firstGuageImageIndex;
        
       //Start up a gauge first time.
        if(pokerTable.getActorGaugeInitialShipRotation() == -1) {
        	pokerTable.setActorGaugeInitialShipRotation(currentRotation);
        	
	        //Clear old gauge screen first (just in case).
	        if(lvzManager.isLvzByUniqueIdShown(Constants.ID_PLAYER_GAUGE, myPokerPlayer.getName()))
	            lvzManager.clearLvzByUniqueId(Constants.ID_PLAYER_GAUGE, myPokerPlayer.getName());
	  	
	        lvzManager.drawScreenImageToPlayer(myPokerPlayer.getName(), Constants.ID_PLAYER_GAUGE, CoordType.C, CoordType.N, -240, 50, gaugeLvzImgageId);
        } else {
        	//Yeah this turned out a bit ugly when a warbird starts rotating it's ship from where it was pointing last.
        	//And not from Left to Right, people wouldn't understand what other person is thinking.
        	//So this makes the game less skill based.
        	//int initialRotation = pokerTable.getActorGaugeInitialShipRotation();
		//[0-39] = 40 rotations.		
		//int minValue = Math.min(initialRotation, currentRotation);    
		//int maxValue = Math.max(initialRotation, currentRotation);
		//int rotationDifference = Math.min(maxValue - minValue, minValue + 40 - maxValue) % 20;
		//pokerTable.setActorGaugeValue((byte)(((rotationDifference / 2) >= 10) ? 10 : (rotationDifference / 2)));
		
		byte gaugeValue = 0;
		
		switch(currentRotation) {
			case 29: //just to even it out.
			case 30: //Left
				gaugeValue = 0;
				break;
			case 31:
			case 32:
				gaugeValue = 1;
				break;
			case 33:
			case 34:
				gaugeValue = 2;
				break;
			case 35:
			case 36:
				gaugeValue = 3;
				break;
			case 37:
			case 38:
				gaugeValue = 4;
				break;
			case 39:
			case 0: //Up
				gaugeValue = 5;
				break;
			case 1:
			case 2:
				gaugeValue = 6;
				break;
			case 3:
			case 4:
				gaugeValue = 7;
				break;
			case 5:
			case 6:
				gaugeValue = 8;
				break;
			case 7:
			case 8:
				gaugeValue = 9;
				break;
			case 9:
			case 10: //Right
				gaugeValue = 10;
				break;
		}
		pokerTable.setActorGaugeValue(gaugeValue);
		gaugeLvzImgageId += pokerTable.getActorGaugeValue();
		System.out.println("Gauge at " + pokerTable.getActorGaugeValue() + " full GaugeValue at " + gaugeLvzImgageId);
		
		//TODO: Cool sound effect for moving the gauge put in later.. commented out for now
		/*
		Player player = m_botAction.getPlayer(playerName);
		System.out.println("PLAYEr = " + player + " name = " + playerName + "  freq = " + player.getFrequency());
	        if(player != null)
			m_botAction.sendUnfilteredTargetTeamMessage(player.getFrequency(), "1",TextToSpeech.SOUNDS.GAUGE_MOVEMENT.id);
		*/
        	lvzManager.replaceShownImageToPlayer(myPokerPlayer.getName(), Constants.ID_PLAYER_GAUGE, gaugeLvzImgageId);
        }
}

    public void drawPlayerNamesOnTable(PokerPlayer player, int tableIndex) {
        if(player == null) return;
        if(tableIndex < 0 || tableIndex >= pokerTables.length) return;

        Table pokerTable = pokerTables[tableIndex];
        if(pokerTable == null) return;

        int playerSeat = pokerTable.getPlayerIndex(player.getName());
        if(playerSeat == -1) return;

        int x, y, id;

        Point tableTopLeftInPixels = Constants.tableTopLeftPixelOffsets[tableIndex];
        Point tablePlayerNamePixels = Constants.tablePlayerNamesPixelOffsets[playerSeat];

        x = tableTopLeftInPixels.x + tablePlayerNamePixels.x;
        y = tableTopLeftInPixels.y + tablePlayerNamePixels.y;
        id = (playerSeat + (tableIndex * 10)) + Constants.ID_PLAYER_NAME;
        String playerName = (player.getName().length() > Constants.TABLE_PLAYERNAME_LENGTH_MAX) ? player.getName().substring(0, Constants.TABLE_PLAYERNAME_LENGTH_MAX) : player.getName();
        int centerWidthPixels = Constants.TABLE_PLAYERNAME_LENGTH_MAX * 8;
        
        //Clear old playernames first (just in case).
        if(lvzManager.isLvzByUniqueIdShown(id, Constants.LVZ_SEND_TO_ALL))
            lvzManager.clearLvzByUniqueId(id);
            
        lvzManager.drawMapStringToAll(id, playerName, x, y, centerWidthPixels);
    }
    
    public void drawPlayerMoneyOnTable(PokerPlayer player, int tableIndex) {
        if(player == null) return;
        if(tableIndex < 0 || tableIndex >= pokerTables.length) return;

        Table pokerTable = pokerTables[tableIndex];
        if(pokerTable == null) return;

        int playerSeat = pokerTable.getPlayerIndex(player.getName());
        if(playerSeat == -1) return;

        int x, y, id;

        Point tableTopLeftInPixels = Constants.tableTopLeftPixelOffsets[tableIndex];
        Point tablePlayerMoneyPixels = Constants.tablePlayerMoneyPixelOffsets[playerSeat];

        x = tableTopLeftInPixels.x + tablePlayerMoneyPixels.x;
        y = tableTopLeftInPixels.y + tablePlayerMoneyPixels.y;
        id = (playerSeat + (tableIndex * 10)) + Constants.ID_PLAYER_MONEY;
        String playerMoney = (formatMoney(player.getCash()).length() > Constants.TABLE_PLAYERMONEY_LENGTH_MAX) ? formatMoney(player.getCash()).substring(0, Constants.TABLE_PLAYERMONEY_LENGTH_MAX) : formatMoney(player.getCash());
        int centerWidthPixels = Constants.TABLE_PLAYERMONEY_LENGTH_MAX * 8;

        //Clear old money first.
        if(lvzManager.isLvzByUniqueIdShown(id, Constants.LVZ_SEND_TO_ALL))
            lvzManager.clearLvzByUniqueId(id);
        lvzManager.drawMapStringToAll(id, playerMoney, x, y, centerWidthPixels);
    }

    public void playerShowCardsOnTable(PokerPlayer pokerPlayer, int tableIndex) {
        if(tableIndex < 0 || tableIndex >= pokerTables.length) return;
        
        Table pokerTable = pokerTables[tableIndex];
        if(pokerTable == null) return;

        int x, y, id;
        Point tableTopLeftInPixels = Constants.tableTopLeftPixelOffsets[tableIndex];
        Point[] playerCardPixels;
        int playerSeat;
        List<String> images = new ArrayList<String>();
        int cardImageIndex;

        if(pokerPlayer == null) return;
        playerSeat = pokerTable.getPlayerIndex(pokerPlayer.getName());
        if(playerSeat == -1) return;

        playerCardPixels = Constants.playerCardsPixelOffsets[playerSeat];
            
        Card[] cards = pokerPlayer.getCards();
        if(cards == null || cards.length < 2) return;

        id = (playerSeat + (tableIndex * 10)) + Constants.ID_PLAYER_CARDS_UP;
        for(int i = 0; i < cards.length; i++) {
            x = tableTopLeftInPixels.x + playerCardPixels[i].x;
            y = tableTopLeftInPixels.y + playerCardPixels[i].y;

            cardImageIndex = Constants.firstPlayerCardImageIndex + cards[i].getLvzPosition();
            images.add(id + "," + x + "," + y + "," + cardImageIndex);
        }

        //Clears the player's cards hidden to everybody except the player himself.
        id = (playerSeat + (tableIndex * 10)) + Constants.ID_PLAYER_CARDS_DOWN;
        if(lvzManager.isLvzByUniqueIdShown(id, Constants.LVZ_SEND_TO_ALL_BUT_ONE))
            lvzManager.clearLvzByUniqueId(id);
        //Extra cleaning of cards of shown cards	
        id = (playerSeat + (tableIndex * 10)) + Constants.ID_PLAYER_CARDS_UP;
        if(lvzManager.isLvzByUniqueIdShown(id, Constants.LVZ_SEND_TO_ALL_BUT_ONE))
            lvzManager.clearLvzByUniqueId(id);
        
        //draw's the cards to yourself.
        lvzManager.drawMapImageToAllButIgnorePlayer(pokerPlayer.getName(), images);
        images.clear();
    }
    
    public void drawPlayerCardsOnTable(int tableIndex) {
        if(tableIndex < 0 || tableIndex >= pokerTables.length) return;

        Table pokerTable = pokerTables[tableIndex];
        if(pokerTable == null) return;

        int x, y, id;
        Point tableTopLeftInPixels = Constants.tableTopLeftPixelOffsets[tableIndex];
        Point[] playerCardPixels;
        int playerSeat;
        Card[] cards;
        List<String> images = new ArrayList<String>();
        int cardImageIndex;

        //2 blank cards for all players sitting on table show to all players
        for(PokerPlayer player : pokerTable.getActivePlayers()) {
            if(player == null) continue;

            playerSeat = pokerTable.getPlayerIndex(player.getName());
            if(playerSeat == -1) continue;

            playerCardPixels = Constants.playerCardsPixelOffsets[playerSeat];
            id = (playerSeat + (tableIndex * 10)) + Constants.ID_PLAYER_CARDS_DOWN;           
            for(int i = 0; i < 2; i++) {
                x = tableTopLeftInPixels.x + playerCardPixels[i].x;
                y = tableTopLeftInPixels.y + playerCardPixels[i].y;
                images.add(id + "," + x + "," + y + "," + (Constants.firstPlayerCardImageIndex+Card.BLANK_CARD));
            }
	    lvzManager.drawMapImageToAllButIgnorePlayer(player.getName(), images);
            images.clear();
        }

        //Real cards show only to own players or to all for showing players (showdown).
        for(PokerPlayer player : pokerTable.getActivePlayers()) {
            if(player == null) continue;

            playerSeat = pokerTable.getPlayerIndex(player.getName());
            if(playerSeat == -1) continue;

            playerCardPixels = Constants.playerCardsPixelOffsets[playerSeat];
            
            cards = player.getCards();
            if(cards == null || cards.length < 2) continue;

            id = (playerSeat + (tableIndex * 10)) + Constants.ID_PLAYER_CARDS_UP;
            for(int i = 0; i < cards.length; i++) {
                x = tableTopLeftInPixels.x + playerCardPixels[i].x;
                y = tableTopLeftInPixels.y + playerCardPixels[i].y;

                cardImageIndex = Constants.firstPlayerCardImageIndex + cards[i].getLvzPosition();
                images.add(id + "," + x + "," + y + "," + cardImageIndex);
            }
            //draw's the cards to yourself.
            lvzManager.drawMapImageToPlayer(player.getName(), images);
            images.clear();
        }
    }
    
    public void drawCommunityCardsOnTable(int tableIndex) {
        if(tableIndex < 0 || tableIndex >= pokerTables.length) return;

        Table pokerTable = pokerTables[tableIndex];
        if(pokerTable == null) return;

        List<Card> communityCards = pokerTable.getCommunityCards();
        if(communityCards == null) return;

        int x, y, id;
        int cardPosition = 0;
        Point tableTopLeftInPixels = Constants.tableTopLeftPixelOffsets[tableIndex];

	for(Card card : communityCards) {
		if(card == null) continue;
		
	        Point communityCardPixels = Constants.tableCardsPixelOffsets[cardPosition];
	        
            	id = (cardPosition + (tableIndex * 5)) + Constants.ID_COMMUNITY_CARDS;
	        x = tableTopLeftInPixels.x + communityCardPixels.x;
	        y = tableTopLeftInPixels.y + communityCardPixels.y;
	        //Check if the community card was already drawn, skip it.
	        if(lvzManager.isLvzByUniqueIdShown(id, Constants.LVZ_SEND_TO_ALL)) {
	            cardPosition++;
	            continue;
	        }
	        
	        //Draw the new community card.
	        lvzManager.drawMapImageToAll(id, x, y, Constants.firstTableCardImageIndex + card.getLvzPosition());
	        cardPosition++;
    	}
    }

    public void drawDealerChipOnTable(int tableIndex) {
        if(tableIndex < 0 || tableIndex >= pokerTables.length) return;

        Table pokerTable = pokerTables[tableIndex];
        if(pokerTable == null) return;

        PokerPlayer dealer = pokerTable.getDealer();
        if(dealer == null) return;
        int dealerSeatIndex = pokerTable.getPlayerIndex(dealer.getName());
        if(dealerSeatIndex == -1) return;

        int x, y, id;
        Point tableTopLeftInPixels = Constants.tableTopLeftPixelOffsets[tableIndex];

        Point dealerChipPixels = Constants.dealerChipPixelOffsets[dealerSeatIndex];
        id = tableIndex + Constants.ID_DEALER_CHIP;
        x = tableTopLeftInPixels.x + dealerChipPixels.x;
        y = tableTopLeftInPixels.y + dealerChipPixels.y;
        //Clear old dealer chip first.
        if(lvzManager.isLvzByUniqueIdShown(id, Constants.LVZ_SEND_TO_ALL))
            lvzManager.clearLvzByUniqueId(id);
        //Draw new dealer chip.
        lvzManager.drawMapImageToAll(id, x, y, Constants.dealerImageIndex);
    }
    
    public void drawBlinkerOnTable(int tableIndex) {
        if(tableIndex < 0 || tableIndex >= pokerTables.length) return;

        Table pokerTable = pokerTables[tableIndex];
        if(pokerTable == null) return;

        PokerPlayer currentTurnPlayer = pokerTable.getActor();
        if(currentTurnPlayer == null) return;
        int currentTurnSeatIndex = pokerTable.getPlayerIndex(currentTurnPlayer.getName());
        if(currentTurnSeatIndex == -1) return;

        int x, y, id;
        Point tableTopLeftInPixels = Constants.tableTopLeftPixelOffsets[tableIndex];

        Point tableBlinkerPixels = Constants.tableBlinkerPixelOffsets[currentTurnSeatIndex];
        id = tableIndex + Constants.ID_TABLE_BLINKER;
        x = tableTopLeftInPixels.x + tableBlinkerPixels.x;
        y = tableTopLeftInPixels.y + tableBlinkerPixels.y;
        //Clear old dealer chip first.
        if(lvzManager.isLvzByUniqueIdShown(id, Constants.LVZ_SEND_TO_ALL))
        	lvzManager.clearLvzByUniqueId(id);
        //Draw new table blinker animation.
        lvzManager.drawMapImageToAll(id, x, y, Constants.tableBlinkerImageIndex);
    }
    
    public void resetTable(int tableIndex) {
    	//TODO: merge clearLeavingPlayersOnTable and resetTable card removing into a void.
       if(tableIndex < 0 || tableIndex >= pokerTables.length) return;

        Table pokerTable = pokerTables[tableIndex];
        if(pokerTable == null) return;

        int id;
        int playerSeat;

        for(PokerPlayer pokerPlayer : pokerTable.getPlayers()) {
            if(pokerPlayer == null) continue;

                playerSeat = pokerTable.getPlayerIndex(pokerPlayer.getName());
                if(playerSeat == -1) continue;
                
                //Clears the player's cards hidden to everybody except the player himself.
                id = (playerSeat + (tableIndex * 10)) + Constants.ID_PLAYER_CARDS_DOWN;
                if(lvzManager.isLvzByUniqueIdShown(id, Constants.LVZ_SEND_TO_ALL_BUT_ONE))
                    lvzManager.clearLvzByUniqueId(id);
                //Clears the player's cards shown to all players when he decides to show cards or wins
                id = (playerSeat + (tableIndex * 10)) + Constants.ID_PLAYER_CARDS_UP;
                if(lvzManager.isLvzByUniqueIdShown(id, Constants.LVZ_SEND_TO_ALL_BUT_ONE))
                    lvzManager.clearLvzByUniqueId(id);
                //Clears the player's cards shown to the player himself only.
                id = (playerSeat + (tableIndex * 10)) + Constants.ID_PLAYER_CARDS_UP;
                if(lvzManager.isLvzByUniqueIdShown(id, pokerPlayer.getName()))
                    lvzManager.clearLvzByUniqueId(id, pokerPlayer.getName());
                //Just in case
                id = (playerSeat + (tableIndex * 10)) + Constants.ID_PLAYER_CARDS_DOWN;
                if(lvzManager.isLvzByUniqueIdShown(id, pokerPlayer.getName()))
                    lvzManager.clearLvzByUniqueId(id, pokerPlayer.getName());    
                //Clear screen help screen (if any)
	        if(lvzManager.isLvzByUniqueIdShown(Constants.ID_PLAYER_HELP, pokerPlayer.getName()))
	           lvzManager.clearLvzByUniqueId(Constants.ID_PLAYER_HELP, pokerPlayer.getName());
		//Clear screen gauge.  (if any)
		if(lvzManager.isLvzByUniqueIdShown(Constants.ID_PLAYER_GAUGE, pokerPlayer.getName()))
		   lvzManager.clearLvzByUniqueId(Constants.ID_PLAYER_GAUGE, pokerPlayer.getName());
        	id = (playerSeat + (tableIndex * 10)) + Constants.ID_CHIP_VALUES;
        	//Clears old chip values and amount (if any)
        	if(lvzManager.isLvzByUniqueIdShown(id, Constants.LVZ_SEND_TO_ALL))
	            lvzManager.clearLvzByUniqueId(id);
        }
        
        //Clear money on middle of table
        id = ((tableIndex+1) * 10) + Constants.ID_CHIP_VALUES;
	//Clears old chip values and amount (if any)
	if(lvzManager.isLvzByUniqueIdShown(id, Constants.LVZ_SEND_TO_ALL))
            lvzManager.clearLvzByUniqueId(id);
        
	//Clear the community cards shown to all players, no risks here.
	for(int cardPosition = 0; cardPosition <= 4; cardPosition++) {
		id = (cardPosition + (tableIndex * 5)) + Constants.ID_COMMUNITY_CARDS;
	        if(lvzManager.isLvzByUniqueIdShown(id, Constants.LVZ_SEND_TO_ALL))
	            lvzManager.clearLvzByUniqueId(id);
	}
    }
	
    public void clearLeavingPlayersOnTable(int tableIndex) {
        if(tableIndex < 0 || tableIndex >= pokerTables.length) return;

        Table pokerTable = pokerTables[tableIndex];
        if(pokerTable == null) return;

        int id;
        int playerSeat;

        for(PokerPlayer pokerPlayer : pokerTable.getPlayers()) {
            if(pokerPlayer == null) continue;
	
            if(pokerPlayer.isLeaving() || pokerPlayer.getCash() < 1) {
                playerSeat = pokerTable.getPlayerIndex(pokerPlayer.getName());
                if(playerSeat == -1) continue;

                //Clear the player name on top of your table seat.
                id = (playerSeat + (tableIndex * 10)) + Constants.ID_PLAYER_NAME;
                if(lvzManager.isLvzByUniqueIdShown(id, Constants.LVZ_SEND_TO_ALL))
                    lvzManager.clearLvzByUniqueId(id);
                //Clears the player's cards hidden to everybody except the player himself.
                id = (playerSeat + (tableIndex * 10)) + Constants.ID_PLAYER_CARDS_DOWN;
                if(lvzManager.isLvzByUniqueIdShown(id, Constants.LVZ_SEND_TO_ALL_BUT_ONE))
                    lvzManager.clearLvzByUniqueId(id);
                //Clears the player's cards shown to the player himself only.
                id = (playerSeat + (tableIndex * 10)) + Constants.ID_PLAYER_CARDS_UP;
                if(lvzManager.isLvzByUniqueIdShown(id, pokerPlayer.getName()))
                    lvzManager.clearLvzByUniqueId(id, pokerPlayer.getName());
                //Clear the player's money shown to all players on bottom of your table seat.
                id = (playerSeat + (tableIndex * 10)) + Constants.ID_PLAYER_MONEY;
                if(lvzManager.isLvzByUniqueIdShown(id, Constants.LVZ_SEND_TO_ALL))
                    lvzManager.clearLvzByUniqueId(id);
		//Clear help screen. (if any)
	        if(lvzManager.isLvzByUniqueIdShown(Constants.ID_PLAYER_HELP, pokerPlayer.getName()))
	           lvzManager.clearLvzByUniqueId(Constants.ID_PLAYER_HELP, pokerPlayer.getName());
		//Clear gauge. (if any)
		if(lvzManager.isLvzByUniqueIdShown(Constants.ID_PLAYER_GAUGE, pokerPlayer.getName()))
		   lvzManager.clearLvzByUniqueId(Constants.ID_PLAYER_GAUGE, pokerPlayer.getName());
	  	
	        if(pokerPlayer.getCash() < 1)
	    	    m_botAction.sendSmartPrivateMessage(pokerPlayer.getName(), "Oh dear you don't have any pubbux left over to play on table #" + (tableIndex+1) + ".", Tools.Sound.GAME_SUCKS);
	    	
                m_botAction.sendSmartPrivateMessage(pokerPlayer.getName(), "You have been removed from table #" + (tableIndex+1) + ".");
                pokerTable.removePlayer(pokerPlayer);

                //prizes the a player Warp! (to get him out of the table in a nice way lol).
                m_botAction.specificPrize(pokerPlayer.getName(), Tools.Prize.WARP);
            }
        }
    }

    public void warnPlayerOnTable(int tableIndex) {
        if(tableIndex < 0 || tableIndex >= pokerTables.length) return;

        Table pokerTable = pokerTables[tableIndex];
        if(pokerTable == null) return;
        PokerPlayer actor = pokerTable.getActor();
        if(actor == null) return;
                
        if(actor.getWarning() == null && (System.currentTimeMillis() - pokerTable.getActTime()) >= Tools.TimeInMillis.SECOND * 10) {
            //After 5 seconds passed, time to give a 15 second remaining warning.
            m_botAction.sendSmartPrivateMessage(actor.getName(), "You have 15 seconds left to react or you will fold.");
            actor.setWarning(WarningType.WARNED);
        } else if(actor.getWarning() == WarningType.WARNED && (System.currentTimeMillis() - pokerTable.getActTime()) >= Tools.TimeInMillis.SECOND * 25) {
            //After 20 seconds passed, make player fold and increase his sitout counter.

            m_botAction.sendSmartPrivateMessage(actor.getName(), "You have folded because time ran out.");
            actor.setWarning(WarningType.WARNING_EXCEEDED);
            actor.setSitOutCount(actor.getSitOutCount() + 1);
            if(actor.getSitOutCount() >= 3) {
                m_botAction.sendSmartPrivateMessage(actor.getName(), "You haven't did anything for 3 rounds, you will be removed from table.");
                actor.setLeaving(true);
            }
        }
    }

    public static int getTotalPlayers() {
   	Table pokerTable;
   	int totalPlayers = 0;
    	for(int t = 0; t < pokerTables.length; t++) {
            if(pokerTables[t] == null) continue;

            pokerTable = pokerTables[t];

            //Close table if no players left on it.
            totalPlayers += pokerTable.getPlayerCount();
    	}
    	return totalPlayers;
    }

public String oldMsg = "";

    // ** Poker Game Timer **
    public void startPokerGameLoop() {
        gameLoop = new TimerTask() {
            public void run() {
                Table pokerTable;

                for(int t = 0; t < pokerTables.length; t++) {
                    if(pokerTables[t] == null) continue;

                    pokerTable = pokerTables[t];

                    //Close table if no players left on it.
                    if(pokerTable.getPlayerCount() == 0) {
                        //Clear all Lvz for the specific table.
                        pokerTables[t] = null;
                        continue;
                    }
                    
                    //Processes the minigame.
                    if(pokerTable.containsMultipleWinners()) {
		        zombieMiniGame.processMiniGame();
		    	break;
		    }
		    
		    //Update the Jackpot every 10 seconds, 100 ms x 100 cycles
		    if((jackpotCycles++ % 100) == 0)
		    	updateRoyalFlushJackpot();

                    //Disconnected players countdown and set as leavingPlayer.
                    String pokerPlayerName;
                    long disconnectTime;

                    for(Map.Entry<String, Long> entry : pokerTable.getDisconnectedList().entrySet()) {
                        pokerPlayerName = entry.getKey();
                        disconnectTime = entry.getValue();

                        //If 60 seconds passed and player is still on disconnected list, time to remove him from table.
                        if((System.currentTimeMillis() - disconnectTime) >= Tools.TimeInMillis.SECOND * 60) {
                            pokerTable.removeDisconnectedPlayer(pokerPlayerName);
                            PokerPlayer pokerPlayer = pokerTable.getPokerPlayerByName(pokerPlayerName);
                            if(pokerPlayer != null)
                                pokerPlayer.setLeaving(true);
                            else
                                pokerTable.removePlayer(pokerPlayerName); //should never go here anyways.
                            m_botAction.sendSmartPrivateMessage(pokerPlayerName, "You have been placed to be removed from table #" + (t+1) + ".");
                        }
                    }

                    //Update each newly joined player on table. Draw his Lvz Name/Money
                    //Player won't be able to play as a new player, until hand is completed.
                    for(PokerPlayer player : pokerTable.getNewPlayers()) {
                        drawPlayerNamesOnTable(player, t);
			drawPlayerMoneyOnTable(player, t);
                        System.out.println("Draw!");
                        player.setNewPlayer(false);
                    }
		
                    pokerTable.run(); //processes the table in whatever state it's in.
                    
                    TableEvent tableEvent = pokerTable.getCurrentTableEvent();
                    if(tableEvent == null) continue;
                    
                    if(!oldMsg.equals(tableEvent.getCurrentGameState().toString())) {
                    	System.out.println("Game State = " + tableEvent.getCurrentGameState());
                    	oldMsg = tableEvent.getCurrentGameState().toString();
		    }
		    
                    //The main table LVZ controller.
                    switch (tableEvent.getCurrentGameState()) {
                    case WAITING_FOR_PLAYERS:
                        //This is before a new game begins, this is where you remove leaving players.
                        clearLeavingPlayersOnTable(t);
                        break;
                    case STARTING_GAME:
                        //This is where you draw the moving dealer chip
                        resetTable(t);
                        drawDealerChipOnTable(t);
                        m_botAction.sendUnfilteredTargetTeamMessage(t+1, (String)tableEvent.getParameterAt(0));
                        break;
                    case ROTATE_ACTOR_SMALL_BLIND:
                    	//Blinker lvz animation to blink the player's seat here, maybe alert sound here too.
                    	drawBlinkerOnTable(t);
                    	break;
                    case POST_SMALL_BLIND:
                    	//Draw Post Small Blind, update the actor's money
                    	drawPlayerMoneyOnTable(pokerTable.getActor(), t);
                    	m_botAction.sendUnfilteredTargetTeamMessage(t+1, (String)tableEvent.getParameterAt(0), TextToSpeech.SOUNDS.PLACE_CHIPS_ON_TABLE.id);
                    	ChipFactory.drawChips(t, pokerTable.getActor());
                    	break;
                    case ROTATE_ACTOR_BIG_BLIND:
                    	//Blinker lvz animation to blink the player's seat here, maybe alert sound here too.
                    	drawBlinkerOnTable(t);
                    	break;
                    case POST_BIG_BLIND:
                    	//Do stuff like animation lvz to blink the player's seat here and alert sound.
                    	drawPlayerMoneyOnTable(pokerTable.getActor(), t);
                    	m_botAction.sendUnfilteredTargetTeamMessage(t+1, (String)tableEvent.getParameterAt(0), TextToSpeech.SOUNDS.PLACE_CHIPS_ON_TABLE.id);
                    	ChipFactory.drawChips(t, pokerTable.getActor());
                    	break;
                    case DEAL_HOLE_CARDS:
                        //This is where you draw all the active player's hand cards. [not table community cards]
                        drawPlayerCardsOnTable(t);
                        System.out.println("Cards Dealt!");
                        m_botAction.sendUnfilteredTargetTeamMessage(t+1, (String)tableEvent.getParameterAt(0));
                        break;
                    case DEAL_COMMUNITY_CARDS:
                    	//This is where you draw the middle 3 (Flop), 4 (Turn), or 5 (River) cards on the table.
                    	drawCommunityCardsOnTable(t);
                    	ChipFactory.drawChips(t, null); //draws the money on table.
                    	System.out.println("Community Cards Dealt!");
                    	m_botAction.sendUnfilteredTargetTeamMessage(t+1, (String)tableEvent.getParameterAt(0));
                    	break;
                    case ROTATE_ACTOR:
                    	//Do stuff like animation lvz to blink the player's seat here and alert sound.
                    	drawBlinkerOnTable(t);
                    	m_botAction.sendUnfilteredTargetTeamMessage(t+1, (String)tableEvent.getParameterAt(0));
                    	break;        
                    case WAIT_PLAYER_ACTION:
                    	//This wouldn't work right if I used a event based pattern, 
                    	//The warnPlayerOnTable has to be constantly called.
                        //Checks player's turn to act timer. To do sound effects, and blickers
                        if(!pokerTable.canAutoFinish()) {
	                        setPlayerShootHelpOptions(t);
	                        warnPlayerOnTable(t);
                        }
                        break;
                    case CHECK_PLAYER_ACTION:
                    	for(Object obj : tableEvent.getParameters()) {
                    		if(obj instanceof String) { //This is where all the messages about what each player does comes in.
                    			m_botAction.sendUnfilteredTargetTeamMessage(t+1, (String)obj, TextToSpeech.SOUNDS.PLACE_CHIPS_ON_TABLE.id);
                    		} else if(obj instanceof PokerPlayer) { //Check for Last player on table, instant win, update money on table.
                    			drawPlayerMoneyOnTable((PokerPlayer)obj, t);
                    			ChipFactory.drawChips(t, (PokerPlayer)obj);
                    		}
                    	}
                    	break;
                    case SHOWDOWN:
                    	//This is where the messages about who won, 
                    	//what cards to show, how they win etc comes in.
                    	//Contains multiple winners.
                    	for(Object obj : tableEvent.getParameters()) {
                    		if(obj instanceof String) { //This is where all the messages about what each player does comes in.
                    			m_botAction.sendUnfilteredTargetTeamMessage(t+1, (String)obj);
                    		} else if(obj instanceof PokerPlayer) { //Each of the winners on the table, update money on table.
                    			drawPlayerMoneyOnTable((PokerPlayer)obj, t);
                    			ChipFactory.drawChips(t, (PokerPlayer)obj);
                    		} else if(obj instanceof Object[]) {
                    			Object[] argsValues = (Object[]) obj;
                    			if(argsValues[0] instanceof Boolean && argsValues[1] instanceof PokerPlayer)    
                    				playerShowCardsOnTable((PokerPlayer)argsValues[1], t);
                    		} else if(obj instanceof Integer) {	//Zombie game detection to start game is here
                    			if(zombieMiniGame.getMiniGameState() == -1) {
                    				zombieMiniGame.startMiniGame(pokerTables, pokerTable.getMultipleWinnerNames(), (Integer)obj);
                    			}
                    		}
                    	}
                    	break;
                    case GAME_OVER:
                    	//Do something when it's game over.
                    	m_botAction.sendUnfilteredTargetTeamMessage(t+1, "Game over!, New hand is starting in " + Constants.PAUSE_SECONDS_UNTIL_NEW_GAME + " seconds!");
                    	break;
                    }
                }
            };
        };
        m_botAction.scheduleTaskAtFixedRate(gameLoop, 0, 100);
    }
}