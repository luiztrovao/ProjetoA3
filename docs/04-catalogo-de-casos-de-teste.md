# 4. Catálogo de casos de teste

> Seção 11 do enunciado. 60 casos, distribuídos entre os níveis (unitário, integração, sistema e aceitação) e as
> técnicas (valor-limite, partição, tabela de decisão, caixa-branca e casos inválidos). Cada caso está ligado a um
> requisito, a um critério de aceitação ([01-requisitos.md](01-requisitos.md)) e ao teste automatizado que o executa.

## Como ler o catálogo

- **Passos.** Todo teste automatizado segue três passos: **preparar** os dados e as pré-condições, **executar** a
  ação e **verificar** o resultado. Os passos completos de cada caso estão no próprio método de teste, citado na
  coluna Evidência. Dois casos têm o roteiro detalhado ao final, como exemplo.
- **Resultado obtido e Status** vêm da execução real do CI de 04/11/2026 (execução 41921004487), salva em
  [/reports](../reports/README.md).
- **Evidência.** O nome do teste; o resultado de cada um está no relatório indicado:
  - backend: `reports/backend/testes-h2/TEST-<classe>.xml` (os mesmos testes rodam também em `testes-postgresql`);
  - frontend: o log do Vitest no CI;
  - sistema: `reports/frontend/testes-sistema/resultados.xml`.
- **Pré-condições comuns.**
  - **P1:** API no perfil de teste, com banco vazio criado pelas migrações e usuários criados pelo próprio teste.
  - **P2:** componente renderizado com a API simulada.
  - **P3:** sistema completo no ar (API + frontend compilado), com os dados de demonstração.

## 4.1 Login e permissões (RF01, RF02, RNF01)

| ID | Req. | CA | Nível / técnica | Objetivo | Pré-cond. | Dados de entrada | Resultado esperado | Resultado obtido | Status | Evidência |
|---|---|---|---|---|---|---|---|---|---|---|
| CT001 | RF01 | CA-01 | Integração / requisito | Login por nome de usuário | P1, usuário ativo | usuário + senha corretos | 200, token e dados do usuário | Conforme esperado | Passou | `AuthControllerTest.loginPorNomeDeUsuarioDevolveTokenEUsuario` |
| CT002 | RF01 | CA-01 | Integração / partição | Login por e-mail | P1 | e-mail + senha | 200 e token | Conforme esperado | Passou | `AuthControllerTest.loginPorEmail` |
| CT003 | RF01 | CA-02 | Integração / inválido | Senha errada | P1 | senha incorreta | 401, mensagem genérica | Conforme esperado | Passou | `AuthControllerTest.senhaErradaDevolve401ComMensagemGenerica` |
| CT004 | RF01 | CA-02 | Integração / inválido | Usuário inexistente | P1 | login desconhecido | 401, a mesma mensagem | Conforme esperado | Passou | `AuthControllerTest.usuarioInexistenteDevolveAMesmaMensagem` |
| CT005 | RF01 | CA-03 | Integração / partição | Usuário inativo | P1, usuário inativo | senha correta | Login recusado | Conforme esperado | Passou | `AuthControllerTest.usuarioInativoNaoEntra` |
| CT006 | RF01 | — | Integração / exceção | Sem token ou token inválido | P1 | requisição sem `Authorization` / token adulterado | 401 | Conforme esperado | Passou | `semTokenDevolve401`, `tokenInvalidoDevolve401` |
| CT007 | RF01 | CA-03 | Integração / risco | Usuário inativado depois do login | P1, token emitido | inativar e reusar o token | Perde o acesso | Conforme esperado | Passou | `tokenDeUsuarioInativadoDepoisDoLoginPerdeOAcesso` |
| CT008 | RF02 | CA-04 | Integração / risco | Vendedor edita ou desativa cliente | P1, Vendedor | PUT/DELETE em cliente | 403 | Conforme esperado | Passou | `PermissoesTest.vendedorNaoEditaNemDesativaClientes` |
| CT009 | RF02 | CA-04 | Integração / risco | Vendedor gerencia produto | P1, Vendedor | POST/PUT em produto | 403 (consulta permitida) | Conforme esperado | Passou | `PermissoesTest.vendedorConsultaProdutosMasNaoGerencia` |
| CT010 | RF02 | CA-04 | Integração / risco | Gerente gerencia funcionários ou configurações | P1, Gerente | POST usuário, PUT configuração | 403 | Conforme esperado | Passou | `PermissoesTest.gerenteNaoGerenciaFuncionariosNemConfiguracoes` |
| CT011 | RF02 | CA-05 | Integração / risco | Perfil alterado com token já emitido | P1 | rebaixar e reusar o token | Nova permissão vale na hora | Conforme esperado | Passou | `PermissoesTest.perfilAlteradoValeNaHoraParaOTokenJaEmitido` |
| CT012 | RNF01 | — | Unitário / valor-limite | Segredo do JWT com 31 e 32 caracteres | — | 31 e 32 caracteres | 31 recusado, 32 aceito | Conforme esperado | Passou | `JwtPropertiesTest.segredoDe31CaracteresEhRecusado`, `segredoDe32CaracteresEhAceito` |

