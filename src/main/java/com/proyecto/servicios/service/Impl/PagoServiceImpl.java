package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoCifrador;
import com.proyecto.servicios.client.GestoPagoTransaccionesIntegration;
import com.proyecto.servicios.config.GestoPagoTransaccionesProperties;
import com.proyecto.servicios.config.PagosProperties;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.pagos.Pago;
import com.proyecto.servicios.enums.EstatusPago;
import com.proyecto.servicios.exception.ConfiguracionIncompletaException;
import com.proyecto.servicios.exception.GestoPagoAutenticacionException;
import com.proyecto.servicios.exception.GestoPagoIntegracionException;
import com.proyecto.servicios.exception.IdempotenciaConflictoException;
import com.proyecto.servicios.exception.OperacionEnCursoException;
import com.proyecto.servicios.exception.PagoNoEncontradoException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.gestopago.GestoPagoTxPayload;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResultado;
import com.proyecto.servicios.model.pagos.PagoRequest;
import com.proyecto.servicios.model.pagos.PagoResponse;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.pagos.PagoRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.PagoService;
import com.proyecto.servicios.service.SegundoFactorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Flujo completo de un pago:
 * <ol>
 *   <li>Idempotencia: si la clave ya se usó, se devuelve el mismo resultado sin volver a cobrar.</li>
 *   <li>Validación contra el catálogo y del segundo factor.</li>
 *   <li>Retención del saldo y alta del pago en una transacción con la cuenta bloqueada.</li>
 *   <li>sendTx fuera de la transacción. Si no hay respuesta concluyente, confirmTx con el mismo
 *       payload; si sigue sin saberse, el pago queda en conciliación con el dinero retenido.</li>
 * </ol>
 * Este servicio no es transaccional a propósito: cada paso que toca la base es una
 * transacción corta en {@link PagoTransacciones}.
 */
@Service
@Slf4j
public class PagoServiceImpl implements PagoService {

    private static final String PATRON_CLAVE = "^[A-Za-z0-9_-]{8,64}$";
    private static final DateTimeFormatter FORMATO_HORA_LOCAL = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final char[] ALFABETO_UPC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int LONGITUD_UPC = 15;
    private static final Set<EstatusPago> POR_CONCILIAR = EnumSet.of(EstatusPago.PENDIENTE, EstatusPago.EN_CONCILIACION);

    private final PagoValidador validador;
    private final SegundoFactorService segundoFactorService;
    private final PagoTransacciones transacciones;
    private final PagoRepository pagoRepository;
    private final CuentaRepository cuentaRepository;
    private final GestoPagoTransaccionesIntegration integracion;
    private final GestoPagoCifrador cifrador;
    private final GestoPagoTokenService tokenService;
    private final PagosProperties pagosProperties;
    private final GestoPagoTransaccionesProperties transaccionesProperties;
    private final SecureRandom random = new SecureRandom();

    public PagoServiceImpl(PagoValidador validador,
                           SegundoFactorService segundoFactorService,
                           PagoTransacciones transacciones,
                           PagoRepository pagoRepository,
                           CuentaRepository cuentaRepository,
                           GestoPagoTransaccionesIntegration integracion,
                           GestoPagoCifrador cifrador,
                           GestoPagoTokenService tokenService,
                           PagosProperties pagosProperties,
                           GestoPagoTransaccionesProperties transaccionesProperties) {
        this.validador = validador;
        this.segundoFactorService = segundoFactorService;
        this.transacciones = transacciones;
        this.pagoRepository = pagoRepository;
        this.cuentaRepository = cuentaRepository;
        this.integracion = integracion;
        this.cifrador = cifrador;
        this.tokenService = tokenService;
        this.pagosProperties = pagosProperties;
        this.transaccionesProperties = transaccionesProperties;
    }

