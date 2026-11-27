import '../../core/api/api_client.dart';
import '../models/producto.dart';

class CatalogoRepository {
  CatalogoRepository(this._api);

  final ApiClient _api;
  List<Servicio>? _enMemoria;

  /// El backend sirve el catálogo desde PostgreSQL/Redis; aquí se guarda durante la sesión de la app.
  Future<List<Servicio>> consultarServicios({bool forzar = false}) async {
    final guardado = _enMemoria;
    if (guardado != null && !forzar) {
      return guardado;
    }
    final respuesta = await _api.get('/productos');
    final productos = respuesta.datosComoLista.map(Producto.desdeJson);
    return _enMemoria = Servicio.agrupar(productos);
  }

  Producto? buscarProducto(int idProducto) {
    for (final servicio in _enMemoria ?? const <Servicio>[]) {
      for (final producto in servicio.productos) {
        if (producto.idProducto == idProducto) {
          return producto;
        }
      }
    }
    return null;
  }
}
