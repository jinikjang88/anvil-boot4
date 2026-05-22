package com.devsmith.anvil.ch03.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 Spring 컨텍스트를 띄워 그래프 산정 로직을 검증한다.
 * (이 테스트는 컨테이너 의존 — 의도적. {@link NotificationServiceTest} 와의 비교용.)
 */
@SpringBootTest
@DisplayName("BeanGraphService — 실제 Spring 컨테이너 위에서")
class BeanGraphServiceTest {

    @Autowired
    private BeanGraphService service;

    @Test
    void 핵심_빈들이_그래프에_포함된다() {
        // when
        BeanGraphService.Graph graph = service.snapshot();

        // then
        assertThat(nodeIds(graph)).contains(
                "NotificationService",
                "ChannelRouter",
                "MessageFormatter",
                "EmailChannel",
                "SmsChannel"
        );
    }

    @Test
    void NotificationService는_세_의존성과_엣지가_연결된다() {
        // when
        BeanGraphService.Graph graph = service.snapshot();

        // then
        assertThat(graph.edges())
                .filteredOn(e -> e.from().equals("NotificationService"))
                .extracting(BeanGraphService.Edge::to)
                .containsExactlyInAnyOrder("MessageFormatter", "ChannelRouter", "Clock");
    }

    @Test
    void ChannelRouter의_Map의존성은_실_채널_빈들로_펼쳐진다() {
        // when
        BeanGraphService.Graph graph = service.snapshot();

        // then — Map<String, Channel> 이 [EmailChannel, SmsChannel] 두 엣지로 풀려야 함
        assertThat(graph.edges())
                .filteredOn(e -> e.from().equals("ChannelRouter"))
                .extracting(BeanGraphService.Edge::to)
                .containsExactlyInAnyOrder("EmailChannel", "SmsChannel");
    }

    @Test
    void Clock은_external_카테고리로_표시된다() {
        BeanGraphService.Graph graph = service.snapshot();

        assertThat(graph.nodes())
                .filteredOn(n -> n.id().equals("Clock"))
                .singleElement()
                .extracting(BeanGraphService.Node::kind)
                .isEqualTo("external");
    }

    private static java.util.List<String> nodeIds(BeanGraphService.Graph graph) {
        return graph.nodes().stream().map(BeanGraphService.Node::id).toList();
    }
}
