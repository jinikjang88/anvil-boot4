package com.devsmith.anvil.ch04;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * ch04-config 진입점.
 * {@code @ConfigurationPropertiesScan} 으로 같은 패키지 하위의
 * {@code @ConfigurationProperties} 가 붙은 record/클래스를 자동 등록한다.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class Ch04Application {
    public static void main(String[] args) {
        SpringApplication.run(Ch04Application.class, args);
    }
}
