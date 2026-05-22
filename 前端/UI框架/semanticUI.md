# semanticUI学习

## 下载

官网：https://semantic-ui.com/

中文文档：https://zijieke.com/semantic-ui/introduction/getting-started.php

使用：引入UI的css和js即可，可以使用cdn和本地的方式





## 按钮（button）

### 按钮的样式和大小

```html
<body>
    <!-- 按钮的样式 -->
    <button class="ui button">按钮</button>
    <button class="ui basic button">基础</button>
    <button class="ui positive button">积极</button>
    <button class="ui negative button">消极</button>
    <br>
    <br>
    <!-- 按钮的大小 -->
    <button class="ui button">正常大小</button>
    <button class="ui mini button">最小</button>
    <button class="ui tiny button">微小</button>
    <button class="ui small button">很小</button>
    <button class="ui medium button">中等</button>
    <button class="ui large button">稍大</button>
    <button class="ui big button">很大</button>
    <button class="ui huge button">巨大</button>
    <button class="ui massive button">最大</button>
</body>
```



### 按钮的颜色

```html
<!-- 和英文单词是相对应的 -->
    <button class="ui positive button">红色</button>
    <button class="ui orange button">橘色</button>
    <!-- 还可以是一些网站按钮的颜色 -->
    <button class="ui facebook button">
        <i class="facebook icon"></i>
        Facebook
      </button>
      <button class="ui twitter button">
        <i class="twitter icon"></i>
        Twitter
      </button>
      <button class="ui google plus button">
        <i class="google plus icon"></i>
        Google Plus
      </button>
```



### 按钮的浮动

```HTML
<button class="ui right floated button">右浮动</button>
<button class="ui left floated button">左浮动</button>
```



### 按钮组

就是将几个按钮放在一起

```html
<!-- 按钮组 -->
<!-- 垂直组：vertical buttons -->
<div class="ui vertical buttons">
    <button class="ui button">Feed</button>
    <button class="ui button">Messages</button>
    <button class="ui button">Events</button>
    <button class="ui button">Photos</button>
</div>
<br><br><br>

<!-- 水平组就是把vertical去掉就可以了 -->
<div class="ui buttons">
    <button class="ui button">Feed</button>
    <button class="ui button">Messages</button>
    <button class="ui button">Events</button>
    <button class="ui button">Photos</button>
</div>
<br><br><br>

<!-- 图标组：在button里面加一个icon -->
<div class="ui icon buttons">
    <button class="ui button">
        <i class="play icon"></i>
    </button>
    <button class="ui button">
        <i class="pause icon"></i>
    </button>
    <button class="ui button">
        <i class="shuffle icon"></i>
    </button>
</div>
```



### 动画

```html
<!-- 动画 -->
<div class="ui animated button" tabindex="0">
    <div class="visible content">
        <i class="ui weixin icon"></i>
    </div>
    <div class="hidden content">
        <i class="right arrow icon"></i>
    </div>
</div>
<div class="ui vertical animated button" tabindex="0">
    <div class="hidden content">商店</div>
    <div class="visible content">
        <i class="shop icon"></i>
    </div>
</div>
<div class="ui animated fade button" tabindex="0">
    <div class="visible content">注册账户</div>
    <div class="hidden content">
        每月 $12.99
    </div>
</div>
```



**补充**：虽然任何标签都可以当按钮使用，但如果想要聚焦键盘，你只能带上 `<button>` 或者添加一个 `tabindex="0"` 的属性。键盘访问按钮将保持焦点样式点击后，会是视觉上的震动。





## 容器（container）

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/semantic-ui/2.2.10/semantic.min.css">
    <script src="https://cdn.jsdelivr.net/semantic-ui/2.2.10/semantic.min.js"></script>
    <title>Document</title>
</head>
<body>
    <!-- 普通的就是占满整个容器 -->
    <div class="ui container">
        <p>
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
        </p>
    </div>

    <!-- 文本容器，里面的字体更大，有一个外边距 -->
    <div class="ui text container">
        <p>
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
        </p>
    </div>

    <!-- 流式容器，占满body容器 -->
    <div class="ui fluid container">
        <p>
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
        </p>
    </div>

    <!-- 右对齐，在容器中对齐 -->
    <div class="ui rught aligned container">
        <p>
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
        </p>
    </div>

    <!-- 左对齐 -->
    <div class="ui left aligned container">
        <p>
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
        </p>
    </div>

    <!-- 居中对齐 -->
    <div class="ui center aligned container">
        <p>
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
            测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试
        </p>
    </div>
