# 第五章：工具系统

> 本章讲解 AgentScope Java 框架的工具系统，包括内置工具、自定义工具开发、工具注册与调用机制、参数验证，以及完整的实战案例。

## 5.1 内置工具概览

AgentScope Java 提供了一系列开箱即用的内置工具，涵盖文件操作、代码执行、多模态等常见场景。

### 5.1.1 文件操作工具

框架内置了 `ReadFileTool` 和 `WriteFileTool`，支持安全的文件读写操作。

**ReadFileTool（文件读取工具）**

```java
// 位于：io.agentscope.core.tool.file.ReadFileTool
// 主要功能：
// - view_text_file: 按行范围读取文件内容，支持负索引（从尾部计算）
// - list_directory: 列出目录内容（文件与子目录分类展示）

// 无限制模式（可读写任意路径）
ReadFileTool readTool = new ReadFileTool();

// 安全限制模式（仅允许访问指定目录）
ReadFileTool readTool = new ReadFileTool("/data/workspace");

// 调用示例
Mono<ToolResultBlock> result = readTool.viewTextFile("path/to/file.txt", "1,100");
Mono<ToolResultBlock> result = readTool.viewTextFile("path/to/file.txt", "-100,-1"); // 最后100行
Mono<ToolResultBlock> result = readTool.listDirectory("/data/workspace");
```

**WriteFileTool（文件写入工具）**

```java
// 位于：io.agentscope.core.tool.file.WriteFileTool
// 主要功能：
// - write_file: 向指定路径写入文本内容
// - append_file: 向文件追加内容

WriteFileTool writeTool = new WriteFileTool("/data/workspace"); // 限制工作目录
Mono<ToolResultBlock> result = writeTool.writeFile("output.txt", "Hello World");
```

文件工具的核心安全机制由 `FileToolUtils` 提供：
- 路径遍历攻击防护（禁止 `../` 逃逸）
- 基础目录限制
- 文件类型与大小检查

### 5.1.2 代码执行工具

**ShellCommandTool（Shell 命令工具）**

```java
// 位于：io.agentscope.core.tool.coding.ShellCommandTool
// 支持跨平台命令验证：
// - UnixCommandValidator: Linux/macOS 命令白名单
// - CommandValidator: Windows 命令验证

ShellCommandTool shellTool = ShellCommandTool.builder()
    .baseDir("/data/workspace")
    .allowedCommands(Set.of("ls", "cat", "grep", "find"))
    .timeout(Duration.ofSeconds(30))
    .build();
```

### 5.1.3 多模态工具

```java
// DashScope 多模态工具
import io.agentscope.core.tool.multimodal.DashScopeMultiModalTool;

// OpenAI 多模态工具
import io.agentscope.core.tool.multimodal.OpenAIMultiModalTool;
```

### 5.1.4 MCP 工具（Model Context Protocol）

MCP 工具用于连接外部 MCP 服务器，访问远程提供的工具集。

```java
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;

// 构建 MCP 客户端
McpClientWrapper mcpClient = McpClientBuilder.builder()
    .name("filesystem-mcp")
    .url("http://localhost:8080/mcp")
    .build()
    .sync(); // 或 .async()

// 注册 MCP 客户端的所有工具
toolkit.registerMcpClient(mcpClient);

// 或者使用 Builder API 进行精细控制
toolkit.registration()
    .mcpClient(mcpClient)
    .enableTools(List.of("read_file", "write_file"))
    .disableTools(List.of("delete_file"))
    .group("mcp_tools")
    .presetParameters(Map.of(
        "read_file", Map.of("baseDir", "/data/workspace")
    ))
    .apply();
```

### 5.1.5 子代理工具（SubAgent as Tool）

将另一个 Agent 作为工具调用，实现复杂任务的分解与协作。

```java
import io.agentscope.core.tool.subagent.SubAgentConfig;
import io.agentscope.core.tool.subagent.SubAgentProvider;

toolkit.registration()
    .subAgent(
        () -> ReActAgent.builder()
            .name("ResearchAgent")
            .model(model)
            .toolkit(specializedToolkit)
            .build(),
        SubAgentConfig.builder()
            .toolName("do_research")
            .description("深入研究给定主题并返回报告")
            .session(new JsonSession(Path.of("sessions/research")))
            .build()
    )
    .group("research")
    .apply();
```

## 5.2 自定义工具开发规范

AgentScope Java 支持两种自定义工具的方式：**注解方式**（推荐）和**接口实现方式**。

### 5.2.1 注解方式开发工具

使用 `@Tool` 注解标记方法，用 `@ToolParam` 注解标记参数。

```java
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

public class MyTools {

    @Tool(
        name = "my_tool",
        description = "工具的详细描述，帮助模型理解何时使用"
    )
    public String myToolMethod(
        @ToolParam(
            name = "param_name",      // 参数名（必填）
            description = "参数描述", // 参数说明
            required = true           // 是否必填，默认 true
        ) String paramName
    ) {
        // 工具逻辑
        return "result";
    }
}
```

**注解详解：**

| 注解 | 属性 | 说明 |
|------|------|------|
| `@Tool` | `name` | 工具名称，建议 snake_case |
| | `description` | 工具功能描述，影响模型决策 |
| | `strict` | 是否启用严格模式（默认 false） |
| | `converter` | 自定义结果转换器（默认使用 DefaultToolResultConverter） |
| `@ToolParam` | `name` | **必填**，参数名称 |
| | `description` | 参数说明 |
| | `required` | 是否必填（默认 true） |

**工具注册：**

```java
Toolkit toolkit = new Toolkit();

// 注册工具对象（自动扫描所有 @Tool 方法）
toolkit.registerTool(new MyTools());

// 注册到指定分组
toolkit.createToolGroup("my_group", "我的工具组", true);
toolkit.registration().tool(new MyTools()).group("my_group").apply();
```

### 5.2.2 实现 AgentTool 接口

对于需要更复杂逻辑的工具，直接实现 `AgentTool` 接口。

```java
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolCallParam;
import io.agentscope.core.message.ToolResultBlock;
import reactor.core.publisher.Mono;

public class CustomAgentTool implements AgentTool {

    @Override
    public String getName() {
        return "custom_tool";
    }

    @Override
    public String getDescription() {
        return "自定义工具描述";
    }

    @Override
    public Map<String, Object> getParameters() {
        // 返回 JSON Schema 格式的参数定义
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "input", Map.of(
                    "type", "string",
                    "description", "输入参数"
                )
            ),
            "required", List.of("input")
        );
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        // 获取输入参数
        Map<String, Object> input = param.getInput();
        String inputValue = (String) input.get("input");

        // 执行逻辑
        String result = doSomething(inputValue);

        // 返回结果
        return Mono.just(ToolResultBlock.text(result));
    }
}

// 注册自定义工具
toolkit.registerAgentTool(new CustomAgentTool());
```

### 5.2.3 异步工具开发

工具方法返回 `Mono<T>` 时自动识别为异步执行。

```java
public class AsyncTools {

    @Tool(name = "async_fetch", description = "异步获取数据")
    public Mono<String> asyncFetch(
        @ToolParam(name = "url", description = "数据源URL") String url
    ) {
        return WebClient.create()
            .get()
            .uri(url)
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> "Fetched: " + response)
            .onErrorResume(e -> Mono.just("Error: " + e.getMessage()));
    }
}
```

## 5.3 工具注册与调用机制

### 5.3.1 Toolkit 核心组件

```
Toolkit
├── ToolRegistry         # 工具注册表（名称 -> AgentTool）
├── ToolGroupManager     # 工具分组管理
├── ToolSchemaProvider    # 工具 Schema 生成
├── ToolExecutor          # 工具执行器
├── McpClientManager      # MCP 客户端管理
└── MetaToolFactory       # 元工具工厂（reset_equipped_tools）
```

