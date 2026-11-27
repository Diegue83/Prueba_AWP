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

class CatalogoPage extends StatefulWidget {
  const CatalogoPage({super.key});

  @override
  State<CatalogoPage> createState() => _CatalogoPageState();
}

class _CatalogoPageState extends State<CatalogoPage> {
  late Future<List<Servicio>> _servicios = _cargar();
  String _busqueda = '';
  int? _categoria;

  Future<List<Servicio>> _cargar({bool forzar = false}) =>
      context.read<CatalogoRepository>().consultarServicios(forzar: forzar);

  Future<void> _recargar() async {
    final futuro = _cargar(forzar: true);
    setState(() => _servicios = futuro);
    await futuro.catchError((_) => <Servicio>[]);
  }

  List<Servicio> _filtrar(List<Servicio> servicios) {
    final texto = _busqueda.trim().toLowerCase();
    return servicios.where((s) {
      final coincideCategoria = _categoria == null || s.idCategoria == _categoria;
      final coincideTexto = texto.isEmpty ||
          s.nombre.toLowerCase().contains(texto) ||
          Categoria.de(s.idCategoria).nombre.toLowerCase().contains(texto) ||
          s.productos.any((p) => p.producto.toLowerCase().contains(texto));
      return coincideCategoria && coincideTexto;
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Servicios y recargas')),
      body: FutureBuilder<List<Servicio>>(
        future: _servicios,
        builder: (context, foto) {
          if (foto.connectionState != ConnectionState.done) {
            return const VistaCargando(mensaje: 'Cargando catálogo de GestoPago…');
          }
          if (foto.hasError) {
            return VistaError(error: foto.error!, alReintentar: _recargar);
          }
          final servicios = foto.data!;
          if (servicios.isEmpty) {
            return VistaMensaje(
              icono: Icons.inventory_2_outlined,
              titulo: 'El catálogo está vacío',
              mensaje: 'GestoPago aún no ha publicado productos para este distribuidor.',
              accion: FilledButton.tonal(onPressed: _recargar, child: const Text('Reintentar')),
            );
          }
          final categorias = {for (final s in servicios) s.idCategoria}.toList()..sort();
          final visibles = _filtrar(servicios);
          return RefreshIndicator(
            onRefresh: _recargar,
            child: CustomScrollView(
              physics: const AlwaysScrollableScrollPhysics(),
              slivers: [
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(16, 4, 16, 8),
                    child: SearchBar(
                      hintText: 'Buscar proveedor o producto',
                      leading: const Icon(Icons.search),
                      elevation: const WidgetStatePropertyAll(0),
                      onChanged: (valor) => setState(() => _busqueda = valor),
                    ),
                  ),
                ),
                SliverToBoxAdapter(
                  child: SizedBox(
                    height: 48,
                    child: ListView(
                      scrollDirection: Axis.horizontal,
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      children: [
                        _Filtro(
                          etiqueta: 'Todos',
                          icono: Icons.apps,
                          seleccionado: _categoria == null,
                          alSeleccionar: () => setState(() => _categoria = null),
                        ),
                        for (final id in categorias)
                          _Filtro(
                            etiqueta: Categoria.de(id).nombre,
                            icono: Categoria.de(id).icono,
                            seleccionado: _categoria == id,
                            alSeleccionar: () => setState(() => _categoria = _categoria == id ? null : id),
                          ),
                      ],
                    ),
                  ),
                ),
                if (visibles.isEmpty)
                  const SliverFillRemaining(
                    hasScrollBody: false,
                    child: VistaMensaje(icono: Icons.search_off, titulo: 'Sin resultados para tu búsqueda'),
                  )
                else
                  SliverPadding(
                    padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
                    sliver: SliverGrid(
                      gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
                        maxCrossAxisExtent: 360,
                        mainAxisExtent: 112,
                        crossAxisSpacing: 12,
                        mainAxisSpacing: 12,
                      ),
                      delegate: SliverChildBuilderDelegate(
                        (context, i) => _TarjetaServicio(servicio: visibles[i]),
                        childCount: visibles.length,
                      ),
                    ),
                  ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _Filtro extends StatelessWidget {
  const _Filtro({required this.etiqueta, required this.icono, required this.seleccionado, required this.alSeleccionar});

  final String etiqueta;
  final IconData icono;
  final bool seleccionado;
  final VoidCallback alSeleccionar;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: FilterChip(
        avatar: Icon(icono, size: 18),
        label: Text(etiqueta),
        selected: seleccionado,
        showCheckmark: false,
        onSelected: (_) => alSeleccionar(),
      ),
    );
  }
}

class _TarjetaServicio extends StatelessWidget {
  const _TarjetaServicio({required this.servicio});

  final Servicio servicio;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final categoria = Categoria.de(servicio.idCategoria);
    final primero = servicio.productos.first;
    final detalle = primero.esRecarga
        ? 'Desde ${Formato.moneda(primero.precio)}'
        : '${servicio.productos.length} ${servicio.productos.length == 1 ? 'opción' : 'opciones'} de pago';
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => context.go(Rutas.servicio(servicio.idServicio)),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            children: [
              LogoServicio(idServicio: servicio.idServicio, nombre: servicio.nombre, tamano: 64),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(servicio.nombre, style: tema.textTheme.titleSmall, maxLines: 2, overflow: TextOverflow.ellipsis),
                    const SizedBox(height: 4),
                    Row(
                      children: [
                        Icon(categoria.icono, size: 14, color: tema.colorScheme.primary),
                        const SizedBox(width: 4),
                        Flexible(
                          child: Text(
                            categoria.nombre,
                            style: tema.textTheme.labelMedium?.copyWith(color: tema.colorScheme.primary),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 2),
                    Text(detalle, style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right),
            ],
          ),
        ),
      ),
    );
  }
}
