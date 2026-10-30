package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.pagos.Pago;
import com.proyecto.servicios.enums.EstatusPago;
import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.ClienteInactivoException;
import com.proyecto.servicios.exception.CuentaInactivaException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.PagoNoEncontradoException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResultado;
import com.proyecto.servicios.model.pagos.PagoValidado;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.pagos.PagoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transacciones cortas de base de datos alrededor de la llamada a GestoPago. Ninguna
 * mantiene un bloqueo mientras se espera al proveedor (hasta 62 s): el saldo se retiene
 * y se confirma después, en transacciones separadas.
 */
@Service
@Slf4j
public class PagoTransacciones {

    private final CuentaRepository cuentaRepository;
    private final PagoRepository pagoRepository;

    public PagoTransacciones(CuentaRepository cuentaRepository, PagoRepository pagoRepository) {
        this.cuentaRepository = cuentaRepository;
        this.pagoRepository = pagoRepository;
    }

    /**
     * Bloquea la cuenta, valida titular, estatus y saldo, y descuenta el total (monto más
     * comisión) en la misma transacción en la que se crea el pago: o pasan las dos cosas o ninguna.
     */
    @Transactional
    public Pago registrarYRetener(Integer clienteId, String numeroCuenta, PagoValidado validado,
                                  String claveIdempotencia, String upc, String horaLocal) {
        Cuenta cuenta = cuentaRepository.bloquearPorNumero(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
        if (!cuenta.getCliente().getId().equals(clienteId)) {
            throw new AccesoDenegadoException();
        }
        if (!cuenta.getCliente().isActivo()) {
            throw new ClienteInactivoException("El cliente está dado de baja y no puede realizar pagos");
        }
        if (!cuenta.estaActiva()) {
            throw new CuentaInactivaException();
        }
        if (!cuenta.tieneSaldoPara(validado.total())) {
            throw new SaldoInsuficienteException();
        }
        cuenta.retener(validado.total());

        Pago pago = Pago.builder()
                .clienteId(clienteId)
                .cuentaId(cuenta.getId())
                .idProducto(validado.idProducto())
                .idServicio(validado.idServicio())
                .tipoFront(validado.tipoFront())
                .monto(validado.monto())
                .comision(validado.comision())
                .claveIdempotencia(claveIdempotencia)
                .upc(upc)
                .horaLocal(horaLocal)
                .telefono(validado.telefono())
                .referencia(validado.referencia())
                .build();
        return pagoRepository.saveAndFlush(pago);
    }

    @Transactional
    public Pago aplicar(Long pagoId, GestoPagoTxResultado resultado) {
        Pago pago = bloquear(pagoId);
        if (pago.getEstatus().esFinal()) {
            return pago;
        }
        pago.aplicar(resultado.codigo(), resultado.mensaje(), resultado.numAutorizacion(), resultado.idTx());
        log.info("Pago aplicado idPago={} upc={}", pago.getId(), pago.getUpc());
        return pago;
    }

    @Transactional
    public Pago rechazarYReembolsar(Long pagoId, GestoPagoTxResultado resultado) {
        Pago pago = bloquear(pagoId);
        if (pago.getEstatus().esFinal()) {
            return pago;
        }
        Cuenta cuenta = cuentaRepository.bloquearPorId(pago.getCuentaId())
                .orElseThrow(() -> new IllegalStateException("La cuenta del pago " + pagoId + " ya no existe"));
        cuenta.reembolsar(pago.total());
        pago.rechazar(resultado.codigo(), resultado.mensaje());
        log.info("Pago rechazado y reembolsado idPago={} upc={} código={}",
                pago.getId(), pago.getUpc(), resultado.codigo());
        return pago;
    }

    @Transactional
    public Pago enviarAConciliacion(Long pagoId) {
        Pago pago = bloquear(pagoId);
        if (!pago.getEstatus().esFinal()) {
            pago.enviarAConciliacion();
            log.warn("Pago en conciliación idPago={} upc={}: no se pudo confirmar con el proveedor",
                    pago.getId(), pago.getUpc());
        }
        return pago;
    }

    @Transactional
    public Pago registrarIntentoConciliacion(Long pagoId, int maxIntentos) {
        Pago pago = bloquear(pagoId);
        if (!pago.getEstatus().esFinal()) {
            pago.registrarIntentoConciliacion(maxIntentos);
            if (pago.getEstatus() == EstatusPago.REVISION_MANUAL) {
                log.error("Pago requiere revisión manual idPago={} upc={}: se agotaron {} intentos de confirmTx",
                        pago.getId(), pago.getUpc(), maxIntentos);
            }
        }
        return pago;
    }

    @Transactional
    public Pago marcarRevisionManual(Long pagoId) {
        Pago pago = bloquear(pagoId);
        if (!pago.getEstatus().esFinal() && pago.getEstatus() != EstatusPago.REVISION_MANUAL) {
            pago.marcarRevisionManual();
            log.error("Pago requiere revisión manual idPago={} upc={}: venció el plazo de 24 h de confirmTx",
                    pago.getId(), pago.getUpc());
        }
        return pago;
    }

    private Pago bloquear(Long pagoId) {
        return pagoRepository.bloquearPorId(pagoId).orElseThrow(() -> new PagoNoEncontradoException(pagoId));
    }
}
