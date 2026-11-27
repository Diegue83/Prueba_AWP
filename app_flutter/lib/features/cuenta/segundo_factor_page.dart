import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../../core/api/api_exception.dart';
import '../../core/validation/validadores.dart';
import '../../data/models/cuenta.dart';
import '../../data/repositories/cuenta_repository.dart';
import '../../shared/widgets/estados.dart';

/// Registro y activación del segundo factor (TOTP). El secreto solo se muestra una vez.
class SegundoFactorPage extends StatefulWidget {
  const SegundoFactorPage({super.key});

  @override
  State<SegundoFactorPage> createState() => _SegundoFactorPageState();
}

class _SegundoFactorPageState extends State<SegundoFactorPage> {
  final _formulario = GlobalKey<FormState>();
  final _codigo = TextEditingController();
  RegistroSegundoFactor? _registro;
  bool _cargando = false;
  bool _activado = false;
  bool _yaActivo = false;
  ApiException? _error;

  @override
  void dispose() {
    _codigo.dispose();
    super.dispose();
  }

  Future<void> _generar() async {
    setState(() {
      _cargando = true;
      _error = null;
    });
    try {
      final registro = await context.read<CuentaRepository>().registrarSegundoFactor();
      setState(() => _registro = registro);
    } on ApiException catch (e) {
      setState(() {
        if (e.clave == '2FA_YA_ACTIVO') {
          _yaActivo = true;
        } else {
          _error = e;
        }
      });
    } finally {
      if (mounted) {
        setState(() => _cargando = false);
      }
    }
  }

  Future<void> _activar() async {
    if (!_formulario.currentState!.validate()) {
      return;
    }
    setState(() {
      _cargando = true;
      _error = null;
    });
    try {
      await context.read<CuentaRepository>().activarSegundoFactor(_codigo.text.trim());
      setState(() => _activado = true);
    } on ApiException catch (e) {
      setState(() => _error = e);
    } finally {
      if (mounted) {
        setState(() => _cargando = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Código de seguridad')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [ContenidoCentrado(anchoMaximo: 520, child: _contenido())],
      ),
    );
  }

  Widget _contenido() {
    final tema = Theme.of(context);
    if (_activado || _yaActivo) {
      return VistaMensaje(
        icono: Icons.verified_user,
        titulo: _activado ? '¡Listo! Tu código de seguridad está activo' : 'Ya tienes activo tu código de seguridad',
        mensaje: 'Cada vez que pagues, escribe el código de 6 dígitos que muestra tu app autenticadora.',
      );
    }
    final registro = _registro;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          'Para autorizar pagos usamos un código que cambia cada 30 segundos. Necesitas una app como '
          'Google Authenticator o Microsoft Authenticator.',
          style: tema.textTheme.bodyLarge,
        ),
        const SizedBox(height: 24),
        if (registro == null)
          BotonConCarga(texto: 'Generar código QR', icono: Icons.qr_code, cargando: _cargando, alPresionar: _generar)
        else ...[
          Text('1. Escanea este código con tu app', style: tema.textTheme.titleMedium),
          const SizedBox(height: 12),
          Center(
            child: Container(
              color: Colors.white,
              padding: const EdgeInsets.all(12),
              child: QrImageView(data: registro.uriOtpauth, size: 220, semanticsLabel: 'Código QR para tu app autenticadora'),
            ),
          ),
          const SizedBox(height: 12),
          Text('¿No puedes escanear? Escribe esta clave:', style: tema.textTheme.bodyMedium),
          Row(
            children: [
              Expanded(child: SelectableText(registro.secreto, style: tema.textTheme.titleMedium)),
              IconButton(
                tooltip: 'Copiar clave',
                icon: const Icon(Icons.copy),
                onPressed: () => Clipboard.setData(ClipboardData(text: registro.secreto)),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            'Guárdala en un lugar seguro: no se volverá a mostrar.',
            style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.error),
          ),
          const SizedBox(height: 24),
          Text('2. Escribe el código que muestra tu app', style: tema.textTheme.titleMedium),
          const SizedBox(height: 12),
          Form(
            key: _formulario,
            child: TextFormField(
              controller: _codigo,
              decoration: const InputDecoration(labelText: 'Código de 6 dígitos', counterText: ''),
              keyboardType: TextInputType.number,
              maxLength: 6,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              autofillHints: const [AutofillHints.oneTimeCode],
              validator: Validadores.codigo2fa,
              onFieldSubmitted: (_) => _activar(),
            ),
          ),
          const SizedBox(height: 16),
          BotonConCarga(texto: 'Activar', icono: Icons.check, cargando: _cargando, alPresionar: _activar),
        ],
        if (_error != null) ...[
          const SizedBox(height: 16),
          Text(_error!.mensajeCompleto, style: TextStyle(color: tema.colorScheme.error)),
        ],
      ],
    );
  }
}
