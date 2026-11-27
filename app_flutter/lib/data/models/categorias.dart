import 'package:flutter/material.dart';

/// Catálogo `idCatTipoServicio` de la especificación de PuntoRed.
class Categoria {
  const Categoria(this.id, this.nombre, this.icono);

  final int id;
  final String nombre;
  final IconData icono;

  static const Map<int, Categoria> _porId = {
    1: Categoria(1, 'Movistar', Icons.phone_android),
    2: Categoria(2, 'Telcel', Icons.phone_android),
    3: Categoria(3, 'AT&T', Icons.phone_android),
    4: Categoria(4, 'Unefon', Icons.phone_android),
    5: Categoria(5, 'Pago de servicios', Icons.receipt_long),
    6: Categoria(6, 'Nextel', Icons.phone_android),
    7: Categoria(7, 'Impuestos y gobierno', Icons.account_balance),
    8: Categoria(8, 'Productos financieros', Icons.savings),
    9: Categoria(9, 'Productos por catálogo', Icons.menu_book),
    10: Categoria(10, 'Servicios de prepago', Icons.card_giftcard),
    11: Categoria(11, 'Entretenimiento', Icons.movie),
    12: Categoria(12, 'Virgin', Icons.phone_android),
    13: Categoria(13, 'Otras recargas', Icons.phone_iphone),
    14: Categoria(14, 'Seguros', Icons.health_and_safety),
    15: Categoria(15, 'Agua', Icons.water_drop),
    16: Categoria(16, 'Juegos y sorteos', Icons.casino),
    17: Categoria(17, 'MazTiempo', Icons.phone_android),
    18: Categoria(18, 'Autopistas y transporte', Icons.directions_car),
    19: Categoria(19, 'Internet de prepago', Icons.wifi),
  };

  static Categoria de(int id) => _porId[id] ?? Categoria(id, 'Otros servicios', Icons.category);
}
