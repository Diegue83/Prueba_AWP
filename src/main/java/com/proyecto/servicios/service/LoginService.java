package com.proyecto.servicios.service;

import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.onboarding.LoginRequest;

public interface LoginService {

    GenericResponse iniciarSesion(LoginRequest request);
}
