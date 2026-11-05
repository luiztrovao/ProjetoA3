# 3. Plano de Testes

> Seções 8, 9, 10, 12, 13 e 14 do enunciado. Os casos individuais estão no
> [catálogo de casos de teste](04-catalogo-de-casos-de-teste.md).

## 3.1 Estratégia baseada em risco

A intensidade dos testes é proporcional ao risco (probabilidade × impacto). Contagem real de 04/11/2026:
253 testes no backend, 63 testes unitários e de componente no frontend e 6 testes de sistema.

| Funcionalidade | Probabilidade | Impacto | Risco | Testes automatizados |
|---|---|---|---|---|
| Registro de venda (total, desconto, troco, filtros de pedidos) | Alta | Alta | **Crítico** | 42 no backend (`VendaTest`, `VendaControllerTest`, `VendaFiltrosTest`, `VendaRepositoryTest`) + 2 de sistema |
| Login e permissões por perfil | Média | Alta | **Crítico** | 37 no backend (`AuthControllerTest`, `PermissoesTest`, `AuthServiceTest`, `JwtPropertiesTest`) + frontend (`LoginPage`, `client`) + 3 de sistema |
| Estoque (baixa e devolução) | Alta | Alta | **Crítico** | dentro de `VendaTest`, `VendaControllerTest` e `ProdutoTest`, incluindo a tabela de decisão |
| Cadastro de funcionários (último Dono) | Média | Alta | Alto | 26 (`UsuarioServiceTest`, `UsuarioControllerTest`) |
| Relatórios e picos de venda | Média | Média | Alto | 28 (`AgregadorVendasTest`, `RelatorioControllerTest`, `PeriodosPicoTest`) + frontend (`periodos`) |
| Cadastro de produtos e clientes | Média | Média | Médio | 38 + frontend (`format`, `mappers`) |
| Upload de imagens | Baixa | Média | Médio | 14 (`ImagemControllerTest`, `ImagemServiceTest`) |
| Notificações e configurações | Baixa | Baixa | Baixo | 17 no backend + frontend (`HeaderMenus`) + 1 de sistema |
| Transversais (validação, cópias defensivas, contexto) | — | — | — | 51 (`LimitesValidacaoTest` 42, `CopiasDefensivasTest` 6, demais 3) |

**Por que a venda recebe mais testes que o upload de imagem?** A venda envolve dinheiro, estoque e permissão ao
mesmo tempo e roda dezenas de vezes por dia: um erro vira prejuízo na hora. A imagem tem impacto visual e é trocada
raramente. Por isso a venda tem três vezes mais testes e é o único fluxo testado de ponta a ponta no navegador.

## 3.2 Técnicas caixa-preta

São aplicadas quatro técnicas às regras reais da API, sem olhar a implementação.

### 3.2.1 Análise de valor-limite

Cada fronteira é testada logo abaixo, em cima e logo acima, em `LimitesValidacaoTest` (42 casos parametrizados)
e nos testes de serviço.

| Regra | Faixa válida | Valores testados | Esperado |
|---|---|---|---|
| Senha do funcionário | 6 a 72 caracteres | 5, 6, 7, 71, 72, 73 | 5 e 73 recusados (400) |
| Quantidade do item | 1 a 999 | 0, 1, 2, 998, 999, 1000 | 0 e 1000 recusados |
| Preço do produto | ≥ 0,01, 2 casas | 0,00, 0,01, 0,02, 0,001 | 0,00 e 0,001 recusados |
| Alerta de pedido atrasado | 5 a 240 min | 4, 5, 6, 239, 240, 241 | 4 e 241 recusados |
| Segredo do JWT | ≥ 32 caracteres | 31, 32 | 31 recusado |
| Desconto da venda | 0 a subtotal | −0,01, 0, subtotal, subtotal + 0,01 | negativo e acima do subtotal recusados |
| Valor recebido em dinheiro | ≥ total | total − 0,01, total, total + 0,01 | abaixo do total recusado; troco 0 e 0,01 |
| Imagem | ≤ 1 MB | 1 MB − 1 byte, 1 MB, 1 MB + 1 byte | acima de 1 MB recusado |
| Estoque baixo | estoque ≤ mínimo | mínimo − 1, mínimo, mínimo + 1 | os dois primeiros marcam estoque baixo |
| Máscaras do frontend | CPF 11, telefone 10/11, CEP 8 dígitos | 10, 11 e 12 dígitos | o excedente é descartado |
| Tempo das notificações | minutos/horas/dias | 0, 1, 59, 60, 1439, 1440 min | "agora", "há 1 min", "há 59 min", "há 1 h", "há 23 h", "ontem" |

### 3.2.2 Particionamento de equivalência

| Entrada | Classes válidas | Classes inválidas |
|---|---|---|
| CPF do cliente | vazio; CPF válido com ou sem máscara | dígitos verificadores errados; menos de 11 dígitos; já cadastrado |
| Login | nome de usuário; e-mail | senha errada; usuário inexistente; usuário inativo; campos vazios |
| Arquivo de imagem | PNG; JPG | GIF/PDF; arquivo renomeado para `.png` sem ser PNG |
| Forma de pagamento | habilitada nas configurações | desabilitada; inexistente |
| Busca de pedidos | número com ou sem máscara; nome do cliente; nome do vendedor | número com mais de 18 dígitos (busca só por nome) |

### 3.2.3 Tabela de decisão: confirmação da venda com estoque

