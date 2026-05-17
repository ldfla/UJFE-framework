package ujfe.live;

import org.junit.jupiter.api.Test;
import ujfe.core.Node;
import ujfe.router.Page;
import ujfe.router.Router;

import static org.junit.jupiter.api.Assertions.*;
import static ujfe.html.UI.p;

final class LiveHttpSecurityTest {
    @Test
    void defaultSecurityHeadersArePresent() {
        var headers = LiveHttpSecurity.securityHeaders();

        assertEquals("nosniff", headers.get(SecurityHeadersConfig.X_CONTENT_TYPE_OPTIONS));
        assertEquals("DENY", headers.get(SecurityHeadersConfig.X_FRAME_OPTIONS));
        assertEquals("strict-origin-when-cross-origin", headers.get(SecurityHeadersConfig.REFERRER_POLICY));
        assertEquals("geolocation=(), microphone=(), camera=()", headers.get(SecurityHeadersConfig.PERMISSIONS_POLICY));
        assertTrue(headers.get(SecurityHeadersConfig.CONTENT_SECURITY_POLICY).contains("default-src 'self'"));
        assertTrue(headers.get(SecurityHeadersConfig.CONTENT_SECURITY_POLICY).contains("script-src 'self'"));
    }

    @Test
    void customSecurityHeadersOverrideDefaults() {
        SecurityHeadersConfig config = SecurityHeadersConfig.builder()
                .header(SecurityHeadersConfig.REFERRER_POLICY, "same-origin")
                .header(SecurityHeadersConfig.CONTENT_SECURITY_POLICY, "default-src 'self'; img-src https:")
                .remove(SecurityHeadersConfig.X_FRAME_OPTIONS)
                .build();

        var headers = LiveHttpSecurity.securityHeaders(config);

        assertEquals("same-origin", headers.get(SecurityHeadersConfig.REFERRER_POLICY));
        assertEquals("default-src 'self'; img-src https:", headers.get(SecurityHeadersConfig.CONTENT_SECURITY_POLICY));
        assertEquals("nosniff", headers.get(SecurityHeadersConfig.X_CONTENT_TYPE_OPTIONS));
        assertFalse(headers.containsKey(SecurityHeadersConfig.X_FRAME_OPTIONS));
    }

    @Test
    void liveSessionConfigCanOverrideSingleSecurityHeader() {
        LiveSessionConfig config = LiveSessionConfig.builder()
                .securityHeader(SecurityHeadersConfig.REFERRER_POLICY, "same-origin")
                .build();

        assertEquals("same-origin", config.securityHeaders().get(SecurityHeadersConfig.REFERRER_POLICY));
        assertEquals("nosniff", config.securityHeaders().get(SecurityHeadersConfig.X_CONTENT_TYPE_OPTIONS));
    }

    @Test
    void securityHeadersCanBeDisabledExplicitly() {
        assertTrue(LiveHttpSecurity.securityHeaders(SecurityHeadersConfig.disabled()).isEmpty());
        assertTrue(LiveSessionConfig.builder()
                .disableSecurityHeaders()
                .build()
                .securityHeaders()
                .isEmpty());
    }

