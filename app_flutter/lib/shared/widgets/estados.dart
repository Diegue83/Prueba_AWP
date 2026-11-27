import 'package:flutter/material.dart';

import '../../core/api/api_exception.dart';

class VistaCargando extends StatelessWidget {
  const VistaCargando({super.key, this.mensaje});

  final String? mensaje;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const CircularProgressIndicator(),
          if (mensaje != null) ...[
            const SizedBox(height: 16),
            Text(mensaje!, textAlign: TextAlign.center),
          ],
        ],
      ),
    );
  }
}

class VistaError extends StatelessWidget {
  const VistaError({super.key, required this.error, this.alReintentar});

  final Object error;
  final VoidCallback? alReintentar;

  @override
  Widget build(BuildContext context) {
    final mensaje = error is ApiException ? (error as ApiException).mensajeCompleto : 'Ocurrió un error inesperado.';
    return VistaMensaje(
      icono: Icons.cloud_off,
      titulo: 'No pudimos cargar la información',
      mensaje: mensaje,
      accion: alReintentar == null
          ? null
          : FilledButton.tonalIcon(
              onPressed: alReintentar,
              icon: const Icon(Icons.refresh),
              label: const Text('Reintentar'),
            ),
    );
  }
}

class VistaMensaje extends StatelessWidget {
  const VistaMensaje({super.key, required this.icono, required this.titulo, this.mensaje, this.accion});

  final IconData icono;
  final String titulo;
  final String? mensaje;
  final Widget? accion;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(32),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 420),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icono, size: 56, color: tema.colorScheme.outline),
              const SizedBox(height: 16),
              Text(titulo, style: tema.textTheme.titleMedium, textAlign: TextAlign.center),
              if (mensaje != null) ...[
                const SizedBox(height: 8),
                Text(mensaje!, style: tema.textTheme.bodyMedium, textAlign: TextAlign.center),
              ],
              if (accion != null) ...[const SizedBox(height: 20), accion!],
            ],
          ),
        ),
      ),
    );
  }
}

/// Limita el ancho del contenido en pantallas grandes (web, tabletas).
class ContenidoCentrado extends StatelessWidget {
  const ContenidoCentrado({super.key, required this.child, this.anchoMaximo = 720});

  final Widget child;
  final double anchoMaximo;

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.topCenter,
      child: ConstrainedBox(constraints: BoxConstraints(maxWidth: anchoMaximo), child: child),
    );
  }
}

class BotonConCarga extends StatelessWidget {
  const BotonConCarga({super.key, required this.texto, required this.cargando, required this.alPresionar, this.icono});

  final String texto;
  final bool cargando;
  final VoidCallback? alPresionar;
  final IconData? icono;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 48,
      child: FilledButton(
        onPressed: cargando ? null : alPresionar,
        child: cargando
            ? const SizedBox.square(dimension: 22, child: CircularProgressIndicator(strokeWidth: 2.5))
            : Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  if (icono != null) ...[Icon(icono, size: 20), const SizedBox(width: 8)],
                  Flexible(child: Text(texto, overflow: TextOverflow.ellipsis)),
                ],
              ),
      ),
    );
  }
}

void mostrarError(BuildContext context, Object error) {
  final mensaje = error is ApiException ? error.mensajeCompleto : 'Ocurrió un error inesperado.';
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(content: Text(mensaje), behavior: SnackBarBehavior.floating));
}
