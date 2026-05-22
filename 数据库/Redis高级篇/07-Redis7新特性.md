# Redis 7 新特性

## 1、Redis 7 概述

### 1.1 发布时间和版本意义

Redis 7.0 是 Redis 历史上最重要的版本之一，于 **2022年4月** 正式发布。这个版本经历了数年的开发，包含了大量的新功能和性能改进，是 Redis 从 6.0 引入 ACL 和 RESP3 之后的又一次重大更新。

Redis 7.0 标志着 Redis 在企业级功能方面的成熟，包括多租户安全、集群架构优化和持久化机制改进等方面。

### 1.2 主要新特性概览

| 特性类别 | 新增功能 |
|---------|---------|
| 函数（Functions） | 使用 JavaScript 引擎替代 Lua，支持更灵活的脚本编写 |
| ACL 增强 | 多用户支持、临时用户、一次性密码 |
| 集群改进 | Cluster Links、跨集群通信、节点重命名 |
| AOF 改进 | 新的文件结构（base+incr+manifest）、appendonlydir |
| 命令增强 | EXAT/PEXAT、SWAPDB 等新命令 |
| 性能优化 | 内存优化、复制缓冲区改进 |

### 1.3 升级建议

```sh
# 查看当前 Redis 版本
redis-server --version

# Redis 7 要求最低内核版本
# CentOS 7+ / Ubuntu 18.04+ / macOS 10.14+
```

**升级注意事项**：
- Redis 7 使用新的 AOF 格式，升级前建议备份数据
- 集群协议版本升级后无法回退
- 部分命令参数可能发生变化

---

## 2、函数（Functions）

### 2.1 函数 vs 脚本（区别）

Redis 7 引入了**函数（Functions）** 作为脚本的替代方案，主要区别如下：

| 特性 | Lua 脚本 | Functions |
|-----|---------|-----------|
| 引擎 | Lua 5.1 | JavaScript（内置 QuickJS） |
| 加载时机 | 每次执行时加载 | 预加载到内存 |
| 持久化 | 不持久化 | 支持持久化到 .rdb 文件 |
| 管理命令 | EVAL/EVALSHA | FCALL/FUNCTION |
| 命名空间 | 无 | 有函数库概念 |
| 集群支持 | 单节点 | 支持集群 |

```sh
# Lua 脚本示例
> EVAL "return redis.call('GET', KEYS[1])" 1 mykey

# Functions 示例 - 先加载函数
> FUNCTION LOAD "#!js name=mylib\nredis.register_string('GET', 'redis.call')"
"mylib"
> FCALL mylib 1 mykey
```

### 2.2 函数管理命令

Redis 7 提供了一套完整的函数管理命令：

```sh
# 加载函数库
FUNCTION LOAD "#!js name=myfunctions\nredis.register_string('HELLO', function() return 'Hello World' end)"

# 列出所有已加载的函数
FUNCTION LIST

# 查看函数详情
FUNCTION LISTLIBRARY myfunctions

# 删除函数
FUNCTION DELETE myfunctions

# 获取函数信息
FUNCTION GET

# 强制刷新函数（用于热更新）
FUNCTION LOAD "#!js name=myfunctions\n..." FLUSH
```

**FCALL 系列命令**：

```sh
# 调用函数
FCALL function_name number_of_keys [key1, key2, ...] [arg1, arg2, ...]

# 调用指定函数库的函数
FCALLBYLOB function_name number_of_keys [key1, key2, ...] [arg1, arg2, ...]

# 检查函数是否存在
FUNCTION EXISTS myfunctions HELLO
```

### 2.3 JavaScript 引擎（不再是 Lua）

Redis 7 使用 **QuickJS** 作为 JavaScript 引擎，相比 Lua 有以下优势：

```javascript
// Redis Functions 示例
#!js name=stringlib

// 注册一个自定义函数
redis.register_string('CONCAT', function(keys, args) {
    var result = "";
    for (var i = 0; i < keys.length; i++) {
        result = result + redis.call('GET', keys[i]);
    }
    return result;
});

// 注册带参数的函数
redis.register_string('GREET', function(keys, args) {
    var name = args[0] || "Guest";
    return "Hello, " + name;
});
```

**JavaScript 优势**：
- 更广泛的开发者社区
- 更丰富的标准库
- 更好的错误处理和调试能力
- 支持更复杂的业务逻辑

### 2.4 函数加载配置

在 `redis.conf` 中配置函数加载：

```sh
# 是否启用函数库（默认 yes）
lua-time-limit 5000

# 函数库目录（存放 .js 文件）
# dir /var/lib/redis/functions

# 启动时自动加载函数库
# functions-load-source myfunctions /path/to/functions.js
```

