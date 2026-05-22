一、Flask框架介绍

### 1	介绍

Flask对比其他web框架：短小精悍

中文文档（http://docs.jinkan.org/docs/flask/）



## 2	常用扩展包

扩展列表：http://flask.pocoo.org/extensions/

-   Flask-SQLalchemy：操作数据库；
-   Flask-script：插入脚本；
-   Flask-migrate：管理迁移数据库；
-   Flask-Session：Session存储方式指定；
-   Flask-WTF：表单；
-   Flask-Mail：邮件；
-   Flask-Bable：提供国际化和本地化支持，翻译；
-   Flask-Login：认证用户状态；
-   Flask-OpenID：认证；
-   Flask-RESTful：开发REST API的工具；
-   Flask-Bootstrap：集成前端Twitter Bootstrap框架；
-   Flask-Moment：本地化日期和时间；
-   Flask-Admin：简单而可扩展的管理接口的框架





# 二、环境和工程搭建

## 1	环境搭建

### ①virtualenvwrapper-win

```sh
pip/pip3 install virtualenvwrapper-win
```



### ②安装virtualenv

```python
pip/pip3 install virtualenv
```

使用`mkvirtualenv -h`命令查看是否安装成功,-h是帮助命令，mkvirtualenv是用来创建虚拟环境的命令



### ③虚拟环境的相关操作

```sh
# 创建虚拟环境
mkvirtualenv 环境名称 -p 根据自已的python环境指定使用python环境[python2或python3]
# 删除虚拟环境
rmvirtualenv 环境名称
# 进入虚拟环境、查看所有虚拟环境
workon 环境名称
# 退出虚拟环境
deactivate 
```



## 2	工程搭建

### ①安装Flask

```shell
pip/pip3 install flask
```



### ②Flask的入门示例

```python
from flask import Flask

app = Flask(__name__)

@app.route('/')
def index():
    return 'hello,world'

if __name__ == '__main__':
    app.run()
```

**执行结果** --> 默认是在5000端口开启

![image-20211110194803507](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211110194805.png)

![image-20211110200232094](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211110200244.png)



## 3Flask入门程序参数说明

Flask 程序实例在创建的时候，需要默认传入当前 Flask 程序所指定的包(模块)，接下来就来详细查看一下 Flask 应用程序在创建的时候一些需要我们关注的参数：

-   import_name
    -   Flask程序所在的包(模块)，传  __name__ 就可以
    -   其可以决定 Flask 在访问静态文件时查找的路径
-   static_url_path
    -   静态文件访问路径，可以不传，**默认为： / + static_folder**
-   static_folder
    -   静态文件存储的文件夹，可以不传，**默认为  static**
-   template_folder
    -   模板文件存储的文件夹，可以不传，默认为  templates

![image-20211110200650023](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211110200651.png)



### ①修改默认静态文件位置

首先我们在默认创建一个static文件，存放一个名为123.png的图片，是可以访问到的

**static_url_path参数**

**这个参数的作用就是静态资源路径映射**

**eg**：我们static目录下有123.png的图片，我们给这个参数设置为/aaa/bbb，那么当我们访问/aaa/bbb这个路径的时候就相当于访问到了static目录

**代码实现**

```python
from flask import Flask

app = Flask(__name__, static_url_path='/aaa/bbb')

@app.route('/')
def index():
    return 'hello,world'

if __name__ == '__main__':
    app.run()
```

**结果显示** --> 就是找到了static目录下的123.png的图片

![image-20211110201528906](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211110201530.png)

==**注意**：`static_url_path='/aaa/bbb'`里面的参数的路径一定要有`/`，不然就会报错==



### ②修改默认的静态资源存放位置

我们的静态资源是默认放在static目录下的，我们可以修改为我们想要的目录下 -->  放在bbb目录中

![image-20211110202421450](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211110202422.png)

```python
from flask import Flask

app = Flask(__name__, static_url_path='bbb', static_folder='bbb')

@app.route('/')
def index():
    return 'hello,world'

if __name__ == '__main__':
    app.run()
```

**执行结果** --> 请求过来的是bbb目录下的那张图片

**注意**：不能使用嵌套目录，静态目录的文件夹只能是在根目录下，不然就会出现找不到的情况



### ③应用程序配置参数

Flask将配置信息保存到了 **app.config 属性**中，该属性可以按照字典类型进行操作。

#### 1.读取

-   app.config.get(name)
-   app.config[name]



#### 2.设置

-   **从配置对象中加载信息**

    **app.config.from_object(配置对象)**

    ```python
    from flask import Flask
    
    class DefaultConfig(object):
        '''默认配置'''
        SECRET_KEY = 'Tadsasf678s6f7s67a8fd76ds7f'
    
    app = Flask(__name__)
    app.config.from_object(DefaultConfig)
    
    @app.route('/')
    def index():
        return app.config['SECRET_KEY']
    ```

    ![image-20211110204016791](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211110204017.png)

    **应用场景**：
    作为默认配置写在程序代码中
    可以继承

    

