---
title: 第9章 MongoDB 副本集与高可用
sidebar_label: 第9章
---

# 第9章 MongoDB 副本集与高可用

副本集（Replica Set）是 MongoDB 提供的高可用性解决方案，通过数据复制和自动故障转移机制确保数据库的持续可用性。本章将详细介绍副本集的原理、配置以及 Spring Boot 中的实战应用。

## 9.1 副本集概念与原理

### 9.1.1 副本集组成

MongoDB 副本集由以下几种类型的节点组成：

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#1E88E5', 'primaryTextColor': '#fff', 'lineColor': '#424242'}}}%%
graph TB
    subgraph ReplicaSet["MongoDB 副本集"]
        subgraph Primary["Primary 节点"]
            P[("主节点<br/>写入/读取")]
        end

        subgraph Secondaries["Secondary 节点"]
            S1[("Secondary 1<br/>热备")]
            S2[("Secondary 2<br/>热备")]
        end

        subgraph Optional["可选节点"]
            A[("Arbiter<br/>投票")]
        end

        P -->|数据复制| S1
        P -->|数据复制| S2
        P -.->|投票| A
        S1 -.->|投票| A
        S2 -.->|投票| A
    end

    Client1["客户端"]
    Client2["客户端"]
    Client1 -->|写入| P
    Client2 -->|读取| S1

    style P fill:#1E88E5,stroke:#1565C0,color:#fff
    style S1 fill:#43A047,stroke:#2E7D32,color:#fff
    style S2 fill:#43A047,stroke:#2E7D32,color:#fff
    style A fill:#FB8C00,stroke:#E65100,color:#fff
    style Client1 fill:#8E24AA,stroke:#6A1B9A,color:#fff
    style Client2 fill:#8E24AA,stroke:#6A1B9A,color:#fff
```

| 节点类型 | 功能 | 参与投票 | 可成为 Primary |
|---------|------|---------|---------------|
| Primary | 接收所有写操作，是副本集中唯一可写的节点 | 是 | 本身即是 |
| Secondary | 从 Primary 复制数据，可提供读服务（可选） | 是 | 是（选举后） |
| Arbiter | 只参与投票，不存储数据 | 是 | 否 |

**节点角色说明：**

- **Primary 节点**：副本集中唯一可接收写操作的节点。所有写操作都通过 Primary 节点执行，然后异步复制到 Secondary 节点。

- **Secondary 节点**：从 Primary 复制 OPlog 并应用，保持数据同步。默认情况下可提供读服务（负载均衡），也可以配置为隐藏节点（不参与读负载均衡）。

- **Arbiter 节点**：不存储实际数据，仅用于在选举中投票。当副本集节点数为偶数时，添加 Arbiter 可以打破僵局，提高可用性。

### 9.1.2 数据复制原理（Oplog）

Oplog（Operations Log）是 MongoDB 副本集复制机制的核心，它是一个特殊的 capped collection，记录了所有对主节点数据的修改操作。

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#1E88E5', 'primaryTextColor': '#fff', 'lineColor': '#424242'}}}%%
sequenceDiagram
    participant P as Primary
    participant O as Oplog
    participant S as Secondary

    Note over P: 用户写入操作
    P->>P: 执行写操作
    P->>O: 写入 oplog 条目
    O-->>P: oplog 写入成功

    loop 异步复制
        S->>O: 拉取新的 oplog
        O-->>S: 返回 oplog 条目
        S->>S: 应用 oplog 操作
    end

    style P fill:#1E88E5,stroke:#1565C0,color:#fff
    style O fill:#FB8C00,stroke:#E65100,color:#fff
    style S fill:#43A047,stroke:#2E7D32,color:#fff
```

**Oplog 关键特性：**

| 特性 | 说明 |
|-----|------|
| 幂等性 | Oplog 条目是幂等的，应用多次与应用一次结果相同 |
| 无锁定 | Primary 写入不需要锁定，不影响读操作 |
| 增量复制 | Secondary 只需拉取增量 oplog，高效同步 |
| 独立存储 | Oplog 存储在 local 数据库中，不影响业务数据 |

**Oplog 常用查询命令：**

```javascript
// 查看 oplog 状态
db.getReplicationInfo()

// 查看 oplog 大小
db.oplog.rs.stats()

// 查看最近的操作
db.oplog.rs.find().sort({$natural:-1}).limit(10)

// 设置 oplog 大小（副本集初始化后）
db.adminCommand({replSetResizeOplog: 1, size: 16384})
```

