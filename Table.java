package twcore.bots.pokerbot;

import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import twcore.core.util.Tools;
import twcore.core.util.Vectoid; //HashMap with indexOf.
import twcore.bots.pokerbot.actions.*;
import static twcore.bots.pokerbot.util.PokerUtils.formatMoney;

/**
 * Limit Texas Hold'em poker table. <br />
 * <br />
 *
 * This class forms the heart of the poker engine. It controls the game flow for a single poker table.
 *
 * @author Oscar Stigter
 */
public class Table {
    /** In fixed-limit games, the maximum number of raises per betting round. */
    private int MAX_RAISES;
    
    /** BotAction to get bot's name */
    private final pokerbot m_pokerbot;
    
    /** Table type (poker variant). */
    private final TableType tableType;

    /** The size of the big blind. */
    private final int bigBlind;
    
    /** The players at the table. */
    private final VectoidReUse<String, PokerPlayer> players;

    /** The active players in the current hand. */
    private final List<PokerPlayer> activePlayers;

    /** New players at the table list (constantly cleared) and returned. */
    private final List<PokerPlayer> newPlayers;

    /** Players that either lagged out or left the arena while being seated. */
    private final Map<String, Long> disconnectedPlayers;

    /** The deck of cards. */
    private final Deck deck;

    /** The community cards on the table. */
    private final List<Card> communityCards;

    /** Whats currently happening and what will happen next on this table */
    private TableEvent tableEvent;
        
    /** Whats the current table action [Flop, Turn, River and Showdown] */
    private PokerTableAction curTableAction;

    /** What will happen on the next table action [Flop, Turn, River and Showdown] */
    private PokerTableAction nextTableAction;

    /** The current dealer position. */
    private int dealerPosition;

    /** The current dealer. */
    private PokerPlayer dealer;

    /** The position of the acting player. */
    private int actorPosition;

    /** Number of players left to Act in a betting round. */
    private int playersToAct;

    /** The acting player. */
    private PokerPlayer actor;

    /** The acting player's first ship rotation (beginning of bet/raise gauge) */
    private byte actorGaugeInitialShipRotation;

    /** The acting player's current ship rotation. */
    private byte actorGaugeValue;
    
    /** Time in milliseconds since actor's turn started */
    private long actTime;

    /** The minimum bet in the current hand. */
    private int minBet;

    /** The current bet in the current hand. */
    private int bet;

    /** All pots in the current hand (main pot and any side pots). */
    private final List<Pot> pots;

    /** The player who bet or raised last (aggressor). */
    private PokerPlayer lastBettor;

    /** Number of raises in the current betting round. */
    private int raises;

    /** Pause moment previous milliseconds long, wait until new starting new game */
    private long previousPauseMillis;
   
    /** Multiple winners name for zombies minigame */
    private List<String> multipleWinners;
    
    /**
     * Constructor.
     *
     * @param m_pokerbot
     *            PokerBot used to access everything,
     * BotAction, PubBuxManager, LvzManager (maybe not this)
     * @param type
     *            The TableType [Fixed Limit or No Limit]
     * @param bigBlind
     *            The size of the big blind.
     */
    public Table(pokerbot m_pokerbot, TableType type, int bigBlind) {
    	this.m_pokerbot = m_pokerbot;
        this.tableType = type;
        this.bigBlind = bigBlind;
        players = new VectoidReUse<String, PokerPlayer>();
        activePlayers = new ArrayList<PokerPlayer>();
        newPlayers = new ArrayList<PokerPlayer>();
        disconnectedPlayers = new ConcurrentHashMap<String, Long>();
        deck = new Deck();
        communityCards = new ArrayList<Card>();
        pots = new ArrayList<Pot>();

        tableEvent = new TableEvent(PokerTableGameState.WAITING_FOR_PLAYERS);
        MAX_RAISES = 3;
        
        dealerPosition = -1;
        actorPosition = -1;
        multipleWinners = new ArrayList<String>();
    }

    /**
     * Returns the table type of this table.
     *
     * @return String
     *            The Table Type in String format.
     */
    public String getTableTypeName() {
        return tableType.getName();
    }

    /**
     * Returns the number of players on this table.
     *
     * @return int
     *            The Table's player count.
     */
    public int getPlayerCount() {
        //Ignore null's.
        int playerCount = 0;
        for (PokerPlayer player : players.values()) {
            if(player == null) continue;
            playerCount++;
        }
        //If player is deleted the size will still never be 0 (since null's are inserted in place).
        //the 0 is only used upon creation of the table, so it won't be deleted by table cleaner.
        return (players.size() == 0) ? -1 : playerCount;
    }

    /**
     * Returns the blinds print out.
     *
     * @return String
     *            The Blinds in format [$Small,XXX,XXX/$Big,XXX,XXX] Blinds.
     */
    public String getBlinds() {
        return formatMoney(bigBlind/2)+"/"+formatMoney(bigBlind);
    }
    /**
     * Returns the Small Blind value.
     *
     * @return int
     *            The Big Blind value.
     */
    public int getSmallBlind() {
        return (bigBlind/2);
    }
    /**
     * Returns the Big Blind value.
     *
     * @return int
     *            The Big Blind value.
     */
    public int getBigBlind() {
        return bigBlind;
    }
    
    /**
      *Returns the Minimum Bet in the current hand.
      *
      *@return int
     *            The current minimum Bet value.
      */
    public int getMinimumBet() {
    	return minBet;
    }
    
    /**
     * Get number of Raises for fixed games only.
     *
     *@return String
     *		Number of MAX_RAISES for Fixed Limit, otherwise a double dash instead.
     */
    public String getMaxRaises() {
    	return (tableType == TableType.FIXED_LIMIT) ? String.format("%02d", MAX_RAISES) : "--";
    }
    
    /**
     * Set number of Raises for fixed games only.
     *
     *@param int
     *		Max raises for fixed games.
     */
    public void setMaxRaises(int MAX_RAISES) {
    	this.MAX_RAISES = MAX_RAISES;
    }

    /**
     * Adds a player.
     *
     * @param player
     *            The player.
     * @return int
     *            The index where the player is seated, -1 if no seats available.
     */
    public int addPlayer(PokerPlayer player) {
        //size could be 10, with 9 nulls, so add players even at size == 10
        if(!players.containsValue(player) && players.size() <= 10)
            players.put(player.getName(), player);
        return players.indexOfKey(player.getName());
    }

