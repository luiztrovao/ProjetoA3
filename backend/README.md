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
├── exception/    → exceções de regra de negócio
├── model/        → entidades JPA e enums (com as regras de negócio)
├── repository/   → acesso ao banco (Spring Data JPA)
├── service/      → regras de negócio que envolvem várias entidades (próximas etapas)
├── controller/   → endpoints HTTP (próximas etapas)
└── dto/          → objetos de entrada/saída da API (próximas etapas)

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
