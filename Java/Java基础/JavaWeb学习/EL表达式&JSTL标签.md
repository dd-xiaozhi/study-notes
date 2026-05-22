# EL表达式

## JavaBean特点

-   实现Serializable接口实现实例化

-   显示无参构造



## 什么是 EL 表达式，EL 表达式的作用? 

EL 表达式的全称是：Expression Language。是表达式语言。 

EL 表达式的什么作用：EL 表达式主要是代替 jsp 页面中的表达式脚本在 jsp 页面中进行数据的输出。 因为 EL 表达式在输出数据的时候，要比 jsp 的表达式脚本要简洁很多

```jsp
<body>
    <% pageContext.setAttribute("key","123"); %>
    用表达式脚本输出数据为：<%= request.getAttribute("key") %>
    <hr>
    用EL表达式脚本输出数据为：${ key }
</body>
```

EL 表达式的格式是：${表达式} 

**注意**：EL 表达式在输出 null 值的时候，输出的是空串。jsp 表达式脚本输出 null 值的时候，输出的是 null 字符串。



## EL 表达式搜索域数据的顺序 

EL 表达式主要是在 jsp 页面中输出数据。 主要是输出域对象中的数据。 当四个域中都有相同的 key 的数据的时候，EL 表达式会按照四个域的从小到大的顺序去进行搜索，找到就输出。

```jsp
<body>
    <%
        session.setAttribute("key","session");
        request.setAttribute("key","request");
        application.setAttribute("key","application");
        pageContext.setAttribute("key","pageContext");
    %>
    ${key}
</body>
```



## EL输出Bean

EL 表达式输出 Bean 的普通属性，数组、List集合、map集合属性

```jsp
<body>
<%
    Person person = new Person();
    // 初始化值
    person.setName("小智");
    person.setPhone(new String[]{"123456","111111","23432423"});
    person.setList(new ArrayList());
    List list = person.getList();
    list.add("黑椒");
    list.add("黑鬼");
    list.add("魔法");
    person.setMap(new HashMap());
    Map map = person.getMap();
    map.put("key1","哈哈");
    map.put("key2","哈哈2");
    map.put("key3","哈哈3");
    map.put("key",person);
    // 将初始化好的person放到域中
    pageContext.setAttribute("p",person);
%>

<%--EL 表达式输出 Bean 的普通属性，数组、List集合、map集合属性--%>
普通属性的值：${p.name}<hr>
    
<%--输出单个数组的时候输出的是对象，而不是放在数组里面的值--%>
直接数组的值：${p.phone}<br>
    
<%--用下标输出数据输出的就是数组的值--%>
用下标输出数组的值：${p.phone[0]}<hr>
    
<%--有序的集合输出数据是可以把值输出的--%>
输出list集合的值：${p.list}<br>
用下标输出集合的值：${p.list[0]}<hr>
    
<%--键值对集合直接输出,输出的是集合里面的值--%>
输出map集合：${p.map}<br>
<%--键值对集合用下标输出是不行的--%>
用下标输出map集合：${p.map[0]}<br>
map集合中某个key的值：${p.map.key1}
</body>
```

**小结**：

1. 数组：取数组中的数据可以用下标的方式取出数组，直接输出数组输出的是数组对象
2. 元集合：直接输出数据就可以把所有数据输出，通过下标的方式取出单个的值
3. 键值对集合：直接输出的是对象，可以通过.key值的方式输出单个值



**EL表达式的底层**：找的是源码中的get方法，而不是直接找属性





# EL表达式-运算

语法：${ 运算表达式 } ， EL 表达式支持如下运算符：



## 关系运算符

| **关系运算符** | **说  明** | **范  例**                         | **结果** |
| -------------- | ---------- | ---------------------------------- | -------- |
| == 或 eq       | 等于       | ${  5 == 5   }   或 ${ 5 eq 5    } | true     |
| != 或 ne       | 不等于     | ${  5 !=5  }  或 ${ 5 ne 5    }    | false    |
| < 或 lt        | 小于       | ${  3 < 5   }   或 ${ 3 lt 5    }  | true     |
| > 或 gt        | 大于       | ${  2 > 10  }   或 ${ 2 gt 10  }   | false    |
| <=  或 le      | 小于等于   | ${ 5 <= 12 } 或 ${ 5 le 12 }       | true     |
| >= 或 ge       | 大于等于   | ${ 3 >= 5 }  或 ${ 3 ge 5 }        | false    |



