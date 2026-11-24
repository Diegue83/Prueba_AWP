package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.pagos.CodigoSegundoFactorRequest;
import com.proyecto.servicios.security.ControlAcceso;
import com.proyecto.servicios.service.SegundoFactorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/2fa", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Segundo factor", description = "Registro y activación del código TOTP para autorizar pagos")
public class SegundoFactorController {

    private final SegundoFactorService segundoFactorService;
    private final ControlAcceso controlAcceso;

    public SegundoFactorController(SegundoFactorService segundoFactorService, ControlAcceso controlAcceso) {
        this.segundoFactorService = segundoFactorService;
        this.controlAcceso = controlAcceso;
    }

    @Operation(
            summary = "Genera el secreto TOTP del cliente autenticado",
            description = "Devuelve el secreto y la URI otpauth para registrarlos en una app autenticadora. "
                    + "Queda pendiente hasta confirmarlo con POST /2fa/activar.")
    @PostMapping("/registro")
    public ResponseEntity<GenericResponse> registrar() {
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Registra el secreto en tu app autenticadora y confírmalo con un código");
        respuesta.setDatos(segundoFactorService.registrar(controlAcceso.obtenerClienteAutenticado()));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(respuesta);
    }

    @Operation(summary = "Activa el segundo factor con el primer código generado por la app")
    @PostMapping(value = "/activar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> activar(@Valid @RequestBody CodigoSegundoFactorRequest request) {
        segundoFactorService.activar(controlAcceso.obtenerClienteAutenticado(), request.getCodigo());
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Segundo factor activado correctamente");
        return ResponseEntity.ok(respuesta);
    }
}
