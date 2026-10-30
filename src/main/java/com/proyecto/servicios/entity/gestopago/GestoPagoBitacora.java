package com.proyecto.servicios.entity.gestopago;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Evidencia de cada intercambio con GestoPago, como exige la especificación de PuntoRed. */
@Entity
@Table(name = "gestopago_bitacora")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GestoPagoBitacora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    @Column(name = "duracion_ms", nullable = false, updatable = false)
    private Integer duracionMs;

    @Column(name = "http_status", updatable = false)
    private Integer httpStatus;

    @Column(name = "operacion", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String operacion;

    @Column(name = "upc", updatable = false, columnDefinition = "TEXT")
    private String upc;

    @Column(name = "peticion", updatable = false, columnDefinition = "TEXT")
    private String peticion;

    @Column(name = "respuesta", updatable = false, columnDefinition = "TEXT")
    private String respuesta;

    public static GestoPagoBitacora registrar(String operacion, String upc, String peticion, Integer httpStatus,
                                              String respuesta, long duracionMs) {
        GestoPagoBitacora registro = new GestoPagoBitacora();
        registro.fecha = LocalDateTime.now();
        registro.operacion = operacion;
        registro.upc = upc;
        registro.peticion = peticion;
        registro.httpStatus = httpStatus;
        registro.respuesta = respuesta;
        registro.duracionMs = (int) Math.min(duracionMs, Integer.MAX_VALUE);
        return registro;
    }
}
