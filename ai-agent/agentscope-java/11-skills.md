# 第十一章：技能系统

> 本章节配套完整可运行的 Spring Boot 项目，演示如何使用 AgentScope Java 的技能（Skill）系统实现动态技能加载与持久化存储。

## 11.1 技能（Skill）的定义

### 11.1.1 什么是 Skill

在 AgentScope Java 中，**技能（Skill）** 是扩展智能体能力的模块化数据包。每个 Skill 包含指令、元数据（metadata）和可选资源（如脚本、参考文档、示例等），智能体在执行相关任务时会自动加载并使用这些资源。

与直接给智能体喂大量上下文不同，Skill 采用**渐进式披露（Progressive Disclosure）**机制：

- **阶段一**：智能体启动时，系统中只加载技能的元数据（name、description），约 100 tokens/Skill
- **阶段二**：AI 判断任务需要某个 Skill 时，调用 `load_skill_through_path` 工具加载完整指令（通常 <5k tokens）
- **阶段三**：智能体按需访问 Skill 附带的资源文件（如参考文档、脚本等）

这种设计显著减少了初始上下文大小，同时保证智能体在需要时能获取完整技能内容。

### 11.1.2 Skill 的结构

一个标准 Skill 的目录结构如下：

```text
skill-name/
├── SKILL.md              # 必需：入口文件，包含 YAML frontmatter 和技能指令
├── references/            # 可选：详细参考文档
│   ├── api-doc.md
│   └── best-practices.md
├── examples/             # 可选：工作示例
│   └── example1.java
└── scripts/              # 可选：可执行脚本
    └── process.py
```

**SKILL.md 格式**：

```yaml
---
name: skill-name                        # 必需：技能名称（小写字母、数字、下划线）
description: This skill should be used when...  # 必需：触发描述，说明何时使用
homepage: https://example.com/docs     # 可选：额外元数据
metadata:
  clawdbot:
    requires:
      env:
        - API_KEY
---

# 技能名称

## 功能概述
[详细说明该技能的功能]

## 使用方法
[使用步骤和最佳实践]
```

### 11.1.3 AgentSkill 类

在代码中，技能由 `io.agentscope.core.skill.AgentSkill` 类表示，核心字段如下：

| 字段 | 说明 |
|------|------|
| `name` | 技能名称，从 metadata 中提取 |
| `description` | 技能描述，AI 据此判断何时使用该技能 |
| `skillContent` | 技能的完整内容（SKILL.md 正文） |
| `resources` | 资源文件 Map（path -> content） |
| `metadata` | 完整元数据 Map |

**创建 Skill 的三种方式**：

```java
// 方式一：使用 Builder（推荐）
AgentSkill skill = AgentSkill.builder()
    .name("data_analysis")
    .description("Use this skill when analyzing data, calculating statistics, or generating reports")
    .skillContent("# Data Analysis\n...")
    .addResource("references/api-doc.md", "# API Reference\n...")
    .addResource("scripts/process.py", "def process(data): ...\n")
    .source("custom")
    .build();

// 方式二：从 Markdown 创建
String skillMd = """
---
name: data_analysis
description: Use this skill when analyzing data
---
# Data Analysis
Content...
""";
Map<String, String> resources = Map.of("references/formulas.md", "# Formulas\n...");
AgentSkill skill = SkillUtil.createFrom(skillMd, resources);

// 方式三：直接构造
AgentSkill skill = new AgentSkill(
    "data_analysis",
    "Use when analyzing data...",
    "# Data Analysis\n...",
    resources
);
```

### 11.1.4 内置 Skill 工具

AgentScope 为 Skill 系统内置了 `load_skill_through_path` 工具：

```java
// 工具签名
load_skill_through_path(skillId: enum, resourcePath?: string)
```

- **`skillId`**：枚举字段，只能从已注册的 Skill 中选择，保证准确性
- **`resourcePath`**：相对于 Skill 根目录的资源路径（如 `references/api-doc.md`）
- 路径错误时返回所有可用资源路径列表，帮助 LLM 自我纠正

## 11.2 技能注册与调用

### 11.2.1 SkillBox：技能容器

`SkillBox` 是技能系统的核心组件，负责：

1. 持有 `Toolkit` 并注册所有 Skill
2. 生成技能系统提示词（注入到 Agent 的 system prompt 中）
3. 管理 Skill 与 Tool 的绑定关系

```java
// 基础用法
Toolkit toolkit = new Toolkit();
SkillBox skillBox = new SkillBox(toolkit);

// 注册技能
skillBox.registerSkill(dataSkill);
skillBox.registerSkill(sqlSkill);

// 或使用 registration() 进行链式注册（支持 Skill 与 Tool 绑定）
skillBox.registration()
    .skill(dataSkill)
    .tool(loadDataTool)   // 将 Tool 绑定到该 Skill，仅在 Skill 激活时生效
    .apply();
```

### 11.2.2 将 Skill 集成到 Agent

```java
// 方式一：使用 SkillBox（推荐）
SkillBox skillBox = new SkillBox(new Toolkit());
skillBox.registerSkill(mySkill);

ReActAgent agent = ReActAgent.builder()
    .name("MyAssistant")
    .model(model)
    .skillBox(skillBox)
    .build();

// 方式二：同时使用 Toolkit 和 SkillBox
Toolkit toolkit = new Toolkit();
SkillBox skillBox = new SkillBox(toolkit);
skillBox.registerSkill(mySkill);

ReActAgent agent = ReActAgent.builder()
    .name("MyAssistant")
    .model(model)
    .toolkit(toolkit)
    .skillBox(skillBox)  // 自动注册 skill 工具
    .memory(new InMemoryMemory())
    .build();
```

### 11.2.3 渐进式披露的 Tool 生命周期

将 Tool 与 Skill 绑定后，Tool 的生命周期与 Skill 保持一致：

