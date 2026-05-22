# 第十章：RAG 检索增强生成

> 本章介绍如何使用 AgentScope Java 构建完整的 RAG（Retrieval-Augmented Generation）系统，包括向量数据库集成、文本分块策略、混合检索以及企业级知识库问答系统的实现。

## 10.1 RAG 架构与原理

### 10.1.1 什么是 RAG

RAG（检索增强生成，Retrieval-Augmented Generation）是一种将信息检索与语言模型生成相结合的技术架构。其核心思想是：在生成回答之前，先从外部知识库中检索与问题相关的内容，然后将这些内容作为上下文提供给语言模型，从而生成更准确、更具事实依据的回答。

### 10.1.2 RAG 工作流程

RAG 系统的完整工作流程可分为以下几个阶段：

```
用户问题 → 向量化查询 → 向量相似度检索 → 上下文组装 → LLM 生成回答
```

**阶段一：文档处理与入库**
```
原始文档 → 文本分块 → Embedding 向量化 → 向量数据库存储
```

**阶段二：查询与检索**
```
用户查询 → Embedding 向量化 → 向量数据库检索 → 相关文档排序
```

**阶段三：增强生成**
```
检索结果 + 用户问题 → 提示词组装 → LLM 生成 → 返回回答
```

### 10.1.3 AgentScope Java 中的 RAG 组件

AgentScope Java 提供了完整的 RAG 支持，核心组件包括：

| 组件 | 说明 |
|------|------|
| `Knowledge` | 知识库接口，定义文档添加和检索的统一 API |
| `SimpleKnowledge` | 简单的知识库实现，整合 Embedding 模型和向量存储 |
| `Document` | 文档实体，包含元数据和向量 |
| `DocumentMetadata` | 文档元数据，包含内容、文档 ID、块 ID 和自定义载荷 |
| `RetrieveConfig` | 检索配置，包含结果数量限制和相似度阈值 |
| `EmbeddingModel` | Embedding 模型接口，支持文本和多模态向量化 |
| `VDBStoreBase` | 向量数据库统一接口，支持 Qdrant、Milvus、Elasticsearch 等 |
| `GenericRAGHook` | Generic 模式 RAG 钩子，自动在推理前注入知识 |
| `KnowledgeRetrievalTools` | Agentic 模式检索工具，支持 Agent 自主调用 |

### 10.1.4 RAG 的两种模式

AgentScope Java 支持两种 RAG 集成模式：

**Generic 模式（通用模式）**
- 通过 `GenericRAGHook` 实现
- 在每次推理前自动检索相关知识
- 适用于固定知识库查询场景
- Agent 无需感知知识检索的存在

**Agentic 模式（智能模式）**
- 通过 `KnowledgeRetrievalTools` 工具实现
- Agent 自主决定何时检索知识
- 适用于复杂推理和多步查询场景
- 检索时机和策略由 Agent 自行控制

## 10.2 向量数据库集成

### 10.2.1 向量数据库概述

向量数据库是专门用于存储和检索高维向量数据的数据库，相比传统数据库，它能够高效地进行相似度搜索。在 RAG 系统中，向量数据库用于存储文档的嵌入向量，并通过余弦相似度或欧氏距离等度量快速找到与查询最相似的文档。

### 10.2.2 Qdrant 集成

Qdrant 是一个用 Rust 编写的开源向量数据库，提供高性能的向量存储和检索服务。

**主要特性：**
- 高性能：使用 Rust 编写，拥有出色的检索速度
- 易于部署：支持 Docker 一键部署
- 丰富的过滤：支持基于 payload 的元数据过滤
- gRPC 接口：提供高效的 gRPC 通信协议

**Java 客户端依赖：**

```xml
<dependency>
    <groupId>io.qdrant</groupId>
    <artifactId>client</artifactId>
    <version>1.15.0</version>
    <scope>provided</scope>
</dependency>
```

**基本使用示例：**

```java
// 创建 QdrantStore 实例
try (QdrantStore store = QdrantStore.builder()
        .location("http://localhost:6333")  // Qdrant 服务地址
        .collectionName("knowledge_base")  // 集合名称
        .dimensions(1024)                    // 向量维度，需与 Embedding 模型匹配
        .apiKey("optional-api-key")         // 可选的 API 密钥
        .build()) {
    // 添加文档到向量数据库
    store.add(List.of(document)).block();

    // 搜索相似文档
    List<Document> results = store.search(
        SearchDocumentDto.builder()
            .queryEmbedding(queryVector)
            .limit(5)
            .build()
    ).block();
}
```

**关键配置参数：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `location` | Qdrant 服务地址 | - |
| `collectionName` | 集合名称 | - |
| `dimensions` | 向量维度 | - |
| `apiKey` | API 密钥（可选） | null |
| `useTransportLayerSecurity` | 是否启用 TLS | false |
| `checkCompatibility` | 是否检查版本兼容性 | true |

### 10.2.3 Milvus 集成

Milvus 是一个云原生的向量数据库，支持分布式部署和水平扩展。

**主要特性：**
- 云原生：支持 Kubernetes 部署
- 高可用：支持分布式架构
- 多种索引：支持 IVF、HNSW、DiskANN 等多种索引类型
- 丰富的数据类型：支持动态字段和标量字段过滤

**Java 客户端依赖：**

```xml
<dependency>
    <groupId>io.milvus</groupId>
    <artifactId>milvus-sdk-java</artifactId>
    <version>2.6.16</version>
</dependency>
```

**基本使用示例：**

```java
// 创建 MilvusStore 实例
try (MilvusStore store = MilvusStore.builder()
        .uri("http://localhost:19530")     // Milvus 服务地址
        .databaseName("knowledge")         // 数据库名称
        .collectionName("docs")             // 集合名称
        .dimensions(1024)                   // 向量维度
        .token("username:password")        // 认证令牌
        .build()) {
    // 添加文档
    store.add(List.of(document)).block();

    // 搜索相似文档
    List<Document> results = store.search(
        SearchDocumentDto.builder()
            .queryEmbedding(queryVector)
            .limit(5)
            .build()
    ).block();
}
```

### 10.2.4 其他向量存储

AgentScope Java 还支持以下向量存储：

**内存存储（InMemoryStore）**
- 适用于开发和测试
- 数据存储在内存中，重启后丢失
- 无需外部服务依赖

```java
VDBStoreBase store = InMemoryStore.builder()
    .dimensions(1024)
    .build();
```

**Elasticsearch**
- 适用于已有 Elasticsearch 集群的场景
- 支持混合检索（向量 + 关键词）

**PgVector（PostgreSQL 扩展）**
- 适用于需要关系型数据库存储的场景
- 便于与现有 PostgreSQL 应用集成

## 10.3 文本分块策略

### 10.3.1 分块的重要性