    /**
    * Same as addPlayer, except it doesn't attempt to add a new player.
    *
    * @param name
    *            The player's name.
    *
    * @return int
    *            The index where the player is seated, -1 if not seated on table.
    */
    public int getPlayerIndex(String name) {
        return players.indexOfKey(name);
    }

    /**
    * Gets a PokerPlayer by their name.
    *
    * @param name
    *            The player's name.
    *
    * @return PokerPlayer
    *            Gets the PokerPlayer class based on player's name.
    */
    public PokerPlayer getPokerPlayerByName(String name) {
        return players.get(name);
    }

    /**
    *Removes the player from the table.
    *
    * @param player
    *		The player to remove from table.
    */
    public void removePlayer(PokerPlayer player) {
        players.remove(player.getName());
    }

    /**
    *Removes the player from the table by name.
    *
    * @param playerName
    *		The player's name to remove from table.
    */
    public void removePlayer(String playerName) {
        players.remove(playerName);
    }

    /**
    * Checks if player exists on table by player's name.
    *
    * @param name
    *		The player's name.
    * @return boolean
    *            True value to indicate if player is part of the table, otherwise false.
    */
    public boolean hasPlayerByName(String name) {
        return players.containsKey(name);
    }

    /**
    * Adds a player to the list of disconnected players.
    *
    * @param name
    *		The player's name to add to disconnected players list.
    */
    public void addDisconnectedPlayer(String name) {
        disconnectedPlayers.put(name, System.currentTimeMillis());
    }

    /**
    * Removes the player from the disconnected players list.
    *
    * @param name
    *		The player's name to remove from disconnected players list.
    */
    public void removeDisconnectedPlayer(String name) {
        disconnectedPlayers.remove(name);
    }

    /**
    * Returns Map of Player name's that disconnected with their start disconnect milliseconds.
    *
    * @return Map of String to Long
    *            Contains the list of players which are disconnected from table.
    */
    public Map<String, Long> getDisconnectedList() {
        return disconnectedPlayers;
    }

    /**
    * Returns if player is on the disconnected list.
    *
    * @return boolean
    *            True value to indicate if player's name is found on disconnected players list, otherwise false.
    */
    public boolean isDisconnectedPlayer(String name) {
        return disconnectedPlayers.containsKey(name);
    }

    /**
    * Gets the current table game state.
    *
    * @return TableEvent
    *            Returns what's happening currently on table.
    *  TableEvent is a combination of TableGameState and 
    * a List of Parameters associated to it.
    */
    public TableEvent getCurrentTableEvent() {
        return tableEvent;
    }

    /**
     * This contains all the newly joined players which need to be updated on table.
     *
     * @return
     *            A New List of New PokerPlayer player's.
     */
    public List<PokerPlayer> getNewPlayers() {
        newPlayers.clear();
        for (PokerPlayer player : players.values()) {
            if(player == null) continue;
            if(player.isNewPlayer())
                newPlayers.add(player);
        }
        return newPlayers;
    }

    /**
    * Returns a list of the active players.
    *
    * @return
    *            List of players sitting on table.
    */
    public List<PokerPlayer> getPlayers() {
        return new ArrayList<PokerPlayer>(players.values());
    }

    /**
    * Returns a list of the active players.
    *
    * @return
    *            List of players currently playing on table.
    */
    public List<PokerPlayer> getActivePlayers() {
        return activePlayers;
    }

    /**
      * Gets the dealer player
      *
      * @return PokerPlayer
      *            Gets the dealer of the table.
      */
    public PokerPlayer getDealer() {
        return dealer;
    }
    
    /**
      * Gets the community Cards. [Flop 3, Turn 1, River 1]
      *
      *@return List of Cards
      *		This may contain 3,4 or 5 cards that are shown at middle of the table.
      */
    public List<Card> getCommunityCards() {
    	return communityCards;
    }
    
    /**
      * Is the current actor the player you are checking
      *
      * @param player
      *		The player.
      * @return boolean
      *            True value to indicate if it's the player's turn to act, otherwise false.
      */
    public boolean isActor(PokerPlayer player) {
        return (actor != null) && actor.equals(player);
    }

    /**
      * Gets the actor player
      *
      * @return PokerPlayer
      *            The current actor of the table, if any.
      */
    public PokerPlayer getActor() {
        return actor;
    }

    /**
      * Gets the actor's time to act in milliseconds countdown.
      *
      * @return long
      *            The start time in milliseconds since, actor's turn began.
      */
    public long getActTime() {
        return actTime;
    }
    
    /**
      * When a warning is reset, 
      * the actTime should be reset to grant the player 20 more seconds.
      */
    public void resetActTime() {
    	actTime = System.currentTimeMillis();
    }
    
    /**
      * Get's Actor's Gauge Initial Ship Rotation
      * @return byte
      *            The Ship rotation 0-39 indicating where gauge began at.
      *            -1 means not set, (Allows you to reset the Gauge).
      */
    public byte getActorGaugeInitialShipRotation() {
    	return actorGaugeInitialShipRotation;
    }
    
    /**
      * Set's Actor's Gauge Initial Ship Rotation
      * @param byte
      *            The Ship rotation 0-39 indicating where gauge should begin at.
      *            -1 means not set, (Allows you to reset the Gauge).
      */
    public void setActorGaugeInitialShipRotation(byte actorGaugeInitialShipRotation) {
    	this.actorGaugeInitialShipRotation = actorGaugeInitialShipRotation;
    }
    
    /**
      * Get's Actor's Gauge Value
      * @return byte
      *            The Ship rotation 0-10 [11 possible values] where gauge is at right now.
      */
    public byte getActorGaugeValue() {
    	return actorGaugeValue;
    }
    
    /**
      * Set's Actor's Gauge Value
      * @param byte
      *            The Ship rotation 0-10 [11 possible values] where gauge is at right now.
      */
    public void setActorGaugeValue(byte actorGaugeValue) {
    	this.actorGaugeValue = actorGaugeValue;
    }
        
