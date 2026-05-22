# 第六章：模型集成

AgentScope Java 支持多种大语言模型（LLM）提供商，通过统一的 `Model` 接口和可插拔的 `Formatter` 机制，实现了灵活的多模型集成。本章将详细介绍模型客户端架构，以及如何配置和使用各种模型提供商。

## 6.1 模型客户端架构

### 核心接口设计

AgentScope Java 的模型层采用清晰的接口分离设计：

```
┌─────────────────────────────────────────────────────────────────┐
│                        ReActAgent                                │
│                    （使用 Model 接口）                           │
└─────────────────────────┬───────────────────────────────────────┘
                          │ stream(messages, tools, options)
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Model 接口                                   │
│  - stream(): Flux<ChatResponse>                                 │
│  - getModelName(): String                                        │
└─────────────────────────┬───────────────────────────────────────┘
                          │
          ┌───────────────┼───────────────┬───────────────┐
          ▼               ▼               ▼               ▼
   ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐
   │DashScope   │  │ OpenAI     │  │ Ollama     │  │ Gemini     │
   │ChatModel   │  │ ChatModel  │  │ ChatModel  │  │ ChatModel  │
   └────────────┘  └────────────┘  └────────────┘  └────────────┘
          │               │               │               │
          ▼               ▼               ▼               ▼
   ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐
   │Formatter   │  │Formatter   │  │Formatter   │  │Formatter   │
   │(DAShScope) │  │(OpenAI)    │  │(Ollama)    │  │(Gemini)    │
   └────────────┘  └────────────┘  └────────────┘  └────────────┘
```

### Model 接口定义

```java
// io.agentscope.core.model.Model
public interface Model {
    /**
     * 流式获取聊天完成响应。
     *
     * @param messages AgentScope 消息列表
     * @param tools 工具模式列表（可为 null）
     * @param options 生成选项（可为 null 使用默认值）
     * @return ChatResponse 的响应流
     */
    Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> tools, GenerateOptions options);

    /**
     * 获取模型名称，用于日志和标识。
     */
    String getModelName();
}
```

### Formatter 格式化器接口

`Formatter` 接口负责在 AgentScope 消息格式和提供商特定格式之间进行转换：

```java
// io.agentscope.core.formatter.Formatter
public interface Formatter<TReq, TResp, TParams> {
    /**
     * 将 AgentScope 消息格式化为提供商请求格式。
     */
    List<TReq> format(List<Msg> msgs);

    /**
     * 解析提供商响应为 AgentScope ChatResponse。
     */
    ChatResponse parseResponse(TResp response, Instant startTime);

    /**
     * 应用生成选项到请求参数。
     */
    void applyOptions(TParams paramsBuilder, GenerateOptions options, GenerateOptions defaultOptions);

    /**
     * 应用工具模式到请求参数。
     */
    void applyTools(TParams paramsBuilder, List<ToolSchema> tools);

    /**
     * 应用工具选择配置。
     */
    default void applyToolChoice(TParams paramsBuilder, ToolChoice toolChoice) {}

    /**
     * 应用工具模式（含提供商检测）。
     */
    default void applyTools(TParams paramsBuilder, List<ToolSchema> tools,
                           String baseUrl, String modelName) {
        applyTools(paramsBuilder, tools);
    }
}
```

### GenerateOptions 生成选项

`GenerateOptions` 提供统一的生成配置，包括：

- `temperature` - 采样温度
- `maxTokens` - 最大 token 数
- `topP` - Top-P 采样
- `stop` - 停止序列
- `apiKey` / `baseUrl` - 运行时覆盖
- `thinkingBudget` - 思考预算（部分模型支持）
- `cacheControl` - 缓存控制

### 模型选择策略

| 场景 | 推荐模型 | 理由 |
|------|---------|------|
| 中文对话 | DashScope (qwen-plus) | 优化中文，理解文化背景 |
| 代码生成 | OpenAI (gpt-4) | 代码能力强 |
| 本地部署 | Ollama | 完全离线，隐私保护 |
| 多模态 | Gemini / DashScope VL | 支持图像理解 |
| 成本敏感 | Ollama / DeepSeek | 本地或低价 |

---

## 6.2 DashScope（阿里云百炼）

