# HTTP协议

## 1. 协议概述

### 1.1 什么是HTTP

HTTP（HyperText Transfer Protocol，超文本传输协议）是互联网上应用最为广泛的应用层协议。它定义了客户端与服务器之间请求-响应模型的通信格式，是Web应用的基石。

```
[客户端] <--- HTTP ---> [服务器]
   |                         |
   |<---- 请求/响应模型 ---->|
```

### 1.2 协议发展历史

| 版本 | 年份 | 核心特性 |
|------|------|----------|
| HTTP/0.9 | 1991 | 仅有GET方法，支持纯文本 |
| HTTP/1.0 | 1996 | 支持多种方法、头部、状态码 |
| HTTP/1.1 | 1997 | Keep-Alive持久连接、管道化 |
| HTTP/2 | 2015 | 多路复用、头部压缩、服务器推送 |
| HTTP/3 | 2022 | 基于QUIC/UDP，低延迟 |

### 1.3 协议位置

```
┌─────────────────────────────────────────────────────────────┐
│                        应用层 (HTTP)                        │
├─────────────────────────────────────────────────────────────┤
│                        传输层 (TCP)                         │
├─────────────────────────────────────────────────────────────┤
│                        网络层 (IP)                           │
└─────────────────────────────────────────────────────────────┘

默认端口：80 (HTTP) / 443 (HTTPS)
```

---

## 2. HTTP请求响应模型

### 2.1 经典请求-响应流程

```mermaid
%%{init: {'theme': 'dark', 'themeVariables': { 'darkMode': true, 'background': '#1e1e2e', 'primaryColor': '#89b4fa', 'primaryTextColor': '#cdd6f4', 'primaryBorderColor': '#89b4fa', 'lineColor': '#94e2d5', 'secondaryColor': '#313244', 'tertiaryColor': '#45475a', 'noteBkgColor': '#313244', 'noteTextColor': '#cdd6f4', 'noteBorderColor': '#f38ba8', 'actorBkg': '#313244', 'actorBorder': '#89b4fa', 'actorTextColor': '#cdd6f4', 'signalColor': '#a6e3a1', 'signalTextColor': '#cdd6f4', 'arrowheadColor': '#89b4fa'}}}%%
sequenceDiagram
    autonumber
    participant C as 客户端 (Browser/App)
    participant S as 服务器 (Web Server)

    Note over C,S: HTTP/1.1 持久连接示例

    C->>S: TCP连接建立 (三次握手)
    activate S

    C->>S: HTTP请求 #1: GET /index.html
    S-->>C: HTTP响应 #1: 200 OK + HTML内容

    C->>S: HTTP请求 #2: POST /api/login
    S-->>C: HTTP响应 #2: 200 OK + JSON

    C->>S: HTTP请求 #3: GET /static/logo.png
    S-->>C: HTTP响应 #3: 200 OK + 图片数据

    Note over C,S: Keep-Alive: 连接复用

    C->>S: TCP连接关闭 (四次挥手)
    deactivate S

    Note over C,S: HTTP/2 多路复用示例

    C->>S: TCP连接建立
    activate S

    par 并发请求
        C->>S: 流#1: GET /index.html
        C->>S: 流#2: GET /style.css
        C->>S: 流#3: GET /app.js
        C->>S: 流#4: GET /logo.png
    end

    S-->>C: 流#1: 200 OK + HTML
    S-->>C: 流#2: 200 OK + CSS
    S-->>C: 流#3: 200 OK + JS
    S-->>C: 流#4: 200 OK + PNG

    C->>S: TCP连接关闭
    deactivate S
```

### 2.2 HTTP消息结构

#### 请求消息格式

```
┌──────────────────────────────────────────────────────────────┐
│  请求行 (Request Line)                                      │
│  GET /index.html HTTP/1.1                                   │
├──────────────────────────────────────────────────────────────┤
│  请求头部 (Headers)                                          │
│  Host: www.example.com                                      │
│  User-Agent: Mozilla/5.0...                                  │
│  Accept: text/html...                                        │
│  Accept-Language: zh-CN,zh;q=0.9                            │
│  Accept-Encoding: gzip, deflate, br                         │
│  Connection: keep-alive                                     │
├──────────────────────────────────────────────────────────────┤
│  空行 (CRLF)                                                │
├──────────────────────────────────────────────────────────────┤
│  请求体 (Body) - 可选                                        │
│  username=admin&password=123456                              │
└──────────────────────────────────────────────────────────────┘
```

#### 响应消息格式

```
┌──────────────────────────────────────────────────────────────┐
│  状态行 (Status Line)                                       │
│  HTTP/1.1 200 OK                                            │
├──────────────────────────────────────────────────────────────┤
│  响应头部 (Headers)                                          │
│  Date: Sat, 23 May 2026 10:00:00 GMT                       │
│  Server: Apache/2.4.41                                      │
│  Content-Type: text/html; charset=utf-8                     │
│  Content-Length: 1234                                       │
│  Connection: keep-alive                                    │
│  Cache-Control: max-age=3600                               │
├──────────────────────────────────────────────────────────────┤
│  空行 (CRLF)                                                │
├──────────────────────────────────────────────────────────────┤
│  响应体 (Body)                                              │
│  <!DOCTYPE html>                                            │
│  <html>...                                                  │
└──────────────────────────────────────────────────────────────┘
```

---

## 3. HTTP版本详解

### 3.1 HTTP/1.0 vs HTTP/1.1

| 特性 | HTTP/1.0 | HTTP/1.1 |
|------|----------|----------|
| 持久连接 | 不支持 | 支持 (Keep-Alive) |
| 管道化 | 不支持 | 支持 |
| 虚拟主机 | 不支持 | 支持 (Host头部) |
| 缓存处理 | 基础 | 强大 (Entity Tag等) |
| 断点续传 | 不支持 | 支持 (Range头部) |

### 3.2 HTTP/1.1 核心改进

#### Keep-Alive机制

```
HTTP/1.0: 每次请求建立新连接
┌────────┐     ┌────────┐     ┌────────┐
│Client │────▶│Server │────▶│Client │
└────────┘     └────────┘     └────────┘
   请求#1        响应#1        关闭
┌────────┐     ┌────────┐     ┌────────┐
│Client │────▶│Server │────▶│Client │
└────────┘     └────────┘     └────────┘
   请求#2        响应#2        关闭

HTTP/1.1: 复用连接 (Keep-Alive)
┌──────────────────────────────────────┐
│           TCP连接 (保持)              │
│  ┌────┐  ┌────┐  ┌────┐  ┌────┐    │
│  │Req1│  │Res1│  │Req2│  │Res2│    │
│  └────┘  └────┘  └────┘  └────┘    │
└──────────────────────────────────────┘
```

