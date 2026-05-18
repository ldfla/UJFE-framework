package ujfe.validation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class DocumentValidatorTest {
    @Test
    void offModeDoesNotRunValidation() {
        ValidationResult result = DocumentValidator.off()
            .validate("<img src=\"/missing-alt.png\"><button></button>");

        assertTrue(result.isValid());
    }

    @Test
    void warnModeCollectsFindingsWithoutThrowing() {
        DocumentValidator validator = DocumentValidator.of(ValidationOptions.builder()
            .mode(ValidationMode.WARN)
            .accessibilityValidationEnabled(true)
            .build());

        ValidationResult result = validator.validate("<main><h1>Title</h1><img src=\"/missing-alt.png\"></main>");

        assertFalse(result.isValid());
        assertEquals(ValidationSeverity.WARN, result.findings()
            .get(0)
            .severity());
        assertDoesNotThrow(() -> validator.validateOrThrow("<main><h1>Title</h1><img src=\"/missing-alt.png\"></main>"));
    }

    @Test
    void strictModeThrowsActionableException() {
        DocumentValidator validator = DocumentValidator.of(ValidationOptions.builder()
            .mode(ValidationMode.STRICT)
            .accessibilityValidationEnabled(true)
            .build());

        ValidationException exception = assertThrows(ValidationException.class,
            () -> validator.validateOrThrow("<main><h1>Title</h1><img src=\"/missing-alt.png\"></main>"));

        assertFalse(exception.result()
            .findingsForRule(AccessibilityValidator.IMG_ALT)
            .isEmpty());
        assertTrue(exception.getMessage()
            .contains("suggestion="));
        assertEquals(ValidationSeverity.ERROR, exception.result()
            .findings()
            .get(0)
            .severity());
    }

    @Test
    void selectedWarnRulesCanBeStrictFailures() {
        DocumentValidator validator = DocumentValidator.of(ValidationOptions.builder()
            .mode(ValidationMode.WARN)
            .accessibilityValidationEnabled(true)
            .strictRule(AccessibilityValidator.IMG_ALT)
            .build());

        assertThrows(ValidationException.class,
            () -> validator.validateOrThrow("<main><h1>Title</h1><img src=\"/missing-alt.png\"></main>"));
    }

    @Test
    void configurationCanEnableAccessibilityOnlySeoOnlyOrBoth() {
        String html = "<main><h1>Title</h1><img src=\"/missing-alt.png\"></main>";

        ValidationResult accessibilityOnly = DocumentValidator.of(ValidationOptions.builder()
                .accessibilityValidationEnabled(true)
                .build())
            .validate(html);
        ValidationResult seoOnly = DocumentValidator.of(ValidationOptions.builder()
                .seoValidationEnabled(true)
                .build())
            .validate(html);
        ValidationResult both = DocumentValidator.of(ValidationOptions.builder()
                .accessibilityValidationEnabled(true)
                .seoValidationEnabled(true)
                .build())
            .validate(html);

        assertFalse(accessibilityOnly.findingsForRule(AccessibilityValidator.IMG_ALT)
            .isEmpty());
        assertTrue(accessibilityOnly.findingsForRule(SeoValidator.DOCUMENT_TITLE)
            .isEmpty());
        assertFalse(seoOnly.findingsForRule(SeoValidator.DOCUMENT_TITLE)
            .isEmpty());
        assertTrue(seoOnly.findingsForRule(AccessibilityValidator.IMG_ALT)
            .isEmpty());
        assertFalse(both.findingsForRule(AccessibilityValidator.IMG_ALT)
            .isEmpty());
        assertFalse(both.findingsForRule(SeoValidator.DOCUMENT_TITLE)
            .isEmpty());
    }

    @Test
    void individualRulesCanBeDisabled() {
        ValidationResult result = DocumentValidator.of(ValidationOptions.builder()
                .accessibilityValidationEnabled(true)
                .disableRule(AccessibilityValidator.IMG_ALT)
                .build())
            .validate("<main><h1>Title</h1><img src=\"/missing-alt.png\"></main>");

        assertTrue(result.findingsForRule(AccessibilityValidator.IMG_ALT)
            .isEmpty());
    }

    @Test
    void validationResultDeduplicatesRepeatedFindings() {
        ValidationFinding finding = ValidationFinding.builder()
            .ruleId(AccessibilityValidator.IMG_ALT)
            .severity(ValidationSeverity.WARN)
            .category(ValidationCategory.ACCESSIBILITY)
            .message("Image is missing an alt attribute.")
            .suggestion("Add alt text.")
            .element("img")
            .attribute("alt")
            .location("main:nth-of-type(1) > img:nth-of-type(1)")
            .build();

        ValidationResult result = ValidationResult.of(List.of(finding, finding));

        assertEquals(1, result.findings()
            .size());
    }
}
