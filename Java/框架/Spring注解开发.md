# 组件注册

## @Configuration

标注的类相当于是创建了对应的xml文件，也就是配置文件，我们就可以在这个类中进行组件的注册了

```java
@Configuration
public class MyConfiguration {   
}
```

相当于是创建了

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
        https://www.springframework.org/schema/beans/spring-beans.xsd">

</beans>
```



## @Bean

@Bean：通过这个注解将对象注册到IOC容器中

### 配置文件方式

**1	配置文件的方式将对象注册到容器中**

```java
<bean id="person" class="com.xiaozhi.pojo.Person">
    <property name="name" value="小智"/>
    <property name="age" value="18"/>
</bean>
```

**2	然后通过配置文件得到对应的bean对象**

```java
@Test
public void Test(){
    ApplicationContext context = new ClassPathXmlApplicationContext("MyConfiguration.xml");
    Person bean = context.getBean(Person.class);
    System.out.println(bean);
}
```



### 注解的方式

1 在配置类中注入容器中

```java
@Configuration
public class MyConfiguration {
    @Bean(value = "person")     // 可以修改默认的对象名
    // 默认是方法名为对象名
    public Person getPerson(){
        return new Person("小智",18);
    }
}
```

2 然后通过创建IOC容器得到对应的bean

```java
@Test
public void Test2(){
    // 创建spring的IOC容器
    ApplicationContext context = new AnnotationConfigApplicationContext(MyConfiguration.class);
    String[] names = context.getBeanDefinitionNames();    // 通过getBeanDefinitionNames可以得到在注册到容器中的对象名
    for (String name : names) {
        System.out.println(name);
    }
}
```

==**注意**：配置文件的方式和注解的方式得到IOC容器的方式是不一样的==

通过getBeanDefinitionNames我们得到了IOC中注册了的bean对象

![image-20210315223806051](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210315223806051.png)



## @Lookup

5.0之后添加的注解，主要的功能是让单例的对象可以依赖多实例对象，每次都是创建新的示例

我们IOC中的单例对象每次获取出来是不变的，所以所依赖的对象也是不变的，如果想要依赖的对象是多实例，那么就需要使用 @Lookup 注解来实现。

注意：

- 这个注解不能标注在属性上，只能标注在方法上
- 要放在对应依赖的类的get方法上，set方法上无效
- 不能使用 @Bean 的方式将被依赖对象注入到 IOC 容器中，同样也是会无效





## @Indexed

加速bean的创建，可以不用等待 SpringIoc创建完毕再进行创建，加速 Bean实例的创建





## @Component

被标注的类表示是一个组件，它还衍生出了三个注解，功能是一样的

**@Controller**、**@Service**、**@Repository**，分别用于控制层，服务层，数据访问层

```java
@Configuration
@ComponentScan(value = "com.xiaozhi")   // 扫描对应包下的组件
public class MyConfiguration {
    @Bean(value = "person")     // 可以修改默认的对象名
    public Person getPerson(){
        return new Person("小智",18);
    }
}
```

然后我们到的组件就注入到了IOC容器中了

![image-20210315225449267](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210315225449267.png)

注意：我们的@Component和@Bean都是IOC中的组件，前者是可以通过扫描注入到IOC容器中，后者是要通过方法返回对应的对象，然后在方法上标注@Bean才能注入到容器中，@Bean可以定制化的注入属性值





## @ComponentScan

==@ComponentScans可以包含多个@ComponentScan==

根据策略将组件注入到IOC容器中，它可以通过一些规则来排除或者指定想要导入的组件

```java
@ComponentScan(value = "com.xiaozhi")		// 扫描指定包下的组件
```



### excludeFilters

表示排除掉包含在规则中的组件，就是排除那些组件

```java
@ComponentScan(excludeFilters = {
        // 没有Controller注解的组件
        @ComponentScan.Filter(type = FilterType.ANNOTATION,classes = {Controller.class})
})   // 扫描对应包下的组件
```



### includeFilters

只导入包含指定规则的组件

```java
@ComponentScan(value = "com.xiaozhi",includeFilters = {
        // 有Controller注解的组件
        @ComponentScan.Filter(type = FilterType.ANNOTATION,classes = {Controller.class})
},useDefaultFilters = false)   // 扫描对应包下的组件
```

==**注意**：使用**includeFilters属性**我们一定要禁用掉默认规则，因为默认规则是扫描全部的包，不禁用的话，其他的组件还是会注册到容器中的，在使用**excludeFilters属性**的时候我们要把它开启，不然就没有组件没扫描进来==



### 说明

在java8，我们要想写多个@ComponentScan可以直接加一个就可以了，8以下的版本就需要用**@ComponentScans**将多个**@ComponentScan**包含起来



### 规则

ANNOTATION				注解类型 默认

ASSIGNABLE_TYPE	指定固定类

ASPECTJ					 ASPECTJ类型

REGEX						正则表达式

CUSTOM						自定义类型



### 自定义规则

通过源码我们可以发现我们要实现自定义规则首先要继承TypeFilter接口

![image-20210315234943081](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210315234943081.png)

```java
public class MyTypeFilter implements TypeFilter {
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
        return false;
    }
}
```

**然后编写我们对应的规则**

```java
@ComponentScan(value = "com.xiaozhi",includeFilters = {
        // 过滤掉没有Controller注解的组件
//        @ComponentScan.Filter(type = FilterType.ANNOTATION,classes = {Controller.class}),
//        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,classes = {BookService.class}),  // 这个类型的所有bean
        @ComponentScan.Filter(type = FilterType.CUSTOM,classes = {MyTypeFilter.class})  // 自定义的规则
},useDefaultFilters = false)   // useDefaultFilters为false就是不自动将组件注册到容器中
```

首先了解实现类中的一些信息

```java
public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
    // 获取当前类注释的信息
    AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
    // 获取当前正在扫描的类的类信息
    ClassMetadata classMetadata = metadataReader.getClassMetadata();
    // 获取当前类资源（类额路径）
    Resource resource = metadataReader.getResource();
    String className = classMetadata.getClassName();
    System.out.println("---->" + className);
    return false;
}
```

结果显示得到这些类的信息

![image-20210316221218435](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210316221218435.png)



接下来我们开始写我们的规则

![image-20210316221622321](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210316221622321.png)

**结果显示**

![image-20210316221939762](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210316221939762.png)





## @Scope

我们的容器在创建对象的时候默认是创建单实例的对象，所以我们可以通过@Scope注解来修改默认的规则

```java
@Configuration
public class MyConfiguration2 {
    /*
     * @see ConfigurableBeanFactory#SCOPE_PROTOTYPE     protptype多实例
     * @see ConfigurableBeanFactory#SCOPE_SINGLETON     singleton单实例
     * @see org.springframework.web.context.WebApplicationContext#SCOPE_REQUEST     request
     * @see org.springframework.web.context.WebApplicationContext#SCOPE_SESSION     session
     *
     * singleton单实例（默认）
     * protptype多实例
     * request      同一个请求创建一个实例(不常用)
     * session      同一个session创建一个实例（不常用）
     */
    @Scope     // 相当于在配置文件中添加一个scope属性
    // 默认是单例的
    @Bean
    public Person person(){
        return new Person();
    }
}
```

**注意**：

​	**单实例情况下**，IOC容器会调用方法创建实例放入到容器中，以后每次获取就是直接从容器中拿，==单实例对象的创建是	在IOC创建成功之前的，要注意一下这个问题==

​	**多实例的情况下**，只有获取对象的时候才会创建，每获取一次就调用一次方法创建对象



**代码实现**

```java
@Scope(scopeName = "prototype")     // 相当于在配置文件中添加一个scope属性
// 默认是单例的
@Bean
public Person person(){
    return new Person();
}
```

```java
@Test
public void Test3(){
    ApplicationContext context = new AnnotationConfigApplicationContext(MyConfiguration2.class);
    Person bean = context.getBean(Person.class);
    Person bean2 = context.getBean(Person.class);
    System.out.println(bean == bean2);
}
```

**结果显示为false**



## @Lazy

**懒加载**

容器启动时不创建对象，第一次使用的时候创建对象，并初始化，**它也是单实例的**

```java
@Lazy
// 默认是单例的
@Bean
public Person person(){
    System.out.println("实例被创建出来了");
    return new Person();
}
```

```java
@Test
public void Test3(){
    ApplicationContext context = new AnnotationConfigApplicationContext(MyConfiguration2.class);
    System.out.println("IOC创建成功");
    Person bean = context.getBean(Person.class);
    Person bean2 = context.getBean(Person.class);
    System.out.println(bean == bean2);
}
```

![image-20210316225144983](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210316225144983.png)

可以看到，IOC在我们的单实例创建之前就已经创建出来了，这个就是懒加载，只有在用的时候才会加载出来



## @Conditional

满足给定的条件才能注册到容器中

```java
public @interface Conditional {

