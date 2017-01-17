package twcore.bots.pokerbot.actions;

import static twcore.bots.pokerbot.util.PokerUtils.formatMoney;

/**
 * The action of raising a previous bet.
 *
 * @author Oscar Stigter
 */
public class RaiseAction extends Action {

    /**
     * Constructor.
     *
     * @param amount
     *            The amount to raise with.
     */
    public RaiseAction(int amount) {
        super("Raise", "raises", amount);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format(getVerb() + " to %s", formatMoney(getAmount()));
    }

}
