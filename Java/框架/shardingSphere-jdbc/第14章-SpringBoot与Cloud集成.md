# 第14章 Spring Boot/Cloud集成

## 14.1 概述

Spring Boot 和 Spring Cloud 已经成为 Java 微服务开发的事实标准，而 ShardingSphere-JDBC 提供了完善的 Spring Boot Starter，使得在 Spring 生态中集成分库分表、读写分离等能力变得轻而易举。本章将详细讲解 ShardingSphere-JDBC 与 Spring Boot 自动配置的深度集成，以及与 Spring Cloud 生态（Feign、Gateway、Nacos、Sentinel）的无缝整合。

通过本章学习，您将掌握：

- Spring Boot 自动配置原理及 ShardingSphere-JDBC Starter 的工作机制
- 熟练使用 `shardingsphere-jdbc-spring-boot-starter` 进行各种配置
- 集成 Spring Cloud Feign 实现服务间调用与数据源透明化
- 集成 Spring Cloud Gateway 实现 API 网关层面的分片路由
- 集成 Nacos 配置中心实现配置的集中管理和动态刷新
- 集成 Sentinel 实现熔断降级和限流保护
- 掌握多数据源配置和注解驱动开发
- 理解 Starter 所有配置项的详细含义
- 解决常见的集成问题

## 14.2 Spring Boot 自动配置原理

### 14.2.1 Spring Boot 自动配置核心概念

Spring Boot 的自动配置（Auto Configuration）是其核心特性之一，它通过 `@EnableAutoConfiguration` 或 `@SpringBootApplication` 注解触发，利用 `spring.factories` 文件和 `@Configuration` 注解的组合，实现了对第三方库组件的自动化注册和配置。

自动配置的核心机制包括：

1. **条件装配**：基于 `@Conditional` 系列注解，只有当特定条件满足时才会注册 Bean
2. **配置属性绑定**：通过 `@ConfigurationProperties` 将配置文件中的属性绑定到配置类
3. **自动注册**：利用 `spring.factories` 或 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件声明需要自动配置的类

### 14.2.2 ShardingSphere-JDBC Starter 自动配置流程

ShardingSphere-JDBC 的 Spring Boot Starter 封装了数据源创建、配置初始化、规则加载等复杂逻辑，使得用户只需关注业务配置而无需关心底层实现。其自动配置流程如下：

```
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot 启动流程                          │
├─────────────────────────────────────────────────────────────────┤
│  1. Spring Boot 应用启动，加载 META-INF/spring.factories         │
│  2. 触发 ShardingSphereAutoConfiguration 自动配置类               │
│  3. 读取 application.yml 中的 shardingsphere 配置项              │
│  4. 创建 DataSource（可能经过装饰器模式包装）                      │
│  5. 初始化 ShardingSphere 元数据和执行引擎                        │
│  6. 注册 ShardingSphere 数据源到 Spring 容器                     │
└─────────────────────────────────────────────────────────────────┘
```

#### 自动配置类结构

ShardingSphere-JDBC 的 Spring Boot Starter 主要包含以下自动配置类：

- **ShardingSphereAutoConfiguration**：主自动配置类，负责数据源创建和规则加载
- **ShardingSphereDataSourceConfiguration**：数据源配置类，处理数据源初始化
- **ShardingSphereAlgorithmConfiguration**：算法配置类，管理各种分片算法的 Bean 注册

这些配置类都通过 `@AutoConfigureAfter` 确保在其他相关配置类之后执行，避免依赖顺序问题。

### 14.2.3 条件装配机制

ShardingSphere 的自动配置使用了多种条件注解来实现精确的组件加载控制：

| 条件注解 | 作用 | 使用场景 |
|----------|------|----------|
| `@ConditionalOnClass` | 當 classpath 中存在指定类时才加载 | 检测 Spring Cloud、Nacos 等依赖是否存在 |
| `@ConditionalOnProperty` | 当配置文件中存在指定属性时才加载 | 根据配置决定是否启用特定功能 |
| `@ConditionalOnBean` | 当容器中存在指定 Bean 时才加载 | 依赖其他组件的间接条件 |
| `@ConditionalOnMissingBean` | 当容器中不存在指定 Bean 时才加载 | 避免覆盖用户自定义 Bean |

例如，ShardingSphere 提供的事务自动配置会使用：

```java
@Configuration
@ConditionalOnClass(DataSourceTransactionManager.class)
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(name = "spring.shardingsphere.transaction.enabled", havingValue = "true", matchIfMissing = true)
public class ShardingSphereTransactionConfiguration {
    // 事务配置
}
```

### 14.2.4 配置属性绑定

ShardingSphere 使用 `@ConfigurationProperties` 实现配置文件的松散绑定（Relaxed Binding），支持多种配置格式：

```java
@ConfigurationProperties(prefix = "spring.shardingsphere")
public class ShardingSphereProperties {
    
    // 支持 spring.shardingsphere.datasource.secondary
    private Map<String, DataSourceProperties> datasource = new HashMap<>();
    
    // 支持 spring.shardingsphere.rules.sharding
    private RulesProperties rules = new RulesProperties();
}
```

这意味着用户可以在 `application.yml` 中使用以下任意格式：

```yaml
spring:
  shardingsphere:
    datasource:
      secondary:          # 驼峰式
        driver-class-name: com.mysql.cj.jdbc.Driver
    rules:
      sharding:           # 短横线式
        tables:
          t_order:
            actual-data-nodes: ds_${0..1}.t_order_${0..15}
```

## 14.3 Spring Boot Starter 使用

### 14.3.1 Maven 依赖配置

在 Spring Boot 项目中使用 ShardingSphere-JDBC，首先需要添加相应的 Starter 依赖。根据功能需求的不同，ShardingSphere 提供了模块化的依赖管理。

#### 基础依赖

对于仅需要分库分表基础功能的 Spring Boot 2.x 项目：

```xml
<dependencies>
    <!-- Spring Boot 父工程管理版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version>
    </parent>

    <dependencies>
        <!-- 核心 Spring Boot Web 依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- ShardingSphere-JDBC Spring Boot Starter -->
        <dependency>
            <groupId>org.apache.shardingsphere</groupId>
            <artifactId>shardingsphere-jdbc-core-spring-boot-starter</artifactId>
            <version>5.4.1</version>
        </dependency>

        <!-- 数据库驱动 -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- 连接池（可选，ShardingSphere 内置默认连接池） -->
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
        </dependency>
    </dependencies>
</dependencies>
```

#### Spring Boot 3.x 兼容版本

对于使用 Spring Boot 3.x 的项目，需要使用适配 Jakarta EE 的版本：

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
    </dependency>

    <dependencies>
        <!-- Spring Boot 3.x 使用 jakarta 命名空间 -->
        <dependency>
            <groupId>org.apache.shardingsphere</groupId>
            <artifactId>shardingsphere-jdbc-core-spring-boot-starter</artifactId>
            <version>5.4.1</version>
        </dependency>

        <!-- MySQL Connector/J 8.x+ 支持 Jakarta -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>8.3.0</version>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
</dependencies>
```

#### 可选功能模块依赖

根据业务需求，可以添加以下扩展模块：

```xml
<dependencies>
    <!-- 读写分离支持 -->
    <dependency>
        <groupId>org.apache.shardingsphere</groupId>
        <artifactId>shardingsphere-read-write-splitting-core</artifactId>
        <version>5.4.1</version>
    </dependency>

    <!-- 数据加密支持 -->
    <dependency>
        <groupId>org.apache.shardingsphere</groupId>
        <artifactId>shardingsphere-encrypt-core</artifactId>
        <version>5.4.1</version>
    </dependency>

    <!-- 分布式事务支持 -->
    <dependency>
        <groupId>org.apache.shardingsphere</groupId>
        <artifactId>shardingsphere-transaction-core</artifactId>
        <version>5.4.1</version>
    </dependency>

    <!-- Spring 事务管理器集成 -->
    <dependency>
        <groupId>org.apache.shardingsphere</groupId>
        <artifactId>shardingsphere-transaction-spring-starter</artifactId>
        <version>5.4.1</version>
    </dependency>

    <!-- Spring Boot 3.x 版本使用 -->
    <dependency>
        <groupId>org.apache.shardingsphere</groupId>
        <artifactId>shardingsphere-infra-reload-core</artifactId>
        <version>5.4.1</version>
    </dependency>
</dependencies>
```

### 14.3.2 最小化配置示例

以下是 ShardingSphere-JDBC 与 Spring Boot 集成的最小化配置示例，实现了最简单的分片策略：

#### 项目结构

```
spring-boot-sharding-demo/
├── pom.xml
├── src/main/
│   ├── java/com/example/sharding/
│   │   ├── ShardingApplication.java
│   │   ├── entity/Order.java
│   │   ├── mapper/OrderMapper.java
│   │   └── service/OrderService.java
│   └── resources/
│       ├── application.yml
│       └── schema.sql
```

#### Spring Boot 启动类

```java
package com.example.sharding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ShardingApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShardingApplication.class, args);
    }
}
```

#### application.yml 配置

```yaml
spring:
  application:
    name: sharding-demo

  # ShardingSphere 数据源配置
  shardingsphere:
    datasource:
      # 数据源名称，多个用逗号分隔
      names: ds_0,ds_1
      # 第一个数据源配置
      ds_0:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3306/ds_0?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
        username: root
        password: root123
        hikari:
          minimum-idle: 5
          maximum-pool-size: 20
          connection-timeout: 30000
      # 第二个数据源配置
      ds_1:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3306/ds_1?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
        username: root
        password: root123
        hikari:
          minimum-idle: 5
          maximum-pool-size: 20
          connection-timeout: 30000

    # 分片规则配置
    rules:
      sharding:
        tables:
          # t_order 表的分片配置
          t_order:
            actual-data-nodes: ds_${0..1}.t_order_${0..15}
            table-strategy:
              standard:
                sharding-column: order_id
                sharding-algorithm-name: t_order_inline
            key-generate-strategy:
              column: order_id
              key-generator-name: snowflake
        # 分片算法定义
        sharding-algorithms:
          t_order_inline:
            type: INLINE
            props:
              algorithm-expression: t_order_${order_id % 16}
        # 分布式序列生成器
        key-generators:
          snowflake:
            type: SNOWFLAKE

    # SQL 显示与日志配置
    props:
      sql-show: true
      sql-logging: true

# 服务端口配置
server:
  port: 8080

# MyBatis-Plus 配置（可选）
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.example.sharding.entity
  configuration:
    map-underscore-to-camel-case: true
```

#### 初始化 SQL 脚本

```sql
-- ds_0 数据库
CREATE DATABASE ds_0 DEFAULT CHARACTER SET utf8mb4;
USE ds_0;

