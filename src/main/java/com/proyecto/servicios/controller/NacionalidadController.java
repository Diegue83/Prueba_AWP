package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.NacionalidadRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.service.NacionalidadService;
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
@RequestMapping(value = "/nacionalidades", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Nacionalidades", description = "Catálogo de nacionalidades válidas")
public class NacionalidadController {

    private final NacionalidadService nacionalidadService;

    public NacionalidadController(NacionalidadService nacionalidadService) {
        this.nacionalidadService = nacionalidadService;
    }

    @Operation(summary = "Lista todas las nacionalidades del catálogo")
    @GetMapping
    public ResponseEntity<GenericResponse> consultarTodas() {
        return ResponseEntity.ok(nacionalidadService.consultarTodas());
    }

    @Operation(summary = "Verifica si una nacionalidad existe (por nombre o por clave de 3 letras)")
    @PostMapping(value = "/consulta", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> consultar(
            @Valid @RequestBody NacionalidadRequest request) {
        return ResponseEntity.ok(nacionalidadService.consultar(request));
    }
}