# 一、基础知识

## 1、分布式基础理论

### 1.1 什么是分布式系统

分布式系统就是由多个计算机提供服务组成的系统

随着互联网的发展，网站应用的规模不断扩大，常规的`垂直应用架构`已经无法应付，`分布式架构`与`流动计算架构`势在必行，还需一个`治理系统`来对应用进行管理



### 1.2 架构演变

**单一应用架构**：在早期，所有的应用和服务都是部署在一台服务器上，当一台服务器的性能不能够处理更多流量时，可以通过横向扩展来分摊单个服务器的压力

![image-20220815103317660](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220815103317660.png)

**缺点**：

-   修改应用逻辑就需要重新部署应用，如果有多台服务器的话就需要多次部署
-   当应用功能越来越多时就会不易扩展
-   多个程序员进行开发时不容易解耦



**垂直应用架构**：将一个应用的多个功能拆分成多个应用进行独立部署，哪一个功能访问多的就增加多几台服务器，修改某一个功能的时候就只需要重新部署已修改功能的服务器，而不需要重新部署没有修改的服务器

![image-20220815103328816](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220815103328816.png)

**缺点**：

-   修改前端页面就需要将整个服务器进行重新部署，所以需要界面和业务逻辑实现分离，也就是我们说的前后端分离
-   应用之间会相互调用，不会完全独立



**分布式服务架构**：将单独的应用拆分成多个独立的服务系统，服务系统之间进行交互的分布式服务框架（RPC）是关键

![image-20220815103942091](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220815103942091.png)



**流动式计算架构**：

当服务越来越多，流量浪费的情况就会出现，服务器分配不均匀的情况就会发生，所以我们需要一个调度中心基于访问压力实时管理集群容量，少人访问的就缩减它的服务器数量，大流量的就增加数量，以此来减少资源的浪费，所以，于提高机器利用率的`资源调度`和`治理中心(SOA)[ Service Oriented Architecture]`是关键

![image-20220815104403025](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220815104403025.png)



### 1.3 什么是RPC

RPC全称是Remote Procedure Call，指远程过程调用，是一种进程间的通信方式，它是一种技术的思想，而不是规范，它允许不同服务器之间进行通信，A服务器调用B服务器的函数，本质就是A向B服务器发送一个请求，但是这个细节不用我们程序员去处理，而是由RPC框架来进行管理

<span style="color: red;">RPC基本原理</span>

![image-20220815104728551](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220815104728551.png)

![image-20220815104804990](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220815104804990.png)

RPC两个核心模块：通讯，序列化。

RPC速度的提升就在于如何快速建立连接和序列化的速度





## 2、Dubbo核心概念

### 2.1 简介

Apache Dubbo (incubating) |ˈdʌbəʊ| 是一款高性能、轻量级的开源Java RPC框架，它提供了三大核心能力：面向接口的远程方法调用，智能容错和负载均衡，以及服务自动注册和发现。

小分享：dubbo是alibaba贡献给Apache的

官网：http://dubbo.apache.org/



### 2.2 基本概念

![arch-service-discovery](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/architecture.png)

-   **服务提供者****（Provider）**：暴露服务的服务提供方，服务提供者在启动时，向注册中心注册自己提供的服务。**
-   **服务消费者（Consumer）**: 调用远程服务的服务消费方，服务消费者在启动时，向注册中心订阅自己所需的服务，服务消费者，从提供者地址列表中，基于软负载均衡算法，选一台提供者进行调用，如果调用失败，再选另一台调用。
-   **注册中心****（Registry）**：注册中心返回服务提供者地址列表给消费者，如果有变更，注册中心将基于长连接推送变更数据给消费者**
-   **监控中心（Monitor）**：服务消费者和提供者，在内存中累计调用次数和调用时间，定时每分钟发送一次统计数据到监控中心

Ø 调用关系说明

