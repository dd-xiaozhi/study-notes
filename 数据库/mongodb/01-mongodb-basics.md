---
title: 第1章 MongoDB 概述与安装
sidebar_label: 第1章
---

# 第1章 MongoDB 概述与安装

## 1.1 NoSQL 数据库简介

NoSQL（Not Only SQL）数据库是为了解决传统关系型数据库在处理大规模数据时遇到的瓶颈而诞生的。NoSQL 数据库放弃了 SQL 语言和事务的强一致性保证，转而追求更高的可用性和伸缩性。

### 1.1.1 NoSQL 数据库分类

NoSQL 数据库主要分为以下四类：

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#1E88E5', 'primaryTextColor': '#fff', 'primaryBorderColor': '#1565C0', 'lineColor': '#424242', 'secondaryColor': '#43A047', 'tertiaryColor': '#FB8C00'}}}%%
graph TB
    A["NoSQL 数据库分类"] --> B["键值存储 KV Store"]
    A --> C["文档数据库 Document Store"]
    A --> D["列式数据库 Column Store"]
    A --> E["图数据库 Graph Store"]

    B --> B1["Redis"]
    B --> B2["Memcached"]
    B --> B3["DynamoDB"]

    C --> C1["MongoDB"]
    C --> C2["CouchDB"]
    C --> C3["Elasticsearch"]

    D --> D1["Cassandra"]
    D --> D2["HBase"]
    D --> D3["ClickHouse"]

    E --> E1["Neo4j"]
    E --> E2["OrientDB"]
    E --> E3["TigerGraph"]

    style A fill:#1E88E5,stroke:#1565C0,color:#fff
    style B fill:#42A5F5,stroke:#1E88E5,color:#fff
    style C fill:#43A047,stroke:#2E7D32,color:#fff
    style D fill:#FB8C00,stroke:#EF6C00,color:#fff
    style E fill:#8E24AA,stroke:#6A1B9A,color:#fff
```

#### 1. 键值存储（Key-Value Store）

最简单的一种 NoSQL 数据库，数据以键值对的形式存储。

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#1E88E5', 'primaryTextColor': '#fff', 'lineColor': '#424242'}}}%%
graph LR
    K1["Key: 'user:1001'"] --> V1["Value: {name: '张三', age: 25}"]
    K2["Key: 'user:1002'"] --> V2["Value: {name: '李四', age: 30}"]

    style K1 fill:#1E88E5,stroke:#1565C0,color:#fff
    style K2 fill:#1E88E5,stroke:#1565C0,color:#fff
    style V1 fill:#43A047,stroke:#2E7D32,color:#fff
    style V2 fill:#43A047,stroke:#2E7D32,color:#fff
```

**典型应用场景**：
- 缓存系统
- 会话存储
- 购物车数据

**代表产品**：Redis、Memcached、DynamoDB

#### 2. 文档数据库（Document Store）

以 JSON/BSON 文档形式存储数据，每个文档包含完整的业务数据。

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#43A047', 'primaryTextColor': '#fff', 'lineColor': '#424242'}}}%%
graph LR
    subgraph "文档结构示例"
        D1["{ _id: ObjectId('...'),<br/>name: '张三',<br/>age: 25,<br/>address: {<br/>  city: '北京',<br/>  district: '朝阳区'<br/>}}"]
    end

    style D1 fill:#43A047,stroke:#2E7D32,color:#fff
```

**典型应用场景**：
- 内容管理系统
- 实时分析
- 物联网数据存储

**代表产品**：MongoDB、CouchDB

#### 3. 列式数据库（Column Store）

数据按列存储，适合大规模数据分析场景。

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#FB8C00', 'primaryTextColor': '#fff', 'lineColor': '#424242', 'secondaryColor': '#1E88E5'}}}%%
graph BT
    subgraph "行式存储"
        R1["Row1: id, name, age, city"]
    end
    subgraph "列式存储"
        C1["Column id: 1, 2, 3, 4"]
        C2["Column name: 张三, 李四, 王五, 赵六"]
        C3["Column age: 25, 30, 28, 35"]
        C4["Column city: 北京, 上海, 广州, 深圳"]
    end

    style R1 fill:#FB8C00,stroke:#EF6C00,color:#fff
    style C1 fill:#1E88E5,stroke:#1565C0,color:#fff
    style C2 fill:#1E88E5,stroke:#1565C0,color:#fff
    style C3 fill:#1E88E5,stroke:#1565C0,color:#fff
    style C4 fill:#1E88E5,stroke:#1565C0,color:#fff
```

**典型应用场景**：
- 数据仓库
- 日志分析
- 时间序列数据

**代表产品**：Cassandra、HBase、ClickHouse

#### 4. 图数据库（Graph Store）