   /**
    * All {@link Condition Conditions} that must {@linkplain Condition#matches match}
    * in order for the component to be registered.
    */
   Class<? extends Condition>[] value();
}
```

源码中我们可以看到它的值是一个Condition类型的，所以如果我们想要设置条件就要实现Condition接口，这个接口中有一个方法，返回false就是条件不匹配，返回true就是条件匹配，被标注注解的就会注册到容器中



**创建两个类继承Condition接口**

```java
public class LinuxConditional implements Condition {
    /**
     * @param context   判定条件能使用的上下文（环境）
     * @param metadata  注释信息
     * @return
     */
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // 获取到ioc使用的beanfactory
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        // 获取类加载器
        ClassLoader classLoader = context.getClassLoader();
        // 获取当前环境信息
        Environment environment = context.getEnvironment();
        // 获取bean定义的注册类，可以通过这个类来对bean进行操作
        BeanDefinitionRegistry registry = context.getRegistry();
        String property = environment.getProperty("os.name");
        if (property.contains("linux")){
            return true;    // 返回true表示注册到容器中
        }
        return false;
    }
}
```

```java
public class WindowsConditional implements Condition {
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment environment = context.getEnvironment();
        String property = environment.getProperty("os.name");
        if (property.contains("Windows")){
            return true;
        }
        return false;
    }
}
```

然后我们进行测试

```java
@Conditional({WindowsConditional.class})
@Bean("bill")
public Person person01(){
    return new Person("Bill Gates",63);
}
@Conditional({LinuxConditional.class})
@Bean("linus")
public Person person02(){
    return new Person("linus",38);
}
```

最后只有bill是注册到了bean中，只有它满足条件

==我们还可以将注解标注在类上，只有满足条件，这个类的所有bean才会被注册到容器中，我们还可以通过BeanDefinitionRegistry对象进行条件的判断，满足才能注册bean==





## @Import

可以快速的导入组件，可以导入多个组件，容器会自动注册这些组件，这些组件的id默认名是全类名



**我们之前给容器中注册bean的几种方式**：

​	①@Conponent、@Controller、@Service、@Repository		这种方式适用于我们自己写的类

​	②@Bean		这种方式可以导入第三方的类

**说明**：这两种方式导入组件太麻烦了，所以我们才有了@Import来进行快速的导入



**代码实现**

```java
// 没有标注组件的注解
public class Color {
}
```

```java
@Configuration
@Import(value = {Color.class})      // 导入Color组件
public class MyConfiguration2 {
```

**结果显示**

![image-20210316234158177](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210316234158177.png)



![image-20210316234712570](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210316234712570.png)



### @ImprotSelector

ImprotSelector它是一个接口，所以我们要实现它

```java
// 自定义逻辑返回需要导入的组件
public class MyImportSelector implements ImportSelector {
    /**
     * @param importingClassMetadata    // 得到当前标注@Import注释类的所有注释信息，要注意是所有的注解
     * @return
     */
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {

        return null;
    }
}
```

如果我们的返回值为null，它会报一个空指针异常，原因如下

![image-20210316235952427](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210316235952427.png)

往下走到下面这个方法

![image-20210317000025963](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210317000025963.png)

==所以会报一个空指针异常，这个要注意一下方法不要返回null==



我们实现一下我们自己的逻辑

```java
public String[] selectImports(AnnotationMetadata importingClassMetadata) {
    return new String[]{"com.xiaozhi.pojo.Blue","com.xiaozhi.pojo.Yellow"};
}
```

返回的数组里面的组件就会被注册到容器中



**理解**：其实@ImprotSelector注解也就相当是一个数组，方法里面呢可以得到其他的注解信息，也就是说我们可以通过判断我们的类上是否含有某个注解，如果有我们就创建，没有我们就不创建，这也就是自己逻辑了



### @ImportBeanDefinitionRegistrar

老套路，实现接口

```java
public class MyImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {
    /**
     * @param importingClassMetadata    // 得到当前标注@Import注释类的所有注释信息
     * @param registry      // 可以定义bean的注册和bean的信息（bean的类型，bean的作用域。。。。）
     */
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        boolean b = registry.containsBeanDefinition("com.xiaozhi.pojo.Blue");
        boolean b1 = registry.containsBeanDefinition("com.xiaozhi.pojo.Yellow");
        if (b && b1){
            // 如果有上面的两个对象我们就将Person类注册到容器中
            BeanDefinition beanDefinition = new RootBeanDefinition(Person.class);
            registry.registerBeanDefinition("person",beanDefinition);
        }
    }
}
```



**小总结**：

​	三个注解都是将组件注册到容器中，前面两个都是自动注册，最后一个是手动注册，不过手动注册的定制性更强，根据	自己需要的场景使用对应的注解



## FactoryBean工厂类

```java
// 创建一个spring定义的FactoryBean工厂类
public class ColorFactoryBean implements FactoryBean {
    // 返回一个color对象，这个对象会添加到容器中
    public Object getObject() throws Exception {
        System.out.println("getObject.......");
        return new Color();
    }