## 4.2 Clientes, produtos e funcionários (RF03, RF04, RF05, RF14)

| ID | Req. | CA | Nível / técnica | Objetivo | Pré-cond. | Dados de entrada | Resultado esperado | Resultado obtido | Status | Evidência |
|---|---|---|---|---|---|---|---|---|---|---|
| CT013 | RF03 | CA-08 | Integração / partição | CPF e telefone com máscara | P1 | `123.456.789-09`, `(81) 99999-8888` | Guarda só dígitos | Conforme esperado | Passou | `ClienteControllerTest.criaClienteNormalizandoCpfETelefone` |
| CT014 | RF03 | CA-06 | Integração / partição | Cliente sem CPF | P1 | só o nome | Cadastro aceito | Conforme esperado | Passou | `ClienteControllerTest.criaClienteSemCpf` |
| CT015 | RF03 | CA-07 | Integração / inválido | CPF inválido | P1 | dígitos verificadores errados | 400 no campo `cpf` | Conforme esperado | Passou | `ClienteControllerTest.recusaCpfInvalido` |
| CT016 | RF03 | CA-07 | Integração / inválido | CPF duplicado | P1, cliente existente | mesmo CPF | 422 | Conforme esperado | Passou | `ClienteControllerTest.recusaCpfDuplicado` |
| CT017 | RF03 | — | Integração / inválido | CEP e UF inválidos | P1 | CEP com 7 dígitos, UF com 3 letras | 400 | Conforme esperado | Passou | `ClienteControllerTest.recusaCepEUfInvalidos` |
| CT018 | RF04 | — | Integração / inválido | Produto sem campos obrigatórios | P1 | corpo vazio | 400 listando os campos | Conforme esperado | Passou | `ProdutoControllerTest.recusaProdutoInvalidoInformandoOsCampos` |
| CT019 | RF04 | CA-09 | Integração / inválido | Código de produto duplicado | P1, produto existente | mesmo código | 422 | Conforme esperado | Passou | `ProdutoControllerTest.recusaCodigoDuplicado` |
| CT020 | RF04 | CA-10 | Unitário / valor-limite | Estoque igual ao mínimo | — | estoque = mínimo | Marcado como estoque baixo | Conforme esperado | Passou | `ProdutoTest.estoqueBaixoQuandoIgualOuAbaixoDoMinimo` |
| CT021 | RF04 | CA-09 | Unitário / valor-limite | Preço 0,00, 0,01, 0,02 e 0,001 | — | os 4 valores | 0,00 e 0,001 recusados | Conforme esperado | Passou | `LimitesValidacaoTest.precoMaiorQueZeroComDuasCasas` |
| CT022 | RF05 | CA-11 | Integração / risco | Inativar o único Dono | P1, só um Dono ativo | `DELETE` no único Dono | 422 | Conforme esperado | Passou | `UsuarioControllerTest.naoPermiteInativarOUnicoDono` |
| CT023 | RF05 | CA-12 | Unitário / risco | Alterar o próprio perfil | usuário logado | mudar o próprio perfil | Recusado | Conforme esperado | Passou | `UsuarioServiceTest.naoPermiteAlterarOProprioPerfil` |
| CT024 | RF05 | — | Integração / requisito | Criar funcionário | P1, Dono | dados completos | Senha nunca aparece na resposta | Conforme esperado | Passou | `UsuarioControllerTest.criaUsuarioCompletoSemExporASenha` |
| CT025 | RF05 | — | Unitário / valor-limite | Senha com 5, 6, 7, 71, 72 e 73 caracteres | — | os 6 tamanhos | Só 5 e 73 recusados | Conforme esperado | Passou | `LimitesValidacaoTest.senhaDe6A72Caracteres` |
| CT026 | RF05 | — | Unitário / inválido | Editar funcionário com CPF de outro | dois funcionários | CPF do outro | Recusado, nada salvo | Conforme esperado | Passou | `UsuarioServiceTest.atualizarComCpfDeOutroUsuarioLancaExcecao` |
| CT027 | RF14 | — | Unitário / inválido | Minha Conta com e-mail de outro funcionário | dois funcionários | e-mail do outro | Recusado, nada salvo | Conforme esperado | Passou | `AuthServiceTest.atualizarContaComEmailDeOutroFuncionarioLancaExcecao` |

