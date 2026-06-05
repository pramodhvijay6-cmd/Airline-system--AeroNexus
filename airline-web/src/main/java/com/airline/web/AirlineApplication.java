package com.airline.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.airline")
@EnableScheduling
@EnableAsync
public class AirlineApplication {
    public static void main(String[] args) {
        SpringApplication.run(AirlineApplication.class, args);
    }
}
