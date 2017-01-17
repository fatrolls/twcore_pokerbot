package twcore.bots.pokerbot.actions;

import static twcore.bots.pokerbot.util.PokerUtils.formatMoney;

/**
 * The action of placing a bet.
 *
 * @author Oscar Stigter
 */
public class BetAction extends Action {

    /**
     * Constructor.
     *
     * @param amount
     *            The amount to bet.
     */
    public BetAction(int amount) {
        super("Bet", "bets", amount);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
//formatMoney(
        return String.format(getVerb() + " %s", formatMoney(getAmount()));
    }

}
