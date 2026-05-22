---
title: 第5章 Spring Data MongoDB 环境搭建
sidebar_label: 第5章
---

# 第5章 Spring Data MongoDB 环境搭建

本章节将详细讲解如何搭建 Spring Data MongoDB 开发环境，包括项目创建、依赖配置、连接配置以及连接验证。通过本章的学习，您将掌握构建完整 Spring Boot + MongoDB 开发环境的核心技能。

## 5.1 Spring Boot 项目创建

### 5.1.1 使用 Spring Initializr 创建项目

Spring Initializr 是官方提供的项目生成工具，您可以通过以下方式访问：

- **在线方式**：访问 https://start.spring.io/
- **IDEA 方式**：File -> New -> Project -> Spring Initializr

#### 项目创建步骤

1. 选择构建工具（Maven 或 Gradle）
2. 填写项目元数据：
   - **Group**: com.example
   - **Artifact**: mongodb-spring-data
   - **Name**: mongodb-spring-data
   - **Package**: com.example.mongodb
3. 选择 Spring Boot 版本（建议使用 3.2.x 或 2.7.x LTS）
4. 添加依赖：
   - Lombok（可选，简化代码）
   - Spring Data MongoDB
   - Spring Web（用于 REST API 开发）

### 5.1.2 项目目录结构

一个标准的 Spring Boot + MongoDB 项目结构如下：

```
mongodb-spring-data/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/mongodb/
│   │   │       ├── MongodbApplication.java          # 启动类
│   │   │       ├── config/
│   │   │       │   └── MongoConfig.java             # MongoDB 配置类
│   │   │       ├── entity/
│   │   │       │   └── User.java                    # 实体类
│   │   │       ├── repository/
│   │   │       │   └── UserRepository.java          # Repository 接口
│   │   │       └── service/
│   │   │           └── UserService.java              # 服务层
│   │   └── resources/
│   │       ├── application.yml                      # 应用配置文件
│   │       └── application-dev.yml                  # 开发环境配置
│   └── test/
│       └── java/
│           └── com/example/mongodb/
│               └── MongodbApplicationTests.java      # 启动测试
├── pom.xml
└── README.md
```

---

## 5.2 MongoDB 驱动与 Spring Data MongoDB 依赖

### 5.2.1 Maven pom.xml 完整配置

#### Spring Boot 3.x 版本（推荐）

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
    <artifactId>mongodb-spring-data</artifactId>
    <version>1.0.0</version>
    <name>mongodb-spring-data</name>
    <description>Spring Data MongoDB Demo Project</description>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Spring Data MongoDB Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
        </dependency>

        <!-- Spring Web Starter（用于 REST API） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Test Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Lombok（可选，简化代码） -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Testcontainers MongoDB（用于集成测试） -->
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>mongodb</artifactId>
            <version>1.19.7</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>1.19.7</version>
            <scope>test</scope>
        </dependency>

        <!-- Apache Commons（工具类） -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
        </plugins>
    </build>
</project>
```

#### Spring Boot 2.7.x 版本

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
        <version>2.7.18</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>mongodb-spring-data</artifactId>
    <version>1.0.0</version>
    <name>mongodb-spring-data</name>
    <description>Spring Data MongoDB Demo Project</description>

    <properties>
        <java.version>11</java.version>
    </properties>

    <dependencies>
        <!-- Spring Data MongoDB Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
        </dependency>

        <!-- Spring Web Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Test Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
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

### 5.2.2 Gradle 构建配置

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.5'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.example'
version = '1.0.0'

java {
    sourceCompatibility = '17'
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Data MongoDB
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
    
    // Spring Web
    implementation 'org.springframework.boot:spring-boot-starter-web'
    
    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:mongodb:1.19.7'
    testImplementation 'org.testcontainers:junit-jupiter:1.19.7'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

---

## 5.3 application.yml 配置

### 5.3.1 基础配置

```yaml
spring:
  application:
    name: mongodb-spring-data

  # MongoDB 连接配置
  data:
    mongodb:
      # 标准连接格式：mongodb://用户名:密码@主机:端口/数据库名
      uri: mongodb://localhost:27017/mydb
      
      # 或者使用独立的连接参数
      # host: localhost
      # port: 27017
      # database: mydb
      # username: admin
      # password: secret

# 服务器配置
server:
  port: 8080

# 日志配置
logging:
  level:
    root: INFO
    org.springframework.data.mongodb: DEBUG
    com.example.mongodb: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 5.3.2 连接池配置

