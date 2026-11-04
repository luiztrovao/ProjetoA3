# 2. Plano de Qualidade de Software

> Seção 4.2 do enunciado. Os requisitos e critérios de aceitação estão em [01-requisitos.md](01-requisitos.md);
> a estratégia e os níveis de teste em detalhe, no [plano de testes](03-plano-de-testes.md).

## 2.1 Objetivos de qualidade

- **Zero erro de cálculo em vendas:** subtotal, desconto, total e troco.
- **Zero acesso indevido entre perfis:** o Vendedor não dá desconto, não cancela pedidos e não vê relatórios.
- **Estoque sempre consistente:** baixa na confirmação, devolução no cancelamento e nunca negativo.
- **Dashboards e picos batendo com as vendas registradas.**
- **Toda mudança verificada automaticamente** (build, testes, cobertura e análise estática) antes de entrar na `develop`.

## 2.2 Atributos de qualidade avaliados

| Atributo | Como é avaliado | Meta |
|---|---|---|
| Confiabilidade | Taxa de falha da suíte automatizada | 0% de falhas na `develop` |
| Segurança | Testes de autenticação e permissão; problemas de segurança na análise estática | 100% dos testes de permissão aprovados; 0 vulnerabilidade alta |
| Usabilidade | Taxa de conclusão de tarefas (registrar venda, cadastrar produto, encontrar pedido) com 3 usuários | ≥ 90% |
| Eficiência | Tempo de resposta do dashboard e dos picos, com 13 mil vendas | p95 ≤ 2 s |
| Manutenibilidade | Complexidade, duplicação e problemas de PMD/SpotBugs/ESLint | 0 problema estático; duplicação 0 bloco ≥ 100 tokens |
| Portabilidade | Execução em H2 e PostgreSQL; desktop e celular | Suíte verde nos dois bancos |

## 2.3 Critérios de aceitação

Os 22 critérios objetivos (CA-01 a CA-22) estão em [01-requisitos.md](01-requisitos.md#110-critérios-de-aceitação).
Cada um está ligado a pelo menos um caso do [catálogo](04-catalogo-de-casos-de-teste.md).

## 2.4 Estratégia de testes

Baseada em risco: vendas, estoque e permissões recebem mais casos (veja a
[estratégia baseada em risco](03-plano-de-testes.md#31-estratégia-baseada-em-risco)).

- **Caixa-preta:** valor-limite, particionamento de equivalência, tabela de decisão, casos inválidos e exceções.
- **Caixa-branca:** cobertura de instruções e de branches com JaCoCo (backend) e V8 (frontend), mais análise de caminhos.
- **Análise estática:** PMD, CPD, SpotBugs e ESLint em todo PR.

## 2.5 Níveis de teste

Unitário, integração, sistema, aceitação e regressão. O detalhe de onde cada nível está no código fica no
[plano de testes](03-plano-de-testes.md#34-níveis-de-teste).

## 2.6 Ferramentas utilizadas

| Ferramenta | Uso |
|---|---|
| JUnit 5, AssertJ, Mockito | Testes unitários e de serviço (backend) |
| Spring Boot Test, MockMvc, H2 e PostgreSQL 17 | Testes de integração da API, nos dois bancos |
| JaCoCo 0.8.15 | Cobertura de instruções e branches, com mínimo que reprova o build |
| Vitest 4, Testing Library, jsdom | Testes unitários e de componente (frontend) |
| Playwright 1.62 | Testes de sistema e aceitação no navegador, com a API e o banco reais |
| PMD 7, CPD, SpotBugs | Análise estática do backend |
| ESLint 9 + typescript-eslint | Análise estática do frontend |
| SonarCloud (opcional) | Painel de qualidade do backend |
| GitHub Actions | Pipeline de CI em todo push e PR |
| Swagger / OpenAPI | Testes exploratórios manuais da API |
| Git + GitHub (`develop`, `feature/*`, `fix/*`) | Controle de versão e revisão por PR |

## 2.7 Responsabilidades

| Integrante | Responsabilidade |
|---|---|
| *(a preencher pela equipe)* | Requisitos, critérios de aceitação e plano de qualidade |
| *(a preencher pela equipe)* | Automação de testes e pipeline de CI |
| *(a preencher pela equipe)* | Análise estática, métricas e relatório de defeitos |
| *(a preencher pela equipe)* | Apresentação e demonstração |

## 2.8 Riscos do projeto de teste

| Risco | Efeito | Mitigação |
|---|---|---|
| Pouco tempo até a entrega | Partes do processo sem evidência | Priorização por pontos e por risco; entregas pequenas por PR |
| Ambiente gratuito instável (Render "dorme") | Medições de desempenho distorcidas | Medir o desempenho localmente, com a mesma base de 13 mil vendas |
| npm bloqueado na rede de desenvolvimento | Frontend sem execução local de testes | Toda a validação do frontend roda no CI (GitHub Actions) |
| Diferença entre H2 e PostgreSQL | Defeito que só aparece em produção | Mesma suíte rodando também no PostgreSQL 17 no CI |

## 2.9 Critérios de entrada

- Requisito com critério de aceitação definido.
- Build compilando e banco H2 subindo com as migrações.
- Dados de teste criados pelo próprio teste (nada depende de dados manuais).

## 2.10 Critérios de saída

- Todos os casos do catálogo executados.
- 100% dos testes automatizados passando no H2 e no PostgreSQL.
- Cobertura do backend ≥ 90% de instruções e ≥ 87% de branches (regra do JaCoCo no `./mvnw verify`).
- 0 problema na análise estática (PMD, CPD, SpotBugs e ESLint).
- Defeitos críticos e altos corrigidos e retestados.
- Relatórios gerados em [/reports](../reports/README.md).

## 2.11 Critérios para aprovação e reprovação

- **Aprovado:** atende a todos os critérios de saída e às metas da tabela de atributos.
- **Reprovado:** qualquer falha em teste de venda, estoque ou permissão; defeito crítico aberto; cobertura abaixo do
  mínimo; ou problema novo na análise estática. As quatro últimas condições já reprovam o PR automaticamente no CI.
