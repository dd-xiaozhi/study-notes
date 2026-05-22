# 一、概述

## 简介

springSecutiry是一个安全框架，负责对用户的认证和授权

**认证**：通过用户名和密码来对用户进行认证，判断是否是本系统的用户

**授权**：用户拥有哪些访问权限，可以访问哪些页面

我们都知道 servlet 是用来接收请求服务器的，而在它之前还会经过一系列的 Filter，通过Filter 来对请求进行拦截过滤，防止非法人员对服务进行非法操作，保护服务安全，而 SpringSecurity 的核心也是由多个Filter 来进行构建的。

它默认是已经帮我们配置好了一些Filter，这里我们简单的介绍下重要的几个Filter：

- **CsrfFilter**：这个Filter 用于防止跨站点请求伪造攻击，它会导致除了 GET 请求之外的所有请求都失败，前后端分离的基于Token认证的API服务可以关闭这个 Filter
- **BasicAuthenticationFilter**：支持 HTTP 的标准 Basic Auth 的身份认证模块
- **UsernamePasswordAuthenticationFilter**：支持 Form 表单形式的身份验证模块
- **DefaultLoginPageGeneratingFilter和DefaultLogoutPageGeneratingFilter**：用于自动生成登录页面和注销页面。
- **AuthorizationFilter：** 这个Filter负责授权模块，发生拒绝访问或权限不够等问题可以 Dubug 这个 Filter



## 架构

本章讲解 Spring Securtiy 的整体构造



### DelegatingFilterProxy

Spring Security是运行在 Spring 环境下的，所以它如何跟 Spring 进行连接呢？

通过 DelegatingFilterProxy 来链接 Spring ，它相当于是一个桥梁，负责将工作委托给实现 Filter 的 Spring Bean，相当于是做了个代理

![image-20240429193710467](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20240429193710467.png)



### FilterChainProxy

它是 DelegatingFilterProxy 的核心，由它来决定调用那个 SecurityFilterChain

![image-20240429194201166](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20240429194201166.png)



### SecurityFilterChain

![image-20240429194029689](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20240429194029689.png)

在 SecurityFilterChain 中存在很多的 Filter，Spring Security 默认配置了一个，我们可以自定义配置我们自己的 SecurityFilterChain ，它会按照我们配置的顺序来执行不同的 Filter

比如我们的配置如下：

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(Customizer.withDefaults())
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .formLogin(Customizer.withDefaults());
        return http.build();
    }
}
```

那么它的顺序如下：

1. CsrfFilter
2. UsernamePasswordAuthnticationFilter
3. BasicAuthenticationFilter
4. AuthorizationFilter

执行顺序也会在程序启动的时候打印出来，它的日志级别是 INFO，如下是我的一个项目打印出来的日志，可以看到对应的Filter执行顺序

```log
2024-04-29 19:49:10.258 [restartedMain] INFO  o.s.s.web.DefaultSecurityFilterChain - Will secure any request with [org.springframework.security.web.session.DisableEncodeUrlFilter@26896487, org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter@484aa090, org.springframework.security.web.context.SecurityContextHolderFilter@5b5ae71d, org.springframework.security.web.header.HeaderWriterFilter@5e589f, org.springframework.web.filter.CorsFilter@11e4257e, com.xiaozhi.aoaojiao.core.security.filter.TokenAuthenticationFilter@3220b2d9, org.springframework.security.web.savedrequest.RequestCacheAwareFilter@20605cf0, org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter@7ba5f6e, org.springframework.security.web.authentication.AnonymousAuthenticationFilter@231d36cb, org.springframework.security.web.session.SessionManagementFilter@9fccfa8, org.springframework.security.web.access.ExceptionTranslationFilter@8cdefdc, org.springframework.security.web.access.intercept.AuthorizationFilter@4b84b52b]
```





# 二、快速上手

这里主要讲解前后端分离项目下如何快速上手 SpringSecurity，想要了解细节的可以去参考网上其他文章









![image-20250615152911541](D:/%E5%AD%A6%E4%B9%A0%E7%AC%94%E8%AE%B0/Java/%E6%A1%86%E6%9E%B6/xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/2025/image-20250615152911541.png)

















