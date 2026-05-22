# 一、CSS概述

CSS的概述：CSS 是「层叠样式表单」。是用于(增强)控制网页样式并允许将样式信息与网页内容分离的一种标记性语言。HTML负责将东西放到浏览器，CSS就是负责对HTML的东西进行修饰和排版，举个例子：我们要建设一栋房子，首先我们需要购买材料，接着由工程师来设计房子，然后让工人来实施，而材料就相当于时HTML，工程师就是CSS，工人就是我们的浏览器，通过CSS来对网页进行设计，接着交给浏览器来进行渲染

CSS的语法规则：

-   选择器：浏览器根据“择器”决定受CSS 样式影响的HTML 元素（标签。

-   属性 (property) 是你要改变的样式名，并且每个属性都一个值。

    属性和值被冒号分开，并由花括号包围，这样就组成了一个完整的样式声明（declaration，例如：p {color: blue}

    多个声明：如果要定义不止一个声明，则需要用分号将每个声明分开。

    虽然最后一条声明的最后可以不加分号(但尽量在每条声明的末尾都加上分号)

-   ```
    选择器 {
    	属性: ;
    }
    ```

    

CSS 注释：

```css
/*注释内容*/
```



## 如何使用CSS？

1.  在标签style属性上设置，修改标签样式
2.  在head标签中用style定义想要的CSS样式
3.  创建css文件，通过link标签引用

```html
第一种方式：

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>TestCss</title>
</head>
<body>

    <!--
        第一种方式：在标签style属性上设置，修改标签样式

    -->
    <!--需求 1：分别定义两个 div、span 标签，分别修改每个 div 标签的样式为：边框 1 个像素，实线，红色。-->
    <div style="border: 1px solid red " >我是小智</div>
    <span style="border: 1px solid brown">我也是小智</span>

</body>
</html>

第二种方式：

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>TestCSS2</title>
    <!--
    第二种方式：
        在head标签中用style定义想要的CSS样式
    -->
    <!--需求 1：分别定义两个 div、span 标签，分别修改每个 div 标签的样式为：边框 1 个像素，实线，红色。-->
    <style type="text/css">
        div{
            border: 5px solid red;
        }
        span{
            border: 5px solid black;
        }
    </style>
</head>
<body>
    <div>我是小智</div>
    <span>我也是小智</span>
</body>
</html>


第三种方式：

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Test3</title>
    <link rel="stylesheet" type="text/css" href="css样式.css"/>
</head>
<body>
    <!--
        第种方式：变成css文件，通过link标签引用
    -->
    <div>我是小智</div>
    <span>我也是小智</span>
</body>
</html>
```







# 二、常用样式

## 2.1 背景

### `background-color`

设置元素背景颜色



### `background-image:url(image);`

将图片设置为背景



### `background-repeat: no-repeat;` 

设置背景图片是否重复及如何重复，默认平铺满。（重要）

-   `no-repeat`不要平铺；
-   `repeat-x`横向平铺；
-   `repeat-y`纵向平铺。



### `background-position:center top;` 

设置背景图片在当前容器中的位置。



### `background-attachment:scroll;`

设置背景图片是否跟着滚动条一起移动。 属性值可以是：`scroll`（与fixed属性相反，默认属性）、`fixed`（背景就会被固定住，不会被滚动条滚走）。



## 2.2 边框

```html
<!--
	红色 1 像素实线边框
	border：1px solid red;
-->
```



## 2.3 文本

```html
<!--
	字体颜色color：red；
        颜色可以写颜色名如：black, blue, red, green 等
        颜色也可以写 rgb 值和十六进制表示值：如 rgb(255,0,0)，#00F6DE，如果写十六进制值必须加#

	字体样式：
         color：#FF0000；字体颜色红色
		font-size：20px; 字体大小
	
	文本居中：
         text-align: center;

-->
```





## 2.4 表单

```html
<!--
	
-->
```



## 2.5 表格

```html
<!--
	表格细线
        table {
        	border: 1px solid black; /*设置边框*/ 
	   	    border-collapse: collapse; /*将边框合并*/
        }
        td,th {
        	border: 1px solid black; /*设置边框*/
        }
-->
```



## 其他

```html
<!--
    宽度
        width:19px;
        宽度可以写像素值：19px； 也可以写百分比值：20%；

   高度
        height:20px;
        高度可以写像素值：19px； 也可以写百分比值：20%；

    列表去除修饰
        ul {
            list-style: none;
        }
	
	超连接去下划线
		text-decoration: none;
-->
```









# 三、CSS选择器

## 1、选择器

