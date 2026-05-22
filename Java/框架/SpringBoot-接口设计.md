# 接口 - 结果返回封装

## enum类

```java
@Getter
@AllArgsConstructor
public enum ResponseStatus {

    SUCCESS("200", "success"),
    FAIL("500", "failed"),

    HTTP_STATUS_200("200", "ok"),
    HTTP_STATUS_400("400", "request error"),
    HTTP_STATUS_401("401", "no authentication"),
    HTTP_STATUS_403("403", "no authorities"),
    HTTP_STATUS_500("500", "server error");

    public static final List<ResponseStatus> HTTP_STATUS_ALL = Collections.unmodifiableList(
            Arrays.asList(HTTP_STATUS_200, HTTP_STATUS_400, HTTP_STATUS_401, HTTP_STATUS_403, HTTP_STATUS_500
            ));

    /**
     * response code
     */
    private final String responseCode;

    /**
     * description.
     */
    private final String description;

}
```



## 结果类封装

```java
@Data
@Builder
public class ResponseResult<T> {

    /**
     * response timestamp.
     */
    private long timestamp;

    /**
     * response code, 200 -> OK.
     */
    private String status;

    /**
     * response message.
     */
    private String message;

    /**
     * response data.
     */
    private T data;

    /**
     * response success result wrapper.
     *
     * @param <T> type of data class
     * @return response result
     */
    public static <T> ResponseResult<T> success() {
        return success(null);
    }

    /**
     * response success result wrapper.
     *
     * @param data response data
     * @param <T>  type of data class
     * @return response result
     */
    public static <T> ResponseResult<T> success(T data) {
        return ResponseResult.<T>builder().data(data)
                .message(ResponseStatus.SUCCESS.getDescription())
                .status(ResponseStatus.SUCCESS.getResponseCode())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * response error result wrapper.
     *
     * @param message error message
     * @param <T>     type of data class
     * @return response result
     */
    public static <T extends Serializable> ResponseResult<T> fail(String message) {
        return fail(null, message);
    }

    /**
     * response error result wrapper.
     *
     * @param data    response data
     * @param message error message
     * @param <T>     type of data class
     * @return response result
     */
    public static <T> ResponseResult<T> fail(T data, String message) {
        return ResponseResult.<T>builder().data(data)
                .message(message)
                .status(ResponseStatus.FAIL.getResponseCode())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> ResponseResult<T> fail(ResponseStatus responseStatus) {
        return ResponseResult.<T>builder().data(null)
                .message(responseStatus.getDescription())
                .status(responseStatus.getResponseCode())
                .timestamp(System.currentTimeMillis())
                .build();
    }

}
```





# 接口 - 参数校验

## 导入依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```



## 请求参数封装

```java
@Data
@Builder
@ApiModel(value = "User", subTypes = {AddressParam.class})
public class UserParam implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "could not be empty")
    private String userId;

    @NotEmpty(message = "could not be empty")
    @Email(message = "invalid email")
    private String email;

    @NotEmpty(message = "could not be empty")
    @Pattern(regexp = "^(\\d{6})(\\d{4})(\\d{2})(\\d{2})(\\d{3})([0-9]|X)$", message = "invalid ID")
    private String cardNo;

    @NotEmpty(message = "could not be empty")
    @Length(min = 1, max = 10, message = "nick name should be 1-10")
    private String nickName;

    @NotEmpty(message = "could not be empty")
    @Range(min = 0, max = 1, message = "sex should be 0-1")
    private int sex;

    @Max(value = 100, message = "Please input valid age")
    private int age;

    @Valid
    private AddressParam address;

}
```

在 controller 参数中使用 `@Vaild` 来标记需要校验的类

```java
@GetMapping("/test")
public String test(@Vaild @RequestBody UserParam userParam) { ... }
```





## Validation分组校验

当我们需要一个参数接收对象被复用的时候，由于复用的时候对应的逻辑不一样就会导致校验冲突

比如：更新和添加操作，更新它的ID不许为空，而添加是可以为空的

解决：使用 Vaildation 分组校验解决这个问题，不同的分组的校验结果不同



### 定义分组

