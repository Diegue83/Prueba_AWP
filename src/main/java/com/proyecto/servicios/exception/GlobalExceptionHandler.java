package com.proyecto.servicios.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.proyecto.servicios.model.onboarding.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Punto único donde cualquier error se convierte en una respuesta JSON con el mismo formato.
 * Los validadores y servicios solo lanzan la excepción; aquí se decide la respuesta HTTP.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String ERROR_VALIDACION = "ERROR_VALIDACION";

    /** Todas las excepciones de negocio y de validación: cada una ya trae su código HTTP, clave y detalles. */
    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorResponse> manejarNegocio(NegocioException ex) {
        log.warn("Petición rechazada [{}]: {} {}", ex.getClave(), ex.getMessage(), ex.getDetalles());
        return responder(ex.getEstatus(), ex.getClave(), ex.getMessage(), ex.getDetalles());
    }

    /** El JSON no se pudo leer: mal formado, sin cuerpo, o un campo con un tipo de dato que no corresponde. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarJsonIlegible(HttpMessageNotReadableException ex) {
        Throwable causa = ex.getCause();
        while (causa != null) {
            if (causa instanceof NegocioException negocio) {
                return manejarNegocio(negocio);
            }
            if (causa instanceof JsonMappingException mapeo && !mapeo.getPath().isEmpty()) {
                String campo = mapeo.getPath().get(mapeo.getPath().size() - 1).getFieldName();
                return responder(HttpStatus.BAD_REQUEST, ERROR_VALIDACION,
                        "La información enviada no es válida",
                        List.of(campo + ": el valor no corresponde al tipo de dato esperado"));
            }
            causa = causa.getCause();
        }
        return responder(HttpStatus.BAD_REQUEST, ERROR_VALIDACION,
                "El cuerpo de la petición es obligatorio y debe ser un JSON bien formado", List.of());
    }

    /** Un parámetro de la URL con el tipo equivocado, por ejemplo /clientes/abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarTipoIncorrecto(MethodArgumentTypeMismatchException ex) {
        return responder(HttpStatus.BAD_REQUEST, ERROR_VALIDACION,
                "Los parámetros enviados no son válidos",
                List.of(ex.getName() + ": debe ser un número entero positivo"));
    }

    /**
     * Red de seguridad: si dos altas idénticas llegan al mismo tiempo, las dos pasan la verificación
     * del servicio y es el UNIQUE de la base de datos el que detiene a la segunda.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> manejarIntegridad(DataIntegrityViolationException ex) {
        log.warn("Restricción de base de datos violada: {}", ex.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, "CLIENTE_YA_REGISTRADO",
                "La operación viola una restricción de datos (CURP, RFC, correo o cuenta ya registrados)", List.of());
    }

    private ResponseEntity<ErrorResponse> responder(HttpStatus estatus, String clave, String mensaje, List<String> detalles) {
        return ResponseEntity.status(estatus).body(new ErrorResponse(estatus.value(), clave, mensaje, detalles));
    }
}
