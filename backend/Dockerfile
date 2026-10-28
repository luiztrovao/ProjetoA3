# Imagem da API para publicar no Render (plano gratuito).
# Os testes rodam no CI do GitHub; aqui só o empacotamento.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home api
COPY --from=build /app/target/lanchonete-api-*.jar app.jar
USER api
# Perfil prod = PostgreSQL (Neon). A memória é ajustada aos 512 MB do plano gratuito.
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