### 5.3.2 工具注册方式

**方式一：直接注册**

```java
Toolkit toolkit = new Toolkit();

// 注册工具对象
toolkit.registerTool(new MyTools());

// 注册 AgentTool 实例
toolkit.registerAgentTool(new CustomAgentTool());

// 注册外部 Schema（用于外部执行工具）
toolkit.registerSchema(ToolSchema.builder()
    .name("external_tool")
    .description("外部工具")
    .parameters(Map.of(
        "type", "object",
        "properties", Map.of("input", Map.of("type", "string")),
        "required", List.of("input")
    ))
    .build());
```

**方式二：使用 Builder API（推荐）**

```java
toolkit.registration()
    .tool(new MyTools())              // 工具对象
    .group("my_group")                // 指定分组
    .presetParameters(Map.of(         // 预设参数（对模型隐藏）
        "my_tool", Map.of("apiKey", "secret")
    ))
    .extendedModel(myExtendedModel)  // 扩展 Schema
    .apply();
```

### 5.3.3 工具分组管理

```java
// 创建工具分组（默认不激活）
toolkit.createToolGroup("file_ops", "文件操作工具", false);
toolkit.createToolGroup("calc_ops", "计算工具", false);

// 将工具注册到分组
toolkit.registration().tool(new FileTools()).group("file_ops").apply();
toolkit.registration().tool(new CalcTools()).group("calc_ops").apply();

// 激活分组
toolkit.updateToolGroups(List.of("file_ops", "calc_ops"), true);

// 查看活跃分组
List<String> activeGroups = toolkit.getActiveGroups();
```

### 5.3.4 元工具（Meta Tool）

启用元工具后，Agent 可以动态管理工具分组。

```java
ReActAgent agent = ReActAgent.builder()
    .name("SmartAgent")
    .model(model)
    .toolkit(toolkit)
    .enableMetaTool(true)  // 启用 reset_equipped_tools 元工具
    .build();
```

Agent 运行时可以调用 `reset_equipped_tools` 来激活需要的工具分组。

### 5.3.5 工具调用流程

```
Agent 推理
    │
    ▼
模型返回 ToolUseBlock
    │
    ▼
ToolExecutor.execute()
    │
    ├──► 验证参数（ToolValidator）
    │
    ├──► 参数合并（预设参数 + 输入参数）
    │
    └──► 执行工具
            │
            ├──► 同步工具：直接调用方法
            │
            └──► 异步工具：Mono 处理
    │
    ▼
ToolResultBlock 结果返回
    │
    ▼
Agent 继续推理
```

### 5.3.6 预设参数（Preset Parameters）

预设参数在工具注册时注入，对模型隐藏，不出现在 JSON Schema 中。

```java
// 定义预设参数
Map<String, Map<String, Object>> presetParams = Map.of(
    "fetch_data", Map.of(
        "apiKey", "sk-xxxxx",
        "baseUrl", "https://api.example.com"
    )
);

toolkit.registration()
    .tool(new DataFetchTools())
    .presetParameters(presetParams)
    .apply();

// 调用时只需传入动态参数
// 框架自动合并: { apiKey: "...", baseUrl: "...", query: "xxx" }
```

## 5.4 工具参数验证

### 5.4.1 Schema 验证

AgentScope 使用 `networknt-schema` 库进行 JSON Schema 验证。

```java
import io.agentscope.core.tool.ToolValidator;

// 获取工具的 Schema
Map<String, Object> schema = tool.getParameters();

// 验证输入参数
String inputJson = "{\"param1\": \"value1\"}";
String error = ToolValidator.validateInput(inputJson, schema);

if (error != null) {
    // 验证失败
    System.out.println("Validation failed: " + error);
} else {
    // 验证通过
}
```

**验证内容：**
- 必填字段检查
- 类型验证（string, number, integer, boolean, array, object）
- 枚举值验证
- 数值范围（min/max）
- 字符串长度（minLength/maxLength）
- 正则表达式（pattern）
- 嵌套对象与数组验证

### 5.4.2 自定义验证示例

```java
public class ValidatedTools {

    @Tool(name = "search_products", description = "搜索产品")
    public String searchProducts(
        @ToolParam(
            name = "keyword",
            description = "搜索关键词（2-50字符）"
        ) String keyword,
        @ToolParam(
            name = "category",
            description = "产品类别",
            required = false
        ) String category,
        @ToolParam(
            name = "max_results",
            description = "最大返回数量（1-100）",
            required = false
        ) Integer maxResults
    ) {
        // 参数校验
        if (keyword == null || keyword.length() < 2 || keyword.length() > 50) {
            return "Error: keyword must be between 2 and 50 characters";
        }
        if (maxResults != null && (maxResults < 1 || maxResults > 100)) {
            return "Error: max_results must be between 1 and 100";
        }

        // 执行业务逻辑
        return search(keyword, category, maxResults);
    }
}
```

### 5.4.3 参数类型自动推断

框架根据方法参数类型自动生成 Schema，支持以下类型：

| Java 类型 | JSON Schema 类型 |
|-----------|------------------|
| String | string |
| Integer / int | integer |
| Long / long | integer |
| Double / double | number |
| Boolean / boolean | boolean |
| List<T> | array |
| Map<K,V> / Object | object |
| 自定义类 | object（含 $defs） |
| 枚举 | string + enum |

```java
@Tool(name = "complex_tool", description = "复杂参数示例")
public String complexTool(
    @ToolParam(name = "name") String name,
    @ToolParam(name = "age") Integer age,
    @ToolParam(name = "tags") List<String> tags,
    @ToolParam(name = "metadata") Map<String, Object> metadata,
    @ToolParam(name = "config") Config config  // 自定义类自动生成嵌套 Schema
) {
    // ...
}
```

## 5.5 【案例】完整工具系统实现

本案例实现一个功能完整的工具系统，包含：

1. 内置工具的使用
2. 自定义工具开发（计算器、天气查询）
3. 工具分组管理
4. 参数验证
5. 与 Spring Boot 3 的集成

### 5.5.1 项目结构

```
chapter05-tools/
├── pom.xml
└── src/main/java/io/agentscope/tutorial/chapter05/
    ├── Chapter05Application.java
    ├── config/
    │   └── AgentConfig.java
    ├── tools/
    │   ├── CalculatorTool.java
    │   ├── WeatherTool.java
    │   └── DateTimeTool.java
    ├── service/
    │   └── ToolRegistrationService.java
    └── controller/
        └── AgentController.java
```

### 5.5.2 Maven 依赖（pom.xml）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-parent</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>chapter05-tools</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>Chapter 05 - Tools System</name>
    <description>AgentScope Java Tutorial - Chapter 05 Tools System</description>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- AgentScope Core -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-core</artifactId>
            <version>1.0.0</version>
        </dependency>

        <!-- Spring Boot 3 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>3.2.5</version>
        </dependency>

        <!-- Lombok（可选，简化代码） -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.32</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
        </plugins>
    </build>
</project>
```

### 5.5.3 计算器工具（CalculatorTool.java）

```java
package io.agentscope.tutorial.chapter05.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.ToolValidator;
import java.util.List;
import java.util.Map;

