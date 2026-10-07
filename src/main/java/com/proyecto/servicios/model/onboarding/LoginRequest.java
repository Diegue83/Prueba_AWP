package com.proyecto.servicios.model.onboarding;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest extends SolicitudEstricta {
    private String correo;
    private String contrasena;
}
