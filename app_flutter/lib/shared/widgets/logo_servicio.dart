import 'package:flutter/material.dart';

import '../../core/config/app_config.dart';

/// Logotipo del proveedor que publica PuntoRed (servido por el backend); si no existe,
/// muestra las iniciales del servicio.
class LogoServicio extends StatelessWidget {
  const LogoServicio({super.key, required this.idServicio, required this.nombre, this.tamano = 48});

  final int? idServicio;
  final String nombre;
  final double tamano;

  @override
  Widget build(BuildContext context) {
    final id = idServicio;
    final colores = Theme.of(context).colorScheme;
    final respaldo = _Iniciales(nombre: nombre, tamano: tamano);
    return Semantics(
      label: 'Logotipo de $nombre',
      image: true,
      child: Container(
        width: tamano,
        height: tamano,
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(tamano * 0.22),
          border: Border.all(color: colores.outlineVariant),
        ),
        clipBehavior: Clip.antiAlias,
        padding: EdgeInsets.all(tamano * 0.08),
        child: id == null
            ? respaldo
            : Image.network(
                AppConfig.logoServicio(id),
                fit: BoxFit.contain,
                excludeFromSemantics: true,
                errorBuilder: (_, _, _) => respaldo,
                loadingBuilder: (context, hijo, progreso) => progreso == null
                    ? hijo
                    : Center(
                        child: SizedBox.square(
                          dimension: tamano * 0.35,
                          child: const CircularProgressIndicator(strokeWidth: 2),
                        ),
                      ),
              ),
      ),
    );
  }
}

class _Iniciales extends StatelessWidget {
  const _Iniciales({required this.nombre, required this.tamano});

  final String nombre;
  final double tamano;

  @override
  Widget build(BuildContext context) {
    final palabras = nombre.trim().split(RegExp(r'\s+')).where((p) => p.isNotEmpty).toList();
    final iniciales = palabras.take(2).map((p) => p[0].toUpperCase()).join();
    final colores = Theme.of(context).colorScheme;
    return Container(
      alignment: Alignment.center,
      color: colores.primaryContainer,
      child: Text(
        iniciales.isEmpty ? '?' : iniciales,
        style: TextStyle(
          color: colores.onPrimaryContainer,
          fontWeight: FontWeight.w700,
          fontSize: tamano * 0.32,
        ),
      ),
    );
  }
}
