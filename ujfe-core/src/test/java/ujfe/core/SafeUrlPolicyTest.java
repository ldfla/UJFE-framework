package ujfe.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static ujfe.core.UI.*;

final class SafeUrlPolicyTest {
    private UrlPolicy savedPolicy;

    @BeforeEach
    void saveDefaultPolicy() {
        savedPolicy = UrlPolicy.getDefault();
    }

    @AfterEach
    void restoreDefaultPolicy() {
        UrlPolicy.setDefault(savedPolicy);
    }

    // --- Allowed URLs under default policy ---

    @Test
    void allowsRelativePath() {
        String html = a("Home").href("/home").render();
        assertTrue(html.contains("href=\"/home\""));
    }

    @Test
    void allowsHttpsUrl() {
        String html = a("Site").href("https://example.com").render();
        assertTrue(html.contains("href=\"https://example.com\""));
    }

    @Test
    void allowsRelativePathWithoutScheme() {
        String html = a("Page").href("page").render();
        assertTrue(html.contains("href=\"page\""));
    }

    @Test
    void allowsCurrentDirectoryRelativePath() {
        String html = a("Here").href("./page").render();
        assertTrue(html.contains("href=\"./page\""));
    }

    @Test
    void allowsParentDirectoryRelativePath() {
        String html = a("Settings").href("../settings").render();
        assertTrue(html.contains("href=\"../settings\""));
    }

    @Test
    void allowsFragmentOnlyReference() {
        String html = a("Section").href("#section").render();
        assertTrue(html.contains("href=\"#section\""));
    }

    @Test
    void allowsEmptyHref() {
        assertDoesNotThrow(() -> a("Empty").href(""));
    }

    @Test
    void allowsDataImageGif() {
        String gif = "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==";
        String html = img().src(gif).render();
        assertTrue(html.contains("src=\"" + gif + "\""));
    }

    @Test
    void allowsDataImagePng() {
        assertDoesNotThrow(() -> img().src("data:image/png;base64,iVBOR"));
    }

    @Test
    void allowsDataImageWebp() {
        assertDoesNotThrow(() -> img().src("data:image/webp;base64,UklGR"));
    }

    @Test
    void allowsDataImageSvgXml() {
        assertDoesNotThrow(() -> img().src("data:image/svg+xml;base64,PHN2Zw=="));
    }

    // --- Blocked URLs under default policy ---

