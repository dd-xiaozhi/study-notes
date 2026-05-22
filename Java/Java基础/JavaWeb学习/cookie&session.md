# cookie

## 什么是 Cookie?

1、 Cookie 翻译过来是饼干的意思。
2、 Cookie 是服务器通知客户端保存键值对的一种技术。
3、 客户端有了 Cookie 后， 每次请求都发送给服务器。
4、 每个 Cookie 的大小不能超过 4kb  





## 如何创建 Cookie  

![image-20201219214720752](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201219214720752.png)



**Servlet中的代码**

```java
	 protected void createCookie(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        // 创建cookie对象
        Cookie cookie = new Cookie("key1","value1");
        // 通知客户端保存cookie
        resp.addCookie(cookie);
        Cookie cookie2 = new Cookie("key2","value2");
        // 通知客户端保存cookie
        resp.addCookie(cookie2);
        Cookie cookie3 = new Cookie("key3","value3");
        // 通知客户端保存cookie
        resp.addCookie(cookie3);
        // 输出给客户端
        resp.getWriter().write("cookie创建成功");
    }
```



## 服务器如何获取 Cookie  

通过 req.getCookies():Cookie[]  获取

**cookie的工具类**

```java
public class CookieUtils {
    /**
     * 查找想要的cookie对象
     * @param name
     * @param cookies
     * @return
     */
    public static Cookie findCookie(String name,Cookie[] cookies){
        if (cookies == null || name == null || cookies.length == 0){
            return null;
        }
        // 遍历cookies数组，查找要查的cookie对象
        for (Cookie cookie:cookies) {
            if (name.equals(cookie.getName())){
                return cookie;
            }
        }
        return null;
    }
}

```

CookieServlet中的代码

```java
protected void getCookie(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 通过 req.getCookies()方法 得到cookie对象的数组
        Cookie[] cookies = req.getCookies();
        // 遍历输出数据
        for (Cookie cookie:cookies) {
            resp.getWriter().write("Cookie[" + cookie.getName() + "=" + cookie.getValue() + "] <br/>");
        }
        // 调用方法得到想要的cookie对象
        Cookie key3 = CookieUtils.findCookie("key3", req.getCookies());
        if (key3 != null){
            System.out.println("找到了需要的cookie");
        }
    }
```



## Cookie 值的修改  

### 方案一：

1、 先创建一个要修改的同名（指的就是 key） 的 Cookie 对象
2、 在构造器， 同时赋于新的 Cookie 值。
3、 调用 response.addCookie( Cookie );  通知客户端修改值

```java
    protected void updateCookie(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        1、 先创建一个要修改的同名（指的就是 key） 的 Cookie 对象
//        2、 在构造器， 同时赋于新的 Cookie 值。
        Cookie cookie = new Cookie("key1", "newkey");
//        3、 调用 response.addCookie( Cookie );
        resp.addCookie(cookie);     // 通知客户端保存修改
    }
```



### 方案二：

1、 先查找到需要修改的 Cookie 对象
2、 调用 setValue()方法赋于新的 Cookie 值。
3、 调用 response.addCookie()通知客户端保存修改  

```java
protected void updateCookie(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 方案二
//        1、 先查找到需要修改的 Cookie 对象
        Cookie key2 = CookieUtils.findCookie("key2", req.getCookies());
//        2、 调用 setValue()方法赋于新的 Cookie 值。
        key2.setValue("newValue2");
//        3、 调用 response.addCookie()通知客户端保存修改
        resp.addCookie(key2);
    }
```



## 浏览器查看cookie

### 谷歌浏览器如何查看 Cookie  

![image-20201219224155384](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201219224155384.png)



### 火狐浏览器如何查看 Cookie

![image-20201219224209570](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201219224209570.png)







## Cookie 生命控制 

Cookie 的生命控制指的是如何管理 Cookie 什么时候被销毁（删除）

