package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;

public interface LoginService {

    LoginResponse iniciarSesion(LoginRequest request);
}
