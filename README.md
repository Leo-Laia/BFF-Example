# BFF Token Handler - exemplo guiado para o curso

Este repositório é um material de apoio para entender, na prática, como funciona o padrão **BFF Token Handler**.

A ideia aqui não é montar uma aplicação completa de produção. É criar um exemplo pequeno, executável e fácil de observar, para responder uma dúvida bem comum:

> Como fazer um front-end browser-based chamar uma API protegida sem guardar tokens OAuth no browser?

## Comece Pelo Manual

O melhor caminho para estudar este exemplo é seguir o manual com prints e evidências:

**[Manual de execução e evidências](docs/evidencias-bff-token-handler/manual-execucao-evidencias.md)**

Ele mostra o fluxo passo a passo, com telas reais, comandos, respostas HTTP e logs. Se você está vendo este assunto pela primeira vez, comece por ele. O README é só o mapa geral do projeto.

## Problema Que Este Projeto Demonstra

Em aplicações front-end que rodam no browser, uma pergunta aparece cedo ou tarde:

> Onde eu guardo o `access_token` e o `refresh_token` com segurança?

Se o JavaScript guarda token em `localStorage`, `sessionStorage` ou algum estado da aplicação, um XSS pode tentar ler esse token e enviá-lo para fora. O BFF Token Handler muda a conversa:

- o front-end deixa de ser o cliente OAuth;
- o browser mantém apenas um cookie de sessão;
- o BFF faz login, mantém a sessão e guarda os tokens associados a ela;
- o front chama apenas URLs relativas do próprio BFF;
- o BFF usa o token para chamar a Resource API;
- a Resource API continua protegida por JWT e scopes.

Ou seja: o browser continua autenticado, mas não precisa conhecer o token OAuth.

## Arquitetura

```text
                    +----------------------+
                    | Authorization Server |
                    | Keycloak             |
                    +----------^-----------+
                               |
                               | OAuth2 / OIDC
                               |
+-------------+      cookie    |      +------------------+
|             |----------------+----->|                  |
|   Browser   |                      |   BFF Gateway     |
|             |<---------------------|                  |
| HTML + JS   |                      | OAuth2 Client     |
| sem tokens  |---- /api/** -------->| TokenRelay        |
+-------------+                      +---------+--------+
                                              |
                                              | Authorization:
                                              | Bearer access_token
                                              v
                                      +------------------+
                                      | Resource API     |
                                      | Resource Server  |
                                      +------------------+
```

O desenho está simplificado, mas representa a decisão principal do exemplo:

- o browser fala com o BFF usando cookie;
- o browser não manda `Authorization: Bearer`;
- o BFF fala com o Keycloak durante o login;
- o BFF fala com a Resource API usando `Authorization: Bearer`;
- a Resource API valida JWT, audiência e scopes.

## Projetos Deste Repositório

Este repositório tem três partes principais:

- `bff-gateway`: aplicação Spring Boot que serve o front-end, faz login OAuth2, mantém sessão por cookie e usa `TokenRelay`.
- `resource-api`: API protegida que exige JWT válido e scopes `messages:read` / `messages:write`.
- `keycloak`: realm de desenvolvimento local, com client confidencial e usuário de demonstração.

Também existe um front-end estático em `bff-gateway/src/main/resources/static`. Ele é propositalmente simples: HTML, CSS e JavaScript puro, sem React e sem build separado.

## Preparando O Ambiente Local

Você precisa ter Docker e Java 21, ou uma toolchain compatível configurada no Gradle.

Antes de subir os serviços, confira se estas portas estão livres:

```text
8080 -> BFF Gateway
8081 -> Resource API
9090 -> Keycloak
```

No Windows PowerShell:

```powershell
docker compose up -d
.\gradlew.bat clean test bootJar
java -jar resource-api\build\libs\resource-api-0.0.1-SNAPSHOT.jar
java -jar bff-gateway\build\libs\bff-gateway-0.0.1-SNAPSHOT.jar
```

Em Linux/macOS, o comando do Gradle muda:

```bash
./gradlew clean test bootJar
```

Depois acesse:

```text
http://localhost:8080/
```

Credenciais do usuário de demonstração:

```text
Usuário: aluno
Senha: alga123
```

## Por Que O Front E O BFF Ficam Na Mesma Origem?

Para fins didáticos, o front-end é servido pelo próprio BFF em `http://localhost:8080/`.

Isso evita misturar o assunto principal com CORS, subdomínios, configuração de domínio de cookie e detalhes de `SameSite`. Esses temas são importantes, mas não são o foco deste primeiro exemplo.

Aqui queremos enxergar uma coisa com clareza:

```text
Browser -> cookie de sessão -> BFF -> Bearer token -> Resource API
```

## Componentes, Em Linguagem Simples

### Keycloak

É o Authorization Server. Ele autentica o usuário e emite os tokens para o BFF.

Neste exemplo, ele já sobe com:

- realm `bff-example`;
- usuário `aluno`;
- client confidencial `bff-gateway`;
- scopes `messages:read` e `messages:write`.

### BFF Gateway

É o meio de campo entre browser e APIs.

Ele:

- serve o HTML, CSS e JavaScript;
- inicia o login no Keycloak;
- recebe o callback OAuth2;
- cria uma sessão por cookie;
- busca o token associado à sessão;
- encaminha `/api/messages` para a Resource API usando `TokenRelay`;
- invalida a sessão no logout;
- exige CSRF nas operações inseguras.

### Resource API

É a API protegida de verdade.

Ela não conhece a sessão do browser e não confia em cookie do BFF. Para responder, ela exige:

- JWT válido;
- audiência esperada;
- `messages:read` para `GET /messages`;
- `messages:write` para `POST /messages`.

### Front-end

O front-end só conversa com o BFF.

Ele chama:

```js
fetch("/bff/user");
fetch("/api/messages");
fetch("/bff/csrf");
```

E ele não faz isto:

```js
localStorage.getItem("access_token");
sessionStorage.getItem("access_token");
fetch("/api/messages", {
  headers: {
    Authorization: `Bearer ${token}`
  }
});
```

Esse é o ponto principal: o JavaScript não precisa carregar o token OAuth.

## O Que Você Deve Observar

Ao executar o manual, procure estas evidências:

- antes do login, chamadas protegidas retornam `401`;
- depois do login, o browser tem cookie de sessão;
- no DevTools, a chamada para o BFF não tem `Authorization`;
- nos logs do BFF, aparece `Authorization vindo do browser? false`;
- nos logs da Resource API, aparece `Authorization comeca com Bearer? true`;
- `POST /api/messages` sem CSRF retorna `403`;
- `POST /api/messages` com CSRF retorna `201`;
- chamada direta para `http://localhost:8081/messages` retorna `401`;
- depois do logout, a sessão deixa de autenticar.

## Limites Do Exemplo

Este projeto evita alguns assuntos de propósito:

- React;
- banco de dados;
- Redis ou sessão distribuída;
- múltiplas APIs;
- CORS e subdomínios;
- logout global no Keycloak;
- refresh token demonstrado manualmente;
- regras de negócio reais.

Essas coisas podem entrar em uma evolução futura. Aqui, o objetivo é entender bem o fluxo principal antes de colocar mais peças na mesa.

## Próximo Passo

Agora vá para o material guiado:

**[Abrir o manual de execução e evidências](docs/evidencias-bff-token-handler/manual-execucao-evidencias.md)**

Ele é o melhor jeito de acompanhar o exemplo sem ficar só na teoria.