### 3.3 HTTP/2 核心改进

| 特性 | 说明 | 优势 |
|------|------|------|
| **多路复用** | 单一TCP连接并行传输多个请求/响应 | 避免队头阻塞 |
| **头部压缩** | HPACK算法压缩头部 | 减少传输量 |
| **服务器推送** | 服务器主动推送资源 | 减少客户端请求 |
| **二进制分帧** | 消息转为二进制帧 | 解析更高效 |

```mermaid
%%{init: {'theme': 'dark', 'themeVariables': { 'darkMode': true, 'background': '#1e1e2e', 'primaryColor': '#89b4fa', 'primaryTextColor': '#cdd6f4', 'primaryBorderColor': '#89b4fa', 'lineColor': '#94e2d5', 'secondaryColor': '#313244', 'tertiaryColor': '#45475a', 'noteBkgColor': '#313244', 'noteTextColor': '#cdd6f4', 'noteBorderColor': '#f38ba8', 'actorBkg': '#313244', 'actorBorder': '#89b4fa', 'actorTextColor': '#cdd6f4', 'signalColor': '#a6e3a1', 'signalTextColor': '#cdd6f4', 'arrowheadColor': '#89b4fa'}}}%%
sequenceDiagram
    participant C as 客户端
    participant S as 服务器

    Note over C,S: HTTP/1.1 队头阻塞问题

    C->>S: GET /index.html (流1)
    Note over S: 服务器处理中...
    C->>S: GET /style.css (流2) 等待中
    C->>S: GET /app.js (流3)   等待中
    S-->>C: 响应 流1 (必须按序返回)

    Note over C,S: HTTP/2 多路复用

    C->>S: [帧] 流1 HEADERS
    C->>S: [帧] 流2 HEADERS
    C->>S: [帧] 流3 HEADERS
    C->>S: [帧] 流1 DATA
    S-->>C: [帧] 流2 HEADERS
    S-->>C: [帧] 流2 DATA
    S-->>C: [帧] 流1 DATA
    S-->>C: [帧] 流3 DATA
```

### 3.4 HTTP/3 核心改进

| 特性 | 说明 | 优势 |
|------|------|------|
| **QUIC协议** | 基于UDP而非TCP | 绕过TCP握手延迟 |
| **0-RTT** | 首次连接即可发送数据 | 连接建立极快 |
| **连接迁移** | 连接ID绑定，切换网络不掉线 | 移动网络友好 |
| **无队头阻塞** | UDP独立流，无阻塞问题 | 真正并行传输 |

```
┌─────────────────────────────────────────────────────────────┐
│                      HTTP/3 协议栈                          │
├─────────────────────────────────────────────────────────────┤
│                      应用层 (HTTP/3)                         │
├─────────────────────────────────────────────────────────────┤
│                      QUIC (传输层)                          │
├─────────────────────────────────────────────────────────────┤
│                         UDP                                 │
└─────────────────────────────────────────────────────────────┘

对比:
TCP握手: 1-RTT (HTTP/1.1, HTTP/2)
TLS握手: 2-RTT (HTTPS)
QUIC握手: 0-1-RTT (HTTP/3)
```

---

## 4. 请求方法

### 4.1 方法分类

| 方法 | 安全性 | 幂等性 | 可缓存 | 说明 |
|------|--------|--------|--------|------|
| GET | 安全 | 幂等 | 可缓存 | 获取资源 |
| HEAD | 安全 | 幂等 | 可缓存 | 获取元数据 |
| POST | 不安全 | 非幂等 | 不可缓存 | 提交数据 |
| PUT | 不安全 | 幂等 | 不可缓存 | 创建/替换资源 |
| DELETE | 不安全 | 幂等 | 不可缓存 | 删除资源 |
| PATCH | 不安全 | 非幂等 | 不可缓存 | 部分修改 |
| OPTIONS | 安全 | 幂等 | 不可缓存 | 获取支持的方法 |
| TRACE | 安全 | 幂等 | 不可缓存 | 诊断用途 |
| CONNECT | 不安全 | 非幂等 | 不可缓存 | 建立隧道 |

> **安全性**: 请求是否会在服务器端产生副作用（修改数据）
> **幂等性**: 多次执行相同请求，结果是否一致

### 4.2 GET vs POST 详解

```
┌─────────────────────────────────────────────────────────────┐
│                        GET 请求                              │
├─────────────────────────────────────────────────────────────┤
│ • 参数在URL查询字符串中 (?name=value&age=25)                │
│ • URL有长度限制 (约2048字符)                                │
│ • 参数暴露在地址栏，不适合敏感数据                           │
│ • 可被浏览器缓存、书签保存                                  │
│ • 请求示例:                                                 │
│   GET /api/users?name=张三&age=25 HTTP/1.1                 │
│   Host: api.example.com                                     │
│   Accept: application/json                                  │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                       POST 请求                              │
├─────────────────────────────────────────────────────────────┤
│ • 参数在请求体中，支持任意格式                               │
│ • 无大小限制 (受服务器配置限制)                              │
│ • 参数不在地址栏，适合敏感数据                               │
│ • 不会被浏览器缓存                                          │
│ • 请求示例:                                                 │
│   POST /api/users HTTP/1.1                                  │
│   Host: api.example.com                                     │
│   Content-Type: application/json                            │
│   Content-Length: 38                                        │
│                                                             │
│   {"name":"张三","age":25}                                  │
└─────────────────────────────────────────────────────────────┘
```

### 4.3 PUT vs PATCH

```
PUT vs PATCH 区别:

PUT /api/users/1  (完整替换)
{
  "name": "李四",
  "age": 30,
  "city": "北京"
}

PATCH /api/users/1  (部分修改)
{
  "age": 31
}

结果对比:
PUT: 用户1的全部信息被替换
PATCH: 只有age字段被更新，其他字段保持不变
```

### 4.4 方法使用场景

