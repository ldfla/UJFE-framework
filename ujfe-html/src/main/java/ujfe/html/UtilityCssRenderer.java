package ujfe.html;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class UtilityCssRenderer {
    private static final Map<String, String> UTILITIES = createUtilities();

    private UtilityCssRenderer() {
    }

    public static String render(Collection<String> classes) {
        return render(classes, CssTheme.defaultTheme());
    }

    public static String render(Collection<String> classes, CssTheme theme) {
        Objects.requireNonNull(classes, "classes");
        Objects.requireNonNull(theme, "theme");

        StringBuilder css = new StringBuilder();
        css.append(theme.renderVariables())
                .append("*,*::before,*::after{box-sizing:border-box;}")
                .append("body{margin:0;font-family:system-ui,-apple-system,BlinkMacSystemFont,\"Segoe UI\",sans-serif;}")
                .append("h1,h2,h3,p,pre{margin:0;}")
                .append("code,kbd,pre,samp{font-family:inherit;}")
                .append("pre code{font-family:inherit;}")
                .append("button,input,select,textarea{font:inherit;}")
                .append("button{cursor:pointer;border:0;}")
                .append("@media(max-width:860px){.app-shell,.demo-grid,.docs-grid,.catalog-grid{grid-template-columns:1fr;}}");

        for (String className : classes) {
            String declarations = utilityDeclarations(className, theme);
            if (declarations != null) {
                css.append('.')
                        .append(escapeSelector(className))
                        .append('{')
                        .append(declarations)
                        .append('}');
            }
        }
        return css.toString();
    }

    private static Map<String, String> createUtilities() {
        Map<String, String> utilities = new LinkedHashMap<>();
        utilities.put("app-shell", "display:grid;grid-template-columns:16rem minmax(0,1fr);align-items:start;");
        utilities.put("demo-grid", "display:grid;grid-template-columns:repeat(2,minmax(0,1fr));");
        utilities.put("docs-grid", "display:grid;grid-template-columns:18rem minmax(0,1fr);align-items:start;");
        utilities.put("catalog-grid", "display:grid;grid-template-columns:repeat(3,minmax(0,1fr));align-items:start;");
        utilities.put("grid", "display:grid;");
        utilities.put("grid-cols-2", "grid-template-columns:repeat(2,minmax(0,1fr));");
        utilities.put("grid-cols-3", "grid-template-columns:repeat(3,minmax(0,1fr));");
        utilities.put("min-h-screen", "min-height:100vh;");
        utilities.put("max-w-6xl", "max-width:72rem;");
        utilities.put("max-w-7xl", "max-width:80rem;");
        utilities.put("mx-auto", "margin-left:auto;margin-right:auto;");
        utilities.put("w-full", "width:100%;");
        utilities.put("min-w-0", "min-width:0;");
        utilities.put("flex-1", "flex:1 1 0%;");
        utilities.put("flex", "display:flex;");
        utilities.put("inline-flex", "display:inline-flex;");
        utilities.put("flex-col", "flex-direction:column;");
        utilities.put("flex-wrap", "flex-wrap:wrap;");
        utilities.put("items-center", "align-items:center;");
        utilities.put("items-start", "align-items:flex-start;");
        utilities.put("items-stretch", "align-items:stretch;");
        utilities.put("justify-between", "justify-content:space-between;");
        utilities.put("justify-center", "justify-content:center;");
        utilities.put("justify-end", "justify-content:flex-end;");
        utilities.put("gap-1", "gap:0.25rem;");
        utilities.put("gap-2", "gap:0.5rem;");
        utilities.put("gap-3", "gap:0.75rem;");
        utilities.put("gap-4", "gap:1rem;");
        utilities.put("gap-6", "gap:1.5rem;");
        utilities.put("m-0", "margin:0;");
        utilities.put("mt-2", "margin-top:0.5rem;");
        utilities.put("mt-4", "margin-top:1rem;");
        utilities.put("p-2", "padding:0.5rem;");
        utilities.put("p-3", "padding:0.75rem;");
        utilities.put("p-4", "padding:1rem;");
        utilities.put("p-5", "padding:1.25rem;");
        utilities.put("p-6", "padding:1.5rem;");
        utilities.put("p-8", "padding:2rem;");
        utilities.put("px-2", "padding-left:0.5rem;padding-right:0.5rem;");
        utilities.put("px-3", "padding-left:0.75rem;padding-right:0.75rem;");
        utilities.put("px-4", "padding-left:1rem;padding-right:1rem;");
        utilities.put("px-6", "padding-left:1.5rem;padding-right:1.5rem;");
        utilities.put("py-1", "padding-top:0.25rem;padding-bottom:0.25rem;");
        utilities.put("py-2", "padding-top:0.5rem;padding-bottom:0.5rem;");
        utilities.put("py-3", "padding-top:0.75rem;padding-bottom:0.75rem;");
        utilities.put("py-4", "padding-top:1rem;padding-bottom:1rem;");
        utilities.put("rounded", "border-radius:0.25rem;");
        utilities.put("rounded-md", "border-radius:0.375rem;");
        utilities.put("rounded-lg", "border-radius:0.5rem;");
        utilities.put("border", "border-width:1px;border-style:solid;");
        utilities.put("border-b", "border-bottom-width:1px;border-bottom-style:solid;");
        utilities.put("border-slate-200", "border-color:#e2e8f0;");
        utilities.put("border-emerald-200", "border-color:#a7f3d0;");
        utilities.put("border-indigo-200", "border-color:#c7d2fe;");
        utilities.put("border-amber-200", "border-color:#fde68a;");
        utilities.put("border-rose-200", "border-color:#fecdd3;");
        utilities.put("shadow-sm", "box-shadow:0 1px 2px rgba(15,23,42,0.08);");
        utilities.put("text-center", "text-align:center;");
        utilities.put("text-left", "text-align:left;");
        utilities.put("text-xs", "font-size:0.75rem;line-height:1rem;");
        utilities.put("text-sm", "font-size:0.875rem;line-height:1.25rem;");
        utilities.put("text-base", "font-size:1rem;line-height:1.5rem;");
        utilities.put("text-lg", "font-size:1.125rem;line-height:1.75rem;");
        utilities.put("text-xl", "font-size:1.25rem;line-height:1.75rem;");
        utilities.put("text-2xl", "font-size:1.5rem;line-height:2rem;");
        utilities.put("text-3xl", "font-size:1.875rem;line-height:2.25rem;");
        utilities.put("text-4xl", "font-size:2.25rem;line-height:2.5rem;");
        utilities.put("font-normal", "font-weight:400;");
        utilities.put("font-medium", "font-weight:500;");
        utilities.put("font-semibold", "font-weight:600;");
        utilities.put("font-bold", "font-weight:700;");
        utilities.put("font-mono", "font-family:\"Fira Code\", Consolas, Monaco, \"Courier New\", monospace;");
        utilities.put("leading-relaxed", "line-height:1.625;");
        utilities.put("overflow-x-auto", "overflow-x:auto;");
        utilities.put("bg-stone-50", "background-color:#fafaf9;");
        utilities.put("bg-slate-50", "background-color:#f8fafc;");
        utilities.put("bg-slate-100", "background-color:#f1f5f9;");
        utilities.put("bg-white", "background-color:#fff;");
        utilities.put("bg-emerald-50", "background-color:#ecfdf5;");
        utilities.put("bg-emerald-700", "background-color:#047857;");
        utilities.put("bg-indigo-50", "background-color:#eef2ff;");
        utilities.put("bg-amber-50", "background-color:#fffbeb;");
        utilities.put("bg-rose-50", "background-color:#fff1f2;");
        utilities.put("bg-blue-600", "background-color:#2563eb;");
        utilities.put("bg-zinc-950", "background-color:#09090b;");
        utilities.put("bg-transparent", "background-color:transparent;");
        utilities.put("text-slate-900", "color:#0f172a;");
        utilities.put("text-slate-700", "color:#334155;");
        utilities.put("text-slate-600", "color:#475569;");
        utilities.put("text-slate-500", "color:#64748b;");
        utilities.put("text-emerald-700", "color:#047857;");
        utilities.put("text-indigo-700", "color:#4338ca;");
        utilities.put("text-amber-800", "color:#92400e;");
        utilities.put("text-rose-700", "color:#be123c;");
        utilities.put("text-white", "color:#fff;");
        utilities.put("text-zinc-50", "color:#fafafa;");
        return utilities;
    }

    private static String utilityDeclarations(String className, CssTheme theme) {
        String staticDeclarations = UTILITIES.get(className);
        if (staticDeclarations != null) {
            return staticDeclarations;
        }

        String spacingDeclarations = spacingDeclarations(className);
        if (spacingDeclarations != null) {
            return spacingDeclarations;
        }

        return themeDeclarations(className, theme);
    }

    private static String spacingDeclarations(String className) {
        boolean negative = className.startsWith("-");
        String token = negative ? className.substring(1) : className;

        String prefix = spacingPrefix(token);
        if (prefix == null) {
            return null;
        }
        if (negative && !prefix.startsWith("m")) {
            return null;
        }

        String scale = token.substring(prefix.length() + 1);
        String value = spacingValue(scale, negative);
        if (value == null) {
            return null;
        }

        switch (prefix) {
            case "gap":
                return "gap:" + value + ";";
            case "gap-x":
                return "column-gap:" + value + ";";
            case "gap-y":
                return "row-gap:" + value + ";";
            case "p":
                return "padding:" + value + ";";
            case "px":
                return "padding-left:" + value + ";padding-right:" + value + ";";
            case "py":
                return "padding-top:" + value + ";padding-bottom:" + value + ";";
            case "pt":
                return "padding-top:" + value + ";";
            case "pr":
                return "padding-right:" + value + ";";
            case "pb":
                return "padding-bottom:" + value + ";";
            case "pl":
                return "padding-left:" + value + ";";
            case "m":
                return "margin:" + value + ";";
            case "mx":
                return "margin-left:" + value + ";margin-right:" + value + ";";
            case "my":
                return "margin-top:" + value + ";margin-bottom:" + value + ";";
            case "mt":
                return "margin-top:" + value + ";";
            case "mr":
                return "margin-right:" + value + ";";
            case "mb":
                return "margin-bottom:" + value + ";";
            case "ml":
                return "margin-left:" + value + ";";
            default:
                return null;
        }
    }

    private static String spacingPrefix(String token) {
        String[] prefixes = {
                "gap-x", "gap-y", "gap",
                "px", "py", "pt", "pr", "pb", "pl", "p",
                "mx", "my", "mt", "mr", "mb", "ml", "m"
        };
        for (String prefix : prefixes) {
            if (token.startsWith(prefix + "-") && token.length() > prefix.length() + 1) {
                return prefix;
            }
        }
        return null;
    }

    private static String spacingValue(String scale, boolean negative) {
        if ("px".equals(scale)) {
            return negative ? "-1px" : "1px";
        }
        if (!scale.matches("\\d+(\\.\\d+)?")) {
            return null;
        }

        BigDecimal value = new BigDecimal(scale).multiply(new BigDecimal("0.25"));
        if (negative) {
            value = value.negate();
        }
        value = value.stripTrailingZeros();
        if (BigDecimal.ZERO.compareTo(value) == 0) {
            return "0";
        }
        return value.toPlainString() + "rem";
    }

    private static String themeDeclarations(String className, CssTheme theme) {
        String color = null;
        String property = null;
        if (className.startsWith("bg-")) {
            color = themeColor(className.substring(3), theme);
            property = "background-color";
        } else if (className.startsWith("text-")) {
            color = themeColor(className.substring(5), theme);
            property = "color";
        } else if (className.startsWith("border-")) {
            color = themeColor(className.substring(7), theme);
            property = "border-color";
        }

        if (color == null || property == null) {
            return null;
        }
        return property + ":" + color + ";";
    }

    private static String themeColor(String token, CssTheme theme) {
        if ("primary".equals(token)) {
            return themeVariable("primary", 500);
        }
        if ("secondary".equals(token)) {
            return themeVariable("secondary", 500);
        }

        int separator = token.lastIndexOf('-');
        if (separator <= 0 || separator == token.length() - 1) {
            return null;
        }

        String palette = token.substring(0, separator);
        if (!"primary".equals(palette) && !"secondary".equals(palette)) {
            return null;
        }

        try {
            int step = Integer.parseInt(token.substring(separator + 1));
            if (!CssTheme.STEPS.contains(step)) {
                return null;
            }
            return themeVariable(palette, step);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String themeVariable(String palette, int step) {
        return "var(--ujfe-" + palette + "-" + step + ")";
    }

    private static String escapeSelector(String className) {
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < className.length(); index++) {
            char current = className.charAt(index);
            if (Character.isLetterOrDigit(current) || current == '_' || current == '-') {
                escaped.append(current);
            } else {
                escaped.append('\\').append(Integer.toHexString(current)).append(' ');
            }
        }
        return escaped.toString();
    }
}
