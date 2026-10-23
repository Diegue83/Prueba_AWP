package com.proyecto.servicios.model.onboarding;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Datos del cliente que SÍ se pueden modificar. Es la base común del alta
 * del request único {@link ClienteRequest} en las operaciones de alta y actualización.
 *
 * Fecha y catálogos llegan como texto para que sean las funciones de validación
 * las que revisen su formato y respondan con un mensaje claro.
 */
@Getter
@Setter
public abstract class ClienteDatos extends SolicitudEstricta {

    // ---- Datos personales ----
    @NotBlank(message = "nombre: es obligatorio")
    @Size(min = 3, max = 50, message = "nombre: debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?: [A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$",
            message = "nombre: solo admite letras y espacios")
    private String nombre;
    @Size(max = 50, message = "segundoNombre: admite máximo 50 caracteres")
    @Pattern(regexp = "^(?:|[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?: [A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*)$",
            message = "segundoNombre: solo admite letras y espacios")
    private String segundoNombre;      // opcional
    @NotBlank(message = "apellidoPaterno: es obligatorio")
    @Size(min = 3, max = 50, message = "apellidoPaterno: debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?: [A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$",
            message = "apellidoPaterno: solo admite letras y espacios")
    private String apellidoPaterno;
    @NotBlank(message = "apellidoMaterno: es obligatorio")
    @Size(min = 3, max = 50, message = "apellidoMaterno: debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?: [A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$",
            message = "apellidoMaterno: solo admite letras y espacios")
    private String apellidoMaterno;
    @NotBlank(message = "fechaNacimiento: es obligatoria")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "fechaNacimiento: usa el formato AAAA-MM-DD")
    private String fechaNacimiento;
    @NotBlank(message = "sexo: es obligatorio")
    @Pattern(regexp = "(?i)^(HOMBRE|MUJER|NO_BINARIO|H|M|X)$",
            message = "sexo: usa HOMBRE, MUJER, NO_BINARIO o sus códigos H, M, X")
    private String sexo;
    @NotBlank(message = "nacionalidad: es obligatoria")
    @Size(min = 2, max = 40, message = "nacionalidad: debe tener entre 2 y 40 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?: [A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$",
            message = "nacionalidad: solo admite letras y espacios")
    private String nacionalidad;
    @NotBlank(message = "estadoCivil: es obligatorio")
    @Pattern(regexp = "(?i)^(SOLTERO|CASADO|DIVORCIADO|VIUDO|UNION_LIBRE|S|C|D|V|U)$",
            message = "estadoCivil: usa un valor válido del catálogo")
    private String estadoCivil;

    // ---- Datos de contacto ----
    @NotBlank(message = "correo: es obligatorio")
    @Email(message = "correo: debe tener un formato válido")
    @Size(max = 100, message = "correo: admite máximo 100 caracteres")
    private String correo;
    @NotBlank(message = "telefonoMovil: es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "telefonoMovil: debe contener exactamente 10 dígitos")
    private String telefonoMovil;
    @Pattern(regexp = "^(?:|\\d{10})$", message = "telefonoAlterno: debe contener exactamente 10 dígitos")
    private String telefonoAlterno;

    // ---- Domicilio ----
    @NotNull(message = "domicilio: es obligatorio")
    @Valid
    private DomicilioRequest domicilio;

    // ---- Información laboral ----
    @NotBlank(message = "ocupacion: es obligatoria")
    @Size(min = 2, max = 60, message = "ocupacion: debe tener entre 2 y 60 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?: [A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$",
            message = "ocupacion: solo admite letras y espacios")
    private String ocupacion;
    @NotBlank(message = "empresa: es obligatoria")
    @Size(min = 2, max = 80, message = "empresa: debe tener entre 2 y 80 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9 .,#&'/-]+$",
            message = "empresa: solo admite letras, números, espacios y signos . , # & ' / -")
    private String empresa;
    @NotNull(message = "ingresoMensual: es obligatorio")
    @DecimalMin(value = "0.00", inclusive = false, message = "ingresoMensual: debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "ingresoMensual: admite máximo 10 enteros y 2 decimales")
    private BigDecimal ingresoMensual;
}
