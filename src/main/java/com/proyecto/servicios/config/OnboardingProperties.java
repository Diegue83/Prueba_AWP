package com.proyecto.servicios.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * Parámetros del onboarding leídos de application.properties (prefijo onboarding.cuenta).
 * Con @Validated la regla "el saldo inicial no puede ser negativo" se revisa al arrancar:
 * si alguien configura un valor negativo, la aplicación no levanta.
 */
@Component
@ConfigurationProperties(prefix = "onboarding.cuenta")
@Validated
@Getter
@Setter
public class OnboardingProperties {

    @NotNull
    @PositiveOrZero(message = "onboarding.cuenta.saldo-inicial no puede ser negativo")
    private BigDecimal saldoInicial = BigDecimal.ZERO;
}
