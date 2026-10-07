package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Catálogo de nacionalidades. Es de solo lectura: se llena con la migración V3. */
@Entity
@Table(name = "nacionalidades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Nacionalidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Column(name = "clave", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String clave;

    @Column(name = "nombre", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String nombre;
}