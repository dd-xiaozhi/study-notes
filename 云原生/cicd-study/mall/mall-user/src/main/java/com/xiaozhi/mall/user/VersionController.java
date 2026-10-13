package com.xiaozhi.mall.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * 版本信息：用来观察"当前请求落在哪个 Pod、运行的是哪个镜像版本"，
 * 是验证滚动更新、负载均衡、回滚效果的利器。
 */
@RestController
public class VersionController {

    /** 由部署环境注入（K8s 中等于镜像 tag），本地运行时为 local */
    @Value("${app.version}")
    private String version;

    @GetMapping("/api/users/version")
    public Map<String, String> version() throws UnknownHostException {
        return Map.of(
                "service", "mall-user",
                "version", version,
                "host", InetAddress.getLocalHost().getHostName());
    }
}
