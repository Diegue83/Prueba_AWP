package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.enums.EstatusCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Integer> {

    @Query("SELECT cu FROM Cuenta cu JOIN FETCH cu.cliente WHERE cu.numeroCuenta = :numeroCuenta")
    Optional<Cuenta> buscarPorNumero(@Param("numeroCuenta") String numeroCuenta);

    @Query("SELECT cu FROM Cuenta cu JOIN FETCH cu.cliente WHERE cu.estatus = :estatus ORDER BY cu.id")
    List<Cuenta> buscarPorEstatus(@Param("estatus") EstatusCuenta estatus);

    @Query("SELECT cu FROM Cuenta cu JOIN FETCH cu.cliente WHERE cu.estatus = :estatus AND cu.cliente.id = :clienteId ORDER BY cu.id")
    List<Cuenta> buscarPorEstatusYCliente(@Param("estatus") EstatusCuenta estatus,
                                          @Param("clienteId") Integer clienteId);

    @Query("SELECT cu FROM Cuenta cu JOIN FETCH cu.cliente "
            + "WHERE cu.cliente.id = :clienteId "
            + "AND (:numeroCuenta IS NULL OR cu.numeroCuenta = :numeroCuenta) "
            + "AND (:estatus IS NULL OR cu.estatus = :estatus) "
            + "AND (:desde IS NULL OR cu.fechaApertura >= :desde) "
            + "AND (:hasta IS NULL OR cu.fechaApertura <= :hasta) "
            + "ORDER BY cu.id")
    List<Cuenta> buscarPorCriterios(@Param("clienteId") Integer clienteId,
                                    @Param("numeroCuenta") String numeroCuenta,
                                    @Param("estatus") EstatusCuenta estatus,
                                    @Param("desde") LocalDate desde,
                                    @Param("hasta") LocalDate hasta);

    /** Proyección: trae únicamente la columna saldo, no la cuenta completa. */
    @Query("SELECT cu.saldo FROM Cuenta cu WHERE cu.numeroCuenta = :numeroCuenta")
    Optional<BigDecimal> consultarSaldo(@Param("numeroCuenta") String numeroCuenta);

    /** El número de cuenta lo entrega una secuencia de PostgreSQL: único aunque haya altas simultáneas. */
    @Query(value = "SELECT nextval('seq_numero_cuenta')", nativeQuery = true)
    Long siguienteNumeroCuenta();
}