    public void modifyPubBux(String name, int amount) {
    	//Couldn't access the PubBuxManager or pokerbot, then Game is cancelled.
        //Unfortunately no refunds, but this should never happen, unless bad programming.
	      if(m_pokerbot != null && m_pokerbot.getPubBuxManager() != null) {
	      		m_pokerbot.getPubBuxManager().modifyPubBuxForPlayerName(name, amount);
	      } else {
	    		tableEvent.setNextGameState(PokerTableGameState.GAME_OVER);
	    		if(m_pokerbot != null && m_pokerbot.m_botAction != null) {
	    			m_pokerbot.m_botAction.sendArenaMessage("Couldn't issue pubbux to " + name + " with amount " + formatMoney(amount));
				m_pokerbot.m_botAction.sendArenaMessage("Game has been cancelled, this is a bug!");
	    		}
	    }
    }
    
    public void modifyRakePubBux(int amount) {
	      if(m_pokerbot != null && m_pokerbot.getPubBuxManager() != null) {
	      		m_pokerbot.getPubBuxManager().addPubBuxToRake(amount);
	      } else {
	    		tableEvent.setNextGameState(PokerTableGameState.GAME_OVER);
	    		if(m_pokerbot != null && m_pokerbot.m_botAction != null) {
	    			m_pokerbot.m_botAction.sendArenaMessage("Couldn't add pubbux to rake with amount " + formatMoney(amount));
				m_pokerbot.m_botAction.sendArenaMessage("Game has been cancelled, this is a bug!");
	    		}
	    }
    }

    /**
     * Query table game, Each table has one of these.
     * If atleast 2 players are not new players and have enough cash.
     * Then it will begin the game for this table.
     */
    public void run() {
        switch (tableEvent.getNextGameState()) {
        case WAITING_FOR_PLAYERS:
            //Checks if game can start.
            int noOfActivePlayers = 0;
            for (PokerPlayer player : players.values()) {
                if(player == null) continue;
                if (player.getCash() >= bigBlind && !player.isNewPlayer() && !player.isLeaving()) {
                    noOfActivePlayers++;
                    if (noOfActivePlayers > 1) {
                        //Indicates the game is about to begin.
                        tableEvent.setNextGameState(PokerTableGameState.STARTING_GAME);
                        return;
                    }
                }
            }
            break;
        case STARTING_GAME:
            //Game begins, dealer is selected.
            startNewHand();
            
           //Rotate actor for Small blind posting, if more then 2 players.
            if (activePlayers.size() > 2) {
                tableEvent.setNextGameState(PokerTableGameState.ROTATE_ACTOR_SMALL_BLIND);
                break;
            }
            // Next event will Post a Small Blind
            tableEvent.setNextGameState(PokerTableGameState.POST_SMALL_BLIND);
            break;
        case ROTATE_ACTOR_SMALL_BLIND:
            //Rotated to next player to post Small Blind.
            rotateActor(true);
            // Next event will Post a Small blind.
            tableEvent.setNextGameState(PokerTableGameState.POST_SMALL_BLIND);
            break;
        case POST_SMALL_BLIND:
            // This current Actor just posted a Small blind.
            postSmallBlind();
            // Next event will rotate actor for big blind.
            tableEvent.setNextGameState(PokerTableGameState.ROTATE_ACTOR_BIG_BLIND);
            break;
        case ROTATE_ACTOR_BIG_BLIND:
            // Rotated to next player to post Big Blind
            rotateActor(true);
            //Next event will Post a Big blind.
            tableEvent.setNextGameState(PokerTableGameState.POST_BIG_BLIND);
            break;
        case POST_BIG_BLIND:
            // This current Actor just posted a Big blind.
            postBigBlind();
            //Set's table's current action to pre-flop.
            nextTableAction = PokerTableAction.PRE_FLOP_CARDS;
            // Next event will give every player cards, i've moved it to doTableAction();
            tableEvent.setNextGameState(PokerTableGameState.DO_TABLE_ACTION);
            break;
        case DO_TABLE_ACTION:
            //Performs one of the table actions, Pre-Flop, Flop, Turn, River and Showdown.
            //This command sets the next gameState inside it.
            doTableAction();
            break;
        case DEAL_HOLE_CARDS:
            // Gives all active players their 2 cards.
            dealHoleCards();
            tableEvent.setNextGameState(PokerTableGameState.DO_TABLE_ACTION);
            break;
        case DEAL_COMMUNITY_CARDS:
            // Sets the table's middle cards to the proper ones.
            dealCommunityCards();
            tableEvent.setNextGameState(PokerTableGameState.DO_TABLE_ACTION);
            break;
        case ROTATE_ACTOR:
            //When the Actor is rotated it goes here.
            //Except when it's rotated automatically by Small/Big Blinds.
            rotateActor(false);
            tableEvent.setNextGameState(PokerTableGameState.WAIT_PLAYER_ACTION);
            break;
        case WAIT_PLAYER_ACTION:
            //This shouldn't really set any gameState unless actor isAllIn or isLeaving or exceeded the warnings of the Table.
            //Or New feature can auto play to finish the game without human interaction.
	    if(canAutoFinish())
                tableEvent.setNextGameState(PokerTableGameState.DO_TABLE_ACTION);
            else if(actor != null && (actor.isAllIn() || actor.isLeaving() || actor.getWarning() == WarningType.WARNING_EXCEEDED))
                tableEvent.setNextGameState(PokerTableGameState.CHECK_PLAYER_ACTION);
            break;
        case CHECK_PLAYER_ACTION:
            //Run's the next actor's action.
            doPlayerAction();
            //If no next player to do any action, then betting round is over.
            if(playersToAct > 0)
            	tableEvent.setNextGameState(PokerTableGameState.ROTATE_ACTOR);
            else
            	tableEvent.setNextGameState(PokerTableGameState.END_BETTING_ROUND);
            break;
        case START_BETTING_ROUND:
            //Starts a new betting round.
            startBettingRound();
            tableEvent.setNextGameState(PokerTableGameState.ROTATE_ACTOR);
            break;
        case END_BETTING_ROUND:
            //Ends the current betting round.
            endBettingRound();
            tableEvent.setNextGameState(PokerTableGameState.DO_TABLE_ACTION);
            break;
        case SHOWDOWN:
            // Performs a SHOWDOWN
            doShowdown();
            tableEvent.setNextGameState(PokerTableGameState.GAME_OVER);
            break;
        case GAME_OVER:
            //This event is also used as a one time trigger to draw stuff like Game Over
            //Since the PAUSE_PERIOD will get called many times it's not good to use.
            
            //Sets the table to pause period, where it waits a moment before starting new game.
            previousPauseMillis = System.currentTimeMillis();
            tableEvent.setNextGameState(PokerTableGameState.PAUSE_PERIOD);
            break;
        case PAUSE_PERIOD:
            //Waits a moment amount of seconds before starting a new game.
            if((System.currentTimeMillis() - previousPauseMillis) >= (Tools.TimeInMillis.SECOND * Constants.PAUSE_SECONDS_UNTIL_NEW_GAME))
                tableEvent.setNextGameState(PokerTableGameState.WAITING_FOR_PLAYERS);
            break;
        }
    }

