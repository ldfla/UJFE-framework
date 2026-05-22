package ujfe.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static ujfe.core.UI.*;

final class AdminPrimitivesTest {
    @Test
    void routeBuilderEncodesParametersQueryFragmentAndContinueUrl() {
        String href = route("/flows/{id}/versions/{version}")
            .with("id", "flow 1")
            .with("version", "v/2")
            .query("tab", "review")
            .query("tag", List.of("ivr", "prod"))
            .continueUrl("/dashboard?next=/flows")
            .fragment("history")
            .build();

        assertEquals("/flows/flow%201/versions/v%2F2?tab=review&tag=ivr&tag=prod&continue=%2Fdashboard%3Fnext%3D%2Fflows#history", href);
        assertEquals("<a href=\"/users/42\">User</a>", route("/users/{id}")
            .with("id", 42)
            .link("User")
            .render());
    }

    @Test
    void routeBuilderRejectsUnsafeContinueUrlAndMissingParameters() {
        assertThrows(IllegalArgumentException.class, () -> route("/login").continueUrl("https://evil.test"));
        assertThrows(IllegalArgumentException.class, () -> route("/login").continueUrl("//evil.test"));
        assertThrows(IllegalStateException.class, () -> route("/flows/{id}").build());
    }

    @Test
    void routeBuilderCoversOptionalBranchesAndValidation() {
        String html = route("recordings/{id}")
            .with("id", "abc")
            .query("tag", (Object) null)
            .query("filter", Arrays.asList("recent", null, "mine"))
            .fragment("")
            .link(strong("Recording"))
            .render();

        assertEquals("<a href=\"/recordings/abc?filter=recent&amp;filter=mine\"><strong>Recording</strong></a>", html);
        assertEquals("/plain", route("/plain").toString());
        assertThrows(IllegalArgumentException.class, () -> route("/bad?query"));
        assertThrows(IllegalArgumentException.class, () -> route("/bad//path"));
        assertThrows(IllegalArgumentException.class, () -> route("/x").with("bad.name", "1"));
        assertThrows(IllegalArgumentException.class, () -> route("/x").fragment("bad\nfragment"));
        assertThrows(IllegalStateException.class, () -> route("/bad/{id").build());
    }

    @Test
    void actionButtonRendersHttpFormWithCsrfConfirmationAndMethodOverride() {
        String html = actionButton("Rollback", "/flows/1/rollback")
            .method("delete")
            .csrf("token")
            .confirm("Rollback flow?")
            .loadingText("Rolling back")
            .refresh("/flows/1")
            .variant("danger")
            .render();

        assertTrue(html.contains("<form method=\"post\" action=\"/flows/1/rollback\""));
        assertTrue(html.contains("data-ujfe-confirm=\"Rollback flow?\""));
        assertTrue(html.contains("<input type=\"hidden\" name=\"_csrf\" value=\"token\">"));
        assertTrue(html.contains("<input type=\"hidden\" name=\"_method\" value=\"DELETE\">"));
        assertTrue(html.contains("<button data-ujfe-action-button=\"true\""));
        assertTrue(html.contains("data-ujfe-variant=\"danger\""));
    }

    @Test
    void actionButtonRendersLiveButtonWithEventRegistrationAndPermissionState() {
        UjfeContext context = UjfeContext.builder()
            .eventRegistrar(handler -> "event-1")
            .build();

        String html = actionButton("Deploy", () -> {
            })
            .permission(false)
            .render(context);

        assertTrue(html.contains("data-ujfe-action-kind=\"live\""));
        assertTrue(html.contains("data-ujfe-event-click=\"event-1\""));
        assertTrue(html.contains("data-ujfe-permission=\"denied\""));
        assertTrue(html.contains("disabled"));
    }

