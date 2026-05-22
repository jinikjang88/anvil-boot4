package com.devsmith.anvil.ch03.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 이메일 채널 데모 구현 — 실제 SMTP 대신 로그에만 남긴다.
 *
 * <p>{@code @Component} 만 붙이면 Spring 이 자동 스캔해 {@link Channel} 인터페이스의
 * 빈 컬렉션에 합류시킨다. 등록을 위해 어디서도 {@code new EmailChannel()} 을 쓰지 않는다.</p>
 */
@Component
public class EmailChannel implements Channel {

    private static final Logger log = LoggerFactory.getLogger(EmailChannel.class);

    @Override
    public String kind() {
        return "email";
    }

    @Override
    public boolean send(String recipient, String body) {
        log.info("[email] -> {} : {}", recipient, body);
        return true;
    }
}
