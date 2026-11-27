package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.entity.pagos.Pago;
import com.proyecto.servicios.enums.EstatusPago;
import com.proyecto.servicios.exception.PagoNoEncontradoException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.pagos.PagoResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductRepository;
import com.proyecto.servicios.repositorys.pagos.PagoRepository;
import com.proyecto.servicios.service.impl.PagoHistorialServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PagoHistorialServiceImplTest {

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private GestoPagoProductRepository productRepository;

    private PagoHistorialServiceImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new PagoHistorialServiceImpl(pagoRepository, productRepository);
    }

    @Test
    void elHistorialEsDelClienteYTraeElNombreDelServicio() {
        Pago recarga = pago(1L, 14302, null, "5512345678", EstatusPago.APLICADO);
        Pago servicioLuz = pago(2L, 5001, "123456789012", "1111111111", EstatusPago.EN_CONCILIACION);
        when(pagoRepository.findByClienteIdOrderByFechaCreacionDescIdDesc(7, PageRequest.of(0, 20)))
                .thenReturn(List.of(servicioLuz, recarga));
        when(productRepository.findAllById(any())).thenReturn(List.of(producto(14302, "TELCEL", "Recarga $100")));

        List<PagoResponse> historial = servicio.consultarHistorial(7, 0, 20);

        assertThat(historial).extracting(PagoResponse::idPago).containsExactly(2L, 1L);
        assertThat(historial.get(1).servicio()).isEqualTo("TELCEL");
        assertThat(historial.get(1).referencia()).isEqualTo("5512345678");
        assertThat(historial.get(0).servicio()).isNull();
        assertThat(historial.get(0).referencia()).isEqualTo("123456789012");
        assertThat(historial.get(0).mensaje()).contains("verificación");
    }

    @Test
    void rechazaPaginacionFueraDeRango() {
        assertThatThrownBy(() -> servicio.consultarHistorial(7, -1, 20)).isInstanceOf(ValidacionException.class);
        assertThatThrownBy(() -> servicio.consultarHistorial(7, 0, 0)).isInstanceOf(ValidacionException.class);
        assertThatThrownBy(() -> servicio.consultarHistorial(7, 0, PagoHistorialServiceImpl.TAMANO_MAXIMO + 1))
                .isInstanceOf(ValidacionException.class);
        verify(pagoRepository, never()).findByClienteIdOrderByFechaCreacionDescIdDesc(any(), any());
    }

    @Test
    void elDetalleDeUnPagoAjenoNoSeEncuentra() {
        when(pagoRepository.findByIdAndClienteId(5L, 7)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.consultarDetalle(7, 5L)).isInstanceOf(PagoNoEncontradoException.class);
    }

    private static Pago pago(Long id, Integer idProducto, String referencia, String telefono, EstatusPago estatus) {
        Pago pago = mock(Pago.class);
        when(pago.getId()).thenReturn(id);
        when(pago.getIdProducto()).thenReturn(idProducto);
        when(pago.getIdServicio()).thenReturn(1);
        when(pago.getReferencia()).thenReturn(referencia);
        when(pago.getTelefono()).thenReturn(telefono);
        when(pago.getEstatus()).thenReturn(estatus);
        when(pago.getMonto()).thenReturn(new BigDecimal("100.00"));
        when(pago.getFechaCreacion()).thenReturn(LocalDateTime.now());
        return pago;
    }

    private static GestoPagoProduct producto(Integer idProducto, String servicio, String nombre) {
        GestoPagoProduct producto = new GestoPagoProduct();
        producto.setIdProducto(idProducto);
        producto.setServicio(servicio);
        producto.setProducto(nombre);
        return producto;
    }
}
