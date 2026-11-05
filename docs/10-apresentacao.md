# 10. Apresentação e demonstração prática

> Seção 25 do enunciado: tudo o que for apresentado precisa ser reproduzido ao vivo. Este roteiro lista o que deixar
> pronto e como responder a cada pedido do professor.

## 10.1 Antes de começar (10 minutos antes)

- [ ] Abrir <https://luiztrovao.github.io/ProjetoA3/> para "acordar" a API no Render (cerca de 50 s na primeira chamada).
- [ ] Na pasta `backend/`, rodar `./mvnw verify` uma vez, para gerar o JaCoCo e a análise estática frescos.
- [ ] Deixar abertos:
  - o JaCoCo: `backend/target/site/jacoco/index.html`;
  - o relatório do PMD: `backend/target/reports/pmd.html`;
  - a última execução do CI no GitHub (aba Actions).
- [ ] Ter a senha dos usuários de demonstração em mãos (Dono `matheus`, Gerente `carlos`, Vendedor `fernanda`).
- [ ] Para a parte de banco de dados, deixar o DBeaver conectado ([banco-de-dados.md](banco-de-dados.md)).

## 10.2 Roteiro sugerido (15 a 20 minutos)

1. **O sistema (2 min).** Problema, público e uma venda ao vivo no site publicado.
2. **Requisitos e validação (2 min).** Um requisito ruim reescrito (ex.: "dashboards estratégicos" → RF10 + CA-20).
3. **Risco (2 min).** A matriz de risco e por que a venda tem mais testes que o upload de imagem.
4. **Caixa-preta (2 min).** `LimitesValidacaoTest`, com os valores 5, 6, 7, 71, 72 e 73 da senha.
5. **Caixa-branca (2 min).** JaCoCo aberto em `VendaRepository.Filtros` (22 de 22 branches) e a tabela de decisão de `Venda.confirmar`.
6. **Automação e CI (2 min).** `./mvnw verify` rodando e o pipeline com seis jobs no GitHub.
7. **Análise estática (2 min).** 56 → 0 problemas e um exemplo de correção (`Contato`) e de falso positivo (injeção do Spring).
8. **Defeitos (2 min).** O ciclo completo do D008: a regressão pegando a refatoração antes do merge.
9. **Métricas e antes × depois (2 min).** A tabela da seção 8.6 do relatório de métricas.
10. **Ética (1 min).** Dados pessoais e a consequência de uma falha grave na venda.

## 10.3 Respostas prontas para os pedidos do professor

| Pedido | Comando ou ação |
|---|---|
| Executar um teste | `./mvnw test -Dtest=VendaTest` |
| Criar um novo caso de teste | Em `LimitesValidacaoTest`, acrescentar `"4, false"` (senha de 4 caracteres) ao `@CsvSource` de `senhaDe6A72Caracteres` e rodar `./mvnw test -Dtest=LimitesValidacaoTest` |
| Alterar um valor | Em `Venda.aplicarDesconto`, comentar o `if` que recusa desconto maior que o subtotal |
| Provocar uma falha | Depois da alteração acima, `./mvnw test -Dtest=VendaTest` mostra `descontoNaoPodePassarDoSubtotalNemSerNegativo` vermelho; desfazer com `git checkout -- backend/src/main/java/com/pontoesabor/lanchonete/model/Venda.java` |
| Executar a suíte automatizada | `./mvnw verify` (backend) e `npm test` (frontend) |
| Mostrar a cobertura | `backend/target/site/jacoco/index.html`, descendo até um pacote e uma classe |
| Localizar um defeito | [05-relatorio-de-defeitos.md](05-relatorio-de-defeitos.md): D008, com o teste que o encontrou |
| Explicar uma métrica | Taxa de aprovação de 100% e por que ela não basta sozinha ([08-relatorio-de-metricas.md](08-relatorio-de-metricas.md#84-interpretação)) |
| Justificar um caso de teste | CT036 (venda acima do estoque): regra R3 da tabela de decisão, risco crítico |
| Demonstrar um teste caixa-preta | `LimitesValidacaoTest` (valor-limite) |
| Demonstrar um teste caixa-branca | `VendaFiltrosTest` contra os branches de `VendaRepository.Filtros` |
| Mostrar a análise estática | `backend/target/reports/pmd.html`, `spotbugsXml.xml` ou o artefato `relatorios-backend` do CI |
| Explicar por que uma funcionalidade tem maior risco | Venda: probabilidade alta (usada o dia todo) × impacto alto (dinheiro, estoque e permissão) |

## 10.4 Divisão sugerida entre os integrantes

| Parte | Integrante |
|---|---|
| Sistema, requisitos e risco (1–3) | *(a definir)* |
| Caixa-preta, caixa-branca e automação (4–6) | *(a definir)* |
| Análise estática, defeitos e métricas (7–9) | *(a definir)* |
| Ética e perguntas (10) | *(a definir)* |
