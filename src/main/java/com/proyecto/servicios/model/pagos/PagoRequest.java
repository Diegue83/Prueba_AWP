package com.proyecto.servicios.model.pagos;

import com.proyecto.servicios.model.onboarding.SolicitudEstricta;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Solicitud de pago. El monto viaja para que el cliente confirme lo que paga, pero
 * nunca se confía en él: se compara contra el precio del catálogo.
 */
@Getter
@Setter
@NoArgsConstructor
public class PagoRequest extends SolicitudEstricta {

    @NotBlank(message = "numeroCuenta: es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "numeroCuenta: debe contener exactamente 10 dígitos")
    private String numeroCuenta;

    @NotNull(message = "idServicio: es obligatorio")
    @Positive(message = "idServicio: debe ser mayor que cero")
    private Integer idServicio;

    @NotNull(message = "idProducto: es obligatorio")
    @Positive(message = "idProducto: debe ser mayor que cero")
    private Integer idProducto;

    @NotNull(message = "monto: es obligatorio")
    @DecimalMin(value = "0.01", message = "monto: debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "monto: máximo 10 enteros y 2 decimales")
    private BigDecimal monto;

    @Pattern(regexp = "^\\d{10}$", message = "telefono: debe contener exactamente 10 dígitos")
    private String telefono;

    @Pattern(regexp = "^[A-Za-z0-9-]{4,40}$",
            message = "referencia: entre 4 y 40 caracteres, solo letras, números y guiones")
    private String referencia;

    @NotBlank(message = "codigo2fa: es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "codigo2fa: debe contener exactamente 6 dígitos")
    private String codigo2fa;
}
