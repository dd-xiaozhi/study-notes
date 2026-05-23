---
title: 第八章：微服务安全性
description: 掌握微服务安全体系设计与实现
---

# 第八章：微服务安全性

## 本章目录

- [8.1 微服务安全概述](#81-微服务安全概述)
  - [8.1.1 安全的核心目标](#811-安全的核心目标)
  - [8.1.2 微服务安全面临的挑战](#812-微服务安全面临的挑战)
  - [8.1.3 安全架构总览](#813-安全架构总览)
- [8.2 认证与授权](#82-认证与授权)
  - [8.2.1 OAuth2.0 四种授权模式](#821-oauth20-四种授权模式)
  - [8.2.2 JWT Token 结构与验证](#822-jwt-token-结构与验证)
  - [8.2.3 OpenID Connect](#823-openid-connect)
- [8.3 服务间认证](#83-服务间认证)
  - [8.3.1 mTLS 双向TLS认证](#831-mtls-双向tls认证)
  - [8.3.2 SPIFFE/SPIRE 身份体系](#832-spiffespire-身份体系)
  - [8.3.3 JWT 降级模式](#833-jwt-降级模式)
- [8.4 API 安全](#84-api-安全)
  - [8.4.1 API Key 与签名认证](#841-api-key-与签名认证)
  - [8.4.2 HTTPS/TLS 加密](#842-https tls-加密)
  - [8.4.3 CORS 配置](#843-cors-配置)
- [8.5 敏感数据管理](#85-敏感数据管理)
  - [8.5.1 密钥管理（Vault、KMS）](#851-密钥管理vaultkms)
  - [8.5.2 加密存储](#852-加密存储)
  - [8.5.3 环境变量与 Secret](#853-环境变量与-secret)
- [8.6 安全最佳实践](#86-安全最佳实践)
  - [8.6.1 最小权限原则](#861-最小权限原则)
  - [8.6.2 纵深防御](#862-纵深防御)
  - [8.6.3 安全扫描](#863-安全扫描)
- [本章小结](#本章小结)
- [思考题](#思考题)

---

## 8.1 微服务安全概述

### 8.1.1 安全的核心目标

信息安全有三大核心目标，通常称为 **CIA 三元组**：

| 目标 | 英文 | 说明 |
|------|------|------|
| 机密性 | Confidentiality | 防止未授权访问敏感信息 |
| 完整性 | Integrity | 确保数据不被未授权篡改 |
| 可用性 | Availability | 确保系统和数据可被合法用户访问 |

在微服务架构中，还需要额外考虑：

- **身份认证**（Authentication）：验证请求来源的身份
- **授权**（Authorization）：判断已认证身份是否有权访问资源
- **审计**（Auditing）：记录安全相关事件便于追溯

### 8.1.2 微服务安全面临的挑战

微服务架构相比单体应用面临更多的安全挑战：

```mermaid
mindmap
  root((微服务安全挑战))
    分布式安全
      多个服务独立认证
      统一策略难以实施
      边界模糊
    服务间通信
      API暴露面大
      内部服务调用安全
      协议多样性
    密钥管理
      大量服务实例
      密钥轮换困难
      密钥分发风险
    身份管理
      去中心化身份
      跨服务会话
      SSO复杂性
    合规要求
      数据保护法规
      审计日志要求
      跨境数据流

```

**与传统单体应用的关键差异：**

| 方面 | 单体应用 | 微服务架构 |
|------|----------|------------|
| 认证入口 | 单一登录入口 | 每个服务可能独立认证 |
| 会话管理 | 共享会话存储 | 需要分布式会话或无状态方案 |
| 信任边界 | 内部方法调用天然可信 | 服务间调用需要显式信任 |
| 攻击面 | 集中在一处 | 分散在多个服务 |

### 8.1.3 安全架构总览

```mermaid
graph TB
    subgraph "客户端层"["<b style='color:#fff'>客户端层</b>"]
        Client["<b style='color:#fff'>浏览器/移动端/其他客户端</b>"]
    end

    subgraph "网关层"["<b style='color:#fff'>网关层</b>"]
        GW["<b style='color:#fff'>API Gateway</b>"]
        GW --> Auth1["<b style='color:#fff'>认证服务</b>"]
    end

    subgraph "服务网格层"["<b style='color:#fff'>服务网格层</b>"]
        SM["<b style='color:#fff'>Service Mesh</b>"]
        SM --> Sidecar["<b style='color:#fff'>Sidecar Proxy</b>"]
    end

    subgraph "微服务层"["<b style='color:#fff'>微服务层</b>"]
        MS1["<b style='color:#fff'>用户服务</b>"]
        MS2["<b style='color:#fff'>订单服务</b>"]
        MS3["<b style='color:#fff'>支付服务</b>"]
    end

    subgraph "数据层"["<b style='color:#fff'>数据层</b>"]
        DB[("<b style='color:#fff'>数据库</b>")]
        Cache[("<b style='color:#fff'>缓存</b>")]
        Vault["<b style='color:#fff'>密钥Vault</b>"]
        KMS["<b style='color:#fff'>KMS服务</b>"]
    end

    subgraph "外部服务"["<b style='color:#fff'>外部服务</b>"]
        IDP["<b style='color:#fff'>身份提供商<br/>OAuth2/OIDC</b>"]
    end

    Client -->|HTTPS| GW
    GW -->|验证JWT| Auth1
    Auth1 -->|获取Token| IDP
    Sidecar -.->|mTLS| Sidecar
    MS1 -.->|JWT/mTLS| MS2
    MS2 -.->|JWT/mTLS| MS3
    MS3 -.->|加密读写| DB
    MS1 -.->|读写| Cache

    style Client fill:#0ea5e9,stroke:#38bdf8,stroke-width:2px,color:#ffffff
    style GW fill:#6366f1,stroke:#818cf8,stroke-width:2px,color:#ffffff
    style Auth1 fill:#8b5cf6,stroke:#a78bfa,stroke-width:2px,color:#ffffff
    style SM fill:#06b6d4,stroke:#22d3ee,stroke-width:2px,color:#ffffff
    style Sidecar fill:#14b8a6,stroke:#2dd4bf,stroke-width:2px,color:#ffffff
    style MS1 fill:#f59e0b,stroke:#fbbf24,stroke-width:2px,color:#ffffff
    style MS2 fill:#ef4444,stroke:#f87171,stroke-width:2px,color:#ffffff
    style MS3 fill:#ec4899,stroke:#f472b6,stroke-width:2px,color:#ffffff
    style DB fill:#10b981,stroke:#34d399,stroke-width:2px,color:#ffffff
    style Cache fill:#64748b,stroke:#94a3b8,stroke-width:2px,color:#ffffff
    style Vault fill:#dc2626,stroke:#ef4444,stroke-width:2px,color:#ffffff
    style KMS fill:#7c3aed,stroke:#a78bfa,stroke-width:2px,color:#ffffff
    style IDP fill:#2563eb,stroke:#60a5fa,stroke-width:2px,color:#ffffff
```

---

## 8.2 认证与授权

### 8.2.1 OAuth2.0 四种授权模式

OAuth 2.0 是一个授权框架，允许第三方应用获取对用户资源的有限访问权限。以下是四种授权模式：

#### 8.2.1.1 授权码模式（Authorization Code Grant）

**最安全的授权模式，适合有后端服务器的应用。**

```mermaid
sequenceDiagram
    participant User as 👤 用户
    participant App as 📱 客户端应用
    participant Auth as 🔐 授权服务器
    participant API as 🌐 资源服务器

    App->>+User: 引导用户访问授权页面
    User->>+Auth: 用户同意授权
    Auth->>-App: 返回授权码
    App->>+Auth: 用授权码兑换访问令牌
    Auth->>-App: 返回访问令牌 + 刷新令牌
    App->>+API: 携带令牌请求资源
    API->>-App: 返回受保护资源

    Note over User,API: 授权码模式时序图
```

**完整流程代码示例（Java Spring Boot）：**

```java
// 授权服务器配置
@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.inMemory()
            .withClient("web-app")
            .secret("web-app-secret")
            .authorizedGrantTypes("authorization_code", "refresh_token")
            .scopes("read", "write")
            .redirectUris("http://localhost:8080/callback")
            .autoApprove(false);
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
        endpoints
            .authorizationCodeServices(authorizationCodeServices())
            .tokenStore(tokenStore())
            .authenticationManager(authenticationManager);
    }

    @Bean
    public AuthorizationCodeServices authorizationCodeServices() {
        return new JdbcAuthorizationCodeServices(dataSource);
    }

    @Bean
    public TokenStore tokenStore() {
        return new JwtTokenStore(jwtAccessTokenConverter());
    }

    @Bean
    public JwtAccessTokenConverter jwtAccessTokenConverter() {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        converter.setSigningKey("my-signing-key");
        return converter;
    }
}
```

**Go 实现示例：**

```go
// OAuth2 授权码处理（使用 golang.org/x/oauth2）
package main

import (
    "context"
    "fmt"
    "log"
    "net/http"

    "golang.org/x/oauth2"
    "golang.org/x/oauth2/github"
)

var (
    oauth2Config = &oauth2.Config{
        ClientID:     "your-client-id",
        ClientSecret: "your-client-secret",
        Scopes:       []string{"read", "write"},
        Endpoint:     github.Endpoint,
        RedirectURL:  "http://localhost:8080/callback",
    }
)

func main() {
    http.HandleFunc("/", handleHome)
    http.HandleFunc("/login", handleLogin)
    http.HandleFunc("/callback", handleCallback)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func handleHome(w http.ResponseWriter, r *http.Request) {
    html := `<html><body><a href="/login">Login with GitHub</a></body></html>`
    fmt.Fprint(w, html)
}

func handleLogin(w http.ResponseWriter, r *http.Request) {
    url := oauth2Config.AuthCodeURL("state", oauth2.AccessTypeOffline)
    http.Redirect(w, r, url, http.StatusTemporaryRedirect)
}

func handleCallback(w http.ResponseWriter, r *http.Request) {
    code := r.URL.Query().Get("code")

    token, err := oauth2Config.Exchange(context.Background(), code)
    if err != nil {
        http.Error(w, "Code exchange failed", http.StatusInternalServerError)
        return
    }

    // token.AccessToken 就是获得的访问令牌
    fmt.Fprintf(w, "Access Token: %s\nRefresh Token: %s",
        token.AccessToken, token.RefreshToken)
}
```

#### 8.2.1.2 简化模式（Implicit Grant）

**已不推荐使用，适合纯前端单页应用（SPA）。** 令牌直接在URL中返回，存在安全风险。

#### 8.2.1.3 密码模式（Resource Owner Password Credentials Grant）

**适用于受信任的第一方应用。**

```java
// Spring Security 密码模式配置
@Override
public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
    clients.inMemory()
        .withClient("trusted-app")
        .secret("trusted-app-secret")
        .authorizedGrantTypes("password")
        .scopes("read", "write")
        .accessTokenValiditySeconds(3600);
}
```

**Go 实现：**

```go
// 密码模式令牌获取
func getTokenWithPassword(username, password string) (*oauth2.Token, error) {
    config := &oauth2.Config{
        ClientID:     "trusted-app",
        ClientSecret: "trusted-app-secret",
        Scopes:       []string{"read", "write"},
        Endpoint:     oauth2.Endpoint{TokenURL: "http://auth-server/oauth/token"},
    }

    return config.PasswordCredentialsToken(context.Background(), username, password)
}
```

#### 8.2.1.4 客户端凭证模式（Client Credentials Grant）

**适用于服务间通信，不涉及用户。**

```mermaid
sequenceDiagram
    participant S1 as 🖥️ 服务A
    participant Auth as 🔐 授权服务器
    participant S2 as 🖥️ 服务B

    S1->>+Auth: 使用客户端凭证请求令牌
    Auth->>-S1: 返回访问令牌
    S1->>+S2: 携带令牌调用服务B
    S2->>-S1: 返回受保护资源
```

**Java 实现：**

```java
@RestController
public class ClientCredentialsController {

    @Value("${security.oauth2.client.client-id}")
    private String clientId;

    @Value("${security.oauth2.client.client-secret}")
    private String clientSecret;

    private final OAuth2RestTemplate restTemplate;

    public ClientCredentialsController(OAuth2RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/call-service-b")
    public String callServiceB() {
        // OAuth2RestTemplate 自动处理令牌获取和刷新
        return restTemplate.getForObject("http://service-b/api/data", String.class);
    }
}

// 服务B的配置（资源服务器）
@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
            .anyRequest().authenticated();
    }
}
```

**Go 实现：**

```go
// 客户端凭证模式 - 服务间认证
package main

import (
    "context"
    "fmt"
    "log"
    "net/http"

    "golang.org/x/oauth2"
    "golang.org/x/oauth2/clientcredentials"
)

func main() {
    // 配置客户端凭证
    config := clientcredentials.Config{
        ClientID:     "service-a",
        ClientSecret: "service-a-secret",
        TokenURL:     "http://auth-server/oauth/token",
        Scopes:       []string{"service:read", "service:write"},
    }

    // 创建 HTTP 客户端，自动处理令牌
    client := config.Client(context.Background())

    // 使用客户端调用其他服务
    resp, err := client.Get("http://service-b/api/protected")
    if err != nil {
        log.Fatal(err)
    }
    defer resp.Body.Close()

    fmt.Printf("Response Status: %s\n", resp.Status)
}
```

#### 8.2.1.5 四种模式对比

| 模式 | 适用场景 | 安全性 | 令牌位置 | 刷新令牌 |
|------|----------|--------|----------|----------|
| 授权码模式 | 有后端服务器的应用 | 高 | 服务器到服务器 | 支持 |
| 简化模式 | 纯前端SPA（不推荐） | 低 | URL片段 | 不支持 |
| 密码模式 | 受信任的第一方应用 | 中 | 服务器到服务器 | 支持 |
| 客户端凭证 | 服务间通信 | 高 | 服务器到服务器 | 支持 |

### 8.2.2 JWT Token 结构与验证

#### 8.2.2.1 JWT 结构

JWT（JSON Web Token）由三部分组成，用点号分隔：

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

| 部分 | 示例 | 说明 |
|------|------|------|
| Header | `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9` | 包含算法和类型 |
| Payload | `eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ` | 声明数据 |
| Signature | `SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c` | 签名验证 |

**Header 解码后：**

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload 解码后：**

```json
{
  "sub": "1234567890",
  "name": "John Doe",
  "iat": 1516239022,
  "exp": 1516242622,
  "iss": "auth-server",
  "aud": "my-app",
  "roles": ["admin", "user"]
}
```

**标准声明：**

| 声明 | 说明 |
|------|------|
| `iss` | 签发者（Issuer） |
| `sub` | 主题（Subject），通常为用户ID |
| `aud` | 受众（Audience），应用ID |
| `exp` | 过期时间（Expiration） |
| `nbf` | 生效时间（Not Before） |
| `iat` | 签发时间（Issued At） |
| `jti` | JWT唯一标识 |

#### 8.2.2.2 JWT 签名算法

```mermaid
graph LR
    A["<b style='color:#fff'>Header</b>"] --> C["<b style='color:#fff'>Base64URL编码</b>"]
    B["<b style='color:#fff'>Payload</b>"] --> D["<b style='color:#fff'>Base64URL编码</b>"]
    C --> E["<b style='color:#22d3ee'>组合<br/>Header.Payload</b>"]
    D --> E
    E --> F["<b style='color:#f472b6'>使用密钥签名</b>"]
    F --> G["<b style='color:#a78bfa'>Signature</b>"]

    style A fill:#0ea5e9,stroke:#38bdf8,stroke-width:2px,color:#ffffff
    style B fill:#0ea5e9,stroke:#38bdf8,stroke-width:2px,color:#ffffff
    style C fill:#6366f1,stroke:#818cf8,stroke-width:2px,color:#ffffff
    style D fill:#6366f1,stroke:#818cf8,stroke-width:2px,color:#ffffff
    style E fill:#06b6d4,stroke:#22d3ee,stroke-width:2px,color:#ffffff
    style F fill:#ec4899,stroke:#f472b6,stroke-width:2px,color:#ffffff
    style G fill:#8b5cf6,stroke:#a78bfa,stroke-width:2px,color:#ffffff
```

**常用算法：**

| 算法 | 类型 | 说明 | 安全性 |
|------|------|------|--------|
| HS256 | 对称 | 使用共享密钥签名 | 中 |
| HS384 | 对称 | 同上，更长摘要 | 中 |
| RS256 | 非对称 | 私钥签名，公钥验证 | 高 |
| ES256 | 非对称 | ECDSA签名，性能好 | 高 |

#### 8.2.2.3 Java JWT 验证

```java
// 使用 JJWT 库进行 JWT 验证
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final SecretKey secretKey = Keys.hmacShaKeyFor(
        "my-very-secure-secret-key-that-is-at-least-256-bits-long".getBytes(StandardCharsets.UTF_8)
    );

    // 生成 Token
    public String generateToken(String userId, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 3600000); // 1小时

        return Jwts.builder()
            .subject(userId)
            .claim("roles", roles)
            .issuer("auth-service")
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact();
    }

    // 验证 Token 并提取信息
    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException("Token 已过期");
        } catch (JwtException e) {
            throw new InvalidTokenException("无效的 Token");
        }
    }

    // 提取用户ID
    public String getUserIdFromToken(String token) {
        return validateToken(token).getSubject();
    }

    // 提取角色
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return validateToken(token).get("roles", List.class);
    }

    // 验证是否包含特定角色
    public boolean hasRole(String token, String role) {
        List<String> roles = getRolesFromToken(token);
        return roles != null && roles.contains(role);
    }
}
```

**使用公钥/私钥对（RS256）：**

```java
// 生成 RSA 密钥对
KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
keyGen.initialize(2048);
KeyPair pair = keyGen.generateKeyPair();
PrivateKey privateKey = pair.getPrivate();
PublicKey publicKey = pair.getPublic();

// 签名（私钥）
String token = Jwts.builder()
    .subject(userId)
    .signWith(privateKey, Jwts.SIG.RS256)
    .compact();

// 验证（公钥）
Claims claims = Jwts.parser()
    .verifyWith(publicKey)
    .build()
    .parseSignedClaims(token)
    .getPayload();
```

#### 8.2.2.4 Go JWT 实现

```go
package main

import (
    "fmt"
    "time"

    "github.com/golang-jwt/jwt/v5"
)

// 自定义声明
type Claims struct {
    UserID string   `json:"sub"`
    Roles  []string `json:"roles"`
    jwt.RegisteredClaims
}

// 生成 Token（使用私钥）
func GenerateToken(userID string, roles []string, privateKey []byte) (string, error) {
    claims := Claims{
        UserID: userID,
        Roles:  roles,
        RegisteredClaims: jwt.RegisteredClaims{
            Issuer:    "auth-service",
            Subject:   userID,
            IssuedAt:  jwt.NewNumericDate(time.Now()),
            ExpiresAt: jwt.NewNumericDate(time.Now().Add(1 * time.Hour)),
        },
    }

    // 使用 HS256 对称签名
    token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
    return token.SignedString([]byte("my-secret-key"))
}

// 验证 Token（使用公钥）
func ValidateToken(tokenString string, publicKey []byte) (*Claims, error) {
    token, err := jwt.ParseWithClaims(tokenString, &Claims{}, func(token *jwt.Token) (interface{}, error) {
        return publicKey, nil
    })

    if err != nil {
        return nil, err
    }

    if claims, ok := token.Claims.(*Claims); ok && token.Valid {
        return claims, nil
    }

    return nil, fmt.Errorf("invalid token")
}

// 使用 RSA 公钥验证
func main() {
    // 生成 token
    tokenString, err := GenerateToken("user123", []string{"admin", "user"}, nil)
    if err != nil {
        panic(err)
    }
    fmt.Printf("Token: %s\n", tokenString)

    // 验证 token
    claims, err := ValidateToken(tokenString, []byte("my-secret-key"))
    if err != nil {
        panic(err)
    }
    fmt.Printf("UserID: %s, Roles: %v\n", claims.UserID, claims.Roles)
}
```

#### 8.2.2.5 JWT 安全最佳实践

```mermaid
graph TB
    A["<b style='color:#fff'>短期令牌</b>"] --> B["<b style='color:#22d3ee'>将 exp 设为较短时间</b>"]
    C["<b style='color:#fff'>使用强密钥</b>"] --> D["<b style='color:#22d3ee'>至少256位对称或2048位RSA</b>"]
    E["<b style='color:#fff'>验证签名</b>"] --> F["<b style='color:#22d3ee'>不要信任未验证的令牌</b>"]
    G["<b style='color:#fff'>验证受众</b>"] --> H["<b style='color:#22d3ee'>确认 aud 声明匹配</b>"]
    I["<b style='color:#fff'>使用 HTTPS</b>"] --> J["<b style='color:#22d3ee'>始终通过 TLS 传输</b>"]
    K["<b style='color:#fff'>敏感数据</b>"] --> L["<b style='color:#22d3ee'>不要在 payload 中存储敏感信息</b>"]

    style A fill:#f59e0b,stroke:#fbbf24,stroke-width:2px,color:#ffffff
    style B fill:#059669,stroke:#34d399,stroke-width:2px,color:#ffffff
    style C fill:#f59e0b,stroke:#fbbf24,stroke-width:2px,color:#ffffff
    style D fill:#059669,stroke:#34d399,stroke-width:2px,color:#ffffff
    style E fill:#f59e0b,stroke:#fbbf24,stroke-width:2px,color:#ffffff
    style F fill:#059669,stroke:#34d399,stroke-width:2px,color:#ffffff
    style G fill:#f59e0b,stroke:#fbbf24,stroke-width:2px,color:#ffffff
    style H fill:#059669,stroke:#34d399,stroke-width:2px,color:#ffffff
    style I fill:#f59e0b,stroke:#fbbf24,stroke-width:2px,color:#ffffff
    style J fill:#059669,stroke:#34d399,stroke-width:2px,color:#ffffff
    style K fill:#f59e0b,stroke:#fbbf24,stroke-width:2px,color:#ffffff
    style L fill:#059669,stroke:#34d399,stroke-width:2px,color:#ffffff
```

### 8.2.3 OpenID Connect

OpenID Connect（OIDC）是建立在 OAuth 2.0 之上的身份层，提供用户身份验证功能。

#### 8.2.3.1 OIDC 与 OAuth2.0 的区别

| 方面 | OAuth 2.0 | OpenID Connect |
|------|-----------|-----------------|
| 目的 | 授权 | 身份验证 + 授权 |
| Token | Access Token | Access Token + ID Token |
| 用户信息 | 无 | 可通过 UserInfo 端点获取 |
| 标准声明 | 无 | 定义了标准用户声明 |
| 发现协议 | 无 | OIDC Discovery |

#### 8.2.3.2 ID Token 结构

ID Token 是 JWT，包含用户身份信息：

```json
{
  "iss": "https://auth.example.com",
  "sub": "user123",
  "aud": "my-app-client-id",
  "exp": 1516242622,
  "iat": 1516239022,
  "name": "John Doe",
  "email": "john@example.com",
  "picture": "https://example.com/photo.jpg"
}
```

#### 8.2.3.3 OIDC 认证流程

```mermaid
sequenceDiagram
    participant User as 👤 用户
    participant App as 📱 客户端应用
    participant Auth as 🔐 OIDC Provider
    participant API as 🌐 资源服务器

    App->>+User: 引导登录
    User->>+Auth: 请求授权（含 openid scope）
    Auth->>-User: 显示登录页面
    User->>+Auth: 输入凭证
    Auth->>-App: 返回授权码 + ID Token
    App->>+Auth: 兑换令牌
    Auth->>-App: Access Token + ID Token + Refresh Token
    App->>+Auth: 请求 UserInfo 端点
    Auth->>-App: 返回用户基本信息
    App->>+API: 携带 Access Token 请求业务API
    API->>-App: 返回业务数据
```

#### 8.2.3.4 Java OIDC 实现

```java
// Spring Security OIDC 配置
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/oauth2/authorization/my-app")
                .authorizationEndpoint(authorization -> authorization
                    .baseUri("/oauth2/authorization")
                )
                .redirectionEndpoint(redirection -> redirection
                    .baseUri("/login/oauth2/code/*")
                )
                .defaultSuccessUrl("/home", true)
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
}

// 获取用户信息
@RestController
public class UserController {

    @GetMapping("/userinfo")
    public Map<String, Object> getUserInfo(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", jwt.getSubject());
        userInfo.put("name", jwt.getClaimAsString("name"));
        userInfo.put("email", jwt.getClaimAsString("email"));
        userInfo.put("roles", jwt.getClaimAsStringList("roles"));
        return userInfo;
    }
}
```

#### 8.2.3.5 Go OIDC 实现

```go
package main

import (
    "context"
    "encoding/json"
    "fmt"
    "log"
    "net/http"

    "github.com/coreos/go-oidc/v3/oidc"
)

var (
    provider *oidc.Provider
    verifier *oidc.IDTokenVerifier
)

func main() {
    ctx := context.Background()

    // 初始化 OIDC Provider
    var err error
    provider, err = oidc.NewProvider(ctx, "https://auth.example.com")
    if err != nil {
        log.Fatal(err)
    }

    // 配置验证器
    verifier = provider.Verifier(&oidc.Config{
        ClientID: "my-app-client-id",
    })

    http.HandleFunc("/login", handleLogin)
    http.HandleFunc("/callback", handleCallback)
    http.HandleFunc("/userinfo", handleUserInfo)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func handleLogin(w http.ResponseWriter, r *http.Request) {
    authURL, _ := provider.AuthURL(ctx, "state", "", []string{"openid", "profile", "email"})
    http.Redirect(w, r, authURL, http.StatusTemporaryRedirect)
}

func handleCallback(w http.ResponseWriter, r *http.Request) {
    code := r.URL.Query().Get("code")

    // 兑换令牌（需要实现 OAuth2 令牌交换）
    tokens, err := exchangeCode(ctx, code)
    if err != nil {
        http.Error(w, "Failed to exchange code", http.StatusInternalServerError)
        return
    }

    // 验证 ID Token
    idToken, err := verifier.Verify(ctx, tokens.IDToken)
    if err != nil {
        http.Error(w, "Failed to verify ID token", http.StatusInternalServerError)
        return
    }

    // 提取用户信息
    var claims struct {
        Sub   string `json:"sub"`
        Name  string `json:"name"`
        Email string `json:"email"`
    }
    idToken.Claims(&claims)

    fmt.Fprintf(w, "User: %s (%s)", claims.Name, claims.Email)
}

func handleUserInfo(w http.ResponseWriter, r *http.Request) {
    // 从 Authorization 头获取 Access Token
    token := r.Header.Get("Authorization")
    if token == "" {
        http.Error(w, "No token provided", http.StatusUnauthorized)
        return
    }

    // 调用 UserInfo 端点
    userInfo, err := provider.UserInfo(ctx, oauth2.StaticTokenSource(&oauth2.Token{AccessToken: token}))
    if err != nil {
        http.Error(w, "Failed to get user info", http.StatusInternalServerError)
        return
    }

    var claims map[string]interface{}
    userInfo.Claims(&claims)

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(claims)
}

// OAuth2 令牌交换（简化实现）
func exchangeCode(ctx context.Context, code string) (*oauth2.Token, error) {
    // 实际实现需要向授权服务器发送请求
    return nil, nil
}
```

---

## 8.3 服务间认证

在微服务架构中，服务间的相互调用需要安全认证。主要有以下几种方案：

### 8.3.1 mTLS 双向TLS认证

mTLS（Mutual TLS）是双向TLS认证，不仅验证服务器身份，还验证客户端身份。

```mermaid
sequenceDiagram
    participant WebEdge as 🖥️ 客户端
    participant Server as 🌐 服务端
    participant CA as 📜 证书颁发机构
    participant ClientCert as 📄 客户端证书

    WebEdge->>+Server: 发起 TLS 连接请求
    Server-->>-WebEdge: 完成双向认证

    CA-->>Server: 颁发服务器证书
    CA-->>ClientCert: 颁发客户端证书
    Server->>+ClientCert: 请求验证客户端证书
```

#### 8.3.1.1 mTLS 工作原理

```mermaid
sequenceDiagram
    participant Client as 🔐 客户端
    participant Server as 🌐 服务端

    Note over Client,Server: 🔒 TLS 握手第一阶段
    Client->>+Server: ClientHello
    Server->>-Client: ServerHello
    Server->>Client: 服务器证书
    Server->>Client: 证书请求（请求客户端证书）
    Server->>Client: ServerHelloDone

    Note over Client,Server: 🔑 TLS 握手第二阶段
    Client->>Server: 客户端证书
    Client->>Server: 证书验证
    Client->>Server: ClientKeyExchange（预主密钥）
    Client->>Server: 证书验证请求

    Note over Client,Server: ⚡ 密钥协商
    Client->>Server: 密钥协商完成
    Client->>Server: ChangeCipherSpec

    Note over Client,Server: ✅ 安全通信建立
    Server->>Client: ChangeCipherSpec
    Server->>Client: 握手完成
    Client->>Server: 加密的应用数据
    Server->>Client: 加密的应用数据
```

#### 8.3.1.2 Go mTLS 实现

```go
package main

import (
    "crypto/tls"
    "crypto/x509"
    "fmt"
    "io/ioutil"
    "net/http"
)

// 加载 mTLS 配置
func loadMTLSConfig(clientCertPath, clientKeyPath, caCertPath string) (*tls.Config, error) {
    // 加载客户端证书
    clientCert, err := tls.LoadX509KeyPair(clientCertPath, clientKeyPath)
    if err != nil {
        return nil, fmt.Errorf("failed to load client cert: %w", err)
    }

    // 加载 CA 证书用于验证服务端
    caCert, err := ioutil.ReadFile(caCertPath)
    if err != nil {
        return nil, fmt.Errorf("failed to read ca cert: %w", err)
    }

    caCertPool := x509.NewCertPool()
    caCertPool.AppendCertsFromPEM(caCert)

    return &tls.Config{
        Certificates: []tls.Certificate{clientCert},
        RootCAs:      caCertPool,
        // 验证服务端证书
        ServerName: "service.example.com",
        // 要求双向认证
        ClientAuth: tls.RequireAndVerifyClientCert,
        ClientCAs:  caCertPool,
    }, nil
}

// 创建 mTLS HTTP 客户端
func NewMTLSClient(clientCert, clientKey, caCert string) (*http.Client, error) {
    config, err := loadMTLSConfig(clientCert, clientKey, caCert)
    if err != nil {
        return nil, err
    }

    return &http.Client{
        Transport: &http.Transport{
            TLSClientConfig: config,
        },
    }, nil
}

// 服务端 mTLS 配置
func createMTLSServerConfig(serverCert, serverKey, caCert string) (*tls.Config, error) {
    serverCertificate, err := tls.LoadX509KeyPair(serverCert, serverKey)
    if err != nil {
        return nil, err
    }

    caCert, err := ioutil.ReadFile(caCert)
    if err != nil {
        return nil, err
    }

    caCertPool := x509.NewCertPool()
    caCertPool.AppendCertsFromPEM(caCert)

    return &tls.Config{
        Certificates: []tls.Certificate{serverCertificate},
        ClientCAs:     caCertPool,
        ClientAuth:    tls.RequireAndVerifyClientCert,
    }, nil
}
```

#### 8.3.1.3 Java mTLS 实现

```java
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class MTLSExample {

    public CloseableHttpClient createMTLSClient(
            String clientCertPath,
            String clientKeyPath,
            String trustStorePath,
            String trustStorePassword) throws Exception {

        // 加载客户端证书和私钥
        KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream certStream = new FileInputStream(clientCertPath)) {
            clientKeyStore.load(certStream, clientKeyPath.toCharArray());
        }

        // 加载信任的 CA 证书
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (FileInputStream trustStream = new FileInputStream(trustStorePath)) {
            trustStore.load(trustStream, trustStorePassword.toCharArray());
        }

        SSLContext sslContext = new SSLContextBuilder()
            .loadKeyMaterial(clientKeyStore, clientKeyPath.toCharArray())
            .loadTrustMaterial(trustStore, new TrustSelfSignedStrategy())
            .build();

        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
            sslContext,
            new String[]{"TLSv1.2", "TLSv1.3"},
            null,
            SSLConnectionSocketFactory.getDefaultHostnameVerifier()
        );

        return HttpClients.custom()
            .setSSLFactory(sslsf)
            .build();
    }

    public String callService(CloseableHttpClient client, String url) throws Exception {
        HttpGet request = new HttpGet(url);
        try (CloseableHttpResponse response = client.execute(request)) {
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity);
        }
    }
}
```

### 8.3.2 SPIFFE/SPIRE 身份体系

SPIFFE（Secure Production Identity Framework for Everyone）是一套用于跨环境安全身份认证的标准。SPIRE 是 SPIFFE 的参考实现。

#### 8.3.2.1 SPIFFE 架构

```mermaid
graph TB
    Server["<b style='color:#fff'>SPIRE Server</b>"]
    CA["<b style='color:#fff'>CA Module</b>"]
    Reg["<b style='color:#fff'>Registration API</b>"]
    Agent1["<b style='color:#fff'>SPIRE Agent</b>"]
    Agent2["<b style='color:#fff'>SPIRE Agent</b>"]
    AgentN["<b style='color:#fff'>SPIRE Agent</b>"]
    W1["<b style='color:#fff'>Workload Pod A</b>"]
    W2["<b style='color:#fff'>Workload Pod B</b>"]
    W3["<b style='color:#fff'>Workload Pod C</b>"]

    Server o-.-o Agent1
    Server o-.-o Agent2
    Server o-.-o AgentN
    Agent1 --> W1
    Agent2 --> W2
    AgentN --> W3
    Reg --> Server
    CA --> Server

    style Server fill:#7c3aed,stroke:#a78bfa,stroke-width:3px,color:#ffffff
    style CA fill:#6366f1,stroke:#818cf8,stroke-width:2px,color:#ffffff
    style Reg fill:#6366f1,stroke:#818cf8,stroke-width:2px,color:#ffffff
    style Agent1 fill:#06b6d4,stroke:#22d3ee,stroke-width:2px,color:#ffffff
    style Agent2 fill:#06b6d4,stroke:#22d3ee,stroke-width:2px,color:#ffffff
    style AgentN fill:#06b6d4,stroke:#22d3ee,stroke-width:2px,color:#ffffff
    style W1 fill:#10b981,stroke:#34d399,stroke-width:2px,color:#ffffff
    style W2 fill:#10b981,stroke:#34d399,stroke-width:2px,color:#ffffff
    style W3 fill:#10b981,stroke:#34d399,stroke-width:2px,color:#ffffff
```

#### 8.3.2.2 SPIFFE ID 与 SVID

- **SPIFFE ID**：统一资源标识符，格式为 `spiffe://trust-domain/path`
- **SVID**：SPIFFE Verifiable Identity Document，X.509 证书或 JWT

```
spiffe://example.com/ns/default/sa/default/service-a
```

#### 8.3.2.3 SPIRE 配置示例

**Server 配置（spire-server.yaml）：**

```yaml
server:
  bind_address: 0.0.0.0
  bind_port: 8081
  trust_domain: example.com
  data_dir: /opt/spire/data/server
  log_level: DEBUG

plugins:
  DataStore:
    sql:
      plugin_name: sql
      data_source: sqlite3:///opt/spire/data/server/datastore.sqlite

  NodeAttestor:
    k8s_psat:
      plugin_name: k8s_psat
      cluster: my-cluster

  KeyManager:
    disk:
      plugin_name: disk
      directory: /opt/spire/data/server/keys

  CACatalog:
    built_in:
      plugin_name: upstreamCA
```

**Agent 配置（spire-agent.yaml）：**

```yaml
agent:
  trust_domain: example.com
  trust_bundle_path: /opt/spire/agent/bootstrap.crt
  socket_path: /opt/spire/agent/sockets/agent.sock
  data_dir: /opt/spire/data/agent
  log_level: DEBUG

plugins:
  NodeAttestor:
    k8s_psat:
      plugin_name: k8s_psat
      cluster: my-cluster

  WorkloadAttestor:
    k8s:
      plugin_name: k8s
      skip_kubelet_verification: false
```

#### 8.3.2.4 使用 SPIFFE 证书进行 mTLS

```go
// 使用 SPIRE API 获取 SVID 并进行 mTLS 通信
package main

import (
    "context"
    "fmt"
    "net/http"

    "github.com/spiffe/go-spiffe/v2/spiffeid"
    "github.com/spiffe/go-spiffe/v2/spiffetls"
    "github.com/spiffe/go-spiffe/v2/workloadapi"
)

func main() {
    ctx := context.Background()

    // 创建 Workload API 客户端
    source, err := workloadapi.NewX509Source(ctx)
    if err != nil {
        panic(err)
    }
    defer source.Close()

    // 创建 TLS 配置（自动使用 SVID）
    tlsConfig := spiffetls.MTLSConfig(ctx, source, source)

    // 创建 HTTP 客户端
    client := &http.Client{
        Transport: &http.Transport{
            TLSClientConfig: tlsConfig,
        },
    }

    // 调用服务（自动进行 mTLS 认证）
    resp, err := client.Get("https://service-b.namespace.svc.cluster.local/api")
    if err != nil {
        panic(err)
    }
    defer resp.Body.Close()

    fmt.Printf("Response: %s\n", resp.Status)
}

// 服务端使用 SPIFFE 证书
func createServer() error {
    ctx := context.Background()

    source, err := workloadapi.NewX509Source(ctx)
    if err != nil {
        return err
    }
    defer source.Close()

    tlsConfig := spiffetls.MTLSConfig(ctx, source, source)

    server := &http.Server{
        Addr:      ":8443",
        TLSConfig: tlsConfig,
        Handler:   http.DefaultServeMux,
    }

    return server.ListenAndServeTLS("", "")
}
```

### 8.3.3 JWT 降级模式

当 mTLS 不可用时，可以使用 JWT 作为服务间认证的降级方案。

```mermaid
graph TB
    A["<b style='color:#fff'>发起请求</b>"] --> B{"<b style='color:#fff'>检查 mTLS<br/>可用性</b>"}
    B -->|"<b style='color:#22d3ee'>可用</b>"| C["<b style='color:#fff'>使用 mTLS</b>"]
    B -->|"<b style='color:#f87171'>不可用</b>"| D{"<b style='color:#fff'>检查 JWT<br/>可用性</b>"}
    D -->|"<b style='color:#22d3ee'>可用</b>"| E["<b style='color:#fff'>使用 JWT 降级</b>"]
    D -->|"<b style='color:#f87171'>不可用</b>"| F["<b style='color:#fff'>拒绝请求</b>"]

    style A fill:#0ea5e9,stroke:#38bdf8,stroke-width:2px,color:#ffffff
    style B fill:#7c3aed,stroke:#a78bfa,stroke-width:3px,color:#ffffff
    style C fill:#059669,stroke:#34d399,stroke-width:2px,color:#ffffff
    style D fill:#f59e0b,stroke:#fbbf24,stroke-width:3px,color:#ffffff
    style E fill:#6366f1,stroke:#818cf8,stroke-width:2px,color:#ffffff
    style F fill:#dc2626,stroke:#ef4444,stroke-width:2px,color:#ffffff
```

#### 8.3.3.1 JWT 降级实现

```go
package auth

import (
    "context"
    "fmt"
    "net/http"
    "strings"
    "time"

    "github.com/golang-jwt/jwt/v5"
)

type ServiceAuth int

const (
    AuthMTLS ServiceAuth = iota
    AuthJWT
    AuthNone
)

// 服务间认证接口
type InterServiceAuth interface {
    // 获取认证类型
    AuthType() ServiceAuth
    // 为请求添加认证信息
    AddAuth(req *http.Request) error
    // 验证请求认证
    ValidateAuth(req *http.Request) error
}

// JWT 降级认证器
type JWTDowngradeAuth struct {
    issuer    string
    audience  string
    signingKey []byte
}

func NewJWTDowngradeAuth(issuer, audience string, signingKey []byte) *JWTDowngradeAuth {
    return &JWTDowngradeAuth{
        issuer:    issuer,
        audience:  audience,
        signingKey: signingKey,
    }
}

func (j *JWTDowngradeAuth) AuthType() ServiceAuth {
    return AuthJWT
}

func (j *JWTDowngradeAuth) AddAuth(req *http.Request) error {
    now := time.Now()
    claims := jwt.MapClaims{
        "iss": j.issuer,
        "sub": "service-name",
        "aud": j.audience,
        "iat": now.Unix(),
        "exp": now.Add(5 * time.Minute).Unix(),
        "jti": fmt.Sprintf("%d-%d", now.UnixNano(), time.Now().UnixNano()),
        "scope": []string{"service:read", "service:write"},
    }

    token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
    tokenString, err := token.SignedString(j.signingKey)
    if err != nil {
        return err
    }

    req.Header.Set("Authorization", "Bearer "+tokenString)
    return nil
}

func (j *JWTDowngradeAuth) ValidateAuth(req *http.Request) error {
    authHeader := req.Header.Get("Authorization")
    if authHeader == "" {
        return fmt.Errorf("missing authorization header")
    }

    parts := strings.Split(authHeader, " ")
    if len(parts) != 2 || parts[0] != "Bearer" {
        return fmt.Errorf("invalid authorization header format")
    }

    tokenString := parts[1]

    token, err := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
        if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
            return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
        }
        return j.signingKey, nil
    })

    if err != nil {
        return fmt.Errorf("invalid token: %w", err)
    }

    if claims, ok := token.Claims.(jwt.MapClaims); ok && token.Valid {
        // 验证 issuer
        if claims["iss"] != j.issuer {
            return fmt.Errorf("invalid issuer")
        }
        // 验证 audience
        if claims["aud"] != j.audience {
            return fmt.Errorf("invalid audience")
        }
        return nil
    }

    return fmt.Errorf("invalid token claims")
}

// 认证选择器
type AuthSelector struct {
    mtlsAuth *MTLSAuth
    jwtAuth  *JWTDowngradeAuth
}

func NewAuthSelector(mtlsAuth *MTLSAuth, jwtAuth *JWTDowngradeAuth) *AuthSelector {
    return &AuthSelector{
        mtlsAuth: mtlsAuth,
        jwtAuth:  jwtAuth,
    }
}

func (s *AuthSelector) ChooseAuth(ctx context.Context) InterServiceAuth {
    // 优先尝试 mTLS
    if s.mtlsAuth != nil && s.mtlsAuth.IsAvailable(ctx) {
        return s.mtlsAuth
    }
    // 降级到 JWT
    if s.jwtAuth != nil {
        return s.jwtAuth
    }
    return nil
}

// 带认证的 HTTP 客户端
type AuthHTTPClient struct {
    selector *AuthSelector
    client   *http.Client
}

func (c *AuthHTTPClient) DoAuthenticatedRequest(ctx context.Context, url string) (*http.Response, error) {
    req, err := http.NewRequestWithContext(ctx, "GET", url, nil)
    if err != nil {
        return nil, err
    }

    auth := c.selector.ChooseAuth(ctx)
    if auth == nil {
        return nil, fmt.Errorf("no authentication available")
    }

    if err := auth.AddAuth(req); err != nil {
        return nil, err
    }

    return c.client.Do(req)
}
```

---

## 8.4 API 安全

### 8.4.1 API Key 与签名认证

#### 8.4.1.1 API Key 认证

API Key 是一种简单的认证方式，适合服务器到服务器的调用。

```mermaid
sequenceDiagram
    participant Client as 🖥️ 客户端
    participant Server as 🌐 服务器
    participant Valid as ✅ 验证服务
    participant Resource as 📦 受保护资源
    participant Err as ❌ 错误处理

    Client->>+Server: 请求 API
    Server->>+Valid: 验证 API Key
    Valid-->>Resource: 有效，返回资源
    Valid-->>Err: 无效，401
```

**API Key 实现：**

```java
// API Key 验证过滤器
@Component
@Order(1)
public class ApiKeyAuthFilter implements Filter {

    @Value("${security.api-key.header-name:X-API-Key}")
    private String apiKeyHeader;

    @Value("${security.api-key.valid-keys}")
    private String validApiKeys;

    private final Set<String> apiKeySet = new HashSet<>();

    @PostConstruct
    public void init() {
        Arrays.stream(validApiKeys.split(","))
            .forEach(apiKeySet::add);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // 跳过不需要认证的路径
        String path = request.getRequestURI();
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(apiKeyHeader);

        if (apiKey == null || !apiKeySet.contains(apiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Invalid API Key\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/public/") || path.startsWith("/health");
    }
}
```

**Go API Key 实现：**

```go
package middleware

import (
    "crypto/subtle"
    "net/http"
    "strings"
)

type APIKeyAuth struct {
    validKeys map[string]string // key -> service name
    headerName string
}

func NewAPIKeyAuth(keys map[string]string, headerName string) *APIKeyAuth {
    return &APIKeyAuth{
        validKeys: keys,
        headerName: headerName,
    }
}

func (a *APIKeyAuth) Middleware(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        apiKey := r.Header.Get(a.headerName)

        if apiKey == "" {
            http.Error(w, "Missing API Key", http.StatusUnauthorized)
            return
        }

        serviceName, valid := a.validKeys[apiKey]
        if !valid {
            http.Error(w, "Invalid API Key", http.StatusUnauthorized)
            return
        }

        // 将服务名存入请求上下文
        ctx := SetServiceName(r.Context(), serviceName)
        next.ServeHTTP(w, r.WithContext(ctx))
    })
}

// 恒定时间比较，防止时序攻击
func secureCompare(a, b string) bool {
    return subtle.ConstantTimeCompare([]byte(a), []byte(b)) == 1
}
```

#### 8.4.1.2 签名认证

签名认证提供更高级别的安全性，用于金融支付、重要的数据操作等场景。

```mermaid
sequenceDiagram
    participant Client as 🖥️ 客户端
    participant Server as 🌐 服务器

    Note over Client: 1️⃣ 准备签名参数
    Client->>Client: 按字典序排序参数
    Client->>Client: 拼接参数字符串
    Client->>Client: 生成时间戳
    Client->>Client: 计算签名

    Client->>+Server: 发送请求
    Note over Server: 2️⃣ 验证签名

    Server->>Server: 检查时间戳(防重放)
    Server->>Server: 重新计算签名
    Server->>Server: 比较签名

    alt 签名匹配
        Server-->>Client: ✅ 返回 200 OK
    else 签名不匹配
        Server-->>Client: ❌ 返回 401 Unauthorized
    end
```

**签名算法实现：**

```java
// 签名生成与验证
@Service
public class SignatureService {

    private static final String SIGNATURE_ALGORITHM = "HmacSHA256";
    private static final long TIMESTAMP_TOLERANCE = 5 * 60 * 1000; // 5分钟

    // 生成签名
    public String generateSignature(Map<String, String> params,
                                    String secretKey,
                                    long timestamp) {
        // 1. 按字典序排序参数
        String sortedParams = params.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining("&"));

        // 2. 拼接字符串
        String stringToSign = String.format("%d%s%s", timestamp, sortedParams, secretKey);

        // 3. 计算签名
        return calculateHMAC(stringToSign, secretKey);
    }

    // 验证签名
    public boolean verifySignature(HttpServletRequest request) {
        String signature = request.getHeader("X-Signature");
        String timestampStr = request.getHeader("X-Timestamp");
        String appId = request.getHeader("X-App-Id");

        if (signature == null || timestampStr == null || appId == null) {
            return false;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            return false;
        }

        // 检查时间戳，防止重放攻击
        if (Math.abs(System.currentTimeMillis() - timestamp) > TIMESTAMP_TOLERANCE) {
            return false;
        }

        // 获取密钥
        String secretKey = getSecretKey(appId);
        if (secretKey == null) {
            return false;
        }

        // 提取参数
        Map<String, String> params = extractParams(request);

        // 重新计算签名
        String expectedSignature = generateSignature(params, secretKey, timestamp);

        // 恒定时间比较
        return secureCompare(signature, expectedSignature);
    }

    private String calculateHMAC(String data, String key) {
        try {
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            SecretKey secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), SIGNATURE_ALGORITHM);
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    // 恒定时间比较，防止时序攻击
    private boolean secureCompare(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
```

**Go 签名实现：**

```go
package signature

import (
    "crypto/hmac"
    "crypto/sha256"
    "encoding/base64"
    "fmt"
    "net/http"
    "sort"
    "strconv"
    "strings"
    "time"
)

const (
    HeaderSignature = "X-Signature"
    HeaderTimestamp = "X-Timestamp"
    HeaderAppId     = "X-App-Id"
)

type SignatureService struct {
    secretKeys map[string]string // appId -> secretKey
    tolerance  time.Duration
}

func NewSignatureService(secretKeys map[string]string) *SignatureService {
    return &SignatureService{
        secretKeys: secretKeys,
        tolerance:  5 * time.Minute,
    }
}

// 生成签名
func (s *SignatureService) GenerateSignature(appId string, params map[string]string, secretKey string) (string, string, error) {
    timestamp := time.Now().Unix()

    // 按字典序排序并拼接参数
    sortedParams := s.sortParams(params)
    stringToSign := fmt.Sprintf("%d%s%s", timestamp, sortedParams, secretKey)

    // 计算 HMAC
    h := hmac.New(sha256.New, []byte(secretKey))
    h.Write([]byte(stringToSign))
    signature := base64.StdEncoding.EncodeToString(h.Sum(nil))

    return signature, strconv.FormatInt(timestamp, 10), nil
}

// 验证签名
func (s *SignatureService) VerifySignature(r *http.Request) error {
    signature := r.Header.Get(HeaderSignature)
    timestampStr := r.Header.Get(HeaderTimestamp)
    appId := r.Header.Get(HeaderAppId)

    if signature == "" || timestampStr == "" || appId == "" {
        return fmt.Errorf("missing required headers")
    }

    // 解析时间戳
    timestamp, err := strconv.ParseInt(timestampStr, 10, 64)
    if err != nil {
        return fmt.Errorf("invalid timestamp")
    }

    // 检查时间戳容差
    if time.Since(time.Unix(timestamp, 0)) > s.tolerance {
        return fmt.Errorf("timestamp too old")
    }

    // 获取密钥
    secretKey, ok := s.secretKeys[appId]
    if !ok {
        return fmt.Errorf("unknown appId")
    }

    // 提取参数
    params := s.extractParams(r)

    // 重新计算签名
    expectedSig, _, err := s.GenerateSignature(appId, params, secretKey)
    if err != nil {
        return err
    }

    // 恒定时间比较
    if !hmac.Equal([]byte(signature), []byte(expectedSig)) {
        return fmt.Errorf("signature mismatch")
    }

    return nil
}

func (s *SignatureService) sortParams(params map[string]string) string {
    keys := make([]string, 0, len(params))
    for k := range params {
        keys = append(keys, k)
    }
    sort.Strings(keys)

    var parts []string
    for _, k := range keys {
        parts = append(parts, fmt.Sprintf("%s=%s", k, params[k]))
    }
    return strings.Join(parts, "&")
}

func (s *SignatureService) extractParams(r *http.Request) map[string]string {
    params := make(map[string]string)

    // 从查询字符串提取
    query := r.URL.Query()
    for k, v := range query {
        if len(v) > 0 {
            params[k] = v[0]
        }
    }

    // 从表单提取
    if err := r.ParseForm(); err == nil {
        for k, v := range r.PostForm {
            if len(v) > 0 {
                params[k] = v[0]
            }
        }
    }

    return params
}
```

### 8.4.2 HTTPS/TLS 加密

所有微服务间的通信都应使用 HTTPS/TLS 加密。

```mermaid
graph LR
    A["<b style='color:#fff'>明文数据</b>"] --> B["<b style='color:#fff'>网络传输</b>"]
    B --> C["<b style='color:#fff'>⚠️ 可能被截获</b>"]

    D["<b style='color:#fff'>明文数据</b>"] --> E["<b style='color:#fff'>🔒 TLS 加密</b>"]
    E --> F["<b style='color:#22d3ee'>密文传输</b>"]
    F --> G["<b style='color:#fff'>🔓 TLS 解密</b>"]
    G --> H["<b style='color:#fff'>明文数据</b>"]

    style A fill:#64748b,stroke:#94a3b8,stroke-width:2px,color:#ffffff
    style B fill:#64748b,stroke:#94a3b8,stroke-width:2px,color:#ffffff
    style C fill:#dc2626,stroke:#ef4444,stroke-width:3px,color:#ffffff
    style D fill:#10b981,stroke:#34d399,stroke-width:2px,color:#ffffff
    style E fill:#059669,stroke:#34d399,stroke-width:2px,color:#ffffff
    style F fill:#06b6d4,stroke:#22d3ee,stroke-width:3px,color:#ffffff
    style G fill:#10b981,stroke:#34d399,stroke-width:2px,color:#ffffff
    style H fill:#10b981,stroke:#34d399,stroke-width:2px,color:#ffffff
```

#### 8.4.2.1 Java HTTPS 配置

```java
// Spring Boot HTTPS 配置
// application.yml
/*
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12
    key-alias: mykey
    client-auth: need   # 需要客户端证书（mTLS）
    trust-store: classpath:truststore.p12
    trust-store-password: changeit
    trust-store-type: PKCS12
*/

// 自定义 TLS 配置
@Configuration
public class TLSConfig {

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessEngine(Engine engine) {
                engine.getPipeline().addValve(new SSLValve());
            }
        };
        return tomcat;
    }

    @Bean
    public RestTemplate restTemplate() throws KeyStoreException, NoSuchAlgorithmException {
        TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;

        SSLContext sslContext = new SSLContextBuilder()
            .loadTrustMaterial(trustStore().getFile(), "changeit".toCharArray())
            .loadTrustMaterial(acceptingTrustStrategy)
            .build();

        CloseableHttpClient httpClient = HttpClients.custom()
            .setSSLContext(sslContext)
            .setSSLHostnameVerifier(new NoopHostnameVerifier())
            .build();

        return new RestTemplateBuilder()
            .httpClient(httpClient)
            .build();
    }

    @Bean
    public KeyStore trustStore() throws Exception {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream fis = new FileInputStream("path/to/truststore.p12")) {
            trustStore.load(fis, "changeit".toCharArray());
        }
        return trustStore;
    }
}
```

#### 8.4.2.2 Go HTTPS 配置

```go
package main

import (
    "crypto/tls"
    "crypto/x509"
    "fmt"
    "io/ioutil"
    "net/http"
)

// 加载 TLS 配置
func loadTLSConfig(certFile, keyFile, caCertFile string) (*tls.Config, error) {
    // 加载服务端证书
    cert, err := tls.LoadX509KeyPair(certFile, keyFile)
    if err != nil {
        return nil, fmt.Errorf("failed to load certificate: %w", err)
    }

    // 创建 CA 池
    caCert, err := ioutil.ReadFile(caCertFile)
    if err != nil {
        return nil, fmt.Errorf("failed to read CA certificate: %w", err)
    }
    caCertPool := x509.NewCertPool()
    caCertPool.AppendCertsFromPEM(caCert)

    return &tls.Config{
        Certificates: []tls.Certificate{cert},
        ClientCAs:    caCertPool,
        ClientAuth:   tls.RequireAndVerifyClientCert,
        MinVersion:   tls.VersionTLS12,
        CipherSuites: []uint16{
            tls.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
            tls.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
            tls.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
            tls.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
        },
    }, nil
}

// 创建 HTTPS 服务器
func createHTTPSServer(addr string, certFile, keyFile, caCertFile string) (*http.Server, error) {
    config, err := loadTLSConfig(certFile, keyFile, caCertFile)
    if err != nil {
        return nil, err
    }

    mux := http.NewServeMux()
    mux.HandleFunc("/api", func(w http.ResponseWriter, r *http.Request) {
        w.Write([]byte("Hello, TLS!"))
    })

    return &http.Server{
        Addr:      addr,
        Handler:   mux,
        TLSConfig: config,
    }, nil
}

// 创建 HTTPS 客户端
func createHTTPSClient(caCertFile string) (*http.Client, error) {
    caCert, err := ioutil.ReadFile(caCertFile)
    if err != nil {
        return nil, err
    }

    caCertPool := x509.NewCertPool()
    caCertPool.AppendCertsFromPEM(caCert)

    config := &tls.Config{
        RootCAs:    caCertPool,
        MinVersion: tls.VersionTLS12,
    }

    return &http.Client{
        Transport: &http.Transport{
            TLSClientConfig: config,
        },
    }, nil
}
```

### 8.4.3 CORS 配置

跨域资源共享（CORS）是浏览器的安全机制，需要正确配置。

#### 8.4.3.1 Java CORS 配置

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(
                "https://example.com",
                "https://app.example.com"
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("X-Request-Id", "X-Total-Count")
            .allowCredentials(true)
            .maxAge(3600);
    }
}

// 细粒度 CORS 配置
@Configuration
public class FineGrainedCorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 公共 API - 允许所有来源
        CorsConfiguration publicApi = new CorsConfiguration();
        publicApi.setAllowedOrigins(Arrays.asList("*"));
        publicApi.setAllowedMethods(Arrays.asList("GET", "POST"));
        publicApi.setAllowedHeaders(Arrays.asList("*"));
        publicApi.setMaxAge(3600L);
        source.registerCorsConfiguration("/api/public/**", publicApi);

        // 私有 API - 需要凭证
        CorsConfiguration privateApi = new CorsConfiguration();
        privateApi.setAllowedOrigins(
            Arrays.asList("https://example.com", "https://app.example.com")
        );
        privateApi.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        privateApi.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        privateApi.setExposedHeaders(Arrays.asList("X-Request-Id"));
        privateApi.setAllowCredentials(true);
        privateApi.setMaxAge(3600L);
        source.registerCorsConfiguration("/api/private/**", privateApi);

        // 管理员 API - 只允许特定来源
        CorsConfiguration adminApi = new CorsConfiguration();
        adminApi.setAllowedOrigins(
            Arrays.asList("https://admin.example.com")
        );
        adminApi.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        adminApi.setAllowedHeaders(Arrays.asList("*"));
        adminApi.setAllowCredentials(true);
        adminApi.setMaxAge(3600L);
        source.registerCorsConfiguration("/api/admin/**", adminApi);

        return source;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new CorsFilter(corsConfigurationSource()));
        bean.addUrlPatterns("/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
```

#### 8.4.3.2 Go CORS 配置

```go
package middleware

import (
    "net/http"

    "github.com/gin-contrib/cors"
    "github.com/gin-gonic/gin"
)

// CORS 配置
func CORSMiddleware() gin.HandlerFunc {
    config := cors.Config{
        AllowOrigins:     []string{"https://example.com", "https://app.example.com"},
        AllowMethods:      []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
        AllowHeaders:     []string{"Origin", "Content-Type", "Authorization", "X-Request-Id"},
        ExposeHeaders:    []string{"X-Request-Id", "X-Total-Count"},
        AllowCredentials: true,
        MaxAge:           3600,
    }
    return cors.New(config)
}

// 自定义 CORS 处理器
func CustomCORS() gin.HandlerFunc {
    return func(c *gin.Context) {
        origin := c.Request.Header.Get("Origin")

        // 验证来源
        allowedOrigins := map[string]bool{
            "https://example.com":      true,
            "https://app.example.com": true,
            "https://admin.example.com": true,
        }

        if allowedOrigins[origin] {
            c.Writer.Header().Set("Access-Control-Allow-Origin", origin)
        }

        c.Writer.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        c.Writer.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Request-Id")
        c.Writer.Header().Set("Access-Control-Expose-Headers", "X-Request-Id, X-Total-Count")
        c.Writer.Header().Set("Access-Control-Allow-Credentials", "true")
        c.Writer.Header().Set("Access-Control-Max-Age", "3600")

        if c.Request.Method == "OPTIONS" {
            c.AbortWithStatus(http.StatusNoContent)
            return
        }

        c.Next()
    }
}

// 使用示例
func main() {
    r := gin.Default()
    r.Use(CustomCORS())

    r.GET("/api/public", func(c *gin.Context) {
        c.JSON(http.StatusOK, gin.H{"message": "public endpoint"})
    })

    r.GET("/api/private", func(c *gin.Context) {
        c.JSON(http.StatusOK, gin.H{"message": "private endpoint"})
    })

    r.Run(":8080")
}
```

---

## 8.5 敏感数据管理

### 8.5.1 密钥管理（Vault、KMS）

#### 8.5.1.1 HashiCorp Vault 架构

```mermaid
graph TB
    subgraph "应用程序层"["<b style='color:#fff'>应用程序层</b>"]
        App1["<b style='color:#fff'>应用 A</b>"]
        App2["<b style='color:#fff'>应用 B</b>"]
        App3["<b style='color:#fff'>应用 C</b>"]
    end

    subgraph "Vault"["<b style='color:#fff'>Vault</b>"]
        API["<b style='color:#fff'>Vault API</b>"]
        Core["<b style='color:#fff'>Core Engine</b>"]
        Bar["<b style='color:#fff'>Barrier</b>"]
    end

    subgraph "存储后端"["<b style='color:#fff'>存储后端</b>"]
        Consul[("<b style='color:#fff'>Consul</b>")]
        DB[("<b style='color:#fff'>数据库</b>")]
    end

    App1 -.->|"<b style='color:#fbbf24'>HTTPS</b>"| API
    App2 -.->|"<b style='color:#fbbf24'>HTTPS</b>"| API
    App3 -.->|"<b style='color:#fbbf24'>HTTPS</b>"| API

    API --> Core
    Core --> Bar
    Bar -.->|"<b style='color:#34d399'>加密存储</b>"| Consul
    Bar -.->|"<b style='color:#34d399'>加密存储</b>"| DB

    style App1 fill:#0ea5e9,stroke:#38bdf8,stroke-width:2px,color:#ffffff
    style App2 fill:#0ea5e9,stroke:#38bdf8,stroke-width:2px,color:#ffffff
    style App3 fill:#0ea5e9,stroke:#38bdf8,stroke-width:2px,color:#ffffff
    style API fill:#7c3aed,stroke:#a78bfa,stroke-width:2px,color:#ffffff
    style Core fill:#6366f1,stroke:#818cf8,stroke-width:2px,color:#ffffff
    style Bar fill:#ec4899,stroke:#f472b6,stroke-width:2px,color:#ffffff
    style Consul fill:#10b981,stroke:#34d399,stroke-width:2px,color:#ffffff
    style DB fill:#059669,stroke:#34d399,stroke-width:2px,color:#ffffff
```

#### 8.5.1.2 Vault Java 客户端

```java
// 使用 Vault Java 客户端
@Component
public class VaultConfig {

    @Value("${vault.address}")
    private String vaultAddress;

    @Value("${vault.token}")
    private String vaultToken;

    @Bean
    public VaultTemplate vaultTemplate() {
        VaultEndpoint vaultEndpoint = new VaultEndpoint();
        vaultEndpoint.setAddress(URI.create(vaultAddress));

        VaultClient vaultClient = new VaultClient(vaultEndpoint);

        // 使用 Token 认证
        TokenAuthenticator tokenAuth = new TokenAuthenticator(vaultClient, vaultToken);
        return new VaultTemplate(vaultClient, tokenAuth);
    }
}

// 安全地获取密钥
@Service
public class SecretService {

    private final VaultTemplate vaultTemplate;

    public SecretService(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    // 读取数据库密码
    public String getDatabasePassword() {
        VaultResponseSupport<Map<String, String>> response = vaultTemplate
            .read("secret/data/database", Map.class);

        if (response != null && response.getData() != null) {
            return response.getData().get("password");
        }
        throw new RuntimeException("Database password not found");
    }

    // 读取 API 密钥
    public String getApiKey(String serviceName) {
        String path = String.format("secret/data/services/%s", serviceName);
        VaultResponseSupport<Map<String, String>> response = vaultTemplate
            .read(path, Map.class);

        if (response != null && response.getData() != null) {
            return response.getData().get("api_key");
        }
        throw new RuntimeException("API key not found: " + serviceName);
    }
}

// Spring Cloud Vault 配置
/*
spring:
  cloud:
    vault:
      host: vault.example.com
      port: 8200
      scheme: https
      token: your-token
      database:
        role: myapp-db-role
        enabled: true
      kv:
        backend: secret
        default-context: myapp
*/
```

#### 8.5.1.3 Vault Go 客户端

```go
package main

import (
    "context"
    "fmt"
    "log"
    "os"

    vault "github.com/hashicorp/vault/api"
)

func main() {
    // 创建 Vault 客户端
    config := vault.DefaultConfig()
    config.Address = os.Getenv("VAULT_ADDR")

    client, err := vault.NewClient(config)
    if err != nil {
        log.Fatal(err)
    }

    // 设置 Token
    client.SetToken(os.Getenv("VAULT_TOKEN"))

    // 读取密钥
    secret, err := client.KVv2("secret").Get(context.Background(), "database")
    if err != nil {
        log.Fatal(err)
    }

    // 提取数据
    if secret != nil {
        fmt.Printf("DB Password: %s\n", secret.Data["password"])
        fmt.Printf("DB Username: %s\n", secret.Data["username"])
    }

    // 读取动态凭据（数据库）
    mountPath := "database/creds"
    creds, err := client.Secrets("database").GenerateCredentials(context.Background(), mountPath)
    if err != nil {
        log.Fatal(err)
    }

    fmt.Printf("Dynamic DB Username: %s\n", creds.Username)
    fmt.Printf("Dynamic DB Password: %s\n", creds.Password)
}

// Vault 与 Kubernetes 集成
type KubernetesAuth struct {
    client *vault.Client
}

func NewKubernetesAuth(client *vault.Client, role string) (*KubernetesAuth, error) {
    // 获取 Kubernetes 服务账户 JWT
    jwt, err := os.ReadFile("/var/run/secrets/kubernetes.io/serviceaccount/token")
    if err != nil {
        return nil, err
    }

    // Kubernetes 认证
    secret, err := client.Auth().Kubernetes().Login(
        context.Background(),
        vault.KubernetesLoginRequest{
            Role:   role,
            JWT:    string(jwt),
        },
    )
    if err != nil {
        return nil, err
    }

    client.SetToken(secret.Auth.ClientToken)
    return &KubernetesAuth{client: client}, nil
}
```

#### 8.5.1.4 AWS KMS 集成

```java
// AWS KMS 密钥管理
@Component
public class AWSKMSService {

    private final AWSKMS kmsClient;

    public AWSKMSService() {
        this.kmsClient = AWSKMSClientBuilder.standard()
            .withRegion("us-east-1")
            .build();
    }

    // 加密数据
    public byte[] encrypt(String plaintext, String keyId) {
        EncryptRequest request = new EncryptRequest()
            .withKeyId(keyId)
            .withPlaintext(SdkBytes.fromUtf8String(plaintext));

        EncryptResult result = kmsClient.encrypt(request);
        return result.getCiphertext().asByteArray();
    }

    // 解密数据
    public String decrypt(byte[] ciphertext, String keyId) {
        DecryptRequest request = new DecryptRequest()
            .withKeyId(keyId)
            .withCiphertextBlob(SdkBytes.fromByteArray(ciphertext));

        DecryptResult result = kmsClient.decrypt(request);
        return result.getPlaintext().asUtf8String();
    }

    // 生成数据密钥
    public GenerateDataKeyResult generateDataKey(String keyId) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest()
            .withKeyId(keyId)
            .withKeySpec(DataKeySpec.AES_256);

        return kmsClient.generateDataKey(request);
    }
}
```

### 8.5.2 加密存储

#### 8.5.2.1 数据库字段加密

```java
// 使用 JPA 加密字段
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    // 加密存储的敏感字段
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "ssn", length = 500)
    private String ssn;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "credit_card", length = 500)
    private String creditCardNumber;

    // 普通字段不加密
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Value("${encryption.key}")
    private String encryptionKey;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKey key = generateKey();
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            // 组合 IV 和加密数据
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(dbData);

            // 提取 IV 和加密数据
            byte[] iv = Arrays.copyOfRange(combined, 0, 12);
            byte[] encrypted = Arrays.copyOfRange(combined, 12, combined.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKey key = generateKey();
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    private SecretKey generateKey() {
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[32];
        System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 32));
        return new SecretKeySpec(key, "AES");
    }
}
```

#### 8.5.2.2 Go 加密实现

```go
package crypto

import (
    "crypto/aes"
    "crypto/cipher"
    "crypto/rand"
    "encoding/base64"
    "errors"
    "io"
)

// AES-GCM 加密器
type Encryptor struct {
    key []byte
}

func NewEncryptor(key string) (*Encryptor, error) {
    keyBytes := []byte(key)
    if len(keyBytes) < 32 {
        // 补齐到 32 字节
        padded := make([]byte, 32)
        copy(padded, keyBytes)
        keyBytes = padded
    } else if len(keyBytes) > 32 {
        keyBytes = keyBytes[:32]
    }
    return &Encryptor{key: keyBytes}, nil
}

func (e *Encryptor) Encrypt(plaintext string) (string, error) {
    block, err := aes.NewCipher(e.key)
    if err != nil {
        return "", err
    }

    gcm, err := cipher.NewGCM(block)
    if err != nil {
        return "", err
    }

    nonce := make([]byte, gcm.NonceSize())
    if _, err := io.ReadFull(rand.Reader, nonce); err != nil {
        return "", err
    }

    ciphertext := gcm.Seal(nonce, nonce, []byte(plaintext), nil)
    return base64.StdEncoding.EncodeToString(ciphertext), nil
}

func (e *Encryptor) Decrypt(encrypted string) (string, error) {
    ciphertext, err := base64.StdEncoding.DecodeString(encrypted)
    if err != nil {
        return "", err
    }

    block, err := aes.NewCipher(e.key)
    if err != nil {
        return "", err
    }

    gcm, err := cipher.NewGCM(block)
    if err != nil {
        return "", err
    }

    nonceSize := gcm.NonceSize()
    if len(ciphertext) < nonceSize {
        return "", errors.New("ciphertext too short")
    }

    nonce, ciphertext := ciphertext[:nonceSize], ciphertext[nonceSize:]
    plaintext, err := gcm.Open(nil, nonce, ciphertext, nil)
    if err != nil {
        return "", err
    }

    return string(plaintext), nil
}
```

### 8.5.3 环境变量与 Secret

#### 8.5.3.1 Kubernetes Secret

```yaml
# Kubernetes Secret 资源
apiVersion: v1
kind: Secret
metadata:
  name: my-secret
  namespace: production
type: Opaque
data:
  # base64 编码的值
  database-password: cGFzc3dvcmQxMjM=
  api-key: YXBpLWtleS1zZWNyZXQ=
stringData:
  # 明文（会自动编码）
  config.yaml: |
    database:
      host: db.example.com
      port: 5432
      username: admin
      password: password123
```

**使用 Secret：**

```yaml
# 在 Pod 中引用 Secret
apiVersion: v1
kind: Pod
metadata:
  name: my-app
spec:
  containers:
  - name: app
    image: my-app:latest
    env:
    - name: DATABASE_PASSWORD
      valueFrom:
        secretKeyRef:
          name: my-secret
          key: database-password
    - name: API_KEY
      valueFrom:
        secretKeyRef:
          name: my-secret
          key: api-key
    volumeMounts:
    - name: config
      mountPath: /etc/config
      readOnly: true
  volumes:
  - name: config
    secret:
      secretName: my-secret
```

#### 8.5.3.2 Spring Boot Secret 管理

```java
// 使用 Spring Cloud Config 和 Vault
/*
spring:
  cloud:
    config:
      server:
        vault:
          host: vault.example.com
          port: 8200
          scheme: https
          backend: secret
          default-key: myapp

# 引用 Vault 中的值
# 在 application.yml 中使用 ${vault:key} 语法
# 例如: database.password: ${vault:database/password}
*/

// 安全地注入敏感配置
@Configuration
public class SensitiveConfig {

    @Value("${secret.database-password}")
    private String dbPassword;

    @Value("${secret.api-key}")
    private String apiKey;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://db.example.com:5432/mydb");
        config.setUsername("admin");
        config.setPassword(dbPassword); // 安全注入
        return new HikariDataSource(config);
    }
}
```

#### 8.5.3.3 Go 应用 Secret 配置

```go
package config

import (
    "fmt"
    "os"
    "strconv"

    "github.com/spf13/viper"
)

// 从环境变量或配置中心加载配置
type Config struct {
    Database DatabaseConfig
    Cache    CacheConfig
    Auth     AuthConfig
}

type DatabaseConfig struct {
    Host     string
    Port     int
    Username string
    Password string
}

type AuthConfig struct {
    JWTKey     string
    OAuth2ID   string
    OAuth2Sec  string
}

func LoadConfig() (*Config, error) {
    viper.AutomaticEnv()

    // 从环境变量覆盖
    viper.SetDefault("database.host", "localhost")
    viper.SetDefault("database.port", 5432)

    config := &Config{
        Database: DatabaseConfig{
            Host:     viper.GetString("DATABASE_HOST"),
            Port:     viper.GetInt("DATABASE_PORT"),
            Username: viper.GetString("DATABASE_USERNAME"),
            Password: viper.GetString("DATABASE_PASSWORD"),
        },
        Auth: AuthConfig{
            JWTKey:    viper.GetString("AUTH_JWT_KEY"),
            OAuth2ID:  viper.GetString("OAUTH2_CLIENT_ID"),
            OAuth2Sec: viper.GetString("OAUTH2_CLIENT_SECRET"),
        },
    }

    // 验证必需的配置
    if err := config.Validate(); err != nil {
        return nil, err
    }

    return config, nil
}

func (c *Config) Validate() error {
    if c.Database.Password == "" {
        return fmt.Errorf("DATABASE_PASSWORD is required")
    }
    if c.Auth.JWTKey == "" {
        return fmt.Errorf("AUTH_JWT_KEY is required")
    }
    return nil
}

// 从 Vault 动态获取配置
func LoadSecretsFromVault(vaultAddr, vaultToken, secretPath string) (map[string]string, error) {
    // 实现 Vault 读取逻辑
    return nil, nil
}

func main() {
    // 使用环境变量或 Vault
    config, err := LoadConfig()
    if err != nil {
        panic(err)
    }

    fmt.Printf("DB Host: %s:%d\n", config.Database.Host, config.Database.Port)
}
```

---

## 8.6 安全最佳实践

### 8.6.1 最小权限原则

最小权限原则（Principle of Least Privilege）要求每个组件、用户或服务只能访问完成其任务所需的最小权限。

```mermaid
graph BT
    subgraph "错误的做法"["<b style='color:#fff'>❌ 错误的做法</b>"]
        W1["<b style='color:#fff'>用户</b>"] --> R1["<b style='color:#fff'>读取所有数据</b>"]
        W2["<b style='color:#fff'>服务</b>"] --> R2["<b style='color:#fff'>完全访问权限</b>"]
        W3["<b style='color:#fff'>进程</b>"] --> R3["<b style='color:#fff'>管理员权限</b>"]
    end

    subgraph "正确的做法"["<b style='color:#fff'>✓ 正确的做法</b>"]
        W4["<b style='color:#fff'>用户</b>"] --> R4["<b style='color:#fff'>仅读取授权数据</b>"]
        W5["<b style='color:#fff'>服务</b>"] --> R5["<b style='color:#fff'>仅访问需要的资源</b>"]
        W6["<b style='color:#fff'>进程</b>"] --> R6["<b style='color:#fff'>运行在普通用户下</b>"]
    end

    style W1 fill:#ef4444,stroke:#f87171,stroke-width:2px,color:#ffffff
    style W2 fill:#ef4444,stroke:#f87171,stroke-width:2px,color:#ffffff
    style W3 fill:#ef4444,stroke:#f87171,stroke-width:2px,color:#ffffff
    style R1 fill:#dc2626,stroke:#ef4444,stroke-width:2px,color:#ffffff
    style R2 fill:#dc2626,stroke:#ef4444,stroke-width:2px,color:#ffffff
    style R3 fill:#dc2626,stroke:#ef4444,stroke-width:2px,color:#ffffff
    style W4 fill:#10b981,stroke:#34d399,stroke-width:2px,color:#ffffff
    style W5 fill:#10b981,stroke:#34d399,stroke-width:2px,color:#ffffff
    style W6 fill:#10b981,stroke:#34d399,stroke-width:2px,color:#ffffff
    style R4 fill:#059669,stroke:#34d399,stroke-width:2px,color:#ffffff
    style R5 fill:#059669,stroke:#34d399,stroke-width:2px,color:#ffffff
    style R6 fill:#059669,stroke:#34d399,stroke-width:2px,color:#ffffff
```

#### 8.6.1.1 Kubernetes RBAC 最小权限

```yaml
# 为服务账户分配最小权限
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: production
  name: service-a-role
rules:
# 只允许读取特定命名空间中的特定资源
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list", "watch"]
- apiGroups: [""]
  resources: ["services"]
  verbs: ["get", "list"]
# 不允许访问 secrets（如果不需要）
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: service-a-role-binding
  namespace: production
subjects:
- kind: ServiceAccount
  name: service-a
  namespace: production
roleRef:
  kind: Role
  name: service-a-role
  apiGroup: rbac.authorization.k8s.io
```

#### 8.6.1.2 数据库最小权限

```sql
-- 创建应用用户，仅授予必要的权限
CREATE USER 'app_user'@'%' IDENTIFIED BY 'strong_password';

-- 只授予 SELECT, INSERT, UPDATE 权限
GRANT SELECT, INSERT, UPDATE ON myapp.users TO 'app_user'@'%';
GRANT SELECT ON myapp.orders TO 'app_user'@'%';
GRANT SELECT, INSERT, UPDATE ON myapp.products TO 'app_user'@'%';

-- 不授予 DELETE 权限（如果不需要）
-- 不授予 DROP 或 ALTER 权限

-- 需要只读访问的服务使用只读用户
CREATE USER 'app_readonly'@'%' IDENTIFIED BY 'readonly_password';
GRANT SELECT ON myapp.* TO 'app_readonly'@'%';
```

### 8.6.2 纵深防御

纵深防御（Defense in Depth）是在多个层面实施安全控制，确保即使某一层被突破，攻击者仍面临其他安全障碍。

```mermaid
graph TB
    subgraph "第1层：网络边界"["<b style='color:#fff'>🛡️ 第1层：网络边界</b>"]
        FW["<b style='color:#fff'>防火墙</b>"]
        WAF["<b style='color:#fff'>Web应用防火墙</b>"]
        DDoS["<b style='color:#fff'>DDoS 防护</b>"]
    end

    subgraph "第2层：身份与访问"["<b style='color:#fff'>🔐 第2层：身份与访问</b>"]
        GW["<b style='color:#fff'>API Gateway</b>"]
        IDP["<b style='color:#fff'>身份提供商</b>"]
        MFA["<b style='color:#fff'>多因素认证</b>"]
    end

    subgraph "第3层：应用安全"["<b style='color:#fff'>🔧 第3层：应用安全</b>"]
        Input["<b style='color:#fff'>输入验证</b>"]
        Output["<b style='color:#fff'>输出编码</b>"]
        CSRF["<b style='color:#fff'>CSRF 防护</b>"]
    end

    subgraph "第4层：数据保护"["<b style='color:#fff'>🔒 第4层：数据保护</b>"]
        Enc["<b style='color:#fff'>传输加密</b>"]
        EncS["<b style='color:#fff'>存储加密</b>"]
        Token["<b style='color:#fff'>敏感数据脱敏</b>"]
    end

    subgraph "第5层：运行时安全"["<b style='color:#fff'>📊 第5层：运行时安全</b>"]
        Monitor["<b style='color:#fff'>安全监控</b>"]
        Audit["<b style='color:#fff'>审计日志</b>"]
        Alert["<b style='color:#fff'>异常告警</b>"]
    end

    style FW fill:#ef4444,stroke:#f87171,stroke-width:2px,color:#ffffff
    style WAF fill:#dc2626,stroke:#ef4444,stroke-width:2px,color:#ffffff
    style DDoS fill:#f97316,stroke:#fb923c,stroke-width:2px,color:#ffffff
    style GW fill:#7c3aed,stroke:#a78bfa,stroke-width:2px,color:#ffffff
    style IDP fill:#6366f1,stroke:#818cf8,stroke-width:2px,color:#ffffff
    style MFA fill:#8b5cf6,stroke:#a78bfa,stroke-width:2px,color:#ffffff
    style Input fill:#0ea5e9,stroke:#38bdf8,stroke-width:2px,color:#ffffff
    style Output fill:#06b6d4,stroke:#22d3ee,stroke-width:2px,color:#ffffff
    style CSRF fill:#14b8a6,stroke:#2dd4bf,stroke-width:2px,color:#ffffff
    style Enc fill:#f59e0b,stroke:#fbbf24,stroke-width:2px,color:#ffffff
    style EncS fill:#eab308,stroke:#facc15,stroke-width:2px,color:#ffffff
    style Token fill:#84cc16,stroke:#a3e635,stroke-width:2px,color:#ffffff
    style Monitor fill:#10b981,stroke:#34d399,stroke-width:2px,color:#ffffff
    style Audit fill:#059669,stroke:#34d399,stroke-width:2px,color:#ffffff
    style Alert fill:#16a34a,stroke:#4ade80,stroke-width:2px,color:#ffffff
```

#### 8.6.2.1 多层安全实现

```java
// 第1层：API Gateway 限流和防护
@Component
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 防止 XSS
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        // 防止点击劫持
        httpResponse.setHeader("X-Frame-Options", "DENY");
        // 防止 MIME 类型嗅探
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        // Content Security Policy
        httpResponse.setHeader("Content-Security-Policy", "default-src 'self'");
        // HSTS
        httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        chain.doFilter(request, response);
    }
}

// 第2层：请求速率限制
@Component
public class RateLimitFilter implements Filter {

    private final RateLimiter rateLimiter = RateLimiter.create(100.0); // 每秒100个请求

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String clientId = getClientId(httpRequest);
        if (!rateLimiter.tryAcquire(clientId)) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(429);
            httpResponse.getWriter().write("{\"error\": \"Too Many Requests\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}

// 第3层：输入验证
@Component
public class InputValidationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 验证 Content-Type
        String contentType = httpRequest.getContentType();
        if (!isAllowedContentType(contentType)) {
            httpResponse.setStatus(415);
            return;
        }

        // 验证请求大小
        if (httpRequest.getContentLengthLong() > MAX_REQUEST_SIZE) {
            httpResponse.setStatus(413);
            return;
        }

        chain.doFilter(request, response);
    }
}
```

### 8.6.3 安全扫描

#### 8.6.3.1 依赖漏洞扫描

```xml
<!-- Maven: 使用 OWASP Dependency Check -->
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven-plugin</artifactId>
    <version>8.0.0</version>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>
        <skipProvidedScope>true</skipProvidedScope>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

```yaml
# GitLab CI: 依赖扫描
# .gitlab-ci.yml
dependency_scanning:
  image: maven:3.8-openjdk-11
  script:
    - mvn org.owasp:dependency-check-maven-plugin:check
  artifacts:
    reports:
      dependency_scanning: target/dependency-check-report.json
  allow_failure: true
```

#### 8.6.3.2 容器镜像扫描

```dockerfile
# 多阶段构建，减少攻击面
FROM maven:3.8-openjdk-11 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# 使用最小基础镜像
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 非 root 用户运行
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/target/myapp.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```yaml
# Kubernetes: 禁止使用特权容器
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
metadata:
  name: restricted
spec:
  privileged: false
  allowPrivilegeEscalation: false
  requiredDropCapabilities:
    - ALL
  volumes:
    - 'configMap'
    - 'emptyDir'
    - 'projected'
    - 'secret'
    - 'downwardAPI'
    - 'persistentVolumeClaim'
  hostNetwork: false
  hostIPC: false
  hostPID: false
  runAsUser:
    rule: 'MustRunAsNonRoot'
  seLinux:
    rule: 'RunAsAny'
  supplementalGroups:
    rule: 'RunAsAny'
  fsGroup:
    rule: 'RunAsAny'
```

#### 8.6.3.3 安全扫描工具对比

| 工具 | 类型 | 扫描内容 | 集成方式 |
|------|------|----------|----------|
| SonarQube | SAST | 代码质量+安全 | CI/CD |
| OWASP Dependency Check | SCA | 依赖漏洞 | Maven/Gradle |
| Trivy | SCA | 容器镜像 | CI/CD, Registry |
| Snyk | SCA | 依赖漏洞 | CI/CD, IDE |
| Falco | 运行时 | 异常行为检测 | Kubernetes |
| Vault | 密钥管理 | 敏感数据 | 应用集成 |

---

## 认证方案对比

| 方案 | 适用场景 | 安全性 | 复杂度 | 性能 | 可扩展性 |
|------|----------|--------|--------|------|----------|
| API Key | 服务间通信 | 中 | 低 | 高 | 中 |
| Basic Auth | 简单场景 | 低 | 低 | 高 | 低 |
| OAuth2 + JWT | 用户认证 | 高 | 中 | 中 | 高 |
| mTLS | 服务间高安全 | 高 | 高 | 中 | 高 |
| SPIFFE/SPIRE | 云原生服务网格 | 高 | 高 | 中 | 高 |
| HMAC 签名 | API 签名认证 | 高 | 中 | 中 | 高 |

---

## 本章小结

本章详细介绍了微服务架构中的安全性设计与实现，主要内容包括：

1. **认证与授权**
   - OAuth2.0 的四种授权模式及其适用场景
   - JWT Token 的结构、签名算法和安全验证
   - OpenID Connect 在身份验证中的应用

2. **服务间认证**
   - mTLS 双向认证原理与实现
   - SPIFFE/SPIRE 身份体系在云原生环境中的应用
   - JWT 降级模式的容错设计

3. **API 安全**
   - API Key 认证与 HMAC 签名机制
   - HTTPS/TLS 加密传输配置
   - CORS 跨域资源共享策略

4. **敏感数据管理**
   - HashiCorp Vault 与 AWS KMS 密钥管理
   - 数据库字段加密存储
   - Kubernetes Secret 与环境变量管理

5. **安全最佳实践**
   - 最小权限原则在 K8s RBAC 和数据库权限中的应用
   - 纵深防御的多层安全架构
   - 依赖扫描、容器镜像扫描等安全实践

---

## 思考题

1. **场景分析**：如果微服务需要同时支持 Web 端用户登录和移动端 API 调用，应该选择哪种认证授权方案？为什么？

2. **设计挑战**：在服务网格环境下，如何平衡 mTLS 的安全性和性能开销？请提出至少两种优化方案。

3. **安全评估**：某电商系统包含用户服务、订单服务、支付服务和库存服务。请设计一个完整的服务间认证方案，包括：
   - 各服务使用的认证机制
   - 密钥如何生成、分发和轮换
   - 如何处理服务实例的动态扩缩容

4. **漏洞排查**：作为安全工程师，你发现某微服务存在以下问题：
   - JWT Token 有效期为 7 天
   - 服务间通信使用 HTTP 而非 HTTPS
   - 数据库连接使用 root 账户
   
   请分析这些问题的安全风险并提出修复方案。

5. **架构设计**：设计一个密钥管理方案，满足以下需求：
   - 支持数千个微服务的密钥管理
   - 密钥自动轮换周期为 90 天
   - 支持密钥的紧急撤销
   - 审计日志记录所有密钥访问

---

**上一章**：[第七章：微服务可观测性](./chapter07-observability.md)

**下一章**：[第九章：微服务架构模式](./chapter09-patterns.md)