-   **从配置文件中加载**

    **app.config.from_pyfile(配置文件)**
    新建一个配置文件setting.py

    ```python
    # setting.py
    SECRET_KEY = '我是python文件中的配置信息'
    ```

    ```python
    from flask import Flask
    
    app = Flask(__name__)
    # 加载配置文件
    app.config.from_pyfile("setting.py")
    
    @app.route('/')
    def index():
        return app.config['SECRET_KEY']
    
    if __name__ == '__main__':
        app.run()
    ```

    ![image-20211110204609199](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211110204610.png)

    

-   **从环境变量中加载**

    ![image-20211110205122846](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211110205123.png)

    它的底层还是使用了`os.environ`去获取环境变量，所以我们也可以直接使用`os.environ`来获取

    

    **Linux环境下实验**

    ```shell
    # 终端执行命令
    export PROJECT_SETTING='~/setting.py'
    ```

    再执行下面代码

    ```python
    app = Flask(__name__)
    app.config.from_envvar('PROJECT_SETTING', silent=True)
    @app.route("/")
    def index():
      return app.config['SECRET_KEY']
    ```



### ④项目中的常用方式

使用工厂模式创建Flask app，并结合使用配置对象与环境变量加载配置

-   使用配置对象加载默认配置
-   使用环境变量加载不想出现在代码中的敏感配置信息





### ⑤app.run参数

可以指定运行的主机IP地址，端口，是否开启调试模式

![image-20211110220502837](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211201213903.png)

```python
app.run(host="0.0.0.0", port=5000, debug = True)
```

关于DEBUG调试模式

1. 程序代码修改后可以自动重启服务器
2. 在服务器出现相关错误的时候可以直接将错误信息返回到前端进行展示



### ⑥服务器启动方式

在1.0版本之后，Flask调整了服务器的启动方式，由代码编写 app.run() 语句调整为命令 flask run 启动。





# 三、路由与蓝图

所谓的蓝图就是模板文件



## 1	路由

```python
@app.route("/hotdas")
def view_func():
	return "hello world"
```



### ①查询路由信息

-   **命令方式**

    ```shell
    flask routes
    ```

    ```sh
    Endpoint Methods Rule
    --------  -------  -----------------------
    index   GET   /
    static  GET   /static/
    ```

-   **在程序中获取**

    在应用中的url_map属性中保存着整个Flask应用的路由映射信息，可以通过读取这个属性获取路由信息

    ```python
    print(app.url_map)
    ```

    可以遍历这个属性获取到单个路由的信息

    ```python
    for rule in app.url_map.iter_rules():
    	print('name={} path={}'.format(rule.endpoint, rule.rule))
    ```



**需求**：通过访问`/`地址，以json的方式返回应用内所有的路由信息

**代码实现**

```python
import json
from flask import Flask

app = Flask(__name__)

@app.route("/")
def index():
    return json.dumps({rule.endpoint : rule.rule
                       for rule in app.url_map.iter_rules()})
```



### ②指定请求方式

在 Flask 中，定义路由其默认的请求方式为：

-   GET
-   OPTIONS(自带)
-   HEAD(自带)

利用 methods 参数可以自己指定一个接口的请求方式

```python
from flask import Flask
# get请求
app = Flask(__name__)

@app.route("/", methods=['GET'])
def index():
    return "get请求"

@app.route("/post", methods=['POST'])
def post():
    return "post请求"

# methods的列表中如果有多个，那么就是多种访问方式都是可行的
@app.route('/getAndPost', methods=['GET', 'POST'])
def getAndPost():
    return "getAndPost"
```





## 2	蓝图

### ①简介

在Flask中，使用蓝图Blueprint来分模块组织管理。
蓝图实际可以理解为是一个存储一组视图方法的容器对象，其具有如下特点：

-   一个应用可以具有多个Blueprint
-   可以将一个Blueprint注册到任何一个未使用的URL下比如 “/user”、“/goods”
-   Blueprint可以单独具有自己的模板、静态文件或者其它的通用操作方法，它并不是必须要实现应
-   用的视图和函数的
-   在一个应用初始化时，就应该要注册需要使用的Blueprint

但是一个Blueprint并不是一个完整的应用，它不能独立于应用运行，而必须要注册到某一个应用中。



### ②使用方式

**代码实现**

```python
from flask import Flask
# 导入蓝图的模块
from flask import Blueprint

app = Flask(__name__)
# 使用蓝图的三个步骤
# 1 创建蓝图对象
user_bp = Blueprint('user', __name__)
# 2 在这个蓝图对象上进行操作,注册路由,指定静态文件夹,注册模版过滤器
@user_bp.route('/')
def user_profile():
    return 'user_profile'
# 3 在应用对象上注册蓝图对象
app.register_blueprint(user_bp)
```



**单个文件蓝图**

可以将创建蓝图对象与定义视图放到一个文件中 。

**目录（包）蓝图**

对于一个打算包含多个文件的蓝图，通常将创建蓝图对象放到Python包的 `__init__.py` 文件中

![image-20211112143812940](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211112152842.png)

**代码实现**

