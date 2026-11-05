# 9. Relatório técnico final

> Consolida o processo de garantia da qualidade aplicado ao **Ponto & Sabor** e responde à pergunta do enunciado:
> *"Como podemos demonstrar, por meio de evidências, que este software atende aos requisitos e apresenta um nível
> aceitável de qualidade?"*

## 9.1 Resumo

O Ponto & Sabor é um sistema web de gestão de lanchonete (Java 21 + Spring Boot, React + TypeScript, PostgreSQL),
publicado sem custo em <https://luiztrovao.github.io/ProjetoA3/>. Sobre ele foi aplicado o ciclo completo do
enunciado:

```
Requisitos → Plano de qualidade → Critérios de aceitação → Plano de testes → Revisão técnica → Análise estática →
Casos de teste → Caixa-preta → Caixa-branca → Unitário → Integração → Sistema → Aceitação → Regressão →
Automação → Métricas → Análise → Defeitos → Correções → Retestes → Relatório final
```

**Resultado:** os 22 critérios de aceitação têm caso de teste aprovado; 322 testes automatizados (575 execuções, nos
bancos H2 e PostgreSQL) passam a 100%; a análise estática foi de 56 problemas para 0; a cobertura de branches do
backend subiu de 81,8% para 88,0%; e nenhum defeito crítico ou alto está aberto. Os critérios de saída do plano de
qualidade foram atendidos, com duas exceções declaradas: a medição de desempenho (D005) e o teste de usabilidade.

## 9.2 Evidências, entregável por entregável

