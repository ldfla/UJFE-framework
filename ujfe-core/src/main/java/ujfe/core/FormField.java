package ujfe.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Standard accessible form field wrapper for administrative screens.
 */
public final class FormField implements Node {
    public enum Kind {
        TEXT,
        NUMBER,
        PASSWORD,
        SELECT,
        CHECKBOX,
        SWITCH,
        TEXTAREA,
        DATE,
        TIME,
        DATETIME_LOCAL,
        SECRET,
        JSON,
        FILE,
        AUDIO_FILE
    }

    private final String name;
    private final List<Option> options = new ArrayList<>();
    private Kind kind = Kind.TEXT;
    private String id;
    private String label;
    private String hint;
    private String error;
    private String value;
    private String placeholder;
    private String autocomplete;
    private String inputMode;
    private String accept;
    private int rows = 4;
    private boolean required;
    private boolean disabled;
    private boolean readonly;
    private boolean checked;

    FormField(String name) {
        this.name = requireText(name, "name");
        this.id = "ujfe-field-" + safeId(name);
    }

    public FormField id(String id) {
        this.id = requireText(id, "id");
        return this;
    }

    public FormField label(String label) {
        this.label = requireText(label, "label");
        return this;
    }

    public FormField hint(String hint) {
        this.hint = requireText(hint, "hint");
        return this;
    }

    public FormField error(String error) {
        this.error = requireText(error, "error");
        return this;
    }

    public FormField value(Object value) {
        this.value = value == null ? null : value.toString();
        return this;
    }

    public FormField placeholder(String placeholder) {
        this.placeholder = requireText(placeholder, "placeholder");
        return this;
    }

    public FormField autocomplete(String autocomplete) {
        this.autocomplete = requireText(autocomplete, "autocomplete");
        return this;
    }

    public FormField inputMode(String inputMode) {
        this.inputMode = requireText(inputMode, "inputMode");
        return this;
    }

    public FormField required(boolean required) {
        this.required = required;
        return this;
    }

    public FormField disabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    public FormField readonly(boolean readonly) {
        this.readonly = readonly;
        return this;
    }

    public FormField checked(boolean checked) {
        this.checked = checked;
        return this;
    }

    public FormField text() {
        this.kind = Kind.TEXT;
        return this;
    }

    public FormField number() {
        this.kind = Kind.NUMBER;
        return this;
    }

    public FormField password() {
        this.kind = Kind.PASSWORD;
        return this;
    }

    public FormField secret() {
        this.kind = Kind.SECRET;
        return this;
    }

    public FormField select() {
        this.kind = Kind.SELECT;
        return this;
    }

    public FormField checkbox() {
        this.kind = Kind.CHECKBOX;
        return this;
    }

    public FormField switchField() {
        this.kind = Kind.SWITCH;
        return this;
    }

    public FormField textarea() {
        this.kind = Kind.TEXTAREA;
        return this;
    }

    public FormField rows(int rows) {
        if (rows < 1) {
            throw new IllegalArgumentException("rows must be greater than zero");
        }
        this.rows = rows;
        return this;
    }

    public FormField date() {
        this.kind = Kind.DATE;
        return this;
    }

    public FormField time() {
        this.kind = Kind.TIME;
        return this;
    }

    public FormField dateTimeLocal() {
        this.kind = Kind.DATETIME_LOCAL;
        return this;
    }

    public FormField json() {
        this.kind = Kind.JSON;
        this.rows = Math.max(rows, 8);
        return this;
    }

    public FormField file() {
        this.kind = Kind.FILE;
        return this;
    }

    public FormField audioFile() {
        this.kind = Kind.AUDIO_FILE;
        this.accept = "audio/*";
        return this;
    }

    public FormField accept(String accept) {
        this.accept = requireText(accept, "accept");
        return this;
    }

