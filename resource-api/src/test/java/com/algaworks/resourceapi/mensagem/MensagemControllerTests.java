package com.algaworks.resourceapi.mensagem;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwks")
@AutoConfigureMockMvc
class MensagemControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveRecusarConsultaSemToken() throws Exception {
        mockMvc.perform(get("/messages"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveConsultarMensagensComJwtValido() throws Exception {
        mockMvc.perform(get("/messages").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].texto").value("Token protegido pelo BFF"));
    }

    @Test
    void deveAdicionarMensagemComAutorDoJwt() throws Exception {
        mockMvc.perform(post("/messages")
                        .with(jwt().jwt(token -> token.claim("preferred_username", "aluno")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Mensagem criada no teste\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.texto").value("Mensagem criada no teste"))
                .andExpect(jsonPath("$.autor").value("aluno"));
    }
}
