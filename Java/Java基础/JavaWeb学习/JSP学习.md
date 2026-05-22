# JSP技术

## 什么是JSP

JSP(全称 Java Server Pages)是由 Sun 公司专门为了解决动态生成 HTML 文档的技术。

### 用Servlet 程序输出 html 页面。

```java
public class HtmlServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 设置回传给客户端的编码集
        resp.setContentType("text/html;charset=UTF-8");
        // 获得输出流
        PrintWriter writer = resp.getWriter();
        writer.write("<!DOCTYPE html>\r\n");
        writer.write("<html>\r\n");
        writer.write("<head>\r\n");
        writer.write("<meta charset=\"UTF-8\">\r\n");
        writer.write("<title>Insert title here</title>\r\n");
        writer.write("</head>\r\n");
        writer.write("<body>\r\n");
        writer.write("这是由 Servlet 程序输出的 html 页面内容！\r\n");
        writer.write("</body></html>\r\n");
    }
}
```

**结果显示**

![image-20201207184836632](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201207184836632.png)



**总结**：上面的代码我们不难发现。通过 Servlet 输出简单的 html 页面信息都非常不方便。 那我们要输出一个复杂页面的时候，就更加的困难，而且不利于页面的维护和调试。 所以 sun 公司推出一种叫做 jsp 的动态页面技术帮助我们实现对页面的输出繁锁工作。 jsp 页面的访问千万不能像 HTML 页面一样。托到浏览器中。只能通过浏览器访问 Tomcat 服务器再访问 jsp 页面。

**创建一个动态的jsp**

![image-20201207185043291](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201207185043291.png)

访问的方式和html的方式一样



# jsp 的运行原理

**jsp 的本质***

其实是一个 Servlet 程序，服务器会把jsp文件翻译成.java文件并进行编译

首先我们去找到我们 Tomcat 的目录下的 work\Catalina\localhost 目录。当我们部署 web 工程。并启动 Tomcat 服务器后。我们发现 在 work\Catalina\localhost 目录下多出来一个 web工程目录

![image-20201207190256413](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201207190256413.png)

进入到a_jsp.java文件中可以看到

![image-20201207191020677](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201207191020677.png)

![image-20201207191537292](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201207191537292.png)

**底层的原理都是一样的**

这些代码是放在了_jspService()方法里面，这个方法和我们的Servlet程序里面的Service()方法是一样的功能





# JSP的语法（重点）

## JSP文件头部声明介绍（page指令）

<%@ page contentType="text/html;charset=UTF-8" language="java" %>

这是jsp头部声明，表示这是jsp页面

- i. language 属性 表示 jsp 翻译后是什么语言文件。暂时只支持 java。

- ii. contentType 属性 表示 jsp 返回的数据类型是什么。也是源码中 response.setContentType()参数值 

- iii. pageEncoding 属性 表示当前 jsp 页面文件本身的字符集。 

- iv. import 属性 跟 java 源代码中一样。用于导包，导类。 

  

  ========================两个属性是给 out 输出流使用============================= 



- v. autoFlush 属性 设置当 out 输出流缓冲区满了之后，是否自动刷新冲级区。默认值是 true。 

- vi. buffer 属性 设置 out 缓冲区的大小。默认是 8kb 

  ![image-20201207193948017](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201207193948017.png)

  **缓冲区溢出错误**：

  ![image-20201207193857100](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201207193857100.png)

  

  

   ========================两个属性是给 out 输出流使用============================= 

  

- vii. errorPage 属性 设置当 jsp 页面运行时出错，自动跳转去的错误页面路径。 
  errorPage 表示错误后自动跳转去的路径
  这个路径一般都是以斜杠打头，它表示请求地址为 http://ip:port/工程路径/	映射到代码的 Web 目录

  ​	![image-20201207194736166](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201207194736166.png)

​	

- viii. isErrorPage 属性 设置当前 jsp 页面是否是错误信息页面。默认是 false。如果是 true 可以 获取异常信息。 

  就是jsp文件中出现了异常，那么这个异常会发送给客户看到，这样对客户是很不友好的



