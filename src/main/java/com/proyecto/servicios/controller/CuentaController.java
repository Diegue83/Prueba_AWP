package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.CuentaResponse;
import com.proyecto.servicios.model.onboarding.SaldoResponse;
import com.proyecto.servicios.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public ResponseEntity<List<CuentaResponse>> consultarActivas() {
        return ResponseEntity.ok(cuentaService.consultarActivas());
    }

    @Operation(summary = "Consulta una cuenta por su número")
    @GetMapping("/{numeroCuenta}")
    public ResponseEntity<CuentaResponse> consultarPorNumero(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.consultarPorNumero(numeroCuenta));
    }

    @Operation(summary = "Consulta el saldo de una cuenta")
    @GetMapping("/{numeroCuenta}/saldo")
    public ResponseEntity<SaldoResponse> consultarSaldo(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.consultarSaldo(numeroCuenta));
    }
}
