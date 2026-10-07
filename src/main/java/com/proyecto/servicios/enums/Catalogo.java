package com.proyecto.servicios.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Contrato común de los catálogos: cada valor del enum tiene un código de un
 * solo carácter, que es lo que se guarda en la base de datos.
 */
public interface Catalogo {

    String getCodigo();

    /**
     * Busca un valor del catálogo por su nombre (MUJER) o por su código (M),
     * sin distinguir mayúsculas de minúsculas.
     */
    static <E extends Enum<E> & Catalogo> Optional<E> buscar(Class<E> tipo, String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        String buscado = texto.trim();
        return Arrays.stream(tipo.getEnumConstants())
                .filter(valor -> valor.name().equalsIgnoreCase(buscado) || valor.getCodigo().equalsIgnoreCase(buscado))
                .findFirst();
    }

    /** Igual que buscar(), pero falla si el texto no corresponde a ningún valor. */
    static <E extends Enum<E> & Catalogo> E leer(Class<E> tipo, String texto) {
        return buscar(tipo, texto)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Valor desconocido para " + tipo.getSimpleName() + ": " + texto));
    }
}