-   0.start：服务容器负责启动，加载，运行服务提供者。
-   1.register：服务提供者在启动时，向注册中心注册自己提供的服务。
-   2.subscribe：服务消费者在启动时，向注册中心订阅自己所需的服务。
-   3.notify：注册中心返回服务提供者地址列表给消费者，如果有变更，注册中心将基于长连接推送变更数据给消费者。
-   4.invoke：服务消费者，从提供者地址列表中，基于软负载均衡算法，选一台提供者进行调用，如果调用失败，再选另一台调用。
-   5.count：服务消费者和提供者，在内存中累计调用次数和调用时间，定时每分钟发送一次统计数据到监控中心。

Dubbo 架构具有以下几个特点，分别是连通性、健壮性、伸缩性、以及向未来架构的升级性。





## 3、Dubbo环境搭建

### windows下安装

#### 1、安装Zookeeper

1.  下载zookeeper

    网址 https://archive.apache.org/dist/zookeeper/zookeeper-3.4.13/ 

2.  解压zookeeper

3.  修改zoo.cfg配置文件

    -   将conf下的zoo_sample.cfg赋值一份改名为zoo.cfg
    -   修改zoo.cfg文件，将dataDir的目录修改为`../data`
    -   端口号配置项可以根据自己需求进行修改

4.  启动服务

    bin目录下zkServer.cmd

5.  使用zkCli.cmd测试

    ```sh
    # 列出zookeeper根下保存的所有节点
    ls /
    
    # 创建一个xiaozhi节点，值为123
    create -e /xiaozhi 123
    
    # 获取xiaozhi节点的值
    get /xiaozhi
    ```



#### 2、安装dubbo-admin

dubbo本身并不是一个服务软件。它其实就是一个jar包能够帮你的java程序连接到zookeeper，并利用zookeeper消费、提供服务。所以你不用在Linux上启动什么dubbo服务。

但是为了让用户更好的管理监控众多的dubbo服务，官方提供了一个可视化的监控程序，不过这个监控即使不装也不影响使用



1.  下载dubbo-admin

    https://github.com/apache/dubbo-admin

2.  进入目录，修改dubbo-admin配置

    修改 src\main\resources\application.properties 指定 zookeeper地址

3.  打包dubbo-admin

    ```sh
    mvn clean package -Dmaven.test.skip=true
    ```

4.  运行 dubbo-admin

    java -jar dubbo-admin-0.0.1-SNAPSHOT.jar

    默认账号密码是root



### linux下安装

#### 1、安装JDK

这里省略



#### 2、安装zookeeper

1.  下载zookeeper

    -   网址 https://archive.apache.org/dist/zookeeper/zookeeper-3.4.11/ 
    -   wget https://archive.apache.org/dist/zookeeper/zookeeper-3.4.11/zookeeper-3.4.11.tar.gz

2.  解压并修改名字为zookeeper

3.  开机自启动脚本

    ```sh
    #!/bin/bash
    #chkconfig:2345 20 90
    #description:zookeeper
    #processname:zookeeper
    ZK_PATH=/usr/local/zookeeper
    # 自定义java安装路径
    export JAVA_HOME=/usr/local/java/jdk1.8.0_171
    case $1 in
             start) sh  $ZK_PATH/bin/zkServer.sh start;;
             stop)  sh  $ZK_PATH/bin/zkServer.sh stop;;
             status) sh  $ZK_PATH/bin/zkServer.sh status;;
             restart) sh $ZK_PATH/bin/zkServer.sh restart;;
             *)  echo "require start|stop|status|restart"  ;;
    esac
    ```

4.  把脚本注册为Service

    ```sh
    # 添加
    chkconfig --add zookeeper
    # 查看是否已经添加
    chkconfig --list
    ```

5.  增加权限

    ```sh
    chmod +x /etc/init.d/zookeeper
    ```

6.  配置zookeeper

    -   将conf下的zoo_sample.cfg赋值一份改名为zoo.cfg
    -   修改zoo.cfg文件，将dataDir的目录修改为`../data`
    -   端口号配置项可以根据自己需求进行修改

