# 第六章：Lua 脚本

## 6.1 Lua 脚本简介

### 6.1.1 Lua 语言特点

Lua 是一种轻量级、可嵌入的脚本语言，由巴西里约热内卢天主教大学于 1993 年设计开发。其设计目标是为应用程序提供灵活的扩展和定制能力。

**Lua 的核心特点：**

| 特点 | 说明 |
|------|------|
| **轻量级** | 核心引擎仅约 200KB，运行时占用资源极少 |
| **可嵌入** | 易于集成到 C/C++、Java、Python 等宿主程序中 |
| **速度快** | 执行效率高，编译后字节码体积小 |
| **简洁易学** | 语法简单，API 友好，学习成本低 |
| **动态类型** | 变量无需声明类型，使用灵活 |

Lua 语言在游戏开发（如 World of Warcraft 插件）、嵌入式系统、Web 应用等领域广泛应用。Redis 从 2.6 版本开始引入 Lua 脚本支持，极大地增强了 Redis 的编程能力。

### 6.1.2 Redis 为什么选择 Lua

Redis 选择 Lua 作为脚本扩展语言，主要基于以下原因：

**1. 原子性保证**

Redis 采用单线程模型，而 Lua 脚本在执行时同样具有原子性。当一个 Lua 脚本正在执行时，不会被其他命令打断，也不会被其他 Lua 脚本打断。这确保了脚本执行期间所有操作的原子性。

```sh
# Lua 脚本执行期间，整个 Redis 实例串行化处理
# 不存在竞态条件，不需要额外加锁
```

**2. 可嵌入性**

Lua 可以很方便地嵌入到 Redis 服务端进程中，直接调用 Redis 的内部函数，访问 Redis 的数据结构。

**3. 执行效率**

Lua 脚本在执行前会被编译成字节码，相比逐条发送命令，执行效率更高。同时可以减少网络往返次数。

**4. 灵活性**

Lua 是一门成熟的脚本语言，提供了丰富的语法和库支持，可以实现复杂的业务逻辑。

### 6.1.3 Redis Lua 环境与限制

**环境限制：**

- Redis 使用的是 Lua 5.1 版本
- 虚拟机关闭了标准的 I/O 库（print 需要使用 redis.log）
- 禁用了一些可能影响安全或性能的函数（如 loadfile、dofile）
- 不支持 Lua 的多线程和协程特性

**可用库：**

- **redis** : Redis 命令接口
- **table** : 表操作库
- **string** : 字符串操作库
- **math** : 数学库
- **pairs** / **ipairs** : 迭代器

**禁止使用：**

- loadfile()、dofile() - 文件操作
- os.* - 操作系统调用
- io.* - I/O 操作
- debug.* - 调试功能
- require - 模块加载

```sh
# 查看 Redis Lua 脚本相关配置
redis-cli CONFIG GET lua*
```

---

## 6.2 EVAL 命令详解

### 6.2.1 EVAL 命令格式

EVAL 是执行 Lua 脚本的基本命令：

```sh
EVAL script numkeys key [key ...] arg [arg ...]
```

**参数说明：**

| 参数 | 说明 |
|------|------|
| script | Lua 脚本内容 |
| numkeys | 键的数量（后续 key 参数的个数） |
| key [key ...] | 访问的 Redis 键列表 |
| arg [arg ...] | 额外的参数列表 |

**返回值：** 命令返回脚本中最后一个语句的返回值

### 6.2.2 numkeys 参数说明

numkeys 指定后续有多少个 key 参数，这些 key 会被传递给 Lua 脚本的 KEYS 数组。

```sh
# 示例：numkeys = 1，表示后续有 1 个 key
EVAL "return redis.call('GET', KEYS[1])" 1 mykey

# 示例：numkeys = 2，表示后续有 2 个 key
EVAL "return redis.call('MGET', KEYS[1], KEYS[2])" 2 key1 key2
```

### 6.2.3 键（key）与参数（arg）的区别

**KEYS 数组：** 存放 Redis 键名，用于访问 Redis 数据

**ARGV 数组：** 存放额外的参数值，不一定是键名