    @Test
    void actionFormRendersFieldsCsrfStatusAndLiveSubmitHandler() {
        UjfeContext context = UjfeContext.builder()
            .eventRegistrar(handler -> "submit-1")
            .build();

        String html = actionForm("/users")
            .csrf("csrf-1")
            .statusTarget("#status")
            .resultTarget("#users")
            .onSubmit(() -> {
            })
            .child(field("email")
                .label("Email")
                .text()
                .required(true))
            .render(context);

        assertTrue(html.contains("data-ujfe-action-form=\"true\""));
        assertTrue(html.contains("data-ujfe-status-target=\"#status\""));
        assertTrue(html.contains("data-ujfe-event-submit=\"submit-1\""));
        assertTrue(html.contains("<input type=\"hidden\" name=\"_csrf\" value=\"csrf-1\">"));
        assertTrue(html.contains("<label for=\"ujfe-field-email\">Email</label>"));
    }

    @Test
    void actionHelpersCoverGetFormsPatchOverrideAndValidationBranches() {
        String getForm = actionForm("/search")
            .method("get")
            .confirm("Search?")
            .loadingText("Searching")
            .refresh("/search")
            .css("toolbar-form")
            .disabled(true)
            .children(inputSearch().name("q"), button("Search").type("submit"))
            .render();

        assertTrue(getForm.contains("method=\"get\""));
        assertTrue(getForm.contains("data-ujfe-confirm=\"Search?\""));
        assertTrue(getForm.contains("class=\"toolbar-form\""));
        assertFalse(getForm.contains("_csrf"));

        String patchButton = actionButton("Rename", "/users/1")
            .id("rename-user")
            .method("patch")
            .csrf("csrf", "abc")
            .resultTarget("#result")
            .css("primary")
            .disabled(true)
            .render();
        assertTrue(patchButton.contains("id=\"rename-user\""));
        assertTrue(patchButton.contains("name=\"_method\" value=\"PATCH\""));
        assertTrue(patchButton.contains("data-ujfe-result-target=\"#result\""));
        assertTrue(patchButton.contains("class=\"primary\""));

        String live = actionButton("Ping", () -> {
            })
            .confirm("Ping?")
            .loadingText("Pinging")
            .resultTarget("#ping")
            .variant("secondary")
            .render(UjfeContext.builder().eventRegistrar(handler -> "ping-1").build());
        assertTrue(live.contains("data-ujfe-confirm=\"Ping?\""));
        assertTrue(live.contains("data-ujfe-loading-text=\"Pinging\""));
        assertTrue(live.contains("data-ujfe-variant=\"secondary\""));

        assertThrows(IllegalArgumentException.class, () -> actionForm("/x").method("po st"));
        assertThrows(IllegalArgumentException.class, () -> actionButton("Bad", "/x").refresh("https://evil.test"));
    }

    @Test
    void dataGridRendersSemanticTableSelectionActionsAndPagination() {
        String html = dataGrid(List.of(new User("1", "Ada", "active"), new User("2", "Linus", "locked")))
            .caption("Users")
            .rowKey("userId", User::id)
            .selectable(true)
            .sortableTextColumn("name", "Name", User::name)
            .column("Status", user -> badge(user.status(), user.status()))
            .rowAction("Open", user -> route("/users/{id}").with("id", user.id()).build())
            .sortedBy("name", false)
            .pagination(1, 20, 2)
            .render();

        assertTrue(html.contains("data-ujfe-data-grid=\"true\""));
        assertTrue(html.contains("<caption>Users</caption>"));
        assertTrue(html.contains("aria-sort=\"ascending\""));
        assertTrue(html.contains("name=\"userId\""));
        assertTrue(html.contains("value=\"1\""));
        assertTrue(html.contains("<span data-ujfe-badge=\"true\" data-ujfe-tone=\"active\">active</span>"));
        assertTrue(html.contains("<a href=\"/users/1\">Open</a>"));
        assertTrue(html.contains("data-ujfe-pagination=\"true\""));
    }

