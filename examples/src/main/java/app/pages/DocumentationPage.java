package app.pages;

import app.AppTheme;
import app.components.BankSelectComponent;
import ujfe.html.Node;
import ujfe.router.Page;

import java.util.Objects;

import static ujfe.html.UI.*;

@Page("/docs")
public final class DocumentationPage {
    private final AppTheme theme;
    private final BankSelectComponent bankSelect = new BankSelectComponent();

    public DocumentationPage(AppTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    public Node render() {
        return div()
                .css("min-h-screen bg-slate-50 text-slate-900")
                .child(topBar())
                .child(
                        div()
                                .css("max-w-7xl mx-auto p-4 docs-grid gap-4")
                                .child(sidebar())
                                .child(
                                        main()
                                                .css("min-w-0 flex flex-col gap-4")
                                                .child(introSection())
                                                .child(themeSection())
                                                .child(elementCatalogSection())
                                                .child(formSection())
                                                .child(cssSection())
                                                .child(securitySection())
                                                .child(restSection())
                                                .child(cliSection())
                                                .child(devPreviewSection())
                                )
                );
    }

    private Node topBar() {
        return header()
                .css("border-b border-primary-200 bg-white")
                .child(
                        div()
                                .css("max-w-7xl mx-auto p-4 flex items-center justify-between gap-4")
                                .child(
                                        div()
                                                .css("flex flex-col gap-1")
                                                .child(span("UJFE Docs").css("text-sm font-bold text-primary-700"))
                                                .child(h1("Documentacao do DSL").css("text-3xl font-bold"))
                                )
                                .child(
                                        nav()
                                                .css("flex items-center gap-2")
                                                .child(a("Exemplo").attr("href", "/").css("px-3 py-2 rounded border border-slate-200 bg-white text-sm font-semibold text-slate-700"))
                                                .child(a("Docs").attr("href", "/docs").css("px-3 py-2 rounded bg-primary-700 text-white text-sm font-semibold"))
                                )
                );
    }

    private Node sidebar() {
        return aside()
                .css("rounded-lg border border-primary-200 bg-primary-50 p-4 shadow-sm")
                .child(nav()
                        .css("flex flex-col gap-3")
                        .child(h2("Mapa").css("text-xl font-bold text-primary-700"))
                        .child(p("Referencia visual dos elementos, formularios, CSS server-side e consumo REST.")
                                .css("text-sm text-slate-700 leading-relaxed"))
                        .child(ul()
                                .css("flex flex-col gap-2 text-sm text-slate-700")
                                .child(le().child(strong("Tema")).child(" com primary/secondary dinamicos"))
                                .child(le().child(strong("Elementos")).child(" com exemplos de uso"))
                                .child(le().child(strong("Formularios")).child(" com inputs e eventos"))
                                .child(le().child(strong("CSS")).child(" com utilitarios e escala de cores"))
                                .child(le().child(strong("Seguranca")).child(" com escaping, URL safe e headers"))
                                .child(le().child(strong("REST")).child(" com select vindo da BrasilAPI"))
                                .child(le().child(strong("CLI")).child(" com conversao HTML para UJFE"))
                                .child(le().child(strong("Dev Preview")).child(" com inspetor visual"))));
    }

    private Node introSection() {
        return section()
                .css("rounded-lg border border-slate-200 bg-white p-6 shadow-sm flex flex-col gap-3")
                .child(h2("Como ler esta pagina").css("text-2xl font-bold"))
                .child(p("Cada bloco mostra o resultado renderizado e o codigo Java equivalente. A pagina tambem usa classes CSS geradas no servidor e eventos Live para trocar o tema sem JavaScript manual.")
                        .css("text-base text-slate-700 leading-relaxed"))
                .child(codeBlock(factoryIndex()));
    }

    private Node themeSection() {
        return section()
                .css("rounded-lg border border-primary-200 bg-white p-6 shadow-sm flex flex-col gap-4")
                .child(h2("Tema dinamico").css("text-2xl font-bold text-primary-700"))
                .child(p(() -> "Primary: " + theme.primaryColor() + " | Secondary: " + theme.secondaryColor())
                        .css("text-sm font-mono text-slate-700"))
                .child(
                        div()
                                .css("flex flex-wrap gap-2")
                                .child(button("Azul + Verde")
                                        .css("px-4 py-2 rounded bg-primary-700 text-white font-semibold")
                                        .onClick(theme::useBlueEmerald))
                                .child(button("Rosa + Amber")
                                        .css("px-4 py-2 rounded border border-primary-200 bg-primary-50 text-primary-700 font-semibold")
                                        .onClick(theme::useRoseAmber))
                                .child(button("Indigo + Cyan")
                                        .css("px-4 py-2 rounded border border-secondary-200 bg-secondary-50 text-secondary-700 font-semibold")
                                        .onClick(theme::useIndigoCyan))
                )
                .child(
                        div()
                                .css("grid grid-cols-2 gap-3")
                                .child(colorScale("Primary", "primary"))
                                .child(colorScale("Secondary", "secondary"))
                )
                .child(codeBlock(themeCode()));
    }

    private Node colorScale(String title, String palette) {
        return div()
                .css("rounded-lg border border-slate-200 bg-white p-3 flex flex-col gap-2")
                .child(h3(title).css("text-lg font-bold text-slate-900"))
                .child(
                        div()
                                .css("grid grid-cols-3 gap-2")
                                .child(swatch("bg-" + palette + "-50 text-slate-900", palette + "-50"))
                                .child(swatch("bg-" + palette + "-200 text-slate-900", palette + "-200"))
                                .child(swatch("bg-" + palette + "-500 text-white", palette + "-500"))
                                .child(swatch("bg-" + palette + "-700 text-white", palette + "-700"))
                                .child(swatch("bg-" + palette + "-900 text-white", palette + "-900"))
                );
    }

    private Node swatch(String classes, String label) {
        return div()
                .css("rounded border border-slate-200 p-2 text-xs font-semibold " + classes)
                .child(label);
    }

    private Node elementCatalogSection() {
        return section()
                .css("flex flex-col gap-4")
                .child(h2("Elementos HTML suportados").css("text-2xl font-bold"))
                .child(
                        div()
                                .css("catalog-grid gap-4")
                                .child(docCard(
                                        "Layout e texto",
                                        "Containers, headings, texto, enfase e blocos de codigo.",
                                        layoutCode(),
                                        layoutPreview()
                                ))
                                .child(docCard(
                                        "Links, listas e midia",
                                        "Ancora, listas, imagem, canvas, audio, video e elementos descritivos.",
                                        listAndMediaCode(),
                                        listAndMediaPreview()
                                ))
                                .child(docCard(
                                        "Eventos Live",
                                        "Click, input, change e submit geram event ids e executam Runnable no Java.",
                                        liveEventCode(),
                                        liveEventPreview()
                                ))
                );
    }

    private Node formSection() {
        return section()
                .css("rounded-lg border border-secondary-200 bg-white p-6 shadow-sm flex flex-col gap-4")
                .child(h2("Formularios").css("text-2xl font-bold text-secondary-700"))
                .child(p("O DSL cobre os principais elementos de formulario e os tipos comuns de input.")
                        .css("text-base text-slate-700 leading-relaxed"))
                .child(
                        form()
                                .css("grid grid-cols-2 gap-3")
                                .onSubmit(() -> {
                                })
                                .child(label("Texto").forId("doc-text").css("text-sm font-semibold text-slate-700"))
                                .child(inputText().id("doc-text").name("text").placeholder("inputText()").css("w-full rounded border border-slate-200 bg-white p-2 text-sm"))
                                .child(label("Numero").forId("doc-number").css("text-sm font-semibold text-slate-700"))
                                .child(inputNumber().id("doc-number").name("number").min("0").max("99").css("w-full rounded border border-slate-200 bg-white p-2 text-sm"))
                                .child(label("Senha").forId("doc-password").css("text-sm font-semibold text-slate-700"))
                                .child(inputPassword().id("doc-password").name("password").placeholder("inputPassword()").css("w-full rounded border border-slate-200 bg-white p-2 text-sm"))
                                .child(label("Select").forId("doc-select").css("text-sm font-semibold text-slate-700"))
                                .child(select().id("doc-select").name("select").css("w-full rounded border border-slate-200 bg-white p-2 text-sm")
                                        .child(option("Java").value("java"))
                                        .child(option("UJFE").value("ujfe").selected(true)))
                                .child(label().css("flex items-center gap-2 text-sm text-slate-700").child(checkbox().name("check").checked(true)).child("checkbox()"))
                                .child(label().css("flex items-center gap-2 text-sm text-slate-700").child(radio().name("radio").value("a").checked(true)).child("radio()"))
                                .child(textarea("textarea()").name("message").rows(3).css("w-full rounded border border-slate-200 bg-white p-2 text-sm"))
                                .child(button("Submit live").type("submit").css("px-4 py-2 rounded bg-secondary-700 text-white font-semibold"))
                )
                .child(codeBlock(formCode()));
    }

    private Node cssSection() {
        return section()
                .css("rounded-lg border border-primary-200 bg-primary-50 p-6 shadow-sm flex flex-col gap-4")
                .child(h2("CSS server-side").css("text-2xl font-bold text-primary-700"))
                .child(p("O renderer coleta as classes usadas durante o render, gera somente os utilitarios encontrados e cria variaveis CSS para primary/secondary.")
                        .css("text-base text-slate-700 leading-relaxed"))
                .child(
                        div()
                                .css("grid grid-cols-3 gap-3")
                                .child(cssPill("Layout", "flex, grid, app-shell, demo-grid, docs-grid"))
                                .child(cssPill("Espacamento", "p-2..p-8, px-*, py-*, gap-*"))
                                .child(cssPill("Tipografia", "text-xs..text-4xl, font-*, font-mono"))
                                .child(cssPill("Bordas", "rounded, border, border-primary-200"))
                                .child(cssPill("Cores", "bg-primary-50, text-secondary-700"))
                                .child(cssPill("Responsivo", "media query para grids do exemplo"))
                )
                .child(codeBlock(cssCode()));
    }

    private Node securitySection() {
        return section()
                .css("rounded-lg border border-rose-200 bg-white p-6 shadow-sm flex flex-col gap-4")
                .child(h2("Seguranca por padrao").css("text-2xl font-bold text-rose-700"))
                .child(p("Texto e atributos sao escapados durante o SSR. Atributos de URL como href, src, action e poster rejeitam protocolos perigosos antes de renderizar.")
                        .css("text-base text-slate-700 leading-relaxed"))
                .child(
                        div()
                                .css("grid grid-cols-3 gap-3")
                                .child(cssPill("A03 Injection", "escaping HTML/atributos e URLs seguras"))
                                .child(cssPill("A05 Misconfiguration", "CSP, nosniff, frame-ancestors"))
                                .child(cssPill("A01 Access Control", "eventId opaco e handlers no servidor"))
                )
                .child(codeBlock(securityCode()));
    }

    private Node restSection() {
        return section()
                .css("rounded-lg border border-secondary-200 bg-white p-6 shadow-sm flex flex-col gap-4")
                .child(h2("Componentes com API REST").css("text-2xl font-bold text-secondary-700"))
                .child(p("O core inclui RestClient baseado no HttpClient do Java 11. O componente abaixo consulta a BrasilAPI e usa o resultado para preencher um select.")
                        .css("text-base text-slate-700 leading-relaxed"))
                .child(bankSelect.render())
                .child(codeBlock(restCode()));
    }

    private Node cliSection() {
        return section()
                .css("rounded-lg border border-primary-200 bg-primary-50 p-6 shadow-sm flex flex-col gap-4")
                .child(h2("CLI").css("text-2xl font-bold text-primary-700"))
                .child(p("O primeiro comando do CLI converte HTML puro para uma pagina Java UJFE. React/JSX fica fora do MVP porque precisa lidar com props dinamicas, hooks, condicionais, map e componentes externos.")
                        .css("text-base text-slate-700 leading-relaxed"))
                .child(
                        div()
                                .css("grid grid-cols-2 gap-3")
                                .child(
                                        div()
                                                .css("rounded-lg border border-primary-200 bg-white p-3 flex flex-col gap-2")
                                                .child(h3("Comando").css("text-lg font-bold text-primary-700"))
                                                .child(codeBlock(cliCommandCode()))
                                )
                                .child(
                                        div()
                                                .css("rounded-lg border border-primary-200 bg-white p-3 flex flex-col gap-2")
                                                .child(h3("Entrada HTML").css("text-lg font-bold text-primary-700"))
                                                .child(codeBlock(cliInputCode()))
                                )
                )
                .child(h3("Saida gerada").css("text-lg font-bold text-primary-700"))
                .child(codeBlock(cliOutputCode()));
    }

    private Node devPreviewSection() {
        return section()
                .css("rounded-lg border border-primary-200 bg-white p-6 shadow-sm flex flex-col gap-4")
                .child(h2("Dev Preview visual").css("text-2xl font-bold text-primary-700"))
                .child(p("No exemplo, o servidor inicia com Dev Preview ativo. O painel flutuante permite selecionar elementos, ver tag/id e testar classes CSS diretamente no browser sem alterar o Java.")
                        .css("text-base text-slate-700 leading-relaxed"))
                .child(codeBlock(devPreviewCode()));
    }

    private Node docCard(String title, String description, String code, Node preview) {
        return div()
                .css("rounded-lg border border-slate-200 bg-white p-4 shadow-sm flex flex-col gap-3")
                .child(h3(title).css("text-xl font-bold"))
                .child(p(description).css("text-sm text-slate-700 leading-relaxed"))
                .child(div().css("rounded-md border border-slate-200 bg-slate-50 p-3 flex flex-col gap-2").child(preview))
                .child(codeBlock(code));
    }

    private Node cssPill(String title, String body) {
        return div()
                .css("rounded-lg border border-primary-200 bg-white p-3 flex flex-col gap-1")
                .child(strong(title).css("text-primary-700"))
                .child(span(body).css("text-xs text-slate-600 font-mono"));
    }

    private Node codeBlock(String codeSample) {
        return pre()
                .css("overflow-x-auto rounded-lg bg-zinc-950 text-zinc-50 p-4 text-sm font-mono")
                .child(code(codeSample).css("font-mono"));
    }

    private Node layoutPreview() {
        return div()
                .css("flex flex-col gap-2")
                .child(h1("h1").css("text-3xl font-bold text-primary-700"))
                .child(h2("h2").css("text-2xl font-semibold"))
                .child(h3("h3").css("text-lg font-semibold text-secondary-700"))
                .child(p("p() com ").child(strong("strong")).child(", ").child(em("em")).child(" e ").child(u("u")))
                .child(pre().css("rounded bg-zinc-950 text-zinc-50 p-2 text-xs font-mono").child(code("pre().child(code(...))").css("font-mono")));
    }

    private Node listAndMediaPreview() {
        return div()
                .css("flex flex-col gap-2")
                .child(a("Link para home").href("/").title("Voltar para home").css("text-primary-700 font-semibold"))
                .child(ul().css("flex flex-col gap-1 text-sm").child(le().child("ul + le/li")).child(le().child("item dois")))
                .child(ol().css("flex flex-col gap-1 text-sm").child(le().child("ol + le/li")).child(le().child("item dois")))
                .child(dl().css("text-sm").child(dt().child(strong("dt()")).child(" dentro de dl()")))
                .child(canvas().width(240).height(72).ariaLabel("Canvas vazio").css("border border-slate-200 rounded bg-white"))
                .child(video().src("/demo.mp4").poster("/poster.png").controls(true).preload("metadata").css("w-full rounded border border-slate-200"))
                .child(audio().controls(true).preload("metadata").child(source().src("/audio.mp3").type("audio/mpeg")))
                .child(img().src("data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==").alt("pixel").css("border border-slate-200 rounded"));
    }

    private Node liveEventPreview() {
        return div()
                .css("flex flex-col gap-2")
                .child(button("onClick").css("px-3 py-2 rounded bg-primary-700 text-white font-semibold").onClick(() -> {
                }))
                .child(inputText().placeholder("onInput").css("w-full rounded border border-slate-200 p-2 text-sm").onInput(() -> {
                }))
                .child(select().css("w-full rounded border border-slate-200 p-2 text-sm").onChange(() -> {
                }).child(option("onChange").value("change")))
                .child(form().onSubmit(() -> {
                }).child(button("onSubmit").type("submit").css("px-3 py-2 rounded bg-secondary-700 text-white font-semibold")));
    }

    private String factoryIndex() {
        return ""
                + "import static ujfe.html.UI.*;\n\n"
                + "// Texto e estrutura\n"
                + "text(), div(), header(), main(), aside(), section(), nav(), span()\n"
                + "h1(), h2(), h3(), h4(), h5(), p(), br(), pre(), code()\n"
                + "b(), i(), u(), em(), strong()\n\n"
                + "// Atributos globais fluentes em qualquer Element\n"
                + ".id(), .title(), .lang(), .dir(), .role(), .aria(), .ariaLabel()\n"
                + ".data(), .tabindex(), .accessKey(), .contentEditable(), .enabled()\n\n"
                + "// Links, listas e midia\n"
                + "a(), img(), picture(), source(), track(), audio(), video(), canvas()\n"
                + "map(), area(), iframe(), object(), embed(), param()\n"
                + "ul(), ol(), le(), li(), dl(), dt(), html()\n\n"
                + "// Formularios\n"
                + "form(), label(), input(), inputText(), inputNumber(), inputPassword()\n"
                + "inputEmail(), inputSearch(), inputTel(), inputUrl(), inputHidden()\n"
                + "inputDate(), inputTime(), inputDateTimeLocal(), inputMonth(), inputWeek()\n"
                + "inputColor(), inputFile(), inputRange(), inputButton(), inputImage()\n"
                + "inputSubmit(), inputReset(), checkbox(), radio(), select(), option()\n"
                + "optgroup(), textarea(), fieldset(), legend(), datalist(), output()\n"
                + "progress(), meter(), button(), element(\"custom-tag\")\n";
    }

    private String themeCode() {
        return ""
                + "AppTheme appTheme = new AppTheme();\n"
                + "Router router = new Router().register(new DocumentationPage(appTheme));\n"
                + "LiveSession liveSession = new LiveSession(router, appTheme::cssTheme);\n\n"
                + "public CssTheme cssTheme() {\n"
                + "    return CssTheme.of(primaryColor.get(), secondaryColor.get());\n"
                + "}\n\n"
                + "div().css(\"bg-primary-50 border border-primary-200 text-primary-700\")\n"
                + "button(\"Trocar tema\").onClick(appTheme::useRoseAmber)\n";
    }

    private String layoutCode() {
        return ""
                + "div()\n"
                + "    .child(header().child(h1(\"Titulo\")))\n"
                + "    .child(main().child(section().child(h2(\"Secao\"))))\n"
                + "    .child(aside().child(nav().child(a(\"Docs\").attr(\"href\", \"/docs\"))))\n"
                + "    .child(p(\"Texto\").child(br()).child(strong(\"forte\")))\n"
                + "    .child(pre().child(code(\"codigo\")));\n";
    }

    private String listAndMediaCode() {
        return ""
                + "a(\"Home\").attr(\"href\", \"/\")\n"
                + "img().src(\"/logo.png\").alt(\"Logo\")\n"
                + "canvas().width(320).height(180).ariaLabel(\"Grafico\")\n"
                + "video().src(\"/demo.mp4\").poster(\"/poster.png\").controls(true)\n"
                + "audio().controls(true).child(source().src(\"/audio.mp3\").type(\"audio/mpeg\"))\n"
                + "param().attr(\"name\", \"autoplay\").attr(\"value\", \"false\")\n"
                + "ul().child(le().child(\"Item\"))\n"
                + "ol().child(li().child(\"Item\"))\n"
                + "dl().child(dt().child(\"Termo\"));\n";
    }

    private String liveEventCode() {
        return ""
                + "button(\"Salvar\").onClick(this::save)\n"
                + "inputText().onInput(this::markDirty)\n"
                + "select().onChange(this::reload)\n"
                + "form().onSubmit(this::submit);\n";
    }

    private String formCode() {
        return ""
                + "form()\n"
                + "    .onSubmit(this::submit)\n"
                + "    .child(label(\"Nome\").forId(\"name\"))\n"
                + "    .child(inputText().id(\"name\").name(\"name\").required(true))\n"
                + "    .child(inputNumber().name(\"age\").min(\"0\").max(\"120\"))\n"
                + "    .child(inputPassword().name(\"password\"))\n"
                + "    .child(checkbox().name(\"terms\").checked(true))\n"
                + "    .child(radio().name(\"plan\").value(\"pro\"))\n"
                + "    .child(select().child(option(\"Java\").value(\"java\")))\n"
                + "    .child(textarea(\"Observacoes\").rows(3))\n"
                + "    .child(button(\"Enviar\").type(\"submit\"));\n";
    }

    private String cssCode() {
        return ""
                + "div().css(\"flex flex-col gap-4 p-6 rounded-lg border shadow-sm\")\n"
                + "h2(\"Titulo\").css(\"text-2xl font-bold text-primary-700\")\n"
                + "p(\"Texto\").css(\"text-sm leading-relaxed text-slate-700\")\n"
                + "button(\"Acao\").css(\"px-4 py-2 rounded bg-secondary-700 text-white\")\n\n"
                + "// O renderer gera uma escala gradual a partir das cores base:\n"
                + "bg-primary-50, bg-primary-500, bg-primary-900\n"
                + "text-secondary-700, border-secondary-200\n";
    }

    private String securityCode() {
        return ""
                + "a(\"Seguro\").href(\"https://example.com\")\n"
                + "img().src(\"data:image/png;base64,...\").alt(\"Preview\")\n"
                + "div().title(\"Titulo\").ariaLabel(\"Regiao\").data(\"test-id\", \"hero\")\n\n"
                + "// Rejeitado antes de renderizar:\n"
                + "a(\"XSS\").href(\"javascript:alert(1)\")\n\n"
                + "// O servidor tambem emite headers como CSP, nosniff,\n"
                + "// Referrer-Policy, Permissions-Policy e frame-ancestors.\n";
    }

    private String restCode() {
        return ""
                + "RestClient client = RestClient.create();\n"
                + "RestResponse response = client\n"
                + "    .get(\"https://brasilapi.com.br/api/banks/v1\")\n"
                + "    .requireSuccessful();\n\n"
                + "List<Bank> banks = parseBanks(response.body());\n\n"
                + "select().children(banks.stream()\n"
                + "    .map(bank -> option(bank.label()).value(bank.selectValue()))\n"
                + "    .collect(Collectors.toList()));\n";
    }

    private String cliCommandCode() {
        return ""
                + "ujfe convert page.html \\\n"
                + "  --out src/main/java/app/pages/Page.java \\\n"
                + "  --type html\n";
    }

    private String cliInputCode() {
        return ""
                + "<section class=\"p-4 flex flex-col gap-2\">\n"
                + "  <h1 title=\"Hero\">Hello UJFE</h1>\n"
                + "  <p>HTML convertido para Java DSL.</p>\n"
                + "  <button>Salvar</button>\n"
                + "</section>\n";
    }

    private String cliOutputCode() {
        return ""
                + "package app.pages;\n\n"
                + "import static ujfe.html.UI.*;\n\n"
                + "import ujfe.html.Node;\n"
                + "import ujfe.router.Page;\n\n"
                + "@Page(\"/\")\n"
                + "public final class Page {\n\n"
                + "    public Node render() {\n"
                + "        return section()\n"
                + "                .css(\"p-4 flex flex-col gap-2\")\n"
                + "                .child(h1().title(\"Hero\").child(text(\"Hello UJFE\")))\n"
                + "                .child(p().child(text(\"HTML convertido para Java DSL.\")))\n"
                + "                .child(button().child(text(\"Salvar\")));\n"
                + "    }\n"
                + "}\n";
    }

    private String devPreviewCode() {
        return ""
                + "LiveSession liveSession = new LiveSession(router, appTheme::cssTheme, true);\n\n"
                + "// Quando ativo, o HTML inclui:\n"
                + "<script src=\"/_ujfe/dev.js\"></script>\n\n"
                + "// O painel permite clicar em elementos e testar classes CSS\n"
                + "// no browser, sem escrever JavaScript manual e sem Node/npm.\n";
    }
}