以图的结构存储数据，节点和边来表达实体关系。

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#8E24AA', 'primaryTextColor': '#fff', 'lineColor': '#424242'}}}%%
graph LR
    A["用户A"] --"关注"--> B["用户B"]
    B --"关注"--> C["用户C"]
    A --"关注"--> C
    C --"发帖"--> D["帖子1"]

    style A fill:#8E24AA,stroke:#6A1B9A,color:#fff
    style B fill:#8E24AA,stroke:#6A1B9A,color:#fff
    style C fill:#8E24AA,stroke:#6A1B9A,color:#fff
    style D fill:#E53935,stroke:#C62828,color:#fff
```

**典型应用场景**：
- 社交网络
- 推荐系统
- 知识图谱

**代表产品**：Neo4j、OrientDB

### 1.1.2 NoSQL vs 关系型数据库对比

| 特性 | 关系型数据库 | NoSQL 数据库 |
|------|-------------|-------------|
| 数据模型 | 固定的表结构 | 灵活的键值、文档、列式、图结构 |
| 查询语言 | SQL | 各有不同的查询接口 |
| 事务支持 | ACID 强一致性 | 最终一致性或弱一致性 |
| 扩展方式 | 垂直扩展（提升单机性能） | 水平扩展（分布式集群） |
| 适用场景 | 结构化数据、复杂查询 | 海量数据、高并发、灵活 schema |

---

## 1.2 MongoDB 简介与应用场景

### 1.2.1 MongoDB 概述

MongoDB 是由 10gen（现 MongoDB Inc.）开发的面向文档的开源 NoSQL 数据库。它将数据存储为类似 JSON 的 BSON 文档，提供了高性能、高可用性和易扩展的特性。

### 1.2.2 MongoDB 核心特点

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#1E88E5', 'primaryTextColor': '#fff', 'lineColor': '#424242'}}}%%
graph TB
    A["MongoDB 核心特点"] --> B["文档存储"]
    A --> C["动态 Schema"]
    A --> D["高性能"]
    A --> E["高可用"]
    A --> F["易扩展"]

    B --> B1["BSON 格式存储"]
    B --> B2["嵌套文档支持"]

    C --> C1["无需预定义表结构"]
    C --> C2["同一集合文档字段可不同"]

    D --> D1["索引支持"]
    D --> D2["内存映射存储"]
    D --> D3["高效的查询优化器"]

    E --> E1["副本集 Replica Set"]
    E --> E2["自动故障转移"]

    F --> F1["分片集群 Sharded Cluster"]
    F --> F2["水平扩展数据"]

    style A fill:#1E88E5,stroke:#1565C0,color:#fff
    style B fill:#42A5F5,stroke:#1E88E5,color:#fff
    style C fill:#43A047,stroke:#2E7D32,color:#fff
    style D fill:#FB8C00,stroke:#EF6C00,color:#fff
    style E fill:#E53935,stroke:#C62828,color:#fff
    style F fill:#8E24AA,stroke:#6A1B9A,color:#fff
```

#### 1. 文档存储

MongoDB 使用 BSON（Binary JSON）格式存储数据，支持复杂的数据结构：

```javascript
// MongoDB 文档示例
{
  "_id": ObjectId("507f1f77bcf86cd799439011"),
  "username": "zhangsan",
  "profile": {
    "name": "张三",
    "age": 28,
    "hobbies": ["篮球", "游泳", "编程"],
    "contact": {
      "email": "zhangsan@example.com",
      "phone": "13800138000"
    }
  },
  "created_at": ISODate("2024-01-15T10:30:00Z")
}
```

#### 2. 动态 Schema（灵活的数据模型）

```javascript
// 同一个集合中可以存储不同结构的文档
db.users.insertOne({ name: "张三", age: 25 })
db.users.insertOne({ name: "李四", address: "北京市朝阳区" })
db.users.insertOne({ name: "王五", isActive: true })
```

#### 3. 高性能

- 支持多种类型的索引（单字段、复合、多键、文本、地理空间等）
- 采用内存映射文件机制
- 高效的查询优化器

#### 4. 高可用 - 副本集（Replica Set）

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#1E88E5', 'primaryTextColor': '#fff', 'lineColor': '#424242'}}}%%
graph
    subgraph "Replica Set 架构"
        P["Primary 主节点<br/>处理所有写操作"]
        S1["Secondary 从节点1<br/>数据复制"]
        S2["Secondary 从节点2<br/>数据复制"]
        A["Arbiter 仲裁节点<br/>仅参与选举"]
    end
    P -->|"同步"| S1
    P -->|"同步"| S2
    S1 -->|"选举投票"| A
    S2 -->|"选举投票"| A

    style P fill:#E53935,stroke:#C62828,color:#fff
    style S1 fill:#43A047,stroke:#2E7D32,color:#fff
    style S2 fill:#43A047,stroke:#2E7D32,color:#fff
    style A fill:#FB8C00,stroke:#EF6C00,color:#fff