文本分块（Chunking）是 RAG 系统中的关键步骤。合理的分块策略可以：

- 确保每个块包含完整的语义单元
- 控制块大小以适应模型上下文限制
- 保留检索结果的可读性和信息完整性

### 10.3.2 分块策略类型

AgentScope Java 通过 `SplitStrategy` 枚举提供了多种分块策略：

**1. 字符分块（CHARACTER）**
- 按固定字符数分割文本
- 简单快速，但可能打断单词或句子
- 适用于对语义完整性要求不高的场景

```java
// 按 500 字符分块，无重叠
List<String> chunks = TextChunker.chunkText(
    text,
    500,
    SplitStrategy.CHARACTER,
    0
);
```

**2. 段落分块（PARAGRAPH）**
- 按段落边界分割（双换行符分隔）
- 保留语义完整性，块大小不均匀
- 适用于结构化文档（文章、报告等）

```java
// 按段落分块，每个块最多 1000 字符，100 字符重叠
List<String> chunks = TextChunker.chunkText(
    text,
    1000,
    SplitStrategy.PARAGRAPH,
    100
);
```

**3. Token 分块（TOKEN）**
- 按近似 token 数分割（1 token ≈ 4 字符）
- 更好地控制 LLM 上下文使用
- 适用于需要精确控制 token 数量的场景

```java
// 按 256 tokens 分块，32 tokens 重叠
List<String> chunks = TextChunker.chunkText(
    text,
    256,
    SplitStrategy.TOKEN,
    32
);
```

**4. 语义分块（SEMANTIC）**
- 按语义边界分割（句子、从句等）
- 当前实现回退到段落分块
- 适用于需要高语义完整性的场景

### 10.3.3 分块参数选择指南

选择合适的分块参数需要考虑以下因素：

| 因素 | 建议 |
|------|------|
| Embedding 模型上下文 | 确保块内容在模型上下文限制内 |
| 查询复杂度 | 简单问题可用较小块，复杂问题需要较大块 |
| 重叠大小 | 较大重叠可避免块边界信息丢失 |
| 检索延迟 | 较大块会增加内存和处理开销 |

**推荐配置：**

| 场景 | chunkSize | overlapSize | 策略 |
|------|-----------|-------------|------|
| 通用问答 | 500-1000 | 50-100 | PARAGRAPH |
| 长文档检索 | 1000-2000 | 100-200 | TOKEN |
| 短查询精确匹配 | 200-500 | 0 | CHARACTER |
| 多模态文档 | 500 | 50 | PARAGRAPH |

### 10.3.4 分块与文档元数据

分块后，每个块需要关联完整的文档元数据：

```java
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.DocumentMetadata;
import io.agentscope.core.message.TextBlock;
import java.util.Map;
import java.util.List;
import java.util.UUID;

// 文档元数据示例
Map<String, Object> payload = new HashMap<>();
payload.put("filename", "annual-report-2025.pdf");
payload.put("department", "Finance");
payload.put("author", "Zhang Wei");
payload.put("created_at", "2025-01-15");

// 创建文档块
List<Document> chunks = chunkedTexts.stream()
    .map(text -> {
        DocumentMetadata metadata = DocumentMetadata.builder()
            .content(TextBlock.builder().text(text).build())
            .docId("doc-" + UUID.randomUUID())
            .chunkId("chunk-" + index)
            .payload(payload)
            .build();
        return new Document(metadata);
    })
    .toList();
```

## 10.4 混合检索

### 10.4.1 混合检索的概念

混合检索（Hybrid Search）结合了向量相似度检索和关键词匹配检索的优势，能够在保持语义相关性的同时确保关键词的精确匹配。

### 10.4.2 混合检索的实现

在 Qdrant 等向量数据库中，可以通过以下方式实现混合检索：

```java
// 构建混合检索请求
SearchDocumentDto searchDto = SearchDocumentDto.builder()
    .queryEmbedding(queryVector)       // 向量检索
    .vectorName("default")              // 向量名称
    .limit(10)                          // 返回数量
    .scoreThreshold(0.7)                // 相似度阈值
    .build();

// 执行向量检索
List<Document> vectorResults = store.search(searchDto).block();

// 在检索结果上进行关键词过滤
List<Document> filteredResults = vectorResults.stream()
    .filter(doc -> {
        String content = doc.getMetadata().getContentText();
        return content.contains(keyword) || content.contains(synonym);
    })
    .toList();
```

### 10.4.3 RRF 融合算法

当需要融合多个检索来源的结果时，可以使用 RRF（Reciprocal Rank Fusion）算法：

```java
/**
 * RRF 融合算法实现
 *
 * @param resultLists 多个检索结果列表
 * @param k 融合参数，通常设为 60
 * @return 融合后的排序结果
 */
public <T> List<T> rrfFusion(List<List<T>> resultLists, int k) {
    Map<T, Double> scores = new HashMap<>();

    // 遍历每个结果列表
    for (List<T> results : resultLists) {
        for (int i = 0; i < results.size(); i++) {
            T item = results.get(i);
            // 计算 RRF 分数：1 / (rank + k)
            double score = 1.0 / (i + k);
            scores.merge(item, score, Double::sum);
        }
    }

    // 按分数排序
    return scores.entrySet().stream()
        .sorted(Map.Entry.<T, Double>comparingByValue().reversed())
        .map(Map.Entry::getKey)
        .toList();
}
```

## 10.5 Embedding 模型配置

### 10.5.1 Embedding 模型概述

Embedding 模型负责将文本内容转换为高维向量。不同的 Embedding 模型在维度、支持语言、效果等方面有所差异。

### 10.5.2 阿里云 DashScope Text Embedding

DashScope 是阿里云的模型服务平台，提供文本嵌入模型。

**配置示例：**

```java
import io.agentscope.core.embedding.dashscope.DashScopeTextEmbedding;

// 创建 DashScope 文本嵌入模型
EmbeddingModel embeddingModel = DashScopeTextEmbedding.builder()
    .apiKey(System.getenv("DASHSCOPE_API_KEY"))  // API 密钥
    .modelName("text-embedding-v3")              // 模型名称
    .dimensions(1024)                             // 向量维度
    .build();

// 生成文本向量
double[] vector = embeddingModel.embed(
    TextBlock.builder().text("要嵌入的文本内容").build()
).block();
```

### 10.5.3 Ollama 本地 Embedding

Ollama 支持本地部署 Embedding 模型，适合需要数据隐私的场景。

**配置示例：**

```java
import io.agentscope.core.embedding.ollama.OllamaTextEmbedding;

// 创建 Ollama 嵌入模型
EmbeddingModel embeddingModel = OllamaTextEmbedding.builder()
    .baseUrl("http://localhost:11434")  // Ollama 服务地址
    .modelName("nomic-embed-text")       // 模型名称
    .dimensions(768)                     // 向量维度
    .timeout(Duration.ofSeconds(30))     // 超时时间
    .build();
```

