package ujfe.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import ujfe.live.SecurityHeadersConfig;

@ConfigurationProperties("ujfe.security.headers")
public final class UjfeSpringSecurityHeadersProperties {
    private Boolean enabled;
    private String xContentTypeOptions;
    private String xFrameOptions;
    private String referrerPolicy;
    private String contentSecurityPolicy;
    private String permissionsPolicy;

    SecurityHeadersConfig toSecurityHeadersConfig() {
        SecurityHeadersConfig.Builder builder = SecurityHeadersConfig.builder();
        if (enabled != null) {
            builder.enabled(enabled);
        }
        if (xContentTypeOptions != null) {
            builder.header(SecurityHeadersConfig.X_CONTENT_TYPE_OPTIONS, xContentTypeOptions);
        }
        if (xFrameOptions != null) {
            builder.header(SecurityHeadersConfig.X_FRAME_OPTIONS, xFrameOptions);
        }
        if (referrerPolicy != null) {
            builder.header(SecurityHeadersConfig.REFERRER_POLICY, referrerPolicy);
        }
        if (contentSecurityPolicy != null) {
            builder.header(SecurityHeadersConfig.CONTENT_SECURITY_POLICY, contentSecurityPolicy);
        }
        if (permissionsPolicy != null) {
            builder.header(SecurityHeadersConfig.PERMISSIONS_POLICY, permissionsPolicy);
        }
        return builder.build();
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getXContentTypeOptions() {
        return xContentTypeOptions;
    }

    public void setXContentTypeOptions(String xContentTypeOptions) {
        this.xContentTypeOptions = xContentTypeOptions;
    }

    public String getXFrameOptions() {
        return xFrameOptions;
    }

    public void setXFrameOptions(String xFrameOptions) {
        this.xFrameOptions = xFrameOptions;
    }

    public String getReferrerPolicy() {
        return referrerPolicy;
    }

    public void setReferrerPolicy(String referrerPolicy) {
        this.referrerPolicy = referrerPolicy;
    }

    public String getContentSecurityPolicy() {
        return contentSecurityPolicy;
    }

    public void setContentSecurityPolicy(String contentSecurityPolicy) {
        this.contentSecurityPolicy = contentSecurityPolicy;
    }

    public String getPermissionsPolicy() {
        return permissionsPolicy;
    }

    public void setPermissionsPolicy(String permissionsPolicy) {
        this.permissionsPolicy = permissionsPolicy;
    }
}
