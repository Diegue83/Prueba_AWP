package com.proyecto.servicios.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Set;

/** Reglas de negocio de los pagos (prefijo pagos). */
@Component
@ConfigurationProperties(prefix = "pagos")
@Validated
@Getter
@Setter
public class PagosProperties {

    /** Límites para productos de monto abierto (precio 0 en el catálogo). */
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal montoMinimo = new BigDecimal("1.00");

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal montoMaximo = new BigDecimal("10000.00");

    /** 1 recargas, 2 y 4 pagos de servicio, 5 venta por referencia. 30/40 son envíos de dinero y depósitos. */
    @NotEmpty
    private Set<Integer> tiposFrontPermitidos = Set.of(1, 2, 4, 5);

    @NotBlank
    private String zonaHoraria = "America/Mexico_City";

    @Min(1)
    private int conciliacionMinutosPendiente = 5;

    @Min(1)
    private int conciliacionMaxIntentos = 12;

    @Min(1)
    private int conciliacionLote = 50;
}