### 10.5.4 OpenAI Embedding

OpenAI 提供高质量的文本嵌入模型。

**配置示例：**

```java
import io.agentscope.core.embedding.openai.OpenAITextEmbedding;

// 创建 OpenAI 嵌入模型
EmbeddingModel embeddingModel = OpenAITextEmbedding.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .modelName("text-embedding-3-small")
    .dimensions(1536)
    .baseUrl("https://api.openai.com/v1")  // 可自定义 API 端点
    .build();
```

### 10.5.5 Embedding 模型选择指南

| 模型 | 维度 | 特点 | 适用场景 |
|------|------|------|----------|
| DashScope text-embedding-v3 | 1024/1536 | 高性能，中文优化 | 中文应用 |
| Ollama nomic-embed-text | 768 | 本地部署，隐私保护 | 数据敏感场景 |
| OpenAI text-embedding-3-small | 1536 | 高质量，全球支持 | 国际化应用 |

## 10.6 【案例】企业知识库问答系统

本案例使用 Spring Boot 3 + Java 21 + Docker Qdrant 构建一个完整的企业知识库问答系统。

### 10.6.1 项目结构

```
chapter10-rag/
├── src/main/java/io/agentscope/tutorial/chapter10/
│   ├── Chapter10Application.java          # Spring Boot 启动类
│   ├── config/
│   │   ├── RAGConfig.java                 # RAG 配置类
│   │   └── ModelConfig.java               # 模型配置类
│   ├── service/
│   │   ├── KnowledgeBaseService.java      # 知识库服务
│   │   ├── DocumentService.java           # 文档处理服务
│   │   └── RAGQueryService.java           # RAG 查询服务
│   ├── controller/
│   │   └── RagController.java             # REST API 控制器
│   └── model/
│       ├── DocUploadRequest.java          # 文档上传请求
│       └── QueryRequest.java              # 查询请求
├── src/main/resources/
│   └── application.yml                    # 应用配置文件
├── src/test/java/
│   └── io/agentscope/tutorial/chapter10/
│       └── RagServiceTest.java           # 单元测试
├── docker-compose.yml                     # Docker 编排文件
└── pom.xml                                # Maven 依赖配置
```

### 10.6.2 Docker Compose 配置

首先，启动 Qdrant 向量数据库服务：

```yaml
# docker-compose.yml
version: '3.8'

services:
  # Qdrant 向量数据库
  qdrant:
    image: qdrant/qdrant:v1.7.4
    container_name: qdrant-vector-db
    ports:
      - "6333:6333"   # REST API 端口
      - "6334:6334"   # gRPC 端口
    volumes:
      - qdrant_data:/qdrant/storage  # 持久化存储
    environment:
      - QDRANT__SERVICE__GRPC_PORT=6334
      - QDRANT__SERVICE__HTTP_PORT=6333
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:6333/health"]
      interval: 10s
      timeout: 5s
      retries: 5

  # 可选：MinIO 对象存储（用于存储原始文档）
  minio:
    image: minio/minio:latest
    container_name: minio-storage
    ports:
      - "9000:9000"   # API 端口
      - "9001:9001"   # 控制台端口
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin123
    volumes:
      - minio_data:/data
    command: server /data --console-address ":9001"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  qdrant_data:
    driver: local
  minio_data:
    driver: local
```

启动服务：

```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看 Qdrant 日志
docker-compose logs -f qdrant

# 停止服务
docker-compose down
```

验证 Qdrant 服务是否正常运行：

```bash
# 检查健康状态
curl http://localhost:6333/health

# 查看集合列表
curl http://localhost:6333/collections
```

### 10.6.3 Maven 依赖配置

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

    <groupId>io.agentscope.tutorial</groupId>
    <artifactId>chapter10-rag</artifactId>
    <version>1.0.0</version>
    <name>Chapter 10 - RAG System</name>
    <description>企业知识库问答系统 - RAG 检索增强生成</description>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- AgentScope Core -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-core</artifactId>
            <version>1.0.0</version>
        </dependency>

        <!-- AgentScope RAG Simple (Qdrant, Embedding models) -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-extensions-rag-simple</artifactId>
            <version>1.0.0</version>
        </dependency>

        <!-- AgentScope DashScope 模型支持 -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-model-dashscope</artifactId>
            <version>1.0.0</version>
        </dependency>

        <!-- Qdrant Java 客户端（显式引入） -->
        <dependency>
            <groupId>io.qdrant</groupId>
            <artifactId>client</artifactId>
            <version>1.15.0</version>
        </dependency>

        <!-- Spring Boot Configuration Processor -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot Test -->
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

### 10.6.4 应用配置文件

```yaml
# src/main/resources/application.yml
server:
  port: 8080

spring:
  application:
    name: chapter10-rag
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

# RAG 配置
rag:
  # Qdrant 向量数据库配置
  qdrant:
    host: http://localhost:6333
    collection-name: enterprise_knowledge
    dimensions: 1024

  # 嵌入模型配置
  embedding:
    provider: dashscope  # dashscope / ollama / openai
    model-name: text-embedding-v3
    dimensions: 1024
    api-key: ${DASHSCOPE_API_KEY:your-api-key-here}

  # Ollama 配置（备用）
  ollama:
    base-url: http://localhost:11434
    model-name: nomic-embed-text
    dimensions: 768

  # 检索配置
  retrieval:
    default-limit: 5
    score-threshold: 0.5

  # 分块配置
  chunking:
    strategy: PARAGRAPH  # CHARACTER / PARAGRAPH / TOKEN / SEMANTIC
    chunk-size: 800
    overlap-size: 100

# 模型配置（用于 LLM 生成）
model:
  provider: dashscope
  api-key: ${DASHSCOPE_API_KEY:your-api-key-here}
  model-name: qwen-plus
  base-url: https://dashscope.aliyuncs.com/compatible-mode/v1

# 日志配置
logging:
  level:
    io.agentscope: DEBUG
    root: INFO
```

### 10.6.5 配置类

