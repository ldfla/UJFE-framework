package ujfe.validation;

import java.util.Objects;
import java.util.stream.Collectors;

public final class ValidationException extends RuntimeException {
    private final ValidationResult result;

    public ValidationException(ValidationResult result) {
        super(messageFor(result));
        this.result = Objects.requireNonNull(result, "result");
    }

    public ValidationResult result() {
        return result;
    }

    private static String messageFor(ValidationResult result) {
        Objects.requireNonNull(result, "result");
        String findings = result.findings()
            .stream()
            .map(ValidationFinding::format)
            .collect(Collectors.joining(System.lineSeparator()));
        if (findings.isEmpty()) {
            return "Validation failed.";
        }
        return result.summary() + System.lineSeparator() + findings;
    }
}
