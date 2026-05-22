# Redis Session 共享实战

## 目录

- [1. 概述](#1-概述)
- [2. Spring Session + Redis 实现原理](#2-spring-session--redis-实现原理)
- [3. 快速开始](#3-快速开始)
- [4. 完整代码示例](#4-完整代码示例)
- [5. Session 序列化问题与 Jackson 配置](#5-session-序列化问题与-jackson-配置)
- [6. 实战案例：用户登录状态跨服务共享](#6-实战案例用户登录状态跨服务共享)
- [7. 关键技术点说明](#7-关键技术点说明)
- [8. 常见问题与解决方案](#8-常见问题与解决方案)

---

## 1. 概述

### 1.1 什么是 Session 共享

在分布式架构中，多个服务节点需要共享用户的会话数据，以确保用户登录状态在任意节点都能被识别。

```
┌─────────────────────────────────────────────────────────────────┐
│                          用户请求                                 │
│                            │                                     │
│                            ▼                                     │
│                    ┌─────────────┐                              │
│                    │   Nginx     │                              │
│                    │  负载均衡    │                              │
│                    └──────┬──────┘                              │
│           ┌───────────────┼───────────────┐                     │
│           ▼               ▼               ▼                     │
│    ┌────────────┐  ┌────────────┐  ┌────────────┐               │
│    │  服务节点1   │  │  服务节点2   │  │  服务节点3   │               │
│    │  Spring Boot│  │  Spring Boot│  │  Spring Boot│              │
│    └──────┬─────┘  └──────┬─────┘  └──────┬─────┘               │
│           │               │               │                      │
│           └───────────────┼───────────────┘                      │
│                           ▼                                      │
│                    ┌─────────────┐                              │
│                    │    Redis    │                              │
│                    │  Session库  │                              │
│                    └─────────────┘                              │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 技术选型

| 方案 | 优点 | 缺点 |
|------|------|------|
| Spring Session + Redis | 与 Spring Boot 深度集成，配置简单 | 仅适用于 Java 技术栈 |
| JWT Token | 无状态，可跨语言 | 需要考虑 Token 刷新、安全性问题 |
| 黏性 Session | 简单 | 负载不均，节点故障会丢失会话 |

本项目选用 **Spring Session + Redis** 方案。

---

## 2. Spring Session + Redis 实现原理

### 2.1 核心架构

```
┌──────────────────────────────────────────────────────────────────┐
│                      Spring Session 架构                          │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│   ┌─────────────┐         ┌─────────────────┐                    │
│   │ HttpSession │◄────────│ SessionRepository│                    │
│   │   接口层     │         │    (接口)         │                    │
│   └─────────────┘         └────────┬────────┘                    │
│                                     │                             │
│                    ┌────────────────┼────────────────┐           │
│                    ▼                ▼                ▼           │
│           ┌─────────────┐   ┌─────────────┐   ┌─────────────┐     │
│           │ RedisIndexed│   │  Map-based  │   │  Custom     │     │
│           │ SessionRepo  │   │ SessionRepo │   │ SessionRepo │     │
│           └──────┬──────┘   └─────────────┘   └─────────────┘     │
│                  │                                               │
│                  ▼                                               │
│           ┌─────────────┐                                        │
│           │   Redis     │                                        │
│           │  (存储层)    │                                        │
│           └─────────────┘                                        │
└──────────────────────────────────────────────────────────────────┘
```

### 2.2 请求流程

```
1. 首次请求:
   用户请求 → Filter拦截 → 判断无Session → 创建Session → 存入Redis → 返回SessionId(Cookie)
                                                                              │
                                                                              ▼
2. 后续请求:                                                        ┌──────────────┐
   用户请求 → Filter拦截 → 获取Cookie中的SessionId ──────────────────►│    Redis     │
                              │                                        │  spring:     │
                              │                                        │  sessions:   │
                              ▼                                        │  <sessionId> │
                       从Redis查询Session                               └──────────────┘
```

### 2.3 关键组件

| 组件 | 说明 |
|------|------|
| `SessionRepository` | Session 仓储接口，定义 Session 的 CRUD 操作 |
| `RedisIndexedSessionRepository` | Redis 实现类，基于 Redis Hash 存储 Session |
| `SessionRepositoryFilter` | Servlet Filter，拦截请求并管理 Session 生命周期 |
| `CookieHttpSessionIdResolver` | Session ID 解析器，通过 Cookie 传递 Session ID |

### 2.4 Redis 数据结构

Spring Session 使用 Redis Hash 存储 Session 数据：

```
Key: spring:session:sessions:<sessionId>
├─ sessionAttr:<属性名>    → 序列化后的属性值
├─ creationTime            → 创建时间戳
├─ maxInactiveInterval     → 最大非活跃间隔(秒)
└─ lastAccessedTime        → 最后访问时间
```

---

## 3. 快速开始

### 3.1 项目结构

```
session-share-demo/
├── pom.xml
├── src/main/java/com/example/session/
│   ├── SessionShareApplication.java
│   ├── config/
│   │   └── SessionConfig.java
│   ├── controller/
│   │   └── AuthController.java
│   ├── entity/
│   │   └── User.java
│   ├── dto/
│   │   ├── LoginRequest.java
│   │   └── LoginResponse.java
│   └── service/
│       └── AuthService.java
└── src/main/resources/
    └── application.yml
```

### 3.2 依赖配置 (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>session-share-demo</artifactId>
    <version>1.0.0</version>
    <name>session-share-demo</name>
    <description>Spring Session + Redis 共享示例</description>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Session + Redis -->
        <dependency>
            <groupId>org.springframework.session</groupId>
            <artifactId>spring-session-data-redis</artifactId>
        </dependency>

        <!-- Spring Data Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Lettuce 连接池 (推荐) -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-pool2</artifactId>
        </dependency>

        <!-- Jackson 用于 JSON 序列化 -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>

        <!-- 引入 spring-boot-starter-validation 用于参数校验 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Lombok (可选，简化代码) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Spring Boot Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 3.3 配置文件 (application.yml)

```yaml
spring:
  application:
    name: session-share-demo

  # Redis 配置
  data:
    redis:
      host: localhost
      port: 6379
      password: # 如有密码请填写
      database: 0
      lettuce:
        pool:
          enabled: true
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms

  # Spring Session 配置
  session:
    store-type: redis
    timeout: 30m          # Session 超时时间
    redis:
      namespace: myapp:session  # Redis Key 前缀

server:
  port: 8080
  servlet:
    session:
      timeout: 30m
      cookie:
        name: MYAPP_SESSIONID  # Cookie 名称
        http-only: true
        secure: false          # 生产环境设为 true
        same-site: lax
```

---

## 4. 完整代码示例

### 4.1 主启动类

```java
package com.example.session;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Session 共享示例启动类
 *
 * @author Example
 * @since 2024
 */
@SpringBootApplication
public class SessionShareApplication {

    public static void main(String[] args) {
        SpringApplication.run(SessionShareApplication.class, args);
    }
}
```

### 4.2 Session 配置类

```java
package com.example.session.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Spring Session + Redis 配置类
 *
 * 核心功能:
 * 1. 启用 Redis 存储 Session
 * 2. 配置 Cookie 序列化方式
 * 3. 配置 Jackson 序列化器 (解决 Session 序列化问题)
 *
 * @EnableRedisHttpSession 注解自动配置:
 * - SessionRepositoryFilter
 * - RedisIndexedSessionRepository
 * - RedisConnectionFactory
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800) // 30分钟
public class SessionConfig {

    /**
     * 配置 Cookie 序列化器
     * 控制 Session ID 如何在 Cookie 中传输
     */
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        // Cookie 名称
        serializer.setCookieName("MYAPP_SESSIONID");
        // Cookie 路径
        serializer.setCookiePath("/");
        // HttpOnly 标志 (防止 XSS 攻击)
        serializer.setUseHttpOnlyCookie(true);
        // SameSite 策略
        serializer.setSameSite("Lax");
        // 生产环境应设为 true (仅 HTTPS 传输)
        // serializer.setUseSecureCookie(false);
        return serializer;
    }

    /**
     * 配置 Jackson ObjectMapper
     *
     * 解决 Session 序列化问题的关键配置:
     * 1. 启用类型信息 - 支持多态对象反序列化
     * 2. 注册 Java 8 时间模块 - 支持 LocalDateTime 等类型
     * 3. 禁用序列化失败特性 - 避免序列化失败
     */
    @Bean
    public ObjectMapper sessionObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 注册 Java 8 时间模块 (支持 LocalDateTime、Duration 等)
        mapper.registerModule(new JavaTimeModule());

        // 禁用将日期写为时间戳的功能，改为 ISO-8601 格式
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 启用类型信息 - 这是解决反序列化问题的关键!
        // 当 Session 中存储的是接口或抽象类时，需要此配置才能正确反序列化
        mapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );

        return mapper;
    }
}
```

### 4.3 用户实体类

```java
package com.example.session.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 用户实体类
 *
 * 注意: 存储在 Session 中的对象必须实现 Serializable 接口
 * 这是 Spring Session 序列化要求的一部分
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 密码 (JsonIgnore 防止序列化到 Session) */
    @JsonIgnore
    private String password;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    // 构造函数
    public User() {
    }

    public User(Long id, String username, String nickname, String email) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.email = email;
    }

    // Getter 和 Setter 方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", nickname='" + nickname + '\'' +
                ", email='" + email + '\'' +
                ", createTime=" + createTime +
                ", lastLoginTime=" + lastLoginTime +
                '}';
    }
}
```

### 4.4 登录请求 DTO

```java
package com.example.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求数据传输对象
 *
 * 使用 record 类型 (Java 16+) 简化代码
 * 也可以使用传统的类
 */
public record LoginRequest(

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度应在3-20个字符之间")
    String username,

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度应不少于6个字符")
    String password
) {
    // Java record 自动生成:
    // - 构造方法
    // - getUsername(), getPassword()
    // - equals(), hashCode(), toString()
}
```

### 4.5 登录响应 DTO

```java
package com.example.session.dto;

import com.example.session.entity.User;
import java.time.LocalDateTime;

/**
 * 登录响应数据传输对象
 */
public record LoginResponse(
    Long id,
    String username,
    String nickname,
    String email,
    LocalDateTime loginTime,
    String sessionId
) {
    /**
     * 从 User 实体创建响应
     */
    public static LoginResponse from(User user, String sessionId) {
        return new LoginResponse(
            user.getId(),
            user.getUsername(),
            user.getNickname(),
            user.getEmail(),
            LocalDateTime.now(),
            sessionId
        );
    }
}
```

### 4.6 认证服务类

```java
package com.example.session.service;

import com.example.session.entity.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证服务类
 *
 * 模拟用户认证逻辑
 * 实际项目中应连接数据库进行用户验证
 */
@Service
public class AuthService {

    /**
     * 模拟用户数据库
     * 格式: username -> User
     */
    private static final Map<String, User> MOCK_USERS = new ConcurrentHashMap<>();

    static {
        // 初始化测试用户
        User user1 = new User(1L, "admin", "管理员", "admin@example.com");
        user1.setPassword("123456");
        user1.setCreateTime(LocalDateTime.now().minusDays(30));
        MOCK_USERS.put("admin", user1);

        User user2 = new User(2L, "user", "普通用户", "user@example.com");
        user2.setPassword("123456");
        user2.setCreateTime(LocalDateTime.now().minusDays(10));
        MOCK_USERS.put("user", user2);
    }

    /**
     * 用户登录验证
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录成功返回用户信息，失败返回空
     */
    public Optional<User> authenticate(String username, String password) {
        User user = MOCK_USERS.get(username);

        if (user == null) {
            return Optional.empty();
        }

        // 验证密码 (实际项目应使用 BCrypt 等加密方式)
        if (!user.getPassword().equals(password)) {
            return Optional.empty();
        }

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());

        return Optional.of(user);
    }

    /**
     * 根据用户ID获取用户
     */
    public Optional<User> findById(Long id) {
        return MOCK_USERS.values().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();
    }

    /**
     * 根据用户名获取用户
     */
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(MOCK_USERS.get(username));
    }
}
```

### 4.7 认证控制器

```java
package com.example.session.controller;

