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

    /** Límites del monto en pagos de servicio, donde lo define el recibo y no el catálogo. */
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal montoMinimo = new BigDecimal("1.00");

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal montoMaximo = new BigDecimal("10000.00");

    /**
     * Solo 1 (recargas) y 2 (pagos de servicio) son ventas con sendTx. 4 y 5 son consultas de
     * referencia; 30, 31 y 40 son operaciones de efectivo y envío de dinero.
     */
    @NotEmpty
    private Set<Integer> tiposFrontPermitidos = Set.of(1, 2);

    @NotBlank
    private String zonaHoraria = "America/Mexico_City";

    @Min(1)
    private int conciliacionMinutosPendiente = 5;

    @Min(1)
    private int conciliacionMaxIntentos = 12;

    @Min(1)
    private int conciliacionLote = 50;
}
