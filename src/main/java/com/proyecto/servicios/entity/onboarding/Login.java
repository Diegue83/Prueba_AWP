package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Credenciales de acceso del cliente, en su propia tabla. Usa el id del cliente
 * como llave primaria (@MapsId). El correo con el que se entra está en Cliente;
 * aquí solo vive la contraseña cifrada.
 */
@Entity
@Table(name = "login")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Login {

    @Id
    @Column(name = "cliente_id")
    private Integer clienteId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    /** Hash BCrypt de la contraseña (60 caracteres). Nunca se guarda ni se devuelve en claro. */
    @Column(name = "contrasena", nullable = false, columnDefinition = "TEXT")
    private String contrasena;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDate fechaCreacion;

    @Column(name = "ultimo_acceso")
    private LocalDate ultimoAcceso;

    static Login crear(Cliente cliente, String contrasenaCifrada) {
        Login login = new Login();
        login.cliente = cliente;
        login.contrasena = contrasenaCifrada;
        return login;
    }

    public void registrarAcceso() {
        this.ultimoAcceso = LocalDate.now();
    }

    @PrePersist
    void alCrear() {
        fechaCreacion = LocalDate.now();
    }
}
