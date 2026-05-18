package ujfe.servlet.fixtures;

import ujfe.core.Node;
import ujfe.router.Page;

import static ujfe.core.UI.h1;

@Page("/scanned")
public final class ServletScannedPage {
    public Node render() {
        return h1("Scanned servlet page");
    }
}
