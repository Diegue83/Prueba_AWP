package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Segundo factor TOTP de un cliente. Nace inactivo al registrarse y solo se activa cuando
 * el cliente demuestra que su app autenticadora genera códigos correctos.
 */
@Entity
@Table(name = "segundo_factor")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SegundoFactor {

    @Id
    @Column(name = "cliente_id")
    private Integer clienteId;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "ultimo_paso")
    private Long ultimoPaso;

    @Column(name = "intentos_fallidos", nullable = false)
    private short intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_activacion")
    private LocalDateTime fechaActivacion;

    @Column(name = "secreto_cifrado", nullable = false, columnDefinition = "TEXT")
    private String secretoCifrado;

    public static SegundoFactor registrar(Integer clienteId, String secretoCifrado) {
        SegundoFactor factor = new SegundoFactor();
        factor.clienteId = clienteId;
        factor.fechaCreacion = LocalDateTime.now();
        factor.reemplazarSecreto(secretoCifrado);
        return factor;
    }

    /** Un registro que nunca se activó puede volver a generarse (por ejemplo, si se perdió el QR). */
    public void reemplazarSecreto(String secretoCifrado) {
        if (activo) {
            throw new IllegalStateException("El segundo factor ya está activo");
        }
        this.secretoCifrado = secretoCifrado;
        this.ultimoPaso = null;
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
    }

    public boolean estaBloqueado(LocalDateTime ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    /** Un mismo código (paso de 30 s) solo se acepta una vez, aunque siga vigente. */
    public boolean pasoYaUsado(long paso) {
        return ultimoPaso != null && paso <= ultimoPaso;
    }

    public void registrarExito(long paso) {
        ultimoPaso = paso;
        intentosFallidos = 0;
        bloqueadoHasta = null;
    }

    public void registrarFallo(int maxIntentos, LocalDateTime bloquearHasta) {
        intentosFallidos++;
        if (intentosFallidos >= maxIntentos) {
            bloqueadoHasta = bloquearHasta;
            intentosFallidos = 0;
        }
    }

    public void activar(long paso) {
        registrarExito(paso);
        activo = true;
        fechaActivacion = LocalDateTime.now();
    }
}
