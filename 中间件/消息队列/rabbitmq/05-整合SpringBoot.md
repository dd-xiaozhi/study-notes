# 1、引入start
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```



# 2、<font style="color:rgb(0,0,0);">修改配置文件</font>

```yaml
spring:
	rabbitmq:
  	host: 127.0.0.1
    port: 5672
  	useranme: guest
  	password: guest
```

:::color1
Tip：接着就可以创建对应的Bean来使用了

:::



