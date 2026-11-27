import 'package:flutter_test/flutter_test.dart';
import 'package:gestopago_app/core/validation/validadores.dart';

void main() {
  test('correo', () {
    expect(Validadores.correo('zayda@correo.com'), isNull);
    expect(Validadores.correo('sin-arroba'), isNotNull);
    expect(Validadores.correo(''), isNotNull);
  });

  test('contraseña nueva con las mismas reglas del backend', () {
    expect(Validadores.contrasenaNueva('Segura123'), isNull);
    expect(Validadores.contrasenaNueva('corta1A'), isNotNull);
    expect(Validadores.contrasenaNueva('sinmayuscula1'), isNotNull);
    expect(Validadores.contrasenaNueva('Con espacio1'), isNotNull);
  });

  test('referencia y teléfono', () {
    expect(Validadores.referencia('ABC-1234'), isNull);
    expect(Validadores.referencia('ab'), isNotNull);
    expect(Validadores.referencia('con espacio'), isNotNull);
    expect(Validadores.telefono('5512345678'), isNull);
    expect(Validadores.telefono('551234567'), isNotNull);
    expect(Validadores.telefono('', obligatorio: false), isNull);
  });

  test('importe con dos decimales dentro del rango', () {
    expect(Validadores.monto('150.50'), isNull);
    expect(Validadores.monto('0.50'), isNotNull);
    expect(Validadores.monto('10000.01'), isNotNull);
    expect(Validadores.monto('12.345'), isNotNull);
    expect(Validadores.monto('abc'), isNotNull);
  });

  test('código de seguridad, CURP y RFC', () {
    expect(Validadores.codigo2fa('123456'), isNull);
    expect(Validadores.codigo2fa('12345'), isNotNull);
    expect(Validadores.curp('VAVZ950312MGTRRY07'), isNull);
    expect(Validadores.curp('VAVZ950312'), isNotNull);
    expect(Validadores.rfc('VAVZ950312AB1'), isNull);
    expect(Validadores.rfc('123'), isNotNull);
  });
}
