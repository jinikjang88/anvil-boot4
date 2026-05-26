package com.devsmith.anvil.ch10.config;

import com.devsmith.anvil.ch10.domain.Account;
import com.devsmith.anvil.ch10.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * 3 계좌 시드 — 각 100,000원. 잔액 합계 300,000원이 conservation 기준값.
 */
@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    ApplicationRunner seed(AccountRepository repo) {
        return args -> {
            if (repo.count() > 0) return;
            repo.save(new Account("민지", new BigDecimal("100000")));
            repo.save(new Account("준호", new BigDecimal("100000")));
            repo.save(new Account("지훈", new BigDecimal("100000")));
            log.info("[ch10 seed] 3 계좌 시드 완료 (각 100,000원)");
        };
    }
}