**通过配置文件加载函数**：

```sh
# 在 redis.conf 中添加
loadmodule /path/to/functions.so
```

### 2.5 实战：自定义函数

**场景：实现分布式限流函数**

```javascript
#!js name=ratelimit
// 限流函数：基于滑动窗口算法
// 参数：key 前缀、窗口大小（秒）、最大请求数
// 返回：1 表示允许，0 表示拒绝

var request_id = redis.register_string('RATE_LIMIT', function(keys, args) {
    var key = keys[0];
    var window = parseInt(args[0]);
    var limit = parseInt(args[1]);
    var now = redis.call('TIME')[0];
    var window_start = now - window;

    // 移除窗口外的记录
    redis.call('ZREMRANGEBYSCORE', key, 0, window_start);

    // 获取当前窗口内请求数
    var current = redis.call('ZCARD', key);

    if (current < limit) {
        // 添加新请求
        redis.call('ZADD', key, now, now + ':' + Math.random());
        redis.call('EXPIRE', key, window);
        return 1;
    }
    return 0;
});
```

**使用限流函数**：

```sh
# 加载函数
> FUNCTION LOAD "$(cat ratelimit.js)"

# 调用限流函数
# 参数：key前缀 窗口秒数 最大请求数
> FCALL RATE_LIMIT 1 rate:user:1001 60 10
(integer) 1

> FCALL RATE_LIMIT 1 rate:user:1 60 10
(integer) 0
```

---

## 3、ACL 用户管理增强

### 3.1 多用户支持

Redis 7 实现了完整的多用户 ACL 系统，每个用户可以拥有不同的权限：

```sh
# 创建用户
ACL SETUSER alice on >password ~cached:* +get +set +del +@read +@write

# 创建只读用户
ACL SETUSER reader on >password ~* -set -del -@write +@read +@slow

# 创建管理员用户
ACL SETUSER admin on >password ~* +@all

# 查看用户信息
ACL LIST

# 删除用户
ACL DELUSER alice
```

**用户规则说明**：

```
user username on|off           # 启用/禁用用户
>password                      # 设置密码
~keypattern                    # 允许访问的键（支持通配符）
+@category                      # 允许的命令类别
-command                        # 禁止的命令
```

### 3.2 用户权限精细控制

```sh
# 允许特定命令
ACL SETUSER custom on >pass ~* +get +set +hget +hset

# 允许命令类别
# @read @write @admin @slow @fast @keyspace @set @sortedset 等

# 允许特定键模式
ACL SETUSER app_user on >pass ~app:*:session ~app:*:cache +@read +@write

# 禁止危险命令
ACL SETUSER safe_user on >pass ~* -@dangerous +get +set

# 组合示例：电商应用用户
ACL SETUSER ecommerce on >ecommerce_pass ~order:* ~product:* ~user:* \
    +get +set +hget +hset +hmget +hmset \
    +incr +decr +incrby +decrby \
    +expire +pexpire +ttl \
    -flushall -flushdb -config +info
```

### 3.3 临时用户（一次性密码）

Redis 7 支持创建临时用户，适用于单次访问场景：

```sh
# 创建临时用户（1小时后自动过期）
ACL SETUSER temp_user on >temp_pass ~* +@read -@dangerous NOPASS MAXMEMORY 100mb MAXTTL 3600

# 创建一次性密码用户（用完即失效）
ACL SETUSER otp_user on >one_time_pass ~cache:* +get -set NOPASS ONESHOT

# ONESHOT 模式：用户首次认证后删除自己
# 验证一次性密码
> AUTH otp_user one_time_pass
OK
# 用户自动被删除
```

**临时用户使用场景**：
- 外部API的临时访问
- 批处理作业的一次性凭证
- 共享账户的限时访问

### 3.4 ACL 规则优化

Redis 7 改进了 ACL 规则的表达能力：

```sh
# 启用用户但无需密码（需要 protected-mode = no）
ACL SETUSER nopass_user off ~* +@all

# 禁用所有命令但保留特定子命令
ACL SETUSER subcommand_user on >pass ~* +client +cluster +slowlog +scripting -@all

# 允许访问指定频道
ACL SETUSER pubsub_user on >pass ~* +subscribe +psubscribe +unsubscribe -@all +@pubsub

# 启用所有已禁用命令的子集
ACL SETUSER selective on >pass ~* -@all +@read +get +mget +@sortedset +zrange

# 重置用户（清除所有规则）
ACL SETUSER username reset
```

**ACL 日志**：

