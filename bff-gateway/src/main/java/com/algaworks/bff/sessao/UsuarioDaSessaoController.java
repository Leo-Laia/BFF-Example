package com.algaworks.bff.sessao;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsuarioDaSessaoController {

    @GetMapping("/bff/user")
    UsuarioDaSessao consultarUsuario(@AuthenticationPrincipal OidcUser usuarioAutenticado) {
        String usuario = usuarioAutenticado.getClaimAsString("preferred_username");
        String nome = usuarioAutenticado.getFullName();
        if (nome == null || nome.isBlank()) {
            nome = usuario;
        }
        return new UsuarioDaSessao(nome, usuario);
    }
}
