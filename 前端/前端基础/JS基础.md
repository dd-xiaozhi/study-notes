# 一、概述







# 二、基本语法









# 三、对象







# 四、常用类库





# 五、异步





# 六、Web API











# JS基础

## JS语言概述

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>HelloJS</title>
    <!--
        JS是弱类型，Java是强类型
            弱类型：定义了还是可以改变
            强类型：定义了就不能改变
        特点：
            1. 交互性（它可以做的就是信息的动态交互
            2. 安全性（不允许直接访问本地硬盘
            3. 跨平台性（只要是可以解释 JS 的浏览器都可以执行，和平台无关

        JS和html结合方式
            ①在head或body标签中，用script标签写
            ②独立创建一个js文件，然后引入js文件

        注意：两种方式不能同时使用，如果标签里面已经引入了js文件，那么就不能在标签里面写代码了，
             这个时候可以创建一个新的script标签，在新的标签里面写代码

        代码的执行方式：和java的一样，顺序结构执行

        alert()函数：弹出一个警告框
    -->
    <script type="text/javascript " src="Hello.js"></script>

    <script type="text/javascript">
        alert("Hello，JavaScript")
    </script>
</head>
<body>
<script type="text/javascript">
    alert("我是在body里面实验的代码")
</script>

</body>
</html>
```



## 变量

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Test变量</title>
    <script type="text/javascript" >
        /*
            变量
                要注意的点：
                    ①和java不同的是，js定义变量不用先定义类型
                    ②和java一样是后面的值覆盖前面的
                    ③所类型的默认值为undefined，不是为null，null值是要显示赋值的


                JS 的变量类型：
                    数值类型： number
                    字符串类型： string
                    对象类型： object
                    布尔类型： boolean
                    函数类型： function
                typeof()方法：返回变量的数据类型
                JS里特殊的值：
                    undefined   未定义，所有 js 变量未赋于初始值的时候，默认值都是 undefined.
                    null        空值
                    NaN         全称是：Not a Number。非数字。非数值。

                JS定义变量的格式：
                    var 变量名;
                    var 变量名 = 值;
					
         */

        var number1 = 1;
        alert(number1)    // 默认值是undefined

        var number1 = 12;
        alert(number1)

        //测试特殊的值
        var a = null;
        var b = undefined;
        var str = "小智";
        var num = 1;
            alert(num * str)    // 这时候就会显示NaN，非数字

        //typeof()方法
        var of = 1;
        alert(typeof(of))

    </script>
</head>
<body>

</body>
</html>
```



## 运算符

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Test运算符</title>
    <script type="text/javascript" >
        /*
            运算符
                比较运算符
                    其他的和java都一样，下面的两个是和java不一样的
                    ①等于： == 等于是简单的做字面值的比较,两者的类型可以是不用的，就是说字符串类型的可							以和数字类型的比较，底层会做类型转化
                    ②全等于： === 除了做字面值的比较之外，还会比较两个变量的数据类型

                js逻辑运算符：逻辑非!、逻辑与&&、逻辑或||。
                                所谓短路操作就是，当&&的第一个操作数的值是false时，
                                直接返回第一个操作数的值，不再对第二个操作数进行计算；

                    &&的两种情况：
                        ①当表达式全为真的时候，返回最后一个表达式的值
                        ②当表达式中一个为假的时候，返回第一个为假的表达式的值
                    ||的两种情况：
                        ①当表达式全部为假的时候，返回最后一个表达式的值
                        ②只要一个表达式为真，就会返回第一个为真的表达式的值

                    总结：&&全部为真，取最后一个，||全部为假，取最后一个
                         &&一个为假，取第一个，||一个为真，取第一个

                在 JavaScript 语言中，所有的变量，都可以做为一个 boolean 类型的变量去使用。
                    0 、null、 undefined、””(空串) 都认为是 false；

         */

        //==等于

        // var number = 1;
        // var str = "1";
        // alert(number == str )   //true
        //
        // //===全等于
        // alert(number === str)   //true
        //


        // //逻辑运算符
        var a = 0;      //0也是false
        var b = "";     //空字符串也是false
        var e = false;
        var c = true;
        var d = 1;

        /*
            &&
            第一种情况：表达式中有一个为假，取第一个为假的表达式的值返回
            第二种情况：表达式中所都为真，返会第一个表达式的值
         */
        // alert(d && b && a)
        // alert(c && d)

        /*
            ||
            第一种情况：表达式中有一个为真，取第一个为真的表达式的值返回
            第二种情况：表达式中没一个为真，取最后一个表达式的值返回
         */
        //alert(a || b || e)
        alert(e || d || c)
    </script>
