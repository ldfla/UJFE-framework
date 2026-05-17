package ujfe.router.fixtures;

import ujfe.core.Node;

import static ujfe.html.UI.div;

public final class UnannotatedCandidate {
    public Node render() {
        return div().child("Ignored");
    }
}
