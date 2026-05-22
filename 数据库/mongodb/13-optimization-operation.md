---
title: 第13章 性能优化与运维
sidebar_label: 第13章
---

# 第13章 性能优化与运维

本章详细介绍 MongoDB 的性能优化与运维管理，包括慢查询分析、索引优化、连接池配置、监控告警以及数据备份恢复等核心内容。

## 13.1 慢查询分析与优化

慢查询是影响 MongoDB 性能的首要因素。通过分析慢查询，可以定位性能瓶颈并进行针对性优化。

### 13.1.1 Profiler 使用

MongoDB Profiler 是一个内置的性能分析工具，可以记录所有执行时间超过阈值的操作。

#### 启用 Profiler

```javascript
// 切换到需要监控的数据库
use myapp

// 启用 Profiler，级别 1 表示只记录慢查询（默认 100ms）
db.setProfilingLevel(1, { slowms: 100 })

// 级别 0：关闭（默认）
// 级别 1：只记录慢查询
// 级别 2：记录所有操作

// 查看当前 Profile 状态
db.getProfilingStatus()
```

#### 查询 Profiler 数据

```javascript
// 查看最近的慢查询记录（默认返回最近 25 条）
db.system.profile.find().pretty()

// 查询执行时间超过 500ms 的查询
db.system.profile.find({ millis: { $gt: 500 } }).pretty()

// 查询特定集合的慢查询
db.system.profile.find({ ns: "myapp.orders" }).pretty()

// 查询特定时间的慢查询
db.system.profile.find({
  ts: {
    $gte: ISODate("2024-01-01T00:00:00Z"),
    $lt: ISODate("2024-01-02T00:00:00Z")
  }
}).pretty()
```

#### Profile 文档结构

```javascript
{
  "op" : "query",                    // 操作类型：query/insert/update/remove/command
  "ns" : "myapp.orders",            // 完整的命名空间（数据库.集合）
  "command" : {                     // 命令详情
    "find" : "orders",
    "filter" : { "status" : "pending", "createdAt" : { "$gt" : ISODate("...") } }
  },
  "cursorid" : 123456789012345,     // 游标 ID
  "nreturned" : 100,                // 返回文档数量
  "nscanned" : 5000,                // 扫描文档数量
  "nscannedObjects" : 5000,         // 扫描对象数量
  "keyUpdates" : 0,                 // 键更新数量
  "writeConflicts" : 0,              // 写冲突数量
  "numYield" : 0,                    // 让步次数
  "locks" : {                        // 锁信息
    "Global" : { "acquireCount" : { "r" : NumberLong(2) } },
    "Database" : { "acquireCount" : { "r" : NumberLong(1) } },
    "Collection" : { "acquireCount" : { "r" : NumberLong(1) } }
  },
  "millis" : 1500,                  // 执行时间（毫秒）
  "execStats" : {                    // 执行统计
    "stage" : "COLLSCAN",           // 扫描类型：COLLSCAN（全集合扫描）/IXSCAN（索引扫描）
    "nReturned" : 100,
    "executionTimeMillis" : 1500,
    "totalKeysExamined" : 0,
    "totalDocsExamined" : 5000
  },
  "ts" : ISODate("2024-01-15T10:30:00.000Z"),  // 操作时间戳
  "client" : "192.168.1.100",       // 客户端 IP
  "user" : "admin"                  // 认证用户
}
```

### 13.1.2 日志分析

MongoDB 日志文件默认位于 `/var/log/mongodb/mongod.log`（Linux）或 `C:\data\log\mongod.log`（Windows）。

#### 常用日志分析命令

```bash
# Linux 系统查看慢查询日志
grep -E "slow query|command:" /var/log/mongodb/mongod.log | tail -100

# Windows 系统使用 PowerShell
Select-String -Path "C:\data\log\mongod.log" -Pattern "slow query" | Select-Object -Last 100

# 分析索引未命中的查询
grep "COLLSCAN" /var/log/mongodb/mongod.log | head -50

# 统计各类操作数量
grep -E "insert|update|remove|query" /var/log/mongodb/mongod.log | awk '{print $NF}' | sort | uniq -c
```

