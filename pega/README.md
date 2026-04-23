# Pega BPM Simulator

Simulador HTTP para integrar o App View com Pega durante desenvolvimento e testes locais.

## O que este simulador cobre

- Endpoint `POST /api/pega/v1/cases/sinistro`
- Endpoint `PUT /api/pega/v1/cases/{caseId}/documents`
- Endpoint `GET /api/pega/v1/cases/{caseId}`
- Basic Auth com usuário/senha configuráveis
- Limite de taxa `100 req/min`
- Regras mínimas de contrato para OCR:
  - `ocrScores` obrigatório na criação
  - `legibilidade >= 60` (RN-020)

## Executar local

Use as credenciais padrão de desenvolvimento (ou sobrescreva via variáveis de ambiente):

- `PEGA_BASIC_AUTH_USERNAME` (default: `appview_local`)
- `PEGA_BASIC_AUTH_PASSWORD` (default: `dev_secret_123`)

```bash
cd /home/tony/Projetos/appview-simulators/pega
bash ./gradlew bootRun
```

## Rodar testes

```bash
cd /home/tony/Projetos/appview-simulators/pega
bash ./gradlew test --no-daemon
```

## Cenários cobertos por testes de contrato

- Criação de caso com payload válido (`201`)
- Validação de campos obrigatórios (`400`)
- Regra RN-020 para legibilidade (`400`)
- Idempotência por número de sinistro (`409` com `caseId` existente)