无需实现接口，可以理解为是一个标记接口

```java
public interface AddValidationGroup {
}
public interface EditValidationGroup {
}
```



### 添加分组

```java
@Data
@Builder
public class UserParam implements Serializable {
    
    private static final long serialVersionUID = 1L;
    @NotEmpty(message = "{user.msg.userId.notEmpty}", groups = {EditValidationGroup.class})
    private String userId;
}
```

只有当使用的是这个分组的时候这个校验才会生效



### 使用分组

```java
@PostMapping("add")
public ResponseEntity<UserParam> add(@Validated(AddValidationGroup.class) @RequestBody UserParam userParam) {
    return ResponseEntity.ok(userParam);
}

@PostMapping("add")
public ResponseEntity<UserParam> add(@Validated(EditValidationGroup.class) @RequestBody UserParam userParam) {
    return ResponseEntity.ok(userParam);
}
```

@Validated(xxx分组) 来使用分组，对应分组的注解才会生效





## @Vaildated 和 @Vaild

- **分组**：@Vaildated 提供一个分组的功能，可以根据不同的分组来让对应的校验生效

- **注解地方**

    @Validated：可以用在类型、方法和方法参数上。但是不能用在成员属性（字段）上

    @Valid：可以用在方法、构造函数、方法参数和成员属性（字段）上

- **嵌套类型**

    @Vaild 可以嵌套另外一个对象





## 常用的校验的注解

**JSR303/JSR-349**: JSR303是一项标准,只提供规范不提供实现，规定一些校验规范即校验注解，如@Null，@NotNull，@Pattern，位于javax.validation.constraints包下。**JSR-349是其的升级版本，添加了一些新特性**

```java
@AssertFalse            被注释的元素只能为false
@AssertTrue             被注释的元素只能为true
@DecimalMax             被注释的元素必须小于或等于{value}
@DecimalMin             被注释的元素必须大于或等于{value}
@Digits                 被注释的元素数字的值超出了允许范围(只允许在{integer}位整数和{fraction}位小数范围内)
@Email                  被注释的元素不是一个合法的电子邮件地址
@Future                 被注释的元素需要是一个将来的时间
@FutureOrPresent        被注释的元素需要是一个将来或现在的时间
@Max                    被注释的元素最大不能超过{value}
@Min                    被注释的元素最小不能小于{value}
@Negative               被注释的元素必须是负数
@NegativeOrZero         被注释的元素必须是负数或零
@NotBlank               被注释的元素不能为空
@NotEmpty               被注释的元素不能为空
@NotNull                被注释的元素不能为null
@Null                   被注释的元素必须为null
@Past                   被注释的元素需要是一个过去的时间
@PastOrPresent          被注释的元素需要是一个过去或现在的时间
@Pattern                被注释的元素需要匹配正则表达式"{regexp}"
@Positive               被注释的元素必须是正数
@PositiveOrZero         被注释的元素必须是正数或零
@Size                   被注释的元素个数必须在{min}和{max}之间
```

**hibernate validation**：hibernate validation是对这个规范的实现，并增加了一些其他校验注解，如@Email，@Length，@Range等等

