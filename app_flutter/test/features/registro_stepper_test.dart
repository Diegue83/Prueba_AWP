import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:gestopago_app/core/api/api_exception.dart';
import 'package:gestopago_app/data/models/cuenta.dart';
import 'package:gestopago_app/data/repositories/auth_repository.dart';
import 'package:gestopago_app/features/auth/registro_page.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:mocktail/mocktail.dart';
import 'package:provider/provider.dart';

class AuthRepositoryFalso extends Mock implements AuthRepository {}

void main() {
  late AuthRepositoryFalso auth;

  setUpAll(() => initializeDateFormatting('es_MX'));

  setUp(() {
    auth = AuthRepositoryFalso();
    when(() => auth.consultarNacionalidades())
        .thenAnswer((_) async => const [Nacionalidad(clave: 'MEX', nombre: 'Mexicana')]);
  });

  Future<void> abrir(WidgetTester tester) async {
    tester.view.physicalSize = const Size(700, 2600);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(
      Provider<AuthRepository>.value(value: auth, child: const MaterialApp(home: RegistroPage())),
    );
    await tester.pumpAndSettle();
  }

  Future<void> escribir(WidgetTester tester, String etiqueta, String valor) async {
    final campo = find.widgetWithText(TextFormField, etiqueta);
    await tester.ensureVisible(campo);
    await tester.enterText(campo, valor);
  }

  Future<void> elegir(WidgetTester tester, String etiqueta, String opcion) async {
    final campo = find.widgetWithText(DropdownButtonFormField<String>, etiqueta);
    await tester.ensureVisible(campo);
    await tester.tap(campo);
    await tester.pumpAndSettle();
    await tester.tap(find.text(opcion).last);
    await tester.pumpAndSettle();
  }

  Future<void> continuar(WidgetTester tester) async {
    final boton = find.widgetWithText(FilledButton, 'Continuar').hitTestable();
    await tester.ensureVisible(boton);
    await tester.tap(boton);
    await tester.pumpAndSettle();
  }

  Future<void> llenarDatosPersonales(WidgetTester tester) async {
    await escribir(tester, 'Nombre', 'Zayda');
    await escribir(tester, 'Apellido paterno', 'Vargas');
    await escribir(tester, 'Apellido materno', 'Vargas');
    await tester.tap(find.byIcon(Icons.calendar_today));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(TextButton, 'OK'));
    await tester.pumpAndSettle();
    await elegir(tester, 'Sexo', 'Mujer');
    await elegir(tester, 'Estado civil', 'Soltero(a)');
    await elegir(tester, 'Nacionalidad', 'Mexicana');
  }

  testWidgets('no avanza mientras el paso tenga errores ni deja saltar pasos', (tester) async {
    await abrir(tester);

    await continuar(tester);

    expect(find.text('Paso 1 de 6'), findsOneWidget);
    expect(find.text('El nombre es obligatorio'), findsOneWidget);
    expect(find.text('Indica tu fecha de nacimiento'), findsOneWidget);

    await tester.tap(find.text('Acceso'));
    await tester.pumpAndSettle();

    expect(find.text('Paso 1 de 6'), findsOneWidget);
  });

  testWidgets('recorre los pasos y un rechazo del backend regresa al paso del campo', (tester) async {
    when(() => auth.registrarCliente(any())).thenThrow(const ApiException(
      mensaje: 'No se pudo registrar',
      estatusHttp: 409,
      clave: 'CURP_DUPLICADA',
      detalles: ['curp: ya está registrada'],
    ));
    await abrir(tester);

    await llenarDatosPersonales(tester);
    await continuar(tester);
    expect(find.text('Paso 2 de 6'), findsOneWidget);

    await escribir(tester, 'CURP', 'VAVZ950312MGTRRY07');
    await escribir(tester, 'RFC con homoclave', 'VAVZ950312AB1');
    await continuar(tester);
    await escribir(tester, 'Correo electrónico', 'zayda@correo.com');
    await escribir(tester, 'Teléfono móvil', '4181234567');
    await continuar(tester);
    await escribir(tester, 'Calle', 'Av. Hidalgo');
    await escribir(tester, 'Número exterior', '12');
    await escribir(tester, 'Colonia', 'Centro');
    await escribir(tester, 'Municipio o alcaldía', 'Dolores Hidalgo');
    await escribir(tester, 'Estado', 'Guanajuato');
    await escribir(tester, 'Código postal', '37800');
    await continuar(tester);
    await escribir(tester, 'Ocupación', 'Ingeniera');
    await escribir(tester, 'Empresa', 'UTNG');
    await escribir(tester, 'Ingreso mensual', '15000');
    await continuar(tester);
    expect(find.text('Paso 6 de 6'), findsOneWidget);

    await escribir(tester, 'Contraseña', 'Segura123');
    await escribir(tester, 'Confirma tu contraseña', 'Segura123');
    final crear = find.widgetWithText(FilledButton, 'Crear mi cuenta').hitTestable();
    await tester.ensureVisible(crear);
    await tester.tap(crear);
    await tester.pumpAndSettle();

    final enviado = verify(() => auth.registrarCliente(captureAny())).captured.single as Map<String, dynamic>;
    expect(enviado['curp'], 'VAVZ950312MGTRRY07');
    expect(enviado['sexo'], 'MUJER');
    expect((enviado['domicilio'] as Map)['codigoPostal'], '37800');
    expect(find.text('Paso 2 de 6'), findsOneWidget);
    expect(find.textContaining('curp: ya está registrada'), findsOneWidget);
  });
}
