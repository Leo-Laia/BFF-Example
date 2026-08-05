package com.algaworks.resourceapi.configuracao;

import java.util.List;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class ValidadorAudiencia implements OAuth2TokenValidator<Jwt> {

    private static final String AUDIENCIA_ESPERADA = "resource-api";

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        List<String> audiencias = token.getAudience();
        boolean audienciasForamInformadas = audiencias != null;
        boolean tokenFoiEmitidoParaResourceApi = audienciasForamInformadas
                && audiencias.contains(AUDIENCIA_ESPERADA);
        if (tokenFoiEmitidoParaResourceApi) {
            return OAuth2TokenValidatorResult.success();
        }

        OAuth2Error erro = new OAuth2Error("invalid_token",
                "Token nao foi emitido para a Resource API", null);
        return OAuth2TokenValidatorResult.failure(erro);
    }
}
