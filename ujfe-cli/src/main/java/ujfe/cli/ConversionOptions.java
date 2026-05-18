package ujfe.cli;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

final class ConversionOptions {
    private final Path input;
    private final Path output;
    private final String type;
    private final String packageName;
    private final String className;
    private final Charset encoding;
    private final CommentPolicy commentPolicy;
    private final boolean unsafeFallbackEnabled;
    private final CssMigrationMode cssMigrationMode;
    private final String cssClassName;
    private final boolean componentize;
    private final boolean safeClassName;
    private final boolean debug;

    private ConversionOptions(Builder builder) {
        this.input = Objects.requireNonNull(builder.input, "input");
        this.output = Objects.requireNonNull(builder.output, "output");
        this.type = Objects.requireNonNull(builder.type, "type");
        this.packageName = Objects.requireNonNull(builder.packageName, "packageName");
        this.className = Objects.requireNonNull(builder.className, "className");
        this.encoding = Objects.requireNonNull(builder.encoding, "encoding");
        this.commentPolicy = Objects.requireNonNull(builder.commentPolicy, "commentPolicy");
        this.unsafeFallbackEnabled = builder.unsafeFallbackEnabled;
        this.cssMigrationMode = Objects.requireNonNull(builder.cssMigrationMode, "cssMigrationMode");
        this.cssClassName = Objects.requireNonNull(builder.cssClassName, "cssClassName");
        this.componentize = builder.componentize;
        this.safeClassName = builder.safeClassName;
        this.debug = builder.debug;
    }

    static Builder builder(Path input, Path output) {
        return new Builder(input, output);
    }

    Path input() {
        return input;
    }

    Path output() {
        return output;
    }

    String type() {
        return type;
    }

    String packageName() {
        return packageName;
    }

    String className() {
        return className;
    }

    Charset encoding() {
        return encoding;
    }

    CommentPolicy commentPolicy() {
        return commentPolicy;
    }

    boolean unsafeFallbackEnabled() {
        return unsafeFallbackEnabled;
    }

    CssMigrationMode cssMigrationMode() {
        return cssMigrationMode;
    }

    String cssClassName() {
        return cssClassName;
    }

    boolean componentize() {
        return componentize;
    }

    boolean safeClassName() {
        return safeClassName;
    }

    boolean debug() {
        return debug;
    }

    static final class Builder {
        private final Path input;
        private final Path output;
        private String type = "html";
        private String packageName;
        private String className;
        private Charset encoding = StandardCharsets.UTF_8;
        private CommentPolicy commentPolicy = CommentPolicy.DROP;
        private boolean unsafeFallbackEnabled;
        private CssMigrationMode cssMigrationMode = CssMigrationMode.NONE;
        private String cssClassName = "GeneratedStyles";
        private boolean componentize;
        private boolean safeClassName;
        private boolean debug;

        private Builder(Path input, Path output) {
            this.input = Objects.requireNonNull(input, "input");
            this.output = Objects.requireNonNull(output, "output");
        }

        Builder type(String type) {
            this.type = Objects.requireNonNull(type, "type");
            return this;
        }

        Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        Builder className(String className) {
            this.className = className;
            return this;
        }

        Builder encoding(Charset encoding) {
            this.encoding = Objects.requireNonNull(encoding, "encoding");
            return this;
        }

        Builder commentPolicy(CommentPolicy commentPolicy) {
            this.commentPolicy = Objects.requireNonNull(commentPolicy, "commentPolicy");
            return this;
        }

        Builder unsafeFallbackEnabled(boolean unsafeFallbackEnabled) {
            this.unsafeFallbackEnabled = unsafeFallbackEnabled;
            return this;
        }

        Builder cssMigrationMode(CssMigrationMode cssMigrationMode) {
            this.cssMigrationMode = Objects.requireNonNull(cssMigrationMode, "cssMigrationMode");
            return this;
        }

        Builder cssClassName(String cssClassName) {
            this.cssClassName = Objects.requireNonNull(cssClassName, "cssClassName");
            return this;
        }

        Builder componentize(boolean componentize) {
            this.componentize = componentize;
            return this;
        }

        Builder safeClassName(boolean safeClassName) {
            this.safeClassName = safeClassName;
            return this;
        }

        Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        ConversionOptions build() {
            if (java.nio.file.Files.isDirectory(output)) {
                throw new HtmlConversionException("Output path is a directory, expected a Java file: " + output);
            }
            boolean useSafeClassName = safeClassName;
            String resolvedPackage = packageName == null
                ? JavaNames.packageNameFromOutput(output)
                : JavaNames.requirePackageName(packageName);
            String rawClassName = className == null ? JavaNames.classNameFromOutput(output) : className;
            String resolvedClassName = JavaNames.requireClassName(rawClassName, useSafeClassName);
            String resolvedCssClassName = JavaNames.requireClassName(cssClassName, useSafeClassName);
            this.packageName = resolvedPackage;
            this.className = resolvedClassName;
            this.cssClassName = resolvedCssClassName;
            return new ConversionOptions(this);
        }
    }
}
