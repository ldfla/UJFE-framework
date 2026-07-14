package ujfe.core;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class Sparkline implements Node {
    private final List<Integer> values;
    private int width = 120;
    private int height = 32;
    private String label = "Trend";
    private String tone = "neutral";

    Sparkline(List<Integer> values) {
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) {
            throw new IllegalArgumentException("sparkline values cannot be empty");
        }
        for (Integer value : values) {
            if (value == null) {
                throw new IllegalArgumentException("sparkline values cannot contain null");
            }
        }
        this.values = List.copyOf(values);
    }

    public Sparkline size(int width, int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("sparkline size must be positive");
        }
        this.width = width;
        this.height = height;
        return this;
    }

    public Sparkline label(String label) {
        this.label = BrowserApiBridge.requireText(label, "label");
        return this;
    }

    public Sparkline tone(String tone) {
        this.tone = BrowserApiBridge.requireText(tone, "tone");
        return this;
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");
        return UI.svg()
            .attr("viewBox", "0 0 " + width + " " + height)
            .attr("width", Integer.toString(width))
            .attr("height", Integer.toString(height))
            .role("img")
            .ariaLabel(label)
            .data("ujfe-sparkline", values.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")))
            .data("ujfe-tone", tone)
            .child(Element.svg("polyline")
                .attr("fill", "none")
                .attr("points", points()))
            .render(context);
    }

    private String points() {
        int min = values.stream()
            .mapToInt(Integer::intValue)
            .min()
            .orElse(0);
        int max = values.stream()
            .mapToInt(Integer::intValue)
            .max()
            .orElse(0);
        int range = Math.max(1, max - min);
        StringBuilder points = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                points.append(' ');
            }
            int x = values.size() == 1 ? width / 2 : Math.round((float) index * width / (values.size() - 1));
            int y = height - Math.round((float) (values.get(index) - min) * height / range);
            points.append(x)
                .append(',')
                .append(y);
        }
        return points.toString();
    }
}
