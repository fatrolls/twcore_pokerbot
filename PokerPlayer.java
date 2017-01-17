package twcore.bots.pokerbot;

import java.util.List;

import twcore.bots.pokerbot.actions.Action;

/**
 * A Texas Hold'em player. <br />
 * <br />
 *
 * The player's actions are delegated to a {@link Client}, which can be either
 * human-controlled or AI-controlled (bot).
 *
 * @author Oscar Stigter
 */
public class PokerPlayer implements Comparable<PokerPlayer> {

    /** Name. */
    private final String name;

    /** Hand of cards. */
    private final Hand hand;

    /** Current amount of cash. */
    private int cash;

    /** Whether the player has hole cards. */
    private boolean hasCards;
    
    /** Want to show my cards at Show down? */
    private boolean showCards;
    
    /** Current bet. */
    private int bet;

    /** Last action performed. */
    private Action action;

    /** Is new player? */
    private boolean isNewPlayer;

    /** Is player leaving? */
    private boolean isLeaving;

    /** Is player warned for taking too long, or even ignored warning? */
    private WarningType warningType;

    /** Player sit out counter */
    private int sitOutCount;
    
    /**
     * Constructor.
     *
     * @param name
     *            The player's name.
     * @param cash
     *            The player's starting amount of cash.
     */
    public PokerPlayer(String name, int cash) {
        this.name = name;
        this.cash = cash;
        isNewPlayer = true;

        hand = new Hand();

        resetHand();
    }

    /**
     * Prepares the player for another hand.
     */
    public void resetHand() {
        hasCards = false;
        hand.removeAllCards();
        resetBet();
    }

    /**
     * Resets the player's bet.
     */
    public void resetBet() {
        bet = 0;
        action = (hasCards() && cash == 0) ? Action.ALL_IN : null;
    }

    /**
     * Sets the hole cards.
     *
     * @param List of Cards
     */
    public void setCards(List<Card> cards) {
        hand.removeAllCards();
        if (cards != null) {
            if (cards.size() == 2) {
                hand.addCards(cards);
                hasCards = true;
                System.out.format("[CHEAT] %s's cards:\t%s\n", name, hand);
            } else {
                throw new IllegalArgumentException("Invalid number of cards");
            }
        }
    }

    /**
     * Returns whether the player has his hole cards dealt.
     *
     * @return True if the hole cards are dealt, otherwise false.
     */
    public boolean hasCards() {
        return hasCards;
    }

    /**
     * Returns whether the player will or won't show his cards.
     *
     * @return True if the hole cards will be shown, otherwise false.
     * Global Constant ALWAYS_CALL_SHOWDOWN overrides this.
     */
    public boolean showCards() {
        return showCards;
    }

    /**
     * Set whether the player will or won't show his cards.
     *
     * @param boolean
     *		True if the hole cards will be shown, otherwise false.
     * Global Constant ALWAYS_CALL_SHOWDOWN overrides this.
     */
    public void showCards(boolean showCards) {
        this.showCards = showCards;
    }

    /**
     * Returns the player's name.
     *
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the player's current amount of cash.
     *
     * @return The amount of cash.
     */
    public int getCash() {
        return cash;
    }
    
    /**
     * Sets the player's current amount of cash.
     *
     * @param int
     *		Amount of Cash the player has on bankroll (PubBux).
     */
    public void setCash(int cash) {
        this.cash = cash;
    }

    /**
     * Returns is the player a new player.
     *
     * @return Whether or not the player requires a LVZ Update.
     */
    public boolean isNewPlayer() {
        return isNewPlayer;
    }

    /**
     * Sets the player's isNewPlayer flag
     *
     * @param isNewPlayer
     *            Whether or not the player requires a LVZ Update.
     */
    public void setNewPlayer(boolean isNewPlayer) {
        this.isNewPlayer = isNewPlayer;
    }

