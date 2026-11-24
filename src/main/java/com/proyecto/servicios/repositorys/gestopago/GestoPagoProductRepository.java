package com.proyecto.servicios.repositorys.gestopago;

import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GestoPagoProductRepository extends JpaRepository<GestoPagoProduct, Integer> {

    List<GestoPagoProduct> findAllByOrderByServicioAscPrecioAscIdProductoAsc();
}
