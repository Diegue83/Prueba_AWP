import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../data/models/pago.dart';
import '../../data/repositories/pago_repository.dart';
import '../../shared/widgets/comprobante.dart';
import '../../shared/widgets/estados.dart';

/// Detalle de una transacción. Siempre consulta `GET /pagos/{id}` para mostrar el estado vigente.
class DetallePagoPage extends StatefulWidget {
  const DetallePagoPage({super.key, required this.idPago, this.pagoInicial});

  final int idPago;
  final Pago? pagoInicial;

  @override
  State<DetallePagoPage> createState() => _DetallePagoPageState();
}

class _DetallePagoPageState extends State<DetallePagoPage> {
  late Future<Pago> _pago = _consultar();

  Future<Pago> _consultar() => context.read<PagoRepository>().consultarPago(widget.idPago);

  void _recargar() => setState(() => _pago = _consultar());

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Pago #${widget.idPago}'),
        actions: [IconButton(tooltip: 'Actualizar', onPressed: _recargar, icon: const Icon(Icons.refresh))],
      ),
      body: FutureBuilder<Pago>(
        future: _pago,
        initialData: widget.pagoInicial,
        builder: (context, foto) {
          final pago = foto.data;
          if (foto.hasError && pago == null) {
            return VistaError(error: foto.error!, alReintentar: _recargar);
          }
          if (pago == null) {
            return const VistaCargando();
          }
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              if (foto.connectionState != ConnectionState.done) const LinearProgressIndicator(),
              const SizedBox(height: 8),
              ContenidoCentrado(child: Comprobante(pago: pago)),
              const SizedBox(height: 16),
              ContenidoCentrado(
                child: Text(pago.mensaje, textAlign: TextAlign.center, style: Theme.of(context).textTheme.bodyMedium),
              ),
            ],
          );
        },
      ),
    );
  }
}
