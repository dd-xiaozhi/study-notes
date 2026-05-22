# Servlet技术

## 什么是Servlet？

1. Servlet是Java规范之一，就是接口
2. Servlet是JavaWeb三大组件之一，三大组件分别是：Servlet程序、Fitter过滤器、Listener监听器
3. Servlet是运行在服务器上的一个小程序，它可以接受客户端的请求，并响应数据给客户端



![image-20201129093833623](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201129093833623.png)



## 实现Servlet程序：

1. 创建一个类实现Servlet接口
2. 实现service()方法，处理请求，并响应数据
3. 到web.xml中配置Servlet的访问地址



**代码实现：**

​	web.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
         version="4.0">
    
    <servlet>
        <servlet-name>HelloServlet</servlet-name>    <!--起别名-->
        <servlet-class>com.xiaozhi.java.ServletTest</servlet-class>     <!--全类名-->
    </servlet>

    <servlet-mapping>
        <servlet-name>HelloServlet</servlet-name>    <!--告诉服务器我配置的地址是给那个Servlet程序使用-->
        <!--
            /       表示地址为http://IP:port/工程路径            <br/>
            hello   表示地址为hhtp://IP:port/工程路径/hello      <br>
            通过这个地址可以访问到Servlet程序
        -->
        <url-pattern>/hello</url-pattern>
    </servlet-mapping>
</web-app>
```

**注意：**地址一定要以/打头，不然就会报错

**Java代码**

```java
public class ServletTest implements Servlet {
    /**
     * Serlet的构造器方法
     */
    public ServletTest(){
        System.out.println("1   Servlet程序的构造器方法");
    }
    /**
     * init初始化方法
     * @param servletConfig
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        System.out.println("2   init初始化方法");
    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }


    /**
     * service方法是用来处理请求和响应的
     * @param servletRequest
     * @param servletResponse
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
//        System.out.println("3   客户端访问了");
        // getMethod()方法只有servletRequest接口的子类，所以要进行类型转换
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

        // 下面代码进行了Servlet请求的分发处理
        String method = httpServletRequest.getMethod();
        if ("GET".equals(method)){
            // 为了方便后期的维护和简洁，将内容放到方法里面
            doGET();
        }else if ("POST".equals(method)){
            doPOST();
        }
    }
    private void doPOST() {
        System.out.println("POST请求");
        System.out.println("POST请求");
    }
    private void doGET() {
        System.out.println("GET请求");
        System.out.println("GET请求");
    }
    @Override
    public String getServletInfo() {
        return null;
    }
    /**
     * 服务器关闭的时候调用的方法
     */
    @Override
    public void destroy() {
        System.out.println("4   结束的时候执行");
    }
}
```



## 方法的生命周期

1. Servlet程序的构造器方法、init()方法在浏览器第一次访问的时候执行，往后都不执行
2. service()方法只要浏览器访问一次就执行一次
3. destroy()服务器关闭的时候执行

**注意：**Servlet程序资源如果没有被访问，那么是不会执行里面的方法的

![image-20201201205007944](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201201205007944.png)



## 实际开发的Servlet程序的编写

1. 继承Servlet接口的实现类 HttpServlet类
2. 根据业务需求重写需要的方法，在这里演示doPOST()和doGet()
3. 到web.xml文件中配置Servlet程序的访问地址

**代码：**

```java
public class ServletTest2 extends HttpServlet {
     //通过继承Servlet的 实现类HttpServlet 实现Servlet程序

