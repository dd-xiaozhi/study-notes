# 01	springBoot2入门

官网有对应的示例



# 02	了解自动配置原理

## 1、SpringBoot特点

### 1.1 依赖管理

**父项目做依赖管理**

```xml
<!-- 依赖管理 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.5.1</version>
    <relativePath/> <!-- lookup parent from repository -->
</parent>
<!-- 依赖管理的父依赖 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-dependencies</artifactId>
    <version>2.5.1</version>
</parent>
<!-- 几乎声明了所有开发中常用的依赖的版本号，自动版本仲裁机制，只能使用我设定好的版本 -->
```

**starter场景启动器**

只要引入starter，这个场景的所有常规需要的依赖我们都自动引入

比如我们引入web的场景启动器可以看到常用需要的依赖也一起导入了，我们就可以直接使用了

![在这里插入图片描述](https://img-blog.csdnimg.cn/2021071715575772.png)


官方的starter都是以`spring-boot-starter`开头的，然后接着是他们的场景名

另外还有第三方提供的starter，`*-spring-boot-starter`



**版本控制**

我们不需要去指定版本，springboot会帮我们控制版本

如果我们想要指定的版本，也可以通过pom.xml文件去配置我们特定的版本

```xml
<!-- 在当前项目配置文件下添加 -->
<properties>
    <mysql.version>5.1.43</mysql.version>
</properties>
```



### 1.2 自动配置

我们只要引入我们对应的场景，springboot就会帮我们自动配好配置文件

比如自动配置tomcat的dispatcherServlet、还有字符编码过滤等等。。。



**默认的包结构**：主程序所在的包及其下面的所有子包里面的组件都会默认被扫描进来，不用我们以前的包扫描配置，一切spiingBoot帮我们配好了



**修改默认包扫描**：

如果想要修改默认的包扫描路径我们可以修改默认配置

	 ①`@SpringBootApplication(scanBasePackages = "com.xiaozhi")`
	
	 ②也可以使用`ComponentScan`指定扫描路径

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717171703531.png)



这样的话我们就可以扫描com.xiaozhi下的所有包，而不只是主程序所在包下的所有包



**自动配置项**

我们在配置文件中进行赋值，最终它会映射到对应的配置类上，然后在容器中创建对象

我们的自动配置类并不是所有都能使用的，只有引入对应的场景才会开启对应的自动配置





## 2、容器功能

### 2.1 组件添加

#### 1、@Configuration

**Fuill模式与Lite模式**

这两个模式的开启对应@Configuration属性proxyBeanMethods值为true和false

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151038621.png)


true为fuill模式，springBoot每次都会检查容器中是否存在这个bean，有的话就返回现有的，没有的话就创建，确保每个@Bean方法被调用多少次返回的都是单例的

false为lite模式，每个@Bean方法被调多少次返回的组件都是新创建的

**注意**：组件依赖必须使用Fill模式

