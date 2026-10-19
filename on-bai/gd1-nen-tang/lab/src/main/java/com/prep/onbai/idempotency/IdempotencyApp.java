package com.prep.onbai.idempotency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;

/** API thanh toan idempotent (bai 02). Chi JDBC: tat JPA de app khoi dong nhanh. */
@SpringBootApplication(exclude = {HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
public class IdempotencyApp {

    @Bean
    FakePsp psp() {
        return new FakePsp();
    }

    public static void main(String[] args) {
        SpringApplication.run(IdempotencyApp.class, args);
    }
}