```sh
# 启用 ACL 日志
 ACL LOG 10                    # 显示最近10条日志
 ACL LOG RESET                 # 清除日志

# 日志内容包括：时间、用户、客户端IP、命令、键、结果（允许/拒绝）
```

---

## 4、集群管理改进

### 4.1 集群节点认证改进

Redis 7 改进了集群内部的认证机制：

```sh
# 在 redis.conf 中配置集群认证
cluster-enabled yes
cluster-config-file nodes.conf
cluster-require-full-coverage no

# 集群节点间认证（使用集群总线密钥）
cluster-preferred-endpoint-type ip

# 新增：集群认证配置
# 在每个节点的 redis.conf 中设置相同的认证密码
```

**集群节点认证命令**：

```sh
# 查看集群节点认证状态
CLUSTER NODES

# 新增：CLUSTER MEET 命令增强
CLUSTER MEET <ip> <port> [bus-port] [flags]

# 设置节点作为主节点并分配插槽
CLUSTER ADDSLOTS <slot> [slot ...]
```

### 4.2 集群配置文件变化

Redis 7 采用了新的集群配置文件格式：

```sh
# 旧版格式 (Redis 6.x)
nodes.conf:
3e11f749478ac4a6ed5d7e2b2e05f5f6c1b3d4e5 10.0.0.1:6379@16379 master - 0 1640000000000 1 connected 0-5460
...

# Redis 7 新格式
# format: id flags Endpoint:port[@bus] [master_id] [role] [last_ping_sent] [last_ok_ping_reply] [last_pong_rcvd] [config_epoch] [link_status] [slots]

3e11f749478ac4a6ed5d7e2b2e05f5f6c1b3d4e5@16379 myself,master - 0 0 0 connected 0-5460 10923-16383
c4f2a8b9d1e7f3c6a2d5e8b1f4a3c7e9d2b5f8a1@16379 master - 0 0 0 connected 5461-10922
```

**新增字段**：
- `@bus`: 集群总线端口
- `link_status`: 连接状态
- `Endpoint`: 支持 DNS 名称

### 4.3 集群命令增强

Redis 7 新增和改进了多个集群命令：

```sh
# 新增：集群链接管理
CLUSTER LINKS                                    # 查看集群节点间所有连接
CLUSTER SLOTS                                    # 获取插槽分配信息

# 新增：节点重命名支持
CLUSTER RENAMENODE <old_name> <new_name>         # 重命名集群节点

# 新增：集群配置重载
CLUSTER RELOAD CONFIG                            # 重新加载集群配置

# 改进：集群信息
CLUSTER INFO                                     # 更详细的集群信息
```

**CLUSTER LINKS 输出示例**：

```sh
CLUSTER LINKS
1) 1) "node_address"
      "10.0.0.1:6379"
   2) "remote_address"
      "10.0.0.2:56379"
   3) "link_direction"
      "->from"
   4) "node_id"
      "c4f2a8b9d1e7f3c6"
   5) "connection_state"
      "connected"
   6) "last_update"
      "1640000000123"
```

### 4.4 插槽迁移改进

Redis 7 改进了插槽迁移流程：

```sh
# 源节点：发起插槽迁移
CLUSTER SETSLOT <slot> MIGRATING <node_id>

# 目标节点：接收插槽
CLUSTER SETSLOT <slot> IMPORTING <node_id>

# 迁移完成后：确认插槽归属
CLUSTER SETSLOT <slot> NODE <node_id>

# 新增：一键迁移命令
CLUSTER MIGRATE <source_node_id> <target_node_id> <slot> [TIMEOUT timeout] [COPY] [REPLACE]

# 改进的迁移状态查询
CLUSTER GETKEYSINSLOT <slot> <count>             # 获取指定插槽的键
```

**迁移改进点**：
- 支持在线迁移（无需停止服务）
- 改进了迁移过程中的数据一致性
- 支持批量迁移多个插槽

---

## 5、Redis 7 多集群支持

### 5.1 Cluster Links 配置

Redis 7 引入了 **Cluster Links** 功能，支持更灵活的集群间通信：

```sh
# redis.conf 配置
# 启用集群间链接
cluster-link-queue-limit 1000
cluster-link-sendbuf-limit 8mb

# 集群间连接超时
cluster-node-timeout 15000

# 集群总线超时
cluster-replica-validity-factor 10
```

**Cluster Links 特性**：
- 支持跨数据中心复制
- 可配置的网络优先级
- 连接池管理优化

### 5.2 跨集群通信

Redis 7 改进了集群间的通信机制：

```sh
# 配置集群间通信协议版本
cluster-protocol-version 2

# 跨集群复制配置
# replicaof <master_ip> <master_port>

# 查看集群间连接状态
CLUSTER SLAVES <node_id>
CLUSTER REPLICAS <node_id>
```

