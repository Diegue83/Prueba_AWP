package com.proyecto.servicios.entity.onboarding;

import com.proyecto.servicios.entity.onboarding.converter.EstatusCuentaConverter;
import com.proyecto.servicios.enums.EstatusCuenta;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Cuenta bancaria. No tiene setters públicos: solo se crea con abrir()
 * y solo cambia con inactivar(), así nadie puede dejarla en un estado inválido.
 */
@Entity
@Table(name = "cuentas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "numero_cuenta", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String numeroCuenta;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    @Column(name = "saldo", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldo;

    @Convert(converter = EstatusCuentaConverter.class)
    @Column(name = "estatus", nullable = false, columnDefinition = "TEXT")
    private EstatusCuenta estatus;

    @Column(name = "fecha_apertura", nullable = false, updatable = false)
    private LocalDate fechaApertura;

    static Cuenta abrir(Cliente cliente, String numeroCuenta, BigDecimal saldoInicial) {
        if (saldoInicial == null || saldoInicial.signum() < 0) {
            throw new IllegalArgumentException("El saldo inicial no puede ser negativo");
        }
        Cuenta cuenta = new Cuenta();
        cuenta.cliente = cliente;
        cuenta.numeroCuenta = numeroCuenta;
        cuenta.saldo = saldoInicial.setScale(2, RoundingMode.HALF_UP);   // siempre 2 decimales
        cuenta.estatus = EstatusCuenta.ACTIVA;
        return cuenta;
    }

    void inactivar() {
        this.estatus = EstatusCuenta.INACTIVA;
    }

    /** Aparta el monto de un pago. Solo debe llamarse con la fila bloqueada (SELECT ... FOR UPDATE). */
    public void retener(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto a retener debe ser mayor que cero");
        }
        if (!tieneSaldoPara(monto)) {
            throw new IllegalStateException("Saldo insuficiente");
        }
        saldo = saldo.subtract(monto).setScale(2, RoundingMode.HALF_UP);
    }

    /** Devuelve un monto retenido cuando el proveedor rechazó el pago. */
    public void reembolsar(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto a reembolsar debe ser mayor que cero");
        }
        saldo = saldo.add(monto).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean tieneSaldoPara(BigDecimal monto) {
        return saldo.compareTo(monto) >= 0;
    }

    public boolean estaActiva() {
        return estatus == EstatusCuenta.ACTIVA;
    }

    @PrePersist
    void alCrear() {
        fechaApertura = LocalDate.now();
    }
}