```java
// src/main/java/io/agentscope/tutorial/chapter10/config/RAGConfig.java
package io.agentscope.tutorial.chapter10.config;

import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.embedding.dashscope.DashScopeTextEmbedding;
import io.agentscope.core.embedding.ollama.OllamaTextEmbedding;
import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.model.RetrieveConfig;
import io.agentscope.core.rag.reader.SplitStrategy;
import io.agentscope.core.rag.store.QdrantStore;
import io.agentscope.core.rag.store.VDBStoreBase;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 配置类
 *
 * 负责初始化 RAG 系统所需的核心组件，包括：
 * - 向量数据库客户端
 * - Embedding 模型
 * - 知识库服务
 *
 * @author AgentScope Tutorial
 */
@Configuration
@ConfigurationProperties(prefix = "rag")
public class RAGConfig {

    private QdrantConfig qdrant = new QdrantConfig();
    private EmbeddingConfig embedding = new EmbeddingConfig();
    private OllamaConfig ollama = new OllamaConfig();
    private RetrievalConfig retrieval = new RetrievalConfig();
    private ChunkingConfig chunking = new ChunkingConfig();

    // ========== 内部配置类 ==========

    public static class QdrantConfig {
        private String host = "http://localhost:6333";
        private String collectionName = "enterprise_knowledge";
        private int dimensions = 1024;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public String getCollectionName() { return collectionName; }
        public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
        public int getDimensions() { return dimensions; }
        public void setDimensions(int dimensions) { this.dimensions = dimensions; }
    }

    public static class EmbeddingConfig {
        private String provider = "dashscope";
        private String modelName = "text-embedding-v3";
        private int dimensions = 1024;
        private String apiKey;

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        public int getDimensions() { return dimensions; }
        public void setDimensions(int dimensions) { this.dimensions = dimensions; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }

    public static class OllamaConfig {
        private String baseUrl = "http://localhost:11434";
        private String modelName = "nomic-embed-text";
        private int dimensions = 768;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        public int getDimensions() { return dimensions; }
        public void setDimensions(int dimensions) { this.dimensions = dimensions; }
    }

    public static class RetrievalConfig {
        private int defaultLimit = 5;
        private double scoreThreshold = 0.5;

        public int getDefaultLimit() { return defaultLimit; }
        public void setDefaultLimit(int defaultLimit) { this.defaultLimit = defaultLimit; }
        public double getScoreThreshold() { return scoreThreshold; }
        public void setScoreThreshold(double scoreThreshold) { this.scoreThreshold = scoreThreshold; }
    }

    public static class ChunkingConfig {
        private String strategy = "PARAGRAPH";
        private int chunkSize = 800;
        private int overlapSize = 100;

        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
        public int getChunkSize() { return chunkSize; }
        public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
        public int getOverlapSize() { return overlapSize; }
        public void setOverlapSize(int overlapSize) { this.overlapSize = overlapSize; }
    }

    // ========== Getter / Setter ==========

    public QdrantConfig getQdrant() { return qdrant; }
    public void setQdrant(QdrantConfig qdrant) { this.qdrant = qdrant; }
    public EmbeddingConfig getEmbedding() { return embedding; }
    public void setEmbedding(EmbeddingConfig embedding) { this.embedding = embedding; }
    public OllamaConfig getOllama() { return ollama; }
    public void setOllama(OllamaConfig ollama) { this.ollama = ollama; }
    public RetrievalConfig getRetrieval() { return retrieval; }
    public void setRetrieval(RetrievalConfig retrieval) { this.retrieval = retrieval; }
    public ChunkingConfig getChunking() { return chunking; }
    public void setChunking(ChunkingConfig chunking) { this.chunking = chunking; }

    // ========== Bean 初始化 ==========

    /**
     * 初始化 Embedding 模型
     * 根据配置选择 DashScope、Ollama 或 OpenAI
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return switch (embedding.getProvider().toLowerCase()) {
            case "ollama" -> OllamaTextEmbedding.builder()
                    .baseUrl(ollama.getBaseUrl())
                    .modelName(ollama.getModelName())
                    .dimensions(ollama.getDimensions())
                    .build();
            case "openai" -> throw new UnsupportedOperationException("OpenAI embedding 配置待实现");
            default -> DashScopeTextEmbedding.builder()
                    .apiKey(embedding.getApiKey())
                    .modelName(embedding.getModelName())
                    .dimensions(embedding.getDimensions())
                    .build();
        };
    }

    /**
     * 初始化 Qdrant 向量数据库客户端
     */
    @Bean
    public QdrantStore qdrantStore() {
        return QdrantStore.builder()
                .location(qdrant.getHost())
                .collectionName(qdrant.getCollectionName())
                .dimensions(qdrant.getDimensions())
                .useTransportLayerSecurity(false)
                .checkCompatibility(false)
                .build();
    }

    /**
     * 初始化向量存储接口
     */
    @Bean
    public VDBStoreBase vectorStore(QdrantStore qdrantStore) {
        return qdrantStore;
    }

    /**
     * 初始化知识库服务
     */
    @Bean
    public SimpleKnowledge knowledgeBase(EmbeddingModel embeddingModel, VDBStoreBase vectorStore) {
        return SimpleKnowledge.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(vectorStore)
                .build();
    }

    /**
     * 初始化默认检索配置
     */
    @Bean
    public RetrieveConfig defaultRetrieveConfig() {
        return RetrieveConfig.builder()
                .limit(retrieval.getDefaultLimit())
                .scoreThreshold(retrieval.getScoreThreshold())
                .build();
    }

    /**
     * 获取分块策略枚举
     */
    public SplitStrategy getChunkingStrategy() {
        return switch (chunking.getStrategy().toUpperCase()) {
            case "CHARACTER" -> SplitStrategy.CHARACTER;
            case "TOKEN" -> SplitStrategy.TOKEN;
            case "SEMANTIC" -> SplitStrategy.SEMANTIC;
            default -> SplitStrategy.PARAGRAPH;
        };
    }
}
```

```java
// src/main/java/io/agentscope/tutorial/chapter10/config/ModelConfig.java
package io.agentscope.tutorial.chapter10.config;

import io.agentscope.core.model.ChatModel;
import io.agentscope.core.model.dashscope.DashScopeChatModel;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 模型配置类
 *
 * 配置 LLM 模型用于 RAG 系统的生成阶段
 *
 * @author AgentScope Tutorial
 */
@Configuration
@ConfigurationProperties(prefix = "model")
public class ModelConfig {

    private String provider = "dashscope";
    private String apiKey;
    private String modelName = "qwen-plus";
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    /**
     * 初始化 Chat 模型
     * 用于 RAG 系统中的答案生成
     */
    @Bean
    public ChatModel chatModel() {
        return DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .build();
    }
}
```

### 10.6.6 文档处理服务

