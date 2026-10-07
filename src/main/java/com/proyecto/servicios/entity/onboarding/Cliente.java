package com.proyecto.servicios.entity.onboarding;

import com.proyecto.servicios.entity.onboarding.converter.EstadoCivilConverter;
import com.proyecto.servicios.entity.onboarding.converter.SexoConverter;
import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.Sexo;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cliente persona física. Es la raíz del modelo: su domicilio, su cuenta y su
 * acceso (login) cuelgan de él y se guardan en cascada con un solo save().
 */
@Entity
@Table(name = "clientes")
@Getter
@Setter
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Integer id;

    // ---- Datos personales ----
    @Column(name = "nombre", nullable = false, columnDefinition = "TEXT")
    private String nombre;

    @Column(name = "segundo_nombre", columnDefinition = "TEXT")
    private String segundoNombre;

    @Column(name = "apellido_paterno", nullable = false, columnDefinition = "TEXT")
    private String apellidoPaterno;

    @Column(name = "apellido_materno", nullable = false, columnDefinition = "TEXT")
    private String apellidoMaterno;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    // updatable = false: Hibernate nunca incluye estas columnas en un UPDATE.
    @Column(name = "curp", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String curp;

    @Column(name = "rfc", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String rfc;

    @Convert(converter = SexoConverter.class)
    @Column(name = "sexo", nullable = false, columnDefinition = "TEXT")
    private Sexo sexo;

    @Column(name = "nacionalidad", nullable = false, columnDefinition = "TEXT")
    private String nacionalidad;

    @Convert(converter = EstadoCivilConverter.class)
    @Column(name = "estado_civil", nullable = false, columnDefinition = "TEXT")
    private EstadoCivil estadoCivil;

    // ---- Datos de contacto ----
    @Column(name = "correo", nullable = false, columnDefinition = "TEXT")
    private String correo;

    @Column(name = "telefono_movil", nullable = false, columnDefinition = "TEXT")
    private String telefonoMovil;

    @Column(name = "telefono_alterno", columnDefinition = "TEXT")
    private String telefonoAlterno;

    // ---- Información laboral ----
    @Column(name = "ocupacion", nullable = false, columnDefinition = "TEXT")
    private String ocupacion;

    @Column(name = "empresa", nullable = false, columnDefinition = "TEXT")
    private String empresa;

    @Column(name = "ingreso_mensual", nullable = false, precision = 12, scale = 2)
    private BigDecimal ingresoMensual;

    // ---- Control ----
    @Column(name = "activo", nullable = false)
    @Setter(AccessLevel.NONE)
    private boolean activo = true;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private LocalDate fechaRegistro;

    @Column(name = "fecha_actualizacion", nullable = false)
    @Setter(AccessLevel.NONE)
    private LocalDate fechaActualizacion;

    // ---- Relaciones ----
    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Setter(AccessLevel.NONE)
    private Domicilio domicilio;

    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Setter(AccessLevel.NONE)
    private Cuenta cuenta;

    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Setter(AccessLevel.NONE)
    private Login acceso;

    // ================= Comportamiento del cliente =================

    /**
     * Enlaza el domicilio en los dos sentidos (cliente -> domicilio y domicilio -> cliente).
     */
    public void asignarDomicilio(Domicilio nuevoDomicilio) {
        nuevoDomicilio.setCliente(this);
        this.domicilio = nuevoDomicilio;
    }

    /**
     * Regla: al registrarse, el cliente recibe una cuenta ACTIVA con el saldo inicial del sistema.
     */
    public void abrirCuenta(String numeroCuenta, BigDecimal saldoInicial) {
        if (!activo) {
            throw new IllegalStateException("Solo los clientes activos pueden tener cuentas activas");
        }
        this.cuenta = Cuenta.abrir(this, numeroCuenta, saldoInicial);
    }

    /**
     * Crea las credenciales de acceso. Recibe la contraseña YA cifrada (hash), nunca en claro.
     */
    public void crearAcceso(String contrasenaCifrada) {
        this.acceso = Login.crear(this, contrasenaCifrada);
    }

    /**
     * Baja lógica. Regla: solo los clientes activos pueden tener cuentas activas,
     * así que la cuenta se inactiva junto con el cliente.
     */
    public void darDeBaja() {
        this.activo = false;
        if (cuenta != null) {
            cuenta.inactivar();
        }
    }

    @PrePersist
    void alCrear() {
        fechaRegistro = LocalDate.now();
        fechaActualizacion = fechaRegistro;
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = LocalDate.now();
    }
}