```html

    <!--
        总结：
            ①标签名择器             不同标签不同样式
              id择器                同样标签不同样式
              .class择器(类择器)   不同标签同样式
              组合择器               几个择器使用同样的样式
    -->

标签名选择器：

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Test择器</title>
    <!--
        标签名择器的格式：不同标签通不同的样式
            标签名{
                属性名:值;
            }

        作用：决定哪些标签被动择样式
    -->
    <!--
        需求 1：在所 div 标签上修改字体颜色为蓝色，字体大小 30 个像素。
        边框为 1 像素黄色实线。并且修改所 span 标签的字体颜色为黄色，字体大小 20 个像素。边框为 5 像素蓝色虚线
    -->
    <style type="text/css">
        div{
            color: blue ;
            font-size: 30px;
            border: 1px black dotted ;
        }
        span{
            color: yellow ;
            font-size: 20px;
            border: 5px blue dashed ;
        }
    </style>
</head>
<body>
    <span>我是小智哥</span>
    <div>我是小智</div>
</body>
</html>




id选择器：


<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Test择器</title>
    <!--
        id择器
            #id 属性值{
                属性:值;
            }
        使用方法：在标签加id属性，参数为择器的属性值
    -->
    <!--
        需求 1：分别定义两个 div 标签，
            第一个 div 标签定义 id 为 id001 ，然后根据 id 属性定义 css 样式修改字体颜色为蓝色，
            字体大小 30 个像素。边框为 1 像素黄色实线。
            第二个 div 标签定义 id 为 id002 ，然后根据 id  属性定义 css 样式 修改的字体颜色为红色，
            字体大小 20 个像素。边框为 5 像素蓝色点线。
    -->
    <style type="text/css">
        #id001{
            color: blue;
            font-size: 30px;
            border: 1px yellow solid;
        }
        #id002{
            color: red;
            font-size: 20px;
            border: 5px blue dotted;
        }
    </style>
</head>
<body>
    <div id="id001">我是小智哥</div>
    <div id="id002">我是小智</div>
</body>
</html>


.class选择器(类选择器):


<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Test择器</title>
    <!--
        class择器（类择器
        .class 属性值{
            属性:值;
        }
        和id择器的用法类似
        class 类型择器，可以通过 class 属性效的择性地去使用这个样式。
    -->
    <!--
        需求 1：修改 class 属性值为 class01 的 span 或 div 标签，
        字体颜色为蓝色，字体大小 30 个像素。边框为 1 像素黄色实线。
        需求 2：修改 class 属性值为 class02 的 div 标签，字体颜色为灰色，
        字体大小 26 个像素。边框为 1 像素红色实线。
    -->
    <style type="text/css">
        .class01 {
            color: blue;
            font-size: 30px;
            border: 1px yellow solid ;
        }
        .class02 {
            color: brown;
            font-size: 26px;
            border: 1px red solid;
        }
    </style>
</head>
<body>
    <div class="class01">我是大帅比</div>
    <span class="class01">小智是大帅哥</span>
    <hr/>
    <div class="class02">纯真的小智</div>
    <span class="class02">点小可爱的小智</span>
</body>
</html>



组合选择器：


<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Test择器</title>
    <!--
        组合择器
            择器1，择器2，择器n{
                属性:值;
            }
        作用：组合择器可以让多个择器共用同一个 css 样式代码。

    -->
    <!--
        需求 1：修改 class="class01" 的div 标签 和 id="id01" 所的 span 标签，
        字体颜色为蓝色，字体大小 20 个像素。边框为 1 像素黄色实线。
    -->
    <style type="text/css">
        #id101,.class01 {
            color: blue;
            font-size: 20px;
            border: 1px yellow solid;
        }
    </style>
</head>
<body>
    <div id="id101">我是小智</div>
    <span class="class01">我是小智的哥哥</span>
</body>
</html>
```



### 通配选择器

*{}



### 交集选择器

所有选择器都要满足才会有效

```html
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
    <style>
        /* 满足是div有又类选择器的元素 */
        div.big {
            border: 1px springgreen solid;
        }
    </style>
</head>
<body>
    <div class="big">
        <div>学习</div>
        <div>学习</div>
    </div>
</body>
</html>
```



### 选择器分组

同组的选择器拥有同样的样式

```html
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
    <style>
        div,span {
            border: 1px springgreen solid;
        }
        /* 这两个标签的样式一样 */
    </style>
</head>
<body>
    <div class="big">
        <div>学习</div>
        <span>努力</span>
    </div>
</body>
</html>
```



### 关系选择器

祖先选择器：祖先 后代					选择被标签包含的所有匹配的元素

父子选择器：父 > 子						选择被它包含的子标签，不包含孙代标签

兄弟选择器:  元素 + 相邻的元素       选择和这个标签同级的元素

所有选择器：元素 ~ 元素下的元素	选择元素下的所有元素

```html
    <style>
        /* 选择.big下的所有div */
        .big div {
            border: 1px solid red;
        }
        /* 选择.big的后代元素中是div的 */
        .big > div{
            border: 1px solid red;
        }
        /* 选择.big相邻的div */
        .big + div {
            border: 1px solid red;
        }
        /* 选择.big之后出现的所有的span */
        .big ~ span {
            border: 1px solid red;
        }
    </style>
</head>
<body>
    <div class="big">
        <div>
            学习
            <div>
                <div>
                    黑色
                </div>
            </div>
        </div>
    </div>
    <div>
        .big相邻的div
    </div>
    <span>我是幸运的</span>
</body>
```



### 伪类选择器

**伪类**：不存在的类，它是特殊的类，描述元素特殊状态

**使用**：元素:伪类

-   **first-child**：始终选择第一个子元素

-   **last-child**：始终选择最后一个子元素

-   **nth-child(n)**：选中第n个子元素，例如选择第三个n就为3

    n的值说明：n：0->	正无穷

    ​				2n或者even：选中偶数元素

    ​				2n+1或者odd：选中奇数元素

-   **first-of-type**：选择相同类型中的第一个

-   **last-of-type**：选择相同类型中的最后一个

-   **a:link**：没访问过的链接

-   **:visited**：访问过的链接，注意只能修改颜色

-   **:hover**：鼠标移入的状态

-   **:active**：鼠标点击的状态

**说明**：:link和:visted只能是a标签才能使用

```html
    <style>
        /* 选中.test下的第一个子元素 */
        .test:first-child{
            border: 1px red solid;
        }
        /* 选中.test下的最后一个子元素 */
        .test:last-child{
            border: 1px red solid;
        }
        /* 选中.test下的第偶数个元素 */
        .test:nth-child(even){
            border: 1px red solid;
        }
        /* 选中.test下的第奇数个元素 */
        .test:nth-child(2n+1){
            border: 1px red solid;
        }
        /* 选中同类型的第一个 */
        p:first-of-type{
            border: 1px red solid;
        }
    </style>
```

