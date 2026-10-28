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

## Por enquanto a análise só relata

Para registrar o **antes** do ciclo de melhoria, os problemas aparecem no log mas **não reprovam** o build:

- backend: propriedade `analise.falhar` no `pom.xml` (`false`);
- frontend: `continue-on-error: true` no passo do ESLint em `.github/workflows/ci.yml`.

Depois das correções, troque `analise.falhar` para `true` e remova o `continue-on-error` para que novos
problemas reprovem o PR.

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
| ESLint | a medir | Primeira execução no CI |
