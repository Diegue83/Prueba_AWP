package com.proyecto.servicios.model.pagos;

/**
 * Se entrega una sola vez al registrar el segundo factor: el cliente lo captura o escanea
 * (uriOtpauth como QR) en su app autenticadora. Después ya no se puede volver a consultar.
 */
public record SegundoFactorRegistroResponse(String secreto, String uriOtpauth) {
}