    /**
     * doGet()在get请求的时候调用
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("get请求");
    }
    /**
     * doPost()在post请求的时候调用
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("post请求");
    }
}
```



**使用IDEA工具创建Servlet程序**

​	找到要放入类的包，右键new，找到Create New Servlet单击创建

![image-20201201210621195](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201201210621195.png)



**注意**：勾选Create JavaEE 6....选项和不勾选是两种不同的方案，方案不同处理的方式也不同









# 继承体系	-	各个类的功能



## ServletConfig类	-	配置信息

初始化使用，Servlet配置信息类

**三大作用**：

①获取Servlet-name的值		起别名的操作

​	init-param		初始化参数

​	param-name	参数名

​	param-value	参数值

②获取初始化init-param

③获取ServletContext对象

**方法**

1. getServletName()				获取Servlet-name的值

2. getInitparameter(参数名)     得到参数的值

   说明：就是param-value的值

3. getServletContext()             得到ServletContext对象

**代码**：

web.xml：

```xml
    <servlet>
        <servlet-name>HelloServlet</servlet-name>    <!--起别名-->
        <servlet-class>com.xiaozhi.java.ServletTest</servlet-class>     <!--全类名-->
        <!--init-param是初始参数-->
        <init-param>
            <!--参数名-->
            <param-name>username</param-name>
            <!--参数值-->
            <param-value>root</param-value>
        </init-param>
    </servlet>
```

java代码：

```java
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        // 获取Servlet-name的值，就是别名值
        System.out.println("Servlet-name的值：" + servletConfig.getServletName());
        // 获取初始化init-param参数下的param-value的值
        System.out.println("param-value的值为：" + servletConfig.getInitParameter("username"));
        // 获取ServletContext对象
        System.out.println("ServletContext对象为：" + servletConfig.getServletContext());
    }
```





**注意**：①ServletConfig对象是由Tomcat负责创建，我们负责使用

​		   ②Servlet程序创建时，就会创建对应的ServletConfig对象

​		   ③其他的Servlet程序可以获取其他的ServletConfig对象，但是获取不了里面的信息，也就是得到了个壳

​		   ④每个ServletConfig对象对应一个Servlet程序，可以有多个ServletConfig对象



## ServletContext类	-	页面信息

1. 是个接口，表示Servlet上下文对象

2. 一个web工程，只会有一个ServletContext对象实例

3. ServletContext对象是一个域对象

   域对象：和map一样		这里的域指的是存储数据的操作范围

   |        |     存数据     |     取数据     |     删除数据      |
   | :----: | :------------: | :------------: | :---------------: |
   |  map   |     put()      |     get()      |     remove()      |
   | 域对象 | setAttribute() | getAttribute() | removeAttribute() |

4. ServletContext在web工程部署启动的时候创建，在web工程停止的时候销毁对象，对象没了数据就没了



**补充**：因为整个web工程中只会有一个ServletContext对象，所以它和ServletConfig对象不一样的是它可以给其他的Servlet程序访问它的数据，就是说只要一个ServletContext对象里面有域对象，那么其他的程序可以取出里面的数据



**代码**：

```java
// 存域对象的Servlet程序 
protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletContext servletContext = getServletContext();
        servletContext.getAttribute("key");
        servletContext.setAttribute("key","域对象");
        System.out.println(servletContext.getAttribute("key"));
        System.out.println(servletContext.getAttribute("key"));
        System.out.println(servletContext.getAttribute("key"));
        System.out.println(servletContext.getAttribute("key"));
    }
```

其他的Servlet程序取域对象

```java
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletContext context = getServletContext();   // 获取ServletContext对象
        // 取出在其他Servlet程序放入的域对象
        System.out.println("其他Sevlet程序中的" + context.getAttribute("key"));
        System.out.println("其他Sevlet程序中的" + context.getAttribute("key"));
        System.out.println("其他Sevlet程序中的" + context.getAttribute("key"));
    }
```



**作用**

1. 获取web.xml文件中上下文参数context-param，context-param属于整个web工程，可以配置多组，根据需求来

2. 获取当前的工程路径，格式：/工程路径

   ​	getContextPath();

3. 获取部署后在服务器硬盘上的绝对路径

   ​	getRealPath("/");

   ​	" / "	表示http://IP:port/工程名		服务器会解析

   ​	映射到IDEA的web目录，比如访问web目录下的子目录：/子目录

   ​											比如访问web目录下的子目录中的子文件：/子目录/子文件	



**获取ServletContext对象的方法：**

​	①ServletConfig.getServletContext()方法

​	②getSverletContext()方法，里面的操作和①的方式一样



**补充**：只有一个ServletContext对象



**代码**：

xml.web文件中的代码

```xml
    <!--在文件的最上面写-->
    <context-param>
        <param-name>username</param-name>
        <param-value>context</param-value>
    </context-param>
    <context-param>
        <param-name>password</param-name>
        <param-value>root</param-value>
    </context-param>
```



java中的代码：

```java
 protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 获取上下文context-param的参数名
        String username = getServletContext().getInitParameter("username");
        System.out.println("context-param的参数名：" + username);
        // 获取当前的工程路径
        System.out.println(getServletContext().getContextPath());       // /Servlet
        // 获取工程部署后在服务器硬盘上的绝对路径
        System.out.println(getServletContext().getRealPath("/"));
    }
```





# HTTP协议

**协议**：指双方或者多方约定好，大家要遵守的规则

**http协议**：客户端给服务器通信，其中发送的数据要遵守的规则，http中的数据叫报文



## 请求的HTTP协议

**GET请求**

①请求行

1. 请求的方式						  GET或POST请求
2. 请求的资源路径                    [+?+请求参数]
3. 请求的协议和版本号		        HTTP1.1

②请求头

​	由key：value组成 

![image-20211013221300604](E:\学习笔记\img\20211013221301.png)



- Accept：告诉服务器，客户端可以接收的内容（文本、图片等等...）

- AcceptEncoding：告诉服务器，客户端可以接收的数据编码（压缩）格式

- Accept-Language：告诉服务器，客户端可以接收的语言

  ​	zh-CN		中文中国

  ​	en-US		英文美国

- Connection：告诉服务器，当前连接如何处理

  ​	Keep-alive：告诉服务器回传数据不要马上关闭，保持一小段时间的连接

  ​	Closed	  ：马上关闭

- Host：表示请求的服务器IP和端口号

- User-Agent：浏览器的信息

- Accept-Charaset:ISO-8859-1 [接受字符编码：iso-8859-1]

-  IF-MODIFIED-Since:Tue,11Jul 2000 18:23:51[告诉服务器我这缓存中有这个文件,该文件的时间是...]

- Referer:http://localhost:8080/test/abc.html[告诉服务器我来自哪里,常用于防止下载，盗链]]

- Date:[浏览器发送数据的请求时间]



**POST请求**

![image-20211013221343924](E:\学习笔记\img\20211013221345.png)

**说明**

-    location:http://www.baidu.org/index.jsp
-    server:apache tomcat [告诉浏览器我是tomcat]
-    Content-Encoding:gzip[告诉浏览器我使用了gzip]
-    Content-Lenght:80 [告诉浏览器回送的数据大小]
-    Content-Language:zh-cn[支持中文]
-    Content-Type:text/html;charset=gb2312[内容格式和编码]
-    Last-Modified:Tue,11 Juj,2000 18 18:29:20[告诉浏览器该资源上次更新时间是多少]
-    Refresh:1;url=http://www.baidu.com[过多久刷新到哪里去]
-    Content-Disposition;attachment;filename=aaa.zip[告诉浏览器有文件下载]
-    Transfer-Encoding:chunked［传输编码］
-    Set-Cookie:
-    Expires:-1[告诉浏览器如何缓存页面]
-    cache-Control:[告诉浏览器如何缓存页面(因为浏览器的兼容性最好设置两个)]
-    pragma:no-cache
-    Connection:close/Keep-Alive
-    Date:Tue,11 Jul 2000 18:23:51





**常用的请求头**

- Accept：告诉服务器，客户端可以接收的内容
- Accept-Language：告诉服务器，客户端可以接收的语言
- Host：表示请求的服务器IP和端口号
- User-Agent：浏览器的信息



## 响应的HTTP协议格式

1. 响应行

   ①响应的协议和版本号

   ②响应状态码

   ③响应状态描述符

2. 响应头

   ①key:value

3. 空行   ->   响应头和响应体之间空一行

4. 响应体     ->     回传给客户端的数据

![image-20211013221501605](E:\学习笔记\img\20211013221502.png)

**补充**：因为没有回传数据给客户端，所以在这里没有显示



## 什么时候用GET请求，什么时候用POST请求

**GET请求有哪些**

1. form标签			method=get
2. a 标签 
3. link 标签引入 css 
4. Script 标签引入 js 文件 
5. img 标签引入图片
6. iframe 引入 html 页面
7. 在浏览器地址栏中输入地址后敲回车



**POST请求有哪些**

form标签		method=post		目前学到的就一种



**常用的响应码**

| 状态码 | 表示                                                   |
| ------ | ------------------------------------------------------ |
| 200    | 成功                                                   |
| 302    | 重定向                                                 |
| 404    | 表示已经收到但是数据不存在（地址不存在或者资源不存在） |
| 504    | 表示服务器收到请求，但是服务器内部发生错误（代码错误） |





## MIMT类型说明

MIME 是 HTTP 协议中数据类型。 MIME 的英文全称是"Multipurpose Internet Mail Extensions" 多功能 Internet 邮件扩充服务。MIME 类型的格式是“大类型/小 类型”，并与某一种文件的扩展名相对应。

**常见的MIME类型**：

| 文件               | MIME 类型                                             |
| ------------------ | ----------------------------------------------------- |
| 超文本标记语言文本 | .html , .htm                 text/html                |
| 普通文本           | .txt                              text/plain          |
| RTF 文本           | .rtf                               application/rtf    |
| GIF 图形           | .gif                               image/gif          |
| JPEG 图形          | .jpeg,.jpg                     image/jpeg             |
| au  声音文件       | .au                               audio/basic         |
| MIDI 音乐文件      | mid,.midi                     audio/midi,audio/x-midi |
| RealAudio 音乐文件 | .ra, .ram                      audio/x-pn-realaudio   |
| MPEG 文件          | .mpg,.mpeg                video/mpeg                  |



### header中Content-Disposition

**格式说明：**
content-disposition = "Content-Disposition" ":" disposition-type *( ";" disposition-parm ) 　

**字段说明：

**Content-Disposition为属性名
disposition-type是以什么方式下载，如attachment为以附件方式下载
disposition-parm为默认保存时的文件名
服务端向客户端游览器发送文件时，如果是浏览器支持的文件类型，一般会默认使用浏览器打开，比如txt、jpg等，会直接在浏览器中显示，如果需要提示用户保存，就要利用Content-Disposition进行一下处理，关键在于一定要加上attachment：

复制代码代码如下:



Response.AppendHeader("Content-Disposition","attachment;filename=FileName.txt");





## 谷歌和火狐查看HTTP协议

1. 按下F12把开发者工具显示出来
2. 找到Network
3. 然后找到你要查看的资源文件

![image-20201203212454931](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201203212454931.png)





## HttpServletRequest类  - 请求

**作用**：每次只要有请求进入 Tomcat 服务器，Tomcat 服务器就会把请求过来的 HTTP 协议信息解析好封装到 Request 对象中。 然后传递到 service 方法（doGet 和 doPost）中给我们使用。我们可以通过 Request 对象，获取到所有请求的 信息。



**常用方法**

- getRequestURI() 获取请求的资源路径 
- getRequestURL() 获取请求的统一资源定位符（绝对路径）
- getRemoteHost() 获取客户端的 ip 地址 
- getHeader() 获取请求头 
- getParameter() 获取请求的参数 
- getParameterValues() 获取请求的参数（多个值的时候使用）
- getMethod() 获取请求的方式 GET 或 POST 
-  setAttribute(key, value); 设置域数据 
- getAttribute(key); 获取域数据 
- getRequestDispatcher() 获取请求转发对象            **请求转发的时候用到**



**代码演示**

页面中的代码

```html
</head>
<body>
    <form>
        用户名：<input type="text" name="username"><br>
        密码：<input type="password" name="password"><br>
        兴趣爱好：<input type="checkbox" name="hobby" value="Java">Java
        <input type="checkbox" name="hobby" value="C++">C++
        <input type="checkbox" name="hobby" value="JS">JS<br>
        <input type="submit">
    </form>
</body>
</html>
```

java中的代码

```java
 @Override
    protected void doGet(javax.servlet.http.HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // getRequestURI() 获取请求的资源路径
        System.out.println("请求的资源路径为：" + req.getRequestURI());

        // getRequestURL() 获取请求的统一资源定位符（绝对路径）
        System.out.println("请求的统一资源定位符为：" + req.getRequestURL());

        // getRemoteHost() 获取客户端的 ip 地址
        System.out.println("客户端的ip地址为：" + req.getRemoteHost());

        // 获得请求的参数
        String username = req.getParameter("username");
        String password = req.getParameter("password");
//        String hobby = req.getParameter("hobby");
        /*
            当选择多个的时候就会只显示第一个选项，
            所以这时候用getParameterValues()得到多个选项值,
            它会返回一个数组
         */
        String[] hobbies = req.getParameterValues("hobby");