![20210619170206106](https://img-blog.csdnimg.cn/20210717151048472.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)

设置为true，那调用的就是为单例的

```java
public static void main(String[] args) {
    ConfigurableApplicationContext run = SpringApplication.run(Springboot1Application.class, args);
    Person person = run.getBean("person", Person.class);
    Animal cat = run.getBean("animal", Animal.class);
    // 判断person中的animal对象和
    System.out.println(person.getAnimal());
    System.out.println("是否单例" + (person.getAnimal() == cat));
}
```

设置为false，每次调用都是新的实例

![20210619170958071](https://img-blog.csdnimg.cn/20210717151106455.png)



#### 2、@Component衍生的注解

@Controller、@Service、@Repository



#### 3、组件扫描 

**①@ComponentScan**

**②Import**

```java
@Import({Person.class, Animal.class})	// 在容器中自动创建两个类型的组件，组件名是全类名
public class Config {
```

![20210619172554899](https://img-blog.csdnimg.cn/20210717151136326.png)



#### 4、@Conditional

条件装配：满足Conditional指定的条件，则进行组件注入

它可以作用在配置类上，条件不满足那么这个配置类下的所有bean都不能注册

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151215264.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)

```java
@Configuration(proxyBeanMethods = true)
public class Config {
    @Bean
    public Animal animal() {
        Animal animal = new Animal("cat");
        return animal;
    }
    // 存在animal这个名字的baen我才创建这个对象
    @ConditionalOnBean(name = "animal")
    @Bean
    public Person person() {
        Person person = new Person("xiaozhi",18);
        person.setAnimal(animal());
        return person;
    }
}
```



### 2.2 原生配置文件引入

#### ① @ImportResource

**注意**：使用这个注解一定要在配置类中

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/context https://www.springframework.org/schema/context/spring-context.xsd">
    
    <bean id="person" class="com.xiaozhi.pojo.Person">
        <property name="name" value="xiaozhi"/>
        <property name="age" value="18"/>
        <property name="animal" ref="animal"/>
    </bean>
    
    <bean id="animal" class="com.xiaozhi.pojo.Animal">
        <property name="name" value="cat"/>
    </bean>
</beans>
```

```java
@Configuration(proxyBeanMethods = true)
@ImportResource("classpath:bean.xml")
public class Config {
//    @Bean
//    public Animal animal() {
//        Animal animal = new Animal("cat");
//        return animal;
//    }
//    // 匹配的类型要 在它之前创建它才会创建，不然就不会创建
//    @ConditionalOnBean(name = "animal")
//    @Bean
//    public Person person() {
//        Person person = new Person("xiaozhi",18);
//        person.setAnimal(animal());
//        return person;
//    }
}
```

![20210619180236036](https://img-blog.csdnimg.cn/20210717151252219.png)




### 2.3 配置绑定

读取properties文件中的内容，并封装到javaBean中

原生的java代码我们需要创建一个Properties对象加载我们的properties文件，然后遍历得到配置文件中的每个名字，然后判断得到我们需要的内容

现在我们可以通过注解来完成读取配置文件的内容



#### ① @ConfigurationProperties

只有在容器中的组件，才会有springBoot的功能

```java
@ConfigurationProperties(prefix = "person")		// 配置文件的前缀
public class Person {
```

```properties
person.name=major
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/2021071715584136.png)






② @EnableConfigurationProperties + @ConfigurationProperties

 @EnableConfigurationProperties(person.class)的两个功能

1.  开启person类的配置功能
2.  把这个person注册到容器中

```java
@EnableConfigurationProperties(Person.class)
public class Springboot1Application {
```
![20210620114305506](https://img-blog.csdnimg.cn/20210717151313111.png)

**注意**：它的组件名是类名-全类名



## 3、自动装配原理入门

我们来看看springBoot帮我们做了什么事



### 3.1 引导自动加载配置类

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(excludeFilters = { @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
      @Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })
public @interface SpringBootApplication {
```

我们可以看到除了一些元注解外，有三个注解是@springBoot的主要功能注解



#### @SpringBootConfiguration

![](https://img-blog.csdnimg.cn/20210717151345918.png)
表明它是一个配置类



#### @ComponentScan

自定扫描那些



#### @EnableAutoConfiguration

开启自动装配，主要功能是导入组件

```java
@AutoConfigurationPackage
@Import(AutoConfigurationImportSelector.class)
public @interface EnableAutoConfiguration {
```



**1、@AutoConfigurationPackage**

```java
@Import(AutoConfigurationPackages.Registrar.class)
public @interface AutoConfigurationPackage {
```

我们点进去可以看到它进行了一个批量导入

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151416607.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


启动springBoot它会执行这个方法将我们定义的bean注册到容器中，我们可以debug计算一下

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151429574.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)
可以看到我们计算出来的结果就是我们主启动类所在包，也就是说@AutoConfigurationPackage的功能是将主启动类所在包下的所有组件进行注册。



**2、@Import(AutoConfigurationImportSelector.class)**

导入一系列组件

```java
public class AutoConfigurationImportSelector implements DeferredImportSelector, BeanClassLoaderAware,
		ResourceLoaderAware, BeanFactoryAware, EnvironmentAware, Ordered {
            
public interface DeferredImportSelector extends ImportSelector {
```

可以看出来AutoConfigurationImportSelector是一个导入选择器，它可以有自己的逻辑实现

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151503594.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


首先它会执行上面这个方法，然后执行getAutoConfigurationEntry(annotationMetadata)方法

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151512716.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


这个方法中主要是getCandidateConfigurations(annotationMetadata, attributes)方法得到我们需要加载组件的名字，其他的操作就是排除一些我们不要的，下面的按需自动开启配置项会讲到

![\[外链图片转存失败,源站可能有防盗链机制,建议将图片保存下来直接上传(img-5UC2mGmJ-1626505674329)(../../Java学习/截图/image-20210620171720147.png)\]](https://img-blog.csdnimg.cn/20210717151523287.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)
![](https://img-blog.csdnimg.cn/20210717151532938.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151559786.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151609790.png)


所以springBoot它会去加载类路径下的META-INF/spring.factories文件，然后得到需要导入的组件进行注册
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151620521.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)
可以看到确实是有的，我们打开这个文件进行查看

![在这里插入图片描述](https://img-blog.csdnimg.cn/202107171516334.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


一共127个官方的自动配置包

springBoot它是会默认找org.springframework.boot.autoconfigure.EnableAutoConfiguration属性的

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151642245.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)
对应前面的getCandidateConfigurations(annotationMetadata, attributes)方法返回结果是一致的
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151651142.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


**测试**

我们导入一个第三方的starter，查看结果

导入mybatis-spring-boot-starter

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151708893.png)


上面这个是它的spring.factories文件，我们可以看到需要导入的自动配置类有两个，我们debug查看一下是否有变化

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151716666.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


可以看到数量编程了129了



### 3.2 按需开启配置项

springBoot并不是将127个全部都导入，它是按需开启的，只有导入对应的starter它才会有效，我们可以看一个案例

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151726362.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)

我们可以有@ConditionalOnClass这个注解，这个注解的意思就是，有Advice.class这个类那么AspectJAutoProxyingConfiguration类下的配置才会生效，其他的自动配置类也是类似的，需要达到条件才会开启。



### 3.3 修改默认配置

比如我们不需要springBoot默认帮我们配置好的，那么我们可以使用@Bean来配置一个我们自己的，以我们自己的优先

我们这边以字符编码拦截器为例，也就是我们的 `HttpEncodingAutoConfiguration`

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151736460.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


```java
@Configuration
public class Config {
    
    @Bean
    @ConditionalOnMissingBean
    public CharacterEncodingFilter characterEncodingFilter() {
        CharacterEncodingFilter filter = new OrderedCharacterEncodingFilter();
        filter.setEncoding("gbk");
        return filter;
    }
}
```

```java
@SpringBootApplication
@EnableConfigurationProperties(Person.class)
public class Springboot1Application {
    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(Springboot1Application.class, args);
        CharacterEncodingFilter filter = run.getBean("characterEncodingFilter", CharacterEncodingFilter.class);
        System.out.println("编码类型为：" + filter.getEncoding());   // 得到编码值
    }
}
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151746418.png)


**@ConditionalOnProperty**	
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151755628.png)
设置为false就会报错
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151803506.png)
![\[外链图片转存失败,源站可能有防盗链机制,建议将图片保存下来直接上传(img-pitAv1cS-1626505674337)(../../Java学习/截图/image-20210620185110331.png)\]](https://img-blog.csdnimg.cn/20210717151812776.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)
**自我理解**：我查看了源码，没有发现和配置文件绑定的属性，那么它就是根据配置文件的的值来判断是否开启这个功能





# 03	配置文件

## 1	文件类型

### 1.1 properties

这个没什么好说的

### 2.2 yaml

#### ①简介

YAML 是 "YAML Ain't Markup Language"（YAML 不是一种标记语言）的递归缩写。在开发的这种语言时，YAML 的意思其实是："Yet Another Markup Language"（仍是一种标记语言）。 非常适合用来做以数据为中心的配置文件



#### ②基本用法

-   key: value；kv之间有空格
-   大小写敏感

-   使用缩进表示层级关系

-   缩进的空格数不重要，只要相同层级的元素左对齐即可
-   '#'表示注释

-   字符串无需加引号，如果要加，''与""表示字符串内容 会被 转义/不转义
    -   单引号会将转义符以字符串的形式输出
    -   双引号就是转义之后输出



#### ③数据类型

-   字面量：单个的、不可再分的值。date、boolean、string、number、null

```yml
k: v
```

-   对象：键值对的集合。map、hash、set、object 

```yml
行内写法：  k: {k1:v1,k2:v2,k3:v3}
#或
k: 
	k1: v1
  k2: v2
  k3: v3
```

-   数组：一组按次序排列的值。array、list、queue

```yml
行内写法：  k: [v1,v2,v3]
#或者
k:
 - v1
 - v2
 - v3
```



#### ④示例

```java
@Data
@Component
@ConfigurationProperties(prefix = "user")
public class User {

    private String name;
    private Integer age;
    private Date birth;
    private Pet pet;
    private String[] animal;
    private List<String> hobby;
    private Map<String, Pet> map;
    private Set<Double> salary;
    private Map<String, List<Pet>> allPets;
}
```

```yml
user:
  name: 小智
  age: 18
  birth: 2001/12/12
  pet: {name: 阿猫, age: 28}
  animal:
    - 阿猫
    - 阿狗
  hobby: [敲代码, 健身]
  map:
    - 怀狗: {name: 黑鬼, age: 19}
  salary: 99.99
  all-pets:
    sick:
      - {name: heigui, age: 20}
      - {name: tom, age: 19}
    health: [{name: mario, age: 29}]
```

```java
@RestController
public class TestController {
    @Autowired
    private User user;
    @RequestMapping("/user")
    public User user () {
        return user;
    }
}
```

**结果如下**

```json
{"name":"猛男",
   "age":18,
   "birth":"2001-12-11T16:00:00.000+00:00",
   "pet":{"name":"阿猫","age":28},
   "animal":["阿猫","阿狗"],
   "hobby":["敲代码","健身"],
   "map":{"0":{"name":"黑鬼","age":19}},
   "salary":[99.99],
   "allPets":{"sick":[{"name":"heigui","age":20},
                      {"name":"tom","age":19}],
              "health":[{"name":"mario","age":29}]}}
```



## 2	配置提示

自定义的类和配置文件绑定一般没有提示

```xml
 <dependency>
     <groupId>org.springframework.boot</groupId>
     <artifactId>spring-boot-configuration-processor</artifactId>
     <optional>true</optional>
 </dependency>
```

打包的时候不要将它加入打包

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <excludes>
                    <exclude>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-configuration-processor</artifactId>
                    </exclude>
                </excludes>
            </configuration>
        </plugin>
    </plugins>
</build>
```





# 04	Web开发

## devtools热部署

导入对应的依赖

```xml
 <dependency>
     <groupId>org.springframework.boot</groupId>
     <artifactId>spring-boot-devtools</artifactId>
     <optional>true</optional>
</dependency>
```

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <fork>true</fork>
                <addResources>true</addResources>
            </configuration>
        </plugin>
    </plugins>
</build>
```

配置热部署，idea配置

![image-20230205155558610](E:\学习笔记\图片\image-20230205155558610.png)

**使用**：点击锤子或者ctrl+F9

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151854320.png)




## 一、SpringMVC自动配置概览

**官网原话**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151904730.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


翻译

Spring Boot provides auto-configuration for Spring MVC that **works well with most applications.(大多场景我们都无需自定义配置)**

The auto-configuration adds the following features on top of Spring’s defaults:

-   Inclusion of `ContentNegotiatingViewResolver` and `BeanNameViewResolver` beans.

    内容协商视图解析器和BeanName视图解析器

-   Support for serving static resources, including support for WebJars (covered [later in this document](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-spring-mvc-static-content))).

    静态资源（包括webjars）

-   Automatic registration of `Converter`, `GenericConverter`, and `Formatter` beans.

    自动注册 `Converter，GenericConverter，Formatter `

-   Support for `HttpMessageConverters` (covered [later in this document](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-spring-mvc-message-converters)).

    支持 `HttpMessageConverters` （后来我们配合内容协商理解原理）

-   Automatic registration of `MessageCodesResolver` (covered [later in this document](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-spring-message-codes)).

    自动注册 `MessageCodesResolver` （国际化用）

-   Static `index.html` support.

    静态index.html 页支持

-   Custom `Favicon` support (covered [later in this document](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-spring-mvc-favicon)).

    自定义 `Favicon`  

-   Automatic use of a `ConfigurableWebBindingInitializer` bean (covered [later in this document](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-spring-mvc-web-binding-initializer)).

    自动使用 `ConfigurableWebBindingInitializer` ，（DataBinder负责将请求数据绑定到JavaBean上）

If you want to keep those Spring Boot MVC customizations and make more [MVC customizations](https://docs.spring.io/spring/docs/5.2.9.RELEASE/spring-framework-reference/web.html#mvc) (interceptors, formatters, view controllers, and other features), you can add your own `@Configuration` class of type `WebMvcConfigurer` but **without** `@EnableWebMvc`.

**不用@EnableWebMvc注解。使用** `**@Configuration**` **+** `**WebMvcConfigurer**` **自定义规则**



If you want to provide custom instances of `RequestMappingHandlerMapping`, `RequestMappingHandlerAdapter`, or `ExceptionHandlerExceptionResolver`, and still keep the Spring Boot MVC customizations, you can declare a bean of type `WebMvcRegistrations` and use it to provide custom instances of those components.

**声明** `**WebMvcRegistrations**` **改变默认底层组件**



If you want to take complete control of Spring MVC, you can add your own `@Configuration` annotated with `@EnableWebMvc`, or alternatively add your own `@Configuration`-annotated `DelegatingWebMvcConfiguration` as described in the Javadoc of `@EnableWebMvc`.

**使用** `**@EnableWebMvc+@Configuration+DelegatingWebMvcConfiguration 全面接管SpringMVC**`





## 二、简单功能分析

### 1.静态资源访问

#### ①静态资源目录

![](https://img-blog.csdnimg.cn/20210717151916917.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


静态资源默认放在/static、/public、/resources、/META-INF/resources目录下都可以访问到

访问路径：当前项目路径 + 资源名

**注意**：如果controller中有和资源名一样的访问路径，先给controller进行处理，如果controller不能处理，才到静态资源管理器来进行处理

在static目录下放入名为static.jpg的资源

```java
@RestController
public class TestController {
    
    @RequestMapping("/static.jpg")
    public String str () {
        return "hello";
    }
}
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151930242.png)


可以看到我们访问的是controller



#### ②静态资源访问前缀

默认无前缀

从官方文档中我们知道默认路径是`/**`，我们可以通过`spring.mvc.static-path-pattern`属性来修改默认的映射路径

```yml
spring:
  mvc:
    static-path-pattern: /res/**
```

**测试**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151943239.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)

**注意**：我们改变默认映射路径后会引发`欢迎页`支持和`自定义Favicon`使用不了，在原理讲解的时候会知道为什么用不了



**我们还可以修改默认的目录**

```yml
  web:
    resources:
      static-locations: [classpath:/xiaozhi/]
```
设置这个之后我们的默认目录就不能使用了，只能使用我们自定义的
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151952528.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717151959670.png)
#### ③webjar

自动映射到/webjars/**

https://www.webjars.org/

```xml
 <dependency>
     <groupId>org.webjars</groupId>
     <artifactId>jquery</artifactId>
     <version>3.5.1</version>
</dependency>
```

**访问地址**：http://localhost:8080/webjars/jquery/3.5.1/jquery.js





### 2.欢迎页支持

在静态资源目录下创建index.html文件，springBoot会自动映射

**注意**：如果自定义了资源映射路径，那么就不起作用了，修改默认资源路径不影响欢迎页支持



### 3.自定义Favicon

favicon.ico 放在静态资源目录下即可。



### 4.静态资源配置原理

**我们的自动配置都在xxxAutoConfiguration 类 （自动配置类）中**

这个是跟web功能相关的，我们去WebMvcAutoConfiguration查找相应的配置

```java
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class, WebMvcConfigurer.class })
@ConditionalOnMissingBean(WebMvcConfigurationSupport.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 10)
@AutoConfigureAfter({ DispatcherServletAutoConfiguration.class, TaskExecutionAutoConfiguration.class,
		ValidationAutoConfiguration.class })
public class WebMvcAutoConfiguration {
```

`@ConditionalOnMissingBean(WebMvcConfigurationSupport.class)`：没有WebMvcConfigurationSupport类型的bean的时候才生效，也就是说我们可以完全自定义。



**给容器配置了什么**

```java
@Configuration(proxyBeanMethods = false)	// 配置类
@Import(EnableWebMvcConfiguration.class)	// 导入对应的组件
@EnableConfigurationProperties({ WebMvcProperties.class,	
      org.springframework.boot.autoconfigure.web.ResourceProperties.class, WebProperties.class })
@Order(0)
public static class WebMvcAutoConfigurationAdapter implements WebMvcConfigurer, ServletContextAware {
```

配置文件的相关属性和配置文件进行了绑定：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152023116.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152030655.png)
![\[外链图片转存失败,源站可能有防盗链机制,建议将图片保存下来直接上传(img-EmK7baXk-1626505674342)(../../Java学习/截图/image-20210621162829287.png)\]](https://img-blog.csdnimg.cn/20210717152037780.png)




#### ①配置类中只有一个有参构造

// 有参构造所有参数的值都会从容器中找

```java
// resourceProperties	获取配置文件中绑定的值
// webProperties		获取配置文件中绑定的值
// mvcProperties		获取配置文件中绑定的值
// beanFactory			spring的beanFactory
// messageConvertersProvider	找到所有的HttpMessageConverters
// resourceHandlerRegistrationCustomizerProvider	找到资源处理器的自定义器
// dispatcherServletPath
// servletRegistrations		给应用注册Servlet、Filter....
public WebMvcAutoConfigurationAdapter(
    org.springframework.boot.autoconfigure.web.ResourceProperties resourceProperties,
    WebProperties webProperties, 
    WebMvcProperties mvcProperties, 
    ListableBeanFactory beanFactory,
    ObjectProvider<HttpMessageConverters> messageConvertersProvider,
    ObjectProvider<ResourceHandlerRegistrationCustomizer> resourceHandlerRegistrationCustomizerProvider,
    ObjectProvider<DispatcherServletPath> dispatcherServletPath,
    ObjectProvider<ServletRegistrationBean<?>> servletRegistrations) {
    this.resourceProperties = resourceProperties.hasBeenCustomized() ? resourceProperties
        : webProperties.getResources();
    this.mvcProperties = mvcProperties;
    this.beanFactory = beanFactory;
    this.messageConvertersProvider = messageConvertersProvider;
    this.resourceHandlerRegistrationCustomizer = resourceHandlerRegistrationCustomizerProvider.getIfAvailable();
    this.dispatcherServletPath = dispatcherServletPath;
    this.servletRegistrations = servletRegistrations;
    this.mvcProperties.checkConfiguration();
}
```



#### ②资源处理的默认规则

```java
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
   if (!this.resourceProperties.isAddMappings()) {
      logger.debug("Default resource handling disabled");
      return;
   }
   addResourceHandler(registry, "/webjars/**", "classpath:/META-INF/resources/webjars/");
   addResourceHandler(registry, this.mvcProperties.getStaticPathPattern(), (registration) -> {
      registration.addResourceLocations(this.resourceProperties.getStaticLocations());
      if (this.servletContext != null) {
         ServletContextResource resource = new ServletContextResource(this.servletContext, SERVLET_LOCATION);
         registration.addResourceLocations(resource);
      }
   });
}
```

这个就是处理我们静态资源的方法

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155913222.png)

==也就是说我们把这个值调为true，这个方法就不会执行，也就是禁用了默认的规则==

```yml
spring:
#  mvc:
  #    static-path-pattern: /res/**
  web:
    resources:
      add-mappings: false	# 禁用所有静态资源规则
```

dubug进行调试，结果和我们想的一样，它直接结束了方法

我们看一下默认的静态资源规则的运行原理

```java
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    if (!this.resourceProperties.isAddMappings()) {
        logger.debug("Default resource handling disabled");
        return;
    }
    addResourceHandler(registry, "/webjars/**", "classpath:/META-INF/resources/webjars/");
    addResourceHandler(registry, this.mvcProperties.getStaticPathPattern(), (registration) -> {
        registration.addResourceLocations(this.resourceProperties.getStaticLocations());
        if (this.servletContext != null) {
            ServletContextResource resource = new ServletContextResource(this.servletContext, SERVLET_LOCATION);
            registration.addResourceLocations(resource);
        }
    });
}
```

执行第一个`addResourceHandler`方法就是设置webjars的访问路径和映射路径

![在这里插入图片描述](https://img-blog.csdnimg.cn/2021071715205443.png)


第二个`addResourceHandler`方法就是设置我们其他的静态资源路径，可以看到有两个属性是绑定了我们的配置文件，我们可以debug来查看一下对应的值

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152105878.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152115444.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)
我们现在知道了我们默认的访问路径是在WebProperties类下的内部类Resources中的属性
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152129617.png)
![](https://img-blog.csdnimg.cn/20210717152136937.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)

我们再回到我们的配置文件
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152153456.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)



==**拓展一下**==

```java
private void addResourceHandler(ResourceHandlerRegistry registry, String pattern,
      Consumer<ResourceHandlerRegistration> customizer) {
   if (registry.hasMappingForPattern(pattern)) {
      return;
   }
   ResourceHandlerRegistration registration = registry.addResourceHandler(pattern);
   customizer.accept(registration);
   registration.setCachePeriod(getSeconds(this.resourceProperties.getCache().getPeriod()));
   registration.setCacheControl(this.resourceProperties.getCache().getCachecontrol().toHttpCacheControl());
   registration.setUseLastModified(this.resourceProperties.getCache().isUseLastModified());
   customizeResourceHandlerRegistration(registration);
}
```

在这个方法中有三个set方法，这个就是可以设置静态资源的信息，也都是绑定了我们的配置文件，我们可以通过配置文件来修改对应的值

```java
// 缓存时间，以s为单位
registration.setCachePeriod(getSeconds(this.resourceProperties.getCache().getPeriod()));
// 缓存控制器
registration.setCacheControl(this.resourceProperties.getCache().getCachecontrol().toHttpCacheControl());
// 是否设置文件(也就是静态资源文件)最后修改的日期和时间 
registration.setUseLastModified(this.resourceProperties.getCache().isUseLastModified());
```

我们来验证我们的假设，在配置文件中看看是否存在
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152201895.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)
我们到浏览器查看一下是否有修改
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152211234.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)
可以看到确实是可以的！



#### ③欢迎页处理规则

```java
@Bean
public WelcomePageHandlerMapping welcomePageHandlerMapping(ApplicationContext applicationContext,
                                                           FormattingConversionService mvcConversionService, ResourceUrlProvider mvcResourceUrlProvider) {
    WelcomePageHandlerMapping welcomePageHandlerMapping = new WelcomePageHandlerMapping(
        new TemplateAvailabilityProviders(applicationContext), applicationContext, getWelcomePage(),
        this.mvcProperties.getStaticPathPattern());
    welcomePageHandlerMapping.setInterceptors(getInterceptors(mvcConversionService, mvcResourceUrlProvider));
    welcomePageHandlerMapping.setCorsConfigurations(getCorsConfigurations());
    return welcomePageHandlerMapping;
}
```

构造器中进行路径校验

```java
WelcomePageHandlerMapping(TemplateAvailabilityProviders templateAvailabilityProviders,
			ApplicationContext applicationContext, Resource welcomePage, String staticPathPattern) {
    if (welcomePage != null && "/**".equals(staticPathPattern)) {
        // 满足条件，使用欢迎页
        logger.info("Adding welcome page: " + welcomePage);
        setRootViewName("forward:index.html");
    }
    else if (welcomeTemplateExists(templateAvailabilityProviders, applicationContext)) {
        // 调用controller		/index
        logger.info("Adding welcome page template: index");
        setRootViewName("index");
    }
}
```

判断是不是在/**路径下的，如果你设置了static-locations属性它也是相当于映射到根目录下的，所以当我们设置路径前缀的时候，欢迎页就会使用不了。只有index.html在 `/**`下才能使用欢迎页



#### ④Favicon

服务器默认是将/**路径下的图标发送给浏览器





## 三、请求参数处理

### 1.请求映射

#### ①rest使用

-   格式：@xxxMapping;
-   rest风格支持（使用HTTP请求方式动词来表示对资源的操作）
    -   *以前：**/getUser*  *获取用户*    */deleteUser* *删除用户*   */editUser*  *修改用户*      */saveUser* *保存用户*
    -   *现在： /user*    *GET-**获取用户*    *DELETE-**删除用户*     *PUT-**修改用户*      *POST-**保存用户*
-   使用：表单使用post请求提交，hidden隐藏域中_method=put
-   需要我们手动开启rest风格



**代码实现**

html页面

```html
<form action="/test" method="post">
    <input type="submit" value="Post">
</form>
<form action="/test" method="get">
    <input type="submit" value="get">
</form>
<form action="/test" method="post">
    <input type="hidden" name="_method" value="put">
    <input type="submit" value="put">
</form>
<form action="/test" method="post">
    <input type="hidden" name="_method" value="delete">
    <input type="submit" value="delete">
</form>
```

java代码

```java
@RestController
public class TestController {
    @PostMapping("test")
    public String post() {
        return "post";
    }
    @GetMapping("test")
    public String get() {
        return "get";
    }
    @PutMapping("test")
    public String put() {
        return "put";
    }
    @DeleteMapping("test")
    public String delete() {
        return "delete";
    }
}
```

我们还要手动开启rest风格，不开启的话一律当Post请求处理

```yml
spring:
  mvc:
    hiddenmethod:
      filter:
        enabled: true
```



#### ②rest原理解析

在我们的WebMvcAutoConfiguration类下

```java
@Bean
@ConditionalOnMissingBean(HiddenHttpMethodFilter.class)
@ConditionalOnProperty(prefix = "spring.mvc.hiddenmethod.filter", name = "enabled")
public OrderedHiddenHttpMethodFilter hiddenHttpMethodFilter() {
   return new OrderedHiddenHttpMethodFilter();
}
```

我们看到它绑定了配置文件，官方默认是关闭的，所以我们需要手动开启

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152234646.png)


创建了一个OrderedHiddenHttpMethodFilter对象，它又实现了OrderedFilter，而OrderedFilter又是继承了Filter接口，所以我们它就是一个拦截器，我们通过源码来看一下它的功能是什么

在`HiddenHttpMethodFilter`类下有一个方法，这个方法就是它的核心方法

```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

   HttpServletRequest requestToUse = request;
	// 判断是否是post请求类型，没有发生异常
   if ("POST".equals(request.getMethod()) && request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) == null) {
      // 默认是得到我们隐藏域_method中的参数
      String paramValue = request.getParameter(this.methodParam);
      if (StringUtils.hasLength(paramValue)) {
          // 将得到的值转换成大写的
         String method = paramValue.toUpperCase(Locale.ENGLISH);
          // 判断是否有这个类型的方法
         if (ALLOWED_METHODS.contains(method)) {
             // 将得到的方法值进行包装，返回一个HttpServletRequestWrapper对象
            requestToUse = new HttpMethodRequestWrapper(request, method);
         }
      }
   }
    // 过滤器放行，我们用的request对象是我们修改过的，getMethod方法得到的就是我们传入的值
   filterChain.doFilter(requestToUse, response);
}
```

**详细展开说明**

我们调用`this.methodParam`就是DEFAULT_METHOD_PARAM常量

```java
public static final String DEFAULT_METHOD_PARAM = "_method";
private String methodParam = DEFAULT_METHOD_PARAM;
```

`ALLOWED_METHODS.contains(method)`的意思是判断是否包含下面其中的一个

```java
private static final List<String> ALLOWED_METHODS =
			Collections.unmodifiableList(Arrays.asList(HttpMethod.PUT.name(),
					HttpMethod.DELETE.name(), HttpMethod.PATCH.name()));
```

创建`HttpMethodRequestWrapper`对象进行返回

```JAVA
private static class HttpMethodRequestWrapper extends HttpServletRequestWrapper {

   private final String method;

   public HttpMethodRequestWrapper(HttpServletRequest request, String method) {
      super(request);
      this.method = method;
   }

   @Override
   public String getMethod() {
      return this.method;
   }
}
```

它的作用就是将method设为我们参数值，然后交给我们的过滤器链放行，我们之后使用getMethod()方法得到的就是我们传入的值



#### ③拓展:修改默认的隐藏域名

我们可以自己注册一个`HiddenHttpMethodFilter` 组件来设置我们想要的名字

```java
@Configuration
public class MyConfig {
    @Bean
    public HiddenHttpMethodFilter hiddenHttpMethodFilter(){
        HiddenHttpMethodFilter hiddenHttpMethodFilter = new HiddenHttpMethodFilter();
        hiddenHttpMethodFilter.setMethodParam("_h");
        return hiddenHttpMethodFilter;
    }
}
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152246500.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)






### 2.请求映射原理  

`DispatcherServlet`是处理我们请求映射的，下面是它的结构树

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152255215.png)


它是一个servlet程序，那么就会有`doGet()`和`doPost()`方法，我们最终的请求会执行这两个方法

`HttpServletBean`中并没有这两个方法，我们接着往下找，在FrameworkServlet找到了这两个方法

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152303468.png)


执行这个方法，我们点进去查看一下

processRequest(request, response);方法中有一个核心方法

```java
doService(request, response);
```

这个方法的具体实现是在DispatcherServlet类中

```java
@Override
	protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
```

在这个方法中还有一个核心方法，这个方法才是我们的具体实现

```java
try {
	doDispatch(request, response);
}
```

最终调用这个核心方法

```java
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152312809.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


我们可以debug计算一下得到的值

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152321686.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


这个方法的作用就是得到处理这个请求的是哪个方法

我们再点进去看一下getHandler(processedRequest)方法的具体实现

```java
@Nullable
protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
    if (this.handlerMappings != null) {
        // 将映射信息进行遍历
        for (HandlerMapping mapping : this.handlerMappings) {
            // 判断映射处理器是否可以处理当前请求，可以就返回HandlerExecutionChain对象，不能就返回null
            HandlerExecutionChain handler = mapping.getHandler(request);
            if (handler != null) {
                return handler;
            }
        }
    }
    return null;
}
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152330735.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)

**==这些映射信息是springBoot在启动的时候就已经帮我们封装到HandlerMapping中了==**

**`RequestMappingHandlerMapping`是标注了@RequestMapping 注解的方法的映射**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152340834.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)

**`WelcomePageHandlerMapping`是首页的映射**

![](https://img-blog.csdnimg.cn/20210717152347287.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)



**==继续往下探究，我们看一下`mapping.getHandler(request);`方法做了那些事==**

```java
// 调用了下面这个方法
getHandlerInternal(request);
// 方法返回下面执行的方法
return super.getHandlerInternal(request);
// 调用下面的方法得到返回的对象值
HandlerMethod handlerMethod = lookupHandlerMethod(lookupPath, request);
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152358199.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


```java
@Nullable
protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {
    List<Match> matches = new ArrayList<>();
    // 得到匹配的路径
    List<T> directPathMatches = this.mappingRegistry.getMappingsByDirectPath(lookupPath);
    if (directPathMatches != null) {
        // 得到最匹配的那个，然后放入到集合中
        addMatchingMappings(directPathMatches, matches, request);
    }
    if (matches.isEmpty()) {
        addMatchingMappings(this.mappingRegistry.getRegistrations().keySet(), matches, request);
    }
    if (!matches.isEmpty()) {
        // 得到集合中的第一个值
        Match bestMatch = matches.get(0);
        // 如果有多个请求匹配，它会报异常
        if (matches.size() > 1) {
            Comparator<Match> comparator = new MatchComparator(getMappingComparator(request));
```



### 3.参数与基本注解

#### 3.1 注解

requile属性是判断这个参数是不是必须的，false表示这个参数不是必须的

@PathVariable、@RequestHeader、@ModelAttribute、@RequestParam、@MatrixVariable、@CookieValue、@RequestBody



##### ①@PathVariable

得到请求路径变量的参数值

```java
@GetMapping("/test/{name}/{age}")
public Map<String, Object> pathVariable(@PathVariable String name,
                                        @PathVariable Integer age,
                                        @PathVariable Map<String, String> paramMap) {
    // 还可以用map<String, String>接收值
    Map<String, Object> map = new HashMap<>();
    map.put("name", name);
    map.put("age", age);
    map.put("paramMap", paramMap);
    return map; 
}
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152408710.png)



##### ②@RequestHeader

得到请求头参数的值

```java
@GetMapping("/test/requestHeader")
public Map<String, Object> requestHeader(@RequestHeader("Accept") String accept,
                                         @RequestHeader Map<String, String> paramMap ) {
    // 使用map集合接收就是全部的请求头信息
    Map<String, Object> map = new HashMap<>();
    map.put("paramMap", paramMap);
    map.put("accept", accept);
    return map;
}
```



##### ③@ModelAttribute

获取请求域中的值

```java
    @GetMapping("/test/request")
    public String request(HttpServletRequest request) {
        request.setAttribute("name","小智");
        request.setAttribute("age", 18);
        return "forward:/test/modelAttribute";
    }

    @ResponseBody
    @GetMapping("/test/modelAttribute")
    public Map<String, Object> modelAttribute(@ModelAttribute("name") String name,
                                              @ModelAttribute("age") String age) {
        // 不能使用map集合去接收参数，参数接收的类型一定是String类型的，不然会报错
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("age", age);
        return map;
    }
```



##### ④@RequestParam

得到请求参数值

```java
@GetMapping("/test/requestParam")
public Map<String, Object> requestParam(@RequestParam("name") String name,
                                        @RequestParam("age") String age,
                                        @RequestParam Map<String, String> paramMap) {
    // 可以用map<String, String>接收值
    Map<String, Object> map = new HashMap<>();
    map.put("name", name);
    map.put("age", age);
    map.put("paramMap", paramMap);
    return map;
}
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152416974.png)






##### ⑤@CookieValue

得到cookie的值

```java
@GetMapping("/test/cookie")
public Map<String, Object> requestParam(@CookieValue("Idea-4a5d61e4") String value) {
    // 可以用map<String, String>接收值
    Map<String, Object> map = new HashMap<>();
    map.put("value", value);
    return map;     // {"value":"b6df2f01-c4d7-4866-a9bf-536b81d7af1f"}
}
```



##### ⑥@RequestBody

获得请求体的参数，Post请求才有请求体

```java
@PostMapping("/test/responseBody")
public Map<String, Object> responseBody(@RequestBody String user) {
    // 可以用map<String, String>接收值
    Map<String, Object> map = new HashMap<>();
    map.put("user", user);
    return map;
}
```

```html
<form action="/test/responseBody" method="post">
    账号<input type="text" name="name">
    密码<input type="text" name="psw">
    <input type="submit" value="Post">
</form>
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152425458.png)




##### ⑦@MatrixVariable

得到路径中矩阵变量

**语法**："；"后面的参数		

eg：/cars/sell;low=34;brand=byd,audi,yd		"；"后面的内容就是我们得到的参数值

-   springBoot默认是关闭的，我们需要手动开启，我们源码探究它做了什么
    -   第一种开启方式：实现WebMvcConfigurer，因为处理矩形变量的方法是在这个类下的
    -   使用@Bean注解直接将WebMvcConfigurer注入到容器中

```java
@Override
// 配置路径匹配
public void configurePathMatch(PathMatchConfigurer configurer) {
   if (this.mvcProperties.getPathmatch()
         .getMatchingStrategy() == WebMvcProperties.MatchingStrategy.PATH_PATTERN_PARSER) {
      configurer.setPatternParser(new PathPatternParser());
   }
   configurer.setUseSuffixPatternMatch(this.mvcProperties.getPathmatch().isUseSuffixPattern());
   configurer.setUseRegisteredSuffixPatternMatch(
         this.mvcProperties.getPathmatch().isUseRegisteredSuffixPattern());
   this.dispatcherServletPath.ifAvailable((dispatcherPath) -> {
      String servletUrlMapping = dispatcherPath.getServletUrlMapping();
      if (servletUrlMapping.equals("/") && singleDispatcherServlet()) {
         // UrlPathHelper类就是对针对URL进行处理
         UrlPathHelper urlPathHelper = new UrlPathHelper();
         urlPathHelper.setAlwaysUseFullPath(true);
         configurer.setUrlPathHelper(urlPathHelper);
      }
   });
}
```

-   UrlPathHelper类下`removeSemicolonContent`属性就是判断是否移除路径中分号(;)后面的的内容

    -   true：就是移除
    -   false：不移除，开启矩形变量功能

    ```java
    /**
     * Whether configured to remove ";" (semicolon) content from the request URI.
       是否配置为删除“；”(请求URI中的内容。
     */
    public boolean shouldRemoveSemicolonContent() {
       checkReadOnly();
       return this.removeSemicolonContent;
    }
    ```

**代码实现**

**开启**

```java
@Configuration
public class MyConfig implements WebMvcConfigurer {
    // 第一种方式
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        UrlPathHelper urlPathHelper = new UrlPathHelper();
        urlPathHelper.setRemoveSemicolonContent(false);
        configurer.setUrlPathHelper(urlPathHelper);
    }

    // 第二种方式
    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void configurePathMatch(PathMatchConfigurer configurer) {
                UrlPathHelper urlPathHelper = new UrlPathHelper();
                urlPathHelper.setRemoveSemicolonContent(false);
                configurer.setUrlPathHelper(urlPathHelper);
            }
        };
    }
}
```

```java
@GetMapping("/test/{path}")
// 必须是在路径变量的后面接上
public Map<String, Object> matrixVariable(@MatrixVariable("cat") String cat,
                                          @MatrixVariable("dog") String dog,
                                          @PathVariable("path") String path) {
    Map<String, Object> map = new HashMap<>();
    map.put("cat", cat);
    map.put("dog", dog);
    map.put("path", path);
    return map;
}

    // 获取两个不同的路径变量后面相同的矩形变量值
    // 使用pathVar属性区分是获取哪一个路径变量后面的值
    @GetMapping("/boss/{bossId}/{empId}")
    public Map<String, Object> matrixVariable2(
        @MatrixVariable(value = "age",pathVar = "bossId") String bossAge,
        @MatrixVariable(value = "age",pathVar = "empId") String empAge,
        @PathVariable("bossId") String bossId,
        @PathVariable("empId") String empId) {
        
        Map<String, Object> map = new HashMap<>();
        map.put("bossAge", bossAge);
        map.put("empAge", empAge);
        map.put("bossId", bossId);
        map.put("empId", empId);
        return map;
    }
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152437259.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152445369.png)


