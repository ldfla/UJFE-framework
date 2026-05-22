package ujfe.live;

import org.junit.jupiter.api.Test;
import ujfe.core.RenderMode;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class LiveHttpCacheTest {
    @Test
    void internalAssetHeadersSupportConditionalRequests() {
        LiveHttpCache.CacheHeaders headers = LiveHttpCache.clientScriptHeaders();

        assertEquals("public, max-age=300, must-revalidate", headers.cacheControl());
        assertTrue(headers.etag()
            .startsWith("\""));
        assertTrue(headers.matches(headers.etag()));
        assertTrue(headers.matches("W/" + headers.etag()));
        assertTrue(headers.matches("\"other\", " + headers.etag()));
        assertFalse(headers.matches("\"other\""));
    }

    @Test
    void cssHeadersArePrivateAndContentBased() {
        LiveHttpCache.CacheHeaders first = LiveHttpCache.cssHeaders(".a{padding:1rem;}");
        LiveHttpCache.CacheHeaders second = LiveHttpCache.cssHeaders(".a{padding:1rem;}");
        LiveHttpCache.CacheHeaders changed = LiveHttpCache.cssHeaders(".a{padding:2rem;}");

        assertEquals("private, no-cache", first.cacheControl());
        assertEquals(first.etag(), second.etag());
        assertNotEquals(first.etag(), changed.etag());
    }

    @Test
    void routeHeadersFollowRenderModeMetadata() {
        Map<String, String> headers = LiveHttpCache.routeHeaders(
            RenderMode.staticShell(Duration.ofSeconds(30))
                .revalidateOn("dashboard.updated")
        );

        assertEquals("private, max-age=30", headers.get(LiveHttpCache.CACHE_CONTROL));
        assertEquals("dashboard.updated", headers.get(LiveHttpCache.REVALIDATE_ON));
        assertEquals("no-store", LiveHttpCache.routeHeaders(RenderMode.dynamic())
            .get(LiveHttpCache.CACHE_CONTROL));
    }
}
