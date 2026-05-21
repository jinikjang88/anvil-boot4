package com.devsmith.anvil.ch07.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 인증/인가 흐름 전체 e2e — 실제 Spring Security 필터 체인 위에서.
 * 시나리오를 순서대로 실행해 가입 → 로그인 → 토큰 → 보호된 엔드포인트 검증.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Security E2E — 회원가입 / 로그인 / 인증 / 인가")
class SecurityE2eTest {

    @Autowired
    private MockMvc mockMvc;

    private static String userToken;
    private static String adminToken;

    @Test @Order(1)
    void 회원가입_USER_는_201_과_Location_을_반환() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"u@x.com","password":"password123","name":"홍",
                                 "phoneNumber":"010-1111-2222","role":"USER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.phoneMasked").value("010-****-2222"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test @Order(2)
    void 회원가입_ADMIN_도_같은_경로로() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@x.com","password":"adminpass123","name":"adm",
                                 "phoneNumber":"010-9999-8888","role":"ADMIN"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test @Order(3)
    void 같은_이메일_중복_가입은_409() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"u@x.com","password":"password123","name":"dup",
                                 "phoneNumber":"010-0000-0000","role":"USER"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://anvil.run/errors/conflict"));
    }

    @Test @Order(4)
    void 로그인_USER_는_accessToken_을_받는다() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"u@x.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andReturn();
        userToken = extractToken(result.getResponse().getContentAsString());
        assertThat(userToken).isNotBlank();
    }

    @Test @Order(5)
    void 로그인_ADMIN() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@x.com","password":"adminpass123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = extractToken(result.getResponse().getContentAsString());
    }

    @Test @Order(6)
    void 잘못된_비번_로그인은_401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"u@x.com","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://anvil.run/errors/unauthorized"));
    }

    @Test @Order(7)
    void 토큰_없이_me_는_401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://anvil.run/errors/unauthorized"));
    }

    @Test @Order(8)
    void USER_토큰으로_me_는_본인_정보_반환() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("u@x.com"))
                .andExpect(jsonPath("$.phoneMasked").value("010-****-2222"));
    }

    @Test @Order(9)
    void USER_가_관리자용_users_호출하면_403() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://anvil.run/errors/forbidden"));
    }

    @Test @Order(10)
    void ADMIN_토큰으로_users_는_200() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test @Order(11)
    void 위조된_토큰은_401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer not.a.real.jwt"))
                .andExpect(status().isUnauthorized());
    }

    private static String extractToken(String responseBody) {
        int idx = responseBody.indexOf("\"accessToken\":\"") + "\"accessToken\":\"".length();
        return responseBody.substring(idx, responseBody.indexOf('"', idx));
    }
}
