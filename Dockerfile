# Estágio 1: Build da aplicação com Maven e OpenJDK 21
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

COPY pom.xml .

# Compilação e empacotamento do JAR executável
COPY src ./src
RUN mvn clean package -DskipTests

# Estágio 2: Imagem final de execução (minimalista JRE 21)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Criação de usuário não-root e diretório de dados para persistência H2 / logs
RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
    && mkdir -p /app/data && chown -R appuser:appgroup /app

USER appuser

# Cópia do JAR gerado no primeiro estágio
COPY --from=builder /workspace/target/*.jar app.jar

# Porta padrão do Lead Service
EXPOSE 8080

# Flags de JVM otimizadas para ambiente de contêineres e Kubernetes
ENV JAVA_OPTS="-Xms128m -Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
