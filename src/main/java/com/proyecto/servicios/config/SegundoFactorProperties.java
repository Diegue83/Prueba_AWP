package com.proyecto.servicios.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Parámetros del segundo factor (prefijo segundo-factor). La clave de cifrado de los
 * secretos TOTP llega por variable de entorno; si no está, el 2FA responde 503 en vez
 * de guardar secretos sin protección.
 */
@Component
@ConfigurationProperties(prefix = "segundo-factor")
@Validated
@Getter
@Setter
public class SegundoFactorProperties {

    private String claveCifrado = "";

    @NotBlank
    private String emisor = "Onboarding";

    @Min(1)
    private int maxIntentos = 5;

    @Min(1)
    private int minutosBloqueo = 15;

    @Min(0)
    @Max(2)
    private int ventanaPasos = 1;
}
