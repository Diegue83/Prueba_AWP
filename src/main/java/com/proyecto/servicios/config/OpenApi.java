package com.proyecto.servicios.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Habilita el botón "Authorize" de Swagger con el token de sesión que entrega POST /login.
 * Sin servidor fijo: Swagger usa el host desde el que se abre (local o Render).
 */
@Configuration
public class OpenApi {

    public static final String TOKEN_SESION = "tokenSesion";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("Onboarding y pagos GestoPago").version("1.0"))
                .components(new Components().addSecuritySchemes(TOKEN_SESION, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token de sesión de POST /login (no es el token de GestoPago)")))
                .addSecurityItem(new SecurityRequirement().addList(TOKEN_SESION));
    }
}