**setMaxAge()**
正数， 表示在指定的秒数后过期

负数， 表示浏览器一关， Cookie 就会被删除（默认值是-1）

零， 表示马上删除 Cookie  ，在响应头中的set-cookie会设置成已过期



**默认存活时间**：就是session级别，就是浏览器关闭cookie就会别销毁，它的值是-1



### 正数

```java
protected void life3600(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 创建cookie对象
        Cookie cookie = new Cookie("life3600", "life3600");
        // 设置cookie的存活时间
        cookie.setMaxAge(3600);
        resp.addCookie(cookie);
        System.out.println("已经创建了一个存活一个小时的cookie对象");
    }
```

**结果显示**

![image-20201219225022319](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201219225022319.png)

**说明**：

T表示分隔符，Z表示的是UTC。

UTC：世界标准时间，在世界标准时间上加上8小时，即东八区时间，也就是北京时间。



### 零	-	立即销毁

```java
protected void deleteNow(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 1 找到要销毁的对象
        Cookie cookie = CookieUtils.findCookie("key3", req.getCookies());
        // 判断不等于null就销毁
        if (cookie != null){
            cookie.setMaxAge(0);       // 马上删除，不需要等到浏览器关闭
            resp.addCookie(cookie);
            System.out.println("key3的cookie已经被删除");
        }
    }
```







## Cookie 有效路径 Path 的设置  

Cookie 的 path 属性可以有效的过滤哪些 Cookie 可以发送给服务器。 哪些不发。

path 属性是通过请求的地址来进行有效的过滤。

| CookieA | path=/工程路径     |
| ------- | ------------------ |
| CookieB | path=/工程路径/abc |

**请求地址如下**：
	http://ip:port/工程路径/a.html
		CookieA 发送
		CookieB 不发送
	http://ip:port/工程路径/abc/a.html
		CookieA 发送
		CookieB 发送

**结论**：访问的地址要在cookie的path属性的地址的包含范围之内，不在范围内就不发送



**代码实现**

```java
protected void setPath(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 得到cookie对象
        Cookie cookie = new Cookie("path", "path");
        Cookie cookie2 = new Cookie("path2", "path2");
        // 设置cookie的path属性
        cookie.setPath(req.getContextPath() + "/abc");
        cookie2.setPath(req.getContextPath());
        // 通知客户端保存
        resp.addCookie(cookie2);
        resp.addCookie(cookie);
        System.out.println("创建了两个个带有path的cookie");
    }
```





## Cookie 练习---免输入用户名登录 

![image-20201219232914409](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201219232914409.png)

**说明**：登录成功就保存到cookie中发送到客户端，下次再进入页面的时候就读取页面中带有用户名的cookie

**代码实现**

```java
public class LoginServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 1获取用户名和密码
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        // 2判断用户名和密码是否正确
        if ("xiaozhi".equals(username) && "123".equals(password)) {
            // 正确就允许登录，并保存用户名到Cookie中，再发给服务器
            Cookie cookie = new Cookie("username", username);
            cookie.setMaxAge(60 * 60 * 24 * 7);     // 设置一周内不死亡
            resp.addCookie(cookie);         // 通知服务器保存
            resp.getWriter().write("登录成功");
        } else {
            // 错误，不允许登录
            resp.getWriter().write("您的用户名或者密码有误，请重新输入");
        }
    }
}
```









# session

## 什么是 Session 会话?  

1、 Session 就一个接口（HttpSession） 。
2、 Session 就是会话。 它是用来维护一个客户端和服务器之间关联的一种技术。
3、 每个客户端都有自己的一个 Session 会话。
4、 Session 会话中， 我们经常用来保存用户登录之后的信息。  





## 如何创建 Session 和获取(id 号,是否为新)  

如何创建和获取 Session。 它们的 API 是一样的。
	**request.getSession()**
第一次调用是： 创建 Session 会话
之后调用都是： 获取前面创建好的 Session 会话对象。

