package ujfe.validation;

import java.util.*;

public final class SeoValidator {
    public static final String DOCUMENT_TITLE = "seo.document.title";
    public static final String META_DESCRIPTION = "seo.document.meta-description";
    public static final String SINGLE_H1 = "seo.heading.single-h1";
    public static final String HEADING_HIERARCHY = "seo.heading.hierarchy";
    public static final String CANONICAL_LINK = "seo.link.canonical";
    public static final String DUPLICATE_TITLE = "seo.meta.duplicate-title";
    public static final String DUPLICATE_DESCRIPTION = "seo.meta.duplicate-description";
    public static final String HTML_LANG = "seo.html.lang";
    public static final String OPEN_GRAPH_BASIC = "seo.open-graph.basic";
    public static final String IMAGE_ALT = "seo.img.alt";

    private final ValidationOptions options;

    public SeoValidator() {
        this(ValidationOptions.seo());
    }

    public SeoValidator(ValidationOptions options) {
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
        validateTitle(document, findings);
        validateMetaDescription(document, findings);
        validateSingleH1(document, findings);
        validateHeadingHierarchy(document, findings);
        validateCanonicalLink(document, findings);
        validateHtmlLang(document, findings);
        validateOpenGraph(document, findings);
        validateImages(document, findings);
    }

    private void validateTitle(ValidationDocument document, List<ValidationFinding> findings) {
        List<ValidationElement> titles = document.elements("title");
        if (titles.isEmpty()) {
            add(findings, DOCUMENT_TITLE,
                "Document is missing a title.",
                "Add a non-empty <title> in the document head.",
                document.root(),
                "title",
                null);
            return;
        }
        for (ValidationElement title : titles) {
            if (!ValidationSupport.hasMeaningfulText(title.textContent())) {
                add(findings, DOCUMENT_TITLE,
                    "Document title is empty.",
                    "Provide a concise, descriptive title for the page.",
                    title,
                    "title",
                    null);
            }
        }
        if (titles.size() > 1) {
            add(findings, DUPLICATE_TITLE,
                "Document has more than one title tag.",
                "Render a single <title> element for the page.",
                titles.get(1),
                "title",
                null);
        }
    }