==**发现一个坑**：`@GetMapping("/test/{bossId}/{empId}")`和`@GetMapping("/test/{path}")`两个同时存在会报错，我们修改一下第一个路径为`/boss/{bossId}/{empId}`就又可以正常访问了，如果非要使用/test前缀的话也可以在两个路径变量中间再加一个值，例如：`/test/{bossId}/test/{empId}`即可==





#### 3.2 注解原理

```java
@GetMapping("/test/RequestHeader")
public Map<String, Object> RequestHeader(@RequestHeader("Accept") String accept,
                                         @RequestHeader Map<String, String> paramMap ) {
    // 使用map集合接收就是全部的请求头信息
    Map<String, Object> map = new HashMap<>();
    map.put("paramMap", paramMap);
    map.put("accept", accept);
    return map;
}
```

我们之前说过mappedHandler是映射处理器，找到对应处理请求的方法，将方法的信息进行封装

```java
HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
```

-   HandlerAdapter（处理程序适配器）是一个接口
    -   supports方法判断有没有分解器可以处理这个参数，有就true
    -   handle方法处理

调用HandlerAdapter中的handle()方法来执行方法，debug看它做了什么

```java
public final ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {

   return handleInternal(request, response, (HandlerMethod) handler);
}
```

调用方法得到返回值

```java
mav = invokeHandlerMethod(request, response, handlerMethod);	// 核心方法
// invokeHandlerMethod中的核心方法
invocableMethod.invokeAndHandle(webRequest, mavContainer);
// 最终来到真正执行方法的方法
Object returnValue = invokeForRequest(webRequest, mavContainer, providedArgs);
// invokeForRequest方法中的核心方法，得到参数值
Object[] args = getMethodArgumentValues(request, mavContainer, providedArgs);
// 这个方法它是在一个循环里面的，循环的次数就是我们参数的个数，它会匹配能解析这个参数的参数解析器，然后进行解析得到匹配的值
args[i] = this.resolvers.resolveArgument(parameter, mavContainer, request, this.dataBinderFactory);
```

往下执行这个方法

```java
@Nullable
private HandlerMethodArgumentResolver getArgumentResolver(MethodParameter parameter) {
    // 得到缓存中的参数解析器
   HandlerMethodArgumentResolver result = this.argumentResolverCache.get(parameter);
    // 第一次为空
   if (result == null) {
      for (HandlerMethodArgumentResolver resolver : this.argumentResolvers) {
          // 遍历能够处理方法参数的参数解析器
         if (resolver.supportsParameter(parameter)) {
             // 可以处理，那么就结束
            result = resolver;
             // 放入到缓存中，下次就可以直接使用缓存中的参数解析器了
            this.argumentResolverCache.put(parameter, result);
            break;
         }
      }
   }
   return result;
}
```

![\[外链图片转存失败,源站可能有防盗链机制,建议将图片保存下来直接上传(img-5sLxcXVU-1626505674356)(../../Java学习/截图/image-20210623214257091.png)\]](https://img-blog.csdnimg.cn/20210717152456537.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)
每个参数解析器都有对应的规则，这个可以debug进行查看它的执行规则
![](https://img-blog.csdnimg.cn/20210717152509646.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)
得到参数后就可以执行方法了

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152525138.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)






#### 4.ServletApi

WebRequest、ServletRequest、MultipartRequest、 HttpSession、javax.servlet.http.PushBuilder、Principal、InputStream、Reader、HttpMethod、Locale、TimeZone、ZoneId

```java
@Override
  public boolean supportsParameter(MethodParameter parameter) {
    Class<?> paramType = parameter.getParameterType();
    return (WebRequest.class.isAssignableFrom(paramType) ||
        ServletRequest.class.isAssignableFrom(paramType) ||
        MultipartRequest.class.isAssignableFrom(paramType) ||
        HttpSession.class.isAssignableFrom(paramType) ||
        (pushBuilder != null && pushBuilder.isAssignableFrom(paramType)) ||
        Principal.class.isAssignableFrom(paramType) ||
        InputStream.class.isAssignableFrom(paramType) ||
        Reader.class.isAssignableFrom(paramType) ||
        HttpMethod.class == paramType ||
        Locale.class == paramType ||
        TimeZone.class == paramType ||
        ZoneId.class == paramType);
  }
```



#### 5.复杂参数

**Map**、**Model（map、model里面的数据会被放在request的请求域  request.setAttribute）、**Errors/BindingResult、**RedirectAttributes（ 重定向携带数据）**、**ServletResponse（response）**、SessionStatus、UriComponentsBuilder、ServletUriComponentsBuilder

```java
Map<String,Object> map,  Model model, HttpServletRequest request 都是可以给request域中放数据request.getAttribute();
```







#### 6.自定义类型参数 封装POJO

将数据封装到pojo中

**ServletModelAttributeMethodProcessor  这个参数处理器支持**

 **是否为简单类型。**

```java
public static boolean isSimpleValueType(Class<?> type) {
    return (Void.class != type && void.class != type &&
        (ClassUtils.isPrimitiveOrWrapper(type) ||
        Enum.class.isAssignableFrom(type) ||
        CharSequence.class.isAssignableFrom(type) ||
        Number.class.isAssignableFrom(type) ||
        Date.class.isAssignableFrom(type) ||
        Temporal.class.isAssignableFrom(type) ||
        URI.class == type ||
        URL.class == type ||
        Locale.class == type ||
        Class.class == type));
  }
```

我们来看看它是怎么将我们的数据封装到我们自定义的对象中的

```java
public final Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

   Assert.state(mavContainer != null, "ModelAttributeMethodProcessor requires ModelAndViewContainer");
   Assert.state(binderFactory != null, "ModelAttributeMethodProcessor requires WebDataBinderFactory");

   String name = ModelFactory.getNameForParameter(parameter);
   ModelAttribute ann = parameter.getParameterAnnotation(ModelAttribute.class);
   if (ann != null) {
      mavContainer.setBinding(name, ann.binding());
   }

   Object attribute = null;
   BindingResult bindingResult = null;

   if (mavContainer.containsAttribute(name)) {
      attribute = mavContainer.getModel().get(name);
   }
   else {
      // Create attribute instance
      try {
         attribute = createAttribute(name, parameter, binderFactory, webRequest);
      }
      catch (BindException ex) {
         if (isBindExceptionRequired(parameter)) {
            // No BindingResult parameter -> fail with BindException
            throw ex;
         }
         // Otherwise, expose null/empty value and associated BindingResult
         if (parameter.getParameterType() == Optional.class) {
            attribute = Optional.empty();
         }
         else {
            attribute = ex.getTarget();
         }
         bindingResult = ex.getBindingResult();
      }
   }

   if (bindingResult == null) {
      // Bean property binding and validation;
      // skipped in case of binding failure on construction.
      WebDataBinder binder = binderFactory.createBinder(webRequest, attribute, name);
      if (binder.getTarget() != null) {
         if (!mavContainer.isBindingDisabled(name)) {
            bindRequestParameters(binder, webRequest);
         }
         validateIfApplicable(binder, parameter);
         if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
            throw new BindException(binder.getBindingResult());
         }
      }
      // Value type adaptation, also covering java.util.Optional
      if (!parameter.getParameterType().isInstance(attribute)) {
         attribute = binder.convertIfNecessary(binder.getTarget(), parameter.getParameterType(), parameter);
      }
      bindingResult = binder.getBindingResult();
   }

   // Add resolved attribute and BindingResult at the end of the model
   Map<String, Object> bindingResultModel = bindingResult.getModel();
   mavContainer.removeAttributes(bindingResultModel);
   mavContainer.addAllAttributes(bindingResultModel);

   return attribute;
}
```

这个就是它的核心处理方法了！

**WebDataBinder binder = binderFactory.createBinder(webRequest, attribute, name);**

WebDataBinder 数据绑定器，将请求参数的值绑定到指定的javaBean中

bindRequestParameters(binder, webRequest);这个才是真正的将数据设置给javaBean

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152540303.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


我们也可以自定义自己的converters

@FunctionalInterface**public interface** Converter<S, T>	函数式接口，String类型转换成我们对象类型



#### 7.自定义converters

**第一种方式**：在WebMvcConfigurer中添加转换器

```java
    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addFormatters(FormatterRegistry registry) {
                registry.addConverter(new Converter<String, Person>() {
                    @Override
                    public Person convert(String source) {
                        Person person = new Person();
                        // 定义自己的规则
                        String[] str = source.split(",");
                        person.setName(str[0]);
                        person.setAge(Integer.parseInt(str[1]));
                        return person;
                    }
                });
            }
        };
    }
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152549308.png)



**第二种方式**：直接创建一个转换器的bean

1.  实现Converter<c1, c2>接口，c1为前端接收类型，c2为参数类型
2.  重写convert方法，里面写转换的逻辑

```java
/**
 * 请求路径url中的参数进行时间日期类型的转换，字符串->日期Date
 */
@Configuration
public class DateConverterConfig implements Converter<String, Date> {

    private static final List<String> formatterList = new ArrayList<>(4);
    static{
        formatterList.add("yyyy-MM");
        formatterList.add("yyyy-MM-dd");
        formatterList.add("yyyy-MM-dd hh:mm");
        formatterList.add("yyyy-MM-dd hh:mm:ss");
    }

    @Override
    public Date convert(String source) {
        String value = source.trim();
        if ("".equals(value)) {
            return null;
        }
        if(source.matches("^\\d{4}-\\d{1,2}$")){
            return parseDate(source, formatterList.get(0));
        }else if(source.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$")){
            return parseDate(source, formatterList.get(1));
        }else if(source.matches("^\\d{4}-\\d{1,2}-\\d{1,2} {1}\\d{1,2}:\\d{1,2}$")){
            return parseDate(source, formatterList.get(2));
        }else if(source.matches("^\\d{4}-\\d{1,2}-\\d{1,2} {1}\\d{1,2}:\\d{1,2}:\\d{1,2}$")){
            return parseDate(source, formatterList.get(3));
        }else {
            GraceException.display(ResponseStatusEnum.SYSTEM_DATE_PARSER_ERROR);
        }
        return null;
    }

    /**
     * 日期转换方法
     * @param dateStr
     * @param formatter
     * @return
     */
    public Date parseDate(String dateStr, String formatter) {
        Date date=null;
        try {
            DateFormat dateFormat = new SimpleDateFormat(formatter);
            date = dateFormat.parse(dateStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date;
    }
}
```





### 4.参数校验

#### 3.1 引入参数校验的start

 spring-boot-starter-web 会⼀并依赖spring-boot-starter-validation 到项⽬中，所以引入web场景的话就不需要再引入



#### 3.2 接收参数的BO或DTO

这里我以登录为例子，接收用户名和密码

```java
public class UserDto {
    
    @NotBlank(message = "用戶名不能為空")
    private String username;
    
    @NotBlank(message = "用戶名不能為空")
    private String password;
    
}
```

在controller中使用`@Valid`或`@Validated`来标注需要进行参数校验的参数

```java
public String test(@RequestBody @Valid UserDto userDto) {}
```

然后通过全局异常捕获进行处理，这样我们就不需要进行多余的判断了





#### 3.3 校验注解的属性

在`@NotBlank`中有三个属性

**message**

这个属性是参数校验不通过抛出的异常信息



**groups**

数组类型的参数，可以放入多个校验类，根据不同的功能去调用不同的校验方法

比如一个参数在插入的时候需要插入判断，在更新的时候需要进行更新判断，那么这个时候就需要我们写两个不同的校验类，对应不同的参数进行不同的校验

```java
@NotNull(message = "id不能为空", groups = {Insert.class, update.class})
private Long id;
```

```java
public void test(@Valid(Insert.class) User user) {
}
```



#### 3.4 自定义参数校验注解

```java
// 校验注解
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
// 标注这个注解就是表示它是一个参数校验注解，同时需要告诉它由哪个校验类来进行校验
@Constraint(validatedBy = CheckUrlValidate.class)
public @interface CheckUrl {

    /**
     * 校验不通过信息
     */
    String message() default "Url不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
```

```java
// 校验类，需要实现ConstraintValidator<校验注解, 校验类型>
public class CheckUrlValidate implements ConstraintValidator<CheckUrl, String> {

    // 在这个方法中写入校验逻辑true表示通过，false表示不通过
    @Override
    public boolean isValid(String url, ConstraintValidatorContext context) {
        return UrlUtil.verifyUrl(url.trim());
    }
}
```







## 四、数据响应和内容协商

### 1.响应json

#### 1、jackson + @Response注解

引入对应的依赖

web场景已经帮我们引入了json场景

上面我们说了我们参数处理器的原理，那么接下来就是返回值处理器了，看一下我们底层做了什么转换
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152558454.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)




##### ①返回值处理器

```java
// doDispatch中的核心方法
mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
// 往下调方法
```

```java
// 处理返回值的核心方法
// public void invokeAndHandle
this.returnValueHandlers.handleReturnValue(
					returnValue, getReturnValueType(returnValue), mavContainer, webRequest);
```

```java
@Override
public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
      ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
	// 方法返回值处理器
   HandlerMethodReturnValueHandler handler = selectHandler(returnValue, returnType);
   if (handler == null) {
      throw new IllegalArgumentException("Unknown return value type: " + returnType.getParameterType().getName());
   }
    // 调用处理方法
   handler.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
}
```

```java
@Override
public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
      ModelAndViewContainer mavContainer, NativeWebRequest webRequest)
      throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {
	
   mavContainer.setRequestHandled(true);
   ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
   ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);

   // 使用消息转换器进行写出，最终写到response中
   writeWithMessageConverters(returnValue, returnType, inputMessage, outputMessage);
}
```



##### ②返回值处理器原理

**1.获取对应的处理器**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152608924.png)


```java
// 这个方法是得到我们能够处理返回值的返回值处理器
@Nullable
private HandlerMethodReturnValueHandler selectHandler(@Nullable Object value, MethodParameter returnType) {
   boolean isAsyncValue = isAsyncReturnValue(value, returnType);
    // 挨个遍历所有的返回值处理器
   for (HandlerMethodReturnValueHandler handler : this.returnValueHandlers) {
       // 处理异步的
      if (isAsyncValue && !(handler instanceof AsyncHandlerMethodReturnValueHandler)) {
         continue;
      }
       // 判断是否可以处理返回值
      if (handler.supportsReturnType(returnType)) {
          // 可以就就行返回
         return handler;
      }
   }
   return null;
}
```

eg:RequestResponseBodyMethodProcessor

```java
@Override
public boolean supportsReturnType(MethodParameter returnType) {
    // 判断有没有标准件@ResponseBody注解,有的话就使用这个返回值处理器
   return (AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), ResponseBody.class) ||
         returnType.hasMethodAnnotation(ResponseBody.class));
}
```

其他的处理器有它自己的判断，判断是否符合条件

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152619294.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)



**2.处理器执行处理方法**

-   writeWithMessageConverters(returnValue, returnType, inputMessage, outputMessage);
    -   ①内容协商(浏览器默认会以请求头的方式告诉服务器它可以接收的数据类型)
        -   acceptableTypes = getAcceptableMediaTypes(request);	得到浏览器可以接收的类型
    -   ②服务器根据自己自身的能力，决定服务器端可以生产出什么类型的数据
        -   List<MediaType> producibleTypes = getProducibleMediaTypes(request, valueType, targetType);
    -   ③springMvc挨个遍历所有容器的HttpMessageConverter，然后将数据写出去
        -   genericConverter.write(body, targetType, selectedMediaType, outputMessage);

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152628693.png)



#### 2.springMVC支持的返回值

这个是根据返回值处理器得出的

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152641904.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


```java
ModelAndView
Model
View
ResponseEntity 
ResponseBodyEmitter
StreamingResponseBody
HttpEntity
HttpHeaders
Callable
DeferredResult
ListenableFuture
CompletionStage
WebAsyncTask
有 @ModelAttribute 且为对象类型的
@ResponseBody 注解 ---> RequestResponseBodyMethodProcessor；
```



#### 3.HTTPMessageConverter消息转换器原理

**1、MessageConverter规范**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152655305.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


HttpMessageConverter: 看是否支持将 此 Class类型的对象，转为MediaType类型的数据。

eg：Person对象转为JSON。或者 JSON转为Person



**2、得到支持的媒体类型**

```java
protected List<MediaType> getProducibleMediaTypes(
      HttpServletRequest request, Class<?> valueClass, @Nullable Type targetType) {

   Set<MediaType> mediaTypes =
         (Set<MediaType>) request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
   if (!CollectionUtils.isEmpty(mediaTypes)) {
      return new ArrayList<>(mediaTypes);
   }
   List<MediaType> result = new ArrayList<>();
    // 挨个遍历消息转换器，看谁能处理我们的转换
   for (HttpMessageConverter<?> converter : this.messageConverters) {
      if (converter instanceof GenericHttpMessageConverter && targetType != null) {
          // 进行判断，是否可以写出
         if (((GenericHttpMessageConverter<?>) converter).canWrite(targetType, valueClass, null)) {
             // 将支持的媒体类型放入到集合中
            result.addAll(converter.getSupportedMediaTypes(valueClass));
         }
      }
      else if (converter.canWrite(valueClass, null)) {
         result.addAll(converter.getSupportedMediaTypes(valueClass));
      }
   }
   return (result.isEmpty() ? Collections.singletonList(MediaType.ALL) : result);
}
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152708194.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