CREATE TABLE t_order_0 (order_id BIGINT PRIMARY KEY, user_id INT NOT NULL, 
    amount DECIMAL(10,2), status VARCHAR(50), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE t_order_1 (order_id BIGINT PRIMARY KEY, user_id INT NOT NULL, 
    amount DECIMAL(10,2), status VARCHAR(50), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
-- ... t_order_2 到 t_order_15

-- ds_1 数据库
CREATE DATABASE ds_1 DEFAULT CHARACTER SET utf8mb4;
USE ds_1;

CREATE TABLE t_order_0 (order_id BIGINT PRIMARY KEY, user_id INT NOT NULL, 
    amount DECIMAL(10,2), status VARCHAR(50), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE t_order_1 (order_id BIGINT PRIMARY KEY, user_id INT NOT NULL, 
    amount DECIMAL(10,2), status VARCHAR(50), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
-- ... t_order_2 到 t_order_15
```

### 14.3.3 完整分片配置示例

以下是一个包含多个分片表、读写分离、数据加密的完整配置示例：

```yaml
spring:
  application:
    name: sharding-complex-demo

  shardingsphere:
    # ==================== 数据源配置 ====================
    datasource:
      names: ds_primary,ds_replica_0,ds_replica_1
      
      # 主库（写库）
      ds_primary:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3306/sharding_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
        username: root
        password: root123
        hikari:
          pool-name: HikariPool-Primary
          minimum-idle: 10
          maximum-pool-size: 50
          auto-commit: true
          connection-timeout: 30000
          idle-timeout: 600000
          max-lifetime: 1800000

      # 读库1
      ds_replica_0:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3307/sharding_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
        username: root
        password: root123
        hikari:
          pool-name: HikariPool-Replica0
          minimum-idle: 5
          maximum-pool-size: 30

      # 读库2
      ds_replica_1:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3308/sharding_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
        username: root
        password: root123
        hikari:
          pool-name: HikariPool-Replica1
          minimum-idle: 5
          maximum-pool-size: 30

    # ==================== 规则配置 ====================
    rules:
      # 分片规则
      sharding:
        tables:
          # 订单表：按 order_id 分片
          t_order:
            actual-data-nodes: ds_primary.t_order_${0..15}
            database-strategy:
              standard:
                sharding-column: user_id
                sharding-algorithm-name: order_db_strategy
            table-strategy:
              standard:
                sharding-column: order_id
                sharding-algorithm-name: order_table_strategy
            key-generate-strategy:
              column: order_id
              key-generator-name: snowflake

          # 订单详情表：与订单表绑定（绑定表）
          t_order_item:
            actual-data-nodes: ds_primary.t_order_item_${0..15}
            table-strategy:
              standard:
                sharding-column: order_id
                sharding-algorithm-name: order_table_strategy
            key-generate-strategy:
              column: order_item_id
              key-generator-name: snowflake
            audit-strategy:
              enable: true
              audit-names: sharding_key_audit

          # 用户表：按 user_id 分片到不同数据库
          t_user:
            actual-data-nodes: ds_${0..1}.t_user
            database-strategy:
              standard:
                sharding-column: user_id
                sharding-algorithm-name: user_db_strategy
            table-strategy:
              standard:
                sharding-column: user_id
                sharding-algorithm-name: user_table_strategy
            key-generate-strategy:
              column: user_id
              key-generator-name: snowflake

        # 绑定表关系（消除笛卡尔积）
        binding-tables:
          - t_order,t_order_item

        # 默认数据库策略
        default-database-strategy:
          standard:
            sharding-column: user_id
            sharding-algorithm-name: default_db_strategy

        # 默认表策略
        default-table-strategy:
          standard:
            sharding-column: order_id
            sharding-algorithm-name: default_table_strategy

        # 分片算法定义
        sharding-algorithms:
          # 订单数据库分片算法：user_id % 2
          order_db_strategy:
            type: INLINE
            props:
              algorithm-expression: ds_primary
              # 对于绑定表演示，实际生产按user_id分片
          
          # 订单表分片算法：order_id % 16
          order_table_strategy:
            type: INLINE
            props:
              algorithm-expression: t_order_${order_id % 16}
          
          # 用户数据库分片算法：user_id % 2
          user_db_strategy:
            type: INLINE
            props:
              algorithm-expression: ds_${user_id % 2}
          
          # 用户表分片算法：user_id % 8
          user_table_strategy:
            type: INLINE
            props:
              algorithm-expression: t_user_${user_id % 8}
          
          # 默认数据库策略
          default_db_strategy:
            type: INLINE
            props:
              algorithm-expression: ds_${user_id % 2}
          
          # 默认表策略
          default_table_strategy:
            type: INLINE
            props:
              algorithm-expression: t_order_${order_id % 16}

        # 分布式序列生成器
        key-generators:
          snowflake:
            type: SNOWFLAKE
            props:
              worker-id: 1
              max-vibration-offset: 3

      # 读写分离规则
      read-write-splitting:
        data-sources:
          # 读写分离配置
          read_write_ds:
            type: Static
            props:
              write-data-source-name: ds_primary
              read-data-source-names: ds_replica_0,ds_replica_1
            # 负载均衡策略
            load-balancer-name: round_robin
        
        # 负载均衡器定义
        load-balancers:
          round_robin:
            type: ROUND_ROBIN
          random:
            type: RANDOM
          weight:
            type: WEIGHT
            props:
              ds_replica_0: 1
              ds_replica_1: 2

      # 数据加密规则
      encrypt:
        tables:
          t_user:
            columns:
              # 手机号加密存储
              phone:
                cipher:
                  type: AES
                  props:
                    aes-key-value: 1234567890abcdef
                # 密文检索支持
                assisted-query:
                  type: MD5
                # 分布式序列
                like-query:
                  type: CHAR_DIGEST_LIKE
            query-with-cipher-column: true

    # ==================== 属性配置 ====================
    props:
      # SQL 显示（控制台日志输出）
      sql-show: true
      # SQL 日志记录
      sql-logging: true
      # 最大连接数
      max-connections-size-per-query: 1
      # 查询线程池大小
      executor-size: 16
      # 最大转译 SQL 长度
      max-sharding-sql-count: 1000
      # 是否开启结果归并分组优化
      enable-group-by-re-optimization: true
      # 是否开启内存分组优化
      enable-memory-grouping-optimization: false
      # 是否开启流处理优化
      stream-group-by-enabled: true
      # 审计策略
      audit-strategy:
        enable: true
        audit-names: sharding_key_audit
        block-mix-audit: false

  # ==================== 其他框架配置 ====================
  # MyBatis-Plus 配置
  mybatis-plus:
    mapper-locations: classpath*:/mapper/**/*.xml
    type-aliases-package: com.example.sharding.entity
    configuration:
      map-underscore-to-camel-case: true
      cache-enabled: false
      log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

  # 日志配置
  logging:
    level:
      root: INFO
      org.apache.shardingsphere: DEBUG
      org.apache.shardingsphere.sharding: DEBUG

# 服务端口
server:
  port: 8080
  servlet:
    context-path: /api
```

## 14.4 application.yml 配置详解

### 14.4.1 配置结构总览

ShardingSphere-JDBC 在 Spring Boot 环境中的配置采用层级结构，所有配置都以 `spring.shardingsphere` 为前缀：

```yaml
spring:
  shardingsphere:
    # 数据源配置
    datasource:
      names: ds_0,ds_1
      ds_0:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://...
        username: root
        password: password
        # HikariCP 连接池专用配置
        hikari:
          pool-name: HikariPool-Sharding
          minimum-idle: 5
          maximum-pool-size: 20
          connection-timeout: 30000
          idle-timeout: 600000
          max-lifetime: 1800000
          connection-test-query: SELECT 1
      ds_1:
        # ...

    # 规则配置（核心）
    rules:
      sharding:
        tables:
          t_order:
            # ...
      read-write-splitting:
        # ...
      encrypt:
        # ...
      # 其他规则...

    # 属性配置
    props:
      sql-show: true
```

### 14.4.2 数据源配置详解

数据源配置是 ShardingSphere 的基础，支持配置多个数据源，每个数据源可以独立设置连接池参数。

#### 标准数据源配置

```yaml
spring:
  shardingsphere:
    datasource:
      names: ds_master,ds_slave
      
      ds_master:
        # 数据源类型（可选，默认HikariCP）
        type: com.zaxxer.hikari.HikariDataSource
        # JDBC 驱动类名（可选，从 URL 自动识别）
        driver-class-name: com.mysql.cj.jdbc.Driver
        # 数据库连接 URL（必填）
        jdbc-url: jdbc:mysql://localhost:3306/ds_master?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
        # 用户名（必填）
        username: root
        # 密码（必填）
        password: password123
        # 自动提交（可选，默认true）
        auto-commit: true
        # 连接超时（毫秒）
        connection-timeout: 30000
        # 空闲超时（毫秒）
        idle-timeout: 600000
        # 最大生命周期（毫秒）
        max-lifetime: 1800000
        # 连接池名称
        pool-name: HikariPool-Master
        # 最小空闲连接数
        minimum-idle: 5
        # 最大连接池大小
        maximum-pool-size: 20
        # 连接测试查询
        connection-test-query: SELECT 1
```

#### 使用 DBCP2 连接池

```yaml
spring:
  shardingsphere:
    datasource:
      ds_0:
        type: org.apache.commons.dbcp2.BasicDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://localhost:3306/ds_0
        username: root
        password: password
        initial-size: 5
        max-total: 20
        max-idle: 10
        min-idle: 5
        max-wait-millis: 30000
        validation-query: SELECT 1
        test-on-borrow: true
        test-while-idle: true
```

#### 使用 Druid 连接池

```yaml
spring:
  shardingsphere:
    datasource:
      ds_0:
        type: com.alibaba.druid.pool.DruidDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://localhost:3306/ds_0
        username: root
        password: password
        # Druid 特有配置
        druid:
          initial-size: 5
          min-idle: 5
          max-active: 20
          max-wait: 60000
          time-between-eviction-runs-millis: 60000
          min-evictable-idle-time-millis: 300000
          validation-query: SELECT 1
          test-while-idle: true
          test-on-borrow: false
          test-on-return: false
          pool-prepared-statements: true
          max-pool-prepared-statement-per-connection-size: 20
          filters: stat,wall,log4j2
          connection-properties: druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000
          use-global-data-source-stat: true
```

### 14.4.3 分片规则配置详解

分片规则是 ShardingSphere 的核心功能，配置在 `spring.shardingsphere.rules.sharding` 下：

```yaml
rules:
  sharding:
    # 表分片配置
    tables:
      <逻辑表名>:
        # 实际数据节点
        actual-data-nodes: ds_${0..1}.t_order_${0..15}
        
        # 数据库分片策略
        database-strategy:
          standard:
            sharding-column: user_id
            sharding-algorithm-name: db_algorithm
        
        # 表分片策略
        table-strategy:
          standard:
            sharding-column: order_id
            sharding-algorithm-name: table_algorithm
        
        # 分布式主键生成策略
        key-generate-strategy:
          column: order_id
          key-generator-name: snowflake
        
        # 自动生成键策略
        auto-generate-key-column: order_id
        
        # 审计策略
        audit-strategy:
          enable: true
          audit-names: sharding_key_audit
        
        # 分片 hints 配置
        hint:
          sharding-algorithm:
            type: STANDARD
            props:
              algorithm-class-name: com.example.CustomShardingAlgorithm
    
    # 绑定表列表
    binding-tables:
      - t_order,t_order_item,t_order_detail
    
    # 广播表列表（所有分片都存在相同数据）
    broadcast-tables:
      - t_config,t_dict
    
    # 默认分片策略
    default-database-strategy:
      standard:
        sharding-column: user_id
        sharding-algorithm-name: default_db_strategy
    
    default-table-strategy:
      standard:
        sharding-column: order_id
        sharding-algorithm-name: default_table_strategy
    
    # 分片算法定义
    sharding-algorithms:
      <算法名>:
        type: INLINE
        props:
          algorithm-expression: t_order_${order_id % 16}
    
    # 分布式序列生成器
    key-generators:
      <生成器名>:
        type: SNOWFLAKE
        props:
          worker-id: 1
```

### 14.4.4 读写分离配置详解

```yaml
rules:
  read-write-splitting:
    # 读写分离数据源配置
    data-sources:
      <数据源名>:
        # 类型：Static（静态）或 Dynamic（动态）
        type: Static
        props:
          # 写库数据源名称
          write-data-source-name: ds_master
          # 读库数据源名称列表，逗号分隔
          read-data-source-names: ds_slave_0,ds_slave_1
        # 负载均衡器名称
        load-balancer-name: round_robin
    
    # 动态读写分离（基于数据库自动探测）
    # dynamic:
    #   auto-aware-data-source-name: replica_data_source_names
    #   discovery-refresh-timeout: 60000
    
    # 负载均衡器定义
    load-balancers:
      round_robin:
        type: ROUND_ROBIN
      random:
        type: RANDOM
      weight:
        type: WEIGHT
        props:
          ds_slave_0: 1
          ds_slave_1: 2
```

### 14.4.5 属性配置详解

```yaml
props:
  # ==================== SQL 相关 ====================
  # 是否在控制台输出 SQL（默认false）
  sql-show: true
  # 是否开启 SQL 日志记录（默认false）
  sql-logging: true
  # SQL 日志格式
  sql-logging-format: |
    Logic SQL: %s
    Actual SQL: %s
    Parameter: %s

  # ==================== 执行引擎相关 ====================
  # 最大连接数（每个查询）（默认1）
  max-connections-size-per-query: 1
  # 执行线程池大小（默认CPU核心数）
  executor-size: 16
  # 最大转译 SQL 数量（默认-1，无限制）
  max-sharding-sql-count: 1000
  
  # ==================== 结果归并优化 ====================
  # 是否开启 GROUP BY 重新优化（默认true）
  enable-group-by-re-optimization: true
  # 是否开启内存分组优化（默认false）
  enable-memory-grouping-optimization: false
  # 是否开启流处理（默认true）
  stream-group-by-enabled: true
  # 是否开启并行处理（默认true）
  parallel-group-by-enabled: true
  # 并行处理线程池大小
  parallel-execution-threads: 1
  
  # ==================== 审计相关 ====================
  # 审计策略配置
  audit-strategy:
    # 是否启用审计（默认true）
    enable: true
    # 审计器名称列表
    audit-names: sharding_key_audit
    # 是否阻止混合审计（默认false）
    block-mix-audit: false
  
  # ==================== 事务相关 ====================
  # 事务类型（LOCAL, XA, BASE）
  transaction-type: LOCAL
  # XA 事务管理器类型（Atomikos, Narayana, Bitronix）
  xa-transaction-manager-type: Atomikos
  
  # ==================== 缓存相关 ====================
  # 元数据缓存配置
  metadata-cache-specs:
    - t_order: 65536
  # 表规则缓存配置  
  table-rule-cache-specs:
    - t_order: 16384
  
  # ==================== 其他 ====================
  # 配置重试次数
  configuration-retry-times: 3
  # 是否开启重写优化
  rewrite-optimization-enabled: true
  # 是否开启数据源连接池优化
  connection-pool-enabled: true
  # 最大转译耗时（毫秒）
  max-translation-latency-millis: 30000
```

## 14.5 Spring Cloud Feign 集成

### 14.5.1 Feign 与 ShardingSphere 集成原理

Spring Cloud Feign 是声明式的 HTTP 客户端，它简化了 RESTful API 的调用。在微服务架构中，Feign 通常与 Ribbon（客户端负载均衡）配合使用，实现服务间的高可用调用。

当 Feign 与 ShardingSphere-JDBC 集成时，主要面临以下挑战：

1. **服务发现与数据源路由**：Feign 调用可能触发数据库操作，需要确保路由到正确的分片
2. **分布式事务上下文传播**：跨服务的分布式事务需要通过 Feign 拦截器传递
3. **连接池管理**：Feign HTTP 客户端与 ShardingSphere 数据源连接池需要协调管理

集成架构如下：

```
┌────────────────┐      ┌────────────────┐      ┌────────────────┐
│   Service A    │      │   Service B    │      │   Service C    │
│  ┌───────────┐ │      │  ┌───────────┐ │      │  ┌───────────┐ │
│  │ Feign     │ │      │  │ Feign     │ │      │  │ Feign     │ │
│  │ Client    │ │      │  │ Client    │ │      │  │ Client    │ │
│  └─────┬─────┘ │      │  └─────┬─────┘ │      │  └─────┬─────┘ │
│        │       │      │        │       │      │        │       │
│  ┌─────▼─────┐ │      │  ┌─────▼─────┐ │      │  ┌─────▼─────┐ │
│  │ Ribbon    │ │      │  │ Ribbon    │ │      │  │ Ribbon    │ │
│  │ LoadBalance│ │      │  │ LoadBalance│ │      │  │ LoadBalance│ │
│  └───────────┘ │      │  └───────────┘ │      │  └───────────┘ │
│        │       │      │        │       │      │        │       │
│  ┌─────▼─────┐ │      │  ┌─────▼─────┐ │      │  ┌─────▼─────┐ │
│  │Sharding-  │ │      │  │Sharding-  │ │      │  │Sharding-  │ │
│  │Sphere-JDBC│ │      │  │Sphere-JDBC│ │      │  │Sphere-JDBC│ │
│  └───────────┘ │      │  └───────────┘ │      │  └───────────┘ │
└────────────────┘      └────────────────┘      └────────────────┘
         │                      │                       │
         └──────────────────────┼───────────────────────┘
                                │
                    ┌───────────▼───────────┐
                    │   Nacos / Consul      │
                    │   (Service Registry)  │
                    └───────────────────────┘
```

### 14.5.2 Maven 依赖配置

```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Cloud OpenFeign -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
        <version>3.1.8</version>
    </dependency>

    <!-- Spring Cloud 负载均衡（Feign 默认集成Ribbon） -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        <version>3.1.8</version>
    </dependency>

    <!-- 服务注册与发现（Nacos） -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        <version>2022.0.0.0</version>
    </dependency>

    <!-- ShardingSphere-JDBC -->
    <dependency>
        <groupId>org.apache.shardingsphere</groupId>
        <artifactId>shardingsphere-jdbc-core-spring-boot-starter</artifactId>
        <version>5.4.1</version>
    </dependency>

    <!-- 分布式事务（可选） -->
    <dependency>
        <groupId>org.apache.shardingsphere</groupId>
        <artifactId>shardingsphere-transaction-spring-starter</artifactId>
        <version>5.4.1</version>
    </dependency>

    <!-- MySQL 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- MyBatis-Plus -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
        <version>3.5.5</version>
    </dependency>
</dependencies>
```

### 14.5.3 Feign 拦截器配置

为了在 Feign 调用中正确传播 ShardingSphere 的分片上下文（特别是分片键值），需要配置 Feign 拦截器：

#### 分片上下文传播拦截器

```java
package com.example.feign.config;

import com.example.feign.context.ShardingContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

/**
 * Feign 请求拦截器，用于传播分片上下文
 * 当 Service A 调用 Service B 时，如果 Service A 已经设置了分片路由信息，
 * 需要将这些信息通过 HTTP Header 传递给 Service B
 */
@Component
public class ShardingContextInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // 获取当前线程的分片上下文
        ShardingContext context = ShardingContextHolder.getContext();
        if (context != null) {
            // 将分片键值添加到请求头
            template.header(ShardingConstants.SHARDING_KEY_HEADER, 
                String.valueOf(context.getShardingValue()));
            template.header(ShardingConstants.SHARDING_TYPE_HEADER, 
                context.getShardingType());
            template.header(ShardingConstants.TRANSACTION_ID_HEADER,
                context.getTransactionId());
        }
    }
}
```

#### 分片上下文 holder

```java
package com.example.feign.context;

/**
 * 分片上下文持有器，使用 ThreadLocal 存储分片信息
 */
public class ShardingContextHolder {
    
    private static final ThreadLocal<ShardingContext> CONTEXT = new ThreadLocal<>();
    
    public static void setContext(ShardingContext context) {
        CONTEXT.set(context);
    }
    
    public static ShardingContext getContext() {
        return CONTEXT.get();
    }
    
    public static void clear() {
        CONTEXT.remove();
    }
}
```

#### 服务消费者方拦截器

```java
package com.example.feign.config;

import feign.Feign;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Feign 自动配置，当 classpath 中存在 Feign 类时生效
 */
@Configuration
@ConditionalOnClass(Feign.class)
@Order(100)
public class FeignAutoConfiguration {

    /**
     * 配置日志级别
     */
    @Bean
    public feign.Logger.Level feignLoggerLevel() {
        return feign.Logger.Level.FULL;
    }
}
```

### 14.5.4 Feign 与 ShardingSphere 事务整合

在分布式事务场景下，Feign 调用需要正确传播事务上下文。ShardingSphere 支持通过 TCC、Saga 等模式实现柔性事务。

#### XA 事务模式配置

```yaml
spring:
  shardingsphere:
    # 事务配置
    transaction:
      # 事务类型：LOCAL, XA, BASE
      type: XA
      # XA 事务管理器
      provider-type: Atomikos
      # 最大超时时间（秒）
      timeout-seconds: 300
      
    props:
      # XA 事务相关配置
      xa-transaction:
        # 恢复检查间隔（秒）
        recovery-interval-seconds: 30
        # 最大分支事务数
        max-branch-count: 1000
```

#### 事务传播拦截器

```java
package com.example.feign.config;

import org.apache.shardingsphere.transaction.api.TransactionType;
import org.apache.shardingsphere.transaction.core.TransactionOperationType;
import org.apache.shardingsphere.transaction.core.TransactionContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

/**
 * 事务上下文传播拦截器
 * 确保跨服务的分布式事务上下文正确传播
 */
@Component
public class TransactionContextInterceptor implements RequestInterceptor {

    private static final String TRANSACTION_TYPE_HEADER = "X-Transaction-Type";
    private static final String TRANSACTION_OPERATION_HEADER = "X-Transaction-Operation";

    @Override
    public void apply(RequestTemplate template) {
        // 获取当前事务上下文
        TransactionContext context = TransactionContextHolder.getCurrentContext();
        if (context != null) {
            template.header(TRANSACTION_TYPE_HEADER, context.getTransactionType().name());
            template.header(TRANSACTION_OPERATION_HEADER, 
                context.getOperationType().name());
            template.header("X-Global-Transaction-Id", context.getGlobalTransactionId());
        }
    }
}

/**
 * 事务上下文持有器
 */
class TransactionContextHolder {
    
    private static final ThreadLocal<TransactionContext> CONTEXT = new ThreadLocal<>();
    
    public static TransactionContext getCurrentContext() {
        return CONTEXT.get();
    }
    
    public static void setCurrentContext(TransactionContext context) {
        CONTEXT.set(context);
    }
    
    public static void clear() {
        CONTEXT.remove();
    }
}
```

### 14.5.5 完整集成示例

#### 订单服务接口定义

```java
package com.example.order.api;

import com.example.order.dto.OrderDTO;
import com.example.order.dto.OrderItemDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单服务 Feign 客户端
 */
@FeignClient(name = "order-service", 
             fallback = OrderClientFallback.class,
             configuration = {FeignConfig.class})
public interface OrderClient {

    @GetMapping("/orders/{orderId}")
    OrderDTO getOrder(@PathVariable("orderId") Long orderId);

    @PostMapping("/orders")
    OrderDTO createOrder(@RequestBody OrderDTO order);

    @GetMapping("/orders/user/{userId}")
    List<OrderDTO> getOrdersByUser(@PathVariable("userId") Long userId);

    @GetMapping("/orders/{orderId}/items")
    List<OrderItemDTO> getOrderItems(@PathVariable("orderId") Long orderId);

    @PutMapping("/orders/{orderId}/status")
    void updateOrderStatus(@PathVariable("orderId") Long orderId, 
                           @RequestParam("status") String status);
}

/**
 * 熔断降级实现
 */
@Component
class OrderClientFallback implements OrderClient {
    
    private static final Logger log = LoggerFactory.getLogger(OrderClientFallback.class);

    @Override
    public OrderDTO getOrder(Long orderId) {
        log.warn("Fallback: getOrder({})", orderId);
        return null;
    }

    @Override
    public OrderDTO createOrder(OrderDTO order) {
        log.warn("Fallback: createOrder({})", order);
        throw new ServiceUnavailableException("Order service unavailable");
    }

    @Override
    public List<OrderDTO> getOrdersByUser(Long userId) {
        log.warn("Fallback: getOrdersByUser({})", userId);
        return Collections.emptyList();
    }

    @Override
    public List<OrderItemDTO> getOrderItems(Long orderId) {
        log.warn("Fallback: getOrderItems({})", orderId);
        return Collections.emptyList();
    }

    @Override
    public void updateOrderStatus(Long orderId, String status) {
        log.warn("Fallback: updateOrderStatus({}, {})", orderId, status);
        throw new ServiceUnavailableException("Order service unavailable");
    }
}
```

#### Feign 配置类

```java
package com.example.feign.config;

import feign.*;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Feign 通用配置
 */
@Configuration
public class FeignConfig {

    /**
     * 配置日志级别
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * 配置契约（支持 @SpringQueryMap 等注解）
     */
    @Bean
    Contract springContract() {
        return new Contract.Default();
    }

    /**
     * 配置请求超时时间
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
            5000,  // 连接超时 5秒
            30000  // 读取超时 30秒
        );
    }

    /**
     * 配置重试策略
     */
    @Bean
    public Retryer retryer() {
        // 重试失败最大次数，重试间隔 = min(maxDelay, attempts * period)
        return new Retryer.Default(100, 1000, 3);
    }

    /**
     * 错误解码器
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new ErrorDecoder.Default();
    }
}
```

## 14.6 Spring Cloud Gateway 集成

### 14.6.1 Gateway 与 ShardingSphere 集成场景

Spring Cloud Gateway 是 Spring Cloud 的新一代 API 网关，提供路由、限流、鉴权等功能。在分库分表场景下，Gateway 可以实现：

1. **请求路由分片**：根据请求参数（如 user_id）将请求路由到不同的服务实例
2. **分片键提取**：从请求中提取分片键并传递给下游服务
3. **多数据源统一入口**：作为多个分片数据库的统一访问入口

### 14.6.2 网关分片路由配置

#### Maven 依赖

```xml
<dependencies>
    <!-- Spring Cloud Gateway -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
        <version>3.1.8</version>
    </dependency>

    <!-- Nacos 服务发现 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        <version>2022.0.0.0</version>
    </dependency>

    <!-- Spring Cloud LoadBalancer -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        <version>3.1.8</version>
    </dependency>

    <!-- ShardingSphere JDBC（用于网关层面的路由判断） -->
    <dependency>
        <groupId>org.apache.shardingsphere</groupId>
        <artifactId>shardingsphere-jdbc-core-spring-boot-starter</artifactId>
        <version>5.4.1</version>
    </dependency>

    <!-- Redis（用于限流） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
    </dependency>
</dependencies>
```

#### Gateway 路由配置

```yaml
spring:
  cloud:
    gateway:
      # 路由配置
      routes:
        # 订单服务路由
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
            # 基于请求头的断言
            - Header=X-Tenant-Id, \d+
          filters:
            # 分片键参数提取过滤器
            - name: ShardingKeyExtract
              args:
                paramName: user_id
                headerName: X-User-Id
            # 限流过滤器
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100
                redis-rate-limiter.burstCapacity: 200
                key-resolver: '#{@shardingKeyResolver}'
            # 重试过滤器
            - name: Retry
              args:
                retries: 3
                statuses: BAD_GATEWAY,SERVICE_UNAVAILABLE
            # 熔断过滤器
            - name: CircuitBreaker
              args:
                name: orderCircuitBreaker
                fallbackUri: forward:/fallback/order
        
        # 用户服务路由
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=1

      # 全局过滤器
      default-filters:
        # 请求日志
        - AddRequestHeader=X-Gateway-Time, #{T(java.lang.System).currentTimeMillis()}
        # 统一鉴权
        - TokenAuth

      # Discovery 路由定位器配置
      discovery:
        locator:
          enabled: true
          # 路由表达式：{serviceId}/{**}
          lower-case-service-id: true

  # Nacos 配置
  alibaba:
    cloud:
      nacos:
        discovery:
          server-addr: localhost:8848
          namespace: dev
          group: DEFAULT_GROUP
```

### 14.6.3 分片键提取过滤器

```java
package com.example.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

/**
 * 分片键提取过滤器
 * 从请求参数或 Header 中提取分片键，并传递给下游服务
 */
@Component
public class ShardingKeyExtractGatewayFilterFactory 
    extends AbstractGatewayFilterFactory<ShardingKeyExtractGatewayFilterFactory.Config> {

    public ShardingKeyExtractGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String shardingKey = extractShardingKey(request, config);
            
            if (shardingKey != null) {
                // 将分片键添加到请求头，传递给下游服务
                ServerHttpRequest modifiedRequest = request.mutate()
                    .header(config.getHeaderName(), shardingKey)
                    .build();
                
                return chain.filter(
                    exchange.mutate().request(modifiedRequest).build()
                );
            }
            
            return chain.filter(exchange);
        };
    }

    private String extractShardingKey(ServerHttpRequest request, Config config) {
        // 优先从请求参数提取
        String paramValue = request.getQueryParams().getFirst(config.getParamName());
        if (paramValue != null) {
            return paramValue;
        }
        
        // 其次从 Header 提取
        String headerValue = request.getHeaders().getFirst(config.getHeaderName());
        if (headerValue != null) {
            return headerValue;
        }
        
        // 最后从路径变量提取（需要 PathPattern 配合）
        return null;
    }

    public static class Config {
        private String paramName = "user_id";
        private String headerName = "X-User-Id";

        public String getParamName() {
            return paramName;
        }

        public void setParamName(String paramName) {
            this.paramName = paramName;
        }

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }
    }
}
```

### 14.6.4 自定义负载均衡器（分片感知）

```java
package com.example.gateway.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.loadbalancer.core.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分片感知负载均衡器
 * 根据分片键将请求路由到负责该分片的服务实例
 */
