package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.CuentaRequest;
import com.proyecto.servicios.model.onboarding.ConsultaCuentaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = "/cuentas", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Cuentas", description = "Consulta de cuentas bancarias")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @Operation(summary = "Consulta las cuentas activas")
    @GetMapping("/activas")
    public ResponseEntity<GenericResponse> consultarActivas() {
        return ResponseEntity.ok(cuentaService.consultarActivas());
    }

    @Operation(
            summary = "Busca cuentas combinando criterios opcionales",
            description = "Todos los criterios son opcionales: numeroCuenta, estatus, fechaAperturaDesde y "
                    + "fechaAperturaHasta. La búsqueda solo devuelve cuentas del titular autenticado.")
    @PostMapping(value = "/consulta", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> consultar(@Valid @RequestBody ConsultaCuentaRequest request) {
        return ResponseEntity.ok(cuentaService.consultar(request));
    }

    @Operation(summary = "Consulta el saldo de una cuenta")
    @PostMapping(value = "/saldo", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> consultarSaldo(@Valid @RequestBody CuentaRequest request) {
        return ResponseEntity.ok(cuentaService.consultarSaldo(request));
    }
}