</head>
```



## 数组

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Test数组</title>
    <script type="text/javascript" >

        /*
            数组:
                格式：
                    var 数组名 = [];   // 空数组
                    var 数组名 = [1 , ’abc’ , true];   // 定义数组同时赋值元素

                扩容方式：以最大的索引扩容，比如说我创建了一个空的数组number[]，
                        给索引值为10的赋值，那么这个时候数组会自动扩容到10，其他没赋值的就是默认值undefined
                特点：js的数组和java的数组不一样，它不用在创建的时候规定长度，而是在创建后再进行扩容
         */

        var numbers = [true,5,5];
        numbers[1] = "小智";
        alert(numbers[100])
        for (var i = 0;i < numbers.length;i++){
            alert(numbers[i])
        }

    </script>
</head>
<body>

</body>
</html>
```



## 函数

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Test函数</title>
    <script type="text/javascript">
        /*
            定义函数的两种方式
                第一种：可以用function关键字来定义函数
                    格式：
                        function 函数名(形参列表){
                            函数体
                        }
                特点：因为js的变量是不确定的，所以多个形参的时候不用写类型，直接用形参名(a1,a2)

                第二种：用变量的方式
                    格式：
                        var 函数名 = function(形参列表) {
                            函数体
                         }

            注意：JS不允许重载，重载了也没用，和java一样，return结束方法
         */


        //第一种方式
        function info(){        //无参
            alert("无参的函数")
        }
        function info2(a1,a2){   //有参有返回值
            return a1 + a2;
        }

        alert(info(1,"小猪佩奇"));
        alert(info2(1,"小猪佩奇"));

        //第二种方式
        var init = function(){
            alert("无参的函数")
        }
        init();

    </script>
</head>
<body>

</body>
</html>
```



## 函数的arguments

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Test隐形函数</title>
    <script type="text/javascript" >
        /*
            函数的 arguments 隐形参数（只在 function 函数内
            arguments相当于是javase中方法里面的可变形参一样
            public void demo(int...numbers){}

            隐形函数的操作和java中的数组是类似的
         */
        // function fun() {
        //     alert(arguments[1])
        //     alert(arguments[2])
        //     alert(arguments[3])
        //     //遍历
        //     for (var i = 0; i < arguments.length; i++) {
        //         alert(arguments[i])
        //     }
        // }
        // fun(1,2,3,4,);      //传入的参数会被当成是隐藏函数，遍历的话是全部遍历的

        // 需求：要求 编写 一个函数。用于计算所有参数相加的和并返回
        function fun2() {
            var sum = 0;
            for (var i = 0; i < arguments.length; i++) {
                if (typeof(arguments[i]) == "number"){    //加一个判断，如果是数字类型就要
                    sum += arguments[i]
                }
            }
            alert(sum)
        }
        fun2(1,2,3,4,5,"小智",6,7,8,9,10)     //在js中字符串进行计算，也是进行字符串拼接

    </script>
</head>
<body>

</body>
</html>
```

## 自定义对象

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Test自定义对象</title>
    <script type="text/javascript">

        /*
            自定义对象：
                ①Object形式
                    格式：
                        var 变量名 = new Object();     //空对象
                        变量名.属性名 = 值;            // 定义属性
                        变量名.函数名 = function(){}  // 定义函数

                ②{}花括号形式
                    格式：var 变量名 = {
                        属性名：值,          // 定义一个属性
                        属性名：值,          // 定义一个属性
                        函数名：function(){} //定义函数
                    };

                    注意：属性和方法之间是逗号隔开的，最后一个不用写逗号

            两种方式的调用方法都是：变量名.属性名/函数名()
            函数里面是可以用this关键字的，和java的一样
         */

        //①Object形式
        var obj = new Object();
        obj.name = "小智";
        obj.init = function () {
            alert(this.name)
        }

        obj.init();     //调用方法


        //{}形式
        var a = {
            name:"小智",
            age:18,
            init:function () {
                alert(this.name +  "永远" + this.age)
            }
        }
        a.init()
    </script>
</head>
<body>