    /**
       * Performs a table action.
       * [Pre-Flop, Flop, Turn, River, Showdown]
       * This deals with setting the minimum bet, resetting current bet.
       * Starting a betting round and showing showdown.
       */
    private void doTableAction() {
       //If 1 player left or no players left, then it's instantly a game over.
       if(activePlayers.size() <= 1) {
       		tableEvent.setNextGameState(PokerTableGameState.GAME_OVER);
       		return;
       }
       
       //Next table action becomes the Current table action.
       curTableAction = nextTableAction;
    
        switch(curTableAction) {
        /* ----------------------- PRE_FLOP -----------------------*/
        case PRE_FLOP_CARDS:
            tableEvent.setNextGameState(PokerTableGameState.DEAL_HOLE_CARDS);
            nextTableAction = PokerTableAction.PRE_FLOP_BETTING_ROUND;
            break;
        case PRE_FLOP_BETTING_ROUND:
            tableEvent.setNextGameState(PokerTableGameState.START_BETTING_ROUND);
            nextTableAction = PokerTableAction.FLOP_CARDS;
            break;
        /* ----------------------- FLOP -----------------------*/
        case FLOP_CARDS:
            tableEvent.setNextGameState(PokerTableGameState.DEAL_COMMUNITY_CARDS);
            nextTableAction = PokerTableAction.FLOP_BETTING_ROUND;
            break;
        case FLOP_BETTING_ROUND:
            bet = 0;
            minBet = bigBlind;
            tableEvent.setNextGameState(PokerTableGameState.START_BETTING_ROUND);
            nextTableAction = PokerTableAction.TURN_CARD;
            break;
        /* ----------------------- TURN -----------------------*/
        case TURN_CARD:
            tableEvent.setNextGameState(PokerTableGameState.DEAL_COMMUNITY_CARDS);
            nextTableAction = PokerTableAction.TURN_BETTING_ROUND;
            break;
        case TURN_BETTING_ROUND:
            bet = 0;
            minBet = (tableType == TableType.FIXED_LIMIT) ? (2 * bigBlind) : bigBlind;
            tableEvent.setNextGameState(PokerTableGameState.START_BETTING_ROUND);
            nextTableAction = PokerTableAction.RIVER_CARD;
            break;
        /* ----------------------- RIVER -----------------------*/
        case RIVER_CARD:
            tableEvent.setNextGameState(PokerTableGameState.DEAL_COMMUNITY_CARDS);
            nextTableAction = PokerTableAction.RIVER_BETTING_ROUND;
            break;
        case RIVER_BETTING_ROUND:
            bet = 0;
            minBet = (tableType == TableType.FIXED_LIMIT) ? (2 * bigBlind) : bigBlind;
            tableEvent.setNextGameState(PokerTableGameState.START_BETTING_ROUND);
            nextTableAction = PokerTableAction.SHOWDOWN;
            break;
        /* ----------------------- SHOWDOWN -----------------------*/
        case SHOWDOWN:
            bet = 0;
            minBet = bigBlind;
            tableEvent.setNextGameState(PokerTableGameState.SHOWDOWN);
            nextTableAction = PokerTableAction.NOTHING;
            break;
        }
    }

    /**
     * Resets the previous game (if any) for a new hand.
     */
    private void startNewHand() {
        // Clear the board
        communityCards.clear();
        pots.clear();

        // Determine the active players.
        activePlayers.clear();
        for (PokerPlayer player : players.values()) {
            if(player == null) continue;
            player.resetHand();
            // Player must be able to afford at least the big blind.
            // Also player must not be a new player in order to play.
            if (player.getCash() >= bigBlind && !player.isNewPlayer())
                activePlayers.add(player);
        }

        // Rotate the dealer button to the right position of the old dealer.
        dealerPosition = (dealerPosition + 1) % activePlayers.size();
        dealer = activePlayers.get(dealerPosition);

        // Shuffle the deck.
        deck.shuffle();

        // Determine the first player to act, which is the dealer.
        actorPosition = dealerPosition;
        actor = activePlayers.get(actorPosition);

        // Set the initial bet to the big blind.
        minBet = bigBlind;
        bet = minBet;
	tableEvent.addParameter("-------------------------------------------------------------------");
        tableEvent.addParameter("New hand is starting, " + dealer + " is the dealer");
    }

    /**
     * Rotates the position of the player in turn (the actor).
     *
     * @param boolean
     *            This is used for the TableEvent Parameter team messages.
     * If true then it's automated rotation (player can't do anything about it).
     */
    private void rotateActor(boolean isAutomatedRotation) {
    	if(actor != null && m_pokerbot != null && m_pokerbot.getLvzManager() != null) {
                //Clear screen help screen (if any) on previous actor.
	        if(m_pokerbot.getLvzManager().isLvzByUniqueIdShown(Constants.ID_PLAYER_HELP, actor.getName()))
	           m_pokerbot.getLvzManager().clearLvzByUniqueId(Constants.ID_PLAYER_HELP, actor.getName());
		//Clear screen gauge (if any) on previous actor.
		if(m_pokerbot.getLvzManager().isLvzByUniqueIdShown(Constants.ID_PLAYER_GAUGE, actor.getName()))
		   m_pokerbot.getLvzManager().clearLvzByUniqueId(Constants.ID_PLAYER_GAUGE, actor.getName());
    	}
    	
    	//Set Next Actor.
        actorPosition = (actorPosition + 1) % activePlayers.size();
        actor = activePlayers.get(actorPosition);
        
        //Automated rotation at the moment are only the blinds.
        //In the future it may include powerup's to force players to do things.
        if(isAutomatedRotation) {
        	tableEvent.addParameter(actor + " will be forced to do the next action.");
        	return;
        }
        
        //Only move below this line if it's not automated rotation.        
        tableEvent.addParameter("It's " + actor + " turn to do something.");
        //Set the first actor's act countdown.
        actTime = System.currentTimeMillis();
        //Reset the actor's bet/raise gauge inital ship rotation, -1 is default for null
        setActorGaugeInitialShipRotation((byte)-1);
    }