public class ShardingAwareLoadBalancer implements ReactorServiceInstanceLoadBalancer {
    
    private final String serviceId;
    private final ObjectProvider<ServiceInstanceListSupplier> supplierProvider;
    
    // 服务实例分片映射
    private final Map<String, Map<Integer, ServiceInstance>> shardInstanceMap = 
        new ConcurrentHashMap<>();

    public ShardingAwareLoadBalancer(String serviceId,
            ObjectProvider<ServiceInstanceListSupplier> supplierProvider) {
        this.serviceId = serviceId;
        this.supplierProvider = supplierProvider;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        // 从请求中获取分片键
        ShardingKeyHolder holder = getShardingKey(request);
        if (holder == null) {
            // 无分片键，使用默认轮询
            return new RoundRobinLoadBalancer(supplierProvider, serviceId).choose(request);
        }
        
        return supplierProvider.getIfAvailable()
            .get(request)
            .next()
            .map(instances -> {
                // 根据分片键选择目标实例
                ServiceInstance instance = selectInstance(instances, holder);
                return new Response<>(instance);
            });
    }

    private ShardingKeyHolder getShardingKey(Request request) {
        // 从请求属性中获取分片键
        Object attr = request.getContext().get("shardingKey");
        if (attr instanceof ShardingKeyHolder) {
            return (ShardingKeyHolder) attr;
        }
        return null;
    }

