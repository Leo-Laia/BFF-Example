package com.algaworks.bff.sessao;

import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@RestController
public class TokenCsrfController {

    @GetMapping("/bff/csrf")
    Mono<TokenCsrf> consultarTokenCsrf(ServerWebExchange exchange) {
        Mono<CsrfToken> tokenCsrf = exchange.getAttribute(CsrfToken.class.getName());
        return tokenCsrf.map(token -> new TokenCsrf(token.getHeaderName(), token.getToken()));
    }
}
