package ujfe.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ComponentPreview implements Node {
    private final String title;
    private final List<Story> stories = new ArrayList<>();

    ComponentPreview(String title) {
        this.title = BrowserApiBridge.requireText(title, "title");
    }

    public ComponentPreview story(String name, Node node) {
        stories.add(new Story(name, node));
        return this;
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");
        Element root = UI.main()
            .data("ujfe-component-preview", "true")
            .child(UI.h1(title));
        for (Story story : stories) {
            root.child(UI.section()
                .data("ujfe-preview-story", story.name)
                .child(UI.h2(story.name))
                .child(story.node));
        }
        return root.render(context);
    }

    private static final class Story {
        private final String name;
        private final Node node;

        private Story(String name, Node node) {
            this.name = BrowserApiBridge.requireText(name, "name");
            this.node = Objects.requireNonNull(node, "node");
        }
    }
}