- Skill 激活后，绑定的 Tool 在整个会话期间保持可用
- 避免了旧机制中每轮对话后 Tool 失活导致的调用失败问题

```java
SkillBox skillBox = new SkillBox(toolkit);

AgentTool dataLoadTool = new AgentTool(...);
AgentTool reportGenTool = new AgentTool(...);

// 为不同 Skill 绑定不同的 Tool
skillBox.registration()
    .skill(dataSkill)
    .tool(dataLoadTool)
    .apply();

skillBox.registration()
    .skill(reportSkill)
    .tool(reportGenTool)
    .apply();
```

### 11.2.4 自定义技能提示词

SkillBox 默认会为每个注册的 Skill 生成 XML `<skill>` 条目注入到系统提示词。可以通过 `instruction` 参数自定义提示词头部：

```java
// 自定义 instruction 头部
String customInstruction = """
    ## 可用技能
    当任务匹配某个技能时，使用 load_skill_through_path 加载它。
    """;

SkillBox skillBox = new SkillBox(toolkit, customInstruction);

// 可选：仅向 prompt 暴露核心 metadata
skillBox.setExposeAllSkillMetadata(false);
```

## 11.3 技能仓库（MySQL / Git）

AgentScope 提供了多种 Skill 持久化存储方案，支持从不同来源加载技能。

### 11.3.1 仓库接口

所有仓库实现 `io.agentscope.core.skill.repository.AgentSkillRepository` 接口：

```java
public interface AgentSkillRepository extends AutoCloseable {
    AgentSkill getSkill(String name);
    List<String> getAllSkillNames();
    List<AgentSkill> getAllSkills();
    boolean save(List<AgentSkill> skills, boolean force);
    boolean delete(String skillName);
    boolean skillExists(String skillName);
    AgentSkillRepositoryInfo getRepositoryInfo();
    String getSource();
}
```

### 11.3.2 MySQL 存储

使用 `MysqlSkillRepository` 将技能存储到 MySQL 数据库。数据库表结构如下：

```sql
-- 技能主表
CREATE TABLE agentscope_skills (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    skill_content LONGTEXT NOT NULL,
    source VARCHAR(255) NOT NULL,
    metadata_json LONGTEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 技能资源表
CREATE TABLE agentscope_skill_resources (
    id BIGINT NOT NULL,
    resource_path VARCHAR(500) NOT NULL,
    resource_content LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id, resource_path),
    FOREIGN KEY (id) REFERENCES agentscope_skills(id) ON DELETE CASCADE
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**使用方式**：

```java
// 简单构造函数（使用默认数据库/表名）
DataSource dataSource = createDataSource();
MysqlSkillRepository repo = new MysqlSkillRepository(dataSource, true, true);

// 使用 Builder 进行自定义配置
MysqlSkillRepository repo = MysqlSkillRepository.builder(dataSource)
    .databaseName("my_database")
    .skillsTableName("my_skills")
    .resourcesTableName("my_resources")
    .createIfNotExist(true)
    .writeable(true)
    .build();

// 保存技能
repo.save(List.of(skill), false);

// 加载技能
AgentSkill loaded = repo.getSkill("data_analysis");

// 列出所有技能
List<AgentSkill> allSkills = repo.getAllSkills();

// 删除技能
repo.delete("old_skill");
```

### 11.3.3 Git 仓库（只读）

使用 `GitSkillRepository` 从 Git 仓库加载技能（只读），支持 HTTPS 和 SSH：

```java
// 自动同步模式（默认每次读取检查远端变化）
AgentSkillRepository repo = new GitSkillRepository(
    "https://github.com/your-org/your-skills-repo.git");
AgentSkill skill = repo.getSkill("data-analysis");
List<AgentSkill> allSkills = repo.getAllSkills();

// 手动同步模式
GitSkillRepository manualRepo = new GitSkillRepository(
    "https://github.com/your-org/your-skills-repo.git", false);
manualRepo.sync();  // 手动刷新
```

### 11.3.4 Classpath 仓库（只读）

从 classpath 资源中加载预打包的 Skills，适用于 JAR 或 Spring Boot Fat JAR：

```java
try (ClasspathSkillRepository repository = new ClasspathSkillRepository("skills")) {
    AgentSkill skill = repository.getSkill("data-analysis");
    List<AgentSkill> allSkills = repository.getAllSkills();
}
```

资源目录结构：`src/main/resources/skills/` 下放置多个 Skill 子目录，每个子目录包含 `SKILL.md`。

### 11.3.5 文件系统仓库

从本地文件系统加载技能：

```java
AgentSkillRepository repo = new FileSystemSkillRepository(Path.of("./skills"));
repo.save(List.of(skill), false);
AgentSkill loaded = repo.getSkill("data_analysis");
```

### 11.3.6 Nacos 仓库（只读）

从 Nacos 配置中心实时拉取或订阅 Skill，适合需要与 Nacos 保持同步的在线场景：

```java
// 需引入 agentscope-extensions-nacos-skill 依赖
try (NacosSkillRepository repository = new NacosSkillRepository(aiService, "namespace")) {
    AgentSkill skill = repository.getSkill("data-analysis");
    boolean exists = repository.skillExists("data-analysis");
}
```

## 11.4 【案例】动态技能加载

本案例使用 Java 21 + Spring Boot 3 + H2 嵌入式数据库，完整演示：

- 技能的定义与注册
- MySQL 风格存储（使用 H2 模拟 MySQL 表结构）
- 动态技能加载与执行

### 11.4.1 项目结构

```
chapter11-skills/
├── pom.xml
├── src/main/java/io/agentscope/tutorial/chapter11/
│   ├── Chapter11Application.java          # Spring Boot 启动类
│   ├── config/
│   │   └── SkillConfig.java              # Skill 配置类
│   ├── model/
│   │   └── MockChatModel.java            # 模拟模型（用于演示）
│   ├── repository/
│   │   └── H2SkillRepository.java        # H2 实现 MySQL 表结构
│   └── service/
│       └── SkillService.java             # 技能服务
└── src/main/resources/
    ├── application.yml
    └── data.sql                           # H2 初始化数据
