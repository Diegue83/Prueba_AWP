package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest extends SolicitudEstricta {
    @NotBlank(message = "correo: es obligatorio")
    @Email(message = "correo: debe tener un formato válido")
    private String correo;
    @NotBlank(message = "contrasena: es obligatoria")
    @Size(max = 72, message = "contrasena: admite máximo 72 caracteres")
    private String contrasena;
}
