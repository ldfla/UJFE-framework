package ujfe.router;

import org.junit.jupiter.api.Test;
import ujfe.core.Node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ujfe.html.UI.div;
import static ujfe.html.UI.h1;

final class RouterTest {
    @Test
    void registersAnnotatedPageAndRendersIt() {
        Router router = new Router().register(new AboutPage());

        assertTrue(router.resolve("/about").isPresent());
        Node node = new PageRenderer().render(router.resolve("/about").orElseThrow());

        assertEquals("<div><h1>About</h1></div>", node.render());
    }

    @Page("/about")
    public static final class AboutPage {
        public Node render() {
            return div().child(h1("About"));
        }
    }
}
