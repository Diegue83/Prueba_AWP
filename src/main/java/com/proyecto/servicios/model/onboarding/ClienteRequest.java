package com.proyecto.servicios.model.onboarding;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * Request único de cliente. Los campos se validan según la operación: alta,
 * actualización, consulta o baja.
 */
@Getter
@Setter
@NoArgsConstructor
public class ClienteRequest extends ClienteDatos {

    private String curp;
    private String rfc;
    private String contrasena;

    @Positive(message = "id: debe ser un número entero mayor que cero")
    private Integer id;

    private String numeroCuenta;
    private Boolean activo;
    private LocalDate fechaRegistroDesde;
    private LocalDate fechaRegistroHasta;
}
