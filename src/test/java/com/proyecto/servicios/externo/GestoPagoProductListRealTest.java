package com.proyecto.servicios.externo;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.client.GestoPagoBitacoraService;
import com.proyecto.servicios.client.GestoPagoProductsClient;
import com.proyecto.servicios.client.GestoPagoProductsIntegration;
import com.proyecto.servicios.exception.GestoPagoAutenticacionException;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProductoXml;
import com.proyecto.servicios.security.TokenService;
import com.proyecto.servicios.service.Impl.GestoPagoProductXmlParser;
import feign.Feign;
import feign.Request;
import feign.codec.Decoder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.HttpMessageConverterCustomizer;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Llama al GestoPago real con las credenciales del .env local. No corre con ./gradlew test:
 * se ejecuta con ./gradlew testExterno y solo si el .env tiene las credenciales.
 * Cada ejecución pide un token y consume una de las 3 llamadas diarias a getProductList que
 * permite PuntoRed (al exceder bloquea la IP): úsala solo cuando haga falta.
 */
@Tag("externo")
@EnabledIf("hayCredenciales")
class GestoPagoProductListRealTest {

    private static final Path ARCHIVO_ENV = Path.of(".env");
    private static final Set<String> REQUERIDAS = Set.of("GESTOPAGO_AUTH_URL", "GESTOPAGO_AUTH_ID_DISTRIBUIDOR",
            "GESTOPAGO_AUTH_CODIGO_DISPOSITIVO", "GESTOPAGO_AUTH_PASSWORD", "JWT_SECRET");

    private static Map<String, String> env;
    private static GestoPagoProductsIntegration integracion;
    private static String tokenGestoPago;

    static boolean hayCredenciales() {
        return leerEnv().keySet().containsAll(REQUERIDAS);
    }

    @BeforeAll
    static void autenticar() {
        env = leerEnv();
        String url = env.get("GESTOPAGO_AUTH_URL");
        GestoPagoAuthResponse respuesta = cliente(GestoPagoAuthClient.class, url).authenticate(
                Integer.valueOf(env.get("GESTOPAGO_AUTH_ID_DISTRIBUIDOR")),
                env.get("GESTOPAGO_AUTH_CODIGO_DISPOSITIVO"),
                env.get("GESTOPAGO_AUTH_PASSWORD"));
        assertThat(respuesta.getSuccess()).isTrue();
        assertThat(respuesta.getToken()).isNotBlank();
        tokenGestoPago = respuesta.getToken();
        integracion = new GestoPagoProductsIntegration(cliente(GestoPagoProductsClient.class, url),
                mock(GestoPagoBitacoraService.class));
    }

    @Test
    void conElTokenDeGestoPagoDevuelveElCatalogoCompleto() {
        List<GestoPagoProductoXml> productos =
                new GestoPagoProductXmlParser().parsear(integracion.consultarCatalogoXml(tokenGestoPago));

        assertThat(productos).hasSizeGreaterThan(100);
        assertThat(productos).allSatisfy(producto -> {
            assertThat(producto.tieneDatosObligatorios()).isTrue();
            assertThat(producto.getTipoFront()).isPositive();
        });
        assertThat(productos.stream().map(GestoPagoProductoXml::getIdProducto).collect(Collectors.toSet()))
                .hasSameSizeAs(productos);
        assertThat(productos).anySatisfy(producto -> assertThat(producto.getPrecio()).isPositive());
    }

    @Test
    void elTokenDeSesionDeLaAppNoSirveParaGestoPago() {
        String tokenSesion = new TokenService(env.get("JWT_SECRET"), 5).generar(1);

        assertThatThrownBy(() -> integracion.consultarCatalogoXml(tokenSesion))
                .isInstanceOf(GestoPagoAutenticacionException.class);
    }

    @Test
    void unTokenInventadoSeRechaza() {
        assertThatThrownBy(() -> integracion.consultarCatalogoXml("token-que-no-existe"))
                .isInstanceOf(GestoPagoAutenticacionException.class);
    }

    private static <T> T cliente(Class<T> tipo, String url) {
        Decoder decoder = new SpringDecoder(
                () -> new HttpMessageConverters(false, List.of(
                        new StringHttpMessageConverter(StandardCharsets.UTF_8),
                        new MappingJackson2HttpMessageConverter())),
                new StaticListableBeanFactory().getBeanProvider(HttpMessageConverterCustomizer.class));
        return Feign.builder()
                .contract(new SpringMvcContract())
                .decoder(decoder)
                .options(new Request.Options(10, TimeUnit.SECONDS, 60, TimeUnit.SECONDS, true))
                .target(tipo, url);
    }

    private static Map<String, String> leerEnv() {
        Map<String, String> valores = new HashMap<>();
        if (!Files.isRegularFile(ARCHIVO_ENV)) {
            return valores;
        }
        try {
            for (String linea : Files.readAllLines(ARCHIVO_ENV, StandardCharsets.UTF_8)) {
                int igual = linea.indexOf('=');
                if (!linea.startsWith("#") && igual > 0 && igual < linea.length() - 1) {
                    valores.put(linea.substring(0, igual).trim(), linea.substring(igual + 1).trim());
                }
            }
        } catch (IOException e) {
            return Map.of();
        }
        return valores;
    }
}
