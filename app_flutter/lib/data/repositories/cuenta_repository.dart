import '../../core/api/api_client.dart';
import '../models/cuenta.dart';

class CuentaRepository {
  const CuentaRepository(this._api);

  final ApiClient _api;

  Future<Saldo> consultarSaldo(String numeroCuenta) async {
    final respuesta = await _api.post('/cuentas/saldo', cuerpo: {'numeroCuenta': numeroCuenta});
    return Saldo.desdeJson(respuesta.datosComoMapa);
  }

  Future<RegistroSegundoFactor> registrarSegundoFactor() async {
    final respuesta = await _api.post('/2fa/registro');
    return RegistroSegundoFactor.desdeJson(respuesta.datosComoMapa);
  }

  Future<String> activarSegundoFactor(String codigo) async {
    final respuesta = await _api.post('/2fa/activar', cuerpo: {'codigo': codigo});
    return respuesta.mensaje;
  }
}