[DashScope](https://www.aliyun.com/product/dashscope) 是阿里云的模型服务平台，提供了通义千问系列模型，支持文本和视觉任务。

### 核心类

```java
io.agentscope.core.model.DashScopeChatModel
```

### 配置示例

```java
// 创建 DashScope 模型（基础配置）
Model dashscopeModel = DashScopeChatModel.builder()
    .apiKey(System.getenv("DASHSCOPE_API_KEY"))
    .modelName("qwen-plus")          // qwen-plus, qwen-max, qwen-vl-plus 等
    .stream(true)                     // 启用流式输出
    .build();

// 配置思考模式
Model thinkingModel = DashScopeChatModel.builder()
    .apiKey(System.getenv("DASHSCOPE_API_KEY"))
    .modelName("qwen-plus")
    .stream(true)
    .enableThinking(true)             // 启用思考模式
    .defaultOptions(GenerateOptions.builder()
        .thinkingBudget(2048)        // 思考 token 预算
        .build())
    .build();

// 配置搜索增强
Model searchModel = DashScopeChatModel.builder()
    .apiKey(System.getenv("DASHSCOPE_API_KEY"))
    .modelName("qwen-plus")
    .stream(true)
    .enableSearch(true)               // 启用搜索增强
    .build();

// 配置视觉模型（支持图片理解）
Model visionModel = DashScopeChatModel.builder()
    .apiKey(System.getenv("DASHSCOPE_API_KEY"))
    .modelName("qwen-vl-plus")
    .stream(true)
    .build();

// 配置加密传输（企业安全需求）
Model encryptedModel = DashScopeChatModel.builder()
    .apiKey(System.getenv("DASHSCOPE_API_KEY"))
    .modelName("qwen-max")
    .stream(true)
    .enableEncrypt(true)             // 启用加密传输
    .build();
```

### DashScope 支持的模型

| 模型名称 | 类型 | 说明 |
|---------|------|------|
| qwen-plus | 文本 | 高性能，性价比优 |
| qwen-max | 文本 | 最强能力，适合复杂任务 |
| qwen-turbo | 文本 | 快速响应，低延迟 |
| qwen-vl-plus | 视觉 | 图像理解，多模态 |
| qwen-vl-max | 视觉 | 最强视觉能力 |
| qwq-32b | 推理 | 开源推理模型 |
| qwen-coder-plus | 代码 | 代码专用模型 |

### 格式化器配置

```java
// 使用默认格式化器
Formatter<DashScopeMessage, DashScopeResponse, DashScopeRequest> formatter =
    new DashScopeChatFormatter();

// 使用多代理格式化器（支持复杂对话场景）
Formatter<DashScopeMessage, DashScopeResponse, DashScopeRequest> multiAgentFormatter =
    new DashScopeMultiAgentFormatter();
```

---

## 6.3 OpenAI 兼容接口

OpenAI 兼容接口可用于连接任何遵循 OpenAI API 格式的服务商，包括：

- OpenAI GPT 系列
- DeepSeek
- Zhipu GLM
- Azure OpenAI
- 其他兼容 API 的服务商

### 核心类

```java
io.agentscope.core.model.OpenAIChatModel
```

### 配置示例

```java
// 基础 OpenAI 配置
Model openaiModel = OpenAIChatModel.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .modelName("gpt-4o")
    .stream(true)
    .baseUrl("https://api.openai.com/v1")
    .build();

// DeepSeek 配置
Model deepseekModel = OpenAIChatModel.builder()
    .apiKey(System.getenv("DEEPSEEK_API_KEY"))
    .modelName("deepseek-chat")
    .stream(true)
    .baseUrl("https://api.deepseek.com/v1")
    .formatter(new DeepSeekFormatter())  // 使用 DeepSeek 专用格式化器
    .build();

// Zhipu GLM 配置
Model glmModel = OpenAIChatModel.builder()
    .apiKey(System.getenv("ZHIPU_API_KEY"))
    .modelName("glm-4")
    .stream(true)
    .baseUrl("https://open.bigmodel.cn/api/paas/v4")
    .formatter(new GLMFormatter())       // 使用 GLM 专用格式化器
    .build();

// Azure OpenAI 配置
Model azureModel = OpenAIChatModel.builder()
    .apiKey(System.getenv("AZURE_OPENAI_KEY"))
    .modelName("gpt-4")
    .stream(true)
    .baseUrl("https://YOUR_RESOURCE.openai.azure.com")
    .endpointPath("/openai/deployments/YOUR_DEPLOYMENT/chat/completions?api-version=2024-02-15-preview")
    .build();

// 自定义端点路径
Model customModel = OpenAIChatModel.builder()
    .apiKey("your-api-key")
    .modelName("llama3")
    .stream(true)
    .baseUrl("https://your-api-server.com")
    .endpointPath("/api/v1/chat/completions")  // 自定义路径
    .build();
```

### 常用格式化器

```java
// OpenAI 标准格式化器
OpenAIChatFormatter formatter = new OpenAIChatFormatter();

// DeepSeek 格式化器（处理特定差异）
DeepSeekFormatter deepseekFormatter = new DeepSeekFormatter();

// GLM 格式化器（处理智谱AI的特殊参数）
GLMFormatter glmFormatter = new GLMFormatter();
```

### OpenAI 兼容提供商对比

| 提供商 | baseUrl | 模型名称 | 格式化器 |
|--------|---------|---------|---------|
| OpenAI | api.openai.com/v1 | gpt-4o, gpt-4-turbo | OpenAIChatFormatter |
| DeepSeek | api.deepseek.com/v1 | deepseek-chat | DeepSeekFormatter |
| Zhipu GLM | open.bigmodel.cn/api/paas/v4 | glm-4, glm-4-flash | GLMFormatter |
| Azure | 你的资源.openai.azure.com | 你的部署名 | OpenAIChatFormatter |

---

## 6.4 Ollama 本地模型

[Ollama](https://ollama.com/) 允许你在本地运行开源大语言模型，支持 Llama 3、Mistral、Qwen 等多种模型。

### 安装 Ollama

```bash
# macOS/Linux
curl -fsSL https://ollama.com/install.sh | sh

# Windows (使用 WSL 或 Docker)
docker run -d -p 11434:11434 ollama/ollama

# 下载模型
ollama pull llama3.2
ollama pull qwen2.5:7b
ollama pull mistral
```

### 核心类

```java
io.agentscope.core.model.OllamaChatModel
io.agentscope.core.model.ollama.OllamaOptions
```

### 配置示例

```java
// 基础 Ollama 配置（假设 Ollama 运行在 localhost:11434）
Model ollamaModel = OllamaChatModel.builder()
    .modelName("llama3.2")
    .baseUrl("http://localhost:11434")
    .build();

// 配置更多参数
Model configuredOllama = OllamaChatModel.builder()
    .modelName("qwen2.5:7b")
    .baseUrl("http://localhost:11434")
    .defaultOptions(OllamaOptions.builder()
        .temperature(0.7)
        .numCtx(4096)              // 上下文窗口大小
        .topP(0.9)
        .repeatPenalty(1.1)
        .seed(42)                  // 固定随机种子（可复现）
        .numKeep(32)               // 保持的 token 数
        .build())
    .build();

// 使用工具调用（需要支持 function calling 的模型）
Model toolOllama = OllamaChatModel.builder()
    .modelName("llama3.2")
    .baseUrl("http://localhost:11434")
    .defaultOptions(OllamaOptions.builder()
        .numCtx(8192)              // 更大的上下文以支持工具
        .build())
    .build();

// 使用多代理格式化器
OllamaMultiAgentFormatter multiAgentFormatter = new OllamaMultiAgentFormatter();
Model multiAgentOllama = OllamaChatModel.builder()
    .modelName("llama3.2")
    .baseUrl("http://localhost:11434")
    .formatter(multiAgentFormatter)
    .build();
```

### OllamaOptions 常用配置

```java
OllamaOptions options = OllamaOptions.builder()
    .temperature(0.8)              // 采样温度（0-2）
    .topP(0.9)                    // Top-P 采样
    .numCtx(4096)                 // 上下文 token 数
    .numKeep(256)                 // 保持的最近 token
    .seed(-1)                     // 随机种子，-1 为随机
    .numPredict(256)              // 最大生成 token 数
    .repeatPenalty(1.1)          // 重复惩罚
    .tfsZ(0.5)                    // TFS-Z 采样
    .numBatch(512)                // 批处理大小
    .repeatLastN(64)             // 重复检测窗口
    .mirostat(0)                  // Mirostat 采样模式
    .mirostatTau(5.0)            // Mirostat 目标熵
    .mirostatEta(0.1)            // Mirostat 学习率
    .penaltyPrompt("")           // 惩罚提示
    .debug(false)                 // 调试模式
    .stop(null)                  // 停止序列
    .build();
```

### Ollama 模型推荐

| 模型 | 参数量 | 最低内存 | 适用场景 |
|------|--------|---------|---------|
| llama3.2 | 3B | 4GB | 快速响应，资源受限 |
| llama3.2:3b | 3B | 4GB | 通用对话 |
| qwen2.5:7b | 7B | 8GB | 中文优化 |
| mistral | 7B | 8GB | 英文任务 |
| codellama | 7B | 8GB | 代码生成 |
| nomic-embed-text | 137M | 1GB | 向量嵌入 |

---

## 6.5 Google Gemini

Gemini 是 Google 的多模态 AI 模型，支持文本、图像、视频和音频理解。

### 前提条件

```xml
<!-- pom.xml 添加 Gemini SDK 依赖 -->
<dependency>
    <groupId>com.google.genai</groupId>
    <artifactId>genai-java</artifactId>
    <version>0.11.0</version>
</dependency>
```

### 核心类

```java
io.agentscope.core.model.GeminiChatModel
```

### 配置示例

```java
// 基础 Gemini 配置（使用 API Key）
Model geminiModel = GeminiChatModel.builder()
    .apiKey(System.getenv("GEMINI_API_KEY"))
    .modelName("gemini-2.0-flash")    // gemini-2.0-flash, gemini-1.5-pro, gemini-1.5-flash
    .streamEnabled(true)
    .build();

// Gemini Flash（快速响应）
Model flashModel = GeminiChatModel.builder()
    .apiKey(System.getenv("GEMINI_API_KEY"))
    .modelName("gemini-2.0-flash")
    .streamEnabled(true)
    .build();

// Gemini Pro（复杂任务）
Model proModel = GeminiChatModel.builder()
    .apiKey(System.getenv("GEMINI_API_KEY"))
    .modelName("gemini-1.5-pro")
    .streamEnabled(false)
    .defaultOptions(GenerateOptions.builder()
        .maxTokens(8192)
        .temperature(0.9)
        .build())
    .build();

// 自定义 Base URL（代理场景）
Model proxiedGemini = GeminiChatModel.builder()
    .apiKey(System.getenv("GEMINI_API_KEY"))
    .baseUrl("https://generativelanguage.googleapis.com")
    .modelName("gemini-2.0-flash")
    .streamEnabled(true)
    .build();
```

### Vertex AI 配置（企业用户）

```java
import com.google.auth.oauth2.GoogleCredentials;
import com.google.genai.types.ClientOptions;

// 使用服务账号凭证
GoogleCredentials credentials = GoogleCredentials.fromStream(
    new FileInputStream("path/to/service-account.json")
);

Model vertexModel = GeminiChatModel.builder()
    .project("your-gcp-project")
    .location("us-central1")
    .vertexAI(true)
    .credentials(credentials)
    .modelName("gemini-1.5-pro")
    .streamEnabled(true)
    .build();
```

### Gemini 模型选择

| 模型 | 特点 | 适用场景 |
|------|------|---------|
| gemini-2.0-flash | 快速响应，新版本 | 实时对话，摘要 |
| gemini-2.0-flash-lite | 最便宜 | 高频调用，简单任务 |
| gemini-1.5-flash | 性价比 | 通用任务 |
| gemini-1.5-pro | 长上下文 | 复杂推理，代码 |
| gemini-1.5-pro-002 | 稳定性优化 | 生产环境 |
| gemini-exp | 实验版本 | 测试新功能 |

---

## 6.6 消息格式化器配置

Formatter 是模型集成的核心组件，负责在 AgentScope 内部消息格式和提供商特定 API 格式之间进行转换。

### 格式化器架构

```
AgentScope Msg ──format()──► 提供商请求格式 ──API 调用──► 提供商响应
                         ◄──parseResponse()──                       │
                                                                   ▼
                                              ChatResponse（统一格式）
```

### 可用格式化器一览

| 提供商 | 格式化器类 | 包路径 |
|--------|-----------|--------|
| DashScope | DashScopeChatFormatter | io.agentscope.core.formatter.dashscope |
| DashScope 多代理 | DashScopeMultiAgentFormatter | io.agentscope.core.formatter.dashscope |
| OpenAI | OpenAIChatFormatter | io.agentscope.core.formatter.openai |
| DeepSeek | DeepSeekFormatter | io.agentscope.core.formatter.openai |
| GLM | GLMFormatter | io.agentscope.core.formatter.openai |
| Ollama | OllamaChatFormatter | io.agentscope.core.formatter.ollama |
| Ollama 多代理 | OllamaMultiAgentFormatter | io.agentscope.core.formatter.ollama |
| Gemini | GeminiChatFormatter | io.agentscope.core.formatter.gemini |
| Anthropic | AnthropicChatFormatter | io.agentscope.core.formatter.anthropic |

### 自定义格式化器

如需支持新的模型提供商，可实现 `Formatter` 接口：

```java
/**
 * 自定义模型格式化器示例
 */
public class CustomModelFormatter implements
        Formatter<CustomMessage, CustomResponse, CustomRequest.Builder> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<CustomMessage> format(List<Msg> msgs) {
        return msgs.stream()
            .map(this::convertToCustomMessage)
            .collect(Collectors.toList());
    }

    @Override
    public ChatResponse parseResponse(CustomResponse response, Instant startTime) {
        String content = response.getContent();
        ChatUsage usage = ChatUsage.builder()
            .promptTokens(response.getPromptTokens())
            .completionTokens(response.getCompletionTokens())
            .totalTokens(response.getTotalTokens())
            .build();

        return ChatResponse.builder()
            .id(response.getId())
            .model(response.getModel())
            .content(ChatContent.ofText(content))
            .usage(usage)
            .build();
    }

    @Override
    public void applyOptions(CustomRequest.Builder builder,
                            GenerateOptions options,
                            GenerateOptions defaultOptions) {
        if (options != null) {
            if (options.getTemperature() != null) {
                builder.temperature(options.getTemperature());
            }
            if (options.getMaxTokens() != null) {
                builder.maxTokens(options.getMaxTokens());
            }
        }
    }

    @Override
    public void applyTools(CustomRequest.Builder builder, List<ToolSchema> tools) {
        if (tools != null && !tools.isEmpty()) {
            List<CustomTool> customTools = tools.stream()
                .map(this::convertToCustomTool)
                .collect(Collectors.toList());
            builder.tools(customTools);
        }
    }

    private CustomMessage convertToCustomMessage(Msg msg) {
        // 实现消息转换逻辑
        return null; // 实现细节
    }

    private CustomTool convertToCustomTool(ToolSchema schema) {
        // 实现工具转换逻辑
        return null; // 实现细节
    }
}
```

### 格式化器配置最佳实践

```java
// 1. 使用默认格式化器（推荐）
// 大多数场景下，使用提供商默认的格式化器即可

// 2. 明确指定格式化器（当有多个选项时）
DashScopeChatFormatter dashFormatter = new DashScopeChatFormatter();
DashScopeChatModel model = DashScopeChatModel.builder()
    .apiKey(apiKey)
    .modelName("qwen-plus")
    .formatter(dashFormatter)  // 明确指定
    .build();

// 3. 在 ReActAgent 中配置格式化器
ReActAgent agent = ReActAgent.builder()
    .name("Assistant")
    .sysPrompt("你是 AI 助手")
    .model(dashscopeModel)
    .build();

// 4. 运行时通过 GenerateOptions 传递
GenerateOptions options = GenerateOptions.builder()
    .temperature(0.7)
    .maxTokens(1000)
    .build();

agent.run(userMsg, options);
```

---

## 6.7 【案例】多模型切换对比系统

本案例实现一个完整的 Spring Boot 应用，支持配置多种模型客户端、模型切换对比和 H2 嵌入式数据库存储。

### 项目结构

```
src/main/java/io/agentscope/tutorial/chapter06/
├── Chapter06Application.java          # Spring Boot 启动类
├── config/
│   └── ModelConfig.java                # 模型配置类
├── controller/
│   └── ModelCompareController.java     # Web 控制器
├── entity/
│   ├── ModelProvider.java             # 模型提供商实体
│   └── ChatConfig.java                # 聊天配置实体
├── repository/
│   ├── ModelProviderRepository.java    # 提供商数据访问
│   └── ChatConfigRepository.java      # 配置数据访问
└── service/
    ├── ModelService.java              # 模型服务
    ├── DashScopeService.java          # DashScope 服务
    ├── OpenAIService.java             # OpenAI 服务
    └── OllamaService.java             # Ollama 服务
```

### pom.xml 依赖配置

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
    <artifactId>chapter06-model-integration</artifactId>
    <version>1.0.0</version>
    <name>chapter06-model-integration</name>
    <description>AgentScope Java Chapter 06 - Model Integration Example</description>

    <properties>
        <java.version>21</java.version>
        <agentscope.version>1.0.0</agentscope.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- H2 数据库 -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- AgentScope Core -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-core</artifactId>
            <version>${agentscope.version}</version>
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

### application.yaml 配置

```yaml
server:
  port: 8080

spring:
  application:
    name: chapter06-model-integration

  # H2 数据库配置（嵌入式）
  datasource:
    url: jdbc:h2:mem:modeldb
    driver-class-name: org.h2.Driver
    username: sa
    password:

  # JPA 配置
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.H2Dialect

  # H2 控制台（开发时启用）
  h2:
    console:
      enabled: true
      path: /h2-console

# 自定义模型配置
model:
  # DashScope 配置
  dashscope:
    api-key: ${DASHSCOPE_API_KEY:demo-key}
    base-url: https://dashscope.aliyuncs.com/api/v1
    default-model: qwen-plus

  # OpenAI 配置
  openai:
    api-key: ${OPENAI_API_KEY:demo-key}
    base-url: https://api.openai.com/v1
    default-model: gpt-4o

  # Ollama 配置
  ollama:
    base-url: http://localhost:11434
    default-model: llama3.2

  # Gemini 配置
  gemini:
    api-key: ${GEMINI_API_KEY:demo-key}
    default-model: gemini-2.0-flash

# 日志配置
logging:
  level:
    io.agentscope: DEBUG
    root: INFO
```

### 实体类

```java
// io.agentscope.tutorial.chapter06.entity.ModelProvider
package io.agentscope.tutorial.chapter06.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 模型提供商实体类
 * 用于存储不同模型提供商的信息
 */
@Entity
@Table(name = "model_provider")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 提供商名称 (如: dashscope, openai, ollama, gemini)
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * 显示名称
     */
    @Column(nullable = false)
    private String displayName;

    /**
     * API Key（加密存储）
     */
    @Column
    private String apiKey;

    /**
     * Base URL
     */
    @Column
    private String baseUrl;

    /**
     * 是否启用
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /**
     * 优先级（数字越小优先级越高）
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 100;

    /**
     * 备注
     */
    @Column
    private String remark;

    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createTime = LocalDateTime.now();

    /**
     * 更新时间
     */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updateTime = LocalDateTime.now();
}
```

```java
// io.agentscope.tutorial.chapter06.entity.ChatConfig
package io.agentscope.tutorial.chapter06.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天配置实体类
 * 用于存储用户的聊天配置和历史
 */
@Entity
@Table(name = "chat_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 配置名称
     */
    @Column(nullable = false)
    private String name;

    /**
     * 模型提供商类型
     */
    @Column(nullable = false)
    private String providerType;

    /**
     * 模型名称
     */
    @Column(nullable = false)
    private String modelName;

    /**
     * Temperature 参数
     */
    @Column
    @Builder.Default
    private Double temperature = 0.7;

    /**
     * 最大 Token 数
     */
    @Column
    @Builder.Default
    private Integer maxTokens = 2000;

    /**
     * 系统提示词
     */
    @Column(length = 4000)
    private String systemPrompt;

    /**
     * 是否启用流式输出
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean streamEnabled = true;

    /**
     * 是否为默认配置
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createTime = LocalDateTime.now();

    /**
     * 更新时间
     */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updateTime = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
```

### Repository 接口

```java
// io.agentscope.tutorial.chapter06.repository.ModelProviderRepository
package io.agentscope.tutorial.chapter06.repository;

import io.agentscope.tutorial.chapter06.entity.ModelProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 模型提供商数据访问层
 */
@Repository
public interface ModelProviderRepository extends JpaRepository<ModelProvider, Long> {

    /**
     * 根据名称查找提供商
     */
    Optional<ModelProvider> findByName(String name);

    /**
     * 查找所有启用的提供商
     */
    List<ModelProvider> findByEnabledTrueOrderByPriorityAsc();

    /**
     * 检查提供商是否存在
     */
    boolean existsByName(String name);
}
```

```java
// io.agentscope.tutorial.chapter06.repository.ChatConfigRepository
package io.agentscope.tutorial.chapter06.repository;

import io.agentscope.tutorial.chapter06.entity.ChatConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 聊天配置数据访问层
 */
@Repository
public interface ChatConfigRepository extends JpaRepository<ChatConfig, Long> {

    /**
     * 查找所有非默认配置
     */
    List<ChatConfig> findByIsDefaultFalse();

    /**
     * 查找指定提供商的配置
     */
    List<ChatConfig> findByProviderType(String providerType);

    /**
     * 查找默认配置
     */
    Optional<ChatConfig> findByIsDefaultTrue();

    /**
     * 根据名称查找配置
     */
    Optional<ChatConfig> findByName(String name);
}
```

### 模型服务类

```java
// io.agentscope.tutorial.chapter06.service.ModelService
package io.agentscope.tutorial.chapter06.service;

import io.agentscope.core.formatter.Formatter;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.formatter.gemini.GeminiChatFormatter;
import io.agentscope.core.formatter.ollama.OllamaChatFormatter;
import io.agentscope.core.formatter.openai.DeepSeekFormatter;
import io.agentscope.core.formatter.openai.GLMFormatter;
import io.agentscope.core.formatter.openai.OpenAIChatFormatter;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型服务 - 统一管理多种模型客户端
 *
 * <p>支持模型的热切换和动态配置
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ModelService {

    private final ModelProviderRepository providerRepository;
    private final ChatConfigRepository configRepository;

    // 模型客户端缓存（线程安全）
    private final Map<String, Model> modelCache = new ConcurrentHashMap<>();

    @Value("${model.dashscope.api-key:demo}")
    private String dashscopeApiKey;

    @Value("${model.dashscope.base-url:https://dashscope.aliyuncs.com/api/v1}")
    private String dashscopeBaseUrl;

    @Value("${model.dashscope.default-model:qwen-plus}")
    private String dashscopeDefaultModel;

    @Value("${model.openai.api-key:demo}")
    private String openaiApiKey;

    @Value("${model.openai.base-url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    @Value("${model.openai.default-model:gpt-4o}")
    private String openaiDefaultModel;

    @Value("${model.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${model.ollama.default-model:llama3.2}")
    private String ollamaDefaultModel;

    @Value("${model.gemini.api-key:demo}")
    private String geminiApiKey;

    @Value("${model.gemini.default-model:gemini-2.0-flash}")
    private String geminiDefaultModel;

    /**
     * 初始化默认模型客户端
     */
    @PostConstruct
    public void init() {
        log.info("初始化模型服务...");

        // 初始化 DashScope 模型
        registerDashScopeModel("qwen-plus");
        registerDashScopeModel("qwen-max");
        registerDashScopeModel("qwen-turbo");

        // 初始化 OpenAI 模型
        registerOpenAIModel("gpt-4o", openaiBaseUrl, new OpenAIChatFormatter());
        registerOpenAIModel("deepseek-chat", "https://api.deepseek.com/v1", new DeepSeekFormatter());
        registerOpenAIModel("glm-4", "https://open.bigmodel.cn/api/paas/v4", new GLMFormatter());

        // 初始化 Ollama 模型
        registerOllamaModel("llama3.2");
        registerOllamaModel("qwen2.5:7b");

        // 初始化 Gemini 模型
        registerGeminiModel("gemini-2.0-flash");
        registerGeminiModel("gemini-1.5-pro");

        log.info("模型服务初始化完成，已注册 {} 个模型", modelCache.size());
    }

    // ==================== 模型注册方法 ====================

    /**
     * 注册 DashScope 模型
     */
    public void registerDashScopeModel(String modelName) {
        String cacheKey = "dashscope:" + modelName;
        Model model = DashScopeChatModel.builder()
                .apiKey(dashscopeApiKey)
                .baseUrl(dashscopeBaseUrl)
                .modelName(modelName)
                .stream(true)
                .formatter(new DashScopeChatFormatter())
                .defaultOptions(GenerateOptions.builder()
                        .temperature(0.7)
                        .maxTokens(2000)
                        .build())
                .build();
        modelCache.put(cacheKey, model);
        log.info("注册 DashScope 模型: {}", modelName);
    }

    /**
     * 注册 OpenAI 兼容模型
     */
    public void registerOpenAIModel(String modelName, String baseUrl, Formatter formatter) {
        String cacheKey = "openai:" + modelName;
        Model model = OpenAIChatModel.builder()
                .apiKey(openaiApiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .stream(true)
                .formatter(formatter)
                .build();
        modelCache.put(cacheKey, model);
        log.info("注册 OpenAI 兼容模型: {} -> {}", modelName, baseUrl);
    }

    /**
     * 注册 Ollama 模型
     */
    public void registerOllamaModel(String modelName) {
        String cacheKey = "ollama:" + modelName;
        Model model = OllamaChatModel.builder()
                .modelName(modelName)
                .baseUrl(ollamaBaseUrl)
                .build();
        modelCache.put(cacheKey, model);
        log.info("注册 Ollama 模型: {}", modelName);
    }

    /**
     * 注册 Gemini 模型
     */
    public void registerGeminiModel(String modelName) {
        String cacheKey = "gemini:" + modelName;
        Model model = GeminiChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName(modelName)
                .streamEnabled(true)
                .formatter(new GeminiChatFormatter())
                .build();
        modelCache.put(cacheKey, model);
        log.info("注册 Gemini 模型: {}", modelName);
    }

    // ==================== 模型获取方法 ====================

    /**
     * 根据提供商和模型名获取模型客户端
     */
    public Model getModel(String provider, String modelName) {
        String cacheKey = provider + ":" + modelName;
        return modelCache.get(cacheKey);
    }

    /**
     * 获取所有已注册的模型
     */
    public Map<String, Model> getAllModels() {
        return new HashMap<>(modelCache);
    }

    /**
     * 获取指定提供商的模型
     */
    public Map<String, Model> getModelsByProvider(String provider) {
        Map<String, Model> result = new HashMap<>();
        modelCache.forEach((key, model) -> {
            if (key.startsWith(provider + ":")) {
                result.put(key, model);
            }
        });
        return result;
    }

    /**
     * 切换默认模型
     */
    public void switchDefaultModel(String provider, String modelName) {
        String cacheKey = provider + ":" + modelName;
        if (!modelCache.containsKey(cacheKey)) {
            throw new IllegalArgumentException("模型不存在: " + cacheKey);
        }
        log.info("切换默认模型: {} -> {}", provider, modelName);
    }

    /**
     * 移除模型
     */
    public void removeModel(String provider, String modelName) {
        String cacheKey = provider + ":" + modelName;
        modelCache.remove(cacheKey);
        log.info("移除模型: {}", cacheKey);
    }

    /**
     * 检查模型是否可用
     */
    public boolean isModelAvailable(String provider, String modelName) {
        return modelCache.containsKey(provider + ":" + modelName);
    }

    /**
     * 获取所有支持的提供商
     */
    public List<String> getSupportedProviders() {
        return List.of("dashscope", "openai", "ollama", "gemini");
    }
}
```

### 控制层实现

```java
// io.agentscope.tutorial.chapter06.controller.ModelCompareController
package io.agentscope.tutorial.chapter06.controller;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.tutorial.chapter06.entity.ChatConfig;
import io.agentscope.tutorial.chapter06.entity.ModelProvider;
import io.agentscope.tutorial.chapter06.repository.ChatConfigRepository;
import io.agentscope.tutorial.chapter06.repository.ModelProviderRepository;
import io.agentscope.tutorial.chapter06.service.ModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * 模型对比控制器
 * 提供模型配置、切换和对话功能
 */
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
@Slf4j
public class ModelCompareController {

    private final ModelService modelService;
    private final ModelProviderRepository providerRepository;
    private final ChatConfigRepository configRepository;

    // ==================== 模型管理接口 ====================

    /**
     * 获取所有支持的模型提供商
     */
    @GetMapping("/providers")
    public ResponseEntity<List<Map<String, Object>>> getProviders() {
        List<ModelProvider> providers = providerRepository.findByEnabledTrueOrderByPriorityAsc();
        List<Map<String, Object>> result = new ArrayList<>();

        for (ModelProvider provider : providers) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", provider.getId());
            map.put("name", provider.getName());
            map.put("displayName", provider.getDisplayName());
            map.put("enabled", provider.getEnabled());
            map.put("priority", provider.getPriority());

            // 获取该提供商下的所有模型
            Map<String, Model> models = modelService.getModelsByProvider(provider.getName());
            List<String> modelNames = new ArrayList<>(models.keySet());
            // 提取模型名（去掉 provider: 前缀）
            modelNames.replaceAll(s -> s.substring(s.indexOf(':') + 1));
            map.put("models", modelNames);

            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 获取指定提供商的所有模型
     */
    @GetMapping("/providers/{provider}/models")
    public ResponseEntity<List<String>> getModelsByProvider(@PathVariable String provider) {
        Map<String, Model> models = modelService.getModelsByProvider(provider);
        List<String> modelNames = new ArrayList<>(models.keySet());
        modelNames.replaceAll(s -> s.substring(s.indexOf(':') + 1));
        return ResponseEntity.ok(modelNames);
    }

    /**
     * 注册新模型
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerModel(
            @RequestBody Map<String, String> request) {
        String provider = request.get("provider");
        String modelName = request.get("modelName");

        try {
            switch (provider.toLowerCase()) {
                case "dashscope" -> modelService.registerDashScopeModel(modelName);
                case "ollama" -> modelService.registerOllamaModel(modelName);
                case "gemini" -> modelService.registerGeminiModel(modelName);
                default -> {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "不支持的提供商: " + provider
                    ));
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "模型注册成功",
                    "modelKey", provider + ":" + modelName
            ));
        } catch (Exception e) {
            log.error("注册模型失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "注册失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 移除模型
     */
    @DeleteMapping("/remove")
    public ResponseEntity<Map<String, Object>> removeModel(
            @RequestBody Map<String, String> request) {
        String provider = request.get("provider");
        String modelName = request.get("modelName");

        modelService.removeModel(provider, modelName);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "模型移除成功"
        ));
    }

    // ==================== 对话接口 ====================

    /**
     * 单模型对话
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody Map<String, Object> request) {
        String provider = (String) request.get("provider");
        String modelName = (String) request.get("modelName");
        String message = (String) request.get("message");
        Double temperature = request.containsKey("temperature")
                ? (Double) request.get("temperature") : 0.7;
        Integer maxTokens = request.containsKey("maxTokens")
                ? (Integer) request.get("maxTokens") : 2000;

        Model model = modelService.getModel(provider, modelName);
        if (model == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "模型不存在: " + provider + ":" + modelName
            ));
        }

        try {
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(message).build())
                    .build();

            GenerateOptions options = GenerateOptions.builder()
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .build();

            // 执行对话
            ChatResponse response = model.stream(List.of(userMsg), null, options)
                    .blockLast();

            if (response != null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "model", modelName,
                        "provider", provider,
                        "response", response.getContent().getText(),
                        "usage", response.getUsage()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "未获取到响应"
                ));
            }
        } catch (Exception e) {
            log.error("对话请求失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "请求失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 流式对话
     */
    @PostMapping("/chat/stream")
    public Flux<Map<String, Object>> streamChat(
            @RequestBody Map<String, Object> request) {
        String provider = (String) request.get("provider");
        String modelName = (String) request.get("modelName");
        String message = (String) request.get("message");
        Double temperature = request.containsKey("temperature")
                ? (Double) request.get("temperature") : 0.7;

        Model model = modelService.getModel(provider, modelName);
        if (model == null) {
            return Flux.just(Map.of(
                    "error", true,
                    "message", "模型不存在"
            ));
        }

        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(message).build())
                .build();

        GenerateOptions options = GenerateOptions.builder()
                .temperature(temperature)
                .build();

        return model.stream(List.of(userMsg), null, options)
                .map(response -> Map.of(
                        "content", response.getContent().getText(),
                        "done", response.isDone(),
                        "usage", response.getUsage()
                ));
    }

    // ==================== 模型对比接口 ====================

    /**
     * 多模型对比（同一问题发给多个模型）
     */
    @PostMapping("/compare")
    public ResponseEntity<Map<String, Object>> compareModels(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> modelList = (List<Map<String, String>>) request.get("models");
        String message = (String) request.get("message");
        String systemPrompt = (String) request.getOrDefault("systemPrompt", "你是一个有用的AI助手。");

        List<Map<String, Object>> results = new ArrayList<>();

        for (Map<String, String> modelConfig : modelList) {
            String provider = modelConfig.get("provider");
            String modelName = modelConfig.get("modelName");

            Map<String, Object> result = new HashMap<>();
            result.put("provider", provider);
            result.put("modelName", modelName);

            try {
                long startTime = System.currentTimeMillis();

                Model model = modelService.getModel(provider, modelName);
                if (model == null) {
                    result.put("success", false);
                    result.put("error", "模型不存在");
                    results.add(result);
                    continue;
                }

                List<Msg> messages = List.of(
                        Msg.builder()
                                .role(MsgRole.SYSTEM)
                                .content(TextBlock.builder().text(systemPrompt).build())
                                .build(),
                        Msg.builder()
                                .role(MsgRole.USER)
                                .content(TextBlock.builder().text(message).build())
                                .build()
                );

                GenerateOptions options = GenerateOptions.builder()
                        .temperature(0.7)
                        .maxTokens(1000)
                        .build();

                ChatResponse response = model.stream(messages, null, options)
                        .blockLast();

                long duration = System.currentTimeMillis() - startTime;

                if (response != null) {
                    result.put("success", true);
                    result.put("response", response.getContent().getText());
                    result.put("duration", duration);
                    result.put("usage", response.getUsage());
                } else {
                    result.put("success", false);
                    result.put("error", "未获取到响应");
                }
            } catch (Exception e) {
                result.put("success", false);
                result.put("error", e.getMessage());
            }

            results.add(result);
        }

        return ResponseEntity.ok(Map.of(
                "question", message,
                "results", results
        ));
    }

    // ==================== 配置管理接口 ====================

    /**
     * 保存聊天配置
     */
    @PostMapping("/config/save")
    public ResponseEntity<Map<String, Object>> saveConfig(
            @RequestBody ChatConfig config) {
        try {
            if (config.getId() == null) {
                config.setCreateTime(java.time.LocalDateTime.now());
            }
            config.setUpdateTime(java.time.LocalDateTime.now());

            ChatConfig saved = configRepository.save(config);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", saved
            ));
        } catch (Exception e) {
            log.error("保存配置失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取所有聊天配置
     */
    @GetMapping("/config/list")
    public ResponseEntity<List<ChatConfig>> listConfigs() {
        return ResponseEntity.ok(configRepository.findAll());
    }

    /**
     * 获取默认配置
     */
    @GetMapping("/config/default")
    public ResponseEntity<Optional<ChatConfig>> getDefaultConfig() {
        return ResponseEntity.ok(configRepository.findByIsDefaultTrue());
    }

    /**
     * 设置默认配置
     */
    @PostMapping("/config/set-default/{id}")
    public ResponseEntity<Map<String, Object>> setDefaultConfig(@PathVariable Long id) {
        // 清除现有默认
        configRepository.findByIsDefaultTrue().ifPresent(config -> {
            config.setIsDefault(false);
            configRepository.save(config);
        });

        // 设置新默认
        configRepository.findById(id).ifPresent(config -> {
            config.setIsDefault(true);
            configRepository.save(config);
        });

        return ResponseEntity.ok(Map.of("success", true));
    }
}
```

### Spring Boot 启动类

```java
// io.agentscope.tutorial.chapter06.Chapter06Application
package io.agentscope.tutorial.chapter06;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentScope Java 教程 - 第六章：模型集成
 *
 * <p>本应用演示如何配置和使用多种模型客户端，
 * 支持 DashScope、OpenAI、Ollama、Gemini 等多种提供商。
 *
 * <p>访问地址：
 * <ul>
 *   <li>API 端点: http://localhost:8080/api/models</li>
 *   <li>H2 控制台: http://localhost:8080/h2-console</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@SpringBootApplication
public class Chapter06Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter06Application.class, args);
    }
}
```

### 数据初始化器（可选）

```java
// io.agentscope.tutorial.chapter06.config.DataInitializer
package io.agentscope.tutorial.chapter06.config;

import io.agentscope.tutorial.chapter06.entity.ChatConfig;
import io.agentscope.tutorial.chapter06.entity.ModelProvider;
import io.agentscope.tutorial.chapter06.repository.ChatConfigRepository;
import io.agentscope.tutorial.chapter06.repository.ModelProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器
 * 在应用启动时初始化默认的模型提供商配置
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ModelProviderRepository providerRepository;
    private final ChatConfigRepository configRepository;

    @Override
    public void run(String... args) {
        initializeProviders();
        initializeDefaultConfigs();
    }

    private void initializeProviders() {
        log.info("初始化模型提供商配置...");

        // DashScope
        if (!providerRepository.existsByName("dashscope")) {
            providerRepository.save(ModelProvider.builder()
                    .name("dashscope")
                    .displayName("阿里云百炼 (DashScope)")
                    .baseUrl("https://dashscope.aliyuncs.com/api/v1")
                    .enabled(true)
                    .priority(10)
                    .remark("通义千问系列模型，支持文本和视觉任务")
                    .build());
        }

        // OpenAI
        if (!providerRepository.existsByName("openai")) {
            providerRepository.save(ModelProvider.builder()
                    .name("openai")
                    .displayName("OpenAI")
                    .baseUrl("https://api.openai.com/v1")
                    .enabled(true)
                    .priority(20)
                    .remark("GPT 系列模型，强大的推理和生成能力")
                    .build());
        }

        // Ollama
        if (!providerRepository.existsByName("ollama")) {
            providerRepository.save(ModelProvider.builder()
                    .name("ollama")
                    .displayName("Ollama (本地)")
                    .baseUrl("http://localhost:11434")
                    .enabled(true)
                    .priority(30)
                    .remark("本地开源模型，隐私保护")
                    .build());
        }

        // Gemini
        if (!providerRepository.existsByName("gemini")) {
            providerRepository.save(ModelProvider.builder()
                    .name("gemini")
                    .displayName("Google Gemini")
                    .baseUrl("https://generativelanguage.googleapis.com")
                    .enabled(true)
                    .priority(40)
                    .remark("Google 多模态模型，支持图像和视频理解")
                    .build());
        }

        log.info("模型提供商初始化完成");
    }

    private void initializeDefaultConfigs() {
        log.info("初始化默认聊天配置...");

        // DashScope 默认配置
        if (configRepository.findByName("DashScope 默认配置").isEmpty()) {
            configRepository.save(ChatConfig.builder()
                    .name("DashScope 默认配置")
                    .providerType("dashscope")
                    .modelName("qwen-plus")
                    .temperature(0.7)
                    .maxTokens(2000)
                    .systemPrompt("你是一个有帮助的AI助手。请用中文回答问题。")
                    .streamEnabled(true)
                    .isDefault(true)
                    .build());
        }

        // OpenAI 默认配置
        if (configRepository.findByName("OpenAI 默认配置").isEmpty()) {
            configRepository.save(ChatConfig.builder()
                    .name("OpenAI 默认配置")
                    .providerType("openai")
                    .modelName("gpt-4o")
                    .temperature(0.7)
                    .maxTokens(2000)
                    .systemPrompt("You are a helpful AI assistant. Please answer in the same language as the user.")
                    .streamEnabled(true)
                    .isDefault(false)
                    .build());
        }

        log.info("默认聊天配置初始化完成");
    }
}
```

### API 使用示例

#### 1. 获取所有模型提供商

```bash
curl http://localhost:8080/api/models/providers
```

响应：
```json
[
  {
    "id": 1,
    "name": "dashscope",
    "displayName": "阿里云百炼 (DashScope)",
    "enabled": true,
    "priority": 10,
    "models": ["qwen-plus", "qwen-max", "qwen-turbo"]
  },
  ...
]
```

#### 2. 单模型对话

```bash
curl -X POST http://localhost:8080/api/models/chat \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "dashscope",
    "modelName": "qwen-plus",
    "message": "请介绍一下你自己",
    "temperature": 0.7
  }'
```

#### 3. 多模型对比

```bash
curl -X POST http://localhost:8080/api/models/compare \
  -H "Content-Type: application/json" \
  -d '{
    "models": [
      {"provider": "dashscope", "modelName": "qwen-plus"},
      {"provider": "openai", "modelName": "gpt-4o"},
      {"provider": "gemini", "modelName": "gemini-2.0-flash"}
    ],
    "message": "解释什么是大语言模型",
    "systemPrompt": "你是一个技术专家，请简洁明了地回答。"
  }'
```

响应：
```json
{
  "question": "解释什么是大语言模型",
  "results": [
    {
      "provider": "dashscope",
      "modelName": "qwen-plus",
      "success": true,
      "response": "大语言模型（Large Language Model, LLM）是一种...",
      "duration": 1234,
      "usage": {"promptTokens": 50, "completionTokens": 200, "totalTokens": 250}
    },
    {
      "provider": "openai",
      "modelName": "gpt-4o",
      "success": true,
      "response": "A large language model (LLM) is a type of...",
      "duration": 1500,
      "usage": {"promptTokens": 45, "completionTokens": 180, "totalTokens": 225}
    }
  ]
}
```

#### 4. 注册新模型

```bash
curl -X POST http://localhost:8080/api/models/register \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "dashscope",
    "modelName": "qwen-coder-plus"
  }'
```

#### 5. 保存聊天配置

```bash
curl -X POST http://localhost:8080/api/models/config/save \
  -H "Content-Type: application/json" \
  -d '{
    "name": "我的自定义配置",
    "providerType": "dashscope",
    "modelName": "qwen-max",
    "temperature": 0.5,
    "maxTokens": 3000,
    "systemPrompt": "你是一个专业的数据分析助手。",
    "streamEnabled": true,
    "isDefault": false
  }'
```

---

## 6.8 本章小结

本章介绍了 AgentScope Java 的模型集成架构：

### 核心要点

1. **Model 接口**：统一的模型接口，支持流式和非流式调用
2. **Formatter 机制**：可插拔的格式化器，处理不同提供商的格式转换
3. **多提供商支持**：DashScope、OpenAI、Ollama、Gemini 等开箱即用
4. **GenerateOptions**：统一的生成选项配置

### 关键类总结

| 类名 | 用途 |
|------|------|
| `Model` | 模型接口，定义聊天能力 |
| `DashScopeChatModel` | 阿里云百炼模型 |
| `OpenAIChatModel` | OpenAI 兼容模型 |
| `OllamaChatModel` | 本地 Ollama 模型 |
| `GeminiChatModel` | Google Gemini 模型 |
| `Formatter` | 消息格式化接口 |
| `GenerateOptions` | 生成选项配置 |

### 下一步

- 尝试不同的模型提供商
- 配置自定义 Formatter
- 实现模型对比和选择策略
- 探索工具调用和 Agent 配合

### 相关资源

- [DashScope 文档](https://help.aliyun.com/zh/dashscope/)
- [OpenAI API 文档](https://platform.openai.com/docs)
- [Ollama 官方文档](https://github.com/ollama/ollama)
- [Gemini API 文档](https://ai.google.dev/docs)