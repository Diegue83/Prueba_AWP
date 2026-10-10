package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.onboarding.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler manejador = new GlobalExceptionHandler();

    @Test
    void tipoIncorrectoEnIdIndicaQueDebeSerEnteroPositivo() {
        MethodArgumentTypeMismatchException excepcion = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "id", (MethodParameter) null, new IllegalArgumentException());

        var respuesta = manejador.manejarTipoIncorrecto(excepcion);
        ErrorResponse cuerpo = respuesta.getBody();

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(cuerpo).isNotNull();
        assertThat(cuerpo.getDetalles())
                .contains("id: debe ser un número entero mayor que cero, sin letras ni decimales");
    }
}