```sh
# 语法示例
EVAL script numkeys key [key ...] arg [arg ...]

# 实际示例：设置带过期时间的键值
# KEYS[1] = user:1001 (键名)
# ARGV[1] = "Tom" (值)
# ARGV[2] = "3600" (过期时间，秒)
EVAL "
    redis.call('SET', KEYS[1], ARGV[1])
    redis.call('EXPIRE', KEYS[1], ARGV[2])
    return 'OK'
" 1 user:1001 Tom 3600
```

**区别总结：**

| 特性 | KEYS | ARGV |
|------|------|------|
| 用途 | Redis 键名 | 自定义参数 |
| 用途场景 | 需要操作的键 | 业务逻辑参数 |
| 集群路由 | 参与 CRC16 槽计算 | 不参与 |
| 数量限制 | 受 redis.conf 配置 | 无限制 |

### 6.2.4 EVALSHA 命令

EVALSHA 基于脚本 SHA1 校验和执行脚本，避免每次传递完整脚本内容，减少网络开销。

```sh
# 先加载脚本，获取 SHA1 校验和
SCRIPT LOAD "return redis.call('GET', KEYS[1])"

# 返回结果示例
# "a420880db46f9d09e2d0c5e9f1f0e3d6c8b7a123"
```

```sh
# 使用 EVALSHA 执行（通过校验和）
EVALSHA a420880db46f9d09e2d0c5e9f1f0e3d6c8b7a123 1 mykey
```

**使用场景：**

- 脚本较长时，减少网络传输
- 预加载常用脚本，提高执行效率
- 生产环境推荐使用 EVALSHA

### 6.2.5 SCRIPT LOAD 与 SCRIPT EXISTS

**SCRIPT LOAD：** 将脚本加载到脚本缓存，返回 SHA1 校验和

```sh
SCRIPT LOAD "return 'Hello, Redis!'"
# 返回: "0c9d2c3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c"
```

**SCRIPT EXISTS：** 检查脚本是否存在于缓存中

```sh
# 检查单个脚本
SCRIPT EXISTS a420880db46f9d09e2d0c5e9f1f0e3d6c8b7a123

# 检查多个脚本
SCRIPT EXISTS a420880db46f9d09e2d0c5e9f1f0e3d6c8b7a123 b520880db46f9d09e2d0c5e9f1f0e3d6c8b7a123
```

**返回值说明：**

- 1 表示脚本已缓存
- 0 表示脚本未缓存

---

## 6.3 Lua 脚本语法基础

### 6.3.1 变量与数据类型

**变量类型：**

```lua
-- Lua 变量无需声明类型
local name = "Redis"           -- 字符串
local count = 100              -- 数字
local isActive = true          -- 布尔值
local data = {1, 2, 3}         -- 表（数组）
local map = {name = "Tom"}     -- 表（哈希）
local nilValue = nil           -- nil 空值

-- 全局变量（不推荐使用）
globalVar = "I am global"
```

**数据类型：**

| 类型 | 说明 | 示例 |
|------|------|------|
| nil | 空值 | nil |
| boolean | 布尔值 | true, false |
| number | 数值 | 100, 3.14 |
| string | 字符串 | "hello" |
| table | 表 | {1,2,3}, {a=1} |
| function | 函数 | function() end |

### 6.3.2 运算符

**算术运算符：**

```lua
local a, b = 10, 3

local add = a + b       -- 加法: 13
local sub = a - b       -- 减法: 7
local mul = a * b       -- 乘法: 30
local div = a / b       -- 除法: 3.333...
local mod = a % b       -- 取余: 1
local pow = a ^ b       -- 幂运算: 1000
```

**比较运算符：**

```lua
local x, y = 10, 20

x == y    -- false，相等
x ~= y    -- true，不等
x < y     -- true，小于
x > y     -- false，大于
x <= y    -- true，小于等于
x >= y    -- false，大于等于
```

**逻辑运算符：**

```lua
local a, b = true, false

a and b   -- false，逻辑与
a or b    -- true，逻辑或
not a     -- false，逻辑非
```

**字符串操作：**

```lua
local s1 = "Hello"
local s2 = "World"

local concat = s1 .. " " .. s2  -- 字符串连接: "Hello World"
local len = #s1                 -- 字符串长度: 5
```