MongoDB Java 驱动使用默认的连接池配置，如需自定义可以通过 MongoDB Client Settings 进行配置：

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://admin:password@localhost:27017,localhost:27018,localhost:27019/mydb?replicaSet=rs0
      
  # 连接池配置（通过 MongoDB Client Options）
  # 注意：Spring Boot 3.x 中部分配置已迁移到代码配置
  # 以下为 Spring Boot 2.7.x 兼容的配置方式
```

### 5.3.3 多环境配置

#### application-dev.yml（开发环境）

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/mydb_dev
      auto-index: true  # 开发环境自动创建索引

logging:
  level:
    org.springframework.data.mongodb: DEBUG
```

#### application-prod.yml（生产环境）

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://admin:${MONGODB_PASSWORD}@mongo-prod-cluster:27017/mydb_prod?replicaSet=rs0&authSource=admin
      auto-index: false  # 生产环境手动管理索引
```

### 5.3.4 连接字符串详解

| 参数 | 说明 | 示例 |
|------|------|------|
| `mongodb://` | 协议前缀 | mongodb:// |
| `username:password` | 认证信息 | admin:secret123 |
| `@` | 分隔认证与主机 | @ |
| `host:port` | 主机地址和端口 | localhost:27017 |
| `/database` | 数据库名称 | /mydb |
| `?replicaSet` | 副本集名称 | ?replicaSet=rs0 |
| `&authSource` | 认证数据库 | &authSource=admin |

**完整的连接字符串格式：**

```
mongodb://用户名:密码@主机1:端口1,主机2:端口2,主机3:端口3/数据库名?replicaSet=副本集名&authSource=认证数据库&maxPoolSize=100&minPoolSize=10
```

---

## 5.4 MongoDB 连接验证

### 5.4.1 启动类

```java
package com.example.mongodb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * MongoDB Spring Boot 启动类
 */
@SpringBootApplication
@EnableMongoAuditing  // 启用 MongoDB 审计功能（自动填充创建时间、修改时间等）
public class MongodbApplication {

    public static void main(String[] args) {
        SpringApplication.run(MongodbApplication.class, args);
    }
}
```

### 5.4.2 MongoDB 配置类

```java
package com.example.mongodb.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.util.concurrent.TimeUnit;

/**
 * MongoDB 配置类
 * 用于自定义 MongoDB 连接和转换器配置
 */
@Configuration
public class MongoConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    /**
     * 配置 MongoDB 客户端
     * 这里可以自定义连接池、超时时间等参数
     */
    @Bean
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(mongoUri);
        
        MongoClientSettings settings = MongoClientSettings.builder()
                // 连接池配置
                .applyToConnectionPoolSettings(builder -> builder
                        .maxSize(100)                    // 最大连接数
                        .minSize(10)                     // 最小连接数
                        .maxWaitTime(5, TimeUnit.SECONDS)  // 最大等待时间
                        .maxConnectionIdleTime(10, TimeUnit.MINUTES)  // 连接空闲时间
                        .maxConnectionLifeTime(30, TimeUnit.MINUTES)   // 连接最大生存时间
                )
                // Socket 配置
                .applyToSocketSettings(builder -> builder
                        .connectTimeout(10, TimeUnit.SECONDS)    // 连接超时时间
                        .readTimeout(30, TimeUnit.SECONDS)       // 读取超时时间
                )
                // Server 配置
                .applyToServerSettings(builder -> builder
                        .heartbeatFrequency(10, TimeUnit.SECONDS)  // 心跳频率
                )
                .build();

        return MongoClients.create(settings);
    }

    /**
     * MongoDB 数据库工厂
     */
    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        return new SimpleMongoClientDatabaseFactory(mongoClient, 
                new ConnectionString(mongoUri).getDatabase());
    }

    /**
     * MongoTemplate 是 MongoDB 操作的核心类
     */
    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
        // 自定义 MappingMongoConverter，去除 _class 字段
        MappingMongoConverter converter = new MappingMongoConverter(
                new DefaultDbRefResolver(mongoDatabaseFactory),
                new MongoMappingContext()
        );
        // 移除 _class 字段的映射
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        
        return new MongoTemplate(mongoDatabaseFactory, converter);
    }
}
```

### 5.4.3 实体类

```java
package com.example.mongodb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户实体类
 * @Document 注解指定 Collection 名称
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    /**
     * 主键，使用 @Id 注解
     * MongoDB 会自动生成 ObjectId
     */
    @Id
    private String id;

    /**
     * 用户名，建立唯一索引
     */
    @Indexed(unique = true)
    private String username;

    /**
     * 密码（实际项目中应加密存储）
     */
    private String password;

    /**
     * 邮箱
     */
    @Indexed(unique = true)
    private String email;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 用户状态：ACTIVE、INACTIVE、BANNED
     */
    private String status;

    /**
     * 用户标签
     */
    private List<String> tags;

    /**
     * 创建时间，自动填充
     */
    @CreatedDate
    private LocalDateTime createdAt;

    /**
     * 更新时间，自动填充
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

### 5.4.4 Repository 接口

```java
package com.example.mongodb.repository;

