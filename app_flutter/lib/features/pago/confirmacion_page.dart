import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../app/router.dart';
import '../../core/api/api_exception.dart';
import '../../core/format/formato.dart';
import '../../core/session/session_controller.dart';
import '../../core/validation/validadores.dart';
import '../../data/models/cuenta.dart';
import '../../data/repositories/cuenta_repository.dart';
import '../../data/repositories/pago_repository.dart';
import '../../shared/widgets/estados.dart';
import '../../shared/widgets/logo_servicio.dart';
import 'flujo_pago.dart';

/// Paso 2: resumen, código de segundo factor y confirmación explícita antes de cobrar.
class ConfirmacionPage extends StatefulWidget {
  const ConfirmacionPage({super.key, required this.flujo});

  final FlujoPago flujo;

  @override
  State<ConfirmacionPage> createState() => _ConfirmacionPageState();
}

class _ConfirmacionPageState extends State<ConfirmacionPage> {
  final _formulario = GlobalKey<FormState>();
  final _codigo = TextEditingController();
  late final String _claveIdempotencia = _generarClave();
  late final Future<Saldo> _saldo = _consultarSaldo();
  bool _aceptado = false;
  bool _pagando = false;
  String? _errorCodigo;
  ApiException? _error;

  FlujoPago get _flujo => widget.flujo;

  static String _generarClave() {
    final aleatorio = Random.secure();
    return List.generate(32, (_) => aleatorio.nextInt(16).toRadixString(16)).join();
  }

  Future<Saldo> _consultarSaldo() {
    final cuenta = context.read<SessionController>().sesion!.numeroCuenta;
    return context.read<CuentaRepository>().consultarSaldo(cuenta);
  }

  @override
  void dispose() {
    _codigo.dispose();
    super.dispose();
  }