### 6.3.3 循环结构

**for 循环：**

```lua
-- 数值 for 循环
for i = 1, 10, 2 do
    print(i)  -- 1, 3, 5, 7, 9
end

-- 泛型 for 循环（遍历数组）
local arr = {"a", "b", "c"}
for index, value in ipairs(arr) do
    print(index, value)
end

-- 遍历哈希表
local map = {name = "Tom", age = 25}
for key, value in pairs(map) do
    print(key, value)
end
```

**while 循环：**

```lua
local i = 1
while i <= 5 do
    print(i)
    i = i + 1
end
```

**repeat 循环：**

```lua
local i = 1
repeat
    print(i)
    i = i + 1
until i > 5
```

### 6.3.4 函数定义

```lua
-- 基本函数
local function add(a, b)
    return a + b
end

-- 多返回值
local function swap(a, b)
    return b, a
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

-- 闭包
local function counter()
    local count = 0
    return function()
        count = count + 1
        return count
    end
end
```

### 6.3.5 Redis Lua 常用 API

**redis.call()：**

```lua
-- 语法：redis.call(command, arg1, arg2, ...)
-- 与 Redis 命令一一对应，失败时抛出异常

-- GET 操作
local value = redis.call('GET', 'mykey')

-- SET 操作
redis.call('SET', 'mykey', 'Hello')

-- HSET 操作
redis.call('HSET', 'user:1', 'name', 'Tom')
redis.call('HSET', 'user:1', 'age', 25)

-- MGET 操作
local values = redis.call('MGET', 'key1', 'key2', 'key3')

-- 返回值类型转换：
-- Redis 整数 -> Lua 数字
-- Redis 字符串 -> Lua 字符串
-- Redis 数组 -> Lua 表
-- Redis nil -> Lua nil
```

**redis.pcall()：**

```lua
-- 语法：redis.pcall(command, arg1, arg2, ...)
-- 与 redis.call 功能相同，但错误处理机制不同

-- redis.call 失败时抛出异常，中断脚本执行
-- redis.pcall 失败时返回错误信息，继续执行

local result = redis.pcall('GET', 'nonexist_key')
if result == false then
    return 'Error: ' .. err
end
```

**返回值的转换规则：**

```lua
-- Redis -> Lua
Redis 整数     -> Lua 数字
Redis 字符串   -> Lua 字符串
Redis 数组     -> Lua 表 (ipairs)
Redis nil      -> Lua nil
Redis 多状态值  -> Lua 表

-- Lua -> Redis (通过 return)
Lua 数字       -> Redis 整数
Lua 字符串     -> Redis 字符串
Lua 表(数组)   -> Redis 数组
Lua 表(哈希)   -> Redis 多状态回复
Lua nil        -> Redis nil
```

### 6.3.6 错误处理

```lua
-- 使用 pcall 捕获错误
local status, result = pcall(function()
    -- 可能出错的代码
    redis.call('HSET', 'user:1', 'name', 'Tom')
    local name = redis.call('HGET', 'user:1', 'name')
    return name
end)

if status then
    print('Success: ' .. result)
else
    print('Error: ' .. result)
end
```

```lua
-- 在 Lua 脚本中主动抛出错误
error('This is an error message')
```

```lua
-- 完整的错误处理模式
local function safe_call(func)
    return function(...)
        local args = {...}
        local ok, result = pcall(function()
            return func(unpack(args))
        end)
        if ok then
            return result
        else
            return nil, result  -- 返回 nil 和错误信息
        end
    end
end
```

---

## 6.4 脚本管理

### 6.4.1 SCRIPT LOAD

将 Lua 脚本加载到脚本缓存，返回 SHA1 校验和。

```sh
SCRIPT LOAD "return redis.call('GET', KEYS[1])"
```

**返回示例：**

```
"a420880db46f9d09e2d0c5e9f1f0e3d6c8b7a123"
```

**应用场景：**

- 预加载常用脚本
- 获取脚本 SHA1 用于 EVALSHA

```sh
# Java 示例：预加载脚本
String sha = jedis.scriptLoad(script);
String result = (String) jedis.evalsha(sha, 1, "mykey");
```

