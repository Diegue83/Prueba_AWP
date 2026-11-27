import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../core/session/session_controller.dart';
import '../data/models/pago.dart';
import '../data/models/producto.dart';
import '../features/auth/login_page.dart';
import '../features/auth/registro_page.dart';
import '../features/catalogo/catalogo_page.dart';
import '../features/catalogo/servicio_page.dart';
import '../features/cuenta/cuenta_page.dart';
import '../features/cuenta/segundo_factor_page.dart';
import '../features/historial/detalle_pago_page.dart';
import '../features/historial/historial_page.dart';
import '../features/home/home_shell.dart';
import '../features/pago/confirmacion_page.dart';
import '../features/pago/flujo_pago.dart';
import '../features/pago/referencia_page.dart';
import '../features/pago/resultado_page.dart';

class Rutas {
  const Rutas._();

  static const login = '/login';
  static const registro = '/registro';
  static const catalogo = '/catalogo';
  static const historial = '/historial';
  static const cuenta = '/cuenta';
  static const segundoFactor = '/cuenta/segundo-factor';
  static const referencia = '/pago/referencia';
  static const confirmar = '/pago/confirmar';
  static const resultado = '/pago/resultado';

  static String servicio(int idServicio) => '$catalogo/servicio/$idServicio';

  static String detallePago(int idPago) => '$historial/$idPago';
}

const _rutasPublicas = {Rutas.login, Rutas.registro};

GoRouter crearRouter(SessionController sesion) {
  return GoRouter(
    initialLocation: Rutas.catalogo,
    refreshListenable: sesion,
    redirect: (context, estado) {
      if (sesion.estado == EstadoSesion.cargando) {
        return null;
      }
      final publica = _rutasPublicas.contains(estado.matchedLocation);
      if (!sesion.autenticado) {
        return publica ? null : Rutas.login;
      }
      return publica ? Rutas.catalogo : null;
    },
    routes: [
      GoRoute(path: Rutas.login, builder: (_, _) => const LoginPage()),
      GoRoute(
        path: Rutas.registro,
        builder: (_, _) => const RegistroPage(),
      ),
      StatefulShellRoute.indexedStack(
        builder: (context, estado, navegacion) => HomeShell(navegacion: navegacion),
        branches: [
          StatefulShellBranch(routes: [
            GoRoute(
              path: Rutas.catalogo,
              builder: (_, _) => const CatalogoPage(),
              routes: [
                GoRoute(
                  path: 'servicio/:idServicio',
                  builder: (_, estado) => ServicioPage(
                    idServicio: int.tryParse(estado.pathParameters['idServicio'] ?? '') ?? 0,
                  ),
                ),
              ],
            ),
          ]),
          StatefulShellBranch(routes: [
            GoRoute(
              path: Rutas.historial,
              builder: (_, _) => const HistorialPage(),
              routes: [
                GoRoute(
                  path: ':idPago',
                  builder: (_, estado) => DetallePagoPage(
                    idPago: int.tryParse(estado.pathParameters['idPago'] ?? '') ?? 0,
                    pagoInicial: estado.extra is Pago ? estado.extra as Pago : null,
                  ),
                ),
              ],
            ),
          ]),
          StatefulShellBranch(routes: [
            GoRoute(
              path: Rutas.cuenta,
              builder: (_, _) => const CuentaPage(),
              routes: [
                GoRoute(path: 'segundo-factor', builder: (_, _) => const SegundoFactorPage()),
              ],
            ),
          ]),
        ],
      ),
      GoRoute(
        path: Rutas.referencia,
        redirect: (_, estado) => estado.extra is Producto ? null : Rutas.catalogo,
        builder: (_, estado) => ReferenciaPage(producto: estado.extra! as Producto),
      ),
      GoRoute(
        path: Rutas.confirmar,
        redirect: (_, estado) => estado.extra is FlujoPago ? null : Rutas.catalogo,
        builder: (_, estado) => ConfirmacionPage(flujo: estado.extra! as FlujoPago),
      ),
      GoRoute(
        path: Rutas.resultado,
        redirect: (_, estado) => estado.extra is Pago ? null : Rutas.historial,
        builder: (_, estado) => ResultadoPage(pago: estado.extra! as Pago),
      ),
    ],
    errorBuilder: (context, estado) => Scaffold(
      appBar: AppBar(),
      body: Center(child: Text('No encontramos la página ${estado.uri.path}')),
    ),
  );
}
