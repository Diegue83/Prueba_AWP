package com.proyecto.servicios.repositorys.onboarding;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * Filtros de la consulta de clientes. clienteId es obligatorio (limita la búsqueda al titular);
 * el resto es opcional y un valor nulo significa "sin filtro".
 */
@Getter
@Builder
public class CriteriosBusquedaCliente {

    private final Integer clienteId;
    private final Integer id;
    private final String curp;
    private final String rfc;
    private final String correo;
    private final String numeroCuenta;
    private final Boolean activo;
    private final LocalDate fechaRegistroDesde;
    private final LocalDate fechaRegistroHasta;
}