</body>
</html>
```



# JS事件

## 事件的概述

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title></title>
</head>
<body>

<!--
    事件注册
        js 中的事件
            什么是事件？事件是电脑输入设备与页面进行交互的响应。我们称之为事件。
        常用的事件：
            onload 加载完成事件： 页面加载完成之后，常用于做页面 js 代码初始化操作
            onclick 单击事件： 常用于钮的点击响应操作。
            onblur 失去焦点事件： 常用用于输入框失去焦点后验证其输入内容是否合法。
            onchange 内容发生改变事件： 常用于下拉列表和输入框内容发生改变后操作
            onsubmit 表单提交事件： 常用于表单提交前，验证所表单项是否合法。


     事件的注册又分为静态注册和动态注册两种：
        什么是事件的注册（绑定？
        其实就是告诉浏览器，当事件响应后要执行哪些操作代码，叫事件注册或事件绑定。

        静态注册事件：通过 html 标签的事件属性直接赋于事件响应后的代码，这种方式我们叫静态注册。
                注意：可以在标签里面写function的代码，但是不建议，代码的可读性太差，所以一般都是在script标签里面写的

        动态注册事件：是指先通过 js 代码得到标签的 dom 对象，然后再通过 dom 对象.事件名 = function(){} 这种形式赋于事件
        响应后的代码，叫动态注册。

        动态注册基本步骤：
            1、获取标签对象    window是顶层对象，其他的都是他的字对象
            2、标签对象.事件名 = fucntion(){}

-->
</body>
</html>
```



## onload事件

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>TestOnload</title>
    <script type="text/javascript">
        //静态注册
        function fun() {
            alert("静态注册")
        };

        //动态注册
        window.onload = function () {
            alert("动态注册")
        }
    </script>
</head>
<body onload="fun()">
    <!--
        onload事件是在浏览器加载完后执行的代码，
        可以理解成是java中的代码块，创建对象的时候就调用了

        静态注册是在body标签上写的，动态注册是在js代码里面，如果静态注册和动态注册同时存在，
        那么只会执行静态注册，所以要注意两者的使用
    -->
</body>
</html>
```



## onclick事件

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Test单击事件</title>
    <script type="text/javascript">
        function fun() {
            alert("我是静态注册的钮1")
        }

        window.onload = function () {
            //1.获取标签对象
            var but02 = document.getElementById("but02");   //id要用引号引起来
            /*
                get         获取
                Element     元素（就是标签
                By          通过。。。
                Id          id
                getElementById(id)：通过id获取元素
            */
            but02.onclick = function () {
                alert("我是动态注册的钮2")
            }
        }
    </script>
</head>
<body>

    <!--
        单击事件：
            点击相应，比如一些按钮或者一些网页的操作给出相应的反应
    -->
    <button onclick="fun()">钮1</button>    <!--静态注册-->
    <button id="but02">钮2</button>

</body>
</html>
```



## onblur事件

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>TestOnblur</title>
    <script type="text/javascript">

        /*
            onblur失去焦点事件
                失去焦点事件：光标移出外面点击就会执行函数

            console是控制台对象，是由JavaScript提供的，专门用于浏览器的控制台输出的，用于测试使用
            log()和java的print()一样，是打印方法


         */
        //静态注册
        function fun() {
            console.log("静态注册")
        }

        //动态注册
        window.onblur = function () {
            //1.获得标签对象
            var elementById = document.getElementById("doc01");
            //2.标签对象.事件名
            elementById.onblur = function () {
                console.log("动态注册")
            }
        }
    </script>
</head>
<body>
    <form>
        用户名<input type="text" onblur="fun();"/><br>
        用户密码<input type="password" id="doc01"/>
    </form>
</body>
</html>
```



## onchange事件

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>TestOnchange</title>
    <script type="text/javascript">
        /*
            onchange内容发生改变事件
                如果内容发生改变，那么就会调用函数

            注意：如果内容没发生改变，那么是不会调用函数的，比如下拉列表如果你不换项，那么就不会调用函数
         */
        //静态注册
        function fun() {
            alert("女神发生了变化")
        }
        //动态注册
        window.onchange = function () {
            //1.得到标签对象
            var elementById = document.getElementById("doc01");
            //2.标签对象.事件名
            elementById.onchange = function () {
               alert("男生发生了变化")
            }
        }
    </script>
</head>
<body>
    <select onchange="fun()">       <!--静态注册-->
        <option>--女神--</option>
        <option>小苍</option>
        <option>海椒</option>
        <option>刘亦菲</option>
    </select>
    <select id="doc01">                        <!--动态注册-->
        <option>--男神--</option>
        <option>小智</option>
        <option>狗栋</option>
        <option>叼林</option>
    </select>
</body>
</html>
```



## onsubmit事件

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>TestOnsubmit</title>
    <script type="text/javascript" >

        /*
            onsubmit提交表事件
                如果返回给onsubmit的参数为false，那么发送不成功，如果是true就发送成功
         */

        //静态注册
        function fun() {
            alert("静态注册---您的输入不合法")
            return false;
        }

        //动态注册
        window.onsubmit = function () {
            //1.得到标签对象
            var elementById = document.getElementById("doc01");
            //2.标签对象.事件名
            elementById.onsubmit = function () {
                alert("动态注册---您的输入不合法")
                return false;
            }
        }
    </script>
