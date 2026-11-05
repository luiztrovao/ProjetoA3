# 1. Definição do sistema, requisitos e critérios de aceitação

> Seções 4.1, 4.3 e 5 do enunciado do Projeto Final de Garantia de Qualidade de Software.

## 1.1 Identificação

| Campo | Valor |
|---|---|
| Título | Projeto Integrado de Garantia e Avaliação da Qualidade de Software |
| Disciplina | Garantia de Qualidade de Software |
| Professor | Christian Azonyetin |
| Sistema sob Teste (SUT) | **Ponto & Sabor**, sistema de gerenciamento de lanchonete (repositório `luiztrovao/ProjetoA3`) |
| Integrantes | Luiz Victor, Matheus Santos, Renan Oliveira, Weslly Salles |
| Entrega | até 30/11/2026 |
| Sistema publicado | <https://luiztrovao.github.io/ProjetoA3/> (API em <https://ponto-e-sabor-api.onrender.com>) |

## 1.2 Descrição do sistema

Aplicação web com API REST (Java 21, Spring Boot 4, PostgreSQL) e frontend em React e TypeScript.
Cobre cadastros, vendas no balcão, pedidos, dashboard, relatórios com picos de venda e notificações.

**Arquitetura.** Frontend (GitHub Pages) → API REST com login JWT (Render, Docker) → PostgreSQL (Neon).
O banco é versionado pelas migrações Flyway (V1 a V6). Em desenvolvimento e nos testes a API usa H2 em memória.

## 1.3 Problema que o sistema resolve

Lanchonetes pequenas controlam vendas e estoque em papel ou planilha e não sabem quando vendem mais.
O sistema registra cada venda, baixa o estoque e mostra os **picos de venda** por hora, dia da semana, semana, mês,
semestre e ano, para apoiar decisões de compra e de escala da equipe.

## 1.4 Público-alvo

Donos, gerentes e vendedores de lanchonetes de pequeno porte.

## 1.5 Funcionalidades

- Login com perfis (Dono, Gerente, Vendedor) e permissões por perfil.
- Cadastro de clientes, produtos (com foto) e funcionários.
- Registro de vendas com desconto, troco e formas de pagamento.
- Pedidos: em preparo, concluído e cancelado (com motivo).
- Dashboard, relatórios de período e picos de venda; exportação em CSV.
- Notificações: estoque baixo, pedidos atrasados e pedidos cancelados.
- Configurações da lanchonete (dados, logo, estoque, pagamentos, status inicial da venda, alerta de atraso).

## 1.6 Requisitos funcionais

| ID | Requisito |
|---|---|
| RF01 | Autenticar o funcionário por nome de usuário ou e-mail e senha, devolvendo token JWT válido por 8 horas. |
| RF02 | Controlar o acesso por perfil (Dono, Gerente, Vendedor) conforme a tabela de permissões. |
| RF03 | Cadastrar, editar, buscar, desativar e reativar clientes; só o nome é obrigatório e o CPF, quando informado, é validado e único. |
| RF04 | Cadastrar, editar, buscar, desativar e reativar produtos com código único, categoria, preço maior que zero e estoque. |
| RF05 | Cadastrar, editar, desativar e reativar funcionários, garantindo ao menos um Dono ativo. |
| RF06 | Registrar venda com itens, cliente (opcional conforme configuração), desconto, forma de pagamento e troco em dinheiro. |
| RF07 | Baixar o estoque na confirmação da venda e bloquear a venda sem estoque quando configurado. |
| RF08 | Concluir pedidos em preparo e cancelar pedidos com motivo obrigatório, devolvendo ao estoque o que foi retirado; buscar pedidos por número, cliente, vendedor, status, pagamento e período. |
| RF09 | Exibir dashboard com faturamento, pedidos e ticket médio de hoje e dos últimos 7 dias. |
| RF10 | Gerar relatório de período e picos de venda por semana, mês, semestre e ano, por hora e por dia da semana. |
| RF11 | Emitir notificações de estoque baixo, pedidos atrasados e pedidos cancelados, conforme o perfil. |
| RF12 | Manter configurações da lanchonete (dados, logo, estoque, pagamentos, status inicial da venda, alerta de atraso). |
| RF13 | Enviar logo e fotos de produto em PNG ou JPG de até 1 MB, verificando o formato pelo conteúdo. |
| RF14 | Permitir ao funcionário editar os próprios dados e trocar a senha (Minha Conta). |

## 1.7 Requisitos não funcionais

| ID | Atributo | Requisito verificável |
|---|---|---|
| RNF01 | Segurança | Senhas guardadas com BCrypt e nunca devolvidas pela API; rotas protegidas exigem token JWT assinado com segredo de 32 caracteres ou mais. |
| RNF02 | Confiabilidade | Valores monetários em `NUMERIC(10,2)` / `BigDecimal`; o estoque nunca fica negativo. |
| RNF03 | Usabilidade | Erros sempre no mesmo formato JSON, com mensagem em português e o campo inválido; 90% ou mais das tarefas principais concluídas por usuários sem ajuda. |
| RNF04 | Eficiência | 95% das chamadas a `/api/dashboard` e `/api/relatorios/picos` respondem em até 2 s com 13 mil vendas na base. |
| RNF05 | Manutenibilidade | Banco alterado só por novas migrações; o CI executa build, testes, cobertura mínima e análise estática em todo PR. |
| RNF06 | Portabilidade | A API roda em H2 (local) e PostgreSQL (produção) sem mudar código; o frontend funciona em desktop e celular. |
| RNF07 | Custo | Hospedagem apenas em planos gratuitos (Neon, Render, GitHub Pages). |

