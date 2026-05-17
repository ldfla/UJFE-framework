package app;

import app.pages.*;
import ujfe.http.UjfeServer;
import ujfe.http.UjfeServerConfig;
import ujfe.live.LiveSession;
import ujfe.live.LiveSessionConfig;
import ujfe.router.Router;
import ujfe.router.source.ManualRouteSource;
import ujfe.runtime.action.RuntimeActionRegistry;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws InterruptedException {
        var appTheme = new AppTheme();
        var runtimeActionsPage = new RuntimeActionsPage();
        var routes = new ManualRouteSource()
            .register("/", () -> new MainPage(appTheme))
            .register("/docs", () -> new DocumentationPage(appTheme))
            .register("/signals", SignalsPage::new)
            .register("/lifecycle", LifecyclePage::new)
            .register("/runtime-actions", () -> runtimeActionsPage);
        var router = new Router()
            .register(routes);

        RuntimeActionRegistry actions = RuntimeActionsPage.sampleRegistry();

        var liveConfig = LiveSessionConfig.builder()
            .themeSupplier(appTheme::cssTheme)
            .devToolsEnabled(true)
            .lang("en")
            .title("UJFE Example")
            .runtimeActions(actions)
            .build();
        var liveSession = new LiveSession(router, liveConfig);
        var server = new UjfeServer(
            UjfeServerConfig.builder()
                .host("0.0.0.0")
                .port(8080)
                .build(),
            liveSession
        );

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
        System.out.println("UJFE example running at http://localhost:8080");
        server.blockUntilShutdown();
    }
}