    /**
     * Posts the small blind.
     */
    private void postSmallBlind() {
        final int smallBlind = bigBlind / 2;
        actor.postSmallBlind(smallBlind);
        contributePot(smallBlind);
        //Subtract's the smallBlind amount from PubBux.
        modifyPubBux(actor.getName(), -smallBlind);

        //Update Current GameState Parameters.
        tableEvent.addParameter(actor + " " + Action.SMALL_BLIND + " " + formatMoney(smallBlind) + ".");
    }

    /**
     * Posts the big blind.
     */
    private void postBigBlind() {
        actor.postBigBlind(bigBlind);
        contributePot(bigBlind);
        //Subtract's the bigBlind amount from PubBux.
        modifyPubBux(actor.getName(), -bigBlind);
        //Update Current GameState Parameters.
        tableEvent.addParameter(actor + " " + Action.BIG_BLIND + " " + formatMoney(bigBlind) + ".");
    }

    /**
     * Actor acted (Called by PokerBot) to advance next player action.
     */
    public void setActorActed() {
        if(tableEvent.getCurrentGameState() != PokerTableGameState.WAIT_PLAYER_ACTION) return;
        if(actor == null) return;      
        if(actor.getAction() == null) return;
        
        // Verify chosen action to guard against broken clients (accidental or on purpose).
        Set<Action> allowedActions = getAllowedActions(actor);
        Action action = actor.getAction();
        // Allowed Action can be Raise(0), but player's Action can be Raise(1000), no match.
	if (!allowedActions.contains(action)) {
	        if (!(action instanceof BetAction && allowedActions.contains(Action.BET)) && !(action instanceof RaiseAction && allowedActions.contains(Action.RAISE)))
	            return;
	}
	
        tableEvent.setNextGameState(PokerTableGameState.CHECK_PLAYER_ACTION);
    }
	
    public boolean canAutoFinish() {
    	//If everyone is either isAllIn and/or can CHECK, 
    	//then can finish the current hand automatically.
    	
    	int noAllIns = 0;
    	int noChecks = 0;
    	
	for (PokerPlayer player : activePlayers) {
		if(player == null) continue;
	    	Set<Action> allowedActions = getAllowedActions(player);
	    	
	    	if(player.isAllIn()) noAllIns++;
	    	if(allowedActions.contains(Action.CHECK)) noChecks++;
	}
	    
	    //If everybody is all in then auto play.
	    if(noAllIns == activePlayers.size()) 
	    	return true;
	    //If everybody but 1 player is all in and that one player can check then auto play.
	    else if((noAllIns + 1) == activePlayers.size() && noChecks > noAllIns) 
	    	return true;
	    //Can't auto play yet.
	    else 
	    	return false;
    }
    /**
     * Deals the Hole Cards.
     */
    private void dealHoleCards() {
        for (PokerPlayer player : activePlayers)
            player.setCards(deck.deal(2));

        tableEvent.addParameter(dealer + " deals the hole cards.");
    }

    /**
     * Deals a number of community cards.
     * This is used by run(), it's automatic based on tableAction.
     */
    private void dealCommunityCards() {
        switch(curTableAction) {
        /* ----------------------- FLOP -----------------------*/
        case FLOP_CARDS:
            dealCommunityCards("Flop", 3);
            break;
        /* ----------------------- TURN -----------------------*/
        case TURN_CARD:
            dealCommunityCards("Turn", 1);
            break;
        /* ----------------------- RIVER -----------------------*/
        case RIVER_CARD:
            dealCommunityCards("River", 1);
            break;
        }
    }

    /**
     * Deals a number of community cards.
     *
     * @param phaseName
     *            The name of the phase.
     * @param noOfCards
     *            The number of cards to deal.
     */
    private void dealCommunityCards(String phaseName, int noOfCards) {
        for (int i = 0; i < noOfCards; i++) {
            communityCards.add(deck.deal());
            System.out.format("[CHEAT] %s's cards:\t%s\n", phaseName, communityCards.get(communityCards.size()-1));
        }
        
        tableEvent.addParameter(dealer + " deals the " + phaseName + ".");
    }

    /**
      * Begins the betting round.
      */
    private void startBettingRound() {
        // Determine the number of active players.
        playersToAct = activePlayers.size();
        // Determine the initial player and bet size.
        if (communityCards.size() == 0) {
            // Pre-Flop; player left of big blind starts, bet is the big blind.
            bet = bigBlind;
        } else {
            // Otherwise, player left of dealer starts, no initial bet.
            //If dealerPosition is in the first position, the next actor will be last player.
            actorPosition = (dealerPosition - 1) < 0 ? playersToAct - 1 : dealerPosition - 1;
            bet = 0;
        }

        lastBettor = null;
        raises = 0;
    }

    /**
      * Reset's player's bets.
      */
    private void endBettingRound() {
        for (PokerPlayer player : activePlayers)
            player.resetBet();
    }
    
