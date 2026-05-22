package com.devsmith.anvil.ch03.service;

import com.devsmith.anvil.ch03.channel.Channel;
import com.devsmith.anvil.ch03.channel.ChannelRouter;
import com.devsmith.anvil.ch03.domain.NotificationRequest;
import com.devsmith.anvil.ch03.domain.NotificationResult;
import com.devsmith.anvil.ch03.formatter.MessageFormatter;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * 알림 발송 서비스 — 본 챕터의 주인공.
 *
 * <p>의존성 3 개({@link MessageFormatter}, {@link ChannelRouter}, {@link Clock})를
 * <b>생성자로</b> 주입받는다. 핵심 효용은 단위 테스트:</p>
 *
 * <pre>{@code
 *   // Spring 컨텍스트 없이, 순수 Java 로
 *   MessageFormatter formatter = new MessageFormatter();
 *   ChannelRouter router = new ChannelRouter(Map.of("email", mockEmail, "sms", mockSms));
 *   Clock fixed = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
 *   NotificationService service = new NotificationService(formatter, router, fixed);
 *   // 밀리초 안에 끝나는 단위 테스트 — 이것이 DI 의 진짜 ROI.
 * }</pre>
 *
 * <p>비교: 만약 이 클래스가 내부에서 {@code new MessageFormatter()}, {@code new EmailChannel()}
 * 을 직접 만들었다면, 테스트에서 그것을 mock 으로 교체할 방법이 없어 통합 테스트로만 검증 가능 —
 * 같은 단위 테스트가 수십~수백 ms 로 늘어난다.</p>
 *
 * <p><b>final 필드</b>: 생성자 주입의 부수 효과 — 불변성을 컴파일러가 강제해 준다.
 * setter / field 주입은 final 을 쓸 수 없어 가변이 된다.</p>
 */
@Service
public class NotificationService {

    private final MessageFormatter formatter;
    private final ChannelRouter router;
    private final Clock clock;

    /**
     * 단일 생성자라 @Autowired 생략 가능 (Spring 4.3+).
     * 명시적으로 모든 의존성을 노출 — 누가, 무엇을 의존하는지가 시그니처에 다 있다.
     */
    public NotificationService(MessageFormatter formatter, ChannelRouter router, Clock clock) {
        this.formatter = formatter;
        this.router = router;
        this.clock = clock;
    }

    public NotificationResult send(NotificationRequest request) {
        String formattedBody = formatter.format(request.subject(), request.body());
        Channel channel = router.route(request.recipient());
        boolean delivered = channel.send(request.recipient(), formattedBody);

        return new NotificationResult(
                request.recipient(),
                channel.kind(),
                formattedBody,
                Instant.now(clock),
                delivered
        );
    }
}
