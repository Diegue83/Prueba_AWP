import 'package:flutter/material.dart';

class Tema {
  const Tema._();

  static const Color _semilla = Color(0xFF0D5C63);

  static ThemeData claro() => _construir(ColorScheme.fromSeed(seedColor: _semilla));

  static ThemeData oscuro() => _construir(ColorScheme.fromSeed(seedColor: _semilla, brightness: Brightness.dark));

  static ThemeData _construir(ColorScheme colores) {
    final base = ThemeData(colorScheme: colores, useMaterial3: true);
    return base.copyWith(
      scaffoldBackgroundColor: colores.surfaceContainerLowest,
      appBarTheme: AppBarTheme(
        backgroundColor: colores.surfaceContainerLowest,
        surfaceTintColor: Colors.transparent,
        centerTitle: false,
      ),
      cardTheme: CardThemeData(
        elevation: 0,
        color: colores.surface,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: BorderSide(color: colores.outlineVariant),
        ),
        margin: EdgeInsets.zero,
      ),
      inputDecorationTheme: InputDecorationTheme(
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
        filled: true,
        fillColor: colores.surface,
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          minimumSize: const Size(64, 48),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size(64, 48),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ),
      ),
      snackBarTheme: const SnackBarThemeData(behavior: SnackBarBehavior.floating),
    );
  }
}
