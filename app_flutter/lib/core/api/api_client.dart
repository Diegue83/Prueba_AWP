import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../config/app_config.dart';
import 'api_exception.dart';

/// Sobre común del backend: `{codigo, mensaje, datos}`.
class RespuestaApi {
  const RespuestaApi({
    required this.estatusHttp,
    required this.codigo,
    required this.mensaje,
    required this.datos,
  });

  final int estatusHttp;
  final int codigo;
  final String mensaje;
  final Object? datos;

  Map<String, dynamic> get datosComoMapa => (datos as Map).cast<String, dynamic>();

  List<Map<String, dynamic>> get datosComoLista =>
      (datos as List? ?? const []).map((e) => (e as Map).cast<String, dynamic>()).toList();
}

typedef ProveedorToken = String? Function();

/// Único punto de salida HTTP de la app: agrega el token, aplica tiempos de espera y convierte
/// cualquier respuesta de error del backend (las que traen `error`) en [ApiException].
class ApiClient {
  ApiClient({
    http.Client? cliente,
    String? urlBase,
    required ProveedorToken token,
    void Function()? alExpirarSesion,
  })  : _cliente = cliente ?? http.Client(),
        _urlBase = urlBase ?? AppConfig.apiBaseUrl,
        _token = token,
        _alExpirarSesion = alExpirarSesion;

  final http.Client _cliente;
  final String _urlBase;
  final ProveedorToken _token;
  void Function()? _alExpirarSesion;

  set alExpirarSesion(void Function()? accion) => _alExpirarSesion = accion;

  Future<RespuestaApi> get(String ruta, {Map<String, String>? parametros}) =>
      _enviar('GET', ruta, parametros: parametros);

  Future<RespuestaApi> post(
    String ruta, {
    Object? cuerpo,
    Map<String, String> encabezados = const {},
    Duration? tiempoEspera,
  }) =>
      _enviar('POST', ruta, cuerpo: cuerpo, encabezados: encabezados, tiempoEspera: tiempoEspera);

  Future<RespuestaApi> _enviar(
    String metodo,
    String ruta, {
    Object? cuerpo,
    Map<String, String>? parametros,
    Map<String, String> encabezados = const {},
    Duration? tiempoEspera,
  }) async {
    final uri = Uri.parse('$_urlBase$ruta').replace(queryParameters: parametros);
    final peticion = http.Request(metodo, uri)
      ..headers.addAll({
        'Accept': 'application/json',
        if (cuerpo != null) 'Content-Type': 'application/json; charset=utf-8',
        ...encabezados,
      });
    final token = _token();
    if (token != null) {
      peticion.headers['Authorization'] = 'Bearer $token';
    }
    if (cuerpo != null) {
      peticion.body = jsonEncode(cuerpo);
    }

    final http.Response respuesta;
    try {
      final enviada = await _cliente.send(peticion).timeout(tiempoEspera ?? AppConfig.tiempoEspera);
      respuesta = await http.Response.fromStream(enviada);
    } on TimeoutException {
      throw const ApiException.tiempoAgotado();
    } on http.ClientException {
      throw const ApiException.sinConexion();
    }
    return _interpretar(respuesta);
  }

  RespuestaApi _interpretar(http.Response respuesta) {
    final Object? json = _decodificar(respuesta);
    if (json is! Map) {
      throw ApiException(
        mensaje: 'El servidor respondió algo inesperado (HTTP ${respuesta.statusCode}).',
        estatusHttp: respuesta.statusCode,
      );
    }
    final cuerpo = json.cast<String, dynamic>();
    final esError = cuerpo.containsKey('error') || respuesta.statusCode >= 500 || respuesta.statusCode == 401;
    if (esError) {
      final error = ApiException(
        mensaje: (cuerpo['mensaje'] as String?) ?? 'Ocurrió un error (HTTP ${respuesta.statusCode}).',
        estatusHttp: respuesta.statusCode,
        clave: cuerpo['error'] as String?,
        detalles: (cuerpo['detalles'] as List? ?? const []).map((e) => e.toString()).toList(),
      );
      if (error.sesionExpirada) {
        _alExpirarSesion?.call();
      }
      throw error;
    }
    return RespuestaApi(
      estatusHttp: respuesta.statusCode,
      codigo: (cuerpo['codigo'] as num?)?.toInt() ?? 0,
      mensaje: (cuerpo['mensaje'] as String?) ?? '',
      datos: cuerpo['datos'],
    );
  }

  Object? _decodificar(http.Response respuesta) {
    if (respuesta.bodyBytes.isEmpty) {
      return null;
    }
    try {
      return jsonDecode(utf8.decode(respuesta.bodyBytes));
    } on FormatException {
      return null;
    }
  }

  void cerrar() => _cliente.close();
}
