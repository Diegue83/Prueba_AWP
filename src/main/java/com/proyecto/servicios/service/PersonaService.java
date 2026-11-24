package com.proyecto.servicios.service;

import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonasRequest;

public interface PersonaService {
    GenericResponse creaPersona(PersonasRequest personasRequest);
    GenericResponse eliminaPersona(PersonasRequest personasRequest);
    GenericResponse actualizaPersona(PersonasRequest personasRequest);
}