```

### 11.4.2 pom.xml

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
        <version>3.3.0</version>
        <relativePath/>
    </parent>

    <groupId>io.agentscope.tutorial</groupId>
    <artifactId>chapter11-skills</artifactId>
    <version>1.0.0</version>
    <name>Chapter 11 - Skills System</name>
    <description>AgentScope Java 技能系统教程案例</description>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- H2 数据库（模拟 MySQL） -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- JDBC（用于 MysqlSkillRepository） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>

        <!-- AgentScope Core（包含 Skill 相关类） -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-core</artifactId>
            <version>1.0.0</version>
        </dependency>

        <!-- AgentScope MySQL Skill Repository 扩展 -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-extensions-skill-mysql-repository</artifactId>
            <version>1.0.0</version>
        </dependency>

        <!-- 简化 YAML 解析 -->
        <dependency>
            <groupId>org.yaml</groupId>
            <artifactId>snakeyaml</artifactId>
            <version>2.2</version>
        </dependency>

        <!-- Lombok（可选，简化代码） -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
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

> **注意**：实际项目中请将 `agentscope-core` 和扩展模块版本替换为实际版本，或通过 Maven Central 获取最新版本。

### 11.4.3 application.yml

```yaml
spring:
  application:
    name: chapter11-skills

  # H2 控制台（开发时方便查看数据）
  h2:
    console:
      enabled: true
      path: /h2-console

  # 数据源配置（H2 模拟 MySQL）
  datasource:
    url: jdbc:h2:mem:agentscope;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE
    driver-class-name: org.h2.Driver
    username: sa
    password:

  # JPA 配置（H2 使用 JDBC 而非 JPA）
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: true

  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql

# 技能仓库配置
skill:
  repository:
    # MySQL 仓库配置（使用 H2 模拟）
    mysql:
      database-name: agentscope
      skills-table: agentscope_skills
      resources-table: agentscope_skill_resources
      create-if-not-exist: true
      writeable: true

# 日志配置
logging:
  level:
    io.agentscope: DEBUG
    chapter11: INFO
```

### 11.4.4 schema.sql（H2 模拟 MySQL 表结构）

```sql
-- =====================================================
-- 技能系统表结构（H2 模拟 MySQL，支持 MysqlSkillRepository）
-- =====================================================

-- 创建技能主表（与 MySQL 版本完全兼容）
CREATE TABLE IF NOT EXISTS agentscope_skills (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    skill_content LONGTEXT NOT NULL,
    source VARCHAR(255) NOT NULL,
    metadata_json LONGTEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建技能资源表
CREATE TABLE IF NOT EXISTS agentscope_skill_resources (
    id BIGINT NOT NULL,
    resource_path VARCHAR(500) NOT NULL,
    resource_content LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id, resource_path),
    FOREIGN KEY (id) REFERENCES agentscope_skills(id) ON DELETE CASCADE
);

-- 创建索引（提升查询性能）
CREATE INDEX IF NOT EXISTS idx_skills_name ON agentscope_skills(name);
CREATE INDEX IF NOT EXISTS idx_resources_skill_id ON agentscope_skill_resources(id);
```

### 11.4.5 data.sql（初始化示例技能）

```sql
-- =====================================================
-- 初始化示例技能数据
-- =====================================================

-- 数据分析技能
INSERT INTO agentscope_skills (name, description, skill_content, source, metadata_json)
VALUES (
    'data_analysis',
    'Use this skill when analyzing data, calculating statistics, or generating data reports',
    CONCAT(
        '# Data Analysis Skill\n\n',
        '## 功能概述\n',
        '提供全面的数据分析能力，包括：\n',
        '- 描述性统计分析（均值、中位数、标准差等）\n',
        '- 数据可视化建议\n',
        '- 趋势分析\n',
        '- 异常检测\n\n',
        '## 使用方法\n',
        '1. 明确分析目标\n',
        '2. 收集并清洗数据\n',
        '3. 选择合适的分析方法\n',
        '4. 解读结果并生成报告\n\n',
        '## 常用公式\n',
        '- 均值: SUM(x) / COUNT(x)\n',
        '- 标准差: SQRT(VAR_POP(x))\n',
        '- 相关系数: CORR(x, y)\n\n',
        '## 注意事项\n',
        '- 确保数据质量再进行分析\n',
        '- 选择与业务场景匹配的分析方法\n',
        '- 结果需结合业务含义解读'
    ),
    'system',
    '{"category": "analytics", "version": "1.0", "tags": ["data", "analysis", "statistics"]}'
);

-- SQL 查询技能
INSERT INTO agentscope_skills (name, description, skill_content, source, metadata_json)
VALUES (
    'sql_query',
    'Use this skill when writing SQL queries, optimizing database performance, or designing data models',
    CONCAT(
        '# SQL Query Skill\n\n',
        '## 功能概述\n',
        '帮助你编写高效的 SQL 查询，包括：\n',
        '- SELECT 查询与聚合函数\n',
        '- 多表连接（JOIN）\n',
        '- 子查询与 CTE\n',
        '- 窗口函数\n',
        '- 查询性能优化\n\n',
        '## 常用模式\n\n',
        '### 聚合查询\n',
        '```sql\n',
        'SELECT dept_id,\n',
        '       COUNT(*) as order_count,\n',
        '       SUM(amount) as total_amount,\n',
        '       AVG(amount) as avg_amount\n',
        'FROM orders\n',
        'WHERE order_date >= ''2024-01-01''\n',
        'GROUP BY dept_id\n',
        'HAVING COUNT(*) > 10\n',
        'ORDER BY total_amount DESC;\n',
        '```\n\n',
        '### 窗口函数\n',
        '```sql\n',
        'SELECT employee_id,\n',
        '       department,\n',
        '       salary,\n',
        '       RANK() OVER (PARTITION BY department ORDER BY salary DESC) as rank,\n',
        '       AVG(salary) OVER (PARTITION BY department) as dept_avg\n',
        'FROM employees;\n',
        '```\n\n',
        '## 优化建议\n',
        '- 使用合适的索引\n',
        '- 避免 SELECT *\n',
        '- 使用 LIMIT 限制结果集\n',
        '- 避免在 WHERE 中使用函数'
    ),
    'system',
    '{"category": "database", "version": "1.0", "tags": ["sql", "database", "query"]}'
);