#### 日志级别配置

```javascript
// 在 mongod.conf 中配置日志
systemLog:
  destination: file
  path: /var/log/mongodb/mongod.log
  logAppend: true
  logLevel: 0  # 0-5，数字越小越详细
```

| 日志级别 | 说明 |
|---------|------|
| 0 | Emergency（紧急）|
| 1 | Alert（警报）|
| 2 | Critical（严重）|
| 3 | Error（错误）|
| 4 | Warning（警告）|
| 5 | Notice（通知）|
| 6 | Informational（信息）|
| 7 | Debug（调试）|

### 13.1.3 常见慢查询原因

#### 1. 全集合扫描（COLLSCAN）

```javascript
// 问题：查询 orders 表中 status 为 "completed" 的订单
db.orders.find({ status: "completed" })

// 执行计划显示使用 COLLSCAN（应避免）
{
  "stage" : "COLLSCAN",
  "totalDocsExamined" : 1000000,
  "nReturned" : 5000
}

// 优化：创建索引
db.orders.createIndex({ status: 1 })
db.orders.createIndex({ status: 1, createdAt: -1 })
```

#### 2. 索引选择不当

```javascript
// 问题：存在索引 {a:1, b:1, c:1} 但查询条件只有 b 和 c
db.collection.find({ b: 1, c: 1 })  // 无法有效利用索引

// 优化：创建合适索引或调整查询顺序
db.collection.createIndex({ b: 1, c: 1 })
```

#### 3. 跳过大量文档

```javascript
// 问题：使用 skip 跳过大量文档
db.orders.find().sort({ createdAt: -1 }).skip(100000).limit(10)

// 优化：使用上一页最后一条记录的 ID 进行分页
db.orders.find({ _id: { $gt: lastSeenId } })
  .sort({ _id: 1 })
  .limit(10)
```

#### 4. 正则表达式前缘匹配

```javascript
// 问题：正则表达式以 ^ 开头但字段无索引，或以 .* 开头
db.users.find({ email: { $regex: "^user@example" } })

// 优化：确保正则表达式可以命中索引前缀
db.users.createIndex({ email: 1 })

// 如果必须使用正则，尽量使用确定前缀的模式
db.users.find({ email: { $regex: "^user@" } })  // 可以使用索引
```

#### 5. $or 查询效率低

```javascript
// 问题：$or 连接多个查询条件
db.orders.find({
  $or: [
    { customerId: "C001" },
    { customerId: "C002" },
    { customerId: "C003" }
  ]
})

// 优化：使用 $in 替代 $or
db.orders.find({ customerId: { $in: ["C001", "C002", "C003"] } })
```

## 13.2 索引优化策略

索引是 MongoDB 性能优化的核心。合理的索引设计可以显著提升查询性能。

### 13.2.1 索引选择性

高选择性的索引能够快速过滤掉大部分文档，只保留少量需要扫描的文档。

```javascript
// 高选择性索引示例：订单 ID
db.orders.createIndex({ orderId: 1 }, { unique: true })
// 唯一索引，选择性极高

// 低选择性索引示例：订单状态
db.orders.createIndex({ status: 1 })
// 只能将结果从 100 万条过滤到约 10 万条（假设 10% 待处理）

// 选择性计算
// 选择性 = 唯一值数量 / 总文档数量
// 越高越好
```

### 13.2.2 复合索引顺序

复合索引的字段顺序至关重要，遵循以下原则：

**原则一：等值查询字段放在前面**

```javascript
// 查询：查找所有状态为 "completed" 的 VIP 客户订单
db.orders.find({ status: "completed", isVip: true })

// 最佳索引顺序：等值字段在前，范围字段在后
db.orders.createIndex({ status: 1, isVip: 1, createdAt: -1 })

// 错误示例：将范围字段放在前面
db.orders.createIndex({ createdAt: -1, status: 1, isVip: 1 })
```