1.  创建一个名为goods的包，然后再`__init__`文件下进行初始化

    ```python
    from flask import Flask
    from Flask.蓝图.goods.views import user_bp
    
    # 创建Flask对象
    app = Flask(__name__)
    # 注册蓝图
    app.register_blueprint(user_bp)
    ```

2.  创建view.py，使用蓝图对象创建路由

    ```python
    from flask import Blueprint
    
    user_bp = Blueprint('user', __name__)
    
    @user_bp.route('/')
    def index():
        return '欢迎来到首页'
    ```

3.  创建manage.py文件来启动服务

    ```python
    from Flask.蓝图.goods import app
    
    if __name__ == '__main__':
        app.run()
    ```



### ③扩展用法

#### ①指定蓝图的url前缀

在应用中注册蓝图时使用 url_prefix 参数指定

```python
app.register_blueprint(user_bp,  url_prefix='/user')
```

![image-20211112144030561](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211112152835.png)

#### ②蓝图内部静态文件

和应用对象不同，**蓝图对象创建时不会默认注册静态目录的路由**。需要我们在创建时指定 static_folder参数。
下面的示例将蓝图所在目录下的static_admin目录设置为静态目录

```python
# 将静态资源目录设置为static_admin，映射路径为/lib
user_bp = Blueprint('user', __name__,
                    static_folder='static_admin',
                    static_url_path='/lib')
```

![image-20211112152829479](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211112152830.png)



#### ③蓝图内部模板目录

蓝图对象默认的模板目录为系统的模版目录，可以在创建蓝图对象时使用 template_folder 关键字参数设置模板目录

```python
admin = Blueprint('admin',__name__,template_folder='my_templates')
```



# 四、请求与相应

## 1	URL路径参数（动态路由）

### ①示例

例如，有一个请求访问的接口地址为 /users/123 ，其中123实际上为具体的请求参数，表明请求123号用户的信息。此时如何从url中提取出123的数据？

**Flask采用转换器语法**：

```python
from flask import Flask

app = Flask(__name__)

@app.route('/user/<user_id>')
def user_info(user_id):
    print(type(user_id))
    return f'id是{user_id}'
```

**说明**：`<>`是一个转换器，默认是字符串类型，我们访问的路径就是/user/6，那么这个参数的值就是6



### ②Flask其他类型的转换器

```python
DEFAULT_CONVERTERS = {
    'default':      UnicodeConverter,
    'string':      UnicodeConverter,
    'any':        AnyConverter,
    'path':       PathConverter,
    'int':        IntegerConverter,
    'float':       FloatConverter,
    'uuid':       UUIDConverter,
}
```

将上面的例子以整型匹配数据，可以如下使用：

```python
from flask import Flask
app = Flask(__name__)

@app.route('/user/<int:user_id>')
def user_info(user_id):
    print(type(user_id))
    return 'hello user {}'.format(user_id)

# 这个表示最小的是1，小于1的就会不匹配
# @app.route('/user/<int(min=1):user_id>')
# def user_info(user_id):
#     print(type(user_id))
#     return 'hello user {}'.format(user_id)
```



### ③自定义转换器

如果遇到需要匹配提取 /sms_codes/18512345678 中的手机号数据，Flask内置的转换器就无法满足需求，此时需要自定义转换器。



#### 1.定义方法

自定义转换器三步走

1.  **创建转换器类，保存匹配是的正则表达式**

    ```python
    from werkzeug.routing import BaseConverter
    class MobileConverter(BaseConverter):
        '''手机号格式'''
        regex = r'1[3-9]\d{9}'  # 重写父类中的regex属性
    ```

    注意 regex 名字固定

2.  将自定义的转换器告知Flask应用

    ```python
    app = Flask(__name__)
    # 放入到转换器容器中，转换器的名字就是容器的key
    app.url_map.converters['mobile'] = MobileConverter
    ```

    

3.  在使用转换器的地方定义使用

    ```python
    from flask import Flask
    from werkzeug.routing import BaseConverter
    class MobileConverter(BaseConverter):
        '''手机号格式'''
        regex = r'1[3-9]\d{9}'  # 重写父类中的regex属性
    
    app = Flask(__name__)
    # 放入到转换器容器中，转换器的名字就是容器的key
    app.url_map.converters['mobile'] = MobileConverter
    
    @app.route('/<mobile:phone_num>')
    def user_info(phone_num):
        print(type(phone_num))
        return '手机号为 {}'.format(phone_num)
    ```

    ![image-20211112152806195](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211112152807.png)

结果显示 --> 是可以的



#### 2.其他参数

如果想要获取其他地方传递的参数，可以通过Flask提供的request对象来读取。不同位置的参数都存放在request的不同属性中

![image-20211112153125540](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211112153126.png)

**eg**： 想要获取请求 /user?user_id=1 中 user_id的参数，可以按如下方式使用：

```python
from flask import Flask
from flask import request
app = Flask(__name__)

@app.route('/user')
def user_info():
    user_id = request.args.get('user_id')
    return 'hello user {}'.format(user_id)
```

![image-20211112153603631](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211112153604.png)



### ④上传图片

客户端上传图片到服务器，并保存到服务器中

