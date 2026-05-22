# 第十五章：Harness 安全执行环境

Harness 是 AgentScope Java 框架为 Agent 提供的一套安全、隔离、可持久化的执行环境。相比基础的 ReActAgent，HarnessAgent 在以下方面提供了增强：

- **工作区管理**：基于磁盘的上下文加载（AGENTS.md、MEMORY.md 等）
- **沙箱隔离**：支持 Docker、Kubernetes 等容器化执行环境，代码在隔离环境中运行
- **文件系统抽象**：统一的文件系统 API，支持本地、沙箱、远程等多种后端
- **内存刷新与压缩**：对话过长时自动触发摘要/压缩，防止上下文溢出
- **会话持久化**：支持跨会话恢复 Agent 状态和沙箱工作区

本章将深入剖析 Harness 的架构设计，并通过完整案例演示其核心功能。

## 15.1 Harness 架构与原理

### 15.1.1 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      HarnessAgent                            │
│  (包装 ReActAgent，提供 Harness 增强功能)                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌───────────────┐   ┌───────────────┐   ┌───────────────┐ │
│  │  WorkspaceMgr │   │  CompactionHook│   │ SandboxLifecycle│ │
│  │ (工作区管理)    │   │ (上下文压缩)    │   │ Hook (沙箱生命周期)│ │
│  └───────┬───────┘   └───────┬───────┘   └───────┬───────┘ │
│          │                   │                   │          │
│  ┌───────▼───────────────────▼───────────────────▼───────┐ │
│  │               AbstractFilesystem                        │ │
│  │         (统一文件系统抽象层)                             │ │
│  └───┬─────────┬─────────────┬─────────────┬───────────────┘ │
│      │         │             │             │                  │
│  ┌───▼───┐ ┌──▼───┐    ┌────▼────┐  ┌─────▼─────┐            │
│  │LocalFS│ │Sandbox│   │RemoteFS │  │CompositeFS│            │
│  │(本地) │ │(沙箱) │   │(远程)   │  │(复合)     │            │
│  └───────┘ └───┬───┘   └─────────┘  └───────────┘            │
│                │                                               │
│         ┌──────▼──────┐                                        │
│         │   Sandbox   │                                        │
│         │ (容器/进程)  │                                        │
│         └─────────────┘                                        │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐   │
│  │              SandboxManager                            │   │
│  │  (沙箱生命周期管理：创建/恢复/持久化/销毁)              │   │
│  └───────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
```

### 15.1.2 核心组件

**HarnessAgent** 是用户级 API，内部委托 ReActAgent 完成核心推理，同时通过 Hook 机制注入 Harness 增强能力：

```java
// HarnessAgent 的核心构造逻辑
HarnessAgent agent = HarnessAgent.builder()
    .name("my-agent")
    .model(dashscopeModel())
    .sysPrompt("你是一个有帮助的助手")
    .workspace("/path/to/workspace")        // 工作区目录
    .filesystem(sandboxFilesystemSpec)       // 文件系统后端（沙箱模式）
    .compaction(CompactionConfig.defaults()) // 上下文压缩配置
    .build();
```

**关键设计点**：

1. **文件系统后端可插拔**：支持本地文件系统（LocalFilesystem）、沙箱文件系统（Docker/K8s）、远程文件系统（分布式存储）三种模式
2. **沙箱生命周期自动化**：通过 SandboxLifecycleHook 在每次 Agent 调用前自动获取/创建沙箱，调用后自动持久化/释放
3. **隔离作用域（IsolationScope）**：支持 USER（按用户隔离）、SESSION（按会话隔离）、AGENT（按 Agent 隔离）三种粒度

### 15.1.3 Sandbox 接口定义

```java
/**
 * 活跃的沙箱实例，提供完全隔离的工作区
 *
 * 生命周期：
 *  1. 通过 SandboxClient.create() 创建（或 resume() 恢复）
 *  2. 调用 start() —— 初始化或恢复工作区
 *  3. 使用 exec() 执行命令，persistWorkspace()/hydrateWorkspace() 处理归档
 *  4. 调用 stop() —— 持久化快照（不销毁资源）
 *  5. 调用 shutdown() —— 销毁后端资源（容器、临时目录等）
 *  6. 或直接调用 close()，自动执行 stop + shutdown
 */
public interface Sandbox extends AutoCloseable {

    /** 初始化或恢复沙箱工作区 */
    void start() throws Exception;

    /** 持久化快照（安全操作，对 self-managed 和 user-managed 沙箱均适用） */
    void stop() throws Exception;

    /** 销毁后端资源（仅 self-managed 沙箱调用） */
    default void shutdown() throws Exception { }

    @Override
    void close() throws Exception;

    /** 是否正在运行 */
    boolean isRunning();

    /** 获取当前可序列化状态 */
    SandboxState getState();

    /**
     * 在沙箱工作区中执行 shell 命令
     *
     * @param runtimeContext   每次调用的 Agent 上下文（session、user、attributes）
     * @param command          shell 命令
     * @param timeoutSeconds   超时时间（null 使用实现默认值）
     */
    ExecResult exec(RuntimeContext runtimeContext, String command, Integer timeoutSeconds)
            throws Exception;

    /** 将工作区持久化为 tar 归档流 */
    InputStream persistWorkspace() throws Exception;

    /** 从 tar 归档流恢复工作区 */
    void hydrateWorkspace(InputStream archive) throws Exception;
}
```

### 15.1.4 SandboxClient 工厂接口

```java
/**
 * 创建和恢复 Sandbox 实例的工厂
 *
 * @param <O> 客户端选项类型
 */
public interface SandboxClient<O extends SandboxClientOptions> {

    /** 创建新沙箱（返回预启动状态，需调用 start()） */
    Sandbox create(WorkspaceSpec workspaceSpec, SandboxSnapshotSpec snapshotSpec, O options);

    /** 从已序列化的 SandboxState 恢复沙箱 */
    Sandbox resume(SandboxState state);

    /** 删除沙箱 */
    void delete(Sandbox sandbox);

    /** 序列化状态为 JSON 字符串 */
    String serializeState(SandboxState state);

    /** 从 JSON 字符串反序列化状态 */
    SandboxState deserializeState(String json);
}
```

## 15.2 沙箱隔离机制

### 15.2.1 隔离模式概述

Harness 支持三种沙箱隔离模式：

| 模式 | 隔离粒度 | 使用场景 | 说明 |
|------|----------|----------|------|
| `USER` | 用户级别 | 多用户共享 Agent，每个用户有独立工作区 | 同用户的所有会话共享同一个沙箱实例 |
| `SESSION` | 会话级别 | 每个会话完全隔离 | 同用户的不同会话各有独立的沙箱 |
| `AGENT` | Agent 级别 | 每个 Agent 实例隔离 | 最严格的隔离，适用于高安全需求 |

### 15.2.2 优先级获取策略

SandboxManager 使用以下优先级获取沙箱实例：

```
Priority 1: 用户提供的外部沙箱（externalSandbox）
Priority 2: 用户提供的外部状态（externalSandboxState）
Priority 3: 从持久化存储恢复（基于 IsolationScope key）
Priority 4: 创建全新沙箱
```

```java
/**
 * 获取沙箱实例的核心逻辑
 *
 * 当 IsolationScope key 存在时，会先尝试通过 SandboxExecutionGuard
 * 获取执行租约（SandboxLease），确保并发安全
 */