/**
 * 计算器工具集
 *
 * <p>提供基础数学运算、科学计算和表达式求值功能。
 * 所有方法均支持参数验证，错误时返回友好的错误信息。
 *
 * <p>工具名称使用 snake_case 规范，便于模型理解。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
public class CalculatorTool {

    // ==================== 基础运算 ====================

    /**
     * 加法运算
     *
     * @param a 第一个数
     * @param b 第二个数
     * @return 加法结果字符串
     */
    @Tool(
        name = "add",
        description = "计算两个数的加法。适用于简单的数值相加场景。"
    )
    public String add(
        @ToolParam(name = "a", description = "第一个加数", required = true) Double a,
        @ToolParam(name = "b", description = "第二个加数", required = true) Double b
    ) {
        validateNotNull(a, "a");
        validateNotNull(b, "b");
        return String.format("%.2f + %.2f = %.2f", a, b, a + b);
    }

    /**
     * 减法运算
     *
     * @param a 被减数
     * @param b 减数
     * @return 减法结果字符串
     */
    @Tool(
        name = "subtract",
        description = "计算两个数的减法"
    )
    public String subtract(
        @ToolParam(name = "a", description = "被减数", required = true) Double a,
        @ToolParam(name = "b", description = "减数", required = true) Double b
    ) {
        validateNotNull(a, "a");
        validateNotNull(b, "b");
        return String.format("%.2f - %.2f = %.2f", a, b, a - b);
    }

    /**
     * 乘法运算
     *
     * @param a 第一个因数
     * @param b 第二个因数
     * @return 乘法结果字符串
     */
    @Tool(
        name = "multiply",
        description = "计算两个数的乘法"
    )
    public String multiply(
        @ToolParam(name = "a", description = "第一个因数", required = true) Double a,
        @ToolParam(name = "b", description = "第二个因数", required = true) Double b
    ) {
        validateNotNull(a, "a");
        validateNotNull(b, "b");
        return String.format("%.2f * %.2f = %.2f", a, b, a * b);
    }

    /**
     * 除法运算
     *
     * @param a 被除数
     * @param b 除数（不能为零）
     * @return 除法结果字符串
     */
    @Tool(
        name = "divide",
        description = "计算两个数的除法。注意：除数不能为零"
    )
    public String divide(
        @ToolParam(name = "a", description = "被除数", required = true) Double a,
        @ToolParam(name = "b", description = "除数（不能为零）", required = true) Double b
    ) {
        validateNotNull(a, "a");
        validateNotNull(b, "b");
        if (b == 0) {
            return "Error: 除数不能为零";
        }
        return String.format("%.2f / %.2f = %.2f", a, b, a / b);
    }

    // ==================== 科学计算 ====================

    /**
     * 计算阶乘
     *
     * @param n 非负整数（最大支持 20! 以避免溢出）
     * @return 阶乘结果
     */
    @Tool(
        name = "factorial",
        description = "计算非负整数的阶乘。最大支持 20! 以避免整数溢出。"
    )
    public String factorial(
        @ToolParam(name = "n", description = "非负整数（0-20）", required = true) Integer n
    ) {
        validateNotNull(n, "n");

        if (n < 0) {
            return "Error: 阶乘不支持负数";
        }
        if (n > 20) {
            return "Error: 数字太大（最大支持 20!）";
        }

        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return String.format("%d! = %d", n, result);
    }

    /**
     * 判断质数
     *
     * @param n 待检测的正整数
     * @return 质数判断结果
     */
    @Tool(
        name = "is_prime",
        description = "判断一个正整数是否为质数。质数定义为大于1的自然数，除了1和它本身外不能被其他自然数整除。"
    )
    public String isPrime(
        @ToolParam(name = "n", description = "待检测的正整数（>= 2）", required = true) Integer n
    ) {
        validateNotNull(n, "n");

        if (n < 2) {
            return n + " 不是质数（质数必须 >= 2）";
        }

        for (int i = 2; i <= Math.sqrt(n); i++) {
            if (n % i == 0) {
                return n + " 不是质数（可以被 " + i + " 整除）";
            }
        }
        return n + " 是质数";
    }

    /**
     * 计算平方根
     *
     * @param x 被开方数（非负数）
     * @return 平方根结果
     */
    @Tool(
        name = "sqrt",
        description = "计算非负数的平方根"
    )
    public String sqrt(
        @ToolParam(name = "x", description = "被开方数（必须 >= 0）", required = true) Double x
    ) {
        validateNotNull(x, "x");

        if (x < 0) {
            return "Error: 平方根不支持负数";
        }
        return String.format("sqrt(%.2f) = %.4f", x, Math.sqrt(x));
    }

    /**
     * 计算幂运算
     *
     * @param base 底数
     * @param exponent 指数
     * @return 幂运算结果
     */
    @Tool(
        name = "power",
        description = "计算底数的指数次幂。如 2^10 = 1024"
    )
    public String power(
        @ToolParam(name = "base", description = "底数", required = true) Double base,
        @ToolParam(name = "exponent", description = "指数", required = true) Double exponent
    ) {
        validateNotNull(base, "base");
        validateNotNull(exponent, "exponent");

        double result = Math.pow(base, exponent);
        return String.format("%.2f ^ %.2f = %.4f", base, exponent, result);
    }

    // ==================== 统计运算 ====================

    /**
     * 计算平均值
     *
     * @param numbers 数字列表（JSON 数组格式，如 "[1, 2, 3, 4, 5]"）
     * @return 平均值
     */
    @Tool(
        name = "average",
        description = "计算一组数字的平均值。输入格式为 JSON 数组字符串，如 '[1, 2, 3, 4, 5]'"
    )
    public String average(
        @ToolParam(name = "numbers", description = "数字列表（JSON数组格式）", required = true) String numbers
    ) {
        try {
            // 解析 JSON 数组
            numbers = numbers.trim();
            if (numbers.startsWith("[")) {
                numbers = numbers.substring(1, numbers.length() - 1);
            }
            String[] parts = numbers.split(",");
            double sum = 0;
            int count = 0;
            for (String part : parts) {
                sum += Double.parseDouble(part.trim());
                count++;
            }
            if (count == 0) {
                return "Error: 数字列表为空";
            }
            return String.format("平均值 = %.2f（共 %d 个数字）", sum / count, count);
        } catch (Exception e) {
            return "Error: 无效的数字列表格式。请使用 JSON 数组格式，如 '[1, 2, 3, 4, 5]'";
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 验证参数是否为空
     */
    private void validateNotNull(Object value, String paramName) {
        if (value == null) {
            throw new IllegalArgumentException("参数 '" + paramName + "' 不能为空");
        }
    }
}
```

### 5.5.4 天气查询工具（WeatherTool.java）

```java
package io.agentscope.tutorial.chapter05.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 天气查询工具
 *
 * <p>模拟天气查询服务，支持全球主要城市的天气查询。
 * 在实际应用中，可替换为真实天气 API（如 OpenWeatherMap、中国气象局 API 等）。
 *
 * <p>注意：这是一个模拟实现，返回的数据是虚构的，仅用于演示目的。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
public class WeatherTool {

    // 模拟的天气数据（实际应用中替换为 API 调用）
    private static final Map<String, WeatherInfo> WEATHER_DB = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();

    static {
        // 初始化模拟天气数据
        WEATHER_DB.put("北京", new WeatherInfo("北京", 15, "晴", "优", "北风 3-4级", 45));
        WEATHER_DB.put("上海", new WeatherInfo("上海", 18, "多云", "良", "东风 2-3级", 60));
        WEATHER_DB.put("广州", new WeatherInfo("广州", 25, "晴", "优", "南风 2级", 70));
        WEATHER_DB.put("深圳", new WeatherInfo("深圳", 26, "晴", "优", "南风 1-2级", 75));
        WEATHER_DB.put("杭州", new WeatherInfo("杭州", 16, "阴", "良", "东北风 2级", 65));
        WEATHER_DB.put("成都", new WeatherInfo("成都", 14, "小雨", "良", "北风 1-2级", 80));
        WEATHER_DB.put("武汉", new WeatherInfo("武汉", 17, "多云", "良", "东风 3级", 55));
        WEATHER_DB.put("西安", new WeatherInfo("西安", 12, "晴", "优", "西风 2级", 40));
        WEATHER_DB.put("南京", new WeatherInfo("南京", 15, "阴", "良", "东风 2-3级", 60));
        WEATHER_DB.put("重庆", new WeatherInfo("重庆", 20, "晴", "良", "西风 1级", 50));
        WEATHER_DB.put("东京", new WeatherInfo("东京", 20, "晴", "优", "东南风 2级", 50));
        WEATHER_DB.put("纽约", new WeatherInfo("纽约", 12, "阴", "中", "西北风 3级", 65));
        WEATHER_DB.put("伦敦", new WeatherInfo("伦敦", 10, "小雨", "良", "西风 4级", 85));
        WEATHER_DB.put("巴黎", new WeatherInfo("巴黎", 14, "多云", "良", "西南风 3级", 70));
        WEATHER_DB.put("新加坡", new WeatherInfo("新加坡", 31, "雷阵雨", "中", "东南风 2级", 90));
    }

    // ==================== 天气查询 ====================

    /**
     * 查询当前天气
     *
     * <p>返回指定城市的当前天气信息，包括温度、天气状况、空气质量等。
     *
     * @param city 城市名称（支持中英文）
     * @param unit 温度单位（celsius/fahrenheit），默认 celsius
     * @return 天气信息字符串
     */
    @Tool(
        name = "get_weather",
        description = "查询指定城市的当前天气信息。包括温度、天气状况、空气质量、风力等详细信息。"
    )
    public String getWeather(
        @ToolParam(
            name = "city",
            description = "城市名称（支持中英文，如'北京'、'Shanghai'）",
            required = true
        ) String city,
        @ToolParam(
            name = "unit",
            description = "温度单位：celsius（摄氏度）或 fahrenheit（华氏度）",
            required = false
        ) String unit
    ) {
        validateNotEmpty(city, "city");

        WeatherInfo weather = WEATHER_DB.get(city);
        if (weather == null) {
            // 尝试模糊匹配
            return findSimilarCity(city);
        }

        return formatWeatherResponse(weather, unit);
    }

    /**
     * 查询天气预报
     *
     * @param city 城市名称
     * @param days 预报天数（1-7），默认 3
     * @return 天气预报字符串
     */
    @Tool(
        name = "get_forecast",
        description = "查询指定城市未来几天的天气预报"
    )
    public String getForecast(
        @ToolParam(name = "city", description = "城市名称", required = true) String city,
        @ToolParam(
            name = "days",
            description = "预报天数（1-7天）",
            required = false
        ) Integer days
    ) {
        validateNotEmpty(city, "city");

        int forecastDays = (days != null && days >= 1 && days <= 7) ? days : 3;

        WeatherInfo baseWeather = WEATHER_DB.get(city);
        if (baseWeather == null) {
            return findSimilarCity(city);
        }

        StringBuilder result = new StringBuilder();
        result.append(String.format("📅 %s 天气预报（未来 %d 天）：\n\n", city, forecastDays));

        LocalDateTime now = LocalDateTime.now();
        String[] conditions = {"晴", "多云", "阴", "小雨", "雷阵雨"};

        for (int i = 0; i < forecastDays; i++) {
            LocalDateTime date = now.plusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("MM月dd日 E"));

            // 模拟数据波动
            int tempVariation = RANDOM.nextInt(6) - 3;
            double temp = baseWeather.temperature + tempVariation;
            String condition = conditions[RANDOM.nextInt(conditions.length)];

            result.append(String.format("【%s】%s 🌡️ %.0f~%.0f°C 💧 %d%%\n",
                dateStr,
                condition,
                temp - 3,
                temp + 3,
                baseWeather.humidity + RANDOM.nextInt(20) - 10));
        }

        return result.toString();
    }

    /**
     * 查询空气质量
     *
     * @param city 城市名称
     * @return 空气质量信息
     */
    @Tool(
        name = "get_air_quality",
        description = "查询指定城市的空气质量指数（AQI）和详细空气质量报告"
    )
    public String getAirQuality(
        @ToolParam(name = "city", description = "城市名称", required = true) String city
    ) {
        validateNotEmpty(city, "city");

        WeatherInfo weather = WEATHER_DB.get(city);
        if (weather == null) {
            return findSimilarCity(city);
        }

        StringBuilder result = new StringBuilder();
        result.append(String.format("🌫️ %s 空气质量报告\n\n", city));
        result.append(String.format("• 空气质量等级：%s\n", weather.airQuality));
        result.append(String.format("• 湿度：%d%%\n", weather.humidity));
        result.append(String.format("• 风力：%s\n", weather.wind));

        // AQI 等级说明
        String aqiDesc = switch (weather.airQuality) {
            case "优" -> "空气质量令人满意，基本无空气污染";
            case "良" -> "空气质量可接受，某些污染物可能对极少数敏感人群有影响";
            case "中" -> "易感人群症状有轻度加剧，健康人群出现刺激症状";
            case "差" -> "进一步加剧，易感人群症状可能加剧，对心脏、呼吸系统有影响";
            default -> "数据获取中";
        };
        result.append(String.format("• 说明：%s\n", aqiDesc));

        return result.toString();
    }

    /**
     * 获取穿衣建议
     *
     * <p>根据天气情况给出穿衣建议
     *
     * @param city 城市名称
     * @param unit 温度单位（可选）
     * @return 穿衣建议
     */
    @Tool(
        name = "get_dressing_advice",
        description = "根据当前天气情况给出穿衣建议，帮助用户合理搭配衣物"
    )
    public String getDressingAdvice(
        @ToolParam(name = "city", description = "城市名称", required = true) String city,
        @ToolParam(
            name = "unit",
            description = "温度单位（可选，默认摄氏度）",
            required = false
        ) String unit
    ) {
        validateNotEmpty(city, "city");

        WeatherInfo weather = WEATHER_DB.get(city);
        if (weather == null) {
            return findSimilarCity(city);
        }

        double temp = weather.temperature;
        if ("fahrenheit".equalsIgnoreCase(unit)) {
            temp = temp * 9 / 5 + 32;
        }

        StringBuilder advice = new StringBuilder();
        advice.append(String.format("👔 %s 穿衣建议（当前 %.0f°C）：\n\n", city, temp));

        // 根据温度给出建议
        if (temp < 0) {
            advice.append("🥶 极寒天气，建议穿：\n");
            advice.append("   • 羽绒服/厚棉服\n");
            advice.append("   • 保暖内衣、毛衣\n");
            advice.append("   • 棉裤、羽绒服内胆\n");
            advice.append("   • 围巾、手套、帽子、棉鞋\n");
        } else if (temp < 10) {
            advice.append("❄️ 寒冷天气，建议穿：\n");
            advice.append("   • 大衣或薄羽绒服\n");
            advice.append("   • 毛衣、保暖内衣\n");
            advice.append("   • 牛仔裤、保暖裤\n");
            advice.append("   • 围巾、手套\n");
        } else if (temp < 15) {
            advice.append("🌤️ 较冷天气，建议穿：\n");
            advice.append("   • 夹克、薄外套\n");
            advice.append("   • 薄毛衣、长袖\n");
            advice.append("   • 牛仔裤、休闲裤\n");
            advice.append("   • 可带围巾\n");
        } else if (temp < 20) {
            advice.append("🍃 凉爽天气，建议穿：\n");
            advice.append("   • 长袖衬衫、薄外套\n");
            advice.append("   • 牛仔裤、休闲裤\n");
            advice.append("   • 舒适的鞋子\n");
        } else if (temp < 25) {
            advice.append("☀️ 温暖天气，建议穿：\n");
            advice.append("   • T恤、短袖\n");
            advice.append("   • 薄裤子、裙子\n");
            advice.append("   • 运动鞋\n");
            advice.append("   • 注意防晒\n");
        } else if (temp < 30) {
            advice.append("🌡️ 较热天气，建议穿：\n");
            advice.append("   • 短袖、短裤\n");
            advice.append("   • 轻薄衣物\n");
            advice.append("   • 凉鞋、拖鞋\n");
            advice.append("   • 注意防暑降温\n");
        } else {
            advice.append("🔥 炎热天气，建议穿：\n");
            advice.append("   • 超薄短袖、短裤\n");
            advice.append("   • 透气性好的衣物\n");
            advice.append("   • 凉鞋、人字拖\n");
            advice.append("   • 强烈建议：多喝水，避免长时间户外活动\n");
        }

        // 根据天气状况补充建议
        if (weather.condition.contains("雨")) {
            advice.append("\n🌧️ 今日有雨，记得带伞！\n");
        }
        if (weather.condition.contains("雪")) {
            advice.append("\n❄️ 有雪，注意防滑！\n");
        }
        if (weather.wind.contains("大风")) {
            advice.append("\n💨 风力较大，注意防风！\n");
        }

        return advice.toString();
    }

    // ==================== 辅助方法 ====================

    private void validateNotEmpty(String value, String paramName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("参数 '" + paramName + "' 不能为空");
        }
    }

    private String formatWeatherResponse(WeatherInfo weather, String unit) {
        StringBuilder result = new StringBuilder();
        result.append(String.format("🌤️ %s 当前天气\n\n", weather.city));
        result.append(String.format("• 温度：%.0f°C", weather.temperature));
        if ("fahrenheit".equalsIgnoreCase(unit)) {
            double fahrenheit = weather.temperature * 9 / 5 + 32;
            result.append(String.format("（%.0f°F）", fahrenheit));
        }
        result.append("\n");
        result.append(String.format("• 天气：%s\n", weather.condition));
        result.append(String.format("• 空气质量：%s\n", weather.airQuality));
        result.append(String.format("• 湿度：%d%%\n", weather.humidity));
        result.append(String.format("• 风力：%s\n", weather.wind));

        // 更新时间戳
        String updateTime = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        result.append(String.format("\n🕐 更新时间：%s", updateTime));

        return result.toString();
    }

    private String findSimilarCity(String city) {
        // 尝试模糊匹配
        for (Map.Entry<String, WeatherInfo> entry : WEATHER_DB.entrySet()) {
            if (entry.getKey().toLowerCase().contains(city.toLowerCase()) ||
                city.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return formatWeatherResponse(entry.getValue(), null);
            }
        }
        return String.format("😢 抱歉，暂不支持查询 '%s' 的天气。\n支持的热门城市：北京、上海、广州、深圳、杭州、成都、武汉、西安、南京、重庆、东京、纽约、伦敦、巴黎、新加坡", city);
    }

    // ==================== 内部类：天气信息 ====================

    /**
     * 天气信息数据结构
     */
    private static class WeatherInfo {
        final String city;
        final double temperature;
        final String condition;
        final String airQuality;
        final String wind;
        final int humidity;

        WeatherInfo(String city, double temperature, String condition,
                    String airQuality, String wind, int humidity) {
            this.city = city;
            this.temperature = temperature;
            this.condition = condition;
            this.airQuality = airQuality;
            this.wind = wind;
            this.humidity = humidity;
        }
    }
}
```

### 5.5.5 日期时间工具（DateTimeTool.java）

```java
package io.agentscope.tutorial.chapter05.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * 日期时间工具集
 *
 * <p>提供日期时间查询、转换、计算等功能。
 * 支持多种时区和日期格式。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
