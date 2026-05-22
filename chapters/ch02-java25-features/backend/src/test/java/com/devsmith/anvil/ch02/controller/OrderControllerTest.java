package com.devsmith.anvil.ch02.controller;

import com.devsmith.anvil.ch02.domain.PaymentGateway;
import com.devsmith.anvil.ch02.service.OrderProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller 슬라이스 테스트.
 *
 * <p>{@link OrderProcessor} 와 {@link PaymentGateway} 를 진짜로 끌어와 동작 검증을 한다.
 * (서비스가 단순하고 외부 의존이 없어서 mock 해도 의미가 적기 때문 — JSON 형태 확인이 목적.)</p>
 */
@WebMvcTest(OrderController.class)
@Import({OrderProcessor.class, PaymentGateway.class})
@DisplayName("OrderController")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void POST_process_virtual은_executor가_virtual인_요약을_반환한다() throws Exception {
        mockMvc.perform(post("/api/orders/process-virtual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "count": 3, "amountMin": 10000, "amountMax": 20000 }
                                """))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.executor").value("virtual"))
               .andExpect(jsonPath("$.total").value(3))
               .andExpect(jsonPath("$.counts.approved").value(3));
    }

    @Test
    void POST_process_platform도_같은_JSON_구조를_가진다() throws Exception {
        mockMvc.perform(post("/api/orders/process-platform")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "count": 3, "amountMin": 10000, "amountMax": 20000 }
                                """))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.executor").value("platform"))
               .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void count가_상한을_넘으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/orders/process-virtual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "count": 9999 }
                                """))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("count")));
    }
}
