import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../app/router.dart';
import '../../core/api/api_exception.dart';
import '../../core/config/app_config.dart';
import '../../core/format/formato.dart';
import '../../core/validation/validadores.dart';
import '../../data/models/producto.dart';
import '../../data/models/referencia.dart';
import '../../data/repositories/pago_repository.dart';
import '../../shared/widgets/estados.dart';
import '../../shared/widgets/logo_servicio.dart';
import 'flujo_pago.dart';

/// Paso 1 del pago: captura la referencia, la valida en formato y la verifica con el proveedor.
/// El botón para continuar solo se habilita con una referencia que el proveedor confirmó.
class ReferenciaPage extends StatefulWidget {
  const ReferenciaPage({super.key, required this.producto});

  final Producto producto;

  @override
  State<ReferenciaPage> createState() => _ReferenciaPageState();
}

class _ReferenciaPageState extends State<ReferenciaPage> {
  final _formulario = GlobalKey<FormState>();
  final _referencia = TextEditingController();
  final _confirmacion = TextEditingController();
  late final _monto = TextEditingController(
    text: widget.producto.tieneMontoFijo ? widget.producto.precio.toStringAsFixed(2) : '',
  );
  bool _verificando = false;
  ReferenciaVerificada? _verificacion;
  ApiException? _error;

  Producto get _producto => widget.producto;

  @override
  void initState() {
    super.initState();
    _referencia.addListener(_alCambiarReferencia);
  }

  @override
  void dispose() {
    _referencia.dispose();
    _confirmacion.dispose();
    _monto.dispose();
    super.dispose();
  }

  void _alCambiarReferencia() {
    final verificada = _verificacion;
    if (verificada != null && verificada.referencia != _normalizar(_referencia.text)) {
      setState(() => _verificacion = null);
    }
  }

  String _normalizar(String texto) => texto.trim().toUpperCase();

  Future<void> _verificar() async {
    FocusScope.of(context).unfocus();
    if (!_formulario.currentState!.validate()) {
      return;
    }
    setState(() {
      _verificando = true;
      _error = null;
      _verificacion = null;
    });
    try {
      final resultado = await context
          .read<PagoRepository>()
          .verificarReferencia(_producto, _normalizar(_referencia.text));
      setState(() {
        _verificacion = resultado;
        final saldo = resultado.saldo;
        if (resultado.esValida && !_producto.tieneMontoFijo && saldo != null && saldo > 0 && _monto.text.isEmpty) {
          _monto.text = saldo.toStringAsFixed(2);
        }
      });
    } on ApiException catch (e) {
      setState(() => _error = e);
    } finally {
      if (mounted) {
        setState(() => _verificando = false);
      }
    }
  }

  void _continuar() {
    if (!_formulario.currentState!.validate()) {
      return;
    }
    final verificacion = _verificacion;
    if (_producto.requiereReferencia && (verificacion == null || !verificacion.esValida)) {
      return;
    }
    final monto = double.parse(_monto.text.replaceAll(',', '').trim());
    context.push(Rutas.confirmar, extra: FlujoPago(producto: _producto, monto: monto, verificacion: verificacion));
  }

  @override
  Widget build(BuildContext context) {
    final puedeContinuar = !_producto.requiereReferencia || (_verificacion?.esValida ?? false);
    return Scaffold(
      appBar: AppBar(title: const Text('Datos del pago')),
      body: Form(
        key: _formulario,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            ContenidoCentrado(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  _ResumenProducto(producto: _producto),
                  const SizedBox(height: 16),
                  if (_producto.requiereReferencia) ..._camposReferencia() else _avisoCanjeable(),
                  const SizedBox(height: 16),
                  _campoMonto(),
                  if (_error != null) ...[
                    const SizedBox(height: 16),
                    _TarjetaResultado.error(_error!),
                  ],
                  if (_verificacion != null) ...[
                    const SizedBox(height: 16),
                    _TarjetaResultado.verificacion(_verificacion!),
                  ],
                  const SizedBox(height: 24),
                  if (_producto.requiereReferencia && !puedeContinuar)
                    BotonConCarga(
                      texto: 'Verificar referencia',
                      icono: Icons.fact_check_outlined,
                      cargando: _verificando,
                      alPresionar: _verificar,
                    )
                  else
                    FilledButton.icon(
                      onPressed: _continuar,
                      icon: const Icon(Icons.arrow_forward),
                      label: const Text('Revisar y confirmar'),
                    ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  List<Widget> _camposReferencia() {
    final esTelefono = _producto.pideTelefono;
    return [
      TextFormField(
        controller: _referencia,
        decoration: InputDecoration(
          labelText: esTelefono ? 'Número de teléfono' : 'Referencia del recibo',
          helperText: esTelefono ? '10 dígitos, sin espacios' : 'Tal como aparece en tu recibo',
          prefixIcon: Icon(esTelefono ? Icons.phone_iphone : Icons.qr_code_2),
          suffixIcon: esTelefono
              ? null
              : IconButton(
                  tooltip: '¿Dónde encuentro mi referencia?',
                  icon: const Icon(Icons.help_outline),
                  onPressed: () => _mostrarAyuda(context),
                ),
        ),
        keyboardType: esTelefono ? TextInputType.phone : TextInputType.text,
        textCapitalization: TextCapitalization.characters,
        maxLength: esTelefono ? 10 : 40,
        inputFormatters: [
          if (esTelefono) FilteringTextInputFormatter.digitsOnly,
          if (!esTelefono) FilteringTextInputFormatter.allow(RegExp('[A-Za-z0-9-]')),
        ],
        validator: esTelefono ? Validadores.telefono : Validadores.referencia,
      ),
      const SizedBox(height: 8),
      TextFormField(
        controller: _confirmacion,
        decoration: InputDecoration(
          labelText: esTelefono ? 'Confirma el número' : 'Confirma la referencia',
          prefixIcon: const Icon(Icons.done_all),
        ),
        keyboardType: esTelefono ? TextInputType.phone : TextInputType.text,
        textCapitalization: TextCapitalization.characters,
        maxLength: esTelefono ? 10 : 40,
        validator: (valor) =>
            _normalizar(valor ?? '') == _normalizar(_referencia.text) ? null : 'No coincide con el dato capturado',
      ),
    ];
  }

  Widget _avisoCanjeable() {
    return Card(
      child: ListTile(
        leading: const Icon(Icons.card_giftcard),
        title: const Text('Este artículo no requiere referencia'),
        subtitle: const Text('Recibirás el código o folio del proveedor al finalizar el pago.'),
      ),
    );
  }

  Widget _campoMonto() {
    if (_producto.tieneMontoFijo) {
      return Card(
        child: ListTile(
          leading: const Icon(Icons.payments_outlined),
          title: const Text('Importe'),
          trailing: Text(Formato.moneda(_producto.precio), style: Theme.of(context).textTheme.titleMedium),
        ),
      );
    }
    return TextFormField(
      controller: _monto,
      decoration: const InputDecoration(
        labelText: 'Importe a pagar',
        prefixText: r'$ ',
        helperText: 'El importe que indica tu recibo',
        prefixIcon: Icon(Icons.payments_outlined),
      ),
      keyboardType: const TextInputType.numberWithOptions(decimal: true),
      inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9.]'))],
      validator: Validadores.monto,
    );
  }

  void _mostrarAyuda(BuildContext context) {
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('¿Dónde está mi referencia?'),
        content: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 520, maxHeight: 520),
          child: Image.network(
            AppConfig.ayudaReferencia(_producto.idServicio),
            fit: BoxFit.contain,
            errorBuilder: (_, _, _) => const Text(
              'Este proveedor no publicó una imagen de ayuda. Busca el número de cuenta, contrato o '
              'referencia impreso en tu recibo.',
            ),
          ),
        ),
        actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Entendido'))],
      ),
    );
  }
}