```

#### 5. 易扩展 - 分片集群（Sharded Cluster）

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#1E88E5', 'primaryTextColor': '#fff', 'lineColor': '#424242'}}}%%
graph TB
    C["Client 客户端"] --> R["Router 路由节点<br/>mongos"]
    R --> S1["Shard1 分片1<br/>{_id: 0-1000}"]
    R --> S2["Shard2 分片2<br/>{_id: 1001-2000}"]
    R --> S3["Shard3 分片3<br/>{_id: 2001-3000}"]
    R --> C1["Config Server<br/>配置节点"]
    S1 --> C1
    S2 --> C1
    S3 --> C1

    style C fill:#1E88E5,stroke:#1565C0,color:#fff
    style R fill:#8E24AA,stroke:#6A1B9A,color:#fff
    style S1 fill:#43A047,stroke:#2E7D32,color:#fff
    style S2 fill:#43A047,stroke:#2E7D32,color:#fff
    style S3 fill:#43A047,stroke:#2E7D32,color:#fff
    style C1 fill:#FB8C00,stroke:#EF6C00,color:#fff
```

### 1.2.3 MongoDB 应用场景

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#1E88E5', 'primaryTextColor': '#fff', 'lineColor': '#424242', 'rootFontSize': '16'}}}%%
mindmap
  root((MongoDB<br/>应用场景))
    内容管理系统
      文章存储
      用户评论
      标签管理
    移动应用
      用户信息
      地理位置
      消息存储
    物联网 IOT
      传感器数据
      设备状态
      时序数据
    实时分析
      日志收集
      指标统计
      实时 dashboard
    电子商务
      商品目录
      订单处理
      购物车
    社交网络
      用户档案
      好友关系
      动态 Feed
```

---

## 1.3 MongoDB 数据模型

### 1.3.1 核心概念对比

| MongoDB 术语 | SQL 术语 | 说明 |
|-------------|---------|------|
| Database | Database | 数据库 |
| Collection | Table | 表/集合 |
| Document | Row | 行/文档 |
| Field | Column | 列/字段 |
| _id | Primary Key | 主键 |
| Index | Index | 索引 |
| Embedded Document | JOIN | 嵌入式文档/关联 |

### 1.3.2 数据结构层次

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#1E88E5', 'primaryTextColor': '#fff', 'lineColor': '#424242'}}}%%
graph BT
    D["Database 数据库"]
    D --> C1["Collection 集合1<br/>users"]
    D --> C2["Collection 集合2<br/>products"]
    D --> C3["Collection 集合3<br/>orders"]

    C1 --> DO1["Document 文档1"]
    C1 --> DO2["Document 文档2"]
    C1 --> DO3["Document 文档3"]

    DO1 --> F1["field: value"]
    DO1 --> F2["field: value"]
    DO1 --> F3["field: value"]

    style D fill:#1E88E5,stroke:#1565C0,color:#fff
    style C1 fill:#42A5F5,stroke:#1E88E5,color:#fff
    style C2 fill:#42A5F5,stroke:#1E88E5,color:#fff
    style C3 fill:#42A5F5,stroke:#1E88E5,color:#fff
    style DO1 fill:#43A047,stroke:#2E7D32,color:#fff
    style DO2 fill:#43A047,stroke:#2E7D32,color:#fff
    style DO3 fill:#43A047,stroke:#2E7D32,color:#fff
```

### 1.3.3 文档结构详解

```javascript
// 单个文档示例 - 用户信息
{
  "_id": ObjectId("507f1f77bcf86cd799439011"),  // 自动生成的唯一 ID
  "username": "zhangsan",
  "password_hash": "sha256...",                 // 密码（安全存储）
  "email": "zhangsan@example.com",
  "profile": {                                 // 嵌套文档
    "name": "张三",
    "age": 28,
    "gender": "male",
    "avatar_url": "https://example.com/avatar.jpg"
  },
  "hobbies": ["篮球", "编程", "旅游"],          // 数组字段
  "address": [                                  // 数组中嵌套文档
    {
      "type": "home",
      "city": "北京",
      "district": "朝阳区"
    },
    {
      "type": "work",
      "city": "北京",
      "district": "海淀区"
    }
  ],
  "is_active": true,
  "created_at": ISODate("2024-01-15T10:30:00Z"),
  "updated_at": ISODate("2024-01-20T15:45:00Z"),
  "tags": ["VIP", "verified"]                   // 标签数组
}
```

### 1.3.4 集合设计模式

#### 模式 1：嵌入式文档（Embedded Document）

适用于一对少量、经常一起查询的数据：

```javascript
// 用户地址嵌入式设计
{
  "_id": ObjectId("..."),
  "name": "张三",
  "addresses": [
    { "city": "北京", "district": "朝阳区", "street": "建国路" },
    { "city": "上海", "district": "浦东新区", "street": "世纪大道" }
  ]
}

// 查询用户及其地址
db.users.findOne({ "_id": ObjectId("...") })
```

