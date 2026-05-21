package com.devsmith.anvil.ch07;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Ch07Application {
    public static void main(String[] args) {
        SpringApplication.run(Ch07Application.class, args);
    }
}