其他的选择器可以查看文档学习



### 伪元素

**解释**：不存在的元素，表示页面中特殊并不存在的元素（特殊的位置）

-   ::first-letter：表示第一个字母
-   ::first-line：表示第一行
-   ::selection：表示选中的内容，鼠标选中就会触发
-   ::before：元素起始位置，不能被鼠标选中
-   ::after：元素末位置，不能被鼠标选中

**说明**：before和after必须结合content属性使用

==w3school中都有对应的例子，忘记了可以去查看==





## 2、样式的继承

**说明**：并不是所有样式都会被继承，比如背景相关的，布局相关的

父类的样式被子类所继承





## 3、选择器的权重

**说明**：不同的选择器选择了同一个元素，此时发生冲突，那么会执行那个选择器

内联>id选择器>类型选择器>元素选择器

**得出结论**：一般不会使用内敛的形式改变样式



**权重**：我们使用数字表示，数字越大，优先级越高

-   内联：1000
-   id：100
-   类型选择器：10
-   元素选择器：1
-   通配选择器：0
-   继承：没有优先级

**比较优先级**：多个选择器可以相加，然后进行比较，比如11个类型选择器就是比1个id选择器大，那么就会使用类型选择器，分组的还是单独计算，如果计算结果相同，此时优先使用靠下的样式！

**!important**：最高优先级，用了这个别的选择器就无效了，开发中慎用



# 四、单位

百分比：相对父元素的百分比

em：相对于字体大小来计算，比如font-size:10px ，那么3em = 30px

rem：也是相对于字体大小，不过是相对于根元素，比如html:font-size=10px	5em=50px





# 五、布局

## 文档流（normal flow）

说明：它是最底下的一层，元素默认都在

脱离文档流：元素不在文档流中



**文档流的特点**

-   块元素：独占一行，默认宽度就是内容，垂直排列，比如div
-   行内元素：不会独占一行，只占自身大小，水平排列，宽度由内容撑开，比如span



## 盒子模型（box model）

**说明**：我们的元素其实是一个矩形的盒子，我们所谓的布局就是要摆放盒子

**组成**：内容区（content）、边框（border）、内边距（pading）、外边距（margin）

内容区：宽高控制大小，也就是width和height



### margin

margin就是外边距，可以设置上下左右的外边距

-   margin-top：设置上外边距
-   margin-buttom：设置下外边距
-   margin-left：设置左外边距
-   margin-right：设置右外边距



**设置四个方向的外边距**

margin：1px 2px 3px 4px		这样就是分别对应上下左右为1234个像素的外边距

margin: 1px 2px		上下为1px，左右为2px

margin:auto			水平居中

```html
    <style>
        .big {
            border: 1px red solid;
            width: 400px;
        }
        .big div{
            width: 200px;
            border: springgreen solid 1px;
            text-align: center; /* 文字居中 */
            margin: auto;  /* 左右外边距相同,相当居中 */ 
            padding: 10px;
        }
    </style>
</head>
<body>
    <div class="big">
        <div>学习</div>
        <div>学习</div>
    </div>
</body>
</html>
```

margin可以设置负值，元素会往相反的方向移动



### pading

内边距，用法和margin的差不多



### border

边框，用法和margin差不多



### 盒子模型之水平方向布局

子元素的最大值=7个值相加=子元素的盒子宽度

子元素的最大值必须在父元素的内容区范围内，如果值没填满，那么它将会将元素的外边距设置到父元素内容区的最大值。

利用左右的外边距来进行水平的布局，这种方式要注意**盒子本身要有宽度**，如果没有宽度，是没有效果的

==如果元素的宽度超过了父元素的宽度，那么为了是计算成立，margin会设置成负值的==



### 盒子模型之垂直方向布局

**超出盒子处理**

overfload：

-   visable	默认值
-   hidden	 超出的直接裁剪了
-   scroll	   超过内容就会生成滚动条
-   auto		 根据需要生成滚动条



### 外边距的折叠

垂直方向的相邻元素外边距如果重叠，取两个元素外边框的最大值作为连个元素的间距

**解决折叠的问题**

①不用外边框

②给元素加上边框就会隔开



**行内元素**：可以设置pading、border、margin。垂直方向不影响布局，水平方向会影响。



### 盒子的尺寸设置

```css
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
    <style>
        .box1 {
            width: 100px;
            height: 100px;
            background-color: #bfa;
            padding: 10px;
            border: 10px red solid;
            /* 
                默认情况下，盒子可见框的大小由内容区、内边距、和边框共同决定

                    box-sizing：用来设置盒子尺寸的计算方式（设置width和height的作用）
                        可选值：
                            content-box 默认值，宽度和高度用来设置内容区的大小
                            border-box  宽度和高度用来设置整个盒子的大小
             */
            box-sizing: border-box;
        }
    </style>
</head>
<body>
    
    <div class="box1"></div>

</body>
</html>
```



### 轮廓和阴影和圆角

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
    <style>
        .box1 {
            width: 200px;
            height: 200px;
            background-color: #bfa;
            /* outline: 10px red solid; */
            /*
                outline：用来设置元素的轮廓线，用法和border一样，
                轮廓和边框不同的点就是轮廓不会影响可见框的大小
            */
            box-shadow: 0px 0px 50px rgba(0,0,0, .3);
            /*
                box-shadow  用来设置元素的阴影效果，阴影不会影响页面的布局
                    第一个值    水平偏移量，设置阴影水平位置，正直向右移动，负值向左移动
                    第二个值    垂直偏移量，设置阴影垂直位置，正直向上移动，负值向下移动
                    第三个值    阴影的模糊半径
                    第四个值    阴影颜色
            */
        }
        .box2 {
            width: 200px;
            height: 200px;
            background-color: orange;
            /* border-radius : 用来设置圆角 圆角设置圆的半径大小*/
            /* 还有几个值是可以是设置单边的圆角 */
            /* 
                border-radius 可以分别制定四个角的圆角
             */
            /* border-radius: 10px; */

            /* 将元素设置为一个圆形 */
            border-radius: 50;
        }
    </style>