import com.example.mongodb.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * User Repository 接口
 * 继承 MongoRepository<User, String> 可以获得基本的 CRUD 和分页功能
 * 
 * Spring Data MongoDB 会自动实现该接口
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * 根据用户名查询用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查询用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据状态查询用户列表
     */
    List<User> findByStatus(String status);

    /**
     * 根据用户名模糊查询
     */
    List<User> findByUsernameContaining(String keyword);

    /**
     * 根据年龄范围查询
     */
    List<User> findByAgeBetween(Integer minAge, Integer maxAge);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 删除 by 用户名
     */
    void deleteByUsername(String username);
}
```

### 5.4.5 Service 层

```java
package com.example.mongodb.service;

import com.example.mongodb.entity.User;
import com.example.mongodb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 用户服务层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 创建用户
     */
    public User createUser(User user) {
        log.info("创建用户: {}", user.getUsername());
        return userRepository.save(user);
    }

    /**
     * 根据 ID 查询用户
     */
    public Optional<User> getUserById(String id) {
        log.info("根据 ID 查询用户: {}", id);
        return userRepository.findById(id);
    }

    /**
     * 根据用户名查询用户
     */
    public Optional<User> getUserByUsername(String username) {
        log.info("根据用户名查询用户: {}", username);
        return userRepository.findByUsername(username);
    }

    /**
     * 查询所有用户
     */
    public List<User> getAllUsers() {
        log.info("查询所有用户");
        return userRepository.findAll();
    }

    /**
     * 更新用户
     */
    public User updateUser(String id, User user) {
        log.info("更新用户: {}", id);
        user.setId(id);
        return userRepository.save(user);
    }

    /**
     * 删除用户
     */
    public void deleteUser(String id) {
        log.info("删除用户: {}", id);
        userRepository.deleteById(id);
    }

    /**
     * 批量创建用户
     */
    public List<User> createUsers(List<User> users) {
        log.info("批量创建用户，数量: {}", users.size());
        return userRepository.saveAll(users);
    }
}
```

### 5.4.6 单元测试示例

#### 基础连接测试

```java
package com.example.mongodb;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MongoDB 连接测试
 */
@SpringBootTest
class MongodbApplicationTests {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoClient mongoClient;

    @Test
    void contextLoads() {
        // 验证 Spring 上下文加载成功
        assertNotNull(mongoTemplate);
        assertNotNull(mongoClient);
    }

    @Test
    void testMongoConnection() {
        // 获取数据库名称
        String databaseName = mongoTemplate.getDb().getName();
        assertEquals("mydb", databaseName);
        
        // 测试 ping 命令
        String result = mongoTemplate.getDb().runCommand(new org.bson.Document("ping", new org.bson.BsonObject())).toString();
        assertNotNull(result);
        
        System.out.println("数据库连接成功，数据库名: " + databaseName);
    }

    @Test
    void testMongoClient() {
        // 获取服务器地址列表
        var clusterSettings = mongoClient.getSettings().getClusterSettings();
        System.out.println("MongoDB 集群配置: " + clusterSettings);
        
        assertNotNull(clusterSettings);
    }
}
```

#### 使用 Testcontainers 进行集成测试

```java
package com.example.mongodb;

import com.example.mongodb.entity.User;
import com.example.mongodb.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户 Repository 集成测试
 * 使用 Testcontainers 启动真实的 MongoDB 容器
 */
@SpringBootTest
@Testcontainers
class UserRepositoryTest {

