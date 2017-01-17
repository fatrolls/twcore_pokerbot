package twcore.bots.pokerbot.actions;

/**
 * The action of going all-in.
 *
 * @author Oscar Stigter
 */
public class AllInAction extends Action {

    /**
     * Constructor.
     */
    /* package */ AllInAction() {
        super("All-in", "goes all-in");
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getVerb();
    }
}
