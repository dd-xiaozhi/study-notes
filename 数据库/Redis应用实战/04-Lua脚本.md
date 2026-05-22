# Redis Lua 脚本实战

> 本文介绍 Redis Lua 脚本的基础语法、Spring Boot 3 整合方式以及三个企业级实战案例。
>
> **环境要求**：Java 21+, Spring Boot 3.x, Redis 6.0+

---

## 目录

1. [Lua 脚本基础语法](#1-lua-脚本基础语法)
2. [Spring Boot 3 整合 Redis Lua 脚本](#2-spring-boot-3-整合-redis-lua-脚本)
3. [实战案例一：库存扣减原子操作](#3-实战案例一库存扣减原子操作)
4. [实战案例二：扣减失败自动回滚](#4-实战案例二扣减失败自动回滚)
5. [实战案例三：访问频率限制](#5-实战案例三访问频率限制)
6. [Lua 脚本调试技巧](#6-lua-脚本调试技巧)

---

## 1. Lua 脚本基础语法

### 1.1 为什么使用 Lua 脚本

Redis Lua 脚本解决以下核心问题：

| 问题 | 说明 |
|------|------|
| **原子性** | 多命令组合执行，保证要么全部成功，要么全部失败 |
| **性能** | 减少网络往返次数，一个请求完成多个操作 |
| **事务替代** | 解决 `MULTI/EXEC` 不支持回滚的问题 |

### 1.2 Lua 基本数据类型

```lua
-- 字符串
local name = "Redis"

-- 数字（Lua 5.3+ 支持整数和浮点数）
local count = 100
local price = 99.99

-- 布尔值
local isActive = true

-- nil（空值）
local empty = nil

-- 表（Table，Lua 唯一的数据结构）
local user = {name = "张三", age = 25}
local list = {1, 2, 3, 4, 5}
```

### 1.3 变量与注释

```lua
-- 单行注释

--[[
    多行注释
    这是第二行
]]

-- 局部变量（推荐使用）
local name = "Redis"

-- 全局变量（不推荐）
-- globalVar = "不应使用"
```

### 1.4 运算符

```lua
-- 算术运算符
local a, b = 10, 3
print(a + b)  -- 13
print(a - b)  -- 7
print(a * b)  -- 30
print(a / b)  -- 3.333...
print(a // b) -- 3 (整除，Lua 5.3+)
print(a % b)  -- 1
print(a ^ b)  -- 1000 (幂运算)

-- 比较运算符（返回 true 或 false）
print(a == b)  -- false
print(a ~= b)  -- true (不等于)
print(a > b)   -- true
print(a < b)   -- false

-- 逻辑运算符
local x, y = true, false
print(x and y) -- false
print(x or y)  -- true
print(not x)   -- false

-- 字符串操作
local s = "Hello"
print(s .. " World")  -- 连接字符串: "Hello World"
print(#s)              -- 长度: 5
```

### 1.5 条件判断

```lua
local score = 85

if score >= 90 then
    print("优秀")
elseif score >= 80 then
    print("良好")
elseif score >= 60 then
    print("及格")
else
    print("不及格")
end
```

### 1.6 循环

```lua
-- 数值循环
for i = 1, 5 do
    print(i)
end

-- 步长循环
for i = 10, 1, -2 do
    print(i)  -- 10, 8, 6, 4, 2
end

-- 遍历表
local fruits = {"苹果", "香蕉", "橙子"}
for index, fruit in ipairs(fruits) do
    print(index, fruit)
end

-- while 循环
local i = 1
while i <= 5 do
    print(i)
    i = i + 1
end
```

### 1.7 函数

```lua
-- 定义函数
local function add(a, b)
    return a + b
end

-- 多返回值
local function divide(a, b)
    if b == 0 then
        return nil, "除数不能为零"
    end
    return a / b, nil
end

-- 可变参数
local function sum(...)
    local args = {...}
    local total = 0
    for _, v in ipairs(args) do
        total = total + v
    end
    return total
end
```

### 1.8 Redis Lua 脚本专用语法

```lua
-- Redis KEYS 和 ARGV 参数
-- KEYS[1] 表示第一个键，ARGV[1] 表示第一个参数
local key = KEYS[1]
local arg = ARGV[1]

-- Redis API 调用
redis.call('SET', key, arg)           -- 执行命令，无返回值
local result = redis.call('GET', key) -- 执行命令，返回结果

-- redis.pcall 与 redis.call 相同，但错误处理方式不同
-- pcall 在错误时返回错误表而不是抛出异常

-- 类型转换
-- Redis 返回的是数字或字符串，Lua 会自动转换
redis.call('INCR', key)  -- 返回 Lua 数字
redis.call('GET', key)    -- 返回字符串

-- 返回值约定
-- 返回 1 表示成功
-- 返回 0 表示失败
-- 返回 nil 表示空结果
-- 返回错误信息字符串表示失败并附带原因

return 1  -- 成功
return 0  -- 失败
return nil, "库存不足"  -- 失败并返回错误信息
```

---

## 2. Spring Boot 3 整合 Redis Lua 脚本

### 2.1 添加依赖

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Spring Boot Starter Data Redis -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Lettuce 连接池（推荐）-->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-pool2</artifactId>
    </dependency>
</dependencies>
```

### 2.2 配置文件

```yaml
# application.yml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: # 可选
      database: 0
      lettuce:
        pool:
          max-active: 10
          max-idle: 5
          min-idle: 2
          max-wait: 3000ms
```

### 2.3 Redis 配置类

```java
package com.example.redis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 * 配置序列化方式和连接工厂
 */
@Configuration
public class RedisConfig {

    /**
     * 配置 RedisTemplate
     * 使用 JSON 序列化方式存储对象
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 创建 JSON 序列化器
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        // 设置键的序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // 设置值的序列化器
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置 StringRedisTemplate
     * 专门用于处理字符串类型，操作更简单高效
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
```

### 2.4 执行 Lua 脚本的方式

#### 方式一：StringRedisTemplate（推荐）

```java
package com.example.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Lua 脚本执行服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LuaScriptService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 执行简单的 Lua 脚本
     * @param script Lua 脚本内容
     * @param keys 键列表
     * @param args 参数列表
     * @return 脚本执行结果
     */
    public String execute(String script, List<String> keys, List<String> args) {
        // 创建 Redis 脚本对象
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(String.class);

        // 执行脚本
        return stringRedisTemplate.execute(redisScript, keys, args.toArray(new String[0]));
    }

    /**
     * 执行带键和参数的 Lua 脚本
     * @param script Lua 脚本
     * @param key 操作的键
     * @param value 要设置的值
     * @return 操作结果
     */
    public String executeWithKeyAndValue(String script, String key, String value) {
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(String.class);

        return stringRedisTemplate.execute(
            redisScript,
            Collections.singletonList(key),
            value
        );
    }

    /**
     * 执行库存扣减脚本
     * @param stockKey 库存键
     * @param quantity 扣减数量
     * @return 扣减后的库存，-1 表示库存不足
     */
    public Long decrementStock(String stockKey, int quantity) {
        // Lua 脚本：原子性扣减库存
        String script = """
            local stock = redis.call('GET', KEYS[1])
            if stock == false then
                return -1
            end
            stock = tonumber(stock)
            local quantity = tonumber(ARGV[1])
            if stock < quantity then
                return -2  -- 库存不足
            end
            stock = stock - quantity
            redis.call('SET', KEYS[1], stock)
            return stock
            """;

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);

        Long result = stringRedisTemplate.execute(
            redisScript,
            Collections.singletonList(stockKey),
            String.valueOf(quantity)
        );

        log.info("库存扣减结果: 键={}, 数量={}, 结果={}", stockKey, quantity, result);
        return result;
    }
}
```

#### 方式二：RedisTemplate

```java
package com.example.redis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 使用 RedisTemplate 执行 Lua 脚本
 * 适用于需要处理复杂对象类型的场景
 */
@Service
@RequiredArgsConstructor
public class RedisTemplateLuaService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 执行脚本并获取列表类型的结果
     */
    public List<Object> executeGetList(String script, List<String> keys, List<Object> args) {
        DefaultRedisScript<List> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(List.class);

        return redisTemplate.execute(redisScript, keys, args.toArray());
    }

    /**
     * 执行脚本并获取数字类型的结果
     */
    public Long executeGetLong(String script, List<String> keys, Object... args) {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);

        return redisTemplate.execute(redisScript, keys, args);
    }
}
```

#### 方式三：脚本加载器（生产环境推荐）

```java
package com.example.redis.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Lua 脚本加载器
 * 在应用启动时预加载所有脚本到 Redis
 * 避免每次执行时都要发送脚本内容
 */
@Component
@RequiredArgsConstructor
public class LuaScriptLoader {

    private final ApplicationContext applicationContext;

    // 存储已编译的脚本实例
    private final java.util.Map<String, DefaultRedisScript<?>> scriptCache = new java.util.concurrent.ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 预加载所有脚本
        loadScript("classpath:lua/decrement_stock.lua");
        loadScript("classpath:lua/rate_limit.lua");
        System.out.println("Lua 脚本预加载完成");
    }

    /**
     * 加载脚本文件
     */
    private void loadScript(String path) {
        try {
            Resource resource = applicationContext.getResource(path);
            String content = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
            ).lines().collect(Collectors.joining("\n"));

            String scriptName = path.substring(path.lastIndexOf('/') + 1);
            scriptCache.put(scriptName, createScript(content));

            System.out.println("已加载脚本: " + scriptName);
        } catch (Exception e) {
            System.out.println("加载脚本失败: " + path + ", 错误: " + e.getMessage());
        }
    }

    /**
     * 创建 Redis 脚本对象
     */
    private DefaultRedisScript<?> createScript(String scriptText) {
        DefaultRedisScript<?> script = new DefaultRedisScript<>();
        script.setScriptText(scriptText);
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 获取已缓存的脚本
     */
    public DefaultRedisScript<?> getScript(String name) {
        return scriptCache.get(name);
    }
}
```

---

## 3. 实战案例一：库存扣减原子操作

### 3.1 业务场景

电商系统中，库存扣减是一个典型的高并发场景。如果不使用原子操作，可能出现：

- **超卖问题**：多个请求同时读到库存为 10，都扣减导致库存变成负数
- **数据不一致**：扣减成功后，更新库存失败

### 3.2 库存扣减 Lua 脚本

```lua
-- decrement_stock.lua
-- 库存扣减原子操作脚本
-- KEYS[1]: 库存键
-- ARGV[1]: 要扣减的数量

-- 获取当前库存
local stock = redis.call('GET', KEYS[1])

-- 检查库存是否存在
if stock == false then
    -- 库存记录不存在，返回错误码 -1
    return -1
end

-- 转换为数字
stock = tonumber(stock)
local quantity = tonumber(ARGV[1])

-- 检查库存是否充足
if stock < quantity then
    -- 库存不足，返回错误码 -2
    return -2
end

-- 执行扣减操作
local newStock = stock - quantity
redis.call('SET', KEYS[1], newStock)

-- 返回扣减后的库存数量
return newStock
```

### 3.3 Java 实现

```java
package com.example.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 库存扣减服务
 * 使用 Lua 脚本保证原子性
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 扣减库存
     *
     * @param productId 商品ID
     * @param quantity  扣减数量
     * @return 扣减后的库存，负数表示失败
     */
    public long decrementStock(Long productId, int quantity) {
        String stockKey = "stock:product:" + productId;

        String script = """
            local stock = redis.call('GET', KEYS[1])
            if stock == false then
                return -1
            end
            stock = tonumber(stock)
            local quantity = tonumber(ARGV[1])
            if stock < quantity then
                return -2
            end
            local newStock = stock - quantity
            redis.call('SET', KEYS[1], newStock)
            return newStock
            """;

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);

        Long result = stringRedisTemplate.execute(
            redisScript,
            Collections.singletonList(stockKey),
            String.valueOf(quantity)
        );

        log.info("库存扣减 - 商品: {}, 数量: {}, 剩余: {}", productId, quantity, result);
        return result != null ? result : -1;
    }

    /**
     * 初始化库存
     */
    public void initStock(Long productId, long stock) {
        String stockKey = "stock:product:" + productId;
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
        log.info("初始化库存 - 商品: {}, 库存: {}", productId, stock);
    }

    /**
     * 查询当前库存
     */
    public Long getStock(Long productId) {
        String stockKey = "stock:product:" + productId;
        String value = stringRedisTemplate.opsForValue().get(stockKey);
        return value != null ? Long.parseLong(value) : null;
    }
}
```

### 3.4 单元测试

```java
package com.example.redis.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 库存扣减服务测试
 */
