package com.devsmith.anvil.ch04.inspect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@DisplayName("ConfigInspector — 활성 프로파일에서")
class ConfigInspectorTest {

    @Autowired
    private ConfigInspector inspector;

    @Test
    void 활성_프로파일이_local로_노출된다() {
        assertThat(inspector.activeProfiles()).contains("local");
    }

    @Test
    void anvil_name_키의_소스는_application_yml이다() {
        Optional<ConfigInspector.SourceInfo> result = inspector.findSource("anvil.name");

        assertThat(result).isPresent();
        assertThat(result.get().value()).isEqualTo("anvil-boot4");
        assertThat(result.get().source()).contains("application.yml");
    }

    @Test
    void local_프로파일_고유_값은_application_local_yml에서_온다() {
        Optional<ConfigInspector.SourceInfo> result = inspector.findSource("anvil.rate-limit.max-per-minute");

        assertThat(result).isPresent();
        assertThat(result.get().source()).contains("application-local.yml");
    }

    @Test
    void 없는_키는_빈_Optional() {
        assertThat(inspector.findSource("anvil.does-not-exist")).isEmpty();
    }

    @Test
    void propertySource_이름들이_여러_개_노출된다() {
        assertThat(inspector.propertySourceNames())
                .as("최소 application.yml 와 application-local.yml 이 보여야 함")
                .anyMatch(name -> name.contains("application.yml"))
                .anyMatch(name -> name.contains("application-local.yml"));
    }
}
