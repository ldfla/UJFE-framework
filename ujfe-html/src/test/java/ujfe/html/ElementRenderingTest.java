package ujfe.html;

import org.junit.jupiter.api.Test;
import ujfe.core.Component;
import ujfe.core.UjfeContext;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ujfe.html.UI.*;

final class ElementRenderingTest {
    @Test
    void rendersEscapedText() {
        assertEquals("<p>&lt;script&gt;alert(&amp;)&lt;/script&gt;</p>", p("<script>alert(&)</script>").render());
    }

    @Test
    void rendersTextConvenienceHelpersDirectly() {
        assertEquals("<main></main>", main().render());
        assertEquals("<address>Contact</address>", address("Contact").render());
        assertEquals("<h4>Level 4</h4>", h4("Level 4").render());
        assertEquals("<h5>Level 5</h5>", h5("Level 5").render());
        assertEquals("<h6>Level 6</h6>", h6("Level 6").render());
        assertEquals("<small>Fine print</small>", small("Fine print").render());
        assertEquals("<mark>Highlighted</mark>", mark("Highlighted").render());
        assertEquals("<abbr>HTML</abbr>", abbr("HTML").render());
        assertEquals("<cite>Reference</cite>", cite("Reference").render());
        assertEquals("<blockquote>Quoted block</blockquote>", blockquote("Quoted block").render());
        assertEquals("<q>Inline quote</q>", q("Inline quote").render());
        assertEquals("<dt>Term</dt>", dt("Term").render());
    }

    @Test
    void rendersAttributesClassesAndChildren() {
        String html = div()
                .css("p-4 flex gap-2")
                .attr("data-test", "home\"page")
                .child(h1("Hello"))
                .child(p("World"))
                .render();

        assertEquals("<div data-test=\"home&quot;page\" class=\"p-4 flex gap-2\"><h1>Hello</h1><p>World</p></div>", html);
    }

    @Test
    void rendersDynamicTextFromSupplier() {
        AtomicInteger count = new AtomicInteger(1);
        Element paragraph = p(() -> "Counter: " + count.get());

        assertEquals("<p>Counter: 1</p>", paragraph.render());
        count.incrementAndGet();
        assertEquals("<p>Counter: 2</p>", paragraph.render());
    }

    @Test
    void rendersPublicConstructorAndChildVarargs() {
        Element article = new Element("article")
                .children(h1("Title"), p("Body"));

        assertEquals("<article><h1>Title</h1><p>Body</p></article>", article.render());
    }

    @Test
    void rendersComponentNodeChildren() {
        Component sample = () -> p("Component");

        assertEquals("<div><p>Component</p></div>", div().child(component(sample)).render());
    }

    @Test
    void rendersCoreNodeChildVarargs() {
        Component sample = () -> p("Component");
        ujfe.core.Node[] nodes = {component(sample), p("Tail")};

        assertEquals("<div><p>Component</p><p>Tail</p></div>", div().children(nodes).render());
    }

    @Test
    void rendersBooleanSupplierAttributes() {
        AtomicInteger required = new AtomicInteger(1);
        Element input = input().attr("required", () -> required.get() > 0);

        assertEquals("<input required>", input.render());
        required.set(0);
        assertEquals("<input>", input.render());
    }

    @Test
    void rendersClickEventAttributesWhenEventRegistrarExists() {
        UjfeContext context = UjfeContext.builder()
                .eventRegistrar(handler -> "evt-1")
                .build();

        String html = button("Go")
                .onClick(() -> {
                })
                .render(context);

        assertTrue(html.contains("data-ujfe-event=\"evt-1\""));
        assertTrue(html.contains("id=\"ujfe-1\""));
    }

