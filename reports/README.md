# Relatórios

Saídas **reais** das ferramentas de teste e de análise, copiadas dos artefatos do GitHub Actions da execução
nº 41921004487, de 04/11/2026 (PR #22). Todos os números da [documentação](../docs/README.md) vêm daqui.

Como a regra 25 do enunciado exige, tudo pode ser gerado de novo a qualquer momento:
- **localmente**, com os comandos da última coluna;
- **no CI**, baixando os artefatos de qualquer execução na aba **Actions** do GitHub.

| Pasta | Conteúdo | Resultado | Como gerar de novo |
|---|---|---|---|
| `backend/testes-h2/` | Resultado de cada teste do backend no H2 (JUnit/Surefire, XML) | 253 / 253 aprovados | `./mvnw verify` → `backend/target/surefire-reports/` |
| `backend/testes-postgresql/` | Os mesmos testes no PostgreSQL 17 | 253 / 253 aprovados | Job *Backend (testes no PostgreSQL)* do CI |
| `backend/cobertura/` | Cobertura do JaCoCo (CSV e XML) | 90,8% instruções · 88,0% branches | `./mvnw verify` → `backend/target/site/jacoco/` (o HTML fica no artefato `cobertura-backend`) |
| `backend/analise-estatica/` | PMD, CPD e SpotBugs (XML) | 0 problemas | `./mvnw verify` → `backend/target/pmd.xml`, `cpd.xml`, `spotbugsXml.xml` |
| `frontend/cobertura/` | Cobertura do Vitest (resumo JSON e LCOV) | 19,5% instruções · 16,2% branches | `npm run test:cobertura` → `frontend/coverage/` |
| `frontend/analise-estatica/` | Problemas do ESLint (JSON) | 0 problemas | `npm run lint:relatorio` |
| `frontend/testes-sistema/` | Testes de sistema e aceitação do Playwright (JUnit XML) | 6 / 6 aprovados | `npm run test:e2e` → `frontend/playwright-report/` |

Os 63 testes unitários e de componente do frontend aparecem no log do job *Frontend* do CI, com PASS/FAIL por arquivo.

Os relatórios do Surefire trazem as propriedades do Java do ambiente do CI (versão, sistema operacional). Nenhum
segredo é gravado neles.
