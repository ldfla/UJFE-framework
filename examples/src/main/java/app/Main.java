package app;

import app.pages.DocumentationPage;
import app.pages.MainPage;
import app.pages.RuntimeActionsPage;
import ujfe.http.UjfeServer;
import ujfe.http.UjfeServerConfig;
import ujfe.live.LiveSession;
import ujfe.live.LiveSessionConfig;
import ujfe.router.Router;
import ujfe.runtime.action.RuntimeActionRegistry;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws InterruptedException {
        var appTheme = new AppTheme();
        var runtimeActionsPage = new RuntimeActionsPage();
        var router = new Router()
                .register(new MainPage())
                .register(new DocumentationPage(appTheme))
                .register(runtimeActionsPage);

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
