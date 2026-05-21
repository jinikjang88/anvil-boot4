package com.devsmith.anvil.ch03.channel;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 수신자 형태에 따라 채널을 선택한다.
 *
 * <p>핵심 학습 포인트 — <b>{@code Map<String, Channel>} 자동 수집</b>:
 * Spring 은 같은 인터페이스를 구현한 모든 빈을 {@code 빈이름 -> 빈객체} 맵으로 묶어 준다.
 * 따라서 새 채널이 추가돼도 이 클래스는 손대지 않아도 된다 (OCP — 개방-폐쇄 원칙).</p>
 *
 * <p>주입 방식: <b>생성자 주입</b> (final 필드 + final 클래스 가능 — 불변 보장).</p>
 */
@Component
public class ChannelRouter {

    private final Map<String, Channel> channelsByBeanName;

    public ChannelRouter(Map<String, Channel> channelsByBeanName) {
        this.channelsByBeanName = channelsByBeanName;
    }

    /**
     * 수신자가 이메일 주소 형태면 email 채널, 아니면 sms 채널.
     * 실제 매칭 기준은 학습용으로 의도적으로 단순화 — 운영에선 정규식/유효성 검증 등 분리.
     */
    public Channel route(String recipient) {
        String kind = recipient.contains("@") ? "email" : "sms";
        return channelsByBeanName.values().stream()
                .filter(channel -> channel.kind().equals(kind))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "등록된 채널 중 '" + kind + "' 가 없다 — Channel 구현체를 컴포넌트 스캔 경로에 둬야 한다"));
    }
}