class _ResumenProducto extends StatelessWidget {
  const _ResumenProducto({required this.producto});

  final Producto producto;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            LogoServicio(idServicio: producto.idServicio, nombre: producto.servicio, tamano: 56),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(producto.servicio, style: tema.textTheme.titleMedium),
                  Text(producto.producto, style: tema.textTheme.bodyMedium),
                  if (producto.comision > 0)
                    Text('Comisión: ${Formato.moneda(producto.comision)}',
                        style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _TarjetaResultado extends StatelessWidget {
  const _TarjetaResultado._({required this.icono, required this.titulo, required this.mensaje, required this.tipo, this.extra});

  factory _TarjetaResultado.verificacion(ReferenciaVerificada verificacion) {
    final saldo = verificacion.saldo;
    String? extra;
    if (saldo != null) {
      extra = saldo > 0
          ? 'Adeudo pendiente: ${Formato.moneda(saldo)}'
          : saldo == 0
              ? 'Sin adeudo pendiente'
              : 'Saldo a favor: ${Formato.moneda(-saldo)}';
    }
    return switch (verificacion.estado) {
      EstadoReferencia.valida => _TarjetaResultado._(
          icono: Icons.verified,
          titulo: 'Referencia válida',
          mensaje: verificacion.mensaje,
          tipo: _Tipo.exito,
          extra: extra,
        ),
      EstadoReferencia.invalida => _TarjetaResultado._(
          icono: Icons.cancel,
          titulo: 'Referencia no válida',
          mensaje: '${verificacion.mensaje}. Revisa el dato e intenta de nuevo.',
          tipo: _Tipo.error,
        ),
      EstadoReferencia.noVerificada => _TarjetaResultado._(
          icono: Icons.hourglass_empty,
          titulo: 'No se pudo verificar',
          mensaje: verificacion.mensaje,
          tipo: _Tipo.aviso,
        ),
    };
  }

  factory _TarjetaResultado.error(ApiException error) => _TarjetaResultado._(
        icono: Icons.error_outline,
        titulo: 'No se pudo verificar la referencia',
        mensaje: error.mensajeCompleto,
        tipo: _Tipo.error,
      );

  final IconData icono;
  final String titulo;
  final String mensaje;
  final String? extra;
  final _Tipo tipo;

  @override
  Widget build(BuildContext context) {
    final colores = Theme.of(context).colorScheme;
    final (fondo, texto) = switch (tipo) {
      _Tipo.exito => (const Color(0xFFDCF5E3), const Color(0xFF0B6B2B)),
      _Tipo.error => (colores.errorContainer, colores.onErrorContainer),
      _Tipo.aviso => (const Color(0xFFFFF1CC), const Color(0xFF7A5300)),
    };
    return Semantics(
      liveRegion: true,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(color: fondo, borderRadius: BorderRadius.circular(16)),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icono, color: texto),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(titulo, style: TextStyle(color: texto, fontWeight: FontWeight.w700)),
                  const SizedBox(height: 4),
                  Text(mensaje, style: TextStyle(color: texto)),
                  if (extra != null) ...[
                    const SizedBox(height: 4),
                    Text(extra!, style: TextStyle(color: texto, fontWeight: FontWeight.w600)),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

enum _Tipo { exito, error, aviso }
