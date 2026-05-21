package com.devsmith.anvil.ch03.channel;

/**
 * 알림 채널 추상화.
 *
 * <p>여러 구현({@code EmailChannel}, {@code SmsChannel}, ...) 을 가질 수 있으며,
 * Spring 은 {@code Map<String, Channel>} 또는 {@code List<Channel>} 형태로
 * <b>같은 인터페이스를 구현한 모든 빈을 한 번에 주입</b>해 준다.</p>
 *
 * <p>학습 포인트: 인터페이스 + 다중 구현 + 컬렉션 주입 = <i>전략 패턴 + DI</i> 의 시너지.
 * 새 채널을 추가하려면 @Component 클래스 하나 만들고 끝 — 라우터/서비스 코드 수정 불필요.</p>
 */
public interface Channel {

    /** 채널 식별자. 라우터가 이 값으로 발송 채널을 고른다. */
    String kind();

    /**
     * 실제 발송. 운영에선 SMTP/SMS 게이트웨이 호출 등.
     * 학습용 데모에선 콘솔 로그 + true 반환.
     */
    boolean send(String recipient, String body);
}
