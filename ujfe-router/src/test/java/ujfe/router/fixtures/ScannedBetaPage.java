package ujfe.router.fixtures;

import ujfe.core.Node;
import ujfe.router.Page;

import static ujfe.core.UI.div;

@Page("/beta")
public final class ScannedBetaPage {
    public Node render() {
        return div().child("Beta");
    }
}