## 4.3 Vendas, estoque e pedidos (RF06, RF07, RF08)

| ID | Req. | CA | Nível / técnica | Objetivo | Pré-cond. | Dados de entrada | Resultado esperado | Resultado obtido | Status | Evidência |
|---|---|---|---|---|---|---|---|---|---|---|
| CT028 | RF06 | CA-13 | Unitário / requisito | Subtotal, desconto e total | venda em montagem | itens + desconto | Total = subtotal − desconto | Conforme esperado | Passou | `VendaTest.calculaSubtotalDescontoETotal` |
| CT029 | RF06 | CA-13 | Unitário / valor-limite | Desconto negativo ou maior que o subtotal | venda em montagem | −0,01 e subtotal + 0,01 | Recusado | Conforme esperado | Passou | `VendaTest.descontoNaoPodePassarDoSubtotalNemSerNegativo` |
| CT030 | RF06 | CA-14 | Integração / valor-limite | Dinheiro com valor recebido menor que o total | P1 | total − 0,01 | 422 | Conforme esperado | Passou | `VendaControllerTest.recusaValorRecebidoMenorQueOTotal` |
| CT031 | RF06 | CA-15 | Integração / risco | Vendedor dá desconto | P1, Vendedor e Gerente | desconto > 0 | Vendedor recusado; Gerente pode | Conforme esperado | Passou | `VendaControllerTest.vendedorNaoDaDescontoMasGerenteSim` |
| CT032 | RF06 | — | Integração / inválido | Pedido sem itens | P1 | lista vazia | Recusado | Conforme esperado | Passou | `VendaControllerTest.recusaPedidoSemItens` |
| CT033 | RF06 | — | Unitário / valor-limite | Quantidade 0, 1, 2, 998, 999 e 1000 | — | as 6 quantidades | Só 0 e 1000 recusados | Conforme esperado | Passou | `LimitesValidacaoTest.quantidadeDoItemDe1A999` |
| CT034 | RF07 | CA-16 | Unitário / tabela de decisão (R1) | Venda sem controle de estoque | controle desligado | qualquer quantidade | Vende, estoque intacto | Conforme esperado | Passou | `VendaTest.semControleDeEstoqueNaoMexeNoEstoque` |
| CT035 | RF07 | CA-16 | Unitário / tabela de decisão (R2) | Venda com estoque suficiente | controle e bloqueio ligados | quantidade ≤ estoque | Vende e baixa | Conforme esperado | Passou | `VendaTest.confirmarAplicaStatusInicialEBaixaEstoque` |
| CT036 | RF07 | CA-16 | Integração / tabela de decisão (R3) | Venda acima do estoque com bloqueio | P1, bloqueio ligado | quantidade > estoque | 422 | Conforme esperado | Passou | `VendaControllerTest.recusaEstoqueInsuficiente` |
| CT037 | RF07 | CA-16 | Unitário / tabela de decisão (R4) | Venda acima do estoque sem bloqueio | bloqueio desligado | quantidade > estoque | Baixa só o disponível, nunca negativo | Conforme esperado | Passou | `VendaTest.semBloqueioVendeEBaixaSoOQueHaNoEstoque` |
| CT038 | RF08 | CA-17 | Unitário / inválido | Cancelar sem motivo | pedido em preparo | motivo vazio | Recusado | Conforme esperado | Passou | `VendaTest.cancelarExigeMotivo` |
| CT039 | RF08 | CA-17 | Unitário / risco | Cancelar venda com baixa parcial | venda com baixa parcial | cancelar | Devolve só o que saiu do estoque | Conforme esperado | Passou | `VendaTest.cancelarDevolveSoOQueFoiRetiradoDoEstoque` |
| CT040 | RF08 | CA-19 | Integração / caixa-branca | Busca por nome sem diferenciar maiúsculas | P1, 3 pedidos | `CAMILA`, `bruno` | Só o pedido de cada um | Conforme esperado | Passou | `VendaFiltrosTest.buscaPeloNomeDoClienteOuDoVendedorSemDiferenciarMaiusculas` |
| CT041 | RF08 | CA-19 | Integração / partição | Busca pelo número com ou sem máscara | P1, 3 pedidos | `#PV-0000NN` e `NN` | O pedido certo | Conforme esperado | Passou | `VendaFiltrosTest.buscaPeloNumeroDoPedidoComOuSemMascara` |
| CT042 | RF08 | — | Integração / valor-limite | Número com mais de 18 dígitos | P1 | 19 dígitos | Busca só por nome, sem erro | Conforme esperado | Passou | `VendaFiltrosTest.numeroComMaisDe18DigitosBuscaSoPorNome` |
| CT043 | RF08 | CA-19 | Integração / valor-limite | Período inclui o dia inteiro | P1, pedidos nos dias 10, 12 e 14 | 12 a 12; 11 a 13 | Só o pedido do dia 12 | Conforme esperado | Passou | `VendaFiltrosTest.periodoIncluiODiaInteiroDoInicioEDoFim` |
| CT044 | RF08 | — | Integração / caixa-branca | Período só com início ou só com fim | P1 | início 12; fim 12 | 12 e 14; 10 e 12 | Conforme esperado | Passou | `VendaFiltrosTest.periodoSoComInicioOuSoComFim` |

