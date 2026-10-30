package com.proyecto.servicios.repositorys.pagos;

import com.proyecto.servicios.entity.pagos.Pago;
import com.proyecto.servicios.enums.EstatusPago;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByClienteIdAndClaveIdempotencia(Integer clienteId, String claveIdempotencia);

    Optional<Pago> findByIdAndClienteId(Long id, Integer clienteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("SELECT p FROM Pago p WHERE p.id = :id")
    Optional<Pago> bloquearPorId(@Param("id") Long id);

    @Query("SELECT p.id FROM Pago p WHERE p.estatus IN :estatus AND p.fechaCreacion < :antesDe ORDER BY p.fechaCreacion")
    List<Long> buscarPorConciliar(@Param("estatus") Collection<EstatusPago> estatus,
                                  @Param("antesDe") LocalDateTime antesDe,
                                  Pageable pagina);
}
