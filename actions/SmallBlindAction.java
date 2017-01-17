package twcore.bots.pokerbot.actions;

/**
 * The action of posting the small blind.
 *
 * @author Oscar Stigter
 */
public class SmallBlindAction extends Action {

    /**
     * Constructor.
     */
    /* package */ SmallBlindAction() {
        super("Small blind", "posts the small blind");
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getVerb();
    }
}