```java
0 - 只支持Byte类型的
1 - String
2 - String
3 - Resource
4 - ResourceRegion
5 - DOMSource.class \ SAXSource.class) \ StAXSource.class \StreamSource.class \Source.class
6 - MultiValueMap
7 - true 
8 - true
9 - 支持注解方式xml处理的。
```





### 2.内容协商

根据客户端接收能力不同，返回不同媒体类型的数据。

#### ①引入xml依赖

```xml
 <dependency>
     <groupId>com.fasterxml.jackson.dataformat</groupId>
     <artifactId>jackson-dataformat-xml</artifactId>
</dependency>
```

我们引入解析xml的包后，浏览器默认返回的就是我们xml的类型，因为xml的权重比json的高

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152717605.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)




#### ②postman分别测试返回json和xml

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152726358.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


![在这里插入图片描述](https://img-blog.csdnimg.cn/2021071715273430.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)




#### ③开启浏览器参数方式内容协商

为了方便内容协商，开启基于请求参数的内容协商功能。

```yml
spring:
    contentnegotiation:
      favor-parameter: true  #开启请求参数内容协商模式
```

开启了这个模式就会多出一个参数内容协商策略器来得到我们需要返回的媒体类型

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152743634.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)

通过传递`format`参数来得到我们想要返回的数据类型

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152752740.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152800697.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)




#### ④内容协商原理

1.  判断当前响应头中是否已经有确定的媒体类型

    writeWithMessageConverters方法

    MediaType contentType = outputMessage.getHeaders().getContentType();

2.  获取请求头中可以接收的媒体类型

    acceptableTypes = getAcceptableMediaTypes(request);

    ```java
    @Override
    public List<MediaType> resolveMediaTypes(NativeWebRequest request) throws HttpMediaTypeNotAcceptableException {
        // 遍历得到内容协商策略器，获取支持的媒体类型
       for (ContentNegotiationStrategy strategy : this.strategies) {
          List<MediaType> mediaTypes = strategy.resolveMediaTypes(request);
          if (mediaTypes.equals(MEDIA_TYPE_ALL_LIST)) {
             continue;
          }
          return mediaTypes;
       }
       return MEDIA_TYPE_ALL_LIST;
    }
    ```

    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155925868.png)


    123

3.  遍历得到系统中所有的messageConverter，看谁支持操作这个对象（Person）

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152810143.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)

导入了jackson处理xml的包，xml的converter就会自动进来


```java
WebMvcConfigurationSupport
jackson2XmlPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader);

if (jackson2XmlPresent) {
    Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.xml();
    if (this.applicationContext != null) {
        builder.applicationContext(this.applicationContext);
    }
    // 导入了这个类它就会帮我们添加到所有的messageConverter
    messageConverters.add(new MappingJackson2XmlHttpMessageConverter(builder.build()));
}
```


​    

4.  统计支持操作Person的Converter，把converter支持的媒体类型统计出来

    

5.  客户端需要xml。服务端支持的（10种，json，xml）

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152820901.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


​    

6.  进行内容协商的最佳匹配

    ```java
    for (MediaType requestedType : acceptableTypes) {
       for (MediaType producibleType : producibleTypes) {
          if (requestedType.isCompatibleWith(producibleType)) {
             mediaTypesToUse.add(getMostSpecificMediaType(requestedType, producibleType));
          }
       }
    }
    
    for (MediaType mediaType : mediaTypesToUse) {
        if (mediaType.isConcrete()) {
            selectedMediaType = mediaType;
            break;
        }
        else if (mediaType.isPresentIn(ALL_APPLICATION_MEDIA_TYPES)) {
            selectedMediaType = MediaType.APPLICATION_OCTET_STREAM;
            break;
        }
    }
    ```

    

7.  最后进行转换，然后调用方法进行输出

    ```java
    if (selectedMediaType != null) {
       selectedMediaType = selectedMediaType.removeQualityValue();
        // 挨个遍历消息转换器
       for (HttpMessageConverter<?> converter : this.messageConverters) {
          GenericHttpMessageConverter genericConverter = (converter instanceof GenericHttpMessageConverter ?
                (GenericHttpMessageConverter<?>) converter : null);
          // 多从判断，判断是否支持操作这个数据，判断是否可以将数据转为我们想要的媒体类型
           if (genericConverter != null ?
                ((GenericHttpMessageConverter) converter).canWrite(targetType, valueType, selectedMediaType) :
                converter.canWrite(valueType, selectedMediaType)) {
              // 得到要输出的内容
             body = getAdvice().beforeBodyWrite(body, returnType, selectedMediaType,
                   (Class<? extends HttpMessageConverter<?>>) converter.getClass(),
                   inputMessage, outputMessage);
             if (body != null) {
                Object theBody = body;
                LogFormatUtils.traceDebug(logger, traceOn ->
                      "Writing [" + LogFormatUtils.formatValue(theBody, !traceOn) + "]");
                addContentDispositionHeader(inputMessage, outputMessage);
                if (genericConverter != null) {
                    // 调用方法进行写出操作
                   genericConverter.write(body, targetType, selectedMediaType, outputMessage);
                }
                else {
                   ((HttpMessageConverter) converter).write(body, selectedMediaType, outputMessage);
                }
             }
             else {
                if (logger.isDebugEnabled()) {
                   logger.debug("Nothing to write: null body");
                }
             }
             return;
          }
       }
    }
    ```





#### ⑤自定义MessageConverter

实现多数据兼容。json、xml、zhi

1、**@ResponseBody** 响应数据出去 调用 **RequestResponseBodyMethodProcessor** 处理

2、Processor 处理方法返回值。通过 **MessageConverter** 处理

3、所有 **MessageConverter** 合起来可以支持各种媒体类型数据的操作（读、写）

4、内容协商找到最终的 **messageConverter**；

springMVC的什么功能。一个入口容器中添加WebMvcConfigurer

```java
@Configuration
public class MyConfig implements WebMvcConfigurer {
    // 将默认的覆盖掉
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    }
    // 扩展功能
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {

    }
}
```

我们使用扩展的来添加我们自己的MessageConverter

首先自定义我们MessageConverter

```java
public class ZhiMessageConverter implements HttpMessageConverter<Person> {
    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return clazz.isAssignableFrom(Person.class);
    }

    /**
     * 服务器要统计所有的messageConverter都能写出那些内容
     * @return
     */
    @Override
    public List<MediaType> getSupportedMediaTypes() {
        // 转为我们自定的类型
        return MediaType.parseMediaTypes("application/zhi");
    }

    @Override
    public Person read(Class<? extends Person> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return null;
    }

    @Override
    public void write(Person person, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        // 自定义协议的写出
        String data = person.getName() + ";" + person.getAge();
        // 写出去
        OutputStream body = outputMessage.getBody();
        body.write(data.getBytes());
    }
}
```

添加到系统中

```java
@Configuration
public class MyConfig implements WebMvcConfigurer {
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new ZhiMessageConverter());
    }
}
```

![](https://img-blog.csdnimg.cn/20210717152832975.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)




**debug分析**

1.  得到客户端需要的到的内容

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152843221.png)


​    

2.  得到服务器可以生产的媒体类型

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152852544.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)

3.  进行内容协商匹配

4.  进行输出



#### ⑥浏览器请求自定义数据

我们知道了参数内容协商策略只支持json和xml

所以我们要自己自定义一个我们自己的参数内容协商策略

```java
/*
*  自定义内容策略
*/
@Override
public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    Map<String, MediaType> mediaTypes = new HashMap<>();
    mediaTypes.put("json",MediaType.APPLICATION_JSON);
    mediaTypes.put("xml",MediaType.APPLICATION_XML);
    // 我们自定义的类型
    mediaTypes.put("zhi",MediaType.parseMediaType("application/zhi"));
    // 自定义的内容协商策略，指定那些参数对应那些媒体类型
    ParameterContentNegotiationStrategy strategy = new ParameterContentNegotiationStrategy(mediaTypes);
    configurer.strategies(Arrays.asList(strategy));
}
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152903427.png)


![](https://img-blog.csdnimg.cn/20210717152911273.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)




==但是，这会造成一个问题，我们再次使用postman使用请求头的方式请求数据的时候返回的都是返回json类型的数据==

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152919395.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


因为我们自定义了我们自己的，所以默认请求头的就被我们覆盖了，我们在自定义的策略中添加上就可以了

```java
@Override
public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    Map<String, MediaType> mediaTypes = new HashMap<>();
    mediaTypes.put("json",MediaType.APPLICATION_JSON);
    mediaTypes.put("xml",MediaType.APPLICATION_XML);
    // 我们自定义的类型
    mediaTypes.put("zhi",MediaType.parseMediaType("application/zhi"));
    // 自定义的内容协商策略，指定那些参数对应那些媒体类型
    ParameterContentNegotiationStrategy strategy = new ParameterContentNegotiationStrategy(mediaTypes);
    // 自定义参数名，默认是format
    strategy.setParameterName("ss");
    // 默认的请求头策略
    HeaderContentNegotiationStrategy headerStrategy = new HeaderContentNegotiationStrategy();
    configurer.strategies(Arrays.asList(strategy, headerStrategy));
}
```

我们还可以自定义我们想要的接收参数

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717152927934.png)




**有可能我们添加的自定义的功能会覆盖默认很多功能，导致一些默认的功能失效。**

**SpringBoot有为我们提供基于配置文件的快速修改媒体类型功能**

![](https://img-blog.csdnimg.cn/20210717152934244.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)




## 五、模板引擎与视图解析

视图解析：**SpringBoot默认不支持 JSP，需要引入第三方模板引擎技术实现页面渲染。**



### 1	模板引擎-Thymeleaf

官方文档：https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html



#### 1.基本语法

##### ①表达式

| 表达式名字 | 语法   | 用途                               |
| ---------- | ------ | ---------------------------------- |
| 变量取值   | ${...} | 获取请求域、session域、对象等值    |
| 选择变量   | *{...} | 获取上下文对象值                   |
| 消息       | #{...} | 获取国际化等值                     |
| 链接       | @{...} | 生成链接                           |
| 片段表达式 | ~{...} | jsp:include 作用，引入公共页面片段 |



##### ②字面量

文本值: **'one text'** **,** **'Another one!'** **,…**数字: **0** **,** **34** **,** **3.0** **,** **12.3** **,…**布尔值: **true** **,** **false**

空值: **null**

变量： one，two，.... 变量不能有空格

##### ③文本操作

字符串拼接: **+**

变量替换: **|The name is ${name}|** 



##### ④数学运算

运算符: + , - , * , / , %



##### ⑤布尔运算

运算符:  **and** **,** **or**

一元运算: **!** **,** **not** 





##### ⑥比较运算

比较: **>** **,** **<** **,** **>=** **,** **<=** **(** **gt** **,** **lt** **,** **ge** **,** **le** **)**等式: **==** **,** **!=** **(** **eq** **,** **ne** **)** 



##### ⑦条件运算

If-then: **(if) ? (then)**

If-then-else: **(if) ? (then) : (else)**

Default: (value) **?: (defaultvalue)** 



##### ⑧特殊操作

无操作： _



#### 2设置属性值-th:attr

设置单个值

```html
<form action="subscribe.html" th:attr="action=@{/subscribe}">
  <fieldset>
    <input type="text" name="email" />
    <input type="submit" value="Subscribe!" th:attr="value=#{subscribe.submit}"/>
  </fieldset>
</form>
```

设置多个值

```html
<img src="../../images/gtvglogo.png"  th:attr="src=@{/images/gtvglogo.png},title=#{logo},alt=#{logo}" />
```



以上两个的代替写法 th:xxxx

```html
<input type="submit" value="Subscribe!" th:value="#{subscribe.submit}"/>
<form action="subscribe.html" th:action="@{/subscribe}">
```



所有h5兼容的标签写法

https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#setting-value-to-specific-attributes



#### 3.迭代

```html
<tr th:each="prod : ${prods}">
        <td th:text="${prod.name}">Onions</td>
        <td th:text="${prod.price}">2.41</td>
        <td th:text="${prod.inStock}? #{true} : #{false}">yes</td>
</tr>
```



```html
<tr th:each="prod,iterStat : ${prods}" th:class="${iterStat.odd}? 'odd'">
  <td th:text="${prod.name}">Onions</td>
  <td th:text="${prod.price}">2.41</td>
  <td th:text="${prod.inStock}? #{true} : #{false}">yes</td>
</tr>
```



#### 4.条件运算

```html
<a href="comments.html"
th:href="@{/product/comments(prodId=${prod.id})}"
th:if="${not #lists.isEmpty(prod.comments)}">view</a>
```



```html
<div th:switch="${user.role}">
  <p th:case="'admin'">User is an administrator</p>
  <p th:case="#{roles.manager}">User is a manager</p>
  <p th:case="*">User is some other thing</p>
</div>
```



#### 5.属性优先级

![img](https://img-blog.csdnimg.cn/img_convert/9f6b7f4cb446cc92ec4650bedcaf3f4b.png)





### 2	thymeleaf使用

#### 1.引入stater

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```



#### 2.自动配置好了thymeleaf

```java
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ThymeleafProperties.class)
@ConditionalOnClass({ TemplateMode.class, SpringTemplateEngine.class })
@AutoConfigureAfter({ WebMvcAutoConfiguration.class, WebFluxAutoConfiguration.class })
public class ThymeleafAutoConfiguration {
```

**自动配置策略**

1.  **绑定了ThymeleafProperties配置类**
2.  **配置好了SpringTemplateEngine模板引擎**
3.  **配好了thymeleafViewResolver(模板视图解析器)**



### 3	构建后台管理系统

#### 1、项目创建

thymeleaf、web-starter、devtools、lombok



#### 2、静态资源处理

自动配置好，我们只需要把所有静态资源放到 static 文件夹下

![在这里插入图片描述](https://img-blog.csdnimg.cn/2021071715295672.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)




#### 3、路径构建

th:action="@{/login}"



#### 4、模板抽取

th:insert/replace/include



#### 5、页面跳转

```java
@PostMapping("/login")
public String main(User user, HttpSession session, Model model){

    if(StringUtils.hasLength(user.getUserName()) && "123456".equals(user.getPassword())){
        //把登陆成功的用户保存起来
        session.setAttribute("loginUser",user);
        //登录成功重定向到main.html;  重定向防止表单重复提交
        return "redirect:/main.html";
    }else {
        model.addAttribute("msg","账号密码错误");
        //回到登录页面
        return "login";
    }

}
```

#### 6、数据渲染

```java
@GetMapping("/dynamic_table")
public String dynamic_table(Model model){
    //表格内容的遍历
    List<User> users = Arrays.asList(new User("zhangsan", "123456"),
            new User("lisi", "123444"),
            new User("haha", "aaaaa"),
            new User("hehe ", "aaddd"));
    model.addAttribute("users",users);

    return "table/dynamic_table";
}
```

```html

<table class="display table table-bordered" id="hidden-table-info">
    <thead>
        <tr>
            <th>#</th>
            <th>用户名</th>
            <th>密码</th>
        </tr>
    </thead>
    <tbody>
        <tr class="gradeX" th:each="user,stats:${users}">
            <td th:text="${stats.count}">Trident</td>
            <td th:text="${user.userName}">Internet</td>
            <td >[[${user.password}]]</td>
        </tr>
    </tbody>
</table>
```





### 4	视图解析原理

#### 1、重定向原理

1.  **得到它的返回值处理器`ViewNameMethodReturnValueHandler`**

 ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153006253.png)


2.  **执行方法**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153014261.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


    ```java
    protected boolean isRedirectViewName(String viewName) {
        // 判断名字是否匹配这个正则表达式或者是不是以redirect:开头的
       return (PatternMatchUtils.simpleMatch(this.redirectPatterns, viewName) || viewName.startsWith("redirect:"));
    }
    ```

3.  **处理完成之后，返回一个ModelAndView对象，里面存放我们的数据和视图地址**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153023999.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


    ![在这里插入图片描述](https://img-blog.csdnimg.cn/2021071715593716.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


4.  **得到数据和视图之后就要将他们进行输出给客户端进行展示**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153031627.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


5.  **处理派发结果**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153039185.png)


6.  **得到`View`视图对象**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153050262.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


    **render	-->	resolveViewName**
    
    ```java
    @Nullable
    protected View resolveViewName(String viewName, @Nullable Map<String, Object> model,
          Locale locale, HttpServletRequest request) throws Exception {
    
       if (this.viewResolvers != null) {
           // 遍历系统中所有的视图解析器，看谁能解析这个视图
          for (ViewResolver viewResolver : this.viewResolvers) {
             View view = viewResolver.resolveViewName(viewName, locale);
             if (view != null) {
                return view;
             }
          }
       }
       return null;
    }
    ```
    
    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155950255.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


7.  **最后重定向**

    `view.render(mv.getModelInternal(), request, response);`

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153100448.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)




#### 2、请求转发原理

和重定向原理基本一致，最后也是调用了原生的请求转发forward方法



#### 3、视图渲染

过程和重定向的差不多，重定向调用的是`ContentNegotiatingViewResolver`内容协商视图解析器，我们的视图是有我们导入进来的`ThymeleafViewResolver`视图解析器进行解析

我们来看看它是怎么进行解析的

1.  **来到DispatcherServlet中的render方法**

    view = resolveViewName(viewName, mv.getModelInternal(), locale, request)

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153108937.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)

2.  view调用render方法

    `view.render(mv.getModelInternal(), request, response);`

    ThymeleafView: renderFragment()

    ==设置一些列东西后，得到我们的视图内容，视图模板引擎调`viewTemplateEngine`用`process`方法进行输出==

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153122628.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


    因为我们会使用thymeleaf中的语法获取值，处理和分析就是为了将我们的值渲染到视图上

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153132912.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


    最终调用flush 方法进行输出，到此我们就完成了我们的页面渲染

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153143736.png)


