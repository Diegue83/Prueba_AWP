import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../app/router.dart';
import '../../core/format/formato.dart';
import '../../core/session/session_controller.dart';
import '../../data/models/cuenta.dart';
import '../../data/repositories/cuenta_repository.dart';
import '../../shared/widgets/estados.dart';

class CuentaPage extends StatefulWidget {
  const CuentaPage({super.key});

  @override
  State<CuentaPage> createState() => _CuentaPageState();
}

class _CuentaPageState extends State<CuentaPage> {
  late Future<Saldo> _saldo = _consultar();

  Future<Saldo> _consultar() {
    final cuenta = context.read<SessionController>().sesion!.numeroCuenta;
    return context.read<CuentaRepository>().consultarSaldo(cuenta);
  }

  Future<void> _cerrarSesion() async {
    final confirmar = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('¿Cerrar sesión?'),
        content: const Text('Tendrás que escribir tu correo y contraseña para volver a entrar.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Cerrar sesión')),
        ],
      ),
    );
    if (confirmar == true && mounted) {
      await context.read<SessionController>().cerrarSesion();
    }
  }

  @override
  Widget build(BuildContext context) {
    final sesion = context.watch<SessionController>().sesion;
    final tema = Theme.of(context);
    if (sesion == null) {
      return const SizedBox.shrink();
    }
    return Scaffold(
      appBar: AppBar(title: const Text('Mi cuenta')),
      body: RefreshIndicator(
        onRefresh: () async {
          final futuro = _consultar();
          setState(() => _saldo = futuro);
          await futuro.catchError((_) => const Saldo(numeroCuenta: '', saldo: 0));
        },
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            ContenidoCentrado(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(sesion.nombreCompleto, style: tema.textTheme.titleLarge),
                          Text(sesion.correo, style: tema.textTheme.bodyMedium),
                          const SizedBox(height: 16),
                          Text('Saldo disponible', style: tema.textTheme.labelLarge),
                          FutureBuilder<Saldo>(
                            future: _saldo,
                            builder: (context, foto) {
                              if (foto.hasError) {
                                return const Text('No se pudo consultar el saldo');
                              }
                              return Text(
                                foto.hasData ? Formato.moneda(foto.data!.saldo) : 'Consultando…',
                                style: tema.textTheme.displaySmall?.copyWith(color: tema.colorScheme.primary),
                              );
                            },
                          ),
                          const SizedBox(height: 8),
                          Text('Cuenta ${Formato.enmascarar(sesion.numeroCuenta)}', style: tema.textTheme.bodyMedium),
                          if (sesion.ultimoAcceso != null)
                            Text('Último acceso: ${Formato.fecha(sesion.ultimoAcceso)}', style: tema.textTheme.bodySmall),
                          Text('Tu sesión vence: ${Formato.fechaHora(sesion.expiraEn)}', style: tema.textTheme.bodySmall),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Card(
                    child: Column(
                      children: [
                        ListTile(
                          leading: const Icon(Icons.shield_outlined),
                          title: const Text('Código de seguridad para pagos'),
                          subtitle: const Text('Activa tu app autenticadora (2FA)'),
                          trailing: const Icon(Icons.chevron_right),
                          onTap: () => context.go(Rutas.segundoFactor),
                        ),
                        const Divider(height: 1),
                        ListTile(
                          leading: Icon(Icons.logout, color: tema.colorScheme.error),
                          title: Text('Cerrar sesión', style: TextStyle(color: tema.colorScheme.error)),
                          onTap: _cerrarSesion,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
