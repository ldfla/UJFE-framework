package ujfe.live;

import org.junit.jupiter.api.Test;
import ujfe.router.Router;

import static org.junit.jupiter.api.Assertions.*;

final class LiveHttpSecurityTest {

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
}
