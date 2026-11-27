import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../app/router.dart';
import '../../core/api/api_exception.dart';
import '../../core/session/session_controller.dart';
import '../../core/validation/validadores.dart';
import '../../shared/widgets/estados.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key, this.correoInicial});

  final String? correoInicial;

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _formulario = GlobalKey<FormState>();
  late final _correo = TextEditingController(text: widget.correoInicial);
  final _contrasena = TextEditingController();
  bool _ocultar = true;
  bool _enviando = false;
  String? _error;

  @override
  void dispose() {
    _correo.dispose();
    _contrasena.dispose();
    super.dispose();
  }

  Future<void> _entrar() async {
    if (!_formulario.currentState!.validate()) {
      return;
    }
    setState(() {
      _enviando = true;
      _error = null;
    });
    try {
      await context.read<SessionController>().iniciarSesion(correo: _correo.text, contrasena: _contrasena.text);
    } on ApiException catch (e) {
      setState(() => _error = e.mensajeCompleto);
    } finally {
      if (mounted) {
        setState(() => _enviando = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final aviso = context.watch<SessionController>().aviso;
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: AutofillGroup(
                child: Form(
                  key: _formulario,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Icon(Icons.account_balance_wallet_rounded, size: 56, color: tema.colorScheme.primary),
                      const SizedBox(height: 12),
                      Text('GestoPago', textAlign: TextAlign.center, style: tema.textTheme.headlineMedium),
                      const SizedBox(height: 4),
                      Text(
                        'Paga tus servicios y recargas',
                        textAlign: TextAlign.center,
                        style: tema.textTheme.bodyLarge?.copyWith(color: tema.colorScheme.onSurfaceVariant),
                      ),
                      const SizedBox(height: 32),
                      if (aviso != null) ...[
                        _Aviso(texto: aviso, icono: Icons.info_outline),
                        const SizedBox(height: 16),
                      ],
                      TextFormField(
                        controller: _correo,
                        decoration: const InputDecoration(labelText: 'Correo electrónico', prefixIcon: Icon(Icons.mail_outline)),
                        keyboardType: TextInputType.emailAddress,
                        autofillHints: const [AutofillHints.email, AutofillHints.username],
                        textInputAction: TextInputAction.next,
                        validator: Validadores.correo,
                      ),
                      const SizedBox(height: 16),
                      TextFormField(
                        controller: _contrasena,
                        decoration: InputDecoration(
                          labelText: 'Contraseña',
                          prefixIcon: const Icon(Icons.lock_outline),
                          suffixIcon: IconButton(
                            tooltip: _ocultar ? 'Mostrar contraseña' : 'Ocultar contraseña',
                            icon: Icon(_ocultar ? Icons.visibility : Icons.visibility_off),
                            onPressed: () => setState(() => _ocultar = !_ocultar),
                          ),
                        ),
                        obscureText: _ocultar,
                        autofillHints: const [AutofillHints.password],
                        textInputAction: TextInputAction.done,
                        onFieldSubmitted: (_) => _entrar(),
                        validator: Validadores.contrasenaLogin,
                      ),
                      if (_error != null) ...[
                        const SizedBox(height: 16),
                        _Aviso(texto: _error!, icono: Icons.error_outline, esError: true),
                      ],
                      const SizedBox(height: 24),
                      BotonConCarga(texto: 'Iniciar sesión', cargando: _enviando, alPresionar: _entrar),
                      const SizedBox(height: 12),
                      TextButton(
                        onPressed: _enviando ? null : () => context.push(Rutas.registro),
                        child: const Text('¿No tienes cuenta? Regístrate'),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _Aviso extends StatelessWidget {
  const _Aviso({required this.texto, required this.icono, this.esError = false});

  final String texto;
  final IconData icono;
  final bool esError;

  @override
  Widget build(BuildContext context) {
    final colores = Theme.of(context).colorScheme;
    final fondo = esError ? colores.errorContainer : colores.secondaryContainer;
    final texto = esError ? colores.onErrorContainer : colores.onSecondaryContainer;
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(color: fondo, borderRadius: BorderRadius.circular(12)),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icono, color: texto),
          const SizedBox(width: 12),
          Expanded(child: Text(this.texto, style: TextStyle(color: texto))),
        ],
      ),
    );
  }
}