### 9.1.3 故障转移机制

当 Primary 节点发生故障时，副本集会自动触发故障转移流程。

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#1E88E5', 'primaryTextColor': '#fff', 'lineColor': '#424242'}}}%%
flowchart TB
    subgraph Normal["正常运行状态"]
        A1[Primary] -->|复制| B1[Secondary 1]
        A1 -->|复制| B2[Secondary 2]
    end

    subgraph Failure["故障发生"]
        C[Primary 宕机] --> D{检测故障}
    end

    subgraph Election["选举过程"]
        D -->|心跳超时| E[Secondary 发起选举]
        E --> F[各节点投票]
        F --> G{是否获得<br/>多数票?}
    end

    subgraph Failover["故障转移完成"]
        G -->|是| H[Secondary 1 成为新 Primary]
        H --> I[恢复读写服务]
        G -->|否| J[无 Primary<br/>副本集只读]
    end

    subgraph Recovery["原节点恢复"]
        I --> K[原 Primary 恢复后<br/>自动变为 Secondary]
    end

    A1 -.-> C
    B1 -.-> D
    B2 -.-> D

    style A1 fill:#1E88E5,stroke:#1565C0,color:#fff
    style B1 fill:#43A047,stroke:#2E7D32,color:#fff
    style B2 fill:#43A047,stroke:#2E7D32,color:#fff
    style C fill:#E53935,stroke:#C62828,color:#fff
    style D fill:#FB8C00,stroke:#E65100,color:#fff
    style E fill:#8E24AA,stroke:#6A1B9A,color:#fff
    style F fill:#8E24AA,stroke:#6A1B9A,color:#fff
    style G fill:#FB8C00,stroke:#E65100,color:#fff
    style H fill:#1E88E5,stroke:#1565C0,color:#fff
    style I fill:#43A047,stroke:#2E7D32,color:#fff
    style J fill:#E53935,stroke:#C62828,color:#fff
    style K fill:#00ACC1,stroke:#00838F,color:#fff
```

**故障转移详细流程：**

1. **故障检测**：Secondary 节点通过心跳机制（默认每 2 秒一次）检测 Primary 是否存活
2. **心跳超时**：若 Primary 10 秒内未响应心跳，判定为不可用
3. **发起选举**：存活的 Secondary 节点发起选举，竞选成为新的 Primary
4. **投票决策**：所有有投票权的节点投票，获得多数票的节点胜出
5. **角色切换**：胜出的节点转换为 Primary，开始接收写操作

### 9.1.4 选举机制

MongoDB 副本集采用基于 Raft 协议的选举机制来选举 Primary 节点。

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#1E88E5', 'primaryTextColor': '#fff', 'lineColor': '#424242'}}}%%
flowchart LR
    subgraph "选举因素"
        A["数据完整性<br/>priority"] --> B["投票权重"]
        C["心跳健康<br/>lastHeartbeat"] --> B
        D["网络延迟<br/>electionTimeout"] --> B
        E["数据最新程度<br/>optime"] --> B
    end

    subgraph "选举规则"
        B --> F{是否获得<br/>多数票?}
        F -->|是| G["成为 Primary"]
        F -->|否| H["保持 Secondary"]
    end

    style A fill:#1E88E5,stroke:#1565C0,color:#fff
    style C fill:#1E88E5,stroke:#1565C0,color:#fff
    style D fill:#1E88E5,stroke:#1565C0,color:#fff
    style E fill:#1E88E5,stroke:#1565C0,color:#fff
    style B fill:#FB8C00,stroke:#E65100,color:#fff
    style F fill:#8E24AA,stroke:#6A1B9A,color:#fff
    style G fill:#43A047,stroke:#2E7D32,color:#fff
    style H fill:#00ACC1,stroke:#00838F,color:#fff
```

**影响选举的关键因素：**

| 因素 | 说明 | 配置方式 |
|-----|------|---------|
| priority | 优先级，越高越可能成为 Primary | `members[n].priority` |
| lastHeartbeat | 心跳健康状态 | 自动检测 |
| optime | 数据最新程度（逻辑时间） | 自动同步 |
| electionTimeout | 选举超时时间（默认 10s） | `settings.electionTimeoutMillis` |
| votes | 投票权重（0 或 1） | `members[n].votes` |

