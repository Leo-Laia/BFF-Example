package com.algaworks.bff.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

@SpringBootTest
class RotaResourceApiTests {

    @Autowired
    private RouteDefinitionLocator localizadorDeRotas;

    @Test
    void deveDescartarAuthorizationDoBrowserAntesDoTokenRelay() {
        RouteDefinition rotaResourceApi = localizadorDeRotas.getRouteDefinitions()
                .filter(rota -> rota.getId().equals("resource-api"))
                .blockFirst();

        assertThat(rotaResourceApi).isNotNull();
        assertThat(rotaResourceApi.getFilters())
                .extracting(FilterDefinition::getName)
                .containsExactly("RemoveRequestHeader", "TokenRelay", "StripPrefix");
    }
}
