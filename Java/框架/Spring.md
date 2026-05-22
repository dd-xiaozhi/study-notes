# Spring的概述

## 简介

>   **spring**
>
>   是一个开源框架，它由Rod Johnson创建。它是为了解决企业应用开发的复杂性而创建的

**Rod Johnson是个音乐家**

==**Spring是一个轻量级的控制反转(IoC)和面向切面(AOP)的容器框架**。==

-   轻量级：只需要导入对应的jar包就可以使用
-   IOC：所有的对象有spring来托管和分配
-   AOP：Spring提供了**面向切面编程**的丰富支持，允许通过分离应用的业务逻辑与系统级服务（例如审计（auditing）和事务（transaction）管理）进行内聚性的开发
-   容器：Spring包含并管理应用对象的配置和生命周期，在这个意义上它是一种容器，你可以配置你的每个bean如何被创建——基于一个可配置原型（prototype），你的bean可以创建一个单独的实例或者每次需要时都生成一个新的实例——以及它们是如何相互关联的



**spring理念**：是现有的技术更加容易使用，本身是一个大杂烩。

-   SSH：Struct2 + Spring + Hibernate
-   SSM: SpringMVC + Spring + Mybatis



官网：https://spring.io/

官网下载地址：

GitHib：https://github.com/spring-projects/spring-framework





## 组成

