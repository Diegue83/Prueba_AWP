"""Convierte la exportación del catálogo de GestoPago (CSV) en un script SQL para gestopago_productos.

Uso:
    python scripts/csv_a_sql_catalogo.py <catalogo.csv> <salida.sql>

El SQL inserta o actualiza por id_producto, así que se puede ejecutar más de una vez. Antes de cargar
verifica que Flyway ya creó la tabla: si se cargara antes, la migración V5 la borraría al arrancar el backend.
"""
import csv
import sys
from decimal import Decimal, InvalidOperation

COLUMNAS = ('id_producto', 'id_servicio', 'id_cat_tipo_servicio', 'tipo_front', 'precio',
            'has_digito_verificador', 'show_ayuda', 'servicio', 'producto', 'tipo_referencia', 'leyenda')
LOTE = 200


def texto(valor):
    if valor is None or valor.strip() == '':
        return 'NULL'
    return "'" + valor.strip().replace("'", "''") + "'"


def booleano(valor):
    return 'TRUE' if (valor or '').strip().lower() == 'true' else 'FALSE'


def entero(valor, campo):
    try:
        return int((valor or '').strip())
    except ValueError:
        raise ValueError(f'{campo} no es un entero: {valor!r}')


def leer(ruta):
    productos, errores = {}, []
    with open(ruta, encoding='utf-8-sig', newline='') as archivo:
        for numero, fila in enumerate(csv.DictReader(archivo), start=2):
            try:
                id_producto = entero(fila['idProducto'], 'idProducto')
                precio = Decimal((fila['precio'] or '').strip())
                if precio < 0:
                    raise ValueError('precio negativo')
                if not (fila['servicio'] or '').strip() or not (fila['nombreProducto'] or '').strip():
                    raise ValueError('servicio o nombreProducto vacío')
                productos[id_producto] = (
                    str(id_producto),
                    str(entero(fila['idServicio'], 'idServicio')),
                    str(entero(fila['idCatTipoServicio'], 'idCatTipoServicio')),
                    str(entero(fila['tipoFront'], 'tipoFront')),
                    str(precio.quantize(Decimal('0.01'))),
                    booleano(fila['hasDigitoVerificador']),
                    booleano(fila['showAyuda']),
                    texto(fila['servicio']),
                    texto(fila['nombreProducto']),
                    texto(fila['tipoReferencia']),
                    texto(fila['legend']),
                )
            except (ValueError, InvalidOperation, KeyError) as e:
                errores.append(f'línea {numero}: {e}')
    return productos, errores


def generar(productos):
    filas = sorted(productos.values(), key=lambda f: int(f[0]))
    partes = [
        '-- Catálogo de GestoPago generado por scripts/csv_a_sql_catalogo.py',
        f'-- {len(filas)} productos. Inserta o actualiza por id_producto.',
        '',
        'DO $$',
        'BEGIN',
        "    IF to_regclass('public.gestopago_productos') IS NULL",
        "       OR to_regclass('public.flyway_schema_history') IS NULL THEN",
        "        RAISE EXCEPTION 'Primero arranca el backend contra esta base para que Flyway cree las tablas';",
        '    END IF;',
        'END $$;',
        '',
        'BEGIN;',
    ]
    actualizar = ', '.join(f'{c} = EXCLUDED.{c}' for c in COLUMNAS if c != 'id_producto')
    for inicio in range(0, len(filas), LOTE):
        valores = ',\n'.join('    (' + ', '.join(f) + ')' for f in filas[inicio:inicio + LOTE])
        partes.append(f'INSERT INTO gestopago_productos ({", ".join(COLUMNAS)})\nVALUES\n{valores}\n'
                      f'ON CONFLICT (id_producto) DO UPDATE SET {actualizar}, fecha_actualizacion = NOW();')
    partes += [
        'COMMIT;',
        '',
        '-- Verificación',
        'SELECT COUNT(*) AS productos, COUNT(DISTINCT id_servicio) AS servicios FROM gestopago_productos;',
        '',
    ]
    return '\n'.join(partes)


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        sys.exit(2)
    productos, errores = leer(sys.argv[1])
    for error in errores:
        print('Se omite', error)
    with open(sys.argv[2], 'w', encoding='utf-8', newline='\n') as salida:
        salida.write(generar(productos))
    print(f'{len(productos)} productos escritos en {sys.argv[2]} ({len(errores)} filas omitidas)')


if __name__ == '__main__':
    main()