上传文件必须要有multipart/form-data请求头或者表单中有`enctype="multipat/form-data"`属性

![image-20211115100143350](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211115100145.png)

**代码实现**

```html
<form action="/upload" enctype="multipart/form-data" method="post">
    <input type="file" name="file"/>
    <input type="submit" value="upload">
</form>
```

后台响应

```python
from flask import Flask, render_template
from flask import request
app = Flask(__name__)

@app.route('/')
def index():
    return render_template("toUpload.html")

@app.route('/upload', methods=['POST'])
def upload():
    f = request.files['file']
    f.save('./123.png')
    return 'ok'
```





## 2	处理响应

### ①返回模板

首先，在模板存放的文件夹下创建一个index.html文件，这个就是我们的模板文件，**默认是templates**，这个是可以修改的，这里我使用默认的

==**注意**：一定是放在模板文件存放处，不然会报找不到对应模板的错误==

```html
# index.html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
</head>
<body>
    我的模板html内容
    <br/>{{ name }}
    <br/>{{ age }}
</body>
</html>
```

**说明**：`{{}}`是flask的模板语法，用来取出对应的数据的

**进行路由跳转和数据响应**

```python
from flask import Flask
from flask import render_template

app = Flask(__name__)

@app.route('/')
def index():
    name = "小智"
    age = 12
    return render_template("index.html", name=name, age=age)
```



## 3	重定向

```python
from flask import Flask
from flask import redirect
app = Flask(__name__)

@app.route('/demo2')
def demo2():
    return redirect('https://www.hotdas.com')
```



## 4	返回JSON

```python
from flask import Flask
from flask import jsonify
app = Flask(__name__)

@app.route('/json')
def demo2():
    json_dict = {
        'name': 'xiaozhi',
        'age': 20
    }
    return jsonify(json_dict)
```



## 5	自定义状态码和响应头

### ①元组方式

可以返回一个元组，这样的元组必须是 (response, status, headers) 的形式，且至少包含一个元素。
status 值会覆盖状态代码， headers 可以是一个列表或字典，作为额外的消息标头值。

```python
from flask import Flask
app = Flask(__name__)

@app.route("/")
def index():
    return '状态码为666', 666, {'p': 'python'}
```

![image-20211112171516001](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211112171517.png)

![image-20211112171649023](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211112171725.png)



### ②make_response方式

```python
from flask import Flask
from flask import make_response
app = Flask(__name__)

@app.route('/')
def index():
    resp = make_response('make_response测试')
    resp.headers['Hotdas'] = 'python'
    resp.status = "400 not found"
    return resp
```

![image-20211112172120866](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211112172122.png)



# 五、cookie和session

## 1	cookie

### ①设置

```python
from flask import Flask, make_response

app = Flask(__name__)

@app.route("/cookie")
def set_cookie():
    # make_response() --> 创建一个响应对象
    resp = make_response('set cookie ok')
    # 设置cookie，max_age参数设置有效期，单位为s
    resp.set_cookie('username', 'xiaozhi', max_age=3600)
    return resp
```



### ②读取

```python
from flask import Flask
from flask import request

app = Flask(__name__)

@app.route('/get_cookie')
def get_cookie():
    # 首先是获取cookie对象，然后使用get函数获取我们需要的cookie值
    resp = request.cookies.get('username')
    return resp
```



### ③删除

```python
from flask import Flask
from flask import make_response

app = Flask(__name__)

@app.route('/delete_cookie')
def delete_cookie():
    resp = make_response('hello world')
    resp.delete_cookie("username")
    return resp
```





## 2	session

需要先设置SECRET_KEY，这个是必须要有的，不然就会报错

```python
from flask import Flask
from flask import session
# 创建配置类
class DefaultConfig(object):
    SECRET_KEY = "sdf8sdfy82hf"

app = Flask(__name__)
# 加载配置类
app.config.from_object(DefaultConfig)
# 或者直接设置
# app.secret_key = 'sdf8sdfy82hf'
```

![image-20211114161334701](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211114161336.png)



### ①设置

```python
@app.route('/set_session')
def set_session():
    session['username'] = 'xiaozhi'
    return 'set session ok'
```



### ②读取

```python
from flask import session
from flask import Flask

app = Flask(__name__)
# 创建配置类
class DefaultConfig(object):
    SECRET_KEY = "sdf8sdfy82hf"
app.config.from_object(DefaultConfig)

@app.route('/get_session')
def get_session():
    session['username'] = 'xiaozhi'
    username = session.get('username')
    return f'get session username --> {username}'
```



### ③思考

Flask将session数据保存到了哪里





# 六、请求钩子与上下文

## 1	异常处理

### ①HTTP异常主动抛出

-   **abort 方法**：

    -   抛出一个给定状态代码的 HTTPException 或者 指定响应，

        **例如**：想要用一个页面未找到异常来终止请求，你可以调用 abort(404)。

-   **参数**：

    -   code – HTTP的错误状态码

```python
from flask import Flask, abort

app = Flask(__name__)
@app.route('/')
def index():
    abort(500)
    return '异常错误'
```

