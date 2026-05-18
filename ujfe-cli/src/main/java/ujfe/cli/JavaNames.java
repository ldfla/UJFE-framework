package ujfe.cli;

import java.nio.file.Path;
import java.util.*;

final class JavaNames {
    private static final Set<String> RESERVED = Set.of(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
        "const", "continue", "default", "do", "double", "else", "enum", "exports", "extends",
        "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
        "int", "interface", "long", "module", "native", "new", "open", "opens", "package",
        "private", "protected", "provides", "public", "record", "requires", "return", "sealed",
        "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw",
        "throws", "to", "transient", "transitive", "try", "uses", "var", "void", "volatile",
        "while", "with", "yield", "true", "false", "null"
    );

    private JavaNames() {
    }

    static String packageNameFromOutput(Path outputPath) {
        String normalized = outputPath.normalize()
            .toString()
            .replace('\\', '/');
        int marker = normalized.indexOf("src/main/java/");
        if (marker < 0) {
            return "";
        }

        String afterJava = normalized.substring(marker + "src/main/java/".length());
        int lastSlash = afterJava.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "";
        }
        return afterJava.substring(0, lastSlash)
            .replace('/', '.');
    }

    static String classNameFromOutput(Path outputPath) {
        String fileName = outputPath.getFileName()
            .toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex < 0 ? fileName : fileName.substring(0, dotIndex);
    }

    static String requireClassName(String rawName, boolean safeFallback) {
        Objects.requireNonNull(rawName, "rawName");
        if (isValidClassName(rawName)) {
            return rawName;
        }
        if (safeFallback) {
            return safeClassName(rawName);
        }
        throw new HtmlConversionException("Invalid Java class name: " + rawName
            + ". Use --class-name with a valid Java class name or --safe-class-name.");
    }

    static String requirePackageName(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return "";
        }
        String[] parts = packageName.split("\\.");
        for (String part : parts) {
            if (!isValidIdentifier(part)) {
                throw new HtmlConversionException("Invalid Java package name: " + packageName);
            }
        }
        return packageName;
    }

    static String safeClassName(String rawName) {
        StringBuilder name = new StringBuilder();
        boolean capitalizeNext = true;
        for (int index = 0; index < rawName.length(); index++) {
            char current = rawName.charAt(index);
            if (Character.isLetterOrDigit(current) || current == '_' || current == '$') {
                if (name.length() == 0 && !Character.isJavaIdentifierStart(current)) {
                    continue;
                }
                name.append(capitalizeNext ? Character.toUpperCase(current) : current);
                capitalizeNext = false;
            } else {
                capitalizeNext = true;
            }
        }
        if (name.length() == 0 || RESERVED.contains(name.toString())) {
            return "GeneratedPage";
        }
        String candidate = name.toString();
        if (!candidate.endsWith("Page")) {
            candidate = candidate + "Page";
        }
        return isValidClassName(candidate) ? candidate : "GeneratedPage";
    }

    static String safeMethodName(String rawName, Set<String> usedNames) {
        String base = safeIdentifier(rawName);
        if (base.isBlank() || RESERVED.contains(base)) {
            base = "renderSection";
        }
        String candidate = base;
        int suffix = 2;
        while (!usedNames.add(candidate)) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }

    private static String safeIdentifier(String rawName) {
        StringBuilder name = new StringBuilder();
        boolean capitalizeNext = false;
        for (int index = 0; index < rawName.length(); index++) {
            char current = rawName.charAt(index);
            if (Character.isLetterOrDigit(current) || current == '_' || current == '$') {
                if (name.length() == 0 && !Character.isJavaIdentifierStart(current)) {
                    continue;
                }
                name.append(capitalizeNext ? Character.toUpperCase(current) : current);
                capitalizeNext = false;
            } else {
                capitalizeNext = true;
            }
        }
        return isValidIdentifier(name.toString()) ? name.toString() : "";
    }

    private static boolean isValidClassName(String name) {
        return isValidIdentifier(name) && Character.isUpperCase(name.charAt(0));
    }

    private static boolean isValidIdentifier(String name) {
        if (name == null || name.isBlank() || RESERVED.contains(name)) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        for (int index = 1; index < name.length(); index++) {
            if (!Character.isJavaIdentifierPart(name.charAt(index))) {
                return false;
            }
        }
        return true;
    }
}