**选举优先级配置示例：**

```javascript
// 副本集配置示例
{
    _id: "rs0",
    members: [
        { _id: 0, host: "192.168.1.101:27017", priority: 3 },  // 最高优先级
        { _id: 1, host: "192.168.1.102:27017", priority: 2 },  // 次高优先级
        { _id: 2, host: "192.168.1.103:27017", priority: 1 },  // 默认优先级
        { _id: 3, host: "192.168.1.104:27017", arbiterOnly: true }  // 仲裁节点
    ],
    settings: {
        electionTimeoutMillis: 15000  // 15秒超时
    }
}
```

---

## 9.2 副本集配置与搭建

### 9.2.1 单机模拟副本集配置

在单机环境下模拟副本集，需要启动多个 MongoDB 实例，每个实例使用不同的端口和数据目录。

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#1E88E5', 'primaryTextColor': '#fff', 'lineColor': '#424242'}}}%%
graph TB
    subgraph "单机模拟副本集"
        M1["MongoDB 实例 1<br/>27017 - Primary"]
        M2["MongoDB 实例 2<br/>27018 - Secondary"]
        M3["MongoDB 实例 3<br/>27019 - Secondary"]
    end

    M1 -->|数据复制| M2
    M1 -->|数据复制| M3

    style M1 fill:#1E88E5,stroke:#1565C0,color:#fff
    style M2 fill:#43A047,stroke:#2E7D32,color:#fff
    style M3 fill:#43A047,stroke:#2E7D32,color:#fff
```

**步骤 1：创建数据目录**

```bash
# 创建三个 MongoDB 实例的数据目录
mkdir -p /data/rs0 /data/rs1 /data/rs2

# 创建日志目录
mkdir -p /var/log/mongodb
```

**步骤 2：启动三个 MongoDB 实例**

```bash
# 启动第一个实例（Primary）
mongod --replSet rs0 --port 27017 --dbpath /data/rs0 --logpath /var/log/mongodb/rs0.log --fork

# 启动第二个实例（Secondary）
mongod --replSet rs0 --port 27018 --dbpath /data/rs1 --logpath /var/log/mongodb/rs1.log --fork

# 启动第三个实例（Secondary）
mongod --replSet rs0 --port 27019 --dbpath /data/rs2 --logpath /var/log/mongodb/rs2.log --fork
```

**步骤 3：初始化副本集**

```javascript
// 连接到任意一个实例
mongosh --port 27017

// 初始化副本集配置
rs.initiate({
    _id: "rs0",
    members: [
        { _id: 0, host: "localhost:27017" },
        { _id: 1, host: "localhost:27018" },
        { _id: 2, host: "localhost:27019" }
    ]
})
```

**Windows 环境下的启动方式：**

```powershell
# 启动第一个实例
mongod --replSet rs0 --port 27017 --dbpath D:\data\rs0 --logpath D:\log\mongodb\rs0.log

# 启动第二个实例
mongod --replSet rs0 --port 27018 --dbpath D:\data\rs1 --logpath D:\log\mongodb\rs1.log

# 启动第三个实例
mongod --replSet rs0 --port 27019 --dbpath D:\data\rs2 --logpath D:\log\mongodb\rs2.log
```

### 9.2.2 副本集初始化命令

**完整副本集配置示例：**

```javascript
// 连接到 Primary 节点后执行
config = {
    _id: "rs0",
    protocolVersion: 1,
    members: [
        {
            _id: 0,
            host: "192.168.1.101:27017",
            priority: 3,
            votes: 1
        },
        {
            _id: 1,
            host: "192.168.1.102:27017",
            priority: 2,
            votes: 1
        },
        {
            _id: 2,
            host: "192.168.1.103:27017",
            priority: 1,
            votes: 1
        },
        {
            _id: 3,
            host: "192.168.1.104:27017",
            arbiterOnly: true,
            votes: 1
        }
    ],
    settings: {
        chainingAllowed: true,           // 允许链式复制
        heartbeatTimeoutSecs: 10,         // 心跳超时（秒）
        electionTimeoutMillis: 15000,     // 选举超时（毫秒）
        catchUpTimeoutMillis: 3000        // 追赶超时（毫秒）
    }
}

// 初始化副本集
rs.initiate(config)

