package ujfe.router.fixtures;

import ujfe.core.Node;
import ujfe.router.Page;

import static ujfe.html.UI.div;

@Page("/alpha")
public final class ScannedAlphaPage {
    public Node render() {
        return div().child("Alpha");
    }
}
