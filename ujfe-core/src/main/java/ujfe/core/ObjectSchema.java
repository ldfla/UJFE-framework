package ujfe.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Small schema model for rendering typed object editors without coupling UJFE to
 * a specific JSON Schema implementation.
 */
public final class ObjectSchema {
    public enum FieldType {
        TEXT,
        NUMBER,
        BOOLEAN,
        SELECT,
        SECRET,
        JSON
    }

    private final List<Field> fields = new ArrayList<>();
    private String title = "Configuration";

    private ObjectSchema() {
    }

    public static ObjectSchema create() {
        return new ObjectSchema();
    }

    public ObjectSchema title(String title) {
        this.title = requireText(title, "title");
        return this;
    }

    public Field field(String name, String label, FieldType type) {
        Field field = new Field(name, label, type);
        fields.add(field);
        return field;
    }

    public String title() {
        return title;
    }

    public List<Field> fields() {
        return List.copyOf(fields);
    }

    public final class Field {
        private final String name;
        private final String label;
        private final FieldType type;
        private final List<Option> options = new ArrayList<>();
        private String hint;
        private String defaultValue;
        private boolean required;

        private Field(String name, String label, FieldType type) {
            this.name = requireText(name, "name");
            this.label = requireText(label, "label");
            this.type = Objects.requireNonNull(type, "type");
        }

        public Field required(boolean required) {
            this.required = required;
            return this;
        }

        public Field hint(String hint) {
            this.hint = requireText(hint, "hint");
            return this;
        }

        public Field defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue == null ? null : defaultValue.toString();
            return this;
        }

        public Field option(String value, String label) {
            options.add(new Option(value, label));
            return this;
        }

        public ObjectSchema done() {
            return ObjectSchema.this;
        }

        public String name() {
            return name;
        }

        public String label() {
            return label;
        }

        public FieldType type() {
            return type;
        }

        public String hint() {
            return hint;
        }

        public String defaultValue() {
            return defaultValue;
        }

        public boolean required() {
            return required;
        }

        public List<Option> options() {
            return List.copyOf(options);
        }
    }

    public static final class Option {
        private final String value;
        private final String label;

        private Option(String value, String label) {
            this.value = requireText(value, "value");
            this.label = requireText(label, "label");
        }

        public String value() {
            return value;
        }

        public String label() {
            return label;
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value.trim();
    }
}
