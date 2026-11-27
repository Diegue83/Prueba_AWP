import 'package:flutter_test/flutter_test.dart';
import 'package:gestopago_app/data/models/pago.dart';
import 'package:gestopago_app/data/models/producto.dart';
import 'package:gestopago_app/data/models/referencia.dart';
import 'package:gestopago_app/data/models/sesion.dart';
import 'package:gestopago_app/data/repositories/pago_repository.dart';

Map<String, dynamic> productoJson({
  int idProducto = 1,
  int idServicio = 10,
  String servicio = 'TELCEL',
  int tipoFront = 1,
  num precio = 50,
  int categoria = 2,
  String? tipoReferencia = 'a',
}) =>
    {
      'idProducto': idProducto,
      'producto': 'Producto $idProducto',
      'idServicio': idServicio,
      'servicio': servicio,
      'idCatTipoServicio': categoria,
      'precio': precio,
      'tipoFront': tipoFront,
      'tipoReferencia': tipoReferencia,
    };

void main() {
  group('Producto y Servicio', () {
    test('agrupa por proveedor, ordena por precio y descarta lo que no se puede pagar', () {
      final productos = [
        productoJson(idProducto: 2, precio: 100),
        productoJson(idProducto: 1, precio: 20),
        productoJson(idProducto: 3, idServicio: 20, servicio: 'CFE', tipoFront: 2, precio: 8, categoria: 5),
        productoJson(idProducto: 4, idServicio: 20, servicio: 'CFE', tipoFront: 4, categoria: 5),
        productoJson(idProducto: 5, idServicio: 30, servicio: 'Cash in', tipoFront: 30),
      ].map(Producto.desdeJson);

      final servicios = Servicio.agrupar(productos);

      expect(servicios.map((s) => s.nombre), ['CFE', 'TELCEL']);
      expect(servicios[1].productos.map((p) => p.idProducto), [1, 2]);
      expect(servicios[0].productos.single.idProducto, 3);
    });

    test('reglas de captura según tipoFront, tipoReferencia y categoría', () {
      final recargaTelefono = Producto.desdeJson(productoJson());
      final recargaConReferencia = Producto.desdeJson(productoJson(tipoReferencia: 'b'));
      final pagoServicio = Producto.desdeJson(productoJson(tipoFront: 2, precio: 9.5, tipoReferencia: 'b'));
      final canjeable = Producto.desdeJson(productoJson(categoria: 10));

      expect(recargaTelefono.pideTelefono, isTrue);
      expect(recargaTelefono.tieneMontoFijo, isTrue);
      expect(recargaTelefono.comision, 0);
      expect(recargaConReferencia.pideTelefono, isFalse);
      expect(pagoServicio.tieneMontoFijo, isFalse);
      expect(pagoServicio.comision, 9.5);
      expect(canjeable.requiereReferencia, isFalse);
    });
  });

  group('SolicitudPago', () {
    test('en recargas con teléfono lo manda en "telefono" y en pagos de servicio en "referencia"', () {
      final recarga = SolicitudPago(
        numeroCuenta: '1000000001',
        producto: Producto.desdeJson(productoJson()),
        monto: 50,
        codigo2fa: '123456',
        referencia: '5512345678',
      ).aJson();
      final servicio = SolicitudPago(
        numeroCuenta: '1000000001',
        producto: Producto.desdeJson(productoJson(tipoFront: 2, tipoReferencia: 'b')),
        monto: 345.678,
        codigo2fa: '123456',
        referencia: 'ABC-123',
      ).aJson();

      expect(recarga['telefono'], '5512345678');
      expect(recarga.containsKey('referencia'), isFalse);
      expect(servicio['referencia'], 'ABC-123');
      expect(servicio.containsKey('telefono'), isFalse);
      expect(servicio['monto'], 345.68);
    });
  });

  group('Pago', () {
    test('solo APLICADO es exitoso; los estados intermedios quedan sin confirmar', () {
      expect(EstatusPago.de('APLICADO').esExitoso, isTrue);
      expect(EstatusPago.de('RECHAZADO').esRechazado, isTrue);
      for (final valor in ['PENDIENTE', 'EN_CONCILIACION', 'REVISION_MANUAL', 'OTRO', null]) {
        expect(EstatusPago.de(valor).estaSinConfirmar, isTrue, reason: '$valor');
      }
    });

    test('lee el historial del backend', () {
      final pago = Pago.desdeJson({
        'idPago': 9,
        'estatus': 'EN_CONCILIACION',
        'idServicio': 10,
        'idProducto': 1,
        'servicio': 'TELCEL',
        'referencia': '5512345678',
        'monto': 100,
        'comision': 0,
        'total': 100,
        'mensaje': 'en verificación',
        'fechaOperacion': '2026-11-26T10:15:30.123',
        'fecha': '2026-11-26T10:16:00',
      });

      expect(pago.folio, 'Pago #9');
      expect(pago.fecha, DateTime(2026, 11, 26, 10, 15, 30, 123));
      expect(pago.estatus.etiqueta, 'En verificación');
    });
  });

  test('ReferenciaVerificada: solo VALIDA permite continuar', () {
    expect(ReferenciaVerificada.desdeJson({'estado': 'VALIDA'}).esValida, isTrue);
    expect(ReferenciaVerificada.desdeJson({'estado': 'INVALIDA'}).esValida, isFalse);
    expect(ReferenciaVerificada.desdeJson({'estado': 'NO_VERIFICADA'}).esValida, isFalse);
    expect(ReferenciaVerificada.desdeJson({}).estado, EstadoReferencia.noVerificada);
  });

  test('Sesion calcula su vencimiento y se guarda sin perder datos', () {
    final ahora = DateTime(2026, 11, 26, 10);
    final sesion = Sesion.desdeLogin({
      'clienteId': 7,
      'nombreCompleto': 'Zayda Vargas',
      'correo': 'zayda@correo.com',
      'numeroCuenta': '1000000001',
      'token': 'jwt',
      'expiraEnMinutos': 30,
    }, ahora: ahora);

    expect(sesion.estaVigente(ahora.add(const Duration(minutes: 29))), isTrue);
    expect(sesion.estaVigente(ahora.add(const Duration(minutes: 30))), isFalse);
    expect(Sesion.desdeJson(sesion.aJson()).expiraEn, sesion.expiraEn);
  });
}