## 逻辑运算符

| **逻辑运算符** | **说  明** | **范  例**                                               | **结果** |
| -------------- | ---------- | -------------------------------------------------------- | -------- |
| && 或 and      | 与运算     | ${  12 == 12 && 12 < 11 } 或 ${ 12 == 12 and 12  < 11 }  | false    |
| \|\| 或 or     | 或运算     | ${ 12 == 12 \|\| 12 < 11  } 或 ${ 12 == 12 or 12 < 11  } | true     |
| ! 或 not       | 取反运算   | ${ !true } 或 ${not true }                               | false    |



## 算术运算符

| 算术运算符 | 说明 | 范例                                   | 结果 |
| ---------- | ---- | -------------------------------------- | ---- |
| +          | 加法 | ${ 12 + 18 }                           | 30   |
| -          | 减法 | ${ 18 - 8 }                            | 10   |
| *          | 乘法 | ${ 12 * 12 }                           | 144  |
| / 或 div   | 除法 | ${ 144 / 12 }  或 ${ 144 **div** 12  } | 12   |
| % 或 mod   | 取模 | ${ 144 % 10 }  或 ${ 144 **mod** 10  } | 4    |



**用法和java的一样，在这不演示了**



## empty运算

**格式**：${ empty 值 }

**作用**：empty 运算可以判断一个数据是否为空，如果为空，则输出 true,不为空输出 false。以下几种情况为空：

​	1、值为 null 值的时候，为空

​	2、值为空串的时候，为空

​	3、值是 Object 类型数组，长度为零的时候

​	4、list 集合，元素个数为零

​	5、map 集合，元素个数为零

**代码**

```jsp
<body>
<%
//    1、值为 null 值的时候，为空
    request.setAttribute("null",null);
//    2、值为空串的时候，为空
    request.setAttribute("空串","");
//    3、值是 Object 类型数组，长度为零的时候
    request.setAttribute("Object",new Object[]{});
//    4、list 集合，元素个数为零
    request.setAttribute("list",new ArrayList<>());
//    5、map 集合，元素个数为零
    request.setAttribute("map",new HashMap<>());
%>
为null值时：${empty null}<br>
为空串值时：${empty 空串}<br>
Object类型数组长度为零时：${empty Object}<br>
lsit集合元素为零时：${empty list}<br>
map集合元素为零时：${empty map}<br>
</body>
```



## 三元运算符

**格式**：表达式 1？表达式 2：表达式 3 

如果表达式 1 的值为真，返回表达式 2 的值，如果表达式 1 的值为假，返回表达式 3 的值。

**代码**

```jsp
三元运算符的应用<br>
${12==12?"小智是个靓仔":"小智变坏了"}
```







## “.”点运算 和 [] 中括号运算符 

.点运算，可以输出 Bean 对象中某个属性的值。 

[]中括号运算，可以输出有序集合中某个元素的值。 并且[]中括号运算，还可以输出 map 集合中 key 里含有**特殊字符**的 key 的值。

**代码**

```jsp
点运算和[]运算<br>
<%
    Map map = new HashMap<String,Object>();
    map.put("a.a.a","zhi1");
    map.put("b-b-b","zhi2");
    map.put("c+c+c","zhi3");
    pageContext.setAttribute("map",map);
%>
${ map["a.a.a"] }<br>
${ map["b-b-b"] }<br>
${ map["c+c+c"] }<br>
</body>
```





# EL 表达式的 11 个隐含对象





是 EL 表达式中自己定义的，可以直接使用。 

| 变量             | 类型            | 作用                                           |
| ---------------- | --------------- | ---------------------------------------------- |
| pageContext      | PageContextImpl | 它可以获取 jsp 中的九大内置对象                |
| pageScope        | Map             | 它可以获取 pageContext 域中的数据              |
| requestScope     | Map             | 它可以获取 Request 域中的数据                  |
| sessionScope     | Map             | 它可以获取 Session 域中的数据                  |
| applicationScope | Map             | 它可以获取 ServletContext 域中的数据           |
| param            | Map             | 它可以获取请求参数的值                         |
| paramValues      | Map             | 它也可以获取请求参数的值，获取多个值的时候使用 |
| header           | Map             | 它可以获取请求头的信息                         |
| headerValues     | Map             | 它可以获取请求头的信息，它可以获取多个值的情况 |
| cookie           | Map             | 它可以获取当前请求的Cookie 信息                |
| initParam        | Map             | 它可以获取在 web.xml 中配置的上下文参数        |



