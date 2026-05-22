

# JSON

## 什么是JSON？

JSON (JavaScript Object Notation) 是一种轻量级的数据交换格式。 易于人阅读和编写。 同时也易于机器解析和生成。 JSON采用完全独立于语言的文本格式， 而且很多语言都提供了对 json 的支持（包括 C, C++, C#, Java, JavaScript, Perl, Python等） 。 这样就使得 JSON 成为理想的数据交换格式。

**json 是一种轻量级的数据交换格式。**
轻量级指的是跟 xml 做比较。比xml解析快，也就是效率高
数据交换指的是客户端和服务器之间业务数据的传递格式。  

**补充**：json对象的存储和map对象是一样的，不过它是用冒号将键和值隔开，也可以用等于号，不过解析到最后还是冒号的

# JSON 在 JavaScript 中的使用  

## json 的定义

json 是由键值对组成， 并且由花括号（大括号） 包围。 每个键由引号引起来， 键和值之间使用冒号进行分隔，
多组键值对之间进行逗号进行分隔。  



json对象可以放的类型可以有整型，字符串，数组，json对象（嵌套一个）,json数组（存放json对象的数组）



## JSON定义示例

```javascript
// json的定义
var jsonObj = {
   "key1":123,                   
   "key2":"abc",
   "key3":false,
   "key4":[123,"abc",false],
   "key5":{
      "key5_1":51123,
      "key5_2":"5_1_abc"
   },
   "key6":[{
   "key_6_1_1":611123,
   "key_6_1_1":"6_1_1_abc"
   },{
      "key_6_1_2":612123,
      "key_6_1_2":"6_1_2_abc"
   }]
};
```



## JSON的访问

json 本身是一个对象。
json 中的 key 我们可以理解为是对象中的一个属性。
json 中的 key 访问就跟访问对象的属性一样： json 对象.key

json 访问示例  

```javascript
// json的访问
var key1 = jsonObj.key1;
alert(key1);
alert(jsonObj.key2);
alert(jsonObj.key3);
alert(jsonObj.key4[1]);       // 也是和普通数组一样的用法
alert(jsonObj.key5);         // 得到json对象
alert(jsonObj.key5.key5_1);       // 访问json
alert(jsonObj.key6[0].key_6_1_1);  // 获得json对象的属性值
```



## json 的两个常用方法  

json 的存在有两种形式。

- 一种是： 对象的形式存在， 我们叫它 json 对象。

- 一种是： 字符串的形式存在， 我们叫它 json 字符串。

  

  JSON对象是Object类型

  一般我们要操作 json 中的数据的时候， 需要 json 对象的格式。
  一般我们要在客户端和服务器之间进行数据交换的时候， 使用 json 字符串。
  **JSON.stringify()**	 把 json 对象转换成为 json 字符串
  **JSON.parse()** 		把 json 字符串转换成为 json 对象  

**代码示例**

```javascript
			// json对象转字符串
			// JSON.stringify()	 把 json 对象转换成为 json 字符串
			var jsonString = JSON.stringify(jsonObj);	// 和java中的toString类似
			alert(jsonString);
			// json字符串转json对象
			// JSON.parse()		把 json 字符串转换成为 json 对象
			var stringToJSON = JSON.parse(jsonString);
			alert(stringToJSON);
```





# JSON 在 java 中的使用

1、导入Gson包

**gson.toJson()**			将java中的变量转换成JSON对象

**gson.fromJson()**		将JSON对象转换成java变量





## javaBean 和 json 的互转 

```java
@Test
public void Test(){
    // java对象转JSON 对象
    Person person = new Person(1, "小智哥");
    // 1 创建GSON对象实例
    Gson gson = new Gson();
    // 2 调用toJson方法将java对象转换成为JSON对象
    String json = gson.toJson(person);
    System.out.println(json);
    // JSON对象转java对象
    Person person1 = gson.fromJson(json, person.getClass());
    System.out.println(person1);
}
```





## List 和 json 的互转  

```java
@Test
public void Test2(){
    // List 和 json 的互转
    List<Person> list = new ArrayList<>();
    list.add(new Person(1,"小智哥"));
    // 创建gson对象
    Gson gson = new Gson();
    // 调用方法
    String listToJsonString = gson.toJson(list);
    System.out.println(listToJsonString);

    // json字符串转list集合
    List<Person> person = gson.fromJson(listToJsonString, new PersonListType().getType());
    System.out.println(person.get(0));
}
```