        System.out.println("用户名为：" + username);
        System.out.println("密码名为：" + password);
        System.out.println("兴趣名为：" + Arrays.asList(hobbies));

        System.out.println("获取的方式为：" + req.getMethod());
    }
```

**结果**

![image-20211013222036040](E:\学习笔记\img\20211013222037.png)



**中文乱码问题**

GET请求不用设置字符集为UTF-8也可以显示中文

POST要设置UTF-8，不然有中文请求的时候会乱码，可以通过setCharaterEncoding("字符集")方法来解决

![image-20211013222047198](E:\学习笔记\img\20211013222048.png)

**解决**

![image-20211013222055223](E:\学习笔记\img\20211013222056.png)

**注意**：在设置之前执行一次获取请求参数的方法都会导致设置无效



## 请求的转发

what？：服务器收到请求后，从一个资源跳转到另一个资源的操作就请求转发

![image-20211013222103322](E:\学习笔记\img\20211013222104.png)



**代码实现**

Servlet1中的代码

```java
   @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 1.获取材料
        String username = req.getParameter("username");
        System.out.println("查看S1中的参数（材料）" + username);
        // 2.处理业务，然后盖章
        req.setAttribute("key",username);
        // 3.问路   -     获取跳转的路径
        RequestDispatcher requestDispatcher = req.getRequestDispatcher("/servlet2");
        // 4.跳转     -   传两个参数
        requestDispatcher.forward(req,resp);
    }
