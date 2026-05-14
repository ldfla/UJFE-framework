package app.model;

import java.util.Objects;

public final class Bank {
    private final String ispb;
    private final String code;
    private final String name;
    private final String fullName;

    public Bank(String ispb, String code, String name, String fullName) {
        this.ispb = Objects.requireNonNull(ispb, "ispb");
        this.code = Objects.requireNonNull(code, "code");
        this.name = Objects.requireNonNull(name, "name");
        this.fullName = Objects.requireNonNull(fullName, "fullName");
    }

    public String ispb() {
        return ispb;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public String fullName() {
        return fullName;
    }

    public String selectValue() {
        if (!code.isBlank()) {
            return code;
        }
        return ispb;
    }

    public String label() {
        if (!code.isBlank()) {
            return code + " - " + name;
        }
        return name;
    }
}