​    

​    




## 六、拦截器

### 1、HandlerInterceptor接口

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153152495.png)


```java
public class MyInterceptor implements HandlerInterceptor {
    /**
     * 拦截之前执行
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 判断是否登录，登录了才能进行操作
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null) {
            // 跳转到登录页面
            request.setAttribute("msg","请先登录");
            request.getRequestDispatcher("/").forward(request, response);
            return false;
        }
        return true;
    }

    /**
     * 目标方法完成之后执行
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    /**
     * 页面渲染之后执行
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
```



### 2、配置拦截器

将我么自定义的拦截器放入到容器中

```java
@Configuration
public class AdminConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MyInterceptor())
                .addPathPatterns("/**")
                // 一定要注意不要将静态资源也拦截了
                .excludePathPatterns("/","/login","/css/**","/js/**","/fonts/**","/images/**");
    }
}
```



### 3、拦截器原理

1.  首先还是在DispatcherServlet下的doDispatch()方法中

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153203629.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


    映射处理器中有我们所有的拦截器

2.  在执行方法之前，它会去调用拦截器进行判断，如果全部拦截器都为true，那么就执行目标方法

    ```java
    if (!mappedHandler.applyPreHandle(processedRequest, response)) {
    	return;
    }
    ```

    ```java
    boolean applyPreHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 遍历循环得到每个拦截器
       for (int i = 0; i < this.interceptorList.size(); i++) {
          HandlerInterceptor interceptor = this.interceptorList.get(i);
           // 调用拦截器的preHandle方法，返回true就是放行
          if (!interceptor.preHandle(request, response, this.handler)) {
             triggerAfterCompletion(request, response, null);
             return false;
          }
           // 执行了拦截器的索引
          this.interceptorIndex = i;
       }
       return true;
    }
    ```

3.  执行完目标方法返回modelAndView对象之后，mappedHandler执行`applyPostHandle`方法

    ```java
    void applyPostHandle(HttpServletRequest request, HttpServletResponse response, @Nullable ModelAndView mv)
          throws Exception {
    	// 反序遍历我们执行成功的拦截器
       for (int i = this.interceptorList.size() - 1; i >= 0; i--) {
          HandlerInterceptor interceptor = this.interceptorList.get(i);
           // 执行postHandle方法
          interceptor.postHandle(request, response, this.handler, mv);
       }
    }
    ```

4.  我们上面的所有操作如果有发生异常的都会触发`triggerAfterCompletion`方法，这个方法就是执行我们的每个拦截器的`afterCompletion`方法

    ```java
    void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response, @Nullable Exception ex) {
       for (int i = this.interceptorIndex; i >= 0; i--) {
          HandlerInterceptor interceptor = this.interceptorList.get(i);
          try {
             interceptor.afterCompletion(request, response, this.handler, ex);
          }
          catch (Throwable ex2) {
             logger.error("HandlerInterceptor.afterCompletion threw exception", ex2);
          }
       }
    }
    ```

5.  如果开启了异步并发，那么它会执行异步拦截器中的`afterConcurrentHandlingStarted`方法

    ![](https://img-blog.csdnimg.cn/20210717153213137.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)




## 七、文件上传

### 1.页面代码

```java
<form role="form" th:action="@{/upload}" method="post" enctype="multipart/form-data">
    <div class="form-group">
        <label for="exampleInputEmail1">邮箱</label>
        <input type="email" name="email" class="form-control" id="exampleInputEmail1" placeholder="Enter email">
    </div>
    <div class="form-group">
        <label for="exampleInputPassword1">名字</label>
        <input type="text" name="username" class="form-control" id="exampleInputPassword1" placeholder="Password">
    </div>
    <div class="form-group">
        <label for="exampleInputFile">头像</label>
        <input type="file" name="headerImg" id="exampleInputFile">
    </div>
    <div class="form-group">
        <label for="exampleInputFile">生活照</label>
        <input type="file" name="photos" multiple>
    </div>
    <div class="checkbox">
        <label>
            <input type="checkbox"> Check me out
        </label>
    </div>
    <button type="submit" class="btn btn-primary">提交</button>
</form>
```

两个要注意的点：

1.  上传文件form元素中一定要有编码类型：`enctype="multipart/form-data"`
2.  `<input type="file" name="photos" multiple>`添加multiple属性可以同时上传多个文件



### 2.文件上传的controller

```java
@Slf4j
@Controller
public class FormController {

    @GetMapping("/form_layouts")
    public String form_layouts() {
        return "/form/form_layouts";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam String email,
                         @RequestParam String username,
                         // 可以不要注解，建议加上
                         @RequestPart("headerImg") MultipartFile headerImg,
                         @RequestPart("photos") MultipartFile[] photos) throws IOException {
        log.info("上传的信息：email={}，username={}，headerImg={}，photos={}",
                email,username,headerImg.getSize(),photos.length);
        if (!headerImg.isEmpty()) {
            // 保存到文件服务器，oss对象存储服务器，在这里我们保存到本地硬盘
            String filename = headerImg.getOriginalFilename();
            headerImg.transferTo(new File("F:\\testUpload\\" + filename));
        }
        if (photos.length > 0) {
            // 遍历进行保存
            for (MultipartFile photo : photos) {
                if (!photo.isEmpty()) {
                    String filename = photo.getOriginalFilename();
                    photo.transferTo(new File("F:\\testUpload\\" + filename));
                }
            }
        }
        return "main";
    }
}
```



### 3.文件上传原理

原理和之前不一样的就是得到的解析器不同

1.  **在得到解析器之前，它会进行检查，判断这个请求是否有文件上传**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153225300.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153233270.png)


​    

2.  **有的话就将当前的request对象进行封装，封装成`MultipartHttpServletRequest`，它是继承了HttpServletRequest的**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153241536.png)


​    

3.  **接下来就是执行目标方法，处理参数环节**

    ```java
    getMethodArgumentValues方法
    // 确定参数值
    args[i] = this.resolvers.resolveArgument(parameter, mavContainer, request, this.dataBinderFactory);
    ```

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153256858.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


    遍历参数处理器，看那个可以处理当前这个参数

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153306767.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153314228.png)


4.  调用方法参数解析器中的`resolveArgument`来解析参数

    **RequestPartMethodArgumentResolver	-->	resolveArgument**

    ```java
    Object mpArg = MultipartResolutionDelegate.resolveMultipartArgument(name, parameter, servletRequest);
    ```

    ![](https://img-blog.csdnimg.cn/20210717153326840.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153338974.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


    **因为我们debug的是多个文件的那个参数，所以执行完上面的就直接返回了**

![在这里插入图片描述](https://img-blog.csdnimg.cn/2021071715334818.png)


    **最后调用最终的方法进行返回**
    
    ```java
    return adaptArgumentIfNecessary(arg, parameter);
    
    protected Object adaptArgumentIfNecessary(@Nullable Object arg, MethodParameter parameter) {
        if (parameter.getParameterType() == Optional.class) {
            if (arg == null || (arg instanceof Collection && ((Collection<?>) arg).isEmpty()) ||
                (arg instanceof Object[] && ((Object[]) arg).length == 0)) {
                return Optional.empty();
            }
            else {
                return Optional.of(arg);
            }
        }
        return arg;
    }
    ```

5.  最后返回结果

    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153440526.png)


![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153448542.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)






## 八、异常处理

### 1、默认规则

我们服务器出现异常后，springBoot默认会找我们templates/error下的错误页面，命名是4xx、5xx或者可以具体到404错误

客户端和浏览器是有区别的：

-   浏览器返回的是页面

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153458634.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


-   客户端返回的是json数据

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153506889.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


他们返回的信息都是一样的，只是展示出来的不一样而已，信息可以通过${}取出来，eg:${status}取出状态码











### 2、定义错误页面处理逻辑

-   自定义错误页

    1.  **error/404.html	error/5xx.html;**	有精确的错误状态码页面就匹配精确， 没有的话就找4xx.html，如果都没有就返回系统默认的错误页面

        

    2.  **@ControllerAdvice + @ExceptionHandler处理全局异常**    底层是**ExceptionHandlerExceptionResolver**专门处理标有 @ExceptionHandler注解的异常

```java
/*
   处理整个web controller的异常
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler  {

    @ExceptionHandler({ArithmeticException.class,NullPointerException.class})   // 处理异常
    // 返回值也可以是ModelAndView对象
    public String handlerArithException(Exception e) {
        log.error("异常:{}" ,e);
        return "login"; // 视图地址
    }
}
```

​        

3.  **@ResponseStatus + 自定义异常**   **ResponseStatusExceptionResolver**处理标注@ResponseStatus注解的异常类

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153521834.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)
*调用底层的response.sendError(statusCode); tomcat发/error请求**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153539885.png)


```java
        @ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "用户数量太多")    // reason为原因
        public class UserTooManyException extends RuntimeException {
        
            public UserTooManyException(String message) {
                super(message);
            }
        }
```

​        

 4.  **spring底层的异常，如参数转换异常**  

         **DefaultHandlerExceptionResolver 处理框架底层的异常**

        它的底层**调用底层的response.sendError(HttpServletResponse.SC BAD_ REQUEST, ex.getMessage(); tomcat发/error请求**

        

    5.  **自定义实现HandlerExceptionResolver异常，可以作为默认的全局异常处理规则**

        ```java
        // 自定义处理异常解析器
        @Order(Ordered.HIGHEST_PRECEDENCE)  // 值越小，优先级越高
        @Component
        public class CustomerHandlerExceptionResolver implements HandlerExceptionResolver {
        
            @Override
            public ModelAndView resolveException(HttpServletRequest request,
                                                 HttpServletResponse response,
                                                 Object handler, Exception ex) {
                try {
                    response.sendError(511, "自定义的解析器");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return new ModelAndView();
            }
        }
        ```

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153645804.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


​        

6.  **ErrorViewResolver实现自定义处理异常**

 -   没有能处理这个异常的解析器，tomcat就会发送**/error**请求，然后被**BasicErrorControler**处理
 -   底层**response.sendError**也是发送**/error**给**BasicErrorControler**处理
 -   **BasicErrorControler**要跳转到那个错误页是由**ErrorViewResolver**来解析的，所以我们也可以自定义**ErrorViewResolver**的逻辑来处理跳转的错误页

        



==**这些页面处理逻辑都有对应的一个处理异常错误解析器**==



### 3、异常处理自动配置原理

-   ErrorMvcAutoConfiguration		自动配置异常处理规则

    1.  组件：**DefaultErrorAttributes**	id：**errorAttributes**

        -   public class DefaultErrorAttributes implements **ErrorAttributes**, **HandlerExceptionResolver**

        -   定义错误页面可以包含哪些数据

            ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717160000832.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


​        

    2.  组件：**BasicErrorController**     id：**basicErrorController**   它是controller，我们没有自定义错误页面逻辑，那么就它会发送/error请求，这个就是负责跳转到默认"/error"的controller
   ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153732286.png)
        -   返回页面的处理方法
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153759264.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


            返回json字符串

![在这里插入图片描述](E:\学习笔记\img\img01\watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70.png)


    -   容器中有组件： **View**    id:error
        它返回系统默认的视图，调用**render**方法进行渲染

​            ![在这里插入图片描述](E:\学习笔记\img\img01\watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70-16875880883303.png)


    -   容器中放组件：**BeanNameViewResolver**  id：beanNameViewResolver 
     **视图解析器**：按照返回的视图名作为组件id去容器中找View对象
    
    4.  组件：**DefaultErrorViewResolver**    **id：conventionErrorViewResolver**
    
        -   发生错误，它会根据出现错误的状态码找到对应的视图

​        ![在这里插入图片描述](E:\学习笔记\img\img01\watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70-16875881017526.png)




### 4、异常处理原理解析

我们给它制造一个算数异常 int i = 1/0;

1.拿到适配器，然后执行目标方法，一旦发生错误，异常就会被捕捉

![在这里插入图片描述](https://img-blog.csdnimg.cn/2021071715390419.png)

2.处理派发结果	**processDispatchResult()**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153912323.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


3.**processDispatchResult  -->  processHandlerException**	遍历所有的处理异常解析器，看谁能处理这个异常，如果不能，它就会继续往上抛异常

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717153920976.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


1.  **DefaultErrorAttributes**：它就是将错误信息保存到请求域中，返回为null

![在这里插入图片描述](https://img-blog.csdnimg.cn/2021071715392890.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


2.  还有一个是**组合**的处理异常解析器，它里面还会进行遍历每个解析器，他们都有自己的规则

    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154026713.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)

4.如果这个异常没有任何一个能处理，那么底层就会发送/error请求，会被底层的**BasicErrorController**处理

1.  经过内容协商，找到适合的处理方法

2.  将保存在请求域中的数据拿出来

3.  来遍历所有的错误视图解析器，看谁能解析这个错误页面，默认的就是springBoot帮我们配好的那个

    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154042387.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)

默认的解析器的作用就是将我们的错误状态码作为错误页的名字

​        ![在这里插入图片描述](E:\学习笔记\img\img01\watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70-16875881516489.png)


4.  最终就会响应erroe/5xx这个页面











## 九、web原生组件注入（Servlet、Filter、Listener）

### 1、使用Servlet	ApI

==**推荐使用这种方式**==

**@ServletComponentScan**(basePackages = **"com.atguigu.admin"**) :指定原生Servlet组件都放在那里

**@WebServlet**(urlPatterns = **"/my"**)：效果：直接响应，**没有经过Spring的拦截器？**

	因为spring的拦截器拦截的是它的DispatcherServlet，我们并没有定义拦截器拦截我们自定义的Serlvet
	
	它是一个优先匹配原则，比如同样都是处理/my下的请求，那么如果有确定的就是映射到确定的
	
	**eg:	①/my	②/my/aa	那么就会精准匹配②**



**@WebFilter**(urlPatterns={**"/css/\*"**,**"/images/\*"**})

**@WebListene**

**代码实现**

```java
@WebServlet(urlPatterns = "/my")	// 处理请求路径
public class MyServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write("666");
    }
}
```

```java
@Slf4j
@WebFilter(urlPatterns = {"/css/**","/js/**"})	// 拦截路径
public class MyFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("过滤器初始化");  
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        chain.doFilter(request, response);
        log.info("过滤器正在工作");
    }
    
    @Override
    public void destroy() {
        log.info("过滤器销毁");
    }
}
```

```java
@Slf4j
@WebListener
public class MyServletContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("监听初始化完成");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("监听到销毁");
    }
}
```

**还要扫描原生的组件，不然是不会生效的**

```java
@ServletComponentScan(basePackages = "com.xiaozhi.admin")
@Configuration
public class AdminConfig implements WebMvcConfigurer {
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154108609.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154115380.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154123372.png)






### 2、使用RegistrationBean

ServletRegistrationBean`, `FilterRegistrationBean`, and `ServletListenerRegistrationBean

**代码实现**

```java
@Configuration
public class AdminConfig implements WebMvcConfigurer {
    @Bean
    public ServletRegistrationBean myServlet() {
        // 对象后面就是路径映射
        return new ServletRegistrationBean(new MyServlet(), "/my", "you");
    }
    @Bean
    public FilterRegistrationBean myFilter() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(new MyFilter());
        // 设置拦截的路径
        filterRegistrationBean.setUrlPatterns(Arrays.asList("/my","/css/*"));
        return filterRegistrationBean;
    }

    @Bean
    public ServletListenerRegistrationBean myServletListener() {
        MyServletContextListener myServletContextListener = new MyServletContextListener();
        return new ServletListenerRegistrationBean(myServletContextListener);
    }

}
```





## 十、嵌入式Servlet容器

### 1.切换嵌入式Servlet容器

springBoot不用配置服务器是因为它已经将服务器内嵌进来了

默认支持的webServer

-   `Tomcat`, `Jetty`,  `Undertow`
-   ServletWebServerApplicationContext 容器启动寻找ServletWebServerFactory 并引导创建服务器
-   默认是tomcat服务器，我们也可以手动切换服务器

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154132928.png)

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <exclusions>
                <!--移除tomcat-->
                <exclusion>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-tomcat</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
		<!--替换tomcat-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-undertow</artifactId>
        </dependency>
```



**原理**：

-   我们先找到自动配置类，我们看它给我们做了什么

-   `ServletWebServerFactoryAutoConfiguration`（web服务工厂  --> 生产web服务的）

    -  ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154147164.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


-   `ServletWebServerFactoryConfiguration`服务器的配置类，动态使用不同的服务器

    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154208500.png)
    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154225697.png)

只有导入对应的包才能使用对应的服务器，默认是导入tomcat的包

![在这里插入图片描述](E:\学习笔记\img\img01\20210717154250242.png)


-   导入了tomcat的包，那么它就会有  `TomcatServletWebServerFactory`（tomcat的服务器工厂）

-   `TomcatServletWebServerFactory`创建出Tomcat服务器并启动；

    -   **TomcatWebServer 的构造器拥有初始化方法initialize---this.tomcat.start();**
    -   ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154300697.png)

    -   ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154312470.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)

        -   ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154323802.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)

        -   ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154332996.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)

        -   ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154343175.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)

        -   ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154351953.png)


-   内嵌服务器就是手动来执行它的代码，设置它的一些配置



### 2.定制Servlet容器

-   修改配置文件server.xxx
-   实现`WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>`
    -   将配置文件的值和ServletWebServerFactory进行绑定
-   自定义ConfigurableServletWebServerFactory

**xxxxCustomizer：定制化器，可以改变xxxx的默认规则**



![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154403386.png)


```java
@Component
public class MyWebServerFactoryCustomizer implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    @Override
    public void customize(ConfigurableServletWebServerFactory server) {
        server.setPort(9000);
    }

}
```







## 十一、定制化原理

### 1、定制化的常见方式 

-   修改配置文件；
-   **xxxxxCustomizer；**

-   **编写自定义的配置类   xxxConfiguration；+** **@Bean替换、增加容器中默认组件；视图解析器** 
-   **Web应用 编写一个配置类实现** **WebMvcConfigurer 即可定制化web功能；+ @Bean给容器中再扩展一些组件**

```java
@Configuration
public class AdminWebConfig implements WebMvcConfigurer
```

-   @EnableWebMvc + WebMvcConfigurer —— @Bean  可以全面接管SpringMVC，所有规则全部自己重新配置； 实现定制和扩展功能



**原理**

-   1、WebMvcAutoConfiguration  默认的SpringMVC的自动配置功能类。静态资源、欢迎页.....

-   2、一旦使用 @EnableWebMvc 、。会 @Import(DelegatingWebMvcConfiguration.**class**)
-   3、**DelegatingWebMvcConfiguration** 的 作用，只保证SpringMVC最基本的使用

-   把所有系统中的 WebMvcConfigurer 拿过来。所有功能的定制都是这些 WebMvcConfigurer  合起来一起生效
-   自动配置了一些非常底层的组件。**RequestMappingHandlerMapping**、这些组件依赖的组件都是从容器中获取

-   **public class** DelegatingWebMvcConfiguration **extends** **WebMvcConfigurationSupport**

-   4、**WebMvcAutoConfiguration** 里面的配置要能生效 必须  @ConditionalOnMissingBean(**WebMvcConfigurationSupport**.**class**)

