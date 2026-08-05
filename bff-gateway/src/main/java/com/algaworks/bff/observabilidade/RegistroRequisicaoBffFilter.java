package com.algaworks.bff.observabilidade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class RegistroRequisicaoBffFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RegistroRequisicaoBffFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        registrarRequisicaoParaApi(exchange);
        return chain.filter(exchange);
    }

    private void registrarRequisicaoParaApi(ServerWebExchange exchange) {
        String caminho = exchange.getRequest().getPath().value();
        boolean requisicaoParaApi = caminho.startsWith("/api/");
        if (!requisicaoParaApi) {
            return;
        }

        HttpHeaders cabecalhos = exchange.getRequest().getHeaders();
        boolean authorizationVeioDoBrowser = cabecalhos.containsHeader(HttpHeaders.AUTHORIZATION);
        boolean cookieDeSessaoPresente = exchange.getRequest().getCookies().containsKey("BFFSESSION");

        logger.info("BFF recebeu {} {} | Authorization vindo do browser? {} | Cookie de sessao presente? {}",
                exchange.getRequest().getMethod(), caminho,
                authorizationVeioDoBrowser, cookieDeSessaoPresente);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
