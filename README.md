# BFF Token Handler Example

Exemplo didático de um **Backend for Frontend (BFF) Token Handler** usando Spring Security OAuth2 Client, Spring Cloud Gateway, TokenRelay, uma Resource API protegida e Keycloak como Authorization Server.

O objetivo é demonstrar um front-end que não gerencia tokens OAuth. O navegador não conhece `access_token`, não conhece `refresh_token`, não usa `localStorage` ou `sessionStorage` para autenticação e não envia `Authorization: Bearer` nas chamadas feitas pela aplicação. Quem cuida do login, da sessão, dos tokens e do encaminhamento autenticado para a API é o BFF.

## Problema Que Este Projeto Demonstra

Em aplicações browser-based, é comum surgir a dúvida: onde guardar tokens OAuth com segurança?

Este exemplo mostra uma alternativa em que o front-end deixa de ser o OAuth client. Em vez disso:

- o BFF atua como OAuth2 Client confidencial;
- o browser mantém apenas uma sessão por cookie;
- os tokens OAuth ficam associados à sessão no servidor;
- o front chama apenas endpoints do BFF;
- o BFF adiciona o `Authorization: Bearer` ao chamar a API protegida.

Esse padrão reduz a exposição dos tokens ao JavaScript do navegador. Ele não elimina todos os riscos de uma aplicação web, mas evita que um script malicioso consiga simplesmente ler e exfiltrar tokens armazenados no browser.

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

## Projetos

```text
bff-token-handler-example/
├── docker-compose.yml
├── keycloak/
│   └── bff-example-realm.json
├── bff-gateway/
│   ├── src/main/java/
│   └── src/main/resources/
│       ├── application.yml
│       └── static/
│           ├── index.html
│           └── app.js
├── resource-api/
│   ├── src/main/java/
│   └── src/main/resources/
│       └── application.yml
└── README.md
```

O projeto deve ser intencionalmente pequeno: duas aplicações Spring e um Keycloak pronto via Docker. A ideia não é ensinar Keycloak, React, banco de dados ou regras de negócio. A ideia é mostrar o fluxo de autenticação e encaminhamento de token pelo BFF.

## Decisão Principal: Mesma Origem

Para o MVP, o front-end deve ser servido pelo próprio BFF:

```text
http://localhost:8080/
http://localhost:8080/bff/user
http://localhost:8080/api/messages
```

Isso mantém front-end e BFF na mesma origem. A demonstração fica mais simples porque evita CORS, evita servidor Vite/React separado e reduz distrações com configuração de cookies entre origens diferentes.

Evite, no MVP:

```text
http://localhost:5173  -> Front-end
http://localhost:8080  -> BFF
```

Portas diferentes criam origens diferentes. Subdomínios também ficam fora da primeira versão: eles podem ser considerados mesmo site em algumas regras de cookies, mas ainda são origens diferentes e adicionam CORS, domínio de cookie, `SameSite` e proteção de origem à conversa.

## Componentes

### Authorization Server

O Keycloak deve subir por Docker com um realm importado automaticamente.

Configuração esperada:

- realm `bff-example`;
- servidor em `http://localhost:9090`;
- administração com `admin` / `admin`;
- usuário de demonstração `aluno` / `alga123`;
- client confidencial `bff-gateway` com secret `bff-gateway-secret`;
- fluxo Authorization Code;
- redirect URI `http://localhost:8080/login/oauth2/code/keycloak`;
- configuração exclusiva para desenvolvimento local.

### Resource API

API Spring Boot mínima, atuando como OAuth2 Resource Server.

Endpoints:

```http
GET /messages
POST /messages
```

Responsabilidades:

- validar JWT recebido no header `Authorization`;
- exigir autenticação para os endpoints protegidos;
- opcionalmente validar scopes;
- não conhecer cookies, sessões ou detalhes do BFF.

Não deve ter banco de dados, JPA, Flyway ou regra de negócio real.

### BFF Gateway

Aplicação Spring Boot com Spring Security OAuth2 Client e Spring Cloud Gateway.

Responsabilidades:

- iniciar o login OAuth;
- receber o callback;
- manter a sessão do usuário por cookie;
- manter os tokens OAuth fora do navegador;
- encaminhar `/api/**` para a Resource API;
- aplicar `TokenRelay`;
- expor dados mínimos da sessão;
- responder `401` em `/api/**` e `/bff/**` quando não houver autenticação;
- realizar logout local com CSRF e invalidação explícita da sessão.

Rotas esperadas:

```http
GET  /oauth2/authorization/keycloak
GET  /login/oauth2/code/keycloak
GET  /bff/user
GET  /bff/csrf
GET  /api/messages
POST /api/messages
POST /logout
```

Configuração conceitual:

```yaml
spring:
  cloud.gateway.server.webflux:
    routes:
      - id: resource-api
        uri: http://localhost:8081
        predicates:
          - Path=/api/**
        filters:
          - RemoveRequestHeader=Authorization
          - TokenRelay=
          - StripPrefix=1
```

O BFF descarta qualquer `Authorization` recebido do browser. O `TokenRelay` recupera o access token associado ao usuário autenticado e adiciona o seu próprio `Authorization: Bearer`. O `StripPrefix=1` remove `/api`, portanto o browser chama `/api/messages`, mas a Resource API recebe `/messages`.

### Front-end

O front-end pode ser apenas `index.html` e `app.js`, servidos pelo BFF.

Interface mínima:

```text
BFF Token Handler

Status: não autenticado

[Entrar]
[Consultar usuário]
[Consultar API protegida]
[Enviar mensagem]
[Sair]
```

