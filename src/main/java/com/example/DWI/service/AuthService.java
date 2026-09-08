package com.example.DWI.service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Servicio de autenticación por tokens Bearer para la API REST.
 * Gestiona el ciclo de vida de sesiones y tokens en memoria para clientes REST.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    public record Session(UserDetails user, Instant expiresAt) {}

    public record LoginResponse(String token, String tokenType, Instant expiresAt, String usuario, String rol) {}

    public AuthService(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(String username, String password) {
        UserDetails user;
        try {
            user = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException ex) {
            log.warn("Intento de login fallido: usuario no encontrado ({})", username);
            throw unauthorized();
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Intento de login fallido: credenciales incorrectas para ({})", username);
            throw unauthorized();
        }

        // Limpiar tokens expirados
        sessions.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(Instant.now()));

        // Generar nuevo token opaco con validez de 8 horas
        String token = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(28800);
        sessions.put(token, new Session(user, expiresAt));

        String rol = user.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .orElse("ROLE_USER");

        log.info("Sesión API iniciada para usuario: {} con rol: {}", user.getUsername(), rol);
        return new LoginResponse(token, "Bearer", expiresAt, user.getUsername(), rol);
    }

    public UserDetails authenticate(String token) {
        Session session = sessions.get(token);
        if (session == null) {
            return null;
        }
        if (!session.expiresAt().isAfter(Instant.now())) {
            sessions.remove(token);
            return null;
        }
        return session.user();
    }

    public void logout(String token) {
        sessions.remove(token);
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
    }
}
