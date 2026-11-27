import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:gestopago_app/core/api/api_client.dart';
import 'package:gestopago_app/core/api/api_exception.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

http.Response json(Object cuerpo, int estatus) =>
    http.Response(jsonEncode(cuerpo), estatus, headers: {'content-type': 'application/json; charset=utf-8'});

void main() {
  test('agrega el token y lee el sobre {codigo, mensaje, datos}', () async {
    late http.Request recibida;
    final api = ApiClient(
      urlBase: 'http://api',
      token: () => 'jwt-123',
      cliente: MockClient((peticion) async {
        recibida = peticion;
        return json({'codigo': 0, 'mensaje': 'ok', 'datos': [1, 2]}, 200);
      }),
    );

    final respuesta = await api.get('/pagos', parametros: {'pagina': '0'});

    expect(recibida.headers['Authorization'], 'Bearer jwt-123');
    expect(recibida.url.toString(), 'http://api/pagos?pagina=0');
    expect(respuesta.datos, [1, 2]);
  });

  test('un pago rechazado (422) no es error de la app: trae los datos del pago', () async {
    final api = ApiClient(
      urlBase: 'http://api',
      token: () => null,
      cliente: MockClient((_) async => json({'codigo': 1, 'mensaje': 'rechazado', 'datos': {'idPago': 1}}, 422)),
    );

    final respuesta = await api.post('/pagos', cuerpo: {});

    expect(respuesta.estatusHttp, 422);
    expect(respuesta.datosComoMapa['idPago'], 1);
  });

  test('las respuestas con "error" se convierten en ApiException con clave y detalles', () async {
    final api = ApiClient(
      urlBase: 'http://api',
      token: () => null,
      cliente: MockClient((_) async => json({
            'codigo': 1,
            'mensaje': 'Datos inválidos',
            'error': 'ERROR_VALIDACION',
            'detalles': ['referencia: muy corta'],
          }, 400)),
    );

    expect(
      () => api.post('/pagos/referencias/verificar', cuerpo: {}),
      throwsA(isA<ApiException>()
          .having((e) => e.clave, 'clave', 'ERROR_VALIDACION')
          .having((e) => e.mensajeCompleto, 'mensaje', contains('referencia: muy corta'))),
    );
  });

  test('solo NO_AUTENTICADO cierra la sesión; un código 2FA incorrecto no', () async {
    var expiradas = 0;
    var clave = 'NO_AUTENTICADO';
    final api = ApiClient(
      urlBase: 'http://api',
      token: () => 'jwt',
      alExpirarSesion: () => expiradas++,
      cliente: MockClient((_) async => json({'codigo': 1, 'mensaje': 'x', 'error': clave}, 401)),
    );

    await expectLater(api.get('/pagos'), throwsA(isA<ApiException>()));
    clave = 'CODIGO_2FA_INVALIDO';
    await expectLater(api.post('/pagos'), throwsA(isA<ApiException>()));

    expect(expiradas, 1);
  });

  test('sin conexión se informa como tal', () async {
    final api = ApiClient(
      urlBase: 'http://api',
      token: () => null,
      cliente: MockClient((_) async => throw http.ClientException('sin red')),
    );

    expect(() => api.get('/productos'),
        throwsA(isA<ApiException>().having((e) => e.sinRespuestaDelServidor, 'sin respuesta', isTrue)));
  });
}