| Etapa do enunciado | Evidência | Documento |
|---|---|---|
| Requisitos (14 RF, 7 RNF), restrições e riscos | Requisitos verificáveis e validados | [01-requisitos.md](01-requisitos.md) |
| Critérios de aceitação | CA-01 a CA-22 | [01-requisitos.md](01-requisitos.md#110-critérios-de-aceitação) |
| Validação dos requisitos | 5 requisitos ruins reescritos | [01-requisitos.md](01-requisitos.md#111-validação-dos-requisitos) |
| Plano de qualidade | Objetivos, atributos, critérios de entrada, saída e aprovação | [02-plano-de-qualidade.md](02-plano-de-qualidade.md) |
| Plano de testes e estratégia por risco | Matriz de risco, técnicas, níveis e ambientes | [03-plano-de-testes.md](03-plano-de-testes.md) |
| Casos de teste | 60 casos com requisito, entrada, esperado, obtido e evidência | [04-catalogo-de-casos-de-teste.md](04-catalogo-de-casos-de-teste.md) |
| Revisão técnica e defeitos | 9 defeitos com ciclo completo, 7 corrigidos | [05-relatorio-de-defeitos.md](05-relatorio-de-defeitos.md) |
| Análise estática | 56 → 0 problemas, com justificativa dos falsos positivos | [06-relatorio-de-analise-estatica.md](06-relatorio-de-analise-estatica.md) |
| Testes automatizados e CI | 575 execuções, 0 falha, seis jobs no pipeline | [07-relatorio-de-testes-automatizados.md](07-relatorio-de-testes-automatizados.md) |
| Métricas e atributos | Testes, defeitos, código e atributos, com interpretação | [08-relatorio-de-metricas.md](08-relatorio-de-metricas.md) |
| Relatórios das ferramentas | Saídas reais do CI | [/reports](../reports/README.md) |

## 9.3 Análise dos resultados

1. **Os requisitos foram verificados, não só implementados.** Cada critério de aceitação tem um teste automatizado
   que falharia se a regra fosse quebrada. O roteiro de demonstração mostra isso ao vivo, alterando o limite do
   desconto e vendo o teste ficar vermelho.
2. **O esforço de teste seguiu o risco.** Venda e permissões, de risco crítico, concentram 79 dos 253 testes do
   backend e são os únicos fluxos validados de ponta a ponta no navegador.
3. **A regressão funcionou na prática.** A refatoração que zerou o código duplicado quebrou a busca de clientes (D008).
   A suíte pegou o problema antes do merge, e ele nunca chegou à `develop`.
4. **Testar no banco de produção era necessário.** A suíte passou a rodar também no PostgreSQL 17 (D006). Ela passou
   de primeira, o que agora é uma evidência medida, e não mais uma suposição.
5. **A qualidade ficou protegida.** Cobertura mínima e análise estática reprovam o PR automaticamente; os números
   deste relatório não podem piorar sem que o CI avise.

## 9.4 Limitações e riscos residuais

| Limitação | Risco | Plano |
|---|---|---|
| Desempenho não medido (D005) | Dashboard lento com mais vendas | Medir o p95 com 13 mil vendas (RNF04) |
| Usabilidade não medida | Tarefas difíceis para usuários reais | Teste com 3 pessoas, medindo a taxa de conclusão |
| Cobertura do frontend de 19,5% | Erro visual em tela grande sem fluxo de sistema | Testes de componente para Cadastros, Pedidos e Relatórios |
| H2 2.4 instável depois de hibernação (D007) | Só no desenvolvimento local | Fixar a versão do H2 ou usar PostgreSQL local |
| Plano gratuito do Render "dorme" | Primeira chamada leva cerca de 50 s | Abrir o sistema antes do uso; aceito pela restrição de custo |

## 9.5 Segurança, privacidade e ética (seção 18)

**Dados tratados e coleta.** Nome, CPF, telefone, e-mail e endereço de clientes e funcionários são dados pessoais
protegidos pela LGPD. Só o nome do cliente é obrigatório, o que reduz a coleta ao mínimo necessário para a venda.

**Armazenamento e proteção.**
- Senhas guardadas com BCrypt e nunca devolvidas pela API.
- Token JWT assinado com segredo de 32 caracteres ou mais, guardado em variável de ambiente.
- Credenciais do banco fora do código; repositório público sem nenhum segredo.
- Conexão ao banco com SSL obrigatório.
- O Vendedor não vê funcionários nem relatórios.
- O login usa mensagem genérica, para não revelar quais usuários existem.
- Os dados de demonstração são fictícios e gerados com semente fixa.

**Exposição de informações.** A análise estática eliminou toda exposição de estado interno (`EI_EXPOSE_REP`): listas e
imagens são copiadas, para que um componente não altere dados de outro.

**Responsabilidade profissional.** Durante a configuração da publicação, a senha do banco e o segredo do JWT
apareceram numa captura de tela. A recomendação registrada foi trocar as duas credenciais. Publicar um sistema com
dados pessoais exige esse cuidado com credenciais, mesmo num trabalho acadêmico.

**Riscos de discriminação ou de decisões injustas.**
- O relatório de desempenho por vendedor pode levar a uma cobrança injusta se for lido sem contexto (turno, dia da
  semana, tipo de pedido). Ele deve apoiar decisões, nunca ser o único critério para avaliar uma pessoa.
- A exclusão lógica mantém o CPF guardado depois da desativação, e ainda falta um caminho para atender um pedido de
  eliminação de dados (direito do titular na LGPD).

**Quais consequências poderiam ocorrer se este software apresentasse uma falha grave?**
- **Total ou troco errado:** prejuízo direto e conflito com o cliente no balcão.
- **Falha de permissão:** um vendedor poderia dar descontos ou cancelar vendas sem autorização.
- **Estoque errado:** venda do que não existe, ou compra desnecessária de insumos.
- **Vazamento de dados:** CPF, telefone e endereço de clientes expostos, com responsabilidade legal da lanchonete.
- **Dashboard errado:** escala de funcionários e compras planejadas sobre números falsos.

Esses cenários são exatamente as funcionalidades classificadas como de risco crítico e alto, e por isso as mais
testadas.

## 9.6 Melhoria contínua (seção 19)

O ciclo **versão inicial → testes → defeitos → métricas → análise → correções → nova execução → comparação** foi
executado de 28/10 a 04/11/2026, em PRs revisados e com o CI como juiz:

| Etapa | O que foi feito | PR |
|---|---|---|
| Versão inicial e medição | 178 testes, 89,5% / 81,8% de cobertura, 56 problemas estáticos, 6 defeitos | — |
| Análise estática | PMD, CPD, SpotBugs, ESLint e SonarCloud no CI | #17 |
| Novos testes (caixa-preta e caixa-branca) | 62 testes de valor-limite e branches de JWT, login e funcionários | #18 |
| Correções estáticas | 56 → 0, build reprovando problemas novos | #19 |
| Testes do frontend e de sistema | Vitest (63) e Playwright (6) | #20 |
| Cobertura mínima e PostgreSQL | Filtros de pedidos cobertos, `jacoco:check`, suíte no PostgreSQL 17 | #22 |
| Nova execução e comparação | Tabela antes × depois | [08-relatorio-de-metricas.md](08-relatorio-de-metricas.md#86-antes--depois-ciclo-de-melhoria-seção-19) |

## 9.7 Conclusão

Com base nas evidências deste relatório, todas reproduzíveis com `./mvnw verify`, `npm test` e `npm run test:e2e`, ou
consultáveis nos artefatos do CI:
- o Ponto & Sabor atende aos seus requisitos funcionais;
- ele apresenta um **nível aceitável de qualidade** para os atributos de confiabilidade, segurança, manutenibilidade
  e portabilidade.

Eficiência e usabilidade têm meta definida e medição pendente, registradas como próximos passos.
