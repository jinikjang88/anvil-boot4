package com.devsmith.anvil.ch03.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmsChannel implements Channel {

    private static final Logger log = LoggerFactory.getLogger(SmsChannel.class);

    @Override
    public String kind() {
        return "sms";
    }

    @Override
    public boolean send(String recipient, String body) {
        log.info("[sms] -> {} : {}", recipient, body);
        return true;
    }
}
