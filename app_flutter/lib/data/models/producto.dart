/// Tipo de captura que pide PuntoRed para el producto (`tipoFront`).
enum TipoFront {
  recarga(1),
  pagoServicio(2),
  consultaReferencia(4),
  otro(0);

  const TipoFront(this.valor);

  final int valor;

  static TipoFront de(int? valor) =>
      TipoFront.values.firstWhere((t) => t.valor == valor, orElse: () => TipoFront.otro);
}

/// Un producto del catálogo de GestoPago (`GET /productos`).
class Producto {
  const Producto({
    required this.idProducto,
    required this.producto,
    required this.idServicio,
    required this.servicio,
    required this.idCategoria,
    required this.precio,
    required this.tipoFront,
    this.tipoReferencia,
    this.leyenda,
  });

  factory Producto.desdeJson(Map<String, dynamic> json) => Producto(
        idProducto: (json['idProducto'] as num).toInt(),
        producto: (json['producto'] as String? ?? '').trim(),
        idServicio: (json['idServicio'] as num).toInt(),
        servicio: (json['servicio'] as String? ?? '').trim(),
        idCategoria: (json['idCatTipoServicio'] as num?)?.toInt() ?? 0,
        precio: (json['precio'] as num?)?.toDouble() ?? 0,
        tipoFront: TipoFront.de((json['tipoFront'] as num?)?.toInt()),
        tipoReferencia: json['tipoReferencia'] as String?,
        leyenda: (json['leyenda'] as String?)?.trim(),
      );

  final int idProducto;
  final String producto;
  final int idServicio;
  final String servicio;
  final int idCategoria;

  /// En recargas es el importe a cobrar; en pagos de servicio es la comisión.
  final double precio;
  final TipoFront tipoFront;
  final String? tipoReferencia;
  final String? leyenda;

  static const _categoriasCanjeables = {10, 11};
  static const _referenciaEnTelefono = {'a', 'ab'};

  bool get esPagable => tipoFront == TipoFront.recarga || tipoFront == TipoFront.pagoServicio;

  bool get esRecarga => tipoFront == TipoFront.recarga;

  /// Artículos canjeables (tarjetas de prepago, entretenimiento): no se captura referencia.
  bool get esCanjeable => _categoriasCanjeables.contains(idCategoria);

  bool get requiereReferencia => !esCanjeable;

  /// En recargas con tipoReferencia "a"/"ab" el dato capturado es un teléfono de 10 dígitos.
  bool get pideTelefono => esRecarga && _referenciaEnTelefono.contains(tipoReferencia);

  /// En recargas el monto es el precio del catálogo; en pagos de servicio lo indica el recibo.
  bool get tieneMontoFijo => esRecarga;

  double get comision => tipoFront == TipoFront.pagoServicio ? precio : 0;
}

/// Agrupa los productos de un mismo proveedor para mostrarlo una sola vez en el catálogo.
class Servicio {
  const Servicio({
    required this.idServicio,
    required this.nombre,
    required this.idCategoria,
    required this.productos,
  });

  final int idServicio;
  final String nombre;
  final int idCategoria;
  final List<Producto> productos;

  String? get descripcion => productos
      .map((p) => p.leyenda)
      .firstWhere((l) => l != null && l.isNotEmpty, orElse: () => null);

  static List<Servicio> agrupar(Iterable<Producto> productos) {
    final porServicio = <int, List<Producto>>{};
    for (final producto in productos.where((p) => p.esPagable)) {
      porServicio.putIfAbsent(producto.idServicio, () => []).add(producto);
    }
    final servicios = porServicio.entries.map((entrada) {
      final lista = entrada.value..sort((a, b) => a.precio.compareTo(b.precio));
      return Servicio(
        idServicio: entrada.key,
        nombre: lista.first.servicio,
        idCategoria: lista.first.idCategoria,
        productos: List.unmodifiable(lista),
      );
    }).toList()
      ..sort((a, b) => a.nombre.toLowerCase().compareTo(b.nombre.toLowerCase()));
    return servicios;
  }
}
