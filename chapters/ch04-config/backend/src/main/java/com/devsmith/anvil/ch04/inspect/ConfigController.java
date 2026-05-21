package com.devsmith.anvil.ch04.inspect;

import com.devsmith.anvil.ch04.config.AppProperties;
import com.devsmith.anvil.ch04.secret.SecretMasker;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final AppProperties properties;
    private final ConfigInspector inspector;
    private final SecretMasker masker;

    public ConfigController(AppProperties properties, ConfigInspector inspector, SecretMasker masker) {
        this.properties = properties;
        this.inspector = inspector;
        this.masker = masker;
    }

    /** 활성 프로파일과 바인딩된 anvil.* 설정 (시크릿 마스킹된 형태). */
    @GetMapping("/active")
    public Map<String, Object> active() {
        Map<String, Object> sender = new LinkedHashMap<>();
        sender.put("baseUrl", properties.sender().baseUrl());
        sender.put("apiKey", masker.mask(properties.sender().apiKey()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activeProfiles", inspector.activeProfiles());
        result.put("name", properties.name());
        result.put("version", properties.version());
        result.put("environmentLabel", properties.environmentLabel());
        result.put("rateLimit", properties.rateLimit());
        result.put("sender", sender);
        return result;
    }

    /**
     * 특정 키가 어느 PropertySource 에서 왔는지 추적한다.
     * <pre>
     *   GET /api/config/source?key=anvil.sender.api-key
     *   → { "key":"anvil.sender.api-key", "value":"sk-****abcd",
     *       "source":"Config resource 'class path resource [application-local-secret.yml]'..." }
     * </pre>
     */
    @GetMapping("/source")
    public ResponseEntity<Map<String, Object>> source(@RequestParam String key) {
        return inspector.findSource(key)
                .<ResponseEntity<Map<String, Object>>>map(info -> {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("key", info.key());
                    body.put("value", masker.isSensitive(key) ? masker.mask(info.value()) : info.value());
                    body.put("source", info.source());
                    body.put("sensitive", masker.isSensitive(key));
                    return ResponseEntity.ok(body);
                })
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(Map.of("error", "key not found: " + key)));
    }

    /** 현재 등록된 PropertySource 들의 우선순위 순서 (디버깅용). */
    @GetMapping("/sources")
    public Map<String, Object> sources() {
        return Map.of("propertySources", inspector.propertySourceNames());
    }
}
