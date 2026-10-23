package com.proyecto.servicios.validation;

import com.proyecto.servicios.enums.Catalogo;
import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.Sexo;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.onboarding.ClienteDatos;
import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.DomicilioRequest;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        validarCuerpoPresente(solicitud);
        List<String> errores = new ArrayList<>();
        rechazarSiIncluido("id", solicitud.getId(), errores);
        rechazarSiIncluido("numeroCuenta", solicitud.getNumeroCuenta(), errores);
        rechazarSiIncluido("activo", solicitud.getActivo(), errores);
        rechazarSiIncluido("fechaRegistroDesde", solicitud.getFechaRegistroDesde(), errores);
        rechazarSiIncluido("fechaRegistroHasta", solicitud.getFechaRegistroHasta(), errores);
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

    public void validarActualizacion(ClienteRequest solicitud) {
        validarCuerpoPresente(solicitud);
        List<String> errores = new ArrayList<>();
        Validaciones.idPositivo("id", solicitud.getId(), errores);
        if (solicitud.getCurp() != null) {
            errores.add("curp: no se puede modificar");
        }
        if (solicitud.getRfc() != null) {
            errores.add("rfc: no se puede modificar");
        }
        if (solicitud.getContrasena() != null) {
            errores.add("contrasena: no se puede modificar");
        }
        if (solicitud.getNumeroCuenta() != null) {
            errores.add("numeroCuenta: no se puede modificar");
        }
        rechazarSiIncluido("activo", solicitud.getActivo(), errores);
        rechazarSiIncluido("fechaRegistroDesde", solicitud.getFechaRegistroDesde(), errores);
        rechazarSiIncluido("fechaRegistroHasta", solicitud.getFechaRegistroHasta(), errores);
        validarDatosModificables(solicitud, errores);
        lanzarSiHayErrores(MENSAJE_CUERPO, errores);
    }

    public void validarBaja(ClienteRequest solicitud) {
        validarCuerpoPresente(solicitud);
        List<String> errores = new ArrayList<>();
        Validaciones.idPositivo("id", solicitud.getId(), errores);
        rechazarDatosModificables(solicitud, errores, true);
        rechazarSiIncluido("curp", solicitud.getCurp(), errores);
        rechazarSiIncluido("rfc", solicitud.getRfc(), errores);
        rechazarSiIncluido("contrasena", solicitud.getContrasena(), errores);
        rechazarSiIncluido("numeroCuenta", solicitud.getNumeroCuenta(), errores);
        rechazarSiIncluido("activo", solicitud.getActivo(), errores);
        rechazarSiIncluido("fechaRegistroDesde", solicitud.getFechaRegistroDesde(), errores);
        rechazarSiIncluido("fechaRegistroHasta", solicitud.getFechaRegistroHasta(), errores);
        lanzarSiHayErrores(MENSAJE_CUERPO, errores);
    }

    public void validarConsulta(ClienteRequest solicitud) {
        validarCuerpoPresente(solicitud);
        List<String> errores = new ArrayList<>();
        rechazarDatosModificables(solicitud, errores, false);
        rechazarSiIncluido("contrasena", solicitud.getContrasena(), errores);
        if (solicitud.getId() != null) {
            Validaciones.idPositivo("id", solicitud.getId(), errores);
        }
        if (solicitud.getCurp() != null) {
            Validaciones.curp("curp", solicitud.getCurp().toUpperCase(Locale.ROOT), errores);
        }
        if (solicitud.getRfc() != null) {
            Validaciones.rfc("rfc", solicitud.getRfc().toUpperCase(Locale.ROOT), errores);
        }
        if (solicitud.getCorreo() != null) {
            Validaciones.correo("correo", solicitud.getCorreo(), errores);
        }
        if (solicitud.getNumeroCuenta() != null) {
            Validaciones.numeroCuenta("numeroCuenta", solicitud.getNumeroCuenta(), errores);
        }
        lanzarSiHayErrores(MENSAJE_CUERPO, errores);
    }

    // ======================== POST /login ========================

    public void validarLogin(LoginRequest solicitud) {
        validarCuerpoPresente(solicitud);
        List<String> errores = new ArrayList<>();
        Validaciones.correo("correo", solicitud.getCorreo(), errores);
        if (solicitud.getContrasena() == null || solicitud.getContrasena().isEmpty()) {
            errores.add("contrasena: es obligatoria y no puede estar vacía");
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
            errores.add("desde: debe ser igual o anterior a 'hasta'; revisa el orden de las fechas");
        }
        lanzarSiHayErrores(MENSAJE_PARAMETROS, errores);
        return new LocalDate[]{fechaDesde, fechaHasta};
    }

    // ===================== Métodos de apoyo =====================

    /** Campos comunes del alta y de la actualización. Devuelve la fecha de nacimiento ya convertida. */
    private LocalDate validarDatosModificables(ClienteDatos datos, List<String> errores) {
        // Datos personales
        Validaciones.soloLetras("nombre", datos.getNombre(), true, 3, 50, errores);
        Validaciones.soloLetras("segundoNombre", datos.getSegundoNombre(), false, 3, 50, errores);
        Validaciones.soloLetras("apellidoPaterno", datos.getApellidoPaterno(), true, 3, 50, errores);
        Validaciones.soloLetras("apellidoMaterno", datos.getApellidoMaterno(), true, 3, 50, errores);
        LocalDate nacimiento = Validaciones.fechaNacimiento("fechaNacimiento", datos.getFechaNacimiento(), errores);
        Validaciones.catalogo("sexo", datos.getSexo(), Sexo.class, errores);
        Validaciones.soloLetras("nacionalidad", datos.getNacionalidad(), true, 2, 40, errores);
        Validaciones.catalogo("estadoCivil", datos.getEstadoCivil(), EstadoCivil.class, errores);

        // Datos de contacto
        Validaciones.correo("correo", datos.getCorreo(), errores);
        Validaciones.telefono("telefonoMovil", datos.getTelefonoMovil(), true, errores);
        Validaciones.telefono("telefonoAlterno", datos.getTelefonoAlterno(), false, errores);
        if (datos.getTelefonoAlterno() != null && datos.getTelefonoAlterno().equals(datos.getTelefonoMovil())) {
            errores.add("telefonoAlterno: debe ser distinto de telefonoMovil");
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
            errores.add("domicilio: es obligatorio; incluye un objeto con calle, número exterior, colonia, "
                    + "municipio, estado, código postal y país");
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

    private void rechazarDatosModificables(ClienteRequest solicitud, List<String> errores,
                                           boolean rechazarCorreo) {
        rechazarSiIncluido("nombre", solicitud.getNombre(), errores);
        rechazarSiIncluido("segundoNombre", solicitud.getSegundoNombre(), errores);
        rechazarSiIncluido("apellidoPaterno", solicitud.getApellidoPaterno(), errores);
        rechazarSiIncluido("apellidoMaterno", solicitud.getApellidoMaterno(), errores);
        rechazarSiIncluido("fechaNacimiento", solicitud.getFechaNacimiento(), errores);
        rechazarSiIncluido("sexo", solicitud.getSexo(), errores);
        rechazarSiIncluido("nacionalidad", solicitud.getNacionalidad(), errores);
        rechazarSiIncluido("estadoCivil", solicitud.getEstadoCivil(), errores);
        if (rechazarCorreo) {
            rechazarSiIncluido("correo", solicitud.getCorreo(), errores);
        }
        rechazarSiIncluido("telefonoMovil", solicitud.getTelefonoMovil(), errores);
        rechazarSiIncluido("telefonoAlterno", solicitud.getTelefonoAlterno(), errores);
        rechazarSiIncluido("domicilio", solicitud.getDomicilio(), errores);
        rechazarSiIncluido("ocupacion", solicitud.getOcupacion(), errores);
        rechazarSiIncluido("empresa", solicitud.getEmpresa(), errores);
        rechazarSiIncluido("ingresoMensual", solicitud.getIngresoMensual(), errores);
    }

    private void rechazarSiIncluido(String campo, Object valor, List<String> errores) {
        if (valor != null) {
            errores.add(campo + ": no se admite en esta operación");
        }
    }

    /** La CURP y el RFC contienen la fecha de nacimiento, y la CURP además el sexo: deben coincidir con lo capturado. */
    private void validarCoherencia(ClienteRequest solicitud, LocalDate nacimiento, List<String> errores) {
        String curp = solicitud.getCurp();
        String rfc = solicitud.getRfc();
        Validaciones.fechaCoincide("curp", curp, 4, nacimiento, errores);
        Validaciones.fechaCoincide("rfc", rfc, rfc.length() - 9, nacimiento, errores);

        Sexo sexo = Catalogo.leer(Sexo.class, solicitud.getSexo());
        if (curp.charAt(10) != sexo.getCodigo().charAt(0)) {
            errores.add("curp: el sexo indicado en la posición 11 no coincide con el campo sexo");
        }
    }

    private void lanzarSiHayErrores(String mensaje, List<String> errores) {
        if (!errores.isEmpty()) {
            throw new ValidacionException(mensaje, errores);
        }
    }

    private void validarCuerpoPresente(Object solicitud) {
        if (solicitud == null) {
            throw new ValidacionException(MENSAJE_CUERPO,
                    List.of("cuerpo: no puede ser null; envía un objeto JSON con los datos requeridos"));
        }
    }
}
