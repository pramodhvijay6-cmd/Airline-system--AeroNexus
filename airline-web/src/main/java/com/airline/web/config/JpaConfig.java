package com.airline.web.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.airline.domain.repository")
@EntityScan(basePackages = "com.airline.domain.entity")
public class JpaConfig {
}