</body>
</html>
```



## 输入框（input）

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/semantic-ui/2.2.10/semantic.min.css">
    <script src="https://cdn.jsdelivr.net/semantic-ui/2.2.10/semantic.min.js"></script>
    <title>输入框的写法</title>
</head>
<body>
    
    <!-- 输入是包含在div中的 -->
    <div class="ui input">
        <input type="text" placeholder="标准的输入框">
    </div>

    <div class="ui focus input">
        <input type="text" placeholder="聚焦输入框">
    </div>

    <div class="ui disabled input">
        <input type="text" placeholder="输入框">
    </div>

    <div class="ui transparent input">
        <input type="text" placeholder="透明输入框">
    </div>
    <br><br><br><br>

    <div class="ui mini input">
        <input type="text" placeholder="微小的输入框">
    </div>
    <div class="ui small input">
        <input type="text" placeholder="稍微大的输入框">
    </div>
    <div class="ui large input">
        <input type="text" placeholder="大一点的输入框">
    </div>
    <div class="ui huge input">
        <input type="text" placeholder="巨大的输入框">
    </div>
    <div class="ui massive input">
        <input type="text" placeholder="最大的输入框">
    </div>
</body>
</html>
```





## 图片（img）

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/semantic-ui/2.2.10/semantic.min.css">
    <script src="https://cdn.jsdelivr.net/semantic-ui/2.2.10/semantic.min.js"></script>
    <title>输入框的写法</title>
</head>
<body>
    
    <!-- 输入是包含在div中的 -->
    <div class="ui input">
        <input type="text" placeholder="标准的输入框">
    </div>

    <div class="ui focus input">
        <input type="text" placeholder="聚焦输入框">
    </div>

    <div class="ui disabled input">
        <input type="text" placeholder="输入框">
    </div>

    <div class="ui transparent input">
        <input type="text" placeholder="透明输入框">
    </div>
    <br><br><br><br>

    <div class="ui mini input">
        <input type="text" placeholder="微小的输入框">
    </div>
    <div class="ui small input">
        <input type="text" placeholder="稍微大的输入框">
    </div>
    <div class="ui large input">
        <input type="text" placeholder="大一点的输入框">
    </div>
    <div class="ui huge input">
        <input type="text" placeholder="巨大的输入框">
    </div>
    <div class="ui massive input">
        <input type="text" placeholder="最大的输入框">
    </div>
    <br><br><br><br>

    <!-- 输入框的组合使用 -->
    <div class="ui error input">
        <input type="text" placeholder="错误">
    </div>
    <!-- icon：让标签在输入框里面 -->
    <div class="ui icon input">
        <input type="text" placeholder="图标在右边">
        <i class="user icon"></i>
    </div>
    <div class="ui left icon input">
        <input type="text" placeholder="图标在左边">
        <i class="user icon"></i>
    </div>
    
    <!-- 加载类型的输入框 -->
    <div class="ui icon loading input">
        <input type="text" placeholder="搜索">
        <i class="search icon"></i>
    </div>

    <!-- 标签和输入框组合 -->
    <div class="ui labeled input">
        <label for="amount" class="ui label">数量</label>
        <input type="text" placeholder="">
        <label for="" class="ui label">0.00</label>
    </div>

    <!-- 图标和标签和输入框组合 -->
    <div class="ui left icon right labeled input">
        <i class="tags icon"></i>
        <input type="text" placeholder="标签">
        <label for="name" class="ui label">标签</label>
    </div>

    <!-- 输入框和按钮的组合，按钮是可以点击的 -->
    <div class="ui action input">
        <input type="text" placeholder="搜索">
        <button class="ui blue button">搜索</button>
    </div>
