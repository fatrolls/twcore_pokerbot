package twcore.bots.pokerbot.actions;

/**
 * The action of calling a previous bet or raise.
 *
 * @author Oscar Stigter
 */
public class CallAction extends Action {

    /**
     * Constructor.
     */
    /* package */ CallAction() {
        super("Call", "calls");
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getVerb();
    }
}
