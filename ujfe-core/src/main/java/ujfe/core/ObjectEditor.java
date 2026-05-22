package ujfe.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Schema-driven object editor with accessible fields and optional JSON preview.
 */
public final class ObjectEditor implements Node {
    private final ObjectSchema schema;
    private final Map<String, String> values = new LinkedHashMap<>();
    private final Map<String, String> errors = new LinkedHashMap<>();
    private boolean jsonPreview;

    ObjectEditor(ObjectSchema schema) {
        this.schema = Objects.requireNonNull(schema, "schema");
    }

    public ObjectEditor value(String fieldName, Object value) {
        if (value != null) {
            values.put(requireText(fieldName, "fieldName"), value.toString());
        }
        return this;
    }

    public ObjectEditor values(Map<String, ?> values) {
        if (values == null) {
            return this;
        }
        values.forEach(this::value);
        return this;
    }

    public ObjectEditor error(String fieldName, String message) {
        errors.put(requireText(fieldName, "fieldName"), requireText(message, "message"));
        return this;
    }

    public ObjectEditor jsonPreview(boolean jsonPreview) {
        this.jsonPreview = jsonPreview;
        return this;
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");
        Element root = UI.fieldset()
            .data("ujfe-object-editor", "true")
            .child(UI.legend(schema.title()));
        for (ObjectSchema.Field field : schema.fields()) {
            root.child(fieldNode(field));
        }
        if (jsonPreview) {
            root.child(UI.div()
                .data("ujfe-json-preview", "true")
                .child(UI.pre()
                    .child(UI.code(previewJson()))));
        }
        return root.render(context);
    }

    private Node fieldNode(ObjectSchema.Field field) {
        String value = values.getOrDefault(field.name(), field.defaultValue());
        FormField formField = UI.field(field.name())
            .label(field.label())
            .required(field.required())
            .value(value);
        if (field.hint() != null) {
            formField.hint(field.hint());
        }
        if (errors.containsKey(field.name())) {
            formField.error(errors.get(field.name()));
        }
        switch (field.type()) {
            case NUMBER:
                formField.number();
                break;
            case BOOLEAN:
                formField.switchField()
                    .checked(Boolean.parseBoolean(value));
                break;
            case SELECT:
                formField.select();
                for (ObjectSchema.Option option : field.options()) {
                    formField.option(option.value(), option.label());
                }
                break;
            case SECRET:
                formField.secret();
                break;
            case JSON:
                formField.json();
                break;
            case TEXT:
            default:
                formField.text();
                break;
        }
        return formField;
    }

    private String previewJson() {
        StringBuilder json = new StringBuilder("{\n");
        boolean first = true;
        for (ObjectSchema.Field field : schema.fields()) {
            String value = values.getOrDefault(field.name(), field.defaultValue());
            if (!first) {
                json.append(",\n");
            }
            json.append("  \"")
                .append(escapeJson(field.name()))
                .append("\": ");
            if (field.type() == ObjectSchema.FieldType.NUMBER && isNumber(value)) {
                json.append(value);
            } else if (field.type() == ObjectSchema.FieldType.BOOLEAN) {
                json.append(Boolean.parseBoolean(value));
            } else {
                json.append('"')
                    .append(escapeJson(value == null ? "" : value))
                    .append('"');
            }
            first = false;
        }
        json.append("\n}");
        return json.toString();
    }

    private static boolean isNumber(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed);
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    escaped.append(current);
                    break;
            }
        }
        return escaped.toString();
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value.trim();
    }
}
