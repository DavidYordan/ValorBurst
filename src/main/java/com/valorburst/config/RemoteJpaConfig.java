package com.valorburst.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "com.valorburst.repository.remote",  // 远程数据库的 Repository 包路径
    entityManagerFactoryRef = "remoteEntityManagerFactory",
    transactionManagerRef = "remoteTransactionManager"
)
public class RemoteJpaConfig {

    @Bean(name = "remoteEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean remoteEntityManagerFactory(
            @Qualifier("remoteDataSource") DataSource remoteDataSource,
            EntityManagerFactoryBuilder builder) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        return builder
                .dataSource(remoteDataSource)
                .packages("com.valorburst.model.remote")  // 实体类的包路径
                .persistenceUnit("remote")
                .build();
    }

    @Bean(name = "remoteTransactionManager")
    public PlatformTransactionManager remoteTransactionManager(
            @Qualifier("remoteEntityManagerFactory") EntityManagerFactory remoteEntityManagerFactory) {
        return new JpaTransactionManager(remoteEntityManagerFactory);
    }
}
