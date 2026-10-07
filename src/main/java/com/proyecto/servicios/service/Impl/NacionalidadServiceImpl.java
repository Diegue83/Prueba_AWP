package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Nacionalidad;
import com.proyecto.servicios.exception.NacionalidadNoEncontradaException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.onboarding.NacionalidadResponse;
import com.proyecto.servicios.repositorys.onboarding.NacionalidadRepository;
import com.proyecto.servicios.service.NacionalidadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class NacionalidadServiceImpl implements NacionalidadService {

    private final NacionalidadRepository nacionalidadRepository;

    public NacionalidadServiceImpl(NacionalidadRepository nacionalidadRepository) {
        this.nacionalidadRepository = nacionalidadRepository;
    }

    @Override
    public List<NacionalidadResponse> consultarTodas() {
        return nacionalidadRepository.buscarTodas().stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Override
    public NacionalidadResponse consultar(String valor) {
        return buscar(valor)
                .map(this::aRespuesta)
                .orElseThrow(() -> new NacionalidadNoEncontradaException(valor));
    }

    @Override
    public String obtenerNombreValido(String valor) {
        return buscar(valor)
                .map(Nacionalidad::getNombre)
                .orElseThrow(() -> new ValidacionException("La información enviada no es válida",
                        List.of("nacionalidad: '" + valor + "' no existe en el catálogo. "
                                + "Consulta las opciones en GET /nacionalidades")));
    }

    private Optional<Nacionalidad> buscar(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        return nacionalidadRepository.buscarPorNombreOClave(valor.trim());
    }

    private NacionalidadResponse aRespuesta(Nacionalidad nacionalidad) {
        return new NacionalidadResponse(nacionalidad.getClave(), nacionalidad.getNombre());
    }
}