O JavaScript deve chamar apenas o BFF:

```js
fetch("/api/messages");
```

O JavaScript não deve conter:

```js
localStorage.getItem("access_token");
sessionStorage.getItem("access_token");
fetch("/api/messages", {
  headers: {
    Authorization: `Bearer ${token}`
  }
});
```

Para login, use navegação normal:

```js
window.location.href = "/oauth2/authorization/keycloak";
```

Se uma chamada protegida retornar `401`, o front deve exibir o estado de não autenticado e permitir que o usuário clique em entrar. Não é necessário iniciar o login automaticamente por `fetch`. Para preservar esse contrato, o BFF deve usar `HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)` em `/api/**` e `/bff/**`. O redirecionamento OAuth fica restrito à navegação explícita para `/oauth2/authorization/keycloak`.

## Fluxos Demonstrados

### Login

```text
1. Usuário clica em Entrar.
2. Browser acessa /oauth2/authorization/keycloak.
3. BFF redireciona para o Authorization Server.
4. Usuário autentica no Keycloak.
5. Authorization Server retorna o authorization code.
6. BFF troca o code pelos tokens.
7. BFF associa os tokens à sessão.
8. Browser recebe apenas cookie de sessão.
9. Usuário volta para a página inicial.
```

### Chamada Protegida

```text
1. Front chama GET /api/messages.
2. Browser envia automaticamente o cookie de sessão.
3. BFF encontra o access token associado à sessão.
4. TokenRelay adiciona Authorization: Bearer.
5. BFF encaminha a chamada para /messages na Resource API.
6. Resource API valida o token.
7. Resposta retorna ao front pelo BFF.
```

### Logout

```text
1. Front envia POST /logout com X-XSRF-TOKEN.
2. BFF usa WebSessionServerLogoutHandler para invalidar a sessão local.
3. Cookie de sessão deixa de ser válido.
4. Novas chamadas protegidas retornam 401.
```

Logout global no Authorization Server pode ficar como evolução. Para o MVP, invalidar a sessão local já demonstra o comportamento principal.

## CSRF

Como o browser passa a autenticar usando cookies, uma operação insegura como `POST` deve exigir proteção contra CSRF.

O front pode obter um token CSRF por endpoint ou cookie de apoio e enviá-lo no header:

```js
fetch("/api/messages", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    "X-XSRF-TOKEN": csrfToken
  },
  body: JSON.stringify({
    text: "Nova mensagem"
  })
});
```

Ponto importante: o token CSRF pode ser legível para o JavaScript quando a estratégia exigir isso. O cookie de sessão, por outro lado, deve continuar protegido contra leitura pelo JavaScript.

## Como Validar

### DevTools: Browser Para BFF

Na aba Network, abra a chamada:

```text
GET http://localhost:8080/api/messages
```

Resultado esperado nos request headers:

```text
Cookie: BFFSESSION=...
```

E também:

```text
Authorization: ausente
```

Na aba Application:

```text
Local Storage: sem access_token e sem refresh_token
Session Storage: sem access_token e sem refresh_token
Cookies: cookie de sessão presente
```

### DevTools: POST Com CSRF

Na chamada:

```text
POST http://localhost:8080/api/messages
```

Resultado esperado:

```text
Cookie: BFFSESSION=...
X-XSRF-TOKEN: ...
Authorization: ausente
```

### Logs: Chegada No BFF

Para fins didáticos, o BFF pode registrar:

```text
BFF recebeu GET /api/messages
Authorization vindo do browser? false
Cookie de sessao presente? true
```

### Logs: Chegada Na Resource API

Para fins didáticos, a Resource API pode registrar:

```text
Resource API recebeu GET /messages
Authorization presente? true
Authorization começa com Bearer? true
```

### Chamada Direta Na API

Sem passar pelo BFF:

```bash
curl -i http://localhost:8081/messages
```

Resultado esperado:

```text
HTTP/1.1 401 Unauthorized
```

## Critérios De Aceite

O exemplo estará concluído quando for possível demonstrar que:

- o usuário consegue fazer login;
- o navegador recebe cookie de sessão;
- não existe token OAuth no `localStorage`, `sessionStorage` ou código JavaScript;
- a chamada do navegador para o BFF não contém `Authorization: Bearer`;
- a chamada do BFF para a API contém `Authorization: Bearer`;
- a API retorna `401` quando chamada diretamente sem token;
- uma operação `POST` exige token CSRF válido;
- após o logout, a chamada protegida deixa de funcionar.

## Fora Do Escopo Do MVP

Para manter o exemplo pequeno:

- integração com AlgaShop;
- React;
- banco de dados;
- Redis;
- sessões distribuídas;
- múltiplas APIs;
- descoberta de serviços;
- refresh token demonstrado manualmente;
- API composition;
- autorização complexa;
- deploy em produção;
- subdomínios e CORS;
- customização da tela de login;
- logout global OIDC;
- observabilidade completa.

Também não deve existir rota genérica que aceite qualquer URL de destino. Um BFF deve encaminhar apenas para APIs, hosts e caminhos previamente conhecidos, evitando que ele seja usado para enviar tokens a destinos arbitrários.

## Resultado Esperado

Este projeto deve servir como conteúdo de apoio para responder:

> Como implementar um front-end que não gerencia tokens, deixando login, armazenamento dos tokens e chamadas às APIs sob responsabilidade do BFF?

A resposta prática é este MVP: um front-end mínimo, servido pelo próprio BFF, autenticado por sessão e sem acesso aos tokens OAuth, com o Gateway encaminhando chamadas para uma API protegida usando `TokenRelay`.
