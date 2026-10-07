package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Domicilio del cliente. No tiene id propio: usa el id del cliente como
 * llave primaria (@MapsId), porque cada cliente tiene exactamente un domicilio.
 */
@Entity
@Table(name = "domicilios")
@Getter
@Setter
public class Domicilio {

    @Id
    @Column(name = "cliente_id")
    private Integer clienteId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "calle", nullable = false, columnDefinition = "TEXT")
    private String calle;

    @Column(name = "numero_exterior", nullable = false, columnDefinition = "TEXT")
    private String numeroExterior;

    @Column(name = "numero_interior", columnDefinition = "TEXT")
    private String numeroInterior;

    @Column(name = "colonia", nullable = false, columnDefinition = "TEXT")
    private String colonia;

    @Column(name = "municipio", nullable = false, columnDefinition = "TEXT")
    private String municipio;

    @Column(name = "estado", nullable = false, columnDefinition = "TEXT")
    private String estado;

    @Column(name = "codigo_postal", nullable = false, columnDefinition = "TEXT")
    private String codigoPostal;

    @Column(name = "pais", nullable = false, columnDefinition = "TEXT")
    private String pais;
}
