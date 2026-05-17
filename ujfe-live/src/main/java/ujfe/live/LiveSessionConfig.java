package ujfe.live;

import ujfe.core.Node;
import ujfe.html.CssTheme;
import ujfe.runtime.action.RuntimeActionRegistry;

import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

import static ujfe.html.UI.link;

public final class LiveSessionConfig {
    public static final int DEFAULT_INTERNAL_ENDPOINT_RATE_LIMIT_CAPACITY = 120;
    public static final int DEFAULT_INTERNAL_ENDPOINT_RATE_LIMIT_REFILL_TOKENS = 60;
    public static final Duration DEFAULT_INTERNAL_ENDPOINT_RATE_LIMIT_REFILL_PERIOD = Duration.ofMinutes(1);

    private final Supplier<CssTheme> themeSupplier;
    private final CssMode cssMode;
    private final boolean devToolsEnabled;
    private final String lang;
    private final String title;
    private final List<Node> headNodes;
    private final RuntimeActionRegistry runtimeActions;
    private final boolean csrfProtectionDisabled;
    private final boolean internalEndpointRateLimitingEnabled;
    private final int internalEndpointRateLimitCapacity;
    private final int internalEndpointRateLimitRefillTokens;
    private final Duration internalEndpointRateLimitRefillPeriod;
    private final Set<String> trustedProxyAddresses;

    private LiveSessionConfig(Builder builder) {
        this.themeSupplier = builder.themeSupplier;
        this.cssMode = builder.cssMode;
        this.devToolsEnabled = builder.devToolsEnabled;
        this.lang = builder.lang;
        this.title = builder.title;
        this.headNodes = List.copyOf(builder.headNodes);
        this.runtimeActions = builder.runtimeActions;
        this.csrfProtectionDisabled = builder.csrfProtectionDisabled;
        this.internalEndpointRateLimitingEnabled = builder.internalEndpointRateLimitingEnabled;
        this.internalEndpointRateLimitCapacity = builder.internalEndpointRateLimitCapacity;
        this.internalEndpointRateLimitRefillTokens = builder.internalEndpointRateLimitRefillTokens;
        this.internalEndpointRateLimitRefillPeriod = builder.internalEndpointRateLimitRefillPeriod;
        this.trustedProxyAddresses = Set.copyOf(builder.trustedProxyAddresses);
    }

    public static LiveSessionConfig defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    Supplier<CssTheme> themeSupplier() {
        return themeSupplier;
    }

    CssMode cssMode() {
        return cssMode;
    }

    boolean devToolsEnabled() {
        return devToolsEnabled;
    }

    String lang() {
        return lang;
    }

    String title() {
        return title;
    }

    List<Node> headNodes() {
        return headNodes;
    }

    public RuntimeActionRegistry runtimeActions() {
        return runtimeActions;
    }

    public boolean isCsrfProtectionDisabled() {
        return csrfProtectionDisabled;
    }

    public boolean isInternalEndpointRateLimitingEnabled() {
        return internalEndpointRateLimitingEnabled;
    }

    public int internalEndpointRateLimitCapacity() {
        return internalEndpointRateLimitCapacity;
    }

    public int internalEndpointRateLimitRefillTokens() {
        return internalEndpointRateLimitRefillTokens;
    }

    public Duration internalEndpointRateLimitRefillPeriod() {
        return internalEndpointRateLimitRefillPeriod;
    }

    public Set<String> trustedProxyAddresses() {
        return trustedProxyAddresses;
    }

    public static final class Builder {
        private Supplier<CssTheme> themeSupplier = CssTheme::defaultTheme;
        private CssMode cssMode = CssMode.INTERNAL;
        private boolean devToolsEnabled;
        private String lang = "en";
        private String title = "UJFE";
        private final List<Node> headNodes = new ArrayList<>();
        private RuntimeActionRegistry runtimeActions = RuntimeActionRegistry.empty();
        private boolean csrfProtectionDisabled;
        private boolean internalEndpointRateLimitingEnabled = true;
        private int internalEndpointRateLimitCapacity = DEFAULT_INTERNAL_ENDPOINT_RATE_LIMIT_CAPACITY;
        private int internalEndpointRateLimitRefillTokens = DEFAULT_INTERNAL_ENDPOINT_RATE_LIMIT_REFILL_TOKENS;
        private Duration internalEndpointRateLimitRefillPeriod = DEFAULT_INTERNAL_ENDPOINT_RATE_LIMIT_REFILL_PERIOD;
        private final Set<String> trustedProxyAddresses = new LinkedHashSet<>();

        private Builder() {
        }

        public Builder themeSupplier(Supplier<CssTheme> themeSupplier) {
            this.themeSupplier = Objects.requireNonNull(themeSupplier, "themeSupplier");
            return this;
        }

        public Builder cssMode(CssMode cssMode) {
            this.cssMode = Objects.requireNonNull(cssMode, "cssMode");
            return this;
        }

        public Builder devToolsEnabled(boolean devToolsEnabled) {
            this.devToolsEnabled = devToolsEnabled;
            return this;
        }

        public Builder lang(String lang) {
            this.lang = requireText(lang, "lang");
            return this;
        }

        public Builder title(String title) {
            this.title = requireText(title, "title");
            return this;
        }

        public Builder head(Node... nodes) {
            Arrays.stream(nodes).forEach(node -> headNodes.add(Objects.requireNonNull(node, "node")));
            return this;
        }

        public Builder head(Collection<? extends Node> nodes) {
            Objects.requireNonNull(nodes, "nodes");
            nodes.forEach(node -> headNodes.add(Objects.requireNonNull(node, "node")));
            return this;
        }

        public Builder runtimeActions(RuntimeActionRegistry runtimeActions) {
            this.runtimeActions = Objects.requireNonNull(runtimeActions, "runtimeActions");
            return this;
        }

        public Builder disableCsrfProtectionForDevelopmentUnsafe() {
            this.csrfProtectionDisabled = true;
            return this;
        }

        public Builder internalEndpointRateLimitingEnabled(boolean enabled) {
            this.internalEndpointRateLimitingEnabled = enabled;
            return this;
        }

        public Builder internalEndpointRateLimit(int capacity, int refillTokens, Duration refillPeriod) {
            if (capacity < 1) {
                throw new IllegalArgumentException("capacity must be positive");
            }
            if (refillTokens < 1) {
                throw new IllegalArgumentException("refillTokens must be positive");
            }
            Objects.requireNonNull(refillPeriod, "refillPeriod");
            if (refillPeriod.isNegative() || refillPeriod.isZero()) {
                throw new IllegalArgumentException("refillPeriod must be positive");
            }
            this.internalEndpointRateLimitCapacity = capacity;
            this.internalEndpointRateLimitRefillTokens = refillTokens;
            this.internalEndpointRateLimitRefillPeriod = refillPeriod;
            return this;
        }

        public Builder trustedProxy(String address) {
            this.trustedProxyAddresses.add(requireText(address, "address"));
            return this;
        }

        public Builder trustedProxies(Collection<String> addresses) {
            Objects.requireNonNull(addresses, "addresses");
            addresses.forEach(this::trustedProxy);
            return this;
        }

        public Builder externalStylesheet(String href) {
            return head(link().attr("rel", "stylesheet").attr("href", href));
        }

        public LiveSessionConfig build() {
            return new LiveSessionConfig(this);
        }

        private static String requireText(String value, String name) {
            Objects.requireNonNull(value, name);
            if (value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return value;
        }
    }
}
