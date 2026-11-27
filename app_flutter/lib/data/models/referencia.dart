enum EstadoReferencia {
  valida,
  invalida,
  noVerificada;

  static EstadoReferencia de(String? valor) => switch (valor) {
        'VALIDA' => EstadoReferencia.valida,
        'INVALIDA' => EstadoReferencia.invalida,
        _ => EstadoReferencia.noVerificada,
      };
}

/// Resultado de `POST /pagos/referencias/verificar`. Solo [EstadoReferencia.valida] permite pagar.
class ReferenciaVerificada {
  const ReferenciaVerificada({
    required this.estado,
    required this.referencia,
    required this.mensaje,
    required this.idServicio,
    required this.idProducto,
    this.codigoProveedor,
    this.saldo,
  });

  factory ReferenciaVerificada.desdeJson(Map<String, dynamic> json) => ReferenciaVerificada(
        estado: EstadoReferencia.de(json['estado'] as String?),
        referencia: json['referencia'] as String? ?? '',
        mensaje: json['mensaje'] as String? ?? '',
        idServicio: (json['idServicio'] as num?)?.toInt() ?? 0,
        idProducto: (json['idProducto'] as num?)?.toInt() ?? 0,
        codigoProveedor: json['codigoProveedor'] as String?,
        saldo: (json['saldo'] as num?)?.toDouble(),
      );

  final EstadoReferencia estado;
  final String referencia;
  final String mensaje;
  final int idServicio;
  final int idProducto;
  final String? codigoProveedor;

  /// Positivo: adeudo pendiente. Cero: al corriente. Negativo: saldo a favor.
  final double? saldo;

  bool get esValida => estado == EstadoReferencia.valida;
}
