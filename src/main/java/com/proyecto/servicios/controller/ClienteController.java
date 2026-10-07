package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import com.proyecto.servicios.model.onboarding.ClienteUpdateRequest;
import com.proyecto.servicios.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import jakarta.validation.Valid;
/**
 * El controlador solo recibe la petición y delega. No valida ni contiene reglas:
 * de eso se encargan ClienteValidador (formato de los datos) y el servicio (reglas de negocio).
 */
@RestController
@RequestMapping(value = "/clientes", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Clientes", description = "Onboarding de clientes personas físicas")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @Operation(summary = "Registra un cliente, le abre su cuenta y crea su acceso (login)")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> registrar(
            @Valid @RequestBody ClienteRequest request) {

        return new ResponseEntity<>(
                clienteService.registrar(request),
                HttpStatus.CREATED
        );
    }

    @Operation(summary = "Consulta todos los clientes")
    @GetMapping
    public ResponseEntity<List<ClienteResponse>> consultarTodos() {
        return ResponseEntity.ok(clienteService.consultarTodos());
    }

    @Operation(summary = "Consulta un cliente por id")
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> consultarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(clienteService.consultarPorId(id));
    }

    @Operation(summary = "Actualiza los datos modificables (no admite CURP, RFC, contraseña ni número de cuenta)")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> actualizar(@PathVariable Integer id,
                                                      @RequestBody ClienteUpdateRequest request) {
        return ResponseEntity.ok(clienteService.actualizar(id, request));
    }

    @Operation(summary = "Baja lógica: desactiva al cliente e inactiva su cuenta")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBaja(@PathVariable Integer id) {
        clienteService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------- Consultas adicionales -------------------------

    @Operation(summary = "Consulta los clientes activos")
    @GetMapping("/activos")
    public ResponseEntity<List<ClienteResponse>> consultarActivos() {
        return ResponseEntity.ok(clienteService.consultarActivos());
    }

    @Operation(summary = "Busca un cliente por CURP")
    @GetMapping("/curp/{curp}")
    public ResponseEntity<ClienteResponse> consultarPorCurp(@PathVariable String curp) {
        return ResponseEntity.ok(clienteService.consultarPorCurp(curp));
    }

    @Operation(summary = "Busca un cliente por RFC")
    @GetMapping("/rfc/{rfc}")
    public ResponseEntity<ClienteResponse> consultarPorRfc(@PathVariable String rfc) {
        return ResponseEntity.ok(clienteService.consultarPorRfc(rfc));
    }

    // required = false: si falta el parámetro, el mensaje lo arma la función de validación.
    @Operation(summary = "Busca un cliente por correo electrónico")
    @GetMapping("/correo")
    public ResponseEntity<ClienteResponse> consultarPorCorreo(@RequestParam(required = false) String correo) {
        return ResponseEntity.ok(clienteService.consultarPorCorreo(correo));
    }

    @Operation(summary = "Busca al cliente dueño de un número de cuenta")
    @GetMapping("/cuenta/{numeroCuenta}")
    public ResponseEntity<ClienteResponse> consultarPorNumeroCuenta(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(clienteService.consultarPorNumeroCuenta(numeroCuenta));
    }

    @Operation(summary = "Clientes registrados en un rango de fechas (AAAA-MM-DD, ambos días incluidos)")
    @GetMapping("/registrados")
    public ResponseEntity<List<ClienteResponse>> consultarRegistradosEntre(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        return ResponseEntity.ok(clienteService.consultarRegistradosEntre(desde, hasta));
    }
}
