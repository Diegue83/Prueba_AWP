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
  int _paso = 0;
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
      setState(() {
        _registro = registro;
        _paso = 1;
      });
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
        const SizedBox(height: 8),
        Stepper(
          physics: const NeverScrollableScrollPhysics(),
          currentStep: _paso,
          onStepTapped: (paso) {
            if (registro != null && paso > 0) {
              setState(() => _paso = paso);
            }
          },
          controlsBuilder: (context, detalles) => _controles(detalles.stepIndex),
          steps: [
            Step(
              title: const Text('Genera tu clave'),
              isActive: true,
              state: registro == null ? StepState.editing : StepState.complete,
              content: const Align(
                alignment: Alignment.centerLeft,
                child: Text('Se crea una clave única para tu cuenta. Solo se muestra una vez.'),
              ),
            ),
            Step(
              title: const Text('Agrégala a tu app autenticadora'),
              isActive: registro != null,
              state: _paso > 1 ? StepState.complete : StepState.indexed,
              content: registro == null ? const SizedBox.shrink() : _clave(registro),
            ),
            Step(
              title: const Text('Confirma con un código'),
              isActive: _paso == 2,
              content: Form(
                key: _formulario,
                child: TextFormField(
                  controller: _codigo,
                  decoration: const InputDecoration(
                    labelText: 'Código de 6 dígitos',
                    helperText: 'El que muestra tu app en este momento',
                    counterText: '',
                  ),
                  keyboardType: TextInputType.number,
                  maxLength: 6,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  autofillHints: const [AutofillHints.oneTimeCode],
                  validator: Validadores.codigo2fa,
                  onFieldSubmitted: (_) => _activar(),
                ),
              ),
            ),
          ],
        ),
        if (_error != null) ...[
          const SizedBox(height: 8),
          Text(_error!.mensajeCompleto, style: TextStyle(color: tema.colorScheme.error)),
        ],
      ],
    );
  }

  Widget _controles(int paso) {
    final Widget principal = switch (paso) {
      0 => BotonConCarga(texto: 'Generar código QR', icono: Icons.qr_code, cargando: _cargando, alPresionar: _generar),
      1 => FilledButton.icon(
          onPressed: () => setState(() => _paso = 2),
          icon: const Icon(Icons.arrow_forward),
          label: const Text('Ya la agregué'),
        ),
      _ => BotonConCarga(texto: 'Activar', icono: Icons.check, cargando: _cargando, alPresionar: _activar),
    };
    return Padding(
      padding: const EdgeInsets.only(top: 16),
      child: Wrap(
        spacing: 12,
        runSpacing: 8,
        children: [
          principal,
          if (paso == 2)
            TextButton(
              onPressed: _cargando ? null : () => setState(() => _paso = 1),
              child: const Text('Ver el código QR'),
            ),
        ],
      ),
    );
  }

  Widget _clave(RegistroSegundoFactor registro) {
    final tema = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text('Escanea este código con tu app:'),
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
        Text(
          'Guárdala en un lugar seguro: no se volverá a mostrar.',
          style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.error),
        ),
      ],
    );
  }
}
