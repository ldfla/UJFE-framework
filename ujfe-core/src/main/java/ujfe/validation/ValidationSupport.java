package ujfe.validation;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

final class ValidationSupport {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Set<String> INTERACTIVE_ROLES = Set.of(
        "button", "checkbox", "link", "menuitem", "menuitemcheckbox", "menuitemradio",
        "option", "radio", "searchbox", "slider", "spinbutton", "switch", "tab", "textbox"
    );

    private ValidationSupport() {
    }

    static String normalizedText(String value) {
        if (value == null) {
            return "";
        }
        return WHITESPACE.matcher(decodeEntities(value).replace('\u00a0', ' '))
            .replaceAll(" ")
            .trim();
    }

    static boolean hasMeaningfulText(String value) {
        return !normalizedText(value).isBlank();
    }

    static boolean hasAccessibleName(ValidationElement element, ValidationDocument document) {
        if (element.hasNonBlankAttribute("aria-label")) {
            return true;
        }
        String labelledBy = element.normalizedAttribute("aria-labelledby");
        if (!labelledBy.isBlank()) {
            for (String id : splitIds(labelledBy)) {
                Optional<ValidationElement> target = document.firstById(id);
                if (target.isPresent() && hasMeaningfulText(accessibleNameText(target.get(), document))) {
                    return true;
                }
            }
        }
        if (hasMeaningfulText(element.visibleTextContent())) {
            return true;
        }
        if (hasDescendantImageAltText(element)) {
            return true;
        }
        return element.hasNonBlankAttribute("title");
    }

    static String accessibleNameText(ValidationElement element, ValidationDocument document) {
        if (element.hasNonBlankAttribute("aria-label")) {
            return element.normalizedAttribute("aria-label");
        }
        String labelledBy = element.normalizedAttribute("aria-labelledby");
        if (!labelledBy.isBlank()) {
            StringBuilder text = new StringBuilder();
            for (String id : splitIds(labelledBy)) {
                document.firstById(id)
                    .ifPresent(target -> text.append(target.textContent())
                        .append(' '));
            }
            return normalizedText(text.toString());
        }
        if (element.hasNonBlankAttribute("value")) {
            return element.normalizedAttribute("value");
        }
        if (element.hasNonBlankAttribute("alt")) {
            return element.normalizedAttribute("alt");
        }
        return element.textContent();
    }

    static boolean isDecorativeImage(ValidationElement element) {
        String alt = element.attribute("alt");
        if (alt == null || !normalizedText(alt).isBlank()) {
            return false;
        }
        return element.attributeEquals("role", "presentation")
            || element.attributeEquals("role", "none")
            || element.attributeEquals("aria-hidden", "true")
            || element.attributeEquals("data-ujfe-decorative", "true");
    }

    static boolean imageHasAltTextOrDecorativeIntent(ValidationElement element) {
        return element.hasNonBlankAttribute("alt") || isDecorativeImage(element);
    }

    static boolean isHiddenInput(ValidationElement element) {
        return "input".equals(element.tagName()) && element.attributeEquals("type", "hidden");
    }

    static boolean inputHasLabelOrName(ValidationElement element, ValidationDocument document) {
        if (isHiddenInput(element)) {
            return true;
        }
        if (element.hasNonBlankAttribute("aria-label")) {
            return true;
        }
        if (hasValidLabelledBy(element, document)) {
            return true;
        }
        if (element.hasAncestor("label")) {
            return true;
        }
        String id = element.normalizedAttribute("id");
        if (!id.isBlank()) {
            for (ValidationElement label : document.elements("label")) {
                if (id.equals(label.normalizedAttribute("for"))) {
                    return true;
                }
            }
        }
        String type = element.normalizedAttribute("type");
        if (Set.of("button", "submit", "reset")
            .contains(type) && element.hasNonBlankAttribute("value")) {
            return true;
        }
        return "image".equals(type) && element.hasNonBlankAttribute("alt");
    }

    static boolean hasValidLabelledBy(ValidationElement element, ValidationDocument document) {
        String labelledBy = element.normalizedAttribute("aria-labelledby");
        if (labelledBy.isBlank()) {
            return false;
        }
        for (String id : splitIds(labelledBy)) {
            Optional<ValidationElement> target = document.firstById(id);
            if (target.isPresent() && hasMeaningfulText(accessibleNameText(target.get(), document))) {
                return true;
            }
        }
        return false;
    }

    static boolean hasMissingReferencedId(ValidationElement element, String attribute, ValidationDocument document) {
        String value = element.normalizedAttribute(attribute);
        if (value.isBlank()) {
            return false;
        }
        for (String id : splitIds(value)) {
            if (!document.hasId(id)) {
                return true;
            }
        }
        return false;
    }

    static List<String> splitIds(String value) {
        String normalized = normalizedText(value);
        if (normalized.isBlank()) {
            return List.of();
        }
        return List.of(normalized.split("\\s+"));
    }

    static boolean isInteractiveElement(ValidationElement element) {
        String tag = element.tagName();
        if ("button".equals(tag) || "select".equals(tag) || "textarea".equals(tag) || "summary".equals(tag)) {
            return true;
        }
        if ("input".equals(tag)) {
            return !isHiddenInput(element);
        }
        if ("a".equals(tag) || "area".equals(tag)) {
            return element.hasAttribute("href");
        }
        String role = element.normalizedAttribute("role")
            .toLowerCase(Locale.ROOT);
        return INTERACTIVE_ROLES.contains(role);
    }

    static boolean isHeading(ValidationElement element) {
        return headingLevel(element) > 0;
    }

    private static boolean hasDescendantImageAltText(ValidationElement element) {
        for (ValidationElement child : element.children()) {
            if ("img".equals(child.tagName()) && child.hasNonBlankAttribute("alt")) {
                return true;
            }
            if (hasDescendantImageAltText(child)) {
                return true;
            }
        }
        return false;
    }

    static int headingLevel(ValidationElement element) {
        String tag = element.tagName();
        if (tag.length() == 2 && tag.charAt(0) == 'h') {
            char level = tag.charAt(1);
            if (level >= '1' && level <= '6') {
                return level - '0';
            }
        }
        return 0;
    }

    static boolean relContains(ValidationElement element, String token) {
        String rel = element.normalizedAttribute("rel")
            .toLowerCase(Locale.ROOT);
        if (rel.isBlank()) {
            return false;
        }
        for (String relToken : rel.split("\\s+")) {
            if (token.equals(relToken)) {
                return true;
            }
        }
        return false;
    }

    static String decodeEntities(String value) {
        String decoded = value
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'");
        StringBuilder builder = new StringBuilder(decoded.length());
        for (int index = 0; index < decoded.length(); index++) {
            char current = decoded.charAt(index);
            if (current == '&' && index + 3 < decoded.length() && decoded.charAt(index + 1) == '#') {
                int semicolon = decoded.indexOf(';', index + 2);
                if (semicolon > 0) {
                    String entity = decoded.substring(index + 2, semicolon);
                    try {
                        int codePoint = parseEntityCodePoint(entity);
                        builder.appendCodePoint(codePoint);
                        index = semicolon;
                        continue;
                    } catch (IllegalArgumentException ignored) {
                        // Preserve unknown entities; validation only needs whitespace and non-empty checks.
                    }
                }
            }
            builder.append(current);
        }
        return builder.toString();
    }

    private static int parseEntityCodePoint(String entity) {
        if (entity.startsWith("x") || entity.startsWith("X")) {
            return Integer.parseInt(entity.substring(1), 16);
        }
        return Integer.parseInt(entity);
    }
}