![\[外链图片转存失败,源站可能有防盗链机制,建议将图片保存下来直接上传(img-2m3meIB5-1616062892078)(https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210114153058665.png)\]](https://img-blog.csdnimg.cn/20210318205121918.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


组成 Spring 框架的每个模块（或组件）都可以单独存在，或者与其他一个或多个模块联合实现。每个模块的功能如下：

-   **核心容器**：核心容器提供 Spring 框架的基本功能。核心容器的主要组件是 BeanFactory，它是工厂模式的实现。BeanFactory 使用*控制反转*（IOC） 模式将应用程序的配置和依赖性规范与实际的应用程序代码分开。
-   **Spring 上下文**：Spring 上下文是一个配置文件，向 Spring 框架提供上下文信息。Spring 上下文包括企业服务，例如 JNDI、EJB、电子邮件、国际化、校验和调度功能。
-   **Spring AOP**：通过配置管理特性，Spring AOP 模块直接将面向切面的编程功能 , 集成到了 Spring 框架中。所以，可以很容易地使 Spring 框架管理任何支持 AOP的对象。Spring AOP 模块为基于 Spring 的应用程序中的对象提供了事务管理服务。通过使用 Spring AOP，不用依赖组件，就可以将声明性事务管理集成到应用程序中。
-   **Spring DAO**：JDBC DAO 抽象层提供了有意义的异常层次结构，可用该结构来管理异常处理和不同数据库供应商抛出的错误消息。异常层次结构简化了错误处理，并且极大地降低了需要编写的异常代码数量（例如打开和关闭连接）。Spring DAO 的面向 JDBC 的异常遵从通用的 DAO 异常层次结构。
-   **Spring ORM**：Spring 框架插入了若干个 ORM 框架，从而提供了 ORM 的对象关系工具，其中包括 JDO、Hibernate 和 iBatis SQL Map。所有这些都遵从 Spring 的通用事务和 DAO 异常层次结构。
-   **Spring Web 模块**：Web 上下文模块建立在应用程序上下文模块之上，为基于 Web 的应用程序提供了上下文。所以，Spring 框架支持与 Jakarta Struts 的集成。Web 模块还简化了处理多部分请求以及将请求参数绑定到域对象的工作。
-   **Spring MVC 框架**：MVC 框架是一个全功能的构建 Web 应用程序的 MVC 实现。通过策略接口，MVC 框架变成为高度可配置的，MVC 容纳了大量视图技术，其中包括 JSP、Velocity、Tiles、iText 和 POI。



## 拓展

**Spring Boot与Spring Cloud**

-   Spring Boot 是 Spring 的一套快速配置脚手架，可以基于Spring Boot 快速开发单个微服务;
-   Spring Cloud是基于Spring Boot实现的；
-   Spring Boot专注于快速、方便集成的单个微服务个体，Spring Cloud关注全局的服务治理框架；
-   Spring Boot使用了约束优于配置的理念，很多集成方案已经帮你选择好了，能不配置就不配置 , Spring Cloud很大的一部分是基于Spring Boot来实现，Spring Boot可以离开Spring Cloud独立使用开发项目，但是Spring Cloud离不开Spring Boot，属于依赖的关系。
-   SpringBoot在SpringClound中起到了承上启下的作用，如果你要学习SpringCloud必须要学习SpringBoot。





## Spring的meven依赖

```xml
<!-- https://mvnrepository.com/artifact/org.springframework/spring-webmvc -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
    <version>5.2.5.RELEASE</version>
</dependency>

<!-- 整合mybait的时候使用 -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>5.2.3.RELEASE</version>
</dependency>
```









# IOC理论推导

## 思想

用一个例子去理解IOC的原型：

根据用户的需求动态修改Dao层

**Dao层**

-   创建一个Dao接口

    ```java
    public interface UserDao {
        void getUser();
    }
    ```

-   创建这个Dao接口的实现类

    ```java
    public class UserDaoImpl implements UserDao {
        @Override
        public void getUser() {
            System.out.println("默认获取用户数据");
        }
    }
    ```

    ```java
    public class UserDaoMySqlImpl implements UserDao{
        @Override
        public void getUser() {
            System.out.println("Mysql获取用户数据");
        }
    }
    ```

    ```java
    public class UserDaoOracleImpl implements UserDao {
        @Override
        public void getUser() {
            System.out.println("Oracle获取用户数据");
        }
    }
    ```



**Service层**

```java
public interface UserService {
    void getUser();
}
```

```java
public class UserServiceImpl implements UserService {
    private UserDao userDao;
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
    @Override
    public void getUser() {
        userDao.getUser();
    }
}
```



**测试**

```java
public class UserServiceTest {
    @Test
    public void Test(){
        UserServiceImpl userService = new UserServiceImpl();
        userService.setUserDao(new UserDaoOracleImpl());
        userService.getUser();
    }
}
```



**有set和没有set的区别**：

-   之前是主动创建对象，控制权在程序员手上。
-   使用set之后，是被动接受对象。

**总结**：我们可以看到，只需要往setUserDao()方法里面传入不同的对象就可以实现动态的修改了，就不用再进行多方面的修改，这个就是IOC思想





## IOC的本质

**控制反转IoC(Inversion of Control)，是一种设计思想，DI(依赖注入)是实现IoC的一种方法**，也有人认为DI只是IoC的另一种说法。没有IoC的程序中 , 我们使用面向对象编程 , 对象的创建与对象间的依赖关系完全硬编码在程序中，对象的创建由程序自己控制，控制反转后将对象的创建转移给第三方，个人认为所谓控制反转就是：获得依赖对象的方式反转了。

**IoC是Spring框架的核心内容**，使用多种方式完美的实现了IoC，可以使用XML配置，也可以使用注解，新版本的Spring也可以零配置实现IoC。

Spring容器在初始化时先读取配置文件，根据配置文件或元数据创建与组织对象存入容器中，程序使用时再从Ioc容器中取出需要的对象。

![\[外链图片转存失败,源站可能有防盗链机制,建议将图片保存下来直接上传(img-zYutQQAj-1616062892086)(https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210114154910458.png)\]](https://img-blog.csdnimg.cn/20210318205203654.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


采用XML方式配置Bean的时候，Bean的定义信息是和实现分离的，而采用注解的方式可以把两者合为一体，Bean的定义信息直接以注解的形式定义在实现类中，从而达到了零配置的目的。

**控制反转是一种通过描述（XML或注解）并通过第三方去生产或获取特定对象的方式。在Spring中实现控制反转的是IoC容器，其实现方法是依赖注入（Dependency Injection,DI）。**



==**补充**：BeanFactory和ApplicationContext接口的区别，BeanFactory是在使用的时候才会创建对象，ApplicationContext在服务器启动的时候就已经创建好了对象，推荐使用ApplicationContext，因为这个在启动的时候就已经创建好了对象，不用在运行的时候又创建对象，这样能够快速的进行调用。==







# 快速上手Spring

## Hello Spring

**属性介绍**

-   id：表示变量名
-   class：要创建对象的全类名
-   value：具体的值，基本数据类型
-   ref：引用spring容器中创建好的对象

1.  创建实体类

    ```java
    public class Hello {
        private String str;
        public String getStr() {
            return str;
        }
        public void setStr(String str) {
            this.str = str;
        }
    }
    ```

2.  创建实体类的配置文件并编写

    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://www.springframework.org/schema/beans
            https://www.springframework.org/schema/beans/spring-beans.xsd">
    
        <bean id="hello" class="com.xiaozhi.pojo.Hello">
            <property name="str" value="Hello Spring"/>
        </bean>
    </beans>
    ```

3.  测试

    -   通过ApplicationContext context = new ClassPathXmlApplicationContext("配置文件名");得到配置文件的全部信息
    -   getBean("变量名")  方法得到对应的对象

    ```java
    public class HelloTest {
        @Test
        public void Test(){
            ApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
            Hello hello = (Hello) context.getBean("hello");
            System.out.println(hello.getStr());
        }
    }
    ```



**注意**：核心是用set方法啊注入，如果没有set方法，那么就会异常，所以set方法是一定要加上的





## 用spring来写之前的例子

-   编写对应的配置文件

    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://www.springframework.org/schema/beans
            https://www.springframework.org/schema/beans/spring-beans.xsd">
        <bean id="userDao" class="com.xiaozhi.dao.UserDaoImpl"/>
        <bean id="userDaoMySql" class="com.xiaozhi.dao.UserDaoMySqlImpl"/>
        <bean id="userDaoOracle" class="com.xiaozhi.dao.UserDaoOracleImpl"/>
        
        <bean id="userService" class="com.xiaozhi.service.UserServiceImpl">
            <!--
                property表示属性值
                ref：引用spring容器中创建好的对象
    			value：具体的值，基本数据类型
            -->
            <property name="userDao" ref="userDao"/>
        </bean>
    </beans>
    ```

-   测试

    ```java
    @Test
    public void Test(){
        ApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
        UserService userService = (UserService) context.getBean("userService");
        userService.getUser();
    }
    ```

    

# IOC创建对象的方式

==**说明：在配置文件加载之前，对象就已经被实例化了**==

-   默认使用无参构造器去创建对象
-   使用有参构造器创建对象的三种方式
    -   下标赋值
    -   类型
    -   参数名

**代码实现**

## 无参构造的实现方式

1.  创建一个User类

    1.  

    ```java
    public class User {
        private String name;
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
    }
    ```

2.  编写对应的xml文件

    ```xml
    <bean id="user" class="com.xiaozhi.pojo.User">
        <property name="name" value="小智"/>
    </bean>
    ```

3.  测试



## 有参构造的实现方式

-   第一种方式：下标

    ```xml
    <bean id="user" class="com.xiaozhi.pojo.User">
        <constructor-arg index="0" value="小智"/>
    </bean>
    ```



-   第二种方式：类型（不建议使用）

    ```xml
    <bean id="user" class="com.xiaozhi.pojo.User">
        <constructor-arg type="java.lang.String" value="小智"/>
    </bean>
    ```



-   第三种方式：参数名的方式

    ```xml
    <bean id="user" class="com.xiaozhi.pojo.User">
        <constructor-arg name="name" value="小智"/>
    </bean>
    ```



![\[外链图片转存失败,源站可能有防盗链机制,建议将图片保存下来直接上传(img-rnP3ubrp-1616062892087)(https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20210114163110811.png)\]](https://img-blog.csdnimg.cn/20210318205228796.png)


**旁边出现叶子就是已经被spring容器托管了**



==**注意**：每个bean它在内存中只存在一份实例==

```java
@Test
public void Test(){
    ApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
    User abc = (User) context.getBean("haha");
    User abc2 = (User) context.getBean("haha");
    System.out.println(abc == abc2);	// true
}
```







# Spring的配置

## 别名

### 第一种方式

alias 设置别名 , 为bean设置别名 , 只能设置一个

-   pojo类

    ```java
    @AllArgsConstructor
    @Getter
    @Setter
    public class User {
        private String name;
    }
    ```

-   对应的配置文件

    ```xml
    <bean id="user" class="com.xiaozhi.pojo.User">
        <constructor-arg name="name" value="小智"/>
    </bean>
    <alias name="user" alias="abc"/>
    ```

-   测试

    ```java
    @Test
    public void Test(){
        ApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
        User abc = (User) context.getBean("abc");
        System.out.println(abc.getName());
    }
    ```



### 第二种方式

name属性设置别名，可以设置多个，用逗号、空格、分号隔开都可以

-   编写配置文件

    ```xml
    <bean id="user" class="com.xiaozhi.pojo.User" name="xiaozhi,haha">
        <constructor-arg name="name" value="小智"/>
    </bean>
    ```





## Bean的配置

-   id：唯一标识符，变量名(对象名),如果没有配置id，name就是默认标识符
-   class：全类限定名=包名+类名
-   name：别名



## import

团队的合作通过import来实现，多人开发的情况下，每个人编写的类会不一样，通过import可以将多个xml配置文件合并为一个，如果存在一样的配置，合并后只会存在一个

```xml
<import resource="{path}/beans.xml"/>
```



**代码实现**

```xml
<import resource="beans.xml"/>
<import resource="beans2.xml"/>
<import resource="beans3.xml"/>
```

![\[外链图片转存失败,源站可能有防盗链机制,建议将图片保存下来直接上传(img-R1N4OxD6-1616062892088)(F:\Java学习\截图\image-20210114171448623.png)\]](https://img-blog.csdnimg.cn/20210318205247214.png)




**测试**

```java
@Test
public void Test(){
    ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
    User abc = (User) context.getBean("haha");
    System.out.println(abc.getName());
}
```





# 依赖注入

-   依赖注入（Dependency Injection,DI）。
-   依赖 : 指Bean对象的创建依赖于容器 . Bean对象的依赖资源 .
-   注入 : 指Bean对象所依赖的资源 , 由容器来设置和装配 .



## 构造器注入

开始的例子就是构造器注入

value：基本数据类型

ref：引用数据类型

- 对象
- 集合类型 -> 获取到所有该类型的bean



## set方式注入(重点)

要求被注入的属性 , 必须有set方法 , set方法的方法名由set + 属性首字母大写 , 如果属性是boolean类型 , 没有set方法 , 是 is .



**代码实现**

**pojo类**

```java
public class Address {
    private String address;
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
}
```

```java
public class Student {
    private String name;
    private Address address;
    private String[] books;
    private List<String> hobbys;
    private Map<String,String> card;
    private Set<String> games;
    private String wife;
    private Properties info;
```

==注：还有get、set、toString方法==

1.  **常量注入**

    ```xml
    <bean id="student" class="com.xiaozhi.pojo.Student">
        <property name="name" value="小智"/>
    </bean>
    ```

2.  **bean（实体类）注入**

    第一种方式：引用的方式

    ```xml
    <bean id="address" class="com.xiaozhi.pojo.Address">
        <property name="address" value="广东"/>
    </bean>
    <bean id="student" class="com.xiaozhi.pojo.Student">
        <property name="name" value="小智"/>
        <property name="address" ref="address"/>
    </bean>
    ```

    第二种方式：嵌套的方式

    ```xml
    <bean id="student" class="com.xiaozhi.pojo.Student">
        <property name="name" value="小智"/>
        <property name="address">
            <bean class="com.xiaozhi.pojo.Address">
                <property name="address" value="广东"/>
            </bean>
        </property>
    </bean>
    ```

3.  **数组注入**

    ```xml
    <property name="books">
        <array>
            <value>西游记</value>
            <value>有趣的你想思维</value>
            <value>狼爱上羊</value>
        </array>
    </property>
    ```

4.  List注入

    ```xml
    <property name="hobbys">
        <list>
            <value>打篮球</value>
            <value>听歌</value>
            <value>敲代码</value>
        </list>
    </property>
    ```

5.  Map注入

    ```xml
    <property name="card">
        <map>
            <entry key="1" value="邮政"/>
            <entry key="2" value="央行"/>
        </map>
    </property>
    ```

6.  set注入

    ```xml
    <property name="games">
        <set>
            <value>吃鸡</value>
            <value>王者</value>
            <value>植物大战僵尸</value>
        </set>
    </property>
    ```

7.  Null注入

    ```xml
    <property name="wife">
        <null/>
    </property>
    ```

8.  Properties注入

    ```xml
    <property name="info">
        <props>
            <prop key="学号">12345</prop>
            <prop key="姓名">玫瑰</prop>
            <prop key="性别">女</prop>
        </props>
    </property>
    ```

**测试**

```java
@Test
public void Test1(){
    ApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
    Student student = (Student) context.getBean("student");
    System.out.println(student.toString());
    /*
    结果显示：
        Student{
         name='小智',
         address=Address{address='广东'},
         books=[西游记, 有趣的你想思维, 狼爱上羊],
         hobbys=[打篮球, 听歌, 敲代码],
         card={1=邮政, 2=央行},
         games=[吃鸡, 王者, 植物大战僵尸],
         wife='null',
         info={学号=12345, 性别=女, 姓名=玫瑰}
        }
     */
}
```



## p命名和c命名注入

-   P命名空间注入 : 需要在头文件中加入约束文件

    **说明**：P就是所谓的属性注入

    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:p="http://www.springframework.org/schema/p"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://www.springframework.org/schema/beans
            https://www.springframework.org/schema/beans/spring-beans.xsd">
        <!--set注入的方式-->
        <bean id="address" class="com.xiaozhi.pojo.Address">
            <property name="address" value="广东"/>
        </bean>
        <!--P命名注入的方式-->
        <bean id="address2" class="com.xiaozhi.pojo.Address" p:address="广东"/>
    </beans>
    ```

-   c 命名空间注入 : 需要在头文件中加入约束文件

    **说明**：c 就是所谓的构造器注入！

    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:c="http://www.springframework.org/schema/c"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://www.springframework.org/schema/beans
            https://www.springframework.org/schema/beans/spring-beans.xsd">
        <!--构造器注入的方式-->
        <bean id="address" class="com.xiaozhi.pojo.Address">
            <constructor-arg name="address" value="广东"/>
        </bean>
        <!--C命名注入的方式-->
        <bean id="address2" class="com.xiaozhi.pojo.Address" c:address="小智"/>
    </beans>
    ```

    ==注意：如果不是有参构造，它会报红线，所以使用C命名一定要有参构造==





# Bean的作用域

在Spring中，那些组成应用程序的主体及由Spring IoC容器所管理的对象，被称之为bean。简单地讲，bean就是由IoC容器初始化、装配及管理的对象 .

![\[外链图片转存失败,源站可能有防盗链机制,建议将图片保存下来直接上传(img-uLKKyzvc-1616062892089)(F:\Java学习\截图\image-20210114232012148.png)\]](https://img-blog.csdnimg.cn/20210318205309202.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


几种作用域中，request、session作用域仅在基于web的应用中使用（不必关心你所采用的是什么web应用框架），只能用在基于web的Spring ApplicationContext环境。



## 1	Singleton(单例)

当一个bean的作用域为Singleton，那么Spring IoC容器中只会存在一个共享的bean实例，并且所有对bean的请求，只要id与该bean定义相匹配，则只会返回bean的同一实例。Singleton是单例类型，就是在创建起容器时就同时自动创建了一个bean的对象，不管你是否使用，他都存在了，每次获取到的对象都是同一个对象。注意，Singleton作用域是Spring中的缺省作用域。要在XML中将bean定义成singleton，可以这样配置：

```xml
 <bean id="ServiceImpl" class="cn.csdn.service.ServiceImpl" scope="singleton">
```

测试：

```java
 @Test
 public void test03(){
     ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
     User user = (User) context.getBean("user");
     User user2 = (User) context.getBean("user");
     System.out.println(user==user2);
 }
```



## 2	Prototype(原型)

当一个bean的作用域为Prototype，表示一个bean定义对应多个对象实例。Prototype作用域的bean会导致在每次对该bean请求（将其注入到另一个bean中，或者以程序的方式调用容器的getBean()方法）时都会创建一个新的bean实例。Prototype是原型类型，它在我们创建容器的时候并没有实例化，而是当我们获取bean的时候才会去创建一个对象，而且我们每次获取到的对象都不是同一个对象。根据经验，对有状态的bean应该使用prototype作用域，而对无状态的bean则应该使用singleton作用域。在XML中将bean定义成prototype，可以这样配置：

```xml
 <bean id="account" class="com.foo.DefaultAccount" scope="prototype"/>  
```



## 3	Request(请求)

当一个bean的作用域为Request，表示在一次HTTP请求中，一个bean定义对应一个实例；即每个HTTP请求都会有各自的bean实例，它们依据某个bean定义创建而成。该作用域仅在基于web的Spring ApplicationContext情形下有效。考虑下面bean定义：

```xml
 <bean id="loginAction" class=cn.csdn.LoginAction" scope="request"/>
```

针对每次HTTP请求，Spring容器会根据loginAction bean的定义创建一个全新的LoginAction bean实例，且该loginAction bean实例仅在当前HTTP request内有效，因此可以根据需要放心的更改所建实例的内部状态，而其他请求中根据loginAction bean定义创建的实例，将不会看到这些特定于某个请求的状态变化。当处理请求结束，request作用域的bean实例将被销毁。



## 4	Session(会话)

当一个bean的作用域为Session，表示在一个HTTP Session中，一个bean定义对应一个实例。该作用域仅在基于web的Spring ApplicationContext情形下有效。考虑下面bean定义：

```xml
 <bean id="userPreferences" class="com.foo.UserPreferences" scope="session"/>
```

针对某个HTTP Session，Spring容器会根据userPreferences bean定义创建一个全新的userPreferences bean实例，且该userPreferences bean仅在当前HTTP Session内有效。与request作用域一样，可以根据需要放心的更改所创建实例的内部状态，而别的HTTP Session中根据userPreferences创建的实例，将不会看到这些特定于某个HTTP Session的状态变化。当HTTP Session最终被废弃的时候，在该HTTP Session作用域内的bean也会被废弃掉。







# 自动装配

## 自动装配说明

-   自动装配是使用spring满足bean依赖的一种方法
-   spring会在应用上下文中为某个bean寻找其依赖的bean。

Spring中bean有三种装配机制，分别是：

1.  在xml中显式配置；
2.  在java中显式配置；
3.  隐式的bean发现机制和自动装配。**(重点)**

这里我们主要讲第三种：自动化的装配bean。

Spring的自动装配需要从两个角度来实现，或者说是两个操作：

1.  组件扫描(component scanning)：spring会自动发现应用上下文中所创建的bean；
2.  自动装配(autowiring)：spring自动满足bean之间的依赖，也就是我们说的IoC/DI；

组件扫描和自动装配组合发挥巨大威力，使得显示的配置降低到最少。

**推荐不使用自动装配xml配置 , 而使用注解 .**

<img src="https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/image-20220315205729982.png" alt="image-20220315205729982" style="zoom:100%;" />



==autowire四种模式的区别：==

![这里写图片描述](https://img-blog.csdn.net/20160824172800163)



## 测试环境搭建

1、新建一个项目

2、新建两个实体类，Cat  Dog  都有一个叫的方法

```java
public class Dog {
    public void show(){
        System.out.println("汪汪汪");
    }
}
```

```java
public class Cat {
    public void show() {
        System.out.println("喵喵喵");
    }
}
```

3、新建一个用户类 People、set和get方法必须要写

```java
public class People {
   private Cat cat;
   private Dog dog;
   private String str;
}
```

4、编写Spring配置文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:c="http://www.springframework.org/schema/c"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
        https://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean class="com.xiaozhi.pojo.Cat" id="cat"/>
    <bean class="com.xiaozhi.pojo.Dog" id="dog"/>

    <bean class="com.xiaozhi.pojo.People" id="people" autowire="byName">
        <property name="name" value="小智家的"/>
        <property name="dog" ref="dog"/>
        <property name="cat" ref="cat"/>
     </bean>
</beans>
```

5、测试

```java
@Test
    public void Test1(){
        ApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
        People people = context.getBean("people", People.class);
        people.getCat().show();
        people.getDog().show();
        System.out.println(people.getName());
    }
```

结果正常输出，环境OK



## byName

**autowire byName (按名称自动装配)**

**通过id或者属性给对应的set方法注入值**

由于在手动配置xml过程中，常常发生字母缺漏和大小写等错误，而无法对其进行检查，使得开发效率降低。

采用自动装配将避免这些错误，并且使配置简单化。



**测试**：

1、修改bean配置，增加一个属性  autowire="byName"

```xml
<bean class="com.xiaozhi.pojo.Cat" id="cat"/>
<bean class="com.xiaozhi.pojo.Dog" id="dog"/>

<bean class="com.xiaozhi.pojo.People" id="people" autowire="byName">
    <property name="name" value="小智家的"/>
    <!--<property name="dog" ref="dog"/>
        <property name="cat" ref="cat"/>-->
</bean>
```

2、再次测试，结果依旧成功输出！

3、我们将 cat 的bean id修改为 catXXX

4、再次测试， 执行时报空指针java.lang.NullPointerException。

==**注意**：byName是根据setXXX中的XXX去找对应的set方法，然后将对象注入，如果你的id后name与之不对应，就会导致真正的setCat就没执行，对象就没有初始化，所以调用时就会报空指针错误。==

**小结：**

当一个bean节点带有 autowire byName的属性时。

1.  将查找其类中所有的set方法名，例如setCat，获得将set去掉并且首字母小写的字符串，即cat。

2.  去spring容器中寻找是否有此字符串名称id的对象。

3.  如果有，就取出注入；如果没有，就报空指针异常。

    

## byType

**autowire byType (按类型自动装配)**

使用autowire byType首先需要保证：同一类型的对象，在spring容器中唯一。如果不唯一，会报不唯一的异常。

```java
NoUniqueBeanDefinitionException
```

测试：

1、将user的bean配置修改一下 ： autowire="byType"

2、测试，正常输出

3、在注册一个cat 的bean对象！

```xml
<bean class="com.xiaozhi.pojo.Cat" id="cat"/>
    <bean class="com.xiaozhi.pojo.Dog" id="dog"/>
    <bean class="com.xiaozhi.pojo.Dog" id="dog2"/>

    <bean class="com.xiaozhi.pojo.People" id="people" autowire="byType">
        <property name="name" value="小智家的"/>
<!--        <property name="dog" ref="dog"/>-->
<!--        <property name="cat" ref="cat"/>-->
     </bean>
```

4、测试，报错：**NoUniqueBeanDefinitionException**

5、报错的原因：因为有两个类型一样的bean对象存在，同一类型的对象是不允许出现的，会报错

这就是按照类型自动装配！







## 使用注解

jdk1.5开始支持注解，spring2.5开始全面支持注解。

准备工作：利用注解的方式注入属性。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
        https://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/context
        https://www.springframework.org/schema/context/spring-context.xsd">

    <context:annotation-config/>
    
</beans>
```

**1、在spring配置文件中引入context文件头**

```xml
xmlns:context="http://www.springframework.org/schema/context"

http://www.springframework.org/schema/context
http://www.springframework.org/schema/context/spring-context.xsd
```

**2、开启属性注解支持！**

```xml
<context:annotation-config/>
```

==这两步一定要做！！！==



### @Autowired

-   它代替的是ByType方式

-   @Autowired是**按类型自动转配**的，不支持id匹配。

-   需要导入 spring-aop的包！

    ![\[外链图片转存失败,源站可能有防盗链机制,建议将图片保存下来直接上传(img-hdOn48PB-1616062892090)(F:\Java学习\截图\image-20210115235430483.png)\]](https://img-blog.csdnimg.cn/20210318205407439.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)

可以注入集合类型，比如`List<User> userList` 这种方式，获取所有User类型的bean


**测试：**

1、将User类中的set方法去掉，使用@Autowired注解

```java
public class People {
    @Autowired
    private Dog dog;
    @Autowired
    private Cat cat;
    private String name;
    public String getName() {
        return name;
    }
    public Dog getDog() {
        return dog;
    }
    public Cat getCat() {
        return cat;
    }
}
```

2、此时配置文件内容

```xml
    <context:annotation-config/>

    <bean class="com.xiaozhi.pojo.People" id="people"/>
    <bean class="com.xiaozhi.pojo.Dog" id="dog2"/>
    <bean class="com.xiaozhi.pojo.Cat" id="cat"/>
</beans>
```

3、测试，成功输出结果！

@Autowired(required=false)  说明：false，对象可以为null；true，对象必须存对象，不能为null。

```java
//如果允许对象为null，设置required = false,默认为true
@Autowired(required = false)
private Cat cat;
```

==**注意**：是可以存在set方法并且可以给set方法加注释达到自动装配的效果的==





### @Qualifier

-   @Autowired和@Qualifier一起使用可以根据byName的方式自动装配
-   @Qualifier不能单独使用。

**测试实验步骤**：

1、配置文件修改内容，保证类型存在对象。且名字不为类的默认名字！

```xml
<bean class="com.xiaozhi.pojo.Dog" id="dog1"/>
<bean class="com.xiaozhi.pojo.Dog" id="dog2"/>
<bean class="com.xiaozhi.pojo.Cat" id="cat1"/>
<bean class="com.xiaozhi.pojo.Cat" id="cat2"/>
```

2、没有加Qualifier测试，直接报错，因为类型卜不唯一

3、在属性上添加Qualifier注解

```java
@Autowired
    @Qualifier(value = "dog1")
    private Dog dog;
    @Autowired
    @Qualifier(value = "cat1")
    private Cat cat;
```

测试，成功输出！





### @Resource

-   @Resource如有指定的name属性，**先按该属性进行byName方式**查找装配；
-   其次再进行默认的byName方式进行装配；
-   如果以上都不成功，则按byType的方式自动装配。
-   都不成功，则报异常。

实体类：

```java
    @Resource(name = "dog1")
    private Dog dog;
    @Resource(name = "cat1")
    private Cat cat;
```

beans.xml

```xml
    <bean class="com.xiaozhi.pojo.Dog" id="dog1"/>
    <bean class="com.xiaozhi.pojo.Dog" id="dog"/>
    <bean class="com.xiaozhi.pojo.Cat" id="cat1"/>
    <bean class="com.xiaozhi.pojo.Cat" id="cat"/>
```

测试：结果OK

配置文件2：beans.xml ， 删掉cat1和dog1

```xml
    <bean class="com.xiaozhi.pojo.Dog" id="dog1"/>
    <bean class="com.xiaozhi.pojo.Cat" id="cat2"/>
```

实体类上只保留注解

```java
@Resource
private Cat cat;
@Resource
private Dog dog;
```

结果：OK

结论：先进行byName查找，失败；再进行byType查找，成功。



### 小结

**@Autowired与@Resource异同**：

1、@Autowired与@Resource都可以用来装配bean。都可以写在属性上，或写在setter方法上。

2、@Autowired默认按类型装配（属于spring规范），默认情况下必须要求依赖对象必须存在，如果要允许null 值，可以设置它的required属性为false，如：@Autowired(required=false) ，如果我们想使用名称装配可以结合@Qualifier注解进行使用

3、@Resource（属于J2EE复返），默认按照名称进行装配，名称可以通过name属性进行指定。如果没有指定name属性，当注解写在字段上时，默认取字段名进行按照名称查找，如果注解写在setter方法上默认取属性名进行装配。当找不到与名称匹配的bean时才按照类型进行装配。但是需要注意的是，如果name属性一旦指定，就只会按照名称进行装配。

它们的作用相同都是用注解方式注入对象，但**执行顺序不同**。@Autowired先byType，@Resource先byName。



==自动装配就是将引用数据类型的分配叫给Spring容器，由Spring容器俩进行分发==





# 使用注解开发

**说明**：在spring4之后，想要使用注解形式，必须得要引入aop的包



![\[外链图片转存失败,源站可能有防盗链机制,建议将图片保存下来直接上传(img-2qnQBDKs-1616062892090)(F:\Java学习\截图\image-20210115235430483.png)\]](https://img-blog.csdnimg.cn/20210318205426347.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


在配置文件当中，还得要引入一个context约束

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xmlns:context="http://www.springframework.org/schema/context"
      xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/context
       http://www.springframework.org/schema/context/spring-context.xsd">

</beans>
```



## Bean的实现

**说明**：我们之前都是使用 bean 的标签进行bean注入，但是实际开发中，我们一般都会使用注解！

1、配置扫描哪些包下的注解

```xml
<!--扫描com.xiaozhi.pojo包下的注解-->
<context:component-scan base-package="com.xiaozhi.pojo"/>
```

2、在指定包下编写类，增加注解

```java
// 加上注释就相当于是配置文件中<bean id="user" class="当前注解的类"/>，value是它的变量名
@Component("people")
public class People {
    public String name = "小智";
}
```

3、测试

```java
@Test
public void Test1(){
    ApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
    People people = context.getBean("people", People.class);
    System.out.println(people.name);
}
```



## 属性注入

使用注解注入属性

1、可以不用提供set方法，直接在直接名上添加@value("值")

```java
@Component("people")
public class People {
    @Value("小智")
    private String name;
    public String getName() {
        return name;
    }
}
```

2、如果提供了set方法，在set方法上添加@value("值");

```java
@Component("people")
public class People {
    private String name;
    public String getName() {
        return name;
    }
    @Value("小智")
    public void setName(String name) {
        this.name = name;
    }
}
```

测试

```java
@Test
public void Test1(){
    ApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
    People people = context.getBean("people", People.class);
    System.out.println(people.getName());
}
```





## 衍生注解

我们这些注解，就是替代了在配置文件当中配置步骤而已！更加的方便快捷！

**@Component三个衍生注解**

为了更好的进行分层，Spring可以使用其它三个注解，功能一样，目前使用哪一个功能都一样。

-   @Controller：web层
-   @Service：service层
-   @Repository：dao层

写上这些注解，就相当于将这个类交给Spring管理装配了！

**说明**：功能一样，只是名字不同而已，增加可阅读性





## 自动装配注解

在Bean的自动装配有，可以回顾！





## 作用域

@scope

-   singleton：默认的，Spring会采用单例模式创建这个对象。关闭工厂 ，所有的对象都会销毁。
-   prototype：多例模式。关闭工厂 ，所有的对象不会销毁。内部的垃圾回收机制会回收

```java
@Component("people")
@Scope("singleton")
public class People {
    @Value("小智")
    private String name;
    public String getName() {
        return name;
    }
}
```



## 小结

**XML与注解比较**

-   XML可以适用任何场景 ，结构清晰，维护方便
-   注解不是自己提供的类使用不了，开发简单方便

**xml与注解整合开发** ：推荐最佳实践

-   xml管理Bean
-   注解完成属性注入
-   使用过程中， 可以不用扫描，扫描是为了类上的注解

```xml
<context:annotation-config/>  
```

作用：

-   进行注解驱动注册，从而使注解生效

-   用于激活那些已经在spring容器里注册过的bean上面的注解，也就是显示的向Spring注册

-   如果不扫描包，就需要手动配置bean

-   如果不加注解驱动，则注入的值为null！

    

## 基于Java类进行配置

JavaConfig 原来是 Spring 的一个子项目，它通过 Java 类的方式提供 Bean 的定义信息，在 Spring4 的版本， JavaConfig 已正式成为 Spring4 的核心功能 。

测试：

1、编写一个实体类，Dog

```java
@Component  //将这个类标注为Spring的一个组件，放到容器中！
public class Dog {
    public String name = "dog";
}
```

2、新建一个config配置包，编写一个MyConfig配置类

bean注释有name和value属性，都可以设置它的变量名

```java
@Configuration	// 它相当于我们之前的配置文件beans.xml
public class MyConfig {
    @Bean   //通过方法注册一个bean，这里的返回值就Bean的类型，方法名就是bean的id！
    public Dog dog(){
        return new Dog();
    }
}
```

3、测试

```java
@Test
public void Test1() {
    ApplicationContext context = new AnnotationConfigApplicationContext(MyConfig.class);
    Dog dog = context.getBean("dog", Dog.class);
    System.out.println(dog.name);
}
```

4、成功输出结果！



**导入其他配置如何做呢？**

1、我们再编写一个配置类！

```java
@Configuration  //代表这是一个配置类
public class MyConfig2 {
}
```

2、在之前的配置类中我们来选择导入这个配置类

```java
@Configuration
@Import(MyConfig2.class)    //导入合并其他配置类，类似于配置文件中的 inculde 标签
public class MyConfig {
    @Bean 
    public Dog dog(){
        return new Dog();
    }
}
```

关于这种Java类的配置方式，我们在之后的SpringBoot 和 SpringCloud中还会大量看到，我们需要知道这些注解的作用即可！







# 静态/动态代理模式

为什么要学习代理模式，因为AOP的底层机制就是动态代理！

代理模式：

-   静态代理
-   动态代理

学习aop之前 , 我们要先了解一下代理模式！

![image-20220316111837517](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/image-20220316111837517.png)




## 静态代理

**静态代理角色分析**

-   抽象角色 : 一般使用接口或者抽象类来实现

-   真实角色 : 被代理的角色

-   代理角色 : 代理真实角色 ; 代理真实角色后 , 一般会做一些附属的操作 .

-   客户  :  使用代理角色来进行一些操作 .

    

**代码实现**

Rent . java 即抽象角色

```java
//抽象角色：租房
public interface Rent {
   public void rent();
}
```

Host . java 即真实角色

```java
//真实角色: 房东，房东要出租房子
public class Host implements Rent{
   public void rent() {
       System.out.println("房屋出租");
  }
}
```

Proxy . java 即代理角色

```java
// 代理角色：中介
public class IntermediaryAgent implements Rent{
    private Host host;
    public IntermediaryAgent(Host host) {
        this.host = host;
    }
    @Override
    public void rent() {
        // 帮房东卖房子
        host.rent();
        // 中介可以做的事
        seeHouse();
        fare();
    }
    //看房
    public void seeHouse(){
        System.out.println("带房客看房");
    }
    //收中介费
    public void fare(){
        System.out.println("收中介费");
    }
}
```

Client . java 即客户

```java
//客户类，一般客户都会去找代理！
public class Client {
    public static void main(String[] args) {
        Host host = new Host();
        Proxy proxy = new Proxy(host);
        proxy.rent();
    }
}
```

**分析**：在这个过程中，我们没有和房东打交道，但是还是租到了房，也就是说中介它完成了租房的业务同时也完成了他自己的业务，比如看房和签合同。



**静态代理的好处:**

-   可以使得我们的真实角色更加纯粹 . 不再去关注一些公共的事情 .
-   公共的业务由代理来完成 . **实现了业务的分工** 
-   公共业务发生扩展时变得更加集中和方便 .

缺点 :

-   类多了 , 多了代理类 , 工作量变大了 . 开发效率降低 .

我们想要静态代理的好处，又不想要静态代理的缺点，所以 , 就有了动态代理 !



## 静态代理再理解

练习步骤：

1、创建一个抽象角色，比如咋们平时做的用户业务，抽象起来就是增删改查！

```java
//抽象角色：增删改查业务
public interface UserService {
    public void add();
    public void update();
    public void delete();
    public void select();
}
```

2、我们需要一个真实对象来完成这些增删改查操作

```java
//真实对象，完成增删改查操作的人
public class UserServiceImpl implements UserService{
    @Override
    public void add() {
        System.out.println("添加了一名用户");
    }
    @Override
    public void update() {
        System.out.println("修改了一名用户");
    }
    @Override
    public void delete() {
        System.out.println("删除了一名用户");
    }
    @Override
    public void select() {
        System.out.println("查询了一名用户");
    }
}
```

3、需求来了，现在我们需要增加一个日志功能，怎么实现！

-   思路1 ：在实现类上增加代码 【麻烦！】
-   思路2：使用代理来做，能够不改变原来的业务情况下，实现此功能就是最好的了！

4、设置一个代理类来处理日志！代理角色

```java
//代理角色，在这里面增加日志的实现
public class UserServiceProxy implements UserService{
    private UserService userService;
    public UserServiceProxy(UserService userService) {
        this.userService = userService;
    }
    @Override
    public void add() {
        userService.add();
        addLog("add");
    }
    @Override
    public void update() {
        userService.update();
        addLog("update");
    }
    @Override
    public void delete() {
        userService.delete();
        addLog("delete");
    }
    @Override
    public void select() {
        userService.select();
        addLog("select");
    }
    public void addLog(String msg){
        System.out.println("添加了" + msg + "方法");
    }
}
```

5、测试访问类：

```java
public class Client {
    public static void main(String[] args) {
        // 真实业务
        UserService userService = new UserServiceImpl();
        // 代理类
        UserServiceProxy proxy = new UserServiceProxy(userService);
        // 使用代理类实现添加日志功能
        proxy.add();
    }
}
```

**说明**：我们在不改变原来的代码的情况下，实现了对原有功能的增强，这是AOP中最核心的思想





聊聊AOP：纵向开发，横向开发

![image-20220316111814461](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/image-20220316111814461.png)






## 动态代理

-   动态代理的角色和静态代理的一样 .

-   动态代理的代理类是动态生成的 . 静态代理的代理类是我们提前写好的

-   动态代理分为两类 : 一类是基于接口动态代理 , 一类是基于类的动态代理

-   -   基于接口的动态代理----JDK动态代理
    -   基于类的动态代理--cglib
    -   现在用的比较多的是 javasist 来生成动态代理 . 百度一下javasist
    -   我们这里使用JDK的原生代码来实现，其余的道理都是一样的！



**JDK的动态代理需要了解两个类**

核心 : InvocationHandler   和   Proxy  ， 打开JDK帮助文档查看



【InvocationHandler：调用处理程序】

![image-20220316111757245](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/image-20220316111757245.png)


```java
Object invoke(Object proxy, 方法 method, Object[] args)；
//参数
//proxy - 调用该方法的代理实例
//method -所述方法对应于调用代理实例上的接口方法的实例。方法对象的声明类将是该方法声明的接口，它可以是代理类继承该方法的代理接口的超级接口。
//args -包含的方法调用传递代理实例的参数值的对象的阵列，或null如果接口方法没有参数。原始类型的参数包含在适当的原始包装器类的实例中，例如java.lang.Integer或java.lang.Boolean 。
```





【Proxy  : 代理】

![image-20220316111655029](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/image-20220316111655029.png)


![image-20220316111716057](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/image-20220316111716057.png)


![image-20220316111738752](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/image-20220316111738752.png)




```java
//生成代理类
public Object getProxy(){
   return Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                 rent.getClass().getInterfaces(),this);
}
```

**代码实现** 

抽象角色和真实角色和之前的一样！

Rent . java 即抽象角色

```java
//抽象角色：租房
public interface Rent {
   public void rent();
}
```

Host . java 即真实角色

```java
//真实角色: 房东，房东要出租房子
public class Host implements Rent{
   public void rent() {
       System.out.println("房屋出租");
  }
}
```

ProxyInvocationHandler. java 即代理角色

```java
public class ProxyInvocationHandler implements InvocationHandler {
    private Rent rent;

    public void setRent(Rent rent) {
        this.rent = rent;
    }

    /*
            生成代理类
                第一个参数是代理类的类加载器
                第二个参数是接口的方法列表
                第三个参数是调用处理程序的方法
         */
    public Object getProxy(){
        return Proxy.newProxyInstance(this.getClass().getClassLoader()
                ,rent.getClass().getInterfaces(),this);
    }
    
    // proxy : 代理类 method : 代理类的调用处理程序的方法对象.
    // 处理代理实例上的方法调用并返回结果
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        seeHouse();
        // 核心：本质利用反射实现
        Object result = method.invoke(rent, args);
        fare();
        return result;
    }
    // 看房
    public void seeHouse(){
        System.out.println("带客户看房子");
    }
    // 收中介费
    public void fare(){
        System.out.println("收中介费");
    }
}
```

Client . java

```java
//租客
public class Client {
    public static void main(String[] args) {
        //真实角色
        Host host = new Host();
        //代理实例的调用处理程序
        ProxyInvocationHandler pih = new ProxyInvocationHandler();
        pih.setRent(host); //将真实角色放置进去！
        Rent proxy = (Rent) pih.getProxy(); //动态生成对应的代理类！
        proxy.rent();
    }
}
```

核心：**一个动态代理 , 一般代理某一类业务 , 一个动态代理可以代理多个类，代理的是接口！、**



## 深化理解

我们来使用动态代理实现代理我们后面写的UserService！

我们也可以编写一个通用的动态代理实现的类！所有的代理对象设置为Object即可！

```java
public class ProxyInvocationHandler implements InvocationHandler {
   private Object target;

   public void setTarget(Object target) {
       this.target = target;
  }
   //生成代理类
   public Object getProxy(){
       return Proxy.newProxyInstance(this.getClass().getClassLoader(),
               target.getClass().getInterfaces(),this);
  }
   // proxy : 代理类
   // method : 代理类的调用处理程序的方法对象.
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
       log(method.getName());
       Object result = method.invoke(target, args);
       return result;
  }
   public void log(String methodName){
       System.out.println("执行了"+methodName+"方法");
  }
}
```

测试！

```java
public class Test {
   public static void main(String[] args) {
       //真实对象
       UserServiceImpl userService = new UserServiceImpl();
       //代理对象的调用处理程序
       ProxyInvocationHandler pih = new ProxyInvocationHandler();
       pih.setTarget(userService); //设置要代理的对象
       UserService proxy = (UserService)pih.getProxy(); //动态生成代理类！
       proxy.delete();
  }
}
```

测试，增删改查，查看结果！



**动态代理的好处**

静态代理有的它都有，静态代理没有的，它也有！

-   可以使得我们的真实角色更加纯粹 . 不再去关注一些公共的事情 .
-   公共的业务由代理来完成 . 实现了业务的分工 ,
-   公共业务发生扩展时变得更加集中和方便 .
-   一个动态代理 , 一般代理某一类业务
-   一个动态代理可以代理多个类，代理的是接口！



## 通用的代理类

```java
public class ProxyInvocationHandler implements InvocationHandler {
    private Object target;
    public void setTarget(Object target) {
        this.target = target;
    }

    public Object getProxy(){
        return Proxy.newProxyInstance(this.getClass().getClassLoader()
                ,target.getClass().getInterfaces(),this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Object result = method.invoke(target, args);
        return result;
    }
}
```



==**要注意的问题**：Java代理不可以对类进行代理，只能针对接口代理==

![image-20220316114055135](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/image-20220316114055135.png)


这个错误是因为对类进行了代理导致的





# AOP

## 什么是AOP

AOP（Aspect Oriented Programming）意为：面向切面编程，通过预编译方式和运行期动态代理实现程序功能的统一维护的一种技术。AOP是OOP的延续，是软件开发中的一个热点，也是Spring框架中的一个重要内容，是函数式编程的一种衍生范型。利用AOP可以对业务逻辑的各个部分进行隔离，从而使得业务逻辑各部分之间的耦合度降低，提高程序的可重用性，同时提高了开发的效率。



![图片](https://img-blog.csdnimg.cn/img_convert/1594ba45fa8f1f8232db24b6b9a9f678.png)



## Aop在Spring中的作用

提供声明式事务；允许用户自定义切面

以下名词需要了解下：

-   横切关注点：跨越应用程序多个模块的方法或功能。即是，与我们业务逻辑无关的，但是我们需要关注的部分，就是横切关注点。如日志 , 安全 , 缓存 , 事务等等 ....
-   切面（ASPECT）：横切关注点 被模块化 的特殊对象。即，它是一个类。
-   通知（Advice）：切面必须要完成的工作。即，它是类中的一个方法。
-   目标（Target）：被通知对象。
-   代理（Proxy）：向目标对象应用通知之后创建的对象。
-   切入点（PointCut）：切面通知 执行的 “地点”的定义。
-   连接点（JointPoint）：与切入点匹配的执行点。



![图片](https://img-blog.csdnimg.cn/img_convert/dcba8c55dfea1996c74bea917bb8f578.png)

SpringAOP中，通过Advice定义横切逻辑，Spring中支持5种类型的Advice:



![图片](https://img-blog.csdnimg.cn/img_convert/0989fa26216d514848cc6b9b85e2bdaf.png)

==**说明**：Advice就是调用方法的位置，如果是前置通知，那么代理角色添加的方法或者输出就是在真实角色的前面，例如我在实现了 前置通知的类，并在里面添加一个add()方法，调用这个方法，那么最终的调用顺序是add()方法先执行的，无非就是调用方法的位置不同而已，其他的通知也是类似的==







## 使用Spring实现Aop

【重点】使用AOP注入，需要导入一个依赖包！

```xml
<!-- https://mvnrepository.com/artifact/org.aspectj/aspectjweaver -->
<dependency>
   <groupId>org.aspectj</groupId>
   <artifactId>aspectjweaver</artifactId>
   <version>1.9.4</version>
</dependency>
```



### **第一种方式**

**通过 Spring API 实现**



首先编写我们的业务接口和实现类

```java
public interface UserService {

   public void add();

   public void delete();

   public void update();

   public void search();

}
public class UserServiceImpl implements UserService{
   @Override
   public void add() {
       System.out.println("增加用户");
  }
   @Override
   public void delete() {
       System.out.println("删除用户");
  }
   @Override
   public void update() {
       System.out.println("更新用户");
  }
   @Override
   public void search() {
       System.out.println("查询用户");
  }
}
```

然后去写我们的增强类 , 我们编写两个 , 一个前置增强 一个后置增强

```java
public class Log implements MethodBeforeAdvice {

   //method : 要执行的目标对象的方法
   //objects : 被调用的方法的参数
   //Object : 目标对象
   @Override
   public void before(Method method, Object[] objects, Object o) throws Throwable {
       System.out.println( o.getClass().getName() + "的" + method.getName() + "方法被执行了");
  }
}
public class AfterLog implements AfterReturningAdvice {
   //returnValue 返回值
   //method被调用的方法
   //args 被调用的方法的对象的参数
   //target 被调用的目标对象
   @Override
   public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
       System.out.println("执行了" + target.getClass().getName()
       +"的"+method.getName()+"方法,"
       +"返回值："+returnValue);
  }
}
```

最后去spring的文件中注册 , 并实现aop切入实现 , 注意导入约束 .

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/aop
       http://www.springframework.org/schema/aop/spring-aop.xsd">

    <!--注册bean-->
    <bean class="com.xiaozhi.AOP.UserServiceImpl" id="userService"/>
    <bean class="com.xiaozhi.AOP.Log" id="log"/>
    <bean class="com.xiaozhi.AOP.afterLog" id="afterLog"/>
    <!--aop的配置-->
    <aop:config>
        <!--
            pointcut标签表示从那里进行切面操作，可以理解为是个标记点
                execution就是限定可以对那么接口或者方法进行代理
                id：是pointcut的唯一表示
                execution：表达式
            advisor标签往标记了的地方注入要添加的代码
                advice-ref：要添加的代码
                poingcut-ref：往那个标记点切入
        -->
        <aop:pointcut id="pointcut" expression="execution(* com.xiaozhi.AOP.UserService.*(..))"/>
        <aop:advisor advice-ref="afterLog" pointcut-ref="pointcut"/>
        <aop:advisor advice-ref="log" pointcut-ref="pointcut"/>
    </aop:config>
</beans>
```

测试

```java
@Test
public void Test1(){
    ApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
    UserService userService = context.getBean("userService", UserService.class);
    userService.add();
}
```

Aop的重要性 : 很重要 . 一定要理解其中的思路 , 主要是思想的理解这一块 .

Spring的Aop就是将公共的业务 (日志 , 安全等) 和领域业务结合起来 , 当执行领域业务时 , 将会把公共业务加进来 . 实现公共业务的重复利用 . 领域业务更纯粹 , 程序猿专注领域业务 , 其本质还是动态代理 . 



### **第二种方式**

**自定义类来实现Aop**

目标业务类不变依旧是userServiceImpl

第一步 : 写我们自己的一个切入类

```java
public class DiyPointcut {
   public void before(){
       System.out.println("---------方法执行前---------");
  }
   public void after(){
       System.out.println("---------方法执行后---------");
  }
}
```

去spring中配置

```xml
<!--第二种方式自定义实现-->
<!--注册bean-->
    <bean class="com.xiaozhi.AOP2.UserServiceImpl" id="userService"/>
    <bean class="com.xiaozhi.AOP2.DiyPointcut" id="diyPointcut"/>
    <!--aop的配置-->
    <aop:config>
        <!--第二种方式：使用AOP的标签实现-->
        <!--ref属性引用类-->
        <aop:aspect ref="diyPointcut">
            <aop:pointcut id="pointcut" expression="execution(* com.xiaozhi.AOP2.UserServiceImpl.*(..))"/>
            <aop:before method="before" pointcut-ref="pointcut"/>
            <aop:after method="after" pointcut-ref="pointcut"/>
        </aop:aspect>
    </aop:config>
```

测试：

```java
@Test
public void Test2(){
    ApplicationContext context = new ClassPathXmlApplicationContext("beans2.xml");
    UserService userService = context.getBean("userService", UserService.class);
    userService.add();
}
```



### **第三种方式**

**使用注解实现**

第一步：编写一个注解实现的增强类

==导包的时候注入是org.aspectj.lang.annotation包下的==

```java
@Aspect //标记为切面
public class annotationPointcut {
    @Before("execution(* com.xiaozhi.AOP3.UserServiceImpl.*(..))")
    public void before(){
        System.out.println("---------方法执行前---------");
    }
    @After("execution(* com.xiaozhi.AOP3.UserServiceImpl.*(..))")
    public void after(){
        System.out.println("---------方法执行后---------");
    }
    // 环绕
    @Around("execution(* com.xiaozhi.AOP3.UserServiceImpl.*(..))")
    public void around(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("环绕前");
        // 执行目标方法proceed
        Object proceed = joinPoint.proceed();
        // 输出目标对象是哪个
        System.out.println(joinPoint.getTarget());
        // 输出签名，签名就是执行了那个方法
        System.out.println(joinPoint.getSignature());
        System.out.println("环绕后");
    }
}
```

第二步：在Spring配置文件中，注册bean，并增加支持注解的配置

```xml
<bean class="com.xiaozhi.AOP3.annotationPointcut" id="annotationPointcut"/>
<bean class="com.xiaozhi.AOP3.UserServiceImpl" id="userService"/>
<!--导入注解支持-->
<aop:aspectj-autoproxy/>
```

aop:aspectj-autoproxy：说明

```xml
通过aop命名空间的<aop:aspectj-autoproxy />声明自动为spring容器中那些配置@aspectJ切面的bean创建代理，织入切面。当然，spring 在内部依旧采用AnnotationAwareAspectJAutoProxyCreator进行自动代理的创建工作，但具体实现的细节已经被<aop:aspectj-autoproxy />隐藏起来了

<aop:aspectj-autoproxy />有一个proxy-target-class属性，默认为false，表示使用jdk动态代理织入增强，当配为<aop:aspectj-autoproxy  poxy-target-class="true"/>时，表示使用CGLib动态代理技术织入增强。不过即使proxy-target-class设置为false，如果目标类没有声明接口，则spring将自动使用CGLib动态代理。
```





 

# 集成MyBatis

## 1	步骤

1、导入相关jar包

junit

```xml
<dependency>
   <groupId>junit</groupId>
   <artifactId>junit</artifactId>
   <version>4.12</version>
</dependency>
```

mybatis

```xml
<dependency>
   <groupId>org.mybatis</groupId>
   <artifactId>mybatis</artifactId>
   <version>3.5.2</version>
</dependency>
```

mysql-connector-java

```xml
<dependency>
   <groupId>mysql</groupId>
   <artifactId>mysql-connector-java</artifactId>
   <version>5.1.47</version>
</dependency>
```

spring相关

```xml
<dependency>
   <groupId>org.springframework</groupId>
   <artifactId>spring-webmvc</artifactId>
   <version>5.1.10.RELEASE</version>
</dependency>
<dependency>
   <groupId>org.springframework</groupId>
   <artifactId>spring-jdbc</artifactId>
   <version>5.1.10.RELEASE</version>
</dependency>
```

aspectJ AOP 织入器

```xml
<!-- https://mvnrepository.com/artifact/org.aspectj/aspectjweaver -->
<dependency>
   <groupId>org.aspectj</groupId>
   <artifactId>aspectjweaver</artifactId>
   <version>1.9.4</version>
</dependency>
```

mybatis-spring整合包 【重点】

```xml
<dependency>
   <groupId>org.mybatis</groupId>
   <artifactId>mybatis-spring</artifactId>
   <version>2.0.2</version>
</dependency>
```

配置Maven静态资源过滤问题！

```xml
<build>
   <resources>
       <resource>
           <directory>src/main/java</directory>
           <includes>
               <include>**/*.properties</include>
               <include>**/*.xml</include>
           </includes>
           <filtering>true</filtering>
       </resource>
   </resources>
</build>
```

2、编写配置文件

3、代码实现







## 2	MyBatis-Spring学习

引入Spring之前需要了解mybatis-spring包中的一些重要类；

http://mybatis.org/spring/zh/index.html

![image-20220316133400249](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/image-20220316133400249.png)



**什么是 MyBatis-Spring？**

MyBatis-Spring 会帮助你将 MyBatis 代码无缝地整合到 Spring 中。

**知识基础**

在开始使用 MyBatis-Spring 之前，你需要先熟悉 Spring 和 MyBatis 这两个框架和有关它们的术语。这很重要

MyBatis-Spring 需要以下版本：

| MyBatis-Spring | MyBatis | Spring 框架 | Spring Batch | Java    |
| :------------- | :------ | :---------- | :----------- | :------ |
| 2.0            | 3.5+    | 5.0+        | 4.0+         | Java 8+ |
| 1.3            | 3.4+    | 3.2.2+      | 2.1+         | Java 6+ |

如果使用 Maven 作为构建工具，仅需要在 pom.xml 中加入以下代码即可：

```xml
<dependency>
   <groupId>org.mybatis</groupId>
   <artifactId>mybatis-spring</artifactId>
   <version>2.0.2</version>
</dependency>
```

要和 Spring 一起使用 MyBatis，需要在 Spring 应用上下文中定义至少两样东西：一个 SqlSessionFactory 和至少一个数据映射器类。

在 MyBatis-Spring 中，可使用SqlSessionFactoryBean来创建 SqlSessionFactory。要配置这个工厂 bean，只需要把下面代码放在 Spring 的 XML 配置文件中：

```xml
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
 <property name="dataSource" ref="dataSource" />
</bean>
```

注意：SqlSessionFactory需要一个 DataSource（数据源）。这可以是任意的 DataSource，只需要和配置其它 Spring 数据库连接一样配置它就可以了。

在基础的 MyBatis 用法中，是通过 ==**SqlSessionFactoryBuilder**== 来创建 SqlSessionFactory 的。而在 MyBatis-Spring 中，则使用 ==**SqlSessionFactoryBean** ==来创建。

在 MyBatis 中，你可以使用 SqlSessionFactory 来创建 SqlSession。一旦你获得一个 session 之后，你可以使用它来执行映射了的语句，提交或回滚连接，最后，当不再需要它的时候，你可以关闭 session。

SqlSessionFactory有一个唯一的必要属性：用于 JDBC 的 DataSource。这可以是任意的 DataSource 对象，它的配置方法和其它 Spring 数据库连接是一样的。

==**一个常用的属性是 configLocation，它用来指定 MyBatis 的 XML 配置文件路径**。==它在需要修改 MyBatis 的基础配置非常有用。通常，基础配置指的是 < settings> 或 < typeAliases>元素。

需要注意的是，这个配置文件并不需要是一个完整的 MyBatis 配置。确切地说，任何环境配置（<environments>），数据源（<DataSource>）和 MyBatis 的事务管理器（<transactionManager>）都会被忽略。SqlSessionFactoryBean 会创建它自有的 MyBatis 环境配置（Environment），并按要求设置自定义环境的值。

==**SqlSessionTemplate 是 MyBatis-Spring 的核心**==。作为 SqlSession 的一个实现，这意味着可以使用它无缝代替你代码中已经在使用的 SqlSession。

模板可以参与到 Spring 的事务管理中，并且由于其是线程安全的，可以供多个映射器类使用，你应该总是用 SqlSessionTemplate 来替换 MyBatis 默认的 DefaultSqlSession 实现。在同一应用程序中的不同类之间混杂使用可能会引起数据一致性的问题。

可以使用 SqlSessionFactory 作为构造方法的参数来创建 SqlSessionTemplate 对象。

```xml
<bean id="sqlSession" class="org.mybatis.spring.SqlSessionTemplate">
 <constructor-arg index="0" ref="sqlSessionFactory" />
</bean>
```

现在，这个 bean 就可以直接注入到你的 DAO bean 中了。你需要在你的 bean 中添加一个 SqlSession 属性，就像下面这样：

```java
public class UserDaoImpl implements UserDao {

 private SqlSession sqlSession;

 public void setSqlSession(SqlSession sqlSession) {
   this.sqlSession = sqlSession;
}

 public User getUser(String userId) {
   return sqlSession.getMapper...;
}
}
```

按下面这样，注入 SqlSessionTemplate：

```xml
<bean id="userDao" class="org.mybatis.spring.sample.dao.UserDaoImpl">
 <property name="sqlSession" ref="sqlSession" />
</bean>
```



### 整合实现一

1、引入Spring配置文件beans.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
        https://www.springframework.org/schema/beans/spring-beans.xsd">

</beans>
```

2、配置数据源替换mybaits的数据源

```xml
<!--配置数据源：数据源有非常多，可以使用第三方的，也可使使用Spring的-->
<bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource">
   <property name="driverClassName" value="com.mysql.jdbc.Driver"/>
   <property name="url" value="jdbc:mysql://localhost:3306/mybatis?useSSL=true&amp;useUnicode=true&amp;characterEncoding=utf8"/>
   <property name="username" value="root"/>
   <property name="password" value="root"/>
</bean>
```

3、配置SqlSessionFactory，关联MyBatis

```xml
<!--配置SqlSessionFactory-->
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
   <property name="dataSource" ref="dataSource"/>
   <!--关联Mybatis-->
   <property name="configLocation" value="classpath:mybatis-config.xml"/>
   <property name="mapperLocations" value="classpath:com/xiaozhi/mapper/*.xml"/>
</bean>
```

4、注册sqlSessionTemplate，关联sqlSessionFactory；

```xml
<!--注册sqlSessionTemplate , 关联sqlSessionFactory-->
<bean id="sqlSession" class="org.mybatis.spring.SqlSessionTemplate">
   <!--利用构造器注入,因为它没有set方法-->
   <constructor-arg index="0" ref="sqlSessionFactory"/>
</bean>
```

5、增加Dao接口的实现类；私有化sqlSessionTemplate

```java
public class UserDaoImpl implements UserMapper {
    //sqlSession不用我们自己创建了，Spring来管理
    private SqlSessionTemplate sqlSession;
    public void setSqlSession(SqlSessionTemplate sqlSession) {
        this.sqlSession = sqlSession;
    }
    public List<User> selectUser() {
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        return mapper.selectUser();
    }
}
```

6、注册bean实现

```xml
<bean id="userDao" class="com.xiaozhi.mapper.UserDaoImpl">
     <!--对属性进行映射-->
   <property name="sqlSession" ref="sqlSession"/>
</bean>
```

7、测试

```java
@Test
public void test1(){
    ApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
    UserMapper mapper = (UserMapper) context.getBean("userDao");
    List<User> user = mapper.selectUser();
    System.out.println(user);
}
```

结果成功输出！现在我们的Mybatis配置文件的状态！发现都可以被Spring整合！

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
       PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
       "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
   <typeAliases>
       <package name="com.xiaozhi.pojo"/>
   </typeAliases>
</configuration>
```



最终的配置文件spring-mybatis.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!--配置数据源，可以使用第三方的，这里使用spring的-->
    <bean id="datasource" class="org.springframework.jdbc.datasource.DriverManagerDataSource">
        <property name="driverClassName" value="com.mysql.jdbc.Driver"/>
        <property name="url" value="jdbc:mysql://localhost:3306/mybatis?useSSL=true&amp;useUnicode=true&amp;characterEncoding=utf8"/>
        <property name="username" value="root"/>
        <property name="password" value="root"/>
    </bean>

    <!--配置SqlSessionFactory关联mybatis-->
    <bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
        <property name="dataSource" ref="datasource"/>
        <!--引用mybaits配置文件-->
        <property name="configLocation" value="classpath:mybatis-config.xml"/>
        <!--引用mapper配置文件-->
        <property name="mapperLocations" value="classpath:com/xiaozhi/mapper/*.xml"/>
    </bean>

    <!--注册SqlSessionTemplate,关联sqlSessionFactory-->
    <bean id="sqlSession" class="org.mybatis.spring.SqlSessionTemplate">
        <!--利用构造器注入，因为没有set方法-->
        <constructor-arg index="0" ref="sqlSessionFactory"/>
    </bean>

</beans>
```

==**说明**：我们可以修改其中的配置信息就可以使用了，然后将这个配置文件引入到核心的配置文件中即可==









###  整合实现二

mybatis-spring1.2.3版以上的才有这个 .

官方文档截图 :

dao继承Support类 , 直接利用 getSqlSession() 获得 , 然后直接注入SqlSessionFactory . 比起方式1 , 不需要管理SqlSessionTemplate , 而且对事务的支持更加友好 . 可跟踪源码查看

![image-20220316140504317](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/image-20220316140504317.png)


测试：

1、将我们上面写的UserDaoImpl修改一下

```java
public class UserMapperImpl2 extends SqlSessionDaoSupport implements UserMapper {
    @Override
    public List<User> selectUser() {
        return getSqlSession().getMapper(UserMapper.class).selectUser();
    }
}
```

2、修改bean的配置

```xml
<bean class="com.xiaozhi.mapper.UserMapperImpl2" id="userMapper2">
    <property name="sqlSessionFactory" ref="sqlSessionFactory"/>
</bean>
```

3、测试

```java
@Test
public void Test2(){
    ApplicationContext context = new ClassPathXmlApplicationContext("applicationConfig.xml");
    UserMapper userMapper2 = context.getBean("userMapper2", UserMapper.class);
    for (User user : userMapper2.selectUser()) {
        System.out.println(user);
    }
}
```

**总结 : 整合到spring以后可以完全不要mybatis的配置文件，除了这些方式可以实现整合之外，我们还可以使用注解来实现，这个等我们后面学习SpringBoot的时候还会测试整合！**







# 声明式事务

## 1	回顾事务

-   事务在项目开发过程非常重要，涉及到数据的一致性的问题，不容马虎！
-   事务管理是企业级应用程序开发中必备技术，用来确保数据的完整性和一致性。

事务就是把一系列的动作当成一个独立的工作单元，这些动作要么全部完成，要么全部不起作用。

**事务四个属性ACID**

-   原子性（atomicity）
    -   事务是原子性操作，由一系列动作组成，事务的原子性确保动作要么全部完成，要么完全不起作用

-   一致性（consistency）
    -   一旦所有事务动作完成，事务就要被提交。数据和资源处于一种满足业务规则的一致性状态中

-   隔离性（isolation）
    -   可能多个事务会同时处理相同的数据，因此每个事务都应该与其他事务隔离开来，防止数据损坏

-   持久性（durability）
    -   事务一旦完成，无论系统发生什么错误，结果都不会受到影响。通常情况下，事务的结果被写到持久化存储器中





## 2测试

1、创建UserMapper接口和它的实现类

```java
public interface UserMapper {
    // 添加一个用户
    public int addUser(User user);
    // 删除一个用户
    public int deleteUser(int id);
}
```

```java
public class UserMapperImpl implements UserMapper {
    private SqlSessionTemplate sqlSession;
    public void setSqlSession(SqlSessionTemplate sqlSession) {
        this.sqlSession = sqlSession;
    }
    @Override
    public int addUser(User user) {
        return sqlSession.getMapper(UserMapper.class).addUser(user);
    }
    @Override
    public int deleteUser(int id) {
        return sqlSession.getMapper(UserMapper.class).deleteUser(id);
    }
}
```

2、编写对应的xml配置文件

**说明：我们让delete语句发生错误**

```xml
<mapper namespace="com.xiaozhi.mapper.UserMapper">
    <insert id="addUser" parameterType="user">
        insert into user(id,username,password) values (#{id},#{username},#{password})
    </insert>

    <delete id="deleteUser" parameterType="int">
        deletes from user where id = #{id}
    </delete>
</mapper>
```

3、注册bean

```xml
<bean class="com.xiaozhi.mapper.UserMapperImpl" id="userMapper">
    <property name="sqlSession" ref="sqlSession"/>
</bean>
```

结果是插入成功了，删除报错，我们需要的是两个业务要么同时成功，要么同时失败，显然他们没有进行事物的管理，所以我们要加入事物，以前我们都需要自己手动管理事务，十分麻烦！

但是Spring给我们提供了事务管理，我们只需要配置即可；



## Spring中的事务管理

Spring在不同的事务管理API之上定义了一个抽象层，使得开发人员不必了解底层的事务管理API就可以使用Spring的事务管理机制。Spring支持编程式事务管理和声明式的事务管理。

**编程式事务管理**	-	就是在try-catch-finally进行提交事物和回滚的操作

-   将事务管理代码嵌到业务方法中来控制事务的提交和回滚
-   缺点：必须在每个事务操作业务逻辑中包含额外的事务管理代码

**声明式事务管理**

-   一般情况下比编程式事务好用。
-   将事务管理代码从业务方法中分离出来，以声明的方式来实现事务管理。
-   将事务管理作为横切关注点，通过aop方法模块化。Spring中通过Spring AOP框架支持声明式事务管理。

**使用Spring管理事务，注意头文件的约束导入 : tx**

```xml
xmlns:tx="http://www.springframework.org/schema/tx"

http://www.springframework.org/schema/tx
http://www.springframework.org/schema/tx/spring-tx.xsd">
```

**事务管理器**

-   无论使用Spring的哪种事务管理策略（编程式或者声明式）事务管理器都是必须的。
-   就是 Spring的核心事务管理抽象，管理封装了一组独立于技术的方法。

**JDBC事务**

```xml
<bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
       <property name="dataSource" ref="dataSource" />
</bean>
```

**配置好事务管理器后我们需要去配置事务的通知**

```xml
<!--配置事务通知-->
<tx:advice id="txAdvice" transaction-manager="transactionManager">
   <tx:attributes>
       <!--配置哪些方法使用什么样的事务,配置事务的传播特性-->
        <tx:method name="select*" propagation="REQUIRED"/>
   </tx:attributes>
</tx:advice>
```

==**说明**：name属性的用法是对应方法名的，如果方法名不对是无法添加事物的，如果只是写上add方法，那么只有名字是add的方法才会有效，其他的方法不会有效，比如说addUser方法也不会有效，名字一定要匹配，还有一种用法是在名字后面加 * 号，这个表示后面的字符可以随意。一定要注意这个问题！！！==



**spring事务传播特性：**

事务传播行为就是多个事务方法相互调用时，事务如何在这些方法间传播。spring支持7种事务传播行为：

-   propagation_requierd：如果当前没有事务，就新建一个事务，如果已存在一个事务中，加入到这个事务中，这是最常见的选择。
-   propagation_supports：支持当前事务，如果没有当前事务，就以非事务方法执行。
-   propagation_mandatory：使用当前事务，如果没有当前事务，就抛出异常。
-   propagation_required_new：新建事务，如果当前存在事务，把当前事务挂起。
-   propagation_not_supported：以非事务方式执行操作，如果当前存在事务，就把当前事务挂起。
-   propagation_never：以非事务方式执行操作，如果当前事务存在则抛出异常。
-   propagation_nested：如果当前存在事务，则在嵌套事务内执行。如果当前没有事务，则执行与propagation_required类似的操作

Spring 默认的事务传播行为是 PROPAGATION_REQUIRED，它适合于绝大多数的情况。

假设 ServiveX#methodX() 都工作在事务环境下（即都被 Spring 事务增强了），假设程序中存在如下的调用链：Service1#method1()->Service2#method2()->Service3#method3()，那么这 3 个服务类的 3 个方法通过 Spring 的事务传播机制都工作在同一个事务中。

就好比，我们刚才的几个方法存在调用，所以会被放在一组事务当中！

**配置AOP**

导入aop的头文件！

```xaml
<!--配置aop织入事务-->
<aop:config>
   <aop:pointcut id="txPointcut" expression="execution(* com.kuang.dao.*.*(..))"/>
   <aop:advisor advice-ref="txAdvice" pointcut-ref="txPointcut"/>
</aop:config>
```

**进行测试**

删掉刚才插入的数据，再次测试！

```java
@Test
public void Test1(){
    // 两个功能要不一起成功，要不就一起失败
    ApplicationContext context = new ClassPathXmlApplicationContext("applicationConfig.xml");
    UserMapper userMapper = context.getBean("userMapper", UserMapper.class);
    // 同时成功，同时失败
    for (User user : userMapper.selectUser()) {
        System.out.println(user);
    }
}
```



### 思考问题？

为什么需要配置事务？

-   如果不配置，就需要我们手动提交控制事务；
-   事务在项目开发过程非常重要，涉及到数据的一致性的问题，不容马虎！





参考原文https://mp.weixin.qq.com/s/kvp_3Uva1J2Q5ZVqCUzEsA



## 



# Spring Expression Language (SpEL)

## bean调用方法

```
"@test."
```





# Spring监听器

Spring监听器它能监听给定的事件，它是观察者模式的实现





