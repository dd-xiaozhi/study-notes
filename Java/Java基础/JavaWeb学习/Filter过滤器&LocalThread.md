# Filter过滤器

1、 Filter 什么是过滤器
1、 Filter 过滤器它是 JavaWeb 的三大组件之一。 三大组件分别是： Servlet 程序、 Listener 监听器、 Filter 过滤器
2、 Filter 过滤器它是 JavaEE 的规范。 也就是接口
3、 Filter 过滤器它的作用是： 拦截请求， 过滤响应。
拦截请求常见的应用场景有：
1、 权限检查
2、 日记操作
3、 事务管理
……等等  





# Filter过滤器的使用

**说明**：Filter过滤器可以拦截访问，只有通过过滤器才能访问指定的文件或者页面



## 为什么要使用Filter过滤器

**要求**： 在你的 web 工程下， 有一个 xiaozhi目录。 这个 xiaozhi目录下的所有资源（html 页面、 jpg 图片、 jsp 文件、 等等） 都必须是用户登录之后才允许访问。  

```jsp
<%
    // 模拟登录之后才能访问资源
    // 获取保存在Session域中的user对象
    Object user = request.getSession().getAttribute("user");
    // 如果没有登录
    if (user == null){
        // 跳转到登录页面
        request.getRequestDispatcher("/login.jsp").forward(request,response);
        // 结束方法
        return;
    }
%>
```



**说明**：当访问jsp页面的时候我们可以用代码去解决访问的问题，但是其他的页面或者资源就不能写代码去处理访问权限的问题了，因此需要引进Filter过滤器进行拦截，如果没有我给你权限你是不能访问目标资源的





## Filter过滤器的工作流程

![image-20201224224033502](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201224224033502.png)





## Filter 过滤器的使用步骤

1、 编写一个类去实现 Filter 接口
2、 实现过滤方法 doFilter()
3、 到 web.xml 中去配置 Filter 的拦截路径  



### 模拟要访问目标资源必须登录的场景

第一步：实现Filter接口并实现过滤方法doFilter();

```java
@Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // 进行类型转换
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        // 获取保存在session域中的user对象,这里我用request域代替session域来演示
        Object user = httpServletRequest.getParameter("user");
        // 需要登录才能访问资源
        if (user == null){
            // 跳转到登录页面
       httpServletRequest.getRequestDispatcher("/login.jsp").forward(servletRequest,servletResponse);
        } else {
            // 可以访问目标资源
            filterChain.doFilter(servletRequest,servletResponse);
        }
    }
```



第二步：配置Filter的拦截路径

```xml
<!--filter标签配置Filter过滤器的配置信息-->
<filter>
    <!--起别名-->
    <filter-name>FilterTest</filter-name>
    <!--配置filter的全类名-->
    <filter-class>com.xiaozhi.java.FilterTest</filter-class>
</filter>
<filter-mapping>
    <!--filter-name表示当前拦截给那个filter使用-->
    <filter-name>FilterTest</filter-name>
    <!--url-pattern 配置拦截路径
        / 表示请求地址为： http://ip:port/工程路径/ 映射到 IDEA 的 web 目录
        /xiaozhi/* 表示请求地址为： http://ip:port/工程路径/xiaozhi/*
    -->
    <url-pattern>/xiaozhi/*</url-pattern>
    <!--可以使用多个url-pattern标签-->
    <!--<url-pattern>/</url-pattern>-->
</filter-mapping>
```





# 完整的用户登录

Filter过滤器

```java
@Override
public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    // 进行类型转换
    HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
    // 获取保存在session域中的user对象,这里我用request域代替session域来演示
    Object user = httpServletRequest.getSession().getAttribute("user");
    // 需要登录才能访问资源
    if (user == null){
        // 跳转到登录页面
        servletRequest.getRequestDispatcher("/login.jsp").forward(servletRequest,servletResponse);
        // 结束方法
        return;
    } else {
        // 可以访问目标资源
        filterChain.doFilter(servletRequest,servletResponse);
    }
}
```

login.jsp页面

```html
这是登录页面。 login.jsp 页面 <br>
<form action="http://localhost:8080/filter/loginServlet" method="get">
用户名： <input type="text" name="username"/> <br>
密 码： <input type="password" name="password"/> <br>
<input type="submit" />
</form>
```



**说明**：当我们访问xiaozhi目录下的资源的时候会跳到登录界面，登录之后我们才能访问xiaozhi目录下的资源







# Filter 的生命周期  

①空参的构造器和init(FilterConfig filterConfig)方法在启动工程的时候就会执行

②doFilter()方法在访问需要拦截的资源时就会调用

③destroy()方法在工程停止的时候调用





# FilterConfig 类  

FilterConfig 类见名知义， 它是 Filter 过滤器的配置文件类。
Tomcat 每次创建 Filter 的时候， 也会同时创建一个 FilterConfig 类， 这里包含了 Filter 配置文件的配置信息。
FilterConfig 类的作用是获取 filter 过滤器的配置内容

1. 获取 Filter 的名称 filter-name 的内容

   **filterConfig.getFilterName()**

2. 获取在 Filter 中配置的 init-param 初始化参数

   **filterConfig.getInitParameter("参数名")**	  参数值		

   **filterConfig.getInitParameter("url")**			url的值

3. 获取 ServletContext 对象  

   **filterConfig.getServletContext()**  



**代码实现**

web.xml