</head>
<body>

    <form action="http://localhost8080 " method="get"  onsubmit="return fun()">
        用户名<input type="text" value="静态注册"/>
        <input type="submit" value="校验">
    </form>
    <form action="http://localhost8080 " method="get" id="doc01">
        用户名<input type="text" value="动态注册"/>
        <input type="submit" value="校验"/>
    </form>
</body>
</html>
```



# document对象

## document对象的概述

![image-20201128130202727](C:\Users\猛男\AppData\Roaming\Typora\typora-user-images\image-20201128130202727.png)

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>title</title>
    <script type="text/javascript">

	

        /*
           DOM模型
               DOM 全称是 Document Object Model 文档对象模型
                   大白话，就是把文档中的标签，属性，文本，转换成为对象来管理。
                   那么 它们是如何实现把标签，属性，文本转换成为对象来管理呢。学习的重点

               什么是对象化，将代码封装到对象里面

               Document 对象的理解：
                   第一点：Document 它管理了所的 HTML 文档内容。
                   第二点：document 它是一种树结构的文档。有层级关系。
                       最常见的就是          html标签
                                   head标签          body标签
                   第点：它让我们把所的标签 都 对象化
                   第四点：我们可以通过 document 访问所的标签对象。

           Document对象方法：
               document.getElementById(elementId)
               通过标签的 id 属性查找标签 dom 对象，elementId 是标签的 id 属性值

               document.getElementsByName(elementName)：返回一个dom对象的集合
               通过标签的 name 属性查找标签 dom 对象，elementName 标签的 name 属性值

               document.getElementsByTagName(tagname)：返回dom对象集合
               通过标签名查找标签 dom 对象。tagname 是标签名

               document.createElement( tagName)
               方法，通过给定的标签名，创建一个标签对象。tagName 是要创建的标签名

               document.createTextNode("文本内容")：创建一个文本节点
               字符串也是属于文本节点的一部分

           private String id;	id 属性
           private String tagName; 表示标签名
           private Dom parentNode; 父亲节电
           private List<Dom> children; 孩子结点
           private String innerHTML; 起始标签和结束标签中间的内容

           节点常用的属性和方法：
                节点就是标签对象

                方法：
                    通过具体的元素节点调用
                    getElementsByTagName()  方法，获取当前节点的指定标签名孩子节点

                    appendChild( oChildNode ) 方法，可以添加一个子节点，oChildNode 是要添加的孩子节点

                属性：
                childNodes
                属性，获取当前节点的所子节点
                firstChild
                属性，获取当前节点的第一个子节点
                lastChild
                属性，获取当前节点的最后一个子节点
                parentNode
                属性，获取当前节点的父节点
                nextSibling
                属性，获取当前节点的下一个节点
                previousSibling
                属性，获取当前节点的上一个节点
                className
                用于获取或设置标签的 class 属性值
                innerHTML
                属性，表示获取/设置起始标签和结束标签中的内容
                innerText
                属性，表示获取/设置起始标签和结束标签中的文本
        */
    </script>
</head>
<body>

</body>
</html>
```



## getElementById函数

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>TestGetElementByid</title>
    <script type="text/javascript">

        /*
            getElementById函数:通过id属性获得标签属性
                innerHTML属性：表示起始标签和结束标签中的内容，这个属性可读可写

            patt对象：正则表达式
                test()方法：用于测试某个字符是否匹配我的规则，是就返回true，否则返回false
         */

        function fun() {

            var usernameObj = document.getElementById("nameuser");
            var usernameText = usernameObj.value;
            var patt = /^\w{5,12}$/;

            var spanobj = document.getElementById("span01");
            // innerHTML表示起始标签和结束标签中的内容，这个属性可读可写
            // var text = spanobj.innerHTML;
            // spanobj.innerHTML = "小智真帅";

            if (patt.test(usernameText)){
                // alert("您的输入不合法")
                spanobj.innerHTML="<img src=\"right.png\" height=\"18\" width=\"18\">";
            }else{
                // alert("您的输入不合法")
                spanobj.innerHTML="<img src=\"wrong.png\" height=\"18\" width=\"18\">";
            }
        }

    </script>
</head>
<body>
    用户名<input type="text" id="nameuser" value="zxcv"/>
    <span id="span01" style="color: red">

    </span>
    <button onclick="fun()">校验</button>
