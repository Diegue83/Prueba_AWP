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
  final _formulario = GlobalKey<FormState>();
  final _c = <String, TextEditingController>{};
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

  Future<void> _registrar() async {
    setState(() => _error = null);
    if (!_formulario.currentState!.validate() || _fechaNacimiento == null) {
      if (_fechaNacimiento == null) {
        setState(() => _error = const ApiException(mensaje: 'Indica tu fecha de nacimiento.'));
      }
      return;
    }
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
      setState(() => _error = e);
    } finally {
      if (mounted) {
        setState(() => _enviando = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final ancho = MediaQuery.sizeOf(context).width;
    final dosColumnas = ancho >= 700;
    return Scaffold(
      appBar: AppBar(title: const Text('Crear cuenta')),
      body: Form(
        key: _formulario,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
          children: [
            ContenidoCentrado(
              anchoMaximo: 860,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  _Seccion(titulo: 'Datos personales', dosColumnas: dosColumnas, campos: [
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
                  ]),
                  _Seccion(titulo: 'Identificación', dosColumnas: dosColumnas, campos: [
                    _campoTexto('curp', 'CURP', mayusculas: true, validador: Validadores.curp, longitud: 18),
                    _campoTexto('rfc', 'RFC con homoclave', mayusculas: true, validador: Validadores.rfc, longitud: 13),
                  ]),
                  _Seccion(titulo: 'Contacto', dosColumnas: dosColumnas, campos: [
                    _campoTexto('correo', 'Correo electrónico',
                        teclado: TextInputType.emailAddress, validador: Validadores.correo),
                    _campoTexto('telefonoMovil', 'Teléfono móvil', teclado: TextInputType.phone,
                        soloDigitos: true, longitud: 10, validador: Validadores.telefono),
                    _campoTexto('telefonoAlterno', 'Teléfono alterno (opcional)', teclado: TextInputType.phone,
                        soloDigitos: true, longitud: 10, validador: (v) => Validadores.telefono(v, obligatorio: false)),
                  ]),
                  _Seccion(titulo: 'Domicilio', dosColumnas: dosColumnas, campos: [
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
                    _campoTexto('pais', 'País', inicial: 'México',
                        validador: (v) => Validadores.obligatorio(v, 'El país')),
                  ]),
                  _Seccion(titulo: 'Actividad económica', dosColumnas: dosColumnas, campos: [
                    _campoTexto('ocupacion', 'Ocupación',
                        validador: (v) => Validadores.soloLetras(v, campo: 'La ocupación')),
                    _campoTexto('empresa', 'Empresa', validador: (v) => Validadores.obligatorio(v, 'La empresa')),
                    _campoTexto('ingresoMensual', 'Ingreso mensual', teclado: const TextInputType.numberWithOptions(decimal: true),
                        prefijo: r'$ ', validador: (v) => Validadores.monto(v, minimo: 0.01, maximo: 9999999999)),
                  ]),
                  _Seccion(titulo: 'Acceso', dosColumnas: dosColumnas, campos: [
                    _campoContrasena('contrasena', 'Contraseña', Validadores.contrasenaNueva),
                    _campoContrasena('confirmacion', 'Confirma tu contraseña',
                        (v) => v == _c['contrasena']?.text ? null : 'Las contraseñas no coinciden'),
                  ]),
                  if (_error != null)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 16),
                      child: Card(
                        color: Theme.of(context).colorScheme.errorContainer,
                        child: Padding(
                          padding: const EdgeInsets.all(16),
                          child: Text(_error!.mensajeCompleto,
                              style: TextStyle(color: Theme.of(context).colorScheme.onErrorContainer)),
                        ),
                      ),
                    ),
                  BotonConCarga(texto: 'Crear mi cuenta', cargando: _enviando, alPresionar: _registrar),
                ],
              ),
            ),
          ],
        ),
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
    return InkWell(
      onTap: _elegirFecha,
      borderRadius: BorderRadius.circular(12),
      child: InputDecorator(
        decoration: const InputDecoration(labelText: 'Fecha de nacimiento', suffixIcon: Icon(Icons.calendar_today)),
        isEmpty: _fechaNacimiento == null,
        child: Text(texto),
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

class _Seccion extends StatelessWidget {
  const _Seccion({required this.titulo, required this.campos, required this.dosColumnas});

  final String titulo;
  final List<Widget> campos;
  final bool dosColumnas;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(titulo, style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 16),
              LayoutBuilder(
                builder: (context, restricciones) {
                  final ancho = dosColumnas ? (restricciones.maxWidth - 16) / 2 : restricciones.maxWidth;
                  return Wrap(
                    spacing: 16,
                    runSpacing: 16,
                    children: [for (final campo in campos) SizedBox(width: ancho, child: campo)],
                  );
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}
