package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class ConsultaClienteRequest extends SolicitudEstricta {

    @Positive(message = "id: debe ser un número entero mayor que cero")
    private Integer id;

    @Pattern(
            regexp = "(?i)^[A-Z][AEIOUX][A-Z]{2}\\d{6}[HMX]"
                    + "(AS|BC|BS|CC|CL|CM|CS|CH|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QT|QR|SP|SL|SR|TC|TS|TL|VZ|YN|ZS|NE)"
                    + "[B-DF-HJ-NP-TV-Z]{3}[A-Z\\d]\\d$",
            message = "curp: debe tener un formato válido")
    @Size(max = 18, message = "curp: admite máximo 18 caracteres")
    private String curp;

    @Pattern(
            regexp = "(?i)^[A-ZÑ&]{3,4}\\d{6}[A-Z\\d]{3}$",
            message = "rfc: debe tener un formato válido")
    @Size(max = 13, message = "rfc: admite máximo 13 caracteres")
    private String rfc;

    @Email(message = "correo: debe tener un formato válido")
    @Size(max = 100, message = "correo: admite máximo 100 caracteres")
    private String correo;

    @Pattern(regexp = "^\\d{10}$", message = "numeroCuenta: debe contener exactamente 10 dígitos")
    private String numeroCuenta;

    private Boolean activo;

    private LocalDate fechaRegistroDesde;

    private LocalDate fechaRegistroHasta;
}