</body>
</html>
```



## getElenmentByName函数

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>TestGetElementByName</title>
    <script type="text/javascript">
        /*
            getElemenByName函数:通过name属性获取标签对象
                // document.getElementsByName();是根据指定的name 属性查询返回多个标签对象集合
                这个集合操作和数组一样，集合的每个元素都是 dom对象

            // checked 表示复框的选中状态。如果选中是true，不中是false
            // checked 这个属性可读，可写


         */

        // 全
        function butfunAll() {
            // 需求：让所有复选框都选中
            // 这个集合中的元素顺序是他们在html 页面中从上到下的顺序
            // checked 表示复框的中状态。如果中是true，不中是false
            // checked 这个属性可读，可写
            //1.通过name属性获得标签对象集合
            var getName = document.getElementsByName("hoby");
            //2.通过checked属性修改中状态
            //遍历
            for (var i = 0; i < getName.length; i++){
                getName[i].checked = true;
            }
        }
        // 全不
        function butfunNot() {
            var getName = document.getElementsByName("hoby");

            for (var i = 0; i < getName.length; i++) {
                getName[i].checked = false;
            }
        }
        // 反
        function bufunfx() {
            var getName = document.getElementsByName("hoby");
            for (var i = 0; i < getName.length; i++) {
                getName[i].checked = !getName[i].checked;
            }
        }
    </script>
</head>
<body>
    兴趣爱好
    <input type="checkbox" name="hoby" value="java" checked="checked"/>
    <input type="checkbox" name="hoby" value="c++"/>
    <input type="checkbox" name="hoby" value="c"/>
    <input type="checkbox" name="hoby" value="python"/>
    <br>
    <button onclick="butfunAll()">全</button>
    <button onclick="butfunNot()">全不</button>
    <button onclick="bufunfx()">反</button>
</body>
</html>
```



## getElenmentTagName函数

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Test</title>
    <script type="text/javascript">
        /*
            getElementsByTagName：通过标签名获得标签对象
                也是得到一个集合，操作和Name属性的方法类似

         */
        //全
        function butfunAll() {
            // 1.得到标签对象
            var inputobj = document.getElementsByTagName("input");
            // 通过checked属性修改项状态
            for (var i = 0; i < inputobj.length; i++) {
                inputobj[i].checked = true;
            }
        }

    </script>
</head>
<body>
兴趣爱好
    <input type="checkbox" name="hoby" value="java" checked="checked"/>
    <input type="checkbox" name="hoby" value="c++"/>
    <input type="checkbox" name="hoby" value="c"/>
    <input type="checkbox" name="hoby" value="python"/>
    <br>
    <button onclick="butfunAll()">全</button>
</body>
</html>
```



## 正则表达式

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Test正则表达式</title>
    <script type="text/javascript">
        /*
            正则表达式：          -- 检索的时候要想到正则表达式来进行判断
                写法：①new RegExg()    创建对象的形式
                     ②/正则表达式主体/修饰符(可)

                注意：①空格也是字符
                     ②是大小写之分的
                     ③包含是包含这个情况在里面的，比如说包含一个a，那就是说3个a也是成立的

         */
        /*
        //是否带小数
        function    isDecimal(strValue )  {
            var  objRegExp= /^\d+\.\d+$/;
            return  objRegExp.test(strValue);
        }

        //校验是否中文名称组成
        function ischina(str) {
            var reg=/^[\u4E00-\u9FA5]{2,4}$/;   //定义验证表达式
            return reg.test(str);     //进行验证
        }

        //校验是否全由8位数字组成
        function isStudentNo(str) {
            var reg=/^[0-9]{8}$/;   //定义验证表达式
            return reg.test(str);     //进行验证
        }

        //校验电话码格式
        function isTelCode(str) {
            var reg= /^((0\d{2,3}-\d{7,8})|(1[3584]\d{9}))$/;
            return reg.test(str);
        }

        //校验邮件地址是否合法
        function IsEmail(str) {
            var reg=/^\w+@[a-zA-Z0-9]{2,10}(?:\.[a-z]{2,4}){1,3}$/;
            return reg.test(str);
        }
        */

//代码实现
        //判断字符串中是否包含这个字符,顺序要一样
        //var patt = /de/;

        //判断字符串中是包含a,c,d，顺序无要求
        //var patt = /[acd]/;

        //字符串中是否包含a到z的字母
        //var patt = /[a-z]/;

        //字符串中是否包含A到Z的字母
        //var patt = /[A-Z]/;

        //字符串中是否包含0-9的数字
        //var patt2 = /[0-9]/;

        //字符串中是否  包含 字母，数字，下划线
        //var patt2 = /\w/;

        //字符串中是否  包含  至少一个a
        //var patt2 = /a+/;

        //字符串中至少  包含  “零个” 或 多个 a
        //var patt2 = /a*/;

        //字符串中是否  包含  一个或者零个a
        //var patt2 = /a?/;

        //字符串中是否包含3个连续的a
        //var patt2 = /a{3}/;

        //字符串中是否 包含 至少3个连续的a ，最多5个连续的a
        //var patt2 = /a{3,5}/;

        //字符串中是否 包含 至少3个连续的a
        //var patt2 = /a{3,}/;

        //字符串中是否以a字母结尾
        //var patt2 = /a$/;

        //字符串中是否以a字母开头
        //var patt2 = /^a/

        //要求：字符串中是否*包含* 至少3个连续的a
        //var patt2 = /a{3,}/;

        //要求:字符串，从头到尾都必须完全匹配
        //var patt3 = /^a{3,}$/;  //以a开头和结尾，三个连续或多个连续的a，这是一段连续的字符串
                                //里面的内容全部是a才行
        //alert(patt.test("abcde"))
        //alert(patt2.test("aeasfaaa23a"))
        //alert(patt3.test("aaaabba"))

        // 要求：比对密码是数字、字母、下划线
        var patt = /^\w{5,12}$/;
        var str = "xiaozhi520_"
        alert(patt.test(str))
    </script>
</head>
<body>

</body>
</html>
```