</head>
<body>
    <div class="box1"></div>
    <span>hello</span>

    <div class="box2"></div>
</body>
</html>
```





## Flex布局

### 1、主轴和交叉轴

**主轴**：控制元素的排列方式，横着排还是竖着排

由`flex-direction`定义，有以下四个值，默认值是`row`

-   `row`：横着排
-   `row-reverse`：横着排，元素位置反转
-   `column`：竖着排列
-   `column-reverse`：竖着排，元素位置反转

注：如果元素总和超出了容器的最大宽度，会重新进行计算，根据元素比列进行重新分配



**交叉轴**：始终和主轴垂直，主轴横向排列，那么它就是纵向的，它可以控制元素的方向，交叉轴在哪，元素从那里开始



**代码示例**

```html
<style>
    .content {
        display: flex;
        /* 改变flex的布局为纵向的 */
        flex-direction: row;
    }

    .a {
        background-color: rgb(12, 239, 239);
        width: 100px;
        height: 100px;
    }
</style>
<body>
     <nav class="content">
        <div class="a">我是1</div>
        <div class="a">我是2</div>
        <div class="a">我是3</div>
     </nav>
</body>
</html>
```





### 2、flex属性

#### flex-wrap

是否实现多行flex容器

-   nowarp(默认值)：当你的元素超出了容器的宽度时，它不会换行，会重新根据容器最大宽度进行计算
-   warp：当元素超出容器宽度，元素会换行排列

```html
<style>
    .content {
        display: flex;
        /* 设置多行flex容器*/
        flex-wrap: wrap;
    }
    .a {
        background-color: rgb(12, 239, 239);
        width: 1000px;
        height: 100px;
    }
</style>
<body>
     <nav class="content">
        <div class="a">我是1</div>
        <div class="a">我是2</div>
        <div class="a">我是3</div>
     </nav>

</body>
```



#### flex-basis

当元素内容超出容器大小时，通过这个属性去进行自动分配以满足不超出容器，在`flex-wrap`设置为`nowarp`的情况下

-   auto：自动分配，显示出元素的全部内容
-   fill：填满



#### flex-grow

元素可以通过这个属性按比例进行空间分配，容器的可用空间会被元素平分，向外延展填充整个容器



#### flex-shrink

和`flex-grow`相反，当元素超出容器范围，元素会向内收缩





### 3、flex简写

Flex` 简写形式允许你把三个数值按这个顺序书写 — `flex-grow`，`flex-shrink`，`flex-basis



### 4、元素间对齐和空间分配

Flexbox 的一个关键特性是能够设置 flex 元素沿主轴方向和交叉轴方向的对齐方式，以及它们之间的空间分配。



#### align-items

设置容器的位置

-   `stretch`
-   `flex-start`：起始线的位置
-   `flex-end`：结束线的位置
-   `center`：起始线和结束线的中间

注：交叉轴的开始位置就是起始线的位置，交叉轴结束的位置是结束线的位置

```html
<style>
    .content {
        display: flex;
        height: 200px;
        flex-flow: row, nowrap;
        /* 位置居中 */
        align-items: center;
    }

    .a {
        background-color: rgb(12, 239, 239);
        width: 100px;
        height: 100px;
    }
</style>
<nav class="content">
    <div class="a">我是1</div>
    <div class="a">我是2</div>
    <div class="a">我是3</div>
</nav>
```

![image-20220725224420179](E:\学习笔记\img\image-20220725224420179.png)



#### justify-content

和align-items相反，它的起始线和主轴的起始线一致，终止线和主轴的终止线一致

-   `stretch`
-   `flex-start`
-   `flex-end`
-   `center`
-   `space-around`
-   `space-between`

![image-20220725225432265](E:\学习笔记\img\image-20220725225432265.png)





## Grid布局











## 浮动

### 简介

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
    <style>
        .box1 {
            width: 200px;
            height: 200px;
            background-color: #bfa;

            /* 
                通过浮动可以使一个元素向其父元素的左侧或右侧移动
                使用float属性来设置元素的浮动
                    可选值：
                        none 默认值，元素不浮动
                        left 元素向左浮动
                        right 元素向右浮动

                        注意：元素设置浮动后，水平布局的等式便不需要强制成立
                            元素设置浮动以后，会完全从文档流中脱离，不会占用文档流的位置，
                            所以元素下边的还在文档流中的元素会自动向上移动
                        
                        浮动的特点：
                            1 浮动元素会完全脱离文档流，不再占据文档流中的位置‘
                            2 设置浮动以后元素会向父元素的左侧或右侧移动，
                            3 浮动元素默认不会从父元素中移出
                            4 浮动元素向左或向右移动时，不会超过它前边的其他浮动元素
                            5 如果浮动元素的上边是一个没有浮动的元素，则浮动元素无法上移
                            6 浮动元素不会超过它上边的浮动的兄弟元素，最多就是和它一样高
             */
             float: left;
        }

        .box2 {
            width: 200px;
            height: 200px;
            background-color: orange;
            float: left;
        }
        .box3 {
            width: 200px;
            height: 200px;
            background-color: yellow;
            float: left;
        }
    </style>
</head>
<body>
    
    <div class="box1"></div>
    <div class="box2"></div>
    <div class="box3"></div>
</body>
</html>
```