```

Servlet2中的代码

```java
@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 1.查看材料
        String username = req.getParameter("username");
        System.out.println("查看S2中的参数（材料）" + username);
        // 2.检查有没有盖章
        Object key = req.getAttribute("key");
        System.out.println("S1是否盖章" + key);
        // 3.处理自己的业务
        System.out.println("处理自己的业务");
    }
```



**请求转发的特点**：

1. 浏览器地址栏中没有变化

   ​	http://localhost:8080/Servlet2/servlet1?username=xiaozhi+&password=123

   ​	虽然已经转到了servlet2，但是地址栏是没有改变的

2. 他们是一次请求

3. 他们共享Request对象的数据

4. 可以用请求转发访问WEB-INF目录下的资源（WEB-INF不能直接访问的）

5. 只能在工程内访问资源，不能访问工程以为的资源，比如访问百度是不可以的







## base标签的作用

**作用**：设置页面相对路径的地址

**为什么要用这个？**

普通页面在跳转的时候没有问题，但是在使用**请求跳转**页面的时候，可以跳转过去，但是跳转不回来，因为地址发生了变化，页面跳转的时候地址是没有变化的，但是经过请求跳转之后，地址变成了Servlet程序中的地址，那么再使用之前的地址就会发生错误

![image-20211013222112320](E:\学习笔记\img\20211013222113.png)

这个时候就可以使用base标签设置相对路径，无论你的当前路径是什么，它的相对路径都是base标签中设置的那个



**代码实现**

Servlet程序

```java
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 设置跳转地址
        RequestDispatcher requestDispatcher = req.getRequestDispatcher("/a/b/c.html");
        // 跳转
        requestDispatcher.forward(req,resp);
    }