## createElement函数

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Test</title>
    <script type="text/javascript">

        /*
            createElement函数：创建标签对象

         */

        window.onload = function () {
            var divobj = document.createElement("div");     //在内存中
            //通过innerHTML属性写入内容
            var textContent = document.createTextNode("智哥真帅");  //创建一个文本节点
            var appendChild = divobj.appendChild(textContent);      //添加一个子节点
            document.body.appendChild(divobj);                  //添加一个子节点
        }

    </script>
</head>
<body>
</body>
</html>
```





# 原型对象

在JavaScript中万物皆对象，**Object和Function都既是函数也是对象。**

1、每一个函数对象都有一个**prototype属性（显性原型属性）**，但是普通对象是没有的；

　 prototype下面又有个construetor，指向这个函数。

2、每个对象都有一个名为**`_proto_`()（隐形原型属性）**的内部属性，指向它所对应的构造函数的原型对象，原型链基于`_proto_`;

**总结**：原型对象就是构造函数创建出来的对象，里面放的是一个个的属性，通过`_protp_`可以拿到这些属性值，也就是`_protp_`指向了函数创建时候的那个对象原型







# 容器

## 1	数组

几乎和java的一样

### ①数组的基本使用

```js
console.log('-----------创建数组-----------');
// 方式一：直接创建
var arr1 = [1, 2, 3, 5]
// 方式二：使用new关键字
var arr2 = new Array(1, 2, 3, 4, 5)

console.log('-----------访问数组-----------');
console.log(arr1[1]);
// 修改数组
arr1[1] = 30
console.log(arr1[1]);

console.log('-----------数组属性和方法-----------');
// 属性返回数组长度
console.log(arr1.length);
// 访问第一个元素
console.log(arr1[0]);
// 访问最后一个元素
console.log(arr1[arr1.length - 1]);

console.log('-----------遍历数组元素-----------');
// 方式一：for循环
for (let index = 0; index < arr1.length; index++) {
    console.log(arr1[index]);
}
// 方式二：foreach(函数)
arr1.forEach(element => {
    console.log(element);
});

console.log('-----------添加数组元素(push)-----------');
// 使用push方法添加元素 --> 元素从数组后面插入
arr1.push(123)
// 使用下角标添加元素
arr1[arr1.length] = 333
console.log(arr1);

console.log('-----------吐出数组元素(pop)-----------');
// pop方法从数组中删除最后一个元素
var a = arr1.pop()  // 接收吐出的值
console.log(arr1);
console.log(a);

console.log('-----------位移元素(shift/unshift)-----------');
// shift --> 移除第一个元素
arr1.shift()
console.log(arr1);
// unshift --> 方法（在开头）向数组添加新元素
arr1.unshift(2000)
console.log(arr1);

console.log('-----------删除元素(delete关键字)-----------');
delete arr1[0]
console.log(arr1);  // 使用delete会在数组上留下空洞

console.log('-----------拼接数组(splice)-----------');
/* 
            splice
                第一个参数（2）定义了应添加新元素的位置（拼接）。
                第二个参数（2）定义应删除多少元素。
                其余参数（“Lemon”，“Kiwi”）定义要添加的新元素。
                splice() 方法返回一个包含已删除项的数组
        */