**原则二：高频查询字段优先**

```javascript
// 用户经常按状态筛选，偶尔按日期排序
db.orders.find({ status: "pending" }).sort({ createdAt: -1 })

// 索引设计：查询字段优先，排序列放在最后
db.orders.createIndex({ status: 1, createdAt: -1 })
```

**原则三：避免索引列上使用表达式**

```javascript
// 错误：索引字段使用表达式
db.orders.find({ createdAt: { $gt: new Date() } })  // 可以使用索引

// 错误：在索引字段上进行计算
db.orders.find({ $expr: { $gt: ["$amount", "$budget"] } })  // 无法使用索引

// 正确：保持索引字段原样
```

### 13.2.3 覆盖索引

覆盖索引是指索引包含了查询所需的所有字段，MongoDB 不需要回表查询。

```javascript
// 创建覆盖索引
db.orders.createIndex({ status: 1, customerId: 1, amount: 1 })

// 验证查询是否使用覆盖索引
db.orders.find(
  { status: "completed", customerId: "C001" },
  { amount: 1, _id: 0 }  // 只查询 amount，不返回 _id
).explain("executionStats")

// 期望输出：stage: "PROJECTION_COVERED" 或 IXSCAN + 不需要 fetch
```

### 13.2.4 索引统计信息

```javascript
// 查看集合的所有索引
db.orders.getIndexes()

// 查看索引大小
db.orders.stats().indexSizes

// 查看索引使用统计（需要 profiling 开启）
db.orders.aggregate([
  { $indexStats: { } }
])

// 结果示例
[
  {
    "name" : "status_1_createdAt_-1",
    "key" : { "status" : 1, "createdAt" : -1 },
    "host" : "localhost:27017",
    "accesses" : {
      "ops" : NumberLong(1000),     // 该索引被使用的次数
      "since" : ISODate("2024-01-01T00:00:00Z")  // 统计开始时间
    }
  }
]
```

### 13.2.5 索引管理最佳实践

```javascript
// 1. 定期删除未使用的索引
db.orders aggregate([
  { $indexStats: { } },
  { $match: { "accesses.ops": { $lt: 10 } } }  // 使用次数过少的索引
]).forEach(function(idx) {
  print("Dropping index: " + idx.name);
  db.orders.dropIndex(idx.name);
});

// 2. 避免过多索引（写操作会变慢）
// 建议：每个集合索引数不超过 10 个

// 3. 索引命名规范
db.orders.createIndex(
  { customerId: 1, status: 1, createdAt: -1 },
  { name: "idx_customer_status_date" }
);

// 4. 后台创建索引（避免阻塞）
db.orders.createIndex(
  { heavyField: 1 },
  { background: true }
);

// 5. 重建索引优化（定期执行）
db.orders.reIndex()
```

## 13.3 连接池配置

连接池是 MongoDB 高性能访问的关键组件，合理的配置可以显著提升应用性能。

### 13.3.1 MongoClient 选项

#### Node.js (mongodb 驱动)

```javascript
const { MongoClient } = require('mongodb');

const client = new MongoClient('mongodb://localhost:27017', {
  // 连接池配置
  maxPoolSize: 100,              // 最大连接数，默认 100
  minPoolSize: 10,              // 最小连接数，默认 0
  maxIdleTimeMS: 30000,         // 连接最大空闲时间（毫秒）

  // 连接超时配置
  connectTimeoutMS: 10000,      // 连接超时，默认 10000
  socketTimeoutMS: 45000,        // Socket 超时，默认 0（无限制）

  // 服务器选择配置
  serverSelectionTimeoutMS: 30000,  // 服务器选择超时
  localThresholdMS: 15,          // 本地阈值，用于就近选择服务器

  // 重试配置
  retryWrites: true,            // 启用写操作重试
  retryReads: true,             // 启用读操作重试

  //压缩配置
  compressers: ['snappy', 'zstd'],  // 压缩算法
});

await client.connect();
const db = client.db('myapp');
```

