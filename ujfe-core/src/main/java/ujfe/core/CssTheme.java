package ujfe.core;

import java.util.*;

public final class CssTheme {
    public static final List<Integer> STEPS = List.of(50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 950);

    private final Map<Integer, String> primary;
    private final Map<Integer, String> secondary;

    private CssTheme(String primaryColor, String secondaryColor) {
        this.primary = colorScale(primaryColor);
        this.secondary = colorScale(secondaryColor);
    }

    public static CssTheme defaultTheme() {
        return of("#2563eb", "#059669");
    }

    public static CssTheme of(String primaryColor, String secondaryColor) {
        return new CssTheme(primaryColor, secondaryColor);
    }

    public String primary(int step) {
        return color("primary", step);
    }

    public String secondary(int step) {
        return color("secondary", step);
    }

    public String color(String palette, int step) {
        Objects.requireNonNull(palette, "palette");
        Map<Integer, String> scale;
        if ("primary".equals(palette)) {
            scale = primary;
        } else if ("secondary".equals(palette)) {
            scale = secondary;
        } else {
            throw new IllegalArgumentException("Unsupported palette: " + palette);
        }

        String color = scale.get(step);
        if (color == null) {
            throw new IllegalArgumentException("Unsupported color step: " + step);
        }
        return color;
    }

    String renderVariables() {
        StringBuilder css = new StringBuilder(":root{");
        appendVariables(css, "primary", primary);
        appendVariables(css, "secondary", secondary);
        css.append('}');
        return css.toString();
    }

    private static void appendVariables(StringBuilder css, String palette, Map<Integer, String> scale) {
        for (Integer step : STEPS) {
            css.append("--ujfe-")
                    .append(palette)
                    .append('-')
                    .append(step)
                    .append(':')
                    .append(scale.get(step))
                    .append(';');
        }
    }

    private static Map<Integer, String> colorScale(String color) {
        int[] base = parseHexColor(color);
        Map<Integer, String> scale = new LinkedHashMap<>();
        scale.put(50, mix(base, new int[]{255, 255, 255}, 0.94));
        scale.put(100, mix(base, new int[]{255, 255, 255}, 0.88));
        scale.put(200, mix(base, new int[]{255, 255, 255}, 0.75));
        scale.put(300, mix(base, new int[]{255, 255, 255}, 0.60));
        scale.put(400, mix(base, new int[]{255, 255, 255}, 0.35));
        scale.put(500, toHex(base));
        scale.put(600, mix(base, new int[]{0, 0, 0}, 0.12));
        scale.put(700, mix(base, new int[]{0, 0, 0}, 0.24));
        scale.put(800, mix(base, new int[]{0, 0, 0}, 0.38));
        scale.put(900, mix(base, new int[]{0, 0, 0}, 0.52));
        scale.put(950, mix(base, new int[]{0, 0, 0}, 0.66));
        return Collections.unmodifiableMap(scale);
    }

    private static int[] parseHexColor(String color) {
        Objects.requireNonNull(color, "color");
        String normalized = color.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }

        if (normalized.length() == 3) {
            normalized = ""
                    + normalized.charAt(0) + normalized.charAt(0)
                    + normalized.charAt(1) + normalized.charAt(1)
                    + normalized.charAt(2) + normalized.charAt(2);
        }

        if (!normalized.matches("[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("CSS color must be #RGB or #RRGGBB: " + color);
        }

        return new int[]{
                Integer.parseInt(normalized.substring(0, 2), 16),
                Integer.parseInt(normalized.substring(2, 4), 16),
                Integer.parseInt(normalized.substring(4, 6), 16)
        };
    }

    private static String mix(int[] base, int[] target, double amount) {
        int red = channel(base[0], target[0], amount);
        int green = channel(base[1], target[1], amount);
        int blue = channel(base[2], target[2], amount);
        return toHex(new int[]{red, green, blue});
    }

    private static int channel(int base, int target, double amount) {
        return (int) Math.round(base + ((target - base) * amount));
    }

    private static String toHex(int[] rgb) {
        return String.format("#%02x%02x%02x", rgb[0], rgb[1], rgb[2]);
    }
}
