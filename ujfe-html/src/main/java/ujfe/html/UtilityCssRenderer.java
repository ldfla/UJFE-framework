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
                .append("h1,h2,h3,p,pre,ul,ol{margin:0;}")
                .append("a{color:inherit;text-decoration:none;}")
                .append("code,kbd,pre,samp{font-family:inherit;}")
                .append("pre code{font-family:inherit;}")
                .append("button,input,select,textarea{font:inherit;}")
                .append("button{cursor:pointer;border:0;}")
                .append("@media(max-width:860px){.app-shell,.demo-grid,.docs-grid,.catalog-grid{grid-template-columns:1fr;}}");

        for (String className : classes) {
            String rule = renderRule(className);
            if (rule != null) {
                css.append(rule);
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
        utilities.put("grid-cols-1", "grid-template-columns:repeat(1,minmax(0,1fr));");
        utilities.put("grid-cols-2", "grid-template-columns:repeat(2,minmax(0,1fr));");
        utilities.put("grid-cols-3", "grid-template-columns:repeat(3,minmax(0,1fr));");
        utilities.put("grid-cols-4", "grid-template-columns:repeat(4,minmax(0,1fr));");
        utilities.put("col-span-1", "grid-column:span 1 / span 1;");
        utilities.put("col-span-2", "grid-column:span 2 / span 2;");
        utilities.put("col-span-3", "grid-column:span 3 / span 3;");
        utilities.put("relative", "position:relative;");
        utilities.put("absolute", "position:absolute;");
        utilities.put("sticky", "position:sticky;");
        utilities.put("top-0", "top:0;");
        utilities.put("top-24", "top:6rem;");
        utilities.put("right-0", "right:0;");
        utilities.put("z-50", "z-index:50;");
        utilities.put("overflow-hidden", "overflow:hidden;");
        utilities.put("min-h-screen", "min-height:100vh;");
        utilities.put("h-9", "height:2.25rem;");
        utilities.put("h-10", "height:2.5rem;");
        utilities.put("h-16", "height:4rem;");
        utilities.put("h-fit", "height:fit-content;");
        utilities.put("w-64", "width:16rem;");
        utilities.put("h-64", "height:16rem;");
        utilities.put("max-w-6xl", "max-width:72rem;");
        utilities.put("max-w-7xl", "max-width:80rem;");
        utilities.put("max-w-3xl", "max-width:48rem;");
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
        utilities.put("hidden", "display:none;");
        utilities.put("block", "display:block;");
        utilities.put("list-none", "list-style:none;padding-left:0;");
        utilities.put("gap-1", "gap:0.25rem;");
        utilities.put("gap-2", "gap:0.5rem;");
        utilities.put("gap-3", "gap:0.75rem;");
        utilities.put("gap-4", "gap:1rem;");
        utilities.put("gap-5", "gap:1.25rem;");
        utilities.put("gap-6", "gap:1.5rem;");
        utilities.put("gap-8", "gap:2rem;");
        utilities.put("m-0", "margin:0;");
        utilities.put("mt-1", "margin-top:0.25rem;");
        utilities.put("mt-2", "margin-top:0.5rem;");
        utilities.put("mt-4", "margin-top:1rem;");
        utilities.put("pt-2", "padding-top:0.5rem;");
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
        utilities.put("rounded-full", "border-radius:9999px;");
        utilities.put("border", "border-width:1px;border-style:solid;");
        utilities.put("border-b", "border-bottom-width:1px;border-bottom-style:solid;");
        utilities.put("border-t", "border-top-width:1px;border-top-style:solid;");
        utilities.put("border-slate-100", "border-color:#f1f5f9;");
        utilities.put("border-slate-200", "border-color:#e2e8f0;");
        utilities.put("border-slate-200/60", "border-color:rgba(226,232,240,0.6);");
        utilities.put("border-slate-200/80", "border-color:rgba(226,232,240,0.8);");
        utilities.put("border-slate-700", "border-color:#334155;");
        utilities.put("border-slate-800", "border-color:#1e293b;");
        utilities.put("border-emerald-200", "border-color:#a7f3d0;");
        utilities.put("border-emerald-200/60", "border-color:rgba(167,243,208,0.6);");
        utilities.put("border-indigo-100", "border-color:#e0e7ff;");
        utilities.put("border-indigo-200", "border-color:#c7d2fe;");
        utilities.put("border-indigo-500", "border-color:#6366f1;");
        utilities.put("border-amber-200", "border-color:#fde68a;");
        utilities.put("border-rose-200", "border-color:#fecdd3;");
        utilities.put("shadow-sm", "box-shadow:0 1px 2px rgba(15,23,42,0.08);");
        utilities.put("shadow-inner", "box-shadow:inset 0 2px 8px rgba(15,23,42,0.08);");
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
        utilities.put("text-5xl", "font-size:3rem;line-height:1;");
        utilities.put("font-normal", "font-weight:400;");
        utilities.put("font-medium", "font-weight:500;");
        utilities.put("font-semibold", "font-weight:600;");
        utilities.put("font-bold", "font-weight:700;");
        utilities.put("font-extrabold", "font-weight:800;");
        utilities.put("font-black", "font-weight:900;");
        utilities.put("font-sans", "font-family:system-ui,-apple-system,BlinkMacSystemFont,\"Segoe UI\",sans-serif;");
        utilities.put("font-mono", "font-family:\"Fira Code\", Consolas, Monaco, \"Courier New\", monospace;");
        utilities.put("uppercase", "text-transform:uppercase;");
        utilities.put("italic", "font-style:italic;");
        utilities.put("leading-none", "line-height:1;");
        utilities.put("leading-relaxed", "line-height:1.625;");
        utilities.put("overflow-x-auto", "overflow-x:auto;");
        utilities.put("outline-none", "outline:2px solid transparent;outline-offset:2px;");
        utilities.put("resize-none", "resize:none;");
        utilities.put("antialiased", "-webkit-font-smoothing:antialiased;-moz-osx-font-smoothing:grayscale;");
        utilities.put("transition-all", "transition-property:all;transition-duration:150ms;transition-timing-function:cubic-bezier(0.4,0,0.2,1);");
        utilities.put("transition-colors", "transition-property:color,background-color,border-color;transition-duration:150ms;transition-timing-function:cubic-bezier(0.4,0,0.2,1);");
        utilities.put("duration-200", "transition-duration:200ms;");
        utilities.put("backdrop-blur-md", "backdrop-filter:blur(12px);-webkit-backdrop-filter:blur(12px);");
        utilities.put("ring-1", "box-shadow:0 0 0 1px var(--ujfe-ring-color,rgba(99,102,241,0.45));");
        utilities.put("ring-indigo-500/50", "--ujfe-ring-color:rgba(99,102,241,0.5);");
        utilities.put("scale-[0.98]", "transform:scale(0.98);");
        utilities.put("bg-stone-50", "background-color:#fafaf9;");
        utilities.put("bg-slate-50", "background-color:#f8fafc;");
        utilities.put("bg-slate-50/50", "background-color:rgba(248,250,252,0.5);");
        utilities.put("bg-slate-100", "background-color:#f1f5f9;");
        utilities.put("bg-slate-800", "background-color:#1e293b;");
        utilities.put("bg-slate-900", "background-color:#0f172a;");
        utilities.put("bg-slate-950", "background-color:#020617;");
        utilities.put("bg-white", "background-color:#fff;");
        utilities.put("bg-white/80", "background-color:rgba(255,255,255,0.8);");
        utilities.put("bg-emerald-50", "background-color:#ecfdf5;");
        utilities.put("bg-emerald-600", "background-color:#059669;");
        utilities.put("bg-emerald-700", "background-color:#047857;");
        utilities.put("bg-indigo-50", "background-color:#eef2ff;");
        utilities.put("bg-indigo-50/40", "background-color:rgba(238,242,255,0.4);");
        utilities.put("bg-indigo-600", "background-color:#4f46e5;");
        utilities.put("bg-indigo-700", "background-color:#4338ca;");
        utilities.put("bg-amber-50", "background-color:#fffbeb;");
        utilities.put("bg-rose-50", "background-color:#fff1f2;");
        utilities.put("bg-blue-600", "background-color:#2563eb;");
        utilities.put("bg-zinc-950", "background-color:#09090b;");
        utilities.put("bg-transparent", "background-color:transparent;");
        utilities.put("bg-gradient-to-r", "background-image:linear-gradient(to right,var(--ujfe-gradient-from,#059669),var(--ujfe-gradient-to,#0d9488));");
        utilities.put("bg-gradient-to-br", "background-image:linear-gradient(to bottom right,var(--ujfe-gradient-from,#eef2ff),var(--ujfe-gradient-to,#f8fafc));");
        utilities.put("from-emerald-600", "--ujfe-gradient-from:#059669;");
        utilities.put("to-teal-600", "--ujfe-gradient-to:#0d9488;");
        utilities.put("from-indigo-50/40", "--ujfe-gradient-from:rgba(238,242,255,0.4);");
        utilities.put("to-slate-50/20", "--ujfe-gradient-to:rgba(248,250,252,0.2);");
        utilities.put("text-slate-900", "color:#0f172a;");
        utilities.put("text-slate-800", "color:#1e293b;");
        utilities.put("text-slate-700", "color:#334155;");
        utilities.put("text-slate-600", "color:#475569;");
        utilities.put("text-slate-500", "color:#64748b;");
        utilities.put("text-slate-400", "color:#94a3b8;");
        utilities.put("text-slate-300", "color:#cbd5e1;");
        utilities.put("text-slate-200", "color:#e2e8f0;");
        utilities.put("text-slate-100", "color:#f1f5f9;");
        utilities.put("text-emerald-400", "color:#34d399;");
        utilities.put("text-emerald-600", "color:#059669;");
        utilities.put("text-emerald-700", "color:#047857;");
        utilities.put("text-indigo-400", "color:#818cf8;");
        utilities.put("text-indigo-500", "color:#6366f1;");
        utilities.put("text-indigo-600", "color:#4f46e5;");
        utilities.put("text-indigo-700", "color:#4338ca;");
        utilities.put("text-indigo-950", "color:#1e1b4b;");
        utilities.put("text-sky-300", "color:#7dd3fc;");
        utilities.put("text-teal-300", "color:#5eead4;");
        utilities.put("text-amber-300", "color:#fcd34d;");
        utilities.put("text-amber-800", "color:#92400e;");
        utilities.put("text-rose-700", "color:#be123c;");
        utilities.put("text-white", "color:#fff;");
        utilities.put("text-zinc-50", "color:#fafafa;");
        return utilities;
    }

    private static String renderRule(String className) {
        Variant variant = Variant.parse(className);
        String declarations = utilityDeclarations(variant.utilityClass());
        if (declarations == null) {
            return null;
        }

        String selector = "." + escapeSelector(className) + variant.selectorSuffix();
        String rule = selector + "{" + declarations + "}";
        if (variant.mediaQuery() != null) {
            return variant.mediaQuery() + "{" + rule + "}";
        }
        return rule;
    }

    private static final class Variant {
        private final String utilityClass;
        private final String selectorSuffix;
        private final String mediaQuery;

        private Variant(String utilityClass, String selectorSuffix, String mediaQuery) {
            this.utilityClass = utilityClass;
            this.selectorSuffix = selectorSuffix;
            this.mediaQuery = mediaQuery;
        }

        static Variant parse(String className) {
            String mediaQuery = null;
            String utilityClass = className;

            if (utilityClass.startsWith("sm:")) {
                mediaQuery = "@media(min-width:640px)";
                utilityClass = utilityClass.substring(3);
            } else if (utilityClass.startsWith("md:")) {
                mediaQuery = "@media(min-width:768px)";
                utilityClass = utilityClass.substring(3);
            } else if (utilityClass.startsWith("lg:")) {
                mediaQuery = "@media(min-width:1024px)";
                utilityClass = utilityClass.substring(3);
            }

            String selectorSuffix = "";
            if (utilityClass.startsWith("hover:")) {
                selectorSuffix = ":hover";
                utilityClass = utilityClass.substring(6);
            } else if (utilityClass.startsWith("focus:")) {
                selectorSuffix = ":focus";
                utilityClass = utilityClass.substring(6);
            } else if (utilityClass.startsWith("active:")) {
                selectorSuffix = ":active";
                utilityClass = utilityClass.substring(7);
            } else if (utilityClass.startsWith("placeholder:")) {
                selectorSuffix = "::placeholder";
                utilityClass = utilityClass.substring(12);
            }

            return new Variant(utilityClass, selectorSuffix, mediaQuery);
        }

        String utilityClass() {
            return utilityClass;
        }

        String selectorSuffix() {
            return selectorSuffix;
        }

        String mediaQuery() {
            return mediaQuery;
        }
    }

    private static String utilityDeclarations(String className) {
        String staticDeclarations = UTILITIES.get(className);
        if (staticDeclarations != null) {
            return staticDeclarations;
        }

        String spacingDeclarations = spacingDeclarations(className);
        if (spacingDeclarations != null) {
            return spacingDeclarations;
        }

        return themeDeclarations(className);
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

    private static String themeDeclarations(String className) {
        String color = null;
        String property = null;
        if (className.startsWith("bg-")) {
            color = themeColor(className.substring(3));
            property = "background-color";
        } else if (className.startsWith("text-")) {
            color = themeColor(className.substring(5));
            property = "color";
        } else if (className.startsWith("border-")) {
            color = themeColor(className.substring(7));
            property = "border-color";
        }

        if (color == null) {
            return null;
        }
        return property + ":" + color + ";";
    }

    private static String themeColor(String token) {
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