#### 模式 2：引用式文档（Reference Document）

适用于数据独立变化、经常跨集合查询的场景：

```javascript
// 用户集合
{
  "_id": ObjectId("user_id"),
  "name": "张三"
}

// 订单集合（引用用户 ID）
{
  "_id": ObjectId("order_id"),
  "user_id": ObjectId("user_id"),  // 引用用户
  "items": [...],
  "total": 299.00
}
```

### 1.3.5 MongoDB 与 SQL 对比表

| SQL 概念 | MongoDB 命令 | 说明 |
|---------|-------------|------|
| CREATE DATABASE | use database | 切换/创建数据库 |
| CREATE TABLE | db.createCollection() | 创建集合 |
| INSERT INTO | db.collection.insertOne() | 插入单个文档 |
| SELECT * FROM | db.collection.find() | 查询所有文档 |
| WHERE | db.collection.find({field: value}) | 条件查询 |
| SELECT col1,col2 | db.collection.find({}, {col1: 1, col2: 1}) | 投影查询 |
| UPDATE SET | db.collection.updateOne() | 更新文档 |
| DELETE FROM | db.collection.deleteOne() | 删除文档 |
| CREATE INDEX | db.collection.createIndex() | 创建索引 |
| JOIN | db.collection.aggregate([{$lookup: ...}]) | 聚合联接 |

---

## 1.4 Windows 安装 MongoDB（社区版）

### 1.4.1 系统要求

- Windows 10 或更高版本
- 64 位架构
- 至少 2GB 可用磁盘空间
- 4GB 以上 RAM（推荐）

### 1.4.2 安装步骤

#### 方法一：MSI 安装包安装（推荐）

**步骤 1：下载 MongoDB 安装包**

访问 MongoDB 官方下载中心：

```
https://www.mongodb.com/try/download/community
```

选择以下配置：
- Version：最新稳定版（当前 7.0.x）
- Package：MSI
- Platform：Windows x64

**步骤 2：运行安装向导**

1. 双击下载的 `.msi` 文件
2. 在 "Custom Setup" 界面选择 "Complete"（完整安装）
3. 勾选 "Install MongoDB as a Service"（作为服务安装）
4. 勾选 "Install MongoDB Compass"（安装图形化管理工具）

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#1E88E5', 'primaryTextColor': '#fff', 'lineColor': '#424242'}}}%%
graph LR
    A["下载 .msi"] --> B["运行安装向导"]
    B --> C["选择 Complete"]
    C --> D["勾选作为服务安装"]
    D --> E["勾选安装 Compass"]
    E --> F["等待安装完成"]
    F --> G["验证安装"]

    style A fill:#1E88E5,stroke:#1565C0,color:#fff
    style B fill:#42A5F5,stroke:#1E88E5,color:#fff
    style C fill:#43A047,stroke:#2E7D32,color:#fff
    style D fill:#43A047,stroke:#2E7D32,color:#fff
    style E fill:#43A047,stroke:#2E7D32,color:#fff
    style F fill:#FB8C00,stroke:#EF6C00,color:#fff
    style G fill:#43A047,stroke:#2E7D32,color:#fff
```

**步骤 3：配置环境变量**

1. 右键 "此电脑" -> "属性"
2. 点击 "高级系统设置"
3. 点击 "环境变量"
4. 在 "系统变量" 中找到 "Path"，双击编辑
5. 点击 "新建"，添加以下路径：

```
C:\Program Files\MongoDB\Server\7.0\bin
```

**步骤 4：创建数据目录**

MongoDB 默认使用 `C:\data\db` 作为数据存储目录，需要手动创建：

```powershell
# 使用管理员权限打开 PowerShell 或命令提示符
mkdir C:\data\db
```

**步骤 5：启动 MongoDB 服务**

```powershell
# 启动 MongoDB 服务
net start MongoDB

# 或者通过命令行启动（不安装服务的情况）
mongod --dbpath C:\data\db
```

**步骤 6：验证安装**

```powershell
# 检查 MongoDB 版本
mongod --version

# 检查 mongosh 版本
mongosh --version
```

输出示例：
```
mongod version: 7.0.5
db version: 7.0.5
```

#### 方法二：ZIP 压缩包安装（手动）

**步骤 1：下载 ZIP 包**

从 MongoDB 下载中心下载 `.zip` 版本：

```
https://www.mongodb.com/try/download/community
```

选择 Package 为 `ZIP`。

**步骤 2：解压并配置**

```powershell
# 解压到指定目录，例如 D:\mongodb
# 创建以下目录结构：
# D:\mongodb
#   ├── bin
#   │   ├── mongod.exe
#   │   ├── mongos.exe
#   │   └── mongosh.exe
#   ├── data
#   │   └── db
#   └── log
```

**步骤 3：创建配置文件**

在 `D:\mongodb` 目录下创建 `mongod.cfg` 配置文件：

```yaml
# mongod.cfg
systemLog:
  destination: file
  path: D:\mongodb\log\mongod.log

