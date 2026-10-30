package com.proyecto.servicios.integration;

import com.proyecto.servicios.entity.pagos.Pago;
import com.proyecto.servicios.enums.EstatusPago;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResultado;
import com.proyecto.servicios.model.pagos.PagoRequest;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.pagos.PagoRepository;
import com.proyecto.servicios.service.Impl.PagoTransacciones;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lanza peticiones simultáneas contra una base real (H2 en modo PostgreSQL, con bloqueos de fila)
 * para comprobar que el SELECT ... FOR UPDATE y las restricciones UNIQUE evitan sobregiros y
 * cargos duplicados.
 */
@SpringJUnitConfig(PagoTransaccionesConcurrenciaTest.Config.class)
class PagoTransaccionesConcurrenciaTest {

    private static final int HILOS = 12;

    @Autowired
    private PagoTransacciones transacciones;
    @Autowired
    private PagoRepository pagoRepository;
    @Autowired
    private CuentaRepository cuentaRepository;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM pagos");
        jdbc.update("DELETE FROM cuentas");
        jdbc.update("DELETE FROM gestopago_productos");
        jdbc.update("DELETE FROM clientes");
        jdbc.update("INSERT INTO clientes (id, nombre, apellido_paterno, apellido_materno, fecha_nacimiento, curp, rfc,"
                + " sexo, nacionalidad, estado_civil, correo, telefono_movil, ocupacion, empresa, ingreso_mensual,"
                + " activo, fecha_registro, fecha_actualizacion) VALUES (1, 'Ana', 'Lopez', 'Diaz', DATE '1990-01-01',"
                + " 'LODA900101MDFPZN01', 'LODA900101AB1', 'M', 'Mexicana', 'S', 'ana@example.com', '5512345678',"
                + " 'Analista', 'ACME', 20000, TRUE, CURRENT_DATE, CURRENT_DATE)");
        jdbc.update("INSERT INTO cuentas (id, cliente_id, numero_cuenta, saldo, estatus, fecha_apertura)"
                + " VALUES (1, 1, '1000000001', 500.00, 'A', CURRENT_DATE)");
        jdbc.update("INSERT INTO gestopago_productos (id_producto, id_servicio, id_cat_tipo_servicio, tipo_front, precio,"
                + " has_digito_verificador, show_ayuda, fecha_creacion, fecha_actualizacion, servicio, producto)"
                + " VALUES (14302, 2284, 13, 1, 100.00, FALSE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ABIB', 'ABIB 100')");
    }

    @Test
    void conPagosSimultaneosNuncaSeRetieneMasQueElSaldo() throws Exception {
        List<Callable<Object>> tareas = new ArrayList<>();
        for (int i = 0; i < HILOS; i++) {
            String clave = "clave-concurrente-" + i;
            String upc = String.format("UPC%012d", i);
            tareas.add(() -> transacciones.registrarYRetener(1, solicitud(), clave, upc, "20261030101500"));
        }

        List<Object> resultados = ejecutarALaVez(tareas);

        long exitosos = resultados.stream().filter(Pago.class::isInstance).count();
        long sinSaldo = resultados.stream().filter(SaldoInsuficienteException.class::isInstance).count();
        assertThat(exitosos).isEqualTo(5);
        assertThat(sinSaldo).isEqualTo(HILOS - 5);
        assertThat(saldo()).isEqualByComparingTo("0.00");
        assertThat(pagoRepository.count()).isEqualTo(5);
    }

    @Test
    void laMismaClaveEnviadaVariasVecesALaVezSoloRetieneUnaVez() throws Exception {
        List<Callable<Object>> tareas = new ArrayList<>();
        for (int i = 0; i < HILOS; i++) {
            String upc = String.format("UPC%012d", i);
            tareas.add(() -> transacciones.registrarYRetener(1, solicitud(), "misma-clave-0001", upc, "20261030101500"));
        }

        List<Object> resultados = ejecutarALaVez(tareas);

        assertThat(resultados.stream().filter(Pago.class::isInstance).count()).isEqualTo(1);
        assertThat(resultados.stream().filter(DataIntegrityViolationException.class::isInstance).count())
                .isEqualTo(HILOS - 1);
        assertThat(saldo()).isEqualByComparingTo("400.00");
    }

    @Test
    void elRechazoReembolsaUnaSolaVezAunqueLleguenDosResultadosALaVez() throws Exception {
        Pago pago = transacciones.registrarYRetener(1, solicitud(), "rechazo-doble-01", "UPC000000000099", "20261030101500");
        GestoPagoTxResultado rechazo = new GestoPagoTxResultado(GestoPagoTxResultado.Tipo.RECHAZADO, "06", "dup", null, null);
        List<Callable<Object>> tareas = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tareas.add(() -> transacciones.rechazarYReembolsar(pago.getId(), rechazo));
        }

        ejecutarALaVez(tareas);

        assertThat(saldo()).isEqualByComparingTo("500.00");
        assertThat(pagoRepository.findById(pago.getId()).orElseThrow().getEstatus()).isEqualTo(EstatusPago.RECHAZADO);
    }

    @Test
    void unPagoAplicadoYaNoSePuedeReembolsar() {
        Pago pago = transacciones.registrarYRetener(1, solicitud(), "aplicado-0001", "UPC000000000098", "20261030101500");
        transacciones.aplicar(pago.getId(), new GestoPagoTxResultado(GestoPagoTxResultado.Tipo.EXITOSO, "01", "ok", "9001", "55"));

        transacciones.rechazarYReembolsar(pago.getId(),
                new GestoPagoTxResultado(GestoPagoTxResultado.Tipo.RECHAZADO, "06", "dup", null, null));

        assertThat(saldo()).isEqualByComparingTo("400.00");
        Pago guardado = pagoRepository.findById(pago.getId()).orElseThrow();
        assertThat(guardado.getEstatus()).isEqualTo(EstatusPago.APLICADO);
        assertThat(guardado.getNumAutorizacion()).isEqualTo("9001");
    }

    private List<Object> ejecutarALaVez(List<Callable<Object>> tareas) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tareas.size());
        CountDownLatch salida = new CountDownLatch(1);
        try {
            List<Future<Object>> futuros = new ArrayList<>();
            for (Callable<Object> tarea : tareas) {
                futuros.add(executor.submit(() -> {
                    salida.await();
                    try {
                        return tarea.call();
                    } catch (Exception e) {
                        return e;
                    }
                }));
            }
            salida.countDown();
            List<Object> resultados = new ArrayList<>();
            for (Future<Object> futuro : futuros) {
                resultados.add(futuro.get());
            }
            return resultados;
        } finally {
            executor.shutdownNow();
        }
    }

    private BigDecimal saldo() {
        return cuentaRepository.consultarSaldo("1000000001").orElseThrow();
    }

    private PagoRequest solicitud() {
        PagoRequest request = new PagoRequest();
        request.setNumeroCuenta("1000000001");
        request.setIdServicio(2284);
        request.setIdProducto(14302);
        request.setMonto(new BigDecimal("100.00"));
        request.setTelefono("5512345678");
        return request;
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = {CuentaRepository.class, PagoRepository.class})
    @Import(PagoTransacciones.class)
    static class Config {

        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource(
                    "jdbc:h2:mem:pagos;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_DELAY=-1",
                    "sa", "");
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
            em.setDataSource(dataSource);
            em.setPackagesToScan("com.proyecto.servicios.entity.onboarding",
                    "com.proyecto.servicios.entity.pagos",
                    "com.proyecto.servicios.entity.gestopago");
            em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            em.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return em;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory);
        }
    }
}
