package com.proyecto.servicios.repositorys.gestopago;

import com.proyecto.servicios.entity.gestopago.GestoPagoBitacora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GestoPagoBitacoraRepository extends JpaRepository<GestoPagoBitacora, Long> {

    List<GestoPagoBitacora> findByUpcOrderByIdAsc(String upc);
}