- ix. session 属性 设置访问当前 jsp 页面，是否会创建 HttpSession 对象。默认是 true。 
- x. extends 属性 设置 jsp 翻译出来的 java 类默认继承谁。   -     和java的继承一样用法



## 声明脚本（极少使用）

格式：<%!	声明Java代码	%>

**作用**：可以给 jsp 翻译出来的 java 类定义属性和方法甚至是静态代码块。内部类等。 

**代码实现**

```jsp
</head>
<body>
<%--1 声明类属性--%>
<%!
    private int id;
    private String name;
    private Map map;
%>
<%-- 2 声明 static静态代码块 --%>
<%!
    static {
        System.out.println("小智是个靓仔");
    }
%>
<%-- 3 声明类方法 --%>
<%!
    public void Demo(){
        System.out.println("在jsp文件中声明的方法");
    }
%>
<%--4 声明内部类--%>
<%!
    class Demo{
        private int id;
        private String name;
    }
%>
</body>
</html>
```

![image-20201207200144354](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201207200144354.png)



## 表达式脚本（常用）

格式：<%=	表达式	%>

表达式脚本的作用是：在jsp 页面上输出数据。 表达式脚本的特点	-	相当于是打印 

- 1、所有的表达式脚本都会被翻译到_jspService() 方法中
- 2、表达式脚本都会被翻译成为 out.print()输出到页面上
- 3、由于表达式脚本翻译的内容都在_jspService() 方法中,所以_jspService()方法中的对象都可以直接使用。
- 4、表达式脚本中的表达式不能以分号结束。因为在编译的.java文件中是已经有分号结尾的了

**代码实现**

```jsp
<%--表达式脚本--%>
<%--1. 输出整型--%>
<%= 11 %><br>
<%--2. 输出浮点型--%>
<%= 11.11 %><br>
<%--3. 输出字符串--%>
<%= "小智是个靓仔" %>><br>
<%--4. 输出对象--%>
<%= map %>><br>
<%--5、使用_jspService()方法的对象 --%>
<%= request.getParameter("username") %><br>
```

![image-20201207201457192](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201207201457192.png)



## 代码脚本

格式：<%	java语句	%>

代码脚本的作用是：可以在 jsp 页面中，编写我们自己需要的功能（写的是 java 语句）。

代码脚本的特点是： 

- 1、代码脚本翻译之后都在_jspService 方法中 _
- _2、代码脚本由于翻译到_jspService()方法中，所以在_jspService()方法中的现有对象都可以直接使用。 
- 3、还可以由多个代码脚本块组合完成一个完整的 java 语句。 
- 4、代码脚本还可以和表达式脚本一起组合使用，在 jsp 页面上输出数据

**代码实现**

```jsp
</head>
<body>
<%--第一种用法--%>
<%
    for (int i = 0; i < 10; i++) {
        System.out.println("智哥帅的一批");
    }
%>
<%--第二种用法：嵌套使用--%>
<%
    for (int i = 0; i < 10; i++) {
%>
    <%= "智哥帅的一批" %><br>
<%
    }
%>
<%
    String username = request.getParameter("username");
    System.out.println(username);
%>
</body>
</html>
```



![image-20201207202219733](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201207202219733.png)



## JSP中的三种注释

html注释：<!--注释的内容-->

java注释：在代码脚本中写，代码脚本写的就是java代码，java中的注释一样可以来用

JSP注释：<%--	注释内容	--%>	这个是不会被翻译到java源码中去的，是真的注释



# JSP九大内置对象

jsp 中的内置对象，是指 Tomcat 在翻译 jsp 页面成为 Servlet 源代码后，内部提供的九大对象，叫内置对象。

![image-20201207210725456](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201207210725456.png)

- request				   请求对象

- response                响应对象

- pageContext           jsp的上下文对象

- session                   回话对象

- application              ServletConntext对象

- config                      ServletConfig对象

- out                           jsp输出流对象

- page                        指向当前的jsp的对象

- exception                 异常对象

  

## JSP四大域对象

**四个域对象分别是**：

- pageContext (PageContextImpl 类) 当前 jsp 页面范围内有效，跳转到别的jsp页面它就无效了 

- request (HttpServletRequest 类)、 一次请求内有效，两次请求同一个页面就无效了 

