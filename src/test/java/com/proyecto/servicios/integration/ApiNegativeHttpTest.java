package com.proyecto.servicios.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.SeguridadWebConfig;
import com.proyecto.servicios.controller.ClienteController;
import com.proyecto.servicios.controller.CuentaController;
import com.proyecto.servicios.controller.LoginController;
import com.proyecto.servicios.controller.NacionalidadController;
import com.proyecto.servicios.controller.PersonaController;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.security.AutenticacionInterceptor;
import com.proyecto.servicios.security.TokenService;
import com.proyecto.servicios.service.CuentaService;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.service.LoginService;
import com.proyecto.servicios.service.NacionalidadService;
import com.proyecto.servicios.service.PersonaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ApiNegativeHttpTest.HttpTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "seguridad.jwt.secreto=local-http-tests-only-secret-key-32-chars",
                "seguridad.jwt.minutos-vigencia=5",
                "spring.main.banner-mode=off"
        })
class ApiNegativeHttpTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @Import({
            ClienteController.class,
            CuentaController.class,
            LoginController.class,
            NacionalidadController.class,
            PersonaController.class,
            GlobalExceptionHandler.class,
            SeguridadWebConfig.class,
            AutenticacionInterceptor.class,
            TokenService.class
    })
    static class HttpTestApplication {
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TokenService tokenService;

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    @MockBean
    private ClienteService clienteService;

    @MockBean
    private CuentaService cuentaService;

    @MockBean
    private LoginService loginService;

    @MockBean
    private NacionalidadService nacionalidadService;

    @MockBean
    private PersonaService personaService;

    private String token;

    @BeforeEach
    void prepararTokenDePrueba() {
        token = tokenService.generar(7);
    }

    @Test
    void altaClienteDevuelveTodosLosErroresDeCadaCampoPorHttp() throws Exception {
        ResponseEntity<String> respuesta = post("/clientes", """
                {
                  "nombre": "",
                  "correo": "no-es-correo",
                  "telefonoMovil": "123",
                  "fechaNacimiento": "ayer"
                }
                """, null);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(400);
        JsonNode cuerpo = objectMapper.readTree(respuesta.getBody());
        assertThat(cuerpo.path("error").asText()).isEqualTo("ERROR_VALIDACION");
        assertThat(cuerpo.path("datos").path("nombre").size()).isGreaterThanOrEqualTo(3);
        assertThat(cuerpo.path("datos").path("correo").size()).isGreaterThanOrEqualTo(1);
        assertThat(cuerpo.path("datos").path("telefonoMovil").size()).isEqualTo(1);
    }

    @Test
    void consultaClienteRechazaIdNegativoPorHttp() {
        ResponseEntity<String> respuesta = post("/clientes/consulta", "{\"id\":-1}", token);

        assertValidationError(respuesta);
        assertThat(respuesta.getBody()).contains("id");
    }

    @Test
    void consultaClienteRechazaCamposQueNoSonFiltrosPorHttp() {
        ResponseEntity<String> respuesta = post("/clientes/consulta", "{\"nombre\":\"Ana\"}", token);

        assertValidationError(respuesta);
    }

    @Test
    void consultaCuentaRechazaNumeroConFormatoIncorrectoPorHttp() {
        ResponseEntity<String> respuesta = post("/cuentas/consulta", "{\"numeroCuenta\":\"123\"}", token);

        assertValidationError(respuesta);
        assertThat(respuesta.getBody()).contains("numeroCuenta");
    }

    @Test
    void saldoRechazaNumeroDeCuentaAusentePorHttp() {
        ResponseEntity<String> respuesta = post("/cuentas/saldo", "{}", token);

        assertValidationError(respuesta);
        assertThat(respuesta.getBody()).contains("numeroCuenta");
    }

    @Test
    void consultaCuentaRechazaFechaImposiblePorHttp() {
        ResponseEntity<String> respuesta = post("/cuentas/consulta",
                "{\"fechaAperturaDesde\":\"2025-02-30\"}", token);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(400);
        assertThat(respuesta.getBody()).contains("fechaAperturaDesde");
    }

    @Test
    void consultaCuentaRechazaEstatusFueraDelCatalogoPorHttp() {
        ResponseEntity<String> respuesta = post("/cuentas/consulta", "{\"estatus\":\"SUSPENDIDA\"}", token);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(400);
        assertThat(respuesta.getBody()).contains("estatus");
    }

    @Test
    void loginRechazaCorreoYContrasenaInvalidosPorHttp() throws Exception {
        ResponseEntity<String> respuesta = post("/login",
                "{\"correo\":\"invalido\",\"contrasena\":\"\"}", null);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(400);
        JsonNode cuerpo = objectMapper.readTree(respuesta.getBody());
        assertThat(cuerpo.path("datos").has("correo")).isTrue();
        assertThat(cuerpo.path("datos").has("contrasena")).isTrue();
    }

    @Test
    void consultaNacionalidadRechazaValorVacioPorHttp() {
        ResponseEntity<String> respuesta = post("/nacionalidades/consulta", "{\"valor\":\" \"}", null);

        assertValidationError(respuesta);
        assertThat(respuesta.getBody()).contains("valor");
    }

    @Test
    void jsonMalformadoDevuelveBadRequestPorHttp() {
        ResponseEntity<String> respuesta = post("/login", "{\"correo\":", null);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(400);
        assertThat(respuesta.getBody()).contains("ERROR_VALIDACION");
    }

    @Test
    void endpointProtegidoSinTokenRespondeNoAutenticadoPorHttp() {
        ResponseEntity<String> respuesta = get("/cuentas/activas", null);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(401);
        assertThat(respuesta.getBody()).contains("NO_AUTENTICADO");
    }

    @Test
    void endpointProtegidoConTokenInvalidoRespondeNoAutenticadoPorHttp() {
        ResponseEntity<String> respuesta = get("/cuentas/activas", "no-es-un-jwt");

        assertThat(respuesta.getStatusCode().value()).isEqualTo(401);
        assertThat(respuesta.getBody()).contains("NO_AUTENTICADO");
    }

    private ResponseEntity<String> post(String ruta, String json, String bearerToken) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url(ruta)))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (bearerToken != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        }
        return send(request.POST(HttpRequest.BodyPublishers.ofString(json)).build());
    }

    private ResponseEntity<String> get(String ruta, String bearerToken) {
        if (bearerToken != null) {
            return send(HttpRequest.newBuilder(URI.create(url(ruta)))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .GET()
                    .build());
        }
        return send(HttpRequest.newBuilder(URI.create(url(ruta))).GET().build());
    }

    private ResponseEntity<String> send(HttpRequest request) {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            return ResponseEntity.status(response.statusCode()).body(response.body());
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo ejecutar la petición HTTP local", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La petición HTTP local fue interrumpida", ex);
        }
    }

    private String url(String ruta) {
        return "http://127.0.0.1:" + port + ruta;
    }

    private void assertValidationError(ResponseEntity<String> respuesta) {
        assertThat(respuesta.getStatusCode().value()).isEqualTo(400);
        assertThat(respuesta.getBody()).contains("ERROR_VALIDACION");
    }
}
