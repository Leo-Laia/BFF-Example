package com.algaworks.bff.sessao;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsuarioDaSessaoController {

    @GetMapping("/bff/user")
    UsuarioDaSessao consultarUsuario(@AuthenticationPrincipal OidcUser usuarioAutenticado) {
        String nome = usuarioAutenticado.getFullName();
        String usuario = usuarioAutenticado.getClaimAsString("preferred_username");
        return new UsuarioDaSessao(nome, usuario);
    }
}