@SpringBootTest
@Slf4j
class StockServiceTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final Long PRODUCT_ID = 1001L;

    @BeforeEach
    void setUp() {
        // 初始化库存为 100
        stockService.initStock(PRODUCT_ID, 100);
    }

    @Test
    @DisplayName("正常扣减库存")
    void testDecrementStock() {
        long result = stockService.decrementStock(PRODUCT_ID, 10);
        assertEquals(90, result);
        assertEquals(90, stockService.getStock(PRODUCT_ID));
    }

    @Test
    @DisplayName("库存不足时返回错误码")
    void testDecrementStockInsufficient() {
        long result = stockService.decrementStock(PRODUCT_ID, 150);
        assertEquals(-2, result);  // 库存不足
        assertEquals(100, stockService.getStock(PRODUCT_ID));  // 库存未变化
    }

    @Test
    @DisplayName("库存记录不存在")
    void testDecrementStockNotExists() {
        long result = stockService.decrementStock(9999L, 10);
        assertEquals(-1, result);  // 记录不存在
    }

    @Test
    @DisplayName("并发扣减库存测试")
    void testConcurrentDecrement() throws InterruptedException {
        int threadCount = 20;
        int decrementsPerThread = 10;
        int totalDecrement = threadCount * decrementsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    long result = stockService.decrementStock(PRODUCT_ID, decrementsPerThread);
                    if (result >= 0) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        Long finalStock = stockService.getStock(PRODUCT_ID);
        log.info("并发测试结果 - 成功: {}, 失败: {}, 最终库存: {}",
            successCount.get(), failCount.get(), finalStock);

        // 验证：初始100，每次扣减10，20个线程总计扣减200
        // 实际只能扣减100，所以最终库存应该为0
        assertEquals(0, finalStock);
        assertEquals(10, successCount.get());  // 只有10个线程成功
        assertEquals(10, failCount.get());    // 10个线程失败
    }
}
```

---

## 4. 实战案例二：扣减失败自动回滚

### 4.1 业务场景

分布式事务中，如果库存扣减成功，但后续操作（如订单创建）失败了，需要自动回滚库存。

### 4.2 方案设计

使用 Redis 的 `WATCH` + Lua 脚本实现乐观锁：

```
1. 开启事务 WATCH 库存键
2. 执行 Lua 脚本扣减库存
3. 如果返回失败，放弃事务
4. 如果返回成功，执行业务操作
5. 如果业务操作失败，执行 Lua 脚本回滚库存
```

### 4.3 回滚脚本

```lua
-- rollback_stock.lua
-- 库存回滚脚本
-- KEYS[1]: 库存键
-- ARGV[1]: 要回滚的数量