**说明**：如果放入的是泛型中的类型，那么就会报异常

![image-20201226160939155](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201226160939155.png)

![image-20201226160921058](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201226160921058.png)

**解决方法**

继承 com.google.gson.reflect包下的TypeToken类就行了，里面传一个和List一样的泛型即可，ArrayList也可以

```java
public class PersonListType extends TypeToken<ArrayList<Person>> {
}
```



## map 和 json 的互转

```java
@Test
public void Test3(){
    // Map转JSON对象
    Map<Integer,Person> map = new HashMap<Integer,Person>();
    map.put(1,new Person(1,"小智哥"));
    Gson gson = new Gson();
    String Json = gson.toJson(map);
    System.out.println(Json);
    // JSON对象转Map，和转list的方式一样
    Map personMap = gson.fromJson(Json, new PersonMapType().getType());
    System.out.println(personMap.get(1));
}
```



## 集合转换优化

**说明**：我们可以用创建一个类的继承TypeToken的方式解决类型转换的问题，每转换一次就要创建一个类，不太好

我们可以使用匿名内部类来解决类型转换问题

**具体实现**	-	创建一个TypeToken类的内部类

```java
Map personMap = gson.fromJson(Json, new TypeToken<HashMap<Integer,Person>>(){}.getType());
```









# AJAX请求

## 什么是AJAX请求

AJAX 即“Asynchronous Javascript And XML”（异步 JavaScript 和 XML） ， 是指一种创建交互式网页应用的网页开发
技术。
ajax 是一种浏览器通过 js 异步发起请求， 局部更新页面的技术。
Ajax 请求的局部更新， 浏览器地址栏不会发生变化
局部更新不会舍弃原来页面的内容  

**异步和同步的理解**

同步：要等上面的代码执行完毕才能执行下面的

异步：只要服务器发生数据来我就执行，不用等上面的代码结束



# 原生的AJAX实例

![image-20201226165614941](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201226165614941.png)

| 方法                         | 描述                                                         |
| ---------------------------- | ------------------------------------------------------------ |
| open(*method*,*url*,*async*) | 规定请求的类型、URL 以及是否异步处理请求。 method：请求的类型；GET 或 POST  *url*：文件在服务器上的位置  *async*：true（异步）或 false（同步） |
| send(*string*)               | 将请求发送到服务器。 *string*：仅用于 POST 请求              |

| 属性               | 描述                                                         |
| ------------------ | ------------------------------------------------------------ |
| onreadystatechange | 存储函数（或函数名），每当 readyState 属性改变时，就会调用该函数。 |
| readyState         | 存有 XMLHttpRequest 的状态。从 0 到 4 发生变化。 0: 请求未初始化  1: 服务器连接已建立  2: 请求已接收  3: 请求处理中  4: 请求已完成，且响应已就绪 |
| status             | 200: "OK" 404: 未找到页面                                    |



**代码实现**

AjaxServlet程序

```java
public class AjaxServlet extends BaseServlet {
    protected void javaScriptAjax(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("AJAX请求过来了");
        Person person = new Person(1, "小智");
        Gson gson = new Gson();
        String json = gson.toJson(person);
        // 发送给客户端
        resp.getWriter().write(json);
    }
}
```

ajax.html页面

```javascript
 <script type="text/javascript">
        // 使用js语言向服务器中的AjaxServlet程序发起请求
        function ajaxRequest() {
//              1、我们首先要创建XMLHttpRequest 
            var xmlHttpRequest = new XMLHttpRequest();
            /*
                2、调用open方法设置请求参数
                    第一个参数是提交的方式GET或POST
                    第二个参数是url，请求访问的地址
                    第三个参数是是否异步，true表示异步，false表示同步
            */
            xmlHttpRequest.open("GET", "http://localhost:8080/JSON_AJAX_i18n/ajaxServlet?action=javaScriptAjax",true);
            //     4、在send方法前绑定onreadystatechange事件，处理请求完成后的操作。
            xmlHttpRequest.onreadystatechange = function () {
                // 当 readyState 等于 4 且状态为 200 时，表示响应已就绪：
                if (xmlHttpRequest.readyState == 4 && xmlHttpRequest.status == 200) {
                    // 获取服务器发送过来的数据，并转换成为Json对象
                    var jsonObj = JSON.parse(xmlHttpRequest.responseText);
               // 把响应的数据显示在页面上
                    document.getElementById("div01").innerHTML = "编号： " + jsonObj.id + " , 姓名： " + jsonObj.name;
                }
            }

//              3、调用send方法发送请求
            xmlHttpRequest.send()
        }
    </script>
```