### 6.4.2 SCRIPT EXISTS

检查脚本是否已缓存。

```sh
# 检查单个脚本
SCRIPT EXISTS a420880db46f9d09e2d0c5e9f1f0e3d6c8b7a123

# 检查多个脚本
SCRIPT EXISTS a420880db46f9d09e2d0c5e9f1f0e3d6c8b7a123 b520880db46f9d09e2d0c5e9f1f0e3d6c8b7a123
```

**返回示例：**

```
1) (integer) 1  -- 已缓存
2) (integer) 0  -- 未缓存
```

### 6.4.3 SCRIPT FLUSH

清空脚本缓存，释放内存。

```sh
SCRIPT FLUSH
```

**返回：** OK

**注意：** 执行此命令后，所有之前加载的脚本 SHA1 将失效，需要重新加载。

**使用场景：**

- 脚本更新后清除旧缓存
- 内存不足时释放脚本内存
- 调试时重置脚本环境

### 6.4.4 SCRIPT KILL

终止正在运行的脚本。

```sh
SCRIPT KILL
```

**条件限制：**

- 只能终止执行时间超过 `lua-time-limit` 的脚本
- 如果脚本已经执行过写操作，则不能使用 KILL（会导致数据不一致）
- 只能终止只读脚本或可安全中断的脚本

**返回：**

- 成功：OK
- 失败：error ( NOSCRIPT No scripts in execution)

**安全终止流程：**

```
1. 脚本运行时间超过 lua-time-limit（默认 5 秒）
2. Redis 发送 SIGTERM 信号
3. 脚本检查是否应该停止
4. 如果脚本没有执行写操作，可以被 KILL
5. 如果脚本已执行写操作，只能等脚本自然结束
```

---

## 6.5 原子性保证

### 6.5.1 Lua 脚本原子执行原理

Redis 采用单线程模型处理命令，而 Lua 脚本在执行时会作为一个整体运行，不会被其他命令分割。

```
┌─────────────────────────────────────────┐
│           Redis 单线程处理               │
├─────────────────────────────────────────┤
│                                         │
│   命令1 ──┐                             │
│   命令2 ──┤──→ 顺序执行，不会交错       │
│   命令3 ──┘                             │
│                                         │
│   ┌─────────────────────────┐           │
│   │    Lua 脚本执行         │           │
│   │    (完整的原子操作)     │           │
│   └─────────────────────────┘           │
│                                         │
└─────────────────────────────────────────┘
```

**执行流程：**

1. 客户端发送 EVAL 命令
2. Redis 将 Lua 脚本加载到 Lua 虚拟机
3. Lua 脚本连续执行所有 Redis 命令
4. 执行完成后一次性返回所有结果
5. 在脚本执行期间，不会有其他命令插入

### 6.5.2 Lua 脚本与事务的区别

| 特性 | Lua 脚本 | 事务 (MULTI/EXEC) |
|------|---------|-------------------|
| 原子性 | 完全原子 | 基本原子 |
| 回滚 | 不支持 | 支持 DISCARD |
| 条件执行 | 支持 if/else | 不支持 |
| 循环 | 支持 | 不支持 |
| 错误处理 | 取决于 call/pcall | 单条命令错误不回滚 |
| 性能 | 较高 | 每次命令都有开销 |
| 集群支持 | 支持 | 支持 |

**事务回滚示例：**

```sh
MULTI
INCR counter
INCR counter
EXEC
# 如果第二条 INCR 失败，第一条不会回滚
```

**Lua 脚本条件执行：**

```lua
local current = redis.call('GET', KEYS[1])
if current == ARGV[1] then
    redis.call('SET', KEYS[1], ARGV[2])
    return 1
else
    return 0
end
```

### 6.5.3 脚本内命令的原子性

**单命令原子性：**

```lua
-- 每个 redis.call 都是原子操作
redis.call('INCR', 'counter')  -- 原子递增
redis.call('LPUSH', 'list', 'item')  -- 原子入队
```

**组合操作的原子性：**