import com.example.session.dto.LoginRequest;
import com.example.session.dto.LoginResponse;
import com.example.session.entity.User;
import com.example.session.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 *
 * 处理用户登录、登出、状态查询等请求
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    /** Session 中存储用户信息的 key */
    private static final String SESSION_USER_KEY = "LOGIN_USER";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录
     *
     * 流程:
     * 1. 验证用户名密码
     * 2. 创建 Session 并存储用户信息
     * 3. 返回登录响应 (包含 Session ID)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpSession session) {

        log.info("登录请求: username={}, sessionId={}",
                request.username(), session.getId());

        // 1. 验证用户凭据
        var authenticatedUser = authService.authenticate(
                request.username(),
                request.password()
        );

        if (authenticatedUser.isEmpty()) {
            log.warn("登录失败: 用户名或密码错误, username={}", request.username());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "用户名或密码错误"
                    ));
        }

        User user = authenticatedUser.get();

        // 2. 将用户信息存储到 Session
        // 重要: Spring Session 会自动将这个对象序列化到 Redis
        session.setAttribute(SESSION_USER_KEY, user);

        // 3. 更新最后登录时间 (同步到数据库)
        user.setLastLoginTime(java.time.LocalDateTime.now());

        log.info("登录成功: userId={}, sessionId={}, 创建时间={}",
                user.getId(), session.getId(), session.getCreationTime());

        // 4. 返回登录响应
        return ResponseEntity.ok(LoginResponse.from(user, session.getId()));
    }

    /**
     * 用户登出
     *
     * 流程:
     * 1. 从 Session 中移除用户信息
     * 2. 使 Session 失效 (可选)
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        User user = (User) session.getAttribute(SESSION_USER_KEY);

        if (user != null) {
            log.info("用户登出: userId={}, sessionId={}", user.getId(), session.getId());
        }

        // 移除 Session 中的用户信息
        session.removeAttribute(SESSION_USER_KEY);

        // 使整个 Session 失效 (清空所有数据)
        session.invalidate();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "登出成功"
        ));
    }

    /**
     * 获取当前登录状态
     *
     * 检查 Session 中是否存有用户信息
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(HttpSession session) {
        User user = (User) session.getAttribute(SESSION_USER_KEY);

        if (user == null) {
            return ResponseEntity.ok(Map.of(
                    "authenticated", false,
                    "sessionId", session.getId()
            ));
        }

        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "sessionId", session.getId(),
                "user", Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "nickname", user.getNickname(),
                        "email", user.getEmail(),
                        "lastLoginTime", user.getLastLoginTime()
                )
        ));
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        User user = (User) session.getAttribute(SESSION_USER_KEY);

        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "未登录或 Session 已过期"
                    ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", user
        ));
    }

    /**
     * 测试: 在 Session 中存储自定义数据
     */
    @PostMapping("/test/store")
    public ResponseEntity<?> storeTestData(
            HttpSession session,
            @RequestBody Map<String, Object> data) {

        // 存储各种类型的数据到 Session
        session.setAttribute("stringData", "Hello Session");
        session.setAttribute("numberData", 12345);
        session.setAttribute("booleanData", true);
        session.setAttribute("objectData", data);
        session.setAttribute("listData", java.util.List.of("a", "b", "c"));

        log.info("存储测试数据: sessionId={}, 数据={}", session.getId(), data);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "数据已存储到 Session",
                "sessionId", session.getId()
        ));
    }

    /**
     * 测试: 从 Session 中读取数据
     */
    @GetMapping("/test/read")
    public ResponseEntity<?> readTestData(HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        // 读取所有存储的数据
        result.put("stringData", session.getAttribute("stringData"));
        result.put("numberData", session.getAttribute("numberData"));
        result.put("booleanData", session.getAttribute("booleanData"));
        result.put("objectData", session.getAttribute("objectData"));
        result.put("listData", session.getAttribute("listData"));
        result.put("sessionId", session.getId());
        result.put("creationTime", session.getCreationTime());
        result.put("lastAccessedTime", session.getLastAccessedTime());
        result.put("maxInactiveInterval", session.getMaxInactiveInterval());

        return ResponseEntity.ok(result);
    }
}
```

### 4.8 健康检查控制器

```java
package com.example.session.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查控制器
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "timestamp", java.time.Instant.now().toString()
        );
    }
}
```

---

## 5. Session 序列化问题与 Jackson 配置

### 5.1 问题描述

在将对象存储到 Session 时，Spring Session 需要将对象序列化为字节流存储到 Redis。如果配置不当，会遇到以下问题:

```
1. 反序列化失败: Could not deserialize object...
2. 类型信息丢失: ClassNotFoundException
3. 多态对象问题: 接口/抽象类无法正确反序列化
4. 日期类型问题: LocalDateTime 序列化为数组而非字符串
```

### 5.2 原因分析

Spring Session 默认使用 Java 原生序列化，存在以下问题:

| 问题 | 原因 | 影响 |
|------|------|------|
| 多态反序列化失败 | 缺少类型信息 | 存储接口实现类时无法还原 |
| 类型信息丢失 | Java 序列化包含完整类名 | 类路径变化后无法反序列化 |
| 日期格式问题 | 默认序列化为时间戳 | 可读性差，时区问题 |

### 5.3 Jackson 序列化配置解决方案

```java
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class SessionConfig {

    /**
     * 自定义 Jackson 序列化器
     * 解决 Session 序列化问题的完整配置
     */
    @Bean
    public ObjectMapper sessionObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 1. 注册 Java 8 时间模块
        // 支持 LocalDateTime、Duration、Instant 等类型的序列化
        mapper.registerModule(new JavaTimeModule());

        // 2. 禁用将日期写为时间戳
        // 改用 ISO-8601 格式: "2024-01-15T10:30:00"
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 3. 启用默认类型信息 (关键配置!)
        // 这允许 Jackson 在序列化时保存类型信息
        // 反序列化时能够正确重建对象类型
        mapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );

        // 4. 忽略未知属性
        // 当类结构变化后，旧数据仍能正确反序列化
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 5. 禁用序列化空对象时报错
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        return mapper;
    }

    /**
     * 配置 Redis 序列化器
     * 使用我们自定义的 Jackson 序列化器
     */
    @Bean
    public RedisSerializer<Object> redisSerializer(ObjectMapper mapper) {
        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}
