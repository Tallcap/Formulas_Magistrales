package com.example.DWI.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.DWI.service.AuthService;

/**
 * Configuración de Seguridad de la Aplicación.
 * Gestiona dos cadenas de filtros:
 * 1. API REST (/api/**): Autenticación basada en Token Bearer sin estado (stateless).
 * 2. Vistas Web (/ y rutas Thymeleaf): Autenticación por formulario de sesión tradicional (/login).
 */
@Configuration
public class SecurityConfig {

/**
 * Cadena de seguridad para los endpoints REST bajo /api/**
 */
@Bean
@Order(1)
public SecurityFilterChain apiSecurity(HttpSecurity http, AuthService auth) throws Exception {
        return http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(apiAuthenticationEntryPoint())
                        .accessDeniedHandler(apiAccessDeniedHandler())
                )
                .addFilterBefore(
                        new BearerTokenFilter(auth),
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
}

/**
 * Cadena de seguridad para la aplicación web (Thymeleaf, Dashboard, Formularios)
 */
@Bean
@Order(2)
public SecurityFilterChain webSecurity(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/login", "/error", "/css/**", "/js/**", "/webjars/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .build();
}

// =========================================================================
// MANEJADORES DE EXCEPCIONES PARA LA API REST
// =========================================================================

private AuthenticationEntryPoint apiAuthenticationEntryPoint() {
        return (request, response, ex) -> {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":401,\"mensaje\":\"Token ausente, inválido o vencido\",\"errores\":{}}");
        };
}

private AccessDeniedHandler apiAccessDeniedHandler() {
        return (request, response, ex) -> {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":403,\"mensaje\":\"Acceso denegado\",\"errores\":{}}");
        };
}
}
