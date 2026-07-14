package ujfe.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Reflection-based schema metadata for Java payload contracts.
 */
public final class TypeSchema {
    private final Class<?> type;
    private final List<Property> properties;

    private TypeSchema(Class<?> type) {
        this.type = Objects.requireNonNull(type, "type");
        this.properties = properties(type);
    }

    public static TypeSchema of(Class<?> type) {
        return new TypeSchema(type);
    }

    public Class<?> type() {
        return type;
    }

    public List<String> propertyNames() {
        List<String> names = new ArrayList<>();
        for (Property property : properties) {
            names.add(property.name);
        }
        return List.copyOf(names);
    }

    public String json() {
        StringBuilder json = new StringBuilder("{\"type\":\"")
            .append(UiContract.escape(type.getName()))
            .append("\",\"properties\":[");
        for (int index = 0; index < properties.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            Property property = properties.get(index);
            json.append("{\"name\":\"")
                .append(UiContract.escape(property.name))
                .append("\",\"type\":\"")
                .append(UiContract.escape(property.typeName))
                .append("\"}");
        }
        json.append("]}");
        return json.toString();
    }

    private static List<Property> properties(Class<?> type) {
        List<Property> properties = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            properties.add(new Property(field.getName(), field.getType().getName()));
        }
        properties.sort(Comparator.comparing(property -> property.name));
        return properties;
    }

    private static final class Property {
        private final String name;
        private final String typeName;

        private Property(String name, String typeName) {
            this.name = name;
            this.typeName = typeName;
        }
    }
}
