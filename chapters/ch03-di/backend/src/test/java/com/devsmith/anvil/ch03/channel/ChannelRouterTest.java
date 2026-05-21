package com.devsmith.anvil.ch03.channel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ChannelRouter")
class ChannelRouterTest {

    private final ChannelRouter router = new ChannelRouter(Map.of(
            "emailChannel", new EmailChannel(),
            "smsChannel", new SmsChannel()
    ));

    @Test
    void 이메일_주소는_email_채널로_라우팅() {
        assertThat(router.route("a@b.com").kind()).isEqualTo("email");
    }

    @Test
    void 그_외는_sms_채널로_라우팅() {
        assertThat(router.route("01012345678").kind()).isEqualTo("sms");
    }

    @Test
    void 매칭되는_채널이_없으면_명확한_에러를_던진다() {
        // given — 이메일 채널만 등록
        Map<String, Channel> onlyEmail = new LinkedHashMap<>();
        onlyEmail.put("emailChannel", new EmailChannel());
        ChannelRouter narrow = new ChannelRouter(onlyEmail);

        // when / then — sms 가 필요한 입력
        assertThatThrownBy(() -> narrow.route("01012345678"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sms");
    }
}
