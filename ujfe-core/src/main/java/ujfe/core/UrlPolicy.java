package ujfe.core;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Configurable URL scheme policy for HTML URL-bearing attributes.
 *
 * <p>The default policy allows {@code https} and blocks dangerous schemes
 * ({@code javascript}, {@code vbscript}, and {@code data} except for
 * explicitly allowed image MIME types). Applications can opt into
 * additional schemes through the builder.</p>
 *
 * <p>Relative references ({@code /path}, {@code ./path}, {@code ../path},
 * {@code #fragment}, and scheme-less paths) are always allowed.</p>
 */
public final class UrlPolicy {
    private static volatile UrlPolicy defaultPolicy = new UrlPolicy(new Builder());

    private final Set<String> allowedSchemes;
    private final boolean allowDataImageUrls;

    private UrlPolicy(Builder builder) {
        this.allowedSchemes = Set.copyOf(builder.allowedSchemes);
        this.allowDataImageUrls = builder.allowDataImageUrls;
    }

    /**
     * Returns the current default policy used by {@link SafeUrl#sanitize(String)}.
     */
    public static UrlPolicy getDefault() {
        return defaultPolicy;
    }

    /**
     * Sets the default policy used by {@link SafeUrl#sanitize(String)}.
     * Call this at application startup to configure URL handling globally.
     */
    public static void setDefault(UrlPolicy policy) {
        defaultPolicy = Objects.requireNonNull(policy, "policy");
    }

    /**
     * Creates a new builder with the secure defaults: only {@code https}
     * is allowed, and {@code data:image/*} URLs are allowed.
     */
    public static Builder builder() {
        return new Builder();
    }

    Set<String> allowedSchemes() {
        return allowedSchemes;
    }

    boolean allowDataImageUrls() {
        return allowDataImageUrls;
    }

    public static final class Builder {
        private final Set<String> allowedSchemes = new LinkedHashSet<>();
        private boolean allowDataImageUrls = true;

        private Builder() {
            allowedSchemes.add("https");
        }

        /**
         * Allows the {@code http} scheme in URL attributes.
         */
        public Builder allowHttp() {
            allowedSchemes.add("http");
            return this;
        }

        /**
         * Allows the {@code mailto} scheme in URL attributes.
         */
        public Builder allowMailto() {
            allowedSchemes.add("mailto");
            return this;
        }

        /**
         * Allows the {@code tel} scheme in URL attributes.
         */
        public Builder allowTel() {
            allowedSchemes.add("tel");
            return this;
        }

        /**
         * Allows a custom scheme in URL attributes. The scheme name must
         * not be a dangerous scheme ({@code javascript}, {@code vbscript}).
         */
        public Builder allowScheme(String scheme) {
            Objects.requireNonNull(scheme, "scheme");
            String lower = scheme.toLowerCase(java.util.Locale.ROOT);
            if ("javascript".equals(lower) || "vbscript".equals(lower)) {
                throw new IllegalArgumentException("Cannot allow dangerous scheme: " + scheme);
            }
            allowedSchemes.add(lower);
            return this;
        }

        /**
         * Controls whether {@code data:image/*} URLs are allowed. Defaults
         * to {@code true}. Note: MIME prefix validation does not prove that
         * decoded bytes are a valid image.
         */
        public Builder allowDataImageUrls(boolean allow) {
            this.allowDataImageUrls = allow;
            return this;
        }

        public UrlPolicy build() {
            return new UrlPolicy(this);
        }
    }
}
