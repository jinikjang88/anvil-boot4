package com.devsmith.anvil.ch05.error;

/** 리소스를 찾지 못함 → 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