    @Test
    void rendersFormElementsAndInputTypes() {
        String html = form()
                .method("post")
                .action("/signup")
                .child(label("Nome").forId("name"))
                .child(inputText()
                        .id("name")
                        .name("name")
                        .placeholder("Seu nome")
                        .required(true))
                .child(inputNumber()
                        .name("age")
                        .min("0")
                        .max("120")
                        .step("1"))
                .child(inputPassword().name("password"))
                .child(checkbox().name("terms").checked(true))
                .child(radio().name("plan").value("pro").checked(false))
                .child(select()
                        .name("role")
                        .child(option("Admin").value("admin").selected(true))
                        .child(option("User").value("user")))
                .child(textarea("Observacao").name("notes").rows(3).cols(20))
                .render();

        assertEquals("<form method=\"post\" action=\"/signup\">"
                + "<label for=\"name\">Nome</label>"
                + "<input type=\"text\" id=\"name\" name=\"name\" placeholder=\"Seu nome\" required>"
                + "<input type=\"number\" name=\"age\" min=\"0\" max=\"120\" step=\"1\">"
                + "<input type=\"password\" name=\"password\">"
                + "<input type=\"checkbox\" name=\"terms\" checked>"
                + "<input type=\"radio\" name=\"plan\" value=\"pro\">"
                + "<select name=\"role\">"
                + "<option value=\"admin\" selected>Admin</option>"
                + "<option value=\"user\">User</option>"
                + "</select>"
                + "<textarea name=\"notes\" rows=\"3\" cols=\"20\">Observacao</textarea>"
                + "</form>", html);
    }

    @Test
    void rendersAdditionalFormElements() {
        String html = fieldset()
                .child(legend("Preferencias"))
                .child(select()
                        .multiple(true)
                        .child(optgroup()
                                .attr("label", "Linguagens")
                                .child(option("Java").value("java"))))
                .child(inputEmail().name("email"))
                .child(inputSearch().name("query"))
                .child(inputTel().name("phone"))
                .child(inputUrl().name("site"))
                .child(inputHidden().name("token").value("abc"))
                .child(inputDate().name("date"))
                .child(inputTime().name("time"))
                .child(inputDateTimeLocal().name("createdAt"))
                .child(inputMonth().name("month"))
                .child(inputWeek().name("week"))
                .child(inputColor().name("color"))
                .child(inputFile().name("avatar"))
                .child(inputRange().name("volume").min("0").max("10"))
                .child(inputButton().value("Validar"))
                .child(inputImage().attr("src", "/submit.png").attr("alt", "Enviar"))
                .child(inputSubmit().value("Enviar"))
                .child(inputReset().value("Limpar"))
                .child(datalist().id("cities").child(option().value("Sao Paulo")))
                .child(output("42").name("result"))
                .child(progress().value("3").max("10"))
                .child(meter().value("0.7").min("0").max("1"))
                .render();

        assertTrue(html.contains("<fieldset><legend>Preferencias</legend>"));
        assertTrue(html.contains("<select multiple><optgroup label=\"Linguagens\"><option value=\"java\">Java</option></optgroup></select>"));
        assertTrue(html.contains("<input type=\"email\" name=\"email\">"));
        assertTrue(html.contains("<input type=\"search\" name=\"query\">"));
        assertTrue(html.contains("<input type=\"tel\" name=\"phone\">"));
        assertTrue(html.contains("<input type=\"url\" name=\"site\">"));
        assertTrue(html.contains("<input type=\"hidden\" name=\"token\" value=\"abc\">"));
        assertTrue(html.contains("<input type=\"datetime-local\" name=\"createdAt\">"));
        assertTrue(html.contains("<input type=\"button\" value=\"Validar\">"));
        assertTrue(html.contains("<input type=\"image\" src=\"/submit.png\" alt=\"Enviar\">"));
        assertTrue(html.contains("<datalist id=\"cities\"><option value=\"Sao Paulo\"></option></datalist>"));
        assertTrue(html.contains("<output name=\"result\">42</output>"));
        assertTrue(html.contains("<progress value=\"3\" max=\"10\"></progress>"));
        assertTrue(html.contains("<meter value=\"0.7\" min=\"0\" max=\"1\"></meter>"));
    }