```

**主页面**

```html
    <title>Title</title>
    <!--设置页面相对路径的地址-->
    <base href="http://localhost:8080/Servlet2/">   <!--后面记得带/-->
</head>
<body>
    <a href="index.html">跳回页面</a>
</body>
```

**跳转的页面**

```html
<body>
    <a href="a/b/c.html">跳转到a/b/c.html中</a><br>
    <a href="http://localhost:8080/Servlet2/forword">请求转发</a>
</body>
```

**注意**：使用base标签的时候是改变当前页面的相对路径，不是整个工程的，所以这个时候你要知道那个页面是需要base标签的，然后进行添加就可以了



**在 javaWeb 中，路径分为相对路径和绝对路径两种**：

 相对路径是： . 表示当前目录 

​						.. 表示上一级目录 

​						资源名 表示当前目录/资源名 

​						绝对路径： http://ip:port/工程路径/资源路径 

在实际开发中，路径都使用绝对路径，而不简单的使用相对路径。 

1、绝对路径 				2、base+相对



**web中 / 斜杠的不同意义**

1. web中，斜杠是一种绝对路径
2. 被浏览器解析：http://IP:port
3. 被服务器解析:http://IP:port/工程名

**特殊情况**： response.sendRediect(“/”); 把斜杠发送给浏览器解析。得到 http://ip:port/







## HttpServletResponse类 - 响应

**作用**：HttpServletResponse 类和 HttpServletRequest 类一样。每次请求进来，Tomcat 服务器都会创建一个 Response 对象传 递给 Servlet 程序去使用。HttpServletRequest 表示请求过来的信息，HttpServletResponse 表示所有响应的信息， 我们如果需要设置返回给客户端的信息，都可以通过 HttpServletResponse 对象来进行设置



**获取流对象**

字节流：getOutStream()		常用于下载（传递二进制数据）

字符流：getWriter()				常用于回传字符串（常用）



**注意**：两个流同时只能使用一个。 使用了字节流，就不能再使用字符流



**往客户端回传数据**

```java
  @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 1.获取流对象
        PrintWriter writer = resp.getWriter();
        // 2.向客户端发送数据
        writer.println("客户端你好");	// 还没设置的编码集会出现乱码
        writer.println("客户端你好");
        writer.println("客户端你好");
    }
