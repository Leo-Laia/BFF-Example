package com.algaworks.bff.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ConteudoEstaticoTests {

    @Test
    void javascriptNaoDeveGerenciarTokensOAuth() throws IOException {
        String javascript = lerArquivo("static/app.js");

        assertThat(javascript)
                .doesNotContain("localStorage")
                .doesNotContain("sessionStorage")
                .doesNotContain("access_token")
                .doesNotContain("refresh_token")
                .doesNotContain("Authorization");
    }

    @Test
    void javascriptDeveChamarSomenteCaminhosDoBff() throws IOException {
        String javascript = lerArquivo("static/app.js");

        assertThat(javascript)
                .doesNotContain("http://")
                .doesNotContain("https://")
                .contains("/bff/user", "/bff/csrf", "/api/messages", "/logout");
    }

    private String lerArquivo(String caminho) throws IOException {
        ClassPathResource arquivo = new ClassPathResource(caminho);
        return arquivo.getContentAsString(StandardCharsets.UTF_8);
    }
}
