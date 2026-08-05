package com.algaworks.bff.seguranca;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOidcLogin;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
class SegurancaBffTests {

    @Autowired
    private ApplicationContext contextoDaAplicacao;

    private WebTestClient clienteWeb;

    @BeforeEach
    void configurarClienteWeb() {
        clienteWeb = WebTestClient.bindToApplicationContext(contextoDaAplicacao)
                .apply(springSecurity())
                .configureClient()
                .build();
    }

    @Test
    void deveResponderUnauthorizedNaApiSemSessao() {
        clienteWeb.get()
                .uri("/api/messages")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deveResponderUnauthorizedAoConsultarUsuarioSemSessao() {
        clienteWeb.get()
                .uri("/bff/user")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deveRetornarDadosDoUsuarioAutenticadoSemExporTokens() {
        clienteWeb.mutateWith(mockOidcLogin()
                        .idToken(token -> token
                                .claim("name", "Aluno AlgaWorks")
                                .claim("preferred_username", "aluno")))
                .get()
                .uri("/bff/user")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.nome").isEqualTo("Aluno AlgaWorks")
                .jsonPath("$.usuario").isEqualTo("aluno")
                .jsonPath("$.access_token").doesNotExist()
                .jsonPath("$.refresh_token").doesNotExist();
    }

    @Test
    void deveDisponibilizarTokenCsrfParaUsuarioAutenticado() {
        clienteWeb.mutateWith(mockOidcLogin())
                .get()
                .uri("/bff/csrf")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.nomeDoHeader").isEqualTo("X-XSRF-TOKEN")
                .jsonPath("$.token").isNotEmpty();
    }

    @Test
    void deveRecusarLogoutSemTokenCsrf() {
        clienteWeb.mutateWith(mockOidcLogin())
                .post()
                .uri("/logout")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void devePermitirLogoutComTokenCsrf() {
        clienteWeb.mutateWith(mockOidcLogin())
                .mutateWith(csrf())
                .post()
                .uri("/logout")
                .exchange()
                .expectStatus().isNoContent();
    }
}
