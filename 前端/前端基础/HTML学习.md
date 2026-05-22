# 一、HTML概述

HTML全称叫超文本标记语言(HyperText Markup Language)，是一种用来告知浏览器如何组织页面的*标记语言*。它通过标签来显示不同的内容。html文件以`.html`结尾



**Html文档结构**

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>
	
</body>
</html>
```

**说明**：

1.  `<!DOCTYPE html>`

     声明文档类型。早期的 HTML（大约 1991-1992 年）文档类型声明类似于链接，规定了 HTML 页面必须遵从的良好规则，能自动检测错误和其他有用的东西。文档类型使用类似于这样：

    ```html
    <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
    ```

    Copy to Clipboard

    文档类型是一个历史遗留问题，需要包含它才能使其他东西正常工作。现在，只需要知道`<!DOCTYPE html>`是最短的有效文档声明！

2.  `<html></html>`: `html` 元素。这个元素包裹了页面中所有的内容，有时被称为根元素。

3.  `<head></head>`: `head`元素。这个元素是一个容器，它包含了所有你想包含在 HTML 页面中但**不在 HTML 页面中显示**的内容。这些内容包括你想在搜索结果中出现的关键字和页面描述、CSS 样式、字符集声明等等。以后的章节中会学到更多相关的内容。

4.  `<meta charset="utf-8">`: `meta` 元素。这个元素代表了不能由其他 HTML 元相关元素表示的元数据，比如 charset` 属性将你的文档的字符集设置为 UTF-8，其中包括绝大多数人类书面语言的大多数字符。有了这个设置，页面现在可以处理它可能包含的任何文本内容。没有理由不对它进行设置，它可以帮助避免以后的一些问题。

5.  `<title></title>`: `title` 元素。这设置了页面的标题，也就是出现在该页面加载的浏览器标签中的内容。当页面被加入书签时，页面标题也被用来描述该页面。

6.  `<body></body>`: `body` 元素。包含了你访问页面时*所有*显示在页面上的内容，包含文本、图片、视频、游戏、可播放音频轨道等等。







# 二、基本标签

## 2. 1 Html元信息

用来描述HTML文档信息的就是Html元信息，它可以告诉浏览器网站或网页的信息，可以帮助搜索引擎更快的找到我们的网站，元信息的声明是放在`head`标签中的

```html
<head>
    <meta charset="UTF-8">
    <title>标签</title>
</head>
```

**title**

网页的标题

![image-20230330112655528](D:\学习笔记\img\img01\image-20230330112655528.png)

标题的内容会出现在浏览器的标签上



**meta**

网页的元信息，用来描述网页的标签

![image-20230330113054430](D:\学习笔记\img\img01\image-20230330112655528.png)

如上图，可以看到关于菜鸟教程的描述信息，而这些描述信息正是写在了`meta`标签中的

![image-20230330153249117](D:\学习笔记\img\img01\image-20230330153249117.png)

常见的`meta`写法

```html
<meta name="description" content="xxxx">	<!--描述信息-->
```









## 2.2 标签语法

```html
<!--title标签里面写这个网页的标题-->
标题小实验

<!-- ①标签不能交叉嵌套 -->
正确：<div><span>早安</span></div>
错误：
<hr />

<!-- ②标签必须正确关闭 -->
<!-- i.文本内容的标签： -->
正确：<div>早安</div>
错误：
<hr />

<!-- ii.没文本内容的标签： -->
正确：<br />
错误：
<hr />

<!-- ③属性必须值，属性值必须加引号 -->
正确：<font color="blue">早安，尚硅谷</font>
错误：
错误：
<hr />

<!-- ④注释不能嵌套 -->
正确：<!-- 注释内容 --> <br/>
错误：<!--<!--错误的注释-->-->
<hr />
```



## 2.3 常用标签

### 1 font标签

```html
<!--
    font标签：修改字体
        color属性修改字体颜色
        size属性修改字体大小，7是最大的
        face属性修改字体的样式
-->
<font color="#00ffff" size="7" face="楷体">智哥最帅</font>
```



### 2 特殊字符

