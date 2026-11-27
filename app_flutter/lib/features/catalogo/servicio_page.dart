import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../app/router.dart';
import '../../core/format/formato.dart';
import '../../data/models/categorias.dart';
import '../../data/models/producto.dart';
import '../../data/repositories/catalogo_repository.dart';
import '../../shared/widgets/estados.dart';
import '../../shared/widgets/logo_servicio.dart';

/// Detalle de un proveedor: descripción, categoría y productos para elegir.
class ServicioPage extends StatefulWidget {
  const ServicioPage({super.key, required this.idServicio});

  final int idServicio;

  @override
  State<ServicioPage> createState() => _ServicioPageState();
}

class _ServicioPageState extends State<ServicioPage> {
  late Future<Servicio?> _servicio = _buscar();
  Producto? _elegido;

  Future<Servicio?> _buscar() async {
    final servicios = await context.read<CatalogoRepository>().consultarServicios();
    for (final servicio in servicios) {
      if (servicio.idServicio == widget.idServicio) {
        return servicio;
      }
    }
    return null;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Elige qué pagar')),
      body: FutureBuilder<Servicio?>(
        future: _servicio,
        builder: (context, foto) {
          if (foto.connectionState != ConnectionState.done) {
            return const VistaCargando();
          }
          if (foto.hasError) {
            return VistaError(error: foto.error!, alReintentar: () => setState(() => _servicio = _buscar()));
          }
          final servicio = foto.data;
          if (servicio == null) {
            return const VistaMensaje(icono: Icons.search_off, titulo: 'Este servicio ya no está en el catálogo');
          }
          final elegido = _elegido ?? (servicio.productos.length == 1 ? servicio.productos.first : null);
          return Column(
            children: [
              Expanded(
                child: ListView(
                  padding: const EdgeInsets.all(16),
                  children: [
                    ContenidoCentrado(child: _Encabezado(servicio: servicio)),
                    const SizedBox(height: 16),
                    ContenidoCentrado(
                      child: Card(
                        child: RadioGroup<int>(
                          groupValue: elegido?.idProducto,
                          onChanged: (id) => setState(
                            () => _elegido = servicio.productos.firstWhere((p) => p.idProducto == id),
                          ),
                          child: Column(
                            children: [
                              for (final producto in servicio.productos)
                                RadioListTile<int>(
                                  value: producto.idProducto,
                                  title: Text(producto.producto),
                                  subtitle: Text(_descripcionPrecio(producto)),
                                ),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              SafeArea(
                top: false,
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: ContenidoCentrado(
                    child: SizedBox(
                      width: double.infinity,
                      child: FilledButton.icon(
                        onPressed: elegido == null ? null : () => context.push(Rutas.referencia, extra: elegido),
                        icon: const Icon(Icons.arrow_forward),
                        label: const Text('Continuar'),
                      ),
                    ),
                  ),
                ),
              ),
            ],
          );
        },
      ),
    );
  }

  static String _descripcionPrecio(Producto producto) {
    if (producto.esRecarga) {
      return 'Importe: ${Formato.moneda(producto.precio)}';
    }
    return producto.precio > 0 ? 'Comisión: ${Formato.moneda(producto.precio)}' : 'Sin comisión';
  }
}

class _Encabezado extends StatelessWidget {
  const _Encabezado({required this.servicio});

  final Servicio servicio;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final categoria = Categoria.de(servicio.idCategoria);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                LogoServicio(idServicio: servicio.idServicio, nombre: servicio.nombre, tamano: 72),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(servicio.nombre, style: tema.textTheme.titleLarge),
                      const SizedBox(height: 6),
                      Chip(
                        avatar: Icon(categoria.icono, size: 16),
                        label: Text(categoria.nombre),
                        visualDensity: VisualDensity.compact,
                      ),
                    ],
                  ),
                ),
              ],
            ),
            if (servicio.descripcion != null) ...[
              const SizedBox(height: 12),
              Text(servicio.descripcion!, style: tema.textTheme.bodyMedium),
            ],
            const SizedBox(height: 8),
            Text(
              'Servicio #${servicio.idServicio}',
              style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }
}
