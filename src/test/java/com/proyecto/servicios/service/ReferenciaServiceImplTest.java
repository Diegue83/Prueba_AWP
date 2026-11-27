package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoCifrador;
import com.proyecto.servicios.client.GestoPagoTransaccionesIntegration;
import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.GestoPagoAutenticacionException;
import com.proyecto.servicios.exception.ProductoNoEncontradoException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.gestopago.GestoPagoReferenciaResultado;
import com.proyecto.servicios.model.gestopago.GestoPagoTxPayload;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResultado;
import com.proyecto.servicios.model.pagos.ReferenciaRequest;
import com.proyecto.servicios.model.pagos.ReferenciaResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductRepository;
import com.proyecto.servicios.service.impl.ReferenciaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenciaServiceImplTest {

    @Mock
    private GestoPagoProductRepository productRepository;
    @Mock
    private GestoPagoTransaccionesIntegration integracion;
    @Mock
    private GestoPagoCifrador cifrador;
    @Mock
    private GestoPagoTokenService tokenService;

    private ReferenciaServiceImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new ReferenciaServiceImpl(productRepository, integracion, cifrador, tokenService);
    }

    @Test
    void unaReferenciaConfirmadaPorElProveedorEsValidaYTraeElSaldo() {
        conProducto();
        when(tokenService.obtenerTokenVigente()).thenReturn(token("token"));
        when(integracion.verificarReferencia(any(), eq("token"))).thenReturn(resultado(
                GestoPagoTxResultado.Tipo.EXITOSO, "01", "Referencia Valida", new BigDecimal("1034.5")));

        ReferenciaResponse respuesta = servicio.verificar(solicitud(" tmx-5610440665 "));

        assertThat(respuesta.estado()).isEqualTo(ReferenciaResponse.Estado.VALIDA);
        assertThat(respuesta.valida()).isTrue();
        assertThat(respuesta.saldo()).isEqualByComparingTo("1034.5");
        assertThat(respuesta.servicio()).isEqualTo("AT&T");
        ArgumentCaptor<GestoPagoTxPayload> payload = ArgumentCaptor.forClass(GestoPagoTxPayload.class);
        verify(integracion).verificarReferencia(payload.capture(), eq("token"));
        assertThat(payload.getValue().referencia()).isEqualTo("TMX-5610440665");
        assertThat(payload.getValue().montoPago()).isNull();
    }

    @Test
    void paraUnProductoDePagoSeVerificaConElProductoDeConsultaDelMismoServicio() {
        GestoPagoProduct recarga = producto();
        recarga.setTipoFront(1);
        GestoPagoProduct consulta = producto();
        consulta.setIdProducto(900);
        when(productRepository.findById(272)).thenReturn(Optional.of(recarga));
        when(productRepository.findFirstByIdServicioAndTipoFrontOrderByIdProductoAsc(108, 4))
                .thenReturn(Optional.of(consulta));
        when(tokenService.obtenerTokenVigente()).thenReturn(token("token"));
        when(integracion.verificarReferencia(any(), anyString())).thenReturn(resultado(
                GestoPagoTxResultado.Tipo.EXITOSO, "01", "Referencia Valida", null));

        ReferenciaResponse respuesta = servicio.verificar(solicitud("5610440665"));

        ArgumentCaptor<GestoPagoTxPayload> payload = ArgumentCaptor.forClass(GestoPagoTxPayload.class);
        verify(integracion).verificarReferencia(payload.capture(), eq("token"));
        assertThat(payload.getValue().idProducto()).isEqualTo(900);
        assertThat(respuesta.idProducto()).isEqualTo(272);
        assertThat(respuesta.valida()).isTrue();
    }

    @Test
    void unRechazoDelProveedorEsInvalidaConSuMensaje() {
        conProducto();
        when(tokenService.obtenerTokenVigente()).thenReturn(token("token"));
        when(integracion.verificarReferencia(any(), anyString())).thenReturn(resultado(
                GestoPagoTxResultado.Tipo.RECHAZADO, "02", "Referencia Invalida", null));

        ReferenciaResponse respuesta = servicio.verificar(solicitud("0000000000"));

        assertThat(respuesta.estado()).isEqualTo(ReferenciaResponse.Estado.INVALIDA);
        assertThat(respuesta.valida()).isFalse();
        assertThat(respuesta.mensaje()).isEqualTo("Referencia Invalida");
        assertThat(respuesta.codigoProveedor()).isEqualTo("02");
    }

    @Test
    void sinRespuestaConcluyenteNoSePermiteContinuar() {
        conProducto();
        when(tokenService.obtenerTokenVigente()).thenReturn(token("token"));
        when(integracion.verificarReferencia(any(), anyString())).thenReturn(
                new GestoPagoReferenciaResultado(GestoPagoTxResultado.indeterminado("timeout"), null));

        ReferenciaResponse respuesta = servicio.verificar(solicitud("5610440665"));

        assertThat(respuesta.estado()).isEqualTo(ReferenciaResponse.Estado.NO_VERIFICADA);
        assertThat(respuesta.valida()).isFalse();
        assertThat(respuesta.mensaje()).contains("No fue posible verificar");
    }

    @Test
    void siElProveedorRechazaElTokenSeRenuevaYSeReintentaUnaVez() {
        conProducto();
        when(tokenService.obtenerTokenVigente()).thenReturn(token("vencido"));
        when(tokenService.solicitarNuevoToken()).thenReturn(token("nuevo"));
        when(integracion.verificarReferencia(any(), eq("vencido")))
                .thenThrow(new GestoPagoAutenticacionException("verifyReference"));
        when(integracion.verificarReferencia(any(), eq("nuevo"))).thenReturn(resultado(
                GestoPagoTxResultado.Tipo.EXITOSO, "01", null, null));

        assertThat(servicio.verificar(solicitud("5610440665")).valida()).isTrue();
    }

    @Test
    void rechazaProductosQueNoExistenONoSonDelServicio() {
        when(productRepository.findById(272)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.verificar(solicitud("5610440665")))
                .isInstanceOf(ProductoNoEncontradoException.class);

        GestoPagoProduct otroServicio = producto();
        otroServicio.setIdServicio(999);
        when(productRepository.findById(272)).thenReturn(Optional.of(otroServicio));

        assertThatThrownBy(() -> servicio.verificar(solicitud("5610440665")))
                .isInstanceOf(ValidacionException.class);
        verify(integracion, never()).verificarReferencia(any(), any());
    }

    private void conProducto() {
        when(productRepository.findById(272)).thenReturn(Optional.of(producto()));
    }

    private static GestoPagoProduct producto() {
        GestoPagoProduct producto = new GestoPagoProduct();
        producto.setIdProducto(272);
        producto.setIdServicio(108);
        producto.setServicio("AT&T");
        producto.setProducto("Recarga AT&T");
        producto.setTipoFront(4);
        return producto;
    }

    private static ReferenciaRequest solicitud(String referencia) {
        ReferenciaRequest request = new ReferenciaRequest();
        request.setIdServicio(108);
        request.setIdProducto(272);
        request.setReferencia(referencia);
        return request;
    }

    private static GestoPagoReferenciaResultado resultado(GestoPagoTxResultado.Tipo tipo, String codigo,
                                                          String mensaje, BigDecimal saldo) {
        return new GestoPagoReferenciaResultado(new GestoPagoTxResultado(tipo, codigo, mensaje, null, null), saldo);
    }

    private static GestoPagoToken token(String valor) {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken(valor);
        return token;
    }
}
