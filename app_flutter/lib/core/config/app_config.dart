import 'package:flutter/foundation.dart';

/// La URL del backend se define al compilar: `--dart-define=API_BASE_URL=https://...`.
/// Sin ella se usa el backend local (en el emulador de Android, localhost es 10.0.2.2).
class AppConfig {
  const AppConfig._();

  static const String _definida = String.fromEnvironment('API_BASE_URL');

  static String get apiBaseUrl {
    final url = _definida.isNotEmpty ? _definida : _urlLocal;
    return url.endsWith('/') ? url.substring(0, url.length - 1) : url;
  }

  static String get _urlLocal {
    if (!kIsWeb && defaultTargetPlatform == TargetPlatform.android) {
      return 'http://10.0.2.2:8091';
    }
    return 'http://localhost:8091';
  }

  static const Duration tiempoEspera = Duration(seconds: 30);

  /// sendTx puede tardar hasta 62 s antes de que el backend consulte confirmTx.
  static const Duration tiempoEsperaPago = Duration(seconds: 90);

  static String logoServicio(int idServicio) => '$apiBaseUrl/productos/servicios/$idServicio/logo';

  static String ayudaReferencia(int idServicio) =>
      '$apiBaseUrl/productos/servicios/$idServicio/ayuda-referencia';
}
