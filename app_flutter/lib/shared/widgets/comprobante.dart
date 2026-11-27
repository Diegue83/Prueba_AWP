import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../core/format/formato.dart';
import '../../data/models/pago.dart';
import 'estado_pago_chip.dart';
import 'logo_servicio.dart';

/// Datos de una operación tal como los devolvió el backend.
class Comprobante extends StatelessWidget {
  const Comprobante({super.key, required this.pago});

  final Pago pago;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final nombre = pago.servicio ?? 'Producto ${pago.idProducto}';
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                LogoServicio(idServicio: pago.idServicio, nombre: nombre, tamano: 52),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(nombre, style: tema.textTheme.titleMedium),
                      if (pago.producto != null) Text(pago.producto!, style: tema.textTheme.bodySmall),
                    ],
                  ),
                ),
                EstadoPagoChip(estatus: pago.estatus),
              ],
            ),
            const Divider(height: 28),
            _Dato('Folio', pago.numAutorizacion ?? 'Sin folio del proveedor', copiable: pago.numAutorizacion != null),
            _Dato('Número de pago', '${pago.idPago}', copiable: true),
            _Dato('Referencia', pago.referencia ?? '—'),
            _Dato('Fecha', Formato.fechaHora(pago.fecha)),
            _Dato('Importe', Formato.moneda(pago.monto)),
            _Dato('Comisión', Formato.moneda(pago.comision)),
            _Dato('Total', Formato.moneda(pago.total), destacado: true),
            if (pago.codigoProveedor != null) _Dato('Código del proveedor', pago.codigoProveedor!),
            if (pago.fechaActualizacion != null && pago.fechaActualizacion != pago.fechaOperacion)
              _Dato('Última actualización', Formato.fechaHora(pago.fechaActualizacion)),
          ],
        ),
      ),
    );
  }
}

class _Dato extends StatelessWidget {
  const _Dato(this.etiqueta, this.valor, {this.destacado = false, this.copiable = false});

  final String etiqueta;
  final String valor;
  final bool destacado;
  final bool copiable;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          Expanded(
            child: Text(etiqueta, style: tema.textTheme.bodyMedium?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
          ),
          Flexible(
            child: SelectableText(
              valor,
              textAlign: TextAlign.end,
              style: destacado ? tema.textTheme.titleMedium : tema.textTheme.bodyMedium,
            ),
          ),
          if (copiable)
            IconButton(
              visualDensity: VisualDensity.compact,
              tooltip: 'Copiar $etiqueta',
              icon: const Icon(Icons.copy, size: 18),
              onPressed: () {
                Clipboard.setData(ClipboardData(text: valor));
                ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$etiqueta copiado')));
              },
            ),
        ],
      ),
    );
  }
}
