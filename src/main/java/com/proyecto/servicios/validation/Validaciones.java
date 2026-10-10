package com.proyecto.servicios.validation;

import com.proyecto.servicios.enums.Catalogo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Funciones de validación, una por tipo de dato. Están fuera del modelo:
 * los DTO solo transportan datos y aquí se decide si son válidos.
 *
 * Ninguna función lanza excepción: cada una agrega su mensaje a la lista de
 * errores, para poder avisarle al usuario de TODOS los campos incorrectos en
 * una sola respuesta 400 (Bad Request).
 */
public final class Validaciones {

    public static final int EDAD_MINIMA = 18;

    private static final String LETRAS = "[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]";

    /** Palabras de solo letras separadas por un espacio. */
    private static final Pattern SOLO_LETRAS = Pattern.compile("^[" + LETRAS + "]+( [" + LETRAS + "]+)*$");

    /** Texto libre acotado: letras, dígitos, espacios y la puntuación habitual de direcciones y razones sociales. */
    private static final Pattern TEXTO_GENERAL = Pattern.compile("^[" + LETRAS + "0-9 .,#&'/-]+$");

    /*
     * CURP: 4 letras + fecha AAMMDD + sexo (H/M/X) + entidad federativa
     *       + 3 consonantes + homoclave + dígito verificador = 18 caracteres.
     */
    private static final Pattern CURP = Pattern.compile(
            "^[A-Z][AEIOUX][A-Z]{2}"
                    + "\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])"
                    + "[HMX]"
                    + "(AS|BC|BS|CC|CL|CM|CS|CH|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QT|QR|SP|SL|SR|TC|TS|TL|VZ|YN|ZS|NE)"
                    + "[B-DF-HJ-NP-TV-Z]{3}"
                    + "[A-Z\\d]\\d$");

    /* RFC: 3 o 4 letras + fecha AAMMDD + homoclave de 3 = 12 o 13 caracteres. */
    private static final Pattern RFC = Pattern.compile(
            "^[A-ZÑ&]{3,4}\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])[A-Z\\d]{3}$");

    private static final Pattern CORREO = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");

    private static final Pattern DIEZ_DIGITOS = Pattern.compile("^\\d{10}$");
    private static final Pattern CINCO_DIGITOS = Pattern.compile("^\\d{5}$");

    /** Fecha simple año-mes-día. STRICT rechaza fechas imposibles como 2023-02-30. */
    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("uuMMdd");

    private Validaciones() {
    }

    // ============================ Textos ============================

    /** Solo letras y espacios, con longitud entre min y max. */
    public static void soloLetras(String campo, String valor, boolean obligatorio, int min, int max, List<String> errores) {
        texto(campo, valor, obligatorio, min, max, SOLO_LETRAS,
                "solo admite letras y espacios (se permiten acentos y ñ)", errores);
    }

    /** Letras, dígitos y puntuación básica, con longitud entre min y max. */
    public static void textoGeneral(String campo, String valor, boolean obligatorio, int min, int max, List<String> errores) {
        texto(campo, valor, obligatorio, min, max, TEXTO_GENERAL,
                "solo admite letras, números, espacios y estos signos: . , # & ' / -", errores);
    }

    // ===================== Identificadores oficiales =====================

    public static void curp(String campo, String valor, List<String> errores) {
        if (falta(campo, valor, true, errores)) {
            return;
        }
        if (valor.length() != 18) {
            errores.add(campo + ": debe contener exactamente 18 caracteres");
        } else if (!CURP.matcher(valor).matches()) {
            errores.add(campo + ": formato inválido; usa 4 letras, fecha AAMMDD, sexo H/M/X, "
                    + "clave de entidad, 3 consonantes, un carácter de homoclave y dígito verificador, "
                    + "todo en mayúsculas");
        }
    }