#### Java (MongoDB Java Driver)

```java
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import org.bson.Document;

ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017");

MongoClientSettings settings = MongoClientSettings.builder()
    .applyConnectionString(connectionString)
    .applyToConnectionPoolSettings(builder ->
        builder
            .maxSize(100)                    // 最大连接数
            .minSize(10)                     // 最小连接数
            .maxIdleTime(30, TimeUnit.SECONDS)   // 最大空闲时间
            .maxWaitTime(30, TimeUnit.SECONDS)   // 最大等待时间
            .maxConnectionLifeTime(60, TimeUnit.MINUTES)  // 连接最大生存时间
    )
    .applyToSocketSettings(builder ->
        builder
            .connectTimeout(10, TimeUnit.SECONDS)   // 连接超时
            .readTimeout(30, TimeUnit.SECONDS)       // 读取超时
    )
    .retryWrites(true)
    .retryReads(true)
    .build();

MongoClient mongoClient = MongoClients.create(settings);
MongoDatabase database = mongoClient.getDatabase("myapp");
```

#### Python (PyMongo)

```python
from pymongo import MongoClient
from pymongo.pool import PoolOptions

# 方式一：直接配置
client = MongoClient(
    'mongodb://localhost:27017',
    maxPoolSize=100,              # 最大连接数
    minPoolSize=10,               # 最小连接数
    maxIdleTimeMS=30000,          # 最大空闲时间
    connectTimeoutMS=10000,       # 连接超时
    serverSelectionTimeoutMS=30000,  # 服务器选择超时
    socketTimeoutMS=45000,        # Socket 超时
    retryWrites=True,             # 重试写操作
    retryReads=True,              # 重试读操作
    compressors=['snappy', 'zstd']  # 压缩算法
)

# 方式二：使用 MongoClientSettings
from pymongo.settings import TopologySettings

client = MongoClient()
```

### 13.3.2 连接池大小设置

连接池大小的设置需要综合考虑以下因素：

```javascript
// 查看当前连接状态
db.serverStatus().connections

// 输出示例
{
  "current" : 85,      // 当前活跃连接数
  "available" : 415,   // 可用连接数
  "total" : 500        // 总连接数
}
```

**连接池大小计算公式：**

```
连接池大小 = ((核心数 × 2) + 有效磁盘数)
```

| 应用场景 | 推荐连接池大小 |
|---------|---------------|
| 小型应用（< 10 QPS）| 10-50 |
| 中型应用（10-100 QPS）| 50-200 |
| 大型应用（100-1000 QPS）| 200-500 |
| 超大型应用（> 1000 QPS）| 500-1000+ |

### 13.3.3 超时配置

```javascript
// MongoDB Shell 中的超时配置
db.collection.find({ ... })
  .maxTimeMS(30000)  // 查询最大执行时间 30 秒

// 索引创建超时
db.collection.createIndex(
  { field: 1 },
  { timeoutMs: 60000 }  // 60 秒超时
)

// mongod 启动参数
mongod --operationProfilingTimeoutMillis 10000
```

## 13.4 监控与告警

### 13.4.1 MongoDB 监控工具

#### 1. mongostat（实时监控）

```bash
# 安装后使用
mongostat --host localhost:27017 -n 10 1

# 输出参数说明
# insert/query/update/delete/getmore 命令操作次数
# flushes 刷新次数
# mapped/vsize 内存映射大小
# res 物理内存使用
# locked % 锁定百分比
# idx miss % 索引未命中率
# q/t/r/wawt conf restar 队列/时间相关

# 常用选项
mongostat --host 192.168.1.100:27017 -u admin -p password --authenticationDatabase admin
```

#### 2. mongotop（集合操作时间）

```bash
# 每秒刷新一次
mongotop --host localhost:27017 1

# 输出示例
#                        ns    total    read    write
#     admin.system.namespaces    0ms     0ms     0ms
#     myapp.orders              250ms   180ms   70ms
#     myapp.users               50ms    50ms    0ms
```