**isNew();** 		判断到底是不是刚创建出来的（新的）
**true** 		表示刚创建
**false**		表示获取之前创建

每个会话都有一个身份证号。 也就是 ID 值。 而且这个 ID 是唯一的。
**getId()** 		得到 Session 的会话 id 值  



**代码实现**

```java
 protected void createSession(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 获取Session对象
        HttpSession session = req.getSession();
        resp.setContentType("text/html; charset=UTF-8");
        resp.getWriter().print("创建的Session对象" + session + "</br>");
        //判断是否是新创建出来的
        resp.getWriter().write("是否是新创建的" + session.isNew() + "</br>");
        resp.getWriter().write("Session的ID是：" + session.getId() + "</br>");
    }
```

![image-20201222105804918](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201222105804918.png)

![image-20201222105820963](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201222105820963.png)



## Session 域数据的存取  

**setAttribute()**		保存数据，键值对的形式

**getAttribute()**		  取出数据，通过key

**removeAttribute(String)**		删除指定的session

```java
    protected void setAttribute(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 得到session对象
        HttpSession session = req.getSession();
        // 保存数据到session域中
        session.setAttribute("key1","value1");
        resp.setContentType("text/html; charset=UTF-8");
        resp.getWriter().write("保存成功");
    }
    protected void getAttribute(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 得到session对象
        HttpSession session = req.getSession();
        // 保存数据到session域中
        String key1 = (String) session.getAttribute("key1");
        resp.getWriter().write(key1);
    }
```





## Session 生命周期控制  

**public void setMaxInactiveInterval(int interval)**  设置 Session 的超时时间（ 以秒为单位） ， 超过指定的时长， Session就会被销毁。

值为正数的时候， 设定 Session 的超时时长。
负数表示永不超时**（极少使用）**，会造成服务器负载，因为会有很多的session对象

**public int getMaxInactiveInterval()**		获取 Session 的超时时间
**public void invalidate()** 						 让当前 Session 会话马上超时无效。

Session 默认的超时时长是多少！
Session 默认的超时时间长为 30 分钟。
因为在 Tomcat 服务器的配置文件 web.xml中默认有以下的配置， 它就表示配置了当前 Tomcat 服务器下所有的 Session
超时配置默认时长为： 30 分钟。我们可以通过配置文件来修改它的默认时间(按需求来**)**

**修改默认时间的操作**：

```xml
<!--表示当前 web 工程。 创建出来 的所有 Session 默认是 20 分钟 超时时长-->
    <session-config>
        <session-timeout>20</session-timeout>
    </session-config>
```

只修改个别 Session 的超时时长。使用 setMaxInactiveInterval(int interval)来进行单独的设置。
session.setMaxInactiveInterval(int interval)单独设置超时时长。  

**Session 超时的概念介绍**

![image-20201222113153725](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201222113153725.png)

**代码实现**

```java
// 默认超时时间
    protected void sessionLife(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int maxInactiveInterval = req.getSession().getMaxInactiveInterval();
        resp.setContentType("text/html; charset=UTF-8");
        resp.getWriter().write("session的默认超长时间为：" + maxInactiveInterval);

    }
    // 3秒销毁
    protected void life3(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 单独设置某个session的超时时间
        req.getSession().setMaxInactiveInterval(3);
    }
    // 马上超时
    protected void now(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getSession().invalidate();
    }
```







## 浏览器和 Session 之间关联的技术内幕  

Session 技术， 底层其实是基于 Cookie 技术来实现的。  

![image-20201222113442850](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201222113442850.png)



**说明**：通过cookie对象保存session的id值，要创建session的时候会先去找有没有这个存有id的cookie，如果没有就会新创建一个session对象，如果有，那么就返回之前创建好的session对象，超时也是通过删除掉保存session对象信息的cookie对象，那么session对象去找cookie的时候就会找不到之前创建的，所以它又会创建一个新的。