    @Test
    void rendersDynamicAttributesAndBooleanAttributes() {
        AtomicInteger count = new AtomicInteger(1);
        Element input = inputText()
                .value(() -> "Counter: " + count.get())
                .checked(() -> count.get() > 1);

        assertEquals("<input type=\"text\" value=\"Counter: 1\">", input.render());
        count.incrementAndGet();
        assertEquals("<input type=\"text\" value=\"Counter: 2\" checked>", input.render());
    }

    @Test
    void rendersGlobalAttributesAndMediaElements() {
        String html = div()
                .id("media")
                .title("Media")
                .lang("pt-BR")
                .dir("ltr")
                .role("region")
                .ariaLabel("Galeria")
                .data("test-id", "media-panel")
                .tabindex(0)
                .accessKey("m")
                .contentEditable(false)
                .draggable(true)
                .spellcheck(false)
                .translate(false)
                .hidden(false)
                .child(canvas().width(320).height(180).ariaLabel("Canvas demo"))
                .child(video().src("/movie.mp4").poster("/poster.png").controls(true).playsInline(true))
                .child(audio().controls(true).child(source().src("https://cdn.example.test/audio.mp3").type("audio/mpeg")))
                .child(picture().child(source().src("/image.webp").attr("media", "(min-width: 800px)")).child(img().src("/image.png").alt("Imagem")))
                .render();

        assertTrue(html.contains("id=\"media\""));
        assertTrue(html.contains("title=\"Media\""));
        assertTrue(html.contains("lang=\"pt-BR\""));
        assertTrue(html.contains("role=\"region\""));
        assertTrue(html.contains("aria-label=\"Galeria\""));
        assertTrue(html.contains("data-test-id=\"media-panel\""));
        assertTrue(html.contains("accesskey=\"m\""));
        assertTrue(html.contains("contenteditable=\"false\""));
        assertTrue(html.contains("draggable=\"true\""));
        assertTrue(html.contains("spellcheck=\"false\""));
        assertTrue(html.contains("translate=\"no\""));
        assertTrue(html.contains("<canvas width=\"320\" height=\"180\" aria-label=\"Canvas demo\"></canvas>"));
        assertTrue(html.contains("<video src=\"/movie.mp4\" poster=\"/poster.png\" controls playsinline></video>"));
        assertTrue(html.contains("<audio controls><source src=\"https://cdn.example.test/audio.mp3\" type=\"audio/mpeg\"></audio>"));
        assertTrue(html.contains("<picture><source src=\"/image.webp\" media=\"(min-width: 800px)\"><img src=\"/image.png\" alt=\"Imagem\"></picture>"));
    }

    @Test
    void rendersInteractionAndWebComponentAttributes() {
        String html = details()
                .open(true)
                .child(summary("More"))
                .child(div()
                        .slot("content")
                        .part("panel")
                        .popover("manual")
                        .child("Body"))
                .render();

        assertEquals("<details open><summary>More</summary><div slot=\"content\" part=\"panel\" popover=\"manual\">Body</div></details>", html);
    }

    @Test
    void rendersExtendedFormAttributes() {
        AtomicInteger selected = new AtomicInteger(1);
        Element option = option("Java").selected(() -> selected.get() > 0);
        Element form = form()
                .child(inputText()
                        .enabled(false)
                        .readonly(true)
                        .autofocus(true)
                        .maxlength(30)
                        .minlength(2)
                        .autocomplete("name")
                        .inputMode("text")
                        .pattern("[A-Za-z ]+"))
                .child(select()
                        .child(optgroup()
                                .label("Languages")
                                .child(option)));

        String html = form.render();

        assertTrue(html.contains("<input type=\"text\" maxlength=\"30\" minlength=\"2\" autocomplete=\"name\" inputmode=\"text\" pattern=\"[A-Za-z ]+\" disabled readonly autofocus>"));
        assertTrue(html.contains("<optgroup label=\"Languages\"><option selected>Java</option></optgroup>"));

        selected.set(0);
        assertTrue(form.render().contains("<optgroup label=\"Languages\"><option>Java</option></optgroup>"));
    }

