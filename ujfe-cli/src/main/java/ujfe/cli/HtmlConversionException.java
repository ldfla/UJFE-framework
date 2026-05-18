package ujfe.cli;

final class HtmlConversionException extends IllegalArgumentException {
    HtmlConversionException(String message) {
        super(message);
    }

    HtmlConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
