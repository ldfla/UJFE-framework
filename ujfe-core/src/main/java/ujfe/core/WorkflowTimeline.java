package ujfe.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Workflow state component for publish/deploy/rollback style operations.
 */
public final class WorkflowTimeline implements Node {
    private final List<Step> steps = new ArrayList<>();
    private String title;
    private String currentStatus;

    WorkflowTimeline() {
    }

    public WorkflowTimeline title(String title) {
        this.title = requireText(title, "title");
        return this;
    }

    public WorkflowTimeline currentStatus(String status) {
        this.currentStatus = requireText(status, "status");
        return this;
    }

    public WorkflowTimeline step(String label, String status) {
        return step(label, status, null);
    }

    public WorkflowTimeline step(String label, String status, String detail) {
        steps.add(new Step(label, status, detail));
        return this;
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");
        Element root = UI.section()
            .data("ujfe-workflow", "true");
        if (currentStatus != null) {
            root.data("ujfe-workflow-status", currentStatus);
        }
        if (title != null) {
            root.child(UI.h2(title));
        }
        Element list = UI.ol()
            .data("ujfe-workflow-timeline", "true");
        for (Step step : steps) {
            Element item = UI.li()
                .data("ujfe-workflow-step-status", step.status)
                .child(UI.strong(step.label))
                .child(UI.span(step.status));
            if (step.detail != null) {
                item.child(UI.p(step.detail));
            }
            list.child(item);
        }
        root.child(list);
        return root.render(context);
    }

    private static final class Step {
        private final String label;
        private final String status;
        private final String detail;

        private Step(String label, String status, String detail) {
            this.label = requireText(label, "label");
            this.status = requireText(status, "status");
            this.detail = detail == null || detail.isBlank() ? null : detail.trim();
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