public class DateTimeTool {

    // ==================== 时间查询 ====================

    /**
     * 获取当前日期时间
     *
     * @param timezone 时区（如 "Asia/Shanghai", "America/New_York", "Europe/London"）
     * @param format 日期格式（如 "yyyy-MM-dd HH:mm:ss"）
     * @return 格式化后的日期时间字符串
     */
    @Tool(
        name = "get_current_datetime",
        description = "获取指定时区的当前日期时间，支持自定义格式输出"
    )
    public String getCurrentDateTime(
        @ToolParam(
            name = "timezone",
            description = "时区标识（如 'Asia/Shanghai', 'America/New_York', 'Europe/London'）",
            required = false
        ) String timezone,
        @ToolParam(
            name = "format",
            description = "输出格式（如 'yyyy-MM-dd HH:mm:ss'），省略则使用默认格式",
            required = false
        ) String format
    ) {
        ZoneId zoneId = ZoneId.systemDefault();
        if (timezone != null && !timezone.isBlank()) {
            try {
                zoneId = ZoneId.of(timezone);
            } catch (Exception e) {
                return String.format("Error: 无效的时区 '%s'。请使用标准的 IANA 时区标识，如 'Asia/Shanghai'。", timezone);
            }
        }

        LocalDateTime now = LocalDateTime.now(zoneId);
        DateTimeFormatter formatter;
        if (format != null && !format.isBlank()) {
            try {
                formatter = DateTimeFormatter.ofPattern(format);
            } catch (Exception e) {
                return String.format("Error: 无效的日期格式 '%s'。", format);
            }
        } else {
            formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        }

        return String.format("📅 当前时间（%s）：%s", zoneId.getId(), now.format(formatter));
    }

