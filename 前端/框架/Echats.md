# 一、ECharts入门示例

## 1	获取ECharts

在 https://www.jsdelivr.com/package/npm/echarts 选择 `dist/echarts.js`，点击并保存为 `echarts.js` 文件。



## 2	快速入门

```html
<script src="./js/echarts.js"></script>
```

```html
<body>
    <!-- ECharts的容器 -->
    <div id="main" style="width: 600px;height: 400px;"></div>
    <script type='text/javascript'>
        // 初始化ECharts实例
        // 是没有 # 号的，直接就是id名
        var myChat = echarts.init(document.getElementById('main'))

        // 指定图标的配置和数据
        var option = {
            title: {
                text: 'ECharts 入门示例'
            },
            tooltip: {},
            legend: {
                data: ['销量']
            },
            xAxis: {
                data: ['衬衫', '羊毛衫', '雪纺衫', '裤子', '高跟鞋', '袜子']
            },
            yAxis: {},
            series: [
                {
                    name: '销量',
                    type: 'bar',
                    data: [5, 20, 36, 10, 10, 20]
                }
            ]
        }
        // 使用钢材指定的配置项和数据显示图表
        myChat.setOption(option)

    </script>
</body>
```

![image-20211116142722771](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211116142724.png)







# 二、配置项说明

## 1	图表容器及大小

### ①在 HTML 中定义有宽度和高度的父容器（推荐）

定义一个div，然后通过css来指定容器高度和宽度，初始化的时候使用这个设置好的容器，图表的大小默认即为该节点的大小，**除非声明了 `opts.width` 或 `opts.height` 将其覆盖。**

```html
<div id="main" style="width: 600px;height:400px;"></div>
<script type="text/javascript">
  var myChart = echarts.init(document.getElementById('main'));
</script>
```



### ②指定图表的大小

```html
<div id="main"></div>
<script type="text/javascript">
  var myChart = echarts.init(document.getElementById('main'), null, {
    width: 600,
    height: 400
  });
</script>
```





## 2	title配置项

这个配置项就是用来设置图标的标题的



### ①text

标题内容



### ②textStyle

这个就是设置标题的样式和大小

-   **fontSytle**：

    主题文字风格

    -   normal:正常
    -   italic：斜体
    -   oblique：倾斜的

-   **fontSize**：

    主标题文字的字体大小

-   **fontFamily**

    主标题文字的字体系列

-   **fontWeight**

    主标题文字字体的粗细

    可选：

    -   `'normal'`
    -   `'bold'`
    -   `'bolder'`
    -   `'lighter'`
    -   100 | 200 | 300 | 400...

-   **color**

    文字颜色

![image-20211207191226891](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211207191228.png)





## 3	legend

图例组件。

图例组件展现了不同系列的标记(symbol)，颜色和名字。可以通过点击图例控制哪些系列不显示。

ECharts 3 中单个 echarts 实例中可以存在多个图例组件，会方便多个图例的布局。



### ①data

