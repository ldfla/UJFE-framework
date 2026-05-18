package ujfe.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AccessibilityValidator {
    public static final String SINGLE_MAIN = "accessibility.document.single-main";
    public static final String SINGLE_H1 = "accessibility.heading.single-h1";
    public static final String IMG_ALT = "accessibility.img.alt";
    public static final String BUTTON_ACCESSIBLE_NAME = "accessibility.button.accessible-name";
    public static final String INPUT_LABEL = "accessibility.input.label";
    public static final String DUPLICATE_ID = "accessibility.id.duplicate";
    public static final String ARIA_LABELLEDBY_TARGET = "accessibility.aria-labelledby.target";
    public static final String ARIA_DESCRIBEDBY_TARGET = "accessibility.aria-describedby.target";
    public static final String INTERACTIVE_ACCESSIBLE_NAME = "accessibility.interactive.accessible-name";
    public static final String INTERACTIVE_NESTED = "accessibility.interactive.nested";

    private final ValidationOptions options;

    public AccessibilityValidator() {
        this(ValidationOptions.accessibility());
    }

    public AccessibilityValidator(ValidationOptions options) {
        this.options = Objects.requireNonNull(options, "options");
    }

    public ValidationResult validate(String html) {
        Objects.requireNonNull(html, "html");
        if (options.mode() == ValidationMode.OFF) {
            return ValidationResult.empty();
        }
        List<ValidationFinding> findings = new ArrayList<>();
        validate(HtmlValidationParser.parse(html), findings);
        return ValidationResult.of(findings);
    }

    void validate(ValidationDocument document, List<ValidationFinding> findings) {
        validateSingleMain(document, findings);
        validateSingleH1(document, findings);
        validateImages(document, findings);
        validateButtons(document, findings);
        validateInputs(document, findings);
        validateDuplicateIds(document, findings);
        validateAriaReferences(document, findings);
        validateInteractiveElements(document, findings);
    }

    private void validateSingleMain(ValidationDocument document, List<ValidationFinding> findings) {
        List<ValidationElement> mainElements = document.elements("main");
        if (mainElements.size() == 1) {
            return;
        }
        add(findings, SINGLE_MAIN,
            mainElements.isEmpty()
                ? "Document is missing a main landmark."
                : "Document has more than one main landmark.",
            "Render exactly one <main> element for the page's primary content.",
            mainElements.isEmpty() ? document.root() : mainElements.get(1),
            "main",
            null);
    }

    private void validateSingleH1(ValidationDocument document, List<ValidationFinding> findings) {
        List<ValidationElement> headings = document.elements("h1");
        if (headings.size() == 1) {
            return;
        }
        add(findings, SINGLE_H1,
            headings.isEmpty()
                ? "Document is missing an h1 heading."
                : "Document has more than one h1 heading.",
            "Render one primary <h1> that describes the page.",
            headings.isEmpty() ? document.root() : headings.get(1),
            "h1",
            null);
    }

    private void validateImages(ValidationDocument document, List<ValidationFinding> findings) {
        for (ValidationElement image : document.elements("img")) {
            if (!ValidationSupport.imageHasAltTextOrDecorativeIntent(image)) {
                add(findings, IMG_ALT,
                    "Image is missing meaningful alt text or explicit decorative intent.",
                    "Add alt text that describes the image, or use alt=\"\" with role=\"presentation\", role=\"none\", aria-hidden=\"true\", or data-ujfe-decorative=\"true\" for decorative images.",
                    image,
                    "img",
                    "alt");
            }
        }
    }

    private void validateButtons(ValidationDocument document, List<ValidationFinding> findings) {
        for (ValidationElement button : document.elements("button")) {
            if (!ValidationSupport.hasAccessibleName(button, document)) {
                add(findings, BUTTON_ACCESSIBLE_NAME,
                    "Button has no visible text or accessible name.",
                    "Add button text, aria-label, or aria-labelledby that points to an element with text.",
                    button,
                    "button",
                    null);
            }
        }
    }

    private void validateInputs(ValidationDocument document, List<ValidationFinding> findings) {
        for (ValidationElement input : document.elements("input")) {
            if (!ValidationSupport.inputHasLabelOrName(input, document)) {
                add(findings, INPUT_LABEL,
                    "Form input has no associated label or accessible name.",
                    "Associate the input with a <label for=\"...\">, wrap it in a <label>, or add aria-label or aria-labelledby.",
                    input,
                    "input",
                    null);
            }
        }
    }

    private void validateDuplicateIds(ValidationDocument document, List<ValidationFinding> findings) {
        for (List<ValidationElement> matches : document.ids()
            .values()) {
            if (matches.size() <= 1) {
                continue;
            }
            for (int index = 1; index < matches.size(); index++) {
                add(findings, DUPLICATE_ID,
                    "Duplicate id attribute found.",
                    "Use unique id values so labels, anchors, and ARIA references resolve deterministically.",
                    matches.get(index),
                    matches.get(index)
                        .tagName(),
                    "id");
            }
        }
    }

    private void validateAriaReferences(ValidationDocument document, List<ValidationFinding> findings) {
        for (ValidationElement element : document.elements()) {
            if (ValidationSupport.hasMissingReferencedId(element, "aria-labelledby", document)) {
                add(findings, ARIA_LABELLEDBY_TARGET,
                    "aria-labelledby references an element id that does not exist.",
                    "Ensure every id in aria-labelledby points to an existing element.",
                    element,
                    element.tagName(),
                    "aria-labelledby");
            }
            if (ValidationSupport.hasMissingReferencedId(element, "aria-describedby", document)) {
                add(findings, ARIA_DESCRIBEDBY_TARGET,
                    "aria-describedby references an element id that does not exist.",
                    "Ensure every id in aria-describedby points to an existing element.",
                    element,
                    element.tagName(),
                    "aria-describedby");
            }
        }
    }

    private void validateInteractiveElements(ValidationDocument document, List<ValidationFinding> findings) {
        for (ValidationElement element : document.elements()) {
            if (!ValidationSupport.isInteractiveElement(element)) {
                continue;
            }
            validateInteractiveName(document, findings, element);
            validateNestedInteractive(document, findings, element);
        }
    }

    private void validateInteractiveName(
        ValidationDocument document,
        List<ValidationFinding> findings,
        ValidationElement element
    ) {
        if ("button".equals(element.tagName()) || "input".equals(element.tagName())) {
            return;
        }
        if (!ValidationSupport.hasAccessibleName(element, document)) {
            add(findings, INTERACTIVE_ACCESSIBLE_NAME,
                "Interactive element has no accessible name.",
                "Add visible text, aria-label, aria-labelledby, title, or another supported accessible-name source.",
                element,
                element.tagName(),
                null);
        }
    }

    private void validateNestedInteractive(
        ValidationDocument document,
        List<ValidationFinding> findings,
        ValidationElement element
    ) {
        for (ValidationElement candidate : document.elements()) {
            if (candidate != element
                && ValidationSupport.isInteractiveElement(candidate)
                && element.containsDescendant(candidate)) {
                add(findings, INTERACTIVE_NESTED,
                    "Interactive element is nested inside another interactive element.",
                    "Avoid placing buttons, links, form controls, or interactive ARIA roles inside another interactive control.",
                    candidate,
                    candidate.tagName(),
                    null);
                return;
            }
        }
    }

    private void add(
        List<ValidationFinding> findings,
        String ruleId,
        String message,
        String suggestion,
        ValidationElement element,
        String tagName,
        String attribute
    ) {
        if (!options.isRuleEnabled(ruleId)) {
            return;
        }
        findings.add(ValidationFinding.builder()
            .ruleId(ruleId)
            .severity(options.severityFor(ruleId))
            .category(ValidationCategory.ACCESSIBILITY)
            .message(message)
            .suggestion(suggestion)
            .element(tagName)
            .attribute(attribute)
            .location(element.location())
            .build());
    }
}