# jQuery 中的 AJAX 请求  

## $.ajax 方法

url			表示请求的地址

type 		表示请求的类型 GET 或 POST 请求

data		 表示发送给服务器的数据 格式有两种：

 		一： name=value&name=value
 	
 		二： {key:value} 请求成功， 响应的回调函数 响应的数据类型 常用的数据类型有： text 表示纯文本

success  		请求成功，响应的回调函数

dataType 		响应的数据类型有：text 纯文本、json 表示 json 对象、xml 表示 xml 数据（被json取代了）

```javascript
 // ajax请求
        $("#ajaxBtn").click(function () {
            $.ajax({
   url: "http://localhost:8080/JSON_AJAX_i18n/ajaxServlet",
   data: { action:"javaScriptAjax"},
   type: "GET",
   success: function (data) {
      // alert("服务器返回的类型是：" + data);
      $("#msg").html("编号： " + data.id + " , 姓名： " + data.name);
   },
   dataType: "json"
});
        });
```



## $.get 方法和$.post

$.get 方法和$.post 方法运用方式是一样的，不一样的是请求的方式不同
url 请求的 url 地址
data 发送的数据
callback 成功的回调函数
type 返回的数据类型

```java
// ajax--get请求
        $("#getBtn").click(function () {
$.ajax({
   url: "http://localhost:8080/JSON_AJAX_i18n/ajaxServlet",
   data: {action: "javaScriptAjax"},
   success : function (data) {
      $("#msg").html("编号： " + data.id + " , 姓名： " + data.name);
   },
   dataType: "json"
});
        });
```



## $.getJSON 方法

url 请求的 url 地址
data 发送给服务器的数据
callback 成功的回调函数  

```javascript
// ajax--getJson请求
        $("#getJSONBtn").click(function () {
            $.getJSON("http://localhost:8080/JSON_AJAX_i18n/ajaxServlet","action=javaScriptAjax",
      function (data) {
         $("#msg").html("编号： " + data.id + " , 姓名： " + data.name);
});
        });
```





## serialize()方法

表单序列化 serialize()
serialize()可以把表单中所有表单项的内容都获取到， 并以 name=value&name=value 的形式进行拼接。  

```javascript
 $("#submit").click(function () {
          // 把参数序列化
          $.getJSON("http://localhost:8080/JSON_AJAX_i18n/ajaxServlet", "action=javaScriptAjax&" +
$("#form01").serialize(), function (data) {
                  $("#msg").html("编号： " + data.id + " , 姓名： " + data.name);
              });
      });
```









# i18n 国际化（了解内容）  

## 什么是 i18n 国际化？

 国际化（Internationalization） 指的是同一个网站可以支持多种不同的语言， 以方便不同国家， 不同语种的用户访问。
 关于国际化我们想到的最简单的方案就是为不同的国家创建不同的网站， 比如苹果公司， 他的英文官网是：
http://www.apple.com 而中国官网是 http://www.apple.com/cn
 苹果公司这种方案并不适合全部公司， 而我们希望相同的一个网站， 而不同人访问的时候可以根据用户所在的区域显示
不同的语言文字， 而网站的布局样式等不发生改变。
 于是就有了我们说的国际化， 国际化总的来说就是同一个网站不同国家的人来访问可以显示出不同的语言。 但实际上这
种需求并不强烈， 一般真的有国际化需求的公司， 主流采用的依然是苹果公司的那种方案， 为不同的国家创建不同的页
面。 所以国际化的内容我们了解一下即可。
 国际化的英文 Internationalization， 但是由于拼写过长， 老外想了一个简单的写法叫做 I18N， 代表的是 Internationalization
这个单词， 以 I 开头， 以 N 结尾， 而中间是 18 个字母， 所以简写为 I18N。 以后我们说 I18N 和国际化是一个意思。  



# 国际化相关要素介绍 

![image-20201226175650413](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201226175650413.png)







# 国际化资源 properties 测试 

配置两个语言的配置文件：  

i18n_zh_CN.properties

```properties
username=用户名
password=密码
sex=性别
age=年龄
regist=注册
boy=男
girl=女
email=邮箱
reset=重置
submit=提交
```