7.  启动zookeeper

    ```sh
    service zookeeper start
    ```



#### 3、安装dubbo-admin

打成jar包直接运行即可





# 二、Quick Start

## 1、需求

在电商系统中，订单服务模块需要调用用户服务来获取用户的信息

| 模块                | 功能           |
| ------------------- | -------------- |
| 订单服务order模块   | 创建订单等     |
| 用户服务service模块 | 查询用户地址等 |





## 2、环境搭建

开启zookeeper和dubbo-admin



### 2.1 创建模块

创建三个maven模块

-   公共接口层：gmall-interface
-   用户服务模块：user-service-provider
-   订单服务模块：order-service-consumer

![image-20220819170227932](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220819170227932.png)

将需要使用的接口放到公共接口层，其余两个模块引用gmall-interface模块

![image-20220819170523676](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220819170523676.png)

```java
public class User implements Serializable {

    private String username;

    private Integer age;

    public User(String username, Integer age) {
        this.username = username;
        this.age = age;
    }

    public User() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", age=" + age +
                '}';
    }
}
```

```java
public interface OrderService {

    public void initOrder(String id);
}
```

```java
public interface UserService {

    public List<User> getUserList(String id);
}
```



### 2.2 引入依赖

`user-service-provider`和`order-service-consumer`都要引入下面依赖

```xml
<dependencies>
    <!-- 公共包 -->
    <dependency>
        <groupId>com.xiiaozhi</groupId>
        <artifactId>gmall-interface</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
    <!-- 引入dubbo -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>dubbo</artifactId>
        <version>2.6.2</version>
    </dependency>
    <!--
            由于我们使用zookeeper作为注册中心，所以需要操作zookeeper
            dubbo 2.6以前的版本引入zkclient操作zookeeper
            dubbo 2.6及以后的版本引入curator操作zookeeper
        -->
    <dependency>
        <groupId>org.apache.curator</groupId>
        <artifactId>curator-framework</artifactId>
        <version>2.12.0</version>
    </dependency>
</dependencies>
```



## 3、功能实现

### user-service-provider

1.  创建实现类 -> UserServiceImpl

    ```java
    @Service
    public class UserServiceImpl implements UserService {
    
        public List<User> getUserList(String id) {
            System.out.println("接收到的ID为：" + id);
            User u1 = new User("小智", 18);
            User u2 = new User("黑仔", 80);
            return Arrays.asList(u1, u2);
        }
    }
    ```

2.  创建spring容器的xml文件 -> `provider.xml`

3.  配置dubbo

    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
           xmlns:dubbo="http://dubbo.apache.org/schema/dubbo"
           xmlns:context="http://www.springframework.org/schema/context"
           xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd http://dubbo.apache.org/schema/dubbo http://dubbo.apache.org/schema/dubbo/dubbo.xsd http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">
    
        <context:component-scan base-package="com.xiaozhi.service.impl"/>
    
        <!-- 应用名 -->
        <dubbo:application name="order-service-consumer"/>
    
        <!-- 指定注册中心地址 -->
        <dubbo:registry protocol="zookeeper" address="127.0.01:2181"/>
    
        <!-- 指定使用的通信协议 -->
        <dubbo:protocol name="dubbo" port="20880"/>
    
        <!-- 指定暴露的服务 -->
        <dubbo:service interface="com.xiaozhi.service.UserService" ref="userServiceImpl"/>
    
    </beans>
    ```

4.  测试

    ```java
    public class ProviderApplication {
    
        public static void main(String[] args) throws IOException {
            ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:provider.xml");
            System.in.read();
        }
    }
    ```

5.  启动程序，查看dubbo上面是否有应用的信息

    

### order-service-consumer

1.  创建实现类

    ```java
    @Service
    public class OrderServiceImpl implements OrderService {
    
        @Autowired
        UserService userService;
    
        public void initOrder(String id) {
            List<User> userList = userService.getUserList(id);
            for (User user : userList) {
                System.out.println(user);
            }
        }
    }
    ```

2.  创建spring容器的xml文件 -> `consumer.xml`

3.  配置dubbo

    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:dubbo="http://code.alibabatech.com/schema/dubbo"
           xmlns:context="http://www.springframework.org/schema/context"
           xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd http://code.alibabatech.com/schema/dubbo http://code.alibabatech.com/schema/dubbo/dubbo.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">
    
        <context:component-scan base-package="com.xiaozhi.service.impl"/>
    
        <!-- 应用名 -->
        <dubbo:application name="order-service-consumer"/>
    
        <!-- 指定注册中心地址 -->
        <dubbo:registry protocol="zookeeper" address="127.0.01:2181"/>
        
        <!-- 生成远程代理，实现远程调用 -->
        <dubbo:reference interface="com.xiaozhi.service.UserService" id="userService"/>
    
    </beans>
    ```