    @Override
    public PagoResponse pagar(Integer clienteId, String claveIdempotencia, PagoRequest request) {
        if (claveIdempotencia == null || !claveIdempotencia.matches(PATRON_CLAVE)) {
            throw new ValidacionException("El encabezado Idempotency-Key no es válido",
                    List.of("Idempotency-Key: entre 8 y 64 caracteres, solo letras, números, guion y guion bajo"));
        }
        var previo = pagoRepository.findByClienteIdAndClaveIdempotencia(clienteId, claveIdempotencia);
        if (previo.isPresent()) {
            return repetido(previo.get(), request);
        }

        validador.validar(request);
        cifrador.validarConfiguracion();
        segundoFactorService.verificar(clienteId, request.getCodigo2fa());

        Pago pago;
        try {
            pago = transacciones.registrarYRetener(clienteId, request, claveIdempotencia, generarUpc(), horaLocal());
        } catch (DataIntegrityViolationException e) {
            return pagoRepository.findByClienteIdAndClaveIdempotencia(clienteId, claveIdempotencia)
                    .map(existente -> repetido(existente, request))
                    .orElseThrow(() -> e);
        } catch (PessimisticLockingFailureException e) {
            throw new OperacionEnCursoException();
        }
        log.info("Pago registrado idPago={} clienteId={} idProducto={} monto={} upc={}",
                pago.getId(), clienteId, pago.getIdProducto(), pago.getMonto(), pago.getUpc());
        try {
            return aRespuesta(procesar(pago));
        } catch (GestoPagoIntegracionException e) {
            throw e;
        } catch (RuntimeException e) {
            // El dinero ya está retenido y el estado real lo resolverá la conciliación.
            log.error("Error inesperado procesando idPago={}; queda para conciliación ({})",
                    pago.getId(), e.getClass().getSimpleName());
            return aRespuesta(pagoRepository.findById(pago.getId()).orElse(pago));
        }
    }

    @Override
    public PagoResponse consultar(Integer clienteId, Long idPago) {
        return pagoRepository.findByIdAndClienteId(idPago, clienteId)
                .map(this::aRespuesta)
                .orElseThrow(() -> new PagoNoEncontradoException(idPago));
    }

    @Override
    public int conciliarPendientes() {
        try {
            cifrador.validarConfiguracion();
        } catch (ConfiguracionIncompletaException e) {
            return 0;
        }
        LocalDateTime antesDe = LocalDateTime.now().minusMinutes(pagosProperties.getConciliacionMinutosPendiente());
        List<Long> ids = pagoRepository.buscarPorConciliar(POR_CONCILIAR, antesDe,
                PageRequest.of(0, pagosProperties.getConciliacionLote()));
        ids.forEach(this::conciliar);
        return ids.size();
    }

    private Pago procesar(Pago pago) {
        GestoPagoTxPayload payload = payload(pago);
        GestoPagoTxResultado envio;
        try {
            envio = conToken(token -> integracion.enviar(payload, token));
        } catch (GestoPagoIntegracionException e) {
            // Sin token válido la petición nunca llegó a procesarse: es seguro devolver el dinero.
            transacciones.rechazarYReembolsar(pago.getId(), new GestoPagoTxResultado(
                    GestoPagoTxResultado.Tipo.RECHAZADO, e.getClave(), "No se pudo enviar al proveedor", null, null));
            throw e;
        }
        if (envio.esExitoso()) {
            return transacciones.aplicar(pago.getId(), envio);
        }
        if (envio.esRechazado()) {
            return transacciones.rechazarYReembolsar(pago.getId(), envio);
        }

        log.warn("sendTx sin resultado concluyente para upc={}; se consulta confirmTx", pago.getUpc());
        GestoPagoTxResultado confirmacion = confirmar(payload);
        if (confirmacion.esExitoso()) {
            return transacciones.aplicar(pago.getId(), confirmacion);
        }
        if (confirmacionDescartaElCobro(confirmacion)) {
            return transacciones.rechazarYReembolsar(pago.getId(), confirmacion);
        }
        return transacciones.enviarAConciliacion(pago.getId());
    }