```mermaid
%%{init: {'theme': 'dark', 'themeVariables': { 'darkMode': true, 'background': '#1e1e2e', 'primaryColor': '#89b4fa', 'primaryTextColor': '#cdd6f4', 'primaryBorderColor': '#89b4fa', 'lineColor': '#94e2d5', 'secondaryColor': '#313244', 'tertiaryColor': '#45475a', 'nodeBkg': '#313244', 'nodeBorder': '#89b4fa', 'clusterBkg': '#313244', 'clusterBorder': '#fab387', 'titleColor': '#cdd6f4', 'edgeLabelColor': '#cdd6f4'}}}%%
flowchart TD
    A["📥 客户端请求"] --> B{"操作类型?"}

    B -->|查询资源| C["GET"]
    B -->|创建资源| D["POST"]
    B -->|完整替换| E["PUT"]
    B -->|部分修改| F["PATCH"]
    B -->|删除资源| G["DELETE"]

    C --> C1["📖 缓存友好<br/>幂等安全"]
    D --> D1["📝 非幂等<br/>常用于表单/上传"]
    E --> E1["🔄 幂等<br/>完整替换"]
    F --> F1["✏️ 非幂等<br/>部分更新"]
    G --> G1["🗑️ 幂等<br/>删除操作"]

    style A fill:#89b4fa,stroke:#89b4fa,color:#1e1e2e
    style B fill:#f38ba8,stroke:#d20f39,color:#fff
    style C fill:#a6e3a1,stroke:#40a02b,color:#1e1e2e
    style D fill:#89b4fa,stroke:#187498,color:#1e1e2e
    style E fill:#cba6f7,stroke:#953bb9,color:#1e1e2e
    style F fill:#fab387,stroke:#fe640b,color:#1e1e2e
    style G fill:#f38ba8,stroke:#d20f39,color:#fff
    style C1 fill:#313244,stroke:#a6e3a1,color:#cdd6f4
    style D1 fill:#313244,stroke:#89b4fa,color:#cdd6f4
    style E1 fill:#313244,stroke:#cba6f7,color:#cdd6f4
    style F1 fill:#313244,stroke:#fab387,color:#cdd6f4
    style G1 fill:#313244,stroke:#f38ba8,color:#cdd6f4
```

---

## 5. 状态码详解

### 5.1 状态码分类

| 类别 | 范围 | 说明 | 典型状态码 |
|------|------|------|------------|
| 1xx | 100-199 | 信息性状态码 | 100 Continue |
| 2xx | 200-299 | 成功状态码 | 200 OK |
| 3xx | 300-399 | 重定向状态码 | 304 Not Modified |
| 4xx | 400-499 | 客户端错误 | 404 Not Found |
| 5xx | 500-599 | 服务器错误 | 500 Internal Server Error |

### 5.2 常见状态码详解

#### 2xx 成功

| 状态码 | 说明 | 使用场景 |
|--------|------|----------|
| 200 | OK | 请求成功，默认成功状态 |
| 201 | Created | 资源创建成功 (POST/PUT) |
| 204 | No Content | 请求成功，无返回内容 (DELETE) |
| 206 | Partial Content | 部分内容 (断点续传) |

#### 3xx 重定向

| 状态码 | 说明 | 使用场景 |
|--------|------|----------|
| 301 | Moved Permanently | 永久重定向 |
| 302 | Found | 临时重定向 |
| 304 | Not Modified | 资源未修改 (缓存) |
| 307 | Temporary Redirect | 临时重定向 (保持方法) |
| 308 | Permanent Redirect | 永久重定向 (保持方法) |

#### 4xx 客户端错误

| 状态码 | 说明 | 使用场景 |
|--------|------|----------|
| 400 | Bad Request | 请求格式错误 |
| 401 | Unauthorized | 未认证 (需要登录) |
| 403 | Forbidden | 已认证但无权限 |
| 404 | Not Found | 资源不存在 |
| 405 | Method Not Allowed | HTTP方法不支持 |
| 408 | Request Timeout | 请求超时 |
| 409 | Conflict | 资源冲突 |
| 413 | Payload Too Large | 请求体过大 |
| 422 | Unprocessable Entity | 语义错误 |
| 429 | Too Many Requests | 请求过于频繁 |

#### 5xx 服务器错误

| 状态码 | 说明 | 使用场景 |
|--------|------|----------|
| 500 | Internal Server Error | 服务器内部错误 |
| 502 | Bad Gateway | 网关错误 |
| 503 | Service Unavailable | 服务不可用 |
| 504 | Gateway Timeout | 网关超时 |

### 5.3 状态码速查图