storage:
  dbPath: D:\mongodb\data\db

net:
  port: 27017
  bindIp: 127.0.0.1
```

**步骤 4：配置环境变量**

将 `D:\mongodb\bin` 添加到系统 Path 环境变量。

**步骤 5：注册并启动服务**

```powershell
# 注册服务
"D:\mongodb\bin\mongod.exe" --config "D:\mongodb\mongod.cfg" --install

# 启动服务
net start MongoDB
```

### 1.4.3 Linux 安装 MongoDB（Ubuntu/Debian）

```bash
# 1. 导入 MongoDB 公钥
wget -qO - https://www.mongodb.org/static/pgp/server-7.0.asc | sudo apt-key add -

# 2. 创建 apt 源列表文件
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list

# 3. 更新软件包
sudo apt update

# 4. 安装 MongoDB
sudo apt-get install -y mongodb-org

# 5. 启动 MongoDB
sudo systemctl start mongod

# 6. 设置开机自启
sudo systemctl enable mongod

# 7. 检查服务状态
sudo systemctl status mongod

# 8. 连接测试
mongosh
```

### 1.4.4 Linux 安装 MongoDB（CentOS/RHEL）

```bash
# 1. 创建 yum 源文件
cat <<EOF | sudo tee /etc/yum.repos.d/mongodb-org-7.0.repo
[mongodb-org-7.0]
name=MongoDB Repository
baseurl=https://repo.mongodb.org/yum/redhat/\$releasever/mongodb-org/7.0/x86_64/
gpgcheck=1
enabled=1
gpgkey=https://www.mongodb.org/static/pgp/server-7.0.asc
EOF

# 2. 安装 MongoDB
sudo yum install -y mongodb-org

# 3. 启动 MongoDB
sudo systemctl start mongod

# 4. 设置开机自启
sudo systemctl enable mongod

# 5. 检查状态
sudo systemctl status mongod
```

---

## 1.5 MongoDB Compass GUI 工具使用

### 1.5.1 MongoDB Compass 简介

MongoDB Compass 是 MongoDB 官方提供的图形化管理工具，可以直观地查看、查询、修改数据库数据。

**下载地址**：`https://www.mongodb.com/products/compass`

### 1.5.2 Compass 主界面

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#1E88E5', 'primaryTextColor': '#fff', 'lineColor': '#424242'}}}%%
graph BT
    A["MongoDB Compass 主界面"]
    A --> B["侧边栏<br/>数据库/集合列表"]
    A --> C["工具栏<br/>查询/聚合/索引"]
    A --> D["文档视图<br/>数据展示区域"]
    A --> E["状态栏<br/>连接信息"]

    style A fill:#1E88E5,stroke:#1565C0,color:#fff
    style B fill:#42A5F5,stroke:#1E88E5,color:#fff
    style C fill:#43A047,stroke:#2E7D32,color:#fff
    style D fill:#FB8C00,stroke:#EF6C00,color:#fff
    style E fill:#8E24AA,stroke:#6A1B9A,color:#fff
```

### 1.5.3 连接 MongoDB

1. 打开 MongoDB Compass
2. 在连接界面输入连接字符串：

```
mongodb://localhost:27017
```

3. 点击 "Connect" 按钮

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#1E88E5', 'primaryTextColor': '#fff', 'lineColor': '#424242'}}}%%
graph LR
    A["Compass 启动"] --> B["输入连接字符串"]
    B --> C["mongodb://localhost:27017"]
    C --> D["点击 Connect"]
    D --> E{"连接成功?"}
    E -->|"是"| F["进入主界面"]
    E -->|"否"| G["检查 MongoDB 服务状态"]
    G --> B

    style A fill:#1E88E5,stroke:#1565C0,color:#fff
    style B fill:#42A5F5,stroke:#1E88E5,color:#fff
    style C fill:#43A047,stroke:#2E7D32,color:#fff
    style D fill:#43A047,stroke:#2E7D32,color:#fff
    style E fill:#FB8C00,stroke:#EF6C00,color:#fff
    style F fill:#43A047,stroke:#2E7D32,color:#fff
    style G fill:#E53935,stroke:#C62828,color:#fff
```

### 1.5.4 Compass 界面功能说明

#### 1. 数据库列表视图

```
┌─────────────────────────────────────────────────────┐
│  CONNECT                              [连接状态指示灯] │
├──────────────┬──────────────────────────────────────┤
│              │                                      │
│  DATABASE    │   [数据库名称: myapp]                 │
│  ├─myapp     │                                      │
│  │ ├─users   │   ┌────────────────────────────────┐ │
│  │ ├─products│   │ Schema Analysis                 │ │
│  │ └─orders  │   │ 集合统计、索引信息              │ │
│  └─admin     │   └────────────────────────────────┘ │
│              │                                      │
│  COLLECTIONS │   ┌────────────────────────────────┐ │
│  [刷新按钮]   │   │ Documents                      │ │
│              │   │ [{ "_id": "...", "name": ... }]│ │
│              │   │                                │ │
│              │   └────────────────────────────────┘ │
└──────────────┴──────────────────────────────────────┘
```

