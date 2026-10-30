package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.service.PagoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Revisa con confirmTx los pagos que quedaron sin resultado: los EN_CONCILIACION y los
 * PENDIENTE viejos (la aplicación pudo caerse entre retener el saldo y llamar al proveedor).
 */
@Component
@Slf4j
public class ConciliacionPagosJob {

    private final PagoService pagoService;

    public ConciliacionPagosJob(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @Scheduled(fixedDelayString = "${pagos.conciliacion-intervalo-ms:300000}",
            initialDelayString = "${pagos.conciliacion-intervalo-ms:300000}")
    public void conciliar() {
        int revisados = pagoService.conciliarPendientes();
        if (revisados > 0) {
            log.info("Conciliación de pagos: {} revisados", revisados);
        }
    }
}