// 查看初始化结果
printjson(rs.status())
```

**添加/移除节点命令：**

```javascript
// 添加 Secondary 节点
rs.add({
    host: "192.168.1.105:27017",
    priority: 1,
    votes: 1
})

// 添加 Arbiter 节点
rs.addArb("192.168.1.106:27017")

// 移除节点
rs.remove("192.168.1.105:27017")

// 重新配置副本集（修改配置）
rs.reconfig(config)
```

### 9.2.3 副本集状态查看

**常用状态查看命令：**

```javascript
// 查看副本集状态（最常用）
rs.status()

// 查看副本集配置
rs.conf()

// 查看当前节点角色
db.isMaster()

// 查看同步状态详情
db.getReplicationInfo()

// 查看各节点的延迟
rs.printSecondaryReplicationInfo()

// 查看 oplog 状态
db.getSiblingDB('local').oplog.rs.stats()
```

**`rs.status()` 关键字段说明：**

```javascript
{
    "set": "rs0",                    // 副本集名称
    "date": ISODate(),               // 状态更新时间
    "myState": 1,                    // 当前节点状态（1=Primary, 2=Secondary, 7=Arbiter）
    "term": 23,                      // 任期号
    "members": [
        {
            "_id": 0,
            "name": "192.168.1.101:27017",
            "health": 1,             // 节点健康状态（1=健康, 0=不健康）
            "state": 1,              // 节点角色
            "stateStr": "PRIMARY",   // 角色描述
            "uptime": 86400,         // 运行时间（秒）
            "optime": {               // 最后同步的 op 时间戳
                "ts": Timestamp(1234567890, 1),
                "t": 23
            },
            "optimeDate": ISODate(), // optime 对应的时间
            "lastHeartbeat": ISODate(),  // 最后心跳时间
            "lastHeartbeatRecv": ISODate(),
            "pingMs": 0,             // ping 延迟（毫秒）
            "syncSourceHost": "",
            "infoMessage": "",
            "electionTime": Timestamp(1234567890, 1),
            "electionDate": ISODate(),
            "configVersion": 3       // 配置文件版本
        },
        // ... 其他成员
    ]
}
```

**节点状态值说明：**

| 状态值 | 状态名 | 说明 |
|-------|--------|------|
| 0 | STARTUP | 节点正在初始化 |
| 1 | PRIMARY | 主节点，可读写 |
| 2 | SECONDARY | 从节点，只读 |
| 3 | RECOVERING | 恢复中 |
| 5 | STARTUP2 | 正在同步 |
| 6 | UNKNOWN | 状态未知 |
| 7 | ARBITER | 仲裁节点 |
| 8 | DOWN | 节点不可达 |
| 9 | ROLLBACK | 数据回滚中 |
| 10 | REMOVED | 已移除 |

---

## 9.3 Spring Boot 连接副本集

### 9.3.1 连接字符串配置

Spring Boot 连接 MongoDB 副本集使用标准的 MongoDB 连接字符串格式。

```yaml
# application.yml 配置
spring:
  data:
    mongodb:
      # 副本集连接字符串
      uri: mongodb://192.168.1.101:27017,192.168.1.102:27017,192.168.1.103:27017/?replicaSet=rs0

      # 或者分开配置
      # host: 192.168.1.101
      # port: 27017
      # database: mydb
      # username: myuser
      # password: mypassword
```

**连接字符串参数说明：**

```yaml
uri: mongodb://[username:password@]host1[:port1][,host2[:port2]]...[/[database][?options]]

# 完整示例
uri: mongodb://myuser:mypass@192.168.1.101:27017,192.168.1.102:27017,192.168.1.103:27017/mydb?replicaSet=rs0&authSource=admin&readPreference=secondaryPreferred&w=majority
```

| 参数 | 说明 | 可选值 |
|-----|------|-------|
| replicaSet | 副本集名称 | 副本集名称 |
| readPreference | 读偏好 | `primary`、`primaryPreferred`、`secondary`、`secondaryPreferred`、`nearest` |
| authSource | 认证数据库 | admin 或其他 |
| w | 写关注 | `1`、`majority`、`<number>` |
| wtimeoutMS | 写操作超时 | 毫秒数 |

### 9.3.2 副本集配置类

```java
package com.example.mongo.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.connection.ServerSettings;
import com.mongodb.connection.netty.NettyStreamFactory;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * MongoDB 副本集配置类
 */
