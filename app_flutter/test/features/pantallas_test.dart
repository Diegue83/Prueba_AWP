import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:gestopago_app/core/session/session_controller.dart';
import 'package:gestopago_app/data/models/producto.dart';
import 'package:gestopago_app/data/models/referencia.dart';
import 'package:gestopago_app/data/repositories/pago_repository.dart';
import 'package:gestopago_app/features/auth/login_page.dart';
import 'package:gestopago_app/features/pago/referencia_page.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:mocktail/mocktail.dart';
import 'package:provider/provider.dart';

import '../core/session_controller_test.dart';
import '../data/modelos_test.dart';

class PagoRepositoryFalso extends Mock implements PagoRepository {}

void main() {
  setUpAll(() async {
    await initializeDateFormatting('es_MX');
    registerFallbackValue(Producto.desdeJson(productoJson()));
  });

  testWidgets('el login valida los campos antes de llamar al backend', (tester) async {
    final sesion = SessionController(almacen: AlmacenEnMemoria());
    await tester.pumpWidget(
      ChangeNotifierProvider.value(value: sesion, child: const MaterialApp(home: LoginPage())),
    );

    await tester.tap(find.text('Iniciar sesión'));
    await tester.pump();

    expect(find.text('Escribe tu correo'), findsOneWidget);
    expect(find.text('Escribe tu contraseña'), findsOneWidget);
    sesion.dispose();
  });

  group('referencia', () {
    final producto = Producto.desdeJson(productoJson(tipoFront: 2, tipoReferencia: 'b', precio: 8));
    late PagoRepositoryFalso repositorio;

    Future<void> abrir(WidgetTester tester) async {
      tester.view.physicalSize = const Size(900, 1800);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      await tester.pumpWidget(
        Provider<PagoRepository>.value(
          value: repositorio,
          child: MaterialApp(home: ReferenciaPage(producto: producto)),
        ),
      );
    }

    Future<void> capturar(WidgetTester tester, String referencia) async {
      await tester.enterText(find.widgetWithText(TextFormField, 'Referencia del recibo'), referencia);
      await tester.enterText(find.widgetWithText(TextFormField, 'Confirma la referencia'), referencia);
      await tester.enterText(find.widgetWithText(TextFormField, 'Importe a pagar'), '250');
      await tester.tap(find.text('Verificar referencia'));
      await tester.pumpAndSettle();
    }

    setUp(() => repositorio = PagoRepositoryFalso());

    testWidgets('una referencia rechazada no permite continuar', (tester) async {
      when(() => repositorio.verificarReferencia(any(), any())).thenAnswer((_) async => const ReferenciaVerificada(
            estado: EstadoReferencia.invalida,
            referencia: 'ABC-0000',
            mensaje: 'Referencia Invalida',
            idServicio: 10,
            idProducto: 1,
          ));
      await abrir(tester);

      await capturar(tester, 'abc-0000');

      verify(() => repositorio.verificarReferencia(any(), 'ABC-0000')).called(1);
      expect(find.text('Referencia no válida'), findsOneWidget);
      expect(find.text('Revisar y confirmar'), findsNothing);
      expect(find.text('Verificar referencia'), findsOneWidget);
    });

    testWidgets('una referencia válida habilita continuar y si se edita vuelve a pedir verificación', (tester) async {
      when(() => repositorio.verificarReferencia(any(), any())).thenAnswer((_) async => const ReferenciaVerificada(
            estado: EstadoReferencia.valida,
            referencia: 'ABC-1234',
            mensaje: 'Referencia Valida',
            idServicio: 10,
            idProducto: 1,
            saldo: 250,
          ));
      await abrir(tester);

      await capturar(tester, 'ABC-1234');

      expect(find.text('Referencia válida'), findsOneWidget);
      expect(find.text('Adeudo pendiente: \$250.00'), findsOneWidget);
      expect(find.text('Revisar y confirmar'), findsOneWidget);

      await tester.enterText(find.widgetWithText(TextFormField, 'Referencia del recibo'), 'ABC-9999');
      await tester.pump();

      expect(find.text('Revisar y confirmar'), findsNothing);
      expect(find.text('Verificar referencia'), findsOneWidget);
    });

    testWidgets('el formato se valida antes de consultar al proveedor', (tester) async {
      await abrir(tester);

      await capturar(tester, 'ab');

      expect(find.text('Entre 4 y 40 caracteres: letras, números y guiones'), findsOneWidget);
      verifyNever(() => repositorio.verificarReferencia(any(), any()));
    });
  });
}