    public Class<?> getObjectType() {
        return Color.class;
    }

    // 控制是否是单例的，true为单实例
    public boolean isSingleton() {
        return true;
    }
}
```

```java
@Test
public void Test5(){
    ApplicationContext context = new AnnotationConfigApplicationContext(MyConfiguration2.class);
    for (String beanDefinitionName : context.getBeanDefinitionNames()) {
        System.out.println(beanDefinitionName);
    }
    Object bean = context.getBean("colorFactoryBean");
    // 获取到的并不是FactoryBean类本身，它会去调用getObject方法来得到对应的实例
    System.out.println("bean的类型->" + bean.getClass().getName());    // com.xiaozhi.bean.Color
    Object bean2 = context.getBean("colorFactoryBean");
    System.out.println(bean == bean2);
    // 要想得到FactoryBean类本身的实例，我们可以在id前面加一个"&"
    Object factoryBean = context.getBean("&colorFactoryBean");
    System.out.println(factoryBean.getClass().getName());       // com.xiaozhi.bean.ColorFactoryBean
}
```

![image-20210317161858482](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210317161858482.png)



# bean的生命周期

bean的生命周期是可以自定义的，定义bean的创建和销毁

**bean的声明周期**：bean创建	-->	初始化	-->	销毁

**创建**：

​	单实例：容器启动的时候创建

​	多实例：在每次获取的时候创建对象

**初始化**：

​	对象创建完成，赋好值，然后进行初始化

**销毁**：

​	单实例：容器关闭就会销毁

​	多实例：容器不会管理多实例的bean，所以不会调用销毁方法，这个要注意一下



## 1	@Bean的方式

@Bean的两个属性来指定初始化和销毁，initMethod和destroyMethod

```java
public class Car {
    public Car() {
        System.out.println("car创建了。。。");
    }
    public void init(){
        System.out.println("car初始化。。。");
    }
    public void detory(){
        System.out.println("car销毁了。。。。");
    }
}
```

```java
@Configuration
public class MyConfigOfLifeCycle {
//    @Scope("prototype")	// 多实例不会调用销毁方法
    @Bean(initMethod = "init",destroyMethod = "detory") // 指定初始化和销毁方法
    public Car car(){
        return new Car();
    }
}
```

**测试** 

```java
@Test
public void Test1(){
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MyConfigOfLifeCycle.class);
    Car bean = context.getBean(Car.class);
    // 在容器关闭的时候就会调用销毁方法
    context.close();    // 关闭容器，这个只有是AnnotationConfigApplicationContext变量接收的时候才会有的方法
}
```



## 2	实现接口的方式

![image-20210317164642537](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210317164642537.png)

![image-20210317164654475](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210317164654475.png)

通过源码我们知道可以通过实现这两个接口来进行初始化和销毁



**代码实现**

```java
@Component
public class Cat implements Initializable, DisposableBean {
    public Cat(){
        System.out.println("cat创建");
    }
    // 初始化方法
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("cat初始化");
    }
    // 销毁方法
    public void destroy() throws Exception {
        System.out.println("cat销毁");
    }
}
```

也就相当于是在@Bean注解指定方法





## 3	使用JSR250规范

**@PostConstruct**	对象创建并赋值之后调用

**@PreDestroy**	在容器销毁之前通知我们进行清理工作，这个是在容器移除之前进行的



**代码实现**

```java
@Component
public class Dog {
    public Dog() {
        System.out.println("dog创建");
    }
    @PostConstruct
    public void init(){
        System.out.println("dog初始化");
    }
    @PreDestroy
    public void destroy(){
        System.out.println("dog清理");
    }
}
```



## 4	BeanPostProcessor接口

它是bean的后置处理器，在初始化前后进行一个工作

它的两个方法是在初始化之间，一个是初始化之前，一个是初始化之后调用的，前面几个是初始化，而这个在初始化之间



**代码实现**

```java
@Component
public class MyBeanPostProcessor implements BeanPostProcessor {
    // 每个bean初始化之前都会调用这个方法进行一些操作
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        System.out.println("初始化之前的" + beanName);
        return bean;
    }
    // 每个bean初始化之前都会调用这个方法进行一些操作
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        System.out.println("初始化之后" + beanName);
        return bean;
    }
}
```



![image-20210317172512811](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210317172512811.png)





### BeanPostProcessor实现原理

![image-20210317204326117](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210317204326117.png)

初始化之前就是给属性赋值，挨个遍历赋值，进行初始化如下

![image-20210317204542609](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210317204542609.png)

这些都是阅读源码发现的，利用DEBUG来进行程序的执行跟踪



**自我发现**：在自定义的后置处理器中的postProcessBeforeInitialization和postProcessAfterInitialization打上断点，开始进行调试，这个时候它会进入到一个循环中，遍历bean的后置处理器，给每个bean进行初始化之前的操作，我还发现这个后置处理器中有BeanFactory，也就是说它遍历的也就是一个个的bean实例，这也就是它可以拦截到每个bean，然后对它进行操作



### spring底层对BeanPostProcessor的使用

 	bean的赋值，其他组件的注入，@Autowird，bean生命周期的注解功能，@Async异步方法，xxxBeanPostProcessor



**我们可以看一个示例**

```java
@Component
public class Dog implements ApplicationContextAware {
    private ApplicationContext applicationContext;
    public Dog() {
        System.out.println("dog创建");
    }
    @PostConstruct
    public void init(){
        System.out.println("dog初始化");
    }
    @PreDestroy
    public void destroy(){
        System.out.println("dog清理");
    }