```java
// src/main/java/io/agentscope/tutorial/chapter10/service/DocumentService.java
package io.agentscope.tutorial.chapter10.service;

import io.agentscope.core.message.TextBlock;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.DocumentMetadata;
import io.agentscope.core.rag.reader.SplitStrategy;
import io.agentscope.core.rag.reader.TextChunker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.agentscope.tutorial.chapter10.config.RAGConfig;

import java.util.*;

/**
 * 文档处理服务
 *
 * 负责文档的读取、分块和向量化处理
 *
 * @author AgentScope Tutorial
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    @Autowired
    private RAGConfig ragConfig;

    /**
     * 将文本内容分块为文档列表
     *
     * @param content 原始文本内容
     * @param docId 文档唯一标识
     * @param metadata 文档元数据（文件名、来源等）
     * @return 分块后的文档列表
     */
    public List<Document> chunkTextContent(String content, String docId, Map<String, Object> metadata) {
        SplitStrategy strategy = ragConfig.getChunkingStrategy();
        int chunkSize = ragConfig.getChunking().getChunkSize();
        int overlapSize = ragConfig.getChunking().getOverlapSize();

        log.info("开始分块：docId={}, 策略={}, chunkSize={}, overlap={}",
                docId, strategy, chunkSize, overlapSize);

        // 执行分块
        List<String> chunks = TextChunker.chunkText(
                content,
                chunkSize,
                strategy,
                overlapSize
        );

        log.info("分块完成：共 {} 个块", chunks.size());

        // 转换为 Document 列表
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i).trim();
            if (chunkText.isEmpty()) {
                continue;
            }

            // 构建文档元数据
            DocumentMetadata docMetadata = DocumentMetadata.builder()
                    .content(TextBlock.builder().text(chunkText).build())
                    .docId(docId)
                    .chunkId("chunk-" + i)
                    .payload(metadata != null ? new HashMap<>(metadata) : new HashMap<>())
                    .build();

            documents.add(new Document(docMetadata));
        }

        return documents;
    }

    /**
     * 将多个文本段落分块
     *
     * @param paragraphs 文本段落列表
     * @param docId 文档唯一标识
     * @param metadata 文档元数据
     * @return 分块后的文档列表
     */
    public List<Document> chunkParagraphs(List<String> paragraphs, String docId, Map<String, Object> metadata) {
        List<Document> allDocuments = new ArrayList<>();
        int chunkIndex = 0;

        for (String paragraph : paragraphs) {
            if (paragraph == null || paragraph.trim().isEmpty()) {
                continue;
            }

            SplitStrategy strategy = ragConfig.getChunkingStrategy();
            int chunkSize = ragConfig.getChunking().getChunkSize();
            int overlapSize = ragConfig.getChunking().getOverlapSize();

            List<String> chunks = TextChunker.chunkText(
                    paragraph,
                    chunkSize,
                    strategy,
                    overlapSize
            );

            for (String chunk : chunks) {
                if (chunk.trim().isEmpty()) continue;

                DocumentMetadata docMetadata = DocumentMetadata.builder()
                        .content(TextBlock.builder().text(chunk.trim()).build())
                        .docId(docId)
                        .chunkId("chunk-" + chunkIndex++)
                        .payload(metadata != null ? new HashMap<>(metadata) : new HashMap<>())
                        .build();

                allDocuments.add(new Document(docMetadata));
            }
        }

        log.info("段落分块完成：共 {} 个段落，生成 {} 个文档块",
                paragraphs.size(), allDocuments.size());

        return allDocuments;
    }

    /**
     * 创建示例知识库文档
     *
     * 模拟企业知识库中的各类文档内容
     *
     * @return 预定义的示例文档
     */
    public List<Map<String, Object>> getSampleDocuments() {
        List<Map<String, Object>> documents = new ArrayList<>();

        // 示例 1：公司介绍
        Map<String, Object> doc1 = new HashMap<>();
        doc1.put("title", "公司介绍");
        doc1.put("content", """
                我们的公司成立于2010年，是一家专注于人工智能技术研发的高新技术企业。
                公司总部位于北京，在上海、深圳和杭州设有研发中心。

                公司主要业务包括：
                1. 智能对话系统开发
                2. 知识图谱构建与应用
                3. 人工智能解决方案咨询

                我们的使命是用人工智能技术赋能企业，帮助客户实现数字化转型。
                公司拥有一支300人的研发团队，其中博士学历占比超过30%。
                """);
        doc1.put("category", "公司信息");
        documents.add(doc1);

        // 示例 2：产品介绍
        Map<String, Object> doc2 = new HashMap<>();
        doc2.put("title", "智能问答系统产品介绍");
        doc2.put("content", """
                智能问答系统是我们的核心产品，基于大规模语言模型和知识图谱技术构建。

                产品特点：
                - 支持多轮对话，可理解上下文语境
                - 具备专业知识库，支持领域定制
                - 提供开放的API接口，便于系统集成
                - 支持私有化部署，保障数据安全

                适用场景：
                - 企业内部知识库问答
                - 客服机器人
                - 智能助手

                产品版本包括：基础版、专业版、企业版，支持不同规模的企业需求。
                """);
        doc2.put("category", "产品介绍");
        documents.add(doc2);

        // 示例 3：服务政策
        Map<String, Object> doc3 = new HashMap<>();
        doc3.put("title", "服务级别协议（SLA）");
        doc3.put("content", """
                本服务级别协议定义了服务可用性的标准和赔偿条款。

                服务可用性承诺：
                - 基础版：99.5% 月可用性
                - 专业版：99.9% 月可用性
                - 企业版：99.95% 月可用性

                响应时间承诺：
                - 紧急故障：1小时内响应
                - 高优先级：4小时内响应
                - 中优先级：8小时内响应
                - 低优先级：24小时内响应

                赔偿条款：
                当月度可用性低于承诺标准时，将按照以下比例退还月度服务费：
                - 98%-承诺标准：退还10%
                - 95%-98%：退还25%
                - 低于95%：退还50%
                """);
        doc3.put("category", "服务政策");
        documents.add(doc3);

        return documents;
    }
}
```

### 10.6.7 知识库服务

