# 8. Relatório de métricas

> Seções 16, 17 e 19 do enunciado. Valores reais da execução do CI de 04/11/2026 (nº 41921004487), salvos em
> [/reports](../reports/README.md). Os valores "antes" são de 28/10/2026, início do ciclo de melhoria.

## 8.1 Métricas de testes

| Métrica | Valor |
|---|---|
| Quantidade de casos de teste (catálogo) | 60 ([04-catalogo-de-casos-de-teste.md](04-catalogo-de-casos-de-teste.md)) |
| Testes automatizados diferentes | 322 (backend 253, frontend 63, sistema 6) |
| Execuções no CI | 575 (os 253 do backend rodam no H2 e no PostgreSQL) |
| Aprovados / reprovados | 575 / 0 |
| Taxa de aprovação | 100% |
| Taxa de falha | 0% |

```
Taxa de aprovação = testes aprovados ÷ testes executados × 100 = 575 ÷ 575 × 100 = 100%
```

## 8.2 Métricas de defeitos

| Métrica | Valor |
|---|---|
| Defeitos encontrados | 9 (D001–D009, [05-relatorio-de-defeitos.md](05-relatorio-de-defeitos.md)) |
| Por severidade | Crítica 0 · Alta 3 · Média 5 · Baixa 1 |
| Corrigidos | 7 (78%) |
| Abertos | 2: D005 (Média, medição de desempenho) e D007 (Baixa, só no H2 local) |
| Densidade de defeitos | 3,3 por mil linhas executáveis (9 ÷ 2,754 KLOC: 1.532 linhas no backend + 1.222 no frontend) |
| Defeitos de comportamento no código | 2 (D007, D008): 0,7 por KLOC |

## 8.3 Métricas de código

| Métrica | Backend (JaCoCo) | Frontend (V8) |
|---|---|---|
| Cobertura de instruções | **90,8%** (7.501 / 8.265) | 19,5% (305 / 1.562) |
| Cobertura de branches | **88,0%** (361 / 410) | 16,2% (206 / 1.273) |
| Cobertura de linhas | 92,6% (1.418 / 1.532) | 20,5% (251 / 1.222) |
| Cobertura de métodos / funções | 94,2% (468 / 497) | 15,2% (83 / 547) |
| Complexidade ciclomática total | 706 (89,5% coberta) | — |
| Código duplicado (CPD, ≥ 100 tokens) | 0 | — |
| Problemas da análise estática | 0 (PMD, CPD, SpotBugs) | 0 (ESLint) |

**Cobertura do backend por camada:**

| Pacote | Instruções | Branches | Branches sem teste |
|---|---|---|---|
| `model` (regras de negócio) | 96,8% | 90,3% | 7 |
| `service` | 95,8% | 88,4% | 23 |
| `repository` | 93,1% | 90,9% | 4 |
| `security` | 97,3% | 50,0% | 2 |
| `controller` | 91,1% | — (sem decisões) | 0 |
| `dto`, `util` | 100% | 100% | 0 |
| `config` (dados de demonstração, CORS, OpenAPI) | 70,3% | 80,6% | 13 |

## 8.4 Interpretação

**O que significa 90,8% de instruções e 88,0% de branches para este projeto?**
- As camadas onde estão as regras de risco crítico, `model` e `service` (venda, estoque, permissões, picos), passam
  de 95% das instruções. Por isso a cobertura é **suficiente** para o risco principal.
- Os 49 branches sem teste se concentram em:
  - `DadosIniciais` (11 de 14): só cria os dados de demonstração, risco baixo;
  - quatro serviços com 4 cada (`UsuarioService`, `RelatorioService`, `VendaService`, `NotificacaoService`), em
    caminhos de erro raros.
- A meta de 88% de branches do plano foi atingida, e o mínimo de 87% no build impede que ela caia sem que alguém
  perceba.

**E o frontend com 19,5%?**
- O número é baixo porque as telas grandes (`App.tsx`, Cadastros, Pedidos, Relatórios, Configurações) não têm teste
  unitário. Elas são exercitadas pelos 6 testes de sistema do Playwright, que não entram nessa medição.
- O que tem lógica de cálculo ou de segurança está coberto por testes unitários: máscaras, conversões API ↔ tela,
  cliente HTTP (token e sessão expirada), login, notificações e menu da conta.
- O fluxo de risco crítico, a venda, é validado de ponta a ponta no navegador.
- **Risco residual:** um erro visual ou de formulário numa tela grande só seria pego pelo teste de sistema, se
  estiver num fluxo coberto, ou pelo uso manual.

**100% de aprovação é suficiente?** Mostra que nada do que é testado falha, mas não que tudo está testado. Por isso
ela é lida junto com a cobertura e com os riscos residuais acima.

## 8.5 Avaliação dos atributos de qualidade (seção 17)

| Atributo | Indicador | Meta | Resultado | Situação |
|---|---|---|---|---|
| Confiabilidade | Taxa de falhas da suíte | 0% | 0% (0 de 575 execuções) | Atingida |
| Segurança | Testes de autenticação e permissão; problemas de segurança na análise estática | 100%; 0 alto | 100% aprovados; 0 problema no SpotBugs | Atingida (SonarCloud pendente) |
| Usabilidade | Taxa de conclusão das tarefas com 3 usuários | ≥ 90% | — | **A medir** |
| Eficiência | p95 do tempo de resposta do dashboard e dos picos | ≤ 2 s | — | **A medir** (D005) |
| Manutenibilidade | Duplicação e problemas estáticos | 0 | 0 duplicação; 0 problema; complexidade 706 | Atingida |
| Portabilidade | Execução em ambientes diferentes | Suíte verde em H2 e PostgreSQL | H2 (Windows e Linux), PostgreSQL 17 (CI), Neon (produção), Chromium (desktop) | Atingida |

## 8.6 Antes × depois (ciclo de melhoria, seção 19)

| Indicador | Antes (28/10/2026) | Depois (04/11/2026) |
|---|---|---|
| Cobertura de instruções (backend) | 89,5% | 90,8% |
| Cobertura de branches (backend) | 81,8% | 88,0% |
| Testes automatizados | 178 (só backend) | 322 (backend 253, frontend 63, sistema 6) |
| Bancos testados | H2 | H2 e PostgreSQL 17 |
| Falhas na suíte | 0 | 0 |
| Defeitos abertos (críticos/altos) | 6 (0/2) | 2 (0/0) |
| Complexidade ciclomática | 727 | 706 |
| Código duplicado | 1 bloco de 43 linhas | 0 |
| Problemas da análise estática | 56 (PMD 1, CPD 1, SpotBugs 53, ESLint 1) | 0 |
| Problemas novos reprovam o PR? | Não | Sim (cobertura mínima e análise estática) |

**As ações de melhoria produziram resultado?** Sim, em todos os indicadores:
- a cobertura de branches subiu 6,2 pontos percentuais;
- a análise estática foi de 56 problemas para 0;
- os defeitos críticos e altos abertos foram de 2 para 0;
- o CI passou a impedir que a qualidade caia de novo.
