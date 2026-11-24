package com.proyecto.servicios.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;


@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {
                "com.proyecto.servicios.repositorys.sf",
                "com.proyecto.servicios.repositorys.gestopago",
                "com.proyecto.servicios.repositorys.onboarding",
                "com.proyecto.servicios.repositorys.pagos"
        },
        transactionManagerRef = "sfTransactionManager",
        entityManagerFactoryRef = "sfEntityManagerFactory"
)
public class ConfigDB {

    private static final String UNIDAD_PERSISTENCIA = "sfDatasource";

    private final Environment env;

    public ConfigDB(Environment env) {
        this.env = env;
    }

    @Bean(name = UNIDAD_PERSISTENCIA)
    public DataSource sfDatasource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(env.getProperty("spring.datasource.url"));
        config.setPassword(env.getProperty("spring.datasource.password"));
        config.setUsername(env.getProperty("spring.datasource.username"));
        config.setMaximumPoolSize(10);
        config.setMaxLifetime(18800);
        config.setConnectionTimeout(5000);
        config.setValidationTimeout(5000);
        config.setMinimumIdle(2);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName(UNIDAD_PERSISTENCIA);
        return new HikariDataSource(config);
    }

    @Bean(name = "sfEntityManagerFactory")
    @DependsOn("flyway")
    public LocalContainerEntityManagerFactoryBean sfEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(sfDatasource());
        em.setPackagesToScan(
                "com.proyecto.servicios.entity.sf",
                "com.proyecto.servicios.entity.gestopago",
                "com.proyecto.servicios.entity.onboarding",
                "com.proyecto.servicios.entity.pagos"
        );
        em.setPersistenceUnitName(UNIDAD_PERSISTENCIA);
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        return em;
    }

    @Bean(name = "sfTransactionManager")
    public PlatformTransactionManager sfTransactionManager(
            @Qualifier("sfEntityManagerFactory") EntityManagerFactory sfEntityManagerFactory) {
        return new JpaTransactionManager(sfEntityManagerFactory);
    }
}