```java
@CreditCardNumber       被注释的元素不合法的信用卡号码
@Currency               被注释的元素不合法的货币 (必须是{value}其中之一)
@EAN                    被注释的元素不合法的{type}条形码
@Email                  被注释的元素不是一个合法的电子邮件地址  (已过期)
@Length                 被注释的元素长度需要在{min}和{max}之间
@CodePointLength        被注释的元素长度需要在{min}和{max}之间
@LuhnCheck              被注释的元素${validatedValue}的校验码不合法, Luhn模10校验和不匹配
@Mod10Check             被注释的元素${validatedValue}的校验码不合法, 模10校验和不匹配
@Mod11Check             被注释的元素${validatedValue}的校验码不合法, 模11校验和不匹配
@ModCheck               被注释的元素${validatedValue}的校验码不合法, ${modType}校验和不匹配  (已过期)
@NotBlank               被注释的元素不能为空  (已过期)
@NotEmpty               被注释的元素不能为空  (已过期)
@ParametersScriptAssert 被注释的元素执行脚本表达式"{script}"没有返回期望结果
@Range                  被注释的元素需要在{min}和{max}之间
@SafeHtml               被注释的元素可能有不安全的HTML内容
@ScriptAssert           被注释的元素执行脚本表达式"{script}"没有返回期望结果
@URL                    被注释的元素需要是一个合法的URL
@DurationMax            被注释的元素必须小于${inclusive == true ? '或等于' : ''}${days == 0 ? '' : days += '天'}${hours == 0 ? '' : hours += '小时'}${minutes == 0 ? '' : minutes += '分钟'}${seconds == 0 ? '' : seconds += '秒'}${millis == 0 ? '' : millis += '毫秒'}${nanos == 0 ? '' : nanos += '纳秒'}
@DurationMin            被注释的元素必须大于${inclusive == true ? '或等于' : ''}${days == 0 ? '' : days += '天'}${hours == 0 ? '' : hours += '小时'}${minutes == 0 ? '' : minutes += '分钟'}${seconds == 0 ? '' : seconds += '秒'}${millis == 0 ? '' : millis += '毫秒'}${nanos == 0 ? '' : nanos += '纳秒'}
```

**spring validation**：spring validation对hibernate validation进行了二次封装，在springmvc模块中添加了自动校验，并将校验信息封装进了特定的类中





## 自定义validation

- **定义注解**

    ```java
    @Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
    @Retention(RUNTIME)
    @Documented
    @Constraint(validatedBy = {TelephoneNumberValidator.class}) // 指定校验器
    public @interface TelephoneNumber {
        String message() default "Invalid telephone number";
        Class<?>[] groups() default { };
        Class<? extends Payload>[] payload() default { };
    }
    ```

- **定义校验器**

    ```java
    public class TelephoneNumberValidator implements ConstraintValidator<TelephoneNumber, String> {
        private static final String REGEX_TEL = "0\\d{2,3}[-]?\\d{7,8}|0\\d{2,3}\\s?\\d{7,8}|13[0-9]\\d{8}|15[1089]\\d{8}";
    
        @Override
        public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
            try {
                return Pattern.matches(REGEX_TEL, s);
            } catch (Exception e) {
                return false;
            }
        }
    
    }
    ```

- **使用**

    ```java
    @Data
    @Builder
    @ApiModel(value = "User", subTypes = {AddressParam.class})
    public class UserParam implements Serializable {
    
        private static final long serialVersionUID = 1L;
    
        @NotEmpty(message = "{user.msg.userId.notEmpty}", groups = {EditValidationGroup.class})
        private String userId;
    
        @TelephoneNumber(message = "invalid telephone number") // 这里
        private String telephone;
    
    }
    ```

    



# 接口 - 参数校验国际化





# 接口 - 如何统一异常处理

## 优雅处理异常

使用 **@ControllerAdvice** + **@ExceptionHandler** 来优雅处理异常



### 对于400参数错误异常

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * exception handler for bad request.
     *
     * @param e
     *            exception
     * @return ResponseResult
     */
    @ResponseBody
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = { BindException.class, ValidationException.class, MethodArgumentNotValidException.class })
    public ResponseResult<ExceptionData> handleParameterVerificationException(@NonNull Exception e) {
        ExceptionData.ExceptionDataBuilder exceptionDataBuilder = ExceptionData.builder();
        log.warn("Exception: {}", e.getMessage());
        if (e instanceof BindException) {
            BindingResult bindingResult = ((MethodArgumentNotValidException) e).getBindingResult();
            bindingResult.getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .forEach(exceptionDataBuilder::error);
        } else if (e instanceof ConstraintViolationException) {
            if (e.getMessage() != null) {
                exceptionDataBuilder.error(e.getMessage());
            }
        } else {
            exceptionDataBuilder.error("invalid parameter");
        }
        return ResponseResultEntity.fail(exceptionDataBuilder.build(), "invalid parameter");
    }

}
```

对于自定义异常

```java
/**
 * handle business exception.
 *
 * @param businessException
 *            business exception
 * @return ResponseResult
 */