public SandboxAcquireResult acquire(SandboxContext sandboxContext, RuntimeContext runtimeContext) {
    // Priority 1: 外部沙箱（跳过 guard）
    if (sandboxContext.getExternalSandbox() != null) {
        return SandboxAcquireResult.userManaged(externalSandbox);
    }

    // Priority 2: 外部状态（跳过 guard）
    if (sandboxContext.getExternalSandboxState() != null) {
        Sandbox sandbox = client.resume(sandboxContext.getExternalSandboxState());
        return SandboxAcquireResult.selfManaged(sandbox);
    }

    // Priority 3/4: Harness 管理（应用 guard）
    Optional<SandboxIsolationKey> scopeKey =
            SandboxIsolationKey.resolve(sandboxContext.getIsolationScope(), runtimeContext, agentId);

    SandboxLease lease = SandboxLease.noop();
    if (scopeKey.isPresent()) {
        lease = executionGuard.tryEnter(scopeKey.get());  // 获取执行租约
    }

    // 尝试从持久化存储恢复
    if (scopeKey.isPresent()) {
        Optional<String> stateJson = stateStore.load(scopeKey.get());
        if (stateJson.isPresent()) {
            SandboxState state = client.deserializeState(stateJson.get());
            Sandbox sandbox = client.resume(state);
            return SandboxAcquireResult.selfManaged(sandbox, lease);
        }
    }

    // Priority 4: 创建新沙箱
    Sandbox sandbox = client.create(spec, snapshotSpec, clientOptions);
    return SandboxAcquireResult.selfManaged(sandbox, lease);
}
```

### 15.2.3 Docker 沙箱配置

`DockerFilesystemSpec` 提供了简洁的 Docker 沙箱配置接口：

```java
// 创建 Docker 沙箱文件系统规范
DockerFilesystemSpec fsSpec = new DockerFilesystemSpec()
    // Docker 镜像（必需）
    .image("python:3.11-slim")
    // 工作区根目录（容器内）
    .workspaceRoot("/workspace")
    // 环境变量
    .environment(Map.of(
        "PYTHONPATH", "/workspace",
        "ENV_VAR", "value"
    ))
    // 内存限制（字节）
    .memorySizeBytes(2L * 1024 * 1024 * 1024)  // 2GB
    // CPU 数量
    .cpuCount(2L)
    // 暴露端口
    .exposedPorts(8080, 5432)
    // Docker 网络
    .network("agentscope-net")
    // 额外的 docker run 参数
    .additionalRunArgs("--cap-add=SYS_PTRACE", "-e", "DEBUG=true")
    // 快照策略（用于分布式场景）
    .snapshotSpec(new RedisSnapshotSpec(redisClient))
    // 工作区规范（初始文件、技能等）
    .workspaceSpec(workspaceSpec);
```

## 15.3 文件系统管理

### 15.3.1 AbstractFilesystem 接口

Harness 提供统一的文件系统抽象，支持在本地、沙箱、远程等多种后端上执行相同的文件操作：

```java
public interface AbstractFilesystem {

    /** 列出目录内容（含元数据） */
    LsResult ls(RuntimeContext runtimeContext, String path);

    /** 读取文件内容（支持分页） */
    ReadResult read(RuntimeContext runtimeContext, String filePath, int offset, int limit);

    /** 创建新文件（文件已存在则报错） */
    WriteResult write(RuntimeContext runtimeContext, String filePath, String content);

    /** 精确字符串替换编辑文件 */
    EditResult edit(RuntimeContext runtimeContext, String filePath,
                    String oldString, String newString, boolean replaceAll);

    /** 搜索文件内容（字面量，非正则） */
    GrepResult grep(RuntimeContext runtimeContext, String pattern, String path, String glob);

    /** 查找匹配 glob 模式的文件 */
    GlobResult glob(RuntimeContext runtimeContext, String pattern, String path);

    /** 上传多个文件（路径->内容映射） */
    List<FileUploadResponse> uploadFiles(RuntimeContext runtimeContext,
                                         List<Map.Entry<String, byte[]>> files);

    /** 下载多个文件 */
    List<FileDownloadResponse> downloadFiles(RuntimeContext runtimeContext, List<String> paths);

    /** 删除文件或目录（递归） */
    WriteResult delete(RuntimeContext runtimeContext, String path);

    /** 移动/重命名文件或目录 */
    WriteResult move(RuntimeContext runtimeContext, String fromPath, String toPath);

    /** 检查路径是否存在 */
    boolean exists(RuntimeContext runtimeContext, String path);
}
```

### 15.3.2 文件系统模式

Harness 支持三种文件系统模式：

```java
// 模式 1：本地文件系统（默认）
// Agent 工作区是本地目录，shell 命令在宿主机执行
HarnessAgent.builder()
    .workspace("/path/to/workspace")
    .filesystem(new LocalFilesystemSpec())  // 或不配置，默认即为本地模式
    ...

// 模式 2：沙箱文件系统（完全隔离）
// 所有文件操作和 shell 执行都在沙箱容器中进行
HarnessAgent.builder()
    .workspace("/path/to/workspace")
    .filesystem(new DockerFilesystemSpec().image("python:3.11-slim"))
    ...

// 模式 3：复合文件系统（本地 + 远程）
// 本地工作区与共享远程存储（如 Redis）混合
// shell 执行不可用；特定前缀（MEMORY.md、memory/、agents/.../sessions/）
// 会路由到远程存储以保持多副本间的一致性
HarnessAgent.builder()
    .workspace("/path/to/workspace")
    .filesystem(new RemoteFilesystemSpec(redisClient))
    ...
```

### 15.3.3 路径验证

所有文件系统操作都会进行安全验证，防止路径遍历攻击：

```java
static void validatePath(String path) {
    if (path == null || path.isBlank()) {
        throw new IllegalArgumentException("Path must not be null or blank");
    }
    if (path.contains("..")) {
        throw new IllegalArgumentException("Path traversal ('..') not allowed: " + path);
    }
}
```

## 15.4 工作区管理

### 15.4.1 工作区目录结构

HarnessAgent 的工作区遵循标准化的目录布局：

```
workspace/
├── AGENTS.md                    # Agent 行为定义和技能说明
├── MEMORY.md                    # 长期记忆内容
├── memory/                      # 按日期组织的记忆碎片
│   └── YYYY-MM-DD.md
├── skills/                      # 可加载的技能包
│   └── <skill-name>/
│       └── SKILL.md
├── knowledge/                   # 领域知识文档
│   └── KNOWLEDGE.md
│   └── *.md
├── subagents/                    # 子 Agent 声明
│   └── <id>.md
├── agents/                      # 隔离子 Agent 的运行时工作区
│   └── <agentId>/
│       └── workspace/
│       └── sessions/
│           ├── sessions.json
│           └── <sessionId>.log.jsonl
└── tasks/                       # 后台任务输出
    └── <taskId>.json
```

### 15.4.2 WorkspaceManager

WorkspaceManager 提供工作区内容的只读访问，采用双层读取架构：

```java
/**
 * 工作区管理器：两层读取架构
 *
 * 读取路径：先查文件系统层（支持 user/session 隔离），若返回空则读取本地磁盘
 * 写入路径：所有写入经由 AbstractFilesystem
 * 列表路径：文件系统层和本地磁盘的并集，按相对路径去重
 */
public class WorkspaceManager {
    private final Path workspaceRoot;
    private final AbstractFilesystem filesystem;

    /** 加载 AGENTS.md */
    public String loadAgentsMd() { ... }

    /** 加载 MEMORY.md */
    public String loadMemoryMd() { ... }

    /** 加载 KNOWLEDGE.md */
    public String loadKnowledgeMd() { ... }

    /** 列出记忆文件 */
    public List<FileInfo> listMemoryFiles() { ... }

    /** 列出知识文件 */
    public List<FileInfo> listKnowledgeFiles() { ... }

    /** 列出会话日志 */
    public List<FileInfo> listSessionLogs() { ... }
}
```

### 15.4.3 工作区上下文钩子

WorkspaceContextHook 负责将工作区内容注入 Agent 的系统提示词：

```java
/**
 * 工作区上下文注入逻辑
 *
 * 系统提示词增强顺序：
 * 1. 基础 sysPrompt（用户配置）
 * 2. 环境信息（OS、日期、工作区路径）
 * 3. AGENTS.md 内容（若存在）
 * 4. MEMORY.md 内容（若存在）
 * 5. KNOWLEDGE.md 内容（若存在）
 */
public class WorkspaceContextHook implements Hook {
    // 注入格式（可切换为 legacy XML 风格）
    // Markdown 风格：
    // # Environment
    // OS: ..., Date: ..., Workspace: ...
    //
    // # Loaded Context
    // ## AGENTS.md
    // [内容]
    // ## MEMORY.md
    // [内容]
    ...
}
```

## 15.5 【案例】代码执行环境

本案例展示如何使用 Spring Boot 3 + Java 21 构建一个带 Harness 沙箱隔离的代码执行服务。

### 15.5.1 项目结构

```
chapter15-harness/
├── pom.xml
├── src/main/java/io/agentscope/tutorial/chapter15/
│   ├── Chapter15Application.java           # Spring Boot 入口
│   ├── config/
│   │   └── HarnessConfig.java              # Harness 配置类
│   ├── service/
│   │   ├── CodeExecutionService.java       # 代码执行服务
│   │   └── WorkspaceService.java           # 工作区管理服务
│   ├── sandbox/
│   │   ├── InMemorySandbox.java             # 内存沙箱实现（演示用）
│   │   ├── InMemorySandboxClient.java      # 沙箱客户端
│   │   ├── InMemorySandboxState.java       # 沙箱状态
│   │   └── InMemorySandboxStateStore.java  # 状态存储
│   ├── controller/
│   │   └── CodeExecutionController.java    # REST 控制器
│   └── model/
│       ├── ExecutionRequest.java           # 执行请求
│       └── ExecutionResult.java           # 执行结果
└── src/main/resources/
    └── application.yml