    @Test
    void dataGridRendersEmptyState() {
        String html = dataGrid(List.<String>of())
            .textColumn("Name", value -> value)
            .emptyState("No users", "Invite the first user.")
            .render();

        assertTrue(html.contains("data-ujfe-empty-state=\"true\""));
        assertTrue(html.contains("<strong>No users</strong>"));
        assertTrue(html.contains("<p>Invite the first user.</p>"));
    }

    @Test
    void dataGridCoversMinimalRowsAndValidationBranches() {
        String html = dataGrid(List.of("one"))
            .css("dense-grid")
            .textColumn("Name", value -> value)
            .render();

        assertTrue(html.contains("class=\"dense-grid\""));
        assertTrue(html.contains("<td>one</td>"));
        assertFalse(html.contains("data-ujfe-pagination"));

        String empty = dataGrid(List.<String>of())
            .textColumn("Name", value -> value)
            .emptyState("Nothing")
            .render();
        assertTrue(empty.contains("<strong>Nothing</strong>"));
        assertFalse(empty.contains("Invite"));

        assertThrows(IllegalStateException.class, () -> dataGrid(List.of("one")).render());
        assertThrows(IllegalArgumentException.class, () -> dataGrid(List.of("one"))
            .textColumn("Name", value -> value)
            .pagination(0, 10, 1));
        assertThrows(IllegalArgumentException.class, () -> dataGrid(List.of("one"))
            .textColumn("Name", value -> value)
            .pagination(1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> dataGrid(List.of("one"))
            .textColumn("Name", value -> value)
            .pagination(1, 10, -1));
    }

    @Test
    void formFieldRendersAccessibleErrorHintAndSpecialTypes() {
        String html = field("configuration.timeout")
            .label("Timeout")
            .number()
            .value(30)
            .hint("Seconds")
            .error("Too low")
            .required(true)
            .render();

        assertTrue(html.contains("data-ujfe-field=\"configuration.timeout\""));
        assertTrue(html.contains("id=\"ujfe-field-configuration-timeout\""));
        assertTrue(html.contains("aria-invalid=\"true\""));
        assertTrue(html.contains("aria-describedby=\"ujfe-field-configuration-timeout-hint ujfe-field-configuration-timeout-error\""));
        assertTrue(html.contains("role=\"alert\""));

        String switchHtml = field("enabled")
            .label("Enabled")
            .switchField()
            .checked(true)
            .render();
        assertTrue(switchHtml.contains("role=\"switch\""));
        assertTrue(switchHtml.contains("aria-checked=\"true\""));
    }

    @Test
    void formFieldCoversSupportedKindsAndValidationBranches() {
        String select = field("provider")
            .label("Provider")
            .select()
            .value("ari")
            .option("ari", "ARI")
            .option("mock", "Mock")
            .disabled(true)
            .render();
        assertTrue(select.contains("<select id=\"ujfe-field-provider\" name=\"provider\" disabled>"));
        assertTrue(select.contains("<option value=\"ari\" selected>ARI</option>"));

        String text = field("username")
            .id("user-name")
            .label("User")
            .text()
            .placeholder("name")
            .autocomplete("username")
            .inputMode("text")
            .readonly(true)
            .render();
        assertTrue(text.contains("id=\"user-name\""));
        assertTrue(text.contains("placeholder=\"name\""));
        assertTrue(text.contains("autocomplete=\"username\""));
        assertTrue(text.contains("readonly"));

        assertTrue(field("secret").label("Secret").secret().render()
            .contains("data-ujfe-secret-field=\"true\""));
        assertTrue(field("password").password().render()
            .contains("type=\"password\""));
        assertTrue(field("notes").textarea().rows(2).value("hi").render()
            .contains("<textarea id=\"ujfe-field-notes\" name=\"notes\" rows=\"2\">hi</textarea>"));
        assertTrue(field("json").json().value("{ }").render()
            .contains("data-ujfe-json-field=\"true\""));
        assertTrue(field("avatar").file().accept("image/*").render()
            .contains("accept=\"image/*\""));
        assertTrue(field("recording").audioFile().render()
            .contains("accept=\"audio/*\""));
        assertTrue(field("date").date().render()
            .contains("type=\"date\""));
        assertTrue(field("time").time().render()
            .contains("type=\"time\""));
        assertTrue(field("created").dateTimeLocal().render()
            .contains("type=\"datetime-local\""));
        assertTrue(field("terms").label("Terms").checkbox().checked(true).render()
            .contains("type=\"checkbox\""));

        assertThrows(IllegalArgumentException.class, () -> field("notes").textarea().rows(0));
        assertThrows(IllegalArgumentException.class, () -> field(" ").render());
    }

    @Test
    void objectEditorRendersSchemaFieldsErrorsAndJsonPreview() {
        ObjectSchema schema = objectSchema()
            .title("Node config");
        schema.field("prompt", "Prompt", ObjectSchema.FieldType.TEXT)
            .required(true)
            .done()
            .field("timeout", "Timeout", ObjectSchema.FieldType.NUMBER)
            .defaultValue(20)
            .done()
            .field("enabled", "Enabled", ObjectSchema.FieldType.BOOLEAN)
            .defaultValue(true)
            .done()
            .field("provider", "Provider", ObjectSchema.FieldType.SELECT)
            .option("ari", "ARI")
            .option("mock", "Mock");

        String html = objectEditor(schema)
            .value("prompt", "hello")
            .value("timeout", 30)
            .error("prompt", "Required")
            .jsonPreview(true)
            .render();

        assertTrue(html.contains("data-ujfe-object-editor=\"true\""));
        assertTrue(html.contains("<legend>Node config</legend>"));
        assertTrue(html.contains("name=\"prompt\""));
        assertTrue(html.contains("Required"));
        assertTrue(html.contains("\"timeout\": 30"));
        assertTrue(html.contains("<option value=\"ari\">ARI</option>"));
    }

    @Test
    void objectEditorCoversSecretJsonNullValuesAndEscapingBranches() {
        ObjectSchema schema = objectSchema().title("Advanced");
        schema.field("token", "Token", ObjectSchema.FieldType.SECRET).done()
            .field("payload", "Payload", ObjectSchema.FieldType.JSON).done()
            .field("count", "Count", ObjectSchema.FieldType.NUMBER).defaultValue("NaN").done()
            .field("enabled", "Enabled", ObjectSchema.FieldType.BOOLEAN);

        String html = objectEditor(schema)
            .values(null)
            .value("token", "a\"b")
            .value("payload", "{\n  \"x\": true\n}")
            .jsonPreview(true)
            .render();

        assertTrue(html.contains("data-ujfe-secret-field=\"true\""));
        assertTrue(html.contains("data-ujfe-json-field=\"true\""));
        assertTrue(html.contains("\"token\": \"a\\\"b\""));
        assertTrue(html.contains("\"count\": \"NaN\""));
        assertTrue(html.contains("\"enabled\": false"));

        assertThrows(IllegalArgumentException.class, () -> objectEditor(schema).error("token", " "));
        assertThrows(IllegalArgumentException.class, () -> objectSchema().field(" ", "Bad", ObjectSchema.FieldType.TEXT));
    }

    @Test
    void clientModuleRealtimeAudioAndDashboardPrimitivesRenderContracts() {
        String module = clientModule("passkey-login")
            .src("/assets/security/passkey-login.js")
            .action("authenticate", PasskeyRequest.class)
            .target("#login")
            .errorTarget("#errors")
            .render();

        assertTrue(module.startsWith("<script type=\"module\" src=\"/assets/security/passkey-login.js\""));
        assertTrue(module.contains("data-ujfe-client-module=\"passkey-login\""));
        assertTrue(module.contains(PasskeyRequest.class.getName()));

        String realtime = realtime("/events/dashboard")
            .onEvent(DashboardEvent.class)
            .heartbeat(Duration.ofSeconds(10))
            .pollingFallback(Duration.ofSeconds(15))
            .connectionStateTarget("#connection")
            .render();
        assertTrue(realtime.contains("data-ujfe-realtime=\"true\""));
        assertTrue(realtime.contains("data-ujfe-event-type=\"" + DashboardEvent.class.getName() + "\""));
        assertTrue(realtime.contains("data-ujfe-polling-fallback-ms=\"15000\""));

        String audio = audioPlayer("/assets/recordings/call.mp3")
            .title("Call recording")
            .metadata("Duration", "00:42")
            .downloadAllowed(true)
            .render();
        assertTrue(audio.contains("data-ujfe-audio-player=\"true\""));
        assertTrue(audio.contains("<audio preload=\"metadata\" src=\"/assets/recordings/call.mp3\""));
        assertTrue(audio.contains("data-ujfe-loading-text=\"Loading audio\""));
        assertTrue(audio.contains("controls"));
        assertTrue(audio.contains("data-ujfe-audio-download=\"true\""));

        assertTrue(metricTile("Calls", "12", "Live", "ok").render()
            .contains("data-ujfe-metric-tile=\"true\""));
        assertTrue(statusIndicator("Asterisk", "ok").render()
            .contains("data-ujfe-status-indicator=\"ok\""));
        assertTrue(toast("Saved", "success").render()
            .contains("role=\"status\""));
    }

    @Test
    void clientRealtimeAudioAndLayoutPrimitivesCoverOptionalBranches() {
        String module = clientModule("dashboard")
            .defer(false)
            .render();
        assertTrue(module.contains("data-ujfe-client-actions=\"[]\""));
        assertFalse(module.contains("defer"));
        assertThrows(IllegalArgumentException.class, () -> clientModule("bad name"));

        String realtime = realtime("/events/calls")
            .reconnectDelay(Duration.ofSeconds(5))
            .debounce(Duration.ofMillis(250))
            .payloadTarget("#payload")
            .render();
        assertTrue(realtime.contains("data-ujfe-reconnect-delay-ms=\"5000\""));
        assertTrue(realtime.contains("data-ujfe-debounce-ms=\"250\""));
        assertTrue(realtime.contains("data-ujfe-payload-target=\"#payload\""));
        assertThrows(IllegalArgumentException.class, () -> realtime("https://evil.test"));
        assertThrows(IllegalArgumentException.class, () -> realtime("/events").heartbeat(Duration.ofMillis(-1)));

        String audio = audioPlayer("/assets/a.mp3")
            .preload("none")
            .downloadAllowed(true)
            .downloadUrl("/assets/download/a.mp3")
            .errorText("No audio")
            .render();
        assertTrue(audio.contains("preload=\"none\""));
        assertTrue(audio.contains("href=\"/assets/download/a.mp3\""));
        assertTrue(audio.contains(">No audio</audio>"));

        assertTrue(sectionHeader("Users", null).render()
            .contains("data-ujfe-section-header=\"true\""));
        assertTrue(toolbar(button("A"), button("B")).render()
            .contains("role=\"toolbar\""));
        assertTrue(splitView(p("List"), p("Detail")).render()
            .contains("data-ujfe-split-view=\"true\""));
        assertTrue(modalDialog("Confirm", p("Body")).render()
            .contains("aria-modal=\"true\""));
        assertTrue(badge("New").render()
            .contains("data-ujfe-tone=\"neutral\""));
        assertTrue(metricTile("Calls", "0").render()
            .contains("data-ujfe-tone=\"neutral\""));
    }

    @Test
    void graphBuilderAndWorkflowPrimitivesRenderAdministrativeContracts() {
        String graph = graphBuilder("ivr-flow")
            .size(400, 220)
            .node("start", "Start", "entry", 80, 80)
            .node("menu", "Menu", "ivr", 240, 80)
            .edge("start", "menu", "next", 1)
            .minimap(true)
            .render();

        assertTrue(graph.contains("data-ujfe-graph-builder=\"true\""));
        assertTrue(graph.contains("viewBox=\"0 0 400 220\""));
        assertTrue(graph.contains("data-ujfe-node-id=\"start\""));
        assertTrue(graph.contains("data-ujfe-edge-order=\"1\""));
        assertTrue(graph.contains("data-ujfe-graph-list=\"true\""));

        String workflow = workflowTimeline()
            .title("Deploy")
            .currentStatus("running")
            .step("Published", "done")
            .step("Asterisk deploy", "running", "Applying configuration")
            .render();

        assertTrue(workflow.contains("data-ujfe-workflow=\"true\""));
        assertTrue(workflow.contains("data-ujfe-workflow-status=\"running\""));
        assertTrue(workflow.contains("data-ujfe-workflow-step-status=\"done\""));
        assertTrue(workflow.contains("<p>Applying configuration</p>"));

        assertTrue(workflowBanner("failed", "Rollback required").render()
            .contains("data-ujfe-workflow-banner=\"true\""));
        assertTrue(diffSummary("Changes", 1, 2, 3).render()
            .contains("data-ujfe-diff-summary=\"true\""));
    }

    @Test
    void graphAndWorkflowCoverOptionalBranchesAndValidation() {
        String graph = graphBuilder("readonly-flow")
            .panZoom(false)
            .readonly(true)
            .node("a", "A", 40, 40)
            .edge("a", "missing")
            .render();

        assertTrue(graph.contains("data-ujfe-pan-zoom=\"false\""));
        assertTrue(graph.contains("data-ujfe-readonly=\"true\""));
        assertTrue(graph.contains("data-ujfe-edge-to=\"missing\""));
        assertFalse(graph.contains("<line"));

        String workflow = workflowTimeline()
            .step("Draft", "pending")
            .render();
        assertTrue(workflow.contains("<strong>Draft</strong>"));
        assertFalse(workflow.contains("<h2>"));

        assertThrows(IllegalArgumentException.class, () -> graphBuilder("bad id!"));
        assertThrows(IllegalArgumentException.class, () -> graphBuilder("g").size(0, 10));
        assertThrows(IllegalArgumentException.class, () -> graphBuilder("g").edge("a", "b", "bad", 0));
        assertThrows(IllegalArgumentException.class, () -> workflowTimeline().title(" "));
    }

    @Test
    void base64UrlAndRenderModeSupportBrowserAndCacheContracts() {
        String encoded = Base64Url.encode(new byte[]{1, 2, 3, 4});

        assertArrayEquals(new byte[]{1, 2, 3, 4}, Base64Url.decode(encoded));
        assertFalse(encoded.contains("="));

        RenderMode mode = RenderMode.staleWhileRevalidate(Duration.ofSeconds(30), Duration.ofMinutes(5))
            .scope(RenderMode.CacheScope.PRIVATE)
            .revalidateOn("flow.published");

        assertEquals(RenderMode.Kind.STALE_WHILE_REVALIDATE, mode.kind());
        assertEquals("private, max-age=30, stale-while-revalidate=300", mode.cacheControlHeader());
        assertEquals("flow.published", mode.revalidateEvent().orElseThrow());
        assertEquals("no-store", RenderMode.dynamic().cacheControlHeader());
        assertEquals("public, max-age=60", RenderMode.staticPage(Duration.ofSeconds(60)).cacheControlHeader());
        assertEquals("private, max-age=15", RenderMode.cached(Duration.ofSeconds(15)).cacheControlHeader());
        assertEquals("private, max-age=20", RenderMode.staticShell(Duration.ofSeconds(20)).cacheControlHeader());
        assertTrue(RenderMode.cached(Duration.ofSeconds(1)).revalidateEvent().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> RenderMode.cached(Duration.ofSeconds(-1)));
        assertThrows(IllegalArgumentException.class, () -> RenderMode.cached(Duration.ofSeconds(1)).revalidateOn("bad event"));
    }

    private static final class PasskeyRequest {
    }

    private static final class DashboardEvent {
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

        private String id() {
            return id;
        }

        private String name() {
            return name;
        }

        private String status() {
            return status;
        }
    }
}