## 1.8 Restrições

- Planos gratuitos: a API no Render "dorme" depois de 15 minutos sem uso e leva cerca de 50 s na primeira chamada.
- Imagens guardadas no próprio banco, limitadas a 1 MB.
- Permissões fixas por perfil, sem configuração pelo usuário.
- Prazo de entrega: 30/11/2026.

## 1.9 Riscos associados ao sistema

- Venda registrada com total, desconto ou troco errado (prejuízo financeiro direto).
- Estoque inconsistente depois de cancelamento ou de venda sem estoque.
- Vendedor acessando funções de Dono ou Gerente (desconto, cancelamento, relatórios).
- Vazamento de dados pessoais de clientes e funcionários (CPF, telefone, e-mail).
- Dashboards com números errados levando a decisões ruins de compra e de escala.

A classificação desses riscos e a intensidade de teste de cada funcionalidade estão no
[plano de testes](03-plano-de-testes.md#31-estratégia-baseada-em-risco).

## 1.10 Critérios de aceitação

Cada requisito importante tem critérios objetivos, numerados para a rastreabilidade
**Requisito → Caso de teste → Resultado → Defeito** (veja o [catálogo de casos de teste](04-catalogo-de-casos-de-teste.md)).

| Requisito | ID | Critério |
|---|---|---|
| RF01 Login | CA-01 | Login por nome de usuário ou e-mail com senha correta devolve token. |
| RF01 Login | CA-02 | Senha errada ou usuário inexistente devolvem 401 com a mesma mensagem genérica. |
| RF01 Login | CA-03 | Usuário inativo não entra, nem com token já emitido. |
| RF02 Permissões | CA-04 | Vendedor recebe 403 ao editar cliente, gerenciar produto, ver funcionários ou relatórios. |
| RF02 Permissões | CA-05 | Mudança de perfil vale na hora para o token já emitido. |
| RF03 Clientes | CA-06 | Cliente só com nome é aceito. |
| RF03 Clientes | CA-07 | CPF inválido ou duplicado é rejeitado com a mensagem no campo `cpf`. |
| RF03 Clientes | CA-08 | CPF e telefone com máscara são guardados só com dígitos. |
| RF04 Produtos | CA-09 | Preço zero ou negativo e código duplicado são rejeitados. |
| RF04 Produtos | CA-10 | Produto com estoque igual ou abaixo do mínimo aparece como estoque baixo. |
| RF05 Funcionários | CA-11 | Não é possível inativar nem rebaixar o último Dono ativo. |
| RF05 Funcionários | CA-12 | Ninguém desativa a própria conta nem altera o próprio perfil. |
| RF06 Venda | CA-13 | Total = subtotal − desconto; o desconto não pode ser negativo nem maior que o subtotal. |
| RF06 Venda | CA-14 | Em dinheiro, valor recebido menor que o total é rejeitado e o troco é calculado. |
| RF06 Venda | CA-15 | Vendedor não pode dar desconto; Gerente e Dono podem. |
| RF07 Estoque | CA-16 | Com bloqueio ligado, venda acima do estoque é recusada; sem bloqueio, baixa só o disponível e o estoque nunca fica negativo. |
| RF08 Pedidos | CA-17 | Cancelar exige motivo e devolve ao estoque exatamente o que foi retirado. |
| RF08 Pedidos | CA-18 | Só pedido em preparo pode ser concluído; pedido não é cancelado duas vezes. |
| RF08 Pedidos | CA-19 | A busca encontra o pedido pelo número (com ou sem a máscara `#PV-`), pelo nome do cliente ou do vendedor, e o período inclui o dia inteiro do início e do fim. |
| RF10 Picos | CA-20 | Período sem vendas não tem pico; empate no pico fica com o primeiro. |
| RF11 Notificações | CA-21 | Gestão vê estoque baixo e todos os pedidos atrasados; o Vendedor vê só os próprios pedidos atrasados e cancelados. |
| RF13 Imagens | CA-22 | Arquivo que não é PNG/JPG pelo conteúdo, ou acima de 1 MB, é recusado. |

## 1.11 Validação dos requisitos

Antes de testar o código, a equipe revisou os próprios requisitos (README e modelo de dados originais).
A revisão encontrou 5 problemas; todos foram reescritos de forma verificável nas seções 1.6, 1.7 e 1.10.

| Requisito original | Problema | Reescrita verificável |
|---|---|---|
| "Dashboards estratégicos de picos de venda" | Subjetivo: "estratégico" não é mensurável | RF10 + CA-20: períodos, agrupamentos e regra de empate definidos |
| Desempenho dos dashboards (não documentado) | Ausente: nenhuma meta de tempo | RNF04: p95 ≤ 2 s com 13 mil vendas na base |
| "Pedido atrasado" | Incompleto: não dizia depois de quanto tempo | RF11 + RF12: `minutosAlertaPreparo`, padrão 20, entre 5 e 240 min |
| "Estoque baixo" | Ambíguo: abaixo ou igual ao mínimo? E sem mínimo? | CA-10: igual ou abaixo do mínimo; sem mínimo usa o alerta das configurações (padrão 5) |
| Senha mínima de 8 vs. 6 caracteres | Contraditório entre o modelo inicial e o protótipo | Decidido pela equipe: 6 a 72 caracteres (histórico de decisões do [modelo de dados](modelagem/modelo-de-dados.md)) |

O modelo de dados completo (tabelas, relacionamentos e regras) está em [modelagem/modelo-de-dados.md](modelagem/modelo-de-dados.md).