    /**
      * Performs a actors action.
      */
    private void doPlayerAction() {
        //If there is any players left to act left.
        if(playersToAct > 0) {
            //Current actor's Action.
            Action action = actor.getAction();
            // Otherwise allow client to act.
            Set<Action> allowedActions = getAllowedActions(actor);

            if (actor.isAllIn()) {
                // Player is all-in, so must check, does nothing pretty much.
                action = Action.CHECK;
            } else if(actor.isLeaving() || actor.getWarning() == WarningType.WARNING_EXCEEDED) {
                // Player is leaving, and isn't all-in, so must auto fold if checking is not possible
                if(allowedActions.contains(Action.CHECK))
                    action = Action.CHECK; //Check if you can check for free.
                else
                    action = Action.FOLD; //Auto fold.
            }
	    
	    actor.setWarning(null); //Remove the warning
	    
            // Verify chosen action to guard against broken clients (accidental or on purpose).
            // allowedActions will not contain a action which differs in amount
            // Ex. allowedActions = [Raise(0), calls, folds], but Raise(1000) isn't on the list.
            //TODO:
            // I was thinking of just overriding the public boolean equals(Object obj) with each action that has amount.
            //To work by instanceof only, but then you wouldn't be able to compare similar Actions with different amounts.
            //At the moment I don't compare Actions by amounts so it's possible to go with that fix.
            
            if (!allowedActions.contains(action)) {
                if (!(action instanceof BetAction && allowedActions.contains(Action.BET)) && !(action instanceof RaiseAction && allowedActions.contains(Action.RAISE))) {
                    //Update Current GameState Parameters.
                    tableEvent.addParameter("Player: " + actor + " acted with illegal " + action + " action.");
                    //throw new IllegalStateException(String.format("Player '%s' acted with illegal %s action", actor, action));
                }
            }
            
            playersToAct--;
            
            //2 step action, this Action can become either Raise/Bet/Call max cash.
            if(action == Action.ALL_IN) { //Works only for NO_LIMIT games.
                if ((bet - actor.getBet()) >= actor.getCash()) //Can do [CALL ALL-IN]
                	action = Action.CALL;
            	else if(allowedActions.contains(Action.BET)) //Can do [BET ALL-IN]
            		action = new BetAction(actor.getCash());
            	else if(allowedActions.contains(Action.RAISE)) //Can do [RAISE ALL-IN]
            		action = new RaiseAction(actor.getCash());
            }
            
            if (action == Action.CHECK) {
                // Do nothing.
                //Update Current GameState Parameters.
        	tableEvent.addParameter(actor + " " + Action.CHECK + ".");
            } else if (action == Action.CALL) {
                int betIncrement = bet - actor.getBet();
                if (betIncrement > actor.getCash())
                    betIncrement = actor.getCash();
                actor.payCash(betIncrement);
                actor.setBet(actor.getBet() + betIncrement);
                contributePot(betIncrement);
                //Subtract's the betIncrement amount from PubBux.
                modifyPubBux(actor.getName(), -betIncrement);
        	//Update Current GameState Parameters.
        	if(actor.isAllIn())
        		tableEvent.addParameter(actor + " " + Action.CALL + " " + formatMoney(betIncrement) + " and " + Action.ALL_IN + ".");
        	else
 			tableEvent.addParameter(actor + " " + Action.CALL + " " + formatMoney(betIncrement) + ".");
        	tableEvent.addParameter(actor); //player to re-draw money lvz.
            } else if (action instanceof BetAction) {
                int amount = (tableType == TableType.FIXED_LIMIT) ? minBet : action.getAmount();
                if(action.getAmount() != amount) action = new BetAction(amount);
	        if (amount < minBet && amount < actor.getCash()) {
                    amount = actor.getCash();		
                    //throw new IllegalStateException("Illegal client action: bet less than minimum bet!");
                }
                if (amount > actor.getCash() && actor.getCash() >= minBet) {		
                    amount = actor.getCash();		
                    //throw new IllegalStateException("Illegal client action: bet more cash than you own!");
                }
                bet = amount;
                minBet = amount;
                int betIncrement = bet - actor.getBet();
                if (betIncrement > actor.getCash())
                    betIncrement = actor.getCash();
                actor.setBet(bet);
                actor.payCash(betIncrement);
                contributePot(betIncrement);
                lastBettor = actor;
                // All players get another turn.
                playersToAct = (tableType == TableType.FIXED_LIMIT && activePlayers.size() == 2) ? activePlayers.size() : (activePlayers.size() - 1);
                //Subtract's the betIncrement amount from PubBux.
                modifyPubBux(actor.getName(), -betIncrement);
        	//Update Current GameState Parameters.
        	if(actor.isAllIn())
        		tableEvent.addParameter(actor + " " + action +" and " + Action.ALL_IN + ".");
        	else
        		tableEvent.addParameter(actor + " " + action + ".");
        	tableEvent.addParameter(actor); //player to re-draw money lvz.
            } else if (action instanceof RaiseAction) {
                int amount = (tableType == TableType.FIXED_LIMIT) ? minBet : action.getAmount();
                if(action.getAmount() != amount) action = new RaiseAction(amount);
 		if (amount < minBet && amount < actor.getCash()) {
                    amount = actor.getCash();		
                    //throw new IllegalStateException("Illegal client action: raise less than minimum bet!");
                }
                if (amount > actor.getCash() && actor.getCash() >= minBet) {		
                    amount = actor.getCash();		
                    //throw new IllegalStateException("Illegal client action: raise more cash than you own!");
                }
                bet += amount;
                minBet = amount;
                int betIncrement = bet - actor.getBet();
                if (betIncrement > actor.getCash())
                    betIncrement = actor.getCash();
                actor.setBet(bet);
                actor.payCash(betIncrement);
                contributePot(betIncrement);
                lastBettor = actor;
                raises++;
                // All players get another turn.
                if (tableType == TableType.FIXED_LIMIT && (raises < MAX_RAISES || activePlayers.size() == 2))
                    playersToAct = activePlayers.size();
                else // Max. number of raises reached; other players get one more turn.
                    playersToAct = activePlayers.size() - 1;
                //Subtract's the betIncrement amount from PubBux.
                modifyPubBux(actor.getName(), -betIncrement);
        	//Update Current GameState Parameters.
        	if(actor.isAllIn())
        		tableEvent.addParameter(actor + " " + action + " and " + Action.ALL_IN + ".");
        	else
        		tableEvent.addParameter(actor + " " + action + ".");
        	tableEvent.addParameter(actor); //player to re-draw money lvz.
            } else if (action == Action.FOLD) {
                actor.setCards(null);
                activePlayers.remove(actor);
                //Go to player before me after removing me, so next Actor Rotation is properly handled.
                actorPosition--;
                //Update Current GameState Parameters.
                tableEvent.addParameter(actor + " " + Action.FOLD + ".");
            } else {
                // Programming error, should never happen.
                tableEvent.addParameter(actor + " performed an invalid action. (Report this!)");
                //throw new IllegalStateException("Invalid action: " + action);
            }
            
            // Only one player left, so he wins the entire pot, 
            //Minus the Rake from Constants.rakePercentage paid to PokerBot
            //To be used as a Royal Flush Jackpot 
            if (activePlayers.size() == 1) {
                PokerPlayer winner = activePlayers.get(0);
                int totalPotAmount = getTotalPot();
                int rakeDeductionAmount = (int)(totalPotAmount*(Constants.RAKE_DEDUCTION_PERCENTAGE/100.0f));
                totalPotAmount -= rakeDeductionAmount;
                
                winner.win(totalPotAmount);
                //Increase player's arena score, so they can buy stuff for the mini game!.
            	//PM winner.getName() > *points potShare
                playersToAct = 0;
                
                //Increase the amount won to PubBux.
                modifyPubBux(winner.getName(), totalPotAmount);
        	if(rakeDeductionAmount > 0)
        		modifyRakePubBux(rakeDeductionAmount);
                //Update Current GameState Parameters.
                tableEvent.addParameter(winner + " wins " + formatMoney(totalPotAmount) + " by default." + ((rakeDeductionAmount > 0) ? " (Rake -" + formatMoney(rakeDeductionAmount) + ")" : ""));
            	tableEvent.addParameter(winner); //player to re-draw money lvz.
            }

            //Clears the last actor's Action.
            actor.setAction(null);
        }
    }

