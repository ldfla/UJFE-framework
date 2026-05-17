package ujfe.http;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.Test;
import ujfe.core.ClientState;
import ujfe.live.LiveRenderResult;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class UjfeHttpHandlerTest {
    @Test
    void extractsEventIdFromJsonPayload() {
        assertEquals("evt-42", UjfeHttpHandler.extractEventId("{\"eventId\":\"evt-42\"}"));
    }

    @Test
    void rejectsPayloadWithoutEventId() {
        assertThrows(IllegalArgumentException.class, () -> UjfeHttpHandler.extractEventId("{}"));
    }

    @Test
    void parsesCookieHeader() {
        assertEquals(Map.of("ujfe_demo", "ativo", "theme", "dark"),
                UjfeHttpHandler.parseCookies("ujfe_demo=ativo; theme=dark"));
    }

    @Test
    void extractsClientStateFromJsonPayload() {
        ClientState state = UjfeHttpHandler.extractClientState("{"
                + "\"clientState\":{"
                + "\"cookies\":\"ujfe_demo=ativo; theme=dark\","
                + "\"localStorage\":{\"ujfe.theme\":\"dark\",\"escaped\":\"A\\nB\"}"
                + "}"
                + "}");

        assertEquals("ativo", state.cookie("ujfe_demo").orElseThrow());
        assertEquals("dark", state.localStorage("ujfe.theme").orElseThrow());
        assertEquals("A\nB", state.localStorage("escaped").orElseThrow());
    }

    @Test
    void rendersLivePayloadAsJson() {
        String payload = UjfeHttpHandler.livePayload(new LiveRenderResult("<div>\"A\"</div>", ".a{color:\"red\";}"));

        assertEquals("{\"html\":\"<div>\\\"A\\\"</div>\",\"css\":\".a{color:\\\"red\\\";}\"}", payload);
    }

    @Test
    void parsesCssClassesForDevPreview() {
        assertEquals(Set.of("p-10", "gap-10", "bg-primary-200"),
                UjfeHttpHandler.parseCssClasses("p-10   gap-10 bg-primary-200"));
    }

    @Test
    void appliesSecurityHeaders() {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.EMPTY_BUFFER
        );

        UjfeHttpHandler.applySecurityHeaders(response);

        assertEquals("nosniff", response.headers().get("X-Content-Type-Options"));
        assertEquals("DENY", response.headers().get("X-Frame-Options"));
        assertEquals("no-referrer", response.headers().get("Referrer-Policy"));
        assertEquals("geolocation=(), microphone=(), camera=()", response.headers().get("Permissions-Policy"));
        String csp = response.headers().get("Content-Security-Policy");
        assertTrue(csp.contains("default-src 'self'"));
        assertTrue(csp.contains("object-src 'none'"));
        assertTrue(csp.contains("frame-ancestors 'none'"));
    }
}