![image-20211114163312395](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211114163313.png)

**抛出状态码的话，只能抛出 HTTP 协议的错误状态码**



### ②捕获错误

-   **errorhandler 装饰器**
    -   注册一个错误处理程序，当程序抛出指定错误状态码的时候，就会调用该装饰器所装饰的方法
-   参数：
    -   **code_or_exception** – HTTP的错误状态码或指定异常
-   例如统一处理状态码为500的错误给用户友好的提示：



#### 1.捕获指定的错误状态码

```python
from flask import Flask, abort

app = Flask(__name__)
@app.route('/')
def index():
    abort(500)
    return '异常错误'

'''
	1.当出现对应状态码的时候就会执行下面装饰器的方法
	2.捕获异常，然后跳转到我们自定义的错误页面
	3.只能捕获指定状态码的异常，比如500，就只能捕获500的异常
'''
@app.errorhandler(500)
def internal_server_error(e):
    print(e)
    return '服务器搬家了'
```



#### 2.捕获指定异常

```python
from flask import Flask

app = Flask(__name__)
@app.route('/')
def index():
    return 1 / 0

# 传入对应的异常对象，只能捕获对应的异常，其他的是不能捕获的
@app.errorhandler(ZeroDivisionError)
def zero_division_error(e):
    return "分子不能为0"
```



## 2	请求钩子

在客户端和服务器交互的过程中，有些准备工作或扫尾工作需要处理，比如：

-   在请求开始时，建立数据库连接；
-   在请求开始时，根据需求进行权限校验；
-   在请求结束时，指定数据的交互格式；
-   为了让每个视图函数避免编写重复功能的代码，Flask提供了通用设施的功能，即请求钩子。

请求钩子是通过装饰器的形式实现，**Flask支持如下四种请求钩子**：

-   **before_first_request**
    -   在处理第一个请求前执行
-   **before_request**
    -   在每次请求前执行
    -   如果在某修饰的函数中返回了一个响应，视图函数将不再被调用
-   **after_request**
    -   如果没有抛出错误，在每次请求后执行
    -   接受一个参数：视图函数作出的响应
    -   在此函数中可以对响应值在返回之前做最后一步修改处理
    -   需要将参数中的响应在此参数中进行返回
-   **teardown_request**：
    -   在每次请求后执行
    -   接受一个参数：错误信息，如果有相关错误抛出

==**这个可以理解成是一个拦截器，前置和后置处理**==

**代码实现**

```python
from flask import Flask

app = Flask(__name__)
# 在第一次请求调用，可以做一些初始化操作
@app.before_first_request
def before_first_request():
    print("before_first...")

# 在每次请求前执行
@app.before_request
def before_request():
    print('before_request...')

# 请求之后执行
@app.after_request
def after_request(response):
    print('after_request...')
    # 可以设置响应的一些信息
    response.headers["Content-Type"] = "application/json"
    return response

# 每一次请求之后都会调用，接收一个参数，参数是服务器出现的错误信息
@app.teardown_request
def teardown_request(response):
    print('teardown_request...')

@app.route("/")
def index():
    return "hello world"
```

**执行结果**

第一次打印

```
before_first...
before_request...
after_request...
teardown_request...
```

第二次打印

```
before_request...
after_request...
teardown_request...
```





## 3	上下文

上下文就是语境（语言环境），它保存了我们当前应用的一些对象和数据，我们可以调用这些对象来访问我们保存的数据

Flask中有两种上下文，**请求上下文和应用上下文**



### ①请求上下文（request context）

高大上的名字，其实就是几个对象

-   **request**

    封装了HTPP请求的内容，可以获取到对应的参数

    **eg**：获取get请求的参数 --> uesr = request.args.get('user')

-   **session**

    用来记录请求会话中的信息，针对的是用户信息。

    **eg**：session['name'] = user.id，可以记录用户信息。还可以通过session.get('name')获取用户信息。



### ②应用上下文(application context)

它的字面意思是 应用上下文，但**它不是一直存在的**，它只是request context 中的一个对 app 的代理(人)，所谓local proxy。它的**作用主要是帮助 request 获取当前的应用**，它是伴 request 而生，随request 而灭的。
**应用上下文对象有**：current_app，g

#### 1.current_app

应用程序上下文,**用于存储应用程序中的变量**，可以通过current_app.name打印当前app的名称，也可以在current_app中存储一些变量，例如：

-   应用的启动脚本是哪个文件，启动时指定了哪些参数
-   加载了哪些配置文件，导入了哪些配置
-   连了哪个数据库
-   有哪些public的工具类、常量
-   应用跑再哪个机器上，IP多少，内存多大

**示例代码**

```python
from flask import Flask, current_app

app1 = Flask(__name__)
app2 = Flask(__name__)

# 以redis客户端对象为例
# 用字符串表示创建的redis客户端
# 为了方便在各个视图中使用，将创建的redis客户端对象保存到flask app中，
# 后续可以在视图中使用current_app.redis_cli获取
app1.redis_cli = 'app1 redis client'
app2.redis_cli = 'app2 redis client'

@app1.route('/route11')
def route11():
    return current_app.redis_cli
@app1.route('/route12')
def route12():
    return current_app.redis_cli
@app2.route('/route21')
def route21():
    return current_app.redis_cli
@app2.route('/route22')
def route22():
    return current_app.redis_cli

if __name__ == '__main__':
    app1.run()
    # app2.run()
```

