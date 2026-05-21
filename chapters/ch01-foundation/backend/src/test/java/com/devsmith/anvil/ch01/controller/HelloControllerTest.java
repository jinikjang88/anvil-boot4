package com.devsmith.anvil.ch01.controller;

import com.devsmith.anvil.ch01.service.HelloService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HelloController 슬라이스 테스트.
 * <p>{@link WebMvcTest} 로 컨트롤러 + MVC 기반 인프라만 로드해 빠르게 검증.
 * 서비스는 {@link MockitoBean} 으로 대체 (Boot 3.4+ 정식 API, 구 @MockBean 의 후속).</p>
 */
@DisplayName("HelloController")
@WebMvcTest(HelloController.class)
class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HelloService helloService;

    @Test
    void GET_api_hello_요청은_200과_인사메시지_JSON을_반환한다() throws Exception {
        // given
        when(helloService.greet(null)).thenReturn("hello, anvil");

        // when & then
        mockMvc.perform(get("/api/hello"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.message").value("hello, anvil"));
    }

    @Test
    void name_쿼리파라미터를_주면_서비스에_전달된다() throws Exception {
        // given
        when(helloService.greet("장진익")).thenReturn("hello, 장진익");

        // when & then
        mockMvc.perform(get("/api/hello").param("name", "장진익"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.message").value("hello, 장진익"));
    }
}