```



**响应乱码问题**

![image-20211013222121472](E:\学习笔记\img\20211013222122.png)

服务器给客户端发送中文会显示乱码，这个是因为浏览器解析的编码集和代码用的编码集不一致导致的，通过设置响应头解决乱码问题

方案一（不推荐使用）：通过响应头，设置浏览器也使用 UTF-8 字符集

```java
        // 设置服务器的编码集为UTF-8
        resp.setCharacterEncoding("UTF-8");
        // 设置浏览器的编码集为UTF-8
        resp.setHeader("Content-Type", "text/html; charset=UTF-8");
```

方案二（推荐使用）：它会同时设置服务器和客户端都使用 UTF-8 字符集，还设置了响应头

```java
        // 同时设置服务器和浏览器的编码集为UTF-8
        resp.setContentType("text/html; charset=UTF-8");
```

**注意**：两个方案都要在输出的前面设置好，不然是没有效果的，因为程序是从上到下执行的



## 请求重定向

相当是告示，告诉服务器我的旧地址不用了，你用这个新地址

```java
// 请求重定向的第一种方案：
// 设置响应状态码 302 ，表示重定向，（已搬迁）
resp.setStatus(302);
// 设置响应头，说明 新的地址在哪里
resp.setHeader("Location", "http://localhost:8080");