先启动app1查看结果，然后启动app2查看结果，发现这两个app是相互独立不影响的，这个就是应用上下文，**只在当前应用生效，其他应用是不生效的**

**作用**

current_app 就是当前运行的flask app，在代码不方便直接操作flask的app对象时，可以操作current_app
current_app 就等价于操作flask app对象



#### 2.g

##### ①入门示例

**g 作为 flask 程序全局的一个临时变量**，充当中间媒介的作用，我们可以通过它在一次请求调用的多个函数间传递一些数据。每次请求都会重设这个变量（重新创建）。

这个东西类似于javaEE中的request域

**示例代码**

```python
from flask import Flask, g

app = Flask(__name__)
def db_query():
    user_id = g.user_id
    user_name = g.user_name
    return 'user_id={} user_name={}'.format(user_id, user_name)

@app.route('/')
def get_user_profile():
    g.user_id = 123
    g.user_name = 'xiaozhi'
    return db_query()
```

![image-20211115165508388](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211115165510.png)



##### ②g对象与请求钩子的综合案例

**需求**:

-   构建认证机制
-   对于特定视图可以提供强制要求用户登录的限制
-   对于所有视图，无论是否强制要求用户登录，都可以在视图中尝试获取用户认证后的身份信息

**代码实现**

```python
from flask import Flask, g, abort

app = Flask(__name__)
@app.before_request
def isLogin():
    # 请求之前验证是否已经登录
    # 假设通过了校验，获取到了对应的用户信息
    g.user_id = 123
    print(g.user_id)

@app.route('/')
def index():
    return f'user_id --> {g.user_id}'

# 登录校验
def login_required(fun):
    def wrapper(*args, **kwargs):
        if g.user_id is not None:
            return fun(*args, **kwargs)
        else:
            # 报500的错误
            abort(500)
    return wrapper

@app.route('/profile')
@login_required
def profile():
    return f'welcome to profile --> user_id = {g.user_id}'
```

![image-20211115171005182](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211115171006.png)



### ③app_context 与 request_context

**思考**？

在Flask程序未运行的情况下，调试代码时需要使用 current_app 、 g 、 request 这些对象，会不会有问题？该如何使用？

#### 1.app_context

app_context`为我们提供了应用上下文环境，允许我们在外部使用应用上下文current_app、g

**可以通过 with 语句进行使用**

**代码实现**

```python
from flask import Flask
from flask import  current_app

app = Flask(__name__)
app.redis_cli = 'redis client'

# print(current_app.redis_cli)    # 报错，没有应用上下文
with app.app_context():
    print(current_app.redis_cli)    # redis client
```



#### 2.request_context

request_context`为我们提供了请求上下文环境，允许我们在外部使用请求上下文request、session

**可以通过with语句使用**

```python
from flask import Flask, request

app = Flask(__name__)
# print(request.args)     # 报错，没有上下文环境
environ = {'wsgi.version':(1,0), 'wsgi.input': '', 'REQUEST_METHOD': 'GET',
'PATH_INFO': '/', 'SERVER_NAME': 'hotdas server', 'wsgi.url_scheme': 'http',
'SERVER_PORT': '80'}    # 模拟解析客户端请求之后的wsgi字典数据
with app.request_context(environ):
    print(request.path)
    print(request.url)
```





# 七、Flask-RESTFUL

## 1	快速入门

### ①安装

```shell
pip install flask-restful
```



### ②入门示例

```python
from flask import Flask
from flask_restful import Resource, Api

app = Flask(__name__)
# 创建api对象
api = Api(app)
# 创建一个类继承Resource
class HelloResource(Resource):
    def get(self):
        return {'method': 'get'}

    def post(self):
        return {'method': 'post'}

# 将类添加到资源中
api.add_resource(HelloResource, '/')
```





## 2	关于视图

### ①为路由起名

通过endpoint参数为路由起名

```python
from flask import Flask
from flask_restful import Resource, Api

app = Flask(__name__)
# 创建api对象
api = Api(app)
# 创建一个类继承Resource
class HelloResource(Resource):
    def get(self):
        return {'method': 'get'}

    def post(self):
        return {'method': 'post'}

# 将类添加到资源中
api.add_resource(HelloResource, '/', endpoint='HelloWorld')
```

![image-20211115194111790](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211115194113.png)



### ②蓝图中使用

```python
from flask import Flask, Blueprint
from flask_restful import Api, Resource

app = Flask(__name__)
user_bp = Blueprint('user', __name__)
user_api = Api(user_bp)

class UserProfileResource(Resource):
    def get(self):
        return {"method": 'get'}

user_api.add_resource(UserProfileResource, '/users/profile')
app.register_blueprint(user_bp)
```



### ③装饰器

使用**`method_decorators`**添加装饰器

