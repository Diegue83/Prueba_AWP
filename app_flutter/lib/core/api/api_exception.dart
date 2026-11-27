/// Error devuelto por el backend o por la red, ya traducido a un mensaje para el usuario.
class ApiException implements Exception {
  const ApiException({
    required this.mensaje,
    this.estatusHttp,
    this.clave,
    this.detalles = const [],
  });

  const ApiException.sinConexion()
      : mensaje = 'No hay conexión con el servidor. Revisa tu red e intenta de nuevo.',
        estatusHttp = null,
        clave = 'SIN_CONEXION',
        detalles = const [];

  const ApiException.tiempoAgotado()
      : mensaje = 'El servidor tardó demasiado en responder.',
        estatusHttp = null,
        clave = 'TIEMPO_AGOTADO',
        detalles = const [];

  final String mensaje;
  final int? estatusHttp;
  final String? clave;
  final List<String> detalles;

  bool get sesionExpirada => estatusHttp == 401 && clave == 'NO_AUTENTICADO';

  bool get sinRespuestaDelServidor => clave == 'SIN_CONEXION' || clave == 'TIEMPO_AGOTADO';

  String get mensajeCompleto => detalles.isEmpty ? mensaje : '$mensaje\n• ${detalles.join('\n• ')}';

  @override
  String toString() => 'ApiException($estatusHttp, $clave): $mensaje';
}