local stock = redis.call('GET', KEYS[1])
if stock == false then
    return -1
end

stock = tonumber(stock)
local quantity = tonumber(ARGV[1])
local newStock = stock + quantity

redis.call('SET', KEYS[1], newStock)
return newStock
```

### 4.4 Java 实现

```java
package com.example.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;

/**
 * 库存扣减与回滚服务
 * 支持失败自动回滚的分布式事务场景
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockWithRollbackService {

    private final StringRedisTemplate stringRedisTemplate;

    // 扣减库存脚本
    private static final String DECREMENT_SCRIPT = """
        local stock = redis.call('GET', KEYS[1])
        if stock == false then
            return -1
        end
        stock = tonumber(stock)
        local quantity = tonumber(ARGV[1])
        if stock < quantity then
            return -2
        end
        local newStock = stock - quantity
        redis.call('SET', KEYS[1], newStock)
        return newStock
        """;

    // 回滚库存脚本
    private static final String ROLLBACK_SCRIPT = """
        local stock = redis.call('GET', KEYS[1])
        if stock == false then
            return -1
        end
        stock = tonumber(stock)
        local quantity = tonumber(ARGV[1])
        local newStock = stock + quantity
        redis.call('SET', KEYS[1], newStock)
        return newStock
        """;

    /**
     * 扣减库存结果
     */
    public record DecrementResult(
        boolean success,      // 是否成功
        long stock,           // 扣减后的库存（或错误码）
        String message        // 消息
    ) {
        public static DecrementResult ok(long stock) {
            return new DecrementResult(true, stock, "扣减成功");
        }

        public static DecrementResult fail(long errorCode, String message) {
            return new DecrementResult(false, errorCode, message);
        }
    }

    /**
     * 扣减库存
     */
    public DecrementResult decrementStock(Long productId, int quantity) {
        String stockKey = "stock:product:" + productId;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(DECREMENT_SCRIPT);
        script.setResultType(Long.class);

        Long result = stringRedisTemplate.execute(
            script,
            Collections.singletonList(stockKey),
            String.valueOf(quantity)
        );

        if (result == null || result == -1) {
            return DecrementResult.fail(-1, "库存记录不存在");
        }
        if (result == -2) {
            return DecrementResult.fail(-2, "库存不足");
        }

        log.info("库存扣减成功 - 商品: {}, 扣减: {}, 剩余: {}", productId, quantity, result);
        return DecrementResult.ok(result);
    }

    /**
     * 回滚库存
     */
    public boolean rollbackStock(Long productId, int quantity) {
        String stockKey = "stock:product:" + productId;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(ROLLBACK_SCRIPT);
        script.setResultType(Long.class);

        Long result = stringRedisTemplate.execute(
            script,
            Collections.singletonList(stockKey),
            String.valueOf(quantity)
        );

        if (result == null || result == -1) {
            log.error("库存回滚失败 - 商品: {}, 数量: {}, 原因: 库存记录不存在",
                productId, quantity);
            return false;
        }

        log.info("库存回滚成功 - 商品: {}, 数量: {}, 回滚后: {}", productId, quantity, result);
        return true;
    }

    /**
     * 模拟创建订单业务
     */
    public boolean createOrder(Long productId, int quantity) {
        // 模拟业务逻辑，可能成功也可能失败
        // 这里简化为 80% 成功率
        boolean success = Math.random() > 0.2;
        if (success) {
            log.info("订单创建成功 - 商品: {}, 数量: {}", productId, quantity);
        } else {
            log.warn("订单创建失败 - 商品: {}, 数量: {}", productId, quantity);
        }
        return success;
    }

    /**
     * 执行下单流程（扣减库存 + 创建订单）
     * 如果订单创建失败，自动回滚库存
     */
    public boolean placeOrder(Long productId, int quantity) {
        // 1. 扣减库存
        DecrementResult decrementResult = decrementStock(productId, quantity);
        if (!decrementResult.success()) {
            log.warn("下单失败 - 库存扣减失败: {}", decrementResult.message());
            return false;
        }

        try {
            // 2. 创建订单
            boolean orderCreated = createOrder(productId, quantity);
            if (!orderCreated) {
                // 3. 订单创建失败，回滚库存
                log.warn("订单创建失败，开始回滚库存");
                rollbackStock(productId, quantity);
                return false;
            }
            return true;
        } catch (Exception e) {
            // 4. 异常情况也回滚
            log.error("下单过程异常: {}", e.getMessage());
            rollbackStock(productId, quantity);
            throw e;
        }
    }
}
```

### 4.5 使用乐观锁的事务版本

```java
package com.example.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 使用事务 + Watch 实现乐观锁的库存扣减
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OptimisticLockStockService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 乐观锁扣减库存
     * 如果库存在此期间被其他请求修改，则重试
     */
    public long decrementStockWithRetry(Long productId, int quantity, int maxRetries) {
        String stockKey = "stock:product:" + productId;

        for (int i = 0; i < maxRetries; i++) {
            try {
                Long result = stringRedisTemplate.execute(new SessionCallback<Long>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public Long execute(RedisOperations operations) throws DataAccessException {
                        // 监听库存键
                        operations.watch(operations.opsForValue().getOperations().getConnectionFactory()
                            .getConnection(), stockKey);

                        // 获取当前库存
                        String stockStr = (String) operations.opsForValue().get(stockKey);
                        if (stockStr == null) {
                            operations.unwatch();
                            return -1L;
                        }

                        long stock = Long.parseLong(stockStr);
                        if (stock < quantity) {
                            operations.unwatch();
                            return -2L;
                        }

                        // 开启事务
                        operations.multi();
                        operations.opsForValue().set(stockKey, String.valueOf(stock - quantity));

                        // 执行事务
                        List<Object> results = operations.exec();
                        if (results == null || results.isEmpty()) {
                            return -3L;  // 事务被中断，说明库存被修改
                        }
                        return (Long) results.get(0);
                    }
                });

                return result != null ? result : -1L;
            } catch (Exception e) {
                log.warn("乐观锁冲突，重试第 {} 次: {}", i + 1, e.getMessage());
            }
        }

        log.error("乐观锁重试次数用尽，扣减失败");
        return -3L;
    }
}
```

---

## 5. 实战案例三：访问频率限制

### 5.1 业务场景

接口防刷、API 限流、恶意访问拦截等场景。

### 5.2 限流算法对比

| 算法 | 优点 | 缺点 |
|------|------|------|
| **计数器** | 简单易实现 | 存在临界问题 |
| **滑动窗口** | 精度高 | 实现复杂 |
| **令牌桶** | 允许突发流量 | 需要额外存储 |
| **漏桶** | 流量平滑 | 不适合突发 |

### 5.3 固定窗口限流 Lua 脚本

```lua
-- rate_limit_fixed.lua
-- 固定窗口限流算法
-- KEYS[1]: 限流键
-- ARGV[1]: 窗口大小（秒）
-- ARGV[2]: 最大请求数

