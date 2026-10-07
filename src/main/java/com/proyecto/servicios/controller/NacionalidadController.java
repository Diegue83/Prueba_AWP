package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.NacionalidadResponse;
import com.proyecto.servicios.service.NacionalidadService;
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
@RequestMapping(value = "/nacionalidades", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Nacionalidades", description = "Catálogo de nacionalidades válidas")
public class NacionalidadController {

    private final NacionalidadService nacionalidadService;

    public NacionalidadController(NacionalidadService nacionalidadService) {
        this.nacionalidadService = nacionalidadService;
    }

    @Operation(summary = "Lista todas las nacionalidades del catálogo")
    @GetMapping
    public ResponseEntity<List<NacionalidadResponse>> consultarTodas() {
        return ResponseEntity.ok(nacionalidadService.consultarTodas());
    }

    @Operation(summary = "Verifica si una nacionalidad existe (por nombre o por clave de 3 letras)")
    @GetMapping("/{valor}")
    public ResponseEntity<NacionalidadResponse> consultar(@PathVariable String valor) {
        return ResponseEntity.ok(nacionalidadService.consultar(valor));
    }
}