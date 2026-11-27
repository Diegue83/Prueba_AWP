import '../../core/api/api_client.dart';
import '../../core/config/app_config.dart';
import '../models/pago.dart';
import '../models/producto.dart';
import '../models/referencia.dart';

/// Lo que el cliente confirmó en la pantalla de resumen.
class SolicitudPago {
  const SolicitudPago({
    required this.numeroCuenta,
    required this.producto,
    required this.monto,
    required this.codigo2fa,
    this.referencia,
  });

  final String numeroCuenta;
  final Producto producto;
  final double monto;
  final String codigo2fa;

  /// Referencia validada (o teléfono en recargas); null en artículos canjeables.
  final String? referencia;

  Map<String, dynamic> aJson() => {
        'numeroCuenta': numeroCuenta,
        'idServicio': producto.idServicio,
        'idProducto': producto.idProducto,
        'monto': double.parse(monto.toStringAsFixed(2)),
        if (referencia != null && producto.pideTelefono) 'telefono': referencia,
        if (referencia != null && !producto.pideTelefono) 'referencia': referencia,
        'codigo2fa': codigo2fa,
      };
}

class PagoRepository {
  const PagoRepository(this._api);

  final ApiClient _api;

  Future<ReferenciaVerificada> verificarReferencia(Producto producto, String referencia) async {
    final respuesta = await _api.post('/pagos/referencias/verificar', cuerpo: {
      'idServicio': producto.idServicio,
      'idProducto': producto.idProducto,
      'referencia': referencia,
    });
    return ReferenciaVerificada.desdeJson(respuesta.datosComoMapa);
  }

  /// La [claveIdempotencia] se conserva entre reintentos: si la primera petición sí llegó,
  /// el backend devuelve el mismo pago en lugar de cobrar dos veces.
  Future<Pago> pagar(SolicitudPago solicitud, String claveIdempotencia) async {
    final respuesta = await _api.post(
      '/pagos',
      cuerpo: solicitud.aJson(),
      encabezados: {'Idempotency-Key': claveIdempotencia},
      tiempoEspera: AppConfig.tiempoEsperaPago,
    );
    return Pago.desdeJson(respuesta.datosComoMapa).conNombres(
      servicio: solicitud.producto.servicio,
      producto: solicitud.producto.producto,
      idServicio: solicitud.producto.idServicio,
      referencia: solicitud.referencia,
    );
  }

  Future<List<Pago>> consultarHistorial({int pagina = 0, int tamano = 20}) async {
    final respuesta = await _api.get('/pagos', parametros: {'pagina': '$pagina', 'tamano': '$tamano'});
    return respuesta.datosComoLista.map(Pago.desdeJson).toList();
  }

  Future<Pago> consultarPago(int idPago) async {
    final respuesta = await _api.get('/pagos/$idPago');
    return Pago.desdeJson(respuesta.datosComoMapa);
  }
}