    private ServiceInstance selectInstance(List<ServiceInstance> instances, 
            ShardingKeyHolder holder) {
        if (instances.isEmpty()) {
            throw new IllegalStateException("No instances available for " + serviceId);
        }
        
        int shardingValue = holder.getShardingValue();
        
        // 根据分片值哈希选择实例
        int index = Math.abs(shardingValue) % instances.size();
        return instances.get(index);
    }
}

/**
 * 分片键持有器
 */
class ShardingKeyHolder {
    private final String shardingType;
    private final int shardingValue;

    public ShardingKeyHolder(String shardingType, int shardingValue) {
        this.shardingType = shardingType;
        this.shardingValue = shardingValue;
    }

    public String getShardingType() {
        return shardingType;
    }

    public int getShardingValue() {
        return shardingValue;
    }
}
```

## 14.7 Nacos 配置中心集成

### 14.7.1 Nacos 与 ShardingSphere 配置同步原理

Nacos 作为配置中心，可以存储和管理 ShardingSphere 的分片配置，实现配置的集中管理、版本控制和动态刷新。ShardingSphere-JDBC 通过监听 Nacos 配置变更，实现无需重启应用即可更新分片规则。

集成架构：

```
┌─────────────────────────────────────────────────────────────────┐
│                        Nacos 配置中心                            │
├─────────────────────────────────────────────────────────────────┤
│  Namespace: dev                                                 │
│  Group: SHARDINGSPHERE_GROUP                                    │
│  Data ID: sharding-rule.yml                                     │
│                                                                 │
│  内容:                                                          │
│  spring:                                                        │
│    shardingsphere:                                              │
│      datasource:                                                │
│        names: ds_${0..1}                                        │
│        ds_0:                                                    │
│          jdbc-url: jdbc:mysql://...                             │
│      rules:                                                     │
│        sharding:                                                │
│          tables:                                                │
│            t_order:                                             │
│              actual-data-nodes: ds_${0..1}.t_order_${0..15}     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ 配置变更推送
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    ShardingSphere-JDBC                           │
├─────────────────────────────────────────────────────────────────┤
│  1. 应用启动时从 Nacos 加载初始配置                               │
│  2. 注册配置监听器                                               │
│  3. 配置变更时触发动态刷新                                        │
│  4. 更新内存中的分片规则                                         │
└─────────────────────────────────────────────────────────────────┘
```

### 14.7.2 Maven 依赖配置

```xml
<dependencies>
    <!-- Nacos 配置中心客户端 -->
    <dependency>
        <groupId>com.alibaba.boot</groupId>
        <artifactId>nacos-config-spring-boot-starter</artifactId>
        <version>0.2.12</version>
    </dependency>

    <!-- Nacos 服务发现（可选） -->
    <dependency>
        <groupId>com.alibaba.boot</groupId>
        <artifactId>nacos-discovery-spring-boot-starter</artifactId>
        <version>0.2.12</version>
    </dependency>

    <!-- ShardingSphere JDBC -->
    <dependency>
        <groupId>org.apache.shardingsphere</groupId>
        <artifactId>shardingsphere-jdbc-core-spring-boot-starter</artifactId>
        <version>5.4.1</version>
    </dependency>

    <!-- ShardingSphere 注册中心（Nacos 实现） -->
    <dependency>
        <groupId>org.apache.shardingsphere</groupId>
        <artifactId>shardingsphere-registry-nacos</artifactId>
        <version>5.4.1</version>
    </dependency>

    <!-- YAML 处理 -->
    <dependency>
        <groupId>org.yaml</groupId>
        <artifactId>snakeyaml</artifactId>
        <version>2.2</version>
    </dependency>
</dependencies>
```

### 14.7.3 Nacos 配置示例

#### 本地 application.yml（引用远程配置）

```yaml
spring:
  application:
    name: sharding-app
  profiles:
    active: dev
  
  # Nacos 配置中心
  nacos:
    config:
      # Nacos 服务器地址
      server-addr: localhost:8848
      # 命名空间 ID
      namespace: c1a2b3c4-d5e6-f7g8-h9i0-j1k2l3m4n5o6
      # 分组
      group: SHARDINGSPHERE_GROUP
      # 文件扩展名
      file-extension: yaml
      # 编码
      encoding: UTF-8
      # 超时时间（毫秒）
      timeout: 3000
      # 监听轮询时间（毫秒）
      refresh-interval: 1000
      # 开启监听（动态配置）
      refresh-enabled: true
      # 配置中心地址列表（支持集群）
      # server-addr: nacos1:8848,nacos2:8848,nacos3:8848
    discovery:
      enabled: true
      # 注册到 Nacos 的服务名
      cluster-name: DEFAULT
      # 命名空间
      namespace: c1a2b3c4-d5e6-f7g8-h9i0-j1k2l3m4n5o6

  # ShardingSphere 本地配置（可省略，配置在 Nacos）
  shardingsphere:
    # 本地配置可以只指定数据源基本连接信息
    # 规则配置从 Nacos 远程加载
    datasource:
      ds_0:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3306/ds_0
        username: root
        password: root123

# 服务端口
server:
  port: 8080
```

#### Nacos 中的共享配置（Data ID: shared-config.yaml）

```yaml
# 共享配置，可被多个微服务引用
spring:
  shardingsphere:
    # 共享的数据源配置
    datasource:
      ds_common:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        username: root
        password: root123
    
    # 共享的分片算法
    rules:
      sharding:
        sharding-algorithms:
          # 哈希分片算法
          hash_mod:
            type: INLINE
            props:
              algorithm-expression: "ds_${user_id % 2}"
          
          # 日期范围分片
          date_range:
            type: CLASS_BASED
            props:
              strategy: STANDARD
              algorithmClassName: com.example.sharding.algorithm.DateRangeShardingAlgorithm
              props:
                from: 2024-01-01
                to: 2025-12-31
                step: 10000
          
          # 复合分片算法
          complex_mod:
            type: COMPLEX_INLINE
            props:
              algorithm-expression: "ds_${user_id % 2}.t_order_${order_id % 16}"
        
        # 共享的分布式序列生成器
        key-generators:
          snowflake:
            type: SNOWFLAKE
            props:
              worker-id: 1
              max-vibration-offset: 3

# 共享属性配置
    props:
      sql-show: true
      sql-logging: false
      max-connections-size-per-query: 1