    /**
     * Indicates whether this player is a leaving player.
     *
     * @return True if leaving, otherwise false.
     */
    public boolean isLeaving() {
        return isLeaving;
    }

    /**
     * Sets the player's leaving flag
     *
     * @param isLeaving
     *            Whether or not the player will leave after game is over.
     */
    public void setLeaving(boolean isLeaving) {
        this.isLeaving = isLeaving;
    }

    /**
     * Indicates how this player has been warned if warned at all.
     *
     * @return WarningType
     *            Either null, WARNED, WARNING_EXCEEDED
     */
    public WarningType getWarning() {
        return warningType;
    }

    /**
     * Sets the player's warned flag.
     *
     * @param warningType
     *            Whether or not the player was warned for taking too long.
     *            Either null, WARNED, WARNING_EXCEEDED
     */
    public void setWarning(WarningType warningType) {
        this.warningType = warningType;
    }

    /**
     * Indicates how much times this player hasn't did anything
     *
     * @return
     *            The count how much times player didn't do anything.
     */
    public int getSitOutCount() {
        return sitOutCount;
    }

    /**
     * Sets the player's sit out count value.
     *
     * @param sitOutCount
     *            Set how much sit outs occurred so far.
     */
    public void setSitOutCount(int sitOutCount) {
        this.sitOutCount = sitOutCount;
    }

    /**
     * Returns the player's current bet.
     *
     * @return The current bet.
     */
    public int getBet() {
        return bet;
    }

    /**
     * Sets the player's current bet.
     *
     * @param bet
     *            The current bet.
     */
    public void setBet(int bet) {
        this.bet = bet;
    }

    /**
     * Returns the player's most recent action.
     *
     * @return The action.
     */
    public Action getAction() {
        return action;
    }

    /**
     * Sets the player's most recent action.
     *
     * @param action
     *            The action.
     */
    public void setAction(Action action) {
        this.action = action;
    }

    /**
     * Indicates whether this player is all-in.
     *
     * @return True if all-in, otherwise false.
     */
    public boolean isAllIn() {
        return hasCards() && (cash == 0);
    }

    /**
     * Returns the player's hole cards.
     *
     * @return The hole cards.
     */
    public Card[] getCards() {
        return hand.getCards();
    }

    /**
     * Posts the small blind.
     *
     * @param blind
     *            The small blind.
     */
    public void postSmallBlind(int blind) {
        action = Action.SMALL_BLIND;
        cash -= blind;
        bet += blind;
    }

    /**
     * Posts the big blinds.
     *
     * @param blind
     *            The big blind.
     */
    public void postBigBlind(int blind) {
        action = Action.BIG_BLIND;
        cash -= blind;
        bet += blind;
    }

    /**
     * Pays an amount of cash.
     *
     * @param amount
     *            The amount of cash to pay.
     */
    public void payCash(int amount) {
        if (amount > cash) {
            //throw new IllegalStateException("Player asked to pay more cash than he owns!");
        }
        cash -= amount;
    }

    /**
     * Wins an amount of money.
     *
     * @param amount
     *            The amount won.
     */
    public void win(int amount) {
        cash += amount;
    }

    /**
     * Returns a clone of this player with only public information.
     *
     * @return The cloned player.
     */
    public PokerPlayer publicClone() {
        PokerPlayer clone = new PokerPlayer(name, cash);
        clone.hasCards = hasCards;
        clone.bet = bet;
        clone.action = action;
        clone.isNewPlayer = isNewPlayer;
        return clone;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PokerPlayer) {
            return ((PokerPlayer) obj).name.equals(name);
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(PokerPlayer player) {
        int thisValue = hashCode();
        int otherValue = player.hashCode();
        if (thisValue < otherValue) {
            return -1;
        } else if (thisValue > otherValue) {
            return 1;
        } else {
            return 0;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
    	//Why exactly do I need a getName() when I have this?
        return name;
    }

}
