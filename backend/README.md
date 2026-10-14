# Backend

API REST em **Java 21 + Spring Boot**, com PostgreSQL (Neon) e Spring Data JPA.

Estrutura planejada:

```
src/main/java/com/lanchonete/
├── controller/   → endpoints HTTP
├── service/      → regras de negócio
├── repository/   → acesso ao banco
├── model/        → entidades JPA
├── dto/          → objetos de entrada/saída
├── exception/    → tratamento de erros
└── config/       → segurança, CORS etc.
```