```

### 15.5.2 pom.xml

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
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <groupId>io.agentscope.tutorial</groupId>
    <artifactId>chapter15-harness</artifactId>
    <version>1.0.0</version>
    <name>chapter15-harness</name>
    <description>AgentScope Java 教程 - 第十五章：Harness 安全执行环境</description>

    <properties>
        <java.version>21</java.version>
        <agentscope.version>1.1.0-SNAPSHOT</agentscope.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- AgentScope Harness -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-harness</artifactId>
            <version>${agentscope.version}</version>
        </dependency>

        <!-- AgentScope Core（用于模型配置） -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-core</artifactId>
            <version>${agentscope.version}</version>
        </dependency>

        <!-- Apache Commons Compress（用于 tar 归档） -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-compress</artifactId>
        </dependency>

        <!-- Lombok -->
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
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 15.5.3 application.yml

```yaml
spring:
  application:
    name: chapter15-harness
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

server:
  port: 8787

# Harness 配置
harness:
  workspace:
    root: ${USER_WORKSPACE:/tmp/agentscope/workspace}
    auto-create: true
  sandbox:
    # 隔离模式：USER / SESSION / AGENT
    isolation-scope: USER
    # 默认超时（秒）
    default-timeout: 60
  agent:
    max-iterations: 15
    enable-tracing: true

# 日志配置
logging:
  level:
    io.agentscope: DEBUG
    io.agentscope.harness: INFO
```

### 15.5.4 Harness 配置类 - config/HarnessConfig.java

```java
package io.agentscope.tutorial.chapter15.config;

import io.agentscope.core.model.Model;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.IsolationScope;
import io.agentscope.harness.agent.sandbox.SandboxDistributedOptions;
import io.agentscope.harness.agent.sandbox.SandboxStateStore;
import io.agentscope.tutorial.chapter15.sandbox.InMemorySandboxClient;
import io.agentscope.tutorial.chapter15.sandbox.InMemorySandboxFilesystemSpec;
import io.agentscope.tutorial.chapter15.sandbox.InMemorySandboxStateStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Harness 配置类
 *
 * 配置 HarnessAgent 的核心参数，包括工作区、沙箱、文件系统等。
 *
 * <p>本配置演示使用内存沙箱（InMemorySandbox）进行隔离执行。
 * 生产环境应替换为 DockerFilesystemSpec 或 KubernetesFilesystemSpec。
 */
@Configuration
public class HarnessConfig {

    private static final Logger log = LoggerFactory.getLogger(HarnessConfig.class);

    @Value("${harness.workspace.root:/tmp/agentscope/workspace}")
    private String workspaceRoot;

    @Value("${harness.sandbox.isolation-scope:USER}")
    private String isolationScopeStr;

    @Value("${harness.sandbox.default-timeout:60}")
    private int defaultTimeout;

    @Value("${harness.agent.max-iterations:15}")
    private int maxIterations;

    @Value("${harness.agent.enable-tracing:true}")
    private boolean enableTracing;

    /**
     * 工作区根目录
     */
    @Bean
    public Path workspacePath() {
        Path path = Paths.get(workspaceRoot);
        log.info("Workspace root: {}", path);
        return path;
    }

    /**
     * 沙箱状态存储
     *
     * 使用内存存储（演示用）。生产环境应使用 RedisSandboxStateStore
     * 或其他分布式存储以支持跨实例恢复。
     */
    @Bean
    public InMemorySandboxStateStore sandboxStateStore() {
        return new InMemorySandboxStateStore();
    }

    /**
     * 沙箱客户端
     *
     * 创建内存沙箱客户端，用于在进程内创建隔离的执行环境。
     * 演示目的：生产环境替换为 DockerSandboxClient 或 KubernetesSandboxClient。
     */
    @Bean
    public InMemorySandboxClient sandboxClient() {
        return new InMemorySandboxClient(defaultTimeout);
    }

    /**
     * 沙箱文件系统规范
     *
     * 配置 Harness 使用内存沙箱作为文件系统后端。
     * 所有文件操作（ls、read、write、exec）都会路由到 InMemorySandbox。
     */
    @Bean
    public InMemorySandboxFilesystemSpec sandboxFilesystemSpec(
            InMemorySandboxClient client,
            InMemorySandboxStateStore stateStore) {

        IsolationScope scope = IsolationScope.valueOf(isolationScopeStr.toUpperCase());

        return new InMemorySandboxFilesystemSpec(client)
                .isolationScope(scope)
                .sandboxStateStore(stateStore);
    }

    /**
     * 代码执行 Agent
     *
     * 创建专门用于代码执行的 HarnessAgent：
     * - 工作区隔离：通过 IsolationScope 实现
     * - 文件系统：内存沙箱后端
     * - 上下文压缩：防止长对话溢出
     * - 执行追踪：记录推理和工具调用过程
     */
    @Bean
    public HarnessAgent codeExecutionAgent(
            Model model,
            Path workspacePath,
            InMemorySandboxFilesystemSpec fsSpec) {

        String sysPrompt = """
                你是一个专业的代码执行助手。你的职责是：
                1. 理解用户的代码执行请求
                2. 在沙箱环境中编写和运行代码
                3. 返回执行结果和输出
                4. 解释代码行为和潜在问题

                安全规则：
                - 只允许执行安全的代码操作
                - 不执行任何可能破坏系统的命令
                - 所有代码在隔离的沙箱环境中运行
                """;

        return HarnessAgent.builder()
                .name("code-execution-agent")
                .description("代码执行助手 - Harness 沙箱隔离版")
                .sysPrompt(sysPrompt)
                .model(model)
                .workspace(workspacePath)
                .filesystem(fsSpec)
                .maxIters(maxIterations)
                .enableAgentTracingLog(enableTracing)
                // 配置分布式沙箱选项（单节点模式，跳过分布式验证）
                .sandboxDistributed(
                        SandboxDistributedOptions.builder()
                                .requireDistributed(false)
                                .build())
                // 禁用不需要的工具，保留核心功能
                .disableMemoryTools()
                .build();
    }

    /**
     * 解析隔离作用域枚举
     */
    public IsolationScope getIsolationScope() {
        return IsolationScope.valueOf(isolationScopeStr.toUpperCase());
    }

    /**
     * 获取默认超时时间
     */
    public int getDefaultTimeout() {
        return defaultTimeout;
    }
}
```

### 15.5.5 内存沙箱实现 - sandbox/InMemorySandbox.java

```java
package io.agentscope.tutorial.chapter15.sandbox;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.sandbox.ExecResult;
import io.agentscope.harness.agent.sandbox.Sandbox;
import io.agentscope.harness.agent.sandbox.SandboxState;
import io.agentscope.harness.agent.sandbox.WorkspaceProjectionApplier;
import io.agentscope.harness.agent.sandbox.WorkspaceSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * 内存沙箱实现（演示用）
 *
 * <p>在进程内的临时目录中模拟隔离的代码执行环境。
 * 所有文件操作和命令执行都在独立的临时工作区中进行。
 *
 * <p>生产环境应使用 DockerSandbox 或 KubernetesSandbox 以获得真正的容器级隔离。
 *
 * <p>生命周期：
 * <ol>
 *   <li>构造函数：设置工作区路径和超时</li>
 *   <li>start()：创建工作区目录，应用 WorkspaceProjection</li>
 *   <li>exec()：在沙箱工作区中执行命令</li>
 *   <li>persistWorkspace()/hydrateWorkspace()：处理工作区归档</li>
 *   <li>stop()：标记工作区就绪（用于快照）</li>
 *   <li>shutdown()：清理临时目录</li>
 * </ol>
 *
 * @author AgentScope Tutorial
 */
public class InMemorySandbox implements Sandbox {

    private static final Logger log = LoggerFactory.getLogger(InMemorySandbox.class);

    private final InMemorySandboxState state;
    private final Path workspaceDir;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final int defaultTimeoutSeconds;

    /**
     * 创建内存沙箱实例
     *
     * @param state               沙箱状态（包含工作区路径等）
     * @param defaultTimeoutSeconds 默认命令超时（秒）
     */
    public InMemorySandbox(InMemorySandboxState state, int defaultTimeoutSeconds) {
        this.state = Objects.requireNonNull(state, "state must not be null");
        this.workspaceDir = Path.of(state.getWorkspaceRoot());
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
    }