```html
<!--
    特殊字符：想要将标签显示出来，所以就需要特殊字符来实现这一功能
            特殊符号前面都要加&
-->
    &lt;br&gt;表示换行    <!--显示出来的内容就是<br>-->
    <!--在html中单纯的空格或者tab键和回车键都只能是一个空格，如果需要显示多个空格就需要nbsp;符号去实现-->
    我是一个&nbsp;&nbsp;&nbsp;&nbsp;tab键    <!--特殊符号表示的是四个空格-->

    <!--
        标题标签：类似world文档里面的标题
        h1是最大的
        h6是最小的
    -->
    <h1>标题</h1>
    <h2>标题</h2>
    <h3>标题</h3>
    <h4>标题</h4>
    <h5>标题</h5>
    <h6>标题</h6>
```



### 3 超链接

```html
<!--
    超链接：a标签是超链接
        href属性设置连接地址
        target属性设置那个目标跳转
            常用的参数:①_self：当前页面跳转
                     ②_blank：打开新的页面跳转
                     ③也可以往里面放入iframe窗口的名字，跳转到iframe窗口里面
		通过在href中赋值对应的标签的id值可以跳转到对应的标签所在的位置
-->

<a href="localhost:8080" target="_blank">跳转新的页面</a>     <!--打开新的页面跳转-->
<br/>   <!--换行-->
<a href="localhost:8080" target="_self">当前页面跳转</a>      <!--当前页面跳转-->
<a href="#a">跳转到对应id的标签</a>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<a id="a" href="#">a标签</a>
```





### 4 列表标签

```html
<!--
    列表标签
        ul标签(无序列表)：没序列号的列表
            type属性可以除去列表前面的符号
        ol标签(序列表)：序列号的列表
-->
<ul type="none">
    <li>小智</li>
    <li>好</li>
    <li>帅</li>
    <li>哦</li>
</ul>
```





### 5 label标签

label标签可以绑定元素，当点击label标签时会触发绑定元素

```html
<!--
	label -> 当点击学习文字时，多选框就会被勾选，label属性的位置可以随意放置，但是
		for绑定的就是对应元素的id
-->
<input type="checkbox" value="study" id="demo"><label for="demo">学习</label>
```





### 6 H5语义化标签

为了实现语义化标记，HTML 提供了明确这些区段的专用标签，例如：

-   header：头部
-   nav：导航栏
-   main：主x内容
-   aside：侧边栏
-   footer：页脚





### 7 其他标签

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Test其他标签</title>
</head>
<body>
    <!--
        其他的标签
            div标签     默认一行，就是占一行
            span标签    它的长度的封装数据的长度，比如说这个名字是个字，那么它的长度就是3
            p段落标签    默认在段落的上方或者下方空出一行来（如果已有就不用再空）
    -->
    <div>我是小智</div>
    <span>小智是我</span>
    <span>小智还是我</span>
    <p>我是第二个小智</p>
</body>
</html>
```





# 三、表单和表格

## 3.1 表单

### 3.1.1  表单标签

```HTML
<!--
        form    表单标签：用于收集用户信息的所元素集合，然后把信息发送给服务器
        input标签
            name属性：可以对其进行分组，分组之后只能一个，就是名字相同的是同一个组
            checked属性：
                参数：checked，表示默认中，就是在那里那里就是默认中的状态
            value属性：设置默认值
            type属性：
                参数：①text        文本输入框
                     ②password    密码输入框
                     ③radio       单框
                     ④checked     多
                     ⑤button      钮       value设置默认值
                     ⑥file        文件上传域，可以上传文件
                     ⑦hidden      隐藏域
                     隐藏域的作用：当我们发信息，而这些信息不需要用户参与时，
                     就可以使用隐藏域（提交的同时发送给服务器，给程序用的

        select标签：下拉列表
        option标签：下拉列表的项
            select属性：这个属性在哪那个就是默认值
                参数：select
        testarea标签：多文本输入框
            rows属性：设置高度
            cols属性：设置字符数量
    -->

<!--
    需求：创建一个个人信息注册的表单界面，包含用户名、密码、确认密码、性别（单选）、
    兴趣爱好、国籍、隐藏域、自我评价（多行文本域、重置、提交
