package ujfe.runtime.action;

/**
 * Contributes nodes to the document {@code <head>} section during
 * document rendering.
 */
@FunctionalInterface
public interface HeadContributionAction {
    void execute(HeadContributionContext context);
}