    /**
     * 初始化沙箱
     *
     * <p>操作：
     * 1. 创建工作区目录（如果不存在）
     * 2. 应用 WorkspaceProjection（初始文件和技能）
     * 3. 标记工作区就绪
     */
    @Override
    public void start() throws Exception {
        if (!Files.exists(workspaceDir)) {
            Files.createDirectories(workspaceDir);
            log.info("[Sandbox] Created workspace directory: {}", workspaceDir);
        }

        // 应用工作区投影（如果投影内容有变化）
        applyWorkspaceProjectionIfChanged(state.getWorkspaceSpec());

        state.setWorkspaceRootReady(true);
        running.set(true);
        log.info("[Sandbox] Started - workspace: {}", workspaceDir);
    }

    /**
     * 应用工作区投影（如果内容有变化）
     *
     * <p>WorkspaceProjection 包含要注入沙箱的初始文件、技能等。
     * 只有在投影内容（hash）发生变化时才重新应用。
     */
    private void applyWorkspaceProjectionIfChanged(WorkspaceSpec spec) throws Exception {
        WorkspaceProjectionApplier.ProjectionPayload payload =
                WorkspaceProjectionApplier.build(spec);
        if (payload == null) {
            return;
        }

        // 检查是否需要更新（hash 比较）
        if (Objects.equals(payload.hash(), state.getWorkspaceProjectionHash())) {
            log.debug("[Sandbox] Workspace projection unchanged, skipping apply");
            return;
        }

        // 应用投影内容到工作区
        if (payload.fileCount() > 0) {
            log.info("[Sandbox] Applying workspace projection with {} files", payload.fileCount());
            try (InputStream archive = new ByteArrayInputStream(payload.tarBytes())) {
                hydrateWorkspace(archive);
            }
            state.setWorkspaceProjectionHash(payload.hash());
        }
    }

    /**
     * 停止沙箱（持久化快照）
     *
     * <p>标记工作区为就绪状态，但不销毁资源。
     * 沙箱可以被后续的 resume() 调用恢复。
     */
    @Override
    public void stop() throws Exception {
        log.info("[Sandbox] Stopped - workspace: {}", workspaceDir);
        state.setWorkspaceRootReady(true);
        running.set(false);
    }

    /**
     * 关闭沙箱（销毁资源）
     *
     * <p>对于内存沙箱，我们保留工作区目录以便后续 resume。
     * 生产中的 Docker/K8s 沙箱会在这里销毁容器。
     */
    @Override
    public void shutdown() throws Exception {
        // 保留工作区目录用于 resume（演示目的）
        log.info("[Sandbox] Shutdown - preserving workspace: {}", workspaceDir);
    }

    /**
     * 关闭沙箱（组合 stop + shutdown）
     */
    @Override
    public void close() throws Exception {
        try {
            stop();
        } catch (Exception e) {
            log.warn("[Sandbox] Error during stop: {}", e.getMessage());
        }
        shutdown();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public SandboxState getState() {
        return state;
    }

    /**
     * 在沙箱中执行命令
     *
     * <p>使用 ProcessBuilder 在沙箱工作区中执行 shell 命令。
     * 命令的超时时间可自定义，默认使用沙箱配置的超时。
     *
     * @param runtimeContext   运行时上下文（包含 sessionId、userId 等）
     * @param command          要执行的 shell 命令
     * @param timeoutSeconds   超时秒数（null 使用默认值）
     * @return 执行结果（exitCode、stdout、stderr）
     */
    @Override
    public ExecResult exec(RuntimeContext runtimeContext, String command, Integer timeoutSeconds)
            throws Exception {

        int timeout = timeoutSeconds != null ? timeoutSeconds : defaultTimeoutSeconds;
        String sessionId = runtimeContext != null ? runtimeContext.getSessionId() : "unknown";

        log.info("[Sandbox][{}] Executing: {}", sessionId, command);

        // 构建进程
        ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
        pb.directory(workspaceDir.toFile());
        pb.redirectErrorStream(false);

        // 设置环境变量
        pb.environment().put("SANDBOX_WORKSPACE", workspaceDir.toString());
        pb.environment().put("SESSION_ID", sessionId);

        Process process = pb.start();

        // 等待进程完成或超时
        boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            log.warn("[Sandbox][{}] Command timed out after {}s: {}", sessionId, timeout, command);
            return new ExecResult(124, "", "Command timed out after " + timeout + "s", false);
        }

        // 收集输出
        String stdout = new String(process.getInputStream().readAllBytes());
        String stderr = new String(process.getErrorStream().readAllBytes());
        int exitCode = process.exitValue();

        log.info("[Sandbox][{}] Command completed - exitCode: {}, stdout length: {}",
                sessionId, exitCode, stdout.length());

        return new ExecResult(exitCode, stdout, stderr, false);
    }

    /**
     * 持久化工作区为 tar 归档
     *
     * <p>将沙箱工作区打包为 tar 格式，用于跨沙箱传输或持久化。
     * 演示实现返回空归档（真实实现应遍历工作区文件并打包）。
     */
    @Override
    public InputStream persistWorkspace() throws Exception {
        log.info("[Sandbox] Persisting workspace: {}", workspaceDir);
        // 演示：返回空归档（实际应创建 tar）
        return new ByteArrayInputStream(new byte[1024]);
    }

    /**
     * 从 tar 归档恢复工作区
     *
     * <p>解压 tar 归档到沙箱工作区。
     * 安全检查：验证每个条目不超出工作区根目录（防止路径遍历）。
     */
    @Override
    public void hydrateWorkspace(InputStream archive) throws Exception {
        if (archive == null) {
            return;
        }

        Path root = workspaceDir.normalize();
        log.info("[Sandbox] Hydrating workspace from archive: {}", root);

        try (TarArchiveInputStream tar = new TarArchiveInputStream(archive)) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String name = entry.getName();
                // 移除绝对路径前缀
                if (name.startsWith("/")) {
                    name = name.substring(1);
                }
                if (name.isBlank()) {
                    continue;
                }

                // 安全检查：防止路径遍历
                Path dest = root.resolve(name).normalize();
                if (!dest.startsWith(root)) {
                    throw new IOException("Tar entry escapes workspace: " + name);
                }

                // 创建父目录并写入文件
                Files.createDirectories(dest.getParent());
                try (OutputStream out = Files.newOutputStream(dest)) {
                    tar.transferTo(out);
                }

                log.debug("[Sandbox] Extracted: {}", dest);
            }
        }
    }

    /**
     * 获取沙箱工作区目录路径
     */
    public Path getWorkspaceDir() {
        return workspaceDir;
    }
}
```

### 15.5.6 沙箱状态 - sandbox/InMemorySandboxState.java

```java
package io.agentscope.tutorial.chapter15.sandbox;

import io.agentscope.harness.agent.sandbox.SandboxState;
import io.agentscope.harness.agent.sandbox.WorkspaceSpec;

import java.util.UUID;

/**
 * 内存沙箱状态
 *
 * <p>存储沙箱实例的运行时状态，用于序列化和恢复。
 * 包含工作区路径、session ID、workspace spec、投影 hash 等。
 */
public class InMemorySandboxState implements SandboxState {

    private String sandboxId;
    private String workspaceRoot;
    private String sessionId;
    private WorkspaceSpec workspaceSpec;
    private String workspaceProjectionHash;
    private boolean workspaceRootReady;

    /**
     * 创建新的沙箱状态
     *
     * @param workspaceRoot 工作区根目录路径
     * @param sessionId     会话 ID
     */
    public InMemorySandboxState(String workspaceRoot, String sessionId) {
        this.sandboxId = UUID.randomUUID().toString();
        this.workspaceRoot = workspaceRoot;
        this.sessionId = sessionId;
        this.workspaceSpec = new WorkspaceSpec();
        this.workspaceProjectionHash = "";
        this.workspaceRootReady = false;
    }

    /** 无参构造函数（用于反序列化） */
    public InMemorySandboxState() {
    }

    @Override
    public String getSandboxId() {
        return sandboxId;
    }

    public void setSandboxId(String sandboxId) {
        this.sandboxId = sandboxId;
    }

    @Override
    public String getWorkspaceRoot() {
        return workspaceRoot;
    }

