package com.devsmith.anvil.ch03.service;

import com.devsmith.anvil.ch03.channel.Channel;
import com.devsmith.anvil.ch03.channel.ChannelRouter;
import com.devsmith.anvil.ch03.domain.NotificationRequest;
import com.devsmith.anvil.ch03.domain.NotificationResult;
import com.devsmith.anvil.ch03.formatter.MessageFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NotificationService 단위 테스트.
 *
 * <p>핵심 학습 포인트 — <b>이 파일에 {@code @SpringBootTest} 도 {@code @Autowired} 도 없다</b>.
 * Spring 컨텍스트를 띄우지 않고 순수 Java 로 객체를 조립한다. 그래서 6 개 케이스 전체가
 * 보통 <i>수십 ms</i> 안에 끝난다. {@code new XxxChannel()} 을 서비스 내부에서 했더라면
 * 채널 동작을 통제할 방법이 없어 통합 테스트로 끌어올려야 했을 것.</p>
 *
 * <p>채널 mock 은 일부러 별도 mocking 프레임워크 없이 손으로 만들었다 — DI 의 효용이
 * Mockito 같은 도구에 의존하지 않는다는 걸 보여주기 위함.</p>
 */
@DisplayName("NotificationService — DI 덕분에 컨텍스트 없이 검증된다")
class NotificationServiceTest {

    private RecordingChannel email;
    private RecordingChannel sms;
    private Clock fixedClock;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        email = new RecordingChannel("email");
        sms = new RecordingChannel("sms");
        ChannelRouter router = new ChannelRouter(Map.of(
                "emailChannel", email,
                "smsChannel", sms
        ));
        fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        service = new NotificationService(new MessageFormatter(), router, fixedClock);
    }

    @Test
    void 이메일_주소면_email_채널로_보낸다() {
        // given
        NotificationRequest request = new NotificationRequest("user@example.com", "hello", "world");

        // when
        NotificationResult result = service.send(request);

        // then
        assertThat(result.channel()).isEqualTo("email");
        assertThat(result.delivered()).isTrue();
        assertThat(email.calls).hasSize(1);
        assertThat(sms.calls).isEmpty();
    }

    @Test
    void 전화번호처럼_생기면_sms_채널로_보낸다() {
        // given
        NotificationRequest request = new NotificationRequest("01012345678", "hello", "world");

        // when
        NotificationResult result = service.send(request);

        // then
        assertThat(result.channel()).isEqualTo("sms");
        assertThat(sms.calls).hasSize(1);
        assertThat(email.calls).isEmpty();
    }

    @Test
    void 본문은_subject가_있으면_괄호로_묶여_포매팅된다() {
        // given
        NotificationRequest request = new NotificationRequest("u@x.com", "alert", "down");

        // when
        NotificationResult result = service.send(request);

        // then
        assertThat(result.body()).isEqualTo("[alert] down");
    }

    @Test
    void subject가_없으면_본문만_보낸다() {
        NotificationResult result = service.send(new NotificationRequest("u@x.com", null, "down"));
        assertThat(result.body()).isEqualTo("down");
    }

    @Test
    void Clock을_고정하면_sentAt이_결정적이다() {
        // when
        NotificationResult result = service.send(new NotificationRequest("u@x.com", "x", "y"));

        // then — fixedClock 으로 결과가 시간에 의존하지 않게 됨 (DI 의 부수 효과)
        assertThat(result.sentAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void 수신자가_비어있으면_record_생성에서_막힌다() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new NotificationRequest("", "x", "y"));
    }

    /** 손으로 만든 미니 mock — Channel 인터페이스가 단순해서 가능하다. */
    private static final class RecordingChannel implements Channel {
        private final String kind;
        private final List<String> calls = new ArrayList<>();

        RecordingChannel(String kind) {
            this.kind = kind;
        }

        @Override public String kind() { return kind; }

        @Override
        public boolean send(String recipient, String body) {
            calls.add(recipient + "|" + body);
            return true;
        }
    }
}
