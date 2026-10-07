package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Consultas de clientes escritas en JPQL. Todas usan JOIN FETCH para traer
 * cliente + domicilio + cuenta + login en UN solo SELECT (sin el problema N+1).
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    String SELECCION_COMPLETA = "SELECT c FROM Cliente c JOIN FETCH c.domicilio JOIN FETCH c.cuenta cu JOIN FETCH c.acceso ";

    @Query(SELECCION_COMPLETA + "ORDER BY c.id")
    List<Cliente> buscarTodos();

    @Query(SELECCION_COMPLETA + "WHERE c.id = :id")
    Optional<Cliente> buscarPorId(@Param("id") Integer id);

    @Query(SELECCION_COMPLETA + "WHERE c.curp = :curp")
    Optional<Cliente> buscarPorCurp(@Param("curp") String curp);

    @Query(SELECCION_COMPLETA + "WHERE c.rfc = :rfc")
    Optional<Cliente> buscarPorRfc(@Param("rfc") String rfc);

    @Query(SELECCION_COMPLETA + "WHERE c.correo = :correo")
    Optional<Cliente> buscarPorCorreo(@Param("correo") String correo);

    @Query(SELECCION_COMPLETA + "WHERE cu.numeroCuenta = :numeroCuenta")
    Optional<Cliente> buscarPorNumeroCuenta(@Param("numeroCuenta") String numeroCuenta);

    @Query(SELECCION_COMPLETA + "WHERE c.activo = true ORDER BY c.id")
    List<Cliente> buscarActivos();

    /** Ambos días incluidos. */
    @Query(SELECCION_COMPLETA + "WHERE c.fechaRegistro BETWEEN :desde AND :hasta ORDER BY c.fechaRegistro, c.id")
    List<Cliente> buscarRegistradosEntre(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    // ---- Verificaciones de duplicados: solo preguntan "¿existe?", no traen la fila ----

    @Query("SELECT COUNT(c) > 0 FROM Cliente c WHERE c.curp = :curp")
    boolean existeCurp(@Param("curp") String curp);

    @Query("SELECT COUNT(c) > 0 FROM Cliente c WHERE c.rfc = :rfc")
    boolean existeRfc(@Param("rfc") String rfc);

    @Query("SELECT COUNT(c) > 0 FROM Cliente c WHERE c.correo = :correo")
    boolean existeCorreo(@Param("correo") String correo);

    /** Para la actualización: ¿otro cliente (distinto de :id) ya usa este correo? */
    @Query("SELECT COUNT(c) > 0 FROM Cliente c WHERE c.correo = :correo AND c.id <> :id")
    boolean existeCorreoEnOtroCliente(@Param("correo") String correo, @Param("id") Integer id);
}
