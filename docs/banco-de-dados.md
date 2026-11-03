# Banco de dados: conexão pelo SGBD e consultas

O banco do sistema publicado é um **PostgreSQL no Neon** (plano gratuito, região AWS US East 1, N. Virginia).
As tabelas são criadas pelo **Flyway** a partir das migrações em `backend/src/main/resources/db/migration`
(V1 a V6); o modelo está em [modelagem/modelo-de-dados.md](modelagem/modelo-de-dados.md).

> **A senha não fica no repositório** (ele é público). Ela está no painel do Neon (**Connect → Show password**)
> e na variável `DB_PASSWORD` do Render. Peça a quem administra o projeto e não a cole em chats ou prints.
>
> **Cuidado:** este é o banco do site publicado. `UPDATE`, `DELETE` e `DROP` alteram os dados de verdade.
> Para demonstrar uma alteração sem deixá-la gravada, use `BEGIN; ... ROLLBACK;` (exemplo 8).

## Dados da conexão

| Campo | Valor |
|---|---|
| Host | `ep-bitter-frog-bbeh9lqu.c-15.us-east-1.aws.neon.tech` |
| Porta | `5432` |
| Banco | `neondb` |
| Usuário | `neondb_owner` |
| Senha | no painel do Neon (veja o aviso acima) |
| SSL | obrigatório (`sslmode=require`) |
| URL JDBC (usada pela API) | `jdbc:postgresql://ep-bitter-frog-bbeh9lqu.c-15.us-east-1.aws.neon.tech/neondb?sslmode=require` |

Use sempre a conexão **direta** (no Neon, *Connection pooling* desligado).

## DBeaver (recomendado)

1. Baixe o **DBeaver Community** em <https://dbeaver.io/download/>.
2. **Nova conexão** (ícone de tomada com +) → **PostgreSQL** → Próximo.
3. Aba **Principal**: Host, Porta, Banco, Usuário e Senha da tabela acima; autenticação *Database Native*.
4. Aba **SSL**: marque **Usar SSL** e escolha o modo `require`.
5. **Testar conexão** (aceite baixar o driver, se pedir) → **Concluir**.
6. As tabelas ficam em `neondb → Schemas → public → Tables`.
7. **Diagrama ER:** botão direito em `public` → **Visualizar diagrama**.

## Outras opções