    @Test
    void rendersExtendedMediaAttributes() {
        assertEquals("<video src=\"/movie.mp4\" crossorigin=\"anonymous\" referrerpolicy=\"no-referrer\" autoplay loop muted></video>",
                video()
                        .src("/movie.mp4")
                        .crossorigin("anonymous")
                        .referrerPolicy("no-referrer")
                        .autoplay(true)
                        .loop(true)
                        .muted(true)
                        .render());
        assertEquals("<img src=\"/logo.png\" loading=\"lazy\">", img().src("/logo.png").loading("lazy").render());
    }

    @Test
    void rendersCommonHtmlTagThroughGenericModel() {
        Element element = Element.of("section")
                .attr("data-ready", true)
                .child("Ready");

        assertEquals(ElementNamespace.HTML, element.namespace());
        assertEquals("<section data-ready>Ready</section>", element.render());
    }

    @Test
    void rendersFutureHtmlElementThroughGenericModel() {
        Element element = Element.of("future-html-element")
                .attr("data-ready", true);

        assertEquals(ElementNamespace.HTML, element.namespace());
        assertEquals("<future-html-element data-ready></future-html-element>", element.render());
    }

    @Test
    void rendersCustomElementWithHyphen() {
        Element element = Element.of("my-card")
                .attr("role", "article")
                .child("Custom element");

        assertEquals("<my-card role=\"article\">Custom element</my-card>", element.render());
        assertEquals("my-card", Element.custom("my-card").tagName());
        assertThrows(IllegalArgumentException.class, () -> Element.custom("widget"));
    }

    @Test
    void rejectsInvalidGenericTagNamesBeforeRendering() {
        assertThrows(IllegalArgumentException.class, () -> Element.of(""));
        assertThrows(IllegalArgumentException.class, () -> Element.of(" "));
        assertThrows(IllegalArgumentException.class, () -> Element.of("<script>"));
        assertThrows(IllegalArgumentException.class, () -> Element.of("my tag"));
        assertThrows(IllegalArgumentException.class, () -> Element.of("div onclick=alert(1)"));
        assertThrows(IllegalArgumentException.class, () -> Element.of("\"script\""));
    }

    @Test
    void rendersSvgTagsThroughControlledNamespace() {
        Element svg = Element.of("svg")
                .attr("viewBox", "0 0 10 10")
                .child(Element.svg("path").attr("d", "M0 0h10v10H0z"));

        assertEquals(ElementNamespace.SVG, svg.namespace());
        assertEquals(ElementNamespace.SVG, Element.svg("path").namespace());
        assertEquals("<svg viewBox=\"0 0 10 10\"><path d=\"M0 0h10v10H0z\"></path></svg>", svg.render());
    }

    @Test
    void rendersMathMlTagsThroughControlledNamespace() {
        Element math = Element.of("math")
                .child(Element.mathMl("mi").child("x"))
                .child(Element.mathMl("mo").child("="))
                .child(Element.mathMl("mn").child("1"));

        assertEquals(ElementNamespace.MATHML, math.namespace());
        assertEquals(ElementNamespace.MATHML, Element.mathMl("mi").namespace());
        assertEquals("<math><mi>x</mi><mo>=</mo><mn>1</mn></math>", math.render());
    }

    @Test
    void helpersRemainOptionalSugarOverGenericElements() {
        assertEquals(Element.of("div").tagName(), div().tagName());
        assertEquals(ElementNamespace.HTML, div().namespace());
        assertEquals(Element.of("svg").tagName(), svg().tagName());
        assertEquals(ElementNamespace.SVG, svg().namespace());
        assertEquals(ElementNamespace.MATHML, math().namespace());
    }

