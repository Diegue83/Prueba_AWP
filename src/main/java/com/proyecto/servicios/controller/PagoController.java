package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.EstatusPago;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.pagos.PagoRequest;
import com.proyecto.servicios.model.pagos.PagoResponse;
import com.proyecto.servicios.security.ControlAcceso;
import com.proyecto.servicios.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/pagos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Pagos", description = "Pagos de servicios y recargas con GestoPago")
public class PagoController {

    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final PagoService pagoService;
    private final ControlAcceso controlAcceso;

    public PagoController(PagoService pagoService, ControlAcceso controlAcceso) {
        this.pagoService = pagoService;
        this.controlAcceso = controlAcceso;
    }

    @Operation(
            summary = "Realiza un pago de servicio o recarga",
            description = "Requiere Authorization: Bearer <token>, el encabezado Idempotency-Key (un valor nuevo "
                    + "por operación; repetirlo devuelve el mismo resultado sin volver a cobrar) y el código "
                    + "2FA vigente. Responde 200 si se aplicó, 422 si el proveedor lo rechazó (monto devuelto) "
                    + "y 202 si quedó en verificación con el proveedor.")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> pagar(@RequestHeader(IDEMPOTENCY_KEY) String claveIdempotencia,
                                                 @Valid @RequestBody PagoRequest request) {
        PagoResponse pago = pagoService.pagar(controlAcceso.obtenerClienteAutenticado(), claveIdempotencia, request);
        return responder(pago);
    }

    @Operation(summary = "Consulta el estatus de un pago propio")
    @GetMapping("/{id}")
    public ResponseEntity<GenericResponse> consultar(@PathVariable("id") Long id) {
        PagoResponse pago = pagoService.consultar(controlAcceso.obtenerClienteAutenticado(), id);
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje(pago.mensaje());
        respuesta.setDatos(pago);
        return ResponseEntity.ok(respuesta);
    }

    private ResponseEntity<GenericResponse> responder(PagoResponse pago) {
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(pago.estatus() == EstatusPago.APLICADO ? 0 : 1);
        respuesta.setMensaje(pago.mensaje());
        respuesta.setDatos(pago);
        return ResponseEntity.status(estatusHttp(pago.estatus())).body(respuesta);
    }

    private HttpStatus estatusHttp(EstatusPago estatus) {
        return switch (estatus) {
            case APLICADO -> HttpStatus.OK;
            case RECHAZADO -> HttpStatus.UNPROCESSABLE_ENTITY;
            case PENDIENTE, EN_CONCILIACION, REVISION_MANUAL -> HttpStatus.ACCEPTED;
        };
    }
}
