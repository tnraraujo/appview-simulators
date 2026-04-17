# CCM - Customer Communication Management (Simulador)

O **CCM** atua como uma aplicação de testes (simulador) inteligente desenhada para mimetizar o comportamento do sistema de Customer Communication Management, garantindo que aplicações clientes possam validar sua integração, tratar resiliência (Circuit Breakers e Retries) e testar integrações complexas de maneira autônoma, sem onerar um sistema de produção.

## 📋 Funcionalidades

- **Simulação da Criação de Comunicações:** Mimetiza o processamento e o envio para canais (SMS, E-mail, Push) gerando de forma pseudo-aleatória o ID de Comunicação.
- **Consulta de Status de Comunicações:** Fornece um "Webhook/Polling Fake" que muda de "PROCESSANDO" para "ENTREGUE" automaticamente pelo tempo, ideal para testes de orquestração.
- **Injeção de Falhas (Chaos Engineering):** Utilizado para testar cenários de degradação baseadas em cabeçalhos (timeout, erros 500 globais, falhas efêmeras conhecidas como flaky, etc).

---

## 🔌 Formas de Integração (Endpoints)

A aplicação de integração expõe endpoints padronizados REST via porta `8184`. 
O caminho base é `/api/ccm/v1/comunicacoes`.

### 1. Criar Comunicação (Agendamento)

- **Método:** `POST`
- **Path:** `/api/ccm/v1/comunicacoes`
- **Cabeçalho Útil:** `X-Sim-Mode: {mode}` (Opcional, sobrescreve o modo padrão)

**Request Payload:**
```json
{
  "numeroSinistro": "SIN-12345",
  "conteudo": {
    "template": "SINISTRO_RECEBIDO"
  },
  "canais": ["SMS", "EMAIL"],
  "agendarPara": "2026-05-10T10:00:00Z"
}
```

**Response Payload (201 Created):**
```json
{
  "comunicacaoId": "COM-A1B2C3D4",
  "status": "AGENDADA",
  "createdAt": "2026-04-03T10:00:00Z",
  "canaisAgendados": [
    {
      "canal": "SMS",
      "status": "AGENDADO",
      "envioEstimado": "2026-05-10T10:00:00Z"
    }
  ]
}
```

### 2. Consultar Status de Comunicação

- **Método:** `GET`
- **Path:** `/api/ccm/v1/comunicacoes/{comunicacaoId}`
- **Parâmetro Útil:** `?mode={mode}` (Opcional)

**Response Payload (200 OK):**
```json
{
  "comunicacaoId": "COM-A1B2C3D4",
  "numeroSinistro": "SIN-12345",
  "status": "ENTREGUE",
  "createdAt": "2026-04-03T10:00:00Z",
  "canais": [
    {
      "canal": "SMS",
      "status": "ENTREGUE"
    }
  ],
  "enviadoEm": "2026-04-03T10:00:03Z",
  "entregueEm": "2026-04-03T10:00:06Z"
}
```

---

## 🎭 Modos de Simulação (Injeção de Falhas)

Você pode passar diferentes modos de execução pelo Header `X-Sim-Mode`, via Query Parameter `?mode=` ou pela configuração padrão no `application.yml` em `simulador.mode`.

**Modos suportados:**

| Modo       | Endpoint Suportado | Efeito Gerado                                                                                                 | Ideal para testar |
|------------|---------------------|---------------------------------------------------------------------------------------------------------------|--------------------|
| `success`  | *Todos*             | Fluxo ideal. O Post retorna agendado (201) e o Get retorna "PROCESSANDO" ou "ENTREGUE" (muda em 10 segundos)| Happy path         |
| `timeout`  | POST                | Trava a requisição por 12 segundos, forçando tempo limite na origem.                                          | Circuit Breaker    |
| `fail`     | POST                | Devolve Http 500 (Internal Server Error) imediatamente.                                                       | Dead Letter        |
| `flaky`    | POST                | Falha com erro 500 na primeira vez, e roda com sucesso na segunda com base no Sinistro+Template.              | Retry Exponential  |
| `notfound` | GET                 | Retorna HTTP 404 - Not Found imediatamente.                                                                   | Cenários NotFound  |
| `entregue` | GET                 | Força o status interno da comunicação como "ENTREGUE".                                                        | Triggers           |
| `falha`    | GET                 | Força o status interno da comunicação como "FALHA".                                                           | Workflows BPM      |

---

## 🛡️ Segurança

A aplicação é protegida utilizando **Spring Security**. 
Para acessar os endpoints REST é essencial passar o token com as respectivas Roles geridas em Nível de Recurso:

- Para chamar o método `POST` (criar):
  Exige Autoridade `ROLE_ccm:comunicacao:write` OU `ROLE_ccm:admin`.
- Para chamar o método `GET` (ler status):
  Exige Autoridade `ROLE_ccm:comunicacao:read` OU `ROLE_ccm:admin` OU ter a general Role `ADMIN`.

Para o acesso direto pelo actuator da base as credenciais básicas por default (Configuráveis no `application.yml` via variáveis):
- **User:** appview
- **Password:** appview-secret

---

## 📈 Observabilidade

A aplicação propaga as métricas padrão e os logs contextualizados.
Além disso, utiliza o *Micrometer / Prometheus* para instrumentar a API e verificar as injecões de falha com sucesso. Você pode acessar as métricas de simulacão via Actuator:
- `http://localhost:8184/actuator/prometheus`
Verá contagens em `ccm.requests.failed` e `ccm.requests.total`, etiquetadas pelos diferentes `modes` testados.

---

## 🐳 Build e Criação da Imagem Docker

Você pode subir e builear este simulador isoladamente e rodar na sua máquina utilizando o `Dockerfile` nativo da aplicação embarcada na raiz.
A aplicação expõe seus acessos por padrão na porta **8184**.

### Via Gradle Direto
Caso queira compilar o executável no próprio terminal:

```bash
# Limpa e cria o artefato Build *.jar
./gradlew clean build
```

### Criando Imagem Localmente (Docker)

1. Para criar a imagem (certifique-se de estar na raiz onde o `Dockerfile` está):
```bash
docker build -t ccm .
```

2. Após o build da imagem, inicie um contêiner utilizando a mesma porta `8184`:
```bash
docker run -d --name ccm -p 8184:8184 ccm
```

Feito isso, a aplicação subirá sob o usuário `spring` de segurança sem permissão `root` e tudo operará corretamente, pronta para escutar as chamadas da aplicação central.

