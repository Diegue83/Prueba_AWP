package com.proyecto.servicios.validation;

import com.proyecto.servicios.enums.Catalogo;
import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.Sexo;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.onboarding.ClienteDatos;
import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteUpdateRequest;
import com.proyecto.servicios.model.onboarding.DomicilioRequest;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Valida lo que llega a cada endpoint usando las funciones de {@link Validaciones}.
 * Junta todos los errores y, si hay al menos uno, lanza ValidacionException,
 * que el manejador global convierte en un 400 (Bad Request) con el detalle por campo.
 */
@Component
public class ClienteValidador {

    private static final String MENSAJE_CUERPO = "La información enviada no es válida";
    private static final String MENSAJE_PARAMETROS = "Los parámetros enviados no son válidos";

    // ====================== POST /clientes ======================

    public void validarAlta(ClienteRequest solicitud) {
        List<String> errores = new ArrayList<>();
        int erroresPrevios;

        Validaciones.curp("curp", solicitud.getCurp(), errores);
        boolean curpValida = errores.isEmpty();

        erroresPrevios = errores.size();
        Validaciones.rfc("rfc", solicitud.getRfc(), errores);
        boolean rfcValido = errores.size() == erroresPrevios;

        Validaciones.contrasenaNueva("contrasena", solicitud.getContrasena(), errores);

        erroresPrevios = errores.size();
        LocalDate nacimiento = validarDatosModificables(solicitud, errores);
        boolean datosValidos = errores.size() == erroresPrevios;

        // La coherencia entre campos solo se revisa cuando cada campo, por separado, ya es válido.
        if (curpValida && rfcValido && datosValidos) {
            validarCoherencia(solicitud, nacimiento, errores);
        }
        lanzarSiHayErrores(MENSAJE_CUERPO, errores);
    }

    // ==================== PUT /clientes/{id} ====================

    public void validarActualizacion(Integer id, ClienteUpdateRequest solicitud) {
        List<String> errores = new ArrayList<>();
        Validaciones.idPositivo("id", id, errores);
        validarDatosModificables(solicitud, errores);
        lanzarSiHayErrores(MENSAJE_CUERPO, errores);
    }

    // ======================== POST /login ========================

    public void validarLogin(LoginRequest solicitud) {
        List<String> errores = new ArrayList<>();
        Validaciones.correo("correo", solicitud.getCorreo(), errores);
        if (solicitud.getContrasena() == null || solicitud.getContrasena().isEmpty()) {
            errores.add("contrasena: es obligatoria");
        }
        lanzarSiHayErrores(MENSAJE_CUERPO, errores);
    }

    // =================== Parámetros de la URL ===================

    public void validarId(Integer id) {
        List<String> errores = new ArrayList<>();
        Validaciones.idPositivo("id", id, errores);
        lanzarSiHayErrores(MENSAJE_PARAMETROS, errores);
    }

    public void validarCurp(String curp) {
        List<String> errores = new ArrayList<>();
        Validaciones.curp("curp", curp, errores);
        lanzarSiHayErrores(MENSAJE_PARAMETROS, errores);
    }

    public void validarRfc(String rfc) {
        List<String> errores = new ArrayList<>();
        Validaciones.rfc("rfc", rfc, errores);
        lanzarSiHayErrores(MENSAJE_PARAMETROS, errores);
    }

    public void validarCorreo(String correo) {
        List<String> errores = new ArrayList<>();
        Validaciones.correo("correo", correo, errores);
        lanzarSiHayErrores(MENSAJE_PARAMETROS, errores);
    }

    public void validarNumeroCuenta(String numeroCuenta) {
        List<String> errores = new ArrayList<>();
        Validaciones.numeroCuenta("numeroCuenta", numeroCuenta, errores);
        lanzarSiHayErrores(MENSAJE_PARAMETROS, errores);
    }

