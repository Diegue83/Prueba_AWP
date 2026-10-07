package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Nacionalidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NacionalidadRepository extends JpaRepository<Nacionalidad, Short> {

    @Query("SELECT n FROM Nacionalidad n ORDER BY n.nombre")
    List<Nacionalidad> buscarTodas();

    /** Acepta el nombre (sin distinguir mayúsculas) o la clave de 3 letras. */
    @Query("SELECT n FROM Nacionalidad n WHERE LOWER(n.nombre) = LOWER(:texto) OR n.clave = UPPER(:texto)")
    Optional<Nacionalidad> buscarPorNombreOClave(@Param("texto") String texto);
}
