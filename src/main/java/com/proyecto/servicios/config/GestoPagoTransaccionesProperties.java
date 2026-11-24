package com.proyecto.servicios.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Parámetros de sendTx/confirmTx (prefijo gestopago.transacciones). La llave y el IV AES
 * los asigna el proveedor y solo llegan por variables de entorno; sin ellos los pagos
 * responden 503 y la aplicación sigue funcionando para lo demás.
 */
@Component
@ConfigurationProperties(prefix = "gestopago.transacciones")
@Getter
@Setter
public class GestoPagoTransaccionesProperties {

    private String llaveAes = "";

    private String ivAes = "";

    private String unidad = "";

    /**
     * Códigos de sendTx que no confirman ni descartan la operación: 82 (tiempo de espera
     * alcanzado) y 03 (el carrier no responde). Se resuelven con confirmTx.
     */
    private Set<String> codigosIndeterminados = Set.of("03", "82");

    /** Códigos de confirmTx que garantizan que la operación NO se aplicó: 70 "La transaccion no fue aplicada". */
    private Set<String> codigosNoAplicada = Set.of("70");

    /** La especificación prohíbe llamar confirmTx antes de 62 s desde el sendTx. */
    private long esperaConfirmacionSegundos = 62;

    /** confirmTx solo responde durante 24 h; después el pago pasa a revisión manual. */
    private long vigenciaConfirmacionHoras = 24;
}