- **pgAdmin** (<https://www.pgadmin.org/download/>): **Register → Server**. Na aba *Connection*, use os mesmos dados;
  na aba *Parameters*, defina **SSL mode = require**.
- **SQL Editor do Neon:** no painel do projeto, menu **SQL Editor**. Funciona no navegador, sem instalar nada.
- **Linha de comando (psql):**

  ```bash
  psql "postgresql://neondb_owner@ep-bitter-frog-bbeh9lqu.c-15.us-east-1.aws.neon.tech/neondb?sslmode=require"
  ```

  O psql pede a senha na hora.

## Primeiro acesso do dia

O Neon gratuito suspende o banco quando fica ocioso e acorda em poucos segundos na primeira conexão.
A API no Render "dorme" depois de 15 minutos sem uso e leva cerca de 50 segundos para responder de novo.
Antes de apresentar, abra o site e rode uma consulta para acordar os dois.

## Consultas para a apresentação

Os status de venda são `EM_PREPARO`, `CONCLUIDA` e `CANCELADA`; as formas de pagamento, `DINHEIRO`, `CREDITO`,
`DEBITO` e `PIX`. Os relatórios do sistema contam só as vendas **não canceladas**.

### 1. Buscar vendas por filtros (o que a tela Pedidos faz)

```sql
SELECT v.id,
       '#PV-' || LPAD(v.id::text, 6, '0') AS numero,
       v.data_hora, c.nome AS cliente, u.nome AS vendedor,
       v.forma_pagamento, v.status, v.total
FROM venda v
LEFT JOIN cliente c ON c.id = v.cliente_id
JOIN usuario u      ON u.id = v.usuario_id
WHERE v.status <> 'CANCELADA'
  AND v.forma_pagamento = 'PIX'
  AND v.data_hora >= CURRENT_DATE - INTERVAL '7 days'
ORDER BY v.data_hora DESC
LIMIT 20;
```

`LEFT JOIN` no cliente porque a venda de balcão pode não ter cliente identificado.

### 2. Pico de vendas por hora do dia

```sql
SELECT EXTRACT(HOUR FROM data_hora) AS hora,
       COUNT(*) AS pedidos, SUM(total) AS faturamento
FROM venda
WHERE status <> 'CANCELADA'
GROUP BY 1
ORDER BY faturamento DESC;
```

### 3. Pico por dia da semana (1 = segunda … 7 = domingo)

```sql
SELECT EXTRACT(ISODOW FROM data_hora) AS dia_semana,
       COUNT(*) AS pedidos, SUM(total) AS faturamento
FROM venda
WHERE status <> 'CANCELADA'
GROUP BY 1
ORDER BY 1;
```

### 4. Faturamento por mês, trimestre ou ano

```sql
SELECT DATE_TRUNC('month', data_hora) AS mes,
       COUNT(*) AS pedidos, SUM(total) AS faturamento,
       ROUND(AVG(total), 2) AS ticket_medio
FROM venda
WHERE status <> 'CANCELADA'
GROUP BY 1
ORDER BY 1;
```

Troque `'month'` por `'quarter'` (trimestre) ou `'year'` (ano). Para semestre:

```sql
SELECT EXTRACT(YEAR FROM data_hora) AS ano,
       CASE WHEN EXTRACT(MONTH FROM data_hora) <= 6 THEN 1 ELSE 2 END AS semestre,
       COUNT(*) AS pedidos, SUM(total) AS faturamento
FROM venda
WHERE status <> 'CANCELADA'
GROUP BY 1, 2
ORDER BY 1, 2;
```

### 5. Os 5 produtos mais vendidos (JOIN de três tabelas)

```sql
SELECT p.codigo, p.nome, SUM(i.quantidade) AS unidades, SUM(i.subtotal) AS faturamento
FROM item_venda i
JOIN produto p ON p.id = i.produto_id
JOIN venda v   ON v.id = i.venda_id
WHERE v.status <> 'CANCELADA'
GROUP BY p.codigo, p.nome
ORDER BY unidades DESC
LIMIT 5;
```

### 6. Ranking de vendedores (função de janela)

```sql
SELECT u.nome, COUNT(*) AS pedidos, SUM(v.total) AS faturamento,
       RANK() OVER (ORDER BY SUM(v.total) DESC) AS posicao
FROM venda v
JOIN usuario u ON u.id = v.usuario_id
WHERE v.status <> 'CANCELADA'
GROUP BY u.nome;
```

### 7. Produtos com estoque baixo (o mesmo critério do sino de notificações)

```sql
SELECT codigo, nome, estoque_atual, estoque_minimo
FROM produto
WHERE ativo AND estoque_atual <= estoque_minimo
ORDER BY estoque_atual;
```

### 8. O banco barrando dado inválido (restrição CHECK)

```sql
BEGIN;
UPDATE usuario SET perfil = 'ADMIN' WHERE username = 'fernanda';
-- erro: o novo valor viola a restrição "ck_usuario_perfil"
ROLLBACK;
```

O `ROLLBACK` desfaz a transação, então nada fica gravado. Outras restrições para mostrar:
`ck_configuracao_minutos_alerta` (alerta de atraso entre 5 e 240 minutos) e as chaves estrangeiras de
`venda`, `item_venda` e `produto`.

### 9. Uso de índice

```sql
EXPLAIN ANALYZE
SELECT COUNT(*) FROM venda
WHERE confirmada_em >= CURRENT_DATE - INTERVAL '30 days';
```

O plano mostra se o PostgreSQL usou o índice `idx_venda_confirmada_em` (criado na migração V5).

### 10. Histórico de evolução do esquema (Flyway)

```sql
SELECT version, description, installed_on
FROM flyway_schema_history
ORDER BY installed_rank;
```

## Demonstração ao vivo: sistema e SGBD juntos

1. No site, entre como Gerente (`carlos`) e cancele um pedido em **Pedidos**, informando o motivo.
2. No SGBD, mostre a linha mudando na hora:

   ```sql
   SELECT id, status, cancelada_em, motivo_cancelamento
   FROM venda
   ORDER BY cancelada_em DESC NULLS LAST
   LIMIT 5;
   ```

3. Se o controle de estoque estiver ligado, mostre também que o estoque voltou:

   ```sql
   SELECT p.nome, p.estoque_atual, i.quantidade_baixada_estoque
   FROM item_venda i
   JOIN produto p ON p.id = i.produto_id
   WHERE i.venda_id = :id_do_pedido_cancelado;
   ```

   O DBeaver pergunta o valor de `:id_do_pedido_cancelado` ao executar. Depois do cancelamento,
   `quantidade_baixada_estoque` fica `0`, porque o que tinha saído do estoque foi devolvido.