```

### 5.4 实体类序列化注解

除了全局配置，还可以在实体类上使用注解:

```java
/**
 * 需要存储到 Session 的实体类
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.CLASS,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@class"
)
@JsonIgnoreProperties(ignoreUnknown = true)
public class User implements Serializable {

    // ...

    @JsonIgnore // 排除敏感字段
    private String password;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") // 自定义日期格式
    private LocalDateTime createTime;
}
```

### 5.5 常见序列化问题排查

| 问题现象 | 可能原因 | 解决方案 |
|----------|----------|----------|
| 反序列化得到的是 LinkedHashMap | 未启用类型信息 | 配置 `activateDefaultTyping()` |
| LocalDateTime 变成数组 | 未注册 JavaTimeModule | 添加 `mapper.registerModule(new JavaTimeModule())` |
| 字段值全部为 null | 类结构变化 | 使用 `@JsonIgnoreProperties(ignoreUnknown = true)` |
| 反序列化报 ClassNotFoundException | 类包名变更 | 保持类包名不变或数据迁移 |

---

## 6. 实战案例：用户登录状态跨服务共享

### 6.1 场景描述

假设我们有一个微服务架构:

```
┌─────────────────────────────────────────────────────────────────┐
│                        用户请求流程                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   用户 ──► 网关服务 ──► 认证服务 ──► 业务服务                     │
│              (8080)      (8081)      (8082)                      │
│                                                                  │
│                    Session 共享 ──► Redis (6379)                 │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 多服务 Session 共享配置

**认证服务 (8081) 和业务服务 (8082) 共用同一 Redis**:

```yaml
# 认证服务 application.yml
spring:
  session:
    store-type: redis
    timeout: 30m
    redis:
      namespace: myapp:session

# 业务服务 application.yml
spring:
  session:
    store-type: redis
    timeout: 30m
    redis:
      namespace: myapp:session  # 必须与认证服务一致!
```

### 6.3 跨服务获取登录用户

在实际业务服务中，可以通过 Spring 注入方式获取当前登录用户:

```java
/**
 * Session 中存储的用户信息包装类
 * 提供从 HttpSession 中获取当前登录用户的便捷方法
 */