local key = KEYS[1]
local window = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])

-- 获取当前计数
local current = redis.call('GET', key)
if current == false then
    -- 首次访问，设置计数为 1，并设置过期时间
    redis.call('SET', key, 1)
    redis.call('EXPIRE', key, window)
    return 1  -- 允许访问
end

current = tonumber(current)
if current >= limit then
    -- 超过限制
    return 0
end

-- 计数 +1
redis.call('INCR', key)
return 1  -- 允许访问
```

### 5.4 滑动窗口限流 Lua 脚本

```lua
-- rate_limit_sliding.lua
-- 滑动窗口限流算法
-- KEYS[1]: 限流键
-- ARGV[1]: 窗口大小（毫秒）
-- ARGV[2]: 最大请求数

local key = KEYS[1]
local window = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

-- 删除窗口外的记录
local windowStart = now - window
redis.call('ZREMRANGEBYSCORE', key, '-inf', windowStart)

-- 统计当前窗口内的请求数
local count = redis.call('ZCARD', key)

if count >= limit then
    return 0  -- 超过限制
end

-- 记录本次请求
redis.call('ZADD', key, now, now .. '-' .. math.random())
redis.call('PEXPIRE', key, window)

return 1  -- 允许访问
```

### 5.5 Java 实现

```java
package com.example.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;