```lua
-- 整个 Lua 脚本范围内的操作都是原子的
local balance = redis.call('GET', 'account:balance')
if tonumber(balance) >= tonumber(ARGV[1]) then
    redis.call('DECRBY', 'account:balance', ARGV[1])
    redis.call('INCRBY', 'account:frozen', ARGV[1])
    return 'SUCCESS'
else
    return 'INSUFFICIENT_BALANCE'
end
```

**竞态条件对比：**

```
不用 Lua（存在竞态）：
─────────────────
客户端A: GET balance -> 100
客户端B: GET balance -> 100
客户端A: DECRBY balance 80 -> 20
客户端B: DECRBY balance 80 -> -60  ❌ 超支

使用 Lua（原子执行）：
─────────────────
客户端A: EVAL script -> 检查100>=80 -> 扣减 -> SUCCESS
客户端B: EVAL script -> 等待脚本完成 -> 检查20>=80 -> FAIL
```

---

## 6.6 实战案例

### 6.6.1 案例 1：分布式锁完整实现

**需求：**

- 互斥访问资源
- 防死锁（设置过期时间）
- 防误删（只能删除自己加的锁）
- 可重入

```lua
-- 分布式锁脚本：lock.lua
-- KEYS[1] = 锁键名
-- ARGV[1] = 锁值（唯一标识，如 UUID + 线程ID）
-- ARGV[2] = 过期时间（毫秒）

local lock_key = KEYS[1]
local lock_value = ARGV[1]
local expire_time = tonumber(ARGV[2])

-- 尝试获取锁（SET NX EX）
local result = redis.call('SET', lock_key, lock_value, 'NX', 'PX', expire_time)

if result then
    return 1  -- 获取锁成功
else
    -- 检查是否是同一个锁（可重入）
    local current_value = redis.call('GET', lock_key)
    if current_value == lock_value then
        -- 重置过期时间
        redis.call('PEXPIRE', lock_key, expire_time)
        return 1  -- 重入成功
    end
    return 0  -- 获取锁失败
end
```

```java
// Java 实现分布式锁
public class RedisDistributedLock {

    private JedisPool jedisPool;
    private String lockKey;
    private String lockValue;
    private int expireTime;  // 毫秒

    public RedisDistributedLock(JedisPool jedisPool, String lockKey, int expireTime) {
        this.jedisPool = jedisPool;
        this.lockKey = lockKey;
        this.lockValue = UUID.randomUUID().toString() + ":" + Thread.currentThread().getId();
        this.expireTime = expireTime;
    }

    /**
     * 尝试获取锁
     * @return 是否获取成功
     */
    public boolean tryLock() {
        try (Jedis jedis = jedisPool.getResource()) {
            String script =
                "local lock_key = KEYS[1] " +
                "local lock_value = ARGV[1] " +
                "local expire_time = tonumber(ARGV[2]) " +
                "local result = redis.call('SET', lock_key, lock_value, 'NX', 'PX', expire_time) " +
                "if result then " +
                "    return 1 " +
                "else " +
                "    local current_value = redis.call('GET', lock_key) " +
                "    if current_value == lock_value then " +
                "        redis.call('PEXPIRE', lock_key, expire_time) " +
                "        return 1 " +
                "    end " +
                "    return 0 " +
                "end";

            Object result = jedis.eval(script, 1, lockKey, lockValue, String.valueOf(expireTime));
            return "1".equals(result.toString());
        }
    }

    /**
     * 释放锁
     * @return 是否释放成功
     */
    public boolean unlock() {
        try (Jedis jedis = jedisPool.getResource()) {
            // 只释放自己持有的锁
            String script =
                "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                "    return redis.call('DEL', KEYS[1]) " +
                "else " +
                "    return 0 " +
                "end";

            Object result = jedis.eval(script, 1, lockKey, lockValue);
            return "1".equals(result.toString());
        }
    }

    /**
     * 带超时重试的获取锁
     */
    public boolean tryLockWithRetry(int maxRetries, int retryIntervalMs) {
        for (int i = 0; i < maxRetries; i++) {
            if (tryLock()) {
                return true;
            }
            try {
                Thread.sleep(retryIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
```

