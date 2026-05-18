package app;

import app.pages.*;
import ujfe.http.UjfeServer;
import ujfe.http.UjfeServerConfig;
import ujfe.live.LiveSession;
import ujfe.live.LiveSessionConfig;
import ujfe.router.Router;
import ujfe.router.source.ManualRouteSource;
import ujfe.runtime.action.RuntimeActionRegistry;

import static ujfe.core.UI.meta;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws InterruptedException {
        var appTheme = new AppTheme();
        var runtimeActionsPage = new RuntimeActionsPage(appTheme);
        var routes = new ManualRouteSource()
            .register("/", () -> new MainPage(appTheme))
            .register("/docs", () -> new DocumentationPage(appTheme))
            .register("/forms", () -> new FormsPage(appTheme))
            .register("/signals", () -> new SignalsPage(appTheme))
            .register("/lifecycle", () -> new LifecyclePage(appTheme))
            .register("/runtime-actions", () -> runtimeActionsPage);
        var router = new Router()
            .register(routes);

        RuntimeActionRegistry actions = RuntimeActionsPage.sampleRegistry();

        var liveConfig = LiveSessionConfig.builder()
            .themeSupplier(appTheme::cssTheme)
            .devToolsEnabled(true)
            .lang("en")
            .title("UJFE Example")
            .head(meta()
                .attr("name", "description")
                .attr("content", "Interactive UJFE example application for server-rendered Java UI."))
            .runtimeActions(actions)
            .allowClientCookie("ujfe_demo")
            .allowLocalStorageKey("ujfe.theme")
            .allowSessionStorageKey("ujfe.tab")
            .build();
        var liveSession = new LiveSession(router, liveConfig);
        var server = new UjfeServer(
            UjfeServerConfig.builder()
                .host("0.0.0.0")
                .port(8080)
                .build(),
            liveSession
        );

        Runtime.getRuntime()
            .addShutdownHook(new Thread(server::stop));
        server.start();
        System.out.println("UJFE example running at http://localhost:8080");
        server.blockUntilShutdown();
    }
}