/**
 * 访问频率限制服务
 * 支持固定窗口和滑动窗口两种算法
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final StringRedisTemplate stringRedisTemplate;

    // 固定窗口限流脚本
    private static final String FIXED_WINDOW_SCRIPT = """
        local key = KEYS[1]
        local window = tonumber(ARGV[1])
        local limit = tonumber(ARGV[2])

        local current = redis.call('GET', key)
        if current == false then
            redis.call('SET', key, 1)
            redis.call('EXPIRE', key, window)
            return 1
        end

        current = tonumber(current)
        if current >= limit then
            return 0
        end

        redis.call('INCR', key)
        return 1
        """;

    // 滑动窗口限流脚本
    private static final String SLIDING_WINDOW_SCRIPT = """
        local key = KEYS[1]
        local window = tonumber(ARGV[1])
        local limit = tonumber(ARGV[2])
        local now = tonumber(ARGV[3])

        local windowStart = now - window
        redis.call('ZREMRANGEBYSCORE', key, '-inf', windowStart)

        local count = redis.call('ZCARD', key)

        if count >= limit then
            return 0
        end

        redis.call('ZADD', key, now, now .. '-' .. math.random())
        redis.call('PEXPIRE', key, window)

        return 1
        """;

    /**
     * 限流结果
     */
    public record RateLimitResult(
        boolean allowed,    // 是否允许访问
        long remaining,     // 剩余请求数
        long resetTime      // 重置时间（Unix 时间戳）
    ) {
        public static RateLimitResult allow(long remaining, long resetTime) {
            return new RateLimitResult(true, remaining, resetTime);
        }

        public static RateLimitResult deny() {
            return new RateLimitResult(false, 0, 0);
        }
    }

    /**
     * 固定窗口限流
     *
     * @param key        限流键（如用户ID、IP地址）
     * @param windowSec  窗口大小（秒）
     * @param maxRequest 窗口内最大请求数
     * @return 限流结果
     */
    public RateLimitResult fixedWindowLimit(String key, int windowSec, int maxRequest) {
        String redisKey = "ratelimit:fixed:" + key;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(FIXED_WINDOW_SCRIPT);
        script.setResultType(Long.class);

        Long result = stringRedisTemplate.execute(
            script,
            Collections.singletonList(redisKey),
            String.valueOf(windowSec),
            String.valueOf(maxRequest)
        );

        boolean allowed = result != null && result == 1L;
        long ttl = stringRedisTemplate.getExpire(redisKey);
        long resetTime = System.currentTimeMillis() / 1000 + (ttl > 0 ? ttl : 0);

        // 计算剩余请求数
        String currentStr = stringRedisTemplate.opsForValue().get(redisKey);
        long remaining = currentStr != null ? maxRequest - Long.parseLong(currentStr) : maxRequest;

        if (allowed) {
            log.debug("固定窗口限流 - 键: {}, 剩余: {}", key, remaining);
            return RateLimitResult.allow(remaining, resetTime);
        } else {
            log.warn("固定窗口限流 - 键: {}, 已达到上限", key);
            return RateLimitResult.deny();
        }
    }

    /**
     * 滑动窗口限流
     *
     * @param key         限流键
     * @param windowMs    窗口大小（毫秒）
     * @param maxRequest  窗口内最大请求数
     * @return 限流结果
     */
    public RateLimitResult slidingWindowLimit(String key, long windowMs, int maxRequest) {
        String redisKey = "ratelimit:sliding:" + key;
        long now = System.currentTimeMillis();

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(SLIDING_WINDOW_SCRIPT);
        script.setResultType(Long.class);

        Long result = stringRedisTemplate.execute(
            script,
            Collections.singletonList(redisKey),
            String.valueOf(windowMs),
            String.valueOf(maxRequest),
            String.valueOf(now)
        );

        boolean allowed = result != null && result == 1L;
        long resetTime = (now + windowMs) / 1000;

        if (allowed) {
            log.debug("滑动窗口限流 - 键: {}, 窗口: {}ms, 上限: {}", key, windowMs, maxRequest);
            return RateLimitResult.allow(maxRequest - 1, resetTime);
        } else {
            log.warn("滑动窗口限流 - 键: {}, 已达到上限", key);
            return RateLimitResult.deny();
        }
    }

    /**
     * 注解式限流（配合 AOP 使用）
     */
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD})
    @interface RateLimit {
        /** 限流键，支持 SpEL 表达式 */
        String key() default "";

        /** 窗口大小（秒）*/
        int window() default 60;

        /** 最大请求数 */
        int maxRequest() default 100;

        /** 限流算法 */
        Algorithm algorithm() default Algorithm.FIXED_WINDOW;

        enum Algorithm {
            FIXED_WINDOW,
            SLIDING_WINDOW
        }
    }
}
```

### 5.6 限流拦截器实现

```java
package com.example.redis.aspect;

