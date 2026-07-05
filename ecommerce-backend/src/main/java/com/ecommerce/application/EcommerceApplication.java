package com.ecommerce.application;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author AmirHossein ZamanZade
 * @since 12/25/25
 */
@SpringBootApplication(scanBasePackages = "com.ecommerce")
@EnableJpaRepositories("com.ecommerce.persistence.repository")
@EntityScan("com.ecommerce.persistence.entity")
@EnableConfigurationProperties
@EnableScheduling
public class EcommerceApplication {

    static void main(String[] args) {
        if (new FileSystemResource("config/logback.xml").getFile().exists()) {
            System.setProperty("logging.config", "file:config/logback.xml");
        } else {
            System.setProperty("logging.config", "classpath:config/logback.xml");
        }
        new SpringApplicationBuilder(EcommerceApplication.class).run(args);
    }
}
