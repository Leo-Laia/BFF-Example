package com.algaworks.bff.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.algaworks.bff.sessao.ManipuladorLogoutSessao;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.mock.web.server.MockWebSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.web.server.handler.DefaultWebFilterChain;

class ManipuladorLogoutSessaoTests {

    private final ManipuladorLogoutSessao manipuladorLogout = new ManipuladorLogoutSessao();

    @Test
    void deveInvalidarAWebSessionUsadaNoLogout() {
        MockWebSession sessaoAutenticada = new MockWebSession();
        sessaoAutenticada.start();
        MockServerWebExchange exchange = MockServerWebExchange
                .builder(MockServerHttpRequest.post("/logout"))
                .session(sessaoAutenticada)
                .build();
        WebFilterExchange filtroExchange = new WebFilterExchange(exchange,
                new DefaultWebFilterChain(entrada -> entrada.getResponse().setComplete(), List.of()));
        TestingAuthenticationToken autenticacao = new TestingAuthenticationToken("aluno", null);

        manipuladorLogout.logout(filtroExchange, autenticacao).block();

        assertThat(sessaoAutenticada.isExpired()).isTrue();
    }
}