```

#### Nacos 中的分片规则配置（Data ID: sharding-rule.yaml）

```yaml
# ShardingSphere 分片规则配置
spring:
  shardingsphere:
    # 数据源配置
    datasource:
      names: ds_0,ds_1
      ds_0:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3306/ds_0?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
        username: root
        password: root123
        hikari:
          minimum-idle: 5
          maximum-pool-size: 20
          connection-timeout: 30000
      ds_1:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3306/ds_1?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
        username: root
        password: root123
        hikari:
          minimum-idle: 5
          maximum-pool-size: 20
          connection-timeout: 30000

    # 分片规则
    rules:
      sharding:
        tables:
          t_order:
            actual-data-nodes: ds_${0..1}.t_order_${0..15}
            table-strategy:
              standard:
                sharding-column: order_id
                sharding-algorithm-name: order_table_inline
            key-generate-strategy:
              column: order_id
              key-generator-name: snowflake
          
          t_order_item:
            actual-data-nodes: ds_${0..1}.t_order_item_${0..15}
            table-strategy:
              standard:
                sharding-column: order_id
                sharding-algorithm-name: order_table_inline
            key-generate-strategy:
              column: order_item_id
              key-generator-name: snowflake
          
          t_user:
            actual-data-nodes: ds_${0..1}.t_user
            database-strategy:
              standard:
                sharding-column: user_id
                sharding-algorithm-name: user_db_inline
            table-strategy:
              standard:
                sharding-column: user_id
                sharding-algorithm-name: user_table_inline
            key-generate-strategy:
              column: user_id
              key-generator-name: snowflake
        
        # 绑定表
        binding-tables:
          - t_order,t_order_item
        
        # 广播表
        broadcast-tables:
          - t_config
        
        # 分片算法
        sharding-algorithms:
          order_table_inline:
            type: INLINE
            props:
              algorithm-expression: t_order_${order_id % 16}
          
          user_db_inline:
            type: INLINE
            props:
              algorithm-expression: ds_${user_id % 2}
          
          user_table_inline:
            type: INLINE
            props:
              algorithm-expression: t_user_${user_id % 8}
        
        # 分布式序列
        key-generators:
          snowflake:
            type: SNOWFLAKE
            props:
              worker-id: 1

    # 属性配置
    props:
      sql-show: true
      max-connections-size-per-query: 1
```

### 14.7.4 动态配置刷新实现

```java
package com.example.nacos.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import org.apache.shardingsphere.infra.config.algorithm.AlgorithmConfiguration;
import org.apache.shardingsphere.infra.config.rule.RuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.ShardingStrategyConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.*;
import java.util.concurrent.Executor;

/**
 * Nacos 配置监听与动态刷新配置
 */
@Configuration
public class NacosShardingConfigRefresh {
    
    @Value("${spring.nacos.config.namespace}")
    private String namespace;
    
    @Value("${spring.nacos.config.group}")
    private String group;
    
    @Value("${spring.nacos.config.server-addr}")
    private String serverAddr;
    
    private ConfigService configService;
    
    @Bean
    @Order(100)
    public ConfigService nacosConfigService() throws NacosException {
        Properties properties = new Properties();
        properties.put("serverAddr", serverAddr);
        properties.put("namespace", namespace);
        
        configService = NacosFactory.createConfigService(properties);
        
        // 监听分片规则配置变更
        String dataId = "sharding-rule.yaml";
        configService.addListener(dataId, group, new Listener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                // 配置变更时触发刷新
                refreshShardingRules(configInfo);
            }
            
            @Override
            public Executor getExecutor() {
                return null;
            }
        });
        
        return configService;
    }
    
    /**
     * 刷新分片规则
     * 注意：ShardingSphere 的动态刷新需要调用特定的 API
     */
    private void refreshShardingRules(String configInfo) {
        // 解析新的配置
        // 实际实现中需要使用 ShardingSphere 的配置更新 API
        // 这里仅作为示例展示刷新流程
        
        System.out.println("Received config update, preparing to refresh...");
        System.out.println("Config content length: " + configInfo.length());
        
        // 通知 ShardingSphere 配置变更
        // 实际调用：
        // ShardingSphereConfigurationManager.refreshRules(parseConfig(configInfo));
    }
}
```

## 14.8 Sentinel 熔断降级集成

### 14.8.1 Sentinel 与 ShardingSphere 集成概述

Sentinel 是阿里巴巴开源的流量控制、熔断降级组件，可以与 ShardingSphere-JDBC 集成实现：

1. **数据源级别熔断**：当某个分片数据源不可用时，自动熔断并切换到其他可用数据源
2. **SQL 级别限流**：对慢 SQL、超大结果集进行限流控制
3. **分片键热点限流**：对高频访问的分片键进行流量控制

集成架构：

```
┌─────────────────────────────────────────────────────────────────┐
│                        Sentinel Dashboard                        │
├─────────────────────────────────────────────────────────────────┤
│  流控规则 | 熔断规则 | 热点规则 | 系统规则                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ 规则推送
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Application with Sentinel                     │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌─────────────────┐                     │
│  │ Sentinel API    │    │ ShardingSphere  │                     │
│  │ Tracer/Feign    │◄──►│ JDBC            │                     │
│  │ Entry           │    │ DataSource      │                     │
│  └─────────────────┘    └─────────────────┘                     │
│           │                       │                              │
│  ┌────────▼────────┐    ┌────────▼────────┐                     │
│  │ DegradeSlot     │    │ Transaction     │                     │
│  │ FlowSlot        │    │ Manager         │                     │
│  └─────────────────┘    └─────────────────┘                     │
└─────────────────────────────────────────────────────────────────┘
```

### 14.8.2 Maven 依赖配置

```xml
<dependencies>
    <!-- Sentinel Core -->
    <dependency>
        <groupId>com.alibaba.csp</groupId>
        <artifactId>sentinel-core</artifactId>
        <version>1.8.6</version>
    </dependency>

    <!-- Sentinel Spring Boot Starter -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
        <version>2022.0.0.0</version>
    </dependency>

    <!-- Sentinel Feign 适配器 -->
    <dependency>
        <groupId>com.alibaba.csp</groupId>
        <artifactId>sentinel-feign-adapter</artifactId>
        <version>1.8.6</version>
    </dependency>

    <!-- Sentinel Dashboard 客户端 -->
    <dependency>
        <groupId>com.alibaba.csp</groupId>
        <artifactId>sentinel-transport-simple-http</artifactId>
        <version>1.8.6</version>
    </dependency>

    <!-- ShardingSphere JDBC -->
    <dependency>
        <groupId>org.apache.shardingsphere</groupId>
        <artifactId>shardingsphere-jdbc-core-spring-boot-starter</artifactId>
        <version>5.4.1</version>
    </dependency>

    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

### 14.8.3 Sentinel 配置详解

#### application.yml 配置

```yaml
spring:
  application:
    name: sharding-sentinel-demo

  cloud:
    # Sentinel 配置
    sentinel:
      # 是否启用（默认true）
      enabled: true
      # Sentinel 控制台地址
      dashboard: localhost:8080
      # 客户端 IP（自动识别）
      client-ip: localhost
      # 客户端端口
      port: 8719
      # 心跳间隔（毫秒）
      heartbeat-interval-ms: 10000
      # 冷启动因子（秒）
      cold-cold-factor: 3
      # HTTP 方法解析
      parse-level: ALL
      
      # 过滤链配置
      filter:
        # URL 模式（Ant 风格）
        url-patterns: /**
        # 是否启用
        enabled: true
        # 全局异常处理
        mediation-only: false
      
      # 削窗器配置
      sliding-window:
        # 窗口大小（毫秒）
        window-size-ms: 1000
        # 采样个数
        sample-count: 2

  shardingsphere:
    datasource:
      names: ds_0,ds_1
      ds_0:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3306/ds_0?useUnicode=true&characterEncoding=utf8
        username: root
        password: root123
      ds_1:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3306/ds_1?useUnicode=true&characterEncoding=utf8
        username: root
        password: root123

    rules:
      sharding:
        tables:
          t_order:
            actual-data-nodes: ds_${0..1}.t_order_${0..15}
            table-strategy:
              standard:
                sharding-column: order_id
                sharding-algorithm-name: t_order_inline
            key-generate-strategy:
              column: order_id
              key-generator-name: snowflake
        sharding-algorithms:
          t_order_inline:
            type: INLINE
            props:
              algorithm-expression: t_order_${order_id % 16}
        key-generators:
          snowflake:
            type: SNOWFLAKE
            props:
              worker-id: 1

    props:
      sql-show: true

server:
  port: 8080

# Sentinel 日志配置
logging:
  level:
    com.alibaba.csp.sentinel: INFO
```

### 14.8.4 熔断降级规则配置

Sentinel 支持多种熔断策略：

#### 熔断器配置类

```java
package com.example.sentinel.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel 熔断降级规则配置
 */
@Configuration
public class SentinelDegradeConfig {

    /**
     * 数据库级别熔断规则
     * 当某个分片数据源出现故障时，自动熔断该数据源的访问
     */
    @PostConstruct
    public void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();
        
        // 订单查询熔断规则
        DegradeRule orderQueryRule = new DegradeRule("order_query")
            // 设置阈值类型：异常比例/异常数/响应时间
            .setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO)
            // 异常比例阈值（0.0 - 1.0）
            .setCount(0.5)
            // 熔断持续时间（秒）
            .setTimeWindow(10)
            // 最小请求数
            .setMinRequestAmount(5)
            // 统计时长（毫秒）
            .setStatIntervalMs(10000)
            // 慢请求比例阈值
            .setSlowRatioThreshold(0.5);
        
        // 订单创建熔断规则
        DegradeRule orderCreateRule = new DegradeRule("order_create")
            .setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_COUNT)
            .setCount(10)
            .setTimeWindow(30)
            .setMinRequestAmount(3)
            .setStatIntervalMs(60000);
        
        // 慢 SQL 熔断规则
        DegradeRule slowSqlRule = new DegradeRule("slow_sql")
            .setGrade(RuleConstant.DEGRADE_GRADE_RT)
            .setCount(1000)  // RT 阈值（毫秒）
            .setTimeWindow(10)
            .setMinRequestAmount(10)
            .setSlowRatioThreshold(0.8);
        
        rules.add(orderQueryRule);
        rules.add(orderCreateRule);
        rules.add(slowSqlRule);
        
        DegradeRuleManager.loadRules(rules);
    }
}
```

#### 流控规则配置类

```java
package com.example.sentinel.config;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel 流控规则配置
 */
@Configuration
public class SentinelFlowConfig {

    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();
        
        // 订单服务流控规则
        FlowRule orderFlowRule = new FlowRule("order_service")
            .setCount(100)  // QPS 阈值
            .setGrade(RuleConstant.FLOW_GRADE_QPS)
            .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT)
            .setStrategy(RuleConstant.STRATEGY_DIRECT)
            .setResourceType(0);
        
        // 基于调用关系的流控（入口流量）
        FlowRule callerFlowRule = new FlowRule("order_service")
            .setCount(50)
            .setGrade(RuleConstant.FLOW_GRADE_QPS)
            .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_WARM_UP)
            .setWarmUpPeriodSec(10)
            .setStrategy(RuleConstant.STRATEGY_CHAIN)
            .setRefResource("user_service");
        
        // 分片键热点流控
        FlowRule hotDataRule = new FlowRule("t_order")
            .setCount(10)
            .setGrade(RuleConstant.FLOW_GRADE_QPS)
            .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_WARM_UP)
            .setWarmUpPeriodSec(30)
            .setResourceType(1);  // 热点参数
        
        rules.add(orderFlowRule);
        rules.add(callerFlowRule);
        rules.add(hotDataRule);
        
        FlowRuleManager.loadRules(rules);
    }
}
```

### 14.8.5 与 ShardingSphere 集成使用示例

```java
package com.example.sentinel.service;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.sentinel.dao.OrderMapper;
import com.example.sentinel.entity.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单服务，结合 Sentinel 熔断和 ShardingSphere 分片
 */
@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 使用 Sentinel 注解保护的分片查询
     * 
     * @param userId 用户ID（分片键）
     * @return 订单列表
     */
    @SentinelResource(
        value = "order_query",
        entryType = EntryType.INWARD,
        blockHandler = "queryBlockHandler",
        fallback = "queryFallback"
    )
    public List<Order> queryByUserId(Long userId) {
        // 创建 Sentinel Entry，关联资源
        try (Entry entry = SphU.entry("order_query", EntryType.INWARD)) {
            // 执行 ShardingSphere 查询
            return orderMapper.selectByUserId(userId);
        } catch (BlockException e) {
            // 触发熔断或限流
            throw new ServiceDegradeException("Service degraded", e);
        } catch (Exception e) {
            // 业务异常也被记录
            Tracer.trace(e);
            throw e;
        }
    }

    /**
     * 热点参数分片键保护
     * 
     * @param orderId 订单ID（分片键）
     * @return 订单详情
     */
    @SentinelResource(
        value = "order_detail",
        entryType = EntryType.INWARD,
        blockHandler = "detailBlockHandler"
    )
    public Order queryByOrderId(Long orderId) {
        return orderMapper.selectByOrderId(orderId);
    }

    /**
     * 创建订单（带熔断保护）
     * 
     * @param order 订单信息
     * @return 成功创建的订单
     */
    @SentinelResource(
        value = "order_create",
        entryType = EntryType.INWARD,
        blockHandler = "createBlockHandler",
        fallback = "createFallback"
    )
    public Order createOrder(Order order) {
        try (Entry entry = SphU.entry("order_create", EntryType.INWARD)) {
            orderMapper.insert(order);
            return order;
        } catch (BlockException e) {
            throw new ServiceDegradeException("Order creation degraded", e);
        }
    }

    // ==================== Block Handler（限流熔断处理）====================

    public List<Order> queryBlockHandler(Long userId, BlockException e) {
        // 限流/熔断时的处理逻辑
        return getDefaultOrders();
    }

    public Order detailBlockHandler(Long orderId, BlockException e) {
        return getDefaultOrder();
    }

    public Order createBlockHandler(Order order, BlockException e) {
        // 降级时返回预定义订单或持久化到本地队列
        return createOfflineOrder(order);
    }

    // ==================== Fallback（业务异常处理）====================

    public List<Order> queryFallback(Long userId, Throwable t) {
        return getDefaultOrders();
    }

    public Order createFallback(Order order, Throwable t) {
        return createOfflineOrder(order);
    }

    // ==================== 辅助方法 ====================

    private List<Order> getDefaultOrders() {
        return List.of();
    }

    private Order getDefaultOrder() {
        return null;
    }

    private Order createOfflineOrder(Order order) {
        // 将订单保存到本地文件或消息队列，等待后续处理
        return order;
    }
}
```