    // 得到IOC容器，这个是ApplicationContextAwareProcessor帮我们放入的
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 保存下来我们就可以使用这个IOC容器的东西了
        this.applicationContext = applicationContext;
    }
}
```

我们给这个方法打上断点

首先底层会进入到下面这个类中

![image-20210317221501702](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210317221501702.png)

然后它会走下面这个方法进行判断，然后将IOC容器放入，我们就可以得到IOC的容器了

![image-20210317221604169](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210317221604169.png)





# 属性赋值

## @Value

三种用法

-   基本数值
-   可以写spring表达式（SpEL）：#{}
-   可以写${}，取出配置文件中的值

**代码实现**

```java
public class Person {
    /*
        使用@Value赋值
        1   基本数值
        2   可以写SpEL: #{}    spring表达式
        3   可以写${},取出配置文件中的值
     */
    @Value("张三")       // 基本数值
    private String name;
    @Value("#{20-2}")   // spring表达式
    private Integer age;
```

结果显示	**Person(name=张三, age=18)**



## @PropertySource

这个注解可以读取外部配置文件，加载到环境变量中，然后可以通过${}将值赋给属性

1	创建一个配置文件，然后里面写入数值

```properties
person.nickName=小智
```

2	然后我们在主配置类中将配置文件引入

```java
@PropertySource("classpath:/person.properties")     // 读取外部配置文件，加载到环境变量中
@Configuration
public class MainConfigOfPropertyValues {
```

3	最后通过${}取出对应key的值

```java
@Value("${person.nickName}")
private String nickName;
```

4	结果显示

![image-20210317223944317](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210317223944317.png)



**因为已经是加载到环境变量中了，我们可以使用环境变量来获取到对应的值**

```java
@Test
public void Test1(){
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainConfigOfPropertyValues.class);
    ConfigurableEnvironment environment = context.getEnvironment();     // 获取当前的环境变量
    String property = environment.getProperty("person.nickName");       // 获取环境变量中的属性值
    System.out.println(property);	// 打印属性值
    context.close();
}
```

==还可以写入多个配置文件，也可以使用@PropertySources注解放入多个PropertySource==





# 自动装配

是spring利用依赖注入（DI），完成对IOC容器中各个组件依赖关系赋值

**补充**：自动装配是又AutoWiredAnnotationBeanPostProcessor解析完成的



## @Autowired

自动注入

​	默认优先按照类型去找对应的组件，如果同类型的组件有多个，那么就会将属性名称作为组件id去查找



**代码实现**

```java
@Service
public class BookService {
    @Autowired
    BookMapper bookMapper;
```



我们再做一个实验

```java
@Repository
@Getter
@Setter
public class BookMapper {
    private String lable = "1";
}
```

```java
@Configuration
@ComponentScan({"com.xiaozhi.mapper","com.xiaozhi.service","com.xiaozhi.controller"})
public class MainConfigAutoWired {
    @Bean("bookMapper2")
    public BookMapper bookMapper(){
        BookMapper bookMapper = new BookMapper();
        bookMapper.setLable("2");
        return bookMapper;
    }
}
```

当有多个相同类型的时候，它会根据它的属性名去找

```java
@Test
public void Test1(){
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainConfigAutoWired.class);
    BookService bean = context.getBean(BookService.class);
    System.out.println(bean.getBookMapper().getLable());
    context.close();
}
```

输出结果为：1



==**注意**：自动装配默认一定要将属性赋值好，不然就会报错，我们可以通过@Autowired的required属性声明不赋值也可以，是在没找到的情况下不赋值==

![image-20210318100151158](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210318100151158.png)

这个值就可以为null了



### 可以标注的位置

构造器、参数（形参）、方法、属性

​	1	标注在构造器上，IOC容器创建对象的时候就会调用构造器赋值

​	2	标注在参数上，IOC容器创建对象的时候就会给这个参数赋值

​	3	标注在方法上，IOC容器创建对象的时候就会调用这个方法

​	4	标注在方法上，IOC容器创建对象的时候就会给这个属性赋值



说明：

​	①如果组件只有一个有参构造器，这个有参构造器的@AutoWired可以省略，

​	②以@Bean的方式创建对象的时候，参数可以省略注解

![image-20210318105855937](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210318105855937.png)



**代码实现**

```java
@Component
@ToString
public class Boss {
    private Car car;
    @Autowired
    public Boss(Car car) {
        this.car = car;
    }