#### 3. db.serverStatus()（服务器状态）

```javascript
// 查看完整状态
db.serverStatus()

// 查看关键指标
db.serverStatus().connections      // 连接信息
db.serverStatus().mem              // 内存信息
db.serverStatus().network          // 网络统计
db.serverStatus().opcounters       // 操作计数
db.serverStatus().metrics          // 详细指标
```

#### 4. MongoDB Cloud Monitor（Atlas）

```javascript
// Atlas 提供的监控指标
{
  "systemMetrics": {
    "cpu": { "user": 45.2, "system": 12.3 },
    "memory": { "resident": 8192, "virtual": 16384 }
  },
  "databaseMetrics": {
    "operations": { "insert": 1000, "query": 5000 },
    "latency": { "read": 5.2, "write": 8.1 }
  }
}
```

### 13.4.2 关键指标（QPS、内存、连接数）

#### QPS（每秒查询数）

```javascript
// 计算当前 QPS
var stats = db.serverStatus().opcounters;
sleep(1000);
var stats2 = db.serverStatus().opcounters;

var qps = {
  insert: stats2.insert - stats.insert,
  query: stats2.query - stats.query,
  update: stats2.update - stats.update,
  delete: stats2.delete - stats.delete
};
printjson(qps);
```

#### 内存监控

```javascript
// 查看内存使用情况
db.serverStatus().mem

// 输出
{
  "bits" : 64,
  "resident" : 4096,    // 物理内存使用（MB）
  "virtual" : 8192,     // 虚拟内存使用（MB）
  "mapped" : 4096,      // 映射内存（MB）
  "mappedWithJournal" : 8192
}

// WiredTiger 缓存配置
db.serverStatus().wiredTiger.cache
// 输出
{
  "bytes currently in the cache" : 4294967296,
  "maximum bytes configured" : 10737418240,  // 10GB 配置
  "percentage of maximum bytes used" : 40.0
}
```

#### 连接数监控

```javascript
// 查看连接数详情
db.serverStatus().connections

// 输出
{
  "current" : 85,        // 当前活跃连接
  "available" : 415,    // 可用连接
  "total" : 500,         // 总连接容量
  "executor" : "epoll",  // 连接 executor
  "internal" : 10        // 内部连接数
}

// 查看当前连接列表（需要 admin 权限）
db.adminCommand({ connectionStatus: 1, showConnections: 1 })
```

### 13.4.3 告警阈值设置

#### 推荐告警阈值

| 指标 | 警告阈值 | 严重阈值 | 处理建议 |
|------|---------|---------|---------|
| CPU 使用率 | > 70% | > 85% | 扩容或优化查询 |
| 内存使用率 | > 75% | > 90% | 增加内存或优化缓存 |
| 连接数使用率 | > 70% | > 85% | 增加最大连接数或限流 |
| 磁盘使用率 | > 75% | > 85% | 扩容或清理数据 |
| 慢查询比例 | > 5% | > 10% | 优化索引或查询 |
| 索引未命中率 | > 10% | > 25% | 添加或优化索引 |
| 锁定百分比 | > 30% | > 50% | 优化写操作 |

#### Prometheus 告警规则示例

```yaml
# prometheus-alerts.yml
groups:
  - name: mongodb-alerts
    rules:
      - alert: MongoDBHighCPU
        expr: rate(mongodb_sys_cpu_user{instance="localhost:27017"}[5m]) > 70
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "MongoDB CPU 使用率过高"
          description: "MongoDB CPU 使用率超过 70%，当前值: {{ $value }}%"

      - alert: MongoDBHighMemory
        expr: mongodb_mem_resident / mongodb_mem_max > 0.9
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "MongoDB 内存使用率过高"
          description: "MongoDB 内存使用率超过 90%"

      - alert: MongoDBTooManyConnections
        expr: mongodb_connections_current / mongodb_connections_max > 0.85
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "MongoDB 连接数接近上限"
          description: "连接数使用率超过 85%"

      - alert: MongoDBSlowQueries
        expr: rate(mongodb_opcounters_query{instance="localhost:27017"}[5m]) > 100
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "MongoDB 慢查询过多"
          description: "每秒查询数超过 100"
```