@Configuration
@EnableMongoRepositories(basePackages = "com.example.mongo.repository")
public class MongoReplicaSetConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String database;

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Override
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(mongoUri);

        // 构建集群设置
        ClusterSettings clusterSettings = ClusterSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        // 构建连接池设置
        ConnectionPoolSettings poolSettings = ConnectionPoolSettings.builder()
                .maxSize(100)                    // 最大连接数
                .minSize(10)                     // 最小连接数
                .maxWaitTime(30, TimeUnit.SECONDS)  // 最大等待时间
                .maxConnectionIdleTime(60, TimeUnit.SECONDS)  // 空闲连接超时
                .maxConnectionLifeTime(300, TimeUnit.SECONDS) // 连接最大生命周期
                .build();

        // 构建服务器设置
        ServerSettings serverSettings = ServerSettings.builder()
                .heartbeatFrequency(10, TimeUnit.SECONDS)  // 心跳频率
                .minHeartbeatFrequency(500, TimeUnit.MILLISECONDS)  // 最小心跳频率
                .build();

        // 构建 MongoDB 客户端设置
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .clusterSettings(clusterSettings)
                .connectionPoolSettings(poolSettings)
                .serverSettings(serverSettings)
                .applicationName("mongodb-replica-set")
                .build();

        return MongoClients.create(settings);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDbFactory,
                                        MappingMongoConverter converter) {
        return new MongoTemplate(mongoDbFactory, converter);
    }
}
```

### 9.3.3 读写分离配置

#### 方式一：使用配置文件指定读偏好

```yaml
# application-replica.yml
spring:
  data:
    mongodb:
      uri: mongodb://192.168.1.101:27017,192.168.1.102:27017,192.168.1.103:27017/mydb?replicaSet=rs0&readPreference=secondaryPreferred
```

#### 方式二：使用 @ReadPreference 注解

```java
package com.example.mongo.repository;

import com.example.mongo.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReadPreference;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户 Repository - 读偏好配置
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    // 使用 Secondary 节点读取
    @ReadPreference("secondaryPreferred")
    List<User> findByStatus(Integer status);

    // 使用 Nearest 节点读取（根据延迟选择）
    @Query(value = "{}", fields = "{'name': 1, 'email': 1}")
    @ReadPreference("nearest")
    List<User> findAllBrief();

    // 默认使用 Primary 读取（敏感数据）
    @ReadPreference("primary")
    User findByUsername(String username);
}
```

#### 方式三：使用 MongoTemplate 配置读偏好

```java
package com.example.mongo.service;

import com.example.mongo.entity.User;
import com.mongodb.ReadPreference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 读偏好策略服务
 */
@Service
public class ReadPreferenceService {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 从 Secondary 节点读取（非实时数据）
     */
    public List<User> findUsersFromSecondary(Integer status) {
        Query query = new Query(Criteria.where("status").is(status));
        return mongoTemplate.find(query, User.class, "users",
                ReadPreference.secondaryPreferred());
    }

    /**
     * 从最近节点读取（低延迟优先）
     */
    public List<User> findUsersFromNearest(Integer status) {
        Query query = new Query(Criteria.where("status").is(status));
        return mongoTemplate.find(query, User.class, "users",
                ReadPreference.nearest());
    }

    /**
     * 强制从 Primary 读取（实时数据）
     */
    public User findUserFromPrimary(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return mongoTemplate.findOne(query, User.class, "users",
                ReadPreference.primary());
    }
}
```

---

## 9.4 高可用特性

### 9.4.1 自动故障转移

MongoDB 副本集的自动故障转移机制确保了服务的高可用性。

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#1E88E5', 'primaryTextColor': '#fff', 'lineColor': '#424242'}}}%%
flowchart TB
    subgraph Step1["Step 1: 正常状态"]
        A[客户端] -->|写入| P[Primary]
        P -->|复制| S1[Secondary 1]
        P -->|复制| S2[Secondary 2]
    end

    subgraph Step2["Step 2: Primary 故障"]
        A -.x|写入失败| P
        S1 -.x|心跳超时| P
        S2 -.x|心跳超时| P
    end

    subgraph Step3["Step 3: 选举新 Primary"]
        S1 -->|发起选举| V1[投票]
        S2 -->|投票| V1
        V1 -->|选举成功| S1[新 Primary]
    end

    subgraph Step4["Step 4: 恢复服务"]
        A -->|重连| S1
        A -->|写入| S1[新 Primary]
        S1 -->|复制| S2
    end

    style A fill:#8E24AA,stroke:#6A1B9A,color:#fff
    style P fill:#1E88E5,stroke:#1565C0,color:#fff
    style S1 fill:#43A047,stroke:#2E7D32,color:#fff
    style S2 fill:#43A047,stroke:#2E7D32,color:#fff
    style V1 fill:#FB8C00,stroke:#E65100,color:#fff
```