-   5、@EnableWebMvc  导致了 **WebMvcAutoConfiguration  没有生效。****慎用**

    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154438942.png)

![在这里插入图片描述](D:\学习笔记\img\img01\20210717154448306.png)

![在这里插入图片描述](D:\学习笔记\img\img01\20210717154458310.png)


-   ... ...



### 2、原理分析套路

**场景starter** **- xxxxAutoConfiguration - 导入xxx组件 - 绑定xxxProperties --** **绑定配置文件项**









## 总结 

**请求处理完整流程**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154633403.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)




**拦截器流程**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154612676.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


















# 05 数据访问

## 一、SQL

### 1、数据源的自动配置

#### 1.导入JDBC场景

```xml
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jdbc</artifactId>
</dependency>
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154643947.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)




**数据库驱动**

官方没有导入驱动，因为官方不知道我们要操作什么数据库，所以需要我们自己手动导入

```xml
<!--官方给我们的数据库驱动做了版本仲裁，默认是8版本的-->
<mysql.version>8.0.25</mysql.version>

<!--需要我们自己来修改版本-->
<!--第一种方式：直接引入-->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>5.1.49</version>
</dependency>

<!--第二种方式：重新声明版本-->
<properties>
    <java.version>1.8</java.version>
    <mysql.version>5.1.49</mysql.version>
</properties>
```



#### 2.分析自动配置

**DataSourceAutoConfiguration**：数据源的自动配置

-   修改数据源相关的配置：spring.datasource

    ```java
    @ConfigurationProperties(prefix = "spring.datasource")
    public class DataSourceProperties implements BeanClassLoaderAware, InitializingBean {
    ```

-   数据库连接池的配置，容器中没有数据源它才会配置

    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154657624.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


-   因此我们默认使用的就是springBoot给我们配置好的。**HikariDataSource**，因为引入jdbc场景的时候就引入了它的包



**DataSourceTransactionManagerAutoConfiguration**：事物管理器的自动配置

**JdbcTemplateAutoConfiguration**：JDBC模板的自动配置，它可以对数据库crud

**JndiDataSourceAutoConfiguration**：Jndi的自动配置

**XADataSourceAutoConfiguration**：分布式事物相关的自动配置



#### 3、修改配置项

```yml
spring:
  datasource:
    username: root
    password: root
    url: jdbc:mysql://localhost:3306/db4?studyserverTimezone=UTC&useUnicode=true&characterEncoding=utf-8
    driver-class-name: com.mysql.jdbc.Driver
```



#### 4、测试

```java
@SpringBootTest
class AdminApplicationTests {
    // springBoot帮我们自动引入了
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
        String sql = "select count(*) from stu";
        System.out.println(jdbcTemplate.queryForObject(sql, Integer.class));
    }
}
```





### 2、使用Druid数据源

**druid官方github地址**

https://github.com/alibaba/druid

**整合第三方技术的两种方式**：

-   自定义
-   官方starter



#### 1.自定义方式

**中文文档**：https://github.com/alibaba/druid/wiki/%E5%B8%B8%E8%A7%81%E9%97%AE%E9%A2%98



##### ①引入依赖

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid</artifactId>
    <version>1.1.17</version>
</dependency>
```



##### ②创建数据源注入到容器中

可以使用xml文件的方式去配置，在这里使用配置类的方式配置

```java
@Configuration
public class DataSourceConfig {

    // 绑定配置文件之后，我们只需要修改配置文件中的就可以了
    @ConfigurationProperties(prefix = "spring.datasource")
    @Bean
    public DataSource dataSource() {
        DruidDataSource dataSource = new DruidDataSource();
//        dataSource.setUsername("root");     // 设置用户名
//        dataSource.setPassword("root");     // 设置密码
        return dataSource;
    }
}
```



**测试**

```java
@Autowired
DataSource dataSource;

@Test
void test1() {
    System.out.println(dataSource.getClass());
}
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154712109.png)




##### ③StatViewServlet

StatViewServlet的用途包括：

-   提供监控信息展示的html页面
-   提供监控信息的JSON API

```java
/**
 * 配置druid监控页功能
 * @return
 */
@Bean
public ServletRegistrationBean statViewServlet() {
    StatViewServlet statViewServlet = new StatViewServlet();
    ServletRegistrationBean<Servlet> servletServletRegistrationBean =
            new ServletRegistrationBean<>(statViewServlet,"/druid/*");
    return servletServletRegistrationBean;
}

// 输入网址访问http://localhost:8080/druid/datasource.html
```



##### ④监控统计功能

```java
// 绑定配置文件之后，我们只需要修改配置文件中的就可以了
@ConfigurationProperties(prefix = "spring.datasource")
@Bean
public DataSource dataSource() throws SQLException {
    DruidDataSource dataSource = new DruidDataSource();
    //        dataSource.setUsername("root");     // 设置用户名
    //        dataSource.setPassword("root");     // 设置密码
    dataSource.setFilters("stat");  // 开启监控功能
    return dataSource;
}
```

也可以使用配置文件

```yml
spring:
  datasource:
    filters: 'stat'
```

**结果显示**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154721109.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)




##### ⑤web和spring关联监控

```java
/**
 * web关联监控
 * @return
 */
@Bean
public FilterRegistrationBean webStatFilter() {
    WebStatFilter webStatFilter = new WebStatFilter();
    FilterRegistrationBean<Filter> filterFilterRegistrationBean =
        new FilterRegistrationBean(webStatFilter);
    filterFilterRegistrationBean.setUrlPatterns(Arrays.asList("/*"));
  filterFilterRegistrationBean.addInitParameter("exclusions","*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*");
    return filterFilterRegistrationBean;
}
```



##### ⑥设置登录账号和密码

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154732893.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154740340.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)




##### ⑦不懂看官网，有教程



#### 2.官方starter

**官方文档**：https://github.com/alibaba/druid/tree/master/druid-spring-boot-starter



##### ①引入官方的stater

```xml
<dependency>
   <groupId>com.alibaba</groupId>
   <artifactId>druid-spring-boot-starter</artifactId>
   <version>1.1.17</version>
</dependency>
```



##### ②Druid自动配置

```java
@Configuration
@ConditionalOnClass(DruidDataSource.class)
@AutoConfigureBefore(DataSourceAutoConfiguration.class)	// 它要在spring默认的数据源启动前启动
// 和配合文件进行绑定
// DruidStatProperties	spring.datasource.druid
@EnableConfigurationProperties({DruidStatProperties.class, DataSourceProperties.class})
@Import({DruidSpringAopConfiguration.class,
    DruidStatViewServletConfiguration.class,
    DruidWebStatFilterConfiguration.class,
    DruidFilterConfiguration.class})
public class DruidDataSourceAutoConfigure {
```

-   **DruidSpringAopConfiguration**	aop相关的
-   **DruidStatViewServletConfiguration**    监控页的配置，默认开启
-   **DruidWebStatFilterConfiguration**    监控配置，默认开启
-   **DruidFilterConfiguration**    Druid自己的Filter配置
    -   ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154753750.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)




**代码实现**

```yml
spring:
  datasource:
    username: root
    password: root
    url: jdbc:mysql://localhost:3306/db4?studyserverTimezone=UTC&useUnicode=true&characterEncoding=utf-8
    driver-class-name: com.mysql.jdbc.Driver

    druid:
      initial-size: 5   # 初始数
      max-active: 10    # 最大活动数
      aop-patterns: com.xiaozhi.admin.*  # 监控SpringBean
      filters: stat,wall     # 底层开启功能，stat（sql监控），wall（防火墙）

