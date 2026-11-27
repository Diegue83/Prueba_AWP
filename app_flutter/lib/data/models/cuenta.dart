class Saldo {
  const Saldo({required this.numeroCuenta, required this.saldo});

  factory Saldo.desdeJson(Map<String, dynamic> json) => Saldo(
        numeroCuenta: json['numeroCuenta'] as String? ?? '',
        saldo: (json['saldo'] as num?)?.toDouble() ?? 0,
      );

  final String numeroCuenta;
  final double saldo;
}

class Nacionalidad {
  const Nacionalidad({required this.clave, required this.nombre});

  factory Nacionalidad.desdeJson(Map<String, dynamic> json) =>
      Nacionalidad(clave: json['clave'] as String? ?? '', nombre: json['nombre'] as String? ?? '');

  final String clave;
  final String nombre;
}

/// Se entrega una sola vez al registrar el segundo factor; el cliente lo escanea en su app autenticadora.
class RegistroSegundoFactor {
  const RegistroSegundoFactor({required this.secreto, required this.uriOtpauth});

  factory RegistroSegundoFactor.desdeJson(Map<String, dynamic> json) => RegistroSegundoFactor(
        secreto: json['secreto'] as String,
        uriOtpauth: json['uriOtpauth'] as String,
      );

  final String secreto;
  final String uriOtpauth;
}