### 浮动的特点

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
    <style>
        .box1 {
            width: 100px;
            height: 100px;
            background-color: #bfa;
            /* 
                浮动元素不会盖住文字，文字会自动环绕在浮动元素的周围，
                所以我们就可以利用浮动来设置文字环绕图片的效果
            */
            float: left;
        }
        .box2 {
            background-color: yellowgreen;
            /*
                元素设置浮动以后，将会从文档流脱离，从文档流脱离后，
                元素的一些特点也会发生变化

                脱离文档流的特点：
                    块元素：
                        1 块元素不再独占页面的一行
                        2 脱离文档流以后，块元素的高度和宽度默认都被内容撑开
                    行内元素：
                        行内元素脱离文档流以后会变成块元素，特点和元素一样
                    总结：脱离文档流以后，不需要区分块和行内了
            */
            float: left;
        }
        .box3 {
            background-color: orange;
        }
        .s1 {
            float: left;
            width: 200px;
            height: 200px;
            background-color: yellow;
        }
    </style>
</head>
<body>
    
    <!-- <div class="box1"></div>
    <p>
        最常见的定义是：“主题是文章中通过具体材料所表达的基本思想。”这个定义由来已久，似无庸置疑，但仔细想来，它似有片面之嫌。常识告诉我们：文章是表情达意的工具。这个定义只及“达意”（表达的基本思想），而不及“表情”，岂不为缺漏？或谓“达意”即“表情”？若然，岂不是说情感与思想等同？但心理学研究早已证明，情感是与思维不同的心理过程，它“具有独特的主观体验的形式和外部表现的形式”，具有极为复杂的社会内容。虽然思想左右情感，但情感也会左右思想。详而言之，在实际心理过程中，有时思想是主流，有时情感是主流，尽管二者不可割裂。美国的J.M.索里、C.W.特尔福德在《教育心理学》中指出：“当全部反应在性质上主要是情感反应时（主要是内脏的），观念性的期望与知觉的和概念的意义（主要是神经的）同样也可以成为全部反应的组成部分。”反之亦然。心理过程中思想与情感所占地位的不同，“外化”或表现为不同文体中主题类型的不同。在逻辑类文章中，是“理为主”，在形象类文章中，是“情为主”。文论上说的“辞以情发”就是指的后者情形。各种形象类或文艺类文章，旨（主题）在“表情言志”，“以情感人”（不同于逻辑类的“以理服人”）。写这类文章，是“情动于中而形于外”，发乎情——“能憎，能爱，始能文”（鲁迅），终乎情——“情尽言止”。所以，为使“主题”或“主旨”的定义更有涵盖性，定义的中心词就应是思想与情感二者，即定义为：
    </p> -->
<!-- 
    <div class="box2">hello</div>
    <div class="box3">hello</div> -->

    <span class="s1">我是一个span</span>
</body>
</html>
```



### 简单的网页布局

```html
<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>简单的网页布局</title>
    <link rel="stylesheet" href="../exercise/css/reset.css">
    <style>
        .nav {
            width: 100%;
            background-color: beige;
        }
        
        nav,main,footer {
            margin: 0 auto;
            width: 900px;
        }

        nav {
            height: 65px;
            background-color: beige;
        }
        .nav-left {
            float: left;
            line-height: 65px;
        }
        .nav-left a {
            color: darkgray;
        }
        .nav-left a:hover {
            color: darkturquoise;
        }
        .nav-right {
            float: right;
            margin-top: 21px;
        }
        main {
            height: 600px;
            background-color: aqua;
        }

        footer {
            width: 100%;
            height: 100px;
            background-color: yellow;
        }

        .left {
            height: 100%;
            width: 680px;
            background-color: red;
            float: left;
            margin-right: 20px;
        }

        .right {
            height: 100%;
            width: 200px;
            background-color: black;
            float: left;
        }
    </style>
</head>

<body>

    <div class="nav">
        <!-- 导航 -->
        <nav>
            <div class="nav-left">
                <a href="#">首页</a>
                <a href="#">详情</a>
                <a href="#">关于网站</a>
            </div>
            <div class="nav-right">
                <button>登录</button>
            </div>
        </nav>
    </div>

    <!-- 主体 -->
    <main>
        <div class="left"></div>
        <div class="right"></div>
    </main>

    <!-- 底部 -->
    <footer></footer>
</body>

</html>
```



### 高度塌陷问题

#### 问题概述

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
    <style>
        .outer {
            border: 10px red solid;
            /* 
                BFC(Block Formatting Context) 块级格式化环境
                    - BFC是CSS中的一个隐含的属性，可以为一个元素开启BFC
                        开启BFC以后该元素会变成一个独立的布局区域
                    - 元素开启BFC后的特点
                        1、开启BFC的元素不会被浮动元素所覆盖
                        2、开启BFC的元素子元素和父元素外边距不会重叠
                        3、开启BFC的元素可以包含浮动的子元素
                    
                    - 可以通过特殊方式来开启元素的BFC；
                        1、设置元素的浮动（不推荐）
                        2、将元素设置为行内元素（不推荐）
                        3、将语速的overflow设置为一个非visible的值
                            - 常用方式 为元素设置 overflow:hidden 开启BFC 使其可以包含浮动元素 
             */
             overflow: hidden;
        }
        .inner {
            width: 100px;
            height: 100px;
            background-color: #bfa;

            /* 
                高度塌陷问题：
                    在浮动布局中，父元素的高度默认是被子元素撑开的，
                        当子元素浮动后，其会完全脱离文档流，子元素从文档流中脱离
                        将会无法撑起父元素的高度，导致父元素的高度丢失
                    
                    父元素高度丢失以后，其下的元素会自动上移，导致页面的布局混乱
                    
             */
            float: left;
        }
    </style>
</head>
<body>
    
    <div class="outer">
        <div class="inner"></div>
    </div>

    <div style="width: 100px; height: 100px;background-color: yellow;"></div>

</body>
</html>
```