import com.example.redis.service.RateLimitService;
import com.example.redis.service.RateLimitService.RateLimitResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 限流切面
 * 结合 @RateLimit 注解实现方法级别的限流
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitAspect {

    private final RateLimitService rateLimitService;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimitService.RateLimit rateLimit) throws Throwable {
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();

        // 构建限流键
        String key = buildKey(rateLimit.key(), request, joinPoint);

        // 根据算法类型执行限流
        RateLimitResult result;
        if (rateLimit.algorithm() == RateLimitService.RateLimit.Algorithm.SLIDING_WINDOW) {
            result = rateLimitService.slidingWindowLimit(
                key,
                rateLimit.window() * 1000L,
                rateLimit.maxRequest()
            );
        } else {
            result = rateLimitService.fixedWindowLimit(key, rateLimit.window(), rateLimit.maxRequest());
        }

        // 设置响应头
        if (request instanceof org.springframework.http.server.reactive.ServerHttpRequest serverRequest) {
            // WebFlux 环境
        } else {
            // Servlet 环境
            attributes.getResponse().setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));
            attributes.getResponse().setHeader("X-RateLimit-Reset", String.valueOf(result.resetTime()));
        }

        // 检查是否被限流
        if (!result.allowed()) {
            log.warn("请求被限流 - 键: {}, 方法: {}", key, joinPoint.getSignature().getName());
            throw new RuntimeException("请求过于频繁，请稍后重试");
        }

        return joinPoint.proceed();
    }

    /**
     * 构建限流键
     */
    private String buildKey(String keyExpression, HttpServletRequest request, ProceedingJoinPoint joinPoint) {
        if (keyExpression.isEmpty()) {
            // 默认使用 IP + 方法名
            return request.getRemoteAddr() + ":" + joinPoint.getSignature().getName();
        }

        // 支持简单的占位符
        String key = keyExpression
            .replace("{ip}", request.getRemoteAddr())
            .replace("{uri}", request.getRequestURI())
            .replace("{userId}", getUserId(request));

        return key;
    }

    /**
     * 获取用户ID（从请求头或参数中）
     */
    private String getUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null) {
            userId = request.getParameter("userId");
        }
        if (userId == null) {
            userId = "anonymous";
        }
        return userId;
    }
}
```

### 5.7 使用示例

```java
package com.example.redis.controller;

