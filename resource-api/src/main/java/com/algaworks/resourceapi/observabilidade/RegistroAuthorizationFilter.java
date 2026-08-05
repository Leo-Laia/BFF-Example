package com.algaworks.resourceapi.observabilidade;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RegistroAuthorizationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RegistroAuthorizationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        registrarAuthorizationDaRequisicao(request);
        filterChain.doFilter(request, response);
    }

    private void registrarAuthorizationDaRequisicao(HttpServletRequest request) {
        boolean requisicaoParaMensagens = request.getRequestURI().startsWith("/messages");
        if (!requisicaoParaMensagens) {
            return;
        }

        String authorization = request.getHeader("Authorization");
        boolean authorizationPresente = authorization != null;
        boolean authorizationEhBearer = authorizationPresente && authorization.startsWith("Bearer ");

        logger.info("Resource API recebeu {} {} | Authorization presente? {} | Authorization comeca com Bearer? {}",
                request.getMethod(), request.getRequestURI(), authorizationPresente, authorizationEhBearer);
    }
}