图例的数据数组。数组项通常为一个字符串，每一项代表一个系列的 `name`（如果是[饼图](#series-pie)，也可以是饼图单个数据的 `name`）。图例组件会自动获取对应系列的颜色，图形标记（symbol）作为自己绘制的颜色和标记，特殊字符串 `''`（空字符串）或者 `'\n'`（换行字符串）用于图例的换行。

如果要设置单独一项的样式，也可以把该项写成配置项对象。此时必须使用 `name` 属性对应表示系列的 `name`。

示例

```js
data: [{
    name: '系列1',
    // 强制设置图形为圆。
    icon: 'circle',
    // 这里的textStyle和title中的textStyle用法是一样的
    textStyle: {
        // 设置颜色为红色
        color: 'red',
    }
}]
```





## 4	tooltip

### ①trigger

当鼠标滑过去的会有一个浮框，这个浮框显示图例的信息

有三个选项：

-   item：只会出现鼠标所在的当前项数据

    ![image-20211213085601639](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211213085657.png)

-   axis：显示一列的数据

    ![image-20211213085648943](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211213085650.png)

-   none：就是不会触发





### ②position

提示框的出现的位置是在鼠标的什么方向

![image-20211213093603778](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211213093604.png)

![image-20211213090354856](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211213090355.png)





### ③textStyle

提示框浮层的文本样式。

和之前的一样

















## 公共配置项

### grid

控制图形在容器中的位置





### series.stack

通过赋值堆叠，名字相同的就会堆叠在一起























# 三、图案例

## 1	柱状图

柱状图（或称条形图）是一种通过柱形的长度来表现数据大小的一种常用图表类型。

**设置柱状图的方式，是将 `series` 的 `type` 设为 `'bar'`。**



### ①最基础的柱状图

统计一个学生多个学科的成绩

```html
<body>
    <div id="main" style="width: 600px;height: 400px;">

    </div>
    <script type='text/javascript'>
        // 初始化ECharts实例
        var myChat = echarts.init(document.getElementById('main'))
        // 指定图标和配置数据
        option = {
            title: {
                text: '统计单个学生成绩'
            },
            legend: {
                data: ['小智']
            },
            xAxis: {
                data: ['数学', '语文', '英语', '物理', '化学', '生物']
            },
            yAxis: {},
            series: [
                {
                    name: '小智',
                    type: 'bar',
                    data: [100, 100, 60, 78, 67, 89]
                }
            ]
        };

        // 使用配置好的数据项来展示图表
        myChat.setOption(option)

    </script>
</body>
```

![image-20211207153015325](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211207153017.png)



### ②多系列柱状图

说明：一个类型有多个系列

统计多个学生多个学科的成绩

例子：比如数学老师有多个学生，语文老师有多个学生，这几个学生是一样的，但是他们的分数不一致

```html
<body>
    <div id="main" style="width: 800px;height: 400px;">

    </div>
    <script type='text/javascript'>
        // 初始化ECharts实例
        var myChat = echarts.init(document.getElementById('main'))
        // 指定图标和配置数据
        option = {
            title: {
                text: '统计单个学生成绩'
            },
            legend: {
                data: ['小智', '黑仔', '张三']
            },
            xAxis: {
                data: ['数学', '语文', '英语', '物理', '化学', '生物']
            },
            yAxis: {},
            series: [
                {
                    name: '小智',
                    type: 'bar',
                    data: [100, 100, 60, 78, 67, 89]
                },
                {
                    name: '黑仔',
                    type: 'bar',
                    data: [89, 98, 23, 30, 80, 90]
                },
                {
                    name: '张三',
                    type: 'bar',
                    data: [100, 100, 100, 100, 100, 100]
                },
            ]
        };

        // 使用配置好的数据项来展示图表
        myChat.setOption(option)

    </script>
</body>
```

![image-20211207153427002](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211207153428.png)



### ③堆叠柱状图

**说明**：同一个类型的合成一条，查看之间的变化

**使用**：添加一个stack配置项，设置成一样的

**案例**：张三同学三个月各类型物品的出售情况

```html
<body>
    <div id="main" style="width: 800px;height: 400px;"></div>
    <script type='text/javascript'>
        // 初始化ECharts实例
        var myChat = echarts.init(document.getElementById('main'))
        // 指定图标和配置数据
        option = {
            title: {
                text: '统计张三同学三个的总业绩'
            },
            legend: {
                data: ['第一个月', '第二个月', '第三个月']
            },
            xAxis: {
                data: ['书本', '辣条', '桶面', '面包', '生活用品', '学习用品']
            },
            yAxis: {},
            series: [
                {
                    name: '第一个月',
                    type: 'bar',
                    data: [100, 100, 60, 78, 67, 89],
                    stack: 'x'
                },
                {
                    name: '第二个月',
                    type: 'bar',
                    data: [89, 98, 23, 30, 80, 90],
                    stack: 'x'
                },
                {
                    name: '第三个月',
                    type: 'bar',
                    data: [90, 74, 34, 70, 77, 55],
                    stack: 'x'
                },
            ]
        };
        // 使用配置好的数据项来展示图表
        myChat.setOption(option)
    </script>
</body>
```

![image-20211207155704184](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211207155705.png)





## 2	折线图

### ①基础折线图

统计张三同学多次月考的变化情况

```html
<body>
    <div id="main" style="width: 600px;height: 400px;"></div>
    <script type='text/javascript'>
        const myChat = echarts.init(document.getElementById('main'))
        option = {
            title: {
                text: "基础折线图"
            },
            xAxis: {
                data: ['第一次月考', '第二次月考', '第三次月考']
            },
            yAxis: {
                type: 'value'
            },
            series: [
                {
                    data: [400, 500, 300],
                    type: 'line',
                }
            ]
        }
        myChat.setOption(option)
    </script>
</body>
```





### ②堆叠折线图

**说明**：显示出两个曲线的变化

```html
<body>
    <div id="main" style="width: 800px;height: 400px;"></div>
    <script type='text/javascript'>
        const myChat = echarts.init(document.getElementById('main'))
        option = {
            title: {
                text: "张三同学两个月成绩的变化"
            },
            legend: {
                data: ['第一个月', '第二个月']
            },
            xAxis: {
                data: ['语文', '数学', '英语']
            },
            yAxis: {
                type: 'value'
            },
            series: [
                {
                    name: '第一个月',
                    data: [400, 500, 300],
                    type: 'line',
                    stack: 'x',
                    areaStyle: {},
                },
                {
                    name: '第二个月',
                    data: [300, 200, 600],
                    type: 'line',
                    stack: 'x',
                    areaStyle: {},
                },
            ]
        }
        myChat.setOption(option)
    </script>
</body>
```

![image-20211207160253982](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211207160255.png)





## 3	饼状图

饼图主要用于表现不同类目的数据在总和中的占比。每个的弧度表示数据数量的比例。



### ①基础饼图

饼图的配置和折线图、柱状图略有不同，**不再需要配置坐标轴，而是把数据名称和值都写在系列中**。以下是一个最简单的饼图的例子。

**radius配置项**：控制圆的大小，单位可以是百分比、像素....等css单位

**案例**：统计商店中的物品比例

```html
<body>
    <div id="main" style="width: 600px; height: 400px;"></div>
    <script>
        const myChat = echarts.init(document.getElementById('main'))
        option = {
            title: {
                text: '商店物品的占比',
            },
            legend: {
                data: ['辣条', '饼干', '糖果']
            },
            series: [
                {
                    type: 'pie',
                    data: [
                        {
                            name: '辣条',
                            value: 334,
                        },
                        {
                            name: '饼干',
                            value: 599,
                        },
                        {
                            name: '糖果',
                            value: 2002,
                        }
                    ],
                    // 通过控制半径来控制整个圆的大小
                    radius: '50%',
                }
            ],
        }
        myChat.setOption(option)
    </script>
</body>
```

![image-20211207162912414](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211207162913.png)



### ②圆环图

通过radius配置项来控制圆环的大小，它的面积 = 大环 - 小环

**randius**: ['小环', '大环']

```html
<body>
    <div id="main" style="width: 600px; height: 400px;"></div>
    <script>
        const myChat = echarts.init(document.getElementById('main'))
        option = {
            title: {
                text: '圆环图例子',
                left: 'center',
                top: 'center',
            },
            legend: {
                data: ['辣条', '饼干', '糖果']
            },
            series: [
                {
                    type: 'pie',
                    data: [
                        {
                            name: '辣条',
                            value: 334,
                        },
                        {
                            name: '饼干',
                            value: 599,
                        },
                        {
                            name: '糖果',
                            value: 2002,
                        }
                    ],
                    // 环形的面积就是大环 - 小环得到的
                    radius: ['40%', '70%'],
                },
            ],
        }
        myChat.setOption(option)
    </script>
</body>
```

![image-20211207164319632](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211207164320.png)















