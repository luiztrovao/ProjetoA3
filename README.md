# ProjetoA3 — Sistema de Gerenciamento de Lanchonete

Sistema web para gerenciamento de lanchonete, desenvolvido para a disciplina de **Qualidade de Software**.

## Funcionalidades

- Cadastro de clientes
- Cadastro de usuários (com autenticação)
- Cadastro de produtos
- Registro de vendas
- **Dashboards estratégicos** de picos de venda por semana, mês, semestre e ano

## Stack

| Camada | Tecnologia | Hospedagem |
|---|---|---|
| Backend | Java 21 + Spring Boot (API REST) | Render |
| Banco de dados | PostgreSQL + Spring Data JPA | Neon |
| Frontend | React + Vite + TypeScript + Tailwind | GitHub Pages |

Tudo nos planos gratuitos. O passo a passo da publicação (e de como abrir o banco num SGBD) está em
[docs/publicacao.md](docs/publicacao.md).

## Estrutura do repositório

```
ProjetoA3/
├── backend/     → API REST em Spring Boot
├── frontend/    → aplicação React
├── docs/        → documentação: requisitos, planos, catálogo de casos e relatórios (entregáveis do enunciado)
├── reports/     → saídas reais dos testes, da cobertura e da análise estática
└── .github/     → templates de PR/issues e workflows de CI
```

O enunciado pede as pastas `/src`, `/tests`, `/docs`, `/reports` e `/config` e permite adaptá-las à tecnologia.
Neste projeto, com duas camadas, elas ficam assim:

| Pasta do enunciado | Onde está |
|---|---|
| `/src` | `backend/src/main/java` (API) e `frontend/src` (telas) |
| `/tests` | `backend/src/test/java` (253 testes), `frontend/tests/unit` (63) e `frontend/tests/e2e` (6) |
| `/docs` | [docs/](docs/README.md) |
| `/reports` | [reports/](reports/README.md) |
| `/config` | `backend/src/main/resources` (perfis e migrações), `backend/pom.xml`, `backend/spotbugs-exclude.xml`, `frontend/vitest.config.ts`, `frontend/playwright.config.ts`, `frontend/eslint.config.js`, `.github/workflows/`, `render.yaml` |

A documentação do Projeto Final de Garantia de Qualidade começa em [docs/README.md](docs/README.md).

## Fluxo de branches

Veja [CONTRIBUTING.md](CONTRIBUTING.md).

| Branch | Uso |
|---|---|
| `main` | Versão final/entregue. Só recebe merge de `develop` via Pull Request. |
| `develop` | Integração do trabalho da equipe. Branch padrão do repositório. |
| `feature/*` | Uma branch por funcionalidade, criada a partir de `develop`. |
| `fix/*` | Correções, criadas a partir de `develop`. |