## 13.5 数据备份与恢复

### 13.5.1 mongodump/mongorestore

#### 基本备份

```bash
# 备份整个数据库
mongodump --host localhost:27017 --out /backup/mongodump_$(date +%Y%m%d)

# 备份指定数据库
mongodump --host localhost:27017 --db myapp --out /backup/mongodump_$(date +%Y%m%d)

# 备份指定集合
mongodump --host localhost:27017 --db myapp --collection orders --out /backup/

# 备份到压缩文件
mongodump --host localhost:27017 --db myapp --archive=/backup/myapp.archive --gzip

# 远程备份
mongodump --host 192.168.1.100 --port 27017 --username admin --password password --authenticationDatabase admin --out /backup/
```

#### 基本恢复

```bash
# 恢复整个备份
mongorestore --host localhost:27017 /backup/mongodump_20240115/

# 恢复指定数据库
mongorestore --host localhost:27017 --db myapp /backup/mongodump_20240115/myapp/

# 从压缩文件恢复
mongorestore --host localhost:27017 --archive=/backup/myapp.archive --gzip

# 恢复并覆盖现有数据（--drop 会先删除现有集合）
mongorestore --host localhost:27017 --db myapp --drop /backup/myapp/

# 恢复时忽略索引（加快恢复速度）
mongorestore --host localhost:27017 --db myapp --noIndexRestore /backup/myapp/

# 恢复后重建索引
mongorestore --host localhost:27017 --db myapp --restoreDbUsersAndRoles /backup/myapp/
```

### 13.5.2 全量备份与增量备份

#### 全量备份脚本

```bash
#!/bin/bash
# backup_full.sh

BACKUP_DIR="/backup/mongodb"
DATE=$(date +%Y%m%d_%H%M%S)
MONGODB_HOST="localhost:27017"
MONGODB_USER="admin"
MONGODB_PASS="password"
DATABASE="myapp"

# 创建备份目录
mkdir -p ${BACKUP_DIR}/${DATE}

# 执行全量备份
mongodump \
    --host ${MONGODB_HOST} \
    --username ${MONGODB_USER} \
    --password ${MONGODB_PASS} \
    --authenticationDatabase admin \
    --db ${DATABASE} \
    --out ${BACKUP_DIR}/${DATE} \
    --gzip \
    --oplog

# 创建备份标记文件
echo "Full backup completed at $(date)" > ${BACKUP_DIR}/${DATE}/backup_info.txt

# 删除 7 天前的备份
find ${BACKUP_DIR} -type d -mtime +7 -exec rm -rf {} \;

echo "Full backup completed: ${BACKUP_DIR}/${DATE}"
```

#### 增量备份（使用 Oplog）

```bash
#!/bin/bash
# backup_incremental.sh

BACKUP_DIR="/backup/mongodb/oplog"
DATE=$(date +%Y%m%d_%H%M%S)
MONGODB_HOST="localhost:27017"
MONGODB_USER="admin"
MONGODB_PASS="password"

# 创建 oplog 备份目录
mkdir -p ${BACKUP_DIR}

# 复制 oplog 切片
mongodump \
    --host ${MONGODB_HOST} \
    --username ${MONGODB_USER} \
    --password ${MONGODB_PASS} \
    --authenticationDatabase admin \
    --db local \
    --collection oplog.rs \
    --out ${BACKUP_DIR} \
    --query='{"ts": {"$gte": Timestamp('$(date +%s)', 0)}}}'

# 重命名备份文件
if [ -f "${BACKUP_DIR}/local/oplog.rs.bson" ]; then
    mv ${BACKUP_DIR}/local/oplog.rs.bson ${BACKUP_DIR}/oplog_${DATE}.bson
fi

echo "Incremental backup completed: ${BACKUP_DIR}/oplog_${DATE}.bson"
```

