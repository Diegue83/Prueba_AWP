package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Nacionalidad;
import com.proyecto.servicios.exception.NacionalidadNoEncontradaException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.onboarding.NacionalidadResponse;
import com.proyecto.servicios.model.onboarding.NacionalidadRequest;
import com.proyecto.servicios.repositorys.onboarding.NacionalidadRepository;
import com.proyecto.servicios.service.NacionalidadService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class NacionalidadServiceImpl implements NacionalidadService {

    private final NacionalidadRepository nacionalidadRepository;

    public NacionalidadServiceImpl(NacionalidadRepository nacionalidadRepository) {
        this.nacionalidadRepository = nacionalidadRepository;
    }

    @Override
    public GenericResponse consultarTodas() {
        List<NacionalidadResponse> nacionalidades = nacionalidadRepository.buscarTodas().stream()
                .map(this::aRespuesta)
                .toList();
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Consulta de nacionalidades realizada correctamente");
        respuesta.setDatos(nacionalidades);
        return respuesta;
    }

    @Override
    public GenericResponse consultar(NacionalidadRequest request) {
        String valor = request.getValor();
        NacionalidadResponse nacionalidad = buscar(valor)
                .map(this::aRespuesta)
                .orElseThrow(() -> new NacionalidadNoEncontradaException(valor));
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Nacionalidad encontrada");
        respuesta.setDatos(nacionalidad);
        return respuesta;
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