    /**
     * 计算日期差
     *
     * @param startDate 起始日期（格式：yyyy-MM-dd）
     * @param endDate 结束日期（格式：yyyy-MM-dd）
     * @param unit 计算单位（days/weeks/months/years）
     * @return 日期差结果
     */
    @Tool(
        name = "date_diff",
        description = "计算两个日期之间的差值，支持按天、周、月、年计算"
    )
    public String dateDiff(
        @ToolParam(name = "start_date", description = "起始日期（格式：yyyy-MM-dd）", required = true) String startDate,
        @ToolParam(name = "end_date", description = "结束日期（格式：yyyy-MM-dd）", required = true) String endDate,
        @ToolParam(
            name = "unit",
            description = "计算单位：days（天）/ weeks（周）/ months（月）/ years（年）",
            required = false
        ) String unit
    ) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            long days = ChronoUnit.DAYS.between(start, end);

            String result = String.format("📊 日期差计算\n");
            result += String.format("• 起始日期：%s\n", startDate);
            result += String.format("• 结束日期：%s\n", endDate);
            result += String.format("• 相差天数：%d 天\n", days);

            if (unit == null || unit.isBlank() || "days".equalsIgnoreCase(unit)) {
                return result;
            }

            switch (unit.toLowerCase()) {
                case "weeks" -> result += String.format("• 相差周数：%.1f 周\n", days / 7.0);
                case "months" -> result += String.format("• 相差月数：%.1f 月\n", days / 30.0);
                case "years" -> result += String.format("• 相差年数：%.2f 年\n", days / 365.0);
                default -> result += "Error: 无效的单位 '" + unit + "'。请使用 days/weeks/months/years。";
            }

