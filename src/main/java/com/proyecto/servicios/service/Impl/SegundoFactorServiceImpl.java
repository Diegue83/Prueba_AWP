package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.SegundoFactorProperties;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.SegundoFactor;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.SegundoFactorBloqueadoException;
import com.proyecto.servicios.exception.SegundoFactorInvalidoException;
import com.proyecto.servicios.exception.SegundoFactorNoConfiguradoException;
import com.proyecto.servicios.exception.SegundoFactorYaActivoException;
import com.proyecto.servicios.model.pagos.SegundoFactorRegistroResponse;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.SegundoFactorRepository;
import com.proyecto.servicios.security.CifradoSecretos;
import com.proyecto.servicios.security.Totp;
import com.proyecto.servicios.service.SegundoFactorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.OptionalLong;

@Service
@Slf4j
public class SegundoFactorServiceImpl implements SegundoFactorService {

    private final SegundoFactorRepository segundoFactorRepository;
    private final ClienteRepository clienteRepository;
    private final CifradoSecretos cifrado;
    private final Totp totp;
    private final SegundoFactorProperties properties;

    public SegundoFactorServiceImpl(SegundoFactorRepository segundoFactorRepository,
                                    ClienteRepository clienteRepository,
                                    CifradoSecretos cifrado,
                                    Totp totp,
                                    SegundoFactorProperties properties) {
        this.segundoFactorRepository = segundoFactorRepository;
        this.clienteRepository = clienteRepository;
        this.cifrado = cifrado;
        this.totp = totp;
        this.properties = properties;
    }

    @Override
    @Transactional
    public SegundoFactorRegistroResponse registrar(Integer clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ClienteNoEncontradoException("id " + clienteId));
        byte[] secreto = totp.generarSecreto();
        String secretoCifrado = cifrado.cifrar(secreto);

        SegundoFactor factor = segundoFactorRepository.bloquearPorCliente(clienteId)
                .map(existente -> {
                    if (existente.isActivo()) {
                        throw new SegundoFactorYaActivoException();
                    }
                    existente.reemplazarSecreto(secretoCifrado);
                    return existente;
                })
                .orElseGet(() -> SegundoFactor.registrar(clienteId, secretoCifrado));
        segundoFactorRepository.save(factor);
        log.info("Segundo factor registrado (pendiente de activar) para clienteId={}", clienteId);

        String secretoBase32 = totp.aBase32(secreto);
        return new SegundoFactorRegistroResponse(secretoBase32, uriOtpauth(cliente.getCorreo(), secretoBase32));
    }

    @Override
    @Transactional(noRollbackFor = {SegundoFactorInvalidoException.class, SegundoFactorBloqueadoException.class})
    public void activar(Integer clienteId, String codigo) {
        SegundoFactor factor = segundoFactorRepository.bloquearPorCliente(clienteId)
                .orElseThrow(SegundoFactorNoConfiguradoException::new);
        if (factor.isActivo()) {
            throw new SegundoFactorYaActivoException();
        }
        factor.activar(validarCodigo(factor, codigo));
        log.info("Segundo factor activado para clienteId={}", clienteId);
    }

    /**
     * Los intentos fallidos se guardan aunque se lance la excepción (noRollbackFor);
     * de otro modo el bloqueo por fuerza bruta nunca se alcanzaría.
     */
    @Override
    @Transactional(noRollbackFor = {SegundoFactorInvalidoException.class, SegundoFactorBloqueadoException.class})
    public void verificar(Integer clienteId, String codigo) {
        SegundoFactor factor = segundoFactorRepository.bloquearPorCliente(clienteId)
                .filter(SegundoFactor::isActivo)
                .orElseThrow(SegundoFactorNoConfiguradoException::new);
        factor.registrarExito(validarCodigo(factor, codigo));
    }

    private long validarCodigo(SegundoFactor factor, String codigo) {
        LocalDateTime ahora = LocalDateTime.now();
        if (factor.estaBloqueado(ahora)) {
            throw new SegundoFactorBloqueadoException();
        }
        OptionalLong paso = totp.verificar(cifrado.descifrar(factor.getSecretoCifrado()), codigo,
                properties.getVentanaPasos());
        if (paso.isEmpty() || factor.pasoYaUsado(paso.getAsLong())) {
            factor.registrarFallo(properties.getMaxIntentos(), ahora.plusMinutes(properties.getMinutosBloqueo()));
            log.warn("Código 2FA rechazado para clienteId={}", factor.getClienteId());
            if (factor.estaBloqueado(ahora)) {
                throw new SegundoFactorBloqueadoException();
            }
            throw new SegundoFactorInvalidoException();
        }
        return paso.getAsLong();
    }

    private String uriOtpauth(String cuenta, String secretoBase32) {
        String emisor = properties.getEmisor();
        String etiqueta = codificar(emisor + ":" + cuenta);
        return "otpauth://totp/" + etiqueta + "?secret=" + secretoBase32
                + "&issuer=" + codificar(emisor) + "&algorithm=SHA1&digits=6&period=30";
    }

    private String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
