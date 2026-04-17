# Simulador de Agendamento de Perícia

> Nome adotado nesta documentação: **prestador**
>
> Nome atual do projeto no código: **prestador**
>
> Porta padrão da aplicação: **8085**

---

## Sumário

1. [Visão geral](#visão-geral)
2. [Objetivo do sistema](#objetivo-do-sistema)
3. [Escopo funcional](#escopo-funcional)
4. [Arquitetura e stack](#arquitetura-e-stack)
5. [Endpoints disponíveis](#endpoints-disponíveis)
6. [Autenticação e autorização](#autenticação-e-autorização)
7. [Rastreabilidade e correlação](#rastreabilidade-e-correlação)
8. [Contrato da API de agendamento](#contrato-da-api-de-agendamento)
9. [Contrato do endpoint administrativo](#contrato-do-endpoint-administrativo)
10. [Health checks, actuator e métricas](#health-checks-actuator-e-métricas)
11. [Validações implementadas](#validações-implementadas)
12. [Modos de simulação](#modos-de-simulação)
13. [Erros e respostas](#erros-e-respostas)
14. [Variáveis de ambiente e configuração](#variáveis-de-ambiente-e-configuração)
15. [Observabilidade, telemetria e logs](#observabilidade-telemetria-e-logs)
16. [Resiliência e comportamento operacional](#resiliência-e-comportamento-operacional)
17. [Limitações conhecidas](#limitações-conhecidas)
18. [Build da aplicação](#build-da-aplicação)
19. [Execução local](#execução-local)
20. [Criação da imagem Docker](#criação-da-imagem-docker)
21. [Execução com Docker](#execução-com-docker)
22. [Testes](#testes)
23. [Checklist para integração com outros sistemas](#checklist-para-integração-com-outros-sistemas)
24. [Referências internas do projeto](#referências-internas-do-projeto)

---

## Visão geral

O sistema **prestador** é uma API HTTP construída em Spring Boot para **simular o agendamento de perícias**.

Na prática, ele funciona como um **serviço de integração controlado por cenários**, permitindo que sistemas consumidores validem:

- autenticação via Basic Auth;
- envio de payload de agendamento;
- resposta de sucesso;
- timeout;
- indisponibilidade temporária;
- falha técnica;
- comportamento intermitente;
- propagação de headers de correlação;
- leitura de health checks e métricas operacionais.

Esse serviço é útil principalmente como:

- simulador para ambientes de desenvolvimento e homologação;
- stub para validação de retry/fallback do consumidor;
- componente de testes de integração entre aplicações.

---

## Objetivo do sistema

O objetivo principal é **expor uma API previsível para o fluxo de agendamento de perícia**, com suporte a cenários controlados de sucesso e falha.

### O que o sistema faz hoje

- recebe solicitações de agendamento de perícia;
- retorna uma confirmação simulada com `periciaId`;
- permite escolher o modo de resposta via header, query string ou endpoint administrativo;
- mantém idempotência local em memória para a mesma chave funcional;
- registra logs estruturados com contexto da requisição;
- expõe métricas técnicas via Prometheus.

### O que o sistema não faz hoje

- não persiste dados em banco de dados;
- não consulta disponibilidade real de prestadores;
- não integra com CCM, BPM ou qualquer sistema externo real;
- não envia notificações;
- não implementa tracing distribuído real;
- não implementa circuit breaker, retry ou rate limiter no código, apesar de possuir dependência de Resilience4j.

---

## Escopo funcional

### Funcionalidades implementadas

1. **Agendar perícia**
   - endpoint principal para criação simulada de agendamento.
2. **Alterar modo global de simulação**
   - endpoint administrativo para definir o comportamento padrão do serviço.
3. **Health check simplificado**
   - endpoint `/health`.
4. **Actuator**
   - health, info e Prometheus.
5. **Correlação de requisições**
   - suporte a `X-Request-ID`, `X-Correlation-ID` e `traceparent`.
6. **Idempotência local**
   - mesma chave funcional retorna o mesmo resultado na instância atual.

---

## Arquitetura e stack

### Stack principal

- **Java 21**
- **Spring Boot 3.5.13**
- **Spring Web**
- **Spring Validation**
- **Spring Security**
- **Spring Actuator**
- **Micrometer + Prometheus**
- **Springdoc OpenAPI**
- **Lombok**
- **Resilience4j** (dependência presente)

### Principais componentes

- `src/main/java/com/zurich/prestador/controller/PericiaController.java`
- `src/main/java/com/zurich/prestador/controller/AdminController.java`
- `src/main/java/com/zurich/prestador/controller/HealthController.java`
- `src/main/java/com/zurich/prestador/service/PericiaSimulationService.java`
- `src/main/java/com/zurich/prestador/config/SecurityConfig.java`
- `src/main/java/com/zurich/prestador/web/CorrelationFilter.java`
- `src/main/java/com/zurich/prestador/exception/GlobalExceptionHandler.java`
- `src/main/resources/application.properties`

### Arquitetura real observada

A aplicação é uma API síncrona com:

- autenticação Basic Auth;
- regras de simulação em serviço central;
- estado temporário mantido em memória;
- sem camada de persistência externa;
- sem mensageria;
- sem integração real com outros sistemas.

---

## Endpoints disponíveis

| Método | Endpoint | Finalidade | Auth | Status principal |
|---|---|---|---|---|
| `POST` | `/api/prestador/v1/pericias` | Cria agendamento simulado | Sim | `200` |
| `POST` | `/__admin/sim-mode` | Define modo global de simulação | Sim | `200` |
| `GET` | `/health` | Health simplificado | Não | `200` |
| `GET` | `/actuator/health` | Health do Actuator | Não | `200` |
| `GET` | `/actuator/info` | Informações da aplicação | Não | `200` |
| `GET` | `/actuator/prometheus` | Métricas Prometheus | Não | `200` |

### Observação sobre OpenAPI

O projeto possui dependência de Springdoc e configuração de segurança para `/swagger-ui/**` e `/v3/api-docs/**`, porém o comportamento observado em runtime foi inconsistente:

- sem autenticação: `401 Unauthorized`;
- com autenticação: `500 Internal Server Error`.

Por isso, **não se recomenda usar o endpoint OpenAPI runtime como fonte oficial do contrato atual**. Este documento deve ser tratado como a documentação funcional principal.

---

## Autenticação e autorização

### Tipo de autenticação

A aplicação utiliza **HTTP Basic Authentication**.

### Credenciais padrão

Definidas em `src/main/resources/application.properties`:

- usuário: `appview`
- senha: `appview-secret-123`

Esses valores podem ser sobrescritos por variáveis de ambiente.

### Authorities configuradas

O usuário em memória recebe as seguintes authorities:

- `pericia:create`
- `prestador:admin`

### Regras de autorização

| Endpoint | Regra |
|---|---|
| `POST /api/prestador/v1/pericias` | requer `pericia:create` |
| `POST /__admin/sim-mode` | requer `prestador:admin` |
| `/health` | público |
| `/actuator/**` | público |

### Considerações de segurança

- o serviço usa um único usuário em memória;
- as credenciais padrão não devem ser usadas em produção;
- a senha está configurada com encoder `{noop}`;
- recomenda-se execução somente atrás de TLS reverso/proxy seguro;
- os endpoints do Actuator estão expostos sem autenticação.

---

## Rastreabilidade e correlação

A aplicação usa um filtro HTTP para suportar rastreamento básico entre sistemas.

### Headers suportados

| Header | Entrada | Saída | Observação |
|---|---|---|---|
| `X-Request-ID` | opcional | sim | se não vier, é gerado |
| `X-Correlation-ID` | opcional | sim | se não vier, é gerado |
| `traceparent` | opcional | sim, se informado | apenas propagação |

### Comportamento

- se `X-Request-ID` não for enviado, a aplicação gera um UUID;
- se `X-Correlation-ID` não for enviado, a aplicação gera um UUID;
- se `traceparent` for enviado, ele é devolvido na resposta;
- esses valores são adicionados ao contexto de log.

### Recomendação para integradores

Sempre envie:

- `X-Request-ID`
- `X-Correlation-ID`
- `traceparent` (quando houver rastreamento distribuído no consumidor)

---

## Contrato da API de agendamento

## Endpoint

`POST /api/prestador/v1/pericias`

### Finalidade

Criar um agendamento de perícia simulado.

### Headers aceitos

| Header | Obrigatório | Exemplo | Observação |
|---|---|---|---|
| `Authorization` | sim | `Basic <base64>` | Basic Auth |
| `Content-Type` | sim | `application/json` | JSON |
| `X-Request-ID` | não | `req-123` | se ausente, será gerado |
| `X-Correlation-ID` | não | `corr-123` | se ausente, será gerado |
| `traceparent` | não | `00-...-...-01` | propagado na resposta |
| `X-Sim-Mode` | não | `success` | controla o modo da resposta |

### Query string aceita

| Parâmetro | Obrigatório | Exemplo | Observação |
|---|---|---|---|
| `mode` | não | `timeout` | alternativa ao `X-Sim-Mode` |

### Precedência do modo de simulação

A escolha do modo de simulação segue esta ordem:

1. `X-Sim-Mode`
2. query param `mode`
3. modo global atual
4. valor default configurado

### Payload de entrada

```json
{
  "numeroSinistro": "SIN123456",
  "tipoPericia": "VISTORIA_VEICULAR",
  "prestadorId": "PREST-001",
  "dataAgendamento": "2026-02-10T14:00:00Z",
  "local": {
    "endereco": "Rua das Flores, 123",
    "cidade": "São Paulo",
    "uf": "SP",
    "cep": "01234-567"
  },
  "contato": {
    "nome": "João da Silva",
    "telefone": "+5511999998888"
  },
  "observacoes": "Veículo disponível das 14h às 17h"
}
```

### Alias aceito

O campo abaixo também é aceito no lugar de `tipoPericia`:

- `tipoPéricia`

### Campos do request

| Campo | Tipo | Obrigatório | Observações |
|---|---|---|---|
| `numeroSinistro` | `string` | sim | não pode ser vazio |
| `tipoPericia` | `string` | sim | não pode ser vazio |
| `prestadorId` | `string` | sim | não pode ser vazio |
| `dataAgendamento` | `string` | sim | formato ISO UTC obrigatório |
| `local` | `object` | sim | endereço do agendamento |
| `local.endereco` | `string` | sim | não pode ser vazio |
| `local.cidade` | `string` | sim | não pode ser vazio |
| `local.uf` | `string` | sim | não pode ser vazio |
| `local.cep` | `string` | sim | não pode ser vazio |
| `contato` | `object` | sim | contato da perícia |
| `contato.nome` | `string` | sim | não pode ser vazio |
| `contato.telefone` | `string` | sim | não pode ser vazio |
| `observacoes` | `string` | não | campo livre |

### Formato aceito de `dataAgendamento`

Regex implementada:

```text
^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$
```

Exemplo válido:

```text
2026-02-10T14:00:00Z
```

### Resposta de sucesso

**Status:** `200 OK`

```json
{
  "periciaId": "PER-184263",
  "status": "AGENDADA",
  "prestador": {
    "nome": "Auto Vistoria SP",
    "telefone": "+5511988887777"
  },
  "dataAgendamento": "2026-02-10T14:00:00Z"
}
```

### Campos da resposta

| Campo | Tipo | Descrição |
|---|---|---|
| `periciaId` | `string` | identificador gerado para a perícia |
| `status` | `string` | atualmente sempre `AGENDADA` em sucesso |
| `prestador.nome` | `string` | nome fixo retornado pelo simulador |
| `prestador.telefone` | `string` | telefone fixo retornado pelo simulador |
| `dataAgendamento` | `string` | espelha o valor recebido |

### Observações importantes do contrato

- o endpoint retorna `200`, não `201`;
- o `prestador` retornado é fixo e não depende de consulta real;
- `prestadorId` é usado na chave funcional, mas não altera o nome/telefone do retorno;
- não existe persistência real do agendamento.

---

## Contrato do endpoint administrativo

## Endpoint

`POST /__admin/sim-mode`

### Finalidade

Alterar o modo global de simulação da instância atual.

### Autenticação

Requer Basic Auth com autoridade `prestador:admin`.

### Request

```json
{
  "mode": "success"
}
```

### Modos válidos

- `success`
- `timeout`
- `fail`
- `unavailable`
- `slow_sla`
- `flaky`

### Resposta de sucesso

**Status:** `200 OK`

```json
{
  "mode": "success"
}
```

### Observação importante

Se for enviado um valor inválido em `mode`, o comportamento atual observado é:

- `500 Internal Server Error`

Não há tratamento padronizado com `400` para esse caso.

---

## Health checks, actuator e métricas

## 1. Health simplificado

### Endpoint

`GET /health`

### Resposta

```json
{
  "status": "UP"
}
```

## 2. Actuator health

### Endpoint

`GET /actuator/health`

### Uso

Endpoint técnico para monitoramento operacional.

## 3. Actuator info

### Endpoint

`GET /actuator/info`

### Observação

Atualmente retorna payload vazio ou mínimo, dependendo do runtime.

## 4. Prometheus

### Endpoint

`GET /actuator/prometheus`

### Uso

Exposição de métricas técnicas para coleta por Prometheus.

### Exemplos de métricas observadas

- `application_ready_time_seconds`
- `application_started_time_seconds`
- `disk_free_bytes`
- métricas JVM e do processo

---

## Validações implementadas

### Validações de Bean Validation

- campos obrigatórios do request;
- validação de objetos aninhados `local` e `contato`;
- regex do campo `dataAgendamento`.

### Validações que não existem hoje

O sistema **não valida** atualmente:

- disponibilidade real do prestador;
- prestador ativo/inativo;
- limite de perícias por dia;
- SLA de 7 dias úteis;
- formato forte de telefone;
- formato forte de CEP;
- enum fechado para `tipoPericia`;
- compatibilidade semântica entre os dados enviados.

---

## Modos de simulação

Os modos de simulação são a principal funcionalidade de integração do serviço.

| Modo | HTTP | Comportamento |
|---|---|---|
| `success` | `200` | resposta normal com pequeno delay |
| `timeout` | `200` | espera ~12s e retorna sucesso |
| `fail` | `500` | retorna erro interno simulado |
| `unavailable` | `503` | retorna indisponibilidade simulada |
| `slow_sla` | `200` | espera ~5,1s e retorna sucesso |
| `flaky` | `503` / `200` | 1ª chamada falha, 2ª com mesma chave tem sucesso |

### 1. `success`

Modo padrão de sucesso.

- aplica delay base configurável;
- usa variação aleatória de aproximadamente ±20% sobre o delay padrão;
- reutiliza resposta idempotente para a mesma chave funcional.

### 2. `timeout`

Simula timeout do provedor.

- espera aproximadamente `12` segundos;
- retorna `200 OK` ao final.

### 3. `fail`

Simula erro interno.

**Resposta:**

```json
{
  "error": "INTERNAL_ERROR",
  "message": "Simulated failure"
}
```

### 4. `unavailable`

Simula indisponibilidade temporária.

**Resposta:**

```json
{
  "error": "SERVICE_UNAVAILABLE",
  "message": "Service Temporarily Unavailable"
}
```

### 5. `slow_sla`

Simula uma chamada bem-sucedida, porém acima de SLA.

- espera aproximadamente `5.1` segundos;
- retorna `200 OK` ao final.

### 6. `flaky`

Simula comportamento intermitente para a mesma chave funcional.

- primeira tentativa: `503`;
- segunda tentativa com a mesma chave: `200`.

### Chave funcional usada pelo sistema

A chave usada para idempotência e tentativa flaky é composta por:

```text
numeroSinistro|prestadorId|dataAgendamento|tipoPericia
```

Campos como `local`, `contato` e `observacoes` não participam dessa composição.

---

## Erros e respostas

## 1. Falta de autenticação

**Status:** `401 Unauthorized`

Resposta gerenciada pelo Spring Security.

## 2. JSON malformado

**Status:** `400 Bad Request`

```json
{
  "timestamp": "2026-04-03T17:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Malformed JSON payload",
  "errors": null
}
```

## 3. Erro de validação

**Status:** `400 Bad Request`

```json
{
  "timestamp": "2026-04-03T17:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "local",
      "rejectedValue": null,
      "message": "local is required"
    }
  ]
}
```

## 4. Erro interno simulado

**Status:** `500 Internal Server Error`

```json
{
  "error": "INTERNAL_ERROR",
  "message": "Simulated failure"
}
```

## 5. Serviço indisponível simulado

**Status:** `503 Service Unavailable`

```json
{
  "error": "SERVICE_UNAVAILABLE",
  "message": "Service Temporarily Unavailable"
}
```

## 6. Modo inválido em `X-Sim-Mode` ou `mode`

Comportamento atual:

- o valor inválido é ignorado no endpoint funcional;
- o sistema tenta o próximo fallback disponível.

## 7. Modo inválido no endpoint admin

Comportamento atual observado:

- `500 Internal Server Error`

### Importante para consumidores

Existem **dois formatos de erro** no sistema:

1. erro estruturado (`timestamp`, `status`, `error`, `message`, `errors`);
2. erro simples (`error`, `message`).

O integrador deve suportar ambos.

---

## Variáveis de ambiente e configuração

Configurações relevantes em `src/main/resources/application.properties`:

```properties
server.port=${PORT:8085}
spring.application.name=prestador
prestador.sim.default-mode=${SIM_MODE:success}
prestador.sim.default-delay-ms=${DEFAULT_DELAY_MS:100}
app.security.basic.username=${PRESTADOR_USERNAME:appview}
app.security.basic.password=${PRESTADOR_PASSWORD:appview-secret-123}
management.endpoints.web.exposure.include=health,info,prometheus
```

### Variáveis suportadas

| Variável | Default | Descrição |
|---|---|---|
| `PORT` | `8085` | porta HTTP da aplicação |
| `SIM_MODE` | `success` | modo default da simulação |
| `DEFAULT_DELAY_MS` | `100` | delay base em ms para o modo `success` |
| `PRESTADOR_USERNAME` | `appview` | usuário Basic Auth |
| `PRESTADOR_PASSWORD` | `appview-secret-123` | senha Basic Auth |

---

## Observabilidade, telemetria e logs

## Logs estruturados

O endpoint funcional registra logs com os seguintes campos:

- `timestamp`
- `method`
- `path`
- `status`
- `duration_ms`
- `requestId`
- `correlationId`
- `traceparent`
- `numeroSinistro`
- `prestadorId`
- `periciaId`
- `simMode`

### Benefícios

- facilita troubleshooting entre sistemas;
- melhora auditoria técnica de chamadas;
- permite identificar rapidamente o cenário executado.

## Telemetria disponível hoje

### Existe

- logs estruturados por requisição;
- métricas técnicas via Actuator/Prometheus;
- correlação por headers.

### Não existe

- OpenTelemetry configurado;
- spans distribuídos reais;
- exportação OTLP;
- tracing end-to-end entre sistemas;
- métricas de negócio customizadas.

---

## Resiliência e comportamento operacional

## Idempotência local

A aplicação mantém um cache em memória de respostas idempotentes.

### Características

- tamanho máximo aproximado: `1000` entradas;
- política LRU simples;
- mesma chave funcional retorna o mesmo `periciaId`;
- válido apenas na instância atual.

### Implicações

- após reinício, o histórico é perdido;
- em múltiplas instâncias, a idempotência não é compartilhada;
- não substitui um mecanismo distribuído de idempotência.

## Estado em memória

Também é mantido em memória:

- modo global de simulação;
- contagem de tentativas do modo `flaky`.

## Resilience4j

O projeto possui a dependência no `build.gradle`, porém **não há uso efetivo no código** de:

- `@Retry`
- `@CircuitBreaker`
- `@RateLimiter`
- `@Bulkhead`

## Simulação de falhas

A principal estratégia de resiliência do sistema é ajudar o **consumidor** a validar resiliência, e não ser resiliente por si próprio.

---

## Limitações conhecidas

1. **Sem persistência real**
   - todo o estado relevante é local da instância.
2. **Sem integração com sistemas externos reais**
   - o serviço é um simulador.
3. **OpenAPI runtime inconsistente**
   - `/v3/api-docs` não está confiável no comportamento observado.
4. **Actuator público**
   - expõe métricas e health sem autenticação.
5. **Credenciais default em properties**
   - inadequado para ambientes sensíveis.
6. **Usuário único in-memory**
   - não há IAM corporativo real.
7. **Dockerfile atual usa Java 17**
   - o projeto compila com Java 21; recomenda-se alinhar a imagem base para Java 21 antes de uso produtivo.
8. **Erros com schemas diferentes**
   - consumidores devem tratar mais de um formato.
9. **Modo inválido no admin retorna 500**
   - comportamento não ideal para contrato público.

---

## Build da aplicação

A aplicação usa Gradle Wrapper.

### Gerar artefatos

```powershell
Set-Location "C:\PROJETOS\TRABALHO\prestador"
.\gradlew.bat clean build
```

### Rodar apenas os testes

```powershell
Set-Location "C:\PROJETOS\TRABALHO\prestador"
.\gradlew.bat test
```

### Artefato esperado

Após o build, o JAR principal fica em:

- `build/libs/prestador-0.0.1-SNAPSHOT.jar`

Também pode existir o JAR plain:

- `build/libs/prestador-0.0.1-SNAPSHOT-plain.jar`

---

## Execução local

### Executar com Gradle

```powershell
Set-Location "C:\PROJETOS\TRABALHO\prestador"
.\gradlew.bat bootRun
```

### Executar com JAR

```powershell
Set-Location "C:\PROJETOS\TRABALHO\prestador"
java -jar .\build\libs\prestador-0.0.1-SNAPSHOT.jar
```

### Executar alterando porta e modo default

```powershell
Set-Location "C:\PROJETOS\TRABALHO\prestador"
$env:PORT="8085"
$env:SIM_MODE="success"
$env:DEFAULT_DELAY_MS="100"
java -jar .\build\libs\prestador-0.0.1-SNAPSHOT.jar
```

### Validar subida da aplicação

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8085/health | Select-Object -ExpandProperty Content
```

Resposta esperada:

```json
{"status":"UP"}
```

---

## Criação da imagem Docker

### Nome novo da imagem

Para esta documentação, o nome recomendado da imagem é:

- **`prestador`**

### Porta correta

A porta correta da aplicação, conforme `application.properties`, é:

- **`8085`**

### Passo 1: gerar o JAR

```powershell
Set-Location "C:\PROJETOS\TRABALHO\prestador"
.\gradlew.bat clean build
```

### Passo 2: criar a imagem Docker com o nome novo

```powershell
Set-Location "C:\PROJETOS\TRABALHO\prestador"
docker build -t prestador:latest .
```

### Observação importante sobre a imagem atual

O `Dockerfile` atual expõe corretamente a porta `8085`, mas usa a imagem base:

```dockerfile
FROM eclipse-temurin:21-jre-alpine
```

Como o projeto está configurado para **Java 21**, recomenda-se alinhar o runtime do container para Java 21 antes do uso contínuo.

### Tag alternativa por versão

```powershell
docker build -t prestador:latest .
```

---

## Execução com Docker

### Rodar o container na porta 8085

```powershell
docker run --rm -p 8085:8085 --name prestador prestador:latest
```

### Rodar com variáveis de ambiente customizadas

```powershell
docker run --rm -p 8085:8085 \
  --name prestador \
  -e PORT=8085 \
  -e SIM_MODE=success \
  -e DEFAULT_DELAY_MS=100 \
  -e PRESTADOR_USERNAME=appview \
  -e PRESTADOR_PASSWORD=appview-secret-123 \
  prestador:latest
```

### Validar container

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8085/health | Select-Object -ExpandProperty Content
```

---

## Testes

Os testes existentes validam os principais cenários do endpoint de perícia.

### Casos cobertos por teste de integração

- sucesso com resposta esperada;
- autenticação obrigatória;
- timeout com espera mínima;
- erro `500` em modo `fail`;
- erro `503` seguido de sucesso em `flaky`;
- idempotência para a mesma chave funcional;
- propagação de headers de correlação.

### Executar testes

```powershell
Set-Location "C:\PROJETOS\TRABALHO\prestador"
.\gradlew.bat test
```

Arquivo de teste principal:

- `src/test/java/com/zurich/prestador/PericiaControllerIntegrationTest.java`

---

## Checklist para integração com outros sistemas

### Obrigatório para o consumidor

- suportar Basic Auth;
- enviar JSON no contrato documentado;
- tratar `200`, `400`, `401`, `500` e `503`;
- suportar dois schemas de erro;
- enviar `X-Request-ID` e `X-Correlation-ID`;
- implementar timeout do lado cliente;
- implementar retry controlado para `503` se fizer sentido;
- não assumir persistência real do provedor;
- não assumir OpenAPI runtime como fonte oficial.

### Recomendado

- enviar `traceparent`;
- tratar `flaky` como cenário de retry;
- usar `timeout` e `slow_sla` nos testes de robustez;
- monitorar `/actuator/prometheus` em ambientes internos.

---

## Referências internas do projeto

### Código-fonte

- `build.gradle`
- `src/main/resources/application.properties`
- `src/main/java/com/zurich/prestador/controller/PericiaController.java`
- `src/main/java/com/zurich/prestador/controller/AdminController.java`
- `src/main/java/com/zurich/prestador/controller/HealthController.java`
- `src/main/java/com/zurich/prestador/service/PericiaSimulationService.java`
- `src/main/java/com/zurich/prestador/config/SecurityConfig.java`
- `src/main/java/com/zurich/prestador/web/CorrelationFilter.java`
- `src/main/java/com/zurich/prestador/exception/GlobalExceptionHandler.java`
- `src/test/java/com/zurich/prestador/PericiaControllerIntegrationTest.java`
- `Dockerfile`
- `docker-compose.yml`

### Documentos corporativos de contexto

- `docs/02_Integracoes_Sistemas_Externos.md`
- `docs/05_Casos_de_Uso.md`
- `docs/06_Matriz_RBAC.md`
- `docs/07_Catalogo_Regras_Negocio.md`
- `docs/09_Guia_Implementacao_Backend.md`
- `docs/10_Observabilidade_Telemetria.md`
- `docs/11_Resiliencia_Cache_Performance.md`
- `docs/13_Estrategia_Testes.md`

---

## Resumo final

O **prestador** deve ser interpretado como uma **API simuladora para integração**, e não como um sistema corporativo completo de gestão de perícias.

Ele é adequado para:

- desenvolvimento;
- homologação;
- testes de contrato e comportamento do consumidor;
- validação de retry, fallback, timeout e correlação.

Para uso produtivo como sistema real, seriam necessários evoluções de persistência, segurança, observabilidade distribuída, validação de domínio e integração efetiva com serviços externos.