-- 代码审查技能
INSERT INTO agentscope_skills (name, description, skill_content, source, metadata_json)
VALUES (
    'code_review',
    'Use this skill when reviewing code changes, identifying bugs, or suggesting improvements',
    CONCAT(
        '# Code Review Skill\n\n',
        '## 功能概述\n',
        '进行代码审查，发现潜在问题和改进机会：\n\n',
        '## 审查要点\n\n',
        '### 正确性\n',
        '- 逻辑是否正确\n',
        '- 边界条件处理\n',
        '- 错误处理是否完善\n\n',
        '### 安全性\n',
        '- 输入验证\n',
        '- SQL 注入防护\n',
        '- XSS 防护\n',
        '- 权限检查\n\n',
        '### 性能\n',
        '- 算法复杂度\n',
        '- 数据库查询优化\n',
        '- 缓存使用\n\n',
        '### 可维护性\n',
        '- 代码清晰度\n',
        '- 命名规范\n',
        '- 注释完整性\n',
        '- 重复代码检测\n\n',
        '## 审查输出格式\n',
        '```\n',
        '## 代码审查报告\n\n',
        '### 问题列表\n',
        '1. [严重] 文件:Line - 问题描述\n',
        '   建议: 修复方案\n\n',
        '### 建议改进\n',
        '- 改进点\n\n',
        '### 优点\n',
        '- 做得好的地方\n',
        '```'
    ),
    'system',
    '{"category": "development", "version": "1.0", "tags": ["code", "review", "quality"]}'
);
```

### 11.4.6 Chapter11Application.java

```java
package io.agentscope.tutorial.chapter11;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentScope Java 技能系统教程 - Spring Boot 启动类.
 *
 * <p>本章演示如何使用技能（Skill）系统实现：
 * <ul>
 *   <li>技能的定义与注册</li>
 *   <li>MySQL 风格存储（使用 H2 模拟）</li>
 *   <li>动态技能加载与执行</li>
 * </ul>
 */
@SpringBootApplication
public class Chapter11Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter11Application.class, args);
    }
}
```

### 11.4.7 H2SkillRepository.java（自定义 H2 实现）

```java
package io.agentscope.tutorial.chapter11.repository;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import io.agentscope.core.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.*;