```mermaid
%%{init: {'theme': 'dark', 'themeVariables': { 'darkMode': true, 'background': '#1e1e2e', 'primaryColor': '#89b4fa', 'primaryTextColor': '#cdd6f4', 'primaryBorderColor': '#89b4fa', 'lineColor': '#94e2d5', 'secondaryColor': '#313244', 'tertiaryColor': '#45475a', 'noteBkgColor': '#313244', 'noteTextColor': '#cdd6f4', 'noteBorderColor': '#f38ba8', 'actorBkg': '#313244', 'actorBorder': '#89b4fa', 'actorTextColor': '#cdd6f4', 'signalColor': '#a6e3a1', 'signalTextColor': '#cdd6f4', 'arrowheadColor': '#89b4fa', 'fillType0': '#f9e2af', 'fillType1': '#a6e3a1', 'fillType2': '#89b4fa', 'fillType3': '#f38ba8', 'fillType4': '#fab387'}}}%%
flowchart LR
    subgraph S1["1xx 信息 (Informational)"]
        direction TB
        C1["100 Continue"]
    end

    subgraph S2["2xx 成功 (Success)"]
        direction TB
        C2A["200 OK"]
        C2B["201 Created"]
        C2C["204 No Content"]
        C2D["206 Partial Content"]
    end

    subgraph S3["3xx 重定向 (Redirection)"]
        direction TB
        C3A["301 Moved Permanently"]
        C3B["302 Found"]
        C3C["304 Not Modified"]
        C3D["307 Temporary Redirect"]
    end

    subgraph S4["4xx 客户端错误 (Client Error)"]
        direction TB
        C4A["400 Bad Request"]
        C4B["401 Unauthorized"]
        C4C["403 Forbidden"]
        C4D["404 Not Found"]
    end

    subgraph S5["5xx 服务器错误 (Server Error)"]
        direction TB
        C5A["500 Internal Server Error"]
        C5B["502 Bad Gateway"]
        C5C["503 Service Unavailable"]
        C5D["504 Gateway Timeout"]
    end

    S1 --> S2 --> S3 --> S4 --> S5

    style S1 fill:#f9e2af,stroke:#f38ba8,color:#1e1e2e
    style S2 fill:#a6e3a1,stroke:#40a02b,color:#1e1e2e
    style S3 fill:#89b4fa,stroke:#187498,color:#1e1e2e
    style S4 fill:#f38ba8,stroke:#d20f39,color:#1e1e2e
    style S5 fill:#fab387,stroke:#fe640b,color:#1e1e2e

    style C1 fill:#313244,stroke:#f9e2af,color:#cdd6f4
    style C2A fill:#313244,stroke:#a6e3a1,color:#cdd6f4
    style C2B fill:#313244,stroke:#a6e3a1,color:#cdd6f4
    style C2C fill:#313244,stroke:#a6e3a1,color:#cdd6f4
    style C2D fill:#313244,stroke:#a6e3a1,color:#cdd6f4
    style C3A fill:#313244,stroke:#89b4fa,color:#cdd6f4
    style C3B fill:#313244,stroke:#89b4fa,color:#cdd6f4
    style C3C fill:#313244,stroke:#89b4fa,color:#cdd6f4
    style C3D fill:#313244,stroke:#89b4fa,color:#cdd6f4
    style C4A fill:#313244,stroke:#f38ba8,color:#cdd6f4
    style C4B fill:#313244,stroke:#f38ba8,color:#cdd6f4
    style C4C fill:#313244,stroke:#f38ba8,color:#cdd6f4
    style C4D fill:#313244,stroke:#f38ba8,color:#cdd6f4
    style C5A fill:#313244,stroke:#fab387,color:#cdd6f4
    style C5B fill:#313244,stroke:#fab387,color:#cdd6f4
    style C5C fill:#313244,stroke:#fab387,color:#cdd6f4
    style C5D fill:#313244,stroke:#fab387,color:#cdd6f4
```

---

## 6. 请求与响应头部详解

### 6.1 通用头部 (General Headers)

```
Date: Sat, 23 May 2026 10:00:00 GMT      # 消息生成时间
Connection: keep-alive                    # 连接管理
Transfer-Encoding: chunked                # 传输编码方式
Cache-Control: max-age=3600               # 缓存控制
Via: 1.1 proxy.example.com               # 代理链
```

### 6.2 请求头部 (Request Headers)

```
Host: www.example.com                    # 目标主机 (必须)
User-Agent: Mozilla/5.0...               # 客户端标识
Accept: text/html,application/json...    # 可接受的内容类型
Accept-Language: zh-CN,zh;q=0.9          # 可接受的语言
Accept-Encoding: gzip,deflate,br         # 可接受的编码
Authorization: Bearer xxx                # 认证信息
Referer: https://www.example.com/page    # 来源页面
Origin: https://www.example.com          # 请求来源 (CORS)
Cookie: session_id=abc123                # Cookie数据
If-Modified-Since: ...                   # 条件请求
If-None-Match: "etag_value"             # 条件请求
Range: bytes=0-1023                      # 范围请求
```

### 6.3 响应头部 (Response Headers)

```
Content-Type: text/html; charset=utf-8  # 内容类型
Content-Length: 1234                      # 内容长度
Content-Encoding: gzip                   # 内容编码
Server: Apache/2.4.41                    # 服务器信息
Set-Cookie: session=abc123; HttpOnly    # 设置Cookie
ETag: "abc123"                          # 资源标识
Last-Modified: Sat, 23 May 2026...      # 最后修改时间
Location: https://...                    # 重定向目标
Access-Control-Allow-Origin: *          # CORS允许来源
Retry-After: 120                         # 限流后重试时间
```

### 6.4 实体头部 (Entity Headers)

```
Content-Type: application/json           # 实体类型
Content-Length: 1024                     # 实体长度
Content-Language: zh-CN                  # 实体语言
Content-Encoding: gzip                    # 实体编码
Content-Location: /files/doc.html        # 实体位置
Content-Range: bytes 0-1023/4096         # 范围响应
Expires: Sat, 23 May 2026 12:00:00 GMT  # 过期时间
```

### 6.5 自定义头部

```
# 常用自定义头部示例
X-Request-ID: uuid-xxx                  # 请求追踪ID
X-Api-Key: your-api-key                 # API密钥
X-Forwarded-For: client_ip              # 真实客户端IP
X-Custom-Header: custom-value           # 自定义值
```

---

## 7. 连接管理

### 7.1 Keep-Alive vs Connection: close

