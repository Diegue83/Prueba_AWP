import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../core/session/session_controller.dart';

/// Navegación principal: barra inferior en teléfonos y riel lateral en pantallas anchas.
class HomeShell extends StatelessWidget {
  const HomeShell({super.key, required this.navegacion});

  final StatefulNavigationShell navegacion;

  static const _destinos = [
    (Icons.storefront_outlined, Icons.storefront, 'Servicios'),
    (Icons.receipt_long_outlined, Icons.receipt_long, 'Historial'),
    (Icons.person_outline, Icons.person, 'Mi cuenta'),
  ];

  void _ir(int indice) => navegacion.goBranch(indice, initialLocation: indice == navegacion.currentIndex);

  @override
  Widget build(BuildContext context) {
    final ancho = MediaQuery.sizeOf(context).width;
    if (ancho < 840) {
      return Scaffold(
        body: navegacion,
        bottomNavigationBar: NavigationBar(
          selectedIndex: navegacion.currentIndex,
          onDestinationSelected: _ir,
          destinations: [
            for (final (icono, seleccionado, texto) in _destinos)
              NavigationDestination(icon: Icon(icono), selectedIcon: Icon(seleccionado), label: texto),
          ],
        ),
      );
    }

    final sesion = context.watch<SessionController>().sesion;
    return Scaffold(
      body: Row(
        children: [
          NavigationRail(
            extended: ancho >= 1100,
            selectedIndex: navegacion.currentIndex,
            onDestinationSelected: _ir,
            leading: Padding(
              padding: const EdgeInsets.symmetric(vertical: 16),
              child: Column(
                children: [
                  CircleAvatar(
                    radius: 22,
                    child: Text(_inicial(sesion?.nombreCompleto)),
                  ),
                  if (ancho >= 1100) ...[
                    const SizedBox(height: 8),
                    SizedBox(
                      width: 200,
                      child: Text(
                        sesion?.nombreCompleto ?? '',
                        textAlign: TextAlign.center,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ],
                ],
              ),
            ),
            destinations: [
              for (final (icono, seleccionado, texto) in _destinos)
                NavigationRailDestination(
                  icon: Icon(icono),
                  selectedIcon: Icon(seleccionado),
                  label: Text(texto),
                ),
            ],
          ),
          const VerticalDivider(width: 1),
          Expanded(child: navegacion),
        ],
      ),
    );
  }

  static String _inicial(String? nombre) =>
      (nombre == null || nombre.isEmpty) ? '?' : nombre.trim()[0].toUpperCase();
}
