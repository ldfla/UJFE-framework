package ujfe.core;

import java.util.Objects;

final class HtmlNames {
    private HtmlNames() {
    }

    static String validateElementName(String tagName) {
        Objects.requireNonNull(tagName, "tagName");
        if (tagName.isBlank()) {
            throw new IllegalArgumentException("HTML tag name must not be blank");
        }
        if (!isAsciiLetter(tagName.charAt(0))) {
            throw new IllegalArgumentException("HTML tag name must start with an ASCII letter: " + tagName);
        }
        for (int index = 1; index < tagName.length(); index++) {
            char character = tagName.charAt(index);
            if (!isElementNameCharacter(character)) {
                throw new IllegalArgumentException("Invalid HTML tag name: " + tagName);
            }
        }
        return tagName;
    }

    static String validateCustomElementName(String tagName) {
        String validTagName = validateElementName(tagName);
        if (!validTagName.contains("-")) {
            throw new IllegalArgumentException("Custom element names must contain a hyphen: " + tagName);
        }
        if (!validTagName.equals(validTagName.toLowerCase(java.util.Locale.ROOT))) {
            throw new IllegalArgumentException("Custom element names must be lowercase: " + tagName);
        }
        return validTagName;
    }

    static String validateAttributeName(String name) {
        Objects.requireNonNull(name, "name");
        if (!name.matches("[A-Za-z][A-Za-z0-9:_-]*")) {
            throw new IllegalArgumentException("Invalid HTML attribute name: " + name);
        }
        if (isInlineEventHandler(name)) {
            throw new IllegalArgumentException(
                "Inline event handler attributes are blocked by default: " + name
                    + ". Use Element.on(event, handler) for server-side live events.");
        }
        return name;
    }

    private static boolean isInlineEventHandler(String name) {
        if (name.length() < 3) {
            return false;
        }
        char first = name.charAt(0);
        char second = name.charAt(1);
        if ((first == 'o' || first == 'O') && (second == 'n' || second == 'N')) {
            char third = name.charAt(2);
            return isAsciiLetter(third);
        }
        return false;
    }

    static String validateAttributeSuffix(String name) {
        Objects.requireNonNull(name, "name");
        if (!name.matches("[A-Za-z0-9][A-Za-z0-9_.:-]*")) {
            throw new IllegalArgumentException("Invalid attribute suffix: " + name);
        }
        return name;
    }

    static String validateEventName(String eventName) {
        Objects.requireNonNull(eventName, "eventName");
        if (!eventName.matches("[a-z][a-z0-9-]*")) {
            throw new IllegalArgumentException("Invalid event name: " + eventName);
        }
        return eventName;
    }

    private static boolean isElementNameCharacter(char character) {
        return isAsciiLetter(character)
            || character >= '0' && character <= '9'
            || character == '-'
            || character == '_';
    }

    private static boolean isAsciiLetter(char character) {
        return character >= 'A' && character <= 'Z'
            || character >= 'a' && character <= 'z';
    }
}
