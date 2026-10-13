package com.xiaozhi.mall.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /** 单元测试不依赖真实的 mall-user 服务，用 Mock 模拟远程调用结果 */
    @MockitoBean
    private UserClient userClient;

    @Test
    void createOrderWhenUserExists() throws Exception {
        given(userClient.getUser(1L)).willReturn(new UserClient.UserDTO(1L, "zhangsan", "张三"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 1, "productName": "机械键盘", "amount": 299.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("zhangsan"))
                .andExpect(jsonPath("$.productName").value("机械键盘"));
    }

    @Test
    void createOrderWhenUserNotExists() throws Exception {
        given(userClient.getUser(404L))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户不存在: 404"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 404, "productName": "机械键盘", "amount": 299.00}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidAmountReturns400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 1, "productName": "机械键盘", "amount": 0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownOrderReturns404() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", 9999))
                .andExpect(status().isNotFound());
    }

    @Test
    void versionEndpoint() throws Exception {
        mockMvc.perform(get("/api/orders/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("mall-order"));
    }
}