    private void validateMetaDescription(ValidationDocument document, List<ValidationFinding> findings) {
        List<ValidationElement> descriptions = metaByName(document, "description");
        if (descriptions.isEmpty()) {
            add(findings, META_DESCRIPTION,
                "Document is missing a meta description.",
                "Add <meta name=\"description\" content=\"...\"> with a concise page summary.",
                document.root(),
                "meta",
                "description");
            return;
        }
        for (ValidationElement description : descriptions) {
            if (!description.hasNonBlankAttribute("content")) {
                add(findings, META_DESCRIPTION,
                    "Meta description is empty.",
                    "Provide non-empty content for the description meta tag.",
                    description,
                    "meta",
                    "content");
            }
        }
        if (descriptions.size() > 1) {
            add(findings, DUPLICATE_DESCRIPTION,
                "Document has more than one meta description.",
                "Render one <meta name=\"description\"> element for the page.",
                descriptions.get(1),
                "meta",
                "description");
        }
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

    private void validateHeadingHierarchy(ValidationDocument document, List<ValidationFinding> findings) {
        int previousLevel = 0;
        for (ValidationElement element : document.elements()) {
            int level = ValidationSupport.headingLevel(element);
            if (level == 0) {
                continue;
            }
            if (previousLevel > 0 && level > previousLevel + 1) {
                add(findings, HEADING_HIERARCHY,
                    "Heading hierarchy skips a level.",
                    "Do not jump from h" + previousLevel + " to h" + level + "; add the missing intermediate heading level or adjust the heading.",
                    element,
                    element.tagName(),
                    null);
            }
            previousLevel = level;
        }
    }

    private void validateCanonicalLink(ValidationDocument document, List<ValidationFinding> findings) {
        if (!options.canonicalLinkValidationEnabled()) {
            return;
        }
        List<ValidationElement> canonicalLinks = new ArrayList<>();
        for (ValidationElement link : document.elements("link")) {
            if (ValidationSupport.relContains(link, "canonical")) {
                canonicalLinks.add(link);
            }
        }
        if (canonicalLinks.isEmpty()) {
            add(findings, CANONICAL_LINK,
                "Document is missing a canonical link.",
                "Add <link rel=\"canonical\" href=\"...\"> when canonical validation is enabled.",
                document.root(),
                "link",
                "canonical");
            return;
        }
        for (ValidationElement canonical : canonicalLinks) {
            String href = canonical.normalizedAttribute("href");
            if (href.isBlank() || href.contains(" ")) {
                add(findings, CANONICAL_LINK,
                    "Canonical link has an empty or invalid href.",
                    "Use a non-empty canonical URL without whitespace.",
                    canonical,
                    "link",
                    "href");
            }
        }
        if (canonicalLinks.size() > 1) {
            add(findings, CANONICAL_LINK,
                "Document has more than one canonical link.",
                "Render one canonical link for the page.",
                canonicalLinks.get(1),
                "link",
                "canonical");
        }
    }

    private void validateHtmlLang(ValidationDocument document, List<ValidationFinding> findings) {
        if (!options.htmlLangValidationEnabled()) {
            return;
        }
        Optional<ValidationElement> html = document.firstElement("html");
        if (html.isEmpty() || !html.get()
            .hasNonBlankAttribute("lang")) {
            add(findings, HTML_LANG,
                "Document html element is missing a lang attribute.",
                "Set the language with <html lang=\"...\"> for full document renders.",
                html.orElse(document.root()),
                "html",
                "lang");
        }
    }

    private void validateOpenGraph(ValidationDocument document, List<ValidationFinding> findings) {
        if (!options.openGraphValidationEnabled()) {
            return;
        }
        if (metaByProperty(document, "og:title").stream()
            .noneMatch(meta -> meta.hasNonBlankAttribute("content"))) {
            add(findings, OPEN_GRAPH_BASIC,
                "Open Graph title metadata is missing or empty.",
                "Add <meta property=\"og:title\" content=\"...\"> when Open Graph validation is enabled.",
                document.root(),
                "meta",
                "og:title");
        }
        if (metaByProperty(document, "og:description").stream()
            .noneMatch(meta -> meta.hasNonBlankAttribute("content"))) {
            add(findings, OPEN_GRAPH_BASIC,
                "Open Graph description metadata is missing or empty.",
                "Add <meta property=\"og:description\" content=\"...\"> when Open Graph validation is enabled.",
                document.root(),
                "meta",
                "og:description");
        }
    }

    private void validateImages(ValidationDocument document, List<ValidationFinding> findings) {
        for (ValidationElement image : document.elements("img")) {
            if (!ValidationSupport.imageHasAltTextOrDecorativeIntent(image)) {
                add(findings, IMAGE_ALT,
                    "Image is missing meaningful alt text or explicit decorative intent.",
                    "Add alt text for content images, or mark decorative images with explicit decorative intent.",
                    image,
                    "img",
                    "alt");
            }
        }
    }

    private List<ValidationElement> metaByName(ValidationDocument document, String name) {
        List<ValidationElement> matches = new ArrayList<>();
        for (ValidationElement meta : document.elements("meta")) {
            if (name.equalsIgnoreCase(meta.normalizedAttribute("name"))) {
                matches.add(meta);
            }
        }
        return matches;
    }

    private List<ValidationElement> metaByProperty(ValidationDocument document, String property) {
        List<ValidationElement> matches = new ArrayList<>();
        for (ValidationElement meta : document.elements("meta")) {
            if (property.equalsIgnoreCase(meta.normalizedAttribute("property"))) {
                matches.add(meta);
            }
        }
        return matches;
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
            .category(ValidationCategory.SEO)
            .message(message)
            .suggestion(suggestion)
            .element(tagName)
            .attribute(attribute)
            .location(element.location())
            .build());
    }
}