    @Test
    void rendersGenericAndModernHtmlElements() {
        String html = section()
                .child(Element.of("dialog")
                        .attr("open", true)
                        .child(p("Example")))
                .child(details()
                        .attr("open", true)
                        .child(summary("More"))
                        .child(p("Details body")))
                .child(template()
                        .child(slot().attr("name", "content")))
                .child(table()
                        .child(caption("Metrics"))
                        .child(colgroup().child(col().attr("span", "2")))
                        .child(thead().child(tr().child(th("Name")).child(th("Value"))))
                        .child(tbody().child(tr().child(td("Users")).child(td("42"))))
                        .child(tfoot().child(tr().child(td("Total")).child(td("42")))))
                .child(figure()
                        .child(svg().attr("viewBox", "0 0 10 10")
                                .child(element("path").attr("d", "M0 0h10v10H0z")))
                        .child(figcaption("Vector")))
                .child(div().popover(true).child("Popover"))
                .render();

        assertTrue(html.contains("<dialog open><p>Example</p></dialog>"));
        assertTrue(html.contains("<details open><summary>More</summary><p>Details body</p></details>"));
        assertTrue(html.contains("<template><slot name=\"content\"></slot></template>"));
        assertTrue(html.contains("<caption>Metrics</caption>"));
        assertTrue(html.contains("<col span=\"2\">"));
        assertTrue(html.contains("<thead><tr><th>Name</th><th>Value</th></tr></thead>"));
        assertTrue(html.contains("<tbody><tr><td>Users</td><td>42</td></tr></tbody>"));
        assertTrue(html.contains("<tfoot><tr><td>Total</td><td>42</td></tr></tfoot>"));
        assertTrue(html.contains("<svg viewBox=\"0 0 10 10\"><path d=\"M0 0h10v10H0z\"></path></svg>"));
        assertTrue(html.contains("<div popover>Popover</div>"));
    }

    @Test
    void exposesHelpersForRequestedHtmlTags() {
        Element[] helpers = {
                html(), head(), body(), title(), meta(), link(), style(), script(), base(),
                main(), section(), article(), aside(), header(), footer(), nav(), address(),
                h1(), h2(), h3(), h4(), h5(), h6(), p(), span(), strong(), em(), small(), mark(), abbr(), cite(),
                code(), pre(), blockquote(), q(), br(), hr(),
                div(), figure(), figcaption(), details(), summary(), dialog(), modal(),
                ul(), ol(), li(), dl(), dt(), dd(),
                a(),
                img(), picture(), source(), audio(), video(), track(), canvas(), svg(), map(), area(), iframe(), embed(), object(), param(),
                table(), thead(), tbody(), tfoot(), tr(), td(), th(), caption(), colgroup(), col(),
                form(), input(), textarea(), button(), select(), option(), optgroup(), label(), fieldset(), legend(),
                datalist(), output(), progress(), meter(),
                template(), slot(), math()
        };

        for (Element helper : helpers) {
            assertNotNull(helper.tagName());
        }
    }

    @Test
    void rejectsUnsafeUrlAttributes() {
        assertThrows(IllegalArgumentException.class, () -> a("Bad").href("javascript:alert(1)"));
        assertThrows(IllegalArgumentException.class, () -> img().src("data:text/html,<script>alert(1)</script>"));
        assertEquals("<img src=\"data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==\">",
                img().src("data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==").render());
    }

    @Test
    void rendersFormLiveEventAttributesWhenEventRegistrarExists() {
        AtomicInteger nextEvent = new AtomicInteger();
        UjfeContext context = UjfeContext.builder()
                .eventRegistrar(handler -> "evt-" + nextEvent.incrementAndGet())
                .build();

        String html = form()
                .onSubmit(() -> {
                })
                .child(inputText().onInput(() -> {
                }))
                .child(select()
                        .onChange(() -> {
                        })
                        .child(option("Java").value("java")))
                .render(context);

        assertTrue(html.contains("data-ujfe-event-submit=\"evt-1\""));
        assertTrue(html.contains("data-ujfe-event-input=\"evt-2\""));
        assertTrue(html.contains("data-ujfe-event-change=\"evt-3\""));
    }
}
