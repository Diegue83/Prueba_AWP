package com.proyecto.servicios.entity.gestopago;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "gestopago_productos")
@Getter
@Setter
public class GestoPagoProduct implements Persistable<Integer> {

    @Id
    @Column(name = "id_producto")
    private Integer idProducto;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "id_cat_tipo_servicio", nullable = false)
    private Integer idCatTipoServicio;

    @Column(name = "tipo_front", nullable = false)
    private Integer tipoFront;

    @Column(name = "precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "has_digito_verificador", nullable = false)
    private Boolean hasDigitoVerificador = false;

    @Column(name = "show_ayuda", nullable = false)
    private Boolean showAyuda = false;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Column(name = "servicio", nullable = false)
    private String servicio;

    @Column(name = "producto", nullable = false)
    private String producto;

    @Column(name = "tipo_referencia")
    private String tipoReferencia;

    @Column(name = "leyenda")
    private String leyenda;

    @Transient
    private boolean nuevo = true;

    @Override
    public Integer getId() {
        return idProducto;
    }

    @Override
    public boolean isNew() {
        return nuevo;
    }

    @PrePersist
    void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = fechaCreacion;
        nuevo = false;
    }

    @PostLoad
    void onLoad() {
        nuevo = false;
    }

    @PreUpdate
    void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
