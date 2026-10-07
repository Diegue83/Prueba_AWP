package com.proyecto.servicios.model.onboarding;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuerpo de POST /clientes: los datos modificables más lo que solo se captura
 * en el alta (CURP, RFC y la contraseña con la que el cliente iniciará sesión).
 */
@Getter
@Setter
@NoArgsConstructor
public class ClienteRequest extends ClienteDatos {
    private String curp;
    private String rfc;
    private String contrasena;
}
