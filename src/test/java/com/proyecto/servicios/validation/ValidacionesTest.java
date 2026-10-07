package com.proyecto.servicios.validation;

import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.Sexo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValidacionesTest {

    private final List<String> errores = new ArrayList<>();

    // ------------------------------ Textos ------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"Zayda", "María José", "De la O"})
    void nombreValido(String nombre) {
        Validaciones.soloLetras("nombre", nombre, true, 2, 50, errores);
        assertThat(errores).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"A", "Ana3", "Ana_", "   "})
    void nombreInvalido(String nombre) {
        Validaciones.soloLetras("nombre", nombre, true, 2, 50, errores);
        assertThat(errores).hasSize(1);
    }

    @Test
    void campoOpcionalVacioNoGeneraError() {
        Validaciones.soloLetras("segundoNombre", null, false, 2, 50, errores);
        Validaciones.telefono("telefonoAlterno", null, false, errores);
        assertThat(errores).isEmpty();
    }

    // ---------------------------- CURP y RFC ----------------------------

    @Test
    void curpValida() {
        Validaciones.curp("curp", "VAVZ950312MGTRRY07", errores);
        assertThat(errores).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "VAVZ950312MGTRRY0",      // 17 caracteres
            "VAVZ950312MGTRRY077",    // 19 caracteres
            "vavz950312mgtrry07",     // minúsculas
            "VAVZ951312MGTRRY07",     // mes 13
            "VAVZ950312ZGTRRY07",     // sexo inválido
            "VAVZ950312MXXRRY07"      // entidad federativa inexistente
    })
    void curpInvalida(String curp) {
        Validaciones.curp("curp", curp, errores);
        assertThat(errores).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"VAVZ950312AB1", "ABC950312AB1"})
    void rfcDeDoceOTreceCaracteresEsValido(String rfc) {
        Validaciones.rfc("rfc", rfc, errores);
        assertThat(errores).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"VAVZ950312AB", "VAVZ950312AB12", "VAVZ953212AB1", "1AVZ950312AB1"})
    void rfcInvalido(String rfc) {
        Validaciones.rfc("rfc", rfc, errores);
        assertThat(errores).hasSize(1);
    }

    // ----------------------------- Contacto -----------------------------

    @ParameterizedTest
    @ValueSource(strings = {"zayda@correo", "a b@c.com", "@correo.com", "zayda.correo.com"})
    void correoInvalido(String correo) {
        Validaciones.correo("correo", correo, errores);
        assertThat(errores).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"418123456", "41812345678", "41812345a7"})
    void telefonoDebeTenerExactamenteDiezDigitos(String telefono) {
        Validaciones.telefono("telefonoMovil", telefono, true, errores);
        assertThat(errores).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"3780", "378000", "3780A"})
    void codigoPostalDebeTenerExactamenteCincoDigitos(String codigoPostal) {
        Validaciones.codigoPostal("codigoPostal", codigoPostal, errores);
        assertThat(errores).hasSize(1);
    }

    // ------------------------------ Montos ------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"15000", "15000.5", "15000.50", "0.01"})
    void montoValido(String monto) {
        Validaciones.monto("ingresoMensual", new BigDecimal(monto), errores);
        assertThat(errores).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5", "15000.555", "12345678901.00"})
    void montoInvalido(String monto) {
        Validaciones.monto("ingresoMensual", new BigDecimal(monto), errores);
        assertThat(errores).isNotEmpty();
    }

    // ------------------------------ Fechas ------------------------------

    @Test
    void fechaSimpleAnioMesDia() {
        assertThat(Validaciones.fecha("fecha", "2026-01-31", errores)).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(Validaciones.fecha("fecha", "2026/01/31", errores)).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(errores).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2023-02-30", "31-01-2026", "2026-1-5", "hola"})
    void fechaInvalida(String fecha) {
        assertThat(Validaciones.fecha("fecha", fecha, errores)).isNull();
        assertThat(errores).hasSize(1);
    }

    @Test
    void mayoriaDeEdadSeCalculaConAniosCumplidos() {
        LocalDate hoy = LocalDate.now();

        Validaciones.fechaNacimiento("fechaNacimiento", hoy.minusYears(18).toString(), errores);   // cumple 18 hoy
        assertThat(errores).isEmpty();

        Validaciones.fechaNacimiento("fechaNacimiento", hoy.minusYears(18).plusDays(1).toString(), errores);   // mañana
        assertThat(errores).hasSize(1);

        Validaciones.fechaNacimiento("fechaNacimiento", hoy.plusDays(1).toString(), errores);   // fecha futura
        assertThat(errores).hasSize(2);
    }

    // ----------------------------- Catálogos -----------------------------

    @Test
    void catalogoAceptaNombreOCodigoDeUnCaracter() {
        assertThat(Validaciones.catalogo("sexo", "MUJER", Sexo.class, errores)).isEqualTo(Sexo.MUJER);
        assertThat(Validaciones.catalogo("sexo", "m", Sexo.class, errores)).isEqualTo(Sexo.MUJER);
        assertThat(Validaciones.catalogo("estadoCivil", "U", EstadoCivil.class, errores)).isEqualTo(EstadoCivil.UNION_LIBRE);
        assertThat(errores).isEmpty();

        assertThat(Validaciones.catalogo("sexo", "Z", Sexo.class, errores)).isNull();
        assertThat(errores).hasSize(1);
    }

    // ---------------------------- Contraseña ----------------------------

    @ParameterizedTest
    @ValueSource(strings = {"segura123", "SEGURA123", "SeguraSinDigito", "Seg1", "Segura 123"})
    void contrasenaDebil(String contrasena) {
        Validaciones.contrasenaNueva("contrasena", contrasena, errores);
        assertThat(errores).isNotEmpty();
    }

    // ---------------------------- Coherencia ----------------------------

    @Test
    void laFechaDeLaCurpDebeCoincidirConLaDeNacimiento() {
        Validaciones.fechaCoincide("curp", "VAVZ950312MGTRRY07", 4, LocalDate.of(1995, 3, 12), errores);
        assertThat(errores).isEmpty();

        Validaciones.fechaCoincide("curp", "VAVZ950312MGTRRY07", 4, LocalDate.of(1995, 3, 13), errores);
        assertThat(errores).hasSize(1);
    }
}
