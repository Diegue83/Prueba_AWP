/// Estatus que devuelve el backend. Solo [aplicado] significa que el proveedor confirmó el pago.
enum EstatusPago {
  aplicado,
  rechazado,
  pendiente,
  enConciliacion,
  revisionManual,
  desconocido;

  static EstatusPago de(String? valor) => switch (valor) {
        'APLICADO' => EstatusPago.aplicado,
        'RECHAZADO' => EstatusPago.rechazado,
        'PENDIENTE' => EstatusPago.pendiente,
        'EN_CONCILIACION' => EstatusPago.enConciliacion,
        'REVISION_MANUAL' => EstatusPago.revisionManual,
        _ => EstatusPago.desconocido,
      };

  bool get esExitoso => this == EstatusPago.aplicado;

  bool get esRechazado => this == EstatusPago.rechazado;

  bool get estaSinConfirmar => !esExitoso && !esRechazado;

  String get etiqueta => switch (this) {
        EstatusPago.aplicado => 'Exitoso',
        EstatusPago.rechazado => 'Rechazado',
        EstatusPago.pendiente => 'Pendiente',
        EstatusPago.enConciliacion => 'En verificación',
        EstatusPago.revisionManual => 'En revisión',
        EstatusPago.desconocido => 'Sin confirmar',
      };
}

/// Un pago del cliente (`POST /pagos`, `GET /pagos`, `GET /pagos/{id}`).
class Pago {
  const Pago({
    required this.idPago,
    required this.estatus,
    required this.idProducto,
    required this.mensaje,
    this.idServicio,
    this.servicio,
    this.producto,
    this.referencia,
    this.monto,
    this.comision,
    this.total,
    this.numAutorizacion,
    this.codigoProveedor,
    this.fechaOperacion,
    this.fechaActualizacion,
  });

  factory Pago.desdeJson(Map<String, dynamic> json) => Pago(
        idPago: (json['idPago'] as num).toInt(),
        estatus: EstatusPago.de(json['estatus'] as String?),
        idServicio: (json['idServicio'] as num?)?.toInt(),
        idProducto: (json['idProducto'] as num?)?.toInt() ?? 0,
        servicio: json['servicio'] as String?,
        producto: json['producto'] as String?,
        referencia: json['referencia'] as String?,
        monto: (json['monto'] as num?)?.toDouble(),
        comision: (json['comision'] as num?)?.toDouble(),
        total: (json['total'] as num?)?.toDouble(),
        numAutorizacion: json['numAutorizacion'] as String?,
        codigoProveedor: json['codigoProveedor'] as String?,
        mensaje: json['mensaje'] as String? ?? '',
        fechaOperacion: DateTime.tryParse(json['fechaOperacion'] as String? ?? ''),
        fechaActualizacion: DateTime.tryParse(json['fecha'] as String? ?? ''),
      );

  final int idPago;
  final EstatusPago estatus;
  final int? idServicio;
  final int idProducto;
  final String? servicio;
  final String? producto;
  final String? referencia;
  final double? monto;
  final double? comision;
  final double? total;
  final String? numAutorizacion;
  final String? codigoProveedor;
  final String mensaje;
  final DateTime? fechaOperacion;
  final DateTime? fechaActualizacion;

  DateTime? get fecha => fechaOperacion ?? fechaActualizacion;

  /// Folio que se muestra al cliente: el número de autorización del proveedor o, si no hay, el número de pago.
  String get folio => numAutorizacion ?? 'Pago #$idPago';

  Pago conNombres({String? servicio, String? producto, int? idServicio, String? referencia}) => Pago(
        idPago: idPago,
        estatus: estatus,
        idServicio: this.idServicio ?? idServicio,
        idProducto: idProducto,
        servicio: this.servicio ?? servicio,
        producto: this.producto ?? producto,
        referencia: this.referencia ?? referencia,
        monto: monto,
        comision: comision,
        total: total,
        numAutorizacion: numAutorizacion,
        codigoProveedor: codigoProveedor,
        mensaje: mensaje,
        fechaOperacion: fechaOperacion,
        fechaActualizacion: fechaActualizacion,
      );
}