    @Test
    void cspIsCompatibleWithExternalClientScript() {
        String csp = SecurityHeadersConfig.defaults().headers().get(SecurityHeadersConfig.CONTENT_SECURITY_POLICY);
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            String document = session.renderDocument("/", ujfe.core.ClientState.empty());

            assertTrue(csp.contains("script-src 'self'"));
            assertTrue(document.contains("<script src=\"/_ujfe/client.js\"></script>"));
            assertFalse(document.contains("<script>"));
        }
    }

    @Test
    void testValidateCsrf_MissingToken_ThrowsException() {
        LiveSessionConfig config = LiveSessionConfig.defaults();
        String sessionToken = LiveHttpSecurity.generateCsrfToken();
        LiveHttpRequestMetadata metadata = new LiveHttpRequestMetadata(null, null, null, null);

        LiveCsrfException exception = assertThrows(LiveCsrfException.class, 
            () -> LiveHttpSecurity.validateCsrf(config, sessionToken, metadata));

        assertEquals(LiveHttpFailureCategory.MISSING_CSRF_TOKEN, exception.category());
        assertFalse(exception.isCsrfHeaderPresent());
    }

    @Test
    void testValidateCsrf_InvalidToken_ThrowsException() {
        LiveSessionConfig config = LiveSessionConfig.defaults();
        String sessionToken = LiveHttpSecurity.generateCsrfToken();
        LiveHttpRequestMetadata metadata = new LiveHttpRequestMetadata("wrong-token", null, null, null);

        LiveCsrfException exception = assertThrows(LiveCsrfException.class, 
            () -> LiveHttpSecurity.validateCsrf(config, sessionToken, metadata));

        assertEquals(LiveHttpFailureCategory.INVALID_CSRF_TOKEN, exception.category());
        assertTrue(exception.isCsrfHeaderPresent());
    }

    @Test
    void testValidateCsrf_ValidToken_NoOrigin_ThrowsException() {
        LiveSessionConfig config = LiveSessionConfig.defaults();
        String sessionToken = LiveHttpSecurity.generateCsrfToken();
        LiveHttpRequestMetadata metadata = new LiveHttpRequestMetadata(sessionToken, null, null, "localhost:8080", "http");

        LiveCsrfException exception = assertThrows(LiveCsrfException.class, 
            () -> LiveHttpSecurity.validateCsrf(config, sessionToken, metadata));

        assertEquals(LiveHttpFailureCategory.CROSS_ORIGIN_REQUEST, exception.category());
        assertEquals("Missing Origin and Referer headers.", exception.safeMessage());
    }

    @Test
    void testValidateCsrf_ValidToken_SameOrigin_Succeeds() {
        LiveSessionConfig config = LiveSessionConfig.defaults();
        String sessionToken = LiveHttpSecurity.generateCsrfToken();
        LiveHttpRequestMetadata metadata = new LiveHttpRequestMetadata(sessionToken, "http://localhost:8080", null, "localhost:8080", "http");

        assertDoesNotThrow(() -> LiveHttpSecurity.validateCsrf(config, sessionToken, metadata));
    }

    @Test
    void testValidateCsrf_ValidToken_SameOriginDefaultPort_Succeeds() {
        LiveSessionConfig config = LiveSessionConfig.defaults();
        String sessionToken = LiveHttpSecurity.generateCsrfToken();
        LiveHttpRequestMetadata metadata = new LiveHttpRequestMetadata(sessionToken, "http://localhost", null, "localhost", "http");

        assertDoesNotThrow(() -> LiveHttpSecurity.validateCsrf(config, sessionToken, metadata));
    }

    @Test
    void testValidateCsrf_ValidToken_SameReferer_Succeeds() {
        LiveSessionConfig config = LiveSessionConfig.defaults();
        String sessionToken = LiveHttpSecurity.generateCsrfToken();
        LiveHttpRequestMetadata metadata = new LiveHttpRequestMetadata(sessionToken, null, "http://localhost:8080/home", "localhost:8080", "http");

        assertDoesNotThrow(() -> LiveHttpSecurity.validateCsrf(config, sessionToken, metadata));
    }

    @Test
    void testValidateCsrf_ValidToken_CrossOrigin_ThrowsException() {
        LiveSessionConfig config = LiveSessionConfig.defaults();
        String sessionToken = LiveHttpSecurity.generateCsrfToken();
        LiveHttpRequestMetadata metadata = new LiveHttpRequestMetadata(sessionToken, "http://evil.com", null, "localhost:8080", "http");

        LiveCsrfException exception = assertThrows(LiveCsrfException.class, 
            () -> LiveHttpSecurity.validateCsrf(config, sessionToken, metadata));

        assertEquals(LiveHttpFailureCategory.CROSS_ORIGIN_REQUEST, exception.category());
    }

    @Test
    void testValidateCsrf_ValidToken_SchemeMismatch_ThrowsException() {
        LiveSessionConfig config = LiveSessionConfig.defaults();
        String sessionToken = LiveHttpSecurity.generateCsrfToken();
        LiveHttpRequestMetadata metadata = new LiveHttpRequestMetadata(sessionToken, "https://localhost", null, "localhost", "http");

        LiveCsrfException exception = assertThrows(LiveCsrfException.class,
                () -> LiveHttpSecurity.validateCsrf(config, sessionToken, metadata));

        assertEquals(LiveHttpFailureCategory.CROSS_ORIGIN_REQUEST, exception.category());
        assertEquals("Cross-origin scheme mismatch.", exception.safeMessage());
    }

    @Test
    void testValidateCsrf_ValidToken_PortMismatch_ThrowsException() {
        LiveSessionConfig config = LiveSessionConfig.defaults();
        String sessionToken = LiveHttpSecurity.generateCsrfToken();
        LiveHttpRequestMetadata metadata = new LiveHttpRequestMetadata(sessionToken, "http://localhost", null, "localhost:8080", "http");

        LiveCsrfException exception = assertThrows(LiveCsrfException.class,
                () -> LiveHttpSecurity.validateCsrf(config, sessionToken, metadata));

        assertEquals(LiveHttpFailureCategory.CROSS_ORIGIN_REQUEST, exception.category());
        assertEquals("Cross-origin port mismatch.", exception.safeMessage());
    }

    @Test
    void testValidateCsrf_DisabledForDevelopment_Succeeds() {
        LiveSessionConfig config = LiveSessionConfig.builder().disableCsrfProtectionForDevelopmentUnsafe().build();
        String sessionToken = LiveHttpSecurity.generateCsrfToken();
        // Missing token and origin, but disabled
        LiveHttpRequestMetadata metadata = new LiveHttpRequestMetadata(null, null, null, null);

        assertDoesNotThrow(() -> LiveHttpSecurity.validateCsrf(config, sessionToken, metadata));
    }

    @Test
    void csrfTokenChangesPerSession() {
        try (LiveSession first = new LiveSession(new Router());
             LiveSession second = new LiveSession(new Router())) {
            assertNotEquals(first.csrfToken(), second.csrfToken());
            assertNotEquals(first.sessionId(), second.sessionId());
        }
    }

    @Page("/")
    public static final class HomePage {
        public Node render() {
            return p("Home");
        }
    }
}
