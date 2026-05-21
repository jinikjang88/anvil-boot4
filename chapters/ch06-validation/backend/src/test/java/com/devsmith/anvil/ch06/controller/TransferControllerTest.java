package com.devsmith.anvil.ch06.controller;

import com.devsmith.anvil.ch06.config.AppConfig;
import com.devsmith.anvil.ch06.repository.TransferRepository;
import com.devsmith.anvil.ch06.service.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 컨트롤러 슬라이스 — 검증 실패가 RFC 7807 ProblemDetail 로 응답되는지 검증.
 */
@WebMvcTest(controllers = {TransferController.class, ValidationExceptionHandler.class})
@Import({TransferService.class, TransferRepository.class, AppConfig.class})
@DisplayName("TransferController — Bean Validation + Problem Details")
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String VALID = """
            {
              "fromAccount": "ACC-001",
              "toAccount":   "ACC-002",
              "amount":      10000,
              "currency":    "KRW",
              "memo":        "정상",
              "options":     { "urgent": false }
            }
            """;

    @Test
    void 정상_요청은_201_Created_와_Location_헤더를_반환() throws Exception {
        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.amount").value(10000));
    }

    @Test
    void amount_음수면_400_ProblemDetail() throws Exception {
        String body = VALID.replace("10000", "-100");

        mockMvc.perform(post("/api/v1/transfers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://anvil.run/errors/validation"))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/v1/transfers"))
                .andExpect(jsonPath("$.errors[?(@.field == 'amount')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'amount')].rejectedValue").value(-100));
    }

    @Test
    void currency_소문자면_Pattern_위반() throws Exception {
        String body = VALID.replace("\"KRW\"", "\"won\"");

        mockMvc.perform(post("/api/v1/transfers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'currency')]").exists());
    }

    @Test
    void urgent_true인데_notifyEmail_없으면_options_notifyEmail로_에러가_매핑() throws Exception {
        String body = """
                {
                  "fromAccount":"ACC-1","toAccount":"ACC-2","amount":5000,"currency":"KRW",
                  "options":{ "urgent": true }
                }
                """;

        mockMvc.perform(post("/api/v1/transfers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'options.notifyEmail')]").exists())
                // 이 케이스는 cross-field 위반 한 건만 — errors[0] 으로 직접 확인
                .andExpect(jsonPath("$.errors[0].field").value("options.notifyEmail"))
                .andExpect(jsonPath("$.errors[0].code").value("UrgentRequiresNotifyEmail"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("urgent=true 면 notifyEmail 이 필수입니다"));
    }

    @Test
    void 여러_필드_위반은_errors_배열에_모두_담긴다() throws Exception {
        String body = """
                {
                  "fromAccount":"",
                  "toAccount":"",
                  "amount":-1,
                  "currency":"won",
                  "options":{ "urgent": true }
                }
                """;

        mockMvc.perform(post("/api/v1/transfers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                // 최소 5건은 있어야 함 (fromAccount, toAccount, amount, currency, options.notifyEmail)
                .andExpect(jsonPath("$.errors.length()", org.hamcrest.Matchers.greaterThanOrEqualTo(5)));
    }

    @Test
    void 없는_id로_GET_하면_404_ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/transfers/{id}", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://anvil.run/errors/not-found"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }
}
