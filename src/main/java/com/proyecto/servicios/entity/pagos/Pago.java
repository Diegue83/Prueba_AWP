package com.proyecto.servicios.entity.pagos;

import com.proyecto.servicios.enums.EstatusPago;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Pago a GestoPago. Igual que Cuenta, no expone setters: solo cambia de estado con los
 * métodos de transición, que rechazan saltos inválidos (por ejemplo, reembolsar un pago aplicado).
 */
@Entity
@Table(name = "pagos", uniqueConstraints = {
        @UniqueConstraint(name = "uq_pagos_upc", columnNames = "upc"),
        @UniqueConstraint(name = "uq_pagos_idempotencia", columnNames = {"cliente_id", "clave_idempotencia"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false, updatable = false)
    private Integer clienteId;

    @Column(name = "cuenta_id", nullable = false, updatable = false)
    private Integer cuentaId;

    @Column(name = "id_producto", nullable = false, updatable = false)
    private Integer idProducto;

    @Column(name = "id_servicio", nullable = false, updatable = false)
    private Integer idServicio;

    @Column(name = "monto", nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "intentos_conciliacion", nullable = false)
    private short intentosConciliacion;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estatus", nullable = false, columnDefinition = "TEXT")
    private EstatusPago estatus;

    @Column(name = "clave_idempotencia", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String claveIdempotencia;

    @Column(name = "upc", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String upc;

    @Column(name = "hora_local", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String horaLocal;

    @Column(name = "telefono", updatable = false, columnDefinition = "TEXT")
    private String telefono;

    @Column(name = "referencia", updatable = false, columnDefinition = "TEXT")
    private String referencia;

    @Column(name = "codigo_respuesta", columnDefinition = "TEXT")
    private String codigoRespuesta;

    @Column(name = "mensaje_respuesta", columnDefinition = "TEXT")
    private String mensajeRespuesta;

    @Column(name = "num_autorizacion", columnDefinition = "TEXT")
    private String numAutorizacion;

    @Column(name = "id_tx", columnDefinition = "TEXT")
    private String idTx;

    @Builder
    private static Pago crear(Integer clienteId, Integer cuentaId, Integer idProducto, Integer idServicio,
                              BigDecimal monto, String claveIdempotencia, String upc, String horaLocal,
                              String telefono, String referencia) {
        Pago pago = new Pago();
        pago.clienteId = clienteId;
        pago.cuentaId = cuentaId;
        pago.idProducto = idProducto;
        pago.idServicio = idServicio;
        pago.monto = monto.setScale(2, RoundingMode.UNNECESSARY);
        pago.claveIdempotencia = claveIdempotencia;
        pago.upc = upc;
        pago.horaLocal = horaLocal;
        pago.telefono = telefono;
        pago.referencia = referencia;
        pago.estatus = EstatusPago.PENDIENTE;
        return pago;
    }

    /** Mismos datos de negocio: un reintento con la misma clave de idempotencia es legítimo. */
    public boolean mismaSolicitud(Integer cuentaIdSolicitud, Integer idProductoSolicitud, BigDecimal montoSolicitud,
                                  String telefonoSolicitud, String referenciaSolicitud) {
        return cuentaId.equals(cuentaIdSolicitud)
                && idProducto.equals(idProductoSolicitud)
                && monto.compareTo(montoSolicitud) == 0
                && Objects.equals(telefono, telefonoSolicitud)
                && Objects.equals(referencia, referenciaSolicitud);
    }

    public void aplicar(String codigo, String mensaje, String numAutorizacion, String idTx) {
        exigirNoFinal();
        this.estatus = EstatusPago.APLICADO;
        registrarRespuesta(codigo, mensaje);
        this.numAutorizacion = numAutorizacion;
        this.idTx = idTx;
    }

    public void rechazar(String codigo, String mensaje) {
        exigirNoFinal();
        this.estatus = EstatusPago.RECHAZADO;
        registrarRespuesta(codigo, mensaje);
    }

    public void enviarAConciliacion() {
        exigirNoFinal();
        this.estatus = EstatusPago.EN_CONCILIACION;
    }

    public void registrarIntentoConciliacion(int maxIntentos) {
        exigirNoFinal();
        intentosConciliacion++;
        estatus = intentosConciliacion >= maxIntentos ? EstatusPago.REVISION_MANUAL : EstatusPago.EN_CONCILIACION;
    }

    private void registrarRespuesta(String codigo, String mensaje) {
        this.codigoRespuesta = codigo;
        this.mensajeRespuesta = mensaje;
    }

    private void exigirNoFinal() {
        if (estatus.esFinal()) {
            throw new IllegalStateException("El pago " + id + " ya está " + estatus);
        }
    }

    @PrePersist
    void alCrear() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = fechaCreacion;
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = LocalDateTime.now();
    }
}
