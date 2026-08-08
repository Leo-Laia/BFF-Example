package com.algaworks.bff.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

class EscoposOAuthClientTests {

    @Test
    void deveSolicitarEscoposDaResourceApiNoLogin() {
        Properties propriedades = carregarConfiguracao();
        String escoposConfigurados = propriedades.getProperty(
                "spring.security.oauth2.client.registration.keycloak.scope");

        assertThat(escopos(escoposConfigurados))
                .contains("openid", "messages:read", "messages:write")
                .doesNotContain("profile", "email");
    }

    private Properties carregarConfiguracao() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        return yaml.getObject();
    }

    private String[] escopos(String escoposConfigurados) {
        return Arrays.stream(escoposConfigurados.split(","))
                .map(String::trim)
                .toArray(String[]::new);
    }
}