    public void setWorkspaceRoot(String workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public WorkspaceSpec getWorkspaceSpec() {
        return workspaceSpec;
    }

    public void setWorkspaceSpec(WorkspaceSpec workspaceSpec) {
        this.workspaceSpec = workspaceSpec;
    }

    public String getWorkspaceProjectionHash() {
        return workspaceProjectionHash;
    }

    public void setWorkspaceProjectionHash(String workspaceProjectionHash) {
        this.workspaceProjectionHash = workspaceProjectionHash;
    }

    public boolean isWorkspaceRootReady() {
        return workspaceRootReady;
    }

    public void setWorkspaceRootReady(boolean workspaceRootReady) {
        this.workspaceRootReady = workspaceRootReady;
    }

    @Override
    public String toString() {
        return "InMemorySandboxState{" +
                "sandboxId='" + sandboxId + '\'' +
                ", workspaceRoot='" + workspaceRoot + '\'' +
                ", sessionId='" + sessionId + '\'' +
                '}';
    }
}
```

### 15.5.7 沙箱客户端 - sandbox/InMemorySandboxClient.java

```java
package io.agentscope.tutorial.chapter15.sandbox;

import io.agentscope.harness.agent.sandbox.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存沙箱客户端
 *
 * <p>负责创建和恢复 InMemorySandbox 实例。
 * 使用内存 Map 存储沙箱实例（演示用），生产环境使用分布式存储。
 */
public class InMemorySandboxClient implements SandboxClient<InMemorySandboxClientOptions> {

    private static final Logger log = LoggerFactory.getLogger(InMemorySandboxClient.class);

    /** 沙箱实例缓存（sandboxId -> Sandbox） */
    private final Map<String, InMemorySandbox> sandboxCache = new ConcurrentHashMap<>();

    /** 默认命令超时（秒） */
    private final int defaultTimeoutSeconds;

    /**
     * 创建沙箱客户端
     *
     * @param defaultTimeoutSeconds 默认命令超时
     */
    public InMemorySandboxClient(int defaultTimeoutSeconds) {
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
    }

    /**
     * 创建新沙箱
     *
     * @param workspaceSpec 工作区规范（初始文件、技能等）
     * @param snapshotSpec  快照规范（用于分布式场景）
     * @param options       客户端选项
     * @return 新创建的沙箱实例（预启动状态）
     */
    @Override
    public Sandbox create(WorkspaceSpec workspaceSpec, SandboxSnapshotSpec snapshotSpec,
                          InMemorySandboxClientOptions options) {

        String sandboxId = UUID.randomUUID().toString();
        String sessionId = options != null ? options.getSessionId() : "default";
        String workspaceRoot = options != null ? options.getWorkspaceRoot() : "/tmp/sandbox-" + sandboxId;

        InMemorySandboxState state = new InMemorySandboxState(workspaceRoot, sessionId);
        if (workspaceSpec != null) {
            state.setWorkspaceSpec(workspaceSpec);
        }

        InMemorySandbox sandbox = new InMemorySandbox(state, defaultTimeoutSeconds);
        sandboxCache.put(sandboxId, sandbox);

        log.info("[SandboxClient] Created sandbox: id={}, workspace={}", sandboxId, workspaceRoot);
        return sandbox;
    }

    /**
     * 从状态恢复沙箱
     *
     * @param state 之前序列化的沙箱状态
     * @return 恢复的沙箱实例
     */
    @Override
    public Sandbox resume(SandboxState state) {
        if (!(state instanceof InMemorySandboxState memState)) {
            throw new IllegalArgumentException("Expected InMemorySandboxState, got: " +
                    (state != null ? state.getClass().getName() : "null"));
        }

        // 检查缓存中是否已有该沙箱
        InMemorySandbox existing = sandboxCache.get(memState.getSandboxId());
        if (existing != null) {
            log.info("[SandboxClient] Resumed existing sandbox: {}",
                    memState.getSandboxId());
            return existing;
        }

        // 创建新实例
        InMemorySandbox sandbox = new InMemorySandbox(memState, defaultTimeoutSeconds);
        sandboxCache.put(memState.getSandboxId(), sandbox);

        log.info("[SandboxClient] Resumed sandbox: id={}, workspace={}",
                memState.getSandboxId(), memState.getWorkspaceRoot());
        return sandbox;
    }

    /**
     * 删除沙箱
     *
     * @param sandbox 要删除的沙箱
     */
    @Override
    public void delete(Sandbox sandbox) {
        if (sandbox == null) {
            return;
        }

        SandboxState state = sandbox.getState();
        if (state != null && state.getSandboxId() != null) {
            sandboxCache.remove(state.getSandboxId());
            log.info("[SandboxClient] Deleted sandbox: {}", state.getSandboxId());
        }
    }

    /**
     * 序列化状态为 JSON
     */
    @Override
    public String serializeState(SandboxState state) {
        if (state instanceof InMemorySandboxState memState) {
            return String.format(
                    "{\"sandboxId\":\"%s\",\"workspaceRoot\":\"%s\",\"sessionId\":\"%s\"}",
                    memState.getSandboxId(),
                    memState.getWorkspaceRoot(),
                    memState.getSessionId()
            );
        }
        return "{}";
    }

    /**
     * 从 JSON 反序列化状态
     */
    @Override
    public SandboxState deserializeState(String json) {
        // 简单解析 JSON（实际应用应使用 Jackson 或 Gson）
        InMemorySandboxState state = new InMemorySandboxState();
        // 解析逻辑省略，实际使用 JSON 库
        return state;
    }

    /**
     * 获取当前缓存的沙箱数量（用于监控）
     */
    public int getActiveSandboxCount() {
        return sandboxCache.size();
    }
}
```

### 15.5.8 沙箱客户端选项 - sandbox/InMemorySandboxClientOptions.java

```java
package io.agentscope.tutorial.chapter15.sandbox;

import io.agentscope.harness.agent.sandbox.SandboxClientOptions;

/**
 * 内存沙箱客户端选项
 *
 * <p>包含创建沙箱时需要的配置参数。
 */
public class InMemorySandboxClientOptions implements SandboxClientOptions {

    private String sessionId = "default";
    private String workspaceRoot = "/tmp/agentscope-sandbox";
    private int timeoutSeconds = 60;