i18n_en_US.properties

```properties
username=username
password=password
sex=sex
age=age
regist=regist
boy=boy
email=email
girl=girl
reset=reset
submit=submit
```

![image-20201226185826742](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201226185826742.png)



国际化测试代码

```java
@Test
    public void Test(){
//        // 获取系统默认语言
//        Locale aDefault = Locale.getDefault();
//        System.out.println(aDefault);
//        // 获取所有国家语言
//        for (Locale availableLocale:Locale.getAvailableLocales()){
//            System.out.println(availableLocale);
//        }
        // 获取中文，中国常量的Locale对象
        Locale china = Locale.CHINA;
        System.out.println(china);
        // 获取英文，美国常量的Locale对象
        Locale us = Locale.US;
        System.out.println(us);
    }
```





# 通过请求头国际化页面  

```javascript
<body>
<%
   // 从请求头重获取Locale信息
   Locale china = request.getLocale();
   // 获取数据包（根据指定的baseNmae和Locale读取 语言信息）
   ResourceBundle i18n = ResourceBundle.getBundle("i18n", china);
%>

   <a href="">中文</a>|
   <a href="">english</a>
   <center>
      <h1><%= i18n.getString("regist") %></h1>
      <table>
      <form>
         <tr>
            <td><%= i18n.getString("username") %></td>
            <td><input name="username" type="text" /></td>
         </tr>
         <tr>
            <td><%= i18n.getString("password") %></td>
            <td><input type="password" /></td>
         </tr>
         <tr>
            <td><%= i18n.getString("sex") %></td>
            <td><input type="radio" /><%= i18n.getString("boy") %>
               <input type="radio" /><%= i18n.getString("girl") %></td>
         </tr>
         <tr>
            <td><%= i18n.getString("email") %></td>
            <td><input type="text" /></td>
         </tr>
         <tr>
            <td colspan="2" align="center">
            <input type="reset" value="<%= i18n.getString("reset") %>" />&nbsp;&nbsp;
            <input type="submit" value="<%= i18n.getString("submit") %>" /></td>
         </tr>
         </form>
      </table>
      <br /> <br /> <br /> <br />
   </center>
   国际化测试：
   <br /> 1、访问页面，通过浏览器设置，请求头信息确定国际化语言。
   <br /> 2、通过左上角，手动切换语言
</body>
</html>
```



# 通过显示的选择语言类型进行国际化  

对Locale对象进行判断

```java
<body>
<%
   Locale locale = null;
   // 得到请求的参数
   String country = request.getParameter("country");
   // 进行判断
   if ("cn".equals(country)){
      locale = Locale.CHINA;
   } else if ("us".equals(country)){
      locale = Locale.US;
   } else {
      locale = request.getLocale();
   }
   ResourceBundle i18n = ResourceBundle.getBundle("i18n", locale);
%>
```





# *JSTL 标签库实现国际化  

<fmt:setLocale value="" />						设置 Locale 信息
<fmt:setBundle basename=""/>			    设置 baseName
<fmt:message key="" />  						  输出指定 key 的国际化信息



**代码实现**

```html
<body>

<%--1 使用标签设置 Locale 信息--%>
<fmt:setLocale value="${param.locale}" />
<%--2 使用标签设置 baseName--%>
<fmt:setBundle basename="i18n"/>

   <a href="i18n_fmt.jsp?locale=zn_CN">中文</a>|
   <a href="i18n_fmt.jsp?locale=en_US">english</a>
   <center>
      <h1><fmt:message key="regist"/></h1>
      <table>
      <form>
         <tr>
            <td><fmt:message key="username"/></td>
            <td><input name="username" type="text" /></td>
         </tr>
         <tr>
            <td><fmt:message key="password"/></td>
            <td><input type="password" /></td>
         </tr>
         <tr>
            <td><fmt:message key="sex"/></td>
            <td><input type="radio" /><fmt:message key="boy"/>
               <input type="radio" /><fmt:message key="girl"/></td>
         </tr>
         <tr>
            <td><fmt:message key="email"/></td>
            <td><input type="text" /></td>
         </tr>
         <tr>
            <td colspan="2" align="center">
            <input type="reset" value="<fmt:message key="reset"/>" />&nbsp;&nbsp;
            <input type="submit" value="<fmt:message key="submit"/>" /></td>
         </tr>
         </form>
      </table>
      <br /> <br /> <br /> <br />
   </center>
</body>
```