-->

<!--
		放入表格中的作用：整齐
	-->

    <form>
        <table align="center">
            <tr>
                <!--用户名-->
                <td>用户名</td>
                <td><input type="text"></td>
            </tr>
            <tr>
                <!--用户密码-->
                <td>用户密码</td>
                <td><input type="password"/></td>
            </tr>
            <tr>
                <!--确认密码-->
                <td>确认密码</td>
                <td><input type="password"/></td>
            </tr>
            <tr>
                <!--性别（单-->
                <td>性别</td>
                <td><input type="radio" checked="checked" name="gender"/>男      <!--默认男的-->
                    <input type="radio" name="gender"/>女
                </td>
            </tr>
            <tr>
                <!--兴趣爱好-->
                <td>兴趣爱好</td>
                <td><input type="checkbox" checked="checked"/>Java      <!--默认Java-->
                    <input type="checkbox"/>JavaScript
                    <input type="checkbox"/>C++
                    <input type="checkbox"/>Python
                </td>
            </tr>
            <tr>
                <!--国籍（下拉列表-->
                <td>国籍</td>
                <td>
                    <select>
                        <option selected="selected">中国</option>     <!--默认中-->
                        <option>美国</option>
                        <option>印度</option>
                        <option>日本</option>
                    </select>
                </td>
            </tr>
            <tr>
                <!--上传文件-->
                <td>上传文件</td>
                <td><input type="file"/></td>
            </tr>
            <tr>
                <!--隐藏域-->
                <td><input type="hidden" /></td>
            </tr>
            <tr>
                <!--自我评价（多行文本域-->
                <td>自我评价</td>
                <td><textarea rows="10" cols="20"> 我喜欢敲代码 </textarea></td>
            </tr>
            <tr>
                <!--重置-->
                <td><input type="reset" value="重置"/></td>
                <!--提交-->
                <td align="center"><input type="submit" value="提交"/></td>
            </tr>
        </table>
    </form>
```



### 3.1.2 表单的提交细节

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Test表单的提交</title>
</head>
<body>
    <!--
        表单的提交
            form标签：
                action属性设置提交给服务器的地址
                method属性设置提交的方式
                    ①get
                        1.浏览器中地址栏中的地址是：action属性+?+请求参数
                            请求参数的格式是：name=value&name=value
                        2.不安全，因为信息显示在了地址栏中
                        3.长度限制
                    ②post：不会显示出来内容在
                        1.浏览器中地址栏中的地址是：action属性
                        2.相对于get来说是安全的
                        3.理论上长度无限制

        表单提交的时候数据没发送给服务器的种情况：
            ①表单项没name属性
            ②单，复（下拉表中的option标签都需要添加value属性，一遍发给服务器
            ③表单项不在form标签中
    -->
    <form action="http://localhost:8080" method="get">
        <table align="center">
            <tr>
                <!--用户名-->
                <td>用户名</td>
                <td><input type="text" name="username"></td>
            </tr>
            <tr>
                <!--用户密码-->
                <td>用户密码</td>
                <td><input type="password" name="userpow"/></td>
            </tr>
            <tr>
                <!--性别（单-->
                <td>性别</td>
                <td><input type="radio" checked="checked" name="gender" value="boy"/>男      <!--默认男的-->
                    <input type="radio" name="gender" value="girl"/>女
                </td>
            </tr>
            <tr>
                <!--兴趣爱好-->
                <td>兴趣爱好</td>
                <td><input type="checkbox" checked="checked" name="lang" value="Java"/>Java      <!--默认Java-->
                    <input type="checkbox" name="js" value="lang"/>JavaScript
                    <input type="checkbox" name="c++" value="lang"/>C++
                    <input type="checkbox" name="PY" value="lang"/>Python
                </td>
            </tr>
            <tr>
                <!--国籍（下拉列表-->
                <td>国籍</td>
                <td>
                    <select name="contory">
                        <option selected="selected" value="cn">中国</option>     <!--默认中-->
                        <option value="mg">美国</option>
                        <option value="yd">印度</option>
                        <option value="jp">日本</option>
                    </select>
                </td>
            </tr>
            <tr>
                <!--隐藏域-->
                <td><input type="hidden" name="yinchangyu"/></td>
            </tr>
            <tr>
                <!--自我评价（多行文本域-->
                <td>自我评价</td>
                <td><textarea rows="10" cols="20" name="text"> 我喜欢敲代码 </textarea></td>
            </tr>
            <tr>
                <!--重置-->
                <td><input type="reset" value="重置"/></td>
                <!--提交-->
                <td align="center"><input type="submit" value="提交"/></td>
            </tr>
        </table>
    </form>
</body>
</html>
```