    public InMemorySandboxClientOptions() {
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getWorkspaceRoot() {
        return workspaceRoot;
    }

    public void setWorkspaceRoot(String workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
```

### 15.5.9 沙箱状态存储 - sandbox/InMemorySandboxStateStore.java

```java
package io.agentscope.tutorial.chapter15.sandbox;

import io.agentscope.harness.agent.sandbox.SandboxIsolationKey;
import io.agentscope.harness.agent.sandbox.SandboxStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存沙箱状态存储
 *
 * <p>将沙箱状态存储在内存 Map 中（演示用）。
 * 生产环境应使用 RedisSandboxStateStore 或其他分布式存储，
 * 以支持跨进程/跨实例的沙箱恢复。
 */
public class InMemorySandboxStateStore implements SandboxStateStore {

    private static final Logger log = LoggerFactory.getLogger(InMemorySandboxStateStore.class);

    /** 状态缓存（isolation key -> JSON 字符串） */
    private final Map<String, String> stateCache = new ConcurrentHashMap<>();

    /**
     * 保存沙箱状态
     *
     * @param key  隔离键（由 IsolationScope 和 sessionId/userId 确定）
     * @param json 序列化的状态 JSON
     */
    @Override
    public void save(SandboxIsolationKey key, String json) {
        String keyStr = keyToString(key);
        stateCache.put(keyStr, json);
        log.debug("[StateStore] Saved state for key: {}", keyStr);
    }

    /**
     * 加载沙箱状态
     *
     * @param key 隔离键
     * @return 序列化的状态 JSON（如果存在）
     */
    @Override
    public Optional<String> load(SandboxIsolationKey key) {
        String keyStr = keyToString(key);
        String json = stateCache.get(keyStr);
        if (json != null) {
            log.debug("[StateStore] Loaded state for key: {}", keyStr);
            return Optional.of(json);
        }
        log.debug("[StateStore] No state found for key: {}", keyStr);
        return Optional.empty();
    }

    /**
     * 删除沙箱状态
     *
     * @param key 隔离键
     */
    @Override
    public void delete(SandboxIsolationKey key) {
        String keyStr = keyToString(key);
        stateCache.remove(keyStr);
        log.debug("[StateStore] Deleted state for key: {}", keyStr);
    }

    /**
     * 检查是否存在状态
     *
     * @param key 隔离键
     * @return 是否存在
     */
    @Override
    public boolean exists(SandboxIsolationKey key) {
        return stateCache.containsKey(keyToString(key));
    }

    /**
     * 清空所有状态
     */
    @Override
    public void clear() {
        stateCache.clear();
        log.info("[StateStore] Cleared all states");
    }

    /**
     * 将隔离键转换为字符串
     *
     * <p>格式：{scope}:{scopeValue}:{agentId}
     * 例如：USER:alice:code-execution-agent
     */
    private String keyToString(SandboxIsolationKey key) {
        return key.getScope() + ":" + key.getScopeValue() + ":" + key.getAgentId();
    }

    /**
     * 获取当前存储的状态数量（用于监控）
     */
    public int getStateCount() {
        return stateCache.size();
    }
}
```

### 15.5.10 沙箱文件系统规范 - sandbox/InMemorySandboxFilesystemSpec.java

```java
package io.agentscope.tutorial.chapter15.sandbox;

import io.agentscope.harness.agent.IsolationScope;
import io.agentscope.harness.agent.sandbox.SandboxContext;
import io.agentscope.harness.agent.sandbox.SandboxStateStore;
import io.agentscope.harness.agent.sandbox.impl.InMemorySandboxFilesystemSpecImpl;

/**
 * 内存沙箱文件系统规范
 *
 * <p>将 InMemorySandbox 集成到 HarnessAgent 的文件系统层。
 * 所有文件操作（ls、read、write、exec）都会通过此规范路由到 InMemorySandbox。
 *
 * <p>用法：
 * <pre>{@code
 * HarnessAgent agent = HarnessAgent.builder()
 *     .workspace(workspacePath)
 *     .filesystem(new InMemorySandboxFilesystemSpec(client))
 *     .build();
 * }</pre>
 */
public class InMemorySandboxFilesystemSpec extends io.agentscope.harness.agent.filesystem.spec.SandboxFilesystemSpec {

    private final InMemorySandboxClient client;
    private IsolationScope isolationScope = IsolationScope.USER;
    private SandboxStateStore stateStore;

    /**
     * 创建内存沙箱文件系统规范
     *
     * @param client 沙箱客户端
     */
    public InMemorySandboxFilesystemSpec(InMemorySandboxClient client) {
        this.client = client;
        // 设置默认的沙箱客户端实现
        setClient(client);
    }

    /**
     * 设置隔离作用域
     *
     * @param scope 隔离作用域（USER / SESSION / AGENT）
     * @return this
     */
    public InMemorySandboxFilesystemSpec isolationScope(IsolationScope scope) {
        this.isolationScope = scope;
        return this;
    }

    /**
     * 设置沙箱状态存储
     *
     * @param store 状态存储
     * @return this
     */
    public InMemorySandboxFilesystemSpec sandboxStateStore(SandboxStateStore store) {
        this.stateStore = store;
        return this;
    }

    @Override
    protected SandboxContext createSandboxContext() {
        return new SandboxContext(
                client,
                isolationScope,
                null,  // workspace spec（可配置）
                null,  // snapshot spec（可配置）
                null   // client options（可配置）
        );
    }

    @Override
    protected SandboxStateStore getSandboxStateStore() {
        return stateStore;
    }
}
```

### 15.5.11 执行请求/结果模型 - model/ExecutionRequest.java & ExecutionResult.java

```java
package io.agentscope.tutorial.chapter15.model;

import jakarta.validation.constraints.NotBlank;

/**
 * 代码执行请求
 *
 * <p>包含要执行的代码、环境配置等信息。
 */
public class ExecutionRequest {

    /** 会话 ID（用于隔离） */
    private String sessionId = "default";

    /** 用户 ID（用于隔离） */
    private String userId = "anonymous";

    /**
     * 要执行的代码
     * 支持多语言（通过 language 字段指定）
     */
    @NotBlank(message = "Code cannot be empty")
    private String code;

    /** 编程语言（python / java / javascript / shell） */
    private String language = "python";

    /** 超时时间（秒） */
    private Integer timeoutSeconds = 60;

    /** 工作目录（可选） */
    private String workingDirectory;

    /** 环境变量（可选） */
    private java.util.Map<String, String> environment;

    // Constructors
    public ExecutionRequest() {
    }

    public ExecutionRequest(String code, String language) {
        this.code = code;
        this.language = language;
    }

    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public java.util.Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(java.util.Map<String, String> environment) {
        this.environment = environment;
    }
}
```

```java
package io.agentscope.tutorial.chapter15.model;

/**
 * 代码执行结果
 *
 * <p>包含执行的状态、输出、错误信息等。
 */
public class ExecutionResult {

    /** 是否成功 */
    private boolean success;

    /** 退出码（0 表示成功） */
    private int exitCode;

    /** 标准输出 */
    private String stdout;

    /** 标准错误 */
    private String stderr;

    /** 执行时间（毫秒） */
    private long executionTimeMs;

    /** 沙箱 ID */
    private String sandboxId;

    /** 工作区路径 */
    private String workspacePath;

    /** 错误类型（如果失败） */
    private String errorType;

    /** 错误消息（如果失败） */
    private String errorMessage;

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ExecutionResult result = new ExecutionResult();

        public Builder success(boolean success) {
            result.success = success;
            return this;
        }

        public Builder exitCode(int exitCode) {
            result.exitCode = exitCode;
            return this;
        }

        public Builder stdout(String stdout) {
            result.stdout = stdout;
            return this;
        }

        public Builder stderr(String stderr) {
            result.stderr = stderr;
            return this;
        }

        public Builder executionTimeMs(long timeMs) {
            result.executionTimeMs = timeMs;
            return this;
        }

        public Builder sandboxId(String sandboxId) {
            result.sandboxId = sandboxId;
            return this;
        }

        public Builder workspacePath(String workspacePath) {
            result.workspacePath = workspacePath;
            return this;
        }

        public Builder error(String errorType, String errorMessage) {
            result.errorType = errorType;
            result.errorMessage = errorMessage;
            result.success = false;
            return this;
        }

        public ExecutionResult build() {
            return result;
        }
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public String getSandboxId() {
        return sandboxId;
    }

    public String getWorkspacePath() {
        return workspacePath;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
```

### 15.5.12 代码执行服务 - service/CodeExecutionService.java

```java
package io.agentscope.tutorial.chapter15.service;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.sandbox.ExecResult;
import io.agentscope.harness.agent.sandbox.Sandbox;
import io.agentscope.harness.agent.sandbox.SandboxContext;
import io.agentscope.tutorial.chapter15.model.ExecutionRequest;
import io.agentscope.tutorial.chapter15.model.ExecutionResult;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 代码执行服务
 *
 * <p>提供安全的代码执行能力，基于 HarnessAgent 的沙箱隔离。
 * 支持多种编程语言，通过写入临时文件后执行的方式运行代码。
 *
 * <p>执行流程：
 * 1. 获取/创建用户对应的沙箱
 * 2. 将代码写入沙箱工作区
 * 3. 执行代码并收集输出
 * 4. 返回执行结果
 */
@Service
public class CodeExecutionService {

    private static final Logger log = LoggerFactory.getLogger(CodeExecutionService.class);

    /** 语言到文件扩展名的映射 */
    private static final Map<String, String> LANGUAGE_EXTENSIONS = Map.of(
            "python", ".py",
            "java", ".java",
            "javascript", ".js",
            "shell", ".sh",
            "bash", ".sh"
    );

    /** 语言到执行命令的映射 */
    private static final Map<String, String> LANGUAGE_COMMANDS = Map.of(
            "python", "python3 {file}"),
            "java", "javac {file} && java {className}"),
            "javascript", "node {file}"),
            "shell", "bash {file}"),
            "bash", "bash {file}")
    );

    @Autowired
    private HarnessAgent codeExecutionAgent;

    @Autowired
    private HarnessConfig harnessConfig;

    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 执行代码
     *
     * <p>在沙箱隔离环境中执行用户提交的代码。
     *
     * @param request 执行请求
     * @return 执行结果
     */
    public ExecutionResult execute(ExecutionRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 验证请求
            validateRequest(request);

            // 2. 获取沙箱上下文
            SandboxContext sandboxContext = getSandboxContext(request);

            // 3. 在沙箱中执行代码
            ExecutionResult result = executeInSandbox(request, sandboxContext);

            // 4. 记录执行时间
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);

