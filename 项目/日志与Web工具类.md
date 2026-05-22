# 日志

## MDC

### 简介

MDC 全称是 Mapped Diagnostic Context，可以粗略的理解成是一个线程安全的存放诊断日志的容器

我们为什么需要使用MDC呢？

我们一般在项目中遇到关键的地方会进行日志的记录，以此来保证出现问题的时候可以快速排查，但是问题是我们一般都是在多线程的环境下处理工作，那么这就会造成日志输出的混乱，比如上一个请求还没结束，下一个请求就进来了，那么就会造成第一条是第一个请求的日志，第二条是第二个请求的日志，这非常不方便我们去排查问题，而MDC呢它可以帮我们记录上下文信息，你可以理解为它就是一个标记，标记当前线程，也就是说我们可以通过这个标记来获取整个请求或者线程的执行日志。

一般我们在项目中会在 MDC 中记录一个 traceId，通过它来标记当前请求，后面查看日志的时候就可以通过这个 traceId 来获取整个请求的日志

### 使用

MDC 它是一个工具类，它在我们的 `org.slf4j` 包下，它的操作和 Map 一样，不过我们需要在日志配置中配置一下我们设置的上下文变量，让它在日志输出的时候携带上我们设置的变量

MDC 提供了以下基本操作方法：

- put(key, value): 向当前线程的 MDC 中添加或更新一个键值对
- get(key): 从当前线程的 MDC 中获取指定键对应的值
- remove(key): 从当前线程的 MDC 中移除指定键及其对应的值
- clear(): 清空当前线程的整个 MDC
- getCopyOfContextMap(): 获取当前线程 MDC 的一个副本（通常是 Map 对象）

我们还需要在配置文件中进行配置，这个必须要配置，不然的话日志是不会记录我们设定好的变量的

可以使用 `%mdc` 或 `%X` 来使用 MDC 中存储的上下文变量

```xml
<encoder>
  <!-- 这里我们使用的是  traceId 变量 -->
  <pattern>%date %level [%thread] %logger{10} [%x{traceId}] %msg%n</pattern>
</encoder>
```

### 自定义工具类

#### MDCUtil

```java
@Slf4j
@UtilityClass
public class MDCUtil {

    public static final String TRACE_ID = "trace-id";

    public static String getTraceId() {
        String traceId = MDC.get(TRACE_ID);
        if (StringUtils.isNotBlank(traceId)) {
            return traceId;
        }

        traceId = create();
        MDC.put(TRACE_ID, traceId);

        return traceId;
    }

    public static Runnable wrapRunnable(final Runnable runnable) {
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                if (contextMap != null) {
                    MDC.clear();
                }
            }
        };
    }

    public static <T> Callable<T> wrapCallable(final Callable<T> callable) {
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                return callable.call();
            } finally {
                if (contextMap != null) {
                    MDC.clear();
                }
            }
        };
    }

    public static <T> Supplier<T> wrapSupplier(final Supplier<T> supplier) {
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                return supplier.get();
            } finally {
                MDC.clear();
            }
        };
    }

    public static void clear() {
        MDC.clear();
    }

    private static String create() {
        String uuid = UUID.randomUUID().toString().replace(SymbolConstant.HYPHEN, StringUtils.EMPTY);
        return uuid + System.currentTimeMillis();
    }
}
```

#### 多线程之间传递 traceId

```java
@Slf4j
public class MDCPropagatingRunnable implements Runnable {

    private final Runnable delegate;
    private final Map<String, String> contextMap;

    public MDCPropagatingRunnable(Runnable delegate) {
        this.delegate = delegate;
        this.contextMap = MDC.getCopyOfContextMap();
    }

    @Override
    public void run() {
        // 从当前线程复制MDC上下文到新线程
        if (contextMap != null) {
            MDC.setContextMap(contextMap);
        }

        try {
            delegate.run();
        } catch (Exception ex) {
            log.error("Async thread run error", ex);
        } finally {
            // 清理MDC以防泄露资源，特别是当runnable被线程池重用时
            MDC.clear();
        }
    }
}
```

# Web

## 请求上下文

```java
/**
 * 获取 HttpServletRequest, 不支持异步获取
 * @return 成功返回 {@link HttpServletRequest}, 失败抛出异常
 */
public static HttpServletRequest getRequest() {
    ServletRequestAttributes attributes = (ServletRequestAttributes)
            RequestContextHolder.getRequestAttributes();
    if (null == attributes) {
        throw new BusinessException(GeneralErrorCode.INVALID_REQUEST);
    }
    return attributes.getRequest();
}
```