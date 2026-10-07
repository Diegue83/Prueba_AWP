package com.proyecto.servicios.model.onboarding;

import lombok.NoArgsConstructor;

/**
 * Cuerpo de PUT /clientes/{id}. No declara CURP, RFC, contraseña ni número de
 * cuenta: como hereda de SolicitudEstricta, enviarlos provoca un 400.
 */
@NoArgsConstructor
public class ClienteUpdateRequest extends ClienteDatos {
}
