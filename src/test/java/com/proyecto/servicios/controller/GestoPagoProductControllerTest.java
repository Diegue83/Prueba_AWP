package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProducto;
import com.proyecto.servicios.service.GestoPagoProductService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GestoPagoProductControllerTest {

    private final GestoPagoProductService productService = mock(GestoPagoProductService.class);
    private final GestoPagoProductController controller = new GestoPagoProductController(productService);

    @Test
    void consultarProductosDevuelveElCatalogoEnElEnvelopeDelProyecto() throws Exception {
        GestoPagoProducto producto = new GestoPagoProducto();
        producto.setIdProducto(14302);
        producto.setServicio("ABIB");
        producto.setProducto("ABIB 100");
        producto.setPrecio(new BigDecimal("100.00"));
        when(productService.consultarProductos()).thenReturn(List.of(producto));

        GenericResponse response = controller.consultarProductos().getBody();
        String json = new ObjectMapper().writeValueAsString(response);

        assertThat(response).isNotNull();
        assertThat(response.getCodigo()).isZero();
        assertThat(response.getMensaje()).isEqualTo("Consulta de productos realizada correctamente");
        assertThat(json).contains("\"idProducto\":14302", "\"servicio\":\"ABIB\"", "\"precio\":100.00");
    }
}
