package com.devsmith.anvil.ch05.controller;

import com.devsmith.anvil.ch05.config.AppConfig;
import com.devsmith.anvil.ch05.domain.OrderStateMachine;
import com.devsmith.anvil.ch05.repository.OrderRepository;
import com.devsmith.anvil.ch05.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST 정석을 메서드별로 검증.
 *
 * <p>실 서비스 + 실 인-메모리 저장소를 끌어와 통합 흐름을 본다 (서비스가 단순해 mock 의 가치가 적음).</p>
 */
@WebMvcTest(controllers = {OrderController.class, RestExceptionHandler.class})
@Import({OrderService.class, OrderRepository.class, OrderStateMachine.class, AppConfig.class})
@DisplayName("OrderController — HTTP 메서드별")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String CREATE_BODY = """
            {
              "customer": "alice",
              "items": [ {"sku":"sku-1","name":"도토리","quantity":2,"price":1500} ],
              "memo": "급한 주문"
            }
            """;

    @Test
    void POST는_201_Created_와_Location_헤더를_반환() throws Exception {
        mockMvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(3000));
    }

    @Test
    void GET_단건_없는_id는_404() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", "nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void PATCH는_부분_수정하고_200() throws Exception {
        String id = createSampleAndGetId();

        mockMvc.perform(patch("/api/v1/orders/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memo": "변경된 메모"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memo").value("변경된 메모"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void PUT_status_정상_전이는_200() throws Exception {
        String id = createSampleAndGetId();

        mockMvc.perform(put("/api/v1/orders/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CONFIRMED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void PUT_status_불가능_전이는_409() throws Exception {
        String id = createSampleAndGetId();

        mockMvc.perform(put("/api/v1/orders/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DELIVERED"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void DELETE는_204_No_Content() throws Exception {
        String id = createSampleAndGetId();

        mockMvc.perform(delete("/api/v1/orders/{id}", id))
                .andExpect(status().isNoContent());

        // 두 번째 DELETE 는 404
        mockMvc.perform(delete("/api/v1/orders/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_목록은_페이지_메타와_함께_200() throws Exception {
        createSampleAndGetId();
        createSampleAndGetId();

        mockMvc.perform(get("/api/v1/orders").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void POST_본문이_도메인_규칙을_위반하면_400() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customer":"alice","items":[],"memo":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    /** 헬퍼 — POST 로 한 건 생성하고 응답 본문에서 id 추출. */
    private String createSampleAndGetId() throws Exception {
        String response = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andReturn()
                .getResponse()
                .getContentAsString();
        // 간단 파싱 — `"id":"xxx"` 추출
        int start = response.indexOf("\"id\":\"") + 6;
        int end = response.indexOf('"', start);
        return response.substring(start, end);
    }
}
