package ujfe.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AccessibilityValidatorTest {
    private final AccessibilityValidator validator = new AccessibilityValidator();

    @Test
    void validDocumentPassesAccessibilityValidation() {
        ValidationResult result = validator.validate("<main>"
            + "<h1>Dashboard</h1>"
            + "<img src=\"/logo.png\" alt=\"UJFE logo\">"
            + "<img src=\"/divider.png\" alt=\"\" role=\"presentation\">"
            + "<button>Save</button>"
            + "<button aria-label=\"Close\"></button>"
            + "<span id=\"details-label\">Open details</span>"
            + "<button aria-labelledby=\"details-label\"></button>"
            + "<form>"
            + "<label for=\"email\">Email</label>"
            + "<input id=\"email\" type=\"email\">"
            + "<label><input type=\"checkbox\"> Accept terms</label>"
            + "<input type=\"text\" aria-label=\"Search\">"
            + "<span id=\"name-label\">Name</span>"
            + "<input type=\"text\" aria-labelledby=\"name-label\">"
            + "<input type=\"hidden\" name=\"csrf\" value=\"token\">"
            + "</form>"
            + "<a href=\"/docs\">Docs</a>"
            + "</main>");

        assertTrue(result.isValid());
    }

    @Test
    void missingAndMultipleH1ProduceFindings() {
        assertHasRule("<main><p>No title</p></main>", AccessibilityValidator.SINGLE_H1);
        assertHasRule("<main><h1>One</h1><h1>Two</h1></main>", AccessibilityValidator.SINGLE_H1);
    }

    @Test
    void missingAndMultipleMainProduceFindings() {
        assertHasRule("<h1>Title</h1>", AccessibilityValidator.SINGLE_MAIN);
        assertHasRule("<main><h1>One</h1></main><main><p>Two</p></main>", AccessibilityValidator.SINGLE_MAIN);
    }

    @Test
    void imageAltRulesRespectDecorativeIntent() {
        assertHasRule("<main><h1>Title</h1><img src=\"/chart.png\"></main>", AccessibilityValidator.IMG_ALT);
        assertHasRule("<main><h1>Title</h1><img src=\"/chart.png\" alt=\"\"></main>", AccessibilityValidator.IMG_ALT);
        assertNoRule("<main><h1>Title</h1><img src=\"/chart.png\" alt=\"Sales chart\"></main>", AccessibilityValidator.IMG_ALT);
        assertNoRule("<main><h1>Title</h1><img src=\"/line.png\" alt=\"\" aria-hidden=\"true\"></main>", AccessibilityValidator.IMG_ALT);
        assertNoRule("<main><h1>Title</h1><img src=\"/line.png\" alt=\"\" data-ujfe-decorative=\"true\"></main>", AccessibilityValidator.IMG_ALT);
    }

    @Test
    void buttonAccessibleNameRulesSupportTextAndAria() {
        assertNoRule("<main><h1>Title</h1><button>Save</button></main>", AccessibilityValidator.BUTTON_ACCESSIBLE_NAME);
        assertNoRule("<main><h1>Title</h1><button aria-label=\"Save\"></button></main>", AccessibilityValidator.BUTTON_ACCESSIBLE_NAME);
        assertNoRule("<main><h1>Title</h1><span id=\"save-label\">Save</span><button aria-labelledby=\"save-label\"></button></main>",
            AccessibilityValidator.BUTTON_ACCESSIBLE_NAME);
        assertNoRule("<main><h1>Title</h1><button><img src=\"/save.png\" alt=\"Save\"></button></main>",
            AccessibilityValidator.BUTTON_ACCESSIBLE_NAME);
        assertHasRule("<main><h1>Title</h1><button><span aria-hidden=\"true\"></span></button></main>",
            AccessibilityValidator.BUTTON_ACCESSIBLE_NAME);
    }

    @Test
    void inputLabelRulesSupportLabelsAndAria() {
        assertNoRule("<main><h1>Title</h1><label for=\"email\">Email</label><input id=\"email\"></main>",
            AccessibilityValidator.INPUT_LABEL);
        assertNoRule("<main><h1>Title</h1><label>Email <input></label></main>",
            AccessibilityValidator.INPUT_LABEL);
        assertNoRule("<main><h1>Title</h1><input aria-label=\"Search\"></main>",
            AccessibilityValidator.INPUT_LABEL);
        assertNoRule("<main><h1>Title</h1><span id=\"query-label\">Query</span><input aria-labelledby=\"query-label\"></main>",
            AccessibilityValidator.INPUT_LABEL);
        assertNoRule("<main><h1>Title</h1><input type=\"hidden\" name=\"csrf\"></main>",
            AccessibilityValidator.INPUT_LABEL);
        assertHasRule("<main><h1>Title</h1><input></main>", AccessibilityValidator.INPUT_LABEL);
    }

    @Test
    void duplicateIdsAndMissingAriaReferencesAreDetected() {
        assertHasRule("<main><h1>Title</h1><p id=\"copy\">A</p><p id=\"copy\">B</p></main>",
            AccessibilityValidator.DUPLICATE_ID);
        assertHasRule("<main><h1>Title</h1><button aria-labelledby=\"missing\"></button></main>",
            AccessibilityValidator.ARIA_LABELLEDBY_TARGET);
        assertHasRule("<main><h1>Title</h1><button aria-describedby=\"missing\">Save</button></main>",
            AccessibilityValidator.ARIA_DESCRIBEDBY_TARGET);
    }

    @Test
    void interactiveElementsNeedNamesAndCannotBeNested() {
        assertHasRule("<main><h1>Title</h1><a href=\"/empty\"></a></main>",
            AccessibilityValidator.INTERACTIVE_ACCESSIBLE_NAME);
        assertHasRule("<main><h1>Title</h1><button>Outer <a href=\"/inner\">Inner</a></button></main>",
            AccessibilityValidator.INTERACTIVE_NESTED);
    }

    private void assertHasRule(String html, String ruleId) {
        assertFalse(validator.validate(html)
            .findingsForRule(ruleId)
            .isEmpty(), "Expected rule " + ruleId);
    }

    private void assertNoRule(String html, String ruleId) {
        assertTrue(validator.validate(html)
            .findingsForRule(ruleId)
            .isEmpty(), "Did not expect rule " + ruleId);
    }
}