@Component
public class SessionUserHolder {

    private static final String SESSION_USER_KEY = "LOGIN_USER";

    private final HttpSession httpSession;

    public SessionUserHolder(HttpSession httpSession) {
        this.httpSession = httpSession;
    }

    /**
     * 获取当前登录用户
     *
     * @return 当前用户，如果未登录返回空
     */
    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(
            (User) httpSession.getAttribute(SESSION_USER_KEY)
        );
    }

    /**
     * 获取当前登录用户的 ID
     */
    public Optional<Long> getCurrentUserId() {
        return getCurrentUser().map(User::getId);
    }

    /**
     * 检查是否已登录
     */
    public boolean isAuthenticated() {
        return getCurrentUser().isPresent();
    }

    /**
     * 要求用户已登录，如果未登录则抛出异常
     */
    public User requireAuthentication() {
        return getCurrentUser()
            .orElseThrow(() -> new UnauthorizedException("请先登录"));
    }
}
```

### 6.4 业务服务示例

```java
/**
 * 订单业务服务
 * 需要验证用户登录状态
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final SessionUserHolder sessionUserHolder;
    private final OrderService orderService;

    public OrderController(SessionUserHolder sessionUserHolder, OrderService orderService) {
        this.sessionUserHolder = sessionUserHolder;
        this.orderService = orderService;
    }

    /**
     * 创建订单
     * 必须登录才能访问
     */
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        // 从 Session 获取当前登录用户
        User currentUser = sessionUserHolder.requireAuthentication();

        // 创建订单
        Order order = orderService.createOrder(currentUser.getId(), request);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", order
        ));
    }

    /**
     * 获取当前用户的订单列表
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyOrders() {
        User currentUser = sessionUserHolder.requireAuthentication();

        List<Order> orders = orderService.getUserOrders(currentUser.getId());

        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", orders
        ));
    }
}
```

### 6.5 统一响应与异常处理

```java
/**
 * 未授权异常
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<?> handleUnauthorized(UnauthorizedException e) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(Map.of(
                "success", false,
                "error", "UNAUTHORIZED",
                "message", e.getMessage()
            ));
    }

    // ... 其他异常处理
}
```

---

## 7. 关键技术点说明

### 7.1 @EnableRedisHttpSession 注解参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| maxInactiveIntervalInSeconds | Session 最大非活跃时间(秒) | 1800 (30分钟) |
| redisNamespace | Redis Key 命名空间 | spring:session |
| redisFlushMode | Redis 刷新模式 | ON_SAVE |
| saveMode | 保存模式 | CHANGEABLE_WITH_WRITE |

### 7.2 Session 事件监听

Spring Session 支持事件监听，可以在 Session 创建、销毁、访问时执行特定逻辑:

```java
/**
 * Session 事件监听器
 */
