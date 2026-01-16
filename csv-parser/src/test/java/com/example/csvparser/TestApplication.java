package com.example.csvparser;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = "com.example.csvparser.core.idempotency")
@EnableJpaRepositories(basePackages = "com.example.csvparser.core.idempotency")
public class TestApplication {
}
