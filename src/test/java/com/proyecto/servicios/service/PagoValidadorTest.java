package com.proyecto.servicios.service;

import com.proyecto.servicios.config.PagosProperties;
import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.exception.ProductoNoEncontradoException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.pagos.PagoRequest;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductRepository;
import com.proyecto.servicios.service.Impl.PagoValidador;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoValidadorTest {

    @Mock
    private GestoPagoProductRepository productRepository;

    @Test
    void aceptaUnaRecargaConElPrecioExactoDelCatalogo() {
        when(productRepository.findById(14302)).thenReturn(Optional.of(producto(14302, 2284, "100.00", 1, "a")));

        GestoPagoProduct producto = validador().validar(solicitud(2284, 14302, "100.00", "5512345678", null));

        assertThat(producto.getIdProducto()).isEqualTo(14302);
    }

    @Test
    void rechazaUnMontoManipuladoEnProductoDePrecioFijo() {
        when(productRepository.findById(14302)).thenReturn(Optional.of(producto(14302, 2284, "100.00", 1, "a")));

        assertThatThrownBy(() -> validador().validar(solicitud(2284, 14302, "1.00", "5512345678", null)))
                .isInstanceOf(ValidacionException.class)
                .extracting("detalles").asList()
                .containsExactly("monto: el producto tiene un precio fijo de 100.00");
    }

    @Test
    void rechazaUnServicioQueNoCorrespondeAlProducto() {
        when(productRepository.findById(14302)).thenReturn(Optional.of(producto(14302, 2284, "100.00", 1, "a")));

        assertThatThrownBy(() -> validador().validar(solicitud(105, 14302, "100.00", "5512345678", null)))
                .isInstanceOf(ValidacionException.class)
                .extracting("detalles").asList()
                .containsExactly("idServicio: no corresponde al producto 14302");
    }

    @Test
    void paraMontoAbiertoAplicaLosLimitesConfigurados() {
        when(productRepository.findById(10034)).thenReturn(Optional.of(producto(10034, 105, "0.00", 2, "b")));

        validador().validar(solicitud(105, 10034, "350.50", null, "1234567890"));
        assertThatThrownBy(() -> validador().validar(solicitud(105, 10034, "10000.01", null, "1234567890")))
                .isInstanceOf(ValidacionException.class);
        assertThatThrownBy(() -> validador().validar(solicitud(105, 10034, "0.50", null, "1234567890")))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void exigeTelefonoOReferenciaSegunElProducto() {
        when(productRepository.findById(14302)).thenReturn(Optional.of(producto(14302, 2284, "100.00", 1, "a")));
        when(productRepository.findById(10034)).thenReturn(Optional.of(producto(10034, 105, "0.00", 2, "b")));

        assertThatThrownBy(() -> validador().validar(solicitud(2284, 14302, "100.00", null, "1234")))
                .isInstanceOf(ValidacionException.class).extracting("detalles").asList()
                .containsExactly("telefono: es obligatorio para este producto");
        assertThatThrownBy(() -> validador().validar(solicitud(105, 10034, "50.00", "5512345678", null)))
                .isInstanceOf(ValidacionException.class).extracting("detalles").asList()
                .containsExactly("referencia: es obligatoria para este producto");
    }

    @Test
    void rechazaProductosInexistentesOTiposNoPermitidos() {
        when(productRepository.findById(1)).thenReturn(Optional.empty());
        when(productRepository.findById(2)).thenReturn(Optional.of(producto(2, 9, "0.00", 40, "b")));

        assertThatThrownBy(() -> validador().validar(solicitud(9, 1, "10.00", null, "1234")))
                .isInstanceOf(ProductoNoEncontradoException.class);
        assertThatThrownBy(() -> validador().validar(solicitud(9, 2, "10.00", null, "1234")))
                .isInstanceOf(ValidacionException.class);
    }

    private PagoValidador validador() {
        return new PagoValidador(productRepository, new PagosProperties());
    }

    static PagoRequest solicitud(int idServicio, int idProducto, String monto, String telefono, String referencia) {
        PagoRequest request = new PagoRequest();
        request.setNumeroCuenta("1000000001");
        request.setIdServicio(idServicio);
        request.setIdProducto(idProducto);
        request.setMonto(new BigDecimal(monto));
        request.setTelefono(telefono);
        request.setReferencia(referencia);
        request.setCodigo2fa("123456");
        return request;
    }

    private GestoPagoProduct producto(int idProducto, int idServicio, String precio, int tipoFront, String tipoReferencia) {
        GestoPagoProduct producto = new GestoPagoProduct();
        producto.setIdProducto(idProducto);
        producto.setIdServicio(idServicio);
        producto.setPrecio(new BigDecimal(precio));
        producto.setTipoFront(tipoFront);
        producto.setTipoReferencia(tipoReferencia);
        return producto;
    }
}
