# Backend — lanchonete-api

API REST em **Java 21 + Spring Boot 4**, com Spring Data JPA, Flyway e PostgreSQL.

## Pré-requisitos

- **JDK 21** (ex.: [Eclipse Temurin](https://adoptium.net/)). O Maven **não** precisa ser instalado: use o `mvnw` do projeto.

## Como rodar

Na pasta `backend/`:

```bash
./mvnw spring-boot:run
```

No Windows (PowerShell/CMD): `.\mvnw.cmd spring-boot:run`

Por padrão a API sobe com um **banco H2 em memória** (perfil `h2`), sem instalar nada:

| O quê | Endereço |
|---|---|
| API | http://localhost:8080 |
| Swagger (documentação da API) | http://localhost:8080/swagger-ui.html |
| Console do banco H2 | http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:lanchonete`, usuário `sa`, sem senha) |

### Login

Quase todas as rotas exigem login. No perfil `h2` a API já sobe com os **funcionários de demonstração
do protótipo**: `matheus` (Dono), `carlos` (Gerente) e `fernanda` (Vendedor). A senha de todos está em
`app.dados-demo.senha`, no arquivo `src/main/resources/application-h2.properties`.

1. `POST /api/auth/login` com `{"login": "matheus", "senha": "..."}` (o login aceita nome de usuário ou e-mail).
2. Copie o `token` da resposta.
3. Envie o token em todas as requisições: `Authorization: Bearer <token>`. No Swagger, clique em **Authorize**
   e cole o token.

### Dados de demonstração (apresentação)

No perfil `h2`, ao subir com o banco vazio, a API cria uma lanchonete fictícia completa:

- **Equipe:** `matheus` (Dono), `carlos` (Gerente), `fernanda`, `julia` e `bruno` (Vendedores) e `rafael` (desligado)
- **Cardápio** com 16 itens (hambúrgueres, bebidas, acompanhamentos, combos e sobremesas) e **20 clientes**
- **Cerca de 13 mil vendas nos últimos 18 meses**, com padrões realistas para os dashboards: picos no **almoço (12h)**
  e no **jantar (19h–20h)**, **sexta e sábado** mais fortes, **julho e dezembro** em alta, crescimento leve mês a mês,
  ~3% de cancelamentos com motivo e os pedidos mais recentes de hoje "em preparo"

A geração usa uma **semente fixa**: os números são sempre os mesmos (bom para ensaiar a apresentação). O histórico não
consome o estoque atual dos produtos. Para usar no PostgreSQL (banco vazio), defina `APP_DADOS_DEMO_HABILITADO=true` e
`APP_DADOS_DEMO_SENHA=<senha>`; `APP_DADOS_DEMO_MESES_DE_HISTORICO` muda o tamanho do histórico (padrão 18).

O token vale **8 horas**. Se o funcionário for desativado ou mudar de perfil, a mudança vale na hora.

### Usando o PostgreSQL (Neon)

Ative o perfil `prod` e informe a conexão por variáveis de ambiente (nunca no código):

```bash
SPRING_PROFILES_ACTIVE=prod DB_URL=jdbc:postgresql://<host>/<banco>?sslmode=require DB_USERNAME=<usuario> DB_PASSWORD=<senha> JWT_SECRET=<segredo> ADMIN_USERNAME=<usuario> ADMIN_SENHA=<senha> ./mvnw spring-boot:run
```

| Variável | Para que serve |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexão com o PostgreSQL |
| `JWT_SECRET` | Segredo que assina os tokens (**obrigatório**, mínimo de 32 caracteres, aleatório) |
| `ADMIN_USERNAME`, `ADMIN_SENHA` | Primeiro usuário **Dono**, criado só quando o banco não tem nenhum usuário |
| `CORS_ORIGENS` | Endereços do frontend que podem chamar a API (padrão `http://localhost:5173`) |

## Endpoints

A documentação completa e interativa fica no Swagger. Resumo:

| Recurso | Endpoints |
|---|---|
| Login e Minha Conta | `POST /api/auth/login` · `GET /api/conta` · `PUT /api/conta` · `PUT /api/conta/senha` |
| Categorias | `GET /api/categorias` |
| Produtos | `GET /api/produtos` · `GET /api/produtos/{id}` · `POST /api/produtos` · `PUT /api/produtos/{id}` · `DELETE /api/produtos/{id}` · `PATCH /api/produtos/{id}/ativar` · `PUT /api/produtos/{id}/imagem` · `DELETE /api/produtos/{id}/imagem` |
| Clientes | `GET /api/clientes` · `GET /api/clientes/{id}` · `POST /api/clientes` · `PUT /api/clientes/{id}` · `DELETE /api/clientes/{id}` · `PATCH /api/clientes/{id}/ativar` |
| Usuários | `GET /api/usuarios` · `GET /api/usuarios/{id}` · `POST /api/usuarios` · `PUT /api/usuarios/{id}` · `DELETE /api/usuarios/{id}` · `PATCH /api/usuarios/{id}/ativar` |
| Vendas e Pedidos | `GET /api/vendas` · `GET /api/vendas/{id}` · `POST /api/vendas` · `PATCH /api/vendas/{id}/concluir` · `PATCH /api/vendas/{id}/cancelar` |
| Dashboard e Relatórios | `GET /api/dashboard` · `GET /api/relatorios/resumo` · `GET /api/relatorios/picos` |
| Notificações (sino) | `GET /api/notificacoes` · `POST /api/notificacoes/lidas` |
| Configurações | `GET /api/configuracoes` · `PUT /api/configuracoes/empresa` · `PUT /api/configuracoes/vendas` · `PUT /api/configuracoes/logo` · `DELETE /api/configuracoes/logo` |
| Público (sem login) | `GET /api/publico/identidade` (nome e logo para a tela de login) · `GET /api/imagens/{id}` |

**Permissões por perfil** (tabela do protótipo; detalhes em [modelo-de-dados.md](../docs/modelagem/modelo-de-dados.md#permissões-por-perfil)):

| Rota | Dono | Gerente | Vendedor |
|---|:---:|:---:|:---:|
| Consultar clientes, produtos, categorias e configurações; cadastrar cliente | ✅ | ✅ | ✅ |
| Editar, desativar e reativar clientes | ✅ | ✅ | ❌ |
| Cadastrar, editar, desativar e reativar produtos | ✅ | ✅ | ❌ |
| Consultar funcionários | ✅ | ✅ | ❌ |
| Cadastrar, editar, desativar e reativar funcionários | ✅ | ❌ | ❌ |
| Alterar configurações | ✅ | ❌ | ❌ |
| Confirmar vendas (sem desconto) | ✅ | ✅ | ✅ |
| Dar desconto na venda | ✅ | ✅ | ❌ |
| Consultar pedidos | todos | todos | só os próprios |
| Concluir e cancelar pedidos | ✅ | ✅ | ❌ |
| Dashboard e Relatórios | ✅ | ✅ | ❌ |
| Minha Conta | ✅ | ✅ | ✅ |
| Notificações | estoque baixo e pedidos atrasados | estoque baixo e pedidos atrasados | os próprios pedidos atrasados e cancelados |

**Convenções:**

- `DELETE` faz **exclusão lógica** (marca como inativo); `PATCH .../ativar` desfaz.
- Listagens aceitam `termo`, `ativo` (`true`/`false`), `page` (começa em 0), `size` e `sort` (ex.: `sort=preco,desc`), e devolvem `{conteudo, pagina, tamanho, totalElementos, totalPaginas}`.
- Filtros extras: `categoriaId` e `estoqueBaixo=true` em produtos e `perfil` em usuários. O `termo` busca:
  - produtos: nome ou código;
  - clientes: nome, CPF, telefone ou e-mail;
  - usuários: nome, nome de usuário, e-mail ou CPF.
- CPF, CNPJ, telefone e CEP podem ser enviados com ou sem máscara; a API guarda só os dígitos.
- Código de produto e UF são guardados em maiúsculas; nome de usuário e e-mail, em minúsculas.
- Produto sem `estoqueMinimo` usa o "alerta de estoque baixo" das configurações.
- **Vendas** (`POST /api/vendas`): `{clienteId?, itens: [{produtoId, quantidade}], desconto?, formaPagamento, valorRecebido?, observacao?}`.
  A confirmação segue as **Preferências de Vendas**: formas de pagamento habilitadas, cliente obrigatório ou não,
  controle de estoque (bloqueando ou não a venda sem estoque) e status inicial (`EM_PREPARO` ou `CONCLUIDA`).
  Em dinheiro, `valorRecebido` deve cobrir o total e a resposta traz o `troco`. O pedido tem um `numero` no formato `#PV-000123`.
- **Dashboard e Relatórios** contam só vendas **não canceladas**:
  - `GET /api/dashboard`: faturamento, pedidos e ticket médio de hoje; produtos (cadastrados, ativos, estoque baixo);
    faturamento dia a dia dos últimos 7 dias; 5 pedidos mais recentes.
  - `GET /api/relatorios/resumo?inicio=&fim=&vendedorId=`: indicadores, produtos vendidos, evolução (por **hora** se for
    um dia, por **dia** até 62 dias, por **mês** acima disso), 5 mais vendidos, desempenho por vendedor e por forma de pagamento.
  - `GET /api/relatorios/picos?periodo=SEMANA|MES|SEMESTRE|ANO&referencia=&vendedorId=`: **picos de venda** do período que
    contém a data de referência (semana de segunda a domingo; semestre jan–jun ou jul–dez): série do período e seu pico,
    vendas por **dia da semana** e por **hora** com os respectivos picos, e comparação com o período anterior (`variacaoFaturamento` em %).
- Pedidos aceitam os filtros `termo` (número, cliente ou vendedor), `status`, `vendedorId`, `clienteId`, `formaPagamento`,
  `inicio` e `fim` (`aaaa-mm-dd`); ordem padrão: mais recentes primeiro. Cancelar exige `{"motivo": "..."}` e devolve ao estoque o que foi retirado.
- **Notificações** (`GET /api/notificacoes`) são calculadas na hora, sem tabela própria: produtos ativos com estoque
  igual ou abaixo do mínimo (Dono e Gerente), pedidos **em preparo** há mais que o tempo de `minutosAlertaPreparo`
  das Preferências de Vendas (padrão 20, de 5 a 240; o Vendedor só vê os próprios) e, para o Vendedor, os próprios
  pedidos cancelados nos últimos 7 dias. A resposta é `{naoLidas, itens[]}` (até 30, mais recentes primeiro);
  `POST /api/notificacoes/lidas` marca tudo o que existe até agora como lido.
- **Imagens** (logo e foto de produto): envio em `multipart/form-data` no campo `arquivo`, só **PNG ou JPG de até 1 MB**
  (o formato é conferido pelo conteúdo do arquivo). Ficam guardadas no banco e as respostas trazem `logoUrl`/`imagemUrl`
  como caminho relativo (ex.: `/api/imagens/5`). Trocar a imagem apaga a anterior.
- A senha do usuário é guardada com BCrypt e **nunca** aparece nas respostas.

**Erros** seguem sempre o mesmo formato:

```json
{
  "timestamp": "2026-10-14T20:00:00Z",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Existem campos inválidos.",
  "caminho": "/api/clientes",
  "campos": [{ "campo": "cpf", "mensagem": "CPF inválido." }]
}
```

| Status | Quando |
|---|---|
| 400 | Campos inválidos, JSON mal formatado ou parâmetro inválido |
| 401 | Sem login, token inválido/expirado, ou usuário/senha incorretos |
| 403 | Perfil sem permissão para a funcionalidade |
| 404 | Registro não encontrado |
| 413 | Arquivo enviado maior que o limite do servidor |
| 409 | Violação de restrição do banco |
| 422 | Regra de negócio violada (ex.: CPF ou usuário já cadastrado, desativar a própria conta, inativar o último Dono) |

## Testes e cobertura

```bash
./mvnw verify
```

- Roda os testes unitários e de integração (JUnit 5 + AssertJ).
- Gera o relatório de cobertura do **JaCoCo** em `target/site/jacoco/index.html`.

## Estrutura

```
src/main/java/com/pontoesabor/lanchonete/
├── config/       → segurança e configurações gerais
├── controller/   → endpoints HTTP (recebem e devolvem DTOs)
├── dto/          → objetos de entrada (com validação) e saída da API
├── exception/    → exceções e tratamento global de erros
├── model/        → entidades JPA e enums (com as regras de negócio)
├── repository/   → acesso ao banco (Spring Data JPA) e filtros de busca
├── service/      → casos de uso e regras que envolvem o banco
└── util/         → funções auxiliares (normalização de textos)

src/main/resources/
├── application.properties        → configuração comum
├── application-h2.properties     → banco em memória (padrão)
├── application-prod.properties   → PostgreSQL via variáveis de ambiente
└── db/migration/                 → scripts do Flyway (versionamento do banco)
```

## Banco de dados

As tabelas são criadas pelo **Flyway** a partir de `db/migration/`. O Hibernate apenas **valida** se as
entidades batem com o banco (`ddl-auto=validate`).

Para alterar o banco, **crie uma nova migration** (`V3__descricao.sql`, `V4__...`). Nunca edite uma
migration que já foi para a `develop`.

Modelo completo: [docs/modelagem/modelo-de-dados.md](../docs/modelagem/modelo-de-dados.md).
