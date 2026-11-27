import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../app/router.dart';
import '../../core/api/api_exception.dart';
import '../../data/models/pago.dart';
import '../../data/repositories/pago_repository.dart';
import '../../shared/widgets/comprobante.dart';
import '../../shared/widgets/estados.dart';

/// Paso 3: muestra lo que respondió el servicio. Solo APLICADO se presenta como pago exitoso.
class ResultadoPage extends StatefulWidget {
  const ResultadoPage({super.key, required this.pago});

  final Pago pago;

  @override
  State<ResultadoPage> createState() => _ResultadoPageState();
}

class _ResultadoPageState extends State<ResultadoPage> {
  late Pago _pago = widget.pago;
  bool _actualizando = false;

  Future<void> _actualizar() async {
    setState(() => _actualizando = true);
    try {
      final actual = await context.read<PagoRepository>().consultarPago(_pago.idPago);
      setState(() => _pago = actual.conNombres(
            servicio: _pago.servicio,
            producto: _pago.producto,
            idServicio: _pago.idServicio,
            referencia: _pago.referencia,
          ));
    } on ApiException catch (e) {
      if (mounted) {
        mostrarError(context, e);
      }
    } finally {
      if (mounted) {
        setState(() => _actualizando = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final estatus = _pago.estatus;
    final (icono, color, titulo) = estatus.esExitoso
        ? (Icons.check_circle, const Color(0xFF0B6B2B), 'Pago exitoso')
        : estatus.esRechazado
            ? (Icons.cancel, tema.colorScheme.error, 'Pago rechazado')
            : (Icons.schedule, const Color(0xFF7A5300), 'Pago sin confirmar');
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (salio, _) {
        if (!salio) {
          context.go(Rutas.catalogo);
        }
      },
      child: Scaffold(
        appBar: AppBar(title: const Text('Resultado'), automaticallyImplyLeading: false),
        body: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            ContenidoCentrado(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const SizedBox(height: 8),
                  Icon(icono, size: 72, color: color),
                  const SizedBox(height: 12),
                  Text(titulo, textAlign: TextAlign.center, style: tema.textTheme.headlineSmall?.copyWith(color: color)),
                  const SizedBox(height: 8),
                  Text(_pago.mensaje, textAlign: TextAlign.center, style: tema.textTheme.bodyLarge),
                  if (estatus.estaSinConfirmar) ...[
                    const SizedBox(height: 8),
                    Text(
                      'No lo des por pagado todavía: el proveedor aún no confirma la operación. '
                      'Puedes consultar el estado aquí o en tu historial.',
                      textAlign: TextAlign.center,
                      style: tema.textTheme.bodyMedium?.copyWith(color: tema.colorScheme.onSurfaceVariant),
                    ),
                  ],
                  const SizedBox(height: 24),
                  Comprobante(pago: _pago),
                  const SizedBox(height: 24),
                  if (estatus.estaSinConfirmar) ...[
                    BotonConCarga(
                      texto: 'Consultar estado',
                      icono: Icons.refresh,
                      cargando: _actualizando,
                      alPresionar: _actualizar,
                    ),
                    const SizedBox(height: 12),
                  ],
                  OutlinedButton.icon(
                    onPressed: () => context.go(Rutas.historial),
                    icon: const Icon(Icons.receipt_long),
                    label: const Text('Ver historial'),
                  ),
                  const SizedBox(height: 12),
                  TextButton(
                    onPressed: () => context.go(Rutas.catalogo),
                    child: const Text('Volver a servicios'),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
