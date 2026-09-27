# Estágio 1: Build da aplicação com Maven e OpenJDK 21
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Cache inteligente de dependências do Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Compilação e empacotamento do JAR executável
COPY src ./src
RUN mvn clean package -DskipTests

# Estágio 2: Imagem final de execução (distroless/minimalista JRE 21)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Criação de usuário não-root por boas práticas de segurança em produção
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Cópia do artefato gerado no estágio de build
COPY --from=builder /workspace/target/*.jar app.jar

# Porta padrão do Spring Cloud Netflix Eureka Discovery Server
EXPOSE 8761

# Variáveis de JVM otimizadas para contêineres
ENV JAVA_OPTS="-Xms64m -Xmx256m -XX:+UseG1GC -XX:+UseStringDeduplication"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
