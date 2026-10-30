package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.mapper.GestoPagoProductMapper;
import com.proyecto.servicios.mapper.GestoPagoProductMapperImpl;
import com.proyecto.servicios.model.gestopago.GestoPagoProducto;
import com.proyecto.servicios.model.gestopago.GestoPagoProductoXml;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductRepository;
import com.proyecto.servicios.service.Impl.GestoPagoProductPersistenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestoPagoProductPersistenceServiceTest {

    @Mock
    private GestoPagoProductRepository productRepository;

    private final GestoPagoProductMapper mapper = new GestoPagoProductMapperImpl();

    @Test
    void actualizaLosProductosExistentesEInsertaLosNuevosSinBorrarNada() {
        GestoPagoProduct existente = new GestoPagoProduct();
        existente.setIdProducto(14302);
        existente.setNuevo(false);
        existente.setServicio("ABIB");
        existente.setProducto("ABIB 100");
        existente.setPrecio(new BigDecimal("90.00"));
        LocalDateTime creado = LocalDateTime.now().minusDays(3);
        existente.setFechaCreacion(creado);
        when(productRepository.findAllById(List.of(14302, 10034))).thenReturn(List.of(existente));
        GestoPagoProductPersistenceService service = new GestoPagoProductPersistenceService(productRepository, mapper);

        service.guardarCatalogo(List.of(xml(14302, "100.0"), xml(10034, "0.0")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GestoPagoProduct>> guardados = ArgumentCaptor.forClass(List.class);
        verify(productRepository).saveAll(guardados.capture());
        List<GestoPagoProduct> entidades = guardados.getValue();
        assertThat(entidades).hasSize(2);
        assertThat(entidades.get(0)).isSameAs(existente);
        assertThat(existente.getPrecio()).isEqualByComparingTo("100");
        assertThat(existente.getFechaCreacion()).isEqualTo(creado);
        assertThat(existente.isNew()).isFalse();
        assertThat(entidades.get(1).isNew()).isTrue();
        assertThat(entidades.get(1).getShowAyuda()).isFalse();
        verify(productRepository, never()).deleteAllInBatch();
    }

    @Test
    void consultaElCatalogoComoDto() {
        GestoPagoProduct entidad = new GestoPagoProduct();
        entidad.setIdProducto(14302);
        entidad.setServicio("ABIB");
        entidad.setProducto("ABIB 100");
        entidad.setPrecio(new BigDecimal("100.00"));
        entidad.setLeyenda("Conserva tu comprobante");
        when(productRepository.findAllByOrderByServicioAscPrecioAscIdProductoAsc()).thenReturn(List.of(entidad));

        List<GestoPagoProducto> productos = new GestoPagoProductPersistenceService(productRepository, mapper)
                .consultarCatalogo();

        assertThat(productos).singleElement()
                .satisfies(producto -> {
                    assertThat(producto.getIdProducto()).isEqualTo(14302);
                    assertThat(producto.getLeyenda()).isEqualTo("Conserva tu comprobante");
                });
    }

    private GestoPagoProductoXml xml(int idProducto, String precio) {
        GestoPagoProductoXml producto = new GestoPagoProductoXml();
        producto.setIdProducto(idProducto);
        producto.setIdServicio(2284);
        producto.setIdCatTipoServicio(13);
        producto.setTipoFront(1);
        producto.setServicio("ABIB");
        producto.setProducto("ABIB " + idProducto);
        producto.setPrecio(new BigDecimal(precio));
        return producto;
    }
}
