# Análise estática

Ferramentas que procuram problemas no código **sem executá-lo**. Elas rodam a cada push e PR no GitHub
Actions e também localmente.

| Camada | Ferramenta | O que procura | Relatório |
|---|---|---|---|
| Backend | **PMD 7** (regras padrão do plugin Maven) | Más práticas, código morto, problemas de estilo | `backend/target/pmd.xml` e `target/reports/pmd.html` |
| Backend | **CPD** (do PMD, a partir de 100 tokens) | Código duplicado | `backend/target/cpd.xml` e `target/reports/cpd.html` |
| Backend | **SpotBugs** (esforço máximo, prioridade média ou maior) | Possíveis bugs no bytecode, exposição de estado interno | `backend/target/spotbugsXml.xml` |
| Backend | **SonarCloud** (opcional) | Bugs, vulnerabilidades, code smells, duplicação e cobertura num painel | sonarcloud.io |
| Frontend | **ESLint 9** + typescript-eslint + regras de hooks do React | Erros de TypeScript e de hooks, variáveis não usadas | `frontend/eslint-report.json` |

No CI os relatórios ficam nos artefatos `relatorios-backend` e `relatorio-eslint` de cada execução.

## Como rodar

Backend (na pasta `backend/`): `./mvnw verify` roda testes, cobertura e as três análises.
Só a análise, sem os testes:

```bash
./mvnw -DskipTests test-compile pmd:check pmd:cpd-check spotbugs:check
```

Frontend (na pasta `frontend/`): `npm run lint` mostra os problemas; `npm run lint:relatorio` gera o JSON.

## A análise reprova o build

Desde 03/11/2026, depois das correções, **qualquer problema novo reprova o build** (localmente e no PR):

- backend: propriedade `analise.falhar` no `pom.xml` (`true`);
- frontend: o passo do ESLint em `.github/workflows/ci.yml` não tem mais `continue-on-error`.

Os falsos positivos do SpotBugs que foram analisados ficam em `backend/spotbugs-exclude.xml`, com a justificativa
de cada grupo (veja abaixo). A exclusão vale só para o par alerta + classe/campo listado.

## Ativando o SonarCloud

O job `SonarCloud (backend)` do CI só roda quando o segredo `SONAR_TOKEN` existe:

1. Entre em [sonarcloud.io](https://sonarcloud.io) com a conta do GitHub e importe o repositório `luiztrovao/ProjetoA3`
   (organização `luiztrovao`, chave do projeto `luiztrovao_ProjetoA3`).
2. Em **Administration → Analysis Method**, desligue a *Automatic Analysis* (a análise vem do CI).
3. Gere um token em **My Account → Security** e cadastre-o no GitHub em
   **Settings → Secrets and variables → Actions** com o nome `SONAR_TOKEN`.

A cobertura do JaCoCo (`target/site/jacoco/jacoco.xml`) é enviada junto.

## Resultado inicial (antes), 28/10/2026

| Ferramenta | Problemas | Detalhe |
|---|---|---|
| PMD | 1 | `TooManyStaticImports` em `VendaService` (prioridade 3) |
| CPD | 1 duplicação, 43 linhas | Getters/setters de telefone, e-mail e endereço repetidos em `Cliente` e `Configuracao` |
| SpotBugs | 53 | 52 `EI_EXPOSE_REP`/`EI_EXPOSE_REP2` (listas e serviços guardados ou devolvidos sem cópia) e 1 `CT_CONSTRUCTOR_THROW` em `ItemVenda` |
| ESLint | 1 erro | `no-irregular-whitespace` no BOM do CSV de relatórios (`ReportsPage.tsx`) |

## Correções e resultado final (depois), 03/11/2026

| Ferramenta | Antes | Depois | O que foi feito |
|---|---|---|---|
| PMD | 1 | **0** | `VendaService` usa `Filtros.buscaGeral(...)` em vez de 6 imports estáticos |
| CPD | 1 duplicação (43 linhas) | **0** | Telefone, e-mail e endereço viraram o `@Embeddable` `Contato` (record imutável), usado por `Cliente` e `Configuracao`. As colunas no banco não mudaram |
| SpotBugs | 53 | **0** | 33 corrigidos no código e 20 falsos positivos documentados (detalhe abaixo) |
| ESLint | 1 erro | **0** | BOM do CSV de relatórios escrito como escape (`no-irregular-whitespace`), no PR #17 |

**SpotBugs, corrigidos no código (33):**

- **30 `EI_EXPOSE_REP`/`EI_EXPOSE_REP2` em 13 records de DTO** (respostas de páginas, erros, notificações, Dashboard, Relatórios,
  Picos e vendas): os records guardam uma **cópia imutável** das listas (`List.copyOf`). Nas requisições
  (`VendaRequest.itens` e `PreferenciasVendaDto.formasPagamento`), a cópia aceita `null`, para que o campo ausente
  continue chegando à validação com a mensagem amigável.
- **2 em `Imagem`**: o arquivo (`byte[]`) é copiado ao criar e ao ler a imagem.
- **1 `CT_CONSTRUCTOR_THROW` em `ItemVenda`**: a validação da quantidade saiu do construtor para a fábrica
  `ItemVenda.criar(...)`. Um construtor que lança exceção deixa um objeto meio criado exposto a subclasses.

**SpotBugs, falsos positivos documentados (20):**

- **11 de injeção de dependência**: controllers e serviços guardam os outros beans que o Spring entrega no construtor
  (serviços, `EntityManager`, `JsonMapper`). Eles são compartilhados de propósito, e copiá-los não faria sentido.
- **9 de associações JPA**: `Produto → Categoria`, `Venda → Cliente/Usuário` e `ItemVenda → Venda/Produto` precisam
  apontar para a mesma instância gerenciada pelo Hibernate; uma cópia deixaria de ser a entidade do banco.

**Regressão:** a refatoração do `Contato` quebrou a busca de clientes por e-mail e telefone, que procurava os campos
direto na entidade. O teste `ClienteControllerTest.buscaPorNomeCpfOuTelefone` falhou e a busca foi corrigida para
`contato.email`/`contato.telefone`. O teste novo `CopiasDefensivasTest` garante que as cópias continuam funcionando.

| Indicador (backend) | Antes (28/10) | Depois (03/11) |
|---|---|---|
| Problemas estáticos (PMD + CPD + SpotBugs) | 55 | 0 |
| Testes automatizados | 240 | 246 (0 falhas) |
| Cobertura de instruções | 90,2% | 90,2% |
| Cobertura de branches | 85,8% | 85,9% |
