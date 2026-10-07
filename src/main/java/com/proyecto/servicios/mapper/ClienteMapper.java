package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.enums.Catalogo;
import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.Sexo;
import com.proyecto.servicios.model.onboarding.ClienteDatos;
import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import com.proyecto.servicios.model.onboarding.CuentaResponse;
import com.proyecto.servicios.model.onboarding.DomicilioRequest;
import com.proyecto.servicios.model.onboarding.DomicilioResponse;
import com.proyecto.servicios.validation.Validaciones;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.util.Locale;

/**
 * Convierte entre DTOs y entidades. También normaliza el texto
 * (quita espacios sobrantes, correo en minúsculas) para que lo guardado sea consistente.
 */
@Component
public class ClienteMapper {

    /**
     * Alta: crea la entidad con CURP y RFC, que solo se asignan aquí.
     * Se llama únicamente con solicitudes que ya pasaron por ClienteValidador.
     */
    public Cliente aEntidad(ClienteRequest request) {
        Cliente cliente = new Cliente();
        cliente.setCurp(request.getCurp());
        cliente.setRfc(request.getRfc());
        cliente.asignarDomicilio(new Domicilio());
        copiarDatosModificables(request, cliente);
        return cliente;
    }

    /** Alta y actualización comparten este método: CURP y RFC nunca pasan por aquí. */
    public void copiarDatosModificables(ClienteDatos datos, Cliente cliente) {
        cliente.setNombre(limpiar(datos.getNombre()));
        cliente.setSegundoNombre(limpiar(datos.getSegundoNombre()));
        cliente.setApellidoPaterno(limpiar(datos.getApellidoPaterno()));
        cliente.setApellidoMaterno(limpiar(datos.getApellidoMaterno()));
        cliente.setFechaNacimiento(Validaciones.leerFecha(datos.getFechaNacimiento()));
        cliente.setSexo(Catalogo.leer(Sexo.class, datos.getSexo()));
        cliente.setNacionalidad(limpiar(datos.getNacionalidad()));
        cliente.setEstadoCivil(Catalogo.leer(EstadoCivil.class, datos.getEstadoCivil()));
        cliente.setCorreo(normalizarCorreo(datos.getCorreo()));
        cliente.setTelefonoMovil(datos.getTelefonoMovil());
        cliente.setTelefonoAlterno(datos.getTelefonoAlterno());
        cliente.setOcupacion(limpiar(datos.getOcupacion()));
        cliente.setEmpresa(limpiar(datos.getEmpresa()));
        // Dinero siempre con 2 decimales exactos (el validador ya rechazó montos con más de 2).
        cliente.setIngresoMensual(datos.getIngresoMensual().setScale(2, RoundingMode.HALF_UP));
        copiarDomicilio(datos.getDomicilio(), cliente.getDomicilio());
    }

    public ClienteResponse aRespuesta(Cliente cliente) {
        ClienteResponse respuesta = new ClienteResponse();
        respuesta.setId(cliente.getId());
        respuesta.setNombre(cliente.getNombre());
        respuesta.setSegundoNombre(cliente.getSegundoNombre());
        respuesta.setApellidoPaterno(cliente.getApellidoPaterno());
        respuesta.setApellidoMaterno(cliente.getApellidoMaterno());
        respuesta.setFechaNacimiento(cliente.getFechaNacimiento());
        respuesta.setCurp(cliente.getCurp());
        respuesta.setRfc(cliente.getRfc());
        respuesta.setSexo(cliente.getSexo());
        respuesta.setNacionalidad(cliente.getNacionalidad());
        respuesta.setEstadoCivil(cliente.getEstadoCivil());
        respuesta.setCorreo(cliente.getCorreo());
        respuesta.setTelefonoMovil(cliente.getTelefonoMovil());
        respuesta.setTelefonoAlterno(cliente.getTelefonoAlterno());
        respuesta.setOcupacion(cliente.getOcupacion());
        respuesta.setEmpresa(cliente.getEmpresa());
        respuesta.setIngresoMensual(cliente.getIngresoMensual());
        respuesta.setActivo(cliente.isActivo());
        respuesta.setFechaRegistro(cliente.getFechaRegistro());
        respuesta.setFechaActualizacion(cliente.getFechaActualizacion());
        respuesta.setDomicilio(aRespuestaDomicilio(cliente.getDomicilio()));
        respuesta.setCuenta(aRespuestaCuenta(cliente.getCuenta()));
        return respuesta;
    }

    public CuentaResponse aRespuestaCuenta(Cuenta cuenta) {
        if (cuenta == null) {
            return null;
        }
        CuentaResponse respuesta = new CuentaResponse();
        respuesta.setNumeroCuenta(cuenta.getNumeroCuenta());
        respuesta.setClienteId(cuenta.getCliente().getId());
        respuesta.setSaldo(cuenta.getSaldo());
        respuesta.setEstatus(cuenta.getEstatus());
        respuesta.setFechaApertura(cuenta.getFechaApertura());
        return respuesta;
    }

    /** El correo se guarda en minúsculas para que "Ana@x.com" y "ana@x.com" cuenten como el mismo. */
    public String normalizarCorreo(String correo) {
        return correo == null ? null : correo.trim().toLowerCase(Locale.ROOT);
    }

    private DomicilioResponse aRespuestaDomicilio(Domicilio domicilio) {
        if (domicilio == null) {
            return null;
        }
        DomicilioResponse respuesta = new DomicilioResponse();
        respuesta.setCalle(domicilio.getCalle());
        respuesta.setNumeroExterior(domicilio.getNumeroExterior());
        respuesta.setNumeroInterior(domicilio.getNumeroInterior());
        respuesta.setColonia(domicilio.getColonia());
        respuesta.setMunicipio(domicilio.getMunicipio());
        respuesta.setEstado(domicilio.getEstado());
        respuesta.setCodigoPostal(domicilio.getCodigoPostal());
        respuesta.setPais(domicilio.getPais());
        return respuesta;
    }

    private void copiarDomicilio(DomicilioRequest origen, Domicilio destino) {
        destino.setCalle(limpiar(origen.getCalle()));
        destino.setNumeroExterior(limpiar(origen.getNumeroExterior()));
        destino.setNumeroInterior(limpiar(origen.getNumeroInterior()));
        destino.setColonia(limpiar(origen.getColonia()));
        destino.setMunicipio(limpiar(origen.getMunicipio()));
        destino.setEstado(limpiar(origen.getEstado()));
        destino.setCodigoPostal(origen.getCodigoPostal());
        destino.setPais(limpiar(origen.getPais()));
    }

    /** Quita espacios al inicio y al final; un texto opcional vacío se guarda como NULL. */
    private String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
