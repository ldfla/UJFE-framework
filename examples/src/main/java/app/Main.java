package app;

import app.pages.DocumentationPage;
import app.pages.MainPage;
import ujfe.http.UjfeServer;
import ujfe.http.UjfeServerConfig;
import ujfe.live.LiveSession;
import ujfe.router.Router;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws InterruptedException {
        AppTheme appTheme = new AppTheme();
        Router router = new Router()
                .register(new MainPage())
                .register(new DocumentationPage(appTheme));

        LiveSession liveSession = new LiveSession(router, appTheme::cssTheme, true);
        UjfeServer server = new UjfeServer(
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
