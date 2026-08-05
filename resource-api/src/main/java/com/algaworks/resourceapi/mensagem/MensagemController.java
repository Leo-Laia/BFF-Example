package com.algaworks.resourceapi.mensagem;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/messages")
public class MensagemController {

    private final List<Mensagem> mensagens = new CopyOnWriteArrayList<>(List.of(
            new Mensagem(UUID.fromString("8f645389-0c0f-49a8-a78f-d66dc5eb10b0"),
                    "Token protegido pelo BFF", "sistema")));

    @GetMapping
    List<Mensagem> consultarMensagens() {
        return List.copyOf(mensagens);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Mensagem adicionarMensagem(@RequestBody NovaMensagem novaMensagem,
            @AuthenticationPrincipal Jwt usuarioAutenticado) {
        validarTextoDaMensagem(novaMensagem.texto());

        String nomeDoAutor = usuarioAutenticado.getClaimAsString("preferred_username");
        Mensagem mensagemCriada = new Mensagem(UUID.randomUUID(), novaMensagem.texto(), nomeDoAutor);
        mensagens.add(mensagemCriada);
        return mensagemCriada;
    }

    private void validarTextoDaMensagem(String texto) {
        boolean textoNaoFoiInformado = texto == null || texto.isBlank();
        if (textoNaoFoiInformado) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o texto da mensagem");
        }
    }
}