## 3.2 表格

### 3.2.1 表格标签

```html
<!--
    table   表格标签
        width   设置表格的宽度
        height  设置表格的高度
        align   设置表格相对于页面的对齐方式
            参数：居中center     右right      左left
        border  给每个单元格添加边框
        cellspacing 设置单元格和表格的间距
    td  单元格标签
        align
        colspan属性：跨行
        rowspan属性：跨列
    tr  行标签
    b   加粗标签
    th  表头标签 -> 居中对齐，加粗

	caption
-->
<table align="right" width="500" height="500" border="1" cellspacing="0">

    <tr align="center">
        <td><b>1.1</b></td>
        <td >1.2</td>
        <td>1.3</td>
    </tr>

    <tr align="center">
        <td>2.1</td>
        <td>2.2</td>
        <td>2.3</td>
    </tr>
    <tr align="center">
        <td>3.1</td>
        <td>3.2</td>
        <td>3.3</td>
    </tr>
    <tr>
        <th>4.1</th>
    </tr>
</table>
```



### 3.2.2 表格的跨行跨列

```html
<!--
    td标签的两个属性的用法
        跨行和跨列和Excel中的合并单元格类似
        colspan属性：跨行，参数是要跨几个单元格
        rowspan属性：跨列
-->
<table border="1">
    <tr>
        <td colspan="2">1.1</td>
        <td>1.2</td>
    </tr>
    <tr>
        <td rowspan="2">2.1</td>
        <td>2.2</td>
        <td>2.3</td>
    </tr>
    <tr>
        <td>3.1</td>
        <td>3.2</td>
    </tr>
    <tr>
        <td>4.1</td>
        <td>4.2</td>
        <td>4.3</td>
    </tr>
</table>
```





# 四、多媒体与嵌入

## 4.1 图片

### 4.1.1 img标签

```html
<!--
    img标签(单标签)：在html页面上显示图片
        src属性设置图片的路径
        width设置图片的宽度
        height设置图片的高度
        border设置相框
        alt设置当指定路径找不到图片的时候显示的内容

    html中的路径
        相对路径：
            .       表示当前目录
            ..      表示上一级目录
            文件名   表示当前目录，相当于./，./可以省略
        绝对路径：http:IP:part/工程名/资源路径
        和java的绝对路径是不一样的
-->

<img src="../img/28.jpg" width="250" height="300" border="1" alt="找不到该照片"/>
<img src="../img/2.jpg" width="250" height="300" border="1"/>
<img src="../img/3.jpg" width="250" height="300" border="1"/>
<img src="../img/4.jpg" width="250" height="300" border="1"/>
```



### 4.1.2 矢量图形



### 4.1.3 响应式图片







## 4.2 视屏和音频

使用` <video>` 和 `<audio>`标签可以将视屏和音频放入到网页中

**浏览器支持的媒体文件**

**MP3** (音频格式) 和 **MP4/H.264** (视频格式) ，MP4以`.mp4`结尾，H.264以`.webm`结尾