    /**
     * Returns the allowed actions of a specific player.
     *
     * @param player
     *            The player.
     *
     * @return The allowed actions.
     */
    public Set<Action> getAllowedActions(PokerPlayer player) {
        Set<Action> actions = new HashSet<Action>();
        if (player.isAllIn()) {
            actions.add(Action.CHECK);
        } else {
            int actorBet = actor.getBet();
            if (bet == 0) {
                actions.add(Action.CHECK);
                if (tableType == TableType.NO_LIMIT || raises < MAX_RAISES || activePlayers.size() == 2)
                    actions.add(Action.BET);
            } else {
                if (actorBet < bet) {
                    actions.add(Action.CALL);
                    if (tableType == TableType.NO_LIMIT || raises < MAX_RAISES || activePlayers.size() == 2)
                        actions.add(Action.RAISE);
                } else {
                    actions.add(Action.CHECK);
                    if (tableType == TableType.NO_LIMIT || raises < MAX_RAISES || activePlayers.size() == 2)
                        actions.add(Action.RAISE);
                }
            }
            //Only No Limit games you can go ALL-IN on.
            if(tableType == TableType.NO_LIMIT)
            	actions.add(Action.ALL_IN);
            //You can always Fold, as long as you are not ALL-IN already.
            actions.add(Action.FOLD);
        }
        return actions;
    }

    /**
     * Contributes to the pot.
     *
     * @param amount
     *            The amount to contribute.
     */
    private void contributePot(int amount) {
        for (Pot pot : pots) {
            if (!pot.hasContributer(actor)) {
                int potBet = pot.getBet();
                if (amount >= potBet) {
                    // Regular call, bet or raise.
                    pot.addContributer(actor);
                    amount -= pot.getBet();
                } else {
                    // Partial call (all-in); redistribute pots.
                    pots.add(pot.split(actor, amount));
                    amount = 0;
                }
            }
            if (amount <= 0)
                break;
        }
        if (amount > 0) {
            Pot pot = new Pot(amount);
            pot.addContributer(actor);
            pots.add(pot);
        }
    }
    
    /**
     * Gets a list of hand values of all activePlayers, ignoring one player.
     *
     * @param PokerPlayer
     *            The player which is ignored.
     * @return int Array
     *            A list of hand values of nearly all activePlayers, except the ignored player.
     */
    private List<Integer> getAllHandValues(PokerPlayer ignorePlayer) {
    	// Sort players by hand value (highest to lowest).
        List<Integer> handValues = new ArrayList<Integer>();

        for (PokerPlayer player : activePlayers) {
            //Skip this player.
            if(player.equals(ignorePlayer)) continue;
            // Create a hand with the community cards and the player's hole cards.
            Hand hand = new Hand(communityCards);
            hand.addCards(player.getCards());
            // Gets the hand value
            HandValue handValue = new HandValue(hand);

            handValues.add(handValue.getValue());
        }
        
        return handValues;
    }
    
