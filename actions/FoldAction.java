package twcore.bots.pokerbot.actions;

/**
 * The action of folding the current hand.
 *
 * @author Oscar Stigter
 */
public class FoldAction extends Action {

    /**
     * Constructor.
     */
    /* package */ FoldAction() {
        super("Fold", "folds");
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getVerb();
    }
}