```
┌─────────────────────────────────────────────────────────────┐
│              HTTP/1.1 默认 Keep-Alive                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Connection: keep-alive (默认，无需显式设置)                │
│                                                             │
│   ┌────────┐                                               │
│   │ Client │                                               │
│   └────┬───┘                                               │
│        │                                                    │
│   ┌────▼───┐                                               │
│   │  TCP   │ ──── 持久连接，复用TCP ────                    │
│   │ Connect│                                               │
│   └────┬───┘                                               │
│        │                                                    │
│   ┌────▼───┐                                               │
│   │  HTTP  │ ──── 请求#1 ──▶ 请求#2 ──▶ 请求#3 ────        │
│   │Request │ ◀─── 响应#1 ◀── 响应#2 ◀── 响应#3             │
│   └────────┘                                               │
│                                                             │
│   优点: 减少TCP握手开销                                     │
│   缺点: 队头阻塞问题                                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│              Connection: close 关闭连接                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Connection: close (显式关闭)                              │
│                                                             │
│   ┌────────┐                                               │
│   │ Client │                                               │
│   └────┬───┘                                               │
│        │                                                    │
│   ┌────▼───┐                                               │
│   │  TCP   │ ──── 连接1: 请求#1 ──▶ ◀─── 响应#1            │
│   │ Connect│ ──── 连接关闭                                 │
│   └────┬───┘                                               │
│        │                                                    │
│   ┌────▼───┐                                               │
│   │  TCP   │ ──── 连接2: 请求#2 ──▶ ◀─── 响应#2            │
│   │ Connect│ ──── 连接关闭                                 │
│   └────┬───┘                                               │
│        │                                                    │
│   适用: 明确知道这是最后一次请求                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 7.2 TCP连接过程

```mermaid
%%{init: {'theme': 'dark', 'themeVariables': { 'darkMode': true, 'background': '#1e1e2e', 'primaryColor': '#89b4fa', 'primaryTextColor': '#cdd6f4', 'primaryBorderColor': '#89b4fa', 'lineColor': '#94e2d5', 'secondaryColor': '#313244', 'tertiaryColor': '#45475a', 'noteBkgColor': '#313244', 'noteTextColor': '#cdd6f4', 'noteBorderColor': '#f38ba8', 'actorBkg': '#313244', 'actorBorder': '#89b4fa', 'actorTextColor': '#cdd6f4', 'signalColor': '#a6e3a1', 'signalTextColor': '#cdd6f4', 'arrowheadColor': '#89b4fa'}}}%%
sequenceDiagram
    participant C as 客户端
    participant S as 服务器

    Note over C,S: HTTP/1.1 Keep-Alive 完整生命周期

    rect rgba(137, 180, 250, 0.15)
        Note over C,S: 1. TCP三次握手
        C->>S: SYN (seq=x)
        S-->>C: SYN+ACK (seq=y, ack=x+1)
        C->>S: ACK (ack=y+1)
        Note over C,S: TCP连接建立完成
    end

    rect rgba(203, 166, 247, 0.15)
        Note over C,S: 2. HTTP请求响应
        C->>S: GET /page1 HTTP/1.1
        S-->>C: 200 OK + Content
    end

    rect rgba(166, 227, 161, 0.15)
        Note over C,S: 3. 连接复用 (Keep-Alive)
        C->>S: GET /page2 HTTP/1.1
        S-->>C: 200 OK + Content
    end

    rect rgba(243, 139, 168, 0.15)
        Note over C,S: 4. TCP四次挥手
        C->>S: FIN
        S-->>C: ACK
        S->>C: FIN
        C-->>S: ACK
        Note over C,S: TCP连接关闭
    end
```

---

## 8. Cookie与Session机制

### 8.1 无状态与会话

```
HTTP是无状态协议，但业务需要会话:

┌─────────────────────────────────────────────────────────────┐
│                     无状态 vs 有会话                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  无状态 (HTTP):                                             │
│    每次请求独立，服务器不保留客户端状态                      │
│    Client ──▶ 请求1 ──▶ Server                             │
│    Client ──▶ 请求2 ──▶ Server (服务器不认识你)            │
│                                                             │
│  有会话 (Session):                                          │
│    服务器保存客户端状态                                      │
│    Client ──▶ 请求1 ──▶ Server [创建Session]                │
│    Client ──▶ 请求2 ──▶ Server [识别Session]                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 8.2 Cookie机制

```mermaid
%%{init: {'theme': 'dark', 'themeVariables': { 'background': '#1e1e2e', 'primaryColor': '#89b4fa', 'primaryTextColor': '#cdd6f4', 'primaryBorderColor': '#89b4fa', 'lineColor': '#94e2d5', 'secondaryColor': '#313244', 'tertiaryColor': '#45475a', 'noteBkgColor': '#313244', 'noteTextColor': '#cdd6f4', 'noteBorderColor': '#f38ba8', 'actorBkg': '#313244', 'actorBorder': '#89b4fa', 'actorTextColor': '#cdd6f4', 'signalColor': '#a6e3a1', 'signalTextColor': '#cdd6f4', 'arrowheadColor': '#89b4fa'}}}%%
sequenceDiagram
    participant C as 浏览器
    participant S as 服务器

    Note over C,S: Cookie 工作流程

    C->>S: POST /login HTTP/1.1
    Note right of C: 用户名密码

    S-->>C: HTTP/1.1 200 OK
    Note left of S: Set-Cookie: session_id=abc123<br/>HttpOnly; Secure; SameSite=Strict

    Note over C,S: 服务器设置Cookie

    C->>S: GET /profile HTTP/1.1
    Note right of C: Cookie: session_id=abc123

    Note over C,S: 浏览器自动携带Cookie

    S-->>C: HTTP/1.1 200 OK
    Note left of S: 识别用户身份，返回个性化内容
```

### 8.3 Cookie属性详解

```
Set-Cookie: session_id=abc123; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=3600

属性说明:
┌──────────────────────────────────────────────────────────────┐
│  session_id=abc123  │  Cookie的名称和值                       │
├────────────────────┼────────────────────────────────────────┤
│  Path=/            │  有效路径 (默认当前路径)                 │
├────────────────────┼────────────────────────────────────────┤
│  HttpOnly          │  无法通过JavaScript访问 (防XSS)          │
├────────────────────┼────────────────────────────────────────┤
│  Secure            │  仅HTTPS传输                            │
├────────────────────┼────────────────────────────────────────┤
│  SameSite=Strict   │  不随跨站请求发送 (防CSRF)               │
│  SameSite=Lax      │  导航请求除外                           │
│  SameSite=None     │  允许跨站 (需Secure)                    │
├────────────────────┼────────────────────────────────────────┤
│  Max-Age=3600      │  有效期(秒)，Expires指定具体时间        │
│  Expires=...       │  过期时间                               │
└────────────────────┴────────────────────────────────────────┘
```

### 8.4 Session机制

```
┌─────────────────────────────────────────────────────────────┐
│                    Session 存储方案                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 内存存储 (开发环境)                                      │
│     优点: 速度快                                            │
│     缺点: 重启丢失，无法分布式                               │
│                                                             │
│  2. Redis存储 (生产环境推荐)                                 │
│     优点: 高速，支持分布式                                   │
│     缺点: 需要额外服务                                       │
│                                                             │
│  3. 数据库存储                                               │
│     优点: 持久化                                            │
│     缺点: 速度慢                                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘

SessionID传输方式:
┌─────────────────────────────────────────────────────────────┐
│  1. Cookie (推荐)                                           │
│     Set-Cookie: SESSIONID=xxx                              │
│                                                             │
│  2. URL参数                                                │
│     http://example.com/page?SESSIONID=xxx                  │
│                                                             │
│  3. 请求头                                                  │
│     X-Session-ID: xxx                                      │
└─────────────────────────────────────────────────────────────┘
```

---

## 9. 缓存控制

### 9.1 缓存类型