- session (HttpSession 类)、 一个会话范围内有效（打开浏览器访问服务器，直到关闭浏览器就无效了） 

- application (ServletContext 类) 整个 web 工程范围内都有效（只要 web 工程不停止，数据都在） 域对象是可以像 Map 一样存取数据的对象。四个域对象功能一样。不同的是它们对数据的存取范围。 虽然四个域对象都可以存取数据。在使用上它们是有优先顺序的。

  

  **注意**：工程重新部署或者停止，四个域对象都会没有

  四个域在使用的时候，优先顺序分别是，他们从小到大的范围的顺序。

   pageContext ====>>> request ====>>> session ====>>> application



**scope.jsp页面**

```jsp
<body>
<h1>scope.jsp 页面</h1>
<%
    // 往四个域中都分别保存了数据
    pageContext.setAttribute("key", "pageContext");
    request.setAttribute("key", "request");
    session.setAttribute("key", "session");
    application.setAttribute("key", "application");
%>
pageContext 域是否有值：<%=pageContext.getAttribute("key")%> <br>
request 域是否有值：<%=request.getAttribute("key")%> <br>
session 域是否有值：<%=session.getAttribute("key")%> <br>
application 域是否有值：<%=application.getAttribute("key")%> <br>
<%
    request.getRequestDispatcher("/scope2.jsp").forward(request,response);
%>
</body>
```

**scope2.jsp页面**

```jsp
<body>
<h1>scope2.jsp 页面</h1>
pageContext 域是否有值：<%=pageContext.getAttribute("key")%> <br>
request 域是否有值：<%=request.getAttribute("key")%> <br>
session 域是否有值：<%=session.getAttribute("key")%> <br>
application 域是否有值：<%=application.getAttribute("key")%> <br>
</body>
```



## JSP中out输出 和 response.getWriter是输出的区别

response 中表示响应，我们经常用于设置返回给客户端的内容（输出） 

out 也是给用户做输出使用的。

![image-20201208130503358](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201208130503358.png)

**得出结论**：两个输出同时存在的时候，不管位置如何，response的输出在out的前面，要想out输出在response输出的前面我们可以在写完out输出就response刷新一下，这样就可以了

JSP使用out来输出，一般在JSP页面中统一out来进行输出，避免打乱页面中的输出，因为服务器翻译的时候也是将JSP输出到页面中的内容用out.writer和out.print()输出的。



### writer和print两者的区别

![image-20201208143123712](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201208143123712.png)

出现上图的原因：它是一个char类型，会转成ASCLL码

![image-20201208144122140](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201208144122140.png)



print()方法的底层就是将内容转型转成字符串类型，然后再调用writer()方法进行输出

writer()方法只适合输出字符串

**深入源码得出结论**：在JSP页面中，统一使用out.print()方法输出，因为什么类型都是可以往里面放的，底层会转型







# JSP常用标签

**说明**：在jsp页面的设计中，有页头，页体和页脚，页脚在实际的应用中一般所有页面的页脚都是一样的，所以这种情况下可以使用包含来引用一个jsp页面，需要修改的时候只需要修改一个jsp页面就可以完成所有的页面设置

### **静态包含**（常用）

一个jsp文件引用另外一个jsp文件，修改一个jsp文件就可以让引用了jsp文件的jsp页面也改变，可以把它理解成是css引用css文件

**格式**：<%@ include file="文件路径"%>

<%@ include file=""%> 就是静态包含 file 属性指定你要包含的 jsp 页面的路径 地址中第一个斜杠 / 表示为 http://ip:port/工程路径/ 映射到代码的 web 目录 

**静态包含的特点**： 

​	1、静态包含不会翻译被包含的 jsp 页面。 

​	2、静态包含其实是把被包含的 jsp 页面的代码拷贝到包含的位置执行输出。

**代码**

```jsp
<%@ include file="b.jsp" %>
```



### 动态包含

和静态包含的功能一样，但是他们的底层源码不一样

**格式**：<jsp:include page="a.jsp">< /jsp:include>

这是动态包含 page 属性是指定你要包含的 jsp 页面的路径 动态包含也可以像静态包含一样。把被包含的内容执行输出到包含位置 

动态包含的特点： 