## 4.4 Relatórios, notificações, configurações e imagens (RF10, RF11, RF12, RF13)

| ID | Req. | CA | Nível / técnica | Objetivo | Pré-cond. | Dados de entrada | Resultado esperado | Resultado obtido | Status | Evidência |
|---|---|---|---|---|---|---|---|---|---|---|
| CT045 | RF10 | CA-20 | Integração / requisito | Picos do ano | P1, vendas no ano | `periodo=ANO` | Série agrupada por mês | Conforme esperado | Passou | `RelatorioControllerTest.picosDoAnoSaoPorMes` |
| CT046 | RF11 | CA-21 | Integração / requisito | Gerente vê estoque baixo e pedido atrasado | P1, produto abaixo do mínimo, pedido há 40 min | `GET /api/notificacoes` | 2 avisos não lidos | Conforme esperado | Passou | `NotificacaoControllerTest.gerenteVeEstoqueBaixoEPedidoAtrasado` |
| CT047 | RF11 | CA-21 | Integração / risco | Vendedor vê só os próprios avisos | P1, pedidos de dois vendedores | `GET /api/notificacoes` | Só os próprios atrasados e cancelados | Conforme esperado | Passou | `NotificacaoControllerTest.vendedorVeSoOsProprioPedidosAtrasadosECancelados` |
| CT048 | RF12 | — | Unitário / valor-limite | Alerta de atraso de 4, 5, 6, 239, 240 e 241 min | — | os 6 valores | Só 4 e 241 recusados | Conforme esperado | Passou | `LimitesValidacaoTest.alertaDePedidoAtrasadoDe5A240Minutos` |
| CT049 | RF13 | CA-22 | Unitário / partição | Arquivo `.png` que não é imagem | — | texto renomeado para `.png` | Recusado | Conforme esperado | Passou | `ImagemServiceTest.recusaOutrosFormatosMesmoComExtensaoDeImagem` |
| CT050 | RNF01 | — | Unitário / caixa-branca | Imagem guardada sem exposição de estado | — | alterar o vetor enviado e o devolvido | A imagem guardada não muda | Conforme esperado | Passou | `CopiasDefensivasTest.imagemGuardaEDevolveCopiaDoArquivo` |

## 4.5 Frontend: unitário e componente (RF01, RF03, RF11)

| ID | Req. | CA | Nível / técnica | Objetivo | Pré-cond. | Dados de entrada | Resultado esperado | Resultado obtido | Status | Evidência |
|---|---|---|---|---|---|---|---|---|---|---|
| CT051 | RF03 | CA-08 | Unitário / valor-limite | Máscara de CPF com 10, 11 e 12 dígitos | — | os 3 tamanhos | Formata 11; descarta o excedente | Conforme esperado | Passou | `tests/unit/format.test.ts` › `maskCpf` |
| CT052 | RF01 | CA-01 | Componente / inválido | Login com campos vazios | P2 | clicar em Entrar | Mensagem e nenhuma chamada à API | Conforme esperado | Passou | `LoginPage.test.tsx` › não chama a API com campos vazios |
| CT053 | RF01 | CA-02 | Componente / inválido | Senha errada na tela | P2, API responde 401 | usuário + senha errada | Mostra a mensagem da API | Conforme esperado | Passou | `LoginPage.test.tsx` › mostra a mensagem da API quando a senha está errada |
| CT054 | RF01 | CA-03 | Unitário / exceção | Sessão expirada | token salvo, API responde 401 | qualquer chamada | Apaga o token e volta ao login | Conforme esperado | Passou | `client.test.ts` › 401 apaga o token e avisa a tela de login |
| CT055 | RF11 | — | Componente / requisito | Marcar todas as notificações como lidas | P2, 1 aviso não lido | clicar em "Marcar todas como lidas" | Contador zerado | Conforme esperado | Passou | `HeaderMenus.test.tsx` › 'Marcar todas como lidas' zera o contador |
| CT056 | RF02 | CA-04 | Componente / partição | Menu da conta por perfil | P2 | Dono e não Dono | "Dados da lanchonete" só para o Dono | Conforme esperado | Passou | `HeaderMenus.test.tsx` › AccountMenu |

