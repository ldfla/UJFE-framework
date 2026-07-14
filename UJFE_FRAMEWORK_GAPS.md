# Prompt: melhorias desejadas no UJFE Framework

Use este prompt com uma LLM quando for evoluir o UJFE Framework a partir das
necessidades observadas no Voicebot.

```text
Voce e uma LLM atuando como engenheiro senior de framework frontend Java.
Analise o UJFE Framework e proponha melhorias concretas para reduzir codigo
manual nas telas administrativas do Voicebot, mantendo o framework generico e
reutilizavel para outros produtos.

Contexto do produto consumidor:
- O Voicebot e uma aplicacao Spring Boot modular com frontend server-driven em
  UJFE.
- As telas atuais e planejadas incluem login com passkey/WebAuthn, dashboard,
  usuarios, fluxos IVR configuraveis, versoes de fluxo, deploy/rollback de
  configuracoes Asterisk, status em tempo real, alertas, providers, chamadas,
  sessoes e player de gravacoes.
- O Asterisk deve ficar como gateway SIP/media com configuracao base estavel;
  o fluxo da chamada, IVR, decisoes de roteamento, observabilidade e operacoes
  administrativas devem ficar principalmente do lado da aplicacao via ARI.

Objetivo:
Criar uma proposta de evolucao do UJFE Framework com APIs, componentes e
padroes que permitam implementar as telas acima com menos codigo repetitivo,
menos JavaScript manual, melhor acessibilidade, melhor seguranca e melhor
suporte a fluxos administrativos criticos.

Direcao de produto para o UJFE:
- O UJFE deve ser Java-first: o desenvolvedor deve conseguir definir estrutura
  HTML, estilos, comportamento cliente, validacao, eventos e chamadas ao
  servidor a partir de um unico ponto de programacao em Java.
- Busque o melhor de React, Vue, TypeScript e Angular, mas sem copiar seus
  custos acidentais. O objetivo e ter composicao declarativa, reatividade,
  componentes reutilizaveis, contracts tipados, roteamento, formularios e
  tooling, mantendo simplicidade e seguranca de aplicacoes Java server-side.
- De React, aproveite componentizacao, composicao, estado previsivel,
  error boundaries, suspense/loading boundaries e atualizacao incremental.
- De Vue, aproveite a ideia de componente coeso com template, estilo e
  comportamento proximos, reatividade simples, transicoes e scoped styles.
- De TypeScript, aproveite contratos estaticos, tipos discriminados,
  inferencia, autocomplete, checagem em build e geracao de clientes/rotas
  tipadas. Em Java, isso deve aparecer como records, sealed interfaces,
  generics, annotation processors e APIs fluentes seguras.
- De Angular, aproveite formularios robustos, router estruturado, guards,
  dependency injection, convencoes de modulo, testabilidade e separacao clara
  entre infraestrutura e logica de produto.
- Use as vantagens do Java moderno: records para props/state/view models,
  sealed interfaces para estados e eventos, pattern matching para renderizacao
  por variante, virtual threads para actions e data loaders bloqueantes
  simples, structured concurrency para carregar dados de pagina em paralelo,
  Bean Validation para formularios e o ecossistema Spring para seguranca,
  observabilidade e configuracao.
- Centralize HTML, CSS e JavaScript sem violar CSP. A API deve permitir que um
  componente Java declare markup, tokens/classes de estilo e comportamento
  cliente tipado; o framework deve gerar/registrar assets externos quando
  JavaScript for necessario, evitando scripts inline.
- Planeje suporte futuro a paginas estaticas e cache: renderizacao estatica,
  snapshots HTML, shells cacheaveis, ETag/Cache-Control, stale-while-revalidate,
  revalidacao por evento, service worker opcional e "islands" interativas.
  Isso deve servir para login, paginas publicas, shells administrativas,
  documentacao, paginas de erro e telas com dados pouco sensiveis, mantendo
  dados protegidos e realtime como conteudo dinamico.

Regras importantes:
- Nao implemente logica especifica do Voicebot dentro do framework.
- Prefira primitivas pequenas, tipadas e composiveis.
- Todo recurso que executar JavaScript no navegador deve ser compatível com CSP
  restritiva, sem depender de scripts inline.
- Todo componente interativo deve ter estados de loading, erro, vazio,
  permissao negada e comportamento de teclado quando aplicavel.
- As APIs devem se integrar bem com Java/Spring server-side rendering e com os
  sinais/reatividade ja existentes no UJFE.
- Preserve compatibilidade incremental: o consumidor deve poder adotar cada
  melhoria sem reescrever toda a UI.

Lacunas observadas e aplicacao esperada:

1. Browser API bridge para WebAuthn/passkeys
   Problema: a tela de login precisou de JavaScript externo manual para lidar
   com challenge, publicKeyCredential, serializacao e erros do navegador.
   Melhorar no UJFE: oferecer helper CSP-safe para carregar modulos cliente,
   chamar browser APIs, converter ArrayBuffer/base64url e reportar erros.
   Aplicacao no Voicebot: login, cadastro de passkey, recuperacao de conta e
   telas futuras de seguranca sem scripts soltos em cada pagina.

2. Server actions e formularios administrativos
   Problema: botoes e formularios precisam repetir CSRF, estado disabled,
   mensagens de sucesso/erro, confirmacao e refresh apos acao.
   Melhorar no UJFE: criar action button/form helpers com confirmacao opcional,
   loading state, resultado tipado, toast/status banner e protecao contra duplo
   submit.
   Aplicacao no Voicebot: bootstrap, criar usuario, publicar fluxo, deploy,
   rollback, desconectar sessao, revogar passkey e operacoes destrutivas.

3. Data grid/lista administrativa densa
   Problema: listas como usuarios, fluxos, sessoes, chamadas e gravacoes tendem
   a virar paineis repetidos e pouco escaneaveis.
   Melhorar no UJFE: data grid server-driven com ordenacao, filtros, paginacao,
   selecao, colunas responsivas, empty state, row actions e badges.
   Aplicacao no Voicebot: `/users`, `/flows`, chamadas ativas, historico de
   gravacoes, providers e auditoria.

4. Realtime subscriptions
   Problema: dashboards e status de chamadas precisam receber eventos sem
   polling manual por pagina.
   Melhorar no UJFE: helper SSE/WebSocket integrado a signals, com reconexao,
   heartbeat, debounce, estado de conexao e fallback de polling.
   Aplicacao no Voicebot: dashboard operacional, chamadas ativas, ARI status,
   conectividade Asterisk, alertas e progresso de deploy.

5. Dashboard e metric primitives
   Problema: metricas e alertas exigem padroes visuais consistentes.
   Melhorar no UJFE: metric tile, compact status indicator, threshold coloring,
   sparkline/chart wrapper, alert list e painel de saude.
   Aplicacao no Voicebot: primeira tela pos-login com status de chamadas,
   Asterisk, WebSocket media, gravacoes, erros recentes e filas.

6. Media playback para gravacoes
   Problema: gravacoes precisam de player seguro com metadados e estados.
   Melhorar no UJFE: componente de audio com preload controlado, timeline,
   slots para metadados, erro/loading, controle de download por permissao e
   opcao futura de waveform.
   Aplicacao no Voicebot: player das gravacoes persistidas pelo pipeline de
   audio e gravacoes do Asterisk.

7. Graph builder para fluxos IVR
   Problema: `/flows/new` com formulario inicial nao e suficiente para editar
   graficamente nos, arestas, ordem e validacoes de fluxo.
   Melhorar no UJFE: canvas/graph builder com nos, conectores, pan/zoom,
   selecao, teclado, edge ordering, minimap opcional e eventos server-driven.
   Aplicacao no Voicebot: editor visual de IVR, versionamento de fluxo,
   comparacao entre versoes e validacao antes de publish/deploy.

8. Structured JSON/object editor
   Problema: configuracoes de no, aresta e Asterisk nao deveriam ser editadas
   como textarea crua.
   Melhorar no UJFE: editor de objeto tipado por schema simples, campos
   aninhados, preview JSON, mensagens de validacao e defaults.
   Aplicacao no Voicebot: `flow_node.configuration_json`,
   `flow_edge.configuration_json` e `configuration_version.configuration_json`.

9. Route helpers tipados
   Problema: rotas stringly typed como `/users`, `/flows/{id}` e
   `/flows/{id}/versions/{version}` espalham erro de digitacao e redirect ruim.
   Melhorar no UJFE: route builder com parametros obrigatorios, query params,
   continue URL seguro e link helpers.
   Aplicacao no Voicebot: dashboard, login redirect, detalhes de fluxo,
   versoes, rollback, usuarios e gravacoes.

10. Workflow state components
    Problema: publish, deploy, rollback e estados falhos precisam padronizar
    risco, permissao e auditabilidade.
    Melhorar no UJFE: componentes para timeline de workflow, banners de estado,
    confirm dialog, diff summary e action toolbar com permissao.
    Aplicacao no Voicebot: `FLOW_DEPLOY`, `ASTERISK_DEPLOY`, rollback de
    configuracao, falha de deploy e links para auditoria.

11. Form fields e validacao
    Problema: labels, hints, autocomplete, erro servidor e constraints tendem a
    repetir markup por tela.
    Melhorar no UJFE: campos padronizados para text, select, checkbox, switch,
    number, date/time, secret, JSON/object e file/audio, todos com binding de
    erro servidor.
    Aplicacao no Voicebot: criacao de usuarios, filtros, configuracao de
    providers, nodes do IVR e parametros de Asterisk.

12. Design tokens e primitives de layout administrativo
    Problema: paginas repetem classes e decisoes visuais de botoes, badges,
    toolbars, paineis e areas de detalhe.
    Melhorar no UJFE: tokens de espacamento/tipografia/cores, toolbar,
    section header, split view, tabs, segmented control, badge, toast, drawer e
    modal com acessibilidade.
    Aplicacao no Voicebot: consistencia entre dashboard, usuarios, fluxos,
    providers, status e gravacoes.

13. Component model Java-first com HTML, CSS e JS co-localizados
    Problema: quando markup, classes CSS, assets JS e handlers ficam espalhados,
    a tela fica dificil de revisar e facil de quebrar por CSP, MIME type,
    redirect ou permissao.
    Melhorar no UJFE: criar uma unidade de componente Java que declare props,
    estado, render tree, estilo escopado/tokens, handlers server-side e
    comportamento browser-side tipado por modulo externo gerado pelo framework.
    Aplicacao no Voicebot: `LoginPage`, `DashboardPage`, `UsersPage`,
    `FlowsPage`, player de gravacoes e editor de IVR podem virar componentes
    coesos, testaveis e reutilizaveis.

14. Contratos tipados e schema-driven UI
    Problema: TypeScript entrega seguranca de contrato no frontend; no UJFE,
    isso precisa existir em Java para rotas, eventos, payloads e validacoes.
    Melhorar no UJFE: records para props/payloads, sealed interfaces para
    eventos/estados, geracao de schemas, validators integrados, converters e
    compile-time checks para action payloads e realtime events.
    Aplicacao no Voicebot: eventos de chamada, status Asterisk, flow graph,
    deploy result, filtros de gravacao e respostas de formulario ficam tipados
    ponta a ponta.

15. Render modes, paginas estaticas e cache simulado
    Problema: nem toda tela precisa ser 100% dinamica; algumas podem ser
    renderizadas como shell estatico ou snapshot cacheavel, reduzindo latencia e
    custo sem sacrificar seguranca.
    Melhorar no UJFE: route-level render modes como `dynamic`, `static`,
    `cached`, `staticShell`, `staleWhileRevalidate` e `revalidateOn(event)`;
    suporte a ETag, Cache-Control, pre-render em build/runtime, invalidacao e
    ilhas interativas.
    Aplicacao no Voicebot: login, paginas de erro, shell do dashboard,
    documentacao operacional, listas com cache curto e telas de configuracao
    podem carregar rapido enquanto widgets realtime atualizam depois.

16. Data loaders e actions com virtual threads
    Problema: frameworks frontend modernos escondem complexidade de loading e
    concorrencia; no Java moderno podemos manter codigo bloqueante simples com
    boa escala usando virtual threads.
    Melhorar no UJFE: data loader por rota/componente, execucao paralela com
    structured concurrency, timeout, fallback, cancellation, loading boundary e
    error boundary.
    Aplicacao no Voicebot: dashboard pode carregar ARI status, chamadas,
    alertas, providers e metricas em paralelo; falha parcial nao deve derrubar
    a pagina inteira.

17. Tooling, preview e testes de componentes
    Problema: React/Vue/Angular tem ecossistemas fortes de DX; o UJFE precisa
    de feedback rapido sem abandonar Java.
    Melhorar no UJFE: preview/storybook-like para componentes Java, snapshot
    tests de HTML, testes de acessibilidade, validacao de CSP, manifest de
    assets, diagnostico de rotas, hot reload/dev mode e tooling para localizar
    handlers/actions usados por uma tela.
    Aplicacao no Voicebot: validar login/passkey, dashboard, flows, modais,
    player e estados de erro sem depender sempre de teste manual no browser.

Formato esperado da resposta:
1. Diagnostico curto das lacunas do UJFE que mais impactam o Voicebot.
2. Proposta de APIs Java/UJFE para cada melhoria prioritaria, com exemplos de
   uso no consumidor.
3. Plano incremental em etapas pequenas, com ordem recomendada e riscos.
4. Criterios de aceite e testes de framework para cada etapa.
5. Guia de migracao mostrando como substituir codigo atual do Voicebot por
   cada nova primitiva.
6. Nao-objetivos: deixe claro o que deve continuar no Voicebot e nao no UJFE.
7. Mapa de inspiracao: para cada proposta, indique qual parte foi inspirada em
   React, Vue, TypeScript ou Angular e como ela foi traduzida para Java.
8. Proposta de render modes/cache, com regras de seguranca para nao cachear
   dados privados indevidamente.

Exemplos de API desejada, apenas como direcao:

- `Ui.actionButton(label, action).confirm(message).permission(authority)`
- `Ui.dataGrid(rows).column("Status", row -> badge(row.status()))`
- `Ui.route("/flows/{id}").with("id", flowId).query("tab", "versions")`
- `Ui.realtime("/events/dashboard").onEvent(DashboardEvent.class, signal::set)`
- `Ui.graphBuilder(flowGraph).onNodeSelected(...).onEdgeCreated(...)`
- `Ui.objectEditor(schema, value).onValidate(...).jsonPreview(true)`
- `Ui.audioPlayer(recordingUrl).metadata(metadata).downloadAllowed(canDownload)`
- `Ui.component(LoginCard.class).props(new LoginProps(continueUrl))`
- `Ui.scopedStyle(componentId).token("gap", "space.3").className("login-card")`
- `Ui.clientModule("passkey-login").action("authenticate", PasskeyRequest.class)`
- `Ui.route("/dashboard").renderMode(RenderMode.staticShell(Duration.ofSeconds(30)))`
- `Ui.loader(DashboardData.class).parallel(ariStatus, calls, alerts).timeout(...)`

Priorize implementacoes que removam JavaScript manual, evitem falhas de
seguranca/CSP, melhorem acessibilidade e reduzam repeticao nas telas
administrativas.
```

## Observacoes para aplicacao no Voicebot

- M1/M5 usam primeiro: WebAuthn helper, route helpers, action/form helpers,
  dashboard primitives e data grid.
- M6 usa primeiro: graph builder, object editor, workflow state components e
  realtime status para deploy/configuracao.
- A direcao de medio prazo e que cada tela relevante tenha um componente Java
  coeso, com markup, estilo, comportamento e contratos tipados no mesmo ponto
  de manutencao.
- Paginas estaticas/cacheaveis devem ser tratadas como otimizacao controlada:
  shells e conteudo publico podem ser cacheados; dados sensiveis, permissoes,
  chamadas em tempo real e gravacoes exigem autorizacao e invalidacao correta.
- O framework deve habilitar UX mais rica, mas a politica de negocio continua
  no Voicebot: permissoes, auditoria, validacao de fluxo, ARI e persistencia.
