# Jarvis Simulator

Simulador da integracao com Jarvis OCR para desenvolvimento e testes do App View.

## Modo local (sem banco)

O simulador funciona 100% em memoria (sem JPA/H2). O fluxo e:

1. Recebe `POST /documents-reading` e retorna `202`.
2. Armazena a solicitacao em memoria.
3. Apos o delay configurado em `jarvis.simulator.delay-ms`, envia callback para `jarvis.simulator.fixed-callback-url`.

> Como o armazenamento e em memoria, reiniciar a aplicacao descarta solicitacoes pendentes.

## Contrato de request (documents-reading)

Endpoint:

`POST /api/jarvis/cognitive-services/v1/documents-reading`

Headers obrigatorios:

- `Content-Type: application/json`
- `Ocp-Apim-Subscription-Key: <API_KEY>`

Campos obrigatorios no payload:

- `documentCode`
- `claimId`
- `documentBase64`

Campos opcionais aceitos por compatibilidade:

- `documentIndex`
- `sequential`
- `channel`
- `channelId`
- `holderName`
- `origin`
- `documentType`
- `callbackUrl` (legado, ignorado)

> Observacao: `callbackUrl` enviada pelo cliente nao e utilizada pelo simulador.

## Callback

O callback de resultado e enviado sempre para a URL fixa configurada em:

- `jarvis.simulator.fixed-callback-url`

Exemplo em `application.properties`:

```ini
jarvis.simulator.fixed-callback-url=http://localhost:8080/api/jarvis/callback
```

Se essa configuracao estiver ausente/vazia, o endpoint de ingestao retorna erro:

- HTTP `500`
- `code: CONFIGURATION_ERROR`

## Rate limit

Quando o limite e excedido, o simulador retorna:

- HTTP `429`
- header `Retry-After: 60`
- `code: RATE_LIMITED`

## Exemplo de chamada minima

```bash
curl -X POST http://localhost:8081/api/jarvis/cognitive-services/v1/documents-reading \
  -H "Content-Type: application/json" \
  -H "Ocp-Apim-Subscription-Key: jarvis-dev-key" \
  -d '{
    "documentCode": "alfresco-doc-001",
    "claimId": "SIN123456",
    "documentBase64": "JVBERi0xLjQKMSAwIG9iago8PAovVGl0bGUgKFRlc3RlKQo+PgplbmRvYmo="
  }'
```

Resposta esperada:

- HTTP `202`
- `requestId`
- `status: PROCESSING`
- `estimatedCompletionTime`

