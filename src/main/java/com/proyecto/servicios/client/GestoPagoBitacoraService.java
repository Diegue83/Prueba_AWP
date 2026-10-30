package com.proyecto.servicios.client;

import com.proyecto.servicios.entity.gestopago.GestoPagoBitacora;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoBitacoraRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Se llama fuera de las transacciones del pago, así que cada registro se confirma solo y no
 * depende de lo que pase después. Si la bitácora falla no interrumpe un pago que ya está en curso.
 * Nunca recibe el token ni el campo signed; la petición se guarda como el JSON sin cifrar.
 */
@Component
@Slf4j
public class GestoPagoBitacoraService {

    private final GestoPagoBitacoraRepository repository;

    public GestoPagoBitacoraService(GestoPagoBitacoraRepository repository) {
        this.repository = repository;
    }

    public void registrar(String operacion, String upc, String peticion, Integer httpStatus,
                          String respuesta, long duracionMs) {
        try {
            repository.save(GestoPagoBitacora.registrar(operacion, upc, peticion, httpStatus, respuesta, duracionMs));
        } catch (RuntimeException e) {
            log.error("No se pudo guardar la bitácora de GestoPago {} upc={} ({})",
                    operacion, upc, e.getClass().getSimpleName());
        }
    }
}