```
┌─────────────────────────────────────────────────────────────┐
│                    HTTP 缓存体系                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                    缓存分类                             │ │
│  ├────────────────────────────────────────────────────────┤ │
│  │                                                        │ │
│  │  ┌─────────────────┐    ┌─────────────────┐           │ │
│  │  │   私有缓存      │    │   共享缓存       │           │ │
│  │  │  (浏览器缓存)   │    │  (代理缓存/CDN)  │           │ │
│  │  └─────────────────┘    └─────────────────┘           │ │
│  │                                                        │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                             │
│  缓存命中流程:                                              │
│  1. 客户端请求资源                                          │
│  2. 检查缓存是否有效                                        │
│  3. 命中 ──▶ 返回缓存                                      │
│  4. 未命中 ──▶ 向服务器请求 ──▶ 存入缓存 ──▶ 返回          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 9.2 Cache-Control 指令

```
请求指令 (Client → Server):
┌──────────────────────────────────────────────────────────────┐
│  Cache-Control: max-age=3600        # 接受缓存最大有效期    │
│  Cache-Control: min-fresh=600       # 缓存至少新鲜600秒     │
│  Cache-Control: max-stale=3600      # 过期多久仍可接受      │
│  Cache-Control: no-cache            # 强制重验证            │
│  Cache-Control: no-store            # 不缓存任何内容        │
│  Cache-Control: only-if-cached      # 仅返回缓存           │
└──────────────────────────────────────────────────────────────┘

响应指令 (Server → Client):
┌──────────────────────────────────────────────────────────────┐
│  Cache-Control: public               # 可被任何缓存存储     │
│  Cache-Control: private              # 仅浏览器缓存         │
│  Cache-Control: max-age=3600         # 缓存有效期           │
│  Cache-Control: s-maxage=7200        # 代理缓存有效期       │
│  Cache-Control: no-cache            # 使用前必须重验证     │
│  Cache-Control: no-store             # 禁止缓存            │
│  Cache-Control: must-revalidate      # 过期必须重验证       │
│  Cache-Control: proxy-revalidate     # 代理过期必须重验证   │
│  Cache-Control: immutable            # 内容不变不重验证     │
└──────────────────────────────────────────────────────────────┘
```

### 9.3 缓存验证机制

```mermaid
%%{init: {'theme': 'dark', 'themeVariables': { 'darkMode': true, 'background': '#1e1e2e', 'primaryColor': '#89b4fa', 'primaryTextColor': '#cdd6f4', 'primaryBorderColor': '#89b4fa', 'lineColor': '#94e2d5', 'secondaryColor': '#313244', 'tertiaryColor': '#45475a', 'noteBkgColor': '#313244', 'noteTextColor': '#cdd6f4', 'noteBorderColor': '#f38ba8', 'actorBkg': '#313244', 'actorBorder': '#89b4fa', 'actorTextColor': '#cdd6f4', 'signalColor': '#a6e3a1', 'signalTextColor': '#cdd6f4', 'arrowheadColor': '#89b4fa'}}}%%
sequenceDiagram
    participant C as 客户端
    participant P as 代理缓存
    participant S as 源服务器

    Note over C,S: 强缓存 (Freshness)

    C->>S: GET /static.js HTTP/1.1
    S-->>C: HTTP/1.1 200 OK
    Note over C,S: Cache-Control: max-age=3600<br/>Expires: Sat, 23 May 2026 11:00:00 GMT
    Note over C,S: 3600秒内重复请求，直接使用缓存

    Note over C,S: 协商缓存 (Validation)

    C->>S: GET /static.js HTTP/1.1
    Note over C: If-Modified-Since: Sat, 23 May 2026 09:00:00 GMT<br/>If-None-Match: "abc123"

    alt 资源未变化
        S-->>C: HTTP/1.1 304 Not Modified
        Note over C,S: 使用缓存，不传输body
    else 资源已变化
        S-->>C: HTTP/1.1 200 OK
        Note over S: ETag: "def456"
        Note over S: Body: [新内容]
        Note over C,S: 返回新内容，更新缓存
    end
```

### 9.4 ETag 与 Last-Modified

```
┌─────────────────────────────────────────────────────────────┐
│                     ETag 机制                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  服务器生成:                                                │
│  ETag: "abc123"           # 基于内容hash                    │
│  ETag: W/"abc123"        # 弱校验器                         │
│                                                             │
│  客户端验证:                                                │
│  If-None-Match: "abc123"  # 匹配则返回304                  │
│  If-None-Match: *          # 强制重新获取                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  Last-Modified 机制                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  服务器返回:                                                │
│  Last-Modified: Sat, 23 May 2026 09:00:00 GMT              │
│                                                             │
│  客户端验证:                                                │
│  If-Modified-Since: Sat, 23 May 2026 09:00:00 GMT         │
│                                                             │
│  注意: 仅精确到秒，可能不准确                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘

优先级: ETag > Last-Modified (ETag更精确)
```

### 9.5 缓存决策流程

```mermaid
%%{init: {'theme': 'dark', 'themeVariables': { 'darkMode': true, 'background': '#1e1e2e', 'primaryColor': '#89b4fa', 'primaryTextColor': '#cdd6f4', 'primaryBorderColor': '#89b4fa', 'lineColor': '#94e2d5', 'secondaryColor': '#313244', 'tertiaryColor': '#45475a', 'nodeBkg': '#313244', 'nodeBorder': '#89b4fa', 'clusterBkg': '#313244', 'clusterBorder': '#fab387', 'titleColor': '#cdd6f4', 'edgeLabelColor': '#cdd6f4'}}}%%
flowchart TD
    A["📥 收到请求"] --> B{"Cache-Control<br/>no-store?"}

    B -->|是| B1["🚫 不缓存<br/>直接请求服务器"]
    B -->|否| C{"Cache-Control<br/>no-cache?"}

    C -->|是| C1["🔍 向服务器验证<br/>发送条件请求"]
    C -->|否| D{"缓存新鲜?"}

    D -->|是| D1["✅ 直接返回缓存<br/>200 OK (from cache)"]
    D -->|否| E{"max-age<br/>expired?"}

    E -->|是| E1["🔍 向服务器验证<br/>条件请求"]
    E -->|否| E2["✅ 返回缓存<br/>200 OK (from cache)"]

    C1 --> F{"服务器返回<br/>304 Not Modified?"}
    E1 --> F

    F -->|是| F1["🔄 更新缓存header<br/>返回304"]
    F -->|否| F2["📦 返回新内容<br/>更新缓存"]

    style A fill:#89b4fa,stroke:#89b4fa,color:#1e1e2e
    style B fill:#f38ba8,stroke:#f38ba8,color:#1e1e2e
    style B1 fill:#f38ba8,stroke:#d20f39,color:#fff
    style C fill:#f9e2af,stroke:#f9e2af,color:#1e1e2e
    style C1 fill:#cba6f7,stroke:#953bb9,color:#fff
    style D fill:#f9e2af,stroke:#f9e2af,color:#1e1e2e
    style D1 fill:#a6e3a1,stroke:#40a02b,color:#1e1e2e
    style E fill:#fab387,stroke:#fab387,color:#1e1e2e
    style E1 fill:#cba6f7,stroke:#953bb9,color:#fff
    style E2 fill:#a6e3a1,stroke:#40a02b,color:#1e1e2e
    style F fill:#89b4fa,stroke:#89b4fa,color:#1e1e2e
    style F1 fill:#89b4fa,stroke:#187498,color:#1e1e2e
    style F2 fill:#a6e3a1,stroke:#40a02b,color:#1e1e2e
