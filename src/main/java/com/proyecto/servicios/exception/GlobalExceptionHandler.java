package com.proyecto.servicios.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
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
                String campo = obtenerRutaCampo(mapeo);
                String detalle;
                if (mapeo instanceof UnrecognizedPropertyException) {
                    detalle = campo + ": campo no permitido; verifica el nombre y consulta los campos admitidos";
                } else if (mapeo instanceof InvalidFormatException formato) {
                    detalle = campo + ": " + descripcionTipo(formato.getTargetType());
                } else {
                    detalle = campo + ": revisa el valor y el tipo de dato esperado";
                }
                return responder(HttpStatus.BAD_REQUEST, ERROR_VALIDACION,
                        "No se pudo interpretar la información enviada; corrige los campos indicados",
                        List.of(detalle));
            }
            if (causa instanceof JsonParseException) {
                return responder(HttpStatus.BAD_REQUEST, ERROR_VALIDACION,
                        "El cuerpo contiene JSON mal formado; revisa comas, comillas, llaves y corchetes",
                        List.of("Corrige la sintaxis del JSON y vuelve a enviar la petición"));
            }
            causa = causa.getCause();
        }
        return responder(HttpStatus.BAD_REQUEST, ERROR_VALIDACION,
                "El cuerpo de la petición es obligatorio y debe ser un JSON válido. "
                        + "Revisa comas, comillas, llaves y tipos de dato",
                List.of("Envía un objeto JSON completo con Content-Type: application/json"));
    }

    /** Un parámetro de la URL con el tipo equivocado, por ejemplo /clientes/abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarTipoIncorrecto(MethodArgumentTypeMismatchException ex) {
        String detalle;
        if ("id".equals(ex.getName())) {
            detalle = "id: debe ser un número entero mayor que cero, sin letras ni decimales";
        } else {
            detalle = ex.getRequiredType() == null
                    ? ex.getName() + ": el valor no tiene el formato esperado"
                    : ex.getName() + ": debe ser " + descripcionTipo(ex.getRequiredType());
        }
        return responder(HttpStatus.BAD_REQUEST, ERROR_VALIDACION,
                "Un parámetro de la URL no tiene el formato esperado",
                List.of(detalle));
    }

    private String obtenerRutaCampo(JsonMappingException excepcion) {
        StringBuilder ruta = new StringBuilder();
        for (JsonMappingException.Reference referencia : excepcion.getPath()) {
            if (referencia.getFieldName() != null) {
                if (!ruta.isEmpty()) {
                    ruta.append('.');
                }
                ruta.append(referencia.getFieldName());
            } else if (referencia.getIndex() >= 0) {
                ruta.append('[').append(referencia.getIndex()).append(']');
            }
        }
        return ruta.isEmpty() ? "cuerpo" : ruta.toString();
    }

    private String descripcionTipo(Class<?> tipo) {
        if (tipo == null) {
            return "un valor con el formato correcto";
        }
        if (Integer.class.equals(tipo) || int.class.equals(tipo)
                || Long.class.equals(tipo) || long.class.equals(tipo)) {
            return "un número entero, sin letras ni decimales";
        }
        if (java.math.BigDecimal.class.equals(tipo) || Double.class.equals(tipo)
                || double.class.equals(tipo) || Float.class.equals(tipo) || float.class.equals(tipo)) {
            return "un número; usa punto para separar los decimales";
        }
        if (Boolean.class.equals(tipo) || boolean.class.equals(tipo)) {
            return "un valor booleano: true o false";
        }
        if (java.time.LocalDate.class.equals(tipo)) {
            return "una fecha real con formato AAAA-MM-DD";
        }
        return "un valor compatible con el tipo " + tipo.getSimpleName();
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