| Regra | R1 | R2 | R3 | R4 |
|---|---|---|---|---|
| Controle de estoque ligado | Não | Sim | Sim | Sim |
| Bloquear venda sem estoque | — | Sim | Sim | Não |
| Quantidade ≤ estoque | — | Sim | Não | Não |
| **Ação** | Vende, não mexe no estoque | Vende e baixa | Recusa (422) | Vende e baixa só o disponível |
| Teste automatizado | `semControleDeEstoqueNaoMexeNoEstoque` | `confirmarAplicaStatusInicialEBaixaEstoque` | `bloqueiaVendaSemEstoqueQuandoConfigurado` | `semBloqueioVendeEBaixaSoOQueHaNoEstoque` |

### 3.2.4 Casos inválidos e exceções

JSON mal formatado (400), ordenação por campo inexistente (400), registro inexistente (404), cancelamento sem motivo,
cancelar duas vezes, concluir pedido já concluído, alterar venda confirmada, servidor fora do ar no frontend
(erro com status 0) e sessão expirada (401 apaga o token e volta ao login).

## 3.3 Técnicas caixa-branca

- **Cobertura de instruções e de branches:** JaCoCo no backend e V8 no frontend. O `./mvnw verify` **reprova o build**
  abaixo de 90% das instruções ou 87% dos branches.
- **Caminhos e decisões:** a tabela de decisão da seção 3.2.3 é também o grafo de caminhos de `Venda.confirmar`:
  4 caminhos independentes (controle de estoque × bloqueio × quantidade), todos cobertos.
- **Decisões dos filtros de pedidos:** `VendaFiltrosTest` passa por cada `if` de `VendaRepository.Filtros`, com o
  parâmetro nulo e preenchido (22 de 22 branches cobertos).
- **Branches orientando novos casos:** o relatório do JaCoCo aponta as classes com branches perdidos, e os casos novos
  foram escritos para elas (veja o [relatório de métricas](08-relatorio-de-metricas.md)).

## 3.4 Níveis de teste

| Nível | O que verifica | Onde está no projeto |
|---|---|---|
| Unitário | Regras de negócio isoladas, sem banco | Backend: `model/VendaTest`, `ProdutoTest`, `ConfiguracaoTest`, `service/AgregadorVendasTest`, `PeriodosPicoTest`, serviços com Mockito. Frontend: `tests/unit/format`, `mappers`, `client`, `periodos` |
| Integração | API + segurança + JPA + Flyway, nos bancos H2 e PostgreSQL 17 (`@SpringBootTest`, MockMvc, `@DataJpaTest`) | `controller/*ControllerTest`, `PermissoesTest`, `repository/VendaRepositoryTest`, `VendaFiltrosTest`. No frontend, os testes de componente (`LoginPage`, `HeaderMenus`) integram tela + cliente HTTP com a API simulada |
| Sistema | Sistema completo (frontend compilado + API + banco com 13 mil vendas) pelo navegador | `frontend/tests/e2e/*.spec.ts` (Playwright) |
| Aceitação | Critérios CA-01 a CA-22 | Cada critério ligado a casos do catálogo; os testes de sistema `venda.spec.ts` e `acesso.spec.ts` validam os fluxos do ponto de vista do Vendedor, do Gerente e do Dono |
| Regressão | Funcionalidades já aprovadas continuam funcionando | Suíte completa no CI em todo push e PR; um PR com qualquer teste vermelho não pode ser mesclado |

## 3.5 Ambiente de teste

| Ambiente | Banco | Uso |
|---|---|---|
| Local (Windows) | H2 em memória (modo PostgreSQL) | Desenvolvimento e `./mvnw verify` |
| CI, job Backend (Ubuntu) | H2 | Suíte completa, cobertura e análise estática |
| CI, job Backend PostgreSQL (Ubuntu) | PostgreSQL 17 (service container) | A mesma suíte no banco de produção |
| CI, job Playwright (Ubuntu + Chromium) | H2 com os dados de demonstração | Testes de sistema e aceitação |
| Produção | PostgreSQL no Neon | Sistema publicado, verificação manual |

## 3.6 Automação

A suíte é 100% automatizada e roda com um comando por camada:

| Comando | Pasta | O que roda |
|---|---|---|
| `./mvnw verify` | `backend/` | 253 testes, cobertura com mínimo, PMD, CPD e SpotBugs |
| `npm test` / `npm run test:cobertura` | `frontend/` | 63 testes unitários e de componente, com cobertura |
| `npm run test:e2e` | `frontend/` | 6 testes de sistema (precisa do jar: `./mvnw -DskipTests package`) |
| `npm run lint` | `frontend/` | ESLint |

**Pipeline (`.github/workflows/ci.yml`), em todo push e PR para `develop` e `main`:**

```
Git → Commit → Pipeline → Análise estática → Testes automatizados → Cobertura → Resultado (PASS/FAIL no PR)
```

1. **Backend:** `./mvnw -B verify`, que compila, roda os testes, mede a cobertura com mínimo e roda PMD, CPD e SpotBugs.
   Artefatos: `cobertura-backend` e `relatorios-backend`.
2. **Backend (testes no PostgreSQL):** a mesma suíte num PostgreSQL 17. Artefato: `relatorios-backend-postgres`.
3. **Frontend:** typecheck, ESLint, Vitest com cobertura e build. Artefatos: `relatorio-eslint`, `cobertura-frontend` e `frontend-dist`.
4. **Testes de sistema (Playwright):** sobe a API e o frontend e roda os fluxos no Chromium. Artefato: `relatorio-playwright`.
5. **SonarCloud:** roda quando o segredo `SONAR_TOKEN` existe.
6. **Imagem Docker da API:** confere o build usado no Render.

Os resultados de cada execução estão no [relatório de testes automatizados](07-relatorio-de-testes-automatizados.md).