@Component
public class SessionEventListener {

    private static final Logger log = LoggerFactory.getLogger(SessionEventListener.class);

    /**
     * Session 创建事件
     */
    @EventListener
    public void onSessionCreated(SessionCreatedEvent event) {
        String sessionId = event.getSessionId();
        log.info("Session 创建: sessionId={}, 创建时间={}",
                sessionId, event.getTimestamp());
    }

    /**
     * Session 删除事件
     */
    @EventListener
    public void onSessionDeleted(SessionDeletedEvent event) {
        String sessionId = event.getSessionId();
        log.info("Session 删除: sessionId={}", sessionId);
    }

    /**
     * Session 过期事件
     */
    @EventListener
    public void onSessionExpired(SessionExpiredEvent event) {
        String sessionId = event.getSessionId();
        log.info("Session 过期: sessionId={}", sessionId);
    }

    /**
     * Session 访问事件
     */
    @EventListener
    public void onSessionAccess(SessionAccessedEvent event) {
        String sessionId = event.getSessionId();
        log.debug("Session 访问: sessionId={}", sessionId);
    }
}
```

### 7.3 自定义 Session 属性

```java
/**
 * Session 中存储的用户信息 Key 定义
 * 统一管理 Session 属性名，避免硬编码
 */
public final class SessionAttributes {

