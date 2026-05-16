package ujfe.html;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Public factory API for standard HTML helper methods.
 *
 * <p>These helpers are optional syntax sugar over {@link Element#of(String)} or
 * an equivalent namespace-aware factory. They must not be treated as a whitelist:
 * applications can always use {@link #element(String)} or {@link Element#of(String)}
 * for current, custom, or future elements that do not have a dedicated helper.</p>
 */
public final class UI {
    private UI() {
    }

    public static TextNode text(String value) {
        return new TextNode(value);
    }

    public static TextNode text(Supplier<String> valueSupplier) {
        return new TextNode(valueSupplier);
    }

    public static Element html(Node... children) {
        Element html = element("html");
        Arrays.stream(children).forEach(html::child);
        return html;
    }

    public static Element head() {
        return element("head");
    }

    public static Element body() {
        return element("body");
    }

    public static Element title() {
        return element("title");
    }

    public static Element title(String text) {
        return title().child(text);
    }

    public static Element meta() {
        return element("meta");
    }

    public static Element link() {
        return element("link");
    }

    public static Element style() {
        return element("style");
    }

    public static Element script() {
        return element("script");
    }

    public static Element base() {
        return element("base");
    }

    public static Element div() {
        return element("div");
    }

    public static Element figure() {
        return element("figure");
    }

    public static Element figcaption() {
        return element("figcaption");
    }

    public static Element figcaption(String text) {
        return figcaption().child(text);
    }

    public static Element details() {
        return element("details");
    }

    public static Element summary() {
        return element("summary");
    }

    public static Element summary(String text) {
        return summary().child(text);
    }

    public static Element dialog() {
        return element("dialog");
    }

    public static Element modal() {
        return dialog();
    }

    public static Element header() {
        return element("header");
    }

    public static Element main() {
        return element("main");
    }

    public static Element aside() {
        return element("aside");
    }

    public static Element section() {
        return element("section");
    }

    public static Element article() {
        return element("article");
    }

    public static Element nav() {
        return element("nav");
    }

    public static Element footer() {
        return element("footer");
    }

    public static Element address() {
        return element("address");
    }

    public static Element address(String text) {
        return address().child(text);
    }

    public static Element h1() {
        return element("h1");
    }

    public static Element h1(String text) {
        return h1().child(text);
    }

    public static Element h2() {
        return element("h2");
    }

    public static Element h2(String text) {
        return h2().child(text);
    }

    public static Element h3() {
        return element("h3");
    }

    public static Element h3(String text) {
        return h3().child(text);
    }

    public static Element h4() {
        return element("h4");
    }

    public static Element h4(String text) {
        return h4().child(text);
    }

    public static Element h5() {
        return element("h5");
    }

    public static Element h5(String text) {
        return h5().child(text);
    }

    public static Element h6() {
        return element("h6");
    }

    public static Element h6(String text) {
        return h6().child(text);
    }

    public static Element b() {
        return element("b");
    }

    public static Element b(String text) {
        return b().child(text);
    }

    public static Element i() {
        return element("i");
    }

    public static Element i(String text) {
        return i().child(text);
    }

    public static Element u() {
        return element("u");
    }

    public static Element u(String text) {
        return u().child(text);
    }

    public static Element em() {
        return element("em");
    }

    public static Element em(String text) {
        return em().child(text);
    }

    public static Element strong() {
        return element("strong");
    }

    public static Element strong(String text) {
        return strong().child(text);
    }

    public static Element small() {
        return element("small");
    }

    public static Element small(String text) {
        return small().child(text);
    }

    public static Element mark() {
        return element("mark");
    }

    public static Element mark(String text) {
        return mark().child(text);
    }

    public static Element abbr() {
        return element("abbr");
    }

    public static Element abbr(String text) {
        return abbr().child(text);
    }

    public static Element cite() {
        return element("cite");
    }

    public static Element cite(String text) {
        return cite().child(text);
    }

    public static Element p() {
        return element("p");
    }

    public static Element p(String text) {
        return p().child(text);
    }

    public static Element p(Supplier<String> textSupplier) {
        return p().child(textSupplier);
    }

    public static Element pre() {
        return element("pre");
    }

    public static Element code() {
        return element("code");
    }

    public static Element code(String text) {
        return code().child(text);
    }

    public static Element blockquote() {
        return element("blockquote");
    }

    public static Element blockquote(String text) {
        return blockquote().child(text);
    }

    public static Element q() {
        return element("q");
    }

    public static Element q(String text) {
        return q().child(text);
    }

    public static Element br() {
        return element("br");
    }

    public static Element hr() {
        return element("hr");
    }

    public static Element img() {
        return element("img");
    }

    public static Element picture() {
        return element("picture");
    }

    public static Element source() {
        return element("source");
    }

    public static Element track() {
        return element("track");
    }

    public static Element audio() {
        return element("audio");
    }

    public static Element video() {
        return element("video");
    }

    public static Element canvas() {
        return element("canvas");
    }

    public static Element svg() {
        return Element.svg("svg");
    }

    public static Element math() {
        return Element.mathMl("math");
    }

    public static Element map() {
        return element("map");
    }

    public static Element area() {
        return element("area");
    }

    public static Element iframe() {
        return element("iframe");
    }

    public static Element object() {
        return element("object");
    }

    public static Element embed() {
        return element("embed");
    }

    public static Element param() {
        return element("param");
    }

    public static Element table() {
        return element("table");
    }

    public static Element thead() {
        return element("thead");
    }

    public static Element tbody() {
        return element("tbody");
    }

    public static Element tfoot() {
        return element("tfoot");
    }

    public static Element tr() {
        return element("tr");
    }

    public static Element td() {
        return element("td");
    }

    public static Element td(String text) {
        return td().child(text);
    }

    public static Element th() {
        return element("th");
    }

    public static Element th(String text) {
        return th().child(text);
    }

    public static Element caption() {
        return element("caption");
    }

    public static Element caption(String text) {
        return caption().child(text);
    }

    public static Element colgroup() {
        return element("colgroup");
    }

    public static Element col() {
        return element("col");
    }

    public static Element form() {
        return element("form");
    }

    public static Element form(Node... children) {
        Element form = form();
        Arrays.stream(children).forEach(form::child);
        return form;
    }

    public static Element label() {
        return element("label");
    }

    public static Element label(String text) {
        return label().child(text);
    }

    public static Element input() {
        return element("input");
    }

    public static Element input(String type) {
        return input().type(type);
    }

    public static Element inputText() {
        return input("text");
    }

    public static Element inputNumber() {
        return input("number");
    }

    public static Element inputPassword() {
        return input("password");
    }

    public static Element inputEmail() {
        return input("email");
    }

    public static Element inputSearch() {
        return input("search");
    }

    public static Element inputTel() {
        return input("tel");
    }

    public static Element inputUrl() {
        return input("url");
    }

    public static Element inputHidden() {
        return input("hidden");
    }

    public static Element inputDate() {
        return input("date");
    }

    public static Element inputTime() {
        return input("time");
    }

    public static Element inputDateTimeLocal() {
        return input("datetime-local");
    }

    public static Element inputMonth() {
        return input("month");
    }

    public static Element inputWeek() {
        return input("week");
    }

    public static Element inputColor() {
        return input("color");
    }

    public static Element inputFile() {
        return input("file");
    }

    public static Element inputRange() {
        return input("range");
    }

    public static Element inputButton() {
        return input("button");
    }

    public static Element inputImage() {
        return input("image");
    }

    public static Element inputSubmit() {
        return input("submit");
    }

    public static Element inputReset() {
        return input("reset");
    }

    public static Element checkbox() {
        return input("checkbox");
    }

    public static Element radio() {
        return input("radio");
    }

    public static Element button() {
        return element("button");
    }

    public static Element button(String text) {
        return button().child(text);
    }

    public static Element a() {
        return element("a");
    }

    public static Element a(String text) {
        return a().child(text);
    }

    public static Element select() {
        return element("select");
    }

    public static Element option() {
        return element("option");
    }

    public static Element option(String text) {
        return option().child(text);
    }

    public static Element optgroup() {
        return element("optgroup");
    }

    public static Element textarea() {
        return element("textarea");
    }

    public static Element textarea(String text) {
        return textarea().child(text);
    }

    public static Element fieldset() {
        return element("fieldset");
    }

    public static Element legend() {
        return element("legend");
    }

    public static Element legend(String text) {
        return legend().child(text);
    }

    public static Element datalist() {
        return element("datalist");
    }

    public static Element output() {
        return element("output");
    }

    public static Element output(String text) {
        return output().child(text);
    }

    public static Element progress() {
        return element("progress");
    }

    public static Element meter() {
        return element("meter");
    }

    public static Element le() {
        return li();
    }

    public static Element li() {
        return element("li");
    }

    public static Element ul() {
        return element("ul");
    }

    public static Element ol() {
        return element("ol");
    }

    public static Element dt() {
        return element("dt");
    }

    public static Element dt(String text) {
        return dt().child(text);
    }

    public static Element dd() {
        return element("dd");
    }

    public static Element dd(String text) {
        return dd().child(text);
    }

    public static Element dl() {
        return element("dl");
    }

    public static Element span() {
        return element("span");
    }

    public static Element span(String text) {
        return span().child(text);
    }

    public static Element template() {
        return element("template");
    }

    public static Element slot() {
        return element("slot");
    }

    public static Element element(String tagName) {
        return Element.of(tagName);
    }
}
