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

### Usando o PostgreSQL (Neon)

Ative o perfil `prod` e informe a conexão por variáveis de ambiente (nunca no código):

```bash
SPRING_PROFILES_ACTIVE=prod DB_URL=jdbc:postgresql://<host>/<banco>?sslmode=require DB_USERNAME=<usuario> DB_PASSWORD=<senha> ./mvnw spring-boot:run
```

## Endpoints

A documentação completa e interativa fica no Swagger. Resumo:

| Recurso | Endpoints |
|---|---|
| Categorias | `GET /api/categorias` |
| Produtos | `GET /api/produtos` · `GET /api/produtos/{id}` · `POST /api/produtos` · `PUT /api/produtos/{id}` · `DELETE /api/produtos/{id}` · `PATCH /api/produtos/{id}/ativar` |
| Clientes | `GET /api/clientes` · `GET /api/clientes/{id}` · `POST /api/clientes` · `PUT /api/clientes/{id}` · `DELETE /api/clientes/{id}` · `PATCH /api/clientes/{id}/ativar` |
| Usuários | `GET /api/usuarios` · `GET /api/usuarios/{id}` · `POST /api/usuarios` · `PUT /api/usuarios/{id}` · `DELETE /api/usuarios/{id}` · `PATCH /api/usuarios/{id}/ativar` |

**Convenções:**

- `DELETE` faz **exclusão lógica** (marca como inativo); `PATCH .../ativar` desfaz.
- Listagens aceitam `termo`, `ativo` (`true`/`false`), `page` (começa em 0), `size` e `sort` (ex.: `sort=preco,desc`), e devolvem `{conteudo, pagina, tamanho, totalElementos, totalPaginas}`.
- Filtros extras: `categoriaId` em produtos e `perfil` em usuários. Em clientes, `termo` busca por nome, CPF, telefone ou e-mail.
- CPF e telefone podem ser enviados com ou sem máscara; a API guarda só os dígitos.
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
| 404 | Registro não encontrado |
| 409 | Violação de restrição do banco |
| 422 | Regra de negócio violada (ex.: CPF ou e-mail já cadastrado, inativar o último Dono) |

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
