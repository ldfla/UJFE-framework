package ujfe.spring;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import ujfe.core.Node;
import ujfe.live.LiveSession;
import ujfe.router.Page;
import ujfe.router.Router;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ujfe.html.UI.button;
import static ujfe.html.UI.div;
import static ujfe.html.UI.p;

final class UjfeSpringHandlerTest {
    @Test
    void mapsOnlyUjfeRoutesAndInternalEndpoints() throws Exception {
        Router router = new Router().register(new HomePage());
        UjfeSpringHandler handler = new UjfeSpringHandler(new LiveSession(router));
        UjfeSpringHandlerMapping mapping = new UjfeSpringHandlerMapping(router, handler);

        assertNotNull(mapping.getHandler(new MockHttpServletRequest("GET", "/")));
        assertNotNull(mapping.getHandler(new MockHttpServletRequest("GET", "/_ujfe/client.js")));
        assertNull(mapping.getHandler(new MockHttpServletRequest("GET", "/assets/app.css")));
        assertNull(mapping.getHandler(new MockHttpServletRequest("POST", "/")));
    }

    @Test
    void rendersUjfePageThroughServletResponse() throws Exception {
        UjfeSpringHandler handler = new UjfeSpringHandler(new LiveSession(new Router().register(new HomePage())));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader("Cookie", "ujfe_demo=ativo");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handleRequest(request, response);

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentType().startsWith("text/html"));
        assertTrue(response.getContentAsString().contains("Home"));
        assertTrue(response.getContentAsString().contains("Cookie: ativo"));
        assertTrue(response.getHeader("Content-Security-Policy").contains("default-src 'self'"));
    }

    @Test
    void servesLiveClientScriptOnSameMvcPipeline() throws Exception {
        UjfeSpringHandler handler = new UjfeSpringHandler(new LiveSession(new Router().register(new HomePage())));
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handleRequest(new MockHttpServletRequest("GET", "/_ujfe/client.js"), response);

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/javascript"));
        assertTrue(response.getContentAsString().contains("/_ujfe/event"));
    }

    @Page("/")
    public static final class HomePage {
        public Node render() {
            return div()
                    .child(p("Home"))
                    .child(p(() -> "Cookie: " + ujfe.core.Ujfe.cookie("ujfe_demo").orElse("missing")))
                    .child(button("Click").onClick(() -> {
                    }));
        }
    }
}