    public Car getCar() {
        return car;
    }

    // 标注的方法，spring容器创建对象的时候就会调用这个方法
    // 方法使用的参数，自定义的值从IOC中拿取
//    @Autowired
    public void setCar(Car car) {
        this.car = car;
    }
}
```





## @Qualifier

使用Qualifier可以指定需要装配组件的id，而不是使用属性名

```java
@Service
@Data
public class BookService {

    @Qualifier("bookMapper2")
    @Autowired(required = false)
    BookMapper bookMapper;
```

```java
@Test
public void Test(){
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainConfigAutoWired.class);
    BookService bean = context.getBean(BookService.class);
    System.out.println(bean.getBookMapper().getLable());
    context.close();
}
```

结果输出为：2

==**注意**：这个注解一定是和Autowired一起使用的，单独使用是没有效果的==



## @Primary

spring装配时，默认使用首选的bean，也可以使用Qualifier指定需要装配的bean

```java
@Configuration
@ComponentScan({"com.xiaozhi.mapper","com.xiaozhi.service","com.xiaozhi.controller"})
public class MainConfigAutoWired {
    @Primary    // 默认指定这个bean
    @Bean("bookMapper2")
    public BookMapper bookMapper(){
        BookMapper bookMapper = new BookMapper();
        bookMapper.setLable("2");
        return bookMapper;
    }
}
```

在没有@Qualifier的情况下，那么装配的就是这个指定默认的bean了

如果有Qualifier，那么就是以Qualifier指定的bean为主



## @Resource

JSR250和@Inject(JSR330)规范中的注解，java规范的注解

和@Autowired的功能一样，默认是根据组件名称去找，也可以通过它的name属性指定，但是它不能结合@Qualifier使用，也没有@Autowired(reqiured=false)的功能，不支持@Primary的功能

```java
public class BookService {

//    @Qualifier("bookMapper")
//    @Autowired(required = false)
    @Resource(name = "bookMapper2")     // 通过name属性指定
    BookMapper bookMapper;
```



## @Inject

和AutoWired的功能一样，它没有required属性



1	导入Inject的依赖

```xml
<!-- https://mvnrepository.com/artifact/javax.inject/javax.inject -->
<dependency>
    <groupId>javax.inject</groupId>
    <artifactId>javax.inject</artifactId>
    <version>1</version>
</dependency>
```

```java
@Inject
BookMapper bookMapper;
```





## 导入spring底层的组件

我们可以通过实现xxxAware来将底层的组件放到我们自定义的类中

每个xxxAware都会有一个xxxProcessor（后置处理器）

当bean创建完成之后，后置处理器就会进行拦截，然后对拦截的bean进行一些操作，比如将ApplicationContext对象传给实现ApplicationContextAware接口的自定义类



**代码实现**

```java
public class Red implements ApplicationContextAware, EnvironmentAware {
    // 我们就可以使用这些底层的组件了
    ApplicationContext applicationContext;
    EnvironmentAware environmentAware;
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    public void setEnvironment(Environment environment) {
        this.environmentAware = environmentAware;
    }
}
```





## @Profile

它是环境变量的表示，标上了这个注解的只有当前是这个环境的时候才会被注册到容器中，默认是default环境



**搭建实验环境**

1	导入mysal驱动和c3p0数据源

2	编写三个数据源，分别是测试用的，开发用的，上线用的

```java
@Configuration
@PropertySource("classpath:/db.properties")
public class MainConfigOfProfile implements EmbeddedValueResolverAware {
    @Value("${name}")
    private String name;
    private StringValueResolver valueResolver;
    private String driver;

