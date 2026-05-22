package com.devsmith.anvil.ch03.domain;

public record NotificationRequest(String recipient, String subject, String body) {

    public NotificationRequest {
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("recipient is required");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("body is required");
        }
        // subject 는 선택 — null 이면 빈 문자열로 정규화
        if (subject == null) {
            subject = "";
        }
    }
}