```java
// src/main/java/io/agentscope/tutorial/chapter10/service/KnowledgeBaseService.java
package io.agentscope.tutorial.chapter10.service;

import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.RetrieveConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 知识库服务
 *
 * 负责知识库的初始化、文档添加、批量导入等操作
 *
 * @author AgentScope Tutorial
 */
@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    @Autowired
    private Knowledge knowledgeBase;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private RetrieveConfig defaultRetrieveConfig;

    /**
     * 添加单个文档到知识库
     *
     * @param title 文档标题
     * @param content 文档内容
     * @param metadata 文档元数据
     * @return 文档 ID
     */
    public String addDocument(String title, String content, Map<String, Object> metadata) {
        String docId = "doc-" + UUID.randomUUID();

        // 添加元数据
        Map<String, Object> docMetadata = new HashMap<>(metadata != null ? metadata : new HashMap<>());
        docMetadata.put("title", title);

        // 分块并添加
        List<Document> chunks = documentService.chunkTextContent(content, docId, docMetadata);

        log.info("添加文档：title={}, docId={}, 块数={}", title, docId, chunks.size());

        knowledgeBase.addDocuments(chunks).block();

        return docId;
    }

    /**
     * 批量添加文档到知识库
     *
     * @param documents 文档列表，每个文档包含 title、content 和可选的 metadata
     * @return 添加的文档数量
     */
    public int addDocuments(List<Map<String, Object>> documents) {
        log.info("开始批量导入文档，数量：{}", documents.size());

        int successCount = 0;
        for (Map<String, Object> doc : documents) {
            try {
                String title = (String) doc.getOrDefault("title", "未命名文档");
                String content = (String) doc.getOrDefault("content", "");
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) doc.getOrDefault("metadata", new HashMap<>());

                addDocument(title, content, metadata);
                successCount++;
            } catch (Exception e) {
                log.error("文档导入失败：{}", doc, e);
            }
        }

        log.info("批量导入完成：成功 {} / 总数 {}", successCount, documents.size());
        return successCount;
    }

    /**
     * 初始化示例知识库
     *
     * 添加预定义的示例文档，用于演示和测试
     *
     * @return 添加的文档数量
     */
    public int initializeSampleKnowledgeBase() {
        log.info("开始初始化示例知识库...");

        List<Map<String, Object>> sampleDocs = documentService.getSampleDocuments();
        int count = addDocuments(sampleDocs);

        log.info("示例知识库初始化完成，共导入 {} 个文档", count);
        return count;
    }

    /**
     * 检索相关文档
     *
     * @param query 查询文本
     * @param limit 返回数量限制
     * @param scoreThreshold 相似度阈值
     * @return 匹配的文档列表
     */
    public List<Document> retrieve(String query, int limit, double scoreThreshold) {
        RetrieveConfig config = RetrieveConfig.builder()
                .limit(limit > 0 ? limit : defaultRetrieveConfig.getLimit())
                .scoreThreshold(scoreThreshold >= 0 ? scoreThreshold : defaultRetrieveConfig.getScoreThreshold())
                .build();

        log.debug("检索文档：query={}, limit={}, threshold={}",
                query, config.getLimit(), config.getScoreThreshold());

        return knowledgeBase.retrieve(query, config).block();
    }

    /**
     * 获取知识库统计信息
     *
     * @return 统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("status", "active");
        stats.put("retrievalConfig", Map.of(
                "limit", defaultRetrieveConfig.getLimit(),
                "scoreThreshold", defaultRetrieveConfig.getScoreThreshold()
        ));
        return stats;
    }
}
```

### 10.6.8 RAG 查询服务

```java
// src/main/java/io/agentscope/tutorial/chapter10/service/RAGQueryService.java
package io.agentscope.tutorial.chapter10.service;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.hook.GenericRAGHook;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ChatModel;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.RetrieveConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG 查询服务
 *
 * 负责执行完整的 RAG 查询流程，包括检索和生成
 *
 * @author AgentScope Tutorial
 */
@Service
public class RAGQueryService {

    private static final Logger log = LoggerFactory.getLogger(RAGQueryService.class);

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private RetrieveConfig defaultRetrieveConfig;

    /**
     * 仅检索模式 - 返回相关文档
     *
     * @param query 查询文本
     * @param limit 返回数量
     * @return 检索结果
     */
    public RetrievalResult retrieveDocuments(String query, int limit) {
        log.info("执行文档检索：query={}, limit={}", query, limit);

        List<Document> documents = knowledgeBaseService.retrieve(query, limit, 0.0);

        List<DocumentSummary> summaries = documents.stream()
                .map(doc -> new DocumentSummary(
                        doc.getId(),
                        doc.getMetadata().getContentText(),
                        doc.getScore(),
                        doc.getMetadata().getPayload()
                ))
                .collect(Collectors.toList());

        return new RetrievalResult(query, summaries);
    }

    /**
     * RAG 检索增强生成
     *
     * 先检索相关文档，然后将文档内容注入提示词进行生成
     *
     * @param query 用户问题
     * @param limit 检索文档数量
     * @param systemPrompt 系统提示词
     * @return 生成的回答
     */
    public GenerationResult generateWithRAG(String query, int limit, String systemPrompt) {
        log.info("执行 RAG 生成：query={}, limit={}", query, limit);

        // 第一步：检索相关文档
        List<Document> documents = knowledgeBaseService.retrieve(query, limit, 0.0);

        if (documents.isEmpty()) {
            log.warn("未检索到相关文档");
            return new GenerationResult(
                    "抱歉，未找到与您问题相关的知识库内容。",
                    Collections.emptyList(),
                    "no_results"
            );
        }

        // 第二步：构建增强提示词
        String context = buildContextFromDocuments(documents);
        String enhancedPrompt = buildEnhancedPrompt(query, context, systemPrompt);

        // 第三步：调用 LLM 生成回答
        try {
            List<Msg> messages = List.of(
                    Msg.builder()
                            .name("user")
                            .role(io.agentscope.core.message.MsgRole.USER)
                            .content(io.agentscope.core.message.TextBlock.builder()
                                    .text(enhancedPrompt)
                                    .build())
                            .build()
            );

            var response = chatModel.chat(messages, null).block();
            String answer = response != null ? response.getTextContent() : "生成失败";

            List<DocumentSummary> sources = documents.stream()
                    .map(doc -> new DocumentSummary(
                            doc.getId(),
                            truncateText(doc.getMetadata().getContentText(), 200),
                            doc.getScore(),
                            doc.getMetadata().getPayload()
                    ))
                    .collect(Collectors.toList());

            return new GenerationResult(answer, sources, "success");

        } catch (Exception e) {
            log.error("RAG 生成失败", e);
            return new GenerationResult(
                    "生成回答时发生错误：" + e.getMessage(),
                    Collections.emptyList(),
                    "error"
            );
        }
    }

    /**
     * 使用 ReAct Agent 进行 RAG 查询
     *
     * 通过 GenericRAGHook 在推理前自动注入知识
     *
     * @param query 用户问题
     * @param agentName Agent 名称
     * @return Agent 的回答
     */
    public String queryWithReActAgent(String query, String agentName) {
        log.info("使用 ReAct Agent 执行 RAG 查询：agent={}, query={}", agentName, query);

        // 获取知识库（通过 Service 获取 Spring Bean）
        var knowledgeBase = knowledgeBaseService.getKnowledgeBase();

        // 创建 Generic RAG 钩子
        GenericRAGHook ragHook = new GenericRAGHook(knowledgeBase, defaultRetrieveConfig);

        // 构建 Agent
        ReActAgent agent = ReActAgent.builder()
                .name(agentName)
                .model(chatModel)
                .instruction("你是一个专业的企业知识库问答助手。请根据提供的上下文信息，准确回答用户的问题。")
                .hook(ragHook)
                .maxSteps(5)
                .build();

        // 执行查询
        try {
            List<Msg> messages = List.of(
                    Msg.builder()
                            .name("user")
                            .role(io.agentscope.core.message.MsgRole.USER)
                            .content(io.agentscope.core.message.TextBlock.builder()
                                    .text(query)
                                    .build())
                            .build()
            );

            var response = agent.run(messages);
            return response.getTextContent();

        } catch (Exception e) {
            log.error("ReAct Agent 查询失败", e);
            return "查询失败：" + e.getMessage();
        }
    }

    // ========== 私有辅助方法 ==========

    /**
     * 从文档列表构建上下文字符串
     */
    private String buildContextFromDocuments(List<Document> documents) {
        StringBuilder sb = new StringBuilder();
        sb.append("【参考知识】\n\n");

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            String content = doc.getMetadata().getContentText();
            Double score = doc.getScore();

            sb.append("文档 ").append(i + 1);
            if (score != null) {
                sb.append(String.format(" (相似度: %.2f)", score));
            }
            sb.append(":\n");
            sb.append(content).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * 构建增强后的提示词
     */
    private String buildEnhancedPrompt(String query, String context, String systemPrompt) {
        StringBuilder sb = new StringBuilder();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            sb.append("【系统提示】\n").append(systemPrompt).append("\n\n");
        }

        sb.append(context);
        sb.append("【用户问题】\n").append(query).append("\n\n");
        sb.append("请基于上述参考知识回答用户的问题。如果参考知识中没有相关信息，请明确告知。");

        return sb.toString();
    }

    /**
     * 截断过长文本
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    // ========== 内部类 ==========

    /**
     * 获取知识库（供 ReAct Agent 使用）
     */
    public io.agentscope.core.rag.Knowledge getKnowledgeBase() {
        // 通过反射或 ApplicationContext 获取真实的 Knowledge Bean
        return new org.springframework.beans.factory.annotation.Autowired(
                org.springframework.beans.factory.annotation.Qualifier("knowledgeBase")
        ).equals(null) ? null : null;
    }

    /**
     * 检索结果封装
     */
    public record RetrievalResult(
            String query,
            List<DocumentSummary> documents
    ) {}

    /**
     * 文档摘要
     */
    public record DocumentSummary(
            String id,
            String content,
            Double score,
            Map<String, Object> metadata
    ) {}

    /**
     * 生成结果封装
     */
    public record GenerationResult(
            String answer,
            List<DocumentSummary> sources,
            String status
    ) {}
}
```