```java
// 使用示例
public class OrderService {

    private JedisPool jedisPool;

    public void createOrder(Order order) {
        String lockKey = "lock:order:" + order.getUserId();
        RedisDistributedLock lock = new RedisDistributedLock(jedisPool, lockKey, 30000);

        try {
            if (lock.tryLock()) {
                // 持有锁，执行下单逻辑
                // ... 下单处理
            } else {
                throw new RuntimeException("系统繁忙，请稍后重试");
            }
        } finally {
            lock.unlock();
        }
    }
}
```

### 6.6.2 案例 2：计数器（INCR 原子操作）

**需求：**

- 统计接口访问次数
- 每日零点重置
- 原子递增

```lua
-- 计数器脚本：counter.lua
-- KEYS[1] = 计数器键名
-- ARGV[1] = 过期时间（秒）

local counter_key = KEYS[1]
local expire_time = tonumber(ARGV[1])

-- 检查是否需要初始化
local current = redis.call('GET', counter_key)
if not current then
    redis.call('SET', counter_key, 0, 'EX', expire_time)
    current = 0
end

-- 原子递增并返回新值
local new_value = redis.call('INCR', counter_key)

return new_value
```

```java
// Java 实现计数器
public class RedisCounter {

    private JedisPool jedisPool;

    public RedisCounter(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    /**
     * 递增计数器
     * @param key 计数器键
     * @param expireSeconds 过期时间（秒）
     * @return 当前计数值
     */
    public long increment(String key, int expireSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String script =
                "local counter_key = KEYS[1] " +
                "local expire_time = tonumber(ARGV[1]) " +
                "local current = redis.call('GET', counter_key) " +
                "if not current then " +
                "    redis.call('SET', counter_key, 0, 'EX', expire_time) " +
                "    current = 0 " +
                "end " +
                "local new_value = redis.call('INCR', counter_key) " +
                "return new_value";

            Object result = jedis.eval(script, 1, key, String.valueOf(expireSeconds));
            return Long.parseLong(result.toString());
        }
    }

    /**
     * 获取当前计数值
     */
    public long get(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(key);
            return value == null ? 0 : Long.parseLong(value);
        }
    }

    /**
     * 重置计数器
     */
    public void reset(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        }
    }
}
```

```java
// 使用示例
public class ApiService {

    private RedisCounter counter;

    // 统计接口调用
    public String getUserInfo(String userId) {
        // 每日计数器，零点自动过期
        String counterKey = "counter:api:getUserInfo:" + LocalDate.now();

        // 原子递增并获取当前值
        long count = counter.increment(counterKey, 86400);

        // 限流检查
        if (count > 10000) {  // 每日限制 10000 次
            throw new RuntimeException("请求过于频繁，请明日再试");
        }

        // ... 正常业务逻辑
        return "User info";
    }
}
```

### 6.6.3 案例 3：批量扣减库存

**需求：**

- 订单包含多个商品
- 一次性扣减多个商品库存
- 任何一个商品库存不足则全部回滚

```lua
-- 批量扣减库存脚本：deduct_stock.lua
-- KEYS = [item1_key, item2_key, ...]
-- ARGV = [qty1, qty2, ...] (每个商品的扣减数量)

-- 检查所有商品库存是否充足
for i = 1, #KEYS do
    local stock = tonumber(redis.call('GET', KEYS[i]) or 0)
    local deduct_qty = tonumber(ARGV[i])
    if stock < deduct_qty then
        return {0, i}  -- 返回失败索引
    end
end

-- 所有库存充足，开始扣减
for i = 1, #KEYS do
    redis.call('DECRBY', KEYS[i], ARGV[i])
end

return {1, 0}  -- 成功
```

