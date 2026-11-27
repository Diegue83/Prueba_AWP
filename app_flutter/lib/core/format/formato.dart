import 'package:intl/intl.dart';

class Formato {
  const Formato._();

  static final NumberFormat _moneda = NumberFormat.currency(locale: 'es_MX', symbol: r'$', decimalDigits: 2);
  static final DateFormat _fechaHora = DateFormat("d MMM yyyy, HH:mm", 'es_MX');
  static final DateFormat _fecha = DateFormat('d MMM yyyy', 'es_MX');

  static String moneda(num? valor) => valor == null ? '—' : _moneda.format(valor);

  static String fechaHora(DateTime? fecha) => fecha == null ? '—' : _fechaHora.format(fecha);

  static String fecha(DateTime? fecha) => fecha == null ? '—' : _fecha.format(fecha);

  /// Muestra solo los últimos 4 caracteres de un dato sensible (cuenta, teléfono).
  static String enmascarar(String? valor) {
    if (valor == null || valor.length <= 4) {
      return valor ?? '—';
    }
    return '•••• ${valor.substring(valor.length - 4)}';
  }
}
