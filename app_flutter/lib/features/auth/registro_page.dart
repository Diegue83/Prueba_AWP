import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../../app/router.dart';
import '../../core/api/api_exception.dart';
import '../../core/validation/validadores.dart';
import '../../data/models/cuenta.dart';
import '../../data/repositories/auth_repository.dart';
import '../../shared/widgets/estados.dart';

/// Alta de cliente (`POST /clientes`). Con el correo y la contraseña de aquí se entra
/// tanto en la app móvil como en la web.
class RegistroPage extends StatefulWidget {
  const RegistroPage({super.key});

  @override
  State<RegistroPage> createState() => _RegistroPageState();
}

class _RegistroPageState extends State<RegistroPage> {
  static const _pasos = [
    'Datos personales',
    'Identificación',
    'Contacto',
    'Domicilio',
    'Actividad económica',
    'Acceso',
  ];

  /// Paso en el que está cada campo que valida el backend, para llevar al usuario al error.
  static const _pasoDelCampo = {
    'nombre': 0, 'segundoNombre': 0, 'apellidoPaterno': 0, 'apellidoMaterno': 0,
    'fechaNacimiento': 0, 'sexo': 0, 'estadoCivil': 0, 'nacionalidad': 0,
    'curp': 1, 'rfc': 1,
    'correo': 2, 'telefonoMovil': 2, 'telefonoAlterno': 2,
    'domicilio': 3,
    'ocupacion': 4, 'empresa': 4, 'ingresoMensual': 4,
    'contrasena': 5,
  };

  final _formularios = List.generate(_pasos.length, (_) => GlobalKey<FormState>());
  final _c = <String, TextEditingController>{};
  int _paso = 0;
  int _pasoMaximo = 0;
  DateTime? _fechaNacimiento;
  String? _sexo;
  String? _estadoCivil;
  String? _nacionalidad;
  late final Future<List<Nacionalidad>> _nacionalidades = context.read<AuthRepository>().consultarNacionalidades();
  bool _enviando = false;
  bool _ocultar = true;
  ApiException? _error;

  static const _sexos = {'MUJER': 'Mujer', 'HOMBRE': 'Hombre', 'NO_BINARIO': 'No binario'};
  static const _estadosCiviles = {
    'SOLTERO': 'Soltero(a)',
    'CASADO': 'Casado(a)',
    'DIVORCIADO': 'Divorciado(a)',
    'VIUDO': 'Viudo(a)',
    'UNION_LIBRE': 'Unión libre',
  };

  TextEditingController _campo(String nombre) => _c.putIfAbsent(nombre, TextEditingController.new);

  String? _texto(String nombre) {
    final valor = _c[nombre]?.text.trim() ?? '';
    return valor.isEmpty ? null : valor;
  }

  @override
  void dispose() {
    for (final controlador in _c.values) {
      controlador.dispose();
    }
    super.dispose();
  }

  Future<void> _elegirFecha() async {
    final hoy = DateTime.now();
    final elegida = await showDatePicker(
      context: context,
      initialDate: _fechaNacimiento ?? DateTime(hoy.year - 25),
      firstDate: DateTime(hoy.year - 100),
      lastDate: DateTime(hoy.year - 18, hoy.month, hoy.day),
      helpText: 'Fecha de nacimiento',
    );
    if (elegida != null) {
      setState(() => _fechaNacimiento = elegida);
    }
  }

  bool _validarPaso(int paso) => _formularios[paso].currentState?.validate() ?? true;

  void _continuar() {
    if (!_validarPaso(_paso)) {
      return;
    }
    if (_paso == _pasos.length - 1) {
      _registrar();
      return;
    }
    setState(() {
      _error = null;
      _paso++;
      if (_paso > _pasoMaximo) {
        _pasoMaximo = _paso;
      }
    });
  }

  void _irAlPaso(int destino) {
    if (destino == _paso || destino > _pasoMaximo) {
      return;
    }
    if (destino > _paso && !_validarPaso(_paso)) {
      return;
    }
    setState(() => _paso = destino);
  }

  /// Lleva al primer paso que tenga un campo señalado por el backend (por ejemplo "curp: ya registrada").
  void _irAlPasoConError(ApiException error) {
    final pasos = [
      for (final detalle in error.detalles)
        _pasoDelCampo[detalle.split(':').first.split('.').first.trim()],
    ].whereType<int>();
    if (pasos.isNotEmpty) {
      _paso = pasos.reduce((a, b) => a < b ? a : b);
    }
  }

