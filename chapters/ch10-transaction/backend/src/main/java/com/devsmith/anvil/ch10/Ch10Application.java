package com.devsmith.anvil.ch10;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class Ch10Application {
    public static void main(String[] args) {
        SpringApplication.run(Ch10Application.class, args);
    }
}
