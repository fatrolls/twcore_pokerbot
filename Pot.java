package twcore.bots.pokerbot;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A pot to which one or more players have contributed. <br />
 * <br />
 *
 * Each pot has an equal fixed amount for all contributing players. In case of a
 * partial call, bet or raise, the pot must be {@link #split}.
 *
 * @author Oscar Stigter
 */
public class Pot {

    /** Bet for this pot. */
    private int bet;

    /** Contributing players to this pot. */
    public final Set<PokerPlayer> contributors;

    /**
     * Constructor.
     *
     * @param bet
     *            The bet for this pot.
     */
    public Pot(int bet) {
        this.bet = bet;
        contributors = new HashSet<PokerPlayer>();
    }

    /**
     * Returns the bet.
     *
     * @return The bet.
     */
    public int getBet() {
        return bet;
    }

    /**
     * Returns the contributing players.
     *
     * @return The conributing players.
     */
    public Set<PokerPlayer> getContributors() {
        return Collections.unmodifiableSet(contributors);
    }

    /**
     * Adds a contributing player.
     *
     * @param player
     *            The player.
     */
    public void addContributer(PokerPlayer player) {
        contributors.add(player);
    }

    /**
     * Indicates whether a specific player has contributed to this pot.
     *
     * @param player
     *            The player.
     *
     * @return True if the player has contributed, otherwise false.
     */
    public boolean hasContributer(PokerPlayer player) {
        return contributors.contains(player);
    }

    /**
     * Returns the total value of this pot.
     *
     * @return The total value.
     */
    public int getValue() {
        return bet * contributors.size();
    }

    /**
     * In case of a partial call, bet or raise, splits this pot into two pots,
     * with this pot keeping the lower bet and the other pot the remainder.
     *
     * @param player
     *            The player with the partial call, bet or raise.
     * @param partialBet
     *            The amount of the partial bet.
     *
     * @return The other pot, with the remainder.
     */
    public Pot split(PokerPlayer player, int partialBet) {
        Pot pot = new Pot(bet - partialBet);
        for (PokerPlayer contributer : contributors) {
            pot.addContributer(contributer);
        }
        bet = partialBet;
        contributors.add(player);
        return pot;
    }

    /**
     * Clears this pot.
     */
    public void clear() {
        bet = 0;
        contributors.clear();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.valueOf(bet));
        sb.append(": {");
        boolean isFirst = true;
        for (PokerPlayer contributer : contributors) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(", ");
            }
            sb.append(contributer.getName());
        }
        sb.append('}');
        sb.append(" (Total: ");
        sb.append(String.valueOf(getValue()));
        sb.append(')');
        return sb.toString();
    }

}