            return result;
        } catch (Exception e) {
            return String.format("Error: 日期格式错误，请使用 yyyy-MM-dd 格式，如 '2024-05-20'。详细：%s", e.getMessage());
        }
    }

    /**
     * 日期计算
     *
     * @param date 基准日期（格式：yyyy-MM-dd）
     * @param amount 加减数量（正数加，负数减）
     * @param unit 单位（days/weeks/months/years）
     * @return 计算后的日期
     */
    @Tool(
        name = "date_add",
        description = "对指定日期进行加/减运算，得到新的日期"
    )
    public String dateAdd(
        @ToolParam(name = "date", description = "基准日期（格式：yyyy-MM-dd）", required = true) String date,
        @ToolParam(name = "amount", description = "加/减的数量（正数加，负数减）", required = true) Integer amount,
        @ToolParam(name = "unit", description = "单位：days / weeks / months / years", required = true) String unit
    ) {
        try {
            LocalDate baseDate = LocalDate.parse(date);
            LocalDate resultDate;

            switch (unit.toLowerCase()) {
                case "days" -> resultDate = baseDate.plusDays(amount);
                case "weeks" -> resultDate = baseDate.plusWeeks(amount);
                case "months" -> resultDate = baseDate.plusMonths(amount);
                case "years" -> resultDate = baseDate.plusYears(amount);
                default -> {
                    return String.format("Error: 无效的单位 '%s'。请使用 days/weeks/months/years。", unit);
                }
            }

            return String.format("📅 日期计算\n• 基准日期：%s\n• 运算：%s %s\n• 结果：%s",
                date,
                (amount >= 0 ? "+" : "") + amount,
                unit,
                resultDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        } catch (Exception e) {
            return String.format("Error: %s", e.getMessage());
        }
    }

    /**
     * 获取星期几
     *
     * @param date 日期（格式：yyyy-MM-dd）
     * @return 星期信息
     */
    @Tool(
        name = "get_weekday",
        description = "查询指定日期是星期几"
    )
    public String getWeekday(
        @ToolParam(name = "date", description = "日期（格式：yyyy-MM-dd）", required = true) String date
    ) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            DayOfWeek dayOfWeek = localDate.getDayOfWeek();
            String weekday = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE);
            int dayNumber = dayOfWeek.getValue(); // 1=Monday, 7=Sunday

            StringBuilder result = new StringBuilder();
            result.append(String.format("📅 %s 是 %s\n", date, weekday));

            // 判断是否是周末
            if (dayNumber == 6 || dayNumber == 7) {
                result.append("🎉 今天是周末！\n");
            } else if (dayNumber == 1) {
                result.append("📌 今天是周一（周一综合征？保持好心情！）\n");
            }

            return result.toString();
        } catch (Exception e) {
            return String.format("Error: 日期格式错误，请使用 yyyy-MM-dd 格式。%s", e.getMessage());
        }
    }

    /**
     * 格式化时间戳
     *
     * @param timestamp Unix 时间戳（秒或毫秒）
     * @param format 输出格式
     * @param timezone 时区
     * @return 格式化后的时间
     */
    @Tool(
        name = "format_timestamp",
        description = "将 Unix 时间戳转换为可读的日期时间格式"
    )
    public String formatTimestamp(
        @ToolParam(name = "timestamp", description = "Unix 时间戳（秒或毫秒）", required = true) Long timestamp,
        @ToolParam(
            name = "format",
            description = "输出格式（如 'yyyy-MM-dd HH:mm:ss'）",
            required = false
        ) String format,
        @ToolParam(
            name = "timezone",
            description = "时区",
            required = false
        ) String timezone
    ) {
        try {
            // 判断是秒还是毫秒
            long millis = timestamp > 1_000_000_000_000L ? timestamp : timestamp * 1000;

            ZoneId zoneId = ZoneId.systemDefault();
            if (timezone != null && !timezone.isBlank()) {
                zoneId = ZoneId.of(timezone);
            }

            Instant instant = Instant.ofEpochMilli(millis);
            LocalDateTime dateTime = LocalDateTime.ofInstant(instant, zoneId);

            DateTimeFormatter formatter = format != null && !format.isBlank()
                ? DateTimeFormatter.ofPattern(format)
                : DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            return String.format("⏰ 时间戳 %d 对应时间：%s", timestamp, dateTime.format(formatter));
        } catch (Exception e) {
            return String.format("Error: 无效的时间戳或格式错误。%s", e.getMessage());
        }
    }

    /**
     * 获取时区列表（常用）
     *
     * @return 常用时区列表
     */
    @Tool(
        name = "list_timezones",
        description = "返回常用的时区列表供选择"
    )
    public String listTimezones() {
        String[] timezones = {
            "Asia/Shanghai",    // 北京时间
            "Asia/Tokyo",       // 东京时间
            "Asia/Hong_Kong",   // 香港时间
            "Asia/Singapore",   // 新加坡时间
            "Asia/Seoul",       // 首尔时间
            "America/New_York",  // 纽约时间（东部）
            "America/Los_Angeles", // 洛杉矶时间（太平洋）
            "America/Chicago",   // 芝加哥时间（中部）
            "Europe/London",    // 伦敦时间
            "Europe/Paris",     // 巴黎时间
            "Europe/Berlin",    // 柏林时间
            "Australia/Sydney", // 悉尼时间
            "Pacific/Tokyo"     // 太平洋时间
        };

        StringBuilder result = new StringBuilder();
        result.append("🌍 常用时区列表：\n\n");

        ZoneId systemZone = ZoneId.systemDefault();
        LocalDateTime now = LocalDateTime.now();

        for (String tz : timezones) {
            LocalDateTime tzTime = LocalDateTime.now(ZoneId.of(tz));
            String timeStr = tzTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            String diff = calculateOffset(now, tzTime);

            String marker = tz.equals(systemZone.getId()) ? " ⭐（系统时区）" : "";
            result.append(String.format("• %-20s %s %s%s\n", tz, timeStr, diff, marker));
        }

        return result.toString();
    }

    private String calculateOffset(LocalDateTime local, LocalDateTime target) {
        long diffMinutes = java.time.Duration.between(local, target).toMinutes();
        if (diffMinutes == 0) return "(同区)";
        return String.format("(UTC%+d)", diffMinutes / 60);
    }
}
```

### 5.5.6 工具注册服务（ToolRegistrationService.java）

```java
package io.agentscope.tutorial.chapter05.service;

import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.ToolkitConfig;
import io.agentscope.tutorial.chapter05.tools.CalculatorTool;
import io.agentscope.tutorial.chapter05.tools.DateTimeTool;
import io.agentscope.tutorial.chapter05.tools.WeatherTool;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * 工具注册服务
 *
 * <p>负责初始化和管理 AgentScope 工具系统。
 * 在应用启动时自动注册所有工具，并配置工具分组。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Service
public class ToolRegistrationService {

    private final Toolkit toolkit;

    public ToolRegistrationService() {
        // 创建 Toolkit，配置为顺序执行
        ToolkitConfig config = ToolkitConfig.builder()
            .parallel(false)
            .allowToolDeletion(true)
            .build();

        this.toolkit = new Toolkit(config);

        // 初始化注册
        initializeTools();
    }

    /**
     * 获取工具包实例
     */
    public Toolkit getToolkit() {
        return toolkit;
    }

    /**
     * 初始化所有工具
     */
    private void initializeTools() {
        System.out.println("=== AgentScope 工具系统初始化 ===");

        // 1. 创建工具分组
        createToolGroups();

        // 2. 注册内置工具
        registerBuiltInTools();

        // 3. 注册自定义工具
        registerCustomTools();

        // 4. 注册元工具（使 Agent 可以动态管理工具分组）
        toolkit.registerMetaTool();

        // 5. 打印注册摘要
        printRegistrationSummary();
    }

    /**
     * 创建工具分组
     */
    private void createToolGroups() {
        // 默认激活的分组
        toolkit.createToolGroup("datetime", "日期时间工具", true);
        toolkit.createToolGroup("calculator", "计算器工具", true);

        // 默认不激活的分组（Agent 需要时可动态激活）
        toolkit.createToolGroup("weather", "天气查询工具", false);
        toolkit.createToolGroup("analysis", "数据分析工具", false);

        System.out.println("工具分组创建完成");
    }

    /**
     * 注册内置工具
     */
    private void registerBuiltInTools() {
        // 注册文件读取工具（限制工作目录）
        // ReadFileTool readFileTool = new ReadFileTool("/data/workspace");
        // toolkit.registerAgentTool(readFileTool);
        System.out.println("内置工具注册完成");
    }