import com.example.redis.service.RateLimitService;
import com.example.redis.service.RateLimitService.RateLimit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API 控制器（限流示例）
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final RateLimitService rateLimitService;

    /**
     * 固定窗口限流示例
     * 限制每个IP每分钟最多访问100次
     */
    @GetMapping("/fixed-window")
    @RateLimit(key = "{ip}", window = 60, maxRequest = 100)
    public ResponseEntity<String> fixedWindow() {
        return ResponseEntity.ok("固定窗口限流 - 请求成功");
    }

    /**
     * 滑动窗口限流示例
     * 限制每个用户每分钟最多访问60次
     */
    @GetMapping("/sliding-window")
    @RateLimit(key = "{userId}", window = 60, maxRequest = 60, algorithm = RateLimit.Algorithm.SLIDING_WINDOW)
    public ResponseEntity<String> slidingWindow() {
        return ResponseEntity.ok("滑动窗口限流 - 请求成功");
    }

    /**
     * 手动限流示例
     */
    @PostMapping("/submit")
    public ResponseEntity<String> submit(@RequestParam String userId) {
        // 手动调用限流服务
        RateLimitResult result = rateLimitService.fixedWindowLimit(
            "submit:" + userId,
            60,
            10
        );

        if (!result.allowed()) {
            return ResponseEntity.status(429)
                .header("X-RateLimit-Remaining", String.valueOf(result.remaining()))
                .header("X-RateLimit-Reset", String.valueOf(result.resetTime()))
                .body("请求过于频繁");
        }

        return ResponseEntity.ok("提交成功");
    }
}
```

---

## 6. Lua 脚本调试技巧

### 6.1 本地调试方法

#### 1. 使用 redis-cli 直接测试

```bash
# 连接 Redis
redis-cli

# 执行 Lua 脚本测试
EVAL "return redis.call('GET', KEYS[1])" 1 stock:product:1001

# 使用外部脚本文件
SCRIPT LOAD "$(cat decrement_stock.lua)"
# 返回: "script_sha1_hash"

# 执行已加载的脚本
EVALSHA "script_sha1_hash" 1 stock:product:1001 10

# 查看脚本缓存
SCRIPT LIST

# 刷新所有脚本
SCRIPT FLUSH
```

#### 2. 使用 Redis Insight / Another Redis Desktop Manager

这些 GUI 工具提供：
- Lua 脚本编辑和语法高亮
- 脚本执行和结果查看
- 历史记录
- 断点调试支持

### 6.2 Spring Boot 中的调试技巧

#### 1. 开启 Redis 日志

```yaml
# application.yml
logging:
  level:
    org.springframework.data.redis: DEBUG
    io.lettuce.core: DEBUG
```

#### 2. 添加执行日志

```java
/**
 * 带调试日志的 Lua 脚本执行
 */
public Long debugExecute(String script, List<String> keys, List<String> args) {
    // 打印脚本内容
    log.debug("执行 Lua 脚本:\n{}", script);
    log.debug("Keys: {}", keys);
    log.debug("Args: {}", args);

    long startTime = System.currentTimeMillis();

    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
    redisScript.setScriptText(script);
    redisScript.setResultType(Long.class);

    Long result = stringRedisTemplate.execute(redisScript, keys, args.toArray(new String[0]));

    long cost = System.currentTimeMillis() - startTime;
    log.debug("脚本执行完成 - 结果: {}, 耗时: {}ms", result, cost);

    return result;
}
```

#### 3. 脚本异常捕获

```java
/**
 * 带异常处理的脚本执行
 */
public Result executeWithErrorHandling(String script, List<String> keys, List<String> args) {
    try {
        Long result = debugExecute(script, keys, args);
        return Result.success(result);
    } catch (Exception e) {
        log.error("Lua 脚本执行失败 - 错误: {}", e.getMessage(), e);
        return Result.failure(e.getMessage());
    }
}