    @Test
    void blocksJavascriptScheme() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> a("Bad").href("javascript:alert(1)"));
        assertTrue(exception.getMessage().contains("javascript"));
    }

    @Test
    void blocksJavascriptSchemeCaseInsensitive() {
        assertThrows(IllegalArgumentException.class,
                () -> a("Bad").href("JavaScript:alert(1)"));
        assertThrows(IllegalArgumentException.class,
                () -> a("Bad").href("JAVASCRIPT:alert(1)"));
    }

    @Test
    void blocksVbscriptScheme() {
        assertThrows(IllegalArgumentException.class,
                () -> a("Bad").href("vbscript:MsgBox(1)"));
    }

    @Test
    void blocksDataTextHtml() {
        assertThrows(IllegalArgumentException.class,
                () -> img().src("data:text/html,<script>alert(1)</script>"));
    }

    @Test
    void blocksDataApplicationJavascript() {
        assertThrows(IllegalArgumentException.class,
                () -> img().src("data:application/javascript,alert(1)"));
    }

    @Test
    void blocksHttpByDefault() {
        assertThrows(IllegalArgumentException.class,
                () -> a("Bad").href("http://example.com"));
    }

    @Test
    void blocksMailtoByDefault() {
        assertThrows(IllegalArgumentException.class,
                () -> a("Bad").href("mailto:user@example.com"));
    }

    @Test
    void blocksTelByDefault() {
        assertThrows(IllegalArgumentException.class,
                () -> a("Bad").href("tel:+1234567890"));
    }

    @Test
    void blocksUnknownScheme() {
        assertThrows(IllegalArgumentException.class,
                () -> a("Bad").href("ftp://files.example.com/data"));
    }

    // --- Configurable policy: http ---

    @Test
    void allowsHttpWhenConfigured() {
        UrlPolicy.setDefault(UrlPolicy.builder().allowHttp().build());
        assertDoesNotThrow(() -> a("Link").href("http://example.com"));
    }

    // --- Configurable policy: mailto ---

    @Test
    void allowsMailtoWhenConfigured() {
        UrlPolicy.setDefault(UrlPolicy.builder().allowMailto().build());
        assertDoesNotThrow(() -> a("Email").href("mailto:user@example.com"));
    }

    // --- Configurable policy: tel ---

    @Test
    void allowsTelWhenConfigured() {
        UrlPolicy.setDefault(UrlPolicy.builder().allowTel().build());
        assertDoesNotThrow(() -> a("Call").href("tel:+1234567890"));
    }

    // --- Configurable policy: combined ---

    @Test
    void allowsMultipleSchemesWhenConfigured() {
        UrlPolicy.setDefault(UrlPolicy.builder()
                .allowHttp()
                .allowMailto()
                .allowTel()
                .build());

        assertDoesNotThrow(() -> a("Link").href("http://example.com"));
        assertDoesNotThrow(() -> a("Email").href("mailto:user@example.com"));
        assertDoesNotThrow(() -> a("Call").href("tel:+1234567890"));
    }

    @Test
    void javascriptAlwaysBlockedEvenWithPermissivePolicy() {
        UrlPolicy.setDefault(UrlPolicy.builder()
                .allowHttp()
                .allowMailto()
                .allowTel()
                .build());

        assertThrows(IllegalArgumentException.class,
                () -> a("Bad").href("javascript:alert(1)"));
    }

    @Test
    void vbscriptAlwaysBlockedEvenWithPermissivePolicy() {
        UrlPolicy.setDefault(UrlPolicy.builder()
                .allowHttp()
                .allowMailto()
                .allowTel()
                .build());

        assertThrows(IllegalArgumentException.class,
                () -> a("Bad").href("vbscript:MsgBox(1)"));
    }

    // --- Configurable policy: data image toggle ---

    @Test
    void blocksDataImageWhenDisabled() {
        UrlPolicy.setDefault(UrlPolicy.builder()
                .allowDataImageUrls(false)
                .build());

        assertThrows(IllegalArgumentException.class,
                () -> img().src("data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw=="));
    }

    // --- Policy builder safety ---

    @Test
    void builderRejectsJavascriptScheme() {
        assertThrows(IllegalArgumentException.class,
                () -> UrlPolicy.builder().allowScheme("javascript"));
    }

    @Test
    void builderRejectsVbscriptScheme() {
        assertThrows(IllegalArgumentException.class,
                () -> UrlPolicy.builder().allowScheme("vbscript"));
    }

    @Test
    void allowsCustomSchemeWhenConfigured() {
        UrlPolicy.setDefault(UrlPolicy.builder()
                .allowScheme("ftp")
                .build());

        assertDoesNotThrow(() -> a("Files").href("ftp://files.example.com/data"));
    }

    // --- URL attributes are consistently sanitized ---

    @Test
    void sanitizesHrefAttribute() {
        assertThrows(IllegalArgumentException.class,
                () -> a("Bad").href("javascript:alert(1)"));
    }

    @Test
    void sanitizesSrcAttribute() {
        assertThrows(IllegalArgumentException.class,
                () -> img().src("javascript:alert(1)"));
    }

    @Test
    void sanitizesActionAttribute() {
        assertThrows(IllegalArgumentException.class,
                () -> form().action("javascript:alert(1)"));
    }

    @Test
    void sanitizesFormactionAttribute() {
        assertThrows(IllegalArgumentException.class,
                () -> button("Go").attr("formaction", "javascript:alert(1)"));
    }

    @Test
    void sanitizesPosterAttribute() {
        assertThrows(IllegalArgumentException.class,
                () -> video().poster("javascript:alert(1)"));
    }

    @Test
    void sanitizesCiteAttribute() {
        assertThrows(IllegalArgumentException.class,
                () -> blockquote().attr("cite", "javascript:alert(1)"));
    }

    @Test
    void sanitizesBackgroundAttribute() {
        assertThrows(IllegalArgumentException.class,
                () -> Element.of("td").attr("background", "javascript:alert(1)"));
    }

    @Test
    void sanitizesDataAttribute() {
        assertThrows(IllegalArgumentException.class,
                () -> Element.of("object").attr("data", "javascript:alert(1)"));
    }

    // --- Direct SafeUrl API with explicit policy ---

    @Test
    void sanitizeWithExplicitPolicy() {
        UrlPolicy permissive = UrlPolicy.builder()
                .allowHttp()
                .allowMailto()
                .allowTel()
                .build();

        assertEquals("http://example.com", SafeUrl.sanitize("http://example.com", permissive));
        assertEquals("mailto:user@example.com", SafeUrl.sanitize("mailto:user@example.com", permissive));
        assertEquals("tel:+1234567890", SafeUrl.sanitize("tel:+1234567890", permissive));
    }

    @Test
    void sanitizeWithExplicitPolicyStillBlocksJavascript() {
        UrlPolicy permissive = UrlPolicy.builder()
                .allowHttp()
                .allowMailto()
                .allowTel()
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> SafeUrl.sanitize("javascript:alert(1)", permissive));
    }

    // --- Data image URL documentation coverage ---

    @Test
    void dataImageMimePrefixValidationIsDocumented() {
        // This test documents the known limitation: MIME prefix validation
        // does not prove that decoded bytes are a valid image. A
        // data:image/gif;base64 URL could contain arbitrary bytes after
        // decoding. Stricter validation (e.g., image-byte inspection) is
        // an explicit future hardening option.
        String fakeImage = "data:image/gif;base64,AAAA";
        assertDoesNotThrow(() -> SafeUrl.sanitize(fakeImage));
    }

    // --- Invalid URL syntax ---

    @Test
    void rejectsInvalidUrlSyntax() {
        assertThrows(IllegalArgumentException.class,
                () -> a("Bad").href("https://example.com/path with spaces"));
    }
}
