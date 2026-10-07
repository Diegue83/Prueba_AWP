package com.proyecto.servicios.model.onboarding;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Datos del cliente que SÍ se pueden modificar. Es la base común del alta
 * ({@link ClienteRequest}) y de la actualización ({@link ClienteUpdateRequest}).
 *
 * Fecha y catálogos llegan como texto para que sean las funciones de validación
 * las que revisen su formato y respondan con un mensaje claro.
 */
@Getter
@Setter
public abstract class ClienteDatos extends SolicitudEstricta {

    // ---- Datos personales ----
    private String nombre;
    private String segundoNombre;      // opcional
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String fechaNacimiento;    // AAAA-MM-DD
    private String sexo;               // HOMBRE/H, MUJER/M, NO_BINARIO/X
    private String nacionalidad;
    private String estadoCivil;        // SOLTERO/S, CASADO/C, DIVORCIADO/D, VIUDO/V, UNION_LIBRE/U

    // ---- Datos de contacto ----
    private String correo;
    private String telefonoMovil;
    private String telefonoAlterno;    // opcional

    // ---- Domicilio ----
    private DomicilioRequest domicilio;

    // ---- Información laboral ----
    private String ocupacion;
    private String empresa;
    private BigDecimal ingresoMensual; // 2 decimales
}