## 4.6 Sistema e aceitação (Playwright, sistema completo)

| ID | Req. | CA | Nível / técnica | Objetivo | Pré-cond. | Dados de entrada | Resultado esperado | Resultado obtido | Status | Evidência |
|---|---|---|---|---|---|---|---|---|---|---|
| CT057 | RF01 | CA-02 | Sistema / inválido | Senha errada no navegador | P3 | `fernanda` + senha errada | Alerta e continua no login | Conforme esperado | Passou | `acesso.spec.ts` › senha errada mostra a mensagem e não entra |
| CT058 | RF02 | CA-04 | Aceitação / risco | Vendedor sem Dashboard nem Relatórios | P3 | login `fernanda` | Abre em Vendas; menus de gestão ausentes | Conforme esperado | Passou | `acesso.spec.ts` › Vendedor entra direto em Vendas… |
| CT059 | RF06, RF08 | CA-13, CA-19 | Aceitação / risco | Venda completa no PIX e busca do pedido | P3 | 2 unidades de um produto, PIX | Venda confirmada e pedido encontrado em Pedidos | Conforme esperado | Passou | `venda.spec.ts` › Vendedor registra uma venda no PIX… |
| CT060 | RF11 | CA-10, CA-21 | Sistema / requisito | Aviso de estoque baixo leva aos produtos filtrados | P3 | login `carlos`, clicar no aviso | Cadastros › Produtos com "Estoque baixo" | Conforme esperado | Passou | `gestao.spec.ts` › Gerente abre o aviso de estoque baixo… |

## 4.7 Resumo

| Categoria | Casos | Passou | Falhou |
|---|---|---|---|
| Login e permissões | 12 | 12 | 0 |
| Cadastros | 15 | 15 | 0 |
| Vendas, estoque e pedidos | 17 | 17 | 0 |
| Relatórios, notificações, configurações e imagens | 6 | 6 | 0 |
| Frontend (unitário e componente) | 6 | 6 | 0 |
| Sistema e aceitação | 4 | 4 | 0 |
| **Total** | **60** | **60** | **0** |

Os 60 casos são uma seleção representativa: a suíte automatizada completa tem 253 testes no backend, 63 no
frontend e 6 de sistema, todos aprovados ([relatório de testes automatizados](07-relatorio-de-testes-automatizados.md)).

## 4.8 Roteiros detalhados (exemplos)

**CT036. Venda acima do estoque com bloqueio (tabela de decisão, regra R3)**

| Campo | Conteúdo |
|---|---|
| Pré-condições | API no perfil de teste; Gerente autenticado; produto com estoque 2; configuração com controle de estoque e bloqueio ligados |
| Dados de entrada | `POST /api/vendas` com 3 unidades do produto, pagamento PIX |
| Passos | 1. Criar o produto com estoque 2. 2. Enviar a venda com quantidade 3. 3. Ler a resposta e o estoque do produto |
| Resultado esperado | 422 com mensagem de estoque insuficiente; o estoque continua 2; nenhuma venda gravada |
| Resultado obtido | Conforme esperado |
| Status | Passou |
| Evidência | `reports/backend/testes-h2/TEST-com.pontoesabor.lanchonete.controller.VendaControllerTest.xml` |

**CT059. Venda completa no navegador (aceitação)**

| Campo | Conteúdo |
|---|---|
| Pré-condições | API no ar com os dados de demonstração; frontend compilado; usuária `fernanda` (Vendedor) |
| Dados de entrada | 2 unidades do primeiro produto disponível; pagamento PIX |
| Passos | 1. Entrar como `fernanda`. 2. Em Vendas, clicar duas vezes em "Adicionar". 3. "Revisar pedido". 4. Escolher PIX. 5. "Confirmar venda". 6. "Ver pedido" e anotar o número. 7. Em Pedidos, buscar o número |
| Resultado esperado | "Venda realizada com sucesso!"; o pedido aparece uma vez na busca de Pedidos |
| Resultado obtido | Conforme esperado |
| Status | Passou |
| Evidência | `reports/frontend/testes-sistema/resultados.xml`; em caso de falha, o CI guarda captura de tela e trace no artefato `relatorio-playwright` |