## EL 获取四个特定域中的属性 

pageScope 					=		pageContext 域 				

requestScope 			   =		Request 域 

sessionScope 			   =		 Session 域 

applicationScope 		 =		ServletContext 域



==声明周期分别是：当前页面、一次请求、一次会话、整个web工程==



**代码演示**

```jsp
<body>
<%
    pageContext.setAttribute("key","pageContext");
    session.setAttribute("key","session");
    request.setAttribute("key","request");
    request.setAttribute("key2","request2");
    application.setAttribute("key","application");
%>
<%--如果直接输出key，那么就会从最小的域中取出数据--%>
输出域中key的值为：${key}<br>
<%--
    通过requestScope获取域中的值，可以用[]的方式获取域中指定的值
    如果没有指定值，那么输出域中的所有的值
--%>
获取request域中所有的值为：${ requestScope }<br>
获取request域中指定的值为：${ requestScope.key }<br>
</body>
```





## pageContext对象的使用

**作用**

1. 获取协议
2. 获取服务器 ip 
3. 获取服务器端口
4. 获取工程路径
5. 获取请求方法
6. 获取客户端 ip 地址
7. 获取会话的 id 编号

```jsp
<%--
    表达式脚本
    1. 协议：<%= request.getScheme() %>
    2. 服务器 ip：<%= request.getServerName() %>
    3. 服务器端口：<%= request.getServerPort() %>
    4. 获取工程路径：<%= request.getContextPath() %>
    5. 获取请求方法：<%= request.getMethod() %>
    6. 获取客户端 ip 地址：<%= request.getRemoteHost() %>
    7. 获取会话的 id 编号：<%= session.getId() %>
--%>

EL表达式<br>
1. 协议：${ pageContext.request.scheme }<br>
2. 服务器 ip：${pageContext.request.serverName}<br>
3. 服务器端口：${ pageContext.request.serverPort }<br>
4. 获取工程路径：${pageContext.request.contextPath}<br>
5. 获取请求方法：${ pageContext.request.method }<br>
6. 获取客户端 ip 地址：${ pageContext.request.remoteHost }<br>
7. 获取会话的 id 编号：${ pageContext.session.id }<br>
<%--底层其实就是调用get方法--%>
```





## 其他隐含对象的使用

**param** 					Map			  它可以获取请求参数的值 

**paramValues** 		Map 	 		它也可以获取请求参数的值，获取多个值的时候使用。

```jsp
${ param.username }<br>
${ param.hobby }<br>    <%--多个值的时候如果用param的话就只会输出第一个--%>
${ paramValues.hobby[1] }
```

![image-20201210180453299](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201210180453299.png)

**补充**：通过?给参数赋值，多个参数用&连接





**header** 				Map 				它可以获取请求头的信息 

**headerValues** 	Map 	 			它可以获取请求头的信息，它可以获取多个值的情况

```jsp
输出整个请求头的值：${ header }<br>     <%--获取整个请求头--%>
输出【accept-language】的值：${header['accept-language']}<br>
输出【host】的值：${ header.host }<br>    <%--获取请求头中的某个属性--%>
输出【User-Agent】中指定的属性的值：${ headerValues['User-Agent'][0] }
```





**cookie** 				Map 				它可以获取当前请求的 Cookie 信息

```jsp
<%--cookie Map<String,Cookie> 它可以获取当前请求的 Cookie 信息--%>
Cookie的名称：${ cookie.JSESSIONID.name }<br>
Cookie的值：${ cookie.JSESSIONID.value }<br>
```



**initParam** 			Map 				它可以获取在 web.xml 中配置的上下文参数 web.xml 中的配置：

```jsp
<%--initParam Map<String,String> 它可以获取在 web.xml 中配置的<context-param>上下文参数--%>
上行文参数username的值为：${ initParam.username }<br>
上行文参数password的值为：${ initParam.password }
```





# JSTL标签库

JSTL 标签库 全称是指 JSP Standard Tag Library JSP 标准标签库。

是一个不断完善的开放源代码的 JSP 标 签库。 EL 表达式主要是为了替换 jsp 中的表达式脚本，而标签库则是为了替换代码脚本。这样使得整个 jsp 页面 变得更佳简洁。



**JSTL 由五个不同功能的标签库组成。**

