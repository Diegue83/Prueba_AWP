package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.onboarding.ClienteDatos;
import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.DomicilioRequest;

import java.math.BigDecimal;

/** Datos válidos reutilizados por las pruebas de los servicios. */
final class DatosDePrueba {

    static final String CURP = "VAVZ950312MGTRRY07";
    static final String RFC = "VAVZ950312AB1";
    static final String CORREO = "zayda@correo.com";
    static final String CONTRASENA = "Segura123";
    static final String NUMERO_CUENTA = "1000000001";

    private DatosDePrueba() {
    }

    static ClienteRequest solicitudDeAlta() {
        ClienteRequest solicitud = new ClienteRequest();
        llenarDatos(solicitud);
        solicitud.setCurp(CURP);
        solicitud.setRfc(RFC);
        solicitud.setContrasena(CONTRASENA);
        return solicitud;
    }

    static Cliente clienteExistente(String contrasenaCifrada) {
        Cliente cliente = new ClienteMapper().aEntidad(solicitudDeAlta());
        cliente.abrirCuenta(NUMERO_CUENTA, BigDecimal.ZERO);
        cliente.crearAcceso(contrasenaCifrada);
        return cliente;
    }

    static void llenarDatos(ClienteDatos datos) {
        DomicilioRequest domicilio = new DomicilioRequest();
        domicilio.setCalle("Av. Hidalgo");
        domicilio.setNumeroExterior("12");
        domicilio.setColonia("Centro");
        domicilio.setMunicipio("Dolores Hidalgo");
        domicilio.setEstado("Guanajuato");
        domicilio.setCodigoPostal("37800");
        domicilio.setPais("México");

        datos.setNombre("Zayda");
        datos.setApellidoPaterno("Vargas");
        datos.setApellidoMaterno("Vargas");
        datos.setFechaNacimiento("1995-03-12");
        datos.setSexo("MUJER");
        datos.setNacionalidad("Mexicana");
        datos.setEstadoCivil("S");
        datos.setCorreo("  Zayda@Correo.com ");
        datos.setTelefonoMovil("4181234567");
        datos.setDomicilio(domicilio);
        datos.setOcupacion("Ingeniera");
        datos.setEmpresa("UTNG");
        datos.setIngresoMensual(new BigDecimal("15000.5"));
    }
}