### 14.8.6 Sentinel Dashboard 配置

在实际生产环境中，建议使用 Sentinel Dashboard 进行规则的集中配置和动态推送：

```
┌─────────────────────────────────────────────────────────────────┐
│                    Sentinel Dashboard                            │
├─────────────────────────────────────────────────────────────────┤
│  功能菜单：                                                       │
│  ├─ 实时监控：查看各资源的 QPS、响应时间、通过率等                  │
│  ├─ 簇点链路：查看资源调用链路                                     │
│  ├─ 流控规则：配置限流规则                                        │
│  ├─ 熔断规则：配置熔断策略                                        │
│  ├─ 热点规则：配置热点参数限流                                     │
│  └─ 系统规则：配置系统自适应保护                                   │
│                                                                 │
│  规则推送配置：                                                   │
│  - 使用 Nacos 等配置中心实现规则持久化                             │
│  - 规则变更后自动推送到客户端                                      │
└─────────────────────────────────────────────────────────────────┘
```

## 14.9 多数据源配置

### 14.9.1 多数据源场景分析

在复杂业务场景中，应用可能需要同时访问多个数据源，这些数据源可能：

1. **异构数据库**：MySQL + PostgreSQL + Oracle 等多种数据库
2. **分库分表**：同一逻辑表的多个物理分片
3. **读写分离**：主库写、从库读
4. **混合场景**：以上场景的任意组合

ShardingSphere-JDBC 支持灵活的多数据源配置，满足各种复杂需求。

### 14.9.2 静态多数据源配置

```yaml
spring:
  shardingsphere:
    # ==================== 数据源定义 ====================
    datasource:
      names: ds_master,ds_replica_0,ds_replica_1,ds_pg,ds_oracle
      
      # 主库（MySQL）
      ds_master:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3306/sharding_db?useUnicode=true&characterEncoding=utf8
        username: root
        password: root123
        hikari:
          maximum-pool-size: 20
          minimum-idle: 5
      
      # 从库0（MySQL）
      ds_replica_0:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3307/sharding_db?useUnicode=true&characterEncoding=utf8
        username: root
        password: root123
        hikari:
          maximum-pool-size: 15
          minimum-idle: 3
      
      # 从库1（MySQL）
      ds_replica_1:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3308/sharding_db?useUnicode=true&characterEncoding=utf8
        username: root
        password: root123
        hikari:
          maximum-pool-size: 15
          minimum-idle: 3
      
      # PostgreSQL 数据源
      ds_pg:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: org.postgresql.Driver
        jdbc-url: jdbc:postgresql://localhost:5432/pg_db
        username: postgres
        password: postgres123
        hikari:
          maximum-pool-size: 10
      
      # Oracle 数据源
      ds_oracle:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: oracle.jdbc.OracleDriver
        jdbc-url: jdbc:oracle:thin:@localhost:1521:orcl
        username: system
        password: oracle123
        hikari:
          maximum-pool-size: 10
```

### 14.9.3 动态多数据源切换

在实际应用中，可能需要根据业务逻辑动态切换数据源。ShardingSphere 提供了 DataSource 路由机制。

#### 数据源路由键持有器

```java
package com.example.datasource.context;

/**
 * 数据源路由上下文，使用 ThreadLocal 存储
 */
public class DataSourceRouteContext {
    
    private static final ThreadLocal<String> ROUTE_KEY = new ThreadLocal<>();
    private static final ThreadLocal<String> ROUTE_TYPE = new ThreadLocal<>();
    
    public static void setDataSourceName(String dataSourceName) {
        ROUTE_KEY.set(dataSourceName);
    }
    
    public static String getDataSourceName() {
        return ROUTE_KEY.get();
    }
    
    public static void setRouteType(String routeType) {
        ROUTE_TYPE.set(routeType);
    }
    
    public static String getRouteType() {
        return ROUTE_TYPE.get();
    }
    
    public static void clear() {
        ROUTE_KEY.remove();
        ROUTE_TYPE.remove();
    }
}
```

#### 自定义数据源路由算法

```java
package com.example.datasource.route;

import org.apache.shardingsphere.sharding.api.sharding.DataSourceRangeShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;

import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * 动态数据源路由算法
 * 根据路由键的值选择目标数据源
 */
public class DynamicDataSourceShardingAlgorithm implements DataSourceRangeShardingAlgorithm<String> {
    
    private Properties props;
    
    @Override
    public void init(Properties props) {
        this.props = props;
    }
    
    @Override
    public Collection<String> doSharding(Collection<String> availableDataSourceNames, 
            RangeShardingValue<String> shardingValue) {
        String shardingColumn = shardingValue.getColumnName();
        String shardingValueStr = shardingValue.getValueRange().lowerEndpoint();
        
        // 根据分片值前缀选择数据源
        // 例如：ds_mysql_* -> ds_mysql_0, ds_mysql_1
        //      ds_pg_* -> ds_pg
        return availableDataSourceNames.stream()
            .filter(ds -> ds.startsWith(getPrefix(shardingValueStr)))
            .collect(Collectors.toList());
    }
    
    private String getPrefix(String shardingValue) {
        // 根据值的前缀判断数据源类型
        if (shardingValue.startsWith("PG_")) {
            return "ds_pg";
        } else if (shardingValue.startsWith("ORA_")) {
            return "ds_oracle";
        } else {
            return "ds_master";
        }
    }
    
    @Override
    public String getType() {
        return "DYNAMIC_DATASOURCE";
    }
    
    @Override
    public Properties getProps() {
        return props;
    }
}
```

### 14.9.4 多数据源事务处理

在多数据源场景下，事务处理变得尤为重要。ShardingSphere 支持 XA 分布式事务来处理跨数据源的操作。

```yaml
spring:
  shardingsphere:
    # 事务配置
    transaction:
      # 事务类型：LOCAL, XA, BASE
      type: XA
      # XA 事务管理器提供者
      provider-type: Atomikos
      
    # 数据源配置
    datasource:
      # ... 数据源配置 ...
    
    # 分片规则
    rules:
      sharding:
        tables:
          # ...
```

#### 多数据源事务服务

```java
package com.example.transaction.service;

import org.apache.shardingsphere.transaction.annotation.ShardingTransactionType;
import org.apache.shardingsphere.transaction.api.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 多数据源事务服务示例
 */
@Service
public class MultiDataSourceTransactionService {

    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private AccountMapper accountMapper;

    /**
     * 跨数据源的分布式事务操作
     * 使用 XA 事务保证一致性
     */
    @Transactional
    @ShardingTransactionType(TransactionType.XA)
    public void createOrderWithPayment(Long orderId, Long productId, Long userId, BigDecimal amount) {
        // 1. 在分片表中创建订单
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setAmount(amount);
        order.setStatus("PENDING");
        orderMapper.insert(order);
        
        // 2. 更新产品库存（在另一个数据源）
        productMapper.updateStock(productId, -1);
        
        // 3. 扣减账户余额（在第三个数据源）
        accountMapper.deductBalance(userId, amount);
        
        // 如果任何一步失败，整个事务回滚
    }

    /**
     * 使用 BASE 柔性事务（最终一致性）
     * 适用于对一致性要求不高但需要高可用的场景
     */
    @Transactional
    @ShardingTransactionType(TransactionType.BASE)
    public void processOrderEventually(Long orderId, Long productId) {
        // 异步处理，可以容忍短暂不一致
        orderMapper.updateStatus(orderId, "PROCESSING");
        productMapper.updateStock(productId, -1);
    }
}
```

## 14.10 注解驱动开发

### 14.10.1 ShardingSphere 注解体系

ShardingSphere 提供了丰富的注解支持，使得开发者可以通过注解方式声明分片策略、分布式主键等，而无需在配置文件中定义。

#### 核心注解一览

| 注解 | 位置 | 功能 |
|------|------|------|
| `@ShardingSphereAlgorithm` | 类 | 定义分片算法 Bean |
| `@ShardingAuditor` | 类 | 定义审计策略 |
| `@ShardingKeyGenerator` | 类 | 定义分布式主键生成器 |
| `@ShardingRuleConfiguration` | 类 | 定义分片规则配置 |
| `@PrimaryKey` | 属性 | 标记分布式主键列 |
| `@Sort` | 属性 | 标记分片键（排序） |
| `@Condition` | 方法 | 自定义分片条件 |
| `@Transactional` | 方法 | 分布式事务注解 |

### 14.10.2 分片算法注解

#### 标准分片算法注解

```java
package com.example.sharding.algorithm;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.infra.algorithm.core.annotation.ShardingSphereAlgorithm;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Properties;

/**
 * 基于订单ID的哈希分片算法
 * 使用注解方式声明为 Spring Bean
 */
@Component
@ShardingSphereAlgorithm(name = "orderHashSharding", type = "CLASS_BASED")
public class OrderHashShardingAlgorithm implements PreciseShardingAlgorithm<Long>, RangeShardingAlgorithm<Long> {
    
    private Properties props;
    
    @Override
    public void init(Properties props) {
        this.props = props;
        // 初始化分片数，默认16
    }
    
    @Override
    public String doSharding(Collection<String> availableTargetNames, 
            PreciseShardingValue<Long> shardingValue) {
        Long orderId = shardingValue.getValue();
        String logicTableName = shardingValue.getLogicTableName();
        
        // 哈希分片
        int shardingCount = getShardingCount();
        int index = (int) (orderId % shardingCount);
        
        String suffix = String.format("%02d", index);
        
        return availableTargetNames.stream()
            .filter(name -> name.endsWith(suffix))
            .findFirst()
            .orElse(availableTargetNames.iterator().next());
    }
    
    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, 
            RangeShardingValue<Long> shardingValue) {
        Long lowerEndpoint = shardingValue.getValueRange().lowerEndpoint();
        Long upperEndpoint = shardingValue.getValueRange().upperEndpoint();
        
        // 范围查询，可能需要访问多个分片
        int lowerIndex = (int) (lowerEndpoint % getShardingCount());
        int upperIndex = (int) (upperEndpoint % getShardingCount());
        
        return availableTargetNames.stream()
            .filter(name -> {
                // 过滤在范围内的目标分片
                int index = extractIndex(name);
                return index >= lowerIndex && index <= upperIndex;
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    private int getShardingCount() {
        String count = props.getProperty("sharding-count", "16");
        return Integer.parseInt(count);
    }
    
    private int extractIndex(String targetName) {
        // 从表名提取索引，如 t_order_05 -> 5
        String indexStr = targetName.substring(targetName.lastIndexOf('_') + 1);
        return Integer.parseInt(indexStr);
    }
    
    @Override
    public Properties getProps() {
        return props;
    }
    
    @Override
    public String getType() {
        return "ORDER_HASH";
    }
}
```

#### 复合分片算法注解

