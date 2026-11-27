import '../../core/api/api_client.dart';
import '../models/cuenta.dart';
import '../models/sesion.dart';

class AuthRepository {
  const AuthRepository(this._api);

  final ApiClient _api;

  Future<Sesion> iniciarSesion({required String correo, required String contrasena}) async {
    final respuesta = await _api.post('/login', cuerpo: {
      'correo': correo.trim().toLowerCase(),
      'contrasena': contrasena,
    });
    return Sesion.desdeLogin(respuesta.datosComoMapa);
  }

  /// `POST /clientes`. Devuelve el número de cuenta que se abrió.
  Future<String?> registrarCliente(Map<String, dynamic> cliente) async {
    final respuesta = await _api.post('/clientes', cuerpo: cliente);
    final datos = respuesta.datosComoMapa;
    final cuenta = datos['cuenta'];
    if (cuenta is Map) {
      return cuenta['numeroCuenta'] as String?;
    }
    return datos['numeroCuenta'] as String?;
  }

  Future<List<Nacionalidad>> consultarNacionalidades() async {
    final respuesta = await _api.get('/nacionalidades');
    return respuesta.datosComoLista.map(Nacionalidad.desdeJson).toList();
  }
}