    private void conciliar(Long idPago) {
        try {
            Pago pago = pagoRepository.findById(idPago).orElseThrow(() -> new PagoNoEncontradoException(idPago));
            GestoPagoTxResultado confirmacion = confirmar(payload(pago));
            if (confirmacion.esExitoso()) {
                transacciones.aplicar(idPago, confirmacion);
            } else if (confirmacionDescartaElCobro(confirmacion)) {
                transacciones.rechazarYReembolsar(idPago, confirmacion);
            } else {
                transacciones.registrarIntentoConciliacion(idPago, pagosProperties.getConciliacionMaxIntentos());
            }
        } catch (RuntimeException e) {
            log.error("No se pudo conciliar el pago idPago={}: {}", idPago, e.getClass().getSimpleName());
        }
    }

    private GestoPagoTxResultado confirmar(GestoPagoTxPayload payload) {
        try {
            return conToken(token -> integracion.confirmar(payload, token));
        } catch (GestoPagoIntegracionException e) {
            return GestoPagoTxResultado.indeterminado(e.getClave());
        }
    }

    /**
     * Un "no" de confirmTx solo libera el dinero cuando el código está declarado como
     * "operación no aplicada". Cualquier otro código deja el pago en conciliación.
     */
    private boolean confirmacionDescartaElCobro(GestoPagoTxResultado confirmacion) {
        return confirmacion.esRechazado()
                && transaccionesProperties.getCodigosNoAplicada().contains(confirmacion.codigo());
    }

    /** Si el proveedor rechaza el token guardado, se pide uno nuevo y se reintenta una vez. */
    private GestoPagoTxResultado conToken(Function<String, GestoPagoTxResultado> llamada) {
        try {
            return llamada.apply(tokenService.obtenerTokenVigente().getToken());
        } catch (GestoPagoAutenticacionException e) {
            return llamada.apply(tokenService.solicitarNuevoToken().getToken());
        }
    }

    private PagoResponse repetido(Pago pago, PagoRequest request) {
        Integer cuentaId = cuentaRepository.buscarPorNumero(request.getNumeroCuenta()).map(Cuenta::getId).orElse(null);
        if (!pago.mismaSolicitud(cuentaId, request.getIdProducto(), request.getMonto(),
                request.getTelefono(), request.getReferencia())) {
            throw new IdempotenciaConflictoException();
        }
        log.info("Solicitud repetida con la misma Idempotency-Key; se devuelve el pago idPago={}", pago.getId());
        return aRespuesta(pago);
    }

    private GestoPagoTxPayload payload(Pago pago) {
        return new GestoPagoTxPayload(pago.getIdServicio(), pago.getIdProducto(), pago.getReferencia(),
                pago.getMonto(), pago.getTelefono(), pago.getHoraLocal(), pago.getUpc(),
                transaccionesProperties.getUnidad());
    }

    private PagoResponse aRespuesta(Pago pago) {
        return new PagoResponse(pago.getId(), pago.getEstatus(), pago.getIdProducto(), pago.getMonto(),
                pago.getNumAutorizacion(), pago.getCodigoRespuesta(), mensaje(pago.getEstatus()),
                pago.getFechaActualizacion());
    }

    private String mensaje(EstatusPago estatus) {
        return switch (estatus) {
            case APLICADO -> "Pago realizado correctamente";
            case RECHAZADO -> "El proveedor rechazó el pago; el monto se devolvió a tu cuenta";
            case PENDIENTE, EN_CONCILIACION ->
                    "El pago está en verificación con el proveedor; el monto queda retenido hasta confirmarlo";
            case REVISION_MANUAL ->
                    "El pago requiere revisión; el monto queda retenido. Contacta a soporte con el número de pago";
        };
    }

    private String generarUpc() {
        char[] upc = new char[LONGITUD_UPC];
        for (int i = 0; i < upc.length; i++) {
            upc[i] = ALFABETO_UPC[random.nextInt(ALFABETO_UPC.length)];
        }
        return new String(upc);
    }

    private String horaLocal() {
        return ZonedDateTime.now(ZoneId.of(pagosProperties.getZonaHoraria())).format(FORMATO_HORA_LOCAL);
    }
}