```java
package com.example.sharding.algorithm;

import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;
import org.apache.shardingsphere.infra.algorithm.core.annotation.ShardingSphereAlgorithm;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 复合分片算法
 * 同时使用多个分片键进行分片
 */
@Component
@ShardingSphereAlgorithm(name = "complexSharding", type = "CLASS_BASED")
public class OrderComplexShardingAlgorithm implements ComplexKeysShardingAlgorithm<Long> {
    
    private Properties props;

    @Override
    public void init(Properties props) {
        this.props = props;
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, 
            ComplexKeysShardingValue<Long> shardingValue) {
        // 获取所有分片键的值
        Map<String, Collection<Long>> shardingValuesMap = shardingValue.getColumnNameAndValuesMap();
        
        Long userId = getFirstValue(shardingValuesMap, "user_id");
        Long orderId = getFirstValue(shardingValuesMap, "order_id");
        
        if (userId != null && orderId != null) {
            // 同时有 user_id 和 order_id，使用复合策略
            return complexSharding(availableTargetNames, userId, orderId);
        } else if (userId != null) {
            // 只有 user_id
            return shardingByUserId(availableTargetNames, userId);
        } else if (orderId != null) {
            // 只有 order_id
            return shardingByOrderId(availableTargetNames, orderId);
        }
        
        // 默认返回所有分片
        return availableTargetNames;
    }
    
    private Collection<String> complexSharding(Collection<String> available, Long userId, Long orderId) {
        int dbIndex = (int) (userId % 2);
        int tableIndex = (int) (orderId % 16);
        String suffix = String.format("%02d", tableIndex);
        
        return available.stream()
            .filter(name -> name.contains("ds_" + dbIndex) && name.endsWith(suffix))
            .collect(Collectors.toList());
    }
    
    private Collection<String> shardingByUserId(Collection<String> available, Long userId) {
        int dbIndex = (int) (userId % 2);
        return available.stream()
            .filter(name -> name.contains("ds_" + dbIndex))
            .collect(Collectors.toList());
    }
    
    private Collection<String> shardingByOrderId(Collection<String> available, Long orderId) {
        int tableIndex = (int) (orderId % 16);
        String suffix = String.format("%02d", tableIndex);
        return available.stream()
            .filter(name -> name.endsWith(suffix))
            .collect(Collectors.toList());
    }
    
    private Long getFirstValue(Map<String, Collection<Long>> map, String key) {
        Collection<Long> values = map.get(key);
        if (values != null && !values.isEmpty()) {
            return values.iterator().next();
        }
        return null;
    }
    
    @Override
    public Properties getProps() {
        return props;
    }
    
    @Override
    public String getType() {
        return "COMPLEX_SHARDING";
    }
}
```

### 14.10.3 分布式主键生成器注解

```java
package com.example.sharding.keygen;

import org.apache.shardingsphere.sharding.api.keygen.KeyGenerateAlgorithm;
import org.apache.shardingsphere.infra.algorithm.core.annotation.ShardingKeyGenerator;
import org.apache.shardingsphere.sharding.spi.ShardingAlgorithm;

import javaProperties props;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 自定义分布式ID生成器
 * 使用雪花算法变体
 */
@Component
@ShardingKeyGenerator(name = "customSnowflake", type = "SNOWFLAKE")
public class CustomSnowflakeKeyGenerator implements KeyGenerateAlgorithm {
    
    private Properties props;
    
    // 时间戳偏移量
    private static final long EPOCH_OFFSET = 1609459200000L; // 2021-01-01
    private static final long WORKER_ID_BITS = 10;
    private static final long SEQUENCE_BITS = 12;
    
    private final long workerId;
    private long sequence = 0;
    private long lastTimestamp = -1;
    
    // 锁
    private final Object mutex = new Object();
    
    public CustomSnowflakeKeyGenerator() {
        this.props = new Properties();
        this.workerId = 1;
    }
    
    @Override
    public void init(Properties props) {
        this.props = props;
        String workerIdStr = props.getProperty("worker-id", "1");
        this.workerId = Long.parseLong(workerIdStr);
    }
    
    @Override
    public Number generateKey() {
        synchronized (mutex) {
            long timestamp = currentTimeMillis();
            
            if (timestamp < lastTimestamp) {
                // 时钟回拨，等待
                try {
                    mutex.wait(lastTimestamp - timestamp);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                timestamp = currentTimeMillis();
            }
            
            if (timestamp == lastTimestamp) {
                sequence = (sequence + 1) & ((1 << SEQUENCE_BITS) - 1);
                if (sequence == 0) {
                    // 序列耗尽，等待下一毫秒
                    timestamp = waitNextMillis(timestamp);
                }
            } else {
                sequence = 0;
            }
            
            lastTimestamp = timestamp;
            
            return ((timestamp - EPOCH_OFFSET) << (WORKER_ID_BITS + SEQUENCE_BITS))
                    | (workerId << SEQUENCE_BITS)
                    | sequence;
        }
    }
    
    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }
    
    private long waitNextMillis(long timestamp) {
        long next = currentTimeMillis();
        while (next <= timestamp) {
            next = currentTimeMillis();
        }
        return next;
    }
    
    @Override
    public Properties getProps() {
        return props;
    }
    
    @Override
    public String getType() {
        return "CUSTOM_SNOWFLAKE";
    }
}
```

### 14.10.4 审计策略注解

```java
package com.example.sharding.audit;

import org.apache.shardingsphere.infra.algorithm.core.annotation.ShardingAuditor;
import org.apache.shardingsphere.sharding.api.audit.ShardingAuditAlgorithm;

import javaProperties props;
import java.util.Properties;

/**
 * 自定义审计策略
 * 用于检测潜在的分片键冲突或热点访问
 */
@Component
@ShardingAuditor(name = "shardingKeyAudit", type = "CLASS_BASED")
public class ShardingKeyConflictAuditAlgorithm implements ShardingAuditAlgorithm {
    
    private Properties props;
    private int maxShardingKeys;
    
    @Override
    public void init(Properties props) {
        this.props = props;
        String maxKeys = props.getProperty("max-sharding-keys", "10000");
        this.maxShardingKeys = Integer.parseInt(maxKeys);
    }
    
    @Override
    public void check(Collection<String> args) {
        // 审计检查逻辑
        if (args == null || args.isEmpty()) {
            throw new AuditException("Sharding key cannot be empty");
        }
        
        // 检查分片键数量
        if (args.size() > maxShardingKeys) {
            throw new AuditException(
                "Too many sharding keys: " + args.size() + ", max: " + maxShardingKeys);
        }
    }
    
    @Override
    public Properties getProps() {
        return props;
    }
    
    @Override
    public String getType() {
        return "SHARDING_KEY_AUDIT";
    }
}

class AuditException extends RuntimeException {
    public AuditException(String message) {
        super(message);
    }
}
```

### 14.10.5 实体类注解配置

```java
package com.example.sharding.entity;

import org.apache.shardingsphere.sharding.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类 - 展示 ShardingSphere 注解
 */
@ShardingRuleConfiguration(
    // 配置绑定表
    bindingTables = {"t_order_item"},
    // 默认分片策略
    defaultDatabaseStrategy = @Strategy(
        standard = @Standard(
            shardingColumn = "user_id",
            algorithm = @Algorithm(
                type = "INLINE",
                props = @Props(algorithmExpression = "ds_${user_id % 2}")
            )
        )
    ),
    defaultTableStrategy = @Strategy(
        standard = @Standard(
            shardingColumn = "order_id",
            algorithm = @Algorithm(
                type = "INLINE",
                props = @Props(algorithmExpression = "t_order_${order_id % 16}")
            )
        )
    )
)
public class Order {
    
    /**
     * 订单ID - 分布式主键
     */
    @PrimaryKey
    @ShardingKeyGenerator(name = "snowflake")
    private Long orderId;
    
    /**
     * 用户ID - 分片键
     */
    @ShardingKey(database = true)
    private Long userId;
    
    /**
     * 订单金额
     */
    private BigDecimal amount;
    
    /**
     * 订单状态
     */
    private String status;
    
    /**
     * 创建时间
     */
    @Sort(order = 1)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    // Getters and Setters
}
```

## 14.11 Starter 各配置项详解

### 14.11.1 数据源配置项（spring.shardingsphere.datasource）

| 配置项 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| `names` | String | 是 | - | 数据源名称列表，逗号分隔 |
| `<name>.type` | String | 否 | HikariDataSource | 数据源实现类全限定名 |
| `<name>.driver-class-name` | String | 否 | 自动检测 | JDBC 驱动类名 |
| `<name>.jdbc-url` | String | 是 | - | JDBC 连接 URL |
| `<name>.username` | String | 是 | - | 数据库用户名 |
| `<name>.password` | String | 是 | - | 数据库密码 |
| `<name>.pool-name` | String | 否 | HikariPool-{name} | 连接池名称 |
| `<name>.minimum-idle` | int | 否 | 5 | 最小空闲连接数 |
| `<name>.maximum-pool-size` | int | 否 | 10 | 最大连接池大小 |
| `<name>.connection-timeout` | long | 否 | 30000 | 连接超时（毫秒） |
| `<name>.idle-timeout` | long | 否 | 600000 | 空闲超时（毫秒） |
| `<name>.max-lifetime` | long | 否 | 1800000 | 最大生命周期（毫秒） |
| `<name>.connection-test-query` | String | 否 | - | 连接测试查询 |

### 14.11.2 分片规则配置项（spring.shardingsphere.rules.sharding）

| 配置项 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| `tables.<logic-table>.actual-data-nodes` | String | 是 | - | 实际数据节点表达式 |
| `tables.<logic-table>.database-strategy` | Object | 否 | - | 数据库分片策略 |
| `tables.<logic-table>.table-strategy` | Object | 否 | - | 表分片策略 |
| `tables.<logic-table>.key-generate-strategy` | Object | 否 | - | 主键生成策略 |
| `tables.<logic-table>.audit-strategy` | Object | 否 | - | 审计策略 |
| `tables.<logic-table>.binding-tables` | List | 否 | - | 绑定表列表 |
| `binding-tables` | List | 否 | - | 全局绑定表配置 |
| `broadcast-tables` | List | 否 | - | 广播表列表 |
| `default-database-strategy` | Object | 否 | - | 默认数据库分片策略 |
| `default-table-strategy` | Object | 否 | - | 默认表分片策略 |
| `sharding-algorithms` | Map | 否 | - | 分片算法定义 |
| `key-generators` | Map | 否 | - | 分布式序列生成器 |
| `auditors` | Map | 否 | - | 审计器定义 |

### 14.11.3 读写分离配置项（spring.shardingsphere.rules.read-write-splitting）

| 配置项 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| `data-sources.<name>.type` | String | 是 | - | 类型：Static 或 Dynamic |
| `data-sources.<name>.props.write-data-source-name` | String | 是 | - | 写库数据源名 |
| `data-sources.<name>.props.read-data-source-names` | String | 是 | - | 读库数据源名列表 |
| `data-sources.<name>.load-balancer-name` | String | 否 | - | 负载均衡器名称 |
| `load-balancers.<name>.type` | String | 是 | - | 负载均衡类型 |
| `load-balancers.<name>.props` | Map | 否 | - | 负载均衡器属性 |

### 14.11.4 数据加密配置项（spring.shardingsphere.rules.encrypt）

| 配置项 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| `tables.<table>.columns.<column>.cipher` | Object | 是 | - | 密文列配置 |
| `tables.<table>.columns.<column>.assisted-query` | Object | 否 | - | 辅助查询列配置 |
| `tables.<table>.columns.<column>.like-query` | Object | 否 | - | 模糊查询列配置 |
| `tables.<table>.query-with-cipher-column` | boolean | 否 | true | 是否使用密文列查询 |

### 14.11.5 属性配置项（spring.shardingsphere.props）

| 配置项 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| `sql-show` | boolean | 否 | false | 是否显示 SQL |
| `sql-logging` | boolean | 否 | false | 是否记录 SQL 日志 |
| `max-connections-size-per-query` | int | 否 | 1 | 每查询最大连接数 |
| `executor-size` | int | 否 | CPU核心数 | 执行线程池大小 |
| `max-sharding-sql-count` | int | 否 | -1 | 最大分片 SQL 数量 |
| `transaction-type` | String | 否 | LOCAL | 事务类型 |
| `xa-transaction-manager-type` | String | 否 | Atomikos | XA 事务管理器 |

### 14.11.6 事务配置项（spring.shardingsphere.transaction）

| 配置项 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| `type` | String | 否 | LOCAL | 事务类型：LOCAL, XA, BASE |
| `provider-type` | String | 否 | Atomikos | XA 提供者类型 |
| `timeout-seconds` | int | 否 | 300 | 事务超时时间（秒） |

