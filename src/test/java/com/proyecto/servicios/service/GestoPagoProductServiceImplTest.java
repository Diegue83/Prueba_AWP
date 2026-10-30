package com.proyecto.servicios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoProductsIntegration;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.GestoPagoAutenticacionException;
import com.proyecto.servicios.exception.GestoPagoTimeoutException;
import com.proyecto.servicios.model.gestopago.GestoPagoProducto;
import com.proyecto.servicios.model.gestopago.GestoPagoProductoXml;
import com.proyecto.servicios.service.Impl.GestoPagoProductPersistenceService;
import com.proyecto.servicios.service.Impl.GestoPagoProductServiceImpl;
import com.proyecto.servicios.service.Impl.GestoPagoProductXmlParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GestoPagoProductServiceImplTest {

    private static final String CACHE_KEY = "gestopago:productos:83:GPS83-TPV-17";

    @Mock
    private GestoPagoProductsIntegration productsIntegration;
    @Mock
    private GestoPagoTokenService tokenService;
    @Mock
    private GestoPagoProductPersistenceService persistenceService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private GestoPagoProductServiceImpl service;

    @BeforeEach
    void preparar() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new GestoPagoProductServiceImpl(productsIntegration, tokenService, persistenceService,
                new GestoPagoProductXmlParser(), redisTemplate, objectMapper, 83, "GPS83-TPV-17", 3600);
    }

    @Test
    void devuelveElCatalogoDeRedisSinConsultarPostgresNiGestoPago() throws Exception {
        when(valueOperations.get(CACHE_KEY)).thenReturn(objectMapper.writeValueAsString(List.of(producto(14302))));

        List<GestoPagoProducto> productos = service.consultarProductos();

        assertThat(productos).extracting(GestoPagoProducto::getIdProducto).containsExactly(14302);
        verify(persistenceService, never()).consultarCatalogo();
        verify(productsIntegration, never()).consultarCatalogoXml(anyString());
    }

    @Test
    void siRedisNoTieneElCatalogoLoLeeDePostgresYLoGuardaEnCache() {
        when(persistenceService.consultarCatalogo()).thenReturn(List.of(producto(14302)));

        List<GestoPagoProducto> productos = service.consultarProductos();

        assertThat(productos).hasSize(1);
        verify(valueOperations).set(eq(CACHE_KEY), anyString(), eq(Duration.ofHours(1)));
        verify(productsIntegration, never()).consultarCatalogoXml(anyString());
    }

    @Test
    void siRedisFallaRespondeConPostgres() {
        when(valueOperations.get(CACHE_KEY)).thenThrow(new RedisConnectionFailureException("sin conexión"));
        doThrow(new RedisConnectionFailureException("sin conexión"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        when(persistenceService.consultarCatalogo()).thenReturn(List.of(producto(14302)));

        assertThat(service.consultarProductos()).hasSize(1);
    }

    @Test
    void siPostgresEstaVacioDescargaElCatalogoDeGestoPago() {
        when(persistenceService.consultarCatalogo()).thenReturn(List.of(), List.of(producto(14302), producto(10034)));
        when(tokenService.obtenerTokenVigente()).thenReturn(token("token-guardado"));
        when(productsIntegration.consultarCatalogoXml("token-guardado"))
                .thenReturn(GestoPagoProductXmlParserTest.XML_CATALOGO);

        List<GestoPagoProducto> productos = service.consultarProductos();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GestoPagoProductoXml>> guardados = ArgumentCaptor.forClass(List.class);
        verify(persistenceService).guardarCatalogo(guardados.capture());
        assertThat(guardados.getValue()).extracting(GestoPagoProductoXml::getIdProducto).containsExactly(14302, 10034);
        assertThat(productos).hasSize(2);
        verify(valueOperations).set(eq(CACHE_KEY), anyString(), any(Duration.class));
    }

    @Test
    void siGestoPagoRechazaElTokenGuardadoSolicitaUnoNuevoYReintentaUnaVez() {
        when(tokenService.obtenerTokenVigente()).thenReturn(token("token-vencido"));
        when(tokenService.solicitarNuevoToken()).thenReturn(token("token-nuevo"));
        when(productsIntegration.consultarCatalogoXml("token-vencido"))
                .thenThrow(new GestoPagoAutenticacionException("getProductList"));
        when(productsIntegration.consultarCatalogoXml("token-nuevo"))
                .thenReturn(GestoPagoProductXmlParserTest.XML_CATALOGO);
        when(persistenceService.consultarCatalogo()).thenReturn(List.of(producto(14302)));

        assertThat(service.sincronizarProductos()).hasSize(1);
        verify(persistenceService).guardarCatalogo(anyList());
    }

    @Test
    void siElNuevoTokenTambienEsRechazadoPropagaElErrorSinGuardar() {
        when(tokenService.obtenerTokenVigente()).thenReturn(token("token-vencido"));
        when(tokenService.solicitarNuevoToken()).thenReturn(token("token-nuevo"));
        when(productsIntegration.consultarCatalogoXml(anyString()))
                .thenThrow(new GestoPagoAutenticacionException("getProductList"));

        assertThatThrownBy(() -> service.sincronizarProductos())
                .isInstanceOf(GestoPagoAutenticacionException.class);
        verify(persistenceService, never()).guardarCatalogo(anyList());
    }

    @Test
    void alIniciarConCatalogoEnPostgresNoConsultaGestoPagoNiReescribeDatos() {
        when(persistenceService.existeCatalogo()).thenReturn(true);
        when(persistenceService.consultarCatalogo()).thenReturn(List.of(producto(14302)));

        service.cargarCatalogoAlIniciar();

        verify(productsIntegration, never()).consultarCatalogoXml(anyString());
        verify(persistenceService, never()).guardarCatalogo(anyList());
        verify(valueOperations).set(eq(CACHE_KEY), anyString(), any(Duration.class));
    }

    @Test
    void alIniciarConCatalogoEnPostgresYRedisNoHaceNada() throws Exception {
        when(persistenceService.existeCatalogo()).thenReturn(true);
        when(valueOperations.get(CACHE_KEY)).thenReturn(objectMapper.writeValueAsString(List.of(producto(1))));

        service.cargarCatalogoAlIniciar();

        verify(persistenceService, never()).consultarCatalogo();
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void alIniciarSinCatalogoLoDescargaYSiGestoPagoFallaNoDetieneElArranque() {
        when(persistenceService.existeCatalogo()).thenReturn(false);
        when(tokenService.obtenerTokenVigente()).thenReturn(token("token"));
        when(productsIntegration.consultarCatalogoXml("token"))
                .thenThrow(new GestoPagoTimeoutException("getProductList"));

        service.cargarCatalogoAlIniciar();

        verify(productsIntegration).consultarCatalogoXml("token");
        verify(persistenceService, never()).guardarCatalogo(anyList());
    }

    private GestoPagoToken token(String valor) {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken(valor);
        token.setActivo(true);
        return token;
    }

    private GestoPagoProducto producto(int idProducto) {
        GestoPagoProducto producto = new GestoPagoProducto();
        producto.setIdProducto(idProducto);
        producto.setServicio("ABIB");
        producto.setProducto("ABIB 100");
        producto.setPrecio(new BigDecimal("100.00"));
        return producto;
    }
}
