import 'package:flutter/material.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'app/app.dart';
import 'core/api/api_client.dart';
import 'core/session/session_controller.dart';
import 'core/session/session_store.dart';
import 'data/repositories/auth_repository.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting('es_MX');

  final sesion = SessionController(almacen: SecureSessionStore());
  final api = ApiClient(token: () => sesion.token, alExpirarSesion: sesion.sesionExpirada);
  sesion.auth = AuthRepository(api);
  await sesion.restaurar();

  runApp(GestoPagoApp(sesion: sesion, api: api));
}