var aa = arr1.splice(2, 2, 'Lemon', 'Kiwi')
console.log(arr1, aa);

console.log('-----------删除数组(splice)-----------');
arr1.splice(0, 1)
console.log(arr1);
```



### ②数组排序

**sort() 方法是最强大的数组方法之一。**

```js
console.log('---------sort方法进行排序--------');
var arr = [1, 23, 543, 15, 9000, 21]
// 旧数组和新数组都是已经排好序了的
console.log(arr.sort());    // 返回一个新数组
console.log(arr);

console.log('---------reverse方法进行反转数组--------');
arr.reverse()
console.log(arr);

console.log('---------自定义排序(数字排序)--------');
/* 
  比值函数：
      当 sort() 函数比较两个值时，会将值发送到比较函数，并根据所返回的值（负、零或正值）对这些值进行排序   
*/
arr.sort((a, b) => a-b)
console.log(arr);
arr.sort((a, b) => b - a)
console.log(arr);

console.log('---------随机顺序排序数组--------');
// 原理还是比值函数
arr.sort((a, b) => 0.5 - Math.random())
console.log(arr);

console.log('---------Math.max.apply查找最高值--------');
console.log(Math.max.apply(null, arr));

console.log('---------Math.max.apply查找最低值--------');
console.log(Math.min.apply(null, arr));
```



### ③数组迭代

```js
        console.log('------------foreach-----------');
        /* 
        foreach()：
            接收一个回调函数
                参数一：项目值
                参数二：项目索引
                参数三：数组本身
        */
        var arr = [1, 234, 32, 453, 221]
        arr.forEach((value, index, array) => {
            console.log(value, index, array);
        });

        console.log('------------map-----------');
        /* 
        map()方法：
        	对每个数组元素进行处理，然后放入一个新的数组中
        	不会改变原来数组
        */
       var arrMap = arr.map((value, index, array) => {
           // 每个元素都乘于2
           return value * 2
        })
        console.log(arr, arrMap);   // 两个函数对比
        
        
        console.log('------------filter-----------');
        /* 
            filter()：
                过滤掉我们不需要的元素，然后放入到一个新的数组中
                不会改变原来数组
        */
        var arrFilter = arr.filter((value, index, array) => {
            return value > 50
        })
        console.log(arr, arrFilter);

        console.log('------------reduce-----------');
        /* 
            reduce：
                遍历每个数组元素，将他们进行总和操作
                不会改变原来原来数组
        */
        var arrReduce = arr.reduce((total, value, index, array) => {
            return total + value
        })
        console.log(arr, arrReduce);

        console.log('------------every-----------');
        /* 
            every：方法检查所有数组值是否通过测试    
        */
        var arrEvery = arr.every((value, index, array) => {
            // 所有元素是否瞒住大于50，没有就返回false
            return value > 50
        })
        console.log(arr, arrEvery);
```



## 2	Map

几乎和java的一样

| new Map() | 创建新的 Map 对象。            |
| --------- | ------------------------------ |
| set()     | 为 Map 对象中的键设置值。      |
| get()     | 获取 Map 对象中键的值。        |
| entries() | 返回 Map 对象中键/值对的数组。 |
| keys()    | 返回 Map 对象中键的数组。      |
| values()  | 返回 Map 对象中值的数组。      |

**其他方法**

| clear()   | 删除 Map 中的所有元素。   |
| --------- | ------------------------- |
| delete()  | 删除由键指定的元素。      |
| has()     | 如果键存在，则返回 true。 |
| forEach() | 为每个键/值对调用回调。   |

```js
    <script>
        var map = new Map()
        map.set('a', 1)
        map.set('b', 2)
        map.set('c', 3)
        map.set('d', 4)

        // 遍历集合
        for (let x of map) {
            console.log(x[0]);
        }

        map.forEach((a, b) => {
            console.log(a, b);
        })

        // size：集合中元素的个数
        console.log(map.size);

        // has：判断集合中是否存在该元素
        console.log(map.has('a'));
    </script>