#### 2. 文档查看与编辑

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#1E88E5', 'primaryTextColor': '#fff', 'lineColor': '#424242'}}}%%
graph LR
    A["选择集合"] --> B["查看文档列表"]
    B --> C{"操作类型"}
    C -->|"查看"| D["点击文档查看详情"]
    C -->|"新增"| E["点击 Add Data"]
    C -->|"编辑"| F["双击文档进入编辑模式"]
    C -->|"删除"| G["选择文档点击删除"]
    C -->|"导入"| H["Import JSON/CSV"]
    C -->|"导出"| I["Export JSON/CSV"]

    style A fill:#1E88E5,stroke:#1565C0,color:#fff
    style B fill:#42A5F5,stroke:#1E88E5,color:#fff
    style C fill:#FB8C00,stroke:#EF6C00,color:#fff
    style D fill:#43A047,stroke:#2E7D32,color:#fff
    style E fill:#43A047,stroke:#2E7D32,color:#fff
    style F fill:#43A047,stroke:#2E7D32,color:#fff
    style G fill:#E53935,stroke:#C62828,color:#fff
    style H fill:#8E24AA,stroke:#6A1B9A,color:#fff
    style I fill:#8E24AA,stroke:#6A1B9A,color:#fff
```

#### 3. 查询构建器

```javascript
// 使用查询过滤器
{
  "age": { "$gte": 18, "$lte": 30 },
  "city": "北京",
  "is_active": true
}

// 等效的 SQL
SELECT * FROM users
WHERE age >= 18 AND age <= 30
  AND city = '北京'
  AND is_active = true
```

**操作符说明**：

| 操作符 | 说明 | 示例 |
|-------|------|------|
| `$eq` | 等于 | `{ age: 25 }` |
| `$ne` | 不等于 | `{ age: { $ne: 25 } }` |
| `$gt` | 大于 | `{ age: { $gt: 18 } }` |
| `$gte` | 大于等于 | `{ age: { $gte: 18 } }` |
| `$lt` | 小于 | `{ age: { $lt: 65 } }` |
| `$lte` | 小于等于 | `{ age: { $lte: 65 } }` |
| `$in` | 匹配数组中的任一值 | `{ city: { $in: ["北京", "上海"] } }` |
| `$nin` | 不匹配数组中的任何值 | `{ city: { $nin: ["北京"] } }` |
| `$and` | 逻辑与 | `{ $and: [{ age: { $gt: 18 } }, { city: "北京" }] }` |
| `$or` | 逻辑或 | `{ $or: [{ age: { $lt: 18 } }, { city: "上海" }] }` |

### 1.5.5 聚合管道构建器

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#1E88E5', 'primaryTextColor': '#fff', 'lineColor': '#424242'}}}%%
graph LR
    A["聚合管道"] --> B["$match<br/>筛选阶段"]
    A --> C["$group<br/>分组阶段"]
    A --> D["$sort<br/>排序阶段"]
    A --> E["$project<br/>投影阶段"]
    A --> F["$limit<br/>限制阶段"]
    A --> G["$lookup<br/>联接阶段"]

    style A fill:#1E88E5,stroke:#1565C0,color:#fff
    style B fill:#E53935,stroke:#C62828,color:#fff
    style C fill:#FB8C00,stroke:#EF6C00,color:#fff
    style D fill:#43A047,stroke:#2E7D32,color:#fff
    style E fill:#8E24AA,stroke:#6A1B9A,color:#fff
    style F fill:#00ACC1,stroke:#00838F,color:#fff
    style G fill:#1E88E5,stroke:#1565C0,color:#fff
```

**聚合示例**：统计每个城市的用户数量

```javascript
// 聚合管道
[
  {
    "$group": {
      "_id": "$city",
      "count": { "$sum": 1 }
    }
  },
  {
    "$sort": { "count": -1 }
  }
]
```

### 1.5.6 索引管理

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#1E88E5', 'primaryTextColor': '#fff', 'lineColor': '#424242'}}}%%
graph TB
    A["索引管理"] --> B["查看索引"]
    A --> C["创建索引"]
    A --> D["删除索引"]
    A --> E["分析索引使用"]

    B --> B1["列出所有索引及其字段"]
    C --> C1["单字段索引"]
    C --> C2["复合索引"]
    C --> C3["多键索引"]
    C --> C4["文本索引"]

    style A fill:#1E88E5,stroke:#1565C0,color:#fff
    style B fill:#42A5F5,stroke:#1E88E5,color:#fff
    style C fill:#43A047,stroke:#2E7D32,color:#fff
    style D fill:#E53935,stroke:#C62828,color:#fff
    style E fill:#FB8C00,stroke:#EF6C00,color:#fff