/**
 * 基于 H2 的技能仓库实现.
 *
 * <p>使用 H2 模拟 MySQL 表结构，完整兼容 MysqlSkillRepository 的使用方式.
 * 在实际生产环境中替换为真实的 MySQL DataSource 即可.
 *
 * <p>表结构：
 * <ul>
 *   <li>agentscope_skills: 存储技能元数据和内容</li>
 *   <li>agentscope_skill_resources: 存储技能附带的资源文件</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Repository
public class H2SkillRepository implements AgentSkillRepository {

    private static final Logger log = LoggerFactory.getLogger(H2SkillRepository.class);

    private final javax.sql.DataSource dataSource;

    public H2SkillRepository(javax.sql.DataSource dataSource) {
        this.dataSource = dataSource;
        log.info("H2SkillRepository 初始化完成");
    }

    // ==================== AgentSkillRepository 接口实现 ====================

    @Override
    public AgentSkill getSkill(String name) {
        validateSkillName(name);

        String selectSkillSql =
            "SELECT id, name, description, skill_content, source, metadata_json " +
            "FROM agentscope_skills WHERE name = ?";

        String selectResourcesSql =
            "SELECT resource_path, resource_content " +
            "FROM agentscope_skill_resources WHERE id = ?";

        try (Connection conn = dataSource.getConnection()) {
            long skillId;
            String description;
            String skillContent;
            String source;
            String metadataJson;

            // 查询技能基本信息
            try (PreparedStatement stmt = conn.prepareStatement(selectSkillSql)) {
                stmt.setString(1, name);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Skill not found: " + name);
                    }
                    skillId = rs.getLong("id");
                    description = rs.getString("description");
                    skillContent = rs.getString("skill_content");
                    source = rs.getString("source");
                    metadataJson = rs.getString("metadata_json");
                }
            }

            // 查询技能资源
            Map<String, String> resources = new HashMap<>();
            try (PreparedStatement stmt = conn.prepareStatement(selectResourcesSql)) {
                stmt.setLong(1, skillId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        resources.put(
                            rs.getString("resource_path"),
                            rs.getString("resource_content")
                        );
                    }
                }
            }

            return buildSkill(name, description, skillContent, source, metadataJson, resources);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load skill: " + name, e);
        }
    }

    @Override
    public List<String> getAllSkillNames() {
        String sql = "SELECT name FROM agentscope_skills ORDER BY name";
        List<String> names = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list skill names", e);
        }

        return names;
    }

    @Override
    public List<AgentSkill> getAllSkills() {
        String selectAllSql =
            "SELECT id, name, description, skill_content, source, metadata_json " +
            "FROM agentscope_skills ORDER BY name";

        String selectResourcesSql =
            "SELECT id, resource_path, resource_content FROM agentscope_skill_resources";

        try (Connection conn = dataSource.getConnection()) {
            // 加载所有技能
            Map<Long, SkillRecord> skillMap = new LinkedHashMap<>();

            try (PreparedStatement stmt = conn.prepareStatement(selectAllSql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    skillMap.put(id, new SkillRecord(
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("skill_content"),
                        rs.getString("source"),
                        rs.getString("metadata_json")
                    ));
                }
            }

            // 加载所有资源
            try (PreparedStatement stmt = conn.prepareStatement(selectResourcesSql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    SkillRecord record = skillMap.get(id);
                    if (record != null) {
                        record.resources.put(
                            rs.getString("resource_path"),
                            rs.getString("resource_content")
                        );
                    }
                }
            }

            // 构建 Skill 对象列表
            List<AgentSkill> skills = new ArrayList<>();
            for (SkillRecord record : skillMap.values()) {
                try {
                    skills.add(buildSkill(
                        record.name,
                        record.description,
                        record.skillContent,
                        record.source,
                        record.metadataJson,
                        record.resources
                    ));
                } catch (Exception e) {
                    log.warn("Failed to build skill: {}", e.getMessage());
                }
            }

            return skills;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load all skills", e);
        }
    }

    @Override
    public boolean save(List<AgentSkill> skills, boolean force) {
        if (skills == null || skills.isEmpty()) {
            return false;
        }

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                for (AgentSkill skill : skills) {
                    // 如果 force=false，先检查是否存在
                    if (!force && skillExistsInternal(conn, skill.getName())) {
                        throw new IllegalStateException(
                            "Skill already exists: " + skill.getName() +
                            ". Use force=true to overwrite."
                        );
                    }

                    // 如果已存在，先删除
                    if (skillExistsInternal(conn, skill.getName())) {
                        deleteSkillInternal(conn, skill.getName());
                    }

                    // 插入新技能
                    long skillId = insertSkill(conn, skill);

                    // 插入资源
                    insertResources(conn, skillId, skill.getResources());

                    log.info("Saved skill: {} (id={})", skill.getName(), skillId);
                }

                conn.commit();
                return true;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save skills", e);
        }
    }

    @Override
    public boolean delete(String skillName) {
        validateSkillName(skillName);

        try (Connection conn = dataSource.getConnection()) {
            if (!skillExistsInternal(conn, skillName)) {
                log.warn("Skill does not exist: {}", skillName);
                return false;
            }

            conn.setAutoCommit(false);
            try {
                deleteSkillInternal(conn, skillName);
                conn.commit();
                log.info("Deleted skill: {}", skillName);
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete skill: " + skillName, e);
        }
    }

    @Override
    public boolean skillExists(String skillName) {
        if (skillName == null || skillName.isEmpty()) {
            return false;
        }

        try (Connection conn = dataSource.getConnection()) {
            return skillExistsInternal(conn, skillName);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check skill existence", e);
        }
    }

    @Override
    public AgentSkillRepositoryInfo getRepositoryInfo() {
        return new AgentSkillRepositoryInfo("h2", "agentscope.agentscope_skills", true);
    }

    @Override
    public String getSource() {
        return "h2_agentscope_agentscope_skills";
    }

    @Override
    public void close() {
        log.debug("H2SkillRepository closed");
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建 AgentSkill 对象.
     */
    private AgentSkill buildSkill(
            String name,
            String description,
            String skillContent,
            String source,
            String metadataJson,
            Map<String, String> resources) {
        Map<String, Object> metadata = deserializeMetadata(metadataJson, name, description);
        return new AgentSkill(metadata, skillContent, resources, source);
    }

    /**
     * 反序列化 metadata_json 字段.
     */
    private Map<String, Object> deserializeMetadata(String metadataJson,
                                                     String name,
                                                     String description) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        if (metadataJson != null && !metadataJson.isBlank()) {
            try {
                Map<String, Object> parsed = JsonUtils.getJsonCodec()
                    .fromJson(metadataJson, new TypeReference<Map<String, Object>>() {});
                if (parsed != null) {
                    metadata.putAll(parsed);
                }
            } catch (RuntimeException e) {
                log.warn("Failed to deserialize metadata_json, using core fields only", e);
            }
        }
        metadata.put("name", name);
        metadata.put("description", description);
        return metadata;
    }

    /**
     * 序列化 metadata 为 JSON 字符串.
     */
    private String serializeMetadata(Map<String, Object> metadata) {
        return JsonUtils.getJsonCodec().toJson(metadata);
    }

    /**
     * 检查技能是否存在（使用现有连接）.
     */
    private boolean skillExistsInternal(Connection conn, String skillName) throws SQLException {
        String sql = "SELECT 1 FROM agentscope_skills WHERE name = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, skillName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * 插入技能记录并返回生成的 ID.
     */
    private long insertSkill(Connection conn, AgentSkill skill) throws SQLException {
        String sql =
            "INSERT INTO agentscope_skills (name, description, skill_content, source, metadata_json) " +
            "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, skill.getName());
            stmt.setString(2, skill.getDescription());
            stmt.setString(3, skill.getSkillContent());
            stmt.setString(4, skill.getSource());
            stmt.setString(5, serializeMetadata(skill.getMetadata()));
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Failed to get generated key for skill: " + skill.getName());
            }
        }
    }

    /**
     * 批量插入技能资源.
     */
    private void insertResources(Connection conn, long skillId,
                                Map<String, String> resources) throws SQLException {
        if (resources == null || resources.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO agentscope_skill_resources (id, resource_path, resource_content) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Map.Entry<String, String> entry : resources.entrySet()) {
                stmt.setLong(1, skillId);
                stmt.setString(2, entry.getKey());
                stmt.setString(3, entry.getValue());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    /**
     * 删除技能（使用现有连接）.
     */
    private void deleteSkillInternal(Connection conn, String skillName) throws SQLException {
        // 资源会通过外键级联删除
        String sql = "DELETE FROM agentscope_skills WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, skillName);
            stmt.executeUpdate();
        }
    }

    /**
     * 验证技能名称.
     */
    private void validateSkillName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Skill name cannot be null or empty");
        }
    }

    // ==================== 内部类 ====================

    /**
     * 临时持有技能记录，用于批量加载时聚合资源.
     */
    private static class SkillRecord {
        final String name;
        final String description;
        final String skillContent;
        final String source;
        final String metadataJson;
        final Map<String, String> resources = new HashMap<>();

        SkillRecord(String name, String description, String skillContent,
                    String source, String metadataJson) {
            this.name = name;
            this.description = description;
            this.skillContent = skillContent;
            this.source = source;
            this.metadataJson = metadataJson;
        }
    }
}
```

### 11.4.8 SkillConfig.java（Spring 配置类）

```java
package io.agentscope.tutorial.chapter11.config;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter11.repository.H2SkillRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 技能系统配置类.
 *
 * <p>演示如何在 Spring Boot 中配置 AgentScope 的技能系统：
 * <ul>
 *   <li>技能仓库（H2 模拟 MySQL）</li>
 *   <li>SkillBox 与技能注册</li>
 *   <li>ReActAgent 集成</li>
 * </ul>
 */
