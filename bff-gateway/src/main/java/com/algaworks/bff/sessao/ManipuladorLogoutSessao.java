package com.algaworks.bff.sessao;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.SecurityContextServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.WebSessionServerLogoutHandler;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class ManipuladorLogoutSessao implements ServerLogoutHandler {

    private final SecurityContextServerLogoutHandler limparContextoDeSeguranca =
            new SecurityContextServerLogoutHandler();
    private final WebSessionServerLogoutHandler invalidarSessao = new WebSessionServerLogoutHandler();

    @Override
    public Mono<Void> logout(WebFilterExchange exchange, Authentication authentication) {
        return limparContextoDeSeguranca.logout(exchange, authentication)
                .then(invalidarSessao.logout(exchange, authentication));
    }
}