    /**
     * Performs the showdown.
     */
    private void doShowdown() {
        /*
                   System.out.println("\n[DEBUG] Pots:");
                   for (Pot pot : pots) {
                      System.out.format("  %s\n", pot);
                  }
                  System.out.format("[DEBUG]  Total: %d\n", getTotalPot());
        */
        // Determine show order; start with all-in players...
        List<PokerPlayer> showingPlayers = new ArrayList<PokerPlayer>();
        for (Pot pot : pots) {
            for (PokerPlayer contributor : pot.getContributors()) {
                if (!showingPlayers.contains(contributor) && contributor.isAllIn()) {
                    showingPlayers.add(contributor);
                }
            }
        }
        // ...then last player to bet or raise (aggressor)...
        if (lastBettor != null) {
            if (!showingPlayers.contains(lastBettor)) {
                showingPlayers.add(lastBettor);
            }
        }
        //...and finally the remaining players, starting left of the button.
        int pos = (dealerPosition + 1) % activePlayers.size();
        while (showingPlayers.size() < activePlayers.size()) {
            PokerPlayer player = activePlayers.get(pos);
            if (!showingPlayers.contains(player)) {
                showingPlayers.add(player);
            }
            pos = (pos + 1) % activePlayers.size();
        }

        // Players automatically show or fold in order.
        boolean firstToShow = true;
        int bestHandValue = -1;
        for (PokerPlayer playerToShow : showingPlayers) {
            Hand hand = new Hand(communityCards);
            hand.addCards(playerToShow.getCards());
            HandValue handValue = new HandValue(hand);
            boolean doShow = Constants.ALWAYS_CALL_SHOWDOWN;
            if (!doShow) { //Now it's players choice.
                if (playerToShow.isAllIn() || playerToShow.showCards()) {
                    // All-in players must always show.
                    doShow = true;
                    firstToShow = false;
                } else if (firstToShow) {
                    // First player must always show.
                    doShow = true;
                    bestHandValue = handValue.getValue();
                    firstToShow = false;
                } else {
                    // Remaining players only show when having a chance to win.
                    if (handValue.getValue() >= bestHandValue) {
                        doShow = true;
                        bestHandValue = handValue.getValue();
                    }
                }
            }
            if (doShow) {
                // Show hand.
                //Update Current GameState Parameters.
                tableEvent.addParameter(new Object[] {true, playerToShow});
                tableEvent.addParameter(playerToShow + " has " + handValue.getDescription(getAllHandValues(playerToShow)) + ".");
            } else {
                // Fold.
                playerToShow.setCards(null);
                activePlayers.remove(playerToShow);
                //Update Current GameState Parameters.
                tableEvent.addParameter(playerToShow + " decided not to show his cards!");
            }
        }
        
        // Sort players by hand value (highest to lowest).
        List<PokerPlayer> royalFlushPlayers = new ArrayList<PokerPlayer>();
        Map<HandValue, List<PokerPlayer>> rankedPlayers = new TreeMap<HandValue, List<PokerPlayer>>();
        for (PokerPlayer player : activePlayers) {
            // Create a hand with the community cards and the player's hole cards.
            Hand hand = new Hand(communityCards);
            hand.addCards(player.getCards());
            // Store the player together with other players with the same hand value.
            HandValue handValue = new HandValue(hand);
            //Store all the players who have a Royal Flush, for the Royal Flush Jackpot!
            if(handValue.getType() == HandValueType.ROYAL_FLUSH)
            	royalFlushPlayers.add(player);
//            System.out.format("[DEBUG] %s: %s\n", player, handValue);
            List<PokerPlayer> playerList = rankedPlayers.get(handValue);
            if (playerList == null)
                playerList = new ArrayList<PokerPlayer>();
            playerList.add(player);
            rankedPlayers.put(handValue, playerList);
        }
        
        // Per rank (single or multiple winners), calculate pot distribution.
        int totalPot = getTotalPot();
        Map<PokerPlayer, Integer> potDivision = new HashMap<PokerPlayer, Integer>();
        for (HandValue handValue : rankedPlayers.keySet()) {
            List<PokerPlayer> winners = rankedPlayers.get(handValue);
            for (Pot pot : pots) {
                // Determine how many winners share this pot.
                int noOfWinnersInPot = 0;
                for (PokerPlayer winner : winners) {
                    if (pot.hasContributer(winner)) {
                        noOfWinnersInPot++;
                    }
                }
                if (noOfWinnersInPot > 0) {
                    // Divide pot over winners.
                    int potShare = pot.getValue() / noOfWinnersInPot;
                    for (PokerPlayer winner : winners) {
                        if (pot.hasContributer(winner)) {
                            Integer oldShare = potDivision.get(winner);
                            if (oldShare != null) {
                                potDivision.put(winner, oldShare + potShare);
                            } else {
                                potDivision.put(winner, potShare);
                            }
                        }
                    }
                    
                    // Determine if we have any odd chips left in the pot.
                    int oddChips = pot.getValue() % noOfWinnersInPot;
                    if (oddChips > 0) {
                        // Divide odd chips over winners, starting left of the dealer.
                        pos = dealerPosition;
                        while (oddChips > 0) {
                            pos = (pos + 1) % activePlayers.size();
                            PokerPlayer winner = activePlayers.get(pos);
                            Integer oldShare = potDivision.get(winner);
                            if (oldShare != null) {
                                potDivision.put(winner, oldShare + 1);
//                                System.out.format("[DEBUG] %s receives an odd chip from the pot.\n", winner);
                                oddChips--;
                            }
                        }

                    }
                    pot.clear();
                }
            }
        }
	
	//Multiple winners detected, minigame time!
	if(Constants.IsZombieGameEnabled 
	&& pokerbot.getTotalPlayers() > potDivision.size() 
	&& potDivision.size() > 1) {
	         for (PokerPlayer winner : potDivision.keySet()) {
	        	if(winner != null) {
				multipleWinners.add(winner.toString());
			}
		}
		//Update Current GameState Parameters.
	        tableEvent.addParameter("Multiple winners detected!, Minigame of zombies will begin shortly!");
	        tableEvent.addParameter(totalPot); //how much they will fight for.
		return;
	} else {
	        int totalWon = 0;
	        for (PokerPlayer winner : potDivision.keySet()) {
	            int potShare = potDivision.get(winner);
	            int rakeDeductionAmount = (int)(potShare*(Constants.RAKE_DEDUCTION_PERCENTAGE/100.0f));
	            potShare -= rakeDeductionAmount;
	            
	            winner.win(potShare);
	            //Increase player's arena score, so they can buy stuff for the mini game!.
	            //PM winner.getName() > *points potShare
	            //Increase the amount won to PubBux.
	            modifyPubBux(winner.getName(), potShare);
	
	            if(rakeDeductionAmount > 0)
	            	modifyRakePubBux(rakeDeductionAmount);
	            totalWon += potDivision.get(winner);
	            //Update Current GameState Parameters.
	            tableEvent.addParameter(winner + " wins " + formatMoney(potShare) + "!" + ((rakeDeductionAmount > 0) ? " (Rake -" + formatMoney(rakeDeductionAmount) + ")" : ""));
	            tableEvent.addParameter(winner); //player to re-draw money lvz.
	        }
	
	        // Sanity check.
	        if (totalWon != totalPot) {
	            tableEvent.addParameter("Incorrect pot division! (Screenshot and Report this bug!)");
	            //throw new IllegalStateException("Incorrect pot division!");
	        }
	        
	        //Add Royal Flush Winnings		
	        if(royalFlushPlayers.size() > 0) {		
	        	int royalFlushJackpot = m_pokerbot.getPubBuxManager().getPubBuxForPlayerName(m_pokerbot.m_botAction.getBotName());		
	        	modifyPubBux(m_pokerbot.m_botAction.getBotName(), -royalFlushJackpot);		
	        	m_pokerbot.updateRoyalFlushJackpot();
	        	
			for (PokerPlayer winner : royalFlushPlayers) {		
		            winner.win(royalFlushJackpot);		
		            //Increase player's arena score, so they can buy stuff for the mini game!.		
		            //PM winner.getName() > *points potShare		
		            //Increase the amount won from royal flush to PubBux.		
		            modifyPubBux(winner.getName(), royalFlushJackpot);		
		            //Update Current GameState Parameters.		
		            tableEvent.addParameter(winner + " wins with ROYAL FLUSH !!!, " + formatMoney(royalFlushJackpot) + "!");		
		            tableEvent.addParameter(winner); //player to re-draw money lvz.		
		}		
	    }
        }
    }

    /**
     * Returns the total pot size.
     *
     * @return The total pot size.
     */
    public int getTotalPot() {
        int totalPot = 0;
        for (Pot pot : pots) {
            totalPot += pot.getValue();
        }
        return totalPot;
    }
    
    public boolean containsMultipleWinners() {
    	return multipleWinners.size() > 0;
    }
    
    public String[] getMultipleWinnerNames() {
    	return multipleWinners.toArray(new String[multipleWinners.size()]);
    }
    
    public void resetMultipleWinners() {
    	multipleWinners.clear();
    }
}
