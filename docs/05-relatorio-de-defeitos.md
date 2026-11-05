# 5. Relatório de defeitos

> Seções 6 (revisão técnica e inspeção) e 15 (defeitos) do enunciado.

## 5.1 Revisão técnica formal

Em 28/10/2026 a equipe revisou requisitos, arquitetura, código, casos de teste e documentação, com o enunciado como
lista de verificação. Os achados viraram os defeitos D001 a D006. Depois disso, a execução dos testes e o uso do
sistema revelaram D007 a D009.

| Artefato revisado | Como | Achados |
|---|---|---|
| Requisitos | Leitura contra os critérios do enunciado (ambíguo, incompleto, contraditório, não verificável) | 5 problemas, reescritos em [01-requisitos.md](01-requisitos.md#111-validação-dos-requisitos); D005 |
| Arquitetura | Diagrama frontend → API → banco; perfis H2 e PostgreSQL | D006 |
| Código | Análise estática (PMD, CPD, SpotBugs, ESLint) e leitura dos serviços de risco crítico | D002 (55 problemas, tratados no [relatório de análise estática](06-relatorio-de-analise-estatica.md)) |
| Casos de teste | Níveis e camadas cobertos pela suíte | D001, D003 |
| Documentação | Índice `docs/README.md` contra os arquivos existentes | D004 |

## 5.2 Registro resumido

| ID | Artefato | Título | Severidade | Prioridade | Status |
|---|---|---|---|---|---|
| D001 | Testes | Frontend sem nenhum teste automatizado | Alta | Alta | Corrigido |
| D002 | Configuração | Nenhuma ferramenta de análise estática no projeto nem no CI | Alta | Alta | Corrigido |
| D003 | CI | Cobertura sem mínimo que reprove o build | Média | Alta | Corrigido |
| D004 | Documentação | Índice cita documentos que não existem | Média | Alta | Corrigido |
| D005 | Requisito | Desempenho dos dashboards sem meta nem medição | Média | Média | **Aberto** (meta definida em RNF04; medição pendente) |
| D006 | Testes | Testes só rodam no H2, nunca no PostgreSQL de produção | Média | Média | Corrigido |
| D007 | Código / ambiente | Login responde 409 no H2 depois que o computador hiberna | Baixa | Baixa | **Aberto** (contornado) |
| D008 | Código | Busca de clientes por e-mail e telefone quebrada pela refatoração do `Contato` | Alta | Alta | Corrigido antes do merge |
| D009 | Configuração | `npm install` do CI falha com Vitest e jsdom | Média | Alta | Corrigido |

## 5.3 Ciclo de cada defeito

O ciclo seguido é o do enunciado: **defeito → registro → análise → correção → reteste → regressão → encerramento**.
A correção vai numa branch, o PR roda a suíte completa no CI (reteste e regressão) e o merge encerra o defeito.

### D001. Frontend sem nenhum teste automatizado

| Campo | Conteúdo |
|---|---|
| Descrição | O `package.json` do frontend só tinha `typecheck` e `build`; nenhuma tela ou função era testada automaticamente. |
| Passos para reprodução | 1. Abrir `frontend/package.json` na versão de 28/10. 2. Procurar um script de teste. |
| Resultado esperado | Testes unitários, de componente e de sistema automatizados (seções 12 e 13 do enunciado). |
| Resultado obtido | Nenhum teste. |
| Severidade / prioridade | Alta / Alta |
| Evidência | Revisão técnica de 28/10/2026 |
| Responsável | Equipe (automação) |
| Correção | PR #20: Vitest + Testing Library (63 testes unitários e de componente) e Playwright (6 testes de sistema com a API real), ligados ao CI. |
| Reteste | CI de 04/11/2026: 63/63 e 6/6 aprovados ([reports/frontend](../reports/README.md)). |
| Status | Corrigido e encerrado |

### D002. Nenhuma ferramenta de análise estática

| Campo | Conteúdo |
|---|---|
| Descrição | O projeto não rodava nenhuma análise estática, obrigatória na seção 7 do enunciado. |
| Passos para reprodução | 1. Procurar PMD, SpotBugs ou ESLint no `pom.xml`, no `package.json` e no CI. |
| Resultado esperado | Análise estática no build e no CI, reprovando problemas novos. |
| Resultado obtido | Nenhuma ferramenta configurada. |
| Severidade / prioridade | Alta / Alta |
| Evidência | Revisão técnica de 28/10/2026 |
| Responsável | Equipe (análise estática) |
| Correção | PR #17: PMD, CPD, SpotBugs, ESLint e SonarCloud no build e no CI (55 + 1 problemas encontrados). PR #19: os 56 problemas tratados e o build passando a reprovar qualquer problema novo. |
| Reteste | `./mvnw verify` e CI: 0 problema em PMD, CPD, SpotBugs e ESLint ([06-relatorio-de-analise-estatica.md](06-relatorio-de-analise-estatica.md)). |
| Status | Corrigido e encerrado |

### D003. Cobertura sem mínimo que reprove o build

| Campo | Conteúdo |
|---|---|
| Descrição | O JaCoCo gerava o relatório, mas uma queda de cobertura passava despercebida. |
| Passos para reprodução | 1. Remover testes. 2. Rodar `./mvnw verify`. |
| Resultado esperado | Build reprovado abaixo da meta. |
| Resultado obtido | Build aprovado com qualquer cobertura. |
| Severidade / prioridade | Média / Alta |
| Evidência | Revisão técnica de 28/10/2026 |
| Responsável | Equipe (automação) |
| Correção | PR #22: regra `jacoco:check` no `verify`, mínimo de 90% das instruções e 87% dos branches. |
| Reteste | Com o mínimo de branches forçado em 95%, o build reprovou: *"branches covered ratio is 0.88, but expected minimum is 0.95"*. Com os valores reais, passou. |
| Status | Corrigido (PR #22) |

### D004. Índice da documentação cita documentos que não existem

| Campo | Conteúdo |
|---|---|
| Descrição | O `docs/README.md` listava "Requisitos" e "Plano de testes", mas os arquivos não existiam. |
| Passos para reprodução | 1. Abrir `docs/README.md`. 2. Procurar os arquivos citados. |
| Resultado esperado | Todos os entregáveis do enunciado no repositório. |
| Resultado obtido | Itens sem arquivo. |
| Severidade / prioridade | Média / Alta |
| Evidência | Revisão técnica de 28/10/2026 |
| Responsável | Equipe (documentação) |
| Correção | Documentação reorganizada nos entregáveis da seção 24 do enunciado (`docs/01` a `docs/10`) e pasta `/reports` criada. |
| Reteste | Todos os links do [índice](README.md) abrem um arquivo existente. |
| Status | Corrigido |

### D005. Desempenho dos dashboards sem meta nem medição

| Campo | Conteúdo |
|---|---|
| Descrição | Nenhum requisito dizia quão rápidos o Dashboard e os Picos devem ser, e nada era medido. |
| Passos para reprodução | 1. Procurar uma meta de tempo nos requisitos originais. |
| Resultado esperado | Meta verificável e medição. |
| Resultado obtido | Sem meta nem medição. |
| Severidade / prioridade | Média / Média |
| Evidência | Validação dos requisitos ([01-requisitos.md](01-requisitos.md#111-validação-dos-requisitos)) |
| Responsável | Equipe (métricas) |
| Correção | Meta definida no RNF04 (p95 ≤ 2 s com 13 mil vendas). **Falta** a medição. |
| Reteste | Pendente |
| Status | Aberto |

### D006. Testes só rodam no H2

| Campo | Conteúdo |
|---|---|
| Descrição | A suíte de integração rodava apenas no H2; uma diferença de comportamento do PostgreSQL (produção) não seria pega. |
| Passos para reprodução | 1. Ver o CI de 28/10: só o job Backend com H2. |
| Resultado esperado | A mesma suíte aprovada no PostgreSQL (RNF06). |
| Resultado obtido | Nenhuma execução no PostgreSQL. |
| Severidade / prioridade | Média / Média |
| Evidência | Revisão técnica de 28/10/2026 |
| Responsável | Equipe (automação) |
| Correção | PR #22: job *Backend (testes no PostgreSQL)* com PostgreSQL 17 como service container. |
| Reteste | CI de 04/11/2026: 253/253 no PostgreSQL ([reports/backend/testes-postgresql](../reports/README.md)). |
| Status | Corrigido (PR #22) |

### D007. Login responde 409 no H2 depois que o computador hiberna

| Campo | Conteúdo |
|---|---|
| Descrição | Com a API rodando localmente no perfil `h2` durante horas, depois que o computador hibernou todos os logins passaram a falhar. |
| Passos para reprodução | 1. Subir a API com o perfil `h2`. 2. Deixar o computador hibernar (o log mostra "clock leap" de cerca de 14 min). 3. Fazer login. |
| Resultado esperado | 200 e token. |
| Resultado obtido | 409 "A operação viola uma restrição do banco de dados"; no log, `Check constraint invalid: "ck_usuario_perfil"` no `UPDATE usuario` que grava o último acesso. |
| Severidade / prioridade | Baixa / Baixa (só no banco em memória de desenvolvimento; não reproduzido no PostgreSQL de produção) |
| Evidência | Log da API local de 28/10/2026 |
| Responsável | Equipe (backend) |
| Análise | O H2 2.4.240 é mais novo que a versão verificada pelo Flyway (aviso no log). A restrição `CHECK` passou a falhar ao avaliar a expressão, e não por dado inválido: o perfil gravado era válido. |
| Correção | Contorno: reiniciar a API. Correção definitiva em estudo: fixar o H2 na versão verificada pelo Flyway, ou usar o PostgreSQL também no desenvolvimento. |
| Reteste | Depois de reiniciar, todos os logins voltaram a funcionar. |
| Status | Aberto (contornado) |

### D008. Busca de clientes por e-mail e telefone quebrada pela refatoração

| Campo | Conteúdo |
|---|---|
| Descrição | Na correção do CPD (PR #19), telefone, e-mail e endereço foram para o `@Embeddable` `Contato`. A busca de clientes continuou procurando `email` e `telefone` direto na entidade. |
| Passos para reprodução | 1. Aplicar a refatoração. 2. Rodar `./mvnw verify`. |
| Resultado esperado | Busca de clientes por nome, CPF ou telefone funcionando. |
| Resultado obtido | Erro 500, "Could not resolve attribute 'email' of 'Cliente'". |
| Severidade / prioridade | Alta / Alta (a tela Cadastros › Clientes deixaria de buscar) |
| Evidência | Falha do teste de regressão `ClienteControllerTest.buscaPorNomeCpfOuTelefone` (1 erro em 240 testes) |
| Responsável | Equipe (backend) |
| Correção | A especificação de busca passou a usar `contato.email` e `contato.telefone`. |
| Reteste | 246/246 aprovados no mesmo PR, antes do merge. |
| Status | Corrigido e encerrado; **nunca chegou à `develop`**, graças à suíte de regressão |

### D009. `npm install` do CI falha com Vitest e jsdom

| Campo | Conteúdo |
|---|---|
| Descrição | Ao adicionar as dependências de teste do frontend, o `npm install` do CI falhou com um erro interno do npm 10 ("edgesOut") na resolução das dependências opcionais. |
| Passos para reprodução | 1. Adicionar Vitest e jsdom ao `package.json`. 2. Rodar o CI. |
| Resultado esperado | Instalação concluída. |
| Resultado obtido | Job Frontend vermelho no `npm install`. |
| Severidade / prioridade | Média / Alta (bloqueava todo o pipeline do frontend) |
| Evidência | Log do CI do PR #20 |
| Responsável | Equipe (automação) |
| Correção | `frontend/.npmrc` com `legacy-peer-deps=true`, que vale também para o build do GitHub Pages. |
| Reteste | CI do PR #20 e da `develop`: instalação e todos os jobs verdes. |
| Status | Corrigido e encerrado |

## 5.4 Situação

| Indicador | Valor |
|---|---|
| Defeitos encontrados | 9 |
| Por severidade | Crítica 0 · Alta 3 · Média 5 · Baixa 1 |
| Corrigidos | 7 |
| Abertos | 2 (D005 Média, D007 Baixa); nenhum crítico ou alto aberto |
| Encontrados pela revisão técnica | 6 (D001–D006) |
| Encontrados pela execução dos testes e do sistema | 3 (D007–D009) |

As métricas de defeitos e a densidade estão no [relatório de métricas](08-relatorio-de-metricas.md).