4.  测试

    ```java
    public class ConsumerApplication {
    
        public static void main(String[] args) throws IOException {
            ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:consumer.xml");
            OrderService orderService = context.getBean(OrderService.class);
            orderService.initOrder("1");
            System.out.println("完成服务调用......");
            System.in.read();
        }
    }
    ```

5.  查看控制台输出

到此，简单入门完成......



## 4、监控中心

dubbo-monitor-simple



### 4.1 安装

1.  下载dubbo-ops

    https://github.com/apache/incubator-dubbo-ops 

2.  修改配置指定注册中心地址

    进入 dubbo-monitor-simple\src\main\resources\conf，修改 dubbo.properties文件

    ![image-20220819172629775](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220819172629775.png)

3.  打包dubbo-monitor-simple

    ```sh
    mvn clean package -Dmaven.test.skip=true
    ```

4.  解压 tar.gz 文件，并运行start.bat

5.  访问8080

    ![image-20220819173055124](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220819173055124.png)



### 4.2 监控中心配置

我们开启了监控中心还需要配置一下，提供者和消费者都要配置，dubbo官网可以找到有哪些配置

```xml
<!-- 配置监控中心 -->
<!-- 方式一：自己发现 -->
<dubbo:monitor protocol="registry"/>
<!-- 方式二：指定地址 -->
<dubbo:monitor address="127.0.0.1:7070"/>
```

配置完重新启动，可以看到监控中心发生了变化





## 5、springBoot整合

### 5.1 环境搭建

在Dubbo下创建两个springBoot工程

-   boot-user-service-provider
-   boot-order-service-consumer

引入依赖

```xml
<dependency>
    <groupId>com.alibaba.boot</groupId>
    <artifactId>dubbo-spring-boot-starter</artifactId>
    <version>0.2.0</version>
</dependency>
```



### 5.2 配置dubbo

-   提供者配置文件

    ```properties
    dubbo.application.name=boot-order-service-consumer
    dubbo.registry.address=zookeeper://127.0.0.1:2181
    
    dubbo.protocol.name=dubbo
    dubbo.protocol.port=20880
    
    # 配置监控中心 -> 去注册中心找
    dubbo.monitor.protocol=register
    ```

-   消费者配置文件

    ```properties
    server.port=8081
    
    # 配置应用名
    dubbo.application.name=boot-order-service-consumer
    # 配置注册中心地址
    dubbo.registry.address=zookeeper://127.0.0.1:2181
    # 配置监控中心 -> 去注册中心找
    dubbo.monitor.protocol=register
    ```



### 5.3 使用注解

```sh
# 开启dubbo，在主程序中标注
@EnableDubbo

# com.alibaba.dubbo.config.annotation.Service;	暴露服务，标注在需要暴露的服务类上
@service

# import com.alibaba.dubbo.config.annotation.Reference;    调用远程服务，使用此注解将bean注入
@Reference
```

注：`@service`提供者使用，`@Reference`消费者使用







# 三、Dubbo配置















































































