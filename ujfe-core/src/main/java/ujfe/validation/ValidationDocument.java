package ujfe.validation;

import java.util.*;
import java.util.stream.Collectors;

final class ValidationDocument {
    private final ValidationElement root;
    private final List<ValidationElement> elements;
    private final Map<String, List<ValidationElement>> ids;

    ValidationDocument(ValidationElement root, List<ValidationElement> elements) {
        this.root = Objects.requireNonNull(root, "root");
        this.elements = List.copyOf(Objects.requireNonNull(elements, "elements"));
        Map<String, List<ValidationElement>> indexedIds = new LinkedHashMap<>();
        for (ValidationElement element : elements) {
            String id = element.normalizedAttribute("id");
            if (!id.isBlank()) {
                indexedIds.computeIfAbsent(id, key -> new ArrayList<>())
                    .add(element);
            }
        }
        Map<String, List<ValidationElement>> immutableIds = new LinkedHashMap<>();
        indexedIds.forEach((id, matches) -> immutableIds.put(id, List.copyOf(matches)));
        this.ids = Map.copyOf(immutableIds);
    }

    ValidationElement root() {
        return root;
    }

    List<ValidationElement> elements() {
        return elements;
    }

    List<ValidationElement> elements(String tagName) {
        String normalized = tagName.toLowerCase(Locale.ROOT);
        return elements.stream()
            .filter(element -> normalized.equals(element.tagName()))
            .collect(Collectors.toUnmodifiableList());
    }

    Optional<ValidationElement> firstElement(String tagName) {
        String normalized = tagName.toLowerCase(Locale.ROOT);
        return elements.stream()
            .filter(element -> normalized.equals(element.tagName()))
            .findFirst();
    }

    boolean hasId(String id) {
        return ids.containsKey(ValidationSupport.normalizedText(id));
    }

    Optional<ValidationElement> firstById(String id) {
        List<ValidationElement> matches = ids.get(ValidationSupport.normalizedText(id));
        if (matches == null || matches.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(matches.get(0));
    }

    Map<String, List<ValidationElement>> ids() {
        return ids;
    }
}