```xml
<filter>
    <!--起别名-->
    <filter-name>FilterTest</filter-name>
    <!--配置filter的全类名-->
    <filter-class>com.xiaozhi.java.FilterTest</filter-class>
    <!--filter的初始化参数-->
    <init-param>
        <param-name>username</param-name>
        <param-value>xiaozhi</param-value>
    </init-param>
    <init-param>
        <param-name>url</param-name>
        <param-value>jdbc:mysql://localhost:3306/test</param-value>
    </init-param>
</filter>
```

java代码

```java
@Override
public void init(FilterConfig filterConfig) throws ServletException {
    // 获得filter-name的值
    System.out.println("filter-name的值是：" + filterConfig.getFilterName());
    // 获取web.xml配置文件中的init-param初始化参数值
    System.out.println("初始化参数username的值为：" + filterConfig.getInitParameter("username"));
    System.out.println("初始化参数url的值为：" + filterConfig.getInitParameter("url"));
    // 获取ServletContext对象
    System.out.println("ServletContext对象为：" + filterConfig.getServletContext());
}
```

结果显示

![image-20201225102950476](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201225102950476.png)





# FilterChain 过滤器链  

Filter 	过滤器

Chain	链

FilterChain 就是过滤器链（多个过滤器如何一起工作）

![image-20201225103223223](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201225103223223.png)







## Filter 的拦截路径  

### --精确匹配

```xml
<url-pattern>/target.jsp</url-pattern>
```


以上配置的路径， 表示请求地址必须为： http://ip:port/工程路径/target.jsp

### --目录匹配

```xml
<url-pattern>/xiaozhi/*</url-pattern>
```


以上配置的路径， 表示请求地址必须为： http://ip:port/工程路径/xiaozhi/*

### --后缀名匹配

```xml
<url-pattern>*.html</url-pattern>
```

以上配置的路径， 表示请求地址必须以.html 结尾才会拦截到

```xml
<url-pattern>*.abc</url-pattern>
```

以上配置的路径， 表示请求地址必须以.do 结尾才会拦截到*

```xml
<url-pattern>*.xiaozhi</url-pattern>
```

以上配置的路径， 表示请求地址必须以.xiaozhi结尾才会拦截到



**注意**：Filter 过滤器它只关心请求的地址是否匹配， 不关心请求的资源是否存在！ ！ ！





# 注意

①Filter的构造器前面必须要public修饰，不然服务启动不了

​	会出现java.lang.IllegalAccessException: Class org.apache.catalina.core.DefaultInstanceManager can not错误

②进行请求跳转的时候一定要记得加上return结束方法







# ThreadLocal

ThreadLocal 的作用， 它可以解决多线程的数据安全问题。
ThreadLocal 它可以给当前线程关联一个数据（可以是普通变量， 可以是对象， 也可以是数组， 集合）
ThreadLocal 的特点：
1、 ThreadLocal 可以为当前线程关联一个数据。 （它可以像 Map 一样存取数据， key 为当前线程）
2、 每一个 ThreadLocal 对象， 只能为当前线程关联一个数据， 如果要为当前线程关联多个数据， 就需要使用多个
ThreadLocal 对象实例。
3、 每个 ThreadLocal 对象实例定义的时候， 一般都是 static 类型
4、 ThreadLocal 中保存数据， 在线程销毁后。 会由 JVM 虚拟自动释放。  

**补充**：ThreadLocal对象的get和set方法可以把对应的线程和数据取出来

**注意**：当前线程只能关联一个数据，如果有多个数据关联，那么是以最后一个为准的，如果想放多个，那么可以创建多个TheadLocal对象进行保存



## 代码测试

OrderDaoTest

```java
public class OrderDaoTest {
    public void saveOrder(){
        String name = Thread.currentThread().getName();
        System.out.println("OrderDao 当前线程[" + name + "]中保存的数据是： " +
                ThreadLocalTest.threadLocal.get());
    }
}
```

OrderServiceTest

```java
public class OrderServiceTest {
    public void createOrder(){
        String name = Thread.currentThread().getName();
        System.out.println("OrderService 当前线程[" + name + "]中保存的数据是： " +
                ThreadLocalTest.threadLocal.get());
    }
}
```

ThreadLocalTest

```java
public class ThreadLocalTest {
    public static ThreadLocal<Object> threadLocal = new ThreadLocal<Object>();
    private static Random random = new Random();
    public static class Task implements Runnable{
        @Override
        public void run() {
            // 随机生成一个数(线程要关联的数据)，然后保存到ThreadLocal中
            Integer i = random.nextInt(1000);
            // 获取当前线程名
            String name = Thread.currentThread().getName();
            System.out.println("线程["+name+"]生成的随机数是： " + i);
            // 保存到ThreadLocal对象中
            threadLocal.set(i);
            try {
                // 休息3秒
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 保存订单
            new OrderServiceTest().createOrder();

            // run方法结束之前，查看是否可以取出之前保存的数据
            Object o = threadLocal.get();
            System.out.println("在线程["+name+"]快结束时取出关联的数据是： " + o);
        }
    }

    public static void main(String[] args) {
        for (int i = 0;i < 3; i++){
            new Thread(new Task()).start();
        }
    }
}
```



**结果显示**

![image-20201225113956917](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201225113956917.png)



**可以看到**：存的线程和取出的线程是一致的，这样的就保证了数据一致性







**书城的第八阶段	-	使用 Filter 和 ThreadLocal 组合管理事务** 



# 作用

​	①权限控制

​	②事物处理：我们事物要进行提交和回滚操作，这两个操作都要使用同一个connection对象，如果不是同一个对象就可	能操作数据丢失的问题，不安全