/**
 * 执行结果
 */
public record Result(boolean success, Long data, String error) {
    public static Result success(Long data) {
        return new Result(true, data, null);
    }

    public static Result failure(String error) {
        return new Result(false, null, error);
    }
}
```

### 6.3 生产环境调试

#### 1. SCRIPT EXISTS 检查脚本是否加载

```java
/**
 * 检查脚本是否已加载
 */
public boolean isScriptLoaded(String scriptSha) {
    List<Object> result = stringRedisTemplate.execute(
        new DefaultRedisScript<List>() {{
            setScriptText("return redis.call('SCRIPT', 'EXISTS', ARGV[1])");
            setResultType(List.class);
        }},
        Collections.emptyList(),
        scriptSha
    );
    return result != null && !result.isEmpty() && (Long) result.get(0) == 1L;
}
```

#### 2. 获取脚本 SHA 用于缓存执行

```java
/**
 * 获取脚本 SHA
 */
public String getScriptSha(String script) {
    return stringRedisTemplate.execute(
        new DefaultRedisScript<String>() {{
            setScriptText("return redis.call('SCRIPT', 'LOAD', ARGV[1])");
            setResultType(String.class);
        }},
        Collections.emptyList(),
        script
    );
}

/**
 * 使用 SHA 执行脚本（如果脚本已加载）
 */
public Long executeBySha(String sha, List<String> keys, List<String> args) {
    // 先尝试使用 SHA 执行
    String script = """
        local ok = pcall(function()
            return redis.call('EVALSHA', KEYS[1], #KEYS, unpack(KEYS, 2), unpack(ARGV))
        end)
        if not ok then
            return redis.error_reply('NOSCRIPT')
        end
        """;

    // 如果 NOSCRIPT，重新加载并执行
    // 这里需要业务层处理重试逻辑
    return stringRedisTemplate.execute(
        new DefaultRedisScript<>(script, Long.class),
        keys,
        args.toArray(new String[0])
    );
}
```

#### 3. MONITOR 命令实时监控

```bash
# 启动 Redis 监控
redis-cli MONITOR

# 过滤 Lua 脚本相关操作
redis-cli MONITOR | grep EVAL
```

### 6.4 常见错误排查

| 错误 | 原因 | 解决方案 |
|------|------|----------|
| `NOSCRIPT No matching script` | 脚本未加载 | 使用 `SCRIPT LOAD` 预加载 |
| `ERR Error compiling script` | Lua 语法错误 | 检查脚本语法 |
| `ERR not an integer` | 参数类型错误 | 确保 KEYS/ARGV 格式正确 |
| `WRONGTYPE Operation against` | 键类型不匹配 | 检查键是否存在其他类型数据 |
| `BUSY Redis is busy` | 脚本运行超时 | 优化脚本或增加 TIMEOUT |

### 6.5 脚本性能优化

```lua
-- 优化前：多次调用 redis.call
local stock = redis.call('GET', KEYS[1])
if stock == false then
    return -1
end
if tonumber(stock) < tonumber(ARGV[1]) then
    return -2
end
redis.call('SET', KEYS[1], tonumber(stock) - tonumber(ARGV[1]))

-- 优化后：减少 redis.call 调用次数
local stock = redis.call('GET', KEYS[1])
if stock == false then
    return -1
end
stock = tonumber(stock)
local quantity = tonumber(ARGV[1])
if stock < quantity then
    return -2
end
redis.call('SET', KEYS[1], stock - quantity)
```

---

## 总结

### 核心要点

1. **原子性保证**：Lua 脚本是 Redis 原子性的最佳实践，适用于复杂业务场景
2. **脚本加载**：生产环境推荐预加载脚本，使用 SHA 执行减少网络传输
3. **错误处理**：Java 层要处理脚本执行的各种异常情况
4. **监控调试**：利用 Redis 日志和 MONITOR 命令排查问题

### 脚本模板

```java
// 标准 Lua 脚本执行模板
DefaultRedisScript<Long> script = new DefaultRedisScript<>();
script.setScriptText("""
    -- Lua 脚本内容
    local stock = redis.call('GET', KEYS[1])
    if stock == false then
        return -1
    end
    return tonumber(stock)
    """);
script.setResultType(Long.class);

Long result = stringRedisTemplate.execute(
    script,
    Collections.singletonList(key),
    arg1, arg2
);
```

### 相关配置

| 配置项 | 推荐值 | 说明 |
|--------|--------|------|
| `lua-timeout` | 5000ms | 脚本最大执行时间 |
| `spring.data.redis.timeout` | 10s | Redis 操作超时 |
| 连接池大小 | CPU 核心数 * 2 | 根据并发量调整 |

---

**参考资料**

- [Redis Lua 脚本官方文档](https://redis.io/docs/manual/programmability/lua-api/)
- [Spring Data Redis 文档](https://spring.io/projects/spring-data-redis)
