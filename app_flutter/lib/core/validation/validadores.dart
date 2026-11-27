/// Mismas reglas que valida el backend, para avisar al usuario antes de enviar.
class Validadores {
  const Validadores._();

  static final RegExp _correo = RegExp(r'^[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9-]+\.)+[A-Za-z]{2,}$');
  static final RegExp _diezDigitos = RegExp(r'^\d{10}$');
  static final RegExp _seisDigitos = RegExp(r'^\d{6}$');
  static final RegExp _referencia = RegExp(r'^[A-Za-z0-9-]{4,40}$');
  static final RegExp _curp = RegExp(r'^[A-Z][AEIOUX][A-Z]{2}\d{6}[HMX][A-Z]{2}[B-DF-HJ-NP-TV-Z]{3}[A-Z\d]\d$');
  static final RegExp _rfc = RegExp(r'^[A-ZÑ&]{3,4}\d{6}[A-Z\d]{3}$');
  static final RegExp _letras = RegExp(r'^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?: [A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$');
  static final RegExp _cincoDigitos = RegExp(r'^\d{5}$');

  static String? obligatorio(String? valor, [String campo = 'Este campo']) =>
      (valor == null || valor.trim().isEmpty) ? '$campo es obligatorio' : null;

  static String? correo(String? valor) {
    final texto = valor?.trim() ?? '';
    if (texto.isEmpty) {
      return 'Escribe tu correo';
    }
    return _correo.hasMatch(texto) ? null : 'El correo no tiene un formato válido';
  }

  static String? contrasenaLogin(String? valor) =>
      (valor == null || valor.isEmpty) ? 'Escribe tu contraseña' : null;

  static String? contrasenaNueva(String? valor) {
    final texto = valor ?? '';
    if (texto.length < 8 || texto.length > 72) {
      return 'Debe tener entre 8 y 72 caracteres';
    }
    if (texto.contains(RegExp(r'\s'))) {
      return 'No puede contener espacios';
    }
    final completa = texto.contains(RegExp('[A-Z]')) && texto.contains(RegExp('[a-z]')) && texto.contains(RegExp(r'\d'));
    return completa ? null : 'Incluye una mayúscula, una minúscula y un número';
  }

  static String? telefono(String? valor, {bool obligatorio = true}) {
    final texto = valor?.trim() ?? '';
    if (texto.isEmpty) {
      return obligatorio ? 'Escribe el número a 10 dígitos' : null;
    }
    return _diezDigitos.hasMatch(texto) ? null : 'Debe tener exactamente 10 dígitos';
  }

  static String? referencia(String? valor) {
    final texto = valor?.trim() ?? '';
    if (texto.isEmpty) {
      return 'Escribe la referencia de tu recibo';
    }
    return _referencia.hasMatch(texto) ? null : 'Entre 4 y 40 caracteres: letras, números y guiones';
  }

  static String? codigo2fa(String? valor) =>
      _seisDigitos.hasMatch(valor?.trim() ?? '') ? null : 'El código tiene 6 dígitos';

  static final RegExp _importe = RegExp(r'^\d{1,10}(\.\d{1,2})?$');

  static String? monto(String? valor, {double minimo = 1, double maximo = 10000}) {
    final texto = (valor ?? '').replaceAll(',', '').trim();
    if (!_importe.hasMatch(texto)) {
      return 'Escribe un importe válido, con máximo dos decimales';
    }
    final numero = double.parse(texto);
    if (numero < minimo || numero > maximo) {
      return 'Debe estar entre \$${minimo.toStringAsFixed(2)} y \$${maximo.toStringAsFixed(2)}';
    }
    return null;
  }

  static String? curp(String? valor) {
    final texto = valor?.trim().toUpperCase() ?? '';
    if (texto.length != 18) {
      return 'La CURP tiene 18 caracteres';
    }
    return _curp.hasMatch(texto) ? null : 'La CURP no tiene un formato válido';
  }

  static String? rfc(String? valor) {
    final texto = valor?.trim().toUpperCase() ?? '';
    return _rfc.hasMatch(texto) ? null : 'El RFC tiene 12 o 13 caracteres con formato válido';
  }

  static String? soloLetras(String? valor, {required String campo, bool obligatorio = true, int minimo = 2}) {
    final texto = valor?.trim() ?? '';
    if (texto.isEmpty) {
      return obligatorio ? '$campo es obligatorio' : null;
    }
    if (texto.length < minimo) {
      return '$campo debe tener al menos $minimo letras';
    }
    return _letras.hasMatch(texto) ? null : '$campo solo admite letras y espacios';
  }

  static String? codigoPostal(String? valor) =>
      _cincoDigitos.hasMatch(valor?.trim() ?? '') ? null : 'El código postal tiene 5 dígitos';
}