    @Bean
    public DataSource dataSourceTest(@Value("${pwd}")String pwd) throws Exception {
        ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setUser(name);
        dataSource.setPassword(pwd);
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/db1");
        dataSource.setDriverClass(driver);
        return dataSource;
    }
    @Bean
    public DataSource dataSourceDev(@Value("${pwd}")String pwd) throws Exception {
        ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setUser(name);
        dataSource.setPassword(pwd);
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/db2");
        dataSource.setDriverClass(driver);
        return dataSource;
    }
    @Bean
    public DataSource dataSourceProd(@Value("${pwd}")String pwd) throws Exception {
        ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setUser(name);
        dataSource.setPassword(pwd);
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/db3");
        dataSource.setDriverClass(driver);
        return dataSource;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.valueResolver = resolver;
        this.driver = valueResolver.resolveStringValue("${driver}");
    }
}
```

**在这里我们使用了前面说的@Value注解的应用**



3	给这三个不同的数据源标上环境标识，只有是对应的环境的时候，才能使用对应的数据源，跟@Conditional差不多

![image-20210318232113397](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210318232113397.png)

我们的程序默认是执行default环境，我们要想激活其他的环境，有两种方式

**第一种**：命令行的方式（没找到IDEA中在哪里输入命令）

**第二种**：代码实现

```java
@Test
public void Test1(){
    // 1 创建一个IOC容器，调用的是空参的构造器
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    // 2 设置需要激活的环境
    context.getEnvironment().setActiveProfiles("test","dev");
    // 3 注册主配置类
    context.register(MainConfigOfProfile.class);
    // 4 启动刷新容器
    context.refresh();
    for (String s : context.getBeanNamesForType(DataSource.class)) {
        System.out.println(s);
    }
    context.close();
}
```

上面的四个步骤相当于我们的有参构造创建IOC的容器的三个步骤，修改环境的步骤就是我们加上去的

![image-20210318232554548](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210318232554548.png)

==**说明**：读源码我们可以发现==

**测试**：我们就可以看到test环境和dev环境的两个数据源就加载到IOC容器中了

![image-20210318232644683](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210318232644683.png)









# AOP

指在程序运行的时候动态的将某段代码切入到执行方法指定位置进行运行的编程方式，它的底层实现是动态代理



## AOP功能测试

**代码演示**

1	先导入AOP的切面包	->	aspectj

2	定义一个业务逻辑类，在方法运行的时候进行日志打印（方法执行前、方法运行结束、方法出现异常。。。）

```java
// 业务逻辑类
public class MathCalculator {