```java
// Java 实现批量扣减库存
public class StockService {

    private JedisPool jedisPool;

    /**
     * 批量扣减库存
     * @param itemQuantities Map<商品ID, 扣减数量>
     * @return 成功返回 null，失败返回不足的商品索引
     */
    public Integer[] deductStock(Map<String, Integer> itemQuantities) {
        if (itemQuantities == null || itemQuantities.isEmpty()) {
            return new Integer[]{1, 0};  // 空订单直接成功
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String[] keys = new String[itemQuantities.size()];
            String[] args = new String[itemQuantities.size()];

            int index = 0;
            for (Map.Entry<String, Integer> entry : itemQuantities.entrySet()) {
                keys[index] = "stock:" + entry.getKey();
                args[index] = String.valueOf(entry.getValue());
                index++;
            }

            String script =
                "for i = 1, #KEYS do " +
                "    local stock = tonumber(redis.call('GET', KEYS[i]) or 0) " +
                "    local deduct_qty = tonumber(ARGV[i]) " +
                "    if stock < deduct_qty then " +
                "        return {0, i} " +
                "    end " +
                "end " +
                "for i = 1, #KEYS do " +
                "    redis.call('DECRBY', KEYS[i], ARGV[i]) " +
                "end " +
                "return {1, 0}";

            Object result = jedis.eval(script, keys, args);

            // 解析返回结果
            if (result instanceof List) {
                @SuppressWarnings("unchecked")
                List<Long> resultList = (List<Long>) result;
                return new Integer[]{resultList.get(0).intValue(), resultList.get(1).intValue()};
            }

            return new Integer[]{0, 0};
        }
    }

    /**
     * 回滚库存（补偿）
     */
    public void rollbackStock(Map<String, Integer> itemQuantities) {
        try (Jedis jedis = jedisPool.getResource()) {
            String[] keys = new String[itemQuantities.size()];
            String[] args = new String[itemQuantities.size()];

            int index = 0;
            for (Map.Entry<String, Integer> entry : itemQuantities.entrySet()) {
                keys[index] = "stock:" + entry.getKey();
                args[index] = String.valueOf(entry.getValue());
                index++;
            }

            String script =
                "for i = 1, #KEYS do " +
                "    redis.call('INCRBY', KEYS[i], ARGV[i]) " +
                "end " +
                "return 'OK'";

            jedis.eval(script, keys, args);
        }
    }
}
```

```java
// 使用示例
public class OrderService {

    private StockService stockService;

    public void createOrder(Order order) {
        Map<String, Integer> itemQuantities = new HashMap<>();
        for (OrderItem item : order.getItems()) {
            itemQuantities.put(item.getItemId(), item.getQuantity());
        }

        Integer[] result = stockService.deductStock(itemQuantities);

        if (result[0] == 0) {
            // 库存不足，失败
            int failedIndex = result[1];
            String failedItemId = (String) itemQuantities.keySet().toArray()[failedIndex - 1];
            throw new RuntimeException("商品[" + failedItemId + "]库存不足");
        }

        // 下单成功，继续处理...
    }
}
```

### 6.6.4 案例 4：防重复提交

**需求：**

- 同一个用户短时间内不能重复提交相同请求
- 使用 Redis 实现请求去重
- 自动过期清理

```lua
-- 防重复提交脚本：dedup.lua
-- KEYS[1] = 去重键名 (format: dedup:user:action:timestamp)
-- ARGV[1] = 过期时间（秒）

local dedup_key = KEYS[1]
local expire_time = tonumber(ARGV[1])

-- SET NX：键不存在才设置（原子操作）
local result = redis.call('SET', dedup_key, '1', 'NX', 'EX', expire_time)

if result then
    return 1  -- 首次提交，设置成功
else
    return 0  -- 重复提交，键已存在
end
```

