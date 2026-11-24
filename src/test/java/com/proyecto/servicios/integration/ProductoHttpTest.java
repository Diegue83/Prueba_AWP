package com.proyecto.servicios.integration;

import com.proyecto.servicios.config.SeguridadWebConfig;
import com.proyecto.servicios.controller.GestoPagoProductController;
import com.proyecto.servicios.controller.PagoController;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.model.gestopago.GestoPagoProducto;
import com.proyecto.servicios.security.AutenticacionInterceptor;
import com.proyecto.servicios.security.ControlAcceso;
import com.proyecto.servicios.security.TokenService;
import com.proyecto.servicios.service.GestoPagoProductService;
import com.proyecto.servicios.service.PagoService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = ProductoHttpTest.HttpTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "seguridad.jwt.secreto=local-http-tests-only-secret-key-32-chars",
                "spring.main.banner-mode=off"
        })
class ProductoHttpTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            RedisAutoConfiguration.class
    })
    @Import({
            GestoPagoProductController.class,
            PagoController.class,
            GlobalExceptionHandler.class,
            SeguridadWebConfig.class,
            AutenticacionInterceptor.class,
            ControlAcceso.class,
            TokenService.class
    })
    static class HttpTestApplication {
    }

    @LocalServerPort
    private int port;
    @MockBean
    private GestoPagoProductService productService;
    @MockBean
    private PagoService pagoService;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void elCatalogoDeProductosSeConsultaSinTokenDeSesion() throws Exception {
        GestoPagoProducto producto = new GestoPagoProducto();
        producto.setIdProducto(14302);
        producto.setPrecio(new BigDecimal("100.00"));
        when(productService.consultarProductos()).thenReturn(List.of(producto));

        HttpResponse<String> respuesta = get("/productos");

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("\"idProducto\":14302");
    }

    @Test
    void lasComprasSiguenRequiriendoTokenDeSesion() throws Exception {
        assertThat(get("/pagos/1").statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> get(String ruta) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + ruta)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