**故障转移配置参数：**

```javascript
// 查看当前配置
rs.conf()

// 修改故障转移相关配置
cfg = rs.conf()
cfg.settings = {
    heartbeatTimeoutSecs: 15,        // 心跳超时（默认 10秒）
    electionTimeoutMillis: 20000,     // 选举超时（默认 15秒）
    catchUpTimeoutMillis: 5000       // 追赶超时
}
rs.reconfig(cfg)
```

### 9.4.2 读写分离策略

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#1E88E5', 'primaryTextColor': '#fff', 'lineColor': '#424242'}}}%%
graph LR
    subgraph "读策略"
        A[读操作] --> B{读偏好设置}
        B -->|primary| P[Primary 节点]
        B -->|secondaryPreferred| S[Secondary 节点]
        B -->|nearest| N[最近节点]
    end

    subgraph "写策略"
        C[写操作] --> W{写关注设置}
        W -->|w: 1| W1[单节点确认]
        W -->|w: majority| W2[多数节点确认]
        W -->|w: 3| W3[指定节点数确认]
    end

    style A fill:#8E24AA,stroke:#6A1B9A,color:#fff
    style C fill:#8E24AA,stroke:#6A1B9A,color:#fff
    style B fill:#FB8C00,stroke:#E65100,color:#fff
    style W fill:#FB8C00,stroke:#E65100,color:#fff
    style P fill:#1E88E5,stroke:#1565C0,color:#fff
    style S fill:#43A047,stroke:#2E7D32,color:#fff
    style N fill:#00ACC1,stroke:#00838F,color:#fff
    style W1 fill:#43A047,stroke:#2E7D32,color:#fff
    style W2 fill:#1E88E5,stroke:#1565C0,color:#fff
    style W3 fill:#8E24AA,stroke:#6A1B9A,color:#fff
```

**常见读写分离策略组合：**

| 场景 | 读偏好 | 写关注 | 说明 |
|-----|--------|-------|------|
| 高性能读 | secondaryPreferred | majority | 优先从 Secondary 读，写入需多数确认 |
| 低延迟读 | nearest | majority | 选择最近节点读写 |
| 数据安全 | primary | majority | 所有操作在 Primary，数据最安全 |
| 读写分离 | secondary | 1 | 写 Primary，读 Secondary |

### 9.4.3 写关注（Write Concern）

写关注是客户端确认写入操作的级别，决定了写入操作返回成功前需要确认的节点数量。

```java
package com.example.mongo.service;

import com.example.mongo.entity.Order;
import com.mongodb.WriteConcern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 写关注策略服务
 */
@Service
public class WriteConcernService {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 写入单个节点（默认，快速但不安全）
     */
    public void insertWithAcknowledged(Order order) {
        mongoTemplate.insert(order);
    }

    /**
     * 写入多数节点（推荐，数据安全）
     */
    public void insertWithMajority(Order order) {
        mongoTemplate.setWriteConcern(WriteConcern.MAJORITY);
        mongoTemplate.insert(order);
    }

    /**
     * 写入所有节点（最安全但慢）
     */
    public void insertWithAll(Order order) {
        mongoTemplate.setWriteConcern(WriteConcern.ALL);
        mongoTemplate.insert(order);
    }

    /**
     * 自定义节点数写关注
     */
    public void insertWithCustomW(Order order, int w) {
        mongoTemplate.setWriteConcern(new WriteConcern(w));
        mongoTemplate.insert(order);
    }

    /**
     * 带有超时和写关注的批量插入
     */
    public void insertOrders(List<Order> orders) {
        WriteConcern wc = new WriteConcern(2, 5000);  // w=2, wtimeout=5s
        mongoTemplate.setWriteConcern(wc);
        mongoTemplate.insertAll(orders);
    }
}
```

**写关注级别：**

```yaml
# application.yml 中的写关注配置
spring:
  data:
    mongodb:
      uri: mongodb://192.168.1.101:27017,192.168.1.102:27017/mydb?replicaSet=rs0&w=majority&wtimeout=5000
