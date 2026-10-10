package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.Sexo;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ClienteResponse {

    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private String curp;
    private String rfc;
    private Sexo sexo;
    private String nacionalidad;
    private EstadoCivil estadoCivil;
    private String correo;
    private String telefonoMovil;
    private String telefonoAlterno;
    private String ocupacion;
    private String empresa;
    private BigDecimal ingresoMensual;
    private boolean activo;
    private LocalDate fechaRegistro;
    private LocalDate fechaActualizacion;
    private DomicilioResponse domicilio;
    private CuentaResponse cuenta;
}
