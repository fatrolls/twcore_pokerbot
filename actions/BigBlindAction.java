package twcore.bots.pokerbot.actions;

/**
 * The action of posting the big blind.
 *
 * @author Oscar Stigter
 */
public class BigBlindAction extends Action {

    /**
     * Constructor.
     */
    /* package */ BigBlindAction() {
        super("Big blind", "posts the big blind");
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getVerb();
    }
}