// 请求重定向的第二种方案（推荐使用）：
resp.sendRedirect("http://localhost:8080");
```

**补充**：重定向还可以用于解决表单重复提交的问题

**注意**：重定向跳转是不支持request域数据共享的





# MVC 概念 

**MVC 全称**：Model 模型、 View 视图、 Controller 控制器。 

MVC 最早出现在 JavaEE 三层中的 Web 层，它可以有效的指导 Web 层的代码如何有效分离，单独工作。 

**View 视图**：只负责数据和界面的显示，不接受任何与显示数据无关的代码，便于程序员和美工的分工合作—— JSP/HTML。 

**Controller 控制器**：只负责接收请求，调用业务层的代码处理请求，然后派发页面，是一个“调度者”的角色——Servlet。 转到某个页面。或者是重定向到某个页面。 

**Model 模型**：将与业务逻辑相关的数据封装为具体的 JavaBean 类，其中不掺杂任何与数据处理相关的代码—— JavaBean/domain/entity/pojo。

MVC 是一种思想 MVC 的理念是将软件代码拆分成为组件，单独开发，组合使用（**目的还是为了降低耦合度**）。

MVC 的作用还是为了降低耦合。让代码合理分层。方便后期升级和维护。

![image-20211013222141431](E:\学习笔记\img\20211013222142.png)











# XML

## 什么是xml文件？

- xml是可扩展性的标记语言，可以使用w3c定义的dom技术来解析



## XML文件的作用：

1. 可以用它来保存数据，而且这些数据具有描述性
2. 用来做项目或者模块的配置文件
3. 作为网络传输数据的格式（现以JSNO为主）



## XML中标签的命名规则

1. 字母、数字以及其他字符，不能有空格隔开
2. 每个属性的 值 必须用引号引起来
3. 正确的嵌套，和HTML一样
4. 标签要有闭合
5. 只能有一个根元素（顶级标签），没有父元素的标签就是根元素
6. xml对大小写敏感
7. XML中的特殊符号和html的一样





## XML的注释

1. 和html一样的注释
2. <![CDATA[   文本内容  ]]>    解析器不会解析，相当于是多行文本注释，不能两个进行嵌套





## 第三方解析

document 对象表示的是整个文档（可以是 html 文档，也可以是 xml 文档） 早期 JDK 为我们提供了两种 xml 解析技术 DOM 和 Sax 简介（已经过时，但我们需要知道这两种技术） dom 解析技术是 W3C 组织制定的，而所有的编程语言都对这个解析技术使用了自己语言的特点进行实现。 Java 对 dom 技术解析标记也做了实现。 

sun 公司在 JDK5 版本对 dom 解析技术进行升级：SAX（ Simple API for XML ） SAX 解析，它跟 W3C 制定的解析不太一样。它是以类似事件机制通过回调告诉用户当前正在解析的内容。 它是一行一行的读取 xml 文件进行解析的。不会创建大量的 dom 对象。 所以它在解析 xml 的时候，在内存的使用上。和性能上。都优于 Dom 解析。

 第三方的解析：对xml文档的解析（document对象的解析）

​		jdom 在 dom 基础上进行了封装 、 

​		dom4j 又对 jdom 进行了封装。 

​		pull 主要用在 Android 手机开发，是在跟 sax 非常类似都是事件机制解析 xml 文件。

 		这个 Dom4j 它是第三方的解析技术。我们需要使用第三方给我们提供好的类库才可以解析 xml 文件。



# Dom4j类库的使用

解压后

![image-20211013222151307](E:\学习笔记\img\20211013222152.png)



## 目录介绍

- docs目录是第三方类库的学习文档

  ​	进入docs中打开index.html文件

  ​	找到Quick start快速入门实例

- lib目录是dom4j需要依赖其他第三方的类库

- src 目录是第三方类库的源码目录（dom4j的源码）



## 编程步骤

1. 导入jar包

2. 创建xml文件，写入内容

3. new SAXReader()对象，通过对    read()方法    得到document对象

4. 通过    document对象.getRootElement()方法     得到根元素（顶级标签）

5. 根元素再通过      elements(“标签名”)方法           得到它的子元素的集合(不包括它)

6. 遍历

   - ​	第一种方式是用Element类型接收

     ​	通过    element("标签名")方法    得到指定的标签对象

   - ​    第二种是拿到标签中的文本内容

     ​	通过    elementText()方法    得到一个字符串文本

   

   

   **代码实现：**

```java
public static void main(String[] args) {

        // 得到SAXReader对象
        SAXReader sr = new SAXReader();
        // 用SAXReader对象的read()方法得到Document对象
        Document document = null;
        try {
            document = sr.read("XML\\books.xml");
        } catch (DocumentException e) {
            e.printStackTrace();
        } 
//        System.out.println(document);
        // 通过document对象拿到xml的根元素对象(books)
        Element rootElement = document.getRootElement();
        // Element.elements(标签名) 它可以得到当前元素下的指定子元素的集合(books标签下的所有子元素)
        // element(标签名)          它可以得到当前元素的指定子元素
        List<Element> books = rootElement.elements("book");
        // 遍历标签。获得标签中的所有内容
        // 第一种获取方式
        for (Element book : books) {
            // 测试：asXML()方法将整个document对象变成文本输出
//            System.out.printf( book.asXML());
            // 获得book下的name、price、author元素对象
            Element name = book.element("name");
            Element price = book.element("price");
            Element author = book.element("author");
            // attributeValue("属性名")获得属性值
            String bh = book.attributeValue("bh");
            //通过getText()方法得到标签之间的文本内容
            System.out.println("书名："+ "编号"+ bh +name.getText() + "价格：" + price.getText()
                        + "作者：" + author.getText());
        }
        // 遍历标签。获得标签中的所有内容
        // 第二种方式：将值封装到对象里
        for (Element book : books) {
            // elementText()方法  直接得到标签里面的文本内容
            String name = book.elementText("name");
            String price = book.elementText("price");
            String author = book.elementText("author");
            String bh = book.elementText("bh");
            System.out.println(new Book(name, Double.parseDouble(price), author, bh));
        }

    }
```

输出结果

![image-20211013222200029](E:\学习笔记\img\20211013222201.png)