-   **为类视图中的所有方法添加装饰器**

    ```python
    from flask import Flask
    from flask_restful import Api, Resource
    
    def decorator1(func):
        def wrapper(*args, **kwargs):
            print('decorator1')
            return func(*args, **kwargs)
        return wrapper
    
    def decorator2(func):
        def wrapper(*args, **kwargs):
            print('decorator2')
            return func(*args, **kwargs)
        return wrapper
    
    class DemoResource(Resource):
        method_decorators = [decorator1, decorator2]
    
        def get(self):
            return {'method': 'get view'}
        def post(self):
            return {'method': 'post view'}
    
    app = Flask(__name__)
    api = Api(app)
    api.add_resource(DemoResource, "/")
    ```

    

-   **为类视图中不同的方法添加不同的装饰器**

    ```python
    from flask import Flask
    from flask_restful import Api, Resource
    
    def decorator1(func):
        def wrapper(*args, **kwargs):
            print('decorator1 -- > get')
            return func(*args, **kwargs)
        return wrapper
    
    def decorator2(func):
        def wrapper(*args, **kwargs):
            print('decorator2 --> post')
            return func(*args, **kwargs)
        return wrapper
    
    class DemoResource(Resource):
        method_decorators = {
            'get': [decorator1],
            'post': [decorator2],
        }
    
        def get(self):
            return {'method': 'get view'}
        def post(self):
            return {'method': 'post view'}
    
    app = Flask(__name__)
    api = Api(app)
    api.add_resource(DemoResource, "/")
    ```

    ![image-20211115201912655](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211115201913.png)



## 3	关于请求处理

### ①RequestParser类

Flask-RESTful 提供了 **RequestParser 类**，用来帮助我们**检验和转换请求数据**。

**eg**：`location='args'`就表示校验请求url中的数据

**代码示例**

```python
from flask import Flask
from flask_restful import Api, Resource
from flask_restful.reqparse import RequestParser

app = Flask(__name__)
api = Api(app)
class IndexResource(Resource):
    def get(self):
        # 1. 创建RequestParser实例
        parser = RequestParser()
        
        # 2. 添加验证参数
        # 第一个参数： 传递的参数的名称
        # 第二个参数（location）： 传递参数的方式
        # 第三个参数（type）： 验证参数的函数(可以自定义验证函数)
        parser.add_argument('username', location='args', type=str)

        # 3. 验证数据
        # args是一个字典
        args = parser.parse_args()

        # 4. 获取验证后的数据
        username = args.get('username')

        return 'get ...{}'.format(username)

api.add_resource(IndexResource, '/')
```

**访问** --> `http://127.0.0.1:5000/?username=xiaozhi`

![image-20211115211047516](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211115211048.png)



### ②使用步骤

1. 创建 RequestParser 对象
2. 向 RequestParser 对象中添加需要检验或转换的参数声明
3. 使用 parse_args() 方法启动检验处理
4. 检验之后从检验结果中获取参数时可按照字典操作或对象属性操作

```python
args.rate
或
args['rate']
```



### ③参数说明

#### 1.required

描述请求是否一定要携带对应参数，**默认值为False**

-   True 强制要求携带

    若未携带，则校验失败，向客户端返回错误信息，**状态码400**

-   False 不强制要求携带

    若不强制携带，在客户端请求未携带参数时，**取出值为None**

**示例代码**

```python
parser.add_argument('username', required=True, location='args', type=str)
```

**不携带参数进行访问**

![image-20211115211635141](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211115211636.png)



#### 2.help

参数检验错误时返回的错误描述信息

```python
parser.add_argument('username', required=True, help='missing a param')
```

![image-20211115212018344](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211115212019.png)



#### 3.action

描述对于请求参数中出现多个同名参数时的处理方式

-   action='store' 保留出现的第一个， 默认
-   action='append' 以列表追加保存所有同名参数的值

```python
app = Flask(__name__)
api = Api(app)
class IndexResource(Resource):
    def get(self):
        # 1. 创建RequestParser实例
        parser = RequestParser()

        # 2. 添加验证参数
        # 第一个参数： 传递的参数的名称
        # 第二个参数（location）： 传递参数的方式
        # 第三个参数（type）： 验证参数的函数(可以自定义验证函数)
        parser.add_argument('a', required=True,
                            help='missing a param', action='append')

        # 3. 验证数据
        # args是一个字典
        args = parser.parse_args()

        # 4. 获取验证后的数据
        list = args.get('a')

        return 'get ...{}'.format(list)

api.add_resource(IndexResource, '/')
```

![image-20211115212831125](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211115212832.png)



#### 4.type

描述参数应该匹配的类型，可以使用python的标准数据类型str、int，也可使用Flask-RESTful提供的检验方法，还可以**自定义**

-   **标准类型**

    ```python
    parser.add_argument('a', required=True, type=int,
                                help='missing a param', action='append')
    ```

    

