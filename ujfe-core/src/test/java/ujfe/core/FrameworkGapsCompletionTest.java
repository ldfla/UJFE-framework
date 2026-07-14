package ujfe.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static ujfe.core.UI.*;

final class FrameworkGapsCompletionTest {
    @Test
    void browserBridgeAndWebAuthnRenderCspSafeContracts() {
        String bridge = browserBridge("passkey-login")
            .moduleSrc("/assets/security/passkey.js")
            .api("navigator.credentials.get")
            .action("authenticate", PasskeyRequest.class, PasskeyResponse.class)
            .trigger("#login")
            .resultTarget("#result")
            .errorTarget("#errors")
            .render();

        assertTrue(bridge.contains("data-ujfe-browser-bridge=\"passkey-login\""));
        assertTrue(bridge.contains("data-ujfe-browser-api=\"navigator.credentials.get\""));
        assertTrue(bridge.contains("data-ujfe-request-type=\"" + PasskeyRequest.class.getName() + "\""));
        assertTrue(bridge.contains("<script type=\"module\" src=\"/assets/security/passkey.js\""));
        assertFalse(HtmlSnapshot.capture(unsafeHtml(bridge)).containsInlineExecutableScript());

        String webauthn = webAuthnLogin("/login/passkey/challenge", "/login/passkey/finish")
            .trigger("#passkey")
            .errorTarget("#login-errors")
            .statusTarget("#login-status")
            .userVerification("required")
            .timeout(Duration.ofSeconds(45))
            .render();

        assertTrue(webauthn.contains("data-ujfe-webauthn=\"authenticate\""));
        assertTrue(webauthn.contains("data-ujfe-challenge-url=\"/login/passkey/challenge\""));
        assertTrue(webauthn.contains("data-ujfe-user-verification=\"required\""));
        assertTrue(webauthn.contains("data-ujfe-timeout-ms=\"45000\""));
        assertThrows(IllegalArgumentException.class, () -> webAuthnRegistration("https://evil.test", "/ok"));
        assertThrows(IllegalArgumentException.class, () -> webAuthnLogin("/challenge", "/finish").timeout(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> browserBridge("bad name"));
    }

    @Test
    void contractsAndTypeSchemasExposePayloadMetadata() {
        TypeSchema schema = TypeSchema.of(PasskeyRequest.class);

        assertEquals(List.of("challenge", "userId"), schema.propertyNames());
        assertTrue(schema.json().contains(PasskeyRequest.class.getName()));
        assertTrue(schema.json().contains("\"challenge\""));

        String contractHtml = contract("login")
            .payload(PasskeyRequest.class)
            .action("authenticate", PasskeyResponse.class)
            .event("failed", BrowserFailure.class)
            .render();

        assertTrue(contractHtml.contains("data-ujfe-contract=\"login\""));
        assertTrue(contractHtml.contains("\"kind\":\"payload\""));
        assertTrue(contractHtml.contains("action:authenticate"));
        assertTrue(contractHtml.contains(BrowserFailure.class.getName()));
    }

    @Test
    void uiComponentAndScopedStyleColocateMarkupAssetsAndContracts() {
        ScopedStyle style = scopedStyle("login-card")
            .href("/assets/login-card.css")
            .token("gap", "space.3");

        String html = uiComponent("login-card")
            .props(new LoginProps("/dashboard"))
            .style(style)
            .contract(contract("login-card").payload(LoginProps.class))
            .clientModule(clientModule("login-card").src("/assets/login-card.js"))
            .css(style.className("root"))
            .child(h1("Login"))
            .render();

        assertTrue(html.contains("data-ujfe-component=\"login-card\""));
        assertTrue(html.contains("data-ujfe-props-type=\"" + LoginProps.class.getName() + "\""));
        assertTrue(html.contains("href=\"/assets/login-card.css\""));
        assertTrue(html.contains("data-ujfe-style-tokens=\"{&quot;gap&quot;:&quot;space.3&quot;}\""));
        assertTrue(html.contains("class=\"login-card__root\""));
        assertTrue(html.contains("<script type=\"module\" src=\"/assets/login-card.js\""));
        assertThrows(IllegalArgumentException.class, () -> scopedStyle("bad id!"));
    }

    @Test
    void dataLoadersSupportSuccessFailureTimeoutFallbackParallelAndBoundaries() {
        LoadResult<String> success = loader(String.class)
            .source(() -> "ready")
            .load();
        assertTrue(success.isSuccess());
        assertEquals("ready", success.value().orElseThrow());

        LoadResult<String> empty = loader(String.class)
            .source(() -> null)
            .load();
        assertEquals(LoadResult.Status.EMPTY, empty.status());
        assertTrue(loadBoundary(empty).empty(p("Empty")).render().contains("<p>Empty</p>"));

        LoadResult<String> failure = loader(String.class)
            .source(() -> {
                throw new IllegalStateException("boom");
            })
            .load();
        assertEquals(LoadResult.Status.FAILURE, failure.status());
        assertTrue(loadBoundary(failure).error(p("Failed")).render().contains("<p>Failed</p>"));

        LoadResult<String> fallback = loader(String.class)
            .source(() -> {
                sleep(150);
                return "late";
            })
            .timeout(Duration.ofMillis(10))
            .fallback(() -> "fallback")
            .load();
        assertEquals("fallback", fallback.value().orElseThrow());

        LoadResult<String> timeout = loader(String.class)
            .source(() -> {
                sleep(150);
                return "late";
            })
            .timeout(Duration.ofMillis(10))
            .load();
        assertEquals(LoadResult.Status.TIMEOUT, timeout.status());
        assertTrue(loadBoundary(timeout).timeout(p("Timeout")).render().contains("<p>Timeout</p>"));

        LoadResult<Map<String, Object>> map = loader(Map.class)
            .executor(Executors.newFixedThreadPool(2))
            .parallel("calls", () -> 7)
            .parallel("alerts", () -> 2)
            .loadMap();
        assertEquals(7, map.value().orElseThrow().get("calls"));
        assertEquals(2, map.value().orElseThrow().get("alerts"));

        assertThrows(IllegalStateException.class, () -> loader(String.class).load());
        assertThrows(IllegalStateException.class, () -> loader(Map.class).loadMap());
        assertThrows(IllegalArgumentException.class, () -> loader(String.class).timeout(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> loader(String.class).parallel("bad name", () -> 1));
        assertSame(String.class, loader(String.class).virtualThreadsWhenAvailable().type());
    }

    @Test
    void renderSnapshotsExposeEtagCacheHeadersAndRevalidationMetadata() {
        RenderMode mode = RenderMode.staticShell(Duration.ofSeconds(30))
            .revalidateOn("dashboard.updated");
        RenderSnapshot snapshot = RenderSnapshot.of("/dashboard", main().child("Dashboard"), mode);

        assertEquals("/dashboard", snapshot.route());
        assertEquals("<main>Dashboard</main>", snapshot.html());
        assertEquals(mode, snapshot.renderMode());
        assertEquals(64, snapshot.etag().length());
        assertEquals("\"" + snapshot.etag() + "\"", snapshot.headers().get("ETag"));
        assertEquals("private, max-age=30", snapshot.headers().get("Cache-Control"));
        assertEquals("dashboard.updated", snapshot.headers().get("X-UJFE-Revalidate-On"));
    }

    @Test
    void dashboardPrimitivesRenderSparklineAlertsAndHealthPanels() {
        String sparklineHtml = sparkline(List.of(1, 3, 2))
            .label("Calls")
            .tone("ok")
            .size(60, 20)
            .render();
        assertTrue(sparklineHtml.contains("data-ujfe-sparkline=\"1,3,2\""));
        assertTrue(sparklineHtml.contains("<polyline"));

        String onePoint = sparkline(List.of(5)).render();
        assertTrue(onePoint.contains("60,32"));

        String alerts = alertList()
            .title("Operational alerts")
            .alert("warn", "High latency", "/alerts/1")
            .render();
        assertTrue(alerts.contains("data-ujfe-alert-list=\"true\""));
        assertTrue(alerts.contains("data-ujfe-alert-tone=\"warn\""));
        assertTrue(alerts.contains("<a href=\"/alerts/1\">Open</a>"));
        assertTrue(alertList().render().contains("No alerts"));

        String health = healthPanel()
            .title("Systems")
            .item("Asterisk", "ok", "Connected")
            .item("ARI", "warn")
            .render();
        assertTrue(health.contains("data-ujfe-health-panel=\"true\""));
        assertTrue(health.contains("data-ujfe-health-status=\"ok\""));
        assertTrue(health.contains("<small>Connected</small>"));

        assertThrows(IllegalArgumentException.class, () -> sparkline(List.of()));
        assertThrows(IllegalArgumentException.class, () -> sparkline(List.of(1)).size(0, 10));
        assertThrows(IllegalArgumentException.class, () -> alertList().alert("warn", "Bad", "https://evil.test"));
    }

    @Test
    void dataGridAndAudioNowCoverFiltersResponsiveColumnsTimelineAndWaveform() {
        String grid = dataGrid(List.of(new User("1", "Ada", "active")))
            .caption("Users")
            .filter("status", "Status", "active")
            .responsiveTextColumn("name", "Name", 1, User::name)
            .textColumn("Status", User::status)
            .render();

        assertTrue(grid.contains("data-ujfe-grid-filters=\"true\""));
        assertTrue(grid.contains("data-ujfe-grid-filter=\"status\""));
        assertTrue(grid.contains("data-ujfe-column-priority=\"1\""));
        assertThrows(IllegalArgumentException.class, () -> dataGrid(List.of("x")).filter("bad.name", "Bad", "x"));

        String audio = audioPlayer("/assets/call.mp3")
            .duration("00:42")
            .loadingText("Loading recording")
            .waveform(List.of(10, 40, 20))
            .metadata("Caller", "1001")
            .render();
        assertTrue(audio.contains("data-ujfe-audio-timeline=\"true\""));
        assertTrue(audio.contains("data-ujfe-duration=\"00:42\""));
        assertTrue(audio.contains("data-ujfe-audio-waveform=\"10,40,20\""));
        assertTrue(audio.contains("data-ujfe-loading-text=\"Loading recording\""));

        assertFalse(audioPlayer("/assets/call.mp3").timeline(false).render()
            .contains("data-ujfe-audio-timeline"));
        assertThrows(IllegalArgumentException.class, () -> audioPlayer("/x.mp3").waveform(List.of()));
        assertThrows(IllegalArgumentException.class, () -> audioPlayer("/x.mp3").waveform(List.of(101)));
    }

    @Test
    void componentPreviewAndHtmlSnapshotsSupportToolingWorkflows() {
        String preview = componentPreview("Login")
            .story("default", p("Ready"))
            .story("error", p("Failed"))
            .render();

        assertTrue(preview.contains("data-ujfe-component-preview=\"true\""));
        assertTrue(preview.contains("data-ujfe-preview-story=\"default\""));

        HtmlSnapshot snapshot = HtmlSnapshot.capture(div().child("A").child(span("B")));
        assertEquals("<div>A<span>B</span></div>", snapshot.normalizedHtml());
        assertEquals(64, snapshot.fingerprint().length());
        assertFalse(HtmlSnapshot.capture(script().src("/assets/app.js")).containsInlineExecutableScript());
        assertFalse(HtmlSnapshot.capture(script().type("application/json").child("{}")).containsInlineExecutableScript());
        assertTrue(HtmlSnapshot.capture(unsafeHtml("<script>alert(1)</script>")).containsInlineExecutableScript());
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread()
                .interrupt();
        }
    }

    private static final class PasskeyRequest {
        private final String challenge = "abc";
        private final String userId = "user";
        @SuppressWarnings("unused")
        private static final String IGNORED = "static";
    }

    private static final class PasskeyResponse {
        private final String credentialId = "cred";
    }

    private static final class BrowserFailure {
        private final String code = "NotAllowedError";
    }

    private static final class LoginProps {
        private final String continueUrl;

        private LoginProps(String continueUrl) {
            this.continueUrl = continueUrl;
        }
    }

    private static final class User {
        private final String id;
        private final String name;
        private final String status;

        private User(String id, String name, String status) {
            this.id = id;
            this.name = name;
            this.status = status;
        }

        private String name() {
            return name;
        }

        private String status() {
            return status;
        }
    }
}