@Configuration
public class SkillConfig {

    private static final String SYSTEM_PROMPT = """
        You are a helpful assistant with specialized skills.
        When a task matches a skill description, use the load_skill_through_path tool to load the skill.
        Skills are loaded progressively - you will see skill metadata first, then load full content when needed.
        """;

    // ==================== 技能仓库配置 ====================

    /**
     * 配置技能仓库（从 H2 数据库加载）.
     */
    @Bean
    public AgentSkillRepository skillRepository(H2SkillRepository h2Repository) {
        // 包装 H2Repository 实现，使其符合 AgentScope 的仓库接口
        // 实际使用时也可以直接注入 H2SkillRepository
        return h2Repository;
    }

    // ==================== SkillBox 配置 ====================

    /**
     * 配置 Toolkit 和 SkillBox.
     *
     * <p>从仓库加载所有技能并注册到 SkillBox.
     */
    @Bean
    public SkillBox skillBox(Toolkit toolkit, AgentSkillRepository skillRepository) {
        SkillBox skillBox = new SkillBox(toolkit);

        // 从仓库加载所有技能并注册
        List<AgentSkill> skills = skillRepository.getAllSkills();
        for (AgentSkill skill : skills) {
            skillBox.registerSkill(skill);
            System.out.println("Registered skill: " + skill.getName() +
                             " - " + skill.getDescription());
        }

        System.out.println("\nTotal skills registered: " + skills.size());

        return skillBox;
    }

    /**
     * 配置 Toolkit（空的工具包，可以根据需要添加工具）.
     */
    @Bean
    public Toolkit toolkit() {
        return new Toolkit();
    }

    // ==================== Agent 配置 ====================

    /**
     * 配置 ReActAgent with Skill support.
     *
     * <p>这个 Agent 可以使用加载的技能来处理复杂任务.
     */
    @Bean
    public ReActAgent skillAgent(Toolkit toolkit, SkillBox skillBox) {
        String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");

        return ReActAgent.builder()
            .name("skill_assistant")
            .sysPrompt(SYSTEM_PROMPT)
            // 如果没有配置 DashScope API Key，可以使用模拟模型进行演示
            .model(DashScopeChatModel.builder()
                .apiKey(apiKey != null ? apiKey : "demo-key")
                .modelName("qwen-plus")
                .build())
            .toolkit(toolkit)
            .skillBox(skillBox)
            .memory(new InMemoryMemory())
            .build();
    }

    // ==================== 启动时打印技能列表 ====================

    /**
     * 启动时打印所有已注册的技能信息.
     */
    @Bean
    public CommandLineRunner listSkills(SkillBox skillBox, AgentSkillRepository repository) {
        return args -> {
            System.out.println("\n========== 技能系统已就绪 ==========");
            System.out.println("仓库源: " + repository.getSource());
            System.out.println("已注册技能列表:");

            for (String name : repository.getAllSkillNames()) {
                AgentSkill skill = repository.getSkill(name);
                System.out.println("  - [" + skill.getName() + "]");
                System.out.println("    描述: " + skill.getDescription());
                System.out.println("    来源: " + skill.getSource());
                System.out.println("    资源数: " + skill.getResources().size());
                System.out.println();
            }
            System.out.println("=====================================\n");
        };
    }
}
```

### 11.4.9 SkillService.java（技能服务）

```java
package io.agentscope.tutorial.chapter11.service;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 技能服务类.
 *
 * <p>提供技能的管理和执行功能：
 * <ul>
 *   <li>技能查询（按名称或标签）</li>
 *   <li>技能 CRUD 操作</li>
 *   <li>技能执行（通过 Agent）</li>
 *   <li>技能搜索（按描述关键词）</li>
 * </ul>
 */
@Service
public class SkillService {

    private static final Logger log = LoggerFactory.getLogger(SkillService.class);

    private final AgentSkillRepository skillRepository;
    private final SkillBox skillBox;
    private final ReActAgent skillAgent;

    public SkillService(
            AgentSkillRepository skillRepository,
            SkillBox skillBox,
            ReActAgent skillAgent) {
        this.skillRepository = skillRepository;
        this.skillBox = skillBox;
        this.skillAgent = skillAgent;
    }

    // ==================== 技能查询 ====================

    /**
     * 获取所有技能名称.
     */
    public List<String> getAllSkillNames() {
        return skillRepository.getAllSkillNames();
    }

    /**
     * 获取所有技能.
     */
    public List<AgentSkill> getAllSkills() {
        return skillRepository.getAllSkills();
    }

    /**
     * 根据名称获取技能.
     */
    public Optional<AgentSkill> getSkill(String name) {
        try {
            return Optional.of(skillRepository.getSkill(name));
        } catch (IllegalArgumentException e) {
            log.debug("Skill not found: {}", name);
            return Optional.empty();
        }
    }