```

---

## 10. 工程示例

### 10.1 cURL命令详解

```bash
# 基础请求
curl https://api.example.com/users

# 显示详细请求/响应头
curl -v https://api.example.com/users

# 显示完整请求/响应头 (包含body)
curl -i https://api.example.com/users

# 自定义请求方法
curl -X POST https://api.example.com/users

# 发送JSON数据
curl -X POST https://api.example.com/users \
  -H "Content-Type: application/json" \
  -d '{"name":"张三","email":"zhang@example.com"}'

# 发送表单数据
curl -X POST https://api.example.com/form \
  -d "username=admin" \
  -d "password=123456"

# 自定义请求头
curl https://api.example.com/users \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" \
  -H "Accept: application/json" \
  -H "X-Request-ID: 12345"

# 下载文件
curl -O https://api.example.com/file.zip
curl -o output.zip https://api.example.com/file.zip

# 限速下载 (100KB/s)
curl --limit-rate 100k -O https://api.example.com/largefile.iso

# 断点续传
curl -C - -O https://api.example.com/largefile.iso

# 带Cookie请求
curl -b "session_id=abc123" https://api.example.com/profile
curl -c cookies.txt -b cookies.txt https://api.example.com/

# 保存响应头到文件
curl -D headers.txt -o response.html https://api.example.com/

# 跟随重定向
curl -L https://short.url/abc

# 忽略SSL证书错误 (测试用)
curl -k https://expired.example.com/

# 使用代理
curl -x http://proxy.example.com:8080 https://api.example.com/

# 并发请求 (使用xargs)
seq 1 10 | xargs -P 5 -I {} curl -s https://api.example.com/item/{}
```

### 10.2 Python requests库示例

```python
import requests
from requests.auth import HTTPBasicAuth
import json

# ============================================================
# 基础GET请求
# ============================================================
response = requests.get('https://api.example.com/users')
print(response.status_code)
print(response.json())

# ============================================================
# 带参数的GET请求
# ============================================================
params = {'page': 1, 'limit': 10, 'name': '张三'}
response = requests.get('https://api.example.com/users', params=params)

# ============================================================
# POST JSON请求
# ============================================================
data = {'name': '李四', 'email': 'li@example.com'}
headers = {'Content-Type': 'application/json'}

response = requests.post(
    'https://api.example.com/users',
    json=data,  # 自动设置Content-Type，自动序列化
    headers=headers
)
print(response.status_code)
print(response.json())

# ============================================================
# 表单提交
# ============================================================
form_data = {'username': 'admin', 'password': '123456'}
response = requests.post(
    'https://api.example.com/login',
    data=form_data  # application/x-www-form-urlencoded
)

# ============================================================
# 文件上传
# ============================================================
files = {'file': open('document.pdf', 'rb')}
response = requests.post(
    'https://api.example.com/upload',
    files=files
)

# 带额外参数
files = {
    'file': ('report.pdf', open('report.pdf', 'rb'), 'application/pdf'),
    'description': '季度报告'
}
response = requests.post('https://api.example.com/upload', files=files)

# ============================================================
# 自定义请求头
# ============================================================
headers = {
    'User-Agent': 'MyApp/1.0',
    'Authorization': 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9',
    'Accept': 'application/json',
    'X-Request-ID': 'req-12345'
}
response = requests.get('https://api.example.com/protected', headers=headers)

# ============================================================
# Basic认证
# ============================================================
response = requests.get(
    'https://api.example.com/admin',
    auth=HTTPBasicAuth('admin', 'password')
)

# ============================================================
# 带Cookie的请求
# ============================================================
# 发送Cookie
cookies = {'session_id': 'abc123', 'theme': 'dark'}
response = requests.get(
    'https://api.example.com/profile',
    cookies=cookies
)

# 使用Session保持Cookie
session = requests.Session()
session.cookies.set('session_id', 'abc123')

# 登录
session.post('https://api.example.com/login', json={
    'username': 'admin',
    'password': 'password'
})

# 后续请求自动携带Cookie
response = session.get('https://api.example.com/profile')
print(response.json())

# ============================================================
# 处理响应
# ============================================================
response = requests.get('https://api.example.com/users/1')

# 状态码
print(response.status_code)  # 200

# 响应头
print(response.headers['Content-Type'])  # application/json

# 响应体
print(response.text)    # 原始文本
print(response.json())  # JSON解析

# 二进制响应 (图片等)
with open('image.png', 'wb') as f:
    f.write(response.content)

# ============================================================
# 错误处理
# ============================================================
try:
    response = requests.get('https://api.example.com/users', timeout=5)
    response.raise_for_status()  # 4xx/5xx时抛出异常
except requests.exceptions.Timeout:
    print('请求超时')
except requests.exceptions.ConnectionError:
    print('连接错误')
except requests.exceptions.HTTPError as e:
    print(f'HTTP错误: {e}')
except requests.exceptions.RequestException as e:
    print(f'请求异常: {e}')

# ============================================================
# 缓存控制示例
# ============================================================
# 强制使用缓存 (需配合Cache-Control)
response = requests.get(
    'https://api.example.com/data',
    headers={'Cache-Control': 'only-if-cached'}
)

# 条件请求 (If-None-Match)
response = requests.get(
    'https://api.example.com/data',
    headers={'If-None-Match': '"abc123"'}
)
if response.status_code == 304:
    print('使用缓存')
