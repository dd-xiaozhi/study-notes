package com.xiaozhi.mall.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * 版本信息：用来观察"当前请求落在哪个 Pod、运行的是哪个镜像版本"
 */
@RestController
public class VersionController {

    /** 由部署环境注入（K8s 中等于镜像 tag），本地运行时为 local */
    @Value("${app.version}")
    private String version;

    @GetMapping("/api/orders/version")
    public Map<String, String> version() throws UnknownHostException {
        return Map.of(
                "service", "mall-order",
                "version", version,
                "host", InetAddress.getLocalHost().getHostName());
    }
}