    /**
     * 检查技能是否存在.
     */
    public boolean skillExists(String name) {
        return skillRepository.skillExists(name);
    }

    /**
     * 按关键词搜索技能（匹配名称或描述）.
     */
    public List<AgentSkill> searchSkills(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllSkills();
        }

        String lowerKeyword = keyword.toLowerCase();
        return getAllSkills().stream()
            .filter(skill ->
                skill.getName().toLowerCase().contains(lowerKeyword) ||
                skill.getDescription().toLowerCase().contains(lowerKeyword)
            )
            .toList();
    }

    // ==================== 技能管理 ====================

    /**
     * 添加新技能.
     *
     * @param skill 要添加的技能
     * @param force 如果技能已存在是否覆盖
     * @return 是否添加成功
     */
    public boolean addSkill(AgentSkill skill, boolean force) {
        try {
            boolean saved = skillRepository.save(List.of(skill), force);
            if (saved) {
                // 重新注册到 SkillBox
                skillBox.registerSkill(skill);
                log.info("Skill added and registered: {}", skill.getName());
            }
            return saved;
        } catch (Exception e) {
            log.error("Failed to add skill: {}", skill.getName(), e);
            return false;
        }
    }

    /**
     * 更新技能.
     *
     * @param skill 更新后的技能
     * @return 是否更新成功
     */
    public boolean updateSkill(AgentSkill skill) {
        return addSkill(skill, true);  // force=true 覆盖现有技能
    }

    /**
     * 删除技能.
     *
     * @param name 技能名称
     * @return 是否删除成功
     */
    public boolean deleteSkill(String name) {
        boolean deleted = skillRepository.delete(name);
        if (deleted) {
            log.info("Skill deleted: {}", name);
            // 注意：SkillBox 不提供 unregister 方法，
            // 重新构建 SkillBox 或重启应用可以清除已删除的技能
        }
        return deleted;
    }

    /**
     * 从 Markdown 和资源创建并添加技能.
     */
    public boolean createAndAddSkill(String name, String description, String content,
                                      Map<String, String> resources) {
        AgentSkill skill = AgentSkill.builder()
            .name(name)
            .description(description)
            .skillContent(content)
            .resources(resources != null ? resources : Map.of())
            .source("custom")
            .build();

        return addSkill(skill, false);
    }

    // ==================== 技能执行 ====================

    /**
     * 使用技能执行任务.
     *
     * <p>通过 Agent 调用技能来处理用户请求.
     *
     * @param userInput 用户输入
     * @return Agent 响应文本
     */
    public String executeWithSkills(String userInput) {
        Msg userMsg = Msg.builder()
            .role(MsgRole.USER)
            .content(TextBlock.builder().text(userInput).build())
            .build();

        try {
            // 调用 Agent（支持 Skill 的渐进式加载）
            Msg response = skillAgent.call(userMsg).block();
            return response != null ? response.getTextContent() : "No response";
        } catch (Exception e) {
            log.error("Failed to execute with skills", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 直接加载并返回技能内容（不通过 Agent）.
     *
     * @param skillName 技能名称
     * @return 技能内容，如果不存在返回空
     */
    public Optional<String> loadSkillContent(String skillName) {
        return getSkill(skillName).map(AgentSkill::getSkillContent);
    }

    /**
     * 获取技能的资源文件内容.
     *
     * @param skillName 技能名称
     * @param resourcePath 资源路径
     * @return 资源内容，如果不存在返回空
     */
    public Optional<String> getSkillResource(String skillName, String resourcePath) {
        return getSkill(skillName)
            .map(skill -> skill.getResource(resourcePath));
    }

    // ==================== 仓库信息 ====================

    /**
     * 获取仓库信息.
     */
    public String getRepositoryInfo() {
        return skillRepository.getRepositoryInfo().toString();
    }
}
```

### 11.4.10 SkillController.java（REST API 控制器）

```java
package io.agentscope.tutorial.chapter11.controller;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.tutorial.chapter11.service.SkillService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 技能系统 REST API.
 *
 * <p>提供技能的查询、管理和执行接口：
 * <ul>
 *   <li>GET  /api/skills - 获取所有技能</li>
 *   <li>GET  /api/skills/{name} - 获取指定技能</li>
 *   <li>GET  /api/skills/search?keyword=xxx - 搜索技能</li>
 *   <li>POST /api/skills - 添加新技能</li>
 *   <li>DELETE /api/skills/{name} - 删除技能</li>
 *   <li>POST /api/skills/execute - 使用技能执行任务</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    /**
     * 获取所有技能.
     */
    @GetMapping
    public List<String> getAllSkillNames() {
        return skillService.getAllSkillNames();
    }

    /**
     * 获取指定技能详情.
     */
    @GetMapping("/{name}")
    public Map<String, Object> getSkill(@PathVariable String name) {
        return skillService.getSkill(name)
            .map(skill -> Map.<String, Object>of(
                "name", skill.getName(),
                "description", skill.getDescription(),
                "skillContent", skill.getSkillContent(),
                "source", skill.getSource(),
                "resourceCount", skill.getResources().size(),
                "metadata", skill.getMetadata()
            ))
            .orElse(Map.of("error", "Skill not found: " + name));
    }

    /**
     * 搜索技能.
     */
    @GetMapping("/search")
    public List<Map<String, String>> searchSkills(@RequestParam String keyword) {
        return skillService.searchSkills(keyword).stream()
            .map(skill -> Map.of(
                "name", skill.getName(),
                "description", skill.getDescription()
            ))
            .toList();
    }

    /**
     * 添加新技能（从请求体构建）.
     */
    @PostMapping
    public Map<String, Object> addSkill(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        String content = (String) request.get("content");
        @SuppressWarnings("unchecked")
        Map<String, String> resources = (Map<String, String>) request.get("resources");

        boolean success = skillService.createAndAddSkill(name, description, content, resources);

        return Map.of(
            "success", success,
            "message", success ? "Skill added: " + name : "Failed to add skill"
        );
    }

    /**
     * 删除技能.
     */
    @DeleteMapping("/{name}")
    public Map<String, Object> deleteSkill(@PathVariable String name) {
        boolean success = skillService.deleteSkill(name);
        return Map.of(
            "success", success,
            "message", success ? "Skill deleted: " + name : "Skill not found or delete failed"
        );
    }

    /**
     * 使用技能执行任务.
     */
    @PostMapping("/execute")
    public Map<String, Object> executeWithSkills(@RequestBody Map<String, String> request) {
        String userInput = request.get("input");
        if (userInput == null || userInput.isBlank()) {
            return Map.of("error", "input is required");
        }

        String response = skillService.executeWithSkills(userInput);
        return Map.of(
            "input", userInput,
            "response", response
        );
    }

    /**
     * 获取仓库信息.
     */
    @GetMapping("/info")
    public Map<String, Object> getInfo() {
        return Map.of(
            "repository", skillService.getRepositoryInfo(),
            "totalSkills", skillService.getAllSkillNames().size()
        );
    }
}
```

### 11.4.11 运行与测试

**1. 编译运行**

```bash
cd chapter11-skills
./mvnw spring-boot:run
```

**2. 访问 H2 控制台**（开发时查看数据）

```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:agentscope
```

**3. REST API 测试**

```bash
# 获取所有技能
curl http://localhost:8080/api/skills

# 获取技能详情
curl http://localhost:8080/api/skills/data_analysis

# 搜索技能
curl "http://localhost:8080/api/skills/search?keyword=data"

# 添加新技能
curl -X POST http://localhost:8080/api/skills \
  -H "Content-Type: application/json" \
  -d '{"name":"python_script","description":"Use for Python scripting","content":"# Python Script\n..."}'

# 使用技能执行任务（需配置 AI_DASHSCOPE_API_KEY）
curl -X POST http://localhost:8080/api/skills/execute \
  -H "Content-Type: application/json" \
  -d '{"input":"分析这份销售数据的趋势"}'
```

### 11.4.12 完整工作流程示例

```java
// 完整的技能使用流程示例
public class SkillUsageExample {

    public static void main(String[] args) {
        // 1. 创建技能
        AgentSkill sqlSkill = AgentSkill.builder()
            .name("advanced_sql")
            .description("Use for complex SQL queries and database optimization")
            .skillContent("# Advanced SQL Skill\n\n## 窗口函数\n...")
            .addResource("references/window-functions.md", "# Window Functions Reference\n...")
            .source("tutorial")
            .build();

        // 2. 保存到仓库
        skillRepository.save(List.of(sqlSkill), false);

        // 3. 注册到 SkillBox
        skillBox.registerSkill(sqlSkill);

        // 4. 通过 Agent 使用
        String userQuery = "Write a SQL to find top 10 customers by order volume";
        String response = skillService.executeWithSkills(userQuery);

        System.out.println("Response: " + response);

        // 5. 直接加载技能内容
        Optional<String> content = skillService.loadSkillContent("advanced_sql");
        content.ifPresent(c -> System.out.println("Skill content length: " + c.length()));
    }
}
```

## 11.5 代码执行能力

Skill 系统还支持为技能提供隔离的代码执行环境：

```java
// 启用代码执行工具
skillBox.codeExecution()
    .workDir("/data/agent-workspace")           // 工作目录
    .uploadDir("/data/agent-workspace/my-skills") // 资源上传目录
    .withShell()    // Shell 命令工具
    .withRead()     // 读文件工具
    .withWrite()    // 写文件工具
    .enable();

// 自定义文件过滤
skillBox.codeExecution()
    .includeFolders(Set.of("scripts/", "data/"))
    .includeExtensions(Set.of(".py", ".json"))
    .withShell()
    .enable();

// 自定义 Shell（指定允许的命令）
ShellCommandTool customShell = new ShellCommandTool(
    null,
    Set.of("python3", "node", "npm"),
    command -> askUserApproval(command)  // 命令审批回调
);

skillBox.codeExecution()
    .withShell(customShell)
    .enable();
```

## 11.6 性能优化建议

1. **控制 SKILL.md 大小**：保持在 5k tokens 以内，建议 1.5-2k tokens
2. **合理组织资源**：将详细文档放在 `references/` 中，而非 SKILL.md
3. **定期清理版本**：使用仓库的 `clearAllSkills()` 清理不再需要的旧技能
4. **避免重复注册**：AgentScope 有重复注册保护机制，相同 Skill 配多个 Tool 不会创建重复版本

## 11.7 小结

本章介绍了 AgentScope Java 的技能系统：

| 概念 | 说明 |
|------|------|
| **AgentSkill** | 技能的数据模型，包含名称、描述、内容、资源和元数据 |
| **SkillBox** | 技能容器，管理技能的注册和系统提示词注入 |
| **渐进式披露** | 三阶段按需加载机制，优化上下文大小 |
| **AgentSkillRepository** | 技能仓库接口，支持 MySQL、Git、Classpath 等多种存储 |
| **load_skill_through_path** | 内置工具，用于按需加载技能内容和资源 |
| **Tool 绑定** | 可将 Tool 与 Skill 绑定，仅在 Skill 激活时生效 |

通过案例我们看到，技能系统与 Spring Boot 可以很好地集成，使用 H2 模拟 MySQL 表结构进行开发和测试，实际生产环境只需替换数据源即可。

## 相关文档

- [Agent 配置](./agent.md) - 智能体配置和使用
- [Tool 使用指南](./tool.md) - 工具系统的使用方法
- [Claude Agent Skills 官方文档](https://platform.claude.com/docs/zh-CN/agents-and-tools/agent-skills/overview) - 完整的概念和架构介绍