@ResponseBody
@ExceptionHandler(BusinessException.class)
public ResponseResult<BusinessException> processBusinessException(BusinessException businessException) {
    log.error(businessException.getLocalizedMessage(), businessException);
    // 这里可以屏蔽掉后台的异常栈信息，直接返回"business error"
    return ResponseResultEntity.fail(businessException, businessException.getLocalizedMessage());
}
```

对于其它异常

```java
/**
 * handle other exception.
 *
 * @param exception
 *            exception
 * @return ResponseResult
 */
@ResponseBody
@ExceptionHandler(Exception.class)
public ResponseResult<Exception> processException(Exception exception) {
    log.error(exception.getLocalizedMessage(), exception);
    // 这里可以屏蔽掉后台的异常栈信息，直接返回"server error"
    return ResponseResultEntity.fail(exception, exception.getLocalizedMessage());
}
```

在 controller 中就无需处理异常了







## @ControllerAdvice还可以怎么用？

### @InitBinder注解

用于请求中注册自定义参数的解析，从而达到自定义请求参数格式的目的；

比如，在@ControllerAdvice注解的类中添加如下方法，来统一处理日期格式的格式化

> 可以理解为是请求参数类型转换

```java
@InitBinder
public void handleInitBinder(WebDataBinder dataBinder){
    dataBinder.registerCustomEditor(Date.class,
            new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd"), false));
}
```

```java
@GetMapping("testDate")
public Date processApi(Date date) {
    return date;
}
```

此时我们的date接收的就是我们处理好时间格式



### @ModelAttribute注解

用来预设全局参数，比如最典型的使用Spring Security时将添加当前登录的用户信息（UserDetails)作为参数。

```java
// @ControllerAdvice 注解的类内
@ModelAttribute("currentUser")
public UserDetails modelAttribute() {
    return (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
}
```

所有 controller类中requestMapping方法都可以直接获取并使用currentUser

```java
@PostMapping("saveSomething")
public ResponseEntity<String> saveSomeObj(@ModelAttribute("currentUser") UserDetails operator) {
    // 保存操作，并设置当前操作人员的ID（从UserDetails中获得）
    return ResponseEntity.success("ok");
}
```





# 接口 - 提供多个版本的接口

### 为什么要提供多个版本的接口？

因为一般情况下我们的 API 接口在上线之后是不会轻易去改动的，所以我们可以对接口进行版本控制。



### 控制接口版本的方式

相同URL，用**不同的版本参数**区分

- `api.pdai.tech/user?version=v1` 表示 v1版本的接口, 保持原有接口不动
- `api.pdai.tech/user?version=v2` 表示 v2版本的接口，更新新的接口

区分**不同的接口域名**，不同的版本有不同的子域名, 路由到不同的实例:

- `v1.api.pdai.tech/user` 表示 v1版本的接口, 保持原有接口不动, 路由到instance1
- `v2.api.pdai.tech/user` 表示 v2版本的接口，更新新的接口, 路由到instance2

网关路由不同子目录到**不同的实例**（不同package也可以）

- `api.pdai.tech/v1/user` 表示 v1版本的接口, 保持原有接口不动, 路由到instance1
- `api.pdai.tech/v2/user` 表示 v2版本的接口，更新新的接口, 路由到instance2

**同一实例**，用注解隔离不同版本控制

- `api.pdai.tech/v1/user` 表示 v1版本的接口, 保持原有接口不动，匹配@ApiVersion("1")的handlerMapping
- `api.pdai.tech/v2/user` 表示 v2版本的接口，更新新的接口，匹配@ApiVersion("2")的handlerMapping

这里主要展示第四种单一实例中如何优雅的控制接口的版本。



### 实现案例

这个例子基于SpringBoot封装了@ApiVersion注解方式控制接口版本



#### 自定义 @ApiVersion 注解

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface ApiVersion {
    String value();
}
```



#### 定义版本匹配RequestCondition

这里我们的版本匹配支持三层版本，可以自己自定义自己的版本匹配规则

- v1.1.1 （大版本.小版本.补丁版本）
- v1.1 (等同于v1.1.0)
- v1 （等同于v1.0.0)

```java
@Slf4j
public class ApiVersionCondition implements RequestCondition<ApiVersionCondition> {

    /**
     * support v1.1.1, v1.1, v1; three levels .
     */
    private static final Pattern VERSION_PREFIX_PATTERN_1 = Pattern.compile("/v\\d\\.\\d\\.\\d/");
    private static final Pattern VERSION_PREFIX_PATTERN_2 = Pattern.compile("/v\\d\\.\\d/");
    private static final Pattern VERSION_PREFIX_PATTERN_3 = Pattern.compile("/v\\d/");
    private static final List<Pattern> VERSION_LIST = Collections.unmodifiableList(
            Arrays.asList(VERSION_PREFIX_PATTERN_1, VERSION_PREFIX_PATTERN_2, VERSION_PREFIX_PATTERN_3)
    );

    @Getter
    private final String apiVersion;

    public ApiVersionCondition(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    /**
     * method priority is higher then class.
     *
     * @param other other
     * @return ApiVersionCondition
     */
    @Override
    public ApiVersionCondition combine(ApiVersionCondition other) {
        return new ApiVersionCondition(other.apiVersion);
    }

    @Override
    public ApiVersionCondition getMatchingCondition(HttpServletRequest request) {
        for (int vIndex = 0; vIndex < VERSION_LIST.size(); vIndex++) {
            Matcher m = VERSION_LIST.get(vIndex).matcher(request.getRequestURI());
            if (m.find()) {
                String version = m.group(0).replace("/v", "").replace("/", "");
                if (vIndex == 1) {
                    version = version + ".0";
                } else if (vIndex == 2) {
                    version = version + ".0.0";
                }
                if (compareVersion(version, this.apiVersion) >= 0) {
                    log.info("version={}, apiVersion={}", version, this.apiVersion);
                    return this;
                }
            }
        }
        return null;
    }

    @Override
    public int compareTo(ApiVersionCondition other, HttpServletRequest request) {
        return compareVersion(other.getApiVersion(), this.apiVersion);
    }

    private int compareVersion(String version1, String version2) {
        if (version1 == null || version2 == null) {
            throw new RuntimeException("compareVersion error:illegal params.");
        }
        String[] versionArray1 = version1.split("\\.");
        String[] versionArray2 = version2.split("\\.");
        int idx = 0;
        int minLength = Math.min(versionArray1.length, versionArray2.length);
        int diff = 0;
        while (idx < minLength
                && (diff = versionArray1[idx].length() - versionArray2[idx].length()) == 0
                && (diff = versionArray1[idx].compareTo(versionArray2[idx])) == 0) {
            ++idx;
        }
        diff = (diff != 0) ? diff : versionArray1.length - versionArray2.length;
        return diff;
    }
}
```





#### 定义HandlerMapping

```java
public class ApiVersionRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

    /**
     * add @ApiVersion to controller class.
     *
     * @param handlerType handlerType
     * @return RequestCondition
     */
    @Override
    protected RequestCondition<?> getCustomTypeCondition(@NonNull Class<?> handlerType) {
        ApiVersion apiVersion = AnnotationUtils.findAnnotation(handlerType, ApiVersion.class);
        return null == apiVersion ? super.getCustomTypeCondition(handlerType) : new ApiVersionCondition(apiVersion.value());
    }

    /**
     * add @ApiVersion to controller method.
     *
     * @param method method
     * @return RequestCondition
     */
    @Override
    protected RequestCondition<?> getCustomMethodCondition(@NonNull Method method) {
        ApiVersion apiVersion = AnnotationUtils.findAnnotation(method, ApiVersion.class);
        return null == apiVersion ? super.getCustomMethodCondition(method) : new ApiVersionCondition(apiVersion.value());
    }

}
```



### 配置注册HandlerMapping

```java
@Configuration
public class CustomWebMvcConfiguration extends WebMvcConfigurationSupport {

    @Override
    public RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
        return new ApiVersionRequestMappingHandlerMapping();
    }
}
```

或者实现WebMvcRegistrations的接口

```java
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer, WebMvcRegistrations {
    //...

    @Override
    @NonNull
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return new ApiVersionRequestMappingHandlerMapping();
    }

}
```





### 测试运行

```java
@RestController
@RequestMapping("api/{v}/user")
public class UserController {

    @RequestMapping("get")
    public User getUser() {
        return User.builder().age(18).name("pdai, default").build();
    }

    @ApiVersion("1.0.0")
    @RequestMapping("get")
    public User getUserV1() {
        return User.builder().age(18).name("pdai, v1.0.0").build();
    }

    @ApiVersion("1.1.0")
    @RequestMapping("get")
    public User getUserV11() {
        return User.builder().age(19).name("pdai, v1.1.0").build();
    }

    @ApiVersion("1.1.2")
    @RequestMapping("get")
    public User getUserV112() {
        return User.builder().age(19).name("pdai2, v1.1.2").build();
    }
}
```





# 接口 - 实现限流

## 概述

当我们的系统流量超过系统的极限能力时，会导致系统的出现卡死或崩溃的情况，那么为了保护我们的系统，我们可以对某些高访问量的接口进行限流，限制它们的进入，保护我们的系统。限流的常见思路：

算法有以下常用算法

- 令牌桶（Token Bucket）
- 漏桶（leaky bucket）
- 计数器

具体的实现我们可以这样做：

单实例：

- 限制总资源数，比如并发数量、连接数、请求数等资源
- 平滑限制某个接口的请求数
- Guava RateLimiter

分布式：

- Redis + Lua 脚本实现限流
- 使用 Nginx + Lua 脚本
- 使用 OpenResty 开源的限流方案
- 限流框架，比如 Sentinel 实现降级限流熔断、Spring Cloud Getway 实现限流





## 单实例

在单体的环境下我们可以使用 Aop 来对某个接口做限流

- 定义  RateLimit 注解
- 通过 Aop 拦截标记 RateLimit 注解的接口
- 使用 Guava RateLimiter 提供的令牌桶算法实现
    - 平滑突发限流（SmoothBursty）
    - 平滑预热限流（SmoothWarmingUp）实现



### 定义 RateLimit 注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    int limit() default 10;
}
```



### 定义 AOP

```java
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    /**
     * 存储对应的限流器，每一个类对应一个
     */
    private final ConcurrentHashMap<String, RateLimiter> EXISTED_RATE_LIMITERS = new ConcurrentHashMap<>();
    
    @Pointcut("@annotation(com.xiaozhi.demo.annotation.RateLimit)")
    public void rateLimit() {} 
    
    @Around("rateLimit()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String typeName = signature.getDeclaringTypeName();
        RateLimit rateLimit = AnnotationUtils.findAnnotation(signature.getMethod(), RateLimit.class);
        // 根据 RateLimit 上标注的限流个数来进行初始化
        RateLimiter rateLimiter = EXISTED_RATE_LIMITERS.computeIfAbsent(typeName, 
                key -> RateLimiter.create(rateLimit.limit()));
        if (Objects.nonNull(rateLimiter) && rateLimiter.tryAcquire()) {
            return joinPoint.proceed();
        } else {
            throw new RuntimeException("too many request, please later.....");
        }
    }
    
}
```



### Controller 中使用

```java
@RateLimit(limit = 4)
@GetMapping("/hello")
public String hello() {
    return "hello";
}
```



### 测试

```java
public static void main(String[] args) throws InterruptedException {
    int clientSize = 10;
    CountDownLatch countDownLatch = new CountDownLatch(clientSize);
    ExecutorService threadPool = Executors.newFixedThreadPool(clientSize);
    IntStream.range(0, clientSize).forEach(i -> threadPool.submit(() -> {
        RestTemplate restTemplate = new RestTemplate();
        String str = restTemplate.getForObject("http://localhost:10001/hello", String.class);
        System.out.println(str);
    }));
    countDownLatch.await();
    threadPool.shutdown();
}
```

结果显示 只有五个能够正常访问

```java
null
null
null
null
null
hello
hello
hello
hello
hello
```

由于我们的程序中的业务非常简单，所以它的处理会很快，也会出现六个或七个能访问的情况



## 分布式