    public static void rfc(String campo, String valor, List<String> errores) {
        if (falta(campo, valor, true, errores)) {
            return;
        }
        if (valor.length() != 12 && valor.length() != 13) {
            errores.add(campo + ": debe contener 12 caracteres (persona moral) o 13 (persona física)");
        } else if (!RFC.matcher(valor).matches()) {
            errores.add(campo + ": formato inválido; debe iniciar con 3 o 4 letras, incluir fecha AAMMDD "
                    + "y terminar con 3 caracteres alfanuméricos, todo en mayúsculas");
        }
    }

    // ============================ Contacto ============================

    public static void correo(String campo, String valor, List<String> errores) {
        if (falta(campo, valor, true, errores)) {
            return;
        }
        String limpio = valor.trim();
        if (limpio.length() > 100) {
            errores.add(campo + ": admite máximo 100 caracteres");
        } else if (!CORREO.matcher(limpio).matches()) {
            errores.add(campo + ": formato inválido; usa el formato usuario@dominio.extensión "
                    + "(por ejemplo, nombre@correo.com)");
        }
    }

    public static void telefono(String campo, String valor, boolean obligatorio, List<String> errores) {
        if (falta(campo, valor, obligatorio, errores)) {
            return;
        }
        if (!DIEZ_DIGITOS.matcher(valor).matches()) {
            errores.add(campo + ": debe contener exactamente 10 dígitos");
        }
    }

    public static void codigoPostal(String campo, String valor, List<String> errores) {
        if (falta(campo, valor, true, errores)) {
            return;
        }
        if (!CINCO_DIGITOS.matcher(valor).matches()) {
            errores.add(campo + ": debe contener exactamente 5 dígitos");
        }
    }

    // ============================= Cuenta =============================

    public static void numeroCuenta(String campo, String valor, List<String> errores) {
        if (falta(campo, valor, true, errores)) {
            return;
        }
        if (!DIEZ_DIGITOS.matcher(valor).matches()) {
            errores.add(campo + ": debe contener exactamente 10 dígitos");
        }
    }

    /** Monto de dinero: obligatorio, mayor a cero, máximo 2 decimales y 10 dígitos enteros. */
    public static void monto(String campo, BigDecimal valor, List<String> errores) {
        if (valor == null) {
            errores.add(campo + ": es obligatorio");
            return;
        }
        if (valor.signum() <= 0) {
            errores.add(campo + ": debe ser mayor que 0");
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            errores.add(campo + ": admite máximo 2 decimales (por ejemplo, 15000.50)");
        }
        if (valor.precision() - valor.scale() > 10) {
            errores.add(campo + ": admite máximo 10 dígitos antes del punto decimal");
        }
    }

    // ============================= Fechas =============================

    /**
     * Fecha simple en formato AAAA-MM-DD o AAAA/MM/DD.
     * Devuelve la fecha ya convertida, o null si faltó o es inválida.
     */
    public static LocalDate fecha(String campo, String valor, List<String> errores) {
        if (falta(campo, valor, true, errores)) {
            return null;
        }
        try {
            return leerFecha(valor);
        } catch (DateTimeParseException e) {
            errores.add(campo + ": debe ser una fecha real con formato AAAA-MM-DD "
                    + "(también se acepta AAAA/MM/DD)");
            return null;
        }
    }

    /** Fecha de nacimiento: fecha válida, no futura y con la mayoría de edad cumplida. */
    public static LocalDate fechaNacimiento(String campo, String valor, List<String> errores) {
        LocalDate nacimiento = fecha(campo, valor, errores);
        if (nacimiento == null) {
            return null;
        }
        LocalDate hoy = LocalDate.now();
        if (nacimiento.isAfter(hoy)) {
            errores.add(campo + ": no puede ser una fecha futura");
        } else if (Period.between(nacimiento, hoy).getYears() < EDAD_MINIMA) {
            // Period cuenta años COMPLETOS: quien cumple 18 mañana todavía tiene 17.
            errores.add(campo + ": el cliente debe tener al menos " + EDAD_MINIMA + " años cumplidos");
        }
        return nacimiento;
    }

