package com.prolizwebservices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // 🚀 Progressive background caching için
public class ProlizWebServicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProlizWebServicesApplication.class, args);
    }

}
