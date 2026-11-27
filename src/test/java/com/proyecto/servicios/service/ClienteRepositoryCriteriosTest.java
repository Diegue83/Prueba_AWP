package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CriteriosBusquedaCliente;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(ClienteRepositoryCriteriosTest.Config.class)
class ClienteRepositoryCriteriosTest {

    @Autowired
    private ClienteRepository clienteRepository;

    private Integer clienteId;

    @BeforeEach
    void preparar() {
        clienteRepository.deleteAll();
        Cliente cliente = DatosDePrueba.clienteExistente("hash");
        cliente.setCorreo(DatosDePrueba.CORREO);
        clienteId = clienteRepository.save(cliente).getId();
    }

    @Test
    void sinFiltrosDevuelveSoloAlClienteIndicado() {
        List<Cliente> encontrados = clienteRepository.buscarPorCriterios(
                CriteriosBusquedaCliente.builder().clienteId(clienteId).build());

        assertThat(encontrados).extracting(Cliente::getId).containsExactly(clienteId);
    }

    @Test
    void combinaTodosLosFiltrosCuandoCoinciden() {
        LocalDate hoy = LocalDate.now(ZoneId.systemDefault());
        List<Cliente> encontrados = clienteRepository.buscarPorCriterios(CriteriosBusquedaCliente.builder()
                .clienteId(clienteId)
                .id(clienteId)
                .curp(DatosDePrueba.CURP)
                .rfc(DatosDePrueba.RFC)
                .correo("ZAYDA@CORREO.COM")
                .numeroCuenta(DatosDePrueba.NUMERO_CUENTA)
                .activo(true)
                .fechaRegistroDesde(hoy.minusDays(1))
                .fechaRegistroHasta(hoy.plusDays(1))
                .build());

        assertThat(encontrados).hasSize(1);
    }

    @Test
    void unFiltroQueNoCoincideExcluyeAlCliente() {
        assertThat(clienteRepository.buscarPorCriterios(CriteriosBusquedaCliente.builder()
                .clienteId(clienteId).rfc("XAXX010101000").build())).isEmpty();
        assertThat(clienteRepository.buscarPorCriterios(CriteriosBusquedaCliente.builder()
                .clienteId(clienteId).activo(false).build())).isEmpty();
        assertThat(clienteRepository.buscarPorCriterios(CriteriosBusquedaCliente.builder()
                .clienteId(clienteId).fechaRegistroDesde(LocalDate.now(ZoneId.systemDefault()).plusDays(1)).build()))
                .isEmpty();
    }

    @Test
    void noDevuelveClientesDeOtroTitular() {
        assertThat(clienteRepository.buscarPorCriterios(CriteriosBusquedaCliente.builder()
                .clienteId(clienteId + 1000).curp(DatosDePrueba.CURP).build())).isEmpty();
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = ClienteRepository.class)
    static class Config {

        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource(
                    "jdbc:h2:mem:clientes;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
            em.setDataSource(dataSource);
            em.setPackagesToScan("com.proyecto.servicios.entity.onboarding");
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
