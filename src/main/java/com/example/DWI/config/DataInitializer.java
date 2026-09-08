package com.example.DWI.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.DWI.model.Usuario;
import com.example.DWI.repository.UsuarioRepository;

/**
 * Inicializador de datos del sistema.
 * Se encarga de sembrar el usuario administrador en la base de datos si aún no existe,
 * asegurando que la contraseña sea hasheada con BCrypt.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:${app.admin.username:admin@gmail.com}}")
    private String adminEmail;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!usuarioRepository.existsByCorreo(adminEmail)) {
            Usuario admin = Usuario.builder()
                    .nombre("Administrador del Sistema")
                    .correo(adminEmail)
                    .claveHash(passwordEncoder.encode(adminPassword))
                    .rol("ROLE_ADMIN")
                    .activo(true)
                    .build();

            usuarioRepository.save(admin);
            log.info(">>> Usuario Administrador creado exitosamente en la BD con correo: {}", adminEmail);
        } else {
            log.info(">>> El usuario administrador ({}) ya existe en la base de datos.", adminEmail);
        }
    }
}