    private SessionAttributes() {}

    /** 登录用户 */
    public static final String LOGIN_USER = "LOGIN_USER";

    /** 登录时的 IP */
    public static final String LOGIN_IP = "LOGIN_IP";

    /** 用户权限列表 */
    public static final String PERMISSIONS = "PERMISSIONS";

    /** 用户菜单 */
    public static final String MENUS = "MENUS";
}
```

### 7.4 Cookie 配置详解

```java
@Bean
public CookieSerializer cookieSerializer() {
    DefaultCookieSerializer serializer = new DefaultCookieSerializer();

    // 1. Cookie 名称
    serializer.setCookieName("MYAPP_SESSIONID");

    // 2. Cookie 路径
    serializer.setCookiePath("/");

    // 3. HttpOnly 标志 (防止 JavaScript 访问)
    // 生产环境建议设为 true
    serializer.setUseHttpOnlyCookie(true);

    // 4. SameSite 策略
    // - Strict: 完全禁止跨域
    // - Lax: GET 请求允许跨域，POST 不允许
    // - None: 允许所有跨域 (需配合 Secure=true)
    serializer.setSameSite("Lax");

    // 5. Secure 标志 (仅 HTTPS 传输)
    // 生产环境必须设为 true
    serializer.setUseSecureCookie(false);

    // 6. Cookie 有效期 (秒)
    // -1 表示浏览器关闭时删除
    serializer.setCookieMaxAge(-1);

    // 7. 域名配置 (如需跨子域共享)
    // serializer.setDomainName(".example.com");

    return serializer;
}
```

### 7.5 集群部署注意事项

```
┌─────────────────────────────────────────────────────────────────┐
│                      集群部署架构                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐                   │
│  │ 节点1    │    │ 节点2    │    │ 节点3    │                   │
│  │ 8081     │    │ 8082     │    │ 8083     │                   │
│  └────┬─────┘    └────┬─────┘    └────┬─────┘                   │
│       │               │               │                          │
│       └───────────────┼───────────────┘                          │
│                       ▼                                          │
│              ┌────────────────┐                                  │
│              │     Redis      │                                  │
│              │  集群/哨兵模式  │                                  │
│              └────────────────┘                                  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