| 功能范围         | URI                                    | 前缀 |
| ---------------- | -------------------------------------- | ---- |
| 核心标签库--重点 | http://java.sun.com/jsp/jstl/core      | c    |
| 格式化           | http://java.sun.com/jsp/jstl/fmt       | fmt  |
| 函数             | http://java.sun.com/jsp/jstl/functions | fn   |
| 数据库(不使用)   | http://java.sun.com/jsp/jstl/sql       | sql  |
| XML(不使用)      | http://java.sun.com/jsp/jstl/xml       | x    |



在 jsp 标签库中使用 taglib 指令引入标签库 

```jsp
CORE 标签库 <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 

XML 标签库<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %> 

FMT 标签库 <%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %> 

SQL 标签库 <%@ taglib prefix="sql" uri="http://java.sun.com/jsp/jstl/sql" %> 

FUNCTIONS 标签库 <%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
```

![image-20201210203725497](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201210203725497.png)



# JSTL 标签库的使用

**使用**：

1. 导入jar包

   taglibs-standard-impl-1.2.1
   taglibs-standard-spec-1.2.1

2. 使用 taglib 指令引入标签库

    ```html
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
    ```

    



## core核心类库

### **set**(用的很少)

**作用**：往域中保存数据
    	**scope属性**	设置保存到那个域
        	page 表示 PageContext 域（默认值）
        	request 表示 Request 域
        	session 表示 Session 域
        	application 表示 ServletContext 域
   	 **var属性**	设置key是多少
    	**value属性**	设置值是多少

```jsp
保存数据之前：${ applicationScope.aa }<br>
<c:set scope="application" var="aa" value="123"></c:set>
保存数据之后：${ applicationScope.aa }<br>
```





### **<c:if />**标签

**注意**：不能用if-else的，只能使用单个if进行判断

```jsp
<%--
    <c:if/>标签的使用
        作用：用来做if判断
        test 属性表示判断的条件（使用 EL 表达式输出）
--%>
<c:if test="${ 10 == 10 }">
    小智好帅哦
</c:if>
<c:if test="${ 10 != 10 }">
    小智还是很帅
</c:if>
```



### <c:chose><c:when><otherwise>组合标签

**作用**：多路判断，和java中的switch-case-default类似
        choose 标签开始选择判断
        when 标签表示每一种判断情况
            test 属性表示当前这种判断情况的值
        otherwise 标签表示剩下的情况

**注意**：
         1、标签里不能使用 html 注释，要使用 jsp 注释
         2、when 标签的父标签一定要是 choose 标签
         3、在标签里面不能使用代码脚本

```jsp
<%
    request.setAttribute("score",50);
%>
<c:choose>
    <c:when test="${ requestScope.score >= 90 }">
        优秀
    </c:when>
    <c:when test="${ requestScope.score >= 70 }">
        良好
    </c:when>
    <c:when test="${ requestScope.score >= 60 }">
        及格
    </c:when>
    <c:otherwise>
        <c:choose>
            <c:when test="${ requestScope.score >= 50 }">
                有点差
            </c:when>
            <c:otherwise>
                太差了
            </c:otherwise>
        </c:choose>
    </c:otherwise>
</c:choose>
```



### <c:forEach />标签

**作用**：遍历输出使用
        begin属性：从那个索引开始
        end属性：到那个索引结束
        items属性：获取数据源
        var属性：接收数据的变量
        step 属性表示遍历的步长值
        varStatus 属性表示当前遍历到的数据的状态

```jsp
<%--
    4. 遍历 List 集合---list 中存放 Student 类，
        有属性：id，用户名，密码信息
--%>
<%
    List<Student> list = new ArrayList<Student>();
    for (int i = 1; i <= 10; i++) {
        list.add(new Student(i, "name" + i, "密码" + i));
    }
    request.setAttribute("list", list);
%>
<table>
    <tr>
        <td>编号</td>
        <td>用户名</td>
        <td>密码</td>
    </tr>
    <c:forEach begin="2" end="7" varStatus="status" step="2" items="${requestScope.list}" var="stu">
        <tr>
            <td>${stu.id}</td>
            <td>${stu.username}</td>
            <td>${stu.password}</td>
            <td>${status.current}</td>
        </tr>
    </c:forEach>
</table>
```

**说明**：当输出varStatus的时候输出的是个对象

![image-20201210223420792](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201210223420792.png)