    /** Valida el rango y lo devuelve ya convertido: posición 0 = desde, posición 1 = hasta. */
    public LocalDate[] validarRango(String desde, String hasta) {
        List<String> errores = new ArrayList<>();
        LocalDate fechaDesde = Validaciones.fecha("desde", desde, errores);
        LocalDate fechaHasta = Validaciones.fecha("hasta", hasta, errores);
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            errores.add("desde: no puede ser posterior a 'hasta'");
        }
        lanzarSiHayErrores(MENSAJE_PARAMETROS, errores);
        return new LocalDate[]{fechaDesde, fechaHasta};
    }

    // ===================== Métodos de apoyo =====================

    /** Campos comunes del alta y de la actualización. Devuelve la fecha de nacimiento ya convertida. */
    private LocalDate validarDatosModificables(ClienteDatos datos, List<String> errores) {
        // Datos personales
        Validaciones.soloLetras("nombre", datos.getNombre(), true, 2, 50, errores);
        Validaciones.soloLetras("segundoNombre", datos.getSegundoNombre(), false, 2, 50, errores);
        Validaciones.soloLetras("apellidoPaterno", datos.getApellidoPaterno(), true, 2, 50, errores);
        Validaciones.soloLetras("apellidoMaterno", datos.getApellidoMaterno(), true, 2, 50, errores);
        LocalDate nacimiento = Validaciones.fechaNacimiento("fechaNacimiento", datos.getFechaNacimiento(), errores);
        Validaciones.catalogo("sexo", datos.getSexo(), Sexo.class, errores);
        Validaciones.soloLetras("nacionalidad", datos.getNacionalidad(), true, 2, 40, errores);
        Validaciones.catalogo("estadoCivil", datos.getEstadoCivil(), EstadoCivil.class, errores);

        // Datos de contacto
        Validaciones.correo("correo", datos.getCorreo(), errores);
        Validaciones.telefono("telefonoMovil", datos.getTelefonoMovil(), true, errores);
        Validaciones.telefono("telefonoAlterno", datos.getTelefonoAlterno(), false, errores);
        if (datos.getTelefonoAlterno() != null && datos.getTelefonoAlterno().equals(datos.getTelefonoMovil())) {
            errores.add("telefonoAlterno: debe ser distinto del teléfono móvil");
        }

        // Domicilio
        validarDomicilio(datos.getDomicilio(), errores);

        // Información laboral
        Validaciones.soloLetras("ocupacion", datos.getOcupacion(), true, 2, 60, errores);
        Validaciones.textoGeneral("empresa", datos.getEmpresa(), true, 2, 80, errores);
        Validaciones.monto("ingresoMensual", datos.getIngresoMensual(), errores);
        return nacimiento;
    }

    private void validarDomicilio(DomicilioRequest domicilio, List<String> errores) {
        if (domicilio == null) {
            errores.add("domicilio: es obligatorio");
            return;
        }
        Validaciones.textoGeneral("domicilio.calle", domicilio.getCalle(), true, 2, 100, errores);
        Validaciones.textoGeneral("domicilio.numeroExterior", domicilio.getNumeroExterior(), true, 1, 10, errores);
        Validaciones.textoGeneral("domicilio.numeroInterior", domicilio.getNumeroInterior(), false, 1, 10, errores);
        Validaciones.textoGeneral("domicilio.colonia", domicilio.getColonia(), true, 2, 80, errores);
        Validaciones.textoGeneral("domicilio.municipio", domicilio.getMunicipio(), true, 2, 80, errores);
        Validaciones.soloLetras("domicilio.estado", domicilio.getEstado(), true, 2, 50, errores);
        Validaciones.codigoPostal("domicilio.codigoPostal", domicilio.getCodigoPostal(), errores);
        Validaciones.soloLetras("domicilio.pais", domicilio.getPais(), true, 2, 50, errores);
    }

    /** La CURP y el RFC contienen la fecha de nacimiento, y la CURP además el sexo: deben coincidir con lo capturado. */
    private void validarCoherencia(ClienteRequest solicitud, LocalDate nacimiento, List<String> errores) {
        String curp = solicitud.getCurp();
        String rfc = solicitud.getRfc();
        Validaciones.fechaCoincide("curp", curp, 4, nacimiento, errores);
        Validaciones.fechaCoincide("rfc", rfc, rfc.length() - 9, nacimiento, errores);

        Sexo sexo = Catalogo.leer(Sexo.class, solicitud.getSexo());
        if (curp.charAt(10) != sexo.getCodigo().charAt(0)) {
            errores.add("curp: el sexo que contiene (" + curp.charAt(10) + ") no coincide con el sexo capturado");
        }
    }

    private void lanzarSiHayErrores(String mensaje, List<String> errores) {
        if (!errores.isEmpty()) {
            throw new ValidacionException(mensaje, errores);
        }
    }
}
