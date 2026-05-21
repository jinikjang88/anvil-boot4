package com.devsmith.anvil.ch07.controller;

import com.devsmith.anvil.ch07.domain.User;
import com.devsmith.anvil.ch07.security.EncryptionService;
import com.devsmith.anvil.ch07.security.JwtService;
import com.devsmith.anvil.ch07.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDtos.UserSummary> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        User user = userService.register(
                request.email(),
                request.password(),
                request.name(),
                request.phoneNumber(),
                request.role()
        );
        return ResponseEntity
                .created(URI.create("/api/v1/users/" + user.id()))
                .body(toSummary(user));
    }

    @PostMapping("/login")
    public AuthDtos.LoginResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        User user = userService.authenticate(request.email(), request.password());
        JwtService.Issued issued = jwtService.issue(user.id(), user.email(), user.role());
        return new AuthDtos.LoginResponse(
                issued.token(),
                issued.expiresInSeconds(),
                toSummary(user)
        );
    }

    /**
     * 학습용 JWT 디코더. 서명 검증까지 통과한 토큰의 클레임만 반환.
     * (위조 / 만료 / issuer 불일치 시 ValidationExceptionHandler 가 401-스러운 에러)
     */
    @PostMapping("/decode")
    public JwtService.Verified decode(@Valid @RequestBody AuthDtos.DecodeRequest request) {
        return jwtService.verify(request.token());
    }

    private AuthDtos.UserSummary toSummary(User user) {
        // phone 은 복호화 → 마스킹된 형태로만 노출 (원본 절대 응답에 X)
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
