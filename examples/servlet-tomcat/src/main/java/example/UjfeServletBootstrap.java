package example;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.annotation.WebListener;
import ujfe.live.LiveSessionConfig;
import ujfe.router.Router;
import ujfe.router.source.ManualRouteSource;
import ujfe.servlet.UjfeServlet;

@WebListener
public final class UjfeServletBootstrap implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        var routes = new ManualRouteSource()
                .register("/home", HomePage::new);
        var router = new Router().register(routes);
        var config = LiveSessionConfig.builder()
                .title("UJFE Servlet Tomcat Example")
                .devToolsEnabled(true)
                .build();

        ServletContext context = event.getServletContext();
        ServletRegistration.Dynamic servlet = context.addServlet("ujfe", new UjfeServlet(router, config));
        servlet.setLoadOnStartup(1);
        servlet.addMapping("/home", "/_ujfe/*");
    }
}