### 13.5.3 恢复流程

#### 全量恢复流程

```bash
# 1. 停止应用程序（避免新的写入）
# systemctl stop myapp

# 2. 检查备份文件完整性
ls -lh /backup/mongodump_20240115/

# 3. 执行全量恢复
mongorestore --host localhost:27017 --drop /backup/mongodump_20240115/myapp/

# 4. 验证数据完整性
mongosh --eval "db.myapp.stats()"
mongosh --eval "db.myapp.countDocuments({})"

# 5. 重启应用程序
# systemctl start myapp
```

#### Point-in-Time 恢复流程

```bash
# 1. 全量恢复
mongorestore --host localhost:27017 --drop /backup/mongodump_20240115/myapp/

# 2. 获取恢复点时间戳
# 假设我们恢复到 2024-01-15 10:30:00
RECOVERY_TIME="2024-01-15T10:30:00"

# 3. 应用 Oplog 增量恢复
mongorestore \
    --host localhost:27017 \
    --db local \
    --collection oplog.rs \
    --oplogReplay \
    --nsFrom "local.oplog.rs" \
    --nsTo "local.oplog.rs" \
    /backup/mongodb/oplog/oplog_20240115.bson

# 4. 使用 mongorestore 的 oplogReplay 功能
mongorestore \
    --host localhost:27017 \
    --oplogReplay \
    --oplogLimit="${RECOVERY_TIME}" \
    /backup/mongodump_20240115/
```

#### 定时备份任务（crontab）

```bash
# 编辑 crontab
crontab -e

# 每天凌晨 2 点执行全量备份
0 2 * * * /backup/scripts/backup_full.sh >> /var/log/mongodb_backup.log 2>&1

# 每小时执行增量备份
0 * * * * /backup/scripts/backup_incremental.sh >> /var/log/mongodb_oplog_backup.log 2>&1

# 每天早上 8 点清理 7 天前的备份
0 8 * * * find /backup/mongodb -type d -mtime +7 -exec rm -rf {} \; >> /var/log/mongodb_cleanup.log 2>&1
```

### 13.5.4 备份恢复最佳实践清单

**备份策略清单：**

- [ ] 每日全量备份，保留至少 30 天
- [ ] 每小时增量备份（使用 Oplog）
- [ ] 备份文件存储在独立存储介质
- [ ] 异地备份重要数据
- [ ] 定期测试备份可恢复性
- [ ] 记录备份元数据（时间、大小、校验和）
- [ ] 加密敏感备份数据
- [ ] 限制备份目录访问权限

**恢复测试清单：**

- [ ] 定期在测试环境恢复备份
- [ ] 验证数据完整性和一致性
- [ ] 测试 Point-in-Time 恢复
- [ ] 记录恢复时间和流程
- [ ] 培训运维人员掌握恢复流程
- [ ] 制定并演练灾难恢复预案

**监控清单：**

- [ ] 监控备份任务执行状态
- [ ] 监控备份存储空间使用
- [ ] 监控备份文件完整性
- [ ] 告警备份失败情况
- [ ] 定期生成备份报告

---

## 附录：常用运维命令速查

```javascript
// 查看集合状态
db.collection.stats()
db.collection.storageSize()
db.collection.totalIndexSize()
db.collection.getIndexes()

// 查看数据库状态
db.stats()
db.serverStatus()

// 查看当前操作
db.currentOp()
db.killOp(opid)

// 分析查询性能
db.collection.find({...}).explain("executionStats")

// 查看慢查询
db.getProfilingStatus()
db.system.profile.find({millis: {$gt: 100}})

// 连接池监控
db.serverStatus().connections
```

```bash
# 常用监控命令
mongostat --host localhost:27017 1
mongotop --host localhost:27017 1

# 常用备份命令
mongodump --host localhost:27017 --db myapp --out /backup/
mongorestore --host localhost:27017 --db myapp /backup/myapp/
```