-   **Flask-RESTFUL提供的**

    检验类型方法在 flask_restful.inputs 模块中

    -   url

    -   natural 自然数0、1、2、3...

    -   positive 正整数 1、2、3...

    -   int_range(low ,high) 整数范围

        ```python
        parser.add_argument('a', required=True, type=inputs.int_range(1, 10),	# 匹配手机号
                            help='missing a param', action='append')
        ```

    -   boolean

    -   regex(指定正则表达式)

        ```python
        parser.add_argument('a', required=True, type=inputs.regex(r'^1[3-9]\d{9}$'),	# 匹配手机号
                            help='missing a param', action='append')
        ```

        ![image-20211115213354136](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211115213355.png)

        

-   **自定义**

    ```python
    def mobile(mobile_str):
         """
         检验手机号格式
         :param mobile_str: str 被检验字符串
         :return: mobile_str
         """
        if re.match(r'^1[3-9]\d{9}$', mobile_str):
            return mobile_str
        else:
            raise ValueError('{} is not a valid mobile'.format(mobile_str))
    parser.add_argument('a', required=True, type=mobile,
                        help='missing a param', action='append')
    ```



#### 5.location

描述参数应该在请求数据中出现的位置

```python
# Look only in the POST body
parser.add_argument('name', type=int, location='form')
# Look only in the querystring
parser.add_argument('PageSize', type=int, location='args')
# From the request headers
parser.add_argument('User-Agent', location='headers')
# From http cookies
parser.add_argument('session_id', location='cookies')
# From json
parser.add_argument('user_id', location='json')
# From file uploads
parser.add_argument('picture', location='files')
```

也可以指明多个位置

```python
parser.add_argument('text', location=['headers', 'json'])
```





## 4	关于响应处理

### ①序列化数据

Flask-RESTful 提供了marshal工具，用来帮助我们将数据序列化为特定格式的字典数据，以便作为视图的返回值。

```python
from flask_restful import Resource, fields, marshal_with, Api, marshal
from flask import Flask

app = Flask(__name__)
api = Api(app)
resource_fields = {
    'name': fields.String,
    'address': fields.String,
    'user_id': fields.Integer
}
class User(object):
    def __init__(self, user_id, name, address):
        self.user_id = user_id
        self.name = name
        self.address = address

# 使用装饰器方式
class Test1(Resource):
    # envelope表示的就是最终数据的key，这个也可以没有，没有的话就是没有对应的key
    @marshal_with(resource_fields, envelope='test1')
    def get(self):
        return User(1, 'xiaozhi', 'GuangDong')

# 不使用装饰器方式
class Test2(Resource):
    def get(self):
        user = User(1, 'xiaozhi', 'GuangDong')
        return marshal(user, resource_fields, envelope='test2')

api.add_resource(Test1, '/test1')
api.add_resource(Test2, '/test2')
```

![image-20211115215940544](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211115215941.png)





### ②定制返回的json格式

**需求**：接口返回json格式的数据

**解决**：Flask-RESTful的Api对象提供了一个 `representation` 的装饰器，允许**定制返回数据**的呈现格式

Flask-RESTful中默认实现的方法在下面的文件下

![image-20211116202342515](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211116202344.png)

```python
def output_json(data, code, headers=None):
    """Makes a Flask response with a JSON encoded body"""

    settings = current_app.config.get('RESTFUL_JSON', {})

    # If we're in debug mode, and the indent is not set, we set it to a
    # reasonable value here.  Note that this won't override any existing value
    # that was set.  We also set the "sort_keys" value.
    if current_app.debug:
        settings.setdefault('indent', 4)
        settings.setdefault('sort_keys', not PY3)

    # always end the json dumps with a new line
    # see https://github.com/mitsuhiko/flask/pull/1262
    dumped = dumps(data, **settings) + "\n"

    resp = make_response(dumped, code)
    resp.headers.extend(headers or {})
    return resp
```



我们需要对原文件进行修改

```python
from __future__ import absolute_import
from flask import make_response, current_app, Flask
from flask_restful.utils import PY3
from flask_restful import Api, fields, Resource, marshal_with
from json import dumps

app = Flask(__name__)
api = Api(app)
resource_fields = {
    'name': fields.String,
    'address': fields.String,
    'user_id': fields.Integer
}
class User(object):
    def __init__(self, user_id, name, address):
        self.user_id = user_id
        self.name = name
        self.address = address

@api.representation('application/json')
def output_json(data, code, headers=None):
    """Makes a Flask response with a JSON encoded body"""

    if 'message' not in data:
        data = {
            'message': 'OK',
            'data': data
        }

    settings = current_app.config.get('RESTFUL_JSON', {})

    if current_app.debug:
        settings.setdefault('indent', 4)
        settings.setdefault('sort_keys', not PY3)

    dumped = dumps(data, **settings) + "\n"

    resp = make_response(dumped, code)
    resp.headers.extend(headers or {})
    return resp

class Test(Resource):
    @marshal_with(resource_fields)
    def get(self):
        return User(1, 'xiaozhi', 'GuangDong')

api.add_resource(Test, '/')
if __name__ == '__main__':
    app.run()
```

![image-20211116203724134](https://gitee.com/xiaozhi-oos/save-img/raw/upload-img/img/20211116203725.png)