```

| 写关注 | 说明 | 典型用例 |
|-------|------|---------|
| `w: 1` | 仅 Primary 确认 | 性能优先，允许少量数据丢失 |
| `w: majority` | 多数节点确认 | 平衡性能和数据安全 |
| `w: <number>` | 指定节点数确认 | 特定数据可靠性要求 |
| `w: all` | 所有节点确认 | 最高数据安全 |

### 9.4.4 读偏好（Read Preference）

读偏好决定了从哪个节点读取数据。

```java
package com.example.mongo.service;

import com.example.mongo.entity.Product;
import com.mongodb.ReadPreference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 读偏好策略服务
 */
@Service
public class ReadPreferenceService {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 只从 Primary 读取（默认）
     * 适用场景：实时数据、敏感数据
     */
    public Product findByIdFromPrimary(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return mongoTemplate.findOne(query, Product.class, "products",
                ReadPreference.primary());
    }

    /**
     * 优先从 Secondary，Fallback 到 Primary
     * 适用场景：报表、统计数据（非实时）
     */
    public List<Product> findAllFromSecondaryPreferred() {
        Query query = new Query();
        return mongoTemplate.find(query, Product.class, "products",
                ReadPreference.secondaryPreferred());
    }

    /**
     * 只从 Secondary 读取
     * 适用场景：历史数据查询
     */
    public List<Product> findByCategory(String category) {
        Query query = new Query(Criteria.where("category").is(category));
        return mongoTemplate.find(query, Product.class, "products",
                ReadPreference.secondary());
    }

    /**
     * 从网络延迟最近的节点读取
     * 适用场景：追求低延迟
     */
    public List<Product> findByTag(String tag) {
        Query query = new Query(Criteria.where("tags").in(tag));
        return mongoTemplate.find(query, Product.class, "products",
                ReadPreference.nearest());
    }
}
```

**标签感知读取：**

```javascript
// 副本集配置标签
{
    _id: "rs0",
    members: [
        {
            _id: 0,
            host: "192.168.1.101:27017",
            tags: { "region": "us-east", "disk": "ssd" }
        },
        {
            _id: 1,
            host: "192.168.1.102:27017",
            tags: { "region": "us-west", "disk": "ssd" }
        },
        {
            _id: 2,
            host: "192.168.1.103:27017",
            tags: { "region": "eu", "disk": "hdd" }
        }
    ]
}
```

```java
// 使用标签进行读取
public List<Product> findProductsFromUSEast() {
    // 构建带标签的读偏好
    ReadPreference taggedPref = ReadPreference.secondaryPreferred()
            .withTagSet(new BsonDocument("region", new BsonString("us-east")));

    Query query = new Query();
    return mongoTemplate.find(query, Product.class, "products", taggedPref);
}
```

**读偏好模式对比：**

| 模式 | 说明 | 适用场景 |
|-----|------|---------|
| `primary` | 仅从 Primary 读取 | 实时数据、敏感数据 |
| `primaryPreferred` | 优先 Primary，Fallback 到 Secondary | 优先数据一致性 |
| `secondary` | 仅从 Secondary 读取 | 报表、离线分析 |
| `secondaryPreferred` | 优先 Secondary，Fallback 到 Primary | 负载均衡读（默认推荐） |
| `nearest` | 从延迟最低的节点读取 | 低延迟优先 |

---

## 9.5 综合实战配置

### 9.5.1 完整 Spring Boot 配置

```yaml
# application.yml - 完整副本集配置
spring:
  application:
    name: mongodb-replica-set-demo

  data:
    mongodb:
      # 副本集连接字符串
      uri: mongodb://myuser:mypassword@192.168.1.101:27017,192.168.1.102:27017,192.168.1.103:27017/mydb?replicaSet=rs0&authSource=admin&readPreference=secondaryPreferred&w=majority&wtimeout=5000&connectTimeoutMS=10000&socketTimeoutMS=30000

      # 数据库名称
      database: mydb

  # Jackson 配置
  jackson:
    serialization:
      write-dates-as-timestamps: false
    default-property-inclusion: non_null
