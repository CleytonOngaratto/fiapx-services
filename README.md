# fiapx-services

Os microsserviços do **FIAP X — Sistema de Processamento de Vídeos**. Maven multi-module, **Java 25 +
Quarkus 3.40 LTS**, cada módulo com Dockerfile e chart próprios.

| Módulo | Responsabilidade | Estado |
|---|---|---|
| `video-api` | única superfície HTTP pública: identidade, upload assinado, status e download | em construção |
| `video-processor` | ffprobe → ffmpeg → zip → upload; é o serviço que escala | a fazer |
| `notifier` | e-mail de falha | a fazer |

As dependências de runtime (PostgreSQL, RabbitMQ, Redis, object storage, Mailpit) vivem em
[`fiapx-infra`](https://github.com/CleytonOngaratto/fiapx-infra); o front, em
[`fiapx-web`](https://github.com/CleytonOngaratto/fiapx-web).

## Rodar

```bash
# 1. as dependências, no repositório de infra
cd ../fiapx-infra/dev && docker compose up -d

# 2. o build (exige JDK 25)
cd ../../fiapx-services && ./mvnw clean verify

# 3. a API
java -jar video-api/target/quarkus-app/quarkus-run.jar
```

⚠️ A porta **8080** pode estar ocupada pelo container do projeto base (`projeto-fiapx:local`). Use
`-Dquarkus.http.port=8081` ou pare aquele container.

## Convenção interna dos módulos

Fatias verticais por contexto, cada uma internamente Clean — herdada das Fases 2, 3 e 4:

```
<contexto>/
├── domain/entity          Java puro, zero framework
├── usecase/
│   ├── interactor         classes puras, SEM anotação de injeção
│   └── port/{in,out}      as fronteiras
├── adapter/
│   ├── controller         recursos JAX-RS
│   ├── dto · presenter    entrada e saída
│   └── gateway            implementações de port/out
└── infrastructure/config  o único lugar onde se faz `new`, via @Produces
```

**ArchUnit garante a regra**, não a boa vontade: domínio livre de framework, `usecase` sem
`infrastructure` e sem `adapter`, domínio e usecase sem SDK de fornecedor, e interactors sem anotação
de injeção. São testes, e quebram o build.

## O upload é assinado, e são dois endpoints

`POST /videos` devolve uma **URL assinada**; o navegador faz `PUT` dela **direto no object storage**, e
os bytes nunca passam pela JVM. Isso mantém o pod da API pequeno e torna o *Claim Check* literal.

A consequência não óbvia é que o adapter precisa de **dois endpoints distintos** para o mesmo storage:

| Propriedade | Quem usa | No cluster vale |
|---|---|---|
| `quarkus.s3.endpoint-override` | o `S3Client`, do lado servidor (`HEAD`, `GET`) | o nome interno do serviço |
| `fiapx.s3.public-endpoint` | **só o `S3Presigner`** | um endereço que o **navegador** resolve |

Confundir os dois gera uma URL que o servidor alcança e o navegador não — falha que só aparece no
console do navegador. Há também **duas superfícies de CORS**: a da API (`quarkus.http.cors.*`) e a do
bucket, no storage.

## Testes

`./mvnw clean verify` roda tudo e aplica o gate de cobertura (**LINE ≥ 80% por módulo**).

O adapter de storage **não é mockado**: `S3ObjectStorageGatewayTest` sobe um S3 real com
Testcontainers e prova que a URL assinada **aceita o PUT** e que o objeto passa a existir. Mockar o
adapter provaria apenas que uma string foi gerada.

## API

`video-api/docs/api/openapi.yaml` é **gerado pelo build**, não escrito à mão.
