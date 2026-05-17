package ujfe.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ujfe.errors.development-details")
public final class UjfeSpringProperties {
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
