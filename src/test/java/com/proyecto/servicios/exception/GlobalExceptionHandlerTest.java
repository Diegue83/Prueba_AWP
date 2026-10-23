package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.onboarding.ErrorResponse;
import com.proyecto.servicios.exception.ValidacionException;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

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
        assertThat(cuerpo.getCodigo()).isEqualTo(1);
        assertThat(cuerpo.getMensaje())
                .contains("id: debe ser un número entero mayor que cero, sin letras ni decimales");
        assertThat(cuerpo.getDetalles())
                .contains("id: debe ser un número entero mayor que cero, sin letras ni decimales");
        assertThat(cuerpo.getDatos())
                .isEqualTo(cuerpo.getDetalles());
    }

    @Test
    void validacionBeanValidationUsaFormatoGlobalDeError() {
        BeanPropertyBindingResult errores = new BeanPropertyBindingResult(new Object(), "request");
        errores.addError(new FieldError("request", "correo", "correo: debe tener un formato válido"));
        errores.addError(new FieldError("request", "correo", "correo: admite máximo 100 caracteres"));
        errores.addError(new FieldError("request", "nombre", "nombre: es obligatorio"));

        var respuesta = manejador.manejarValidacionDto(new MethodArgumentNotValidException(null, errores));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getCodigo()).isEqualTo(1);
        assertThat(respuesta.getBody().getError()).isEqualTo("ERROR_VALIDACION");
        assertThat(respuesta.getBody().getMensaje())
                .contains("correo: debe tener un formato válido", "correo: admite máximo 100 caracteres",
                        "nombre: es obligatorio");
        assertThat(respuesta.getBody().getDetalles())
                .containsExactly("correo: debe tener un formato válido",
                        "correo: admite máximo 100 caracteres", "nombre: es obligatorio");
        assertThat(respuesta.getBody().getDatos())
                .isEqualTo(Map.of(
                        "correo", List.of("debe tener un formato válido", "admite máximo 100 caracteres"),
                        "nombre", List.of("es obligatorio")));
    }

    @Test
    void validacionDeNegocioAgregaLosDetallesAlMensajeGeneral() {
        ValidacionException excepcion = new ValidacionException(
                "La información enviada no es válida",
                List.of("curp: formato inválido", "rfc: formato inválido"));

        var respuesta = manejador.manejarNegocio(excepcion);

        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getCodigo()).isEqualTo(1);
        assertThat(respuesta.getBody().getMensaje())
                .isEqualTo("La información enviada no es válida: curp: formato inválido; rfc: formato inválido");
        assertThat(respuesta.getBody().getDetalles())
                .containsExactly("curp: formato inválido", "rfc: formato inválido");
        assertThat(respuesta.getBody().getDatos())
                .isEqualTo(List.of("curp: formato inválido", "rfc: formato inválido"));
    }

    @Test
    void errorInternoUsaFormatoUniformeSinExponerDetallesTecnicos() {
        var respuesta = manejador.manejarErrorInterno(new IllegalStateException("SELECT * FROM clientes"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getCodigo()).isEqualTo(1);
        assertThat(respuesta.getBody().getError()).isEqualTo("ERROR_INTERNO");
        assertThat(respuesta.getBody().getMensaje()).doesNotContain("SELECT", "clientes");
        assertThat(respuesta.getBody().getDatos()).isEqualTo(List.of());
    }
}
