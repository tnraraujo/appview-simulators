# Jarvis (Simulador de Serviços Cognitivos)

## Visão Geral
O **Jarvis** é uma aplicação desenvolvida em Spring Boot criada para simular serviços cognitivos (como APIs de OCR e Inteligência Artificial para leitura de documentos). Ele implementa um fluxo de processamento assíncrono: a aplicação recebe uma requisição contendo a imagem de um documento (em Base64), processa essa requisição em background de forma agendada simulando o tempo de processamento real, e envia de volta os resultados extraídos do documento para um Webhook (URL de Callback) informado na requisição original.

---

## Funcionalidades Principais

- **Processamento Assíncrono:** Recebe a requisição, salva em banco de dados e devolve o protocolo imediatamente (`202 Accepted`).
- **Geração de Mocks Dinâmicos:** Dependendo do tipo de documento informado (ex: `CNH`, `RG`, `COMPROVANTE_RESIDENCIA`), o sistema gera dados fictícios estruturados (nome, CPF, categoria, data de nascimento, etc.) e simula métricas de leitura como `legibilidade` e `acuracidade`.
- **Callback (Webhook) Seguro e Resiliente:** Envia o resultado para a URL de callback do cliente aplicando retentativas automáticas (Retry) em caso de falha de conexão e garantindo a autenticidade através de assinaturas criptográficas (HMAC SHA-256).
- **Simulação de Erros Controlada:** Permite forçar o retorno de erros HTTP específicos para testar os cenários de falha no sistema cliente, utilizando o cabeçalho `X-Simulate-Error`.

---

## Integração e APIs

### 1. Ingestão de Documentos (Entrada)

Responsável por receber o documento para processamento.

- **Método/URL:** `POST /api/jarvis/cognitive-services/v1/documents-reading`
- **Cabeçalhos (Headers):**
  - `Ocp-Apim-Subscription-Key` (Obrigatório): Chave de acesso ("API Key") para autenticação.
  - `X-Simulate-Error` (Opcional): Código HTTP (ex: 500, 400). Se enviado, a API rejeitará a chamada forçando o erro especificado.

- **Corpo da Requisição (JSON):**
```json
{
  "documentCode": "DOC-123",
  "documentIndex": 1,
  "sequential": "001",
  "channel": "APP",
  "channelId": "CH-999",
  "holderName": "João da Silva",
  "origin": "MOBILE",
  "claimId": "CLM-456",
  "callbackUrl": "https://meu-sistema.com/api/callback",
  "documentBase64": "iVBORw0KGgoAAAANSUhEUgAAAAE...",
  "documentType": "CNH"
}
```

- **Respostas Esperadas:**
  - `202 Accepted`: Requisição recebida e enfileirada com sucesso.
  - `401 Unauthorized`: Chave de API inválida ou ausente.
  - `413 Payload Too Large`: Tamanho em megabytes do Base64 excede o limite.
  - `429 Too Many Requests`: Limite de requisições excedido.

**Exemplo de Resposta de Sucesso (202):**
```json
{
  "requestId": "f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
  "status": "PROCESSING",
  "eta": "2026-04-03T10:05:00Z"
}
```

### 2. Retorno de Processamento (Webhook/Callback)

Após o processamento agendado, o Jarvis fará uma requisição POST na `callbackUrl` informada na requisição de ingestão.

- **Método:** `POST`
- **Cabeçalhos Enviados pelo Jarvis:**
  - `X-Correlation-Id`: O mesmo `requestId` devolvido na ingestão (UUID).
  - `Idempotency-Key`: Chave para garantir idempotência no cliente.
  - `X-Jarvis-Timestamp`: Timestamp UNIX gerado no momento do envio.
  - `X-Jarvis-Signature`: Assinatura HMAC gerada para validação.

