package com.proyecto.servicios.model.pagos;

import com.proyecto.servicios.model.onboarding.SolicitudEstricta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReferenciaRequest extends SolicitudEstricta {

    @NotNull(message = "idServicio: es obligatorio")
    @Positive(message = "idServicio: debe ser mayor que cero")
    private Integer idServicio;

    @NotNull(message = "idProducto: es obligatorio")
    @Positive(message = "idProducto: debe ser mayor que cero")
    private Integer idProducto;

    @NotBlank(message = "referencia: es obligatoria")
    @Pattern(regexp = "^[A-Za-z0-9-]{4,40}$",
            message = "referencia: entre 4 y 40 caracteres, solo letras, números y guiones")
    private String referencia;
}