    /** Convierte el texto a fecha. Solo debe llamarse con valores que ya pasaron por fecha(). */
    public static LocalDate leerFecha(String valor) {
        return LocalDate.parse(valor.trim().replace('/', '-'), FECHA);
    }

    // ============================ Catálogos ============================

    /**
     * Valor de un enum: se acepta el nombre (MUJER) o su código de un carácter (M).
     * Devuelve el valor del enum, o null si faltó o no existe.
     */
    public static <E extends Enum<E> & Catalogo> E catalogo(String campo, String valor, Class<E> tipo, List<String> errores) {
        if (falta(campo, valor, true, errores)) {
            return null;
        }
        Optional<E> encontrado = Catalogo.buscar(tipo, valor);
        if (encontrado.isEmpty()) {
            String opciones = Arrays.stream(tipo.getEnumConstants())
                    .map(opcion -> opcion.name() + " (" + opcion.getCodigo() + ")")
                    .collect(Collectors.joining(", "));
            errores.add(campo + ": valor no reconocido. Escribe una de estas opciones "
                    + "(se acepta el nombre o su clave): " + opciones);
        }
        return encontrado.orElse(null);
    }

    // ============================== Login ==============================

    /** Contraseña nueva: 8 a 72 caracteres, sin espacios, con mayúscula, minúscula y dígito. */
    public static void contrasenaNueva(String campo, String valor, List<String> errores) {
        if (valor == null || valor.isEmpty()) {
            errores.add(campo + ": es obligatoria");
            return;
        }
        if (valor.length() < 8 || valor.length() > 72) {
            errores.add(campo + ": debe tener entre 8 y 72 caracteres");
        }
        if (valor.chars().anyMatch(Character::isWhitespace)) {
            errores.add(campo + ": no puede contener espacios ni otros caracteres en blanco");
        }
        boolean completa = valor.chars().anyMatch(Character::isUpperCase)
                && valor.chars().anyMatch(Character::isLowerCase)
                && valor.chars().anyMatch(Character::isDigit);
        if (!completa) {
            errores.add(campo + ": debe incluir al menos una letra mayúscula, una letra minúscula "
                    + "y un dígito");
        }
    }

    // ====================== Coherencia entre campos ======================

    /** Los dígitos 5 a 10 de la CURP y del RFC son la fecha de nacimiento en formato AAMMDD. */
    public static void fechaCoincide(String campo, String identificador, int inicio, LocalDate nacimiento, List<String> errores) {
        String esperado = nacimiento.format(FECHA_CORTA);
        if (!identificador.substring(inicio, inicio + 6).equals(esperado)) {
            errores.add(campo + ": la fecha AAMMDD del identificador no coincide con fechaNacimiento");
        }
    }

    // ============================ Auxiliares ============================

    public static void idPositivo(String campo, Integer valor, List<String> errores) {
        if (valor == null) {
            errores.add(campo + ": es obligatorio y debe ser un número entero");
        } else if (valor <= 0) {
            errores.add(campo + ": debe ser un número entero mayor que cero");
        }
    }

    /** Indica si el valor viene vacío; si además era obligatorio, registra el error. */
    private static boolean falta(String campo, String valor, boolean obligatorio, List<String> errores) {
        if (valor == null || valor.isBlank()) {
            if (obligatorio) {
                errores.add(campo + ": es obligatorio y no puede estar vacío");
            }
            return true;
        }
        return false;
    }

    private static void texto(String campo, String valor, boolean obligatorio, int min, int max,
                              Pattern formato, String mensajeFormato, List<String> errores) {
        if (falta(campo, valor, obligatorio, errores)) {
            return;
        }
        String limpio = valor.trim();
        if (limpio.length() < min || limpio.length() > max) {
            errores.add(campo + ": debe tener entre " + min + " y " + max
                    + " caracteres; se recibieron " + limpio.length());
        }
        if (!formato.matcher(limpio).matches()) {
            errores.add(campo + ": " + mensajeFormato);
        }
    }
}