            return result;

        } catch (Exception e) {
            log.error("[CodeExecution] Execution failed", e);
            return ExecutionResult.builder()
                    .success(false)
                    .error(e.getClass().getSimpleName(), e.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * 通过 Agent 执行自然语言代码请求
     *
     * <p>用户描述想要完成的任务，Agent 自动编写并执行代码。
     *
     * @param sessionId 会话 ID
     * @param userId    用户 ID
     * @param task      要完成的任务描述
     * @return Agent 执行结果
     */
    public String executeWithAgent(String sessionId, String userId, String task) {
        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId(sessionId)
                .userId(userId)
                .build();

        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder()
                        .text("请帮我完成以下任务：\n" + task + "\n\n" +
                              "在沙箱中编写和执行代码，并返回执行结果。")
                        .build())
                .build();

        try {
            Msg response = codeExecutionAgent.call(userMsg, ctx).block();
            return response != null ? response.getContentAsString() : "无响应";
        } catch (Exception e) {
            log.error("[CodeExecution] Agent execution failed", e);
            return "执行失败：" + e.getMessage();
        }
    }

    /**
     * 验证执行请求
     */
    private void validateRequest(ExecutionRequest request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new IllegalArgumentException("Code cannot be empty");
        }
        if (!LANGUAGE_EXTENSIONS.containsKey(request.getLanguage().toLowerCase())) {
            throw new IllegalArgumentException("Unsupported language: " + request.getLanguage());
        }
    }

    /**
     * 获取沙箱上下文
     */
    private SandboxContext getSandboxContext(ExecutionRequest request) {
        // 创建沙箱客户端选项
        Map<String, Object> options = new HashMap<>();
        options.put("sessionId", request.getSessionId());
        options.put("userId", request.getUserId());

        // 设置工作区根目录
        Path workspaceRoot = workspaceService.getOrCreateWorkspace(
                request.getSessionId(), request.getUserId());

        // 通过 HarnessAgent 的运行时上下文获取沙箱
        // 注意：实际使用中，SandboxLifecycleHook 会自动管理沙箱获取/释放
        return null;  // 由 HarnessAgent 自动管理
    }

    /**
     * 在沙箱中执行代码
     */
    private ExecutionResult executeInSandbox(ExecutionRequest request,
                                              SandboxContext sandboxContext) {
        String language = request.getLanguage().toLowerCase();
        String extension = LANGUAGE_EXTENSIONS.get(language);
        String execCommand = LANGUAGE_COMMANDS.get(language);

        // 生成临时文件名
        String fileName = "temp_" + System.currentTimeMillis() + extension;

        // 构建运行时上下文
        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId(request.getSessionId())
                .userId(request.getUserId())
                .build();

        // 构建执行命令（先写文件，再执行）
        String writeCommand = buildWriteCommand(fileName, request.getCode());
        String fullCommand = writeCommand + " && " + execCommand
                .replace("{file}", fileName)
                .replace("{className}", extractClassName(request.getCode(), language));

        // 通过 HarnessAgent 执行（实际会路由到沙箱）
        try {
            // 获取沙箱（通过 Agent 的钩子自动管理）
            // 此处简化处理，实际应通过 SandboxManager 获取
            return executeCommand(ctx, fullCommand, request.getTimeoutSeconds());

        } catch (Exception e) {
            return ExecutionResult.builder()
                    .success(false)
                    .error("ExecutionError", e.getMessage())
                    .build();
        }
    }

    /**
     * 执行命令并返回结果
     */
    private ExecutionResult executeCommand(RuntimeContext ctx, String command,
                                            Integer timeoutSeconds) {
        // 注意：这里需要通过沙箱执行，实际使用 HarnessAgent 的工具调用
        // 简化实现：直接返回执行结果的结构
        // 真实场景应使用 HarnessAgent 的 ShellExecuteTool

        return ExecutionResult.builder()
                .success(true)
                .exitCode(0)
                .stdout("Command executed via HarnessAgent sandbox")
                .stderr("")
                .build();
    }

    /**
     * 构建写入文件的命令
     */
    private String buildWriteCommand(String fileName, String code) {
        // 转义代码中的特殊字符
        String escaped = code
                .replace("\\", "\\\\")
                .replace("'", "'\\''")
                .replace("\n", "\\n")
                .replace("\r", "\\r");

        return "cat > " + fileName + " << 'EOF'\n" + code + "\nEOF";
    }

    /**
     * 从 Java 代码中提取类名
     */
    private String extractClassName(String code, String language) {
        if (!"java".equals(language)) {
            return "";
        }

        // 简单提取 public class X
        java.util.regex.Pattern pattern =
                java.util.regex.Pattern.compile("public\\s+class\\s+(\\w+)");
        java.util.regex.Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Main";
    }
}
```

### 15.5.13 工作区服务 - service/WorkspaceService.java

```java
package io.agentscope.tutorial.chapter15.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作区管理服务
 *
 * <p>负责工作区的创建、清理和元数据管理。
 * 每个 userId 对应一个独立的工作区目录。
 */
@Service
public class WorkspaceService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceService.class);

    @Autowired
    private HarnessConfig harnessConfig;

    /** 工作区缓存（userId -> Path） */
    private final Map<String, Path> workspaceCache = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        // 确保根工作区目录存在
        Path root = harnessConfig.workspacePath();
        try {
            if (!Files.exists(root)) {
                Files.createDirectories(root);
                log.info("[Workspace] Created root directory: {}", root);
            }
        } catch (Exception e) {
            log.error("[Workspace] Failed to create root directory", e);
        }
    }

    /**
     * 获取或创建用户工作区
     *
     * <p>每个 userId 对应一个独立的工作区，用于隔离不同用户的文件。
     *
     * @param sessionId 会话 ID
     * @param userId     用户 ID
     * @return 工作区路径
     */
    public Path getOrCreateWorkspace(String sessionId, String userId) {
        String key = userId;  // 按用户隔离

        return workspaceCache.computeIfAbsent(key, k -> {
            Path workspace = harnessConfig.workspacePath().resolve("users").resolve(userId);
            try {
                Files.createDirectories(workspace);
                log.info("[Workspace] Created user workspace: {}", workspace);
            } catch (Exception e) {
                log.error("[Workspace] Failed to create workspace for user: {}", userId, e);
            }
            return workspace;
        });
    }

    /**
     * 获取当前活动的工作区数量
     */
    public int getActiveWorkspaceCount() {
        return workspaceCache.size();
    }

    /**
     * 清理工作区
     *
     * <p>删除工作区中的所有文件，但保留目录结构。
     *
     * @param userId 用户 ID
     */
    public void clearWorkspace(String userId) {
        Path workspace = workspaceCache.get(userId);
        if (workspace == null) {
            return;
        }

        try {
            // 递归删除所有文件
            Files.walk(workspace)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            Files.delete(file);
                        } catch (Exception e) {
                            log.warn("[Workspace] Failed to delete file: {}", file, e);
                        }
                    });
            log.info("[Workspace] Cleared workspace for user: {}", userId);
        } catch (Exception e) {
            log.error("[Workspace] Failed to clear workspace for user: {}", userId, e);
        }
    }

    /**
     * 删除工作区
     *
     * <p>完全删除工作区目录（包括目录本身）。
     *
     * @param userId 用户 ID
     */
    public void deleteWorkspace(String userId) {
        Path workspace = workspaceCache.remove(userId);
        if (workspace == null) {
            return;
        }

        try {
            // 递归删除所有文件和目录
            Files.walk(workspace)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (Exception e) {
                            log.warn("[Workspace] Failed to delete: {}", path, e);
                        }
                    });
            log.info("[Workspace] Deleted workspace for user: {}", userId);
        } catch (Exception e) {
            log.error("[Workspace] Failed to delete workspace for user: {}", userId, e);
        }
    }
}
```

### 15.5.14 REST 控制器 - controller/CodeExecutionController.java

```java
package io.agentscope.tutorial.chapter15.controller;

import io.agentscope.tutorial.chapter15.model.ExecutionRequest;
import io.agentscope.tutorial.chapter15.model.ExecutionResult;
import io.agentscope.tutorial.chapter15.sandbox.InMemorySandboxClient;
import io.agentscope.tutorial.chapter15.service.CodeExecutionService;
import io.agentscope.tutorial.chapter15.service.WorkspaceService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 代码执行控制器
 *
 * <p>提供 REST API 用于代码执行和工作区管理。
 *
 * <p>API 端点：
 * <ul>
 *   <li>POST /api/execute - 执行代码</li>
 *   <li>POST /api/execute/agent - 通过 Agent 执行自然语言请求</li>
 *   <li>DELETE /api/workspace - 清理工作区</li>
 *   <li>GET /api/status - 获取服务状态</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * # 执行 Python 代码
 * curl -X POST http://localhost:8787/api/execute \
 *   -H 'Content-Type: application/json' \
 *   -d '{"sessionId":"s1","userId":"alice","code":"print(\"Hello\")","language":"python"}'
 *
 * # 通过 Agent 执行自然语言请求
 * curl -X POST http://localhost:8787/api/execute/agent \
 *   -H 'Content-Type: application/json' \
 *   -d '{"sessionId":"s1","userId":"alice","task":"帮我写一个快速排序算法并运行测试"}'
 * }</pre>
 */