### 4.2.1 视屏

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>
    <!-- 
        video
            src：视屏资源地址
            controls：表示打开控件，可以控制视屏的播放
            autoplay：自动播放，即使视屏还没加载完成
            loop：使音频或视屏循环播放
            muted：媒体播放时静音
            poster：属性指向一个图像的URL，图像会在视屏播放前显示
            preload：缓冲较大的文件，有三个值可选
                none：不缓冲
                auto：页面加载后缓存媒体文件
                metadata：仅缓冲文件的元数据（也就是描述文件的数据）


        使用方式：
            第一种：直接在标签中写入属性来指定资源和控件
                <video src="" controls>
            第二种：在便签内写入 <source> 标签来指定资源地址和媒体类型
                <video controls>
                    <source src="" type="video/mp4">
                </video>
                    
        视屏无法播放提示（现在的浏览器一般都支持）：
            使用第一种方式的情况下，会直接显示出标签内的内容做为提示
            使用第二种方式的情况下，会尝试多个 source ，如果都无法播放，最后就会显示最后一行的内容作为提示
            如果标签内没有提示的内容，则不会显示
     -->
    <video src="./video/video.mp4" width="400" height="400" controls>
        <p>您的浏览器不支持HTML5视屏</p>
    </video>

    <video controls width="400" height="400" preload="auto"
        poster="./img/1.jpg">
        <source src="./video/1.mp4" type="video/mp4">
        <p>您的浏览器不支持HTML5视屏</p>
    </video>
    
</body>
</html>
```



### 4.2.2 音频

**基本使用**

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>
    
    <!-- 
        audio
            controls：开启控件
            
        使用方式：
            和 video 一样，可以使用 src属性 和 内嵌resource属性 两种方式

        注：
            audio标签没有 width 和 height 属性，所以不能设置宽度和高度
            也没有 poster 属性，因为没有视觉部件
            不能自动播放，所以也没有  autoplay属性

        重新播放媒体：
            调用 load函数 可以重置媒体，利用这个方法可以实现下一首功能
     -->

    <audio controls id="my-media-element">
        <source src="./audio/群星 - 非诚勿扰 - 开场曲 - 吹牛 B 了.mp3">
    </audio><br>
    <button id="next-btn" onclick="nextBtn()">下一首</button>

    <script>
        function nextBtn(){
            var mediaElem = document.getElementById('my-media-element');
            mediaElem.setAttribute('src', './audio/Andrew Belize _ Adaptiv _ Menno - Butterfly (Extended Mix).mp3');
            mediaElem.load();
        }
    </script>
</body>
</html>
```



### 4.2.3 显示文本字幕

在很多音乐网页中，我们可以看到歌词，滑动音轨，歌词也跟着变化，很多视屏中有字幕。要想做到这种效果，我们就需要设定在什么时候显示什么内容，而`WebVTT`文件就是用来设置对应时间内显示的内容的，`WebVTT`以 .vtt 结尾。

```vtt
WEBVTT

1
00:00:22.230 --> 00:00:24.606
第一段字幕

2
00:00:30.739 --> 00:00:34.074
第二段

  ...
```

**如何在网页中使用？**

1.  首先需要编写`.vtt`文件，格式按照上面的即可
2.  用`<track>`的`src属性`来链接`.vtt`文件，放在`<audio>`或`<video>`标签中，同时需要放在所有`<source>`标签之后
    -   `kind`属性：指明是哪一种类型，如 subtitles、captions、descriptions。
    -   `srclang`属性：指明编写内容的语言

```html
<video controls width="400" height="400" preload="auto">
    <source src="./video/1.mp4" type="video/mp4">
    <track label="English" kind="subtitles" src="./text/zimu.vtt" srclang="en">
</video>
```







## 4.3 Iframe

```html
<!--
    iframe框架标签（内嵌窗口：在html页面中开辟一个窗口，窗口中显示另外一个html页面
        src     设置打开页面的路径
        width   设置窗口的宽度
        height  设置窗口的高度
        name    设置窗口的名字

    ifranme和a标签一起用：
        先设置ifranme窗口的名字，名字不能为中文，
        然后在a标签target属性放入它iframe窗口的名字，
        当我们点击超链接的时候就是跳转到这个窗口里面
-->
<iframe src="1-font标签.html" width="500" height="500" name="abc"></iframe>
    <!--创建一个超链接菜单-->
    <ul>
        <li><a href="2-特殊字符.html" target="abc">2-特殊字符.html</a></li>
        <li><a href="5-列表标签.html" target="abc">5-列表标签.html</a></li>
        <li><a href="7-表格的跨行跨列.html" target="abc">7-表格的跨行跨列.html</a></li>
</ul>
```











