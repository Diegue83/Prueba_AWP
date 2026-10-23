package com.proyecto.servicios.model.onboarding;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Solo transporta datos: las reglas están en validation/ClienteValidador. */
@Getter
@Setter
@NoArgsConstructor
public class DomicilioRequest extends SolicitudEstricta {
    @NotBlank(message = "domicilio.calle: es obligatoria")
    @Size(min = 2, max = 100, message = "domicilio.calle: debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9 .,#&'/-]+$",
            message = "domicilio.calle: contiene caracteres no permitidos")
    private String calle;
    @NotBlank(message = "domicilio.numeroExterior: es obligatorio")
    @Size(max = 10, message = "domicilio.numeroExterior: admite máximo 10 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9 .,#&'/-]+$",
            message = "domicilio.numeroExterior: contiene caracteres no permitidos")
    private String numeroExterior;
    @Size(max = 10, message = "domicilio.numeroInterior: admite máximo 10 caracteres")
    @Pattern(regexp = "^(?:|[A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9 .,#&'/-]+)$",
            message = "domicilio.numeroInterior: contiene caracteres no permitidos")
    private String numeroInterior;
    @NotBlank(message = "domicilio.colonia: es obligatoria")
    @Size(min = 2, max = 80, message = "domicilio.colonia: debe tener entre 2 y 80 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9 .,#&'/-]+$",
            message = "domicilio.colonia: contiene caracteres no permitidos")
    private String colonia;
    @NotBlank(message = "domicilio.municipio: es obligatorio")
    @Size(min = 2, max = 80, message = "domicilio.municipio: debe tener entre 2 y 80 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9 .,#&'/-]+$",
            message = "domicilio.municipio: contiene caracteres no permitidos")
    private String municipio;
    @NotBlank(message = "domicilio.estado: es obligatorio")
    @Size(min = 2, max = 50, message = "domicilio.estado: debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?: [A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$",
            message = "domicilio.estado: solo admite letras y espacios")
    private String estado;
    @NotBlank(message = "domicilio.codigoPostal: es obligatorio")
    @Pattern(regexp = "^\\d{5}$", message = "domicilio.codigoPostal: debe contener exactamente 5 dígitos")
    private String codigoPostal;
    @NotBlank(message = "domicilio.pais: es obligatorio")
    @Size(min = 2, max = 50, message = "domicilio.pais: debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?: [A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$",
            message = "domicilio.pais: solo admite letras y espacios")
    private String pais;
}
