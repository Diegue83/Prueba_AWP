import 'package:flutter/material.dart';

import '../../data/models/pago.dart';

class EstadoPagoChip extends StatelessWidget {
  const EstadoPagoChip({super.key, required this.estatus});

  final EstatusPago estatus;

  static (Color fondo, Color texto, IconData icono) estilo(EstatusPago estatus, ColorScheme colores) {
    if (estatus.esExitoso) {
      return (const Color(0xFFDCF5E3), const Color(0xFF0B6B2B), Icons.check_circle);
    }
    if (estatus.esRechazado) {
      return (colores.errorContainer, colores.onErrorContainer, Icons.cancel);
    }
    return (const Color(0xFFFFF1CC), const Color(0xFF7A5300), Icons.schedule);
  }

  @override
  Widget build(BuildContext context) {
    final (fondo, texto, icono) = estilo(estatus, Theme.of(context).colorScheme);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(color: fondo, borderRadius: BorderRadius.circular(999)),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icono, size: 16, color: texto),
          const SizedBox(width: 4),
          Text(estatus.etiqueta, style: TextStyle(color: texto, fontWeight: FontWeight.w600, fontSize: 12)),
        ],
      ),
    );
  }
}
