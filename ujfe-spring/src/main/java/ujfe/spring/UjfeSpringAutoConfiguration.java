package ujfe.spring;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;
import ujfe.live.LiveSession;
import ujfe.live.LiveSessionConfig;
import ujfe.router.Router;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnBean(Router.class)
@EnableConfigurationProperties(UjfeSpringProperties.class)
public class UjfeSpringAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public LiveSessionConfig ujfeLiveSessionConfig(UjfeSpringProperties properties) {
        LiveSessionConfig.Builder builder = LiveSessionConfig.builder();
        if (properties.isEnabled()) {
            builder.enableDevelopmentErrorDetailsUnsafe();
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public LiveSession ujfeLiveSession(Router router, LiveSessionConfig config) {
        return new LiveSession(router, config);
    }

    @Bean
    @ConditionalOnMissingBean
    public UjfeSpringHandler ujfeSpringHandler(LiveSession liveSession) {
        return new UjfeSpringHandler(liveSession);
    }

    @Bean
    @ConditionalOnMissingBean
    public UjfeSpringHandlerMapping ujfeSpringHandlerMapping(Router router, UjfeSpringHandler handler) {
        return new UjfeSpringHandlerMapping(router, handler);
    }
}
