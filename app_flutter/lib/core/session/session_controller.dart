import 'dart:async';

import 'package:flutter/foundation.dart';

import '../../data/models/sesion.dart';
import '../../data/repositories/auth_repository.dart';
import 'session_store.dart';

enum EstadoSesion { cargando, sinSesion, autenticado }

class SessionController extends ChangeNotifier {
  SessionController({required SessionStore almacen, DateTime Function()? reloj})
      : _almacen = almacen,
        _reloj = reloj ?? DateTime.now;

  final SessionStore _almacen;
  final DateTime Function() _reloj;
  AuthRepository? _auth;
  Timer? _temporizador;

  EstadoSesion _estado = EstadoSesion.cargando;
  Sesion? _sesion;
  String? _aviso;

  EstadoSesion get estado => _estado;

  Sesion? get sesion => _sesion;

  String? get token => _sesion?.token;

  bool get autenticado => _estado == EstadoSesion.autenticado;

  /// Mensaje para la pantalla de inicio de sesión (por ejemplo, "tu sesión expiró").
  String? get aviso => _aviso;

  set auth(AuthRepository repositorio) => _auth = repositorio;

  Future<void> restaurar() async {
    final guardada = await _almacen.leer();
    if (guardada != null && guardada.estaVigente(_reloj())) {
      _activar(guardada);
    } else {
      await _almacen.borrar();
      _estado = EstadoSesion.sinSesion;
    }
    notifyListeners();
  }

  Future<void> iniciarSesion({required String correo, required String contrasena}) async {
    final auth = _auth;
    if (auth == null) {
      throw StateError('AuthRepository no configurado');
    }
    final sesion = await auth.iniciarSesion(correo: correo, contrasena: contrasena);
    await _almacen.guardar(sesion);
    _aviso = null;
    _activar(sesion);
    notifyListeners();
  }

  Future<void> cerrarSesion({String? aviso}) async {
    _temporizador?.cancel();
    _temporizador = null;
    _sesion = null;
    _aviso = aviso;
    _estado = EstadoSesion.sinSesion;
    await _almacen.borrar();
    notifyListeners();
  }

  void sesionExpirada() {
    if (_estado == EstadoSesion.autenticado) {
      cerrarSesion(aviso: 'Tu sesión expiró. Inicia sesión de nuevo.');
    }
  }

  void _activar(Sesion sesion) {
    _sesion = sesion;
    _estado = EstadoSesion.autenticado;
    _temporizador?.cancel();
    final restante = sesion.expiraEn.difference(_reloj());
    _temporizador = Timer(restante.isNegative ? Duration.zero : restante, sesionExpirada);
  }

  @override
  void dispose() {
    _temporizador?.cancel();
    super.dispose();
  }
}
