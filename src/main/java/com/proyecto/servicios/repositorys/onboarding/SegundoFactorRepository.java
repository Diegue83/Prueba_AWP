package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.SegundoFactor;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SegundoFactorRepository extends JpaRepository<SegundoFactor, Integer> {

    /** Bloquea el registro para que dos peticiones simultáneas no acepten el mismo código. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("SELECT sf FROM SegundoFactor sf WHERE sf.clienteId = :clienteId")
    Optional<SegundoFactor> bloquearPorCliente(@Param("clienteId") Integer clienteId);
}
