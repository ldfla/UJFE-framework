package ujfe.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Safe audio playback primitive with metadata, preload policy, and controlled
 * download affordance.
 */
public final class AudioPlayer implements Node {
    private final String sourceUrl;
    private final Map<String, String> metadata = new LinkedHashMap<>();
    private String title;
    private String preload = "metadata";
    private String downloadUrl;
    private String errorText = "Audio could not be loaded.";
    private String loadingText = "Loading audio";
    private String duration;
    private List<Integer> waveform;
    private boolean timeline = true;
    private boolean downloadAllowed;

    AudioPlayer(String sourceUrl) {
        this.sourceUrl = Objects.requireNonNull(sourceUrl, "sourceUrl");
    }

    public AudioPlayer title(String title) {
        this.title = requireText(title, "title");
        return this;
    }

    public AudioPlayer preload(String preload) {
        this.preload = requireText(preload, "preload");
        return this;
    }

    public AudioPlayer metadata(String label, Object value) {
        if (value != null) {
            metadata.put(requireText(label, "label"), value.toString());
        }
        return this;
    }

    public AudioPlayer downloadAllowed(boolean allowed) {
        this.downloadAllowed = allowed;
        return this;
    }

    public AudioPlayer downloadUrl(String downloadUrl) {
        this.downloadUrl = Objects.requireNonNull(downloadUrl, "downloadUrl");
        return this;
    }

    public AudioPlayer errorText(String errorText) {
        this.errorText = requireText(errorText, "errorText");
        return this;
    }

    public AudioPlayer loadingText(String loadingText) {
        this.loadingText = requireText(loadingText, "loadingText");
        return this;
    }

    public AudioPlayer duration(String duration) {
        this.duration = requireText(duration, "duration");
        return this;
    }

    public AudioPlayer timeline(boolean timeline) {
        this.timeline = timeline;
        return this;
    }

    public AudioPlayer waveform(List<Integer> samples) {
        Objects.requireNonNull(samples, "samples");
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("waveform samples cannot be empty");
        }
        for (Integer sample : samples) {
            if (sample == null || sample < 0 || sample > 100) {
                throw new IllegalArgumentException("waveform samples must be between 0 and 100");
            }
        }
        this.waveform = List.copyOf(samples);
        return this;
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");
        Element figure = UI.figure()
            .data("ujfe-audio-player", "true");
        if (title != null) {
            figure.child(UI.figcaption(title));
        }
        figure.child(UI.audio()
            .controls(true)
            .preload(preload)
            .src(sourceUrl)
            .data("ujfe-loading-text", loadingText)
            .child(errorText));
        if (timeline) {
            Element timelineElement = UI.div()
                .data("ujfe-audio-timeline", "true")
                .role("slider")
                .ariaLabel("Audio timeline")
                .aria("valuemin", "0")
                .aria("valuenow", "0");
            if (duration != null) {
                timelineElement.data("ujfe-duration", duration)
                    .aria("valuetext", "0 of " + duration);
            }
            figure.child(timelineElement);
        }
        if (waveform != null) {
            figure.child(UI.div()
                .data("ujfe-audio-waveform", waveform.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","))));
        }
        if (!metadata.isEmpty()) {
            Element list = UI.dl()
                .data("ujfe-audio-metadata", "true");
            metadata.forEach((label, value) -> list.child(UI.dt(label))
                .child(UI.dd(value)));
            figure.child(list);
        }
        if (downloadAllowed) {
            String href = downloadUrl == null ? sourceUrl : downloadUrl;
            figure.child(UI.a("Download")
                .href(href)
                .attr("download", true)
                .data("ujfe-audio-download", "true"));
        }
        return figure.render(context);
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value.trim();
    }
}