    // 启动 MongoDB 容器
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // 每个测试前清空集合
        userRepository.deleteAll();
    }

    @Test
    void testSave() {
        User user = User.builder()
                .username("testuser")
                .password("password123")
                .email("test@example.com")
                .age(25)
                .status("ACTIVE")
                .tags(Arrays.asList("java", "spring"))
                .build();

        User savedUser = userRepository.save(user);

        assertNotNull(savedUser.getId());
        assertEquals("testuser", savedUser.getUsername());
        assertEquals("test@example.com", savedUser.getEmail());
        assertNotNull(savedUser.getCreatedAt());
        System.out.println("保存用户成功: " + savedUser);
    }

    @Test
    void testFindByUsername() {
        User user = User.builder()
                .username("findme")
                .password("password")
                .email("find@example.com")
                .build();
        userRepository.save(user);

        Optional<User> found = userRepository.findByUsername("findme");

        assertTrue(found.isPresent());
        assertEquals("find@example.com", found.get().getEmail());
    }

    @Test
    void testFindByStatus() {
        // 创建多个用户
        User user1 = User.builder()
                .username("user1")
                .status("ACTIVE")
                .build();
        User user2 = User.builder()
                .username("user2")
                .status("INACTIVE")
                .build();
        User user3 = User.builder()
                .username("user3")
                .status("ACTIVE")
                .build();
        userRepository.saveAll(Arrays.asList(user1, user2, user3));

        List<User> activeUsers = userRepository.findByStatus("ACTIVE");

        assertEquals(2, activeUsers.size());
    }

    @Test
    void testFindByUsernameContaining() {
        User user1 = User.builder().username("john_doe").build();
        User user2 = User.builder().username("john_smith").build();
        User user3 = User.builder().username("jane_doe").build();
        userRepository.saveAll(Arrays.asList(user1, user2, user3));

        List<User> johnUsers = userRepository.findByUsernameContaining("john");

        assertEquals(2, johnUsers.size());
    }

    @Test
    void testFindByAgeBetween() {
        User user1 = User.builder().username("user1").age(20).build();
        User user2 = User.builder().username("user2").age(30).build();
        User user3 = User.builder().username("user3").age(40).build();
        userRepository.saveAll(Arrays.asList(user1, user2, user3));

        List<User> usersInRange = userRepository.findByAgeBetween(25, 35);

        assertEquals(1, usersInRange.size());
        assertEquals("user2", usersInRange.get(0).getUsername());
    }

    @Test
    void testExistsByUsername() {
        User user = User.builder()
                .username("existinguser")
                .build();
        userRepository.save(user);

        assertTrue(userRepository.existsByUsername("existinguser"));
        assertFalse(userRepository.existsByUsername("nonexistent"));
    }

    @Test
    void testDeleteByUsername() {
        User user = User.builder()
                .username("todelete")
                .build();
        userRepository.save(user);

        userRepository.deleteByUsername("todelete");

        assertFalse(userRepository.existsByUsername("todelete"));
    }

    @Test
    void testSaveAll() {
        List<User> users = Arrays.asList(
                User.builder().username("user1").build(),
                User.builder().username("user2").build(),
                User.builder().username("user3").build()
        );

        List<User> savedUsers = userRepository.saveAll(users);

        assertEquals(3, savedUsers.size());
        assertEquals(3, userRepository.count());
    }
}
```

### 5.4.7 Controller 层（可选）

```java
package com.example.mongodb.controller;

import com.example.mongodb.entity.User;
import com.example.mongodb.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理 REST API
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User created = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable String id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable String id, @RequestBody User user) {
        return ResponseEntity.ok(userService.updateUser(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## 5.5 常见问题与解决方案

### 5.5.1 连接失败问题

**问题**: `MongoTimeoutException: Timeout waiting for a node`

**解决方案**:
1. 检查 MongoDB 服务是否启动
2. 确认防火墙是否开放 27017 端口
3. 验证连接字符串是否正确
4. 检查用户名密码是否正确

### 5.5.2 认证失败问题

**问题**: `MongoCommandException: Command failed with error 13`

**解决方案**:
```yaml
# 确保使用正确的认证数据库
spring:
  data:
    mongodb:
      uri: mongodb://admin:password@localhost:27017/mydb?authSource=admin
```

### 5.5.3 索引创建问题

**问题**: `MongoWriteException: E11000 duplicate key error`

**解决方案**:
1. 检查字段是否已建立唯一索引
2. 在代码中处理重复键异常
3. 使用 `@Indexed(unique = true)` 时确保数据不重复

---

## 5.6 本章小结

本章完成了 Spring Data MongoDB 开发环境的所有配置工作：

1. **项目创建**: 掌握了使用 Spring Initializr 或 IDEA 创建 Spring Boot 项目的方法
2. **依赖配置**: 完成了 Maven pom.xml 的完整配置，包括 Spring Data MongoDB 依赖
3. **配置文件**: 学会了 application.yml 的各种配置方式，包括多环境配置
4. **连接验证**: 通过启动类、配置类、单元测试等多种方式验证 MongoDB 连接
5. **代码示例**: 提供了完整的实体类、Repository、Service、Controller 示例代码

下一章我们将学习 **实体映射与 Repository**，深入了解 Spring Data MongoDB 的注解和查询方法。

---

## 相关资源

- [Spring Data MongoDB 官方文档](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/)
- [MongoDB Java Driver 文档](https://www.mongodb.com/docs/drivers/java/)
- [Testcontainers MongoDB 模块](https://java.testcontainers.org/modules/databases/mongodb/)
