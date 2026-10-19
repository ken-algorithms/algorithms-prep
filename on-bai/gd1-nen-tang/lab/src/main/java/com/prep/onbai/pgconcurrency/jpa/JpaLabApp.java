package com.prep.onbai.pgconcurrency.jpa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** App Spring Boot + JPA nho cho bai 04: cac bay @Transactional va locking trong Spring Data JPA. */
@SpringBootApplication
public class JpaLabApp {

    public static void main(String[] args) {
        SpringApplication.run(JpaLabApp.class, args);
    }
}
