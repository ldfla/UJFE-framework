package ujfe.live;

import ujfe.core.Node;
import ujfe.html.CssTheme;
import ujfe.runtime.action.RuntimeActionRegistry;

import java.util.*;
import java.util.function.Supplier;

import static ujfe.html.UI.link;

public final class LiveSessionConfig {
    private final Supplier<CssTheme> themeSupplier;
    private final CssMode cssMode;
    private final boolean devToolsEnabled;
    private final String lang;
    private final String title;
    private final List<Node> headNodes;
    private final RuntimeActionRegistry runtimeActions;
    private final boolean csrfProtectionDisabled;

    private LiveSessionConfig(Builder builder) {
        this.themeSupplier = builder.themeSupplier;
        this.cssMode = builder.cssMode;
        this.devToolsEnabled = builder.devToolsEnabled;
        this.lang = builder.lang;
        this.title = builder.title;
        this.headNodes = List.copyOf(builder.headNodes);
        this.runtimeActions = builder.runtimeActions;
        this.csrfProtectionDisabled = builder.csrfProtectionDisabled;
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

    public static final class Builder {
        private Supplier<CssTheme> themeSupplier = CssTheme::defaultTheme;
        private CssMode cssMode = CssMode.INTERNAL;
        private boolean devToolsEnabled;
        private String lang = "en";
        private String title = "UJFE";
        private final List<Node> headNodes = new ArrayList<>();
        private RuntimeActionRegistry runtimeActions = RuntimeActionRegistry.empty();
        private boolean csrfProtectionDisabled;

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
