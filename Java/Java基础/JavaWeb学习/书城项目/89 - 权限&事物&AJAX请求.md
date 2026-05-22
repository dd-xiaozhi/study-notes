# 第八阶段

# 权限检查

使用 Filter 过滤器拦截/pages/manager/所有内容， 实现权限检查  

**代码实现**

web.xml

```xml
<!--ManagerFilter过滤器的配置信息-->
<filter>
    <filter-name>ManagerFilter</filter-name>
    <filter-class>com.xiaozhi.Filter.ManagerFilter</filter-class>
</filter>
<filter-mapping>
    <filter-name>ManagerFilter</filter-name>
    <url-pattern>/pages/manager/*</url-pattern>
</filter-mapping>
```

ManagerFilter类

```java
@Override
public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    // 登录才能进行查看
    HttpServletRequest req  = (HttpServletRequest) servletRequest;
    // 获取保存在session域中的user对象进行判断
    User user = (User) req.getSession().getAttribute("user");
    if (user == null){
        // 跳转到登录页面
        req.getRequestDispatcher("/pages/user/login.jsp").forward(req,servletResponse);
        return;
    } else {
        // 可以访问目标资源
        filterChain.doFilter(servletRequest,servletResponse);
    }
}
```







# 使用 Filter 和 ThreadLocal 组合管理事务  

**说明**：在结账的时候会出现付款了但是没有生成订单的情况，因为没有开启事物的原因



使用 ThreadLocal 来确保所有 dao 操作都在同一个 Connection 连接对象中完成  

**原理分析**：从图中我们可以看出保存订单的操作是由三个Dao类来完成的，所以我们要保证他们连接的是同一个连接对象，这里用到了我们的ThreadLocal对象set()保存，然后使用get()获取数据就可以了。

![image-20201225194221795](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201225194221795.png)





## JdbcUtils 工具类的修改 

**思路**：判断ThreadLocal对象之前是否有创建，如果没有那么就用dataSource.getConnection()方法得到一个Connection对象，然后放入到ThreadLocal对象中

JDBCUtils工具类的修改

```java
public class JDBCUtils {
    // 数据库连接池
    private static DataSource dataSource;
    // 创建一个ThreadLocal对象
    private static ThreadLocal<Connection> conns = new ThreadLocal<Connection>();
    // 初始化连接池
    static {
        try {
            Properties properties = new Properties();
            // 读取 jdbc.properties 属性配置文件
            InputStream inputStream = JDBCUtils.class.getClassLoader()
                    .getResourceAsStream("jdbc.properties");
            // 加载文件
            properties.load(inputStream);
            dataSource  = (DataSource) DruidDataSourceFactory.createDataSource(properties);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    // 获取连接
    public static Connection getConnection(){
        Connection conn = conns.get();
        // 如果coon为空，那么就创建一个
        if ( conn == null ){
            try {
                conn = dataSource.getConnection();
                // 保存到ThreadLocal对象中
                conns.set(conn);
                conn.setAutoCommit(false);      // 手动提交事物
            } catch (SQLException e) {
                e.printStackTrace();
                // 回滚事物
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return conn;
    }

    /**
     * 提交事物并关闭释放资源
     */
    public static void commitAntClose(){
        // 获取连接
        Connection conn = conns.get();
        if (conn != null){     // 如果不等于 null， 说明 之前使用过连接， 操作过数据库
            try {
                conn.commit();  // 提交事物
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    conn.close();   // 关闭连接
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

        }
        // 一定要执行remove操作，Tomcat底层使用了线程池技术
        conns.remove();
    }

    /**
     * 回滚事物并释放连接
     */
    public void rollbackAndClose(){
        Connection conn = conns.get();
        if (conn != null){       // 如果不等于 null， 说明 之前使用过连接， 操作过数据库
            try {
                conn.rollback();    // 回滚事物
            } catch (SQLException e) {
                e.printStackTrace();
            }finally {
                // 关闭并释放连接
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
//    // 关闭连接
//    public static void getClose(Connection conn){
//        // 判断
//        if (conn != null){
//            try {
//                conn.close();
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//        }
//    }
}
```





## BaseDao的修改

**说明**：Dao中的异常往外抛，让事物可以捕捉到，处理提交或者回滚，因为在Dao中已经进行了对异常的捕捉，这个时候事物就会认为没有异常，那么就会进行事物提交，导致我们的事物出现问题

Dao中不能有关闭连接的操作，因为用的是同一个连接，如果关了，那么后面的程序就不能执行了

