package com.algaworks.bff.configuracao;

import static org.springframework.security.config.Customizer.withDefaults;

import com.algaworks.bff.sessao.ManipuladorLogoutSessao;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authentication.logout.HttpStatusReturningServerLogoutSuccessHandler;
import org.springframework.security.web.server.csrf.WebSessionServerCsrfTokenRepository;

@Configuration
@EnableWebFluxSecurity
public class ConfiguracaoSeguranca {

    @Bean
    SecurityWebFilterChain filtroDeSeguranca(ServerHttpSecurity http,
            ManipuladorLogoutSessao manipuladorDeLogout) {
        return http
                .authorizeExchange(autorizacao -> autorizacao
                        .pathMatchers("/", "/index.html", "/app.js", "/styles.css", "/favicon.ico",
                                "/oauth2/**", "/login/**", "/error")
                        .permitAll()
                        .pathMatchers("/api/**", "/bff/**")
                        .authenticated()
                        .anyExchange()
                        .denyAll())
                .oauth2Login(withDefaults())
                .csrf(csrf -> csrf.csrfTokenRepository(criarRepositorioCsrf()))
                .exceptionHandling(excecoes -> excecoes
                        .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)))
                .logout(logout -> logout
                        .logoutHandler(manipuladorDeLogout)
                        .logoutSuccessHandler(
                                new HttpStatusReturningServerLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .build();
    }

    private WebSessionServerCsrfTokenRepository criarRepositorioCsrf() {
        WebSessionServerCsrfTokenRepository repositorioCsrf =
                new WebSessionServerCsrfTokenRepository();
        repositorioCsrf.setHeaderName("X-XSRF-TOKEN");
        return repositorioCsrf;
    }

}