    public int div(int i,int j){
        return i/j;
    }
}
```

3	定义一个日志切面类，来给业务逻辑类进行切入

```java
// 切面类
@Aspect     // 告诉spring这是一个切面类
public class LogAspects {
    // 抽取公共的切入点表达式
    @Pointcut("execution(* com.xiaozhi.aopTest.MathCalculator.*(..))")
    public void pointCut(){};

    @Before("pointCut()")
    public void logStart(){
        System.out.println("除法运行，参数列表是{}");
    }

    @After("pointCut()")
    public void logEnd(){
        System.out.println("除法结束。。。");
    }

    @AfterReturning("pointCut()")
    public void logReturn(){
        System.out.println("方法正常返回，运行结果：{}");
    }

    @AfterReturning("pointCut()")
    public void lgoException(){
        System.out.println("除法异常。。。异常信息{}");
    }
}
```

**通知方法**

-   **前置通知（@Before）**：logStart：在目标方法（div）执行之前运行
-   **后置通知（@After）**：logEnd：在目标方法执行之后执行
-   **返回通知（@AfterReturning）**：logReturn：在目标方法正常返回后执行
-   **异常通知（@AfterThrowing）**：logException：在目标方法出现异常以后执行
-   **环绕通知（@Around）**：动态代理，手动推进目标方法进行（joinPoint.procced()）



4	开启aop注解功能

![image-20210325230940532](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210325230940532.png)

5	测试

```java
@Test
public void Test1(){
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainConfigOfAOP.class);
    // 不要自己创建对象，要用spring帮我们创建对象
    MathCalculator calculator = context.getBean(MathCalculator.class);
    calculator.div(1,1);
    context.close();
}
```

6	测试结果

![image-20210325231017330](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210325231017330.png)



7	我们可以通过传入对应的参数获取对应的值

```java
// 切面类
@Aspect     // 告诉spring这是一个切面类
public class LogAspects {
    // 抽取公共的切入点表达式
    @Pointcut("execution(* com.xiaozhi.aopTest.MathCalculator.*(..))")
    public void pointCut(){};

