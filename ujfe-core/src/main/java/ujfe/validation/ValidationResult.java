package ujfe.validation;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ValidationResult {
    private static final ValidationResult EMPTY = new ValidationResult(List.of());

    private final List<ValidationFinding> findings;

    private ValidationResult(Collection<ValidationFinding> findings) {
        this.findings = List.copyOf(new LinkedHashSet<>(Objects.requireNonNull(findings, "findings")));
    }

    public static ValidationResult empty() {
        return EMPTY;
    }

    public static ValidationResult of(Collection<ValidationFinding> findings) {
        if (findings == null || findings.isEmpty()) {
            return empty();
        }
        return new ValidationResult(findings);
    }

    public List<ValidationFinding> findings() {
        return findings;
    }

    public boolean isValid() {
        return findings.isEmpty();
    }

    public boolean hasFindings() {
        return !findings.isEmpty();
    }

    public boolean hasErrors() {
        return findings.stream()
            .anyMatch(finding -> finding.severity() == ValidationSeverity.ERROR);
    }

    public List<ValidationFinding> errors() {
        return findings.stream()
            .filter(finding -> finding.severity() == ValidationSeverity.ERROR)
            .collect(Collectors.toUnmodifiableList());
    }

    public List<ValidationFinding> findingsForRule(String ruleId) {
        Objects.requireNonNull(ruleId, "ruleId");
        return findings.stream()
            .filter(finding -> ruleId.equals(finding.ruleId()))
            .collect(Collectors.toUnmodifiableList());
    }

    public void throwIfInvalid() {
        if (hasFindings()) {
            throw new ValidationException(this);
        }
    }

    public void throwIfErrors() {
        if (hasErrors()) {
            throw new ValidationException(ValidationResult.of(errors()));
        }
    }

    public String summary() {
        if (findings.isEmpty()) {
            return "Validation passed.";
        }
        return "Validation produced " + findings.size() + " finding(s).";
    }
}
