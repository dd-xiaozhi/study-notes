#    Vue3快速上手

<img src="D:\学习笔记\img\20211206174721.png" style="width:200px" />



## 1.Vue3简介

- 2020年9月18日，Vue.js发布3.0版本，代号：One Piece（海贼王）
- 耗时2年多、[2600+次提交](https://github.com/vuejs/vue-next/graphs/commit-activity)、[30+个RFC](https://github.com/vuejs/rfcs/tree/master/active-rfcs)、[600+次PR](https://github.com/vuejs/vue-next/pulls?q=is%3Apr+is%3Amerged+-author%3Aapp%2Fdependabot-preview+)、[99位贡献者](https://github.com/vuejs/vue-next/graphs/contributors) 
- github上的tags地址：https://github.com/vuejs/vue-next/releases/tag/v3.0.0

## 2.Vue3带来了什么

### 1.性能的提升

- 打包大小减少41%

- 初次渲染快55%, 更新渲染快133%

- 内存减少54%

    ......

### 2.源码的升级

- 使用Proxy代替defineProperty实现响应式

- 重写虚拟DOM的实现和Tree-Shaking

    ......

### 3.拥抱TypeScript

- Vue3可以更好的支持TypeScript

### 4.新的特性

1. Composition API（组合API）

    - setup配置
    - ref与reactive
    - watch与watchEffect
    - provide与inject
    - ......
2. 新的内置组件
    - Fragment 
    - Teleport
    - Suspense
3. 其他改变

    - 新的生命周期钩子
    - data 选项应始终被声明为一个函数
    - 移除keyCode支持作为 v-on 的修饰符
    - ......





# 一、创建Vue3.0工程

## 1	使用 vue-cli 创建

官方文档：https://cli.vuejs.org/zh/guide/creating-a-project.html#vue-create

```bash
## 查看@vue/cli版本，确保@vue/cli版本在4.5.0以上
vue --version
## 安装或者升级你的@vue/cli
npm install -g @vue/cli
## 创建
vue create vue_test
## 启动
cd vue_test
npm run serve
```





## 2	使用 vite 创建

官方文档：https://v3.cn.vuejs.org/guide/installation.html#vite

vite官网：https://vitejs.cn

- 什么是vite？—— 新一代前端构建工具。
- 优势如下：
    - 开发环境中，无需打包操作，可快速的冷启动。
    - 轻量快速的热重载（HMR）。
    - 真正的按需编译，不再等待整个应用编译完成。
- 传统构建 与 vite构建对比图

<img src="D:\学习笔记\img\20211206174847.png" style="width:500px;height:280px;float:left" /><img src="https://cn.vitejs.dev/assets/esm.3070012d.png" style="width:480px;height:280px" />









```bash
## 创建工程
npm init vite-app <project-name>
## 进入工程目录
cd <project-name>
## 安装依赖
npm install
## 运行
npm run dev
```



## 3	分析工程结构

vue3并没有发生很大的变化，最多的就是main.js发生了很大的改变

```js
// 下面这种方式不能使用了
// import Vue from 'vue'

import { createApp } from 'vue'
import App from './App.vue'

// createApp工厂函数，直接调用创建app实例对象，以前的vm实例变成了现在的app实例
const app = createApp(App)
// 调用mount方法来绑定容器
app.mount('#app')

// 调用mount方法来绑定容器
app.mount('#app')
setTimeout(() => {
    app.unmount()
}, 1000)
```

app实例相比于vm实例要轻很多，减少了很多函数，同时添加了新的函数

![image-20211213171520259](D:\学习笔记\img\20211213171521.png)

小改变：在组件中可以省略div了





# 二、常用 Composition API

## 1	初识setup

1. 理解：Vue3.0中一个新的配置项，值为一个函数。
2. setup是所有<strong style="color:#DD5145">Composition API（组合API）</strong><i style="color:gray;font-weight:bold">“ 表演的舞台 ”</i>。
3. 组件中所用到的：数据、方法等等，均要配置在setup中。
4. setup函数的两种返回值：
    1. 若返回一个对象，则对象中的属性、方法, 在模板中均可以直接使用。（重点关注！）
    2. <span style="color:#aad">若返回一个渲染函数：则可以自定义渲染内容。（了解）</span>
5. 注意点：
    1. 尽量不要与Vue2.x配置混用
        - Vue2.x配置（data、methos、computed...）中<strong style="color:#DD5145">可以访问到</strong>setup中的属性、方法。
        - 但在setup中<strong style="color:#DD5145">不能访问到</strong>Vue2.x配置（data、methos、computed...）。
        - 如果有重名, setup优先。
    2. setup不能是一个async函数，因为返回值不再是return的对象, 而是promise, 模板看不到return对象中的属性。（后期也可以返回一个Promise实例，但需要Suspense和异步组件的配合）

**测试代码**

```vue
<template>
	<div>
		<h1>一个人的信息</h1>
		<h2>姓名：{{ name }}</h2>
		<h2>性别：{{ sex }}</h2>
		<h2>年龄：{{ age }}</h2>
        <button @click="sayWelcome">vue2中methods中的函数</button>
        <br>
        <button @click="sayHello">vue3中setup中的函数</button>
        <br>
        <button @click="test1">测试一下vue2的配置读取vue3的数据、方法</button>
        <br>
        <button @click="test2">测试一下vue3的配置读取vue2的数据、方法</button>
	</div>
</template>
<script>
// 使用渲染函数需要导入h函数
import {h} from 'vue';
export default {
	name: "App",
  data() {
    return {
      sex: '男',
      age: 18,
    }
  },
  methods: {
    sayWelcome() {
      alert('欢迎来到我的世界')
    },
    test1() {
      console.log(this.name);
      console.log(this.age);
      console.log(this.sayHello);
    }
  },
  setup() {
    let name = '张三'
    let age = 19
    
    function sayHello() {
      alert(`你好，我是${name},${age}了`)
    }
    
    function test2() {
      console.log(this.sex);
      console.log(this.age);
      console.log(this.sayWelcome);
    }

    // 返回一个对象（常用） --> 只有将东西交出去才能使用
    return {
      name,
      age,
      sayHello,
      test2,
    }

    // 返回一个函数(渲染函数) --> 需要导入h函数
    // return () => h('h1', '小智哥')
  }
};
</script>
```





## 2	ref函数

==这个ref和vue2的ref是不一样的==

**作用**：定义一个响应式的数据

**使用方式**：

1.  首先引入ref

    ```js
    import { ref } from "vue";
    ```

2.  使用：`let a = ref(xxx)`，这个xxx可以是基本数据类型，也可以是一个对象

    -   基本数据类型：在设置值和取值的时候需要`.value`去设置和取出对应的值
    -   对象类型：在第一层的时候需要`.value`去取值，第二层就是普通的对象调用了

==**注意**：在模板语法中使用的时候不用`.value`去取值，vue会帮我们去调用的==

**备注**：

- 基本类型的数据：响应式依然是靠``Object.defineProperty()``的```get```与```set```完成的。
- 对象类型的数据：内部 <i style="color:gray;font-weight:bold">“ 求助 ”</i> 了Vue3.0中的一个新函数—— ```reactive```函数。

**代码实现**

```vue
<template>
	<div>
		<h1>一个人的信息</h1>
		<h2>姓名：{{ name }}</h2>
		<h2>年龄：{{ age }}</h2>
		<h2>工作种类：{{ job.type }}</h2>
		<h2>工作薪水：{{ job.salary }}</h2>
		<button @click="updatePerson">修改人员信息</button>
		<br />
	</div>
</template>
<script>
import { ref } from "vue";
export default {
	name: "App",
	setup() {
		// 使用ref来实现动态数据 --> 需要引入ref
		let name = ref("张三");
		let age = ref(19);
		let job = ref({
			type: "前端工程师",
			salary: "30K",
		});
		console.log("@基本数据类型", name);
		console.log("@引用数据类型", job);
		function updatePerson() {
			// 基本数据类型要使用.value去获取值
			name.value = "李四";
			age.value = 18;
			// 对象类型，在第一层的时候需要使用.value去取值，里面的属性直接名字获取即可
			job.value.type = 'Java开发工程师'
			job.value.salary = '40K'
		}
		// 返回一个函数 --> 模板就可以使用里面的东西
		return {
			name,
			age,
      job,
			updatePerson,
		};
	},
};
</script>
```

![image-20211214163402331](D:\学习笔记\img\20211214163404.png)





## 3	reactive

### ①说明

**作用**: 定义一个<strong style="color:#DD5145">对象类型</strong>的响应式数据（==基本类型不要用它，要用```ref```函数==）

**语法**：```const 代理对象= reactive(源对象)```接收一个对象（或数组），返回一个<strong style="color:#DD5145">代理对象（Proxy的实例对象，简称proxy对象）</strong>

reactive定义的响应式数据是“深层次的”。

内部基于 ES6 的 Proxy 实现，通过代理对象操作源对象内部数据进行操作。

**说明**：ref中放入对象类型最终也是借助了reactive，然后reactive返回的就是proxy对象，所以我们直接使用reactive岂不是更好，更方便

![image-20211214164322504](D:\学习笔记\img\20211214164323.png)



### ②代码实现

```vue
<template>
	<div>
		<h1>一个人的信息</h1>
		<h2>姓名：{{ name }}</h2>
		<h2>年龄：{{ age }}</h2>
		<h2>工作种类：{{ job.type }}</h2>
		<h2>工作薪水：{{ job.salary }}</h2>
		<h2>爱好：{{hobby}}</h2>
		<button @click="updatePerson">修改人员信息</button>
		<br />
	</div>
</template>
<script>
import { ref, reactive } from "vue";
export default {
	name: "App",
	setup() {
		let name = ref("张三");
		let age = ref(19);
		let job = reactive({
			type: "前端工程师",
			salary: "30K",
		});
		// reactive也是可以封装数组类型
		let hobby = reactive(['抽烟', '喝酒', '烫头'])

		function updatePerson() {
			name.value = "李四";
			age.value = 18;
			// 使用了reactive就不需要通过.value去取值了
			job.type = "Java开发工程师";
			job.salary = "40K";
			hobby[0] = '学习';
		}
		return {
			name,
			age,
			job,
			hobby,
			updatePerson,
		};
	},
};
</script>
```



### ③小技巧

我们可以直接将数据放入到对象中，然后通过reactive来进行包装，然后我们就不需要`.value`去调用和取值了

```vue
<template>
	<div>
		<h1>一个人的信息</h1>
		<h2>姓名：{{ person.name }}</h2>
		<h2>年龄：{{ person.age }}</h2>
		<h2>工作种类：{{ person.job.type }}</h2>
		<h2>工作薪水：{{ person.job.salary }}</h2>
		<h2>爱好：{{ person.hobby }}</h2>
		<button @click="updatePerson">修改人员信息</button>
		<br />
	</div>
</template>
<script>
import { ref, reactive } from "vue";
export default {
	name: "App",
	setup() {
		// let name = ref("张三");
		// let age = ref(19);
		// let job = reactive({
		// 	type: "前端工程师",
		// 	salary: "30K",
		// });
		// // reactive也是可以封装数组类型
		// let hobby = reactive(["抽烟", "喝酒", "烫头"]);

		const person = reactive({
			name: "张三",
			age: 19,
			job: {
				type: "前端工程师",
				salary: "30K",
			},
			hobby: ["抽烟", "喝酒", "烫头"],
		});
		function updatePerson() {
			person.name = "李四";
			person.age = 18;
			// 使用了reactive就不需要通过.value去取值了
			person.job.type = "Java开发工程师";
			person.job.salary = "40K";
			person.hobby[0] = "学习";
		}
		return {
			// name,
			// age,
			// job,
			// hobby,
			person,
			updatePerson,
		};
	},
};
</script>
```





## 4	Vue3中的响应式原理

### ① Vue2.x的响应式

- 实现原理：

    - 对象类型：通过```Object.defineProperty()```对属性的读取、修改进行拦截（数据劫持）。

    - 数组类型：通过重写更新数组的一系列方法来实现拦截。（对数组的变更方法进行了包裹）。

        ```js
        Object.defineProperty(data, 'count', {
            get () {}, 
            set () {}
        })
        ```

- 存在问题：

    - 新增属性、删除属性, 界面不会更新。

        这里vue有提供对应的方法去解决($set、Vue.delete)

    - 直接通过下标修改数组, 界面不会自动更新。



### ②Vue3.0的响应式

- 实现原理: 

    - 通过Proxy（代理）:  拦截对象中任意属性的变化, 包括：属性值的读写、属性的添加、属性的删除等。

    - 通过Reflect（反射）:  对源对象的属性进行操作。

    - MDN文档中描述的Proxy与Reflect：

        - Proxy：https://developer.mozilla.org/zh-CN/docs/Web/JavaScript/Reference/Global_Objects/Proxy

            **说明**：Proxy*对象用于创建一个对象的代理，从而实现基本操作的拦截和自定义（如属性查找、赋值、枚举、函数调用等）。

        - Reflect：https://developer.mozilla.org/zh-CN/docs/Web/JavaScript/Reference/Global_Objects/Reflect

            **说明**：Reflect 是一个内置的对象，它提供拦截 JavaScript 操作的方法。。`Reflect`不是一个函数对象，因此它是不可构造的。它里面的方法都是静态方法

            ```js
            new Proxy(data, {
            	// 拦截读取属性值
                get (target, prop) {
                	return Reflect.get(target, prop)
                },
                // 拦截设置属性值或添加新属性
                set (target, prop, value) {
                	return Reflect.set(target, prop, value)
                },
                // 拦截删除属性
                deleteProperty (target, prop) {
                	return Reflect.deleteProperty(target, prop)
                }
            })
            
            proxy.name = 'tom'   
            ```

==这里先了解，把js基础补回来然后在回来看==





## 5	reactive对比ref

-  从定义数据角度对比：
    -  ref用来定义：<strong style="color:#DD5145">基本类型数据</strong>。
    -  reactive用来定义：<strong style="color:#DD5145">对象（或数组）类型数据</strong>。
    -  备注：ref也可以用来定义<strong style="color:#DD5145">对象（或数组）类型数据</strong>, 它内部会自动通过```reactive```转为<strong style="color:#DD5145">代理对象</strong>。
-  从原理角度对比：
    -  ref通过``Object.defineProperty()``的```get```与```set```来实现响应式（数据劫持）。
    -  reactive通过使用<strong style="color:#DD5145">Proxy</strong>来实现响应式（数据劫持）, 并通过<strong style="color:#DD5145">Reflect</strong>操作<strong style="color:orange">源对象</strong>内部的数据。
-  从使用角度对比：
    -  ref定义的数据：操作数据<strong style="color:#DD5145">需要</strong>```.value```，读取数据时模板中直接读取<strong style="color:#DD5145">不需要</strong>```.value```。
    -  reactive定义的数据：操作数据与读取数据：<strong style="color:#DD5145">均不需要</strong>```.value```





## 6	setup两个注意点

- setup执行的时机
    - 在beforeCreate之前执行一次，在setup使用this是undefined。

- setup的参数
    - props：值为对象，包含：组件外部传递过来，且组件内部声明接收了的属性。
    - context：上下文对象
        - attrs: 值为对象，包含：组件外部传递过来，但没有在props配置中声明的属性, 相当于 ```this.$attrs```。
        - slots: 收到的插槽内容, 相当于 ```this.$slots```。
        - emit: 分发自定义事件的函数, 相当于 ```this.$emit```。

**测试代码**

App组件

```vue
<template>
	<Demo01 @hello="showHelloMsg" msg="你好啊" school="社会大学"/>
</template>
<script>
import Demo01 from "./components/Demo01.vue";
export default {
	name: "App",
	components: {Demo01},
	setup() {
		function showHelloMsg(value) {
			alert(`触发了hello事件，我收到的参数是${value}`)
		}

		return {
			showHelloMsg,
		}
	}
};
</script>
```

Demo01组件

```vue
<template>
	<div>
		<h1>一个人的信息</h1>
		<h2>姓名：{{ person.name }}</h2>
		<h2>年龄：{{ person.age }}</h2>
		<h2>消息：{{ msg }}</h2>
		<h2>学校：{{ school }}</h2>
        <button @click="test">测试触发一下Demo组件的Hello事件</button>
	</div>
</template>

<script>
import {reactive} from 'vue';
export default {
	name: "Demo01",
    // 声明接收，可以省略（不影响），但是会报警告
    props: ['msg', 'school'],
    // 自我思考：因为在setup里面不能使用this来获取组件实例对象，所以它吧props和context传入进来
	setup(props, context) {
		const person = reactive({
			name: "张三",
			age: 19,
		});
        // 输出this是undefined，因为它在beforeCreate之前就调用了
        var aaa = this
        function test() {
            // console.log(aaa);
            // console.log(props.msg, context);
            // 触发hello事件绑定的函数
            context.emit('hello', 666)
        }
        return {
            person,
            test,
        }
	},
};
</script>
```





## 7	计算属性与侦听

### ① computed函数

与Vue2.x中computed配置功能一致

**代码实现**

```vue
<template>
	<div>
		<h1>一个人的信息</h1>
		<h2>姓：{{ person.firstName }}</h2>
		<h2>名：{{ person.lastName }}</h2>
		<h2>全名：{{ person.fullName }}</h2>
        全名:<input type="text" v-model="person.fullName">
	</div>
</template>

<script>
import { reactive, computed } from "vue";
export default {
	name: "Demo01",
	setup() {
		const person = reactive({
			firstName: "张",
			lastName: "三",
		});

		// 计算属性——简写（没有考虑计算属性被修改的情况）
/*  person.fullName = computed(() => {
			return person.firstName + "-" + person.lastName;
		}); */

        // 计算属性——完整写法（读和写），传入一个对象
        person.fullName = computed({
            get() {
                return person.firstName + "-" + person.lastName;
            },
            set(value) {
                const nameArr = value.split('-')
                person.firstName = nameArr[0]
                person.lastName = nameArr[1]
            }
        })
		return {
			person,
		};
	},
};
</script>
```



### ② watch函数

**用法**：watch(参数一，参数二，参数三)

-   参数一：
    -   侦听单个数据，传入对应变量
    -   侦听多个数据，传入一个数组
    -   侦听reactive所定义的数据的某个属性，参数一需要传入函数
-   参数二：函数，可以使用箭头函数和普通函数，接收两个值（newValue, oldValue），和vue2的一样
-   参数三：传入一个对象，里面是对应的配置项

**说明**：watch函数时可以调用多次的，就是可以同时使用多个watch函数，这是和vue2不同的地方，vue2只能有一个watch配置项

**注意**：侦听reactive的时候，强制开启深度侦听（deep配置项无效）

**代码实现**

```vue
<template>
	<div>
		<h2>当前求和为{{ sum }}</h2>
		<button @click="sum++">点我+1</button>
		<h2>当前消息为{{ msg }}</h2>
		<button @click="msg += '!'">点我改变信息</button>
        <h2>姓名：{{person.name}}</h2>
        <h2>年龄：{{person.age}}</h2>
        <h2>薪资：{{person.job.j1.salary}}K</h2>
        <button @click="person.name+='~'">姓名变化</button>
        <button @click="person.age++">年龄增长</button>
        <button @click="person.job.j1.salary++">涨薪</button>
	</div>
</template>

<script>
import { reactive, ref, watch } from "vue";
export default {
	name: "Demo01",
	setup() {
		let sum = ref(0);
		let msg = ref("hello");
        const person = reactive({
            name: '张三',
            age: 19,
            job: {
                j1: {
                    salary: 20
                }
            }
        })

		// 情况一：侦听ref所定义的单个响应式数据
		// watch(
		// 	sum,(newValue, oldValue) => {
		// 		console.log("sum变了", newValue, oldValue);
		// 	},{ immediate: true }
		// );

		// 情况二：侦听ref所定义的多个响应式数据
		// watch(
		// 	[sum, msg],(newValue, oldValue) => {
		// 		console.log("sum变了", newValue, oldValue);},
		// 	{ immediate: true, deep: true }
		// );

        /* 
            情况三：侦听reactive所定义的一个响应式数据的全部属性
                注意：1.此处不能正确获取到oldValue，输出的oldValue和newValue是一样的
                     2.强制开启深度侦听（deep无效）
        */
        // watch(person, (newValue, oldValue) => {
        //     console.log('person发生了变化', newValue, oldValue);
        // }, {deep: true})

        /* 
            情况四：侦听reactive所定义的一个响应式数据的某个属性
                结果：无法侦听到数据的变化
        */ 
        // watch(person.name, (newValue, oldValue) => {
        //     console.log("person中的name属性发生了改变", newValue, oldValue);
        // }, {immediate: true})

        /* 
            情况五：监视reactive所定义的一个响应式数据中的某些属性
                说明：如果要侦听reactive所定义的数据的某些属性，参数需要传入一个函数
        */
        // watch([() => person.name, () => person.age], (newValue, oldValue) => {
        //     console.log("person的name或age发生了改变", newValue, oldValue);
        // })

        /* 
            特殊情况：侦听reactive所定义的对象中的某个属性，deep配置项有效
        */
       watch(() => person.job, (newValue) => {
           console.log("person的job发生了改变", newValue);
       }, {deep: true})

		return {
			sum,
			msg,
            person
		};
	},
};
</script>
```





### ③ watch函数注意点

```vue
<script>
import { reactive, ref, watch } from "vue";
export default {
	name: "Demo01",
	setup() {
		let sum = ref(0);
		let msg = ref("hello");
		const person = ref({
			name: "张三",
			age: 19,
			job: {
				j1: {
					salary: 20,
				},
			},
		});
		/* 
            注意点：watch侦测ref所定义的引用数据类型
                说明：要想侦测到ref所定义的数据的属性，可以有以下两种做法
                    1.侦听对应数据的.value，ref的value里面是一个Proxy实例对象
                    2.开启深深度侦听
        */
		// watch(person.value, (newValue, oldValue) => {
		//     console.log('person发生了改变', newValue, oldValue);
		// })

		watch(person, (newValue, oldValue) => {
			console.log("person发生了改变", newValue, oldValue);
		}, {deep: true});

		return {
			sum,
			msg,
			person,
		};
	},
};
</script>
```



### ④ watchEffect函数

- watch的套路是：既要指明监视的属性，也要指明监视的回调。
- watchEffect的套路是：不用指明监视哪个属性，监视的回调中用到哪个属性，那就监视哪个属性。
    - 就是使用哪个就监视哪个
- watchEffect有点像computed：

    - 但computed注重的计算出来的值（回调函数的返回值），所以必须要写返回值。
    - 而watchEffect更注重的是过程（回调函数的函数体），所以不用写返回值。

```js
// 默认是开启immediate的
watchEffect(() => {
    // 注意：我们使用的是.value的值，不是RefImpl对象，所以我们使用的也是.value
    const a1 = sum.value
    const a2 = person.name
    console.log('watchEffect配置的回调执行了');
})
```





## 8	生命周期

<div style="border:1px solid black;width:380px;float:left;margin-right:20px;"><strong>vue2.x的生命周期</strong><img src="https://cn.vuejs.org/images/lifecycle.png" alt="lifecycle_2" style="zoom:33%;width:1200px" /></div><div style="border:1px solid black;width:510px;height:985px;float:left"><strong>vue3.0的生命周期</strong><img src="https://v3.cn.vuejs.org/images/lifecycle.svg" alt="lifecycle_2" style="zoom:33%;width:2500px" /></div>







































- 

- Vue3.0中可以继续使用Vue2.x中的生命周期钩子，但有有两个被更名：

    - ```beforeDestroy```改名为 ```beforeUnmount```

        ```destroyed```改名为 ```unmounted```

- Vue3.0也提供了 Composition API 形式的生命周期钩子，与Vue2.x中钩子对应关系如下：

    - `beforeCreate`===>`setup()`
    - `created =======> setup()`
    - `beforeMount` ===>`onBeforeMount`
    - `mounted =======> onMounted`
    - `beforeUpdate ===> onBeforeUpdate`
    - `updated =======>onUpdated`
    - `beforeUnmount ==> onBeforeUnmount`
    - `unmounted =====> onUnmounted`



**代码实现**

App组件

```vue
<template>
	<div>
		<button @click="isShowDemo = !isShowDemo">显示/隐藏</button>
		<Demo01 v-if="isShowDemo" />
	</div>
</template>
<script>
import { ref } from "vue";
import Demo01 from "./components/Demo01.vue";
export default {
	name: "App",
	components: { Demo01 },
	setup() {
		let isShowDemo = ref(true);

		return { isShowDemo };
	},
};
</script>
```

Demo01组件

```vue
<template>
	<div>
		<h2>当前求和为{{ sum }}</h2>
		<button @click="sum++">点我+1</button>
	</div>
</template>

<script>
import { ref, onUpdated, onBeforeUpdate, onBeforeMount, onMounted, onBeforeUnmount, onUnmounted } from "vue";
export default {
	name: "Demo01",
	setup() {
		console.log('---setup---');
		let sum = ref(0);

		onUpdated(() => {
			console.log('---onUpdated---');
		}) 
		onBeforeUpdate(() => {
			console.log('---onBeforeUpdate---');
		}) 
		onBeforeMount(() => {
			console.log('---onBeforeMount---');
		}) 
		onMounted(() => {
			console.log('---onMounted---');
		}) 
		onBeforeUnmount(() => {
			console.log('---onBeforeUnmount---');
		}) 
		onUnmounted(() => {
			console.log('---onUnmounted---');
		}) 

		return { sum };
	},
	beforeCreate() {
		console.log('---配置项beforeCreate---');
	},
	created() {
		console.log('---配置项created---');
	},
	beforeUpdate() {
		console.log('---配置项beforeUpdate---');
	},
	updated() {
		console.log('---配置项updated---');
	},
	beforeMount() {
		console.log('---配置项beforeMount---');
	},
	mounted() {
		console.log('---配置项mounted---');
	},
	beforeUnmount() {
		console.log('---配置项beforeUnmount---');
	},
	unmounted() {
		console.log('---配置项unmounted---');
	},

};
</script>
```

![image-20211215192246220](D:\学习笔记\img\20211215192247.png)

**总结**：要么全部使用组合式API，要么都使用配置项的生命钩子





## 9	自定义hook函数

**说明**：在Vue3中，使用什么API就引入，这个就是组合式API，但是如果引入很多的API就会需要引入多个API，不利于代码阅读，所以我们可以自定义hook函数来存放我们组合式API的东西，然后需要的时候进行引用

- 什么是hook？—— 本质是一个函数，把setup函数中使用的Composition API进行了封装。
    - 就是将使用到的函数放到一个文件里面，然后进行引入

- 类似于vue2.x中的mixin。
- 自定义hook的优势: 复用代码, 让setup中的逻辑更清楚易懂。

**实现**

1.  首先src目录下创建一个名为hook的文件夹，然后创建一个js文件，文件的名字是可以其定义的，不是固定的

2.  代码实现

    -   src/hooks/usePoint.js

        ```js
        import { reactive, onMounted,onBeforeUnmount } from 'vue';
        export default function () {
            // 获取到鼠标点击的坐标
            let point = reactive({
                x: 0,
                y: 0,
            })
            // 实现鼠标打点的方法
            function savePoint(event) {
                point.x = event.pageX
                point.y = event.pageY
                console.log(event.pageX, event.pageY);
            }
        
            // 实现鼠标打点的生命周期钩子
            onMounted(() => {
                window.addEventListener('click', savePoint)
            }),
            onBeforeUnmount(() => {
                window.removeEventListener('click', savePoint)
            })
            
            return point
        }
        ```

    -   Demo组件中引入hooks函数

        ```vue
        <template>
        	<div>
        		<h2>当前求和为{{ sum }}</h2>
        		<button @click="sum++">点我+1</button>
        		<h2>当前点击时鼠标的坐标为：x: {{point.x}}，Y：{{point.y}}</h2>
        	</div>
        </template>
        
        <script>
        import { ref} from "vue";
        import usePoint from '../hooks/usePoint';
        export default {
        	name: "Demo01",
        	setup() {
        		console.log('---setup---');
        		let sum = ref(0);
        		let point = usePoint()
        		return { sum, point };
        	},
        };
        </script>
        ```





## 10	toRef和toRefs

- 作用：创建一个 ref 对象，其value值指向另一个对象中的某个属性。
- 说明：在模板使用的时候，如果一个对象嵌套的太深，那么调用的时候就需要一层层往下掉，toRef可以解决这个问题，使得模板变得更加简洁，更符合vue的准则
- 语法：```const name = toRef(person,'name')```，第一个参数是要封装的那个对象，第二个参数是哪个属性
- 应用:   要将响应式对象中的某个属性单独提供给外部使用时。


- 扩展：```toRefs``` 与```toRef```功能一致，但可以批量创建多个 ref 对象，语法：```toRefs(person)```



**代码实现**

```vue
<template>
	<div>
		<h4>{{person}}</h4>
		<h2>姓名：{{ name }}</h2>
		<h2>年龄：{{ age }}</h2>
		<h2>薪资：{{ job.j1.salary }}</h2>
		<button @click="name += '~'">修改名字</button>
		<button @click="age++">增加年龄</button>
		<button @click="job.j1.salary++">涨薪</button>
	</div>
</template>

<script>
import { reactive, toRefs, ToRef } from "vue";
export default {
	name: "Demo01",
	setup() {
		// 数据
		let person = reactive({
			name: "张三",
			age: 18,
			job: {
				j1: {
					salary: 20,
				},
			},
		});

		return {
			// 情况一：直接取出返回，破坏了响应式
			// name: person.name,
			// age: person.age,
			// salary: person.job.j1.salary

			/* 
				情况二：使用ref去定义返回
					结果：数据变成了响应式，但是person中的属性没有被修改，
					也就是说它将person的值拆分出去了，独立开来了，不再是代理person中的属性值，而是一个单独的值
			*/
			// name: ref(person.name),
			// age: ref(person.age),
			// salary: ref(person.job.j1.salary)

			/* 
				情况三：使用toRef去解决情况二的问题
					结果：toRef它就是代理了person，修改toRef所定义的值时，person中的值也会发生改变
					使用：toRef(参数一，参数二)
						参数一：要代理的对象
						参数二：要绑定的属性
			*/
			// name: toRef(person, 'name'),
			// age: toRef(person, 'age'),
			// salary: toRef(person.job.j1, 'salary'),

			/* 
				情况四：使用toRefs一次代理多个属性 --> 传入一个对象
			*/
			person,
			// ...展开对象
			...toRefs(person)
		};
	},
};
</script>
```







# 三、其他Componsition API

## 1	shallowReactive和refReactive

- shallowReactive：只处理对象最外层属性的响应式（浅响应式）。
- shallowRef：只处理基本数据类型的响应式, 不进行对象的响应式处理。

- **什么时候使用?**
    -  如果有一个对象数据，结构比较深, 但变化时只是外层属性变化 ===> shallowReactive。
    -  如果有一个对象数据，后续功能不会修改该对象中的属性，而是生新的对象来替换 ===> shallowRef。

**代码实现**

```vue
<template>
	<div>
		<h4>x.y的值为{{ x.y }}</h4>
		<button @click="x.y++">x.y++</button>
		<button @click="x = {y:999}">替换x</button>
		<hr />
		<h2>名字：{{ name }}</h2>
		<h2>年龄：{{ age }}</h2>
		<h2>薪资：{{ job.j1.salary }}</h2>
		<button @click="name += '!'">改变名字</button>
		<button @click="age++">增加年龄</button>
		<button @click="job.j1.salary++">涨薪</button>
	</div>
</template>

<script>
import { reactive, ref, toRefs, shallowReactive, shallowRef } from "vue";
export default {
	name: "Demo01",
	setup() {
		// let x = ref(0);
		// 不处理响应式的数据
		let x = shallowRef({
			y: 0
		});

		// 响应第一层的数据，浅层次响应
		let person = shallowReactive({
		// let person = reactive({
			name: "张三",
			age: 18,
			job: {
				j1: {
					salary: 30000,
				},
			},
		});
		return {
			x,
			...toRefs(person),
		};
	},
};
</script>
```





## 2	readOnly和shallowReadOnly

- readonly: 让一个响应式数据变为只读的（深只读）。所有数据都不能修改
- shallowReadonly：让一个响应式数据变为只读的（浅只读）。第一层不能修改，只能读
- 应用场景: 接收到其他组件传过来的数据不允许修改其他组件的数据时可以使用这个生成新的对象

**代码实现**

```vue
<template>
	<div>
		<h4>x的值为{{ x }}</h4>
		<button @click="x++">x++</button>
		<hr />
		<h2>名字：{{ name }}</h2>
		<h2>年龄：{{ age }}</h2>
		<h2>薪资：{{ job.j1.salary }}</h2>
		<button @click="name += '!'">改变名字</button>
		<button @click="age++">增加年龄</button>
		<button @click="job.j1.salary++">涨薪</button>
	</div>
</template>

<script>
import { reactive, ref, toRefs, readonly, shallowReadonly } from "vue";
export default {
	name: "Demo01",
	setup() {
		let x = ref(0);
		let person = reactive({
			name: "张三",
			age: 18,
			job: {
				j1: {
					salary: 30000,
				},
			},
		});

		x = readonly(x)
		person = shallowReadonly(person)
		return {
			x,
			...toRefs(person),
		};
	},
};
</script>
```





## 3	toRaw 与 markRaw

- toRaw：
    - 作用：将一个由```reactive```生成的<strong style="color:orange">响应式对象</strong>转为<strong style="color:orange">普通对象</strong>。
    - 使用场景：用于读取响应式对象对应的普通对象，对这个普通对象的所有操作，不会引起页面更新。
- markRaw：
    - 作用：标记一个对象，使其永远不会再成为响应式对象。
    - 说明：被它所定义的数据不是响应式的，它是随后才进行渲染的
    - 例如：对它所定义的数据进行修改，它不会立即修改，当我们去做其他事情的时候它才会被渲染出来
    - 应用场景:
        1. 有些值不应被设置为响应式的，例如复杂的第三方类库等。
        2. 当渲染具有不可变数据源的大列表时，跳过响应式转换可以提高性能。

**代码实现**

```vue
<template>
	<div>
		<h4>x的值为{{ x }}</h4>
		<button @click="x++">x++</button>
		<hr />
		<h2>名字：{{ name }}</h2>
		<h2>年龄：{{ age }}</h2>
		<h2>薪资：{{ job.j1.salary }}</h2>
		<h2>座驾信息:{{ car }}</h2>
		<button @click="name += '!'">改变名字</button>
		<button @click="age++">增加年龄</button>
		<button @click="job.j1.salary++">涨薪</button>
		<button @click="showRawPerson">输出最原始的person</button>
		<button @click="addCar">给人添加一台车</button>
		<button @click="car.name += '~'">改车信息</button>
		<button @click="changePrice">改价格</button>
	</div>
</template>

<script>
import { markRaw, reactive, ref, toRaw, toRefs } from "vue";
export default {
	name: "Demo01",
	setup() {
		let x = ref(0);
		let person = reactive({
			name: "张三",
			age: 18,
			job: {
				j1: {
					salary: 30000,
				},
			},
			car: {},
		});

		// 输出最原始的person --> 通过toRaw转换成普通的对象
		function showRawPerson() {
			const p = toRaw(person);
			console.log(p);
		}

		// 给人添加一台车
		function addCar() {
			let c = {
				name: "法拉利",
				price: 2000,
			};
			person.car = markRaw(c);
		}

		function changePrice() {
			person.car.price++;
			console.log(person.car.price);
		}
		return {
			x,
			...toRefs(person),
			showRawPerson,
			addCar,
			changePrice,
		};
	},
};
</script>
```





## 4	customRef

创建一个自定义的 ref，并对其依赖项跟踪和更新触发进行显式控制。它需要一个工厂函数，该函数接收 `track` 和 `trigger` 函数作为参数，并且应该返回一个带有 `get` 和 `set` 的对象。

**代码实现**

```vue
<template>
	<div>
		<input type="text" v-model="keyWord" />
		<h2>{{ keyWord }}</h2>
	</div>
</template>

<script>
import { customRef, ref } from "vue";
export default {
	name: "Demo01",
	setup() {
		// 自定义一个ref，名为myRef	--> 自定义的功能：让它延时渲染到页面上
		function myRef(value, delay) {
			let timer
			return customRef((track, trigger) => {
				return {
					get() {
						console.log(`有人从myRef这个容器中读取数据了，我把${value}给他`);
						track(); // 通知Vue追踪value的变化
						return value;
					},
					set(newValue) {
						console.log(`有人把myRef这个容器中数据改为了：${newValue}`);
						// 防抖 ——> 进来之前先清除定时器
						clearTimeout(timer)
						timer = setTimeout(() => {
							value = newValue;
							trigger(); // 通知Vue重新解析模板
						}, delay);
					},
				};
			});
		}

		// let keyWord = ref('hello')
		let keyWord = myRef("hello", 500);

		return {
			keyWord,
		};
	},
};
</script>
```





## 5	provide 与 inject

<img src="https://v3.cn.vuejs.org/images/components_provide.png" style="width:300px" />

- 作用：实现<strong style="color:#DD5145">祖与后代组件间</strong>通信

- 套路：父组件有一个 `provide` 选项来提供数据，后代组件有一个 `inject` 选项来开始使用这些数据

**代码实现**

App组件

```vue
<template>
	<div class="app">
		<h2>祖组件（App组件）, {{name}}--{{price}}</h2>
		<Child />
	</div>
</template>
<script>
import { reactive,provide } from 'vue';
import Child from "./components/Child.vue";
export default {
	name: "App",
	components: { Child },
	setup() {
		let car = reactive({name: '法拉利', price: '40w'})
		provide('car', car)		// 给自己的后代组件传递数据	
		return {...car}
	}
};
</script>
<style >
.app {
	width: 500px;
	height: 300px;
	padding: 10px;
	background-color: aqua;
}
</style>
```

Child组件

```vue
<template>
	<div class="child">
		<h2>父组件 {{name}}---{{price}}</h2>
		<Son />
	</div>
</template>

<script>
import Son from "./Son.vue";
import {inject} from 'vue';
export default {
	components: { Son },
	setup() {
		// 获取到祖组件传来的数据
		let car = inject("car");
		return { ...car };
	},
};
</script>

<style>
.child {
	background-color: coral;
	padding: 10px;
	height: 200px;
}
</style>
```

Son组件

```vue
<template>
  <div class="son">
      <h2>孙组件 {{name}}---{{price}}</h2>
  </div>
</template>

<script>
import {inject} from 'vue';
export default {
    name: 'Son',
    setup() {
        // 获取到祖组件传来的数据
        let car = inject('car')   
        return {...car}
    },
}
</script>

<style>
.son {
    background-color:rgb(228, 250, 105);
    height: 100px;
}
</style>
```





## 6	响应式数据的判断

- isRef: 检查一个值是否为一个 ref 对象
- isReactive: 检查一个对象是否是由 `reactive` 创建的响应式代理
- isReadonly: 检查一个对象是否是由 `readonly` 创建的只读代理
- isProxy: 检查一个对象是否是由 `reactive` 或者 `readonly` 方法创建的代理

```vue
<template>
	<h2>hello</h2>
	<button @click="show">判断</button>
</template>
<script>
import {ref, reactive, isRef, isReactive, isReadonly, isProxy, readonly} from 'vue';
export default {
	name: "App",
	setup() {
		let x = ref(0)
		let person = reactive({name: '小智', age:19})
		let p = readonly(person)

		function show() {
			console.log(isRef(x));
			console.log(isReactive(person));
			console.log(isReadonly(p));
			console.log(isProxy(person));
		}

		return {show}
	}
};
</script>
```







# 四、Composition API 的优势

## 1.Options API 存在的问题

使用传统OptionsAPI中，新增或者修改一个需求，就需要分别在data，methods，computed里修改 。

<div style="width:600px;height:370px;overflow:hidden;float:left">
    <img src="https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/f84e4e2c02424d9a99862ade0a2e4114~tplv-k3u1fbpfcp-watermark.image" style="width:600px;float:left" />
</div>
<div style="width:300px;height:370px;overflow:hidden;float:left">
    <img src="https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e5ac7e20d1784887a826f6360768a368~tplv-k3u1fbpfcp-watermark.image" style="zoom:50%;width:560px;left" /> 
</div>
















## 2.Composition API 的优势

我们可以更加优雅的组织我们的代码，函数。让相关功能的代码更加有序的组织在一起。

<div style="width:500px;height:340px;overflow:hidden;float:left">
    <img src="https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/bc0be8211fc54b6c941c036791ba4efe~tplv-k3u1fbpfcp-watermark.image"style="height:360px"/>
</div>
<div style="width:430px;height:340px;overflow:hidden;float:left">
    <img src="https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6cc55165c0e34069a75fe36f8712eb80~tplv-k3u1fbpfcp-watermark.image"style="height:360px"/>
</div>































# 五、新的组件

## 1	Fragment

- 在Vue2中: 组件必须有一个根标签
- 在Vue3中: 组件可以没有根标签, 内部会将多个标签包含在一个Fragment虚拟元素中
- 好处: 减少标签层级, 减小内存占用

```vue
vue2
<template>
	<div>
        <h2>hello</h2>
		<button @click="show">判断</button>
    </div>
</template>

vue3
<template>
	<h2>hello</h2>
	<button @click="show">判断</button>
</template>
```





## 2	Teleport

- 什么是Teleport？—— `Teleport` 是一种能够将我们的<strong style="color:#DD5145">组件html结构</strong>移动到指定位置的技术。

- 说明：就是能将元素放到指定的位置，比如我要放在body中，而不是在body元素中的元素里，那么就可以使用这个去移动到对应的位置中

    ```vue
    <teleport to="移动位置">
    	<div v-if="isShow" class="mask">
    		<div class="dialog">
    			<h3>我是一个弹窗</h3>
    			<button @click="isShow = false">关闭弹窗</button>
    		</div>
    	</div>
    </teleport>
    ```

    这里实现一个遮罩弹窗的效果





## 3	Suspense

- 等待异步组件时渲染一些额外内容，让应用有更好的用户体验

- 使用步骤：

    - 异步引入组件（必须是异步引入组件才有用）

        ```js
        // 静态引入
        // import Child from './components/Child.vue'
        import {defineAsyncComponent} from 'vue'
        const Child = defineAsyncComponent(()=>import('./components/Child.vue'))
        ```

    - 使用```Suspense```包裹组件，并配置好```default``` 与 ```fallback```

        - 说明：default和fallback是Suspense组件中已经设置好的插槽

        ```vue
        <template>
        	<div class="app">
        		<h3>我是App组件</h3>
        		<Suspense>
                    <!--默认渲染的页面-->
        			<template v-slot:default>
        				<Child/>
        			</template>
        			<!--默认渲染的页面还没加载出来展示的页面-->
        			<template v-slot:fallback>
        				<h3>加载中.....</h3>
        			</template>
        		</Suspense>
        	</div>
        </template>
        ```





# 六、其他

## 1.全局API的转移

- Vue 2.x 有许多全局 API 和配置。

    - 例如：注册全局组件、注册全局指令等。

        ```js
        //注册全局组件
        Vue.component('MyButton', {
          data: () => ({
            count: 0
          }),
          template: '<button @click="count++">Clicked {{ count }} times.</button>'
        })
        
        //注册全局指令
        Vue.directive('focus', {
          inserted: el => el.focus()
        }
        ```

- Vue3.0中对这些API做出了调整：

    - 将全局的API，即：```Vue.xxx```调整到应用实例（```app```）上

        | 2.x 全局 API（```Vue```） | 3.x 实例 API (`app`)                        |
        | ------------------------- | ------------------------------------------- |
        | Vue.config.xxxx           | app.config.xxxx                             |
        | Vue.config.productionTip  | <strong style="color:#DD5145">移除</strong> |
        | Vue.component             | app.component                               |
        | Vue.directive             | app.directive                               |
        | Vue.mixin                 | app.mixin                                   |
        | Vue.use                   | app.use                                     |
        | Vue.prototype             | app.config.globalProperties                 |



## 2.其他改变

- data选项应始终被声明为一个函数。

- 过度类名的更改：

    - Vue2.x写法

        ```css
        .v-enter,
        .v-leave-to {
          opacity: 0;
        }
        .v-leave,
        .v-enter-to {
          opacity: 1;
        }
        ```

    - Vue3.x写法

        ```css
        .v-enter-from,
        .v-leave-to {
          opacity: 0;
        }
        
        .v-leave-from,
        .v-enter-to {
          opacity: 1;
        }
        ```

- <strong style="color:#DD5145">移除</strong>keyCode作为 v-on 的修饰符，同时也不再支持```config.keyCodes```

- <strong style="color:#DD5145">移除</strong>```v-on.native```修饰符

    - 父组件中绑定事件

        ```vue
        <my-component
          v-on:close="handleComponentEvent"
          v-on:click="handleNativeClickEvent"
        />
        ```

    - 子组件中声明自定义事件

        ```vue
        <script>
          export default {
            emits: ['close']
          }
        </script>
        ```

- <strong style="color:#DD5145">移除</strong>过滤器（filter）

    > 过滤器虽然这看起来很方便，但它需要一个自定义语法，打破大括号内表达式是 “只是 JavaScript” 的假设，这不仅有学习成本，而且有实现成本！建议用方法调用或计算属性去替换过滤器。

- ...... 







# 七、使用路由

可以参考一篇文章：https://www.jianshu.com/p/ca0615a8ce08







# Vue3组件通信方式