else:
    print(response.json())

# ============================================================
# 下载大文件 (流式)
# ============================================================
with requests.get('https://api.example.com/largefile.zip',
                  stream=True) as response:
    response.raise_for_status()
    with open('largefile.zip', 'wb') as f:
        for chunk in response.iter_content(chunk_size=8192):
            f.write(chunk)

# ============================================================
# HTTPS与SSL
# ============================================================
# 使用自定义CA证书
response = requests.get(
    'https://api.example.com/',
    verify='/path/to/ca-bundle.crt'
)

# 忽略SSL验证 (测试用)
response = requests.get('https://expired.example.com/', verify=False)

# ============================================================
# 代理设置
# ============================================================
proxies = {
    'http': 'http://proxy.example.com:8080',
    'https': 'http://proxy.example.com:8080'
}
response = requests.get('https://api.example.com/', proxies=proxies)
```

### 10.3 完整请求响应示例

#### GET请求示例

```http
GET /api/users?page=1&limit=10 HTTP/1.1
Host: api.example.com
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
Accept: application/json, text/plain, */*
Accept-Language: zh-CN,zh;q=0.9,en;q=0.8
Accept-Encoding: gzip, deflate, br
Connection: keep-alive
Referer: https://www.example.com/users
```

```http
HTTP/1.1 200 OK
Date: Sat, 23 May 2026 10:00:00 GMT
Server: nginx/1.24.0
Content-Type: application/json; charset=utf-8
Content-Length: 1024
Connection: keep-alive
Cache-Control: max-age=3600
ETag: "abc123def456"
X-Request-ID: req-12345

{
  "code": 0,
  "message": "success",
  "data": {
    "users": [
      {"id": 1, "name": "张三", "email": "zhang@example.com"},
      {"id": 2, "name": "李四", "email": "li@example.com"}
    ],
    "pagination": {
      "page": 1,
      "limit": 10,
      "total": 100,
      "totalPages": 10
    }
  }
}
```

#### POST请求示例

```http
POST /api/users HTTP/1.1
Host: api.example.com
Content-Type: application/json; charset=utf-8
Content-Length: 78
Accept: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9
X-Request-ID: req-67890
Origin: https://www.example.com

{
  "name": "王五",
  "email": "wang@example.com",
  "password": "securePassword123",
  "age": 28
}
```

```http
HTTP/1.1 201 Created
Date: Sat, 23 May 2026 10:00:01 GMT
Server: nginx/1.24.0
Content-Type: application/json; charset=utf-8
Content-Length: 156
Connection: keep-alive
Location: /api/users/3
X-Request-ID: req-67890

{
  "code": 0,
  "message": "用户创建成功",
  "data": {
    "id": 3,
    "name": "王五",
    "email": "wang@example.com",
    "createdAt": "2026-05-23T10:00:01Z"
  }
}
```

#### 错误响应示例

```http
HTTP/1.1 400 Bad Request
Date: Sat, 23 May 2026 10:00:02 GMT
Server: nginx/1.24.0
Content-Type: application/json; charset=utf-8
Content-Length: 245
Connection: keep-alive
X-Request-ID: req-11111

{
  "code": 400,
  "message": "参数验证失败",
  "errors": [
    {
      "field": "email",
      "message": "邮箱格式不正确"
    },
    {
      "field": "password",
      "message": "密码长度至少8位"
    }
  ]
}
```

#### 304 Not Modified示例

```http
GET /api/users HTTP/1.1
Host: api.example.com
If-None-Match: "abc123def456"
If-Modified-Since: Sat, 23 May 2026 10:00:00 GMT
```

```http
HTTP/1.1 304 Not Modified
Date: Sat, 23 May 2026 10:05:00 GMT
Server: nginx/1.24.0
Connection: keep-alive
ETag: "abc123def456"
Cache-Control: max-age=3600
```

---

## 11. 常见问题与调试

### 11.1 常见HTTP问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 400 Bad Request | 请求格式错误 | 检查JSON/表单格式 |
| 401 Unauthorized | 未认证 | 添加Authorization头 |
| 403 Forbidden | 无权限 | 检查用户权限 |
| 404 Not Found | 资源不存在 | 检查URL是否正确 |
| 405 Method Not Allowed | 方法不支持 | 检查HTTP方法 |
| 429 Too Many Requests | 请求过于频繁 | 添加延迟，使用缓存 |
| 500 Internal Server Error | 服务器错误 | 查看服务器日志 |
| 502 Bad Gateway | 网关错误 | 检查上游服务 |
| 503 Service Unavailable | 服务不可用 | 检查服务状态 |

### 11.2 调试工具

```bash
# 查看完整请求响应
curl -v https://api.example.com/

# 查看请求时间
curl -w "@curl-format.txt" -o /dev/null -s https://api.example.com/

# curl-format.txt 内容:
#    time_namelookup:  %{time_namelookup}\n
#    time_connect:  %{time_connect}\n
#    time_starttransfer:  %{time_starttransfer}\n
#    time_total:  %{time_total}\n

# 使用Chrome DevTools查看网络请求
# F12 -> Network -> 选中请求 -> Headers/Payload/Response

# 使用Wireshark抓包
# 过滤器: http.host == "example.com"
```

---

## 12. 最佳实践

### 12.1 性能优化

```
1. 使用HTTP/2或HTTP/3
   - 多路复用避免队头阻塞
   - 头部压缩减少传输量

2. 启用缓存
   - 合理设置Cache-Control
   - 使用ETag进行精准验证

3. 压缩传输
   - 启用Gzip/Brotli压缩
   - 压缩文本资源(HTML/CSS/JS)

4. 连接复用
   - 使用Keep-Alive
   - 使用HTTP/2多路复用

5. 减少请求
   - 资源合并 (CSS/JS合并)
   - 雪碧图
   - 内联小资源 (data URI)
```

### 12.2 安全建议

```
1. 使用HTTPS
   - 加密传输数据
   - 防止中间人攻击

2. 设置安全头部
   - Strict-Transport-Security
   - X-Content-Type-Options: nosniff
   - X-Frame-Options: DENY
   - Content-Security-Policy

3. Cookie安全
   - 设置HttpOnly
   - 设置Secure
   - 设置SameSite

4. 输入验证
   - 验证所有用户输入
   - 防止XSS/SQL注入

5. 限流防护
   - 防止DDoS
   - 防止暴力破解
```

---
