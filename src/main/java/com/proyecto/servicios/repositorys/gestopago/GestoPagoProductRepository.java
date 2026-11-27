package com.proyecto.servicios.repositorys.gestopago;

import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GestoPagoProductRepository extends JpaRepository<GestoPagoProduct, Integer> {

    List<GestoPagoProduct> findAllByOrderByServicioAscPrecioAscIdProductoAsc();

    Optional<GestoPagoProduct> findFirstByIdServicioAndTipoFrontOrderByIdProductoAsc(Integer idServicio, Integer tipoFront);
}
