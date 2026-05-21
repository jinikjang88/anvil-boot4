package com.devsmith.anvil.ch01.service;

import org.springframework.stereotype.Service;

/**
 * ch01 학습 포인트:
 * <p>
 * Spring 이 관리하는 가장 단순한 형태의 서비스 빈.
 * 컨트롤러가 직접 문자열을 조립하지 않고, 도메인 동작을 서비스에 위임한다.
 * (SRP — Controller 는 HTTP 변환, Service 는 도메인 규칙)
 * </p>
 * <p>
 * 빈 생성을 Spring 에게 맡기기 위해 {@link Service} 를 붙였다.
 * 학습자가 ch03(DI) 에서 왜 {@code new HelloService()} 가 아닌
 * 컨테이너 주입을 쓰는지 비교할 때 다시 사용한다.
 * </p>
 */
@Service
public class HelloService {

    private static final String DEFAULT_TARGET = "anvil";

    /**
     * 입력된 이름으로 인사 문구를 만든다.
     *
     * <p>엣지 케이스 — null, 공백 문자열은 기본값({@value #DEFAULT_TARGET})으로 치환한다.
     * 컨트롤러로 NPE 가 흘러가지 않도록 도메인 계층에서 막는 것이 핵심.</p>
     *
     * @param name 인사 대상. null/blank 허용.
     * @return "hello, {대상}" 형식의 인사 문구
     */
    public String greet(String name) {
        String target = (name == null || name.isBlank()) ? DEFAULT_TARGET : name.trim();
        return "hello, " + target;
    }
}