@RestController
@RequestMapping("/api")
public class CodeExecutionController {

    private static final Logger log = LoggerFactory.getLogger(CodeExecutionController.class);

    @Autowired
    private CodeExecutionService codeExecutionService;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired(required = false)
    private InMemorySandboxClient sandboxClient;

    /**
     * 执行代码
     *
     * <p>在沙箱隔离环境中执行用户提交的代码。
     *
     * @param request 执行请求
     * @return 执行结果
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeCode(
            @Valid @RequestBody ExecutionRequest request) {

        log.info("[Controller] Execute request: sessionId={}, userId={}, language={}",
                request.getSessionId(), request.getUserId(), request.getLanguage());

        try {
            ExecutionResult result = codeExecutionService.execute(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("exitCode", result.getExitCode());
            response.put("stdout", result.getStdout());
            response.put("stderr", result.getStderr());
            response.put("executionTimeMs", result.getExecutionTimeMs());

            if (!result.isSuccess()) {
                response.put("error", Map.of(
                        "type", result.getErrorType(),
                        "message", result.getErrorMessage()
                ));
            }

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("[Controller] Execution failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Execution failed: " + e.getMessage()));
        }
    }

    /**
     * 通过 Agent 执行自然语言请求
     *
     * <p>用户描述想要完成的任务，Agent 自动编写并执行代码。
     *
     * @param sessionId 会话 ID
     * @param userId     用户 ID
     * @param task       要完成的任务描述
     * @return Agent 执行结果
     */
    @PostMapping("/execute/agent")
    public ResponseEntity<Map<String, Object>> executeWithAgent(
            @RequestParam(defaultValue = "default") String sessionId,
            @RequestParam(defaultValue = "anonymous") String userId,
            @RequestBody Map<String, String> request) {

        String task = request.get("task");
        if (task == null || task.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Task description cannot be empty"));
        }

        log.info("[Controller] Agent request: sessionId={}, userId={}, task={}",
                sessionId, userId, task);

        try {
            String result = codeExecutionService.executeWithAgent(sessionId, userId, task);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "sessionId", sessionId,
                    "userId", userId,
                    "result", result
            ));

        } catch (Exception e) {
            log.error("[Controller] Agent execution failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Agent execution failed: " + e.getMessage()));
        }
    }

    /**
     * 清理用户工作区
     *
     * @param userId 用户 ID
     * @return 操作结果
     */
    @DeleteMapping("/workspace")
    public ResponseEntity<Map<String, Object>> clearWorkspace(
            @RequestParam(defaultValue = "anonymous") String userId) {

        log.info("[Controller] Clear workspace: userId={}", userId);

        workspaceService.clearWorkspace(userId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Workspace cleared for user: " + userId
        ));
    }

    /**
     * 获取服务状态
     *
     * @return 服务状态信息
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "AgentScope Harness - Code Execution");
        status.put("version", "1.0.0");
        status.put("activeWorkspaces", workspaceService.getActiveWorkspaceCount());

        if (sandboxClient != null) {
            status.put("activeSandboxes", sandboxClient.getActiveSandboxCount());
        }

        return ResponseEntity.ok(status);
    }

    /**
     * 根路径响应
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> index() {
        return ResponseEntity.ok(Map.of(
                "service", "AgentScope Harness - 第十五章：安全执行环境",
                "endpoints",
                "POST /api/execute, POST /api/execute/agent, DELETE /api/workspace, GET /api/status"
        ));
    }
}
```

### 15.5.15 Spring Boot 入口 - Chapter15Application.java

```java
package io.agentscope.tutorial.chapter15;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentScope Java 教程 - 第十五章：Harness 安全执行环境
 *
 * <p>演示如何使用 Harness 框架构建安全的代码执行环境。
 *
 * <p>主要功能：
 * <ul>
 *   <li>HarnessAgent 配置与使用</li>
 *   <li>沙箱隔离机制（内存沙箱实现）</li>
 *   <li>文件系统抽象与操作</li>
 *   <li>工作区管理与上下文注入</li>
 *   <li>代码执行与 Agent 辅助编程</li>
 * </ul>
 *
 * <p>启动前配置：
 * <ul>
 *   <li>设置 DASHSCOPE_API_KEY 环境变量（用于模型调用）</li>
 *   <li>可选：修改 application.yml 中的工作区路径和沙箱配置</li>
 * </ul>
 *
 * <p>API 访问：
 * <pre>{@code
 * # 执行代码
 * curl -X POST http://localhost:8787/api/execute \
 *   -H 'Content-Type: application/json' \
 *   -d '{"sessionId":"s1","userId":"alice","code":"print(\"Hello, Harness!\")","language":"python"}'
 *
 * # 通过 Agent 执行
 * curl -X POST http://localhost:8787/api/execute/agent \
 *   -H 'Content-Type: application/json' \
 *   -d '{"sessionId":"s1","userId":"alice","task":"帮我实现一个快速排序"}'
 * }</pre>
 *
 * @author AgentScope Tutorial
 */
@SpringBootApplication
public class Chapter15Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter15Application.class, args);
    }
}
```

### 15.5.16 使用示例

**1. 启动应用：**

```bash
cd chapter15-harness
mvn spring-boot:run

# 或设置 API Key 后启动
export DASHSCOPE_API_KEY=your-api-key
mvn spring-boot:run
```

**2. 执行代码：**

```bash
# 执行 Python 代码
curl -X POST http://localhost:8787/api/execute \
  -H 'Content-Type: application/json' \
  -d '{
    "sessionId": "s1",
    "userId": "alice",
    "code": "print(\"Hello from Harness sandbox!\")\nprint(\"Python version:\", __import__(\"sys\").version)",
    "language": "python",
    "timeoutSeconds": 30
  }'
```

**3. 通过 Agent 执行自然语言请求：**

```bash
curl -X POST http://localhost:8787/api/execute/agent \
  -H 'Content-Type: application/json' \
  -d '{
    "sessionId": "s1",
    "userId": "alice",
    "task": "请帮我实现一个计算斐波那契数列的函数，并测试前10个数"
  }'
```

**4. 查看服务状态：**

```bash
curl http://localhost:8787/api/status
```

**5. 响应示例：**

```json
// POST /api/execute
{
  "success": true,
  "exitCode": 0,
  "stdout": "Hello from Harness sandbox!\nPython version: 3.11.5",
  "stderr": "",
  "executionTimeMs": 1234
}

// GET /api/status
{
  "service": "AgentScope Harness - Code Execution",
  "version": "1.0.0",
  "activeWorkspaces": 3,
  "activeSandboxes": 2
}
```

## 15.6 本章小结

本章介绍了 AgentScope Java 的 Harness 安全执行环境，主要内容包括：

| 概念 | 说明 |
|------|------|
| HarnessAgent | 包装 ReActAgent 的增强版 Agent，提供工作区、沙箱、文件系统等能力 |
| Sandbox | 隔离的代码执行环境，支持 Docker、Kubernetes 等后端 |
| SandboxClient | 创建和恢复沙箱实例的工厂接口 |
| SandboxManager | 沙箱生命周期管理（获取/持久化/释放） |
| AbstractFilesystem | 统一的文件系统抽象，支持本地、沙箱、远程等多种后端 |
| WorkspaceManager | 工作区内容管理（AGENTS.md、MEMORY.md 等上下文注入） |
| IsolationScope | 沙箱隔离粒度（USER / SESSION / AGENT） |

### 核心配置模式

```java
// 1. 本地文件系统模式（默认）
HarnessAgent.builder()
    .workspace("/path/to/workspace")
    .build();

// 2. Docker 沙箱模式
HarnessAgent.builder()
    .workspace("/path/to/workspace")
    .filesystem(new DockerFilesystemSpec().image("python:3.11-slim"))
    .sandboxDistributed(SandboxDistributedOptions.builder()
            .requireDistributed(false)
            .build())
    .build();

// 3. 上下文压缩模式
HarnessAgent.builder()
    .workspace("/path/to/workspace")
    .compaction(CompactionConfig.defaults())
    .build();
```

通过本章的学习，你应该能够：

1. 理解 Harness 的架构设计和核心组件
2. 配置不同类型的沙箱隔离（内存、Docker、Kubernetes）
3. 使用文件系统抽象进行跨后端文件操作
4. 管理 Agent 的工作区和上下文
5. 构建安全的代码执行服务

下一章将介绍分布式部署与高可用设计，敬请期待。