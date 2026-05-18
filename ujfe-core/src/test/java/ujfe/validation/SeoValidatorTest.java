package ujfe.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class SeoValidatorTest {
    @Test
    void validSeoDocumentPasses() {
        ValidationResult result = fullSeoValidator().validate("<!doctype html>"
            + "<html lang=\"en\">"
            + "<head>"
            + "<title>Dashboard</title>"
            + "<meta name=\"description\" content=\"A useful dashboard.\">"
            + "<link rel=\"canonical\" href=\"https://example.test/dashboard\">"
            + "<meta property=\"og:title\" content=\"Dashboard\">"
            + "<meta property=\"og:description\" content=\"A useful dashboard.\">"
            + "</head>"
            + "<body>"
            + "<main>"
            + "<h1>Dashboard</h1>"
            + "<h2>Reports</h2>"
            + "<img src=\"/chart.png\" alt=\"Sales chart\">"
            + "</main>"
            + "</body>"
            + "</html>");

        assertTrue(result.isValid());
    }

    @Test
    void titleRulesDetectMissingEmptyAndDuplicateTitles() {
        assertHasRule("<html><head></head><body><h1>Title</h1></body></html>", SeoValidator.DOCUMENT_TITLE);
        assertHasRule("<html><head><title> </title></head><body><h1>Title</h1></body></html>", SeoValidator.DOCUMENT_TITLE);
        assertHasRule("<html><head><title>One</title><title>Two</title></head><body><h1>Title</h1></body></html>",
            SeoValidator.DUPLICATE_TITLE);
    }

    @Test
    void metaDescriptionRulesDetectMissingEmptyAndDuplicateDescriptions() {
        assertHasRule("<html><head><title>Title</title></head><body><h1>Title</h1></body></html>",
            SeoValidator.META_DESCRIPTION);
        assertHasRule("<html><head><title>Title</title><meta name=\"description\" content=\" \"></head><body><h1>Title</h1></body></html>",
            SeoValidator.META_DESCRIPTION);
        assertHasRule("<html><head>"
            + "<title>Title</title>"
            + "<meta name=\"description\" content=\"One\">"
            + "<meta name=\"description\" content=\"Two\">"
            + "</head><body><h1>Title</h1></body></html>", SeoValidator.DUPLICATE_DESCRIPTION);
    }

    @Test
    void h1AndHeadingHierarchyRulesAreDetected() {
        assertHasRule(validHead() + "<body><main><p>No h1</p></main></body></html>", SeoValidator.SINGLE_H1);
        assertHasRule(validHead() + "<body><main><h1>One</h1><h1>Two</h1></main></body></html>", SeoValidator.SINGLE_H1);
        assertHasRule(validHead() + "<body><main><h1>One</h1><h3>Skipped</h3></main></body></html>",
            SeoValidator.HEADING_HIERARCHY);
    }

    @Test
    void canonicalLinkIsValidatedOnlyWhenEnabled() {
        String document = validHead() + "<body><main><h1>Title</h1></main></body></html>";

        assertTrue(new SeoValidator().validate(document)
            .findingsForRule(SeoValidator.CANONICAL_LINK)
            .isEmpty());
        assertFalse(fullSeoValidator().validate(document)
            .findingsForRule(SeoValidator.CANONICAL_LINK)
            .isEmpty());
        assertHasRule(validHead() + "<link rel=\"canonical\" href=\" \"></head><body><main><h1>Title</h1></main></body></html>",
            SeoValidator.CANONICAL_LINK);
    }

    @Test
    void openGraphIsValidatedOnlyWhenEnabled() {
        String document = validHead() + "<body><main><h1>Title</h1></main></body></html>";

        assertTrue(new SeoValidator().validate(document)
            .findingsForRule(SeoValidator.OPEN_GRAPH_BASIC)
            .isEmpty());
        assertFalse(fullSeoValidator().validate(document)
            .findingsForRule(SeoValidator.OPEN_GRAPH_BASIC)
            .isEmpty());
    }

    @Test
    void htmlLangAndImageAltRulesAreDetected() {
        assertHasRule("<html><head><title>Title</title><meta name=\"description\" content=\"Description\"></head>"
            + "<body><main><h1>Title</h1></main></body></html>", SeoValidator.HTML_LANG);
        assertHasRule(validHead() + "<body><main><h1>Title</h1><img src=\"/chart.png\"></main></body></html>",
            SeoValidator.IMAGE_ALT);
    }

    private SeoValidator fullSeoValidator() {
        return new SeoValidator(ValidationOptions.builder()
            .seoValidationEnabled(true)
            .canonicalLinkValidationEnabled(true)
            .openGraphValidationEnabled(true)
            .htmlLangValidationEnabled(true)
            .build());
    }

    private void assertHasRule(String html, String ruleId) {
        assertFalse(fullSeoValidator().validate(html)
            .findingsForRule(ruleId)
            .isEmpty(), "Expected rule " + ruleId);
    }

    private String validHead() {
        return "<html lang=\"en\"><head><title>Title</title><meta name=\"description\" content=\"Description\">";
    }
}
