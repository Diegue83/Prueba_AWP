import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../app/router.dart';
import '../../core/api/api_exception.dart';
import '../../core/format/formato.dart';
import '../../data/models/pago.dart';
import '../../data/repositories/pago_repository.dart';
import '../../shared/widgets/estado_pago_chip.dart';
import '../../shared/widgets/estados.dart';
import '../../shared/widgets/logo_servicio.dart';

/// Pagos del cliente autenticado (`GET /pagos`), del más reciente al más antiguo.
class HistorialPage extends StatefulWidget {
  const HistorialPage({super.key});

  @override
  State<HistorialPage> createState() => _HistorialPageState();
}

class _HistorialPageState extends State<HistorialPage> {
  static const _tamano = 20;

  final List<Pago> _pagos = [];
  int _pagina = 0;
  bool _cargando = true;
  bool _hayMas = true;
  ApiException? _error;

  @override
  void initState() {
    super.initState();
    _recargar();
  }

  Future<void> _recargar() async {
    setState(() {
      _cargando = true;
      _error = null;
    });
    try {
      final primeros = await context.read<PagoRepository>().consultarHistorial(pagina: 0, tamano: _tamano);
      setState(() {
        _pagos
          ..clear()
          ..addAll(primeros);
        _pagina = 0;
        _hayMas = primeros.length == _tamano;
      });
    } on ApiException catch (e) {
      setState(() => _error = e);
    } finally {
      if (mounted) {
        setState(() => _cargando = false);
      }
    }
  }

  Future<void> _cargarMas() async {
    setState(() => _cargando = true);
    try {
      final siguientes =
          await context.read<PagoRepository>().consultarHistorial(pagina: _pagina + 1, tamano: _tamano);
      setState(() {
        _pagos.addAll(siguientes);
        _pagina++;
        _hayMas = siguientes.length == _tamano;
      });
    } on ApiException catch (e) {
      if (mounted) {
        mostrarError(context, e);
      }
    } finally {
      if (mounted) {
        setState(() => _cargando = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Historial de pagos'),
        actions: [
          IconButton(tooltip: 'Actualizar', onPressed: _cargando ? null : _recargar, icon: const Icon(Icons.refresh)),
        ],
      ),
      body: _contenido(),
    );
  }

  Widget _contenido() {
    if (_cargando && _pagos.isEmpty) {
      return const VistaCargando(mensaje: 'Consultando tus pagos…');
    }
    if (_error != null && _pagos.isEmpty) {
      return VistaError(error: _error!, alReintentar: _recargar);
    }
    if (_pagos.isEmpty) {
      return VistaMensaje(
        icono: Icons.receipt_long_outlined,
        titulo: 'Aún no tienes pagos',
        mensaje: 'Cuando pagues un servicio o una recarga aparecerá aquí.',
        accion: FilledButton(onPressed: () => context.go(Rutas.catalogo), child: const Text('Ver servicios')),
      );
    }
    return RefreshIndicator(
      onRefresh: _recargar,
      child: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: _pagos.length + (_hayMas ? 1 : 0),
        separatorBuilder: (_, _) => const SizedBox(height: 8),
        itemBuilder: (context, i) {
          if (i == _pagos.length) {
            return ContenidoCentrado(
              child: Center(
                child: _cargando
                    ? const Padding(padding: EdgeInsets.all(12), child: CircularProgressIndicator())
                    : TextButton(onPressed: _cargarMas, child: const Text('Cargar más')),
              ),
            );
          }
          return ContenidoCentrado(child: _FilaPago(pago: _pagos[i]));
        },
      ),
    );
  }
}

class _FilaPago extends StatelessWidget {
  const _FilaPago({required this.pago});

  final Pago pago;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final nombre = pago.servicio ?? 'Producto ${pago.idProducto}';
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => context.go(Rutas.detallePago(pago.idPago), extra: pago),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            children: [
              LogoServicio(idServicio: pago.idServicio, nombre: nombre, tamano: 48),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(nombre, style: tema.textTheme.titleSmall, overflow: TextOverflow.ellipsis),
                    const SizedBox(height: 2),
                    Text(
                      'Ref. ${pago.referencia ?? '—'}',
                      style: tema.textTheme.bodySmall,
                      overflow: TextOverflow.ellipsis,
                    ),
                    Text(
                      '${Formato.fechaHora(pago.fecha)} · Folio ${pago.folio}',
                      style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(Formato.moneda(pago.total ?? pago.monto), style: tema.textTheme.titleSmall),
                  const SizedBox(height: 6),
                  EstadoPagoChip(estatus: pago.estatus),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