```java
public class BaseDao {
    private QueryRunner queryRunner = new QueryRunner();
    /**
     * 增删改操作
     * @param sql
     * @param args
     * @return  返回-1表示操作失败，反之表示成功
     */
    public int update(String sql,Object...args){
        // 获取连接
        Connection connection = JDBCUtils.getConnection();
        try {
            int update = queryRunner.update(connection, sql, args);
            return update;
        } catch (SQLException e) {
            e.printStackTrace();
            // 向上抛异常
            throw new RuntimeException(e);
        }
    }
    /**
     * 查询返回一个javaBean对象
     * @param type
     * @param sql
     * @param args
     * @param <T>
     * @return
     */
    public <T> T queryForOne(Class type ,String sql,Object...args){
        Connection connection = JDBCUtils.getConnection();
        try {
            T query = queryRunner.query(connection, sql, new BeanHandler<T>(type), args);
            return query;
        } catch (SQLException e) {
            e.printStackTrace();
            e.printStackTrace();
            // 向上抛异常
            throw new RuntimeException(e);
        }
    }

    /**
     * 查询返回多个JavaBean对象
     * @param type
     * @param sql
     * @param args
     * @param <T>
     * @return
     */
    public  <T> List<T> queryForList(Class type , String sql, Object...args){
        Connection connection = JDBCUtils.getConnection();
        try {
            List<T> list = queryRunner.query(connection, sql, new BeanListHandler<T>(type), args);
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            e.printStackTrace();
            // 向上抛异常
            throw new RuntimeException(e);
        }
    }

    /**
     * 查询返回一行一列的sql语句，比如count函数之类的
     * @param sql
     * @param args
     * @return
     */
    public Object queryOne(String sql,Object...args){
        Connection connection = JDBCUtils.getConnection();
        try {
            Object query = queryRunner.query(connection, sql, new ScalarHandler(), args);
            return query;
        } catch (SQLException e) {
            e.printStackTrace();
            e.printStackTrace();
            // 向上抛异常
            throw new RuntimeException(e);
        }
    }
}
```



**我们还要给OrderServlet中的保存操作进行事物处理**

```java
try {
    // 保存到数据库中
    orderId = orderService.createOrder(cart, userId);
    // 没有异常就提交事物并关闭释放资源
    JDBCUtils.commitAntClose();
} catch (Exception e) {
    e.printStackTrace();
    JDBCUtils.rollbackAndClose();    // 回滚事物
}
```





# Filter处理事物

**原理分析图**：

![image-20201225205817394](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201225205817394.png)

**说明**：之前的方式是调用Service中的方法都要 try-catch 一下，这样会显得很笨重，而且特别麻烦，所以我们可以使用Filter过滤器一次性全部进行事物管理，从上图可以看到，我们直接将Filter过滤器中的filterChain.doFilter()方法try-catch一下，然后再到web.xml文件中配置要过滤的资源就可以实现事物的管理了



**代码实现**

Filter类代码

```java
@Override
public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    try {
        filterChain.doFilter(servletRequest,servletResponse);
        // 没有发生异常就提交事物
        JDBCUtils.commitAntClose();
    } catch (IOException e) {
        e.printStackTrace();
        // 发生异常后回滚事物
        JDBCUtils.rollbackAndClose();
    }
}
```



web.xml文件

```xml
<!--TransactionFilter过滤器的配置信息-->
<filter>
    <filter-name>TransactionFilter</filter-name>
    <filter-class>com.xiaozhi.Filter.TransactionFilter</filter-class>
</filter>
<filter-mapping>
    <filter-name>TransactionFilter</filter-name>
    <!--/*表示当前工程下的所有请求-->
    <url-pattern>/*</url-pattern>
</filter-mapping>
```



BaseServlet程序的修改	-	一定要抛出异常给Filter过滤器捕捉到，不然是没有进行事物处理的

```java
@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String action = req.getParameter("action");
    try {
        Method method = this.getClass().getDeclaredMethod(action, HttpServletRequest.class, HttpServletResponse.class);
        method.invoke(this,req,resp);
    } catch (Exception e) {
        e.printStackTrace();
        throw  new RuntimeException(e);
    }
}
```

**补充**：如果不抛异常的话页面就会一片空白，客户是一脸懵逼的。

# 友好提示

**说明**：因为处理事物之后还要将异常抛给Tomcat服务器去处理，客户端看到的页面是一堆异常信息，这对用户很不友好，所以当出现异常的时候让页面跳转到我们学号的错误页面，并给用户提示



具体实现	-	在web.xml中配置通过错误页面配置来进行管理