#### BFC

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
    <style>
        .box1 {
            width: 200px;
            height: 200px;
            background-color: #bfa;
            /* float: left; */
            overflow: hidden;
        }
        .box2 {
            width: 200px;
            height: 200px;
            background-color: orange;
            overflow: hidden;
        }
        .box3 {
            width: 100px;
            height: 100px;
            background-color: yellow;
            margin-top: 100px;
        }
    </style>
</head>
<body>
    
    <div class="box1">
        <div class="box3"></div>
    </div>
    <!-- <div class="box2"></div> -->

</body>
</html>
```



#### clear

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
    <style>
        div {
            font-size: 50px;
        }
        .box1 {
            width: 200px;
            height: 200px;
            background-color: #bfa;
            float: left;
        }
        .box2 {
            width: 400px;
            height: 400px;
            background-color: #ff0;
            float: right;
        }
        .box3 {
            width: 200px;
            height: 200px;
            background-color: orange;
            /* 
                由于box1的浮动，导致box3位置上移
                    也就是box3受到box1浮动的影响，位置发生了改变
                
                clear
                    - 作用：清除浮动元素对当前元素的影响
                    - 可选值：
                        left 清除左侧浮动元素对当前元素的影响
                        right 清除右侧浮动元素对当前元素的影响
                        both 清除两侧中最大影响的那侧
                    
                    原理：设置清楚浮动以后，浏览器会自动为元素添加一个上外边距，使其位置不受其他元素的影响
             */
             clear: right;
        }
    </style>
</head>
<body>
    <div class="box1">1</div>
    <div class="box2"></div>
    <div class="box3">3</div>
</body>
</html>
```



#### 最终解决方案

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
    <style>
        .box1 {
            border: 10px red solid;
            /* overflow: hidden; */
        }
        .box2 {
            width: 100px;
            height: 100px;
            background-color: #bfa;
            float: left;
        }
        /* .box3 {
            clear: both;
        } */
        .box1::after {
            content: '';
            display: block;
            clear: both;
        }
    </style>
</head>
<body>
    <div class="box1">
        <div class="box2"></div>
        <!-- <div class="box3">3</div> -->
    </div>
</body>
</html>
```



#### clearfix

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
    <style>
        .box1 {
            width: 200px;
            height: 200px;
            background-color: #bfa;
        }
        .box2 {
            width: 100px;
            height: 100px;
            background-color: orange;
            margin-top: 100px;
        }

        /* clearfix 这个样式可以同时解决高度塌陷问题和外边距重叠的问题，遇到这些问题时，直接使用clearfix去解决 */
        .clearfix::before,
        .clearfix::after {
            content: '';
            display: table;
            clear: both;
        }
    </style>
</head>
<body>
    
    <div class="box1 clearfix">
        <div class="box2"></div>
    </div>

</body>
</html>
```



## 定位

### 定位简介

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
    <style>
        /*
            定位(position)
                - 定位是一种更加高级的布局手段
                - 通过定位可以将元素摆放到页面的任意位置
                - 使用position属性来设置定位
                    可选值：
                        static 默认值，元素是静止的，没有开启定位
                        relative 开启元素的相对定位
                        absolute 开启元素的绝对定位
                        fixed 开启元素的固定定位
                        sticky 开启元素的粘滞定位
                -相对定位：
                    - 当元素的position属性值设置为relative时则快开启了元素的相对定位
                        1 元素开启相对定位以后，如果不设置偏移量，元素不会发生任何的变化
                        2 相对定位是参照于文档流的中的位置进行定位的
                        3 相对定位会提升元素的层级
                        4 相对定位不会使元素脱离文档流
                        5 相对定位不会改变元素的性质块还是块，行内还是行内

                    - 偏移量(offset)
                        - 当元素开启定位以后，可以通过偏移量来设置元素的位置
                            top：定位元素和定位位置上边的距离
                            bottom：定位元素和定位位置下边的距离
                            left：定位元素和定位位置左边的距离
                            right：定位元素和定位位置右边的距离
                
        */
        body {
            font-size: 60px;
        }
        .box1 {
            width: 200px;
            height: 200px;
            background-color: #bfa;
        }
        .box2 {
            width: 200px;
            height: 200px;
            background-color: orange;
            /* 只要不是static就是开启定位 */
            position: relative;
            left: 200px;
            top: -200px;
        }
        .box3 {
            width: 200px;
            height: 200px;
            background-color: yellow;
        }
        

    </style>
</head>
<body>
    <div class="box1">1</div>
    <div class="box2">2</div>
    <div class="box3">3</div>