**跨集群通信改进**：
- 更低的延迟
- 更好的故障检测
- 支持跨地域部署

### 5.3 节点重命名支持

Redis 7 支持在运行时重命名集群节点：

```sh
# 重命名节点
CLUSTER RENAMENODE c4f2a8b9d1e7f3c6a2d5e8b1f4a3c7e9d2b5f8a1 my-new-node-name

# 查看重命名后的节点
CLUSTER NODES
```

**使用场景**：
- 节点替换后的标识更新
- 维护期间的临时标识
- 多集群环境中的节点标识管理

---

## 6、AOF 文件变化

### 6.1 appendonlydir 配置

Redis 7 改变了 AOF 文件的存储方式，采用目录化管理：

```sh
# Redis 7 配置
# AOF 文件目录（所有 AOF 文件存储在此目录）
appendonlydir /var/lib/redis

# AOF 文件命名规则
appendfilename "appendonly.aof"

# AOF 持久化策略
appendfsync everysec

# 旧版单一文件配置（Redis 7 仍支持但有变化）
# appendonly yes
# appendfilename "appendonly.aof.${date}.${port}"
```

**目录结构**：

```sh
/var/lib/redis/
├── appendonly.aof.1.base.aof    # 基础 AOF 文件
├── appendonly.aof.1.incr.aof    # 增量 AOF 文件
├── appendonly.aof.manifest      # AOF 文件清单
└── dump.rdb                      # RDB 文件
```

### 6.2 新的 AOF 文件结构

Redis 7 使用 **base + incr + manifest** 三层结构：

```
┌─────────────────────────────────────────────────────────────┐
│                     appendonly.manifest                      │
│  (文件清单，记录当前 base 和 incr 文件)                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────┐       ┌──────────────────┐           │
│  │  appendonly.aof  │       │  appendonly.aof  │           │
│  │  .1.base.aof     │       │  .2.incr.aof     │           │
│  │  (基础快照)        │       │  (增量操作)        │           │
│  └──────────────────┘       └──────────────────┘           │
│                                                             │
│  ┌──────────────────┐       ┌──────────────────┐           │
│  │  appendonly.aof  │       │  appendonly.aof  │           │
│  │  .2.base.aof     │       │  .1.incr.aof     │           │
│  │  (新基础快照)     │       │  (增量操作)       │           │
│  └──────────────────┘       └──────────────────┘           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**manifest 文件内容示例**：

```sh
# cat appendonly.manifest
file appendonly.aof.1.base.aof seq 1 type b
file appendonly.aof.1.incr.aof seq 1 type i
file appendonly.aof.2.incr.aof seq 2 type i
```

**三层结构说明**：

| 文件类型 | 说明 | 作用 |
|---------|------|------|
| base | 基础快照 | 某一时间点的完整数据快照 |
| incr | 增量操作 | base 之后的增量写操作 |
| manifest | 文件清单 | 管理和追踪所有 AOF 文件 |

### 6.3 AOF 重写改进

Redis 7 改进了 AOF 重写机制：

```sh
# AOF 重写配置
aof-use-rdb-preamble yes                    # 混合持久化（默认开启）
aof-load-truncated yes                       # 加载截断的 AOF 文件
aof-rewrite-incremental-fsync yes            # 增量 fsync

# 手动触发 AOF 重写
BGREWRITEAOF

# 查看 AOF 状态
INFO persistence
```

**改进点**：

1. **混合持久化**：
   - 使用 RDB 格式存储基础数据
   - 使用 AOF 格式存储增量操作
   - 结合了 RDB 的快速加载和 AOF 的数据完整性

2. **增量 fsync**：
   - 每写入一定量数据后执行 fsync
   - 减少 fsync 对性能的影响

3. **更好的容错**：
   - AOF 文件损坏时自动尝试恢复
   - 支持从多个 AOF 文件恢复

**AOF 恢复流程**：

```sh
# Redis 7 启动时自动执行
1. 读取 manifest 文件
2. 按顺序加载 base 文件
3. 按顺序应用 incr 文件
4. 如果 incr 文件损坏，尝试跳过损坏部分
5. 完成数据恢复
```

---

## 7、其他重要更新

### 7.1 新的 EXPIRE 命令变体

Redis 7 新增了精确到毫秒的过期时间命令：

```sh
# 设置键的过期时间（秒）- 原有命令
EXPIRE key 3600

# 设置键的过期时间（毫秒）- 新增命令
PEXPIRE key 3600000

# 设置键在指定时间戳过期（秒）- 新增命令
EXAT <key> <timestamp>