Redis 集群配置示例:

```yaml
spring:
  data:
    redis:
      # 集群模式
      # cluster:
      #   nodes:
      #     - 192.168.1.101:6379
      #     - 192.168.1.102:6379
      #     - 192.168.1.103:6379

      # 哨兵模式
      sentinel:
        master: mymaster
        nodes:
          - 192.168.1.101:26379
          - 192.168.1.102:26379
          - 192.168.1.103:26379
```

---

## 8. 常见问题与解决方案

### 8.1 Session 丢失问题

| 可能原因 | 排查方法 | 解决方案 |
|----------|----------|----------|
| Redis 连接失败 | 检查 Redis 日志 | 确保 Redis 正常运行 |
| Session 过期 | 检查 maxInactiveInterval | 调整过期时间 |
| Cookie 被阻止 | 浏览器检查 Cookie | 确认 Cookie 策略 |
| 域名/路径不一致 | 对比 Cookie 配置 | 统一 Cookie 配置 |

### 8.2 序列化失败问题

| 错误信息 | 原因 | 解决方案 |
|----------|------|----------|
| Could not deserialize | 缺少类型信息 | 配置 `activateDefaultTyping()` |
| ClassNotFoundException | 类包名变化 | 保持类结构不变 |
| No suitable constructor | 缺少无参构造 | 添加无参构造函数或使用 record |

### 8.3 性能问题

| 问题 | 优化方案 |
|------|----------|
| Session 数据过大 | 只存储必要数据，大数据存 Redis |
| 序列化耗时 | 使用 Kryo 等高效序列化库 |
| Redis 连接数过多 | 配置 Lettuce 连接池 |

### 8.4 安全建议

1. **敏感数据不存 Session**: 密码等敏感信息不要存储在 Session 中
2. **Session ID 安全**: 设置 `httpOnly=true`，生产环境设置 `secure=true`
3. **定期更换 Session ID**: 用户登录后更换 Session ID
4. **合理设置过期时间**: 根据业务需求设置适当的 Session 超时时间

---

## 附录: API 测试示例

### 登录并创建 Session

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' \
  -c cookies.txt

# 响应示例:
# {
#   "id": 1,
#   "username": "admin",
#   "nickname": "管理员",
#   "email": "admin@example.com",
#   "loginTime": "2024-01-15T10:30:00",
#   "sessionId": "abc123..."
# }
```

### 检查登录状态

```bash
curl http://localhost:8080/api/auth/status \
  -b cookies.txt

# 响应示例:
# {
#   "authenticated": true,
#   "sessionId": "abc123...",
#   "user": {
#     "id": 1,
#     "username": "admin",
#     ...
#   }
# }
```

### 登出

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -b cookies.txt
```

---

## 参考资料

- [Spring Session 官方文档](https://docs.spring.io/spring-session/reference/)
- [Spring Data Redis 官方文档](https://docs.spring.io/spring-data/data-redis/docs/)
- [Redis 官方文档](https://redis.io/documentation)
