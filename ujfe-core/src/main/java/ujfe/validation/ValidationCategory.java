package ujfe.validation;

public enum ValidationCategory {
    ACCESSIBILITY("accessibility"),
    SEO("seo");

    private final String value;

    ValidationCategory(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