# 设置键在指定时间戳过期（毫秒）- 新增命令
PEXAT <key> <timestamp_ms>

# 示例
> SET token "abc123"
> EXAT token 1748000000          # 在 Unix 时间戳 1748000000 过期

> SET session "xyz789"
> PEXAT session 1748000000000    # 在毫秒时间戳过期
```

**使用场景**：
- 定时任务精确控制过期时间
- 从其他系统导入数据时保留原始 TTL
- 分布式系统中时间同步

### 7.2 新的 SWAPDB 命令

Redis 7 新增了 SWAPDB 命令，用于交换两个数据库的内容：

```sh
# 交换数据库 0 和 1
SWAPDB 0 1

# 示例：交换前
> SELECT 0
OK
> KEYS *
1) "user:1001"
2) "user:1002"

> SELECT 1
OK
> KEYS *
1) "product:001"
2) "product:002"

# 执行交换
> SWAPDB 0 1

# 交换后
> SELECT 0
OK
> KEYS *
1) "product:001"
2) "product:002"

> SELECT 1
OK
> KEYS *
1) "user:1001"
2) "user:1002"
```

**使用场景**：
- 蓝绿部署中的数据库切换
- 批量导入数据时避免停机
- 测试环境与生产环境数据切换

### 7.3 性能改进

Redis 7 在性能方面做了多项优化：

| 优化项 | 说明 | 性能提升 |
|-------|------|---------|
| 内存优化 | 优化了内部数据结构 | 降低 20-30% 内存使用 |
| 复制缓冲区 | 改进 replication buffer 管理 | 减少复制延迟 |
| 命令处理 | 优化了命令执行路径 | 提升 QPS 5-10% |
| 网络 I/O | 改进 IO 多路复用 | 更好的并发处理 |
| 集群通信 | 优化了节点间通信 | 降低集群开销 |

**内存优化详情**：

```sh
# 查看内存使用详情
> INFO memory
# Memory
# used_memory:1048576
# used_memory_human:1.00M
# used_memory_rss:2097152
# used_memory_rss_human:2.00M
# allocator_allocated:1048576
# allocator_active:1572864
# allocator_resident:2097152
# total_system_memory:8589934592
# total_system_memory_human:8.00G
# used_memory_lua:33792
# used_memory_lua_human:33.00K
# used_memory_overhead:40960
# used_memory_startup:917504
# used_memory_dataset:614400
# memory_fragmentation_ratio:2.00
# memory_fragmentation_bytes:1048576
```

### 7.4 内存优化

Redis 7 主要的内存优化技术：

```sh
# 内存分配器优化
#jemalloc 5.x 集成，提供更好的内存碎片管理

# 紧凑型内部数据结构
# 使用更节省内存的编码格式

# 键空间通知优化
# 减少通知生成的开销
notify-keyspace-events Ex

# Lua 脚本内存优化
# 改进的脚本执行引擎
```

**内存优化技术对比**：

| Redis 6.x | Redis 7 | 改进 |
|-----------|---------|------|
| 简单字符串编码 | 更紧凑的编码 | 节省 30% |
| 压缩列表 | listpack | 减少内存碎片 |
| 较大对象池 | 优化的对象池 | 降低分配开销 |

### 7.5 调试和安全增强

```sh
# 新增调试命令
DEBUG SLEEP <seconds>                    # 模拟延迟
DEBUG STRUCTSIZE                         # 查看内部结构大小
DEBUG AOFCHECK                           # 检查 AOF 文件完整性

# 安全增强
# 改进的 PROTECTED-MODE
protected-mode yes                       # 默认启用更严格
# 禁止危险命令组合
```

---

## 总结

Redis 7 是一个里程碑版本，带来了众多企业级特性：

| 类别 | 核心改进 |
|-----|---------|
| **函数** | JavaScript 引擎、持久化函数库、更好的集群支持 |
| **安全** | 完整的多用户 ACL、临时用户、精细权限控制 |
| **集群** | Cluster Links、跨集群通信、节点重命名 |
| **持久化** | 新的 AOF 结构（base+incr+manifest）、混合持久化 |
| **性能** | 内存优化、复制改进、命令处理优化 |
| **管理** | EXAT/PEXAT、SWAPDB、改进的监控命令 |

**升级建议**：
1. 充分利用函数功能简化业务逻辑
2. 使用 ACL 实现多租户安全隔离
3. 利用新的 AOF 结构提升数据安全性
4. 关注集群改进以支持更大规模部署

Redis 7 的设计目标是为企业级应用提供更安全、更易管理、更高性能的 Redis 体验。
