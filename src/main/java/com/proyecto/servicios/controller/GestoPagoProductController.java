package com.proyecto.servicios.controller;

import com.proyecto.servicios.client.GestoPagoImagenes;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.service.GestoPagoProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping(value = "/productos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Productos", description = "Catálogo de productos de GestoPago")
public class GestoPagoProductController {

    private static final CacheControl CACHE_IMAGENES = CacheControl.maxAge(Duration.ofDays(1)).cachePublic();

    private final GestoPagoProductService productService;
    private final GestoPagoImagenes imagenes;

    public GestoPagoProductController(GestoPagoProductService productService, GestoPagoImagenes imagenes) {
        this.productService = productService;
        this.imagenes = imagenes;
    }

    @Operation(
            summary = "Consulta el catálogo de productos",
            description = "Devuelve el catálogo desde Redis; si no está en cache lo lee de PostgreSQL. "
                    + "Es público: no requiere token de sesión.")
    @GetMapping
    public ResponseEntity<GenericResponse> consultarProductos() {
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Consulta de productos realizada correctamente");
        respuesta.setDatos(productService.consultarProductos());
        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Logotipo del servicio (PNG de PuntoRed)", description = "Público. 404 si el servicio no tiene logotipo.")
    @GetMapping(value = "/servicios/{idServicio}/logo", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> logo(@PathVariable("idServicio") int idServicio) {
        return imagen(GestoPagoImagenes.Tipo.LOGO, idServicio);
    }

    @Operation(summary = "Imagen de ayuda que muestra dónde está la referencia en el recibo",
            description = "Público. 404 si el servicio no tiene imagen de ayuda.")
    @GetMapping(value = "/servicios/{idServicio}/ayuda-referencia", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> ayudaReferencia(@PathVariable("idServicio") int idServicio) {
        return imagen(GestoPagoImagenes.Tipo.AYUDA_REFERENCIA, idServicio);
    }

    private ResponseEntity<byte[]> imagen(GestoPagoImagenes.Tipo tipo, int idServicio) {
        if (idServicio < 1) {
            return ResponseEntity.notFound().build();
        }
        return imagenes.obtener(tipo, idServicio)
                .map(bytes -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(tipo.contentType()))
                        .cacheControl(CACHE_IMAGENES)
                        .body(bytes))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