```java
// Java 实现防重复提交
public class DeduplicationService {

    private JedisPool jedisPool;

    /**
     * 生成去重键
     * @param userId 用户ID
     * @param action 操作类型
     * @param businessId 业务ID（如订单号）
     * @param timeWindow 时间窗口（秒）
     * @return 是否通过（true=首次，false=重复）
     */
    public boolean checkAndSet(String userId, String action, String businessId, int timeWindow) {
        // 生成唯一键：用户+操作+业务ID 组合
        String dedupKey = String.format("dedup:%s:%s:%s", userId, action, businessId);

        try (Jedis jedis = jedisPool.getResource()) {
            String script =
                "local dedup_key = KEYS[1] " +
                "local expire_time = tonumber(ARGV[1]) " +
                "local result = redis.call('SET', dedup_key, '1', 'NX', 'EX', expire_time) " +
                "if result then " +
                "    return 1 " +
                "else " +
                "    return 0 " +
                "end";

            Object result = jedis.eval(script, 1, dedupKey, String.valueOf(timeWindow));
            return "1".equals(result.toString());
        }
    }

    /**
     * 更灵活的防重：基于请求内容的 hash
     */
    public boolean checkByContent(String userId, String action, String requestContent, int timeWindow) {
        // 对请求内容计算 MD5
        String contentHash = md5(requestContent);
        String dedupKey = String.format("dedup:%s:%s:%s", userId, action, contentHash);

        try (Jedis jedis = jedisPool.getResource()) {
            String script =
                "local dedup_key = KEYS[1] " +
                "local expire_time = tonumber(ARGV[1]) " +
                "local result = redis.call('SET', dedup_key, '1', 'NX', 'EX', expire_time) " +
                "if result then " +
                "    return 1 " +
                "else " +
                "    return 0 " +
                "end";

            Object result = jedis.eval(script, 1, dedupKey, String.valueOf(timeWindow));
            return "1".equals(result.toString());
        }
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
```

```java
// 使用示例
public class SubmitService {

    private DeduplicationService dedupService;

    /**
     * 订单提交（防重复）
     */
    public void submitOrder(String userId, String orderId) {
        // 60 秒内同一用户同一订单只能提交一次
        if (!dedupService.checkAndSet(userId, "submit_order", orderId, 60)) {
            throw new RuntimeException("请勿重复提交订单");
        }

        // 正常处理订单逻辑...
        orderService.processOrder(orderId);
    }

    /**
     * 批量处理（防重复）
     */
    public void batchProcess(String userId, String batchId, List<String> items) {
        // 5分钟内同一用户同一批次只能处理一次
        if (!dedupService.checkAndSet(userId, "batch_process", batchId, 300)) {
            throw new RuntimeException("该批次已在处理中，请勿重复提交");
        }

        // 正常批量处理逻辑...
        batchService.processBatch(items);
    }
}
```

---

## 6.7 本章小结

### 核心概念

| 概念 | 说明 |
|------|------|
| **EVAL** | 执行 Lua 脚本的基本命令 |
| **EVALSHA** | 通过 SHA1 执行预加载的脚本 |
| **原子性** | Lua 脚本在 Redis 单线程中完整执行，无竞态条件 |
| **KEYS vs ARGV** | KEYS 参与集群路由，ARGV 不参与 |

### 关键命令

```sh
EVAL script numkeys key [key ...] arg [arg ...]   # 执行脚本
EVALSHA sha1 numkeys key [key ...] arg [arg ...]   # 通过SHA执行
SCRIPT LOAD script                                  # 加载脚本
SCRIPT EXISTS sha1 [sha1 ...]                      # 检查脚本
SCRIPT FLUSH                                        # 清空缓存
SCRIPT KILL                                         # 终止脚本
```

### 应用场景

```
┌─────────────────────────────────────────────────────┐
│                   Lua 脚本应用场景                   │
├─────────────────────────────────────────────────────┤
│  1. 分布式锁 - 原子性保证互斥访问                    │
│  2. 计数器 - 原子递增、自动初始化                    │
│  3. 库存扣减 - 批量操作、原子回滚                     │
│  4. 防重复提交 - 请求去重、时间窗口控制               │
│  5. 条件操作 - if/else 分支逻辑                     │
│  6. 复杂聚合 - 多步骤计算、一致性保证               │
└─────────────────────────────────────────────────────┘
```

### 最佳实践

1. **使用 EVALSHA 代替 EVAL**：减少网络传输，预加载常用脚本
2. **合理设置键数量**：numkeys 要与实际 key 数量一致
3. **注意脚本超时**：`lua-time-limit` 默认 5 秒，设计脚本时避免死循环
4. **脚本幂等性**：编写脚本时考虑重试场景
5. **错误处理**：使用 pcall 进行异常捕获
6. **监控脚本执行**：生产环境监控慢脚本日志

---

> **下一章预告**：Redis 高级篇第七章将介绍 **主从复制** 原理与实践，包括：主从同步原理、复制拓扑、故障转移、哨兵机制等内容。