```xml
<!--页面错误配置文件-->
<error-page>
    <!--error-code是错误的类型-->
    <error-code>500</error-code>
    <!--location是发生错误后要跳转的页面的地址-->
    <location>/pages/error/error500.jsp</location>
</error-page>
<error-page>
    <!--error-code是错误的类型-->
    <error-code>404</error-code>
    <!--location是发生错误后要跳转的页面的地址-->
    <location>/pages/error/error404.jsp</location>
</error-page>
```







# 使用 AJAX 验证用户名是否可用 

![image-20201226212706065](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201226212706065.png)



UserServlet中添加

```java
protected void ajaxExistsUsername(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    // 获取AJAX请求发送过来的用户名
    String username = req.getParameter("username");
    boolean existsUsername = userService.existsUsername(username);
    Map<String,Object> map = new HashMap<String,Object>();
    map.put("existsUsername",existsUsername);
    // 转换成JSON 对象
    Gson gson = new Gson();
    String json = gson.toJson(map);
    resp.getWriter().write(json);
}
```

regist.jsp页面

```javascript
// 给用户名输入框绑上焦点改变事件
$("#username").blur(function () {
    var username = this.value;
    $.getJSON("http://localhost:8080/book/userServlet","action=ajaxExistsUsername" +
        "&username=" + username ,function (data) {
        // 收到响应后要执行的代码
        if (data.existsUsername){
            $("span.errorMsg").text("用户名可用");
        } else {
            $("span.errorMsg").text("用户名已存在");
        }

    });
});
```





# 使用 AJAX 修改把商品添加到购物车  

![image-20201226214842877](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201226214842877.png)



index.jsp

js代码

```javascript
// 给添加购物车按钮绑上单击事件
$("button.addToCart").click(function () {
    // 1得到书本的id值
    var bookId = $(this).attr("bookId");
    // 带有id参数跳转到CartSerlvet程序中的addItem方法中
    //location.href = "http://localhost:8080/book/cartServlet?action=addItem&id=" + bookId;
    // 发送ajax请求
    $.getJSON("http://localhost:8080/book/cartServlet", "action=ajaxAddItem&id=" + bookId
        , function (data) {
            var count = data.count;
            var lastName = data.lastNmae;
            $("#cartTotalCount").text("您的购物车中有" + count + "件商品");
            $("#LastName").text(lastName);
        })
});
```

html代码

```html
<div>
    <%--没登录之前的界面--%>
    <c:if test="${empty sessionScope.user}">
        <a href="pages/user/login.jsp">登录</a> |
        <a href="pages/user/regist.jsp">注册</a> &nbsp;&nbsp;
    </c:if>
    <%--登录成功的界面--%>
    <c:if test="${not empty sessionScope.user}">
        <span>欢迎<span class="um_span">${sessionScope.user.username}</span>光临尚硅谷书城</span>
        <a href="pages/order/order.jsp">我的订单</a>
        <a href="userServlet?action=logout">注销</a>&nbsp;&nbsp;
    </c:if>

    <a href="pages/cart/cart.jsp">购物车</a>
    <a href="pages/manager/manager.jsp">后台管理</a>
</div>
```



cartServlet程序

```java
protected void ajaxAddItem(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    // 获得请求过来的id
    int id = WebUtils.parseInt(req.getParameter("id"), 0);
    // 通过id值获得book对象
    BookService bookService = new BookServiceImpl();
    Book book = bookService.queryBookById(id);
    // 将book对象中的信息放入到购物车对象中
    CartItem cartItem = new CartItem(book.getId(), book.getName(), book.getPrice(), 1, book.getPrice());
    // 取出session中保存的cart对象
    Cart cart = (Cart) req.getSession().getAttribute("cart");
    // 如果之前没有创建，就创建一个cart对象
    if (cart == null) {
        cart = new Cart();
        req.getSession().setAttribute("cart", cart);
    }
    // 调用cart中的方法添加商品项
    cart.addItem(cartItem);
    // 保存最后一个值到session域中
    // 最后一个添加的商品名称
    req.getSession().setAttribute("lastName", cartItem.getName());
    // 创建一个map存放数据
    Map<String,Object> resultMap = new HashMap<String,Object>();
    resultMap.put("count",cart.getTotalCount());
    resultMap.put("lastNmae",cartItem.getName());
    // 创建json对象
    Gson gson = new Gson();
    String json = gson.toJson(resultMap);
    resp.getWriter().write(json);
}
```









# 管理员

设置管理员，只有管理员才能进入到后台管理	-	写死，只能是用户名为xiaozhi

思路：使用Filter过滤器过滤掉



代码实现