      stat-view-servlet:  # 配置监控页功能
        login-username: admin
        login-password: admin
        url-pattern: /druid/*
        enabled: true

      web-stat-filter:  # 监控web
        enabled: true
        exclusions: "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*"
        url-pattern: /*

      filter:
        stat:   # 对上面filters里面的stat的详细配置
          slow-sql-millis: 1000
          enabled: true
        wall:
          enabled: true
          config:
            drop-table-allow: false
```





### 3、整合mybatis操作

https://github.com/mybatis

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.1.4</version>
</dependency>
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154803355.png)




#### 1.配置模式

-   全局配置文件

-   **SqlSessionFactory**    自动配好了

-   **SqlSessionTemplate**   自动配好了

-   **@EnableConfigurationProperties(MybatisProperties.class)**     绑定了配置文件 mybatis.xxxx

-   ```java
    @org.springframework.context.annotation.Configuration
    // 扫描标注有@Mapper注解的接口
    @Import(AutoConfiguredMapperScannerRegistrar.class)
    @ConditionalOnMissingBean({ MapperFactoryBean.class, MapperScannerConfigurer.class })
    public static class MapperScannerRegistrarNotFoundConfiguration implements InitializingBean {
    ```



**注意**：一定要给mapper接口标注@Mapper注解，要不然就使用@MapperScan()注解扫描进去，不然会识别不了



##### 配置方式

```yml
mybatis:
  config-location: classpath:mybatis/mybatis-config.xml  # 全局配置文件位置
  mapper-locations: classpath:mybatis/mapper/*.xml  #sql映射文件位置
  type-aliases-package: com.xiaozhi.admin.pojo  # 开启类别名
```

也可以使用全部都是配置文件的方式

```yml
mybatis:
  # 如果使用configuration配置项，那么就不需要全局配置文件了，两个只能存在一个，同时存在会报错
  #  config-location: classpath:mybatis/mybatis-config.xml  # 全局配置文件位置
  mapper-locations: classpath:mybatis/mapper/*.xml  #sql映射文件位置
  type-aliases-package: com.xiaozhi.admin.pojo  # 开启类别名
  configuration:  # 相当于全局配置文件的配置项
    map-underscore-to-camel-case: true  # 开启驼峰命名
```



##### **测试**

**sql语句**

```sql
CREATE TABLE city
(
  id      INT(11) PRIMARY KEY AUTO_INCREMENT,
  NAME    VARCHAR(30) ,
  state   VARCHAR(30),
  country VARCHAR(30)
);
```

**实体类**

```java
@Data
public class City {

    private Long id;
    private String name;
    private String state;
    private String country;
}
```

**Mapper接口**

```java
@Mapper
public interface CityMapper {

    void insert(City city);
}
```

也可以

```java
@MapperScan(basePackages = "com.xaiozhi.admin.mapper")
@Configuration
public class MybatisConfig {
}
```

**编写映射文件**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.xiaozhi.admin.mapper.CityMapper">

   <insert id="insert">
       INSERT INTO city (name, state, country) VALUES(#{name}, #{state}, #{country})
   </insert>

</mapper>
```

**测试**

```java
@Test
public void Test2(){
    cityMapper.insert(new City(null, "111", "222", "333"));
}

@Test
public void Test3(){
    System.out.println(cityMapper.findById(1L));
}
```







#### 2.注解模式

```java
@Mapper
public interface CityMapper {

    @Select("select * from city where id = #{id}")
    void insert(City city);

    @Insert("INSERT INTO city (name, state, country) VALUES(#{name}, #{state}, #{country})")
    City findById(long id);
}
```



##### 扩展

注解也可以进行一些较复杂的动作

编写一个完整的开发流程

**service**

```java
@Service
public class CityServiceImpl implements CityService {
    
    @Autowired
    CityMapper cityMapper;
    
    @Override
    public void saveCity(City city) {
        cityMapper.insert(city);
    }
}
```

**controller**

```java
@Controller
public class CityController {

    @Autowired
    CityService cityService;

    @ResponseBody
    @PostMapping("/city")
    public City city(City city) {
        cityService.saveCity(city);
        return city;
    }
}
```

**使用postman进行测试**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154818404.png)


返回的id是为null的，我们想要插入的同时也要设置我们的id值，我们可以这样做

**配置文件的形式**

```java
   <insert id="insert" useGeneratedKeys="true" keyProperty="id">
       INSERT INTO city (name, state, country) VALUES(#{name}, #{state}, #{country})
   </insert>
```

**使用注解**

使用@Options注解放我们其他的功能

```java
@Insert("INSERT INTO city (name, state, country) VALUES(#{name}, #{state}, #{country})")
@Options(useGeneratedKeys = true, keyProperty = "id")
void insert(City city);
```

**结果显示**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154827758.png)




#### 3.混合模式

混合模式就是注解 + xml映射文件

简单的sql语句我们可以使用注解

复杂的语句我们可以使用映射文件来处理



### 4、整合Mybatis-Plus完成CRUD

https://mp.baomidou.com/



##### 1.引入stater

```xml
<dependency>
      <groupId>com.baomidou</groupId>
      <artifactId>mybatis-plus-boot-starter</artifactId>
      <version>3.4.1</version>
</dependency>
```

它引入了mybatis的整合包，所以引入它就可以有mybatis的全部功能了

==**注意**：它和配置文件绑定的前缀是mybatis-plus==

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154836843.png)




##### 2.按照官方的文档来学习













## 二、NoSQL

Redis 是一个开源（BSD许可）的，内存中的数据结构存储系统，它可以用作数据库、**缓存**和消息中间件。 它支持多种类型的数据结构，如 [字符串（strings）](http://www.redis.cn/topics/data-types-intro.html#strings)， [散列（hashes）](http://www.redis.cn/topics/data-types-intro.html#hashes)， [列表（lists）](http://www.redis.cn/topics/data-types-intro.html#lists)， [集合（sets）](http://www.redis.cn/topics/data-types-intro.html#sets)， [有序集合（sorted sets）](http://www.redis.cn/topics/data-types-intro.html#sorted-sets) 与范围查询， [bitmaps](http://www.redis.cn/topics/data-types-intro.html#bitmaps)， [hyperloglogs](http://www.redis.cn/topics/data-types-intro.html#hyperloglogs) 和 [地理空间（geospatial）](http://www.redis.cn/commands/geoadd.html) 索引半径查询。 Redis 内置了 [复制（replication）](http://www.redis.cn/topics/replication.html)，[LUA脚本（Lua scripting）](http://www.redis.cn/commands/eval.html)， [LRU驱动事件（LRU eviction）](http://www.redis.cn/topics/lru-cache.html)，[事务（transactions）](http://www.redis.cn/topics/transactions.html) 和不同级别的 [磁盘持久化（persistence）](http://www.redis.cn/topics/persistence.html)， 并通过 [Redis哨兵（Sentinel）](http://www.redis.cn/topics/sentinel.html)和自动 [分区（Cluster）](http://www.redis.cn/topics/cluster-tutorial.html)提供高可用性（high availability）。



### 1、reids自动配置

#### ①引入starter

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154854442.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


默认导入的是kettuce客户端，需要jedis的需要导入jedis的包，还需要手动开启



#### ②自动配置

```java
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RedisOperations.class)
@EnableConfigurationProperties(RedisProperties.class)
@Import({ LettuceConnectionConfiguration.class, JedisConnectionConfiguration.class })
public class RedisAutoConfiguration {
```

-   绑定了配置文件  **spring.redis.xxxx**

-   @Import({ **LettuceConnectionConfiguration**.class, **JedisConnectionConfiguration**.class })    支持的两个客户端

    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154907409.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


    如果想要使用jedis就需要导入jedis的包，还需要手动修改客户端类型


​    

-   **RedisTemplate<Object, Object>**    自动配好了

-   **StringRedisTemplate**    自动配好了

-   我们使用RedisTemplate和 **StringRedisTemplate**    操作redis数据库就可以了



### 2、RedisTemplate与Lettuce

```yml
# redis配置
spring
  redis:
    host: 192.168.6.156
    port: 6379
    # 有密码的要加上密码
```

```java
@Autowired
StringRedisTemplate redisTemplate;

@Test
public void Test4(){
    ValueOperations<String, String> operations = redisTemplate.opsForValue();
    operations.set("hello", "world");
    String hello = operations.get("hello");
    System.out.println(hello);
}
```







### 3、切换到jedis

```xml
<!-- 导入jedis -->
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
</dependency>
```

```yml
  # redis配置
spring
  redis:
    host: 192.168.6.156
    port: 6379
    # 有密码的要加上密码
    client-type: jedis    # 修改客户端为jedis
    # 修改jedis客户端的配置
    jedis:
      pool:
        max-active: 10
        max-wait: 1s
```



### 4、使用redis来记录访问次数

**使用拦截器来处理**

```java
@Component
public class RedisInterceptor implements HandlerInterceptor {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        System.out.println(uri);
        redisTemplate.opsForValue().increment(uri); // 每次访问 + 1
        return true;
    }
}
```

```java
@Configuration
public class MyConfig implements WebMvcConfigurer {

    @Autowired
    RedisInterceptor redisInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(redisInterceptor)
                .addPathPatterns("/*")
                // 放行所有的静态资源
                .excludePathPatterns("/","/login","/css/**","/js/**","/fonts/**","/images/**");
    }
}
```







## 三、事物处理

### 1、Sql事物







### 2、NoSql事物







# 06	单元测试

## 1.JUnit5变化

springBoot2.2版本开始引入JUnit5作为单元测试默认库

作为最新版本的JUnit框架，JUnit5与之前版本的Junit框架有很大的不同。由三个不同子项目的几个不同模块组成。

**JUnit 5 = JUnit Platform + JUnit Jupiter + JUnit Vintage**

**JUnit Platform**: Junit Platform是在JVM上启动测试框架的基础，不仅支持Junit自制的测试引擎，其他测试引擎也都可以接入。

**JUnit Jupiter**: JUnit Jupiter提供了JUnit5的新的编程模型，是JUnit5新特性的核心。内部 包含了一个**测试引擎**，用于在Junit Platform上运行。

**JUnit Vintage**: 由于JUint已经发展多年，为了照顾老的项目，JUnit Vintage提供了兼容JUnit4.x,Junit3.x的测试引擎。

![img](https://img-blog.csdnimg.cn/img_convert/234bb64a1accdc1cae6161ec4bbb199b.png)



**注意**：

springBoot2.4版本开始移除了对Vintage的依赖，如果需要使用JUnit4需要自行引入



我们只需要引入单元测试的starter就可以使用对应的功能了

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

**SpringBoot整合Junit以后**

-   编写测试方法：@Test标注（注意需要使用junit5版本的注解）
-   Junit类具有Spring的功能，@Autowired、比如 @Transactional 标注测试方法，测试完成后自动回滚



## 2.JUnit5常用注解

JUnit5的注解与JUnit4的注解有所变化

https://junit.org/junit5/docs/current/user-guide/#writing-tests-annotations

-   **@Test :**表示方法是测试方法。但是与JUnit4的@Test不同，他的职责非常单一不能声明任何属性，拓展的测试将会由Jupiter提供额外测试
-   **@ParameterizedTest :**表示方法是参数化测试，下方会有详细介绍

-   **@RepeatedTest :**表示方法可重复执行，下方会有详细介绍
-   **@DisplayName :**为测试类或者测试方法设置展示名称

-   **@BeforeEach :**表示在每个单元测试之前执行
-   **@AfterEach :**表示在每个单元测试之后执行

-   **@BeforeAll :**表示在所有单元测试之前执行
-   **@AfterAll :**表示在所有单元测试之后执行

-   **@Tag :**表示单元测试类别，类似于JUnit4中的@Categories
-   **@Disabled :**表示测试类或测试方法不执行，类似于JUnit4中的@Ignore

-   **@Timeout :**表示测试方法运行如果超过了指定时间将会返回错误
-   **@ExtendWith :**为测试类或测试方法提供扩展类引用



**测试**

```java
@DisplayName("测试JUnit5")
@SpringBootTest
public class JUnit5Test {

    @BeforeEach // 方法执行前执行
    @Test
    public void beforeEach(){
        System.out.println("测试方法执行前执行");
    }

    @BeforeAll
    @Test
    public static void Test(){
        System.out.println("所有测试之前执行");
    }

    @Test
    public void Test2(){
        System.out.println(1);
    }

    @DisplayName("测试Disabled")
    @Disabled
    @Test
    public void Test3(){
        System.out.println("我是不会执行的");
    }

    @AfterEach  // 测试方法执行后执行
    @Test
    public void Test4(){
        System.out.println("测试方法执行完成后执行");
    }

    // 执行之间超过400毫秒报错
    @Timeout(value = 400, unit = TimeUnit.MILLISECONDS)
    @Test
    public void Test5() throws InterruptedException {
        Thread.sleep(500);
    }
}
```



## 3.断言(assertions)

**断言就是断定会怎么怎么样，如果不是，就不成功或者报错**

断言（assertions）是测试方法中的核心部分，用来对测试需要满足的条件进行验证。**这些断言方法都是 org.junit.jupiter.api.Assertions 的静态方法**。JUnit 5 内置的断言可以分成如下几个类别：

**检查业务逻辑返回的数据是否合理。**

**所有的测试运行结束以后，会有一个详细的测试报告；**



### ①简单断言

用来对单个值进行简单的验证。如：

| 方法            | 说明                                 |
| --------------- | ------------------------------------ |
| assertEquals    | 判断两个对象或两个原始类型是否相等   |
| assertNotEquals | 判断两个对象或两个原始类型是否不相等 |
| assertSame      | 判断两个对象引用是否指向同一个对象   |
| assertNotSame   | 判断两个对象引用是否指向不同的对象   |
| assertTrue      | 判断给定的布尔值是否为 true          |
| assertFalse     | 判断给定的布尔值是否为 false         |
| assertNull      | 判断给定的对象引用是否为 null        |
| assertNotNull   | 判断给定的对象引用是否不为 null      |



```java
@DisplayName("简单断言")
@Test
public void Test1(){
    // 第三个参数就是报错给出的消息，跟异常的message一样
    assertEquals(1,1,"两个数字不相等");    // 比较值
    // 判断两个对象是否一致
    assertSame(new Object(), new Object(), "两个对象不相等");
}
```



### ②数组断言

通过 assertArrayEquals 方法来判断两个对象或原始类型的数组是否相等

```java
@DisplayName("数组断言")
@Test
public void Test2(){
    assertArrayEquals(new int[]{1, 2}, new int[]{1, 2}, "两个数组不相等");
}
```



### ③组合断言

assertAll 方法接受多个 org.junit.jupiter.api.Executable 函数式接口的实例作为要验证的断言，可以通过 lambda 表达式很容易的提供这些断言

```java
@DisplayName("组合断言")
@Test
public void Test3(){
    Object o = new Object();
    Object o1 = new Object();
    assertAll("Math",
            () -> assertEquals(2, 2, "两个值不相等"),
            () -> assertTrue(1 > 0),
            () -> assertSame(o, o1, "不是同一个对象")
    );
}
```



### ④异常断言

在JUnit4时期，想要测试方法的异常情况时，需要用**@Rule**注解的ExpectedException变量还是比较麻烦的。而JUnit5提供了一种新的断言方式**Assertions.assertThrows()** ,配合函数式编程就可以进行使用

```java
@DisplayName("异常测试")
@Test
public void Test4(){
    assertThrows(
            // 抛出断言异常，没抛出就是没有成功
            ArithmeticException.class,
            () -> {
                System.out.println(1 / 0);
            }
    );
}
```



### ⑤超时断言

Junit5还提供了**Assertions.assertTimeout()** 为测试方法设置了超时时间

```java
@DisplayName("超时断言")
@Test
public void Test(){
    // 测试方法执行事件超过1s就会报错
    assertTimeout(Duration.ofMillis(1000), () -> {Thread.sleep(500);});
}
```



### ⑥快速失败

通过 fail 方法直接使得测试失败

```java
@DisplayName("快速失败")
@Test
public void Test6(){
    fail("快速失败");   // 提示信息
}
```



## 4.前置条件（assumptions）

JUnit 5 中的前置条件（**assumptions【假设】**）类似于断言，不同之处在于**不满足的断言会使得测试方法失败**，而不满足的**前置条件只会使得测试方法的执行终止**。前置条件可以看成是测试方法执行的前提，当该前提不满足时，就没有继续执行的必要。

```java
@DisplayName("测试前置条件")
@SpringBootTest
public class TestAssumptions {

    private final String environment = "DEV";

    @DisplayName("simple")
    @Test
    public void Test1(){
        // assumeTrue和assumeFalse确保给定条件为true或false，不满足条件会使得测试停止
        assumeTrue(Objects.equals(this.environment, "DEV"));
        assumeFalse(() -> Objects.equals(this.environment, "PROD"));
    }

    @DisplayName("assume then do")
    @Test
    public void Test2(){
        // assumingThat 的参数是表示条件的布尔值和对应的 Executable 接口的实现对象。
        // 只有条件满足时，Executable 对象才会被执行；当条件不满足时，测试执行并不会终止。
        assumingThat(
                Objects.equals(this.environment, "DEV"),
                () -> System.out.println("In DEV")
        );
    }
}
```



## 5.嵌套测试

JUnit 5 可以通过 Java 中的内部类和@Nested 注解实现嵌套测试，从而可以更好的把相关的测试方法组织在一起。在内部类中可以使用@BeforeEach 和@AfterEach 注解，而且嵌套的层次没有限制。

外面的不能使用里面的@Before和@after中的测试方法，内嵌的可以使用外部的@Before和@After的测试方法

```java
@DisplayName("测试嵌套测试")
@SpringBootTest
public class TestNested {

    Stack<Object> stack;

    @Test
    @DisplayName("is instantiated with new Stack()")
    void isInstantiatedWithNew() {
        new Stack<>();
    }

    // 不能使用内嵌的before，所以对象为null，会报错
    @Test
    public void Test(){
        assertNotNull(stack, "对象为null");
    }

    @Nested
    @DisplayName("when new")
    class WhenNew {

        @BeforeEach
        void createNewStack() {
            stack = new Stack<>();
        }

        @Test
        @DisplayName("is empty")
          void isEmpty() {
            assertTrue(stack.isEmpty());
        }

        @Test
        @DisplayName("throws EmptyStackException when popped")
        void throwsExceptionWhenPopped() {
            assertThrows(EmptyStackException.class, stack::pop);
        }

        @Test
        @DisplayName("throws EmptyStackException when peeked")
        void throwsExceptionWhenPeeked() {
            assertThrows(EmptyStackException.class, stack::peek);
        }

        @Nested
        @DisplayName("after pushing an element")
        class AfterPushing {

            String anElement = "an element";

            @BeforeEach
            void pushAnElement() {
                stack.push(anElement);
            }

            @Test
            @DisplayName("it is no longer empty")
            void isNotEmpty() {
                assertFalse(stack.isEmpty());
            }

            @Test
            @DisplayName("returns the element when popped and is empty")
            void returnElementWhenPopped() {
                assertEquals(anElement, stack.pop());
                assertTrue(stack.isEmpty());
            }

            @Test
            @DisplayName("returns the element when peeked but remains not empty")
            void returnElementWhenPeeked() {
                assertEquals(anElement, stack.peek());
                assertFalse(stack.isEmpty());
            }
        }
    }
}
```



## 6.参数化测试

参数化测试是JUnit5很重要的一个新特性，它使得用不同的参数多次运行测试成为了可能，也为我们的单元测试带来许多便利。

利用**@ValueSource**等注解，指定入参，我们将可以使用不同的参数进行多次单元测试，而不需要每新增一个参数就新增一个单元测试，省去了很多冗余代码。



**@ValueSource**: 为参数化测试指定入参来源，支持八大基础类以及String类型,Class类型

**@NullSource**: 表示为参数化测试提供一个null的入参

**@EnumSource**: 表示为参数化测试提供一个枚举入参

**@CsvFileSource**：表示读取指定CSV文件内容作为参数化测试入参

**@MethodSource**：表示读取指定方法的返回值作为参数化测试入参(注意方法返回需要是一个流)



当然如果参数化测试仅仅只能做到指定普通的入参还达不到让我觉得惊艳的地步。让我真正感到他的强大之处的地方在于他可以支持外部的各类入参。如:CSV,YML,JSON 文件甚至方法的返回值也可以作为入参。只需要去实现**ArgumentsProvider**接口，任何外部文件都可以作为它的入参。

```java
@DisplayName("测试参数化测试")
@SpringBootTest
public class TestSource {

    @ParameterizedTest
    @ValueSource(strings = {"one", "two", "three"})
    @DisplayName("参数化测试")
    @Test
    public void Test1(String string){
        System.out.println(string);
        assertTrue(StringUtils.isNotBlank(string));
    }

    @ParameterizedTest
    @MethodSource("method")     // 指定方法名
    @DisplayName("方法来源参数")
    @Test
    public void Test2(String name){
        System.out.println(name);
        assertNotNull(name);
    }

    // 必须是static的
    static Stream<String> method() {
        return Stream.of("apple", "banana");
    }
}
```



## 7.不会参考官方文档

https://junit.org/junit5/docs/current/user-guide/#writing-tests-parameterized-tests



# 07	指标监控

## 1.SpringBoot Actuator

### ①简介

未来每一个微服务在云上部署以后，我们都需要对其进行监控、追踪、审计、控制等。SpringBoot就抽取了Actuator场景，使得我们每个微服务快速引用即可获得生产级别的应用监控、审计等功能。



### ②1.x与2.x的不同

![img](https://img-blog.csdnimg.cn/img_convert/88d6c142bde6fd8b22723e7ecfade0c5.png)



### ③如何使用

**引入指标监控的stater**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

只要引入场景我们就可以使用指标监控功能了

-   访问 http://localhost:8080/actuator/**，默认暴露这几个端点

-   ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154941863.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


-   默认是开启了health和info的web展示

-   暴露所有监控信息为http

-   ```yml
    management:
      endpoints:  # 所有的端点配置
        enabled-by-default: true  # 暴露所有端点
        web:
          exposure:
            include: '*'  # 以web的方式暴露
      endpoint:   # 使用单个的端点配置
        health:
          show-details: always    # 总是展示详细信息
    ```

**测试**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154950965.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717154959565.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)




**cmd输入jconsole打开java的指标监控功能，原生的JMX**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155007994.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)






## 2.Actuator Endpoint

### ①最常使用的端点

| ID                 | 描述                                                         |
| ------------------ | ------------------------------------------------------------ |
| `auditevents`      | 暴露当前应用程序的审核事件信息。需要一个`AuditEventRepository组件`。 |
| `beans`            | 显示应用程序中所有Spring Bean的完整列表。                    |
| `caches`           | 暴露可用的缓存。                                             |
| `conditions`       | 显示自动配置的所有条件信息，包括匹配或不匹配的原因。         |
| `configprops`      | 显示所有`@ConfigurationProperties`。                         |
| `env`              | 暴露Spring的属性`ConfigurableEnvironment`                    |
| `flyway`           | 显示已应用的所有Flyway数据库迁移。 需要一个或多个`Flyway`组件。 |
| `health`           | 显示应用程序运行状况信息。                                   |
| `httptrace`        | 显示HTTP跟踪信息（默认情况下，最近100个HTTP请求-响应）。需要一个`HttpTraceRepository`组件。 |
| `info`             | 显示应用程序信息。                                           |
| `integrationgraph` | 显示Spring `integrationgraph` 。需要依赖`spring-integration-core`。 |
| `loggers`          | 显示和修改应用程序中日志的配置。                             |
| `liquibase`        | 显示已应用的所有Liquibase数据库迁移。需要一个或多个`Liquibase`组件。 |
| `metrics`          | 显示当前应用程序的“指标”信息。                               |
| `mappings`         | 显示所有`@RequestMapping`路径列表。                          |
| `scheduledtasks`   | 显示应用程序中的计划任务。                                   |
| `sessions`         | 允许从Spring Session支持的会话存储中检索和删除用户会话。需要使用Spring Session的基于Servlet的Web应用程序。 |
| `shutdown`         | 使应用程序正常关闭。默认禁用。                               |
| `startup`          | 显示由`ApplicationStartup`收集的启动步骤数据。需要使用`SpringApplication`进行配置`BufferingApplicationStartup`。 |
| `threaddump`       | 执行线程转储。                                               |





如果您的应用程序是Web应用程序（Spring MVC，Spring WebFlux或Jersey），则可以使用以下附加端点：

| ID           | 描述                                                         |
| ------------ | ------------------------------------------------------------ |
| `heapdump`   | 返回`hprof`堆转储文件。                                      |
| `jolokia`    | 通过HTTP暴露JMX bean（需要引入Jolokia，不适用于WebFlux）。需要引入依赖`jolokia-core`。 |
| `logfile`    | 返回日志文件的内容（如果已设置`logging.file.name`或`logging.file.path`属性）。支持使用HTTP`Range`标头来检索部分日志文件的内容。 |
| `prometheus` | 以Prometheus服务器可以抓取的格式公开指标。需要依赖`micrometer-registry-prometheus`。 |



最常用的Endpoint

-   **Health：监控状况**
-   **Metrics：运行时指标**

-   **Loggers：日志记录**



### ②Health Endpoint

健康检查端点，我们一般用于在云平台，平台会定时的检查应用的健康状况，我们就需要Health Endpoint可以为平台返回当前应用的一系列组件健康状况的集合。

重要的几点：

-   health endpoint返回的结果，应该是一系列健康检查后的一个汇总报告
-   很多的健康检查默认已经自动配置好了，比如：数据库、redis等

-   可以很容易的添加自定义的健康检查机制

![img](https://img-blog.csdnimg.cn/img_convert/375e6b2d4b0dcaa76b8b12e4ea2cadd7.png)



### ③Metrics Endpoint

提供详细的、层级的、空间指标信息，这些信息可以被pull（主动推送）或者push（被动获取）方式得到；

-   通过Metrics对接多种监控系统
-   简化核心Metrics开发

-   添加自定义Metrics或者扩展已有Metrics



### ④管理Endpoions

#### 1.开启与禁用Endpoions

-   默认开启所有的Endpoint都是开启的（除了shutdown）

-   需要开启或者禁用某个Endpoint。可以是用单个endpoint来开启或者禁用

    -   ```yml
        management:
          endpoint:
            beans:
              enabled: true		# false就是禁用
        ```



#### 2.暴露Endpoints

**支持的暴露方式**

-   HTTP：默认只暴露**health**和**info** Endpoint
-   **JMX**：默认暴露所有Endpoint

-   除过health和info，剩下的Endpoint都应该进行保护访问。如果引入SpringSecurity，则会默认配置安全访问规则

| ID                 | JMX  | Web  |
| ------------------ | ---- | ---- |
| `auditevents`      | Yes  | No   |
| `beans`            | Yes  | No   |
| `caches`           | Yes  | No   |
| `conditions`       | Yes  | No   |
| `configprops`      | Yes  | No   |
| `env`              | Yes  | No   |
| `flyway`           | Yes  | No   |
| `health`           | Yes  | Yes  |
| `heapdump`         | N/A  | No   |
| `httptrace`        | Yes  | No   |
| `info`             | Yes  | Yes  |
| `integrationgraph` | Yes  | No   |
| `jolokia`          | N/A  | No   |
| `logfile`          | N/A  | No   |
| `loggers`          | Yes  | No   |
| `liquibase`        | Yes  | No   |
| `metrics`          | Yes  | No   |
| `mappings`         | Yes  | No   |
| `prometheus`       | N/A  | No   |
| `scheduledtasks`   | Yes  | No   |
| `sessions`         | Yes  | No   |
| `shutdown`         | Yes  | No   |
| `startup`          | Yes  | No   |
| `threaddump`       | Yes  | No   |



## 3.定制 Endpoint

### ①定制Health

```java
@Component  // 放入到容器中
// 类名后面一定是HealthIndicator，规范
public class MyHealthIndicator extends AbstractHealthIndicator {

    /**
     * 真实的检查方法
     * @param builder
     * @throws Exception
     */
    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        // 创建map来存放我们想要展示的数据
        HashMap<String, Object> map = new HashMap<>();
        if (1 == 1){
//            builder.up();   // up就是健康的
            builder.status(Status.UP);
            map.put("count", 1);
            map.put("ms", 500);
        } else {
//            builder.down(); // down就是不健康的
            builder.status(Status.DOWN);
            map.put("msg", "连接超时");
        }
        // 详细信息
        builder.withDetail("code", 100);
        // 传入一个map，里面是我们想要展示的详细信息
        builder.withDetails(map);
    }
}
```

**结果显示**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155023765.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


**这个监控的名字就是我们的类名（除了HealthIndicator）**



### ②定制info信息

```java
@Component
public class ExampleInfoContributor implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        // 用法和之前的一样
        builder.withDetail("example", Collections.singletonMap("key", "value"));
    }
}
```

```yaml
info:
  appName: boot-admin
  version: 1.0
  mavenProjectName: @project.artifactId@    # 使用@@可以获取maven的pom文本值
  mavenProjectVersion: @project.version@
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155033950.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)




### ③定制Metrics信息

#### 1.SpringBoot支持自动适配的Metrics

-   JVM metrics, report utilization of:

-   Various memory and buffer pools
-   Statistics related to garbage collection

-   Threads utilization
-   Number of classes loaded/unloaded

-   CPU metrics
-   File descriptor metrics

-   Kafka consumer and producer metrics
-   Log4j2 metrics: record the number of events logged to Log4j2 at each level

-   Logback metrics: record the number of events logged to Logback at each level
-   Uptime metrics: report a gauge for uptime and a fixed gauge representing the application’s absolute start time

-   Tomcat metrics (`server.tomcat.mbeanregistry.enabled` must be set to `true` for all Tomcat metrics to be registered)
-   [Spring Integration](https://docs.spring.io/spring-integration/docs/5.4.1/reference/html/system-management.html#micrometer-integration) metrics



#### 2、增加定制Metrics

```java
@Controller
public class CityController {

    @Autowired
    CityService cityService;

    Counter counter;
    public CityController(MeterRegistry meterRegistry) {
        counter = meterRegistry.counter("myservice.saveCity.count");
    }

    @ResponseBody
    @PostMapping("/city")
    public City saveCity(City city) {
        cityService.saveCity(city);
        counter.increment();    // 调用一次就+1
        System.out.println(counter.count());    // 打印记录的次数
        return city;
    }
}
```

**结果显示**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155045620.png)