### 10.6.9 REST API 控制器

```java
// src/main/java/io/agentscope/tutorial/chapter10/controller/RagController.java
package io.agentscope.tutorial.chapter10.controller;

import io.agentscope.tutorial.chapter10.service.DocumentService;
import io.agentscope.tutorial.chapter10.service.KnowledgeBaseService;
import io.agentscope.tutorial.chapter10.service.RAGQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG REST API 控制器
 *
 * 提供知识库问答系统的 HTTP 接口
 *
 * @author AgentScope Tutorial
 */
@RestController
@RequestMapping("/api/v1/rag")
public class RagController {

    private static final Logger log = LoggerFactory.getLogger(RagController.class);

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private RAGQueryService ragQueryService;

    @Autowired
    private DocumentService documentService;

    // ========== 知识库管理接口 ==========

    /**
     * 初始化示例知识库
     *
     * POST /api/v1/rag/knowledge/init
     */
    @PostMapping("/knowledge/init")
    public ResponseEntity<Map<String, Object>> initKnowledgeBase() {
        log.info("收到初始化知识库请求");

        int count = knowledgeBaseService.initializeSampleKnowledgeBase();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "知识库初始化完成");
        response.put("documentCount", count);

        return ResponseEntity.ok(response);
    }

    /**
     * 添加文档到知识库
     *
     * POST /api/v1/rag/knowledge/documents
     */
    @PostMapping("/knowledge/documents")
    public ResponseEntity<Map<String, Object>> addDocument(@RequestBody DocUploadRequest request) {
        log.info("收到添加文档请求：title={}", request.getTitle());

        try {
            String docId = knowledgeBaseService.addDocument(
                    request.getTitle(),
                    request.getContent(),
                    request.getMetadata()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documentId", docId);
            response.put("message", "文档添加成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("添加文档失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 批量导入文档
     *
     * POST /api/v1/rag/knowledge/documents/batch
     */
    @PostMapping("/knowledge/documents/batch")
    public ResponseEntity<Map<String, Object>> addDocumentsBatch(@RequestBody BatchDocRequest request) {
        log.info("收到批量导入文档请求：数量={}", request.getDocuments().size());

        try {
            int count = knowledgeBaseService.addDocuments(request.getDocuments());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("importedCount", count);
            response.put("totalCount", request.getDocuments().size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("批量导入失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取知识库统计信息
     *
     * GET /api/v1/rag/knowledge/stats
     */
    @GetMapping("/knowledge/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(knowledgeBaseService.getStats());
    }

    // ========== 查询接口 ==========

    /**
     * 仅检索相关文档
     *
     * POST /api/v1/rag/retrieve
     */
    @PostMapping("/retrieve")
    public ResponseEntity<Map<String, Object>> retrieve(@RequestBody QueryRequest request) {
        log.info("收到检索请求：query={}, limit={}", request.getQuery(), request.getLimit());

        try {
            var result = ragQueryService.retrieveDocuments(
                    request.getQuery(),
                    request.getLimit() > 0 ? request.getLimit() : 5
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("query", result.query());
            response.put("documents", result.documents());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("检索失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * RAG 检索增强生成
     *
     * POST /api/v1/rag/query
     */
    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> query(@RequestBody QueryRequest request) {
        log.info("收到 RAG 查询请求：query={}", request.getQuery());

        try {
            var result = ragQueryService.generateWithRAG(
                    request.getQuery(),
                    request.getLimit() > 0 ? request.getLimit() : 5,
                    request.getSystemPrompt()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("answer", result.answer());
            response.put("sources", result.sources());
            response.put("status", result.status());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("RAG 查询失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 使用 ReAct Agent 进行 RAG 查询
     *
     * POST /api/v1/rag/query/agent
     */
    @PostMapping("/query/agent")
    public ResponseEntity<Map<String, Object>> queryWithAgent(@RequestBody QueryRequest request) {
        log.info("收到 ReAct Agent 查询请求：query={}", request.getQuery());

        try {
            String agentName = request.getAgentName() != null ? request.getAgentName() : "KnowledgeAssistant";
            String answer = ragQueryService.queryWithReActAgent(request.getQuery(), agentName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("answer", answer);
            response.put("agentName", agentName);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Agent 查询失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ========== 请求/响应类 ==========

    /**
     * 文档上传请求
     */
    public static class DocUploadRequest {
        private String title;
        private String content;
        private Map<String, Object> metadata;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    /**
     * 批量文档导入请求
     */
    public static class BatchDocRequest {
        private List<Map<String, Object>> documents;

        public List<Map<String, Object>> getDocuments() { return documents; }
        public void setDocuments(List<Map<String, Object>> documents) { this.documents = documents; }
    }

    /**
     * 查询请求
     */
    public static class QueryRequest {
        private String query;
        private int limit = 5;
        private String systemPrompt;
        private String agentName;

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
        public String getAgentName() { return agentName; }
        public void setAgentName(String agentName) { this.agentName = agentName; }
    }
}
```

