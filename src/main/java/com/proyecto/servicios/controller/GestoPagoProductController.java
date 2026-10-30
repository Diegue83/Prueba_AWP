package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.service.GestoPagoProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/productos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Productos", description = "Catálogo de productos de GestoPago")
public class GestoPagoProductController {

    private final GestoPagoProductService productService;

    public GestoPagoProductController(GestoPagoProductService productService) {
        this.productService = productService;
    }

    @Operation(
            summary = "Consulta el catálogo de productos",
            description = "Devuelve el catálogo desde Redis; si no está en cache lo lee de PostgreSQL. "
                    + "Requiere el encabezado Authorization: Bearer <token>.")
    @GetMapping
    public ResponseEntity<GenericResponse> consultarProductos() {
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Consulta de productos realizada correctamente");
        respuesta.setDatos(productService.consultarProductos());
        return ResponseEntity.ok(respuesta);
    }
}
