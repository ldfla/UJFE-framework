package app.pages;

import app.AppTheme;
import app.components.AppHeader;
import ujfe.core.Component;
import ujfe.core.Element;
import ujfe.core.Node;
import ujfe.router.Page;
import ujfe.signals.Signal;
import ujfe.signals.Signals;

import java.util.Objects;

import static ujfe.core.UI.*;

@Page("/forms")
public final class FormsPage implements Component {
    private final AppTheme theme;
    private final Signal<String> name = Signals.signal("");
    private final Signal<String> notes = Signals.signal("");
    private final Signal<String> profile = Signals.signal("fullstack");
    private final Signal<String> interests = Signals.signal("java\nsecurity");
    private final Signal<Boolean> newsletter = Signals.signal(false);
    private final Signal<String> plan = Signals.signal("starter");
    private final Signal<Integer> submits = Signals.signal(0);

    public FormsPage(AppTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    @Override
    public Node render() {
        return AppHeader.pageShell(theme)
            .child(new AppHeader(theme, AppHeader.FORMS).render())
            .child(main()
                .css("max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 flex flex-col gap-6")
                .child(section()
                    .css(theme.darkMode()
                        ? "max-w-3xl rounded-lg border border-slate-800 bg-slate-900 p-6 shadow-sm flex flex-col gap-5"
                        : "max-w-3xl rounded-lg border border-slate-200 bg-white p-6 shadow-sm flex flex-col gap-5")
                    .child(h1("Live form events").css(theme.darkMode() ? "text-3xl font-bold text-slate-100" : "text-3xl font-bold"))
                    .child(p("Input and change handlers receive the current element value on the JVM. Submit handlers prevent native form reloads and re-render the page through the live runtime.")
                        .css(theme.darkMode() ? "text-sm text-slate-300 leading-relaxed" : "text-sm text-slate-600 leading-relaxed"))
                    .child(form()
                        .onSubmit(() -> submits.update(value -> value + 1))
                        .css("flex flex-col gap-4")
                        .child(field("Name", "forms-name", inputText()
                            .name("name")
                            .placeholder("Said Adla")
                            .value(name::get)
                            .onInput(name::set)
                            .css(inputClass())))
                        .child(field("Notes", "forms-notes", textarea()
                            .name("notes")
                            .rows(3)
                            .onInput(notes::set)
                            .child(notes::get)
                            .css(inputClass() + " h-auto py-2")))
                        .child(field("Profile", "forms-profile", select()
                            .name("profile")
                            .onChange(profile::set)
                            .css(inputClass())
                            .child(option("Java backend").value("backend")
                                .selected(() -> "backend".equals(profile.get())))
                            .child(option("Full stack Java").value("fullstack")
                                .selected(() -> "fullstack".equals(profile.get())))))
                        .child(field("Interests", "forms-interests", select()
                            .name("interests")
                            .multiple(true)
                            .onChange(interests::set)
                            .css(inputClass() + " h-24 py-2")
                            .child(option("Java").value("java")
                                .selected(() -> selected(interests.get(), "java")))
                            .child(option("Security").value("security")
                                .selected(() -> selected(interests.get(), "security")))
                            .child(option("Runtime").value("runtime")
                                .selected(() -> selected(interests.get(), "runtime")))))
                        .child(label()
                            .css(theme.darkMode() ? "flex items-center gap-2 text-sm text-slate-300" : "flex items-center gap-2 text-sm text-slate-700")
                            .child(checkbox()
                                .name("newsletter")
                                .value("enabled")
                                .checked(newsletter::get)
                                .onChange(value -> newsletter.set(!value.isBlank())))
                            .child("Receive framework updates"))
                        .child(div()
                            .css(theme.darkMode() ? "flex flex-wrap gap-4 text-sm text-slate-300" : "flex flex-wrap gap-4 text-sm text-slate-700")
                            .child(label()
                                .css("flex items-center gap-2")
                                .child(radio()
                                    .name("plan")
                                    .value("starter")
                                    .checked(() -> "starter".equals(plan.get()))
                                    .onChange(this::selectPlan))
                                .child("Starter"))
                            .child(label()
                                .css("flex items-center gap-2")
                                .child(radio()
                                    .name("plan")
                                    .value("pro")
                                    .checked(() -> "pro".equals(plan.get()))
                                    .onChange(this::selectPlan))
                                .child("Pro")))
                        .child(button("Submit without reload")
                            .type("submit")
                            .css("h-10 rounded bg-primary-600 px-4 text-sm font-semibold text-white")))
                    .child(div()
                        .css(theme.darkMode()
                            ? "rounded bg-slate-800 border border-slate-700 p-4 text-sm text-slate-300 flex flex-col gap-2"
                            : "rounded bg-slate-100 border border-slate-200 p-4 text-sm text-slate-700 flex flex-col gap-2")
                        .child(p(() -> "Name: " + emptyFallback(name.get())))
                        .child(p(() -> "Notes: " + emptyFallback(notes.get())))
                        .child(p(() -> "Profile: " + profile.get()))
                        .child(p(() -> "Interests: " + selectedSummary(interests.get())))
                        .child(p(() -> "Newsletter: " + (newsletter.get() ? "enabled" : "disabled")))
                        .child(p(() -> "Plan: " + plan.get()))
                        .child(p(() -> "Submits: " + submits.get())))));
    }

    private Node field(String labelText, String id, Element control) {
        return div()
            .css("flex flex-col gap-1.5")
            .child(label(labelText).forId(id)
                .css(theme.darkMode() ? "text-xs font-semibold text-slate-200" : "text-xs font-semibold text-slate-700"))
            .child(control.id(id));
    }

    private String inputClass() {
        return theme.darkMode()
            ? "h-10 rounded border border-slate-700 bg-slate-950 px-3 text-sm text-slate-100 placeholder:text-slate-500"
            : "h-10 rounded border border-slate-300 px-3 text-sm";
    }

    private void selectPlan(String value) {
        if (!value.isBlank()) {
            plan.set(value);
        }
    }

    private static String emptyFallback(String value) {
        return value == null || value.isBlank() ? "not provided" : value;
    }

    private static boolean selected(String values, String expected) {
        if (values == null || values.isBlank()) {
            return false;
        }
        for (String value : values.split("\\n")) {
            if (expected.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static String selectedSummary(String values) {
        return values == null || values.isBlank() ? "none" : values.replace("\n", ", ");
    }
}
