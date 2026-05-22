package com.devsmith.anvil.ch07.controller;

import com.devsmith.anvil.ch07.domain.User;
import com.devsmith.anvil.ch07.security.EncryptionService;
import com.devsmith.anvil.ch07.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 본인 정보 조회. JWT 인증만 통과하면 됨 (USER, ADMIN 모두 가능).
     *
     * <p>{@link AuthenticationPrincipal} 로 JwtAuthenticationFilter 가 채운 principal(userId) 을 받는다.</p>
     */
    @GetMapping("/me")
    public AuthDtos.UserSummary me(@AuthenticationPrincipal String userId) {
        User user = userService.findById(userId);
        return toSummary(user);
    }

    /**
     * 전체 사용자 목록 — <b>ADMIN 만</b>. {@code @PreAuthorize} 로 선언적 인가.
     * USER 가 호출하면 {@code JwtAccessDeniedHandler} 가 403 응답.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuthDtos.UserSummary> list() {
        return userService.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    private AuthDtos.UserSummary toSummary(User user) {
        String phonePlain = userService.decryptPhone(user);
        return new AuthDtos.UserSummary(
                user.id(),
                user.email(),
                user.name(),
                EncryptionService.maskPhone(phonePlain),
                user.role()
        );
    }
}
