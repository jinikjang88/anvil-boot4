package com.devsmith.anvil.ch04.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 프로파일에 따라 다른 yml 이 로드되는지 검증.
 * <p>{@code @ActiveProfiles} 를 바꿔가며 같은 코드가 다른 값을 보는 모습을 확인한다.</p>
 */
@SpringBootTest
class AppPropertiesTest {

    @ActiveProfiles("local")
    @SpringBootTest
    @DisplayName("local 프로파일")
    static class LocalProfile {

        @Autowired AppProperties properties;

        @Test
        void 로컬은_rate_limit_무제한이고_environment_label이_로컬이다() {
            assertThat(properties.environmentLabel()).contains("로컬");
            assertThat(properties.rateLimit().maxPerMinute()).isEqualTo(-1);
        }

        @Test
        void 공통_yml에서_name과_version이_바인딩된다() {
            assertThat(properties.name()).isEqualTo("anvil-boot4");
            assertThat(properties.version()).isEqualTo("0.4.0");
        }
    }

    @ActiveProfiles("dev")
    @SpringBootTest
    @DisplayName("dev 프로파일")
    static class DevProfile {

        @Autowired AppProperties properties;

        @Test
        void dev는_rate_limit_100_이고_sandbox_url을_쓴다() {
            assertThat(properties.environmentLabel()).contains("개발");
            assertThat(properties.rateLimit().maxPerMinute()).isEqualTo(100);
            assertThat(properties.sender().baseUrl()).contains("sandbox");
        }
    }

    @ActiveProfiles("prod")
    @SpringBootTest
    @DisplayName("prod 프로파일")
    static class ProdProfile {

        @Autowired AppProperties properties;

        @Test
        void prod는_rate_limit_10_이고_운영_url을_쓴다() {
            assertThat(properties.environmentLabel()).contains("운영");
            assertThat(properties.rateLimit().maxPerMinute()).isEqualTo(10);
            assertThat(properties.sender().baseUrl()).contains("api.anvil.run");
        }
    }
}
