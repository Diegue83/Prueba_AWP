/// Datos que devuelve `POST /login`. El token caduca en [expiraEn]; la app no lo usa después.
class Sesion {
  const Sesion({
    required this.clienteId,
    required this.nombreCompleto,
    required this.correo,
    required this.numeroCuenta,
    required this.token,
    required this.expiraEn,
    this.ultimoAcceso,
  });

  factory Sesion.desdeLogin(Map<String, dynamic> json, {DateTime? ahora}) {
    final minutos = (json['expiraEnMinutos'] as num?)?.toInt() ?? 30;
    return Sesion(
      clienteId: (json['clienteId'] as num).toInt(),
      nombreCompleto: json['nombreCompleto'] as String? ?? '',
      correo: json['correo'] as String? ?? '',
      numeroCuenta: json['numeroCuenta'] as String? ?? '',
      token: json['token'] as String,
      ultimoAcceso: DateTime.tryParse(json['ultimoAcceso'] as String? ?? ''),
      expiraEn: (ahora ?? DateTime.now()).add(Duration(minutes: minutos)),
    );
  }

  factory Sesion.desdeJson(Map<String, dynamic> json) => Sesion(
        clienteId: (json['clienteId'] as num).toInt(),
        nombreCompleto: json['nombreCompleto'] as String,
        correo: json['correo'] as String,
        numeroCuenta: json['numeroCuenta'] as String,
        token: json['token'] as String,
        ultimoAcceso: DateTime.tryParse(json['ultimoAcceso'] as String? ?? ''),
        expiraEn: DateTime.parse(json['expiraEn'] as String),
      );

  final int clienteId;
  final String nombreCompleto;
  final String correo;
  final String numeroCuenta;
  final String token;
  final DateTime? ultimoAcceso;
  final DateTime expiraEn;

  bool estaVigente([DateTime? ahora]) => (ahora ?? DateTime.now()).isBefore(expiraEn);

  Map<String, dynamic> aJson() => {
        'clienteId': clienteId,
        'nombreCompleto': nombreCompleto,
        'correo': correo,
        'numeroCuenta': numeroCuenta,
        'token': token,
        'ultimoAcceso': ultimoAcceso?.toIso8601String(),
        'expiraEn': expiraEn.toIso8601String(),
      };
}