```

**创建索引示例**：

```javascript
// 在 users 集合的 email 字段上创建唯一索引
db.users.createIndex(
  { "email": 1 },
  { unique: true, name: "idx_email_unique" }
)
```

---

## 1.6 MongoDB Shell (mongosh) 基础命令

### 1.6.1 mongosh 简介

mongosh 是 MongoDB 官方提供的交互式 JavaScript Shell，用于连接和操作 MongoDB 数据库。

### 1.6.2 连接 MongoDB

```bash
# 连接本地 MongoDB
mongosh

# 连接指定数据库
mongosh myapp

# 带认证连接
mongosh "mongodb://username:password@localhost:27017/myapp?authSource=admin"

# 连接远程 MongoDB
mongosh "mongodb://远程IP:27017/database_name"
```

### 1.6.3 数据库操作

```javascript
// 查看所有数据库
show dbs

// 切换/创建数据库
use myapp

// 查看当前数据库
db

// 查看当前数据库中的所有集合
show collections

// 删除当前数据库（谨慎使用！）
db.dropDatabase()
```

### 1.6.4 集合操作

```javascript
// 创建集合
db.createCollection("users")

// 删除集合
db.users.drop()

// 重命名集合
db.users.renameCollection("customers")
```

### 1.6.5 CRUD 操作

#### 插入文档

```javascript
// 插入单条文档
db.users.insertOne({
  name: "张三",
  age: 28,
  email: "zhangsan@example.com",
  is_active: true
})

// 插入多条文档
db.users.insertMany([
  { name: "李四", age: 25, email: "lisi@example.com" },
  { name: "王五", age: 30, email: "wangwu@example.com" },
  { name: "赵六", age: 22, email: "zhaoliu@example.com" }
])

// insertOne 返回结果
{
  acknowledged: true,
  insertedId: ObjectId("...")
}
```

#### 查询文档

```javascript
// 查询所有文档
db.users.find()

// 美化输出格式
db.users.find().pretty()

// 查询单条文档
db.users.findOne({ name: "张三" })

// 条件查询
db.users.find({ age: { $gte: 25 } })

// 多条件查询（AND）
db.users.find({
  age: { $gte: 25 },
  city: "北京"
})

// 多条件查询（OR）
db.users.find({
  $or: [
    { age: { $lt: 20 } },
    { age: { $gt: 40 } }
  ]
})

// 查询特定字段（投影）
db.users.find(
  { status: "active" },
  { name: 1, email: 1, _id: 0 }
)

// 排序
db.users.find().sort({ age: 1 })     // 按 age 升序
db.users.find().sort({ age: -1 })    // 按 age 降序

// 分页查询
db.users.find().skip(10).limit(5)    // 跳过前10条，返回5条

// 统计数量
db.users.countDocuments({ age: { $gte: 18 } })

// 判断集合是否为空
db.users.countDocuments({ _id: { $exists: true } }) > 0
```

#### 更新文档

```javascript
// 更新单条文档（updateOne）
db.users.updateOne(
  { name: "张三" },
  {
    $set: { age: 30 },
    $currentDate: { updated_at: true }
  }
)

// 更新多条文档（updateMany）
db.users.updateMany(
  { status: "inactive" },
  { $set: { status: "archived" } }
)

// 替换整个文档（replaceOne）
db.users.replaceOne(
  { name: "张三" },
  { name: "张三", age: 35, city: "上海" }
)

// 使用 $inc 递增/递减
db.users.updateOne(
  { name: "李四" },
  { $inc: { age: 1 } }  // age 加 1
)

// 使用 $push 添加数组元素
db.users.updateOne(
  { name: "王五" },
  { $push: { hobbies: "游泳" } }
)

// 使用 $addToSet 添加唯一数组元素
db.users.updateOne(
  { name: "王五" },
  { $addToSet: { hobbies: "篮球" } }
)

// 使用 $pull 删除数组元素
db.users.updateOne(
  { name: "王五" },
  { $pull: { hobbies: "篮球" } }
)
```

#### 删除文档

```javascript
// 删除单条文档
db.users.deleteOne({ name: "张三" })

// 删除多条文档
db.users.deleteMany({ status: "deleted" })

// 删除集合中的所有文档（慎用！）
db.users.deleteMany({})

// 删除操作返回结果
{
  acknowledged: true,
  deletedCount: 5
}
```

### 1.6.6 索引操作

```javascript
// 创建单字段索引
db.users.createIndex({ email: 1 })

// 创建复合索引
db.users.createIndex({ city: 1, age: -1 })

// 创建唯一索引
db.users.createIndex({ username: 1 }, { unique: true })

