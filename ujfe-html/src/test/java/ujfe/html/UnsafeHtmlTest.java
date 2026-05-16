package ujfe.html;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ujfe.html.UI.p;
import static ujfe.html.UI.unsafeHtml;

final class UnsafeHtmlTest {
    @Test
    void normalTextEscapesScriptContentByDefault() {
        assertEquals("<p>&lt;script&gt;</p>", p("<script>").render());
    }

    @Test
    void unsafeHtmlRendersTrustedRawHtmlWithoutEscaping() {
        assertEquals("<strong>x</strong>", unsafeHtml("<strong>x</strong>").render());
        assertEquals("<p>Trusted HTML</p>", UnsafeHtml.of("<p>Trusted HTML</p>").render());
    }

    @Test
    void unsafeHtmlRendersDangerousScriptAsExpectedBehavior() {
        assertEquals("<script>alert(1)</script>", unsafeHtml("<script>alert(1)</script>").render());
    }

    @Test
    void publicApiNameContainsUnsafe() throws Exception {
        Method helper = UI.class.getMethod("unsafeHtml", String.class);

        assertTrue(helper.getName().toLowerCase(Locale.ROOT).contains("unsafe"));
        assertTrue(UnsafeHtml.class.getSimpleName().toLowerCase(Locale.ROOT).contains("unsafe"));
    }
}