    /**
     * 注册自定义工具
     */
    private void registerCustomTools() {
        // 注册计算器工具到 calculator 分组
        toolkit.registration()
            .tool(new CalculatorTool())
            .group("calculator")
            .apply();

        // 注册日期时间工具到 datetime 分组
        toolkit.registration()
            .tool(new DateTimeTool())
            .group("datetime")
            .apply();

        // 注册天气工具到 weather 分组
        toolkit.registration()
            .tool(new WeatherTool())
            .group("weather")
            .apply();

        System.out.println("自定义工具注册完成");
    }

    /**
     * 激活工具分组
     *
     * @param groupNames 分组名称列表
     */
    public void activateGroups(List<String> groupNames) {
        toolkit.updateToolGroups(groupNames, true);
    }

    /**
     * 停用工具分组
     *
     * @param groupNames 分组名称列表
     */
    public void deactivateGroups(List<String> groupNames) {
        toolkit.updateToolGroups(groupNames, false);
    }

    /**
     * 获取当前活跃的分组
     */
    public List<String> getActiveGroups() {
        return toolkit.getActiveGroups();
    }

    /**
     * 获取已注册的工具列表
     */
    public List<String> getRegisteredTools() {
        return toolkit.getToolNames().stream().toList();
    }

    /**
     * 打印注册摘要
     */
    private void printRegistrationSummary() {
        System.out.println("\n=== 工具注册摘要 ===");
        System.out.println("总工具数: " + toolkit.getToolNames().size());
        System.out.println("活跃分组: " + toolkit.getActiveGroups());
        System.out.println("已注册工具: " + toolkit.getToolNames());
        System.out.println("元工具: reset_equipped_tools（已启用）");
        System.out.println("======================\n");
    }

    /**
     * 演示工具调用
     */
    public String demoToolCalling() {
        StringBuilder result = new StringBuilder();
        result.append("=== 工具调用演示 ===\n\n");

        // 演示计算器工具
        result.append("【计算器工具演示】\n");
        CalculatorTool calculator = new CalculatorTool();
        result.append("• add(10, 5) = ").append(calculator.add(10.0, 5.0)).append("\n");
        result.append("• factorial(5) = ").append(calculator.factorial(5)).append("\n");
        result.append("• is_prime(17) = ").append(calculator.isPrime(17)).append("\n\n");

        // 演示天气工具
        result.append("【天气工具演示】\n");
        WeatherTool weather = new WeatherTool();
        result.append(weather.getWeather("北京", null)).append("\n\n");

        // 演示日期时间工具
        result.append("【日期时间工具演示】\n");
        DateTimeTool dateTime = new DateTimeTool();
        result.append(dateTime.getCurrentDateTime("Asia/Shanghai", null)).append("\n");
        result.append(dateTime.getWeekday("2024-05-20")).append("\n");

        return result.toString();
    }
}
```

### 5.5.7 Spring Boot 配置类（AgentConfig.java）

```java
package io.agentscope.tutorial.chapter05.config;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter05.service.ToolRegistrationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AgentScope 配置类
 *
 * <p>配置 ReActAgent 实例，集成工具系统和模型客户端。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Configuration
public class AgentConfig {

    @Value("${agentscope.api.key:demo-key}")
    private String apiKey;

    @Value("${agentscope.model.name:qwen-max}")
    private String modelName;

