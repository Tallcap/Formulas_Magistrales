package com.example.DWI.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Módulo de seguridad para el cifrado y hasheo de contraseñas.
 * Utiliza el algoritmo BCrypt, que incorpora generación automática de sal (salt)
 * y una función de derivación de claves resistente a ataques de fuerza bruta y rainbow tables.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