</body>
</html>
```





## 标题（H）

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/semantic-ui/2.2.10/semantic.min.css">
    <script src="https://cdn.jsdelivr.net/semantic-ui/2.2.10/semantic.min.js"></script>
    <title>标题的写法</title>
</head>
<body>
    <!-- 第一种写法 -->
    <h1 class="ui header">H1</h1>
    <h2 class="ui header">H2</h2>
    <h3 class="ui header">H3</h3>
    <h4 class="ui header">H4</h4>
    <h5 class="ui header">H5</h5>

    <!-- 第二种写法 -->
    <div class="ui tiny header">tiny</div>
    <div class="ui small header">small</div>
    <div class="ui medium header">medium</div>
    <div class="ui large header">large</div>
    <div class="ui huge header">huge</div>

    <!-- 标题的颜色：对应的英文单词 -->
    <h4 class="ui red header">red</h4>
    <h4 class="ui teal header">teal</h4>
    <h4 class="ui green header">green</h4>

    <!-- inverted：反色的强调颜色 -->
    <div class="ui inverted segment">
        <h4 class="ui red header">red</h4>
    <h4 class="ui teal header">teal</h4>
    <h4 class="ui green header">green</h4>
    </div>

    <!-- 标题的组合 -->
    <!-- 副标题下放一个子标题 -->
    <!-- 一级标题到五级标题都有这种写法，在这里就不举例了 -->
    <h1 class="ui header">
        H1 <div class="sub header">Sub Header</div>
    </h1>

    <!-- 图片的大小和标题一样 -->
    <h2 class="ui header">
        <img src="./20.jpg" alt="" class="ui image">
        <div class="content">测试</div>
    </h2>
    <h1 class="ui header">
        <img src="./20.jpg" alt="" class="ui image">
        <div class="content">测试</div>
    </h1>

    <!-- 图片和标题组合 -->
    <h3 class="ui header">
        <i class="plug icon"></i>
        <div class="content">测试</div>
    </h3>

    <!-- 标题下面出现下划线 -->
    <h4 class="ui dividing header">测试</h4>

    <!-- 块样式 -->
    <h5 class="ui block header">测试</h5>

    <!-- 让图标标题组合在一起 -->
    <h2 class="ui icon header">
        <i class="settings icon"></i>
        <div class="content">设置</div>
        <div class="sub header">设置你的账号信息</div>
    </h2>
</body>
</html>
```







## 片段（segment）

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/semantic-ui/2.2.10/semantic.min.css">
    <script src="https://cdn.jsdelivr.net/semantic-ui/2.2.10/semantic.min.js"></script>
    <title>分段的样式</title>
</head>
<body>
    <!-- 标准分段 -->
    <div class="ui segment">
        <p>测试</p>
    </div>

    <!-- 浮雕类型的，往上凸显的样式 -->
    <div class="ui raised segment">
        <p>测试</p>
    </div>

    <!-- 堆叠式 -->
    <div class="ui stacked segment">
        <p>测试</p>
    </div>

    <!-- 更加突出的堆叠方式 -->
    <div class="ui tall stacked segment">
        <p>测试</p>
    </div>

    <!-- 另一种堆叠样式,x形 -->
    <div class="ui piled segment">
        <p>测试</p>
    </div>

    <!-- 次要的分段 -->
    <div class="ui secondary segment">
        <p>测试</p>
    </div>

    <!-- 更加次要的分段 -->
    <div class="ui tertiary segment">
        <p>测试</p>
    </div>

    <!-- 颜色：加上对应的单词即可 -->
    <div class="ui red segment">
        <p>测试</p>
    </div>
    <!-- 反色 -->
    <div class="ui inverted red segment">
        <p>测试</p>
    </div>

    <!-- 次要的反色 -->
    <div class="ui secondary inverted red segment">
        <p>测试</p>
    </div>
    <!-- 更次要的反色 -->
    <div class="ui tertiary inverted red segment">
        <p>测试</p>
    </div>
    <br><br><br><br>

    <!-- 分段的组合 -->
    <div class="ui stacked segments">
        <div class="ui segment">
            <p>测试</p>
        </div>
        <div class="ui segment">
            <p>测试</p>
        </div>
        <div class="ui segment">
            <p>测试</p>
        </div>
    </div>
    <br><br><br>

    <!-- 分段的预先展示的样式 -->
    <div class="ui placeholder segment">
        <div class="ui icon header">
          <i class="search icon"></i>
          我们没有任何文件符合您查询
        </div>
        <div class="inline">
          <div class="ui primary button">清除查询</div>
          <div class="ui button">添加文件</div>
        </div>
      </div>

      <!-- 加载的分段 -->
      <div class="ui loading segment">
          <p>测试</p>
      </div>

      <!-- 禁用的分段 -->
      <div class="ui disable segment">
          <p>测试</p>
      </div>

      <!-- 圆角的分段 -->
      <!-- 要注意的问题：我们要手动调整宽高，不然是椭圆形的 -->
      <div class="ui circular segment" style="width: 150px;
       height: 150px;">
          <h2 class="ui header">测试</h2>
          <div class="sub header">100元</div>
      </div>

      <div class="ui right aligned segment">右边</div>
      <div class="ui left aligned segment">左边</div>
      <div class="ui center aligned segment">居中</div>
      
</body>
</html>
```

