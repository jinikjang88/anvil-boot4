package com.devsmith.anvil.ch07.controller;

import com.devsmith.anvil.ch07.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** AuthController 가 받는 요청 / 돌려주는 응답 DTO. ch06 의 Bean Validation 연속. */
public final class AuthDtos {

    private AuthDtos() { }

    public record RegisterRequest(

            @NotBlank @Email
            String email,

            @NotBlank @Size(min = 8, max = 64, message = "비밀번호는 8~64자")
            String password,

            @NotBlank @Size(max = 30)
            String name,

            @NotBlank @Pattern(regexp = "^010-?\\d{3,4}-?\\d{4}$", message = "전화번호 형식이 올바르지 않습니다")
            String phoneNumber,

            @NotNull
            UserRole role
    ) { }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) { }

    public record LoginResponse(
            String accessToken,
            long expiresInSeconds,
            UserSummary user
    ) { }

    public record UserSummary(
            String id,
            String email,
            String name,
            String phoneMasked,
            UserRole role
    ) { }

    public record DecodeRequest(@NotBlank String token) { }
}