## 14.12 常见集成问题与解决方案

### 14.12.1 数据源初始化失败

**问题描述**：应用启动时报错 `No DataSource configured` 或 `Unable to create DataSource`。

**可能原因**：
1. 配置文件中数据源名称与 `spring.shardingsphere.datasource.names` 不匹配
2. 数据库连接 URL 格式错误
3. 缺少数据库驱动依赖
4. 数据库服务器不可达

**解决方案**：

```yaml
# 错误配置示例
spring:
  shardingsphere:
    datasource:
      # names 中定义的名称与下方数据源名称不一致
      names: ds_0,ds_1
      # 这里写的是 ds_master，不是 ds_0
      ds_master:
        jdbc-url: jdbc:mysql://localhost:3306/ds_0
        
# 正确配置
spring:
  shardingsphere:
    datasource:
      names: ds_0,ds_1
      ds_0:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3306/ds_0?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
        username: root
        password: root123
      ds_1:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3306/ds_1?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
        username: root
        password: root123
```

检查清单：
1. 确认 `names` 中的名称与下方数据源配置的 key 完全一致
2. 确认 JDBC URL 格式正确，包含必要的参数
3. 确认 pom.xml 中包含正确的数据库驱动依赖
4. 确认数据库服务正在运行且网络可达

### 14.12.2 分片键无法自动获取

**问题描述**：分片算法中使用分片键值时，返回 `null` 或抛出异常。

**可能原因**：
1. SQL 中未包含分片键
2. 分片键值在 BindingTable 关联查询中传递
3. 使用了 JOIN 查询但未指定分片键

**解决方案**：

```java
// 错误示例：JOIN 查询中未指定分片键
@Select("SELECT o.*, i.* FROM t_order o " +
       "JOIN t_order_item i ON o.order_id = i.order_id " +
       "WHERE o.user_id = #{userId}")  // 只指定了 user_id，但查询 item 表需要 order_id
List<Order> queryOrders(Long userId);

// 正确示例：明确传递分片键
@Select("SELECT o.*, i.* FROM t_order o " +
       "JOIN t_order_item i ON o.order_id = i.order_id " +
       "WHERE o.user_id = #{userId} AND o.order_id = #{orderId}")  // 同时传递 user_id 和 order_id
List<Order> queryOrders(Long userId, Long orderId);

// 或者使用 BindingTable 的正确查询方式
// 确保 t_order 和 t_order_item 在同一数据分片
```

### 14.12.3 分布式事务超时或回滚失败

**问题描述**：XA 事务执行过程中超时，或者部分分支事务回滚失败。

**可能原因**：
1. 事务超时时间设置过短
2. 分支事务执行时间过长
3. XA 事务管理器配置不当

**解决方案**：

```yaml
spring:
  shardingsphere:
    # 延长事务超时时间
    transaction:
      type: XA
      provider-type: Atomikos
      timeout-seconds: 600  # 10分钟超时
      
    props:
      # XA 相关属性
      xa-transaction:
        # 恢复检查间隔
        recovery-interval-seconds: 60
        # 最大分支事务数
        max-branch-count: 1000
```

```java
// 代码层面控制事务范围
@Service
public class OrderService {
    
    @Transactional(timeout = 600)  // 注解级别超时控制
    @ShardingTransactionType(TransactionType.XA)
    public void createOrder(Order order) {
        // 业务逻辑
    }
    
    // 对于长时间操作，使用编程式事务管理
    public void createOrderWithLongOperation(Order order) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setTimeout(600);
        
        template.execute(status -> {
            try {
                // 业务逻辑
                return null;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });
    }
}
```

### 14.12.4 主键生成冲突

**问题描述**：分布式主键出现重复，导致唯一键冲突。

**可能原因**：
1. 多个服务实例使用相同的 worker-id
2. 时钟回拨导致序列重置
3. 自定义主键生成器实现有 bug

**解决方案**：

```yaml
spring:
  shardingsphere:
    rules:
      sharding:
        key-generators:
          snowflake:
            type: SNOWFLAKE
            props:
              # 每个实例使用不同的 worker-id
              worker-id: ${INSTANCE_ID:1}
              # 最大振动偏移，防止时钟回拨
              max-vibration-offset: 3
              # 时间戳类型
              max-timestamp-bits: 32
```

```java
// 自定义主键生成器，处理时钟回拨
@Component
@ShardingKeyGenerator(name = "snowflake", type = "SNOWFLAKE")
public class SafeSnowflakeKeyGenerator implements KeyGenerateAlgorithm {
    
    private final Object mutex = new Object();
    private long lastTimestamp = -1;
    private long sequence = 0;
    private final long workerId;
    private final long maxVibrationOffset;
    
    public SafeSnowflakeKeyGenerator() {
        // 从环境变量或配置中心获取 worker-id
        this.workerId = Long.parseLong(
            System.getProperty("sharding.worker.id", "1"));
        this.maxVibrationOffset = 3;
    }
    
    @Override
    public Number generateKey() {
        synchronized (mutex) {
            long timestamp = System.currentTimeMillis();
            
            // 处理时钟回拨
            if (timestamp < lastTimestamp) {
                // 等待时钟追上
                long diff = lastTimestamp - timestamp;
                if (diff < maxVibrationOffset) {
                    try {
                        mutex.wait(diff);
                        timestamp = System.currentTimeMillis();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    // 使用新的时间戳（可能有重复风险，但避免死锁）
                    timestamp = lastTimestamp;
                }
            }
            
            if (timestamp == lastTimestamp) {
                sequence = (sequence + 1) & 4095;
                if (sequence == 0) {
                    timestamp = waitNextMillis(lastTimestamp);
                }
            } else {
                sequence = 0;
            }
            
            lastTimestamp = timestamp;
            
            return ((timestamp - EPOCH) << 22) | (workerId << 12) | sequence;
        }
    }
}
```

### 14.12.5 读写分离数据不一致

**问题描述**：主从延迟导致读写分离场景下读取到过期数据。

**可能原因**：
1. 主从复制延迟
2. 读写分离路由策略配置错误
3. 从库同步中断

**解决方案**：

```yaml
spring:
  shardingsphere:
    rules:
      read-write-splitting:
        data-sources:
          read_write_ds:
            type: Static
            props:
              write-data-source-name: ds_master
              read-data-source-names: ds_replica_0,ds_replica_1
            # 使用强制路由确保数据一致性
            # 注意：会导致读写分离失效，仅在对一致性要求高的场景使用
            # route-rule:
            #   static:
            #     write: ds_master
            #     read: ds_master  # 读写都走主库
            load-balancer-name: round_robin
        
        load-balancers:
          round_robin:
            type: ROUND_ROBIN
          # 权重负载均衡
          weight:
            type: WEIGHT
            props:
              ds_replica_0: 1
              ds_replica_1: 1
```

```java
// 强制路由到主库（重要更新后立即读取）
@Service
public class OrderService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // 写后读：强制路由到主库
    @ShardingType(type = "read_write_splitting", 
                  strategy = "强制主库路由")
    public Order createAndGetOrder(Order order) {
        // 1. 写入主库
        orderMapper.insert(order);
        
        // 2. 立即读取，确保读取到最新数据（走主库）
        // 使用 Hint API 强制路由
        HintManager hintManager = HintManager.getInstance();
        hintManager.setWriteRouteOnly();
        try {
            return orderMapper.selectById(order.getId());
        } finally {
            hintManager.close();
        }
    }
    
    // 非关键读取：可接受一定延迟（走从库）
    public List<Order> listOrders(Long userId) {
        return orderMapper.selectByUserId(userId);  // 默认走从库
    }
}
```

### 14.12.6 配置动态刷新不生效

**问题描述**：修改 Nacos 配置后，ShardingSphere 配置未刷新。

**可能原因**：
1. 未正确配置 Nacos 监听
2. 配置变更后未触发刷新方法
3. 配置格式不正确

**解决方案**：

```java
// 确保配置刷新监听器正确配置
@Configuration
public class NacosConfigRefresh implements NacosConfigListener {
    
    @Autowired
    private ShardingSphereDataSourceFactory dataSourceFactory;
    
    @NacosConfig(dataId = "sharding-rule.yaml", group = "SHARDINGSPHERE_GROUP")
    public void onConfigChange(String config) {
        // 重新加载配置
        try {
            // 1. 解析新配置
            ShardingRuleConfig newRuleConfig = parseConfig(config);
            
            // 2. 获取当前数据源
            DataSource currentDataSource = dataSourceFactory.getDataSource();
            
            // 3. 重新创建 DataSource（关键步骤）
            DataSource newDataSource = ShardingSphereDataSourceFactory.createDataSource(
                currentDataSource, 
                newRuleConfig
            );
            
            // 4. 替换 Bean
            ((DefaultListableBeanFactory) beanFactory)
                .destroySingleton("dataSource");
            ((DefaultListableBeanFactory) beanFactory)
                .registerSingleton("dataSource", newDataSource);
                
        } catch (Exception e) {
            log.error("Failed to refresh ShardingSphere config", e);
        }
    }
}
```

### 14.12.7 分片算法性能问题

**问题描述**：分片查询性能低，大量请求路由到错误的分片。

**可能原因**：
1. 分片算法复杂度高
2. IN 查询包含过多分片键值
3. 范围查询跨越大量分片

**解决方案**：

```java
// 优化分片算法实现
@Component
@ShardingSphereAlgorithm(name = "optimizedInline", type = "INLINE")
public class OptimizedInlineShardingAlgorithm implements PreciseShardingAlgorithm<Long> {
    
    // 使用缓存避免重复计算
    private final Map<Long, String> cache = new ConcurrentHashMap<>();
    private final int shardingCount;
    
    public OptimizedInlineShardingAlgorithm() {
        Properties props = new Properties();
        this.shardingCount = Integer.parseInt(
            props.getProperty("sharding-count", "16"));
    }
    
    @Override
    public String doSharding(Collection<String> availableTargetNames, 
            PreciseShardingValue<Long> shardingValue) {
        Long value = shardingValue.getValue();
        
        // 使用缓存
        return cache.computeIfAbsent(value, v -> {
            int index = (int) (v % shardingCount);
            String suffix = String.format("%02d", index);
            return availableTargetNames.stream()
                .filter(name -> name.endsWith(suffix))
                .findFirst()
                .orElse(availableTargetNames.iterator().next());
        });
    }
    
    // 定期清理缓存，防止内存泄漏
    @Scheduled(fixedRate = 3600000)
    public void clearCache() {
        if (cache.size() > 1000000) {
            cache.clear();
        }
    }
}
```

## 14.13 本章小结

本章详细介绍了 ShardingSphere-JDBC 与 Spring Boot/Spring Cloud 生态的深度集成：

1. **Spring Boot 自动配置原理**：深入理解了 `@Conditional` 条件装配、`@ConfigurationProperties` 配置绑定等核心机制，明白 ShardingSphere Starter 如何实现零配置自动装配。

2. **Spring Boot Starter 使用**：掌握了从 Maven 依赖引入到 `application.yml` 完整配置的技能，包括最小化配置示例和包含分片、读写分离、加密的复杂配置。

3. **application.yml 配置详解**：系统学习了数据源配置、分片规则配置、读写分离配置、加密配置以及各类属性配置项的详细含义。

4. **Spring Cloud Feign 集成**：实现了微服务间调用时分布式事务上下文和分片键的透明传播，掌握了 Feign 拦截器的配置方法。

5. **Spring Cloud Gateway 集成**：学会了在 API 网关层面实现分片路由、负载均衡和限流保护。

6. **Nacos 配置中心集成**：实现了配置的集中管理、版本控制和动态刷新，无需重启应用即可更新分片规则。

7. **Sentinel 熔断降级集成**：实现了数据源级别的熔断保护，防止故障扩散，并支持热点参数的限流控制。

8. **多数据源配置**：掌握了异构数据库、多数据源动态切换以及跨数据源事务处理的方法。

9. **注解驱动开发**：使用 `@ShardingSphereAlgorithm`、`@ShardingKeyGenerator` 等注解简化分片算法和主键生成器的配置。

10. **常见问题与解决方案**：总结了数据源初始化、分片键获取、分布式事务、主键冲突、读写分离一致性和动态配置刷新等常见问题的解决方案。

通过本章的学习，读者应当能够在实际项目中熟练运用 ShardingSphere-JDBC 的 Spring Boot/Spring Cloud 集成能力，构建高性能、高可用的分布式数据库架构。
