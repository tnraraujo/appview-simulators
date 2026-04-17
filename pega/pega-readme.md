# Documentação da Aplicação - Pega BPM Simulator

## 📋 Visão Geral
O **Pega BPM Simulator** é uma aplicação Spring Boot desenhada para simular as APIs do sistema Pega BPM. Ele é utilizado para fins de teste de integração, permitindo simular com precisão comportamentos normais, atrasos (delays), falhas (Chaos Engineering) e Rate Limiting, essencial para validar a resiliência dos sistemas consumidores (Circuit Breaker, Retries, etc.).

## 🚀 Tecnologias Utilizadas
- **Java 21 LTS**
- **Spring Boot 3.3.0**
- **Resilience4j** (Rate Limiter)
- **Lombok**
- **Docker** para containerização

---

## ⚙️ Build da Aplicação e Docker

Você pode compilar a aplicação e empacotá-la usando o Gradle Wrapper fornecido no projeto.

### 1. Fazer o Build via Gradle
```bash
./gradlew clean build -x test
```

### 2. Criar Imagem Docker
Nome da Imagem sugerida: `pega`. A porta da aplicação, definida em `application.properties`, é **8186**.

Crie ou valide seu `Dockerfile` na raiz do projeto contendo a porta correta:
```dockerfile
FROM eclipse-temurin:21-jdk-alpine
VOLUME /tmp
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8186
ENTRYPOINT ["java","-jar","/app.jar"]
```

Faça o build da imagem Docker utilizando o "nome novo" `pega`:
```bash
docker build -t pega:latest .
```

### 3. Subir o Container Docker
Inicie o container mapeando a porta correta do Host para a do container (8186):
```bash
docker run -d -p 8186:8186 --name pega pega:latest
```

---

## 🔒 Segurança e Autenticação
A aplicação possui suporte (simulado) à autenticação via **Basic Auth**, configurada globalmente em seu *consumer* ou pelas chaves de propriedades:
- `simulator.pega.auth.username` (padrão: `appview_local`)
- `simulator.pega.auth.password` (padrão: `dev_secret_123`)

Em ambientes de produção/homologação, as credenciais devem ser injetadas externalmente (ex: Azure Key Vault).

---

## 📡 Integrações e Endpoints Disponíveis

A aplicação funciona gerando Mocks dinâmicos e permitindo testes de resiliência usando os Headers HTP `X-Simulate-Error` (código HTTP) e `X-Simulate-Delay` (em milissegundos).

### 1. Criar Caso (Sinistro)
- **Endpoint:** `POST /api/pega/v1/cases/sinistro`
- **Rate Limit:** 100 requisições por minuto (`pegaServer`)
- **Descrição:** Simula a abertura de um caso de sinistro no Pega BPM.

**Exemplo de Resposta (201 Created):**
```json
{
  "caseId": "PEGA-CASE-A1B2C3D4",
  "status": "CREATED",
  "workflowStarted": true,
  "nextStep": "ANALISE_DOCUMENTACAO",
  "createdAt": "2026-04-03T10:00:00+00:00"
}
```

> **Simulação de Duplicidade:** Se enviar uma re-tentativa pelo mesmo número de sinistro ou passar `X-Simulate-Error: 409`, retornará erro `409 Conflict`.

### 2. Atualizar Documentos
- **Endpoint:** `PUT /api/pega/v1/cases/{caseId}/documents`
- **Rate Limit:** 100 req/min
- **Descrição:** Vincula ou atualiza documentos em um caso existente. Retorna HTTP 404 caso o simulador não encontre o `caseId` prévio na memória.

### 3. Consultar Caso
- **Endpoint:** `GET /api/pega/v1/cases/{caseId}`
- **Rate Limit:** 100 req/min
- **Descrição:** Retorna as informações mais atuais de um caso específico.

---

## 🌪️ Chaos Engineering Rest
Ideal para teste de integração em pipelines e validação das aplicações consumidoras.

- **Ativar Falhas (POST):** `POST /api/simulator/chaos` - Injeta status e delay global na API baseando-se no corpo da requisição:
```json
{
  "status": 500,
  "delayMs": 15000
}
```
- **Resetar (DELETE):** `DELETE /api/simulator/chaos` - Retoma o uso normal.

## 🎛️ Monitoramento
Métricas para Prometheus/Actuator rodando na porta oficial:
- `GET /actuator/health`
- `GET /actuator/prometheus` (métricas de rate limiter, JVM, requests e latência)

