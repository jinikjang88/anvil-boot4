package com.devsmith.anvil.ch04.inspect;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 살아있는 Spring 환경의 PropertySource 들을 들여다본다.
 *
 * <p>같은 키가 여러 곳에 정의될 수 있다 (yml / 시스템 프로퍼티 / 환경변수 / 명령줄 인자 ...).
 * Spring 은 등록 순서대로 우선순위를 매기고 첫 번째 매치를 사용한다.
 * 본 inspector 는 "이 키가 실제로 어디서 왔나" 를 추적해 디버깅 / 시각화에 쓴다.</p>
 */
@Service
public class ConfigInspector {

    /**
     * Spring Boot 가 {@code @ConfigurationProperties} 조회 최적화를 위해 등록하는
     * 집계 의사-소스. 우리는 <i>원본</i> 출처(yml/env/system)를 보여주는 게 학습 목적이므로
     * 추적 시 건너뛴다.
     */
    private static final String CONFIGURATION_PROPERTIES_PSEUDO_SOURCE = "configurationProperties";

    private final ConfigurableEnvironment environment;

    public ConfigInspector(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    public List<String> activeProfiles() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length == 0
                ? List.of(environment.getDefaultProfiles())
                : List.of(profiles);
    }

    /**
     * PropertySource 들을 우선순위 순서대로 순회하며 첫 번째 매치를 반환.
     * {@code configurationProperties} 의사-소스는 건너뛴다 (실 출처를 가리는 view 일 뿐).
     */
    public Optional<SourceInfo> findSource(String key) {
        for (PropertySource<?> source : environment.getPropertySources()) {
            if (CONFIGURATION_PROPERTIES_PSEUDO_SOURCE.equals(source.getName())) {
                continue;
            }
            if (source.containsProperty(key)) {
                Object value = source.getProperty(key);
                return Optional.of(new SourceInfo(
                        key,
                        value == null ? null : value.toString(),
                        source.getName()
                ));
            }
        }
        return Optional.empty();
    }

    /** 디버깅용 — 현재 등록된 모든 PropertySource 이름을 순서대로 반환. */
    public List<String> propertySourceNames() {
        return environment.getPropertySources().stream()
                .map(PropertySource::getName)
                .toList();
    }

    public record SourceInfo(String key, String value, String source) { }
}
