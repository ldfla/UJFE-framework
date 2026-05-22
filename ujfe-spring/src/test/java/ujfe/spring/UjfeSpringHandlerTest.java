package ujfe.spring;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.stereotype.Controller;
import ujfe.core.Node;
import ujfe.live.LiveHttpPaths;
import ujfe.live.LiveSession;
import ujfe.live.LiveSessionConfig;
import ujfe.live.SecurityHeadersConfig;
import ujfe.router.Page;
import ujfe.router.Router;
import ujfe.validation.AccessibilityValidator;
import ujfe.validation.ValidationMode;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static ujfe.core.UI.*;

final class UjfeSpringHandlerTest {
    @Test
    void mapsOnlyUjfeRoutesAndInternalEndpoints() throws Exception {
        Router router = new Router()
            .register(new HomePage())
            .register("/dashboard", DashboardPage::new)
            .register("/assets/example.css", DashboardPage::new);
        try (LiveSession session = new LiveSession(router)) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            UjfeSpringHandlerMapping mapping = new UjfeSpringHandlerMapping(router, handler);

            assertNotNull(mapping.getHandler(new MockHttpServletRequest("GET", "/")));
            assertNotNull(mapping.getHandler(new MockHttpServletRequest("GET", "/dashboard")));
            assertNotNull(mapping.getHandler(new MockHttpServletRequest("GET", "/_ujfe/client.js")));
            assertNotNull(mapping.getHandler(new MockHttpServletRequest("GET", "/_ujfe/state")));
            assertNotNull(mapping.getHandler(new MockHttpServletRequest("POST", "/_ujfe/state")));
            assertNotNull(mapping.getHandler(new MockHttpServletRequest("GET", "/_ujfe/unknown.js")));
            assertNull(mapping.getHandler(new MockHttpServletRequest("GET", "/api/users")));
            assertNull(mapping.getHandler(new MockHttpServletRequest("GET", "/api/users/42")));
            assertNull(mapping.getHandler(new MockHttpServletRequest("GET", "/actuator/health")));
            assertNull(mapping.getHandler(new MockHttpServletRequest("GET", "/missing")));
            assertNull(mapping.getHandler(new MockHttpServletRequest("GET", "/favicon.ico")));
            assertNull(mapping.getHandler(new MockHttpServletRequest("GET", "/assets/app.css")));
            assertNull(mapping.getHandler(new MockHttpServletRequest("GET", "/assets/example.css")));
            assertNull(mapping.getHandler(new MockHttpServletRequest("GET", "/static/example.js")));
            assertNull(mapping.getHandler(new MockHttpServletRequest("GET", "/public/example.txt")));
            assertNull(mapping.getHandler(new MockHttpServletRequest("GET", "/webjars/example/example.js")));
            assertNull(mapping.getHandler(new MockHttpServletRequest("GET", "/css/app.css")));
            assertNull(mapping.getHandler(new MockHttpServletRequest("GET", "/js/app.js")));
            assertNull(mapping.getHandler(new MockHttpServletRequest("GET", "/images/logo.png")));
            assertNull(mapping.getHandler(new MockHttpServletRequest("POST", "/")));
            assertEquals(UjfeSpringHandlerMapping.DEFAULT_ORDER, mapping.getOrder());
        }
    }

    @Nested
    final class SpringMvcCoexistenceTests {
        @Test
        void registeredUjfeRoutesAndInternalEndpointsRunThroughSpringMvc() throws Exception {
            try (SpringMvcFixture fixture = SpringMvcFixture.create()) {
                MvcResult dashboard = fixture.performGet("/dashboard");
                assertEquals(200, dashboard.getResponse()
                    .getStatus());
                assertTrue(dashboard.getResponse()
                    .getContentType()
                    .startsWith("text/html"));
                assertTrue(dashboard.getResponse()
                    .getContentAsString()
                    .contains("Dashboard"));

                MvcResult docs = fixture.performGet("/docs");
                assertEquals(200, docs.getResponse()
                    .getStatus());
                assertTrue(docs.getResponse()
                    .getContentAsString()
                    .contains("Docs"));

                MvcResult client = fixture.performGet("/_ujfe/client.js");
                assertEquals(200, client.getResponse()
                    .getStatus());
                assertTrue(client.getResponse()
                    .getContentType()
                    .startsWith("application/javascript"));
                assertTrue(client.getResponse()
                    .getContentAsString()
                    .contains("/_ujfe/event"));

                MvcResult css = fixture.mockMvc.perform(MockMvcRequestBuilders.get("/_ujfe/css")
                        .param("classes", "p-4"))
                    .andReturn();
                assertEquals(200, css.getResponse()
                    .getStatus());
                assertTrue(css.getResponse()
                    .getContentType()
                    .startsWith("text/css"));
            }
        }

        @Test
        void liveEventEndpointRunsThroughUjfeInternalLogic() throws Exception {
            try (SpringMvcFixture fixture = SpringMvcFixture.create()) {
                MvcResult page = fixture.performGet("/dashboard");
                String eventId = firstEventId(page.getResponse()
                    .getContentAsString());

                MvcResult event = fixture.mockMvc.perform(MockMvcRequestBuilders.post(LiveHttpPaths.EVENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}"))
                    .andReturn();

                assertEquals(200, event.getResponse()
                    .getStatus());
                assertTrue(event.getResponse()
                    .getContentType()
                    .startsWith("application/json"));
                assertTrue(event.getResponse()
                    .getContentAsString()
                    .contains("\"html\""));
            }
        }

        @Test
        void springMvcControllersKeepTheirStatusBodyAndContentType() throws Exception {
            try (SpringMvcFixture fixture = SpringMvcFixture.create()) {
                MvcResult users = fixture.performGet("/api/users");
                assertEquals(200, users.getResponse()
                    .getStatus());
                assertTrue(users.getResponse()
                    .getContentType()
                    .startsWith(MediaType.APPLICATION_JSON_VALUE));
                assertEquals("{\"source\":\"spring-rest\",\"count\":2}", users.getResponse()
                    .getContentAsString());

                MvcResult user = fixture.performGet("/api/users/42");
                assertEquals(202, user.getResponse()
                    .getStatus());
                assertTrue(user.getResponse()
                    .getContentType()
                    .startsWith(MediaType.APPLICATION_JSON_VALUE));
                assertEquals("{\"id\":\"42\",\"source\":\"spring-rest\"}", user.getResponse()
                    .getContentAsString());

                MvcResult mvcPage = fixture.performGet("/mvc/page");
                assertEquals(203, mvcPage.getResponse()
                    .getStatus());
                assertTrue(mvcPage.getResponse()
                    .getContentType()
                    .startsWith(MediaType.TEXT_PLAIN_VALUE));
                assertEquals("spring-mvc-page", mvcPage.getResponse()
                    .getContentAsString());
            }
        }

        @Test
        void actuatorLikeAndUnknownRoutesAreNotConvertedToUjfeErrors() throws Exception {
            try (SpringMvcFixture fixture = SpringMvcFixture.create()) {
                MvcResult health = fixture.performGet("/actuator/health");
                assertEquals(200, health.getResponse()
                    .getStatus());
                assertEquals("{\"status\":\"UP\"}", health.getResponse()
                    .getContentAsString());

                MvcResult info = fixture.performGet("/actuator/info");
                assertEquals(200, info.getResponse()
                    .getStatus());
                assertEquals("{\"app\":\"spring\"}", info.getResponse()
                    .getContentAsString());

                try (LoggerSilencer ignored = LoggerSilencer.attach("org.springframework.web.servlet.PageNotFound")) {
                    MvcResult missing = fixture.performGet("/missing");
                    assertEquals(404, missing.getResponse()
                        .getStatus());
                    assertFalse(missing.getResponse()
                        .getContentAsString()
                        .contains("UJFE_ROUTE_NOT_FOUND"));
                }
            }
        }

        @Test
        void springStaticResourcesAreNotHijacked() throws Exception {
            try (SpringMvcFixture fixture = SpringMvcFixture.create()) {
                assertStaticResource(fixture.performGet("/favicon.ico"), "ujfe-test-favicon");
                assertStaticResource(fixture.performGet("/assets/example.css"), "body{color:#123456;}");
                assertStaticResource(fixture.performGet("/static/example.js"), "window.ujfeStaticExample=true;");
                assertStaticResource(fixture.performGet("/webjars/example/example.js"), "window.ujfeWebjarExample=true;");

                try (LoggerSilencer ignored = LoggerSilencer.attach("org.springframework.web.servlet.resource")) {
                    MvcResult missingStatic = fixture.performGet("/assets/missing.css");
                    assertEquals(404, missingStatic.getResponse()
                        .getStatus());
                    assertFalse(missingStatic.getResponse()
                        .getContentAsString()
                        .contains("UJFE_ROUTE_NOT_FOUND"));
                }
            }
        }

        @Test
        void servletSecurityFiltersStillApplyToUjfeAndSpringRoutes() throws Exception {
            try (SpringMvcFixture fixture = SpringMvcFixture.create(requiringAuthHeaderFilter())) {
                assertEquals(401, fixture.performGet("/dashboard")
                    .getResponse()
                    .getStatus());
                assertEquals(401, fixture.performGet("/api/users")
                    .getResponse()
                    .getStatus());
                assertEquals(401, fixture.performGet("/_ujfe/client.js")
                    .getResponse()
                    .getStatus());

                assertEquals(200, fixture.performGet("/dashboard", true)
                    .getResponse()
                    .getStatus());
                assertEquals(200, fixture.performGet("/api/users", true)
                    .getResponse()
                    .getStatus());
                assertEquals(200, fixture.performGet("/_ujfe/client.js", true)
                    .getResponse()
                    .getStatus());
            }
        }

        private void assertStaticResource(MvcResult result, String expectedBody) throws Exception {
            assertEquals(200, result.getResponse()
                .getStatus());
            assertEquals(expectedBody, result.getResponse()
                .getContentAsString()
                .stripTrailing());
        }
    }

    @Test
    void springPropertiesCanEnableDevelopmentErrorDetails() {
        UjfeSpringProperties properties = new UjfeSpringProperties();
        properties.setEnabled(true);

        LiveSessionConfig config = new UjfeSpringAutoConfiguration()
            .ujfeLiveSessionConfig(
                properties,
                new UjfeSpringSecurityHeadersProperties(),
                new UjfeSpringClientStateProperties()
            );

        assertTrue(config.isDevelopmentErrorDetailsEnabled());
    }

    @Test
    void springPropertiesCanCustomizeAndDisableSecurityHeaders() {
        UjfeSpringSecurityHeadersProperties custom = new UjfeSpringSecurityHeadersProperties();
        custom.setEnabled(true);
        custom.setXContentTypeOptions("nosniff");
        custom.setXFrameOptions("SAMEORIGIN");
        custom.setReferrerPolicy("same-origin");
        custom.setContentSecurityPolicy("default-src 'self'");
        custom.setPermissionsPolicy("geolocation=()");

        LiveSessionConfig customConfig = new UjfeSpringAutoConfiguration()
            .ujfeLiveSessionConfig(
                new UjfeSpringProperties(),
                custom,
                new UjfeSpringClientStateProperties()
            );

        assertTrue(custom.getEnabled());
        assertEquals("nosniff", custom.getXContentTypeOptions());
        assertEquals("SAMEORIGIN", custom.getXFrameOptions());
        assertEquals("same-origin", custom.getReferrerPolicy());
        assertEquals("default-src 'self'", custom.getContentSecurityPolicy());
        assertEquals("geolocation=()", custom.getPermissionsPolicy());
        assertEquals("same-origin", customConfig.securityHeaders()
            .get(SecurityHeadersConfig.REFERRER_POLICY));
        assertEquals("default-src 'self'", customConfig.securityHeaders()
            .get(SecurityHeadersConfig.CONTENT_SECURITY_POLICY));

        UjfeSpringSecurityHeadersProperties disabled = new UjfeSpringSecurityHeadersProperties();
        disabled.setEnabled(false);
        LiveSessionConfig disabledConfig = new UjfeSpringAutoConfiguration()
            .ujfeLiveSessionConfig(
                new UjfeSpringProperties(),
                disabled,
                new UjfeSpringClientStateProperties()
            );

        assertFalse(disabledConfig.securityHeadersConfig()
            .isEnabled());
        assertTrue(disabledConfig.securityHeaders()
            .isEmpty());
    }

    @Test
    void springPropertiesCanConfigureClientStatePolicy() {
        UjfeSpringClientStateProperties clientState = new UjfeSpringClientStateProperties();
        clientState.setCookies(java.util.List.of("ujfe_demo"));
        clientState.setLocalStorageKeys(java.util.List.of("ujfe.theme"));
        clientState.setSessionStorageKeys(java.util.List.of("ujfe.tab"));

        LiveSessionConfig config = new UjfeSpringAutoConfiguration()
            .ujfeLiveSessionConfig(
                new UjfeSpringProperties(),
                new UjfeSpringSecurityHeadersProperties(),
                clientState
            );

        assertEquals(java.util.Set.of("ujfe_demo"), config.clientStatePolicy()
            .allowedCookies());
        assertEquals(java.util.Set.of("ujfe.theme"), config.clientStatePolicy()
            .allowedLocalStorageKeys());
        assertEquals(java.util.Set.of("ujfe.tab"), config.clientStatePolicy()
            .allowedSessionStorageKeys());
        assertEquals(java.util.List.of("ujfe_demo"), clientState.getCookies());
        assertEquals(java.util.List.of("ujfe.theme"), clientState.getLocalStorageKeys());
        assertEquals(java.util.List.of("ujfe.tab"), clientState.getSessionStorageKeys());
    }

    @Test
    void springPropertiesCanConfigureValidation() {
        UjfeSpringValidationProperties validation = new UjfeSpringValidationProperties();
        validation.setMode(ValidationMode.WARN);
        validation.setAccessibilityEnabled(true);
        validation.setSeoEnabled(true);
        validation.setCanonicalEnabled(true);
        validation.setOpenGraphEnabled(true);
        validation.setHtmlLangEnabled(true);
        validation.setDisabledRules(java.util.List.of(AccessibilityValidator.INTERACTIVE_NESTED));

        LiveSessionConfig config = new UjfeSpringAutoConfiguration()
            .ujfeLiveSessionConfig(
                new UjfeSpringProperties(),
                new UjfeSpringSecurityHeadersProperties(),
                new UjfeSpringClientStateProperties(),
                validation
            );

        assertEquals(ValidationMode.WARN, config.validationOptions()
            .mode());
        assertTrue(config.validationOptions()
            .accessibilityValidationEnabled());
        assertTrue(config.validationOptions()
            .seoValidationEnabled());
        assertTrue(config.validationOptions()
            .canonicalLinkValidationEnabled());
        assertTrue(config.validationOptions()
            .openGraphValidationEnabled());
        assertTrue(config.validationOptions()
            .htmlLangValidationEnabled());
        assertTrue(config.validationOptions()
            .disabledRuleIds()
            .contains(AccessibilityValidator.INTERACTIVE_NESTED));
    }

    @Test
    void rendersUjfePageThroughServletResponse() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()), clientStateConfig())) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
            request.addHeader("Cookie", "ujfe_demo=ativo");
            MockHttpServletResponse response = new MockHttpServletResponse();

            handler.handleRequest(request, response);

            assertEquals(200, response.getStatus());
            assertTrue(response.getContentType()
                .startsWith("text/html"));
            assertTrue(response.getContentAsString()
                .contains("Home"));
            assertTrue(response.getContentAsString()
                .contains("Cookie: ativo"));
            assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
            assertEquals("strict-origin-when-cross-origin", response.getHeader("Referrer-Policy"));
            assertTrue(response.getHeader("Content-Security-Policy")
                .contains("default-src 'self'"));
        }
    }

    @Test
    void defaultClientStatePolicyDoesNotExposeCookiesFromPageRequests() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
            request.addHeader("Cookie", "ujfe_demo=ativo");
            MockHttpServletResponse response = new MockHttpServletResponse();

            handler.handleRequest(request, response);

            assertEquals(200, response.getStatus());
            assertTrue(response.getContentAsString()
                .contains("Cookie: missing"));
        }
    }

    @Test
    void doesNotOverrideSecurityHeadersAlreadySetBySpringSecurity() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse response = new MockHttpServletResponse();
            response.setHeader("Content-Security-Policy", "default-src 'none'");
            response.setHeader("X-Frame-Options", "SAMEORIGIN");

            handler.handleRequest(new MockHttpServletRequest("GET", "/"), response);

            assertEquals(200, response.getStatus());
            assertEquals("default-src 'none'", response.getHeader("Content-Security-Policy"));
            assertEquals("SAMEORIGIN", response.getHeader("X-Frame-Options"));
            assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        }
    }

    @Test
    void servesLiveClientScriptOnSameMvcPipeline() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse response = new MockHttpServletResponse();

            handler.handleRequest(new MockHttpServletRequest("GET", "/_ujfe/client.js"), response);

            assertEquals(200, response.getStatus());
            assertTrue(response.getContentType()
                .startsWith("application/javascript"));
            assertTrue(response.getContentAsString()
                .contains("/_ujfe/event"));
        }
    }

    @Test
    void handlesLiveEventWithSharedCodecPayload() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse page = new MockHttpServletResponse();
            handler.handleRequest(new MockHttpServletRequest("GET", "/"), page);
            String eventId = firstEventId(page.getContentAsString());
            String csrfToken = firstCsrfToken(page.getContentAsString());

            MockHttpServletResponse event = new MockHttpServletResponse();
            MockHttpServletRequest request = postJson(LiveHttpPaths.EVENT,
                "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"cookies\":\"ujfe_demo=ativo\","
                    + "\"localStorage\":{\"theme\":\"dark\"}}}");
            request.addHeader("X-UJFE-CSRF", csrfToken);
            request.addHeader("Origin", "http://localhost");
            request.addHeader("Host", "localhost");
            handler.handleRequest(request, event);

            assertEquals(200, event.getStatus());
            assertTrue(event.getContentType()
                .startsWith("application/json"));
            assertTrue(event.getContentAsString()
                .contains("\"html\""));
        }
    }

    @Test
    void rejectsLiveEventWithoutCsrfToken() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse page = new MockHttpServletResponse();
            handler.handleRequest(new MockHttpServletRequest("GET", "/"), page);
            String eventId = firstEventId(page.getContentAsString());

            MockHttpServletRequest request = postJson(LiveHttpPaths.EVENT,
                "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}");
            request.addHeader("Origin", "http://localhost");
            request.addHeader("Host", "localhost");
            MockHttpServletResponse response = new MockHttpServletResponse();

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                handler.handleRequest(request, response);
            }

            assertEquals(403, response.getStatus());
            assertError(response, 403, "UJFE_CSRF_VALIDATION_FAILED", "The request could not be verified.");
        }
    }

    @Test
    void rejectsLiveEventWithInvalidCsrfToken() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse page = new MockHttpServletResponse();
            handler.handleRequest(new MockHttpServletRequest("GET", "/"), page);
            String eventId = firstEventId(page.getContentAsString());

            MockHttpServletRequest request = postJson(LiveHttpPaths.EVENT,
                "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}");
            request.addHeader("X-UJFE-CSRF", "invalid-token");
            request.addHeader("Origin", "http://localhost");
            request.addHeader("Host", "localhost");
            MockHttpServletResponse response = new MockHttpServletResponse();

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                handler.handleRequest(request, response);
            }

            assertEquals(403, response.getStatus());
            assertError(response, 403, "UJFE_CSRF_VALIDATION_FAILED", "The request could not be verified.");
        }
    }

    @Test
    void rejectsCrossOriginLiveEvent() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse page = new MockHttpServletResponse();
            handler.handleRequest(new MockHttpServletRequest("GET", "/"), page);
            String eventId = firstEventId(page.getContentAsString());
            String csrfToken = firstCsrfToken(page.getContentAsString());

            MockHttpServletRequest request = postJson(LiveHttpPaths.EVENT,
                "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}");
            request.addHeader("X-UJFE-CSRF", csrfToken);
            request.addHeader("Origin", "http://evil.test");
            request.addHeader("Host", "localhost");
            MockHttpServletResponse response = new MockHttpServletResponse();

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                handler.handleRequest(request, response);
            }

            assertEquals(403, response.getStatus());
            assertError(response, 403, "UJFE_CSRF_VALIDATION_FAILED", "The request could not be verified.");
        }
    }

    @Test
    void developmentOverrideAllowsMissingCsrfToken() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()),
            ujfe.live.LiveSessionConfig.builder()
                .disableCsrfProtectionForDevelopmentUnsafe()
                .build())) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse page = new MockHttpServletResponse();
            handler.handleRequest(new MockHttpServletRequest("GET", "/"), page);
            String eventId = firstEventId(page.getContentAsString());

            MockHttpServletResponse response = new MockHttpServletResponse();
            handler.handleRequest(postJson(LiveHttpPaths.EVENT,
                "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}"), response);

            assertEquals(200, response.getStatus());
            assertTrue(response.getContentAsString()
                .contains("\"html\""));
            assertFalse(page.getContentAsString()
                .contains("ujfe-csrf-token"));
        }
    }

    @Test
    void rejectsMalformedJsonWithSafeCodecError() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse response = new MockHttpServletResponse();

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                handler.handleRequest(postJson(LiveHttpPaths.EVENT, "{\"eventId\":}"), response);
            }

            assertError(response, 400, "UJFE_BAD_REQUEST", "The request is invalid.");
        }
    }

    @Test
    void rejectsOversizedPayloadWithConfiguredLimit() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session, 32);
            MockHttpServletResponse response = new MockHttpServletResponse();

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                handler.handleRequest(postJson(LiveHttpPaths.EVENT,
                    "{\"eventId\":\"evt-42\",\"clientState\":{\"localStorage\":{}}}"), response);
            }

            assertError(response, 413, "UJFE_BAD_REQUEST", "The request is invalid.");
        }
    }

    @Test
    void rateLimitsLiveEventEndpoint() throws Exception {
        LiveSessionConfig config = LiveSessionConfig.builder()
            .disableCsrfProtectionForDevelopmentUnsafe()
            .internalEndpointRateLimit(1, 1, Duration.ofMinutes(1))
            .build();
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()), config)) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse page = new MockHttpServletResponse();
            handler.handleRequest(new MockHttpServletRequest("GET", "/"), page);
            String eventId = firstEventId(page.getContentAsString());

            MockHttpServletResponse first = new MockHttpServletResponse();
            handler.handleRequest(postJson(LiveHttpPaths.EVENT,
                "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}"), first);

            MockHttpServletResponse second = new MockHttpServletResponse();
            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                handler.handleRequest(postJson(LiveHttpPaths.EVENT,
                    "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}"), second);
            }

            assertEquals(200, first.getStatus());
            assertError(second, 429, "UJFE_RATE_LIMITED", "Too many requests.");
            assertNotNull(second.getHeader("Retry-After"));
        }
    }

    @Test
    void rateLimitsStateEndpoint() throws Exception {
        LiveSessionConfig config = LiveSessionConfig.builder()
            .disableCsrfProtectionForDevelopmentUnsafe()
            .internalEndpointRateLimit(1, 1, Duration.ofMinutes(1))
            .build();
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()), config)) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            handler.handleRequest(new MockHttpServletRequest("GET", "/"), new MockHttpServletResponse());

            MockHttpServletResponse first = new MockHttpServletResponse();
            handler.handleRequest(postJson(LiveHttpPaths.STATE,
                "{\"clientState\":{\"localStorage\":{}}}"), first);

            MockHttpServletResponse second = new MockHttpServletResponse();
            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                handler.handleRequest(postJson(LiveHttpPaths.STATE,
                    "{\"clientState\":{\"localStorage\":{}}}"), second);
            }

            assertEquals(200, first.getStatus());
            assertError(second, 429, "UJFE_RATE_LIMITED", "Too many requests.");
        }
    }

    @Test
    void renderFailureReturnsSafeJsonErrorResponse() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new FailingRenderPage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
            request.addHeader("X-Request-Id", "req-render-1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                handler.handleRequest(request, response);
            }

            assertError(response, 500, "UJFE_RENDER_ERROR", "An error occurred while rendering the page.");
            assertTrue(response.getContentAsString()
                .contains("\"requestId\":\"req-render-1\""));
            assertFalse(response.getContentAsString()
                .contains("render-secret"));
            assertFalse(response.getContentAsString()
                .contains("FailingRenderPage"));
        }
    }

    @Test
    void eventFailureReturnsSafeJsonErrorResponse() throws Exception {
        LiveSessionConfig config = LiveSessionConfig.builder()
            .disableCsrfProtectionForDevelopmentUnsafe()
            .build();
        try (LiveSession session = new LiveSession(new Router().register(new FailingEventPage()), config)) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse page = new MockHttpServletResponse();
            handler.handleRequest(new MockHttpServletRequest("GET", "/"), page);
            String eventId = firstEventId(page.getContentAsString());
            MockHttpServletResponse response = new MockHttpServletResponse();

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                handler.handleRequest(postJson(LiveHttpPaths.EVENT,
                    "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}"), response);
            }

            assertError(response, 500, "UJFE_EVENT_HANDLER_ERROR", "An error occurred while handling the live event.");
            assertFalse(response.getContentAsString()
                .contains("event-secret"));
            assertFalse(response.getContentAsString()
                .contains("FailingEventPage"));
        }
    }

    @Test
    void missingRouteReturnsSafeJsonNotFoundResponse() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse response = new MockHttpServletResponse();

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                handler.handleRequest(new MockHttpServletRequest("GET", "/missing"), response);
            }

            assertError(response, 404, "UJFE_ROUTE_NOT_FOUND", "The requested route was not found.");
            assertFalse(response.getContentAsString()
                .contains("No UJFE route registered"));
        }
    }

    @Test
    void staticAssetRequestsReturnPlain404BeforePageRenderingWhenHandlerReceivesThem() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new AssetLikePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);

            assertStaticAssetNotFound(handle(handler, new MockHttpServletRequest("GET", "/poster.png")));
            assertStaticAssetNotFound(handle(handler, new MockHttpServletRequest("GET", "/demo.mp4")));
            assertStaticAssetNotFound(handle(handler, new MockHttpServletRequest("GET", "/audio.mp3")));
        }
    }

    @Test
    void unsafeStaticAssetPathIsRejectedWithoutRendering() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new AssetLikePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse response;

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                response = handle(handler, new MockHttpServletRequest("GET", "/assets/%2e%2e/secret.txt"));
            }

            assertEquals(400, response.getStatus());
            assertTrue(response.getContentType()
                .startsWith("text/plain"));
            assertEquals("Invalid static asset path.", response.getContentAsString());
        }
    }

    @Test
    void publicPageRouteIsNotRateLimited() throws Exception {
        LiveSessionConfig config = LiveSessionConfig.builder()
            .internalEndpointRateLimit(1, 1, Duration.ofMinutes(1))
            .build();
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()), config)) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse first = new MockHttpServletResponse();
            MockHttpServletResponse second = new MockHttpServletResponse();

            handler.handleRequest(new MockHttpServletRequest("GET", "/"), first);
            handler.handleRequest(new MockHttpServletRequest("GET", "/"), second);

            assertEquals(200, first.getStatus());
            assertEquals(200, second.getStatus());
        }
    }

    private static MockHttpServletRequest postJson(String path, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.setContentType("application/json");
        return request;
    }

    private static MockHttpServletResponse handle(UjfeSpringHandler handler, MockHttpServletRequest request)
        throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.handleRequest(request, response);
        return response;
    }

    private static LiveSessionConfig clientStateConfig() {
        return LiveSessionConfig.builder()
            .allowClientCookie("ujfe_demo")
            .allowLocalStorageKey("theme")
            .build();
    }

    private static String firstEventId(String html) {
        Matcher matcher = Pattern.compile("data-ujfe-event-click=\"([^\"]+)\"")
            .matcher(html);
        assertTrue(matcher.find(), "Expected rendered page to contain a click event id");
        return matcher.group(1);
    }

    private static String firstCsrfToken(String html) {
        Matcher matcher = Pattern.compile("meta name=\"ujfe-csrf-token\" content=\"([^\"]+)\"")
            .matcher(html);
        assertTrue(matcher.find(), "Expected rendered page to contain a csrf token");
        return matcher.group(1);
    }

    private static void assertError(MockHttpServletResponse response, int status, String code, String message)
        throws Exception {
        assertEquals(status, response.getStatus());
        assertTrue(response.getContentType()
            .startsWith("application/json"));
        String body = response.getContentAsString();
        assertTrue(body.contains("\"code\":\"" + code + "\""), body);
        assertTrue(body.contains("\"message\":\"" + message + "\""), body);
        assertFalse(body.contains("Exception"), body);
        assertFalse(body.contains("LiveHttpCodec"), body);
        assertFalse(body.contains("/Users/"), body);
    }

    private static void assertStaticAssetNotFound(MockHttpServletResponse response) throws Exception {
        assertEquals(404, response.getStatus());
        assertTrue(response.getContentType()
            .startsWith("text/plain"));
        assertEquals("Static asset not found.", response.getContentAsString());
        assertFalse(response.getContentAsString()
            .contains("UJFE_ROUTE_NOT_FOUND"));
        assertFalse(response.getContentAsString()
            .contains("No UJFE route registered"));
    }

    @Page("/")
    public static final class HomePage {
        public Node render() {
            return div()
                .child(p("Home"))
                .child(p(() -> "Cookie: " + ujfe.core.Ujfe.cookie("ujfe_demo")
                    .orElse("missing")))
                .child(button("Click").onClick(() -> {
                }));
        }
    }

    public static final class DashboardPage {
        public Node render() {
            return main()
                .child(h1("Dashboard"))
                .child(button("Refresh").onClick(() -> {
                }));
        }
    }

    public static final class DocsPage {
        public Node render() {
            return main()
                .child(h1("Docs"));
        }
    }

    @Page("/")
    public static final class FailingRenderPage {
        public Node render() {
            throw new IllegalStateException("render-secret from /Users/leandrof/Dev/ujfe/FailingRenderPage.java");
        }
    }

    @Page("/")
    public static final class FailingEventPage {
        public Node render() {
            return button("Fail").onClick(() -> {
                throw new IllegalStateException("event-secret from FailingEventPage");
            });
        }
    }

    @Page("/poster.png")
    public static final class AssetLikePage {
        public Node render() {
            throw new IllegalStateException("asset-like route should not render");
        }
    }

    @Configuration
    @EnableWebMvc
    static class RouteCoexistenceConfig implements WebMvcConfigurer {
        @Bean
        Router ujfeRouter() {
            return new Router()
                .register("/dashboard", DashboardPage::new)
                .register("/docs", DocsPage::new);
        }

        @Bean
        LiveSessionConfig ujfeLiveSessionConfig() {
            return LiveSessionConfig.builder()
                .disableCsrfProtectionForDevelopmentUnsafe()
                .build();
        }

        @Bean
        LiveSession ujfeLiveSession(Router router, LiveSessionConfig config) {
            return new LiveSession(router, config);
        }

        @Bean
        UjfeSpringHandler ujfeSpringHandler(LiveSession liveSession) {
            return new UjfeSpringHandler(liveSession);
        }

        @Bean
        UjfeSpringHandlerMapping ujfeSpringHandlerMapping(Router router, UjfeSpringHandler handler) {
            return new UjfeSpringHandlerMapping(router, handler);
        }

        @Bean
        ApiController apiController() {
            return new ApiController();
        }

        @Bean
        MvcController mvcController() {
            return new MvcController();
        }

        @Bean
        ActuatorLikeController actuatorLikeController() {
            return new ActuatorLikeController();
        }

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/ujfe-spring-test/");
            registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/ujfe-spring-test/assets/");
            registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/ujfe-spring-test/static/");
            registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/ujfe-spring-test/webjars/");
        }
    }

    @RestController
    static class ApiController {
        @GetMapping(value = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
        String users() {
            return "{\"source\":\"spring-rest\",\"count\":2}";
        }

        @GetMapping(value = "/api/users/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
        ResponseEntity<String> user(@PathVariable("id") String id) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"id\":\"" + id + "\",\"source\":\"spring-rest\"}");
        }
    }

    @Controller
    static class MvcController {
        @GetMapping(value = "/mvc/page", produces = MediaType.TEXT_PLAIN_VALUE)
        @ResponseBody
        ResponseEntity<String> page() {
            return ResponseEntity.status(203)
                .contentType(MediaType.TEXT_PLAIN)
                .body("spring-mvc-page");
        }
    }

    @RestController
    static class ActuatorLikeController {
        @GetMapping(value = "/actuator/health", produces = MediaType.APPLICATION_JSON_VALUE)
        String health() {
            return "{\"status\":\"UP\"}";
        }

        @GetMapping(value = "/actuator/info", produces = MediaType.APPLICATION_JSON_VALUE)
        String info() {
            return "{\"app\":\"spring\"}";
        }
    }

    private static Filter requiringAuthHeaderFilter() {
        return (request, response, chain) -> {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            String path = UjfeSpringPaths.pathWithinApplication(httpRequest);
            if (isProtectedPath(path) && !"ok".equals(httpRequest.getHeader("X-Test-Auth"))) {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            chain.doFilter(request, response);
        };
    }

    private static boolean isProtectedPath(String path) {
        return "/dashboard".equals(path)
            || "/api/users".equals(path)
            || path.startsWith("/_ujfe/");
    }

    private static final class SpringMvcFixture implements AutoCloseable {
        private final AnnotationConfigWebApplicationContext context;
        private final MockMvc mockMvc;

        private SpringMvcFixture(AnnotationConfigWebApplicationContext context, MockMvc mockMvc) {
            this.context = context;
            this.mockMvc = mockMvc;
        }

        static SpringMvcFixture create(Filter... filters) {
            AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
            context.setServletContext(new MockServletContext());
            context.register(RouteCoexistenceConfig.class);
            context.refresh();
            var builder = MockMvcBuilders.webAppContextSetup(context);
            if (filters.length > 0) {
                builder.addFilters(filters);
            }
            return new SpringMvcFixture(context, builder.build());
        }

        MvcResult performGet(String path) throws Exception {
            return performGet(path, false);
        }

        MvcResult performGet(String path, boolean authenticated) throws Exception {
            var request = MockMvcRequestBuilders.get(path);
            if (authenticated) {
                request.header("X-Test-Auth", "ok");
            }
            return mockMvc.perform(request)
                .andReturn();
        }

        @Override
        public void close() {
            context.close();
        }
    }

    private static final class LoggerSilencer implements AutoCloseable {
        private final Logger logger;
        private final boolean useParentHandlers;

        private LoggerSilencer(Logger logger) {
            this.logger = logger;
            this.useParentHandlers = logger.getUseParentHandlers();
            this.logger.setUseParentHandlers(false);
        }

        static LoggerSilencer attach(String loggerName) {
            return new LoggerSilencer(Logger.getLogger(loggerName));
        }

        @Override
        public void close() {
            logger.setUseParentHandlers(useParentHandlers);
        }
    }

    private static final class CodecLogSilencer implements AutoCloseable {
        private final Logger codecLogger;
        private final Logger errorLogger;
        private final Logger staticAssetLogger;
        private final boolean codecUseParentHandlers;
        private final boolean errorUseParentHandlers;
        private final boolean staticAssetUseParentHandlers;

        private CodecLogSilencer(Logger codecLogger, Logger errorLogger, Logger staticAssetLogger) {
            this.codecLogger = codecLogger;
            this.errorLogger = errorLogger;
            this.staticAssetLogger = staticAssetLogger;
            this.codecUseParentHandlers = codecLogger.getUseParentHandlers();
            this.errorUseParentHandlers = errorLogger.getUseParentHandlers();
            this.staticAssetUseParentHandlers = staticAssetLogger.getUseParentHandlers();
            this.codecLogger.setUseParentHandlers(false);
            this.errorLogger.setUseParentHandlers(false);
            this.staticAssetLogger.setUseParentHandlers(false);
        }

        static CodecLogSilencer attach() {
            return new CodecLogSilencer(
                Logger.getLogger(ujfe.live.LiveHttpCodec.class.getName()),
                Logger.getLogger(ujfe.live.ErrorResponseRenderer.class.getName()),
                Logger.getLogger(ujfe.live.StaticAssetHandler.class.getName())
            );
        }

        @Override
        public void close() {
            codecLogger.setUseParentHandlers(codecUseParentHandlers);
            errorLogger.setUseParentHandlers(errorUseParentHandlers);
            staticAssetLogger.setUseParentHandlers(staticAssetUseParentHandlers);
        }
    }
}