- **Corpo da Requisição Efetuada:**
```json
{
  "documentCode": "DOC-123",
  "claimId": "CLM-456",
  "status": "COMPLETED",
  "result": {
    "legibilidade": 85,
    "acuracidade": 92,
    "matchDocumento": 78,
    "texto": "Texto extraido do documento...",
    "confianca": 0.92,
    "tipoDocumento": "CNH",
    "dadosExtraidos": {
      "nome": "Usuario Simulador CNH",
      "cpf": "111.222.333-44",
      "numeroRegistro": "12345678900",
      "categoria": "AB",
      "dataValidade": "2030-12-31"
    }
  },
  "timestamp": "2026-04-03T10:05:05Z",
  "processingDurationMs": 5000
}
```

---

## Segurança 🔐

- **Autenticação Inbound (API Key):** Toda chamada à API do Jarvis exige uma chave válida configurada pela propriedade `jarvis.simulator.api-key`.
- **Autenticação Outbound (Assinatura de Callback):** Para evitar que os clientes recebam chamadas maliciosas forjadas de terceiros (Spoofing), o Jarvis envia dados assinados via HMAC SHA-256 (`webhook-signature-enabled=true`). O hash inclui o conteúdo da mensagem e cabeçalhos de tempo para proteção também contra ataques de repetição (*Replay Attacks*).
- **Proteção de Carga (Payload):** O sistema valida limites máximos de tamanho do conteúdo em base64 decodificado (padrão 10MB) evitando esgotamento de memória.

---

## Resiliência, Observabilidade e Telemetria 📊

- **Limitação de Taxa (Rate Limit):** A API possui sistema de controle numérico de requisições por API Key (Padrão de 50 requests/minuto). Acima disso, será retornado `429 Too Many Requests`.
- **Tolerância a Falha no Webhook:** Caso o sistema cliente fique fora do ar e o Jarvis não consiga entregar o JSON final, ele enfileirará o webhook com recuo dinâmico (Backoff de retentativa padrão: 5 segundos e 2 re-tentativas).  
- **Métricas via Prometheus e Actuator:** A aplicação coleta e expõe contadores métricos avançados pelo Spring Boot Actuator/Micrometer na rota `/actuator/prometheus`:
  - `jarvis_requests_total`: Número de requisições com tags para aceitas e os motivos de rejeição.
  - `jarvis_callback_total`: Número de webhooks que tiveram sucesso ou que falharam de forma terminal.
  - `jarvis_processing_duration`: Tabela de percentis (Timer) da latência de ponta a ponta.
- **Banco de Dados em Memória:** As requisições são armazenadas num banco de dados leve H2 em-memória (`jdbc:h2:mem:jarvisdb`) para otimizar os testes, podendo ser facilmente trocado para PostgreSQL ou MySQL mudando apenas as propriedades e os drivers do projeto.

---

## Build e Execução

### Passo 1: Como efetuar o Build local (Gradle)

Para buildar sua aplicação localmente utilizando o Spring Boot via Gradle (O Java 21 é obrigatório):

```bash
# Limpar builds passados e gerar pacote final (sem rodar testes ou com testes):
./gradlew clean build
```

A aplicação subirá na porta **8081**. A documentação de APIs possivelmente pode ser acessada nos endpoints base da aplicação.

### Passo 2: Construção da Imagem Docker

O projeto já contém um `Dockerfile` em multi-stage preparado para gerar uma imagem enxuta (Alpine) rodando Java 21.

Para gerar e subir a imagem Docker do projeto utilizando o nome novo ajustado (`jarvis`):

```bash
# Gerando a build da imagem localmente e dando a tag de 'jarvis:latest'
docker build -t jarvis:latest .
```

#### Executar o Container Manualmente:
Depois de criada a imagem, você pode inicializá-la mapeando a porta padrão configurada (`8081`):

```bash
docker run -p 8081:8081 --name jarvis -d jarvis:latest
```

### Executar via Docker Compose
O projeto já reflete o nome da aplicação. Basta rodar o comando na raiz para que o Compose cuide da construção (build) local e inicialização.

```bash
docker-compose up -d --build
```
Isso publicará a aplicação `jarvis` na porta `8081` do seu localhost, com restart automático ativado e injetando as variáveis por ambiente local se necessário.
