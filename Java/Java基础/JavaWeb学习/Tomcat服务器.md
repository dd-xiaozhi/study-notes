# Tomcat服务器

## Web资源分类

静态资源：HTML、CSS、JavaScript、txt、MP4、jpg图片....等等

动态资源：JSP页面、servlet程序....等等





## Tomcat的概述和安装

![image-20201128204732075](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201128204732075.png)



**常用的Web服务器**

Tomcat：由 Apache 组织提供的一种 Web 服务器，提供对 jsp 和 Servlet 的支持。它是一种轻量级的 javaWeb 容器（服务 器），也是当前应用最广的 JavaWeb 服务器（免费）。 

Jboss：是一个遵从 JavaEE 规范的、开放源代码的、纯 Java 的 EJB 服务器，它支持所有的 JavaEE 规范（免费）。

 GlassFish： 由 Oracle 公司开发的一款 JavaWeb 服务器，是一款强健的商业服务器，达到产品级质量（应用很少）。 Resin：是 CAUCHO 公司的产品，是一个非常流行的服务器，对 servlet 和 JSP 提供了良好的支持， 性能也比较优良，resin 自身采用 JAVA 语言开发（收费，应用比较多）。 

WebLogic：是 Oracle 公司的产品，是目前应用最广泛的 Web 服务器，支持 JavaEE 规范， 而且不断的完善以适应新的开发要求，适合大型项目（收费，用的不多，适合大公司）。



**Tomcat服务器和Servlet版本的对应关系**

当前企业常用的版本 7.*、8.* 

![image-20201128204933955](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201128204933955.png)

Servlet 程序从 2.5 版本是现在世面使用最多的版本（xml 配置） 到了 Servlet3.0 之后。就是注解版本的 Servlet 使用。 以 2.5 版本为主线讲解 Servlet 程序。



**Tomcat的安装**

直接解压就是安装了



**目录的介绍**

- bin目录：专门存放Tomcat服务器的可执行文件

- conf目录：存放配置文件

- lib目录：存放jar包，一些JavaEE规范的实现类

- logs：存放运行时输出的日志

- temp：运行时产生的临时数据

- webapps：专门存放部署的web工程，每一个文件夹就是一个工程

- work：是Tomcat服务器运行时JSP翻译servlet的源码，和Session钝化的目录（钝化就是序列化）

  

## Tomcat的启动

**一、第一种方式启动**

​	找到Tomcat目录下的bin目录下的startup.bat文件，双击运行

​		如何知道是否运行成功？

​			①在浏览器中输入http://localhost:8080，如果有Tomcat的主页出来那就是成功了

​			②http://127.0.0.1:8080

​			③http://IP:8080



**二、第二种方式启动**

1. 打开命令行
2. cd到Tomcat的bin目录下
3. 输入启动命令：catalina run



**错误**

常见的错误：用第一种方式的时候有一个小黑框一闪而过，服务器启动失败，失败的原因是因为环境变量没有配好

​					 用命令行的方式它是会返回错误信息给你的，这时候你就知道哪里有问题了

配置环境变量要注意的问题：①要有变量名JAVA_HOME 都是大写的，中间用下划线隔开

​											   ②路径是到jdk的安装目录下就可以了，不用进入到bin目录





## Tomcat的停止

第一种方式：点击服务器关闭按钮

第二种方式：把Tomcat服务器窗口置为当前窗口，然后ctrl + c 关闭

第三种方式（主要的）：找到Tomcat的bin目录下的Stutdown.bat，双击关闭



## 修改Tomcat的端口号

Tomcat默认的端口号是：8080

​	找到Tomcat目录下的conf文件夹里面的Server.xml文件，把文件里面的port参数修改成你想要的端口号

​	端口号的范围：1-65535	修改完成之后一定要重启Tomcat



## 部署Web工程的方式

**第一种方法：**

只需要把Web工程的目录拷贝到Tomcat下的Webapps目录下就可以了

​		访问工程：浏览器中输入http://IP:端口号/工程名



**第二种方法：**

找到Tomcat下的conf目录下的catalina目录下的localhost创建一个xml文件

​		修改一下里面的内容就可以了，记得是utf-8 的编码，不然会报错的

```xml
<!-- 
Context 表示一个工程上下文
path 表示工程的访问路径:/abc
docBase 表示你的工程目录在哪里
-->
<Context path="/abc" docBase="E:\book" />
```

访问这个工程的路径如下:http://ip:port/abc/ 就表示访问 E:\book 目录





手动打开的html网页和部署在Tomcat服务器上的html网页的区别

**手动：**它的协议是file协议，读取本地的资源加载到浏览器上



**部署在服务器上的：**客户端向服务端发送请求，服务端根据客户端的请求返回相应的数据（Tomcat解析的页面）

​		

## Root工程的访问

​	没有工程名的时候默认访问的是Root工程

​	当有工程名，没有资源名的时候就会默认访问index.html 或者 index.jsp(没有index.html情况)



## IDEA整合Tomcat

**添加**

​	setting	->	File | Settings | Build, Execution, Deployment	->	Application Servers

​	点击  +  添加，找到安装好的Tomcat的文件		可以添加多个



**IDEA中创建动态工程**

新建一个模块，选择Java Enterprise

![image-20201128213628951](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201128213628951.png)

​											

目录介绍

- lib目录：存放第三方的jar包

- src目录：存放自己写的java源代码

- web目录专门用来存放web工程的资源文件。比如：HTML页面，CSS文件，JS文件.....等等。

- WEB-INF目录：是一个受浏览器保护的目录，浏览器无法直接访问到此目录的内容

- web.xml：是整个动态web工程的配置描述文件，可以配置很多的web工程组件

  ​	比如：Svelet程序、Filter过滤器、Listener监听器、Session超时....等等。

添加jar包的第二种方式：

​	主菜单	->	ProjectStructure	->	Libraries	然后点击加号添加

​	![image-20201128221018341](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201128221018341.png)

![image-20201128222158771](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201128222158771.png)

![image-20201128223356352](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201128223356352.png)

![image-20201128222513352](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201128222513352.png)



**要注意的异常**

java.lang.UnsatisfiedLinkError: apache-tomcat-7.0.37\bin\tcnative-1.dll:Can load AMD 64

JDK的版本和Tomcat服务器的版本不一致，JDK可能是X64的，Tomcat可能是32的才会出现这个异常