</body>
</html>
```



### 绝对定位

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
    <style>
        /*
            绝对定位
                - 当元素的position属性值设置为absolute时，则开启元素的绝对定位
                - 绝对定位的特点：
                    1 开启绝对定位后，如果不设置偏移量元素的位置不会发生改变
                    2 开启绝对定位后，元素会从文档流中脱离
                    3 绝对定位会改变元素的性质，行内变成块，块的宽高被内容撑开
                    4 绝对定位会使元素提升一个层级
                    5 绝对定位元素是相对于其包含块进行定位的

                    包含块（containing block）
                        - 正常情况下
                            包含块就是离当前元素最近的祖先块元素
                        
                        - 绝对定位的包含块：
                            就是离它最近的开启了定位的祖先元素，
                            如果所有的祖先元素都没有开启定位则相对于根元素进行定位
                            
                        - html（根元素、初始包含块）
        */
        body {
            font-size: 60px;
        }
        .box1 {
            width: 200px;
            height: 200px;
            background-color: #bfa;
        }
        .box2 {
            width: 200px;
            height: 200px;
            background-color: orange;
            /* 只要不是static就是开启定位 */
            position: absolute;
            left: 0;
            top: 0;
        }
        .box3 {
            width: 200px;
            height: 200px;
            background-color: yellow;
        }
        .box4 {
            width: 400px;
            height: 400px;
            background-color: tomato;
        }
        .box5 {
            width: 300px;
            height: 300px;
            background-color: aliceblue;
        }

    </style>
</head>
<body>
    <div class="box1">1</div>
    <div class="box4">
        4
        <div class="box5">
            5
            <div class="box2">2</div>
        </div>
    </div>
    
    <div class="box3">3</div>
</body>
</html>
```



### 固定定位

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
    <style>
        /*
            固定定位：
                - 当元素的position属性值设置为fixed时，则开启元素的绝对定位
                - 固定定位也是一种绝对定位，所以固定定位的大部分特点和绝对定位一样
                    唯一不同的是固定定位永远参照于浏览器视口进行定位
        */
        body {
            font-size: 60px;
        }
        .box1 {
            width: 200px;
            height: 200px;
            background-color: #bfa;
        }
        .box2 {
            width: 200px;
            height: 200px;
            background-color: orange;
            /* 只要不是static就是开启定位 */
            position: fixed;
            left: 0;
            top: 0;
        }
        .box3 {
            width: 200px;
            height: 200px;
            background-color: yellow;
        }
        .box4 {
            width: 400px;
            height: 400px;
            background-color: tomato;
        }
        .box5 {
            width: 300px;
            height: 300px;
            background-color: aliceblue;
        }

    </style>
</head>
<body>
    <div class="box1">1</div>
    <div class="box4">
        4
        <div class="box5">
            5
            <div class="box2">2</div>
        </div>
    </div>
    
    <div class="box3">3</div>
</body>
</html>
```



### 粘滞定位

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>导航条</title>
    <link rel="stylesheet" href="../exercise/css/reset.css">
    <style>
        body {
            height: 3000px;
        }
        /* 设置nav的大小 */
        .nav {
            width: 1043px;
            height: 48px;
            background-color: #E8E7E3;
            margin: 100px auto; 

            /*
                粘滞定位
                    - 当元素的position属性设置为sticky时则开启元素的粘滞定位
                    - 粘滞定位和相对定位的特点基本一致，
                        不同的是粘滞定位可以在元素到达某个位置时将其固定
            */

            position: sticky;
            top: 0;

        }

        /* 设置nav中的li */
        .nav li{
            float: left;
            /* 垂直居中 */
            line-height: 48px;
        }
        .nav a {
            /* 将a转换为块元素 */
            display: block;
            color: #777777;
            font-style: 18px;
            padding: 0 39px;
        }
        .nav li:last-child a {
            padding: 0 42px 0 41px;
        }

        /* 设置鼠标移入的效果 */
        .nav a:hover {
            background-color: #3F3F3F;
            color: #E8E7E3;
        }
        
    </style>
</head>
<body>
    <!-- 创建导航条的结构 -->
    <ul class="nav">
        <li>
            <a href="#">HTML/CSS</a>
        </li>
        <li>
            <a href="#">Browser Side</a>
        </li>
        <li>
            <a href="#">Server Side</a>
        </li>
        <li>
            <a href="#">Program ming</a>
        </li>
        <li>
            <a href="#">XML</a>
        </li>
        <li>
            <a href="#">Web Building</a>
        </li>
        <li>
            <a href="#">Reference</a>
        </li>
    </ul>
</body>
</html>
```



### 绝对定位元素的布局

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
    <style>
        .box1 {
            width: 500px;
            height: 500px;
            background-color: #bfa;
            position: relative;
        }
        .box2 {
            width: 100px;
            height: 100px;
            background-color: orange;
            position: absolute;
            /*
                水平布局
                    left + margin-left + border-left + padding-left = width = padding-left + width
                    + padding-right + border-right + margin-right + right = 包含块的内容区的宽度
                - 当我们开启了绝对定位后：
                    水平方向的布局等式就需要添加left和right两个值
                    当发生过度约束：
                        如果9个值中没有auto，则自动调整right值以使等式满足
                        如果有auto，则自动调整auto的值以使等式满足

                    - 可设置auto的值
                        margin width left right       

                    - 因为left 和 right的默认是auto，所以如果不知道left和right
                        则等式不满足时，会自动调整这两个值

                垂直方向布局的等式也必须满足
                    top + margin-top/bottom + padding-top/bottom = border-top/bottom + height = 包含块的高度 
                    
            */
            /* 父元素开启定位，设置下面五个值为0即可达到包含块内水平垂直居中 */
            margin: auto;
            left: 0;
            right: 0;
            top: 0;
            bottom: 0;
        }
    </style>
</head>
<body>
    
    <div class="box1">
        <div class="box2"></div>
    </div>

