package com.proyecto.servicios.service;

import com.proyecto.servicios.config.PagosProperties;
import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.exception.ProductoNoEncontradoException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.pagos.PagoRequest;
import com.proyecto.servicios.model.pagos.PagoValidado;
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
    void recargaConElPrecioExactoEnviaElTelefonoSinReferenciaNiComision() {
        when(productRepository.findById(14302)).thenReturn(Optional.of(producto(14302, 2284, "100.00", 1, "a", 13)));

        PagoValidado validado = validador().validar(solicitud(2284, 14302, "100.00", "5512345678", null));

        assertThat(validado.telefono()).isEqualTo("5512345678");
        assertThat(validado.referencia()).isNull();
        assertThat(validado.comision()).isZero();
        assertThat(validado.total()).isEqualByComparingTo("100.00");
    }

    @Test
    void recargaPorReferenciaLaEnviaEnElCampoTelefono() {
        when(productRepository.findById(500)).thenReturn(Optional.of(producto(500, 18, "200.00", 1, "bc", 18)));

        PagoValidado validado = validador().validar(solicitud(18, 500, "200.00", null, "TAG12345678"));

        assertThat(validado.telefono()).isEqualTo("TAG12345678");
        assertThat(validado.referencia()).isNull();
    }

    @Test
    void pagoDeServicioUsaTelefonoFijoReferenciaYCobraLaComisionDelCatalogo() {
        when(productRepository.findById(205)).thenReturn(Optional.of(producto(205, 76, "10.00", 2, "b", 5)));

        PagoValidado validado = validador().validar(solicitud(76, 205, "601.99", "5512345678", "25720381"));

        assertThat(validado.telefono()).isEqualTo("1111111111");
        assertThat(validado.referencia()).isEqualTo("25720381");
        assertThat(validado.monto()).isEqualByComparingTo("601.99");
        assertThat(validado.comision()).isEqualByComparingTo("10.00");
        assertThat(validado.total()).isEqualByComparingTo("611.99");
    }

    @Test
    void articulosCanjeablesUsanElTelefonoFijoConGato() {
        when(productRepository.findById(900)).thenReturn(Optional.of(producto(900, 300, "150.00", 1, "a", 11)));

        PagoValidado validado = validador().validar(solicitud(300, 900, "150.00", null, null));

        assertThat(validado.telefono()).isEqualTo("#1111111111");
    }

    @Test
    void rechazaUnMontoManipuladoEnUnaRecarga() {
        when(productRepository.findById(14302)).thenReturn(Optional.of(producto(14302, 2284, "100.00", 1, "a", 13)));

        assertThatThrownBy(() -> validador().validar(solicitud(2284, 14302, "1.00", "5512345678", null)))
                .isInstanceOf(ValidacionException.class)
                .extracting("detalles").asList()
                .containsExactly("monto: el producto tiene un precio fijo de 100.00");
    }

    @Test
    void rechazaUnServicioQueNoCorrespondeAlProducto() {
        when(productRepository.findById(14302)).thenReturn(Optional.of(producto(14302, 2284, "100.00", 1, "a", 13)));

        assertThatThrownBy(() -> validador().validar(solicitud(105, 14302, "100.00", "5512345678", null)))
                .isInstanceOf(ValidacionException.class)
                .extracting("detalles").asList()
                .containsExactly("idServicio: no corresponde al producto 14302");
    }

    @Test
    void enPagosDeServicioAplicaLosLimitesDeMonto() {
        when(productRepository.findById(205)).thenReturn(Optional.of(producto(205, 76, "10.00", 2, "b", 5)));

        assertThatThrownBy(() -> validador().validar(solicitud(76, 205, "10000.01", null, "25720381")))
                .isInstanceOf(ValidacionException.class);
        assertThatThrownBy(() -> validador().validar(solicitud(76, 205, "0.50", null, "25720381")))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void exigeTelefonoOReferenciaSegunElProducto() {
        when(productRepository.findById(14302)).thenReturn(Optional.of(producto(14302, 2284, "100.00", 1, "a", 13)));
        when(productRepository.findById(205)).thenReturn(Optional.of(producto(205, 76, "10.00", 2, "b", 5)));

        assertThatThrownBy(() -> validador().validar(solicitud(2284, 14302, "100.00", null, "1234")))
                .isInstanceOf(ValidacionException.class).extracting("detalles").asList()
                .containsExactly("telefono: es obligatorio para este producto");
        assertThatThrownBy(() -> validador().validar(solicitud(76, 205, "50.00", "5512345678", null)))
                .isInstanceOf(ValidacionException.class).extracting("detalles").asList()
                .containsExactly("referencia: es obligatoria para este producto");
    }

    @Test
    void rechazaProductosInexistentesYLosQueNoSonVentas() {
        when(productRepository.findById(1)).thenReturn(Optional.empty());
        when(productRepository.findById(2)).thenReturn(Optional.of(producto(2, 9, "0.00", 4, "b", 5)));
        when(productRepository.findById(3)).thenReturn(Optional.of(producto(3, 9, "0.00", 40, "b", 8)));

        assertThatThrownBy(() -> validador().validar(solicitud(9, 1, "10.00", null, "1234")))
                .isInstanceOf(ProductoNoEncontradoException.class);
        assertThatThrownBy(() -> validador().validar(solicitud(9, 2, "10.00", null, "1234")))
                .isInstanceOf(ValidacionException.class);
        assertThatThrownBy(() -> validador().validar(solicitud(9, 3, "10.00", null, "1234")))
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

    private GestoPagoProduct producto(int idProducto, int idServicio, String precio, int tipoFront,
                                      String tipoReferencia, int idCatTipoServicio) {
        GestoPagoProduct producto = new GestoPagoProduct();
        producto.setIdProducto(idProducto);
        producto.setIdServicio(idServicio);
        producto.setPrecio(new BigDecimal(precio));
        producto.setTipoFront(tipoFront);
        producto.setTipoReferencia(tipoReferencia);
        producto.setIdCatTipoServicio(idCatTipoServicio);
        return producto;
    }
}