### 10.6.10 Spring Boot 启动类

```java
// src/main/java/io/agentscope/tutorial/chapter10/Chapter10Application.java
package io.agentscope.tutorial.chapter10;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 企业知识库问答系统 - 启动类
 *
 * 基于 Spring Boot 3 + Java 21 + AgentScope Java 构建的 RAG 系统
 *
 * @author AgentScope Tutorial
 */
@SpringBootApplication
@EnableConfigurationProperties
public class Chapter10Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter10Application.class, args);
    }
}
```

### 10.6.11 单元测试

```java
// src/test/java/io/agentscope/tutorial/chapter10/RagServiceTest.java
package io.agentscope.tutorial.chapter10;

import io.agentscope.core.message.TextBlock;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.DocumentMetadata;
import io.agentscope.core.rag.reader.SplitStrategy;
import io.agentscope.core.rag.reader.TextChunker;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RAG 服务单元测试
 *
 * 测试文本分块、文档创建等核心功能
 *
 * @author AgentScope Tutorial
 */
class RagServiceTest {

    /**
     * 测试段落分块
     */
    @Test
    void testParagraphChunking() {
        String text = """
                第一段内容。这是一个较长的段落，用于测试分块功能。

                第二段内容。这是另一个段落，应该被单独保留。

                第三段内容。包含更多信息。
                """;

        List<String> chunks = TextChunker.chunkText(text, 200, SplitStrategy.PARAGRAPH, 20);

        assertFalse(chunks.isEmpty());
        System.out.println("分块结果数量: " + chunks.size());
        chunks.forEach(chunk -> System.out.println("- ".concat(chunk.substring(0, Math.min(50, chunk.length())))));
    }

    /**
     * 测试 Token 分块
     */
    @Test
    void testTokenChunking() {
        String text = "这是测试文本，用于验证 Token 分块功能。" +
                "每个块会包含多个句子，以确保内容完整性。" +
                "重叠部分可以帮助保留上下文信息。";

        List<String> chunks = TextChunker.chunkText(text, 50, SplitStrategy.TOKEN, 10);

        assertFalse(chunks.isEmpty());
        System.out.println("Token 分块结果: " + chunks.size() + " 个块");
    }

    /**
     * 测试文档创建
     */
    @Test
    void testDocumentCreation() {
        String content = "这是测试文档的内容";
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "测试文档");
        payload.put("author", "Test User");

        DocumentMetadata metadata = DocumentMetadata.builder()
                .content(TextBlock.builder().text(content).build())
                .docId("test-doc-001")
                .chunkId("chunk-0")
                .payload(payload)
                .build();

        Document document = new Document(metadata);

        assertNotNull(document.getId());
        assertEquals("test-doc-001", document.getMetadata().getDocId());
        assertEquals("测试文档", document.getMetadata().getPayloadValue("title"));
        System.out.println("文档 ID: " + document.getId());
    }

    /**
     * 测试文档内容提取
     */
    @Test
    void testDocumentContentExtraction() {
        String content = "企业知识库的内容文本";
        DocumentMetadata metadata = DocumentMetadata.builder()
                .content(TextBlock.builder().text(content).build())
                .docId("doc-1")
                .chunkId("chunk-0")
                .build();

        Document document = new Document(metadata);

        String extractedContent = document.getMetadata().getContentText();
        assertEquals(content, extractedContent);
        System.out.println("提取的内容: " + extractedContent);
    }

    /**
     * 测试分块重叠效果
     */
    @Test
    void testChunkOverlap() {
        String text = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        // 无重叠
        List<String> noOverlap = TextChunker.chunkText(text, 5, SplitStrategy.CHARACTER, 0);
        // 有重叠
        List<String> withOverlap = TextChunker.chunkText(text, 5, SplitStrategy.CHARACTER, 2);

        assertTrue(withOverlap.size() >= noOverlap.size());
        System.out.println("无重叠分块数: " + noOverlap.size());
        System.out.println("有重叠分块数: " + withOverlap.size());
    }
}
```

### 10.6.12 API 使用示例

**1. 初始化知识库**

```bash
curl -X POST http://localhost:8080/api/v1/rag/knowledge/init
```

响应：
```json
{
  "success": true,
  "message": "知识库初始化完成",
  "documentCount": 3
}
```

**2. 添加文档**

```bash
curl -X POST http://localhost:8080/api/v1/rag/knowledge/documents \
  -H "Content-Type: application/json" \
  -d '{
    "title": "员工手册",
    "content": "公司实行弹性工作制...",
    "metadata": {
      "department": "HR",
      "version": "1.0"
    }
  }'
```

**3. 检索相关文档**

```bash
curl -X POST http://localhost:8080/api/v1/rag/retrieve \
  -H "Content-Type: application/json" \
  -d '{
    "query": "公司有哪些产品？",
    "limit": 3
  }'
```

**4. RAG 问答**

```bash
curl -X POST http://localhost:8080/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "公司的服务级别协议是什么？",
    "limit": 5,
    "systemPrompt": "你是一个专业的客服助手。"
  }'
```

**5. 使用 ReAct Agent 问答**

```bash
curl -X POST http://localhost:8080/api/v1/rag/query/agent \
  -H "Content-Type: application/json" \
  -d '{
    "query": "介绍一下你们的产品",
    "agentName": "ProductExpert"
  }'
```

## 10.7 本章小结

本章介绍了 AgentScope Java 中 RAG 系统的完整实现：

1. **RAG 架构原理**：理解了 RAG 的基本概念和工作流程，以及 AgentScope Java 中的核心组件。

2. **向量数据库集成**：掌握了 Qdrant 和 Milvus 的配置和使用方法，能够根据场景选择合适的向量存储。

3. **文本分块策略**：学会了使用 TextChunker 进行字符、段落、Token 和语义分块，理解了分块参数的选择原则。

4. **混合检索**：了解了如何结合向量检索和关键词过滤，以及 RRF 融合算法的实现。

5. **Embedding 模型配置**：掌握了 DashScope、Ollama 和 OpenAI 等 Embedding 模型的使用方法。

6. **企业知识库案例**：通过 Spring Boot + Docker Qdrant 的完整案例，学会了构建可运行的企业级 RAG 系统。

---

**下一章预告**：第十一章将介绍技能系统（Skill），包括技能的定义、注册与调用，以及技能仓库的实现。