</body>
</html>
```



### 元素的层级

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
    <style>
        /*
            对于开启了定位元素，可以通过z-index属性来指定元素的层级
                z-index需要一个整数作为参数，值越大预元素层级越高
                    元素的层级越高越优先显示

                如果元素的层级一样，优先显示下面的元素

                祖先元素的层级再高也不会盖住后代元素
        */
        body {
            font-size: 60px;
        }
        .box1 {
            width: 200px;
            height: 200px;
            background-color: #bfa;
            position: absolute;
            z-index: 1;
        }
        .box2 {
            width: 200px;
            height: 200px;
            background-color: rgba(255, 0, 0,.3);
            /* 只要不是static就是开启定位 */
            position: absolute;
            top: 50px;
            left: 50px;
            z-index: 2;
        }
        .box3 {
            width: 200px;
            height: 200px;
            background-color: yellow;
            position: absolute;
            top: 100px;
            left: 100px;
            z-index: 3;
        }
        .box4 {
            width: 100px;
            height: 100px;
            background-color: orange;
            position: absolute;
        }

    </style>
</head>
<body>
    <div class="box1">1</div>
    <div class="box2">2</div>
    <div class="box3">3
        <div class="box4">4</div>
    </div>

</body>
</html>
```









# 小知识点

## 重置样式

我们默认是使用浏览器的样式，所以我们要出去浏览器的样式，因为这不是我们想要的

### ①测试使用

```html
        * {
            margin: 0;
            padding: 0;
        }h
```



### ②引入别人改好的

```css
html {
    -ms-text-size-adjust: 100%;
    -webkit-text-size-adjust: 100%;
    -webkit-tap-highlight-color: transparent;
    height: 100%;
}

body {
    margin: 0;
    font-size: 14px;
    font-family: "Helvetica Neue", Helvetica, STHeiTi, Arial, sans-serif;
    line-height: 1.5;
    color: #333;
    background-color: #fff;
    min-height: 100%;
}

article,
aside,
details,
figcaption,
figure,
footer,
header,
hgroup,
main,
menu,
nav,
section,
summary {
    display: block;
}

audio,
canvas,
progress,
video {
    display: inline-block;
}

audio:not([controls]) {
    display: none;
    height: 0;
}

progress {
    vertical-align: baseline;
}

[hidden],
template {
    display: none;
}

a {
    background: transparent;
    text-decoration: none;
    color: #08c;
}

a:active {
    outline: 0;
}

abbr[title] {
    border-bottom: 1px dotted;
}

b,
strong {
    font-weight: bold;
}

dfn {
    font-style: italic;
}

mark {
    background: #ff0;
    color: #000;
}

small {
    font-size: 80%;
}

sub,
sup {
    font-size: 75%;
    line-height: 0;
    position: relative;
    vertical-align: baseline;
}

sup {
    top: -0.5em;
}

sub {
    bottom: -0.25em;
}

img {
    max-width: 100%;
    border: 0;
    vertical-align: middle;
}

svg:not(:root) {
    overflow: hidden;
}

pre {
    overflow: auto;
    white-space: pre;
    white-space: pre-wrap;
    word-wrap: break-word;
}

code,
kbd,
pre,
samp {
    font-family: monospace, monospace;
    font-size: 1em;
}

button,
input,
optgroup,
select,
textarea {
    color: inherit;
    font: inherit;
    margin: 0;
    vertical-align: middle;
}

button,
input,
select {
    overflow: visible;
}

button,
select {
    text-transform: none;
}

button,
html input[type="button"],
input[type="reset"],
input[type="submit"] {
    -webkit-appearance: button;
    cursor: pointer;
}

[disabled] {
    cursor: default;
}

button::-moz-focus-inner,
input::-moz-focus-inner {
    border: 0;
    padding: 0;
}

input {
    line-height: normal;
}

input[type="checkbox"],
input[type="radio"] {
    box-sizing: border-box;
    padding: 0;
}

input[type="number"]::-webkit-inner-spin-button,
input[type="number"]::-webkit-outer-spin-button {
    height: auto;
}

input[type="search"] {
    -webkit-appearance: textfield;
    box-sizing: border-box;
}

input[type="search"]::-webkit-search-cancel-button,
input[type="search"]::-webkit-search-decoration {
    -webkit-appearance: none;
}

fieldset {
    border: 1px solid #c0c0c0;
    margin: 0 2px;
    padding: 0.35em 0.625em 0.75em;
}

legend {
    border: 0;
    padding: 0;
}

textarea {
    overflow: auto;
    resize: vertical;
    vertical-align: top;
}

optgroup {
    font-weight: bold;
}

input,
select,
textarea {
    outline: 0;
}

textarea,
input {
    -webkit-user-modify: read-write-plaintext-only;
}

input::-ms-clear,
input::-ms-reveal {
    display: none;
}

input::-moz-placeholder,
textarea::-moz-placeholder {
    color: #999;
}

input:-ms-input-placeholder,
textarea:-ms-input-placeholder {
    color: #999;
}

input::-webkit-input-placeholder,
textarea::-webkit-input-placeholder {
    color: #999;
}

.placeholder {
    color: #999;
}

table {
    border-collapse: collapse;
    border-spacing: 0;
}

td,
th {
    padding: 0;
}

h1,
h2,
h3,
h4,
h5,
h6,
p,
figure,
form,
blockquote {
    margin: 0;
}

ul,
ol,
li,
dl,
dd {
    margin: 0;
    padding: 0;
}

ul,
ol {
    list-style: none outside none;
}

h1,
h2,
h3 {
    line-height: 2;
    font-weight: normal;
}

h1 {
    font-size: 18px;
}

h2 {
    font-size: 16px;
}

h3 {
    font-size: 14px;
}

i {
    font-style: normal;
}

* {
    box-sizing: border-box;
}

.clearfix::before,
.clearfix::after {
    content: "";
    display: table;
}

.clearfix::after {
    clear: both;
}
```



## 设置图标

```html
<link rel="icon" href="../static/images/blog.ico" type="image/x-icon">
```



## 样式

### 渐变色文字

```css
background: linear-gradient(0.25turn, #c21500, #ffc500);
background-clip: text;
-webkit-background-clip: text;
color: transparent;
```