    /**
     * 创建 ReActAgent 实例
     */
    @Bean
    public ReActAgent reactAgent(ToolRegistrationService toolService) {
        // 获取已配置的 Toolkit
        Toolkit toolkit = toolService.getToolkit();

        // 构建 Agent
        return ReActAgent.builder()
            .name("TutorialAgent")
            .sysPrompt(
                "你是一个智能助手，名为 TutorialAgent。\n" +
                "你可以使用各种工具来回答问题和完成任务。\n" +
                "可用工具包括：\n" +
                "  • 计算器：数学运算、阶乘、质数判断等\n" +
                "  • 天气查询：查询城市天气、预报、空气质量\n" +
                "  • 日期时间：获取当前时间、日期计算、星期查询\n\n" +
                "当用户需要使用工具时，请清楚地说明你正在使用什么工具。\n" +
                "如果某个工具分组未激活，你可以要求用户激活它。"
            )
            .model(
                DashScopeChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .stream(true)
                    .enableThinking(false)
                    .formatter(new DashScopeChatFormatter())
                    .build()
            )
            .toolkit(toolkit)
            .memory(new InMemoryMemory())
            .enableMetaTool(true)  // 启用元工具，允许 Agent 动态管理工具分组
            .build();
    }
}
```

### 5.5.8 REST API 控制器（AgentController.java）

```java
package io.agentscope.tutorial.chapter05.controller;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.*;
import io.agentscope.tutorial.chapter05.service.ToolRegistrationService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Agent HTTP 控制器
 *
 * <p>提供 RESTful API 接口，供前端或外部系统调用 Agent 服务。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final ReActAgent agent;
    private final ToolRegistrationService toolService;

    public AgentController(ReActAgent agent, ToolRegistrationService toolService) {
        this.agent = agent;
        this.toolService = toolService;
    }

    /**
     * 发送消息给 Agent
     *
     * @param request 包含消息内容的请求
     * @return Agent 的回复
     */
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody ChatRequest request) {
        try {
            Msg userMsg = Msg.of(Msg.Role.USER, request.getMessage());
            List<Msg> history = agent.run(userMsg);

            // 获取最后一条 Agent 回复
            String reply = "";
            for (int i = history.size() - 1; i >= 0; i--) {
                if (history.get(i).getRole() == Msg.Role.ASSISTANT) {
                    reply = history.get(i).getContent().toString();
                    break;
                }
            }

            return Map.of(
                "success", true,
                "reply", reply,
                "history", history
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * 流式对话
     *
     * @param request 包含消息的请求
     * @return SSE 流
     */
    @PostMapping("/chat/stream")
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        Msg userMsg = Msg.of(Msg.Role.USER, request.getMessage());

        return Flux.create(sink -> {
            try {
                // 使用流式响应
                Flux<Msg> stream = agent.runStream(userMsg);
                stream.subscribe(
                    msg -> {
                        // 发送消息片段
                        String content = msg.getContent() != null ? msg.getContent().toString() : "";
                        sink.next("data: " + content + "\n\n");
                    },
                    error -> {
                        sink.error(error);
                    },
                    () -> {
                        sink.complete();
                    }
                );
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    /**
     * 获取工具列表
     */
    @GetMapping("/tools")
    public Map<String, Object> getTools() {
        return Map.of(
            "tools", toolService.getRegisteredTools(),
            "activeGroups", toolService.getActiveGroups()
        );
    }

    /**
     * 激活工具分组
     */
    @PostMapping("/tools/activate")
    public Map<String, Object> activateGroups(@RequestBody List<String> groups) {
        toolService.activateGroups(groups);
        return Map.of(
            "success", true,
            "activeGroups", toolService.getActiveGroups()
        );
    }

    /**
     * 停用工具分组
     */
    @PostMapping("/tools/deactivate")
    public Map<String, Object> deactivateGroups(@RequestBody List<String> groups) {
        toolService.deactivateGroups(groups);
        return Map.of(
            "success", true,
            "activeGroups", toolService.getActiveGroups()
        );
    }

    /**
     * 工具调用演示
     */
    @GetMapping("/tools/demo")
    public Map<String, Object> demoTools() {
        return Map.of(
            "success", true,
            "result", toolService.demoToolCalling()
        );
    }

    // ==================== 请求/响应类 ====================

    public static class ChatRequest {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
```

### 5.5.9 Spring Boot 应用入口（Chapter05Application.java）

```java
package io.agentscope.tutorial.chapter05;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentScope 第五章教程 - 工具系统
 *
 * <p>本应用演示了 AgentScope Java 的完整工具系统，包括：
 * <ul>
 *   <li>内置工具的使用（文件操作等）</li>
 *   <li>自定义工具开发（计算器、天气查询、日期时间）</li>
 *   <li>工具分组管理</li>
 *   <li>元工具（reset_equipped_tools）</li>
 *   <li>Spring Boot 3 集成</li>
 * </ul>
 *
 * <p>启动后可通过以下端点交互：
 * <ul>
 *   <li>POST /api/agent/chat - 发送消息给 Agent</li>
 *   <li>GET /api/agent/tools - 获取工具列表</li>
 *   <li>POST /api/agent/tools/activate - 激活工具分组</li>
 *   <li>GET /api/agent/tools/demo - 演示工具调用</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@SpringBootApplication
public class Chapter05Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter05Application.class, args);
    }
}
```

### 5.5.10 application.yml 配置

```yaml
# AgentScope 第五章教程配置
server:
  port: 8080

spring:
  application:
    name: chapter05-tools

# AgentScope 配置
agentscope:
  # 阿里云百炼 API Key（请替换为实际值）
  api:
    key: ${DASHSCOPE_API_KEY:your-api-key-here}

  # 模型配置
  model:
    name: qwen-max

# 日志配置
logging:
  level:
    io.agentscope: DEBUG
    root: INFO
```

### 5.5.11 单元测试（CalculatorToolTest.java）

```java
package io.agentscope.tutorial.chapter05;

import io.agentscope.tutorial.chapter05.tools.CalculatorTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 计算器工具单元测试
 */
class CalculatorToolTest {

    private CalculatorTool calculator;

    @BeforeEach
    void setUp() {
        calculator = new CalculatorTool();
    }

    @Nested
    @DisplayName("基础运算测试")
    class BasicOperationsTest {

        @Test
        @DisplayName("加法测试")
        void testAdd() {
            String result = calculator.add(10.0, 5.0);
            assertTrue(result.contains("15.00"));
        }

        @Test
        @DisplayName("减法测试")
        void testSubtract() {
            String result = calculator.subtract(10.0, 5.0);
            assertTrue(result.contains("5.00"));
        }

        @Test
        @DisplayName("乘法测试")
        void testMultiply() {
            String result = calculator.multiply(10.0, 5.0);
            assertTrue(result.contains("50.00"));
        }

        @Test
        @DisplayName("除法测试")
        void testDivide() {
            String result = calculator.divide(10.0, 2.0);
            assertTrue(result.contains("5.00"));
        }

        @Test
        @DisplayName("除数为零测试")
        void testDivideByZero() {
            String result = calculator.divide(10.0, 0.0);
            assertTrue(result.contains("Error"));
            assertTrue(result.contains("零"));
        }
    }

    @Nested
    @DisplayName("科学计算测试")
    class ScientificCalculationTest {

        @Test
        @DisplayName("阶乘测试 - 5! = 120")
        void testFactorial5() {
            String result = calculator.factorial(5);
            assertTrue(result.contains("120"));
        }

        @Test
        @DisplayName("阶乘测试 - 0! = 1")
        void testFactorial0() {
            String result = calculator.factorial(0);
            assertTrue(result.contains("1"));
        }

        @Test
        @DisplayName("阶乘测试 - 负数报错")
        void testFactorialNegative() {
            String result = calculator.factorial(-1);
            assertTrue(result.contains("Error"));
            assertTrue(result.contains("负数"));
        }

        @Test
        @DisplayName("阶乘测试 - 数字太大")
        void testFactorialTooLarge() {
            String result = calculator.factorial(21);
            assertTrue(result.contains("Error"));
            assertTrue(result.contains("太大"));
        }

        @Test
        @DisplayName("质数判断 - 17 是质数")
        void testIsPrimeTrue() {
            String result = calculator.isPrime(17);
            assertTrue(result.contains("质数"));
        }

        @Test
        @DisplayName("质数判断 - 15 不是质数")
        void testIsPrimeFalse() {
            String result = calculator.isPrime(15);
            assertFalse(result.contains("是质数"));
            assertTrue(result.contains("可以被"));
        }

        @Test
        @DisplayName("平方根测试")
        void testSqrt() {
            String result = calculator.sqrt(16.0);
            assertTrue(result.contains("4"));
        }

        @Test
        @DisplayName("平方根测试 - 负数报错")
        void testSqrtNegative() {
            String result = calculator.sqrt(-16.0);
            assertTrue(result.contains("Error"));
        }

        @Test
        @DisplayName("幂运算测试")
        void testPower() {
            String result = calculator.power(2.0, 10.0);
            assertTrue(result.contains("1024"));
        }
    }

    @Nested
    @DisplayName("统计运算测试")
    class StatisticsTest {

        @Test
        @DisplayName("平均值测试")
        void testAverage() {
            String result = calculator.average("[1, 2, 3, 4, 5]");
            assertTrue(result.contains("3.00"));
            assertTrue(result.contains("5"));
        }
    }
}
```

### 5.5.12 运行说明

**1. 配置 API Key**

在运行前，需要配置阿里云百炼的 API Key：

```bash
# 设置环境变量
export DASHSCOPE_API_KEY=your-actual-api-key

# 或者修改 application.yml 中的 agentscope.api.key 配置
```

**2. 编译和运行**

```bash
# 编译项目
mvn clean compile

# 运行单元测试
mvn test

# 启动应用
mvn spring-boot:run
```

**3. 测试 API**

```bash
# 获取工具列表
curl http://localhost:8080/api/agent/tools

# 发送消息给 Agent
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "计算 25 + 17 等于多少？"}'

# 激活天气分组
curl -X POST http://localhost:8080/api/agent/tools/activate \
  -H "Content-Type: application/json" \
  -d '["weather"]'

# 演示工具调用
curl http://localhost:8080/api/agent/tools/demo
```

**4. 预期输出示例**

```json
{
  "success": true,
  "result": "=== 工具调用演示 ===\n\n" +
    "【计算器工具演示】\n" +
    "• add(10, 5) = 10.00 + 5.00 = 15.00\n" +
    "• factorial(5) = 5! = 120\n" +
    "• is_prime(17) = 17 是质数\n\n" +
    "【天气工具演示】\n" +
    "🌤️ 北京 当前天气\n" +
    "• 温度：15°C\n" +
    "• 天气：晴\n" +
    "• 空气质量：优\n" +
    "• 湿度：45%\n" +
    "• 风力：北风 3-4级\n\n" +
    "【日期时间工具演示】\n" +
    "📅 当前时间（Asia/Shanghai）：2024-05-20 14:30:00\n" +
    "📅 2024-05-20 是 星期一\n"
}
```

## 5.6 小结

本章介绍了 AgentScope Java 工具系统的完整知识：

| 主题 | 核心内容 |
|------|----------|
| **内置工具** | ReadFileTool, WriteFileTool, ShellCommandTool, MCP 工具, SubAgent 工具 |
| **自定义工具** | 注解方式（@Tool + @ToolParam）、接口实现方式（AgentTool） |
| **工具注册** | 直接注册、Builder API、工具分组、预设参数 |
| **工具调用** | Toolkit -> ToolExecutor -> AgentTool.callAsync() |
| **参数验证** | JSON Schema 自动验证、ToolValidator、类型自动推断 |
| **工具分组** | 创建、激活/停用、元工具（reset_equipped_tools） |

**关键设计理念：**

1. **注解驱动**：通过 `@Tool` 和 `@ToolParam` 注解，框架自动完成工具发现、Schema 生成、参数绑定
2. **异步优先**：工具方法支持 `Mono<T>` 异步返回，框架自动处理
3. **安全优先**：文件工具提供路径验证、分组隔离、预设参数隐藏等安全机制
4. **灵活扩展**：支持 MCP 协议、SubAgent、子类扩展，满足各种集成需求

下一章我们将学习 **第六章：模型集成**，了解如何配置和使用不同的模型客户端（DashScope、OpenAI、Ollama、Gemini 等）。