// 创建文本索引
db.articles.createIndex({ content: "text", title: "text" })

// 查看集合的所有索引
db.users.getIndexes()

// 查看索引大小
db.users.totalIndexSize()

// 删除指定索引
db.users.dropIndex("email_1")

// 删除所有索引（除了 _id）
db.users.dropIndexes()
```

### 1.6.7 聚合操作

```javascript
// 聚合管道示例：统计每个城市的用户数量
db.users.aggregate([
  {
    $group: {
      _id: "$city",
      count: { $sum: 1 },
      avg_age: { $avg: "$age" }
    }
  },
  {
    $sort: { count: -1 }
  },
  {
    $limit: 5
  }
])

// $match - 筛选
db.orders.aggregate([
  { $match: { status: "completed" } }
])

// $project - 投影
db.users.aggregate([
  {
    $project: {
      name: 1,
      email: 1,
      is_adult: { $gte: ["$age", 18] }
    }
  }
])

// $lookup - 左外连接
db.orders.aggregate([
  {
    $lookup: {
      from: "users",
      localField: "user_id",
      foreignField: "_id",
      as: "user_info"
    }
  }
])
```

### 1.6.8 实用命令

```javascript
// 查看 MongoDB 版本
db.version()

// 查看服务器状态
db.serverStatus()

// 查看当前数据库状态
db.stats()

// 查看集合统计信息
db.users.stats()

// 查看当前操作（类似 show processlist）
db.currentOp()

// 终止长时间运行的操作
db.killOp(opid)

// 验证集合数据
db.users.validate()

// 查看帮助
db.help()
db.users.find().help()
```

### 1.6.9 mongosh 常用快捷键

| 快捷键 | 功能 |
|-------|------|
| `↑ / ↓` | 浏览历史命令 |
| `Tab` | 自动补全 |
| `Ctrl + A` | 移动到行首 |
| `Ctrl + E` | 移动到行尾 |
| `Ctrl + L` | 清屏 |
| `Ctrl + C` | 取消当前输入 |

### 1.6.10 示例：完整的数据操作流程

```javascript
// ============ 完整示例 ============

// 1. 创建数据库和集合
use shop
db.createCollection("products")
db.createCollection("orders")

// 2. 插入商品数据
db.products.insertMany([
  { name: "iPhone 15", price: 6999, category: "手机", stock: 100 },
  { name: "MacBook Pro", price: 12999, category: "电脑", stock: 50 },
  { name: "AirPods Pro", price: 1899, category: "配件", stock: 200 },
  { name: "iPad Air", price: 4799, category: "平板", stock: 80 },
  { name: "Apple Watch", price: 2999, category: "手表", stock: 120 }
])

// 3. 查询数据
// 3.1 查询所有商品
db.products.find().pretty()

// 3.2 查询价格小于 5000 的商品
db.products.find({ price: { $lt: 5000 } })

// 3.3 查询手机和电脑分类的商品
db.products.find({ category: { $in: ["手机", "电脑"] } })

// 4. 更新数据
// 4.1 iPhone 15 降价到 5999
db.products.updateOne(
  { name: "iPhone 15" },
  { $set: { price: 5999 } }
)

// 4.2 所有配件库存加 50
db.products.updateMany(
  { category: "配件" },
  { $inc: { stock: 50 } }
)

// 5. 聚合查询
// 5.1 统计每个分类的商品数量和平均价格
db.products.aggregate([
  {
    $group: {
      _id: "$category",
      count: { $sum: 1 },
      avg_price: { $avg: "$price" },
      total_stock: { $sum: "$stock" }
    }
  },
  {
    $sort: { avg_price: -1 }
  }
])

// 5.2 查询价格前三的商品
db.products.find().sort({ price: -1 }).limit(3)

// 6. 创建索引
db.products.createIndex({ category: 1, price: -1 })
db.products.createIndex({ name: "text" })

// 7. 删除数据
// 7.1 删除指定商品
db.products.deleteOne({ name: "iPad Air" })

// 8. 查看统计
db.products.stats()
db.products.countDocuments({})
```

---

## 总结

本章我们学习了：

1. **NoSQL 数据库分类**：键值存储、文档数据库、列式数据库、图数据库
2. **MongoDB 特点**：文档存储、动态 Schema、高性能、高可用、易扩展
3. **MongoDB 数据模型**：数据库、集合、文档，与 SQL 概念的对应关系
4. **Windows 安装 MongoDB**：MSI 安装和 ZIP 手动安装两种方式
5. **MongoDB Compass**：图形化管理工具的使用方法
6. **mongosh 基础命令**：CRUD 操作、索引、聚合等核心命令

下一章我们将深入学习 MongoDB 的查询操作和聚合管道。

---

**参考资源**：

- MongoDB 官方文档：https://docs.mongodb.com
- MongoDB University（免费课程）：https://learn.mongodb.com