  StepState _estado(int paso) {
    if (paso == _paso) {
      return StepState.editing;
    }
    return paso < _pasoMaximo || (paso == _pasoMaximo && paso < _paso) ? StepState.complete : StepState.indexed;
  }

  Future<void> _registrar() async {
    setState(() => _error = null);
    final cliente = <String, dynamic>{
      'nombre': _texto('nombre'),
      'segundoNombre': _texto('segundoNombre'),
      'apellidoPaterno': _texto('apellidoPaterno'),
      'apellidoMaterno': _texto('apellidoMaterno'),
      'fechaNacimiento': DateFormat('yyyy-MM-dd').format(_fechaNacimiento!),
      'sexo': _sexo,
      'nacionalidad': _nacionalidad,
      'estadoCivil': _estadoCivil,
      'curp': _texto('curp')?.toUpperCase(),
      'rfc': _texto('rfc')?.toUpperCase(),
      'correo': _texto('correo')?.toLowerCase(),
      'telefonoMovil': _texto('telefonoMovil'),
      'telefonoAlterno': _texto('telefonoAlterno'),
      'ocupacion': _texto('ocupacion'),
      'empresa': _texto('empresa'),
      'ingresoMensual': double.tryParse(_texto('ingresoMensual') ?? ''),
      'contrasena': _c['contrasena']!.text,
      'domicilio': {
        'calle': _texto('calle'),
        'numeroExterior': _texto('numeroExterior'),
        'numeroInterior': _texto('numeroInterior'),
        'colonia': _texto('colonia'),
        'municipio': _texto('municipio'),
        'estado': _texto('estado'),
        'codigoPostal': _texto('codigoPostal'),
        'pais': _texto('pais') ?? 'México',
      }..removeWhere((_, v) => v == null),
    }..removeWhere((_, v) => v == null);

    setState(() => _enviando = true);
    try {
      final cuenta = await context.read<AuthRepository>().registrarCliente(cliente);
      if (!mounted) {
        return;
      }
      await showDialog<void>(
        context: context,
        builder: (context) => AlertDialog(
          icon: const Icon(Icons.check_circle, size: 40),
          title: const Text('¡Registro exitoso!'),
          content: Text(
            'Tu cuenta${cuenta == null ? '' : ' $cuenta'} está lista. Inicia sesión con tu correo y contraseña; '
            'son los mismos en la app y en la web.',
          ),
          actions: [FilledButton(onPressed: () => Navigator.pop(context), child: const Text('Iniciar sesión'))],
        ),
      );
      if (mounted) {
        context.go(Rutas.login);
      }
    } on ApiException catch (e) {
      setState(() {
        _error = e;
        _irAlPasoConError(e);
      });
    } finally {
      if (mounted) {
        setState(() => _enviando = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final ancho = MediaQuery.sizeOf(context).width;
    final horizontal = ancho >= 840;
    final dosColumnas = ancho >= 700;
    final secciones = _secciones();
    return Scaffold(
      appBar: AppBar(title: const Text('Crear cuenta')),
      body: ContenidoCentrado(
        anchoMaximo: 960,
        child: Stepper(
          type: horizontal ? StepperType.horizontal : StepperType.vertical,
          currentStep: _paso,
          onStepContinue: _enviando ? null : _continuar,
          onStepCancel: _enviando || _paso == 0 ? null : () => setState(() => _paso--),
          onStepTapped: _enviando ? null : _irAlPaso,
          controlsBuilder: _controles,
          steps: [
            for (var i = 0; i < _pasos.length; i++)
              Step(
                title: Text(_pasos[i]),
                isActive: i <= _pasoMaximo,
                state: _estado(i),
                content: Form(
                  key: _formularios[i],
                  child: _Campos(dosColumnas: dosColumnas, campos: secciones[i]),
                ),
              ),
          ],
        ),
      ),
    );
  }

  List<List<Widget>> _secciones() => [
        [
          _campoTexto('nombre', 'Nombre',
              validador: (v) => Validadores.soloLetras(v, campo: 'El nombre', minimo: 3)),
          _campoTexto('segundoNombre', 'Segundo nombre (opcional)',
              validador: (v) => Validadores.soloLetras(v, campo: 'El segundo nombre', obligatorio: false)),
          _campoTexto('apellidoPaterno', 'Apellido paterno',
              validador: (v) => Validadores.soloLetras(v, campo: 'El apellido paterno')),
          _campoTexto('apellidoMaterno', 'Apellido materno',
              validador: (v) => Validadores.soloLetras(v, campo: 'El apellido materno')),
          _selectorFecha(),
          _desplegable('Sexo', _sexo, _sexos, (v) => setState(() => _sexo = v)),
          _desplegable('Estado civil', _estadoCivil, _estadosCiviles, (v) => setState(() => _estadoCivil = v)),
          _selectorNacionalidad(),
        ],
        [
          _campoTexto('curp', 'CURP', mayusculas: true, validador: Validadores.curp, longitud: 18),
          _campoTexto('rfc', 'RFC con homoclave', mayusculas: true, validador: Validadores.rfc, longitud: 13),
        ],
        [
          _campoTexto('correo', 'Correo electrónico', teclado: TextInputType.emailAddress, validador: Validadores.correo),
          _campoTexto('telefonoMovil', 'Teléfono móvil', teclado: TextInputType.phone,
              soloDigitos: true, longitud: 10, validador: Validadores.telefono),
          _campoTexto('telefonoAlterno', 'Teléfono alterno (opcional)', teclado: TextInputType.phone,
              soloDigitos: true, longitud: 10, validador: (v) => Validadores.telefono(v, obligatorio: false)),
        ],
        [
          _campoTexto('calle', 'Calle', validador: (v) => Validadores.obligatorio(v, 'La calle')),
          _campoTexto('numeroExterior', 'Número exterior',
              validador: (v) => Validadores.obligatorio(v, 'El número exterior')),
          _campoTexto('numeroInterior', 'Número interior (opcional)'),
          _campoTexto('colonia', 'Colonia', validador: (v) => Validadores.obligatorio(v, 'La colonia')),
          _campoTexto('municipio', 'Municipio o alcaldía',
              validador: (v) => Validadores.soloLetras(v, campo: 'El municipio')),
          _campoTexto('estado', 'Estado', validador: (v) => Validadores.soloLetras(v, campo: 'El estado')),
          _campoTexto('codigoPostal', 'Código postal', teclado: TextInputType.number,
              soloDigitos: true, longitud: 5, validador: Validadores.codigoPostal),
          _campoTexto('pais', 'País', inicial: 'México', validador: (v) => Validadores.obligatorio(v, 'El país')),
        ],
        [
          _campoTexto('ocupacion', 'Ocupación', validador: (v) => Validadores.soloLetras(v, campo: 'La ocupación')),
          _campoTexto('empresa', 'Empresa', validador: (v) => Validadores.obligatorio(v, 'La empresa')),
          _campoTexto('ingresoMensual', 'Ingreso mensual',
              teclado: const TextInputType.numberWithOptions(decimal: true),
              prefijo: r'$ ', validador: (v) => Validadores.monto(v, minimo: 0.01, maximo: 9999999999)),
        ],
        [
          _campoContrasena('contrasena', 'Contraseña', Validadores.contrasenaNueva),
          _campoContrasena('confirmacion', 'Confirma tu contraseña',
              (v) => v == _c['contrasena']?.text ? null : 'Las contraseñas no coinciden'),
        ],
      ];

  Widget _controles(BuildContext context, ControlsDetails detalles) {
    final ultimo = detalles.stepIndex == _pasos.length - 1;
    final colores = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.only(top: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (_error != null && detalles.stepIndex == _paso) ...[
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(color: colores.errorContainer, borderRadius: BorderRadius.circular(12)),
              child: Text(_error!.mensajeCompleto, style: TextStyle(color: colores.onErrorContainer)),
            ),
            const SizedBox(height: 12),
          ],
          Wrap(
            spacing: 12,
            runSpacing: 8,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              BotonConCarga(
                texto: ultimo ? 'Crear mi cuenta' : 'Continuar',
                icono: ultimo ? Icons.check : Icons.arrow_forward,
                cargando: _enviando,
                alPresionar: detalles.onStepContinue,
              ),
              if (detalles.stepIndex > 0)
                TextButton(onPressed: detalles.onStepCancel, child: const Text('Atrás')),
              Text('Paso ${detalles.stepIndex + 1} de ${_pasos.length}',
                  style: Theme.of(context).textTheme.bodySmall),
            ],
          ),
        ],
      ),
    );
  }

  Widget _campoTexto(
    String nombre,
    String etiqueta, {
    String? Function(String?)? validador,
    TextInputType? teclado,
    bool mayusculas = false,
    bool soloDigitos = false,
    int? longitud,
    String? prefijo,
    String? inicial,
  }) {
    final controlador = _campo(nombre);
    if (inicial != null && controlador.text.isEmpty) {
      controlador.text = inicial;
    }
    return TextFormField(
      controller: controlador,
      decoration: InputDecoration(labelText: etiqueta, prefixText: prefijo, counterText: ''),
      keyboardType: teclado,
      textCapitalization: mayusculas ? TextCapitalization.characters : TextCapitalization.words,
      maxLength: longitud,
      inputFormatters: [if (soloDigitos) FilteringTextInputFormatter.digitsOnly],
      textInputAction: TextInputAction.next,
      validator: validador,
    );
  }

  Widget _campoContrasena(String nombre, String etiqueta, String? Function(String?) validador) {
    return TextFormField(
      controller: _campo(nombre),
      obscureText: _ocultar,
      decoration: InputDecoration(
        labelText: etiqueta,
        helperText: nombre == 'contrasena' ? '8+ caracteres, mayúscula, minúscula y número' : null,
        suffixIcon: IconButton(
          tooltip: _ocultar ? 'Mostrar' : 'Ocultar',
          icon: Icon(_ocultar ? Icons.visibility : Icons.visibility_off),
          onPressed: () => setState(() => _ocultar = !_ocultar),
        ),
      ),
      autofillHints: const [AutofillHints.newPassword],
      validator: validador,
    );
  }

  Widget _selectorFecha() {
    final texto = _fechaNacimiento == null ? '' : DateFormat('d MMMM yyyy', 'es_MX').format(_fechaNacimiento!);
    return FormField<DateTime>(
      validator: (_) => _fechaNacimiento == null ? 'Indica tu fecha de nacimiento' : null,
      builder: (campo) => InkWell(
        onTap: () async {
          await _elegirFecha();
          campo.didChange(_fechaNacimiento);
        },
        borderRadius: BorderRadius.circular(12),
        child: InputDecorator(
          decoration: InputDecoration(
            labelText: 'Fecha de nacimiento',
            suffixIcon: const Icon(Icons.calendar_today),
            errorText: campo.errorText,
          ),
          isEmpty: _fechaNacimiento == null,
          child: Text(texto),
        ),
      ),
    );
  }

  Widget _desplegable(String etiqueta, String? valor, Map<String, String> opciones, ValueChanged<String?> alCambiar) {
    return DropdownButtonFormField<String>(
      initialValue: valor,
      decoration: InputDecoration(labelText: etiqueta),
      items: [for (final e in opciones.entries) DropdownMenuItem(value: e.key, child: Text(e.value))],
      onChanged: alCambiar,
      validator: (v) => v == null ? 'Selecciona una opción' : null,
    );
  }

  Widget _selectorNacionalidad() {
    return FutureBuilder<List<Nacionalidad>>(
      future: _nacionalidades,
      builder: (context, foto) {
        final lista = foto.data ?? const <Nacionalidad>[];
        return DropdownButtonFormField<String>(
          initialValue: _nacionalidad,
          isExpanded: true,
          decoration: InputDecoration(
            labelText: 'Nacionalidad',
            helperText: foto.hasError ? 'No se pudo cargar el catálogo de nacionalidades' : null,
          ),
          items: [for (final n in lista) DropdownMenuItem(value: n.nombre, child: Text(n.nombre))],
          onChanged: (v) => setState(() => _nacionalidad = v),
          validator: (v) => v == null ? 'Selecciona tu nacionalidad' : null,
        );
      },
    );
  }
}

class _Campos extends StatelessWidget {
  const _Campos({required this.campos, required this.dosColumnas});

  final List<Widget> campos;
  final bool dosColumnas;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: LayoutBuilder(
        builder: (context, restricciones) {
          final ancho = dosColumnas ? (restricciones.maxWidth - 16) / 2 : restricciones.maxWidth;
          return Wrap(
            spacing: 16,
            runSpacing: 16,
            children: [for (final campo in campos) SizedBox(width: ancho, child: campo)],
          );
        },
      ),
    );
  }
}