    public FormField option(String value, String label) {
        options.add(new Option(value, label));
        return this;
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");
        Element wrapper = UI.div()
            .data("ujfe-field", name)
            .data("ujfe-field-kind", kind.name()
                .toLowerCase());
        if (label != null && kind != Kind.CHECKBOX && kind != Kind.SWITCH) {
            wrapper.child(UI.label(label)
                .forId(id));
        }

        Element control = control();
        describe(control);

        if (kind == Kind.CHECKBOX || kind == Kind.SWITCH) {
            Element line = UI.label()
                .forId(id)
                .child(control)
                .child(UI.span(label == null ? name : label));
            wrapper.child(line);
        } else {
            wrapper.child(control);
        }

        if (hint != null) {
            wrapper.child(UI.p(hint)
                .id(id + "-hint")
                .data("ujfe-field-hint", "true"));
        }
        if (error != null) {
            wrapper.child(UI.p(error)
                .id(id + "-error")
                .role("alert")
                .data("ujfe-field-error", "true"));
        }
        return wrapper.render(context);
    }

    private Element control() {
        switch (kind) {
            case SELECT:
                return selectControl();
            case CHECKBOX:
                return inputBase(UI.checkbox()).checked(checked);
            case SWITCH:
                return inputBase(UI.checkbox()).checked(checked)
                    .role("switch")
                    .aria("checked", Boolean.toString(checked));
            case TEXTAREA:
            case JSON:
                return textareaControl();
            case NUMBER:
                return inputBase(UI.inputNumber());
            case PASSWORD:
            case SECRET:
                return inputBase(UI.inputPassword());
            case DATE:
                return inputBase(UI.inputDate());
            case TIME:
                return inputBase(UI.inputTime());
            case DATETIME_LOCAL:
                return inputBase(UI.inputDateTimeLocal());
            case FILE:
            case AUDIO_FILE:
                Element file = inputBase(UI.inputFile());
                if (accept != null) {
                    file.attr("accept", accept);
                }
                return file;
            case TEXT:
            default:
                return inputBase(UI.inputText());
        }
    }

    private Element inputBase(Element input) {
        input.id(id)
            .name(name)
            .required(required)
            .disabled(disabled)
            .readonly(readonly);
        if (value != null && kind != Kind.FILE && kind != Kind.AUDIO_FILE) {
            input.value(value);
        }
        if (placeholder != null) {
            input.placeholder(placeholder);
        }
        if (autocomplete != null) {
            input.autocomplete(autocomplete);
        }
        if (inputMode != null) {
            input.inputMode(inputMode);
        }
        if (kind == Kind.SECRET) {
            input.autocomplete("current-password")
                .data("ujfe-secret-field", "true");
        }
        return input;
    }

    private Element selectControl() {
        Element select = UI.select()
            .id(id)
            .name(name)
            .required(required)
            .disabled(disabled);
        for (Option option : options) {
            select.child(UI.option(option.label)
                .value(option.value)
                .selected(option.value.equals(value)));
        }
        return select;
    }

    private Element textareaControl() {
        Element textarea = UI.textarea(value == null ? "" : value)
            .id(id)
            .name(name)
            .rows(rows)
            .required(required)
            .disabled(disabled)
            .readonly(readonly);
        if (placeholder != null) {
            textarea.placeholder(placeholder);
        }
        if (kind == Kind.JSON) {
            textarea.data("ujfe-json-field", "true")
                .attr("spellcheck", "false");
        }
        return textarea;
    }

    private void describe(Element control) {
        List<String> ids = new ArrayList<>();
        if (hint != null) {
            ids.add(id + "-hint");
        }
        if (error != null) {
            ids.add(id + "-error");
            control.aria("invalid", "true");
        }
        if (!ids.isEmpty()) {
            control.aria("describedby", String.join(" ", ids));
        }
    }

    private static String safeId(String name) {
        StringBuilder builder = new StringBuilder(name.length());
        for (int index = 0; index < name.length(); index++) {
            char current = name.charAt(index);
            if (Character.isLetterOrDigit(current) || current == '-' || current == '_') {
                builder.append(current);
            } else {
                builder.append('-');
            }
        }
        if (builder.length() == 0 || !Character.isLetter(builder.charAt(0))) {
            builder.insert(0, 'f');
        }
        return builder.toString();
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value.trim();
    }

    private static final class Option {
        private final String value;
        private final String label;

        private Option(String value, String label) {
            this.value = requireText(value, "value");
            this.label = requireText(label, "label");
        }
    }
}
