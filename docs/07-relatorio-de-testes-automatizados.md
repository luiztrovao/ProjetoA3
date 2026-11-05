# 7. Relatório de testes automatizados

> Seções 13 e 14 do enunciado: execução automática **executar → dezenas de casos → resultados → PASS/FAIL → relatório**,
> integrada ao pipeline de CI. Execução de referência: CI do GitHub Actions nº 41921004487, de 04/11/2026
> (commit do PR #22), com os relatórios salvos em [/reports](../reports/README.md).

## 7.1 Resultado da execução

| Suíte | Ferramenta | Banco / ambiente | Executados | Aprovados | Reprovados | Duração | Relatório |
|---|---|---|---|---|---|---|---|
| Backend: unitários e integração | JUnit 5 + Spring Boot Test | H2 | 253 | 253 | 0 | ≈ 30 s | `reports/backend/testes-h2/` |
| Backend: mesma suíte | JUnit 5 + Spring Boot Test | PostgreSQL 17 | 253 | 253 | 0 | — | `reports/backend/testes-postgresql/` |
| Frontend: unitários e componente | Vitest 4 + Testing Library | jsdom | 63 | 63 | 0 | 4,3 s | log do CI + `reports/frontend/cobertura/` |
| Sistema e aceitação | Playwright 1.62 (Chromium) | API real + H2 com 13 mil vendas | 6 | 6 | 0 | 23 s | `reports/frontend/testes-sistema/resultados.xml` |
| **Total de execuções** | | | **575** | **575** | **0** | | |

São 322 testes diferentes (253 + 63 + 6); os 253 do backend rodam duas vezes, uma em cada banco.

**Taxa de aprovação** = aprovados ÷ executados × 100 = 575 ÷ 575 × 100 = **100%**. **Taxa de falha: 0%.**

## 7.2 Tipos de teste na suíte (seção 13 do enunciado)

| Tipo pedido | Exemplos na suíte |
|---|---|
| Positivos | Login correto, venda confirmada, cliente só com nome, picos do ano agrupados por mês |
| Negativos | Senha errada, CPF duplicado, Vendedor dando desconto, cancelamento sem motivo |
| Casos-limite | `LimitesValidacaoTest` (42 casos), máscaras com 10, 11 e 12 dígitos, `timeAgo` de 59/60 min, período que inclui o dia inteiro |
| Regressão | A suíte inteira em todo PR; o caso D008 mostra a regressão pegando um defeito antes do merge |
| Diferentes níveis | Unitário, integração (H2 e PostgreSQL), componente, sistema e aceitação (Playwright) |

## 7.3 Como executar

**Backend**, na pasta `backend/` (no Windows, `.\mvnw.cmd`):

```bash
./mvnw verify
```

Roda os 253 testes, mostra PASS/FAIL por caso, gera a cobertura e reprova o build se ela ficar abaixo do mínimo ou se
a análise estática achar algum problema. Saídas:
- `target/surefire-reports/`: resultado de cada teste;
- `target/site/jacoco/index.html`: cobertura.

**Frontend**, na pasta `frontend/`:

```bash
npm test                  # 63 testes unitários e de componente
npm run test:cobertura    # os mesmos, com relatório de cobertura em coverage/
npm run test:e2e          # 6 testes de sistema (antes: ./mvnw -DskipTests package no backend)
```

## 7.4 Integração ao pipeline de CI

```
Git → Commit → Pipeline → Análise estática → Testes automatizados → Cobertura → Resultado
```

Em todo push e PR para `develop` e `main`, o GitHub Actions roda os seis jobs descritos no
[plano de testes](03-plano-de-testes.md#36-automação). O resultado aparece no próprio PR: verde libera o merge,
vermelho bloqueia. Cada execução publica os relatórios como artefatos para download:

| Artefato | Conteúdo |
|---|---|
| `relatorios-backend` | Surefire (resultado de cada teste), PMD, CPD e SpotBugs |
| `relatorios-backend-postgres` | Surefire da execução no PostgreSQL |
| `cobertura-backend` | Relatório HTML, CSV e XML do JaCoCo |
| `cobertura-frontend` | Relatório HTML, JSON e LCOV da cobertura do frontend |
| `relatorio-eslint` | Problemas do ESLint em JSON |
| `relatorio-playwright` | Relatório HTML e JUnit dos testes de sistema; capturas de tela e trace em caso de falha |

## 7.5 Demonstração pedida pelo professor (seção 25)

| Pedido | Como mostrar |
|---|---|
| Executar um teste | `./mvnw test -Dtest=VendaTest` ou `npx vitest run tests/unit/format.test.ts` |
| Criar um novo caso | Acrescentar `"4, false"` ao `@CsvSource` de `LimitesValidacaoTest.senhaDe6A72Caracteres` (senha com 4 caracteres) e rodar de novo |
| Alterar um valor e provocar uma falha | Comentar o `if` que recusa desconto maior que o subtotal em `Venda.aplicarDesconto` e ver `descontoNaoPodePassarDoSubtotalNemSerNegativo` ficar vermelho |
| Executar a suíte automatizada | `./mvnw verify` e `npm test` |
| Mostrar a cobertura | Abrir `backend/target/site/jacoco/index.html` |
| Demonstrar caixa-preta | `LimitesValidacaoTest` (valor-limite) e a tabela de decisão de `VendaTest` |
| Demonstrar caixa-branca | `VendaFiltrosTest` e a classe `VendaRepository.Filtros` com 22 de 22 branches cobertos no JaCoCo |
