import 'package:flutter_test/flutter_test.dart';
import 'package:gestopago_app/core/session/session_controller.dart';
import 'package:gestopago_app/core/session/session_store.dart';
import 'package:gestopago_app/data/models/sesion.dart';

class AlmacenEnMemoria implements SessionStore {
  Sesion? guardada;

  @override
  Future<void> borrar() async => guardada = null;

  @override
  Future<void> guardar(Sesion sesion) async => guardada = sesion;

  @override
  Future<Sesion?> leer() async => guardada;
}

Sesion sesion(DateTime expiraEn) => Sesion(
      clienteId: 7,
      nombreCompleto: 'Zayda',
      correo: 'z@c.com',
      numeroCuenta: '1000000001',
      token: 'jwt',
      expiraEn: expiraEn,
    );

void main() {
  test('restaura una sesión vigente', () async {
    final almacen = AlmacenEnMemoria()..guardada = sesion(DateTime.now().add(const Duration(minutes: 10)));
    final controlador = SessionController(almacen: almacen);

    await controlador.restaurar();

    expect(controlador.autenticado, isTrue);
    expect(controlador.token, 'jwt');
    controlador.dispose();
  });

  test('descarta una sesión vencida', () async {
    final almacen = AlmacenEnMemoria()..guardada = sesion(DateTime.now().subtract(const Duration(minutes: 1)));
    final controlador = SessionController(almacen: almacen);

    await controlador.restaurar();

    expect(controlador.estado, EstadoSesion.sinSesion);
    expect(almacen.guardada, isNull);
    controlador.dispose();
  });

  test('al expirar borra la sesión y deja un aviso para el login', () async {
    final almacen = AlmacenEnMemoria()..guardada = sesion(DateTime.now().add(const Duration(minutes: 10)));
    final controlador = SessionController(almacen: almacen);
    await controlador.restaurar();

    controlador.sesionExpirada();
    await Future<void>.delayed(Duration.zero);

    expect(controlador.autenticado, isFalse);
    expect(controlador.aviso, contains('expiró'));
    expect(almacen.guardada, isNull);
    controlador.dispose();
  });
}
