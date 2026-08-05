package com.algaworks.resourceapi.configuracao;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class ValidadorAudienciaTests {

    private final ValidadorAudiencia validadorAudiencia = new ValidadorAudiencia();

    @Test
    void deveAceitarTokenEmitidoParaResourceApi() {
        Jwt token = criarTokenComAudiencia("resource-api");

        OAuth2TokenValidatorResult resultado = validadorAudiencia.validate(token);

        assertThat(resultado.hasErrors()).isFalse();
    }

    @Test
    void deveRecusarTokenEmitidoParaOutroCliente() {
        Jwt token = criarTokenComAudiencia("outro-cliente");

        OAuth2TokenValidatorResult resultado = validadorAudiencia.validate(token);

        assertThat(resultado.hasErrors()).isTrue();
    }

    @Test
    void deveRecusarTokenSemAudiencia() {
        Jwt token = criarTokenSemAudiencia();

        OAuth2TokenValidatorResult resultado = validadorAudiencia.validate(token);

        assertThat(resultado.hasErrors()).isTrue();
    }

    private Jwt criarTokenComAudiencia(String audiencia) {
        Instant agora = Instant.now();
        return Jwt.withTokenValue("token-de-teste")
                .header("alg", "RS256")
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(300))
                .audience(List.of(audiencia))
                .build();
    }

    private Jwt criarTokenSemAudiencia() {
        Instant agora = Instant.now();
        return Jwt.withTokenValue("token-de-teste")
                .header("alg", "RS256")
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(300))
                .build();
    }
}