![在这里插入图片描述](https://img-blog.csdnimg.cn/2021071715505330.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)




**也可以使用@Bean注解注入到容器中**

```java
@Bean
MeterBinder queueSize(Queue queue) {
    return (registry) -> Gauge.builder("queueSize", queue::size).register(registry);
}
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155101665.png)




### ④定制Endpoint

在类上标注@Endpoint注解就表示这个类是一个端点类

**@ReadOperation**是它的读方法，也就是展示需要读取的数据

**@WriteOperation**写方法，它可以用来改变或者停止我们线上的功能



**代码实现**

```java
@Component
@Endpoint(id = "container")
public class DockerEndpoint  {

    @ReadOperation
    public Map getDockerInfo() {
        return Collections.singletonMap("info", "docker started...");
    }

    @WriteOperation
    public void stopDocker() {
        System.out.println("docker stop....");
    }
}
```

**结果**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155111274.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155120671.png)


**获取@ReadOperation标注的方法的返回值展示给我们**

打开jconsole，原生的JMX来执行 @WriteOperation标注的方法

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155130711.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155139687.png)




场景：开发**ReadinessEndpoint**来管理程序是否就绪，或者**Liveness****Endpoint**来管理程序是否存活；

当然，这个也可以直接使用 https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-features.html#production-ready-kubernetes-probes



## 4.可视化

https://github.com/codecentric/spring-boot-admin



### ①设置springBoot管理器

官方文档：https://codecentric.github.io/spring-boot-admin/2.3.1/#getting-started



**创建一个新的项目**

**引入依赖**

```xml
<dependency>
    <groupId>de.codecentric</groupId>
    <artifactId>spring-boot-admin-starter-server</artifactId>
    <version>2.3.1</version>
</dependency>
```

**开启功能**

```java
@EnableAdminServer	// 开启服务
@SpringBootApplication
public class ServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }

}
```

**修改端口号**

```properties
server.port=8888
```

**进行访问：http://localhost:8888/applications**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155152693.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)




### ②注册客户端应用程序

**引入依赖**

```xml
<dependency>
    <groupId>de.codecentric</groupId>
    <artifactId>spring-boot-admin-starter-client</artifactId>
    <version>2.3.1</version>
</dependency>
<!--暂时不需要-->
<!--        <dependency>-->
<!--            <groupId>org.springframework.boot</groupId>-->
<!--            <artifactId>spring-boot-starter-security</artifactId>-->
<!--        </dependency>-->
```



**配置应用属性**

```properties
spring.boot.admin.client.url=http://localhost:8888  # 服务端口号
management.endpoints.web.exposure.include=*  # web形式暴露所有端口
```



**查看是否注册成功**

![在这里插入图片描述](https://img-blog.csdnimg.cn/202107171552048.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155211286.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


**使用它我们就可以监控我们所有的端点了**



# 08	高级特性

## 1.Profile功能

### ①application-profile功能

-   默认配置文件 application.yaml	一定会加载

-   指定环境配置文件 aaplication-{env}.yml

-   激活指定环境

    -   配置文件激活     

        ```yml
        spring:
          profiles:
            active: dev		# 激活指定的环境
        
            # active: dev, druid 指定多个环境
        ```

    -   命令行激活：**java -jar xxx.jar --spring.profile.active=dev --person.name=major**

    -   修改配置文件任意值，命令行优先

-   默认配置与环境配置同时生效

-   同名配置项，profile配置优先，默认配置会别覆盖



**测试**

在类路径下创建两个文件，生产环境和测试环境

![在这里插入图片描述](https://img-blog.csdnimg.cn/2021071715522464.png)


```yml
server:
  port: 8080  # 默认配置

server:
  port: 8001  # 生产配置
  
server:
  port: 8002  # 测试配置
```

**使用默认配置来指定环境**

```yml
spring:
  profiles:
    active: test
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/2021071715523317.png)








### ②@Profile条件装配功能

@Profile注解功能就是只有当前是指定环境的时候，这个类的的功能才会生效



#### 1.标注在类上

```java
public interface Person {

    String getName();
    Integer getAge();
}
```

```java
@Profile("test")    // 测试配置下生效
@ConfigurationProperties("person")
@Component
@Data
public class Boss implements Person {
    private String name;
    private Integer age;
}
```

```java
@Profile("pro")    // 生产配置有效
@ConfigurationProperties("person")
@Component
@Data
public class Worker implements Person{
    private String name;
    private Integer age;
}
```



**controller编写**

```java
@RestController
public class PersonController {

    @Autowired
    Person person;

    @GetMapping("/person")
    public String person() {
        return person.getName() + "和" + person.getAge();
    }
}
```



**配置文件编写**

```yml
# 默认配置
spring:
  profiles:
    active: test
    
# 生产环境
person:
  name: 员工
  age: 19

# 测试环境
person:
  name: 老板
  age: 40
```

**结果显示**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155243530.png)






#### 2.标注在方法上

```java
@Data
public class Color {

    private String name;
}
```

```java
@Configuration
public class MyConfig {

    @Profile("test")	// test下有效
    @Bean
    public Color color() {
        Color color = new Color();
        color.setName("red");
        return color;
    }

    @Profile("pro")		// pro下有效
    @Bean
    public Color color2() {
        Color color = new Color();
        color.setName("blue");
        return color;
    }
}
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/202107171553056.png)






## 2.外部话配置

https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config



### ①外部配置源

外部配置源常用：**properties文件**、**YAML文件**、**环境变量**、**命令行参数**；



### ②配置文件查找位置

(1) classpath 根路径

(2) classpath 根路径下config目录

(3) jar包当前目录

(4) jar包当前目录的config目录

(5) /config子目录的直接子目录



### ③配置文件加载顺序

1.  当前jar包内部的application.properties和application.yml
2.  当前jar包内部的application-{profile}.properties 和 application-{profile}.yml
3.  引用的外部jar包的application.properties和application.yml
4.  引用的外部jar包的application-{profile}.properties 和 application-{profile}.yml



### ④指定环境优先，外部优先，后面的可以覆盖前面的同名配置项





## 3.自定义starter

①创建一个空的java项目 **customer-starter**

②在空的java项目中创建一个空的的maven项目和一个SpringBoot项目（名字随你起）

maven：**xiaozhi-hello-spring-boot-starte（启动器）**

springBoot项目：**xiaozhi-hello-spring-boot-starter-autoconfigure（自动配置包）**

③空的maven项目将springBoot项目引入进来，maven项目的主要作用就是引用我们的springBoot项目

④在springBoot项目中编写我们的starter代码

-   可以仿照springBoot的来做



**代码实现**

### 1.创建项目并引入依赖

①创建springBoot项目，只留下启动器就好了

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155337818.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


②创建maven项目，并引入springBoot项目的依赖

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155350654.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)




### 2.在springBoot项目中编写代码

#### ①创建配置类

```java
@ConfigurationProperties("xiaozhi.name")
public class HelloProperties {

    private String prefix;
    private String suffix;

	get和set方法。。。
}
```

#### ②编写功能类

```java
public class HelloService {
    
    @Autowired
    HelloProperties helloProperties;
    
    public String getName(String username) {
        return helloProperties.getPrefix() + username + helloProperties.getSuffix();
    } 
}
```

#### ③编写配置类

编写配置类，然后将功能类注入到容器中

```java
@Controller
@EnableConfigurationProperties(HelloProperties.class)
public class HelloServiceAutoConfiguration {
    
    // 注入写好的功能类
    @ConditionalOnMissingBean(HelloService.class)   // 没有这个类型的组件注入
    @Bean
    public HelloService helloService(){
        return new HelloService();
    }
}
```

#### ④创建spring.factories文件

在类路径下创建META-INF文件，再创建spring.factories文件，内容是我们需要到入的xxxAutoConfiguration类

```factories
# Auto Configure
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.xiaozhi.hello.auto.HelloServiceAutoConfiguration
```

#### ⑤下载到本地仓库

先将自动配置包clear再install下载到本地maven仓库，再将启动器clear再install



### 3.测试

#### ①引入我们自定义的starter

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155408593.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)




#### ②编写业务代码

**controller**

```java
@RestController
public class HelloController {

    @Autowired
    HelloService helloService;

    @GetMapping("/hello")
    public String hello() {
        String xiaozhi = helloService.getName("小智");
        return xiaozhi;
    }
}
```

**配置文件**

```yml
xiaozhi:
  name:
    prefix: 中国的
    suffix: 是最帅的
```

**结果**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155430214.png)




# 09	原理解析



## 1.springBoot启动过程

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155438298.png)


可以拆分成两个部分，一个是**创建SpringApplication**和**运行SpringApplication**



### ①创建SpringApplication

1.  创建一个集合，放入我们的主启动类

2.  判断当前应用的类型（servlet类型）

    -   ClassUtils.isPresent（）方法判断

3.  **bootstrappers**：初始启动引导器。 去**spring.factories**文件中找**org.springframework.boot.Bootstrapper**

    ==getSpringFactoriesInstances(Class<T> type, Class<?>[] parameterTypes, Object... args)==

    -   往下执行到**getSpringFactoriesInstances**方法中通过**loadFactoryNames**方法得到需要导入的类名

    -   **loadFactoryNames**方法调用**loadSpringFactories**方法来**加载spring.factories文件**得到需要的类名

        1.  首先它会从缓存中拿取数据，第一次是没有数据的

        2.  第一次它会加载**spring.factories**文件，然后通过Properties去加载得到kv键值对，value是List类型

            ![在这里插入图片描述](https://img-blog.csdnimg.cn/2021071716001214.png)


            ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717160020358.png)


        3.  将加载的信息放到缓存中，下次来就不需要加载spring.factories文件了
    
        4.  最终返回一个Map<String, List<String>>集
    
    -   遍历得到的类名，创建它的实例

4.  **ApplicationContextInitializer**；去spring.factories找 **ApplicationContextInitializer**

    -   List<ApplicationContextInitializer<?>> **initializers**

5.  **找** **ApplicationListener  ；应用监听器。**去**spring.factories****找** **ApplicationListener**

    -   List<ApplicationListener<?>> **listeners**

    

### ②运行SpringApplication

1.  **stopWatch.start();**	开始时间

2.  **createBootstrapContext()**    创建引导环境（context环境）

    -   用之前得到的**bootstrappers**遍历得到每个**bootstrapper**来调用**intitialize方法**设置引导器上下文环境设置

        ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155448928.png)


3.   

4.  **configureHeadlessProperty();**    让当前应用进入**headless**模式。**java.awt.headless**

5.  获取所有的**RunListener（运行时监听器）**，方便所有的listener进行事件感知

    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155459351.png)


6.  遍历 **SpringApplicationRunListener** 调用 **starting** 方法；

    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717160144553.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


    -   **相当于通知所有感兴趣系统正在启动过程的人，项目正在 starting。**

7.  **applicationArguments**      保存命令行参数

8.  **prepareEnvironment(...)**    准备环境

    1.  返回或者创建基础环境信息对象。**StandardServletEnvironment**

        ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155508862.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


    2.  配置环境信息
    
        -   **读取所有的配置源的配置属性值。**
    
            ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155519257.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)
    
    3.  **ConfigurationPropertySources.attach(environment);**    绑定环境信息
    
    4.  监听器调用 **listener.environmentPrepared()；**通知所有的监听器当前环境准备完成

9.  **createApplicationContext()**    创建IOC容器

    -   根据当前应用类型创建容器，当前是servlet应用

        ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717155527302.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


10.  **prepareContext(...)**    准备容器的基本信息

     1.  设置环境信息
     2.  IOC容器的后置处理流程
     3.  应用初始化器
         -   遍历所有的**ApplicationContextInitializer**调用**initialize(...)**方法，来对容器进行初始化扩展功能
     4.  注册**springApplicationArguments**和**springBootBanner**组件
     5.  **所有的监听器 调用** **contextLoaded。通知所有的监听器** **contextLoaded；**

11.  **refreshContext(context);**    刷新IOC容器

     -   创建容器中的所有组件（Spring注解）

12.  **afterRefresh(context, applicationArguments);**    容器刷新后工作，默认什么都没做

13.  **stopWatch.stop();**    结束时间

14.  **listeners.started(context);**   通知所有监听器started

15.  **callRunners(context, applicationArguments);**    调用所有**runners**

     -   **获取容器中的** **ApplicationRunner** 
     -   **获取容器中的**  **CommandLineRunner**

     -   **合并所有runner并且按照@Order进行排序**
     -   **遍历所有的runner。调用 run** **方法**

16.  如果有异常

     -   **调用Listener 的 failed**

     -   **调用所有监听器的 running 方法**  listeners.running(context); **通知所有的监听器** **running** 
     -   **running如果有问题。继续通知 failed 。****调用所有 Listener 的** **failed；****通知所有的监听器** **failed**



## 2.Application Events and Listeners

```java
public class MyApplicationContextInitializer implements ApplicationContextInitializer {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        System.out.println("MyApplicationContextInitializer...initialize...");
    }
}
```

```java
public class MyApplicationListener implements ApplicationListener {

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        System.out.println("MyApplicationListener...onApplicationEvent...");
    }
}
```

```java
public class MySpringApplicationRunListener implements SpringApplicationRunListener {

    @Override
    public void starting(ConfigurableBootstrapContext bootstrapContext) {
        System.out.println("MySpringApplicationRunListener...starting...");
    }

    @Override
    public void environmentPrepared(ConfigurableBootstrapContext bootstrapContext, ConfigurableEnvironment environment) {
        System.out.println("MySpringApplicationRunListener...environmentPrepared...");
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
        System.out.println("MySpringApplicationRunListener...contextPrepared...");
    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {
        System.out.println("MySpringApplicationRunListener...contextLoaded...");
    }

    @Override
    public void started(ConfigurableApplicationContext context) {
        System.out.println("MySpringApplicationRunListener...started...");
    }

    @Override
    public void running(ConfigurableApplicationContext context) {
        System.out.println("MySpringApplicationRunListener...running...");
    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {
        System.out.println("MySpringApplicationRunListener...failed...");
    }
}
```

spring.factories写入

```properties
org.springframework.context.ApplicationContextInitializer=\
  com.xiaozhi.springbootnull.listener.MyApplicationContextInitializer

org.springframework.context.ApplicationListener=\
  com.xiaozhi.springbootnull.listener.MyApplicationListener
```



## 3.ApplicationRunner 与 CommandLineRunner

```java
@Order(1)
@Component
public class MyApplicationRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("MyApplicationRunner...run...");
    }
}
```

```java
@Order(2)
@Component
public class MyCommandLineRunner implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        System.out.println("MyCommandLineRunner...run...");
    }
}
```

spring.factories写入

```properties
org.springframework.context.SpringApplicationRunListener=\
  com.xiaozhi.springbootnull.listener.MySpringApplicationRunListener
```

   1.  首先它会从缓存中拿取数据，第一次是没有数据的

        2.  第一次它会加载**spring.factories**文件，然后通过Properties去加载得到kv键值对，value是List类型

            ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717160235512.png)	![在这里插入图片描述](D:\学习笔记\img\2025\20210717160257768.png)


        3.  将加载的信息放到缓存中，下次来就不需要加载spring.factories文件了
    
        4.  最终返回一个Map<String, List<String>>集
    
    -   遍历得到的类名，创建它的实例

4.  **ApplicationContextInitializer**；去spring.factories找 **ApplicationContextInitializer**

    -   List<ApplicationContextInitializer<?>> **initializers**

5.  **找** **ApplicationListener  ；应用监听器。**去**spring.factories****找** **ApplicationListener**

    -   List<ApplicationListener<?>> **listeners**

    

### ②运行SpringApplication

1.  **stopWatch.start();**	开始时间

2.  **createBootstrapContext()**    创建引导环境（context环境）

    -   用之前得到的**bootstrappers**遍历得到每个**bootstrapper**来调用**intitialize方法**设置引导器上下文环境设置

        ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717160325315.png)


3.   

4.  **configureHeadlessProperty();**    让当前应用进入**headless**模式。**java.awt.headless**

5.  获取所有的**RunListener（运行时监听器）**，方便所有的listener进行事件感知

    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717160351331.png)


6.  遍历 **SpringApplicationRunListener** 调用 **starting** 方法；

    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717160404994.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


    -   **相当于通知所有感兴趣系统正在启动过程的人，项目正在 starting。**

7.  **applicationArguments**      保存命令行参数

8.  **prepareEnvironment(...)**    准备环境

    1.  返回或者创建基础环境信息对象。**StandardServletEnvironment**

        ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717160432943.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


    2.  配置环境信息
    
        -   **读取所有的配置源的配置属性值。**
    
            ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717160503195.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


    3.  **ConfigurationPropertySources.attach(environment);**    绑定环境信息
    
    4.  监听器调用 **listener.environmentPrepared()；**通知所有的监听器当前环境准备完成

9.  **createApplicationContext()**    创建IOC容器

    -   根据当前应用类型创建容器，当前是servlet应用

        ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210717160521357.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


10.  **prepareContext(...)**    准备容器的基本信息

     1.  设置环境信息
     2.  IOC容器的后置处理流程
     3.  应用初始化器
         -   遍历所有的**ApplicationContextInitializer**调用**initialize(...)**方法，来对容器进行初始化扩展功能
     4.  注册**springApplicationArguments**和**springBootBanner**组件
     5.  **所有的监听器 调用** **contextLoaded。通知所有的监听器** **contextLoaded；**

11.  **refreshContext(context);**    刷新IOC容器

     -   创建容器中的所有组件（Spring注解）

12.  **afterRefresh(context, applicationArguments);**    容器刷新后工作，默认什么都没做

13.  **stopWatch.stop();**    结束时间

14.  **listeners.started(context);**   通知所有监听器started

15.  **callRunners(context, applicationArguments);**    调用所有**runners**

     -   **获取容器中的** **ApplicationRunner** 
     -   **获取容器中的**  **CommandLineRunner**

     -   **合并所有runner并且按照@Order进行排序**
     -   **遍历所有的runner。调用 run** **方法**

16.  如果有异常

     -   **调用Listener 的 failed**

     -   **调用所有监听器的 running 方法**  listeners.running(context); **通知所有的监听器** **running** 
     -   **running如果有问题。继续通知 failed 。****调用所有 Listener 的** **failed；****通知所有的监听器** **failed**



## 2.Application Events and Listeners

```java
public class MyApplicationContextInitializer implements ApplicationContextInitializer {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        System.out.println("MyApplicationContextInitializer...initialize...");
    }
}
```

```java
public class MyApplicationListener implements ApplicationListener {

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        System.out.println("MyApplicationListener...onApplicationEvent...");
    }
}
```

```java
public class MySpringApplicationRunListener implements SpringApplicationRunListener {

    @Override
    public void starting(ConfigurableBootstrapContext bootstrapContext) {
        System.out.println("MySpringApplicationRunListener...starting...");
    }

    @Override
    public void environmentPrepared(ConfigurableBootstrapContext bootstrapContext, ConfigurableEnvironment environment) {
        System.out.println("MySpringApplicationRunListener...environmentPrepared...");
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
        System.out.println("MySpringApplicationRunListener...contextPrepared...");
    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {
        System.out.println("MySpringApplicationRunListener...contextLoaded...");
    }

    @Override
    public void started(ConfigurableApplicationContext context) {
        System.out.println("MySpringApplicationRunListener...started...");
    }

    @Override
    public void running(ConfigurableApplicationContext context) {
        System.out.println("MySpringApplicationRunListener...running...");
    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {
        System.out.println("MySpringApplicationRunListener...failed...");
    }
}
```

spring.factories写入

```properties
org.springframework.context.ApplicationContextInitializer=\
  com.xiaozhi.springbootnull.listener.MyApplicationContextInitializer

org.springframework.context.ApplicationListener=\
  com.xiaozhi.springbootnull.listener.MyApplicationListener
```



## 3.ApplicationRunner 与 CommandLineRunner

```java
  com.xiaozhi.springbootnull.listener.MySpringApplicationRunListener@Order(1)
@Component
public class MyApplicationRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("MyApplicationRunner...run...");
    }
}
```

```java
@Order(2)
@Component
public class MyCommandLineRunner implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        System.out.println("MyCommandLineRunner...run...");
    }
}
```

spring.factories写入

```properties
org.springframework.context.SpringApplicationRunListener=\
  com.xiaozhi.springbootnull.listener.MySpringApplicationRunListener
```