```

### 9.5.2 多环境配置

```yaml
# application-dev.yml - 开发环境
spring:
  data:
    mongodb:
      uri: mongodb://192.168.1.101:27017,192.168.1.102:27017,192.168.1.103:27017/mydb_dev?replicaSet=rs0&readPreference=secondaryPreferred

# application-prod.yml - 生产环境
spring:
  data:
    mongodb:
      uri: mongodb://produser:prodpass@192.168.1.101:27017,192.168.1.102:27017,192.168.1.103:27017/mydb_prod?replicaSet=rs0&authSource=admin&readPreference=secondaryPreferred&w=majority&wtimeout=10000
```

### 9.5.3 健康检查配置

```java
package com.example.mongo.config;

import com.mongodb.client.MongoClient;
import org.springframework.boot.actuate.health.MongoDBHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MongoDB 健康检查配置（副本集）
 */
@Configuration
public class MongoHealthConfig {

    @Bean
    public MongoDBHealthIndicator mongoDBHealthIndicator(MongoClient mongoClient) {
        return new MongoDBHealthIndicator(mongoClient) {
            @Override
            protected void doHealthCheck(Health.Builder builder) throws Exception {
                // 执行健康检查
                builder.withDetail("maxBsonObjectSize", mongoClient.getMaxBsonObjectSize())
                       .withDetail("readPreference", "secondaryPreferred");

                // 检查副本集状态
                // ...
            }
        };
    }
}
```

### 9.5.4 事务配置（副本集环境）

```java
package com.example.mongo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MongoDB 事务配置（副本集环境）
 */
@Configuration
@EnableTransactionManagement
public class MongoTransactionConfig {

    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}
```

```java
package com.example.mongo.service;

import com.example.mongo.entity.Order;
import com.example.mongo.entity.Inventory;
import com.example.mongo.repository.OrderRepository;
import com.example.mongo.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 分布式事务示例
 */
@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    /**
     * 创建订单并扣减库存（事务保证）
     * 注意：需要在副本集环境下运行
     */
    @Transactional
    public Order createOrderWithInventory(String orderId, String productId, int quantity) {
        // 创建订单
        Order order = new Order();
        order.setId(orderId);
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setStatus(1);
        order = orderRepository.save(order);

        // 扣减库存
        Inventory inventory = inventoryRepository.findByProductId(productId);
        if (inventory.getStock() < quantity) {
            throw new RuntimeException("库存不足");
        }
        inventory.setStock(inventory.getStock() - quantity);
        inventoryRepository.save(inventory);

        return order;
    }
}
```

---

## 9.6 运维常用命令

### 副本集日常维护

```javascript
// 查看副本集状态
rs.status()

// 查看同步延迟
rs.printSecondaryReplicationInfo()

// 查看配置
rs.conf()

// 查看 oplog 大小和状态
db.getReplicationInfo()

// 强制重新同步某个节点（慎用）
db.adminCommand({ resync: "secondary-host:27017" })

// 重新配置副本集
rs.reconfig(cfg)

// 添加节点
rs.add("192.168.1.104:27017")

// 添加仲裁节点
rs.addArb("192.168.1.105:27017")

// 移除节点
rs.remove("192.168.1.104:27017")

// 转换为仲裁节点
cfg = rs.conf()
cfg.members[0].arbiterOnly = true
rs.reconfig(cfg)

// 修改节点优先级
cfg = rs.conf()
cfg.members[0].priority = 2
rs.reconfig(cfg)

// 阻止选举（维护时使用）
rs.freeze(300)  // 锁定5分钟

// 解锁选举
rs.freeze(0)

// 手动触发选举（测试用）
rs.stepDown()
```

---

## 本章小结

本章介绍了 MongoDB 副本集的核心概念和实战配置：

1. **副本集组成**：Primary、Secondary、Arbiter 三种角色协同工作
2. **数据复制**：通过 Oplog 实现异步增量复制
3. **故障转移**：心跳检测 + 自动选举 + 角色切换
4. **Spring Boot 集成**：连接字符串、配置类、读写分离
5. **高可用特性**：写关注、读偏好、标签感知

副本集是 MongoDB 高可用架构的基础，建议生产环境都采用副本集部署方式。

---

**下章预告**：Chapter 10 将介绍 MongoDB 分片集群，探讨如何通过水平扩展应对海量数据存储需求。