  Future<void> _pagar() async {
    setState(() {
      _errorCodigo = null;
      _error = null;
    });
    if (!_formulario.currentState!.validate() || !_aceptado) {
      return;
    }
    final confirmado = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        icon: const Icon(Icons.payments_outlined),
        title: const Text('¿Confirmas el pago?'),
        content: Text(
          'Se cargarán ${Formato.moneda(_flujo.total)} a tu cuenta para pagar '
          '${_flujo.producto.servicio}${_flujo.referencia == null ? '' : ' (${_flujo.referencia})'}.',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Pagar')),
        ],
      ),
    );
    if (confirmado != true || !mounted) {
      return;
    }

    final sesion = context.read<SessionController>().sesion!;
    final solicitud = SolicitudPago(
      numeroCuenta: sesion.numeroCuenta,
      producto: _flujo.producto,
      monto: _flujo.monto,
      referencia: _flujo.referencia,
      codigo2fa: _codigo.text.trim(),
    );
    setState(() => _pagando = true);
    try {
      final pago = await context.read<PagoRepository>().pagar(solicitud, _claveIdempotencia);
      if (mounted) {
        context.pushReplacement(Rutas.resultado, extra: pago);
      }
    } on ApiException catch (e) {
      if (!mounted) {
        return;
      }
      switch (e.clave) {
        case 'CODIGO_2FA_INVALIDO':
          setState(() => _errorCodigo = 'El código no es correcto o ya se usó. Espera el siguiente.');
        case '2FA_NO_CONFIGURADO':
          await _pedirConfigurarSegundoFactor();
        default:
          setState(() => _error = e);
      }
    } finally {
      if (mounted) {
        setState(() => _pagando = false);
      }
    }
  }

  Future<void> _pedirConfigurarSegundoFactor() async {
    final ir = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        icon: const Icon(Icons.security),
        title: const Text('Activa tu código de seguridad'),
        content: const Text(
          'Para pagar necesitas una app autenticadora (Google Authenticator, Microsoft Authenticator…). '
          'Actívala una sola vez desde Mi cuenta.',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Ahora no')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Activar')),
        ],
      ),
    );
    if (ir == true && mounted) {
      context.go(Rutas.segundoFactor);
    }
  }

  @override
  Widget build(BuildContext context) {
    final sesion = context.watch<SessionController>().sesion;
    return PopScope(
      canPop: !_pagando,
      child: Scaffold(
        appBar: AppBar(title: const Text('Confirma tu pago')),
        body: Stack(
          children: [
            Form(
              key: _formulario,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  ContenidoCentrado(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        _Resumen(flujo: _flujo, numeroCuenta: sesion?.numeroCuenta, saldo: _saldo),
                        const SizedBox(height: 16),
                        TextFormField(
                          controller: _codigo,
                          enabled: !_pagando,
                          decoration: InputDecoration(
                            labelText: 'Código de seguridad',
                            helperText: 'Los 6 dígitos de tu app autenticadora',
                            prefixIcon: const Icon(Icons.shield_outlined),
                            errorText: _errorCodigo,
                            counterText: '',
                          ),
                          keyboardType: TextInputType.number,
                          maxLength: 6,
                          autofillHints: const [AutofillHints.oneTimeCode],
                          inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                          validator: Validadores.codigo2fa,
                        ),
                        const SizedBox(height: 8),
                        CheckboxListTile(
                          value: _aceptado,
                          onChanged: _pagando ? null : (v) => setState(() => _aceptado = v ?? false),
                          controlAffinity: ListTileControlAffinity.leading,
                          contentPadding: EdgeInsets.zero,
                          title: const Text('Revisé los datos y autorizo el cargo a mi cuenta'),
                        ),
                        if (_error != null) ...[
                          const SizedBox(height: 8),
                          _ErrorPago(error: _error!),
                        ],
                        const SizedBox(height: 16),
                        BotonConCarga(
                          texto: _error?.sinRespuestaDelServidor == true
                              ? 'Reintentar la misma operación'
                              : 'Pagar ${Formato.moneda(_flujo.total)}',
                          icono: Icons.lock_outline,
                          cargando: _pagando,
                          alPresionar: _aceptado ? _pagar : null,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            if (_pagando)
              const ModalBarrier(dismissible: false, color: Color(0x66000000)),
            if (_pagando)
              Center(
                child: Card(
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const CircularProgressIndicator(),
                        const SizedBox(height: 16),
                        Text('Procesando tu pago…', style: Theme.of(context).textTheme.titleMedium),
                        const SizedBox(height: 4),
                        const Text('Puede tardar hasta un minuto. No cierres la app.'),
                      ],
                    ),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _Resumen extends StatelessWidget {
  const _Resumen({required this.flujo, required this.numeroCuenta, required this.saldo});

  final FlujoPago flujo;
  final String? numeroCuenta;
  final Future<Saldo> saldo;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final producto = flujo.producto;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                LogoServicio(idServicio: producto.idServicio, nombre: producto.servicio, tamano: 56),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(producto.servicio, style: tema.textTheme.titleMedium),
                      Text(producto.producto, style: tema.textTheme.bodyMedium),
                    ],
                  ),
                ),
              ],
            ),
            const Divider(height: 32),
            if (flujo.referencia != null)
              _Fila(producto.pideTelefono ? 'Teléfono' : 'Referencia', flujo.referencia!, destacado: true),
            if (flujo.verificacion?.mensaje.isNotEmpty ?? false)
              _Fila('Verificación', flujo.verificacion!.mensaje),
            _Fila('Importe', Formato.moneda(flujo.monto)),
            _Fila('Comisión', Formato.moneda(flujo.comision)),
            const Divider(height: 24),
            _Fila('Total a cargar', Formato.moneda(flujo.total), destacado: true),
            _Fila('Cuenta de cargo', Formato.enmascarar(numeroCuenta)),
            FutureBuilder<Saldo>(
              future: saldo,
              builder: (context, foto) {
                final valor = foto.data?.saldo;
                final insuficiente = valor != null && valor < flujo.total;
                return _Fila(
                  'Saldo disponible',
                  foto.hasError ? 'No disponible' : (valor == null ? 'Consultando…' : Formato.moneda(valor)),
                  color: insuficiente ? tema.colorScheme.error : null,
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}

class _Fila extends StatelessWidget {
  const _Fila(this.etiqueta, this.valor, {this.destacado = false, this.color});

  final String etiqueta;
  final String valor;
  final bool destacado;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final estilo = destacado ? tema.textTheme.titleMedium : tema.textTheme.bodyMedium;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(child: Text(etiqueta, style: tema.textTheme.bodyMedium?.copyWith(color: tema.colorScheme.onSurfaceVariant))),
          const SizedBox(width: 16),
          Flexible(
            child: Text(valor, textAlign: TextAlign.end, style: estilo?.copyWith(color: color)),
          ),
        ],
      ),
    );
  }
}

class _ErrorPago extends StatelessWidget {
  const _ErrorPago({required this.error});

  final ApiException error;

  @override
  Widget build(BuildContext context) {
    final colores = Theme.of(context).colorScheme;
    final sinRespuesta = error.sinRespuestaDelServidor || error.clave == 'TIMEOUT_GESTOPAGO';
    final titulo = sinRespuesta ? 'No sabemos si el pago se aplicó' : 'No se pudo realizar el pago';
    final detalle = sinRespuesta
        ? 'No recibimos respuesta a tiempo. Antes de intentarlo de nuevo revisa tu historial: '
            'si reintentas desde aquí se usa la misma operación y no se cobra dos veces.'
        : error.mensajeCompleto;
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: sinRespuesta ? const Color(0xFFFFF1CC) : colores.errorContainer,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(titulo, style: const TextStyle(fontWeight: FontWeight.w700)),
          const SizedBox(height: 4),
          Text(detalle),
          if (sinRespuesta)
            TextButton.icon(
              onPressed: () => context.go(Rutas.historial),
              icon: const Icon(Icons.receipt_long),
              label: const Text('Ver historial'),
            ),
        ],
      ),
    );
  }
}
