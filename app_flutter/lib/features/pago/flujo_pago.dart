import '../../data/models/producto.dart';
import '../../data/models/referencia.dart';

/// Lo que pasa de la pantalla de referencia a la de confirmación. Solo se construye con una
/// referencia que el proveedor marcó como válida (o sin referencia en artículos canjeables).
class FlujoPago {
  FlujoPago({required this.producto, required this.monto, this.verificacion})
      : assert(verificacion == null || verificacion.esValida),
        assert(verificacion != null || !producto.requiereReferencia);

  final Producto producto;
  final double monto;
  final ReferenciaVerificada? verificacion;

  String? get referencia => verificacion?.referencia;

  double get comision => producto.comision;

  double get total => monto + comision;
}
