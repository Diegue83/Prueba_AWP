package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.ConsultaClienteRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.service.ClienteService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
/**
 * El controlador solo recibe la petición y delega. No valida ni contiene reglas:
 * Bean Validation y el servicio se encargan de las reglas de los datos y del negocio.
 */
@RestController
@RequestMapping(value = "/clientes", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Clientes", description = "Onboarding de clientes personas físicas")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @Operation(
            summary = "Registra un cliente, le abre su cuenta y crea su acceso (login)",
            description = "Requiere los datos personales, de contacto, domicilio y laborales, además de curp, rfc "
                    + "y contrasena. No envíes id, numeroCuenta, activo ni fechas de registro.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos completos para registrar una persona física.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "Alta de cliente", value = """
                                    {
                                      "nombre": "Diego",
                                      "apellidoPaterno": "Garcia",
                                      "apellidoMaterno": "Lopez",
                                      "fechaNacimiento": "1990-01-01",
                                      "sexo": "H",
                                      "nacionalidad": "Mexicana",
                                      "estadoCivil": "SOLTERO",
                                      "correo": "diego.garcia@example.com",
                                      "telefonoMovil": "5512345678",
                                      "domicilio": {
                                        "calle": "Reforma",
                                        "numeroExterior": "123",
                                        "colonia": "Centro",
                                        "municipio": "Cuauhtemoc",
                                        "estado": "Ciudad de Mexico",
                                        "codigoPostal": "06000",
                                        "pais": "Mexico"
                                      },
                                      "ocupacion": "Ingeniero",
                                      "empresa": "Empresa Ejemplo",
                                      "ingresoMensual": 25000.00,
                                      "curp": "GODE900101HDFRRL09",
                                      "rfc": "GODE900101ABC",
                                      "contrasena": "Segura123"
                                    }
                                    """))))
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> registrar(@Valid @RequestBody ClienteRequest request) {
        return new ResponseEntity<>(clienteService.registrar(request), HttpStatus.CREATED);
    }

    @Operation(
            summary = "Busca clientes combinando los criterios enviados en el cuerpo",
            description = "Todos los criterios son opcionales. Se aceptan id, curp, rfc, correo, numeroCuenta, "
                    + "activo, fechaRegistroDesde y fechaRegistroHasta; los campos de datos personales no aplican.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Envía únicamente los criterios de búsqueda que quieras aplicar.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "Búsqueda por id y periodo", value = """
                                    {
                                      "id": 42,
                                      "activo": true,
                                      "fechaRegistroDesde": "2024-01-01",
                                      "fechaRegistroHasta": "2024-12-31"
                                    }
                                    """))))
    @PostMapping(value = "/consulta", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> consultar(@Valid @RequestBody ConsultaClienteRequest criterios) {
        return ResponseEntity.ok(clienteService.consultar(criterios));
    }

    @Operation(
            summary = "Actualiza los datos modificables",
            description = "Requiere id y todos los datos personales, de contacto, domicilio y laborales. No admite "
                    + "curp, rfc, contrasena, numeroCuenta, activo ni fechas de registro.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Incluye id y los datos completos que se actualizarán.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "Actualización de cliente", value = """
                                    {
                                      "id": 42,
                                      "nombre": "Diego",
                                      "apellidoPaterno": "Garcia",
                                      "apellidoMaterno": "Lopez",
                                      "fechaNacimiento": "1990-01-01",
                                      "sexo": "H",
                                      "nacionalidad": "Mexicana",
                                      "estadoCivil": "SOLTERO",
                                      "correo": "diego.garcia@example.com",
                                      "telefonoMovil": "5512345678",
                                      "domicilio": {
                                        "calle": "Reforma",
                                        "numeroExterior": "123",
                                        "colonia": "Centro",
                                        "municipio": "Cuauhtemoc",
                                        "estado": "Ciudad de Mexico",
                                        "codigoPostal": "06000",
                                        "pais": "Mexico"
                                      },
                                      "ocupacion": "Ingeniero",
                                      "empresa": "Empresa Ejemplo",
                                      "ingresoMensual": 25000.00
                                    }
                                    """))))
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> actualizar(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(clienteService.actualizar(request));
    }

    @Operation(
            summary = "Baja lógica: desactiva al cliente e inactiva su cuenta",
            description = "Envía únicamente el id positivo del cliente que se dará de baja.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Identificador del cliente a dar de baja.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "Baja de cliente", value = """
                                    {
                                      "id": 42
                                    }
                                    """))))
    @DeleteMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> darDeBaja(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(clienteService.darDeBaja(request));
    }
}
