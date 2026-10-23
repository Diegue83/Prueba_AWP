package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Personas;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.repositorys.sf.PersonasRepository;
import com.proyecto.servicios.service.PersonaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class PersonasServiceImpl implements PersonaService {
    @Autowired
    private PersonasRepository personasRepository;
    @Override
    public GenericResponse creaPersona(PersonasRequest personasRequest) {
        PersonaResponse person=new PersonaResponse();
     Personas persona=new Personas();
     persona.setNombre(personasRequest.getNombre());
     persona.setApellidoMaterno(personasRequest.getApellidoMaterno());
     persona.setApellidoP(personasRequest.getApellidoP());
     personasRepository.save(persona);
     BeanUtils.copyProperties(persona,person);

     GenericResponse respuesta = new GenericResponse();
     respuesta.setCodigo(0);
     respuesta.setMensaje("Exito");
     respuesta.setDatos(person);
     return respuesta;
    }

    @Override
    public GenericResponse eliminaPersona(PersonasRequest personasRequest) {
        Optional<Personas> existePersona=personasRepository.findByNombre(personasRequest.getNombre());
        String mensaje;
        int codigo;
        if(existePersona.isPresent()){
            Personas personaElimina=existePersona.get();
            personasRepository.delete(personaElimina);
            codigo = 0;
            mensaje = "La persona ha sido eliminada correctamente";
        }else{
            codigo = 1;
            mensaje = "La persona no existe";
        }
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(codigo);
        respuesta.setMensaje(mensaje);
        respuesta.setDatos(null);
        return respuesta;
    }

    @Override
    public GenericResponse actualizaPersona(PersonasRequest personasRequest) {
        Optional<Personas> existePersona=personasRepository.findByNombre(personasRequest.getNombre());
        String mensaje;
        int codigo;
        if(existePersona.isPresent()){
            Personas personaActualiza=existePersona.get();
            personaActualiza.setApellidoP(personasRequest.getApellidoP());
            personaActualiza.setApellidoMaterno(personasRequest.getApellidoMaterno());
            personasRepository.save(personaActualiza);
            codigo = 0;
            mensaje = "La persona ha sido actualizada correctamente";
        }else{
            codigo = 1;
            mensaje = "La persona no existe";
        }
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(codigo);
        respuesta.setMensaje(mensaje);
        respuesta.setDatos(null);
        return respuesta;
    }
}
