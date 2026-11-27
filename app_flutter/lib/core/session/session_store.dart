import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../../data/models/sesion.dart';

/// Guarda la sesión en el almacenamiento seguro del dispositivo (Keychain, Keystore o, en web,
/// cifrada con WebCrypto). Nunca se guarda la contraseña.
abstract class SessionStore {
  Future<Sesion?> leer();

  Future<void> guardar(Sesion sesion);

  Future<void> borrar();
}

class SecureSessionStore implements SessionStore {
  SecureSessionStore([FlutterSecureStorage? almacenamiento])
      : _almacenamiento = almacenamiento ?? const FlutterSecureStorage();

  static const _clave = 'gestopago.sesion';

  final FlutterSecureStorage _almacenamiento;

  @override
  Future<Sesion?> leer() async {
    try {
      final valor = await _almacenamiento.read(key: _clave);
      if (valor == null) {
        return null;
      }
      return Sesion.desdeJson((jsonDecode(valor) as Map).cast<String, dynamic>());
    } on Exception {
      await borrar();
      return null;
    }
  }

  @override
  Future<void> guardar(Sesion sesion) => _almacenamiento.write(key: _clave, value: jsonEncode(sesion.aJson()));

  @override
  Future<void> borrar() async {
    try {
      await _almacenamiento.delete(key: _clave);
    } on Exception {
      return;
    }
  }
}
