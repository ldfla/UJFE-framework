package ujfe.live;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StaticAssetHandlerTest {
    @Test
    void detectsCommonStaticAssetPathsWithoutMatchingInternalEndpoints() {
        assertTrue(StaticAssetHandler.isStaticAssetPath("/poster.png"));
        assertTrue(StaticAssetHandler.isStaticAssetPath("/demo.mp4"));
        assertTrue(StaticAssetHandler.isStaticAssetPath("/audio.mp3"));
        assertTrue(StaticAssetHandler.isStaticAssetPath("/assets/app.css"));
        assertTrue(StaticAssetHandler.isStaticAssetPath("/static/font.woff2"));
        assertFalse(StaticAssetHandler.isStaticAssetPath("/_ujfe/client.js"));
        assertFalse(StaticAssetHandler.isStaticAssetPath("/docs"));
    }

    @Test
    void rejectsPathTraversalAndUnsafeAssetPaths() {
        assertTrue(StaticAssetHandler.isUnsafePath("/assets/../secret.txt"));
        assertTrue(StaticAssetHandler.isUnsafePath("/assets/%2e%2e/secret.txt"));
        assertTrue(StaticAssetHandler.isUnsafePath("/assets/%252e%252e/secret.txt"));
        assertTrue(StaticAssetHandler.isUnsafePath("/assets\\secret.txt"));
        assertFalse(StaticAssetHandler.isUnsafePath("/assets/poster.png"));
    }

    @Test
    void resolvesSafeContentTypesForDocumentedAssetExtensions() {
        assertEquals("image/png", StaticAssetHandler.contentType("/poster.png"));
        assertEquals("video/mp4", StaticAssetHandler.contentType("/demo.mp4"));
        assertEquals("audio/mpeg", StaticAssetHandler.contentType("/audio.mp3"));
        assertEquals("text/css; charset=utf-8", StaticAssetHandler.contentType("/assets/app.css"));
        assertEquals("application/octet-stream", StaticAssetHandler.contentType("/download.bin"));
    }
}
