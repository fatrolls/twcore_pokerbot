package twcore.bots.pokerbot.actions;

/**
 * The action of checking.
 *
 * @author Oscar Stigter
 */
public class CheckAction extends Action {

    /**
     * Constructor.
     */
    /* package */ CheckAction() {
        super("Check", "checks");
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getVerb();
    }
}