​	1、动态包含会把包含的 jsp 页面也翻译成为 java 代码 

​	2、动态包含底层代码使用如下代码去调用被包含的 jsp 页面执行输出。

​		JspRuntimeLibrary.include(request, response, "/include/footer.jsp", out, false); 

![image-20201208122451436](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201208122451436.png)

​	3、动态包含，还可以传递参数 



**动态包含的底层原理**：footer页面中的out对象引用main页面的out缓冲区

![image-20201208123144473](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201208123144473.png)



**代码**

```jsp
<jsp:include page="a.jsp">
    <jsp:param name="username" value="xiaozhi"/>   <%--传参--%>
    <jsp:param name="password" value="123"/>
</jsp:include>
```



### 请求转发

**格式**：<jsp:forword page="文件路径">

**代码**

```jsp
<jsp:forward page="scope2.jsp"></jsp:forward>
```





# JSP练习

## 在页面上打印九九乘法表

```jsp
<body>
<%--在页面中打印久久乘法表--%>
<h1 align="center">九九乘法表</h1>
    <table align="center">
        <% for (int i = 1; i <= 9; i++) { %>
        <tr>
            <% for (int j = 1; j <= i; j++) { %>
            <th><%= j + "*" + i + "=" + (j * i) %>
            </th>
            <% } %>
        </tr>
        <% } %>
    </table>
</div>
</body>
```



## 练习二：打印表格

jsp 输出一个表格，里面有 10 个学生信息。	-	请求转发的方式

模拟从数据库获取信息显示到网页上

请求转发的具体实现
        1.获取请求的参数
        2.到数据库中查询数据
        3.返回结果给客户端

**Servlet程序代码**

```java
@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 请求转发的具体实现
        // 1.获取请求的参数
        // 2.到数据库中查询数据
        // 利用for循环模拟从数据库中拿取数据
        List<Student> list = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            list.add(new Student("name" + i,18 + i,"phone" + i));
        }
        // 保存结果到域中
        req.setAttribute("key",list);
        // 请求转发到jsp页面中显示
        req.getRequestDispatcher("/test/stuTable.jsp").forward(req,resp);
    }
```

**jsp页面**

```jsp
<table align="center">
    <tr>
        <td>姓名</td>
        <td>年龄</td>
        <td>电话</td>
    </tr>

    <%
        // 3.返回结果给客户端
        List<Student> list = (List<Student>)request.getAttribute("key");
        for (Student stu:list) {
    %>
        <tr>
            <td><%= stu.getName() %></td>
            <td><%= stu.getAge() %></td>
            <td><%= stu.getPhone() %></td>
        </tr>
    <% } %>
</table>
```

**注意**：是经过了servlet程序请求转发到jsp页面，所以访问的时候要访问servlet程序，直接访问jsp页面就没有数据了的





# 监听器

## 什么是监听器？

1. 三大组件之一
2. 是JavaEE的规范，是个接口
3. 监听事物的变化，通过回调函数，反馈给客户



## ServletContextListener

有8个监听器，但是经过时代的变更，基本不适用，只需要了解一下ServletContextListener

**特点**：在web工程启动的时候就创建了，在web工程停止的时候销毁

**作用**：可以监听servletcontext对象的创建和销毁

**使用方式**：（具体在Spring阶段）

1. 自定义一个类实现接口
2. 实现两个回调方法
3. 到web.xml中配置监听器的配置信息

```java
public class ServletContextListener implements javax.servlet.ServletContextListener {
    /**
     * 在web启动的时候对servletcontext对象进行初始化
     * @param servletContextEvent
     */
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        System.out.println("web启动的时候进行初始化");
    }
    /**
     * web停止的时候销毁对象
     * @param servletContextEvent
     */
    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        System.out.println("web停止的时候销毁");
    }
}
```

web.xml

```xml
 <!--配置ServletContextListener类的配置信息-->
    <listener>
        <listener-class>com.xiaozhi.java.ServletContextListener</listener-class>
    </listener>
```







# 总结

1. .java和.class文件只有在客户端访问jsp资源的时候才会有的，服务器停止就没了
2. 写语句的时候要注意不能写注释在句子的后面
3. 请求转发不仅可以转发到Servlet程序，还可以转发到页面