    // 传入对应的参数获取对应的值
    @Before("pointCut()")
    public void logStart(JoinPoint joinPoint){
        Object[] args = joinPoint.getArgs();    // 获取方法的参数列表
        System.out.println(joinPoint.getSignature().getName() +"运行，参数列表是{" + Arrays.asList(args) +"}");
    }

    @After("pointCut()")
    public void logEnd(JoinPoint joinPoint){
        System.out.println(joinPoint.getSignature().getName()+ "结束。。。");
    }

    // returuning参数告诉spring这个参数是用来接收方法返回值的
    @AfterReturning(value = "pointCut()",returning = "result")
    public void logReturn(Object result){
        System.out.println("方法正常返回，运行结果：{"+ result +"}");
    }

    // throwing参数告诉spring这个参数是用来接收异常的
    @AfterThrowing(value = "pointCut()",throwing = "exception")
    public void lgoException(Exception exception){
        System.out.println("除法异常。。。异常信息{" + exception + "}");
    }
}
```

**说明**：spring它会去判断切面类中的方法是否有参数，如果有参数的话，它会将对应的对象传给对应的参数，然后我们就可以使用了



### 三步走

1	将业务逻辑类和切面类加入到容器中，告诉spring那个是切面类（@Aspect）

2	在切面类上的每一个通知方法标注通知注解，告诉spring何时何地运行（切入点表达式）

3	开启基于注解的aop模式







