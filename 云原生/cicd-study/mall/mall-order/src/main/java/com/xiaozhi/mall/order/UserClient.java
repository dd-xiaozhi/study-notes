package com.xiaozhi.mall.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/**
 * 用户服务客户端。
 * <p>
 * 服务地址来自配置 mall.user-service.url：
 * 本地为 http://localhost:8081；Docker Compose 中为 http://mall-user:8081；
 * K8s 中同样是 http://mall-user:8081 —— 由 K8s Service 的 DNS 完成服务发现，无需注册中心。
 */
@Component
public class UserClient {

    private final RestClient restClient;

    public UserClient(RestClient.Builder builder, @Value("${mall.user-service.url}") String userServiceUrl) {
        this.restClient = builder.baseUrl(userServiceUrl).build();
    }

    public UserDTO getUser(Long userId) {
        try {
            return restClient.get()
                    .uri("/api/users/{id}", userId)
                    .retrieve()
                    .body(UserDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户不存在: " + userId);
        }
    }

    public record UserDTO(Long id, String username, String nickname) {
    }
}
