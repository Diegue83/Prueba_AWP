package com.proyecto.servicios.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SeguridadConfig {

    /**
     * BCrypt cifra la contraseña en un solo sentido (no se puede descifrar) y le
     * agrega una "sal" aleatoria: dos clientes con la misma contraseña guardan hashes distintos.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
