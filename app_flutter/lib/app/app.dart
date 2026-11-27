import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../core/api/api_client.dart';
import '../core/session/session_controller.dart';
import '../data/repositories/auth_repository.dart';
import '../data/repositories/catalogo_repository.dart';
import '../data/repositories/cuenta_repository.dart';
import '../data/repositories/pago_repository.dart';
import 'router.dart';
import 'tema.dart';

class GestoPagoApp extends StatefulWidget {
  const GestoPagoApp({super.key, required this.sesion, required this.api});

  final SessionController sesion;
  final ApiClient api;

  @override
  State<GestoPagoApp> createState() => _GestoPagoAppState();
}

class _GestoPagoAppState extends State<GestoPagoApp> {
  late final GoRouter _router = crearRouter(widget.sesion);

  @override
  void dispose() {
    _router.dispose();
    widget.api.cerrar();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final api = widget.api;
    return MultiProvider(
      providers: [
        ChangeNotifierProvider<SessionController>.value(value: widget.sesion),
        Provider<AuthRepository>(create: (_) => AuthRepository(api)),
        Provider<CatalogoRepository>(create: (_) => CatalogoRepository(api)),
        Provider<PagoRepository>(create: (_) => PagoRepository(api)),
        Provider<CuentaRepository>(create: (_) => CuentaRepository(api)),
      ],
      child: MaterialApp.router(
        title: 'GestoPago',
        debugShowCheckedModeBanner: false,
        theme: Tema.claro(),
        darkTheme: Tema.oscuro(),
        routerConfig: _router,
        locale: const Locale('es', 'MX'),
        supportedLocales: const [Locale('es', 'MX'), Locale('es')],
        localizationsDelegates: const [
          GlobalMaterialLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate,
          GlobalCupertinoLocalizations.delegate,
        ],
      ),
    );
  }
}