```





## 3	Set

set不会存在相同的元素

| new Set() | 创建新的 Set 对象。       |
| --------- | ------------------------- |
| add()     | 向 Set 添加新元素。       |
| clear()   | 从 Set 中删除所有元素。   |
| delete()  | 删除由其值指定的元素。    |
| entries() | 返回 Set 对象中值的数组。 |
| has()     | 如果值存在则返回 true。   |
| forEach() | 为每个元素调用回调。      |
| keys()    | 返回 Set 对象中值的数组。 |
| values()  | 与 keys() 相同。          |
| size      | 返回元素计数。            |















# JS格式转换

## 1	科学计数法转换

### ①与空字符直接相加

```js
// 第一种 --> 与空字符相加
let eformat = 1.34e5
let number = "" + eformat   
// console.log(number + 1);    // 此时的number是字符串，要将字符串转成基本数据类型
console.log( Number(number) + 1);   // 此时的number才是数值
```



### ②调用toString方法

```js
// 第二种 --> 调用toString方法
let number2 = eformat.toString()
// 此时的number是字符串，要将字符串转成基本数据类型
// console.log(number2 + 1); 
console.log(parseInt(number2) + 1); // 此时的number才是数值
```



### ③字符串转Number类型

1.  Number方法
2.  parseInt()方法
3.  parseFloat()方法





# 时间

## 1	创建时间

```js
console.log("--------当前时间--------");
console.log(new Date());

console.log("--------指定时间--------");
// 6个参数 --> 年月日时分秒
console.log(new Date(2001, 5, 6, 3, 30, 30));

console.log("--------从日期字符串中创建时间--------");
const str = "October 13, 2014 11:13:00"
console.log(new Date(str));

console.log("--------将日期变为毫秒--------");
// 从1970年算起
console.log(new Date(100000343043));

console.log("--------将事件输出为字符串--------");
const dateStr = new Date()
// 可以不调用toString方法，log输出的时候会自动帮我们调用
console.log(dateStr.toString());
// 方法将日期转换为 UTC 字符串（一种日期显示标准）
console.log(dateStr.toUTCString());
// 变成更简便的时间格式
console.log(dateStr.toDateString());

console.log("--------指定时间--------");
```



## 2	时间格式

```js
console.log('----------ISO日期--------');
// 年月日
console.log(new Date("2018-02-19"));
// 年和月
console.log(new Date("2018-5"));
// 只有年
console.log(new Date("2018"));
// 完整ISO日期
console.log(new Date("2018-02-19T12:00:00"));

console.log('----------短日期--------');
console.log(new Date("02/19/2018"));

console.log('----------长日期--------');
console.log(new Date("Feb 19 2019"));
```



## 3	日期获取方法

```js
const d = new Date();
console.log('----------获取自1970年1月1日以来的毫秒数---------');
console.log(d.getTime());

console.log('----------获取年份---------');
console.log(d.getFullYear());

console.log('----------获取月份---------');
console.log(d.getMonth());

console.log('----------获取日---------');
console.log(d.getDate());

console.log('----------获取小时---------');
console.log(d.getHours());

console.log('----------获取分钟数---------');
console.log(d.getMinutes());

console.log('----------获取秒数---------');
console.log(d.getSeconds());

console.log('----------获取毫秒数---------');
console.log(d.getMilliseconds());

console.log('----------获取星期名---------');
console.log(d.getDay());
```





# 字符串

## 1	字符串方法

```js
        const str = "asdfdbgsdgsd"
        console.log("----------字符串长度--------");
        console.log(str.length);

        console.log("---------indexOf()方法返回首次出现的索引---------");
        console.log(str.indexOf('f'));

        console.log("---------lastIndexOf()方法返回最后一次出现的索引---------");
        console.log(str.lastIndexOf("s"));

        console.log("---------search方法搜索特定值的字符串，并返回匹配的位置---------");
        console.log(str.search("fdb"));

        console.log("---------slice方法返回瘦子出现的索引---------");
        console.log(str.slice(3, 7));

        console.log("---------subString和slice一样，但是不能接受负索引---------");
        console.log(str.substring(3, 7));

        console.log("---------substr方法---------");
        // substr类似slice，不同的是第二个参数是提取的长度
        console.log(str.substr(3, 7));
        
        console.log("---------replace方法---------");
        // 替换字符串，默认只替换首个匹配的，然后返回一个新的字符串
        console.log(str.replace("asd", "abc"));
        
        console.log("---------concat---------");
        // concat连接连个或多个字符串
        var text1 = "Hello";
        var text2 = "World";
        console.log(text1.concat(" ", text2));
        
        console.log("---------trim方法---------");
        // 去除字符串两端的空白符
        var test = "   asdfs  "
        console.log(test.trim());

        console.log("---------charAt方法---------");
        // 返回字符串中指定下标的字符串
        console.log(str.charAt(0));

        console.log("---------split方法---------");
        // 将字符串转换为数组
        var test = "a, b, c, d, e"
        console.log(test.split(","));
        console.log(test.split(" "));
        console.log(test.split("|"));
```









